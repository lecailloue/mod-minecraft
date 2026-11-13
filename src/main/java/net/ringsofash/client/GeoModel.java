package net.ringsofash.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.ringsofash.entity.EagleEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Modele generique anime d'apres le nom des pieces (head, leg_*, arm_*, wing_*, tail*, tent*, jaw, flame*, whip*). */
public class GeoModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final List<ModelPart> flames = new ArrayList<>();

    public GeoModel(ModelPart root, String geo) {
        this.root = root;
        for (GeoDef.Part p : GeoDef.get(geo).parts) {
            ModelPart parent = p.parent == null ? root : parts.get(p.parent);
            parts.put(p.name, parent.getChild(p.name));
            if (p.name.startsWith("flame")) flames.add(parts.get(p.name));
        }
    }

    @Override
    public ModelPart root() { return root; }

    private ModelPart p(String n) { return parts.get(n); }

    @Override
    public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
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
    }

    private void leg(String n, float rot) {
        ModelPart l = p(n);
        if (l != null) l.xRot += rot;
    }
}
