# SAOMCZH 效能分析擴展（開發版）

日期：2026-10-30。預設關閉；功能已實作，正式發布與實際遊戲卡頓診斷尚未完成。此模組是分析器，不是提升 FPS 的優化器。

## 為何與遊戲優化分開

使用者要求先了解卡頓，再做針對遊戲本身的優化擴展。分析模組不強制 GC、不變更 JVM 參數、不降低視距、不刪除模組、不改實體／AI 邏輯、不碰伺服器封包。後續優化需要相同場景、同樣設定的前後測試，單項可關閉與撤回。

## 已完成的觀測入口

從目前安裝的 Minecraft 1.21.4 intermediary JAR 與 SAOUI 2.41.2 JAR 驗證以下 5 個類別、7 個方法：

| 範圍 | 精確入口 | 代表什麼 |
|---|---|---|
| 幀循環 | class_310.method_1523(Z)V | 包含 tick、渲染、等待與限幀的 wall time |
| 客戶端 tick | class_310.method_1574()V | 客戶端邏輯，不是遠端伺服器 TPS |
| 遊戲渲染 | class_757.method_3192(class_9779,Z)V | CPU 呼叫耗時，不是 GPU 執行時間 |
| 視窗更新／等待 | class_1041.method_15998(class_10219)V | 可能包含同步與等待，不能推論 GPU 使用率 |
| TTF 字形生成 | class_1011.method_4316(FT_Face,I)Z | 原生字形第一次生成的成本 |
| MSDF 字寬量測 | MsdfRenderer.measure(...)I | 量字寬成本 |
| MSDF 繪製呼叫 | MsdfRenderer.drawLine(...)Z | 文字渲染呼叫成本 |

另以幀循環入口差計算 frame_start_interval，避免只用平均 FPS 掩蓋卡頓。字形磁碟讀／寫的兩個計時通道已預留，但尚未與磁碟字形快取接入；沒有樣本時是未量到，不能當成零成本。

PerformanceBridge 使用各通道固定 4,096 筆環形緩衝。報告包含最近視窗 P50/P95/P99、全程平均／最長與 ≥50ms 次數。不同範圍互相包含，不能把所有耗時相加。報告執行緒複製資料時，渲染端使用 tryLock，不等待檔案輸出；因競爭丟棄的樣本數另行揭露。異常離開的方法不計為完成；下一次進入會覆蓋舊時間並記錄 abandoned_scopes，原遊戲例外不會被吞掉。

## JVM 與深入分析

背景工作執行緒每秒讀取 heap used/committed/max、程序 CPU 比例、JVM 執行緒數、GC 收集次數與時間差。GC MXBean 的集合時間不等於精準 stop-the-world 暫停時間。

可選 deepJfr=true：最多錄製 60 秒，設定 maxSize=64 MiB，僅啟用 execution/native samples、GC pause、monitor/park、CPU 與慢速 file I/O。預設不開啟環境變數、系統參數或完整啟動資訊事件。JFR 先寫入 pending 檔案，完成後才原子改名，避免讀到空白或半成品錄製檔。JFR 的實際檔案大小仍受 chunk 行為影響，不應把設定值說成任何情況的精確硬上限。

PerformanceJfrReport 可在離線階段把 JFR 轉成取樣位置、事件次數與耗時統計，不輸出檔案路徑、IP、完整執行緒名稱或事件內容。原始 JFR 仍可能包含本機路徑，只留本地，禁止自動上傳。

GPU 執行時間、GPU 使用率、遠端伺服器 TPS、網路 RTT 本版皆未量測，JSON 明確輸出 null，不推估成數字。

## 使用邊界

固定啟動 Agent 不需更換；模組接在新版外部 Compatibility 的啟動流程，並把純 java.base 的 PerformanceBridge 放入已驗證的 bootstrap helper。java.management 與 jdk.jfr 留在 system loader，避免 bootstrap 找不到模組。

完成相容版本與簽章發布後，把 client-framework/config/performance.properties.example 複製至外部擴展根目錄的 SAO_ZH_Performance/performance.properties：

```properties
enabled=true
durationSeconds=120
deepJfr=false
```

啟用設定需在原有遊戲實例下次啟動時讀取；目前未實作關閉狀態下的遊戲內熱啟用按鍵。正在錄製時將 enabled 改成 false，取樣器會在下一次讀取控制檔時停止。超過 durationSeconds（允許 10–1800 秒）會自動停止。停止後繪製 Hook 只檢查 disabled 旗標，不再計時。

每次錄製輸出到 SAO_ZH_Performance/reports/session-日期時間-短ID/：
- summary.json：最新／最終摘要，原子替換。
- timeline.jsonl：每秒一筆數值觀測。
- deep-local-only.jfr：僅啟用深入模式才產生。

目錄最多保留 10 個錄製工作階段；達上限時拒絕新增，不自動刪除使用者報告。需自行移走或封存已分析的舊工作階段。

```powershell
py client-framework/tools/analyze_performance.py "<session 目錄>"
```

產出繁中 analysis.md。工具會提示慢幀長尾、GC 同期重疊、資料不足與未知 GPU／TPS，不會把相關性宣告為根因。

## 可重現驗證

```powershell
py client-framework/tests/run_performance_probe.py --jdk "<JDK 根目錄>" --asm "<ASM JAR>" --minecraft-jar "<原生 intermediary JAR>" --saoui-jar "<SAOUI JAR>" --java "<Java 21 執行檔>"
```

驗證包括有限緩衝、關閉不取樣、百分位數、實際目標位元碼、JVM 載入隔離 fixture、原始例外保留、真 bootstrap loader、背景報告寫入、控制檔停止及真實 JFR 生成／讀取。這些是離線／測試 JVM 的測試，不代表已量到目前遊戲的卡頓。

本輪查詢時沒有找到正在執行的 Java 遊戲程序，沒有自行開啟新實例。沒有修改 main、穩定更新索引、已安裝 Agent 或正式資源包；工具開銷與真實畫面 P95/P99 尚待實機同場景 A/B 測量。

## 本輪驗證結果

完整 Python 測試：128 + 30 項，153 項實際通過、5 項依環境略過。計時行為 13 項斷言、原生位元碼與 VM fixture 18 項斷言通過。真 bootstrap loader、背景報告、停止控制、JFR 原子落盤、JFR 讀取及去除私人路徑的摘要測試通過。JFR 事件數隨測試 JVM 活動不同，不能把斷言次數當成遊戲效能數據。

最終 Java 17 目標完整編譯與 Compatibility/embedded bridge 包裝檢查通過。開發候選及驗證報告留在 D:/翻譯/SAO_ZH_DynamicFont_20261030/preview，未簽章，不能直接安裝。
