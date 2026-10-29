# Layout/runtime 1.1 self-review

No independent reviewer/subagent was available. Source, packaged fixture results and native integration were reviewed directly.

## Confirmed boundaries
- Installed V16 kernel is byte-identical; only external runtime/API and signed functional rules change.
- New schema 2 rules use a separate v2 channel; existing v1 rule channel stays unchanged.
- Text layout uses renderer measurements, bounded input, line count and measurement budget, an LRU limit and weak screen references. Warm-layout tests check measurement count, not guessed FPS.
- Only declared instance Map cache fields and mutable String cache keys are supported. Bytecode validation rejects absent or mismatched fields before committing a generation.
- Removal retains lightweight cleanup entry points so an already-open screen can drop stale translated cache data without reopening. Removed guards do not translate and remain bounded to 128 definitions.
- Refresh occurs on the instrumented render thread, not the network worker. No container close, packet sending, inventory editing, or GUI reinitialization is added.
- Runtime replacements still require the next JVM bootstrap; this release does not claim hot replacement of arbitrary runtime binaries.

## Reproductions and corrections
- Old runtime rejects new typed capabilities; pure layout class initially absent. Tests witnessed these failures.
- Zero-width animation frames initially counted as hook failures and disabled subsequent valid wraps. Added a regression and moved transient input checks before the failure counter; regression passed.
- Native isolated harness initially saw an environment transition from unavailable to WORK_SANS when its font setting initialized without a Minecraft singleton. The second refresh was legitimate. Updated the test to verify stable environment does not repeatedly clear caches; no production behavior was hidden or bypassed.
- Original persistence test harness needed LiveContract compiled as a dependency; fixed runner setup and reran all original signing/persistence/watch tests.

## Evidence
32 Python composition tests; 27 pure width/Unicode/bounds checks; 27 same-JVM refresh/wrap/dictionary/rollback checks; 12 checks using real SAOUI cache fields plus full legacy native integration. 41 bytecode hooks across 11 native target classes validated. Original safety, persistence, watcher and no-command-replay suites pass.

## Remaining limitations
No live Minecraft graphical/GL validation. Lower auction action subtitles remain single-line; HUD static caches, World Boss panels and comprehensive missing translations remain separate work. Some GUI implementations may clamp scrolling after layout height changes. Retired refresh guards remain installed until process exit. Trusted signed cache field selection is not a general sandbox against a malicious trusted maintainer. Native JVM/GPU crashes cannot be guaranteed recoverable.
No private signing key, captured player/server text, original mod bytecode or runtime game logs are included in public artifacts.
