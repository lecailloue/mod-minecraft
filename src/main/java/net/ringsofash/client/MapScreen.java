package net.ringsofash.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.world.MapData;
import net.ringsofash.world.Towns;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Carte geante de la Hautemarche (touche M) : relief, biomes, lieux, repères personnels, position du joueur. */
public class MapScreen extends Screen {
    static final int TEX = 640;
    private static final int[] COLORS = {
            0x7FA850, 0xA9A858, 0x6E6A66, 0x4C7A3A, 0x7E7E7E, 0xE8ECEF, 0x33457A, 0xD4C48E, 0x3C5BA8, 0x4E4440, 0x6B9448, 0x74A050, 0xD4541A};
    private static ResourceLocation texture;
    /** Reperes poses par le joueur (coordonnees monde), enregistres dans ringsofash_markers.txt. */
    public static final class Marker {
        public int x, z, color;
        public String name;
        Marker(int x, int z, int color, String name) { this.x = x; this.z = z; this.color = color; this.name = name; }
        public String label() { return name.isEmpty() ? "Repère" : name; }
    }
    private static final List<Marker> MARKERS = new ArrayList<>();
    private static boolean markersLoaded;
    /** Couleurs proposees pour un repere. */
    private static final int[] PALETTE = {0xE03030, 0xF08A20, 0xF0D030, 0x40C040, 0x30C8D0, 0x3060E0, 0xA040E0, 0xF0F0F0};

    private double zoom = 1, cx = 0, cz = 0;          // vue : facteur de zoom et centre (coordonnees monde)
    private int x0, y0, size;                          // zone de la carte
    private int fx, fy, fw, fh;                        // cadre
    private boolean legend, placing;
    /** Edition d'un repere : index (-1 = aucune), champ du nom, position de la fenetre. */
    private int editing = -1, editInit = -1;
    private EditBox nameBox;
    private int ex0, ey0;
    private static final int EW = 158, EH = 86;
    private double pressX, pressY;
    private boolean dragged;

    public MapScreen() { super(Component.literal("Carte de la Hautemarche")); }

    /** Variante pour les captures d'essai. */
    MapScreen(boolean legend, double zoom, boolean placing) {
        this();
        this.legend = legend; this.zoom = zoom; this.placing = placing;
    }

    MapScreen(boolean legend, double zoom, boolean placing, int edit) {
        this(legend, zoom, placing);
        this.editInit = edit;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    static void addMarker(int x, int z) { addMarker(x, z, "", PALETTE[0]); }

    static void addMarker(int x, int z, String name, int color) { loadMarkers(); MARKERS.add(new Marker(x, z, color, name)); }

    /** Les reperes du joueur (pour la boussole du haut de l'ecran). */
    public static List<Marker> markers() { loadMarkers(); return MARKERS; }

    /** le joueur tient une arme a rune des Hobbits et porte le guide : la carte s'ouvre en entier */
    static boolean memory() {
        var p = Minecraft.getInstance().player;
        if (p == null) return false;
        var w = p.getMainHandItem();
        if (!net.ringsofash.item.Runes.runic(w) || !net.ringsofash.item.Rune.on(w).contains(net.ringsofash.item.Rune.HOBBIT)) return false;
        for (var s : p.getInventory().items) if (net.ringsofash.data.Guide.isGuide(s)) return true;
        return false;
    }

    /** Vrai si le joueur est en Hautemarche (la carte et les reperes n'ont de sens que la). */
    public static boolean inLands() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.level.dimension().location().getPath().equals("hautemarche");
    }

    private static Path markerFile() { return Minecraft.getInstance().gameDirectory.toPath().resolve("ringsofash_markers.txt"); }

    private static void loadMarkers() {
        if (markersLoaded) return;
        markersLoaded = true;
        try {
            Path f = markerFile();
            if (!Files.exists(f)) return;
            for (String line : Files.readAllLines(f)) {
                String[] p = line.trim().split("\\s+", 4);
                if (p.length >= 2) MARKERS.add(new Marker(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                        p.length >= 3 ? Integer.parseInt(p[2], 16) & 0xFFFFFF : PALETTE[0], p.length >= 4 ? p[3] : ""));
            }
        } catch (IOException | NumberFormatException ignored) { }
    }

    private static void saveMarkers() {
        try {
            List<String> lines = new ArrayList<>();
            for (Marker m : MARKERS) lines.add(m.x + " " + m.z + " " + String.format("%06X", m.color) + (m.name.isEmpty() ? "" : " " + m.name));
            Files.write(markerFile(), lines);
        } catch (IOException ignored) { }
    }

    /** le vide : ce qu'on n'a pas encore vu */
    private static final int BLANK = 0xFFF4F2EA;

    private static NativeImage image;
    private static DynamicTexture dynamic;

    /**
     * La carte part blanche et ne montre que ce que le joueur a parcouru (voir MapMemory).
     * On repeint un chunk des qu'il change, donc une maison batie finit par apparaitre.
     */
    private static void build() {
        image = new NativeImage(TEX, TEX, false);
        if (MapMemory.revealed()) generated(); else image.fillRect(0, 0, TEX, TEX, abgr(BLANK));
        for (Map.Entry<Long, int[]> e : MapMemory.all()) paint(e.getKey(), e.getValue());
        texture = ResourceLocation.fromNamespaceAndPath(RingsOfAsh.MODID, "hautemarche_map");
        dynamic = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(texture, dynamic);
    }

    /** /anneaux map : ce que le joueur n'a pas vu est comble par le relief du generateur */
    private static void generated() {
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
                int r = Math.min(255, (int) (((rgb >> 16) & 255) * shade));
                int gg = Math.min(255, (int) (((rgb >> 8) & 255) * shade));
                int bl = Math.min(255, (int) ((rgb & 255) * shade));
                image.setPixelRGBA(px, py, 0xFF000000 | bl << 16 | gg << 8 | r);
            }
        }
    }

    /** la texture de la carte, construite si besoin : la fenetre de ville s'en sert aussi */
    static ResourceLocation ensureTexture() {
        if (texture == null) build();
        return texture;
    }

    /** a refaire : on a change de monde, ou relu un fichier de decouverte */
    public static void forget() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(texture);
        texture = null;
        image = null;
        dynamic = null;
    }

    /** NativeImage range ses pixels a l'envers */
    private static int abgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    private static int toPixel(int world) {
        return (int) Math.floor((world + MapData.HALF) / (double) MapData.SIZE * TEX);
    }

    private static void paint(long key, int[] quarters) {
        if (image == null || quarters == null) return;
        int wx = net.minecraft.world.level.ChunkPos.getX(key) << 4, wz = net.minecraft.world.level.ChunkPos.getZ(key) << 4;
        for (int q = 0; q < 4; q++) {
            if (quarters[q] == 0) continue;
            int ox = (q & 1) * 8, oz = (q >> 1) * 8;
            int px0 = toPixel(wx + ox), px1 = Math.max(px0 + 1, toPixel(wx + ox + 8));
            int py0 = toPixel(wz + oz), py1 = Math.max(py0 + 1, toPixel(wz + oz + 8));
            int col = abgr(quarters[q]);
            for (int y = Math.max(0, py0); y < Math.min(TEX, py1); y++)
                for (int x = Math.max(0, px0); x < Math.min(TEX, px1); x++)
                    image.setPixelRGBA(x, y, col);
        }
    }

    /** on prend les nouveautes relevees depuis la derniere image */
    private static void refresh() {
        if (image == null) return;
        boolean any = false;
        for (int i = 0; i < 4096; i++) {
            Long key = MapMemory.nextFresh();
            if (key == null) break;
            paint(key, MapMemory.color(key));
            any = true;
        }
        if (any && dynamic != null) dynamic.upload();
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

    private boolean playerHere() { return inLands(); }

    // ------------------------------------------------------------------ boutons
    private record Btn(int x, int y, int w, int h, int icon, String label, String tip, boolean active, Runnable act) {}

    private List<Btn> buttons() {
        List<Btn> l = new ArrayList<>();
        int bx = fx + fw + 4, by = fy + 14, st = 24;
        l.add(new Btn(bx, by, 20, 20, Ui.I_PLUS, null, "Zoom avant", false, () -> zoomBy(1.5)));
        l.add(new Btn(bx, by + st, 20, 20, Ui.I_MINUS, null, "Zoom arrière", false, () -> zoomBy(1 / 1.5)));
        l.add(new Btn(bx, by + st * 2, 20, 20, Ui.I_TARGET, null, "Centrer sur ma position", false, this::recenter));
        l.add(new Btn(bx, by + st * 3, 20, 20, Ui.I_PIN, null, placing ? "Repère : cliquez sur la carte pour en poser un" : "Poser un repère (cliquer un repère existant le modifie)", placing,
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
        if (editing >= 0 || !Ui.in(mx, my, x0, y0, size, size)) return true;
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
        if (editing >= 0) return true;
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
            double dx = toScreenX(MARKERS.get(i).x) - mx, dy = toScreenY(MARKERS.get(i).z) - 4 - my;
            if (dx * dx + dy * dy < 7 * 7) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        pressX = mx; pressY = my; dragged = false;
        if (editing >= 0) { editClick(mx, my, button); return true; }
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
        if (editing >= 0) return true;
        if (button == 0 && !dragged && Ui.in(mx, my, x0, y0, size, size) && Ui.in(pressX, pressY, x0, y0, size, size)) {
            loadMarkers();
            int i = markerAt(mx, my);
            // un clic sur un village ouvre sa fenetre : plan, reputation, quetes
            if (i < 0 && !placing) {
                Map.Entry<String, int[]> e = placeAt(mx, my);
                if (e != null && Towns.radius(e.getKey()) > 0) {
                    Minecraft.getInstance().setScreen(new TownScreen(e.getKey(), e.getValue()[0], e.getValue()[1]));
                    return true;
                }
            }
            if (i < 0 && placing) {
                MARKERS.add(new Marker((int) toWorldX(mx), (int) toWorldZ(my), PALETTE[0], ""));
                i = MARKERS.size() - 1;
                saveMarkers();
            }
            if (i >= 0) { openEdit(i); return true; }
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (editing >= 0) {
            if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) closeEdit();
            else nameBox.keyPressed(key, scan, mods);
            return true;
        }
        if (ClientSetup.MAP_KEY.matches(key, scan)) { onClose(); return true; }
        if (key == InputConstants.KEY_ADD || key == InputConstants.KEY_EQUALS) { zoomBy(1.5); return true; }
        if (key == InputConstants.KEY_MINUS) { zoomBy(1 / 1.5); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (editing >= 0) return nameBox.charTyped(c, mods);
        return super.charTyped(c, mods);
    }

    // ------------------------------------------------------------------ edition d'un repere (nom, couleur, suppression)
    private void openEdit(int i) {
        editing = i;
        Marker m = MARKERS.get(i);
        int px = (int) toScreenX(m.x), py = (int) toScreenY(m.z);
        ex0 = px + 12 + EW > this.width - 4 ? px - 12 - EW : px + 12;
        ex0 = Math.max(4, Math.min(this.width - EW - 4, ex0));
        ey0 = Math.max(4, Math.min(this.height - EH - 4, py - EH / 2));
        nameBox = new EditBox(this.font, ex0 + 8, ey0 + 20, EW - 16, 14, Component.literal("Nom du repère"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.literal("Nom du repère"));
        nameBox.setValue(m.name);
        nameBox.setFocused(true);
    }

    private void closeEdit() {
        if (editing >= 0 && editing < MARKERS.size()) { MARKERS.get(editing).name = nameBox.getValue().trim(); saveMarkers(); }
        editing = -1;
    }

    private void editClick(double mx, double my, int button) {
        if (!Ui.in(mx, my, ex0, ey0, EW, EH)) { closeEdit(); return; }
        if (button != 0) return;
        Marker m = MARKERS.get(editing);
        nameBox.mouseClicked(mx, my, button);
        for (int i = 0; i < PALETTE.length; i++) {
            if (Ui.in(mx, my, ex0 + 8 + i * 18, ey0 + 42, 14, 14)) { m.color = PALETTE[i]; saveMarkers(); return; }
        }
        if (Ui.in(mx, my, ex0 + 8, ey0 + EH - 24, 72, 16)) { MARKERS.remove(editing); editing = -1; saveMarkers(); return; }
        if (Ui.in(mx, my, ex0 + EW - 68, ey0 + EH - 24, 60, 16)) closeEdit();
    }

    private void drawEdit(GuiGraphics g, int mx, int my, float pt) {
        Marker m = MARKERS.get(editing);
        Ui.frame(g, ex0, ey0, EW, EH);
        Ui.parchment(g, ex0 + 4, ey0 + 4, EW - 8, EH - 8);
        g.drawString(this.font, "Repère", ex0 + 8, ey0 + 8, Ui.INK, false);
        Ui.icon(g, Ui.I_PIN, ex0 + EW - 14, ey0 + 11, 0xFF000000 | m.color);
        nameBox.setX(ex0 + 8); nameBox.setY(ey0 + 20);
        nameBox.render(g, mx, my, pt);
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = ex0 + 8 + i * 18, sy = ey0 + 42;
            boolean sel = PALETTE[i] == m.color;
            g.fill(sx - 2, sy - 2, sx + 16, sy + 16, sel ? 0xFFFFFFFF : Ui.BRONZE_D);
            g.fill(sx - 1, sy - 1, sx + 15, sy + 15, sel ? Ui.BRONZE_D : Ui.GOLD);
            g.fill(sx, sy, sx + 14, sy + 14, 0xFF000000 | PALETTE[i]);
        }
        boolean hd = Ui.in(mx, my, ex0 + 8, ey0 + EH - 24, 72, 16), ho = Ui.in(mx, my, ex0 + EW - 68, ey0 + EH - 24, 60, 16);
        Ui.button(g, ex0 + 8, ey0 + EH - 24, 72, 16, hd, false, true);
        g.drawCenteredString(this.font, "Supprimer", ex0 + 8 + 36, ey0 + EH - 24 + 4, 0xFFFF9080);
        Ui.button(g, ex0 + EW - 68, ey0 + EH - 24, 60, 16, ho, false, true);
        g.drawCenteredString(this.font, "OK", ex0 + EW - 68 + 30, ey0 + EH - 24 + 4, 0xFFF0E0B0);
    }

    static String pretty(String key) {
        String name = key.replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ------------------------------------------------------------------ dessin
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (texture == null) build();
        refresh();
        loadMarkers();
        if (editInit >= 0 && editInit < MARKERS.size()) { int k = editInit; editInit = -1; openEdit(k); }
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
            // une ville ne se nomme qu'une fois qu'on y a mis les pieds
            if (Towns.radius(e.getKey()) > 0 && !MapMemory.visited(e.getKey())) continue;
            int px = (int) toScreenX(e.getValue()[0]), py = (int) toScreenY(e.getValue()[1]);
            if (px < x0 - 40 || px > x0 + size + 40 || py < y0 - 10 || py > y0 + size + 10) continue;
            boolean h = e == hover;
            g.fill(px - (h ? 2 : 1), py - (h ? 2 : 1), px + (h ? 3 : 2), py + (h ? 3 : 2), h ? 0xFFFFFFFF : 0xFFFFD070);
            g.drawString(this.font, pretty(e.getKey()), px + 4, py - 4, h ? 0xFFFFFFFF : 0xFFF0E0B0, true);
        }
        // Memoire des Hobbits : tous les lieux du monde apparaissent, nommes
        if (memory()) {
            for (var en : MapData.places().entrySet()) {
                int[] pos = en.getValue();
                if (pos == null || pos.length < 2) continue;
                int px = (int) toScreenX(pos[0]), py = (int) toScreenY(pos[1]);
                if (px < x0 - 8 || px > x0 + size + 8 || py < y0 - 8 || py > y0 + size + 16) continue;
                Ui.icon(g, Ui.I_PIN, px, py - 4, 0xFF6ADC8A);
                g.drawString(this.font, en.getKey().replace('_', ' '), px + 6, py - 9, 0xFFB8F0C8, true);
            }
        }
        for (Marker m : MARKERS) {
            int px = (int) toScreenX(m.x), py = (int) toScreenY(m.z);
            if (px < x0 - 8 || px > x0 + size + 8 || py < y0 - 8 || py > y0 + size + 16) continue;
            Ui.icon(g, Ui.I_PIN, px, py - 4, 0xFF000000 | m.color);
            if (!m.name.isEmpty()) g.drawString(this.font, m.name, px + 6, py - 9, 0xFFFFFFFF, true);
        }
        Minecraft mc = Minecraft.getInstance();
        boolean here = playerHere();
        if (here && mc.player != null) {
            int px = (int) toScreenX(mc.player.getX()), py = (int) toScreenY(mc.player.getZ());
            g.fill(px - 3, py - 3, px + 4, py + 4, 0xFFFFFFFF);
            g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFE02020);
        }
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
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
        g.pose().popPose();
        g.disableScissor();
        if (editing >= 0 && editing < MARKERS.size()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            drawEdit(g, mx, my, pt);
            g.pose().popPose();
        }

        // bandeau haut : titre et embleme
        g.drawString(this.font, "La Hautemarche", fx + 12, fy + 5, Ui.INK, false);
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
        int avail = fw - 26 - (font.width("Légende") + 34);
        g.drawString(this.font, this.font.plainSubstrByWidth(coords, avail), fx + 26, by + 2, Ui.INK, false);

        // boutons
        Btn tip = null;
        for (Btn b : buttons()) {
            boolean hv = Ui.in(mx, my, b.x, b.y, b.w, b.h);
            Ui.button(g, b.x, b.y, b.w, b.h, hv, b.active, true);
            if (b.icon >= 0) Ui.icon(g, b.icon, b.x + b.w / 2, b.y + b.h / 2, b.active ? 0xFFFFF0B0 : Ui.GOLD_L);
            else g.drawCenteredString(this.font, b.label, b.x + b.w / 2, b.y + (b.h - 8) / 2 + 1, 0xFFF0E0B0);
            if (hv) tip = b;
        }
        int hm = editing < 0 && Ui.in(mx, my, x0, y0, size, size) ? markerAt(mx, my) : -1;
        if (editing >= 0) { }
        else if (hm >= 0) g.renderTooltip(this.font, Component.literal(MARKERS.get(hm).label() + " — clic pour renommer, colorer ou effacer"), mx, my);
        else if (tip != null) g.renderTooltip(this.font, Component.literal(tip.tip), mx, my);
        else if (hover != null) g.renderTooltip(this.font, Component.literal(pretty(hover.getKey()) + " — clic droit pour s'y rendre"), mx, my);
        else if (placing && Ui.in(mx, my, x0, y0, size, size)) g.renderTooltip(this.font, Component.literal("Poser un repère"), mx, my);
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
