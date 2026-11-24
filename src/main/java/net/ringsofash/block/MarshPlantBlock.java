package net.ringsofash.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Les plantes de rive (roseaux, massettes, herbe morte) : elles tiennent aussi sur le sable,
 * le gravier, l'argile et la vase, pas seulement sur la terre.
 */
public class MarshPlantBlock extends BushBlock {
    public static final MapCodec<MarshPlantBlock> CODEC = simpleCodec(MarshPlantBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 13, 14);

    public MarshPlantBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends BushBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS) || state.is(Blocks.MOSS_BLOCK)
                || super.mayPlaceOn(state, level, pos);
    }
}
