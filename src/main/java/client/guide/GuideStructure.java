package client.guide;

import block.ModBlocks;
import block.PlantCasingBlock;
import client.PlantModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Vector3f;
import block.PlantControllerBlock;
import blockentity.PlantControllerBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
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
    /** A placed block; {@code state} carries its facing or axis so the view matches the world. */
    public record Cell(BlockPos pos, ItemStack stack, String blockId, BlockState state) {}

    public final List<Cell> cells;
    public final int sizeX, sizeY, sizeZ;
    /** The controller's machine when this layout is a large machine with an assembled model, else null. */
    @Nullable public final String machine;
    /** Where the controller sits in this layout; the assembled model is drawn from there. */
    public final BlockPos controller;
    /** Depth of the machine in blocks, for models that stretch with it. */
    public final int machineLength;
    private final Map<BlockPos, BlockState> byPos = new HashMap<>();

    private static final Map<String, Optional<GuideStructure>> CACHE = new HashMap<>();

    private GuideStructure(List<Cell> cells, int sizeX, int sizeY, int sizeZ, @Nullable String machine, BlockPos controller, int machineLength) {
        this.cells = cells;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.machine = machine;
        this.controller = controller;
        this.machineLength = machineLength;
        for (Cell cell : cells) byPos.put(cell.pos, cell.state);
    }

    /** The assembled model of the machine this layout builds, if it has one. */
    @Nullable
    public PlantModel model() {
        if (machine == null) return null;
        return PlantModel.get(switch (machine) {
            case "mill_controller" -> PlantModel.ROLLING_MILL;
            case "plant_controller" -> PlantModel.ORE_PLANT;
            default -> machine;
        });
    }

    /**
     * Moves from layout space into model space. Guide machines face south, while models run towards
     * +z from a north-facing controller, so the model turns half round about the controller.
     */
    public void toModel(PoseStack poseStack) {
        poseStack.translate(controller.getX() + 0.5F, controller.getY(), controller.getZ() + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(-0.5F, 0, -0.5F);
    }

    /** Corners of what is drawn, in layout blocks: the assembled model when formed, else the layout. */
    public Vector3f[] extent(boolean formed) {
        PlantModel model = formed ? model() : null;
        if (model == null) return new Vector3f[]{new Vector3f(), new Vector3f(sizeX, sizeY, sizeZ)};
        // After the half turn, model x and z run back from the controller's far corner.
        float cx = controller.getX() + 1, cz = controller.getZ() + 1;
        int extra = model.stretch.baseLength() > 0 ? Math.max(0, machineLength - model.stretch.baseLength()) : 0;
        return new Vector3f[]{
            new Vector3f(cx - model.max.x, controller.getY() + model.min.y, cz - model.max.z - extra),
            new Vector3f(cx - model.min.x, controller.getY() + model.max.y, cz - model.min.z)};
    }

    /** The block at a layout position, or null for air; used to hide faces between neighbours. */
    @Nullable
    public BlockState stateAt(BlockPos pos) { return byPos.get(pos); }

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
        if (name.equals("ore_plant") || name.equals("rolling_mill"))
            return Optional.of(plant((name.equals("rolling_mill") ? ModBlocks.MILL_CONTROLLER : ModBlocks.PLANT_CONTROLLER).get()));
        Block named = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("iuce", name));
        if (named instanceof PlantControllerBlock && PlantStructure.formedModel(
                named.defaultBlockState().setValue(PlantControllerBlock.LEGACY, false)) != null)
            return Optional.of(plant(named));
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
                        Block block = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(blockId));
                        if (!stack.isEmpty()) cells.add(new Cell(new BlockPos(x, y, z), stack, blockId, block.defaultBlockState()));
                    }
                }
            }
            return Optional.of(new GuideStructure(List.copyOf(cells), sizeX, layers.size(), sizeZ, null, BlockPos.ZERO, 0));
        } catch (Exception error) {
            return Optional.empty();
        }
    }

    private static GuideStructure plant(Block controller) {
        // The book shows the machine facing the reader: controller front towards south-west.
        Direction facing = Direction.SOUTH;
        var state = controller.defaultBlockState().setValue(PlantControllerBlock.LEGACY, false).setValue(PlantControllerBlock.FACING, facing);
        var layout = PlantStructure.cells(BlockPos.ZERO, facing, state);
        // The bounds of the parts actually placed: a mill's bounds cover its longest setting.
        var bounds = net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(
            layout.stream().map(PlantStructure.Cell::pos).toList()).orElseThrow().encapsulate(BlockPos.ZERO);
        var offset = new BlockPos(-bounds.minX(), -bounds.minY(), -bounds.minZ());
        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        placed.put(offset, state);
        for (PlantStructure.Cell cell : layout) {
            placed.put(cell.pos().offset(offset), cell.part().sample(facing));
        }
        List<Cell> cells = new ArrayList<>();
        for (var entry : placed.entrySet()) {
            BlockState part = entry.getValue();
            if (part.getBlock() instanceof PlantCasingBlock) part = joined(part, entry.getKey(), placed);
            Block block = part.getBlock();
            cells.add(new Cell(entry.getKey(), new ItemStack(block), BuiltInRegistries.BLOCK.getKey(block).toString(), part));
        }
        return new GuideStructure(List.copyOf(cells), bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan(),
            BuiltInRegistries.BLOCK.getKey(controller).getPath(), offset, PlantControllerBlockEntity.targetLength(state));
    }

    /** Casing trims hide where they touch other machine blocks, as {@code PlantCasingBlock#joinedState} does in the world. */
    private static BlockState joined(BlockState casing, BlockPos pos, Map<BlockPos, BlockState> placed) {
        for (Direction direction : Direction.values()) {
            BlockState neighbour = placed.get(pos.relative(direction));
            casing = casing.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighbour != null && PlantCasingBlock.joins(neighbour));
        }
        return casing;
    }
}
