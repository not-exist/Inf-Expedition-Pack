#!/usr/bin/env python3
"""Create pack/server-cache with the dedicated-server subset of pack/cache.

Client-only/config-client mods are excluded. Used for the KubeJS/data smoke test:
  ./gradlew runServer -PpackCache=../pack/server-cache
"""
from __future__ import annotations

import os
import re
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "pack" / "cache"
OUT = ROOT / "pack" / "server-cache"
EXCLUDE = {
    "FancyMenu", "Konkrete", "Melody", "Drippy Loading Screen", "Embeddium",
    "Xaero's Minimap", "Controlling", "Searchables", "Mouse Tweaks",
    "Inventory Profiles Next", "Inventory HUD+", "libIPN", "Boss Checklist",
    "Enhanced Boss Bars", "Dusty Decorations", "Kotlin for Forge",
}


def safe(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9._ -]+", "_", name).strip()


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    spec = yaml.safe_load((ROOT / "pack" / "mods.yaml").read_text())
    kept = 0
    for mod in spec.get("mods") or []:
        name = mod.get("name", "")
        if mod.get("side") == "client" or name in EXCLUDE:
            continue
        source = CACHE / f"{safe(name)}.jar"
        if not source.exists():
            print(f"missing {source.name}")
            continue
        target = OUT / source.name
        if target.is_symlink() or target.exists():
            target.unlink()
        os.symlink(source, target)
        kept += 1
    print(f"server cache: {kept} jars in {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
