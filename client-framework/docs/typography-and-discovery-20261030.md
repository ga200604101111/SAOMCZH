# 地區發現通知、半形冒號空格與中文換行

日期: 2026-10-30。配套候選: Runtime 1.7.0、規則 1.11.0 / sequence 115 / 129 hooks、詞庫 1.0.18 / 8,769 條。Agent v17 與 Compatibility 1.1.0 不變。

## HANAKA 不是整張圖片

從實際 SAOUI 2.41.2 的 DiscoveryOverlayRenderer 核對，上方城徽來自 assets/saoui/textures/ui/discovery/icon.png；已取出圖片確認與截圖中的城堡及葉環相符。白色 HANAKA 則由 DiscoveryPayload.title() 轉成 Text，存入 titleText，繪製時先 TextUtil.width 再 TextUtil.drawText。它不是圖片內的英文字，也不是羅盤的城市標題。

新增 worlds-locations:discovery-title，只在 DiscoveryOverlayRenderer.onHudRender 讀取 titleText 的一個位置投影，先翻譯後量測與繪製；重用既有 names/lines 的 HANAKA → 哈納卡，不複製一套地名，不改原封包、來源 Text 或城徽圖片。此來源同樣使用原有的 styled-name 投影與語言檢查。

總 Hook 數從 128 增至 129，整體編譯／Runtime 上限從 128 調成 160，仍保留明確上限。單模組上限 128、腳本大小、字串長度、欄位／來源限制與快取上限均不放寬。新規則只配套新 Runtime 在下次啟動載入，不交給仍使用舊上限的執行中 Runtime。

## 標籤: 數值

235 處翻譯輸出統一中文字欄位冒號：移除冒號前多餘空白，使用 ASCII 冒號，後面保留一個半形空格，例如「攻擊傷害: 7」「吸血: +5%」。原文比對鍵、數值、格式碼及其他標點不更動。固定值和翻譯規則共用同一規格；padded_exact 的來源級規格仍維持不含首尾空白，真正顯示時再完成欄位格式化。

FieldTypography 只在已核准物品內容輸出處執行，不對全域聊天或玩家名稱做標點替換；必須是漢字欄位後的分隔符才會處理。12:30、1:2、網址、minecraft:stone、Lv. 1+ 不會被改寫。字串內的顏色控制碼原順序保留。

display_typography.py 用於詞庫與資源包輸出，build_glyph_contract_pack.py 也接入同一規則，避免下次重建又產生舊格式。本輪資源包只修改 39 個語言檔、454 個值；其他 219 個資產逐項驗證內容位元組相同，沒有改動圖片或字型資料；saoui.node.level 與 saoui.mob.level 原樣保留。

## 自動換行的保守改善

沿用 CjkWrap 的原生 renderer 字寬度量，不以固定字數猜測行寬、不縮小字體，也不修改 tooltip 欄位或物品數值。

- 可完整放進一行的數值／單位、百分比、材料比例與 Lv. 等級標示，優先保持在一起。
- 在中文段落最後兩行都能容納的前提下，優先選擇附近適合的逗號／句末斷點。
- 末行只剩很短內容時，適度把前行末尾移到下一行；不增加行數，不跨越原本段落。
- 避免把常用句尾標點放在行首、或把左括號放在行尾；原有字素／代理對保護不變。
- 原本的換行、空行、各屬性列均保留；不會因為這次換行改善就把未知 lore 列合併。描述合併仍須通過上一版的已審核完整列序對照。

空間極窄、單個 token 超過整欄、達到顯示行數上限或特殊字型度量不成立時，仍受既有保護與 fail-open 限制，不承諾任何寬度下都能得到理想排版。原有的世代與寬度快取繼續使用，沒有在每幀做全詞庫掃描。

本輪後補的分行圖片未在可讀附件中取得，所以以上是針對已確認換行邏輯的通用改善，不聲稱已對該張未讀圖片做逐項畫面驗收。

## 驗證範圍

TypographyContract 先重現單字尾行與錯誤冒號格式，再驗證保留內容、像素寬度、適當句讀、數值單位、硬換行、色碼和不相關字串。NativeDiscoveryContract 轉換真實 SAOUI 類別，確認投影位於 titleText 讀取之後、字寬量測與繪製之前，城徽讀取不變，並驗證 names/lines 共用。

另執行原有 LayoutUnit、中文描述、混色系統通知、玩家排除、原生 Text/Style、完整詞庫／Hook 回歸及 Agent v17 隔離 JVM 啟動測試。真正遊戲不強制關閉、不另開實例；更新只放入待啟動區，保留備份，實際畫面仍需原實例重啟後驗收。
