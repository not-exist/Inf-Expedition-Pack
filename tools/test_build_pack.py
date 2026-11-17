#!/usr/bin/env python3
"""Tests for the pack manifest/zip builder."""
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_pack


class BuildPackTest(unittest.TestCase):
    def _spec(self, root: Path) -> dict:
        (root / "pack" / "overrides" / "config").mkdir(parents=True)
        (root / "pack" / "overrides" / "config" / "x.toml").write_text("x=1\n")
        (root / "inf-core" / "build" / "libs").mkdir(parents=True)
        (root / "inf-core" / "build" / "libs" / "inf-0.2.0.jar").write_bytes(b"core")
        (root / "build" / "libs").mkdir(parents=True)
        (root / "build" / "libs" / "inf_expedition-0.1.0.jar").write_bytes(b"expedition")
        return {
            "pack": {
                "name": "Test Pack",
                "version": "0.1.0",
                "minecraft": "1.21.1",
                "loader": "neoforge-21.1.266",
                "author": "Inf",
            },
            "mods": [
                {"name": "FTB Quests", "source": "curseforge",
                 "project_id": 289412, "file_id": None, "side": "both"},
            ],
            "local_mods": [
                {"name": "Inf", "path": "inf-core/build/libs/inf-0.2.0.jar"},
                {"name": "Inf Expedition", "path": "build/libs/inf_expedition-0.1.0.jar"},
            ],
            "overrides": "overrides",
        }

    def test_validate_warns_on_missing_file_id(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            errors, warnings = build_pack.validate(spec, root)
            self.assertEqual([], errors)
            self.assertTrue(any("file_id" in warning for warning in warnings))

    def test_manifest_contains_loader_and_local_jar(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            manifest = build_pack.build_manifest(spec)
            self.assertEqual("1.21.1", manifest["minecraft"]["version"])
            self.assertEqual("neoforge-21.1.266",
                             manifest["minecraft"]["modLoaders"][0]["id"])
            self.assertTrue((root / "inf-core" / "build" / "libs" / "inf-0.2.0.jar").exists())

    def test_zip_contains_manifest_and_local_mods(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            out = build_pack.build_zip(spec, root, root / "out.zip")
            with zipfile.ZipFile(out) as archive:
                names = set(archive.namelist())
            self.assertIn("manifest.json", names)
            self.assertIn("overrides/mods/inf-0.2.0.jar", names)
            self.assertIn("overrides/mods/inf_expedition-0.1.0.jar", names)
            self.assertIn("overrides/config/x.toml", names)

    def test_local_build_fails_when_required_local_mod_is_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            (root / "build" / "libs" / "inf_expedition-0.1.0.jar").unlink()
            with self.assertRaisesRegex(FileNotFoundError, "Missing required local mods"):
                build_pack.build_local(spec, root, root / "local")

    def test_local_build_copies_local_mods(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            out = build_pack.build_local(spec, root, root / "local")
            self.assertTrue((out / "mods" / "inf-0.2.0.jar").exists())
            self.assertTrue((out / "mods" / "inf_expedition-0.1.0.jar").exists())
            self.assertTrue((out / "config" / "x.toml").exists())

    def test_zip_validate_rejects_unlocked_ids(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            errors, _ = build_pack.validate(spec, root, for_zip=True)
            self.assertTrue(any("file_id" in error for error in errors))

    def test_role_and_dependency_checks(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            spec["mods"][0]["role"] = "content"
            spec["mods"][0]["depends"] = ["Missing Library"]
            errors, warnings = build_pack.validate(spec, root)
            self.assertEqual([], errors)
            self.assertTrue(any("Missing Library" in warning for warning in warnings))
            errors, _ = build_pack.validate(spec, root, for_zip=True)
            self.assertTrue(any("Missing Library" in error for error in errors))
            spec["mods"][0]["role"] = "nonsense"
            errors, _ = build_pack.validate(spec, root)
            self.assertTrue(any("role" in error for error in errors))

    def test_zip_validate_rejects_loader_drift(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            spec["pack"]["loader"] = "neoforge-21.0.0"
            errors, _ = build_pack.validate(spec, root, for_zip=True)
            self.assertTrue(any("loader" in error for error in errors))

    def test_zip_rejects_modrinth_entries(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            spec = self._spec(root)
            spec["mods"].append({
                "name": "KubeJS", "source": "modrinth",
                "project": "kubejs", "version": "x", "side": "both"})
            errors, _ = build_pack.validate(spec, root, for_zip=True)
            self.assertTrue(any("modrinth" in error.lower() for error in errors))


if __name__ == "__main__":
    unittest.main()
