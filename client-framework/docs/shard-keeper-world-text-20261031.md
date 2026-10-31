# Shard Keeper：浮空文字分段翻譯修正（2026-10-31）

## 問題來源

使用者提供的遊戲畫面顯示大型英文浮空文字：

- `Shard Keeper`
- `Right-click: your shards of the day`

這不是 Zap Dialogue 的 NPC 對話視窗，也不能僅憑畫面推定它屬於 NPC Entity 名稱。從目前本機 `SAO_TW_Dynamic/review/entries.jsonl` 已收集的伺服器語言表資料確認，這個公共浮空標籤可能採用**法文來源鍵**，並在同一 Text 元件內以 `U+E001` 作為名稱和說明的分隔符：

`Gardien des Éclats \uE001 Clic droit : vos éclats du jour`

服務端對應的英文顯示字串則有 `Shard Keeper \uE001 Right-click: your shards of the day`。舊版詞庫只有 `Shard Keeper` 和另一種文句 `Right-click: your daily shards.`，缺少法文鍵與實際英文變體。原本的精確詞庫查詢也不會自動拆分中間帶 `U+E001` 的兩段文字。

## 修正範圍

1. 新增四個已經從伺服器語言表確認的精確詞條：
   - `Gardien des Éclats` → `碎片管理員`（names）
   - `Clic droit : vos éclats du jour` → `右鍵領取每日碎片。`（lines）
   - `Right-click: your shards of the day` → `右鍵領取每日碎片。`（lines）
   - `Right-click: your daily shards` → `右鍵領取每日碎片。`（lines）
2. `WorldTextSegments.resolve` 只對現有**明確白名單**的 world-text Hook 生效，對合併字串最多八段的 `U+E001` 進行分段精確詞條比對，保留分隔符和各段原有空格。不新增網路資料讀取、模糊匹配或全域 UI 攔截。
3. `StyledNameProjection` 在處理現有漸層 Text 時允許 `U+E001` 作為明確分隔符，分段轉譯後以原 Style 與字元原樣重建分隔符。仍遵守原本的樣式和安全界限，遇上複雜且無法保證保真的片段就保留原文。
4. **不把合併式字串額外寫入詞庫。** 名稱與說明仍依既有的 `names / items / lines` 共用詞庫精確查詢，避免重複與日後不同來源的譯文衝突。
5. 原版 Minecraft、SAOUI、SAOJob、Zap Dialogue、MCEF JAR 全部不修改。固定 Agent v17 不重新製作；只產生待簽章外部 Runtime，現有 1.15.0／137 個 Hook 規則保持不變。

## 測試

- `test_shard_world_text.py` 驗證法文、英文、原始精確鍵及詞庫版本，並限制只使用現有的世界文字 Hook。
- `RealShardWorldTextContract.java` 使用已安裝的 **Minecraft 1.21.4 真正 intermediary Text / Style 類別**和其 84 個原生依賴 JAR，在隔離 JVM 執行 23 個檢查：英文／法文全文、漸層、粗體、分隔符、原文未修改、未審核原文保持不變、非世界的 Actionbar 不被改寫。
- 完整框架與儲存庫回歸測試、字型覆蓋以及使用固定 Agent v17 的隔離啟動測試為部署前必要條件。
- 原有中文字幕使用的漢字已在啟用中的 CJK 圖集收錄，因此本次不需重建資源包。

**注意：** 獨立 JVM 測試不能當作 Minecraft 畫面或伺服器 NPC 的實機驗收。如果該大型浮空文字實際是伺服器發給玩家的另一種 Text 元件，仍須記錄其原生類別與繪製入口，不能直接擴大全域攔截範圍。
