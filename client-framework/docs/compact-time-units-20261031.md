# Compact native time labels for profile and HUD (2026-10-31)

## User-facing contract

For the Sword Art Online profile's **Inspect / Playtime** row, display the original English header and the original compact duration units. Do not translate `h`, `hr`, `m`, `min`, `s`, or `d` to wide Chinese words inside numerical status elements.

The original SAOUI 2.41.2 implementation uses `EquipmentPanel.duration(long)`, with the resource keys `saoui.equipment.duration.hours` and `saoui.equipment.duration.minutes`. The original upstream English strings are `%sh %s` and `%s min`; the profile label is `saoui.equipment.playtime = Playtime:` in `triggersmod`. Existing Chinese resource-pack strings converted these to `小時`, `分鐘` and `遊玩時間`, making the fixed UI column wider.

After this patch:

| Native key | Preserved compact display |
| --- | --- |
| `saoui.equipment.playtime` | `Playtime:` |
| `saoui.equipment.duration.hours` | `%sh %s` |
| `saoui.equipment.duration.minutes` | `%s min` |
| `friends.ago.hours` | `%s h` |
| `friends.ago.minutes` | `%s min` |
| `bundle.timer_text` | `剩餘時間: {days}d {hours}h {minutes}m` |

These six keys are applied only to `assets/triggersmod/lang/{zh_tw,zh_hk}.json` and `assets/saoui/lang/{zh_tw,zh_hk}.json` by `client-framework/tools/build_glyph_contract_pack.py` when producing the Agent-managed resource pack. No other Minecraft languages, text components, device names or numerical server values are modified.

The shared `zh_tw` glossary also restores only a reviewed set of **compact UI duration sources**, such as `1min`, `3min` and `Single use - cooldown of 1min`. Their Chinese labels stay translated, but the numeric suffix remains `1min` or `3min` without adding `分鐘`. Whole narrative or quest descriptions remain natural Chinese: we do not blindly replace the words 「時間」、「小時」 or 「分鐘」 in prose.

## Unsafe legacy rule removed

The former `worlds-locations:transfer-duration` scoped overlay used three unbounded literal operations replacing each individual letter `s`, `m`, and `h` with 「秒」、「分鐘」、「小時」. This also mangled unrelated English words (for example `Class: assassin`). Those replacements have been removed completely. The duration hook still has its explicitly reviewed exact glossary, but does not rewrite arbitrary letters or time-unit fragments.

## Test and deployment boundary

- `test_compact_time_units.py` verifies native key values, approved glossary keys, the lack of global timer operations, and unchanged storyline text.
- `test_rule_sets.py` and `test_central_locale.py` compare all existing hooks and allow only precisely reviewed compact time exceptions.
- `test_glyph_pack_contract.py` verifies original icons, font providers and all glyph atlases. The new candidate keeps every font atlas and all non-locale resources identical to the verified NPC candidate.
- Resource-pack changes require a resource reload / next normal game startup. The Java Agent v17, original SAOUI / Zap Dialogue client, server statistics and network messages are not modified.
- Offline tests are not a live screenshot or a guarantee that every third-party UI uses these six native keys.
