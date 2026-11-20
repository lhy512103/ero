package client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Render state snapshot for {@link PlantControllerBlockEntityRenderer}. */
public class PlantControllerRenderState extends BlockEntityRenderState {
    public boolean formed;
    public boolean mill, screw;
    public boolean legacy;
    public boolean working;
    public Direction facing = Direction.NORTH;
    public int length;
    public float spinAngle;
}
