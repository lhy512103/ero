// The plant and mill controllers' top, back and bottom, painted inside Blockbench.
// Load core.js first, then this file; run IUCEControllers.all() to write every face.
// The controllers' front, vents and the hatches all sit in the casing frame of core.js (dark outer
// bevel, light inner bevel, a bolt in each corner); these faces use the same frame, so a controller
// no longer shows the bare structure plate (machine/plant/casing) on its top and back.
(function () {
    const A = window.IUCEArt;
    const { mix, dark, STEEL, FRAME, GLASS } = A;
    const AMBER = [214, 150, 50], CABLE = [46, 50, 56];

    // a round cable gland seen from above: a nut ring with the cable in the middle
    function gland(g, x, y, core) {
        g.rect(x, y, x + 2, y + 2, STEEL[3]);
        g.px(x, y, STEEL[4]); g.px(x + 2, y + 2, FRAME[1]); g.px(x + 2, y, STEEL[2]); g.px(x, y + 2, STEEL[2]);
        g.px(x + 1, y + 1, core);
    }

    const FACES = {
        // top: a recessed gland plate where the power and data cables come in
        controller_top(g) {
            A.casing(g, 31);
            A.recess(g, 4, 5, 11, 10, mix(FRAME[1], FRAME[2], 0.4));
            gland(g, 5, 6, CABLE);
            gland(g, 8, 6, CABLE);
            g.px(6, 9, AMBER); g.px(9, 9, [72, 150, 206]);
            g.rect(5, 4, 10, 4, dark(STEEL[2], 0.15));
        },
        // back: a raised service door with a hinge, a latch and a rating plate
        controller_back(g) {
            A.casing(g, 37);
            g.rect(4, 3, 11, 12, mix(STEEL[2], STEEL[3], 0.45));
            g.bevel(4, 3, 11, 12, STEEL[4], FRAME[0]);
            g.px(4, 5, FRAME[1]); g.px(4, 10, FRAME[1]);
            g.rect(10, 7, 10, 8, FRAME[0]); g.px(9, 7, STEEL[4]);
            g.rect(6, 5, 8, 6, GLASS); g.px(6, 5, AMBER); g.px(7, 6, dark(AMBER, 0.4));
            for (let y = 9; y <= 10; y++) g.rect(6, y, 8, y, (y & 1) ? STEEL[1] : STEEL[3]);
        },
        // bottom: the plain framed plate, as on the process machines' boxes
        controller_bottom(g) {
            A.casing(g, 43);
            g.rect(4, 7, 11, 7, STEEL[1]); g.rect(4, 8, 11, 8, STEEL[3]);
        },
    };

    function write(id) {
        const img = A.paint(FACES[id]);
        return A.save(img, 'block/machine/plant/' + id + '.png');
    }

    function all() { return Object.keys(FACES).map(write); }

    window.IUCEControllers = { FACES, write, all };
    return 'controllers loaded';
})();
