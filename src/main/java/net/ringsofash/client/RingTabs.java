package net.ringsofash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ringsofash.data.Net;
import net.ringsofash.gen.Reg;

/** Les deux onglets sur le cote gauche de l'inventaire : Inventaire, Anneaux et Artefacts. */
final class RingTabs {
    private RingTabs() {}

    static final int W = 28, H = 26;

    static void init() {
        NeoForge.EVENT_BUS.addListener(RingTabs::render);
        NeoForge.EVENT_BUS.addListener(RingTabs::click);
    }

    /** 4 onglets, plus le four portable une fois la competence apprise */
    static int count() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && net.ringsofash.menu.PocketFurnaceMenu.unlocked(mc.player) ? 5 : 4;
    }

    private static int tabX(AbstractContainerScreen<?> s) { return s.getGuiLeft() - W + 2; }
    private static int tabY(AbstractContainerScreen<?> s, int i) { return s.getGuiTop() + 6 + i * (H + 3); }

    static void draw(GuiGraphics g, AbstractContainerScreen<?> s, int tab, int mx, int my) { drawAt(g, s.getGuiLeft(), s.getGuiTop(), tab, mx, my); }

    /** Colonne d'onglets a gauche d'un cadre dont le coin haut-gauche est (left, top). */
    static void drawAt(GuiGraphics g, int left, int top, int tab, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack[] icons = {new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Reg.RING_HOARD.get()), new ItemStack(Reg.ARTIFACT_CHAIN_1.get()), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.FURNACE)};
        String[] names = {"Inventaire", "Anneaux", "Artefacts", "Compétences", "Four portable"};
        for (int i = 0; i < count(); i++) {
            int x = left - W + 2, y = top + 6 + i * (H + 3);
            boolean on = i == tab, hover = Ui.in(mx, my, x, y, W, H);
            g.fill(x, y, x + W, y + H, Ui.BRONZE_D);
            g.fill(x + 1, y + 1, x + W, y + H - 1, on ? Ui.GOLD : Ui.BRONZE);
            g.fill(x + 2, y + 2, x + W, y + H - 2, on ? 0xFF7A5A22 : hover ? 0xFF5A4220 : 0xFF3A2A14);
            g.renderItem(icons[i], x + 5, y + 5);
        }
        for (int i = 0; i < count(); i++) {
            if (Ui.in(mx, my, left - W + 2, top + 6 + i * (H + 3), W, H)) g.renderTooltip(mc.font, Component.literal(names[i]), mx, my);
        }
    }

    private static boolean ours(AbstractContainerScreen<?> s) { return s instanceof InventoryScreen; }

    private static void render(ScreenEvent.Render.Post e) {
        if (e.getScreen() instanceof InventoryScreen s) draw(e.getGuiGraphics(), s, 0, e.getMouseX(), e.getMouseY());
    }

    private static void click(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof InventoryScreen s) || e.getButton() != 0) return;
        if (Ui.in(e.getMouseX(), e.getMouseY(), tabX(s), tabY(s, 1), W, H)) { PacketDistributor.sendToServer(new Net.OpenRings()); e.setCanceled(true); }
        if (Ui.in(e.getMouseX(), e.getMouseY(), tabX(s), tabY(s, 2), W, H)) { PacketDistributor.sendToServer(new Net.OpenArtifacts()); e.setCanceled(true); }
        if (Ui.in(e.getMouseX(), e.getMouseY(), tabX(s), tabY(s, 3), W, H)) { Minecraft.getInstance().setScreen(new SkillScreen()); e.setCanceled(true); }
        if (count() > 4 && Ui.in(e.getMouseX(), e.getMouseY(), tabX(s), tabY(s, 4), W, H)) { PacketDistributor.sendToServer(new Net.OpenFurnace()); e.setCanceled(true); }
    }

    /** clic sur un onglet depuis un de nos ecrans conteneur (current = onglet affiche) ; renvoie true si le clic est traite */
    static boolean clickFrom(AbstractContainerScreen<?> s, double mx, double my, int current) {
        return clickAt(s.getGuiLeft(), s.getGuiTop(), mx, my, current, true);
    }

    /** meme chose pour un cadre quelconque ; container = l'ecran actuel est un menu a fermer cote serveur */
    static boolean clickAt(int left, int top, double mx, double my, int current, boolean container) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (int i = 0; i < count(); i++) {
            if (i == current || !Ui.in(mx, my, left - W + 2, top + 6 + i * (H + 3), W, H)) continue;
            if (i == 0) { if (container) mc.player.closeContainer(); mc.setScreen(new InventoryScreen(mc.player)); }
            else if (i == 3) { if (container) mc.player.closeContainer(); mc.setScreen(new SkillScreen()); }
            else if (i == 4) PacketDistributor.sendToServer(new Net.OpenFurnace());
            else PacketDistributor.sendToServer(i == 1 ? new Net.OpenRings() : new Net.OpenArtifacts());
            return true;
        }
        return false;
    }
}
