package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SmokingRockBlock extends Block {
    public SmokingRockBlock(Properties p) { super(p); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.1, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    0.0, 0.07, 0.0);
        }
        if (random.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}
