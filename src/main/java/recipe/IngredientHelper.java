package recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ingredient 解析工具类
 * NeoForge 1.21.2+ 的 Ingredient.CODEC 不再接受旧的 JSON 格式，
 * 需要直接使用 API 创建 Ingredient 对象
 */
public class IngredientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("ero/IngredientHelper");

    /**
     * 解析字符串为 Ingredient
     * 支持格式：
     * - 物品ID: "minecraft:diamond"
     * - 标签: "#c:ingots/iron"
     */
    public static Ingredient parse(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        try {
            if (input.startsWith("#")) {
                return ofTag(input.substring(1));
            } else {
                return ofItem(input);
            }
        } catch (Exception e) {
            LOGGER.error("[IngredientHelper] Failed to parse '{}': {}", input, e.getMessage());
            return null;
        }
    }

    /**
     * 创建物品 Ingredient
     */
    public static Ingredient ofItem(String itemId) {
        var holder = BuiltInRegistries.ITEM.get(Identifier.parse(itemId));
        if (holder.isPresent()) {
            return Ingredient.of(holder.get().value());
        }
        LOGGER.error("[IngredientHelper] Unknown item: {}", itemId);
        return null;
    }

    /**
     * 创建标签 Ingredient
     */
    public static Ingredient ofTag(String tagPath) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(tagPath));
        var holderSet = BuiltInRegistries.ITEM.getOrThrow(tagKey);
        return Ingredient.of(holderSet);
    }
}
