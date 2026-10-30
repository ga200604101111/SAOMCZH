# 名稱、等級、距離與系統訊息的來源限定翻譯

日期: 2026-10-30。使用者要求先取得實際原文，再把名稱與等級等變動資料分開翻譯。本輪候選: Runtime 1.4.0、規則 1.8.0 / sequence 110 / 123 hooks、詞庫 1.0.13 / 8,426 條。Compatibility 1.1.0 與固定 Agent v17 不需替換。

## 已核對的輸入與既有缺口

- 截圖確認 Mini Treant、Marécage Putride、Training Pavise、OAK STUMP、[NPC] Local Lumberjack、帶距離的羅盤任務文字。
- 本地日誌確認 Calme Mortel activé / déclenché / épuisé / prêt ! 與 You are at the spawn. 的完整系統原文。未把玩家聊天上傳或寫入公開詞庫。
- Training Pavise、Training Katana 和任務句其實已有精確譯文。自訂 TipModel.name() 的 Hook 也已存在，不能把問題全部歸咎為缺詞或缺 Hook。本輪沿用該入口，增加受限格式碼／數量拆分與本地缺漏觀測，不重複新增同位置 Hook。
- 原 MobNameSplit.split() 只處理逗號分隔的名稱／稱號，不足以涵蓋一般單行名稱。MobBarRenderer.shownName() 另有原生快取與 player 標記，需要處理已快取文字和原始名稱的關係。
- 羅盤任務句加上距離後，已不等於字典的純句子；區域名稱原先只存在 items，names 詞庫缺少同一名稱。這次補上共用名稱與明確來源的 names/lines 查找，不把數值組合列成詞條。

## 翻譯結果與保留部分

| 原始名稱／文字 | 共用譯文 | 保留資料 |
|---|---|---|
| Mini Treant | 迷你樹人 | Lv. / Lv ° / 等級數值與加號 |
| Treant | 樹人 | 等級與原色彩 |
| Marécage Putride | 腐敗沼澤 | 羅盤樣式、方向、距離 |
| Se rendre là où résident les brigands | 前往盜匪的居所 | 原分隔符與 2773m 等距離 |
| Training Pavise | 訓練用大盾 | 稀有度色彩、數量與所有屬性 |
| Oak Stump / OAK STUMP | 橡木樹樁 | Lv ° 1+、滑鼠圖示、收穫提示 |
| Local Lumberjack | 當地伐木工 | [NPC] 標記與獨立的標記顏色 |
| Calme Mortel déclenché ! | 致命沉靜已觸發！ | 原通知顏色 |
| Calme Mortel épuisé ! | 致命沉靜已耗盡！ | 原通知顏色 |
| You are at the spawn. | 你已在重生點。 | ✔ 等原圖示 |

另包含致命沉靜的啟用、就緒通知。沒有新增 Mini Treant Lv. 3、Lv. 4 等數字專用字典，也沒有改寫原伺服器實體資料、等級、距離或物品數量。

## 來源邊界與顯示流程

SemanticText 只處理已核准的怪物、NPC、世界文字、物品和羅盤來源；未知或不符合限定格式的文字保留原文。名稱使用精確字典對照，不做全域 substring 替換。

怪物來源使用原生 MobBarEntry.player 布林值排除玩家。玩家直接保留原來源文字；怪物優先以 source name 查字典，避免上游已快取的文字阻止詞庫更新。顯示名稱改變後，由原生字寬／排版／快取接續處理。

系統通知只在 MessageHandler.method_44736 呼叫 ChatHud.method_1812 之前替換顯示引數。未修改玩家 signed chat 方法、封包、原始記錄或原訊息物件。整段訊息先過濾玩家聊天格式，避免在玩家聊天的某個子節點發現相同短句而誤翻。未知系統聊天不自動收集。

一般 EntityRenderer 的 NPC 入口只允許有 [NPC] 標記的整段文字；未帶標記的名稱保持原樣。標記與名字不同顏色時，保留各自 Style。對空根節點包裹相同有效 Style 的文字片段，允許先合成完整句子；真正的混色內容不盲目扁平化。

怪物缺漏收集受非玩家入口限制，記錄純名稱，不把等級放進 source_key。羅盤和物品的已有缺漏入口也先排除受支援的距離、數量格式再去重。寫檔仍由原有有界背景佇列處理。

## 文字與圖示資源

新譯文需要的「樁」被覆蓋檢查攔下，已從原始授權字體重建補齊所有 26 套相關圖集，現有語料覆蓋為 1,997 個漢字。30 個安全圖示別名及原 bitmap provider 保留，半形冒號規則持續生效。沒有向 Git 提交字體、圖片或資源包二進位。

## 已執行驗證與限制

- SemanticSurfaceContract: 34 項斷言，涵蓋等級／距離／數量、格式碼、NPC、系統通知、聊天排除、未知名稱、純名稱缺漏收集。
- NativeSemanticContract: 24 項斷言；檢查實際 5 個目標類別位元碼，再執行轉換後 JVM fixtures，測試玩家排除、原訊息不可變、顏色與空根節點組合。
- 整體 123 hooks / 33 個目標類別的原生 JAR 位元碼驗證通過。
- 使用現有 Agent v17 啟動隔離 JVM，載入 Runtime 1.4.0、新 bootstrap ABI 與規則 1.8.0 成功；不啟動 Minecraft 或 OpenGL。
- 完整 Python 測試: framework 136 項（5 項依環境略過），repo 30 項；161 項實際執行通過。
- 字形覆蓋、圖示保留、半形冒號、原有匹配鍵不變、詞庫雜湊和檔案格式均納入檢查。

以上為離線與隔離 JVM 證據，不是實際畫面已修復的宣告。尚未識別的第三方繪製來源與未知原文不宣稱全面涵蓋。系統通知修正只作用於載入後的新訊息，不回溯改寫聊天歷史。

## 部署與診斷

既有單一實例正常重啟後才載入新的 Runtime／規則與資源。使用 pending、next-start、staged 與內容定址檔案，不強制重啟或改寫目前掛載檔案。保留上版模組、資源與字典備份。

Runtime 成功翻譯時只記錄 SEMANTIC_TEXT_TRANSLATED hook=...，不輸出原文字串或玩家資料。status.txt 每 15 秒更新計數，修正以前只有啟動快照、transforms=0 容易被誤判的診斷缺口。

本機安全收集與建置目錄: D:/翻譯/SAO_ZH_Semantic_20261030。完整變更併入 main 後，驗證報告與本機部署記錄保留在該目錄。
