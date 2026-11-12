package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/** Porte naine : s'ouvre et se ferme a la main (clic droit) comme une porte en bois, et reagit a la redstone. */
public class DwarvenDoorBlock extends DoorBlock {
    public DwarvenDoorBlock(BlockBehaviour.Properties props) { super(BlockSetType.STONE, props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        setOpen(player, level, state, pos, !state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
