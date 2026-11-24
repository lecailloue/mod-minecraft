package net.ringsofash.world;

import net.ringsofash.RingsOfAsh;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

/**
 * Les grandes zones peintes sur la carte : prairies de la Comte (1), foret elfique (2), foret de Fangorn (3), marais des morts (4).
 * Lues dans data/ringsofash/map/zones.bin (grille de 1000 x 1000 cases de 5 blocs), puis rééchantillonnées en cases de 4 blocs
 * avec des bords irreguliers (le trace a la main est trop lisse pour une frontiere naturelle).
 */
public final class Zones {
    public static final int NONE = 0, SHIRE = 1, ELVEN = 2, FANGORN = 3, MARSH = 4, CYPRESS = 5;
    /** numeros de biome (dans la liste de la dimension) de chaque zone */
    public static final int B_SHIRE = 13, B_ELVEN = 14, B_FANGORN = 15, B_MARSH = 16, B_CYPRESS = 17;

    private static final int RES = 4, HALF = 2500, M = 2 * HALF / RES;   // 1250 cases de 4 blocs
    private static byte[] grid;
    private static float[] marshDepth;      // distance (en blocs) au bord le plus proche, a l'interieur du marais

    private Zones() {}

    private static synchronized void load() {
        if (grid != null) return;
        byte[] src = new byte[0];
        int n = 1000;
        try (InputStream raw = Zones.class.getResourceAsStream("/data/ringsofash/map/zones.bin")) {
            if (raw != null) {
                byte[] all = raw.readAllBytes();
                ByteBuffer bb = ByteBuffer.wrap(all);
                bb.position(4);
                n = bb.getInt();
                src = new InflaterInputStream(new java.io.ByteArrayInputStream(all, 8, all.length - 8)).readAllBytes();
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("zones.bin illisible : pas de zones peintes", e);
        }
        byte[] g = new byte[M * M];
        if (src.length >= n * n) {
            float[] w = new float[6];
            double cs = 2500.0 * 2 / n;
            for (int j = 0; j < M; j++) for (int i = 0; i < M; i++) {
                double x = -HALF + i * RES + RES / 2.0, z = -HALF + j * RES + RES / 2.0;
                // bords irreguliers : on lit la zone un peu plus loin, decalage de grande et de petite echelle
                double wx = x + (MapChunkGenerator.fbm(x * 0.013, z * 0.013, 61) - 0.5) * 90 + (MapChunkGenerator.fbm(x * 0.07, z * 0.07, 62) - 0.5) * 18;
                double wz = z + (MapChunkGenerator.fbm(x * 0.013 + 70, z * 0.013, 63) - 0.5) * 90 + (MapChunkGenerator.fbm(x * 0.07, z * 0.07 + 30, 64) - 0.5) * 18;
                double fx = (wx + HALF) / cs - 0.5, fz = (wz + HALF) / cs - 0.5;
                int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
                float tx = (float) (fx - ix), tz = (float) (fz - iz);
                java.util.Arrays.fill(w, 0);
                for (int dz = 0; dz < 2; dz++) for (int dx = 0; dx < 2; dx++) {
                    int cx = Math.max(0, Math.min(n - 1, ix + dx)), cz = Math.max(0, Math.min(n - 1, iz + dz));
                    w[src[cz * n + cx]] += (dx == 0 ? 1 - tx : tx) * (dz == 0 ? 1 - tz : tz);
                }
                int best = 0;
                float bw = 0.5f;
                for (int c = 1; c < 6; c++) if (w[c] > bw) { bw = w[c]; best = c; }
                g[j * M + i] = (byte) best;
            }
        }
        // distance au bord du marais (balayage en deux passes, cases de 4 blocs)
        float[] d = new float[M * M];
        for (int k = 0; k < d.length; k++) d[k] = g[k] == MARSH ? 1e9f : 0;
        for (int j = 0; j < M; j++) for (int i = 0; i < M; i++) {
            int k = j * M + i;
            if (d[k] == 0) continue;
            float v = d[k];
            if (i > 0) v = Math.min(v, d[k - 1] + 1);
            if (j > 0) { v = Math.min(v, d[k - M] + 1); if (i > 0) v = Math.min(v, d[k - M - 1] + 1.414f); if (i < M - 1) v = Math.min(v, d[k - M + 1] + 1.414f); }
            d[k] = v;
        }
        for (int j = M - 1; j >= 0; j--) for (int i = M - 1; i >= 0; i--) {
            int k = j * M + i;
            if (d[k] == 0) continue;
            float v = d[k];
            if (i < M - 1) v = Math.min(v, d[k + 1] + 1);
            if (j < M - 1) { v = Math.min(v, d[k + M] + 1); if (i < M - 1) v = Math.min(v, d[k + M + 1] + 1.414f); if (i > 0) v = Math.min(v, d[k + M - 1] + 1.414f); }
            d[k] = v;
        }
        for (int k = 0; k < d.length; k++) d[k] = Math.min(d[k], 200f) * RES;
        marshDepth = d;
        grid = g;
    }

    /** zone peinte a cet endroit (0 si aucune) ; la prairie de la Comte comprend aussi l'ancien contour rouge */
    public static int zone(int x, int z) {
        if (grid == null) load();
        int i = Math.floorDiv(x + HALF, RES), j = Math.floorDiv(z + HALF, RES);
        int c = i < 0 || j < 0 || i >= M || j >= M ? 0 : grid[j * M + i];
        if (c == NONE && ShireDecorFeature.inPoly(x, z)) return SHIRE;
        return c;
    }

    /** vrai si la zone approche du chunk (bords compris, a 16 blocs pres) */
    public static boolean touches(int x0, int z0, int zn) {
        for (int dz = -16; dz <= 32; dz += 8) for (int dx = -16; dx <= 32; dx += 8) if (zone(x0 + dx, z0 + dz) == zn) return true;
        return false;
    }

    /** vrai dans les prairies de la Comte */
    public static boolean inShire(int x, int z) { return zone(x, z) == SHIRE; }

    /** biome de plaine ou de bois que la zone remplace par le sien */
    private static boolean plain(int b) { return b == 0 || b == 1 || b == 3 || b == 10 || b == 11; }

    /** biome effectif (numero dans la liste de la dimension) : le biome de la carte, ou celui de la zone peinte */
    public static int biomeFor(int b, int x, int z) {
        if (b != 0 && b != 1 && b != 3 && b != 4 && b != 10 && b != 11) return b;
        int zn = zone(x, z);
        if (zn == NONE) return b;
        if (b == 4) return zn == ELVEN ? B_ELVEN : b;                 // les monts rocheux ne deviennent forets que chez les elfes
        return B_SHIRE - 1 + zn;
    }

    /** poids (0..1) du marais a cet endroit : 0 au bord, 1 a une cinquantaine de blocs a l'interieur */
    public static float marshWeight(int x, int z) {
        if (grid == null) load();
        int i = Math.floorDiv(x + HALF, RES), j = Math.floorDiv(z + HALF, RES);
        if (i < 0 || j < 0 || i >= M || j >= M) return 0;
        float d = marshDepth[j * M + i];
        if (d <= 0) return 0;
        float t = Math.min(1f, d / 56f);
        return t * t * (3 - 2 * t);
    }

    /** relief du marais : plaines basses, a peine au dessus de l'eau, creusees de mares ; les monts et les rivieres ne bougent pas */
    public static int marshHeight(int x, int z, int h, int rawBiome) {
        if (!plain(rawBiome)) return h;
        float w = marshWeight(x, z);
        if (w <= 0) return h;
        float n = MapChunkGenerator.fbm(x * 0.035, z * 0.035, 66);
        float big = MapChunkGenerator.fbm(x * 0.012, z * 0.012, 67);
        float target = MapData.SEA + 0.9f + (n - 0.5f) * 8f - Math.max(0, big - 0.55f) * 22f;
        if (target >= h) return h;
        return Math.round(h + (target - h) * w);
    }
}
