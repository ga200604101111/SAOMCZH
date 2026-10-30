# 描述段落保守重排與跑酷浮空文字

日期: 2026-10-30。候選: Runtime 1.5.0、規則 1.9.0 / sequence 113 / 124 hooks、詞庫 1.0.16 / 8,762 條。固定 Agent v17、Compatibility 1.1.0 與現有字型資源包不需更換。

## 本次資料來源與定位

使用者提醒原始描述可能刻意分行，不應一律合併。先前 Sylvan Bark 顯示成「一種天然材料，／主要用於／適合輕巧而迅速的／護甲。」的原因，已從實際 SAOUI 2.41.2 確認: TipModel.description() 回傳 List<String>，ItemTip 對每一列分別呼叫 PassCanvas.wrap。舊的逐列譯文無法重新整成通順的一段。

本次截圖兩句原文 Impossible Jump and Run、Respect if you finish it 已存在本機 SAO_TW_Dynamic/review/entries.jsonl 的 names 收集資料；主要 missing_zh_tw.jsonl 與 hook_missing_zh_tw.jsonl 沒有這兩句。這說明原始來源曾被收集，不代表已在 missing 中，也不代表可只憑截圖斷定目前採用哪種世界文字渲染器。圖上文字補入 lines 共用詞庫，供既有 TextDisplay、SAOJob Hologram、ArmorStand 顯示路徑查找；未放寬全域玩家名稱／聊天收集。

- Impossible Jump and Run → 不可能的跳躍跑酷
- Respect if you finish it → 成功通關者值得敬佩
- 包含換行的兩句完整原文也有對照，保留雙行格式。舊簡單版翻譯不變。

## 不猜測換行的段落處理

新增 description_list 種類只允許 items-economy:tooltip-description-paragraphs、fr/sao/tooltip/TipModel.description()Ljava/util/List;、items 分類。不得套用到 effects、stats、actions 或任意 List/玩家聊天。

LoreParagraphs 只使用「詞庫中已審核、以換行明確記錄來源列序的完整描述」來合併，沒有按逗點、短句、行長或中英文任意猜測。此次審核森林樹皮的英文與法文四列原文，整段譯為:

一種天然材料，主要用於製作輕巧靈活的護甲。

原描述已被相容層逐列翻成中文時，可由原文片段的既有譯文建立對照索引，重新匹配同一個已審核段落；不靠全域中文字替換。

保護條件:
- 空行、原本嵌在單列內的換行、不同顏色／格式、不同縮排及私人使用區圖示，禁止跨越合併。
- 屬性、數字、Lv.、材料計數、冒號欄位、條列與操作提示不當成段落拼接。
- 未確認的列序保留原本分行；有單列翻譯仍可逐列套用，不宣稱每件物品已完成整段翻譯。
- 不修改來源 List、物品資料、數值與操作欄；只回傳新的顯示 List。
- 由原生 PassCanvas 按當前實際字寬重新換行，行數與提示框高度隨原生布局計算；中文譯文不硬寫固定每行字數。

## 容量、快取與更新

每份描述最多 128 列、總共 8,192 字元；一次段落匹配最多 8 列，索引上限 1,024。只對完整匹配且完全已審核的整段結果做每執行緒 32 筆的有限快取；未知內容仍可重試缺漏收集。詞庫重新載入時重建段落索引，epoch 改變會使編碼結果快取失效。

一般文字投影仍維持原本的型態保護。因 ArrayList、ListN、List12 可能不同，另新增有界的 Dispatch.description List 介面，沒有為了段落功能放寬全域 Dispatch.value 的型態檢查。

此次新文字全在已驗證的 2,043 漢字範圍中，不需重建或替換字型包；半形冒號、Lv.、圖示字碼隔離與其測試繼續保留。

## 驗證及限制

LoreParagraphContract 驗證已審核英／法文、已翻譯片段、未知／刻意換行、空行、異色、圖示、原清單不可變、epoch 與容量限制。NativeLoreContract 轉換實際 SAOUI TipModel 位元碼，再執行同簽名 fixture，確認 List 實作不同不會讓結果被攔截器拒絕，且原模型不被更動；ItemTip 的描述先於原生 wrap 呼叫。

另外執行現有系統訊息、混色文字、怪物／NPC／物品／世界文字回歸、整體原生類別規則驗證和 Agent v17 的隔離 JVM 啟動。實際遊戲未被強制重啟，也沒有另開 Minecraft 實例；離線通過不代表截圖所在伺服器畫面已驗收。

原始碼合併到 main，簽章的新 Runtime／規則放入 pending、next-start 等待原實例下次完整啟動。現行檔案與舊版備份保留。純新增完整段落詞條以後可跟著詞庫更新，不需要再為每一段修改程式。
