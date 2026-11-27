package net.ringsofash.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ringsofash.data.Net.ClientLore;
import net.ringsofash.data.People;
import net.ringsofash.world.MapData;
import net.ringsofash.world.Towns;

/**
 * La fenetre d'une ville, ouverte d'un clic sur son nom depuis la carte : le plan de la
 * ville, ce que ses habitants pensent de vous, et les quetes en cours.
 */
public class TownScreen extends Screen {
    private final String key;
    private final int wx, wz;
    private final People who;

    private int x0, y0, w, h;

    public TownScreen(String key, int wx, int wz) {
        super(Component.literal(MapScreen.pretty(key)));
        this.key = key;
        this.wx = wx;
        this.wz = wz;
        People o = Towns.owner(wx, wz);
        this.who = o == null ? People.HUMAN : o;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);

        w = Math.min(420, this.width - 40);
        h = Math.min(300, this.height - 40);
        x0 = (this.width - w) / 2;
        y0 = (this.height - h) / 2;

        Ui.frame(g, x0, y0, w, h);
        Ui.parchment(g, x0 + 4, y0 + 4, w - 8, h - 8);

        // ---- le plan de la ville, en haut a gauche
        int plan = Math.min(h - 96, w - 150);
        int px = x0 + 16, py = y0 + 18;
        g.fill(px - 2, py - 2, px + plan + 2, py + plan * 3 / 4 + 2, Ui.BRONZE_D);
        g.fill(px - 1, py - 1, px + plan + 1, py + plan * 3 / 4 + 1, Ui.GOLD);
        plan(g, px, py, plan, plan * 3 / 4);

        // ---- le nom, sous le plan
        g.pose().pushPose();
        g.pose().translate(px, py + plan * 3 / 4 + 6, 0);
        g.pose().scale(1.8F, 1.8F, 1F);
        g.drawString(this.font, MapScreen.pretty(key), 0, 0, 0xFFEBD28A, true);
        g.pose().popPose();

        // ---- les quetes, a droite
        int qx = px + plan + 12, qw = x0 + w - 16 - qx;
        g.drawString(this.font, "Quêtes", qx, y0 + 18, 0xFFEBD28A, true);
        g.fill(qx - 2, y0 + 30, qx + qw + 2, y0 + 30 + plan * 3 / 4 - 12, 0x66201408);
        int qy = y0 + 36;
        List<String[]> qs = quests();
        if (qs.isEmpty()) g.drawString(this.font, "Rien à faire ici.", qx + 2, qy, 0xFF9A8E70, false);
        for (String[] q : qs) {
            if (qy > y0 + 30 + plan * 3 / 4 - 24) break;
            g.drawString(this.font, q[0], qx + 2, qy, Integer.parseInt(q[1], 16) | 0xFF000000, false);
            qy += 11;
        }

        // ---- la reputation, en bas
        bar(g, x0 + 16, y0 + h - 40, w - 32, 16);
    }

    /** le plan : la carte deja decouverte, centree sur la ville */
    private void plan(GuiGraphics g, int px, int py, int pw, int ph) {
        int span = Math.max(80, Towns.radius(key) * 2 + 60);     // la ville entiere, et un peu autour
        double u = (wx - span / 2.0 + MapData.HALF) / MapData.SIZE * MapScreen.TEX;
        double v = (wz - span / 2.0 + MapData.HALF) / MapData.SIZE * MapScreen.TEX;
        int uw = Math.max(1, (int) Math.round(span / (double) MapData.SIZE * MapScreen.TEX));
        if (MapScreen.ensureTexture() != null)
            g.blit(MapScreen.ensureTexture(), px, py, pw, ph, (float) u, (float) v, uw, uw, MapScreen.TEX, MapScreen.TEX);
        // la ville au milieu
        Ui.icon(g, Ui.I_PIN, px + pw / 2, py + ph / 2 - 4, 0xFFFFD070);
    }

    /** la barre de reputation du peuple qui habite ici */
    private void bar(GuiGraphics g, int bx, int by, int bw, int bh) {
        int rep = ClientLore.rep[who.ordinal()];
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, Ui.BRONZE_D);
        g.fill(bx, by, bx + bw, by + bh, 0xFF2A2012);
        // de -100 a +100, le zero au milieu
        int mid = bx + bw / 2;
        int end = mid + (int) (Math.max(-100, Math.min(100, rep)) / 100.0 * (bw / 2.0));
        int col = rep < 0 ? 0xFFB44A3C : 0xFF6FA84E;
        if (end != mid) g.fill(Math.min(mid, end), by + 2, Math.max(mid, end), by + bh - 2, col);
        g.fill(mid, by, mid + 1, by + bh, Ui.GOLD);
        String txt = who.label + " — " + People.rank(rep) + " (" + rep + ")";
        g.drawString(this.font, txt, bx + 4, by + (bh - 8) / 2, 0xFFEBD28A, true);
    }

    /** les quetes du joueur, celles qui touchent ce peuple en tete */
    private List<String[]> quests() {
        List<String[]> out = new ArrayList<>();
        int rep = ClientLore.rep[who.ordinal()];
        out.add(new String[]{rep >= 15 ? "✔ Gagner leur confiance" : "• Gagner leur confiance", rep >= 15 ? "7FD68A" : "D8CCA8"});
        out.add(new String[]{ClientLore.quest >= 1 ? "✔ L'Anneau de Cendre" : "• L'Anneau de Cendre",
                ClientLore.quest >= 1 ? "7FD68A" : "D8CCA8"});
        out.add(new String[]{ClientLore.quest >= 2 ? "✔ Le Mont Funeste" : "• Le Mont Funeste",
                ClientLore.quest >= 2 ? "7FD68A" : "D8CCA8"});
        if (rep <= -15) out.add(new String[]{"Ils ne vous laisseront pas entrer.", "E09050"});
        return out;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
