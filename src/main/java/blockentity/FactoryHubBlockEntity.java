package blockentity;

import com.mojang.serialization.Codec;
import factory.FactoryData;
import factory.FactoryHubMenu;
import factory.FactoryRating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Factory hub: takes in products and rates the factory. Every minute it uses up the required
 * amount of each product it holds enough of; when enough different products were supplied the
 * minute counts. One missed minute is forgiven, a second one in a row starts the count over.
 * Stars belong to the player who placed the hub.
 */
public class FactoryHubBlockEntity extends BlockEntity implements MenuProvider {
    public static final UUID NO_OWNER = new UUID(0, 0);
    public static final int MINUTE = 1200;
    private static final Codec<Map<Item, Integer>> STOCK_CODEC = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT);

    private UUID owner = NO_OWNER;
    private String ownerName = "";
    private final Map<Item, Integer> stock = new HashMap<>();
    private int ticks, minutes, misses;
    @Nullable private List<Item> candidates;
    private int candidatesFor = -1;

    /** Where hoppers, chutes, conveyors and pipes put products; emptied into {@link #stock} every tick. */
    private final ItemStacksResourceHandler intake = new ItemStacksResourceHandler(9) {
        @Override
        public boolean isValid(int index, ItemResource resource) { return accepts(resource.getItem()); }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return isValid(index, resource) ? super.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) { return 0; }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) { setChanged(); }
    };

    public FactoryHubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_HUB.get(), pos, state);
    }

    public ItemStacksResourceHandler getIntake() { return intake; }
    public int getProgressMinutes() { return minutes; }
    public int getMisses() { return misses; }
    public int getSecondsIntoMinute() { return ticks / 20; }
    public int getStock(Item item) { return stock.getOrDefault(item, 0); }
    public UUID getOwner() { return owner; }

    public void setOwner(UUID owner, String name) {
        this.owner = owner;
        this.ownerName = name;
        setChanged();
    }

    /** Stars of this hub's owner; always 0 on the client, which reads them from the menu instead. */
    public int getStars() {
        return level instanceof ServerLevel server ? FactoryData.get(server).stars(owner) : 0;
    }

    private List<Item> candidates(int stars) {
        if (candidates == null || candidatesFor != stars) {
            candidates = FactoryRating.candidates(stars);
            candidatesFor = stars;
        }
        return candidates;
    }

    /** True when the item counts towards the owner's next star. */
    public boolean accepts(Item item) {
        return candidates(getStars()).contains(item);
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        int stars = getStars();
        FactoryRating.Tier tier = FactoryRating.tier(stars);
        List<Item> wanted = candidates(stars);
        for (int slot = 0; slot < intake.size(); slot++) {
            int amount = intake.getAmountAsInt(slot);
            if (amount <= 0) continue;
            ItemResource resource = intake.getResource(slot);
            if (tier == null || !wanted.contains(resource.getItem())) {
                // no longer asked for (the factory was promoted): hand it back
                intake.set(slot, ItemResource.EMPTY, 0);
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), resource.toStack(amount));
                continue;
            }
            int moved = Math.min(amount, tier.capacity() - getStock(resource.getItem()));
            if (moved <= 0) continue;
            stock.merge(resource.getItem(), moved, Integer::sum);
            intake.set(slot, amount == moved ? ItemResource.EMPTY : resource, amount - moved);
        }
        if (tier != null && ++ticks >= MINUTE) {
            ticks = 0;
            endMinute();
        }
    }

    /** Settles one minute: uses up the products and decides whether the minute counts. */
    public void endMinute() {
        if (!(level instanceof ServerLevel server)) return;
        candidatesFor = -1;   // tags may have been reloaded
        int stars = getStars();
        FactoryRating.Tier tier = FactoryRating.tier(stars);
        if (tier == null) return;
        List<Item> wanted = candidates(stars);
        int supplied = 0;
        for (Item item : wanted) if (getStock(item) >= tier.rate()) supplied++;
        if (supplied >= tier.kinds()) {
            for (Item item : wanted) if (getStock(item) >= tier.rate()) stock.merge(item, -tier.rate(), Integer::sum);
            minutes++;
            misses = 0;
            if (minutes >= tier.minutes()) promote(server, stars + 1);
        } else if (minutes > 0 && ++misses > 1) {
            minutes = 0;
            misses = 0;
        }
        setChanged();
    }

    private void promote(ServerLevel server, int stars) {
        FactoryData.get(server).setStars(owner, stars);
        minutes = 0;
        misses = 0;
        stock.clear();
        candidatesFor = -1;
        Component who = ownerName.isEmpty() ? getBlockState().getBlock().getName() : Component.literal(ownerName);
        server.getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("message.iuce.hub.promoted", who, stars).withStyle(net.minecraft.ChatFormatting.GOLD), false);
        server.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (int slot = 0; slot < intake.size(); slot++) {
            int amount = intake.getAmountAsInt(slot);
            if (amount <= 0) continue;
            ItemStack stack = intake.getResource(slot).toStack(amount);
            intake.set(slot, ItemResource.EMPTY, 0);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    @Override
    public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FactoryHubMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Owner", UUIDUtil.STRING_CODEC, owner);
        output.putString("OwnerName", ownerName);
        output.putInt("Ticks", ticks);
        output.putInt("Minutes", minutes);
        output.putInt("Misses", misses);
        output.store("Stock", STOCK_CODEC, stock);
        intake.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("Owner", UUIDUtil.STRING_CODEC).orElse(NO_OWNER);
        ownerName = input.getStringOr("OwnerName", "");
        ticks = input.getIntOr("Ticks", 0);
        minutes = input.getIntOr("Minutes", 0);
        misses = input.getIntOr("Misses", 0);
        stock.clear();
        input.read("Stock", STOCK_CODEC).ifPresent(stock::putAll);
        intake.deserialize(input);
        candidatesFor = -1;
    }
}
