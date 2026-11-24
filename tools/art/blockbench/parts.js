// Multiblock part blocks: plant casing and trim, controllers, item / fluid / gas / energy / maintenance hatches
// and the extruder's screw-barrel segments. Painted in the same language as the process machine fronts
// (machines.js): steel plate with a faint grain, a dark outer frame and lit inner bevel, corner bolts, dark
// recessed openings and only small coloured accents.
// Run inside Blockbench after core.js:
//   eval(fs.readFileSync('D:/Code/java/IUCE/tools/art/blockbench/parts.js', 'utf8')); IUCEParts.paintAll()
(function () {
    const A = window.IUCEArt;
    const { paint, save, mix, light, dark, STEEL, FRAME, GLASS, HOT, COPPER } = A;
    const IN = [72, 150, 206];        // input accent
    const OUT = [222, 138, 48];       // output accent
    const WATER = [56, 116, 180];
    const GAS = [190, 182, 120];
    const SCREEN = [14, 34, 26];
    const INK = [110, 230, 140];
    const AMBER = [214, 150, 50];
    const HV = [196, 62, 48];
    const HAZARD = [222, 176, 40];

    const ARROW_DOWN = ['..#..', '..#..', '#.#.#', '.###.', '..#..'];
    const ARROW_UP = ['..#..', '.###.', '#.#.#', '..#..', '..#..'];
    const BOLT = ['..##', '.##.', '####', '.##.', '##..'];

    function icon(t, rows, x0, y0, c) {
        rows.forEach((row, y) => [...row].forEach((ch, x) => { if (ch === '#') t.px(x0 + x, y0 + y, c); }));
    }
    // A square port sunk into the plate, its rim painted in the accent colour.
    function port(t, x0, y0, x1, y1, rim, fill) {
        t.bevel(x0 - 1, y0 - 1, x1 + 1, y1 + 1, light(rim, 0.25), dark(rim, 0.4));
        A.recess(t, x0, y0, x1, y1, fill || GLASS);
    }
    // A round pipe flange with four bolts, filled by fill(x, y, d).
    function flange(t, rim, fill) {
        A.roundWindow(t, 7.5, 7.5, 4.6, (x, y, d) => d < 3.2 ? fill(x, y, d) : mix(dark(rim, 0.3), light(rim, 0.2), x + y < 15 ? 0.9 : 0.1));
        for (const [x, y] of [[7, 2], [7, 12], [2, 7], [12, 7]]) { t.px(x, y, STEEL[4]); t.px(x + 1, y, STEEL[2]); t.px(x, y + 1, STEEL[2]); t.px(x + 1, y + 1, FRAME[0]); }
    }
    function vent(t, x0, y0, x1, y1) {
        t.rect(x0, y0, x1, y1, FRAME[1]);
        for (let y = y0 + 1; y < y1; y += 2) t.rect(x0 + 1, y, x1 - 1, y, FRAME[0]);
        t.bevel(x0, y0, x1, y1, STEEL[1], STEEL[4]);
    }

    // ---- controllers: a screen above a button strip and a status lamp
    function controller(t, seed, formed, picture) {
        A.casing(t, seed);
        A.recess(t, 3, 3, 12, 9, SCREEN);
        picture(t, formed);
        A.glare(t, 4, 4);
        for (const x of [4, 6, 8]) { t.px(x, 11, formed ? STEEL[4] : STEEL[3]); t.px(x, 12, FRAME[0]); }
        t.px(8, 11, formed ? [120, 200, 230] : [60, 90, 110]);
        A.lamp(t, 11, 11, formed);
    }
    function plantScreen(t, formed) {
        const ink = formed ? INK : AMBER, dim = dark(ink, 0.45);
        if (formed) { t.rect(5, 5, 7, 5, ink); t.rect(5, 7, 10, 7, dim); t.px(10, 5, ink); t.px(9, 6, ink); t.px(8, 5, dim); }
        else { t.rect(5, 5, 10, 5, dim); t.rect(5, 7, 8, 7, dim); t.px(7, 6, ink); t.px(7, 8, ink); }
    }
    function millScreen(t, formed) {
        const ink = formed ? INK : AMBER, dim = dark(ink, 0.45);
        for (const cx of [6, 9]) for (let y = 4; y <= 8; y++) for (let x = cx - 2; x <= cx + 2; x++) {
            const d = Math.hypot(x - cx, y - 6);
            if (d > 1.1 && d < 2.1) t.px(x, y, ink); else if (d <= 1.1) t.px(x, y, dim);
        }
        if (!formed) { t.px(7, 8, ink); t.px(8, 8, ink); }
    }

    // ---- energy: copper terminals on insulators, an HV stripe on the high-voltage hatch
    function energyHatch(t, seed, hv) {
        A.casing(t, seed);
        A.recess(t, 3, 3, 12, 10, FRAME[1]);
        for (const x0 of [4, 9]) {
            t.rect(x0, 6, x0 + 2, 9, [220, 214, 196]); t.rect(x0, 9, x0 + 2, 9, [170, 164, 148]);
            t.rect(x0, 4, x0 + 2, 5, COPPER[2]); t.px(x0, 4, COPPER[3]); t.rect(x0 + 2, 4, x0 + 2, 5, COPPER[0]);
        }
        if (hv) for (let x = 2; x <= 13; x++) t.px(x, 12, (x >> 1) % 2 ? HV : FRAME[0]);
        else icon(t, BOLT, 6, 11, HAZARD);
    }

    const PAINTS = {
        // ---- plant casing: a seamless plate, joined edges carry the trim overlay
        casing: () => paint(t => {
            A.noise(t, 0, 0, 15, 15, STEEL[1], STEEL[3], 31);
            t.rect(0, 7, 15, 7, STEEL[1]); t.rect(0, 8, 15, 8, light(STEEL[2], 0.12));
        }),
        trim: () => paint(t => {
            t.rect(0, 0, 15, 0, FRAME[0]); t.rect(0, 1, 15, 1, STEEL[4]); t.rect(0, 2, 15, 2, STEEL[1]);
            for (const x of [3, 12]) { t.px(x, 1, STEEL[2]); t.px(x, 2, FRAME[0]); }
        }),
        vent: () => paint(t => { A.casing(t, 33); vent(t, 4, 4, 11, 11); }),
        // Sides of the flanged and service hatches: the bare bolted casing their fronts are drawn on.
        hatch_side: () => paint(t => A.casing(t, 46)),
        // Top and bottom of every hatch: the same bolted casing as its front, with a plain inset cover.
        hatch_top: () => paint(t => {
            A.casing(t, 45);
            t.bevel(4, 4, 11, 11, FRAME[0], STEEL[4]);
            t.bevel(5, 5, 10, 10, STEEL[4], STEEL[1]);
        }),

        controller: () => paint(t => controller(t, 34, false, plantScreen)),
        controller_formed: () => paint(t => controller(t, 34, true, plantScreen)),
        mill_controller: () => paint(t => controller(t, 35, false, millScreen)),
        mill_controller_formed: () => paint(t => controller(t, 35, true, millScreen)),

        // ---- item hatches: a square chute, blue rim and arrow in, orange rim and arrow out
        input_hatch: () => paint(t => {
            A.casing(t, 36); port(t, 4, 4, 11, 11, IN);
            for (let y = 5; y <= 10; y += 2) t.rect(5, y, 10, y, FRAME[1]);
            icon(t, ARROW_DOWN, 6, 5, IN);
        }),
        output_hatch: () => paint(t => {
            A.casing(t, 37); port(t, 4, 4, 11, 11, OUT);
            for (let y = 5; y <= 10; y += 2) t.rect(5, y, 10, y, FRAME[1]);
            icon(t, ARROW_UP, 6, 6, OUT);
        }),
        energy_hatch: () => paint(t => energyHatch(t, 38, false)),
        energy_hatch_hv: () => paint(t => energyHatch(t, 39, true)),

        // ---- fluid and gas hatches: a bolted pipe flange
        fluid_input_hatch: () => paint(t => {
            A.casing(t, 40);
            flange(t, IN, (x, y, d) => y >= 8 ? mix(WATER, dark(WATER, 0.5), (y - 8) / 4) : y === 7 ? light(WATER, 0.35) : GLASS);
        }),
        fluid_output_hatch: () => paint(t => {
            A.casing(t, 41);
            flange(t, OUT, (x, y, d) => y >= 8 ? mix(WATER, dark(WATER, 0.5), (y - 8) / 4) : y === 7 ? light(WATER, 0.35) : GLASS);
        }),
        gas_output_hatch: () => paint(t => {
            A.casing(t, 42);
            flange(t, OUT, (x, y, d) => d < 1.2 ? GLASS : mix(dark(GAS, 0.55), dark(GAS, 0.2), ((x + y) % 3) / 3));
            A.gauge(t, 12, 12, 0.6);
        }),
        generation_hatch: () => paint(t => {
            A.casing(t, 43);
            A.recess(t, 3, 3, 12, 12, FRAME[1]);
            for (const y of [5, 10]) { t.rect(4, y, 11, y, COPPER[2]); t.rect(4, y + 1, 11, y + 1, COPPER[0]); }
            icon(t, BOLT, 6, 5, OUT); t.px(7, 7, light(OUT, 0.4));
        }),
        maintenance_hatch: () => paint(t => {
            A.casing(t, 44);
            t.rect(3, 3, 12, 12, STEEL[2]); t.bevel(3, 3, 12, 12, STEEL[4], FRAME[0]);
            for (let x = 4; x <= 11; x++) t.px(x, 4, ((x >> 1) % 2) ? HAZARD : FRAME[0]);
            t.rect(4, 6, 4, 7, FRAME[1]); t.rect(4, 10, 4, 11, FRAME[1]);          // hinges
            t.rect(10, 7, 10, 10, STEEL[4]); t.rect(11, 7, 11, 10, FRAME[0]);      // handle
        }),
    };

    // ---- screw-barrel segments: flanged barrel sections that join side by side.
    // The barrel is shaded in a few flat steps with the casings' faint grain, not a smooth gradient,
    // and the same shell is used on the top and bottom so every face reads as one bolted part.
    const BARREL = [STEEL[1], STEEL[2], STEEL[3], STEEL[4], STEEL[4], STEEL[3], STEEL[2], STEEL[2], STEEL[1], FRAME[1]];
    function segmentShell(t, seed, accent) {
        A.noise(t, 0, 0, 15, 2, FRAME[1], FRAME[3], seed);
        A.noise(t, 0, 13, 15, 15, FRAME[1], FRAME[3], seed + 100);
        t.rect(0, 0, 15, 0, FRAME[0]); t.rect(0, 1, 15, 1, FRAME[4]); t.rect(0, 2, 15, 2, FRAME[1]);
        t.rect(0, 13, 15, 13, FRAME[3]); t.rect(0, 14, 15, 14, FRAME[1]); t.rect(0, 15, 15, 15, FRAME[0]);
        for (let y = 3; y <= 12; y++) { const c = BARREL[y - 3]; A.noise(t, 0, y, 15, y, dark(c, 0.08), light(c, 0.06), seed * 16 + y); }
        for (let y = 3; y <= 12; y++) { t.tint(0, y, FRAME[0], 0.45); t.tint(15, y, [255, 255, 255], 0.12); }
        for (const x of [2, 7, 12]) { t.px(x, 1, STEEL[4]); t.px(x + 1, 1, STEEL[2]); t.px(x, 14, STEEL[3]); t.px(x + 1, 14, FRAME[0]); }
        accent(t);
    }
    const SEGMENTS = {
        main_feed_segment: g => { A.recess(g, 4, 4, 11, 9, [22, 26, 30]); g.rect(5, 5, 10, 5, FRAME[2]); icon(g, ARROW_DOWN, 6, 5, IN); },
        melting_segment: g => {
            for (const x0 of [2, 9]) { g.rect(x0, 4, x0 + 4, 11, COPPER[1]); g.rect(x0, 4, x0 + 4, 4, COPPER[2]); g.rect(x0, 11, x0 + 4, 11, COPPER[0]); g.px(x0 + 2, 7, HOT[2]); }
        },
        side_feed_segment: g => {
            A.roundWindow(g, 7.5, 7.5, 3.8, (x, y, d) => d < 2.0 ? [20, 24, 28] : IN);
        },
        mixing_segment: g => { for (const x of [3, 7, 11]) { g.rect(x, 4, x, 11, STEEL[4]); g.rect(x + 1, 4, x + 1, 11, FRAME[0]); } },
        venting_segment: g => { vent(g, 4, 4, 9, 10); A.gauge(g, 12, 8, 0.5); },
        die_segment: g => {
            g.rect(3, 4, 12, 11, FRAME[1]); g.bevel(3, 4, 12, 11, STEEL[3], FRAME[0]);
            for (const x of [5, 7, 9, 11]) { g.px(x, 7, HOT[2]); g.px(x, 8, HOT[3]); }
        },
    };

    const DIR = { fluid_input_hatch: 'block/', fluid_output_hatch: 'block/', gas_output_hatch: 'block/', generation_hatch: 'block/', maintenance_hatch: 'block/' };

    function paintAll() {
        const out = [];
        for (const name in PAINTS) out.push(save(PAINTS[name](), (DIR[name] || 'block/machine/plant/') + name + '.png'));
        let seed = 50;
        for (const name in SEGMENTS) { const s = seed++; out.push(save(paint(t => segmentShell(t, s, SEGMENTS[name])), 'block/' + name + '.png')); }
        out.push(save(paint(t => segmentShell(t, 49, () => {})), 'block/structure/segment_top.png'));
        return out.length + ' textures';
    }

    window.IUCEParts = { PAINTS, SEGMENTS, paintAll };
    return 'parts loaded';
})();
