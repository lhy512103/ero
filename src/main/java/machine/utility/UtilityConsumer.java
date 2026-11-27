package machine.utility;

import net.minecraft.core.BlockPos;

/**
 * A machine that runs recipes which ask for utilities. Machines implement this so the network can
 * find them across fluid pipes; they report their demand every tick they try to start a recipe.
 */
public interface UtilityConsumer {

    BlockPos position();
}
