package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.gen.Reg;

/**
 * Arc elfique en vue subjective : l'arc est droit devant, la corde vers le joueur, et il reste bien visible quand on le bande
 * (la pose de tir du jeu, prevue pour une icone plate, le sortait de l'ecran). Le rendu 3D de l'arc est celui de WeaponItemRenderer.
 */
public final class BowDraw {
    private BowDraw() {}

    public static void init() { NeoForge.EVENT_BUS.addListener(BowDraw::hand); }

    private static void hand(RenderHandEvent e) {
        if (!e.getItemStack().is(Reg.ELVEN_BOW.get())) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        e.setCanceled(true);
        boolean right = (e.getHand() == InteractionHand.MAIN_HAND) == (p.getMainArm() == HumanoidArm.RIGHT);
        float i = right ? 1F : -1F;
        float pull = WeaponItemRenderer.drawAmount(e.getItemStack());
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(i * 0.56F, -0.52F + e.getEquipProgress() * -0.6F, -0.72F);
        ps.translate(-i * 0.34F, 0.36F, 0F);
        ps.scale(0.62F, 0.62F, 0.62F);
        ps.translate(0F, 0F, pull * 0.03F);                    // le bras se tend un peu
        ps.mulPose(Axis.YP.rotationDegrees(90F));              // la corde vers la camera, le ventre vers l'avant
        ps.mulPose(Axis.ZP.rotationDegrees(45F));              // annule l'inclinaison "icone" de WeaponItemRenderer
        ps.mulPose(Axis.ZP.rotationDegrees(-6F * i));
        mc.getItemRenderer().renderStatic(p, e.getItemStack(), right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !right,
                ps, e.getMultiBufferSource(), p.level(), e.getPackedLight(), OverlayTexture.NO_OVERLAY, p.getId());
        ps.popPose();
    }
}
