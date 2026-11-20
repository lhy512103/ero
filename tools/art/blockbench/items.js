// Item icons for the PLA and PVC lines, painted inside Blockbench after core.js.
// IUCEItems.all() writes every icon and updates the matching .bbmodel sources.
// Vanilla-style sprites: transparent background, light from the upper left, a dark outline
// in the object's own hue, and a few shared shapes per family so the set reads as one.
(function () {
    const A = window.IUCEArt;
    const { mix, light, dark, hex } = A;

    // ---- primitives

    function tone(base, t) {
        if (t < 0.22) return dark(base, 0.42);
        if (t < 0.45) return dark(base, 0.2);
        if (t < 0.75) return base;
        if (t < 0.92) return light(base, 0.25);
        return light(base, 0.5);
    }
    function opaque(g, x, y) { return g.alpha(x, y) > 0; }
    // A dark rim round everything drawn so far, tinted by whatever it borders.
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

    // A heap of powder. specks: [[colour, chance]] sprinkled over the surface.
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

    // An irregular lump. Facets catch the light from the upper left; overlapping lumps get an edge.
    function lump(g, cx, cy, rx, ry, base, seed, opts) {
        opts = opts || {};
        const r = A.rng(seed);
        const n = opts.facets || 7;
        const radii = []; for (let i = 0; i < n; i++) radii.push(0.78 + 0.3 * r());
        const facet = a => { const k = ((a + Math.PI) / (2 * Math.PI)) * n; const i = Math.floor(k) % n; const f = k - Math.floor(k); return [i, radii[i] * (1 - f) + radii[(i + 1) % n] * f]; };
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
            const a = Math.atan2(dy, dx); const [i, rad] = facet(a);
            const norm = Math.hypot(dx, dy);
            if (norm > rad) continue;
            const mid = (i + 0.5) / n * 2 * Math.PI - Math.PI;
            const facing = Math.cos(mid + 0.75 * Math.PI);
            const rim = norm / rad;
            let t = rim < 0.5 ? 0.66 : 0.5 + 0.42 * facing;
            t += (r() * 2 - 1) * (opts.noise || 0.06);
            let c = tone(base, t);
            if (opts.gloss && facing > 0.6 && rim > 0.45 && rim < 0.85) c = light(base, 0.55);
            if (opts.porous && r() < opts.porous) c = dark(base, 0.5);
            if (opts.sparkle && r() < opts.sparkle) c = opts.sparkleColour || light(base, 0.7);
            const edge = (rad - norm) * Math.min(rx, ry) < 1.0;
            if (edge && opaque(g, x, y)) c = dark(base, 0.5);
            g.px(x, y, c);
        }
    }

    // Cylindrical pellet seen side-on, its cut end lighter.
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
    function pelletHeap(g, base, count, tint) {
        HEAP.slice(0, count || HEAP.length).forEach(([x, y, d], i) => pellet(g, x, y, tint && i % 3 === 0 ? tint : base, d));
    }

    function cube(g, x, y, s, pal) {
        g.rect(x, y + 1, x + s - 1, y + s, pal[1]);
        g.rect(x + 1, y, x + s, y, pal[0]);
        g.rect(x + s, y + 1, x + s, y + s, pal[2]);
        g.px(x, y + 1, pal[0]);
        g.px(x + 1, y + 2, light(pal[1], 0.25));
    }
    function prism(g, x, base, h, pal) {
        for (let k = 0; k <= h; k++) {
            const y = base - k;
            if (k >= h - 1) { g.px(x + 1, y, pal[0]); continue; }
            g.px(x, y, pal[0]); g.px(x + 1, y, pal[1]); g.px(x + 2, y, pal[2]);
        }
    }
    function flake(g, cx, cy, w, h, base, tilt) {
        const pts = [[cx - w, cy + tilt], [cx + tilt, cy - h], [cx + w, cy - tilt], [cx - tilt, cy + h]];
        poly(g, pts, (x, y) => {
            const t = 0.55 - 0.3 * ((x + 0.5 - cx) / w + (y + 0.5 - cy) / h);
            return tone(base, t);
        });
    }

    // ---- containers

    function bottle(g, glass, liquid, level, cap, label) {
        const body = (x, y) => (y >= 6 && y <= 14 && x >= 4 && x <= 11) || (y === 5 && x >= 5 && x <= 10) || (y >= 3 && y <= 4 && x >= 6 && x <= 9);
        const surface = 14 - Math.round(level * 8);
        for (let y = 3; y <= 14; y++) for (let x = 4; x <= 11; x++) {
            if (!body(x, y)) continue;
            const side = (x - 4) / 7;
            const inside = liquid && y >= surface && y >= 6;
            const c = inside ? tone(liquid, 0.75 - 0.5 * side) : tone(glass, 0.8 - 0.45 * side);
            g.px(x, y, c);
        }
        if (liquid && surface >= 6) g.rect(5, surface, 10, surface, light(liquid, 0.35));
        g.rect(5, 7, 5, 12, light(glass, 0.6));
        g.rect(5, 1, 10, 2, cap); g.rect(5, 1, 10, 1, light(cap, 0.3)); g.px(10, 2, dark(cap, 0.35));
        if (label) label(g);
    }
    function can(g, body, labelColour, mark) {
        for (let y = 3; y <= 14; y++) for (let x = 3; x <= 12; x++) g.px(x, y, mix(dark(body, 0.45), light(body, 0.35), A.vcol(x, 3, 12)));
        g.rect(4, 2, 11, 2, light(body, 0.45)); g.rect(3, 3, 12, 3, light(body, 0.2));
        g.rect(9, 1, 10, 1, dark(body, 0.2));
        for (const y of [6, 12]) for (let x = 3; x <= 12; x++) g.px(x, y, dark(g.get(x, y), 0.25));
        for (let y = 8; y <= 10; y++) for (let x = 3; x <= 12; x++) g.px(x, y, mix(dark(labelColour, 0.3), light(labelColour, 0.2), A.vcol(x, 3, 12)));
        if (mark) mark(g);
    }
    function sack(g, cloth, band, mark) {
        poly(g, [[3, 5], [12, 5], [13.5, 14.5], [2, 14.5]], (x, y) => tone(cloth, 0.75 - 0.35 * ((x - 2) / 11) + ((x * 7 + y * 3) % 5 === 0 ? -0.08 : 0)));
        poly(g, [[6, 1.5], [10, 1.5], [11, 5.5], [5, 5.5]], (x, y) => tone(cloth, 0.7 - 0.3 * ((x - 5) / 6)));
        g.rect(5, 4, 10, 4, dark(cloth, 0.35));
        g.rect(4, 7, 4, 13, light(cloth, 0.25));
        g.rect(3, 10, 12, 11, band); g.rect(3, 10, 12, 10, light(band, 0.2));
        if (mark) mark(g);
    }

    // ---- tool-steel moulds

    const TS = [[48, 54, 66], [82, 90, 104], [118, 126, 142], [158, 166, 182], [204, 210, 222]];
    function mold(g, cavity, seed) {
        A.noise(g, 1, 2, 14, 12, TS[1], TS[3], seed || 7, 0.55, 0.08);
        g.rect(1, 13, 14, 13, TS[1]); g.rect(1, 14, 14, 14, TS[0]);
        g.bevel(1, 2, 14, 12, TS[4], TS[0]);
        for (const [x, y] of [[2, 3], [12, 3], [2, 10], [12, 10]]) { g.px(x, y, TS[4]); g.px(x + 1, y, TS[2]); g.px(x, y + 1, TS[2]); g.px(x + 1, y + 1, TS[0]); }
        if (!cavity) {
            // a face-milled blank: fly-cutter arcs
            for (let y = 3; y <= 11; y++) for (let x = 2; x <= 13; x++) if (Math.round(Math.hypot(x - 1, y - 14)) % 3 === 0) g.tint(x, y, TS[4], 0.35);
            return;
        }
        const inC = (x, y) => (x === 7 || x === 8) && y >= 2 && y <= 4 || cavity(x, y);
        for (let y = 2; y <= 12; y++) for (let x = 1; x <= 14; x++) {
            if (inC(x, y)) {
                let c = [40, 44, 54];
                if (!inC(x, y - 1) || !inC(x - 1, y)) c = [22, 24, 30];
                else if (!inC(x, y + 1) || !inC(x + 1, y)) c = [70, 76, 90];
                g.px(x, y, c);
            } else if (inC(x, y - 1) || inC(x - 1, y)) g.px(x, y, TS[4]);
        }
    }
    const rectIn = (x0, y0, x1, y1) => (x, y) => x >= x0 && x <= x1 && y >= y0 && y <= y1;
    const ring = (cx, cy, r0, r1) => (x, y) => { const d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy); return d >= r0 && d <= r1; };

    // ---- the icons

    const PVC = [238, 238, 232];
    const ICONS = {
        // PLA line
        lactide(g) {
            // a heap of white crystals with glints
            pile(g, 1, [232, 234, 236], [[[255, 255, 255], 0.12], [[190, 200, 210], 0.08]], { height: 7 });
            for (const [x, y] of [[5, 8], [9, 7], [11, 10]]) { g.px(x, y, [255, 255, 255]); g.px(x - 1, y, [214, 222, 230]); }
            outline(g);
        },
        pla_injection_pellet(g) { pelletHeap(g, [236, 228, 204]); outline(g); },
        pla_film_pellet(g) { pelletHeap(g, [206, 226, 214], 13, [226, 240, 232]); outline(g); },
        process_card(g) {
            poly(g, [[2, 3], [14, 3], [14, 13], [2, 13]], (x, y) => tone([52, 126, 168], 0.7 - 0.25 * (y - 3) / 10));
            g.rect(2, 5, 13, 6, [30, 34, 40]);
            g.rect(4, 8, 6, 10, [224, 184, 84]); g.px(4, 8, [250, 226, 150]); g.px(5, 9, [176, 136, 52]);
            g.rect(8, 8, 12, 8, [200, 226, 236]); g.rect(8, 10, 11, 10, [140, 186, 206]);
            g.px(13, 12, [30, 70, 96]);
            outline(g);
        },
        tin_catalyst(g) {
            bottle(g, [150, 100, 54], [236, 214, 120], 0.55, [40, 42, 46], g => {
                g.rect(5, 9, 10, 12, [236, 234, 224]); g.rect(6, 10, 9, 10, [120, 120, 130]); g.px(6, 11, [200, 60, 40]);
            });
            outline(g);
        },

        // coal chemistry
        coke(g) {
            const c = [96, 98, 104];
            lump(g, 5, 10, 4, 3.6, c, 11, { porous: 0.18 });
            lump(g, 10.5, 9.5, 4.2, 4, c, 12, { porous: 0.18 });
            lump(g, 8, 5.5, 3.6, 3, c, 13, { porous: 0.18 });
            outline(g);
        },
        coke_dust(g) { pile(g, 2, [74, 76, 82], [[[40, 40, 44], 0.15], [[130, 132, 140], 0.06]], { height: 6 }); outline(g, 0.55); },
        pitch(g) {
            const c = [36, 34, 38];
            lump(g, 6, 10, 5, 3.6, c, 21, { facets: 5, gloss: true, noise: 0.02 });
            lump(g, 10, 6, 4, 3.6, c, 22, { facets: 5, gloss: true, noise: 0.02 });
            outline(g, 0.4);
        },
        heavy_residue(g) {
            // a sticky puddle with a drip hanging off the lip
            const c = [44, 34, 28];
            poly(g, [[1, 12], [4, 9], [8, 8], [12, 9], [15, 12], [12, 14], [3, 14]], (x, y) => tone(c, 0.62 - 0.05 * (x - 8) + (y < 10 ? 0.15 : 0)));
            lump(g, 8, 7, 4, 3.4, c, 23, { facets: 9, gloss: true, noise: 0.02 });
            g.px(4, 10, light(c, 0.6)); g.px(9, 5, light(c, 0.7));
            outline(g, 0.4);
        },
        naphthalene(g) {
            const c = [232, 236, 240];
            flake(g, 5, 10, 4, 3, c, 1); flake(g, 10, 9, 4, 4, c, -1); flake(g, 7, 6, 3, 3, light(c, 0.2), 1); flake(g, 11, 4, 2, 2, c, 0);
            g.px(8, 4, [255, 255, 255]); g.px(10, 8, [255, 255, 255]);
            outline(g);
        },
        tar_placeholder() { },

        // carbide and lime
        calcium_carbonate(g) { pile(g, 3, [240, 240, 234], [[[214, 214, 206], 0.1]], { height: 7 }); outline(g, 0.55); },
        quicklime(g) {
            const c = [234, 230, 218];
            lump(g, 4.5, 11, 3.6, 3, c, 31, { noise: 0.1 });
            lump(g, 11, 11, 3.6, 3, c, 32, { noise: 0.1 });
            lump(g, 8, 7, 4, 3.6, c, 33, { noise: 0.1 });
            outline(g, 0.55);
        },
        calcium_carbide(g) {
            const c = [104, 104, 120];
            lump(g, 5, 10.5, 3.8, 3.4, c, 41, { facets: 5, sparkle: 0.08, sparkleColour: [150, 130, 190] });
            lump(g, 11, 10, 3.6, 3.6, c, 42, { facets: 5, sparkle: 0.08, sparkleColour: [130, 150, 200] });
            lump(g, 8, 5.5, 3.6, 3.2, c, 43, { facets: 5, sparkle: 0.08, sparkleColour: [150, 130, 190] });
            outline(g);
        },
        carbide_lump(g) {
            const c = [96, 96, 112];
            lump(g, 8, 8.5, 6.5, 5.5, c, 44, { facets: 6, sparkle: 0.07, sparkleColour: [156, 136, 196] });
            outline(g);
        },
        carbide_slag(g) {
            pile(g, 4, [182, 186, 184], [[[150, 154, 152], 0.15], [[214, 218, 216], 0.08]], { height: 7, lumpy: true });
            for (const [x, y] of [[5, 9], [9, 8], [11, 11]]) g.px(x, y, [236, 244, 246]);
            outline(g);
        },
        carbon_electrode(g) {
            // a graphite rod with a threaded nipple on the far end
            const ax = 3, ay = 13, bx = 12.5, by = 3.5;
            const len = Math.hypot(bx - ax, by - ay), ux = (bx - ax) / len, uy = (by - ay) / len;
            const pal = [[40, 42, 48], [64, 68, 76], [96, 100, 110], [140, 146, 158]];
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const px = x + 0.5 - ax, py = y + 0.5 - ay;
                const t = px * ux + py * uy, s = -px * uy + py * ux;
                const thread = t > len - 2.2;
                const r = thread ? 1.3 : 2.2;
                if (t < 0 || t > len || Math.abs(s) > r) continue;
                let k = s < -r * 0.4 ? 3 : s < 0.2 ? 2 : s < r * 0.6 ? 1 : 0;
                if (thread && Math.round(t * 1.4) % 2 === 0) k = Math.max(0, k - 1);
                g.px(x, y, pal[k]);
            }
            g.px(3, 12, pal[3]); g.px(4, 13, pal[2]);
            outline(g, 0.45);
        },

        // chlor-alkali
        rock_salt(g) {
            const pal = [[250, 240, 240], [226, 196, 200], [170, 132, 140]];
            cube(g, 2, 8, 5, pal); cube(g, 8, 9, 4, pal); cube(g, 6, 3, 4, pal); cube(g, 11, 4, 3, [[244, 236, 236], [214, 206, 206], [160, 150, 152]]);
            outline(g);
        },
        caustic_soda(g) {
            // flake caustic: thin translucent-white chips lying at angles, each with a lit top edge
            const chips = [[1, 11, 5, 2], [7, 12, 5, 2], [12, 10, 3, 2], [3, 7, 4, 2], [9, 7, 4, 2], [6, 3, 4, 2]];
            chips.forEach(([x, y, w, h], i) => {
                const c = i % 2 ? [236, 240, 244] : [222, 230, 236];
                g.rect(x, y, x + w - 1, y + h - 1, c);
                g.rect(x, y, x + w - 2, y, [255, 255, 255]);
                g.px(x + w - 1, y + h - 1, [176, 190, 200]);
            });
            outline(g, 0.6);
        },

        // VCM and PVC
        mercury_free_catalyst(g) {
            pelletHeap(g, [54, 56, 62]);
            for (const [x, y] of [[2, 11], [7, 9], [9, 12], [6, 5], [12, 11]]) g.px(x, y, [236, 196, 80]);
            outline(g, 0.45);
        },
        initiator(g) {
            bottle(g, [230, 230, 226], null, 0, [196, 52, 40], g => {
                // an organic peroxide diamond
                poly(g, [[7.5, 8], [10.5, 11], [7.5, 14], [4.5, 11]], [234, 120, 30]);
                g.px(7, 11, [40, 30, 20]); g.px(8, 10, [40, 30, 20]);
            });
            outline(g);
        },
        dispersant(g) {
            sack(g, [226, 220, 204], [52, 110, 176], g => { g.px(7, 8, [52, 110, 176]); g.px(8, 8, [52, 110, 176]); });
            outline(g);
        },
        pvc_resin(g) { pile(g, 5, PVC, [[[255, 255, 255], 0.12]], { height: 8, grain: 0.06 }); outline(g, 0.55); },
        wet_pvc(g) {
            pile(g, 6, [226, 230, 232], [[[200, 214, 222], 0.12]], { height: 7, lumpy: true });
            for (const [x, y] of [[4, 10], [8, 7], [10, 9], [6, 12]]) { g.px(x, y, [170, 206, 232]); g.px(x, y - 1, [240, 250, 255]); }
            outline(g, 0.55);
        },
        heat_stabilizer(g) {
            can(g, [150, 156, 166], [232, 196, 52], g => { g.rect(6, 9, 9, 9, [40, 40, 44]); g.px(5, 8, [40, 40, 44]); });
            outline(g);
        },
        plasticizer(g) {
            bottle(g, [200, 214, 220], [240, 226, 140], 0.75, [64, 132, 74], g => {
                g.rect(5, 10, 10, 12, [236, 236, 228]); g.rect(6, 11, 9, 11, [64, 132, 74]);
            });
            outline(g);
        },
        rigid_pvc_dry_blend(g) {
            pile(g, 7, [232, 228, 214], [[[150, 154, 160], 0.06], [[214, 150, 70], 0.04], [[250, 250, 246], 0.08]], { height: 8 });
            outline(g, 0.55);
        },
        soft_pvc_dry_blend(g) {
            pile(g, 8, [232, 220, 176], [[[206, 190, 140], 0.14], [[246, 238, 206], 0.08]], { height: 7, lumpy: true });
            outline(g, 0.55);
        },

        // additives and by-products
        pe_wax(g) {
            const c = [240, 236, 220];
            const beads = [[2, 11], [5, 12], [9, 12], [12, 11], [4, 9], [7, 9], [10, 9], [6, 6], [9, 6], [7, 3]];
            for (const [x, y] of beads) {
                g.rect(x, y, x + 1, y + 1, c); g.px(x, y, [255, 255, 255]); g.px(x + 1, y + 1, dark(c, 0.25));
            }
            outline(g, 0.55);
        },
        sulfur(g) {
            const pal = [[255, 244, 140], [232, 200, 40], [176, 136, 20]];
            prism(g, 2, 14, 7, pal); prism(g, 5, 14, 10, pal); prism(g, 8, 14, 8, pal); prism(g, 11, 14, 6, pal);
            prism(g, 7, 14, 4, [[255, 236, 120], [218, 184, 36], [160, 120, 16]]);
            outline(g);
        },
        activated_carbon(g) {
            // extruded carbon granules, matte black with a bluish sheen on the cut ends
            pelletHeap(g, [58, 60, 66], 13, [72, 76, 86]);
            for (const [x, y] of [[3, 11], [10, 12], [8, 9], [12, 7]]) g.px(x, y, [30, 32, 36]);
            outline(g, 0.4);
        },

        // injection moulding: moulds
        mold_blank(g) { mold(g, null, 50); },
        brick_mold(g) {
            const cav = rectIn(4, 5, 11, 10);
            mold(g, (x, y) => cav(x, y) && !([5, 9].includes(x) && [6, 8].includes(y)), 51);
        },
        crate_mold(g) {
            const outer = rectIn(3, 5, 12, 11), inner = rectIn(5, 6, 10, 10);
            mold(g, (x, y) => outer(x, y) && !inner(x, y) || (x === 7 || x === 8) && y === 8, 52);
        },
        emitter_mold(g) {
            mold(g, (x, y) => Math.hypot(x + 0.5 - 8, y + 0.5 - 8) <= 2.6 || (x === 8 && y >= 10 && y <= 11), 53);
        },
        fitting_mold(g) { mold(g, (x, y) => rectIn(3, 8, 10, 10)(x, y) || rectIn(8, 5, 10, 10)(x, y), 54); },
        housing_mold(g) {
            const outer = rectIn(3, 5, 12, 11), inner = rectIn(4, 6, 11, 10);
            mold(g, (x, y) => outer(x, y) && !inner(x, y) || (y === 8 && x >= 6 && x <= 9), 55);
        },
        lamp_shade_mold(g) {
            mold(g, (x, y) => y >= 5 && y <= 10 && Math.abs(x + 0.5 - 8) <= 1 + (y - 5) * 0.8, 56);
        },
        pail_lid_mold(g) { mold(g, (x, y) => ring(8, 8, 2.2, 4.4)(x, y) || (x === 7 || x === 8) && y >= 4 && y <= 5, 57); },
        pail_mold(g) {
            const hw = y => 4.6 - (y - 5) * 0.35;
            mold(g, (x, y) => {
                if (y < 5 || y > 11) return false;
                const d = Math.abs(x + 0.5 - 8);
                return d <= hw(y) && (d >= hw(y) - 1.2 || y >= 10);
            }, 58);
        },

        // injection moulding: parts
        sprue(g) {
            const c = [226, 212, 164];
            const tone2 = (x, y) => tone(c, 0.7 - 0.25 * ((x % 2) ? 1 : 0));
            poly(g, [[6, 1], [10, 1], [9, 11], [7, 11]], tone2);
            g.rect(2, 9, 13, 10, c); g.rect(2, 9, 13, 9, light(c, 0.25));
            for (const x of [2, 12]) { g.rect(x, 7, x + 1, 12, c); g.rect(x, 7, x, 12, light(c, 0.25)); }
            g.rect(7, 12, 8, 13, dark(c, 0.15)); g.px(7, 12, c);
            outline(g);
        },
        drip_emitter(g) {
            // an inline emitter: a black body with a purple labyrinth strip and barbed ends,
            // with a water drop leaving its outlet
            const c = [50, 52, 58];
            for (let x = 3; x <= 12; x++) for (let y = 5; y <= 10; y++) g.px(x, y, tone(c, 0.95 - (y - 5) * 0.14));
            for (const x of [1, 13]) for (let y = 6; y <= 9; y++) {
                g.px(x, y, tone(c, 0.9 - (y - 6) * 0.2)); g.px(x + 1, y, tone(c, 0.8 - (y - 6) * 0.2));
            }
            g.rect(0, 7, 0, 8, dark(c, 0.2)); g.rect(15, 7, 15, 8, dark(c, 0.2));
            // labyrinth flow path
            const purple = [168, 64, 186];
            for (let x = 4; x <= 11; x++) g.px(x, 6, x % 3 === 0 ? light(purple, 0.3) : purple);
            for (const x of [4, 7, 10]) g.px(x, 7, purple);
            g.rect(7, 9, 8, 9, [20, 20, 24]);
            g.px(8, 11, [150, 206, 240]); g.px(8, 12, [110, 176, 226]); g.px(7, 12, [190, 230, 250]); g.px(8, 13, [80, 140, 200]);
            outline(g, 0.45);
        },
        lamp_shade(g) {
            const c = [238, 236, 226];
            g.rect(6, 1, 9, 2, [150, 156, 166]); g.rect(6, 1, 9, 1, [196, 200, 208]);
            for (let y = 3; y <= 12; y++) {
                const hw = 1.6 + (y - 3) * 0.66;
                for (let x = 0; x < 16; x++) {
                    const dx = (x + 0.5 - 8) / hw;
                    if (Math.abs(dx) <= 1) g.px(x, y, tone(c, 0.7 - 0.4 * dx));
                }
            }
            g.rect(2, 12, 13, 12, light(c, 0.3)); g.rect(3, 13, 12, 13, [170, 160, 130]); g.rect(5, 13, 10, 13, [250, 226, 150]);
            outline(g);
        },
        machine_housing(g) {
            const c = [178, 182, 186];
            poly(g, [[2, 5], [5, 2], [15, 2], [12, 5]], light(c, 0.25));
            poly(g, [[2, 5], [12, 5], [12, 15], [2, 15]], c);
            poly(g, [[12, 5], [15, 2], [15, 12], [12, 15]], dark(c, 0.3));
            for (const y of [7, 9, 11]) g.rect(4, y, 9, y, dark(c, 0.55));
            for (const [x, y] of [[3, 6], [10, 6], [3, 13], [10, 13]]) g.px(x, y, dark(c, 0.4));
            g.rect(13, 6, 13, 9, dark(c, 0.5));
            outline(g);
        },
        pipe_fitting(g) {
            const c = [150, 154, 160];
            // horizontal arm, shaded by row
            for (let y = 8; y <= 12; y++) for (let x = 1; x <= 8; x++) g.px(x, y, tone(c, 0.9 - (y - 8) * 0.17));
            // vertical arm, shaded by column
            for (let x = 8; x <= 12; x++) for (let y = 1; y <= 8; y++) g.px(x, y, tone(c, 0.9 - (x - 8) * 0.17));
            for (let y = 8; y <= 12; y++) for (let x = 8; x <= 12; x++) {
                if (Math.hypot(x - 8, y - 8) > 4.6) continue;
                g.px(x, y, tone(c, 0.9 - Math.hypot(x - 8, y - 8) * 0.17));
            }
            // sockets
            for (let y = 7; y <= 13; y++) for (let x = 1; x <= 2; x++) g.px(x, y, tone(light(c, 0.1), 0.9 - (y - 7) * 0.14));
            for (let x = 7; x <= 13; x++) for (let y = 1; y <= 2; y++) g.px(x, y, tone(light(c, 0.1), 0.9 - (x - 7) * 0.14));
            outline(g);
        },
        pail_body(g) { pail(g, false, false); outline(g); },
        pail_lid(g) {
            const c = [52, 116, 190];
            for (let y = 4; y <= 12; y++) for (let x = 0; x < 16; x++) {
                const dx = (x + 0.5 - 8) / 7, dy = (y + 0.5 - 8) / 3.6;
                const d = Math.hypot(dx, dy);
                if (d > 1) continue;
                let t = 0.7 - 0.25 * dx - 0.2 * dy;
                if (d > 0.82) t += 0.2;
                if (Math.abs(d - 0.5) < 0.09) t -= 0.3;
                g.px(x, y, tone(c, t));
            }
            g.rect(2, 11, 13, 11, dark(c, 0.35)); g.rect(4, 12, 11, 12, dark(c, 0.45));
            outline(g);
        },
        plastic_pail(g) { pail(g, true, false); outline(g); bail(g); },
        tool_steel_ingot(g) {
            const pal = [[64, 72, 88], [102, 112, 130], [148, 158, 176], [200, 208, 222]];
            poly(g, [[2, 9], [9, 5], [14, 7.5], [7, 11.5]], pal[2]);
            poly(g, [[2, 9], [7, 11.5], [7, 13.5], [2, 11]], pal[1]);
            poly(g, [[7, 11.5], [14, 7.5], [14, 9.5], [7, 13.5]], pal[0]);
            line(g, 3, 9, 8, 6.5, pal[3]);
            g.px(8, 8, pal[1]); g.px(9, 8, pal[1]);
            outline(g, 0.5);
        },
    };
    delete ICONS.tar_placeholder;

    function pail(g, lid, handle) {
        const c = [52, 116, 190];
        for (let y = 6; y <= 14; y++) {
            const hw = 4.6 - (y - 6) * 0.12;
            for (let x = 0; x < 16; x++) {
                const dx = (x + 0.5 - 8) / hw;
                if (Math.abs(dx) <= 1) g.px(x, y, tone(c, 0.72 - 0.38 * dx - (y === 14 ? 0.2 : 0)));
            }
        }
        for (const y of [8, 12]) for (let x = 3; x <= 12; x++) if (opaque(g, x, y)) g.px(x, y, dark(g.get(x, y), 0.18));
        // the rim seen slightly from above
        for (let y = 3; y <= 6; y++) for (let x = 0; x < 16; x++) {
            const dx = (x + 0.5 - 8) / 5.6, dy = (y + 0.5 - 4.5) / 1.8;
            const d = Math.hypot(dx, dy);
            if (d > 1) continue;
            const inner = d < 0.7;
            g.px(x, y, lid ? tone(c, inner ? 0.8 : 0.95 - 0.3 * dx) : inner ? dark(c, 0.62) : tone(c, 0.95 - 0.3 * dx));
        }
        if (handle) bail(g);
    }

    function bail(g) {
        {
            // a wire bail arching over the lid, with a black plastic grip
            const arc = [[2, 5], [2, 4], [3, 3], [3, 2], [4, 1], [5, 1], [6, 0], [9, 0], [10, 1], [11, 1], [12, 2], [12, 3], [13, 4], [13, 5]];
            for (const [x, y] of arc) g.px(x, y, x < 8 ? [206, 210, 216] : [140, 146, 154]);
            g.rect(6, 0, 9, 1, [36, 38, 42]); g.rect(6, 0, 9, 0, [70, 72, 78]);
        }
    }

    const MODEL_DIR = {
        lactide: null, pla_film_pellet: null, pla_injection_pellet: null, process_card: null, tin_catalyst: null,
        brick_mold: 'm3', crate_mold: 'm3', drip_emitter: 'm3', emitter_mold: 'm3', fitting_mold: 'm3', housing_mold: 'm3',
        lamp_shade: 'm3', lamp_shade_mold: 'm3', machine_housing: 'm3', mold_blank: 'm3', pail_body: 'm3', pail_lid: 'm3',
        pail_lid_mold: 'm3', pail_mold: 'm3', pipe_fitting: 'm3', plastic_pail: 'm3', sprue: 'm3', tool_steel_ingot: 'm3',
    };

    function all() {
        const out = [];
        for (const [name, draw] of Object.entries(ICONS)) {
            const img = A.paint(draw);
            A.save(img, 'item/component/' + name + '.png');
            const dir = name in MODEL_DIR ? MODEL_DIR[name] : 'm4';
            if (dir) A.syncModel('tools/models/' + dir + '/' + name + '.bbmodel', name, img);
            out.push(name);
        }
        return out.length;
    }

    window.IUCEItems = { ICONS, all, outline, pile, lump };
    return 'items loaded';
})();
