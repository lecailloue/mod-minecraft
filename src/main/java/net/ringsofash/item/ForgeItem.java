package net.ringsofash.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.ringsofash.block.ForgeBlock;

/** Objet de la forge naine : pose un bloc geant de 3 x 3 x 3 (il faut la place). */
public class ForgeItem extends BlockItem {
    public ForgeItem(Block block, Properties props) { super(block, props); }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos base = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!ctx.canPlace()) return InteractionResult.FAIL;
        for (int dx = 0; dx < 3; dx++) for (int dy = 0; dy < 3; dy++) for (int dz = 0; dz < 3; dz++) {
            BlockPos p = base.offset(dx - 1, dy, dz - 1);
            if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
                if (player != null && level.isClientSide) player.displayClientMessage(Component.literal("Il faut un espace libre de 3 x 3 x 3 pour la forge."), true);
                return InteractionResult.FAIL;
            }
        }
        if (!level.isClientSide) {
            ForgeBlock.build(level, base.getX(), base.getY(), base.getZ());
            level.playSound(null, base, getBlock().defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
