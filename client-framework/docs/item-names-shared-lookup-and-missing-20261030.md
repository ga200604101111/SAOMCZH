# 物品名稱數量、共用查表與漏譯收集

日期: 2026-10-30。候選配套: Runtime 1.4.1、規則 1.8.1 / sequence 111 / 123 hooks、詞庫 1.0.14 / 8,745 條。固定 Agent v17 與 Compatibility 1.1.0 不需改動。

## 問題與處理

每日獎勵的 daily_reward_name 原先有 Healing/Mana/Stamina/Halloween 四個 prefix 規則，會把原文的 xN 一併寫進譯名；先前只針對 Healing Shard x2 做 exact 特判，所以魔力碎片仍出現重複數量。

ItemPresentation 改為在已確認的八個物品名稱顯示入口處理: 每日獎勵名稱與 cached sheetName、原生 ItemStack 名稱、自訂 TipModel.name、ItemInfo 名稱與標題、ItemLang 名稱、拍賣物品名稱。先分離正整數的 xN / × N / N x 名稱格式，再查純名稱；數量不放回名稱欄。原有獨立數量欄、實際 ItemStack 數量、獎勵數量、屬性、藥水階級與 lore 中的數字均保持原樣。

例如 Mana Shard x2 與已翻譯的魔力碎片 x2 都顯示為「魔力碎片」；獨立的 ×2 / 2 字串不會被刪除。未知名稱也能清除名稱欄的重複數量，但不會冒充翻譯成功: 啟用 capture 的入口仍收集純名稱，不把這種清理結果當成正向翻譯快取，佇列暫滿時可再次嘗試。

這是名稱欄的顯示政策，沒有全域刪除字串中的數字，沒有改寫原始伺服器物品資料，也沒有移除 lore 的全域帶數量譯文。舊 declarative rules 留作相容性資料，新 Runtime 的名稱政策在這些舊 prefix 規則之前執行。

## 共用翻譯表

SharedDictionary 統一明確內容來源的精確查找: 優先目前分類；沒有時，允許在 names / items / lines 之間重用已審核的相同原文。物品、圖鑑、怪物、標記 NPC、任務、羅盤、世界文字及指定技能來源可使用。不同備援分類對同一原文有不同譯文時，不自行猜測，保留原文；目前分類的明確覆寫仍優先。

messages、一般玩家聊天、玩家名稱與無來源的共用 renderer 不加入跨分類查找，不做全域 substring 或近似替換。此輪針對模組化 Runtime 的內容入口統一查表；舊 Compatibility 的既有行為保留。

稽核顯示現有 3,440 個原文同時存在多個分類，譯文都一致。為維持舊客戶端相容性，本輪不批次刪除既有匹配鍵。這次新增 319 條全部只寫入 items 一次，其他合格入口透過共用查找重用，沒有再複製到 names / lines。

## 新詞條與數值格式

本輪固定快照共 403 筆尚無精確對照的物品類來源:

- 319 筆完成精確翻譯，包括黑暗魔杖、附魔金屬錠、蜘蛛手鐲、狩獵弓、卷軸、木材、魚類、礦物、鍛造材料、工具、鍛造技能與工序提示。保留來源鍵、前後空白、圖示與數值，所有譯文使用半形冒號。
- 76 筆由四種受限且完整匹配的物品格式覆蓋: 耐久度、採集能力、持有材料數、製作經驗獎勵。只翻標籤並保留數字，不逐一建立耐久度組合。
- 8 筆暫留待查: 三筆情境不足的來源、一筆未展開 {{TIMER}}、三筆被舊格式佔位符規則誤判的百分比句子，以及一筆已中文化但含重複數量的合成顯示字串。未把殘句或模板變數猜成完整譯文。

本機詳細紀錄: D:/翻譯/SAO_ZH_Items_20261030/TRANSLATION_BATCH.json、remaining_items.jsonl。
新增文字所需的字形已重建並驗證 26 套圖集，語料覆蓋由 1,997 增至 2,037 個漢字；維持 30 個文字／圖示別名隔離與原圖示 provider，不把字體二進位提交至 Git。

## 漏譯是否全部在 miss

不是所有畫面上的外文都會收進同一個檔案。目前主要有兩條收集管線:

1. D:/翻譯/SAO_TW_Dynamic/review/missing_zh_tw.jsonl: Compatibility 的物品、技能、Actionbar 等指定顯示入口。檢查時共有 2,525 筆歷史記錄，其中 actionbar_system 440 筆。
2. D:/翻譯/SAO_TW_Dynamic/review/hook_missing_zh_tw.jsonl: 新 Runtime 宣告式 Hook 的任務、圖鑑、物品、技能等指定來源。檢查時共有 77 筆歷史記錄。

其他 *_seen.jsonl 與 entries.jsonl 是原始來源觀察／匯出，不全是尚未翻譯的內容。上述兩個 missing 檔也是歷史記錄，完成翻譯後不會把舊行刪除，不能把總行數當成目前剩餘漏譯數。

Actionbar 確實包含在主收集管線，但仍須通過來源、長度、格式與隱私檢查。純數字、部分含 § 的格式字串、中英混合、玩家聊天樣式及地址／識別碼可能被過濾；重複文字會去重，寫入佇列及檔案也有容量界限。未掛鉤的介面不會憑空被掃描。未知系統聊天保持不自動收集。本次沒有放寬成全域聊天攔截，也沒有上傳原始缺漏檔或玩家聊天。

## 驗證與部署邊界

- ItemTitleAndStatsContract: 127 項斷言通過，修正前 85 項失敗。
- SharedDictionaryContract: 14 項斷言通過，修正前跨分類三項失敗；保留衝突 fail-open 和聊天隔離。
- ItemCaptureContract: 純名稱收集、數量移除不當成已翻譯、避免正向快取吞掉漏譯重試。
- 既有 SemanticSurfaceContract 34 項、NativeSemanticContract 24 項通過；123 hooks / 33 個實際目標 class 位元碼驗證通過。
- 以 Agent v17 啟動隔離 JVM，成功載入 Runtime 1.4.1 與規則 1.8.1；未啟動 Minecraft 或 OpenGL。
- framework 139 項中 134 項執行通過、5 項依環境略過；repo 30 項全部通過。合併至 main 後重新執行，具體結果保留本機報告。

本機更新只寫入已驗證的新內容定址檔與 pending / next-start / staged 指標，保留原 active 與備份。不強制關閉或另開遊戲。更新仍須原實例完整重啟，離線測試不代表已完成遊戲內畫面驗收。
