package net.ringsofash.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ringsofash.data.ModData;
import net.ringsofash.data.Net;
import net.ringsofash.data.PlayerLore;
import net.ringsofash.item.RingItem;

/** Onglet d'anneaux : 10 emplacements ranges dans les donnees du joueur ; seul l'anneau actif (menu Alt) agit. */
public class RingsMenu extends AbstractContainerMenu {
    public static final int COLS = 5;
    private final Container rings;

    public RingsMenu(int id, Inventory pinv) { this(id, pinv, new SimpleContainer(PlayerLore.RING_SLOTS)); }

    public RingsMenu(int id, Inventory pinv, Container rings) {
        super(ModMenus.RINGS.get(), id);
        this.rings = rings;
        for (int i = 0; i < PlayerLore.RING_SLOTS; i++) {
            addSlot(new Slot(rings, i, 44 + (i % COLS) * 18, 36 + (i / COLS) * 22) {
                @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof RingItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c + r * 9 + 9, 8 + c * 18, 98 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c, 8 + c * 18, 156));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        super.clicked(slot, button, type, player);
        changed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem(), copy = st.copy();
        boolean ok = index < PlayerLore.RING_SLOTS ? moveItemStackTo(st, PlayerLore.RING_SLOTS, slots.size(), true)
                : st.getItem() instanceof RingItem && moveItemStackTo(st, 0, PlayerLore.RING_SLOTS, false);
        if (!ok) return ItemStack.EMPTY;
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        changed(player);
    }

    private void changed(Player player) {
        if (player instanceof ServerPlayer sp) { ModData.lore(sp).fixActiveRing(); Net.sync(sp); }
    }

    /** Ouvre l'onglet d'anneaux. */
    public static void open(ServerPlayer sp) {
        PlayerLore l = ModData.lore(sp);
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new RingsMenu(id, inv, l.rings), Component.literal("Anneaux")));
    }

    /** Choisit l'anneau actif (menu Alt). */
    public static void select(ServerPlayer sp, int slot) {
        PlayerLore l = ModData.lore(sp);
        if (slot >= PlayerLore.RING_SLOTS || (slot >= 0 && !(l.rings.getItem(slot).getItem() instanceof RingItem))) return;
        if (slot == l.activeRing) return;
        l.activeRing = slot;
        Net.sync(sp);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
        if (slot < 0) sp.displayClientMessage(Component.literal("Aucun anneau actif.").withStyle(ChatFormatting.GRAY), true);
        else {
            ItemStack st = l.rings.getItem(slot);
            sp.displayClientMessage(Component.empty().append(st.getHoverName()).append(Component.literal(" : " + ((RingItem) st.getItem()).kind.power)).withStyle(ChatFormatting.GOLD), true);
        }
    }
}
