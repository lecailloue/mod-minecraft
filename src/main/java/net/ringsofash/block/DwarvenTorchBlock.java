package net.ringsofash.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Torche murale naine : une coupe de bronze portee par un bras de fer, fixee a un mur par une platine de pierre. */
public class DwarvenTorchBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DwarvenTorchBlock> CODEC = simpleCodec(DwarvenTorchBlock::new);
    private static final VoxelShape NORTH = box(4, 2, 4, 12, 16, 16), SOUTH = box(4, 2, 0, 12, 16, 12), WEST = box(4, 2, 4, 16, 16, 12), EAST = box(0, 2, 4, 12, 16, 12);

    public DwarvenTorchBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) { case SOUTH -> SOUTH; case WEST -> WEST; case EAST -> EAST; default -> NORTH; };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        BlockPos wall = pos.relative(f.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, f);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        for (Direction d : ctx.getNearestLookingDirections()) {
            if (d.getAxis().isHorizontal()) {
                BlockState st = defaultBlockState().setValue(FACING, d.getOpposite());
                if (st.canSurvive(ctx.getLevel(), ctx.getClickedPos())) return st;
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) { b.add(FACING); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.85, z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
    }
}
