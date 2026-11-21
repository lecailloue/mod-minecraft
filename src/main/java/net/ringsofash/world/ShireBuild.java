package net.ringsofash.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.gen.Reg;

import static net.ringsofash.world.Build.E;
import static net.ringsofash.world.Build.N;
import static net.ringsofash.world.Build.S;
import static net.ringsofash.world.Build.W;
import static net.ringsofash.world.Build.slab;
import static net.ringsofash.world.Build.stairs;

/** La Comte (Hobbiteville) : le terrain, les terriers, l'Arbre de la fete, le pont, le moulin et l'auberge du Dragon Vert, d'apres ShirePlan. */
public final class ShireBuild {
    private ShireBuild() {}

    private static final BlockState AIR = Build.AIR;
    private static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.RED_TULIP,
            Blocks.PINK_TULIP, Blocks.ORANGE_TULIP, Blocks.LILY_OF_THE_VALLEY};
    private static final Block[] DISC = {Blocks.GREEN_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.YELLOW_TERRACOTTA};
    private static final Block[] DOORS = {Blocks.WARPED_DOOR, Blocks.SPRUCE_DOOR, Blocks.CRIMSON_DOOR, Blocks.BIRCH_DOOR};
    private static final Block[] BEDS = {Blocks.GREEN_BED, Blocks.BLUE_BED, Blocks.RED_BED, Blocks.YELLOW_BED};

    private static BlockState hang(Block l) { return l.defaultBlockState().setValue(LanternBlock.HANGING, true); }
    private static BlockState leaves(Block l) { return l.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true); }
    private static BlockState log(Block l, Direction.Axis a) { return l.defaultBlockState().setValue(RotatedPillarBlock.AXIS, a); }
    private static Direction dirOf(int dx, int dz) { return dx > 0 ? E : dx < 0 ? W : dz > 0 ? S : N; }
    private static boolean touches(Build b, int x0, int z0, int x1, int z1) { return b.clip.intersects(b.ox + x0, b.oz + z0, b.ox + x1, b.oz + z1); }
    private static int ground(ShirePlan p, int x, int z) { int h = p.h(x, z); return h > 0 ? h - 1 : -1; }
    private static void people(Build b, int x, int y, int z, int role, int prof, int home) {
        b.mob(ModEntities.HALFLING.get(), x, y, z, home, e -> { if (prof >= 0) e.setProfession(prof); e.setRole(role); });
    }

    public static void build(Build b) {
        ShirePlan p = ShirePlan.get();
        terrain(b, p);
        fields(b, p);
        for (ShirePlan.Hole h : p.holes) { garden(b, h); hole(b, h); }
        bridge(b);
        mill(b);
        inn(b);
        partyTree(b, p);
        for (int[] t : p.trees) tree(b, p, t[0], t[1], t[2]);
    }

    // ================================================================= TERRAIN
    private static void terrain(Build b, ShirePlan p) {
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        int xa = Math.max(0, b.clip.minX() - b.ox), xb = Math.min(ShirePlan.W - 1, b.clip.maxX() - b.ox);
        int za = Math.max(0, b.clip.minZ() - b.oz), zb = Math.min(ShirePlan.D - 1, b.clip.maxZ() - b.oz);
        for (int z = za; z <= zb; z++) for (int x = xa; x <= xb; x++) {
            int ix = ShirePlan.idx(x, z);
            int hh = p.hg[ix];
            int g = Math.max(-40, Math.min(b.groundY(x, z), 20));
            int edge = Math.min(Math.min(x, ShirePlan.W - 1 - x), Math.min(z, ShirePlan.D - 1 - z));
            int top;
            if (hh > 0) top = hh - 1;
            else if (hh == 0) top = (p.res[ix] || edge >= 10) ? -1 : (int) Math.round(g + (-1 - g) * (edge / 10.0));
            else top = -1;
            for (int y = Math.min(g, top + 16); y > top; y--) b.s(x, y, z, AIR);
            b.s(x, top + 1, z, AIR);
            if (hh < 0) {
                int d = -hh, bed = -d - 1;
                for (int y = Math.max(g + 1, bed - 24); y < bed; y++) b.s(x, y, z, dirt);
                b.s(x, bed, z, ShirePlan.hash(x, z, 5) % 3 == 0 ? Blocks.GRAVEL : Blocks.DIRT);
                for (int y = -d; y <= -1; y++) b.s(x, y, z, Blocks.WATER);
                continue;
            }
            for (int y = Math.max(g + 1, top - 24); y < top; y++) b.s(x, y, z, dirt);
            if (hh > 1) { b.s(x, top - 1, z, Blocks.MOSS_BLOCK); if (hh > 2) b.s(x, top - 2, z, Blocks.MOSS_BLOCK); }     // les flancs des collines restent verts
            int sf = p.sf[ix];
            double r = ShirePlan.rnd(x, z, 9);
            if (sf == ShirePlan.PATH) {
                b.s(x, top, z, r < 0.08 ? Blocks.COARSE_DIRT : r < 0.14 ? Blocks.GRAVEL : Blocks.DIRT_PATH);
            } else if (sf == ShirePlan.FIELD) {
                b.s(x, top, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            } else {
                b.s(x, top, z, Blocks.GRASS_BLOCK);
                if (sf == ShirePlan.GARDEN) {
                    if (r < 0.16) b.s(x, top + 1, z, FLOWERS[ShirePlan.hash(x, z, 11) % FLOWERS.length]);
                    else if (r < 0.3) b.s(x, top + 1, z, Blocks.SHORT_GRASS);
                } else if (r < 0.04) b.s(x, top + 1, z, FLOWERS[ShirePlan.hash(x, z, 11) % FLOWERS.length]);
                else if (r < 0.2) b.s(x, top + 1, z, Blocks.SHORT_GRASS);
                else if (r < 0.215) b.s(x, top + 1, z, Blocks.FERN);
            }
        }
    }

    private static void fields(Build b, ShirePlan p) {
        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES};
        for (int[] f : p.fields) {
            if (!touches(b, f[0] - 1, f[1] - 1, f[2] + 1, f[3] + 1)) continue;
            Block c = crops[f[4]];
            for (int z = f[1]; z <= f[3]; z++) for (int x = f[0]; x <= f[2]; x++) {
                boolean edge = x == f[0] || x == f[2] || z == f[1] || z == f[3];
                if (edge) {
                    b.s(x, -1, z, Blocks.GRASS_BLOCK);
                    boolean gate = z == f[3] && x == (f[0] + f[2]) / 2;
                    b.s(x, 0, z, gate ? Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, S) : Blocks.OAK_FENCE.defaultBlockState());
                    if ((x + z) % 8 == 0 && !gate) b.s(x, 1, z, Blocks.LANTERN);
                } else if ((z - f[1]) % 5 == 2) {
                    b.s(x, -1, z, Blocks.WATER);
                    b.s(x, 0, z, AIR);
                } else {
                    b.s(x, -1, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                    int hv = ShirePlan.hash(x, z, 13);
                    if (hv % 23 == 0) b.s(x, 0, z, Blocks.PUMPKIN);
                    else b.s(x, 0, z, c.defaultBlockState().setValue(CropBlock.AGE, 4 + hv % 4));
                }
            }
            int cx = (f[0] + f[2]) / 2 + 3, cz = f[1] + 8;
            b.s(cx, 0, cz, Blocks.OAK_FENCE); b.s(cx, 1, cz, Blocks.OAK_FENCE); b.s(cx - 1, 1, cz, Blocks.OAK_FENCE); b.s(cx + 1, 1, cz, Blocks.OAK_FENCE);
            b.s(cx, 2, cz, Blocks.CARVED_PUMPKIN.defaultBlockState());
            b.s(cx, 3, cz, slab(Blocks.OAK_SLAB, false));
        }
    }

    // ================================================================= ARBRES
    private static void ball(Build b, int cx, int cy, int cz, double rx, double ry, BlockState lv, int salt) {
        int ix = (int) Math.ceil(rx), iy = (int) Math.ceil(ry);
        for (int dx = -ix; dx <= ix; dx++) for (int dz = -ix; dz <= ix; dz++) for (int dy = -iy; dy <= iy; dy++) {
            double d = (dx * dx + dz * dz) / (rx * rx) + (dy * dy) / (ry * ry);
            if (d > 1) continue;
            int x = cx + dx, y = cy + dy, z = cz + dz;
            if (d > 0.55 && ShirePlan.hash(x * 3 + y, z, salt) % 100 < 14) continue;
            if (b.in(x, y, z) && b.get(x, y, z).isAir()) b.s(x, y, z, lv);
        }
    }

    private static void tree(Build b, ShirePlan p, int tx, int tz, int kind) {
        int r = kind == 1 ? 9 : 6;
        if (!touches(b, tx - r, tz - r, tx + r, tz + r)) return;
        int gy = ground(p, tx, tz);
        int hs = ShirePlan.hash(tx, tz, 7);
        BlockState lv = leaves(hs % 6 == 0 ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES);
        if (kind == 2) { ball(b, tx, gy + 2, tz, 2.1, 1.6, lv, hs); return; }
        boolean big = kind == 1;
        int hgt = big ? 10 + hs % 4 : 5 + hs % 3;
        Block trunk = lv.is(Blocks.BIRCH_LEAVES) ? Blocks.BIRCH_LOG : Blocks.OAK_LOG;
        ball(b, tx, gy + hgt, tz, big ? 6.5 : 3.4, big ? 4.6 : 2.9, lv, hs);
        if (big) { ball(b, tx + 3, gy + hgt - 2, tz + 2, 3.5, 2.6, lv, hs + 1); ball(b, tx - 3, gy + hgt - 1, tz - 2, 3.5, 2.6, lv, hs + 2); }
        for (int y = gy + 1; y <= gy + hgt; y++) {
            b.s(tx, y, tz, trunk);
            if (big) { b.s(tx + 1, y, tz, trunk); b.s(tx, y, tz + 1, trunk); b.s(tx + 1, y, tz + 1, trunk); }
        }
    }

    private static void partyTree(Build b, ShirePlan p) {
        int cx = ShirePlan.PARTY_X, cz = ShirePlan.PARTY_Z;
        if (!touches(b, cx - 18, cz - 18, cx + 18, cz + 18)) return;
        int gy = ground(p, cx, cz);
        BlockState lv = leaves(Blocks.OAK_LEAVES);
        int H = 17;
        ball(b, cx, gy + H + 5, cz, 13, 8.5, lv, 31);
        ball(b, cx + 8, gy + H + 1, cz + 3, 6.5, 4.5, lv, 32);
        ball(b, cx - 8, gy + H + 2, cz - 3, 6.5, 4.5, lv, 33);
        ball(b, cx + 2, gy + H + 1, cz - 9, 6.5, 4.5, lv, 34);
        ball(b, cx - 3, gy + H + 1, cz + 9, 6.5, 4.5, lv, 35);
        BlockState lg = Blocks.OAK_LOG.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            double d = Math.hypot(dx, dz);
            int x = cx + dx, z = cz + dz;
            int top = d <= 2.3 ? H : d <= 3.2 ? 4 : -9;                        // tronc, racines evasees
            if (d <= 3.2 && d > 2.3 && ShirePlan.hash(x, z, 41) % 3 == 0) top = 2;
            if (top < 0) continue;
            int g0 = ground(p, x, z);
            for (int y = g0; y <= gy + top; y++) b.s(x, y, z, lg);
        }
        // grosses branches
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int i = 0; i < dirs.length; i++) {
            int bx = cx, bz = cz, by = gy + H - 3 - i % 3;
            for (int k = 1; k <= 8; k++) {
                bx += dirs[i][0]; bz += dirs[i][1];
                if (k % 2 == 0) by++;
                b.s(bx, by, bz, log(Blocks.OAK_LOG, dirs[i][0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
            }
        }
        // lanternes accrochees sous le feuillage, bancs au pied de l'arbre
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14;
            int lx = cx + (int) Math.round(Math.cos(a) * 7.5), lz = cz + (int) Math.round(Math.sin(a) * 7.5);
            int ly = ground(p, lx, lz);
            b.s(lx, ly + 1, lz, Blocks.OAK_FENCE); b.s(lx, ly + 2, lz, Blocks.OAK_FENCE); b.s(lx, ly + 3, lz, Blocks.LANTERN);
            int bx2 = cx + (int) Math.round(Math.cos(a + 0.2) * 5.5), bz2 = cz + (int) Math.round(Math.sin(a + 0.2) * 5.5);
            if (i % 2 == 0) b.s(bx2, ground(p, bx2, bz2) + 1, bz2, stairs(Blocks.SPRUCE_STAIRS, Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? (Math.cos(a) > 0 ? W : E) : (Math.sin(a) > 0 ? N : S), false));
        }
        // tables de la fete
        for (int i = 0; i < 3; i++) {
            int tx = cx - 12 + i * 12, tz = cz + 13;
            int gz = ground(p, tx, tz);
            for (int dx = -2; dx <= 2; dx++) { b.s(tx + dx, gz + 1, tz, slab(Blocks.SPRUCE_SLAB, false)); }
            b.s(tx - 2, gz + 1, tz, Blocks.SPRUCE_FENCE); b.s(tx + 2, gz + 1, tz, Blocks.SPRUCE_FENCE);
            b.s(tx - 2, gz + 2, tz, slab(Blocks.SPRUCE_SLAB, false)); b.s(tx + 2, gz + 2, tz, slab(Blocks.SPRUCE_SLAB, false));
        }
        people(b, cx + 2, gy + 1, cz + 6, PeopleEntity.CIVIL, 0, 14);
        people(b, cx - 4, gy + 1, cz + 5, PeopleEntity.CIVIL, 1, 14);
    }

    // ================================================================= TERRIERS
    /** repere d'un terrier : u = lateral (droite en regardant vers l'interieur), v = profondeur (v < 0 : devant la porte) */
    private static final class H {
        final Build b; final ShirePlan.Hole h; final Direction out, in, right;
        final boolean[] carved = new boolean[64 * 64];
        H(Build b, ShirePlan.Hole h) { this.b = b; this.h = h; out = dirOf(h.ox, h.oz); in = out.getOpposite(); right = dirOf(-h.oz, h.ox); }
        void s(int u, int y, int v, BlockState st) { b.s(h.x(u, v), y, h.z(u, v), st); }
        void s(int u, int y, int v, Block bl) { s(u, y, v, bl.defaultBlockState()); }
        void fill(int u0, int y0, int v0, int u1, int y1, int v1, Block bl) {
            for (int u = u0; u <= u1; u++) for (int y = y0; y <= y1; y++) for (int v = v0; v <= v1; v++) s(u, y, v, bl);
        }
        boolean has(int u, int v) { return u >= -31 && u <= 31 && v >= 0 && v < 64 && carved[(u + 32) * 64 + v]; }
        /** meuble : seulement si la case a bien ete creusee */
        void put(int u, int y, int v, BlockState st) { if (has(u, v)) s(u, y, v, st); }
        void put(int u, int y, int v, Block bl) { put(u, y, v, bl.defaultBlockState()); }
        void people(int u, int v, int role, int prof, int home) { if (has(u, v)) ShireBuild.people(b, h.x(u, v), 0, h.z(u, v), role, prof, home); }
    }

    private static boolean touchesHole(Build b, ShirePlan.Hole h) {
        int R = (int) Math.ceil(h.ru) + 10, DV = (int) Math.ceil(h.dv) + 3;
        int[] xs = {h.x(-R, -16), h.x(R, -16), h.x(-R, DV), h.x(R, DV)}, zs = {h.z(-R, -16), h.z(R, -16), h.z(-R, DV), h.z(R, DV)};
        int x0 = 9999, x1 = -9999, z0 = 9999, z1 = -9999;
        for (int v : xs) { x0 = Math.min(x0, v); x1 = Math.max(x1, v); }
        for (int v : zs) { z0 = Math.min(z0, v); z1 = Math.max(z1, v); }
        return touches(b, x0, z0, x1, z1);
    }

    private static void garden(Build b, ShirePlan.Hole h) {
        if (!touchesHole(b, h)) return;
        H a = new H(b, h);
        int F = (int) Math.ceil(h.ru) + 1;
        for (int v = -1; v >= -12; v--) {
            for (int u = -1; u <= 1; u++) a.s(u, -1, v, Blocks.DIRT_PATH);
            if ((v & 1) == 0) a.s(0, -1, v, Blocks.COBBLESTONE);
        }
        // clotures blanches, portillon sur le chemin
        for (int u = -F; u <= F; u++) if (u != 0) a.s(u, 0, -12, Blocks.BIRCH_FENCE);
        a.s(0, 0, -12, Blocks.BIRCH_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, a.out));
        for (int v = -2; v >= -11; v--) { a.s(-F, 0, v, Blocks.BIRCH_FENCE); a.s(F, 0, v, Blocks.BIRCH_FENCE); }
        a.s(-F, 0, -1, Blocks.BIRCH_FENCE); a.s(F, 0, -1, Blocks.BIRCH_FENCE);
        for (int[] c : new int[][]{{-F, -12}, {F, -12}, {-F, -1}, {F, -1}, {-1, -12}, {1, -12}}) a.s(c[0], 1, c[1], Blocks.LANTERN);
        // potager a droite du chemin, buisson et bancs a gauche
        Block[] crops = {Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT};
        Block crop = crops[Math.floorMod(h.fx + h.fz, 3)];
        int u1 = Math.min(F - 1, h.big ? 9 : F - 1);
        for (int u = 3; u <= u1; u++) for (int v = -3; v >= -10; v--) {
            a.s(u, -1, v, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            a.s(u, 0, v, crop.defaultBlockState().setValue(CropBlock.AGE, 3 + ShirePlan.hash(a.h.x(u, v), a.h.z(u, v), 17) % 4));
        }
        for (int dv = -1; dv <= 1; dv++) for (int du = -1; du <= 1; du++) for (int dy = 0; dy <= 1; dy++)
            if (du * du + dv * dv + dy * dy <= 2) a.s(-F + 2 + du, dy, -6 + dv, leaves(Blocks.OAK_LEAVES));
        int bx = (int) Math.ceil((h.big ? 4.2 : 3.2)) + 2;
        a.s(-bx, 0, -1, Blocks.BARREL); a.s(bx, 0, -1, Blocks.HAY_BLOCK);
        for (int sgn = -1; sgn <= 1; sgn += 2) { a.s(sgn * 2, 0, -2, Blocks.SPRUCE_FENCE); a.s(sgn * 2, 1, -2, Blocks.LANTERN); }
        if (ShirePlan.hash(h.fx, h.fz, 19) % 2 == 0) {
            int bx2 = h.x(-3, -5), bz2 = h.z(-3, -5);
            if (b.in(bx2, 0, bz2)) people(b, bx2, 0, bz2, PeopleEntity.CIVIL, ShirePlan.hash(h.fx, h.fz, 20) % 3, 12);
        }
    }

    private static void disc(H a, int cu, int cy, double rin, double rout, BlockState in, BlockState frame) {
        int r = (int) Math.ceil(rout);
        for (int du = -r; du <= r; du++) for (int dy = -r; dy <= r; dy++) {
            int y = cy + dy, u = cu + du;
            if (y < 0 || y > a.h.domeH(u, 0) - 2) continue;
            double d = Math.hypot(du, dy);
            if (d <= rin) a.s(u, y, 0, in);
            else if (d <= rout) a.s(u, y, 0, frame);
        }
    }

    private static void hole(Build b, ShirePlan.Hole h) {
        if (!touchesHole(b, h)) return;
        H a = new H(b, h);
        int R = (int) Math.ceil(h.ru) + 1;
        Block wall = h.wall == 0 ? Blocks.SMOOTH_SANDSTONE : h.wall == 1 ? Blocks.STONE_BRICKS : Blocks.BRICKS;
        Block frame = h.wall == 0 ? Blocks.DARK_OAK_PLANKS : h.wall == 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS;
        // facade : le dome est coupe en demi-cercle, on l'habille de platre / pierre / brique ; un liseré de pierre court sur l'arc
        for (int u = -R; u <= R; u++) {
            int dh = h.domeH(u, 0);
            if (dh <= 0) continue;
            for (int y = 0; y < dh - 1; y++) {
                a.s(u, y, 0, h.wall == 1 && ShirePlan.hash(u + h.fx, y, h.fz) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : wall);
            }
            a.s(u, dh - 1, 0, Blocks.STONE_BRICKS);
        }
        int cy = h.big ? 4 : 2;
        double rin = h.big ? 3.2 : 2.2;
        disc(a, 0, cy, rin, rin + 1.0, DISC[h.door].defaultBlockState(), frame.defaultBlockState());
        a.s(0, cy, 0, Blocks.GOLD_BLOCK);
        b.door(h.x(0, 0), 0, h.z(0, 0), DOORS[h.door], a.out);
        int wu = h.big ? 8 : 5, wy = h.big ? 3 : 2;
        double wr = h.big ? 1.6 : 1.1;
        for (int sgn = -1; sgn <= 1; sgn += 2) disc(a, sgn * wu, wy, wr, wr + 0.9, Blocks.YELLOW_STAINED_GLASS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
        if (h.big) for (int sgn = -1; sgn <= 1; sgn += 2) disc(a, sgn * 11, 2, 1.1, 1.9, Blocks.YELLOW_STAINED_GLASS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState());

        if (h.big) bagEnd(a); else smallHole(a);
    }

    private record Room(int u0, int u1, int v0, int v1, int rh) {}

    private static void rooms(H a, Room... rs) {
        ShirePlan.Hole h = a.h;
        for (Room r : rs) for (int u = r.u0 - 1; u <= r.u1 + 1; u++) for (int v = Math.max(1, r.v0 - 1); v <= r.v1 + 1; v++) {
            int dh = h.domeH(u, v);
            for (int y = -1; y <= r.rh && y <= dh - 1; y++) a.s(u, y, v, Blocks.SPRUCE_PLANKS);
        }
        for (Room r : rs) for (int u = r.u0; u <= r.u1; u++) for (int v = r.v0; v <= r.v1; v++) {
            if (h.domeH(u, v) < r.rh + 2) continue;
            a.s(u, -1, v, Blocks.OAK_PLANKS);
            for (int y = 0; y < r.rh; y++) a.s(u, y, v, AIR);
            a.s(u, r.rh, v, Blocks.SPRUCE_PLANKS);
            a.carved[(u + 32) * 64 + v] = true;
        }
        for (Room r : rs) if (r.u1 - r.u0 >= 2) for (int v = r.v0 + 2; v <= r.v1; v += 3) for (int u = r.u0; u <= r.u1; u++)
            if (a.has(u, v)) a.s(u, r.rh - 1, v, log(Blocks.STRIPPED_SPRUCE_LOG, a.right.getAxis()));
    }

    private static void smallHole(H a) {
        ShirePlan.Hole h = a.h;
        rooms(a, new Room(-3, 3, 1, 8, 4), new Room(-2, 2, 9, 11, 3));
        a.put(0, 0, 1, Blocks.RED_CARPET); a.put(0, 0, 2, Blocks.RED_CARPET);
        // lit, coffre, tonneau
        if (a.has(-2, 8) && a.has(-2, 7)) b(a).bed(h.x(-2, 8), 0, h.z(-2, 8), BEDS[h.door], a.in);
        if (a.has(2, 8)) b(a).chest(h.x(2, 8), 0, h.z(2, 8), a.out, "halfling_house");
        a.put(3, 0, 8, Blocks.BARREL);
        a.put(-3, 0, 2, Blocks.CRAFTING_TABLE);
        a.put(-3, 0, 3, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, a.right));
        a.put(3, 0, 2, Blocks.BOOKSHELF); a.put(3, 1, 2, Blocks.BOOKSHELF); a.put(3, 0, 3, Blocks.BOOKSHELF); a.put(3, 1, 3, Blocks.BOOKSHELF);
        // table et deux chaises
        a.put(0, 0, 5, Blocks.SPRUCE_FENCE); a.put(0, 1, 5, Blocks.SPRUCE_PRESSURE_PLATE);
        a.put(1, 0, 5, stairs(Blocks.SPRUCE_STAIRS, a.right, false)); a.put(-1, 0, 5, stairs(Blocks.SPRUCE_STAIRS, a.right.getOpposite(), false));
        a.put(0, 3, 5, hang(Blocks.LANTERN)); a.put(0, 3, 8, hang(Blocks.LANTERN));
        a.put(-3, 1, 6, Blocks.POTTED_POPPY);
        // garde-manger
        a.put(-2, 0, 10, Blocks.BARREL); a.put(2, 0, 10, Blocks.BARREL); a.put(2, 0, 11, Blocks.HAY_BLOCK); a.put(-2, 0, 11, Blocks.BARREL);
        if (a.has(0, 11)) b(a).chest(h.x(0, 11), 0, h.z(0, 11), a.out, "halfling_house");
        a.put(0, 2, 10, hang(Blocks.LANTERN));
        a.people(0, 3, PeopleEntity.CIVIL, ShirePlan.hash(h.fx, h.fz, 21) % 3, 10);
        if (ShirePlan.hash(h.fx, h.fz, 22) % 2 == 0) a.people(1, 7, PeopleEntity.CIVIL, ShirePlan.hash(h.fx, h.fz, 23) % 3, 10);
    }

    private static Build b(H a) { return a.b; }

    /** Cul-de-Sac : grande salle, cheminee, couloir en cul-de-sac et deux chambres */
    private static void bagEnd(H a) {
        ShirePlan.Hole h = a.h;
        rooms(a, new Room(-7, 7, 1, 10, 7), new Room(-2, 2, 11, 22, 5), new Room(-7, -4, 13, 19, 4), new Room(4, 7, 13, 19, 4), new Room(-3, -3, 15, 16, 3), new Room(3, 3, 15, 16, 3));
        for (int u = -4; u <= 4; u++) for (int v = 2; v <= 9; v++) a.put(u, 0, v, (u + v) % 2 == 0 ? Blocks.RED_CARPET : Blocks.GREEN_CARPET);
        for (int v = 11; v <= 22; v++) for (int u = -1; u <= 1; u++) a.put(u, 0, v, Blocks.GREEN_CARPET);
        // cheminee contre le mur de gauche
        for (int v = 4; v <= 6; v++) for (int y = 0; y <= 3; y++) a.put(-7, y, v, Blocks.BRICKS);
        a.put(-7, 0, 5, Blocks.CAMPFIRE); a.put(-7, 1, 5, AIR);
        a.put(-6, 0, 3, stairs(Blocks.SPRUCE_STAIRS, a.right, false)); a.put(-5, 0, 5, stairs(Blocks.SPRUCE_STAIRS, a.right.getOpposite(), false));
        a.put(-6, 0, 7, stairs(Blocks.SPRUCE_STAIRS, a.right, false));
        // longue table et chaises
        for (int u = -2; u <= 2; u++) for (int v = 5; v <= 6; v++) { a.put(u, 1, v, slab(Blocks.SPRUCE_SLAB, false)); }
        for (int u : new int[]{-2, 2}) for (int v = 5; v <= 6; v++) a.put(u, 0, v, Blocks.SPRUCE_FENCE);
        for (int u = -2; u <= 2; u += 2) { a.put(u, 0, 4, stairs(Blocks.SPRUCE_STAIRS, a.out, false)); a.put(u, 0, 7, stairs(Blocks.SPRUCE_STAIRS, a.in, false)); }
        a.put(0, 2, 5, Blocks.CANDLE); a.put(-1, 2, 6, Blocks.CANDLE);
        // bibliotheque du mur de droite
        for (int v = 2; v <= 9; v++) for (int y = 0; y <= 2; y++) a.put(7, y, v, Blocks.BOOKSHELF);
        for (int u : new int[]{-4, 4}) for (int v : new int[]{3, 8}) a.put(u, 6, v, hang(Blocks.LANTERN));
        a.put(0, 6, 5, hang(Blocks.LANTERN));
        // couloir : tonneaux, foin, coffres ; cuisine au fond
        for (int v = 12; v <= 21; v++) {
            a.put(-2, 0, v, v % 3 == 0 ? Blocks.HAY_BLOCK : Blocks.BARREL);
            a.put(2, 0, v, v % 4 == 0 ? Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, a.right.getOpposite()) : Blocks.BOOKSHELF.defaultBlockState());
        }
        for (int v : new int[]{13, 18}) a.put(0, 4, v, hang(Blocks.LANTERN));
        a.put(-1, 0, 22, Blocks.CRAFTING_TABLE); a.put(0, 0, 22, Blocks.SMOKER.defaultBlockState().setValue(FurnaceBlock.FACING, a.out)); a.put(1, 0, 22, Blocks.CAULDRON);
        // chambres
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            int u = sgn * 6;
            if (a.has(u, 19) && a.has(u, 18)) a.b.bed(h.x(u, 19), 0, h.z(u, 19), sgn < 0 ? Blocks.GREEN_BED : Blocks.BLUE_BED, a.in);
            if (a.has(sgn * 7, 15)) a.b.chest(h.x(sgn * 7, 15), 0, h.z(sgn * 7, 15), sgn < 0 ? a.right : a.right.getOpposite(), "halfling_house");
            a.put(sgn * 5, 0, 14, Blocks.BARREL);
            a.put(sgn * 5, 0, 19, Blocks.CRAFTING_TABLE);
            a.put(sgn * 5, 3, 16, hang(Blocks.LANTERN));
            a.put(sgn * 4, 0, 13, Blocks.FLOWER_POT);
        }
        a.people(0, 3, PeopleEntity.ELDER, 1, 12);
        a.people(3, 8, PeopleEntity.CIVIL, 0, 12);
        a.people(0, 16, PeopleEntity.CIVIL, 2, 10);
    }

    // ================================================================= PONT, MOULIN, AUBERGE
    private static void bridge(Build b) {
        int bx = ShirePlan.BRIDGE_X, z0 = ShirePlan.STREAM_Z0, z1 = ShirePlan.STREAM_Z1;
        if (!touches(b, bx - 5, z0 - 6, bx + 5, z1 + 6)) return;
        Block sb = Blocks.STONE_BRICKS;
        for (int x = bx - 2; x <= bx + 2; x++) {
            b.fill(x, 2, z0, x, 2, z1, sb);
            for (int z : new int[]{z0, z0 + 3, z0 + 4, z1}) b.fill(x, -3, z, x, 1, z, ShirePlan.hash(x, z, 3) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS : sb);
        }
        // garde-corps et lanternes
        for (int z = z0; z <= z1; z++) { b.s(bx - 2, 3, z, Blocks.STONE_BRICK_WALL); b.s(bx + 2, 3, z, Blocks.STONE_BRICK_WALL); }
        for (int z : new int[]{z0, z1}) for (int x : new int[]{bx - 2, bx + 2}) b.s(x, 4, z, Blocks.LANTERN);
        // rampes : trois marches de chaque cote
        for (int i = 0; i < 3; i++) {
            int zn = z0 - 3 + i, zs = z1 + 3 - i;
            for (int x = bx - 1; x <= bx + 1; x++) {
                b.fill(x, 0, zn, x, i - 1, zn, sb); b.fill(x, 0, zs, x, i - 1, zs, sb);
                b.s(x, i, zn, stairs(Blocks.STONE_BRICK_STAIRS, S, false)); b.s(x, i, zs, stairs(Blocks.STONE_BRICK_STAIRS, N, false));
            }
            for (int x : new int[]{bx - 2, bx + 2}) {
                b.fill(x, 0, zn, x, i, zn, sb); b.fill(x, 0, zs, x, i, zs, sb);
                b.s(x, i + 1, zn, Blocks.STONE_BRICK_WALL); b.s(x, i + 1, zs, Blocks.STONE_BRICK_WALL);
            }
        }
    }

    private static final class At {
        final Build b; final int ox, oz;
        At(Build b, int ox, int oz) { this.b = b; this.ox = ox; this.oz = oz; }
        void s(int x, int y, int z, BlockState st) { b.s(ox + x, y, oz + z, st); }
        void s(int x, int y, int z, Block bl) { b.s(ox + x, y, oz + z, bl); }
        void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block bl) { b.fill(ox + x0, y0, oz + z0, ox + x1, y1, oz + z1, bl); }
        void air(int x0, int y0, int z0, int x1, int y1, int z1) { b.air(ox + x0, y0, oz + z0, ox + x1, y1, oz + z1); }
        void ring(int x0, int y0, int z0, int x1, int y1, int z1, Block bl) { b.walls(ox + x0, y0, oz + z0, ox + x1, y1, oz + z1, bl); }
        void door(int x, int z, Block door, Direction f) { b.door(ox + x, 0, oz + z, door, f); }
        void chest(int x, int y, int z, Direction f) { b.chest(ox + x, y, oz + z, f, "halfling_house"); }
        void people(int x, int z, int role, int prof) { ShireBuild.people(b, ox + x, 0, oz + z, role, prof, 14); }
    }

    /** toit de chaume a deux pans dont le faitage court le long de x, sur [x0..x1] et de z0 a z1 (inclus, debord compris) ; pignons de planches */
    private static void roofX(At a, int x0, int x1, int z0, int z1, int y, Block pignon) {
        Block ts = Reg.THATCH_STAIRS.get(), tb = Reg.THATCH.get();
        int n = (z1 - z0) / 2;
        for (int i = 0; i <= n; i++) {
            int zn = z0 + i, zs = z1 - i, yy = y + i;
            for (int x = x0; x <= x1; x++) {
                if (zn == zs) a.s(x, yy, zn, tb);
                else { a.s(x, yy, zn, stairs(ts, S, false)); a.s(x, yy, zs, stairs(ts, N, false)); }
            }
            for (int z = zn + 1; z <= zs - 1; z++) { a.s(x0 + 1, yy, z, pignon); a.s(x1 - 1, yy, z, pignon); }
        }
    }

    private static void mill(Build b) {
        int ox = ShirePlan.MILL_X0, oz = ShirePlan.MILL_Z0;
        if (!touches(b, ox - 3, oz - 3, ox + 15, oz + 16)) return;
        At a = new At(b, ox, oz);
        int w = 12, d = 11;
        a.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.COBBLESTONE);
        a.fill(-1, -1, -3, w, -1, -1, Blocks.COBBLESTONE);
        a.ring(0, 0, 0, w - 1, 2, d - 1, Blocks.COBBLESTONE);
        for (int i = 0; i < 12; i++) a.s(1 + (i * 7) % 10, (i * 3) % 3, i % 2 == 0 ? 0 : d - 1, Blocks.MOSSY_COBBLESTONE);
        a.ring(0, 3, 0, w - 1, 5, d - 1, Blocks.BIRCH_PLANKS);
        for (int x : new int[]{0, 4, 7, 11}) for (int z : new int[]{0, d - 1}) a.fill(x, 0, z, x, 5, z, Blocks.DARK_OAK_LOG);
        a.fill(0, 0, 5, 0, 5, 5, Blocks.DARK_OAK_LOG); a.fill(w - 1, 0, 5, w - 1, 5, 5, Blocks.DARK_OAK_LOG);
        a.ring(0, 5, 0, w - 1, 5, d - 1, Blocks.DARK_OAK_PLANKS);
        a.air(1, 0, 1, w - 2, 5, d - 2);
        a.fill(1, -1, 1, w - 2, -1, d - 2, Blocks.SPRUCE_PLANKS);
        roofX(a, -1, w, -1, d, 6, Blocks.BIRCH_PLANKS);
        a.door(8, 0, Blocks.SPRUCE_DOOR, N);
        a.s(7, 1, 0, Blocks.LANTERN); a.s(9, 1, 0, Blocks.LANTERN);
        for (int x : new int[]{2, 5, 9}) { a.s(x, 4, 0, Blocks.GLASS_PANE); a.s(x, 4, d - 1, Blocks.GLASS_PANE); }
        a.s(0, 4, 3, Blocks.GLASS_PANE); a.s(w - 1, 4, 3, Blocks.GLASS_PANE); a.s(0, 4, 7, Blocks.GLASS_PANE); a.s(w - 1, 4, 7, Blocks.GLASS_PANE);
        // etage : plancher et echelle
        a.fill(1, 3, 1, 10, 3, 4, Blocks.SPRUCE_PLANKS);
        for (int y = 0; y <= 4; y++) a.s(1, y, 5, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, E));
        // meule, sacs, tonneaux
        a.s(6, 0, 6, Blocks.GRINDSTONE); a.s(7, 0, 6, Blocks.COBBLESTONE_SLAB); a.s(5, 0, 6, Blocks.COBBLESTONE_SLAB);
        for (int x = 9; x <= 10; x++) for (int z = 6; z <= 9; z++) { a.s(x, 0, z, Blocks.HAY_BLOCK); if ((x + z) % 2 == 0) a.s(x, 1, z, Blocks.HAY_BLOCK); }
        for (int z = 6; z <= 9; z++) a.s(2, 0, z, Blocks.BARREL);
        a.s(3, 0, 9, Blocks.COMPOSTER); a.chest(4, 0, 9, N);
        a.s(5, 2, 4, hang(Blocks.LANTERN)); a.s(6, 2, 8, hang(Blocks.LANTERN));
        a.s(6, 4, 2, hang(Blocks.LANTERN));
        a.people(6, 4, PeopleEntity.CIVIL, 1);
        // roue a aubes : axe le long de z, dans le ruisseau
        int cx = 6, cy = 2;
        for (int z = d; z <= d + 3; z++) a.s(cx, cy, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        for (int z = d + 1; z <= d + 2; z++) for (int dx = -4; dx <= 4; dx++) for (int dy = -4; dy <= 4; dy++) {
            double dist = Math.hypot(dx, dy);
            int y = cy + dy;
            if (y < -2 || dist < 0.9) continue;
            boolean spoke = (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy)) && dist <= 3.6;
            if (spoke) a.s(cx + dx, y, z, Blocks.SPRUCE_PLANKS);
            else if (dist > 3.6 && dist <= 4.5) a.s(cx + dx, y, z, Blocks.DARK_OAK_PLANKS);
        }
        // chemin devant la porte
        for (int z = -3; z <= -1; z++) a.s(8, -1, z, Blocks.DIRT_PATH);
    }

    private static void inn(Build b) {
        int ox = ShirePlan.INN_X0, oz = ShirePlan.INN_Z0;
        if (!touches(b, ox - 4, oz - 8, ox + 24, oz + 14)) return;
        At a = new At(b, ox, oz);
        int w = 20, d = 11;
        a.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.SPRUCE_PLANKS);
        a.ring(0, 0, 0, w - 1, 4, d - 1, Blocks.BIRCH_PLANKS);
        a.ring(0, 0, 0, w - 1, 1, d - 1, Blocks.STONE_BRICKS);
        for (int i = 0; i < 20; i++) a.s((i * 7 + 3) % 18 + 1, i % 2, i % 3 == 0 ? 0 : d - 1, Blocks.MOSSY_STONE_BRICKS);
        for (int x : new int[]{0, 5, 10, 15, 19}) for (int z : new int[]{0, d - 1}) a.fill(x, 0, z, x, 4, z, Blocks.DARK_OAK_LOG);
        for (int z : new int[]{5}) { a.fill(0, 0, z, 0, 4, z, Blocks.DARK_OAK_LOG); a.fill(w - 1, 0, z, w - 1, 4, z, Blocks.DARK_OAK_LOG); }
        a.ring(0, 4, 0, w - 1, 4, d - 1, Blocks.DARK_OAK_PLANKS);
        a.air(1, 0, 1, w - 2, 4, d - 2);
        roofX(a, -1, w, -1, d, 5, Blocks.BIRCH_PLANKS);
        // porte, fenetres, enseigne
        a.door(9, 0, Blocks.SPRUCE_DOOR, N); a.fill(10, 0, 0, 10, 3, 0, Blocks.DARK_OAK_LOG);
        a.s(8, 3, 0, Blocks.AIR); a.s(9, 2, 0, Blocks.GLASS_PANE);
        for (int x : new int[]{2, 3, 6, 7, 13, 14, 17, 18}) a.s(x, 2, 0, Blocks.GLASS_PANE);
        for (int x : new int[]{3, 4, 8, 9, 10, 14, 15}) a.s(x, 2, d - 1, Blocks.GLASS_PANE);
        for (int z : new int[]{2, 3, 7, 8}) { a.s(0, 2, z, Blocks.GLASS_PANE); a.s(w - 1, 2, z, Blocks.GLASS_PANE); }
        a.s(12, 3, -1, Blocks.GREEN_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, N));
        a.s(7, 1, -1, Blocks.OAK_FENCE); a.s(7, 2, -1, Blocks.LANTERN); a.s(12, 1, -1, Blocks.OAK_FENCE); a.s(12, 2, -1, Blocks.LANTERN);
        // cheminee
        a.fill(1, 0, 4, 2, 13, 6, Blocks.BRICKS);
        a.air(1, 0, 5, 2, 1, 5); a.s(1, 0, 5, Blocks.CAMPFIRE); a.air(1, 2, 5, 1, 13, 5);
        // salle : comptoir, tonneaux, tables, chaises
        for (int z = 2; z <= 8; z++) { a.s(14, 0, z, Blocks.SPRUCE_PLANKS); a.s(14, 1, z, slab(Blocks.SPRUCE_SLAB, false)); }
        for (int z = 1; z <= 9; z++) { a.s(18, 0, z, Blocks.BARREL); if (z % 2 == 1) a.s(18, 1, z, Blocks.BARREL); a.s(18, 2, z, Blocks.BOOKSHELF); }
        a.chest(17, 0, 1, S); a.chest(17, 0, 9, N);
        for (int[] t : new int[][]{{5, 2}, {5, 8}, {9, 5}, {10, 8}}) {
            a.s(t[0], 0, t[1], Blocks.SPRUCE_FENCE); a.s(t[0], 1, t[1], Blocks.SPRUCE_PRESSURE_PLATE);
            a.s(t[0] + 1, 0, t[1], stairs(Blocks.SPRUCE_STAIRS, E, false)); a.s(t[0] - 1, 0, t[1], stairs(Blocks.SPRUCE_STAIRS, W, false));
            a.s(t[0], 0, t[1] + 1, stairs(Blocks.SPRUCE_STAIRS, S, false)); a.s(t[0], 0, t[1] - 1, stairs(Blocks.SPRUCE_STAIRS, N, false));
        }
        for (int x = 7; x <= 12; x++) a.s(x, 0, 3, Blocks.GREEN_CARPET);
        for (int[] l : new int[][]{{5, 5}, {9, 3}, {9, 8}, {13, 5}, {16, 5}}) a.s(l[0], 3, l[1], hang(Blocks.LANTERN));
        a.s(1, 3, 2, Blocks.WALL_TORCH.defaultBlockState());
        // terrasse devant : tables et fleurs
        for (int[] t : new int[][]{{2, -4}, {10, -5}, {16, -4}}) {
            a.s(t[0], 0, t[1], Blocks.SPRUCE_FENCE); a.s(t[0], 1, t[1], Blocks.SPRUCE_PRESSURE_PLATE);
            a.s(t[0] + 1, 0, t[1], stairs(Blocks.SPRUCE_STAIRS, E, false)); a.s(t[0] - 1, 0, t[1], stairs(Blocks.SPRUCE_STAIRS, W, false));
            a.s(t[0], 0, t[1] + 1, stairs(Blocks.SPRUCE_STAIRS, S, false));
            a.s(t[0], 2, t[1], Blocks.LANTERN);
        }
        a.s(0, 0, -1, Blocks.BARREL); a.s(19, 0, -1, Blocks.BARREL); a.s(19, 1, -1, Blocks.POTTED_RED_TULIP); a.s(0, 1, -1, Blocks.POTTED_AZURE_BLUET);
        for (int x = 7; x <= 12; x++) a.s(x, -1, -1, Blocks.DIRT_PATH);
        for (int z = -4; z <= -2; z++) a.s(9, -1, z, Blocks.DIRT_PATH);
        a.people(16, 5, PeopleEntity.ELDER, 1);
        a.people(6, 5, PeopleEntity.CIVIL, 0);
        a.people(10, 7, PeopleEntity.CIVIL, 2);
        a.people(11, -3, PeopleEntity.CIVIL, 1);
    }
}
