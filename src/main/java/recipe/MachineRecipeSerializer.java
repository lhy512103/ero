package recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MachineRecipeSerializer {

    /**
     * 支持 Ingredient 的多种 JSON 格式：
     * - 裸字符串: "iuce:zinc_ore" → 通过 IngredientHelper 创建
     * - 标签字符串: "#iuce:ores/zinc" → 通过 IngredientHelper 创建
     */
    private static final Codec<Ingredient> FLEXIBLE_INGREDIENT_CODEC = Codec.withAlternative(
        Ingredient.CODEC,
        Codec.STRING.flatXmap(
            str -> {
                Ingredient ingredient = IngredientHelper.parse(str);
                if (ingredient != null) {
                    return DataResult.success(ingredient);
                }
                return DataResult.error(() -> "Failed to parse ingredient: " + str);
            },
            ingredient -> {
                return DataResult.error(() -> "Cannot serialize Ingredient back to string");
            }
        )
    );

    /**
     * 支持 ItemStackTemplate 的多种 JSON 格式：
     * - 裸字符串: "minecraft:cobblestone" → 自动转为 {"id":"minecraft:cobblestone","count":1}
     * - 标准对象: {"id":"iuce:zinc_1","count":2}
     */
    private static final Codec<ItemStackTemplate> FLEXIBLE_RESULT_CODEC = Codec.withAlternative(
        ItemStackTemplate.CODEC,
        Identifier.CODEC.flatXmap(
            id -> {
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
                if (item.isEmpty()) {
                    return DataResult.error(() -> "Unknown item: " + id);
                }
                return DataResult.success(new ItemStackTemplate(item.get(), 1));
            },
            template -> {
                Item item = template.item().value();
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                return DataResult.success(id);
            }
        )
    );

    private static final Codec<MachineRecipe.ExtraResult> EXTRA_RESULT_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            FLEXIBLE_RESULT_CODEC.fieldOf("result").forGetter(MachineRecipe.ExtraResult::template),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(MachineRecipe.ExtraResult::chance)
        ).apply(instance, MachineRecipe.ExtraResult::new)
    );

    public static final MapCodec<MachineRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.fieldOf("machine_type").forGetter(MachineRecipe::getMachineType),
            FLEXIBLE_INGREDIENT_CODEC.fieldOf("input").forGetter(MachineRecipe::getInput),
            FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("input2").forGetter(MachineRecipe::getInput2),
            Codec.INT.optionalFieldOf("input_count", 1).forGetter(MachineRecipe::getInputCount),
            Codec.INT.optionalFieldOf("input2_count", 1).forGetter(MachineRecipe::getInput2Count),
            FLEXIBLE_RESULT_CODEC.fieldOf("result").forGetter(MachineRecipe::getOutputTemplate),
            EXTRA_RESULT_CODEC.listOf().optionalFieldOf("extra_results", List.of()).forGetter(MachineRecipe::getExtraResults),
            Codec.LONG.fieldOf("energy").forGetter(MachineRecipe::getEnergyCost),
            Codec.INT.fieldOf("time").forGetter(MachineRecipe::getProcessTime)
        ).apply(instance, MachineRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> STREAM_CODEC =
        StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.getMachineType());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getInput());
                buf.writeBoolean(recipe.getInput2().isPresent());
                recipe.getInput2().ifPresent(ing -> Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing));
                buf.writeVarInt(recipe.getInputCount());
                buf.writeVarInt(recipe.getInput2Count());
                ItemStack.STREAM_CODEC.encode(buf, recipe.getOutput());
                buf.writeVarInt(recipe.getExtraResults().size());
                for (MachineRecipe.ExtraResult extra : recipe.getExtraResults()) {
                    ItemStack.STREAM_CODEC.encode(buf, extra.create());
                    buf.writeFloat(extra.chance());
                }
                buf.writeLong(recipe.getEnergyCost());
                buf.writeVarInt(recipe.getProcessTime());
            },
            buf -> {
                String machineType = buf.readUtf();
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                Optional<Ingredient> input2 = buf.readBoolean() ?
                    Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buf)) : Optional.empty();
                int inputCount = buf.readVarInt();
                int input2Count = buf.readVarInt();
                ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                int extraCount = buf.readVarInt();
                List<MachineRecipe.ExtraResult> extras = new ArrayList<>(extraCount);
                for (int i = 0; i < extraCount; i++) {
                    ItemStack extraStack = ItemStack.STREAM_CODEC.decode(buf);
                    float chance = buf.readFloat();
                    extras.add(new MachineRecipe.ExtraResult(
                        new ItemStackTemplate(extraStack.getItem(), extraStack.getCount()), chance));
                }
                long energyCost = buf.readLong();
                int processTime = buf.readVarInt();
                return new MachineRecipe(machineType, input, input2, inputCount, input2Count,
                    new ItemStackTemplate(output.getItem(), output.getCount()), List.copyOf(extras), energyCost, processTime);
            }
        );

    public static RecipeSerializer<MachineRecipe> create() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
