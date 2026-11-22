package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ringsofash.RingsOfAsh;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

/**
 * Le decor de la Comte (la zone rouge de la carte) : arbres et buissons repris de Hobbiteville, plaques de mousse, azalees le long des chemins,
 * et des chemins en terres melangees (terre, terre battue, boue...). Rien n'est pose sur Hobbiteville et ses champs (la recopie les gere).
 */
public class ShireDecorFeature extends Feature<NoneFeatureConfiguration> {
    /** contour de la Comte (x, z du monde), relevé sur la carte */
    static final int[][] POLY = new int[][] {{-1065, 924}, {-1129, 924}, {-1153, 908}, {-1222, 777}, {-1219, 706}, {-1179, 576}, {-1182, 520}, {-1208, 504}, {-1235, 525}, {-1295, 683}, {-1305, 693}, {-1333, 696}, {-1349, 680}, {-1343, 573}, {-1363, 525}, {-1363, 471}, {-1307, 344}, {-1281, 126}, {-1266, 112}, {-1225, 109}, {-1119, 145}, {-1091, 124}, {-1081, 91}, {-1102, 31}, {-1102, -17}, {-1085, -74}, {-1059, -109}, {-911, -241}, {-855, -327}, {-795, -371}, {-835, -395}, {-825, -408}, {-766, -374}, {-681, -361}, {-584, -309}, {-475, -215}, {-464, -158}, {-544, -14}, {-554, 57}, {-569, 98}, {-636, 192}, {-770, 302}, {-866, 480}, {-953, 582}, {-973, 641}, {-970, 756}, {-981, 797}, {-1065, 924}};
    /** chemins violets de la carte (x, z du monde) */
    static final int[][][] PATHS = new int[][][] {
            {{-802, 200}, {-805, 240}, {-824, 286}, {-858, 324}, {-913, 352}, {-943, 373}, {-991, 449}, {-994, 482}},
            {{-995, 483}, {-1050, 505}, {-1108, 516}},
            {{-765, -94}, {-745, -164}, {-746, -274}, {-754, -348}, {-790, -382}, {-828, -399}},
            {{-1112, 516}, {-1141, 508}, {-1181, 486}, {-1211, 486}},
            {{-1138, 903}, {-1132, 646}, {-1110, 517}},
            {{-955, 565}, {-980, 534}, {-994, 483}},
            // chemins rouges de la carte : la suite des chemins de Hobbiteville vers la campagne
            {{-620, 251}, {-627, 263}, {-637, 270}, {-648, 275}, {-660, 280}, {-671, 292}, {-682, 307}, {-694, 324}, {-701, 342}, {-708, 360}, {-716, 378}, {-729, 399}, {-743, 421}, {-747, 426}},
            {{-594, 37}, {-585, 37}, {-578, 29}, {-575, 20}, {-576, 10}, {-579, 1}, {-581, -9}, {-584, -18}, {-586, -27}, {-588, -37}, {-589, -46}, {-589, -55}, {-590, -63}, {-592, -73}, {-596, -82}, {-604, -92}, {-612, -99}, {-621, -105}, {-630, -110}, {-639, -114}, {-648, -119}, {-657, -123}, {-667, -128}, {-675, -139}, {-677, -148}, {-677, -156}},
            {{-603, -19}, {-600, -20}, {-597, -24}, {-594, -27}, {-592, -31}, {-591, -34}, {-589, -38}}};
    /** champs de ble arrondis (violet) : la suite des deux champs que la source coupe tout droit au bord de Hobbiteville */
    static final int[][][] FIELDS = new int[][][] {
            {{-867, -100}, {-860, -97}, {-834, -99}, {-796, -96}, {-792, -98}, {-792, -102}, {-800, -115}, {-807, -123}, {-817, -129}, {-826, -130}, {-834, -121}, {-854, -114}},
            {{-584, 160}, {-573, 153}, {-566, 145}, {-562, 133}, {-560, 122}, {-562, 111}, {-568, 100}, {-574, 94}, {-583, 90}, {-591, 92}, {-592, 98}, {-586, 144}, {-588, 157}}};

    private record Tpl(boolean tree, int[] dx, int[] dy, int[] dz, BlockState[] st, int leaves, int reach) {}

    private static boolean loaded;
    private static final List<Tpl> TREES = new ArrayList<>(), BUSHES = new ArrayList<>();
    private static int stampX0, stampZ0, stampX1, stampZ1, ox, oz;
    private static final BlockState FARM = Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
    private static final BlockState WHEAT = Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
    private static final BlockState WELL = Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
            .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP)
            .setValue(net.minecraft.world.level.block.TrapDoorBlock.WATERLOGGED, true);
    private static final int ZX0 = -1363, ZX1 = -464, ZZ0 = -408, ZZ1 = 924;
    private static byte[] zoneGrid;

    public ShireDecorFeature() { super(NoneFeatureConfiguration.CODEC); }

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

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        int[] pl = MapData.place("comte");
        if (pl != null) {
            ox = HobbitonStamp.originX(pl[0]);
            oz = HobbitonStamp.originZ(pl[1]);
            // le rectangle du fichier source (325 x 358), lui seul est recopie par Hobbiteville
            stampX0 = ox + HobbitonStamp.PADX;
            stampZ0 = oz + HobbitonStamp.PADZ;
            stampX1 = stampX0 + 325;
            stampZ1 = stampZ0 + 358;
        }
        try (InputStream raw = ShireDecorFeature.class.getResourceAsStream("/data/ringsofash/map/shire_trees.bin")) {
            if (raw == null) throw new IllegalStateException("shire_trees.bin introuvable");
            ByteBuffer bb = ByteBuffer.wrap(new InflaterInputStream(raw).readAllBytes());
            int np = bb.getInt();
            BlockState[] pal = new BlockState[np];
            for (int i = 0; i < np; i++) {
                byte[] s = new byte[bb.getShort() & 0xFFFF];
                bb.get(s);
                pal[i] = parse(new String(s, java.nio.charset.StandardCharsets.UTF_8));
            }
            int nt = bb.getInt();
            for (int t = 0; t < nt; t++) {
                boolean tree = bb.get() == 0;
                int n = bb.getInt();
                int[] dx = new int[n], dy = new int[n], dz = new int[n];
                BlockState[] st = new BlockState[n];
                int leaves = 0, reach = 0;
                for (int i = 0; i < n; i++) {
                    dx[i] = bb.get(); dy[i] = bb.get(); dz[i] = bb.get();
                    st[i] = pal[bb.getShort() & 0xFFFF];
                    if (st[i].getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) leaves++;
                    reach = Math.max(reach, Math.max(Math.abs(dx[i]), Math.abs(dz[i])));
                }
                (tree ? TREES : BUSHES).add(new Tpl(tree, dx, dy, dz, st, leaves, reach));
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("arbres de la Comte illisibles", e);
        }
    }

    private static boolean inPoly(int[][] poly, int x, int z) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            int xi = poly[i][0], zi = poly[i][1], xj = poly[j][0], zj = poly[j][1];
            if ((zi > z) != (zj > z) && x < (double) (xj - xi) * (z - zi) / (zj - zi) + xi) in = !in;
        }
        return in;
    }

    private static synchronized void buildGrid() {
        if (zoneGrid != null) return;
        int w = ZX1 - ZX0 + 1, d = ZZ1 - ZZ0 + 1;
        byte[] g = new byte[w * d];
        for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) g[z * w + x] = (byte) (inPoly(POLY, ZX0 + x, ZZ0 + z) ? 1 : 0);
        zoneGrid = g;
    }

    /** vrai dans le contour rouge de la Comte (grille precalculee : appele pour chaque colonne de terrain) */
    public static boolean inZone(int x, int z) {
        if (x < ZX0 || x > ZX1 || z < ZZ0 || z > ZZ1) return false;
        if (zoneGrid == null) buildGrid();
        return zoneGrid[(z - ZZ0) * (ZX1 - ZX0 + 1) + (x - ZX0)] != 0;
    }

    /** biomes naturels que la Comte remplace : toutes les terres de plaine, de bois et de bosquets */
    public static boolean shireLand(int b) { return b == 0 || b == 1 || b == 3 || b == 10 || b == 11; }

    /** numero du champ de ble qui contient la colonne, ou -1 */
    private static int fieldAt(int x, int z) {
        for (int i = 0; i < FIELDS.length; i++) if (inPoly(FIELDS[i], x, z)) return i;
        return -1;
    }

    public static boolean nearField(int x, int z, int m) {
        for (int[][] f : FIELDS) {
            int a = 1 << 30, b = -(1 << 30), c = 1 << 30, d = -(1 << 30);
            for (int[] q : f) { a = Math.min(a, q[0]); b = Math.max(b, q[0]); c = Math.min(c, q[1]); d = Math.max(d, q[1]); }
            if (x >= a - m && x <= b + m && z >= c - m && z <= d + m) return true;
        }
        return false;
    }

    /** une colonne de champ de ble : terre labouree a l'altitude de sa case de 9 x 9 (comme Hobbiteville), eau enterree au centre ; le lit de riviere est comble */
    private static void fieldColumn(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z) {
        int vx = x - ox, vz = z - oz;
        int cvx = Math.floorDiv(vx, 9) * 9 + 4, cvz = Math.floorDiv(vz, 9) * 9 + 4;
        int L = Math.max(MapData.height(ox + cvx, oz + cvz), MapData.SEA + 3);
        int tg = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int y = Math.min(tg, L) - 2; y < L; y++) level.setBlock(p.set(x, y, z), dirt, 2);
        level.setBlock(p.set(x, L, z), FARM, 2);
        level.setBlock(p.set(x, L + 1, z), WHEAT, 2);
        if (vx == cvx && vz == cvz) {
            level.setBlock(p.set(x, L - 2, z), Blocks.GLOWSTONE.defaultBlockState(), 2);
            level.setBlock(p.set(x, L - 1, z), WELL, 2);
        }
        for (int y = L + 2; y <= Math.max(tg, L) + 16; y++) {
            p.set(x, y, z);
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static boolean inStamp(int x, int z) { return x >= stampX0 && x < stampX1 && z >= stampZ0 && z < stampZ1; }

    /** distance au chemin le plus proche (grande si loin) */
    static double pathDist(int x, int z) {
        double best = 1e9;
        for (int[][] pl : PATHS) for (int i = 0; i + 1 < pl.length; i++) {
            double ax = pl[i][0], az = pl[i][1], bx = pl[i + 1][0], bz = pl[i + 1][1];
            double dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
            double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / l2));
            double ex = ax + t * dx - x, ez = az + t * dz - z;
            best = Math.min(best, Math.sqrt(ex * ex + ez * ez));
        }
        return best;
    }

    private static int hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static boolean ground(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL);
    }

    private static boolean soft(BlockState s) { return s.isAir() || s.canBeReplaced(); }

    private static BlockState pathBlock(int x, int z, double pd, double hw) {
        int h = (hash(x, z, 7) >>> 8) % 100;
        if (pd < hw * 0.55) {
            if (h < 38) return Blocks.DIRT_PATH.defaultBlockState();
            if (h < 62) return Blocks.COARSE_DIRT.defaultBlockState();
            if (h < 77) return Blocks.PACKED_MUD.defaultBlockState();
            if (h < 87) return Blocks.ROOTED_DIRT.defaultBlockState();
            if (h < 94) return Blocks.MUD.defaultBlockState();
            return Blocks.MUD_BRICKS.defaultBlockState();
        }
        if (h < 32) return Blocks.COARSE_DIRT.defaultBlockState();
        if (h < 52) return Blocks.DIRT.defaultBlockState();
        if (h < 68) return Blocks.PODZOL.defaultBlockState();
        if (h < 82) return Blocks.DIRT_PATH.defaultBlockState();
        if (h < 92) return Blocks.PACKED_MUD.defaultBlockState();
        return Blocks.MOSS_BLOCK.defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        load();
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        if (x0 > -464 || x0 + 15 < -1363 || z0 > 924 || z0 + 15 < -408) return false;
        if (x0 >= stampX0 && x0 + 15 < stampX1 && z0 >= stampZ0 && z0 + 15 < stampZ1) return false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        // sol : chemins, mousse, azalees
        for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
            int x = x0 + dx, z = z0 + dz;
            if (inStamp(x, z)) continue;
            if (fieldAt(x, z) >= 0) { fieldColumn(level, p, x, z); any = true; continue; }
            if (!inZone(x, z) || !shireLand(MapData.biome(x, z))) continue;
            int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            p.set(x, gy, z);
            BlockState g = level.getBlockState(p);
            if (!ground(g) || !level.getFluidState(p.above()).isEmpty()) continue;
            double pd = pathDist(x, z);
            double hw = 1.3 + 1.5 * MapChunkGenerator.fbm(x * 0.09, z * 0.09, 77);
            BlockPos up = new BlockPos(x, gy + 1, z);
            if (pd < hw) {
                level.setBlock(p, pathBlock(x, z, pd, hw), 2);
                if (soft(level.getBlockState(up))) level.setBlock(up, Blocks.AIR.defaultBlockState(), 2);
                if (soft(level.getBlockState(up.above())) && !level.getBlockState(up.above()).isAir()) level.setBlock(up.above(), Blocks.AIR.defaultBlockState(), 2);
                any = true;
                continue;
            }
            if (!soft(level.getBlockState(up))) continue;
            if (pd < hw + 3.2) {
                int h = (hash(x, z, 11) >>> 8) % 1000;
                if (h < 55) { level.setBlock(up, (h & 1) == 0 ? Blocks.AZALEA.defaultBlockState() : Blocks.FLOWERING_AZALEA.defaultBlockState(), 2); any = true; continue; }
                if (h < 70) {
                    // un petit buisson d'azalees
                    BlockState leaves = ((h & 1) == 0 ? Blocks.AZALEA_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES).defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
                    level.setBlock(up, leaves, 2);
                    if (h % 3 == 0 && soft(level.getBlockState(up.above()))) level.setBlock(up.above(), leaves, 2);
                    any = true;
                    continue;
                }
            }
            float m = MapChunkGenerator.fbm(x * 0.05, z * 0.05, 91);
            if (m > 0.64f && (hash(x, z, 13) >>> 8) % 100 < 86) {
                level.setBlock(p, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                int h = (hash(x, z, 17) >>> 8) % 100;
                if (h < 40) level.setBlock(up, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                else if (h < 44) level.setBlock(up, Blocks.FLOWERING_AZALEA.defaultBlockState(), 2);
                any = true;
            }
        }
        // arbres puis buissons
        if (TREES.isEmpty()) return any;
        List<int[]> done = new ArrayList<>();
        for (int k = 0; k < 12; k++) {
            boolean tree = k < 3 || (k >= 6 && k < 9);
            if (r.nextFloat() > (tree ? 0.55f : 0.65f)) continue;
            int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
            if (inStamp(x, z) || !inZone(x, z) || nearField(x, z, 9)) continue;
            int nb = MapData.biome(x, z);
            if (!shireLand(nb) || (k >= 6 && nb != 3 && nb != 10 && nb != 11)) continue;   // les anciens bois gardent un peu plus d'arbres
            boolean near = false;
            for (int[] d : done) if (Math.abs(d[0] - x) < 6 && Math.abs(d[1] - z) < 6) near = true;
            if (near || pathDist(x, z) < 5.5) continue;
            int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            p.set(x, gy, z);
            if (!ground(level.getBlockState(p)) || !level.getFluidState(p.above()).isEmpty()) continue;
            int sl = 0;
            for (int[] d : new int[][] {{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) sl = Math.max(sl, Math.abs(level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + d[0], z + d[1]) - 1 - gy));
            if (sl > 3) continue;
            List<Tpl> pool = tree ? TREES : BUSHES;
            if (pool.isEmpty()) continue;
            Tpl t = pool.get(r.nextInt(pool.size()));
            if (tree && t.leaves > 500 && r.nextFloat() > 0.35f) t = pool.get(r.nextInt(pool.size()));
            if (t.reach > 15) continue;
            Rotation rot = Rotation.values()[r.nextInt(4)];
            for (int i = 0; i < t.dx.length; i++) {
                int ax = t.dx[i], az = t.dz[i];
                int rx = switch (rot) { case CLOCKWISE_90 -> -az; case CLOCKWISE_180 -> -ax; case COUNTERCLOCKWISE_90 -> az; default -> ax; };
                int rz = switch (rot) { case CLOCKWISE_90 -> ax; case CLOCKWISE_180 -> -az; case COUNTERCLOCKWISE_90 -> -ax; default -> az; };
                p.set(x + rx, gy + 1 + t.dy[i], z + rz);
                BlockState cur = level.getBlockState(p);
                boolean wood = !(t.st[i].getBlock() instanceof net.minecraft.world.level.block.LeavesBlock);
                if (soft(cur) || cur.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock || (wood && cur.getBlock() instanceof net.minecraft.world.level.block.BushBlock))
                    level.setBlock(p, t.st[i].rotate(rot), 2);
            }
            done.add(new int[] {x, z});
            any = true;
        }
        return any;
    }
}
