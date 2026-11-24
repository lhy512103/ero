package machine.process;

import machine.process.ProcessLayout.Pos;
import machine.process.ProgressStyle.Mode;

import java.util.List;
import java.util.Map;

/**
 * A machine whose process page is a picture of the machine itself, the way the vanilla brewing stand
 * or furnace is drawn, instead of the generic left-to-right row. The picture
 * {@code textures/gui/process/schematic/<id>.png} is {@link #WIDTH} x {@link #HEIGHT} and covers the
 * panel from row {@link #TOP}; the slots sit where the picture leaves room for them.
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
                               List<Pos> tanks, Pos window, int windowW, int windowH, Mode mode, int frames,
                               boolean idleOverlay) {
    public static final int TOP = 16, WIDTH = 176, HEIGHT = 60;

    private static final Pos IN = new Pos(26, 40), OUT = new Pos(136, 40);

    /** One input on the left, one output on the right, the machine in between. */
    private static ProcessSchematic line(String id, Pos catalyst, Pos window, int w, int h, Mode mode, int frames, boolean idle) {
        return new ProcessSchematic(id, 8, List.of(IN), List.of(OUT), catalyst, null, List.of(), window, w, h, mode, frames, idle);
    }

    private static final Map<String, ProcessSchematic> ALL = Map.of(
        // pellets drop through the hopper into the barrel, the core wire comes down into the die, the product leaves on the right
        "single_screw_extruder", new ProcessSchematic("single_screw_extruder", 8,
            List.of(new Pos(32, 20), new Pos(122, 20)), List.of(new Pos(150, 47)), new Pos(122, 47), null,
            List.of(), new Pos(52, 51), 64, 10, Mode.RIGHT, 6, true),
        // the hot product runs through a sprayed water trough and comes out cold
        "cooling_tank", line("cooling_tank", null, new Pos(44, 47), 88, 10, Mode.RIGHT, 4, false),
        // the platen comes down on the mould between the press columns, steam heats it, then it lifts
        "vulcanizer", line("vulcanizer", new Pos(80, 44), new Pos(60, 24), 56, 19, Mode.PHASE, 8, false),
        // sheets hang in the chamber over a log fire; the smoke fills it from below and cures them
        "smokehouse", new ProcessSchematic("smokehouse", 8, List.of(new Pos(26, 26), new Pos(26, 52)), List.of(OUT),
            null, null, List.of(), new Pos(56, 31), 60, 38, Mode.UP, 4, false),
        // the film runs through the nip rollers and over a guide onto the roll, which grows
        "film_winder", line("film_winder", null, new Pos(70, 18), 60, 48, Mode.PHASE, 8, false),
        // the tracks pull the pipe through, the saw cuts it into lengths
        "haul_off_cutter", line("haul_off_cutter", null, new Pos(46, 30), 84, 34, Mode.RIGHT, 4, true),
        // the spindle travels along the rail and cuts the pocket into the stock in the vise
        "cnc_machining_center", line("cnc_machining_center", null, new Pos(52, 23), 72, 42, Mode.PHASE, 8, false));

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
