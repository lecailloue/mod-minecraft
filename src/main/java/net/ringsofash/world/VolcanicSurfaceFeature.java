package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ringsofash.gen.Reg;

/** Recouvre le sol du biome volcanique (monde normal) de cendre et de roche noire. */
public class VolcanicSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public VolcanicSurfaceFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        Noise2 n = new Noise2(level.getSeed() ^ 0x5EEDA5L);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                p.set(x, top, z);
                if (!level.getBiome(p).is(ModWorldgen.VOLCANO_BIOME)) continue;
                double v = n.fbm(x * 0.07, z * 0.07);
                for (int d = 0; d < 5; d++) {
                    p.set(x, top - d, z);
                    BlockState s = level.getBlockState(p);
                    boolean soft = s.is(BlockTags.DIRT) || s.is(Blocks.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)
                            || s.is(Blocks.SANDSTONE) || s.is(Blocks.MYCELIUM) || s.is(Blocks.GRASS_BLOCK);
                    if (!soft && !(d < 2 && s.is(Blocks.STONE))) continue;
                    BlockState r;
                    if (d == 0 && v < -0.05) r = Reg.ASH_BLOCK.get().defaultBlockState();
                    else if (v > 0.45) r = Blocks.BASALT.defaultBlockState();
                    else if (v > 0.3 && d == 0) r = Blocks.BLACKSTONE.defaultBlockState();
                    else r = Reg.VOLCANIC_ROCK.get().defaultBlockState();
                    level.setBlock(p, r, 2);
                    any = true;
                }
            }
        }
        return any;
    }
}
