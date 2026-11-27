package net.ringsofash.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.ringsofash.world.MapData;

/**
 * Ce que le joueur a vu du monde.
 *
 * La carte ne sait rien au depart : elle est blanche. Chaque morceau de terrain que le
 * joueur traverse est releve dans le vrai monde — pas dans le generateur — et retenu
 * sous forme d'une couleur par chunk. On repasse regulierement sur les chunks tout
 * proches, si bien qu'une maison batie ou une foret abattue finit par se voir.
 *
 * Tout est garde dans un fichier a cote de la sauvegarde, pour que la decouverte reste
 * acquise d'une partie a l'autre.
 */
public final class MapMemory {
    private MapMemory() {}

    /** couleur retenue pour chaque chunk, par sa cle de position */
    private static final Map<Long, Integer> SEEN = new HashMap<>();
    /** les chunks qui viennent de changer, que la carte doit repeindre */
    private static final Queue<Long> FRESH = new ArrayDeque<>();

    private static String world;
    private static boolean dirty;
    private static int sweep;

    private static final int LOOK = 8;      // on releve jusqu'a 8 chunks autour du joueur
    private static final int REFRESH = 3;   // et on repasse sur les 3 plus proches

    public static void init(IEventBus bus) { bus.addListener(MapMemory::tick); }

    // ------------------------------------------------------------------ ce que la carte lit

    public static boolean known(long key) { return SEEN.containsKey(key); }

    public static int color(long key) { return SEEN.getOrDefault(key, 0); }

    public static Iterable<Map.Entry<Long, Integer>> all() { return SEEN.entrySet(); }

    /** un chunk a repeindre, ou null s'il n'y a rien de neuf */
    public static Long nextFresh() { return FRESH.poll(); }

    public static int count() { return SEEN.size(); }

    // ------------------------------------------------------------------ le releve

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { save(); return; }
        if (!MapScreen.inLands()) return;
        load(mc);
        if (mc.player.tickCount % 2400 == 0) write();     // deux minutes : on met a l'abri
        if (mc.player.tickCount % 10 != 0) return;

        ChunkPos here = new ChunkPos(mc.player.blockPosition());
        // un anneau par passage : on balaie tout le voisinage en quelques secondes
        int ring = sweep++ % (LOOK + 1);
        for (int dx = -ring; dx <= ring; dx++) {
            for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                int cx = here.x + dx, cz = here.z + dz;
                long key = ChunkPos.asLong(cx, cz);
                boolean close = Math.max(Math.abs(dx), Math.abs(dz)) <= REFRESH;
                if (SEEN.containsKey(key) && !close) continue;      // deja vu et loin : on laisse
                scan(mc.level, cx, cz, key);
            }
        }
    }

    /** la couleur d'un chunk : la teinte moyenne de ce qu'on voit du ciel, ombree par le relief */
    private static void scan(Level level, int cx, int cz, long key) {
        if (!level.hasChunk(cx, cz)) return;
        int wx = cx << 4, wz = cz << 4;
        if (!MapData.inside(wx, wz)) return;
        ChunkAccess chunk = level.getChunk(cx, cz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long r = 0, g = 0, b = 0;
        int n = 0, hsum = 0;
        for (int ox = 2; ox < 16; ox += 4) {
            for (int oz = 2; oz < 16; oz += 4) {
                int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, wx + ox, wz + oz);
                pos.set(wx + ox, h - 1, wz + oz);
                BlockState s = chunk.getBlockState(pos);
                MapColor mc = s.getMapColor(level, pos);
                if (mc == MapColor.NONE) continue;
                int col = mc.col;
                r += (col >> 16) & 255; g += (col >> 8) & 255; b += col & 255;
                hsum += h;
                n++;
            }
        }
        if (n == 0) return;
        int avg = hsum / n;
        // au-dessus de la mer on eclaircit avec l'altitude, en dessous on assombrit
        float shade = avg >= MapData.SEA
                ? Math.min(1.25F, 0.88F + (avg - MapData.SEA) * 0.004F)
                : Math.max(0.45F, 0.95F - (MapData.SEA - avg) * 0.012F);
        int col = 0xFF000000
                | (Math.min(255, (int) (r / n * shade)) << 16)
                | (Math.min(255, (int) (g / n * shade)) << 8)
                | Math.min(255, (int) (b / n * shade));
        Integer old = SEEN.put(key, col);
        if (old == null || old != col) { FRESH.add(key); dirty = true; }
    }

    // ------------------------------------------------------------------ le fichier

    private static Path file(String key) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("ringsofash_carte_" + key + ".dat");
    }

    /** un nom de fichier par monde : la sauvegarde locale, ou l'adresse du serveur */
    private static String worldKey(Minecraft mc) {
        String name;
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null)
            name = mc.getSingleplayerServer().getWorldData().getLevelName();
        else if (mc.getCurrentServer() != null) name = mc.getCurrentServer().ip;
        else name = "inconnu";
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) sb.append(Character.isLetterOrDigit(c) ? Character.toLowerCase(c) : '_');
        return sb.length() > 48 ? sb.substring(0, 48) : sb.toString();
    }

    private static void load(Minecraft mc) {
        String key = worldKey(mc);
        if (key.equals(world)) return;
        SEEN.clear();
        FRESH.clear();
        world = key;
        dirty = false;
        Path p = file(key);
        if (!Files.exists(p)) { MapScreen.forget(); return; }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(p)))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) SEEN.put(in.readLong(), in.readInt());
        } catch (IOException | RuntimeException ignored) {
            SEEN.clear();
        }
        MapScreen.forget();      // la carte sera redessinee avec ce qu'on vient de relire
    }

    private static void write() {
        if (world == null || !dirty) return;
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file(world))))) {
            out.writeInt(SEEN.size());
            for (Map.Entry<Long, Integer> e : SEEN.entrySet()) {
                out.writeLong(e.getKey());
                out.writeInt(e.getValue());
            }
            dirty = false;
        } catch (IOException ignored) {
            // tant pis : on reperdra la decouverte, ce n'est pas la fin du monde
        }
    }

    /** a la sortie du monde */
    private static void save() {
        if (world == null) return;
        write();
        world = null;
        dirty = false;
        SEEN.clear();
        FRESH.clear();
        MapScreen.forget();
    }
}
