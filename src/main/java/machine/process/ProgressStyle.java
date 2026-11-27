package machine.process;

import com.mojang.serialization.Codec;

/**
 * How a process machine shows its progress, chosen with {@code "progress"} in its definition.
 * Each style is one sheet {@code textures/gui/process/progress/<id>.png}, painted by
 * {@code tools/art/blockbench/gui.js}, made of rows of {@link #width()} x {@link #height()}.
 * <ul>
 *   <li>{@link Mode#RIGHT} / {@link Mode#UP}: row 0 is the empty picture, row 1 the full one (revealed
 *   from the left or from the bottom as the run advances) and the remaining {@link #frames()} rows an
 *   overlay that loops while the machine runs.</li>
 *   <li>{@link Mode#PHASE}: {@link #frames()} rows, one per stage of the run.</li>
 * </ul>
 */
public enum ProgressStyle {
    ARROW("arrow", 22, 16, Mode.RIGHT, 0),
    SCREW("screw", 44, 16, Mode.RIGHT, 6),
    WAVE("wave", 22, 16, Mode.RIGHT, 4),
    PRESS("press", 22, 16, Mode.PHASE, 8),
    SMOKE("smoke", 22, 16, Mode.UP, 4),
    WINDER("winder", 22, 16, Mode.PHASE, 8),
    SAW("saw", 22, 16, Mode.RIGHT, 4);

    public enum Mode { RIGHT, UP, PHASE }

    public static final Codec<ProgressStyle> CODEC = Codec.STRING.xmap(ProgressStyle::of, ProgressStyle::id);

    private final String id;
    private final int width, height, frames;
    private final Mode mode;

    ProgressStyle(String id, int width, int height, Mode mode, int frames) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.mode = mode;
        this.frames = frames;
    }

    public String id() { return id; }
    public int width() { return width; }
    public int height() { return height; }
    public Mode mode() { return mode; }
    public int frames() { return frames; }

    /** Rows in the sheet. */
    public int rows() { return mode == Mode.PHASE ? frames : 2 + frames; }

    /** The style called {@code id}; unknown names fall back to the arrow. */
    public static ProgressStyle of(String id) {
        for (ProgressStyle style : values()) if (style.id.equals(id)) return style;
        return ARROW;
    }
}
