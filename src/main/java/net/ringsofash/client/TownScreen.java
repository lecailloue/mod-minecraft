package net.ringsofash.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.data.Net.ClientLore;
import net.ringsofash.data.People;
import net.ringsofash.world.MapData;
import net.ringsofash.world.Towns;

/**
 * La fenetre d'une ville, ouverte d'un clic sur son nom depuis la carte.
 *
 * Le fond est la texture dessinee a la main : on ne redessine rien par-dessus, on se
 * contente de poser le plan de la ville dans son cadre, son nom a la place du mot
 * efface, les quetes dans la colonne de droite et le remplissage de la barre du bas.
 */
public class TownScreen extends Screen {
    private static final ResourceLocation TEX = RingsOfAsh.id("textures/gui/ville.png");
    private static final int W = 176, H = 166;

    // les zones relevees sur la texture elle-meme
    private static final int MAP_X = 13, MAP_Y = 11, MAP_W = 101, MAP_H = 72;
    private static final int NAME_X = 63, NAME_Y = 88;          // centre du mot efface
    private static final float NAME_SCALE = 1.4F;
    private static final int QUEST_X = 121, QUEST_Y = 26, QUEST_W = 50;
    private static final int TITLE_X = 121, TITLE_Y = 8;        // a la place du mot efface
    private static final int BAR_X = 13, BAR_Y = 144, BAR_W = 150, BAR_H = 9;
    /** le remplissage laisse voir les graduations de la barre */
    private static final int FILL = 0xB4C9A04A;

    private final String key;
    private final int wx, wz;
    private final People who;
    private int x0, y0;

    public TownScreen(String key, int wx, int wz) {
        super(Component.literal(MapScreen.pretty(key)));
        this.key = key;
        this.wx = wx;
        this.wz = wz;
        People o = Towns.owner(wx, wz);
        this.who = o == null ? People.HUMAN : o;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        x0 = (this.width - W) / 2;
        y0 = (this.height - H) / 2;
        g.blit(TEX, x0, y0, 0, 0, W, H, 256, 256);

        plan(g);
        name(g);
        title(g);
        quests(g);
        bar(g);
    }

    /** le plan de la ville, dans son cadre : ce qu'on en a deja decouvert */
    private void plan(GuiGraphics g) {
        ResourceLocation map = MapScreen.ensureTexture();
        if (map == null) return;
        int span = Math.max(80, Towns.radius(key) * 2 + 60);
        double u = (wx - span / 2.0 + MapData.HALF) / MapData.SIZE * MapScreen.TEX;
        double v = (wz - span / 2.0 + MapData.HALF) / MapData.SIZE * MapScreen.TEX;
        int uw = Math.max(1, (int) Math.round(span / (double) MapData.SIZE * MapScreen.TEX));
        g.blit(map, x0 + MAP_X, y0 + MAP_Y, MAP_W, MAP_H, (float) u, (float) v, uw, uw, MapScreen.TEX, MapScreen.TEX);
        Ui.icon(g, Ui.I_PIN, x0 + MAP_X + MAP_W / 2, y0 + MAP_Y + MAP_H / 2 - 4, 0xFFFFD070);
    }

    /** le nom de la ville, a la place du mot que portait la texture */
    private void name(GuiGraphics g) {
        String n = MapScreen.pretty(key);
        float w = this.font.width(n) * NAME_SCALE;
        g.pose().pushPose();
        g.pose().translate(x0 + NAME_X - w / 2F, y0 + NAME_Y, 0);
        g.pose().scale(NAME_SCALE, NAME_SCALE, 1F);
        g.drawString(this.font, n, 0, 0, 0xFFC9A04A, false);
        g.pose().popPose();
    }

    /** « Quêtes », dans la meme ecriture que le nom de la ville */
    private void title(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(x0 + TITLE_X, y0 + TITLE_Y, 0);
        g.pose().scale(NAME_SCALE, NAME_SCALE, 1F);
        g.drawString(this.font, "Quêtes", 0, 0, 0xFFC9A04A, false);
        g.pose().popPose();
    }

    private void quests(GuiGraphics g) {
        int y = y0 + QUEST_Y;
        List<String[]> qs = list();
        if (qs.isEmpty()) {
            g.drawString(this.font, "Rien à faire", x0 + QUEST_X, y, 0xFF8A7B5E, false);
            return;
        }
        for (String[] q : qs) {
            if (y > y0 + 130) break;
            for (var line : this.font.split(Component.literal(q[0]), QUEST_W)) {
                if (y > y0 + 130) break;
                g.drawString(this.font, line, x0 + QUEST_X, y, Integer.parseInt(q[1], 16) | 0xFF000000, false);
                y += 9;
            }
            y += 2;
        }
    }

    /** la barre du bas se remplit avec la reputation du peuple qui habite ici */
    private void bar(GuiGraphics g) {
        int rep = ClientLore.rep[who.ordinal()];
        int clamped = Math.max(-100, Math.min(100, rep));
        int filled = Math.round((clamped + 100) / 200F * BAR_W);
        if (filled > 0) g.fill(x0 + BAR_X, y0 + BAR_Y, x0 + BAR_X + filled, y0 + BAR_Y + BAR_H, FILL);
        String txt = who.label + " — " + People.rank(rep) + " (" + rep + ")";
        g.drawString(this.font, txt, x0 + BAR_X + (BAR_W - this.font.width(txt)) / 2, y0 + BAR_Y + 1, 0xFFF4E6C0, true);
    }

    private List<String[]> list() {
        List<String[]> out = new ArrayList<>();
        int rep = ClientLore.rep[who.ordinal()];
        out.add(new String[]{(rep >= 15 ? "✔ " : "") + "Leur confiance", rep >= 15 ? "7FD68A" : "D8CCA8"});
        out.add(new String[]{(ClientLore.quest >= 1 ? "✔ " : "") + "L'Anneau", ClientLore.quest >= 1 ? "7FD68A" : "D8CCA8"});
        out.add(new String[]{(ClientLore.quest >= 2 ? "✔ " : "") + "Mont Funeste", ClientLore.quest >= 2 ? "7FD68A" : "D8CCA8"});
        if (rep <= -15) out.add(new String[]{"Ville fermée", "E09050"});
        return out;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
