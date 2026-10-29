# SAOMCZH 1.6.0 — 全介面文字來源擴充及精簡翻譯規則

## 這次做了什麼

- 10 個功能模組、115 個聲明式攔截點（1.5.1 是 88 個），由 extensions/modules 定義；不要按每個介面建立單獨 Java Agent。
- 新增加入任務左側清單標題／狀態／目標、任務 HUD 原始 Text、羅盤顯示、圖鑑 Local.hint/Local.lore、技能樹描述及公會成員詳情階級等。所有 Hook 都對 SAOUI 2.41.2 / SAOJob 1.1.3 的真實位元碼與 descriptor 驗證。
- 翻譯仍全部集中於 languages/zh_tw；沒有 extensions/zh_tw。
- 為 31 個經來源限定的語義欄位設定 capture family：quest、codex、skill、item、world。無玩家聊天、玩家 ID、國旗或操作封包入口。

## 重複翻譯例外整理

原 languages/zh_tw/hook_text_rules.json 裡，Coup、Hit、total 等基底詞按「原字／前空格／後空格／前後空格」四次重複，兩個 damage Hook 又重複相同對照與 operations。1.6.0 改成 schema 2 的共用 rule_sets 和 padded_exact（語言資料內的**編譯時語法**）。例：

    "rule_sets": {
      "combat_damage": {
        "padded_exact": {"Coup":"單次傷害","Hit":"單次傷害","打擊":"單次傷害","total":"總傷害","Total":"總傷害","total soigné":"總治療量"}
      }
    },
    "hooks": {
      "combat-feedback:damage-segment":{"use":"combat_damage"},
      "combat-feedback:damage-text":{"use":"combat_damage"}
    }

padded_exact **只能匹配完整原文字串的零或一個前後 ASCII 空格**，空格原樣留著，不能寫任意 "*" 或正則式。編譯器把它展開為舊版 Runtime1.1 已支援的 exact 表，因此不會把玩家名稱或別的詞拿來配對。當原始字串是例如「⚔ 打擊 21 | total 1009」時，保留原本有順序且有界的 replace_literal operations 來翻譯組合文字。不能把字串中的任意部分當成萬用替代。

同一組規則可以給數個功能 Hook 使用：combat_damage、item_properties、daily_reward_name、compass_goal、world_name，原本 23 組直寫翻譯全部比對過，編譯輸出與舊版逐字一致。來源 JSON 從 37,650 B 減到 16,120 B（約減少 57%）；最終發佈的已簽章規則會在編譯時展開，保留 Runtime1.1 相容性。

## 新增：不需要逐項開畫面抄字的收集方式

原本已保存的 SAO 原生完整語言表仍會整批匯出；這次額外新增已接入的執行期文字來源補漏。

外部 Runtime1.2.0 只監控在擴充規則中**明確標記**的少數語義入口：
- quest：任務列表、任務日誌、HUD。
- codex：怪物、地點、圖鑑提示／描述。
- skill：技能名稱、效果描述、技能樹。
- item：物品詳情、物品 Tooltip 與登入獎勵。
- world：專用傳送點及 Boss 名稱。

遇到繁中詞庫沒有對照的文字且通過隱私篩選後，將來源、不超過 480 字元的原文、Hook ID 寫入：
    D:\翻譯\SAO_TW_Dynamic\review\hook_missing_zh_tw.jsonl

收集只在記憶體中做輕量判斷並把訊息投入 1024 容量的非阻塞佇列，背景單一 daemon writer 批次寫入，單局最多 15,000 條，檔案最多 12 MiB。不能用此檔自動反向翻譯到聊天欄；原文字詞需審核後才可放進 languages/zh_tw/translations.json。

不收集玩家聊天／私訊、UUID、IP、網址、電子郵件、路徑、明顯密碼或 API token；跳過明顯使用者名稱、單字 ID、過長字串。不會自動上傳到 Hugging Face 或 GitHub，不會替換玩家名字，寫入失敗保留原文、不影響遊戲。

原本批次匯出工具 tools/export_inventory.py 現在接受 origin 為 hook.quest、hook.codex、hook.skill、hook.item、hook.world、hook.system 的來源並排除非批准來源；能保留中英混合詞句。原本的語言表、補丁材質包與舊資料都不會被覆寫。

## 部署與相容

- SAO_ZH_Agent_v16.jar **不更換**；外部 Runtime 升級到 1.2.0 是為了安全地收集新來源，下載並簽章驗證後，下一次正常啟動才載入此二進位模組。
- 規則 1.6.0（sequence 108）可以被已安裝的 Runtime1.1.0 安全讀取，新增的 capture 欄位在 1.1.0 上不執行；任務／圖鑑等新文字 Hook 仍能熱更新。
- 繁中翻譯版號升至 1.0.8（翻譯詞庫 8,240 筆保持內容不變，版本變動記錄 hook_text_rules schema 2），不修改遊戲選項、玩家帳號或資源包。
- 簽章／SHA-256 與更新失敗回退上一版邏輯維持，規則 1.5.1 保留於 Github 供緊急回復。
- 目前 **沒有完成所有介面的百分百攔截**：尚未送到客戶端的伺服器文字、圖片、特殊 OpenGL 字形、其他模組自繪 UI、超出 480 字的描述不在這次自動擷取範圍。任務標題「19…」與中文字型筆畫不一致需要在正式 Minecraft 畫面再驗證，不能因離線測試成功就視為修好。
