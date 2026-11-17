package net.ringsofash.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Position de l'Oeil de Sauron au sommet de Barad-dur. Elle se calcule sans charger aucun chunk (meme hauteur de sol que celle de la generation),
 * donc l'oeil existe toujours dans la dimension : le serveur envoie ce point au client, qui le dessine de loin quand l'Anneau est actif.
 */
public final class EyeOfSauron {
    private EyeOfSauron() {}

    /** hauteur du centre de l'oeil au-dessus du sol de la forteresse : plateforme (TOP) + 23 pour le pied de la boite + 6 pour son centre */
    public static final int RISE = BaradPlan.TOP + 23 + 6;
    /** corruption minimale (sur 1000, soit 35 %) a partir de laquelle l'Anneau de cendre montre l'oeil */
    public static final int MIN_CORRUPTION = 350;

    private static Object cacheKey;
    private static int[] cache;

    /** {x, y du centre, z} ou null si Barad-dur n'est pas place */
    public static synchronized int[] pos(ServerLevel lands) {
        if (cacheKey == lands) return cache;
        int[] pl = MapData.place("barad_dur");
        int[] r = null;
        if (pl != null) {
            var gen = lands.getChunkSource().getGenerator();
            int py = gen.getFirstOccupiedHeight(pl[0], pl[1], Heightmap.Types.WORLD_SURFACE_WG, lands, lands.getChunkSource().randomState());
            r = new int[]{pl[0], py + RISE, pl[1]};
        }
        cacheKey = lands;
        cache = r;
        return r;
    }
}
