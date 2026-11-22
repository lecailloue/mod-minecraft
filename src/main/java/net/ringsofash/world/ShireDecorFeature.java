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
            {{-955, 565}, {-980, 534}, {-994, 483}}};

    private record Tpl(boolean tree, int[] dx, int[] dy, int[] dz, BlockState[] st, int leaves, int reach) {}

    private static boolean loaded;
    private static final List<Tpl> TREES = new ArrayList<>(), BUSHES = new ArrayList<>();
    private static int stampX0, stampZ0, stampX1, stampZ1;

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
            stampX0 = HobbitonStamp.originX(pl[0]) - HobbitonStamp.PADX - 10;
            stampZ0 = HobbitonStamp.originZ(pl[1]) - HobbitonStamp.PADZ - 10;
            stampX1 = stampX0 + HobbitonStamp.TILES_X * HobbitonStamp.TW + 20;
            stampZ1 = stampZ0 + HobbitonStamp.TILES_Z * HobbitonStamp.TD + 20;
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

    public static boolean inZone(int x, int z) {
        boolean in = false;
        for (int i = 0, j = POLY.length - 1; i < POLY.length; j = i++) {
            int xi = POLY[i][0], zi = POLY[i][1], xj = POLY[j][0], zj = POLY[j][1];
            if ((zi > z) != (zj > z) && x < (double) (xj - xi) * (z - zi) / (zj - zi) + xi) in = !in;
        }
        return in;
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
            if (inStamp(x, z) || !inZone(x, z) || MapData.biome(x, z) != 0) continue;
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
        for (int k = 0; k < 6; k++) {
            boolean tree = k < 3;
            if (r.nextFloat() > (tree ? 0.55f : 0.65f)) continue;
            int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
            if (inStamp(x, z) || !inZone(x, z) || MapData.biome(x, z) != 0) continue;
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
