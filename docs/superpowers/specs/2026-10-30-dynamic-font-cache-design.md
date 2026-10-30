# 完整字體、隨需字形與持久化快取設計

## 使用者選定的方向

不再讓現有翻譯詞庫的預烘焙集合決定能否顯示新字。採完整字體與隨需產生字形，加入 disk caching / page caching；文字與伺服器圖示依來源隔離。另增加性能分析擴展，針對遊戲的優化以實測證據決定。

## 已核對的現況

完整字體檔：regular 21,658,236 bytes、bold 21,637,352 bytes，原變數字體 36,143,328 bytes。未見過的字形可不烘焙，但字體檔、cmap 等共用資料仍有資源成本，不能宣稱整套字體只需幾 MB 或未知字完全零成本。

Minecraft 1.21.4 實際位元碼：class_395 是接收 ByteBuffer、FT_Face 與 size/oversample/shift 的 FreeType provider；class_395$class_397$1.method_2030 在需要烘焙時建立 NativeImage，經 class_1011.method_4316(FT_Face,glyphIndex) 產生像素後上傳原生圖集。因此可以重用既有隨需 TTF 字形路徑，不必每個字啟動外部 msdf-atlas-gen 程式。

## 實作方案

選擇原生 TTF 隨需路徑 + 有界快取，而不是把完整字庫複製到 26 個巨大圖集。先維持同一字體的 metrics 和像素來源；需要額外大倍率縮放時再評估共享多頁 SDF/MSDF，不把它當成防缺字的前提。

- Font identity：字體內容 SHA-256、face index、variation axes、FreeType/renderer 版本。
- Glyph identity：glyph index、像素大小、oversample、字重／合成樣式、hint/load flags、shift/transform、像素模式。不得只以 Unicode 當快取鍵。
- 記憶體：有界 LRU，實際使用才產生字形；提案 CPU 快取 32 MiB、磁碟 256 MiB，實機後再定案。
- 磁碟：保存可重建的 CPU glyph pixels、metrics 與版本；不保存 GPU texture ID／程序指標。原子寫入、長度與雜湊驗證，損壞就重算。
- 分頁：依出現過的字形配置，不因看到一個生僻字便生成整個 Unicode 區塊。GPU 上傳僅由 render thread 執行；磁碟／壓縮走背景工作執行緒。
- 冷啟動：首字生成仍有成本，不能承諾瞬間或零卡頓。優先預熱新可見內容，工作執行緒不可同時操作原生渲染用的同一 FT_Face。
- 失效：字體、字型設定或渲染版本變更即切換快取 namespace；資源重載刷新相依顯示快取，不能套用舊字寬。
- 安全：保留真正的圖示 provider，文字字型不得誤選 bitmap 橫幅；磁碟快取本身不會解決語意字碼衝突。

## 驗收

必須測試未出現在詞庫的受支援文字、冷／暖啟動、刪除與損壞快取、字體與資源包切換、圖示混排、動態任務文字、記憶體上限與重載。比較同場景幀時間、首次字形生成、cache hit/miss、磁碟與上傳成本。

## 目前狀態

已確認原生 lazy TTF 入口，效能分析擴展已接入原生 raster 與 MSDF 的計時。glyph disk read/write 計時通道已預留，但真正的磁碟／分頁快取尚未接線完成。這份文件不是已啟用的快取功能；也沒有縮減或替換正式資源包。字體檔不隨對話附件或 Git 變更散佈。

## 2026-10-30 本機試用進度

原生 TTF 像素磁碟頁已實作並通過實際 FreeType／Minecraft 類別的冷暖 JVM 測試，已簽章準備原實例下次啟動載入。這不是自訂 MSDF/GPU page cache，也未移除既有預烘焙圖集。實作、上限、停用與實機驗收界線見 client-framework/docs/native-glyph-cache-trial-20261030.md。
