#!/usr/bin/env python3
"""Download pack jars into pack/cache from Modrinth or ForgeCDN.

Usage:
  python3 tools/fetch_mods.py --dry-run
  python3 tools/fetch_mods.py
  python3 tools/fetch_mods.py --only KubeJS,FTB Quests

Third-party jars are cached locally and are not committed. A lock file with
resolved Modrinth version ids/urls/hashes is written to pack/mods.lock.json.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import urllib.parse
import urllib.request
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "pack" / "cache"
LOCK = ROOT / "pack" / "mods.lock.json"
UA = "InfExpeditionPack/0.1 (modpack build)"


def api_json(url: str):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def safe_name(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9._ -]+", "_", name).strip()


def modrinth_file(mod: dict) -> dict:
    slug = mod.get("modrinth")
    if not slug:
        raise ValueError("no Modrinth slug")
    url = (f"https://api.modrinth.com/v2/project/{urllib.parse.quote(slug)}/version"
           "?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D")
    versions = api_json(url)
    if not versions:
        raise ValueError(f"no 1.21.1 NeoForge version for {slug}")
    wanted = mod.get("version")
    chosen = versions[0]
    if wanted:
        exact = [v for v in versions if v.get("version_number") == wanted]
        if exact:
            chosen = exact[0]
    files = chosen.get("files") or []
    primary = next((f for f in files if f.get("primary")), files[0] if files else None)
    if not primary:
        raise ValueError(f"no file for {slug} {chosen.get('version_number')}")
    return {
        "kind": "modrinth",
        "project": slug,
        "version_id": chosen["id"],
        "version_number": chosen["version_number"],
        "filename": primary["filename"],
        "url": primary["url"],
        "sha1": primary.get("hashes", {}).get("sha1"),
        "sha512": primary.get("hashes", {}).get("sha512"),
    }


def curseforge_file(mod: dict) -> dict:
    file_id = int(mod["file_id"])
    filename = mod["filename"]
    url = f"https://mediafilez.forgecdn.net/files/{file_id // 1000}/{file_id % 1000}/{filename}"
    return {
        "kind": "curseforge",
        "project": mod.get("project_id"),
        "file_id": file_id,
        "filename": filename,
        "url": url,
        "sha1": None,
        "sha512": None,
    }


def resolve(mod: dict) -> dict:
    if mod.get("file_id") and mod.get("filename"):
        return curseforge_file(mod)
    if mod.get("modrinth"):
        return modrinth_file(mod)
    raise ValueError("no file_id/filename or modrinth slug")


def download(url: str, target: Path, expected_sha1: str | None) -> str:
    target.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    digest = hashlib.sha1()
    tmp = target.with_suffix(target.suffix + ".part")
    with urllib.request.urlopen(request, timeout=300) as response, tmp.open("wb") as out:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            out.write(chunk)
            digest.update(chunk)
    actual = digest.hexdigest()
    if expected_sha1 and actual != expected_sha1:
        tmp.unlink(missing_ok=True)
        raise ValueError(f"sha1 mismatch for {target.name}: {actual} != {expected_sha1}")
    tmp.replace(target)
    return actual


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--only", default="")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args(argv)
    spec = yaml.safe_load((ROOT / "pack" / "mods.yaml").read_text())
    only = {part.strip() for part in args.only.split(",") if part.strip()}
    lock = {}
    if LOCK.exists():
        lock = json.loads(LOCK.read_text())
    failures = []
    for mod in spec.get("mods") or []:
        name = mod.get("name", "unknown")
        if only and name not in only:
            continue
        try:
            resolved = resolve(mod)
        except Exception as exc:
            failures.append((name, str(exc)))
            continue
        target = CACHE / f"{safe_name(name)}.jar"
        resolved["target"] = str(target.relative_to(ROOT))
        print(f"{name:42} {resolved['version_number'] if resolved['kind']=='modrinth' else resolved['file_id']}  {resolved['filename']}")
        if args.dry_run:
            lock[name] = resolved
            continue
        if target.exists() and not args.force:
            print(f"  cached {target.name}")
        else:
            try:
                sha1 = download(resolved["url"], target, resolved.get("sha1"))
                resolved["sha1"] = sha1
            except Exception as exc:
                failures.append((name, str(exc)))
                continue
        lock[name] = resolved
    if not args.dry_run:
        LOCK.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if failures:
        print("\nFailures:", file=sys.stderr)
        for name, error in failures:
            print(f"  - {name}: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
