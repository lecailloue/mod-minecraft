package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ringsofash.item.UdunFire;

/**
 * La Flamme d'Udun : un feu a part, rouge sang.
 *
 * Elle ne se propage pas et ne met pas le feu aux constructions. Elle reste ou on l'a
 * posee, brule bien plus fort que le feu ordinaire, et s'eteint d'elle-meme au bout
 * d'une dizaine de secondes.
 */
public class UdunFireBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 5);
    private static final int STEP = 40;                       // un cran toutes les deux secondes
    private static final VoxelShape SHAPE = Shapes.empty();

    public UdunFireBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader l, BlockPos pos) {
        return l.getBlockState(pos.below()).isFaceSturdy(l, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) {
        l.scheduleTick(pos, this, STEP);
    }

    @Override
    protected void tick(BlockState s, ServerLevel l, BlockPos pos, RandomSource rnd) {
        if (!canSurvive(s, l, pos)) { l.removeBlock(pos, false); return; }
        int age = s.getValue(AGE);
        if (age >= 5) l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        else {
            l.setBlock(pos, s.setValue(AGE, age + 1), 3);
            l.scheduleTick(pos, this, STEP);
        }
    }

    @Override
    protected void entityInside(BlockState s, Level l, BlockPos pos, Entity e) {
        if (l.isClientSide || !(e instanceof LivingEntity le)) return;
        UdunFire.ignite(le, 100);
    }
}
