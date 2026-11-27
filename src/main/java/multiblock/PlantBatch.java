package multiblock;

import blockentity.PlantTankEntity;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public record PlantBatch(String id, Map<String,Integer> inputs, Map<String,Integer> outputs,
                         Map<String,Integer> fluids, Map<String,Integer> products, Map<String,Integer> tools,
                         long energy, int time, List<String> segments) {
    private static final Map<String,List<PlantBatch>> CACHE=new HashMap<>();
    public static synchronized List<PlantBatch> all(String plant) {
        if (plant.equals("waste_heat_power_plant")) return List.of();
        return CACHE.computeIfAbsent(plant,key -> {
            try(var stream=PlantBatch.class.getResourceAsStream("/iuce/plants/batches/"+key+".json")) {
                if(stream==null) throw new IllegalStateException("Missing plant batches "+key);
                return List.of(new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),PlantBatch[].class));
            } catch(java.io.IOException ex){throw new IllegalStateException(key,ex);}
        });
    }
    public static boolean matches(ItemStack stack,String ref) {
        return ref.startsWith("#") ? stack.is(TagKey.create(Registries.ITEM,Identifier.parse(ref.substring(1)))) :
            stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(ref)));
    }
    public record Ports(List<ItemStacksResourceHandler> in,List<ItemStacksResourceHandler> out,
                        List<ItemStacksResourceHandler> maintenance,List<PlantTankEntity> supply,List<PlantTankEntity> drain,List<PlantTankEntity> vent) {}
    public boolean transfer(Ports ports,int batches,boolean commit) {
        try(var tx=Transaction.openRoot()){
            net.minecraft.world.item.DyeColor color=null;
            for(var entry:inputs.entrySet()){
                int left=entry.getValue()*batches;
                for(var inventory:ports.in) for(int slot=0;slot<inventory.size() && left>0;slot++){
                    var resource=inventory.getResource(slot);
                    if(resource.isEmpty() || !matches(resource.toStack(1),entry.getKey()))continue;
                    var dye=resource.toStack(1).get(DataComponents.DYE);
                    if(dye!=null && color!=null && color!=dye) continue;
                    if(dye!=null) color=dye;
                    left-=inventory.extract(slot,resource,left,tx);
                }
                if(left>0)return false;
            }
            for(var entry:tools.entrySet()){
                boolean found=false;
                for(var inventory:ports.maintenance) for(int slot=0;slot<inventory.size() && !found;slot++){
                    var resource=inventory.getResource(slot);
                    if(resource.isEmpty() || !matches(resource.toStack(1),entry.getKey()))continue;
                    var stack=resource.toStack(1);
                    int wear=entry.getValue()*batches;
                    if(wear>0){
                        int damage=stack.getDamageValue()+wear;
                        if(damage>stack.getMaxDamage())continue;
                        if(inventory.extract(slot,resource,1,tx)!=1)return false;
                        if(damage<stack.getMaxDamage()){
                            stack.setDamageValue(damage);
                            if(inventory.insert(slot,ItemResource.of(stack),1,tx)!=1)return false;
                        }else if(!insert(ports.out,new ItemStack(item.ModItems.SPENT_CATALYST.get()),1,tx))return false;
                    }
                    found=true;
                }
                if(!found)return false;
            }
            for(var entry:fluids.entrySet()) {
                var resource=FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())));
                int left=entry.getValue()*batches;
                for(var hatch:ports.supply)for(var tank:hatch.getTanks())if(left>0)left-=tank.extract(0,resource,left,tx);
                if(left>0)return false;
            }
            for(var entry:products.entrySet()) {
                var fluid=BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey()));
                var resource=FluidResource.of(fluid);
                int left=entry.getValue()*batches;
                // gases leave through the gas output hatch, liquids through the fluid output hatch
                for(var hatch:fluid.getFluidType().isLighterThanAir()?ports.vent:ports.drain)for(var tank:hatch.getTanks())if(left>0)left-=tank.insert(0,resource,left,tx);
                if(left>0)return false;
            }
            for(var entry:outputs.entrySet()){
                var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getKey())));
                if(color!=null){
                    stack.set(DataComponents.DYE,color);
                    stack.set(DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(color.getTextureDiffuseColor()));
                }
                if(!insert(ports.out,stack,batches*entry.getValue(),tx))return false;
            }
            if(commit)tx.commit();
            return true;
        }
    }
    private static boolean insert(List<ItemStacksResourceHandler> stores,ItemStack stack,int amount,Transaction tx){
        for(var store:stores)if(amount>0)amount-=store.insert(ItemResource.of(stack),amount,tx);
        return amount==0;
    }
}
