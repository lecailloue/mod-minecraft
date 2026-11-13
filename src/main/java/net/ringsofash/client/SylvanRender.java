package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ringsofash.data.Net;
import net.ringsofash.entity.TreemanEntity;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.SylvanHeart;

/** Rendu de la forme de Sylvgarde (joueur), des mains-branches en vue subjective, et du Sylvgarde fige en arbre. */
public final class SylvanRender {
    private SylvanRender() {}

    private static GeoModel<Player> model;

    private static GeoModel<Player> model() {
        if (model == null) model = new GeoModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer("geo_treeman")), "treeman");
        return model;
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SylvanRender::renderPlayer);
        NeoForge.EVENT_BUS.addListener(SylvanRender::renderHand);
        NeoForge.EVENT_BUS.addListener(SylvanRender::clickEmpty);
        NeoForge.EVENT_BUS.addListener(SylvanRender::clickItem);
        NeoForge.EVENT_BUS.addListener(SylvanRender::clickBlock);
    }

    // ---------- clic droit = racines
    private static long lastCast;

    private static void cast(Player p) {
        if (!p.level().isClientSide || p != Minecraft.getInstance().player || !SylvanHeart.isSylvan(p)) return;
        long now = p.level().getGameTime();
        if (now - lastCast < 5) return;                         // un seul envoi par clic (les deux mains declenchent l'evenement)
        lastCast = now;
        PacketDistributor.sendToServer(new Net.CastRoots());
        p.swing(InteractionHand.MAIN_HAND);
    }

    private static void clickEmpty(PlayerInteractEvent.RightClickEmpty e) { cast(e.getEntity()); }
    private static void clickItem(PlayerInteractEvent.RightClickItem e) { if (SylvanHeart.isSylvan(e.getEntity())) { cast(e.getEntity()); e.setCanceled(true); } }
    private static void clickBlock(PlayerInteractEvent.RightClickBlock e) { if (SylvanHeart.isSylvan(e.getEntity())) { cast(e.getEntity()); e.setCanceled(true); } }

    // ---------- le joueur devient un Sylvgarde (vue a la troisieme personne et pour les autres joueurs)
    private static void renderPlayer(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        if (!SylvanHeart.isSylvan(p)) return;
        e.setCanceled(true);
        GeoModel<Player> m = model();
        float pt = e.getPartialTick();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        float body = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot), head = Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - body));
        ps.scale(-1.0F, -1.0F, 1.0F);
        float s = 0.52F * p.getBbHeight() / 5.0F;              // le modele remplit la hauteur du joueur agrandi
        ps.scale(s, s, s);
        ps.translate(0.0F, -1.501F, 0.0F);
        m.attackTime = p.getAttackAnim(pt);
        m.riding = p.isPassenger();
        m.young = false;
        float swing = p.walkAnimation.position(pt), amount = Math.min(1.0F, p.walkAnimation.speed(pt));
        m.setupAnim(p, swing, amount, p.tickCount + pt, Mth.wrapDegrees(head - body), p.getViewXRot(pt));
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(Renderers.tex("treeman")));
        m.renderToBuffer(ps, vc, e.getPackedLight(), LivingEntityRenderer.getOverlayCoords(p, 0.0F), -1);
        ps.popPose();
    }

    // ---------- vue subjective : deux bras-branches a la place des mains
    private static void renderHand(RenderHandEvent e) {
        Player p = Minecraft.getInstance().player;
        if (p == null || !SylvanHeart.isSylvan(p)) return;
        e.setCanceled(true);
        if (e.getHand() != InteractionHand.MAIN_HAND) return;  // les deux bras sont dessines une seule fois
        GeoModel<Player> m = model();
        m.root().getAllParts().forEach(ModelPart::resetPose);
        float age = p.tickCount + e.getPartialTick();
        float swing = e.getSwingProgress();
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(Renderers.tex("treeman")));
        arm(m.part("arm_r"), e.getPoseStack(), vc, e.getPackedLight(), 1, swing, age);
        arm(m.part("arm_l"), e.getPoseStack(), vc, e.getPackedLight(), -1, 0, age + 20);
    }

    private static void arm(ModelPart arm, PoseStack ps, VertexConsumer vc, int light, int side, float swing, float age) {
        if (arm == null) return;
        float ox = arm.x, oy = arm.y, oz = arm.z, rx = arm.xRot, ry = arm.yRot, rz = arm.zRot;
        arm.x = arm.y = arm.z = 0;
        arm.xRot = arm.yRot = arm.zRot = 0;
        float hit = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        float bob = Mth.sin(age * 0.08F) * 0.015F;
        ps.pushPose();
        ps.translate(side * 0.62F, -0.98F + bob + hit * 0.15F, -0.3F - hit * 0.35F);
        ps.mulPose(Axis.XP.rotationDegrees(117.0F - hit * 40.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(side * (6.0F + hit * 10.0F)));
        float s = 0.0176F;
        ps.scale(-s, -s, s);
        arm.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        arm.x = ox; arm.y = oy; arm.z = oz; arm.xRot = rx; arm.yRot = ry; arm.zRot = rz;
    }

    // ---------- le Sylvgarde fige en arbre : tronc et feuillage
    public static class TreemanRenderer extends Renderers.Geo<TreemanEntity> {
        public TreemanRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, ClientSetup.layer("geo_treeman"), "treeman", 1.4F, 0.52F, f -> "treeman", null);
        }

        @Override
        public void render(TreemanEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            if (!e.isTree()) { super.render(e, yaw, pt, ps, buf, light); return; }
            var br = Minecraft.getInstance().getBlockRenderer();
            BlockState log = Reg.ANCIENT_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState leaves = Reg.ANCIENT_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            int seed = e.getId();
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
            ps.translate(-0.5, 0, -0.5);
            for (int y = 0; y < 5; y++) block(br, ps, buf, light, log, 0, y, 0);
            block(br, ps, buf, light, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 1, 0, 0);
            block(br, ps, buf, light, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 0, 0, -1);
            for (int dx = -2; dx <= 2; dx++) for (int dy = 3; dy <= 6; dy++) for (int dz = -2; dz <= 2; dz++) {
                double d = dx * dx / 5.0 + (dy - 4.6) * (dy - 4.6) / 3.2 + dz * dz / 5.0;
                if (d > 1.0 || (dx == 0 && dz == 0 && dy < 5)) continue;
                if (d > 0.7 && ((dx * 31 + dy * 17 + dz * 7 + seed) & 3) == 0) continue;
                block(br, ps, buf, light, leaves, dx, dy, dz);
            }
            ps.popPose();
        }

        private static void block(net.minecraft.client.renderer.block.BlockRenderDispatcher br, PoseStack ps, MultiBufferSource buf, int light, BlockState s, int x, int y, int z) {
            ps.pushPose();
            ps.translate(x, y, z);
            br.renderSingleBlock(s, ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
    }
}
