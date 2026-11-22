package net.ringsofash.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.ringsofash.RingsOfAsh;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

/**
 * Hobbiteville, d'apres une reconstitution fournie : le terrain et les constructions sont lus dans hobbiton.bin (colonnes de blocs, une suite de (longueur, bloc) par colonne,
 * du niveau 0 de la carte source jusqu'au dernier bloc) et recopies dans le monde, par morceaux de TW x TD blocs (un morceau = une piece de structure).
 * Pres du bord, le terrain est fondu dans le relief naturel du monde, pour qu'il n'y ait ni falaise ni trou.
 */
public final class HobbitonStamp {
    private HobbitonStamp() {}

    public static final int TW = 163, TD = 120, TILES_X = 3, TILES_Z = 4;
    /** marge entre le bord de la region recopiee (TILES_X x TILES_Z morceaux) et le fichier source : les champs y debordent sur le relief naturel */
    public static final int PADX = (TILES_X * TW - 325) / 2, PADZ = (TILES_Z * TD - 358) / 2;
    /** y du monde = y du fichier + OFF (le sol moyen de la source, -38, tombe a 72, le niveau de la plaine) */
    public static final int OFF = 110;
    /** le village : (x, z) dans le fichier, et premier z du fichier recopie */
    public static final int VILLAGE_X = 195, VILLAGE_Z = 139, FILE_Z0 = 9;
    private static final double BLEND = 80;

    private static boolean loaded;
    private static int W, D, Y0;
    private static BlockState[] pal;
    private static short[] ground;
    private static int[] colOff;
    private static byte[] data;

    /** coin nord-ouest de la region recopiee, dans le monde, quand le centre du village est en (cx, cz) */
    public static int originX(int cx) { return cx - VILLAGE_X; }
    public static int originZ(int cz) { return cz - (VILLAGE_Z - FILE_Z0); }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream raw = HobbitonStamp.class.getResourceAsStream("/data/ringsofash/map/hobbiton.bin")) {
            if (raw == null) throw new IllegalStateException("hobbiton.bin introuvable");
            ByteBuffer bb = ByteBuffer.wrap(new InflaterInputStream(raw).readAllBytes());
            bb.position(4);
            W = bb.getInt(); D = bb.getInt(); Y0 = bb.getInt();
            int n = bb.getInt();
            pal = new BlockState[n];
            for (int i = 0; i < n; i++) {
                byte[] s = new byte[bb.getShort() & 0xFFFF];
                bb.get(s);
                pal[i] = parse(new String(s, java.nio.charset.StandardCharsets.UTF_8));
            }
            data = bb.array();
            ground = new short[W * D];
            colOff = new int[W * D];
            int p = bb.position();
            for (int i = 0; i < W * D; i++) {
                ground[i] = (short) (((data[p] & 0xFF) << 8) | (data[p + 1] & 0xFF));
                p += 2;
                colOff[i] = p;
                int runs = 0, sh = 0, b;
                do { b = data[p++] & 0xFF; runs |= (b & 127) << sh; sh += 7; } while (b >= 128);
                for (int r = 0; r < runs * 2; r++) { while ((data[p++] & 0x80) != 0) { } }
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("Hobbiteville illisible", e);
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

    /** altitude de la surface d'une colonne : celle du fichier (fondue dans le relief pres du bord) ou, hors du fichier, celle du relief naturel */
    private static int surf(int fx, int fz, int wx, int wz) {
        if (fx >= 0 && fx < W && fz >= 0 && fz < D && ground[fz * W + fx] > -900) return ground[fz * W + fx] + OFF + shift(fx, fz, wx, wz);
        return MapData.height(wx, wz);
    }

    private static int shift(int fx, int fz, int wx, int wz) {
        int edge = Math.min(Math.min(fx, W - 1 - fx), Math.min(fz, D - 1 - fz));
        double t = Math.max(0, 1 - edge / BLEND);
        double w = t * t * (3 - 2 * t);
        return (int) Math.round(w * (MapData.height(wx, wz) - (ground[fz * W + fx] + OFF)));
    }

    private static final int CELL = 9;
    private static final BlockState FARM = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
    private static final BlockState WHEAT = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
    private static final BlockState POTATO = Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, 7);
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState LAMP = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState WELL = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.WATERLOGGED, true);

    /** une colonne de champ : terre, terre labouree au niveau de sa case de 9 x 9 (un point d'eau enterre au centre) et culture ; rend false si la colonne n'est pas un champ (eau, trop pentu) */
    private static boolean field(Build b, int vx, int vz, int fx, int fz, int wx, int wz, int crop, int ownSurf) {
        int cvx = Math.floorDiv(vx, CELL) * CELL + CELL / 2, cvz = Math.floorDiv(vz, CELL) * CELL + CELL / 2;
        int cwx = wx + cvx - vx, cwz = wz + cvz - vz;
        if (MapData.waterTop(cwx, cwz) != Integer.MIN_VALUE || MapData.waterTop(wx, wz) != Integer.MIN_VALUE) return false;
        int level = surf(fx + cvx - vx, fz + cvz - vz, cwx, cwz);
        if (Math.abs(ownSurf - level) > 5) return false;
        int lx = wx - b.ox, lz = wz - b.oz;
        int tg = MapData.height(wx, wz);
        for (int y = Math.min(tg, level) - 2; y < level; y++) b.s(lx, y - b.oy, lz, DIRT);
        b.s(lx, level - b.oy, lz, FARM);
        if (vx == cvx && vz == cvz) {                              // le point d'eau est enterre : lampe, puis eau sous la terre labouree
            b.s(lx, level - 2 - b.oy, lz, LAMP);
            b.s(lx, level - 1 - b.oy, lz, WELL);
        }
        b.s(lx, level + 1 - b.oy, lz, crop == HobbitonFields.WHEAT ? WHEAT : POTATO);
        for (int y = level + 2; y <= Math.max(tg, level) + 16; y++) b.s(lx, y - b.oy, lz, Build.AIR);
        return true;
    }

    /** recopie le morceau (tx, tz) : la piece a pour origine (b.ox, b.oz) = son coin nord-ouest */
    public static void stamp(Build b, int tx, int tz) {
        load();
        if (W == 0) return;
        int xa = Math.max(b.clip.minX(), b.ox), xb = Math.min(b.clip.maxX(), b.ox + TW - 1);
        int za = Math.max(b.clip.minZ(), b.oz), zb = Math.min(b.clip.maxZ(), b.oz + TD - 1);
        for (int wz = za; wz <= zb; wz++) for (int wx = xa; wx <= xb; wx++) {
            int vx = tx * TW + (wx - b.ox), vz = tz * TD + (wz - b.oz);
            int fx = vx - PADX, fz = vz - PADZ;
            boolean in = fx >= 0 && fx < W && fz >= 0 && fz < D && ground[fz * W + fx] > -900;   // colonne vide dans la source : on laisse le monde
            // champs agrandis retires (demande) : on garde ceux de la source tels quels
            if (!in) continue;
            int ix = fz * W + fx;
            int tg = MapData.height(wx, wz);
            int y0 = OFF + shift(fx, fz, wx, wz);
            int lx = wx - b.ox, lz = wz - b.oz;
            int[] pp = {colOff[ix]};
            int runs = varint(pp);
            int y = Y0, top = Y0 - 1;
            for (int r = 0; r < runs; r++) {
                int len = varint(pp), id = varint(pp);
                // les vides entre deux blocs sont ecrits aussi (sinon le terrain naturel reste sous les arbres et dans les maisons)
                BlockState st = id != 0 ? pal[id] : Build.AIR;
                for (int k = 0; k < len; k++) b.s(lx, y + k + y0 - b.oy, lz, st);
                if (id != 0) top = y + len - 1;
                y += len;
            }
            // ce que le monde avait au-dessus (herbes, arbres) disparait
            for (int wy = top + y0 + 1; wy <= tg + 16; wy++) b.s(lx, wy - b.oy, lz, Build.AIR);
        }
    }
}
