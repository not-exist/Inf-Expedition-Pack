#!/usr/bin/env python3
"""Generate v0.1 room structures, room manifests, encounters and loot tables.

The generator writes standard Minecraft structure NBT files (gzipped NBT) plus
the JSON files consumed by inf_expedition. Run from the repository root:

    python3 tools/gen_rooms.py
    python3 tools/gen_rooms.py --check
"""
from __future__ import annotations

import argparse
import gzip
import json
import struct
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, Optional

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12
DATA_VERSION = 3955

MODID = "inf_expedition"
MARKER_NAMES = ("spawn", "mob", "boss", "reward", "exit", "shop", "rest")


# ---------------------------------------------------------------------------
# NBT writing
# ---------------------------------------------------------------------------

def _u16(value: int) -> bytes:
    return struct.pack(">H", value)


def _i32(value: int) -> bytes:
    return struct.pack(">i", value)


def _string(value: str) -> bytes:
    raw = value.encode("utf-8")
    return _u16(len(raw)) + raw


def _named(tag_id: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_id]) + _string(name) + payload


def _compound(fields: Iterable[bytes]) -> bytes:
    return b"".join(fields) + bytes([TAG_END])


def _list(tag_id: int, payloads: Iterable[bytes]) -> bytes:
    materialized = list(payloads)
    return bytes([tag_id]) + _i32(len(materialized)) + b"".join(materialized)


def _int_list(values: Iterable[int]) -> bytes:
    return _list(TAG_INT, (_i32(value) for value in values))


def _palette_entry(block: dict) -> bytes:
    fields = [_named(TAG_STRING, "Name", _string(block["Name"]))]
    properties = block.get("Properties")
    if properties:
        fields.append(_named(
            TAG_COMPOUND,
            "Properties",
            _compound(_named(TAG_STRING, key, _string(value))
                      for key, value in sorted(properties.items()))))
    return _compound(fields)


def write_structure(
        path: Path,
        size: tuple[int, int, int],
        palette: list[dict],
        blocks: list[dict],
) -> None:
    """Write a standard structure NBT file."""
    size_tag = _named(TAG_LIST, "size", _int_list(size))
    palette_tag = _named(
        TAG_LIST, "palette", _list(TAG_COMPOUND, (_palette_entry(block) for block in palette)))
    block_payloads = []
    for block in blocks:
        fields = [
            _named(TAG_LIST, "pos", _int_list(block["pos"])),
            _named(TAG_INT, "state", _i32(block["state"])),
        ]
        if block.get("nbt"):
            fields.append(_named(TAG_COMPOUND, "nbt", _compound(block["nbt"])))
        block_payloads.append(_compound(fields))
    blocks_tag = _named(TAG_LIST, "blocks", _list(TAG_COMPOUND, block_payloads))
    entities_tag = _named(TAG_LIST, "entities", _list(TAG_END, []))
    root = _named(
        TAG_COMPOUND,
        "",
        _compound([
            size_tag,
            palette_tag,
            blocks_tag,
            entities_tag,
            _named(TAG_INT, "DataVersion", _i32(DATA_VERSION)),
        ]))
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(path, "wb") as handle:
        handle.write(root)


# ---------------------------------------------------------------------------
# NBT reading (self-check only)
# ---------------------------------------------------------------------------

class _Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def take(self, count: int) -> bytes:
        value = self.data[self.pos:self.pos + count]
        if len(value) != count:
            raise ValueError("truncated NBT")
        self.pos += count
        return value

    def u1(self) -> int:
        return self.take(1)[0]

    def i2(self) -> int:
        return struct.unpack(">h", self.take(2))[0]

    def i4(self) -> int:
        return struct.unpack(">i", self.take(4))[0]

    def i8(self) -> int:
        return struct.unpack(">q", self.take(8))[0]

    def _string(self) -> str:
        length = struct.unpack(">H", self.take(2))[0]
        return self.take(length).decode("utf-8")

    def payload(self, tag_id: int):
        if tag_id == TAG_END:
            return None
        if tag_id == TAG_BYTE:
            return self.u1()
        if tag_id == TAG_SHORT:
            return self.i2()
        if tag_id == TAG_INT:
            return self.i4()
        if tag_id == TAG_LONG:
            return self.i8()
        if tag_id == TAG_FLOAT:
            return struct.unpack(">f", self.take(4))[0]
        if tag_id == TAG_DOUBLE:
            return struct.unpack(">d", self.take(8))[0]
        if tag_id == TAG_BYTE_ARRAY:
            return list(self.take(self.i4()))
        if tag_id == TAG_STRING:
            return self._string()
        if tag_id == TAG_LIST:
            element_type = self.u1()
            length = self.i4()
            return [self.payload(element_type) for _ in range(length)]
        if tag_id == TAG_COMPOUND:
            result = {}
            while True:
                child = self.u1()
                if child == TAG_END:
                    return result
                name = self._string()
                result[name] = self.payload(child)
        if tag_id == TAG_INT_ARRAY:
            return [self.i4() for _ in range(self.i4())]
        if tag_id == TAG_LONG_ARRAY:
            return [self.i8() for _ in range(self.i4())]
        raise ValueError(f"unknown NBT tag {tag_id}")

    def root(self):
        tag_id = self.u1()
        if tag_id != TAG_COMPOUND:
            raise ValueError("structure root must be a compound")
        self._string()
        return self.payload(TAG_COMPOUND)


def read_structure(path: Path) -> dict:
    with gzip.open(path, "rb") as handle:
        root = _Reader(handle.read()).root()
    blocks = []
    block_entities = []
    for block in root.get("blocks", []):
        entry = {"pos": block["pos"], "state": block["state"]}
        if "nbt" in block:
            entry["nbt"] = block["nbt"]
            block_entities.append(block["nbt"])
        blocks.append(entry)
    return {
        "size": root["size"],
        "palette": root["palette"],
        "blocks": blocks,
        "block_entities": block_entities,
    }


# ---------------------------------------------------------------------------
# Room specifications
# ---------------------------------------------------------------------------

@dataclass
class RoomSpec:
    key: str
    type: str
    size: tuple[int, int, int]
    floor: str
    wall: str
    ceiling_block: str
    light: str = "minecraft:glowstone"
    ceiling: bool = True
    markers: dict[str, list[tuple[int, int, int]]] = field(default_factory=dict)
    decorations: list[tuple[tuple[int, int, int], str]] = field(default_factory=list)
    encounter: Optional[dict] = None
    reward_loot: Optional[str] = None
    title_key: str = ""


def _spec(
        key: str,
        room_type: str,
        size: tuple[int, int, int],
        floor: str,
        wall: str,
        ceiling_block: str,
        markers: dict[str, list[tuple[int, int, int]]],
        decorations: Iterable[tuple[tuple[int, int, int], str]] = (),
        encounter: Optional[dict] = None,
        reward_loot: Optional[str] = None,
        ceiling: bool = True,
) -> RoomSpec:
    name = key.replace("/", "_")
    return RoomSpec(
        key=key,
        type=room_type,
        size=size,
        floor=floor,
        wall=wall,
        ceiling_block=ceiling_block,
        markers=markers,
        decorations=list(decorations),
        encounter=encounter,
        reward_loot=reward_loot,
        title_key=f"room.{MODID}.{name}",
        ceiling=ceiling,
    )


def room_specs() -> list[RoomSpec]:
    standard_scale = {
        "health_per_act": 0.35,
        "damage_per_act": 0.2,
        "count_per_act": 0.2,
        "health_per_extra_player": 0.5,
        "damage_per_extra_player": 0.25,
    }
    return [
        _spec(
            "start/camp", "start", (24, 8, 24),
            "minecraft:stone_bricks", "minecraft:dark_oak_planks",
            "minecraft:dark_oak_planks",
            {"spawn": [(12, 1, 4)], "exit": [(20, 1, 20)], "reward": [(4, 1, 20)]},
            [
                ((12, 1, 18), "inf:forge_table"),
                ((14, 1, 18), "minecraft:crafting_table"),
                ((10, 1, 18), "minecraft:barrel"),
                ((8, 1, 8), "minecraft:lantern"),
            ],
            reward_loot="start_cache",
        ),
        _spec(
            "battle/crypt", "battle", (28, 10, 28),
            "minecraft:deepslate_tiles", "minecraft:deepslate_bricks",
            "minecraft:deepslate_bricks",
            {
                "spawn": [(14, 1, 4)], "exit": [(24, 1, 24)],
                "mob": [(8, 1, 14), (20, 1, 14), (14, 1, 22), (14, 1, 8)],
                "reward": [(14, 1, 24)],
            },
            [
                ((6, 1, 6), "minecraft:barrel"),
                ((21, 1, 21), "minecraft:barrel"),
                ((4, 1, 14), "minecraft:bookshelf"),
                ((23, 1, 14), "minecraft:bookshelf"),
            ],
            encounter={
                "boss": False,
                "waves": [
                    [("minecraft:zombie", 4)],
                    [("minecraft:skeleton", 3), ("minecraft:zombie", 2)],
                ],
                "scale": standard_scale,
                "reward_loot": "battle_common",
            },
            reward_loot="battle_common",
        ),
        _spec(
            "battle/forge_hall", "battle", (28, 10, 28),
            "minecraft:stone_bricks", "minecraft:blackstone",
            "minecraft:blackstone",
            {
                "spawn": [(14, 1, 4)], "exit": [(24, 1, 24)],
                "mob": [(8, 1, 14), (20, 1, 14), (14, 1, 22), (14, 1, 8)],
                "reward": [(14, 1, 24)],
            },
            [
                ((10, 1, 20), "inf:forge_table"),
                ((16, 1, 20), "inf:forge_table"),
                ((12, 1, 21), "minecraft:anvil"),
                ((8, 1, 20), "minecraft:magma_block"),
                ((20, 1, 20), "minecraft:magma_block"),
            ],
            encounter={
                "boss": False,
                "waves": [
                    [("minecraft:pillager", 3)],
                    [("minecraft:vindicator", 2), ("minecraft:pillager", 2)],
                ],
                "scale": standard_scale,
                "reward_loot": "battle_common",
            },
            reward_loot="battle_common",
        ),
        _spec(
            "battle/cavern", "battle", (30, 12, 30),
            "minecraft:mossy_cobblestone", "minecraft:stone",
            "minecraft:stone",
            {
                "spawn": [(15, 1, 4)], "exit": [(26, 1, 26)],
                "mob": [(7, 1, 10), (22, 1, 10), (7, 1, 22), (22, 1, 22), (15, 1, 15)],
                "reward": [(15, 1, 26)],
            },
            [
                ((5, 1, 5), "minecraft:moss_block"),
                ((24, 1, 5), "minecraft:moss_block"),
                ((5, 1, 24), "minecraft:moss_block"),
                ((24, 1, 24), "minecraft:moss_block"),
            ],
            encounter={
                "boss": False,
                "waves": [
                    [("minecraft:spider", 4)],
                    [("minecraft:cave_spider", 4)],
                    [("minecraft:silverfish", 5)],
                ],
                "scale": standard_scale,
                "reward_loot": "battle_common",
            },
            reward_loot="battle_common",
        ),
        _spec(
            "elite/arena", "elite", (30, 12, 30),
            "minecraft:polished_blackstone", "minecraft:blackstone",
            "minecraft:blackstone",
            {
                "spawn": [(15, 1, 4)], "exit": [(26, 1, 26)],
                "mob": [(7, 1, 10), (22, 1, 10), (7, 1, 22), (22, 1, 22), (15, 1, 15)],
                "reward": [(15, 1, 26)],
            },
            [
                ((4, 1, 4), "minecraft:chiseled_polished_blackstone"),
                ((25, 1, 4), "minecraft:chiseled_polished_blackstone"),
                ((4, 1, 25), "minecraft:chiseled_polished_blackstone"),
                ((25, 1, 25), "minecraft:chiseled_polished_blackstone"),
            ],
            encounter={
                "boss": False,
                "waves": [
                    [("legendary_monsters:ambusher", 1), ("minecraft:zombie", 2)],
                ],
                "scale": standard_scale,
                "reward_loot": "elite",
            },
            reward_loot="elite",
        ),
        _spec(
            "forge/smithy", "forge", (24, 8, 24),
            "minecraft:deepslate_tiles", "minecraft:dark_oak_planks",
            "minecraft:dark_oak_planks",
            {"spawn": [(12, 1, 4)], "exit": [(20, 1, 20)], "reward": [(4, 1, 20)]},
            [
                ((12, 1, 16), "inf:forge_table"),
                ((10, 1, 18), "minecraft:anvil"),
                ((14, 1, 18), "minecraft:anvil"),
                ((6, 1, 6), "minecraft:barrel"),
                ((18, 1, 6), "minecraft:barrel"),
                ((12, 1, 8), "minecraft:magma_block"),
            ],
            reward_loot="forge_cache",
        ),
        _spec(
            "rest/camp", "rest", (20, 8, 20),
            "minecraft:oak_planks", "minecraft:spruce_planks",
            "minecraft:spruce_planks",
            {"spawn": [(10, 1, 4)], "exit": [(16, 1, 16)], "rest": [(6, 1, 6), (14, 1, 14)]},
            [
                ((6, 1, 6), "minecraft:campfire"),
                ((14, 1, 14), "minecraft:campfire"),
                ((4, 1, 10), "minecraft:barrel"),
                ((16, 1, 10), "minecraft:barrel"),
            ],
        ),
        _spec(
            "shop/stall", "shop", (20, 8, 20),
            "minecraft:spruce_planks", "minecraft:oak_planks",
            "minecraft:oak_planks",
            {"spawn": [(10, 1, 4)], "exit": [(16, 1, 16)], "shop": [(10, 1, 14)]},
            [
                ((10, 1, 14), "minecraft:lectern"),
                ((7, 1, 14), "minecraft:barrel"),
                ((13, 1, 14), "minecraft:barrel"),
                ((4, 1, 6), "minecraft:lantern"),
                ((16, 1, 6), "minecraft:lantern"),
            ],
        ),
        _spec(
            "event/treasure", "event", (24, 8, 24),
            "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
            "minecraft:stone_bricks",
            {
                "spawn": [(12, 1, 4)], "exit": [(20, 1, 20)], "reward": [(12, 1, 20)],
                "mob": [(8, 1, 12), (16, 1, 12), (12, 1, 8)],
            },
            [
                ((6, 1, 6), "minecraft:barrel"),
                ((18, 1, 6), "minecraft:barrel"),
                ((12, 1, 12), "minecraft:chiseled_stone_bricks"),
            ],
            encounter={
                "boss": False,
                "waves": [
                    [("minecraft:skeleton", 2), ("minecraft:zombie", 2)],
                ],
                "scale": standard_scale,
                "reward_loot": "event",
            },
            reward_loot="event",
        ),
        _spec(
            "boss/wroughtnaut", "boss", (36, 14, 36),
            "minecraft:polished_blackstone", "minecraft:blackstone",
            "minecraft:blackstone",
            {
                "spawn": [(18, 1, 4)], "exit": [(32, 1, 32)],
                "boss": [(18, 1, 28)], "reward": [(18, 1, 32)],
            },
            [
                ((6, 1, 10), "minecraft:chiseled_polished_blackstone"),
                ((29, 1, 10), "minecraft:chiseled_polished_blackstone"),
                ((6, 1, 25), "minecraft:chiseled_polished_blackstone"),
                ((29, 1, 25), "minecraft:chiseled_polished_blackstone"),
                ((18, 1, 16), "minecraft:ancient_debris"),
            ],
            encounter={
                "boss": True,
                "waves": [
                    [("mowziesmobs:ferrous_wroughtnaut", 1)],
                ],
                "scale": standard_scale,
                "reward_loot": "boss",
            },
            reward_loot="boss",
        ),
    ]


# ---------------------------------------------------------------------------
# Structure construction
# ---------------------------------------------------------------------------

def _normalize(name: str) -> dict:
    return {"Name": name}


def _jigsaw(name: str) -> tuple[dict, list[bytes]]:
    state = {
        "Name": "minecraft:jigsaw",
        "Properties": {"orientation": "north_up", "joint": "aligned"},
    }
    nbt = [
        _named(TAG_STRING, "id", _string("minecraft:jigsaw")),
        _named(TAG_STRING, "name", _string(f"{MODID}:{name}")),
        _named(TAG_STRING, "target", _string("")),
        _named(TAG_STRING, "pool", _string("")),
        _named(TAG_STRING, "final_state", _string("minecraft:air")),
        _named(TAG_STRING, "joint", _string("aligned")),
    ]
    return state, nbt


def build_room(spec: RoomSpec) -> tuple[list[dict], list[dict]]:
    sx, sy, sz = spec.size
    palette: list[dict] = []
    palette_index: dict[str, int] = {}
    blocks: dict[tuple[int, int, int], tuple[int, Optional[list[bytes]]]] = {}

    def palette_id(state: dict) -> int:
        key = state["Name"]
        if "Properties" in state:
            key += "|" + "|".join(f"{k}={v}" for k, v in sorted(state["Properties"].items()))
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(state)
        return palette_index[key]

    def put(x: int, y: int, z: int, state: dict, nbt: Optional[list[bytes]] = None) -> None:
        blocks[(x, y, z)] = (palette_id(state), nbt)

    for x in range(sx):
        for z in range(sz):
            put(x, 0, z, _normalize(spec.floor))
    for y in range(1, sy - 1):
        for x in range(sx):
            put(x, y, 0, _normalize(spec.wall))
            put(x, y, sz - 1, _normalize(spec.wall))
        for z in range(1, sz - 1):
            put(0, y, z, _normalize(spec.wall))
            put(sx - 1, y, z, _normalize(spec.wall))
    if spec.ceiling:
        for x in range(sx):
            for z in range(sz):
                put(x, sy - 1, z, _normalize(spec.ceiling_block))
        step = max(4, min(sx, sz) // 4)
        for x in range(step // 2, sx, step):
            for z in range(step // 2, sz, step):
                put(x, sy - 1, z, _normalize(spec.light))
    for (x, y, z), block in spec.decorations:
        put(x, y, z, _normalize(block))
    for name, positions in spec.markers.items():
        if name not in MARKER_NAMES:
            raise ValueError(f"unknown marker {name}")
        state, nbt = _jigsaw(name)
        for (x, y, z) in positions:
            put(x, y, z, state, nbt)

    block_list = []
    for (x, y, z), (state, nbt) in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][0], item[0][2])):
        entry = {"pos": [x, y, z], "state": state}
        if nbt:
            entry["nbt"] = nbt
        block_list.append(entry)
    return palette, block_list


def build_hub() -> tuple[list[dict], list[dict]]:
    spec = _spec(
        "hub", "hub", (9, 8, 9),
        "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone",
        "minecraft:polished_blackstone",
        markers={},
        decorations=[
            ((4, 1, 4), f"{MODID}:expedition_altar"),
            ((6, 1, 4), f"{MODID}:quest_board"),
            ((2, 1, 4), "minecraft:anvil"),
            ((2, 1, 6), "minecraft:barrel"),
            ((6, 1, 6), "minecraft:campfire"),
        ],
        ceiling=False,
    )
    return build_room(spec)


# ---------------------------------------------------------------------------
# JSON manifests
# ---------------------------------------------------------------------------

def room_json(spec: RoomSpec) -> dict:
    data = {
        "id": f"{MODID}:{spec.key}",
        "type": spec.type,
        "weight": 10,
        "structure": f"{MODID}:rooms/{spec.key}",
        "min_act": 1,
        "max_act": 1,
        "title": spec.title_key,
    }
    if spec.encounter is not None:
        data["encounter"] = f"{MODID}:{spec.key}"
    if spec.reward_loot is not None:
        data["reward_loot"] = f"{MODID}:chests/{spec.reward_loot}"
    return data


def encounter_json(spec: RoomSpec) -> Optional[dict]:
    if spec.encounter is None:
        return None
    waves = []
    for wave in spec.encounter["waves"]:
        waves.append({"mobs": [{"id": entity, "count": count} for entity, count in wave]})
    return {
        "title": f"encounter.{MODID}.{spec.key.replace('/', '_')}",
        "boss": spec.encounter.get("boss", False),
        "waves": waves,
        "scale": spec.encounter["scale"],
        "reward_loot": f"{MODID}:chests/{spec.encounter['reward_loot']}",
    }


def _item(name: str, count: Optional[dict] = None, weight: Optional[int] = None) -> dict:
    entry = {"type": "minecraft:item", "name": name}
    if count is not None:
        entry["functions"] = [{"function": "minecraft:set_count", "count": count}]
    if weight is not None:
        entry["weight"] = weight
    return entry


def loot_tables() -> dict[str, dict]:
    return {
        "battle_common": {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 1, "entries": [
                    _item("minecraft:emerald", {"min": 4, "max": 8}),
                    _item("minecraft:iron_ingot", {"min": 2, "max": 4}),
                    _item("minecraft:cooked_beef", {"min": 3, "max": 5}),
                ]},
                {"rolls": 1, "entries": [
                    _item("inf:sword_blank", weight=1),
                    _item("inf:pickaxe_blank", weight=1),
                    _item("inf:axe_blank", weight=1),
                    _item("inf:scythe_blank", weight=1),
                    _item("inf:dagger_blank", weight=1),
                ]},
            ],
            "random_sequence": f"{MODID}:chests/battle_common",
        },
        "elite": {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 1, "entries": [
                    _item("minecraft:emerald", {"min": 8, "max": 12}),
                    _item("inf:hammer"),
                    _item("minecraft:golden_apple"),
                ]},
                {"rolls": 2, "entries": [
                    _item("inf:sword_blank", weight=1),
                    _item("inf:pickaxe_blank", weight=1),
                    _item("inf:axe_blank", weight=1),
                    _item("inf:scythe_blank", weight=1),
                    _item("inf:dagger_blank", weight=1),
                ]},
            ],
            "random_sequence": f"{MODID}:chests/elite",
        },
        "boss": {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 1, "entries": [
                    _item("inf:hammer"),
                    _item("minecraft:emerald", {"min": 16, "max": 24}),
                    _item("minecraft:diamond", {"min": 2, "max": 4}),
                ]},
                {"rolls": 2, "entries": [
                    _item("inf:sword_blank", weight=1),
                    _item("inf:pickaxe_blank", weight=1),
                    _item("inf:axe_blank", weight=1),
                    _item("inf:scythe_blank", weight=1),
                    _item("inf:dagger_blank", weight=1),
                ]},
            ],
            "random_sequence": f"{MODID}:chests/boss",
        },
        "forge_cache": {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 2, "entries": [
                    _item("inf:sword_blank", weight=1),
                    _item("inf:pickaxe_blank", weight=1),
                    _item("inf:axe_blank", weight=1),
                    _item("inf:scythe_blank", weight=1),
                    _item("inf:dagger_blank", weight=1),
                ]},
                {"rolls": 1, "entries": [_item("inf:hammer")]},
                {"rolls": 1, "entries": [_item("minecraft:coal", {"min": 4, "max": 8})]},
            ],
            "random_sequence": f"{MODID}:chests/forge_cache",
        },
        "start_cache": {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 1, "entries": [
                    _item("minecraft:emerald", {"min": 4, "max": 8}),
                    _item("minecraft:cooked_beef", {"min": 4, "max": 6}),
                ]},
                {"rolls": 1, "entries": [
                    _item("inf:sword_blank", weight=1),
                    _item("inf:pickaxe_blank", weight=1),
                    _item("inf:axe_blank", weight=1),
                    _item("inf:scythe_blank", weight=1),
                    _item("inf:dagger_blank", weight=1),
                ]},
            ],
            "random_sequence": f"{MODID}:chests/start_cache",
        },
        "event": {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": 1, "entries": [
                    _item("minecraft:emerald", {"min": 10, "max": 20}),
                    _item("minecraft:golden_apple", {"min": 1, "max": 2}),
                ]},
                {"rolls": 1, "entries": [
                    _item("inf:sword_blank", weight=1),
                    _item("inf:pickaxe_blank", weight=1),
                    _item("inf:axe_blank", weight=1),
                    _item("inf:scythe_blank", weight=1),
                    _item("inf:dagger_blank", weight=1),
                ]},
            ],
            "random_sequence": f"{MODID}:chests/event",
        },
    }


def map_json() -> dict:
    return {
        "acts": 1,
        "floors": 8,
        "columns": 3,
        "paths": 2,
        "boss_floor": 8,
        "elite_min_floor": 3,
        "shop_min_floor": 2,
        "forge_min_floor": 2,
        "guarantee_rest_before_boss": True,
        "weights": {
            "battle": 40,
            "event": 20,
            "forge": 14,
            "rest": 12,
            "shop": 8,
            "elite": 6,
        },
    }


# ---------------------------------------------------------------------------
# Generation
# ---------------------------------------------------------------------------

def generate(out: Path) -> None:
    out = Path(out)
    (out / "structure" / "rooms").mkdir(parents=True, exist_ok=True)
    (out / "expedition" / "rooms").mkdir(parents=True, exist_ok=True)
    (out / "expedition" / "encounters").mkdir(parents=True, exist_ok=True)
    (out / "loot_table" / "chests").mkdir(parents=True, exist_ok=True)

    for spec in room_specs():
        palette, blocks = build_room(spec)
        write_structure(out / "structure" / "rooms" / f"{spec.key}.nbt", spec.size, palette, blocks)
        room_path = out / "expedition" / "rooms" / f"{spec.key}.json"
        room_path.parent.mkdir(parents=True, exist_ok=True)
        room_path.write_text(json.dumps(room_json(spec), ensure_ascii=False, indent=2) + "\n",
                             encoding="utf-8")
        encounter = encounter_json(spec)
        if encounter is not None:
            encounter_path = out / "expedition" / "encounters" / f"{spec.key}.json"
            encounter_path.parent.mkdir(parents=True, exist_ok=True)
            encounter_path.write_text(json.dumps(encounter, ensure_ascii=False, indent=2) + "\n",
                                      encoding="utf-8")

    hub_palette, hub_blocks = build_hub()
    write_structure(out / "structure" / "hub_platform.nbt", (9, 8, 9), hub_palette, hub_blocks)

    for name, table in loot_tables().items():
        (out / "loot_table" / "chests" / f"{name}.json").write_text(
            json.dumps(table, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (out / "expedition" / "map.json").write_text(
        json.dumps(map_json(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def check() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp)
        generate(out)
        for spec in room_specs():
            structure = read_structure(out / "structure" / "rooms" / f"{spec.key}.nbt")
            assert structure["size"] == list(spec.size), (spec.key, structure["size"])
            for marker in spec.markers:
                names = {entry.get("name") for entry in structure["block_entities"]}
                assert f"{MODID}:{marker}" in names, (spec.key, marker)
        read_structure(out / "structure" / "hub_platform.nbt")
    print("OK")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path,
                        default=Path("src/main/resources/data/inf_expedition"))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        check()
        return 0
    generate(args.out)
    print(f"generated rooms in {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
