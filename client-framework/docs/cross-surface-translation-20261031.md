# 共用物品詞庫、命名容器、Actionbar 與 NPC 名稱補漏（2026-10-31）

## 問題分析

使用者在正式遊戲截圖中發現：物品 `Iron Axe`／`Coal Ore`／`Tactical Boots` 和 `Lingot de Fer Chaud` 已有詞庫譯名，卻仍在某些面板、鍛造訊息和跨服通知顯示英文。原因並不只有詞條缺失，而是多套不同的原生繪製入口並未全部攔截。截圖的版面中還有與文字渲染不同的美術材質或自訂 UI，沒有證據可以把 `RARITY`、`DURABILITY`、`SYSTEM CONTROL AUTHORITY` 這類美術標籤一律認定為可直接翻譯的普通字串。

## 本輪新增入口

- `named-container:visible-title`：Minecraft 1.21.4 `HandledScreen.method_2388` 只攔截標題 `field_22785`，不修改玩家背包副標題 `field_29347`、伺服器容器名稱資料、容器封包或玩家箱子標題。只接受 `Forge - Categories`、`Forge — Categories` 及已確認的 Anvil 類型等嚴格白名單，再從共用 `names/items` 詞庫查詢。
- `dynamic-translation:game-actionbar-display`：Minecraft `InGameHud.method_1758(Text, boolean)` 的第一個參數，在顯示到 Actionbar 前進行限定範圍翻譯；保留原有 Boolean 參數，並不攔截 ChatHud 全域流量或玩家聊天。
- `npc-dialogue:speaker` 增加 `npc_name` 本地收集源，僅授權 Zap Dialogue `BubbleRenderer.drawSpeakerName`。拒絕單字 ID、玩家名稱格式、憑證與非公開來源。已確認的 `Shepherd Ivo` 既有對照沿用，未知 NPC 名稱待遊戲重新載入規則後，才可能進入本地的 `hook_missing_zh_tw.jsonl`。
- Item Info 的 `[Iron Axe]` 等帶方括號標題，僅轉換**確定為物品顯示入口**的名稱部分，不改原本括號。材料 `Coal Ore: 5/10` 會使用已審核詞條，保留數量。用 `items` 既有譯文覆蓋已知 Anvil 標籤，避免同一原文字串在 `names/messages/items` 重複維護出衝突譯文。
- 僅針對已審核格式的鍛造/Interlink/Actionbar 句型，動態替換固定文字並重用物品名稱；配合受控 `< 1 min` 排隊格式，保留時間縮寫、百分比、數量、打擊偏差 `ms` 與圖示樣式。

## 容器 / 物品 / 玩家資料的安全邊界

`Minecraft class_465` 與 `class_329` 的實際原生 JAR 通過位元碼 Hook 契約：只在指定的 UI 顯示位置做轉換，原生類別欄位與方法簽章不變。未知玩家自訂容器標題保留原文，不收集到翻譯資料庫。全部只透過既有的 `SAO_ZH_Agent_v17.jar` 載入簽署的外部 Runtime 與規則，不新增 Fabric 模組，不改原版 JAR。

## 漏譯收集現況

本地 `missing_zh_tw.jsonl` 內有 `Woodcutting`、`Iron Axe`、`Coal Ore`、`Tactical Boots`、`New objective` 等資料；`hook_missing_zh_tw.jsonl` 裡有 `Return to Shepherd Ivo`、`Talk to Bob`、`20. La mine de Geldorack`、`Obtain a Geldorack Dungeon Key` 等**任務**文字。不可把任務描述提及的 NPC 名稱冒充已由新增 `npc_name` 功能收集；新 `npc_name` Hook 需下一次正常啟動載入後，才會開始處理此專用顯示入口。

## 驗證與未覆蓋內容

新增的受控文字模板、已知容器標題、括號物品名稱、跨服物品重用均有獨立 Java 契約與實際 Agent v17 隔離啟動測試。更新前不宣稱直播 Minecraft 渲染已全部翻譯；尤其圖片式自訂美術文字、未知裸英文、伺服器訊息未經文字顯示 Hook 的情況，仍需逐項取得實際來源。字型包按完整新詞庫重新建立 26 套字形圖集，但不能將圖片中的英文自動當成可替換文字。
