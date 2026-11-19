package client.guide;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * One frame of a {@code {structure: ...}} view: which structure, how much of it, and from where.
 * {@code formed} swaps the block parts for the machine's assembled model; the view turns about
 * {@code centre}, in layout blocks.
 */
public record GuideStructureRenderState(
    GuideStructure structure,
    int layers,
    boolean formed,
    Vector3f centre,
    float yaw,
    float pitch,
    float spin,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuideStructureRenderState(GuideStructure structure, int layers, boolean formed, Vector3f centre,
                                     float yaw, float pitch, float spin,
                                     int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
        this(structure, layers, formed, centre, yaw, pitch, spin, x0, y0, x1, y1, scale, scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
