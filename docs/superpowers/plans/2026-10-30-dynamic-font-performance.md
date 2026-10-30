# Dynamic font cache and performance diagnostics implementation plan

**Goal:** User-selected full-font on-demand rendering and persistent cache, plus an independently switchable client performance diagnostic module; game optimization follows evidence, not guessed GC/render changes.
**Architecture:** Reuse Minecraft 1.21.4 FreeType lazy glyph baking. Performance observation is optional, with bounded in-memory timing and an asynchronous report writer. Optional short JFR capture adds Java stack and pause data; neither is an optimizer.
**Tech Stack:** Existing Java 17 target / Java 21 client, ASM 9.x, java.management, optional jdk.jfr, existing compatibility bootstrap bridge.
**Spec:** User decisions in this conversation: full font, disk/page cache, performance analysis first, game optimization afterward.

## Constraints
- Work only in the existing isolated development worktree. Do not start another game instance, replace signed releases or change JVM flags.
- No disk/network/reflection in timing hooks. No collection of chats, usernames, coordinates or command lines by summary reports.
- Frame duration is wall time, not GPU time; remote server TPS and GPU utilization are unavailable unless actually measured.
- Disabled by default. Bounded duration and report retention. Raw optional JFR stays local and may include file paths.
- Known fonts are 21.6 MB per weight; unrendered characters still share font metadata. No zero-cost or instantaneous rendering promise.

## Implementation tasks
- [x] Write failing bounded-timing and native-transform tests. Validate disabled state, nesting-safe timing scope, dropped/abandoned samples, percentile math, no behavior changes.
- [x] Add PerformanceBridge (bootstrap), PerformanceTransformer and PerformanceService (system loader). Integrate opt-in startup and package bridge correctly.
- [x] Verify exact installed 1.21.4 method descriptors and VM verification on isolated transformed fixtures. Run full regression suites.
- [x] Record the actual existing lazy FreeType and atlas behavior; design persistent cache keys/invalidation before connecting native bitmap reads.
- [x] Keep the optimization module unimplemented until actual recordings identify a bottleneck; preserve complete rollback and no speculative claims.

## Review focus
- Exceptions must not change original game behavior or contaminate the next timing sample.
- High FPS must not grow memory or perform per-frame file writes.
- Profile-disabled mode must install no timing transformer.
- Report threads must never invoke OpenGL or modify render state.
- JFR availability/failure must not prevent the game or light profiling from running.

## Scope outcome
Performance diagnostics are implemented and offline-tested. Native TTF lazy rendering was investigated and the disk/page-cache design recorded; actual disk/page-cache integration remains unimplemented. No optimizer or in-game benchmark is claimed.
