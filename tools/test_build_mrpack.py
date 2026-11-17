#!/usr/bin/env python3
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_mrpack


class MrpackTest(unittest.TestCase):
    def test_build_fails_when_required_local_mod_is_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "pack" / "overrides").mkdir(parents=True)
            spec = {
                "pack": {"name": "Test", "version": "0.1.0"},
                "mods": [],
                "local_mods": [{"name": "Inf", "path": "inf-core/build/libs/inf-0.2.0.jar"}],
            }
            with self.assertRaisesRegex(FileNotFoundError, "Inf"):
                build_mrpack.build(spec, {}, root, root / "out.mrpack")

    def test_build_index_and_zip(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "pack" / "overrides" / "config").mkdir(parents=True)
            (root / "pack" / "overrides" / "config" / "x.toml").write_text("x=1\n")
            (root / "pack" / "overrides" / "mods").mkdir(parents=True)
            (root / "pack" / "cache").mkdir(parents=True)
            jar = root / "pack" / "cache" / "Demo.jar"
            jar.write_bytes(b"demo-jar")
            (root / "inf-core" / "build" / "libs").mkdir(parents=True)
            (root / "inf-core" / "build" / "libs" / "inf-0.2.0.jar").write_bytes(b"inf")
            (root / "build" / "libs").mkdir(parents=True)
            (root / "build" / "libs" / "inf_expedition-0.1.0.jar").write_bytes(b"expedition")
            spec = {
                "pack": {"name": "Test", "version": "0.1.0", "minecraft": "1.21.1",
                          "loader": "neoforge-21.1.266", "author": "Inf"},
                "mods": [{"name": "Demo", "side": "both", "required": True,
                          "source": "curseforge"}],
                "local_mods": [
                    {"name": "Inf", "path": "inf-core/build/libs/inf-0.2.0.jar"},
                    {"name": "Inf Expedition", "path": "build/libs/inf_expedition-0.1.0.jar"},
                ],
            }
            lock = {"Demo": {"filename": "demo.jar", "url": "https://example.invalid/demo.jar",
                             "target": "pack/cache/Demo.jar"}}
            out = build_mrpack.build(spec, lock, root, root / "out.mrpack")
            with zipfile.ZipFile(out) as archive:
                names = set(archive.namelist())
                index = json.loads(archive.read("modrinth.index.json"))
            self.assertIn("overrides/config/x.toml", names)
            self.assertIn("overrides/mods/inf-0.2.0.jar", names)
            self.assertIn("overrides/mods/inf_expedition-0.1.0.jar", names)
            self.assertEqual(["mods/demo.jar"], [f["path"] for f in index["files"]])
            self.assertEqual("1.21.1", index["dependencies"]["minecraft"])


if __name__ == "__main__":
    unittest.main()
