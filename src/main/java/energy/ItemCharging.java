package energy;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Bounded, round-robin charging through the standard transactional item capability. */
public final class ItemCharging {
    private ItemCharging() {}

    public static void charge(EUEnergyStorage source, ItemStacksResourceHandler inventory, long tick) {
        long budget = Math.min(source.getEnergyStored(), source.getMaxTransferRate());
        if (budget <= 0) return;
        // Storage UI exposes indices 4..9. Hidden legacy slots must not consume power.
        int start = (int) Math.floorMod(tick, 6);
        for (int offset = 0; offset < 6 && budget > 0; offset++) {
            int slot = 4 + (start + offset) % 6;
            if (inventory.getAmountAsInt(slot) != 1) continue;
            var access = ItemAccess.forHandlerIndexStrict(inventory, slot);
            var target = access.getCapability(Capabilities.Energy.ITEM);
            if (target == null) continue;
            int offered = (int) Math.min(Integer.MAX_VALUE, source.extractEnergy(budget, true));
            if (offered <= 0) break;
            try (var transaction = Transaction.openRoot()) {
                int accepted = target.insert(offered, transaction);
                if (accepted <= 0) continue;
                long extracted = source.extractEnergy(accepted, false);
                if (extracted != accepted) {
                    source.receiveEnergy(extracted, false);
                    continue;
                }
                transaction.commit();
                budget -= accepted;
            }
        }
    }
}
