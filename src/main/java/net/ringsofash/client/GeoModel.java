package net.ringsofash.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.ringsofash.entity.EagleEntity;
import net.ringsofash.entity.FoeEntity;
import net.ringsofash.entity.TrollSlamGoal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Modele generique anime d'apres le nom des pieces (head, leg_*, arm_*, wing_*, tail*, tent*, jaw, flame*, whip*). */
public class GeoModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final List<ModelPart> flames = new ArrayList<>();
    private final List<ModelPart> branches = new ArrayList<>();
    /** Sylvestres : feuillages, feuilles, barbes et branches qui bougent au vent ; [type, rang dans la chaine, phase] */
    private final List<ModelPart> sway = new ArrayList<>();
    private final List<float[]> swayInfo = new ArrayList<>();

    public GeoModel(ModelPart root, String geo) {
        this.root = root;
        for (GeoDef.Part p : GeoDef.get(geo).parts) {
            ModelPart parent = p.parent == null ? root : parts.get(p.parent);
            parts.put(p.name, parent.getChild(p.name));
            if (p.name.startsWith("flame")) flames.add(parts.get(p.name));
            if (p.name.startsWith("couronne") || p.name.contains("branche")) branches.add(parts.get(p.name));
            if (geo.startsWith("sylvestre")) addSway(p.name);
        }
    }

    @Override
    public ModelPart root() { return root; }

    public ModelPart part(String n) { return parts.get(n); }

    /** Teinte multipliee a la texture (variations d'un individu a l'autre), -1 = aucune. */
    private int tint = -1;
    /** Opacite 0..255 (spectres translucides). */
    public int alpha = 255;

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay, int color) {
        int c = tint == -1 ? color : net.minecraft.util.FastColor.ARGB32.multiply(color, tint);
        if (alpha < 255) c = net.minecraft.util.FastColor.ARGB32.multiply(c, net.minecraft.util.FastColor.ARGB32.color(alpha, 255, 255, 255));
        super.renderToBuffer(ps, vc, light, overlay, c);
    }

    private ModelPart p(String n) { return parts.get(n); }

    private void addSway(String n) {
        int type = n.startsWith("frond") ? 0 : n.contains("leaf") ? 1 : n.startsWith("beard") || n.startsWith("must") ? 2
                : n.startsWith("cr") || n.startsWith("top") || n.startsWith("shb") || n.startsWith("back") ? 3 : -1;
        if (type < 0 || n.endsWith("yaw")) return;
        int cut = n.lastIndexOf('_'), depth = 0;
        String base = n;
        if (cut > 0 && n.substring(cut + 1).matches("\\d+")) { depth = Integer.parseInt(n.substring(cut + 1)); base = n.substring(0, cut); }
        sway.add(parts.get(n));
        swayInfo.add(new float[]{type, depth, (base.hashCode() & 0xFFFF) / 10430F});
    }

    /**
     * Sylvestres : le vent fait onduler les rideaux de feuilles (de plus en plus vers le bout de chaque brin, qui trainent derriere quand il
     * marche), les feuilles au bout des branches frissonnent, la barbe et les branches se balancent doucement.
     */
    private void sylvPose(float swing, float amount, float age) {
        for (int i = 0; i < sway.size(); i++) {
            ModelPart m = sway.get(i);
            float[] f = swayInfo.get(i);
            int type = (int) f[0];
            float d = f[1], ph = f[2];
            float walk = Mth.sin(swing * 0.6662F - d * 0.7F) * amount;
            switch (type) {
                case 0 -> {
                    float amp = 0.045F + 0.03F * d;
                    m.xRot += Mth.sin(age * 0.06F + ph + d * 0.55F) * amp + Mth.sin(age * 0.03F + d * 0.4F) * 0.02F * (d + 1) - amount * 0.08F * (1 + d * 0.3F) + walk * 0.06F;
                    m.zRot += Mth.cos(age * 0.047F + ph * 1.3F + d * 0.5F) * amp * 0.8F;
                }
                case 1 -> {
                    m.xRot += Mth.sin(age * 0.22F + ph) * 0.14F + walk * 0.1F;
                    m.zRot += Mth.cos(age * 0.18F + ph * 1.7F) * 0.14F;
                }
                case 2 -> {
                    m.xRot += Mth.sin(age * 0.05F + ph + d * 0.5F) * 0.03F * (1 + d * 0.4F) + walk * 0.05F;
                    m.zRot += Mth.cos(age * 0.04F + ph + d * 0.4F) * 0.02F * (1 + d * 0.3F);
                }
                default -> {
                    m.zRot += Mth.sin(age * 0.04F + ph) * 0.025F;
                    m.xRot += Mth.cos(age * 0.035F + ph * 1.3F) * 0.02F;
                }
            }
        }
    }

    @Override
    public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        tint = -1;
        if (e instanceof net.ringsofash.entity.TreemanEntity) {             // chaque Sylvgarde a une ecorce tres legerement differente
            long h = e.getUUID().getMostSignificantBits() ^ e.getUUID().getLeastSignificantBits();
            int r = 232 + (int) ((h & 0xFF) % 24), g = 232 + (int) (((h >> 8) & 0xFF) % 24), b = 226 + (int) (((h >> 16) & 0xFF) % 30);
            tint = net.minecraft.util.FastColor.ARGB32.color(255, Math.min(255, r), Math.min(255, g), Math.min(255, b));
        }
        float atk = Mth.sin(this.attackTime * Mth.PI);
        // mobs qui ne se deplacent qu'en volant : jamais de cycle de marche, ailes toujours battantes, pattes repliees
        boolean crow = e instanceof net.ringsofash.entity.BeastEntity bc && bc.isCrow();
        boolean bird = e instanceof EagleEntity || crow;
        boolean worm = e instanceof net.ringsofash.entity.BossEntity bo && bo.kind() == net.ringsofash.entity.BossEntity.Kind.DRAGON;
        if (bird || worm) { swing = 0F; amount = 0F; }
        float vy = (float) e.getDeltaMovement().y;
        ModelPart head = p("head"), neck = p("neck");
        if (head != null) {
            float k = neck != null ? 0.5F : 1.0F;
            head.yRot += yaw * Mth.DEG_TO_RAD * k; head.xRot += pitch * Mth.DEG_TO_RAD * k;
            if (neck != null) { neck.yRot += yaw * Mth.DEG_TO_RAD * 0.5F; neck.xRot += pitch * Mth.DEG_TO_RAD * 0.3F; }
        }
        float c = Mth.cos(swing * 0.6662F), c2 = Mth.cos(swing * 0.6662F + Mth.PI);
        leg("leg_r", c * 1.0F * amount); leg("leg_l", c2 * 1.0F * amount);
        leg("leg_fr", c * 0.9F * amount); leg("leg_bl", c * 0.9F * amount);
        leg("leg_fl", c2 * 0.9F * amount); leg("leg_br", c2 * 0.9F * amount);
        ModelPart ar = p("arm_r"), al = p("arm_l");
        if (ar != null) ar.xRot += c2 * 0.7F * amount - atk * 1.9F;
        if (al != null) al.xRot += c * 0.7F * amount - atk * 1.2F;
        if (ar != null) ar.zRot += Mth.cos(age * 0.06F) * 0.03F + 0.03F;
        if (al != null) al.zRot -= Mth.cos(age * 0.06F) * 0.03F + 0.03F;
        ModelPart wr = p("wing_r"), wl = p("wing_l");
        if (wr != null && wl != null) {
            boolean flying = bird || worm || !e.onGround() || (e instanceof EagleEntity ea && ea.isFlying());
            ModelPart wr2 = p("wing_r2"), wl2 = p("wing_l2");
            if (!flying && wr2 != null && wl2 != null) {          // perche : ailes repliees le long du corps, legere respiration
                float br = Mth.sin(age * 0.08F) * 0.02F;
                wr.zRot += -0.52F + br; wl.zRot += 0.52F - br;
                wr2.yRot += -1.40F; wl2.yRot += 1.40F;
                wr2.zRot += 0.44F; wl2.zRot -= 0.44F;
            } else {
                float rate = worm ? 0.16F : crow ? 0.85F : bird ? 0.34F : 0.45F;
                float amp = worm ? 0.45F : crow ? 0.75F : bird ? 0.55F : 0.6F;
                if ((bird || worm) && vy < -0.04F) amp *= 0.35F;                 // plane en descendant
                float flap = flying ? Mth.sin(age * rate) * amp : Mth.sin(age * 0.08F) * 0.06F - 0.05F;
                wr.zRot += flap; wl.zRot -= flap;
                if (wr2 != null && wl2 != null) { wr2.zRot += flap * 0.5F; wl2.zRot -= flap * 0.5F; }
            }
        }
        if (bird || worm) {
            ModelPart bd = p("body");
            float beat = age * (worm ? 0.16F : crow ? 0.85F : 0.34F);
            if (bd != null) {
                bd.y += Mth.cos(beat) * (worm ? 1.6F : 0.5F);
                bd.xRot += Mth.clamp(-vy * 0.9F, -0.35F, 0.35F);
            }
            if (bird) { leg("leg_r", 0.9F); leg("leg_l", 0.9F); }
            if (worm) { leg("leg_fr", -0.6F); leg("leg_fl", -0.6F); leg("leg_br", 0.55F); leg("leg_bl", 0.55F); }
            ModelPart tl = p("tail");
            if (tl != null && bird) tl.xRot += Mth.clamp(vy * 0.8F, -0.3F, 0.3F) + Mth.sin(age * 0.1F) * 0.05F;
        }
        for (int i = 1; i <= 3; i++) {
            ModelPart t = p(i == 1 ? "tail" : "tail" + i);
            if (t != null) { t.yRot += Mth.cos(swing * 0.5F + i) * 0.25F * amount + Mth.sin(age * 0.07F + i * 0.8F) * 0.12F; t.xRot += Mth.sin(age * 0.05F + i) * 0.04F; }
        }
        for (int i = 1; i <= 8; i++) {
            ModelPart t = p("tent" + i), t2 = p("tent" + i + "b");
            if (t == null) break;
            float ph = age * 0.11F + i * 0.8F;
            t.xRot += Mth.sin(ph) * 0.22F + atk * (i % 2 == 0 ? 0.9F : -0.5F); t.zRot += Mth.cos(ph * 0.9F) * 0.22F;
            if (t2 != null) { t2.xRot += Mth.sin(ph + 1.2F) * 0.4F + atk * 0.6F; t2.zRot += Mth.cos(ph + 0.7F) * 0.35F; }
        }
        // flammes : chaque pointe vacille (etirement + leger balancement), dephasee selon son rang
        for (int i = 0; i < flames.size(); i++) {
            ModelPart f = flames.get(i);
            float ph = i * 1.7F, s = Mth.sin(age * 0.55F + ph);
            f.yScale = 1.0F + s * 0.14F + Mth.sin(age * 1.3F + ph * 2.1F) * 0.06F;
            f.xScale = f.zScale = 1.0F - s * 0.05F;
            f.xRot += Mth.sin(age * 0.23F + ph) * 0.07F; f.zRot += Mth.cos(age * 0.19F + ph) * 0.07F;
        }
        // branches : leger balancement dans le vent
        for (int i = 0; i < branches.size(); i++) {
            ModelPart b = branches.get(i);
            b.zRot += Mth.sin(age * 0.045F + i * 0.9F) * 0.035F;
            b.xRot += Mth.cos(age * 0.038F + i * 1.3F) * 0.025F;
        }
        // fouet : une onde parcourt la chaine de segments, et il claque vers le haut a l'attaque
        for (int i = 1; i <= 12; i++) {
            ModelPart w = p("whip" + i);
            if (w == null) break;
            w.xRot += Mth.sin(age * 0.2F - i * 0.75F) * 0.15F - atk * 0.12F;
            w.yRot += Mth.cos(age * 0.14F - i * 0.6F) * 0.12F + Mth.cos(swing * 0.33F - i * 0.5F) * 0.08F * amount;
        }
        // trompe du Mumak : balancement en vague ; oreilles : battement lent
        for (int i = 1; i <= 4; i++) {
            ModelPart t = p(i == 1 ? "trunk" : "trunk" + i);
            if (t == null) break;
            t.xRot += Mth.sin(age * 0.07F - i * 0.7F) * 0.07F + Mth.sin(swing * 0.6662F - i * 0.8F) * 0.1F * amount;
            t.yRot += Mth.cos(age * 0.05F - i * 0.5F) * 0.06F;
        }
        ModelPart er = p("ear_r"), el = p("ear_l");
        if (er != null && el != null) {
            float fl = Mth.sin(age * 0.11F + e.getId()) * 0.06F + Math.max(0F, Mth.sin(age * 0.023F + e.getId() * 1.7F) - 0.92F) * 3F;
            er.yRot += fl; el.yRot -= fl;
            er.zRot += fl * 0.5F; el.zRot -= fl * 0.5F;
        }
        ModelPart jaw = p("jaw");
        if (jaw != null) jaw.xRot += 0.08F + Math.abs(Mth.sin(age * 0.05F)) * 0.08F + atk * 0.6F;
        ModelPart body = p("body");
        if (body != null && ar != null) body.xRot += atk * 0.15F;
        // masse a deux mains (bras et masse rattaches au corps) : balancement lourd, coup de masse en abattant le buste
        if (body != null && p("mace") != null) {
            body.xRot += atk * 0.55F + Mth.sin(age * 0.05F) * 0.02F;
            body.zRot += Mth.cos(swing * 0.6662F) * 0.04F * amount;
        }
        // cape : se souleve vers l'arriere quand il marche, ondule en continu (segments de plus en plus mobiles)
        ModelPart cape = p("cape");
        if (cape != null) {
            float run = Math.min(1F, amount * 1.6F);
            float base = 0.06F + run * 0.32F;
            cape.xRot += base + Mth.sin(age * 0.05F) * 0.025F + atk * 0.12F;
            String[] cs = {"cape_c", "cape_d", "cape_e"};
            for (int i = 0; i < cs.length; i++) {
                ModelPart cp = p(cs[i]);
                if (cp == null) continue;
                cp.xRot += 0.04F + run * 0.2F + Mth.sin(age * 0.09F - i * 0.9F) * (0.05F + 0.03F * i) + Mth.sin(swing * 0.6662F - i) * 0.08F * amount;
                cp.zRot += Mth.cos(age * 0.07F - i * 0.7F) * 0.025F * (i + 1);
            }
        }
        if (e instanceof net.ringsofash.entity.ShadowRiderEntity) {             // cavalier : le bras d'epee se leve a l'attaque, la monture tangue au trot
            ModelPart rr = p("rarm_r");
            if (rr != null) rr.xRot -= atk * 1.5F;
            if (body != null) { body.xRot += Mth.sin(swing * 0.6662F * 2F) * 0.025F * amount; body.y += Mth.abs(Mth.sin(swing * 0.6662F)) * -0.9F * amount; }
        }
        if (e instanceof FoeEntity f && f.isTroll()) trollPose(f, swing, amount, age);
        if (e instanceof FoeEntity f && f.kind() == FoeEntity.Kind.GOBLIN_KING) kingPose(e.getId(), swing, amount, age, atk, yaw, pitch);
        if (e instanceof net.ringsofash.entity.GiantSpiderEntity) spiderPose(swing, amount, age, atk);
        if (!sway.isEmpty()) sylvPose(swing, amount, age);
        if (e instanceof net.ringsofash.entity.ShadowDragonEntity) dragonPose(e, age, atk);
        if (e instanceof net.ringsofash.entity.ShadowChiefEntity) masterPose(e, age, amount);
    }

    /**
     * Roi des gobelins : demarche lourde et lente (jambes courtes, tout le corps roule d'un pied sur l'autre), longs bras qui se balancent,
     * ventre qui ballotte a chaque pas, tete qui dodeline et bajoue qui se balance en trois segments.
     */
    /** Etat du pendule de la bajoue du roi, par entite : [segment][ax, vx, az, vz] puis dernier lacet / tangage de la tete et dernier age. */
    private final java.util.Map<Integer, float[]> dewState = new java.util.HashMap<>();
    private static final float[] DEW_K = {0.13F, 0.10F, 0.08F}, DEW_C = {0.11F, 0.09F, 0.07F};

    /**
     * La bajoue pend sous la tete : elle est fixee a la tete mais obeit a la gravite et a l'inertie. Chaque segment est un ressort amorti qui
     * tend a rester a la verticale (donc a contre-tourner quand la tete regarde en haut/en bas ou de cote), pousse par le rebond des pas et
     * par les mouvements brusques de la tete, et entraine par le segment du dessus : elle se balance en retard sur la tete.
     */
    private void dewlap(int id, float g, float age, float amount, float hy, float hp) {
        ModelPart[] d = {p("dewlap"), p("dewlap2"), p("dewlap3")};
        if (d[0] == null) return;
        if (dewState.size() > 64) dewState.clear();
        float[] st = dewState.computeIfAbsent(id, k -> new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, hy, hp, age});
        float dt = Mth.clamp(age - st[14], 0F, 3F);
        float dyaw = dt > 0 ? (hy - st[12]) / dt : 0F, dpitch = dt > 0 ? (hp - st[13]) / dt : 0F;
        st[12] = hy; st[13] = hp; st[14] = age;
        int n = Math.max(1, (int) Math.ceil(dt * 2F));
        float h = dt / n;
        for (int it = 0; it < n; it++) {
            float t = age - dt + (it + 1) * h;
            float walk = amount;
            float prevVx = 0F, prevVz = 0F;
            for (int i = 0; i < 3; i++) {
                int o = i * 4;
                // cible : rester pendu a la verticale -> contre-rotation de la tete (en partie pour la tete, entierement pour le bout)
                float tx = -hp * (0.55F + 0.15F * i), tz = hy * (0.30F + 0.10F * i);
                float fx = Mth.sin(g * 2F - 0.9F - 0.7F * i) * 0.020F * walk - dpitch * 0.9F + prevVx * 0.18F + Mth.sin(t * 0.06F - 0.8F * i) * 0.0012F;
                float fz = Mth.sin(g - 0.6F - 0.8F * i) * 0.012F * walk + dyaw * 0.8F + prevVz * 0.18F;
                st[o + 1] += (-DEW_K[i] * (st[o] - tx) - DEW_C[i] * st[o + 1] + fx) * h;
                st[o + 3] += (-DEW_K[i] * (st[o + 2] - tz) - DEW_C[i] * st[o + 3] + fz) * h;
                st[o] += st[o + 1] * h; st[o + 2] += st[o + 3] * h;
                st[o] = Mth.clamp(st[o], -0.9F, 0.9F); st[o + 2] = Mth.clamp(st[o + 2], -0.7F, 0.7F);
                prevVx = st[o + 1]; prevVz = st[o + 3];
            }
        }
        for (int i = 0; i < 3; i++) if (d[i] != null) { d[i].xRot += st[i * 4]; d[i].zRot += st[i * 4 + 2]; }
    }

    /**
     * Araignee geante : marche en tetrapode (pattes 1g, 3g, 2d, 4d ensemble, les quatre autres en opposition) ; chaque patte avance et se leve
     * pendant qu'elle avance. L'abdomen oscille, le corps se baisse a chaque pas, les crocs et les pattes avant s'ecartent quand elle mord.
     */
    /**
     * Dragon des ombres : en vol permanent. Les ailes battent (le poignet suit avec du retard), le corps monte et descend avec elles, et le cou
     * ondule de bas en haut comme un serpent : une onde qui parcourt les 7 segments du cou, plus forte vers la tete. La queue ondule en
     * lacet, la machoire s'ouvre quand il mord, la cape du chef flotte. Le nez se leve quand il monte et se baisse quand il pique.
     */
    /** Chef des ombres : cape qui flotte ; assis sur le dragon, jambes ployees de part et d'autre de la selle et bras en avant. */
    private void masterPose(T e, float age, float amount) {
        boolean ride = e.isPassenger();
        for (int i = 0; i < 4; i++) {
            ModelPart c = p("cape_" + i);
            if (c != null) c.xRot += 0.10F + (ride ? 0.22F : amount * 0.5F) * (1 + i * 0.4F) + Mth.sin(age * 0.25F - i * 0.9F) * (ride ? 0.12F : 0.05F);
        }
        ModelPart lr = p("leg_r"), ll = p("leg_l"), ar = p("arm_r"), al = p("arm_l");
        if (!ride) return;
        if (lr != null) { lr.xRot += -0.95F; lr.zRot += 0.32F; }
        if (ll != null) { ll.xRot += -0.95F; ll.zRot -= 0.32F; }
        if (ar != null) ar.xRot += -0.8F;
        if (al != null) al.xRot += -0.8F;
    }

    private void dragonPose(T e, float age, float atk) {
        float beat = age * 0.17F, flap = Mth.sin(beat);
        ModelPart body = p("body");
        float dy = (float) e.getDeltaMovement().y;
        if (body != null) {
            body.y += Mth.cos(beat) * 1.6F;
            body.xRot += Mth.clamp(-dy * 0.9F, -0.35F, 0.35F) + 0.04F;
            body.zRot += Mth.sin(age * 0.05F) * 0.02F;
        }
        ModelPart wr = p("dwing_r"), wl = p("dwing_l"), wr2 = p("dwing_r2"), wl2 = p("dwing_l2");
        float up = 0.10F + flap * 0.55F, tip = Mth.sin(beat - 1.0F) * 0.5F;
        if (wr != null) wr.zRot += up;
        if (wl != null) wl.zRot -= up;
        if (wr2 != null) wr2.zRot += tip;
        if (wl2 != null) wl2.zRot -= tip;
        float w = age * 0.15F;
        for (int i = 0; i < 7; i++) {
            ModelPart n = p("dneck_" + i);
            if (n != null) n.xRot += Mth.sin(w - i * 0.8F) * (0.14F + 0.015F * i);
        }
        ModelPart head = p("head"), jaw = p("jaw");
        if (head != null) head.xRot += Mth.sin(w - 7 * 0.8F) * 0.12F;
        if (jaw != null) jaw.xRot += atk * 0.8F + Math.max(0F, Mth.sin(age * 0.05F)) * 0.08F;
        for (int i = 0; i < 7; i++) {
            ModelPart t = p("dtail_" + i);
            if (t != null) { t.yRot += Mth.sin(age * 0.1F - i * 0.7F) * 0.13F; t.xRot += Mth.sin(age * 0.1F - i * 0.7F + 1.6F) * 0.04F; }
        }
        ModelPart rider = p("rider");
        if (rider != null) rider.xRot += Mth.sin(beat + 0.6F) * 0.02F;
        for (int i = 0; i < 4; i++) {
            ModelPart c = p("cape_" + i);
            if (c != null) c.xRot += 0.12F + Mth.sin(age * 0.32F - i * 0.9F) * 0.14F;
        }
    }

    private void spiderPose(float swing, float amount, float age, float atk) {
        float ph = swing * 0.85F;
        for (int i = 1; i <= 4; i++) {
            for (int sd = 0; sd < 2; sd++) {
                String s = i + (sd == 0 ? "l" : "r");
                int sg = sd == 0 ? -1 : 1;
                float th = ph + (((i + sd) % 2 == 1) ? 0F : Mth.PI);
                float fwd = Mth.cos(th) * 0.42F * amount;                      // + = vers l'avant
                float lift = Math.max(0F, -Mth.sin(th)) * 0.55F * amount;      // se leve quand elle avance
                ModelPart yaw = p("leg_" + s + "_yaw"), a = p("leg_" + s + "_a"), b = p("leg_" + s + "_b"), c = p("leg_" + s + "_c");
                if (yaw != null) yaw.yRot += -sg * fwd;
                if (a != null) a.zRot += sg * (lift + Mth.sin(age * 0.07F + i * 1.3F + sd) * 0.02F);
                if (b != null) b.zRot -= sg * lift * 0.9F;
                if (c != null) c.zRot += sg * lift * 0.3F;
                if (i == 1) {                                                  // les pattes avant se dressent pour mordre
                    if (a != null) a.zRot += sg * atk * 0.7F;
                    if (yaw != null) yaw.yRot += -sg * atk * 0.3F;
                }
            }
        }
        ModelPart root = p("body_root"), abd = p("abdomen"), fl = p("fang_l"), fr = p("fang_r");
        if (root != null) root.y += Mth.cos(ph * 2F) * 0.9F * amount;
        if (abd != null) { abd.xRot += Mth.sin(age * 0.05F) * 0.025F - atk * 0.12F; abd.zRot += Mth.sin(ph) * 0.03F * amount; }
        float tw = Mth.sin(age * 0.11F) * 0.04F + atk * 0.55F;
        if (fl != null) fl.xRot += tw;
        if (fr != null) fr.xRot += tw;
    }

    private void kingPose(int id, float swing, float amount, float age, float atk, float yaw, float pitch) {
        ModelPart body = p("body"), head = p("head"), belly = p("belly"), lr = p("leg_r"), ll = p("leg_l"), ar = p("arm_r"), al = p("arm_l");
        float g = swing * 0.6662F * 0.85F, s = Mth.sin(g), c = Mth.cos(g);
        if (lr != null) lr.xRot *= 0.62F;
        if (ll != null) ll.xRot *= 0.62F;
        if (ar != null) ar.xRot += Mth.cos(g + Mth.PI) * 0.30F * amount;                 // les bras pendent loin devant : grand balancier
        if (al != null) al.xRot += c * 0.30F * amount;
        if (ar != null) ar.zRot += (s * 0.05F) * amount;
        if (al != null) al.zRot -= (s * 0.05F) * amount;
        if (body != null) {
            body.zRot += c * 0.07F * amount;                                               // il roule d'un pied sur l'autre
            body.yRot += c * 0.05F * amount;
            body.xRot += Mth.sin(age * 0.05F) * 0.015F + atk * 0.28F;                      // respiration, et il s'abat en frappant
        }
        root.y += Mth.abs(s) * 1.5F * amount;                                              // il s'affaisse a chaque pas
        if (belly != null) {
            belly.xRot += Mth.sin(g * 2F - 0.9F) * 0.13F * amount + Mth.sin(age * 0.07F) * 0.02F - atk * 0.10F;
            belly.y += Mth.cos(g * 2F - 0.5F) * 1.4F * amount;
            belly.zRot += Mth.sin(g - 0.6F) * 0.03F * amount;
        }
        if (head != null) head.xRot += Mth.sin(g * 2F - 0.3F) * 0.05F * amount;
        dewlap(id, g, age, amount, yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD);
    }

    private static float ease(float k) { k = Mth.clamp(k, 0F, 1F); return k * k * (3F - 2F * k); }

    /**
     * Trolls : (1) coup de poing lent a deux mains (poings leves, puis abattus), (2) course a quatre pattes comme un gorille quand
     * ils chassent : buste presque a l'horizontale, bras tendus vers le sol qui se relaient avec les jambes.
     */
    private void trollPose(FoeEntity f, float swing, float amount, float age) {
        ModelPart body = p("body"), head = p("head"), ar = p("arm_r"), al = p("arm_l"), lr = p("leg_r"), ll = p("leg_l");
        if (body == null || head == null || ar == null || al == null) return;
        int t = f.slamTick();
        if (t > 0) {
            float tt = t + (age - Mth.floor(age));                                   // + partie fractionnaire = fluidite entre deux ticks
            float W = TrollSlamGoal.WINDUP, S = TrollSlamGoal.STRIKE, R = TrollSlamGoal.RECOVER;
            float bodyD, armD, spread = 0F;
            if (tt <= W) {                                                           // il se redresse, poings au-dessus de la tete
                float k = ease(tt / W);
                bodyD = -0.55F * k; armD = -2.9F * k; spread = 0.22F * k;
                if (tt > W - 5F) { bodyD += Mth.sin(age * 2.6F) * 0.02F; armD += Mth.sin(age * 3.1F) * 0.04F; }   // tremblement de la charge
            } else if (tt <= W + S) {                                                // il s'abat
                float k = ease((tt - W) / S);
                bodyD = Mth.lerp(k, -0.55F, 0.7F); armD = Mth.lerp(k, -2.9F, -1.0F); spread = Mth.lerp(k, 0.22F, 0F);
            } else {                                                                 // il reprend son souffle
                float k = ease((tt - W - S) / R);
                bodyD = Mth.lerp(k, 0.7F, 0F); armD = Mth.lerp(k, -1.0F, 0F);
            }
            body.xRot += bodyD;
            head.xRot -= bodyD * 0.7F;
            ar.xRot += armD; al.xRot += armD;
            ar.zRot -= spread; al.zRot += spread;
            return;
        }
        float run = f.runBlend();
        if (run < 0.01F) return;
        float g = swing * 0.62F, c = Mth.cos(g), c2 = Mth.cos(g + Mth.PI);
        float bodyAbs = body.xRot + (0.95F - body.xRot) * run;                       // inclinaison totale visee : ~55 degres
        body.xRot = bodyAbs;
        body.yRot += c * 0.10F * run;
        head.xRot -= 0.6F * run;                                                     // la tete reste relevee vers sa proie
        ar.xRot = Mth.lerp(run, ar.xRot, -bodyAbs - 0.30F + c2 * 0.55F);             // bras tendus au sol, en alternance avec les jambes
        al.xRot = Mth.lerp(run, al.xRot, -bodyAbs - 0.30F + c * 0.55F);
        ar.zRot *= 1F - run; al.zRot *= 1F - run;
        if (lr != null) lr.xRot += c * 0.5F * run * amount;
        if (ll != null) ll.xRot += c2 * 0.5F * run * amount;
        root.y -= Mth.abs(Mth.sin(g)) * 1.6F * run * amount;                         // bond de la course
    }

    private void leg(String n, float rot) {
        ModelPart l = p(n);
        if (l != null) l.xRot += rot;
    }
}
