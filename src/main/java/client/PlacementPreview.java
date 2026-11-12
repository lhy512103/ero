package client;

import block.ChuteBlock;
import block.ConveyorBlock;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * While a conveyor or chute is held, shows the block as it would be placed: its model drawn
 * half-transparent, a white wireframe around it and a yellow wireframe arrow for the direction it
 * will carry items.
 */
public final class PlacementPreview {
    private PlacementPreview() {}

    private static final int MODEL_TINT = 0xA0FFFFFF;
    private static final int FRAME_COLOR = 0xFFFFFFFF;
    private static final int ARROW_COLOR = 0xFFFFD21E;
    private static final Direction[] SIDES = Direction.values();

    public static void onOutline(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlockItem item)) continue;
            Block block = item.getBlock();
            if (!(block instanceof ConveyorBlock) && !(block instanceof ChuteBlock)) continue;

            BlockPlaceContext context = new BlockPlaceContext(minecraft.player, hand, stack, event.getHitResult());
            if (!context.canPlace()) return;
            BlockState state = block.getStateForPlacement(context);
            if (state == null) return;
            BlockPos pos = context.getClickedPos();
            boolean conveyor = block instanceof ConveyorBlock;
            Direction facing = conveyor ? state.getValue(ConveyorBlock.FACING) : state.getValue(ChuteBlock.FACING);
            VoxelShape frame = state.getShape(event.getLevel(), pos);
            VoxelShape arrow = arrow(facing, conveyor ? (state.getValue(ConveyorBlock.ASCENDING) || state.getValue(ConveyorBlock.DESCENDING) ? 14 : 6.5) : 8);

            List<BlockStateModelPart> parts = new ArrayList<>();
            minecraft.getModelManager().getBlockStateModelSet().get(state)
                .collectParts(event.getLevel(), pos, state, RandomSource.create(42L), parts);

            event.addCustomRenderer((renderState, buffer, poseStack, translucentPass, levelState) -> {
                if (!translucentPass) return false;
                Vec3 camera = levelState.cameraRenderState.pos;
                double x = pos.getX() - camera.x, y = pos.getY() - camera.y, z = pos.getZ() - camera.z;

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                VertexConsumer model = buffer.getBuffer(RenderTypes.translucentMovingBlock());
                QuadInstance instance = new QuadInstance();
                instance.setColor(MODEL_TINT);
                for (BlockStateModelPart part : parts) {
                    for (var quad : part.getQuads(null)) model.putBakedQuad(poseStack.last(), quad, instance);
                    for (Direction side : SIDES) {
                        for (var quad : part.getQuads(side)) model.putBakedQuad(poseStack.last(), quad, instance);
                    }
                }
                poseStack.popPose();
                buffer.endBatch(RenderTypes.translucentMovingBlock());

                float width = minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
                ShapeRenderer.renderShape(poseStack, buffer.getBuffer(RenderTypes.lines()), frame, x, y, z, FRAME_COLOR, width);
                ShapeRenderer.renderShape(poseStack, buffer.getBuffer(RenderTypes.lines()), arrow, x, y, z, ARROW_COLOR, width);
                return false;
            });
            return;
        }
    }

    /** A shaft with a wider head at the end the items leave through. */
    private static VoxelShape arrow(Direction facing, double height) {
        return Shapes.or(part(facing, height, -5, 3, 0.75), part(facing, height, 3, 7, 2.5));
    }

    private static VoxelShape part(Direction facing, double height, double from, double to, double halfWidth) {
        double[] min = new double[3], max = new double[3];
        boolean vertical = facing.getAxis() == Direction.Axis.Y;
        double[] centre = {8, vertical ? 8 : height, 8};
        int[] step = {facing.getStepX(), facing.getStepY(), facing.getStepZ()};
        for (int axis = 0; axis < 3; axis++) {
            if (step[axis] != 0) {
                min[axis] = centre[axis] + Math.min(from * step[axis], to * step[axis]);
                max[axis] = centre[axis] + Math.max(from * step[axis], to * step[axis]);
            } else {
                double half = axis == 1 && !vertical ? 0.5 : halfWidth;
                min[axis] = centre[axis] - half;
                max[axis] = centre[axis] + half;
            }
        }
        return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }
}
