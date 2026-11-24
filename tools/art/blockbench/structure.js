// Structural building blocks: H-beams, checker plate, grating, handrail, ladder, process pipe,
// refractory, furnace and vessel shells, screw segments, plant casing/drum, and gas output hatch.
// Painted in a crisp Minecraft pixel-art style (quantized palette steps, 2x2 pixel clusters,
// pixel-perfect bevels & rivets, zero blurry gradients or diagonal moiré) inside Blockbench:
//   eval(fs.readFileSync('D:/Code/java/IUCE/tools/art/blockbench/structure.js', 'utf8')); IUCEStructure.paintAll()
(function () {
    const A = window.IUCEArt;
    const { paint, save, mix, light, dark, rng, bolt, STEEL, FRAME, COPPER, HOT } = A;

    // Crisp Minecraft-style stepped palettes (vanilla iron/netherite/polished-deepslate feel)
    // Heavy structural slate-iron (H-beams, channels, skids, buckstays)
    const IRON_DARK = [
        [28, 32, 38],   // 0: deep shadow / seam
        [46, 52, 60],   // 1: dark base
        [66, 74, 84],   // 2: mid slate iron
        [88, 98, 110],  // 3: lit slate iron
        [118, 128, 140],// 4: crisp bevel highlight
        [152, 162, 174] // 5: specular bolt / corner
    ];
    // Clean rolled steel (checker plate, machine panels, pipe barrels)
    const ROLLED = [
        [52, 56, 62],   // 0: shadow seam
        [84, 90, 98],   // 1: dark tone
        [118, 124, 132],// 2: mid steel
        [146, 152, 160],// 3: main lit plate
        [174, 180, 188],// 4: highlight
        [204, 210, 216] // 5: bright rim
    ];
    // Brushed stainless vessel cladding (tank_shell)
    const VESSEL = [
        [88, 94, 102],  // 0: deep weld shadow
        [124, 130, 138],// 1: low tone
        [156, 162, 170],// 2: mid stainless
        [180, 186, 192],// 3: main stainless
        [202, 208, 214],// 4: highlight
        [224, 228, 232] // 5: specular rim
    ];
    const YELLOW = [[96, 70, 14], [168, 124, 22], [216, 172, 44], [242, 208, 96]];
    const GALV = [[56, 62, 68], [96, 102, 110], [138, 144, 152], [182, 188, 196]];
    const BRICK = [[92, 60, 40], [136, 94, 62], [172, 126, 86], [202, 158, 114]];
    const MORTAR = [76, 70, 64];
    const CERAMIC = [[118, 64, 40], [164, 94, 58], [198, 124, 82]];

    // Crisp MC block-style clustered pixel grain (2x1 / 2x2 patches, strictly quantized to palette steps, tileable 16x16)
    function mcPlate(t, pal, baseIdx, seed, patches) {
        t.rect(0, 0, 15, 15, pal[baseIdx]);
        const r = rng(seed);
        const count = patches === undefined ? 18 : patches;
        for (let i = 0; i < count; i++) {
            const px = Math.floor(r() * 8) * 2;
            const py = Math.floor(r() * 8) * 2;
            const w = r() < 0.65 ? 2 : 4;
            const h = r() < 0.7 ? 1 : 2;
            const tone = pal[Math.max(0, Math.min(pal.length - 1, baseIdx + (r() < 0.5 ? -1 : 1)))];
            for (let dy = 0; dy < h; dy++) {
                for (let dx = 0; dx < w; dx++) {
                    t.px((px + dx) & 15, (py + dy) & 15, tone);
                }
            }
        }
    }

    // Stepped MC cylindrical shading (discrete pixel bands like vanilla pistons/logs/cauldrons, not smooth float sine)
    const CYL_BANDS = [0, 1, 2, 3, 4, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0];
    function mcCylinderV(t, pal, seed) {
        const r = rng(seed);
        for (let x = 0; x < 16; x++) {
            const idx = CYL_BANDS[x];
            for (let y = 0; y < 16; y++) {
                let step = idx;
                if (((x + (y >> 1)) & 3) === 0 && r() < 0.28) step = Math.max(0, idx - 1);
                t.px(x, y, pal[step]);
            }
        }
    }

    function mcRivet(t, x, y, hi, lo) {
        t.px(x, y, hi);
        t.px(x + 1, y, lo);
    }

    function mcBolt2(t, x, y, pal) {
        t.px(x, y, pal[5]); t.px(x + 1, y, pal[3]);
        t.px(x, y + 1, pal[2]); t.px(x + 1, y + 1, pal[0]);
    }

    function segmentShell(t, seed, accentDraw) {
        mcPlate(t, IRON_DARK, 2, seed, 14);
        // Top and bottom bolted clamshell flanges
        t.rect(0, 0, 15, 0, IRON_DARK[4]); t.rect(0, 1, 15, 1, IRON_DARK[2]); t.rect(0, 2, 15, 2, IRON_DARK[0]);
        t.rect(0, 13, 15, 13, IRON_DARK[3]); t.rect(0, 14, 15, 14, IRON_DARK[1]); t.rect(0, 15, 15, 15, IRON_DARK[0]);
        // Stepped cylindrical barrel band (rows 3..12)
        const rowStep = [1, 2, 3, 4, 5, 4, 3, 2, 1, 0];
        for (let y = 3; y <= 12; y++) {
            const c = ROLLED[rowStep[y - 3]];
            for (let x = 1; x <= 14; x++) t.px(x, y, c);
        }
        t.rect(0, 3, 0, 12, IRON_DARK[0]); t.rect(15, 3, 15, 12, IRON_DARK[0]);
        for (const x of [2, 7, 13]) { t.px(x, 1, ROLLED[5]); t.px(x, 14, ROLLED[4]); }
        if (accentDraw) accentDraw(t);
    }

    const PAINTS = {
        // ---- H-beam (iuce:plant_frame): crisp stepped slate-iron girder without diagonal moiré stripes!
        // Flange outer face (uv x=2..14): crisp bright left bevel, stepped iron body, centre web root line, bolt pairs.
        h_beam_flange: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 101, 14);
            // Outer flange edges at x=2..3 and x=12..13 (since model samples uv x=2..14)
            t.rect(0, 0, 1, 15, IRON_DARK[3]);
            t.rect(2, 0, 2, 15, IRON_DARK[4]);
            t.rect(3, 0, 3, 15, IRON_DARK[3]);
            t.rect(12, 0, 12, 15, IRON_DARK[2]);
            t.rect(13, 0, 13, 15, IRON_DARK[0]);
            t.rect(14, 0, 15, 15, IRON_DARK[0]);
            // Subtle centre web root shadow & highlight along x=7..8
            t.rect(7, 0, 7, 15, IRON_DARK[3]);
            t.rect(8, 0, 8, 15, IRON_DARK[1]);
            // Crisp structural rivets along flange wings
            for (const y of [3, 11]) {
                mcRivet(t, 4, y, IRON_DARK[5], IRON_DARK[0]);
                mcRivet(t, 10, y, IRON_DARK[5], IRON_DARK[0]);
            }
        }),
        // Flange side thickness edge (2.5 px wide): clean stepped iron strip, seamless end-to-end
        h_beam_edge: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 102, 10);
            t.rect(2, 0, 2, 15, IRON_DARK[4]);
            t.rect(4, 0, 4, 15, IRON_DARK[0]);
            t.rect(11, 0, 11, 15, IRON_DARK[4]);
            t.rect(13, 0, 13, 15, IRON_DARK[0]);
        }),
        // Recessed H-beam web (uv x=4.5..11.5): darker recessed iron with crisp bolted stiffener rib
        h_beam_web: () => paint(t => {
            mcPlate(t, IRON_DARK, 1, 103, 12);
            t.rect(4, 0, 5, 15, IRON_DARK[0]);
            t.rect(10, 0, 11, 15, IRON_DARK[0]);
            // Transverse stiffener rib across the web middle
            t.rect(0, 7, 15, 7, IRON_DARK[3]);
            t.rect(0, 8, 15, 8, IRON_DARK[0]);
            mcRivet(t, 7, 3, IRON_DARK[4], IRON_DARK[0]);
            mcRivet(t, 7, 12, IRON_DARK[4], IRON_DARK[0]);
        }),
        // Saw-cut machined steel H-section end
        h_beam_end: () => paint(t => {
            mcPlate(t, ROLLED, 3, 104, 10);
            t.bevel(2, 2, 13, 4, ROLLED[5], IRON_DARK[0]);
            t.bevel(2, 11, 13, 13, ROLLED[5], IRON_DARK[0]);
            t.rect(6, 4, 9, 11, ROLLED[2]);
        }),
        // Bolted gusset / splice node plate for perpendicular H-beam joints
        h_beam_gusset: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 105, 10);
            t.bevel(1, 1, 14, 14, IRON_DARK[4], IRON_DARK[0]);
            t.bevel(2, 2, 13, 13, IRON_DARK[3], IRON_DARK[1]);
            for (const [x, y] of [[3, 3], [11, 3], [3, 11], [11, 11], [3, 7], [11, 7]]) {
                mcBolt2(t, x, y, IRON_DARK);
            }
        }),

        // ---- Checker plate (iuce:checker_plate): seamless interior field + separate edge trim!
        checker_plate: () => paint(t => {
            // Seamless 16x16 anti-slip tread plate (no outer border so connected plates merge cleanly!)
            mcPlate(t, ROLLED, 3, 201, 14);
            // Crisp MC-style 2-pixel alternating raised tread lugs on an 8x8 grid
            for (let gy = 0; gy < 16; gy += 8) {
                for (let gx = 0; gx < 16; gx += 8) {
                    const alt = ((gx >> 3) + (gy >> 3)) & 1;
                    if (alt === 0) {
                        // Horizontal 3x1 lug + 1px shadow
                        t.rect(gx + 2, gy + 3, gx + 4, gy + 3, ROLLED[5]);
                        t.rect(gx + 2, gy + 4, gx + 4, gy + 4, ROLLED[1]);
                        t.rect(gx + 3, gy + 6, gx + 5, gy + 6, ROLLED[4]);
                        t.rect(gx + 3, gy + 7, gx + 5, gy + 7, ROLLED[1]);
                    } else {
                        // Vertical 1x3 lug + 1px shadow
                        t.rect(gx + 3, gy + 2, gx + 3, gy + 4, ROLLED[5]);
                        t.rect(gx + 4, gy + 2, gx + 4, gy + 4, ROLLED[1]);
                        t.rect(gx + 6, gy + 3, gx + 6, gy + 5, ROLLED[4]);
                        t.rect(gx + 7, gy + 3, gx + 7, gy + 5, ROLLED[1]);
                    }
                }
            }
        }),
        checker_plate_item: () => paint(t => {
            const base = PAINTS.checker_plate();
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, base.get(x, y));
            t.bevel(0, 0, 15, 15, ROLLED[5], ROLLED[0]);
            t.bevel(1, 1, 14, 14, ROLLED[4], ROLLED[1]);
        }),
        checker_plate_trim: () => paint(t => {
            t.rect(0, 0, 15, 15, [0, 0, 0], 0);
            t.rect(0, 0, 15, 0, ROLLED[0]);
            t.rect(0, 1, 15, 1, ROLLED[5]);
            for (const x of [3, 12]) { t.px(x, 1, ROLLED[1]); }
        }),
        // Seamless horizontal rolled channel side (no vertical seam lines at x=0/15 so adjacent plates run continuously!)
        checker_plate_side: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 202, 12);
            t.rect(0, 0, 15, 0, IRON_DARK[4]);
            t.rect(0, 1, 15, 1, IRON_DARK[3]);
            t.rect(0, 2, 15, 2, IRON_DARK[0]);
            t.rect(0, 13, 15, 13, IRON_DARK[3]);
            t.rect(0, 14, 15, 14, IRON_DARK[1]);
            t.rect(0, 15, 15, 15, IRON_DARK[0]);
        }),
        checker_plate_side_item: () => paint(t => {
            const base = PAINTS.checker_plate_side();
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, base.get(x, y));
            t.rect(0, 3, 0, 12, IRON_DARK[3]);
            t.rect(15, 3, 15, 12, IRON_DARK[0]);
            mcRivet(t, 2, 7, IRON_DARK[5], IRON_DARK[0]);
            mcRivet(t, 12, 7, IRON_DARK[5], IRON_DARK[0]);
        }),

        // ---- Steel grating, handrail & ladder
        steel_grating: () => paint(t => {
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, [0, 0, 0], 0);
            for (let x = 0; x < 16; x += 4) {
                for (let y = 0; y < 16; y++) {
                    t.px(x, y, GALV[3]);
                    t.px(x + 1, y, GALV[1]);
                }
            }
            for (let y = 0; y < 16; y += 4) {
                for (let x = 0; x < 16; x++) {
                    t.px(x, y, x % 4 === 0 ? GALV[3] : GALV[2]);
                }
            }
            t.bevel(0, 0, 15, 15, GALV[3], GALV[0]);
        }),
        steel_grating_side: () => paint(t => {
            mcPlate(t, GALV, 2, 203, 10);
            t.rect(0, 0, 15, 0, GALV[3]); t.rect(0, 15, 15, 15, GALV[0]);
            for (let x = 2; x < 16; x += 4) t.rect(x, 4, x + 1, 11, GALV[0]);
        }),
        steel_handrail: () => paint(t => {
            mcCylinderV(t, IRON_DARK, 204);
        }),
        steel_ladder: () => paint(t => {
            for (const x0 of [2, 12]) {
                for (let y = 0; y < 16; y++) {
                    t.px(x0, y, IRON_DARK[4]); t.px(x0 + 1, y, IRON_DARK[1]);
                }
            }
            for (const y of [2, 6, 10, 14]) {
                t.rect(2, y, 13, y, GALV[3]);
                t.rect(2, y + 1, 13, y + 1, GALV[0]);
                for (let x = 4; x <= 11; x += 2) t.px(x, y, GALV[2]);
                t.px(2, y, IRON_DARK[4]); t.px(13, y, IRON_DARK[0]);
            }
        }),

        // ---- Process pipe (iuce:process_pipe): seamless barrel + cast elbow/tee fitting hub + bolted flange end
        process_pipe: () => paint(t => {
            // Seamless stepped steel pipe barrel (flanges are real 3D caps/arms so straight runs have no harsh seam!)
            mcCylinderV(t, ROLLED, 301);
            // Subtle circumferential weld ring at y=8
            for (let x = 0; x < 16; x++) {
                const idx = CYL_BANDS[x];
                t.px(x, 8, ROLLED[Math.max(0, idx - 1)]);
            }
        }),
        process_pipe_hub: () => paint(t => {
            // Cast iron elbow/tee fitting collar with corner bolts & machined rim
            mcPlate(t, IRON_DARK, 2, 302, 12);
            t.bevel(0, 0, 15, 15, IRON_DARK[4], IRON_DARK[0]);
            t.bevel(2, 2, 13, 13, IRON_DARK[3], IRON_DARK[1]);
            for (const [x, y] of [[3, 3], [11, 3], [3, 11], [11, 11]]) mcBolt2(t, x, y, IRON_DARK);
        }),
        process_pipe_end: () => paint(t => {
            mcPlate(t, ROLLED, 3, 303, 8);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 3.4) t.px(x, y, [18, 20, 24]);
                else if (d < 4.6) t.px(x, y, IRON_DARK[1]);
            }
            for (const [x, y] of [[3, 7], [12, 7], [7, 3], [7, 12], [4, 4], [11, 4], [4, 11], [11, 11]]) {
                t.px(x, y, IRON_DARK[0]);
            }
            t.bevel(0, 0, 15, 15, ROLLED[5], IRON_DARK[0]);
        }),

        // ---- Kiln refractory bricks & furnace shell (crisp vanilla MC stone-bricks / blast-furnace feel)
        refractory_bricks: () => paint(t => {
            const r = rng(401);
            t.rect(0, 0, 15, 15, MORTAR);
            for (let row = 0; row < 4; row++) {
                const y0 = row * 4, off = (row & 1) ? 4 : 0;
                for (let b = -1; b < 2; b++) {
                    const x0 = b * 8 + off;
                    const base = r() < 0.4 ? 1 : 2;
                    for (let y = y0; y < y0 + 3; y++) {
                        for (let x = x0; x < x0 + 7; x++) {
                            if (x < 0 || x > 15) continue;
                            let idx = base;
                            if (y === y0 || x === x0) idx = Math.min(3, base + 1);
                            else if (y === y0 + 2 || x === x0 + 6) idx = Math.max(0, base - 1);
                            else if (r() < 0.2) idx = Math.max(0, base - 1);
                            t.px(x, y, BRICK[idx]);
                        }
                    }
                }
            }
        }),
        furnace_shell: () => paint(t => {
            mcPlate(t, IRON_DARK, 1, 402, 16);
            t.bevel(0, 0, 15, 15, IRON_DARK[3], IRON_DARK[0]);
            t.rect(0, 7, 15, 7, IRON_DARK[0]);
            t.rect(0, 8, 15, 8, IRON_DARK[3]);
            for (const [x, y] of [[2, 2], [12, 2], [2, 12], [12, 12]]) mcBolt2(t, x, y, IRON_DARK);
        }),

        // ---- Tank / vessel shell (iuce:tank_shell): connected stainless steel cladding!
        tank_shell: () => paint(t => {
            // Seamless interior stainless plate (merges cleanly in 3x3 fermenters & columns!)
            mcPlate(t, VESSEL, 3, 501, 16);
            // Subtle stepped sheen band without hard border lines
            for (let y = 0; y < 16; y++) {
                if ((y & 3) !== 0) {
                    t.px(4, y, VESSEL[4]);
                    t.px(5, y, VESSEL[4]);
                    t.px(12, y, VESSEL[2]);
                }
            }
        }),
        tank_shell_item: () => paint(t => {
            const base = PAINTS.tank_shell();
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) t.px(x, y, base.get(x, y));
            t.bevel(0, 0, 15, 15, VESSEL[5], VESSEL[0]);
            t.bevel(1, 1, 14, 14, VESSEL[4], VESSEL[1]);
            for (const [x, y] of [[2, 2], [13, 2], [2, 13], [13, 13]]) t.px(x, y, VESSEL[1]);
        }),
        tank_shell_trim: () => paint(t => {
            t.rect(0, 0, 15, 15, [0, 0, 0], 0);
            t.rect(0, 0, 15, 0, VESSEL[0]);
            t.rect(0, 1, 15, 1, VESSEL[5]);
            for (const x of [3, 8, 13]) t.px(x, 1, VESSEL[1]);
        }),

        // ---- Graphite electrode
        graphite_electrode: () => paint(t => {
            const CARB = [[20, 22, 26], [32, 34, 40], [46, 48, 54], [62, 64, 72], [78, 82, 90], [96, 100, 108]];
            mcCylinderV(t, CARB, 601);
            for (let x = 0; x < 16; x++) {
                t.px(x, 0, CARB[0]);
                t.px(x, 1, CARB[Math.min(5, CYL_BANDS[x] + 1)]);
            }
        }),
        graphite_electrode_end: () => paint(t => {
            const CARB = [[20, 22, 26], [34, 36, 42], [52, 54, 60], [72, 74, 82]];
            mcPlate(t, CARB, 2, 602, 14);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                if (Math.hypot(x - 7.5, y - 7.5) < 2.6) t.px(x, y, CARB[0]);
            }
        }),

        // ---- Gas output hatch & screw barrel segments
        gas_output_hatch: () => paint(t => {
            A.casing(t, 61);
            A.roundWindow(t, 7.5, 7.5, 5, (x, y, d) => d < 2.6 ? [14, 16, 18] : d < 3.6 ? YELLOW[2] : STEEL[2]);
            A.gauge(t, 12, 12, 0.6);
        }),
        segment_top: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 80, 12);
            t.bevel(0, 0, 15, 15, IRON_DARK[4], IRON_DARK[0]);
            t.rect(3, 2, 12, 13, ROLLED[2]);
            t.bevel(3, 2, 12, 13, ROLLED[4], IRON_DARK[0]);
            t.rect(6, 5, 9, 10, IRON_DARK[1]);
            t.px(7, 7, COPPER[2]); t.px(8, 7, COPPER[3]);
        }),
        main_feed_segment: () => paint(t => segmentShell(t, 81, g => {
            g.rect(4, 2, 11, 6, ROLLED[3]); g.bevel(4, 2, 11, 6, ROLLED[5], IRON_DARK[0]);
            g.rect(6, 3, 9, 5, [22, 26, 30]);
        })),
        melting_segment: () => paint(t => segmentShell(t, 82, g => {
            for (const x0 of [2, 9]) {
                g.rect(x0, 4, x0 + 4, 11, CERAMIC[1]);
                g.rect(x0, 4, x0 + 4, 4, CERAMIC[2]);
                g.rect(x0, 11, x0 + 4, 11, CERAMIC[0]);
            }
            g.px(7, 6, COPPER[2]); g.px(8, 6, COPPER[3]);
        })),
        side_feed_segment: () => paint(t => segmentShell(t, 83, g => {
            A.roundWindow(g, 7.5, 7.5, 3.8, (x, y, d) => d < 2.0 ? [20, 24, 28] : ROLLED[3]);
        })),
        mixing_segment: () => paint(t => segmentShell(t, 84, g => {
            for (const x of [3, 6, 9, 12]) { g.rect(x, 4, x, 11, IRON_DARK[4]); g.rect(x + 1, 4, x + 1, 11, IRON_DARK[0]); }
        })),
        venting_segment: () => paint(t => segmentShell(t, 85, g => {
            g.rect(5, 2, 10, 5, IRON_DARK[3]); g.bevel(5, 2, 10, 5, ROLLED[5], IRON_DARK[0]);
            g.rect(6, 3, 9, 4, [20, 24, 28]);
            A.gauge(g, 12, 7, 0.5);
        })),
        die_segment: () => paint(t => segmentShell(t, 86, g => {
            g.rect(3, 4, 12, 11, IRON_DARK[1]); g.bevel(3, 4, 12, 11, ROLLED[4], IRON_DARK[0]);
            for (const x of [5, 7, 9, 11]) { g.px(x, 7, HOT[2]); g.px(x, 8, HOT[3]); }
        })),

        // ---- Plant casing & connected industrial roll drum (iuce:plant_casing, iuce:plant_drum)
        plant_casing: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 901, 16);
        }),
        plant_trim: () => paint(t => {
            t.rect(0, 0, 15, 15, [0, 0, 0], 0);
            t.rect(0, 0, 15, 0, IRON_DARK[0]);
            t.rect(0, 1, 15, 1, IRON_DARK[4]);
            t.rect(0, 2, 15, 2, IRON_DARK[1]);
            for (const x of [3, 12]) { t.px(x, 1, ROLLED[5]); t.px(x + 1, 1, IRON_DARK[0]); }
        }),
        // Seamless roll barrel (no dark lines at y=0 / y=15, so coaxial drums connect into one unbroken steel roll!)
        plant_drum: () => paint(t => {
            mcCylinderV(t, ROLLED, 902);
            // Subtle machined groove rings at y=5 and y=10 (continuous across circumference, no end seams)
            for (let x = 0; x < 16; x++) {
                const idx = CYL_BANDS[x];
                t.px(x, 5, ROLLED[Math.max(0, idx - 1)]);
                t.px(x, 10, ROLLED[Math.max(0, idx - 1)]);
            }
        }),
        plant_drum_collar: () => paint(t => {
            for (let x = 0; x < 16; x++) {
                const idx = CYL_BANDS[x];
                for (let y = 0; y < 16; y++) t.px(x, y, IRON_DARK[idx]);
                t.px(x, 0, IRON_DARK[0]);
                t.px(x, 1, COPPER[Math.min(3, idx >> 1)]);
                t.px(x, 14, COPPER[Math.min(3, idx >> 1)]);
                t.px(x, 15, IRON_DARK[0]);
            }
        }),
        plant_bearing: () => paint(t => {
            mcPlate(t, IRON_DARK, 2, 903, 8);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 2.6) t.px(x, y, ROLLED[4]);
                else if (d < 4.2) t.px(x, y, COPPER[2]);
                else if (d < 5.6) t.px(x, y, IRON_DARK[0]);
            }
            t.bevel(1, 1, 14, 14, IRON_DARK[4], IRON_DARK[0]);
        }),
    };

    const PATH_MAP = {
        gas_output_hatch: 'block/gas_output_hatch.png',
        main_feed_segment: 'block/main_feed_segment.png',
        melting_segment: 'block/melting_segment.png',
        side_feed_segment: 'block/side_feed_segment.png',
        mixing_segment: 'block/mixing_segment.png',
        venting_segment: 'block/venting_segment.png',
        die_segment: 'block/die_segment.png',
        plant_casing: 'block/machine/plant/casing.png',
        plant_trim: 'block/machine/plant/trim.png',
        plant_drum: 'block/machine/plant/drum.png',
        plant_drum_collar: 'block/machine/plant/drum_collar.png',
        plant_bearing: 'block/machine/plant/bearing.png',
    };

    function paintAll() {
        const fs = A.fs, dir = A.TEX + 'block/structure/';
        if (!fs.existsSync(dir)) fs.mkdirSync(dir);
        const out = [];
        for (const name in PAINTS) {
            const rel = PATH_MAP[name] || ('block/structure/' + name + '.png');
            out.push(save(PAINTS[name](), rel));
        }
        return out.join(', ');
    }

    window.IUCEStructure = { PAINTS, paintAll };
    return 'structure loaded';
})();
