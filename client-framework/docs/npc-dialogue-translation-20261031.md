# Zap Dialogue NPC 對話及選項翻譯（2026-10-31）

## 根本原因

使用者的 NPC 截圖顯示：

- 說話者 `Shepherd Ivo`
- 正文 `The pens are holding and my animals sleep. That is thanks to you.`
- 畫面為 `zap-dialogue-client-1.6.10.jar` 的 `DialogueScreen`，並非 SAOUI 的 NPC 名稱牌、任務選單或系統通知。

舊版 130 個 Hook 未包含 `fr/zapdialogue/`。原模組也提供 `OptionScreen` 及選項索引發送程序，必須保留原始索引。

## Java Agent 專用翻譯入口

新增模組定義 `extensions/modules/npc-dialogue/module.json`。只在原版 Zap Dialogue 的 4 個已審核方法參數做顯示翻譯：

| Hook | 實際方法 | 用途 |
| --- | --- | --- |
| `npc-dialogue:speaker` | `BubbleRenderer.drawSpeakerName(…, Text, …)` | 說話者顯示名稱，以 `names` 共用詞庫查詢 |
| `npc-dialogue:text-wrap` | `StyledText.wrap(StringVisitable, int, TextRenderer)` | 完整對話、選項文字在原生字寬換行之前轉譯，以 `lines` 查詢；未知文字經隱私過濾後記錄於本機 |
| `npc-dialogue:text-trim` | `StyledText.trim(StringVisitable, int)` | 翻譯後再裁切打字機可見字數 |
| `npc-dialogue:text-length` | `StyledText.length(StringVisitable)` | 使用中文全文長度計算打字機速度／進度 |

`BubbleContent.layout` 透過 `BubbleRenderer.wrapText` 通往同一 `StyledText.wrap`。`OptionScreen.layoutOptions` 同樣呼叫 `StyledText.wrap`，且保留選項原始 index；`OptionScreen.selectOption(int)` 仍以原 index 呼叫 `ZapClientPackets.sendOptionSelect(int)`，**沒有任何網路封包、選項模型或點擊方法 Hook**。

已補入的已確認譯文：

- `Shepherd Ivo` → `牧羊人伊沃`
- `The pens are holding and my animals sleep. That is thanks to you.` → `圍欄守住了，我的牲畜也睡得安穩。這都多虧了你。`

已存在的跨模組 `names`、`items`、`lines` 精確共用詞庫繼續重用；未審核的新 NPC 台詞及選項保持原文，避免自動猜譯。**此功能提供了對話與選項的通用入口，但不代表已翻譯遊戲所有 NPC 內容。**

## 安全與可逆

1. 所有 Hook 的原生 owner、方法描述符、參數位置、locale 和分類有嚴格白名單；新增 StringVisitable 投影僅允許原版 `StyledText.wrap/trim/length` 的第 1 個參數。
2. 不修改玩家聊天、玩家名稱、原始 NPC 對話、原生選項索引、伺服器資料與網路封包。
3. 僅在 `zh_tw` 生效；未知語言、不支援的 Text、未審核條目保持原文。
4. 未知多詞 NPC 文字只能經 `UnknownCollector.safe` 過濾、寫進本地 `hook_missing_zh_tw.jsonl`；不進行自動上傳，私聊格式與憑證／識別資訊被拒絕。
5. 本功能只由現有 `SAO_ZH_Agent_v17.jar` 加載**已簽章的外部 Runtime** 和規則；不另裝 Fabric 模組、不改 `zap-dialogue-client-1.6.10.jar` 或 MCEF。
6. 實際字型包從已驗證的原始基底生成；保留 `Lv.`、圖示與目前其他模組的字形路由。

## 已驗證項目與仍需驗收

- 真正 `zap-dialogue-client-1.6.10.jar` 的位元碼參數入口及 `OptionScreen.selectOption` 原始索引路徑驗證。
- 真正已安裝 Agent v17 在隔離 JVM 載入新的已簽章 Runtime、134 個 Hook，測試說話者、完整正文、選項、中文字數、打字機裁切、原始資料不變、非中文回退。
- 字型包與完整 Python 回歸測試，和遊戲內相同原始路徑的原版類別檢查。

**離線驗證不等於遊戲畫面驗收。** 實際文字顏色、選項 hover、玩家互動、字體與多人伺服器流程仍需原本實例重啟後確認。新 Runtime、規則與本地資源包只會在下次正常啟動載入，不會強制結束遊戲。
