#!/usr/bin/env python3
"""Self-checks for the generated structure NBT files."""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_rooms


class GenRoomsTest(unittest.TestCase):
    def test_generate_all_rooms(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            gen_rooms.generate(out)
            specs = gen_rooms.room_specs()
            self.assertEqual(10, len(specs))
            for spec in specs:
                nbt = out / "structure" / "rooms" / f"{spec.key}.nbt"
                self.assertTrue(nbt.exists(), nbt)
                structure = gen_rooms.read_structure(nbt)
                self.assertEqual(list(spec.size), structure["size"])
                for block in structure["blocks"]:
                    self.assertLess(block["state"], len(structure["palette"]))
                marker_names = {
                    entry.get("name") for entry in structure["block_entities"]
                }
                for marker in spec.markers:
                    self.assertIn(f"inf_expedition:{marker}", marker_names)
                self.assertTrue((out / "expedition" / "rooms" / f"{spec.key}.json").exists())
                if spec.encounter is not None:
                    self.assertTrue(
                        (out / "expedition" / "encounters" / f"{spec.key}.json").exists())
            self.assertTrue((out / "structure" / "hub_platform.nbt").exists())


if __name__ == "__main__":
    unittest.main()
