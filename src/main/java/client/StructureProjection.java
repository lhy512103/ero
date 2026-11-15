package client;

import block.PlantControllerBlock;
import multiblock.PlantStructure;
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
 * casing positions in yellow, dedicated parts in blue and incorrect positions in red.
 */
public final class StructureProjection {
    private StructureProjection() {}

    private static final int FREE = 0xFFFFD21E, PART = 0xFF64D3FF, WRONG = 0xFFFF5050;

    private record Outline(BlockPos pos, int color) {}

    public static void onOutline(ExtractBlockOutlineRenderStateEvent event) {
        BlockState state = event.getBlockState();
        if (!(state.getBlock() instanceof PlantControllerBlock) || state.getValue(PlantControllerBlock.FORMED)) return;
        Minecraft minecraft = Minecraft.getInstance();
        var level = event.getLevel();
        BlockPos pos = event.getBlockPos();
        Direction facing = state.getValue(PlantControllerBlock.FACING);
        List<Outline> outlines = new ArrayList<>();
        for (PlantStructure.Cell cell : PlantStructure.cells(pos, facing, state)) {
            if (!level.isLoaded(cell.pos())) continue;
            BlockState there = level.getBlockState(cell.pos());
            if (cell.part().accepts(there, facing)) continue;
            int color = !there.canBeReplaced() ? WRONG : cell.part().canFillWithCasing() ? FREE : PART;
            outlines.add(new Outline(cell.pos(), color));
        }
        if (outlines.isEmpty()) return;

        event.addCustomRenderer((renderState, buffer, poseStack, translucentPass, levelState) -> {
            if (!translucentPass) return false;
            Vec3 camera = levelState.cameraRenderState.pos;
            float width = minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            var lines = buffer.getBuffer(RenderTypes.lines());
            for (Outline outline : outlines) {
                BlockPos p = outline.pos();
                ShapeRenderer.renderShape(poseStack, lines, Shapes.block(), p.getX() - camera.x, p.getY() - camera.y, p.getZ() - camera.z, outline.color(), width);
            }
            return false;
        });
    }
}
