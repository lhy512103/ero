import json
from pathlib import Path
import struct
import zlib

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/iuce"
TARGET = ASSETS / "textures/block/machine/plant"
ENAMEL = 0xD7E4E5
LIGHT = 0xEDF6F5
EDGE = 0xA4B6BA
METAL = 0x647580
DARK = 0x394650
DEEP = 0x202B33
GLASS = 0x17252C
OCHRE = 0xDC9946
CYAN = 0x69C8DC
GREEN = 0x8AE6A1


def rect(tile, left, top, right, bottom, color):
    for row in tile[top:bottom]:
        row[left:right] = [color] * (right - left)


def png(path, tile):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    size = struct.pack(">2I5B", 16, 16, 8, 2, 0, 0, 0)
    data = b"".join(b"\0" + b"".join(color.to_bytes(3, "big") for color in row) for row in tile)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", size) + chunk(b"IDAT", zlib.compress(data, 9)) + chunk(b"IEND", b""))


def blank(color):
    return [[color] * 16 for _ in range(16)]


def enamel():
    tile = blank(ENAMEL)
    for left, top, right, bottom, color in (
        (4, 7, 10, 8, 0xCFDEE0), (3, 8, 6, 9, 0xCFDEE0), (11, 11, 13, 12, 0xCFDEE0),
        (5, 2, 8, 3, 0xE0EDEE), (10, 4, 14, 5, 0xE0EDEE),
    ):
        rect(tile, left, top, right, bottom, color)
    return tile


def panel():
    tile = enamel()
    rect(tile, 1, 2, 15, 13, METAL)
    rect(tile, 2, 3, 14, 12, DARK)
    rect(tile, 3, 4, 13, 11, DEEP)
    rect(tile, 1, 12, 15, 13, LIGHT)
    for column in (1, 14):
        tile[3][column] = LIGHT
        tile[11][column] = EDGE
    return tile


def material_port(output):
    tile = panel()
    accent = OCHRE if output else CYAN
    rect(tile, 3, 5, 13, 10, GLASS)
    rect(tile, 4, 9, 12, 10, METAL)
    for row, span in ((5, (6, 10)), (6, (5, 11)), (7, (4, 12)), (8, (7, 9))):
        target_row = row if output else 13 - row
        rect(tile, span[0], target_row, span[1], target_row + 1, accent)
    rect(tile, 4, 12, 12, 13, accent)
    return tile


def power_port(high_voltage):
    tile = panel()
    for column in (4, 7, 10):
        rect(tile, column, 4, column + 2, 10, GLASS)
        rect(tile, column, 5, column + 1, 9, OCHRE if high_voltage else EDGE)
        rect(tile, column + 1, 6, column + 2, 8, LIGHT if high_voltage else CYAN)
    for column in ((5, 7, 9) if high_voltage else (7,)):
        tile[11][column] = OCHRE if high_voltage else CYAN
    return tile


def control(mill, formed):
    tile = panel()
    rect(tile, 3, 4, 11, 10, GLASS)
    rect(tile, 4, 5, 10, 9, 0x26444E if formed else 0x344854)
    accent = GREEN if formed else 0x73949F
    if mill:
        rect(tile, 4, 5, 10, 6, accent)
        rect(tile, 4, 8, 10, 9, accent)
        rect(tile, 5, 6, 6, 8, accent)
        rect(tile, 8, 6, 9, 8, accent)
    else:
        for column, top in ((4, 7), (5, 6), (6, 6), (7, 5), (8, 7), (9, 8)):
            rect(tile, column, top, column + 1, 9, accent)
    rect(tile, 12, 5, 13, 10, METAL)
    tile[5][12] = GREEN if formed else OCHRE
    tile[8][12] = CYAN
    rect(tile, 3, 10, 13, 11, EDGE)
    return tile


def girder():
    tile = blank(DARK)
    for left, right, color in ((0, 3, DARK), (3, 4, LIGHT), (4, 6, METAL), (6, 10, DEEP), (10, 12, METAL), (12, 13, EDGE)):
        rect(tile, left, 0, right, 16, color)
    rect(tile, 7, 0, 8, 16, 0x2B3942)
    return tile


def drum():
    tile = blank(DARK)
    for left, right, color in ((1, 3, METAL), (3, 4, DARK), (4, 6, EDGE), (6, 9, ENAMEL), (9, 10, LIGHT), (10, 12, EDGE), (12, 15, METAL)):
        rect(tile, left, 0, right, 16, color)
    rect(tile, 1, 1, 15, 2, DARK)
    rect(tile, 1, 2, 15, 3, OCHRE)
    rect(tile, 1, 3, 15, 4, DEEP)
    rect(tile, 1, 14, 15, 15, METAL)
    return tile


def bearing():
    tile = blank(DARK)
    rect(tile, 3, 3, 13, 13, EDGE)
    rect(tile, 4, 4, 12, 12, METAL)
    rect(tile, 5, 5, 11, 11, DEEP)
    rect(tile, 6, 6, 10, 10, OCHRE)
    rect(tile, 7, 7, 9, 9, DARK)
    return tile


def edge():
    tile = blank(DARK)
    rect(tile, 0, 1, 16, 2, METAL)
    rect(tile, 0, 2, 16, 3, LIGHT)
    return tile


def casing_models():
    directions = {"west": (0, False), "east": (0, True), "down": (1, False),
                  "up": (1, True), "north": (2, False), "south": (2, True)}
    multipart = [{"apply": {"model": "iuce:block/plant_casing_panel"}}]
    trims = []
    for edge_name, (cross_axis, positive) in directions.items():
        elements = []
        for face, (face_axis, outward) in directions.items():
            if face_axis == cross_axis:
                continue
            start, stop = [0, 0, 0], [16, 16, 16]
            start[cross_axis], stop[cross_axis] = (13, 16) if positive else (0, 3)
            offset = 0.01 + cross_axis * 0.003
            start[face_axis] = stop[face_axis] = 16 + offset if outward else -offset
            tangent_axis = 2 if face_axis == 0 else 0
            rotation = 0
            if cross_axis == tangent_axis:
                increasing = face in ("south", "west", "up", "down")
                rotation = 90 if positive == increasing else 270
            else:
                increasing = face in ("up", "down")
                rotation = 0 if positive != increasing else 180
            elements.append({"from": start, "to": stop, "faces": {face: {
                "uv": [0, 0, 16, 3], "rotation": rotation, "texture": "#trim", "cullface": face}}})
        name = f"plant_casing_edge_{edge_name}"
        model = {"parent": "minecraft:block/block", "textures": {"trim": "iuce:block/machine/plant/trim", "particle": "#trim"}, "elements": elements}
        trims.append((name, model))
        multipart.append({"when": {edge_name: "false"}, "apply": {"model": f"iuce:block/{name}"}})

    panel_model = {"parent": "minecraft:block/cube_all", "textures": {"all": "iuce:block/machine/plant/casing"}}
    for name, model in [("plant_casing_panel", panel_model), *trims]:
        (ASSETS / "models/block" / f"{name}.json").write_text(json.dumps(model, separators=(",", ":")) + "\n", encoding="utf-8")
    (ASSETS / "blockstates/plant_casing.json").write_text(json.dumps({"multipart": multipart}, indent=2) + "\n", encoding="utf-8")


def main():
    faces = {
        "casing": enamel(), "input_hatch": material_port(False), "output_hatch": material_port(True),
        "energy_hatch": power_port(False), "energy_hatch_hv": power_port(True),
        "controller": control(False, False), "controller_formed": control(False, True),
        "mill_controller": control(True, False), "mill_controller_formed": control(True, True),
        "girder": girder(), "drum": drum(), "bearing": bearing(), "trim": edge(),
    }
    for name, face in faces.items():
        png(TARGET / f"{name}.png", face)
    casing_models()


if __name__ == "__main__":
    main()
