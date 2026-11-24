// GUI parts of the process machine screens: the bolted steel panel, recessed slots, glass tanks, the
// energy gauge, the side tabs and their icons, buttons, the dark display behind the text pages, the
// faint slot hints and one progress sheet per progress style. Same steel greys, bevels and bolts as
// the machine casings (core.js), so the screens read as part of the machines.
// Run inside Blockbench after core.js:
//   eval(fs.readFileSync('D:/Code/java/IUCE/tools/art/blockbench/gui.js', 'utf8')); IUCEGui.paintAll()
//
// Progress sheets are rows of one frame each. Styles that fill (right or up) hold the empty row, the
// full row and then the moving overlay frames; phase styles hold one row per stage of the run. The
// Java side (machine.process.ProgressStyle) must list the same sizes and frame counts.
(function () {
    const A = window.IUCEArt;
    const { fs, TEX, rng, mix, light, dark, clamp, STEEL, FRAME, GLASS, HOT, COPPER } = A;
    const PANEL = [140, 146, 154];
    const IN = [72, 150, 206], OUT = [222, 138, 48];
    const SCREEN = [16, 28, 24];
    const WATER = [56, 116, 180];
    const SHADOW = [36, 39, 44], LIP = [205, 209, 214];
    const SLOT = [62, 67, 75];

    // An image of any size (core.js's Img is fixed at 16 wide).
    class G {
        constructor(w, h) { this.w = w; this.h = h; this.d = new Uint8ClampedArray(w * h * 4); }
        px(x, y, c, a) {
            x = Math.round(x); y = Math.round(y);
            if (!c || x < 0 || x >= this.w || y < 0 || y >= this.h) return;
            const i = (y * this.w + x) * 4;
            const alpha = a === undefined ? (c[3] === undefined ? 255 : c[3]) : a;
            this.d[i] = clamp(c[0]); this.d[i + 1] = clamp(c[1]); this.d[i + 2] = clamp(c[2]); this.d[i + 3] = clamp(alpha);
        }
        get(x, y) {
            if (x < 0 || x >= this.w || y < 0 || y >= this.h) return null;
            const i = (y * this.w + x) * 4;
            return [this.d[i], this.d[i + 1], this.d[i + 2], this.d[i + 3]];
        }
        clear(x, y) {
            if (x < 0 || x >= this.w || y < 0 || y >= this.h) return;
            const i = (y * this.w + x) * 4;
            this.d.fill(0, i, i + 4);
        }
        rect(x0, y0, x1, y1, c, a) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) this.px(x, y, c, a); }
        bevel(x0, y0, x1, y1, l, d) {
            this.rect(x0, y0, x1, y0, l); this.rect(x0, y0, x0, y1, l);
            this.rect(x0, y1, x1, y1, d); this.rect(x1, y0, x1, y1, d);
        }
        // Blends c over the pixel with opacity t; on a clear pixel it becomes c at that opacity.
        over(x, y, c, t) {
            const p = this.get(x, y);
            if (!p) return;
            if (!p[3]) this.px(x, y, c, 255 * t);
            else this.px(x, y, mix(p, c, t), p[3]);
        }
        // The faint grain of the machine casings.
        grain(x0, y0, x1, y1, base, seed, spread) {
            const r = rng(seed), s = spread === undefined ? 0.06 : spread;
            for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) this.px(x, y, mix(dark(base, s), light(base, s), r()));
        }
        draw(src, dx, dy) {
            for (let y = 0; y < src.h; y++) for (let x = 0; x < src.w; x++) {
                const p = src.get(x, y);
                if (p[3]) this.px(dx + x, dy + y, p, p[3]);
            }
        }
        dataURL() {
            const c = document.createElement('canvas'); c.width = this.w; c.height = this.h;
            const ctx = c.getContext('2d'); const id = ctx.createImageData(this.w, this.h);
            id.data.set(this.d); ctx.putImageData(id, 0, 0);
            return c.toDataURL('image/png');
        }
    }

    function save(g, rel, meta) {
        const path = TEX + rel, dir = path.substring(0, path.lastIndexOf('/'));
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        fs.writeFileSync(path, Buffer.from(g.dataURL().split(',')[1], 'base64'));
        if (meta) fs.writeFileSync(path + '.mcmeta', JSON.stringify(meta, null, 2) + '\n');
        else if (fs.existsSync(path + '.mcmeta')) fs.unlinkSync(path + '.mcmeta');
        return rel;
    }
    const nine = (w, h, border) => ({ gui: { scaling: { type: 'nine_slice', width: w, height: h, border: border } } });

    // Paints the dark outline (inside pixels next to the outside) and the lit / shaded ring inside it.
    function frame(g, inside, o) {
        const edge = (x, y) => inside(x, y) && (!inside(x - 1, y) || !inside(x + 1, y) || !inside(x, y - 1) || !inside(x, y + 1));
        for (let y = 0; y < g.h; y++) for (let x = 0; x < g.w; x++) {
            if (!inside(x, y)) { g.clear(x, y); continue; }
            if (edge(x, y)) { g.px(x, y, o.outline); continue; }
            const tl = edge(x - 1, y) || edge(x, y - 1) || edge(x - 1, y - 1);
            const br = edge(x + 1, y) || edge(x, y + 1) || edge(x + 1, y + 1);
            if (tl && br) g.px(x, y, mix(o.lit, o.shade, 0.5));
            else if (tl) g.px(x, y, o.lit);
            else if (br) g.px(x, y, o.shade);
        }
    }
    // A rectangle with its corners cut off; openRight keeps the right side open so it joins the panel.
    const chamfered = (w, h, c, openRight) => (x, y) => {
        if (y < 0 || y >= h || x < 0) return false;
        if (x >= w) return !!openRight;
        const rx = openRight ? 99 : w - 1 - x, by = h - 1 - y;
        return !(x + y < c || rx + y < c || x + by < c || rx + by < c);
    };
    // A raised steel plate: grain, chamfered outline, lit top-left ring, optional bolts.
    function plate(w, h, base, seed, o) {
        o = o || {};
        const g = new G(w, h);
        g.grain(0, 0, w - 1, h - 1, base, seed, o.grain);
        const lit = o.pressed ? dark(base, 0.35) : light(base, 0.45);
        const shade = o.pressed ? light(base, 0.3) : dark(base, 0.3);
        frame(g, chamfered(w, h, o.chamfer || 0, o.openRight), { outline: o.outline || FRAME[0], lit: lit, shade: shade });
        for (const [x, y] of o.bolts || []) A.bolt(g, x, y);
        return g;
    }
    // A hole sunk into the panel: shadowed top-left edge, lit lower-right lip, inner shadow line.
    function recessed(w, h, fill, seed, o) {
        o = o || {};
        const g = new G(w, h);
        g.grain(1, 1, w - 2, h - 2, fill, seed, o.grain === undefined ? 0.04 : o.grain);
        g.rect(0, 0, w - 1, 0, SHADOW); g.rect(0, 0, 0, h - 1, SHADOW);
        g.rect(0, h - 1, w - 1, h - 1, LIP); g.rect(w - 1, 0, w - 1, h - 1, LIP);
        g.px(w - 1, 0, PANEL); g.px(0, h - 1, PANEL);
        g.rect(1, 1, w - 2, 1, dark(fill, 0.22)); g.rect(1, 1, 1, h - 2, dark(fill, 0.22));
        return g;
    }
    // Pixel art: '#' cells painted with color(x, y).
    function pattern(g, rows, x0, y0, color) {
        rows.forEach((row, y) => [...row].forEach((ch, x) => { if (ch === '#') g.px(x0 + x, y0 + y, color(x, y)); }));
    }
    // A one-pixel outline around everything opaque.
    function outline(g, c) {
        const hits = [];
        for (let y = 0; y < g.h; y++) for (let x = 0; x < g.w; x++) {
            if (g.get(x, y)[3]) continue;
            if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const p = g.get(x + dx, y + dy); return p && p[3] > 128; })) hits.push([x, y]);
        }
        for (const [x, y] of hits) g.px(x, y, c);
    }
    // A shape filled with fill(x, y), shadowed where it meets the outside at the top or left, lit at the bottom or right.
    function shaped(w, h, inside, fill, top, bottom) {
        const g = new G(w, h);
        for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
            if (!inside(x, y)) continue;
            if (top && (!inside(x, y - 1) || !inside(x - 1, y))) g.px(x, y, top);
            else if (bottom && (!inside(x, y + 1) || !inside(x + 1, y))) g.px(x, y, bottom);
            else g.px(x, y, fill(x, y));
        }
        return g;
    }
    function sheet(rows) {
        const w = rows[0].w, h = rows[0].h, g = new G(w, h * rows.length);
        rows.forEach((r, i) => g.draw(r, 0, i * h));
        return g;
    }

    const CHEVRONS = [
        '......##......', '.....####.....', '....######....', '...###..###...', '..###....###..', '.###......###.',
        '......##......', '.....####.....', '....######....', '...###..###...', '..###....###..', '.###......###.',
    ];
    const LIGHTNING = [
        '.......####.', '......####..', '.....####...', '....####....', '...########.', '..########..', '......###...',
        '.....###....', '....###.....', '...###......', '..###.......', '.##.........', '.#..........', '#...........',
    ];

    // ---- the parts: [image, mcmeta or null]
    function tank(accent) {
        const g = recessed(18, 54, GLASS, accent[0], { grain: 0.03 });
        for (let y = 2; y < 53; y++) for (let x = 2; x < 17; x++) g.over(x, y, [70, 90, 104], 0.14 * Math.sin(Math.PI * (x - 1.5) / 15));
        g.rect(1, 0, 16, 0, dark(accent, 0.25));
        g.rect(1, 1, 16, 1, dark(accent, 0.55));
        return g;
    }
    function glass() {
        const g = new G(16, 52);
        for (let y = 1; y < 51; y++) { g.px(2, y, [255, 255, 255], 54); g.px(3, y, [255, 255, 255], 24); }
        for (const m of [13, 26, 39]) {
            const len = m === 26 ? 5 : 3;
            for (let x = 16 - len; x < 16; x++) { g.px(x, m, [214, 222, 226], 170); g.px(x, m + 1, [0, 0, 0], 80); }
        }
        for (let x = 1; x < 15; x++) g.px(x, 0, [255, 255, 255], 30);
        return g;
    }
    function energyBar() {
        const g = recessed(9, 54, [36, 31, 22], 18, { grain: 0.03 });
        for (let y = 4; y < 53; y += 4) g.rect(2, y, 7, y, [22, 19, 14]);
        return g;
    }
    function energyFill() {
        const g = new G(7, 52);
        for (let x = 0; x < 7; x++) {
            const t = 0.25 + 0.7 * Math.sin(Math.PI * (x + 0.5) / 7) - 0.12 * x / 6;
            const c = mix([150, 98, 18], [255, 228, 104], t);
            for (let y = 0; y < 52; y++) g.px(x, y, y % 4 === 3 ? dark(c, 0.3) : c);
        }
        g.rect(0, 0, 6, 0, [255, 244, 190]);
        return g;
    }
    function screen() {
        const g = recessed(16, 16, SCREEN, 17, { grain: 0.05 });
        for (let y = 2; y < 15; y += 2) for (let x = 2; x < 15; x++) g.over(x, y, [0, 0, 0], 0.18);
        return g;
    }

    const ICONS = {
        process: () => {
            const g = new G(16, 16);
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                const dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
                const tooth = ((a / (Math.PI * 2) * 8 + 8.25) % 1) < 0.5;
                if (d <= (tooth ? 6.6 : 5.0) && d >= 2.0) g.px(x, y, mix(STEEL[1], STEEL[4], 0.5 - (dx + dy) / 18));
            }
            outline(g, FRAME[0]);
            return g;
        },
        mold: () => {
            const g = new G(16, 16);
            g.rect(2, 4, 13, 13, STEEL[3]); g.bevel(2, 4, 13, 13, STEEL[4], STEEL[1]);
            g.rect(5, 4, 10, 10, [30, 32, 36]); g.rect(5, 8, 10, 8, HOT[1]); g.rect(5, 9, 10, 10, HOT[2]);
            g.rect(4, 4, 4, 11, FRAME[1]); g.rect(11, 4, 11, 11, STEEL[4]); g.rect(4, 11, 11, 11, STEEL[4]);
            outline(g, FRAME[0]);
            return g;
        },
        upgrades: () => {
            const g = new G(16, 16);
            pattern(g, CHEVRONS, 1, 2, (x, y) => mix([56, 160, 76], [168, 244, 156], 1 - (y % 6) / 5));
            outline(g, [20, 50, 26]);
            return g;
        },
        info: () => {
            const g = new G(16, 16), W = [250, 250, 250];
            for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
                if (Math.hypot(x - 7.5, y - 7.5) < 6.8) g.px(x, y, mix([40, 84, 160], [110, 160, 230], 0.5 - (x + y - 15) / 16));
            }
            outline(g, [16, 30, 60]);
            g.rect(7, 3, 8, 4, W); g.rect(7, 6, 8, 11, W); g.px(6, 6, W); g.rect(6, 11, 9, 11, W);
            return g;
        },
        recipes: () => {
            const g = new G(16, 16);
            g.rect(3, 2, 12, 14, [226, 220, 198]); g.rect(12, 3, 12, 14, [190, 182, 160]); g.rect(3, 14, 12, 14, [190, 182, 160]);
            for (const y of [6, 8, 10, 12]) g.rect(5, y, y === 12 ? 8 : 10, y, [120, 116, 104]);
            outline(g, [70, 56, 40]);
            g.rect(6, 1, 9, 3, STEEL[3]); g.rect(6, 3, 9, 3, STEEL[1]); g.px(6, 1, STEEL[4]);
            return g;
        },
        energy: () => {
            const g = new G(16, 16);
            pattern(g, LIGHTNING, 2, 1, (x, y) => mix([255, 236, 120], [214, 150, 30], y / 13));
            outline(g, [70, 44, 10]);
            return g;
        },
    };

    // Faint light silhouettes shown in empty special slots.
    function ghost(draw) {
        const g = new G(16, 16);
        draw((x, y) => g.px(x, y, [255, 255, 255], 46));
        return g;
    }
    const GHOSTS = {
        catalyst: () => ghost(p => {
            for (let y = 4; y <= 13; y++) for (let x = 2; x <= 13; x++) if (!(x >= 5 && x <= 10 && y <= 10)) p(x, y);
        }),
        card: () => ghost(p => {
            for (let y = 3; y <= 12; y++) for (let x = 2; x <= 13; x++) {
                if (x + y < 7) continue;
                if ((y === 6 || y === 9) && x >= 5 && x <= 11 && x % 2 === 1) continue;
                p(x, y);
            }
        }),
        upgrade: () => ghost(p => pattern({ px: (x, y) => p(x, y) }, CHEVRONS, 1, 2, () => 0)),
    };

    const PARTS = {
        'sprites/process/panel': () => [plate(32, 32, PANEL, 11, { chamfer: 3, bolts: [[4, 4], [26, 4], [4, 26], [26, 26]] }), nine(32, 32, 7)],
        'sprites/process/tab': () => [plate(22, 22, dark(PANEL, 0.18), 12, { chamfer: 3, openRight: true }), null],
        'sprites/process/tab_on': () => [plate(25, 22, PANEL, 13, { chamfer: 3, openRight: true }), null],
        'sprites/process/button': () => [plate(20, 20, light(PANEL, 0.1), 14, { chamfer: 1 }), nine(20, 20, 3)],
        'sprites/process/button_on': () => [plate(20, 20, [108, 140, 116], 15, { chamfer: 1, pressed: true }), nine(20, 20, 3)],
        'sprites/process/screen': () => [screen(), nine(16, 16, 3)],
        'process/slot': () => [recessed(18, 18, SLOT, 16), null],
        'process/tank_in': () => [tank(IN), null],
        'process/tank_out': () => [tank(OUT), null],
        'process/tank_glass': () => [glass(), null],
        'process/energy_bar': () => [energyBar(), null],
        'process/energy_fill': () => [energyFill(), null],
    };
    for (const name in ICONS) PARTS['process/icon_' + name] = () => [ICONS[name](), null];
    for (const name in GHOSTS) PARTS['process/ghost_' + name] = () => [GHOSTS[name](), null];

    // ---- progress sheets
    const arrowIn = (x, y) => x >= 0 && x < 22 && y >= 0 && y < 16 && (x < 15 ? y >= 6 && y <= 9 : Math.abs(y - 7.5) < 7 - (x - 15));
    const arrowInner = (x, y) => arrowIn(x, y) && arrowIn(x - 1, y) && arrowIn(x + 1, y) && arrowIn(x, y - 1) && arrowIn(x, y + 1);

    // Single screw: a cut-away barrel with a feed hopper, heater bands and the screw turning inside.
    function screwBg() {
        const g = new G(44, 16), HOLE = [24, 26, 30];
        for (const x0 of [0, 41]) {
            g.rect(x0, 1, x0 + 2, 14, STEEL[2]); g.bevel(x0, 1, x0 + 2, 14, STEEL[4], FRAME[0]);
            g.px(x0 + 1, 3, FRAME[0]); g.px(x0 + 1, 12, FRAME[0]);
        }
        g.rect(3, 3, 40, 3, FRAME[0]); g.rect(3, 4, 40, 4, STEEL[3]);
        g.grain(3, 5, 40, 10, [30, 32, 36], 21, 0.05);
        g.rect(3, 11, 40, 11, STEEL[1]); g.rect(3, 12, 40, 12, FRAME[0]);
        for (const x0 of [15, 25, 34]) for (let x = x0; x < x0 + 4; x++) {
            g.px(x, 3, COPPER[0]); g.px(x, 4, x === x0 ? COPPER[3] : COPPER[2]); g.px(x, 11, COPPER[1]); g.px(x, 12, COPPER[0]);
        }
        for (let y = 0; y < 3; y++) {
            const l = 5 + y, r = 12 - y;
            g.px(l, y, STEEL[3]); g.px(r, y, STEEL[1]);
            for (let x = l + 1; x < r; x++) g.px(x, y, HOLE);
        }
        g.rect(7, 3, 10, 4, HOLE);
        g.rect(3, 7, 40, 7, STEEL[3]); g.rect(3, 8, 40, 8, STEEL[1]);
        g.rect(42, 7, 43, 8, HOLE);
        return g;
    }
    function screwFill() {
        const g = new G(44, 16), r = rng(22);
        for (let y = 5; y <= 10; y++) for (let x = 3; x <= 40; x++) {
            if (y === 7 || y === 8) continue;
            if (x < 14) { if (r() < 0.45) g.px(x, y, [226, 226, 214], 235); }
            else g.px(x, y, mix(mix(HOT[3], HOT[2], (x - 14) / 26), HOT[1], r() * 0.35), 225);
        }
        g.rect(42, 7, 43, 8, HOT[1]);
        return g;
    }
    function screwFlight(f) {
        const g = new G(44, 16);
        for (let y = 5; y <= 10; y++) for (let x = 3; x <= 40; x++) {
            const k = (((x - f + Math.floor((y - 5) * 0.67)) % 6) + 6) % 6;
            if (k === 0) g.px(x, y, STEEL[4]); else if (k === 1) g.px(x, y, STEEL[2]);
        }
        return g;
    }

    // Cooling tank: a water trough; the hot strand pulled through it cools from orange to grey.
    function waveBg() {
        const g = new G(22, 16);
        g.rect(0, 3, 1, 15, STEEL[2]); g.rect(20, 3, 21, 15, STEEL[2]); g.rect(0, 14, 21, 15, STEEL[2]);
        g.rect(0, 3, 0, 15, STEEL[3]); g.rect(1, 3, 1, 13, STEEL[1]); g.rect(20, 3, 20, 13, STEEL[3]); g.rect(21, 3, 21, 15, FRAME[1]);
        g.rect(0, 15, 21, 15, FRAME[0]); g.rect(2, 14, 19, 14, STEEL[3]);
        for (const x of [0, 1, 20, 21]) g.px(x, 3, STEEL[4]);
        for (let y = 7; y <= 13; y++) for (let x = 2; x <= 19; x++) g.px(x, y, mix(WATER, dark(WATER, 0.55), (y - 7) / 7));
        return g;
    }
    function waveFill() {
        const g = new G(22, 16);
        for (let x = 0; x < 22; x++) {
            const c = mix(HOT[2], [150, 156, 162], x / 21);
            g.px(x, 9, light(c, 0.2)); g.px(x, 10, dark(c, 0.15));
        }
        return g;
    }
    function waveAnim(f) {
        const g = new G(22, 16), r = rng(30);
        for (let x = 2; x <= 19; x++) {
            const k = (((x - f * 2) % 8) + 8) % 8;
            if (k < 3) g.px(x, 7, light(WATER, 0.5));
            if (k === 1) g.px(x, 6, light(WATER, 0.25), 210);
        }
        for (let i = 0; i < 4; i++) {
            const bx = 3 + Math.floor(r() * 16), by = 13 - (Math.floor(r() * 6) + f) % 6;
            if (by > 7 && by !== 9 && by !== 10) g.px(bx, by, light(WATER, 0.45));
        }
        return g;
    }

    // Vulcanizer: the heated top platen comes down on the rubber sheet and both platens glow.
    function press(k) {
        const g = new G(22, 16), t = k / 7;
        for (const x of [0, 21]) { g.rect(x, 0, x, 15, STEEL[2]); g.px(x, 0, STEEL[4]); }
        g.rect(1, 11, 20, 14, STEEL[2]); g.bevel(1, 11, 20, 14, STEEL[4], FRAME[0]);
        const glow = mix([86, 42, 30], HOT[2], t);
        g.rect(3, 13, 18, 13, glow);
        const th = t > 0.8 ? 1 : 2;
        g.rect(4, 11 - th, 17, 10, [44, 40, 38]); g.rect(4, 11 - th, 17, 11 - th, [70, 64, 60]);
        const bottom = Math.min(3 + Math.round(t * 6), 10 - th);
        g.rect(10, 0, 11, bottom - 4, STEEL[3]);
        g.rect(1, bottom - 3, 20, bottom, STEEL[2]); g.bevel(1, bottom - 3, 20, bottom, STEEL[4], FRAME[0]);
        g.rect(3, bottom - 1, 18, bottom - 1, glow);
        if (t > 0.4) for (const x of [6, 11, 16]) g.px(x + (k % 2), bottom - 4, HOT[1], 150 * t);
        return g;
    }

    // Smokehouse: sausages on a rail over embers; smoke fills the room from the floor and they darken.
    const SAUSAGE = [5, 10, 15];
    function sausages(g, c) {
        for (const x of SAUSAGE) {
            g.rect(x - 1, 4, x + 1, 9, c); g.rect(x - 1, 4, x - 1, 9, light(c, 0.18)); g.rect(x + 1, 4, x + 1, 9, dark(c, 0.25));
        }
    }
    function smokeBg() {
        const g = recessed(22, 16, [46, 42, 40], 31, { grain: 0.06 });
        g.rect(2, 2, 19, 2, STEEL[3]);
        for (const x of SAUSAGE) g.px(x, 3, STEEL[2]);
        sausages(g, [176, 96, 72]);
        for (let x = 2; x < 20; x++) g.px(x, 13, x % 2 ? STEEL[2] : FRAME[1]);
        for (const x of [4, 8, 11, 15, 18]) g.px(x, 14, HOT[3]);
        return g;
    }
    function smokeFill() {
        const g = new G(22, 16);
        for (let y = 1; y <= 14; y++) for (let x = 1; x <= 20; x++) g.px(x, y, [150, 148, 142], 80);
        sausages(g, [128, 64, 40]);
        return g;
    }
    function smokeAnim(f) {
        const g = new G(22, 16);
        for (let i = 0; i < 3; i++) {
            const x = 3 + i * 7 + (f + i) % 2, y = 12 - ((f * 3 + i * 4) % 11);
            g.rect(x, y, x + 1, y + 1, [206, 204, 198], 140); g.px(x + 1, y, [230, 230, 226], 170);
        }
        return g;
    }

    // Film winder: the film runs in from the left and the roll grows fatter.
    function winder(k) {
        const g = new G(22, 16), r = 2 + k * 0.66, cx = 14, cy = 7, FILM = [176, 208, 222];
        g.rect(6, 15, 21, 15, STEEL[2]); g.px(6, 15, STEEL[4]);
        const ty = Math.round(cy + r);
        for (let x = 0; x <= cx; x++) g.px(x, ty, x < 2 ? light(FILM, 0.3) : FILM, 230);
        for (let y = 0; y < 16; y++) for (let x = 0; x < 22; x++) {
            const d = Math.hypot(x - cx, y - cy);
            if (d > r + 0.3) continue;
            let c = mix(dark(FILM, 0.25), light(FILM, 0.3), 0.5 - (x - cx + y - cy) / (2.5 * r + 1));
            if (d > 1.7 && Math.floor(d / 1.3) % 2) c = dark(c, 0.1);
            if (d > r - 0.7) c = dark(c, 0.3);
            if (d < 1.7) c = d < 0.8 ? STEEL[4] : STEEL[0];
            g.px(x, y, c);
        }
        return g;
    }

    // Haul-off cutter: the pipe is pulled along its trough under the spinning saw.
    function sawBg() {
        const g = new G(22, 16);
        g.rect(0, 9, 21, 14, [96, 102, 110]); g.rect(0, 9, 21, 9, SHADOW); g.rect(0, 14, 21, 14, LIP);
        g.rect(0, 15, 21, 15, STEEL[2]);
        return g;
    }
    function sawFill() {
        const g = new G(22, 16);
        g.rect(0, 10, 21, 10, [110, 114, 120]); g.rect(0, 11, 21, 12, [48, 50, 54]); g.rect(0, 13, 21, 13, [30, 31, 34]);
        return g;
    }
    function sawAnim(f) {
        const g = new G(22, 16), cx = 15.5, cy = 6.5;
        for (let y = 0; y < 16; y++) for (let x = 0; x < 22; x++) {
            const d = Math.hypot(x - cx, y - cy), a = Math.atan2(y - cy, x - cx);
            if (d < 1.2) g.px(x, y, FRAME[1]);
            else if (d < 4.2) g.px(x, y, mix(STEEL[2], STEEL[4], 0.5 - (x - cx + y - cy) / 10));
            else if (d < 5.3 && ((a / (Math.PI * 2) * 10 + f / 4 + 10) % 1) < 0.5) g.px(x, y, STEEL[3]);
        }
        g.rect(9, 0, 21, 4, STEEL[2]); g.bevel(9, 0, 21, 4, STEEL[4], FRAME[0]);
        g.px(11, 2, HOT[2]); g.px(12, 2, HOT[2]);
        return g;
    }

    const PROGRESS = {
        arrow: () => [
            shaped(22, 16, arrowIn, () => [66, 71, 79], SHADOW, LIP),
            shaped(22, 16, arrowInner, (x, y) => mix([64, 160, 84], [150, 236, 156], 1 - (y - 1) / 13)),
        ],
        screw: () => [screwBg(), screwFill(), ...[0, 1, 2, 3, 4, 5].map(screwFlight)],
        wave: () => [waveBg(), waveFill(), ...[0, 1, 2, 3].map(waveAnim)],
        press: () => [0, 1, 2, 3, 4, 5, 6, 7].map(press),
        smoke: () => [smokeBg(), smokeFill(), ...[0, 1, 2, 3].map(smokeAnim)],
        winder: () => [0, 1, 2, 3, 4, 5, 6, 7].map(winder),
        saw: () => [sawBg(), sawFill(), ...[0, 1, 2, 3].map(sawAnim)],
    };

    function paintAll() {
        const out = [];
        for (const name in PARTS) { const [g, meta] = PARTS[name](); out.push(save(g, 'gui/' + name + '.png', meta)); }
        for (const name in PROGRESS) out.push(save(sheet(PROGRESS[name]()), 'gui/process/progress/' + name + '.png'));
        return out.length + ' textures';
    }

    window.IUCEGui = { G, PARTS, PROGRESS, paintAll };
    return 'gui loaded';
})();
