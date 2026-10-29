# Modular translation implementation plan
Goal: deploy side-by-side v16 bootstrap, external runtime, compatibility and hot hook scripts; publish existing tested v1.0.5 pack separately.
- [ ] Write failing packaged-JVM contract test before core implementation.
- [ ] Implement minimal signed bootstrap and stable guarded dispatch API.
- [ ] Implement external engine, exact bounded script parser and retransform transaction.
- [ ] Add update cache, signed remote release check, explicit refresh and rollback.
- [ ] Externalize V15 classes/assets without regressing splash bootstrap resources.
- [ ] Run live-JVM same-process attach/new-target/removal/state-preservation/failure tests.
- [ ] Run installed SAOUI native offline integration with v1.0.5 (do not launch game).
- [ ] Publish source/rules and verified artifact releases, verify unauthenticated fetch.
- [ ] Copy v16 distribution beside V15; do not change launcher arguments or running instance.
