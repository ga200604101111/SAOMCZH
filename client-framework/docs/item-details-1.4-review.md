# Item details release self-review
No independent review agent was available. Checked actual native class contracts, source diff, fixtures and privacy boundary directly.
- New scalar hooks target Info.name, Layout.upperName and daily reward names only. Original objects and source lists are not modified.
- Acquisition prefix replacements are bounded and retain prices; unknown merchant names are not guessed.
- Canonical phrases and exact bilingual aliases were reviewed from permitted game records. No player messages, raw capture files, fonts or keys are published. Tests use invented players.
- Identified and tested importer omission of legacy §x gradient marker. Fix changes offline alias matching only, not GPU or font code.
- Strict formatter mismatch entries are deferred rather than loosening validation.
- Existing runtime cannot safely refresh a static cached string-array popup; this release requires reopening that popup for source/description layout. No arbitrary renderer-wide chat hook added.
- Cross-component gradient Text composition is not solved by alias expansion and remains explicitly documented.
- Native tests pass; last observed real game not running, so no claim of live or graphical verification.
