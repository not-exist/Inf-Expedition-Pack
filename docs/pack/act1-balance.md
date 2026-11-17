# Act 1 balance — v0.1 first pass

Date: 2026-11-16, updated 2026-11-17. Status: **first-pass assumptions; partial in-game measurement complete**.
The full cached pack now joins a save and the first death path is verified; boss and timing
measurements remain pending.


## Inputs from Inf

From `docs/balance-report.md` (100k trials, seed 42, defaults):

- average strikes per blade: 22.0;
- legendary rate: 5.19%;
- rare or better: 42.28%;
- fine or better: 68.68%;
- one hammer holds 128 strikes;
- the starting kit gives two random blanks and one hammer.

This means a normal run can support roughly 5-7 careful blades per hammer; Act 1 should
not require more than one or two good blades.

## Act 1 encounter assumptions

- Act scaling is disabled in v0.1 (`act = 1`, multiplier 1.0).
- Single-player party scaling is 1.0.
- Battle rooms: 2-3 waves of 3-5 mobs.
- Elite: one `legendary_monsters:ambusher` plus two zombies (fallback: ravager/witch).
- Boss: one `mowziesmobs:ferrous_wroughtnaut` (fallback: `legendary_monsters:frostbitten_golem`
  or a vanilla ravager if the modded id fails to resolve).
- A fine (240) or rare (450) blade should be enough for the boss; legendary (1000) is
  not required.

## Loot assumptions

| Source | Expected value |
|---|---|
| `battle_common` | 4-8 emeralds, iron, food, one random blank |
| `elite` | 8-12 emeralds, one hammer, golden apple, two random blanks |
| `boss` | hammer, two blanks, 16-24 emeralds, diamonds |
| `forge_cache` | 2-3 blanks, hammer, coal |
| `event` | 10-20 emeralds, golden apple, one random blank |

A shop purchase costs 4 emeralds for one random supply item.

## Run shape to measure

Target: 20-35 minutes for a full Act 1, 8 map floors, 8-10 rooms, one boss.

| Metric | Target | Measured |
|---|---:|---:|
| Time per normal battle room | 2-4 min | TBD |
| Highest tier reached in a run | fine or rare | TBD |
| Deaths per first clear attempt | 0-2 | 1 (solo combat-node attempt) |
| Boss attempts needed | 1-3 | TBD |
| Blades forged per run | 2-5 | TBD |
| Hammer strikes consumed | 80-128 | TBD |

## Death / failure contract to verify

- Solo death: run becomes `FAILED`, inventory cleared after 60 ticks, player returns to
  the hub, `clears` unchanged.
- Recall scroll: only the user leaves; if the party is then empty the run ends.
- Server restart while `RUNNING`: saved data loads as `FAILED`; login returns to hub.
- Room placement failure: the run ends and the party returns to hub rather than being
  left in a cleared/void room.

## Tuning levers, in order

1. Encounter JSON wave counts.
2. `pack/overrides/config/inf-common.toml` (`baseGain`, `riskPerStrike`, `gainVariance`).
3. Loot table rolls.
4. Boss choice/fallback.

Do not change Inf's formula or code to hit a target; use the config and data pack.
