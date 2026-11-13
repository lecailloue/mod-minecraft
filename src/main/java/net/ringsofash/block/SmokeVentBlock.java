package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bouche de fumee : invisible, intangible, incassable ; ne fait que cracher une colonne de fumee noire. */
public class SmokeVentBlock extends Block {
    public SmokeVentBlock(Properties p) { super(p); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.empty(); }
    @Override public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }
    @Override public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1.0F; }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.3, z = pos.getZ() + 0.5;
        if (r.nextInt(2) == 0) level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.01, 0.07 + r.nextDouble() * 0.03, (r.nextDouble() - 0.5) * 0.01);
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, x + (r.nextDouble() - 0.5), y + r.nextDouble(), z + (r.nextDouble() - 0.5), 0, 0.04, 0);
    }
}
