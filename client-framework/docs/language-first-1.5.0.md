# SAOMCZH v1.5.0：語言集中管理與模組 Hook 擴充

## 你指定的結構

- languages/zh_tw/translations.json：繁體中文的唯一主要詞庫（names/items/lines/messages），版本 1.0.7。
- languages/zh_tw/hook_text_rules.json：少數只對特定顯示方法有效的精確翻譯、數字模板與字串規則；仍然只歸這個語言管理。
- languages/zh_tw/zh-tw-version.json：版本、SHA-256、大小，以及 hook_text_rules 的內容檢查碼。
- languages/locales.json：已建立語言目錄的清單（目前只有 zh_tw；不能假稱其他語言已翻譯）。
- extensions/modules/<name>/module.json：只宣告 Java 類別、方法、文字入口、版面刷新與模組相依。不再藏「dictionary」「exact」「operations」「locale」。
- client-framework/tools/compose_localized.py：將指定語言的 Hook 特例和模組合併成可簽署、可驗證的規則；舊純模組 composer CLI 已拒絕跳過語言資料的直接發布。
- extensions/v2/rules-1.5.0.json：下載給 V16 Runtime 1.1.0 的宣告式規則。SHA-256/Ed25519 簽章與上個可用版本回退機制維持原狀。

## 比之前強在哪裡

原 1.4.0 規則把詞庫反覆塞進每一個 Java Hook，使整個簽名規則約 1.50 MB。1.5.0 將大量重複詞條集中管理，規則縮到約 73 KB，執行時透過已有的本機雜湊詞庫查詢。翻譯修改只需更新 languages/zh_tw，不必為了調整物品翻譯而更新每一支模組。

從 68 增加至 88 個攔截點，新增高價值的共通 PassCanvas 與 TextUtil 顯示字串、字串裁切前翻譯，以及圖鑑、拍賣行、日常獎勵、Boss HUD 的專用文字入口。先前原本有的入口、字串特例以及被採用的譯法均保留；在搬移測試中自動恢復 311 個會因譯表優先序而改變結果的特例，且全部集中在 zh_tw 的 hook_text_rules。

**這並不代表所有遊戲文字已全翻譯**：不存在的伺服器資料、圖形貼圖裡的字、未接入的特殊 UI、字型筆畫等仍另需檢查。特別是新共通繪字 Hook 為精確匹配，不做模糊全文替換、不收集玩家發言，也不修改任何帳號、玩家名稱、物品或伺服器狀態。

## 相容與多語言

既有 V16 下載器硬性讀取 languages/zh_tw/zh-tw-version.json 和 languages/zh_tw/translations.json，所以我們保留這兩個路徑，不要求重裝 Agent。Extensions 只有功能 Hook，不承載語言。將來新增 zh_cn/en_us 等語言時，要各自建立 languages/<locale>/translations.json、hook_text_rules.json、版本檔並核對相容的專屬更新通道；這次沒有偽造任何其他語言資料，也沒有假裝 V16 已支援各語言擴充規則自動切換。

功能模組新的 Hook 更新由外部 Runtime 1.1.0 透過已存在的 GitHub 更新機制熱套用；詞庫仍由既有下載機制非同步抓取。本次不更換 Agent、Runtime、相容 JAR 或材質包。遇錯保留上一版規則，且離線亦有已核對的本機詞庫。

## 驗證與侷限

完成 37 項 Python 測試、88 個 Hook 對 SAOUI 2.41.2 及 SAOJob 1.1.3 的位元碼檢查，以及隔離 JVM 中真實 Minecraft Text、Tooltip、系統訊息、回退與不修改原始物件的整合測試。未直接開啟你的 Minecraft，故不保證所有畫面在實際遊戲都已正常渲染。

舊規則 1.4.0、原始翻譯版 1.0.6 與遊戲本機執行檔保留作為回退依據。發布分兩階段：先推送 canonical 語言資料與模組原始碼，再更新已簽署的穩定規則指標，減少客戶端拿到新版 Hook、卻還沒取得新版語言表的時間差。


## 1.5.1 安全修正（新版功能規則熱更新）

通用 PassCanvas/TextUtil 畫字掛鉤只查 messages，不再用含大量地名、職業、物品名的 lines 去比對玩家的顯示字串。這避免碰巧與地名或職業重名的玩家被更名。圖鑑的艾恩葛朗特、物品詳細規格及其他專用名稱仍透過各自功能模組的專屬 Hook 查詢。measured-cjk-wrap 既有行為未修改，以保留已翻的取得方式及描述。若不經共用渲染器的文字依然未覆蓋，需依稽核繼續擴充來源，不應改寫玩家資料。
GitHub 維持前版 1.5.0 簽署規則供回退；1.5.1 是同一 V16 核心可接收的新版簽章規則，沒有新的 JAR 或材質包。
