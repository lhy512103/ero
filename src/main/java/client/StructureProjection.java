package client;

import block.PlantControllerBlock;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import multiblock.PlantStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Building aid for large machines: every missing or wrong position of an unfinished structure
 * renders a translucent ghost of the expected part with its required orientation, plus
 * color-coded outlines (yellow = casing, blue = dedicated girder/drum/hatch, red = wrong block).
 * It shows while the crosshair is on the controller, and for controllers pinned from their panel
 * whenever the crosshair is on any block. Pins belong to this client only.
 */
public final class StructureProjection {
    private StructureProjection() {}

    private static final int FREE_LINE = 0xAAFFD21E, PART_LINE = 0xFF64D3FF, WRONG_LINE = 0xFFFF5050;
    private static final int FREE_TINT = 0x70FFE090, PART_TINT = 0x9888E8FF;
    private static final Direction[] SIDES = Direction.values();
    private static final Set<BlockPos> PINNED = new HashSet<>();

    private record Entry(BlockPos pos, int lineColor, int ghostTint, VoxelShape shape, List<BlockStateModelPart> parts) {}

    public static boolean pinned(BlockPos controller) { return PINNED.contains(controller); }

    public static void togglePin(BlockPos controller) {
        BlockPos pos = controller.immutable();
        if (!PINNED.remove(pos)) PINNED.add(pos);
    }

    public static void onOutline(ExtractBlockOutlineRenderStateEvent event) {
        var level = event.getLevel();
        List<Entry> entries = new ArrayList<>();
        Set<BlockPos> done = new HashSet<>();
        BlockState looked = event.getBlockState();
        // pinned controllers are drawn every frame by onRenderLevel; here only the one being looked at
        if (looked.getBlock() instanceof PlantControllerBlock && !PINNED.contains(event.getBlockPos())) {
            done.add(event.getBlockPos());
            collect(level, event.getBlockPos(), looked, entries);
        }
        if (entries.isEmpty()) return;
        event.addCustomRenderer((renderState, buffer, poseStack, translucentPass, levelState) -> {
            if (!translucentPass) return false;
            draw(entries, buffer, poseStack, levelState.cameraRenderState.pos);
            return false;
        });
    }

    /** Pinned previews stay visible wherever the crosshair is. */
    public static void onRenderLevel(net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (PINNED.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        PINNED.removeIf(pos -> level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof PlantControllerBlock));
        List<Entry> entries = new ArrayList<>();
        for (BlockPos pos : PINNED) if (level.isLoaded(pos)) collect(level, pos, level.getBlockState(pos), entries);
        if (entries.isEmpty()) return;
        var buffer = minecraft.renderBuffers().bufferSource();
        com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        draw(entries, buffer, poseStack, event.getLevelRenderState().cameraRenderState.pos);
        buffer.endBatch(RenderTypes.lines());
        poseStack.popPose();
    }

    private static void draw(List<Entry> entries, net.minecraft.client.renderer.MultiBufferSource.BufferSource buffer,
                             com.mojang.blaze3d.vertex.PoseStack poseStack, Vec3 camera) {
            Minecraft minecraft = Minecraft.getInstance();

            VertexConsumer ghost = buffer.getBuffer(RenderTypes.translucentMovingBlock());
            QuadInstance instance = new QuadInstance();
            for (Entry entry : entries) {
                if (entry.parts().isEmpty()) continue;
                BlockPos p = entry.pos();
                double x = p.getX() - camera.x, y = p.getY() - camera.y, z = p.getZ() - camera.z;
                poseStack.pushPose();
                poseStack.translate(x + 0.5, y + 0.5, z + 0.5);
                poseStack.scale(0.96F, 0.96F, 0.96F);
                poseStack.translate(-0.5, -0.5, -0.5);
                instance.setColor(entry.ghostTint());
                for (BlockStateModelPart part : entry.parts()) {
                    for (var quad : part.getQuads(null)) ghost.putBakedQuad(poseStack.last(), quad, instance);
                    for (Direction side : SIDES) {
                        for (var quad : part.getQuads(side)) ghost.putBakedQuad(poseStack.last(), quad, instance);
                    }
                }
                poseStack.popPose();
            }
            buffer.endBatch(RenderTypes.translucentMovingBlock());

            float width = minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            var lines = buffer.getBuffer(RenderTypes.lines());
            for (Entry entry : entries) {
                BlockPos p = entry.pos();
                ShapeRenderer.renderShape(
                    poseStack, lines, entry.shape(),
                    p.getX() - camera.x, p.getY() - camera.y, p.getZ() - camera.z,
                    entry.lineColor(), width
                );
            }
    }

    /** Adds the ghosts of one unfinished controller's structure. */
    private static void collect(ClientLevel level, BlockPos pos, BlockState state, List<Entry> entries) {
        if (!(state.getBlock() instanceof PlantControllerBlock) || state.getValue(PlantControllerBlock.FORMED)) return;
        Minecraft minecraft = Minecraft.getInstance();
        Direction facing = state.getValue(PlantControllerBlock.FACING);
        var modelSet = minecraft.getModelManager().getBlockStateModelSet();
        RandomSource random = RandomSource.create(42L);
        List<PlantStructure.Cell> cells = PlantStructure.cells(pos, facing, state);

        Map<BlockPos, BlockState> expectedMap = new java.util.HashMap<>();
        for (PlantStructure.Cell cell : cells) {
            BlockState there = level.isLoaded(cell.pos()) ? level.getBlockState(cell.pos()) : null;
            expectedMap.put(cell.pos(), there != null && cell.part().accepts(there, facing) ? there : cell.part().sample(facing));
        }
        expectedMap.put(pos, state);

        for (PlantStructure.Cell cell : cells) {
            if (!level.isLoaded(cell.pos())) continue;
            BlockState there = level.getBlockState(cell.pos());
            if (cell.part().accepts(there, facing)) continue;
            boolean wrong = !there.canBeReplaced();
            boolean casing = cell.part().canFillWithCasing();
            BlockState expected = cell.part().sample(facing);
            if (expected.getBlock() instanceof block.ConnectedStructureBlock csb) expected = csb.joinedInLayout(expected, cell.pos(), expectedMap);
            else if (expected.getBlock() instanceof block.PlantDrumBlock drum) expected = drum.joinedInLayout(expected, cell.pos(), expectedMap);
            else if (expected.getBlock() instanceof block.PlantFrameBlock frame) expected = frame.joinedInLayout(expected, cell.pos(), expectedMap);
            List<BlockStateModelPart> parts = List.of();
            VoxelShape shape = Shapes.block();
            if (!wrong) {
                List<BlockStateModelPart> collected = new ArrayList<>();
                modelSet.get(expected).collectParts(level, cell.pos(), expected, random, collected);
                parts = List.copyOf(collected);
                VoxelShape expectedShape = expected.getShape(level, cell.pos());
                if (!expectedShape.isEmpty()) shape = expectedShape;
            }
            entries.add(new Entry(cell.pos(), wrong ? WRONG_LINE : casing ? FREE_LINE : PART_LINE,
                wrong ? 0 : casing ? FREE_TINT : PART_TINT, shape, parts));
        }
    }
}
