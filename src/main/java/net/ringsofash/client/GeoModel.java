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

    public GeoModel(ModelPart root, String geo) {
        this.root = root;
        for (GeoDef.Part p : GeoDef.get(geo).parts) {
            ModelPart parent = p.parent == null ? root : parts.get(p.parent);
            parts.put(p.name, parent.getChild(p.name));
            if (p.name.startsWith("flame")) flames.add(parts.get(p.name));
            if (p.name.startsWith("couronne") || p.name.contains("branche")) branches.add(parts.get(p.name));
        }
    }

    @Override
    public ModelPart root() { return root; }

    public ModelPart part(String n) { return parts.get(n); }

    /** Teinte multipliee a la texture (variations d'un individu a l'autre), -1 = aucune. */
    private int tint = -1;

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay, int color) {
        super.renderToBuffer(ps, vc, light, overlay, tint == -1 ? color : net.minecraft.util.FastColor.ARGB32.multiply(color, tint));
    }

    private ModelPart p(String n) { return parts.get(n); }

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
            boolean flying = !e.onGround() || (e instanceof EagleEntity ea && ea.isFlying());
            float flap = flying ? Mth.sin(age * 0.45F) * 0.6F : Mth.sin(age * 0.08F) * 0.06F - 0.05F;
            wr.zRot += flap; wl.zRot -= flap;
            ModelPart wr2 = p("wing_r2"), wl2 = p("wing_l2");
            if (wr2 != null && wl2 != null) { wr2.zRot += flap * 0.5F; wl2.zRot -= flap * 0.5F; }
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
        if (e instanceof FoeEntity f && f.isTroll()) trollPose(f, swing, amount, age);
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
