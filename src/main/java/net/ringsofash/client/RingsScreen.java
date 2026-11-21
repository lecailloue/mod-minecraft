package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ringsofash.RingsOfAsh;
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

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RingsOfAsh.MODID, "textures/gui/rings.png");

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i = 0; i < 10; i++) {                                       // l'anneau actif est entoure d'or
            if (i != Net.ClientRings.active) continue;
            Slot s = menu.slots.get(i);
            int sx = x + s.x - 1, sy = y + s.y - 1;
            g.fill(sx - 1, sy - 1, sx + 19, sy, Ui.GOLD_L);
            g.fill(sx - 1, sy + 18, sx + 19, sy + 19, Ui.GOLD_L);
            g.fill(sx - 1, sy, sx, sy + 18, Ui.GOLD_L);
            g.fill(sx + 18, sy, sx + 19, sy + 18, Ui.GOLD_L);
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
