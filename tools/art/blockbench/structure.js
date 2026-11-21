// Structural building blocks: H-beams, checker plate, grating, handrail, process pipe, refractory,
// furnace and vessel shells, and the gas output hatch. Run inside Blockbench (risky_eval) after core.js:
//   eval(fs.readFileSync('D:/Code/java/IUCE/tools/art/blockbench/structure.js', 'utf8')); IUCEStructure.paintAll()
// Writes textures/block/structure/*.png and textures/block/gas_output_hatch.png.
(function () {
    const A = window.IUCEArt;
    const { paint, save, noise, mix, light, dark, rng, bolt, STEEL, FRAME } = A;
    // Structural paint: the blue-grey primer common on plant steelwork.
    const PAINT = [[38, 48, 58], [58, 72, 86], [80, 98, 114], [108, 128, 144], [150, 168, 180]];
    const YELLOW = [[96, 70, 14], [168, 124, 22], [222, 176, 40], [244, 210, 96]];
    const GALV = [[70, 76, 80], [118, 124, 128], [160, 166, 168], [204, 208, 208]];
    const BRICK = [[118, 84, 56], [168, 126, 86], [198, 160, 112], [224, 196, 150]];
    const MORTAR = [176, 170, 160];

    // A painted surface with a little rust bleeding from the bottom edge.
    function painted(t, pal, seed, rust) {
        noise(t, 0, 0, 15, 15, pal[1], pal[3], seed, 0.5, 0.06);
        const r = rng(seed + 7);
        for (let i = 0; i < (rust || 0); i++) {
            const x = Math.floor(r() * 16), y = 12 + Math.floor(r() * 4);
            t.tint(x, y, [120, 66, 34], 0.35 + r() * 0.2);
            if (r() < 0.5) t.tint(x, y - 1, [120, 66, 34], 0.2);
        }
    }

    const PAINTS = {
        // H-beam flange face, running vertically: web weld lines either side and two bolt holes per metre.
        h_beam_flange: () => paint(t => {
            painted(t, PAINT, 11, 3);
            t.rect(0, 0, 0, 15, PAINT[0]); t.rect(15, 0, 15, 15, PAINT[0]);
            t.rect(1, 0, 1, 15, PAINT[4]); t.rect(14, 0, 14, 15, PAINT[1]);
            for (const y of [3, 11]) for (const x of [4, 10]) { t.px(x, y, FRAME[0]); t.px(x + 1, y, PAINT[1]); t.px(x, y + 1, PAINT[1]); t.px(x + 1, y + 1, PAINT[3]); }
        }),
        // Web between the flanges: in shadow, with a stiffener plate at mid height.
        h_beam_web: () => paint(t => {
            painted(t, PAINT.map(c => dark(c, 0.25)), 12, 2);
            t.rect(0, 7, 15, 8, PAINT[2]); t.rect(0, 9, 15, 9, PAINT[0]);
            t.rect(0, 6, 15, 6, PAINT[3]);
        }),
        // Saw-cut end: bright steel with a painted rim.
        h_beam_end: () => paint(t => {
            noise(t, 0, 0, 15, 15, STEEL[2], STEEL[4], 13, 0.5, 0.1);
            for (let y = 0; y < 16; y += 3) for (let x = 0; x < 16; x++) t.tint(x, y, STEEL[1], 0.25);
            t.bevel(0, 0, 15, 15, PAINT[2], PAINT[1]);
        }),
        // Diamond tread plate: matches the formed deck texture (brushed steel, crisp 2px lugs, weld border).
        checker_plate: () => paint(t => {
            noise(t, 0, 0, 15, 15, STEEL[2], STEEL[4], 21, 0.5, 0.06);
            for (let y = 0; y < 16; y += 4) for (let x = 0; x < 16; x += 4) {
                const o = (y >> 2) & 1 ? 2 : 0;
                if ((y >> 2) & 1) { t.px(x + o, y + 1, STEEL[4]); t.px(x + o + 1, y + 1, STEEL[1]); }
                else { t.px(x + 1, y, STEEL[4]); t.px(x + 1, y + 1, STEEL[1]); }
            }
            for (let i = 0; i < 16; i++) {
                t.px(i, 0, mix(STEEL[1], STEEL[2], (i * 5 % 4) / 4));
                t.px(0, i, mix(STEEL[1], STEEL[2], (i * 5 % 4) / 4));
                t.tint(i, 1, [0, 0, 0], 0.22);
                t.tint(1, i, [0, 0, 0], 0.22);
            }
        }),
        // Rolled channel side for checker plate & stairs, matching the formed skid texture.
        checker_plate_side: () => paint(t => {
            noise(t, 0, 0, 15, 15, PAINT[1], PAINT[3], 22, 0.5, 0.06);
            t.rect(0, 0, 15, 0, PAINT[4]); t.rect(0, 1, 15, 1, PAINT[3]); t.rect(0, 2, 15, 2, PAINT[0]);
            t.rect(0, 13, 15, 13, PAINT[3]); t.rect(0, 14, 15, 14, PAINT[1]); t.rect(0, 15, 15, 15, PAINT[0]);
            for (const x of [3, 11]) { bolt(t, x, 5); bolt(t, x, 9); }
            t.rect(15, 3, 15, 12, PAINT[0]);
        }),
        // Pressed bar grating: bearing bars along z, twisted cross bars, open in between.
        steel_grating: () => paint(t => {
            for (let x = 0; x < 16; x += 3) for (let y = 0; y < 16; y++) t.px(x, y, mix(GALV[1], GALV[3], (y % 4) / 6 + 0.2));
            for (let y = 1; y < 16; y += 5) for (let x = 0; x < 16; x++) t.px(x, y, x % 3 === 0 ? GALV[3] : GALV[2]);
            t.rect(0, 0, 15, 0, GALV[2]); t.rect(0, 15, 15, 15, GALV[0]);
            t.rect(15, 0, 15, 15, GALV[1]);
        }),
        // Edge strip of the grating, seen from the side.
        steel_grating_side: () => paint(t => {
            noise(t, 0, 0, 15, 15, GALV[1], GALV[2], 23, 0.5, 0.08);
            t.rect(0, 0, 15, 0, GALV[3]); t.rect(0, 15, 15, 15, GALV[0]);
        }),
        // Industrial tubular steel handrail: dark gunmetal steel with subtle brushed highlight.
        steel_handrail: () => paint(t => {
            for (let x = 0; x < 16; x++) {
                const c = x < 4 ? PAINT[4] : x < 9 ? PAINT[3] : x < 13 ? PAINT[2] : PAINT[1];
                for (let y = 0; y < 16; y++) t.px(x, y, c);
            }
            const r = rng(25);
            for (let i = 0; i < 10; i++) t.tint(Math.floor(r() * 16), Math.floor(r() * 16), [30, 36, 42], 0.25);
        }),
        // Industrial steel ladder: dark painted stringers with galvanized non-slip rungs and mounting brackets.
        steel_ladder: () => paint(t => {
            for (const x0 of [2, 12]) {
                for (let y = 0; y < 16; y++) {
                    t.px(x0, y, PAINT[3]); t.px(x0 + 1, y, PAINT[1]);
                }
            }
            for (const y of [2, 6, 10, 14]) {
                t.rect(2, y, 13, y, GALV[3]);
                t.rect(2, y + 1, 13, y + 1, GALV[0]);
                for (let x = 4; x <= 11; x += 2) t.px(x, y, GALV[2]);
                t.px(2, y, PAINT[4]); t.px(13, y, PAINT[0]);
            }
        }),
        // Process pipe: painted barrel with a bolted flange band at the top of each block.
        process_pipe: () => paint(t => {
            for (let x = 0; x < 16; x++) for (let y = 0; y < 16; y++) t.px(x, y, mix(STEEL[0], STEEL[4], A.vcol(x) * 0.9));
            for (let x = 0; x < 16; x++) {
                const s = A.vcol(x);
                t.px(x, 0, mix(FRAME[1], STEEL[4], s)); t.px(x, 1, mix(FRAME[1], STEEL[3], s)); t.px(x, 2, mix(FRAME[0], STEEL[1], s));
            }
            for (const x of [2, 7, 12]) t.px(x, 1, STEEL[4]);
            for (let y = 5; y < 16; y += 1) if (y % 5 === 0) t.px(4, y, dark(STEEL[2], 0.2));
        }),
        // Pipe end: flange face with bolt circle and a dark bore.
        process_pipe_end: () => paint(t => {
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 3.2) t.px(x, y, [14, 16, 18]);
                else if (d < 4.2) t.px(x, y, STEEL[1]);
                else t.px(x, y, mix(STEEL[2], STEEL[4], 0.3 + 0.4 * ((x + y) % 3 === 0)));
            }
            for (let i = 0; i < 8; i++) { const a = i / 8 * Math.PI * 2; t.px(7.5 + Math.cos(a) * 5.6, 7.5 + Math.sin(a) * 5.6, FRAME[0]); }
            t.bevel(0, 0, 15, 15, STEEL[4], FRAME[1]);
        }),
        // Firebrick, running bond, buff with soot near the joints.
        refractory_bricks: () => paint(t => {
            const r = rng(31);
            t.rect(0, 0, 15, 15, MORTAR);
            for (let row = 0; row < 4; row++) {
                const y0 = row * 4, off = row % 2 ? 4 : 0;
                for (let b = -1; b < 2; b++) {
                    const x0 = b * 8 + off, shade = 0.35 + r() * 0.35;
                    for (let y = y0; y < y0 + 3; y++) for (let x = x0; x < x0 + 7; x++) {
                        if (x < 0 || x > 15) continue;
                        t.px(x, y, mix(BRICK[1], BRICK[3], shade + (r() * 2 - 1) * 0.08 - (y === y0 + 2 ? 0.2 : 0)));
                    }
                }
            }
            for (let i = 0; i < 6; i++) t.tint(Math.floor(r() * 16), Math.floor(r() * 16), [60, 46, 38], 0.3);
        }),
        // Furnace shell: heavy plate tinted by heat, a horizontal weld seam and a vertical stiffener.
        furnace_shell: () => paint(t => {
            const r = rng(41);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const heat = 0.5 + 0.5 * Math.sin((x + y * 0.6) / 4);
                t.px(x, y, mix(mix(FRAME[1], FRAME[3], 0.5 + (r() * 2 - 1) * 0.1), [96, 70, 92], heat * 0.18));
            }
            t.rect(0, 7, 15, 7, FRAME[0]); t.rect(0, 8, 15, 8, FRAME[4]);
            for (let x = 1; x < 16; x += 2) t.px(x, 7, FRAME[2]);
            t.rect(11, 0, 12, 15, FRAME[3]); t.rect(13, 0, 13, 15, FRAME[0]);
            for (const y of [2, 12]) bolt(t, 3, y);
        }),
        // Vessel shell: light grey painted plate with staggered weld seams.
        tank_shell: () => paint(t => {
            noise(t, 0, 0, 15, 15, [170, 176, 178], [204, 208, 208], 51, 0.5, 0.06);
            for (let x = 0; x < 16; x++) { t.px(x, 0, [150, 154, 156]); t.px(x, 1, [214, 218, 218]); }
            for (let y = 2; y < 16; y++) { t.px(8, y, [150, 154, 156]); t.px(9, y, [214, 218, 218]); }
            const r = rng(52);
            for (let i = 0; i < 4; i++) { const x = Math.floor(r() * 16); t.tint(x, 15, [130, 96, 60], 0.3); t.tint(x, 14, [130, 96, 60], 0.15); }
        }),
        // Graphite electrode: dark grey with a faint sheen and the threaded nipple joint at the top.
        graphite_electrode: () => paint(t => {
            for (let x = 0; x < 16; x++) for (let y = 0; y < 16; y++) t.px(x, y, mix([28, 28, 30], [104, 104, 108], A.vcol(x) * 0.8));
            const r = rng(71);
            for (let i = 0; i < 14; i++) t.tint(Math.floor(r() * 16), Math.floor(r() * 16), [16, 16, 18], 0.4);
            for (let x = 0; x < 16; x++) t.px(x, 0, [18, 18, 20]);
        }),
        graphite_electrode_end: () => paint(t => {
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                t.px(x, y, d < 2.5 ? [20, 20, 22] : mix([58, 58, 62], [88, 88, 92], ((x * 7 + y * 3) % 5) / 5));
            }
        }),
        // Gas output hatch: casing frame, a flanged nozzle with a yellow gas band, and a pressure gauge.
        gas_output_hatch: () => paint(t => {
            A.casing(t, 61);
            A.roundWindow(t, 7.5, 7.5, 5, (x, y, d) => d < 2.6 ? [14, 16, 18] : d < 3.6 ? YELLOW[2] : STEEL[2]);
            A.gauge(t, 12, 12, 0.6);
        }),
    };

    function paintAll() {
        const fs = A.fs, dir = A.TEX + 'block/structure/';
        if (!fs.existsSync(dir)) fs.mkdirSync(dir);
        const out = [];
        for (const name in PAINTS) out.push(save(PAINTS[name](), (name === 'gas_output_hatch' ? 'block/' : 'block/structure/') + name + '.png'));
        return out.join(', ');
    }

    window.IUCEStructure = { PAINTS, paintAll };
    return 'structure loaded';
})();
