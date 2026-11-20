package net.ringsofash.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.ringsofash.gen.Reg;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Genere la Hautemarche a partir de la carte predefinie : le meme terrain dans tous les mondes. */
public final class MapChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MapChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)).apply(i, MapChunkGenerator::new));

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState();
    private static final BlockState COARSE = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState PATH = Blocks.DIRT_PATH.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState CLAY = Blocks.CLAY.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState ANDESITE = Blocks.ANDESITE.defaultBlockState();
    private static final BlockState POLISHED_ANDESITE = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public MapChunkGenerator(BiomeSource source) { super(source); }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    private static float hash(int x, int y, int z) {
        long h = x * 3129871L ^ z * 116129781L ^ y * 42317861L;
        h = h * h * 42317861L + h * 11L;
        return ((h >> 16) & 0xFFFF) / 65536f;
    }

    private static float lattice(int ix, int iz, int seed) {
        long h = ix * 73856093L ^ iz * 19349663L ^ seed * 83492791L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    /** bruit de valeurs lisse (0..1), deterministe : le meme dans tous les mondes */
    static float vnoise(double x, double z, int seed) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = x - ix, fz = z - iz;
        fx = fx * fx * fx * (fx * (fx * 6 - 15) + 10);
        fz = fz * fz * fz * (fz * (fz * 6 - 15) + 10);
        double a = lattice(ix, iz, seed), b = lattice(ix + 1, iz, seed), c = lattice(ix, iz + 1, seed), d = lattice(ix + 1, iz + 1, seed);
        double top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return (float) (top + (bot - top) * fz);
    }

    /** bruit lent (-1..1) pour la ligne des neiges */
    private static double slow(int x, int z) {
        return (Math.sin(x * 0.013) + Math.sin(z * 0.017 + 1.3) + Math.sin((x + z) * 0.011 + 2.1)) / 3.0;
    }

    private static double snowLine(int x, int z) { return 190 + 16 * slow(x, z); }

    /** Couches de neige posees au-dessus du sol, juste sous la limite des neiges (0 = aucune). */
    static int snowLayers(int x, int z, int h, int b) {
        if (b != MapData.B_CIMES) return 0;
        double line = snowLine(x, z);
        if (h < line - 16 || h >= line) return 0;
        float v = vnoise(x * 0.12, z * 0.12, 5);
        if (v < 0.42f) return 0;
        return 1 + (int) (hash(x, 9, z) * 3);
    }

    /** bruit de Perlin-like fractal (0..1) : plusieurs octaves de bruit lisse */
    static float fbm(double x, double z, int seed) {
        return vnoise(x, z, seed) * 0.57f + vnoise(x * 2.1 + 17, z * 2.1 - 9, seed + 31) * 0.29f + vnoise(x * 4.3 - 5, z * 4.3 + 23, seed + 67) * 0.14f;
    }

    /** bruit 3D lisse (0..1) */
    static float vnoise3(double x, double y, double z, int seed) {
        int iy = (int) Math.floor(y);
        double fy = y - iy; fy = fy * fy * (3 - 2 * fy);
        return (float) (vnoise(x + iy * 31.7, z - iy * 17.3, seed) * (1 - fy) + vnoise(x + (iy + 1) * 31.7, z - (iy + 1) * 17.3, seed) * fy);
    }

    static float fbm3(double x, double y, double z, int seed) {
        return vnoise3(x, y, z, seed) * 0.62f + vnoise3(x * 2.2 + 11, y * 2.2, z * 2.2 - 7, seed + 29) * 0.38f;
    }

    /** taches de sol continues : renvoie 0 (rien), 1 (terre), 2 (chemin) ; seed distincte par biome */
    static int groundPatch(int x, int z, int seed, float thr) {
        float n = fbm(x * 0.022, z * 0.022, seed);
        if (n <= thr) return 0;
        float t = fbm(x * 0.09, z * 0.09, seed + 6) + (hash(x, seed, z) - 0.5f) * 0.05f;
        float d = n - thr;
        if (t > 0.62f - d * 1.6f) return 2;
        if (t > 0.5f - d * 1.6f) return 1;
        return 0;
    }

    /** texture de roche en volume : pierre, andesite, andesite polie, cobblestone et gravier, partout dans la montagne */
    static BlockState rock(int x, int y, int z) {
        float v = fbm3(x * 0.045, y * 0.06, z * 0.045, 14) + (hash(x, y + 11, z) - 0.5f) * 0.06f;
        float w = fbm3(x * 0.08, y * 0.1, z * 0.08, 15) + (hash(x, y + 12, z) - 0.5f) * 0.06f;
        if (v > 0.64f) return POLISHED_ANDESITE;
        if (v > 0.52f && w > 0.5f) return ANDESITE;
        if (w < 0.36f && v > 0.35f) return COBBLE;
        if (v < 0.30f && w > 0.55f) return GRAVEL;
        if (w > 0.66f) return ANDESITE;
        return STONE;
    }

    /** sol des montagnes : herbe en bas avec une limite irreguliere, roche texturee au-dessus */
    static BlockState mountainTop(int x, int z, int h, int slope, boolean under) {
        double line = 92 + 38 * fbm(x * 0.011, z * 0.011, 20) + 10 * (fbm(x * 0.05, z * 0.05, 21) - 0.5);
        float p = (float) Math.max(0, Math.min(1, (line - h) / 14.0));
        if (slope >= 4) p = 0; else if (slope == 3) p *= 0.4f; else if (slope == 2) p *= 0.8f;
        float r = hash(x, 13, z);
        if (!under && r < p) return GRASS;
        if (!under && r < p + 0.1f && p > 0) return COARSE;
        return rock(x, h, z);
    }

    private static boolean rocky(int b, int slope) { return b == MapData.B_MONTS || b == MapData.B_CIMES || b == MapData.B_MONTS_CENDRE || slope >= 4; }

    private static boolean landBiome(int b) { return b != MapData.B_MER && b != MapData.B_RIVAGE && b != MapData.B_RIVIERE && b != MapData.B_LAVE; }

    /** Bloc a l'altitude y pour la colonne decrite (h = dernier bloc solide, wt = surface de l'eau). */
    /** Chemin d'entree de Fondcombe : pierre blanche, depuis la route de l'esplanade est vers l'est. */
    private static final int[][] WHITE_ROAD = {{-344, -162}, {-318, -160}, {-292, -166}, {-262, -171}, {-232, -168}, {-204, -160}, {-176, -152}, {-146, -150}};

    static boolean onWhiteRoad(int x, int z) {
        if (x < -346 || x > -144 || z < -176 || z > -144) return false;
        for (int i = 0; i + 1 < WHITE_ROAD.length; i++) {
            double ax = WHITE_ROAD[i][0], az = WHITE_ROAD[i][1], bx = WHITE_ROAD[i + 1][0], bz = WHITE_ROAD[i + 1][1];
            double dx = bx - ax, dz = bz - az, t = Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz)));
            double ex = ax + t * dx - x, ez = az + t * dz - z;
            double w = 2.6 + 0.5 * Math.sin(x * 0.11);
            if (ex * ex + ez * ez <= w * w) return true;
        }
        return false;
    }

    private static BlockState[] moriaPal;

    private static synchronized BlockState[] moriaPal() {
        if (moriaPal != null) return moriaPal;
        BlockState[] p = new BlockState[MoriaPlan.NIDS];
        java.util.Arrays.fill(p, STONE);
        net.minecraft.core.Direction[] dirs = {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST};
        p[MoriaPlan.AIR] = AIR;
        p[MoriaPlan.DSTONE] = Reg.DWARVEN_STONE.get().defaultBlockState();
        p[MoriaPlan.DCOBBLE] = Reg.DWARVEN_COBBLESTONE.get().defaultBlockState();
        p[MoriaPlan.DBRICKS] = Reg.DWARVEN_BRICKS.get().defaultBlockState();
        p[MoriaPlan.DPILLAR] = Reg.DWARVEN_PILLAR.get().defaultBlockState();
        p[MoriaPlan.DCHISEL] = Reg.DWARVEN_CHISELED.get().defaultBlockState();
        p[MoriaPlan.DRUNES] = Reg.DWARVEN_RUNES.get().defaultBlockState();
        p[MoriaPlan.DLAMP] = Reg.DWARVEN_LAMP.get().defaultBlockState();
        p[MoriaPlan.WHITE] = Reg.WHITE_STONE.get().defaultBlockState();
        p[MoriaPlan.DEEPSLATE] = DEEPSLATE;
        p[MoriaPlan.LAVA] = LAVA;
        p[MoriaPlan.WATER] = WATER;
        p[MoriaPlan.MAGMA] = Blocks.MAGMA_BLOCK.defaultBlockState();
        p[MoriaPlan.OAKP] = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        p[MoriaPlan.OAKLOG] = Blocks.DARK_OAK_LOG.defaultBlockState();
        p[MoriaPlan.OAKLOGX] = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X);
        p[MoriaPlan.OAKLOGZ] = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z);
        p[MoriaPlan.FENCE] = Blocks.DARK_OAK_FENCE.defaultBlockState();
        p[MoriaPlan.CHAIN] = Blocks.CHAIN.defaultBlockState();
        p[MoriaPlan.LANTERN] = Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);
        p[MoriaPlan.SLANTERN] = Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);
        p[MoriaPlan.WEB] = Blocks.COBWEB.defaultBlockState();
        p[MoriaPlan.BONES] = Blocks.BONE_BLOCK.defaultBlockState();
        p[MoriaPlan.QUARTZ] = Blocks.QUARTZ_BLOCK.defaultBlockState();
        p[MoriaPlan.QSLAB] = Blocks.QUARTZ_SLAB.defaultBlockState();
        p[MoriaPlan.BOOKS] = Blocks.BOOKSHELF.defaultBlockState();
        p[MoriaPlan.BARS] = Blocks.IRON_BARS.defaultBlockState();
        p[MoriaPlan.FORGE] = Reg.DWARVEN_FORGE.get().defaultBlockState();
        p[MoriaPlan.ANVIL] = Blocks.ANVIL.defaultBlockState();
        p[MoriaPlan.BSLAB] = Reg.DWARVEN_BRICKS_SLAB.get().defaultBlockState();
        p[MoriaPlan.BWALL] = Reg.DWARVEN_BRICKS_WALL.get().defaultBlockState();
        p[MoriaPlan.GRAVEL] = GRAVEL;
        p[MoriaPlan.SPRLOG] = Blocks.SPRUCE_LOG.defaultBlockState();
        p[MoriaPlan.SPRLEAF] = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        p[MoriaPlan.BLACKSTONE] = Blocks.BLACKSTONE.defaultBlockState();
        p[MoriaPlan.ORE] = Reg.LUNARGENT_ORE.get().defaultBlockState();
        p[MoriaPlan.ORE + 1] = Reg.SOMBREFER_ORE.get().defaultBlockState();
        p[MoriaPlan.ORE + 2] = Reg.BRAISITE_ORE.get().defaultBlockState();
        p[MoriaPlan.ORE + 3] = Reg.BRUMECRISTAL_ORE.get().defaultBlockState();
        p[MoriaPlan.ORE + 4] = Reg.ORRUNE_ORE.get().defaultBlockState();
        p[MoriaPlan.GOLD] = Blocks.GOLD_BLOCK.defaultBlockState();
        p[MoriaPlan.RAWGOLD] = Blocks.RAW_GOLD_BLOCK.defaultBlockState();
        p[MoriaPlan.RAILX] = Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
        p[MoriaPlan.RAILZ] = Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.NORTH_SOUTH);
        p[MoriaPlan.BARREL] = Blocks.BARREL.defaultBlockState();
        p[MoriaPlan.RCARPET] = Blocks.RED_CARPET.defaultBlockState();
        p[MoriaPlan.REDWOOL] = Blocks.RED_WOOL.defaultBlockState();
        p[MoriaPlan.BLACKWOOL] = Blocks.BLACK_WOOL.defaultBlockState();
        p[MoriaPlan.CAMPFIRE] = Blocks.CAMPFIRE.defaultBlockState();
        p[MoriaPlan.CRAFT] = Blocks.CRAFTING_TABLE.defaultBlockState();
        p[MoriaPlan.FURNACE] = Blocks.FURNACE.defaultBlockState();
        p[MoriaPlan.YELLOWWOOL] = Blocks.YELLOW_WOOL.defaultBlockState();
        p[MoriaPlan.BLUEW] = Blocks.BLUE_WOOL.defaultBlockState();
        p[MoriaPlan.POLDEEP] = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        p[MoriaPlan.COBDEEP] = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        p[MoriaPlan.DEEPBR] = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        p[MoriaPlan.CHISDEEP] = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        p[MoriaPlan.POLBLK] = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        p[MoriaPlan.CAULDRON] = Blocks.CAULDRON.defaultBlockState();
        p[MoriaPlan.COPPER] = Blocks.WAXED_CUT_COPPER.defaultBlockState();
        p[MoriaPlan.SHROOM] = Blocks.SHROOMLIGHT.defaultBlockState();
        p[MoriaPlan.IRONB] = Blocks.IRON_BLOCK.defaultBlockState();
        p[MoriaPlan.SMBAS] = Blocks.SMOOTH_BASALT.defaultBlockState();
        p[MoriaPlan.CRACKB] = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        p[MoriaPlan.DTILES] = Blocks.DEEPSLATE_TILES.defaultBlockState();
        p[MoriaPlan.CRDEEP] = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        p[MoriaPlan.DCRAFT] = net.ringsofash.block.Furniture.CRAFTING.get().defaultBlockState();
        for (int i = 0; i < 4; i++) {
            p[MoriaPlan.DCHEST + i] = net.ringsofash.block.Furniture.CHEST.get().defaultBlockState().setValue(net.ringsofash.block.DwarvenChestBlock.FACING, dirs[i]);
            p[MoriaPlan.DFURN + i] = net.ringsofash.block.Furniture.FURNACE.get().defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, dirs[i]);
            p[MoriaPlan.DBEDF + i] = net.ringsofash.block.Furniture.BED.get().defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, dirs[i])
                    .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
            p[MoriaPlan.DBEDH + i] = net.ringsofash.block.Furniture.BED.get().defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, dirs[i])
                    .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD);
            p[MoriaPlan.DBEDFL + i] = p[MoriaPlan.DBEDF + i].setValue(net.ringsofash.block.DwarvenBedBlock.JOIN_LEFT, true);
            p[MoriaPlan.DBEDFR + i] = p[MoriaPlan.DBEDF + i].setValue(net.ringsofash.block.DwarvenBedBlock.JOIN_RIGHT, true);
            p[MoriaPlan.DBEDHL + i] = p[MoriaPlan.DBEDH + i].setValue(net.ringsofash.block.DwarvenBedBlock.JOIN_LEFT, true);
            p[MoriaPlan.DBEDHR + i] = p[MoriaPlan.DBEDH + i].setValue(net.ringsofash.block.DwarvenBedBlock.JOIN_RIGHT, true);
            p[MoriaPlan.BSTAIR + i] = Reg.DWARVEN_BRICKS_STAIRS.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, dirs[i]);
            p[MoriaPlan.DSTAIR + i] = Reg.DWARVEN_STONE_STAIRS.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, dirs[i]);
            p[MoriaPlan.BSTAIR_TOP + i] = Reg.DWARVEN_BRICKS_STAIRS.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, dirs[i]).setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
        }
        return moriaPal = p;
    }

    static BlockState moria(int id) { return moriaPal()[id]; }

    static BlockState block(int x, int y, int z, int h, int wt, int b, int slope) {
        if (y > h) {
            if (y <= MoriaPlan.aboveTop(x, z)) {                                      // la facade des Portes : statues, parapets, braziers au dessus du sol
                int m = MoriaPlan.at(x, y, z, h);
                if (m != MoriaPlan.KEEP) return moria(m);
            }
            return y <= wt ? (wt > MapData.SEA && !MoriaPlan.inLake(x, z) || b == MapData.B_LAVE ? LAVA : WATER) : AIR;
        }
        if (y <= -64) return BEDROCK;
        if (y < -59 && hash(x, y, z) < (-59 - y) / 5f) return BEDROCK;
        if (MoriaPlan.inBox(x, y, z)) {                                          // les mines de la Moria, creusees sous la carte
            int m = MoriaPlan.at(x, y, z, h);
            if (m != MoriaPlan.KEEP) return moria(m);
        }
        int depth = h - y;
        boolean under = wt > h;                       // sous l'eau
        if (!under && depth <= 1 && h > MapData.SEA + 1 && onWhiteRoad(x, z)) return Reg.WHITE_STONE.get().defaultBlockState();
        BlockState top = null;
        int layers = 3;
        if (landBiome(b) && b != 2 && b != MapData.B_MONTS_CENDRE && h <= MapData.SEA + 1) {
            // berges et plages : sable et gravier, en pente douce
            top = hash(x, 6, z) < 0.25f ? GRAVEL : SAND; layers = 3;
        } else switch (b) {
            case MapData.B_MER -> { top = h >= 42 ? (vnoise(x * 0.07, z * 0.07, 3) > 0.7f ? CLAY : SAND) : GRAVEL; layers = 3; }
            case MapData.B_RIVAGE -> { top = SAND; layers = 3; }
            case MapData.B_LAVE -> { float q = vnoise(x * 0.12, z * 0.12, 27); top = q > 0.78f ? Blocks.MAGMA_BLOCK.defaultBlockState() : q > 0.4f ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState(); layers = 3; }
            case MapData.B_RIVIERE -> {
                float n = vnoise(x * 0.09, z * 0.09, 4);
                top = n < 0.3f ? GRAVEL : n < 0.45f ? CLAY : SAND; layers = 3;
            }
            case 0, MapData.B_BOSQ -> {
                if (slope >= 4 && h >= 78) { top = rock(x, h, z); layers = 1; }
                else {
                    top = GRASS; layers = 3;
                    int gp = under ? 0 : groundPatch(x, z, 21, 0.545f);
                    if (gp == 2) top = hash(x, 3, z) < 0.5f ? COARSE : PATH;
                    else if (gp == 1) top = COARSE;
                    else if (fbm(x * 0.05, z * 0.05, 23) > 0.66f && fbm(x * 0.2, z * 0.2, 24) > 0.5f) top = DIRT;
                }
            }
            case 1 -> {
                if (slope >= 4 && h >= 78) { top = rock(x, h, z); layers = 1; }
                else {
                    top = GRASS;
                    // texture de sol : grandes taches continues de chemin / terre, bords adoucis (aucun bloc isole)
                    float n = fbm(x * 0.022, z * 0.022, 11);
                    if (!under && n > 0.54f) {
                        float t = fbm(x * 0.09, z * 0.09, 17) + (hash(x, 2, z) - 0.5f) * 0.05f;
                        float depth2 = n - 0.54f;                  // plus on s'enfonce dans la tache, plus elle est dense
                        if (t > 0.62f - depth2 * 1.6f) top = PATH;
                        else if (t > 0.5f - depth2 * 1.6f) top = COARSE;
                    }
                    if (top == GRASS && fbm(x * 0.05, z * 0.05, 16) > 0.66f && fbm(x * 0.2, z * 0.2, 18) > 0.5f) top = COARSE;
                }
            }
            case 2 -> {
                float v = fbm(x * 0.05, z * 0.05, 7);
                top = v > 0.68f ? Reg.VOLCANIC_ROCK.get().defaultBlockState() : hash(x, 7, z) < 0.03f ? GRAVEL : Reg.ASH_BLOCK.get().defaultBlockState();
                layers = 3;
            }
            case MapData.B_FORET, MapData.B_CLAIR -> {
                if (slope >= 4 && h >= 78) { top = rock(x, h, z); layers = 1; }
                else {
                    top = under ? DIRT : GRASS; layers = 3;
                    if (!under) {
                        float pz = fbm(x * 0.06, z * 0.06, 8);
                        int gp = groundPatch(x, z, 26, b == MapData.B_FORET ? 0.55f : 0.58f);
                        if (pz > (b == MapData.B_FORET ? 0.56f : 0.66f)) top = PODZOL;
                        else if (gp == 2) top = COARSE;
                        else if (gp == 1) top = hash(x, 4, z) < 0.5f ? PODZOL : COARSE;
                    }
                }
            }
            case MapData.B_MONTS -> {
                top = mountainTop(x, z, h, slope, under);
                layers = top == GRASS || top == COARSE ? 3 : 1;
            }
            case MapData.B_CIMES -> {
                if (h >= snowLine(x, z)) {
                    if (slope >= 7 && hash(x, 3, z) < 0.45f) top = rock(x, h, z);      // roche nue sur les parois
                    else { top = SNOW; layers = 3; }
                } else { top = mountainTop(x, z, h, slope, under); layers = top == GRASS || top == COARSE ? 3 : 1; }
            }
            case MapData.B_MONTS_CENDRE -> {
                float v = fbm(x * 0.05, z * 0.05, 13);
                top = slope <= 3 && v > 0.5f ? Reg.ASH_BLOCK.get().defaultBlockState() : rock(x, h, z);
                layers = top == STONE ? 3 : 2;
            }
            default -> { top = under ? DIRT : GRASS; layers = 3; }
        }
        if (top != null && depth < layers) {
            if (depth == 0) return top;
            if (top == GRASS || top == PODZOL || top == COARSE || top == PATH) return DIRT;
            return top;
        }
        if (y < 0) return DEEPSLATE;
        if (rocky(b, slope) && y > h - 60 && y >= 8) return rock(x, y, z);
        if (y < 8 && hash(x, y, z) < (8 - y) / 8f) return DEEPSLATE;
        return STONE;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int minY = chunk.getMinBuildHeight();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                int h = MapData.height(x, z), wt = MapData.waterTop(x, z), b = MapData.biome(x, z), slope = MapData.slope(x, z);
                int top = Math.max(h, wt);
                int snow = wt > h ? 0 : snowLayers(x, z, h, b);
                BlockState topState = AIR, floor = AIR;
                int yEnd = Math.max(top, MoriaPlan.aboveTop(x, z)), hi = top;
                BlockState hiState = AIR;
                for (int y = minY; y <= yEnd; y++) {
                    BlockState s = block(x, y, z, h, wt, b, slope);
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(lx, y & 15, lz, s, false);
                    if (y == h) floor = s;
                    if (y == top) topState = s;
                    if (y > top && !s.isAir()) { hi = y; hiState = s; }
                }
                if (hi > top) { top = hi; topState = hiState; snow = 0; }
                if (snow > 0) {
                    topState = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, snow);
                    chunk.getSection(chunk.getSectionIndex(h + 1)).setBlockState(lx, (h + 1) & 15, lz, topState, false);
                    top = h + 1;
                }
                ocean.update(lx, h, lz, floor);
                surface.update(lx, top, lz, topState);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {}

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos cp = region.getCenter();
        var biome = region.getBiome(cp.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), cp.getMinBlockX(), cp.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region, biome, cp, random);
    }

    @Override public int getGenDepth() { return 512; }
    @Override public int getSeaLevel() { return MapData.SEA; }
    @Override public int getMinY() { return -64; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int h = MapData.height(x, z);
        boolean solid = type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
        return (solid ? h : Math.max(h, MapData.waterTop(x, z))) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int h = MapData.height(x, z), wt = MapData.waterTop(x, z), b = MapData.biome(x, z), slope = MapData.slope(x, z);
        BlockState[] column = new BlockState[level.getHeight()];
        for (int i = 0; i < column.length; i++) column[i] = block(x, level.getMinBuildHeight() + i, z, h, wt, b, slope);
        return new NoiseColumn(level.getMinBuildHeight(), column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Carte de la Hautemarche : relief " + MapData.height(pos.getX(), pos.getZ()));
    }
}
