package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.gen.Reg;

/** Cape a capuche portee sur le torse : pelerine evasee, collerette et capuche ouverte devant. */
public class CloakLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ModelPart root;

    public CloakLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
        super(parent);
        this.root = root;
    }

    /** Geometrie : les boites se placent dans le repere du corps (cape, collerette) ou de la tete (capuche). */
    public static LayerDefinition definition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        // cape : trois paliers qui s'evasent, pivot en haut du dos
        PartDefinition cape = r.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, 0.0F, 10, 6, 1), PartPose.offset(0, 0.5F, 2.2F));
        cape.addOrReplaceChild("cape2", CubeListBuilder.create().texOffs(0, 8).addBox(-5.5F, 6.0F, 0.0F, 11, 5, 1), PartPose.ZERO);
        cape.addOrReplaceChild("cape3", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, 11.0F, 0.0F, 12, 5, 1), PartPose.ZERO);
        // collerette sur les epaules
        r.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0, 24).addBox(-5.5F, -0.6F, -3.0F, 11, 3, 6), PartPose.ZERO);
        r.addOrReplaceChild("clasp", CubeListBuilder.create().texOffs(46, 45).addBox(-1.0F, 0.4F, -3.4F, 2, 2, 1), PartPose.ZERO);
        // capuche : dessus avance en visiere, dos, deux cotes, visage ouvert
        PartDefinition hood = r.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 34).addBox(-4.5F, -9.0F, -5.0F, 9, 1, 10), PartPose.ZERO);
        hood.addOrReplaceChild("hood_back", CubeListBuilder.create().texOffs(0, 45).addBox(-4.5F, -9.0F, 4.0F, 9, 10, 1), PartPose.ZERO);
        hood.addOrReplaceChild("hood_l", CubeListBuilder.create().texOffs(24, 45).addBox(4.0F, -9.0F, -4.0F, 1, 10, 8), PartPose.ZERO);
        hood.addOrReplaceChild("hood_r", CubeListBuilder.create().texOffs(24, 45).mirror().addBox(-5.0F, -9.0F, -4.0F, 1, 10, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static ResourceLocation texture(ItemStack s) {
        if (s.is(Reg.ELVEN_CLOAK.get())) return RingsOfAsh.id("textures/entity/cloak_elven.png");
        if (s.is(Reg.RANGER_CLOAK.get())) return RingsOfAsh.id("textures/entity/cloak_ranger.png");
        if (s.is(Reg.ASH_CLOAK.get())) return RingsOfAsh.id("textures/entity/cloak_ash.png");
        return null;
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float headYaw, float headPitch) {
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ResourceLocation tex = texture(chest);
        if (tex == null || p.isInvisible()) return;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));
        PlayerModel<AbstractClientPlayer> pm = getParentModel();
        ModelPart cape = root.getChild("cape");
        // balancement : la cape se souleve en marchant et se plaque en position accroupie
        float sway = Mth.cos(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;
        cape.xRot = 0.06F + limbSwingAmount * 0.45F + (p.isCrouching() ? 0.2F : 0.0F) + sway;
        ps.pushPose();
        pm.body.translateAndRotate(ps);
        cape.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        root.getChild("mantle").render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        root.getChild("clasp").render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        if (p.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            ps.pushPose();
            pm.head.translateAndRotate(ps);
            root.getChild("hood").render(ps, vc, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
    }
}
