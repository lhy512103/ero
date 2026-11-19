package recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON and network format of {@link ProcessRecipe}:
 * <pre>{@code
 * {
 *   "type": "iuce:process",
 *   "machine": "polymerizer",
 *   "items":   [{"item": "iuce:lactide", "count": 2}, {"item": "#c:dusts/coal"}],
 *   "fluids":  [{"fluid": "#iuce:any_water", "amount": 500}],
 *   "catalyst": {"item": "iuce:tin_catalyst", "damage": 1},
 *   "card": {"family": "pla", "options": {"molecular_weight": 2}},
 *   "utilities": ["steam"],
 *   "segments": ["side_feed"],
 *   "results": [{"id": "iuce:pla_resin", "count": 2, "chance": 1.0}],
 *   "fluid_results": [{"fluid": "iuce:lactic_acid", "amount": 100}],
 *   "energy": 4000, "time": 200
 * }
 * }</pre>
 */
public final class ProcessRecipeSerializer {
    private ProcessRecipeSerializer() {}

    /** A plain item id, "#tag", or a vanilla ingredient object. */
    private static final Codec<Ingredient> INGREDIENT = Codec.withAlternative(Ingredient.CODEC,
        Codec.STRING.comapFlatMap(text -> {
            Ingredient ingredient = IngredientHelper.parse(text);
            return ingredient != null ? DataResult.success(ingredient) : DataResult.error(() -> "Unknown ingredient: " + text);
        }, ingredient -> "minecraft:air"));

    private static final Codec<ProcessRecipe.ItemInput> ITEM_INPUT = RecordCodecBuilder.create(i -> i.group(
        INGREDIENT.fieldOf("item").forGetter(ProcessRecipe.ItemInput::ingredient),
        Codec.intRange(1, 1 << 20).optionalFieldOf("count", 1).forGetter(ProcessRecipe.ItemInput::count)
    ).apply(i, ProcessRecipe.ItemInput::new));

    private static final Codec<ProcessRecipe.FluidInput> FLUID_INPUT = RecordCodecBuilder.create(i -> i.group(
        FluidIngredient.CODEC.fieldOf("fluid").forGetter(ProcessRecipe.FluidInput::ingredient),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(ProcessRecipe.FluidInput::amount)
    ).apply(i, ProcessRecipe.FluidInput::new));

    private static final Codec<ProcessRecipe.Catalyst> CATALYST = RecordCodecBuilder.create(i -> i.group(
        INGREDIENT.fieldOf("item").forGetter(ProcessRecipe.Catalyst::ingredient),
        Codec.intRange(0, 1 << 20).optionalFieldOf("damage", 0).forGetter(ProcessRecipe.Catalyst::damage)
    ).apply(i, ProcessRecipe.Catalyst::new));

    private static final Codec<ProcessRecipe.CardRequirement> CARD = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("family").forGetter(ProcessRecipe.CardRequirement::family),
        Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 4)).optionalFieldOf("options", Map.of())
            .forGetter(ProcessRecipe.CardRequirement::options)
    ).apply(i, ProcessRecipe.CardRequirement::new));

    private static final Codec<ProcessRecipe.Output> OUTPUT = RecordCodecBuilder.create(i -> i.group(
        ItemStackTemplate.MAP_CODEC.forGetter(ProcessRecipe.Output::template),
        Codec.floatRange(0, 1).optionalFieldOf("chance", 1f).forGetter(ProcessRecipe.Output::chance)
    ).apply(i, ProcessRecipe.Output::new));

    private static final Codec<ProcessRecipe.FluidOutput> FLUID_OUTPUT = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(ProcessRecipe.FluidOutput::fluid),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(ProcessRecipe.FluidOutput::amount)
    ).apply(i, ProcessRecipe.FluidOutput::new));

    public static final MapCodec<ProcessRecipe> CODEC = RecordCodecBuilder.<ProcessRecipe>mapCodec(i -> i.group(
        Codec.STRING.fieldOf("machine").forGetter(ProcessRecipe::machine),
        ITEM_INPUT.listOf().optionalFieldOf("items", List.of()).forGetter(ProcessRecipe::items),
        FLUID_INPUT.listOf().optionalFieldOf("fluids", List.of()).forGetter(ProcessRecipe::fluids),
        CATALYST.optionalFieldOf("catalyst").forGetter(ProcessRecipe::catalyst),
        CARD.optionalFieldOf("card").forGetter(ProcessRecipe::card),
        Codec.STRING.listOf().optionalFieldOf("utilities", List.of()).forGetter(ProcessRecipe::utilities),
        Codec.STRING.listOf().optionalFieldOf("segments", List.of()).forGetter(ProcessRecipe::segments),
        OUTPUT.listOf().optionalFieldOf("results", List.of()).forGetter(ProcessRecipe::results),
        FLUID_OUTPUT.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(ProcessRecipe::fluidResults),
        Codec.LONG.fieldOf("energy").forGetter(ProcessRecipe::energy),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("time").forGetter(ProcessRecipe::time)
    ).apply(i, ProcessRecipe::new)).validate(recipe -> recipe.results().isEmpty() && recipe.fluidResults().isEmpty()
        && (recipe.items().isEmpty() && recipe.fluids().isEmpty())
        ? DataResult.error(() -> "Process recipe for " + recipe.machine() + " has neither inputs nor products")
        : DataResult.success(recipe));

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe.ItemInput> ITEM_INPUT_STREAM = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, ProcessRecipe.ItemInput::ingredient,
        ByteBufCodecs.VAR_INT, ProcessRecipe.ItemInput::count,
        ProcessRecipe.ItemInput::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe.FluidInput> FLUID_INPUT_STREAM = StreamCodec.composite(
        FluidIngredient.STREAM_CODEC, ProcessRecipe.FluidInput::ingredient,
        ByteBufCodecs.VAR_INT, ProcessRecipe.FluidInput::amount,
        ProcessRecipe.FluidInput::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe.Catalyst> CATALYST_STREAM = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, ProcessRecipe.Catalyst::ingredient,
        ByteBufCodecs.VAR_INT, ProcessRecipe.Catalyst::damage,
        ProcessRecipe.Catalyst::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe.CardRequirement> CARD_STREAM = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ProcessRecipe.CardRequirement::family,
        ByteBufCodecs.<RegistryFriendlyByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
        ProcessRecipe.CardRequirement::options,
        ProcessRecipe.CardRequirement::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe.Output> OUTPUT_STREAM = StreamCodec.composite(
        ItemStackTemplate.STREAM_CODEC, ProcessRecipe.Output::template,
        ByteBufCodecs.FLOAT, ProcessRecipe.Output::chance,
        ProcessRecipe.Output::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe.FluidOutput> FLUID_OUTPUT_STREAM = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.FLUID), ProcessRecipe.FluidOutput::fluid,
        ByteBufCodecs.VAR_INT, ProcessRecipe.FluidOutput::amount,
        ProcessRecipe.FluidOutput::new);

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<String>> STRINGS =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe> STREAM_CODEC = StreamCodec.of(
        (buf, recipe) -> {
            buf.writeUtf(recipe.machine());
            ITEM_INPUT_STREAM.apply(ByteBufCodecs.list()).encode(buf, recipe.items());
            FLUID_INPUT_STREAM.apply(ByteBufCodecs.list()).encode(buf, recipe.fluids());
            ByteBufCodecs.optional(CATALYST_STREAM).encode(buf, recipe.catalyst());
            ByteBufCodecs.optional(CARD_STREAM).encode(buf, recipe.card());
            STRINGS.encode(buf, recipe.utilities());
            STRINGS.encode(buf, recipe.segments());
            OUTPUT_STREAM.apply(ByteBufCodecs.list()).encode(buf, recipe.results());
            FLUID_OUTPUT_STREAM.apply(ByteBufCodecs.list()).encode(buf, recipe.fluidResults());
            buf.writeVarLong(recipe.energy());
            buf.writeVarInt(recipe.time());
        },
        buf -> new ProcessRecipe(
            buf.readUtf(),
            ITEM_INPUT_STREAM.apply(ByteBufCodecs.list()).decode(buf),
            FLUID_INPUT_STREAM.apply(ByteBufCodecs.list()).decode(buf),
            ByteBufCodecs.optional(CATALYST_STREAM).decode(buf),
            ByteBufCodecs.optional(CARD_STREAM).decode(buf),
            STRINGS.decode(buf),
            STRINGS.decode(buf),
            OUTPUT_STREAM.apply(ByteBufCodecs.list()).decode(buf),
            FLUID_OUTPUT_STREAM.apply(ByteBufCodecs.list()).decode(buf),
            buf.readVarLong(),
            buf.readVarInt()));

    public static RecipeSerializer<ProcessRecipe> create() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
