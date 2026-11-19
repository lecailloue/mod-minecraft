package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ringsofash.data.Jobs;
import net.ringsofash.data.Jobs.Job;
import net.ringsofash.data.Jobs.Skill;
import net.ringsofash.data.Net;

import java.util.Map;

/** Arbre de competences des metiers (touche P ou onglet de l'inventaire). */
public class SkillScreen extends Screen {
    private static final int NODE = 24;
    private static final Map<String, ItemStack> ICONS = Map.ofEntries(
            Map.entry("Deux mains", new ItemStack(Items.NETHERITE_SWORD)), Map.entry("Épée et bouclier", new ItemStack(Items.SHIELD)),
            Map.entry("Archer", new ItemStack(Items.BOW)), Map.entry("Coup écrasant", new ItemStack(Items.MACE)),
            Map.entry("Frappe rapide", new ItemStack(Items.FEATHER)), Map.entry("Frappe lourde", new ItemStack(Items.ANVIL)),
            Map.entry("Tourbillon", new ItemStack(Items.NAUTILUS_SHELL)), Map.entry("Vitesse", new ItemStack(Items.SUGAR)),
            Map.entry("Frappe renforcée", new ItemStack(Items.IRON_SWORD)), Map.entry("Tir rapide", new ItemStack(Items.CROSSBOW)),
            Map.entry("Double flèche", new ItemStack(Items.ARROW)), Map.entry("Flèche géante", new ItemStack(Items.SPECTRAL_ARROW)),
            Map.entry("Flèches de feu", new ItemStack(Items.BLAZE_POWDER)), Map.entry("Flèche téléportée", new ItemStack(Items.ENDER_PEARL)),
            Map.entry("Flèche de soin", new ItemStack(Items.GLISTERING_MELON_SLICE)),
            Map.entry("Maître main", new ItemStack(Items.GOLDEN_PICKAXE)), Map.entry("Grosse main", new ItemStack(Items.IRON_PICKAXE)),
            Map.entry("Fondeur", new ItemStack(Items.BLAST_FURNACE)), Map.entry("Effet de fortune", new ItemStack(Items.DIAMOND)),
            Map.entry("Veine entière", new ItemStack(Items.RAW_IRON)), Map.entry("Pioche incassable", new ItemStack(Items.NETHERITE_PICKAXE)),
            Map.entry("Casse en 3×3", new ItemStack(Items.TNT)), Map.entry("Four portable", new ItemStack(Items.FURNACE)));

    private int job = 0;
    /** voie affichee au palier 10 et 15 (celle choisie, ou celle qu'on regarde) */
    private int view = 0;
    /** competence selectionnee : palier (0 a 2) et option (1 a 3), -1 = aucune */
    private int selT = -1, selO = -1;
    private int x0, y0, w, h;

    public SkillScreen() {
        super(Component.literal("Compétences"));
        view = Net.ClientJobs.choice[0];
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ donnees
    private boolean hasTree() { return job <= 1; }
    private Job job() { return Job.values()[job]; }
    private int xp() { return Net.ClientJobs.xp[job]; }
    private int level() { return Jobs.level(xp()); }
    private int chosen(int t) { return Net.ClientJobs.choice[job * 3 + t]; }

    /** 2 = choisie, 1 = disponible, 0 = verrouillee */
    private int state(int t, int opt) {
        int ch = chosen(t);
        if (t > 0 && view != chosen(0)) return 0;
        if (ch == opt) return 2;
        if (ch != 0 || level() < Jobs.TIERS[t]) return 0;
        if (t > 0 && chosen(t - 1) == 0) return 0;
        return 1;
    }

    private Skill[] skillsAt(int t) { return Jobs.skills(job(), t, t == 0 ? 0 : view); }

    private static ItemStack icon(Skill s) { return ICONS.getOrDefault(s.name(), new ItemStack(Items.BARRIER)); }

    // ------------------------------------------------------------------ geometrie
    private void layout() {
        w = Math.min(this.width - 70, 400);
        h = Math.min(this.height - 8, 236);
        x0 = Math.max(34, (this.width - w) / 2 + 14);
        if (x0 + w > this.width - 4) x0 = this.width - 4 - w;
        y0 = (this.height - h) / 2;
    }

    private int treeTop() { return y0 + 56; }
    private int treeBottom() { return y0 + h - 62; }
    private int colX(int c) { return x0 + 34 + c * ((w - 68) / 3); }
    private int rowY(int r) {
        int mid = (treeTop() + 12 + treeBottom()) / 2, gap = Math.min(42, (treeBottom() - treeTop() - 12) / 3);
        return mid + (r - 1) * gap;
    }

    // ------------------------------------------------------------------ dessin
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        layout();
        Ui.frame(g, x0, y0, w, h);
        g.fill(x0 + 4, y0 + 4, x0 + w - 4, y0 + h - 4, 0xFF1D150D);
        RingTabs.drawAt(g, x0, y0, 3, mx, my);
        // metiers
        for (int i = 0; i < 3; i++) {
            int tx = x0 + 10 + i * 82, ty = y0 + 7;
            boolean on = job == i, hv = Ui.in(mx, my, tx, ty, 78, 16);
            Ui.button(g, tx, ty, 78, 16, hv, on, true);
            g.drawCenteredString(this.font, Job.values()[i].fr, tx + 39, ty + 4, on ? 0xFFFFF0B0 : 0xFFD8C69A);
        }
        int cxb = x0 + w - 24, cyb = y0 + 7;
        boolean hc = Ui.in(mx, my, cxb, cyb, 16, 14);
        Ui.button(g, cxb, cyb, 16, 14, hc, false, true);
        Ui.icon(g, Ui.I_CLOSE, cxb + 8, cyb + 7, Ui.GOLD_L);
        // niveau et XP
        int lv = level(), xp = xp();
        g.drawString(this.font, "§l" + job().fr + "§r — niveau " + lv, x0 + 10, y0 + 28, 0xFFEBD28A, false);
        int bx = x0 + 10, by = y0 + 40, bw = w - 20;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 6, Ui.BRONZE_D);
        g.fill(bx, by, bx + bw, by + 5, 0xFF15100A);
        int lo = Jobs.xpFor(lv), hi = lv >= Jobs.MAX_LEVEL ? lo : Jobs.xpFor(lv + 1);
        int fill = hi > lo ? (int) ((long) bw * (xp - lo) / (hi - lo)) : bw;
        g.fill(bx, by, bx + fill, by + 5, Ui.GOLD);
        g.fill(bx, by, bx + fill, by + 1, Ui.GOLD_L);
        String xs = lv >= Jobs.MAX_LEVEL ? "niveau maximum" : (xp - lo) + " / " + (hi - lo) + " XP";
        g.drawString(this.font, xs, x0 + w - 10 - this.font.width(xs), y0 + 28, 0xFFB8A880, false);

        Component tip = null;
        if (hasTree()) tip = renderTree(g, mx, my);
        else g.drawCenteredString(this.font, "Cet arbre de compétences arrive bientôt.", x0 + w / 2, (treeTop() + treeBottom()) / 2, 0xFF9A8A6A);
        renderDetail(g, mx, my);
        if (tip != null) g.renderTooltip(this.font, tip, mx, my);
        else if (hc) g.renderTooltip(this.font, Component.literal("Fermer"), mx, my);
    }

    private Component renderTree(GuiGraphics g, int mx, int my) {
        Component tip = null;
        String[] heads = {"Voie", "Niveau 5", "Niveau 10", "Niveau 15"};
        for (int c = 0; c < 4; c++) {
            g.drawCenteredString(this.font, heads[c], colX(c), treeTop(), c == 0 ? 0xFFEBD28A : (level() >= Jobs.TIERS[Math.max(0, c - 1)] || c == 0 ? 0xFFEBD28A : 0xFF8A7A60));
            g.fill(colX(c) - 30, treeTop() + 10, colX(c) + 31, treeTop() + 11, Ui.BRONZE);
        }
        // liens
        for (int r = 0; r < 3; r++) link(g, colX(0), rowY(1), colX(1), rowY(r), chosen(0) == r + 1);
        Skill[] s1 = skillsAt(1), s2 = skillsAt(2);
        if (view != 0) {
            for (int r = 0; r < s1.length; r++) link(g, colX(1), rowY(view - 1), colX(2), rowY(r), view == chosen(0) && chosen(1) == r + 1);
            for (int r = 0; r < s2.length; r++) link(g, colX(2), rowY(chosen(1) == 0 ? 1 : chosen(1) - 1), colX(3), rowY(r), view == chosen(0) && chosen(2) == r + 1);
        }
        // noeuds
        node(g, colX(0), rowY(1), new ItemStack(job == 1 ? Items.IRON_PICKAXE : Items.IRON_CHESTPLATE), 2, false);
        if (Ui.in(mx, my, colX(0) - NODE / 2, rowY(1) - NODE / 2, NODE, NODE)) tip = Component.literal(job().fr);
        Skill[] s0 = skillsAt(0);
        for (int r = 0; r < s0.length; r++) {
            int st = state(0, r + 1);
            node(g, colX(1), rowY(r), icon(s0[r]), st, view == r + 1);
            g.drawCenteredString(this.font, this.font.plainSubstrByWidth(s0[r].name(), 92), colX(1), rowY(r) + NODE / 2 + 2, st == 0 ? 0xFF9A8A6A : 0xFFF0E4C0);
            if (Ui.in(mx, my, colX(1) - NODE / 2, rowY(r) - NODE / 2, NODE, NODE)) tip = Component.literal(s0[r].name());
        }
        for (int t = 1; t <= 2; t++) {
            Skill[] ss = skillsAt(t);
            if (ss.length == 0) {
                node(g, colX(t + 1), rowY(1), new ItemStack(Items.BARRIER), 0, false);
                g.drawCenteredString(this.font, view == 0 ? "Choisir une voie" : "À définir", colX(t + 1), rowY(1) + NODE / 2 + 2, 0xFF9A8A6A);
                continue;
            }
            for (int r = 0; r < ss.length; r++) {
                int st = state(t, r + 1);
                boolean sel = selT == t && selO == r + 1;
                node(g, colX(t + 1), rowY(r), icon(ss[r]), st, sel);
                g.drawCenteredString(this.font, this.font.plainSubstrByWidth(ss[r].name(), 92), colX(t + 1), rowY(r) + NODE / 2 + 2, st == 0 ? 0xFF9A8A6A : 0xFFF0E4C0);
                if (Ui.in(mx, my, colX(t + 1) - NODE / 2, rowY(r) - NODE / 2, NODE, NODE)) tip = Component.literal(ss[r].name());
            }
        }
        return tip;
    }

    private void link(GuiGraphics g, int x1, int y1, int x2, int y2, boolean on) {
        int col = on ? Ui.GOLD : 0xFF4A3820;
        int a = x1 + NODE / 2, b = x2 - NODE / 2, mid = (a + b) / 2;
        g.fill(a, y1, mid + 1, y1 + 1, col);
        g.fill(mid, Math.min(y1, y2), mid + 1, Math.max(y1, y2) + 1, col);
        g.fill(mid, y2, b, y2 + 1, col);
    }

    private void node(GuiGraphics g, int cx, int cy, ItemStack it, int state, boolean selected) {
        int x = cx - NODE / 2, y = cy - NODE / 2;
        int edge = state == 2 ? Ui.GOLD_L : state == 1 ? Ui.GOLD : 0xFF4A3820;
        int body = state == 2 ? 0xFF7A5A22 : state == 1 ? 0xFF3A2A14 : 0xFF15100A;
        g.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, Ui.BRONZE_D);
        g.fill(x, y, x + NODE, y + NODE, edge);
        g.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, body);
        g.renderItem(it, cx - 8, cy - 8);
        if (state == 0) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 250);                 // au-dessus de l'objet
            g.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, 0xA015100A);
            Ui.icon(g, Ui.I_LOCK, cx, cy, 0xFFC9A04A);
            g.pose().popPose();
        }
        if (selected) g.fill(x - 3, y - 3, x + NODE + 3, y - 2, Ui.GOLD_L);
    }

    private void renderDetail(GuiGraphics g, int mx, int my) {
        int dy = y0 + h - 56, dx = x0 + 8, dw = w - 16;
        Ui.inset(g, dx, dy, dw, 48, 0xFF241A10);
        if (!hasTree()) return;
        if (selT < 0) {
            g.drawString(this.font, "Cliquez sur une compétence pour la voir.", dx + 6, dy + 6, 0xFF9A8A6A, false);
            g.drawString(this.font, "Les choix se font aux niveaux 5, 10 et 15 — et sont définitifs.", dx + 6, dy + 18, 0xFF9A8A6A, false);
            return;
        }
        Skill s = skillsAt(selT)[selO - 1];
        int st = state(selT, selO);
        g.drawString(this.font, "§l" + s.name(), dx + 6, dy + 4, 0xFFEBD28A, false);
        int yy = dy + 15;
        for (FormattedCharSequence l : this.font.split(Component.literal(s.desc()), dw - 140)) { g.drawString(this.font, l, dx + 6, yy, 0xFFE0D4B0, false); yy += 9; }
        int bx = dx + dw - 124, by = dy + 28, bw = 118;
        if (st == 2) {
            g.drawCenteredString(this.font, "Compétence apprise", bx + bw / 2, by + 4, 0xFF7FD68A);
        } else if (!s.live()) {
            g.drawCenteredString(this.font, "Effet à venir", bx + bw / 2, by + 4, 0xFF9A8A6A);
        } else if (st == 1) {
            boolean hv = Ui.in(mx, my, bx, by, bw, 15);
            Ui.button(g, bx, by, bw, 15, hv, false, true);
            g.drawCenteredString(this.font, "Choisir (définitif)", bx + bw / 2, by + 4, 0xFFEBD28A);
        } else {
            int need = Jobs.TIERS[selT];
            g.drawCenteredString(this.font, chosen(selT) != 0 ? "Autre choix déjà fait" : level() < need ? "Niveau " + need + " requis" : "Choisissez d'abord la voie", bx + bw / 2, by + 4, 0xFF9A8A6A);
        }
    }

    // ------------------------------------------------------------------ souris et clavier
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (RingTabs.clickAt(x0, y0, mx, my, 3, false)) return true;
            if (Ui.in(mx, my, x0 + w - 24, y0 + 7, 16, 14)) { onClose(); return true; }
            for (int i = 0; i < 3; i++) if (Ui.in(mx, my, x0 + 10 + i * 82, y0 + 7, 78, 16)) { job = i; selT = selO = -1; view = Net.ClientJobs.choice[i * 3]; return true; }
            if (hasTree()) {
                Skill[] s0 = skillsAt(0);
                for (int r = 0; r < s0.length; r++) {
                    if (Ui.in(mx, my, colX(1) - NODE / 2, rowY(r) - NODE / 2, NODE, NODE)) { view = r + 1; selT = 0; selO = r + 1; return true; }
                }
                for (int t = 1; t <= 2; t++) {
                    Skill[] ss = skillsAt(t);
                    for (int r = 0; r < ss.length; r++) {
                        if (Ui.in(mx, my, colX(t + 1) - NODE / 2, rowY(r) - NODE / 2, NODE, NODE)) { selT = t; selO = r + 1; return true; }
                    }
                }
                if (selT >= 0) {
                    Skill s = skillsAt(selT)[selO - 1];
                    int dx = x0 + 8, dw = w - 16, dy = y0 + h - 56;
                    if (state(selT, selO) == 1 && s.live() && Ui.in(mx, my, dx + dw - 124, dy + 28, 118, 15)) {
                        PacketDistributor.sendToServer(new Net.ChooseSkill(job, selT, selO));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientSetup.SKILLS_KEY.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
