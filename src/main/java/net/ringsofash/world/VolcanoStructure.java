package net.ringsofash.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Volcan geant : un seul morceau procedural qui s'etend sur de nombreux chunks. */
public class VolcanoStructure extends Structure {
    public static final MapCodec<VolcanoStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.INT.fieldOf("radius").forGetter(s -> s.radius),
            Codec.INT.fieldOf("height").forGetter(s -> s.height),
            Codec.BOOL.fieldOf("doom").forGetter(s -> s.doom)).apply(i, VolcanoStructure::new));

    private final int radius;
    private final int height;
    private final boolean doom;

    public VolcanoStructure(StructureSettings settings, int radius, int height, boolean doom) {
        super(settings);
        this.radius = radius;
        this.height = height;
        this.doom = doom;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        // le volcan doit etre au coeur du biome : on verifie le biome tout autour
        int check = doom ? 40 : 36;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int bx = x + (int) (Math.cos(a) * check), bz = z + (int) (Math.sin(a) * check);
            if (!ctx.validBiome().test(ctx.chunkGenerator().getBiomeSource().getNoiseBiome(
                    QuartPos.fromBlock(bx), QuartPos.fromBlock(70), QuartPos.fromBlock(bz), ctx.randomState().sampler()))) {
                return Optional.empty();
            }
        }
        int base = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int cy = Math.max(base, ctx.chunkGenerator().getSeaLevel() + 2);
        BlockPos center = new BlockPos(x, cy, z);
        long seed = ctx.seed() ^ (x * 31L + z * 961L);
        int minY = ctx.heightAccessor().getMinBuildHeight();
        return Optional.of(new GenerationStub(center, b -> b.addPiece(new VolcanoPiece(center, radius, height, seed, doom, minY))));
    }

    @Override
    public StructureType<?> type() { return ModWorldgen.VOLCANO.get(); }
}
