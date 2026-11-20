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
            const c = A.paint(g => crate(g, hex(colour)));
            A.save(c, 'block/crate_' + name + '.png');
            A.syncModel('tools/models/m3/crate.bbmodel', 'crate_' + name, c);
            const b = A.paint(g => brick(g, hex(colour)));
            A.save(b, 'block/toy_brick_' + name + '.png');
            A.syncModel('tools/models/m3/toy_brick.bbmodel', 'toy_brick_' + name, b);
            out.push(name);
        }
        // undyed: natural translucent-looking polypropylene and an off-white brick
        A.save(A.paint(g => crate(g, [92, 150, 140])), 'block/crate.png');
        A.save(A.paint(g => brick(g, [222, 226, 214])), 'block/toy_brick.png');
        const ls = A.paint(limestone); A.save(ls, 'block/limestone.png');
        A.syncModel('tools/models/m4/limestone.bbmodel', 'limestone', ls);
        const rs = A.paint(rockSaltOre); A.save(rs, 'block/rock_salt_ore.png');
        A.syncModel('tools/models/m4/rock_salt_ore.bbmodel', 'rock_salt_ore', rs);
        return out.length * 2 + 4;
    }

    window.IUCEBlocks = { DYES, crate, brick, limestone, rockSaltOre, all };
    return 'blocks loaded';
})();
