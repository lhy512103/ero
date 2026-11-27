// Shared pixel-painting helpers for IUCE textures, run inside Blockbench (risky_eval):
//   eval(requireNativeModule('fs').readFileSync('D:/Code/java/IUCE/tools/art/blockbench/core.js', 'utf8'))
// Defines window.IUCEArt. The palette and shapes follow tools/art/m1_machines.py so the new
// machines sit next to the M1 ones without looking out of place.
(function () {
    const fs = requireNativeModule('fs');
    const ROOT = 'D:/Code/java/IUCE/';
    const TEX = ROOT + 'src/main/resources/assets/iuce/textures/';

    function rng(seed) {
        let a = seed >>> 0;
        return function () {
            a = (a + 0x6D2B79F5) >>> 0;
            let t = a;
            t = Math.imul(t ^ (t >>> 15), t | 1);
            t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        };
    }
    function hex(s) {
        s = s.replace('#', '');
        return [parseInt(s.substr(0, 2), 16), parseInt(s.substr(2, 2), 16), parseInt(s.substr(4, 2), 16)];
    }
    function clamp(v) { return Math.max(0, Math.min(255, v)); }
    function mix(a, b, t) { t = Math.max(0, Math.min(1, t)); return [0, 1, 2].map(i => a[i] + (b[i] - a[i]) * t); }
    function shade(c, f) { return [clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f)]; }
    function light(c, t) { return mix(c, [255, 255, 255], t); }
    function dark(c, t) { return mix(c, [0, 0, 0], t); }

    class Img {
        constructor(h) { this.w = 16; this.h = h || 16; this.d = new Uint8ClampedArray(16 * this.h * 4); this.oy = 0; }
        px(x, y, c, a) {
            x = Math.round(x); y = Math.round(y);
            if (!c || x < 0 || x > 15 || y < 0 || y > 15) return;
            const i = ((y + this.oy) * 16 + x) * 4;
            const alpha = a === undefined ? (c[3] === undefined ? 255 : c[3]) : a;
            this.d[i] = clamp(c[0]); this.d[i + 1] = clamp(c[1]); this.d[i + 2] = clamp(c[2]); this.d[i + 3] = alpha;
        }
        get(x, y) {
            if (x < 0 || x > 15 || y < 0 || y > 15) return null;
            const i = ((y + this.oy) * 16 + x) * 4;
            return [this.d[i], this.d[i + 1], this.d[i + 2], this.d[i + 3]];
        }
        alpha(x, y) { const p = this.get(x, y); return p ? p[3] : 0; }
        rect(x0, y0, x1, y1, c) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) this.px(x, y, c); }
        bevel(x0, y0, x1, y1, l, d) {
            this.rect(x0, y0, x1, y0, l); this.rect(x0, y0, x0, y1, l);
            this.rect(x0, y1, x1, y1, d); this.rect(x1, y0, x1, y1, d);
        }
        // Blend a colour over what is already there.
        tint(x, y, c, t) { const p = this.get(x, y); if (p && p[3]) this.px(x, y, mix(p, c, t)); }
        canvas() {
            const c = document.createElement('canvas'); c.width = 16; c.height = this.h;
            const ctx = c.getContext('2d'); const id = ctx.createImageData(16, this.h);
            id.data.set(this.d); ctx.putImageData(id, 0, 0); return c;
        }
        dataURL() { return this.canvas().toDataURL('image/png'); }
    }

    // A single 16x16 image, or an 8-frame vertical strip when animated.
    function paint(draw, frames) {
        frames = frames || 1;
        const img = new Img(16 * frames);
        for (let f = 0; f < frames; f++) { img.oy = f * 16; draw(img, f); }
        img.oy = 0;
        return img;
    }

    function save(img, relPath, frametime) {
        const b64 = img.dataURL().split(',')[1];
        fs.writeFileSync(TEX + relPath, Buffer.from(b64, 'base64'));
        const meta = TEX + relPath + '.mcmeta';
        if (img.h > 16) fs.writeFileSync(meta, '{\n  "animation": {\n    "frametime": ' + (frametime || 2) + '\n  }\n}\n');
        else if (fs.existsSync(meta)) fs.unlinkSync(meta);
        return relPath;
    }

    // Replaces the embedded texture of the same file name inside a .bbmodel so the sources match.
    function syncModel(modelPath, name, img) {
        const path = ROOT + modelPath;
        if (!fs.existsSync(path)) return false;
        const model = JSON.parse(fs.readFileSync(path, 'utf8'));
        let hit = false;
        for (const t of model.textures || []) {
            if (t.name === name + '.png' || t.name === name) {
                t.source = img.dataURL();
                t.width = 16; t.height = img.h; t.uv_width = 16; t.uv_height = 16;
                if (img.h > 16) t.frame_time = 2;
                hit = true;
            }
        }
        if (hit) fs.writeFileSync(path, JSON.stringify(model));
        return hit;
    }

    const STEEL = [[46, 50, 56], [78, 84, 92], [112, 118, 126], [148, 154, 162], [190, 195, 200]];
    const FRAME = [[30, 32, 36], [54, 58, 64], [78, 82, 90], [104, 110, 118], [140, 146, 152]];
    const GLASS = [18, 24, 30];
    const MV = [214, 124, 40];
    const HOT = [[255, 244, 190], [255, 200, 96], [242, 132, 44], [176, 66, 28], [86, 38, 26]];
    const COPPER = [[110, 58, 34], [170, 96, 52], [222, 146, 92], [246, 196, 150]];

    function noise(img, x0, y0, x1, y1, lo, hi, seed, base, spread) {
        const r = rng(seed); base = base === undefined ? 0.5 : base; spread = spread === undefined ? 0.07 : spread;
        for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) img.px(x, y, mix(lo, hi, base + (r() * 2 - 1) * spread));
    }
    function bolt(img, x, y) { img.px(x, y, STEEL[4]); img.px(x + 1, y, STEEL[2]); img.px(x, y + 1, STEEL[2]); img.px(x + 1, y + 1, FRAME[0]); }
    function casing(img, seed) {
        noise(img, 0, 0, 15, 15, STEEL[1], STEEL[3], seed);
        img.bevel(0, 0, 15, 15, FRAME[2], FRAME[0]);
        img.bevel(1, 1, 14, 14, STEEL[4], STEEL[1]);
        [[2, 2], [12, 2], [2, 12], [12, 12]].forEach(p => bolt(img, p[0], p[1]));
    }
    function vcol(x, l, r) { l = l || 0; r = r === undefined ? 15 : r; const t = (x - l) / (r - l); return 0.35 + 0.55 * Math.sin(Math.PI * Math.min(1, t * 1.15)) - 0.2 * t; }
    function vessel(img, seed, bands) {
        const r = rng(seed);
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) img.px(x, y, mix(STEEL[0], STEEL[4], vcol(x) + (r() * 2 - 1) * 0.03));
        for (const row of bands) for (let x = 0; x < 16; x++) {
            const s = vcol(x);
            img.px(x, row, mix(STEEL[1], STEEL[4], s + 0.15));
            img.px(x, row + 1, mix(FRAME[0], STEEL[2], s));
        }
    }
    function tankBase(img, seed) { vessel(img, 61 + seed, [1, 14]); }
    function columnBase(img) {
        vessel(img, 41, [1, 6, 11]);
        for (const row of [1, 6, 11]) for (const x of [2, 6, 10, 13]) img.px(x, row, STEEL[4]);
        img.rect(0, 0, 0, 15, FRAME[1]); img.rect(15, 0, 15, 15, FRAME[0]);
    }
    // An inset dark area with a lit lower-right lip, as on the M1 windows.
    function recess(img, x0, y0, x1, y1, fill) {
        img.rect(x0, y0, x1, y1, fill || GLASS);
        img.bevel(x0, y0, x1, y1, FRAME[0], STEEL[4]);
    }
    function glare(img, x, y) { img.px(x, y, [150, 170, 180]); }
    function lamp(img, x, y, on) {
        img.px(x, y, on ? [120, 240, 120] : [34, 48, 34]);
        img.px(x + 1, y, on ? [60, 190, 70] : [26, 36, 26]);
        img.px(x, y + 1, on ? [60, 190, 70] : [26, 36, 26]);
        img.px(x + 1, y + 1, on ? [30, 120, 40] : [20, 26, 20]);
    }
    function tier(img, voltage, y, x0, x1) {
        if (voltage !== 'MV') return;
        y = y === undefined ? 14 : y; x0 = x0 === undefined ? 2 : x0; x1 = x1 === undefined ? 10 : x1;
        for (let x = x0; x <= x1; x++) img.px(x, y, (x >> 1) % 2 === 0 ? MV : FRAME[0]);
    }
    function sightGlass(img, x0, y0, x1, y1, liquid, level, f, bubbles, seed) {
        img.rect(x0, y0, x1, y1, GLASS);
        const surface = y1 - Math.round((y1 - y0) * level);
        for (let y = y0 + 1; y < y1; y++) for (let x = x0 + 1; x < x1; x++) {
            if (y >= surface) {
                const depth = (y - surface) / Math.max(1, y1 - surface);
                img.px(x, y, mix(liquid, dark(liquid, 0.6), depth * 0.7));
            } else img.px(x, y, mix(GLASS, [70, 86, 96], x === x0 + 1 ? 0.25 : 0));
        }
        if (level > 0) for (let x = x0 + 1; x < x1; x++) img.px(x, surface, light(liquid, 0.35));
        if (bubbles) {
            const r = rng(seed); const height = y1 - surface - 1;
            for (let i = 0; i < (bubbles === true ? 3 : bubbles); i++) {
                const bx = x0 + 1 + Math.floor(r() * (x1 - x0 - 1));
                const start = Math.floor(r() * 8);
                const by = y1 - 1 - (start + f) % Math.max(1, height);
                if (by > surface) img.px(bx, by, light(liquid, 0.6));
            }
        }
        glare(img, x0 + 1, y0 + 1);
        img.bevel(x0, y0, x1, y1, FRAME[0], STEEL[4]);
        return surface;
    }
    function roundWindow(img, cx, cy, radius, fill) {
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const d = Math.hypot(x - cx, y - cy);
            if (d < radius - 0.8) img.px(x, y, fill(x, y, d, Math.atan2(y - cy, x - cx) * 180 / Math.PI));
            else if (d < radius + 0.4) img.px(x, y, x + y < cx + cy ? FRAME[0] : STEEL[4]);
        }
    }
    // A small round pressure dial; value 0..1 swings the needle from lower left to lower right.
    function gauge(img, cx, cy, value) {
        for (let y = cy - 2; y <= cy + 2; y++) for (let x = cx - 2; x <= cx + 2; x++) {
            const d = Math.hypot(x - cx, y - cy);
            if (d < 1.6) img.px(x, y, [230, 228, 214]);
            else if (d < 2.5) img.px(x, y, x + y < cx + cy ? FRAME[0] : STEEL[4]);
        }
        const a = (225 - 270 * value) * Math.PI / 180;
        img.px(cx, cy, [40, 30, 24]);
        img.px(cx + Math.round(Math.cos(a)), cy - Math.round(Math.sin(a)), [200, 40, 30]);
    }
    function heaterRow(img, x0, x1, y, active, f) {
        for (let x = x0; x <= x1; x++) {
            const hot = active && (x + (f >> 1)) % 3 !== 0;
            img.px(x, y, hot ? HOT[2] : [70, 40, 30]);
        }
    }
    function pipeH(img, x0, x1, y) { img.rect(x0, y, x1, y, STEEL[4]); img.rect(x0, y + 1, x1, y + 1, STEEL[1]); }
    function pipeV(img, x, y0, y1) { img.rect(x, y0, x, y1, STEEL[4]); img.rect(x + 1, y0, x + 1, y1, STEEL[1]); }
    // Spinning blades for round windows: n blades, phase in degrees.
    function blades(n, phase, on, off, hub) {
        const span = 360 / n;
        return function (x, y, d, ang) {
            if (d < 1.0) return hub || STEEL[3];
            return ((ang - phase) % span + span) % span < span / 3 ? on : off;
        };
    }

    window.IUCEArt = {
        fs, ROOT, TEX, rng, hex, mix, shade, light, dark, clamp, Img, paint, save, syncModel,
        STEEL, FRAME, GLASS, MV, HOT, COPPER,
        noise, bolt, casing, vcol, vessel, tankBase, columnBase, recess, glare, lamp, tier,
        sightGlass, roundWindow, gauge, heaterRow, pipeH, pipeV, blades,
    };
    return 'core loaded';
})();
