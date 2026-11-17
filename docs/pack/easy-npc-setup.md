# Easy NPC setup — v0.2

Easy NPC is installed to replace the KubeJS "shop chest" with a real merchant and
dialogue rooms. Its preset/trade authoring is done in-game (or with `.npc.snbt` presets);
the wiki does not publish a stable JSON schema for trades, so this is a one-time manual
step rather than a generated data file.

## Wiki

- https://github.com/MarkusBordihn/BOs-Easy-NPC/wiki
- Commands: https://github.com/MarkusBordihn/BOs-Easy-NPC/wiki/Commands
- Trading: https://github.com/MarkusBordihn/BOs-Easy-NPC/wiki/Trading
- Presets: https://github.com/MarkusBordihn/BOs-Easy-NPC/wiki/Preset
- Default presets: https://github.com/MarkusBordihn/BOs-Easy-NPC/wiki/Default-Presets

## One-time setup

1. Install `Easy NPC`, `Easy NPC: Core`, `Easy NPC: Config UI` (and optionally
   `EasyNPC x FTB Quests Compat`).
2. In a creative test world, run `/easy_npc preset_browser` or
   `/easy_npc preset import_new default <preset>` and pick a merchant preset
   (the wiki lists **Skeleton Trader**, **Welcome Coin Trader**, **Griselda Grindstone**,
   **Goldmere**).
3. Configure trades with `/easy_npc configure <npc>`:
   - selling: Inf blanks/hammers/food/potions for emeralds;
   - optional: one rare artifact per run for a higher emerald price;
   - max uses/reset interval as desired.
4. Save/export the preset to `config/easy_npc/preset` (`.npc.snbt`) or the world preset
   storage; commit only files we authored.
5. In the pack, spawn the configured preset at the room's `shop` marker on
   `RoomEnteredEvent`; remove the KubeJS shop chest fallback once the preset is validated.

## Until the preset is validated

`shop.js` keeps the chest fallback so the vertical slice remains playable. The NPC is an
upgrade, not a dependency of the run loop.

## Verification

- NPC spawns once per shop room and is removed by `RoomManager.clear`.
- Trade offers/uses reset as configured.
- 2-player: only one trade session at a time.
- FTB Quests compatibility (optional) gates dialogue/quest completion correctly.
