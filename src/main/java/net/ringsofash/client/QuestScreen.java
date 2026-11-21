package net.ringsofash.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ringsofash.data.Net.ClientLore;
import net.ringsofash.data.People;
import net.ringsofash.gen.Reg;

import java.util.ArrayList;
import java.util.List;

/** Journal : onglet Quetes (liste, detail, suivi) et onglet Reputation (touche R). */
public class QuestScreen extends Screen {
    /** Quete suivie (affichee a l'ecran en jeu) ; -1 = aucune. */
    public static int tracked = -1;

    private record Obj(String text, boolean done) {}

    /** state : 0 verrouillee, 1 en cours, 2 terminee. */
    private record Q(String title, String desc, int state, List<Obj> objs, List<ItemStack> rewards, String rewardNote, ItemStack icon, String lockHint) {}

    private int tab, sel = -1;
    private int x0, y0, w, h;

    public QuestScreen(int tab) {
        super(Component.literal("Journal"));
        this.tab = tab;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ donnees (tirees de l'avancement reel du joueur)
    static int general() {
        int s = 0;
        for (int v : ClientLore.rep) s += v;
        return Math.round(s / (float) People.values().length);
    }

    static List<Q> quests() {
        int q = ClientLore.quest;
        List<Q> l = new ArrayList<>();
        l.add(new Q("L'Anneau de Cendre",
                "On murmure qu'un anneau maudit dort au fond des mines naines des Monts de Fer, gardé par un démon de braise. Il faut le prendre avant que l'Ombre ne le réclame.",
                q >= 1 ? 2 : 1,
                List.of(new Obj("Trouver l'Anneau de Cendre dans les mines naines", q >= 1)),
                List.of(), "Aucune : la quête principale se poursuit.", new ItemStack(Reg.ASH_RING.get()), ""));
        l.add(new Q("Le Mont Funeste",
                "Cet anneau ne doit pas rester dans le monde. Portez-le jusqu'au Mont Funeste, dans les Terres de Cendre, et jetez-le dans la lave du cratère.",
                q >= 2 ? 2 : q == 1 ? 1 : 0,
                List.of(new Obj("Jeter l'Anneau dans la lave du cratère", q >= 2)),
                List.of(), "Aucune : mais le maître de l'Anneau viendra le réclamer.", new ItemStack(Items.LAVA_BUCKET), "Terminez « L'Anneau de Cendre » pour débloquer cette quête."));
        boolean dark = general() < 0;
        List<ItemStack> rw = dark
                ? List.of(new ItemStack(Reg.ASHLORD_CHESTPLATE.get()))
                : List.of(new ItemStack(Reg.ELFKING_CHESTPLATE.get()));
        l.add(new Q("Le Seigneur de Cendre",
                "L'Anneau a fondu et son maître est sorti de l'ombre. Descendez l'affronter sur le lac figé.",
                q >= 3 ? 2 : q == 2 ? 1 : 0,
                List.of(new Obj("Vaincre le Seigneur de Cendre", q >= 3)),
                rw, dark ? "Voie de l'ombre : armure complète du Seigneur de Cendre." : "Voie de la lumière : armure du roi elfe, +30 de réputation chez chaque peuple.",
                new ItemStack(Reg.ASHLORD_CHESTPLATE.get()), "Jetez l'Anneau dans le Mont Funeste pour débloquer cette quête."));
        List<Obj> friends = new ArrayList<>();
        boolean all = true;
        for (People p : People.values()) {
            int rep = p.ordinal() < ClientLore.rep.length ? ClientLore.rep[p.ordinal()] : 0;
            boolean ok = rep >= 15;
            all &= ok;
            friends.add(new Obj("Devenir ami des " + p.label + " (" + rep + "/15)", ok));
        }
        l.add(new Q("L'amitié des peuples",
                "Hommes, elfes, nains et semi-hommes jugent vos actes. Aidez-les, ne les frappez pas : leur estime décide de la voie que vous suivrez à la fin.",
                all ? 2 : 1, friends, List.of(), "Une réputation générale positive ouvre la voie de la lumière.", new ItemStack(Reg.HALFLING_RING.get()), ""));
        return l;
    }

    /** Lignes du suivi de quete affichees en jeu : {titre, objectif} ou null. */
    static String[] tracker() {
        List<Q> qs = quests();
        if (tracked < 0 || tracked >= qs.size()) return null;
        Q q = qs.get(tracked);
        if (q.state != 1) return null;
        for (Obj o : q.objs) if (!o.done) return new String[]{q.title, o.text};
        return null;
    }

    static void renderTracker(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        String[] t = tracker();
        if (t == null) return;
        Font f = mc.font;
        int tw = Math.min(190, Math.max(f.width(t[0]), f.width(t[1])) + 4);
        List<FormattedCharSequence> body = f.split(Component.literal(t[1]), tw);
        int bw = tw + 10, bh = 16 + body.size() * 9 + 4;
        int x = g.guiWidth() - bw - 6, y = 22;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, Ui.GOLD);
        g.fill(x, y, x + bw, y + bh, 0xB0140E08);
        Ui.icon(g, Ui.I_FLAG, x + 8, y + 7, Ui.GOLD_L);
        g.drawString(f, t[0], x + 16, y + 3, 0xFFEBD28A, true);
        int yy = y + 15;
        for (FormattedCharSequence s : body) { g.drawString(f, s, x + 5, yy, 0xFFE8DCC0, true); yy += 9; }
    }

    @Override
    protected void init() {
        if (sel < 0) {
            List<Q> qs = quests();
            sel = 0;
            for (int i = 0; i < qs.size(); i++) if (qs.get(i).state == 1) { sel = i; break; }
        }
    }

    // ------------------------------------------------------------------ geometrie
    private void layout() {
        w = Math.min(this.width - 8, 424);
        h = Math.min(this.height - 8, 240);
        x0 = (this.width - w) / 2; y0 = (this.height - h) / 2;
    }

    private int stripY() { return y0 + h - 52; }
    private int rowY(int i) { return y0 + 41 + i * 27; }

    // ------------------------------------------------------------------ dessin
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        layout();
        Ui.frame(g, x0, y0, w, h);
        g.fill(x0 + 4, y0 + 4, x0 + w - 4, y0 + h - 4, 0xFF1D150D);
        renderTabs(g, mx, my);
        ItemStack tipItem = null;
        if (tab == 0) tipItem = renderQuests(g, mx, my);
        else renderReputation(g);
        // fermer
        int cxb = x0 + w - 24, cyb = y0 + 7;
        boolean hc = Ui.in(mx, my, cxb, cyb, 16, 14);
        Ui.button(g, cxb, cyb, 16, 14, hc, false, true);
        Ui.icon(g, Ui.I_CLOSE, cxb + 8, cyb + 7, Ui.GOLD_L);
        if (tipItem != null) g.renderTooltip(this.font, tipItem, mx, my);
        else if (hc) g.renderTooltip(this.font, Component.literal("Fermer"), mx, my);
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        String[] names = {"Quêtes", "Réputation"};
        int[] icons = {Ui.I_SCROLL, Ui.I_FLAG};
        for (int i = 0; i < 2; i++) {
            int tx = x0 + 10 + i * 82, ty = y0 + 7;
            boolean on = tab == i, hv = Ui.in(mx, my, tx, ty, 78, 16);
            Ui.button(g, tx, ty, 78, 16, hv, on, true);
            Ui.icon(g, icons[i], tx + 9, ty + 8, on ? 0xFFFFF0B0 : Ui.GOLD);
            g.drawString(this.font, names[i], tx + 19, ty + 4, on ? 0xFFFFF0B0 : 0xFFD8C69A, false);
        }
    }

    private static int stateColor(int state) { return state == 2 ? 0xFF7FD68A : state == 1 ? 0xFFEBD28A : 0xFF8A7A60; }
    private static String stateName(int state) { return state == 2 ? "Terminée" : state == 1 ? "En cours" : "Verrouillée"; }

    private ItemStack renderQuests(GuiGraphics g, int mx, int my) {
        List<Q> qs = quests();
        if (sel >= qs.size()) sel = 0;
        int lx = x0 + 8, lw = 156, bottom = stripY() - 5;
        g.drawString(this.font, "Quêtes disponibles", lx + 2, y0 + 29, Ui.GOLD, false);
        Ui.inset(g, lx, y0 + 38, lw, bottom - (y0 + 38), 0xFF2A2016);
        for (int i = 0; i < qs.size(); i++) {
            Q q = qs.get(i);
            int ry = rowY(i);
            if (ry + 25 > bottom) break;
            boolean on = i == sel, hv = Ui.in(mx, my, lx + 2, ry, lw - 4, 25);
            g.fill(lx + 2, ry, lx + lw - 2, ry + 25, on ? 0xFF6E5026 : hv ? 0xFF4A3820 : 0xFF33281A);
            if (on) { g.fill(lx + 2, ry, lx + lw - 2, ry + 1, Ui.GOLD); g.fill(lx + 2, ry + 24, lx + lw - 2, ry + 25, Ui.GOLD); }
            g.fill(lx + 5, ry + 4, lx + 23, ry + 22, 0xFF15100A);
            g.renderItem(q.icon, lx + 6, ry + 5);
            if (q.state == 0) {
                g.fill(lx + 5, ry + 4, lx + 23, ry + 22, 0xAA15100A);
                Ui.icon(g, Ui.I_LOCK, lx + 14, ry + 13, 0xFFC9A04A);
            }
            String title = this.font.plainSubstrByWidth(q.state == 0 ? "???" : q.title, lw - 48);
            g.drawString(this.font, title, lx + 27, ry + 5, q.state == 0 ? 0xFF9A8A6A : 0xFFF0E4C0, false);
            g.drawString(this.font, stateName(q.state), lx + 27, ry + 15, stateColor(q.state), false);
            if (i == tracked && q.state == 1) Ui.icon(g, Ui.I_FLAG, lx + lw - 10, ry + 12, 0xFFEBD28A);
            else if (q.state == 2) Ui.icon(g, Ui.I_CHECK, lx + lw - 10, ry + 12, 0xFF7FD68A);
        }
        renderDetail(g, qs.get(sel), mx, my);
        ItemStack tip = detailTip;
        renderFactions(g);
        return tip;
    }

    private ItemStack detailTip;

    private void renderDetail(GuiGraphics g, Q q, int mx, int my) {
        detailTip = null;
        int dx = x0 + 172, dw = w - 180, bottom = stripY() - 5;
        Ui.inset(g, dx - 2, y0 + 28, dw + 2, bottom - (y0 + 28), 0xFF241A10);
        g.enableScissor(dx, y0 + 29, dx + dw, bottom - 1);
        boolean locked = q.state == 0;
        // illustration
        int ix = dx + dw - 62, iy = y0 + 33;
        Ui.inset(g, ix, iy, 56, 56, 0xFF15100A);
        g.fill(ix + 1, iy + 38, ix + 55, iy + 55, 0xFF2A2016);
        g.pose().pushPose();
        g.pose().translate(ix + 4, iy + 4, 0);
        g.pose().scale(3F, 3F, 1F);
        g.renderItem(q.icon, 0, 0);
        g.pose().popPose();
        if (locked) { g.fill(ix, iy, ix + 56, iy + 56, 0xB015100A); Ui.icon(g, Ui.I_LOCK, ix + 28, iy + 28, Ui.GOLD); }

        g.drawString(this.font, "§l" + (locked ? "Quête verrouillée" : q.title), dx + 4, y0 + 33, 0xFFEBD28A, false);
        g.fill(dx + 4, y0 + 44, dx + dw - 68, y0 + 45, Ui.BRONZE);
        int yy = y0 + 49;
        if (locked) {
            for (FormattedCharSequence s : this.font.split(Component.literal(q.lockHint), dw - 76)) { g.drawString(this.font, s, dx + 4, yy, 0xFFB8A880, false); yy += 9; }
            g.disableScissor();
            drawFollow(g, q, mx, my, dx, bottom);
            return;
        }
        g.drawString(this.font, "Description", dx + 4, yy, Ui.GOLD, false);
        yy += 10;
        for (FormattedCharSequence s : this.font.split(Component.literal(q.desc), dw - 76)) { g.drawString(this.font, s, dx + 4, yy, 0xFFE0D4B0, false); yy += 9; }
        yy = Math.max(yy + 3, iy + 60);
        g.drawString(this.font, "Objectifs", dx + 4, yy, Ui.GOLD, false);
        yy += 10;
        for (Obj o : q.objs) {
            g.fill(dx + 4, yy, dx + 12, yy + 8, Ui.BRONZE_D);
            g.fill(dx + 5, yy + 1, dx + 11, yy + 7, 0xFF15100A);
            if (o.done) Ui.icon(g, Ui.I_CHECK, dx + 8, yy + 3, 0xFF7FD68A);
            List<FormattedCharSequence> ls = this.font.split(Component.literal(o.text), dw - 24);
            for (FormattedCharSequence s : ls) { g.drawString(this.font, s, dx + 16, yy, o.done ? 0xFF9ABF9A : 0xFFE0D4B0, false); yy += 9; }
            if (ls.isEmpty()) yy += 9;
        }
        yy += 3;
        g.drawString(this.font, "Récompenses", dx + 4, yy, Ui.GOLD, false);
        yy += 10;
        int rx = dx + 4;
        for (ItemStack st : q.rewards) {
            g.fill(rx - 1, yy - 1, rx + 17, yy + 17, Ui.BRONZE_D);
            g.fill(rx, yy, rx + 16, yy + 16, 0xFF33281A);
            g.renderItem(st, rx, yy);
            if (Ui.in(mx, my, rx, yy, 16, 16)) detailTip = st;
            rx += 21;
        }
        List<FormattedCharSequence> note = this.font.split(Component.literal(q.rewardNote), dw - (rx - dx) - 6);
        int ny = q.rewards.isEmpty() ? yy : yy + 1;
        for (FormattedCharSequence s : note) { g.drawString(this.font, s, q.rewards.isEmpty() ? dx + 4 : rx + 2, ny, 0xFFD8C69A, false); ny += 9; }
        g.disableScissor();
        drawFollow(g, q, mx, my, dx, bottom);
    }

    private void drawFollow(GuiGraphics g, Q q, int mx, int my, int dx, int bottom) {
        int bw = 118, bx = x0 + w - 8 - bw - 4, by = bottom - 19;
        boolean can = q.state == 1;
        boolean on = can && sel == tracked;
        Ui.button(g, bx, by, bw, 15, can && Ui.in(mx, my, bx, by, bw, 15), on, can);
        String label = q.state == 2 ? "Quête terminée" : q.state == 0 ? "Verrouillée" : on ? "Ne plus suivre" : "Suivre la quête";
        g.drawCenteredString(this.font, label, bx + bw / 2, by + 4, can ? 0xFFF0E4C0 : 0xFF8A7A60);
    }

    private void renderFactions(GuiGraphics g) {
        int sx = x0 + 8, sy = stripY(), sw = w - 16;
        Ui.inset(g, sx, sy, sw, 44, 0xFF241A10);
        g.drawString(this.font, "Réputation des factions", sx + 5, sy + 4, Ui.GOLD, false);
        People[] ps = People.values();
        int cw = (sw - 10) / ps.length;
        for (int i = 0; i < ps.length; i++) {
            int cx = sx + 5 + i * cw;
            int rep = ps[i].ordinal() < ClientLore.rep.length ? ClientLore.rep[ps[i].ordinal()] : 0;
            g.drawString(this.font, ps[i].label, cx, sy + 16, 0xFF000000 | ps[i].color, false);
            bar(g, cx, sy + 26, cw - 8, 5, rep, 0xFF000000 | ps[i].color);
            g.drawString(this.font, People.rank(rep) + " (" + rep + ")", cx, sy + 33, rankColor(rep), false);
        }
    }

    private static int rankColor(int rep) {
        if (rep <= -50) return 0xFFE05050;
        if (rep <= -15) return 0xFFE09050;
        if (rep < 15) return 0xFFB8B8B8;
        if (rep < 50) return 0xFF7FD68A;
        return 0xFFEBD28A;
    }

    private void renderReputation(GuiGraphics g) {
        int lx = x0 + 12, lw = w - 24, y = y0 + 32;
        g.drawString(this.font, "§lRéputation dans la Hautemarche", lx, y, 0xFFEBD28A, false);
        y += 16;
        int sum = 0;
        for (People p : People.values()) {
            int rep = p.ordinal() < ClientLore.rep.length ? ClientLore.rep[p.ordinal()] : 0;
            sum += rep;
            String rk = People.rank(rep) + " (" + rep + ")";
            g.drawString(this.font, p.label, lx, y, 0xFF000000 | p.color, false);
            g.drawString(this.font, rk, lx + lw - this.font.width(rk), y, rankColor(rep), false);
            bar(g, lx, y + 11, lw, 7, rep, 0xFF000000 | p.color);
            y += 25;
        }
        int gen = Math.round(sum / (float) People.values().length);
        String gr = People.rank(gen) + " (" + gen + ")";
        g.drawString(this.font, "§lRéputation générale", lx, y, 0xFFEBD28A, false);
        g.drawString(this.font, gr, lx + lw - this.font.width(gr), y, rankColor(gen), false);
        bar(g, lx, y + 11, lw, 7, gen, 0xFFEBD28A);
        y += 26;
        g.drawString(this.font, gen < 0 ? "§cVoie de l'ombre : armure et masse du Seigneur de Cendre" : "§aVoie de la lumière : armure et épée du roi elfe", lx, y, 0xFFFFFFFF, false);
        y += 12;
        g.drawString(this.font, "Corruption : " + (ClientLore.corruption / 10) + " %      Œil : " + Math.min(100, ClientLore.eye * 100 / 120) + " %", lx, y,
                ClientLore.corruption > 500 ? 0xFFFF6060 : 0xFFA09070, false);
    }

    /** Barre centree : positive vers la droite (couleur du peuple), negative vers la gauche (rouge) ; reperes aux paliers de rang. */
    private void bar(GuiGraphics g, int x, int y, int bw, int bh, int value, int color) {
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, Ui.BRONZE_D);
        g.fill(x, y, x + bw, y + bh, 0xFF0A0806);
        int mid = x + bw / 2;
        int len = Math.round((bw / 2F) * Math.abs(value) / 100F);
        if (value >= 0) g.fill(mid, y + 1, mid + len, y + bh - 1, color);
        else g.fill(mid - len, y + 1, mid, y + bh - 1, 0xFFC03030);
        for (int t : new int[]{-50, -15, 15, 50}) g.fill(mid + Math.round((bw / 2F) * t / 100F), y, mid + Math.round((bw / 2F) * t / 100F) + 1, y + bh, 0x60FFFFFF);
        g.fill(mid, y - 1, mid + 1, y + bh + 1, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ interactions
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        if (button == 0) {
            if (Ui.in(mx, my, x0 + w - 24, y0 + 7, 16, 14)) { onClose(); return true; }
            for (int i = 0; i < 2; i++) {
                if (Ui.in(mx, my, x0 + 10 + i * 82, y0 + 7, 78, 16)) { tab = i; return true; }
            }
            if (tab == 0) {
                List<Q> qs = quests();
                int lx = x0 + 8, lw = 156, bottom = stripY() - 5;
                for (int i = 0; i < qs.size(); i++) {
                    if (rowY(i) + 25 > bottom) break;
                    if (Ui.in(mx, my, lx + 2, rowY(i), lw - 4, 25)) { sel = i; return true; }
                }
                int bw = 118, bx = x0 + w - 8 - bw - 4, by = bottom - 19;
                if (sel >= 0 && sel < qs.size() && qs.get(sel).state == 1 && Ui.in(mx, my, bx, by, bw, 15)) {
                    tracked = tracked == sel ? -1 : sel;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientSetup.REP_KEY.matches(key, scan)) { onClose(); return true; }
        if (tab == 0) {
            int n = quests().size();
            if (key == InputConstants.KEY_DOWN) { sel = Math.min(n - 1, sel + 1); return true; }
            if (key == InputConstants.KEY_UP) { sel = Math.max(0, sel - 1); return true; }
        }
        if (key == InputConstants.KEY_TAB || key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT) { tab = 1 - tab; return true; }
        return super.keyPressed(key, scan, mods);
    }
}
