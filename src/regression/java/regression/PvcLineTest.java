package regression;

import energy.EUEnergyStorageImpl;
import init.ModFluids;
import machine.process.ProcessMachineBlockEntity;
import machine.process.ProcessMachines;
import machine.utility.UtilityNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.item.ItemResource;
import recipe.ProcessRecipe;
import recipe.cache.ProcessRecipeCache;
import java.util.*;

final class PvcLineTest {
    record Station(ProcessMachineBlockEntity machine, ProcessRecipe recipe) {}
    static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":") ? id : "iuce:"+id)); }
    static void run(GameTestHelper helper) {
        Map<Object,Integer> stock = new HashMap<>();
        List<Station> line = new ArrayList<>();
        String[] routes = {"coking_oven/coke","lime_kiln/limestone","lime_kiln/recover_lime","carbide_furnace/carbide",
            "acetylene_generator/acetylene","filter_press/slag","brine_dissolver/raw_brine","brine_purifier/brine",
            "membrane_cell/electrolysis","hcl_furnace/synthesis","scrubber/acetylene","vcm_converter/crude_vcm",
            "scrubber/vcm_gas","gas_compressor/liquefied_vcm","distillation_column/topped_vcm","distillation_column/vinyl_chloride",
            "neutralizer/spent_lye","water_treatment/deionized_water","polymerization_reactor/pvc_slurry","flash_tank/strip",
            "centrifuge/wet_pvc","dryer/pvc_resin","high_speed_mixer/rigid"};
        for (int i=0;i<routes.length;i++) {
            String route=routes[i], id=route.split("/")[0];
            BlockPos pos=new BlockPos(1+i%6*2,1,1+i/6*2);
            helper.setBlock(pos,ProcessMachines.get(id).block().get());
            helper.setBlock(pos.above(),block.ModBlocks.CREATIVE_UTILITY_SOURCE.get());
            var machine=helper.getBlockEntity(pos,ProcessMachineBlockEntity.class);
            var recipe=ProcessRecipeCache.recipes(helper.getLevel(),id).stream()
                .filter(h->h.id().identifier().getPath().equals(route)).findFirst().orElseThrow().value();
            line.add(new Station(machine,recipe));
            recipe.catalyst().ifPresent(c->machine.getItemHandler().set(machine.def().catalystIndex(),
                ItemResource.of(new ItemStack(c.ingredient().items().findFirst().orElseThrow())),1));
        }
        UtilityNetwork.of(helper.getLevel()).clear();
        stock.put(item("minecraft:coal"),512);
        stock.put(item("limestone"),128);
        stock.put(item("rock_salt"),60);
        stock.put(Fluids.WATER,200000);
        for(String supply:List.of("dispersant","initiator","heat_stabilizer","pe_wax","calcium_carbonate")) stock.put(item(supply),128);
        for (int tick=0; tick<12000; tick++) {
            // A macerator supplies two crushed carbide for every lump.
            int lumps=stock.getOrDefault(item("carbide_lump"),0);
            if(lumps>0){stock.put(item("carbide_lump"),0);stock.merge(item("calcium_carbide"),lumps*2,Integer::sum);}
            for(var station:line) {
                var m=station.machine();var r=station.recipe();
                boolean empty=true;
                for(int slot:m.getInputSlots()) empty &= m.getItemHandler().getAmountAsInt(slot)==0;
                for(int i=0;i<m.getInputTankCount();i++) empty &= m.getTanks().get(i).isEmpty();
                if(empty) {
                    List<Item> items=new ArrayList<>();
                    List<Fluid> fluids=new ArrayList<>();
                    boolean available=true;
                    for(var in:r.items()){
                        Item chosen=in.ingredient().items().findFirst().orElseThrow().value();
                        items.add(chosen);available &= stock.getOrDefault(chosen,0)>=in.count();
                    }
                    for(var in:r.fluids()){
                        Fluid chosen=in.ingredient().fluids().getFirst().value();
                        if(r.machine().equals("water_treatment")) chosen=ModFluids.chemical("process_water");
                        fluids.add(chosen);available &= stock.getOrDefault(chosen,0)>=in.amount();
                    }
                    if(available) {
                        for(int i=0;i<items.size();i++){
                            int n=r.items().get(i).count();var key=items.get(i);
                            stock.merge(key,-n,Integer::sum);m.getItemHandler().set(i,ItemResource.of(new ItemStack(key)),n);
                        }
                        for(int i=0;i<fluids.size();i++){
                            int n=r.fluids().get(i).amount();var key=fluids.get(i);
                            stock.merge(key,-n,Integer::sum);m.getTanks().get(i).fill(key,n);
                        }
                    }
                }
                var power=(EUEnergyStorageImpl)m.getEnergyStorage();
                power.setEnergy(power.getMaxEnergyStored());m.tick();
                for(int slot:m.getOutputSlots()){
                    var inv=m.getItemHandler();int n=inv.getAmountAsInt(slot);
                    if(n>0){stock.merge(inv.getResource(slot).toStack(1).getItem(),n,Integer::sum);inv.set(slot,ItemResource.EMPTY,0);}
                }
                for(int i=m.getInputTankCount();i<m.getTanks().size();i++){
                    var tank=m.getTanks().get(i);
                    if(!tank.isEmpty()){Fluid key=tank.getResource().getFluid();stock.merge(key,tank.drain(tank.getAmount()),Integer::sum);}
                }
            }
        }
        helper.assertTrue(stock.getOrDefault(item("rigid_pvc_dry_blend"),0)>=8,"coal, limestone and salt must reach the high-speed mixer");
        helper.assertTrue(stock.getOrDefault(item("carbide_slag"),0)<=1,"slag must return to the kiln");
        helper.assertTrue(stock.getOrDefault(ModFluids.chemical("spent_lye"),0)<1000,"spent lye must return to the brine supply");
        helper.assertTrue(stock.getOrDefault(ModFluids.chemical("process_water"),0)<1000,"treatment must keep up with waste water");
        ItemStack batch=new ItemStack(item("rigid_pvc_dry_blend"),8);
        for(String id:List.of("compounding_extruder","strand_pelletizer","single_screw_extruder","cooling_tank","haul_off_cutter")){
            BlockPos pos=new BlockPos(1,1,1);
            helper.setBlock(pos,ProcessMachines.get(id).block().get());
            var station=helper.getBlockEntity(pos,ProcessMachineBlockEntity.class);
            station.getItemHandler().set(0,ItemResource.of(batch),batch.getCount());
            if(id.equals("single_screw_extruder"))station.getItemHandler().set(station.def().catalystIndex(),ItemResource.of(new ItemStack(item("pipe_die"))),1);
            UtilityNetwork.of(helper.getLevel()).clear();
            for(int t=0;t<205;t++){var energy=(EUEnergyStorageImpl)station.getEnergyStorage();energy.setEnergy(energy.getMaxEnergyStored());station.tick();}
            int slot=station.def().outputStart();
            batch=station.getItemHandler().getResource(slot).toStack(station.getItemHandler().getAmountAsInt(slot));
            helper.assertTrue(!batch.isEmpty(),id+" must produce the next intermediate");
        }
        helper.assertTrue(batch.is(item("pvc_pipe")) && batch.getCount()==16,"coal line must deliver sixteen PVC pipes");
        helper.succeed();
    }
}
