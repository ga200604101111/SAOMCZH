## Current client framework — V16 public test build

The small fixed Agent now loads a separately signed runtime and SAO compatibility extension. Signed bounded hook rules can add interception for already-loaded String/Text methods and refresh display projections without replacing the Agent or closing the game. Rollback, local refresh controls and input/scroll preservation tests are included.

Source and installation details: [client-framework](client-framework/README.md). Signed update index: [extensions/v1](extensions/v1).
Latest resource pack: [SAOMCZHPACK v1.0.5](https://github.com/ga200604101111/SAOMCZHPACK/releases/tag/v1.0.5), now publicly downloadable (testing release).

Important: new declarative hooks are hot-updateable; runtime/legacy compatibility binaries are auto-downloaded for the next start. This is not a claim that every original SAO GUI or all missing vocabulary has already been translated. No real Minecraft graphical play-session test has yet been performed for V16.

---

# SAOMCTW — Community SAO Localization
A proposed community-maintained translation table for the Minecraft SAO client integration. Community-maintained translation data and a GitHub cache updater for the local SAO Minecraft translation extension. Repository: https://github.com/ga200604101111/SAOMCZH. Third-party materials require their own redistribution permissions.

## Architecture
- `manifest.json` lists available locales and their version manifests.
- `languages/zh_tw/zh-tw-version.json` pins version, SHA-256 and size.
- `languages/zh_tw/translations.json` contains reviewed exact mappings in `names`, `messages`, `lines` and `items`. Keys may include original French source text **or** the English text after native translation.
- `tools/github_sync.py` retrieves public GitHub raw JSON, validates hash and locale, then writes a local cache. Applying to a V9 instance is opt-in; V9 only supports zh_tw, while future clients may select additional locales.
- `tools/validate.py` checks all indexed manifests and placeholder integrity.
- No server authentication, original assets or game mods are distributed here.

## How updates work
1. Contributors edit the selected language JSON and open PRs.
2. Maintainer runs `python tools/validate.py` and reviews wording.
3. On release, regenerate the language-version file with an updated `version`, `size_bytes`, `sha256`.
4. Clients check the small version file at low frequency, download only when checksum changes, and keep a last-known-good cache on network failure.
5. Original server translations remain the fallback for unknown strings.

## Local staging versus public publishing
`translations.json` currently contains 416 translated entries from the local project. Their legal status is distinct from the original extension code. Confirm source-text redistribution permission and choose a contributor license before accepting outside submissions.

## Locale codes
Minecraft uses `zh_tw`; human-facing BCP-47 is `zh-TW`. The manifest consistently uses Minecraft locale codes. The version filename is `zh-tw-version.json` (not `verison`).

## GitHub repository configuration
Example: a public repository at `OWNER/REPO` is read from
`https://raw.githubusercontent.com/OWNER/REPO/main/manifest.json`.
Run `python tools/github_sync.py --help` for configuration.

## Release assets
Large resource packs should be published under [GitHub Releases](https://github.com/ga200604101111/SAOMCZH/releases), not committed directly to Git. `releases/v1.0.0-assets.json` is a **draft manifest** with exact sizes and hashes; `published=false` means no public release exists yet. Do not treat draft release URLs as live downloads.

## Configure a client
To use the optional Python updater, copy `github-source.example.json` to your local `D:/翻譯/SAO_TW_Dynamic/github-source.json`, set `enabled=true`, and invoke:
```powershell
py tools/github_sync.py --root D:/翻譯
```
By default downloads are cached, not applied to your live V9 dictionary. Set `apply_to_v9=true` only after verifying the repository source. Multi-language download is supported by the updater; **The current V11 agent renders zh_tw; the manifest and updater can cache other locales for future client integration.**

## V11 agent: versioned resource packs and single-copy cache

The next client agent is named `SAO_ZH_Agent_v11.jar` (Minecraft 1.21.4 / Fabric / SAOUI 2.35.0).
It reads the same language `manifest.json` and version JSON.

When `resource_packs.available=true` **and** the client config enables `allow_asset_downloads`, public GitHub Release ZIPs are streamed into `SAO_ZH_Cache/packs/`, SHA-256 checked and validated. While Minecraft is running, newly downloaded packs are marked as **staged**; the old pack is not replaced or deleted. On shutdown, cleanup is attempted. At the next game launch, a verified staged pack becomes active and deletion of old ZIPs is retried if needed.

The currently published version manifest deliberately has `resource_packs.available=false` because the binary Release is still a private draft and third-party asset redistribution rights have not been confirmed. No client can automatically fetch the asset pack until a permitted public Release exists and the manifest is updated.

Do **not** place the pack ZIP or Java agent JAR in Git history. Use GitHub Releases for optional downloadable binaries.

## Community missing-translation reports

V11 records missing *allowlisted UI labels only*, not player messages, to `D:/翻譯/SAO_TW_Dynamic/review/missing_zh_tw.jsonl`.
Share them privately or attach a filtered proposal to an issue:

```powershell
python tools/export_missing.py --root D:/翻譯
```

A clean, deduplicated submission is written under `D:/翻譯/SAO_TW_Dynamic/submissions/`. Nothing is uploaded automatically. Review the resulting file once more before making it public.

### Scope and limitations

Confirmed hook targets: SAOUI equipment stat labels, SAOJob skill descriptions/lore, MobNameSplit names, vanilla bossbar and scoreboard rendering, Actionbar, and unsigned system-chat exact matches. Signed or player-formatted chat is intentionally left unchanged. Unknown words remain in the original language and only safe allowlisted labels are proposed for review. A translated user interface may still require content-specific text layout fixes.

## Dedicated resource repository — V12

The language database and its version live here in SAOMCZH. Resource packs live in
[SAOMCZHPACK](https://github.com/ga200604101111/SAOMCZHPACK) and its dedicated
[pack-version.json](https://github.com/ga200604101111/SAOMCZHPACK/blob/main/pack-version.json).

The V12 Java agent uses separate HTTPS allowlists for the two owners/repos and does not
download unverified or unpublished ZIPs. When a release becomes public and its manifest
is enabled, freshly installed clients can bootstrap a local configuration automatically,
download one full ZIP + one support ZIP, and retain the old pack until Minecraft restarts.
No ZIP asset is committed to Git history. Staged resources are SHA-256 checked.

Currently the pack release remains a draft, so resource_packs.available is false and
a fresh install cannot fetch its ZIP assets yet. This restriction is intentional until
asset redistribution rights have been reviewed.
