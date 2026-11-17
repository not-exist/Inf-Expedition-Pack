# Inf Expedition Pack

《锻途：千锤之路》 (`Inf: Thousand Hammers`) is a Minecraft 1.21.1 NeoForge
roguelike expedition built around the Inf forging mod.

The repository contains the `inf_expedition` mod, room/map data, pack overrides,
and reproducible pack-building tools. The Inf core is pinned as the `inf-core`
Git submodule at the `v0.2.0` tag; this repository is maintained independently
from the Inf core repository.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.266
- Java 21
- Python 3.11+ with `Pillow` and `PyYAML` for pack tooling
- Git with submodule support

## Clone and build

Clone with the core submodule:

```bash
git clone --recurse-submodules https://github.com/not-exist/Inf-Expedition-Pack.git
cd Inf-Expedition-Pack
```

Build the pinned Inf core first, then the expedition mod:

```bash
JAVA_HOME=/path/to/jdk-21 ./inf-core/gradlew -p inf-core build
JAVA_HOME=/path/to/jdk-21 ./gradlew build
```

Run the unit tests:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew test
python3 tools/test_gen_rooms.py
python3 tools/test_pack_assets.py
python3 tools/test_build_pack.py
python3 tools/test_build_mrpack.py
```

## Build the pack

Third-party jars are not committed. Download the versions recorded in
`pack/mods.yaml`, producing `pack/cache/` and updating the lock file:

```bash
python3 tools/fetch_mods.py
python3 tools/build_pack.py --check
python3 tools/build_pack.py --local
python3 tools/build_mrpack.py
```

Outputs are written to ignored paths:

- `pack/export/local/` — local playable directory
- `pack/export/duantu-qianchui-0.1.0.mrpack` — Modrinth pack

The local pack includes the pinned Inf core jar and the freshly built
`inf_expedition` jar. Third-party jars are copied from `pack/cache/` and are
never committed to this repository.

## Development client

After building the Inf core and downloading the pack cache:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew runClient -PpackCache=pack/cache
```

The `inf_expedition` mod is intentionally independent at the code level: its
runtime integration uses Inf item IDs and the pinned core jar rather than a
source dependency.

## Licensing and credits

See [CREDITS.md](CREDITS.md). Inf and Inf Expedition are separate local mods;
third-party mods remain external dependencies and are referenced by the pack
lock file rather than stored in Git.
