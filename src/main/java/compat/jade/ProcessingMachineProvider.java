package compat.jade;

import blockentity.ProcessingDisplay;
import energy.EUEnergyStorage;
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
 * 处理机器 Jade 兼容 - 显示能量条和处理进度
 * <p>
 * 适用于所有实现 {@link ProcessingDisplay} 接口的机器方块实体。
 * </p>
 */
public class ProcessingMachineProvider implements StreamServerDataProvider<BlockAccessor, ProcessingMachineProvider.Data> {

    public static final ProcessingMachineProvider INSTANCE = new ProcessingMachineProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("iuce", "processing_machine");

    /** 暗红色 (ARGB) */
    private static final int DARK_RED = 0xFFAA0000;

    public record Data(long energy, long capacity, String voltageName, boolean processing, float progress, String progressLabel) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Data::energy,
                ByteBufCodecs.VAR_LONG, Data::capacity,
                ByteBufCodecs.STRING_UTF8, Data::voltageName,
                ByteBufCodecs.BOOL, Data::processing,
                ByteBufCodecs.FLOAT, Data::progress,
                ByteBufCodecs.STRING_UTF8, Data::progressLabel,
                Data::new);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return (StreamCodec<RegistryFriendlyByteBuf, Data>) (StreamCodec<?, Data>) Data.STREAM_CODEC;
    }

    @Override
    public Data streamData(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof ProcessingDisplay display) {
            EUEnergyStorage storage = null;
            String voltageName = "";

            if (be instanceof blockentity.EnergyDisplay energyDisplay) {
                storage = energyDisplay.getEnergyStorage();
                voltageName = storage.getVoltage().getName();
            }

            if (storage != null) {
                return new Data(
                        storage.getEnergyStored(),
                        storage.getMaxEnergyStored(),
                        voltageName,
                        display.isProcessing(),
                        display.getProcessingProgress(),
                        display.getProgressLabel());
            }
        }
        return null;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    /**
     * 客户端渲染 - 显示能量条和处理进度（文本形式）
     */
    public static class Client implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data data = ProcessingMachineProvider.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null || data.capacity() <= 0) {
                return;
            }

            // 显示能量条
            float ratio = (float) data.energy() / data.capacity();
            Component text = Component.literal(data.energy() + " / " + data.capacity() + " EU (" + data.voltageName() + ")");

            ProgressView energyView = new ProgressView(
                    ProgressView.Part.of(Math.clamp(ratio, 0f, 1f), DARK_RED),
                    text,
                    JadeUI.progressStyle(),
                    BoxStyle.nestedBox());

            tooltip.add(JadeUI.progress(energyView));

            // 如果正在处理且有进度标签，显示文本进度
            if (data.processing() && !data.progressLabel().isEmpty()) {
                Component progressText = Component.translatable(data.progressLabel()).append(": " + (int)(data.progress() * 100) + "%");
                tooltip.add(progressText);
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
