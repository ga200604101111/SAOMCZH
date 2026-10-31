<p align="center">
  <a href="./README.md">English</a> | <strong>繁體中文</strong> | <a href="./README.zh-CN.md">簡體中文</a>
</p>

# SAOMCZH

**Minecraft《刀劍神域》（Sword Art Online）社群維護的繁體中文翻譯專案。**

SAOMCZH 提供經過審核的翻譯詞庫、限定來源的客戶端文字攔截規則、中文字形與排版修正，以及採用簽章驗證的 Java Agent 更新架構。目標是在**不修改伺服器原始資料、玩家訊息或第三方模組 JAR** 的前提下，讓遊戲內容逐步繁體中文化。

> **語言說明：** 本 README 提供 English、繁體中文、简体中文三種閱讀語言；目前實際維護的**遊戲翻譯語系只有 `zh_tw`**。提供簡體中文說明文件，不代表已提供 `zh_cn` 遊戲詞庫。

## 目前的開發版本

以下為截至 **2026 年 10 月 31 日**，已測試的原始碼與本機整合狀態；**不代表所有對應的二進位檔都已公開發布到 GitHub Releases**。

| 組件 | 狀態 |
| --- | --- |
| 固定 Java Agent | `SAO_ZH_Agent_v17.jar` |
| 外部 Runtime | 1.9.1 — 已完成簽章並準備於下次啟動載入 |
| 攔截規則 | 1.15.0 — 137 個限定來源 Hook |
| 繁體中文詞庫 | 1.0.23 — 8,881 條 |
| Compatibility 相容模組 | 1.1.0 |
| 開發用遊戲環境 | Minecraft 1.21.4／Fabric；針對特定已安裝 SAO 模組進行測試 |

Runtime 與規則已通過位元碼檢查及隔離 Java Agent 啟動測試。**這不代表所有介面、字型或 MCEF 語音功能都已在實際 Minecraft 遊戲畫面完成驗收。**

## 功能特色

- **跨模組共用詞庫：** `names`、`items`、`lines`、`messages` 可在支援的介面之間重用已審核譯文，同時保留分類優先級及安全限制，避免重複翻譯。
- **限定來源的動態翻譯：** 支援部分物品名稱與描述、生物及 NPC 名稱、任務、技能、Actionbar／系統通知、地點、浮空文字及 HUD。
- **中文字形與排版：** 盡可能保留文字顏色、圖示、原生物品數量、`Lv.` 等級標示與數值，並在支援的入口先翻譯、再量測中文字寬。
- **漏譯收集：** 記錄經過允許的未知遊戲文字供後續審核，而不是無差別收集玩家聊天。
- **模組化更新：** 通過驗證的詞庫可與 Runtime、攔截規則分開更新；部分程式或資源更新仍需重新啟動。
- **Sound of Aincrad 網頁介面：** 透過 **Java Agent 的限定攔截入口**翻譯 MCEF 內嵌網頁，**不需安裝額外的 Fabric 模組**，也不修改音訊、WebRTC、裝置名稱或按鍵操作。

目前僅針對已確認的文字來源處理，不保證所有畫面都已翻譯；無法辨識或不支援的文字會保留原文。

## 專案目錄

| 路徑 | 用途 |
| --- | --- |
| [`languages/zh_tw/translations.json`](languages/zh_tw/translations.json) | 四個分類的繁體中文審核詞條 |
| [`languages/zh_tw/zh-tw-version.json`](languages/zh_tw/zh-tw-version.json) | 詞庫版本、檔案大小與 SHA-256 |
| [`languages/zh_tw/hook_text_rules.json`](languages/zh_tw/hook_text_rules.json) | 與語言相關且有執行限制的文字規則 |
| [`languages/locales.json`](languages/locales.json) | 已登記的遊戲翻譯語系 |
| [`extensions/modules/`](extensions/modules/README.md) | 模組化攔截規則及相依關係 |
| [`client-framework/`](client-framework/README.md) | Agent、Runtime、Compatibility 原始碼及技術說明 |
| [`tools/validate.py`](tools/validate.py) | 版本描述檔、雜湊與變數佔位符驗證 |
| [`tools/github_sync.py`](tools/github_sync.py) | 選用的 GitHub 詞庫快取同步工具 |
| [`tools/export_missing.py`](tools/export_missing.py) | 匯出本機收集的漏譯候選內容 |

根目錄的 [`manifest.json`](manifest.json) 記錄專案與語系版本資訊。**資源包另外維護於 [SAOMCZHPACK](https://github.com/ga200604101111/SAOMCZHPACK)**，不與翻譯詞庫混在一起。

## 翻譯如何更新？

1. 保留**原始文字比對鍵**，新增或修訂對應翻譯；不要破壞變數佔位符、顏色碼、圖示或必要的空白。
2. 更新詞庫版本，以及版本檔內記錄的 `size_bytes`、`sha256`。
3. 執行下列驗證指令，檢查結果後再發布。
4. 客戶端更新器可下載並驗證新版詞庫；已開啟的介面或已快取文字可能需要重新開啟或刷新。
5. Runtime 與 Compatibility 使用獨立的簽章驗證流程，通常在下次遊戲啟動時生效。**只更新詞庫不必重新製作固定 Java Agent。**

更新資訊採用數位簽章及 SHA-256 驗證。未簽章的開發版 Runtime 不應冒充正式可信任的更新。

## 開發者驗證

在 GitHub 專案根目錄執行：

```bash
python tools/validate.py
python -m unittest discover -s client-framework/tests -p "test_*.py"
python -m unittest discover -s tests -p "test_*.py"
```

需要自行編譯時，可參考 [`client-framework/tools/compile_sources.py`](client-framework/tools/compile_sources.py)。原始碼編譯只產生**未簽章**的開發檔案，部署前還須經過獨立的信任金鑰、簽章與驗證流程。

漏譯紀錄可透過以下指令查看匯出方式：

```bash
python tools/export_missing.py --help
```

分享前請再次檢查匯出內容；本機診斷紀錄可能包含情境資訊，不應直接全部公開上傳。

## 安裝與運作限制

這個儲存庫**不是一鍵模組安裝包**。在支援的環境中，既有固定 Java Agent 透過 JVM 啟動參數 `-javaagent:...` 載入，外部模組與規則則由各自的簽章更新流程管理。**不要把 Java Agent 當成 Minecraft 模組丟進 `mods`，也不需要為 Sound of Aincrad 翻譯另裝 Fabric 模組。**

請保留原版 SAO、Sound of Aincrad 與 MCEF 模組，並妥善保存上一次成功執行的備份。若更新 Runtime 或資源包，可能需要完整重開原本的遊戲實例。

## 更多技術文件

- [Agent 與 Runtime 架構](client-framework/README.md)
- [功能模組目錄](extensions/modules/README.md)
- [浮空文字翻譯流程](client-framework/docs/world-text-pipeline-20261029.md)
- [原生 Title、Subtitle 與 BossBar 翻譯](client-framework/docs/native-title-bossbar-20261031.md)
- [共用物品翻譯、命名容器與 NPC 名稱收集](client-framework/docs/cross-surface-translation-20261031.md)
- [NPC 對話與選項翻譯](client-framework/docs/npc-dialogue-translation-20261031.md)
- [遊玩時間與精簡時間單位](client-framework/docs/compact-time-units-20261031.md)
- [中文字體、排版與地點 HUD](client-framework/docs/typography-and-discovery-20261030.md)
- [純 Java Agent 的 MCEF 網頁介面翻譯](client-framework/docs/zapaudio-agent-webui-20261031.md)
- [獨立資源包儲存庫](https://github.com/ga200604101111/SAOMCZHPACK)
- [GitHub Releases](https://github.com/ga200604101111/SAOMCZH/releases)

## 社群專案與第三方權利

SAOMCZH 是非官方社群翻譯專案，並非《刀劍神域》或相關 Minecraft 模組的官方發行版。第三方內容與資產仍受其原授權及散布條件限制。請勿將伺服器憑證、私人簽章金鑰或未獲授權散布的原版模組資產提交到此儲存庫。
