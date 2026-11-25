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

    // ---- machine schematics, in the vanilla panel colours (C6C6C6 panel, white light, 555555 shade, 373737 dark).
    // The background covers panel rows 16..75 and is drawn under the slot frames, which the screen draws itself.
    const VD = [55, 55, 55], VS = [85, 85, 85], VL = [255, 255, 255], PELLET = [228, 226, 212];

    // Single screw extruder: hopper slot over the feed throat, heated barrel with a cut-away window,
    // die slot in a housing at the barrel end, core wire guided down into the die, product after it.
    function extruderBack() {
        const O = 16, g = new G(176, 60);
        const P = (x, y, c) => g.px(x, y - O, c);
        const R = (x0, y0, x1, y1, c) => g.rect(x0, y0 - O, x1, y1 - O, c);
        for (let y = 38; y <= 47; y++) {
            const t = (y - 38) / 9, l = Math.round(31 + 5 * t), r = Math.round(48 - 5 * t);
            P(l, y, VD); P(r, y, VD);
            for (let x = l + 1; x < r; x++) P(x, y, x < l + 3 ? [176, 176, 176] : x > r - 3 ? [118, 118, 118] : [44, 44, 44]);
        }
        P(39, 41, PELLET); P(41, 44, PELLET); P(38, 46, PELLET);
        const BAR = [VD, [236, 236, 236], [216, 216, 216], [206, 206, 206], [194, 194, 194], [186, 186, 186], [178, 178, 178], [172, 172, 172],
            [164, 164, 164], [156, 156, 156], [148, 148, 148], [138, 138, 138], [124, 124, 124], [104, 104, 104], VS, VD];
        for (let i = 0; i < 16; i++) R(20, 48 + i, 120, 48 + i, BAR[i]);
        R(36, 48, 43, 49, [44, 44, 44]);
        R(17, 45, 21, 66, [128, 128, 128]); g.bevel(17, 45 - O, 21, 66 - O, [190, 190, 190], VD);
        P(19, 47, VL); P(19, 64, VL);
        g.bevel(51, 50 - O, 116, 61 - O, VS, [234, 234, 234]);
        R(52, 51, 115, 60, [30, 30, 30]);
        for (const x0 of [60, 80, 100]) {
            for (const [y, c] of [[47, COPPER[0]], [48, COPPER[3]], [49, COPPER[2]], [62, COPPER[1]], [63, COPPER[1]], [64, COPPER[0]]]) R(x0, y, x0 + 3, y, c);
            R(x0 + 3, 47, x0 + 3, 49, COPPER[0]); R(x0 + 3, 62, x0 + 3, 64, COPPER[0]);
        }
        R(119, 44, 141, 66, [140, 144, 150]);
        g.bevel(119, 44 - O, 141, 66 - O, VD, VD);
        g.bevel(120, 45 - O, 140, 65 - O, [206, 210, 214], [92, 96, 102]);
        P(140, 45, VL); P(140, 65, VS);
        R(142, 53, 148, 53, VD); R(142, 54, 148, 56, [156, 156, 156]); R(142, 57, 148, 57, VS);
        R(127, 38, 132, 43, [180, 180, 180]); R(127, 38, 127, 43, VS); R(132, 38, 132, 43, VD);
        R(129, 38, 129, 43, COPPER[2]); R(130, 38, 130, 43, COPPER[1]);
        return g;
    }
    // Window overlay, 64 x 10 per row: row 0 the melt (pellets turning into hot melt), then six frames of the turning screw.
    function extruderMelt() {
        const g = new G(64, 10), r = rng(41);
        for (let y = 0; y < 10; y++) for (let x = 0; x < 64; x++) {
            if (x < 10) { if (r() < 0.4) g.px(x, y, PELLET); continue; }
            const c = mix(mix([150, 72, 40], HOT[2], Math.min(1, (x - 10) / 34)), HOT[1], Math.max(0, (x - 44) / 20));
            g.px(x, y, y < 2 ? light(c, 0.15) : y > 7 ? dark(c, 0.2) : c);
        }
        return g;
    }
    function extruderScrew(f) {
        const g = new G(64, 10);
        for (let y = 0; y < 10; y++) for (let x = 0; x < 64; x++) {
            const k = (((x - f + Math.floor(y / 2)) % 6) + 6) % 6;
            if (k === 0) g.px(x, y, [214, 214, 214]); else if (k === 1) g.px(x, y, [130, 130, 130]);
        }
        g.rect(0, 4, 63, 4, [200, 200, 200]); g.rect(0, 5, 63, 5, [120, 120, 120]);
        return g;
    }

    // ---- the other one-in, one-out machines. The helpers draw in panel coordinates (y from 16).
    const METAL = [170, 173, 178], METAL_L = [218, 220, 224], METAL_D = [104, 108, 114], HOLE = [36, 36, 38];
    const FILM = [200, 226, 240], FILM_D = [140, 176, 200];

    function panelCanvas() {
        const O = 16, g = new G(176, 60);
        const R = (x0, y0, x1, y1, c) => g.rect(x0, y0 - O, x1, y1 - O, c);
        const P = (x, y, c, a) => g.px(x, y - O, c, a);
        const body = (x0, y0, x1, y1, face, lit, shade) => {
            R(x0, y0, x1, y1, face || METAL);
            g.bevel(x0, y0 - O, x1, y1 - O, VD, VD);
            if (x1 - x0 > 2 && y1 - y0 > 2) g.bevel(x0 + 1, y0 + 1 - O, x1 - 1, y1 - 1 - O, lit || METAL_L, shade || METAL_D);
        };
        // a short roller conveyor from a slot into the machine
        const chute = (x0, x1, yc) => {
            R(x0, yc - 2, x1, yc - 2, VD); R(x0, yc + 2, x1, yc + 2, VD);
            for (let x = x0; x <= x1; x++) {
                P(x, yc - 1, [204, 204, 204]); P(x, yc, [160, 160, 160]); P(x, yc + 1, (x - x0) % 3 === 0 ? VS : [132, 132, 132]);
            }
        };
        return { g, R, P, body, chute };
    }
    function discAt(g, cx, cy, r, f) {
        for (let y = Math.floor(cy - r) - 1; y <= cy + r + 1; y++) for (let x = Math.floor(cx - r) - 1; x <= cx + r + 1; x++) {
            const d = Math.hypot(x - cx, y - cy);
            if (d <= r) g.px(x, y, f(d, Math.atan2(y - cy, x - cx), x, y));
        }
    }
    function seg(g, x0, y0, x1, y1, c) {
        const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0), 1);
        for (let i = 0; i <= n; i++) g.px(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, c);
    }
    const rowsOf = (n, draw) => [...Array(n).keys()].map(i => draw(i));
    // lit from the top left; a dark spoke shows it turning
    const roller = (g, cx, cy, r, spin) => discAt(g, cx, cy, r, (d, a) => d < 1.2 || d > r - 0.7 ? VD
        : Math.abs(Math.sin(a - spin)) < 0.25 ? METAL_D : mix(METAL_L, METAL_D, (1 - Math.cos(a + 2.4)) / 2));

    // Cooling tank: a water trough under a spray bar; the product crosses it and goes from glowing to grey.
    function coolingBack() {
        const { g, R, P, body } = panelCanvas();
        const SPRAY = [150, 200, 240];
        body(125, 17, 129, 33);
        body(48, 33, 129, 36);
        for (let x = 54; x <= 120; x += 11) {
            R(x, 37, x + 1, 38, VD);
            P(x - 1, 40, SPRAY); P(x + 2, 41, SPRAY); P(x, 42, SPRAY); P(x + 1, 43, SPRAY);
        }
        body(43, 44, 134, 61);
        for (let y = 45; y <= 58; y++) R(44, y, 131, y, y === 45 ? [150, 196, 236] : mix([70, 130, 196], [40, 88, 150], (y - 46) / 12));
        R(43, 50, 43, 53, HOLE); R(132, 50, 134, 53, HOLE);
        body(50, 62, 54, 69); body(121, 62, 125, 69);
        R(86, 62, 89, 64, VS);
        return g;
    }
    function coolingProduct() {
        const g = new G(88, 10);
        for (let x = 0; x < 88; x++) {
            const t = Math.min(1, x / 70), c = t < 0.5 ? mix(HOT[1], HOT[3], t * 2) : mix(HOT[3], [120, 122, 128], (t - 0.5) * 2);
            g.px(x, 3, light(c, 0.3)); g.px(x, 4, c); g.px(x, 5, c); g.px(x, 6, dark(c, 0.3));
        }
        return g;
    }
    function coolingBubbles(f) {
        const g = new G(88, 10), r = rng(57);
        for (let i = 0; i < 16; i++) {
            const x = Math.floor(r() * 88), y = [0, 1, 8, 9][Math.floor(r() * 4)];
            g.px((x + f * 2) % 88, y, [176, 214, 246]);
            if (r() < 0.4) g.px((x + f * 2 + 1) % 88, y, [120, 170, 220]);
        }
        return g;
    }

    // Vulcanizer: a press frame; the heated platen comes down on the mould slot, steams, and lifts.
    function vulcBack() {
        const { g, R, P, body, chute } = panelCanvas();
        body(46, 17, 129, 23);
        for (const x of [50, 124, 87]) { P(x, 20, VL); P(x + 1, 20, VS); }
        body(49, 24, 55, 61); body(120, 24, 126, 61);
        body(46, 61, 129, 67);
        body(58, 56, 77, 60, COPPER[1], COPPER[3], COPPER[0]); body(98, 56, 117, 60, COPPER[1], COPPER[3], COPPER[0]);
        body(56, 68, 119, 71, [232, 232, 232], VL, [176, 176, 176]);
        R(86, 68, 89, 71, [176, 48, 40]);
        chute(43, 48, 48); chute(127, 134, 48);
        return g;
    }
    function vulcPhase(k) {
        const g = new G(56, 19), py = [0, 4, 8, 13, 13, 13, 13, 6][k];
        for (let y = 0; y < py; y++) { g.px(26, y, METAL_L); g.px(27, y, METAL); g.px(28, y, METAL); g.px(29, y, METAL_D); }
        g.rect(2, py, 53, py + 5, METAL); g.bevel(2, py, 53, py + 5, VD, VD); g.bevel(3, py + 1, 52, py + 4, METAL_L, METAL_D);
        g.rect(4, py + 3, 51, py + 3, COPPER[2]);
        if (k >= 4 && k <= 6) {
            const r = rng(70 + k);
            for (let i = 0; i < 12; i++) {
                const x = i % 2 ? 53 + Math.floor(r() * 3) : Math.floor(r() * 3);
                g.px(x, 8 + Math.floor(r() * 10), [244, 244, 244], 210);
            }
        }
        return g;
    }

    // Smokehouse: sheets hang on a rail in a wooden house over a log fire; smoke fills it from below.
    function smokeBack() {
        const { g, R, P, chute } = panelCanvas();
        const WOOD = [156, 110, 70], WOOD_D = [112, 76, 46], SHEET = [226, 210, 166], RAIL = [156, 156, 156];
        for (let y = 17; y <= 30; y++) {
            const half = Math.round((y - 17) * 36 / 13);
            R(86 - half, y, 86 + half, y, (y - 17) % 3 === 2 ? WOOD_D : WOOD);
            P(86 - half, y, VD); P(86 + half, y, VD);
        }
        R(102, 16, 107, 24, [150, 72, 56]); g.bevel(102, 0, 107, 8, [196, 110, 90], VD);
        R(52, 31, 119, 70, WOOD);
        for (let x = 52; x <= 119; x += 5) R(x, 31, x, 70, WOOD_D);
        g.bevel(52, 15, 119, 54, VD, VD);
        R(56, 31, 115, 68, [64, 56, 48]);
        R(43, 33, 115, 33, RAIL); R(43, 34, 55, 34, VS);
        for (const x0 of [59, 70, 81, 92, 103]) {
            R(x0, 35, x0 + 7, 52, SHEET); g.bevel(x0, 19, x0 + 7, 36, light(SHEET, 0.4), dark(SHEET, 0.25));
            P(x0 + 3, 34, VD); P(x0 + 4, 34, VD);
        }
        R(56, 61, 115, 61, VD);
        for (const x of [62, 76, 90, 104]) {
            R(x, 63, x + 9, 66, [110, 72, 40]); R(x, 63, x + 9, 63, [150, 104, 62]);
            P(x + 9, 64, [196, 160, 110]); P(x + 9, 65, [196, 160, 110]);
        }
        const r = rng(83);
        for (let i = 0; i < 18; i++) P(57 + Math.floor(r() * 58), 67 + Math.floor(r() * 2), r() < 0.5 ? [232, 120, 40] : [150, 52, 24]);
        R(52, 58, 55, 66, HOLE); chute(43, 51, 60);
        R(116, 44, 119, 52, HOLE); chute(120, 134, 48);
        return g;
    }
    function smokeFill() {
        const g = new G(60, 38), r = rng(91);
        for (let y = 0; y < 38; y++) for (let x = 0; x < 60; x++) {
            const t = y / 37;
            g.px(x, y, mix([168, 162, 154], [104, 98, 92], t), 70 + 100 * t + 30 * r());
        }
        return g;
    }
    function smokeFire(f) {
        const g = new G(60, 38), r = rng(95 + f);
        [11, 25, 39, 53].forEach((cx, i) => {
            const h = 4 + ((f + i) % 3), lean = (f + i) % 2 ? 1 : -1;
            for (let dy = 0; dy <= h; dy++) {
                const half = Math.max(0, Math.round((h - dy) / 2.2));
                for (let dx = -half; dx <= half; dx++) g.px(cx + dx + (dy > 2 ? lean : 0), 32 - dy, dy < 2 ? HOT[2] : dy < h - 1 ? HOT[1] : HOT[0]);
            }
        });
        for (let i = 0; i < 6; i++) g.px(Math.floor(r() * 60), 4 + Math.floor(r() * 22), [200, 196, 190], 150);
        return g;
    }

    // Film winder: the film runs through the nip rollers and over a guide onto a roll that grows.
    function winderBack() {
        const { g, R, body } = panelCanvas();
        body(45, 27, 59, 56);
        roller(g, 52, 20, 4, 0.5); roller(g, 52, 30, 4, 2);
        R(43, 41, 69, 41, FILM);
        body(105, 44, 111, 65);
        body(92, 65, 124, 70);
        return g;
    }
    function winderPhase(k) {
        const g = new G(60, 48), r = 4 + 2 * k, cx = 38, cy = 26;
        g.rect(0, 23, 5, 23, FILM);
        roller(g, 8, 23, 3, k * 0.9);
        seg(g, 8, 20, cx, cy - r, FILM);
        discAt(g, cx, cy, r, (d, a) => {
            if (d < 3) return d < 1.2 ? VD : Math.abs(Math.sin(a - k * 0.9)) < 0.35 ? METAL_L : [96, 98, 104];
            if (d > r - 0.8) return FILM_D;
            const c = Math.floor(d) % 2 ? FILM : mix(FILM, FILM_D, 0.5);
            return a < -0.6 && a > -2.6 && d > r - 2.5 ? light(c, 0.4) : c;
        });
        return g;
    }

    // Haul-off cutter: two caterpillar tracks pull the pipe through, a swinging saw cuts it.
    function hauloffBack() {
        const { g, R, body } = panelCanvas();
        const RUBBER = [92, 94, 98], RUBBER_L = [150, 152, 156], RUBBER_D = [60, 62, 66];
        body(66, 17, 73, 36); body(66, 61, 73, 69);
        body(50, 36, 90, 44, RUBBER, RUBBER_L, RUBBER_D); body(50, 53, 90, 61, RUBBER, RUBBER_L, RUBBER_D);
        body(46, 65, 129, 70);
        body(43, 43, 47, 54); R(44, 46, 46, 51, HOLE);
        body(130, 43, 134, 54); R(130, 46, 133, 51, HOLE);
        body(113, 17, 127, 25);
        for (let i = -1; i <= 1; i++) seg(g, 119 + i, 9, 110 + i, 23, i < 0 ? METAL_L : i > 0 ? METAL_D : METAL);
        discAt(g, 110, 23, 11, (d, a) => d > 9.5 && a < -0.3 && a > -2.85 ? (d > 10.5 ? VD : [236, 200, 60]) : undefined);
        return g;
    }
    function hauloffPipe() {
        const g = new G(84, 34), PIPE = [[60, 64, 72], [128, 136, 148], [104, 112, 124], [88, 94, 106], [72, 78, 88], [44, 48, 54]];
        for (let x = 0; x < 84; x++) PIPE.forEach((c, i) => g.px(x, 16 + i, c));
        return g;
    }
    function hauloffMove(f) {
        const g = new G(84, 34), DARK = [40, 40, 42], LIT = [130, 132, 136];
        for (let x = 5; x <= 43; x++) {
            const inner = (((x - f) % 4) + 4) % 4 < 2, outer = (((x + f) % 4) + 4) % 4 < 2;
            g.px(x, 13, inner ? DARK : LIT); g.px(x, 24, inner ? DARK : LIT);
            g.px(x, 7, outer ? DARK : LIT); g.px(x, 30, outer ? DARK : LIT);
        }
        discAt(g, 64, 9, 8.5, (d, a) => {
            if (d < 2) return VD;
            if (d < 6.5) return d > 3.5 && d < 4.5 ? METAL_D : mix(METAL_L, METAL, (1 - Math.cos(a + 2.4)) / 2);
            const t = ((((a / (2 * Math.PI)) * 12 + f / 4) % 1) + 1) % 1;
            return t < 0.55 ? [236, 236, 240] : undefined;
        });
        return g;
    }

    // CNC machining centre: the spindle travels along the beam and mills a pocket into the stock in the vise.
    function cncBack() {
        const { g, R, body, chute } = panelCanvas();
        body(45, 17, 130, 22); R(48, 20, 127, 20, VD);
        body(45, 23, 50, 69); body(125, 23, 130, 69);
        body(45, 65, 130, 70);
        R(127, 28, 128, 30, [80, 200, 120]);
        chute(43, 44, 48); chute(131, 134, 48);
        return g;
    }
    function cncPhase(k) {
        const g = new G(72, 42), STOCK = [156, 162, 172];
        const sx = [10, 26, 30, 34, 38, 42, 46, 46][k], tip = k === 0 || k === 7 ? 20 : 34, end = tip - 6;
        g.rect(4, 38, 67, 41, METAL); g.bevel(4, 38, 67, 41, METAL_L, METAL_D);
        for (const x0 of [16, 50]) { g.rect(x0, 29, x0 + 5, 37, [70, 96, 150]); g.bevel(x0, 29, x0 + 5, 37, [130, 160, 210], [40, 56, 96]); }
        g.rect(22, 31, 49, 37, STOCK); g.rect(22, 31, 49, 31, light(STOCK, 0.4)); g.rect(22, 37, 49, 37, dark(STOCK, 0.3));
        if (k > 0) { g.rect(25, 31, sx, 33, [62, 66, 74]); g.rect(25, 34, sx, 34, [96, 100, 108]); }
        g.rect(sx - 6, 0, sx + 6, 5, METAL); g.bevel(sx - 6, 0, sx + 6, 5, VD, VD); g.bevel(sx - 5, 1, sx + 5, 4, METAL_L, METAL_D);
        g.rect(sx - 3, 6, sx + 3, end, METAL); g.bevel(sx - 3, 6, sx + 3, end, METAL_L, VD);
        g.rect(sx - 1, end + 1, sx, tip, [232, 232, 236]);
        if (k > 0 && k < 7) {
            const r = rng(110 + k);
            for (let i = 0; i < 6; i++) g.px(sx - 5 + Math.floor(r() * 11), 27 + Math.floor(r() * 4), [226, 200, 140]);
        }
        return g;
    }

    // ---- machines with tanks. The screen draws the tank gauges; the picture puts drums in a rack around
    // them and runs pipes from the drums and slots to the vessel.
    const PIPE_L = [196, 198, 202], WATER_L = [90, 140, 200];
    function pipes(R) {
        const h = (x0, x1, y) => { R(x0, y - 1, x1, y - 1, VD); R(x0, y, x1, y, PIPE_L); R(x0, y + 1, x1, y + 1, METAL_D); R(x0, y + 2, x1, y + 2, VD); };
        const v = (x, y0, y1) => { R(x - 1, y0, x - 1, y1, VD); R(x, y0, x, y1, PIPE_L); R(x + 1, y0, x + 1, y1, METAL_D); R(x + 2, y0, x + 2, y1, VD); };
        return { h, v };
    }
    function drums(c, x0, x1) {
        c.body(x0, 20, x1, 73);
        c.R(x0 + 2, 74, x0 + 4, 74, VD); c.R(x1 - 4, 74, x1 - 2, 74, VD);
        for (let x = x0 + 3; x <= x1 - 3; x += 5) { c.P(x, 21, VS); c.P(x, 72, VS); }
    }
    // a cut-away window into a vessel, filled with a colour or a picture
    function cutaway(c, x0, y0, x1, y1, fill) {
        const EDGE_L = [234, 234, 234];
        c.R(x0 - 1, y0 - 1, x1 + 1, y0 - 1, VS); c.R(x0 - 1, y0 - 1, x0 - 1, y1 + 1, VS);
        c.R(x0 - 1, y1 + 1, x1 + 1, y1 + 1, EDGE_L); c.R(x1 + 1, y0 - 1, x1 + 1, y1 + 1, EDGE_L);
        if (fill instanceof G) c.g.draw(fill, x0, y0 - 16); else c.R(x0, y0, x1, y1, fill);
    }

    // Polymerization reactor: monomer drums on the left, the slurry and off-gas drums on the right; the
    // card reader drives the agitator motor on the lid, the slurry fills the jacketed vessel.
    function polyBack() {
        const c = panelCanvas(), { R, P, body } = c, { h, v } = pipes(R);
        drums(c, 17, 47); drums(c, 134, 164);
        body(70, 31, 109, 70);
        for (const y of [40, 50, 60]) { R(71, y, 72, y, WATER_L); R(107, y, 108, y, WATER_L); }
        cutaway(c, 74, 34, 105, 66, [30, 30, 30]);
        body(73, 71, 77, 74); body(102, 71, 106, 74);
        body(81, 17, 98, 27, [92, 122, 170], [146, 174, 220], [52, 72, 112]);
        for (let x = 84; x <= 95; x += 2) R(x, 19, x, 25, [70, 96, 140]);
        body(87, 28, 92, 30);
        h(67, 77, 24); v(76, 27, 30);
        h(67, 69, 44); h(67, 69, 64);
        R(48, 34, 69, 34, PIPE_L); R(48, 35, 69, 35, METAL_D);
        h(110, 133, 44); h(110, 112, 64);
        R(99, 23, 112, 23, VD); R(99, 24, 112, 24, [70, 70, 72]); R(99, 25, 112, 25, VD);
        return c.g;
    }
    function polySlurry() {
        const g = new G(32, 33), r = rng(131);
        for (let y = 0; y < 33; y++) for (let x = 0; x < 32; x++) {
            const col = mix([236, 234, 224], [186, 182, 170], y / 32);
            g.px(x, y, y === 0 ? VL : r() < 0.08 ? dark(col, 0.08) : col);
        }
        return g;
    }
    function polyStir(f) {
        const g = new G(32, 33), half = [11, 7, 2, 7][f], lit = f % 2 ? METAL_D : METAL_L, shade = f % 2 ? METAL_L : METAL_D;
        for (let y = 0; y <= 28; y++) { g.px(15, y, METAL_L); g.px(16, y, METAL_D); }
        for (const by of [12, 25]) {
            g.rect(15 - half, by, 16 + half, by, lit); g.rect(15 - half, by + 1, 16 + half, by + 1, shade);
            g.rect(14, by - 1, 17, by + 2, VD); g.rect(15, by, 16, by + 1, METAL);
        }
        return g;
    }

    // Distillation column: the feed drum on the left, a trayed column with the condenser at its head and
    // the reboiler at its foot; the tops go over to the outer drum, the bottoms to the inner one, the
    // residue drains to the slot at the foot.
    function distBack() {
        const c = panelCanvas(), { R, P, body } = c, { h, v } = pipes(R);
        drums(c, 18, 37); drums(c, 134, 165);
        body(66, 18, 93, 72);
        cutaway(c, 70, 22, 89, 68, [26, 28, 32]);
        for (let k = 0; k < 5; k++) {
            const ty = 28 + k * 8, xa = k % 2 ? 74 : 70, xb = k % 2 ? 89 : 85;
            R(xa, ty, xb, ty, METAL_L); R(xa, ty + 1, xb, ty + 1, METAL_D);
        }
        R(70, 73, 89, 74, VS);
        h(38, 65, 40);
        h(61, 65, 62);
        h(94, 99, 21);
        body(100, 17, 128, 27);
        for (let x = 103; x <= 125; x += 3) R(x, 19, x, 25, [120, 160, 200]);
        R(129, 17, 158, 17, PIPE_L); R(129, 18, 158, 18, METAL_D); R(157, 19, 158, 19, METAL_D);
        h(94, 97, 64);
        body(98, 57, 124, 70);
        for (let x = 101; x <= 121; x += 4) { R(x, 60, x, 67, COPPER[2]); R(x + 1, 60, x + 1, 67, COPPER[0]); }
        h(125, 133, 64);
        v(110, 71, 74); P(110, 72, [190, 50, 40]); P(111, 72, [190, 50, 40]);
        return c.g;
    }
    function distVapour() {
        const g = new G(20, 47), r = rng(141);
        for (let y = 0; y < 47; y++) for (let x = 0; x < 20; x++) g.px(x, y, mix([250, 238, 206], [236, 196, 132], y / 46), 100 + 50 * r());
        return g;
    }
    function distBubble(f) {
        const g = new G(20, 47), LIQ = [214, 176, 92], FOAM = [250, 240, 214];
        for (let k = 0; k < 5; k++) {
            const ty = 6 + k * 8, xa = k % 2 ? 4 : 0, xb = k % 2 ? 19 : 15;
            g.rect(xa, ty - 1, xb, ty - 1, LIQ, 220);
            for (let x = xa; x <= xb; x++) {
                const s = (x * 5 + k * 3 + f * 2) % 7;
                if (s === 0) g.px(x, ty - 2, FOAM, 230);
                if (s === 3) g.px(x, ty - 3, FOAM, 170);
            }
            const dx = k % 2 ? 2 : 17, dy = ty + 1 + ((f * 2 + k) % 6);
            g.px(dx, dy, LIQ, 220);
        }
        return g;
    }

    // Lime kiln: the belt carries the stone up into the hopper on a brick shaft; burners fire it from the
    // side, it turns to lime from the bottom up and drops to the slot, the gas goes off to the drum.
    function stones(base) {
        const g = new G(24, 41);
        for (let y = 0; y < 41; y++) for (let x = 0; x < 24; x++) {
            const row = Math.floor(y / 5), off = row % 2 ? 3 : 0, col = Math.floor((x + off) / 6);
            const gap = y % 5 === 4 || (x + off) % 6 === 5;
            const t = ((row * 7 + col * 13) % 5) / 4;
            const s = gap ? dark(base, 0.5) : mix(light(base, 0.1), dark(base, 0.16), t);
            g.px(x, y, y % 5 === 0 && !gap ? light(s, 0.2) : s);
        }
        return g;
    }
    function kilnBack() {
        const c = panelCanvas(), { g, R, P, body, chute } = c, { h } = pipes(R);
        const BRICK = [168, 84, 60], BRICK_D = [122, 58, 42], MORTAR = [196, 180, 160];
        for (let y = 22; y <= 70; y++) for (let x = 70; x <= 101; x++) {
            const row = Math.floor((y - 22) / 3), off = row % 2 ? 2 : 0;
            const mortar = (y - 22) % 3 === 2 || (x - 70 + off) % 5 === 4;
            P(x, y, mortar ? MORTAR : (x + row) % 3 ? BRICK : BRICK_D);
        }
        R(70, 22, 101, 22, VD); R(70, 70, 101, 70, VD); R(70, 22, 70, 70, VD); R(101, 22, 101, 70, VD);
        cutaway(c, 74, 26, 97, 66, stones([132, 128, 122]));
        body(66, 71, 105, 74);
        for (let y = 17; y <= 21; y++) {
            const l = 77 + (y - 17), r = 94 - (y - 17);
            R(l, y, r, y, y === 17 ? VD : [64, 62, 60]); P(l, y, VD); P(r, y, VD);
        }
        P(83, 19, [150, 146, 140]); P(87, 20, [150, 146, 140]); P(90, 19, [150, 146, 140]);
        for (let t = -1; t <= 1; t++) seg(g, 40, 41 + t - 16, 76, 19 + t - 16, t === 0 ? [72, 72, 74] : VD);
        for (let i = 1; i < 6; i++) { const x = 40 + i * 6; P(x, Math.round(41 - 22 * (x - 40) / 36) - 2, [150, 146, 140]); }
        roller(g, 40, 41 - 16, 2, 0); roller(g, 76, 19 - 16, 2, 0);
        R(56, 33, 56, 74, METAL_L); R(57, 33, 57, 74, METAL_D);
        const STEEL_G = [150, 150, 150], STEEL_GL = [204, 204, 204], STEEL_GD = [100, 100, 100];
        body(63, 42, 69, 47, STEEL_G, STEEL_GL, STEEL_GD); body(63, 52, 69, 57, STEEL_G, STEEL_GL, STEEL_GD);
        R(66, 48, 66, 51, VD); P(65, 44, [200, 50, 40]); P(65, 54, [200, 50, 40]);
        h(102, 145, 24);
        chute(102, 116, 60);
        drums(c, 146, 163);
        return g;
    }
    function kilnFlame(f) {
        const g = new G(24, 41);
        [3, 9, 15, 21].forEach((cx, i) => {
            const ht = 5 + ((f + i) % 3) * 2, lean = (f + i) % 2 ? 1 : -1;
            for (let dy = 0; dy <= ht; dy++) {
                const half = Math.max(0, Math.round((ht - dy) / 2.5));
                for (let dx = -half; dx <= half; dx++) g.px(cx + dx + (dy > 2 ? lean : 0), 27 - dy, dy < 2 ? HOT[2] : dy < ht - 1 ? HOT[1] : HOT[0], 220);
            }
        });
        return g;
    }

    // Fermenter: feed and culture go in through ports on the lid, the liquid drums sit either side of the
    // jacketed tank; the sugar solution clouds over from the bottom up and foams at the top.
    function fermBase() {
        const g = new G(52, 27);
        for (let y = 0; y < 27; y++) for (let x = 0; x < 52; x++) {
            if (y < 5) g.px(x, y, [44, 44, 48]);
            else g.px(x, y, y === 5 ? [236, 196, 110] : mix([214, 164, 72], [166, 114, 42], (y - 6) / 20));
        }
        return g;
    }
    function fermBroth() {
        const g = new G(52, 27), r = rng(151);
        for (let y = 3; y < 27; y++) for (let x = 0; x < 52; x++) {
            if (y < 7) g.px(x, y, r() < 0.25 ? [214, 204, 180] : [244, 238, 220]);
            else g.px(x, y, r() < 0.1 ? [200, 176, 126] : mix([224, 202, 152], [186, 160, 110], (y - 7) / 19));
        }
        return g;
    }
    function fermBubbles(f) {
        const g = new G(52, 27);
        [5, 12, 20, 27, 33, 41, 47].forEach((bx, i) => {
            for (let j = 0; j < 2; j++) {
                const y = 26 - ((f * 3 + j * 11 + i * 5) % 21);
                if (y < 6) continue;
                g.px(bx, y, [252, 244, 226], 220);
                if ((i + j) % 2) g.px(bx + 1, y, [252, 244, 226], 140);
            }
        });
        return g;
    }
    function fermBack() {
        const c = panelCanvas(), { R, body } = c, { h } = pipes(R);
        drums(c, 17, 47); drums(c, 135, 165);
        body(54, 35, 113, 70);
        for (const y of [46, 54, 62]) { R(55, y, 56, y, WATER_L); R(111, y, 112, y, WATER_L); }
        cutaway(c, 58, 40, 109, 66, fermBase());
        for (const cx of [63, 82, 101]) R(cx, 34, cx + 1, 34, METAL_L);
        body(58, 71, 62, 74); body(105, 71, 109, 74);
        h(48, 53, 60);
        h(114, 134, 44); h(114, 114, 64);
        return c.g;
    }

    // ---- the reactor series: the polymerization reactor's picture for any count of slots and drums.
    // Input drums sit left of the jacketed vessel and output drums right of it; the item slots stand in a
    // column either side (inputs and then the catalyst from the top, outputs from the bottom). Stirred
    // machines carry the agitator motor on the lid, gas-phase ones a dome, with bubbles rising through
    // the charge. ProcessSchematic.reactor works out the same positions from the same counts.
    const REACTORS = [
        { id: 'additive_reactor', in: 3, out: 1, fIn: 2, fOut: 0, cat: false, stir: true, top: [232, 226, 206], bottom: [196, 186, 160] },
        { id: 'catalyst_prep_tank', in: 2, out: 1, fIn: 2, fOut: 0, cat: false, stir: true, top: [150, 170, 190], bottom: [96, 112, 132] },
        { id: 'dehydration_reactor', in: 0, out: 1, fIn: 1, fOut: 2, cat: true, stir: false, top: [236, 214, 160], bottom: [196, 160, 96] },
        { id: 'depolymerization_reactor', in: 0, out: 1, fIn: 1, fOut: 1, cat: true, stir: true, top: [196, 150, 96], bottom: [132, 92, 52] },
        { id: 'oligomerization_reactor', in: 0, out: 0, fIn: 1, fOut: 2, cat: false, stir: true, top: [244, 236, 196], bottom: [208, 196, 140] },
        { id: 'vcm_converter', in: 0, out: 1, fIn: 2, fOut: 1, cat: true, stir: false, top: [200, 226, 196], bottom: [140, 176, 136] },
        { id: 'neutralizer', in: 0, out: 0, fIn: 2, fOut: 1, cat: false, stir: true, top: [220, 232, 240], bottom: [170, 190, 206] },
        { id: 'acetylene_generator', in: 1, out: 0, fIn: 1, fOut: 2, cat: false, stir: false, top: [208, 208, 204], bottom: [150, 150, 146] },
    ];
    const REACTOR_YS = [17, 37, 57];
    function reactorBack(s) {
        const c = panelCanvas(), { R, body } = c, { h, v } = pipes(R);
        if (s.fIn) drums(c, 17, 17 + 15 * s.fIn);
        if (s.fOut) drums(c, 149 - 15 * (s.fOut - 1), 164);
        body(70, 31, 109, 70);
        for (const y of [40, 50, 60]) { R(71, y, 72, y, WATER_L); R(107, y, 108, y, WATER_L); }
        cutaway(c, 74, 34, 105, 66, [30, 30, 30]);
        body(73, 71, 77, 74); body(102, 71, 106, 74);
        if (s.stir) {
            body(81, 17, 98, 27, [92, 122, 170], [146, 174, 220], [52, 72, 112]);
            for (let x = 84; x <= 95; x += 2) R(x, 19, x, 25, [70, 96, 140]);
            body(87, 28, 92, 30);
        } else {
            body(82, 23, 97, 30);
            for (let x = 85; x <= 94; x += 3) R(x, 25, x, 28, METAL_D);
            v(89, 17, 22);
        }
        const left = s.in + (s.cat ? 1 : 0);
        for (let i = 0; i < left; i++) {
            const y = REACTOR_YS[i];
            if (y === 17) { h(67, 77, 24); v(76, 27, 30); } else h(67, 69, y + 7);
        }
        if (s.fIn) { R(18 + 15 * s.fIn, 34, 69, 34, PIPE_L); R(18 + 15 * s.fIn, 35, 69, 35, METAL_D); }
        if (s.fOut) h(110, 148 - 15 * (s.fOut - 1), 44);
        for (let i = 0; i < s.out; i++) h(110, 112, REACTOR_YS[2 - i] + 7);
        return c.g;
    }
    function reactorFill(s) {
        const g = new G(32, 33), r = rng(s.top[0] * 7 + s.bottom[2]);
        for (let y = 0; y < 33; y++) for (let x = 0; x < 32; x++) {
            const col = mix(s.top, s.bottom, y / 32);
            g.px(x, y, y === 0 ? light(s.top, 0.4) : r() < 0.08 ? dark(col, 0.08) : col);
        }
        return g;
    }
    function reactorBubbles(f) {
        const g = new G(32, 33), FOAM = [250, 250, 246];
        [3, 8, 13, 18, 23, 28].forEach((bx, i) => {
            for (let j = 0; j < 2; j++) {
                const y = 31 - ((f * 4 + i * 5 + j * 16) % 32);
                g.px(bx, y, FOAM, 210);
                if ((i + j) % 2) g.px(bx + 1, y - 1, FOAM, 130);
            }
        });
        return g;
    }

    // ---- the column series: the distillation column's picture for any count of drums. The feed drums
    // sit left of the trayed column and the product drums right of it; the condenser at the head sends
    // the tops to the outer drum, the reboiler at the foot the bottoms to the inner one, and any solid
    // product leaves through the slots beside the column. ProcessSchematic.column works out the same
    // positions from the same counts.
    const COLUMNS = [
        { id: 'solvent_recovery_tower', out: 1, fIn: 1, fOut: 1, top: [236, 240, 244], bottom: [196, 208, 220] },
        { id: 'tar_distiller', out: 2, fIn: 1, fOut: 1, top: [120, 104, 86], bottom: [70, 58, 46] },
        { id: 'scrubber', out: 1, fIn: 2, fOut: 2, top: [214, 232, 240], bottom: [160, 196, 214] },
        { id: 'flash_tank', out: 0, fIn: 1, fOut: 2, top: [248, 240, 214], bottom: [220, 196, 140] },
        { id: 'evaporator', out: 1, fIn: 1, fOut: 2, top: [244, 246, 248], bottom: [200, 214, 228] },
        { id: 'crystallizer', out: 1, fIn: 1, fOut: 1, top: [236, 244, 250], bottom: [186, 212, 232] },
        { id: 'vent_stack', out: 0, fIn: 1, fOut: 0, top: [222, 222, 222], bottom: [170, 170, 170] },
    ];
    function columnBack(s) {
        const c = panelCanvas(), { R, P, body } = c, { h, v } = pipes(R);
        if (s.fIn) drums(c, 17, 17 + 15 * s.fIn);
        const inner = 149 - 15 * (s.fOut - 1);
        if (s.fOut) drums(c, inner, 164);
        body(66, 18, 93, 72);
        cutaway(c, 70, 22, 89, 68, [26, 28, 32]);
        for (let k = 0; k < 5; k++) {
            const ty = 28 + k * 8, xa = k % 2 ? 74 : 70, xb = k % 2 ? 89 : 85;
            R(xa, ty, xb, ty, METAL_L); R(xa, ty + 1, xb, ty + 1, METAL_D);
        }
        R(70, 73, 89, 74, VS);
        if (s.fIn) h(18 + 15 * s.fIn, 65, 40);
        if (s.fOut) {
            h(94, 99, 21);
            body(100, 17, 128, 27);
            for (let x = 103; x <= 125; x += 3) R(x, 19, x, 25, [120, 160, 200]);
            R(129, 17, 158, 17, PIPE_L); R(129, 18, 158, 18, METAL_D); R(157, 19, 158, 19, METAL_D);
            h(94, 97, 64);
            body(98, 57, 124, 70);
            for (let x = 101; x <= 121; x += 4) { R(x, 60, x, 67, COPPER[2]); R(x + 1, 60, x + 1, 67, COPPER[0]); }
            if (s.fOut > 1) h(125, inner - 1, 64);
        } else {
            // a plain stack: the gas leaves at the top
            for (let y = 14; y <= 17; y++) { P(76, y, VD); P(83, y, VD); }
            R(77, 16, 82, 17, METAL);
        }
        if (s.out) h(94, 96, 44);
        return c.g;
    }
    function columnFill(s) {
        const g = new G(20, 47), r = rng(s.top[1] * 5 + s.bottom[0]);
        for (let y = 0; y < 47; y++) for (let x = 0; x < 20; x++) g.px(x, y, mix(s.top, s.bottom, y / 46), 100 + 50 * r());
        return g;
    }

    // ---- the kiln series: the lime kiln's brick shaft for any count of slots and drums. Fuel drums sit
    // on the left and feed the burners, the belt carries the charge from the input slots up into the
    // hopper, the product drops out to the slots on the right and the gas goes off to the right drums.
    // ProcessSchematic.kiln works out the same positions from the same counts.
    const KILNS = [
        { id: 'carbide_furnace', in: 2, out: 2, fIn: 0, fOut: 1, cat: true, raw: [150, 146, 140], done: [80, 80, 88] },
        { id: 'coking_oven', in: 1, out: 1, fIn: 0, fOut: 2, cat: false, raw: [56, 52, 50], done: [112, 112, 116] },
        { id: 'hcl_furnace', in: 0, out: 0, fIn: 2, fOut: 1, cat: false, gas: true },
        { id: 'dryer', in: 1, out: 1, fIn: 1, fOut: 1, cat: false, raw: [190, 166, 120], done: [236, 226, 200] },
    ];
    const kilnInputX = s => s.fIn ? 22 + 15 * s.fIn : 26;
    function kilnGas() {
        const g = new G(24, 41), r = rng(171);
        for (let y = 0; y < 41; y++) for (let x = 0; x < 24; x++) g.px(x, y, mix([250, 244, 200], [226, 190, 120], y / 40), 90 + 60 * r());
        return g;
    }
    function kilnSeriesBack(s) {
        const c = panelCanvas(), { g, R, P, body, chute } = c, { h } = pipes(R);
        const BRICK = [168, 84, 60], BRICK_D = [122, 58, 42], MORTAR = [196, 180, 160];
        for (let y = 22; y <= 70; y++) for (let x = 70; x <= 101; x++) {
            const row = Math.floor((y - 22) / 3), off = row % 2 ? 2 : 0;
            const mortar = (y - 22) % 3 === 2 || (x - 70 + off) % 5 === 4;
            P(x, y, mortar ? MORTAR : (x + row) % 3 ? BRICK : BRICK_D);
        }
        R(70, 22, 101, 22, VD); R(70, 70, 101, 70, VD); R(70, 22, 70, 70, VD); R(101, 22, 101, 70, VD);
        cutaway(c, 74, 26, 97, 66, s.gas ? [30, 30, 32] : stones(s.raw));
        body(66, 71, 105, 74);
        if (s.in) {
            for (let y = 17; y <= 21; y++) {
                const l = 77 + (y - 17), r = 94 - (y - 17);
                R(l, y, r, y, y === 17 ? VD : [64, 62, 60]); P(l, y, VD); P(r, y, VD);
            }
            const x0 = kilnInputX(s) + 18;
            for (let t = -1; t <= 1; t++) seg(g, x0, 39 + t - 16, 76, 19 + t - 16, t === 0 ? [72, 72, 74] : VD);
            roller(g, x0, 39 - 16, 2, 0); roller(g, 76, 19 - 16, 2, 0);
        } else {
            body(78, 17, 93, 21);
        }
        const STEEL_G = [150, 150, 150], STEEL_GL = [204, 204, 204], STEEL_GD = [100, 100, 100];
        body(63, 42, 69, 47, STEEL_G, STEEL_GL, STEEL_GD); body(63, 52, 69, 57, STEEL_G, STEEL_GL, STEEL_GD);
        R(66, 48, 66, 51, VD); P(65, 44, [200, 50, 40]); P(65, 54, [200, 50, 40]);
        if (s.fIn) { drums(c, 17, 17 + 15 * s.fIn); h(18 + 15 * s.fIn, 62, 49); }
        else { R(58, 33, 58, 74, METAL_L); R(59, 33, 59, 74, METAL_D); }
        // the electrode comes in from the catalyst slot under the shaft
        if (s.cat) { h(44, 69, 65); body(60, 62, 64, 68); }
        if (s.out) chute(102, 112, 60);
        if (s.out > 1) chute(102, 112, 40);
        if (s.fOut) { const inner = 149 - 15 * (s.fOut - 1); h(102, inner - 1, 24); drums(c, inner, 164); }
        return g;
    }

    // ---- the tank series: the fermenter's jacketed tank for any count of slots and drums. Item feeds go
    // in through ports on the lid (the catalyst in the middle), the liquid drums sit either side, the
    // solid product leaves at the lower right. The charge fills the tank from the bottom up.
    // ProcessSchematic.vessel works out the same positions from the same counts.
    const VESSELS = [
        { id: 'brine_dissolver', in: 1, out: 0, fIn: 1, fOut: 1, cat: false, top: [230, 238, 244], bottom: [180, 200, 216] },
        { id: 'brine_purifier', in: 1, out: 0, fIn: 1, fOut: 1, cat: false, top: [236, 244, 250], bottom: [196, 216, 232] },
        { id: 'coagulation_tank', in: 1, out: 1, fIn: 1, fOut: 1, cat: false, top: [246, 242, 228], bottom: [214, 204, 176] },
        { id: 'liquefaction_tank', in: 1, out: 0, fIn: 1, fOut: 1, cat: false, top: [236, 222, 180], bottom: [196, 172, 120] },
        { id: 'saccharification_tank', in: 0, out: 0, fIn: 1, fOut: 1, cat: true, top: [240, 214, 150], bottom: [206, 160, 80] },
        { id: 'water_treatment', in: 0, out: 0, fIn: 1, fOut: 1, cat: false, top: [150, 196, 236], bottom: [80, 130, 200] },
        { id: 'centrifuge', in: 0, out: 1, fIn: 1, fOut: 1, cat: false, top: [232, 226, 210], bottom: [180, 170, 146] },
        { id: 'filter_press', in: 0, out: 1, fIn: 1, fOut: 1, cat: false, top: [210, 200, 180], bottom: [150, 138, 116] },
        { id: 'membrane_cell', in: 0, out: 0, fIn: 1, fOut: 3, cat: false, top: [226, 240, 220], bottom: [170, 200, 160] },
        { id: 'wet_mill', in: 1, out: 1, fIn: 1, fOut: 1, cat: false, top: [240, 236, 220], bottom: [206, 196, 168] },
        { id: 'gas_compressor', in: 0, out: 0, fIn: 1, fOut: 1, cat: false, top: [236, 236, 236], bottom: [196, 200, 206] },
        { id: 'strand_pelletizer', in: 1, out: 1, fIn: 1, fOut: 0, cat: false, top: [150, 196, 236], bottom: [90, 140, 200] },
        { id: 'sheeting_press', in: 1, out: 1, fIn: 0, fOut: 1, cat: false, top: [236, 230, 214], bottom: [196, 186, 160] },
    ];
    function vesselBack(s) {
        const c = panelCanvas(), { R, body } = c, { h } = pipes(R);
        if (s.fIn) drums(c, 17, 17 + 15 * s.fIn);
        const inner = 149 - 15 * (s.fOut - 1);
        if (s.fOut) drums(c, inner, 164);
        body(54, 35, 113, 70);
        for (const y of [46, 54, 62]) { R(55, y, 56, y, WATER_L); R(111, y, 112, y, WATER_L); }
        cutaway(c, 58, 40, 109, 66, [30, 30, 32]);
        const ports = [63, 101].slice(0, s.in);
        if (s.cat) ports.push(82);
        for (const cx of ports) { R(cx - 1, 33, cx + 2, 34, METAL_L); R(cx, 31, cx + 1, 32, METAL_D); }
        body(58, 71, 62, 74); body(105, 71, 109, 74);
        if (s.fIn) h(18 + 15 * s.fIn, 53, 60);
        if (s.fOut) h(114, inner - 1, 44);
        if (s.out) h(114, 114, 64);
        return c.g;
    }
    function vesselFill(s) {
        const g = new G(52, 27), r = rng(s.top[2] * 3 + s.bottom[1]);
        for (let y = 0; y < 27; y++) for (let x = 0; x < 52; x++) {
            const col = mix(s.top, s.bottom, y / 26);
            g.px(x, y, y === 0 ? light(s.top, 0.4) : r() < 0.08 ? dark(col, 0.08) : col);
        }
        return g;
    }

    // ---- the mixer series: machines that take only items. The feeds stand left of a mixing chamber
    // (one column, or two once there are more than two), each row on a conveyor into it; the products
    // leave on conveyors to the right, the mould sits under the feed. Inside the chamber the charge
    // builds up while rotors, a knife rotor, warm air or the injection ram work it.
    // ProcessSchematic.mixer works out the same positions from the same counts.
    const MIXERS = [
        { id: 'compounding_extruder', in: 3, out: 1, cat: false, kind: 'rotor', top: [222, 210, 180], bottom: [170, 150, 110] },
        { id: 'dehumidifying_dryer', in: 1, out: 1, cat: false, kind: 'air', top: [240, 236, 222], bottom: [206, 198, 176] },
        { id: 'high_speed_mixer', in: 4, out: 1, cat: false, kind: 'blade', top: [236, 236, 228], bottom: [196, 196, 186] },
        { id: 'injection_molder', in: 1, out: 2, cat: true, kind: 'ram', top: [250, 180, 90], bottom: [210, 110, 50] },
        { id: 'plastic_granulator', in: 1, out: 1, cat: false, kind: 'blade', top: [210, 220, 230], bottom: [150, 166, 182] },
        { id: 'rubber_mixer', in: 3, out: 1, cat: false, kind: 'rotor', top: [96, 92, 88], bottom: [52, 50, 48] },
    ];
    const mixerIns = s => {
        const xs = s.in > 2 ? [22, 42] : [32], ys = s.in === 1 ? [32] : [22, 42], at = [];
        for (let i = 0; i < s.in; i++) at.push([xs[i % xs.length], ys[Math.floor(i / xs.length)]]);
        return at;
    };
    function mixerBack(s) {
        const c = panelCanvas(), { R, body, chute } = c;
        body(64, 20, 111, 56);
        body(64, 57, 68, 74); body(107, 57, 111, 74);
        cutaway(c, 68, 24, 107, 51, [30, 30, 32]);
        if (s.kind === 'rotor' || s.kind === 'blade') { body(80, 17, 95, 19); R(87, 16, 88, 16, VD); }
        if (s.kind === 'air') for (let x = 70; x <= 104; x += 5) R(x, 53, x + 2, 55, VD);
        if (s.kind === 'ram') { body(57, 30, 63, 46); R(58, 37, 62, 38, VD); }
        const rows = new Map();
        for (const [x, y] of mixerIns(s)) rows.set(y, Math.max(rows.get(y) || 0, x));
        for (const [y, x] of rows) chute(x + 17, s.kind === 'ram' ? 56 : 63, y + 8);
        const outYs = s.out > 1 ? [24, 44] : [40];
        for (let i = 0; i < s.out; i++) chute(112, 134, outYs[i] + 8);
        if (s.cat) { R(49, 62, 63, 62, VD); R(49, 63, 63, 63, METAL_D); }
        return c.g;
    }
    function mixerFill(s) {
        const g = new G(40, 28), r = rng(s.top[0] * 7 + s.bottom[2]);
        for (let y = 0; y < 28; y++) for (let x = 0; x < 40; x++) {
            const col = mix(s.top, s.bottom, y / 27);
            g.px(x, y, y === 0 ? light(s.top, 0.4) : r() < 0.08 ? dark(col, 0.08) : col);
        }
        return g;
    }
    function mixerMove(s, f) {
        const g = new G(40, 28);
        if (s.kind === 'rotor') { roller(g, 11, 14, 8, f * 0.8); roller(g, 28, 14, 8, -f * 0.8); }
        else if (s.kind === 'blade') roller(g, 20, 14, 10, f * 1.2);
        else if (s.kind === 'air') {
            [4, 11, 18, 25, 32, 37].forEach((bx, i) => {
                for (let j = 0; j < 2; j++) {
                    const y = 27 - ((f * 3 + j * 11 + i * 5) % 22);
                    if (y >= 4) g.px(bx, y, [255, 240, 210], 200);
                }
            });
        } else {
            const x = 1 + f * 2;
            g.rect(0, 12, x - 1, 15, METAL_D);
            g.rect(x, 0, x + 2, 27, METAL_L);
        }
        return g;
    }

    function paintSchematics() {
        const S = 'gui/process/schematic/';
        return [
            save(extruderBack(), S + 'single_screw_extruder.png'),
            save(sheet([extruderMelt(), ...rowsOf(6, extruderScrew)]), S + 'single_screw_extruder_progress.png'),
            save(coolingBack(), S + 'cooling_tank.png'),
            save(sheet([coolingProduct(), ...rowsOf(4, coolingBubbles)]), S + 'cooling_tank_progress.png'),
            save(vulcBack(), S + 'vulcanizer.png'),
            save(sheet(rowsOf(8, vulcPhase)), S + 'vulcanizer_progress.png'),
            save(smokeBack(), S + 'smokehouse.png'),
            save(sheet([smokeFill(), ...rowsOf(4, smokeFire)]), S + 'smokehouse_progress.png'),
            save(winderBack(), S + 'film_winder.png'),
            save(sheet(rowsOf(8, winderPhase)), S + 'film_winder_progress.png'),
            save(hauloffBack(), S + 'haul_off_cutter.png'),
            save(sheet([hauloffPipe(), ...rowsOf(4, hauloffMove)]), S + 'haul_off_cutter_progress.png'),
            save(cncBack(), S + 'cnc_machining_center.png'),
            save(sheet(rowsOf(8, cncPhase)), S + 'cnc_machining_center_progress.png'),
            save(polyBack(), S + 'polymerization_reactor.png'),
            save(sheet([polySlurry(), ...rowsOf(4, polyStir)]), S + 'polymerization_reactor_progress.png'),
            save(distBack(), S + 'distillation_column.png'),
            save(sheet([distVapour(), ...rowsOf(4, distBubble)]), S + 'distillation_column_progress.png'),
            save(kilnBack(), S + 'lime_kiln.png'),
            save(sheet([stones([238, 236, 228]), ...rowsOf(4, kilnFlame)]), S + 'lime_kiln_progress.png'),
            save(fermBack(), S + 'fermenter.png'),
            save(sheet([fermBroth(), ...rowsOf(4, fermBubbles)]), S + 'fermenter_progress.png'),
            ...REACTORS.flatMap(s => [
                save(reactorBack(s), S + s.id + '.png'),
                save(sheet([reactorFill(s), ...rowsOf(4, s.stir ? polyStir : reactorBubbles)]), S + s.id + '_progress.png'),
            ]),
            ...COLUMNS.flatMap(s => [
                save(columnBack(s), S + s.id + '.png'),
                save(sheet([columnFill(s), ...rowsOf(4, distBubble)]), S + s.id + '_progress.png'),
            ]),
            ...KILNS.flatMap(s => [
                save(kilnSeriesBack(s), S + s.id + '.png'),
                save(sheet([s.gas ? kilnGas() : stones(s.done), ...rowsOf(4, kilnFlame)]), S + s.id + '_progress.png'),
            ]),
            ...VESSELS.flatMap(s => [
                save(vesselBack(s), S + s.id + '.png'),
                save(sheet([vesselFill(s), ...rowsOf(4, fermBubbles)]), S + s.id + '_progress.png'),
            ]),
            ...MIXERS.flatMap(s => [
                save(mixerBack(s), S + s.id + '.png'),
                save(sheet([mixerFill(s), ...rowsOf(4, f => mixerMove(s, f))]), S + s.id + '_progress.png'),
            ]),
        ].join(', ');
    }

    function paintAll() {
        const out = [];
        for (const name in PARTS) { const [g, meta] = PARTS[name](); out.push(save(g, 'gui/' + name + '.png', meta)); }
        for (const name in PROGRESS) out.push(save(sheet(PROGRESS[name]()), 'gui/process/progress/' + name + '.png'));
        return out.length + ' textures';
    }

    window.IUCEGui = { G, PARTS, PROGRESS, paintAll, paintSchematics };
    return 'gui loaded';
})();
