package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.item.UdunFire;

/**
 * Les flammes rouges de la Flamme d'Udun sur une creature.
 *
 * Minecraft ne sait dessiner qu'un seul feu, le sien, et sa texture est commune a tout
 * le jeu. On eteint donc le feu ordinaire sur la creature (voir UdunFire) et on dessine
 * le notre par-dessus : deux panneaux croises tournes vers la camera, dans la texture
 * animee du bloc.
 */
public final class UdunFlameRender {
    private UdunFlameRender() {}

    private static final ResourceLocation TEX = RingsOfAsh.id("textures/block/udun_fire.png");
    private static final int FRAMES = 8;

    public static void init(IEventBus bus) { bus.addListener(UdunFlameRender::render); }

    private static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = e.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        PoseStack pose = e.getPoseStack();

        for (Entity any : mc.level.entitiesForRendering()) {
            if (!(any instanceof LivingEntity le) || !UdunFire.marked(le)) continue;
            if (le == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (vc == null) vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEX));
            draw(pose, vc, le, cam, e.getPartialTick().getGameTimeDeltaPartialTick(false), mc);
        }
        if (vc != null) buffers.endBatch(RenderType.entityTranslucentEmissive(TEX));
    }

    private static void draw(PoseStack pose, VertexConsumer vc, LivingEntity le, Vec3 cam, float pt, Minecraft mc) {
        double px = net.minecraft.util.Mth.lerp(pt, le.xo, le.getX()) - cam.x;
        double py = net.minecraft.util.Mth.lerp(pt, le.yo, le.getY()) - cam.y;
        double pz = net.minecraft.util.Mth.lerp(pt, le.zo, le.getZ()) - cam.z;

        float w = le.getBbWidth() * 1.45F;
        float h = le.getBbHeight() * 1.35F;
        // l'image courante de l'animation, dans une planche de huit
        int frame = (int) ((le.level().getGameTime() / 2) % FRAMES);
        float v0 = frame / (float) FRAMES, v1 = (frame + 1) / (float) FRAMES;

        pose.pushPose();
        pose.translate(px, py, pz);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));
        // deux panneaux croises : la flamme garde du corps quand on tourne autour
        for (int i = 0; i < 2; i++) {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(i * 90F));
            quad(pose, vc, w, h, v0, v1);
        }
        pose.popPose();
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float w, float h, float v0, float v1) {
        var m = pose.last();
        float x = w / 2F;
        vert(vc, m, -x, 0, 0F, v1);
        vert(vc, m, x, 0, 1F, v1);
        vert(vc, m, x, h, 1F, v0);
        vert(vc, m, -x, h, 0F, v0);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose m, float x, float y, float u, float v) {
        vc.addVertex(m, x, y, 0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(m, 0F, 1F, 0F);
    }
}
