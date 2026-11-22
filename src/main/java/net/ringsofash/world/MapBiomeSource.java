package net.ringsofash.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/** Source de biomes lue dans la carte predefinie (l'ordre de la liste suit MapData.BIOMES). */
public final class MapBiomeSource extends BiomeSource {
    public static final MapCodec<MapBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.biomes)).apply(i, MapBiomeSource::new));

    private final List<Holder<Biome>> biomes;

    public MapBiomeSource(List<Holder<Biome>> biomes) { this.biomes = biomes; }

    @Override
    protected MapCodec<? extends BiomeSource> codec() { return CODEC; }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() { return biomes.stream(); }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        int bx = (x << 2) + 2, bz = (z << 2) + 2;
        int i = MapData.biome(bx, bz);
        if (i == 0 && biomes.size() > 13 && ShireDecorFeature.inZone(bx, bz)) i = 13;   // la Comte : plaines sans arbres naturels
        return biomes.get(Math.min(i, biomes.size() - 1));
    }
}
