package regression;

import client.gui.ProcessMachineScreen;
import machine.process.ProcessMachineBlockEntity;
import machine.process.ProcessMachineMenu.Page;
import machine.process.ProcessMachines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.item.ItemResource;
import recipe.ProcessRecipe;
import recipe.cache.ProcessRecipeCache;

import java.util.List;
import java.util.Locale;

/**
 * Development tool: places a few process machines next to the player, opens each one and saves a
 * screenshot of every tab to {@code screenshots/machine_<id>_<tab>.png}, then quits. Run with
 * {@code gradlew runClient -PsmokeTest -PregressionTests -PguideShots=machines -PguideWorld=<save>}.
 */
public final class MachineShots {
    private MachineShots() {}

    private static final List<String> MACHINES = List.of("single_screw_extruder", "cooling_tank", "vulcanizer",
        "smokehouse", "film_winder", "haul_off_cutter", "cnc_machining_center", "polymerization_reactor",
        "distillation_column", "lime_kiln", "fermenter", "additive_reactor", "catalyst_prep_tank", "dehydration_reactor",
        "depolymerization_reactor", "oligomerization_reactor", "vcm_converter", "neutralizer", "acetylene_generator",
        "solvent_recovery_tower", "tar_distiller", "scrubber", "flash_tank", "evaporator", "crystallizer", "vent_stack",
        "carbide_furnace", "coking_oven", "hcl_furnace", "dryer");
    private static final int PER_MACHINE = 100, OPEN = 10, FIRST_PAGE = 40, PER_PAGE = 10;

    private static int ticks, settle = -1, step = -1;
    private static BlockPos placed;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MachineShots::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        ticks++;
        var server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null) {
            if (ticks > 2400) minecraft.stop();
            return;
        }
        if (settle < 0) settle = ticks + 100;
        if (ticks < settle) return;
        step++;
        int machine = step / PER_MACHINE, phase = step % PER_MACHINE;
        if (machine >= MACHINES.size()) {
            minecraft.stop();
            return;
        }
        String id = MACHINES.get(machine);
        // The block goes down first; the menu opens a few ticks later, once the client has the new block entity.
        if (phase == 0) place(minecraft, server, id, machine);
        if (phase == OPEN) open(minecraft, server);
        if (phase < FIRST_PAGE || !(minecraft.screen instanceof ProcessMachineScreen screen)) return;
        List<Page> pages = screen.pages();
        int page = (phase - FIRST_PAGE) / PER_PAGE, inPage = (phase - FIRST_PAGE) % PER_PAGE;
        if (page >= pages.size()) return;
        if (inPage == 0) screen.selectPage(pages.get(page));
        if (inPage == PER_PAGE - 2) {
            String name = "machine_" + id + "_" + pages.get(page).name().toLowerCase(Locale.ROOT) + ".png";
            Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1, message -> { });
            System.out.println("[MachineShots] " + name);
        }
    }

    /** Places the machine in the air above the player, charges it and loads the inputs of its first recipe. */
    private static void place(Minecraft minecraft, net.minecraft.server.MinecraftServer server, String id, int index) {
        var uuid = minecraft.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            var entry = ProcessMachines.get(id);
            if (player == null || entry == null) return;
            var level = player.level();
            // within reach of the player, or the menu closes again at once
            BlockPos pos = player.blockPosition().offset(index % 8 - 4, 4, 0);
            placed = pos;
            level.setBlockAndUpdate(pos, entry.block().get().defaultBlockState());
            if (!(level.getBlockEntity(pos) instanceof ProcessMachineBlockEntity machine)) return;
            if (machine.getEnergyStorage() instanceof energy.EUEnergyStorageImpl storage) {
                storage.setEnergy(storage.getMaxEnergyStored() * 3 / 4);
            }
            var recipes = ProcessRecipeCache.recipes(level, id);
            if (!recipes.isEmpty()) {
                ProcessRecipe recipe = recipes.getFirst().value();
                var handler = machine.getItemHandler();
                int slot = 0;
                for (ProcessRecipe.ItemInput input : recipe.items()) {
                    if (slot >= machine.def().itemsIn()) break;
                    var item = input.ingredient().items().findFirst();
                    if (item.isEmpty()) continue;
                    handler.set(slot++, ItemResource.of(item.get().value()), Math.min(64, input.count() * 8));
                }
                // input tanks get the recipe's fluids, output tanks some of its products, so the gauges show
                var tanks = machine.getTanks();
                int in = machine.getInputTankCount();
                for (int i = 0; i < recipe.fluids().size() && i < in; i++) {
                    var tank = tanks.get(i);
                    recipe.fluids().get(i).ingredient().fluids().forEach(fluid -> {
                        if (tank.isEmpty()) tank.fill(fluid.value(), tank.getCapacity() * 3 / 4);
                    });
                }
                for (int i = 0; i < recipe.fluidResults().size() && in + i < tanks.size(); i++) {
                    var tank = tanks.get(in + i);
                    tank.fill(recipe.fluidResults().get(i).fluid(), tank.getCapacity() / (3 + i));
                }
            }
        });
    }

    private static void open(Minecraft minecraft, net.minecraft.server.MinecraftServer server) {
        var uuid = minecraft.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player == null || placed == null) return;
            if (player.level().getBlockEntity(placed) instanceof ProcessMachineBlockEntity machine) player.openMenu(machine, placed);
        });
    }
}
