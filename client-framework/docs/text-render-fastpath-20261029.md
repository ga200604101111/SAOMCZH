# SAOMCZH 文字繪製攔截：Runtime 開發版效能改進

日期：2026-10-29。狀態：**開發分支、只含原始碼，未簽章、未正式部署。**

## 已修改

**Engine.java**
- Render 執行緒使用自己的有限翻譯快取，最多 48 個 Hook、每個 Hook 32 條、每條原文長度不超過 256 字元。
- 用 Hook 身分＋原文隔離各來源；規則/詞庫 epoch 改變即清空快取。
- 對啟用 capture 的 Hook，找不到譯文時不進行負快取，避免收集佇列暫滿後漏掉重試。
- 不快取 Minecraft 可變 Text 物件、不改寫原始 UI 模型。

**TextProjection.java**
- 不變的文字樹不再為每個子節點配置 ArrayList。
- 子節點有變更時才建立新清單；使用 Minecraft 1.21.4 的 method_27662 (copyContentOnly) 與現有 Style/append 邏輯建構結果。
- 保持原圖示與節點結構，不合條件時保留原文。

## 離線測試

- JDK 26 / Java 17 release target：原始碼全量編譯通過。
- Python 既有回歸：105 + 30 = **135 項通過**。
- 隔離 Java probe：測試原文不可變、彩色 Style、子節點個數、翻譯後文本、跨 Hook 隔離、epoch 清除、未知字串負快取及 LRU 界限，通過。
- 120,000 次相同無變更文字投影：每呼叫配置量從 **120 B** 減至 **24 B**，只表示模擬環境的配置量改善；不得推論遊戲 FPS 或延遲已改善。
- 詳細測試及原始碼備份：D:/翻譯/SAO_ZH_Render_Optimization_20261029。

## 繪製來源稽核（仍是待驗證候選，不等於漏譯）

用本地 SAOUI-2.41.2、SAOJob-1.1.3、SAOBlessing-1.1.0 的 .class methodref 靜態掃描 162 個 UI/HUD 類別：

| 類別 | 數量 |
|---|---:|
| 直接有宣告式 Hook | 14 |
| 呼叫已掛鉤的共用繪製方法 | 33 |
| 呼叫尚未直接掛鉤的共用繪製方法 | 6 |
| 類別直接參照底層文字繪製方法 | 21 |
| 沒有找到已識別的直接文字繪製方法 | 88 |

此掃描只看 class/method descriptor 的靜態引用；不表示已執行該呼叫、不表示文字一定會被翻譯，也不表示後 88 個畫面全部漏譯。排除純圖片/矩形、DrawContext 矩陣等非文字呼叫。

後續值得查的來源：SAOJob SaoPopupScreen、技能確認視窗、MapScreen；SAOUI MobBarRenderer、WorkbenchScreen、CamcorderHud，及少量直接 MSDF 繪製的文字。

## 下一階段：尚未實作的重點

1. 在物品、任務、技能、圖鑑語義來源翻譯「完整文字」，在量字寬之前處理，而不是無條件攔截所有 DrawContext。
2. 擴充精確的 class/method/descriptor call-site，對玩家 ID、簽章聊天、伺服器原始資料保持隔離。
3. 必要時依 Style/圖示/佔位符邊界安全重組 Text 子節點；不能保證完整保存樣式時，原文 fail-open。
4. 建立受限數值模板，避免傷害、治療及倒數的每一種數值組合都進入詞庫。
5. 文字譯文改變時，對字寬、分行、省略及顯示快取一起刷新；不清除視窗滾動、搜尋與選中狀態。
6. 用真實模組位元碼、無圖形 JVM 原生測試及實機畫面/效能驗收確認，再評估獨立簽章的 Runtime 發布。

## Windows 跨平台換行安全

本分支新增根目錄 .gitattributes，強制 languages/zh_tw/*.json 保持 LF。因版本清單包含 SHA-256 與大小，Windows core.autocrlf=true 若將原始 JSON 轉為 CRLF，會使詞庫雜湊驗證失敗；本修正不更改原有 JSON 內容與發佈版號。

## 發佈隔離

此分支不更新 extensions/v1/runtime.properties、extensions/v2/stable.properties、GitHub Releases、遊戲模組、已安裝 Agent、材質包或設定。**開發源碼不會被遊戲的官方簽章更新機制自動載入。**
