package net.ringsofash.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.data.Net;
import net.ringsofash.item.RingItem;
import net.ringsofash.world.EyeOfSauron;
import net.ringsofash.world.ModWorldgen;

/**
 * L'Oeil de Sauron au sommet de Barad-dur, dessine par le client a une position fixe (envoyee par le serveur) : il ne depend d'aucun chunk charge,
 * donc il est toujours la dans la dimension. Il n'apparait que si le joueur porte l'Anneau Unique (actif) et que sa corruption atteint 35 %.
 * Tres loin, on le dessine plus pres de la camera (meme direction, meme taille apparente) pour qu'il ne sorte pas du champ de vue ; on coupe le
 * brouillard pour qu'il reste net, et deux passes additives le rendent brillant.
 */
public final class EyeOfSauronSky {
    private EyeOfSauronSky() {}

    /** pour les tests : montre l'oeil sans anneau ni corruption */
    public static boolean force;

    public static void init() { NeoForge.EVENT_BUS.addListener(EyeOfSauronSky::render); }

    public static boolean visible(Minecraft mc) {
        if (mc.level == null || mc.player == null || Net.ClientLore.eyeY < 0 || mc.level.dimension() != ModWorldgen.LANDS) return false;
        if (force) return true;
        return RingItem.worn(mc.player) == RingItem.Kind.ASH && Net.ClientLore.corruption >= EyeOfSauron.MIN_CORRUPTION;
    }

    private static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        Vec3 cam = e.getCamera().getPosition();
        double rx = Net.ClientLore.eyeX + 0.5 - cam.x, ry = Net.ClientLore.eyeY - cam.y, rz = Net.ClientLore.eyeZ + 0.5 - cam.z;
        double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (dist < 1.0) return;
        double maxD = Math.min(300.0, mc.options.getEffectiveRenderDistance() * 16.0 * 2.5);
        float mul = 1F;
        if (dist > maxD) {
            double k = maxD / dist;
            rx *= k; ry *= k; rz *= k;
            mul = (float) (k * Mth.clamp(dist / 700.0, 1.0, 5.0));        // meme taille apparente, un peu gonflee de tres loin
        }
        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        mv.mul(e.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        float fs = RenderSystem.getShaderFogStart(), fe = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(1.0E6F);
        RenderSystem.setShaderFogEnd(2.0E6F);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        ps.translate(rx, ry, rz);
        EyeOfSauronRenderer.draw(ps, buf, -rx, -ry, -rz, (System.nanoTime() / 1_000_000L) / 1000F, 0x5AL * 7919L, mul, true);
        buf.endBatch();
        RenderSystem.setShaderFogStart(fs);
        RenderSystem.setShaderFogEnd(fe);
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
