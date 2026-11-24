package net.ringsofash.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Les plantes de rive (roseaux, massettes, herbe morte) : elles tiennent aussi sur le sable,
 * le gravier, l'argile et la vase, pas seulement sur la terre.
 */
public class MarshPlantBlock extends TallGrassBlock {
    public static final MapCodec<MarshPlantBlock> CODEC = simpleCodec(MarshPlantBlock::new);

    public MarshPlantBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends TallGrassBlock> codec() { return CODEC; }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS) || state.is(Blocks.MOSS_BLOCK)
                || super.mayPlaceOn(state, level, pos);
    }
}
