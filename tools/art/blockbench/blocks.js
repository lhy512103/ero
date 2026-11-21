// Crates, toy bricks and the two new stones, painted inside Blockbench after core.js.
// IUCEBlocks.all() writes every texture and updates the matching .bbmodel sources.
(function () {
    const A = window.IUCEArt;
    const { mix, light, dark, hex } = A;

    // Dye colours tuned for moulded plastic: a little brighter and cleaner than vanilla wool.
    const DYES = {
        white: '#e8eae6', orange: '#e8781e', magenta: '#c04ab8', light_blue: '#4aa8dc',
        yellow: '#f0c41e', lime: '#78c41e', pink: '#ec8cac', gray: '#4c5054',
        light_gray: '#9a9c98', cyan: '#168c96', purple: '#7a32ac', blue: '#3442a0',
        brown: '#7a4e2c', green: '#4e6a1c', red: '#b02a24', black: '#22242a',
    };

    // A stackable tote: thick rim, moulded ribs, a hand hole and a label plate.
    function crate(g, base) {
        const lum = (base[0] * 0.3 + base[1] * 0.59 + base[2] * 0.11) / 255;
        const hi = light(base, lum > 0.7 ? 0.25 : 0.3);
        const lo = dark(base, lum < 0.2 ? 0.35 : 0.3);
        const deep = dark(base, lum < 0.2 ? 0.55 : 0.5);
        g.rect(0, 0, 15, 15, base);
        // the rim
        g.rect(0, 0, 15, 2, hi); g.rect(0, 2, 15, 2, lo);
        g.rect(0, 0, 15, 0, light(base, 0.45));
        g.rect(0, 15, 15, 15, deep);
        g.rect(0, 0, 0, 15, hi); g.rect(15, 0, 15, 15, deep);
        // vertical ribs on the walls
        for (const x of [3, 12]) { g.rect(x, 3, x, 14, hi); g.rect(x + 1, 3, x + 1, 14, lo); }
        // hand hole under the rim
        g.rect(6, 4, 9, 5, deep); g.rect(6, 4, 9, 4, dark(base, 0.7));
        g.rect(5, 4, 5, 5, lo); g.rect(10, 4, 10, 5, hi);
        // label plate
        g.rect(6, 9, 9, 12, [228, 226, 214]); g.rect(6, 9, 9, 9, [246, 244, 236]);
        g.rect(7, 10, 8, 10, [150, 150, 140]); g.px(7, 11, [150, 150, 140]);
        g.bevel(5, 8, 10, 13, lo, hi);
    }

    function crateTones(base) {
        const lum = (base[0] * 0.3 + base[1] * 0.59 + base[2] * 0.11) / 255;
        return {
            hi: light(base, lum > 0.7 ? 0.25 : 0.3),
            lo: dark(base, lum < 0.2 ? 0.35 : 0.3),
            deep: dark(base, lum < 0.2 ? 0.55 : 0.5),
            rim: light(base, lum > 0.7 ? 0.4 : 0.45),
        };
    }

    // Wall of the tote: a thick top rim, corner posts, vertical ribs and a recessed hand slot.
    function crateSide(g, base) {
        const t = crateTones(base);
        const r = A.rng(91);
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) g.px(x, y, mix(base, r() < 0.5 ? t.hi : t.lo, 0.06));
        g.rect(0, 0, 15, 1, t.rim); g.rect(0, 2, 15, 3, t.hi); g.rect(0, 4, 15, 4, t.lo);
        g.rect(0, 15, 15, 15, t.deep); g.rect(0, 14, 15, 14, t.lo);
        g.rect(0, 0, 1, 15, t.lo); g.rect(14, 0, 15, 15, t.lo);
        g.rect(0, 0, 0, 15, t.hi); g.rect(15, 0, 15, 15, t.deep);
        for (const x of [3, 12]) { g.rect(x, 5, x, 13, t.hi); g.rect(x + 1, 5, x + 1, 13, t.lo); }
        g.rect(5, 6, 10, 8, t.deep); g.rect(5, 6, 10, 6, dark(base, 0.75));
        g.rect(5, 9, 10, 9, t.hi);
        g.rect(6, 11, 9, 12, t.lo); g.rect(6, 11, 9, 11, t.hi);
    }

    // Closed lid seen from above: a raised border, a flat panel and a latch.
    function crateTop(g, base) {
        const t = crateTones(base);
        g.rect(0, 0, 15, 15, base);
        g.rect(0, 0, 15, 1, t.rim); g.rect(0, 0, 1, 15, t.rim);
        g.rect(14, 1, 15, 15, t.lo); g.rect(1, 14, 15, 15, t.lo);
        g.rect(2, 2, 13, 13, mix(base, t.hi, 0.35));
        g.bevel(2, 2, 13, 13, t.lo, t.hi);
        for (const x of [4, 11]) g.rect(x, 4, x, 11, t.lo);
        g.rect(6, 6, 9, 9, [70, 74, 80]); g.bevel(6, 6, 9, 9, [150, 154, 160], [30, 32, 36]);
        g.px(7, 7, [200, 204, 210]); g.px(8, 8, [40, 42, 46]);
        g.rect(1, 7, 1, 8, t.deep); g.rect(14, 7, 14, 8, t.deep);
    }

    // Inside of the open tote: a dark well with a lit rim.
    function crateInner(g, base) {
        const t = crateTones(base);
        g.rect(0, 0, 15, 15, t.rim);
        g.rect(0, 0, 15, 0, t.rim); g.rect(1, 1, 14, 14, t.hi);
        for (let y = 2; y <= 13; y++) for (let x = 2; x <= 13; x++) {
            const d = Math.min(x - 2, 13 - x, y - 2, 13 - y);
            g.px(x, y, mix(dark(base, 0.8), dark(base, 0.55), Math.min(1, d / 3)));
        }
        g.rect(2, 2, 13, 2, dark(base, 0.88)); g.rect(2, 2, 2, 13, dark(base, 0.8));
        g.rect(14, 1, 15, 15, t.lo); g.rect(1, 14, 15, 15, t.lo);
    }

    // A glossy studded brick; every face shares this texture so it stays uniform at the edges.
    function brick(g, base) {
        const lum = (base[0] * 0.3 + base[1] * 0.59 + base[2] * 0.11) / 255;
        const hi = light(base, lum > 0.8 ? 0.0 : 0.28);
        const lo = dark(base, lum > 0.8 ? 0.16 : 0.22);
        const edge = dark(base, lum > 0.8 ? 0.28 : 0.4);
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const t = (x + y) / 30;
            g.px(x, y, mix(light(base, lum > 0.8 ? 0 : 0.08), dark(base, 0.08), t));
        }
        g.rect(0, 0, 15, 0, hi); g.rect(0, 0, 0, 15, hi);
        g.rect(0, 15, 15, 15, edge); g.rect(15, 0, 15, 15, edge);
        g.rect(1, 14, 14, 14, lo); g.rect(14, 1, 14, 14, lo);
        // the gloss streak of injection-moulded ABS
        for (let i = 0; i < 4; i++) g.px(2 + i, 4 - i, light(base, 0.5));
        g.px(2, 2, light(base, 0.6));
    }

    function stone(g, seed, base, spots) {
        const r = A.rng(seed);
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const v = r();
            g.px(x, y, v < 0.18 ? dark(base, 0.12) : v > 0.85 ? light(base, 0.12) : base);
        }
        // bedding planes, as in sedimentary limestone
        for (const row of [4, 10]) for (let x = 0; x < 16; x++) {
            const y = row + ((x >> 2) % 2);
            g.px(x, y, dark(base, 0.22));
            g.px(x, y - 1, light(base, 0.08));
        }
        if (spots) spots(g, r);
    }

    function limestone(g) {
        stone(g, 61, [206, 200, 180], (g, r) => {
            // shell fossils
            for (const [x, y] of [[3, 7], [11, 13], [12, 6]]) {
                g.px(x, y, [236, 232, 214]); g.px(x + 1, y, [176, 170, 150]); g.px(x, y + 1, [176, 170, 150]);
            }
        });
    }

    function rockSaltOre(g) {
        // stone matrix like vanilla, with pink-white halite crystals
        const r = A.rng(62);
        const base = [126, 126, 126];
        for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
            const v = r();
            g.px(x, y, v < 0.2 ? dark(base, 0.15) : v > 0.82 ? light(base, 0.12) : base);
        }
        const halite = [[244, 236, 236], [230, 200, 204], [196, 160, 166], [150, 118, 124]];
        const crystals = [[2, 2, 3], [9, 3, 2], [5, 8, 3], [11, 10, 3], [2, 12, 2], [13, 1, 2]];
        for (const [x0, y0, s] of crystals) {
            for (let y = 0; y < s; y++) for (let x = 0; x < s; x++) g.px(x0 + x, y0 + y, halite[1]);
            g.rect(x0, y0, x0 + s - 1, y0, halite[0]); g.rect(x0, y0, x0, y0 + s - 1, halite[0]);
            g.rect(x0, y0 + s, x0 + s, y0 + s, halite[3]); g.rect(x0 + s, y0, x0 + s, y0 + s, halite[3]);
            if (s > 2) g.px(x0 + s - 1, y0 + s - 1, halite[2]);
        }
    }

    function all() {
        const out = [];
        for (const [name, colour] of Object.entries(DYES)) {
            const c = A.paint(g => crateSide(g, hex(colour)));
            A.save(c, 'block/crate_' + name + '.png');
            A.syncModel('tools/models/m3/crate.bbmodel', 'crate_' + name, c);
            A.save(A.paint(g => crateTop(g, hex(colour))), 'block/crate_' + name + '_top.png');
            A.save(A.paint(g => crateInner(g, hex(colour))), 'block/crate_' + name + '_inner.png');
            const b = A.paint(g => brick(g, hex(colour)));
            A.save(b, 'block/toy_brick_' + name + '.png');
            A.syncModel('tools/models/m3/toy_brick.bbmodel', 'toy_brick_' + name, b);
            out.push(name);
        }
        // undyed: natural translucent-looking polypropylene and an off-white brick
        A.save(A.paint(g => crateSide(g, [92, 150, 140])), 'block/crate.png');
        A.save(A.paint(g => crateTop(g, [92, 150, 140])), 'block/crate_top.png');
        A.save(A.paint(g => crateInner(g, [92, 150, 140])), 'block/crate_inner.png');
        A.save(A.paint(g => brick(g, [222, 226, 214])), 'block/toy_brick.png');
        const ls = A.paint(limestone); A.save(ls, 'block/limestone.png');
        A.syncModel('tools/models/m4/limestone.bbmodel', 'limestone', ls);
        const rs = A.paint(rockSaltOre); A.save(rs, 'block/rock_salt_ore.png');
        A.syncModel('tools/models/m4/rock_salt_ore.bbmodel', 'rock_salt_ore', rs);
        return out.length * 2 + 4;
    }

    window.IUCEBlocks = { DYES, crate, crateSide, crateTop, crateInner, brick, limestone, rockSaltOre, all };
    return 'blocks loaded';
})();
