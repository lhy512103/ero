package factory;

import blockentity.FactoryHubBlockEntity;
import init.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Slot-less menu that only carries the hub's numbers to the screen. */
public class FactoryHubMenu extends AbstractContainerMenu {
    private static final int FIXED = 4;
    private final int[] values = new int[FIXED + FactoryRating.MAX_CANDIDATES];

    public FactoryHubMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, (FactoryHubBlockEntity) inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public FactoryHubMenu(int containerId, Inventory inventory, FactoryHubBlockEntity hub) {
        super(ModMenus.FACTORY_HUB.get(), containerId);
        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                int stars = hub.getStars();
                switch (index) {
                    case 0: return stars;
                    case 1: return hub.getProgressMinutes();
                    case 2: return hub.getSecondsIntoMinute();
                    case 3: return hub.getMisses();
                    default:
                        List<Item> items = FactoryRating.candidates(stars);
                        return index - FIXED < items.size() ? hub.getStock(items.get(index - FIXED)) : 0;
                }
            }

            @Override
            public void set(int index, int value) { values[index] = value; }

            @Override
            public int getCount() { return values.length; }
        });
    }

    public int getStars() { return values[0]; }
    public int getMinutes() { return values[1]; }
    public int getSeconds() { return values[2]; }
    public int getMisses() { return values[3]; }
    public int getStock(int candidate) { return values[FIXED + candidate]; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
