package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ringsofash.gen.Reg;

/** Bouches de fumee dans le cratere du volcan et sur ses flancs (grille irreguliere deterministe). */
public class CraterSmokeFeature extends Feature<NoneFeatureConfiguration> {
    public CraterSmokeFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        int[] v = MapData.volcano();
        if (v == null) return false;
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        long cx = x0 + 8 - v[0], cz = z0 + 8 - v[1];
        long reach = v[2] + 40;
        if (cx * cx + cz * cz > (reach + 12) * (reach + 12)) return false;
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                long ddx = x - v[0], ddz = z - v[1];
                long d2 = ddx * ddx + ddz * ddz;
                if (d2 > reach * reach) continue;
                long h = (x * 73856093L) ^ (z * 19349663L) ^ 0x5A0CE5L;
                h = (h ^ (h >>> 13)) * 1274126177L; h ^= h >>> 16;
                int chance = d2 <= (long) v[2] * v[2] ? 16 : 80;       // dense dans le cratere, plus clairseme autour
                if (Math.floorMod(h, chance) != 0) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                p.set(x, Math.max(top, v[3] + 1), z);
                if (level.getBlockState(p).isAir()) { level.setBlock(p, Reg.SMOKE_VENT.get().defaultBlockState(), 2); any = true; }
            }
        }
        return any;
    }
}
