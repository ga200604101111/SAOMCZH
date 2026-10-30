# 原生 TTF 字形磁碟／分頁快取：本機試用

日期：2026-10-30。狀態：程式已實作；真實 Minecraft 字形類別、FreeType 與跨 JVM 冷／暖啟動測試通過。本機專用 Compatibility 1.0.2 已簽章，放入 pending，原實例下次啟動載入。尚未做遊戲畫面／FPS 驗收，未發布至全體玩家的 stable 更新來源。

## 這次可以先用的範圍

- 使用 Minecraft 1.21.4 原生 TTF/FreeType 的隨需字形生成與原生 GPU 圖集；新增的是 CPU 字形像素的記憶體快取和持久化磁碟頁。
- 只攔截 TrueTypeFont 建立／關閉，以及其專用 glyph baker 呼叫 NativeImage 的位置。沒有改寫全域 FT_Load_Glyph、字寬／排版或 OpenGL 上傳。
- 已出現的字形才寫入磁碟。同一頁最多 64 個相近 glyph index，但不因看到一個字便預生成整個區塊。
- 第一次出現時仍可能走原生生成；背景讀取尚未完成時也直接用原生路徑，不讓渲染執行緒等磁碟。後續啟動可讀回已生成的像素。
- 磁碟不保存 GPU texture ID 或原生指標。GPU 圖集在每次執行時由 Minecraft 正常重建。
- 這不是自訂的動態 MSDF 分頁渲染器，沒有移除原本 26 套預烘焙圖集；MSDF 已存在的字形也不會經過這個 TTF 快取。它不會新增字體原本沒有的字形。
- 字體與圖示隔離、其他開發中的翻譯／渲染修正未隨此本機最小試用一起部署。不得將「快取已可試用」理解為所有畫面問題已驗收。

## 隔離與容量

快取 namespace 包含字體 ByteBuffer 的 SHA-256、face index、FreeType 版本、CPU 架構、size/oversample/shift 與 native size/transform；每筆再核對 glyph index、寬高及像素長度。變數字體目前保守使用原生路徑，不套用不完整的 variation axes key。原生 transform 或尺寸變更時不重用舊像素。

記憶體像素頁預設 32 MiB（LRU），另外最多 8 MiB 有界待寫佇列及固定數量 metadata；磁碟頁預設 256 MiB，採最舊持久化頁逐出。這些不是整個字體系統的總記憶體上限，原生字體資料、GPU 圖集和暫存仍有成本。

檔案有格式版本、頁 identity、長度限制與 SHA-256；損壞或不相容時不返回像素。原子更名防止讀取半成品，寫入鎖避免兩個程序同時覆寫；第二個占用者不能取得鎖時保持原生字形生成。只管理自有 .gpage 檔案，不刪除其他檔案。限額不包含最大 4 MiB 的單次原子寫入暫存與小型狀態／鎖檔。

## 本機啟用與停用

外部根目錄是 D:/翻譯。已建立：

```properties
# D:/翻譯/SAO_ZH_GlyphCache/cache.properties
enabled=true
memoryMiB=32
diskMiB=256
```

正式原始碼預設關閉；上列是使用者要求先用而啟用的本機設定。需要原本 SAO 實例下次正常啟動，不新增遊戲實例，不修改 -javaagent 或 JVM 參數。

快取頁目錄：D:/翻譯/SAO_ZH_GlyphCache/pages-v1。
運作後的 status.properties 包含 memory_hits、misses、disk_reads、disk_writes、memory_bytes、corrupt_pages、io_errors 等計數；未啟動新模組前不會有有效遊戲數據。

把 enabled 改為 false，可由控制執行緒在下一次輪詢時停止快取（約 2 秒讀取一次）；正在進行的背景寫入會嘗試完成。重新啟用需下一次遊戲啟動。完整撤回腳本：D:/翻譯/SAO_ZH_GlyphCache_20261030/rollback_glyph_cache.py。腳本保留快取資料，只取消本次 pending 或準備回復上一個已簽章模組，不覆蓋其他人的後續更新。

## 本機最小化載入包

不是直接安裝整個未發布的開發分支。以現用 Compatibility 1.0.1 為基底，只修改 DirectAgent 的 bootstrap helper 安裝後入口及 embedded helper 容器，新增快取所需 class；既有其他 class 和資源逐位元組保留。

本機試用版 SHA-256：a2a3d6e8b21cedebdcbeb1864de4ab03e5ee3ac9eb240a01eb03282662bd5865。
固定 SAO_ZH_Agent_v16.jar、Runtime、規則、資源包與啟動器設定未更動。現用 Compatibility 的 current 描述檔也未直接覆寫；pending 由既有 Kernel 在下次啟動時驗證及晉升。

使用既有受信任簽章完成本機 descriptor。local_only=true，不提供假下載網址，缺少本機 JAR 會回復上一個已簽章版本；此行為已用現有 Kernel 本身驗證。試用 descriptor 有 30 天有效期，到期由既有 previous 機制回復。簽章私鑰未輸出或上傳。

## 已執行驗證

- GlyphPageStoreContract：17 項斷言通過，涵蓋跨開啟讀取、同頁合併、只儲存出現過的字形、字型／寬高隔離、陣列不可變、檔案鎖、checksum 損壞、記憶體與磁碟上限。
- GlyphNativeRasterContract：在真正的 Minecraft class_395 / NativeImage 與 LWJGL FreeType 上測試「田、氾、嶄、龘、鬱」。冷 JVM 51 項、暖 JVM 52 項通過；像素逐位元組等於原生結果。冷 JVM 新生成 5 次；第二個 JVM 從 5 個磁碟頁讀回，測試中生成 0 次、命中 10 次。測試明確等候非同步預熱，不能保證實際遊戲每次啟動都是零 miss。
- 使用試用包真正的 bootstrap helper 再執行同一組 native cold/warm 測試通過，沒有開視窗、沒有 OpenGL 上傳、沒有啟動 Minecraft 入口。
- GlyphCacheServiceContract：5 項斷言通過，包含預設關閉、不重複安裝、bootstrap loader 可見、停止控制；patched stable DirectAgent 通過 JVM 驗證。
- GlyphCacheTrustContract：4 項斷言通過，使用目前已安裝 Kernel 驗證簽章、拒絕損壞簽章、pending 選用，以及本機 trial JAR 缺失時回復舊模組；在隔離目錄進行，未連網。
- 既有 Python：128 + 30 項，153 項執行通過、5 項依環境略過。詞庫與 git diff 驗證通過。

所有上述結果不是遊戲 FPS 數字，也不是正式畫面已修復的宣告。實機收益依實際 TTF 缺字生成比例而定；主因若為世界渲染、GC、其他模組或伺服器，此快取不會直接解決。
