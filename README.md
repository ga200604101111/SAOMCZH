<p align="center">
  <strong>English</strong> | <a href="./README.zh-TW.md">繁體中文</a> | <a href="./README.zh-CN.md">簡體中文</a>
</p>

# SAOMCZH

**Community-maintained Traditional Chinese localization for the Minecraft Sword Art Online experience.**

SAOMCZH maintains reviewed translations, scoped client-side text hooks, typography fixes, and a signed Java Agent update architecture. Its goal is to translate game content without changing original server data, player messages, or third-party mod JARs.

> **Language note:** These README pages are available in English, Traditional Chinese and Simplified Chinese. The game's currently maintained translation locale is **`zh_tw`**. A Simplified Chinese README does **not** imply that a `zh_cn` game dictionary is available.

## Current development snapshot

The following describes the tested source and local integration as of **October 31, 2026**; it is **not** a promise that the matching binaries are published as public GitHub Releases.

| Component | Status |
| --- | --- |
| Fixed Java Agent | `SAO_ZH_Agent_v17.jar` |
| External Runtime | 1.9.0 — signed and locally staged for the next launch |
| Hook rules | 1.14.0 — 136 scoped hooks |
| Traditional Chinese dictionary | 1.0.22 — 8,876 entries |
| Compatibility module | 1.1.0 |
| Development client | Minecraft 1.21.4 / Fabric; tested against specific installed SAO mods |

The Runtime and hook rules have passed bytecode checks and an isolated Java Agent startup test. **Not every screen, font, or MCEF voice feature has been verified in a live Minecraft play session.**

## Features

- **Shared translation glossary:** `names`, `items`, `lines` and `messages` can reuse reviewed translations across supported mod interfaces, with context-specific precedence and safeguards.
- **Scoped text translation:** item names and descriptions, creature and NPC labels, quests, skills, actionbar/system notifications, locations, world text, and selected HUD elements.
- **Typography and glyph handling:** preserve colors, icons, original item counts, `Lv.` indicators, and numeric values; measure Chinese text before layout when the relevant hook supports it.
- **Missing-translation collection:** record approved unknown game text for later review without indiscriminately collecting player chat.
- **Modular updates:** verified dictionary changes can be distributed separately from signed Runtime and interception-rule updates. Certain code or resource changes need a restart.
- **Sound of Aincrad MCEF interface:** an allowlisted **Java Agent hook**, not an extra Fabric mod, localizes the embedded web UI while keeping its audio, WebRTC, device names, and input behavior untouched.

Coverage is deliberately limited to known, audited text sources. Unknown or unsupported text may remain untranslated.

## Repository structure

| Path | Purpose |
| --- | --- |
| [`languages/zh_tw/translations.json`](languages/zh_tw/translations.json) | Reviewed Traditional Chinese entries in four sections |
| [`languages/zh_tw/zh-tw-version.json`](languages/zh_tw/zh-tw-version.json) | Dictionary version, size and SHA-256 |
| [`languages/zh_tw/hook_text_rules.json`](languages/zh_tw/hook_text_rules.json) | Locale-specific, bounded render-text rules |
| [`languages/locales.json`](languages/locales.json) | Registered game translation locales |
| [`extensions/modules/`](extensions/modules/README.md) | Scoped, modular hook definitions and dependencies |
| [`client-framework/`](client-framework/README.md) | Agent, Runtime, Compatibility sources and technical notes |
| [`tools/validate.py`](tools/validate.py) | Manifest, integrity and placeholder validation |
| [`tools/github_sync.py`](tools/github_sync.py) | Optional GitHub dictionary-cache synchronization |
| [`tools/export_missing.py`](tools/export_missing.py) | Export reviewed candidates from locally collected missing text |

The root [`manifest.json`](manifest.json) describes the project and locale manifests. **Resource packs are maintained separately** in [SAOMCZHPACK](https://github.com/ga200604101111/SAOMCZHPACK).

## How translation updates work

1. Add or revise entries using the **exact original source keys**. Keep variable placeholders, color codes, icons and meaningful whitespace intact.
2. Update the dictionary version and its recorded `size_bytes` and `sha256`.
3. Run the validation commands below and review the output before publishing.
4. The client-side updater can fetch and verify a changed dictionary. Already-rendered UI or cached text may need to be reopened or refreshed.
5. Signed Runtime or Compatibility changes are validated separately and generally activate on the next game start. A dictionary update alone does **not** require rebuilding the fixed Java Agent.

Signed metadata and SHA-256 checks protect updates. Do not use an unsigned development Runtime as if it were a trusted release.

## Validation for contributors

From the repository root:

```bash
python tools/validate.py
python -m unittest discover -s client-framework/tests -p "test_*.py"
python -m unittest discover -s tests -p "test_*.py"
```

For custom development builds, see [`client-framework/tools/compile_sources.py`](client-framework/tools/compile_sources.py). Source compilation produces unsigned artifacts; a separate trusted signing and verification process is required before deployment.

Missing-translation records can be exported with:

```bash
python tools/export_missing.py --help
```

Review exported records before sharing. Local diagnostics may include contextual information and are not automatically suitable for public commits.

## Installation and operational boundaries

This repository is **not** a one-click mod installer. In the supported setup, the existing fixed Agent is supplied through the Java launch argument `-javaagent:...`, while external modules and rules follow their own signed update paths. **Do not place the Java Agent in the Minecraft `mods` folder or install a separate Fabric mod for Sound of Aincrad localization.**

Keep the original SAO, Sound of Aincrad, and MCEF mods in place. Preserve known-good backups, and restart the original game instance when a new Runtime or resource pack must be loaded.

## More technical documentation

- [Agent and Runtime architecture](client-framework/README.md)
- [Module catalog](extensions/modules/README.md)
- [World text translation pipeline](client-framework/docs/world-text-pipeline-20261029.md)
- [Shared item names, named containers and NPC captures](client-framework/docs/cross-surface-translation-20261031.md)
- [NPC dialogue and response options](client-framework/docs/npc-dialogue-translation-20261031.md)
- [Compact time units and Playtime labels](client-framework/docs/compact-time-units-20261031.md)
- [Chinese typography and location HUD](client-framework/docs/typography-and-discovery-20261030.md)
- [Java Agent-only MCEF web UI localization](client-framework/docs/zapaudio-agent-webui-20261031.md)
- [Resource pack repository](https://github.com/ga200604101111/SAOMCZHPACK)
- [GitHub Releases](https://github.com/ga200604101111/SAOMCZH/releases)

## Community and third-party rights

SAOMCZH is a community localization project, not an official release of Sword Art Online or the underlying Minecraft mods. Third-party content and assets remain subject to their respective licenses and redistribution permissions. Do not commit server credentials, private signing keys, or unlicensed original mod assets to this repository.
