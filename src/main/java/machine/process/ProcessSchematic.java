package machine.process;

import machine.process.ProcessLayout.Pos;

import java.util.List;
import java.util.Map;

/**
 * A machine whose process page is a picture of the machine itself, the way the vanilla brewing stand
 * or furnace is drawn, instead of the generic left-to-right row. The picture
 * {@code textures/gui/process/schematic/<id>.png} is {@link #WIDTH} x {@link #HEIGHT} and covers the
 * panel from row {@link #TOP}; the slots sit where the picture leaves room for them.
 * <p>
 * The progress shows in {@link #window()}, {@link #windowW()} x {@link #windowH()}, from the sheet
 * {@code <id>_progress.png}: row 0 is revealed from the left as the run advances, and the next
 * {@link #frames()} rows are an overlay that loops while the machine runs (frame 0 when idle).
 * Positions are item positions, as in {@link ProcessLayout}.
 * </p>
 */
public record ProcessSchematic(String id, int energyX, List<Pos> inputs, List<Pos> outputs, Pos catalyst, Pos card,
                               List<Pos> tanks, Pos window, int windowW, int windowH, int frames) {
    public static final int TOP = 16, WIDTH = 176, HEIGHT = 60;

    private static final Map<String, ProcessSchematic> ALL = Map.of(
        // pellets drop through the hopper into the barrel, the core wire comes down into the die, the product leaves on the right
        "single_screw_extruder", new ProcessSchematic("single_screw_extruder", 8,
            List.of(new Pos(32, 20), new Pos(122, 20)), List.of(new Pos(150, 47)), new Pos(122, 47), null,
            List.of(), new Pos(52, 51), 64, 10, 6));

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
