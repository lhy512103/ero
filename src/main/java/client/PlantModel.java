package client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Baked mesh of a formed large machine, exported from Blockbench by {@code tools/export_plant_model.py}
 * into {@code models/multiblock/<name>.json} with its atlas in {@code textures/entity/<name>.png}.
 * Coordinates are in blocks with the controller's block at the origin and the machine running
 * towards +z. Static, emissive and translucent boxes are kept apart from the spinning groups, each
 * of which turns about its own pivot, so rendering emits quads without per-frame allocations.
 */
public final class PlantModel {
    public static final String ORE_PLANT = "ore_processing_plant", ROLLING_MILL = "rolling_mill";

    private static final Map<String, Optional<PlantModel>> CACHE = new HashMap<>();

    public record Quad(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float[] normals,
        float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3
    ) {
        /** One normal for all four corners. */
        Quad(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
             float nx, float ny, float nz, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
            this(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, new float[]{nx, ny, nz, nx, ny, nz, nx, ny, nz, nx, ny, nz},
                u0, v0, u1, v1, u2, v2, u3, v3);
        }

        public void emit(PoseStack.Pose pose, VertexConsumer consumer, int light) {
            float[] n = normals;
            consumer.addVertex(pose, x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n[0], n[1], n[2]);
            consumer.addVertex(pose, x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n[3], n[4], n[5]);
            consumer.addVertex(pose, x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n[6], n[7], n[8]);
            consumer.addVertex(pose, x3, y3, z3).setColor(255, 255, 255, 255).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n[9], n[10], n[11]);
        }
    }

    /** A group that turns while the machine works, {@code speed} times the base spin about {@code axis}. */
    public record Spinner(List<Quad> quads, Vector3f pivot, Axis axis, float speed) {}

    /** Boxes placed together: lit normally, full-bright while working, see-through, and spinning. */
    public record Section(List<Quad> solid, List<Quad> glow, List<Quad> translucent, List<Spinner> spinners) {}

    /**
     * Where the sections of a stretchable machine go. {@code head} stays put and {@code tail} moves back
     * by the extra length. Each block from {@code bayFirst} to {@code bayLast} blocks before the end
     * gets a {@code stand} if it is a stand depth (the first, every {@code standEvery} after it, and
     * {@code standLast} before the end) and a {@code bay} otherwise. Sections are modelled at the
     * depth of their first copy; {@code standFirst} must not come before {@code bayFirst}.
     */
    public record Stretch(int baseLength, int bayFirst, int bayLast, int standFirst, int standEvery, int standLast) {
        static final Stretch NONE = new Stretch(0, 0, 0, 0, 0, 0);

        public boolean standAt(int depth, int length) {
            int last = length - 1 - standLast;
            return depth == standFirst || depth == last || (depth > standFirst && depth < last && (depth - standFirst) % standEvery == 0);
        }
    }

    public final Identifier texture;
    public final Map<String, Section> sections;
    public final Stretch stretch;
    /** Corners of everything the model draws at its base length, in blocks. */
    public final Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);

    private PlantModel(Identifier texture, Map<String, Section> sections, Stretch stretch) {
        this.texture = texture;
        this.sections = sections;
        this.stretch = stretch;
        for (Section section : sections.values()) {
            for (List<Quad> quads : List.of(section.solid(), section.glow(), section.translucent())) {
                for (Quad quad : quads) extend(quad, 0, 0, 0);
            }
            for (Spinner spinner : section.spinners()) {
                for (Quad quad : spinner.quads()) extend(quad, spinner.pivot().x, spinner.pivot().y, spinner.pivot().z);
            }
        }
    }

    private void extend(Quad quad, float dx, float dy, float dz) {
        float[] corners = {quad.x0(), quad.y0(), quad.z0(), quad.x1(), quad.y1(), quad.z1(),
            quad.x2(), quad.y2(), quad.z2(), quad.x3(), quad.y3(), quad.z3()};
        for (int i = 0; i < 12; i += 3) {
            Vector3f corner = new Vector3f(corners[i] + dx, corners[i + 1] + dy, corners[i + 2] + dz);
            min.min(corner);
            max.max(corner);
        }
    }

    public static void invalidate() {
        CACHE.clear();
    }

    @Nullable
    public static PlantModel get(String name) {
        return CACHE.computeIfAbsent(name, PlantModel::load).orElse(null);
    }

    private static Optional<PlantModel> load(String name) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
            .getResource(Identifier.fromNamespaceAndPath("iuce", "models/multiblock/" + name + ".json"));
        if (resource.isEmpty()) return Optional.empty();
        try (BufferedReader reader = resource.get().openAsReader()) {
            JsonObject root = GsonHelper.parse(reader);
            Map<String, JsonObject> spinDefs = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(root, "spinners", new JsonObject()).entrySet()) {
                spinDefs.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
            Map<String, Map<String, List<Quad>>> parts = new LinkedHashMap<>();
            Map<String, String> spinnerSection = new HashMap<>();

            for (JsonElement elem : GsonHelper.getAsJsonArray(root, "boxes")) {
                JsonObject box = elem.getAsJsonObject();
                String part = GsonHelper.getAsString(box, "part");
                String section = GsonHelper.getAsString(box, "section", "head");
                Vector3f from = vec3(box.getAsJsonArray("from"));
                Vector3f to = vec3(box.getAsJsonArray("to"));
                Quaternionf rot = null;
                Vector3f origin = null;
                if (box.has("rot")) {
                    JsonArray r = box.getAsJsonArray("rot");
                    rot = new Quaternionf().rotationXYZ(
                        (float) Math.toRadians(r.get(0).getAsFloat()),
                        (float) Math.toRadians(r.get(1).getAsFloat()),
                        (float) Math.toRadians(r.get(2).getAsFloat())
                    );
                    origin = vec3(box.getAsJsonArray("origin"));
                }
                JsonObject spin = spinDefs.get(part);
                if (spin != null) spinnerSection.put(part, section);
                Vector3f groupPivot = spin == null ? null : vec3(spin.getAsJsonArray("pivot"));
                List<Quad> target = parts.computeIfAbsent(section, key -> new LinkedHashMap<>())
                    .computeIfAbsent(part, key -> new ArrayList<>());
                for (Map.Entry<String, JsonElement> f : box.getAsJsonObject("faces").entrySet()) {
                    JsonArray uv = f.getValue().getAsJsonArray();
                    target.add(buildQuad(
                        f.getKey(), from, to, rot, origin, groupPivot,
                        uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                        uv.get(2).getAsFloat(), uv.get(3).getAsFloat()
                    ));
                }
            }
            // Free-form faces (round shells, elbows, cones): four corners in order, each x, y, z, u, v, nx, ny, nz.
            for (JsonElement elem : GsonHelper.getAsJsonArray(root, "quads", new JsonArray())) {
                JsonObject face = elem.getAsJsonObject();
                String part = GsonHelper.getAsString(face, "part");
                String section = GsonHelper.getAsString(face, "section", "head");
                JsonObject spin = spinDefs.get(part);
                if (spin != null) spinnerSection.put(part, section);
                Vector3f pivot = spin == null ? new Vector3f() : vec3(spin.getAsJsonArray("pivot"));
                JsonArray v = face.getAsJsonArray("v");
                float[] c = new float[32];
                for (int i = 0; i < 32; i++) c[i] = v.get(i).getAsFloat();
                float[] normals = new float[12];
                for (int i = 0; i < 4; i++) System.arraycopy(c, i * 8 + 5, normals, i * 3, 3);
                parts.computeIfAbsent(section, key -> new LinkedHashMap<>()).computeIfAbsent(part, key -> new ArrayList<>()).add(new Quad(
                    c[0] - pivot.x, c[1] - pivot.y, c[2] - pivot.z, c[8] - pivot.x, c[9] - pivot.y, c[10] - pivot.z,
                    c[16] - pivot.x, c[17] - pivot.y, c[18] - pivot.z, c[24] - pivot.x, c[25] - pivot.y, c[26] - pivot.z,
                    normals, c[3], c[4], c[11], c[12], c[19], c[20], c[27], c[28]));
            }
            Map<String, Section> sections = new LinkedHashMap<>();
            for (var entry : parts.entrySet()) {
                Map<String, List<Quad>> byPart = entry.getValue();
                List<Spinner> spinners = new ArrayList<>();
                for (var spin : spinDefs.entrySet()) {
                    if (!entry.getKey().equals(spinnerSection.get(spin.getKey()))) continue;
                    JsonObject def = spin.getValue();
                    Axis axis = switch (GsonHelper.getAsString(def, "axis", "x")) {
                        case "y" -> Axis.YP;
                        case "z" -> Axis.ZP;
                        default -> Axis.XP;
                    };
                    spinners.add(new Spinner(List.copyOf(byPart.get(spin.getKey())), vec3(def.getAsJsonArray("pivot")), axis,
                        GsonHelper.getAsFloat(def, "speed", 1)));
                }
                sections.put(entry.getKey(), new Section(
                    List.copyOf(byPart.getOrDefault("static", List.of())),
                    List.copyOf(byPart.getOrDefault("emissive", List.of())),
                    List.copyOf(byPart.getOrDefault("translucent", List.of())),
                    List.copyOf(spinners)));
            }
            Stretch stretch = Stretch.NONE;
            if (root.has("stretch")) {
                JsonObject s = root.getAsJsonObject("stretch");
                stretch = new Stretch(GsonHelper.getAsInt(s, "base_length"), GsonHelper.getAsInt(s, "bay_first"),
                    GsonHelper.getAsInt(s, "bay_last"), GsonHelper.getAsInt(s, "stand_first"),
                    GsonHelper.getAsInt(s, "stand_every"), GsonHelper.getAsInt(s, "stand_last"));
            }
            return Optional.of(new PlantModel(
                Identifier.fromNamespaceAndPath("iuce", "textures/entity/" + name + ".png"), Map.copyOf(sections), stretch));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Vector3f vec3(@Nullable JsonArray arr) {
        if (arr == null) return new Vector3f();
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private static Quad buildQuad(
        String face, Vector3f min, Vector3f max,
        @Nullable Quaternionf rot, @Nullable Vector3f origin, @Nullable Vector3f groupPivot,
        float u0, float v0, float u1, float v1
    ) {
        float x0 = min.x, y0 = min.y, z0 = min.z;
        float x1 = max.x, y1 = max.y, z1 = max.z;
        Vector3f p0, p1, p2, p3, n;
        switch (face) {
            case "north" -> {
                p0 = new Vector3f(x1, y1, z0);
                p1 = new Vector3f(x1, y0, z0);
                p2 = new Vector3f(x0, y0, z0);
                p3 = new Vector3f(x0, y1, z0);
                n = new Vector3f(0, 0, -1);
            }
            case "south" -> {
                p0 = new Vector3f(x0, y1, z1);
                p1 = new Vector3f(x0, y0, z1);
                p2 = new Vector3f(x1, y0, z1);
                p3 = new Vector3f(x1, y1, z1);
                n = new Vector3f(0, 0, 1);
            }
            case "west" -> {
                p0 = new Vector3f(x0, y1, z0);
                p1 = new Vector3f(x0, y0, z0);
                p2 = new Vector3f(x0, y0, z1);
                p3 = new Vector3f(x0, y1, z1);
                n = new Vector3f(-1, 0, 0);
            }
            case "east" -> {
                p0 = new Vector3f(x1, y1, z1);
                p1 = new Vector3f(x1, y0, z1);
                p2 = new Vector3f(x1, y0, z0);
                p3 = new Vector3f(x1, y1, z0);
                n = new Vector3f(1, 0, 0);
            }
            case "up" -> {
                p0 = new Vector3f(x0, y1, z0);
                p1 = new Vector3f(x0, y1, z1);
                p2 = new Vector3f(x1, y1, z1);
                p3 = new Vector3f(x1, y1, z0);
                n = new Vector3f(0, 1, 0);
            }
            default -> { // down
                p0 = new Vector3f(x0, y0, z1);
                p1 = new Vector3f(x0, y0, z0);
                p2 = new Vector3f(x1, y0, z0);
                p3 = new Vector3f(x1, y0, z1);
                n = new Vector3f(0, -1, 0);
            }
        }
        if (rot != null && origin != null) {
            rotateAround(p0, rot, origin);
            rotateAround(p1, rot, origin);
            rotateAround(p2, rot, origin);
            rotateAround(p3, rot, origin);
            n.rotate(rot).normalize();
        }
        if (groupPivot != null) {
            p0.sub(groupPivot);
            p1.sub(groupPivot);
            p2.sub(groupPivot);
            p3.sub(groupPivot);
        }
        return new Quad(
            p0.x, p0.y, p0.z,
            p1.x, p1.y, p1.z,
            p2.x, p2.y, p2.z,
            p3.x, p3.y, p3.z,
            n.x, n.y, n.z,
            u0, v0, u0, v1, u1, v1, u1, v0
        );
    }

    private static void rotateAround(Vector3f point, Quaternionf rot, Vector3f origin) {
        point.sub(origin).rotate(rot).add(origin);
    }

    public static void emitList(List<Quad> quads, PoseStack.Pose pose, VertexConsumer consumer, int light) {
        for (int i = 0, size = quads.size(); i < size; i++) {
            quads.get(i).emit(pose, consumer, light);
        }
    }
}
