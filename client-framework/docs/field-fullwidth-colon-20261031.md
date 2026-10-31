# 修正裝備屬性欄位的全形冒號（2026-10-31）

## 問題與驗證來源

指定格式：**`攻擊傷害: 7`**，冒號須為 `U+003A`，而不是全形的 `U+FF1A`，其後保留一個半形空格。

由實際詞庫 `languages/zh_tw/translations.json` 與啟用中的中文字型資源包 `zh_tw.json`／`zh_hk.json` 檢查，兩者的譯文均沒有 `U+FF1A`。此外，舊的 `FieldTypography` 本來就能將 items 分類中標籤後的 `U+FF1A` 轉成半形。

**漏網點：** `items-economy:equipment-stat` 的原生來源是 `fr/sao/equipment/EquipmentClient$Stat.name()`，但對應 Hook 指定 `section="names"`。此前 `RenderTextPipeline.resolve()` 只對 `section="items"`（非物品名稱）執行 `FieldTypography.normalize`，因此 `names` 的裝備屬性若收到 `U+FF1A`，即使已經是中文，也會原樣返回。

## 修正

只為確定的 `items-economy:equipment-stat` + `fr/sao/equipment/EquipmentClient$Stat` 開啟相同的字元正規化處理。其他 `names`（人物、玩家、地點）與 `messages`（聊天、時間、網址）不會被全域替換，原本的 `items` 正規化保持不變。

`FullwidthStatColonContract.java` 的測試採用**真實 U+FF1A 字元**，包含：
- `攻擊傷害：7` → `攻擊傷害: 7`
- `攻擊速度：1.2/s` → `攻擊速度: 1.2/s`
- `閃避：+5%` → `閃避: +5%`
- 含樣式碼／物品圖示的項目。
- `Lv. 3+`、`12:30`、玩家聊天與其他地點名稱保持不變。

修正前首例測試失敗，修正後 11 項均通過。**這證明 Runtime 轉換路徑正確；不能據此宣稱已經在使用者的遊戲畫面完成驗收。**

此變更僅更新 SAOMCZH **外部 Java Agent Runtime** 的程式碼；固定 v17 Agent、已簽章規則 1.12.0、詞庫 1.0.19 和原版模組 JAR 均不需要更換。部署採用簽章的下次啟動候選版本，且不放任何 Java Agent／模組 JAR 到 GitHub。
