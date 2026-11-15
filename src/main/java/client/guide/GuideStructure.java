package client.guide;

import block.ModBlocks;
import block.PlantControllerBlock;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import multiblock.PlantStructure;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A block layout loaded from {@code assets/iuce/guide/structures/<name>.json}:
 * <pre>
 * { "palette": { "C": "iuce:reactor_casing", "F": "iuce:fission_controller" },
 *   "layers": [ ["CCC", "CCC", "CCC"], ["CCC", "C C", "CFC"] ] }
 * </pre>
 * Layers run bottom to top, rows north to south, characters west to east; a space or any
 * character missing from the palette is air. The same data (positions + block ids) is what an
 * in-world projection needs, so {@link #cells} is deliberately plain.
 */
public final class GuideStructure {
    public record Cell(BlockPos pos, ItemStack stack, String blockId) {}

    public final List<Cell> cells;
    public final int sizeX, sizeY, sizeZ;

    private static final Map<String, Optional<GuideStructure>> CACHE = new HashMap<>();

    private GuideStructure(List<Cell> cells, int sizeX, int sizeY, int sizeZ) {
        this.cells = cells;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    static void clearCache() { CACHE.clear(); }

    @Nullable
    public static GuideStructure get(String name) {
        return CACHE.computeIfAbsent(name, GuideStructure::load).orElse(null);
    }

    /** Block counts in first-seen order, for the materials list. */
    public List<ItemStack> materials(int layers) {
        Map<String, ItemStack> totals = new LinkedHashMap<>();
        for (Cell cell : cells) {
            if (cell.pos.getY() >= layers) continue;
            ItemStack existing = totals.get(cell.blockId);
            if (existing == null) totals.put(cell.blockId, cell.stack.copyWithCount(1));
            else existing.grow(1);
        }
        return new ArrayList<>(totals.values());
    }

    private static Optional<GuideStructure> load(String name) {
        if (name.equals("ore_plant") || name.equals("rolling_mill")) return Optional.of(plant(name.equals("rolling_mill")));
        Optional<Resource> resource = GuideBook.resource("guide/structures/" + name + ".json");
        if (resource.isEmpty()) return Optional.empty();
        try (BufferedReader reader = resource.get().openAsReader()) {
            JsonObject json = GsonHelper.parse(reader);
            Map<Character, String> palette = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "palette").entrySet()) {
                if (!entry.getKey().isEmpty()) palette.put(entry.getKey().charAt(0), entry.getValue().getAsString());
            }
            List<Cell> cells = new ArrayList<>();
            JsonArray layers = GsonHelper.getAsJsonArray(json, "layers");
            int sizeX = 0, sizeZ = 0;
            for (int y = 0; y < layers.size(); y++) {
                JsonArray rows = layers.get(y).getAsJsonArray();
                sizeZ = Math.max(sizeZ, rows.size());
                for (int z = 0; z < rows.size(); z++) {
                    String row = rows.get(z).getAsString();
                    sizeX = Math.max(sizeX, row.length());
                    for (int x = 0; x < row.length(); x++) {
                        String blockId = palette.get(row.charAt(x));
                        if (blockId == null) continue;
                        ItemStack stack = GuideBook.stack(blockId);
                        if (!stack.isEmpty()) cells.add(new Cell(new BlockPos(x, y, z), stack, blockId));
                    }
                }
            }
            return Optional.of(new GuideStructure(List.copyOf(cells), sizeX, layers.size(), sizeZ));
        } catch (Exception error) {
            return Optional.empty();
        }
    }

    private static GuideStructure plant(boolean mill) {
        var controller = (mill ? ModBlocks.MILL_CONTROLLER : ModBlocks.PLANT_CONTROLLER).get();
        var state = controller.defaultBlockState().setValue(PlantControllerBlock.LEGACY, false);
        var bounds = PlantStructure.bounds(BlockPos.ZERO, Direction.NORTH, state);
        var cellsInWorld = PlantStructure.cells(BlockPos.ZERO, Direction.NORTH, state);
        int radius = -bounds.minX();
        List<Cell> cells = new ArrayList<>();
        cells.add(new Cell(new BlockPos(radius, 1, 0), new ItemStack(controller), BuiltInRegistries.BLOCK.getKey(controller).toString()));
        for (PlantStructure.Cell cell : cellsInWorld) {
            var block = cell.part().sample(Direction.NORTH).getBlock();
            cells.add(new Cell(cell.pos().offset(radius, 1, 0), new ItemStack(block), BuiltInRegistries.BLOCK.getKey(block).toString()));
        }
        int depth = 1 + cellsInWorld.stream().mapToInt(cell -> cell.pos().getZ()).max().orElse(0);
        return new GuideStructure(List.copyOf(cells), bounds.getXSpan(), bounds.getYSpan(), depth);
    }
}
