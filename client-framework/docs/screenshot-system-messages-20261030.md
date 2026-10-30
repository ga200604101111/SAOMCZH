# SAO 畫面系統通知與世界名稱動態翻譯（2026-10-30）

## 範圍

此修正針對本次截圖與原始 `latest.log` 已確認的遊戲內容。字庫為 `languages/zh_tw/translations.json`；訊息內容使用模組化 Runtime 的既有 `dynamic-translation:game-system-display` Hook，限定 Minecraft 的用戶端顯示系統訊息入口，不修改玩家 signed chat、伺服器資料、物品數量或原始記錄。

既有 `names`／`items`／`lines` 的 Scoped SharedDictionary 不變：
- `Eugeo` → `尤吉歐`，`Cardinal Relay` → `卡迪納爾中繼裝置`。未能驗證身分的玩家名稱、空名稱或未命名列表仍保留原文，不使用全域玩家名稱替換。
- `Oak Bark`、`Young Oak Stick`、`Anthracite` 新增於 items；已存在的 `Coal Ore`、`Lingot de Fer Chaud` 與 `Creuset de Forge` 沿用已確認的物品對照。
- 不為每個溫度、物品數量、分數寫一筆固定詞條。

## 系統通知來源與翻譯模式

固定通知寫入 messages，保留原文鍵，包括燃料已備妥、開始加熱、開始加熱處理、淬火、完成、不可更改／使用位置的警告。

`DynamicSystemMessages` 僅接受完整限定格式：
1. `Heating stopped at N°C. Right-click with the item to insert it.`：僅翻固定文句，保留 N 與 °C。
2. `Item inserted: Nx ITEM`、`You added Nx ITEM`、`You received  ITEM`：保留原數量及空格，物品名稱從共用 items／names／lines 的精確對照取得。
3. `Furnace — temperature N pts | time N pts | precision N%`：保留分數與分隔符，翻譯標籤與單位。
4. 可選前置資訊圖示、`§` 格式碼和不同顏色的 Text sibling：只改文字片段，不展平不同 Style，原數值及色彩保留。

未匹配的訊息維持原樣；玩家聊天格式、私人訊息、未確認的 UI 使用者名稱，不會改寫。針對混色組件，必須先確認完整訊息符合上列已核准格式，才允許翻譯其中的片段，避免任意玩家聊天被局部套字典。

## 字型與介面

增補字需與既有中文 MSDF 字形矩陣同步，並確保保留圖示映射、原始材質以及 `Lv.` 等級標示。這次不修改 Java Agent、Compatibility、物品實際數量或其他畫面元素。尚未確認來源的介面「未命名」欄位保持原值，不能無條件隱藏，否則可能漏掉原生占位名稱或玩家資料。

## 測試契約

- `FurnaceSystemContract`：原始日誌句型、數量與溫度變數、格式碼、單位、Unknown names fail-open、玩家聊天排除。
- `StyledSystemTextContract`：真正的 `Text` styled sibling 模擬，檢查 prefix/item 兩色、紅灰警示、鍛造分數與溫度分段，包含輸入不變與色彩保持。
- `test_screenshot_messages.py`：詞庫內容、共用物品對照、世界名稱及不加入逐數值精確詞條。
- 既有 `test_glyph_coverage_guard.py`／`test_glyph_pack_contract.py`：所有新中文字需存在 26 套圖集，兼顧既有 bitmap icon 和 `Lv.` 斷言。
- 新的 Runtime、宣告規則及簽章更新在隔離 JVM 和實際目標 JAR 上測試；實機世界 UI 仍須重啟遊戲後驗收。

此檔紀錄具體內容修復，不宣稱已涵蓋所有外掛或全域聊天，也不把截圖當成完整遊戲內渲染測試。
