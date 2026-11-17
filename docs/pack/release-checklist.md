# Release checklist — v0.1

Date: 2026-11-16, updated 2026-11-17. Status: **not releasable yet**. Automated checks and
partial in-game acceptance passed in the source workspace; this migrated repository still needs
an independent clean-clone build. Release also needs Boss/clear validation, longer stability
testing, and locked CurseForge IDs.

## Automated

- [ ] GitHub Actions clean-clone workflow passes on the standalone repository.
- [x] `python3 tools/test_gen_rooms.py` — OK.
- [x] Pack tests passed in the source workspace; rerun independently in this repository before release.
- [x] `python3 tools/test_pack_assets.py` — OK.
- [x] `python3 tools/gen_rooms.py --check` — OK.
- [x] `python3 tools/build_pack.py --check` — zero errors; 142 unresolved CurseForge id warnings remain across 76 entries (release blocker).
- [ ] `python3 tools/build_pack.py --zip` — **expected to fail** until the CurseForge
      ids are locked; the builder now exits non-zero instead of shipping an empty manifest.
- [x] Pack source, lock file and `.mrpack` export were prepared in the source workspace; rebuild independently from this checkout.
- [ ] This standalone checkout builds the Inf core submodule and expedition mod; source-workspace results are kept as historical evidence in `integration-log.md`.

## Manual acceptance (design §12.2)

- [x] Full-pack client joined an existing world after the Pufferfish curve fix; no login OOM.
- [x] Existing test save → hub → direct right-click on the Expedition Altar starts a run and places the start room.
- [x] Map opens with M; reachable nodes advance; the rendered Act 1 map shows 8 floors.
- [x] Rest room placed, exit interaction completed the room, and the next node unlocked.
- [x] Encounter room placed and spawned a combat wave; solo death failed the run.
- [x] Death/failure path returned to the hub, cleared the run inventory, and reset to IDLE.
- [ ] Boss room completes Act 1; clears +1; next map differs.
- [ ] Recall scroll: only the user leaves; if the party is then empty the run ends.
- [ ] Server restart/relog while `RUNNING` behaves per `act1-balance.md`.
- [ ] Forge table works in start/forge rooms; forged weapon fights.
- [ ] Shop purchase and rest healing complete successfully.
- [ ] Quest guide loads; 12 quests complete; rewards auto-claim in-run.
- [ ] zh_cn/en_us text has no missing keys (quest SNBT is zh_cn-only in v0.1).
- [x] Full client launches with all cached required mods; no missing dependency crash.
- [ ] 30-minute stability run.
- [ ] Two-player LAN.

## Known blockers

- `pack/mods.yaml` has unresolved CurseForge `project_id`/`file_id` for most entries;
  `--zip` is gated. The `.mrpack` path is built and is the current installable artifact.
- `pack/cache/` contains all 76 third-party jars; the local export has 78 jars including Inf and Inf Expedition.
- KubeJS server scripts and the 12-task FTB Quests chapter loaded in the full-pack runtime. Easy NPC presets and the optional trading setup still need configuration.
