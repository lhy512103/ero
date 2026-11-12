package block;

import net.minecraft.util.StringRepresentable;

/** How items reach a belt: from behind (straight), or only from its left or right side (a curve). */
public enum ConveyorShape implements StringRepresentable {
    STRAIGHT("straight"), LEFT("left"), RIGHT("right");

    private final String name;

    ConveyorShape(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }
}
