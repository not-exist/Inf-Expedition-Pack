# External mod integration status — 2026-11-16

All Tier S + A + B mods and their resolved dependencies were added to `pack/mods.yaml`
(76 entries, 75 required). All 76 jars are cached in `pack/cache`; the full 76-mod client and
a 59-jar dedicated-server subset both launch. Versions below were resolved on Modrinth/ForgeCDN
for 1.21.1 NeoForge. CurseForge project/file ids remain pending for most entries, so the
CF `--zip` path stays gated; the `.mrpack` is the installable format.

| Area | Mods | Integration | Status |
|---|---|---|---|
| Relics | Artifacts 13.2.5 | `loot_artifacts.js` adds one `artifacts:artifacts` pool to elite/boss/event chests with an empty-entry weight (≈1/3 elite/event, 1/2 boss) | wired; runtime pending |
| NPC | Easy NPC 7.19.0 + Core/Config UI, FTB compat | setup guide; KubeJS chest fallback retained | mods listed; preset manual |
| Meta | Pufferfish's Skills 0.19.2, Keystone 1.1.0, Puffish Attributes 0.8.3 | `kubejs/data/.../puffish_skills/difficulty` 4-node tree; `boss.js` grants a point on clear; start-kit flags | data wired; server scripts 0 errors; player flow pending |
| Act 1 mobs | Mobs of Mythology 3.1.0, Illager Invasion 21.1.6 | new `battle/illager_warband`, `battle/mythic_grove`, `elite/mutant` room variants reuse existing NBT | wired; entity ids from upstream registry |
| Act 2+ bosses | Bosses of Mass Destruction 1.3.3 | `boss/void_blossom`, `boss/gauntlet` room variants, `min_act: 2` | data ready; not in v0.1 act 1 |
| Elite | Crimson Steve's Mutant Mobs 1.6.0 | `elite/mutant` wave | wired |
| Boss UI | Boss Checklist 5.3.2, Enhanced Boss Bars 1.1.3 | client-only install | listed |
| Loot scripting | LootJS 1.21.1-3.7.0, MoreJS 0.16.0 | LootJS used by Artifacts; MoreJS reserved | wired/reserved |
| Decor | Dusty 2.3.2, Fetzi 1.8.2.1, XK 1.1.10 | installed; room palettes not generated yet | listed |
| Dungeon event | Dimensional Dungeons 203 | no room strategy/bridge yet | listed; M3 |
| Worldgen | YUNG's Better Dungeons 5.1.4, YUNG's API 5.1.9, When Dungeons Arise 2.1.68 | installed; void run does not use overworld worldgen | listed; M3 |
| Long relics | L2 Artifacts 3.0.7 | installed; no loot injection yet | listed; M3 |

## Blocked

- Full pack launch/manual acceptance still needs third-party jars + a launcher.
- Easy NPC presets/trades require an in-game editor session.
- Decor palettes and Dimensional Dungeons event rooms need runtime API/schema checks.
- Pufferfish category/command id must be verified in a real pack.
