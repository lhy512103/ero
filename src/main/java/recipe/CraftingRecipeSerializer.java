package recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import java.util.function.Supplier;

public class CraftingRecipeSerializer {

    private final RecipeType<CraftingRecipe> recipeType;
    private RecipeSerializer<CraftingRecipe> serializer;
    public final MapCodec<CraftingRecipe> CODEC;
    public final StreamCodec<RegistryFriendlyByteBuf, CraftingRecipe> STREAM_CODEC;

    public CraftingRecipeSerializer(RecipeType<CraftingRecipe> recipeType) {
        this.recipeType = recipeType;

        this.CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                INPUT_DATA_CODEC.fieldOf("input").forGetter(InputData::fromRecipe),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(CraftingRecipe::getOutputTemplate)
            ).apply(instance, (inputData, output) -> inputData.toRecipe(output, recipeType, () -> serializer))
        );

        this.STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeBoolean(recipe.isShaped());
                if (recipe.isShaped()) {
                    buf.writeVarInt(recipe.getGridWidth());
                    buf.writeVarInt(recipe.getGridHeight());
                    boolean[] empty = recipe.getEmptySlots();
                    buf.writeVarInt(empty.length);
                    for (boolean b : empty) buf.writeBoolean(b);
                }
                buf.writeVarInt(recipe.getInputs().size());
                for (Ingredient ingredient : recipe.getInputs()) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                }
                ItemStack.STREAM_CODEC.encode(buf, recipe.getOutput());
            },
            buf -> {
                boolean shaped = buf.readBoolean();
                if (shaped) {
                    int gridW = buf.readVarInt();
                    int gridH = buf.readVarInt();
                    int emptyLen = buf.readVarInt();
                    boolean[] emptySlots = new boolean[emptyLen];
                    for (int i = 0; i < emptyLen; i++) emptySlots[i] = buf.readBoolean();
                    int inputSize = buf.readVarInt();
                    List<Ingredient> inputs = new ArrayList<>();
                    for (int i = 0; i < inputSize; i++) {
                        inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                    return CraftingRecipe.createShaped(inputs, emptySlots, gridW, gridH,
                        new ItemStackTemplate(output.getItem(), output.getCount()),
                        recipeType, () -> serializer);
                }
                int size = buf.readVarInt();
                List<Ingredient> inputs = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                return CraftingRecipe.create(inputs, new ItemStackTemplate(output.getItem(), output.getCount()),
                    recipeType, () -> serializer);
            }
        );
    }

    // ── 中间数据类 ──

    record InputData(List<Ingredient> inputs, boolean shaped, boolean[] emptySlots,
                     int gridWidth, int gridHeight) {
        static InputData fromRecipe(CraftingRecipe r) {
            return new InputData(r.getInputs(), r.isShaped(), r.getEmptySlots(),
                r.getGridWidth(), r.getGridHeight());
        }

        CraftingRecipe toRecipe(ItemStackTemplate output, RecipeType<?> type,
                                Supplier<RecipeSerializer<?>> serializerSupplier) {
            if (shaped) {
                return CraftingRecipe.createShaped(inputs, emptySlots, gridWidth, gridHeight,
                    output, type, serializerSupplier);
            }
            return CraftingRecipe.create(inputs, output, type, serializerSupplier);
        }
    }

    // ── 2D网格行编解码器 ──

    /**
     * 单行编解码器：List<Ingredient | empty string>
     * 空字符串 "" 表示空位
     */
    private static final Codec<List<IngredientSlot>> ROW_CODEC = Codec.list(
        Codec.withAlternative(
            Ingredient.CODEC.flatXmap(
                ing -> DataResult.success(new IngredientSlot(false, ing)),
                slot -> slot.isEmpty()
                    ? DataResult.error(() -> "Cannot encode empty slot as Ingredient")
                    : DataResult.success(slot.ingredient())
            ),
            Codec.STRING.flatXmap(
                str -> {
                    if (str.isEmpty()) {
                        return DataResult.success(new IngredientSlot(true, null));
                    }
                    // 使用 IngredientHelper 解析物品ID或标签
                    Ingredient ingredient = IngredientHelper.parse(str);
                    if (ingredient != null) {
                        return DataResult.success(new IngredientSlot(false, ingredient));
                    }
                    return DataResult.error(() -> "Unknown ingredient: " + str);
                },
                slot -> slot.isEmpty()
                    ? DataResult.success("")
                    : DataResult.error(() -> "Cannot encode non-empty slot as empty string")
            )
        )
    );

    /**
     * 输入数据编解码器：自动检测2D/1D格式
     * - 1D: ["item1", "item2", ...] → 无序配方
     * - 2D: [["item1", "", "item2"], ...] → 有序配方
     */
    private static final Codec<InputData> INPUT_DATA_CODEC = Codec.withAlternative(
        // 先尝试2D网格格式
        ROW_CODEC.listOf().flatXmap(
            rows -> {
                int gridH = rows.size();
                if (gridH == 0) return DataResult.error(() -> "Grid must have at least one row");
                int gridW = rows.stream().mapToInt(List::size).max().orElse(0);
                if (gridW == 0) return DataResult.error(() -> "Grid must have at least one column");

                boolean[] emptySlots = new boolean[gridW * gridH];
                List<Ingredient> ingredients = new ArrayList<>();

                for (int r = 0; r < gridH; r++) {
                    List<IngredientSlot> row = rows.get(r);
                    for (int c = 0; c < gridW; c++) {
                        int idx = r * gridW + c;
                        if (c >= row.size() || row.get(c).isEmpty()) {
                            emptySlots[idx] = true;
                        } else {
                            emptySlots[idx] = false;
                            ingredients.add(row.get(c).getIngredient());
                        }
                    }
                }
                return DataResult.success(new InputData(ingredients, true, emptySlots, gridW, gridH));
            },
            inputData -> {
                if (!inputData.shaped()) {
                    return DataResult.error(() -> "Cannot encode shapeless recipe as 2D grid");
                }
                List<List<IngredientSlot>> rows = new ArrayList<>();
                for (int r = 0; r < inputData.gridHeight(); r++) {
                    List<IngredientSlot> row = new ArrayList<>();
                    for (int c = 0; c < inputData.gridWidth(); c++) {
                        int idx = r * inputData.gridWidth() + c;
                        if (inputData.emptySlots()[idx]) {
                            row.add(new IngredientSlot(true, null));
                        } else {
                            int nonEmptyIdx = countNonEmptyBefore(inputData.emptySlots(), idx);
                            row.add(new IngredientSlot(false, inputData.inputs().get(nonEmptyIdx)));
                        }
                    }
                    rows.add(row);
                }
                return DataResult.success(rows);
            }
        ),
        // 回退到1D平铺格式（无序配方）
        IngredientHelper.FLEXIBLE_CODEC.listOf().flatXmap(
            inputs -> DataResult.success(new InputData(inputs, false, null, 0, 0)),
            inputData -> {
                if (inputData.shaped()) {
                    return DataResult.error(() -> "Cannot encode shaped recipe as flat list");
                }
                return DataResult.success(inputData.inputs());
            }
        )
    );

    private static int countNonEmptyBefore(boolean[] emptySlots, int idx) {
        int count = 0;
        for (int i = 0; i < idx; i++) {
            if (!emptySlots[i]) count++;
        }
        return count;
    }

    /** 临时包装：网格中的单个槽位（空位或有Ingredient） */
    record IngredientSlot(boolean empty, Ingredient ingredient) {
        boolean isEmpty() { return empty; }

        Ingredient getIngredient() {
            return ingredient;
        }
    }

    public static CraftingRecipeSerializer forType(RecipeType<CraftingRecipe> recipeType) {
        return new CraftingRecipeSerializer(recipeType);
    }

    public RecipeSerializer<CraftingRecipe> create() {
        this.serializer = new RecipeSerializer<>(CODEC, STREAM_CODEC);
        return this.serializer;
    }
}
