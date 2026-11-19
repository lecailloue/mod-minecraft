package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.ringsofash.entity.GiantArrowEntity;

/** Fleche geante : modele geo "giant_arrow" (pointe vers -z, axe a la hauteur 18 du modele), tourne dans le sens du vol. */
public class GiantArrowRenderer extends EntityRenderer<GiantArrowEntity> {
    private static final float SCALE = 0.6F;
    private final GeoModel<GiantArrowEntity> model;

    public GiantArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_giant_arrow")), "giant_arrow");
    }

    @Override
    public ResourceLocation getTextureLocation(GiantArrowEntity e) { return Renderers.tex("giant_arrow"); }

    @Override
    public void render(GiantArrowEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.lerp(pt, e.yRotO, e.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        float shake = e.shakeTime - pt;
        if (shake > 0.0F) ps.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(0.0F, -0.375F, 1.75F);            // l'axe de la fleche passe par l'entite, ~28 px derriere la pointe
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e)));
        model.renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, -1);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }
}
