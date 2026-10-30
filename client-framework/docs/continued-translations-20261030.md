# 動態翻譯續批與 cooldown 混譯修正（2026-10-30）

## 截圖問題

截圖顯示「使用一次 - cooldown of 1 分鐘」。本機漏譯收集已確認來源為：
- `Single use - cooldown of 1min`
- `3 uses - cooldown of 3min`

舊詞庫同時存在完整句與片段翻譯；部分 UI 會分片繪製，因此 `Single use`、`cooldown of`、`1min` 可能分別查表。先前 `cooldown of` 在 names/items/lines 的譯法不一致，造成某些入口保留英文或顯示為沒有冒號的「冷卻時間」。

本次統一為：
- `cooldown of` → `冷卻時間: `
- `Single use - cooldown of 1min` → `使用一次 - 冷卻時間: 1 分鐘`
- `3 uses - cooldown of 3min` → `可使用 3 次 - 冷卻時間: 3 分鐘`

保留 ASCII 冒號與一個半形空格。

## 新翻譯批次

從目前 missing/hook_missing 中只處理可確認的公開遊戲內容；殘句、亂碼及來源不完整的資料不猜譯。本輪新增 67 個鍵，涵蓋：
- 任務：探索隧道、懸崖、星期二篝火等。
- 技能／鍛造：武器與副手等級、溫度與判定窗口說明。
- 生物／圖鑑：樹人戰士、盜匪刺客、盜匪弓箭手、精英樹人、林地法師、巨型守衛。
- 地點：格爾多拉克礦坑。
- 物品：森林弓弦、阿爾巴爾藥水。

共用名稱仍由 SharedDictionary 在 names/items/lines 間重用，不為每個模組複製同一條譯文。

三條含「10% forging」「5% more」「21% faster」的技能敘述暫緩，因舊 placeholder 驗證器把自然語言中的百分號誤判為 printf 參數。未用繞過驗證的方式發布。

## 字形

新增譯文需要 11 個先前圖集沒有的漢字：隧、脊、瞰、憑、崖、男、闖、粹、宛、篝、鑿。從已驗證的原始未路由基底重新產生 26 套字形圖集，漢字語料覆蓋由 2,043 增至 2,054。原始圖示提供者與 Lv. 標示保持不變。

## 驗證

- client-framework Python：163 tests，5 skipped，0 failures。
- repository Python：30 tests，0 failures。
- translation validator：zh_tw 1.0.19，8,836 entries。
- glyph candidate：26 atlases，2,054 Han corpus；原圖示路由與 Lv. contract 通過。

此批是詞庫／字型資源更新，不新增 Runtime Hook。實際畫面仍需客戶端重新載入相應資源後驗收。
