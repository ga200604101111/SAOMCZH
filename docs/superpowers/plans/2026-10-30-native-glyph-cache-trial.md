# Native glyph cache local trial implementation record

**Goal:** Make the user-selected native TTF disk/page cache usable independently, without deploying unrelated unpublished features.
**Spec:** docs/superpowers/specs/2026-10-30-dynamic-font-cache-design.md
**Boundary:** Reuse native GPU pages. Cache grayscale pixels only. No new game instance, global stable release, replacement font, or launcher flag change.

- [x] Read exact 1.21.4 native font initialization, glyph metrics, and image bake bytecode.
- [x] Add failing disk-page tests; implement bounded memory, asynchronous disk pages, integrity, lock, and eviction.
- [x] Instrument only native TTF constructor state, close, and the dedicated baker raster call site.
- [x] Verify real FreeType pixels, cold and separate warm JVM, bootstrap loader, transform mismatch, and disable behavior.
- [x] Build glyph-only trial from the currently installed compatibility binary; preserve all other existing entries byte-for-byte.
- [x] Sign using the preexisting local trust key; validate with the installed Kernel and test missing-local-binary recovery.
- [x] Stage only pending compatibility and opt-in cache configuration; retain baseline and rollback utility.
- [ ] User's original game instance: verify actual load, resource reloads, hit/miss counts, and frame-time benefit. No in-game performance claim yet.

Ruling: Memory misses may regenerate rather than block on disk — preserves render responsiveness; first startup is not guaranteed to avoid rasterization.
Ruling: Variable-font axes are not inferred — variable faces keep native behavior until a complete axes identity is supported.
Ruling: Trial uses existing 1.0.1 compatibility as immutable baseline — prevents cache deployment from accidentally installing unfinished glyph/translation changes.
