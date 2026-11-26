package compat.jei;

import client.gui.MachineScreen;
import client.guide.GuideStructure;
import client.guide.GuideStructureRenderState;
import compat.jei.JeiPage.Mark;
import compat.jei.JeiPage.Role;
import item.StructureManualItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The structure page of every large machine, in the look of the machine pages: two dark screens
 * side by side, the left one building the machine up layer by layer and the right one showing it
 * assembled, then every block the machine needs as inputs and a structure manual as the product.
 * Encoding the page in an AE2 pattern encoding terminal therefore gives a processing pattern from
 * the blocks to one manual, for ordering a whole machine as one fake craft.
 */
final class StructureCategory extends AbstractRecipeCategory<StructureCategory.Page> {
    /** One large machine, by guide structure name. */
    record Page(String name) {}

    static final IRecipeType<Page> TYPE = IRecipeType.create(Identifier.parse("iuce:structure"), Page.class);
    static final List<String> NAMES = List.of("ore_plant", "rolling_mill", "twin_screw_extruder", "bio_ethylene_plant",
        "carbide_furnace_plant", "coke_oven_battery", "waste_heat_power_plant");

    private static final int PAD = JeiPage.PAD, WIDTH = 178, GAP = 4, VIEW_W = (WIDTH - 2 * PAD - GAP) / 2, VIEW_H = 84;
    private static final int TOP = PAD, LABEL_H = 11, PER_ROW = 7;
    private static final int GRID_X = PAD + 2, GRID_Y = TOP + VIEW_H + LABEL_H + 6;
    private static final int OUT_X = WIDTH - PAD - 2 - 16;

    private final int rows;

    StructureCategory(IJeiHelpers helpers) {
        this(helpers, rows());
    }

    private StructureCategory(IJeiHelpers helpers, int rows) {
        super(TYPE, Component.translatable("jei.iuce.structure"),
            helpers.getGuiHelper().createDrawableItemStack(new ItemStack(item.ModItems.STRUCTURE_MANUAL.get())),
            WIDTH, GRID_Y + rows * 18 - 2 + 2 + PAD);
        this.rows = rows;
    }

    static List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        for (String name : NAMES) if (GuideStructure.get(name) != null) pages.add(new Page(name));
        return pages;
    }

    private static int rows() {
        int kinds = 1;
        for (Page page : pages()) kinds = Math.max(kinds, materials(page).size());
        return Math.max(1, (kinds + PER_ROW - 1) / PER_ROW);
    }

    /** Every block of the machine, controller included, with its count. */
    private static List<ItemStack> materials(Page page) {
        GuideStructure structure = GuideStructure.get(page.name());
        return structure == null ? List.of() : structure.materials(structure.sizeY);
    }

    private int outY() { return GRID_Y + (rows * 18 - 18) / 2; }

    @Override
    public boolean needsRecipeBorder() {
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focus) {
        List<ItemStack> materials = materials(page);
        for (int i = 0; i < materials.size(); i++) {
            builder.addInputSlot(GRID_X + (i % PER_ROW) * 18, GRID_Y + (i / PER_ROW) * 18).add(materials.get(i));
        }
        builder.addOutputSlot(OUT_X, outY()).add(StructureManualItem.of(page.name()))
            .addRichTooltipCallback((view, tooltip) ->
                tooltip.add(Component.translatable("jei.iuce.structure.manual").withStyle(ChatFormatting.AQUA)));
    }

    private static float fit(Vector3f size, int width, int height) {
        float diagonal = (float) Math.sqrt(size.x * size.x + size.z * size.z);
        float tall = size.y + diagonal * 0.5f;
        return Math.min((width - 6) / diagonal, (height - 6) / tall);
    }

    @Override
    public void draw(Page page, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        JeiPage.panel(g, getWidth(), getHeight());
        GuideStructure structure = GuideStructure.get(page.name());
        if (structure == null) return;
        long now = System.currentTimeMillis();
        float yaw = 225 + (now % 24000L) * 0.015f;
        // the build-up screen adds one layer a second and holds the finished machine for two
        int cycle = structure.sizeY + 2;
        int layers = Math.min(structure.sizeY, (int) ((now / 1000L) % cycle) + 1);
        boolean formed = structure.model() != null;
        int rightX = PAD + VIEW_W + GAP;
        view(g, structure, PAD, layers, false, yaw);
        view(g, structure, rightX, structure.sizeY, formed, yaw);

        var font = Minecraft.getInstance().font;
        Component left = Component.translatable("jei.iuce.structure.layers", layers, structure.sizeY);
        Component right = Component.translatable(formed ? "jei.iuce.structure.formed" : "jei.iuce.structure.complete");
        int ly = TOP + VIEW_H + 3;
        g.text(font, left, PAD + (VIEW_W - font.width(left)) / 2, ly, MachineScreen.LABEL, false);
        g.text(font, right, rightX + (VIEW_W - font.width(right)) / 2, ly, MachineScreen.LABEL, false);

        List<Mark> marks = new ArrayList<>();
        int kinds = materials(page).size();
        for (int i = 0; i < kinds; i++) marks.add(new Mark(GRID_X + (i % PER_ROW) * 18, GRID_Y + (i / PER_ROW) * 18, Role.INPUT));
        marks.add(new Mark(OUT_X, outY(), Role.OUTPUT));
        JeiPage.slots(g, marks);
        // an arrow from the blocks to the manual
        int ax = GRID_X + PER_ROW * 18 + 2, end = OUT_X - 5, ay = outY() + 7;
        if (end - ax > 6) {
            g.fill(ax, ay, end - 3, ay + 2, MachineScreen.LABEL);
            for (int i = 0; i < 4; i++) g.fill(end - 4 + i, ay - 3 + i, end - 3 + i, ay + 5 - i, MachineScreen.LABEL);
        }
    }

    /** One dark machine screen with the structure turning in it. */
    private static void view(GuiGraphicsExtractor g, GuideStructure structure, int x, int layers, boolean formed, float yaw) {
        MachineScreen.inset(g, x, TOP, VIEW_W, VIEW_H, MachineScreen.SCREEN);
        g.fillGradient(x + 1, TOP + 1, x + VIEW_W - 1, TOP + VIEW_H - 1, 0xFF34424D, 0xFF161B20);
        Vector3f[] extent = structure.extent(formed);
        Vector3f size = new Vector3f(extent[1]).sub(extent[0]);
        Vector3f centre = new Vector3f(extent[0]).add(extent[1]).mul(0.5f);
        float spin = formed ? (System.currentTimeMillis() % 36000L) * 0.06f : 0;
        // picture-in-picture views are placed in screen space, so the page's offset is added here
        var pose = g.pose();
        int ox = Math.round(pose.m20()), oy = Math.round(pose.m21());
        g.submitPictureInPictureRenderState(new GuideStructureRenderState(structure, layers, formed, centre, yaw, 30, spin,
            ox + x + 1, oy + TOP + 1, ox + x + VIEW_W - 1, oy + TOP + VIEW_H - 1, fit(size, VIEW_W - 2, VIEW_H - 2),
            g.peekScissorStack()));
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Page page, IRecipeSlotsView slots, double mouseX, double mouseY) {
        GuideStructure structure = GuideStructure.get(page.name());
        if (structure == null || !JeiPage.over(mouseX, mouseY, PAD, TOP, VIEW_W * 2 + GAP, VIEW_H)) return;
        tooltip.add(Component.translatable("jei.iuce.structure.size", structure.sizeX, structure.sizeY, structure.sizeZ)
            .withStyle(ChatFormatting.GRAY));
        int total = 0;
        for (ItemStack stack : materials(page)) total += stack.getCount();
        tooltip.add(Component.translatable("jei.iuce.structure.blocks", total).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.iuce.structure.hint").withStyle(ChatFormatting.AQUA));
    }
}
