package compat.jei;

import java.util.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import multiblock.PlantBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

final class PlantRecipeCategory extends AbstractRecipeCategory<PlantBatch> {
    static final List<String> PLANTS=List.of("twin_screw_extruder","bio_ethylene_plant","carbide_furnace_plant","coke_oven_battery","waste_heat_power_plant");
    static final Map<String,IRecipeType<PlantBatch>> TYPES=new LinkedHashMap<>();
    static { for(String name:PLANTS) TYPES.put(name,IRecipeType.create(Identifier.parse("iuce:"+name),PlantBatch.class)); }
    static ItemStack icon(String ref){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(ref)));}
    PlantRecipeCategory(IJeiHelpers helpers,String name){
        super(TYPES.get(name),icon("iuce:"+name).getHoverName(),helpers.getGuiHelper().createDrawableItemStack(icon("iuce:"+name)),155,74);
    }
    static List<PlantBatch> recipes(String name) {
        if(name.equals("waste_heat_power_plant")) return List.of(new PlantBatch("power",Map.of("minecraft:coal",1),Map.of(),Map.of(),Map.of(),Map.of(),512,1600,List.of()));
        return PlantBatch.all(name);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,PlantBatch batch,IFocusGroup focus){
        int i=0;
        for(var entry:batch.inputs().entrySet()){
            var slot=builder.addInputSlot(2+i++*18,2).setStandardSlotBackground();
            if(entry.getKey().startsWith("#"))BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s->PlantBatch.matches(s,entry.getKey())).forEach(s->slot.add(s.copyWithCount(entry.getValue())));
            else slot.add(icon(entry.getKey()).copyWithCount(entry.getValue()));
        }
        for(var entry:batch.fluids().entrySet())builder.addInputSlot(2+i++*18,2).setStandardSlotBackground().setFluidRenderer(entry.getValue(),true,16,16).add(BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())),entry.getValue());
        i=0;
        for(var ref:batch.tools().keySet())builder.addSlot(RecipeIngredientRole.CRAFTING_STATION,2+i++*18,23).setStandardSlotBackground().add(icon(ref));
        i=0;
        for(var entry:batch.outputs().entrySet())builder.addOutputSlot(80+i++*18,2).setStandardSlotBackground().add(icon(entry.getKey()).copyWithCount(entry.getValue()));
        for(var entry:batch.products().entrySet())builder.addOutputSlot(80+(i%4)*18,2+18*(i++/4)).setStandardSlotBackground().setFluidRenderer(entry.getValue(),true,16,16).add(BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())),entry.getValue());
        for(int n=0;n<batch.segments().size();n++)builder.addSlot(RecipeIngredientRole.CRAFTING_STATION,2+n*18,44).add(icon("iuce:"+batch.segments().get(n)+"_segment"));
    }
    @Override public void draw(PlantBatch b,IRecipeSlotsView slots,GuiGraphicsExtractor g,double x,double y){
        String cost=b.id().equals("power")?"512 EU/t · 64 steam":b.energy()+" EU · "+b.time()+" t";
        g.text(Minecraft.getInstance().font,cost,2,63,0xff454545,false);
    }
}
