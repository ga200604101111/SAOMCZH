# Sound of Aincrad：純 Java Agent MCEF 繁體中文介面（2026-10-31）

此擴展**不安裝、不修改 Fabric 模組**；僅由現有 `SAO_ZH_Agent_v17.jar` 載入已簽章的外部 Runtime 1.8.0 與規則 1.12.0（sequence 116、130 hooks）後，攔截原版 `fr.saofr.zapaudio.client.web.WebClientExtractor.extract(): Path`。

## 運作時序
1. Sound of Aincrad 1.0.0 用原生 `WebClientExtractor.extract` 從 JAR 解壓 `web/index-template.html` 等檔案至 `~/.zapaudio-web-cache/v-*/`。
2. Java Agent 在 **extract 的 Path 回傳**執行一個已白名單限制的 `web_extract` 規則。只在 `zh_tw` locale 生效，不讀取伺服器資料、不翻玩家名稱。
3. 驗證原始 HTML SHA-256 `c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02`、正確網頁快取路徑、無符號連結；只在本機 HTML 的 upstream 語言 bootstrap 後插入 `saomczh-zh-tw.js`。該資產隨已簽章 Runtime JAR 搭載。
4. MCEF 仍由原模組啟動與渲染。網頁只替換既有 `ZAP_T`、`[data-i18n]`、`[data-i18n-title]`、`[data-i18n-tooltip]` 及少數原始未標註的固定字標；任何原有 callback／event listener／HTML 結構保持不變。

* 玩家 ID、麥克風與音訊輸出裝置名稱、按鍵值、音量滑桿值、WebRTC 音訊協商、WebSocket 封包與訊息歷史均不修改。
* 不修改 `zap-audio-fabric-1.0.0.jar`、`mcef-fabric-2.1.6-1.21.4.jar`，不覆蓋原模組的 `voice-client.js`、`music-player.js`、`ambiance-player.js`。
* 未知原模組版本或快取路徑不合法時 fail-open；翻譯故障不阻止 Sound of Aincrad 的原始啟動。
* 只在 SAOMCZH 外部 Runtime／規則更新後重新啟動原本的實例；不需另安裝 Fabric mod，也不變更固定 Agent v17。
* 當 SAO 原模組更新 HTML 時需重新審核其 SHA256 及原始 MCEF 網頁結構，不能盲目套用。
* **限制：** 程式測試不是實際 Minecraft/MCEF 遊戲畫面或多玩家語音實測。

## 驗證

* 真正原版模組 JAR 的 `extract` 兩個返回點、強制靜態同步方法、字節碼及 ABI 比對：15 checks。
* 在隔離 JVM 使用**真正已安裝的 Agent v17**、已簽章 Runtime/規則，執行動態載入的 fixture extract：繁中自動嵌入腳本，英文 untouched，遊戲沒有啟動。
* 原始 HTML 三個語言 bootstrap + 翻譯 JS：Node VM 52 項，`zh_tw/en_us/fr_fr`，裝置與玩家字串及點擊 callback 不變。
* 模組 HTML 安裝器 15 項：SHA allowlist、路徑防護、原本三個媒體 JS 不變、重入與未知版本 fail-open。
* 175 個 framework tests（5 skipped）、30 個 repository tests，與全部 130 hooks／36 個實際目標類別驗證。
