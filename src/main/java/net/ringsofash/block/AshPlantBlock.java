package net.ringsofash.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ringsofash.gen.Reg;

public class AshPlantBlock extends BushBlock {
    public static final MapCodec<AshPlantBlock> CODEC = simpleCodec(AshPlantBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 13, 14);

    public AshPlantBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends BushBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Reg.ASH_BLOCK.get()) || state.is(Reg.VOLCANIC_ROCK.get()) || super.mayPlaceOn(state, level, pos);
    }
}
