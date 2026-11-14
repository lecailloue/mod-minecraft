package net.ringsofash.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.world.MapData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Carte geante de la Hautemarche (touche M) : relief, biomes, lieux, repères personnels, position du joueur. */
public class MapScreen extends Screen {
    private static final int TEX = 640;
    private static final int[] COLORS = {
            0xBEEB9E, 0xFFEB9E, 0x96938E, 0x2F7A3A, 0x808080, 0xFFFFFF, 0x2A5FB0, 0xF0E0A0, 0x4F94EB, 0x60504A, 0x6FAF55, 0x9FD680, 0xFF6A14};
    private static ResourceLocation texture;
    /** Reperes poses par le joueur (coordonnees monde), enregistres dans ringsofash_markers.txt. */
    private static final List<int[]> MARKERS = new ArrayList<>();
    private static boolean markersLoaded;

    private double zoom = 1, cx = 0, cz = 0;          // vue : facteur de zoom et centre (coordonnees monde)
    private int x0, y0, size;                          // zone de la carte
    private int fx, fy, fw, fh;                        // cadre
    private boolean legend, placing;
    private double pressX, pressY;
    private boolean dragged;

    public MapScreen() { super(Component.literal("Carte de la Hautemarche")); }

    /** Variante pour les captures d'essai. */
    MapScreen(boolean legend, double zoom, boolean placing) {
        this();
        this.legend = legend; this.zoom = zoom; this.placing = placing;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    static void addMarker(int x, int z) { loadMarkers(); MARKERS.add(new int[]{x, z}); }

    private static Path markerFile() { return Minecraft.getInstance().gameDirectory.toPath().resolve("ringsofash_markers.txt"); }

    private static void loadMarkers() {
        if (markersLoaded) return;
        markersLoaded = true;
        try {
            Path f = markerFile();
            if (!Files.exists(f)) return;
            for (String line : Files.readAllLines(f)) {
                String[] p = line.trim().split("\\s+");
                if (p.length == 2) MARKERS.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])});
            }
        } catch (IOException | NumberFormatException ignored) { }
    }

    private static void saveMarkers() {
        try {
            List<String> lines = new ArrayList<>();
            for (int[] m : MARKERS) lines.add(m[0] + " " + m[1]);
            Files.write(markerFile(), lines);
        } catch (IOException ignored) { }
    }

    private static void build() {
        NativeImage img = new NativeImage(TEX, TEX, false);
        double step = MapData.SIZE / (double) TEX;
        for (int py = 0; py < TEX; py++) {
            for (int px = 0; px < TEX; px++) {
                int x = (int) (px * step - MapData.HALF), z = (int) (py * step - MapData.HALF);
                int h = MapData.height(x, z), b = MapData.biome(x, z);
                int rgb = COLORS[Math.max(0, Math.min(COLORS.length - 1, b))];
                float shade;
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

    private boolean playerHere() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.level.dimension().location().getPath().equals("hautemarche");
    }

    // ------------------------------------------------------------------ boutons
    private record Btn(int x, int y, int w, int h, int icon, String label, String tip, boolean active, Runnable act) {}

    private List<Btn> buttons() {
        List<Btn> l = new ArrayList<>();
        int bx = fx + fw + 4, by = fy + 14, st = 24;
        l.add(new Btn(bx, by, 20, 20, Ui.I_PLUS, null, "Zoom avant", false, () -> zoomBy(1.5)));
        l.add(new Btn(bx, by + st, 20, 20, Ui.I_MINUS, null, "Zoom arrière", false, () -> zoomBy(1 / 1.5)));
        l.add(new Btn(bx, by + st * 2, 20, 20, Ui.I_TARGET, null, "Centrer sur ma position", false, this::recenter));
        l.add(new Btn(bx, by + st * 3, 20, 20, Ui.I_PIN, null, placing ? "Repère : cliquez sur la carte (recliquez un repère pour l'effacer)" : "Poser / effacer un repère", placing,
                () -> placing = !placing));
        l.add(new Btn(bx, by + st * 4, 20, 20, Ui.I_SCROLL, null, "Journal de quêtes", false, () -> Minecraft.getInstance().setScreen(new QuestScreen(0))));
        l.add(new Btn(fx + fw - 24, fy + 3, 16, 10, Ui.I_CLOSE, null, "Fermer", false, this::onClose));
        String lg = "Légende";
        l.add(new Btn(fx + fw - 12 - font.width(lg) - 8, fy + fh - 17, font.width(lg) + 12, 13, -1, lg, "Afficher / masquer la légende", legend, () -> legend = !legend));
        return l;
    }

    private void zoomBy(double f) {
        zoom = Math.max(1, Math.min(16, zoom * f));
        clampView();
    }

    private void recenter() {
        Minecraft mc = Minecraft.getInstance();
        if (!playerHere() || mc.player == null) return;
        zoom = Math.max(zoom, 3);
        cx = mc.player.getX(); cz = mc.player.getZ();
        clampView();
    }

    // ------------------------------------------------------------------ souris
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (!Ui.in(mx, my, x0, y0, size, size)) return true;
        double wx = toWorldX(mx), wz = toWorldZ(my);
        double nz = Math.max(1, Math.min(16, zoom * (sy > 0 ? 1.4 : 1 / 1.4)));
        if (nz == zoom) return true;
        zoom = nz;
        cx = wx - (mx - x0 - size / 2.0) * span() / size;       // zoom centre sur le curseur
        cz = wz - (my - y0 - size / 2.0) * span() / size;
        clampView();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && Ui.in(pressX, pressY, x0, y0, size, size)) {
            if (Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) dragged = true;
            cx -= dx * span() / size; cz -= dy * span() / size; clampView();
            return true;
        }
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

    private int markerAt(double mx, double my) {
        for (int i = 0; i < MARKERS.size(); i++) {
            double dx = toScreenX(MARKERS.get(i)[0]) - mx, dy = toScreenY(MARKERS.get(i)[1]) - 4 - my;
            if (dx * dx + dy * dy < 7 * 7) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        pressX = mx; pressY = my; dragged = false;
        if (button == 0) {
            for (Btn b : buttons()) {
                if (Ui.in(mx, my, b.x, b.y, b.w, b.h)) { b.act.run(); return true; }
            }
        }
        if (button == 1 && Ui.in(mx, my, x0, y0, size, size)) {
            Map.Entry<String, int[]> e = placeAt(mx, my);
            if (e != null) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ringsofash.data.Net.MapTeleport(e.getValue()[0], e.getValue()[1]));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && placing && !dragged && Ui.in(mx, my, x0, y0, size, size) && Ui.in(pressX, pressY, x0, y0, size, size)) {
            loadMarkers();
            int i = markerAt(mx, my);
            if (i >= 0) MARKERS.remove(i);
            else MARKERS.add(new int[]{(int) toWorldX(mx), (int) toWorldZ(my)});
            saveMarkers();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientSetup.MAP_KEY.matches(key, scan)) { onClose(); return true; }
        if (key == InputConstants.KEY_ADD || key == InputConstants.KEY_EQUALS) { zoomBy(1.5); return true; }
        if (key == InputConstants.KEY_MINUS || key == InputConstants.KEY_SUBTRACT) { zoomBy(1 / 1.5); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private static String pretty(String key) {
        String name = key.replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ------------------------------------------------------------------ dessin
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (texture == null) build();
        loadMarkers();
        this.renderBackground(g, mx, my, pt);

        size = Math.max(120, Math.min(this.height - 44, this.width - 60));
        fw = size + 16; fh = size + 32;
        fx = (this.width - (fw + 28)) / 2; fy = (this.height - fh) / 2;
        x0 = fx + 8; y0 = fy + 15;

        // cadre et parchemin
        Ui.frame(g, fx, fy, fw, fh);
        Ui.parchment(g, fx + 4, fy + 4, fw - 8, fh - 8);
        g.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, Ui.BRONZE_D);
        g.fill(x0 - 1, y0 - 1, x0 + size + 1, y0 + size + 1, Ui.GOLD);

        double u = (cx - span() / 2 + MapData.HALF) / MapData.SIZE * TEX, v = (cz - span() / 2 + MapData.HALF) / MapData.SIZE * TEX;
        int uw = Math.max(1, (int) Math.round(span() / MapData.SIZE * TEX));
        g.blit(texture, x0, y0, size, size, (float) u, (float) v, uw, uw, TEX, TEX);

        g.enableScissor(x0, y0, x0 + size, y0 + size);
        Map.Entry<String, int[]> hover = Ui.in(mx, my, x0, y0, size, size) ? placeAt(mx, my) : null;
        for (Map.Entry<String, int[]> e : MapData.places().entrySet()) {
            int px = (int) toScreenX(e.getValue()[0]), py = (int) toScreenY(e.getValue()[1]);
            if (px < x0 - 40 || px > x0 + size + 40 || py < y0 - 10 || py > y0 + size + 10) continue;
            boolean h = e == hover;
            g.fill(px - (h ? 2 : 1), py - (h ? 2 : 1), px + (h ? 3 : 2), py + (h ? 3 : 2), h ? 0xFFFFFFFF : 0xFFFFD070);
            g.drawString(this.font, pretty(e.getKey()), px + 4, py - 4, h ? 0xFFFFFFFF : 0xFFF0E0B0, true);
        }
        for (int[] m : MARKERS) {
            int px = (int) toScreenX(m[0]), py = (int) toScreenY(m[1]);
            if (px < x0 - 8 || px > x0 + size + 8 || py < y0 - 8 || py > y0 + size + 16) continue;
            Ui.icon(g, Ui.I_PIN, px, py - 4, 0xFFE03030);
        }
        Minecraft mc = Minecraft.getInstance();
        boolean here = playerHere();
        if (here && mc.player != null) {
            int px = (int) toScreenX(mc.player.getX()), py = (int) toScreenY(mc.player.getZ());
            g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFFFFFF);
            g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFE02020);
        }
        // boussole : le nord est en haut
        int ccx = x0 + 22, ccy = y0 + 22;
        Ui.disc(g, ccx, ccy, 17, Ui.BRONZE_D);
        Ui.disc(g, ccx, ccy, 16, Ui.GOLD);
        Ui.disc(g, ccx, ccy, 14, 0xE01C140A);
        g.fill(ccx, ccy - 9, ccx + 1, ccy + 10, 0xFF8A7A50);
        g.fill(ccx - 9, ccy, ccx + 10, ccy + 1, 0xFF8A7A50);
        g.fill(ccx - 1, ccy - 6, ccx + 2, ccy + 1, 0xFFE05030);
        g.drawString(this.font, "N", ccx - 2, ccy - 13, 0xFFFFD070, false);
        g.drawString(this.font, "S", ccx - 2, ccy + 6, 0xFFCCBB99, false);
        g.drawString(this.font, "O", ccx - 13, ccy - 3, 0xFFCCBB99, false);
        g.drawString(this.font, "E", ccx + 8, ccy - 3, 0xFFCCBB99, false);
        if (legend) drawLegend(g);
        g.disableScissor();

        // bandeau haut : titre et embleme
        g.drawString(this.font, "Carte de la Hautemarche", fx + 12, fy + 5, Ui.INK, false);
        int ex = fx + fw / 2;
        Ui.disc(g, ex, fy + 4, 9, Ui.BRONZE_D);
        Ui.disc(g, ex, fy + 4, 8, Ui.GOLD);
        Ui.disc(g, ex, fy + 4, 6, Ui.DARK);
        Ui.tree(g, ex, fy + 3, 0xFFF4F0E4);

        // bandeau bas : banniere, coordonnees
        int by = fy + fh - 19;
        g.fill(fx + 11, by - 1, fx + 21, by + 12, Ui.GOLD);
        g.fill(fx + 12, by, fx + 20, by + 11, 0xFF7A1F1F);
        g.fill(fx + 15, by + 3, fx + 17, by + 7, 0xFFEBD28A);
        g.fill(fx + 12, by + 9, fx + 20, by + 11, Ui.PARCH);
        g.fill(fx + 12, by + 9, fx + 16, by + 10, 0xFF7A1F1F); g.fill(fx + 16, by + 9, fx + 20, by + 10, 0xFF7A1F1F);
        String coords;
        if (Ui.in(mx, my, x0, y0, size, size)) {
            int wx = (int) toWorldX(mx), wz = (int) toWorldZ(my);
            coords = "X: " + wx + "   Z: " + wz + "   " + Component.translatable("biome.ringsofash." + MapData.BIOMES[MapData.biome(wx, wz)]).getString();
        } else if (here && mc.player != null) {
            coords = "X: " + (int) mc.player.getX() + "   Z: " + (int) mc.player.getZ();
        } else {
            coords = "Vous n'êtes pas en Hautemarche";
        }
        g.drawString(this.font, coords, fx + 26, by + 2, Ui.INK, false);

        // boutons
        Btn tip = null;
        for (Btn b : buttons()) {
            boolean hv = Ui.in(mx, my, b.x, b.y, b.w, b.h);
            Ui.button(g, b.x, b.y, b.w, b.h, hv, b.active, true);
            if (b.icon >= 0) Ui.icon(g, b.icon, b.x + b.w / 2, b.y + b.h / 2, b.active ? 0xFFFFF0B0 : Ui.GOLD_L);
            else g.drawCenteredString(this.font, b.label, b.x + b.w / 2, b.y + (b.h - 8) / 2 + 1, 0xFFF0E0B0);
            if (hv) tip = b;
        }
        if (tip != null) g.renderTooltip(this.font, Component.literal(tip.tip), mx, my);
        else if (hover != null) g.renderTooltip(this.font, Component.literal(pretty(hover.getKey()) + " — clic droit pour s'y rendre"), mx, my);
        else if (placing && Ui.in(mx, my, x0, y0, size, size)) g.renderTooltip(this.font, Component.literal(markerAt(mx, my) >= 0 ? "Effacer ce repère" : "Poser un repère"), mx, my);
    }

    private void drawLegend(GuiGraphics g) {
        int n = MapData.BIOMES.length;
        int rows = Math.min(n, COLORS.length);
        int lw = 0;
        String[] names = new String[rows];
        for (int i = 0; i < rows; i++) {
            names[i] = Component.translatable("biome.ringsofash." + MapData.BIOMES[i]).getString();
            lw = Math.max(lw, this.font.width(names[i]));
        }
        int w = lw + 22, h = rows * 10 + 14;
        int lx = x0 + size - w - 4, ly = y0 + 4;
        g.fill(lx - 1, ly - 1, lx + w + 1, ly + h + 1, Ui.GOLD);
        g.fill(lx, ly, lx + w, ly + h, 0xE81C140A);
        g.drawString(this.font, "Légende", lx + 4, ly + 3, Ui.GOLD_L, false);
        for (int i = 0; i < rows; i++) {
            int yy = ly + 14 + i * 10;
            g.fill(lx + 4, yy, lx + 14, yy + 7, 0xFF000000 | COLORS[i]);
            g.drawString(this.font, names[i], lx + 18, yy, 0xFFE8DCC0, false);
        }
    }
}
