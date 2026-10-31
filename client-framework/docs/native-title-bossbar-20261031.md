# 原生 Title／Subtitle 與 BossBar 文字翻譯（2026-10-31）

## 覆蓋差異

- **Minecraft 原生 Title / Subtitle：** 既有 `worlds-locations:hud-location-title`、`worlds-locations:hud-location-subtitle` 分別在 `InGameHud.method_34004(Text)`、`method_34002(Text)` 的文字參數進行翻譯；畫面的顏色、持續時間與動畫仍歸原版。
- **SAOUI 自訂 Boss HUD：** 既有 `worlds-locations:boss-plain`（`BossHud.plain`）和 `boss-visible-text`（`BossHud.saoText`）處理該自訂 HUD，不等於 Minecraft 原生 BossBar。
- **Minecraft 原生 BossBar：** 本輪新增 `worlds-locations:vanilla-bossbar-title`；僅攔截 `BossBarHud.method_1796(DrawContext)` 中單一的 `ClientBossBar.method_5414(): Text` 方法回傳值，讓同一份本地譯文同時供 `TextRenderer.method_27525` 量測及 `DrawContext.method_27535` 繪製。沒有修改 BossBar 進度、顏色、樣式、封包或原始 Server 文本。

## 與既有詞庫共用

原生 BossBar 名稱使用 `names`，可在其他已批准來源提供唯一一致的譯文時，重用 `items/lines`。只做精確名稱對照，不把任何玩家說話、任意 BossBar 句子或全域 JSON 當作子字串替換。

已從本機 `missing_zh_tw.jsonl` 的 `bossbar_title` 真實紀錄確認下列原文，新增精確繁體翻譯：

| 原文 | 譯文 |
| --- | --- |
| `Ambush!` | `伏擊!` |
| `Ambush! ` | `伏擊! `（保留結尾空格） |
| `Ambush — wave 1/3` | `伏擊 — 第 1/3 波` |
| `Ambush — wave 2/3` | `伏擊 — 第 2/3 波` |
| `Ambush — wave 3/3` | `伏擊 — 第 3/3 波` |

所有新增中文字均存在啟用中的 26 套中文字形圖集，故**此次不重建字型資源包**。

## 限定攔截與安全驗證

- Hook 的 owner、方法描述符、目標 getter、參數、翻譯分類、sites=1 皆在 Java Agent 和來源產生器做硬性白名單驗證；變更其中任何一項會被拒絕。
- `StyledNameProjection` 對可確認的彩色 Text 進行樣式保留，超出樣式、元件數或複雜度上限時原樣保留；不攔截 `BossBarPacket` 或改寫 BossBar 模型。
- 原生 Minecraft BossBar 舊版有 `bossbar_title` 本地收集，但並非所有原生 BossBar 都已完成翻譯，這次不增加任意名稱的收集與玩家聊天曝光。
- `NativeBossBarContract` 用實際 Minecraft 1.21.4 原生 JAR 證實唯一一個 name getter、翻譯插入點、文字量測和繪製順序、以及原類別 ABI 不變。
- 維持固定 `SAO_ZH_Agent_v17.jar`，只更新**已簽章的外部 Runtime** 和 `1.15.0` 規則；不新增 Fabric mod、不改原版 JAR，不公開上傳 Agent v17 或 Runtime 二進位檔。

> 程式及隔離 JVM 測試不是 Minecraft 實際遊戲內畫面驗收；不在原生 Text 路徑的 GUI 圖片文字仍不在本輪覆蓋範圍。
