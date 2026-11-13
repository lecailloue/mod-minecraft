package net.ringsofash.client;

import net.minecraft.client.Minecraft;
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

    private Atmosphere() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(Atmosphere::gui);
        NeoForge.EVENT_BUS.addListener(Atmosphere::tick);
        NeoForge.EVENT_BUS.addListener(Atmosphere::fog);
        NeoForge.EVENT_BUS.addListener(Atmosphere::fogColor);
    }

    private static float amount(float pt) { return Mth.lerp(pt, prev, cur); }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        prev = cur;
        boolean in = false;
        if (mc.level != null && mc.player != null) {
            in = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                    .map(k -> k.location().getNamespace().equals(RingsOfAsh.MODID) && ASH.contains(k.location().getPath())).orElse(false);
        }
        cur = Mth.clamp(cur + (in ? 0.012F : -0.02F), 0F, 1F);
    }

    private static void fog(ViewportEvent.RenderFog e) {
        float a = amount((float) e.getPartialTick());
        if (a <= 0.001F || e.getCamera().getFluidInCamera() != FogType.NONE) return;
        e.setNearPlaneDistance(Mth.lerp(a, e.getNearPlaneDistance(), 90F));
        e.setFarPlaneDistance(Mth.lerp(a, e.getFarPlaneDistance(), 300F));
        e.setCanceled(true);
    }

    private static void fogColor(ViewportEvent.ComputeFogColor e) {
        float a = amount((float) e.getPartialTick());
        if (a <= 0.001F) return;
        e.setRed(Mth.lerp(a, e.getRed(), 0.55F));
        e.setGreen(Mth.lerp(a, e.getGreen(), 0.20F));
        e.setBlue(Mth.lerp(a, e.getBlue(), 0.06F));
    }

    private static void gui(RegisterGuiLayersEvent e) {
        e.registerBelowAll(RingsOfAsh.id("ash_dusk"), (g, dt) -> {
            float a = amount(dt.getGameTimeDeltaPartialTick(false));
            if (a <= 0.01F) return;
            int alpha = Mth.clamp(Math.round(a * 0.30F * 255F), 0, 255);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0x1A0502);
            // ciel noir -> orange a l'horizon, dessine a l'ecran : fonctionne aussi avec les shaders
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                double fov = Math.toRadians(mc.options.fov().get());
                double pitch = Math.toRadians(mc.player.getViewXRot(dt.getGameTimeDeltaPartialTick(false)));
                int h = g.guiHeight();
                int hy = (int) Mth.clamp(h / 2.0 * (1 + Math.tan(pitch) / Math.tan(fov / 2)), 0, h);
                if (hy > 2) {
                    int top = Mth.clamp(Math.round(a * 0.62F * 255F), 0, 255);
                    int bot = Mth.clamp(Math.round(a * 0.34F * 255F), 0, 255);
                    g.fillGradient(0, 0, g.guiWidth(), hy, top << 24 | 0x080201, bot << 24 | 0xFF5A14);
                }
            }
            int glow = Mth.clamp(Math.round(a * 0.07F * 255F), 0, 255);          // legere lueur orangee
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), glow << 24 | 0xFF6A1E);
        });
    }
}
