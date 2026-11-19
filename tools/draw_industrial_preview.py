import json
from pathlib import Path
import struct
from zipfile import ZIP_DEFLATED, ZipFile
import zlib


ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "extras" / "iuce-industrial-preview"
ASSETS = PACK / "assets" / "iuce"
TEXTURES = ASSETS / "textures" / "block" / "industrial_preview"
MATERIAL = "iuce:block/industrial_preview/"
DARK = 0x212C34
BLACK = 0x171F25
LIP = 0x687881
AMBER = 0xD7A34E
TEAL = 0x69BBA8


def rect(canvas, left, top, right, bottom, color):
    for row in canvas[top:bottom]:
        row[left:right] = [color] * (right - left)


def steel():
    deltas = (0, 1, 2, 2, 0, -2, -3, -4, -4, -3, -2, 0, 1, 2, 1, 0)
    canvas = [[0x525E68 + delta * 0x010101] * 16 for delta in deltas]
    for left, top, right, delta in ((3, 2, 9, 3), (8, 5, 13, -3), (1, 8, 5, 2), (5, 11, 11, -3), (10, 13, 14, 3)):
        rect(canvas, left, top, right, top + 1, canvas[top][left] + delta * 0x010101)
    return canvas


def panel():
    canvas = steel()
    rect(canvas, 2, 2, 14, 14, DARK)
    rect(canvas, 2, 13, 14, 14, LIP)
    rect(canvas, 3, 3, 13, 13, 0x38444E)
    rect(canvas, 3, 3, 13, 4, BLACK)
    for column in (3, 12):
        for row in (4, 11):
            canvas[row][column] = 0x829298
            canvas[row + 1][column] = DARK
    rect(canvas, 5, 12, 11, 13, DARK)
    return canvas


def material_port(outgoing):
    canvas = panel()
    accent = AMBER if outgoing else TEAL
    rect(canvas, 5, 5, 11, 10, BLACK)
    rect(canvas, 5, 10, 11, 11, LIP)
    rect(canvas, 6, 9, 10, 10, 0x525E68)
    arrow = ((6, 7), (7, 7), (8, 7), (9, 7), (8, 6), (8, 8))
    for column, row in arrow:
        canvas[row][column if outgoing else 15 - column] = accent
    rect(canvas, 8 if outgoing else 5, 12, 11 if outgoing else 8, 13, accent)
    return canvas


def power_port(high_voltage):
    canvas = panel()
    accent = 0xDE8753 if high_voltage else AMBER
    rect(canvas, 5, 5, 11, 11, BLACK)
    for column in (5, 8):
        rect(canvas, column, 6, column + 3, 10, DARK)
        rect(canvas, column + 1, 7, column + 2, 9, accent)
    rect(canvas, 5, 10, 11, 11, LIP)
    for column in ((5, 7, 9) if high_voltage else (5,)):
        canvas[12][column] = accent
    return canvas


def controller(mill, formed):
    canvas = panel()
    rect(canvas, 5, 5, 11, 10, BLACK)
    rect(canvas, 5, 10, 11, 11, LIP)
    rect(canvas, 6, 6, 10, 9, 0x25463F if formed else 0x303C42)
    ink = TEAL if formed else 0x71828A
    if mill:
        rect(canvas, 6, 6, 10, 7, ink)
        rect(canvas, 6, 8, 10, 9, ink)
    else:
        for column, row in ((6, 8), (7, 7), (8, 6), (9, 7)):
            canvas[row][column] = ink
    canvas[12][5] = TEAL if formed else AMBER
    canvas[12][8] = canvas[12][10] = 0x525E68
    return canvas


def png(path, canvas):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    size = struct.pack(">2I5B", len(canvas[0]), len(canvas), 8, 2, 0, 0, 0)
    data = b"".join(b"\0" + b"".join(color.to_bytes(3, "big") for color in row) for row in canvas)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", size) + chunk(b"IDAT", zlib.compress(data, 9)) + chunk(b"IEND", b""))


def model(name, parent, **textures):
    path = ASSETS / "models" / "block" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"parent": f"minecraft:block/{parent}", "textures": textures}, indent=2) + "\n", encoding="utf-8")


def main():
    faces = {
        "steel": steel(),
        "item_input_hatch": material_port(False),
        "item_output_hatch": material_port(True),
        "energy_hatch": power_port(False),
        "energy_hatch_hv": power_port(True),
        "plant_controller": controller(False, False),
        "plant_controller_formed": controller(False, True),
        "mill_controller": controller(True, False),
        "mill_controller_formed": controller(True, True),
    }
    for name, face in faces.items():
        png(TEXTURES / f"{name}.png", face)
    model("plant_casing", "cube_all", all=MATERIAL + "steel")
    for name in ("item_input_hatch", "item_output_hatch", "energy_hatch", "energy_hatch_hv"):
        model(name, "cube_all", all=MATERIAL + name)
    for name in ("plant_controller", "plant_controller_formed", "mill_controller", "mill_controller_formed"):
        model(name, "orientable", top=MATERIAL + "steel", side=MATERIAL + "steel", front=MATERIAL + name)

    target = ROOT / "build" / "iuce-industrial-preview.zip"
    target.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(target, "w", ZIP_DEFLATED) as archive:
        archive.write(PACK / "pack.mcmeta", "pack.mcmeta")
        for file in sorted(ASSETS.rglob("*")):
            if file.is_file():
                archive.write(file, file.relative_to(PACK))


if __name__ == "__main__":
    main()
