package compat.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class JeiPage {
    private JeiPage() {}
    static void energy(ITooltipBuilder tooltip, long total, long perTick) {
        if (total <= 0) {
            tooltip.add(Component.translatable("jei.iuce.energy_none").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("jei.iuce.energy_total", total).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("jei.iuce.energy_rate", perTick).withStyle(ChatFormatting.GRAY));
    }
}
