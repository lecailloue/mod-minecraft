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
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
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

    /** bruit lent (-1..1) pour la ligne des neiges */
    private static double slow(int x, int z) {
        return (Math.sin(x * 0.013) + Math.sin(z * 0.017 + 1.3) + Math.sin((x + z) * 0.011 + 2.1)) / 3.0;
    }

    /** Bloc a l'altitude y pour la colonne decrite (h = dernier bloc solide, wt = surface de l'eau). */
    static BlockState block(int x, int y, int z, int h, int wt, int b, int slope) {
        if (y > h) return y <= wt ? WATER : AIR;
        if (y <= -64) return BEDROCK;
        if (y < -59 && hash(x, y, z) < (-59 - y) / 5f) return BEDROCK;
        int depth = h - y;
        boolean under = wt > h;                       // sous l'eau
        BlockState top = null;
        int layers = 3;
        switch (b) {
            case MapData.B_MER -> { top = h >= 40 ? SAND : GRAVEL; layers = 3; }
            case MapData.B_RIVAGE -> { top = SAND; layers = 3; }
            case MapData.B_RIVIERE -> { top = hash(x, 0, z) < 0.35f ? GRAVEL : SAND; layers = 2; }
            case MapData.B_MONTS -> {
                if (slope <= 2 && h < 140 && !under) { top = GRASS; layers = 3; }
                else if (hash(x, 1, z) < 0.07f) { top = GRAVEL; layers = 1; }
            }
            case MapData.B_CIMES -> {
                double line = 300 + 28 * slow(x, z);
                if (h >= line && (slope <= 5 || hash(x >> 1, 2, z >> 1) < 0.5f)) { top = SNOW; layers = slope <= 3 ? 3 : 2; }
                else if (slope <= 2 && h < 150) { top = GRASS; layers = 3; }
            }
            case MapData.B_FORET -> { top = under ? DIRT : hash(x, 3, z) < 0.18f ? PODZOL : GRASS; layers = 3; }
            case 2 -> { top = under ? DIRT : hash(x, 4, z) < 0.06f ? COARSE : GRASS; layers = 3; }
            default -> { top = under ? DIRT : GRASS; layers = 3; }
        }
        if (top != null && depth < layers) {
            if (depth == 0) return top;
            if (top == GRASS || top == PODZOL || top == COARSE) return DIRT;
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
                BlockState topState = AIR, floor = AIR;
                for (int y = minY; y <= top; y++) {
                    BlockState s = block(x, y, z, h, wt, b, slope);
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(lx, y & 15, lz, s, false);
                    if (y == h) floor = s;
                    if (y == top) topState = s;
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
