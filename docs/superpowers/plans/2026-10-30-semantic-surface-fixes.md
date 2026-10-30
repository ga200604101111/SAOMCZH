# 語義顯示來源與名稱／數值拆分修復

**Goal:** Capture the supplied HUD/item/system/gathering/NPC samples, translate canonical content and preserve level, distance, quantity, style, icons, player names and signed chat.
**Architecture:** Existing exact dictionaries remain canonical. A bounded source-scoped semantic parser supplies spans, never whole-screen/global replacement. Add exact native hooks before item layout and after mob display caching, and a system-only display callsite. No source model mutation.
**Tech Stack:** Existing Java 17 target, Java 21 client, ASM 9.x, Minecraft 1.21.4 / SAOUI 2.41.2.
**Spec:** User requests in this turn: collect actual text, split names from levels before translating; previous authorization permits merging tested source to main and staging a signed local update.

## Constraints
- Existing isolated worktree; preserve all unrelated files and current running game. No new game instance and no forced restart.
- System messages must not alter player chat, signed message content or network packets; skip ambiguous input.
- Halfwidth colons, same glossary as codex, no number-per-entry dictionary expansion.
- Only narrow approved source methods. MobBarEntry.player=true must bypass even if its name equals a glossary word.
- A changed Runtime API/rule set is staged as a matched next-start update, not injected into old loaded classes.
- No font binaries or raw chats committed; verify new Chinese against existing glyph coverage.

## Tasks
- [x] Inspect screenshots, installed dictionary, current module versions and actual target bytecode. Confirm existing translated tooltip title/quest objective use missed or compound display paths.
- [x] Add failing semantic span tests and target shape tests. Verify failures on baseline.
- [x] Implement scoped exact span resolution for names, level/count/distance suffixes and formatted prefixes; retain original separators and numbers.
- [x] Bind custom tooltip title, player-excluded mob display, NPC-tagged label and game-message UI callsite. Keep original caches/models authoritative.
- [x] Add reviewed missing names/region/system messages to canonical dictionaries; preserve keys/format tokens and update version/hash.
- [x] Run original-jar transformations, isolated VM execution, source glyph/locale tests and existing regressions.
- [x] Merge/push validated source to main, sign matched Runtime/rules and stage for the existing instance's next start; preserve rollback and disclose no visual/FPS proof.

## Review focus
Mixed styles and legacy section formatting; player/system spoofing; source strings with no spaces; negative cached results across dictionary generation; novel glyph coverage.
