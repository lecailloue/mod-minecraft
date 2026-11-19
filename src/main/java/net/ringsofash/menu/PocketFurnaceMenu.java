package net.ringsofash.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ringsofash.data.Jobs;
import net.ringsofash.data.ModData;
import net.ringsofash.data.PlayerLore;

/**
 * Four portable du Fondeur : case 0 = objet a fondre, case 1 = combustible. Le resultat n'a pas de case : il arrive directement
 * dans l'inventaire (voir MinerJobs). Le contenu vit dans les donnees du joueur, donc le four continue de chauffer ecran ferme.
 */
public class PocketFurnaceMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final Level level;

    public PocketFurnaceMenu(int id, Inventory pinv) { this(id, pinv, new SimpleContainer(2), new SimpleContainerData(4)); }

    public PocketFurnaceMenu(int id, Inventory pinv, Container box, ContainerData data) {
        super(ModMenus.FURNACE.get(), id);
        this.data = data;
        this.level = pinv.player.level();
        addSlot(new Slot(box, 0, 56, 17) {
            @Override public boolean mayPlace(ItemStack s) { return canSmelt(level, s); }
        });
        addSlot(new Slot(box, 1, 56, 53) {
            @Override public boolean mayPlace(ItemStack s) { return fuelTime(s) > 0; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int burn() { return data.get(0); }
    public int burnTotal() { return data.get(1); }
    public int cook() { return data.get(2); }
    public int cookTotal() { return data.get(3); }

    public static boolean canSmelt(Level lv, ItemStack s) {
        return !s.isEmpty() && lv.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(s), lv).isPresent();
    }

    public static int fuelTime(ItemStack s) { return s.isEmpty() ? 0 : s.getBurnTime(RecipeType.SMELTING); }

    /** le joueur a choisi Mineur > Fondeur > Four portable */
    public static boolean unlocked(Player p) {
        return Jobs.choice(p, Jobs.Job.MINER, 0) == 3 && Jobs.choice(p, Jobs.Job.MINER, 1) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot s = slots.get(index);
        if (!s.hasItem()) return ItemStack.EMPTY;
        ItemStack st = s.getItem(), copy = st.copy();
        if (index < 2) {
            if (!moveItemStackTo(st, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (canSmelt(level, st)) moved = moveItemStackTo(st, 0, 1, false);
            if (!moved && fuelTime(st) > 0) moved = moveItemStackTo(st, 1, 2, false);
            if (!moved) moved = index < 29 ? moveItemStackTo(st, 29, 38, false) : moveItemStackTo(st, 2, 29, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (st.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        if (st.getCount() == copy.getCount()) return ItemStack.EMPTY;
        s.onTake(player, st);
        return copy;
    }

    @Override public boolean stillValid(Player player) { return true; }

    public static void open(ServerPlayer sp) {
        if (!unlocked(sp)) return;
        PlayerLore l = ModData.lore(sp);
        ContainerData d = new ContainerData() {
            @Override public int get(int i) { return switch (i) { case 0 -> l.furnaceBurn; case 1 -> l.furnaceBurnTotal; case 2 -> l.furnaceCook; default -> l.furnaceCookTotal; }; }
            @Override public void set(int i, int v) { }
            @Override public int getCount() { return 4; }
        };
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new PocketFurnaceMenu(id, inv, l.furnace, d), Component.literal("Four portable")));
    }
}
