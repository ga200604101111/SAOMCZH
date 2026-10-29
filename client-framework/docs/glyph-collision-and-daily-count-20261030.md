# 截圖問題更正：中文字碼／圖示與每日獎勵數量

使用者修正的判讀：圖二不是外部圖示遮住標題，而是標題內的漢字被當成圖示；圖四與圖五都屬於字形來源／字型問題，不是小標籤寬度問題。圖一要求名稱只顯示「治療碎片」，第三行的原生數量才是獨立數量欄，不應被移除。

## 已實作：只修每日獎勵名稱

在 languages/zh_tw/hook_text_rules.json 的 daily_reward_name 共用規則增加兩個 padded_exact：

- Healing Shard x2 → 治療碎片
- 治療碎片 x2 → 治療碎片

此規則只被 items-economy:reward-name 與 items-economy:reward-cached-name 使用。前者處理 DailyModel$Reward.name() 的顯示回傳值，後者處理 DailyScreen.drawSheet() 讀取 sheetName 的顯示字串。前後空格照原樣保留，原生數量 getter、give()、圖示和第三行數量未修改；translations.json 的全域帶數量對照也沒有更動。

開發分支的語言版本 1.0.11 記錄本次 hook text 規則變動；總詞條仍為 8,415。這是開發分支修正，尚未修改 main、簽章穩定索引、正式 Release 或遊戲中正在使用的檔案。

## 已查證：田確實與橫幅圖示共用字碼

從本地已快取的主資源包及附加包檢查到：

| 欄位 | 實際值 |
|---|---|
| 字元 | 田，U+7530 |
| provider 類型 | bitmap |
| 字型 JSON | assets/minecraft/font/default.json 與 uniform.json |
| 圖片 | hsur:required/ui/menu_banner.png |
| height / ascent | 256 / -14 |

詞庫中的任務標題是「19. 田野下的密室」。因此「田」確實可能進入選單橫幅字形路徑；這與截圖的大橫幅一致，也能合理解釋為何標題字寬暴增後被省略成「19…」。後者屬於依據資源與畫面作出的推論，本輪沒有開啟遊戲追蹤實際 glyph 查找。

主包辨識：main-735a6f59df540574eddc2fc23eb78af4df05bc7d69bd4207adb9e861ea1d7a8e.zip。
附加包辨識：support-60f032b10310b5ed6c801f9b0dc0f934794adbb3d720d06dea991821e85a4e99.zip。

## 已查證：匪、氾、嶄的字型覆蓋不完整

在主包內 26 份 MSDF 字形表中：

| 字元 | 有收錄的圖集數 | 備註 |
|---|---:|---|
| 田 | 0 / 26 | 另有上列 bitmap 圖示綁定 |
| 匪 | 4 / 26 | 只有 SAOUI 的四份 Work Sans 表；sao-ui 表沒有 |
| 氾 | 0 / 26 | 未收錄於檢查的 MSDF 表 |
| 嶄 | 0 / 26 | 未收錄於檢查的 MSDF 表 |
| 訓 | 0 / 26 | 同樣有缺字覆蓋問題 |
| 米、井、彭 | 各 4 / 26 | 同樣只補齊部分表 |

因此第 4、5 項均歸入字形路徑／覆蓋不一致，但不能把所有字都當成已證實綁到 bitmap 圖片。FontColorPolicy 只是顏色修補，沒有完成字形來源隔離。

後續修復必須區分文字用途與原生圖示用途：自然語言中的漢字使用一致的純中文字形及度量，真正的伺服器圖示仍保留原圖。不能全域刪除 U+7530 的圖示 provider，也不能只靠放寬標題寬度掩蓋問題。本輪沒有聲稱這部分已修好，也沒有替換或散佈字型檔。

## 驗證紀錄

- 新增 test_daily_reward_quantity.py 的 4 個測試。修正前名稱測試的 16 個子案例失敗；修正後全部通過。
- 完整 Python 測試：client-framework 115 項（5 項依環境略過）、repo 30 項；140 項實際執行通過。
- 舊版本行為比對僅明確調整兩個 Daily 名稱 Hook 的八個展開 exact keys，其他 Hook 的規則與舊行為仍逐項比對。
- 詞庫 SHA-256／大小驗證通過；120 個候選 Hook、30 個目標 class 的原始 JAR 位元碼驗證通過。
- 未開啟新的 Minecraft 實例，未做遊戲內截圖驗收。
