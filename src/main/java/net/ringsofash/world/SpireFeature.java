package net.ringsofash.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Aiguilles de roche, rochers, pierres levees et arbres morts (radius 0 = tronc simple). */
public class SpireFeature extends Feature<SpireFeature.Config> {
    public record Config(BlockState block, BlockState cap, int minHeight, int maxHeight, int radius) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("block").forGetter(Config::block),
                BlockState.CODEC.fieldOf("cap").forGetter(Config::cap),
                Codec.INT.fieldOf("min_height").forGetter(Config::minHeight),
                Codec.INT.fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.INT.fieldOf("radius").forGetter(Config::radius)).apply(i, Config::new));
    }

    public SpireFeature() { super(Config.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        Config c = ctx.config();
        BlockPos o = ctx.origin();
        BlockState ground = level.getBlockState(o.below());
        if (ground.isAir() || !level.getFluidState(o).isEmpty() || !level.getFluidState(o.below()).isEmpty()) return false;
        if (ground.is(net.minecraft.tags.BlockTags.LEAVES) || ground.is(net.minecraft.tags.BlockTags.LOGS)) return false;
        int h = c.minHeight + r.nextInt(Math.max(1, c.maxHeight - c.minHeight + 1));
        if (c.radius == 0) {
            for (int y = 0; y < h; y++) set(level, o.above(y), y == h - 1 ? c.cap : c.block);
            if (c.block.hasProperty(RotatedPillarBlock.AXIS)) {
                int branches = r.nextInt(3);
                for (int b = 0; b < branches; b++) {
                    Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
                    int by = 1 + r.nextInt(Math.max(1, h - 1));
                    int len = 1 + r.nextInt(2);
                    for (int k = 1; k <= len; k++) set(level, o.above(by).relative(d, k), c.block.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
                }
            }
            return true;
        }
        double lx = (r.nextDouble() - 0.5) * 0.5, lz = (r.nextDouble() - 0.5) * 0.5;
        boolean blob = h <= 3;
        for (int y = -2; y < h; y++) {
            double t = Math.max(0, y) / (double) h;
            double rad = blob ? c.radius * (1 - t * 0.5) + r.nextDouble() * 0.6 : c.radius * (1 - t) + 0.6;
            int ir = (int) Math.ceil(rad);
            double cx = lx * Math.max(0, y), cz = lz * Math.max(0, y);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double ddx = dx - cx, ddz = dz - cz;
                    if (ddx * ddx + ddz * ddz > rad * rad) continue;
                    BlockPos p = o.offset(dx, y, dz);
                    boolean edge = y >= h - 2 || r.nextInt(5) == 0;
                    set(level, p, edge ? c.cap : c.block);
                }
            }
        }
        return true;
    }

    private void set(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced() || !cur.isSolid() || cur.is(net.minecraft.tags.BlockTags.DIRT)) level.setBlock(p, s, 2);
    }
}
