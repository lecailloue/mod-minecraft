package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ringsofash.RingsOfAsh;
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

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RingsOfAsh.MODID, "textures/gui/artifacts.png");

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, lv = level();
        g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i = 0; i < Artifacts.SLOTS && i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            int sx = x + s.x - 1, sy = y + s.y - 1;
            if (i == 0) {                                                    // la chaine : cadre d'or
                g.fill(sx - 1, sy - 1, sx + 19, sy, Ui.GOLD_L);
                g.fill(sx - 1, sy + 18, sx + 19, sy + 19, Ui.GOLD_L);
                g.fill(sx - 1, sy, sx, sy + 18, Ui.GOLD_L);
                g.fill(sx + 18, sy, sx + 19, sy + 18, Ui.GOLD_L);
            } else if (i > lv) {                                             // case pas encore ouverte : voilee, cadenas
                g.fill(sx, sy, sx + 18, sy + 18, 0xCC15100A);
                Ui.icon(g, Ui.I_LOCK, sx + 9, sy + 9, Ui.GOLD);
            }
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
