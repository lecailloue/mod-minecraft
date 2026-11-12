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
 * La carte predefinie de la Hautemarche : 2000 x 2000 blocs, centree sur (0, 0).
 * Lue depuis data/ringsofash/map/hautemarche.bin (hauteurs, niveau d'eau des rivieres, biome).
 * Elle est identique pour tous les mondes, quelle que soit la graine.
 */
public final class MapData {
    public static final int N = 2000, HALF = N / 2;
    public static final int SEA = 63;
    public static final int B_MONTS_CENDRE = 9, B_CLAIR = 10, B_BOSQ = 11;
    public static final int B_MER = 6, B_RIVAGE = 7, B_RIVIERE = 8, B_CIMES = 5, B_MONTS = 4, B_FORET = 3;
    public static final String[] BIOMES = {"plaines_vertes", "plaines_jaunes", "plaines_cendre", "grande_foret", "monts_rocheux",
            "cimes_neigeuses", "mer_grise", "rivage", "riviere", "monts_de_cendre", "foret_clairsemee", "bosquets"};

    private static short[] height;
    private static short[] water;
    private static byte[] biome;
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

    private static int idx(int x, int z) {
        int ix = Math.max(0, Math.min(N - 1, x + HALF)), iz = Math.max(0, Math.min(N - 1, z + HALF));
        return iz * N + ix;
    }

    public static boolean inside(int x, int z) { return x >= -HALF && x < HALF && z >= -HALF && z < HALF; }

    /** Altitude du dernier bloc solide. */
    public static int height(int x, int z) {
        if (!loaded) load();
        if (!inside(x, z)) return 28;
        return (height[idx(x, z)] & 0xFFFF) - 64;
    }

    /** Altitude de la surface de l'eau (riviere ou mer), ou Integer.MIN_VALUE s'il n'y a pas d'eau. */
    public static int waterTop(int x, int z) {
        if (!loaded) load();
        if (!inside(x, z)) return SEA;
        int i = idx(x, z);
        int w = water[i] & 0xFFFF;
        if (w > 0) return w - 64;
        return (height[i] & 0xFFFF) - 64 < SEA ? SEA : Integer.MIN_VALUE;
    }

    public static int biome(int x, int z) {
        if (!loaded) load();
        if (!inside(x, z)) return B_MER;
        return biome[idx(x, z)];
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
