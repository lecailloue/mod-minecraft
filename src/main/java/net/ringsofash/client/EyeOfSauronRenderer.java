package net.ringsofash.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.block.EyeOfSauronBlockEntity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oeil de Sauron (modele Blockbench eye_of_sauron) : dessine au centre du bloc d'ancrage, a pleine lumiere, et tourne en permanence vers la
 * camera du joueur (lacet et inclinaison). Animations lues dans animations/eye_of_sauron.animation.json : flammes qui pulsent, flottement,
 * pupille qui se dilate, et un clignement de temps en temps.
 */
public class EyeOfSauronRenderer implements BlockEntityRenderer<EyeOfSauronBlockEntity> {
    private static final ResourceLocation TEX = RingsOfAsh.id("textures/entity/eye_of_sauron.png");
    /** facteur de taille : le modele fait 9,5 blocs de large, la couronne de Barad-dur laisse environ 18 blocs libres entre les pointes */
    private static final float SCALE = 1.6F;
    private static final float PIVOT_Y = 56F;                      // centre de l'oeil dans le modele (unites de 1/16 de bloc)

    // ------------------------------------------------------------------ donnees du modele (lues une fois)
    private static final class Bone { String name; String parent; float[] pivot = new float[3]; final List<float[]> quads = new ArrayList<>(); final List<Bone> kids = new ArrayList<>(); }
    private static final class Track { float[] times; float[][] vals; boolean sine; }
    private static final class Anim { float length; boolean loop; final Map<String, Track> pos = new HashMap<>(), scale = new HashMap<>(); }

    private static Bone ROOT;
    private static Anim IDLE, BLINK;

    private static synchronized void load() {
        if (ROOT != null) return;
        try {
            JsonObject geo = parse("/assets/ringsofash/geo/eye_of_sauron.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject desc = geo.getAsJsonObject("description");
            float tw = desc.get("texture_width").getAsFloat(), th = desc.get("texture_height").getAsFloat();
            Map<String, Bone> bones = new LinkedHashMap<>();
            for (JsonElement be : geo.getAsJsonArray("bones")) {
                JsonObject bo = be.getAsJsonObject();
                Bone b = new Bone();
                b.name = bo.get("name").getAsString();
                b.parent = bo.has("parent") ? bo.get("parent").getAsString() : null;
                if (bo.has("pivot")) b.pivot = floats(bo.getAsJsonArray("pivot"));
                if (bo.has("cubes")) for (JsonElement ce : bo.getAsJsonArray("cubes")) cube(b, ce.getAsJsonObject(), tw, th);
                bones.put(b.name, b);
            }
            Bone root = null;
            for (Bone b : bones.values()) { if (b.parent == null) root = b; else bones.get(b.parent).kids.add(b); }
            ROOT = root;
            JsonObject an = parse("/assets/ringsofash/animations/eye_of_sauron.animation.json").getAsJsonObject("animations");
            IDLE = anim(an.getAsJsonObject("animation.eye_of_sauron.idle"));
            BLINK = anim(an.getAsJsonObject("animation.eye_of_sauron.blink"));
        } catch (Exception e) {
            throw new IllegalStateException("Oeil de Sauron : modele illisible", e);
        }
    }

    private static JsonObject parse(String path) throws java.io.IOException {
        try (InputStream in = EyeOfSauronRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new java.io.FileNotFoundException(path);
            return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        }
    }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    /** une face = 4 sommets (x, y, z, u, v), de haut-gauche a bas-gauche vue de l'exterieur (repere Bedrock : y vers le haut, avant = nord = -z) */
    private static void cube(Bone b, JsonObject c, float tw, float th) {
        float[] o = floats(c.getAsJsonArray("origin")), s = floats(c.getAsJsonArray("size"));
        float x0 = o[0], y0 = o[1], z0 = o[2], x1 = o[0] + s[0], y1 = o[1] + s[1], z1 = o[2] + s[2];
        JsonObject uv = c.getAsJsonObject("uv");
        for (String face : new String[]{"north", "south", "east", "west", "up", "down"}) {
            if (!uv.has(face)) continue;
            JsonObject f = uv.getAsJsonObject(face);
            float[] p = floats(f.getAsJsonArray("uv")), z = floats(f.getAsJsonArray("uv_size"));
            if (z[0] == 0 || z[1] == 0) continue;
            float ua = p[0] / tw, va = p[1] / th, ub = (p[0] + z[0]) / tw, vb = (p[1] + z[1]) / th;
            float[][] c4 = switch (face) {
                case "north" -> new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
                case "south" -> new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
                case "east" -> new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
                case "west" -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
                case "up" -> new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
                default -> new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
            };
            float[][] uvs = {{ua, va}, {ub, va}, {ub, vb}, {ua, vb}};
            float[] q = new float[20];
            for (int i = 0; i < 4; i++) { q[i * 5] = c4[i][0]; q[i * 5 + 1] = c4[i][1]; q[i * 5 + 2] = c4[i][2]; q[i * 5 + 3] = uvs[i][0]; q[i * 5 + 4] = uvs[i][1]; }
            b.quads.add(q);
        }
    }

    private static Anim anim(JsonObject a) {
        Anim r = new Anim();
        r.length = a.get("animation_length").getAsFloat();
        r.loop = a.has("loop") && a.get("loop").isJsonPrimitive() && a.get("loop").getAsJsonPrimitive().isBoolean() && a.get("loop").getAsBoolean();
        for (Map.Entry<String, JsonElement> be : a.getAsJsonObject("bones").entrySet()) {
            JsonObject ch = be.getValue().getAsJsonObject();
            if (ch.has("position")) r.pos.put(be.getKey(), track(ch.get("position")));
            if (ch.has("scale")) r.scale.put(be.getKey(), track(ch.get("scale")));
        }
        return r;
    }

    private static Track track(JsonElement e) {
        Track t = new Track();
        if (e.isJsonArray()) {                                      // valeur constante
            t.times = new float[]{0}; t.vals = new float[][]{floats(e.getAsJsonArray())};
            return t;
        }
        List<float[]> ts = new ArrayList<>();
        List<float[]> vs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> k : e.getAsJsonObject().entrySet()) {
            float[] v;
            JsonElement x = k.getValue();
            if (x.isJsonArray()) v = floats(x.getAsJsonArray());
            else { v = floats(x.getAsJsonObject().getAsJsonArray("vector")); if (x.getAsJsonObject().has("easing") && x.getAsJsonObject().get("easing").getAsString().contains("Sine")) t.sine = true; }
            ts.add(new float[]{Float.parseFloat(k.getKey())}); vs.add(v);
        }
        Integer[] order = new Integer[ts.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (p, q) -> Float.compare(ts.get(p)[0], ts.get(q)[0]));
        t.times = new float[order.length]; t.vals = new float[order.length][];
        for (int i = 0; i < order.length; i++) { t.times[i] = ts.get(order[i])[0]; t.vals[i] = vs.get(order[i]); }
        return t;
    }

    private static float[] sample(Track t, float time, float[] out) {
        int n = t.times.length;
        if (n == 1 || time <= t.times[0]) { System.arraycopy(t.vals[0], 0, out, 0, 3); return out; }
        if (time >= t.times[n - 1]) { System.arraycopy(t.vals[n - 1], 0, out, 0, 3); return out; }
        int i = 0;
        while (i < n - 2 && time >= t.times[i + 1]) i++;
        float k = (time - t.times[i]) / (t.times[i + 1] - t.times[i]);
        if (t.sine) k = 0.5F - 0.5F * Mth.cos(k * Mth.PI);
        for (int c = 0; c < 3; c++) out[c] = Mth.lerp(k, t.vals[i][c], t.vals[i + 1][c]);
        return out;
    }

    // ------------------------------------------------------------------ rendu
    public EyeOfSauronRenderer(BlockEntityRendererProvider.Context ctx) { load(); }

    @Override
    public boolean shouldRenderOffScreen(EyeOfSauronBlockEntity be) { return true; }

    /** visible de tres loin : on le voit depuis toute la zone de rendu */
    @Override
    public int getViewDistance() { return 1024; }

    /** l'oeil depasse largement le cube du bloc : sans cela il disparaitrait des que le bloc sort du champ de vision */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(EyeOfSauronBlockEntity be) { return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(14.0); }

    @Override
    public void render(EyeOfSauronBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ROOT == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double cx = be.getBlockPos().getX() + 0.5, cy = be.getBlockPos().getY() + 0.5, cz = be.getBlockPos().getZ() + 0.5;
        double dx = cam.x - cx, dy = cam.y - cy, dz = cam.z - cz, flat = Math.sqrt(dx * dx + dz * dz), dist = Math.sqrt(flat * flat + dy * dy);
        float yaw = dist < 1.0E-3 ? 0F : (float) Math.atan2(-dx, -dz);        // l'avant du modele (nord, -z) fait face a la camera
        float pitch = dist < 1.0E-3 ? 0F : (float) Math.asin(Mth.clamp(dy / dist, -1.0, 1.0));
        float time = (mc.level.getGameTime() + partial) / 20F;
        long seed = be.getBlockPos().asLong();
        float phase = (seed & 1023) / 1023F * 4F;
        float blinkT = blinkTime(time, seed);

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);                                           // le bloc d'ancrage est au centre de l'oeil
        ps.mulPose(Axis.YP.rotation(yaw));
        ps.mulPose(Axis.XP.rotation(pitch));
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(0, -PIVOT_Y / 16F, 0);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX));
        drawBone(ROOT, ps, vc, time + phase, blinkT);
        ps.popPose();
    }

    /** temps ecoule dans le clignement en cours (0..0,4 s), ou -1 : un clignement toutes les 5 a 9 secondes, decale d'un oeil a l'autre */
    private static float blinkTime(float time, long seed) {
        float period = 7F + ((seed >> 10) & 7) * 0.4F, t = (time + ((seed >> 4) & 15)) % period;
        return t < BLINK.length ? t : -1F;
    }

    private static void drawBone(Bone b, PoseStack ps, VertexConsumer vc, float time, float blinkT) {
        ps.pushPose();
        float[] v = new float[3];
        float px = b.pivot[0] / 16F, py = b.pivot[1] / 16F, pz = b.pivot[2] / 16F;
        float tx = 0, ty = 0, tz = 0, sx = 1, sy = 1, sz = 1;
        Track tr = IDLE.pos.get(b.name);
        if (tr != null) { sample(tr, time % IDLE.length, v); tx += v[0] / 16F; ty += v[1] / 16F; tz += v[2] / 16F; }
        tr = IDLE.scale.get(b.name);
        if (tr != null) { sample(tr, time % IDLE.length, v); sx *= v[0]; sy *= v[1]; sz *= v[2]; }
        if (blinkT >= 0F) {
            tr = BLINK.scale.get(b.name);
            if (tr != null) { sample(tr, blinkT, v); sx *= v[0]; sy *= v[1]; sz *= v[2]; }
        }
        ps.translate(tx, ty, tz);
        ps.translate(px, py, pz);
        ps.scale(sx, sy, sz);
        ps.translate(-px, -py, -pz);
        PoseStack.Pose pose = ps.last();
        for (float[] q : b.quads) {
            for (int i = 0; i < 4; i++) {
                vc.addVertex(pose, q[i * 5] / 16F, q[i * 5 + 1] / 16F, q[i * 5 + 2] / 16F).setColor(255, 255, 255, 255).setUv(q[i * 5 + 3], q[i * 5 + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0F, 1F, 0F);
            }
        }
        for (Bone k : b.kids) drawBone(k, ps, vc, time, blinkT);
        ps.popPose();
    }
}
