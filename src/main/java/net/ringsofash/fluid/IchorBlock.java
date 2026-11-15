package net.ringsofash.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/** Bloc d'ichor : degage une fumee verte claire qui monte de la surface. */
public class IchorBlock extends LiquidBlock {

    public IchorBlock(FlowingFluid fluid, BlockBehaviour.Properties props) {
        super(fluid, props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!level.getBlockState(pos.above()).isAir()) return;                       // seulement la surface
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            level.addParticle(ModFluids.ICHOR_SMOKE.get(), x, pos.getY() + 1.0 + random.nextDouble() * 0.3, z,
                    (random.nextDouble() - 0.5) * 0.02, 0.03 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
