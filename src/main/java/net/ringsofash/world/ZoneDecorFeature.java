package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ringsofash.gen.Reg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decor des grandes zones peintes : foret elfique (arbres de Fondcombe), foret de Fangorn (vieux arbres moussus et tombants),
 * marais des morts (arbres morts, flammes bleues, ruines). Comme ShireDecorFeature, il est place sans filtre et ecrit chaque colonne de son chunk.
 */
public class ZoneDecorFeature extends Feature<NoneFeatureConfiguration> {
    public static final int ELVEN = 0, FANGORN = 1, MARSH = 2, CYPRESS = 3;
    private final int mode;

    public ZoneDecorFeature(int mode) {
        super(NoneFeatureConfiguration.CODEC);
        this.mode = mode;
    }

    private static int hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static double rnd(int x, int y, int z) { return ((hash(x * 31 + y, z, y * 7 + 3) >>> 8) & 0xFFFF) / 65536.0; }

    private static double vn(double x, double z, int seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = rnd(x0, seed, z0), b = rnd(x0 + 1, seed, z0), c = rnd(x0, seed, z0 + 1), d = rnd(x0 + 1, seed, z0 + 1);
        return ((a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz) * 2 - 1;
    }

    private static BlockState leaf(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState log(net.minecraft.world.level.block.Block b, Direction.Axis a) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, a);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        int target = switch (mode) {
            case ELVEN -> Zones.B_ELVEN;
            case FANGORN -> Zones.B_FANGORN;
            case MARSH -> Zones.B_MARSH;
            default -> Zones.B_CYPRESS;
        };
        int zn = switch (mode) {
            case ELVEN -> Zones.ELVEN;
            case FANGORN -> Zones.FANGORN;
            case MARSH -> Zones.MARSH;
            default -> Zones.CYPRESS;
        };
        if (!Zones.touches(x0, z0, zn)) return false;
        Gen g = new Gen(ctx.level(), ctx.random(), x0, z0, target);
        return g.run();
    }

    private final class Gen {
        final WorldGenLevel level;
        final RandomSource r;
        final int x0, z0, target;
        final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        final List<int[]> done = new ArrayList<>();
        final Map<Long, BlockState> logs = new HashMap<>(), leaves = new HashMap<>();
        final List<int[]> post = new ArrayList<>();
        final List<int[]> trees = new ArrayList<>();   // x, z, rayon du houppier, teinte dominante

        Gen(WorldGenLevel level, RandomSource r, int x0, int z0, int target) {
            this.level = level; this.r = r; this.x0 = x0; this.z0 = z0; this.target = target;
        }

        boolean mine(int x, int z) { return Zones.biomeFor(MapData.biome(x, z), x, z) == target; }

        int groundY(int x, int z) { return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; }

        boolean inReach(int x, int z) { return x >= x0 - 15 && x <= x0 + 30 && z >= z0 - 15 && z <= z0 + 30; }

        boolean soil(BlockState s) {
            return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.MOSS_BLOCK)
                    || s.is(Blocks.MUD) || s.is(Blocks.ROOTED_DIRT) || s.is(Reg.PEAT.get());
        }

        boolean soft(BlockState s) { return s.isAir() || s.canBeReplaced(); }

        void set(int x, int y, int z, BlockState s) {
            if (!inReach(x, z) || y < level.getMinBuildHeight() + 2 || y >= level.getMaxBuildHeight() - 2) return;
            level.setBlock(p.set(x, y, z), s, 2);
        }

        /** pose seulement dans l'air, l'herbe haute ou les feuilles */
        void setSoft(int x, int y, int z, BlockState s) {
            if (!inReach(x, z)) return;
            BlockState cur = level.getBlockState(p.set(x, y, z));
            if (soft(cur) || cur.getBlock() instanceof LeavesBlock) set(x, y, z, s);
        }

        void flush() {
            for (Map.Entry<Long, BlockState> e : leaves.entrySet()) {
                long k = e.getKey();
                int x = (int) (k >> 38), y = (int) ((k << 26) >> 52), z = (int) ((k << 38) >> 38);
                setSoft(x, y, z, e.getValue());
            }
            for (Map.Entry<Long, BlockState> e : logs.entrySet()) {
                long k = e.getKey();
                int x = (int) (k >> 38), y = (int) ((k << 26) >> 52), z = (int) ((k << 38) >> 38);
                BlockState cur = level.getBlockState(p.set(x, y, z));
                if (soft(cur) || cur.getBlock() instanceof LeavesBlock || cur.is(Blocks.WATER) || cur.is(Blocks.SEAGRASS) || cur.is(Blocks.TALL_SEAGRASS)) set(x, y, z, e.getValue());
            }
            logs.clear();
            leaves.clear();
        }

        long key(int x, int y, int z) { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF); }

        void putLog(int x, int y, int z, BlockState s) { logs.put(key(x, y, z), s); }

        void putLeaf(int x, int y, int z, BlockState s) { if (!logs.containsKey(key(x, y, z))) leaves.put(key(x, y, z), s); }

        BlockState logAxis(net.minecraft.world.level.block.Block b, double dx, double dz) {
            return log(b, Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z);
        }

        boolean far(int x, int z, int d) {
            for (int[] q : done) if (Math.abs(q[0] - x) < Math.max(d, q[2]) && Math.abs(q[1] - z) < Math.max(d, q[2])) return false;
            return true;
        }

        int slope(int x, int z, int gy, int d) {
            int s = 0;
            for (int[] o : new int[][] {{d, 0}, {-d, 0}, {0, d}, {0, -d}}) s = Math.max(s, Math.abs(groundY(x + o[0], z + o[1]) - gy));
            return s;
        }

        /** boule de feuilles aplatie, bord irregulier */
        void blob(double cx, double cy, double cz, double rad, double flat, java.util.function.IntFunction<BlockState> pick) {
            int rr = (int) Math.ceil(rad) + 1;
            for (int dx = -rr; dx <= rr; dx++) for (int dy = -rr; dy <= rr; dy++) for (int dz = -rr; dz <= rr; dz++) {
                int bx = (int) Math.round(cx) + dx, by = (int) Math.round(cy) + dy, bz = (int) Math.round(cz) + dz;
                double e = ((bx - cx) * (bx - cx) + (bz - cz) * (bz - cz)) / (rad * rad) + (by - cy) * (by - cy) / (rad * rad * flat * flat) + 0.32 * vn(bx / 2.0 + by * 0.4, bz / 2.0, 23);
                if (e < 1) putLeaf(bx, by, bz, pick.apply(hash(bx, by, bz)));
            }
        }

        boolean run() {
            boolean any = false;
            switch (mode) {
                case ELVEN -> any = elven();
                case FANGORN -> any = fangorn();
                case MARSH -> any = marsh();
                default -> any = cypress();
            }
            return any;
        }

        // ------------------------------------------------------------------ foret elfique : les arbres de Fondcombe
        BlockState elvenLeaf(int main, int h) {
            double q = (h & 0xFFFF) / 65536.0;
            if (q < 0.14) return leaf(Blocks.OAK_LEAVES);
            if (q < 0.30) return leaf(main == 0 ? Reg.GOLDEN_LEAVES.get() : Reg.AUTUMN_ORANGE_LEAVES.get());
            if (q < 0.36) return leaf(Reg.AUTUMN_RED_LEAVES.get());
            return leaf(main == 0 ? Reg.AUTUMN_ORANGE_LEAVES.get() : main == 1 ? Reg.GOLDEN_LEAVES.get() : main == 2 ? Reg.AUTUMN_RED_LEAVES.get() : Blocks.OAK_LEAVES);
        }

        boolean elven() {
            boolean any = false;
            for (int k = 0; k < 11; k++) {
                boolean big = r.nextFloat() < 0.10f;
                if (r.nextFloat() > (big ? 0.5f : 0.65f)) continue;
                int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
                if (!mine(x, z) || !far(x, z, big ? 14 : 8)) continue;
                int gy = groundY(x, z);
                if (!soil(level.getBlockState(p.set(x, gy, z))) || !level.getFluidState(p.set(x, gy + 1, z)).isEmpty()) continue;
                if (slope(x, z, gy, 3) > (big ? 2 : 4)) continue;
                double q = r.nextDouble();
                int main = q < 0.4 ? 0 : q < 0.75 ? 1 : q < 0.9 ? 2 : 3;
                double rad = 3.4 + r.nextDouble() * 2.2;
                if (big) elvenBig(x, gy + 1, z, main); else elvenTree(x, gy + 1, z, 8 + r.nextInt(8), rad, main);
                flush();
                done.add(new int[] {x, z, big ? 12 : 6});
                trees.add(new int[] {x, z, big ? 11 : (int) Math.round(rad) + 2, main});
                any = true;
            }
            if (r.nextFloat() < 0.32f) any |= boulder();
            if (r.nextFloat() < 0.28f) any |= fallenLog();
            any |= elvenGround();
            return any;
        }

        /** la couleur des feuilles tombees suit celle de l'arbre au dessus */
        net.minecraft.world.level.block.Block litterOf(int main, int h, boolean thick) {
            int q = h % 10;
            if (thick && q < 5) return Reg.DEEP_LEAF_LITTER.get();
            if (q == 9) return Reg.BROWN_LEAF_LITTER.get();
            if (q == 8) return Reg.DEEP_LEAF_LITTER.get();
            if (q == 7) return main == 1 ? Reg.ORANGE_LEAF_LITTER.get() : Reg.GOLDEN_LEAF_LITTER.get();
            return switch (main) {
                case 0 -> Reg.ORANGE_LEAF_LITTER.get();
                case 1 -> Reg.GOLDEN_LEAF_LITTER.get();
                case 2 -> Reg.RED_LEAF_LITTER.get();
                default -> Reg.BROWN_LEAF_LITTER.get();
            };
        }

        BlockState elvenFlower(int h) {
            return switch (h % 9) {                       // uniquement les fleurs du mod
                case 0 -> Reg.FLAME_ROSE.get().defaultBlockState();
                case 1 -> Reg.GOLD_BELL.get().defaultBlockState();
                case 2 -> Reg.EMBER_BLOOM.get().defaultBlockState();
                case 3 -> Reg.SCARLET_SPIKE.get().defaultBlockState();
                case 4 -> Reg.PALE_DAISY.get().defaultBlockState();
                case 5 -> Reg.RED_SPRIG.get().defaultBlockState();
                case 6 -> Reg.GOLDEN_LILY.get().defaultBlockState();
                case 7 -> Reg.SUNSTAR.get().defaultBlockState();
                default -> Reg.MOONBELL.get().defaultBlockState();
            };
        }

        /** le sol de Fondcombe : feuilles tombees sous les arbres, fleurs, fougeres et buissons dans les clairieres */
        boolean elvenGround() {
            boolean any = false;
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                int x = x0 + dx, z = z0 + dz;
                if (!mine(x, z)) continue;
                int gy = groundY(x, z);
                BlockState g = level.getBlockState(p.set(x, gy, z));
                if (!soil(g) || g.is(Blocks.MUD)) continue;
                if (!level.getFluidState(p.set(x, gy + 1, z)).isEmpty()) continue;
                BlockState up = level.getBlockState(p.set(x, gy + 1, z));
                if (!up.isAir() && !up.is(Blocks.SHORT_GRASS) && !up.is(Blocks.FERN)) continue;
                double near = 1e9;
                int main = -1;
                for (int[] t : trees) {
                    double d = Math.hypot(t[0] - x, t[1] - z) - t[2];
                    if (d < near) { near = d; main = t[3]; }
                }
                int h = (hash(x, z, 72) >>> 8) & 0xFFFF;
                double n = (vn(x * 0.13, z * 0.13, 71) + 1) * 0.5;
                double dens = main < 0 || near > 9 ? 0.10 : near <= 0 ? 0.78 : near < 5 ? 0.45 : 0.20;
                if (n * 0.55 + (h % 100) / 220.0 < dens) {
                    set(x, gy + 1, z, litterOf(main, h >> 4, near <= -1).defaultBlockState());
                    any = true;
                } else if (near > 1 && h % 97 < (vn(x * 0.06, z * 0.06, 75) > 0.3 ? 17 : 4)) {   // clairieres fleuries
                    set(x, gy + 1, z, elvenFlower(h >> 7));
                    any = true;
                } else if (h % 71 < 9) {                           // sous bois : herbes et buissons d'automne
                    BlockState u = switch ((h >> 5) % 8) {
                        case 0 -> Reg.GOLD_TUFT.get().defaultBlockState();
                        case 1 -> Reg.RED_TUFT.get().defaultBlockState();
                        case 2 -> Reg.ORANGE_TUFT.get().defaultBlockState();
                        case 3 -> Reg.GOLD_BUSH.get().defaultBlockState();
                        case 4 -> Reg.RED_BUSH.get().defaultBlockState();
                        case 5 -> Blocks.FERN.defaultBlockState();
                        default -> Blocks.SHORT_GRASS.defaultBlockState();
                    };
                    set(x, gy + 1, z, u);
                    any = true;
                } else if (h % 211 < 3 && g.is(Blocks.MOSS_BLOCK)) {
                    set(x, gy + 1, z, Blocks.MOSS_CARPET.defaultBlockState());
                    any = true;
                } else if (h % 149 < 3 && (g.is(Blocks.PODZOL) || g.is(Blocks.MOSS_BLOCK)) && near < 6) {
                    set(x, gy + 1, z, Reg.AMBER_MUSHROOM.get().defaultBlockState());
                    any = true;
                } else if (h % 127 < 4) {
                    set(x, gy + 1, z, (h & 8) == 0 ? Reg.TWIGS.get().defaultBlockState() : Reg.PEBBLES.get().defaultBlockState());
                    any = true;
                }
            }
            return any;
        }

        /** un rocher moussu pose dans le sous bois */
        boolean boulder() {
            int x = x0 + 4 + r.nextInt(8), z = z0 + 4 + r.nextInt(8);
            if (!mine(x, z)) return false;
            int gy = groundY(x, z);
            if (!soil(level.getBlockState(p.set(x, gy, z)))) return false;
            int rr = 1 + r.nextInt(2);
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) for (int dy = -1; dy <= rr; dy++) {
                if (dx * dx + dz * dz + dy * dy > rr * rr + 1) continue;
                int bx = x + dx, bz = z + dz, by = gy + dy;
                int q = (hash(bx, bz, 73) >>> 8) % 10;
                set(bx, by, bz, q < 4 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : q < 7 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
            return true;
        }

        /** un tronc couche, mange par la mousse */
        boolean fallenLog() {
            int x = x0 + 3 + r.nextInt(9), z = z0 + 3 + r.nextInt(9);
            if (!mine(x, z)) return false;
            boolean alongX = r.nextBoolean();
            int len = 3 + r.nextInt(4);
            BlockState wl = log(Reg.GOLDEN_LOG.get(), alongX ? Direction.Axis.X : Direction.Axis.Z);
            for (int i = 0; i < len; i++) {
                int bx = alongX ? x + i : x, bz = alongX ? z : z + i;
                int gy = groundY(bx, bz);
                if (!soil(level.getBlockState(p.set(bx, gy, bz)))) return i > 1;
                if (!soft(level.getBlockState(p.set(bx, gy + 1, bz)))) continue;
                set(bx, gy + 1, bz, wl);
                if ((hash(bx, bz, 74) >>> 8) % 3 == 0 && soft(level.getBlockState(p.set(bx, gy + 2, bz))))
                    set(bx, gy + 2, bz, Blocks.MOSS_CARPET.defaultBlockState());
            }
            return true;
        }

        void elvenTree(int x, int y0, int z, int h, double rad, int main) {
            double lean = r.nextDouble() * 0.4 - 0.2;
            int tx = x, tz = z;
            BlockState gl = log(Reg.GOLDEN_LOG.get(), Direction.Axis.Y);
            for (int y = 0; y < h; y++) {
                if (y > 3 && r.nextDouble() < 0.12) tx += lean > 0 ? 1 : -1;
                putLog(tx, y0 + y, tz, gl);
                if (y < 2) { putLog(tx + 1, y0 + y, tz, gl); putLog(tx, y0 + y, tz + 1, gl); }
            }
            for (int i = 0; i < 4; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double bx = tx, bz = tz, by = y0 + h * (0.5 + 0.25 * r.nextDouble());
                int len = 3 + r.nextInt(4);
                for (int k = 0; k < len; k++) {
                    bx += Math.cos(a); bz += Math.sin(a); by += 0.6;
                    putLog((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), logAxis(Reg.GOLDEN_LOG.get(), Math.cos(a), Math.sin(a)));
                }
                blob(bx, by + 1, bz, rad * 0.62, 0.8, hh -> elvenLeaf(main, hh));
            }
            blob(tx, y0 + h, tz, rad, 0.75, hh -> elvenLeaf(main, hh));
        }

        /** le grand arbre de Fondcombe : houppier plat et large sur des branches maitresses */
        void elvenBig(int x, int y0, int z, int main) {
            int h = 22 + r.nextInt(6);
            BlockState gl = log(Reg.GOLDEN_LOG.get(), Direction.Axis.Y);
            for (int y = -2; y < h; y++) {
                double t = y / (double) h;
                double rad = y < 4 ? 3.0 : t < 0.5 ? 2.3 : t < 0.75 ? 1.7 : 1.2;
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) if (Math.hypot(dx, dz) <= rad) putLog(x + dx, y0 + y, z + dz, gl);
            }
            for (int i = 0; i < 6; i++) {            // racines apparentes
                double a = i * Math.PI * 2 / 6 + r.nextDouble() * 0.4;
                double px = x, pz = z; int py = y0 + 2;
                for (int k = 0; k < 5; k++) { px += Math.cos(a); pz += Math.sin(a); if (k > 1 && r.nextBoolean()) py--; putLog((int) Math.round(px + Math.cos(a) * 2.5), Math.max(y0 - 1, py), (int) Math.round(pz + Math.sin(a) * 2.5), gl); }
            }
            List<double[]> tips = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                double a = i * Math.PI * 2 / 7 + r.nextDouble() * 0.6;
                double bx = x, bz = z, by = y0 + h * (0.4 + 0.4 * r.nextDouble());
                int len = 5 + r.nextInt(3);
                for (int k = 0; k < len; k++) {
                    bx += Math.cos(a); bz += Math.sin(a); by += 0.5; a += (r.nextDouble() - 0.5) * 0.3;
                    putLog((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), logAxis(Reg.GOLDEN_LOG.get(), Math.cos(a), Math.sin(a)));
                    if (k > 2) putLog((int) Math.round(bx), (int) Math.round(by) - 1, (int) Math.round(bz), gl);
                }
                tips.add(new double[] {bx, by, bz, 5 + r.nextDouble() * 1.5});
            }
            tips.add(new double[] {x, y0 + h + 3, z, 6.5});
            for (double[] t : tips) {
                int m = ((int) (t[0] * 3 + t[2]) & 3) < 2 ? main : (main + 1) & 3;
                blob(t[0], t[1], t[2], t[3], 0.62, hh -> elvenLeaf(m, hh));
            }
        }

        // ------------------------------------------------------------------ foret de cypres : conif res en fleche, sequoias, sous bois vert
        boolean cypress() {
            boolean any = false;
            for (int k = 0; k < 11; k++) {
                boolean giant = r.nextFloat() < 0.08f;
                if (r.nextFloat() > (giant ? 0.45f : 0.6f)) continue;
                int x = x0 + 2 + r.nextInt(12), z = z0 + 2 + r.nextInt(12);
                if (!mine(x, z) || !far(x, z, giant ? 13 : 7)) continue;
                int gy = groundY(x, z);
                if (!soil(level.getBlockState(p.set(x, gy, z))) || !level.getFluidState(p.set(x, gy + 1, z)).isEmpty()) continue;
                if (slope(x, z, gy, 3) > (giant ? 3 : 5)) continue;
                if (giant) redwoodTree(x, gy + 1, z);
                else cypressTree(x, gy + 1, z, 13 + r.nextInt(15), 2.3 + r.nextDouble() * 1.4);
                flush();
                done.add(new int[] {x, z, giant ? 11 : 4});
                trees.add(new int[] {x, z, giant ? 9 : 4, 0});
                any = true;
            }
            for (int k = 0; k < 5; k++) {                                     // jeunes pousses
                if (r.nextFloat() > 0.5f) continue;
                int x = x0 + 1 + r.nextInt(14), z = z0 + 1 + r.nextInt(14);
                if (!mine(x, z) || !far(x, z, 3)) continue;
                int gy = groundY(x, z);
                if (!soil(level.getBlockState(p.set(x, gy, z))) || !soft(level.getBlockState(p.set(x, gy + 1, z)))) continue;
                cypressTree(x, gy + 1, z, 3 + r.nextInt(4), 1.1 + r.nextDouble() * 0.8);
                flush();
                any = true;
            }
            if (r.nextFloat() < 0.55f) any |= mossyBoulder();
            if (r.nextFloat() < 0.30f) any |= cypressFallenLog();
            any |= cypressGround();
            return any;
        }

        /** cypres : un fut droit et un houppier en fleche, etage */
        void cypressTree(int x, int y0, int z, int h, double rad) {
            BlockState lg = log(Reg.CYPRESS_LOG.get(), Direction.Axis.Y);
            BlockState lf = leaf(Reg.CYPRESS_LEAVES.get());
            for (int y = -1; y < h; y++) putLog(x, y0 + y, z, lg);
            if (h > 12) for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})   // empattement
                if (r.nextFloat() < 0.6f) putLog(x + o[0], y0 - 1, z + o[1], lg);
            int base = Math.max(1, (int) (h * (h > 10 ? 0.44 : 0.15)));
            for (int y = base; y <= h + 1; y++) {
                double t = (y - base) / (double) Math.max(1, h + 1 - base);
                double rr = rad * Math.pow(1 - t, 0.8) + 0.45;
                if ((y - base) % 3 == 0) rr += 0.55;                           // etages de branches
                int ri = (int) Math.ceil(rr);
                for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= rr + 0.45 * vn(x * 0.5 + dx, z * 0.5 + dz + y * 0.3, 81)) putLeaf(x + dx, y0 + y, z + dz, lf);
                }
            }
            putLeaf(x, y0 + h + 2, z, lf);
        }

        /** sequoia : un tronc roux de deux blocs de cote, nu sur la moitie basse, couronne sombre tout en haut */
        void redwoodTree(int x, int y0, int z) {
            int h = 28 + r.nextInt(14);
            BlockState lg = log(Reg.REDWOOD_LOG.get(), Direction.Axis.Y);
            BlockState lf = leaf(Reg.REDWOOD_LEAVES.get());
            for (int y = -2; y < h; y++) {
                double t = y / (double) h;
                int w = y < 3 ? 2 : 1;                                         // base evasee
                for (int dx = -w + 1; dx <= w; dx++) for (int dz = -w + 1; dz <= w; dz++) {
                    if (w == 2 && Math.abs(dx) + Math.abs(dz) > 2 && r.nextFloat() < 0.5f) continue;
                    putLog(x + dx, y0 + y, z + dz, lg);
                }
                if (t > 0.55 && y % 4 == 0) {                                  // branches de la couronne
                    double a = r.nextDouble() * Math.PI * 2;
                    double bx = x, bz = z, by = y0 + y;
                    int len = 2 + r.nextInt(3);
                    for (int k = 0; k < len; k++) {
                        bx += Math.cos(a); bz += Math.sin(a); by += 0.4;
                        putLog((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), logAxis(Reg.REDWOOD_LOG.get(), Math.cos(a), Math.sin(a)));
                    }
                    blob(bx, by, bz, 2.2 + r.nextDouble(), 0.7, hh -> lf);
                }
            }
            int base = (int) (h * 0.6);
            for (int y = base; y <= h + 3; y++) {
                double t = (y - base) / (double) (h + 3 - base);
                double rr = (3.6 + r.nextDouble() * 0.6) * Math.pow(1 - t, 0.7) + 0.6;
                int ri = (int) Math.ceil(rr);
                for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= rr + 0.5 * vn(x * 0.4 + dx, z * 0.4 + dz + y * 0.25, 82)) putLeaf(x + dx, y0 + y, z + dz, lf);
                }
            }
        }

        /** rocher couvert de mousse, comme au bord du ruisseau */
        boolean mossyBoulder() {
            int x = x0 + 4 + r.nextInt(8), z = z0 + 4 + r.nextInt(8);
            if (!mine(x, z)) return false;
            int gy = groundY(x, z);
            if (!soil(level.getBlockState(p.set(x, gy, z))) && !level.getBlockState(p.set(x, gy, z)).is(Blocks.STONE)) return false;
            int rr = 1 + r.nextInt(3);
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) for (int dy = -2; dy <= rr; dy++) {
                if (dx * dx + dz * dz + Math.max(0, dy) * Math.max(0, dy) > rr * rr + 1) continue;
                int bx = x + dx, bz = z + dz, by = gy + dy;
                int q = (hash(bx, by, bz) >>> 8) % 10;
                set(bx, by, bz, q < 5 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : q < 8 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) {     // mousse sur le dessus
                int bx = x + dx, bz = z + dz;
                int top = groundY(bx, bz);
                if ((hash(bx, bz, 83) >>> 8) % 10 < 5 && soft(level.getBlockState(p.set(bx, top + 1, bz))))
                    set(bx, top + 1, bz, Blocks.MOSS_CARPET.defaultBlockState());
            }
            return true;
        }

        boolean cypressFallenLog() {
            int x = x0 + 3 + r.nextInt(9), z = z0 + 3 + r.nextInt(9);
            if (!mine(x, z)) return false;
            boolean alongX = r.nextBoolean();
            int len = 3 + r.nextInt(5);
            BlockState wl = log(Reg.CYPRESS_LOG.get(), alongX ? Direction.Axis.X : Direction.Axis.Z);
            for (int i = 0; i < len; i++) {
                int bx = alongX ? x + i : x, bz = alongX ? z : z + i;
                int gy = groundY(bx, bz);
                if (!soil(level.getBlockState(p.set(bx, gy, bz)))) return i > 1;
                if (!soft(level.getBlockState(p.set(bx, gy + 1, bz)))) continue;
                set(bx, gy + 1, bz, wl);
                if ((hash(bx, bz, 84) >>> 8) % 3 == 0 && soft(level.getBlockState(p.set(bx, gy + 2, bz))))
                    set(bx, gy + 2, bz, Blocks.MOSS_CARPET.defaultBlockState());
            }
            return true;
        }

        BlockState cypressFlower(int h) {
            return switch (h % 7) {
                case 0, 1 -> Reg.FOREST_POPPY.get().defaultBlockState();
                case 2, 3 -> Reg.FOREST_GOLD.get().defaultBlockState();
                case 4 -> Reg.SNOW_BELL.get().defaultBlockState();
                case 5 -> Reg.CRIMSON_SPIRE.get().defaultBlockState();
                default -> Reg.MOONBELL.get().defaultBlockState();
            };
        }

        /** sous bois : herbes hautes, fougeres, mousse et fleurs rouges et jaunes */
        boolean cypressGround() {
            boolean any = false;
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                int x = x0 + dx, z = z0 + dz;
                if (!mine(x, z)) continue;
                int gy = groundY(x, z);
                BlockState g = level.getBlockState(p.set(x, gy, z));
                if (!soil(g) || g.is(Blocks.MUD)) continue;
                if (!level.getFluidState(p.set(x, gy + 1, z)).isEmpty()) continue;
                BlockState up = level.getBlockState(p.set(x, gy + 1, z));
                if (!up.isAir() && !up.is(Blocks.SHORT_GRASS) && !up.is(Blocks.FERN)) continue;
                int h = (hash(x, z, 85) >>> 8) & 0xFFFF;
                double n = (vn(x * 0.09, z * 0.09, 86) + 1) * 0.5;             // clairieres plus fournies
                double near = 1e9;
                for (int[] t : trees) near = Math.min(near, Math.hypot(t[0] - x, t[1] - z) - t[2]);
                if (near < 3 && h % 100 < 58) {                                // tapis d'aiguilles sous les arbres
                    set(x, gy + 1, z, Reg.NEEDLE_LITTER.get().defaultBlockState());
                    any = true;
                } else if (h % 173 < 3 && near < 7) {
                    set(x, gy + 1, z, (h & 16) == 0 ? Reg.AMBER_MUSHROOM.get().defaultBlockState() : Reg.PALE_MUSHROOM.get().defaultBlockState());
                    any = true;
                } else if (h % 181 < 3 && near > 3) {
                    set(x, gy + 1, z, Reg.BLUEBERRY_BUSH.get().defaultBlockState());
                    any = true;
                } else if (h % 131 < 4) {
                    set(x, gy + 1, z, (h & 8) == 0 ? Reg.TWIGS.get().defaultBlockState() : Reg.PEBBLES.get().defaultBlockState());
                    any = true;
                } else if (h % 100 < (n > 0.55 ? 46 : 22)) {
                    BlockState u = switch ((h >> 6) % 7) {
                        case 0, 1 -> Reg.GREEN_TUFT.get().defaultBlockState();
                        case 2 -> Reg.PALE_GREEN_TUFT.get().defaultBlockState();
                        case 3 -> Reg.FERN_BUSH.get().defaultBlockState();
                        case 4 -> Blocks.FERN.defaultBlockState();
                        default -> Blocks.SHORT_GRASS.defaultBlockState();
                    };
                    set(x, gy + 1, z, u);
                    any = true;
                } else if (h % 101 < (vn(x * 0.05, z * 0.05, 87) > 0.25 ? 14 : 3)) {
                    set(x, gy + 1, z, cypressFlower(h >> 7));
                    any = true;
                } else if (h % 53 < 4 && g.is(Blocks.MOSS_BLOCK)) {
                    set(x, gy + 1, z, Blocks.MOSS_CARPET.defaultBlockState());
                    any = true;
                } else if (h % 211 < 2 && soft(level.getBlockState(p.set(x, gy + 2, z)))) {
                    BlockState tall = Blocks.LARGE_FERN.defaultBlockState();
                    set(x, gy + 1, z, tall);
                    set(x, gy + 2, z, tall.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
                    any = true;
                }
            }
            return any;
        }

        // ------------------------------------------------------------------ Fangorn : vieux arbres noueux
        boolean fangorn() {
            boolean any = false;
            for (int k = 0; k < 8; k++) {
                if (r.nextFloat() > 0.7f) continue;
                int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
                if (!mine(x, z) || !far(x, z, 9)) continue;
                int gy = groundY(x, z);
                if (!soil(level.getBlockState(p.set(x, gy, z))) || !level.getFluidState(p.set(x, gy + 1, z)).isEmpty()) continue;
                if (slope(x, z, gy, 3) > 3) continue;
                fangornTree(x, gy + 1, z);
                flush();
                trunkVines();
                done.add(new int[] {x, z, 8});
                any = true;
            }
            // sous-bois : tapis de mousse, fougeres, rares ruines de pierre mangees par le lierre
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                int x = x0 + dx, z = z0 + dz;
                if (!mine(x, z)) continue;
                int gy = groundY(x, z);
                p.set(x, gy, z);
                BlockState g = level.getBlockState(p);
                if (!soil(g) || !level.getFluidState(p.above()).isEmpty()) continue;
                BlockState up = level.getBlockState(p.above());
                if (!soft(up)) continue;
                int h = (hash(x, z, 5) >>> 8) % 100;
                if (g.is(Blocks.MOSS_BLOCK) && h < 55) { set(x, gy + 1, z, Blocks.MOSS_CARPET.defaultBlockState()); any = true; }
                else if (h < 3) { set(x, gy + 1, z, Blocks.LARGE_FERN.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)); set(x, gy + 2, z, Blocks.LARGE_FERN.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)); any = true; }
                else if (h < 5) { set(x, gy + 1, z, Blocks.AZALEA.defaultBlockState()); any = true; }
                else if (h < 8) { set(x, gy + 1, z, Reg.PALE_MUSHROOM.get().defaultBlockState()); any = true; }
                else if (h < 12) { set(x, gy + 1, z, (h & 1) == 0 ? Reg.TWIGS.get().defaultBlockState() : Reg.PEBBLES.get().defaultBlockState()); any = true; }
            }
            if (r.nextFloat() < 0.07f) any |= ruin(false);
            return any;
        }

        void disc(double cx, double cz, double rad, int y, BlockState s, boolean knots) {
            int rr = (int) Math.ceil(rad) + 1;
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) {
                int bx = (int) Math.floor(cx) + dx, bz = (int) Math.floor(cz) + dz;
                double d = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (d <= rad + (knots ? vn(bx * 0.9 + y * 0.3, bz * 0.9, 31) * 0.6 : 0)) {
                    BlockState st = s;
                    if (knots && d > rad - 0.9 && rnd(bx, y, bz) < 0.07) st = Blocks.MOSS_BLOCK.defaultBlockState();
                    putLog(bx, y, bz, st);
                }
            }
        }

        void fangornTree(int x, int y0, int z) {
            BlockState al = log(Reg.ANCIENT_LOG.get(), Direction.Axis.Y);
            int h = 16 + r.nextInt(13);
            double base = 2.0 + r.nextDouble() * 1.3;
            double cx = x + 0.5, cz = z + 0.5;
            List<int[]> trunk = new ArrayList<>();
            for (int y = -1; y < h; y++) {
                double t = y / (double) h;
                double rad = base * (1 - 0.5 * t) + (y < 4 ? (4 - y) * 0.3 : 0);
                if (y > 3 && y % 4 == 0) { cx += (r.nextDouble() - 0.5) * 1.3; cz += (r.nextDouble() - 0.5) * 1.3; }
                disc(cx, cz, rad, y0 + y, al, true);
                trunk.add(new int[] {(int) Math.floor(cx), y0 + y, (int) Math.floor(cz), (int) Math.ceil(rad)});
            }
            int tx = (int) Math.floor(cx), tz = (int) Math.floor(cz);
            for (int i = 0; i < 6; i++) {                     // racines en contreforts
                double a = i * Math.PI * 2 / 6 + r.nextDouble() * 0.7;
                int len = 4 + r.nextInt(3);
                for (int k = 1; k <= len; k++) {
                    int bx = (int) Math.round(x + 0.5 + Math.cos(a) * (base + k * 0.9)), bz = (int) Math.round(z + 0.5 + Math.sin(a) * (base + k * 0.9));
                    putLog(bx, y0 + Math.max(0, 2 - k / 2), bz, logAxis(Reg.ANCIENT_LOG.get(), Math.cos(a), Math.sin(a)));
                    if (k < 3) putLog(bx, y0 + 1 + Math.max(0, 2 - k), bz, al);
                }
            }
            int nb = 4 + r.nextInt(3);
            for (int i = 0; i < nb; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double bx = tx, bz = tz, by = y0 + h * (0.55 + 0.3 * r.nextDouble());
                int len = 6 + r.nextInt(5);
                for (int k = 0; k < len; k++) {
                    bx += Math.cos(a); bz += Math.sin(a); by += 0.55; a += (r.nextDouble() - 0.5) * 0.5;
                    putLog((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), logAxis(Reg.ANCIENT_LOG.get(), Math.cos(a), Math.sin(a)));
                    if (k < 3) putLog((int) Math.round(bx), (int) Math.round(by) - 1, (int) Math.round(bz), al);
                }
                blob(bx, by + 1, bz, 3.6 + r.nextDouble() * 1.6, 0.7, hh -> fangornLeaf(hh));
            }
            blob(tx, y0 + h, tz, 4.8 + r.nextDouble() * 1.6, 0.8, hh -> fangornLeaf(hh));
            // lianes sur le tronc, rideaux de feuilles sous le feuillage
            for (int[] t : trunk) {
                if (t[1] < y0 + 4 || rnd(t[0], t[1], t[2]) > 0.2) continue;
                post.add(t);
            }
            List<Long> curtains = new ArrayList<>();
            for (Long kk : leaves.keySet()) if ((hash((int) (kk >> 38), (int) (kk & 0x3FFFFFF), 3) >>> 8) % 100 < 9) curtains.add(kk);
            for (Long kk : curtains) {
                int bx = (int) (kk >> 38), by = (int) ((kk << 26) >> 52), bz = (int) ((kk << 38) >> 38);
                int len = 1 + (hash(bx, bz, by) >>> 8) % 4;
                for (int j = 1; j <= len; j++) if (!leaves.containsKey(key(bx, by - j, bz)) && !logs.containsKey(key(bx, by - j, bz))) putLeaf(bx, by - j, bz, leaf(Reg.ANCIENT_LEAVES.get()));
            }
        }

        /** lianes collees au tronc, tant que la bûche les retient */
        void trunkVines() {
            for (int[] t : post) {
                Direction dir = Direction.from2DDataValue(hash(t[0], t[2], t[1]) & 3);
                int px = t[0], pz = t[2], steps = 0;
                while (steps++ < 8 && level.getBlockState(p.set(px, t[1], pz)).is(Reg.ANCIENT_LOG.get())) { px += dir.getStepX(); pz += dir.getStepZ(); }
                if (steps > 8) continue;
                int len = 2 + (hash(px, pz, 9) >>> 8) % 7;
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(dir.getOpposite()), true);
                for (int k = 0; k < len; k++) {
                    BlockState cur = level.getBlockState(p.set(px, t[1] - k, pz));
                    if (!soft(cur) || !level.getBlockState(p.set(px - dir.getStepX(), t[1] - k, pz - dir.getStepZ())).is(Reg.ANCIENT_LOG.get())) break;
                    set(px, t[1] - k, pz, vine);
                }
            }
            post.clear();
        }

        BlockState fangornLeaf(int h) {
            double q = (h & 0xFFFF) / 65536.0;
            if (q < 0.10) return leaf(Blocks.DARK_OAK_LEAVES);
            if (q < 0.16) return leaf(Blocks.AZALEA_LEAVES);
            return leaf(Reg.ANCIENT_LEAVES.get());
        }

        /** un pan de mur ecroule et quelques piliers brises, mangés par la mousse */
        boolean ruin(boolean marsh) {
            int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
            if (!mine(x, z)) return false;
            int gy = groundY(x, z);
            BlockState[] pal = {Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()};
            boolean alongX = r.nextBoolean();
            int len = 5 + r.nextInt(5);
            for (int i = 0; i < len; i++) {
                int bx = alongX ? x + i - len / 2 : x, bz = alongX ? z : z + i - len / 2;
                int g = groundY(bx, bz);
                int top = 1 + (int) (vn(i * 0.9, x * 0.1, 5) * 1.5 + 1.6) + (r.nextInt(5) == 0 ? 2 : 0);
                for (int y = -1; y <= top; y++) set(bx, g + 1 + y, bz, pal[r.nextInt(r.nextInt(4) == 0 ? 4 : 2)]);
            }
            for (int e = 0; e < 2; e++) {                   // piliers
                int bx = alongX ? x - len / 2 + (e == 0 ? 0 : len - 1) : x, bz = alongX ? z : z - len / 2 + (e == 0 ? 0 : len - 1);
                int g = groundY(bx, bz), ph = 3 + r.nextInt(5);
                for (int y = -1; y <= ph; y++) set(bx, g + 1 + y, bz, y == ph ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : pal[r.nextInt(3)]);
            }
            return true;
        }

        // ------------------------------------------------------------------ marais des morts
        boolean marsh() {
            boolean any = false;
            // roseaux sur les rives, plaques de mousse brune
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                int x = x0 + dx, z = z0 + dz;
                if (!mine(x, z)) continue;
                int gy = groundY(x, z);
                p.set(x, gy, z);
                BlockState g = level.getBlockState(p);
                if (!soil(g) || !level.getFluidState(p.above()).isEmpty() || !soft(level.getBlockState(p.above()))) continue;
                boolean shore = level.getFluidState(p.set(x + 1, gy + 1, z)).is(net.minecraft.tags.FluidTags.WATER) || level.getFluidState(p.set(x - 1, gy + 1, z)).is(net.minecraft.tags.FluidTags.WATER)
                        || level.getFluidState(p.set(x, gy + 1, z + 1)).is(net.minecraft.tags.FluidTags.WATER) || level.getFluidState(p.set(x, gy + 1, z - 1)).is(net.minecraft.tags.FluidTags.WATER);
                int h = (hash(x, z, 21) >>> 8) % 100;
                if (shore && h < 58) {
                    int q = (hash(x, z, 22) >>> 8) % 10;
                    if (q < 4) {
                        int n = 1 + (hash(x, z, 24) >>> 8) % 3;
                        for (int k = 1; k <= n; k++) set(x, gy + k, z, Blocks.SUGAR_CANE.defaultBlockState());
                    } else {
                        set(x, gy + 1, z, q < 7 ? Reg.REED_TUFT.get().defaultBlockState() : Reg.CATTAIL.get().defaultBlockState());
                    }
                    any = true;
                } else if (h < 10) { set(x, gy + 1, z, Reg.DEAD_TUFT.get().defaultBlockState()); any = true; }
                else if (h < 13) { set(x, gy + 1, z, Reg.BOG_MOSS.get().defaultBlockState()); any = true; }
                else if (h < 15) { set(x, gy + 1, z, Reg.SWAMP_MUSHROOM.get().defaultBlockState()); any = true; }
                else if (h < 17) { set(x, gy + 1, z, Blocks.MOSS_CARPET.defaultBlockState()); any = true; }
            }
            // arbres morts, dans l'eau comme sur les buttes
            for (int k = 0; k < 4; k++) {
                if (r.nextFloat() > 0.4f) continue;
                int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
                if (!mine(x, z) || !far(x, z, 11)) continue;
                int gy = groundY(x, z);
                if (!soil(level.getBlockState(p.set(x, gy, z)))) continue;
                int wy = 0;
                while (wy < 4 && level.getBlockState(p.set(x, gy + 1 + wy, z)).is(Blocks.WATER)) wy++;
                if (wy >= 4) continue;
                deadTree(x, gy + 1, z);
                flush();
                done.add(new int[] {x, z, 9});
                any = true;
            }
            // flammes bleues des Marais : une pierre moussue, du sable des ames, la flamme
            if (r.nextFloat() < 0.2f) {
                int n = 1 + r.nextInt(2);
                for (int i = 0; i < n; i++) {
                    int x = x0 + 2 + r.nextInt(12), z = z0 + 2 + r.nextInt(12);
                    if (!mine(x, z)) continue;
                    int gy = groundY(x, z);
                    int top = Math.max(gy, MapData.SEA);
                    if (!soft(level.getBlockState(p.set(x, top + 2, z))) || !level.getFluidState(p.set(x, top + 2, z)).isEmpty()) continue;
                    for (int y = gy + 1; y <= top; y++) set(x, y, z, r.nextInt(3) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                    set(x, top + 1, z, Blocks.SOUL_SOIL.defaultBlockState());
                    set(x, top + 2, z, Blocks.SOUL_FIRE.defaultBlockState());
                    any = true;
                }
            }
            if (r.nextFloat() < 0.05f) any |= ruin(true);
            if (r.nextFloat() < 0.04f) any |= jetty();
            return any;
        }

        void deadTree(int x, int y0, int z) {
            BlockState wl = log(Reg.WEATHERED_LOG.get(), Direction.Axis.Y);
            int h = 6 + r.nextInt(7);
            double cx = x, cz = z;
            int tx = x, tz = z;
            for (int y = 0; y < h; y++) {
                if (y > 2 && r.nextDouble() < 0.18) { tx += r.nextInt(3) - 1; }
                if (y > 2 && r.nextDouble() < 0.18) { tz += r.nextInt(3) - 1; }
                putLog(tx, y0 + y, tz, wl);
                if (y < 3) { if (r.nextFloat() < 0.7f) putLog(tx + 1, y0 + y, tz, wl); if (r.nextFloat() < 0.7f) putLog(tx, y0 + y, tz + 1, wl); }
            }
            int nb = 2 + r.nextInt(2);
            for (int i = 0; i <= nb; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                int sy = i == nb ? h - 1 : (int) (h * (0.45 + 0.45 * r.nextDouble()));
                branch(tx, y0 + sy, tz, a, 3 + r.nextInt(4), true);
            }
        }

        void branch(int sx, int sy, int sz, double a, int len, boolean fork) {
            double bx = sx, bz = sz;
            int by = sy;
            for (int k = 0; k < len; k++) {
                bx += Math.cos(a); bz += Math.sin(a);
                if (r.nextFloat() < 0.6f) by++;
                a += (r.nextDouble() - 0.5) * 0.9;
                int ix = (int) Math.round(bx), iz = (int) Math.round(bz);
                putLog(ix, by, iz, logAxis(Reg.WEATHERED_LOG.get(), Math.cos(a), Math.sin(a)));
                if (fork && k == len / 2 && r.nextFloat() < 0.6f) branch(ix, by, iz, a + (r.nextBoolean() ? 1.0 : -1.0), 2 + r.nextInt(3), false);
                if (r.nextFloat() < 0.05f) hangMoss(ix, by - 1, iz, 2 + r.nextInt(4));
            }
            if (r.nextFloat() < 0.5f) hangMoss((int) Math.round(bx), by - 1, (int) Math.round(bz), 2 + r.nextInt(4));
        }

        /** mousse pendante : lianes, retenues par la branche au dessus */
        void hangMoss(int x, int y, int z, int len) {
            for (int k = 0; k < len; k++) {
                if (logs.containsKey(key(x, y - k, z))) break;
                BlockState cur = level.getBlockState(p.set(x, y - k, z));
                if (!soft(cur) && !cur.is(Blocks.WATER)) break;
                putLeaf(x, y - k, z, k == len - 1 ? Blocks.CAVE_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25) : Blocks.CAVE_VINES_PLANT.defaultBlockState());
            }
        }

        /** un ponton de planches grises sur pilotis, au dessus de l'eau */
        boolean jetty() {
            int x = x0 + 3 + r.nextInt(10), z = z0 + 3 + r.nextInt(10);
            if (!mine(x, z)) return false;
            int dxs = r.nextBoolean() ? 1 : 0, dzs = 1 - dxs;
            int len = 8 + r.nextInt(7), y = MapData.SEA + 1, wet = 0;
            for (int i = 0; i < len; i++) {
                int bx = x + dxs * i, bz = z + dzs * i;
                int g = groundY(bx, bz);
                if (g + 1 > y + 1) { if (i == 0) return false; break; }
                if (g < MapData.SEA) wet++;
                set(bx, y, bz, Reg.WEATHERED_PLANKS.get().defaultBlockState());
                if (i % 3 == 0) for (int s = -1; s <= 1; s += 2) {
                    int px = bx + dzs * s, pz = bz + dxs * s;
                    int pg = groundY(px, pz);
                    for (int yy = pg + 1; yy <= y + 1; yy++) set(px, yy, pz, yy == y + 1 ? Blocks.DARK_OAK_FENCE.defaultBlockState() : Blocks.DARK_OAK_LOG.defaultBlockState());
                }
            }
            return wet > 2;
        }
    }
}
