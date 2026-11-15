package net.ringsofash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.ringsofash.world.MapData;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.RingsOfAsh;

import java.util.Set;

/** Terres de Cendre et volcan : ciel rouge sombre, brouillard epais et lumiere de quasi-nuit (transition douce). */
public final class Atmosphere {
    private static final Set<String> ASH = Set.of("plaines_cendre", "monts_de_cendre", "rivieres_de_lave", "volcan");
    private static float prev = 0, cur = 0;
    /** lueur verte de Minas Morgul : 1 pres du chateau, 0 loin (transition douce) */
    private static float gPrev = 0, gCur = 0;
    private static final DustParticleOptions GREEN_MOTE = new DustParticleOptions(new Vector3f(0.30F, 1.0F, 0.45F), 1.6F);
    private static final DustParticleOptions GREEN_HAZE = new DustParticleOptions(new Vector3f(0.12F, 0.8F, 0.35F), 3.2F);

    private Atmosphere() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(Atmosphere::gui);
        NeoForge.EVENT_BUS.addListener(Atmosphere::tick);
        NeoForge.EVENT_BUS.addListener(Atmosphere::fog);
        NeoForge.EVENT_BUS.addListener(Atmosphere::fogColor);
    }

    private static float amount(float pt) { return Mth.lerp(pt, prev, cur); }
    private static float glow(float pt) { return Mth.lerp(pt, gPrev, gCur); }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        prev = cur;
        boolean in = false;
        if (mc.level != null && mc.player != null) {
            in = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                    .map(k -> k.location().getNamespace().equals(RingsOfAsh.MODID) && ASH.contains(k.location().getPath())).orElse(false);
        }
        cur = Mth.clamp(cur + (in ? 0.012F : -0.02F), 0F, 1F);
        gPrev = gCur;
        float target = 0;
        int[] pl = MapData.place("minas_morgul");
        if (pl != null && mc.level != null && mc.player != null) {
            double dx = mc.player.getX() - pl[0], dz = mc.player.getZ() - pl[1];
            target = Mth.clamp((float) (1.0 - (Math.sqrt(dx * dx + dz * dz) - 70.0) / 130.0), 0F, 1F);
        }
        gCur += (target - gCur) * 0.04F;
        if (gCur < 0.002F) gCur = 0;
        if (gCur > 0.12F && mc.level != null && mc.player != null && !mc.isPaused()) {
            // brume et lucioles vertes qui montent autour du joueur : la lueur de la citadelle flotte dans l'air
            var r = mc.level.random;
            int n = 1 + (int) (gCur * 7);
            for (int i = 0; i < n; i++) {
                double x = mc.player.getX() + (r.nextDouble() - 0.5) * 56, y = mc.player.getY() + (r.nextDouble() - 0.35) * 24, z = mc.player.getZ() + (r.nextDouble() - 0.5) * 56;
                mc.level.addParticle(r.nextInt(5) == 0 ? GREEN_HAZE : GREEN_MOTE, x, y, z, (r.nextDouble() - 0.5) * 0.01, 0.012 + r.nextDouble() * 0.02, (r.nextDouble() - 0.5) * 0.01);
            }
        }
    }

    private static void fog(ViewportEvent.RenderFog e) {
        float a = amount((float) e.getPartialTick()), gl = glow((float) e.getPartialTick());
        if ((a <= 0.001F && gl <= 0.001F) || e.getCamera().getFluidInCamera() != FogType.NONE) return;
        float near = Mth.lerp(a, e.getNearPlaneDistance(), 90F), far = Mth.lerp(a, e.getFarPlaneDistance(), 300F);
        near = Mth.lerp(gl, near, 24F);
        far = Mth.lerp(gl * 0.85F, far, 190F);
        e.setNearPlaneDistance(near);
        e.setFarPlaneDistance(far);
        e.setCanceled(true);
    }

    private static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (e.getCamera().getBlockAtCamera().getFluidState().getType().isSame(net.ringsofash.fluid.ModFluids.ICHOR.get())) {   // plonge dans l'ichor : brouillard vert vif
            e.setRed(0.10F); e.setGreen(0.65F); e.setBlue(0.22F);
            return;
        }
        float a = amount((float) e.getPartialTick()), gl = glow((float) e.getPartialTick());
        if (a <= 0.001F && gl <= 0.001F) return;
        float r = Mth.lerp(a, e.getRed(), 0.42F), g = Mth.lerp(a, e.getGreen(), 0.15F), b = Mth.lerp(a, e.getBlue(), 0.06F);
        e.setRed(Mth.lerp(gl * 0.9F, r, 0.06F));
        e.setGreen(Mth.lerp(gl * 0.9F, g, 0.36F));
        e.setBlue(Mth.lerp(gl * 0.9F, b, 0.18F));
    }

    private static void gui(RegisterGuiLayersEvent e) {
        e.registerBelowAll(RingsOfAsh.id("ash_dusk"), (g, dt) -> {
            float pt = dt.getGameTimeDeltaPartialTick(false);
            float a = amount(pt), gl = glow(pt);
            if (a > 0.01F) {
                int alpha = Mth.clamp(Math.round(a * 0.36F * 255F), 0, 255);
                g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0x1A0502);
            }
            if (gl > 0.01F) {
                int alpha = Mth.clamp(Math.round(gl * 0.20F * 255F), 0, 255);
                g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0x0C4A24);
            }
        });
    }
}
