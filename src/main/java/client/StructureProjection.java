package client;

import block.PlantControllerBlock;
import blockentity.PlantControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Building aid for large machines: while the crosshair is on a controller whose structure is not
 * complete, every position that still needs a casing block or hatch is outlined in the world,
 * free positions in yellow and positions holding a wrong block in red.
 */
public final class StructureProjection {
    private StructureProjection() {}

    private static final int FREE = 0xFFFFD21E, WRONG = 0xFFFF5050;

    public static void onOutline(ExtractBlockOutlineRenderStateEvent event) {
        BlockState state = event.getBlockState();
        if (!(state.getBlock() instanceof PlantControllerBlock) || state.getValue(PlantControllerBlock.FORMED)) return;
        Minecraft minecraft = Minecraft.getInstance();
        var level = event.getLevel();
        BlockPos pos = event.getBlockPos();
        Direction facing = state.getValue(PlantControllerBlock.FACING);
        int length = PlantControllerBlockEntity.targetLength(state);

        List<BlockPos> free = new ArrayList<>(), wrong = new ArrayList<>();
        for (BlockPos shell : PlantControllerBlockEntity.shell(pos, facing, length)) {
            BlockState there = level.getBlockState(shell);
            if (PlantControllerBlockEntity.isShellBlock(there)) continue;
            (there.canBeReplaced() ? free : wrong).add(shell);
        }
        if (free.isEmpty() && wrong.isEmpty()) return;

        event.addCustomRenderer((renderState, buffer, poseStack, translucentPass, levelState) -> {
            if (!translucentPass) return false;
            Vec3 camera = levelState.cameraRenderState.pos;
            float width = minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            var lines = buffer.getBuffer(RenderTypes.lines());
            for (BlockPos p : free) {
                ShapeRenderer.renderShape(poseStack, lines, Shapes.block(), p.getX() - camera.x, p.getY() - camera.y, p.getZ() - camera.z, FREE, width);
            }
            for (BlockPos p : wrong) {
                ShapeRenderer.renderShape(poseStack, lines, Shapes.block(), p.getX() - camera.x, p.getY() - camera.y, p.getZ() - camera.z, WRONG, width);
            }
            return false;
        });
    }
}
