#!/usr/bin/env python3
"""Writes the block layouts of the modelled plants so the unformed structure follows the formed model.

    python tools/gen_plant_layouts.py [name ...]

Each plant is described in model pixels, the same units as tools/art/blockbench/plants.js: the
controller block spans x 0..16, the machine runs towards +z and every layer is 16 px high. Cell
(i, d, layer) covers x (i - half) * 16 .. +16, z d * 16 .. +16, y layer * 16 .. +16. Later calls win,
so draw the bulk first and the ports last. Symbols are PlantStructure.Part.SYMBOLS.
"""
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLANTS = ROOT / "src/main/resources/iuce/plants"


class Grid:
    def __init__(self, width, depth, layers):
        self.w, self.d, self.h = width, depth, layers
        self.cells = [[["." for _ in range(width)] for _ in range(depth)] for _ in range(layers)]

    def x(self, i):
        return (i - self.w // 2) * 16 + 8

    def z(self, d):
        return d * 16 + 8

    def set(self, i, d, y, ch):
        if 0 <= i < self.w and 0 <= d < self.d and 0 <= y < self.h:
            self.cells[y][d][i] = ch

    def fill(self, i0, i1, d0, d1, y0, y1, ch):
        for y in range(y0, y1 + 1):
            for d in range(d0, d1 + 1):
                for i in range(i0, i1 + 1):
                    self.set(i, d, y, ch)

    def ring(self, cx, cz, r0, r1, y0, y1, ch):
        """Cells whose centre lies between radius r0 and r1 of the point (cx, cz)."""
        for d in range(self.d):
            for i in range(self.w):
                if r0 <= math.hypot(self.x(i) - cx, self.z(d) - cz) < r1:
                    for y in range(y0, y1 + 1):
                        self.set(i, d, y, ch)

    def perimeter(self, y, ch, inset=0):
        for d in range(inset, self.d - inset):
            for i in range(inset, self.w - inset):
                if i in (inset, self.w - 1 - inset) or d in (inset, self.d - 1 - inset):
                    self.set(i, d, y, ch)

    def row(self, y, d, text):
        for i, ch in enumerate(text):
            if ch != " ":
                self.set(i, d, y, ch)

    def layers(self):
        return [["".join(r) for r in layer] for layer in self.cells]


def carbide_furnace_plant():
    g = Grid(11, 11, 18)
    cx, cz = 8, 88
    # Skid: checker plate deck, ports on the front and back rows.
    g.fill(0, 10, 0, 10, 0, 0, "c")
    g.row(0, 0, "    I@O    ")
    g.row(0, 10, "  SSSH     ")
    # Corner columns, ring beams under both decks and the top ring with the crane girder.
    for i, d in ((0, 0), (10, 0), (0, 10), (10, 10)):
        g.fill(i, i, d, d, 1, 17, "V")
    for y in (7, 11, 17):
        g.fill(1, 9, 0, 0, y, y, "B"); g.fill(1, 9, 10, 10, y, y, "B")
        g.fill(0, 0, 1, 9, y, y, "A"); g.fill(10, 10, 1, 9, y, y, "A")
    g.fill(1, 9, 5, 5, 17, 17, "B")
    # Furnace: brick foot, shell, conical hood and the roof the electrodes pass through.
    g.ring(cx, cz, 38, 62, 1, 1, "k")
    g.ring(cx, cz, 38, 62, 2, 6, "u")
    g.ring(cx, cz, 28, 50, 7, 7, "u")
    g.ring(cx, cz, 0, 34, 7, 7, "u")
    g.set(9, 5, 1, "k"); g.set(9, 5, 2, "k")
    # Main deck beams framing the roof opening, grating and handrail.
    g.fill(2, 2, 1, 9, 7, 7, "A"); g.fill(8, 8, 1, 9, 7, 7, "A")
    g.fill(3, 7, 2, 2, 7, 7, "B"); g.fill(3, 7, 8, 8, 7, 7, "B")
    for y in (8, 12):
        g.fill(1, 9, 1, 9, y, y, "g")
        g.perimeter(y, "f")
        for i, d in ((0, 0), (10, 0), (0, 10), (10, 10)):
            g.set(i, d, y, "V")
        g.set(1, 0, y, ".")
    g.fill(3, 7, 3, 7, 8, 8, ".")
    # Electrode deck beams, carried by the ring beams, and the opening round the electrodes.
    g.fill(3, 3, 1, 9, 11, 11, "A"); g.fill(7, 7, 1, 9, 11, 11, "A")
    g.fill(4, 6, 2, 2, 11, 11, "B"); g.fill(4, 6, 8, 8, 11, 11, "B")
    g.fill(4, 6, 3, 7, 12, 12, ".")
    # Transformer and radiators at the back, conservator on top; charging bins on the electrode deck.
    g.fill(3, 7, 9, 10, 1, 3, "t")
    g.fill(3, 7, 10, 10, 4, 4, "t")
    for i in (2, 8):
        g.fill(i, i, 3, 3, 13, 15, "t")
    # Off-gas flue up the back-left corner, CO line and cooling water up the front.
    g.fill(1, 1, 9, 9, 7, 17, "p")
    g.fill(6, 6, 0, 0, 2, 5, "p"); g.fill(6, 6, 0, 1, 6, 6, "q")
    g.fill(4, 4, 0, 0, 2, 3, "p")
    # Electrodes in a triangle round the furnace centre.
    for i, d in ((5, 4), (6, 6), (4, 6)):
        g.fill(i, i, d, d, 8, 16, "e")
    # Front ports: cooling water in, maintenance, furnace gas out.
    g.row(1, 0, "    LMQ    ")
    return {"base_y": 0, "minimum": 11, "maximum": 11, "repeat_start": 3, "repeat_size": 4, "layers": g.layers()}


def stand_at(depth, length, st):
    """Same rule as client/PlantModel.Stretch.standAt."""
    last = length - 1 - st["stand_last"]
    first, every = st["stand_first"], st["stand_every"]
    return depth == first or depth == last or (first < depth < last and (depth - first) % every == 0)


def stretched(width, height, minimum, maximum, st, head, bay, stand, tail, base_y=0):
    """One layout per length for a machine that grows the way its model does: head keeps its depths,
    tail(g, shift) moves back with the extra length, and every depth between them is a bay or a stand."""
    layouts = {}
    for length in range(minimum, maximum + 1):
        g = Grid(width, length, height)
        head(g)
        for d in range(st["bay_first"], length - st["bay_last"]):
            (stand if stand_at(d, length, st) else bay)(g, d)
        tail(g, length - st["base_length"])
        layouts[str(length)] = g.layers()
    return {"base_y": base_y, "minimum": minimum, "maximum": maximum, "repeat_start": 0, "repeat_size": 1,
            "layouts": layouts}


def ore():
    g = Grid(5, 7, 6)
    for d, row in enumerate(("SI@OS", "ccccc", "ScccS", "ccccc", "ScccS", "ccccc", "ScEcS")):
        g.row(0, d, row)
    # Frame over the crusher and the trommel: columns, cross beams on top and side beams between them.
    for d in (0, 2, 4):
        g.fill(0, 0, d, d, 1, 2, "V"); g.fill(4, 4, d, d, 1, 2, "V")
        g.row(3, d, "BBBBB")
    for d in (1, 3):
        g.set(0, d, 3, "A"); g.set(4, d, 3, "A")
    # Roll crusher in its housing, the feed hopper above it resting on the front cross beam.
    g.fill(1, 3, 0, 1, 1, 1, "C"); g.fill(1, 3, 0, 0, 2, 2, "C")
    g.fill(1, 3, 1, 1, 2, 2, "Z")
    g.fill(1, 3, 1, 1, 3, 3, "t"); g.fill(1, 3, 0, 1, 4, 4, "t")
    # Washing trommel on its trunnion stand, spray header along the middle cross beam.
    g.fill(1, 3, 2, 2, 1, 1, "C")
    g.fill(1, 3, 3, 4, 1, 2, "Y")
    g.fill(2, 2, 2, 4, 4, 4, "q")
    # Roasting kiln with its stack.
    g.fill(1, 3, 5, 6, 1, 3, "u")
    g.fill(2, 2, 6, 6, 4, 5, "p")
    return {"base_y": 0, "layouts": {"7": g.layers()}}


MILL_STRETCH = {"base_length": 9, "bay_first": 2, "bay_last": 2, "stand_first": 2, "stand_every": 4, "stand_last": 2}


def mill():
    # Layer 0 is the roll pit below the floor, layer 1 is floor level; the strip runs down the middle.
    def bay(g, d):
        g.row(0, d, "..CCCCC..")
        g.row(1, d, "..AZZZA..")

    def stand(g, d):
        # Four-high stand between two housing posts: backup roll in the pit, work rolls at the pass
        # line, top backup roll, then the crosshead with the screw-down. Spindles and motor on +x.
        g.row(0, d, "..VZZZVCC")
        g.row(1, d, "..VZZZVrC")
        g.row(2, d, "..VZZZV..")
        g.row(3, d, "..BBBBB..")
        g.row(4, d, "....C....")

    def head(g):
        g.row(0, 0, "CCCCCCCCC"); g.row(1, 0, "S.cI@Oc.S")
        g.row(0, 1, "CCCCCCCCC"); g.row(1, 1, "S.AZZZA.S")

    def tail(g, shift):
        d = 7 + shift
        g.row(0, d, "CCCCCCCCC"); g.row(1, d, "S.AZZZA.S")
        # Down-coiler: mandrel between two housing plates.
        d = 8 + shift
        g.row(0, d, "CCCCCCCCC"); g.row(1, d, "SECZZZC.S"); g.row(2, d, "..CZZZC..")

    return stretched(9, 5, 9, 17, MILL_STRETCH, head, bay, stand, tail, base_y=-1)


TWIN_STRETCH = {"base_length": 13, "bay_first": 3, "bay_last": 3, "stand_first": 3, "stand_every": 4, "stand_last": 3}


def twin_screw_extruder():
    def base(g, d):
        # Bed: checker plate, two sill beams with the barrel pedestal between them.
        g.fill(0, 4, d, d, 0, 0, "c")
        g.row(1, d, ".ACA.")

    def bay(g, d):
        base(g, d)
        g.set(2, d, 2, "G")
        g.set(0, d, 4, "A"); g.set(4, d, 4, "A")

    def stand(g, d):
        base(g, d)
        g.set(2, d, 2, "G")
        # Portal frame carrying the cable tray and the barrel cooling header.
        g.fill(0, 0, d, d, 1, 3, "V"); g.fill(4, 4, d, d, 1, 3, "V")
        g.row(4, d, "BBBBB")

    def head(g):
        g.row(0, 0, "cI@Oc"); g.row(1, 0, "VLMRV"); g.row(2, 0, "V.C.V"); g.row(3, 0, "V...V"); g.row(4, 0, "BBBBB")
        # Gearbox, then the first barrel segment with the feed hopper over it.
        base(g, 1); g.row(2, 1, ".CCC."); g.row(4, 1, "A...A")
        bay(g, 2); g.set(2, 2, 3, "t")

    def tail(g, shift):
        bay(g, 10 + shift)
        d = 11 + shift
        base(g, d); g.set(2, d, 2, "C"); g.row(4, d, "A...A")
        # Strand bath and pelletiser under the end portal.
        d = 12 + shift
        g.row(0, d, "ccHcc"); g.row(1, d, "VtttV"); g.row(2, d, "V.C.V"); g.row(3, d, "V...V"); g.row(4, d, "BBBBB")

    return stretched(5, 5, 13, 33, TWIN_STRETCH, head, bay, stand, tail)


COKE_STRETCH = {"base_length": 17, "bay_first": 2, "bay_last": 2, "stand_first": 3, "stand_every": 2, "stand_last": 3}


def coke_oven_battery():
    def oven(g, d):
        # One oven chamber running across the battery on its regenerator, the charging deck on top,
        # charging car rails, the ascension pipe and the gas collecting main.
        g.fill(1, 7, d, d, 0, 4, "k")
        g.set(0, d, 0, "c"); g.set(8, d, 0, "c")
        g.fill(1, 7, d, d, 5, 5, "c")
        g.set(2, d, 6, "A"); g.set(4, d, 6, "A")
        g.set(6, d, 6, "p")
        g.set(6, d, 7, "q")

    def stand(g, d):
        oven(g, d)
        # Buckstays either side of the oven wall, tied together over the top.
        g.fill(0, 0, d, d, 1, 7, "V"); g.fill(8, 8, d, d, 1, 7, "V")
        g.fill(1, 5, d, d, 8, 8, "B"); g.fill(7, 7, d, d, 8, 8, "B")
        g.set(0, d, 8, "B"); g.set(8, d, 8, "B"); g.set(6, d, 8, "B")

    def head(g):
        g.row(0, 0, "cccI@Occc")
        g.row(1, 0, "...LMRQ..")
        # Gas offtake from the collecting main down to the gas hatch, and the end buckstay frame.
        g.fill(6, 6, 0, 0, 2, 7, "p")
        stand(g, 1)

    def tail(g, shift):
        stand(g, 15 + shift)
        d = 16 + shift
        g.row(0, d, "cccSHSccc")
        # End wall: brick buttress and the waste-gas stack.
        g.fill(1, 7, d, d, 1, 3, "k")
        g.fill(1, 1, d, d, 4, 8, "k")

    return stretched(9, 9, 17, 33, COKE_STRETCH, head, oven, stand, tail)


def bio_ethylene_plant():
    g = Grid(9, 17, 9)
    g.fill(0, 8, 0, 16, 0, 0, "c")
    g.row(0, 0, "cccI@Occc")
    g.row(1, 0, "...LMRQ..")
    g.row(0, 16, "cccSHSccc")
    # Four fermenters: each a ring of shell plates with the manway on its dome.
    for ci, cd in ((1, 3), (7, 3), (1, 7), (7, 7)):
        for i in range(ci - 1, ci + 2):
            for d in range(cd - 1, cd + 2):
                if (i, d) != (ci, cd):
                    g.fill(i, i, d, d, 1, 5, "t")
        g.set(ci, cd, 6, "t")
    # Pipe rack down the middle: a bent every four blocks, three lines on top.
    for d in (2, 6, 10, 14):
        g.fill(3, 3, d, d, 1, 4, "V"); g.fill(5, 5, d, d, 1, 4, "V")
        g.fill(3, 5, d, d, 5, 5, "B")
    for d in range(3, 14):
        if d not in (6, 10):
            g.set(3, d, 5, "A"); g.set(5, d, 5, "A")
    g.fill(3, 5, 2, 14, 6, 6, "q")
    # Gas line from the rack down to the gas hatch.
    g.fill(6, 6, 1, 1, 1, 6, "p")
    # Ethanol column with a ring platform half way and its reflux drum.
    g.fill(1, 1, 12, 12, 1, 8, "t")
    for i in range(0, 3):
        for d in range(11, 14):
            if (i, d) != (1, 12):
                g.set(i, d, 5, "f")
    g.fill(0, 0, 14, 15, 1, 1, "t")
    # Dehydration reactor, its fired heater and the heater stack.
    g.fill(7, 7, 12, 12, 1, 5, "t")
    g.fill(6, 8, 14, 15, 1, 3, "u")
    g.fill(7, 7, 15, 15, 4, 8, "p")
    return {"base_y": 0, "minimum": 17, "maximum": 17, "repeat_start": 0, "repeat_size": 1, "layers": g.layers()}


def waste_heat_power_plant():
    g = Grid(9, 17, 9)
    g.fill(0, 8, 0, 16, 0, 0, "c")
    g.row(0, 0, "cccI@Occc")
    g.row(1, 0, "...LMR...")
    g.row(0, 16, "ccccJcccc")
    # Boiler: shell walls and roof, the steam drum along the roof.
    for y in range(1, 6):
        for d in range(2, 8):
            for i in range(2, 7):
                if i in (2, 6) or d in (2, 7):
                    g.set(i, d, y, "u")
    g.fill(2, 6, 2, 7, 6, 6, "u")
    g.fill(2, 6, 5, 5, 7, 7, "t")
    # Stair up the +x side to a landing on two columns behind the boiler, level with its roof.
    for k in range(6):
        g.set(8, 2 + k, 1 + k, "s")
    g.fill(7, 8, 8, 8, 1, 6, "V")
    g.fill(7, 8, 8, 8, 7, 7, "g")
    g.fill(7, 8, 8, 8, 8, 8, "f")
    # Flue duct from the back of the boiler into the brick stack.
    g.set(4, 8, 5, "q")
    g.fill(4, 4, 9, 9, 1, 8, "k")
    # Steam line from the drum along the roof, over a pipe support and down into the turbine.
    g.fill(6, 6, 6, 10, 7, 7, "q")
    g.fill(6, 6, 9, 9, 1, 6, "V")
    g.fill(6, 6, 11, 11, 4, 7, "p")
    # Turbine table: columns, deck and the condenser underneath; turbine and generator on the deck.
    for d in (11, 13, 15):
        g.set(3, d, 1, "V"); g.set(7, d, 1, "V")
    g.fill(3, 7, 11, 15, 2, 2, "c")
    g.fill(5, 5, 12, 13, 1, 1, "t")
    g.fill(4, 6, 11, 12, 3, 3, "C")
    g.fill(4, 6, 14, 15, 3, 3, "C")
    return {"base_y": 0, "minimum": 17, "maximum": 17, "repeat_start": 0, "repeat_size": 1, "layers": g.layers()}


BUILD = {"carbide_furnace_plant": carbide_furnace_plant, "ore": ore, "mill": mill, "twin_screw_extruder": twin_screw_extruder,
         "coke_oven_battery": coke_oven_battery, "bio_ethylene_plant": bio_ethylene_plant,
         "waste_heat_power_plant": waste_heat_power_plant}


def count(data):
    layers = data.get("layers") or next(iter(data["layouts"].values()))
    return sum(1 for layer in layers for row in layer for c in row if c not in ".@")


def main(names):
    for name in names or BUILD:
        data = BUILD[name]()
        path = PLANTS / f"{name}.json"
        path.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8", newline="\n")
        print(name, count(data), "blocks")


if __name__ == "__main__":
    main(sys.argv[1:])
