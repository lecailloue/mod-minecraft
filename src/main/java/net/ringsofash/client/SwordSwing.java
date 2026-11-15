package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.item.SylvanHeart;

/**
 * Coup d'epee realiste : le bras se leve au-dessus de la tete, le buste tourne, la jambe avance et la lame tranche en diagonale de haut en bas,
 * puis suit son elan et revient. Vue a la troisieme personne (et pour les autres joueurs) : SwingPlayerModel ; vue subjective : evenement de rendu de la main.
 * Le meme mouvement sert a toutes les epees (SwordItem), pour les droitiers comme pour les gauchers.
 */
public final class SwordSwing {
    private SwordSwing() {}

    public static void init() { NeoForge.EVENT_BUS.addListener(SwordSwing::hand); }

    public static boolean isSword(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof SwordItem || s.canPerformAction(ItemAbilities.SWORD_SWEEP));
    }

    /** images cles a t = 0, 0.2 (arme levee), 0.45 (coup), 0.7 (elan), 1 (repos) */
    private static final float[] T = {0F, 0.20F, 0.45F, 0.70F, 1F};

    /** mouvement du corps (radians, pour un droitier) : bras x/y/z, buste y, jambe d'appui avant x, autre jambe x, autre bras x/z */
    private static final float[][] BODY = {
            {0, -2.45F, -0.85F, -0.35F, 0},        // 0 bras : leve puis rabattu
            {0, 0.55F, -0.55F, -1.05F, 0},         // 1 bras : lacet (de la droite vers la gauche)
            {0, 0.30F, 0, -0.15F, 0},              // 2 bras : ecarte puis ramene
            {0, 0.35F, -0.55F, -0.60F, 0},         // 3 buste : tourne en arriere puis suit le coup
            {0, 0.35F, -0.55F, -0.25F, 0},         // 4 jambe du bras arme : recule puis fente en avant
            {0, -0.30F, 0.45F, 0.20F, 0},          // 5 autre jambe
            {0, -0.50F, 0.45F, 0.20F, 0},          // 6 autre bras : equilibre
            {0, -0.40F, -0.25F, -0.10F, 0}};       // 7 autre bras : ecarte

    /** vue subjective : decalage x, y, z puis rotations x, y, z (degres) de la lame */
    private static final float[][] FIRST = {
            {0, 0, 0, 0, 0, 0},
            {0.10F, 0.34F, 0.20F, 45F, -8F, -28F},
            {-0.40F, 0.02F, -0.30F, -70F, 20F, 35F},
            {-0.46F, -0.10F, -0.18F, -50F, 26F, 45F},
            {0, 0, 0, 0, 0, 0}};

    private static float ease(int seg, float f) {
        f = Mth.clamp(f, 0F, 1F);
        if (seg == 1) return 1F - (1F - f) * (1F - f) * (1F - f);          // le coup : depart vif, fin amortie
        return (1F - Mth.cos(f * Mth.PI)) * 0.5F;
    }

    private static float lerpKeys(float t, java.util.function.IntToDoubleFunction key) {
        t = Mth.clamp(t, 0F, 1F);
        int s = 0;
        while (s < T.length - 2 && t > T[s + 1]) s++;
        float f = ease(s, (t - T[s]) / (T[s + 1] - T[s]));
        return (float) (key.applyAsDouble(s) + (key.applyAsDouble(s + 1) - key.applyAsDouble(s)) * f);
    }

    static float body(int ch, float t) { return lerpKeys(t, i -> BODY[ch][i]); }

    // ---------- vue subjective
    private static void hand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || e.getSwingProgress() <= 0F) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || p.isUsingItem() || !isSword(e.getItemStack()) || SylvanHeart.isSylvan(p)) return;
        e.setCanceled(true);
        float t = e.getSwingProgress();
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        float i = right ? 1F : -1F;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(i * 0.56F, -0.52F + e.getEquipProgress() * -0.6F, -0.72F);
        float[] k = new float[6];
        for (int c = 0; c < 6; c++) { final int cc = c; k[c] = lerpKeys(t, s -> FIRST[s][cc]); }
        ps.translate(i * k[0], k[1], k[2]);
        ps.mulPose(Axis.YP.rotationDegrees(i * k[4]));
        ps.mulPose(Axis.ZP.rotationDegrees(i * k[5]));
        ps.mulPose(Axis.XP.rotationDegrees(k[3]));
        mc.getItemRenderer().renderStatic(p, e.getItemStack(), right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !right,
                ps, e.getMultiBufferSource(), p.level(), e.getPackedLight(), OverlayTexture.NO_OVERLAY, p.getId());
        ps.popPose();
    }

    // ---------- troisieme personne : modele de joueur qui remplace celui du jeu
    public static class SwingPlayerModel extends PlayerModel<AbstractClientPlayer> {
        public SwingPlayerModel(ModelPart root, boolean slim) { super(root, slim); }

        @Override
        public void setupAnim(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
            float t = this.attackTime;
            boolean slash = t > 0F && p.swingingArm == InteractionHand.MAIN_HAND && isSword(p.getMainHandItem()) && !SylvanHeart.isSylvan(p);
            if (slash) this.attackTime = 0F;
            super.setupAnim(p, limbSwing, limbSwingAmount, age, yaw, pitch);
            if (!slash) return;
            this.attackTime = t;
            float s = p.getMainArm() == HumanoidArm.RIGHT ? 1F : -1F;
            ModelPart arm = s > 0 ? rightArm : leftArm, off = s > 0 ? leftArm : rightArm;
            ModelPart leg = s > 0 ? rightLeg : leftLeg, legO = s > 0 ? leftLeg : rightLeg;
            body.yRot = body(3, t) * s;
            float sin = Mth.sin(body.yRot) * 5F, cos = Mth.cos(body.yRot) * 5F;
            rightArm.z = sin; rightArm.x = -cos; leftArm.z = -sin; leftArm.x = cos;
            arm.xRot += body(0, t);
            arm.yRot += body.yRot + body(1, t) * s;
            arm.zRot += body(2, t) * s;
            off.yRot += body.yRot;
            off.xRot += body(6, t);
            off.zRot += body(7, t) * s;
            leg.xRot += body(4, t);
            legO.xRot += body(5, t);
            leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg);
            leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
            jacket.copyFrom(body);
        }
    }
}
