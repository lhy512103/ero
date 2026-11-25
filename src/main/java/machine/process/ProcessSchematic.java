package machine.process;

import machine.process.ProcessLayout.Box;
import machine.process.ProcessLayout.Pos;
import machine.process.ProgressStyle.Mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A machine whose process page is a picture of the machine itself, the way the vanilla brewing stand
 * or furnace is drawn, instead of the generic left-to-right row. The picture
 * {@code textures/gui/process/schematic/<id>.png} is {@link #WIDTH} x {@link #HEIGHT} and covers the
 * panel from row {@link #TOP}; the slots and tank gauges sit where the picture leaves room for them,
 * the tanks inside the vessels the picture draws around them.
 * <p>
 * The progress shows in {@link #window()}, {@link #windowW()} x {@link #windowH()}, from the sheet
 * {@code <id>_progress.png}, in rows of the window's size:
 * </p>
 * <ul>
 *   <li>{@link Mode#RIGHT} / {@link Mode#UP}: row 0 is revealed from the left or from the bottom as the
 *   run advances; the next {@link #frames()} rows are an overlay that loops while the machine runs. When
 *   idle the overlay shows frame 0 if {@link #idleOverlay()}, otherwise nothing.</li>
 *   <li>{@link Mode#PHASE}: {@link #frames()} rows, one per stage of the run; row 0 is the idle picture.</li>
 * </ul>
 * Positions are item positions, as in {@link ProcessLayout}.
 */
public record ProcessSchematic(String id, int energyX, List<Pos> inputs, List<Pos> outputs, Pos catalyst, Pos card,
                               List<Box> tanks, Pos window, int windowW, int windowH, Mode mode, int frames,
                               boolean idleOverlay) {
    public static final int TOP = 16, WIDTH = 176, HEIGHT = 60;

    private static final Pos IN = new Pos(26, 40), OUT = new Pos(136, 40);

    /** One input on the left, one output on the right, the machine in between. */
    private static ProcessSchematic line(String id, Pos catalyst, Pos window, int w, int h, Mode mode, int frames, boolean idle) {
        return new ProcessSchematic(id, 8, List.of(IN), List.of(OUT), catalyst, null, List.of(), window, w, h, mode, frames, idle);
    }

    /**
     * The reactor series: the polymerization reactor's picture for any count of slots and drums
     * (gui.js reactorBack paints it from the same counts). Input drums left of the vessel, output drums
     * right of it; item inputs and then the catalyst down the left column, outputs up the right one.
     */
    private static ProcessSchematic reactor(String id, int in, int out, int fluidsIn, int fluidsOut, boolean catalyst, boolean stir) {
        int[] ys = {17, 37, 57};
        List<Pos> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (int i = 0; i < in; i++) inputs.add(new Pos(50, ys[i]));
        for (int i = 0; i < out; i++) outputs.add(new Pos(114, ys[2 - i]));
        List<Box> tanks = new ArrayList<>();
        for (int i = 0; i < fluidsIn; i++) tanks.add(new Box(19 + 15 * i, 24, 12, 46));
        for (int i = 0; i < fluidsOut; i++) tanks.add(new Box(151 - 15 * (fluidsOut - 1 - i), 24, 12, 46));
        return new ProcessSchematic(id, 8, List.copyOf(inputs), List.copyOf(outputs), catalyst ? new Pos(50, ys[in]) : null, null,
            List.copyOf(tanks), new Pos(74, 34), 32, 33, Mode.UP, 4, stir);
    }

    /**
     * The column series: the distillation column's picture for any count of drums (gui.js columnBack
     * paints it from the same counts). Feed drums left of the column, product drums right of it, the
     * solid products in the slots beside the column.
     */
    private static ProcessSchematic column(String id, int out, int fluidsIn, int fluidsOut) {
        List<Pos> outputs = new ArrayList<>();
        for (int i = 0; i < out; i++) outputs.add(new Pos(98 + 19 * i, 37));
        List<Box> tanks = new ArrayList<>();
        for (int i = 0; i < fluidsIn; i++) tanks.add(new Box(19 + 15 * i, 24, 12, 46));
        for (int i = 0; i < fluidsOut; i++) tanks.add(new Box(151 - 15 * (fluidsOut - 1 - i), 24, 12, 46));
        return new ProcessSchematic(id, 8, List.of(), List.copyOf(outputs), null, null,
            List.copyOf(tanks), new Pos(70, 22), 20, 47, Mode.UP, 4, false);
    }

    /**
     * The kiln series: the lime kiln's brick shaft for any count of slots and drums (gui.js
     * kilnSeriesBack paints it from the same counts). Fuel drums on the left, the charge slots beside
     * them with the catalyst below, the products to the right of the shaft and the gas drums beyond.
     */
    private static ProcessSchematic kiln(String id, int in, int out, int fluidsIn, int fluidsOut, boolean catalyst) {
        int x = fluidsIn > 0 ? 22 + 15 * fluidsIn : 26;
        int[] inYs = {20, 38}, outYs = {52, 32};
        List<Pos> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (int i = 0; i < in; i++) inputs.add(new Pos(x, inYs[i]));
        for (int i = 0; i < out; i++) outputs.add(new Pos(114, outYs[i]));
        List<Box> tanks = new ArrayList<>();
        for (int i = 0; i < fluidsIn; i++) tanks.add(new Box(19 + 15 * i, 24, 12, 46));
        for (int i = 0; i < fluidsOut; i++) tanks.add(new Box(151 - 15 * (fluidsOut - 1 - i), 24, 12, 46));
        return new ProcessSchematic(id, 8, List.copyOf(inputs), List.copyOf(outputs), catalyst ? new Pos(x, 57) : null, null,
            List.copyOf(tanks), new Pos(74, 26), 24, 41, Mode.UP, 4, false);
    }

    /**
     * The tank series: the fermenter's jacketed tank for any count of slots and drums (gui.js
     * vesselBack paints it from the same counts). Item feeds on the lid with the catalyst between them,
     * liquid drums either side, the solid product at the lower right.
     */
    private static ProcessSchematic vessel(String id, int in, int out, int fluidsIn, int fluidsOut, boolean catalyst) {
        int[] xs = {56, 94};
        List<Pos> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (int i = 0; i < in; i++) inputs.add(new Pos(xs[i], 17));
        for (int i = 0; i < out; i++) outputs.add(new Pos(116, 52));
        List<Box> tanks = new ArrayList<>();
        for (int i = 0; i < fluidsIn; i++) tanks.add(new Box(19 + 15 * i, 24, 12, 46));
        for (int i = 0; i < fluidsOut; i++) tanks.add(new Box(151 - 15 * (fluidsOut - 1 - i), 24, 12, 46));
        return new ProcessSchematic(id, 8, List.copyOf(inputs), List.copyOf(outputs), catalyst ? new Pos(75, 17) : null, null,
            List.copyOf(tanks), new Pos(58, 40), 52, 27, Mode.UP, 4, false);
    }

    private static final Map<String, ProcessSchematic> ALL = index(List.of(
        // pellets drop through the hopper into the barrel, the core wire comes down into the die, the product leaves on the right
        new ProcessSchematic("single_screw_extruder", 8,
            List.of(new Pos(32, 20), new Pos(122, 20)), List.of(new Pos(150, 47)), new Pos(122, 47), null,
            List.of(), new Pos(52, 51), 64, 10, Mode.RIGHT, 6, true),
        // the hot product runs through a sprayed water trough and comes out cold
        line("cooling_tank", null, new Pos(44, 47), 88, 10, Mode.RIGHT, 4, false),
        // the platen comes down on the mould between the press columns, steam heats it, then it lifts
        line("vulcanizer", new Pos(80, 44), new Pos(60, 24), 56, 19, Mode.PHASE, 8, false),
        // sheets hang in the chamber over a log fire; the smoke fills it from below and cures them
        new ProcessSchematic("smokehouse", 8, List.of(new Pos(26, 26), new Pos(26, 52)), List.of(OUT),
            null, null, List.of(), new Pos(56, 31), 60, 38, Mode.UP, 4, false),
        // the film runs through the nip rollers and over a guide onto the roll, which grows
        line("film_winder", null, new Pos(70, 18), 60, 48, Mode.PHASE, 8, false),
        // the tracks pull the pipe through, the saw cuts it into lengths
        line("haul_off_cutter", null, new Pos(46, 30), 84, 34, Mode.RIGHT, 4, true),
        // the spindle travels along the rail and cuts the pocket into the stock in the vise
        line("cnc_machining_center", null, new Pos(52, 23), 72, 42, Mode.PHASE, 8, false),
        // feed drums on the left, monomers and catalyst charged from the left, the card reader drives the
        // agitator motor; the slurry fills the stirred vessel and drains to the right, the gases to the right drums
        new ProcessSchematic("polymerization_reactor", 8, List.of(new Pos(50, 17), new Pos(50, 37)), List.of(new Pos(114, 57)),
            new Pos(50, 57), new Pos(114, 17),
            List.of(new Box(19, 24, 12, 46), new Box(34, 24, 12, 46), new Box(136, 24, 12, 46), new Box(151, 24, 12, 46)),
            new Pos(74, 34), 32, 33, Mode.UP, 4, true),
        // the feed enters the trayed column halfway up; vapour rises from the reboiler, the condenser sends the
        // tops over to the outer drum, the bottoms go across to the inner one, the heavy residue drains at the foot
        new ProcessSchematic("distillation_column", 8, List.of(), List.of(new Pos(44, 55)), null, null,
            List.of(new Box(21, 24, 14, 46), new Box(152, 24, 12, 46), new Box(137, 24, 12, 46)),
            new Pos(70, 22), 20, 47, Mode.UP, 4, false),
        // the belt carries the stone up into the shaft; it burns to lime from the bottom up, the gas goes to the drum
        new ProcessSchematic("lime_kiln", 8, List.of(new Pos(26, 20)), List.of(new Pos(118, 52)), null, null,
            List.of(new Box(148, 24, 14, 46)), new Pos(74, 26), 24, 41, Mode.UP, 4, false),
        // feed and culture ports on the lid, liquid drums either side; the broth clouds over and foams as it ferments
        new ProcessSchematic("fermenter", 8, List.of(new Pos(56, 17), new Pos(94, 17)), List.of(new Pos(116, 52)),
            new Pos(75, 17), null,
            List.of(new Box(19, 24, 12, 46), new Box(34, 24, 12, 46), new Box(137, 24, 12, 46), new Box(152, 24, 12, 46)),
            new Pos(58, 40), 52, 27, Mode.UP, 4, false),
        // the reactor series, laid out like the polymerization reactor (see reactor below)
        reactor("additive_reactor", 3, 1, 2, 0, false, true),
        reactor("catalyst_prep_tank", 2, 1, 2, 0, false, true),
        reactor("dehydration_reactor", 0, 1, 1, 2, true, false),
        reactor("depolymerization_reactor", 0, 1, 1, 1, true, true),
        reactor("oligomerization_reactor", 0, 0, 1, 2, false, true),
        reactor("vcm_converter", 0, 1, 2, 1, true, false),
        reactor("neutralizer", 0, 0, 2, 1, false, true),
        reactor("acetylene_generator", 1, 0, 1, 2, false, false),
        // the column series, laid out like the distillation column (see column below)
        column("solvent_recovery_tower", 1, 1, 1),
        column("tar_distiller", 2, 1, 1),
        column("scrubber", 1, 2, 2),
        column("flash_tank", 0, 1, 2),
        column("evaporator", 1, 1, 2),
        column("crystallizer", 1, 1, 1),
        column("vent_stack", 0, 1, 0),
        // the kiln series, laid out like the lime kiln (see kiln below)
        kiln("carbide_furnace", 2, 2, 0, 1, true),
        kiln("coking_oven", 1, 1, 0, 2, false),
        kiln("hcl_furnace", 0, 0, 2, 1, false),
        kiln("dryer", 1, 1, 1, 1, false),
        // the tank series, laid out like the fermenter (see vessel below)
        vessel("brine_dissolver", 1, 0, 1, 1, false),
        vessel("brine_purifier", 1, 0, 1, 1, false),
        vessel("coagulation_tank", 1, 1, 1, 1, false),
        vessel("liquefaction_tank", 1, 0, 1, 1, false),
        vessel("saccharification_tank", 0, 0, 1, 1, true),
        vessel("water_treatment", 0, 0, 1, 1, false),
        vessel("centrifuge", 0, 1, 1, 1, false),
        vessel("filter_press", 0, 1, 1, 1, false),
        vessel("membrane_cell", 0, 0, 1, 3, false),
        vessel("wet_mill", 1, 1, 1, 1, false),
        vessel("gas_compressor", 0, 0, 1, 1, false),
        vessel("strand_pelletizer", 1, 1, 1, 0, false),
        vessel("sheeting_press", 1, 1, 0, 1, false)));

    private static Map<String, ProcessSchematic> index(List<ProcessSchematic> all) {
        Map<String, ProcessSchematic> map = new HashMap<>();
        for (ProcessSchematic s : all) map.put(s.id(), s);
        return Map.copyOf(map);
    }

    /** Rows in the progress sheet. */
    public int rows() { return mode == Mode.PHASE ? frames : 1 + frames; }

    /** The schematic for {@code def}, or null if it has none or its slots no longer match the definition. */
    public static ProcessSchematic of(ProcessMachineDef def) {
        ProcessSchematic s = ALL.get(def.id());
        if (s == null) return null;
        boolean fits = s.inputs.size() == def.itemsIn() && s.outputs.size() == def.itemsOut()
            && (s.catalyst != null) == def.catalystSlot() && (s.card != null) == def.cardSlot()
            && s.tanks.size() == def.fluidsIn().size() + def.fluidsOut().size();
        return fits ? s : null;
    }
}
