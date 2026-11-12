package net.ringsofash.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ringsofash.RingsOfAsh;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

/**
 * La carte predefinie de la Hautemarche : 5000 x 5000 blocs (grille de 2000 cases, interpolee et detaillee par du bruit), centree sur (0, 0).
 * Lue depuis data/ringsofash/map/hautemarche.bin (hauteurs, niveau d'eau des rivieres, biome).
 * Elle est identique pour tous les mondes, quelle que soit la graine.
 */
public final class MapData {
    public static final int N = 2000, SIZE = 5000, HALF = SIZE / 2;
    public static final double SCALE = SIZE / (double) N;   // grille de 2000 cases de 2,5 blocs = 5000 x 5000 blocs
    public static final int SEA = 63;
    public static final int B_MONTS_CENDRE = 9, B_CLAIR = 10, B_BOSQ = 11, B_LAVE = 12;
    public static final int B_MER = 6, B_RIVAGE = 7, B_RIVIERE = 8, B_CIMES = 5, B_MONTS = 4, B_FORET = 3;
    public static final String[] BIOMES = {"plaines_vertes", "plaines_jaunes", "plaines_cendre", "grande_foret", "monts_rocheux",
            "cimes_neigeuses", "mer_grise", "rivage", "riviere", "monts_de_cendre", "foret_clairsemee", "bosquets", "rivieres_de_lave"};

    private static short[] height;
    private static short[] water;
    private static byte[] biome;
    private static int[] volcano;        // x, z, rayon, niveau de lave
    private static final Map<String, int[]> PLACES = new LinkedHashMap<>();
    private static boolean loaded;

    private MapData() {}

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream raw = MapData.class.getResourceAsStream("/data/ringsofash/map/hautemarche.bin")) {
            if (raw == null) throw new IllegalStateException("hautemarche.bin introuvable");
            byte[] all = raw.readAllBytes();
            ByteBuffer head = ByteBuffer.wrap(all);
            head.position(4);
            int n = head.getInt();
            if (n != N) throw new IllegalStateException("taille de carte inattendue : " + n);
            byte[] data = new InflaterInputStream(new java.io.ByteArrayInputStream(all, 8, all.length - 8)).readAllBytes();
            ByteBuffer bb = ByteBuffer.wrap(data);
            height = new short[N * N];
            water = new short[N * N];
            biome = new byte[N * N];
            bb.asShortBuffer().get(height);
            bb.position(N * N * 2);
            bb.asShortBuffer().get(water);
            bb.position(N * N * 4);
            bb.get(biome);
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("Carte de la Hautemarche illisible : tout sera de la mer", e);
            height = new short[N * N];
            water = new short[N * N];
            biome = new byte[N * N];
            java.util.Arrays.fill(height, (short) (30 + 64));
            java.util.Arrays.fill(biome, (byte) B_MER);
        }
        try (InputStream in = MapData.class.getResourceAsStream("/data/ringsofash/map/volcan.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
                volcano = new int[]{o.get("x").getAsInt(), o.get("z").getAsInt(), o.get("r").getAsInt(), o.get("level").getAsInt()};
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("Volcan illisible", e);
        }
        try (InputStream in = MapData.class.getResourceAsStream("/data/ringsofash/map/places.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    JsonObject p = e.getValue().getAsJsonObject();
                    PLACES.put(e.getKey(), new int[]{p.get("x").getAsInt(), p.get("z").getAsInt()});
                }
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("Lieux de la carte illisibles", e);
        }
    }

    private static int cell(int gx, int gz) {
        return Math.max(0, Math.min(N - 1, gz)) * N + Math.max(0, Math.min(N - 1, gx));
    }

    public static boolean inside(int x, int z) { return x >= -HALF && x < HALF && z >= -HALF && z < HALF; }

    private static float grid(short[] a, double gx, double gz) {
        int ix = (int) Math.floor(gx), iz = (int) Math.floor(gz);
        float fx = (float) (gx - ix), fz = (float) (gz - iz);
        float a0 = a[cell(ix, iz)] & 0xFFFF, a1 = a[cell(ix + 1, iz)] & 0xFFFF, b0 = a[cell(ix, iz + 1)] & 0xFFFF, b1 = a[cell(ix + 1, iz + 1)] & 0xFFFF;
        return (a0 + (a1 - a0) * fx) * (1 - fz) + (b0 + (b1 - b0) * fx) * fz;
    }

    /** Altitude du dernier bloc solide : carte interpolee + relief fin (aretes, rochers) qui grandit avec l'altitude. */
    public static int height(int x, int z) {
        if (!loaded) load();
        if (!inside(x, z)) return 28;
        double gx = (x + HALF) / SCALE - 0.5, gz = (z + HALF) / SCALE - 0.5;
        float g = grid(height, gx, gz) - 64;
        double amp = 1.4 * Math.max(0, Math.min(1, (g - 65) / 5)) + 3 * Math.max(0, Math.min(1, (g - 85) / 40)) + 26 * Math.max(0, Math.min(1, (g - 105) / 130));
        if (amp > 0) {
            float n1 = MapChunkGenerator.fbm(x * 0.03, z * 0.03, 41) - 0.5f;
            float rid = 1 - Math.abs(2 * MapChunkGenerator.fbm(x * 0.011, z * 0.011, 42) - 1);
            g += (float) (amp * (n1 * 1.5 + (rid - 0.45f) * 0.9));
            if (g > 85) {                                      // crevasses, aretes et blocs de roche : relief anguleux
                float rid2 = 1 - Math.abs(2 * MapChunkGenerator.fbm(x * 0.035, z * 0.035, 44) - 1);
                float n3 = MapChunkGenerator.fbm(x * 0.11, z * 0.11, 43) - 0.5f;
                g += (float) (amp * (0.5 * (rid2 - 0.5f) + 0.5 * n3));
            }
        }
        return Math.round(g);
    }

    /** Altitude de la surface du liquide : eau (mer, rivieres) ou lave dans le cratere du volcan ; MIN_VALUE s'il n'y en a pas. */
    public static int waterTop(int x, int z) {
        if (!loaded) load();
        if (!inside(x, z)) return SEA;
        int h = height(x, z);
        if (volcano != null) {
            long dx = x - volcano[0], dz = z - volcano[1];
            if (dx * dx + dz * dz <= (long) volcano[2] * volcano[2] && h < volcano[3]) return volcano[3];
        }
        return h < SEA ? SEA : Integer.MIN_VALUE;
    }

    public static int biome(int x, int z) {
        if (!loaded) load();
        if (!inside(x, z)) return B_MER;
        int gx = (int) Math.floor((x + HALF) / SCALE), gz = (int) Math.floor((z + HALF) / SCALE);
        int b0 = biome[cell(gx, gz)];
        if (b0 == B_MER || b0 == B_RIVIERE || b0 == B_RIVAGE || b0 == B_LAVE) return b0;
        // frontieres irregulieres : on decale legerement le point lu
        double wx = x + (MapChunkGenerator.fbm(x * 0.02, z * 0.02, 51) - 0.5) * 70, wz = z + (MapChunkGenerator.fbm(x * 0.02 + 40, z * 0.02, 52) - 0.5) * 70;
        int b1 = biome[cell((int) Math.floor((wx + HALF) / SCALE), (int) Math.floor((wz + HALF) / SCALE))];
        return b1 == B_MER || b1 == B_RIVIERE || b1 == B_RIVAGE || b1 == B_LAVE ? b0 : b1;
    }

    /** Pente locale : plus grand ecart d'altitude avec les 4 voisins. */
    public static int slope(int x, int z) {
        int h = height(x, z);
        return Math.max(Math.max(Math.abs(height(x + 1, z) - h), Math.abs(height(x - 1, z) - h)),
                Math.max(Math.abs(height(x, z + 1) - h), Math.abs(height(x, z - 1) - h)));
    }

    public static Map<String, int[]> places() {
        if (!loaded) load();
        return PLACES;
    }

    public static int[] place(String name) { return places().get(name); }
}
