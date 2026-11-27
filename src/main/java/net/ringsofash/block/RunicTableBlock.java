package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ringsofash.menu.RunicMenu;

/** Table runique : on y scelle les runes dans les armes. C'est definitif. */
public class RunicTableBlock extends Block {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 14, 16);

    /** l'enclume ne prend que les armures et les boucliers */
    private final boolean armor;

    public RunicTableBlock(Properties p) { this(p, false); }

    public RunicTableBlock(Properties p, boolean armor) { super(p); this.armor = armor; }

    @Override
    protected VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter l, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new RunicMenu(id, inv, ContainerLevelAccess.create(level, pos), armor),
                Component.literal(armor ? "Enclume runique" : "Table runique")));
        return InteractionResult.CONSUME;
    }
}
