// The older machines' screens (macerator, generator, ...), painted like the process machine schematics:
// vanilla panel colours, the machine drawn around the slots the menus already have. Run after gui.js:
//   eval(fs.readFileSync('D:/Code/java/IUCE/tools/art/blockbench/machine_screens.js', 'utf8')); IUCEMachineScreens.paintAll()
//
// Each picture is 176 wide and covers the panel from row 16 to the row above the inventory label; the
// helpers draw in panel coordinates. The progress windows must match the Window constants of the
// screens (client.gui.MachineScreen and its subclasses).
(function () {
    const A = window.IUCEArt, Gui = window.IUCEGui, G = Gui.G;
    const { rng, mix, light, dark, HOT, COPPER } = A;
    const { save, sheet, rowsOf, panelCanvas, cutaway, pipes, roller, discAt, VD, VS, METAL_L, METAL_D, HOLE } = Gui.parts;
    const S = 'gui/machine/schematic/';
    const DUST = [150, 146, 140], LATEX = [232, 228, 210], LEAF = [96, 120, 60], BRICK = [150, 120, 100];

    // the cable from the energy gauge (x 12..18) into the machine at row y
    function cable(c, x1, y) {
        c.R(20, y - 1, x1, y - 1, VD); c.R(20, y, x1, y, COPPER[2]); c.R(20, y + 1, x1, y + 1, COPPER[0]);
    }
    const grains = (g, x0, y0, x1, y1, base, seed, spread) => {
        const r = rng(seed);
        for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) g.px(x, y, mix(dark(base, spread), light(base, spread), r()));
    };

    // ---- one input above, one output below, the machine between them (the window is 72..103 x 35..58)
    function singleBack(decor, x1) {
        const c = panelCanvas();
        cable(c, 65, 46);
        c.body(66, 33, x1 || 109, 60);
        cutaway(c, 72, 35, 103, 58, HOLE);
        if (decor) decor(c);
        return c.g;
    }

    // Macerator: toothed rolls under the feed, the dust piling up below; the motor drives them from the side.
    function maceratorBack() {
        return singleBack(c => {
            c.body(110, 39, 123, 53);
            for (let y = 41; y <= 51; y += 2) c.R(112, y, 121, y, METAL_D);
            c.R(104, 45, 109, 46, VD);
        });
    }
    function macFill() {
        const g = new G(32, 24);
        grains(g, 0, 0, 31, 23, DUST, 5, 0.18);
        g.rect(0, 0, 31, 0, light(DUST, 0.3));
        return g;
    }
    function macRolls(f) {
        const g = new G(32, 24), r = rng(9);
        roller(g, 9.5, 7.5, 6.5, f * 0.8); roller(g, 22.5, 7.5, 6.5, -f * 0.8);
        for (let i = 0; i < 6; i++) g.px(12 + Math.floor(r() * 8), 15 + ((f * 2 + i * 3) % 9), [120, 112, 100]);
        return g;
    }

    // Compressor: the ram comes down between the columns and presses the loose charge into a block.
    function compressorBack() {
        return singleBack(c => {
            c.body(60, 30, 65, 74); c.body(110, 30, 115, 74);
            c.body(58, 70, 117, 74);
        });
    }
    function compPhase(k) {
        const g = new G(32, 24), r = rng(13 + k);
        g.rect(2, 20, 29, 23, METAL_D); g.rect(2, 20, 29, 20, METAL_L);
        const press = k <= 4 ? k / 4 : (7 - k) / 3;
        const charge = k <= 4 ? Math.round(10 - 5 * k / 4) : 5, top = 20 - charge;
        const col = k >= 4 ? [184, 184, 190] : [150, 140, 120];
        for (let y = top; y < 20; y++) for (let x = 8; x <= 23; x++) g.px(x, y, mix(dark(col, 0.12), light(col, 0.12), r()));
        if (k >= 4) g.bevel(8, top, 23, 19, light(col, 0.35), dark(col, 0.35));
        const bottom = Math.round(6 + (top - 1 - 6) * press);
        g.rect(14, 0, 17, bottom - 4, METAL_L); g.rect(17, 0, 17, bottom - 4, METAL_D);
        g.rect(6, bottom - 3, 25, bottom, METAL_L); g.rect(6, bottom, 25, bottom, METAL_D); g.rect(6, bottom - 3, 6, bottom, VS);
        return g;
    }

    // Extractor: the plate squeezes the charge on a sieve; the sap drips through into the trough.
    function extractorBack() {
        return singleBack(c => {
            const { g } = c;
            for (let y = 36; y <= 56; y++) for (let x = 110; x <= 124; x++) {
                const d = Math.hypot(x - 117, y - 46);
                if (d <= 7 && (d > 5.5 || d < 1.5 || Math.abs(x - 117) < 1 || Math.abs(y - 46) < 1)) g.px(x, y - 16, d > 6.5 ? VD : METAL_D);
            }
            c.R(104, 45, 110, 46, VD);
        });
    }
    function extFill() {
        const g = new G(32, 24);
        for (let y = 0; y < 24; y++) for (let x = 0; x < 32; x++) g.px(x, y, y === 0 ? light(LATEX, 0.4) : mix(LATEX, dark(LATEX, 0.18), y / 23));
        return g;
    }
    function extPress(f) {
        const g = new G(32, 24);
        g.rect(4, 0, 27, 0, METAL_L);
        grains(g, 6, 1, 25, 5, LEAF, 21 + (f % 2), 0.2);
        g.rect(0, 6, 31, 7, METAL_D);
        for (let x = 1; x < 32; x += 3) g.px(x, 7, HOLE);
        for (let i = 0; i < 4; i++) {
            const x = 4 + i * 8, y = 9 + ((f * 3 + i * 4) % 13);
            g.px(x, y, LATEX); g.px(x, y + 1, LATEX);
        }
        return g;
    }

    // Metal former: the stock runs down between the forming rolls and comes out thin.
    function formerBack() {
        return singleBack(null, 105);
    }
    function formerPhase(k) {
        const g = new G(32, 24), end = Math.min(23, k * 3 + 2);
        g.rect(13, 0, 18, Math.min(end, 12), [200, 202, 208]); g.rect(18, 0, 18, Math.min(end, 12), METAL_D);
        roller(g, 9.5, 12, 6, k * 0.8); roller(g, 22.5, 12, 6, -k * 0.8);
        if (end > 12) { g.rect(15, 13, 16, end, [214, 216, 222]); g.rect(16, 13, 16, end, METAL_D); }
        return g;
    }

    // Electric furnace: the hearth glows between the heating coils.
    function furnaceBack() {
        return singleBack(c => {
            for (let y = 36; y <= 57; y += 3) { c.R(67, y, 70, y, COPPER[1]); c.R(105, y, 108, y, COPPER[1]); }
        });
    }
    function furnFill() {
        const g = new G(32, 24), r = rng(31);
        for (let y = 0; y < 24; y++) for (let x = 0; x < 32; x++) {
            const c = mix(HOT[4], HOT[1], y / 23);
            g.px(x, y, r() < 0.1 ? light(c, 0.15) : c);
        }
        return g;
    }
    function furnCoil(f) {
        const g = new G(32, 24);
        for (let x = 0; x < 32; x++) {
            const up = (x + f) % 4 < 2 ? 0 : 1, c = (x + f) % 8 < 4 ? HOT[0] : HOT[1];
            g.px(x, 1 + up, c); g.px(x, 21 + up, c);
        }
        return g;
    }

    // Induction furnace: both charges fall into the crucible in the copper coil; the alloy pours out below.
    function inductionBack() {
        const c = panelCanvas();
        cable(c, 61, 50);
        c.body(62, 34, 113, 64);
        const crucible = new G(44, 26);
        crucible.rect(0, 0, 43, 25, HOLE);
        grains(crucible, 4, 0, 7, 25, BRICK, 3, 0.15); grains(crucible, 36, 0, 39, 25, BRICK, 4, 0.15);
        grains(crucible, 4, 22, 39, 25, BRICK, 5, 0.15);
        cutaway(c, 66, 36, 109, 61, crucible);
        for (let y = 37; y <= 60; y += 3) { c.R(63, y, 65, y, COPPER[1]); c.R(110, y, 112, y, COPPER[1]); }
        return c.g;
    }
    function indFill() {
        const g = new G(44, 26);
        for (let y = 2; y <= 21; y++) for (let x = 8; x <= 35; x++) g.px(x, y, y === 2 ? HOT[0] : mix(HOT[1], HOT[3], (y - 2) / 19));
        return g;
    }
    function indCoil(f) {
        const g = new G(44, 26), r = rng(51);
        for (let y = 3; y < 24; y += 4) {
            const c = (y / 4 + f) % 2 < 1 ? COPPER[3] : COPPER[2];
            g.rect(0, y, 3, y, c); g.rect(40, y, 43, y, c);
        }
        for (let i = 0; i < 5; i++) g.px(10 + Math.floor(r() * 24), 21 - ((f * 2 + i * 3) % 8), HOT[0]);
        return g;
    }

    // ---- generator, energy store, ore washer, electrolyzer, planter
    const WATER = [56, 116, 180], SOIL = [110, 80, 50], GREEN = [76, 150, 60], GREEN_D = [44, 100, 40];
    const GLASS_BG = [182, 206, 198], LAMP = [255, 236, 150];
    // a copper wire along row y
    const wire = (c, x0, x1, y) => { c.R(x0, y - 1, x1, y - 1, VD); c.R(x0, y, x1, y, COPPER[2]); c.R(x0, y + 1, x1, y + 1, COPPER[0]); };
    // the pictures are 60 rows for an inventory at 87; the generator's, at 84, is 57
    function cropTo(g, h) { const o = new G(g.w, h); o.draw(g, 0, 0); return o; }
    function flameRows(w, h, f) {
        const g = new G(w, h);
        for (let x = 1; x < w - 1; x++) {
            const t = 0.55 + 0.45 * Math.sin(x * 1.3 + f * 1.7) * Math.sin(x * 0.7 - f);
            const top = Math.round((h - 3) * (1 - t));
            for (let y = h - 3; y >= top; y--) {
                const k = (h - 3 - y) / Math.max(1, h - 3 - top);
                g.px(x, y, k < 0.35 ? HOT[1] : k < 0.75 ? HOT[2] : HOT[3]);
            }
        }
        return g;
    }

    // Generator: the firebox between the fuel slots, the shaft to the dynamo, the wire to the charge slot and the store.
    function generatorBack() {
        const c = panelCanvas(), { R, body } = c;
        body(41, 34, 66, 53);
        cutaway(c, 47, 37, 60, 50, HOLE);
        body(67, 41, 72, 47);
        for (let x = 76; x <= 92; x += 4) R(x, 28, x + 1, 30, METAL_D);
        body(73, 31, 96, 56);
        cutaway(c, 76, 34, 93, 53, HOLE);
        wire(c, 97, 101, 45); wire(c, 121, 130, 45);
        return cropTo(c.g, 57);
    }
    function genFuel() {
        const g = new G(14, 14), r = rng(71);
        for (let y = 0; y < 14; y++) for (let x = 0; x < 14; x++) {
            const c = mix(HOT[4], HOT[2], y / 13);
            g.px(x, y, r() < 0.2 ? dark(c, 0.3) : c);
        }
        return g;
    }
    function genRotor(f) {
        const g = new G(18, 20), step = Math.PI / 4;
        discAt(g, 8.5, 9.5, 8, (d, a) => d > 7.3 ? VD : d < 1.5 ? METAL_L
            : (((Math.floor((a + f * Math.PI / 8) / step)) % 2) + 2) % 2 ? COPPER[2] : COPPER[1]);
        return g;
    }

    // Energy store: a cabinet of cells with the gauge across its front, the charge slots wired into it.
    function storageBack() {
        const c = panelCanvas(), { R, body } = c;
        body(60, 20, 156, 62);
        for (let x = 64; x <= 145; x += 9) {
            body(x, 23, x + 7, 31, [120, 124, 130]); R(x + 3, 22, x + 4, 22, VD);
            body(x, 51, x + 7, 59, [120, 124, 130]);
        }
        wire(c, 53, 59, 27); wire(c, 53, 59, 49);
        R(6, 62, 81, 62, VD); R(6, 63, 81, 63, METAL_D);
        return c.g;
    }

    // Ore washer: the washing drum between the feed and product slots, the water piped in from the tank.
    function washerBack() {
        const c = panelCanvas(), { R, body } = c, { h } = pipes(R);
        h(78, 125, 21);
        body(69, 24, 96, 71);
        cutaway(c, 73, 28, 92, 67, HOLE);
        for (const y of [30, 47, 64]) { R(70, y, 71, y, VD); R(94, y, 95, y, VD); }
        return c.g;
    }
    function washFill() {
        const g = new G(20, 40);
        for (let y = 0; y < 40; y++) for (let x = 0; x < 20; x++) g.px(x, y, y === 0 ? light(WATER, 0.4) : mix(light(WATER, 0.1), dark(WATER, 0.3), y / 39));
        return g;
    }
    function washTumble(f) {
        const g = new G(20, 40);
        for (let i = 0; i < 8; i++) {
            const a = f * Math.PI / 8 + i * Math.PI / 4, x = Math.round(10 + 6 * Math.cos(a)), y = Math.round(26 + 9 * Math.sin(a));
            g.rect(x - 1, y - 1, x, y, DUST);
        }
        for (let i = 0; i < 3; i++) g.px(4 + i * 6, 39 - ((f * 5 + i * 11) % 36), [200, 225, 245], 200);
        return g;
    }

    // Electrolyzer: the cell under the filler, the electrodes over the three product slots, the water tank piped in.
    function electroBack() {
        const c = panelCanvas(), { R, body } = c;
        R(36, 44, 39, 44, VD); R(36, 45, 39, 45, METAL_L); R(36, 46, 39, 46, METAL_D); R(36, 47, 39, 47, VD);
        body(40, 38, 135, 54);
        cutaway(c, 44, 40, 131, 52, HOLE);
        for (const x of [55, 87, 119]) R(x - 1, 53, x + 1, 54, COPPER[1]);
        return c.g;
    }
    function electroFill() {
        const g = new G(88, 13);
        for (let y = 0; y < 13; y++) for (let x = 0; x < 88; x++) g.px(x, y, y === 0 ? light(WATER, 0.4) : mix(light(WATER, 0.1), dark(WATER, 0.3), y / 12));
        return g;
    }
    function electroBubbles(f) {
        const g = new G(88, 13);
        for (const cx of [11, 43, 75]) {
            g.rect(cx - 1, 9, cx + 1, 12, COPPER[2]);
            for (let j = 0; j < 3; j++) {
                const y = 8 - ((f * 2 + j * 3) % 9);
                if (y >= 0) g.px(cx + (j % 2 ? 2 : -2), y, [230, 240, 255], 220);
            }
        }
        return g;
    }

    // Planter: the glass chamber between the seed and the soil under its grow lamp; the sky lamp on the left.
    function planterBack() {
        const c = panelCanvas(), { R, body, chute } = c;
        body(56, 27, 85, 63);
        cutaway(c, 60, 31, 81, 59, GLASS_BG);
        R(60, 31, 81, 31, LAMP);
        chute(86, 96, 46);
        body(21, 50, 34, 63);
        cutaway(c, 23, 52, 32, 61, HOLE);
        return c.g;
    }
    function planterPhase(k) {
        const g = new G(22, 29);
        g.rect(0, 0, 21, 28, GLASS_BG);
        g.rect(0, 0, 21, 0, LAMP);
        grains(g, 0, 24, 21, 28, SOIL, 61, 0.15);
        const h = Math.round(2 + k * 2.6), top = 23 - h;
        g.rect(10, top, 11, 23, GREEN_D);
        for (let i = 1; i <= Math.min(k, 6); i++) {
            const y = 23 - Math.round(i * h / 7), side = i % 2 ? -1 : 1, len = 2 + Math.min(3, Math.floor(k / 2));
            for (let j = 1; j <= len; j++) g.px(side > 0 ? 11 + j : 10 - j, y - Math.floor(j / 2), GREEN);
        }
        if (k >= 6) g.rect(9, top - 2, 12, top, k === 7 ? [230, 190, 60] : [200, 210, 120]);
        return g;
    }
    function skyPhase(k) {
        const NIGHT = [24, 32, 60], g = new G(10, 10);
        g.rect(0, 0, 9, 9, k ? [120, 180, 230] : NIGHT);
        if (k) discAt(g, 4.5, 4.5, 3, () => [255, 220, 90]);
        else {
            discAt(g, 4.5, 4.5, 3, () => [230, 230, 210]);
            discAt(g, 6, 3.5, 2.6, () => NIGHT);
            for (const [x, y] of [[1, 1], [8, 7], [2, 8]]) g.px(x, y, [220, 220, 255]);
        }
        return g;
    }

    function paintAll() {
        return [
            save(generatorBack(), S + 'generator.png'),
            save(sheet([genFuel(), ...rowsOf(4, f => flameRows(14, 14, f))]), S + 'generator_progress.png'),
            save(sheet([new G(18, 20), ...rowsOf(4, genRotor)]), S + 'generator_rotor.png'),
            save(storageBack(), S + 'storage.png'),
            save(washerBack(), S + 'ore_washer.png'),
            save(sheet([washFill(), ...rowsOf(4, washTumble)]), S + 'ore_washer_progress.png'),
            save(electroBack(), S + 'electrolyzer.png'),
            save(sheet([electroFill(), ...rowsOf(4, electroBubbles)]), S + 'electrolyzer_progress.png'),
            save(planterBack(), S + 'planter.png'),
            save(sheet(rowsOf(8, planterPhase)), S + 'planter_progress.png'),
            save(sheet(rowsOf(2, skyPhase)), S + 'planter_sky.png'),
            save(maceratorBack(), S + 'macerator.png'),
            save(sheet([macFill(), ...rowsOf(4, macRolls)]), S + 'macerator_progress.png'),
            save(compressorBack(), S + 'compressor.png'),
            save(sheet(rowsOf(8, compPhase)), S + 'compressor_progress.png'),
            save(extractorBack(), S + 'extractor.png'),
            save(sheet([extFill(), ...rowsOf(4, extPress)]), S + 'extractor_progress.png'),
            save(formerBack(), S + 'metal_former.png'),
            save(sheet(rowsOf(8, formerPhase)), S + 'metal_former_progress.png'),
            save(furnaceBack(), S + 'electric_furnace.png'),
            save(sheet([furnFill(), ...rowsOf(4, furnCoil)]), S + 'electric_furnace_progress.png'),
            save(inductionBack(), S + 'induction_furnace.png'),
            save(sheet([indFill(), ...rowsOf(4, indCoil)]), S + 'induction_furnace_progress.png'),
        ].join(', ');
    }

    window.IUCEMachineScreens = { paintAll };
    return 'machine screens loaded';
})();
