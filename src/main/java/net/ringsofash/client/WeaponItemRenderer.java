package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        String name = "arme_" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        GeoModel<Entity> m = models.computeIfAbsent(name, n -> new GeoModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer("geo_" + n)), n));
        GeoDef d = GeoDef.get(name);
        float len = d.ymax - d.ymin, mid = (d.ymax + d.ymin) / 2F;
        // en main et au sol : taille reelle (la lance et la masse sont longues) ; en inventaire : on ramene l'arme dans la case
        float s = 0.55F;
        if (ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.FIXED) s = Math.min(s, 1.25F / (len / 16F));
        ps.pushPose();
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(-45F));
        ps.scale(s, s, s);
        ps.mulPose(Axis.YP.rotationDegrees(90F));
        ps.translate(0F, 1.5F - mid / 16F, 0F);
        ps.scale(-1F, -1F, 1F);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(Renderers.tex(name)));
        m.renderToBuffer(ps, vc, light, overlay, -1);
        ps.popPose();
    }
}
