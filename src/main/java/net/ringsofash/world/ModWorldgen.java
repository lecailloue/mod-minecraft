package net.ringsofash.world;

import net.minecraft.core.registries.Registries;
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

    public static final DeferredHolder<Feature<?>, VolcanicSurfaceFeature> VOLCANIC_SURFACE = FEATURES.register("volcanic_surface", VolcanicSurfaceFeature::new);
    public static final DeferredHolder<Feature<?>, SpireFeature> SPIRE = FEATURES.register("spire", SpireFeature::new);
    public static final DeferredHolder<Feature<?>, BigTreeFeature> BIG_TREE = FEATURES.register("big_tree", BigTreeFeature::new);

    public static final DeferredHolder<StructureType<?>, StructureType<VolcanoStructure>> VOLCANO =
            STRUCTURE_TYPES.register("volcano", () -> (StructureType<VolcanoStructure>) () -> VolcanoStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> VOLCANO_PIECE =
            PIECES.register("volcano", () -> (StructurePieceType.ContextlessType) VolcanoPiece::new);

    public static final ResourceKey<Level> LANDS = ResourceKey.create(Registries.DIMENSION, RingsOfAsh.id("hautemarche"));
    public static final ResourceKey<Biome> VOLCANO_BIOME = biome("volcan");
    public static final ResourceKey<Biome> HALFLING_BIOME = biome("vertes_collines");
    public static final ResourceKey<Biome> ASH_BIOME = biome("terres_de_cendre");
    public static final TagKey<Structure> VOLCANOES = TagKey.create(Registries.STRUCTURE, RingsOfAsh.id("volcanoes"));
    public static final ResourceKey<Structure> VOLCANO_STRUCTURE = ResourceKey.create(Registries.STRUCTURE, RingsOfAsh.id("volcano"));
    public static final ResourceKey<Structure> DOOM_STRUCTURE = ResourceKey.create(Registries.STRUCTURE, RingsOfAsh.id("mont_funeste"));

    public static ResourceKey<Biome> biome(String name) { return ResourceKey.create(Registries.BIOME, RingsOfAsh.id(name)); }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
        STRUCTURE_TYPES.register(bus);
        PIECES.register(bus);
    }

    private ModWorldgen() {}
}
