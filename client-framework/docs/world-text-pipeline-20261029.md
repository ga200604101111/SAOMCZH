# SAOMCZH：世界浮空文字與共用文字繪製流程（開發版）

日期：2026-10-29
狀態：開發分支，未簽章／未啟用於正式遊戲。

## 目的與範圍

按照「語義來源 → 完整文字組合 → 字寬量測 → 繪製」建立來源限定的處理鏈，而不是攔截 Minecraft 全域的字形/渲染 API。

處理流程：

1. **語義來源**：由 domain-specific Hook 選擇 lines/names/items/messages 字典，保護玩家 ID、簽章聊天與原始封包。
2. **完整文字**：RenderTextPipeline 先處理已審核精確對照與受限 combat 數值樣板；對核准來源且樣式完全一致的 Text 子節點，可在分行前拼出完整字串。含異色子節點、§ 格式碼或私人使用區字形時不可扁平化，保留逐節點與原文。
3. **量測及斷行**：ArmorStand 在建立名稱顯示狀態後翻譯，交由原版 EntityRenderer 量測。原生 TextDisplay 的 TextRenderer.method_1728 會收到已翻譯的完整 Text；PassCanvas 的既有 CjkWrap 仍依原生字型度量分行；字形/Renderer 的全域寫入不變。
4. **繪製**：由原本 Minecraft / SAOUI / SAOJob 自己執行，無 GPU 注入、無伺服器封包修改。

## 世界浮空文字的三個已確認入口

- **Minecraft 1.21.4 Text Display**：net/minecraft/class_8138$class_8141.method_49057(Text,int)，在進入原生 TextRenderer.method_1728(Text,int) 前攔截 Text 引數。
- **Text Display 斷行快取**：net/minecraft/class_8113$class_8123.method_48905(...) 專用 field_42442，僅當詞庫 epoch 或語言／字型環境改變時重置，原始實體訊息與其他屬性不變。新增 reset_display_lines 僅允許這一個類別、方法與欄位。
- **SAOJob HologramRenderer**：com/eighteen/saojob/hologram/HologramRenderer.parseLegacySectionFormatting(String) 輸入 String 和回傳的帶格式 Text 都有獨立 Hook；在字寬計算前生效。
- **原版 Armor Stand Nameplate**：只在 net/minecraft/class_877.method_62375(ArmorStand,ArmorStandRenderState,float) 完成原版 superclass 狀態提取後，將客戶端 render-state 的 field_53337 Text 交給翻譯器。規則採固定 kind=armorstand_nameplate，只許這個 class/method/descriptor/lines；不碰玩家 RenderState、Entity NBT、伺服器名稱或聊天。

這三條是已從實際 JAR 位元碼驗證的顯示路徑。但使用者提供的截圖**無法僅憑畫面判定究竟是哪一種或其他自繪系統**。方塊告示牌、玩家頭頂與聊天、圖片文字、未知第三方文字**不在本次新增的覆蓋保證內**。

## 截圖文字對照

- Jump and Run (Easy) → 跳躍跑酷（簡單）
- (Respect if you finish it) → （成功通關者值得敬佩）
- 包含換行的兩句完整原文也具備 exact 對照，避免在原生 TextDisplay 中因拼接/換行順序而漏譯。

版本為 **zh_tw 1.0.10 / 8,415 筆**，只在本開發分支；正式主分支仍為 1.0.9。

## 附加增強

- RenderTextPipeline 新增一組只允許 combat-feedback Hook 呼叫的有界數值格式：閃避傷害、魔力護盾容量、總治療量。數字保持原樣，不使用泛用 regex 替換玩家聊天。
- Engine 延續有限 per-thread memo，規則/詞庫更新清空，capture-enabled 的未知文字仍可在佇列滿時重試。
- 不變 Text 不再建立多餘的子節點 ArrayList；核准的語義來源優先嘗試完整文字匹配，異色/圖示直接 fail-open。
- World TextDisplay 的顯示快取記錄仍使用 WeakReference，並把限定的弱狀態數量從 256 擴到 2,048，減少密集浮空文字場景頻繁被逐出而重排的風險。
- 成功翻譯時只以 once-only 記錄 WORLD_TEXT_TRANSLATED hook=...，不記錄玩家名稱、來源字串或座標。

## 驗證

1. 編譯：JDK 26 --release 17，完整 Runtime 原始碼編譯通過。
2. Python：client-framework 111 項測試（5 項依環境略過），另有 repo 30 項測試，所有已執行項目通過。
3. native 測試：120 個 Hook、30 個目標 class、4 個原始 JAR（Minecraft 1.21.4 intermediary、SAOUI 2.41.2、SAOJob 1.1.3、SAOBlessing 1.1.0）均通過 RuleTransformer 位元碼驗證。
4. Java probe：38 項 isolated checks，驗證 TextDisplay 翻譯在原生斷行方法之前、Hologram 在格式處理前後、Text 的顏色與圖示、原資料不可變、快取 epoch 刷新與相同 epoch 不重排。
5. **沒有啟動 Minecraft 或驗證當前畫面 FPS**。離線測試通過不等於特定伺服器的所有浮空文字都會翻譯。

本地測試目錄：D:/翻譯/SAO_ZH_Render_Optimization_20261029

## Runtime 和 Agent 的升級順序

這次修改 Engine.java、Script.java、TextProjection.java、RenderTextPipeline.java、RuleTransformer.java 等 Runtime 源碼。**既有固定 SAO_ZH_Agent_v16.jar 不需要重製**，但若要在正式遊戲啟用，需發布並簽章**新 Runtime（預定 1.3.x）**，之後才可簽章並切換到**新規則 1.7.0 / sequence 109** 及翻譯 1.0.10。

**不可把目前的未簽章開發 JAR 直接蓋到正式版，也不可先把規則 1.7.0 掛上現有 Runtime 1.2.0**，因為新的非 void TextDisplay refresh / 專用 cache action 需要新 Runtime 才能解析。只有待發行檔案真正簽章及驗證成功後，遊戲的安全更新器才能載入。

不更動舊的 Runtime 索引或正式穩定規則，並保留上一版本可回復能力。
