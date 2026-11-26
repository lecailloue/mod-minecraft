package net.ringsofash.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ringsofash.item.Rune;
import net.ringsofash.item.Runes;
import net.ringsofash.menu.RunicMenu;

/** Fenetre de la table runique : l'arme, la rune, l'orrune, et ce que ca va couter. */
public class RunicScreen extends AbstractContainerScreen<RunicMenu> {
    private final Button[] buttons = new Button[3];
    private static final String[] LABELS = {"Sceller", "Fusionner", "Améliorer"};

    public RunicScreen(RunicMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
        this.inventoryLabelY = 105;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            final int id = i;
            buttons[i] = addRenderableWidget(Button.builder(Component.literal(LABELS[i]), b -> {
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }).bounds(leftPos + 74, topPos + 20 + i * 22, 94, 18).build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 3; i++) {
            if (buttons[i] != null) buttons[i].active = minecraft != null && minecraft.player != null
                    && menu.refuse(i, minecraft.player) == null;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF14110E);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF463F4A);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 102, 0xFF332E38);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + 5, 0xFF6A5C78);
        for (Slot s : menu.slots) g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF1A1820);
        for (Slot s : menu.slots) g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF575160);

        ItemStack w = menu.weapon();
        // les emplacements de l'arme, a droite de sa case
        if (Runes.runic(w)) {
            int slots = Rune.slots(w), used = Rune.used(w), by = y + 27;
            String count = used + "/" + slots;
            g.drawString(font, count, x + 30, by - 1, 0xFF9A90A8, false);
            int bx = x + 32 + font.width(count);
            int i = 0;
            for (Rune r : Rune.on(w)) {
                Integer c = r.color().getColor();
                for (int k = 0; k < r.cost(); k++, i++) box(g, bx + i * 6, by, c == null ? 0xFFFFFFFF : 0xFF000000 | c);
            }
            for (; i < slots; i++) box(g, bx + i * 6, by, 0xFF4A4452);
        } else {
            g.drawString(font, "Arme", x + 30, y + 26, 0xFF9A90A8, false);
        }
        g.drawString(font, "Rune", x + 8, y + 42, 0xFF9A90A8, false);
        g.drawString(font, "Orrune", x + 34, y + 42, 0xFF9A90A8, false);

        // le cout du geste possible, ou la raison du refus
        List<String> lines = status();
        for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 8, y + 84 + 10 * i, 0xFFD0C4E0, false);
    }

    private List<String> status() {
        List<String> out = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) return out;
        for (int i = 0; i < 3 && out.size() < 2; i++) {
            if (menu.refuse(i, minecraft.player) == null) {
                int xp = RunicMenu.xpCost(i, menu.weapon(), menu.second());
                int ore = RunicMenu.oreCost(i, menu.weapon(), menu.second());
                out.add(LABELS[i] + " : " + xp + " niveaux, " + ore + " orrune");
            }
        }
        if (out.isEmpty()) {
            String why = menu.refuse(RunicMenu.SEAL, minecraft.player);
            if (why != null) out.addAll(wrap(why, 160).subList(0, Math.min(2, wrap(why, 160).size())));
        }
        return out;
    }

    private List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String probe = cur.isEmpty() ? word : cur + " " + word;
            if (font.width(probe) > width && !cur.isEmpty()) { out.add(cur.toString()); cur = new StringBuilder(word); }
            else cur = new StringBuilder(probe);
        }
        if (!cur.isEmpty()) out.add(cur.toString());
        return out;
    }

    private static void box(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 5, y + 5, 0xFF1A1820);
        g.fill(x + 1, y + 1, x + 4, y + 4, color);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
