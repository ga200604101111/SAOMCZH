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
`translations.json` currently contains 373 translated entries from the local project. Their legal status is distinct from the original extension code. Confirm source-text redistribution permission and choose a contributor license before accepting outside submissions.

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
By default downloads are cached, not applied to your live V9 dictionary. Set `apply_to_v9=true` only after verifying the repository source. Multi-language download is supported by the updater; **V9 only renders zh_tw** until a future client version adds locale-aware loading.
