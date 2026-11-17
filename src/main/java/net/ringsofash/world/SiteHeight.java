package net.ringsofash.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Hauteur du sol telle que la generation la calcule pour poser une structure (sans charger de chunk). */
public final class SiteHeight {
    private SiteHeight() {}

    public static int py(ServerLevel l, int x, int z) {
        var gen = l.getChunkSource().getGenerator();
        return gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, l, l.getChunkSource().randomState());
    }
}
