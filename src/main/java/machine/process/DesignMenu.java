package machine.process;

import block.ModBlocks;
import init.ModMenus;
import item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import recipe.ModRecipes;
import recipe.ProcessRecipe;
import recipe.ProcessRecipeSerializer;
import java.util.Comparator;
import java.util.List;

public class DesignMenu extends AbstractContainerMenu {
    private final SimpleContainer cards = new SimpleContainer(2);
    private final ContainerLevelAccess access;
    public final List<ProcessRecipe> choices;
    private final DataSlot selection = DataSlot.standalone();

    private static List<ProcessRecipe> choices(Level level) {
        return ((ServerLevel)level).recipeAccess().recipeMap().byType(ModRecipes.PROCESS_TYPE.get()).stream()
            .filter(h -> h.value().card().isPresent()).sorted(Comparator.comparing(h -> h.id().toString()))
            .map(RecipeHolder::value).toList();
    }

    public static void sendChoices(Level level, RegistryFriendlyByteBuf buf) {
        ProcessRecipeSerializer.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()).encode(buf, choices(level));
    }
    public DesignMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), ProcessRecipeSerializer.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()).decode(buf));
    }
    public DesignMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, pos, choices(inv.player.level()));
    }
    private DesignMenu(int id, Inventory inv, BlockPos pos, List<ProcessRecipe> choices) {
        super(ModMenus.DESIGN.get(), id);
        this.choices = choices;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        addDataSlot(selection);
        for (int i = 0; i < 2; i++) {
            final boolean pattern = i == 1;
            addSlot(new Slot(cards, i, 10 + 24*i, 35) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.PROCESS_CARD.get()) && stack.has(ProcessCards.TYPE.get()) == pattern;
                }
            });
        }
        addStandardInventorySlots(inv, 8, 100);
    }
    public ProcessRecipe selected() {
        return choices.isEmpty() ? null : choices.get(Math.clamp(selection.get(), 0, choices.size()-1));
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || choices.isEmpty()) return false;
        if (button == 0) {
            selection.set((selection.get()+1) % choices.size());
            return true;
        }
        var card = selected().card().orElseThrow();
        if (button == 2) {
            var template = cards.getItem(1).get(ProcessCards.TYPE.get());
            if (template == null || choices.stream().noneMatch(r -> r.card().orElseThrow().equals(template))) return false;
            card = template;
        } else if (button != 1) return false;
        if (!getSlot(0).mayPlace(cards.getItem(0))) return false;
        cards.removeItem(0, 1);
        ItemStack written = ProcessCards.write(card);
        if (!player.getInventory().add(written)) player.drop(written, false);
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PROCESS_DESIGN_TABLE.get());
    }
    @Override public void removed(Player player) {
        super.removed(player);
        clearContainer(player, cards);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (index < 2 ? !moveItemStackTo(stack, 2, slots.size(), true) : !moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
