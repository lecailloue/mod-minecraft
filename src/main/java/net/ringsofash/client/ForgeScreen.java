package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ringsofash.menu.ForgeMenu;
import net.ringsofash.menu.ForgeUpgrade;

public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private Button button;

    public ForgeScreen(ForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = 89;
    }

    @Override
    protected void init() {
        super.init();
        button = addRenderableWidget(Button.builder(Component.literal("Améliorer"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 88, topPos + 58, 74, 20).build());
    }

    private int level() {
        return minecraft == null || minecraft.level == null ? 0 : ForgeUpgrade.level(menu.tool(), minecraft.level.registryAccess());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int lvl = level();
        boolean ok = ForgeUpgrade.canForge(menu.tool()) && lvl < ForgeUpgrade.MAX && menu.ingots().getCount() >= ForgeUpgrade.COSTS[Math.min(lvl, ForgeUpgrade.MAX - 1)];
        if (button != null) button.active = ok;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF14110E);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF4A4642);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 88, 0xFF35322F);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 5, 0xFF6A645C);
        for (Slot s : menu.slots) g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF1C1A18);
        for (Slot s : menu.slots) g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF5B5651);
        g.drawString(font, "Arme / armure", x + 58, y + 40, 0xFFD8C8A8, false);
        g.drawString(font, "Lingots", x + 58, y + 66, 0xFFD8C8A8, false);
        int lvl = level();
        if (!ForgeUpgrade.canForge(menu.tool())) {
            g.drawString(font, "Posez une arme ou armure", x + 88, y + 20, 0xFFB0A090, false);
        } else if (lvl >= ForgeUpgrade.MAX) {
            g.drawString(font, (minecraft != null && minecraft.level != null && ForgeUpgrade.isMega(menu.tool(), minecraft.level.registryAccess())) ? "Trempe ancestrale !" : "Niveau maximal !", x + 88, y + 22, 0xFFFFD070, false);
        } else {
            g.drawString(font, "Trempe : " + lvl + " / " + ForgeUpgrade.MAX, x + 88, y + 16, 0xFFFFD070, false);
            g.drawString(font, "Coût : " + ForgeUpgrade.COSTS[lvl] + " lingot" + (ForgeUpgrade.COSTS[lvl] > 1 ? "s" : ""), x + 88, y + 28, 0xFFE8E0D0, false);
            g.drawString(font, "Réussite : " + Math.round((1 - ForgeUpgrade.FAIL_CHANCE) * 100) + " %", x + 88, y + 40, 0xFFE8E0D0, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
