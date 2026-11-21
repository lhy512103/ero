package client;

import block.PlantControllerBlock;
import blockentity.PlantControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders the assembled 3D model of a formed large machine in place of its block parts: the ore
 * plant's crusher rollers and washing drum, the rolling mill's roll stands, each spinning while
 * the machine works.
 */
public class PlantControllerBlockEntityRenderer implements BlockEntityRenderer<PlantControllerBlockEntity, PlantControllerRenderState> {

    public PlantControllerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public PlantControllerRenderState createRenderState() {
        return new PlantControllerRenderState();
    }

    @Override
    public void extractRenderState(
        PlantControllerBlockEntity blockEntity,
        PlantControllerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.model = multiblock.PlantStructure.formedModel(blockState);
        state.formed = blockState.getValue(PlantControllerBlock.FORMED) && state.model != null;
        state.mill = blockEntity.isMill();
        state.legacy = blockState.getValue(PlantControllerBlock.LEGACY);
        state.working = blockEntity.isWorking();
        state.facing = blockState.getValue(PlantControllerBlock.FACING);
        state.length = PlantControllerBlockEntity.targetLength(blockState);

        Level level = blockEntity.getLevel();
        if (level != null && state.formed && !state.legacy) {
            state.lightCoords = samplePerimeterLight(level, blockEntity.getBlockPos(), state.facing);
            state.spinAngle = state.working ? (float) (((level.getGameTime() + partialTicks) * 8.0) % 360.0) : 0.0F;
        }
    }

    private static int samplePerimeterLight(Level level, BlockPos origin, Direction facing) {
        Direction back = facing.getOpposite();
        BlockPos[] samples = {
            origin.relative(facing),
            origin.relative(facing).above(2),
            origin.relative(back, 3).above(6),
            origin.relative(back, 3).relative(facing.getClockWise(), 3).above(2),
            origin.relative(back, 3).relative(facing.getCounterClockWise(), 3).above(2),
            origin.relative(back, 7).above(1)
        };
        int maxBlock = 0;
        int maxSky = 0;
        for (BlockPos sample : samples) {
            if (!level.isLoaded(sample)) continue;
            int packed = LevelRenderer.getLightCoords(level, sample);
            maxBlock = Math.max(maxBlock, LightCoordsUtil.block(packed));
            maxSky = Math.max(maxSky, LightCoordsUtil.sky(packed));
        }
        return LightCoordsUtil.pack(maxBlock, maxSky);
    }

    @Override
    public void submit(PlantControllerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed || state.legacy) return;
        PlantModel model = PlantModel.get(state.model);
        if (model == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingAngle(state.facing)));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        PlantModelRenderer.draw(model, PlantModelRenderer.Sink.of(collector), poseStack, state.length, state.spinAngle, state.working, state.lightCoords);
        poseStack.popPose();
    }

    private static float facingAngle(Direction facing) {
        return switch (facing) {
            case WEST -> 90.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    @Override
    public AABB getRenderBoundingBox(PlantControllerBlockEntity blockEntity) {
        BoundingBox box = blockEntity.box();
        // Formed models may rise well above the block layout (stacks, electrode columns).
        String name = multiblock.PlantStructure.formedModel(blockEntity.getBlockState());
        PlantModel model = name == null ? null : PlantModel.get(name);
        double top = Math.max(box.maxY() + 5.0, model == null ? 0 : blockEntity.getBlockPos().getY() + model.max.y + 1.0);
        return new AABB(box.minX() - 1.0, box.minY(), box.minZ() - 1.0, box.maxX() + 2.0, top, box.maxZ() + 2.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
