#!/usr/bin/env python3
"""Build a Modrinth .mrpack from pack/mods.lock.json + pack/cache.

Local jars (Inf, Inf Expedition) are placed in overrides/mods so they do not
need a hosted download URL.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import zipfile
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]


def _sha(path: Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as handle:
        while chunk := handle.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def build_index(spec: dict, lock: dict, root: Path = ROOT) -> dict:
    mods = {mod["name"]: mod for mod in spec.get("mods") or []}
    files = []
    used_names: set[str] = set()
    for name, entry in lock.items():
        mod = mods.get(name)
        if mod is None:
            continue
        cache = root / entry["target"]
        if not cache.is_file():
            raise FileNotFoundError(cache)
        filename = entry.get("filename") or cache.name
        if filename in used_names:
            filename = f"{cache.stem}-{len(used_names)}{cache.suffix}"
        used_names.add(filename)
        env = "optional" if not mod.get("required", True) else "required"
        side = mod.get("side", "both")
        files.append({
            "path": f"mods/{filename}",
            "hashes": {
                "sha1": _sha(cache, "sha1"),
                "sha512": _sha(cache, "sha512"),
            },
            "env": {
                "client": "unsupported" if side == "server" else env,
                "server": "unsupported" if side == "client" else env,
            },
            "downloads": [entry["url"]],
            "fileSize": cache.stat().st_size,
        })
    pack = spec.get("pack") or {}
    return {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": pack.get("version", "0.1.0"),
        "name": pack.get("name", "Inf Expedition"),
        "files": files,
        "dependencies": {
            "minecraft": pack.get("minecraft", "1.21.1"),
            "neoforge": "21.1.266",
        },
    }


def build(spec: dict, lock: dict, root: Path = ROOT, out: Path | None = None) -> Path:
    pack = spec.get("pack") or {}
    out = Path(out or root / "pack" / "export" /
               f"duantu-qianchui-{pack.get('version', '0.1.0')}.mrpack")
    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        stage = Path(tmp)
        shutil.copytree(root / "pack" / "overrides", stage / "overrides", dirs_exist_ok=True)
        (stage / "overrides" / "mods").mkdir(parents=True, exist_ok=True)
        for local in spec.get("local_mods") or []:
            source = root / str(local["path"])
            if not source.is_file():
                raise FileNotFoundError(f"Required local mod {local['name']} is missing: {source}")
            shutil.copy2(source, stage / "overrides" / "mods" / source.name)
        (stage / "modrinth.index.json").write_text(
            json.dumps(build_index(spec, lock, root), ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(stage.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(stage).as_posix())
    return out


def main() -> int:
    spec = yaml.safe_load((ROOT / "pack" / "mods.yaml").read_text())
    lock = json.loads((ROOT / "pack" / "mods.lock.json").read_text())
    print(build(spec, lock, ROOT))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
