package net.ringsofash.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.ringsofash.RingsOfAsh;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

/**
 * Fondcombe, d'apres une schematique fournie : la vallee (rivendell.bin, 260 x 380 colonnes) est recopiee dans le monde par morceaux de TW x TD blocs. Chaque colonne donne le sol,
 * le premier bloc conserve, puis une suite de (longueur, bloc) jusqu'au dernier bloc. Pres du bord, le terrain est fondu dans le relief naturel.
 */
public final class RivendellStamp {
    private RivendellStamp() {}

    public static final int TW = 163, TD = 120, TILES_X = 2, TILES_Z = 4;
    /** taille du fichier : 260 x 380 ; la marge entre la region recopiee et le fichier */
    public static final int FW = 260, FD = 380;
    public static final int PADX = (TILES_X * TW - FW) / 2, PADZ = (TILES_Z * TD - FD) / 2;
    /** colonne (x, z) du fichier qui tombe sur le lieu marque sur la carte */
    public static final int CX = 130, CZ = 190;
    private static final double BLEND = 70;

    private static boolean loaded;
    private static int W, D, REF;
    private static BlockState[] pal;
    private static short[] ground, start;
    private static int[] colOff;
    private static byte[] data;

    /** coin nord-ouest du fichier dans le monde, quand le lieu marque est en (px, pz) */
    public static int originX(int px) { return px - CX; }
    public static int originZ(int pz) { return pz - CZ; }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream raw = RivendellStamp.class.getResourceAsStream("/data/ringsofash/map/rivendell.bin")) {
            if (raw == null) throw new IllegalStateException("rivendell.bin introuvable");
            ByteBuffer bb = ByteBuffer.wrap(new InflaterInputStream(raw).readAllBytes());
            bb.position(4);
            W = bb.getInt(); D = bb.getInt(); REF = bb.getInt();
            int n = bb.getInt();
            pal = new BlockState[n];
            for (int i = 0; i < n; i++) {
                byte[] s = new byte[bb.getShort() & 0xFFFF];
                bb.get(s);
                pal[i] = parse(new String(s, java.nio.charset.StandardCharsets.UTF_8));
            }
            data = bb.array();
            ground = new short[W * D];
            start = new short[W * D];
            colOff = new int[W * D];
            int p = bb.position();
            for (int i = 0; i < W * D; i++) {
                ground[i] = (short) (((data[p] & 0xFF) << 8) | (data[p + 1] & 0xFF));
                start[i] = (short) (((data[p + 2] & 0xFF) << 8) | (data[p + 3] & 0xFF));
                p += 4;
                colOff[i] = p;
                int runs = 0, sh = 0, b;
                do { b = data[p++] & 0xFF; runs |= (b & 127) << sh; sh += 7; } while (b >= 128);
                for (int r = 0; r < runs * 2; r++) { while ((data[p++] & 0x80) != 0) { } }
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("Fondcombe illisible", e);
            W = 0; D = 0;
        }
    }

    private static <T extends Comparable<T>> BlockState set(BlockState st, Property<T> p, String v) {
        return p.getValue(v).map(x -> st.setValue(p, x)).orElse(st);
    }

    private static BlockState parse(String s) {
        int br = s.indexOf('[');
        String name = br < 0 ? s : s.substring(0, br);
        Block bl = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
        BlockState st = bl.defaultBlockState();
        if (br >= 0) {
            for (String kv : s.substring(br + 1, s.length() - 1).split(",")) {
                int eq = kv.indexOf('=');
                if (eq < 0) continue;
                Property<?> p = bl.getStateDefinition().getProperty(kv.substring(0, eq));
                if (p != null) st = set(st, p, kv.substring(eq + 1));
            }
        }
        return st;
    }

    private static int varint(int[] pos) {
        int v = 0, sh = 0, b;
        do { b = data[pos[0]++] & 0xFF; v |= (b & 127) << sh; sh += 7; } while (b >= 128);
        return v;
    }

    /** recopie le morceau (tx, tz) ; offr = decalage vertical (altitude du lieu - sol de reference du fichier) */
    public static void stamp(Build b, int tx, int tz, int offr) {
        load();
        if (W == 0) return;
        BlockState stone = Blocks.STONE.defaultBlockState();
        int xa = Math.max(b.clip.minX(), b.ox), xb = Math.min(b.clip.maxX(), b.ox + TW - 1);
        int za = Math.max(b.clip.minZ(), b.oz), zb = Math.min(b.clip.maxZ(), b.oz + TD - 1);
        for (int wz = za; wz <= zb; wz++) for (int wx = xa; wx <= xb; wx++) {
            int fx = tx * TW + (wx - b.ox) - PADX, fz = tz * TD + (wz - b.oz) - PADZ;
            if (fx < 0 || fx >= W || fz < 0 || fz >= D) continue;
            int ix = fz * W + fx;
            if (ground[ix] < -900) continue;
            int tg = MapData.height(wx, wz);
            int edge = Math.min(Math.min(fx, W - 1 - fx), Math.min(fz, D - 1 - fz));
            double t = Math.max(0, 1 - edge / BLEND);
            double w = t * t * (3 - 2 * t);
            int y0 = offr + (int) Math.round(w * (tg - (ground[ix] + offr)));
            int lx = wx - b.ox, lz = wz - b.oz;
            int[] pp = {colOff[ix]};
            int runs = varint(pp);
            int y = start[ix], base = y + y0;
            // sol rehausse : de la pierre sous le premier bloc conserve, jusqu'au relief naturel
            for (int wy = Math.min(tg, base - 1) - 2; wy < base; wy++) b.s(lx, wy - b.oy, lz, stone);
            int top = y - 1;
            for (int r = 0; r < runs; r++) {
                int len = varint(pp), id = varint(pp);
                BlockState st = id != 0 ? pal[id] : Build.AIR;
                for (int k = 0; k < len; k++) b.s(lx, y + k + y0 - b.oy, lz, st);
                if (id != 0) top = y + len - 1;
                y += len;
            }
            for (int wy = top + y0 + 1; wy <= Math.max(tg, top + y0) + 16; wy++) b.s(lx, wy - b.oy, lz, Build.AIR);
        }
    }

    /** decalage vertical : altitude naturelle du lieu moins le sol de reference du fichier */
    public static int offset(int px, int pz) { load(); return MapData.height(px, pz) - REF; }
}
