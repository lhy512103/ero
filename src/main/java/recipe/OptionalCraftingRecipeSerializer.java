package recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public class OptionalCraftingRecipeSerializer {

    private final RecipeType<OptionalCraftingRecipe> recipeType;
    public final MapCodec<OptionalCraftingRecipe> CODEC;
    public final StreamCodec<RegistryFriendlyByteBuf, OptionalCraftingRecipe> STREAM_CODEC;

    public OptionalCraftingRecipeSerializer(RecipeType<OptionalCraftingRecipe> recipeType) {
        this.recipeType = recipeType;
        this.CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Ingredient.CODEC.listOf().fieldOf("input").forGetter(OptionalCraftingRecipe::getRequiredInputs),
                Ingredient.CODEC.listOf().optionalFieldOf("optional", List.of()).forGetter(OptionalCraftingRecipe::getOptionalInputs),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(OptionalCraftingRecipe::getOutputTemplate)
            ).apply(instance, OptionalCraftingRecipe::new)
        );
        this.STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeVarInt(recipe.getRequiredInputs().size());
                for (Ingredient ingredient : recipe.getRequiredInputs()) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                }
                buf.writeVarInt(recipe.getOptionalInputs().size());
                for (Ingredient ingredient : recipe.getOptionalInputs()) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                }
                ItemStack.STREAM_CODEC.encode(buf, recipe.getOutput());
            },
            buf -> {
                int requiredSize = buf.readVarInt();
                List<Ingredient> requiredInputs = new ArrayList<>();
                for (int i = 0; i < requiredSize; i++) {
                    requiredInputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                int optionalSize = buf.readVarInt();
                List<Ingredient> optionalInputs = new ArrayList<>();
                for (int i = 0; i < optionalSize; i++) {
                    optionalInputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                return new OptionalCraftingRecipe(requiredInputs, optionalInputs,
                    new ItemStackTemplate(output.getItem(), output.getCount()));
            }
        );
    }

    public static OptionalCraftingRecipeSerializer forType(RecipeType<OptionalCraftingRecipe> recipeType) {
        return new OptionalCraftingRecipeSerializer(recipeType);
    }

    public RecipeSerializer<OptionalCraftingRecipe> create() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
