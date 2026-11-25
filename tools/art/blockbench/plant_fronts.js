// Fronts of the five industrial plant controllers, painted inside Blockbench in the look of the
// process machine fronts (machines.js): the casing or column shell of core.js, recessed windows,
// a status lamp, and a picture of what the plant does. The blocks are not animated, so each front
// is one 16x16 idle texture at textures/block/<id>.png.
// Load core.js first, then this file; run IUCEPlantFronts.all() to write every front.
(function () {
    const A = window.IUCEArt;
    const { mix, light, dark, STEEL, FRAME, GLASS, HOT, MV } = A;
    const GRAPHITE = [52, 54, 60], BRICK = [128, 70, 52], BROTH = [104, 138, 60];

    const FRONTS = {};

    // waste heat boiler and turbine: a turbine wheel behind glass, the hot tube bank under it and a
    // power bar beside it
    FRONTS.waste_heat_power_plant = g => {
        A.casing(g, 71);
        A.recess(g, 3, 3, 9, 8);
        const hub = [6, 5];
        for (const [dx, dy] of [[-2, -1], [-1, -2], [2, 1], [1, 2], [1, -2], [2, -1], [-1, 2], [-2, 1]]) g.px(hub[0] + dx, hub[1] + dy, STEEL[3]);
        g.rect(5, 4, 7, 6, STEEL[2]); g.px(6, 5, STEEL[4]);
        A.glare(g, 8, 4);
        A.recess(g, 11, 3, 12, 8);
        g.rect(11, 6, 12, 7, MV); g.rect(11, 5, 12, 5, light(MV, 0.4));
        A.recess(g, 3, 10, 9, 11, HOT[4]);
        for (let x = 4; x <= 8; x += 2) { g.px(x, 10, HOT[2]); g.px(x, 11, HOT[3]); }
        A.lamp(g, 11, 10, false);
    };

    // twin screw extruder: a feed hopper over the barrel window with the two screws side by side
    FRONTS.twin_screw_extruder = g => {
        A.casing(g, 73);
        g.rect(5, 3, 9, 3, STEEL[1]); g.rect(6, 4, 8, 4, STEEL[2]); g.px(7, 4, FRAME[0]);
        A.recess(g, 3, 5, 12, 10);
        for (let x = 4; x <= 11; x++) {
            g.px(x, 6 + (x & 1), STEEL[(x & 1) ? 2 : 4]);
            g.px(x, 8 + ((x + 1) & 1), STEEL[((x + 1) & 1) ? 2 : 4]);
        }
        g.rect(12, 6, 12, 9, mix(HOT[3], STEEL[1], 0.4));
        A.glare(g, 4, 6);
        g.rect(4, 12, 9, 12, FRAME[1]); g.rect(4, 13, 9, 13, STEEL[3]);
        A.lamp(g, 10, 11, false);
    };

    // bio-ethylene plant: a fermenter column with the broth behind its sight glass and a product line
    FRONTS.bio_ethylene_plant = g => {
        A.columnBase(g);
        A.recess(g, 4, 2, 9, 5, mix(BROTH, GLASS, 0.35));
        g.rect(5, 3, 8, 3, light(BROTH, 0.25)); g.px(6, 4, light(BROTH, 0.5)); g.px(8, 4, light(BROTH, 0.5));
        A.glare(g, 5, 2);
        A.recess(g, 4, 8, 9, 10, mix(BROTH, GLASS, 0.6));
        g.rect(5, 10, 8, 10, BROTH);
        A.pipeV(g, 11, 2, 13);
        g.rect(10, 4, 10, 4, STEEL[4]); g.rect(10, 12, 10, 12, STEEL[4]);
        A.lamp(g, 5, 12, false);
    };

    // carbide furnace: three graphite electrodes reaching into the glowing hearth
    FRONTS.carbide_furnace_plant = g => {
        A.casing(g, 79);
        for (const x of [4, 7, 10]) {
            g.rect(x, 3, x + 1, 8, GRAPHITE);
            g.rect(x, 3, x, 8, light(GRAPHITE, 0.25));
            g.rect(x, 3, x + 1, 3, STEEL[3]);
        }
        A.recess(g, 3, 9, 12, 12, HOT[4]);
        g.rect(4, 11, 11, 11, HOT[2]); g.rect(5, 10, 10, 10, HOT[3]);
        for (const x of [4, 7, 10]) { g.px(x, 10, HOT[1]); g.px(x + 1, 10, HOT[2]); }
        g.rect(13, 5, 13, 6, MV);
    };

    // coke oven battery: a row of oven doors in the refractory, glowing at the seams, with standpipes
    FRONTS.coke_oven_battery = g => {
        A.casing(g, 83);
        A.recess(g, 3, 4, 12, 12, dark(BRICK, 0.35));
        for (let y = 5; y <= 11; y++) for (let x = 4; x <= 11; x++) {
            if (y % 3 === 1 || (x + (y % 6 < 3 ? 0 : 2)) % 4 === 0) g.px(x, y, dark(BRICK, 0.55));
        }
        for (const x0 of [4, 8]) {
            g.rect(x0, 5, x0 + 2, 11, FRAME[2]);
            g.bevel(x0, 5, x0 + 2, 11, FRAME[3], FRAME[0]);
            g.px(x0 + 1, 8, STEEL[4]);
            g.rect(x0, 12, x0 + 2, 12, HOT[2]);
        }
        g.px(7, 12, HOT[3]); g.px(11, 12, HOT[3]);
        for (const x of [5, 9]) { g.rect(x, 2, x, 3, STEEL[3]); g.px(x + 1, 3, STEEL[1]); }
    };

    function write(id) { return A.save(A.paint(FRONTS[id]), 'block/' + id + '.png'); }
    function all() { return Object.keys(FRONTS).map(write); }

    window.IUCEPlantFronts = { FRONTS, write, all };
    return 'plant fronts loaded';
})();
