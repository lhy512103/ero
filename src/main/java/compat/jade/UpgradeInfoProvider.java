package compat.jade;

import blockentity.MachineBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 升级插件 Jade 兼容 - 显示机器安装的升级插件数量
 */
public class UpgradeInfoProvider implements StreamServerDataProvider<BlockAccessor, UpgradeInfoProvider.Data> {

    public static final UpgradeInfoProvider INSTANCE = new UpgradeInfoProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("iuce", "upgrade_info");

    /** 升级插件信息，用逗号分隔的 "物品ID=数量" 字符串 */
    public record Data(String upgradeInfo) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
                .map(Data::new, Data::upgradeInfo);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return (StreamCodec<RegistryFriendlyByteBuf, Data>) (StreamCodec<?, Data>) Data.STREAM_CODEC;
    }

    @Override
    public Data streamData(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof MachineBlockEntity machine)) {
            return null;
        }

        ItemStacksResourceHandler itemHandler = machine.getItemHandler();
        if (itemHandler == null) {
            return null;
        }

        int upgradeSlotCount = machine.getUpgradeSlotCount();
        int upgradeSlotStart = machine.getUpgradeSlotStart();
        if (upgradeSlotCount <= 0) {
            return null;
        }

        Map<String, Integer> upgradeCounts = new HashMap<>();

        for (int i = 0; i < upgradeSlotCount; i++) {
            int slotIndex = upgradeSlotStart + i;
            ItemResource resource = itemHandler.getResource(slotIndex);
            int amount = itemHandler.getAmountAsInt(slotIndex);

            if (!resource.isEmpty() && amount > 0) {
                String itemId = BuiltInRegistries.ITEM.getKey(resource.getItem()).toString();
                upgradeCounts.merge(itemId, amount, Integer::sum);
            }
        }

        if (upgradeCounts.isEmpty()) {
            return null;
        }

        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : upgradeCounts.entrySet()) {
            entries.add(entry.getKey() + "=" + entry.getValue());
        }
        return new Data(String.join(",", entries));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    /**
     * 客户端渲染 - 显示升级插件信息
     */
    public static class Client implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data data = UpgradeInfoProvider.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null || data.upgradeInfo().isEmpty()) {
                return;
            }

            String[] entries = data.upgradeInfo().split(",");
            for (String entry : entries) {
                int eqIdx = entry.lastIndexOf('=');
                if (eqIdx > 0) {
                    String itemId = entry.substring(0, eqIdx);
                    int count = Integer.parseInt(entry.substring(eqIdx + 1));

                    var holder = BuiltInRegistries.ITEM.get(Identifier.parse(itemId));
                    if (holder.isPresent()) {
                        Item item = holder.get().value();
                        ItemStack stack = new ItemStack(item, count);
                        int lineIdx = tooltip.size();
                        tooltip.add(JadeUI.item(stack));
                        tooltip.append(lineIdx, JadeUI.text(Component.literal("")
                                .append(item.getName(stack))
                                .append(Component.literal(" x" + count))));
                    }
                }
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}