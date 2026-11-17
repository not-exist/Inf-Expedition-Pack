#!/usr/bin/env python3
"""Assemble the CurseForge-format pack zip and a local test directory.

The source of truth is pack/mods.yaml. Third-party jars are never committed;
they are referenced by CurseForge project/file id (or supplied through
pack/cache for a local build).
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SPEC = ROOT / "pack" / "mods.yaml"
LOADER = "neoforge-21.1.266"
VALID_ROLES = {"core", "content", "meta", "client", "library"}


def load_spec(path: Path) -> dict:
    with Path(path).open("r", encoding="utf-8") as handle:
        return yaml.safe_load(handle)


def validate(spec: dict, root: Path = ROOT, for_zip: bool = False) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    pack = spec.get("pack") or {}
    for key in ("name", "version", "minecraft", "loader", "author"):
        if not pack.get(key):
            errors.append(f"pack.{key} is required")
    if pack.get("loader") and pack.get("loader") != LOADER:
        message = f"pack.loader is {pack.get('loader')}, expected {LOADER}"
        if for_zip:
            errors.append(message)
        else:
            warnings.append(message)
    mods = spec.get("mods") or []
    if not mods:
        errors.append("mods list is empty")
    known = set()
    for mod in mods:
        if mod.get("name"):
            known.add(mod["name"])
        if mod.get("modrinth"):
            known.add(mod["modrinth"])
    for index, mod in enumerate(mods):
        label = mod.get("name") or f"mods[{index}]"
        if not mod.get("name"):
            errors.append(f"{label}: name is required")
        source = mod.get("source")
        if source not in ("curseforge", "modrinth", "manual"):
            errors.append(f"{label}: source must be curseforge, modrinth or manual")
        side = mod.get("side", "both")
        if side not in ("client", "server", "both"):
            errors.append(f"{label}: side must be client, server or both")
        role = mod.get("role")
        if role is not None and role not in VALID_ROLES:
            errors.append(f"{label}: role must be one of {sorted(VALID_ROLES)}")
        depends = mod.get("depends", [])
        if not isinstance(depends, list):
            errors.append(f"{label}: depends must be a list")
            depends = []
        for dependency in depends:
            if dependency not in known:
                message = f"{label}: dependency {dependency} is not in mods.yaml"
                if for_zip:
                    errors.append(message)
                else:
                    warnings.append(message)
        if source == "curseforge":
            if not mod.get("project_id"):
                message = f"{label}: project_id is not locked yet"
                if for_zip:
                    errors.append(message)
                else:
                    warnings.append(message)
            if not mod.get("file_id"):
                message = f"{label}: file_id is not locked yet"
                if for_zip:
                    errors.append(message)
                else:
                    warnings.append(message)
        if source == "modrinth":
            if for_zip:
                errors.append(f"{label}: modrinth entries cannot be referenced by a "
                              "CurseForge manifest; use a CurseForge id or a manual cache entry")
            elif not mod.get("project") or not mod.get("version"):
                errors.append(f"{label}: modrinth project/version are required")
        if source == "manual":
            if for_zip:
                errors.append(f"{label}: manual entries cannot be referenced by a CurseForge manifest")
            elif not mod.get("url"):
                warnings.append(f"{label}: manual entry has no url; put the jar in pack/cache")
    local_mods = spec.get("local_mods") or []
    if not local_mods:
        errors.append("local_mods list is empty")
    for local in local_mods:
        path = root / str(local.get("path", ""))
        if not path.is_file():
            errors.append(f"local mod {local.get('name')}: missing {path}")
    overrides = root / "pack" / str(spec.get("overrides", "overrides"))
    if not overrides.is_dir():
        errors.append(f"overrides directory missing: {overrides}")
    return errors, warnings


def build_manifest(spec: dict) -> dict:
    files = []
    for mod in spec.get("mods") or []:
        if mod.get("source") != "curseforge":
            continue
        project_id = mod.get("project_id")
        file_id = mod.get("file_id")
        if not project_id or not file_id:
            continue
        files.append({
            "projectID": int(project_id),
            "fileID": int(file_id),
            "required": True,
        })
    pack = spec.get("pack") or {}
    return {
        "minecraft": {
            "version": pack.get("minecraft", "1.21.1"),
            "modLoaders": [{"id": pack.get("loader", LOADER), "primary": True}],
        },
        "manifestType": "minecraftModpack",
        "manifestVersion": 1,
        "name": pack.get("name", "Inf Expedition"),
        "version": pack.get("version", "0.1.0"),
        "author": pack.get("author", "Inf"),
        "files": files,
        "overrides": spec.get("overrides", "overrides"),
    }


def _copy_local_mods(spec: dict, root: Path, mods_dir: Path) -> list[str]:
    missing = []
    mods_dir.mkdir(parents=True, exist_ok=True)
    for local in spec.get("local_mods") or []:
        source = root / str(local.get("path", ""))
        if not source.is_file():
            missing.append(str(source))
            continue
        shutil.copy2(source, mods_dir / source.name)
    if missing:
        raise FileNotFoundError("Missing required local mods: " + ", ".join(missing))
    return missing


def build_zip(spec: dict, root: Path = ROOT, out: Path | None = None) -> Path:
    pack = spec.get("pack") or {}
    out = Path(out or ROOT / "pack" / "export" /
               f"duantu-qianchui-{pack.get('version', '0.1.0')}.zip")
    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        stage = Path(tmp)
        overrides_src = root / "pack" / str(spec.get("overrides", "overrides"))
        shutil.copytree(overrides_src, stage / "overrides", dirs_exist_ok=True)
        _copy_local_mods(spec, root, stage / "overrides" / "mods")
        (stage / "manifest.json").write_text(
            json.dumps(build_manifest(spec), ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(stage.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(stage).as_posix())
    return out


def build_local(spec: dict, root: Path = ROOT, out: Path | None = None) -> Path:
    out = Path(out or ROOT / "pack" / "export" / "local")
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True, exist_ok=True)
    _copy_local_mods(spec, root, out / "mods")
    overrides_src = root / "pack" / str(spec.get("overrides", "overrides"))
    shutil.copytree(overrides_src, out, dirs_exist_ok=True)

    missing = []
    cache = root / "pack" / "cache"
    for mod in spec.get("mods") or []:
        name = mod.get("name", "unknown")
        safe = re.sub(r"[^A-Za-z0-9._ -]+", "_", name).strip()
        candidates = [
            cache / f"{name}.jar",
            cache / f"{name.replace(' ', '_')}.jar",
            cache / f"{safe}.jar",
        ]
        found = next((candidate for candidate in candidates if candidate.is_file()), None)
        if found is None:
            missing.append(name)
        else:
            shutil.copy2(found, out / "mods" / found.name)
    (out / "mods.txt").write_text(
        "\n".join(m.get("name", "") for m in spec.get("mods") or []) + "\n",
        encoding="utf-8")
    if missing:
        print("Missing third-party jars (put them in pack/cache as <name>.jar):")
        for name in missing:
            print(f"  - {name}")
    return out


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--spec", type=Path, default=DEFAULT_SPEC)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--zip", action="store_true")
    parser.add_argument("--local", action="store_true")
    args = parser.parse_args(argv)
    spec = load_spec(args.spec)
    errors, warnings = validate(spec, ROOT, for_zip=args.zip)
    for warning in warnings:
        print(f"warning: {warning}")
    for error in errors:
        print(f"error: {error}", file=sys.stderr)
    if errors:
        return 1
    if args.check:
        print("OK")
        return 0
    if args.zip:
        print(build_zip(spec, ROOT))
        return 0
    if args.local:
        print(build_local(spec, ROOT))
        return 0
    parser.print_help()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
