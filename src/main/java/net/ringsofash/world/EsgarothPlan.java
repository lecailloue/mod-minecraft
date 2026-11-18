package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan d'Esgaroth (Lac-ville) : 320 x 320 blocs. Un lac aux rives sableuses, et au milieu une ville de bois bati sur pilotis : des ilots de planches de 28 x 22 separes par des canaux,
 * relies par des passerelles (dont de hautes passerelles arquees), chacun portant quatre maisons a pignons raides, a un ou deux etages, aux toits de bardeaux sombres, d'ardoise ou de chaume ;
 * au centre, la place avec la grande halle du Maitre (trois etages, perron, tour a flèche), des kiosques a toit pointu sur quatre poteaux, des etals ; une longue chaussee sur pilotis
 * rejoint chaque rive ; des bateaux dans les canaux. Grille de blocs abstraits, sans dependance a Minecraft.
 * Repere local : x de 0 a W - 1 (ouest a est), z de 0 a D - 1 (nord a sud) ; y = 0 : le sol des rives (le sol au centre du site) ; l'eau affleure a y = -1 ; le pont des pilotis est a y = DK.
 */
public final class EsgarothPlan {
    public static final int W = 320, D = 320, OFF = 20, TOP = 64, H = OFF + TOP + 1, CX = 160, CZ = 160;
    public static final int BAND = 30;
    public static final int WL = -1, DK = 2;

    public static final int AIR = 0, SAND = 1, GRAVEL = 2, DIRT = 3, GRASS = 4, STONE = 5, CLAY = 6, COARSE = 7, WATER = 8, PODZOL = 9, SHORTG = 10, FERN = 11, DANDEL = 12, POPPY = 13,
            CORNFL = 14, BLUET = 15, OAKLOG = 16, OAKLEAF = 17, BIRCHLEAF = 18, COBBLE = 19, MOSSC = 20, SPRLOG = 21, SPRLEAF = 22, BIRCHLOG = 23;
    public static final int WLOG_Y = 24, WLOG_X = 25, WLOG_Z = 26, WPLANK = 27, DKPLANK = 28, SPPLANK = 29, OAKPLANK = 30, SLATE = 31, DKLOG_Y = 32, DKLOG_X = 33, DKLOG_Z = 34,
            WFENCE = 35, DKFENCE = 36, GLASS = 37, LANTERN = 38, HLANT = 39, CHAIN = 40, BARREL = 41, HAY = 42, STB = 43, CHIM = 44, BOOKS = 45, CRAFT = 46,
            WOOL_R = 47, WOOL_Y = 48, WOOL_W = 49, WOOL_B = 50, THATCH = 51, LADN = 52, LADE = 53, LADS = 54, LADW = 55, DOOR = 56 /* 56..63 : direction * 2 + moitie haute */;
    // familles d'escaliers : base + direction * 2 + moitie haute (direction = sens de la montee : 0 N, 1 E, 2 S, 3 O) ; base + 8 dalle basse, base + 9 dalle haute
    public static final int WS = 96, SPS = 108, DKS = 120, SLS = 132, OKS = 144, STS = 156, THS = 168;
    public static final int N = 0, E = 1, S_ = 2, W_ = 3;

    private final byte[] g = new byte[W * H * D];
    private final int[] th = new int[W * D];
    private final short[] wl = new short[W * D];
    private final byte[] edge = new byte[W * D];
    private final boolean[] res = new boolean[W * D];
    private final boolean[] norail = new boolean[W * D];
    private final int[] top = new int[W * D];
    public final List<int[]> spawns = new ArrayList<>();         // {x, y, z}
    private final List<int[]> blocks = new ArrayList<>();        // {x0, z0, x1, z1, xc, zs, i, j}
    private final List<int[]> boatSpots = new ArrayList<>();

    private static EsgarothPlan inst;
    public static synchronized EsgarothPlan get() { if (inst == null) inst = new EsgarothPlan(); return inst; }

    // ------------------------------------------------------------------ outils
    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public byte raw(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    public byte get(int x, int y, int z) { return raw(x, y, z); }
    private int gi(int x, int y, int z) { return get(x, y, z) & 255; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    private void setIfAir(int x, int y, int z, int id) { if (ok(x, y, z) && g[idx(x, y, z)] == AIR) g[idx(x, y, z)] = (byte) id; }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int top(int x, int z) { return top[z * W + x]; }
    public int terrain(int x, int z) { return th[z * W + x]; }
    public int water(int x, int z) { return wl[z * W + x]; }
    public int weight(int x, int z) { return edge[z * W + x] & 255; }
    private int tAt(int x, int z) { return area(x, z) ? th[z * W + x] : 999; }

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
    private static double sm(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
    private static int stair(int base, int dir, boolean topHalf) { return base + dir * 2 + (topHalf ? 1 : 0); }
    private static int slab(int base, boolean topHalf) { return base + (topHalf ? 9 : 8); }
    private static int door(int dir, boolean upper) { return DOOR + dir * 2 + (upper ? 1 : 0); }
    private static int ladder(int dir) { return LADN + dir; }              // dir : cote ou se trouve le mur (N, E, S, W)

    private void box(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, id);
    }
    private void put(boolean ax, int a, int f, int y, int id) { if (ax) set(a, y, f, id); else set(f, y, a, id); }
    private void put2(int axis, int perp, int len, int y, int id) { if (axis == 0) set(len, y, perp, id); else set(perp, y, len, id); }
    private void reserve(int x0, int z0, int x1, int z1, int m) {
        for (int x = x0 - m; x <= x1 + m; x++) for (int z = z0 - m; z <= z1 + m; z++) if (area(x, z)) res[z * W + x] = true;
    }

    // ------------------------------------------------------------------ geometrie
    private static double lakeR(double x, double z) {
        double ang = Math.atan2(z - CZ, x - CX);
        return 104 + 8 * vn(Math.cos(ang) * 2.2 + 10, Math.sin(ang) * 2.2 + 10, 71) + 3.5 * vn(Math.cos(ang) * 6 + 3, Math.sin(ang) * 6 + 3, 72);
    }
    private static boolean wetAt(double x, double z) { return Math.hypot(x - CX, z - CZ) < lakeR(x, z) - 3; }

    // ------------------------------------------------------------------ construction
    public EsgarothPlan() {
        java.util.Arrays.fill(wl, (short) -999);
        terrain();
        materialize();
        layout();
        for (int[] b : blocks) platform(b[0], b[1], b[2], b[3]);
        platform(PLAZA[0], PLAZA[1], PLAZA[2], PLAZA[3]);
        bridges();
        causeways();
        plaza();
        for (int[] b : blocks) district(b);
        railings();
        boats();
        shore();
        trees();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
            int e = Math.min(Math.min(x, z), Math.min(W - 1 - x, D - 1 - z));
            edge[z * W + x] = (byte) Math.round(255 * sm(e / (double) BAND));
        }
    }

    private final boolean[] beach = new boolean[W * D];

    private void terrain() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            double xw = x + 0.5, zw = z + 0.5, d = Math.hypot(xw - CX, zw - CZ), R = lakeR(xw, zw), z0 = R - d;
            double land = 0.7 * vn(xw / 23.0, zw / 23.0, 73) + 0.4 * vn(xw / 7.0, zw / 7.0, 74)
                    + 11 * sm((d - 140) / 85.0) * (0.55 + 0.45 * (vn(xw / 45.0, zw / 45.0, 75) + 1) / 2) + (d > 140 ? 1.5 * vn(xw / 9.0, zw / 9.0, 78) * sm((d - 140) / 30.0) : 0);
            double h;
            if (z0 <= 0) h = land + 1.0 * sm(-z0 / 6.0);
            else {
                double bed = -Math.min(z0 * 0.45, 2.2) - 12.5 * sm((z0 - 4) / 50.0) + 1.3 * vn(xw / 13.0, zw / 13.0, 76) * sm(z0 / 10.0);
                double k = sm(z0 / 4.0);
                h = land * (1 - k) + bed * k;
            }
            int i = z * W + x;
            th[i] = (int) Math.round(h);
            if (th[i] <= -2) wl[i] = WL;
            beach[i] = z0 > -7 && z0 < 8;
        }
    }

    private void materialize() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x, t = th[i];
            boolean wet = wl[i] > -999;
            for (int y = -OFF + 2; y <= t; y++) {
                int depth = t - y, id;
                if (depth == 0) {
                    double q = rnd(x, 3, z);
                    if (wet) id = t < -9 ? (q < 0.5 ? CLAY : GRAVEL) : (q < 0.6 ? SAND : q < 0.85 ? GRAVEL : CLAY);
                    else if (beach[i] && t <= 1) id = q < 0.9 ? SAND : GRAVEL;
                    else id = q < 0.05 ? PODZOL : q < 0.1 ? COARSE : GRASS;
                } else if (depth <= 3) id = wet || (beach[i] && t <= 1) ? SAND : DIRT;
                else if (depth <= 5) id = DIRT;
                else id = STONE;
                set(x, y, z, id);
            }
            for (int y = t + 1; y <= wl[i]; y++) set(x, y, z, WATER);
        }
    }

    // ------------------------------------------------------------------ disposition
    private static final int[] PLAZA = {124, 124, 196, 172};
    private static final int PX = 34, PZ = 29;

    private void layout() {
        for (int j = -4; j <= 4; j++) for (int i = -4; i <= 4; i++) {
            int jx = i == 0 ? 0 : (int) Math.round(2 * vn(i * 1.9 + 5, j * 1.3 + 2, 81)), jz = (int) Math.round(2 * vn(i * 1.1 + 9, j * 2.3 + 1, 82));
            int cx = CX + i * PX + jx, cz = CZ + j * PZ + jz;
            int w = 25 + (hash(i, j, 83) & 3), dp = 21 + (hash(i, j, 84) & 1);
            if (i == 0) w = 27;
            int x0 = cx - w / 2, z0 = cz - dp / 2, x1 = x0 + w - 1, z1 = z0 + dp - 1;
            if (x0 - 8 < PLAZA[2] && x1 + 8 > PLAZA[0] && z0 - 8 < PLAZA[3] && z1 + 8 > PLAZA[1]) continue;
            boolean in = true;
            for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) if (Math.hypot(c[0] - CX, c[1] - CZ) > lakeR(c[0], c[1]) - 5) in = false;
            if (!in) continue;
            blocks.add(new int[]{x0, z0, x1, z1, (x0 + x1) / 2, (z0 + z1) / 2, i, j});
        }
    }

    private void platform(int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (!area(x, z)) continue;
            boolean dk = rnd(x, 9, z) < 0.14;
            set(x, DK, z, dk ? DKPLANK : (((x + (z >> 2)) & 7) == 0 ? SPPLANK : WPLANK));
        }
        reserve(x0, z0, x1, z1, 2);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
            boolean pile = ((x - x0) % 6 == 0 || x == x1) && ((z - z0) % 6 == 0 || z == z1);
            boolean edgePile = (ex && (z - z0) % 4 == 0) || (ez && (x - x0) % 4 == 0);
            if (pile || edgePile) pile(x, z, DK - 1);
            if (ex && ((z - z0) % 4 != 1)) set(x, DK - 1, z, WLOG_Z);
            else if (ez && ((x - x0) % 4 != 1)) set(x, DK - 1, z, WLOG_X);
        }
        for (int x = x0 + 6; x < x1; x += 6) for (int z = z0; z <= z1; z++) if (get(x, DK - 1, z) == AIR && rnd(x, z, 2) < 0.6) set(x, DK - 1, z, WLOG_Z);
    }

    private void pile(int x, int z, int yTop) {
        if (!area(x, z)) return;
        for (int y = th[z * W + x] + 1; y <= yTop; y++) if (get(x, y, z) == AIR || get(x, y, z) == WATER) set(x, y, z, WLOG_Y);
    }

    // ------------------------------------------------------------------ passerelles et chaussees
    private void walkX(int xa, int xb, int z, int wd, boolean arch) {              // passerelle est-ouest : de xa a xb, largeur wd centree sur z (arch : dos d'ane pour laisser passer les barques)
        int a = Math.min(xa, xb), b = Math.max(xa, xb), len = b - a + 1, z0 = z - wd / 2, z1 = z0 + wd - 1;
        for (int x = a; x <= b; x++) {
            int t = x - a, lift = arch ? archLift(t, len) : 0, prev = arch ? archLift(t - 1, len) : 0;
            for (int zz = z0; zz <= z1; zz++) {
                int yy = DK + lift;
                int id = WPLANK;
                if (arch && lift > prev) id = stair(SPS, E, false);
                else if (arch && lift < prev) id = stair(SPS, W_, false);
                set(x, yy, zz, id);
                if (arch && lift > 0 && (zz == z0 || zz == z1)) for (int y = DK; y < yy; y++) if (get(x, y, zz) == AIR) set(x, y, zz, WLOG_Y);
                norail[zz * W + x] = true;
            }
            set(x, DK + lift + 1, z0, WFENCE);
            set(x, DK + lift + 1, z1, WFENCE);
            set(x, DK - 1, z0, WLOG_X);
            set(x, DK - 1, z1, WLOG_X);
            if (!arch && (t % 6) == 3) { pile(x, z0, DK - 1); pile(x, z1, DK - 1); }
        }
        for (int zz = z0; zz <= z1; zz++) { if (area(a - 1, zz)) norail[zz * W + a - 1] = true; if (area(b + 1, zz)) norail[zz * W + b + 1] = true; }
    }

    private int archLift(int t, int len) {
        if (t < 0 || t >= len) return 0;
        double u = (t + 0.5) / len;
        return (int) Math.floor(4.4 * (1 - Math.pow(2 * u - 1, 2)));
    }

    private void walkZ(int za, int zb, int x, int wd) {
        int a = Math.min(za, zb), b = Math.max(za, zb), x0 = x - wd / 2, x1 = x0 + wd - 1;
        for (int z = a; z <= b; z++) {
            for (int xx = x0; xx <= x1; xx++) { set(xx, DK, z, WPLANK); norail[z * W + xx] = true; }
            set(x0, DK + 1, z, WFENCE);
            set(x1, DK + 1, z, WFENCE);
            set(x0, DK - 1, z, WLOG_Z);
            set(x1, DK - 1, z, WLOG_Z);
            if (((z - a) % 6) == 3) { pile(x0, z, DK - 1); pile(x1, z, DK - 1); }
        }
        for (int xx = x0; xx <= x1; xx++) { if (area(xx, a - 1)) norail[(a - 1) * W + xx] = true; if (area(xx, b + 1)) norail[(b + 1) * W + xx] = true; }
    }

    private int[] rectOf(int i, int j) {
        for (int[] b : blocks) if (b[6] == i && b[7] == j) return b;
        return null;
    }

    private void bridges() {
        for (int[] a : blocks) {
            int[] e = rectOf(a[6] + 1, a[7]);                                  // voisin a l'est
            if (e != null) {
                int z = (a[5] + e[5]) / 2;
                boolean arch = (hash(a[6], a[7], 85) & 3) == 0 && e[0] - a[2] - 1 >= 8;
                walkX(a[2] + 1, e[0] - 1, z, 3, arch);
            }
            int[] s = rectOf(a[6], a[7] + 1);                                  // voisin au sud
            if (s != null) walkZ(a[3] + 1, s[1] - 1, (a[4] + s[4]) / 2, 5);
        }
        int[] sN = null, sS = null;
        for (int[] b : blocks) {                                               // blocs voisins de la place
            if (b[0] > PLAZA[2] && b[0] - PLAZA[2] < 28) { int z = Math.max(Math.max(PLAZA[1] + 3, b[1] + 3), Math.min(Math.min(PLAZA[3] - 3, b[3] - 3), b[5])); if (z >= PLAZA[1] + 3 && z <= PLAZA[3] - 3 && z >= b[1] + 3 && z <= b[3] - 3) walkX(PLAZA[2] + 1, b[0] - 1, z, 3, false); }
            if (b[2] < PLAZA[0] && PLAZA[0] - b[2] < 28) { int z = Math.max(Math.max(PLAZA[1] + 3, b[1] + 3), Math.min(Math.min(PLAZA[3] - 3, b[3] - 3), b[5])); if (z >= PLAZA[1] + 3 && z <= PLAZA[3] - 3 && z >= b[1] + 3 && z <= b[3] - 3) walkX(b[2] + 1, PLAZA[0] - 1, z, 3, false); }
            if (b[6] == 0 && b[1] > PLAZA[3] && (sS == null || b[1] < sS[1])) sS = b;
            if (b[6] == 0 && b[3] < PLAZA[1] && (sN == null || b[3] > sN[3])) sN = b;
        }
        if (sS != null) walkZ(PLAZA[3] + 1, sS[1] - 1, sS[4], 5);
        if (sN != null) walkZ(sN[3] + 1, PLAZA[1] - 1, sN[4], 5);
    }

    private void causeways() {
        int[] last = null, first = null;                                       // chaussees nord et sud, dans l'axe de la colonne centrale
        for (int[] b : blocks) if (b[6] == 0) { if (last == null || b[7] > last[7]) last = b; if (first == null || b[7] < first[7]) first = b; }
        int xs = last != null ? last[4] : CX, zs = last != null ? last[3] + 1 : PLAZA[3] + 1;
        causeway(xs, zs, 1);
        int xn = first != null ? first[4] : CX, zn = first != null ? first[1] - 1 : PLAZA[1] - 1;
        causeway(xn, zn, -1);
    }

    private void causeway(int x, int z, int dir) {
        int x0 = x - 2, x1 = x + 2;
        boolean landed = false;
        for (int zz = z; area(x, zz); zz += dir) {
            int t = tAt(x, zz);
            if (t >= 0) {                                                                 // la rive : une marche vers le sol, puis on s'arrete
                for (int xx = x0; xx <= x1; xx++) set(xx, Math.max(t + 1, 1), zz, stair(WS, dir > 0 ? N : S_, false));
                for (int xx = x0; xx <= x1; xx++) norail[zz * W + xx] = true;
                landed = true;
                break;
            }
            for (int xx = x0; xx <= x1; xx++) { set(xx, DK, zz, ((zz >> 3) & 1) == 0 ? WPLANK : SPPLANK); norail[zz * W + xx] = true; }
            set(x0, DK + 1, zz, WFENCE);
            set(x1, DK + 1, zz, WFENCE);
            set(x0, DK - 1, zz, WLOG_Z);
            set(x1, DK - 1, zz, WLOG_Z);
            if (Math.abs(zz - z) % 5 == 2) { pile(x0, zz, DK - 1); pile(x1, zz, DK - 1); pile(x, zz, DK - 1); }
            if (Math.abs(zz - z) % 10 == 7) { lamp(x0, zz); lamp(x1, zz); }
        }
        reserve(x0, Math.min(z, z + dir * 130), x1, Math.max(z, z + dir * 130), 3);
    }

    private void lamp(int x, int z) {
        if (!area(x, z)) return;
        for (int y = DK + 1; y <= DK + 3; y++) set(x, y, z, WFENCE);
        set(x, DK + 4, z, LANTERN);
    }

    // ------------------------------------------------------------------ toits
    /** toit a deux pans raide ; murs exterieurs de pmin a pmax (perpendiculaire a la crete) et de l0 a l1 (le long de la crete) ; yb : premiere rangee de toit */
    private int gable(int axis, int pmin, int pmax, int l0, int l1, int yb, int fam, int blk, int endMat, int endAcc) {
        int a = pmin - 1, b = pmax + 1, h = yb, rows = 0;
        while (a + rows <= b - rows) rows++;
        int dA = axis == 0 ? S_ : E, dB = axis == 0 ? N : W_;
        for (int i = 0; i < rows; i++, a++, b--, h += 2) {
            for (int l = l0 - 1; l <= l1 + 1; l++) {
                if (a == b) { put2(axis, a, l, h, blk); put2(axis, a, l, h + 1, slab(fam, false)); }
                else if (a + 1 == b) { put2(axis, a, l, h, stair(fam, dA, false)); put2(axis, b, l, h, stair(fam, dB, false)); put2(axis, a, l, h + 1, slab(fam, false)); put2(axis, b, l, h + 1, slab(fam, false)); }
                else { put2(axis, a, l, h, stair(fam, dA, false)); put2(axis, b, l, h, stair(fam, dB, false)); put2(axis, a, l, h + 1, blk); put2(axis, b, l, h + 1, blk); }
            }
        }
        // pignons
        int pc = (pmin + pmax) / 2;
        for (int e : new int[]{l0, l1}) for (int p = pmin; p <= pmax; p++) {
            int r = Math.min(p - (pmin - 1), (pmax + 1) - p);
            for (int y = yb; y < yb + 2 * r; y++) put2(axis, p, e, y, endMat);
            put2(axis, p, e, yb - 1, endAcc);
            if (p == pc || (p == pc + 1 && (pmax - pmin) % 2 == 1)) for (int y = yb + 1; y < yb + 2 * r - 2 && y <= yb + 4; y++) put2(axis, p, e, y, GLASS);
        }
        // poutres de rive : une file de bois sombre le long de l'avant-toit
        for (int e : new int[]{l0 - 1, l1 + 1}) {
            int aa = pmin - 1, bb = pmax + 1, hh = yb;
            for (int i = 0; i < rows; i++, aa++, bb--, hh += 2) { if (aa >= bb) break; put2(axis, aa, e, hh, stair(DKS, axis == 0 ? S_ : E, false)); put2(axis, bb, e, hh, stair(DKS, axis == 0 ? N : W_, false)); }
        }
        return h;
    }

    /** toit pyramidal raide jusqu'a la pointe ; murs de x0..x1, z0..z1 ; renvoie la hauteur de la pointe */
    private int pyramid(int x0, int z0, int x1, int z1, int yb, int fam, int blk, int spire) {
        int xa = x0 - 1, xb = x1 + 1, za = z0 - 1, zb = z1 + 1, h = yb;
        while (xa <= xb && za <= zb) {
            for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) {
                boolean ring = x == xa || x == xb || z == za || z == zb;
                if (!ring) continue;
                if (xa == xb || za == zb || (xa + 1 == xb && za + 1 == zb)) { set(x, h, z, blk); set(x, h + 1, z, blk); continue; }
                int dir = z == za ? S_ : z == zb ? N : x == xa ? E : W_;
                set(x, h, z, stair(fam, dir, false));
                set(x, h + 1, z, blk);
            }
            xa++; xb--; za++; zb--; h += 2;
        }
        int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        for (int y = h; y < h + spire; y++) set(cx, y, cz, y == h + spire - 1 ? LANTERN : WFENCE);
        return h + spire;
    }

    // ------------------------------------------------------------------ maisons
    private static final int[] ROOFF = {DKS, SPS, SLS, THS, DKS, SPS}, ROOFB = {DKPLANK, SPPLANK, SLATE, THATCH, DKPLANK, SPPLANK};

    /**
     * Maison de bois : coins en grumes, murs de planches vieillies, bandeaux a chaque etage, fenetres, porte cote 'door' (0 N, 1 E, 2 S, 3 O), echelle interieure, toit raide.
     * (x0, z0)-(x1, z1) : emprise ; y0 : niveau du pont ; fh : hauteur d'etage ; ridge : 0 = faitage le long de x, 1 = le long de z.
     */
    private void house(int x0, int z0, int x1, int z1, int y0, int floors, int fh, int ridge, int roof, int doorSide, int variant) {
        int rf = ROOFF[roof], rb = ROOFB[roof];
        int wallLow = (variant & 1) == 0 ? WPLANK : OAKPLANK, wallUp = (variant & 2) == 0 ? WPLANK : SPPLANK;
        int topY = y0 + floors * fh;
        for (int f = 0; f < floors; f++) {
            int yf = y0 + f * fh;
            int mat = f == 0 ? wallLow : wallUp;
            for (int y = yf + 1; y <= yf + fh; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
                if (!ex && !ez) { if (y <= yf + fh - 1) set(x, y, z, AIR); continue; }
                boolean corner = ex && ez;
                int id;
                if (corner) id = WLOG_Y;
                else if (y == yf + fh) id = ez ? WLOG_X : WLOG_Z;
                else id = mat;
                set(x, y, z, id);
            }
            // plancher
            if (f > 0) for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++) set(x, yf, z, WPLANK);
            // fenetres
            for (int k = 2; k < Math.max(x1 - x0, z1 - z0) - 1; k += 3) {
                for (int s = 0; s < 4; s++) {
                    if (s == doorSide && f == 0) continue;
                    int x, z; boolean along = s == 0 || s == 2;
                    if (along) { x = x0 + k; z = s == 0 ? z0 : z1; if (x >= x1) continue; } else { z = z0 + k; x = s == 3 ? x0 : x1; if (z >= z1) continue; }
                    if (s == doorSide && (along ? Math.abs(x - (x0 + x1) / 2) <= 1 : Math.abs(z - (z0 + z1) / 2) <= 1)) continue;
                    set(x, yf + 2, z, GLASS);
                    set(x, yf + 3, z, GLASS);
                    if (rnd(x, yf, z) < 0.45) { int o = s == 0 ? -1 : s == 2 ? 1 : 0, p = s == 3 ? -1 : s == 1 ? 1 : 0;       // jardiniere sous la fenetre
                        setIfAir(x + p, yf + 2, z + o, slab(WS, false)); setIfAir(x + p, yf + 3, z + o, OAKLEAF); }
                }
            }
            // eclairage et meubles
            set((x0 + x1) / 2, yf + 1, (z0 + z1) / 2, WFENCE);
            set((x0 + x1) / 2, yf + 2, (z0 + z1) / 2, LANTERN);
            if (f == 0) {
                set(x0 + 1, yf + 1, z0 + 1, BARREL);
                if (rnd(x0, y0, z0) < 0.5) set(x1 - 1, yf + 1, z0 + 1, CRAFT); else set(x1 - 1, yf + 1, z1 - 1, BOOKS);
            }
        }
        // echelle interieure
        int lx = x0 + 1, lz = z1 - 1;
        for (int y = y0 + 1; y <= topY - 1; y++) set(lx, y, lz, ladder(N));
        // porte
        int dx, dz, cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        switch (doorSide) {
            case 0 -> { dx = cx; dz = z0; }
            case 1 -> { dx = x1; dz = cz; }
            case 2 -> { dx = cx; dz = z1; }
            default -> { dx = x0; dz = cz; }
        }
        set(dx, y0 + 1, dz, door(doorSide, false));
        set(dx, y0 + 2, dz, door(doorSide, true));
        // plaque au-dessus de la porte et lanterne a l'exterieur
        int ox = doorSide == 1 ? 1 : doorSide == 3 ? -1 : 0, oz = doorSide == 2 ? 1 : doorSide == 0 ? -1 : 0;
        set(dx + ox, y0 + 1, dz + oz, WFENCE);
        set(dx + ox, y0 + 2, dz + oz, LANTERN);
        // toit
        int ridgeTop;
        if (ridge == 0) ridgeTop = gable(0, z0, z1, x0, x1, topY + 1, rf, rb, wallUp, WLOG_X);
        else ridgeTop = gable(1, x0, x1, z0, z1, topY + 1, rf, rb, wallUp, WLOG_Z);
        // cheminee
        if (variant % 3 == 0) {
            int hx = ridge == 0 ? x0 + 2 : (x0 + x1) / 2, hz = ridge == 0 ? (z0 + z1) / 2 : z0 + 2;
            for (int y = topY - 1; y <= ridgeTop + 1; y++) set(hx, y, hz, STB);
            set(hx, ridgeTop + 2, hz, CHIM);
            set(hx, ridgeTop + 3, hz, CHIM);
        }
        // balcon sur la rue
        if ((variant & 4) != 0 && floors >= 2) balcony(x0, z0, x1, z1, y0 + fh, doorSide);
        spawns.add(new int[]{cx, y0 + 1, cz});
    }

    private void balcony(int x0, int z0, int x1, int z1, int yf, int side) {
        int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        for (int k = -2; k <= 2; k++) {
            int x = side == 0 || side == 2 ? cx + k : (side == 1 ? x1 + 1 : x0 - 1), z = side == 1 || side == 3 ? cz + k : (side == 2 ? z1 + 1 : z0 - 1);
            set(x, yf, z, WSLAB_T());
            int x2 = side == 1 ? x + 1 : side == 3 ? x - 1 : x, z2 = side == 2 ? z + 1 : side == 0 ? z - 1 : z;
            set(x2, yf, z2, WSLAB_T());
            set(x2, yf + 1, z2, WFENCE);
            if (Math.abs(k) == 2) { set(x, yf + 1, z, WFENCE); }
        }
        for (int k : new int[]{-2, 2}) {
            int x = side == 0 || side == 2 ? cx + k : (side == 1 ? x1 + 2 : x0 - 2), z = side == 1 || side == 3 ? cz + k : (side == 2 ? z1 + 2 : z0 - 2);
            set(x, yf + 1, z, WFENCE);
            for (int y = DK + 1; y < yf; y++) set(x, y, z, WLOG_Y);
        }
    }

    private static int WSLAB_T() { return slab(WS, true); }

    /** tour carree a pointe : sept etages */
    private void tower(int x0, int z0, int size, int y0, int floors, int roof) {
        int x1 = x0 + size - 1, z1 = z0 + size - 1, fh = 4;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) { for (int y = DK + 1; y < y0; y++) set(x, y, z, WLOG_Y); if (y0 > DK) set(x, y0, z, WPLANK); }
        house(x0, z0, x1, z1, y0, floors, fh, 0, 0, 2, 1);
        // le toit de house() est a pignon : on le remplace par une pyramide
        int topY = y0 + floors * fh;
        box(x0 - 2, topY + 1, z0 - 2, x1 + 2, topY + 2 * size + 6, z1 + 2, AIR);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (x == x0 || x == x1 || z == z0 || z == z1) set(x, topY, z, DKLOG_Y);
        pyramid(x0, z0, x1, z1, topY + 1, ROOFF[roof], ROOFB[roof], 4);
    }

    // ------------------------------------------------------------------ ilots
    private void district(int[] b) {
        int x0 = b[0], z0 = b[1], x1 = b[2], z1 = b[3], xc = b[4], zs = b[5], i = b[6], j = b[7];
        int rowN0 = z0 + 1, rowN1 = zs - 3, rowS0 = zs + 3, rowS1 = z1 - 1;                  // rangees de maisons au nord et au sud de la rue centrale
        int depth = Math.min(rowN1 - rowN0 + 1, rowS1 - rowS0 + 1);
        boolean towerBlock = (i == -2 && j == 0) || (i == 2 && j == 1) || (i == -1 && j == -2);
        for (int row = 0; row < 2; row++) for (int half = 0; half < 2; half++) {
            int xa = half == 0 ? x0 + 1 : xc + 3, xb = half == 0 ? xc - 3 : x1 - 1;
            int za = row == 0 ? rowN1 - depth + 1 : rowS0, zb = za + depth - 1;
            int h = hash(i * 7 + row, j * 5 + half, 86) & 0x7fffffff;
            int len = xb - xa + 1, doorSide = row == 0 ? 2 : 0;
            if (towerBlock && row == 0 && half == 0) { tower(xa + 1, za - 1, 7, DK, 7, 2); continue; }
            double q = (h & 1023) / 1024.0;
            int[] pos = (i + j) % 5 == 0 && row == 0 && half == 1 ? null : new int[0];
            if (pos == null) { market(xa, za, xb, zb); continue; }
            if (len >= 11 && q < 0.30) {                                                 // deux petites maisons hautes
                int w = (len - 1) / 2;
                house(xa, za, xa + w - 1, zb, DK, 3, 4, 1, (h >> 3) % 6, doorSide, h >> 6);
                house(xb - w + 1, za, xb, zb, DK, 2 + ((h >> 10) & 1), 4, 1, (h >> 5) % 6, doorSide, h >> 9);
            } else {
                int rg = q < 0.65 ? 0 : 1;
                int w = Math.min(len, rg == 1 ? 8 : 8 + ((h >> 3) & 3));
                int xs = half == 0 ? xb - w + 1 : xa;                                     // les maisons se serrent vers le couloir central
                house(xs, za, xs + w - 1, zb, DK, 2 + ((h >> 8) % 4 == 0 ? 1 : 0), 4, rg, (h >> 4) % 6, doorSide, h >> 7);
            }
        }
        // rue : lampadaires, tonneaux, caisses, filets
        for (int x = x0 + 2; x < x1; x += 7) { lamp(x, zs - 2 + (((x / 7) & 1) * 4)); }
        for (int k = 0; k < 5; k++) {
            int x = x0 + 2 + (int) (rnd(i, j, 90 + k) * (x1 - x0 - 3)), z = zs - 1 + (int) (rnd(j, i, 95 + k) * 3);
            if (Math.abs(x - xc) <= 2) continue;
            set(x, DK + 1, z, rnd(x, z, 3) < 0.5 ? BARREL : HAY);
        }
        for (int s = 0; s < 4; s++) spawns.add(new int[]{x0 + 3 + (int) (rnd(i, j, 100 + s) * (x1 - x0 - 6)), DK + 1, zs});
    }

    private void market(int xa, int za, int xb, int zb) {                                    // petit marche couvert : etals a auvents
        for (int x = xa + 1; x + 3 <= xb; x += 5) {
            int z = za + 1;
            int wool = (x / 5 + xa) % 3 == 0 ? WOOL_R : (x / 5 + xa) % 3 == 1 ? WOOL_Y : WOOL_W;
            for (int dx : new int[]{0, 3}) for (int dz : new int[]{0, 4}) for (int y = DK + 1; y <= DK + 3; y++) set(x + dx, y, z + dz, WFENCE);
            for (int dx = -0; dx <= 3; dx++) for (int dz = 0; dz <= 4; dz++) set(x + dx, DK + 4, z + dz, (dx + dz) % 2 == 0 ? wool : WOOL_W);
            for (int dx = 1; dx <= 2; dx++) set(x + dx, DK + 1, z + 4, BARREL);
            set(x + 1, DK + 1, z + 1, HAY); set(x + 2, DK + 1, z + 1, BARREL);
        }
    }

    // ------------------------------------------------------------------ la place
    private void plaza() {
        int x0 = PLAZA[0], z0 = PLAZA[1], x1 = PLAZA[2], z1 = PLAZA[3], cx = (x0 + x1) / 2;
        // soubassement de la halle : un perron de deux marches
        int hx0 = cx - 17, hx1 = cx + 12, hz0 = z0 + 3, hz1 = z0 + 17, hy = DK + 2;
        for (int x = hx0 - 2; x <= hx1 + 2; x++) for (int z = hz0 - 2; z <= hz1 + 7; z++) {
            set(x, DK + 1, z, WLOG_Y);
            set(x, DK + 2, z, WPLANK);
        }
        for (int x = cx - 8; x <= cx + 3; x++) { set(x, DK + 1, hz1 + 8, WLOG_Y); set(x, DK + 2, hz1 + 8, stair(WS, N, false)); set(x, DK + 1, hz1 + 9, stair(WS, N, false)); }
        // la halle : trois etages hauts, faitage le long de x
        house(hx0, hz0, hx1, hz1, hy, 3, 5, 0, 0, 2, 0);
        for (int y = hy + 1; y <= hy + 3; y++) for (int x = cx - 3; x <= cx; x++) set(x, y, hz1, AIR);                  // grande porte a deux battants
        for (int x : new int[]{cx - 4, cx + 1}) for (int y = hy + 1; y <= hy + 4; y++) set(x, y, hz1, DKLOG_Y);
        for (int x = cx - 4; x <= cx + 1; x++) set(x, hy + 4, hz1, DKPLANK);
        for (int x = cx - 3; x <= cx; x++) set(x, hy + 3, hz1, GLASS);
        set(cx - 2, hy + 1, hz1, door(S_, false)); set(cx - 2, hy + 2, hz1, door(S_, true));
        set(cx - 1, hy + 1, hz1, door(S_, false)); set(cx - 1, hy + 2, hz1, door(S_, true));
        // grands vitrages sur la facade
        for (int k = 0; k < 5; k++) { int x = hx0 + 3 + k * 6; if (Math.abs(x - cx + 2) <= 3) continue; for (int y = hy + 7; y <= hy + 10; y++) set(x, y, hz1, GLASS); for (int y = hy + 2; y <= hy + 4; y++) set(x, y, hz1, GLASS); }
        // porche : quatre poteaux et un auvent
        for (int x = cx - 7; x <= cx + 4; x += 11) for (int z = hz1 + 4; z <= hz1 + 4; z++) for (int y = hy + 1; y <= hy + 5; y++) set(x, y, z, DKLOG_Y);
        for (int x = cx - 8; x <= cx + 5; x++) for (int z = hz1 + 1; z <= hz1 + 5; z++) set(x, hy + 6, z, z == hz1 + 5 ? stair(DKS, N, true) : DKPLANK);
        for (int x = cx - 7; x <= cx + 4; x += 11) set(x, hy + 7, hz1 + 4, LANTERN);
        // tour a fleche, accolee a la halle
        tower(hx1 + 1, hz0, 8, hy, 8, 2);
        // kiosques a toit pointu, aux angles
        kiosk(x0 + 9, z1 - 8);
        kiosk(x1 - 13, z1 - 8);
        kiosk(x0 + 9, z0 + 6);
        kiosk(x1 - 13, z0 + 6);
        // etals et lampadaires
        for (int k = 0; k < 4; k++) market(x0 + 16 + k * 14, z1 - 10, x0 + 28 + k * 14, z1 - 3);
        for (int x = x0 + 4; x <= x1 - 4; x += 8) { lamp(x, z1 - 1); lamp(x, hz1 + 11); }
        // appontements au bord ouest et est
        dock(x0, (z0 + z1) / 2, W_);
        dock(x1, (z0 + z1) / 2, E);
        dock(x0 + 12, z1, S_);
        dock(x1 - 12, z1, S_);
        for (int s = 0; s < 14; s++) spawns.add(new int[]{x0 + 8 + (int) (rnd(s, 1, 2) * (x1 - x0 - 16)), DK + 1, hz1 + 11 + (int) (rnd(s, 3, 4) * 8)});
    }

    private void kiosk(int x0, int z0) {                                                     // kiosque de 5 x 5 : quatre poteaux, un toit en pyramide raide
        int x1 = x0 + 4, z1 = z0 + 4;
        for (int dx : new int[]{0, 4}) for (int dz : new int[]{0, 4}) for (int y = DK + 1; y <= DK + 5; y++) set(x0 + dx, y, z0 + dz, DKLOG_Y);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (x == x0 || x == x1 || z == z0 || z == z1) set(x, DK + 6, z, x == x0 || x == x1 ? DKLOG_Z : DKLOG_X);
        for (int x = x0 + 1; x < x1; x++) { set(x, DK + 1, z0, WFENCE); set(x, DK + 1, z1, WFENCE); }
        for (int z = z0 + 1; z < z1; z++) { set(x0, DK + 1, z, WFENCE); set(x1, DK + 1, z, WFENCE); }
        set(x0 + 2, DK + 1, z1, AIR);
        set((x0 + x1) / 2, DK + 1, (z0 + z1) / 2, BARREL);
        pyramid(x0, z0, x1, z1, DK + 7, SPS, SPPLANK, 3);
        reserve(x0, z0, x1, z1, 1);
    }

    private void dock(int ex, int ez, int dir) {                                             // ponton au ras de l'eau, relie au pont par une marche (ex, ez : case du bord du pont)
        int dx = dir == E ? 1 : dir == W_ ? -1 : 0, dz = dir == S_ ? 1 : 0, back = dir == E ? W_ : dir == W_ ? E : dir == S_ ? N : S_;
        for (int k = 1; k <= 8; k++) for (int w = -2; w <= 2; w++) {
            int xx = ex + dx * k + (dz != 0 ? w : 0), zz = ez + dz * k + (dx != 0 ? w : 0);
            if (!area(xx, zz) || th[zz * W + xx] > -2) continue;
            set(xx, 0, zz, WPLANK);
            if (k == 1) set(xx, 1, zz, stair(WS, back, false));
            if (k % 3 == 0 && Math.abs(w) == 2) pile(xx, zz, -1);
            if (k == 1) { int bx = ex + (dz != 0 ? w : 0), bz = ez + (dx != 0 ? w : 0); if (area(bx, bz)) norail[bz * W + bx] = true; }
        }
        boatSpots.add(new int[]{ex + dx * 4 + (dz != 0 ? 4 : 0), ez + dz * 4 + (dx != 0 ? 4 : 0), dx != 0 ? 1 : 0});
    }

    // ------------------------------------------------------------------ finitions
    private void railings() {
        List<int[]> all = new ArrayList<>(blocks);
        all.add(new int[]{PLAZA[0], PLAZA[1], PLAZA[2], PLAZA[3]});
        for (int[] b : all) {
            for (int x = b[0]; x <= b[2]; x++) for (int z = b[1]; z <= b[3]; z++) {
                if (!(x == b[0] || x == b[2] || z == b[1] || z == b[3])) continue;
                if (norail[z * W + x] || get(x, DK + 1, z) != AIR) continue;
                set(x, DK + 1, z, WFENCE);
                if ((x - b[0] + z - b[1]) % 9 == 4) { set(x, DK + 2, z, WFENCE); set(x, DK + 3, z, LANTERN); }
            }
        }
    }

    private void boatAt(int x, int z, int ax) {                                               // barque de 7 x 3 au ras de l'eau : coque de planches, bordages bas
        int len = 7, wd = 3;
        for (int a = 0; a < len; a++) for (int b = 0; b < wd; b++) {
            int xx = ax == 0 ? x + a : x + b, zz = ax == 0 ? z + b : z + a;
            if (!area(xx, zz) || get(xx, -1, zz) != WATER || get(xx, 0, zz) != AIR || get(xx, 1, zz) != AIR || get(xx, DK, zz) != AIR) return;
        }
        for (int a = 0; a < len; a++) for (int b = 0; b < wd; b++) {
            int xx = ax == 0 ? x + a : x + b, zz = ax == 0 ? z + b : z + a;
            boolean side = b == 0 || b == wd - 1 || a == 0 || a == len - 1;
            if ((a == 0 || a == len - 1) && b != 1) continue;
            set(xx, -1, zz, SPPLANK);
            if (side) set(xx, 0, zz, a == 0 || a == len - 1 ? SPPLANK : slab(SPS, false));
        }
        int mx = ax == 0 ? x + 3 : x + 1, mz = ax == 0 ? z + 1 : z + 3;
        set(mx, 0, mz, rnd(x, z, 5) < 0.5 ? BARREL : HAY);
    }

    private void boats() {
        for (int[] bs : boatSpots) boatAt(bs[0], bs[1], bs[2]);
        for (int k = 0; k < 90; k++) {                                                        // barques amarrees dans les canaux
            int x = 20 + (int) (rnd(k, 3, 7) * (W - 40)), z = 20 + (int) (rnd(k, 4, 8) * (D - 40));
            if (!wetAt(x, z) || Math.hypot(x - CX, z - CZ) > 98) continue;
            boatAt(x, z, hash(k, 5, 9) & 1);
        }
    }

    private void shore() {                                                                    // roseaux, herbes et fleurs sur les rives
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x, t = th[i];
            if (wl[i] > -999 || t < 0) continue;
            if (get(x, t + 1, z) != AIR) continue;
            double q = rnd(x, 21, z);
            if (beach[i]) { if (q < 0.03) set(x, t + 1, z, SHORTG); continue; }
            if (q < 0.18) set(x, t + 1, z, SHORTG);
            else if (q < 0.21) set(x, t + 1, z, FERN);
            else if (q < 0.235) set(x, t + 1, z, DANDEL);
            else if (q < 0.25) set(x, t + 1, z, POPPY);
            else if (q < 0.26) set(x, t + 1, z, CORNFL);
        }
    }

    private void tree(int x, int z, int kind) {
        int t = tAt(x, z);
        if (t < 1) return;
        if (kind == 2) {                                                                     // epicea
            int hgt = 9 + (int) (rnd(x, 4, z) * 5);
            for (int y = 1; y <= hgt; y++) set(x, t + y, z, SPRLOG);
            for (int y = 3; y <= hgt + 1; y++) { int r = Math.max(0, (hgt + 2 - y) * 2 / 5); for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) if (Math.abs(dx) + Math.abs(dz) <= r + 1) setIfAir(x + dx, t + y, z + dz, SPRLEAF); }
            return;
        }
        int hgt = 5 + (int) (rnd(x, 6, z) * 3), lf = kind == 0 ? OAKLEAF : BIRCHLEAF;
        for (int y = 1; y <= hgt; y++) set(x, t + y, z, kind == 0 ? OAKLOG : BIRCHLOG);
        for (int dx = -3; dx <= 3; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -3; dz <= 3; dz++) {
            double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
            if (d <= 3.1 && rnd(x + dx, t + dy, z + dz) < 0.92) setIfAir(x + dx, t + hgt + dy, z + dz, lf);
        }
    }

    private void trees() {
        for (int z = 6; z < D - 6; z += 3) for (int x = 6; x < W - 6; x += 3) {
            int i = z * W + x;
            if (th[i] < 1 || res[i] || beach[i] && th[i] <= 1) continue;
            double d = Math.hypot(x - CX, z - CZ), R = lakeR(x, z);
            if (d < R + 5) continue;
            double dens = 0.02 + 0.30 * sm((vn(x / 34.0, z / 34.0, 87) - 0.1) * 2.5) + 0.10 * sm((d - 150) / 50);
            if (rnd(x, 88, z) > dens) continue;
            int xx = x + (int) (rnd(x, 1, z) * 3) - 1, zz = z + (int) (rnd(x, 2, z) * 3) - 1;
            int kind = d > 165 && rnd(x, 7, z) < 0.6 ? 2 : (rnd(x, 8, z) < 0.7 ? 0 : 1);
            tree(xx, zz, kind);
        }
    }
}
