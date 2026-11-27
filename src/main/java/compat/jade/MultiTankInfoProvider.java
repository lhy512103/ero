package compat.jade;

import fluid.MachineTank;
import fluid.MultiTankProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

/** Lists every non-empty tank of a {@link MultiTankProvider}, marked as input or output. */
public class MultiTankInfoProvider implements StreamServerDataProvider<BlockAccessor, List<MultiTankInfoProvider.Tank>> {
    public static final MultiTankInfoProvider INSTANCE = new MultiTankInfoProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("iuce", "multi_tank_info");

    public record Tank(boolean input, Fluid fluid, int amount, int capacity) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Tank> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, Tank::input,
            ByteBufCodecs.registry(net.minecraft.core.registries.Registries.FLUID), Tank::fluid,
            ByteBufCodecs.VAR_INT, Tank::amount,
            ByteBufCodecs.VAR_INT, Tank::capacity,
            Tank::new);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Tank>> CODEC = Tank.STREAM_CODEC.apply(ByteBufCodecs.list());

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, List<Tank>> streamCodec() {
        return CODEC;
    }

    @Override
    public List<Tank> streamData(BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MultiTankProvider provider)) return null;
        List<Tank> tanks = new ArrayList<>();
        List<MachineTank> all = provider.getTanks();
        for (int i = 0; i < all.size(); i++) {
            MachineTank tank = all.get(i);
            if (tank.isEmpty()) continue;
            tanks.add(new Tank(i < provider.getInputTankCount(), tank.getResource().getFluid(), tank.getAmount(), tank.getCapacity()));
        }
        return tanks.isEmpty() ? null : tanks;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    public static class Client implements IBlockComponentProvider {
        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            List<Tank> tanks = MultiTankInfoProvider.INSTANCE.decodeFromData(accessor).orElse(null);
            if (tanks == null) return;
            for (Tank tank : tanks) {
                tooltip.add(Component.translatable(tank.input() ? "tooltip.iuce.jade_tank_in" : "tooltip.iuce.jade_tank_out",
                    tank.fluid().getFluidType().getDescription(), tank.amount(), tank.capacity()));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
