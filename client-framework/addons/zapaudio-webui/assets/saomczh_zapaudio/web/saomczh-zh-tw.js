/* SAOMCZH zh_tw overlay for Sound of Aincrad 1.0.0.
 * Visual locale only. No changes to WebSocket, media devices, event handlers or player names.
 * Loads after the original FR→EN bootstrap, before voice-client.js.
 */
(function () {
    'use strict';
    const lang = (new URLSearchParams(window.location.search).get('zapLang') || '').toLowerCase();
    if (lang !== 'zh_tw' && lang !== 'zh_hk') return;
    const EN_ZH = Object.freeze({
    "Minecraft voice chat": "Minecraft 語音聊天",
    "Click to activate microphone": "點擊啟用麥克風",
    "Audio starts automatically": "音訊將自動啟動",
    "Disconnect": "中斷連線",
    "Refresh": "重新整理",
    "Disconnected": "未連線",
    "Connected": "已連線",
    "Waiting": "等待中",
    "Voice active": "語音已啟用",
    "Mic activated": "麥克風已啟用",
    "Mic off": "麥克風已關閉",
    "Close (Esc)": "關閉（Esc）",
    "Close": "關閉",
    "Mic": "麥克風",
    "Voice": "語音",
    "Music": "音樂",
    "Other": "其他音效",
    "Settings": "設定",
    "Quest Voiceline": "任務語音",
    "Nearby players": "附近玩家",
    "Nobody nearby...": "附近沒有玩家……",
    "Audio Settings": "音訊設定",
    "Additional settings": "進階設定",
    "Noise reduction": "降噪",
    "Mic power": "麥克風增益",
    "None": "關閉",
    "Light": "輕度",
    "Strict": "強力",
    "Mic mode": "麥克風模式",
    "Open mic": "常開麥克風",
    "Push-to-talk": "按鍵說話",
    "No key bound — Options → Controls → Sound of Aincrad": "尚未設定按鍵 — 選項 → 控制 → 艾恩葛朗特之聲",
    "Key:": "按鍵:",
    "Audio Output": "音訊輸出",
    "Voice range": "語音距離",
    "blocks": "格",
    "The minimum between your range and the other player's range applies. If a player has a lower range than yours, you won't hear them (and they won't hear you) beyond their range.": "雙方的通話距離以較小的設定值為準。如果對方設定的距離較短，超出對方的距離後，雙方就無法聽見彼此。",
    "Controls all Minecraft game sounds except music: blocks (place/break), players (footsteps, damage), mobs (hostile, passive), ambient (caves, water), weather (rain, thunder), UI (menus).": "控制除音樂以外的 Minecraft 遊戲音效，包括方塊操作、玩家動作、怪物、環境、天氣及介面音效。",
    "Output switching not supported by your browser": "目前瀏覽器不支援切換輸出裝置",
    "Connection error": "連線錯誤",
    "Microphone error": "麥克風錯誤",
    "Microphone not allowed": "未取得麥克風權限",
    "AI loading error": "AI 載入失敗",
    "🔌 Connecting to server...": "🔌 正在連接伺服器……",
    "🔗 Connecting to voice chat...": "🔗 正在連接語音聊天……",
    "✅ Authenticating...": "✅ 正在驗證身分……",
    "🎤 Requesting microphone access...": "🎤 正在請求麥克風權限……",
    "Microphone access denied": "麥克風權限遭拒",
    "Voice restricted": "語音功能受限",
    "Speaking...": "說話中……",
    "Unknown player": "未知玩家",
    "🚫 Voice disabled": "🚫 語音已停用",
    "🔇 Muted": "🔇 已靜音",
    "🎤 Mic active": "🎤 麥克風已啟用",
    "Reconnect": "重新連線",
    "❌ Invalid link - Connect to the server to receive your link": "❌ 連結無效，請連接伺服器取得專屬連結",
    "VOICE BANNED": "語音功能遭停權",
    "Permanent": "永久",
    "You can't enable your microphone. You can still hear other players, music and ambiances.": "你無法啟用麥克風，但仍可聽見其他玩家、音樂與環境音效。",
    "🚫 Voice banned": "🚫 語音遭停權",
    "Voice banned": "語音遭停權",
    "Banned": "遭停權",
    "expired...": "已到期……",
    "You are voice-banned — you can't enable your microphone.": "你的語音功能已遭停權，無法啟用麥克風。",
    "Ban expired — re-enabling voice...": "停權已到期，正在重新啟用語音……",
    "✅ Voice re-allowed": "✅ 語音權限已恢復",
    "You can use voice chat again": "你已可以再次使用語音聊天",
    "Reconnecting (%1/%2)...": "正在重新連線（%1/%2）……",
    "Mic: %1": "麥克風: %1",
    "Reason: %1": "原因: %1",
    "Duration: %1": "期限: %1",
    "⏳ Banned for %1 more": "⏳ 剩餘停權時間: %1",
    "%1d %2h": "%1 天 %2 小時",
    "Volumes": "音量",
    "Microphone": "麥克風",
    "Test": "測試",
    "Sound of Aincrad": "艾恩葛朗特之聲"
});

    const oldT = (typeof window.ZAP_T === 'function') ? window.ZAP_T : function(s) { return s; };
    const own = Object.prototype.hasOwnProperty;
    function translateEnglish(value) {
        return typeof value === 'string' && own.call(EN_ZH, value) ? EN_ZH[value] : value;
    }
    // Dynamic components may read the brand before DOMContentLoaded.
    window.ZAP_BRAND_NAME = translateEnglish('Sound of Aincrad');
    // Original voice-client.js calls t(fr, ...values) which delegates to ZAP_T.
    // Translate the TEMPLATE before interpolating variables, never the values.
    window.ZAP_T = function (fr) {
        const base = oldT.call(this, fr);
        let result = translateEnglish(base);
        for (let i = 1; i < arguments.length; i++) {
            result = result.replace('%' + i, String(arguments[i]));
        }
        return result;
    };
    // Keep all controls, values, IDs, listeners and microphone/audio device options untouched.
    function applyStatic() {
        document.documentElement.lang = 'zh-TW';
        window.ZAP_BRAND_NAME = translateEnglish('Sound of Aincrad');
        document.querySelectorAll('[data-zap-brand]').forEach(function (el) {
            el.textContent = window.ZAP_BRAND_NAME;
        });
        document.title = window.ZAP_BRAND_NAME;
        document.querySelectorAll('[data-i18n]').forEach(function (el) {
            // The original FR→EN DOMContentLoaded callback runs first.
            if (el.childElementCount !== 0) return;
            const original = el.textContent.trim();
            const translated = translateEnglish(original);
            if (translated !== original) el.textContent = translated;
        });
        document.querySelectorAll('[data-i18n-title]').forEach(function (el) {
            const title = el.getAttribute('title');
            const translated = translateEnglish(title);
            if (title !== translated) el.setAttribute('title', translated);
        });
        document.querySelectorAll('[data-i18n-tooltip]').forEach(function (el) {
            const tooltip = el.getAttribute('data-tooltip');
            const translated = translateEnglish(tooltip);
            if (tooltip !== translated) el.setAttribute('data-tooltip', translated);
        });
        // Three static text elements are intentionally not marked data-i18n upstream.
        // Limit these lookups to fixed element types, never player/device/option nodes.
        document.querySelectorAll('h3').forEach(function (el) {
            if (el.textContent.trim() === 'Volumes') el.textContent = EN_ZH['Volumes'];
        });
        document.querySelectorAll('#settingsModal label').forEach(function (el) {
            if (el.textContent.trim() === 'Microphone') el.textContent = EN_ZH['Microphone'];
        });
        document.querySelectorAll('#settingsModal span').forEach(function (el) {
            if (el.id !== 'pttKeyName' && el.textContent.trim() === 'Test') el.textContent = EN_ZH['Test'];
        });
        // Match the original Inter design while providing consistent CJK fallback.
        const font = document.createElement('style');
        font.id = 'saomczh-zh-tw-font';
        font.textContent = 'html[lang="zh-TW"] * { font-family: Inter, "Microsoft JhengHei UI", "Microsoft JhengHei", "Noto Sans CJK TC", "PingFang TC", sans-serif; }';
        document.head.appendChild(font);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applyStatic, { once: true });
    } else {
        applyStatic();
    }
})();
