package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ringsofash.item.Artifacts;
import net.ringsofash.menu.ArtifactMenu;

/** Onglet d'artefacts : une case de chaine, puis jusqu'a quatre cases d'artefacts ouvertes par le niveau de la chaine. */
public class ArtifactScreen extends AbstractContainerScreen<ArtifactMenu> {
    public ArtifactScreen(ArtifactMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelY = 86;
    }

    private int level() { return Artifacts.level(menu.slots.get(0).container); }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, lv = level();
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF14110E);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF4A4642);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 82, 0xFF35322F);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 5, 0xFF6A645C);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                g.fill(x + s.x - 2, y + s.y - 2, x + s.x + 18, y + s.y + 18, Ui.GOLD_L);
                g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, Ui.GOLD);
            } else if (i < Artifacts.SLOTS) {
                boolean on = i <= lv;
                g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, on ? 0xFF1C1A18 : 0xFF232120);
                g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, on ? 0xFF5B5651 : 0xFF3B3835);
                if (!on) { g.fill(x + s.x + 4, y + s.y + 7, x + s.x + 12, y + s.y + 12, 0xFF2A2826); g.fill(x + s.x + 6, y + s.y + 3, x + s.x + 10, y + s.y + 7, 0xFF2A2826); }
                continue;
            } else g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF1C1A18);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF5B5651);
        }
        g.drawCenteredString(font, lv == 0 ? "Une chaîne ouvre les cases" : "Chaîne niveau " + lv + " : " + lv + " case" + (lv > 1 ? "s" : ""), x + imageWidth / 2, y + 48, 0xFFD8C8A8);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFE8D8B0, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFD8C8A8, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        RingTabs.draw(g, this, 2, mx, my);
        renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && RingTabs.clickFrom(this, mx, my, 2)) return true;
        return super.mouseClicked(mx, my, button);
    }
}
