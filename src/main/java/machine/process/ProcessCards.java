package machine.process;

import item.ModItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import recipe.ProcessRecipe.CardRequirement;
import recipe.ProcessRecipeSerializer;
import java.util.function.Supplier;

public final class ProcessCards {
    public static final DeferredRegister<DataComponentType<?>> REGISTER = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "iuce");
    public static final Supplier<DataComponentType<CardRequirement>> TYPE = REGISTER.register("process_card",
        () -> DataComponentType.<CardRequirement>builder().persistent(ProcessRecipeSerializer.CARD).build());

    /** Which large machine a structure manual describes. */
    public static final Supplier<DataComponentType<String>> STRUCTURE = REGISTER.register("structure_manual",
        () -> DataComponentType.<String>builder().persistent(com.mojang.serialization.Codec.STRING)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8).build());

    public static boolean matches(ItemStack stack, CardRequirement required) {
        var card = stack.get(TYPE.get());
        return stack.is(ModItems.PROCESS_CARD.get()) && card != null && card.family().equals(required.family())
            && card.options().entrySet().containsAll(required.options().entrySet());
    }

    public static Component label(CardRequirement card) {
        var options = Component.empty();
        card.options().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
            if (!options.getString().isEmpty()) options.append(" / ");
            options.append(Component.translatableWithFallback("gui.iuce.design." + entry.getKey() + "." + entry.getValue(),
                entry.getKey() + " " + entry.getValue()));
        });
        return Component.translatable("gui.iuce.design.card", Component.translatableWithFallback("gui.iuce.design." + card.family(), card.family()), options);
    }

    public static ItemStack write(CardRequirement card) {
        ItemStack stack = new ItemStack(ModItems.PROCESS_CARD.get());
        stack.set(TYPE.get(), card);
        stack.set(DataComponents.ITEM_NAME, label(card));
        return stack;
    }
}
