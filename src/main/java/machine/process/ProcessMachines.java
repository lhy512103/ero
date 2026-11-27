package machine.process;

import block.ModBlockItems;
import block.ModBlocks;
import blockentity.ModBlockEntities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import init.ModMenus;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers every process machine listed in {@code iuce/machines/_index.json} (and, in regression
 * runs, {@code _index_regression.json}). Each gets a block and a block item; all of them share one
 * block entity type and one menu type. Must run before the registers are attached to the mod bus.
 */
public final class ProcessMachines {
    private ProcessMachines() {}

    public record Entry(ProcessMachineDef def, DeferredBlock<ProcessMachineBlock> block, DeferredItem<BlockItem> item) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    public static Supplier<BlockEntityType<ProcessMachineBlockEntity>> BLOCK_ENTITY;
    public static Supplier<MenuType<ProcessMachineMenu>> MENU;

    public static Collection<Entry> all() { return Collections.unmodifiableCollection(ENTRIES.values()); }

    public static Entry get(String id) { return ENTRIES.get(id); }

    public static void bootstrap() {
        if (BLOCK_ENTITY != null) return;
        loadIndex("_index.json", true);
        loadIndex("_index_regression.json", false);
        BLOCK_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register("process_machine", () -> new BlockEntityType<>(
            ProcessMachineBlockEntity::new, false,
            ENTRIES.values().stream().map(entry -> entry.block().get()).toArray(net.minecraft.world.level.block.Block[]::new)));
        MENU = ModMenus.MENUS.register("process_machine", () -> IMenuTypeExtension.create(
            (IContainerFactory<ProcessMachineMenu>) (id, inv, data) -> new ProcessMachineMenu(id, inv,
                (ProcessMachineBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))));
    }

    private static void loadIndex(String name, boolean required) {
        JsonElement index = read(name);
        if (index == null) {
            if (required) throw new IllegalStateException("Missing iuce/machines/" + name);
            return;
        }
        JsonArray ids = index.getAsJsonObject().getAsJsonArray("machines");
        for (JsonElement id : ids) {
            JsonElement json = read(id.getAsString() + ".json");
            if (json == null) throw new IllegalStateException("Missing machine definition iuce/machines/" + id.getAsString() + ".json");
            ProcessMachineDef def = ProcessMachineDef.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new IllegalStateException("Bad machine definition " + id.getAsString() + ": " + error));
            register(def);
        }
    }

    private static void register(ProcessMachineDef def) {
        if (ENTRIES.containsKey(def.id())) throw new IllegalStateException("Duplicate process machine " + def.id());
        DeferredBlock<ProcessMachineBlock> block = ModBlocks.BLOCKS.registerBlock(def.id(),
            properties -> new ProcessMachineBlock(properties, def),
            () -> BlockBehaviour.Properties.of()
                .destroyTime(3.5f)
                .explosionResistance(6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        DeferredItem<BlockItem> item = ModBlockItems.registerBlockItem(block);
        ENTRIES.put(def.id(), new Entry(def, block, item));
    }

    private static JsonElement read(String name) {
        try (InputStream stream = ProcessMachines.class.getResourceAsStream("/iuce/machines/" + name)) {
            if (stream == null) return null;
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read iuce/machines/" + name, e);
        }
    }
}
