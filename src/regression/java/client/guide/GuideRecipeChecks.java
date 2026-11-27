package client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import recipe.ProcessRecipe;

import java.util.List;

/** Client regression for scaled recipe hitboxes, fluid lookup and JEI-free history navigation. */
public final class GuideRecipeChecks {
    private static int ticks, step;
    private static String fluidLink;
    private static GuideScreen screen;

    private GuideRecipeChecks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuideRecipeChecks::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        if (minecraft.level == null || minecraft.player == null) return;
        if (++ticks < 100 || ticks % 12 != 0) return;
        switch (step++) {
            case 0 -> {
                require(GuideRecipes.ofMachine("vent_stack").stream().anyMatch(view ->
                    view.holder().value() instanceof ProcessRecipe recipe
                        && recipe.results().isEmpty() && recipe.fluidResults().isEmpty()), "Outputless recipe missing");
                screen = new GuideScreen(null, "plastic/depolymerization_reactor");
                minecraft.setScreen(screen);
                screen.scrollTo(180);
            }
            case 1 -> {
                Ctx.Zone fluid = zones().stream().filter(zone -> zone.link() != null && zone.link().startsWith("fluid:"))
                    .filter(zone -> !GuideRecipes.forFluid(BuiltInRegistries.FLUID.getValue(
                        Identifier.parse(zone.link().substring(6))), false).isEmpty()).findFirst().orElseThrow();
                fluidLink = fluid.link();
                click(fluid, 0);
                require(page().id.startsWith("recipe/FLUID_PRODUCES/"), "Fluid recipe click missed");
            }
            case 2 -> {
                String id = page().id;
                screen.resize(screen.width, screen.height);
                require(page().id.equals(id), "Recipe page lost on resize");
            }
            case 3 -> {
                require(zones().stream().anyMatch(zone -> fluidLink.equals(zone.link())), "Fluid recipe has no tank");
                Screenshot.grab(minecraft.gameDirectory, "guide_fluid_recipes.png", minecraft.getMainRenderTarget(), 1, message -> {});
                screen.mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(3, 0)), false);
                require(page().id.equals("plastic/depolymerization_reactor"), "Back navigation lost guide page");
                screen.scrollTo(180);
            }
            case 4 -> {
                click(zones().stream().filter(zone -> fluidLink.equals(zone.link())).findFirst().orElseThrow(), 1);
                require(page().id.startsWith("recipe/FLUID_USES/"), "Fluid uses click missed");
            }
            case 5 -> {
                require(zones().stream().anyMatch(zone -> fluidLink.equals(zone.link())), "Fluid uses has no tank");
                screen.mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(3, 0)), false);
                screen.mouseClicked(new MouseButtonEvent(0, 0, new MouseButtonInfo(4, 0)), false);
                require(page().id.startsWith("recipe/FLUID_USES/"), "Forward navigation lost fluid query");
                screen = new GuideScreen(null, "machines/macerator");
                minecraft.setScreen(screen);
                screen.scrollTo(180);
            }
            case 6 -> {
                Ctx.Zone input = zones().stream().filter(zone -> !zone.stack().isEmpty()
                    && !GuideRecipes.using(zone.stack().getItem()).isEmpty()).findFirst().orElseThrow();
                click(input, 1);
                if (ModList.get().isLoaded("jei")) require(minecraft.screen != screen, "JEI item lookup did not open");
                else require(page().id.startsWith("recipe/USES/"), "Native item uses did not open");
            }
            case 7 -> {
                if (!ModList.get().isLoaded("jei")) {
                    Ctx.Zone item = zones().stream().filter(zone -> !zone.stack().isEmpty()
                        && GuideBook.get().pageFor(zone.stack().getItem()) == null
                        && !GuideRecipes.producing(zone.stack().getItem()).isEmpty()).findFirst().orElseThrow();
                    click(item, 0);
                    require(page().id.startsWith("recipe/PRODUCES/"), "Native item recipes did not open");
                }
            }
            case 8 -> {
                Screenshot.grab(minecraft.gameDirectory, "guide_item_recipes.png", minecraft.getMainRenderTarget(), 1, message -> {});
                System.out.println("[GuideRecipeChecks] PASS fluid recipes/uses, scaled clicks, resize, history, item lookup, outputless index");
            }
            case 10 -> minecraft.stop();
            default -> {}
        }
    }

    private static void click(Ctx.Zone zone, int button) {
        require(screen.mouseClicked(new MouseButtonEvent(zone.x() + zone.w() / 2.0, zone.y() + zone.h() / 2.0,
            new MouseButtonInfo(button, 0)), false), "Ingredient click unhandled");
    }

    private static GuideBook.Page page() {
        return (GuideBook.Page) field("page");
    }

    @SuppressWarnings("unchecked")
    private static List<Ctx.Zone> zones() {
        return (List<Ctx.Zone>) field("zones");
    }

    private static Object field(String name) {
        try {
            var field = GuideScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("[GuideRecipeChecks] " + message);
    }
}
