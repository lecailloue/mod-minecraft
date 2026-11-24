package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ringsofash.block.MarshPlantBlock;
import net.ringsofash.gen.Reg;

/**
 * Les bords de l'eau : roseaux et massettes les pieds dans l'eau ou sur la berge, nenuphars,
 * vase et galets au ras de l'eau, troncs echoues et pierres dans le courant.
 * Les marais ont deja leur propre decor : on les laisse tranquilles.
 */
public class RiverDecorFeature extends Feature<NoneFeatureConfiguration> {
    public RiverDecorFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static int hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static boolean soil(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)
                || s.is(Blocks.MUD) || s.is(Blocks.ROOTED_DIRT);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        int bankCount = 0;
        int lastBx = 0, lastBz = 0, lastBy = 0;

        for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
            int x = x0 + dx, z = z0 + dz;
            int biome = Zones.biomeFor(MapData.biome(x, z), x, z);
            if (biome == Zones.B_MARSH || biome == MapData.B_MER || biome == 12) continue;
            int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            BlockState ground = level.getBlockState(p.set(x, gy, z));
            if (!soil(ground)) continue;
            BlockState above = level.getBlockState(p.set(x, gy + 1, z));
            boolean water = level.getFluidState(p.set(x, gy + 1, z)).is(FluidTags.WATER);
            int h = (hash(x, z, 311) >>> 8) & 0xFFFF;

            if (water) {                                        // dans l'eau : hauts-fonds
                int depth = 0;
                while (depth < 5 && level.getFluidState(p.set(x, gy + 1 + depth, z)).is(FluidTags.WATER)) depth++;
                if (depth <= 2 && h % 100 < 30) {               // roseaux les pieds dans l'eau
                    BlockState reed = (h % 7 == 0 ? Reg.CATTAIL.get() : Reg.REED_TUFT.get()).defaultBlockState()
                            .setValue(MarshPlantBlock.WATERLOGGED, true);
                    level.setBlock(p.set(x, gy + 1, z), reed, 2);
                    any = true;
                } else if (depth >= 2 && depth <= 4 && h % 101 < 7) {      // nenuphar
                    int top = gy + depth;
                    if (level.getBlockState(p.set(x, top + 1, z)).isAir()) {
                        level.setBlock(p.set(x, top + 1, z), Blocks.LILY_PAD.defaultBlockState(), 2);
                        any = true;
                    }
                } else if (depth <= 2 && h % 103 < 6) {          // galets du lit
                    level.setBlock(p.set(x, gy, z), (h & 1) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.CLAY.defaultBlockState(), 2);
                    any = true;
                }
                continue;
            }

            if (!above.isAir() && !above.is(Blocks.SHORT_GRASS) && !above.is(Blocks.FERN)) continue;
            if (ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.FARMLAND)) continue;

            int near = 9;                                        // distance a l'eau, en blocs
            for (int rr = 1; rr <= 3 && near == 9; rr++)
                for (int[] o : new int[][] {{rr, 0}, {-rr, 0}, {0, rr}, {0, -rr}, {rr, rr}, {-rr, rr}, {rr, -rr}, {-rr, -rr}}) {
                    int nx = x + o[0], nz = z + o[1];
                    int ny = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, nx, nz) - 1;
                    if (Math.abs(ny - gy) <= 2 && level.getFluidState(p.set(nx, ny + 1, nz)).is(FluidTags.WATER)) { near = rr; break; }
                }
            if (near == 9) continue;
            bankCount++;
            lastBx = x; lastBy = gy; lastBz = z;

            int q = h % 100;
            if (near <= 2 && q < 34) {                           // la ceinture de roseaux
                BlockState reed = (h % 5 == 0 ? Reg.CATTAIL.get() : Reg.REED_TUFT.get()).defaultBlockState();
                level.setBlock(p.set(x, gy + 1, z), reed, 2);
                any = true;
            } else if (q < 44) {
                level.setBlock(p.set(x, gy + 1, z), (h % 3 == 0 ? Blocks.FERN : Blocks.TALL_GRASS).defaultBlockState(), 2);
                if (h % 3 != 0) level.setBlock(p.set(x, gy + 2, z), Blocks.TALL_GRASS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
                any = true;
            } else if (q < 50) {
                level.setBlock(p.set(x, gy + 1, z), Reg.PEBBLES.get().defaultBlockState(), 2);
                any = true;
            } else if (q < 54 && near <= 2) {                    // la boue du bord
                level.setBlock(p.set(x, gy, z), (h % 3 == 0 ? Blocks.MUD : Blocks.GRAVEL).defaultBlockState(), 2);
                any = true;
            } else if (q < 58) {
                level.setBlock(p.set(x, gy + 1, z), (h % 4 == 0 ? Reg.FIELD_DAISIES.get() : Reg.FOREST_POPPY.get()).defaultBlockState(), 2);
                any = true;
            } else if (q < 61 && near == 1) {
                level.setBlock(p.set(x, gy + 1, z), Blocks.MOSS_CARPET.defaultBlockState(), 2);
                any = true;
            }
        }

        if (bankCount > 8) {                                     // une fois sur la berge : bois flotte ou pierres
            if (r.nextFloat() < 0.25f) {
                boolean alongX = r.nextBoolean();
                BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
                for (int i = 0; i < 2 + r.nextInt(4); i++) {
                    int bx = lastBx + (alongX ? i : 0), bz = lastBz + (alongX ? 0 : i);
                    int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz) - 1;
                    if (Math.abs(gy - lastBy) > 1) break;
                    if (level.getBlockState(p.set(bx, gy + 1, bz)).isAir() || level.getFluidState(p.set(bx, gy + 1, bz)).is(FluidTags.WATER))
                        level.setBlock(p.set(bx, gy + 1, bz), log, 2);
                }
                any = true;
            }
            if (r.nextFloat() < 0.3f) {
                for (int i = 0; i < 3 + r.nextInt(4); i++) {
                    int bx = lastBx + r.nextInt(5) - 2, bz = lastBz + r.nextInt(5) - 2;
                    int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz) - 1;
                    if (Math.abs(gy - lastBy) > 1) continue;
                    BlockState st = (hash(bx, bz, 312) >>> 8) % 3 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                    if (level.getBlockState(p.set(bx, gy + 1, bz)).isAir() || level.getFluidState(p.set(bx, gy + 1, bz)).is(FluidTags.WATER))
                        level.setBlock(p.set(bx, gy + 1, bz), st, 2);
                }
                any = true;
            }
        }
        return any;
    }
}
