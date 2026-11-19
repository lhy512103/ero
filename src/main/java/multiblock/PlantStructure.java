package multiblock;

import block.ModBlocks;
import block.PlantControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public final class PlantStructure {
    public static final int ORE_LENGTH = 7;

    public enum Part {
        CASING, SOCKET, INPUT, OUTPUT, POWER, FRAME_UP, FRAME_ALONG, FRAME_ACROSS, DRUM_UP, DRUM_ALONG, DRUM_ACROSS;

        public boolean canFillWithCasing() { return this == CASING || this == SOCKET; }

        public BlockState sample(Direction facing) {
            return switch (this) {
                case CASING, SOCKET -> ModBlocks.PLANT_CASING.get().defaultBlockState();
                case INPUT -> ModBlocks.ITEM_INPUT_HATCH.get().defaultBlockState();
                case OUTPUT -> ModBlocks.ITEM_OUTPUT_HATCH.get().defaultBlockState();
                case POWER -> ModBlocks.ENERGY_HATCH.get().defaultBlockState()
                    .setValue(block.MachineBlock.FACING, facing.getOpposite());
                case FRAME_UP, FRAME_ALONG, FRAME_ACROSS -> ModBlocks.PLANT_FRAME.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis(facing));
                case DRUM_UP, DRUM_ALONG, DRUM_ACROSS -> ModBlocks.PLANT_DRUM.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis(facing));
            };
        }

        private Direction.Axis axis(Direction facing) {
            return switch (this) {
                case FRAME_ALONG, DRUM_ALONG -> facing.getAxis();
                case FRAME_ACROSS, DRUM_ACROSS -> facing.getClockWise().getAxis();
                default -> Direction.Axis.Y;
            };
        }

        public boolean accepts(BlockState state, Direction facing) {
            return switch (this) {
                case SOCKET -> state.is(ModBlocks.PLANT_CASING.get()) || state.is(ModBlocks.ITEM_INPUT_HATCH.get())
                    || state.is(ModBlocks.ITEM_OUTPUT_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH.get())
                    || state.is(ModBlocks.ENERGY_HATCH_HV.get());
                case POWER -> state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get());
                case INPUT, OUTPUT, CASING -> state.is(sample(facing).getBlock());
                default -> state.is(sample(facing).getBlock()) && state.getValue(RotatedPillarBlock.AXIS) == axis(facing);
            };
        }
    }

    public record Cell(BlockPos pos, Part part) {}

    private PlantStructure() {}

    public static BlockPos at(BlockPos origin, Direction facing, int depth, int across, int up) {
        Direction back = facing.getOpposite();
        return origin.relative(back, depth).relative(back.getCounterClockWise(), across).above(up);
    }

    public static List<Cell> cells(BlockPos origin, Direction facing, BlockState controller) {
        boolean mill = controller.is(ModBlocks.MILL_CONTROLLER.get());
        boolean legacy = controller.getValue(PlantControllerBlock.LEGACY);
        int length = mill ? controller.getValue(PlantControllerBlock.LENGTH) : legacy ? 5 : ORE_LENGTH;
        List<Cell> cells = new ArrayList<>();
        int radius = legacy ? 2 : mill ? 4 : 2;
        int minY = legacy || mill ? -1 : 0;
        int roof = legacy ? 3 : 5;
        for (int up = minY; up <= roof; up++) {
            for (int depth = 0; depth < length; depth++) {
                for (int across = -radius; across <= radius; across++) {
                    if (depth == 0 && across == 0 && up == 0) continue;
                    Part part = legacy ? legacy(depth, Math.abs(across), up, length)
                        : mill ? mill(depth, across, up, length) : ore(depth, across, up);
                    if (part != null) cells.add(new Cell(at(origin, facing, depth, across, up), part));
                }
            }
        }
        return List.copyOf(cells);
    }

    public static BoundingBox bounds(BlockPos origin, Direction facing, BlockState controller) {
        boolean mill = controller.is(ModBlocks.MILL_CONTROLLER.get());
        boolean legacy = controller.getValue(PlantControllerBlock.LEGACY);
        int radius = legacy ? 2 : mill ? 4 : 2;
        int minY = legacy || mill ? -1 : 0;
        int roof = legacy ? 3 : 5;
        int length = mill ? 17 : legacy ? 5 : ORE_LENGTH;
        return BoundingBox.fromCorners(at(origin, facing, 0, -radius, minY), at(origin, facing, length - 1, radius, roof));
    }

    private static Part legacy(int depth, int side, int up, int length) {
        return depth == 0 || depth == length - 1 || side == 2 || up == -1 || up == 3 ? Part.SOCKET : null;
    }

    private static Part ore(int depth, int across, int up) {
        int side = Math.abs(across);
        if (up == 0) {
            if (depth == 0) {
                if (across == -1) return Part.INPUT;
                if (across == 1) return Part.OUTPUT;
                return side == 2 ? Part.SOCKET : Part.CASING;
            }
            if (depth == 6) {
                if (across == 0) return Part.POWER;
                return side == 2 ? Part.SOCKET : Part.CASING;
            }
            if ((depth == 2 || depth == 4) && side == 2) return Part.SOCKET;
            return Part.CASING;
        }
        if (up == 1 || up == 2) {
            if ((depth == 0 || depth == 2 || depth == 4) && side == 2) return Part.FRAME_UP;
            if (across == -2 && depth == 3) return Part.CASING;
            if (depth <= 2 && side <= 1) {
                if (depth == 1) return Part.DRUM_ACROSS;
                return up == 1 ? Part.CASING : null;
            }
            if ((depth == 3 || depth == 4) && across == 0) return up == 1 ? Part.CASING : Part.DRUM_ALONG;
            if (depth >= 5 && side <= 1) return side == 1 ? Part.FRAME_UP : Part.CASING;
            return null;
        }
        if (up == 3) {
            if (side == 2 && depth <= 4) return Part.FRAME_ALONG;
            if ((depth == 0 || depth == 2 || depth == 4) && side <= 1) return Part.FRAME_ACROSS;
            if (depth == 1 && side <= 1) return Part.CASING;
            if (depth >= 5 && side <= 1) return Part.CASING;
            return null;
        }
        if (up == 4) {
            if (depth <= 2 && side <= 1 && (depth == 0 || depth == 2 || side == 1)) return Part.CASING;
            if (depth == 5 && across == -1) return Part.CASING;
            if (depth == 5 && across == 1) return Part.DRUM_UP;
            return null;
        }
        if (up == 5 && depth == 5 && across == 1) return Part.DRUM_UP;
        return null;
    }

    private static Part mill(int depth, int across, int up, int length) {
        int side = Math.abs(across);
        boolean endBay = depth <= 1 || depth >= length - 2;
        boolean gantry = depth == 2 || depth == length - 3 || (depth > 2 && depth < length - 3 && (depth - 2) % 4 == 0);
        if (up == -1 && (side <= 3 || (side == 4 && (gantry || endBay)))) return Part.CASING;
        if (up == 0) {
            if (depth == 0 && across == -1) return Part.INPUT;
            if (depth == 0 && across == 1) return Part.OUTPUT;
            if (depth == length - 1 && side == 0) return Part.POWER;
            if (side == 4 && endBay) return Part.SOCKET;
            if (side == 3) return Part.FRAME_ALONG;
            if (side <= 2) return Part.DRUM_ACROSS;
        }
        if (gantry && side == 4 && up >= 0 && up <= 4) return Part.FRAME_UP;
        if (gantry && up == 5 && side <= 4) return Part.FRAME_ACROSS;
        if (gantry && (up == 1 || up == 3) && side <= 1) return Part.DRUM_ACROSS;
        if (endBay && up >= 1 && up <= 2 && side == 3) return up == 1 ? Part.CASING : Part.FRAME_ALONG;
        if (depth >= length - 2 && up >= 1 && up <= 3 && side <= 1) return up == 3 ? Part.DRUM_ALONG : Part.CASING;
        if (up == 3 && side == 3 && depth >= 2 && depth <= length - 3) return Part.FRAME_ALONG;
        return null;
    }
}
