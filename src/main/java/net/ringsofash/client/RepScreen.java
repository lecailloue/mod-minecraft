package net.ringsofash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ringsofash.data.Net.ClientLore;
import net.ringsofash.data.People;

/** Menu de reputation (touche R). */
public class RepScreen extends Screen {
    public RepScreen() { super(Component.literal("Réputation")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        int w = 250, h = 206;
        int x0 = (this.width - w) / 2, y0 = (this.height - h) / 2;
        g.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, 0xFF6B5330);
        g.fill(x0, y0, x0 + w, y0 + h, 0xF0201A14);
        g.drawCenteredString(this.font, "§6§lRéputation dans la Hautemarche", this.width / 2, y0 + 8, 0xFFFFFF);
        int sum = 0;
        int y = y0 + 30;
        for (People p : People.values()) {
            int rep = p.ordinal() < ClientLore.rep.length ? ClientLore.rep[p.ordinal()] : 0;
            sum += rep;
            g.drawString(this.font, p.label, x0 + 12, y, 0xFF000000 | p.color);
            g.drawString(this.font, People.rank(rep) + " (" + rep + ")", x0 + w - 12 - this.font.width(People.rank(rep) + " (" + rep + ")"), y, 0xDDDDDD);
            bar(g, x0 + 12, y + 11, w - 24, rep, 0xFF000000 | p.color);
            y += 28;
        }
        int general = Math.round(sum / (float) People.values().length);
        g.drawString(this.font, "§lRéputation générale", x0 + 12, y + 2, 0xFFD070);
        String gr = People.rank(general) + " (" + general + ")";
        g.drawString(this.font, gr, x0 + w - 12 - this.font.width(gr), y + 2, 0xFFFFFF);
        bar(g, x0 + 12, y + 13, w - 24, general, 0xFFFFD070);
        y += 28;
        String end = general < 0 ? "§cVoie de l'ombre : armure et masse du Seigneur de Cendre" : "§aVoie de la lumière : armure et épée du roi elfe";
        g.drawCenteredString(this.font, "Récompense finale promise :", this.width / 2, y, 0xAAAAAA);
        g.drawCenteredString(this.font, end, this.width / 2, y + 11, 0xFFFFFF);
        String quest = switch (ClientLore.quest) {
            case 0 -> "Quête : trouver l'Anneau de Cendre (mines naines)";
            case 1 -> "Quête : jeter l'Anneau dans le Mont Funeste";
            case 2 -> "Quête : vaincre le Seigneur de Cendre";
            default -> "Quête accomplie";
        };
        g.drawCenteredString(this.font, quest, this.width / 2, y + 26, 0xE0C080);
        g.drawCenteredString(this.font, "Corruption : " + (ClientLore.corruption / 10) + " %   Œil : " + Math.min(100, ClientLore.eye * 100 / 120) + " %", this.width / 2, y + 38,
                ClientLore.corruption > 500 ? 0xFF5050 : 0x999999);
    }

    private void bar(GuiGraphics g, int x, int y, int w, int value, int color) {
        g.fill(x, y, x + w, y + 6, 0xFF0A0806);
        int mid = x + w / 2;
        int len = Math.round((w / 2F) * Math.abs(value) / 100F);
        if (value >= 0) g.fill(mid, y + 1, mid + len, y + 5, color);
        else g.fill(mid - len, y + 1, mid, y + 5, 0xFFC03030);
        g.fill(mid, y, mid + 1, y + 6, 0xFFFFFFFF);
    }
}
