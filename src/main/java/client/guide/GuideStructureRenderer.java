package client.guide;

import client.PlantModel;
import client.PlantModelRenderer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a guide structure as real block models with their facing and axis, or, for a large machine
 * shown as formed, its assembled model. Faces against solid neighbours are skipped so joined
 * casings read as one surface.
 */
public final class GuideStructureRenderer extends PictureInPictureRenderer<GuideStructureRenderState> {
    private static final Direction[] SIDES = Direction.values();
    private final List<BlockStateModelPart> parts = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private final QuadInstance instance = new QuadInstance();

    public GuideStructureRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<GuideStructureRenderState> getRenderStateClass() {
        return GuideStructureRenderState.class;
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected void renderToTexture(GuideStructureRenderState state, PoseStack poseStack) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        GuideStructure structure = state.structure();
        // GUI y runs down; turning half round about z puts the sky on top, as the entity preview does.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw()));
        poseStack.translate(-state.centre().x, -state.centre().y, -state.centre().z);

        PlantModel model = state.formed() ? structure.model() : null;
        if (model != null) {
            poseStack.pushPose();
            structure.toModel(poseStack);
            PlantModelRenderer.draw(model, PlantModelRenderer.Sink.of(bufferSource), poseStack,
                structure.machineLength, state.spin(), true, 0xF000F0);
            poseStack.popPose();
            return;
        }

        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        for (GuideStructure.Cell cell : structure.cells) {
            if (cell.pos().getY() >= state.layers()) continue;
            parts.clear();
            random.setSeed(42L);
            models.get(cell.state()).collectParts(random, parts);
            poseStack.pushPose();
            poseStack.translate(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ());
            for (BlockStateModelPart part : parts) {
                for (var quad : part.getQuads(null)) consumer.putBakedQuad(poseStack.last(), quad, instance);
                for (Direction side : SIDES) {
                    if (hidden(structure, cell, side, state.layers())) continue;
                    for (var quad : part.getQuads(side)) consumer.putBakedQuad(poseStack.last(), quad, instance);
                }
            }
            poseStack.popPose();
        }
        if (state.highlight() != null) {
            var p = state.highlight();
            var box = net.minecraft.world.phys.shapes.Shapes.box(-0.002, -0.002, -0.002, 1.002, 1.002, 1.002);
            bufferSource.endBatch();
            var lines = bufferSource.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
            var pose = poseStack.last();
            float hue = (System.currentTimeMillis() % 3000L) / 3000F;
            box.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                var normal = new org.joml.Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
                int color1 = 0xFF000000 | java.awt.Color.HSBtoRGB((hue + (float) (x1 + y1 + z1) / 3F) % 1F, 0.8F, 1F);
                int color2 = 0xFF000000 | java.awt.Color.HSBtoRGB((hue + (float) (x2 + y2 + z2) / 3F) % 1F, 0.8F, 1F);
                lines.addVertex(pose, (float) (p.getX() + x1), (float) (p.getY() + y1), (float) (p.getZ() + z1))
                    .setColor(color1).setNormal(pose, normal).setLineWidth(3F);
                lines.addVertex(pose, (float) (p.getX() + x2), (float) (p.getY() + y2), (float) (p.getZ() + z2))
                    .setColor(color2).setNormal(pose, normal).setLineWidth(3F);
            });
        }
    }

    /** A culled face is covered by a solid neighbour that is itself shown. */
    private static boolean hidden(GuideStructure structure, GuideStructure.Cell cell, Direction side, int layers) {
        var pos = cell.pos().relative(side);
        if (pos.getY() >= layers) return false;
        BlockState neighbour = structure.stateAt(pos);
        return neighbour != null && neighbour.isSolidRender();
    }

    @Override
    protected String getTextureLabel() {
        return "iuce guide structure";
    }
}
