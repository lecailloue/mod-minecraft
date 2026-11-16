package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan de Minas Tirith : une cite de sept niveaux adossee a une montagne (le Mindolluin), coupee en deux par l'eperon de roche qui porte la
 * citadelle, la tour d'Ecthelion et la cour de l'Arbre Blanc. Grille de blocs abstraits, sans dependance a Minecraft.
 * Repere local : x de 0 a W - 1, z de 0 a D - 1 (la montagne est au nord, z petit ; la cite regarde le sud) ; y = 0 : premier niveau d'air au-dessus du sol.
 */
public final class TirithPlan {
    public static final int W = 320, D = 270, OFF = 8, TOP = 280, H = OFF + TOP + 1, CX = 160, CZ = 130;
    public static final double[] R = {125, 107, 89, 71, 53, 35, 17};              // rayon exterieur de chaque niveau (largeur 18)
    public static final int[] FL = {18, 36, 54, 72, 90, 108, 126};                // niveau debout de chaque terrasse
    public static final int PROW_TOP = 126, PROW_TIP = 100;                         // sommet de l'eperon, longueur de l'eperon vers le sud

    public static final int AIR = 0, STONE = 1, ANDESITE = 2, TUFF = 3, CALCITE = 4, DIORITE = 5, GRASS = 6, DIRT = 7, SNOW = 8, COBBLE = 9, GRAVEL = 10,
            WB = 20, WB_STAIR = 24, WB_SLAB = 32, WB_SLAB_TOP = 33, WB_WALL = 34, QB = 35, SQ = 36, PD = 37, SBR = 38, QPILLAR_Y = 40, CHISEL = 41, PAVE = 42, PAVE2 = 43,
            GLASS = 50, BARS = 51, DPLANKS = 52, DLOG_Y = 53, LANTERN = 54, CHAIN = 55, FENCE_B = 56, WATER = 57, BIRCH_Y = 58, BIRCH_FENCE = 59,
            SLATE = 60, SLATE_STAIR = 64, SLATE_SLAB = 72, SLATE_SLAB_TOP = 73, COPPER = 74, COPPER_STAIR = 80, COPPER_SLAB = 88, COPPER_SLAB_TOP = 89,
            LEAF = 90, FLOWER1 = 91, FLOWER2 = 92, GRASS_S = 93, DWALL = 94, BANNER_G = 95, WOOL_W = 96, WOOL_G = 97, TORCH_B = 98, LAMPBLOCK = 99, SLAB_STONE = 100,
            QUARTZ_SLAB = 101, QUARTZ_SLAB_TOP = 102, GOLD = 103, DOOR_PL = 104, HLANTERN = 105, KEEL = 111, YB = 44;
    public static final int N = 0, E = 1, S = 2, W_ = 3;

    private final byte[] g = new byte[W * H * D];
    private final int[] fill = new int[W * D];                                   // y de la derniere cellule pleine de la colonne (-99 = rien)
    private final byte[] kind = new byte[W * D];                                 // 0 rien, 1 terrasse, 2 montagne, 3 eperon, 4 rampe, 5 contrefort
    private final byte[] ring = new byte[W * D];                                 // niveau de la terrasse (-1 hors terrasses)
    private final int[] top = new int[W * D];
    private final boolean[] occ = new boolean[W * D];                            // emprise prise par un batiment
    public int houses, towers;
    public int tower_x, tower_z, court_x, court_z, hall_x, hall_z, gate_x, gate_z;

    private static TirithPlan inst;
    public static synchronized TirithPlan get() { if (inst == null) inst = new TirithPlan(); return inst; }

    // ------------------------------------------------------------------ outils
    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public byte get(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    private void setIfAir(int x, int y, int z, int id) { if (ok(x, y, z) && g[idx(x, y, z)] == AIR) g[idx(x, y, z)] = (byte) id; }
    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 362437;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
    private static double rnd(int x, int y, int z) { return ((hash(x, y, z) >>> 5) & 1023) / 1024.0; }
    private static double vn(double x, double z, int seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = rnd(x0, seed, z0), b = rnd(x0 + 1, seed, z0), c = rnd(x0, seed, z0 + 1), d = rnd(x0 + 1, seed, z0 + 1);
        return ((a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz) * 2 - 1;
    }
    private static double smooth(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
    private void box(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, id);
    }
    private static int stair(int base, int dir, boolean topHalf) { return base + dir * 2 + (topHalf ? 1 : 0); }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int top(int x, int z) { return top[z * W + x]; }
    public int fillAt(int x, int z) { return fill[z * W + x]; }
    public int kindAt(int x, int z) { return kind[z * W + x]; }

    // ------------------------------------------------------------------ construction
    public TirithPlan() {
        java.util.Arrays.fill(fill, -99);
        java.util.Arrays.fill(ring, (byte) -1);
        columns();
        materialize();
        ramps();
        keelTunnels();
        parapets();
        terraces();
        citadel();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    private static double cliffZ(int x) { return CZ - 3 + 6 * vn(x / 20.0, 0.5, 1); }

    private double mountain(int x, int z) {
        double b = cliffZ(x) - (z + 0.5);
        if (b < 0) return -1;
        double lat = Math.abs(x + 0.5 - CX);
        double lx = 1 - smooth((lat - 90) / 70);
        double base = 128 + 150 * smooth(b / 115);
        double rg = vn(x / 40.0, z / 40.0, 2) * 30 + vn(x / 16.0, z / 16.0, 3) * 12 + (1 - Math.abs(vn(x / 55.0, z / 55.0, 4))) * 35 * smooth(b / 40);
        double taper = 1 - 0.5 * smooth((b - 100) / 30);
        double h = (base + rg) * taper * lx * (0.35 + 0.65 * smooth((z + 0.5) / 80.0));
        return h < 3 ? -1 : h;
    }

    /** la quille : plateau jusqu'a 26 blocs du centre, puis elle redescend vers la proue */
    private static int prowTop(int z) {
        double dz = z + 0.5 - CZ;
        if (dz <= 26) return PROW_TOP - 1;
        return (int) Math.max(FL[1] + 3, PROW_TOP - 1 - (dz - 26) * 1.15);
    }

    private static boolean inProw(int x, int z) {
        double dz = z + 0.5 - CZ;
        if (dz < -14 || dz > PROW_TIP + 1) return false;
        double hw = dz < 0 ? 21 : 21 - 13 * dz / PROW_TIP;
        if (dz > PROW_TIP - 9) hw *= Math.sqrt(Math.max(0, 1 - Math.pow((dz - (PROW_TIP - 9)) / 10.0, 2)));
        hw += 1.6 * vn(z / 8.0, 3.3, 5);
        return Math.abs(x + 0.5 - CX) <= hw;
    }

    /** rampes : {x central, demi-largeur, z de depart (le pied, au sud), niveau du bas, niveau du haut} ; elles montent vers le nord, une marche par bloc */
    public final List<int[]> ramps = new ArrayList<>();

    private void columns() {
        ramps.add(new int[]{CX, 5, CZ + (int) R[0], 0, FL[0]});
        double[][] rp = {{46, 1}, {-40, 2}, {32, 3}, {-30, 4}, {27, 5}};
        for (int k = 0; k < 5; k++) {
            int xr = CX + (int) rp[k][0];
            double dx = rp[k][0];
            int z0 = CZ + (int) Math.round(Math.sqrt(R[k + 1] * R[k + 1] - dx * dx));
            ramps.add(new int[]{xr, 3, z0, FL[k], FL[k + 1]});
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x;
            double dx = x + 0.5 - CX, dz = z + 0.5 - CZ, r = Math.hypot(dx, dz);
            double m = mountain(x, z);
            if (m > 0) { fill[i] = (int) m; kind[i] = 2; continue; }
            if (inProw(x, z)) { fill[i] = prowTop(z); kind[i] = 3; continue; }
            if (r <= R[0] && z + 0.5 >= cliffZ(x)) {
                int k = Math.min(6, (int) ((R[0] - r) / 18));
                fill[i] = FL[k] - 1; kind[i] = 1; ring[i] = (byte) k; continue;
            }
            double lat = Math.abs(dx);
            if (lat > 100 && z + 0.5 >= cliffZ(x)) {
                double fh = 70 * smooth((lat - 100) / 25) * (1 - smooth((lat - 135) / 25)) * (1 - smooth((z + 0.5 - cliffZ(x)) / 75)) + 6 * vn(x / 12.0, z / 12.0, 6) * smooth((lat - 100) / 25);
                if (fh > 5) { fill[i] = (int) fh; kind[i] = 5; }
            }
        }
        // rampe du flanc de l'eperon (niveau 5 -> 6), au bord est de l'eperon
        for (int z = CZ + 7; z <= CZ + 25; z++) {
            double dz = z + 0.5 - CZ, hw = 21 - 13 * dz / PROW_TIP;
            for (int x = CX + (int) hw; x <= CX + (int) hw + 4; x++) {
                int i = z * W + x;
                if (kind[i] == 3) continue;
                fill[i] = FL[5] - 1 + (int) Math.round(18 - (z - (CZ + 7))); kind[i] = 4;
                if (fill[i] > PROW_TOP - 1) fill[i] = PROW_TOP - 1;
            }
        }
        // rampes des murs
        for (int[] rp2 : ramps) {
            int run = rp2[4] - rp2[3];
            for (int z = rp2[2] - run; z <= rp2[2] + (rp2[3] == 0 ? 8 : 0); z++) for (int x = rp2[0] - rp2[1]; x <= rp2[0] + rp2[1]; x++) {
                if (!area(x, z)) continue;
                int i = z * W + x;
                if (kind[i] == 2 || kind[i] == 3) continue;
                int h = z > rp2[2] ? rp2[3] - 1 : rp2[3] - 1 + (rp2[2] - z);
                if (z <= rp2[2] - run) h = rp2[4] - 1;
                if (h > rp2[4] - 1) h = rp2[4] - 1;
                fill[i] = h; kind[i] = 4;
            }
        }
    }


    /** chaque rue des niveaux 2 a 5 traverse l'eperon par un tunnel voute */
    private void keelTunnels() {
        for (int k = 1; k <= 4; k++) {
            int b = FL[k];
            for (int x = CX - 26; x <= CX + 26; x++) for (int z = CZ + 10; z <= CZ + 110; z++) {
                if (!area(x, z) || kind[z * W + x] != 3) continue;
                double r = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ), rel = R[k] - r;
                if (rel < 7.0 || rel > 13.0) continue;
                int f = fill[z * W + x];
                int h = Math.min(5, f - b - 2);
                if (h < 3) continue;
                boolean inside = rel >= 8.0 && rel <= 12.0;
                for (int y = b; y <= b + h; y++) set(x, y, z, inside ? (y == b + h ? WB : AIR) : WB);
                set(x, b - 1, z, inside ? PAVE : WB);
                if (inside && (x + z) % 9 == 0) set(x, b + h - 1, z, HLANTERN);
                if (inside && (x + z) % 9 == 0) set(x, b + h, z, CHAIN);
            }
        }
    }

    private int nbMin(int x, int z, int d) {
        int mn = fill[z * W + x];
        int[][] o = {{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
        for (int[] q : o) {
            int xx = x + q[0], zz = z + q[1];
            int f = area(xx, zz) ? fill[zz * W + xx] : -2;
            if (f == -99) f = -2;
            mn = Math.min(mn, f);
        }
        return mn;
    }

    private void materialize() {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x, f = fill[i];
            if (f == -99) continue;
            int mn = nbMin(x, z, 4), mn1 = nbMin(x, z, 1);
            int k = kind[i];
            boolean rock = k == 2 || k == 5, keel = k == 3;
            for (int y = -6; y <= f; y++) {
                boolean solid = y <= 2 || y >= f - 3 || y > mn;
                if (!solid) continue;
                boolean exposed = y > mn1 && y < f;
                int id;
                if (keel) id = y == f ? WB : (rnd(x, y, z) < 0.12 ? DIORITE : KEEL);
                else if (rock) id = rockBlock(x, y, z, f, k, y == f || y > mn1);
                else if (y == f) id = paving(x, z);
                else if (exposed || y > mn) id = wallBlock(x, y, z);
                else id = STONE;
                set(x, y, z, id);
            }
        }
    }

    private int rockBlock(int x, int y, int z, int f, int k, boolean surface) {
        double n = vn(x / 7.0 + y / 11.0, z / 7.0 - y / 9.0, 7), q = rnd(x, y, z);
        if (k == 3) {                                                            // l'eperon : roche claire
            if (n > 0.35) return CALCITE;
            return q < 0.35 ? CALCITE : q < 0.65 ? DIORITE : q < 0.88 ? STONE : ANDESITE;
        }
        if (surface && y >= 258 + 14 * vn(x / 20.0, z / 20.0, 8)) return SNOW;
        if (surface && f < 80 && y == f) return k == 5 ? GRASS : (q < 0.5 ? GRASS : STONE);
        if (n > 0.55) return CALCITE;
        if (n < -0.5) return TUFF;
        return q < 0.5 ? STONE : q < 0.85 ? ANDESITE : q < 0.93 ? TUFF : DIORITE;
    }

    private int wallBlock(int x, int y, int z) {
        if (y % 7 == 0) return YB;
        double q = rnd(x, y, z);
        return q < 0.04 ? CALCITE : q < 0.08 ? PD : WB;
    }

    private int paving(int x, int z) {
        double q = rnd(x, 3, z);
        boolean line = (x % 6 == 0) || (z % 6 == 0);
        return line ? PAVE2 : (q < 0.1 ? PD : PAVE);
    }

    // ------------------------------------------------------------------ rampes : escaliers de pierre blanche
    private void ramps() {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x;
            if (kind[i] != 4) continue;
            int f = fill[i];
            int below = area(x, z + 1) ? fill[(z + 1) * W + x] : f;
            boolean slope = below != f || f < PROW_TOP - 1;
            boolean step = false;
            for (int[] r2 : ramps) if (x >= r2[0] - r2[1] && x <= r2[0] + r2[1] && z >= r2[2] - (r2[4] - r2[3]) && z <= r2[2]) step = true;
            if (z >= CZ + 7 && z <= CZ + 25) step = true;
            if (step && f < PROW_TOP - 1 && f > -1) set(x, f, z, stair(WB_STAIR, N, false));
            else set(x, f, z, WB);
        }
    }

    // ------------------------------------------------------------------ parapets crenelés au bord des terrasses
    private void parapets() {
        for (int x = 1; x < W - 1; x++) for (int z = 1; z < D - 1; z++) {
            int i = z * W + x;
            if (kind[i] != 1 && kind[i] != 4) continue;
            int f = fill[i];
            if (get(x, f, z) == (byte) stair(WB_STAIR, N, false)) continue;
            int lo = Math.min(Math.min(fill[i + 1] == -99 ? -2 : fill[i + 1], fill[i - 1] == -99 ? -2 : fill[i - 1]), Math.min(fill[i + W] == -99 ? -2 : fill[i + W], fill[i - W] == -99 ? -2 : fill[i - W]));
            if (lo >= f - 2) continue;                                       // pas un bord
            if (kind[i] == 4 && f < 0) continue;
            boolean merlon = ((x + z) / 2) % 2 == 0;
            set(x, f + 1, z, WB);
            if (merlon) set(x, f + 2, z, WB); else set(x, f + 2, z, WB_SLAB);
            if (merlon && (x + z) % 12 == 0) set(x, f + 3, z, WB_SLAB);
        }
    }

    // ------------------------------------------------------------------ batiments
    private boolean freeCell(int x, int z, int k) {
        if (!area(x, z)) return false;
        int i = z * W + x;
        return kind[i] == 1 && ring[i] == k && fill[i] == FL[k] - 1 && !occ[i];
    }

    private boolean nearRamp(int x, int z) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int xx = x + dx, zz = z + dz;
            if (area(xx, zz) && kind[zz * W + xx] == 4) return true;
        }
        return false;
    }

    private void terraces() {
        gates();
        for (int k = 0; k < 7; k++) houses(k);
        for (int k = 0; k < 7; k++) streetLife(k);
    }

    /** tours de garde de chaque porte : deux tours de part et d'autre de la rampe et un linteau qui l'enjambe */
    private void gates() {
        for (int n = 0; n < ramps.size(); n++) {
            int[] r = ramps.get(n);
            int xr = r[0], hw = r[1], z0 = r[2], lo = r[3], hi = r[4];
            boolean grand = n == 0;
            int sz = grand ? 11 : 7, th = grand ? 30 : 15, base = hi;
            int zc = z0 - (grand ? 9 : 5);
            for (int sd = -1; sd <= 1; sd += 2) {
                int cx0 = xr + sd * (hw + 2 + sz / 2 + 1);
                int x0 = cx0 - sz / 2, z1 = zc - sz / 2;
                boolean can = true;
                for (int x = x0; x < x0 + sz && can; x++) for (int z = z1; z < z1 + sz; z++) {
                    if (!area(x, z) || kind[z * W + x] != 1 || fill[z * W + x] != base - 1 || occ[z * W + x]) { can = false; break; }
                }
                if (!can) continue;
                gateTower(x0, z1, sz, base, th);
            }
            // linteau
            int zb = zc;
            boolean okL = true;
            for (int x = xr - hw - 1; x <= xr + hw + 1; x++) if (!area(x, zb)) okL = false;
            if (okL) {
                int yb = base + (grand ? 12 : 8);
                for (int x = xr - hw - 1; x <= xr + hw + 1; x++) for (int z = zb - 1; z <= zb + 1; z++) {
                    set(x, yb, z, WB); set(x, yb + 1, z, WB);
                    if (z == zb - 1 || z == zb + 1) set(x, yb + 2, z, (x + z) % 2 == 0 ? WB : AIR);
                }
                for (int x = xr - hw; x <= xr + hw; x++) {
                    int dxm = x - xr;
                    int drop = (Math.abs(dxm) >= hw - 1) ? 2 : (Math.abs(dxm) >= hw - 3 ? 1 : 0);
                    for (int d = 1; d <= drop; d++) set(x, yb - d, zb, WB);
                }
                set(xr, yb - 2, zb, GOLD);
                for (int x : new int[]{xr - 2, xr + 2}) { set(x, yb - 1, zb, CHAIN); set(x, yb - 2, zb, HLANTERN); }
                for (int x = xr - hw + 1; x <= xr + hw - 1; x += 2) { set(x, yb - 1, zb - 1, WOOL_W); set(x, yb - 2, zb - 1, WOOL_W); set(x, yb - 3, zb - 1, WOOL_G); }
            }
        }
    }

    private void gateTower(int x0, int z0, int sz, int base, int h) {
        for (int x = x0; x < x0 + sz; x++) for (int z = z0; z < z0 + sz; z++) {
            occ[z * W + x] = true;
            boolean edge = x == x0 || x == x0 + sz - 1 || z == z0 || z == z0 + sz - 1;
            boolean corner = (x == x0 || x == x0 + sz - 1) && (z == z0 || z == z0 + sz - 1);
            for (int y = 0; y <= h; y++) {
                int id = edge ? (corner ? QPILLAR_Y : (y % 10 == 9 ? QB : WB)) : (y == 0 ? WB : AIR);
                if (edge && !corner && y % 8 >= 4 && y % 8 <= 5 && y > 3 && y < h - 3 && ((x - x0 + z - z0) % 4 == 2)) id = BARS;
                if (id != AIR) set(x, base + y, z, id);
            }
            if (!edge) continue;
            int mer = ((x + z) % 2 == 0) ? 1 : 0;
            set(x, base + h + 1, z, WB); if (mer == 1) set(x, base + h + 2, z, WB);
        }
        for (int x = x0 + 1; x < x0 + sz - 1; x++) for (int z = z0 + 1; z < z0 + sz - 1; z++) {
            set(x, base + h, z, WB);
            boolean mid = Math.abs(x - (x0 + sz / 2)) <= 1 && Math.abs(z - (z0 + sz / 2)) <= 1;
            if (mid) { set(x, base + h + 1, z, WB); set(x, base + h + 2, z, WB); }
        }
        int cxp = x0 + sz / 2, czp = z0 + sz / 2;
        set(cxp, base + h + 3, czp, FENCE_B); set(cxp, base + h + 4, czp, FENCE_B); set(cxp, base + h + 5, czp, LANTERN);
        for (int x = x0 + 1; x < x0 + sz - 1; x++) for (int z = z0; z <= z0 + sz - 1; z += sz - 1) if (rnd(x, z, 77) < 0.6) { set(x, base + h - 2, z + (z == z0 ? -1 : 1), WOOL_W); set(x, base + h - 3, z + (z == z0 ? -1 : 1), WOOL_W); }
    }

    // -------- maisons
    private void houses(int k) {
        double ro = R[k];
        for (int band = 0; band < 2; band++) {
            double depth = band == 0 ? 6 : 5.4, rc = band == 0 ? ro - 5 : ro - 14.8;
            boolean faceIn = band == 0;
            double th = 0.02;
            int guard = 0;
            while (th < Math.PI - 0.02 && guard++ < 400) {
                double w = 5 + (int) (rnd(k, band * 977 + guard, 11) * 4);
                double arc = w / rc;
                double mid = th + arc / 2;
                double hx = CX + Math.cos(mid) * rc, hz = CZ + Math.sin(mid) * rc;
                if (!houseAt(k, hx, hz, mid, w, depth, faceIn, guard + band * 500)) { th += 2.5 / rc; continue; }
                th += arc + (rnd(k, guard, 13) < 0.18 ? 3.5 : 1.1) / rc;
            }
        }
    }

    private boolean houseAt(int k, double hx, double hz, double ang, double w, double d, boolean faceIn, int seed) {
        double tx = -Math.sin(ang), tz = Math.cos(ang), rx = Math.cos(ang), rz = Math.sin(ang);
        int rad = (int) Math.ceil(Math.hypot(w, d) / 2 + 2);
        int ix = (int) Math.round(hx), iz = (int) Math.round(hz);
        List<int[]> cells = new ArrayList<>();
        for (int x = ix - rad; x <= ix + rad; x++) for (int z = iz - rad; z <= iz + rad; z++) {
            double ax = x + 0.5 - hx, az = z + 0.5 - hz;
            double a = ax * tx + az * tz, b = ax * rx + az * rz;
            if (Math.abs(a) <= w / 2 && Math.abs(b) <= d / 2) {
                if (!freeCell(x, z, k) || nearRamp(x, z)) return false;
                cells.add(new int[]{x, z});
            }
        }
        if (cells.size() < 16) return false;
        int base = FL[k];
        int hh = 5 + (int) (rnd(ix, iz, seed) * 4) + (k >= 4 ? 1 : 0);
        int roofKind = rnd(iz, ix, 5) < 0.35 ? 1 : 0;
        int wallMat = rnd(ix, iz, 6) < 0.38 ? YB : WB, bandMat = wallMat == WB ? YB : WB;
        for (int[] c : cells) {
            int x = c[0], z = c[1];
            double ax = x + 0.5 - hx, az = z + 0.5 - hz;
            double a = ax * tx + az * tz, b = ax * rx + az * rz;
            double da = w / 2 - Math.abs(a), db = d / 2 - Math.abs(b);
            boolean edge = false;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                double bx = x + o[0] + 0.5 - hx, bz = z + o[1] + 0.5 - hz;
                double a2 = bx * tx + bz * tz, b2 = bx * rx + bz * rz;
                if (Math.abs(a2) > w / 2 || Math.abs(b2) > d / 2) edge = true;
            }
            occ[z * W + x] = true;
            boolean doorFace = faceIn ? b < -d / 2 + 1.1 : b > d / 2 - 1.1;
            boolean door = edge && doorFace && Math.abs(a) < 0.9;
            for (int y = 0; y <= hh; y++) {
                int id;
                if (!edge) id = y == 0 ? WB : AIR;
                else if (door && y >= 1 && y <= 2) id = AIR;
                else if (door && y == 3) id = DPLANKS;
                else if (y % 4 == 0 && y > 0) id = bandMat;
                else if (y >= 2 && y <= 3 && ((x * 3 + z * 5) % 3 == 0) && !doorFace) id = GLASS;
                else if (y >= 2 && y <= 3 && doorFace && Math.abs(a) > 1.5 && ((int) Math.floor(a * 2)) % 3 == 0) id = GLASS;
                else id = wallMat;
                if (id != AIR) set(x, base + y, z, id);
            }
            if (door) set(x, base, z, WB);
            int rh = (int) Math.floor(Math.min(da, db * 1.25));
            int top = Math.min(rh, 3);
            for (int j = 0; j <= top; j++) {
                int id = roofKind == 1 ? COPPER : SLATE;
                if (j == top && rh > 3) id = roofKind == 1 ? COPPER_SLAB : SLATE_SLAB;
                set(x, base + hh + 1 + j, z, id);
            }
            if (edge && !door && ((x + z) % 7 == 3) && rnd(x, z, 9) < 0.5) { set(x, base + hh + 5, z, FENCE_B); }
        }
        return true;
    }

    private void streetLife(int k) {
        double ro = R[k];
        for (double th = 0.1; th < Math.PI - 0.1; th += 13.0 / (ro - 10)) {
            for (int side = 0; side < 2; side++) {
                double rr = ro - (side == 0 ? 8.4 : 11.6);
                int x = (int) Math.round(CX + Math.cos(th) * rr), z = (int) Math.round(CZ + Math.sin(th) * rr);
                if (!freeCell(x, z, k) || nearRamp(x, z)) continue;
                if (side == 1 && ((int) (th * 7)) % 2 == 0) continue;
                occ[z * W + x] = true;
                int b = FL[k];
                set(x, b, z, FENCE_B); set(x, b + 1, z, FENCE_B); set(x, b + 2, z, FENCE_B); set(x, b + 3, z, LANTERN);
            }
        }
        // buissons, parterres et petits arbres dans les ruelles
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            if (!freeCell(x, z, k) || nearRamp(x, z)) continue;
            double rr = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ);
            boolean inBand = rr > ro - 7.9 && rr < ro - 2.2 || rr > ro - 17.4 && rr < ro - 12.1;
            if (!inBand) continue;
            double q = rnd(x, z, 41);
            int b = FL[k];
            if (q < 0.045) { set(x, b, z, LEAF); if (rnd(x, z, 42) < 0.5) set(x, b + 1, z, LEAF); else set(x, b + 1, z, FLOWER1); occ[z * W + x] = true; }
            else if (q < 0.07) { set(x, b, z, rnd(x, z, 43) < 0.5 ? FLOWER1 : FLOWER2); occ[z * W + x] = true; }
        }
    }

    // ------------------------------------------------------------------ citadelle, tour d'Ecthelion, cour de l'Arbre Blanc
    private void citadel() {
        int cx = CX, cz = CZ, y0 = PROW_TOP;
        tower_x = cx; tower_z = cz - 5; court_x = cx; court_z = cz + 14;
        // parapet autour du plateau
        for (int x = cx - 24; x <= cx + 24; x++) for (int z = cz - 18; z <= cz + 28; z++) {
            if (!area(x, z) || kind[z * W + x] != 3 || fill[z * W + x] != PROW_TOP - 1) continue;
            boolean edge = false;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int xx = x + o[0], zz = z + o[1];
                if (!area(xx, zz) || kind[zz * W + xx] != 3 || fill[zz * W + xx] < PROW_TOP - 2) edge = true;
            }
            if (!edge) continue;
            if (z < cz - 12) continue;
            set(x, y0, z, WB);
            if ((x + z) % 2 == 0) set(x, y0 + 1, z, WB); else set(x, y0 + 1, z, WB_SLAB);
        }
        // sol de la cour : motif de dalles
        for (int x = cx - 21; x <= cx + 21; x++) for (int z = cz - 14; z <= cz + 26; z++) {
            if (!area(x, z) || kind[z * W + x] != 3 || fill[z * W + x] != PROW_TOP - 1) continue;
            int dx = x - cx;
            boolean border = Math.abs(dx) % 4 == 0 && Math.abs(dx) < 12;
            set(x, y0 - 1, z, ((x + z) % 2 == 0 && (Math.abs(dx) < 12)) ? SQ : (border ? PD : PAVE));
        }
        tower(cx, cz - 5, y0);
        wings(cx, cz, y0);
        courtGate(cx, cz + 26, y0);
        court(cx, cz + 14, y0);
        occ[0] = false;
    }

    private void tower(int cx, int cz, int y0) {
        int[] half = {8, 6, 4, 3};
        int[] top = {36, 70, 94, 106};
        int prev = 0;
        for (int s = 0; s < 4; s++) {
            int h = half[s];
            for (int y = prev; y <= top[s]; y++) for (int x = cx - h; x <= cx + h; x++) for (int z = cz - h; z <= cz + h; z++) {
                boolean edge = Math.abs(x - cx) == h || Math.abs(z - cz) == h;
                boolean corner = Math.abs(x - cx) == h && Math.abs(z - cz) == h;
                boolean nearCorner = (Math.abs(x - cx) >= h - 1 && Math.abs(z - cz) >= h - 1) && !corner;
                int id = WB;
                if (edge) {
                    if (corner) id = QPILLAR_Y;
                    else if (y == top[s] - 1 || y == prev + 1 || (y - prev) % 12 == 11) id = YB;
                    else if (y > prev + 4 && y < top[s] - 3 && (y - prev) % 12 >= 5 && (y - prev) % 12 <= 8 && ((Math.abs(x - cx) + Math.abs(z - cz)) % 4 == 1) && !nearCorner) id = BARS;
                } else if (s == 3 && y > top[2] + 2) { id = AIR; }
                if (s == 3 && y > top[2] + 1 && !edge) id = AIR;
                set(x, y0 + y, z, id);
            }
            // chaperon crenele
            for (int x = cx - h - 1; x <= cx + h + 1; x++) for (int z = cz - h - 1; z <= cz + h + 1; z++) {
                boolean ed = Math.abs(x - cx) == h + 1 || Math.abs(z - cz) == h + 1;
                if (!ed) continue;
                int yy = top[s] + 1;
                set(x, y0 + yy, z, QB);
                if (s < 3 && (x + z) % 2 == 0) set(x, y0 + yy + 1, z, QB);
            }
            prev = top[s] + 1;
        }
        // lanterne sommitale : piliers, toit, fleche de cuivre
        int yb = y0 + top[3] + 2;
        for (int y = 0; y < 8; y++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
            if (corner) set(cx + dx, yb + y, cz + dz, QPILLAR_Y);
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            set(cx + dx, yb + 8, cz + dz, QB);
        }
        set(cx, yb, cz, LANTERN);
        for (int j = 0; j < 20; j++) {
            int hr = Math.max(0, 3 - j / 2);
            if (hr <= 0) { set(cx, yb + 9 + j, cz, j > 14 ? FENCE_B : SLATE); continue; }
            for (int dx = -hr; dx <= hr; dx++) for (int dz = -hr; dz <= hr; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == hr) set(cx + dx, yb + 9 + j, cz + dz, SLATE);
            }
        }
        set(cx, yb + 30, cz, GOLD); set(cx, yb + 31, cz, LANTERN);
        // contreforts au pied de la tour, sur la face sud
        for (int x = cx - 5; x <= cx + 5; x++) for (int y = 0; y < 14; y++) {
            int zz = cz + 9;
            if (Math.abs(x - cx) == 5 || Math.abs(x - cx) == 0) set(x, y0 + y, zz, y < 10 ? WB : QB);
        }
        // grande porte de la tour, cote cour
        for (int y = 0; y < 5; y++) for (int dx = -1; dx <= 1; dx++) set(cx + dx, y0 + y, cz + 8, AIR);
        for (int dx = -2; dx <= 2; dx++) set(cx + dx, y0 + 5, cz + 8, QB);
    }

    private void wings(int cx, int cz, int y0) {
        for (int sd = -1; sd <= 1; sd += 2) {
            int xa = sd < 0 ? cx - 21 : cx + 12, xb = sd < 0 ? cx - 12 : cx + 21;
            int za = cz - 3, zb = cz + 17, hh = 20;
            for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) {
                boolean edge = x == xa || x == xb || z == za || z == zb;
                boolean inner = sd < 0 ? x == xb : x == xa;
                for (int y = 0; y <= hh; y++) {
                    int id = WB;
                    if (!edge) id = AIR;
                    else if (y % 7 == 6) id = QB;
                    else if (y >= 3 && y <= 5 && (z % 3 == 1) && inner) id = GLASS;
                    else if (y >= 11 && y <= 13 && (z % 3 == 1) && inner) id = GLASS;
                    else if (y >= 3 && y <= 4 && x % 3 == 0 && !inner && !(z == za)) id = BARS;
                    if ((x == xa || x == xb) && (z == za || z == zb)) id = QPILLAR_Y;
                    if (id != AIR) set(x, y0 + y, z, id);
                }
                // toit en pavillon : pente nord-sud
                double dd = Math.min(Math.min(x - xa, xb - x), Math.min(z - za, zb - z) * 0.5);
                int rh = (int) Math.floor(dd);
                for (int j = 0; j <= Math.min(rh, 4); j++) set(x, y0 + hh + 1 + j, z, j == Math.min(rh, 4) && rh > 4 ? SLATE_SLAB : SLATE);
            }
            // porche a colonnes sur le cote cour
            int xc = sd < 0 ? xb + 1 : xa - 1;
            for (int z = za + 2; z <= zb - 2; z += 4) for (int y = 0; y < 7; y++) set(xc, y0 + y, z, QPILLAR_Y);
            for (int z = za + 2; z <= zb - 2; z++) set(xc, y0 + 7, z, QB);
            // porte
            int xd = sd < 0 ? xb : xa;
            for (int y = 0; y < 3; y++) for (int dz = -1; dz <= 0; dz++) set(xd, y0 + y, cz + 9 + dz, AIR);
        }
    }

    private void courtGate(int cx, int z, int y0) {
        for (int x = cx - 21; x <= cx + 21; x++) {
            if (!area(x, z) || kind[z * W + x] != 3) continue;
            int ax = Math.abs(x - cx);
            if (ax <= 3) { if (ax == 3) for (int y = 0; y <= 11; y++) set(x, y0 + y, z, y % 6 == 5 ? QB : QPILLAR_Y); continue; }
            for (int y = 0; y <= 6; y++) set(x, y0 + y, z, y % 6 == 5 ? QB : WB);
            if ((x + z) % 2 == 0) set(x, y0 + 7, z, WB); else set(x, y0 + 7, z, WB_SLAB);
        }
        for (int x = cx - 3; x <= cx + 3; x++) { set(x, y0 + 12, z, QB); set(x, y0 + 11, z, QB); }
        for (int x = cx - 2; x <= cx + 2; x++) set(x, y0 + 10, z, Math.abs(x - cx) == 2 ? QB : AIR);
        set(cx, y0 + 13, z, GOLD);
        for (int x : new int[]{cx - 2, cx + 2}) { set(x, y0 + 9, z, CHAIN); set(x, y0 + 8, z, HLANTERN); }
    }

    private void court(int cx, int cz, int y0) {
        hall_x = cx; hall_z = cz;
        // l'Arbre Blanc, dans un parterre souleve, au nord de la cour
        int tx = cx, tz = cz - 6;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            set(tx + dx, y0, tz + dz, rim ? QB : GRASS);
            if (!rim) { set(tx + dx, y0 - 1, tz + dz, DIRT); if (rnd(tx + dx, 5, tz + dz) < 0.25) set(tx + dx, y0 + 1, tz + dz, rnd(dx, 6, dz) < 0.5 ? FLOWER1 : FLOWER2); }
        }
        for (int y = 1; y <= 9; y++) set(tx, y0 + y, tz, BIRCH_Y);
        set(tx + 1, y0 + 1, tz, BIRCH_Y); set(tx - 1, y0 + 1, tz, BIRCH_Y);
        int[][] br = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] b : br) for (int j = 1; j <= 5; j++) set(tx + b[0] * j, y0 + 6 + j / 2 + 1, tz + b[1] * j, BIRCH_Y);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) for (int dy = 6; dy <= 16; dy++) {
            double e = Math.pow(dx / 7.0, 2) + Math.pow(dz / 7.0, 2) + Math.pow((dy - 11) / 5.5, 2);
            if (e < 1 && (e < 0.7 || rnd(tx + dx, dy, tz + dz) < 0.7)) setIfAir(tx + dx, y0 + dy, tz + dz, LEAF);
        }
        // fontaine : bassin carre, pilier central
        int fx = cx, fz = cz + 6;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
            if (rim) { set(fx + dx, y0, fz + dz, QB); set(fx + dx, y0 - 1, fz + dz, QB); if ((Math.abs(dx) == 4 && Math.abs(dz) == 4)) set(fx + dx, y0 + 1, fz + dz, QUARTZ_SLAB); }
            else { set(fx + dx, y0 - 1, fz + dz, WATER); set(fx + dx, y0 - 2, fz + dz, SQ); set(fx + dx, y0, fz + dz, WATER); }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(fx + dx, y0, fz + dz, QB);
        for (int y = 1; y <= 4; y++) set(fx, y0 + y, fz, QPILLAR_Y);
        set(fx, y0 + 5, fz, WATER);
        for (int[] b : br) { set(fx + b[0], y0 + 5, fz + b[1], QB); set(fx + b[0], y0 + 6, fz + b[1], WATER); }
        set(fx, y0 + 6, fz, WATER);
        // bancs : face a la cour, dossier vers l'exterieur
        for (int z = cz - 8; z <= cz + 20; z += 4) {
            if (Math.abs(z - fz) <= 5 && false) continue;
            for (int sd = -1; sd <= 1; sd += 2) {
                int x = cx + sd * 10;
                set(x, y0, z, stair(WB_STAIR, sd < 0 ? W_ : E, false)); set(x, y0, z + 1, stair(WB_STAIR, sd < 0 ? W_ : E, false));
                set(x + sd, y0, z, WB_WALL); set(x + sd, y0, z + 1, WB_WALL);
            }
        }
        // bancs autour de la fontaine
        for (int dz : new int[]{-6, 6}) for (int dx = -1; dx <= 1; dx++) {
            set(fx + dx, y0, fz + dz, stair(WB_STAIR, dz < 0 ? N : S, false));
        }
        for (int dx : new int[]{-6, 6}) for (int dz = -1; dz <= 1; dz++) {
            set(fx + dx, y0, fz + dz, stair(WB_STAIR, dx < 0 ? W_ : E, false));
        }
        // buissons et parterres le long des ailes
        for (int sd = -1; sd <= 1; sd += 2) for (int z = cz - 7; z <= cz + 21; z += 4) {
            int x = cx + sd * 8;
            if (z >= fz - 5 && z <= fz + 5 && false) continue;
            set(x, y0, z, LEAF); set(x, y0, z + 1, LEAF); set(x, y0 + 1, z, LEAF);
            set(x + sd, y0 - 1, z + 1, DIRT); set(x + sd, y0, z + 1, rnd(x, z, 3) < 0.5 ? FLOWER1 : FLOWER2);
        }
        // lampadaires
        for (int dz : new int[]{-8, 2, 12, 22}) for (int sd = -1; sd <= 1; sd += 2) {
            int x = cx + sd * 5, z = cz + dz;
            if (Math.abs(x - fx) <= 4 && Math.abs(z - fz) <= 4) continue;
            for (int y = 0; y < 4; y++) set(x, y0 + y, z, WB_WALL);
            set(x, y0 + 4, z, LANTERN);
        }
    }
}
