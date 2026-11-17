package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ringsofash.data.Net;
import net.ringsofash.menu.RingsMenu;

/** Onglet d'anneaux : 10 emplacements, l'anneau actif est entoure d'or. */
public class RingsScreen extends AbstractContainerScreen<RingsMenu> {
    public RingsScreen(RingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelY = 86;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF14110E);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF4A4642);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 82, 0xFF35322F);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 5, 0xFF6A645C);
        for (int i = 0; i < 10; i++) {
            Slot s = menu.slots.get(i);
            boolean on = i == Net.ClientRings.active;
            g.fill(x + s.x - 2, y + s.y - 2, x + s.x + 18, y + s.y + 18, on ? Ui.GOLD_L : 0xFF1C1A18);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, on ? Ui.GOLD : 0xFF2B2927);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF5B5651);
        }
        for (int i = 10; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF1C1A18);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF5B5651);
        }
        g.drawCenteredString(font, "Alt : choisir l'anneau actif", x + imageWidth / 2, y + 20, 0xFFD8C8A8);
        g.drawCenteredString(font, "Seul l'anneau actif agit", x + imageWidth / 2, y + 73, 0xFF9C9486);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFE8D8B0, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFD8C8A8, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        RingTabs.draw(g, this, 1, mx, my);
        renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && RingTabs.clickFrom(this, mx, my, 1)) return true;
        return super.mouseClicked(mx, my, button);
    }
}
