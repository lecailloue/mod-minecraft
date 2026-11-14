package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;

/** Petite boite a outils graphique : cadres de bronze et de parchemin, boutons et pictogrammes dessines au pixel. */
final class Ui {
    private Ui() {}

    static final int BRONZE_D = 0xFF2B1D0C, BRONZE = 0xFF6E5026, GOLD = 0xFFC9A04A, GOLD_L = 0xFFEBD28A;
    static final int PARCH = 0xFFD8C69A, PARCH_D = 0xFFB9A474, PARCH_L = 0xFFE9DDB8, INK = 0xFF2A1E0E, DARK = 0xFF15100A;

    static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    /** Cadre de bronze (liseres, rivets aux coins) ; l'interieur est laisse a l'appelant. */
    static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BRONZE_D);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BRONZE);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GOLD);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BRONZE_D);
        for (int i = 0; i < 4; i++) {
            int cx = (i & 1) == 0 ? x + 1 : x + w - 6, cy = (i & 2) == 0 ? y + 1 : y + h - 6;
            g.fill(cx, cy, cx + 5, cy + 5, GOLD);
            g.fill(cx + 1, cy + 1, cx + 4, cy + 4, BRONZE_D);
            g.fill(cx + 2, cy + 2, cx + 3, cy + 3, GOLD_L);
        }
    }

    /** Fond de parchemin sobre. */
    static void parchment(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PARCH);
        for (int yy = y + 3; yy < y + h; yy += 7) g.fill(x, yy, x + w, yy + 1, 0x14604020);
        g.fill(x, y, x + w, y + 1, PARCH_D);
        g.fill(x, y + h - 1, x + w, y + h, PARCH_D);
        g.fill(x, y, x + 1, y + h, PARCH_D);
        g.fill(x + w - 1, y, x + w, y + h, PARCH_D);
    }

    /** Encadre un contenu (sillon sombre). */
    static void inset(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BRONZE_D);
        g.fill(x, y, x + w, y + h, fill);
    }

    static void button(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean active, boolean enabled) {
        int edge = enabled ? (active ? GOLD_L : GOLD) : 0xFF5A4A30;
        int body = !enabled ? 0xFF2A2218 : active ? 0xFF7A5A22 : hover ? 0xFF5A4220 : 0xFF3A2A14;
        g.fill(x, y, x + w, y + h, BRONZE_D);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, edge);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, body);
        if (enabled) g.fill(x + 2, y + 2, x + w - 2, y + 3, hover || active ? 0x40FFFFFF : 0x20FFFFFF);
    }

    static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    // ------------------------------------------------------------------ pictogrammes (centres sur cx, cy)
    static final int I_PLUS = 0, I_MINUS = 1, I_TARGET = 2, I_PIN = 3, I_SCROLL = 4, I_CLOSE = 5, I_CHECK = 6, I_LOCK = 7, I_FLAG = 8, I_BOOK = 9;

    static void icon(GuiGraphics g, int id, int cx, int cy, int c) {
        switch (id) {
            case I_PLUS -> { g.fill(cx - 4, cy - 1, cx + 5, cy + 1, c); g.fill(cx - 1, cy - 4, cx + 1, cy + 5, c); }
            case I_MINUS -> g.fill(cx - 4, cy - 1, cx + 5, cy + 1, c);
            case I_TARGET -> {
                disc(g, cx, cy, 5, c);
                disc(g, cx, cy, 3, 0xFF2B1D0C);
                g.fill(cx - 1, cy - 1, cx + 1, cy + 1, c);
                g.fill(cx, cy - 7, cx + 1, cy - 4, c); g.fill(cx, cy + 5, cx + 1, cy + 8, c);
                g.fill(cx - 7, cy, cx - 4, cy + 1, c); g.fill(cx + 5, cy, cx + 8, cy + 1, c);
            }
            case I_PIN -> {
                disc(g, cx, cy - 2, 3, c);
                for (int i = 0; i < 5; i++) { int hw = Math.max(0, 3 - i); g.fill(cx - hw, cy + 1 + i, cx + hw + 1, cy + 2 + i, c); }
                g.fill(cx - 1, cy - 3, cx + 1, cy - 1, 0xFF2B1D0C);
            }
            case I_SCROLL -> {
                g.fill(cx - 4, cy - 4, cx + 4, cy + 4, c);
                g.fill(cx - 5, cy - 5, cx + 5, cy - 3, c); g.fill(cx - 5, cy + 3, cx + 5, cy + 5, c);
                g.fill(cx - 2, cy - 1, cx + 3, cy, 0xFF2B1D0C); g.fill(cx - 2, cy + 1, cx + 2, cy + 2, 0xFF2B1D0C);
            }
            case I_CLOSE -> {
                for (int i = -4; i <= 4; i++) {
                    g.fill(cx + i, cy + i, cx + i + 2, cy + i + 1, c);
                    g.fill(cx + i, cy - i, cx + i + 2, cy - i + 1, c);
                }
            }
            case I_CHECK -> {
                g.fill(cx - 4, cy, cx - 2, cy + 2, c); g.fill(cx - 3, cy + 1, cx - 1, cy + 3, c);
                for (int i = 0; i < 6; i++) g.fill(cx - 1 + i, cy + 2 - i, cx + 1 + i, cy + 3 - i, c);
            }
            case I_LOCK -> {
                g.fill(cx - 3, cy - 1, cx + 4, cy + 5, c);
                g.fill(cx - 2, cy - 4, cx + 3, cy - 3, c); g.fill(cx - 2, cy - 3, cx - 1, cy - 1, c); g.fill(cx + 2, cy - 3, cx + 3, cy - 1, c);
                g.fill(cx, cy + 1, cx + 1, cy + 3, 0xFF2B1D0C);
            }
            case I_FLAG -> {
                g.fill(cx - 3, cy - 5, cx - 2, cy + 5, c);
                g.fill(cx - 2, cy - 5, cx + 5, cy + 1, c);
                g.fill(cx + 3, cy - 3, cx + 5, cy - 1, 0xFF2B1D0C);
            }
            case I_BOOK -> {
                g.fill(cx - 5, cy - 4, cx + 5, cy + 5, c);
                g.fill(cx, cy - 4, cx + 1, cy + 5, 0xFF2B1D0C);
                g.fill(cx - 4, cy - 2, cx - 1, cy - 1, 0xFF2B1D0C); g.fill(cx + 2, cy - 2, cx + 5, cy - 1, 0xFF2B1D0C);
            }
            default -> { }
        }
    }

    /** Arbre blanc de la Hautemarche (embleme). */
    static void tree(GuiGraphics g, int cx, int cy, int c) {
        g.fill(cx - 1, cy + 2, cx + 1, cy + 7, c);
        for (int i = 0; i < 5; i++) g.fill(cx - 1 - i, cy - 6 + i * 2, cx + 2 + i, cy - 4 + i * 2, c);
        g.fill(cx - 4, cy + 2, cx + 5, cy + 3, c);
        g.fill(cx - 5, cy + 7, cx - 1, cy + 8, c); g.fill(cx + 1, cy + 7, cx + 5, cy + 8, c);
    }
}
