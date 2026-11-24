// Polyethylene, natural rubber and tier-II logistics textures, painted inside Blockbench after core.js.
// IUCEPolymer.all() writes the eleven machine fronts (idle + 8-frame active), the eight logistics/farm
// blocks and the item icons, and syncs the matching .bbmodel sources under tools/models/m5..m9.
// Fronts follow machines.js (steel casing, recessed windows, MV stripe); icons follow items.js
// (transparent sprite, light from the upper left, dark rim in the object's own hue).
(function () {
    const A = window.IUCEArt;
    const { mix, light, dark, STEEL, FRAME, GLASS, HOT, COPPER } = A;

    // ---- primitives (local copies so this file only depends on core.js)

    function tone(base, t) {
        if (t < 0.22) return dark(base, 0.42);
        if (t < 0.45) return dark(base, 0.2);
        if (t < 0.75) return base;
        if (t < 0.92) return light(base, 0.25);
        return light(base, 0.5);
    }
    function outline(g, k) {
        const src = [];
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) src.push(g.get(x, y));
        const at = (x, y) => (x < 0 || x > 15 || y < 0 || y > 15) ? null : src[y * 16 + x];
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            if (at(x, y)[3]) continue;
            const n = [at(x - 1, y), at(x + 1, y), at(x, y - 1), at(x, y + 1)].filter(p => p && p[3]);
            if (!n.length) continue;
            const avg = [0, 1, 2].map(i => n.reduce((s, p) => s + p[i], 0) / n.length);
            const c = dark(avg, k || 0.62);
            g.px(x, y, [Math.max(c[0], 14), Math.max(c[1], 14), Math.max(c[2], 18)]);
        }
    }
    function inPoly(px, py, pts) {
        let inside = false;
        for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
            const [xi, yi] = pts[i], [xj, yj] = pts[j];
            if ((yi > py) !== (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) inside = !inside;
        }
        return inside;
    }
    function poly(g, pts, c) {
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++)
            if (inPoly(x + 0.5, y + 0.5, pts)) g.px(x, y, typeof c === 'function' ? c(x, y) : c);
    }
    function line(g, x0, y0, x1, y1, c) {
        const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (let i = 0; i <= n; i++) g.px(Math.round(x0 + (x1 - x0) * i / n), Math.round(y0 + (y1 - y0) * i / n), c);
    }
    // Calls fn for every pixel inside an ellipse; fn returns a colour or null to skip.
    function disc(g, cx, cy, rx, ry, fn) {
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry, d = Math.hypot(dx, dy);
            if (d > 1) continue;
            const c = fn(x, y, dx, dy, d, Math.atan2(dy, dx) * 180 / Math.PI);
            if (c) g.px(x, y, c);
        }
    }
    function pile(g, seed, base, specks, opts) {
        opts = opts || {};
        const r = A.rng(seed);
        const h = opts.height || 8, hw = opts.half || 6.8, floor = 13;
        for (let x = 0; x < 16; x++) {
            const dx = (x + 0.5 - 8) / hw;
            if (Math.abs(dx) > 1) continue;
            const bump = opts.lumpy ? Math.sin(x * 1.9 + seed) * 0.8 : 0;
            const top = Math.round(floor - h * Math.pow(1 - dx * dx, 0.75) + bump);
            for (let y = top; y <= floor; y++) {
                const up = (floor - y) / h;
                let t = 0.62 - 0.38 * dx + 0.12 * up + (r() * 2 - 1) * (opts.grain || 0.1);
                if (y === top) t += 0.2;
                let c = tone(base, t);
                for (const [sc, p] of specks || []) if (r() < p) c = sc;
                g.px(x, y, c);
            }
        }
    }
    // An irregular rounded lump lit from the upper left.
    function lump(g, cx, cy, rx, ry, base, seed, o) {
        o = o || {};
        const r = A.rng(seed);
        const n = 7, rad = [];
        for (let i = 0; i < n; i++) rad.push(0.8 + 0.22 * r());
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
            const k = ((Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI)) * n;
            const i = Math.floor(k) % n, fr = k - Math.floor(k);
            const R = rad[i] * (1 - fr) + rad[(i + 1) % n] * fr;
            const d = Math.hypot(dx, dy);
            if (d > R) continue;
            let t = 0.62 - 0.32 * dx / R - 0.32 * dy / R + (r() * 2 - 1) * (o.noise || 0.05);
            if (d > R * 0.8 && dx + dy > 0) t -= 0.15;
            let c = tone(base, t);
            if (o.gloss && dx + dy < -0.55 && d > 0.3 * R && d < 0.62 * R) c = light(base, 0.55);
            g.px(x, y, c);
        }
    }
    function pellet(g, x, y, base, dir) {
        const hi = light(base, 0.32), lo = dark(base, 0.3), end = light(base, 0.15);
        if (dir === 'h') {
            g.px(x, y, hi); g.px(x + 1, y, hi); g.px(x + 2, y, end);
            g.px(x, y + 1, base); g.px(x + 1, y + 1, lo); g.px(x + 2, y + 1, dark(end, 0.15));
        } else {
            g.px(x, y, end); g.px(x + 1, y, dark(end, 0.1));
            g.px(x, y + 1, hi); g.px(x + 1, y + 1, base);
            g.px(x, y + 2, base); g.px(x + 1, y + 2, lo);
        }
    }
    const HEAP = [[1, 11, 'h'], [4, 12, 'h'], [8, 12, 'h'], [12, 11, 'v'], [3, 8, 'v'], [6, 9, 'h'], [10, 8, 'h'],
        [13, 7, 'v'], [5, 5, 'h'], [9, 5, 'v'], [7, 2, 'h'], [1, 5, 'v'], [11, 3, 'v']];
    function heap(g, base, pick) { HEAP.forEach(([x, y, d], i) => pellet(g, x, y, pick ? pick(i) : base, d)); }

    // ---- machine fronts: draw(g, f, on), f the frame 0..7

    const F = {};

    // Latex meets dilute acid in an open tank; curds of coagulum float up and drift.
    F.coagulation_tank = (g, f, on) => {
        A.vessel(g, 92, [1, 14]);
        const latex = [236, 230, 208];
        const s = A.sightGlass(g, 3, 3, 12, 11, latex, 0.6, f, false, 31);
        [[4, 0], [6, 1], [9, 0], [10, 1]].forEach(([x, d], i) => {
            const bob = on ? ((f + i * 3) >> 2) % 2 : 0;
            const y = Math.min(10, s + 1 + d + bob);
            g.px(x, y, [250, 244, 222]); g.px(x + 1, y, [206, 196, 160]);
        });
        g.rect(7, 2, 8, 3, FRAME[1]); g.px(7, 2, STEEL[3]);
        if (on) { const y = 4 + (f % 4); if (y < s) g.px(8, y, [196, 226, 120]); }
        g.rect(3, 13, 6, 13, [196, 206, 92]); g.px(3, 13, [230, 236, 140]);
        A.lamp(g, 12, 12, on);
    };

    // Twin co-rotating screws behind the barrel window, the melt warming from left to right.
    F.compounding_extruder = (g, f, on) => {
        A.casing(g, 141);
        A.recess(g, 2, 4, 13, 9);
        g.rect(4, 1, 9, 1, STEEL[3]); g.rect(5, 2, 8, 2, STEEL[2]); g.rect(6, 3, 7, 4, FRAME[1]);
        const ph = on ? f : 0;
        for (let x = 3; x <= 12; x++) {
            const melt = mix([214, 210, 196], [222, 140, 60], (x - 3) / 9 * (on ? 1 : 0.4));
            const k1 = (x + ph) % 3, k2 = (x + ph + 1) % 3;
            const c1 = k1 === 0 ? STEEL[4] : k1 === 1 ? STEEL[2] : melt;
            const c2 = k2 === 0 ? STEEL[4] : k2 === 1 ? STEEL[2] : melt;
            g.px(x, 5, c1); g.px(x, 6, dark(c1, 0.3)); g.px(x, 7, c2); g.px(x, 8, dark(c2, 0.3));
        }
        A.heaterRow(g, 3, 12, 10, on, f);
        A.recess(g, 8, 11, 12, 13, [16, 40, 30]);
        g.rect(9, 12, 11, 12, on ? [70, 170, 100] : [36, 70, 46]);
        if (on) g.px(9 + (f % 3), 12, [170, 250, 190]);
        A.lamp(g, 3, 11, on);
        A.tier(g, 'MV');
    };

    // A water trough: the extruded pipe runs through it under spray nozzles.
    F.cooling_tank = (g, f, on) => {
        A.casing(g, 142);
        A.recess(g, 2, 3, 13, 11);
        for (let y = 6; y <= 10; y++) for (let x = 3; x <= 12; x++) g.px(x, y, mix([80, 150, 196], [28, 64, 104], (y - 6) / 4));
        for (let x = 3; x <= 12; x++) g.px(x, 6, (x + (on ? f : 0)) % 4 === 0 ? [200, 232, 246] : [120, 186, 222]);
        for (let x = 3; x <= 12; x++) { g.px(x, 8, [62, 66, 72]); g.px(x, 9, [30, 32, 36]); }
        if (on) g.px(3 + (f * 2) % 10, 8, [128, 134, 142]);
        for (const x of [4, 8, 11]) {
            g.px(x, 4, STEEL[3]);
            if (on && (f + x) % 2 === 0) g.px(x, 5, [170, 220, 250]);
        }
        A.glare(g, 3, 4);
        A.lamp(g, 12, 12, on);
    };

    // A film roll building up on its core; a tape mark shows it turning.
    F.film_winder = (g, f, on) => {
        A.casing(g, 143);
        A.roundWindow(g, 8, 7, 5.5, (x, y, d, ang) => {
            if (d < 1.2) return STEEL[3];
            if (d < 2.1) return [150, 118, 80];
            const a = ((ang - (on ? f * 45 : 0)) % 360 + 360) % 360;
            if (a < 22) return [146, 158, 166];
            return Math.floor(d * 1.5) % 2 ? [222, 230, 234] : [196, 206, 214];
        });
        for (let x = 2; x <= 9; x++) g.px(x, 13, x % 2 ? [222, 230, 234] : [196, 206, 214]);
        g.px(10, 12, [222, 230, 234]); g.px(10, 13, STEEL[4]);
        A.lamp(g, 12, 13, on);
    };

    // Caterpillar pads grip the pipe above; a saw blade cuts it to length below.
    F.haul_off_cutter = (g, f, on) => {
        A.casing(g, 144);
        A.recess(g, 2, 2, 13, 8);
        const ph = on ? f : 0;
        for (let x = 3; x <= 12; x++) {
            g.px(x, 3, (x + ph) % 2 ? FRAME[3] : FRAME[1]);
            g.px(x, 7, (x - ph + 16) % 2 ? FRAME[3] : FRAME[1]);
            g.px(x, 4, [72, 76, 82]); g.px(x, 5, [46, 48, 52]); g.px(x, 6, [28, 30, 32]);
        }
        const spin = on ? f * 30 : 0;
        for (let y = 8; y <= 14; y++) for (let x = 7; x <= 14; x++) {
            const dx = x + 0.5 - 10.5, dy = y + 0.5 - 11.5, d = Math.hypot(dx, dy);
            const a = ((Math.atan2(dy, dx) * 180 / Math.PI - spin) % 60 + 60) % 60;
            if (d < 0.9) g.px(x, y, FRAME[1]);
            else if (d < 2.2) g.px(x, y, dx + dy < 0 ? [214, 218, 224] : [160, 166, 172]);
            else if (d < 3.1 && a < 30) g.px(x, y, [196, 200, 206]);
        }
        if (on && f % 2 === 0) { g.px(7, 13, HOT[1]); g.px(6, 14, HOT[2]); }
        A.lamp(g, 3, 11, on);
    };

    // An internal mixer: two lobed rotors in a dark chamber, the ram pressing down from above.
    F.rubber_mixer = (g, f, on) => {
        A.casing(g, 145);
        A.recess(g, 2, 6, 13, 13, [34, 30, 28]);
        const rot = (cx, ph) => {
            for (let y = 7; y <= 12; y++) for (let x = 3; x <= 12; x++) {
                const dx = x + 0.5 - cx, dy = y + 0.5 - 9.5, d = Math.hypot(dx, dy);
                if (d > 2.6) continue;
                const a = ((Math.atan2(dy, dx) * 180 / Math.PI - ph) % 180 + 180) % 180;
                g.px(x, y, d < 0.8 ? STEEL[4] : a < 70 ? STEEL[3] : STEEL[1]);
            }
        };
        const ph = on ? f * 22.5 : 0;
        rot(5.5, ph); rot(10.5, -ph);
        const t = on ? [0, 1, 2, 2, 1, 0, 0, 0][f] : 0;
        g.rect(5, 1, 10, 1, FRAME[2]);
        g.rect(7, 2, 8, 2 + t, STEEL[4]); g.rect(8, 2, 8, 2 + t, STEEL[2]);
        g.rect(5, 3 + t, 10, 3 + t, STEEL[3]); g.rect(5, 4 + t, 10, 4 + t, STEEL[1]);
        A.lamp(g, 12, 2, on);
        A.tier(g, 'MV');
    };

    // A two-roll mill: the rubber sheet comes through the nip between two chilled rolls.
    F.sheeting_press = (g, f, on) => {
        A.casing(g, 146);
        for (const x of [2, 12]) { g.rect(x, 2, x, 11, FRAME[2]); g.rect(x + 1, 2, x + 1, 11, FRAME[1]); }
        const roll = (y0, dir) => {
            const shade = [STEEL[2], STEEL[4], STEEL[3], STEEL[1]];
            const groove = on ? ((dir * f) % 4 + 4) % 4 : 0;
            for (let k = 0; k < 4; k++) for (let x = 4; x <= 11; x++) g.px(x, y0 + k, k === groove ? dark(shade[k], 0.2) : shade[k]);
            g.px(3, y0 + 1, STEEL[4]); g.px(12, y0 + 1, STEEL[4]);
        };
        roll(2, 1); roll(8, -1);
        for (let x = 4; x <= 11; x++) { g.px(x, 6, [96, 84, 66]); g.px(x, 7, [62, 56, 48]); }
        if (on) g.px(4 + (f % 8), 6, [124, 110, 88]);
        for (let x = 3; x <= 9; x++) g.px(x, 13, (x >> 1) % 2 ? [230, 196, 40] : [32, 32, 32]);
        A.lamp(g, 12, 12, on);
    };

    // A single screw under the barrel window, pellets melting as they move to the die.
    F.single_screw_extruder = (g, f, on) => {
        A.casing(g, 147);
        A.recess(g, 2, 4, 13, 8);
        g.rect(2, 1, 6, 1, STEEL[3]); g.rect(3, 2, 5, 2, STEEL[2]); g.rect(4, 3, 4, 4, FRAME[1]);
        const ph = on ? f : 0;
        for (let y = 5; y <= 7; y++) for (let x = 3; x <= 12; x++) {
            const melt = mix([226, 222, 206], [226, 132, 56], ((x - 3) / 9) * (on ? 1 : 0.35));
            const k = ((x - y + ph) % 3 + 3) % 3;
            g.px(x, y, k === 0 ? (y === 5 ? STEEL[4] : STEEL[3]) : y === 7 ? dark(melt, 0.2) : melt);
        }
        for (const x of [6, 9, 12]) { g.px(x, 4, COPPER[2]); g.px(x, 8, COPPER[1]); }
        A.heaterRow(g, 3, 12, 10, on, f);
        A.recess(g, 3, 11, 8, 13, [16, 40, 30]);
        g.rect(4, 12, 7, 12, on ? [70, 170, 100] : [36, 70, 46]);
        if (on) g.px(4 + (f % 4), 12, [170, 250, 190]);
        A.lamp(g, 11, 11, on);
        A.tier(g, 'MV');
    };

    // Ribbed rubber sheets hang on rods in the smoke; a firebox glows underneath.
    F.smokehouse = (g, f, on) => {
        A.casing(g, 148);
        A.recess(g, 2, 2, 12, 11, [40, 34, 30]);
        g.rect(3, 3, 11, 3, STEEL[3]);
        for (const x0 of [3, 6, 9]) for (let y = 4; y <= 10; y++) {
            const c = y % 2 ? [156, 98, 44] : [128, 78, 34];
            g.px(x0, y, c); g.px(x0 + 1, y, dark(c, 0.15));
        }
        if (on) for (let i = 0; i < 3; i++) {
            const x = [5, 8, 11][i];
            g.px(x, 10 - ((i * 3 + f) % 7), [124, 116, 106]);
        }
        g.rect(3, 12, 11, 12, FRAME[0]);
        A.heaterRow(g, 4, 10, 13, on, f);
        g.rect(13, 5, 13, 8, STEEL[4]); g.rect(13, 8, 13, 8, STEEL[2]);
        A.lamp(g, 12, 12, on);
    };

    // A packed column: vapour above the trays, the recovered solvent collecting in the sump.
    F.solvent_recovery_tower = (g, f, on) => {
        A.columnBase(g);
        A.recess(g, 3, 2, 9, 6);
        g.rect(4, 4, 8, 4, STEEL[3]);
        if (on) { g.px(4 + (f % 5), 3, [200, 214, 222]); g.px(4 + ((f + 2) % 5), 5, [170, 220, 160]); }
        A.sightGlass(g, 3, 8, 10, 12, [176, 214, 150], 0.5, f, on ? 2 : false, 52);
        A.gauge(g, 12, 4, on ? 0.65 : 0.1);
        A.lamp(g, 12, 9, on);
        A.tier(g, 'MV', 14, 3, 11);
    };

    // A platen press: the heated upper platen closes on the mould while running.
    F.vulcanizer = (g, f, on) => {
        A.casing(g, 149);
        g.rect(2, 2, 13, 2, FRAME[2]); g.rect(2, 3, 13, 3, FRAME[1]);
        for (const x of [2, 12]) { g.rect(x, 4, x, 12, FRAME[2]); g.rect(x + 1, 4, x + 1, 12, FRAME[1]); }
        g.rect(4, 4, 11, 12, [28, 30, 34]);
        const t = on ? [0, 1, 2, 2, 2, 2, 1, 0][f] : 0;
        const hot = on ? HOT[2] : [120, 70, 50];
        g.rect(5, 8, 10, 9, [74, 80, 92]); g.rect(6, 9, 9, 9, [40, 42, 48]);
        g.rect(7, 4, 8, 4 + t, STEEL[4]); g.rect(8, 4, 8, 4 + t, STEEL[2]);
        g.rect(4, 5 + t, 11, 5 + t, STEEL[3]); g.rect(4, 6 + t, 11, 6 + t, hot);
        g.rect(4, 10, 11, 10, hot); g.rect(4, 11, 11, 11, STEEL[2]);
        if (on && t === 2 && f % 2) { g.px(4, 9, [150, 150, 150]); g.px(11, 9, [150, 150, 150]); }
        A.lamp(g, 4, 12, on);
        A.tier(g, 'MV', 14, 7, 11);
    };

    const FRONT_DIR = {
        coagulation_tank: 'm8', compounding_extruder: 'm6', cooling_tank: 'm7', film_winder: 'm7',
        haul_off_cutter: 'm7', rubber_mixer: 'm8', sheeting_press: 'm8', single_screw_extruder: 'm7',
        smokehouse: 'm8', solvent_recovery_tower: 'm5', vulcanizer: 'm8',
    };

    // ---- blocks

    const BRONZE = [[86, 52, 26], [132, 82, 40], [178, 120, 60], [212, 158, 92], [240, 204, 148]];
    // Pipe faces sample the middle band of the texture both ways, so it is painted as a cross:
    // two shaded tube bands and a flanged junction where they meet.
    function pipeCross(g, P, ringHi, ringLo) {
        g.rect(0, 0, 15, 15, mix(FRAME[0], FRAME[1], 0.5));
        const cyl = [P[1], P[3], P[4], P[3], P[2], P[0]];
        for (let i = 0; i < 16; i++) for (let k = 0; k < 6; k++) { g.px(5 + k, i, cyl[k]); g.px(i, 5 + k, cyl[k]); }
        for (const i of [0, 15]) for (let k = 0; k < 6; k++) { g.px(5 + k, i, dark(cyl[k], 0.25)); g.px(i, 5 + k, dark(cyl[k], 0.25)); }
        g.rect(6, 6, 9, 9, P[2]);
        g.bevel(5, 5, 10, 10, ringHi, ringLo);
        for (const [x, y] of [[6, 6], [9, 6], [6, 9], [9, 9]]) g.px(x, y, P[4]);
    }

    const B = {};
    B.fluid_pipe = () => A.paint(g => pipeCross(g, STEEL, [110, 170, 230], [34, 70, 120]));
    B.bronze_fluid_pipe = () => A.paint(g => pipeCross(g, BRONZE, BRONZE[4], [70, 40, 20]));

    // Only rows 11-15 (side) and columns 6-9 (top) are sampled by the flat tube model.
    B.drip_pipe = () => A.paint(g => {
        const T = [[22, 24, 22], [38, 42, 38], [58, 64, 58], [84, 92, 84]];
        for (let y = 0; y < 16; y++) for (let x = 6; x <= 9; x++) g.px(x, y, [T[1], T[3], T[2], T[0]][x - 6]);
        for (let y = 11; y < 16; y++) for (let x = 0; x < 16; x++) g.px(x, y, [T[3], T[2], T[2], T[1], T[0]][y - 11]);
        for (const y of [2, 6, 10, 14]) { g.px(7, y, [80, 150, 210]); g.px(8, y, [40, 92, 150]); }
        for (const x of [2, 6, 10, 14]) { g.px(x, 13, [80, 150, 210]); g.px(x, 14, [40, 92, 150]); }
    });

    // Black mulch film laid over the soil, with planting holes.
    B.mulched_farmland = () => A.paint(g => {
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const s = (x + y) % 8;
            g.px(x, y, s === 0 ? [62, 66, 70] : s === 1 ? [50, 52, 56] : [40, 42, 44]);
        }
        for (const [cx, cy, sprout] of [[4, 4, true], [11.5, 11, false]]) {
            disc(g, cx, cy, 1.9, 1.7, (x, y, dx, dy, d) => d > 0.75 ? [24, 20, 16] : dx + dy < 0 ? [104, 72, 46] : [84, 56, 36]);
            if (sprout) { g.px(3, 3, [112, 176, 64]); g.px(4, 3, [80, 140, 50]); }
        }
        for (let x = 0; x < 16; x++) g.px(x, 15, [30, 32, 34]);
    });

    B.bulk_silo = () => A.paint(g => {
        A.vessel(g, 77, [1, 7, 14]);
        for (const x of [4, 11]) for (let y = 3; y <= 13; y++) if (y !== 7 && y !== 8) g.tint(x, y, FRAME[0], 0.35);
        for (const row of [1, 7, 14]) for (const x of [2, 6, 9, 13]) g.px(x, row, STEEL[4]);
        A.recess(g, 6, 9, 9, 13);
        for (let y = 11; y <= 12; y++) for (let x = 7; x <= 8; x++) g.px(x, y, (x + y) % 2 ? [228, 226, 214] : [196, 194, 184]);
        g.rect(5, 3, 10, 5, [52, 110, 176]); g.bevel(5, 3, 10, 5, [96, 150, 210], [30, 70, 120]);
        g.rect(7, 4, 7, 4, [236, 240, 244]); g.rect(9, 4, 9, 4, [236, 240, 244]);
    });

    B.gas_tank = () => A.paint(g => {
        A.vessel(g, 78, [1, 14]);
        for (let x = 0; x < 16; x++) {
            const s = A.vcol(x);
            g.px(x, 8, mix([150, 120, 20], [246, 214, 70], s));
            g.px(x, 9, mix([120, 94, 16], [214, 180, 40], s));
        }
        A.gauge(g, 8, 4, 0.55);
        for (const [x, y] of [[8, 11], [7, 12], [8, 12], [9, 12], [8, 13]]) g.px(x, y, [204, 52, 40]);
        g.px(8, 12, [246, 214, 70]);
    });

    B.logistics_port_ii = () => A.paint(g => {
        A.casing(g, 79);
        A.recess(g, 3, 3, 12, 12);
        disc(g, 8, 8, 3.6, 3.6, (x, y, dx, dy, d) => d < 0.55 ? [12, 16, 20] : dx + dy < 0 ? [96, 206, 226] : [40, 126, 150]);
        for (const [x, y] of [[5, 5], [10, 5], [5, 10], [10, 10]]) g.px(x, y, [96, 206, 226]);
        A.glare(g, 4, 4);
    });

    // Top face is the whole texture; the sides only sample rows 12-15 as the frame.
    B.conveyor_ii = () => A.paint(g => {
        const RAIL = [[32, 58, 104], [52, 96, 160], [90, 140, 200]];
        for (let y = 0; y <= 11; y++) for (let x = 2; x <= 13; x++) g.px(x, y, y % 3 === 0 ? [72, 74, 80] : y % 3 === 1 ? [46, 46, 50] : [38, 38, 42]);
        for (let y = 0; y <= 11; y++) {
            g.px(0, y, RAIL[2]); g.px(1, y, RAIL[1]); g.px(14, y, RAIL[1]); g.px(15, y, RAIL[0]);
            if (y % 4 === 1) { g.px(1, y, STEEL[4]); g.px(14, y, STEEL[4]); }
        }
        g.rect(0, 12, 15, 12, STEEL[3]);
        g.rect(0, 13, 15, 14, RAIL[1]); g.rect(0, 14, 15, 14, RAIL[0]);
        for (const x of [1, 5, 9, 13]) { g.px(x, 13, STEEL[4]); g.px(x + 1, 13, STEEL[2]); }
        g.rect(0, 15, 15, 15, FRAME[0]);
    });

    const BLOCK_DIR = {
        bronze_fluid_pipe: 'm9', bulk_silo: 'm9', conveyor_ii: 'm9', fluid_pipe: 'm9', gas_tank: 'm9',
        logistics_port_ii: 'm9', drip_pipe: 'm7', mulched_farmland: 'm7',
    };

    // ---- item icons

    const TS = [[48, 54, 66], [82, 90, 104], [118, 126, 142], [158, 166, 182], [204, 210, 222]];

    // A pipe length lying from lower left to upper right, its open end towards the viewer.
    function tube(g, base, o) {
        o = o || {};
        const ux = Math.SQRT1_2, uy = -Math.SQRT1_2, vx = Math.SQRT1_2, vy = Math.SQRT1_2;
        const P0 = [2.5, 13.5], len = o.len || 13.5, R = 2.3;
        const P1 = [P0[0] + ux * len, P0[1] + uy * len];
        const r = A.rng(o.seed || 3);
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const rx = x + 0.5 - P0[0], ry = y + 0.5 - P0[1];
            const t = rx * ux + ry * uy, s = rx * vx + ry * vy;
            if (t < 0 || t > len || Math.abs(s) > R) continue;
            let k = 0.74 - 0.32 * (s / R);
            if (o.rough) k += (r() * 2 - 1) * 0.1;
            let c = tone(base, k);
            if (o.stripe && s > -1.7 && s < -0.8) c = o.stripe;
            g.px(x, y, c);
        }
        disc(g, P1[0], P1[1], R, R, (x, y, dx, dy, d) => d < 0.5 ? dark(base, 0.7) : d < 0.66 ? dark(base, 0.42) : tone(base, 0.82 - 0.25 * (dx + dy)));
    }
    // A roll of film seen from the side, its wound end on the right.
    function roll(g, film, o) {
        o = o || {};
        const x0 = 2, x1 = o.long ? 11 : 10, y0 = 5, y1 = 12, cy = 8.5;
        const body = y => tone(film, 0.88 - 0.56 * (y - y0) / (y1 - y0));
        disc(g, x0 + 0.5, cy, 1.4, 4.1, (x, y) => body(y));
        for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) g.px(x, y, body(y));
        if (o.holes) for (let x = x0 + 1; x <= x1; x += 3) g.px(x, 9, dark(film, 0.5));
        disc(g, x1 + 0.5, cy, 2.4, 4.1, (x, y, dx, dy, d) => d < 0.3 ? [36, 32, 28] : d < 0.48 ? [178, 140, 96] : Math.floor(d * 6) % 2 ? light(film, 0.12) : light(film, 0.32));
        if (o.tail) for (let y = y1 + 1; y <= 14; y++) for (let x = x0 + 1 + (y - y1); x <= x0 + 4 + (y - y1); x++) g.px(x, y, tone(film, 0.7 - 0.1 * (y - y1)));
    }
    // The blown-film bubble rising from its ring die.
    function bubble(g, film) {
        for (let y = 0; y <= 11; y++) {
            const w = y < 4 ? [1.5, 3, 3.9, 4.4][y] : y <= 8 ? 4.6 : [4.0, 3.2, 2.4][y - 9];
            for (let x = 0; x < 16; x++) {
                const dx = (x + 0.5 - 8) / w;
                if (Math.abs(dx) > 1) continue;
                let c = tone(film, 0.72 - 0.3 * dx);
                if (Math.abs(dx + 0.45) < 0.16 && y > 1 && y < 10) c = light(film, 0.6);
                g.px(x, y, c);
            }
        }
        g.rect(5, 12, 10, 12, TS[4]); g.rect(4, 13, 11, 13, TS[2]); g.rect(4, 14, 11, 14, TS[0]);
        g.px(8, 12, [30, 32, 36]);
    }
    // A coil seen from above at an angle: concentric turns with dark gaps.
    function coil(g, base, o) {
        o = o || {};
        const rings = o.rings || 3;
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const dx = (x + 0.5 - 8) / 7, dy = (y + 0.5 - 8.5) / 5.2, d = Math.hypot(dx, dy);
            if (d > 1 || d < 0.24) continue;
            const band = d * rings, fr = band - Math.floor(band);
            if (fr > 0.74) continue;
            let c = tone(base, 0.68 - 0.28 * dx - 0.28 * dy + (fr < 0.25 ? 0.16 : 0));
            if (o.dots && fr > 0.25 && fr < 0.6 && Math.floor((Math.atan2(dy, dx) + Math.PI) * 6 / Math.PI) % 3 === 0) c = o.dots;
            g.px(x, y, c);
        }
    }
    // A round tool-steel die plate with a bolt circle.
    function die(g, feat) {
        disc(g, 8, 8, 6.6, 6.6, (x, y, dx, dy, d) => d > 0.86 ? tone(TS[2], 0.74 - 0.4 * (dx + dy)) : tone(TS[2], 0.6 - 0.2 * (dx + dy)));
        for (let i = 0; i < 6; i++) {
            const a = (i * 60 + 30) * Math.PI / 180;
            const bx = Math.round(8 + Math.cos(a) * 5.1 - 0.5), by = Math.round(8 + Math.sin(a) * 5.1 - 0.5);
            g.px(bx, by, TS[4]); g.px(bx + 1, by + 1, TS[0]);
        }
        feat();
    }
    // A mould half with guide pins; cav(x, y) marks the cavity.
    function moldPlate(g, cav, seed) {
        A.noise(g, 1, 2, 14, 12, TS[1], TS[3], seed, 0.55, 0.05);
        g.rect(1, 13, 14, 13, TS[1]); g.rect(1, 14, 14, 14, TS[0]);
        g.bevel(1, 2, 14, 12, TS[4], TS[0]);
        for (const [x, y] of [[2, 3], [12, 3], [2, 10], [12, 10]]) { g.px(x, y, TS[4]); g.px(x + 1, y, TS[2]); g.px(x, y + 1, TS[2]); g.px(x + 1, y + 1, TS[0]); }
        for (let y = 3; y <= 11; y++) for (let x = 2; x <= 13; x++) {
            if (cav(x, y)) {
                let c = [40, 44, 54];
                if (!cav(x, y - 1) || !cav(x - 1, y)) c = [22, 24, 30];
                else if (!cav(x, y + 1) || !cav(x + 1, y)) c = [74, 80, 94];
                g.px(x, y, c);
            } else if (cav(x, y - 1) || cav(x - 1, y)) g.px(x, y, TS[4]);
        }
    }
    function jar(g, body, cap, label, mark) {
        for (let y = 5; y <= 14; y++) for (let x = 4; x <= 11; x++) g.px(x, y, mix(dark(body, 0.42), light(body, 0.35), A.vcol(x, 4, 11)));
        g.rect(5, 5, 10, 5, light(body, 0.4));
        g.rect(6, 3, 9, 4, light(body, 0.15)); g.px(9, 4, dark(body, 0.3));
        g.rect(5, 1, 10, 2, cap); g.rect(5, 1, 10, 1, light(cap, 0.3)); g.px(10, 2, dark(cap, 0.35));
        for (let y = 8; y <= 11; y++) for (let x = 4; x <= 11; x++) g.px(x, y, mix(dark(label, 0.3), light(label, 0.2), A.vcol(x, 4, 11)));
        if (mark) mark();
    }

    const PE_BLACK = [52, 54, 58], PVC_GREY = [176, 180, 184];
    const PE_FILM = [212, 226, 234], PLA_FILM = [232, 224, 188];
    const I = {};

    I.bio_pe_pellet = g => { heap(g, null, i => i % 4 === 0 ? [168, 212, 146] : [216, 232, 206]); outline(g); };
    I.filled_pe_pellet = g => { heap(g, null, i => i % 3 === 0 ? [176, 174, 166] : [212, 210, 202]); outline(g); };
    I.rigid_pvc_pellet = g => { heap(g, null, i => i % 3 === 1 ? [214, 216, 212] : [238, 238, 232]); outline(g); };
    I.soft_pvc_pellet = g => { heap(g, null, i => i % 3 === 0 ? [214, 238, 232] : [180, 218, 210]); outline(g); };
    I.color_masterbatch = g => { const C = [[204, 58, 48], [52, 110, 196], [232, 190, 48], [72, 160, 76]]; heap(g, null, i => C[i % 4]); outline(g); };
    I.pe_powder = g => { pile(g, 61, [238, 240, 238], [[[255, 255, 255], 0.12], [[206, 212, 216], 0.1]], { height: 7 }); outline(g, 0.55); };
    I.wet_pe_powder = g => {
        pile(g, 62, [220, 226, 230], [[[196, 210, 220], 0.12]], { height: 7, lumpy: true });
        for (const [x, y] of [[4, 10], [8, 7], [10, 10], [6, 12]]) { g.px(x, y, [150, 196, 232]); g.px(x, y - 1, [236, 248, 255]); }
        outline(g, 0.55);
    };
    I.polyolefin_catalyst = g => {
        jar(g, [150, 156, 166], [196, 52, 40], [112, 72, 172], () => { g.rect(7, 9, 8, 10, [236, 230, 246]); g.px(8, 10, [180, 160, 214]); });
        outline(g);
    };
    I.strand = g => {
        const c = [222, 220, 208];
        for (let k = 0; k < 3; k++) for (let x = 1; x <= 14; x++) {
            const y = 3 + k * 4 + Math.round(Math.sin(x * 0.55 + k * 1.3) * 0.9);
            g.px(x, y, light(c, 0.25)); g.px(x, y + 1, dark(c, 0.22));
        }
        outline(g, 0.5);
    };

    I.pe_pipe = g => { tube(g, PE_BLACK, { stripe: [62, 124, 204] }); outline(g, 0.5); };
    I.pvc_pipe = g => { tube(g, PVC_GREY); outline(g, 0.55); };
    I.pe_pipe_blank = g => { tube(g, [64, 66, 70], { rough: true, len: 11.5, seed: 4 }); outline(g, 0.5); };
    I.pvc_pipe_blank = g => { tube(g, [192, 194, 196], { rough: true, len: 11.5, seed: 5 }); outline(g, 0.55); };
    I.hot_pe_pipe = g => { tube(g, [222, 108, 40]); g.px(6, 9, HOT[0]); g.px(9, 6, HOT[0]); outline(g, 0.45); };
    I.hot_pvc_pipe = g => { tube(g, [232, 150, 84]); g.px(6, 9, HOT[0]); g.px(9, 6, HOT[0]); outline(g, 0.45); };

    I.pe_film_roll = g => { roll(g, PE_FILM, { tail: true }); outline(g, 0.55); };
    I.pla_film_roll = g => { roll(g, PLA_FILM, { tail: true }); outline(g, 0.55); };
    I.mulch_film_pe = g => { roll(g, [54, 56, 58], { long: true, holes: true }); outline(g, 0.5); };
    I.mulch_film_pla = g => { roll(g, [206, 190, 140], { long: true, holes: true }); outline(g, 0.55); };
    I.pe_film_bubble = g => { bubble(g, [204, 222, 232]); outline(g, 0.55); };
    I.pla_film_bubble = g => { bubble(g, [230, 222, 186]); outline(g, 0.55); };
    I.waste_film = g => {
        lump(g, 8, 9, 6.2, 4.8, [96, 98, 94], 71, { noise: 0.05 });
        for (const [x, y] of [[5, 10], [6, 10], [5, 11], [10, 7], [11, 7], [9, 12], [10, 12]]) g.px(x, y, [108, 78, 50]);
        line(g, 4, 8, 8, 11, [64, 66, 62]); line(g, 8, 6, 12, 9, [140, 142, 136]);
        outline(g, 0.5);
    };
    I.drip_tape = g => { coil(g, [48, 52, 48], { dots: [72, 150, 212] }); outline(g, 0.5); };
    I.hot_drip_tape = g => { coil(g, [222, 116, 44]); outline(g, 0.45); };
    I.hot_coated_wire = g => {
        coil(g, [226, 128, 52], { rings: 4 });
        line(g, 11, 5, 14, 2, COPPER[2]); g.px(14, 1, COPPER[3]); g.px(12, 5, COPPER[1]);
        outline(g, 0.45);
    };

    I.coating_die = g => die(g, () => {
        disc(g, 8, 8, 2.9, 2.9, (x, y, dx, dy) => tone(TS[3], 0.7 - 0.3 * (dx + dy)));
        disc(g, 8, 8, 1.9, 1.9, () => [22, 24, 28]);
        g.rect(7, 7, 8, 8, COPPER[2]); g.px(7, 7, COPPER[3]); g.px(8, 8, COPPER[1]);
        outline(g, 0.5);
    });
    I.film_die = g => die(g, () => {
        disc(g, 8, 8, 4.4, 4.4, (x, y, dx, dy, d) => d > 0.8 ? [24, 26, 30] : tone(TS[3], 0.68 - 0.25 * (dx + dy)));
        for (const [x, y] of [[6, 5], [10, 7], [8, 10], [5, 8]]) g.tint(x, y, TS[0], 0.35);
        g.rect(7, 7, 8, 8, [24, 26, 30]);
        outline(g, 0.5);
    });
    I.pipe_die = g => die(g, () => {
        disc(g, 8, 8, 3.8, 3.8, (x, y, dx, dy, d) => d > 0.5 ? [26, 28, 32] : tone(TS[4], 0.66 - 0.25 * (dx + dy)));
        for (const [x, y] of [[7, 4], [8, 4], [4, 9], [11, 9]]) g.px(x, y, TS[2]);
        outline(g, 0.5);
    });
    I.seal_mold = g => { moldPlate(g, (x, y) => { const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 7.5); return d >= 1.9 && d <= 3.6; }, 81); outline(g, 0.5); };
    I.belt_mold = g => { moldPlate(g, (x, y) => (x >= 3 && x <= 12 && y >= 5 && y <= 7) || (y === 8 && x >= 3 && x <= 12 && x % 2 === 1), 82); outline(g, 0.5); };

    I.rubber_seal = g => {
        const c = [58, 56, 56];
        disc(g, 8, 8.5, 6.6, 5, (x, y, dx, dy, d) => {
            if (d < 0.58) return null;
            const m = (d - 0.58) / 0.42, side = (dx + dy) / d;
            if (m > 0.3 && m < 0.6 && side < -0.6) return light(c, 0.5);
            return tone(c, 0.62 - 0.3 * side * (m - 0.5) * 2 + 0.12 * (1 - Math.abs(m - 0.5) * 2));
        });
        outline(g, 0.5);
    };
    I.rubber_belt = g => {
        const c = [50, 48, 48];
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const X = x + 0.5, Y = y + 0.5, px = Math.max(4.5, Math.min(11.5, X));
            const d = Math.hypot(X - px, Y - 8);
            if (d > 4.2 || d < 2.6) continue;
            const m = (d - 2.6) / 1.6;
            let k = Y < 8 ? 0.82 - 0.22 * m : 0.42 + 0.12 * m;
            if ((x + y) % 3 === 0) k -= 0.08;
            let col = tone(c, k);
            if (Y < 6 && x % 3 === 0 && m > 0.3 && m < 0.7) col = [178, 152, 102];
            g.px(x, y, col);
        }
        outline(g, 0.5);
    };
    I.rubber_sheet = g => {
        const c = [214, 198, 152];
        poly(g, [[1, 9], [9, 5], [15, 8], [7, 12.5]], (x, y) => tone(dark(c, 0.14), 0.6 + ((x + y) % 2 ? 0 : -0.12)));
        poly(g, [[1, 7], [9, 3], [15, 6], [7, 10.5]], (x, y) => tone(c, 0.74 - 0.04 * (y - 3) + ((x - y + 20) % 3 === 0 ? -0.16 : 0)));
        outline(g, 0.55);
    };
    I.rubber_compound = g => {
        lump(g, 6, 10, 4.6, 3.4, [50, 48, 48], 83, { gloss: true });
        lump(g, 10.5, 7, 4.2, 3.6, [58, 56, 56], 84, { gloss: true });
        outline(g, 0.4);
    };
    I.coagulum = g => {
        lump(g, 8, 9, 6.4, 5, [230, 220, 182], 91, { gloss: true, noise: 0.04 });
        g.px(5, 7, [240, 250, 255]); g.px(10, 12, [190, 218, 232]); g.px(11, 11, [240, 250, 255]);
        outline(g, 0.5);
    };

    const ITEM_DIR = {
        belt_mold: 'm8', coagulum: 'm8', rubber_belt: 'm8', rubber_compound: 'm8', rubber_seal: 'm8', rubber_sheet: 'm8', seal_mold: 'm8',
        bio_pe_pellet: 'm5', pe_powder: 'm5', polyolefin_catalyst: 'm5', wet_pe_powder: 'm5',
        color_masterbatch: 'm6', filled_pe_pellet: 'm6', rigid_pvc_pellet: 'm6', soft_pvc_pellet: 'm6', strand: 'm6',
    };

    // ---- writers

    function all() {
        const missed = [];
        const sync = (dir, id, name, img) => { if (!A.syncModel('tools/models/' + dir + '/' + id + '.bbmodel', name, img)) missed.push(name); };
        for (const id in F) {
            const idle = A.paint(g => F[id](g, 0, false));
            const active = A.paint((g, f) => F[id](g, f, true), 8);
            A.save(idle, 'block/machine/process/' + id + '_front.png');
            A.save(active, 'block/machine/process/' + id + '_front_active.png', 2);
            sync(FRONT_DIR[id], id, id + '_front', idle);
            sync(FRONT_DIR[id], id, id + '_front_active', active);
        }
        for (const id in B) {
            const img = B[id]();
            A.save(img, 'block/' + id + '.png');
            sync(BLOCK_DIR[id], id, id, img);
        }
        for (const id in I) {
            const img = A.paint(I[id]);
            A.save(img, 'item/component/' + id + '.png');
            sync(ITEM_DIR[id] || 'm7', id, id, img);
        }
        return JSON.stringify({ fronts: Object.keys(F).length, blocks: Object.keys(B).length, items: Object.keys(I).length, unsynced: missed });
    }

    window.IUCEPolymer = { FRONTS: F, BLOCKS: B, ICONS: I, all };
    return 'polymer loaded';
})();
