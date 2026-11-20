package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Table de craft naine : meme fenetre qu'une table de craft, mais verifiee sur notre bloc (sinon elle se refermerait). */
public class DwarvenCraftingBlock extends CraftingTableBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(2, 8, 2, 14, 12, 14),
            Block.box(1, 0, 1, 4, 12, 4), Block.box(12, 0, 1, 15, 12, 4), Block.box(1, 0, 12, 4, 12, 15), Block.box(12, 0, 12, 15, 12, 15));

    public DwarvenCraftingBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new Menu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.ringsofash.dwarven_crafting"));
    }

    public static class Menu extends CraftingMenu {
        private final ContainerLevelAccess access;

        public Menu(int id, Inventory inv, ContainerLevelAccess access) {
            super(id, inv, access);
            this.access = access;
        }

        @Override
        public boolean stillValid(Player player) { return stillValid(access, player, Furniture.CRAFTING.get()); }
    }
}
