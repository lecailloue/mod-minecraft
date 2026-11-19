package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ringsofash.menu.PocketFurnaceMenu;

/** Four portable : case a fondre, case combustible, flamme et fleche de cuisson ; le resultat part dans l'inventaire. */
public class PocketFurnaceScreen extends AbstractContainerScreen<PocketFurnaceMenu> {
    public PocketFurnaceScreen(PocketFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF14110E);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF4A4642);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 70, 0xFF35322F);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 5, 0xFF6A645C);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i < 2) { g.fill(x + s.x - 2, y + s.y - 2, x + s.x + 18, y + s.y + 18, Ui.GOLD_L); g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, Ui.GOLD); }
            else g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF1C1A18);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF5B5651);
        }
        // flamme : monte et descend avec la reserve de chaleur
        int bt = menu.burnTotal(), bl = menu.burn();
        int fx = x + 57, fy = y + 37, fh = 13;
        g.fill(fx - 1, fy - 1, fx + 13, fy + fh + 1, 0xFF1C1A18);
        g.fill(fx, fy, fx + 12, fy + fh, 0xFF2A2826);
        if (bl > 0 && bt > 0) {
            int h = Math.max(2, fh * bl / bt);
            g.fill(fx, fy + fh - h, fx + 12, fy + fh, 0xFFE8761C);
            g.fill(fx + 3, fy + fh - h * 2 / 3, fx + 9, fy + fh, 0xFFFFC94A);
        }
        // fleche de cuisson
        int ax = x + 80, ay = y + 34, aw = 24;
        int ct = menu.cookTotal(), cp = menu.cook();
        g.fill(ax - 1, ay - 1, ax + aw + 1, ay + 17, 0xFF1C1A18);
        g.fill(ax, ay, ax + aw, ay + 16, 0xFF2A2826);
        if (cp > 0 && ct > 0) g.fill(ax, ay, ax + aw * cp / ct, ay + 16, Ui.GOLD);
        g.drawString(font, "→ inventaire", x + 108, y + 38, 0xFFD8C8A8, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFE8D8B0, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFD8C8A8, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        RingTabs.draw(g, this, 4, mx, my);
        renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && RingTabs.clickFrom(this, mx, my, 4)) return true;
        return super.mouseClicked(mx, my, button);
    }
}
