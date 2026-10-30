# Sound of Aincrad 1.0.0 · MCEF 繁體中文介面擴展

本擴展只負責 **Sound of Aincrad 1.0.0**（`zapaudioclient`）的可見介面文字。

## 來源與實作

已核對使用者實際安裝的 `zap-audio-fabric-1.0.0.jar` 與 `mcef-fabric-2.1.6-1.21.4.jar`：
- `WebClientExtractor.extract()` 從原模組 `web/index-template.html` 等資源解出網頁到 `~/.zapaudio-web-cache/v-*/`。
- `WebClientExtractor.rewriteAudioUrl` 保留服務端 `wsUrl` / `audioBaseUrl`，追加原生 `zapLang`，並讓 MCEF 使用本機 `file:` 網頁。
- 原生 bootstrap 只支援 `fr_*` 與英文 `FR_TO_EN`；`voice-client.js` 的 `t(...)` 直接轉呼叫 `window.ZAP_T(...)`。
- 這項擴展利用 Fabric Mixin 的 **extract 返回時**安裝一支小型、本地 `saomczh-zh-tw.js`，加入在原始 bootstrap 後、原生 `voice-client.js` 前，並由它接入 `window.ZAP_T`。原模組 JAR 不動。
- HTML SHA-256 強制限定已核對的 1.0.0 原版：`c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02`。未知或更新後的 HTML 直接保留原文（fail-open）。

## 功能保護

- 僅對 `zh_tw` / `zh_hk` 啟用，英文與法文沿用原本流程；保留 `%1` / `%2` 動態參數的內容。
- HTML 標記 `data-i18n` / `data-i18n-title` / `data-i18n-tooltip` 的已知文字、幾個原生漏標的固定標籤，加上 `ZAP_T` 動態狀態。
- 不對 `select#audioInputSelect` 或 `#audioOutputSelect` 的 option、玩家 ID、顯示中的伺服器名稱、音訊裝置名稱或按鍵值做全域替換。
- 不觸及 `voice-client.js`、`music-player.js`、`ambiance-player.js`、WebRTC、WebSocket、媒體裝置權限或音量滑桿的值／事件。
- `zapaudioclient/lang/zh_tw.json` 是遊戲原生按鍵／提示語言鍵，和 MCEF HTML 介面不同。
- 音訊介面自訂 CSS 使用原字體 Inter + 微軟正黑體等 CJK fallback；不修改任何 JCEF/CEF 原生檔案。

## 測試與限制

- `python client-framework/addons/zapaudio-webui/build.py`：僅組合本擴展到獨立 JAR，確認原聲音模組 JAR 不變。
- `WebUiLocaleInstallerContract`：使用原模組的真正 HTML / 3 個 JS 檔案，測試 patch 位於正確位置、15 項防呆、可重入、上游更新 fail-open、原音訊腳本內容不變。
- `webui-mcef-contract.cjs`：透過 Node VM 執行原版 HTML 語言初始化和擴展，對 `zh_tw/en_us/fr_fr` 共 52 項斷言。**這不是 Chromium 畫面渲染測試。**
- `test_zapaudio_mcef_target.py`：對實際安裝的 MCEF / Sound of Aincrad JAR、原生 extract descriptor 做 target 校驗。

本擴展並不呼叫外部 Edge。因為真正的 CEF 繪圖與音訊裝置在執行中的 Minecraft/MCEF JVM 中，未實際重啟遊戲前不能聲稱畫面或 WebRTC 功能已經實機驗收。若移除擴展 JAR，下次模組重建網頁快取會復原英文／法文介面；不需要移除原聲音模組。

## 安裝與回退

將產出的 `SAOMCZH-ZapAudio-ZH-TW-1.0.0.jar` 放進原實例的 `mods`，完整重啟原實例。**不要刪除、停用或覆蓋原版 `zap-audio-fabric-1.0.0.jar` 或 MCEF。** 如有相容性問題，退出遊戲、移除本擴展 JAR 再重啟即可回到原版。
