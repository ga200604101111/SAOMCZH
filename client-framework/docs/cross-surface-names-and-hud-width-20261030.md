# 跨模組名稱、彩色 NPC 與羅盤底板量測修復

日期: 2026-10-30。配套候選: Runtime 1.6.0、規則 1.10.0 / sequence 114 / 128 hooks、詞庫 1.0.17 / 8,769 條。固定 Agent v17、Compatibility 1.1.0 與現有字型資源包不需更換。

## 不是每個模組另寫一份翻譯

names / items / lines 已有來源限定的 SharedDictionary。模組顯示入口必須接到這個查表流程，才能重用譯名；有詞條不等於所有 renderer 都已用到。系統聊天、玩家名稱、無來源的共用繪製 API 不納入任意跨分類替換。本次新增七條只各自保存一次，沒有複製到三個分類。

新增名稱為 Shard Keeper → 碎片管理員、Copper Katana → 銅製武士刀；互動提示 Right-click: your daily shards. → 右鍵領取每日碎片。

銅製武士刀的已確認三行描述加入完整換行來源對照，翻譯為「以銅鍛造，刀刃比其他刀更堅固、更鋒利。」另有各片段精確對照作為保守備援。整段重排沿用已審核描述列序機制，不無條件合併未知空行、屬性、等級或不同格式。

## 已確認的程式原因

### 怪物名稱

本地與 GitHub 詞庫原本已存在 Mini Treant → 迷你樹人。實際 SAOUI 2.41.2 的 MobBarRenderer.LEVEL_NUMBER 使用 `✦ Lv.3 »` 與 `[Lv. 3]` 等標示。舊 SemanticText.privateMessage 把 `»` 當成聊天分隔符，導致即使名稱在詞庫內仍被整段略過。

修復只在已具備 player 布林保護的怪物來源，精確辨識原生星形等級前／後綴，抽出純名稱再檢查私訊格式、查表。等級、空格、符號、`Lv.` 大小寫與顏色保留；真正的 `Player » ...` 不會被放行，玩家本體仍由原本 player guard 排除。

### NPC／世界文字與非 literal Text

NPC 顯示未必走怪物血條，可能走 TextDisplay、SAOJob Hologram、ArmorStand 或有 [NPC] 標記的原生名稱牌。彩色漸層可能把每個字拆成不同 Style，舊版只能逐葉匹配，無法找到完整名字。

新增 StyledNameProjection 限定上述世界／名稱來源、地點 HUD 以及既有物品名稱來源；使用 Minecraft 原生 styled visitor 取得解析後文字，逐邏輯行進行精確查表，再依原字元位置重新採樣顏色。只有變動區間除顏色外的樣式 metadata 一致才會替換；字型、粗體、點擊／懸浮樣式等不同時保守保留原文。不修改來源 Text。

未變動的圖示前綴、等級後綴與其字型保持原樣。限制為 1,024 字元、128 個訪問片段、最多 8 行；陌生原文不重建物件。這不是全域玩家名稱翻譯，也不是任意字型／樣式扁平化。

### 地點大標題

已確認 SAOUI TitleTranslateMixin 把原生標題／副標題送到 TranslateClient.messages。Hanaka/HANAKA 譯名雖在 names/lines，仍可能沒有在這條路徑命中。

新增 class_329.method_34004(Text) 與 method_34002(Text) 的限定引數投影，分別在原生 title/subtitle 儲存前查 names，可重用同一個「哈納卡」。不把 messages 全表混入 names，不收集或改寫聊天。

### 羅盤地名、任務文字與白色底板

QuestCompassRenderer.flat(Text) 先取得可見字串並剔除 PUA 圖示；splitInfo 隨後儲存 infoName/infoDetail 及對應 Text。drawInfoBarContent 以這些內容量測、畫背景，最後才繪字。舊版若只在繪字時翻譯，可能沿用英文寬度；若過早編碼中文安全別名，原生 flat 又可能把別名當圖示刪掉。

新版在 native flat 呼叫結果處進行翻譯，先保存翻譯後的 String 和 Text，再由原生同一份資料量測白底、分隔符與字。地名則在 FlatText.of 的快取回傳之後、cityFold 和畫面建立之前查表。進入原生 flat 前只把本系統的漢字別名解回可讀文字，真正圖示仍照原流程處理。

詞庫 epoch 改變時，CompassTextState 只清除三個明確型別的派生來源快取欄位；同一 epoch 不反覆清除。距離數字、HUD 位置、動畫、原資料及任務狀態均不改動。這是修正量測來源，不是把白色長方形強制設成某個固定寬度。

## 驗證

CrossSurfaceContract 驗證實際原生等級格式、顏色碼、純名稱收集、玩家排除、跨分類共用及 Lv. 不變。StyledNameContract 與 CompassCacheContract 使用隔離 fixture 驗證樣式邊界和單次 epoch 失效；RealStyledNameContract 另以真正 Minecraft 1.21.4 的 Text/Style、styled visitor 及 translatable content 執行，不只使用自製字串模擬。

NativeHudSourceContract 對實際 SAOUI 與 Minecraft 類別執行 ASM 轉換，檢查翻譯在儲存文字、原生量測與繪圖之前；並驗證兩個 HUD title/subtitle 入口不涉及聊天。另執行既有物品描述、系統訊息、混色文字、原生攔截與詞庫回歸測試，再使用固定 Agent v17 啟動隔離 JVM。

測試期間未找到正在執行的遊戲 Java 程序，因此沒有附加到遊戲 JVM，沒有另開 Minecraft 或做 GUI 截圖驗收。離線通過不代表任何未知第三方 NPC renderer 都已覆蓋。原始碼合併後，新簽章 Runtime／規則只準備在原實例下一次完整啟動載入，取代前一份已辨識的待更新候選，保留備份。
