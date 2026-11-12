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

    private static boolean landBiome(int b) { return b != MapData.B_MER && b != MapData.B_RIVAGE && b != MapData.B_RIVIERE; }

    /** Bloc a l'altitude y pour la colonne decrite (h = dernier bloc solide, wt = surface de l'eau). */
    static BlockState block(int x, int y, int z, int h, int wt, int b, int slope) {
        if (y > h) return y <= wt ? WATER : AIR;
        if (y <= -64) return BEDROCK;
        if (y < -59 && hash(x, y, z) < (-59 - y) / 5f) return BEDROCK;
        int depth = h - y;
        boolean under = wt > h;                       // sous l'eau
        BlockState top = null;
        int layers = 3;
        if (landBiome(b) && h <= MapData.SEA + 1) {
            // berges et plages : sable et gravier, en pente douce
            top = hash(x, 6, z) < 0.25f ? GRAVEL : SAND; layers = 3;
        } else switch (b) {
            case MapData.B_MER -> { top = h >= 42 ? (vnoise(x * 0.07, z * 0.07, 3) > 0.7f ? CLAY : SAND) : GRAVEL; layers = 3; }
            case MapData.B_RIVAGE -> { top = SAND; layers = 3; }
            case MapData.B_RIVIERE -> {
                float n = vnoise(x * 0.09, z * 0.09, 4);
                top = n < 0.3f ? GRAVEL : n < 0.45f ? CLAY : SAND; layers = 3;
            }
            case 0 -> { top = slope >= 4 && h >= 78 ? STONE : GRASS; layers = slope >= 4 && h >= 78 ? 1 : 3; }
            case 1 -> {
                if (slope >= 4 && h >= 78) { top = STONE; layers = 1; }
                else {
                    top = GRASS;
                    // chemins sinueux et plaques de terre pour casser l'uniformite
                    double p = vnoise(x * 0.012, z * 0.012, 11);
                    double g = Math.abs(vnoise((x + 1) * 0.012, z * 0.012, 11) - p) + Math.abs(vnoise(x * 0.012, (z + 1) * 0.012, 11) - p);
                    if (Math.abs(p - 0.5) < Math.max(g * 1.1, 1e-9) && !under) top = PATH;
                    else {
                        float n = vnoise(x * 0.05, z * 0.05, 1);
                        if (n > 0.64f) top = COARSE;
                        else if (vnoise(x * 0.09, z * 0.09, 2) > 0.74f) top = DIRT;
                    }
                }
            }
            case 2 -> {
                float v = vnoise(x * 0.06, z * 0.06, 7);
                top = v > 0.68f ? Reg.VOLCANIC_ROCK.get().defaultBlockState() : hash(x, 7, z) < 0.03f ? GRAVEL : Reg.ASH_BLOCK.get().defaultBlockState();
                layers = 3;
            }
            case MapData.B_FORET -> {
                if (slope >= 4 && h >= 78) { top = STONE; layers = 1; }
                else { top = under ? DIRT : vnoise(x * 0.07, z * 0.07, 8) > 0.6f ? PODZOL : GRASS; layers = 3; }
            }
            case MapData.B_MONTS -> {
                if (slope <= 2 && h < 150 && !under) { top = GRASS; layers = 3; }
                else if (vnoise(x * 0.08, z * 0.08, 12) > 0.7f) { top = GRAVEL; layers = 1; }
            }
            case MapData.B_CIMES -> {
                if (h >= snowLine(x, z)) {
                    if (slope >= 7 && hash(x, 3, z) < 0.45f) top = null;      // roche nue sur les parois
                    else { top = SNOW; layers = 3; }
                } else if (slope <= 2 && h < 150) { top = GRASS; layers = 3; }
                else if (vnoise(x * 0.08, z * 0.08, 12) > 0.7f) { top = GRAVEL; layers = 1; }
            }
            case MapData.B_MONTS_CENDRE -> {
                float v = vnoise(x * 0.05, z * 0.05, 13);
                top = slope <= 3 && v > 0.5f ? Reg.ASH_BLOCK.get().defaultBlockState() : Reg.VOLCANIC_ROCK.get().defaultBlockState();
                layers = 2;
            }
            default -> { top = under ? DIRT : GRASS; layers = 3; }
        }
        if (top != null && depth < layers) {
            if (depth == 0) return top;
            if (top == GRASS || top == PODZOL || top == COARSE || top == PATH) return DIRT;
            return top;
        }
        if (y < 0) return DEEPSLATE;
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
                for (int y = minY; y <= top; y++) {
                    BlockState s = block(x, y, z, h, wt, b, slope);
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(lx, y & 15, lz, s, false);
                    if (y == h) floor = s;
                    if (y == top) topState = s;
                }
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
