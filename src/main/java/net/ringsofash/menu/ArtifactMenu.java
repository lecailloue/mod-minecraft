package net.ringsofash.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.ringsofash.item.Artifacts;

/**
 * Onglet d'artefacts : case 0 = chaine (collier), cases 1 a 4 = artefacts. La chaine de niveau n ouvre n cases d'artefacts.
 * Le clic maj (deplacement rapide) est volontairement inactif ; n'importe quelle piece d'artefact peut etre rangee, expertisee ou non.
 */
public class ArtifactMenu extends AbstractContainerMenu {
    public static final int CHAIN_X = 80, CHAIN_Y = 26, ART_Y = 64;
    public static int artX(int i) { return 35 + i * 27; }
    private final Container box;

    public ArtifactMenu(int id, Inventory pinv) { this(id, pinv, new SimpleContainer(Artifacts.SLOTS)); }

    public ArtifactMenu(int id, Inventory pinv, Container box) {
        super(ModMenus.ARTIFACTS.get(), id);
        this.box = box;
        addSlot(new Slot(box, 0, CHAIN_X, CHAIN_Y) {
            @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof Artifacts.Chain; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 1; i < Artifacts.SLOTS; i++) {
            final int n = i;
            addSlot(new Slot(box, i, artX(i - 1), ART_Y) {
                @Override public boolean isActive() { return Artifacts.level(box) >= n; }
                @Override public boolean mayPlace(ItemStack s) { return isActive() && s.getItem() instanceof Artifacts.Piece; }
                @Override public boolean mayPickup(Player p) { return isActive(); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c + r * 9 + 9, 8 + c * 18, 98 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(pinv, c, 8 + c * 18, 156));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (type == ClickType.QUICK_MOVE) return;                                  // pas de clic maj
        super.clicked(slot, button, type, player);
        trim(player);
    }

    /** pas de deplacement rapide dans cet onglet */
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index >= Artifacts.SLOTS; }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        trim(player);
    }

    /** si la chaine est retiree ou remplacee par un niveau plus bas, les artefacts des cases fermees retournent dans l'inventaire */
    private void trim(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        int lv = Artifacts.level(box);
        for (int i = lv + 1; i < Artifacts.SLOTS; i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty()) continue;
            box.setItem(i, ItemStack.EMPTY);
            if (!sp.getInventory().add(s)) sp.drop(s, false);
        }
        broadcastChanges();
    }

    public static void open(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ArtifactMenu(id, inv, ModData.lore(sp).artifacts), Component.literal("Artefacts")));
    }
}
