package client;

import block.ChuteBlock;
import block.ConveyorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * While a conveyor or chute is held, outlines where it would be placed and draws an arrow for the
 * direction it would carry items, so the direction is known before the block is put down.
 */
public final class PlacementPreview {
    private PlacementPreview() {}

    private static final int COLOR = 0xFF40E0FF;

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
            VoxelShape shape = Shapes.or(conveyor ? state.getShape(event.getLevel(), pos) : Shapes.block(),
                arrow(facing, conveyor ? 6.5 : 8));
            event.addCustomRenderer((renderState, buffer, poseStack, translucentPass, levelState) -> {
                if (translucentPass) return false;
                Vec3 camera = levelState.cameraRenderState.pos;
                ShapeRenderer.renderShape(poseStack, buffer.getBuffer(RenderTypes.lines()), shape,
                    pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, COLOR,
                    minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth);
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
        double[] centre = {8, facing.getAxis() == Direction.Axis.Y ? 8 : height, 8};
        int[] step = {facing.getStepX(), facing.getStepY(), facing.getStepZ()};
        for (int axis = 0; axis < 3; axis++) {
            if (step[axis] != 0) {
                min[axis] = centre[axis] + Math.min(from * step[axis], to * step[axis]);
                max[axis] = centre[axis] + Math.max(from * step[axis], to * step[axis]);
            } else {
                double half = axis == 1 ? 0.5 : halfWidth;
                if (facing.getAxis() == Direction.Axis.Y) half = halfWidth;
                min[axis] = centre[axis] - half;
                max[axis] = centre[axis] + half;
            }
        }
        return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }
}
