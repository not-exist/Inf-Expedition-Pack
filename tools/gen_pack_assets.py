#!/usr/bin/env python3
"""Generate the pack title art, loading tips and expedition map icons."""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
NODE_TYPES = ("battle", "elite", "boss", "shop", "rest", "event", "forge", "start")
NODE_COLORS = {
    "battle": (190, 107, 69, 255),
    "elite": (190, 69, 69, 255),
    "boss": (160, 35, 35, 255),
    "shop": (210, 170, 85, 255),
    "rest": (105, 175, 105, 255),
    "event": (145, 110, 195, 255),
    "forge": (235, 150, 55, 255),
    "start": (80, 150, 235, 255),
}

TIPS = [
    "每一锤都同时提高价值与碎裂风险，停手才是收益。",
    "大成功双倍加值，损伤只损失 10% 价值。",
    "狂热窗口内的三锤不会碎裂，留给最贵的一把刀。",
    "地图节点只能向前走，当前房间完成前不能移动。",
    "精英与首领需要更好的刀；精良是最低建议。",
    "锻造房间是安全屋，先把刀锻到满意再出门。",
    "商店用绿宝石换刀胚、锻锤和补给。",
    "休息营火每个房间每名玩家只能恢复一次。",
    "死亡会结束本局并清空背包，但 clears 元进度保留。",
    "通关 3 次后起始包会多一把锻锤。",
]


def _font(size: int, bold: bool = True) -> ImageFont.ImageFont:
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ]
    for candidate in candidates:
        if Path(candidate).is_file():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def _lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def _title_bg() -> Image.Image:
    width, height = 1920, 1080
    image = Image.new("RGBA", (width, height), (12, 9, 8, 255))
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / height
        draw.line((0, y, width, y), fill=_lerp((28, 20, 17, 255), (8, 6, 6, 255), t))
    random.seed(7)
    for _ in range(260):
        x = random.randrange(width)
        y = random.randrange(height // 2, height)
        radius = random.choice((1, 1, 2, 3))
        alpha = random.randrange(40, 190)
        draw.ellipse((x - radius, y - radius, x + radius, y + radius),
                     fill=(255, random.randrange(90, 190), 40, alpha))
    # Anvil silhouette.
    anvil = [(640, 700), (1280, 700), (1190, 760), (1120, 760),
             (1150, 850), (770, 850), (800, 760), (730, 760)]
    draw.polygon(anvil, fill=(10, 9, 9, 255))
    draw.polygon([(760, 700), (1160, 700), (1110, 735), (810, 735)], fill=(52, 47, 44, 255))
    # Forge glow behind the anvil.
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for radius in range(420, 40, -20):
        alpha = max(0, 70 - radius // 8)
        gdraw.ellipse((960 - radius, 760 - radius // 2, 960 + radius, 760 + radius // 2),
                      fill=(240, 100, 35, alpha))
    image = Image.alpha_composite(image, glow)
    draw = ImageDraw.Draw(image)
    font = _font(110)
    draw.text((960, 170), "THOUSAND HAMMERS", font=font, anchor="mm",
              fill=(232, 216, 176, 255))
    font_small = _font(42, bold=False)
    draw.text((960, 260), "INF EXPEDITION", font=font_small, anchor="mm",
              fill=(184, 168, 136, 255))
    # Vignette.
    vignette = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vignette)
    for inset in range(0, 260, 20):
        alpha = int(160 * (1 - inset / 260) ** 2)
        vdraw.rectangle((inset, inset, width - inset, height - inset),
                        outline=(0, 0, 0, alpha), width=20)
    return Image.alpha_composite(image, vignette)


def _title_logo() -> Image.Image:
    width, height = 800, 200
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((4, 4, width - 4, height - 4), radius=18,
                           outline=(217, 164, 65, 235), width=4,
                           fill=(20, 14, 11, 210))
    draw.text((width // 2, 72), "THOUSAND HAMMERS", font=_font(54),
              anchor="mm", fill=(240, 221, 170, 255))
    draw.text((width // 2, 140), "INF  ·  ENDLESS FORGE", font=_font(28, bold=False),
              anchor="mm", fill=(196, 160, 104, 255))
    return image


def _map_background() -> Image.Image:
    width, height = 1024, 768
    image = Image.new("RGBA", (width, height), (17, 14, 12, 255))
    draw = ImageDraw.Draw(image)
    random.seed(11)
    for _ in range(2400):
        x = random.randrange(width)
        y = random.randrange(height)
        shade = random.randrange(20, 48)
        draw.point((x, y), fill=(shade, shade - 3, shade - 6, 255))
    for x in range(0, width, 64):
        draw.line((x, 0, x, height), fill=(40, 33, 27, 90))
    for y in range(0, height, 64):
        draw.line((0, y, width, y), fill=(40, 33, 27, 90))
    draw.rectangle((14, 14, width - 14, height - 14), outline=(92, 71, 45, 220), width=3)
    return image


def _node_icon(kind: str) -> Image.Image:
    size = 32
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    color = NODE_COLORS[kind]
    draw.rounded_rectangle((2, 2, size - 3, size - 3), radius=7,
                           fill=color, outline=(20, 16, 13, 255), width=2)
    white = (248, 240, 220, 255)
    if kind == "battle":
        draw.line((9, 23, 23, 9), fill=white, width=3)
        draw.line((9, 9, 23, 23), fill=white, width=3)
    elif kind == "elite":
        points = []
        for index in range(10):
            angle = math.radians(-90 + index * 36)
            radius = 11 if index % 2 == 0 else 5
            points.append((16 + radius * math.cos(angle), 16 + radius * math.sin(angle)))
        draw.polygon(points, fill=white)
    elif kind == "boss":
        draw.ellipse((8, 8, 24, 24), fill=white)
        draw.ellipse((11, 13, 15, 17), fill=(40, 10, 10, 255))
        draw.ellipse((17, 13, 21, 17), fill=(40, 10, 10, 255))
        draw.line((12, 22, 20, 22), fill=(40, 10, 10, 255), width=2)
    elif kind == "shop":
        draw.ellipse((8, 8, 24, 24), outline=white, width=3)
        draw.text((16, 16), "¤", font=_font(18), anchor="mm", fill=white)
    elif kind == "rest":
        draw.polygon([(16, 7), (23, 22), (16, 18), (9, 22)], fill=white)
        draw.ellipse((13, 17, 19, 24), fill=(255, 190, 80, 255))
    elif kind == "event":
        draw.text((16, 15), "?", font=_font(22), anchor="mm", fill=white)
    elif kind == "forge":
        draw.rectangle((9, 9, 22, 13), fill=white)
        draw.line((15, 13, 15, 24), fill=white, width=4)
    else:
        draw.polygon([(16, 7), (27, 16), (20, 16), (20, 25),
                      (12, 25), (12, 16), (5, 16)], fill=white)
    return image


def _player_icon() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon([(8, 1), (15, 14), (8, 11), (1, 14)], fill=(255, 244, 210, 255),
                 outline=(20, 16, 13, 255))
    return image


def _title_screen_layout() -> str:
    return """type = fancymenu_layout

layout-meta {
  identifier = title_screen
  render_custom_elements_behind_vanilla = false
  last_edited_time = 0
  is_enabled = true
  randommode = false
  randomgroup = 1
  randomonlyfirsttime = false
  layout_index = 0
}

customization {
  action = autoscale
  basewidth = 1920
  baseheight = 1080
}

menu_background {
  image_path = [source:local]/config/fancymenu/assets/title_bg.png
  slide = false
  repeat_texture = false
  fallback_path = [source:local]/config/fancymenu/assets/title_bg.png
  parallax = false
  parallax_intensity = 0.02
  invert_parallax = false
  restart_animated_on_menu_load = false
  instance_identifier = 1c2f0a3e-3b3f-4a2d-9b5d-0a7b6c5d4e3f-0
  background_type = image
}

customization {
  action = backgroundoptions
  keepaspectratio = true
}

layout_action_executable_blocks {
}

element {
  source = [source:local]/config/fancymenu/assets/title_logo.png
  repeat_texture = false
  nine_slice_texture = false
  nine_slice_texture_border_x = 5
  nine_slice_texture_border_y = 5
  image_tint = #FFFFFF
  restart_animated_on_menu_load = false
  element_type = image
  instance_identifier = 7d8e9f01-2a3b-4c5d-8e6f-123456789abc-0
  appearance_delay = no_delay
  appearance_delay_seconds = 1.0
  fade_in_v2 = no_fading
  fade_in_speed = 1.0
  fade_out = no_fading
  fade_out_speed = 1.0
  base_opacity = 1.0
  auto_sizing = false
  auto_sizing_base_screen_width = 1920
  auto_sizing_base_screen_height = 1080
  sticky_anchor = false
  anchor_point = top-left
  x = 80
  y = 40
  width = 400
  height = 100
  stretch_x = false
  stretch_y = false
  stay_on_screen = true
  enable_parallax = false
  parallax_intensity_v2 = 0.5
  invert_parallax = false
  animated_offset_x = 0
  animated_offset_y = 0
  load_once_per_session = false
  in_editor_color = #FFC800FF
  layer_hidden_in_editor = false
  rotation_degrees = 0.0
  advanced_rotation_mode = false
  vertical_tilt_degrees = 0.0
  advanced_vertical_tilt_mode = false
  horizontal_tilt_degrees = 0.0
  advanced_horizontal_tilt_mode = false
}
"""


def generate(overrides: Path, resources: Path) -> None:
    overrides = Path(overrides)
    resources = Path(resources)
    fancy_assets = overrides / "config" / "fancymenu" / "assets"
    fancy_custom = overrides / "config" / "fancymenu" / "customization"
    map_dir = resources / "assets" / "inf_expedition" / "textures" / "gui" / "map"
    nodes_dir = map_dir / "nodes"
    for directory in (fancy_assets, fancy_custom, map_dir, nodes_dir):
        directory.mkdir(parents=True, exist_ok=True)

    _title_bg().save(fancy_assets / "title_bg.png")
    _title_logo().save(fancy_assets / "title_logo.png")
    (fancy_assets / "loading_tips.txt").write_text("\n".join(TIPS) + "\n", encoding="utf-8")
    (overrides / "config" / "tips.json").write_text(
        json.dumps({"tips": [{"type": "tips", "tip": tip} for tip in TIPS]},
                   ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (fancy_custom / "title_screen.txt").write_text(_title_screen_layout(), encoding="utf-8")

    _map_background().save(map_dir / "background.png")
    for kind in NODE_TYPES:
        _node_icon(kind).save(nodes_dir / f"{kind}.png")
    _player_icon().save(map_dir / "player.png")


def main() -> int:
    generate(ROOT / "pack" / "overrides", ROOT / "src" / "main" / "resources")
    print("generated pack assets")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
