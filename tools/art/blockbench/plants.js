// Formed large-machine models in the steel chemical-plant style, built inside Blockbench:
//   eval(requireNativeModule('fs').readFileSync('D:/Code/java/IUCE/tools/art/blockbench/core.js', 'utf8'))
//   eval(requireNativeModule('fs').readFileSync('D:/Code/java/IUCE/tools/art/blockbench/plants.js', 'utf8'))
//   IUCEPlants.build('carbide_furnace_plant')
// Opens the model as a Blockbench project, then writes tools/models/<name>.bbmodel and its textures
// to tools/models/<name>_textures; run tools/export_plant_model.py <name> afterwards.
// Units are Blockbench pixels: the controller block spans 0..16 on every axis and the machine
// runs towards +z, across x. Everything is a mesh: flat surfaces are cut on a world-aligned grid
// the size of their texture, so seamless textures run on across neighbouring parts at one texel
// per pixel; round parts are swept along their centre line with smooth normals.
(function () {
    const A = window.IUCEArt;
    const { mix, light, dark, rng, HOT, COPPER } = A;
    const MODELS = A.ROOT + 'tools/models/';

    // ---- Textures of any size; drawing wraps around the edges so seamless patterns are easy.
    class Tex {
        constructor(w, h) { this.w = w; this.h = h; this.d = new Uint8ClampedArray(w * h * 4); }
        i(x, y) { x = ((Math.round(x) % this.w) + this.w) % this.w; y = ((Math.round(y) % this.h) + this.h) % this.h; return (y * this.w + x) * 4; }
        px(x, y, c, a) {
            if (!c) return;
            const i = this.i(x, y);
            this.d[i] = c[0]; this.d[i + 1] = c[1]; this.d[i + 2] = c[2];
            this.d[i + 3] = a === undefined ? (c[3] === undefined ? 255 : c[3]) : a;
        }
        get(x, y) { const i = this.i(x, y); return [this.d[i], this.d[i + 1], this.d[i + 2], this.d[i + 3]]; }
        rect(x0, y0, x1, y1, c, a) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) this.px(x, y, c, a); }
        tint(x, y, c, t) { const p = this.get(x, y); if (p[3]) this.px(x, y, mix(p, c, t)); }
        each(f) { for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) f(x, y); }
        dataURL() {
            const c = document.createElement('canvas'); c.width = this.w; c.height = this.h;
            const ctx = c.getContext('2d'); const id = ctx.createImageData(this.w, this.h);
            id.data.set(this.d); ctx.putImageData(id, 0, 0); return c.toDataURL('image/png');
        }
    }

    // Tileable value noise: lattice cells must divide the texture size.
    function lattice(w, h, cx, cy, seed) {
        const r = rng(seed), nx = Math.max(1, w / cx), ny = Math.max(1, h / cy), g = [];
        for (let i = 0; i < nx * ny; i++) g.push(r());
        const G = (i, j) => g[(((j % ny) + ny) % ny) * nx + (((i % nx) + nx) % nx)];
        return (x, y) => {
            const fx = x / cx, fy = y / cy, x0 = Math.floor(fx), y0 = Math.floor(fy);
            const tx = fx - x0, ty = fy - y0, sx = tx * tx * (3 - 2 * tx), sy = ty * ty * (3 - 2 * ty);
            const a = G(x0, y0) + (G(x0 + 1, y0) - G(x0, y0)) * sx;
            const b = G(x0, y0 + 1) + (G(x0 + 1, y0 + 1) - G(x0, y0 + 1)) * sx;
            return a + (b - a) * sy;
        };
    }
    function fbm(w, h, seed, cells) {
        const ls = cells.map((c, i) => [lattice(w, h, Math.min(c[0], w), Math.min(c[1], h), seed + i * 31), 1 / (i + 1)]);
        const total = ls.reduce((a, l) => a + l[1], 0);
        return (x, y) => ls.reduce((a, l) => a + l[0](x, y) * l[1], 0) / total;
    }

    const C = {
        steel: [[44, 48, 54], [68, 72, 78], [112, 117, 123], [152, 157, 162], [184, 189, 194], [206, 210, 214]],
        frame: [[40, 46, 54], [62, 70, 80], [86, 96, 108], [112, 122, 134], [140, 150, 162]],
        blue: [60, 112, 190], orange: [222, 128, 40], yellow: [208, 170, 56], water: [58, 104, 168],
        green: [96, 210, 120], red: [214, 64, 52], dark: [22, 24, 28],
    };

    // Brushed steel: soft blotches plus fine streaks along one axis.
    function steel(t, seed, lo, hi, streakVertical) {
        const blot = fbm(t.w, t.h, seed, [[16, 16], [8, 8], [4, 4]]);
        const streak = streakVertical ? lattice(t.w, t.h, 1, Math.min(16, t.h), seed + 5) : lattice(t.w, t.h, Math.min(16, t.w), 1, seed + 5);
        t.each((x, y) => t.px(x, y, mix(lo, hi, 0.15 + 0.7 * blot(x, y) + 0.25 * (streak(x, y) - 0.5))));
    }
    // A weld seam: raised bead with a light top edge and a dark shadow, horizontal or vertical.
    function weldH(t, y, x0, x1) { for (let x = x0; x <= x1; x++) { t.tint(x, y - 1, [255, 255, 255], 0.18); t.px(x, y, mix(C.steel[1], C.steel[2], (x * 7 % 5) / 5)); t.tint(x, y + 1, [0, 0, 0], 0.3); } }
    function weldV(t, x, y0, y1) { for (let y = y0; y <= y1; y++) { t.tint(x - 1, y, [255, 255, 255], 0.18); t.px(x, y, mix(C.steel[1], C.steel[2], (y * 7 % 5) / 5)); t.tint(x + 1, y, [0, 0, 0], 0.3); } }
    function rivet(t, x, y) { t.px(x, y, C.steel[5]); t.px(x + 1, y, C.steel[2]); t.px(x, y + 1, C.steel[2]); t.px(x + 1, y + 1, C.steel[0]); }
    function grime(t, seed, c, amount, fromBottom) {
        const n = fbm(t.w, t.h, seed, [[8, 8], [4, 4], [2, 2]]);
        t.each((x, y) => { const g = n(x, y) * amount * (fromBottom ? 0.4 + 0.6 * y / t.h : 1); t.tint(x, y, c, Math.max(0, g - 0.1)); });
    }
    function icon(t, rows, x0, y0, c) { rows.forEach((row, y) => [...row].forEach((ch, x) => { if (ch === '#') t.px(x0 + x, y0 + y, c); else if (ch === '+') t.px(x0 + x, y0 + y, dark(c, 0.45)); })); }

    const ARROW_IN = ['..#..', '..#..', '#.#.#', '.###.', '..#..'];
    const ARROW_OUT = ['..#..', '.###.', '#.#.#', '..#..', '..#..'];

    const TEXTURES = {
        // Checker plate for the skid top: one big welded panel every four blocks.
        deck: [64, 64, t => {
            steel(t, 3, C.steel[2], C.steel[4], false);
            for (let y = 0; y < 64; y += 4) for (let x = 0; x < 64; x += 4) {
                const o = (y >> 2) & 1 ? 2 : 0;
                if ((y >> 2) & 1) { t.px(x + o, y + 1, C.steel[5]); t.px(x + o + 1, y + 1, C.steel[1]); }
                else { t.px(x + 1, y, C.steel[5]); t.px(x + 1, y + 1, C.steel[1]); }
            }
            weldH(t, 0, 0, 63); weldV(t, 0, 0, 63);
            grime(t, 4, [60, 52, 44], 0.35, false);
        }],
        // Skid side: a rolled channel with a lip at top and bottom.
        skid: [64, 16, t => {
            steel(t, 5, C.frame[2], C.frame[4], false);
            t.rect(0, 0, 63, 0, C.frame[4]); t.rect(0, 1, 63, 1, light(C.frame[4], 0.25)); t.rect(0, 2, 63, 2, C.frame[1]);
            t.rect(0, 13, 63, 13, C.frame[4]); t.rect(0, 14, 63, 14, C.frame[1]); t.rect(0, 15, 63, 15, C.frame[0]);
            for (let x = 0; x < 64; x += 16) { rivet(t, x + 3, 5); rivet(t, x + 3, 9); weldV(t, x + 15, 3, 12); }
            grime(t, 6, [50, 44, 38], 0.5, true);
        }],
        // Furnace shell: rolled plates, vertical and horizontal butt welds, heat tint near the top.
        shell: [64, 80, t => {
            steel(t, 7, C.steel[2], C.steel[5], true);
            for (const x of [0, 32]) weldV(t, x, 0, 79);
            for (const y of [26, 53]) weldH(t, y, 0, 63);
            t.each((x, y) => { if (y < 14) t.tint(x, y, [150, 112, 84], (14 - y) / 40); });
            for (let x = 4; x < 64; x += 8) { rivet(t, x, 2); rivet(t, x, 76); }
            grime(t, 8, [70, 60, 50], 0.3, true);
        }],
        // Hood: darker heat-treated plate with radial ribs.
        hood: [64, 48, t => {
            steel(t, 9, C.steel[1], C.steel[3], true);
            for (let x = 0; x < 64; x += 16) { t.rect(x, 0, x, 47, C.steel[4]); t.rect(x + 1, 0, x + 1, 47, C.steel[3]); t.rect(x + 2, 0, x + 2, 47, C.steel[0]); }
            t.each((x, y) => t.tint(x, y, [130, 96, 70], 0.12 + 0.12 * Math.sin(x / 64 * Math.PI * 4) ** 2));
            weldH(t, 24, 0, 63);
        }],
        // Roof lid seen from above, projected straight down onto the round cap.
        roof: [80, 80, t => {
            const n = fbm(80, 80, 11, [[16, 16], [8, 8], [4, 4]]);
            t.each((x, y) => {
                const r = Math.hypot(x - 39.5, y - 39.5);
                let c = mix(C.steel[2], C.steel[4], 0.2 + 0.6 * n(x, y));
                if (Math.abs(r - 30) < 0.8 || Math.abs(r - 15) < 0.8) c = C.steel[1];
                if (Math.abs(r - 31) < 0.6 || Math.abs(r - 16) < 0.6) c = C.steel[5];
                if (r > 37) c = mix(c, C.steel[1], 0.4);
                t.px(x, y, c);
            });
            for (let i = 0; i < 24; i++) { const a = i / 24 * Math.PI * 2; rivet(t, 39.5 + Math.cos(a) * 34, 39.5 + Math.sin(a) * 34); }
        }],
        // Painted structural steel for the frame, decks' beams and stairs.
        frame: [64, 64, t => { steel(t, 13, C.frame[2], C.frame[4], false); grime(t, 14, [40, 36, 32], 0.3, false); }],
        grating: [32, 32, t => {
            t.each((x, y) => {
                const bar = x % 4 === 0, rung = y % 8 === 0;
                if (bar) t.px(x, y, rung ? C.frame[4] : C.frame[3]);
                else if (rung) t.px(x, y, C.frame[2]);
                else if (x % 4 === 1) t.px(x, y, C.frame[1]);
                else t.px(x, y, [0, 0, 0], 0);
            });
        }],
        pipe: [32, 32, t => { steel(t, 15, C.steel[2], C.steel[4], false); weldH(t, 0, 0, 31); }],
        water: [32, 32, t => { steel(t, 16, dark(C.water, 0.25), light(C.water, 0.25), false); weldH(t, 0, 0, 31); grime(t, 17, [30, 40, 50], 0.25, false); }],
        gas: [32, 32, t => { steel(t, 18, dark(C.yellow, 0.22), light(C.yellow, 0.2), false); weldH(t, 0, 0, 31); grime(t, 19, [60, 50, 30], 0.3, false); }],
        // Aluminium cladding over flue insulation: overlapping sheets with screw rows.
        orange: [32, 32, t => { steel(t, 33, dark(C.orange, 0.2), light(C.orange, 0.15), false); grime(t, 34, [60, 40, 20], 0.3, false); }],
        blue: [32, 32, t => { steel(t, 35, dark(C.blue, 0.2), light(C.blue, 0.15), false); grime(t, 36, [20, 30, 50], 0.3, false); }],
        insul: [32, 32, t => {
            steel(t, 20, [170, 176, 182], [222, 226, 230], true);
            t.rect(0, 0, 31, 0, [236, 238, 240]); t.rect(0, 1, 31, 1, [128, 134, 140]);
            for (let x = 2; x < 32; x += 8) t.px(x, 3, [110, 114, 120]);
        }],
        brick: [32, 32, t => {
            const n = fbm(32, 32, 21, [[8, 8], [4, 4], [2, 2]]);
            t.each((x, y) => {
                const row = y >> 3, joint = y % 8 === 0 || (x + (row & 1 ? 8 : 0)) % 16 === 0;
                t.px(x, y, joint ? [96, 84, 76] : mix([150, 118, 92], [196, 160, 124], n(x, y)));
            });
        }],
        graphite: [32, 32, t => {
            const n = fbm(32, 32, 22, [[8, 16], [4, 8], [2, 2]]);
            t.each((x, y) => t.px(x, y, mix([34, 36, 40], [84, 86, 92], n(x, y))));
            for (let x = 0; x < 32; x++) { t.px(x, 0, [24, 24, 28]); t.px(x, 1, [102, 104, 110]); }
        }],
        copper: [32, 32, t => {
            const n = fbm(32, 32, 23, [[16, 16], [8, 8], [2, 2]]);
            t.each((x, y) => t.px(x, y, mix(COPPER[1], COPPER[3], 0.1 + 0.8 * n(x, y))));
            grime(t, 24, [70, 120, 100], 0.3, false);
        }],
        glow: [32, 32, t => {
            const n = fbm(32, 32, 25, [[8, 8], [4, 4], [2, 2]]);
            t.each((x, y) => { const v = n(x, y); t.px(x, y, v > 0.62 ? HOT[0] : v > 0.48 ? HOT[1] : v > 0.36 ? HOT[2] : HOT[3]); });
        }],
        // Transformer tank paint and its radiator panels.
        tank: [64, 64, t => { steel(t, 26, [92, 104, 98], [132, 146, 138], false); weldH(t, 0, 0, 63); weldV(t, 0, 0, 63); grime(t, 27, [40, 40, 36], 0.3, true); }],
        ceramic: [32, 16, t => { const n = fbm(32, 16, 28, [[8, 8], [4, 4]]); t.each((x, y) => t.px(x, y, mix([132, 72, 44], [188, 112, 70], n(x, y) + (y < 4 ? 0.3 : 0)))); }],
        rubber: [32, 32, t => { const n = fbm(32, 32, 29, [[16, 4], [4, 2]]); t.each((x, y) => t.px(x, y, mix([24, 24, 28], [56, 56, 62], n(x, y)))); }],
        cavity: [16, 16, t => t.each((x, y) => t.px(x, y, mix([14, 15, 18], [34, 36, 40], y / 15)))],

        // ---- Ports. Item ports are square hatches with chevrons, fluid ports round flanges with a drop;
        // blue means "goes in", orange means "comes out", and the arrows point that way.
        item_in: [16, 16, t => {
            t.rect(0, 0, 15, 15, C.steel[2]); t.rect(0, 0, 15, 0, C.steel[4]); t.rect(0, 15, 15, 15, C.steel[0]);
            t.rect(2, 2, 13, 13, C.blue); t.rect(4, 4, 11, 11, C.dark);
            for (const [x, y, dx, dy] of [[7, 2, 0, 1], [7, 12, 0, -1], [2, 7, 1, 0], [12, 7, -1, 0]]) {
                const w = dx ? [0, 1] : [0, 1];
                for (let i = 0; i < 2; i++) { t.px(x + dx * i + (dy ? w[0] : 0), y + dy * i + (dx ? w[0] : 0), [235, 242, 250]); t.px(x + dx * i + (dy ? w[1] : 0), y + dy * i + (dx ? w[1] : 0), [235, 242, 250]); }
            }
            icon(t, ['.####.', '#+..+#', '#....#', '#+..+#', '.####.'], 5, 5, C.steel[3]);
        }],
        item_out: [16, 16, t => {
            t.rect(0, 0, 15, 15, C.steel[2]); t.rect(0, 0, 15, 0, C.steel[4]); t.rect(0, 15, 15, 15, C.steel[0]);
            t.rect(2, 2, 13, 13, C.orange); t.rect(4, 4, 11, 11, C.dark);
            for (const [x, y] of [[7, 0], [7, 14], [0, 7], [14, 7]]) t.rect(x, y, x + 1, y + 1, [255, 236, 200]);
            icon(t, ARROW_OUT, 5, 5, [255, 214, 160]);
        }],
        // Labels above the ports, so even from a distance it is clear which hatch is which.
        tag_item_in: [16, 8, t => { t.rect(0, 0, 15, 7, C.blue); t.rect(0, 7, 15, 7, dark(C.blue, 0.4)); icon(t, ['##.....#', '#+#...#.', '###..##.', '#+#.####', '##.....#'].map(r => r), 1, 1, [240, 244, 250]); }],
        tag_item_out: [16, 8, t => { t.rect(0, 0, 15, 7, C.orange); t.rect(0, 7, 15, 7, dark(C.orange, 0.4)); icon(t, ['##..#...', '#+#.##..', '###.###.', '#+#.##..', '##..#...'], 1, 1, [255, 246, 232]); }],
        tag_fluid_in: [16, 8, t => { t.rect(0, 0, 15, 7, C.blue); t.rect(0, 7, 15, 7, dark(C.blue, 0.4)); icon(t, ['..#....#', '.###...#', '#####.##', '#####..#', '.###...#'], 1, 1, [240, 244, 250]); }],
        tag_fluid_out: [16, 8, t => { t.rect(0, 0, 15, 7, C.orange); t.rect(0, 7, 15, 7, dark(C.orange, 0.4)); icon(t, ['..#.#...', '.###.#..', '#####.#.', '#####.#.', '.###.#..'], 1, 1, [255, 246, 232]); }],
        flange_in: [16, 16, t => {
            t.each((x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); t.px(x, y, r > 7.6 ? C.steel[1] : r > 5.2 ? C.steel[3] : r > 3.8 ? C.blue : C.dark); });
            for (let i = 0; i < 8; i++) { const a = i / 8 * Math.PI * 2; t.px(7.5 + Math.cos(a) * 6.4, 7.5 + Math.sin(a) * 6.4, C.steel[5]); }
            icon(t, ['.#.', '###', '.#.'], 6, 6, [120, 170, 240]);
        }],
        flange_out: [16, 16, t => {
            t.each((x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); t.px(x, y, r > 7.6 ? C.steel[1] : r > 5.2 ? C.steel[3] : r > 3.8 ? C.orange : C.dark); });
            for (let i = 0; i < 8; i++) { const a = i / 8 * Math.PI * 2; t.px(7.5 + Math.cos(a) * 6.4, 7.5 + Math.sin(a) * 6.4, C.steel[5]); }
        }],
        // Control desk: screen, lamps and push buttons on a dark bezel.
        panel: [16, 16, t => {
            t.rect(0, 0, 15, 15, C.frame[1]); t.rect(0, 0, 15, 0, C.frame[3]);
            t.rect(1, 1, 14, 8, C.dark); t.rect(2, 2, 13, 7, [16, 44, 32]);
            for (let x = 2; x <= 13; x++) t.px(x, Math.round(5 - 2 * Math.sin(x * 0.9)), C.green);
            t.rect(2, 7, 6, 7, [70, 160, 100]);
            t.rect(2, 10, 3, 11, C.green); t.rect(5, 10, 6, 11, C.red); t.rect(8, 10, 9, 11, C.yellow);
            t.rect(11, 10, 13, 13, C.steel[1]); t.rect(12, 11, 12, 12, C.steel[4]);
            t.rect(2, 13, 9, 13, C.steel[2]);
        }],
        desk: [16, 16, t => { steel(t, 31, C.frame[2], C.frame[3], false); t.rect(0, 0, 15, 0, C.frame[4]); }],
        // Maintenance cabinet door: tool outline, hinges, a latch and louvres; nothing like the desk.
        maint: [16, 16, t => {
            t.rect(0, 0, 15, 15, C.steel[3]); t.rect(0, 0, 15, 0, C.steel[5]); t.rect(0, 0, 0, 15, C.steel[4]);
            t.rect(15, 0, 15, 15, C.steel[1]); t.rect(0, 15, 15, 15, C.steel[0]);
            t.rect(1, 2, 1, 4, C.steel[1]); t.rect(1, 11, 1, 13, C.steel[1]);
            t.rect(13, 6, 14, 9, C.steel[1]); t.px(13, 7, C.steel[5]);
            for (const y of [11, 13]) t.rect(4, y, 11, y, C.steel[1]);
            icon(t, ['.#..##', '##...#', '.#..#.', '..##..', '.#..#.', '#...##', '##..#.'], 5, 2, [240, 240, 236]);
            icon(t, ['.#..##', '##...#', '.#..#.', '..##..', '.#..#.', '#...##', '##..#.'].map(r => r.replace(/#/g, '.')), 5, 2, C.dark);
        }],
        socket: [16, 16, t => {
            t.rect(0, 0, 15, 15, C.frame[2]); t.rect(0, 0, 15, 0, C.frame[4]); t.rect(0, 15, 15, 15, C.frame[0]);
            t.rect(2, 2, 13, 13, C.dark);
            icon(t, ['...##', '..##.', '.####', '..##.', '.##..', '##...'], 5, 4, [250, 210, 70]);
            for (const [x, y] of [[1, 1], [13, 1], [1, 13], [13, 13]]) rivet(t, x, y);
        }],
        cabinet: [32, 32, t => { steel(t, 32, C.steel[2], C.steel[4], false); weldV(t, 0, 0, 31); weldH(t, 0, 0, 31); for (let y = 4; y < 32; y += 8) rivet(t, 2, y); }],
        gauge: [8, 8, t => { t.each((x, y) => { const r = Math.hypot(x - 3.5, y - 3.5); t.px(x, y, r > 3.6 ? C.steel[1] : [236, 236, 228]); }); t.px(3, 3, C.red); t.px(4, 2, C.red); t.px(5, 1, C.red); }],
        flange_gas: [16, 16, t => {
            t.each((x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); t.px(x, y, r > 7.6 ? C.steel[1] : r > 5.2 ? C.steel[3] : r > 3.8 ? C.yellow : C.dark); });
            for (let i = 0; i < 8; i++) { const a = i / 8 * Math.PI * 2; t.px(7.5 + Math.cos(a) * 6.4, 7.5 + Math.sin(a) * 6.4, C.steel[5]); }
        }],
        // Stainless vessel shell: bright plate, circumferential and long seams.
        vessel: [64, 64, t => {
            steel(t, 41, [168, 174, 178], [216, 220, 222], true);
            weldH(t, 0, 0, 63); weldH(t, 32, 0, 63); weldV(t, 0, 0, 63);
            grime(t, 42, [90, 80, 70], 0.2, true);
        }],
        // Coke oven door: heavy dark plate in a frame, heat-browned towards the middle.
        door: [32, 32, t => {
            steel(t, 43, C.frame[0], C.frame[2], true);
            t.each((x, y) => t.tint(x, y, [110, 70, 40], 0.25 * Math.sin(y / 32 * Math.PI)));
            t.rect(0, 0, 31, 0, C.frame[3]); t.rect(0, 0, 0, 31, C.frame[3]);
            for (let y = 4; y < 32; y += 8) rivet(t, 3, y);
        }],
        // Polished roll and strip surfaces: bright with streaks along the roll axis.
        roll: [32, 32, t => { steel(t, 45, C.steel[3], C.steel[5], false); t.rect(0, 0, 31, 0, C.steel[2]); }],
        coil: [32, 32, t => {
            steel(t, 46, C.steel[2], C.steel[4], false);
            for (let y = 0; y < 32; y += 4) t.rect(0, y, 31, y, C.steel[1]);
        }],
        coil_end: [32, 32, t => t.each((x, y) => {
            const r = Math.hypot(x - 15.5, y - 15.5);
            t.px(x, y, r < 5 ? C.dark : Math.floor(r) % 2 ? C.steel[4] : C.steel[2]);
        })],
        // Perforated trommel screen plate.
        screen: [32, 32, t => {
            steel(t, 47, C.steel[1], C.steel[3], false);
            for (let y = 1; y < 32; y += 4) for (let x = (y >> 2) % 2 ? 3 : 1; x < 32; x += 4) { t.px(x, y, C.dark); t.px(x + 1, y, C.dark); t.px(x, y + 1, C.dark); t.px(x + 1, y + 1, [40, 42, 46]); }
        }],
    };

    // ---- Geometry. Each mesh collects vertices (deduplicated by position) and faces.
    const add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
    const sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
    const mul = (a, s) => [a[0] * s, a[1] * s, a[2] * s];
    const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
    const norm = a => { const l = Math.hypot(a[0], a[1], a[2]) || 1; return [a[0] / l, a[1] / l, a[2] / l]; };

    class Shape {
        constructor(name, smooth) { this.name = name; this.smooth = smooth; this.verts = {}; this.faces = []; this.n = 0; }
        v(p) {
            const key = p.map(c => Math.round(c * 1000)).join(',');
            if (!this.verts[key]) this.verts[key] = { id: 'v' + (this.n++), p: p.map(c => Math.round(c * 1000) / 1000) };
            return this.verts[key].id;
        }
        face(points, tex, uvs) {
            const ids = [], keep = [];
            points.forEach((p, i) => { const id = this.v(p); if (!ids.includes(id)) { ids.push(id); keep.push(uvs[i]); } });
            if (ids.length >= 3) this.faces.push({ ids, tex, uvs: keep });
            return this;
        }
    }

    const scene = [];
    // Outliner groups the shapes being drawn belong to: stretch sections (bay, stand, tail) and
    // spinners, whose group origin is the pivot they turn about.
    const groupPath = [];
    function shape(name, smooth) { const s = new Shape(name, smooth); s.path = groupPath.slice(); scene.push(s); return s; }
    function within(name, origin, draw) { groupPath.push({ name, origin: origin || [0, 0, 0] }); try { draw(); } finally { groupPath.pop(); } }

    // A flat rectangle p + U*a + V*b (a in 0..la, b in 0..lb), normal U x V. Cut on the world grid of
    // the texture size along world axes so the pattern continues across faces.
    function plane(s, p, U, la, V, lb, tex, fixed) {
        const [W, H] = TEXTURES[tex];
        if (fixed) {
            const q = [p, add(p, mul(U, la)), add(add(p, mul(U, la)), mul(V, lb)), add(p, mul(V, lb))];
            const [u0, v0, u1, v1] = fixed === true ? [0, 0, W, H] : fixed;
            return s.face(q, tex, [[u0, v1], [u1, v1], [u1, v0], [u0, v0]]);
        }
        const su = dot(p, U), sv = dot(p, V);
        const cuts = (start, len, period) => {
            const out = [0];
            for (let k = Math.floor(start / period) + 1; k * period < start + len - 1e-6; k++) if (k * period > start + 1e-6) out.push(k * period - start);
            out.push(len);
            return out;
        };
        const ca = cuts(su, la, W), cb = cuts(sv, lb, H);
        for (let i = 0; i + 1 < ca.length; i++) for (let j = 0; j + 1 < cb.length; j++) {
            const a0 = ca[i], a1 = ca[i + 1], b0 = cb[j], b1 = cb[j + 1];
            const ku = Math.floor((su + a0 + 1e-6) / W) * W, kv = Math.floor((sv + b0 + 1e-6) / H) * H;
            const u0 = su + a0 - ku, u1 = su + a1 - ku, w0 = H - (sv + b0 - kv), w1 = H - (sv + b1 - kv);
            const q = [add(add(p, mul(U, a0)), mul(V, b0)), add(add(p, mul(U, a1)), mul(V, b0)), add(add(p, mul(U, a1)), mul(V, b1)), add(add(p, mul(U, a0)), mul(V, b1))];
            s.face(q, tex, [[u0, w0], [u1, w0], [u1, w1], [u0, w1]]);
        }
        return s;
    }

    const FACE = {
        north: { n: [0, 0, -1], U: [-1, 0, 0], V: [0, 1, 0] }, south: { n: [0, 0, 1], U: [1, 0, 0], V: [0, 1, 0] },
        east: { n: [1, 0, 0], U: [0, 0, -1], V: [0, 1, 0] }, west: { n: [-1, 0, 0], U: [0, 0, 1], V: [0, 1, 0] },
        up: { n: [0, 1, 0], U: [1, 0, 0], V: [0, 0, -1] }, down: { n: [0, -1, 0], U: [1, 0, 0], V: [0, 0, 1] },
    };
    // Axis-aligned box. tex is a texture name or {all, up, north, ...}; skip lists faces left out.
    function box(s, a, b, tex, skip, fixed) {
        const lo = [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])], hi = [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])];
        const size = sub(hi, lo);
        for (const name in FACE) {
            if (skip && skip.includes(name)) continue;
            const f = FACE[name], t = typeof tex === 'string' ? tex : (tex[name] || tex.all);
            if (!t) continue;
            const la = Math.abs(dot(size, f.U)), lb = Math.abs(dot(size, f.V));
            let p = [0, 1, 2].map(i => f.n[i] > 0 ? hi[i] : lo[i]);
            for (let i = 0; i < 3; i++) if (f.n[i] === 0) p[i] = (f.U[i] < 0 || f.V[i] < 0) ? hi[i] : lo[i];
            plane(s, p, f.U, la, f.V, lb, t, fixed && fixed[name]);
        }
        return s;
    }

    // Centre line with rounded corners: each interior corner becomes an arc of the given radius.
    function fillet(points, radius, steps) {
        if (!radius || points.length < 3) return points.slice();
        const out = [points[0]];
        for (let i = 1; i + 1 < points.length; i++) {
            const a = points[i - 1], b = points[i], c = points[i + 1];
            const d0 = norm(sub(a, b)), d1 = norm(sub(c, b));
            const ang = Math.acos(Math.max(-1, Math.min(1, dot(d0, d1))));
            if (ang > Math.PI - 1e-3) { out.push(b); continue; }
            const r = Math.min(radius, Math.hypot(...sub(a, b)) * 0.5 / Math.tan(ang / 2) * 0.999, Math.hypot(...sub(c, b)) * 0.5 / Math.tan(ang / 2) * 0.999);
            const t = r / Math.tan(ang / 2);
            const p0 = add(b, mul(d0, t)), p1 = add(b, mul(d1, t));
            const center = add(b, mul(norm(add(d0, d1)), r / Math.sin(ang / 2)));
            const e0 = sub(p0, center), e1 = sub(p1, center);
            const sweep = Math.PI - ang, n = steps || Math.max(3, Math.ceil(sweep / (Math.PI / 12)));
            const axis = norm(cross(e0, e1));
            for (let k = 0; k <= n; k++) {
                const th = sweep * k / n, cs = Math.cos(th), sn = Math.sin(th);
                out.push(add(center, add(mul(e0, cs), mul(cross(axis, e0), sn))));
            }
        }
        out.push(points[points.length - 1]);
        return out;
    }

    // Sweeps a circle along a centre line. radius may be a number or a function of the distance
    // travelled. The texture wraps a whole number of times around and repeats along the length.
    function sweep(name, points, radius, tex, opt) {
        opt = opt || {};
        const s = shape(name, true);
        const [W, H] = TEXTURES[tex];
        const rf = typeof radius === 'function' ? radius : () => radius;
        const seg0 = opt.seg || 16;
        const lens = [0];
        for (let i = 1; i < points.length; i++) lens.push(lens[i - 1] + Math.hypot(...sub(points[i], points[i - 1])));
        const total = lens[lens.length - 1];
        const ds = new Set(lens.map(l => Math.round(l * 1000) / 1000));
        if (!opt.noTile) for (let k = 1; k * H < total; k++) ds.add(k * H);
        (opt.extra || []).forEach(d => ds.add(d));
        const dist = [...ds].sort((a, b) => a - b);
        const at = d => {
            let i = 1; while (i < lens.length - 1 && lens[i] < d) i++;
            const t = (d - lens[i - 1]) / ((lens[i] - lens[i - 1]) || 1);
            return { p: add(points[i - 1], mul(sub(points[i], points[i - 1]), t)), i };
        };
        const tangent = i => norm(sub(points[Math.min(i + 1, points.length - 1)], points[Math.max(i - 1, 0)]));
        // Tangents at original vertices average both neighbours, so bends stay round.
        const samples = dist.map(d => {
            const { p, i } = at(d);
            const exact = lens.findIndex(l => Math.abs(l - d) < 1e-3);
            const tan = exact >= 0 ? tangent(exact) : norm(sub(points[i], points[i - 1]));
            return { p, d, tan };
        });
        const maxR = Math.max(...samples.map(q => rf(q.d)));
        const reps = Math.max(1, Math.round(2 * Math.PI * maxR / W));
        const per = Math.max(2, Math.ceil(seg0 / reps)), seg = per * reps;
        let nrm = opt.up ? norm(cross(cross(samples[0].tan, opt.up), samples[0].tan)) : null;
        if (!nrm || isNaN(nrm[0])) nrm = norm(cross(samples[0].tan, Math.abs(samples[0].tan[1]) < 0.9 ? [0, 1, 0] : [1, 0, 0]));
        const rings = [];
        let prev = samples[0].tan;
        for (const q of samples) {
            const axis = cross(prev, q.tan);
            if (Math.hypot(...axis) > 1e-6) {
                const ang = Math.acos(Math.max(-1, Math.min(1, dot(prev, q.tan)))), k = norm(axis);
                nrm = add(add(mul(nrm, Math.cos(ang)), mul(cross(k, nrm), Math.sin(ang))), mul(k, dot(k, nrm) * (1 - Math.cos(ang))));
            }
            nrm = norm(sub(nrm, mul(q.tan, dot(nrm, q.tan))));
            const bin = cross(q.tan, nrm), r = rf(q.d), ring = [];
            for (let j = 0; j < seg; j++) {
                const a = j / seg * Math.PI * 2;
                ring.push(add(q.p, add(mul(nrm, Math.cos(a) * r), mul(bin, Math.sin(a) * r))));
            }
            rings.push({ ring, d: q.d });
            prev = q.tan;
        }
        for (let i = 0; i + 1 < rings.length; i++) {
            const d0 = rings[i].d, d1 = rings[i + 1].d;
            if (d1 - d0 < 1e-4) continue;
            const kv = opt.noTile ? 0 : Math.floor((d0 + 1e-4) / H) * H;
            const scale = opt.noTile ? H / total : 1;
            const w0 = H - (d0 - kv) * scale, w1 = H - (d1 - kv) * scale;
            for (let j = 0; j < seg; j++) {
                const j1 = (j + 1) % seg, u0 = (j % per) / per * W, u1 = u0 + W / per;
                s.face([rings[i].ring[j], rings[i].ring[j1], rings[i + 1].ring[j1], rings[i + 1].ring[j]], tex, [[u0, w0], [u1, w0], [u1, w1], [u0, w1]]);
            }
        }
        const capTex = opt.cap;
        if (capTex) {
            const c = shape(name, false);
            const [CW, CH] = TEXTURES[capTex];
            for (const [end, flip] of [[0, true], [rings.length - 1, false]]) {
                if (opt.capEnds && !opt.capEnds.includes(end === 0 ? 'start' : 'end')) continue;
                const ring = rings[end].ring, center = samples[end].p, r = rf(samples[end].d);
                const tan = samples[end].tan, ax = norm(sub(ring[0], center)), ay = cross(tan, ax);
                const uv = p => { const d = sub(p, center); return [CW / 2 + dot(d, ax) / (2 * r) * CW, CH / 2 - dot(d, ay) / (2 * r) * CH]; };
                for (let j = 0; j < seg; j++) {
                    const a = ring[j], b = ring[(j + 1) % seg];
                    const tri = flip ? [center, b, a] : [center, a, b];
                    c.face(tri, capTex, tri.map(uv));
                }
            }
        }
        return s;
    }
    const tube = (name, x, z, r, y0, y1, tex, opt) => sweep(name, [[x, y0, z], [x, y1, z]], r, tex, opt);
    // A flat ring (annulus) facing up or down at height y.
    function annulus(name, x, z, r0, r1, y, up, tex, seg) {
        const s = shape(name, false), [W, H] = TEXTURES[tex];
        seg = seg || 48;
        const reps = Math.max(1, Math.round(2 * Math.PI * r1 / W)), per = Math.max(1, Math.round(seg / reps));
        seg = per * reps;
        const dv = Math.min(H, r1 - r0);
        for (let j = 0; j < seg; j++) {
            const a0 = j / seg * Math.PI * 2, a1 = (j + 1) / seg * Math.PI * 2;
            const P = (r, a) => [x + Math.cos(a) * r, y, z + Math.sin(a) * r];
            const u0 = (j % per) / per * W, u1 = u0 + W / per;
            let q = [P(r0, a0), P(r1, a0), P(r1, a1), P(r0, a1)], uv = [[u0, dv], [u0, 0], [u1, 0], [u1, dv]];
            if (r0 < 1e-6) { q = [q[0], q[1], q[2]]; uv = [[(u0 + u1) / 2, dv], uv[1], uv[2]]; }
            if (up) { q.reverse(); uv.reverse(); }
            s.face(q, tex, uv);
        }
        return s;
    }
    // A solid ring band around a cylinder: outer wall plus top and bottom annuli.
    function band(name, x, z, rIn, rOut, y0, y1, tex, seg) {
        tube(name, x, z, rOut, y0, y1, tex, { seg: seg || 48 });
        annulus(name, x, z, rIn, rOut, y1, true, tex, seg || 48);
        annulus(name, x, z, rIn, rOut, y0, false, tex, seg || 48);
    }
    // H-section column or beam between two points along one world axis.
    function hbeam(s, a, b, size, flange) {
        const axis = [0, 1, 2].find(i => Math.abs(b[i] - a[i]) > 1e-6);
        const lo = Math.min(a[axis], b[axis]), hi = Math.max(a[axis], b[axis]);
        const others = [0, 1, 2].filter(i => i !== axis), h = size / 2, f = flange || 2, w = 1;
        const mk = (o0, o1, p0, p1) => {
            const from = [0, 0, 0], to = [0, 0, 0];
            from[axis] = lo; to[axis] = hi;
            from[others[0]] = a[others[0]] + o0; to[others[0]] = a[others[0]] + o1;
            from[others[1]] = a[others[1]] + p0; to[others[1]] = a[others[1]] + p1;
            box(s, from, to, 'frame');
        };
        mk(-h, -h + f, -h, h); mk(h - f, h, -h, h); mk(-h + f, h - f, -w, w);
    }
    // A slanted strut between two points: a square tube swept straight.
    const strut = (name, a, b, r) => sweep(name, [a, b], r || 1.6, 'frame', { seg: 4 });
    const rail = (pts, r) => sweep('rail', fillet(pts, 3, 4), r || 0.9, 'pipe', { seg: 8 });

    const BUILD = {};

    BUILD.carbide_furnace_plant = function () {
        const X0 = -80, X1 = 96, Z0 = 0, Z1 = 176;
        const cx = 8, cz = 88;
        const base = shape('base', false);
        // Skid: one plinth over the whole footprint; the front is stepped back where the ports sit.
        box(base, [X0, 0, Z0], [X1, 16, Z1], { up: 'deck', all: 'skid' }, ['down']);

        // ---- Front: item hatches either side of the control desk, fluid nozzles and the maintenance
        // cabinet on a service frame above.
        const front = shape('front', false);
        box(front, [-16, 0, -1], [0, 16, 0], { north: 'item_in', all: 'skid' }, ['south', 'down'], { north: true });
        box(front, [16, 0, -1], [32, 16, 0], { north: 'item_out', all: 'skid' }, ['south', 'down'], { north: true });
        // Input hopper lip: four sloped blue plates flaring out from the hatch.
        const lip = shape('front', false);
        const flare = (x0, x1, y0, y1, out) => {
            const inner = [[x0 + 2, y0 + 2], [x1 - 2, y0 + 2], [x1 - 2, y1 - 2], [x0 + 2, y1 - 2]], outer = [[x0, y0], [x1, y0], [x1, y1], [x0, y1]];
            for (let i = 0; i < 4; i++) {
                const a = inner[i], b = inner[(i + 1) % 4], c = outer[(i + 1) % 4], d = outer[i];
                lip.face([[a[0], a[1], -1], [b[0], b[1], -1], [c[0], c[1], -1 - out], [d[0], d[1], -1 - out]], 'cavity', [[0, 0], [16, 0], [16, 16], [0, 16]]);
                lip.face([[d[0], d[1], -1 - out], [c[0], c[1], -1 - out], [c[0], c[1], -1.6 - out], [d[0], d[1], -1.6 - out]].reverse(), 'blue', [[0, 0], [16, 0], [16, 1], [0, 1]]);
            }
        };
        flare(-16, 0, 0.5, 16, 4);
        // Output chute: a sloped orange-rimmed trough below the hatch opening.
        const chute = shape('front', false);
        box(chute, [19, 2, -7], [29, 3, -1], 'orange');
        box(chute, [19, 3, -7], [20, 7, -1], 'orange'); box(chute, [28, 3, -7], [29, 7, -1], 'orange');
        box(front, [-15, 17, -1], [-1, 21, 2], { north: 'tag_item_in', all: 'pipe' }, null, { north: true });
        box(front, [17, 17, -1], [31, 21, 2], { north: 'tag_item_out', all: 'pipe' }, null, { north: true });
        // Control desk in front of the controller block.
        const desk = shape('desk', false);
        box(desk, [1, 0, -6], [15, 8, 0], { all: 'desk' }, ['south', 'down']);
        desk.face([[15, 8, -6], [1, 8, -6], [1, 14, -1], [15, 14, -1]], 'panel', [[0, 16], [16, 16], [16, 0], [0, 0]]);
        box(desk, [1, 14, -1], [15, 16, 0], 'desk', ['south']);
        desk.face([[1, 8, -6], [1, 8, 0], [1, 14, 0], [1, 14, -1]], 'desk', [[0, 8], [6, 8], [6, 2], [5, 2]]);
        desk.face([[15, 8, 0], [15, 8, -6], [15, 14, -1], [15, 14, 0]], 'desk', [[0, 8], [6, 8], [5, 2], [6, 2]]);
        // Service frame above the front row carrying the cabinet and both fluid nozzles.
        const svc = shape('front', false);
        box(svc, [-18, 16, 2], [34, 34, 16], { all: 'cabinet', up: 'deck' });
        box(svc, [0.5, 17, 0], [15.5, 32, 2], { north: 'maint', all: 'cabinet' }, ['south'], { north: true });
        box(svc, [3, 32.5, -0.5], [13, 34, 2], 'cabinet');
        box(svc, [-15, 33, 1.4], [-1, 37, 2], { north: 'tag_fluid_in', all: 'pipe' }, null, { north: true });
        box(svc, [17, 33, 1.4], [31, 37, 2], { north: 'tag_fluid_out', all: 'pipe' }, null, { north: true });
        // Fluid nozzles: round pipe stubs with a bolted flange, blue water in, yellow gas out.
        sweep('nozzle', [[-8, 27, 2], [-8, 27, -5]], 3, 'water', { seg: 16, noTile: true });
        sweep('nozzle', [[-8, 27, -5], [-8, 27, -7]], 5, 'pipe', { seg: 16, noTile: true, cap: 'flange_in', capEnds: ['end'] });
        sweep('nozzle', [[24, 27, 2], [24, 27, -5]], 3, 'gas', { seg: 16, noTile: true });
        sweep('nozzle', [[24, 27, -5], [24, 27, -7]], 5, 'pipe', { seg: 16, noTile: true, cap: 'flange_out', capEnds: ['end'] });
        // Handwheel valve on the water line.
        sweep('valve', fillet([[-8, 31, -2.5], [-12, 31, -2.5], [-12, 35, -2.5], [-4, 35, -2.5], [-4, 31, -2.5], [-8, 31, -2.5]], 2, 5), 0.6, 'water', { seg: 6 });
        tube('valve', -8, -2.5, 0.8, 30, 31, 'pipe', { seg: 6 });
        box(front, [30, 28, -1], [33, 31, 2], { north: 'gauge', all: 'pipe' }, null, { north: true });

        // ---- Furnace: brick foot, welded shell with stiffener bands and a cooling-water ring,
        // conical hood and round roof lid.
        band('foot', cx, cz, 56, 64, 16, 22, 'brick', 64);
        tube('shell', cx, cz, 60, 22, 102, 'shell', { seg: 64, noTile: true });
        band('band', cx, cz, 59, 62, 44, 47, 'pipe', 64);
        band('band', cx, cz, 59, 62, 74, 77, 'pipe', 64);
        band('flange', cx, cz, 58, 64, 100, 104, 'pipe', 64);
        const ringPts = [];
        for (let i = 0; i <= 48; i++) { const a = i / 48 * Math.PI * 2; ringPts.push([cx + Math.cos(a) * 64, 60, cz + Math.sin(a) * 64]); }
        sweep('water_ring', ringPts, 2.6, 'water', { seg: 10 });
        for (let i = 0; i < 12; i++) {
            const a = (i + 0.5) / 12 * Math.PI * 2, c = Math.cos(a), s = Math.sin(a);
            if (Math.abs(a - Math.PI * 1.5) < 0.4) continue;
            sweep('water_drop', fillet([[cx + c * 64, 60, cz + s * 64], [cx + c * 64, 54, cz + s * 64], [cx + c * 61, 50, cz + s * 61]], 2, 3), 1.1, 'water', { seg: 6 });
        }
        sweep('hood', [[cx, 104, cz], [cx, 128, cz]], d => 60 - d * (24 / 24), 'hood', { seg: 64, noTile: true });
        sweep('roof', [[cx, 128, cz], [cx, 134, cz]], 37, 'pipe', { seg: 48, noTile: true, cap: 'roof', capEnds: ['end'] });

        // ---- Tap hole on the +x side: tapping spout, glowing stream and the carbide pot on a bogie.
        const tap = shape('tap', false);
        box(tap, [66, 22, 80], [72, 36, 96], 'brick');
        box(shape('glow_tap', false), [72, 26, 84], [72.5, 32, 92], 'glow');
        const launder = shape('tap', false);
        box(launder, [72, 24, 82], [86, 26, 94], 'brick');
        box(launder, [72, 26, 82], [86, 30, 83], 'brick'); box(launder, [72, 26, 93], [86, 30, 94], 'brick');
        box(shape('glow_stream', false), [72, 26.5, 83], [86, 27.2, 93], 'glow', ['down', 'north', 'south']);
        box(shape('glow_stream', false), [84, 18, 86], [86, 26.5, 90], 'glow', ['up']);
        band('pot', 85, 88, 8, 10, 16, 18.5, 'pipe', 24);
        tube('pot', 85, 88, 10.5, 16, 17, 'frame', { seg: 24 });
        annulus('glow_pot', 85, 88, 0, 8, 18, true, 'glow', 24);
        tube('pot', 85, 88, 9.5, 18.5, 23, 'hood', { seg: 24, noTile: true });
        annulus('pot', 85, 88, 8, 9.5, 23, true, 'hood', 24);

        // ---- Gas off-take: insulated duct from the hood side bending up into a tall flue with a rain cap.
        sweep('duct', fillet([[-22, 118, 118], [-54, 118, 150], [-54, 302, 150]], 14, 8), 8, 'insul', { seg: 20 });
        sweep('duct', [[-54, 302, 150], [-54, 306, 150]], 11, 'pipe', { seg: 20, noTile: true });
        sweep('duct', [[-54, 310, 150], [-54, 316, 150]], d => 13 - d * 2, 'hood', { seg: 20, noTile: true, cap: 'roof', capEnds: ['end'] });
        for (const a of [0, 1, 2, 3]) {
            const c = Math.cos(a * Math.PI / 2 + 0.78), s = Math.sin(a * Math.PI / 2 + 0.78);
            strut('duct', [-54 + c * 9, 305, 150 + s * 9], [-54 + c * 9, 311, 150 + s * 9], 0.7);
        }
        for (const y of [160, 220, 280]) band('duct', -54, 150, 7.5, 9.5, y, y + 3, 'pipe', 20);
        // CO gas line (yellow) from the hood front round two bends down to the gas outlet nozzle.
        sweep('gas_line', fillet([[24, 112, 44], [24, 112, 10], [24, 60, 10], [24, 40, 6], [24, 27, 6], [24, 27, 2]], 6, 6), 3, 'gas', { seg: 12 });
        // Cooling water from the water inlet up into the ring header.
        sweep('water_line', fillet([[-8, 27, 2], [-8, 27, 10], [-8, 60, 10], [-8, 60, 26]], 6, 6), 2.6, 'water', { seg: 12 });

        // ---- Electrodes: three graphite columns in a triangle with copper contact clamps,
        // hydraulic slipping rams and a crosshead each.
        const electrodes = [0, 1, 2].map(i => { const a = -Math.PI / 2 + i * Math.PI * 2 / 3; return [cx + Math.cos(a) * 20, cz + Math.sin(a) * 20]; });
        electrodes.forEach(([ex, ez], i) => {
            tube('electrode', ex, ez, 8, 120, 262, 'graphite', { seg: 16, cap: 'graphite', capEnds: ['end'] });
            band('glow_seal', ex, ez, 8, 11, 133, 134.5, 'glow', 16);
            band('clamp', ex, ez, 8, 11, 150, 164, 'copper', 16);
            band('clamp', ex, ez, 8, 12, 166, 169, 'pipe', 16);
            band('slip', ex, ez, 8, 12, 222, 226, 'pipe', 16);
            band('slip', ex, ez, 8, 12, 244, 248, 'pipe', 16);
            for (const s of [-1, 1]) {
                tube('ram', ex + s * 14, ez, 2.2, 226, 244, 'pipe', { seg: 8 });
                tube('ram', ex + s * 14, ez, 1.2, 244, 252, 'pipe', { seg: 6 });
            }
            const head = shape('crosshead', false);
            box(head, [ex - 17, 252, ez - 3], [ex + 17, 257, ez + 3], 'frame');
        });
        // Copper bus tubes from the transformer bushings to the clamps, rounded where they bend.
        const bush = [[-24, 150], [8, 150], [40, 150]];
        bush.forEach(([bx, bz], i) => {
            const [ex, ez] = electrodes[[1, 0, 2][i]];
            sweep('bus', fillet([[bx, 76, bz], [bx, 157, bz], [ex + (ex - cx) * 0.6, 157, ez + (ez - cz) * 0.6], [ex + (ex - cx) * 0.14, 157, ez + (ez - cz) * 0.14]], 10, 6), 2.2, 'copper', { seg: 10 });
        });

        // ---- Transformer at the back with radiator banks, bushings and the HV terminal box.
        const tf = shape('transformer', false);
        box(tf, [-40, 16, 140], [56, 56, 166], 'tank', ['down']);
        box(tf, [-42, 56, 138], [58, 60, 168], 'tank');
        for (let x = -36; x < 54; x += 6) box(tf, [x, 20, 166], [x + 2, 52, 172], 'tank');
        box(tf, [-38, 52, 166], [54, 54, 172], 'tank'); box(tf, [-38, 18, 166], [54, 20, 172], 'tank');
        bush.forEach(([bx, bz]) => {
            const shed = d => 4.5 + ((Math.floor(d / 2) % 2) ? 0 : 2.2);
            sweep('bushing', [[bx, 60, bz], [bx, 76, bz]], shed, 'ceramic', { seg: 12, extra: [2, 4, 6, 8, 10, 12, 14], noTile: true });
            tube('bushing', bx, bz, 3, 76, 80, 'copper', { seg: 10, cap: 'copper', capEnds: ['end'] });
        });
        sweep('conservator', [[-30, 72, 160], [46, 72, 160]], 7, 'tank', { seg: 16, cap: 'tank' });
        for (const x of [-22, 38]) box(shape('conservator', false), [x - 1.5, 60, 158.5], [x + 1.5, 66, 161.5], 'tank');
        // HV terminal box and the three energy sockets with their cables, on the back face.
        const back = shape('back', false);
        box(back, [1, 1, 176], [15, 15, 178], { south: 'socket', all: 'skid' }, ['north'], { south: true });
        for (let i = 0; i < 3; i++) {
            const x0 = -47 + i * 16;
            box(back, [x0, 1, 176], [x0 + 14, 15, 178], { south: 'socket', all: 'skid' }, ['north'], { south: true });
            sweep('cable', fillet([[x0 + 7, 8, 177], [x0 + 7, 8, 182], [x0 + 7, 14, 182]], 3, 4), 2, 'rubber', { seg: 8 });
            sweep('cable', fillet([[x0 + 7, 14, 182], [x0 + 7, 22, 176], [x0 + 7, 30, 172]], 4, 4), 2, 'rubber', { seg: 8 });
        }

        // ---- Charging bins for lime and coke on the upper deck, feeding the roof through bent chutes.
        for (const [bx, bz] of [[-40, 62], [56, 62]]) {
            sweep('bin', [[bx, 212, bz], [bx, 228, bz]], d => 4 + d * (10 / 16), 'pipe', { seg: 16, noTile: true });
            tube('bin', bx, bz, 14, 228, 248, 'cabinet', { seg: 16, noTile: true, cap: 'roof', capEnds: ['end'] });
            band('bin', bx, bz, 13.5, 15, 246, 248, 'pipe', 16);
            const toward = bx < cx ? 1 : -1;
            sweep('chute', fillet([[bx, 212, bz], [bx, 190, bz], [cx + toward * -26, 150, cz - 22], [cx + toward * -26, 133, cz - 22]], 10, 6), 3, 'pipe', { seg: 10 });
            for (const sx of [-1, 1]) for (const sz of [-1, 1]) strut('bin_leg', [bx + sx * 10, 228, bz + sz * 10], [bx + sx * 10, 200, bz + sz * 10], 1.2);
        }

        // ---- Steel frame: H-section corner columns, ring beams, bracing, two decks and a crane girder.
        const fr = shape('frame', false);
        const cols = [[-72, 8], [88, 8], [-72, 168], [88, 168]];
        for (const [x, z] of cols) { hbeam(fr, [x, 18, z], [x, 280, z], 12); box(fr, [x - 8, 16, z - 8], [x + 8, 18, z + 8], 'frame', ['down']); }
        for (const y of [128, 200, 280]) {
            hbeam(fr, [-66, y - 6, 8], [82, y - 6, 8], 12); hbeam(fr, [-66, y - 6, 168], [82, y - 6, 168], 12);
            hbeam(fr, [-72, y - 6, 14], [-72, y - 6, 162], 12); hbeam(fr, [88, y - 6, 14], [88, y - 6, 162], 12);
        }
        for (const [a, b] of [[[-72, 8], [-72, 168]], [[88, 8], [88, 168]], [[-72, 168], [88, 168]]]) {
            const m = [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
            for (const [y0, y1] of [[16, 122], [128, 194]]) {
                strut('brace', [a[0], y0 + 4, a[1]], [m[0], y1 - 2, m[1]], 1.6);
                strut('brace', [b[0], y0 + 4, b[1]], [m[0], y1 - 2, m[1]], 1.6);
            }
        }
        // Main deck at the hood shoulder, open around the roof, with a grating floor and pipe railings.
        const deck = shape('deck', false);
        const hole = [-36, 52, 44, 132];
        const gr = (x0, z0, x1, z1) => box(deck, [x0, 128, z0], [x1, 129, z1], 'grating');
        gr(-66, 14, 82, hole[2]); gr(-66, hole[3], 82, 162); gr(-66, hole[2], hole[0], hole[3]); gr(hole[1], hole[2], 82, hole[3]);
        // Beams framing the opening: the long pair lands on the front and back ring beams, the short
        // trimmers on the long pair.
        hbeam(fr, [hole[0], 122, 14], [hole[0], 122, 162], 10); hbeam(fr, [hole[1], 122, 14], [hole[1], 122, 162], 10);
        for (const z of [hole[2], hole[3]]) hbeam(fr, [hole[0] + 5, 122, z], [hole[1] - 5, 122, z], 10);
        for (const z of [14, 162]) rail([[-66, 128, z], [-66, 146, z], [82, 146, z], [82, 128, z]]);
        for (const x of [-66, 82]) rail([[x, 146, 14], [x, 146, 162]]);
        for (const x of [-30, 0, 30, 60]) { rail([[x, 128, 14], [x, 146, 14]]); rail([[x, 128, 162], [x, 146, 162]]); }
        for (const z of [40, 88, 136]) { rail([[-66, 128, z], [-66, 146, z]]); rail([[82, 128, z], [82, 146, z]]); }
        rail([[-66, 137, 14], [82, 137, 14]], 0.7); rail([[-66, 137, 162], [82, 137, 162]], 0.7);
        rail([[-66, 137, 14], [-66, 137, 162]], 0.7); rail([[82, 137, 14], [82, 137, 162]], 0.7);
        // Electrode deck higher up with a round opening rail around the columns.
        gr2(deck, -66, 14, 82, 162, 200);
        function gr2(s, x0, z0, x1, z1, y) {
            box(s, [x0, y, z0], [x1, y + 1, 44], 'grating'); box(s, [x0, y, 132], [x1, y + 1, z1], 'grating');
            box(s, [x0, y, 44], [-24, y + 1, 132], 'grating'); box(s, [40, y, 44], [x1, y + 1, 132], 'grating');
        }
        hbeam(fr, [-24, 194, 14], [-24, 194, 162], 10); hbeam(fr, [40, 194, 14], [40, 194, 162], 10);
        for (const z of [44, 132]) hbeam(fr, [-19, 194, z], [35, 194, z], 10);
        for (const z of [14, 162]) rail([[-66, 218, z], [82, 218, z]]);
        for (const x of [-66, 82]) rail([[x, 218, 14], [x, 218, 162]]);
        for (const [x, z] of [[-66, 14], [82, 14], [-66, 162], [82, 162], [8, 14], [8, 162], [-66, 88], [82, 88]]) rail([[x, 200, z], [x, 218, z]]);
        // Crane girder across the top with a hoist trolley for electrode changes.
        // Crane girder across the top, sitting on the side ring beams with a bearing plate at each end.
        hbeam(fr, [-78, 286, cz], [94, 286, cz], 12);
        for (const x of [-72, 88]) box(fr, [x - 6, 280, cz - 8], [x + 6, 280.5, cz + 8], 'frame');
        const hoist = shape('hoist', false);
        box(hoist, [-2, 270, cz - 10], [18, 280, cz + 10], 'cabinet');
        tube('hoist', 8, cz, 0.6, 252, 270, 'pipe', { seg: 4 });
        // Caged ladder up the front-left corner to the main deck and on to the electrode deck.
        const ladder = shape('ladder', false);
        for (const [y0, y1, z] of [[16, 146, 4], [128, 218, 4]]) {
            for (const x of [-62, -52]) box(ladder, [x - 1, y0, z - 1], [x + 1, y1, z + 1], 'frame');
            for (let y = y0 + 6; y < Math.min(y1, y0 === 16 ? 128 : 200); y += 6) box(ladder, [-61, y, z - 0.5], [-53, y + 1, z + 0.5], 'pipe');
            for (let y = y0 + 36; y < y1 - 4; y += 14) sweep('cage', fillet([[-63, y, z], [-63, y, z - 8], [-51, y, z - 8], [-51, y, z]], 4, 4), 0.6, 'frame', { seg: 6 });
            for (const x of [-63, -57, -51]) strut('cage', [x, y0 + 36, z - 8 + (x === -57 ? 0 : 3)], [x, y1 - 6, z - 8 + (x === -57 ? 0 : 3)], 0.5);
        }
    };

    // ---- Shared pieces for plants drawn straight over their block layout (tools/gen_plant_layouts.py):
    // cell i of a plant w cells wide spans x cellX(w, i) .. +16, depth d spans z d*16 .. +16.
    const cellX = (w, i) => (i - (w >> 1)) * 16;
    const PORT_FACE = { I: 'item_in', O: 'item_out', L: 'flange_in', R: 'flange_out', Q: 'flange_gas', M: 'maint', '@': 'panel', S: 'socket', H: 'socket', E: 'socket', J: 'socket' };
    const NOZZLE = { L: 'water', R: 'pipe', Q: 'gas' };
    // Hatch boxes on one face of row d; rows maps a layer to its row text, as in the layout.
    function ports(w, side, d, rows, baseY) {
        const dir = side === 'north' ? -1 : 1;
        for (const layer in rows) [...rows[layer]].forEach((ch, i) => {
            const face = PORT_FACE[ch];
            if (!face) return;
            const x0 = cellX(w, i), y0 = layer * 16 + (baseY || 0), z0 = d * 16, zf = side === 'north' ? z0 : z0 + 16;
            box(shape('port', false), [x0, y0, z0], [x0 + 16, y0 + 16, z0 + 16], { [side]: face, all: 'cabinet' }, null, { [side]: true });
            if (!NOZZLE[ch]) return;
            sweep('nozzle', [[x0 + 8, y0 + 8, zf], [x0 + 8, y0 + 8, zf + dir * 4]], 3.5, NOZZLE[ch], { seg: 12, noTile: true });
            sweep('nozzle', [[x0 + 8, y0 + 8, zf + dir * 4], [x0 + 8, y0 + 8, zf + dir * 6]], 5.5, 'pipe', { seg: 12, noTile: true, cap: face, capEnds: ['end'] });
        });
    }
    // Checker-plate skid over cells [i0, i1) x [d0, d1).
    const skid = (w, i0, i1, d0, d1, y0, y1) => box(shape('base', false), [cellX(w, i0), y0 || 0, d0 * 16], [cellX(w, i1), y1 || 16, d1 * 16], { up: 'deck', all: 'skid' }, ['down']);
    // Square tapered hopper with an open top, lined inside.
    function hopper(name, cx, cz, top, bot, y0, y1, tex) {
        const s = shape(name, false);
        const P = (h, y) => [[cx - h[0], y, cz - h[1]], [cx + h[0], y, cz - h[1]], [cx + h[0], y, cz + h[1]], [cx - h[0], y, cz + h[1]]];
        const lo = P(bot, y0), hi = P(top, y1);
        const [W, H] = TEXTURES[tex];
        for (let k = 0; k < 4; k++) {
            const a = lo[k], b = lo[(k + 1) % 4], c = hi[(k + 1) % 4], d = hi[k];
            s.face([b, a, d, c], tex, [[0, H], [W, H], [W, 0], [0, 0]]);
            s.face([a, b, c, d], 'cavity', [[0, 16], [16, 16], [16, 0], [0, 0]]);
        }
        return s;
    }
    // Vertical vessel: shell, stiffening bands and a dished head.
    function vessel(name, x, z, r, y0, y1, tex, head) {
        tube(name, x, z, r, y0, y1, tex, { seg: 24, noTile: true });
        const h = head || r * 0.5;
        sweep(name, [[x, y1, z], [x, y1 + h, z]], d => r * Math.sqrt(Math.max(0, 1 - (d / h) ** 2)), tex, { seg: 24, noTile: true, extra: [h * 0.3, h * 0.55, h * 0.75, h * 0.9] });
        band(name, x, z, r - 0.5, r + 1, y0, y0 + 4, 'frame', 24);
    }
    // Horizontal drum along x or z with flat ends.
    const drum = (name, a, b, r, tex) => sweep(name, [a, b], r, tex, { seg: 20, noTile: true, cap: tex });

    BUILD.coke_oven_battery = function () {
        const w = 9, X = i => cellX(w, i);
        const fr = shape('frame', false);
        // One oven: brick body on its regenerator, doors at both ends, charging deck with the car rails,
        // ascension pipe into the gas collecting main.
        const oven = (z0, stand, ends) => {
            const skip = ['north', 'south'].filter(f => !(ends || []).includes(f));
            const s = shape('oven', false);
            box(s, [X(1), 0, z0], [X(8), 80, z0 + 16], 'brick', skip);
            box(s, [X(1), 80, z0], [X(8), 96, z0 + 16], { up: 'deck', all: 'skid' }, skip);
            box(shape('base', false), [X(0), 0, z0], [X(1), 16, z0 + 16], { up: 'deck', all: 'skid' }, ['down', ...skip]);
            box(shape('base', false), [X(8), 0, z0], [X(9), 16, z0 + 16], { up: 'deck', all: 'skid' }, ['down', ...skip]);
            for (const [x, out] of [[X(1), -1], [X(8), 1]]) {
                const door = shape('door', false), xo = x + out * 2.5;
                box(shape('glow_door', false), [x, 10, z0 + 1.5], [x + out * 0.5, 76, z0 + 14.5], 'glow');
                box(door, [x, 12, z0 + 2.5], [x + out * 2, 74, z0 + 13.5], 'door');
                for (const y of [24, 46, 64]) box(door, [x + out * 2, y, z0 + 1.5], [xo, y + 3, z0 + 14.5], 'frame');
            }
            const rails = shape('rail', false);
            for (const x of [-24, 8]) { box(rails, [x - 3, 96, z0], [x + 3, 97.5, z0 + 16], 'frame', ['north', 'south']); box(rails, [x - 1, 97.5, z0], [x + 1, 101, z0 + 16], 'pipe', ['north', 'south']); }
            tube('lid', -8, z0 + 8, 4, 96, 97.5, 'pipe', { seg: 12, cap: 'pipe', capEnds: ['end'] });
            tube('ascension', 40, z0 + 8, 4, 96, 116, 'insul', { seg: 12 });
            band('ascension', 40, z0 + 8, 4, 5.5, 104, 106, 'pipe', 12);
            sweep('main', [[40, 120, z0], [40, 120, z0 + 16]], 6, 'gas', { seg: 16 });
            if (!stand) return;
            // Buckstays either side of the oven wall, tied together over the top.
            for (const x of [X(0) + 8, X(8) + 8]) { hbeam(fr, [x, 16, z0 + 8], [x, 130, z0 + 8], 12); box(fr, [x - 7, 16, z0 + 1], [x + 7, 18, z0 + 15], 'frame', ['down']); }
            hbeam(fr, [X(0) + 2, 136, z0 + 8], [X(9) - 2, 136, z0 + 8], 12);
            box(fr, [36, 126, z0 + 4], [44, 130, z0 + 12], 'frame');
        };
        // Head: ports, the gas offtake from the main down to the gas hatch, first stand.
        skid(w, 0, 3, 0, 1); skid(w, 6, 9, 0, 1);
        ports(w, 'north', 0, { 0: '...I@O...', 1: '...LMRQ..' });
        sweep('main', fillet([[40, 32, 8], [40, 120, 8], [40, 120, 16]], 6, 6), 6, 'gas', { seg: 16 });
        oven(16, true, ['north']);
        within('bay', null, () => oven(32, false));
        within('stand', null, () => oven(48, true));
        // Tail: last stand, brick end buttress with the waste-gas stack, sockets at the back.
        within('tail', null, () => {
            oven(240, true, ['south']);
            skid(w, 0, 3, 16, 17); skid(w, 6, 9, 16, 17);
            ports(w, 'south', 16, { 0: '...SHS...' });
            box(shape('oven', false), [X(1), 16, 256], [X(8), 64, 272], 'brick');
            tube('stack', -40, 264, 7.5, 64, 150, 'brick', { seg: 16 });
            band('stack', -40, 264, 7.5, 9, 146, 150, 'pipe', 16);
            sweep('main', [[40, 120, 256], [40, 120, 262]], 6, 'gas', { seg: 16, cap: 'pipe', capEnds: ['end'] });
        });
    };

    BUILD.twin_screw_extruder = function () {
        const w = 5, X = i => cellX(w, i);
        const fr = shape('frame', false);
        // Bed: checker plate, two sill beams and the barrel pedestal.
        const base = (d, plinth) => {
            const z0 = d * 16;
            if (plinth !== false) skid(w, 0, 5, d, d + 1);
            for (const x of [-8, 24]) hbeam(fr, [x, 24, z0], [x, 24, z0 + 16], 12);
            box(shape('pedestal', false), [2, 16, z0 + 3], [14, 33, z0 + 13], 'cabinet');
        };
        // Barrel segment with its heater band, cooling line and thermocouple.
        const barrel = d => {
            const z0 = d * 16;
            sweep('barrel', [[8, 40, z0], [8, 40, z0 + 16]], 6, 'pipe', { seg: 16 });
            sweep('heater', [[8, 40, z0 + 2], [8, 40, z0 + 14]], 7.5, 'ceramic', { seg: 16, noTile: true });
            tube('thermo', 8, z0 + 8, 0.8, 47, 53, 'copper', { seg: 6 });
            sweep('cooling', [[-1, 46, z0], [-1, 46, z0 + 16]], 1, 'water', { seg: 8 });
        };
        const sideBeams = d => { for (const x of [-24, 40]) hbeam(fr, [x, 72, d * 16], [x, 72, d * 16 + 16], 12); };
        const portal = d => {
            for (const x of [-24, 40]) { hbeam(fr, [x, 16, d * 16 + 8], [x, 66, d * 16 + 8], 12); box(fr, [x - 7, 16, d * 16 + 1], [x + 7, 18, d * 16 + 15], 'frame', ['down']); }
            hbeam(fr, [-32, 72, d * 16 + 8], [48, 72, d * 16 + 8], 12);
        };
        const bay = d => { base(d); barrel(d); sideBeams(d); };
        const stand = d => { base(d); barrel(d); portal(d); };
        // Head: ports, drive motor and gearbox, then the feed segment with its hopper.
        skid(w, 0, 1, 0, 1); skid(w, 4, 5, 0, 1);
        ports(w, 'north', 0, { 0: '.I@O.', 1: '.LMR.' });
        portal(0);
        sweep('motor', [[8, 40, 1], [8, 40, 16]], 7, 'blue', { seg: 16, noTile: true, cap: 'pipe', capEnds: ['start'] });
        for (let z = 3; z < 15; z += 3) sweep('motor', [[8, 40, z], [8, 40, z + 1]], 7.8, 'blue', { seg: 16, noTile: true });
        base(1);
        box(shape('gearbox', false), [-16, 32, 16], [32, 48, 32], 'blue');
        for (const x of [-24, 40]) hbeam(fr, [x, 72, 16], [x, 72, 32], 12);
        bay(2);
        hopper('hopper', 8, 40, [7, 7], [3, 3], 48, 64, 'vessel');
        within('bay', null, () => bay(3));
        within('stand', null, () => stand(3));
        // Tail: last barrel, die head, strand bath and pelletiser under the end portal.
        within('tail', null, () => {
            bay(10);
            base(11); sideBeams(11);
            box(shape('die', false), [1, 33, 176], [15, 47, 192], 'pipe');
            box(shape('die', false), [3, 35, 191.5], [13, 45, 192], 'flange_out', null, { south: true });
            skid(w, 0, 2, 12, 13); skid(w, 3, 5, 12, 13);
            ports(w, 'south', 12, { 0: '..H..' });
            portal(12);
            box(shape('bath', false), [-16, 16, 192], [32, 30, 208], 'blue');
            box(shape('bath', false), [-14, 29, 193], [30, 29.5, 207], 'water');
            for (const x of [4, 8, 12]) sweep('strand', [[x, 37, 192], [x, 30, 200], [x, 32, 207]], 0.6, 'orange', { seg: 4 });
            box(shape('pelletiser', false), [0, 32, 200], [16, 48, 208], { all: 'cabinet', up: 'orange' });
        });
    };

    BUILD.bio_ethylene_plant = function () {
        const w = 9, X = i => cellX(w, i);
        const fr = shape('frame', false);
        skid(w, 0, 9, 1, 16); skid(w, 0, 3, 0, 1); skid(w, 6, 9, 0, 1); skid(w, 0, 3, 16, 17); skid(w, 6, 9, 16, 17);
        ports(w, 'north', 0, { 0: '...I@O...', 1: '...LMRQ..' });
        ports(w, 'south', 16, { 0: '...SHS...' });
        // Four fermenters with agitator drives, each piped up to the rack.
        for (const [x, z] of [[-40, 56], [56, 56], [-40, 120], [56, 120]]) {
            vessel('fermenter', x, z, 22, 16, 96, 'vessel', 14);
            for (const y of [44, 72]) band('fermenter', x, z, 21.5, 23, y, y + 3, 'pipe', 24);
            tube('agitator', x, z, 4, 106, 116, 'blue', { seg: 12, cap: 'pipe', capEnds: ['end'] });
            const rx = x < 0 ? -8 : 24;
            sweep('line', fillet([[x + (rx - x) * 0.3, 104, z], [x + (rx - x) * 0.3, 112, z], [rx, 112, z], [rx, 106, z]], 3, 4), 2, 'pipe', { seg: 8 });
        }
        // Pipe rack down the middle: bents every four blocks, longitudinal beams, three lines on top.
        for (const z of [40, 104, 168, 232]) {
            for (const x of [-8, 24]) { hbeam(fr, [x, 16, z], [x, 82, z], 12); box(fr, [x - 7, 16, z - 7], [x + 7, 18, z + 7], 'frame', ['down']); }
            hbeam(fr, [-14, 88, z], [30, 88, z], 12);
        }
        for (const [z0, z1] of [[46, 98], [110, 162], [174, 226]]) for (const x of [-8, 24]) hbeam(fr, [x, 88, z0], [x, 88, z1], 10);
        for (const [x, tex] of [[-8, 'water'], [8, 'pipe'], [24, 'gas']]) sweep('rack_pipe', [[x, 101, 32], [x, 101, 240]], 4.5, tex, { seg: 12, cap: 'pipe' });
        // Gas line from the rack down to the gas hatch.
        sweep('gas_line', fillet([[24, 101, 36], [24, 101, 24], [40, 101, 24], [40, 24, 24], [40, 24, 16]], 6, 6), 3.5, 'gas', { seg: 12 });
        // Ethanol column with its ring platform, overhead line and reflux drum.
        vessel('column', -40, 200, 7, 16, 136, 'vessel', 5);
        for (const y of [48, 112]) band('column', -40, 200, 7, 8.5, y, y + 2, 'pipe', 16);
        const deck = shape('deck', false);
        for (const [a, b] of [[[-64, 176], [-16, 192]], [[-64, 208], [-16, 224]], [[-64, 192], [-48, 208]], [[-32, 192], [-16, 208]]]) box(deck, [a[0], 80, a[1]], [b[0], 81, b[1]], 'grating');
        for (const [x, z] of [[-62, 178], [-18, 178], [-62, 222], [-18, 222]]) strut('bracket', [-40 + Math.sign(x + 40) * 6, 56, 200 + Math.sign(z - 200) * 6], [x, 79, z], 1.2);
        rail([[-64, 81, 176], [-64, 98, 176], [-16, 98, 176], [-16, 98, 224], [-64, 98, 224], [-64, 98, 176]]);
        for (const [x, z] of [[-16, 176], [-16, 224], [-64, 224], [-40, 176], [-40, 224], [-16, 200], [-64, 200]]) rail([[x, 81, z], [x, 98, z]]);
        drum('reflux', [-56, 24, 226], [-56, 24, 254], 7, 'vessel');
        for (const z of [230, 250]) box(fr, [-62, 16, z - 2], [-50, 19, z + 2], 'frame', ['down']);
        sweep('overhead', fillet([[-40, 141, 200], [-40, 142, 200], [-58, 142, 200], [-58, 142, 240], [-58, 31, 240]], 5, 5), 2.5, 'insul', { seg: 10 });
        // Dehydration reactor, fired heater with peep holes and its stack, transfer line between them.
        vessel('reactor', 56, 200, 7, 16, 92, 'vessel', 4);
        const heater = shape('heater', false);
        box(heater, [32, 16, 224], [80, 64, 256], 'hood');
        for (const z of [232, 244]) box(shape('glow_heater', false), [80, 26, z], [80.5, 32, z + 6], 'glow');
        tube('stack', 56, 248, 5, 64, 144, 'pipe', { seg: 16, cap: 'cavity', capEnds: ['end'] });
        band('stack', 56, 248, 5, 6.5, 100, 103, 'frame', 16);
        sweep('transfer', fillet([[44, 64, 232], [44, 104, 232], [56, 104, 232], [56, 104, 207]], 5, 5), 2.5, 'insul', { seg: 10 });
    };

    BUILD.waste_heat_power_plant = function () {
        const w = 9, X = i => cellX(w, i);
        const fr = shape('frame', false);
        skid(w, 0, 9, 1, 16); skid(w, 0, 3, 0, 1); skid(w, 6, 9, 0, 1); skid(w, 0, 4, 16, 17); skid(w, 5, 9, 16, 17);
        ports(w, 'north', 0, { 0: '...I@O...', 1: '...LMR...' });
        ports(w, 'south', 16, { 0: '....J....' });
        // Boiler: plate casing with buckstay bands, steam drum on saddles along the roof.
        const blr = shape('boiler', false);
        box(blr, [X(2), 16, 32], [X(7), 112, 128], { all: 'shell', up: 'deck' }, ['down']);
        for (const y of [44, 76]) box(fr, [X(2) - 2, y, 30], [X(7) + 2, y + 4, 130], 'frame');
        drum('drum', [X(2) + 2, 120, 88], [X(7) - 2, 120, 88], 7, 'vessel');
        for (const x of [-24, 40]) box(fr, [x - 4, 112, 82], [x + 4, 114, 94], 'frame', ['down']);
        for (const x of [-8, 24]) tube('valve', x, 88, 1.6, 126, 133, 'pipe', { seg: 8, cap: 'pipe', capEnds: ['end'] });
        // Stair up the +x side to the landing behind the boiler.
        const stair = shape('stair', false);
        for (let k = 0; k < 6; k++) {
            box(stair, [X(8) + 1, 16 + 16 * k + 7, 32 + 16 * k], [X(9) - 1, 16 + 16 * k + 8, 40 + 16 * k], 'grating');
            box(stair, [X(8) + 1, 16 + 16 * k + 15, 40 + 16 * k], [X(9) - 1, 16 + 16 * k + 16, 48 + 16 * k], 'grating');
        }
        for (const x of [X(8) + 1, X(9) - 1]) { strut('stringer', [x, 16, 32], [x, 112, 128], 1.2); rail([[x, 34, 34], [x, 130, 128]]); }
        box(stair, [X(7), 112, 128], [X(9), 114, 144], 'grating');
        for (const x of [56, 72]) hbeam(fr, [x, 16, 136], [x, 112, 136], 10);
        rail([[X(7), 114, 144], [X(7), 130, 144], [X(9), 130, 144], [X(9), 130, 128]]);
        // Flue duct from the back of the boiler into the brick stack.
        box(shape('flue', false), [X(4), 80, 128], [X(5), 96, 144], 'insul');
        tube('stack', 8, 152, 8, 16, 150, 'brick', { seg: 16 });
        band('stack', 8, 152, 8, 9.5, 146, 150, 'pipe', 16);
        // Feedwater up the front of the boiler into the drum; steam line over its support into the turbine.
        sweep('feed', fillet([[-8, 24, 16], [-8, 24, 24], [-8, 120, 24], [-8, 120, 81]], 6, 6), 2.5, 'water', { seg: 10 });
        sweep('steam', fillet([[40, 120, 95], [40, 120, 184], [40, 70, 184], [30, 62, 186]], 8, 6), 3, 'insul', { seg: 12 });
        hbeam(fr, [40, 16, 152], [40, 114, 152], 10);
        // Turbine table: columns, deck and condenser below; turbine, coupling and generator on top.
        for (const z of [184, 216, 248]) for (const x of [-8, 56]) hbeam(fr, [x, 16, z], [x, 32, z], 10);
        box(shape('table', false), [X(3), 32, 176], [X(8), 48, 256], { up: 'deck', all: 'skid' });
        drum('condenser', [24, 24, 194], [24, 24, 222], 7, 'vessel');
        box(fr, [2, 48, 176], [46, 52, 208], 'frame', ['down']);
        sweep('turbine', [[24, 58, 178], [24, 58, 206]], d => 6 + d * 0.12, 'blue', { seg: 20, noTile: true, cap: 'pipe' });
        for (const z of [184, 196]) sweep('turbine', [[24, 58, z], [24, 58, z + 2]], 9.5, 'pipe', { seg: 20, noTile: true });
        sweep('shaft', [[24, 58, 206], [24, 58, 226]], 2, 'pipe', { seg: 8 });
        box(shape('generator', false), [6, 48, 226], [42, 66, 254], 'blue');
        for (let z = 229; z < 252; z += 4) box(shape('generator', false), [5, 50, z], [43, 64, z + 1.5], 'frame');
        for (const x of [4, 12]) sweep('cable', fillet([[x + 8, 52, 254], [x + 8, 52, 262], [x + 8, 16, 262]], 4, 4), 1.8, 'rubber', { seg: 8 });
    };

    BUILD.ore_processing_plant = function () {
        const w = 5, X = i => cellX(w, i);
        const fr = shape('frame', false);
        skid(w, 0, 5, 1, 6); skid(w, 0, 1, 0, 1); skid(w, 4, 5, 0, 1); skid(w, 0, 2, 6, 7); skid(w, 3, 5, 6, 7);
        ports(w, 'north', 0, { 0: '.I@O.' });
        ports(w, 'south', 6, { 0: '..E..' });
        // Frame over the crusher and the trommel.
        for (const z of [8, 40, 72]) {
            for (const x of [-24, 40]) { hbeam(fr, [x, 16, z], [x, 50, z], 12); box(fr, [x - 7, 16, z - 7], [x + 7, 18, z + 7], 'frame', ['down']); }
            hbeam(fr, [-32, 56, z], [48, 56, z], 12);
        }
        for (const [z0, z1] of [[14, 34], [46, 66]]) for (const x of [-24, 40]) hbeam(fr, [x, 56, z0], [x, 56, z1], 12);
        // Roll crusher: housing, two counter-rotating rolls, hopper on the front cross beam.
        const housing = shape('crusher', false);
        box(housing, [X(1), 16, 0], [X(4), 32, 32], 'blue');
        box(housing, [X(1), 32, 0], [X(4), 48, 16], 'blue');
        for (const x of [X(1), X(4) - 3]) box(housing, [x, 32, 16], [x + 3, 48, 32], 'blue');
        within('roller_front', [8, 42, 20], () => sweep('roll', [[-13, 42, 20], [29, 42, 20]], 3.8, 'roll', { seg: 12, cap: 'pipe' }));
        within('roller_back', [8, 42, 28], () => sweep('roll', [[-13, 42, 28], [29, 42, 28]], 3.8, 'roll', { seg: 12, cap: 'pipe' }));
        hopper('hopper', 8, 24, [22, 8], [12, 4], 48, 64, 'vessel');
        box(shape('hopper', false), [X(1), 64, 0], [X(4), 80, 32], 'vessel', ['up']);
        box(shape('hopper', false), [X(1) + 1, 79, 1], [X(4) - 1, 79.5, 31], 'cavity');
        // Washing trommel on its drive housing, spray header resting on the cross beams.
        box(shape('drive', false), [X(1), 16, 32], [X(4), 32, 48], 'cabinet');
        within('washing_drum', [8, 32, 64], () => {
            sweep('trommel', [[8, 32, 48], [8, 32, 80]], 15, 'screen', { seg: 20, cap: 'pipe' });
            for (const z of [49, 63, 77]) sweep('trommel', [[8, 32, z], [8, 32, z + 2.5]], 15.8, 'pipe', { seg: 20, noTile: true });
        });
        sweep('spray', [[8, 72, 34], [8, 72, 80]], 3, 'water', { seg: 10, cap: 'pipe' });
        for (const z of [40, 72]) box(fr, [5, 62, z - 3], [11, 69, z + 3], 'frame');
        for (const z of [52, 64]) tube('spray', 8, z, 1, 50, 69, 'water', { seg: 6 });
        // Roasting kiln with a glowing sight port and its stack.
        const kiln = shape('kiln', false);
        box(kiln, [X(1), 16, 80], [X(4), 64, 112], 'hood');
        for (const y of [28, 50]) box(fr, [X(1) - 1, y, 79], [X(4) + 1, y + 3, 113], 'frame');
        box(shape('glow_kiln', false), [X(4), 34, 88], [X(4) + 0.5, 44, 104], 'glow');
        tube('stack', 8, 104, 5, 64, 96, 'pipe', { seg: 16, cap: 'cavity', capEnds: ['end'] });
    };

    BUILD.rolling_mill = function () {
        const w = 9, X = i => cellX(w, i);
        const fr = shape('frame', false);
        // Floor-level row d: sockets at both edges, the side frames and one roll of the roller table.
        const table = (d, spinner) => {
            const z0 = d * 16;
            for (const i of [0, 8]) box(shape('cabinet', false), [X(i), 0, z0], [X(i) + 16, 16, z0 + 16], 'cabinet');
            for (const x of [-24, 40]) hbeam(fr, [x, 8, z0], [x, 8, z0 + 16], 12);
            within(spinner, [8, 8, z0 + 8], () => sweep('roll', [[-18, 8, z0 + 8], [34, 8, z0 + 8]], 5, 'roll', { seg: 12, cap: 'pipe' }));
        };
        const strip = (z0, z1) => box(shape('strip', false), [-8, 13, z0], [24, 13.5, z1], 'roll', ['north', 'south']);
        // Head: foundation, ports and the entry table.
        box(shape('base', false), [X(0), -16, 0], [X(9), 0, 32], { up: 'deck', all: 'skid' }, ['down']);
        for (const i of [0, 8]) box(shape('cabinet', false), [X(i), 0, 0], [X(i) + 16, 16, 16], 'cabinet');
        for (const i of [2, 6]) skid(w, i, i + 1, 0, 1, 0, 16);
        ports(w, 'north', 0, { 1: '...I@O...' }, -16);
        table(1, 'entry_roll');
        strip(16, 32);
        within('bay', null, () => {
            box(shape('pit', false), [X(2), -16, 32], [X(7), 0, 48], { up: 'cavity', all: 'skid' }, ['north', 'south', 'down']);
            for (const x of [-24, 40]) hbeam(fr, [x, 8, 32], [x, 8, 48], 12);
            within('bay_roll', [8, 8, 40], () => sweep('roll', [[-18, 8, 40], [34, 8, 40]], 5, 'roll', { seg: 12, cap: 'pipe' }));
            strip(32, 48);
        });
        // Four-high stand: housing posts and crosshead, backup and work rolls, screw-down on top,
        // drive spindle and motor on the +x side.
        within('stand', null, () => {
            const blue = shape('housing', false);
            for (const x of [X(2), X(6)]) box(blue, [x, -16, 32], [x + 16, 32, 48], 'blue');
            box(blue, [X(2), 32, 32], [X(7), 48, 48], 'blue');
            for (const x of [-16, 28]) box(fr, [x, -14, 34], [x + 4, 31, 46], 'frame');
            for (const [name, y, r] of [['stand_backup_bottom', -6.5, 7], ['stand_roll_bottom', 5, 3.5], ['stand_roll_top', 13.5, 3.5], ['stand_backup_top', 24.5, 7]])
                within(name, [8, y, 40], () => sweep('roll', [[-12, y, 40], [28, y, 40]], r, 'roll', { seg: 16, cap: 'pipe' }));
            tube('screw', 8, 40, 4, 48, 56, 'pipe', { seg: 12 });
            box(blue, [1, 56, 34], [15, 64, 46], 'blue');
            box(shape('base', false), [X(7), -16, 32], [X(9), 0, 48], { up: 'deck', all: 'skid' }, ['down']);
            sweep('spindle', [[32, 9, 40], [64, 9, 40]], 3, 'pipe', { seg: 10 });
            sweep('motor', [[64, 8, 40], [80, 8, 40]], 7, 'blue', { seg: 16, noTile: true, cap: 'pipe' });
            strip(32, 48);
        });
        // Tail: exit table and the down-coiler between its housing plates.
        within('tail', null, () => {
            box(shape('base', false), [X(0), -16, 112], [X(9), 0, 144], { up: 'deck', all: 'skid' }, ['down']);
            table(7, 'exit_roll');
            strip(112, 128);
            for (const i of [0, 8]) box(shape('cabinet', false), [X(i), 0, 128], [X(i) + 16, 16, 144], 'cabinet');
            ports(w, 'south', 8, { 1: '.E.......' }, -16);
            for (const x of [X(2), X(6)]) box(shape('housing', false), [x, 0, 128], [x + 16, 32, 144], 'blue');
            sweep('mandrel', [[-16, 16, 136], [32, 16, 136]], 3, 'pipe', { seg: 10 });
            within('coiler', [8, 16, 136], () => sweep('coil', [[-14, 16, 136], [30, 16, 136]], 13, 'coil', { seg: 24, noTile: true, cap: 'coil_end' }));
        });
    };

    function build(name) {
        const fs = A.fs;
        const texDir = MODELS + name + '_textures/';
        if (!fs.existsSync(texDir)) fs.mkdirSync(texDir);
        for (const f of fs.readdirSync(texDir)) fs.unlinkSync(texDir + f);
        scene.length = 0;
        BUILD[name]();
        const used = new Set();
        for (const s of scene) for (const f of s.faces) used.add(f.tex);
        newProject(Formats.free);
        Project.name = name;
        const tex = {};
        for (const key of Object.keys(TEXTURES)) {
            if (!used.has(key)) continue;
            const [w, h, draw] = TEXTURES[key];
            const t = new Tex(w, h); draw(t);
            const url = t.dataURL();
            fs.writeFileSync(texDir + key + '.png', Buffer.from(url.split(',')[1], 'base64'));
            tex[key] = new Texture({ name: key + '.png' }).fromDataURL(url).add(false);
            tex[key].uv_width = w; tex[key].uv_height = h;
        }
        let faces = 0;
        const groups = {};
        const groupFor = path => {
            if (!path.length) return null;
            const key = path.map(g => g.name).join('/');
            if (!groups[key]) {
                const parent = groupFor(path.slice(0, -1)), last = path[path.length - 1];
                const g = new Group({ name: last.name, origin: last.origin }).init();
                if (parent) g.addTo(parent);
                groups[key] = g;
            }
            return groups[key];
        };
        for (const s of scene) {
            if (!s.faces.length) continue;
            const mesh = new Mesh({ name: s.name, vertices: {}, origin: [0, 0, 0] });
            const ids = {};
            for (const key in s.verts) { const v = s.verts[key]; ids[v.id] = mesh.addVertices(v.p)[0]; }
            for (const f of s.faces) {
                const keys = f.ids.map(id => ids[id]);
                const uv = {}; keys.forEach((k, i) => uv[k] = f.uvs[i]);
                mesh.addFaces(new MeshFace(mesh, { vertices: keys, uv, texture: tex[f.tex].uuid }));
                faces++;
            }
            mesh.shading = s.smooth ? 'smooth' : 'flat';
            mesh.init();
            const g = groupFor(s.path);
            if (g) mesh.addTo(g);
        }
        Canvas.updateAll();
        const model = JSON.parse(Codecs.project.compile());
        for (const t of model.textures) { const k = t.name.replace(/\.png$/, ''); if (TEXTURES[k]) { t.uv_width = TEXTURES[k][0]; t.uv_height = TEXTURES[k][1]; } }
        fs.writeFileSync(MODELS + name + '.bbmodel', JSON.stringify(model));
        return scene.length + ' shapes, ' + faces + ' faces, ' + Object.keys(tex).length + ' textures';
    }

    window.IUCEPlants = { TEXTURES, BUILD, build, Tex };
    return 'plants loaded';
})();
