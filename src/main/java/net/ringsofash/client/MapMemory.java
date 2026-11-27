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
    private static final Map<Long, int[]> SEEN = new HashMap<>();
    /** les chunks qui viennent de changer, que la carte doit repeindre */
    private static final Queue<Long> FRESH = new ArrayDeque<>();

    /** les villes ou le joueur a mis les pieds : avant cela, leur nom n'est pas sur la carte */
    private static final java.util.Set<String> VISITED = new java.util.HashSet<>();

    private static String world;
    private static boolean dirty;
    private static int sweep;
    private static boolean revealed;

    /** marque du fichier : il range quatre couleurs par chunk */
    private static final int MAGIC = 0x524F4132;        // quatre couleurs par chunk
    private static final int MAGIC4 = 0x524F4134;       // seize couleurs par chunk

    private static final int LOOK = 4;      // on releve jusqu'a 4 chunks autour du joueur
    private static final int REFRESH = 3;   // et on repasse sur les 3 plus proches

    public static void init(IEventBus bus) { bus.addListener(MapMemory::tick); }

    // ------------------------------------------------------------------ ce que la carte lit

    public static boolean known(long key) { return SEEN.containsKey(key); }

    /** la grille de CELLS x CELLS couleurs du chunk, lue ligne par ligne */
    public static int[] color(long key) { return SEEN.get(key); }

    /** cases par cote dans un chunk : 4 cases de 4 blocs */
    public static final int CELLS = 4;
    public static final int CELL = 16 / CELLS;

    public static Iterable<Map.Entry<Long, int[]>> all() { return SEEN.entrySet(); }

    /** un chunk a repeindre, ou null s'il n'y a rien de neuf */
    public static Long nextFresh() { return FRESH.poll(); }

    public static int count() { return SEEN.size(); }

    /** le nom de ce lieu est-il connu ? */
    public static boolean visited(String place) { return revealed || VISITED.contains(place); }

    /** toute la carte est-elle devoilee (/anneaux map) ? */
    public static boolean revealed() { return revealed; }

    // ------------------------------------------------------------------ le releve

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { save(); return; }
        if (!MapScreen.inLands()) return;
        load(mc);
        if (net.ringsofash.data.Net.ClientLore.mapRevealed && !revealed) {
            revealed = true;
            dirty = true;
            MapScreen.forget();
        }
        if (mc.player.tickCount % 2400 == 0) write();     // deux minutes : on met a l'abri
        if (mc.player.tickCount % 10 != 0) return;
        town(mc);

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

    /** on retient la ville ou l'on vient d'entrer : c'est ce qui donne son nom sur la carte */
    private static void town(Minecraft mc) {
        if (mc.player == null) return;
        int x = mc.player.getBlockX(), z = mc.player.getBlockZ();
        for (var e : MapData.places().entrySet()) {
            int r = net.ringsofash.world.Towns.radius(e.getKey());
            if (r <= 0 || VISITED.contains(e.getKey())) continue;
            long dx = x - e.getValue()[0], dz = z - e.getValue()[1];
            if (dx * dx + dz * dz <= (long) r * r) { VISITED.add(e.getKey()); dirty = true; }
        }
    }

    /** la couleur d'un chunk : la teinte moyenne de ce qu'on voit du ciel, ombree par le relief */
    private static void scan(Level level, int cx, int cz, long key) {
        if (!level.hasChunk(cx, cz)) return;
        int wx = cx << 4, wz = cz << 4;
        if (!MapData.inside(wx, wz)) return;
        ChunkAccess chunk = level.getChunk(cx, cz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] cells = new int[CELLS * CELLS];
        boolean any = false;
        // une couleur par case de quatre blocs : de quoi tenir le zoom
        for (int cj = 0; cj < CELLS; cj++) {
            for (int ci = 0; ci < CELLS; ci++) {
                long r = 0, g = 0, b = 0;
                int n = 0, hsum = 0;
                for (int ox = 1; ox < CELL; ox += 2) {
                    for (int oz = 1; oz < CELL; oz += 2) {
                        int bx = wx + ci * CELL + ox, bz = wz + cj * CELL + oz;
                        int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
                        pos.set(bx, h - 1, bz);
                        BlockState st = chunk.getBlockState(pos);
                        if (st.isAir()) continue;
                        int col = BlockTint.of(st, level, pos);
                        r += (col >> 16) & 255; g += (col >> 8) & 255; b += col & 255;
                        hsum += h;
                        n++;
                    }
                }
                if (n == 0) continue;
                int avg = hsum / n;
                float shade = avg >= MapData.SEA
                        ? Math.min(1.25F, 0.88F + (avg - MapData.SEA) * 0.004F)
                        : Math.max(0.45F, 0.95F - (MapData.SEA - avg) * 0.012F);
                cells[cj * CELLS + ci] = 0xFF000000
                        | (Math.min(255, (int) (r / n * shade)) << 16)
                        | (Math.min(255, (int) (g / n * shade)) << 8)
                        | Math.min(255, (int) (b / n * shade));
                any = true;
            }
        }
        if (!any) return;
        int[] old = SEEN.put(key, cells);
        if (old == null || !java.util.Arrays.equals(old, cells)) { FRESH.add(key); dirty = true; }
    }

    // ------------------------------------------------------------------ le fichier

    /** un vieux releve a quatre quarts devient une grille de seize cases */
    private static int[] spread(int[] q) {
        int[] cells = new int[CELLS * CELLS];
        for (int j = 0; j < CELLS; j++)
            for (int i = 0; i < CELLS; i++)
                cells[j * CELLS + i] = q[(j >= CELLS / 2 ? 2 : 0) + (i >= CELLS / 2 ? 1 : 0)];
        return cells;
    }

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
        VISITED.clear();
        world = key;
        dirty = false;
        revealed = net.ringsofash.data.Net.ClientLore.mapRevealed;
        Path p = file(key);
        if (!Files.exists(p)) { MapScreen.forget(); return; }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(p)))) {
            int first = in.readInt();
            if (first == MAGIC4) {                      // format courant : seize couleurs
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    long k = in.readLong();
                    int[] cells = new int[CELLS * CELLS];
                    for (int c = 0; c < cells.length; c++) cells[c] = in.readInt();
                    SEEN.put(k, cells);
                }
            } else if (first == MAGIC) {                // quatre couleurs : on les etale
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    long k = in.readLong();
                    int[] q = {in.readInt(), in.readInt(), in.readInt(), in.readInt()};
                    SEEN.put(k, spread(q));
                }
            } else {                                    // une seule couleur
                for (int i = 0; i < first; i++) {
                    long k = in.readLong();
                    int c = in.readInt();
                    int[] cells = new int[CELLS * CELLS];
                    java.util.Arrays.fill(cells, c);
                    SEEN.put(k, cells);
                }
            }
            revealed = in.readBoolean();
            int v = in.readInt();
            for (int i = 0; i < v; i++) VISITED.add(in.readUTF());
        } catch (IOException | RuntimeException ignored) {
            SEEN.clear();
            VISITED.clear();
        }
        MapScreen.forget();      // la carte sera redessinee avec ce qu'on vient de relire
    }

    private static void write() {
        if (world == null || !dirty) return;
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file(world))))) {
            out.writeInt(MAGIC4);
            out.writeInt(SEEN.size());
            for (Map.Entry<Long, int[]> e : SEEN.entrySet()) {
                out.writeLong(e.getKey());
                for (int c : e.getValue()) out.writeInt(c);
            }
            out.writeBoolean(revealed);
            out.writeInt(VISITED.size());
            for (String v : VISITED) out.writeUTF(v);
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
        revealed = false;
        net.ringsofash.data.Net.ClientLore.mapRevealed = false;
        SEEN.clear();
        FRESH.clear();
        VISITED.clear();
        BlockTint.clear();
        MapScreen.forget();
    }
}
