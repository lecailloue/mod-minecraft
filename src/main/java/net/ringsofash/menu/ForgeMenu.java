package net.ringsofash.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.ringsofash.gen.Reg;

/** Fenetre de la forge naine : une arme ou un outil, des lingots de lunargent, un bouton Ameliorer. */
public class ForgeMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Container inv = new SimpleContainer(2) {
        @Override public void setChanged() { super.setChanged(); ForgeMenu.this.slotsChanged(this); }
    };

    public ForgeMenu(int id, Inventory pinv) { this(id, pinv, ContainerLevelAccess.NULL); }

    public ForgeMenu(int id, Inventory pinv, ContainerLevelAccess access) {
        super(ModMenus.FORGE.get(), id);
        this.access = access;
        addSlot(new Slot(inv, 0, 36, 36) {
            @Override public boolean mayPlace(ItemStack s) { return ForgeUpgrade.canForge(s); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(inv, 1, 36, 62) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Reg.LUNARGENT_INGOT.get()); }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c + r * 9 + 9, 8 + c * 18, 100 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c, 8 + c * 18, 158));
    }

    public ItemStack tool() { return inv.getItem(0); }
    public ItemStack ingots() { return inv.getItem(1); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !(player instanceof ServerPlayer sp)) return false;
        ItemStack tool = tool(), ing = ingots();
        if (!ForgeUpgrade.canForge(tool)) { msg(sp, "Placez une arme, un outil ou une armure dans la forge.", ChatFormatting.RED); return false; }
        int lvl = ForgeUpgrade.level(tool, sp.level().registryAccess());
        if (lvl >= ForgeUpgrade.MAX) { msg(sp, "Cet objet est déjà au sommet de l'art nain.", ChatFormatting.GRAY); return false; }
        int need = ForgeUpgrade.COSTS[lvl];
        if (!ing.is(Reg.LUNARGENT_INGOT.get()) || ing.getCount() < need) {
            msg(sp, "Il faut " + need + " lingot" + (need > 1 ? "s" : "") + " de lunargent.", ChatFormatting.RED); return false;
        }
        if (!sp.getAbilities().instabuild) ing.shrink(need);
        boolean fail = sp.getRandom().nextFloat() < ForgeUpgrade.FAIL_CHANCE;
        access.execute((level, pos) -> {
            if (fail) level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.8F, 0.7F);
            else level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            if (level instanceof ServerLevel sl) sl.sendParticles(fail ? ParticleTypes.SMOKE : ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 14, 0.5, 0.2, 0.5, 0);
        });
        if (fail) {
            msg(sp, "La trempe a échoué... les lingots sont perdus.", ChatFormatting.RED);
        } else {
            if (sp.getRandom().nextFloat() < ForgeUpgrade.MEGA_CHANCE) {
                ForgeUpgrade.applyMega(tool, sp.level().registryAccess());
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F));
                msg(sp, "TREMPE ANCESTRALE ! L'objet vibre d'une puissance oubliée (durabilité -20 %).", ChatFormatting.LIGHT_PURPLE);
            } else {
                Holder<Enchantment> h = ForgeUpgrade.holder(sp.level().registryAccess(), tool);
                EnchantmentHelper.updateEnchantments(tool, m -> m.set(h, lvl + 1));
                msg(sp, "Trempe réussie ! Niveau " + (lvl + 1) + " / " + ForgeUpgrade.MAX, ChatFormatting.GOLD);
            }
        }
        inv.setChanged();
        broadcastChanges();
        return true;
    }

    private static void msg(ServerPlayer p, String text, ChatFormatting c) { p.displayClientMessage(Component.literal(text).withStyle(c), true); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack st = slot.getItem();
            ret = st.copy();
            if (index < 2) {
                if (!moveItemStackTo(st, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else if (st.is(Reg.LUNARGENT_INGOT.get())) {
                if (!moveItemStackTo(st, 1, 2, false)) return ItemStack.EMPTY;
            } else if (ForgeUpgrade.canForge(st)) {
                if (!moveItemStackTo(st, 0, 1, false)) return ItemStack.EMPTY;
            } else return ItemStack.EMPTY;
            if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return ret;
    }

    @Override
    public boolean stillValid(Player player) { return stillValid(access, player, Reg.DWARVEN_FORGE.get()); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inv));
    }
}
