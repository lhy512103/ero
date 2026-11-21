package regression;

import block.ModBlocks;
import block.PlantControllerBlock;
import blockentity.*;
import multiblock.*;
import machine.utility.Utility;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.*;
import java.util.function.Consumer;

final class IntegratedPlantTests {
    static void register(Map<String,Consumer<GameTestHelper>> tests) {
        if(Boolean.getBoolean("iuce.factoryBenchmark")) tests.put("factory_benchmark",IntegratedPlantTests::benchmark);
        for(String id:List.of("twin_screw_extruder","bio_ethylene_plant","carbide_furnace_plant","coke_oven_battery","waste_heat_power_plant"))
            tests.put("plant_"+id,h->run(h,id));
    }
    private static void benchmark(GameTestHelper helper){
        var server=helper.getLevel().getServer();
        int i=0;
        for(String id:List.of("twin_screw_extruder","bio_ethylene_plant","carbide_furnace_plant","coke_oven_battery","waste_heat_power_plant")){
            var pos=new BlockPos(5,70+15*i++,1);
            helper.setBlock(pos,block(id).defaultBlockState().setValue(PlantControllerBlock.LEGACY,false));
            var controller=helper.getBlockEntity(pos,PlantControllerBlockEntity.class);
            controller.assembleCreative();
            helper.assertTrue(controller.isFormed(),"benchmark plant formation");
            for(var cell:PlantStructure.cells(controller.getBlockPos(),Direction.NORTH,controller.getBlockState())){
                var entity=helper.getLevel().getBlockEntity(cell.pos());
                if(cell.part()==PlantStructure.Part.INPUT && entity instanceof HatchBlockEntity hatch)hatch.getInventory().set(0,ItemResource.of(new ItemStack(Items.COAL)),64);
                if(entity instanceof EnergyHatchBlockEntity hatch)hatch.storage().setEnergy(80000);
            }
        }
        for(int n=0;n<60;n++){
            var pos=new BlockPos(1+n%10,155,1+n/10);
            helper.setBlock(pos,block("coking_oven"));
            var machine=helper.getBlockEntity(pos,machine.process.ProcessMachineBlockEntity.class);
            ((energy.EUEnergyStorageImpl)machine.getEnergyStorage()).setEnergy(8000);
            machine.getItemHandler().set(0,ItemResource.of(new ItemStack(Items.COAL)),4);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"spark profiler start --interval 1");
        helper.runAfterDelay(160,()->{
            long[] samples=server.getTickTimesNanos().clone();
            Arrays.sort(samples);
            System.out.printf(Locale.ROOT,"[IUCE factory] 60 small + 5 formed, mean %.3f ms/tick, p95 %.3f ms/tick%n",Arrays.stream(samples).average().orElse(0)/1e6,samples[(samples.length*95-1)/100]/1e6);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"spark profiler stop --save-to-file");
            helper.runAfterDelay(500,helper::succeed);
        });
    }
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("iuce:"+id));}
    private static Item item(String id){return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));}
    private static void run(GameTestHelper h,String id){
        BlockPos at=new BlockPos(4,24,1);
        var state=block(id).defaultBlockState().setValue(PlantControllerBlock.LEGACY,false).setValue(PlantControllerBlock.FACING,Direction.NORTH);
        h.setBlock(at,state);
        var machine=h.getBlockEntity(at,PlantControllerBlockEntity.class);
        machine.assembleCreative();
        h.assertTrue(machine.isFormed(),id+" must form: "+machine.describe().getString());
        var cells=PlantStructure.cells(machine.getBlockPos(),Direction.NORTH,state);
        var hatches=new EnumMap<PlantStructure.Part,BlockPos>(PlantStructure.Part.class);
        for(var cell:cells)hatches.putIfAbsent(cell.part(),cell.pos());
        var level=h.getLevel();
        var input=((HatchBlockEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.INPUT))).getInventory();
        if(id.equals("waste_heat_power_plant")){
            input.set(0,ItemResource.of(new ItemStack(Items.COAL)),1);
            machine.tick();
            var generator=(EnergyHatchBlockEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.GENERATION));
            h.assertTrue(generator.storage().getEnergyStored()==512 && machine.ready() && machine.shares(Utility.STEAM)==64,"power plant must generate and supply steam");
            h.succeed();return;
        }
        var output=((HatchBlockEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.OUTPUT))).getInventory();
        var tools=((HatchBlockEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.MAINTENANCE))).getInventory();
        var tankIn=((PlantTankEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.FLUID_IN))).getTanks();
        var tankOut=new ArrayList<fluid.MachineTank>();
        for(var part:List.of(PlantStructure.Part.FLUID_OUT,PlantStructure.Part.GAS_OUT))
            if(hatches.containsKey(part))tankOut.addAll(((PlantTankEntity)level.getBlockEntity(hatches.get(part))).getTanks());
        var energy=((EnergyHatchBlockEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.HV))).storage();
        int tested=0;
        for(var recipe:PlantBatch.all(id)){
            if(id.equals("twin_screw_extruder") && (recipe.id().equals("soft_pvc_pellet") || recipe.id().startsWith("color_") && !recipe.id().equals("color_masterbatch")))continue;
            int index=0;
            for(var entry:recipe.inputs().entrySet()){
                var stack=new ItemStack(entry.getKey().equals("#c:dyes")?Items.RED_DYE:item(entry.getKey()));
                input.set(index++,ItemResource.of(stack),entry.getValue());
            }
            index=0;
            for(String ref:recipe.tools().keySet())tools.set(index++,ItemResource.of(new ItemStack(item(ref))),1);
            index=0;
            for(var entry:recipe.fluids().entrySet())tankIn.get(index++).fill(BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())),entry.getValue());
            if(!recipe.segments().isEmpty()){
                var segments=cells.stream().filter(c->c.part()==PlantStructure.Part.SEGMENT).toList();
                machine.tick();
                if(tested==0)h.assertTrue(machine.describe().getString().contains("segment") || machine.describe().getString().contains("段"),"missing segment status");
                for(int i=0;i<segments.size();i++){
                    String seg=i<recipe.segments().size()?recipe.segments().get(i):"melting";
                    level.setBlockAndUpdate(segments.get(i).pos(),block(seg+"_segment").defaultBlockState());
                }
                machine.markStructureDirty();
            }
            for(int i=0;i<recipe.time()+30;i++){energy.setEnergy(80000);machine.tick();}
            for(var entry:recipe.outputs().entrySet()){
                int amount=0;
                for(int i=0;i<output.size();i++)if(output.getResource(i).getItem()==item(entry.getKey()))amount+=output.getAmountAsInt(i);
                h.assertTrue(amount==entry.getValue(),id+"/"+recipe.id()+" missing "+entry.getKey()+": "+machine.describe().getString());
            }
            for(var entry:recipe.products().entrySet()){
                int amount=tankOut.stream().filter(t->!t.isEmpty() && BuiltInRegistries.FLUID.getKey(t.getResource().getFluid()).toString().equals(entry.getKey())).mapToInt(fluid.MachineTank::getAmount).sum();
                h.assertTrue(amount==entry.getValue(),"wrong fluid output "+entry.getKey());
                boolean gas=BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())).getFluidType().isLighterThanAir();
                if(gas)h.assertTrue(((PlantTankEntity)level.getBlockEntity(hatches.get(PlantStructure.Part.GAS_OUT))).getTanks().stream()
                    .anyMatch(t->!t.isEmpty() && BuiltInRegistries.FLUID.getKey(t.getResource().getFluid()).toString().equals(entry.getKey())),entry.getKey()+" must leave through the gas output hatch");
            }
            for(int i=0;i<output.size();i++)output.set(i,ItemResource.EMPTY,0);
            tested++;
        }
        h.assertTrue(tested>0,"no route tested");h.succeed();
    }
}
