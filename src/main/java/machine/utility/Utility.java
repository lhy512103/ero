package machine.utility;

import com.mojang.serialization.Codec;

/**
 * The three utilities a recipe may ask for while it runs. They are not fluids that travel in pipes:
 * a machine next to a supplier (or to a run of fluid pipes that ends at one) simply needs one
 * <em>share</em> of the utility, and a supplier hands out a fixed number of shares.
 */
public enum Utility {
    STEAM("steam"),
    COOLING_WATER("cooling_water"),
    AIR("air");

    /** Serialized by name in the pipe block's codec. */
    public static final Codec<Utility> CODEC = Codec.STRING.xmap(Utility::of, Utility::id);

    private final String id;

    Utility(String id) { this.id = id; }

    /** Name used both as the recipe's {@code utilities} entry and as a language key part. */
    public String id() { return id; }

    /** The recipe entry that names this utility. */
    public String tag() { return id; }

    public String nameKey() { return "gui.iuce.utility." + id; }

    /**
     * Returns the utility a recipe entry names, or null for an id this version does not know.
     * Unknown ids never match, so an old recipe asking for a utility we dropped just stays idle.
     */
    public static Utility of(String id) {
        for (Utility utility : values()) if (utility.id.equals(id)) return utility;
        return null;
    }
}
