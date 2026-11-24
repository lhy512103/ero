// Structural building blocks: H-beams, checker plate, grating, handrail, ladder, process pipe,
// refractory, furnace and vessel shells, the screw barrel top and the plant roll drum.
// Same look as the process machines: steel greys with a faint grain, shading on bevels, seams and bolts.
// Run inside Blockbench (risky_eval) after core.js:
//   eval(fs.readFileSync('D:/Code/java/IUCE/tools/art/blockbench/structure.js', 'utf8')); IUCEStructure.paintAll()
(function () {
    const A = window.IUCEArt;
    const { paint, save, mix, light, dark, STEEL, FRAME, COPPER, HOT } = A;
    // Structural steel shares the process machines' greys so the plant reads as one set.
    const PAINT = [FRAME[0], FRAME[2], STEEL[1], STEEL[2], STEEL[3]];
    const YELLOW = [[96, 70, 14], [168, 124, 22], [222, 176, 40], [244, 210, 96]];
    const GALV = [STEEL[1], STEEL[2], STEEL[3], STEEL[4]];
    const BRICK = [[118, 84, 56], [150, 110, 76], [176, 138, 96], [204, 172, 128]];
    const MORTAR = [150, 144, 134];
    const VESSEL = [STEEL[1], STEEL[2], mix(STEEL[2], STEEL[3], 0.5), STEEL[3], STEEL[4]];
    const CERAMIC = [[118, 64, 40], [164, 94, 58], [198, 124, 82]];

    let grainSeed = 900;
    // A plate of colour c with the faint grain of the machine casings.
    const flat = (t, c) => A.noise(t, 0, 0, 15, 15, dark(c, 0.12), light(c, 0.1), grainSeed++);
    // A 2x2 bolt head lit from the top left.
    function boltP(t, x, y, pal) { t.px(x, y, pal[4]); t.px(x + 1, y, pal[2]); t.px(x, y + 1, pal[2]); t.px(x + 1, y + 1, pal[0]); }
    // Smooth round shading across x, for barrels and bars running vertically.
    function cyl(t, lo, hi, k) { for (let x = 0; x < 16; x++) { const c = mix(lo, hi, A.vcol(x) * (k || 0.9)); for (let y = 0; y < 16; y++) t.px(x, y, c); } }

    const PAINTS = {
        // ---- H-beam: painted flange face (model samples x = 2..14), web, edge, saw-cut end and joint gusset.
        h_beam_flange: () => paint(t => {
            flat(t, PAINT[2]);
            t.rect(2, 0, 2, 15, PAINT[4]); t.rect(3, 0, 3, 15, PAINT[3]);
            t.rect(12, 0, 12, 15, PAINT[1]); t.rect(13, 0, 13, 15, PAINT[0]);
            t.rect(7, 0, 7, 15, PAINT[1]); t.rect(8, 0, 8, 15, PAINT[3]);
            for (const y of [3, 11]) for (const x of [4, 10]) boltP(t, x, y, PAINT);
        }),
        h_beam_edge: () => paint(t => {
            flat(t, PAINT[3]);
            t.rect(2, 0, 2, 15, PAINT[4]); t.rect(4, 0, 4, 15, PAINT[1]);
        }),
        h_beam_web: () => paint(t => {
            flat(t, dark(PAINT[2], 0.25));
            t.rect(4, 0, 4, 15, PAINT[0]); t.rect(11, 0, 11, 15, PAINT[0]);
            t.rect(0, 6, 15, 6, PAINT[3]); t.rect(0, 7, 15, 8, PAINT[2]); t.rect(0, 9, 15, 9, PAINT[0]);
        }),
        h_beam_end: () => paint(t => {
            flat(t, PAINT[1]);
            t.rect(2, 2, 13, 4, STEEL[3]); t.rect(2, 11, 13, 13, STEEL[3]); t.rect(7, 5, 8, 10, STEEL[2]);
            t.rect(2, 2, 13, 2, STEEL[4]); t.rect(2, 11, 13, 11, STEEL[4]);
            t.bevel(0, 0, 15, 15, PAINT[3], PAINT[0]);
        }),
        h_beam_gusset: () => paint(t => {
            flat(t, PAINT[3]);
            t.bevel(0, 0, 15, 15, PAINT[4], PAINT[0]);
            t.bevel(1, 1, 14, 14, PAINT[3], PAINT[1]);
            for (const [x, y] of [[3, 3], [11, 3], [3, 7], [11, 7], [3, 11], [11, 11]]) boltP(t, x, y, PAINT);
        }),

        // ---- Checker plate: seamless tread field; the border lives in the trim overlay and item texture.
        checker_plate: () => paint(t => {
            flat(t, STEEL[2]);
            for (let y = 0; y < 16; y += 4) for (let x = 0; x < 16; x += 4) {
                if (((x + y) >> 2) & 1) { t.px(x + 1, y + 2, STEEL[4]); t.px(x + 2, y + 1, STEEL[4]); t.px(x + 2, y + 2, STEEL[1]); t.px(x + 3, y + 1, STEEL[1]); }
                else { t.px(x + 1, y + 1, STEEL[4]); t.px(x + 2, y + 2, STEEL[4]); t.px(x + 2, y + 1, STEEL[1]); t.px(x + 3, y + 2, STEEL[1]); }
            }
        }),
        checker_plate_item: () => paint(t => {
            const base = PAINTS.checker_plate();
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, base.get(x, y));
            t.bevel(0, 0, 15, 15, STEEL[3], STEEL[0]);
        }),
        checker_plate_trim: () => paint(t => {
            t.rect(0, 0, 15, 0, STEEL[0]); t.rect(0, 1, 15, 1, STEEL[3]);
        }),
        checker_plate_side: () => paint(t => {
            flat(t, PAINT[2]);
            t.rect(0, 0, 15, 0, PAINT[4]); t.rect(0, 1, 15, 1, PAINT[3]); t.rect(0, 2, 15, 2, PAINT[0]);
            t.rect(0, 13, 15, 13, PAINT[3]); t.rect(0, 14, 15, 14, PAINT[1]); t.rect(0, 15, 15, 15, PAINT[0]);
        }),
        checker_plate_side_item: () => paint(t => {
            const base = PAINTS.checker_plate_side();
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, base.get(x, y));
            t.rect(0, 3, 0, 12, PAINT[3]); t.rect(15, 3, 15, 12, PAINT[0]);
            for (const x of [3, 11]) boltP(t, x, 7, PAINT);
        }),

        // ---- Grating, handrail and ladder.
        steel_grating: () => paint(t => {
            for (let x = 0; x < 16; x += 3) for (let y = 0; y < 16; y++) t.px(x, y, y % 5 === 1 ? GALV[3] : GALV[2]);
            for (let y = 1; y < 16; y += 5) for (let x = 0; x < 16; x++) t.px(x, y, x % 3 === 0 ? GALV[3] : GALV[1]);
            t.rect(0, 0, 15, 0, GALV[2]); t.rect(0, 15, 15, 15, GALV[0]); t.rect(15, 0, 15, 15, GALV[1]);
        }),
        steel_grating_side: () => paint(t => {
            flat(t, GALV[2]);
            t.rect(0, 0, 15, 0, GALV[3]); t.rect(0, 15, 15, 15, GALV[0]);
        }),
        steel_handrail: () => paint(t => cyl(t, PAINT[0], PAINT[4], 1)),
        steel_ladder: () => paint(t => {
            for (const x0 of [2, 12]) for (let y = 0; y < 16; y++) { t.px(x0, y, PAINT[3]); t.px(x0 + 1, y, PAINT[1]); }
            for (const y of [2, 6, 10, 14]) {
                t.rect(2, y, 13, y, GALV[3]); t.rect(2, y + 1, 13, y + 1, GALV[0]);
                t.px(2, y, PAINT[4]); t.px(13, y, PAINT[0]);
            }
        }),

        // ---- Process pipe: plain round barrel (flanges are real geometry), cast fitting hub, flange face.
        process_pipe: () => paint(t => cyl(t, STEEL[0], STEEL[4])),
        process_pipe_hub: () => paint(t => {
            flat(t, FRAME[2]);
            t.bevel(0, 0, 15, 15, FRAME[4], FRAME[0]);
            t.bevel(2, 2, 13, 13, FRAME[1], FRAME[3]);
            for (const [x, y] of [[3, 3], [11, 3], [3, 11], [11, 11]]) A.bolt(t, x, y);
        }),
        process_pipe_end: () => paint(t => {
            flat(t, STEEL[3]);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 3.2) t.px(x, y, [22, 24, 28]); else if (d < 4.2) t.px(x, y, STEEL[1]);
            }
            for (let i = 0; i < 8; i++) { const a = i / 8 * Math.PI * 2; t.px(Math.round(7.5 + Math.cos(a) * 5.6), Math.round(7.5 + Math.sin(a) * 5.6), FRAME[0]); }
            t.bevel(0, 0, 15, 15, STEEL[4], FRAME[1]);
        }),

        // ---- Refractory and furnace shell.
        refractory_bricks: () => paint(t => {
            flat(t, MORTAR);
            const shade = [2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2];
            let k = 0;
            for (let row = 0; row < 4; row++) {
                const y0 = row * 4, off = row % 2 ? 4 : 0;
                for (let b = -1; b < 2; b++) {
                    const x0 = b * 8 + off, s = shade[k++ % shade.length];
                    for (let y = y0; y < y0 + 3; y++) for (let x = x0; x < x0 + 7; x++) {
                        if (x < 0 || x > 15) continue;
                        t.px(x, y, y === y0 ? BRICK[s + 1] : y === y0 + 2 ? BRICK[s - 1] : BRICK[s]);
                    }
                }
            }
        }),
        furnace_shell: () => paint(t => {
            flat(t, FRAME[2]);
            t.rect(0, 7, 15, 7, FRAME[0]); t.rect(0, 8, 15, 8, FRAME[3]);
            t.rect(11, 0, 11, 15, FRAME[4]); t.rect(12, 0, 12, 15, FRAME[3]); t.rect(13, 0, 13, 15, FRAME[0]);
            for (const y of [2, 12]) A.bolt(t, 3, y);
        }),

        // ---- Vessel shell: seamless interior (connected), border only on item texture and trim overlay.
        tank_shell: () => paint(t => {
            flat(t, VESSEL[2]);
            t.rect(3, 0, 4, 15, VESSEL[3]); t.rect(11, 0, 11, 15, VESSEL[1]);
        }),
        tank_shell_item: () => paint(t => {
            const base = PAINTS.tank_shell();
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, base.get(x, y));
            t.bevel(0, 0, 15, 15, VESSEL[4], VESSEL[0]);
        }),
        tank_shell_trim: () => paint(t => {
            t.rect(0, 0, 15, 0, VESSEL[0]); t.rect(0, 1, 15, 1, VESSEL[4]);
        }),

        // ---- Graphite electrode.
        graphite_electrode: () => paint(t => {
            cyl(t, [28, 28, 30], [104, 104, 108], 0.8);
            for (let x = 0; x < 16; x++) t.px(x, 0, [18, 18, 20]);
        }),
        graphite_electrode_end: () => paint(t => {
            flat(t, [70, 70, 74]);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 2.5) t.px(x, y, [22, 22, 24]); else if (d > 6.5 && d < 7.5) t.px(x, y, [54, 54, 58]);
            }
        }),

        // ---- Screw barrel top and the connected roll drum (hatches and segments live in parts.js).
        segment_top: () => paint(t => {
            flat(t, FRAME[2]);
            t.bevel(0, 0, 15, 15, FRAME[4], FRAME[0]);
            t.rect(3, 2, 12, 13, STEEL[2]); t.bevel(3, 2, 12, 13, STEEL[4], FRAME[0]);
            t.rect(6, 5, 9, 10, FRAME[1]);
        }),
        plant_drum: () => paint(t => {
            cyl(t, STEEL[0], STEEL[4]);
            for (let x = 0; x < 16; x++) { const c = mix(STEEL[0], STEEL[3], A.vcol(x)); t.px(x, 5, c); t.px(x, 10, c); }
        }),
        plant_drum_collar: () => paint(t => {
            cyl(t, FRAME[0], FRAME[4]);
            for (let x = 0; x < 16; x++) {
                const c = mix(COPPER[0], COPPER[3], A.vcol(x));
                t.px(x, 0, FRAME[0]); t.px(x, 1, c); t.px(x, 14, c); t.px(x, 15, FRAME[0]);
            }
        }),
        plant_bearing: () => paint(t => {
            flat(t, FRAME[2]);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 2.6) t.px(x, y, STEEL[4]); else if (d < 4.2) t.px(x, y, COPPER[2]); else if (d < 5.4) t.px(x, y, FRAME[0]);
            }
            t.bevel(0, 0, 15, 15, FRAME[4], FRAME[0]);
            for (const [x, y] of [[1, 1], [13, 1], [1, 13], [13, 13]]) A.bolt(t, x, y);
        }),
    };

    const PATH_MAP = {
        plant_drum: 'block/machine/plant/drum.png',
        plant_drum_collar: 'block/machine/plant/drum_collar.png',
        plant_bearing: 'block/machine/plant/bearing.png',
    };

    function paintAll() {
        const fs = A.fs, dir = A.TEX + 'block/structure/';
        if (!fs.existsSync(dir)) fs.mkdirSync(dir);
        const out = [];
        for (const name in PAINTS) out.push(save(PAINTS[name](), PATH_MAP[name] || ('block/structure/' + name + '.png')));
        return out.join(', ');
    }

    window.IUCEStructure = { PAINTS, paintAll };
    return 'structure loaded';
})();
