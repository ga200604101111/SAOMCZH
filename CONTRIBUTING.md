# Contributing translations

Contributions are welcome after checking the original source material's reuse rules.

- Make translation proposals against `languages/<locale>/translations.json`, not against raw server dumps.
- Preserve `%s`, `%d`, `%%`, `{placeholders}`, Minecraft formatting codes, icons and literal newlines.
- Use Minecraft locale IDs such as `zh_tw`, `en_us`, `ja_jp`.
- For new locales, add a version manifest under `languages/<locale>/` and register it in `manifest.json`.
- After editing a translation file, run `python tools/bump_version.py <locale> <new-semver>`, then `python tools/validate.py` and `python -m unittest discover -s tests -v`.
- Do not upload client logs, authentication data, personal identifiers, server translation snapshots, game JAR files, original game assets or binary resources without permission.
- Source and translations do not yet carry a contributor redistribution license. Choose an appropriate license before accepting outside contributions; the original game's copyrights are separate.
