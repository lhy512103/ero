package compat.jade;

import blockentity.cable.CableBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

/**
 * 导线 Jade 兼容 - 显示网络级统一能量缓冲和传输信息
 */
public class CableEnergyProvider implements StreamServerDataProvider<BlockAccessor, CableEnergyProvider.Data> {

    public static final CableEnergyProvider INSTANCE = new CableEnergyProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("iuce", "cable_energy");

    private static final int DARK_RED = 0xFFAA0000;

    public record Data(long energy, long capacity, String voltageName, long transferRate, int cableCount) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Data::energy,
                ByteBufCodecs.VAR_LONG, Data::capacity,
                ByteBufCodecs.STRING_UTF8, Data::voltageName,
                ByteBufCodecs.VAR_LONG, Data::transferRate,
                ByteBufCodecs.VAR_INT, Data::cableCount,
                Data::new);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return (StreamCodec<RegistryFriendlyByteBuf, Data>) (StreamCodec<?, Data>) Data.STREAM_CODEC;
    }

    @Override
    public Data streamData(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CableBlockEntity cable) {
            CableBlockEntity.NetworkEnergyInfo info = cable.getNetworkEnergyInfo();
            return new Data(
                    info.energy(),
                    info.capacity(),
                    cable.getVoltage().getName(),
                    cable.getVoltage().getMaxVoltage(),
                    info.cableCount());
        }
        return null;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    /**
     * 客户端渲染 - 显示导线网络统一能量缓冲和传输速率
     */
    public static class Client implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data data = CableEnergyProvider.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null || data.capacity() <= 0) {
                return;
            }

            float ratio = (float) data.energy() / data.capacity();
            String cableInfo = data.cableCount() > 1 ? " ×" + data.cableCount() : "";
            Component text = Component.literal(
                data.energy() + " / " + data.capacity() + " EU (" + data.voltageName() + cableInfo + ", " + data.transferRate() + " EU/t)");

            ProgressView view = new ProgressView(
                    ProgressView.Part.of(ratio, DARK_RED),
                    text,
                    JadeUI.progressStyle(),
                    BoxStyle.nestedBox());

            tooltip.add(JadeUI.progress(view));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
