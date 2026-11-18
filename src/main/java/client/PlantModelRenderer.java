package client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;

import java.util.List;

/**
 * Draws a {@link PlantModel} in the controller's block space: the world renderer submits it through a
 * {@link SubmitNodeCollector}, the guide draws it straight into a buffer. Stretchable machines lay
 * their sections out for {@code length} the same way their block layout grows.
 */
public final class PlantModelRenderer {
    private PlantModelRenderer() {}

    /** Where quads go: a submit collector in the world, a plain buffer source in the guide. */
    public interface Sink {
        void draw(PoseStack poseStack, RenderType type, List<PlantModel.Quad> quads, int light);

        static Sink of(SubmitNodeCollector collector) {
            return (poseStack, type, quads, light) -> {
                if (!quads.isEmpty()) collector.submitCustomGeometry(poseStack, type,
                    (pose, consumer) -> PlantModel.emitList(quads, pose, consumer, light));
            };
        }

        static Sink of(MultiBufferSource buffers) {
            return (poseStack, type, quads, light) -> {
                if (quads.isEmpty()) return;
                VertexConsumer consumer = buffers.getBuffer(type);
                PlantModel.emitList(quads, poseStack.last(), consumer, light);
            };
        }
    }

    public static void draw(PlantModel model, Sink sink, PoseStack poseStack, int length, float spin, boolean working, int light) {
        PlantModel.Stretch stretch = model.stretch;
        section(model, sink, poseStack, "head", 0, spin, working, light);
        if (stretch.baseLength() == 0) return;
        int extra = Math.max(0, length - stretch.baseLength());
        section(model, sink, poseStack, "tail", extra, spin, working, light);
        // A stand is a complete slice of its own, so bays leave its depths free.
        for (int depth = stretch.bayFirst(); depth <= length - 1 - stretch.bayLast(); depth++) {
            boolean stand = stretch.standAt(depth, length);
            section(model, sink, poseStack, stand ? "stand" : "bay", depth - (stand ? stretch.standFirst() : stretch.bayFirst()),
                spin, working, light);
        }
    }

    private static void section(PlantModel model, Sink sink, PoseStack poseStack, String name, float shift,
                                float spin, boolean working, int light) {
        PlantModel.Section section = model.sections.get(name);
        if (section == null) return;
        RenderType solid = RenderTypes.entityCutoutCull(model.texture);
        poseStack.pushPose();
        poseStack.translate(0, 0, shift);
        sink.draw(poseStack, solid, section.solid(), light);
        sink.draw(poseStack, solid, section.glow(), working ? LightCoordsUtil.FULL_BRIGHT : light);
        for (PlantModel.Spinner spinner : section.spinners()) {
            poseStack.pushPose();
            poseStack.translate(spinner.pivot().x, spinner.pivot().y, spinner.pivot().z);
            float angle = spin * spinner.speed();
            if (angle != 0) poseStack.mulPose(spinner.axis().rotationDegrees(angle));
            sink.draw(poseStack, solid, spinner.quads(), light);
            poseStack.popPose();
        }
        sink.draw(poseStack, RenderTypes.entityTranslucent(model.texture), section.translucent(), light);
        poseStack.popPose();
    }
}
