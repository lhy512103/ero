package machine.utility;

import net.minecraft.core.BlockPos;

/**
 * A block that hands out shares of a utility: the boiler (steam), the cooling tower (cooling water)
 * and, in creative and tests, the creative utility source, which hands out all of them.
 * <p>
 * The block owns its own fuel, water and power; {@link UtilityNetwork} only tells it how many of
 * its shares are in use each round, so a supplier with nothing attached uses nothing.
 * </p>
 */
public interface UtilitySupplier {

    /** How many shares of {@code utility} this block can serve at once; 0 if it does not supply it. */
    int shares(Utility utility);

    /** Called every round with the number of shares of each utility that are now in use. */
    void setOccupied(Utility utility, int occupied);

    /** Whether the supplier can run at all right now (has fuel, water, power). */
    default boolean ready() { return true; }

    BlockPos position();
}
