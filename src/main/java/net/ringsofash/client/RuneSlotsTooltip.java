package net.ringsofash.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.ringsofash.item.Rune;
import net.ringsofash.item.RuneSlots;

/**
 * Dessine les emplacements de runes dans l'infobulle : une case de 18 px par emplacement,
 * les cases d'une meme rune collees ensemble avec son icone au milieu, les cases libres
 * vides et sombres.
 */
public class RuneSlotsTooltip implements ClientTooltipComponent {
    private static final int CELL = 18, PAD = 1;

    private final RuneSlots data;

    public RuneSlotsTooltip(RuneSlots data) { this.data = data; }

    private int cells() { return Math.max(data.slots(), Rune.used(data.runes())); }

    @Override
    public int getHeight() { return CELL + 2; }

    @Override
    public int getWidth(Font font) { return Math.max(1, cells()) * CELL; }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        int i = 0;
        for (Rune r : data.runes()) {
            int w = r.cost() * CELL;
            Integer c = r.color().getColor();
            int col = c == null ? 0xFFFFFFFF : 0xFF000000 | c;
            frame(g, x + i * CELL, y, w, col, shade(col));
            ItemStack icon = r.stack();
            if (!icon.isEmpty()) g.renderItem(icon, x + i * CELL + (w - 16) / 2, y + 1);
            i += r.cost();
        }
        for (; i < cells(); i++) frame(g, x + i * CELL, y, CELL, 0xFF55505E, 0xFF221F28);
    }

    /** une case : bord clair, fond sombre */
    private static void frame(GuiGraphics g, int x, int y, int w, int border, int fill) {
        g.fill(x, y, x + w - PAD, y + CELL, border);
        g.fill(x + 1, y + 1, x + w - PAD - 1, y + CELL - 1, fill);
    }

    /** la meme couleur, trois fois plus sombre : le fond de la case */
    private static int shade(int argb) {
        int r = (argb >> 16) & 0xFF, gg = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return 0xFF000000 | ((r / 4) << 16) | ((gg / 4) << 8) | (b / 4);
    }
}
