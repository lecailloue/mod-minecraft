package net.ringsofash.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.Rune;
import net.ringsofash.item.Runes;

/**
 * Table runique. Trois cases : l'arme, une rune (ou une amelioration runique), et des
 * lingots d'orrune. Trois gestes :
 *   - Sceller  : la rune entre dans l'arme, pour toujours.
 *   - Fusionner: trois runes identiques donnent la rune du niveau au-dessus.
 *   - Ameliorer: une amelioration runique fait monter l'arme d'une rarete, donc d'un emplacement.
 * Rien n'est gratuit : chaque geste coute des niveaux d'experience et de l'orrune.
 */
public class RunicMenu extends AbstractContainerMenu {
    public static final int SEAL = 0, FUSE = 1, UPGRADE = 2;

    private final ContainerLevelAccess access;
    private final Container inv = new SimpleContainer(3) {
        @Override public void setChanged() { super.setChanged(); RunicMenu.this.slotsChanged(this); }
    };

    public RunicMenu(int id, Inventory pinv) { this(id, pinv, ContainerLevelAccess.NULL); }

    public RunicMenu(int id, Inventory pinv, ContainerLevelAccess access) {
        super(ModMenus.RUNIC.get(), id);
        this.access = access;
        addSlot(new Slot(inv, 0, 8, 22) {
            @Override public boolean mayPlace(ItemStack s) { return Runes.runic(s); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(inv, 1, 8, 52) {
            @Override public boolean mayPlace(ItemStack s) { return Rune.of(s) != null || s.is(Reg.RUNIC_UPGRADE.get()); }
        });
        addSlot(new Slot(inv, 2, 34, 52) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Reg.ORRUNE_INGOT.get()); }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c + r * 9 + 9, 8 + c * 18, 116 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c, 8 + c * 18, 174));
    }

    /** pour les captures d'ecran de ClientTest : remplit une case sans passer par le reseau */
    public void put(int i, ItemStack s) { inv.setItem(i, s); }

    public ItemStack weapon() { return inv.getItem(0); }
    public ItemStack second() { return inv.getItem(1); }
    public ItemStack ingots() { return inv.getItem(2); }

    // ------------------------------------------------------------------ couts

    /** niveaux d'experience demandes par le geste, -1 si le geste est impossible */
    public static int xpCost(int action, ItemStack weapon, ItemStack second) {
        Rune r = Rune.of(second);
        switch (action) {
            case SEAL -> { if (r == null) return -1; return r == Rune.BLACK ? 40 : 10 * r.level; }
            case FUSE -> { if (r == null || r.next() == null) return -1; return 5 * r.level; }
            case UPGRADE -> { return 25; }
            default -> { return -1; }
        }
    }

    /** lingots d'orrune demandes par le geste */
    public static int oreCost(int action, ItemStack weapon, ItemStack second) {
        Rune r = Rune.of(second);
        switch (action) {
            case SEAL -> { if (r == null) return -1; return r == Rune.BLACK ? 3 : r.level; }
            case FUSE -> { return 1; }
            case UPGRADE -> { return 2; }
            default -> { return -1; }
        }
    }

    /** nombre de runes a consommer pour une fusion */
    public static final int FUSE_COUNT = 3;

    /** pourquoi le geste est refuse, ou null s'il peut se faire */
    public String refuse(int action, Player player) {
        ItemStack w = weapon(), s = second();
        Rune r = Rune.of(s);
        switch (action) {
            case SEAL -> {
                String why = Rune.refuse(w, r);
                if (why != null) return why;
            }
            case FUSE -> {
                if (r == null) return "Posez trois runes identiques.";
                if (r.next() == null) return "Cette rune est déjà au niveau le plus haut.";
                if (s.getCount() < FUSE_COUNT) return "Il faut " + FUSE_COUNT + " runes identiques.";
            }
            case UPGRADE -> {
                if (!Runes.runic(w)) return "Posez une arme du mod.";
                if (!s.is(Reg.RUNIC_UPGRADE.get())) return "Posez une amélioration runique.";
                if (Runes.tier(w) >= 5) return "Cette arme est déjà ultime.";
            }
            default -> { return "Geste inconnu."; }
        }
        int xp = xpCost(action, w, s), ore = oreCost(action, w, s);
        if (!player.getAbilities().instabuild && player.experienceLevel < xp) return "Il faut " + xp + " niveaux d'expérience.";
        if (!player.getAbilities().instabuild && (!ingots().is(Reg.ORRUNE_INGOT.get()) || ingots().getCount() < ore))
            return "Il faut " + ore + " lingot" + (ore > 1 ? "s" : "") + " d'orrune.";
        return null;
    }

    // ------------------------------------------------------------------ gestes

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (id != SEAL && id != FUSE && id != UPGRADE) return false;
        String why = refuse(id, sp);
        if (why != null) { msg(sp, why, ChatFormatting.RED); return false; }

        ItemStack w = weapon(), s = second();
        int xp = xpCost(id, w, s), ore = oreCost(id, w, s);
        if (!sp.getAbilities().instabuild) {
            sp.giveExperienceLevels(-xp);
            ingots().shrink(ore);
        }

        switch (id) {
            case SEAL -> {
                Rune r = Rune.of(s);
                Rune.socket(w, r);
                s.shrink(1);
                msg(sp, r.label() + " scellée dans " + w.getHoverName().getString() + ".", ChatFormatting.GOLD);
                for (Rune.Combo c : Rune.combos(w)) msg(sp, "Combo " + c.label + " : " + c.text, ChatFormatting.LIGHT_PURPLE);
                effects(true);
            }
            case FUSE -> {
                Rune r = Rune.of(s), up = r.next();
                s.shrink(FUSE_COUNT);
                ItemStack made = up.stack();
                if (!sp.getInventory().add(made)) sp.drop(made, false);
                msg(sp, "Trois runes fondues en une : " + up.label() + ".", ChatFormatting.AQUA);
                effects(true);
            }
            case UPGRADE -> {
                int t = Math.min(5, Runes.tier(w) + 1);
                Runes.setTier(w, t);
                s.shrink(1);
                msg(sp, "L'arme devient " + Runes.NAMES[t].toLowerCase() + " : un emplacement de plus.", ChatFormatting.GOLD);
                effects(true);
            }
            default -> { }
        }
        inv.setChanged();
        broadcastChanges();
        return true;
    }

    private void effects(boolean ok) {
        access.execute((level, pos) -> {
            level.playSound(null, pos, ok ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.9F, ok ? 1.3F : 0.7F);
            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 24, 0.4, 0.3, 0.4, 0.4);
        });
    }

    private static void msg(ServerPlayer p, String text, ChatFormatting c) {
        p.displayClientMessage(Component.literal(text).withStyle(c), false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack st = slot.getItem();
            ret = st.copy();
            if (index < 3) {
                if (!moveItemStackTo(st, 3, slots.size(), true)) return ItemStack.EMPTY;
            } else if (st.is(Reg.ORRUNE_INGOT.get())) {
                if (!moveItemStackTo(st, 2, 3, false)) return ItemStack.EMPTY;
            } else if (Rune.of(st) != null || st.is(Reg.RUNIC_UPGRADE.get())) {
                if (!moveItemStackTo(st, 1, 2, false)) return ItemStack.EMPTY;
            } else if (Runes.runic(st)) {
                if (!moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
            } else return ItemStack.EMPTY;
            if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return ret;
    }

    @Override
    public boolean stillValid(Player player) { return stillValid(access, player, Reg.RUNIC_TABLE.get()); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inv));
    }
}
