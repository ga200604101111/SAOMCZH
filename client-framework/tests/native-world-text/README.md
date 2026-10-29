# 世界浮空文字：可重現原生位元碼測試

本目錄只包含測試 Java 原始碼與最小 Minecraft Text/Style/快取假物件，不包含遊戲或第三方模組 JAR。測試須由使用者自行提供已安裝的 Minecraft 1.21.4 intermediary JAR 與 SAOJob 1.1.3 JAR。

以 Windows 命令列為例，從本專案根目錄執行：

```powershell
py client-framework/tests/native-world-text/run_probe.py --jdk "D:\Program Files\Zulu\zulu-26" --asm "<ASM 9.x JAR 路徑>" --minecraft-jar "<Minecraft 1.21.4 intermediary JAR 路徑>" --saojob-jar "<saojob-1.1.3.jar 路徑>" --java "<Java 21 java.exe 路徑>"
```

測試會在暫存目錄編譯 Runtime（Java 17 target）與隔離假物件，執行 `WorldTextNativeProbe`。測試會檢查真實位元碼中 TextDisplay 的翻譯早於原生分行、分行快取版本失效、SAOJob Hologram、ArmorStand 專用客戶端 RenderState、Style/文字樹不變性、限制性的傷害統計格式與未知字串 fail-open。

預期成功標記：`WORLD_TEXT_RENDER_TESTS_ALL_PASS`。**不會啟動 Minecraft、不會修改遊戲檔案，也不代表正式畫面已實測。**

開發候選程式碼與規則必須經正式簽章、相容性及遊戲內測試後才可發布；請勿直接安裝未簽章的 Runtime 預覽版。
