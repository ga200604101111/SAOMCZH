# 功能模組 1.1.0 審查紀錄

本次沒有可用的獨立 subagent reviewer；由實作者對來源、測試及實際 native 轉換逐項自查，不能稱為獨立審查。

## 已驗證
- 7 個功能模組，不按畫面重複建立插件。單模組可含多個 class/method 攔截點。
- 同一現有 V16 kernel / Runtime 1.0.0：編譯成原有 schema-1 規則，多的 module_catalog 供作者與檢查工具使用。
- 相依版本、循環、字典衝突、重複 hook site、占位符、輸入大小、允許指令受限。
- 顯示用投影，不改 id、status、封包、交易資料、不關閉視窗。
- native class/method signatures 逐一由實際 SAOUI 2.35.0 和 SAOJob 1.1.3 確認。
- 25 個作者端測試；同 JVM 17 項模組更新與選擇性回退檢查；38 hooks/10 類別原生驗證。

## 檢查發現並修正
- 非字串 dependency key 會以 TypeError 外拋而非明確拒絕：先新增失敗測試，再補型別檢查。
- tests 可以傳入空 object、數字或 null，部分跳過、部分 TypeError：改成受限的 array 與全字串欄位。
- Python bool 被接受成 schema_version=1：改成嚴格 int。

## 尚存限制，禁止以已完成描述
- 既有相容模組雖已外部化，仍整體於啟動載入；未全拆成逐功能可熱卸載。
- 這次新增模組來源採一份簽章 bundle 原子切換；單模組回退需要作者組合舊模組＋其他新模組，以新的 sequence 發布，不是撤回全站簽章檢查。
- 每畫面可見內容是否立即更新取決於攔截的讀取時機。已快取成 List/圖像/布局的字串，可能需要更深入刷新策略。本輪沒有通用 UI cache invalidation。
- 初始化期、native/abstract、修改 class schema 不在這個規則引擎範圍。
- 尚未測試正在運行 Minecraft 的逐介面視覺與互動。不能保證所有 Unicode/文字來源都涵蓋。
- Runtime 的本地 override dictionary 和模組 scoped exact 是不同層：模組 exact 先處理，通用 dictionary 只作未命中的 fallback。本輪不宣稱已統一所有來源優先級。
- 針對已查證的精確方法與 sites，不等於支援不同模組版本同名方法的任何語意變動；更新模組版本後仍需驗證。

## 發布防護
僅推送 modules 的人工選定對照與程式；不推送原模組 JAR、日誌、UUID、伺服器 raw dump、維護者私鑰或授權憑證。正式包以既有 Ed25519 信任根簽署。材質包不重建，kernel SHA 必須保持不變。
