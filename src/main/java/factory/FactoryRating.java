package factory;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The star ratings a factory can earn. To earn star {@code n + 1} a hub must be kept supplied,
 * minute after minute, with enough of several products from the item tag
 * {@code iuce:rating/star_<n + 1>}; datapacks can change what counts by editing those tags.
 */
public final class FactoryRating {
    private FactoryRating() {}

    /**
     * @param kinds   how many different products must be supplied
     * @param rate    items of each product used up per minute
     * @param minutes consecutive minutes the supply has to last
     */
    public record Tier(int kinds, int rate, int minutes) {
        /** A hub stores at most two minutes' worth of each product. */
        public int capacity() { return rate * 2; }
    }

    /** Requirements for the first star, the second star, ... Later stars come with later phases. */
    private static final Tier[] TIERS = {new Tier(3, 16, 5), new Tier(5, 256, 6)};
    public static final int MAX_STARS = 5;
    /** The hub screen shows this many products at most. */
    public static final int MAX_CANDIDATES = 16;

    /** What a factory holding {@code stars} stars has to do for the next one; null when there is none yet. */
    public static Tier tier(int stars) {
        return stars >= 0 && stars < TIERS.length ? TIERS[stars] : null;
    }

    /** Products that count towards the next star, in a stable order shared by server and client. */
    public static List<Item> candidates(int stars) {
        List<Item> items = new ArrayList<>();
        if (tier(stars) == null) return items;
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("iuce", "rating/star_" + (stars + 1)));
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) items.add(holder.value());
        items.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return items.size() > MAX_CANDIDATES ? new ArrayList<>(items.subList(0, MAX_CANDIDATES)) : items;
    }
}
