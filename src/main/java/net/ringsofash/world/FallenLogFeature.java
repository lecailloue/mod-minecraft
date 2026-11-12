package net.ringsofash.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Un tronc d'arbre tombe a terre, parfois couvert de champignons ou de mousse. */
public class FallenLogFeature extends Feature<FallenLogFeature.Config> {
    public record Config(BlockState log, int minLength, int maxLength) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                Codec.INT.fieldOf("min_length").forGetter(Config::minLength),
                Codec.INT.fieldOf("max_length").forGetter(Config::maxLength)).apply(i, Config::new));
    }

    public FallenLogFeature() { super(Config.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        Config c = ctx.config();
        BlockPos o = ctx.origin();
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        int len = c.minLength + r.nextInt(Math.max(1, c.maxLength - c.minLength + 1));
        int placed = 0;
        for (int k = 0; k < len; k++) {
            BlockPos p = o.relative(d, k);
            BlockState ground = level.getBlockState(p.below());
            if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL) || ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT))) break;
            if (!level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()) break;
            level.setBlock(p, c.log.setValue(RotatedPillarBlock.AXIS, d.getAxis()), 2);
            placed++;
            BlockPos up = p.above();
            if (level.getBlockState(up).isAir()) {
                float f = r.nextFloat();
                if (f < 0.12f) level.setBlock(up, Blocks.BROWN_MUSHROOM.defaultBlockState(), 2);
                else if (f < 0.20f) level.setBlock(up, Blocks.RED_MUSHROOM.defaultBlockState(), 2);
                else if (f < 0.30f) level.setBlock(up, Blocks.MOSS_CARPET.defaultBlockState(), 2);
            }
        }
        if (placed < c.minLength - 1) return false;
        // une souche a un bout
        BlockPos end = o.relative(d.getOpposite());
        if (level.getBlockState(end).isAir() && level.getBlockState(end.below()).is(Blocks.GRASS_BLOCK)) level.setBlock(end, c.log.setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y), 2);
        return placed > 0;
    }
}
