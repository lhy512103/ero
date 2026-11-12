package item;

import blockentity.MachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.function.Consumer;

/**
 * Overclocker upgrade. Each installed upgrade multiplies the processing time and the energy drawn
 * per tick by its own factors; up to {@link #MAX_STACK} upgrades count and tiers can be mixed.
 * <p>
 * Basic x0.75 time / x1.5 energy, advanced x0.60 / x1.8, ultimate x0.45 / x2.2.
 * </p>
 */
public class OverclockerUpgradeItem extends UpgradeItem {

    public static final double SPEED_MULTIPLIER = 0.75;
    public static final double ENERGY_MULTIPLIER = 1.5;

    /** At most this many overclockers take effect in one machine. */
    public static final int MAX_STACK = 4;

    /** Combined effect of a machine's overclockers; {@link #NONE} when it has none. */
    public record Effect(double time, double energy) {
        public static final Effect NONE = new Effect(1, 1);

        public int time(int baseTicks) {
            return time == 1 ? baseTicks : Math.max(1, (int) Math.round(baseTicks * time));
        }

        public long energy(long basePerTick) {
            return energy == 1 ? basePerTick : Math.round(basePerTick * energy);
        }
    }

    public OverclockerUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean needsBinding() { return false; }

    /** Processing time factor of one upgrade of this tier. */
    public double timeFactor() { return SPEED_MULTIPLIER; }

    /** Energy-per-tick factor of one upgrade of this tier. */
    public double energyFactor() { return ENERGY_MULTIPLIER; }

    /** Number of overclockers that take effect in the machine (any tier). */
    public static int countOverclockers(MachineBlockEntity machine) {
        ItemStacksResourceHandler handler = machine.getItemHandler();
        if (handler == null) return 0;
        int start = machine.getUpgradeSlotStart();
        int count = 0;
        for (int slot = start; slot < start + machine.getUpgradeSlotCount(); slot++) {
            if (handler.getAmountAsInt(slot) > 0 && handler.getResource(slot).getItem() instanceof OverclockerUpgradeItem) count++;
        }
        return Math.min(count, MAX_STACK);
    }

    /** Combined effect of the first {@link #MAX_STACK} overclockers in the machine's upgrade slots. */
    public static Effect effect(MachineBlockEntity machine) {
        ItemStacksResourceHandler handler = machine.getItemHandler();
        if (handler == null) return Effect.NONE;
        int start = machine.getUpgradeSlotStart();
        int count = 0;
        double time = 1, energy = 1;
        for (int slot = start; slot < start + machine.getUpgradeSlotCount() && count < MAX_STACK; slot++) {
            if (handler.getAmountAsInt(slot) <= 0) continue;
            ItemResource resource = handler.getResource(slot);
            if (resource.getItem() instanceof OverclockerUpgradeItem upgrade) {
                time *= upgrade.timeFactor();
                energy *= upgrade.energyFactor();
                count++;
            }
        }
        return count == 0 ? Effect.NONE : new Effect(time, energy);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("tooltip.iuce.overclocker.time", String.valueOf(timeFactor())).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable("tooltip.iuce.overclocker.energy", String.valueOf(energyFactor())).withStyle(ChatFormatting.RED));
        builder.accept(Component.translatable("tooltip.iuce.overclocker.max", MAX_STACK).withStyle(ChatFormatting.GRAY));
    }
}
