#!/usr/bin/env python3
import ast
import json
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_pack_assets


def dimensions(path):
    with Image.open(path) as image:
        return image.size


def evaluate_curve(expression, level):
    """Evaluate the small arithmetic expression language used by the pack."""
    tree = ast.parse(expression, mode="eval")
    operators = {
        ast.Add: lambda left, right: left + right,
        ast.Sub: lambda left, right: left - right,
        ast.Mult: lambda left, right: left * right,
        ast.FloorDiv: lambda left, right: left // right,
        ast.Mod: lambda left, right: left % right,
    }

    def visit(node):
        if isinstance(node, ast.Expression):
            return visit(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return node.value
        if isinstance(node, ast.Name) and node.id == "level":
            return level
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            value = visit(node.operand)
            return value if isinstance(node.op, ast.UAdd) else -value
        if isinstance(node, ast.BinOp) and type(node.op) in operators:
            return operators[type(node.op)](visit(node.left), visit(node.right))
        raise ValueError(f"unsupported curve expression: {expression!r}")

    return visit(tree)


class PackAssetsTest(unittest.TestCase):
    def test_generate_assets(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            overrides = root / "overrides"
            resources = root / "resources"
            gen_pack_assets.generate(overrides, resources)
            self.assertEqual((1920, 1080),
                             dimensions(overrides / "config/fancymenu/assets/title_bg.png"))
            self.assertEqual((800, 200),
                             dimensions(overrides / "config/fancymenu/assets/title_logo.png"))
            tips = (overrides / "config/fancymenu/assets/loading_tips.txt").read_text().splitlines()
            self.assertGreaterEqual(len([t for t in tips if t.strip()]), 10)
            config = json.loads((overrides / "config/tips.json").read_text())
            self.assertIn("tips", config)
            map_dir = resources / "assets/inf_expedition/textures/gui/map"
            self.assertEqual((1024, 768), dimensions(map_dir / "background.png"))
            for node in ("battle", "elite", "boss", "shop", "rest", "event", "forge", "start"):
                self.assertEqual((32, 32), dimensions(map_dir / "nodes" / f"{node}.png"))
            self.assertEqual((16, 16), dimensions(map_dir / "player.png"))
            self.assertTrue((overrides / "config/fancymenu/customization/title_screen.txt").exists())


class PuffishSkillsConfigTest(unittest.TestCase):
    CATEGORY = (Path(__file__).resolve().parent.parent
                / "pack/overrides/kubejs/data/kubejs/puffish_skills/categories/difficulty")

    def test_experience_curve_is_positive(self):
        config = json.loads((self.CATEGORY / "experience.json").read_text())
        curve = config["experience_per_level"]
        self.assertEqual("expression", curve["type"])
        expression = curve["data"]["expression"]
        for level in range(0, 21):
            value = evaluate_curve(expression, level)
            self.assertGreaterEqual(
                value, 1,
                f"experience_per_level must be >= 1 at level {level}: "
                f"got {value} from {expression!r}")


if __name__ == "__main__":
    unittest.main()
