package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ringsofash.world.Travel;

/** La lave eveillee par l'or : on la traverse pour rejoindre la Hautemarche. */
public class PaleLavaBlock extends Block {
    public PaleLavaBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.clearFire();
        entity.fallDistance = 0;
        if (!level.isClientSide && entity instanceof ServerPlayer sp && !sp.isSpectator()) {
            Travel.enterThroughLava(sp, pos);
        }
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, dir);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) return;
        if (random.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(),
                    0, 0.02 + random.nextDouble() * 0.03, 0);
        }
        if (random.nextInt(200) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
                    0.3F + random.nextFloat() * 0.2F, 1.3F + random.nextFloat() * 0.3F, false);
        }
    }
}
