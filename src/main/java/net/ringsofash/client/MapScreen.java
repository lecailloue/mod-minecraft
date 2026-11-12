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

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (texture == null) build();
        this.renderBackground(g, mx, my, pt);
        int s = Math.min(this.width, this.height) - 30;
        int x0 = (this.width - s) / 2, y0 = (this.height - s) / 2 + 6;
        g.fill(x0 - 3, y0 - 3, x0 + s + 3, y0 + s + 3, 0xFF6B5330);
        g.blit(texture, x0, y0, s, s, 0f, 0f, TEX, TEX, TEX, TEX);
        g.drawCenteredString(this.font, "§6§lCarte de la Hautemarche", this.width / 2, y0 - 12, 0xFFFFFF);
        // nord : le haut de la carte (rose des vents en haut a droite)
        int cx = x0 + s - 22, cy = y0 + 28;
        g.fill(cx - 15, cy - 20, cx + 16, cy + 21, 0xB0201A14);
        g.fill(cx, cy - 15, cx + 1, cy + 14, 0xFFE8D9A8);
        g.fill(cx - 14, cy, cx + 15, cy + 1, 0xFF8A7A50);
        g.fill(cx - 2, cy - 13, cx + 3, cy - 11, 0xFFE02020);
        g.fill(cx - 1, cy - 15, cx + 2, cy - 13, 0xFFE02020);
        g.drawCenteredString(this.font, "§c§lN", cx, cy - 31, 0xFFFFFF);
        g.drawString(this.font, "S", cx - 2, cy + 12, 0xFFCCBB99, false);
        g.drawString(this.font, "O", cx - 14, cy - 3, 0xFFCCBB99, false);
        g.drawString(this.font, "E", cx + 8, cy - 3, 0xFFCCBB99, false);
        g.drawCenteredString(this.font, "§7Nord en haut", this.width / 2, y0 + s + 20, 0xFFFFFF);
        double k = s / (double) MapData.SIZE;
        // lieux connus
        for (Map.Entry<String, int[]> e : MapData.places().entrySet()) {
            int px = x0 + (int) ((e.getValue()[0] + MapData.HALF) * k), py = y0 + (int) ((e.getValue()[1] + MapData.HALF) * k);
            g.fill(px - 1, py - 1, px + 2, py + 2, 0xFFFFD070);
            String name = e.getKey().replace('_', ' ');
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            g.drawString(this.font, name, px + 4, py - 4, 0xFFF0E0B0, true);
        }
        // joueur
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean here = mc.level != null && mc.level.dimension().location().getPath().equals("hautemarche");
            int bx = (int) mc.player.getX(), bz = (int) mc.player.getZ();
            if (here) {
                int px = x0 + (int) ((bx + MapData.HALF) * k), py = y0 + (int) ((bz + MapData.HALF) * k);
                g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFFFFFF);
                g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFE02020);
                String bn = Component.translatable("biome.ringsofash." + MapData.BIOMES[MapData.biome(bx, bz)]).getString();
                g.drawCenteredString(this.font, "Vous êtes ici : X " + bx + "  Z " + bz + "  —  " + bn, this.width / 2, y0 + s + 8, 0xFFFFFF);
            } else {
                g.drawCenteredString(this.font, "§7Vous n'êtes pas en Hautemarche", this.width / 2, y0 + s + 8, 0xFFFFFF);
            }
        }
        // survol
        if (mx >= x0 && mx < x0 + s && my >= y0 && my < y0 + s) {
            int wx = (int) ((mx - x0) / k) - MapData.HALF, wz = (int) ((my - y0) / k) - MapData.HALF;
            String bn = Component.translatable("biome.ringsofash." + MapData.BIOMES[MapData.biome(wx, wz)]).getString();
            g.drawString(this.font, "X " + wx + "  Z " + wz + "  " + bn, mx + 10, my + 10, 0xFFFFFF, true);
        }
    }
}
