# 字形修復整合與半形冒號

日期: 2026-10-30。使用者明確同意把 dev/text-render-fastpath-20261029 合併到 main，並要求中文顯示文字全部採半形冒號。

## 這次修正的實際載入落差

檢查原實例日誌時，已使用 Agent v17，但實際載入 Runtime 1.2.0、Compatibility 1.0.2（僅原生 TTF 字形磁碟快取試用）、規則 1.6.0 / 115 hooks。主資源包 hash 仍是 735a6f59df540574eddc2fc23eb78af4df05bc7d69bd4207adb9e861ea1d7a8e。因此只有換 Agent 或啟用字形快取，不會自動載入前面完成的中文字／圖示隔離與 26 套字形補齊。

整合配套版本為 Runtime 1.3.0、Compatibility 1.1.0、規則 1.7.0 / sequence 109 / 120 hooks、詞庫 1.0.12。字形包仍沿用已驗證的 1,996 個漢字、30 個文字／圖示隔離別名與 26 套完整覆蓋圖集，不重新生成或變更圖示字碼。

## 半形冒號

- translations.json 的 307 條輸出: names 75、items 129、lines 96、messages 7。
- hook_text_rules.json 的 14 處輸出（exact、padded_exact、operations.to）。
- 主資源包 42 個 zh_tw / zh_hk 語言檔的 552 個全形冒號。
- SplashBridge 載入 53 條啟動畫面文字時，將其中 5 個全形冒號轉成半形；既有啟動字形圖集已含半形冒號，保留原圖集與授權資訊。

只把 U+FF1A 改為 U+003A。原文匹配鍵、operations.from、佔位符、格式碼、數字、空白與圖示保持原值。不全域改寫玩家聊天、帳號名稱、正規表示式或原始伺服器資料。

新增 test_halfwidth_colons.py 防止未來詞庫／規則重新加入全形冒號。原文鍵集合以 SHA-256 固定驗證，避免誤改匹配鍵。舊版行為回歸只對此明確的輸出變更調整預期，其他行為仍需一致。

## 安全啟動與到期語義

已安裝 Runtime / Compatibility 繼續驗證簽章與內容 hash，但不因更新描述檔日期過期而失效。網路更新及尚未接納的 pending 描述檔仍檢查期限。

同樣補齊已安裝 signed rules 的恢復流程: parseInstalled 只在已驗證簽章、hash 的本地 active/previous 快取使用；新取得的規則仍用有期限檢查的 parse。未修改其他語法、安全範圍或序號回退限制。

新增 rules/next-start.json 與 next-start.sig 的啟動接納流程。新版 Runtime 才會讀此入口，避免把新的 hook kind 提前熱載入舊 Runtime。成功套用並持久化後才移除 offer；失敗保留原規則。

## 整合驗證

- 132 項 framework Python 測試，5 項依環境略過；repo 30 項全部通過，共 157 項實際執行通過。
- LiteralGlyphsContract 254 項斷言通過。
- BootExpiryContract 8 項斷言，涵蓋本地過期可用、新更新過期拒絕、損壞簽章拒絕及 previous 回復。
- InstalledRulesExpiryContract 驗證已安裝規則期限與新規則期限分離，語法限制不放寬。
- SplashPunctuationContract 驗證 53 條啟動文字的半形冒號、字形覆蓋及其他語言不變。
- 真正以 v17 -javaagent 啟動隔離測試 JVM，成功載入 Runtime 1.3.0 及 120 hooks 的啟動規則，沒有啟動 Minecraft。
- 120 hooks / 30 個真實目標 class 的位元碼驗證通過。使用中的 Agent v17 class bytes 與目前來源一致，無須替換已開啟的 Agent JAR。

整合 Compatibility 保留既有啟動畫面的 5 個資源檔，避免只從 Java 源碼編譯後遺失開場畫面。

## 本機部署界線

本機更新寫到內容定址新檔、pending 描述檔、next-start 規則與 staged 資源包標記；不改寫正在載入的模組或已掛載 ZIP，不強制結束遊戲，不另開實例。舊版描述檔、資源包標記與字典保留備份。

主資源包 SHA-256: 08db08ad0f1181099453d433409338956bd46ba9822b2207a522c8f21611a3c3。
附加包 SHA-256: 4a25ed6b2b15d4d51e6f51a5b782de0e0ee6bee70b77891a86e5ade5449b8e25。

程式、規則及本機資源必須配套載入。沒有把字體或資源包二進位提交到 Git 歷史；沒有把「離線測試通過」說成使用者目前畫面已經改好。需要原實例正常重啟後，才能驗收任務標題、圖示、字體和半形冒號的實際畫面。
