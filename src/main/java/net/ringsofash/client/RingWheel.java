package net.ringsofash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ringsofash.data.Net;
import net.ringsofash.item.RingItem;

import java.util.ArrayList;
import java.util.List;

/** Petit menu en roue, ouvert tant que Alt est maintenu : on survole un anneau et on relache (ou on clique) pour le rendre actif. */
public class RingWheel extends Screen {
    /** apres un clic, on attend que Alt soit relache avant de pouvoir rouvrir la roue */
    static boolean waitRelease;
    /** tests automatiques : la roue reste ouverte sans toucher Alt */
    static boolean keepOpen;
    private final List<Integer> slots = new ArrayList<>();
    private int hover = -2;                                  // indice dans slots, -1 = "aucun anneau", -2 = rien
    private static final int NONE_W = 96, NONE_H = 16;

    public RingWheel() {
        super(Component.literal("Anneaux"));
        for (int i = 0; i < Net.ClientRings.stacks.length; i++) if (Net.ClientRings.stacks[i].getItem() instanceof RingItem) slots.add(i);
    }

    static boolean hasRings() {
        for (ItemStack s : Net.ClientRings.stacks) if (s.getItem() instanceof RingItem) return true;
        return false;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { g.fill(0, 0, width, height, 0x66000000); }

    private int radius() { return slots.size() > 7 ? 70 : 58; }
    private int noneX() { return width / 2 - NONE_W / 2; }
    private int noneY() { return height / 2 + radius() + 62; }

    private void pick(double mx, double my) {
        hover = -2;
        if (Ui.in(mx, my, noneX(), noneY(), NONE_W, NONE_H)) { hover = -1; return; }
        double dx = mx - width / 2.0, dy = my - height / 2.0, d = Math.sqrt(dx * dx + dy * dy);
        if (d < 18 || d > radius() + 40 || slots.isEmpty()) return;
        double ang = Math.toDegrees(Math.atan2(dy, dx)) + 90;                  // 0 = en haut, sens horaire
        double step = 360.0 / slots.size();
        ang = ((ang % 360) + 360) % 360;
        hover = (int) Math.round(ang / step) % slots.size();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        pick(mx, my);
        int cx = width / 2, cy = height / 2, r = radius();
        Ui.disc(g, cx, cy, r + 26, 0x55100C08);
        Ui.disc(g, cx, cy, 20, 0x88000000);
        g.drawCenteredString(font, "Alt", cx, cy - 4, 0xFF9C8A5C);
        for (int k = 0; k < slots.size(); k++) {
            double a = Math.toRadians(-90 + k * 360.0 / slots.size());
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            boolean on = slots.get(k) == Net.ClientRings.active, hv = k == hover;
            Ui.inset(g, x - 15, y - 15, 30, 30, hv ? 0xFF6A4E1E : 0xFF2C2216);
            if (on) { g.fill(x - 16, y - 16, x + 16, y - 14, Ui.GOLD_L); g.fill(x - 16, y + 14, x + 16, y + 16, Ui.GOLD_L); g.fill(x - 16, y - 16, x - 14, y + 16, Ui.GOLD_L); g.fill(x + 14, y - 16, x + 16, y + 16, Ui.GOLD_L); }
            else if (hv) { g.fill(x - 16, y - 16, x + 16, y - 15, 0xFFFFFFFF); g.fill(x - 16, y + 15, x + 16, y + 16, 0xFFFFFFFF); }
            g.pose().pushPose();
            g.pose().translate(x - 12, y - 12, 0);
            g.pose().scale(1.5F, 1.5F, 1);
            g.renderItem(Net.ClientRings.stacks[slots.get(k)], 0, 0);
            g.pose().popPose();
        }
        // texte : l'anneau survole, sinon l'anneau actif
        ItemStack info = hover >= 0 ? Net.ClientRings.stacks[slots.get(hover)] : Net.ClientRings.active();
        int ty = cy + r + 28;
        if (info.getItem() instanceof RingItem ri) {
            g.drawCenteredString(font, info.getHoverName(), cx, ty, 0xFFFFD070);
            g.drawCenteredString(font, ri.kind.power, cx, ty + 12, 0xFFE8E0D0);
            if (ri.kind.curse != null) g.drawCenteredString(font, ri.kind.curse, cx, ty + 24, 0xFFE05040);
            if (hover < 0 || slots.get(hover) == Net.ClientRings.active) g.drawCenteredString(font, "(actif)", cx, ty - 12, 0xFF9CE08A);
        } else {
            g.drawCenteredString(font, "Aucun anneau actif", cx, ty, 0xFFB0A090);
        }
        Ui.button(g, noneX(), noneY(), NONE_W, NONE_H, hover == -1, Net.ClientRings.active < 0, true);
        g.drawCenteredString(font, "Aucun anneau", cx, noneY() + 4, 0xFFF0E4C0);
    }

    private void commit() {
        if (hover >= 0) PacketDistributor.sendToServer(new Net.SelectRing(slots.get(hover)));
        else if (hover == -1) PacketDistributor.sendToServer(new Net.SelectRing(-1));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        pick(mx, my);
        if (hover != -2) { commit(); waitRelease = true; onClose(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void tick() {
        if (!keepOpen && !Screen.hasAltDown()) { commit(); onClose(); }
    }
}
