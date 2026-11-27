// Machine fronts for the PLA and PVC lines (stage 2 M2-M4), painted inside Blockbench.
// Load core.js first, then this file; run IUCEMachines.all() to write every front.
// Each front is a 16x16 idle texture and an 8-frame animated active strip.
(function () {
    const A = window.IUCEArt;
    const { mix, light, dark, hex, STEEL, FRAME, GLASS, HOT, COPPER } = A;
    const WHITE = [236, 238, 232];

    // ---- small parts shared by several fronts

    function stirrer(g, f, on, x, y0, y1, shaft) {
        shaft = shaft || [160, 164, 170];
        g.rect(x, y0, x, y1, shaft);
        const s = on ? [0, 1, 2, 1, 0, -1, -2, -1][f] : 2;
        const w = Math.abs(s) + 1;
        g.rect(x - w, y1, x - 1, y1, shaft);
        g.rect(x + 1, y1, x + w, y1, dark(shaft, 0.3));
    }
    function ribbon(g, f, on, x0, x1, y0, y1) {
        // a helical ribbon agitator seen side-on: diagonal blades that walk upward when turning
        for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) {
            if (((x + y + (on ? f : 0)) % 4) === 0) g.px(x, y, (x + y + f) % 8 < 4 ? [196, 200, 206] : [120, 126, 134]);
        }
        g.rect((x0 + x1) >> 1, y0 - 1, (x0 + x1) >> 1, y1, [170, 174, 180]);
    }
    function flame(g, f, x0, x1, ytop, ybase, cold) {
        const pal = cold || HOT;
        for (let x = x0; x <= x1; x++) {
            const h = (ybase - ytop) * (0.55 + 0.45 * Math.sin((x * 1.7 + f * 1.3)));
            for (let y = ybase; y >= ybase - h; y--) {
                const t = (ybase - y) / Math.max(1, h);
                g.px(x, y, t < 0.3 ? pal[1] : t < 0.7 ? pal[2] : pal[3]);
            }
        }
    }
    function bubbles(g, f, on, x0, x1, ytop, ybot, n, c, seed) {
        if (!on) return;
        const r = A.rng(seed);
        const h = ybot - ytop + 1;
        for (let i = 0; i < n; i++) {
            const bx = x0 + Math.floor(r() * (x1 - x0 + 1));
            const by = ybot - (Math.floor(r() * h) + f) % h;
            g.px(bx, by, c);
        }
    }

    // ---- shells

    function tankShell(g, seed) { A.vessel(g, 61 + seed, [1, 14]); }
    function columnShell(g) { A.columnBase(g); }
    function boxShell(g, seed) { A.casing(g, seed); }

    // ---- fronts. draw(g, f, on): f is the frame 0..7, on whether the machine runs.

    const FRONTS = {};

    // PLA line ------------------------------------------------------------------

    FRONTS.catalyst_prep_tank = { v: 'LV', draw(g, f, on) {
        tankShell(g, 11);
        // a dosing hopper feeding tin octoate into a small mixing vessel
        g.rect(5, 3, 10, 3, STEEL[1]); g.rect(6, 4, 9, 4, STEEL[2]); g.px(7, 5, STEEL[1]); g.px(8, 5, STEEL[1]);
        const liquid = [222, 204, 120];
        A.sightGlass(g, 4, 6, 11, 13, liquid, 0.6, f, on, 11);
        stirrer(g, f, on, 7, 7, 11);
        if (on && f % 2 === 0) g.px(8, 6 + (f >> 1) % 2, light(liquid, 0.5));
        A.lamp(g, 12, 3, on);
        A.tier(g, 'LV');
    } };

    FRONTS.crystallizer = { v: 'LV', draw(g, f, on) {
        tankShell(g, 12);
        const brine = [150, 200, 214];
        A.sightGlass(g, 3, 3, 12, 12, brine, 0.75, f, false, 12);
        // cooling jacket coil on both sides of the glass
        for (let y = 3; y <= 12; y++) {
            const c = (y + (on ? f : 0)) % 3 === 0 ? [120, 190, 240] : [60, 110, 160];
            g.px(2, y, c); g.px(13, y, c);
        }
        // crystals grow on the floor of the vessel
        const crystals = [[5, 11], [7, 11], [9, 11], [10, 10], [6, 10], [8, 9]];
        const grown = on ? 3 + (f >> 1) : 4;
        crystals.slice(0, grown).forEach(([x, y], i) => {
            g.px(x, y, i % 2 ? [246, 248, 250] : [210, 230, 236]);
            if (on && (f + i) % 4 === 0) g.px(x, y - 1, [255, 255, 255]);
        });
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.depolymerization_reactor = { v: 'MV', draw(g, f, on) {
        tankShell(g, 13);
        const melt = [214, 150, 60];
        // vapour line on top carries lactide away
        A.pipeH(g, 3, 12, 3); g.rect(12, 2, 13, 4, STEEL[3]);
        A.sightGlass(g, 3, 5, 11, 12, melt, 0.55, f, on, 13);
        if (on) for (let i = 0; i < 2; i++) g.px(5 + ((f + i * 3) % 5), 7 - ((f >> 1) + i) % 2, [246, 230, 196]);
        A.heaterRow(g, 4, 10, 11, on, f);
        A.gauge(g, 13, 8, on ? 0.7 : 0.1);
        A.lamp(g, 12, 11, on);
        A.tier(g, 'MV', 13, 3, 11);
    } };

    FRONTS.evaporator = { v: 'LV', draw(g, f, on) {
        columnShell(g);
        // vapour chamber on top, boiling liquor in the calandria below
        A.recess(g, 3, 2, 10, 5);
        if (on) for (let x = 4; x <= 9; x++) if ((x + f) % 3 === 0) g.px(x, 4 - (f + x) % 2, [214, 226, 232]);
        A.sightGlass(g, 3, 7, 10, 12, [220, 196, 120], 0.7, f, on, 14);
        // calandria tubes
        for (let x = 4; x <= 9; x += 2) g.px(x, 11, on ? HOT[2] : [90, 60, 40]);
        A.gauge(g, 12, 4, on ? 0.6 : 0.15);
        A.lamp(g, 11, 13, on);
    } };

    FRONTS.oligomerization_reactor = { v: 'LV', draw(g, f, on) {
        tankShell(g, 14);
        const syrup = [226, 178, 82];
        A.sightGlass(g, 4, 3, 11, 12, syrup, 0.65, f, on ? 2 : false, 15);
        stirrer(g, f, on, 7, 4, 10);
        A.heaterRow(g, 5, 10, 11, on, f);
        // condenser stub on the side
        g.rect(12, 4, 13, 9, STEEL[3]); g.rect(13, 4, 13, 9, STEEL[1]);
        if (on) g.px(12, 5 + (f % 4), [150, 200, 230]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.polymerization_reactor = { v: 'MV', draw(g, f, on) {
        tankShell(g, 15);
        const melt = [236, 226, 196];
        A.sightGlass(g, 3, 3, 12, 12, melt, 0.7, f, false, 16);
        ribbon(g, f, on, 4, 11, 6, 11);
        if (on) g.px(4 + (f % 8), 5, light(melt, 0.6));
        A.gauge(g, 13, 7, on ? 0.75 : 0.1);
        A.lamp(g, 12, 13, on);
        A.tier(g, 'MV', 14, 2, 10);
    } };

    FRONTS.strand_pelletizer = { v: 'LV', draw(g, f, on) {
        boxShell(g, 101);
        A.recess(g, 2, 4, 13, 11);
        // strands come in from the water bath on the left
        for (const y of [6, 8]) for (let x = 3; x <= 7; x++) g.px(x, y, (x + (on ? f : 0)) % 4 === 0 ? [250, 248, 236] : [222, 216, 196]);
        g.rect(3, 9, 7, 10, [40, 90, 120]);
        if (on) g.px(3 + (f % 5), 9, [120, 190, 220]);
        // the cutter rotor
        A.roundWindow(g, 10, 7, 2.6, A.blades(4, on ? f * 22.5 : 0, [210, 214, 220], [70, 76, 84]));
        // pellets dropping into the chute
        if (on) for (let i = 0; i < 3; i++) g.px(9 + i, 10 - ((f + i * 3) % 3) + 1, [236, 232, 214]);
        A.lamp(g, 12, 12, on);
        A.tier(g, 'LV');
    } };

    // PVC and chlor-alkali line ---------------------------------------------------

    FRONTS.acetylene_generator = { v: 'LV', draw(g, f, on) {
        tankShell(g, 21);
        const water = [120, 160, 170];
        A.sightGlass(g, 4, 3, 11, 12, water, 0.6, f, false, 21);
        // carbide lumps on the grate fizzing into acetylene
        for (const [x, y] of [[5, 11], [7, 11], [9, 11], [6, 10], [10, 11]]) g.px(x, y, [92, 90, 100]);
        bubbles(g, f, on, 5, 10, 7, 10, 6, [236, 240, 240], 22);
        if (on) g.rect(5, 5 + (f % 2), 10, 5 + (f % 2), [170, 186, 190]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.additive_reactor = { v: 'LV', draw(g, f, on) {
        tankShell(g, 22);
        const milk = [228, 224, 206];
        A.sightGlass(g, 4, 4, 11, 12, milk, 0.6, f, on ? 2 : false, 23);
        stirrer(g, f, on, 7, 5, 10, [120, 126, 134]);
        // three dosing valves on top
        for (const x of [5, 8, 11]) { g.px(x, 2, [200, 60, 50]); g.px(x, 3, STEEL[1]); }
        if (on) g.px([5, 8, 11][f % 3], 4, [250, 210, 120]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.air_compressor = { v: 'LV', draw(g, f, on) {
        boxShell(g, 102);
        // a big intake fan behind a guard grille
        A.roundWindow(g, 7, 7, 5, (x, y, d, a) => {
            if ((x + y) % 2 === 0 && d > 1.6) return FRAME[0];
            return A.blades(3, on ? f * 15 : 10, [170, 176, 184], [40, 44, 50], STEEL[4])(x, y, d, a);
        });
        A.gauge(g, 12, 3, on ? 0.65 + 0.05 * (f % 2) : 0.05);
        A.lamp(g, 12, 12, on);
    } };

    FRONTS.brine_dissolver = { v: 'LV', draw(g, f, on) {
        tankShell(g, 23);
        const brine = [170, 200, 210];
        // spray header above the salt bed
        A.pipeH(g, 3, 12, 3);
        A.sightGlass(g, 3, 5, 12, 12, brine, 0.5, f, false, 24);
        // undissolved rock salt heaped on the floor
        for (let x = 4; x <= 11; x++) {
            const h = 1 + (x === 6 || x === 7 || x === 8 ? 1 : 0);
            for (let k = 0; k < h; k++) g.px(x, 11 - k, (x + k) % 2 ? [236, 228, 222] : [214, 196, 192]);
        }
        if (on) for (let x = 4; x <= 11; x += 2) g.px(x, 6 + ((f + x) % 3), [210, 230, 236]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.brine_purifier = { v: 'LV', draw(g, f, on) {
        tankShell(g, 24);
        const brine = [184, 214, 220];
        A.sightGlass(g, 4, 3, 11, 12, brine, 0.8, f, false, 25);
        // sludge layer at the bottom, flocs settling through the brine
        g.rect(5, 10, 10, 11, [120, 104, 86]); g.rect(5, 10, 10, 10, [146, 128, 104]);
        const flocs = [[5, 5], [8, 6], [10, 4], [6, 7], [9, 8]];
        flocs.forEach(([x, y], i) => g.px(x, on ? 5 + (y + f + i) % 5 : y, [160, 140, 112]));
        // a cone outlet under the vessel
        g.rect(6, 13, 9, 13, STEEL[1]); g.rect(7, 14, 8, 14, FRAME[1]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.carbide_furnace = { v: 'MV', draw(g, f, on) {
        boxShell(g, 103);
        // refractory pit with three electrodes hanging in the melt
        A.recess(g, 2, 3, 13, 12, [40, 30, 28]);
        for (let x = 3; x <= 12; x++) for (let y = 9; y <= 11; y++) {
            const glow = on ? ((x * 3 + y + f) % 5 < 2 ? HOT[1] : HOT[2]) : [70, 52, 44];
            g.px(x, y, y === 9 ? light(glow, 0.15) : glow);
        }
        for (const x of [4, 7, 10]) {
            g.rect(x, 1, x + 1, 8, [46, 46, 50]); g.rect(x, 1, x, 8, [80, 80, 86]);
            if (on && (x + f) % 3) g.px(x + (f % 2), 9, HOT[0]);
        }
        // copper busbar above
        g.rect(3, 1, 12, 1, COPPER[2]);
        A.lamp(g, 12, 13, on);
        A.tier(g, 'MV', 14, 2, 10);
    } };

    FRONTS.centrifuge = { v: 'MV', draw(g, f, on) {
        boxShell(g, 104);
        // a spinning basket: solids thrown to the wall, a dark core of liquor
        A.roundWindow(g, 7.5, 7, 5, (x, y, d, a) => {
            if (d < 1.2) return STEEL[4];
            if (d > 3.3) {
                const s = ((a - (on ? f * 40 : 0)) % 60 + 60) % 60;
                return s < 20 ? [226, 222, 210] : [186, 180, 164];
            }
            const s = ((a - (on ? f * 40 : 0)) % 90 + 90) % 90;
            return s < 12 ? [90, 110, 120] : [40, 52, 60];
        });
        A.lamp(g, 12, 12, on);
        A.tier(g, 'MV');
    } };

    FRONTS.cnc_machining_center = { v: 'MV', draw(g, f, on) {
        boxShell(g, 105);
        // enclosure window with a spindle above the mould block
        A.recess(g, 2, 2, 11, 12, [26, 34, 40]);
        A.glare(g, 3, 3);
        const travel = on ? [0, 1, 2, 3, 3, 2, 1, 0][f] : 1;
        const sx = 4 + travel;
        g.rect(sx, 3, sx + 2, 5, STEEL[3]); g.rect(sx + 1, 6, sx + 1, 8, [220, 220, 210]);
        g.rect(3, 9, 10, 11, [150, 156, 166]); g.rect(3, 9, 10, 9, [196, 200, 206]);
        if (on) { g.px(sx + 1, 9, [255, 240, 180]); g.px(sx + (f % 2 ? 0 : 2), 8, [255, 200, 120]); }
        // control screen
        A.recess(g, 12, 3, 13, 7, [20, 60, 40]);
        if (on) g.px(12 + (f % 2), 4 + (f >> 1) % 3, [120, 240, 140]);
        A.lamp(g, 12, 12, on);
        A.tier(g, 'MV', 14, 2, 10);
    } };

    FRONTS.coking_oven = { v: 'LV', draw(g, f, on) {
        // a firebrick oven face with a cast door
        const brick = [[112, 54, 40], [136, 68, 48], [160, 86, 60]];
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const row = y >> 1; const off = row % 2 ? 2 : 0;
            const mortar = y % 2 === 1 || (x + off) % 4 === 3;
            g.px(x, y, mortar ? [70, 60, 56] : brick[(x * 7 + y * 3) % 3]);
        }
        A.recess(g, 3, 3, 12, 12, [48, 50, 54]);
        g.bevel(4, 4, 11, 11, STEEL[3], FRAME[0]);
        g.rect(5, 5, 10, 10, STEEL[1]);
        // the peep slot shows the charge glowing
        for (let x = 6; x <= 9; x++) g.px(x, 7, on ? HOT[(x + f) % 3 + 1] : [60, 44, 36]);
        g.rect(6, 9, 9, 9, STEEL[4]); g.px(9, 9, FRAME[0]);
        if (on) g.px(4 + (f % 8), 2, [120, 116, 110]);
        A.lamp(g, 13, 13, on);
    } };

    FRONTS.dehumidifying_dryer = { v: 'LV', draw(g, f, on) {
        boxShell(g, 106);
        // pellet hopper with a sight glass
        A.recess(g, 2, 2, 6, 12);
        for (let y = 5; y <= 11; y++) for (let x = 3; x <= 5; x++) g.px(x, y, (x + y + (on ? f >> 1 : 0)) % 2 ? [232, 226, 200] : [206, 198, 170]);
        A.glare(g, 3, 3);
        // desiccant wheel turning between process and regeneration air
        A.roundWindow(g, 10.5, 6.5, 3.5, (x, y, d, a) => {
            if (d < 0.9) return STEEL[4];
            const s = ((a - (on ? f * 22.5 : 0)) % 45 + 45) % 45;
            const regen = y < 6.5;
            const base = regen && on ? [200, 120, 70] : [150, 170, 120];
            return s < 8 ? dark(base, 0.45) : base;
        });
        A.lamp(g, 12, 12, on);
    } };

    FRONTS.filter_press = { v: 'LV', draw(g, f, on) {
        boxShell(g, 107);
        // the plate pack between the fixed head and the hydraulic ram
        g.rect(2, 4, 3, 11, STEEL[2]); g.bevel(2, 4, 3, 11, STEEL[4], FRAME[0]);
        for (let x = 4; x <= 11; x++) g.rect(x, 4, x, 11, x % 2 ? [200, 196, 180] : [96, 100, 108]);
        g.rect(12, 6, 13, 9, STEEL[4]); g.rect(13, 6, 13, 9, FRAME[1]);
        g.rect(2, 3, 13, 3, FRAME[0]);
        // filtrate dripping into the tray
        g.rect(3, 12, 12, 12, FRAME[1]);
        if (on) for (const x of [5, 7, 9, 11]) g.px(x, 12 + ((f + x) % 2), [140, 196, 220]);
        A.lamp(g, 12, 1, on);
    } };

    FRONTS.flash_tank = { v: 'LV', draw(g, f, on) {
        tankShell(g, 25);
        const liquor = [196, 210, 220];
        const s = A.sightGlass(g, 4, 3, 11, 12, liquor, 0.35, f, on ? 3 : false, 26);
        // flash vapour churning above the liquid
        if (on) for (let x = 5; x <= 10; x++) if ((x + f) % 3) g.px(x, s - 1 - ((x + f) % 3), [214, 222, 228]);
        // tangential inlet
        A.pipeH(g, 1, 4, 7);
        A.gauge(g, 13, 5, on ? 0.45 : 0.1);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.gas_compressor = { v: 'MV', draw(g, f, on) {
        boxShell(g, 108);
        // two cylinders driven by a crankshaft, pistons out of phase
        for (const [x, ph] of [[3, 0], [8, 4]]) {
            A.recess(g, x, 3, x + 3, 9);
            const p = on ? [0, 1, 2, 3, 3, 2, 1, 0][(f + ph) % 8] : 1;
            g.rect(x + 1, 4 + p, x + 2, 5 + p, STEEL[4]);
            g.rect(x + 1, 6 + p, x + 2, 9, [90, 96, 104]);
        }
        g.rect(3, 10, 11, 11, FRAME[1]); g.rect(3, 10, 11, 10, STEEL[3]);
        A.gauge(g, 13, 5, on ? 0.85 : 0.1);
        A.lamp(g, 12, 12, on);
        A.tier(g, 'MV', 14, 2, 10);
    } };

    FRONTS.hcl_furnace = { v: 'LV', draw(g, f, on) {
        columnShell(g);
        // a graphite synthesis chamber: pale chlorine-hydrogen flame through the port
        A.roundWindow(g, 7, 7, 4, (x, y, d) => {
            if (!on) return d < 2 ? [50, 48, 46] : [34, 32, 32];
            const flick = Math.sin(x * 2.1 + y * 1.3 + f * 1.7);
            if (d < 1.3) return [250, 252, 230];
            if (d < 2.4 + flick * 0.4) return [210, 240, 170];
            return [88, 120, 70];
        });
        // feed lines for chlorine (yellow) and hydrogen (red) tags
        g.rect(1, 12, 4, 12, [210, 200, 80]); g.rect(1, 13, 4, 13, [180, 60, 50]);
        A.lamp(g, 11, 13, on);
    } };

    FRONTS.high_speed_mixer = { v: 'MV', draw(g, f, on) {
        boxShell(g, 109);
        // resin powder thrown into a vortex by the bottom blade
        A.roundWindow(g, 7.5, 7, 5, (x, y, d, a) => {
            if (d < 1.0) return STEEL[4];
            const swirl = ((a + d * 40 - (on ? f * 45 : 0)) % 90 + 90) % 90;
            return swirl < 30 ? [240, 238, 230] : swirl < 55 ? [214, 212, 204] : [160, 160, 156];
        });
        A.lamp(g, 12, 12, on);
        A.tier(g, 'MV');
    } };

    FRONTS.injection_molder = { v: 'MV', draw(g, f, on) {
        boxShell(g, 110);
        // hopper on top
        g.rect(9, 2, 12, 2, STEEL[3]); g.rect(10, 3, 11, 4, STEEL[2]);
        // the barrel with heater bands
        g.rect(5, 5, 13, 7, STEEL[2]); g.rect(5, 5, 13, 5, STEEL[4]); g.rect(5, 7, 13, 7, STEEL[1]);
        for (const x of [6, 8, 10, 12]) {
            const hot = on && (x + f) % 4 !== 1;
            g.rect(x, 5, x, 7, hot ? HOT[2] : COPPER[1]);
            g.px(x, 5, hot ? HOT[1] : COPPER[2]);
        }
        // platens and the mould closing on the nozzle
        const gap = on ? [1, 0, 0, 0, 0, 1, 1, 1][f] : 1;
        g.rect(4, 3, 4, 10, [176, 70, 52]);                       // fixed mould half
        g.rect(3 - gap, 3, 3 - gap, 10, [150, 58, 44]);            // moving mould half
        g.rect(2 - gap, 3, 2 - gap, 10, STEEL[4]);                 // moving platen
        if (on && gap === 0) g.px(3, 6, HOT[1]);
        // tie bars
        g.rect(2, 11, 13, 11, FRAME[1]);
        A.lamp(g, 12, 12, on);
        A.tier(g, 'MV', 14, 2, 10);
    } };

    FRONTS.lime_kiln = { v: 'LV', draw(g, f, on) {
        columnShell(g);
        // refractory-lined shaft full of stone, burning at the bottom
        A.recess(g, 3, 2, 11, 13, [60, 40, 34]);
        const r = A.rng(31);
        for (let y = 3; y <= 12; y++) for (let x = 4; x <= 10; x++) {
            const stone = r() < 0.7;
            if (!stone) continue;
            const heat = (y - 3) / 9;
            const base = mix([214, 210, 196], on ? HOT[2] : [150, 140, 128], heat * (on ? 0.9 : 0.4));
            g.px(x, y, (x + y) % 3 === 0 ? dark(base, 0.15) : base);
        }
        if (on) for (let x = 4; x <= 10; x++) if ((x + f) % 2) g.px(x, 12 - ((x + f) % 3 === 0 ? 1 : 0), HOT[1]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.membrane_cell = { v: 'MV', draw(g, f, on) {
        boxShell(g, 111);
        // one cell: anolyte (chlorine side) on the left, caustic on the right, membrane between
        A.recess(g, 2, 3, 13, 11);
        g.rect(3, 4, 7, 10, [150, 170, 110]); g.rect(9, 4, 12, 10, [170, 196, 214]);
        g.rect(8, 4, 8, 10, [230, 220, 160]);
        bubbles(g, f, on, 3, 7, 4, 10, 4, [220, 230, 160], 41);
        bubbles(g, f, on, 9, 12, 4, 10, 4, [246, 250, 252], 42);
        // copper busbars at the electrodes
        g.rect(3, 2, 4, 2, COPPER[2]); g.rect(11, 2, 12, 2, COPPER[2]);
        g.rect(3, 3, 3, 3, COPPER[1]); g.rect(12, 3, 12, 3, COPPER[1]);
        A.lamp(g, 12, 12, on);
        A.tier(g, 'MV', 13, 2, 10);
    } };

    FRONTS.neutralizer = { v: 'LV', draw(g, f, on) {
        tankShell(g, 26);
        // liquor drifting from acidic red to neutral green while it runs
        const colours = [[214, 120, 100], [200, 150, 100], [180, 176, 100], [140, 190, 110]];
        const liquor = on ? colours[(f >> 1) % 4] : colours[0];
        A.sightGlass(g, 3, 3, 9, 12, liquor, 0.65, f, false, 27);
        stirrer(g, f, on, 6, 4, 10);
        // pH meter readout
        A.recess(g, 11, 4, 13, 8, [20, 40, 30]);
        g.px(12, 5, on ? [120, 240, 140] : [40, 70, 50]); g.px(12, 7, on ? [120, 240, 140] : [40, 70, 50]);
        A.lamp(g, 12, 13, on);
    } };

    FRONTS.plastic_granulator = { v: 'LV', draw(g, f, on) {
        boxShell(g, 112);
        // a feed throat with a rotor of knives under it
        g.rect(4, 2, 11, 2, STEEL[1]); g.rect(5, 3, 10, 3, FRAME[1]);
        A.roundWindow(g, 7.5, 7.5, 4.2, (x, y, d, a) => {
            if (d < 1.0) return STEEL[4];
            const s = ((a - (on ? f * 30 : 0)) % 90 + 90) % 90;
            if (s < 14) return [216, 220, 226];
            if (on && (x * 5 + y * 3 + f) % 7 === 0) return [96, 170, 214];
            return [44, 48, 54];
        });
        // regrind falling out
        g.rect(5, 12, 10, 13, FRAME[0]);
        if (on) for (const x of [6, 8, 9]) g.px(x, 12 + (f + x) % 2, [96, 170, 214]);
        A.lamp(g, 12, 12, on);
    } };

    FRONTS.scrubber = { v: 'LV', draw(g, f, on) {
        columnShell(g);
        // packed bed of rings with wash liquid trickling down through rising gas
        A.recess(g, 3, 2, 10, 13);
        for (let y = 5; y <= 12; y++) for (let x = 4; x <= 9; x++) {
            if ((x + y * 2) % 3 === 0) g.px(x, y, [196, 190, 176]);
            else if ((x + y * 2) % 3 === 1) g.px(x, y, [120, 116, 108]);
        }
        // spray nozzle at the top
        g.rect(4, 3, 9, 3, STEEL[3]);
        if (on) for (const x of [4, 6, 8]) g.px(x + (f % 2), 4 + (f + x) % 9, [150, 200, 230]);
        A.gauge(g, 12, 4, on ? 0.5 : 0.1);
        A.lamp(g, 11, 13, on);
    } };

    FRONTS.tar_distiller = { v: 'LV', draw(g, f, on) {
        columnShell(g);
        // amber oil on the upper tray, black tar boiling in the pot
        A.sightGlass(g, 3, 2, 9, 5, [214, 150, 50], 0.5, f, false, 51);
        if (on) g.px(4 + (f % 5), 3, [240, 200, 120]);
        A.sightGlass(g, 3, 7, 9, 12, [44, 36, 30], 0.75, f, on ? 2 : false, 52);
        A.heaterRow(g, 4, 8, 11, on, f);
        A.gauge(g, 12, 9, on ? 0.6 : 0.1);
        A.lamp(g, 11, 13, on);
    } };

    FRONTS.vcm_converter = { v: 'MV', draw(g, f, on) {
        tankShell(g, 27);
        // a tube bundle packed with catalyst, the hot spot moving along the tubes
        A.recess(g, 3, 3, 12, 12);
        for (let x = 4; x <= 11; x++) {
            const tube = x % 2 === 0;
            for (let y = 4; y <= 11; y++) {
                if (tube) {
                    const hot = on && Math.abs(y - (4 + f)) <= 1;
                    g.px(x, y, hot ? HOT[2] : (y % 2 ? [56, 58, 60] : [36, 38, 40]));
                } else g.px(x, y, [110, 140, 150]);
            }
        }
        A.lamp(g, 12, 13, on);
        A.tier(g, 'MV', 14, 2, 10);
    } };

    FRONTS.water_treatment = { v: 'LV', draw(g, f, on) {
        boxShell(g, 113);
        // a sand filter: water over strata of sand and gravel
        A.recess(g, 2, 2, 8, 12);
        g.rect(3, 3, 7, 5, [90, 150, 190]);
        g.rect(3, 6, 7, 8, [214, 196, 150]);
        g.rect(3, 9, 7, 11, [140, 132, 120]);
        for (let x = 3; x <= 7; x++) if (x % 2) g.px(x, 10, [176, 168, 156]);
        if (on) { g.px(3 + (f % 5), 3, [180, 220, 240]); g.px(4 + ((f + 2) % 3), 6 + (f % 3), [130, 180, 210]); }
        // clean water out to a second window with the UV lamp
        A.recess(g, 10, 2, 13, 9, [60, 110, 140]);
        g.rect(11, 3, 12, 8, on ? [190, 160, 250] : [80, 90, 110]);
        if (on && f % 2) g.px(11, 3 + (f % 6), [240, 230, 255]);
        A.lamp(g, 12, 12, on);
    } };

    // the process design table is a static block with only one front
    const DESIGN_TABLE = { v: 'LV', draw(g) {
        boxShell(g, 120);
        // screen showing a flowsheet
        A.recess(g, 2, 2, 13, 8, [16, 40, 30]);
        const ink = [110, 230, 140];
        g.rect(4, 4, 5, 5, ink); g.rect(6, 5, 8, 5, [70, 170, 100]); g.rect(9, 4, 10, 6, ink);
        g.rect(11, 5, 12, 5, [70, 170, 100]); g.px(10, 7, [70, 170, 100]); g.px(4, 6, [240, 200, 90]);
        A.glare(g, 3, 3);
        // card slot with a card half inserted and a keypad
        g.rect(3, 10, 8, 10, FRAME[0]); g.rect(4, 9, 7, 9, [96, 176, 214]); g.px(4, 9, [200, 230, 240]);
        for (let y = 10; y <= 12; y += 2) for (let x = 10; x <= 13; x += 2) g.px(x, y, (x + y) % 4 ? STEEL[4] : [214, 124, 40]);
    } };

    const MODEL_DIR = {
        catalyst_prep_tank: 'm2', crystallizer: 'm2', depolymerization_reactor: 'm2', evaporator: 'm2',
        oligomerization_reactor: 'm2', polymerization_reactor: 'm2', strand_pelletizer: 'm2',
        air_compressor: 'm3', cnc_machining_center: 'm3', dehumidifying_dryer: 'm3', injection_molder: 'm3',
        plastic_granulator: 'm3',
    };

    function write(id) {
        const def = FRONTS[id];
        const dir = 'tools/models/' + (MODEL_DIR[id] || 'm4') + '/' + id + '.bbmodel';
        const idle = A.paint((g, f) => def.draw(g, 0, false));
        const active = A.paint((g, f) => def.draw(g, f, true), 8);
        A.save(idle, 'block/machine/process/' + id + '_front.png');
        A.save(active, 'block/machine/process/' + id + '_front_active.png', 2);
        A.syncModel(dir, id + '_front', idle);
        A.syncModel(dir, id + '_front_active', active);
        return id;
    }

    function all() {
        const done = Object.keys(FRONTS).map(write);
        const table = A.paint(g => DESIGN_TABLE.draw(g));
        A.save(table, 'block/machine/process/process_design_table_front.png');
        A.syncModel('tools/models/m2/process_design_table.bbmodel', 'process_design_table_front', table);
        return done.length + 1;
    }

    window.IUCEMachines = { FRONTS, DESIGN_TABLE, write, all };
    return 'machines loaded';
})();
