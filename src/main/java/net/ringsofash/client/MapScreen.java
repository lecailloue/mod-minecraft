package net.ringsofash.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.world.MapData;

import java.util.Map;

/** Carte geante de la Hautemarche (touche M) : relief, biomes, position du joueur. */
public class MapScreen extends Screen {
    private static final int TEX = 640;
    private static final int[] COLORS = {
            0xBEEB9E, 0xFFEB9E, 0x96938E, 0x2F7A3A, 0x808080, 0xFFFFFF, 0x2A5FB0, 0xF0E0A0, 0x4F94EB, 0x60504A, 0x6FAF55, 0x9FD680, 0xFF6A14};
    private static ResourceLocation texture;

    public MapScreen() { super(Component.literal("Carte de la Hautemarche")); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static void build() {
        NativeImage img = new NativeImage(TEX, TEX, false);
        double step = MapData.SIZE / (double) TEX;
        for (int py = 0; py < TEX; py++) {
            for (int px = 0; px < TEX; px++) {
                int x = (int) (px * step - MapData.HALF), z = (int) (py * step - MapData.HALF);
                int h = MapData.height(x, z), b = MapData.biome(x, z);
                int rgb = COLORS[Math.max(0, Math.min(COLORS.length - 1, b))];
                float shade = 1f;
                if (h >= MapData.SEA) {
                    int dx = MapData.height(x + 6, z) - MapData.height(x - 6, z), dz = MapData.height(x, z + 6) - MapData.height(x, z - 6);
                    shade = Math.max(0.55f, Math.min(1.35f, 0.95f + (dx + dz) * 0.012f));
                } else {
                    shade = 0.6f + 0.4f * Math.max(0, Math.min(1, (h + 20) / 80f));
                }
                int r = Math.min(255, (int) (((rgb >> 16) & 255) * shade)), g = Math.min(255, (int) (((rgb >> 8) & 255) * shade)), bl = Math.min(255, (int) ((rgb & 255) * shade));
                img.setPixelRGBA(px, py, 0xFF000000 | bl << 16 | g << 8 | r);
            }
        }
        texture = ResourceLocation.fromNamespaceAndPath(RingsOfAsh.MODID, "hautemarche_map");
        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(img));
    }

    private double zoom = 1, cx = 0, cz = 0;          // vue : facteur de zoom et centre (coordonnees monde)
    private int x0, y0, size;

    private double span() { return MapData.SIZE / zoom; }
    private double toScreenX(double wx) { return x0 + (wx - (cx - span() / 2)) * size / span(); }
    private double toScreenY(double wz) { return y0 + (wz - (cz - span() / 2)) * size / span(); }
    private double toWorldX(double sx) { return cx - span() / 2 + (sx - x0) * span() / size; }
    private double toWorldZ(double sy) { return cz - span() / 2 + (sy - y0) * span() / size; }

    private void clampView() {
        double h = span() / 2;
        cx = Math.max(-MapData.HALF + h, Math.min(MapData.HALF - h, cx));
        cz = Math.max(-MapData.HALF + h, Math.min(MapData.HALF - h, cz));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        double wx = toWorldX(mx), wz = toWorldZ(my);
        double nz = Math.max(1, Math.min(16, zoom * (sy > 0 ? 1.4 : 1 / 1.4)));
        if (nz == zoom) return true;
        boolean inside = mx >= x0 && mx < x0 + size && my >= y0 && my < y0 + size;
        zoom = nz;
        if (inside) {                                        // zoom centre sur le curseur
            cx = wx - (mx - x0 - size / 2.0) * span() / size;
            cz = wz - (my - y0 - size / 2.0) * span() / size;
        }
        clampView();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) { cx -= dx * span() / size; cz -= dy * span() / size; clampView(); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private Map.Entry<String, int[]> placeAt(double mx, double my) {
        Map.Entry<String, int[]> best = null;
        double bd = 8 * 8;
        for (Map.Entry<String, int[]> e : MapData.places().entrySet()) {
            double dx = toScreenX(e.getValue()[0]) - mx, dy = toScreenY(e.getValue()[1]) - my;
            if (dx * dx + dy * dy < bd) { bd = dx * dx + dy * dy; best = e; }
        }
        return best;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1) {
            Map.Entry<String, int[]> e = placeAt(mx, my);
            if (e != null) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ringsofash.data.Net.MapTeleport(e.getValue()[0], e.getValue()[1]));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static String pretty(String key) {
        String name = key.replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (texture == null) build();
        this.renderBackground(g, mx, my, pt);
        int s = Math.min(this.width, this.height) - 30;
        x0 = (this.width - s) / 2; y0 = (this.height - s) / 2 + 6; size = s;
        g.fill(x0 - 3, y0 - 3, x0 + s + 3, y0 + s + 3, 0xFF6B5330);
        double u = (cx - span() / 2 + MapData.HALF) / MapData.SIZE * TEX, v = (cz - span() / 2 + MapData.HALF) / MapData.SIZE * TEX;
        int uw = Math.max(1, (int) Math.round(span() / MapData.SIZE * TEX));
        g.blit(texture, x0, y0, s, s, (float) u, (float) v, uw, uw, TEX, TEX);
        g.drawCenteredString(this.font, "§6§lCarte de la Hautemarche", this.width / 2, y0 - 12, 0xFFFFFF);
        g.enableScissor(x0, y0, x0 + s, y0 + s);
        // lieux connus
        Map.Entry<String, int[]> hover = placeAt(mx, my);
        for (Map.Entry<String, int[]> e : MapData.places().entrySet()) {
            int px = (int) toScreenX(e.getValue()[0]), py = (int) toScreenY(e.getValue()[1]);
            if (px < x0 - 40 || px > x0 + s + 40 || py < y0 - 10 || py > y0 + s + 10) continue;
            boolean h = e == hover;
            g.fill(px - (h ? 2 : 1), py - (h ? 2 : 1), px + (h ? 3 : 2), py + (h ? 3 : 2), h ? 0xFFFFFFFF : 0xFFFFD070);
            g.drawString(this.font, pretty(e.getKey()), px + 4, py - 4, h ? 0xFFFFFFFF : 0xFFF0E0B0, true);
        }
        // joueur
        var mc = Minecraft.getInstance();
        boolean here = mc.player != null && mc.level != null && mc.level.dimension().location().getPath().equals("hautemarche");
        if (here) {
            int px = (int) toScreenX(mc.player.getX()), py = (int) toScreenY(mc.player.getZ());
            g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFFFFFF);
            g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFE02020);
        }
        g.disableScissor();
        // nord : le haut de la carte (rose des vents en haut a droite)
        int rx = x0 + s - 22, ry = y0 + 28;
        g.fill(rx - 15, ry - 20, rx + 16, ry + 21, 0xB0201A14);
        g.fill(rx, ry - 15, rx + 1, ry + 14, 0xFFE8D9A8);
        g.fill(rx - 14, ry, rx + 15, ry + 1, 0xFF8A7A50);
        g.fill(rx - 2, ry - 13, rx + 3, ry - 11, 0xFFE02020);
        g.fill(rx - 1, ry - 15, rx + 2, ry - 13, 0xFFE02020);
        g.drawCenteredString(this.font, "§c§lN", rx, ry - 31, 0xFFFFFF);
        g.drawString(this.font, "S", rx - 2, ry + 12, 0xFFCCBB99, false);
        g.drawString(this.font, "O", rx - 14, ry - 3, 0xFFCCBB99, false);
        g.drawString(this.font, "E", rx + 8, ry - 3, 0xFFCCBB99, false);
        g.drawCenteredString(this.font, "§7Molette : zoom (x" + String.format("%.1f", zoom) + ")  —  glisser : déplacer  —  clic droit sur un lieu : s'y téléporter", this.width / 2, y0 + s + 20, 0xFFFFFF);
        if (mc.player != null) {
            int bx = (int) mc.player.getX(), bz = (int) mc.player.getZ();
            if (here) {
                String bn = Component.translatable("biome.ringsofash." + MapData.BIOMES[MapData.biome(bx, bz)]).getString();
                g.drawCenteredString(this.font, "Vous êtes ici : X " + bx + "  Z " + bz + "  —  " + bn, this.width / 2, y0 + s + 8, 0xFFFFFF);
            } else {
                g.drawCenteredString(this.font, "§7Vous n'êtes pas en Hautemarche", this.width / 2, y0 + s + 8, 0xFFFFFF);
            }
        }
        // survol
        if (mx >= x0 && mx < x0 + s && my >= y0 && my < y0 + s) {
            int wx = (int) toWorldX(mx), wz = (int) toWorldZ(my);
            String bn = Component.translatable("biome.ringsofash." + MapData.BIOMES[MapData.biome(wx, wz)]).getString();
            String txt = hover != null ? "§e" + pretty(hover.getKey()) + " §7— clic droit pour s'y rendre" : "X " + wx + "  Z " + wz + "  " + bn;
            g.drawString(this.font, txt, mx + 10, my + 10, 0xFFFFFF, true);
        }
    }
}
