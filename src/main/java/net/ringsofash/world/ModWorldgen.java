package net.ringsofash.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, RingsOfAsh.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, RingsOfAsh.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, RingsOfAsh.MODID);

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, RingsOfAsh.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, RingsOfAsh.MODID);
    static {
        GENERATORS.register("map", () -> MapChunkGenerator.CODEC);
        BIOME_SOURCES.register("map", () -> MapBiomeSource.CODEC);
    }

    public static final DeferredHolder<Feature<?>, VolcanicSurfaceFeature> VOLCANIC_SURFACE = FEATURES.register("volcanic_surface", VolcanicSurfaceFeature::new);
    public static final DeferredHolder<Feature<?>, SpireFeature> SPIRE = FEATURES.register("spire", SpireFeature::new);
    public static final DeferredHolder<Feature<?>, RockFeature> ROCK = FEATURES.register("rock", RockFeature::new);
    public static final DeferredHolder<Feature<?>, CraterSmokeFeature> CRATER_SMOKE = FEATURES.register("crater_smoke", CraterSmokeFeature::new);
    public static final DeferredHolder<Feature<?>, FallenLogFeature> FALLEN_LOG = FEATURES.register("fallen_log", FallenLogFeature::new);
    public static final DeferredHolder<Feature<?>, ShireDecorFeature> SHIRE_DECOR = FEATURES.register("shire_decor", ShireDecorFeature::new);
    public static final DeferredHolder<Feature<?>, ZoneDecorFeature> ELVEN_DECOR = FEATURES.register("elven_forest_decor", () -> new ZoneDecorFeature(ZoneDecorFeature.ELVEN));
    public static final DeferredHolder<Feature<?>, ZoneDecorFeature> FANGORN_DECOR = FEATURES.register("fangorn_decor", () -> new ZoneDecorFeature(ZoneDecorFeature.FANGORN));
    public static final DeferredHolder<Feature<?>, ZoneDecorFeature> MARSH_DECOR = FEATURES.register("marsh_decor", () -> new ZoneDecorFeature(ZoneDecorFeature.MARSH));
    public static final DeferredHolder<Feature<?>, ZoneDecorFeature> CYPRESS_DECOR = FEATURES.register("cypress_decor", () -> new ZoneDecorFeature(ZoneDecorFeature.CYPRESS));
    public static final DeferredHolder<Feature<?>, BigTreeFeature> BIG_TREE = FEATURES.register("big_tree", BigTreeFeature::new);

    public static final DeferredHolder<StructureType<?>, StructureType<VolcanoStructure>> VOLCANO =
            STRUCTURE_TYPES.register("volcano", () -> (StructureType<VolcanoStructure>) () -> VolcanoStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> VOLCANO_PIECE =
            PIECES.register("volcano", () -> (StructurePieceType.ContextlessType) VolcanoPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<Settlement.S>> SETTLEMENT =
            STRUCTURE_TYPES.register("settlement", () -> (StructureType<Settlement.S>) () -> Settlement.S.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SETTLEMENT_PIECE =
            PIECES.register("settlement", () -> (StructurePieceType.ContextlessType) Settlement.Piece::new);

    public static final ResourceKey<Level> LANDS = ResourceKey.create(Registries.DIMENSION, RingsOfAsh.id("hautemarche"));
    public static final ResourceKey<Biome> VOLCANO_BIOME = biome("volcan");
    public static final ResourceKey<Biome> HALFLING_BIOME = biome("plaines_vertes");
    public static final ResourceKey<Biome> ASH_BIOME = biome("plaines_cendre");
    public static final TagKey<Structure> VOLCANOES = TagKey.create(Registries.STRUCTURE, RingsOfAsh.id("volcanoes"));
    public static final ResourceKey<Structure> VOLCANO_STRUCTURE = ResourceKey.create(Registries.STRUCTURE, RingsOfAsh.id("volcano"));
    public static final ResourceKey<Structure> DOOM_STRUCTURE = ResourceKey.create(Registries.STRUCTURE, RingsOfAsh.id("mont_funeste"));

    public static ResourceKey<Biome> biome(String name) { return ResourceKey.create(Registries.BIOME, RingsOfAsh.id(name)); }

    public static void register(IEventBus bus) {
        GENERATORS.register(bus);
        BIOME_SOURCES.register(bus);
        FEATURES.register(bus);
        STRUCTURE_TYPES.register(bus);
        PIECES.register(bus);
    }

    private ModWorldgen() {}
}
