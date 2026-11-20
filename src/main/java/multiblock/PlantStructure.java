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
        CASING, SOCKET, INPUT, OUTPUT, POWER, FRAME_UP, FRAME_ALONG, FRAME_ACROSS, DRUM_UP, DRUM_ALONG, DRUM_ACROSS,
        FLUID_IN, FLUID_OUT, MAINTENANCE, GENERATION, HV, SEGMENT;

        public boolean canFillWithCasing() { return this == CASING || this == SOCKET; }

        public BlockState sample(Direction facing) {
            return switch (this) {
                case CASING, SOCKET -> ModBlocks.PLANT_CASING.get().defaultBlockState();
                case FLUID_IN -> ModBlocks.FLUID_INPUT_HATCH.get().defaultBlockState();
                case FLUID_OUT -> ModBlocks.FLUID_OUTPUT_HATCH.get().defaultBlockState();
                case MAINTENANCE -> ModBlocks.MAINTENANCE_HATCH.get().defaultBlockState();
                case GENERATION -> ModBlocks.GENERATION_HATCH.get().defaultBlockState();
                case HV -> ModBlocks.ENERGY_HATCH_HV.get().defaultBlockState();
                case SEGMENT -> ModBlocks.MELTING_SEGMENT.get().defaultBlockState();
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
                case INPUT, OUTPUT, CASING, FLUID_IN, FLUID_OUT, MAINTENANCE, GENERATION, HV -> state.is(sample(facing).getBlock());
                case SEGMENT -> state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.parse("iuce:screw_segments")));
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

    private record Definition(int base_y, int minimum, int maximum, int repeat_start, int repeat_size,
                              java.util.Map<String,List<List<String>>> layouts, List<List<String>> layers) {
        List<List<String>> layersAt(int length) { return layouts == null ? layers : layouts.get(Integer.toString(length)); }
        int sourceDepth(int depth,int length) {
            if (layouts != null || length == minimum || depth < repeat_start) return depth;
            int extra=length-minimum;
            return depth < repeat_start+extra ? repeat_start+(depth-repeat_start)%repeat_size : depth-extra;
        }
    }
    private static final java.util.Map<String,Definition> DEFINITIONS = new java.util.concurrent.ConcurrentHashMap<>();
    public static String industrialId(BlockState state) {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return switch(id) {
            case "twin_screw_extruder","bio_ethylene_plant","carbide_furnace_plant","coke_oven_battery","waste_heat_power_plant" -> id;
            default -> null;
        };
    }
    private static Definition definition(BlockState state) {
        String id=industrialId(state);
        if(id==null) {
            boolean old=state.getValue(PlantControllerBlock.LEGACY);
            id=state.is(ModBlocks.MILL_CONTROLLER.get()) ? (old?"legacy_mill":"mill") : (old?"legacy":"ore");
        }
        return DEFINITIONS.computeIfAbsent(id, name -> {
            try(var stream=PlantStructure.class.getResourceAsStream("/iuce/plants/"+name+".json")) {
                if(stream==null) throw new IllegalStateException("Missing plant layout "+name);
                return new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8),Definition.class);
            } catch(java.io.IOException ex) { throw new IllegalStateException(name,ex); }
        });
    }
    public static int length(BlockState state) {
        var d=definition(state);
        if(d.layouts()!=null) return state.is(ModBlocks.MILL_CONTROLLER.get()) ? Math.clamp(state.getValue(PlantControllerBlock.LENGTH),9,17)
            : state.getValue(PlantControllerBlock.LEGACY)?5:ORE_LENGTH;
        return Math.clamp(state.getValue(PlantControllerBlock.LENGTH),d.minimum(),d.maximum());
    }
    public static List<Cell> cells(BlockPos origin,Direction facing,BlockState state) {
        Definition d=definition(state);
        int length=length(state);
        var layers=d.layersAt(length);
        List<Cell> cells=new ArrayList<>();
        for(int y=0;y<layers.size();y++) for(int depth=0;depth<length;depth++) {
            String row=layers.get(y).get(d.sourceDepth(depth,length));
            for(int x=0;x<row.length();x++) {
                char symbol=row.charAt(x);
                if(symbol=='.'||symbol=='@') continue;
                Part part=Part.values()["CSIOEVABXYZLRMJHG".indexOf(symbol)];
                cells.add(new Cell(at(origin,facing,depth,x-row.length()/2,y+d.base_y()),part));
            }
        }
        return List.copyOf(cells);
    }
    public static BoundingBox bounds(BlockPos origin,Direction facing,BlockState state) {
        var d=definition(state);
        int max=d.layouts()==null?d.maximum():state.is(ModBlocks.MILL_CONTROLLER.get())?17:length(state);
        var layers=d.layersAt(max);
        int radius=layers.getFirst().getFirst().length()/2;
        return BoundingBox.fromCorners(at(origin,facing,0,-radius,d.base_y()),at(origin,facing,max-1,radius,d.base_y()+layers.size()-1));
    }
}
