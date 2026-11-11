package compat.jade;





import machine.generator.GeneratorBlockEntity;


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


 * 发电机 Jade 兼容 - 显示燃料燃烧进度条


 */


public class GeneratorEnergyProvider implements StreamServerDataProvider<BlockAccessor, GeneratorEnergyProvider.Data> {





    public static final GeneratorEnergyProvider INSTANCE = new GeneratorEnergyProvider();


    public static final Identifier UID = Identifier.fromNamespaceAndPath("iuce", "generator_energy");





    /** 暗红色 (ARGB) */


    private static final int DARK_RED = 0xFFAA0000;





    public record Data(long energy, long capacity, boolean burning, float burnProgress) {


        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(


                ByteBufCodecs.VAR_LONG, Data::energy,


                ByteBufCodecs.VAR_LONG, Data::capacity,


                ByteBufCodecs.BOOL, Data::burning,


                ByteBufCodecs.FLOAT, Data::burnProgress,


                Data::new);


    }


    @Override


    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {


        return (StreamCodec<RegistryFriendlyByteBuf, Data>) (StreamCodec<?, Data>) Data.STREAM_CODEC;


    }


    @Override


    public Data streamData(BlockAccessor accessor) {


        BlockEntity be = accessor.getBlockEntity();


        if (be instanceof GeneratorBlockEntity generator) {


            EUEnergyStorage storage = generator.getEnergyStorage();


            return new Data(


                    storage.getEnergyStored(), 


                    storage.getMaxEnergyStored(), 


                    generator.isBurning(),


                    generator.getBurnProgress());


        }


        return null;


    }


    @Override


    public Identifier getUid() {


        return UID;


    }





    /**


     * 客户端渲染 - 显示燃料燃烧进度条


     */


    public static class Client implements IBlockComponentProvider {





        public static final Client INSTANCE = new Client();





        @Override


        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {


            Data data = GeneratorEnergyProvider.INSTANCE.decodeFromData(accessor).orElse(null);


            if (data == null || data.capacity() <= 0) {


                return;


            }





            // 显示能量条


            float ratio = (float) data.energy() / data.capacity();


            Component text = Component.literal(data.energy() + " / " + data.capacity() + " EU");





            ProgressView progressView = new ProgressView(


                    ProgressView.Part.of(ratio, DARK_RED),


                    text,


                    JadeUI.progressStyle(),


                    BoxStyle.nestedBox());





            tooltip.add(JadeUI.progress(progressView));





            // 如果正在燃烧，显示燃料进度


            if (data.burning()) {


                Component fuelText = Component.translatable("tooltip.iuce.fuel_progress", (int)(data.burnProgress() * 100));


                ProgressView fuelProgress = new ProgressView(


                        ProgressView.Part.of(data.burnProgress(), DARK_RED),


                        fuelText,


                        JadeUI.progressStyle(),


                        BoxStyle.nestedBox());


                tooltip.add(JadeUI.progress(fuelProgress));


            }


        }





        @Override


        public Identifier getUid() {


            return UID;


        }


    }


}


