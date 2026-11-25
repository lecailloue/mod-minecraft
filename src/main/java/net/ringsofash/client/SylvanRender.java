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
import net.ringsofash.RingsOfAsh;
import net.ringsofash.data.Net;
import net.ringsofash.entity.TreemanEntity;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.SylvanHeart;

/** Rendu de la forme de Sylvgarde (joueur), des mains-branches en vue subjective, et du Sylvgarde fige en arbre. */
public final class SylvanRender {
    private SylvanRender() {}

    private static GeoModel<Player> model;
    private static int dbgHand;
    /** taille des bras-branches en vue subjective (essais) */
    public static float ARM_SCALE = 0.20F;
    public static boolean DBG_SHOW;

    private static GeoModel<Player> model() {
        if (model == null) model = new GeoModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer("geo_treeman")), "treeman");
        return model;
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SylvanRender::renderPlayer);
        NeoForge.EVENT_BUS.addListener(SylvanRender::renderRoots);
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
        m.renderToBuffer(ps, e.getMultiBufferSource().getBuffer(RenderType.eyes(Renderers.tex("treeman_glow"))), 0xF00000, OverlayTexture.NO_OVERLAY, -1);
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
        if (dbgHand < 4) {
            dbgHand++;
            net.ringsofash.RingsOfAsh.LOGGER.info("[SYL] renderHand main=" + e.getHand() + " armR=" + (m.part("arm_r") != null) + " armL=" + (m.part("arm_l") != null));
        }
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
        ps.translate(side * 0.46F, -0.62F + bob + hit * 0.18F, -0.48F - hit * 0.35F);
        ps.mulPose(Axis.XP.rotationDegrees(104.0F - hit * 40.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(side * (8.0F + hit * 12.0F)));
        ps.mulPose(Axis.YP.rotationDegrees(side * 8.0F));
        float s = ARM_SCALE;
        ps.scale(-s, -s, s);
        arm.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        arm.x = ox; arm.y = oy; arm.z = oz; arm.xRot = rx; arm.yRot = ry; arm.zRot = rz;
    }

    // ---------- racines : vraies racines en 3D autour de la creature prise
    private static GeoModel<net.minecraft.world.entity.LivingEntity> rootsModel;
    /** debut de l'enracinement vu par le client (pour l'animation de pousse) */
    private static final java.util.Map<Integer, Long> ROOT_START = new java.util.HashMap<>();
    private static int dbg, dbg2;

    private static GeoModel<net.minecraft.world.entity.LivingEntity> rootsModel() {
        if (rootsModel == null) rootsModel = new GeoModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer("geo_racines")), "racines");
        return rootsModel;
    }

    /** vrai si la creature porte les racines (les effets des mobs ne sont pas envoyes au client :
     *  on lit le modificateur d'attribut, lui, qui est synchronise) */
    private static boolean rooted(net.minecraft.world.entity.LivingEntity le) {
        var attr = le.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        return attr != null && attr.getModifier(net.ringsofash.RingsOfAsh.id("rooted_speed")) != null;
    }

    private static void renderRoots(net.neoforged.neoforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (dbg < 2) { dbg++; RingsOfAsh.LOGGER.info("[SYL] passe monde ok"); }
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        net.minecraft.world.phys.Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        net.minecraft.client.renderer.MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        GeoModel<net.minecraft.world.entity.LivingEntity> m = null;
        long now = mc.level.getGameTime();
        for (net.minecraft.world.entity.Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof net.minecraft.world.entity.LivingEntity le) || le.isInvisible()) continue;
            if (!rooted(le)) { ROOT_START.remove(le.getId()); continue; }
            if (m == null) {
                m = rootsModel();
                m.root().getAllParts().forEach(ModelPart::resetPose);
            }
            long start = ROOT_START.computeIfAbsent(le.getId(), k -> now);
            float grow = Mth.clamp((now - start + pt) / 5.0F, 0.2F, 1.0F);
            float blocks = Mth.clamp(le.getBbHeight() * 0.60F, 0.7F, 2.4F);
            float s = blocks * 16.0F / GeoDef.get("racines").height;
            double x = Mth.lerp(pt, le.xo, le.getX()) - cam.x;
            double y = Mth.lerp(pt, le.yo, le.getY()) - cam.y;
            double z = Mth.lerp(pt, le.zo, le.getZ()) - cam.z;
            ps.pushPose();
            ps.translate(x, y, z);
            ps.mulPose(Axis.YP.rotationDegrees((le.getId() * 61) % 360));
            ps.scale(-1.0F, -1.0F, 1.0F);
            ps.scale(s, s * grow, s);
            ps.translate(0.0F, -1.501F / grow, 0.0F);
            m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(Renderers.tex("racines"))),
                    mc.getEntityRenderDispatcher().getPackedLightCoords(le, pt), OverlayTexture.NO_OVERLAY, -1);
            ps.popPose();
            if (dbg2 < 4) { dbg2++; RingsOfAsh.LOGGER.info("[SYL] racines " + le.getType().getDescriptionId() + " s=" + s + " grow=" + grow + " pos=" + x + "," + y + "," + z + " cam=" + cam); }
        }
        if (DBG_SHOW && mc.player != null) {                      // essai : une cage devant le joueur
            if (m == null) { m = rootsModel(); m.root().getAllParts().forEach(ModelPart::resetPose); }
            double px = Mth.lerp(pt, mc.player.xo, mc.player.getX()) - cam.x;
            double py = Mth.lerp(pt, mc.player.yo, mc.player.getY()) - cam.y;
            double pz = Mth.lerp(pt, mc.player.zo, mc.player.getZ()) - cam.z;
            ps.pushPose();
            ps.translate(px, py, pz - 3.0);
            ps.scale(-1.0F, -1.0F, 1.0F);
            ps.scale(0.6F, 0.6F, 0.6F);
            ps.translate(0.0F, -1.501F, 0.0F);
            m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(Renderers.tex("racines"))), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
            ps.popPose();
        }
        buf.endBatch();
    }

    // ---------- le Sylvgarde fige en arbre : tronc et feuillage
    public static class TreemanRenderer extends Renderers.Geo<TreemanEntity> {
        /** modele, texture et yeux lumineux de chaque variante ; la forme d'arbre de nuit est la meme pour toutes */
        private static final String[] NAMES = {"treeman", "sylvestre_ancien", "sylvestre_saule"};
        /** hauteur visee a l'ecran (en blocs) */
        private static final float[] BLOCKS = {5.1F, 5.2F, 5.1F};
        private final GeoModel<TreemanEntity>[] models;
        private final float[] scales = new float[NAMES.length];
        private final net.minecraft.client.renderer.RenderType[] glows = new net.minecraft.client.renderer.RenderType[NAMES.length];
        private int cur;

        @SuppressWarnings("unchecked")
        public TreemanRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, ClientSetup.layer("geo_treeman"), "treeman", 1.4F, 0.52F, f -> NAMES[Mth.clamp(f.variant(), 0, 2)], null);
            models = new GeoModel[NAMES.length];
            for (int i = 0; i < NAMES.length; i++) {
                models[i] = new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[i])), NAMES[i]);
                scales[i] = i == 0 ? 0.52F : BLOCKS[i] * 16F / GeoDef.get(NAMES[i]).height;
                glows[i] = net.minecraft.client.renderer.RenderType.eyes(Renderers.tex(NAMES[i] + "_glow"));
            }
            addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<TreemanEntity, GeoModel<TreemanEntity>>(this) {
                @Override
                public void render(PoseStack ps, MultiBufferSource buf, int light, TreemanEntity e, float a, float b, float c, float d, float f, float g) {
                    if (e.isTree()) return;
                    getParentModel().renderToBuffer(ps, buf.getBuffer(glows[cur]), 0xF00000, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(TreemanEntity e, PoseStack ps, float pt) { float s = scales[cur]; ps.scale(s, s, s); }

        @Override
        public void render(TreemanEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            cur = Mth.clamp(e.variant(), 0, 2);
            this.model = models[cur];
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
