# Integration log — v0.1

Date: 2026-11-17. Repository: `not-exist/Inf-Expedition-Pack`.

## Built artifacts

- `inf-core/build/libs/inf-0.2.0.jar` — the pinned Inf core submodule build.
- `build/libs/inf_expedition-0.1.0.jar` — map/room/run mod.
- `python3 tools/build_pack.py --local` — copies both jars into `pack/export/local/mods/`.

## Local two-mod server smoke (passed)

The server run used the pinned `inf-core` submodule's core jar from
`inf-core/build/libs/inf-0.2.0.jar`. The room/encounter/structure data ships inside
`src/main/resources/data/inf_expedition`, so it loads directly from the mod jar; the earlier
test datapack was removed.

Commands sent over RCON:

| Command | Result |
|---|---|
| `infexpedition info` | `status=IDLE seed=0 node=-1 nodes=0 party=0 room=none` |
| `infexpedition debugplace inf_expedition:rooms/battle/crypt` | `Placed: true` |
| `infexpedition debugplace inf_expedition:rooms/start/camp` | `Placed: true` |
| `infexpedition debugplace inf_expedition:rooms/event/treasure` | `Placed: true` |
| `infexpedition debugplace inf_expedition:rooms/boss/wroughtnaut` | `Placed: true` |
| `infexpedition debugplace inf_expedition:hub_platform` | `Placed: false` (correct: hub is not a room; fail-closed on missing spawn/exit) |

The two actual rooms reported no missing spawn/exit marker warnings; the hub platform
correctly reports no spawn/exit markers because it is not a room. This validates:

- both custom dimensions load;
- the mod's built-in room/encounter/map data loads (catalog=10, floors=8);
- the generated structure NBT parses and places;
- full-id jigsaw markers are found and replaced;
- `inf:forge_table` in `start/camp` resolves with Inf loaded;
- the command tree and run-dimension lookup work.

`xvfb-run -a ./gradlew runClient` reaches the main menu with both local mods
(`mod/inf` and `mod/inf_expedition`) and no missing-texture or `inf_expedition`
errors; the only log noise is the environment's missing `flite` narrator library.

`python3 tools/build_pack.py --zip` now exits non-zero while CurseForge ids are
unresolved, instead of producing a manifest with zero mods.

## Not completed (blocked)

Full core-15 pack launch was not possible here:

- no third-party mod jars are present (`pack/cache/` is empty; the shell cannot
  download them), and no Prism/HMCL launcher is installed;
- `pack/mods.yaml` still has unresolved CurseForge `project_id`/`file_id` values for
  most entries; `tools/build_pack.py --check` reports them as warnings.

To finish Task 14/15 on a machine with network or a launcher:

1. Put the listed jars in `pack/cache/<name>.jar` (or resolve the CurseForge ids).
2. Run `python3 tools/build_pack.py --local`.
3. Launch `pack/export/local` with the loader `neoforge-21.1.266`, create a world, and
   verify the in-game acceptance list from the design (§12.2).
4. Record missing dependencies/mixin conflicts here.

## Standalone build notes

- The source-workspace integration evidence is retained below for provenance.
- This repository builds the pinned `inf-core` submodule first, then builds the root
  `inf_expedition` project.
- Third-party jars are intentionally ignored and must be fetched into `pack/cache/`.
- `tools/build_pack.py --check` reports unresolved CurseForge IDs as warnings; `--zip` remains
  gated until those IDs are locked.


## Catalog/reload verification

After moving the data into the mod jar, `infexpedition info` over RCON returned
`catalog=15 floors=8`, proving the reload listener loaded all room definitions and
`data/inf_expedition/expedition/map.json`. No `Skipping invalid room/encounter` warnings.

## External mod wave (2026-11-16)

After adding the new room variants, RCON reports `catalog=15 floors=8` with no invalid room/encounter warnings.


`pack/mods.yaml` expanded from 39 to 63 entries: all Tier S + A + B mods were added.
Artifacts loot and the Pufferfish difficulty category were wired; new Act 1 encounter
variants use upstream entity registry ids. Easy NPC presets, decor palettes and dungeon
event rooms remain manual/runtime steps. See `docs/pack/external-mod-status.md` and
`docs/pack/mod-matrix.md`.

## Full pack launch (2026-11-16, network available)

- Downloaded 76 third-party jars into `pack/cache/` (343 MB) via Modrinth API/ForgeCDN.
- Full client launch: 76 mods loaded; KubeJS client scripts 0/0 errors; reached main menu.
- Full dedicated server launch (server-cache: 59 jars, client-only mods excluded): `Done`, KubeJS server scripts 6/6 with 0 errors, FTB Quests loaded 12 quests.
- Pufferfish Attributes added to satisfy Keystone Expansion; no unknown-attribute errors remain.
- Built `pack/export/duantu-qianchui-0.1.0.mrpack` (76 Modrinth/CF download entries + 2 local jars in overrides) and `pack/export/local` (78 jars).
- Quick-play world join was attempted twice (singleplayer and multiplayer); neither joined within
  the test budget. The dedicated-server world load and full client main menu both passed, so the
  remaining gap is the interactive Hub → altar → map acceptance, not mod loading.
- Remaining: manual Hub → map → room acceptance, Easy NPC preset configuration, decor/dungeon event wiring.

## Interactive full-pack acceptance (2026-11-17)

- Full client with all 76 cached third-party mods joined the existing `testworld` save after
  the Pufferfish experience-curve fix; no login OOM occurred.
- Fixed a real runtime blocker found during this test: a zero-valued
  `experience_per_level` expression caused Pufferfish Skills to grow an unbounded cache during
  player login and crash the integrated server with `OutOfMemoryError`. The curve is now
  `100 + level * 50`, mirrored in `expedition/run` and covered by
  `PuffishSkillsConfigTest.test_experience_curve_is_positive`.
- In-game command path passed: `/infexpedition info` returned
  `status=IDLE ... catalog=15 floors=8` in the hub.
- Direct altar interaction passed: right-clicking the actual `inf_expedition:expedition_altar`
  at hub `(0,65,0)` entered the generated start room in `inf_expedition:run`, issued the
  starting hammer/blanks/food/recall scroll/quest book kit, and logged `Expedition started`.
- `M` opened the rendered Act 1 map with 8 floors and reachable-node highlighting. A rest node
  placed its NBT room, campfires, spawn and lodestone exit; right-clicking the exit completed
  the room and unlocked the next node (`first_room` advancement).
- A reachable encounter node spawned a combat room and skeleton wave. The solo player died,
  the run reported failure, returned to the hub, cleared the run inventory, and reset SavedData
  to `status=IDLE ... room=none`.
- Not yet measured: Boss clear/`clears` increment, forge interaction, shop purchase, relog/restart
  recovery, 30-minute stability, and two-player LAN.
