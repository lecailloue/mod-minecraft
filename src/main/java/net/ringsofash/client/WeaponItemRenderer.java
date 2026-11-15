package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Armes d'orques rendues en 3D : le meme modele (cubes) que celui que les orques tiennent, au sol, dans l'inventaire et en main.
 * Le modele est incline a 45 degres comme un sprite d'epee, pour que les reglages d'affichage "handheld" s'appliquent tels quels.
 */
public class WeaponItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final Map<String, GeoModel<Entity>> models = new HashMap<>();

    public WeaponItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** lueur des lames sentinelles : seulement quand leur cible est proche (comme avant) ; les autres brillent toujours */
    private static boolean glowing(String id) {
        net.minecraft.world.entity.player.Player p = Minecraft.getInstance().player;
        return switch (id) {
            case "elven_blade", "elven_glaive" -> ClientSetup.orcNear(p);
            case "sentinel_blade" -> ClientSetup.near(1, p, m -> m instanceof net.minecraft.world.entity.monster.Enemy);
            case "sun_blade" -> ClientSetup.near(2, p, net.ringsofash.item.SpecialSword::isUndead);
            default -> true;
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        String name = "arme_" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        GeoModel<Entity> m = models.computeIfAbsent(name, n -> new GeoModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer("geo_" + n)), n));
        GeoDef d = GeoDef.get(name);
        float len = d.ymax - d.ymin, mid = (d.ymax + d.ymin) / 2F;
        boolean hand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        // en main et au sol : taille reelle (la lance et la masse sont longues) ; en inventaire : on ramene l'arme dans la case
        float s = 0.55F;
        if (d.hold > 0) { s = 0.152F; if (hand) mid = d.hold; }          // armes de weapons3d : 1 unite du modele = 1 cm, tenues par la poignee
        if (name.contains("goblin")) s = Math.min(s, (name.contains("pickaxe") ? 1.2F : 1.9F) / (len / 16F));   // baton du roi : 1,9 bloc ; pioche du gobelin : 1,2 bloc
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.FIXED) s = Math.min(s, 1.25F / (len / 16F));
        ps.pushPose();
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(-45F));
        ps.scale(s, s, s);
        boolean icon = d.hold > 0 && ctx == ItemDisplayContext.GUI;      // icone : face large de la lame vers nous
        if (!icon) ps.mulPose(Axis.YP.rotationDegrees(90F));
        ps.translate(0F, 1.5F - mid / 16F, 0F);
        ps.scale(-1F, -1F, 1F);
        if (icon) ps.scale(2.4F, 1F, 1F);                                  // proportions reelles = trait de 1 px : on epaissit pour l'icone
        VertexConsumer vc = buf.getBuffer(d.translucent ? RenderType.entityTranslucent(Renderers.tex(name)) : RenderType.entityCutoutNoCull(Renderers.tex(name)));
        m.renderToBuffer(ps, vc, light, overlay, -1);
        if (d.glow && glowing(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()))
            m.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(Renderers.tex(name + "_glow"))), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        ps.popPose();
    }
}
