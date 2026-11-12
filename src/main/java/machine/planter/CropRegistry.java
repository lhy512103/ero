package machine.planter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.*;
import java.util.function.Function;

/** Lazy virtual cultivation profiles. No registry-wide scans, world edits, or random-tick simulation. */
public final class CropRegistry {
    public record Profile(ItemResource output, int count, int ticks, long energyPerTick) {
        public Profile {
            if (output.isEmpty() || count < 1 || count > output.toStack().getMaxStackSize() || ticks < 1 || energyPerTick < 1)
                throw new IllegalArgumentException("Invalid cultivation profile");
        }
        public ItemStack result() { return output.toStack(count); }
    }
    public record Context(ServerLevel level, BlockPos pos, ItemStack seed) {}
    private static final Map<Item, Profile> REGISTERED = new IdentityHashMap<>();
    private static final List<Function<Context, Optional<Profile>>> RESOLVERS = new ArrayList<>();
    private static final Map<ItemResource, Optional<Profile>> CACHE = new HashMap<>();
    private static Object recipeMapIdentity;
    private static long revision;
    private CropRegistry() {}

    /** Call from common setup/enqueueWork. Explicit machine JSON recipes take precedence over this registry. */
    public static void register(ItemLike seed, ItemStack result, int ticks, long euPerTick) {
        REGISTERED.put(seed.asItem(), new Profile(ItemResource.of(result), result.getCount(), ticks, euPerTick));
        CACHE.clear();
        revision++;
    }
    /** Resolver extensions can support non-BlockItem seeds and mod-specific growth systems. */
    public static void registerResolver(Function<Context, Optional<Profile>> resolver) {
        RESOLVERS.add(Objects.requireNonNull(resolver));
        CACHE.clear();
        revision++;
    }
    public static long revision() { return revision; }

    /** A seed the planter can grow without a data recipe, with the product of one cycle. */
    public record Entry(ItemStack seed, Profile profile) {}

    /**
     * Every item the planter can cultivate automatically, in registry order. Used by recipe viewers;
     * it walks the item registry once, so call it when recipes are (re)loaded rather than every tick.
     */
    public static List<Entry> listAll(ServerLevel level, BlockPos pos) {
        List<Entry> out = new ArrayList<>();
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack seed = new ItemStack(item);
            if (seed.isEmpty()) continue;
            try {
                resolve(level, pos, seed).ifPresent(profile -> out.add(new Entry(seed, profile)));
            } catch (RuntimeException ignored) {
                // A modded block that cannot report drops out of world must not break the listing.
            }
        }
        return out;
    }
    public static Optional<Profile> resolve(ServerLevel level, BlockPos pos, ItemStack seed) {
        Object map = level.recipeAccess().recipeMap();
        if (recipeMapIdentity != map) { CACHE.clear(); recipeMapIdentity = map; }
        Profile explicit = REGISTERED.get(seed.getItem());
        if (explicit != null) return Optional.of(explicit);
        ItemResource key = ItemResource.of(seed);
        Optional<Profile> cached = CACHE.get(key);
        if (cached != null) return cached;
        Context context = new Context(level, pos, seed.copyWithCount(1));
        Optional<Profile> result = Optional.empty();
        for (var resolver : RESOLVERS) {
            result = resolver.apply(context);
            if (result.isPresent()) break;
        }
        if (result.isEmpty()) result = discover(context);
        // Also cache unsupported inputs. Bound component-sensitive keys to avoid unbounded modded stacks.
        if (CACHE.size() >= 2048) CACHE.clear();
        CACHE.put(key, result);
        return result;
    }
    private static Optional<Profile> discover(Context context) {
        ItemStack seed = context.seed();
        if (seed.is(Items.MELON_SEEDS)) return fixed(Items.MELON, 1, 400);
        if (seed.is(Items.PUMPKIN_SEEDS)) return fixed(Items.PUMPKIN, 1, 400);
        if (seed.is(Items.GLOW_BERRIES)) return fixed(Items.GLOW_BERRIES, 1, 300);
        if (!(seed.getItem() instanceof BlockItem item)) return Optional.empty();
        Block block = item.getBlock();
        if (block instanceof SaplingBlock || block instanceof NetherFungusBlock) return tree(seed.getItem());
        BlockState mature = block.defaultBlockState();
        int stages = 0;
        if (block instanceof CropBlock crop) {
            stages = crop.getMaxAge() + 1;
            mature = crop.getStateForAge(crop.getMaxAge());
        } else {
            for (var property : mature.getProperties()) {
                if (property instanceof IntegerProperty age && (age.getName().equals("age") || age.getName().equals("growth"))
                        && (block instanceof VegetationBlock || block instanceof BonemealableBlock
                            || block instanceof CactusBlock || block instanceof SugarCaneBlock)) {
                    int max = Collections.max(age.getPossibleValues());
                    stages = max + 1;
                    mature = mature.setValue(age, max);
                    break;
                }
            }
        }
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock
                || block instanceof KelpBlock) return fixed(seed.getItem(), 1, 400);
        if (stages == 0) {
            // Plants without growth stages (flowers, mushrooms, vines, lily pads...) propagate themselves slowly.
            boolean plant = block instanceof VegetationBlock || block instanceof VineBlock
                || block instanceof GrowingPlantBlock || block instanceof MultifaceSpreadeableBlock
                || block instanceof ChorusFlowerBlock || block instanceof BigDripleafBlock;
            return plant ? fixed(seed.getItem(), 1, 600) : Optional.empty();
        }
        // Only four native loot queries on FIRST use of a seed, never every tick or during startup.
        // Prefer a common non-seed product; rare drops (e.g. poisonous potatoes) are not mass-produced.
        Map<ItemResource, Integer> frequency = new LinkedHashMap<>();
        for (int sample = 0; sample < 4; sample++) {
            Set<ItemResource> seen = new HashSet<>();
            for (ItemStack drop : Block.getDrops(mature, context.level(), context.pos(), null)) {
                if (!drop.isEmpty()) seen.add(ItemResource.of(drop));
            }
            for (ItemResource drop : seen) frequency.merge(drop, 1, Integer::sum);
        }
        ItemResource product = null;
        for (var entry : frequency.entrySet()) {
            if (entry.getValue() >= 3 && entry.getKey().getItem() != seed.getItem()) { product = entry.getKey(); break; }
        }
        if (product == null) {
            ItemResource own = ItemResource.of(seed);
            if (frequency.getOrDefault(own, 0) >= 3) product = own;
        }
        if (product == null) return Optional.empty();
        // Fixed one-product yield: growth-stage scaling changes time, never grants rare-loot multipliers.
        return Optional.of(new Profile(product, 1, Math.clamp(stages * 25, 100, 1200), 2));
    }
    /** Saplings and fungi yield their wood, found by naming convention so modded trees work too. */
    private static Optional<Profile> tree(Item sapling) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sapling);
        String path = id.getPath();
        for (String suffix : new String[]{"_sapling", "_propagule", "_fungus"}) {
            if (!path.endsWith(suffix)) continue;
            String base = path.substring(0, path.length() - suffix.length());
            for (String wood : new String[]{"_log", "_stem", "_wood"}) {
                var target = net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), base + wood);
                var found = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(target);
                if (found.isPresent()) return fixed(found.get(), 1, 600);
            }
        }
        return fixed(sapling, 1, 600);
    }
    private static Optional<Profile> fixed(ItemLike item, int count, int ticks) {
        return Optional.of(new Profile(ItemResource.of(new ItemStack(item)), count, ticks, 2));
    }
}
