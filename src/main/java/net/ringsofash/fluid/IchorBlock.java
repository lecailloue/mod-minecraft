package net.ringsofash.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import org.joml.Vector3f;

/** Bloc d'ichor : degage une fumee verte claire qui monte de la surface. */
public class IchorBlock extends LiquidBlock {
    private static final DustParticleOptions SMOKE = new DustParticleOptions(new Vector3f(0.40F, 0.95F, 0.55F), 4.0F);
    private static final DustParticleOptions WISP = new DustParticleOptions(new Vector3f(0.75F, 1.0F, 0.80F), 3.0F);

    public IchorBlock(FlowingFluid fluid, BlockBehaviour.Properties props) {
        super(fluid, props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!level.getBlockState(pos.above()).isAir()) return;                       // seulement la surface
        for (int i = 0; i < 5; i++) {
            double x = pos.getX() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            level.addParticle(random.nextInt(3) == 0 ? WISP : SMOKE, x, pos.getY() + 1.0 + random.nextDouble() * 0.4, z,
                    (random.nextDouble() - 0.5) * 0.02, 0.05 + random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
