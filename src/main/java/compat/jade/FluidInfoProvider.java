package compat.jade;

import fluid.FluidProvider;
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
 * 通用流体 Jade 兼容 - 显示流体液位
 * <p>
 * 适用于所有实现 {@link FluidProvider} 接口的机器方块实体。
 * </p>
 */
public class FluidInfoProvider implements StreamServerDataProvider<BlockAccessor, FluidInfoProvider.Data> {

    public static final FluidInfoProvider INSTANCE = new FluidInfoProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("iuce", "fluid_info");

    /** 蓝色 (ARGB) */
    private static final int WATER_BLUE = 0xFF3366CC;

    public record Data(int fluid, int maxFluid) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::fluid,
                ByteBufCodecs.VAR_INT, Data::maxFluid,
                Data::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return (StreamCodec<RegistryFriendlyByteBuf, Data>) (StreamCodec<?, Data>) Data.STREAM_CODEC;
    }

    @Override
    public Data streamData(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof FluidProvider provider && provider.getMaxStoredFluid() > 0) {
            return new Data(provider.getStoredFluid(), provider.getMaxStoredFluid());
        }
        return null;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    /**
     * 客户端渲染 - 显示流体液位条
     */
    public static class Client implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data data = FluidInfoProvider.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null || data.maxFluid() <= 0) {
                return;
            }

            float ratio = (float) data.fluid() / data.maxFluid();
            int buckets = data.fluid() / 1000;
            int maxBuckets = data.maxFluid() / 1000;
            Component text = Component.literal(buckets + " / " + maxBuckets + " 桶 (" + data.fluid() + " mB)");

            ProgressView progressView = new ProgressView(
                    ProgressView.Part.of(ratio, WATER_BLUE),
                    text,
                    JadeUI.progressStyle(),
                    BoxStyle.nestedBox());

            tooltip.add(JadeUI.progress(progressView));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
