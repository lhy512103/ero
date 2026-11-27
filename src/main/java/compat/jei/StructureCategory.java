package compat.jei;

import client.gui.MachineScreen;
import client.recipe.RecipePage;
import client.guide.GuideStructure;
import client.guide.GuideStructureRenderState;
import client.recipe.RecipePage.Mark;
import client.recipe.RecipePage.Role;
import item.StructureManualItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The structure page of every large machine, in the look of the machine pages: one large dark
 * screen with the machine in it, a layer selector and a formed toggle under it, then every block
 * the machine needs as inputs and a structure manual as the product. Scrolling over the screen
 * zooms, dragging turns it, and clicking a block outlines and names it. Encoding the page in an
 * AE2 pattern encoding terminal gives a processing pattern from the blocks to one manual, for
 * ordering a whole machine as one fake craft.
 */
final class StructureCategory extends AbstractRecipeCategory<StructureCategory.Page> {
    /** One large machine, by guide structure name. */
    record Page(String name, int length) {
        GuideStructure structure() { return length < 0 ? GuideStructure.get(name) : GuideStructure.get(name, length); }
    }

    static final IRecipeType<Page> TYPE = IRecipeType.create(Identifier.parse("iuce:structure"), Page.class);
    static final List<String> NAMES = List.of("ore_plant", "rolling_mill", "twin_screw_extruder", "bio_ethylene_plant",
        "carbide_furnace_plant", "coke_oven_battery", "waste_heat_power_plant");

    private static final int PAD = RecipePage.PAD, WIDTH = 178, VIEW_W = WIDTH - 2 * PAD, VIEW_H = 112;
    private static final int TOP = PAD, BAR_Y = TOP + VIEW_H + 3, BAR_H = 12, PER_ROW = 7;
    private static final int GRID_X = PAD + 2, GRID_Y = BAR_Y + BAR_H + 5;
    private static final int OUT_X = WIDTH - PAD - 2 - 16;
    private static final float PITCH = 30;
    private static final Identifier BTN = Identifier.parse("iuce:jei/button"), BTN_HOVER = Identifier.parse("iuce:jei/button_hover"),
        BTN_ON = Identifier.parse("iuce:jei/button_on"), BTN_OFF = Identifier.parse("iuce:jei/button_off");
    private static final int TEXT = 0xFFE0E6EA, TEXT_OFF = 0xFF6A747C, ON = 0xFF2E6F8E, HOVER = 0xFF3A4752;

    // the controls under the screen: [<] layer [>] [auto] ......... [formed]
    private static final int PREV_X = PAD, BTN_W = 12, LABEL_W = 60;
    private static final int NEXT_X = PREV_X + BTN_W + LABEL_W, AUTO_X = NEXT_X + BTN_W + 3, AUTO_W = 30;
    private static final int FORM_W = 44, FORM_X = WIDTH - PAD - FORM_W;

    private final int rows;

    StructureCategory(IJeiHelpers helpers) {
        this(helpers, rows());
    }

    private StructureCategory(IJeiHelpers helpers, int rows) {
        super(TYPE, Component.translatable("jei.iuce.structure"),
            helpers.getGuiHelper().createDrawableItemStack(new ItemStack(item.ModItems.STRUCTURE_MANUAL.get())),
            WIDTH, GRID_Y + rows * 18 + PAD);
        this.rows = rows;
    }

    static List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        for (String name : NAMES) {
            if (GuideStructure.get(name) == null) continue;
            int[] range = GuideStructure.lengths(name);
            if (range == null) {
                pages.add(new Page(name, -1));
                continue;
            }
            // the default length first, then every other length the machine can be built at
            pages.add(new Page(name, range[2]));
            for (int l = range[0]; l <= range[1]; l += range[3]) if (l != range[2]) pages.add(new Page(name, l));
        }
        return pages;
    }

    private static int rows() {
        int kinds = 1;
        for (Page page : pages()) kinds = Math.max(kinds, materials(page).size());
        return Math.max(1, (kinds + PER_ROW - 1) / PER_ROW);
    }

    /** Every block of the machine, controller included, with its count. */
    private static List<ItemStack> materials(Page page) {
        GuideStructure structure = page.structure();
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

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Page page, IFocusGroup focus) {
        GuideStructure structure = page.structure();
        if (structure == null) return;
        Viewer viewer = new Viewer(structure, page.length());
        builder.addWidget(viewer);
        builder.addGuiEventListener(viewer);
    }

    @Override
    public void draw(Page page, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        RecipePage.panel(g, getWidth(), getHeight());
        List<Mark> marks = new ArrayList<>();
        int kinds = materials(page).size();
        for (int i = 0; i < kinds; i++) marks.add(new Mark(GRID_X + (i % PER_ROW) * 18, GRID_Y + (i / PER_ROW) * 18, Role.INPUT));
        marks.add(new Mark(OUT_X, outY(), Role.OUTPUT));
        RecipePage.slots(g, marks);
        // an arrow from the blocks to the manual
        int ax = GRID_X + PER_ROW * 18 + 2, end = OUT_X - 5, ay = outY() + 7;
        if (end - ax > 6) {
            g.fill(ax, ay, end - 3, ay + 2, MachineScreen.LABEL);
            for (int i = 0; i < 4; i++) g.fill(end - 4 + i, ay - 3 + i, end - 3 + i, ay + 5 - i, MachineScreen.LABEL);
        }
    }

    private static float fit(Vector3f size, int width, int height) {
        float diagonal = (float) Math.sqrt(size.x * size.x + size.z * size.z);
        float tall = size.y + diagonal * 0.5f;
        return Math.min((width - 6) / diagonal, (height - 6) / tall);
    }

    /**
     * The machine screen of one shown page and its controls. JEI builds one per recipe layout, so
     * the chosen layer, zoom, turn and outlined block last while the page is open.
     */
    private static final class Viewer implements IRecipeWidget, IJeiGuiEventListener {
        private final GuideStructure structure;
        /** Layers shown, or 0 for the automatic build-up. */
        private int layers;
        private boolean formed;
        private float zoom = 1;
        /** Turn set by dragging, or NaN while the view still turns by itself. */
        private float yaw = Float.NaN;
        private float pitch = PITCH;
        private boolean dragged;
        private BlockPos picked;
        private float displayedYaw;
        private int displayedLayers;
        private final int length;

        Viewer(GuideStructure structure, int length) {
            this.structure = structure;
            this.length = length;
        }

        private boolean canForm() { return structure.model() != null; }

        private int shownLayers() {
            if (formed) return structure.sizeY;
            if (layers > 0) return layers;
            // the build-up adds one layer a second and holds the finished machine for two
            int cycle = structure.sizeY + 2;
            return Math.min(structure.sizeY, (int) ((System.currentTimeMillis() / 1000L) % cycle) + 1);
        }

        private float currentYaw() {
            return Float.isNaN(yaw) ? 225 + (System.currentTimeMillis() % 24000L) * 0.015f : yaw;
        }

        private float scale() {
            Vector3f[] e = structure.extent(formed);
            return fit(new Vector3f(e[1]).sub(e[0]), VIEW_W - 2, VIEW_H - 2) * zoom;
        }

        private Vector3f centre() {
            Vector3f[] e = structure.extent(formed);
            return new Vector3f(e[0]).add(e[1]).mul(0.5f);
        }

        private static boolean inView(double x, double y) {
            return RecipePage.over(x, y, PAD, TOP, VIEW_W, VIEW_H);
        }

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(PAD, TOP, VIEW_W, BAR_Y + BAR_H - TOP);
        }

        @Override
        public void drawWidget(GuiGraphicsExtractor g, double mouseX, double mouseY) {
            MachineScreen.inset(g, PAD, TOP, VIEW_W, VIEW_H, MachineScreen.SCREEN);
            g.fillGradient(PAD + 1, TOP + 1, PAD + VIEW_W - 1, TOP + VIEW_H - 1, 0xFF34424D, 0xFF161B20);
            int shown = shownLayers();
            displayedLayers = shown;
            displayedYaw = currentYaw();
            float spin = formed ? (System.currentTimeMillis() % 36000L) * 0.06f : 0;
            // picture-in-picture views are placed in screen space, so the page's offset is added here
            var pose = g.pose();
            int ox = Math.round(pose.m20()), oy = Math.round(pose.m21());
            g.submitPictureInPictureRenderState(new GuideStructureRenderState(structure, shown, formed, centre(),
                displayedYaw, pitch, spin, ox + PAD + 1, oy + TOP + 1, ox + PAD + VIEW_W - 1, oy + TOP + VIEW_H - 1,
                scale(), !formed && picked != null && picked.getY() < shown ? picked : null, g.peekScissorStack()));

            Font font = Minecraft.getInstance().font;
            if (picked != null && !formed) {
                var state = structure.stateAt(picked);
                if (state != null) g.text(font, state.getBlock().getName(), PAD + 4, TOP + VIEW_H - 11, 0xFFFFD84A, true);
            }
            if (length > 0) {
                Component len = Component.translatable("jei.iuce.structure.length", length);
                g.text(font, len, PAD + VIEW_W - 4 - font.width(len), TOP + 4, 0xFFFFD84A, true);
            }

            button(g, font, PREV_X, BTN_W, Component.literal("<"), false, mouseX, mouseY, !formed);
            Component label = Component.translatable("jei.iuce.structure.layer", shown, structure.sizeY);
            g.text(font, label, PREV_X + BTN_W + (LABEL_W - font.width(label)) / 2, BAR_Y + 2, MachineScreen.LABEL, false);
            button(g, font, NEXT_X, BTN_W, Component.literal(">"), false, mouseX, mouseY, !formed);
            button(g, font, AUTO_X, AUTO_W, Component.translatable("jei.iuce.structure.auto"), layers == 0 && !formed,
                mouseX, mouseY, !formed);
            if (canForm()) {
                button(g, font, FORM_X, FORM_W, Component.translatable("jei.iuce.structure.formed"), formed,
                    mouseX, mouseY, true);
            }
        }

        private static void button(GuiGraphicsExtractor g, Font font, int x, int w, Component text, boolean on,
                                   double mx, double my, boolean enabled) {
            boolean hover = enabled && RecipePage.over(mx, my, x, BAR_Y, w, BAR_H);
            Identifier sprite = !enabled ? BTN_OFF : on ? BTN_ON : hover ? BTN_HOVER : BTN;
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, x, BAR_Y, w, BAR_H);
            int dy = on ? 3 : 2;
            g.text(font, text, x + (w - font.width(text) + 1) / 2, BAR_Y + dy, !enabled ? 0xFF7A7F86 : on ? 0xFFFFFFFF : MachineScreen.LABEL, on);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
            if (!inView(mouseX, mouseY)) return;
            tooltip.add(Component.translatable("jei.iuce.structure.size", structure.sizeX, structure.sizeY, structure.sizeZ)
                .withStyle(ChatFormatting.GRAY));
            int total = 0;
            for (ItemStack stack : structure.materials(structure.sizeY)) total += stack.getCount();
            tooltip.add(Component.translatable("jei.iuce.structure.blocks", total).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("jei.iuce.structure.controls").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("jei.iuce.structure.hint").withStyle(ChatFormatting.AQUA));
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
            if (!inView(mouseX, mouseY) || dy == 0) return false;
            zoom = Math.max(0.5f, Math.min(6f, zoom * (dy > 0 ? 1.15f : 1 / 1.15f)));
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
            if (button != 0) return false;
            if (Float.isNaN(yaw)) yaw = currentYaw();
            yaw += (float) dx * 1.5f;
            pitch = Math.max(-89f, Math.min(89f, pitch - (float) dy * 1.5f));
            if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) dragged = true;
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            dragged = false;
            if (canForm() && RecipePage.over(mouseX, mouseY, FORM_X, BAR_Y, FORM_W, BAR_H)) {
                formed = !formed;
                return true;
            }
            if (!formed) {
                int now = layers == 0 ? shownLayers() : layers;
                if (RecipePage.over(mouseX, mouseY, PREV_X, BAR_Y, BTN_W, BAR_H)) {
                    layers = Math.max(1, now - 1);
                    return true;
                }
                if (RecipePage.over(mouseX, mouseY, NEXT_X, BAR_Y, BTN_W, BAR_H)) {
                    layers = Math.min(structure.sizeY, now + 1);
                    return true;
                }
                if (RecipePage.over(mouseX, mouseY, AUTO_X, BAR_Y, AUTO_W, BAR_H)) {
                    layers = layers == 0 ? structure.sizeY : 0;
                    return true;
                }
            }
            return inView(mouseX, mouseY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button != 0 || dragged || formed || !inView(mouseX, mouseY)) return false;
            BlockPos hit = pick(mouseX, mouseY);
            picked = hit != null && hit.equals(picked) ? null : hit;
            return true;
        }

        private Matrix4f view() {
            Vector3f c = centre();
            return new Matrix4f().rotateZ((float) Math.PI)
                .rotateX((float) Math.toRadians(-pitch)).rotateY((float) Math.toRadians(displayedYaw))
                .translate(-c.x, -c.y, -c.z);
        }

        /**
         * The shown block the mouse ray hits first: the ray through the mouse is taken back into
         * layout space and tested against each block's box, once per click and never per frame.
         */
        private BlockPos pick(double mouseX, double mouseY) {
            float s = scale();
            Matrix4f inv = view().invert();
            float vx = (float) (mouseX - (PAD + VIEW_W / 2f)) / s, vy = (float) (mouseY - (TOP + VIEW_H / 2f)) / s;
            // PictureInPictureRenderer negates the pose Z scale, so smaller view Z is nearer.
            Vector3f o = inv.transformPosition(vx, vy, -1000f, new Vector3f());
            Vector3f d = inv.transformPosition(vx, vy, 1000f, new Vector3f()).sub(o);
            int shown = displayedLayers;
            BlockPos best = null;
            float bestT = Float.MAX_VALUE;
            for (GuideStructure.Cell cell : structure.cells) {
                BlockPos p = cell.pos();
                if (p.getY() >= shown) continue;
                float t = hit(o, d, p.getX(), p.getY(), p.getZ());
                if (t >= 0 && t < bestT) {
                    bestT = t;
                    best = p;
                }
            }
            return best;
        }

        /** Entry distance of the ray o + t d into the unit box at x, y, z, or -1 for a miss. */
        private static float hit(Vector3f o, Vector3f d, int x, int y, int z) {
            float t0 = 0, t1 = Float.MAX_VALUE;
            float[] os = {o.x, o.y, o.z}, ds = {d.x, d.y, d.z}, lo = {x, y, z};
            for (int i = 0; i < 3; i++) {
                if (Math.abs(ds[i]) < 1e-7f) {
                    if (os[i] < lo[i] || os[i] > lo[i] + 1) return -1;
                    continue;
                }
                float a = (lo[i] - os[i]) / ds[i], b = (lo[i] + 1 - os[i]) / ds[i];
                if (a > b) { float k = a; a = b; b = k; }
                t0 = Math.max(t0, a);
                t1 = Math.min(t1, b);
                if (t0 > t1) return -1;
            }
            return t0;
        }
    }
}
