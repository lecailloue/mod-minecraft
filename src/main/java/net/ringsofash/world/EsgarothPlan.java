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
    // mobilier et armes : lits (rouge 64..71, bleu 72..79 : base + direction du pied vers la tete * 2 + tete), tapis, coffres, fours, volets, fleches noires
    public static final int BED = 64, BEDB = 72, CARPR = 80, CARPB = 81, PLATE = 82, CHEST = 83 /* 83..86 */, FURN = 87 /* 87..90 */, CAULD = 91, TRAPD = 92 /* 92..95 : sens vers l'exterieur */;
    public static final int BKS = 180 /* famille : pierre noire */, BWX = 190, BWZ = 191, BWP = 192, BLKB = 193, BLKWOOL = 194, BLKCARP = 195, CAMP = 196, WEB = 197, CHAINX = 198, CHAINZ = 199, BLKPOL = 200;
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
    private static int bed(int dir, boolean head, boolean blue) { return (blue ? BEDB : BED) + dir * 2 + (head ? 1 : 0); }
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
        fishing();
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
            if (th[z0 * W + x] > -2 || t < 2 || t >= len - 2) set(x, DK - 1, z0, WLOG_X);                // pas de poutre sous le tablier au-dessus du canal : les barques passent
            if (th[z1 * W + x] > -2 || t < 2 || t >= len - 2) set(x, DK - 1, z1, WLOG_X);
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
            if (th[z * W + x0] > -2 || z - a < 2 || b - z < 2) set(x0, DK - 1, z, WLOG_Z);
            if (th[z * W + x1] > -2 || z - a < 2 || b - z < 2) set(x1, DK - 1, z, WLOG_Z);
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
        for (int xx = x0; xx <= x1; xx++) if (area(xx, z - dir)) norail[(z - dir) * W + xx] = true;   // pas de garde-corps du bloc en travers de la chaussee
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
            if (Math.abs(zz - z) % 10 < 5) { set(x0, DK - 1, zz, WLOG_Z); set(x1, DK - 1, zz, WLOG_Z); }   // poutres par tronçons : des fenetres laissent passer les barques
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
        int a0 = pmin - 1, b0 = pmax + 1, rows = 0;
        while (a0 + rows <= b0 - rows) rows++;
        int[] yy = new int[rows + 2];                                                       // hauteur de la marche de chaque rangee : avant-toit releve (1), puis pente raide (2 par rang)
        yy[0] = yb;
        for (int i = 0; i < rows; i++) yy[i + 1] = yy[i] + (i == 0 ? 1 : 2);
        int dA = axis == 0 ? S_ : E, dB = axis == 0 ? N : W_, top = yb, ra = 0, rb = 0;
        for (int i = 0; i < rows; i++) {
            int a = a0 + i, b = b0 - i, y = yy[i];
            for (int l = l0 - 1; l <= l1 + 1; l++) {
                if (a == b) { if (i > 0) put2(axis, a, l, y - 1, blk); put2(axis, a, l, y, blk); put2(axis, a, l, y + 1, slab(fam, false)); }
                else if (a + 1 == b) {
                    if (i > 0) { put2(axis, a, l, y - 1, blk); put2(axis, b, l, y - 1, blk); }
                    put2(axis, a, l, y, stair(fam, dA, false)); put2(axis, b, l, y, stair(fam, dB, false));
                    put2(axis, a, l, y + 1, slab(fam, false)); put2(axis, b, l, y + 1, slab(fam, false));
                } else {                                                                     // marche = escalier au sommet de la colonne, contremarche pleine dessous : pas de dessus plat
                    if (i > 0) { put2(axis, a, l, y - 1, blk); put2(axis, b, l, y - 1, blk); }
                    put2(axis, a, l, y, stair(fam, dA, false)); put2(axis, b, l, y, stair(fam, dB, false));
                }
            }
            if (a >= b - 1) { top = y + 1; ra = a; rb = b; break; }
            top = y;
        }
        // pignons : bardage jusqu'a la marche, fente vitree au centre
        int pc = (pmin + pmax) / 2;
        for (int e : new int[]{l0, l1}) for (int p = pmin; p <= pmax; p++) {
            int r = Math.min(p - (pmin - 1), (pmax + 1) - p);
            r = Math.min(r, rows - 1);
            for (int y = yb; y < yy[r]; y++) put2(axis, p, e, y, endMat);
            put2(axis, p, e, yb - 1, endAcc);
            if (p == pc || (p == pc + 1 && (pmax - pmin) % 2 == 1)) for (int y = yb + 1; y < yy[r] - 2 && y <= yb + 4; y++) put2(axis, p, e, y, GLASS);
        }
        // poutres de rive : une file de bois sombre le long de l'avant-toit
        for (int e : new int[]{l0 - 1, l1 + 1}) {
            for (int i = 0; i < rows; i++) { int aa = a0 + i, bb = b0 - i; if (aa >= bb) break; put2(axis, aa, e, yy[i], stair(DKS, axis == 0 ? S_ : E, false)); put2(axis, bb, e, yy[i], stair(DKS, axis == 0 ? N : W_, false)); }
        }
        // faitage prolonge aux deux bouts : poutre sombre et epi dresse (pointes des pignons comme sur les dessins du lac)
        int hy = top - 1, lg = axis == 0 ? DKLOG_X : DKLOG_Z;
        for (int pcol = ra; pcol <= rb; pcol++) for (int sg = -1; sg <= 1; sg += 2) {
            int e = sg < 0 ? l0 - 1 : l1 + 1;
            put2(axis, pcol, e + sg, hy, lg); put2(axis, pcol, e + 2 * sg, hy, lg);
            put2(axis, pcol, e + 2 * sg, hy + 1, DKFENCE); put2(axis, pcol, e + 2 * sg, hy + 2, DKFENCE);
        }
        return top + 1;
    }

    /** toit pyramidal raide jusqu'a la pointe ; murs de x0..x1, z0..z1 ; renvoie la hauteur de la pointe */
    private int pyramid(int x0, int z0, int x1, int z1, int yb, int fam, int blk, int spire) {
        int xa = x0 - 1, xb = x1 + 1, za = z0 - 1, zb = z1 + 1, h = yb;
        boolean first = true;
        while (xa <= xb && za <= zb) {
            for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) {
                boolean ring = x == xa || x == xb || z == za || z == zb;
                if (!ring) continue;
                if (xa == xb || za == zb || (xa + 1 == xb && za + 1 == zb)) { if (!first) set(x, h - 1, z, blk); set(x, h, z, blk); set(x, h + 1, z, blk); continue; }
                int dir = z == za ? S_ : z == zb ? N : x == xa ? E : W_;
                set(x, h, z, stair(fam, dir, false));
                if (!first) set(x, h - 1, z, blk);
            }
            xa++; xb--; za++; zb--; h += first ? 1 : 2; first = false;
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
    private boolean furnish = true;

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
            if (furnish) furnish(x0, z0, x1, z1, yf, f, floors, fh, doorSide, variant);
            facade(x0, z0, x1, z1, yf, f, fh, doorSide, variant);
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
        // (aucune barriere ni lanterne devant la porte : le passage reste libre)
        // toit
        int ridgeTop;
        if (ridge == 0) ridgeTop = gable(0, z0, z1, x0, x1, topY + 1, rf, rb, wallUp, WLOG_X);
        else ridgeTop = gable(1, x0, x1, z0, z1, topY + 1, rf, rb, wallUp, WLOG_Z);
        // cheminee
        if (variant % 3 == 0) {
            int hx = ridge == 0 ? x0 + 2 : (x0 + x1) / 2, hz = ridge == 0 ? (z0 + z1) / 2 : z0 + 2;
            for (int y = topY - 1; y <= ridgeTop + 1; y++) set(hx, y, hz, STB);
            set(hx, ridgeTop + 2, hz, CHIM);
            set(hx, ridgeTop + 3, hz, CAMP);                                                    // feu qui fume : la maison est habitee
        }
        // balcon sur la rue
        if ((variant & 4) != 0 && floors >= 2) balcony(x0, z0, x1, z1, y0 + fh, doorSide);
        spawns.add(new int[]{cx, y0 + 1, cz});
    }

    /** relief des murs : volets ouverts de chaque cote des fenetres, auvents, bandeaux saillants aux etages, contreforts d'angle, jardinieres, caisses */
    private void facade(int x0, int z0, int x1, int z1, int yf, int f, int fh, int doorSide, int variant) {
        for (int s = 0; s < 4; s++) {
            boolean along = s == 0 || s == 2;
            int o = s == 0 ? -1 : s == 2 ? 1 : 0, pq = s == 3 ? -1 : s == 1 ? 1 : 0;
            int a0 = along ? x0 : z0, a1 = along ? x1 : z1, wf = along ? (s == 0 ? z0 : z1) : (s == 3 ? x0 : x1);
            for (int a = a0 + 1; a < a1; a++) {
                int x = along ? a : wf, z = along ? wf : a;
                int w = gi(x, yf + 2, z);
                if (w == GLASS) {
                    for (int sd = -1; sd <= 1; sd += 2) {                                   // volets ouverts, a plat contre le mur
                        int sx = along ? x + sd : x, sz = along ? z : z + sd;
                        if ((along ? sx : sz) <= a0 || (along ? sx : sz) >= a1 || gi(sx, yf + 2, sz) == GLASS) continue;
                        setIfAir(sx + pq, yf + 2, sz + o, TRAPD + s);
                        setIfAir(sx + pq, yf + 3, sz + o, TRAPD + s);
                    }
                    for (int sd = -1; sd <= 1; sd++) setIfAir((along ? x + sd : x) + pq, yf + fh, (along ? z : z + sd) + o, slab(DKS, true));      // auvent
                }
                if (f > 0 && (s != doorSide) && !(a == a0 + 1 && false)) setIfAir(x + pq, yf, z + o, slab(DKS, true));                      // bandeau saillant sous l'etage
            }
        }
        for (int[] c : new int[][]{{x0, z0, -1, -1}, {x1, z0, 1, -1}, {x0, z1, -1, 1}, {x1, z1, 1, 1}})                                  // contreforts d'angle
            for (int y = yf + 1; y <= yf + fh; y++) { setIfAir(c[0] + c[2], y, c[1], WLOG_Y); setIfAir(c[0], y, c[1] + c[3], WLOG_Y); }
        if (f == 0 && (variant & 8) != 0) {                                                                                                 // caisses et tonneaux devant la maison
            int o = doorSide == 0 ? -1 : 1, cx = (x0 + x1) / 2, cz = doorSide == 0 ? z0 - 1 : z1 + 1;
            setIfAir(cx + 2, yf + 1, cz, BARREL); setIfAir(cx + 3, yf + 1, cz, HAY); setIfAir(cx - 2, yf + 1, cz, BARREL);
        }
    }

    /** interieur : lits, tapis, table et chaises, coffre, four, etabli, bibliotheque, lanterne ; porte au nord ou au sud (0 / 2) */
    private void furnish(int x0, int z0, int x1, int z1, int yf, int f, int floors, int fh, int doorSide, int variant) {
        int ix0 = x0 + 1, ix1 = x1 - 1, wi = ix1 - ix0 + 1;
        int di = doorSide == 2 ? 1 : -1, back = doorSide == 2 ? N : S_, front = doorSide == 2 ? S_ : N;
        if ((doorSide != 0 && doorSide != 2) || wi < 4) { setIfAir((x0 + x1) / 2, yf + 1, (z0 + z1) / 2, WFENCE); setIfAir((x0 + x1) / 2, yf + 2, (z0 + z1) / 2, LANTERN); return; }
        boolean blue = (variant & 1) != 0;
        int rows = z1 - z0 - 1;
        java.util.function.IntUnaryOperator zr = r -> doorSide == 2 ? z0 + 1 + r : z1 - 1 - r;       // rang r : 0 = fond de la piece
        int lad = doorSide == 0 ? 0 : -1;                                                           // l'echelle occupe le coin (x0 + 1, z1 - 1)
        int kmin = doorSide == 0 ? 1 : 0;
        if (f == 0) {
            int kb = wi - 1;                                                                        // lit contre le mur du fond, au bout est
            set(ix0 + kb, yf + 1, zr.applyAsInt(0), bed(back, true, blue));
            set(ix0 + kb, yf + 1, zr.applyAsInt(1), bed(back, false, blue));
            set(ix0 + kb - 1, yf + 1, zr.applyAsInt(0), CARPR);
            set(ix0 + kb - 1, yf + 1, zr.applyAsInt(1), CARPR);
            set(ix0 + Math.max(kmin, kb - 3), yf + 1, zr.applyAsInt(0), CHEST + front);
            set(ix0 + kmin, yf + 1, zr.applyAsInt(0), FURN + front);
            set(ix0 + kmin + 1, yf + 1, zr.applyAsInt(0), CRAFT);
            if (wi >= 7) set(ix0 + kmin + 2, yf + 1, zr.applyAsInt(0), BOOKS);
            int kt = wi / 2;                                                                        // table ronde et deux chaises
            set(ix0 + kt, yf + 1, zr.applyAsInt(2), slab(WS, false));                                 // la rangee de la porte reste libre : on entre et on gagne l'echelle sans escalader le mobilier
            set(ix0 + kt - 1, yf + 1, zr.applyAsInt(2), stair(WS, W_, false));
            set(ix0 + kt + 1, yf + 1, zr.applyAsInt(2), stair(WS, E, false));
            set(ix0 + kt, yf + 1, zr.applyAsInt(1), stair(WS, back, false));
            set(ix0 + kb, yf + 1, zr.applyAsInt(2), BARREL);
            set(ix0 + kmin, yf + 1, zr.applyAsInt(doorSide == 2 ? Math.min(rows - 2, 2) : 1), CAULD);
            set(ix0 + wi - 2, yf + 2, zr.applyAsInt(0), LANTERN);
        } else if (f < floors - 1 || floors == 1 || variant % 2 == 0) {                              // chambres
            for (int b = 0; b < 2; b++) {
                int kb = wi - 1 - b * 3;
                if (kb < kmin + 1) break;
                set(ix0 + kb, yf + 1, zr.applyAsInt(0), bed(back, true, (variant & 2) != 0 ^ b == 1));
                set(ix0 + kb, yf + 1, zr.applyAsInt(1), bed(back, false, (variant & 2) != 0 ^ b == 1));
                set(ix0 + kb - 1, yf + 1, zr.applyAsInt(0), b == 0 ? CARPB : CARPR);
            }
            set(ix0 + kmin, yf + 1, zr.applyAsInt(0), CHEST + front);
            set(ix0 + kmin + 1, yf + 1, zr.applyAsInt(0), BOOKS);
            set(ix0 + kmin + 1, yf + 2, zr.applyAsInt(0), BOOKS);
            set(ix0 + wi / 2, yf + 1, zr.applyAsInt(3), CARPR);
            set(ix0 + wi / 2 - 1, yf + 1, zr.applyAsInt(3), CARPR);
            set(ix0 + wi / 2, yf + 1, zr.applyAsInt(2), WFENCE);
            set(ix0 + wi / 2, yf + 2, zr.applyAsInt(2), LANTERN);
        } else {                                                                                    // grenier : reserves
            for (int k = 0; k < wi; k += 2) { set(ix0 + k, yf + 1, zr.applyAsInt(0), BARREL); if (k % 4 == 0) set(ix0 + k, yf + 2, zr.applyAsInt(0), HAY); }
            set(ix0 + wi / 2, yf + 1, zr.applyAsInt(2), WFENCE);
            set(ix0 + wi / 2, yf + 2, zr.applyAsInt(2), LANTERN);
        }
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
            if (towerBlock && row == 0 && half == 0) {
                if (i == -2 && j == 0) ballistaTower(xa + 1, za - 1, W_);                       // deux tours de guet a arbalete geante, vers l'exterieur du lac
                else if (i == 2 && j == 1) ballistaTower(xa + 1, za - 1, E);
                else tower(xa + 1, za - 1, 7, DK, 7, 2);
                continue;
            }
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
        for (int s = 0; s < 2; s++) if (rnd(i, j, 110 + s) < 0.55) { int lxs = x0 + 3 + (int) (rnd(i, j, 120 + s) * (x1 - x0 - 12)); if (Math.abs(lxs - xc) > 6 && Math.abs(lxs + 4 - xc) > 6) laundry(lxs, zs - 2 + s * 4, 4); }
        for (int s = 0; s < 4; s++) spawns.add(new int[]{x0 + 3 + (int) (rnd(i, j, 100 + s) * (x1 - x0 - 6)), DK + 1, zs});
    }

    // ------------------------------------------------------------------ armes : tours a arbalete geante
    private static final int[] DXA = {0, 1, 0, -1}, DZA = {-1, 0, 1, 0};

    /** local (u : devant, v : droite) vers x / z */
    private int lx(int cx, int dir, int u, int v) { return cx + DXA[dir] * u + DXA[(dir + 1) & 3] * v; }
    private int lz(int cz, int dir, int u, int v) { return cz + DZA[dir] * u + DZA[(dir + 1) & 3] * v; }

    /** tour de guet de six etages, terrasse en encorbellement et arbalete geante dressee vers dir */
    private void ballistaTower(int x0, int z0, int dir) {
        int size = 7, floors = 8, fh = 4, x1 = x0 + size - 1, z1 = z0 + size - 1, topY = DK + floors * fh;
        int cx = x0 + 3, cz = z0 + 3;
        house(x0, z0, x1, z1, DK, floors, fh, 0, 0, 2, 1);
        box(x0 - 3, topY + 1, z0 - 3, x1 + 3, topY + 24, z1 + 3, AIR);
        // pied : contreforts de pierre noire
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) {
            if (x != x0 - 1 && x != x1 + 1 && z != z0 - 1 && z != z1 + 1) continue;
            if ((x == x0 - 1 || x == x1 + 1) && (z == z0 - 1 || z == z1 + 1)) { for (int y = DK + 1; y <= DK + 5; y++) setIfAir(x, y, z, BLKB); continue; }
            if (z == z1 + 1 && Math.abs(x - (x0 + x1) / 2) <= 1) continue;                      // le seuil de la porte reste degage
            if ((x + z) % 2 == 0) for (int y = DK + 1; y <= DK + 2; y++) setIfAir(x, y, z, BLKB);
        }
        // terrasse 13 x 13 en encorbellement
        for (int u = -6; u <= 6; u++) for (int v = -6; v <= 6; v++) {
            int x = lx(cx, dir, u, v), z = lz(cz, dir, u, v);
            set(x, topY, z, (Math.abs(u) + Math.abs(v)) % 5 == 0 ? DKPLANK : SPPLANK);
            int m = Math.max(Math.abs(u), Math.abs(v));
            if (m >= 4 && m <= 5) set(x, topY - 1, z, DKPLANK);
            if (m == 5 && (Math.abs(u) == 5 || Math.abs(v) == 5) && (u + v) % 2 == 0) set(x, topY - 2, z, stair(DKS, dirOut(u, v), true));
            if (m == 6) { set(x, topY - 1, z, DKLOG_Y); }
        }
        // trappe : le haut de l'echelle debouche sur la terrasse
        set(x0 + 1, topY, z1 - 1, AIR);
        set(x0 + 1, topY, z1 - 1, ladder(N));
        for (int u = -6; u <= 6; u++) for (int v = -6; v <= 6; v++) {                          // parapet de bois sombre, poteaux et lanternes
            int m = Math.max(Math.abs(u), Math.abs(v));
            if (m != 6) continue;
            int x = lx(cx, dir, u, v), z = lz(cz, dir, u, v);
            boolean post = (Math.abs(u) + Math.abs(v)) % 4 == 0 || (Math.abs(u) == 6 && Math.abs(v) == 6);
            set(x, topY + 1, z, post ? DKLOG_Y : DKFENCE);
            if (post) { set(x, topY + 2, z, DKFENCE); set(x, topY + 3, z, LANTERN); }
            else set(x, topY + 2, z, DKFENCE);
        }
        // grande arbalete : piedestal, arc et corde de chaine
        boolean axX = dir == E || dir == W_;
        int stock = axX ? DKLOG_X : DKLOG_Z, bar = axX ? DKLOG_Z : DKLOG_X;
        for (int y = topY + 1; y <= topY + 2; y++) set(cx, y, cz, DKLOG_Y);
        for (int v = -1; v <= 1; v += 2) { for (int y = topY + 1; y <= topY + 2; y++) set(lx(cx, dir, -3, v), y, lz(cz, dir, -3, v), DKFENCE); }
        for (int y = topY + 1; y <= topY + 2; y++) set(lx(cx, dir, 3, 0), y, lz(cz, dir, 3, 0), DKFENCE);
        int sy = topY + 3, by = topY + 4;
        for (int u = -4; u <= 5; u++) set(lx(cx, dir, u, 0), sy, lz(cz, dir, u, 0), stock);
        for (int u = -4; u <= -3; u++) { set(lx(cx, dir, u, 1), sy, lz(cz, dir, u, 1), DKPLANK); set(lx(cx, dir, u, -1), sy, lz(cz, dir, u, -1), DKPLANK); }   // crosse
        for (int v = -3; v <= 3; v++) { set(lx(cx, dir, 4, v), by, lz(cz, dir, 4, v), bar); set(lx(cx, dir, 4, v), sy, lz(cz, dir, 4, v), v == 0 ? stock : AIR); }
        for (int sg = -1; sg <= 1; sg += 2) {                                                   // bras recourbes vers l'arriere
            set(lx(cx, dir, 3, sg * 4), by, lz(cz, dir, 3, sg * 4), bar);
            set(lx(cx, dir, 3, sg * 5), by, lz(cz, dir, 3, sg * 5), DKLOG_Y);
            set(lx(cx, dir, 2, sg * 5), by, lz(cz, dir, 2, sg * 5), DKLOG_Y);
            set(lx(cx, dir, 2, sg * 6), by, lz(cz, dir, 2, sg * 6), DKPLANK);
            // corde : de la pointe du bras a l'encoche
            int ux = 2, vx = sg * 6, un = -1, vn_ = 0, n = Math.max(Math.abs(ux - un), Math.abs(vx - vn_));
            for (int k = 0; k <= n; k++) {
                int u = ux + (un - ux) * k / n, v = vx + (vn_ - vx) * k / n;
                int x = lx(cx, dir, u, v), z = lz(cz, dir, u, v);
                setIfAir(x, by, z, (axX ^ (Math.abs(vn_ - vx) > Math.abs(un - ux))) ? CHAINX : CHAINZ);
            }
        }
        arrow(cx, cz, dir, 0, by, -3, 6, 1);                                                      // carreau charge, 10 blocs
        // carreaux en reserve, couches sur la terrasse, de chaque cote
        for (int sg = -1; sg <= 1; sg += 2) {
            arrow(cx, cz, dir, sg * 3, topY + 1, -4, 5, 1);
            arrow(cx, cz, dir, sg * 4, topY + 1, -4, 5, 1);
            arrow(cx, cz, dir, sg * 3, topY + 2, -4, 5, 1);
            // chevalet de pose
            for (int u : new int[]{-3, 3}) for (int v : new int[]{sg * 3, sg * 4}) set(lx(cx, dir, u, v), topY + 1, lz(cz, dir, u, v), DKPLANK);
        }
        // fanion noir
        int fx = lx(cx, dir, -5, 5), fz = lz(cz, dir, -5, 5);
        for (int y = topY + 3; y <= topY + 9; y++) set(fx, y, fz, DKFENCE);
        for (int u = 1; u <= 3; u++) for (int y = topY + 7; y <= topY + 8; y++) set(lx(fx, dir, u, 0) - 0, y, lz(fz, dir, u, 0), BLKWOOL);
        // carquois au pied de la tour : tonneaux d'ou sortent de grands carreaux dresses
        for (int k = 0; k < 2; k++) {
            int bx = k == 0 ? cx - 3 : cx + 3, bz = cz + 5;
            if (get(bx, DK, bz) == AIR) continue;
            set(bx, DK + 1, bz, BARREL);
            arrowUp(bx, DK + 2, bz, 5);
        }
        for (int s = 0; s < 3; s++) spawns.add(new int[]{cx, topY + 1, cz});
        reserve(cx - 7, cz - 7, cx + 7, cz + 7, 2);
    }

    private int dirOut(int u, int v) { return Math.abs(u) >= Math.abs(v) ? (u > 0 ? E : W_) : (v > 0 ? S_ : N); }

    /**
     * grand carreau noir couche : axe a la distance v du centre, de u0 a u1 (fut de mur de pierre noire, penne de laine noire, pointe de pierre noire).
     */
    private void arrow(int cx, int cz, int dir, int v, int y, int u0, int u1, int unused) {
        boolean axX = dir == E || dir == W_;
        for (int u = u0; u <= u1; u++) {
            int x = lx(cx, dir, u, v), z = lz(cz, dir, u, v);
            int id;
            if (u <= u0 + 1) id = BLKWOOL;                                                       // penne
            else if (u >= u1 - 1) id = BLKB;                                                     // pointe
            else id = axX ? BWX : BWZ;
            if (u == u1) id = BKS + 8;                                                           // pointe : dalle de pierre noire
            if (u <= u0 + 1) { set(x, y, z, id); if (u == u0) { set(lx(cx, dir, u, v + 1), y, lz(cz, dir, u, v + 1), id); set(lx(cx, dir, u, v - 1), y, lz(cz, dir, u, v - 1), id); set(x, y + 1, z, id); } }
            else set(x, y, z, id);
        }
        // tete elargie : deux ailerons
        for (int sg = -1; sg <= 1; sg += 2) set(lx(cx, dir, u1 - 2, v + sg), y, lz(cz, dir, u1 - 2, v + sg), BKS + 8);
    }

    /** carreau dresse (poteau de pierre noire, penne en bas, pointe en haut) */
    private void arrowUp(int x, int y, int z, int h) {
        for (int k = 0; k < h; k++) set(x, y + k, z, k == 0 ? BLKWOOL : BWP);
        set(x, y + h, z, BLKB);
        set(x, y + h + 1, z, BKS + 8);
    }

    private void hallFurniture(int hx0, int hz0, int hx1, int hz1, int hy) {
        for (int t = 0; t < 2; t++) {
            int z = hz0 + 4 + t * 6;
            for (int x = hx0 + 5; x <= hx1 - 5; x++) {
                set(x, hy + 1, z, slab(DKS, false));
                if ((x - hx0) % 3 == 0) { set(x, hy + 1, z - 1, stair(WS, N, false)); set(x, hy + 1, z + 1, stair(WS, S_, false)); set(x + 1, hy + 1, z - 1, stair(WS, N, false)); set(x + 1, hy + 1, z + 1, stair(WS, S_, false)); }
                if ((x - hx0) % 6 == 2) { set(x, hy + 2, z, WFENCE); set(x, hy + 3, z, LANTERN); }
                if ((x - hx0) % 5 == 1) set(x, hy + 2, z, rnd(x, z, 77) < 0.5 ? PLATE : AIR);
            }
        }
        for (int x = hx0 + 5; x <= hx1 - 5; x += 6) { set(x, hy + 4, hz0 + 7, CHAIN); set(x, hy + 3, hz0 + 7, CHAIN); set(x, hy + 2, hz0 + 7, HLANT); }
        for (int x = hx0 + 2; x <= hx1 - 2; x += 5) { set(x, hy + 1, hz0 + 1, BARREL); }
        set((hx0 + hx1) / 2 - 1, hy + 1, hz0 + 1, stair(DKS, S_, false)); set((hx0 + hx1) / 2, hy + 1, hz0 + 1, stair(DKS, S_, false));        // sieges du maitre de la ville
        for (int x = hx0 + 3; x < hx1 - 2; x += 4) { set(x, hy + 1, hz0 + 13, CHEST + N); }
    }

    // ------------------------------------------------------------------ la peche
    /** ponton de pecheurs : jusqu'a deux, sur les rives libres du lac, avec cabane, sechoirs a filets, feu, barque retournee et lignes */
    private void fishing() {
        List<int[]> done = new ArrayList<>();
        for (int len = 14; len >= 8 && done.size() < 2; len -= 3) {
            for (int a = 0; a < 72 && done.size() < 2; a++) {
                double th0 = Math.toRadians(a * 5 + 12);
                int sx = -1, sz = -1;
                for (int r = 40; r < 190; r++) {
                    int x = (int) Math.round(CX + r * Math.cos(th0)), z = (int) Math.round(CZ + r * Math.sin(th0));
                    if (!area(x, z)) break;
                    boolean wet = wl[z * W + x] > -999;
                    if (wet) { sx = x; sz = z; continue; }
                    if (sx >= 0 && th[z * W + x] >= 0) break;
                }
                if (sx < 0) continue;
                double ca = Math.cos(th0), sa = Math.sin(th0);
                int d = Math.abs(ca) > Math.abs(sa) ? (ca > 0 ? W_ : E) : (sa > 0 ? N : S_);
                int dx = DXA[d], dz = DZA[d], px = DXA[(d + 1) & 3], pz = DZA[(d + 1) & 3];
                boolean good = true;
                for (int k = -2; k <= len + 6 && good; k++) for (int w = -5; w <= 5 && good; w++) {
                    int x = sx + dx * k + px * w, z = sz + dz * k + pz * w;
                    if (!area(x, z) || res[z * W + x]) { good = false; break; }
                    if (k >= 1 && (wl[z * W + x] <= -999 || th[z * W + x] > -2)) { good = false; break; }
                    for (int y = 0; y <= 6; y++) if (k >= 0 && get(x, y, z) != AIR && get(x, y, z) != WATER) { good = false; break; }
                }
                if (!good) continue;
                boolean far = true;
                for (int[] o : done) if (Math.hypot(o[0] - sx, o[1] - sz) < 50) far = false;
                if (!far) continue;
                done.add(new int[]{sx, sz});
                quay(sx, sz, d, len);
            }
        }
    }

    private void quay(int sx, int sz, int d, int len) {
        int dx = DXA[d], dz = DZA[d], px = DXA[(d + 1) & 3], pz = DZA[(d + 1) & 3];
        java.util.function.IntBinaryOperator X = (k, w) -> sx + dx * k + px * w, Z = (k, w) -> sz + dz * k + pz * w;
        int xmn = Integer.MAX_VALUE, xmx = -1, zmn = Integer.MAX_VALUE, zmx = -1;
        for (int k = -14; k <= len + 6; k++) for (int w = -8; w <= 8; w++) { int x = X.applyAsInt(k, w), z = Z.applyAsInt(k, w); xmn = Math.min(xmn, x); xmx = Math.max(xmx, x); zmn = Math.min(zmn, z); zmx = Math.max(zmx, z); }
        reserve(xmn, zmn, xmx, zmx, 0);
        // ponton
        for (int k = -1; k <= len; k++) {
            int hw = k <= 5 ? 2 : 1;
            for (int w = -hw; w <= hw; w++) {
                int x = X.applyAsInt(k, w), z = Z.applyAsInt(k, w);
                int t = tAt(x, z);
                if (k <= 0 && t >= 1) continue;
                set(x, 0, z, ((k + w) & 3) == 0 ? DKPLANK : WPLANK);
                for (int y = 1; y <= 3; y++) if (get(x, y, z) == WATER) set(x, y, z, AIR);
                if (k % 4 == 0 && Math.abs(w) == hw) pile(x, z, -1);
            }
        }
        // plateforme terminale en T
        for (int k = len + 1; k <= len + 4; k++) for (int w = -4; w <= 4; w++) {
            int x = X.applyAsInt(k, w), z = Z.applyAsInt(k, w);
            set(x, 0, z, ((k * 3 + w) & 3) == 0 ? DKPLANK : WPLANK);
            if (((k - len) % 3 == 1 && Math.abs(w) == 4) || (k == len + 4 && w % 4 == 0)) pile(x, z, -1);
        }
        for (int k = len + 1; k <= len + 4; k++) for (int w = -4; w <= 4; w++) {
            boolean edge = k == len + 4 || Math.abs(w) == 4;
            if (!edge) continue;
            int x = X.applyAsInt(k, w), z = Z.applyAsInt(k, w);
            if (k == len + 4 && (w == -3 || w == 0 || w == 3)) {                                  // cannes a peche : ligne de chaine et flotteur rouge
                set(x, 1, z, DKFENCE); set(x, 2, z, DKFENCE);
                int ox = X.applyAsInt(k + 1, w), oz = Z.applyAsInt(k + 1, w);
                set(ox, 3, oz, DKFENCE); set(ox, 2, oz, CHAIN); set(ox, 1, oz, CHAIN); set(ox, 0, oz, CHAIN); set(ox, -1, oz, WOOL_R);
                continue;
            }
            set(x, 1, z, DKFENCE);
            if (Math.abs(w) == 4 && k == len + 2) { set(x, 2, z, DKFENCE); set(x, 3, z, LANTERN); }
        }
        // siege de pecheur, seaux et paniers
        for (int sg = -1; sg <= 1; sg += 2) {
            set(X.applyAsInt(len + 3, sg * 2), 1, Z.applyAsInt(len + 3, sg * 2), BARREL);
            set(X.applyAsInt(len + 2, sg * 2), 1, Z.applyAsInt(len + 2, sg * 2), CAULD);
        }
        set(X.applyAsInt(len + 2, 1), 1, Z.applyAsInt(len + 2, 1), HAY);
        set(X.applyAsInt(len + 3, 0), 1, Z.applyAsInt(len + 3, 0), stair(WS, (d + 2) & 3, false));
        // sechoirs a filets le long de la partie large
        for (int sg = -1; sg <= 1; sg += 2) {
            int xa = X.applyAsInt(1, sg * 2), za = Z.applyAsInt(1, sg * 2), xb = X.applyAsInt(5, sg * 2), zb = Z.applyAsInt(5, sg * 2);
            for (int y = 1; y <= 3; y++) { set(xa, y, za, DKFENCE); set(xb, y, zb, DKFENCE); }
            for (int k = 2; k <= 4; k++) { int x = X.applyAsInt(k, sg * 2), z = Z.applyAsInt(k, sg * 2); set(x, 3, z, DKFENCE); set(x, 2, z, WEB); }
        }
        // cabane de pecheur sur la rive, porte vers le ponton (lateral si le mur est est-ouest)
        int hxc = X.applyAsInt(-8, 0), hzc = Z.applyAsInt(-8, 0);
        int hx0 = hxc - 3, hz0 = hzc - 3, hx1 = hx0 + 6, hz1 = hz0 + 5;
        int base = 0;
        for (int x = hx0 - 1; x <= hx1 + 1; x++) for (int z = hz0 - 1; z <= hz1 + 1; z++) base = Math.max(base, tAt(x, z));
        base = Math.min(base, 6);
        for (int x = hx0 - 1; x <= hx1 + 1; x++) for (int z = hz0 - 1; z <= hz1 + 1; z++) { for (int y = Math.max(tAt(x, z), -OFF + 1) + 1; y <= base; y++) set(x, y, z, WLOG_Y); set(x, base, z, WPLANK); }
        int doorSide = (d == N || d == S_) ? ((d == N) ? 0 : 2) : 2;
        house(hx0, hz0, hx1, hz1, base, 2, 4, 1, 0, doorSide, 0x1b);
        // chemin de planches de la cabane au ponton
        for (int k = -7; k <= 0; k++) for (int w = -1; w <= 1; w++) { int x = X.applyAsInt(k, w), z = Z.applyAsInt(k, w), t = tAt(x, z); if (get(x, t + 1, z) == AIR && t >= 0 && !(x >= hx0 && x <= hx1 && z >= hz0 && z <= hz1)) set(x, t, z, WPLANK); }
        // abords : feu, bancs, barque retournee, tonneaux, sechoir et linge
        int fx = X.applyAsInt(-4, 5), fz = Z.applyAsInt(-4, 5), ft = tAt(fx, fz);
        set(fx, ft + 1, fz, CAMP);
        for (int k = 0; k < 4; k++) { int bx = fx + DXA[k], bz = fz + DZA[k]; set(bx, tAt(bx, bz) + 1, bz, stair(WS, (k + 2) & 3, false)); }
        int bx0 = X.applyAsInt(-5, -5), bz0 = Z.applyAsInt(-5, -5);
        for (int a = 0; a < 7; a++) for (int b = 0; b < 3; b++) {
            int x = d == E || d == W_ ? bx0 + a : bx0 + b, z = d == E || d == W_ ? bz0 + b : bz0 + a;
            int t = tAt(x, z);
            if (t < 0) continue;
            int hh = (a == 0 || a == 6) ? 0 : 1;
            if (hh == 0 && b != 1) continue;
            set(x, t + 1, z, b == 1 ? SPPLANK : slab(SPS, false));
            if (b == 1 && a > 0 && a < 6) set(x, t + 2, z, slab(SPS, false));
        }
        for (int k = 0; k < 3; k++) { int x = X.applyAsInt(-6 - k, 4), z = Z.applyAsInt(-6 - k, 4); set(x, tAt(x, z) + 1, z, k == 1 ? HAY : BARREL); }
        for (int sg = -1; sg <= 1; sg += 2) {                                                         // sechoir terrestre
            int xa = X.applyAsInt(-3, sg * 6), za = Z.applyAsInt(-3, sg * 6), xb = X.applyAsInt(1, sg * 6), zb = Z.applyAsInt(1, sg * 6);
            for (int y = 1; y <= 3; y++) { set(xa, tAt(xa, za) + y, za, DKFENCE); set(xb, tAt(xb, zb) + y, zb, DKFENCE); }
            for (int k = -3; k <= 1; k++) { int x = X.applyAsInt(k, sg * 6), z = Z.applyAsInt(k, sg * 6), t = tAt(x, z); if (k > -3 && k < 1) { set(x, t + 3, z, DKFENCE); set(x, t + 2, z, WEB); set(x, t + 1, z, k == -1 ? HAY : AIR); } }
        }
        int lxa = X.applyAsInt(-12, -2), lza = Z.applyAsInt(-12, -2), lxb = X.applyAsInt(-12, 4), lzb = Z.applyAsInt(-12, 4);                    // corde a linge
        for (int y = 1; y <= 4; y++) { set(lxa, tAt(lxa, lza) + y, lza, DKFENCE); set(lxb, tAt(lxb, lzb) + y, lzb, DKFENCE); }
        for (int w = -1; w <= 3; w++) { int x = X.applyAsInt(-12, w), z = Z.applyAsInt(-12, w); int t = tAt(x, z); set(x, t + 4, z, w % 2 == 0 ? WOOL_W : WOOL_B); set(x, t + 3, z, w % 2 == 0 ? WOOL_R : AIR); }
        // barques amarrees de chaque cote, voile pour l'une
        for (int sg = -1; sg <= 1; sg += 2) {
            int cx = X.applyAsInt(len - 3, sg * 4), cz = Z.applyAsInt(len - 3, sg * 4);
            boolean ax0 = dx != 0;
            boatAt(ax0 ? cx - 3 : cx - 1, ax0 ? cz - 1 : cz - 3, ax0 ? 0 : 1, sg > 0);
        }
        spawns.add(new int[]{X.applyAsInt(len + 3, 1), 1, Z.applyAsInt(len + 3, 1)});
        spawns.add(new int[]{X.applyAsInt(len + 2, -2), 1, Z.applyAsInt(len + 2, -2)});
        spawns.add(new int[]{fx, tAt(fx, fz) + 1, fz});
    }

    private void laundry(int x0, int z, int len) {
        for (int y = DK + 1; y <= DK + 4; y++) { setIfAir(x0, y, z, DKFENCE); setIfAir(x0 + len, y, z, DKFENCE); }
        for (int x = x0 + 1; x < x0 + len; x++) { setIfAir(x, DK + 4, z, DKFENCE); setIfAir(x, DK + 3, z, (x - x0) % 2 == 0 ? WOOL_W : (x % 3 == 0 ? WOOL_R : WOOL_B)); }
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
        furnish = false;
        house(hx0, hz0, hx1, hz1, hy, 3, 5, 0, 0, 2, 0);
        furnish = true;
        hallFurniture(hx0, hz0, hx1, hz1, hy);
        for (int y = hy + 1; y <= hy + 3; y++) for (int x = cx - 3; x <= cx; x++) set(x, y, hz1, AIR);                  // grande porte a deux battants
        for (int x : new int[]{cx - 4, cx + 1}) for (int y = hy + 1; y <= hy + 4; y++) set(x, y, hz1, DKLOG_Y);
        for (int x = cx - 4; x <= cx + 1; x++) set(x, hy + 4, hz1, DKPLANK);
        for (int x = cx - 3; x <= cx; x++) set(x, hy + 3, hz1, GLASS);
        // grande porte : portail ouvert de quatre blocs de large (pas de battants a moitie poses)
        // grands vitrages sur la facade
        for (int k = 0; k < 5; k++) { int x = hx0 + 3 + k * 6; if (Math.abs(x - cx + 2) <= 3) continue; for (int y = hy + 7; y <= hy + 10; y++) set(x, y, hz1, GLASS); for (int y = hy + 2; y <= hy + 4; y++) set(x, y, hz1, GLASS); }
        // porche : quatre poteaux et un auvent
        for (int x = cx - 7; x <= cx + 4; x += 11) for (int z = hz1 + 4; z <= hz1 + 4; z++) for (int y = hy + 1; y <= hy + 5; y++) set(x, y, z, DKLOG_Y);
        for (int x = cx - 8; x <= cx + 5; x++) for (int z = hz1 + 1; z <= hz1 + 5; z++) set(x, hy + 6, z, z == hz1 + 5 ? stair(DKS, N, true) : DKPLANK);
        for (int x = cx - 7; x <= cx + 4; x += 11) set(x, hy + 7, hz1 + 4, LANTERN);
        // tour a fleche, accolee a la halle
        tower(hx1 + 1, hz0, 8, hy, 8, 2);
        for (int x = hx1 + 3; x <= hx1 + 7; x++) {                                                              // perron devant la porte de la tour : deux rangs de plancher, puis un escalier vers la place
            int zt = hz0 + 8;
            for (int z = zt; z <= zt + 1; z++) { set(x, DK + 1, z, WLOG_Y); set(x, DK + 2, z, WPLANK); }
            set(x, DK + 1, zt + 2, WLOG_Y); set(x, DK + 2, zt + 2, stair(WS, N, false)); set(x, DK + 1, zt + 3, stair(WS, N, false));
        }
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

    private void boatAt(int x, int z, int ax) { boatAt(x, z, ax, rnd(x, z, 41) < 0.22); }

    private void boatAt(int x, int z, int ax, boolean sail) {                                               // barque de 7 x 3 au ras de l'eau : coque de planches, bordages bas
        int len = 7, wd = 3;
        for (int a = 0; a < len; a++) for (int b = 0; b < wd; b++) {
            int xx = ax == 0 ? x + a : x + b, zz = ax == 0 ? z + b : z + a;
            if (!area(xx, zz) || get(xx, -1, zz) != WATER || get(xx, 0, zz) != AIR || get(xx, 1, zz) != AIR || get(xx, DK, zz) != AIR || get(xx, DK + 1, zz) != AIR || get(xx, DK + 4, zz) != AIR) return;
        }
        for (int a = 0; a < len; a++) for (int b = 0; b < wd; b++) {
            int xx = ax == 0 ? x + a : x + b, zz = ax == 0 ? z + b : z + a;
            boolean side = b == 0 || b == wd - 1 || a == 0 || a == len - 1;
            if ((a == 0 || a == len - 1) && b != 1) continue;
            set(xx, -1, zz, SPPLANK);
            if (side) set(xx, 0, zz, a == 0 || a == len - 1 ? SPPLANK : slab(SPS, false));
        }
        int mx = ax == 0 ? x + 3 : x + 1, mz = ax == 0 ? z + 1 : z + 3;
        if (sail) {                                                                                  // mat et voile blanche, rien d'autre
            for (int y = 0; y <= 5; y++) set(mx, y, mz, y == 0 ? DKLOG_Y : DKFENCE);
            for (int a = -1; a <= 1; a++) for (int y = 2; y <= 4; y++) { int sx = ax == 0 ? mx + a : mx, sz = ax == 0 ? mz : mz + a; set(sx, y, sz, WOOL_W); }
        } else if (rnd(x, z, 5) < 0.5) set(ax == 0 ? x + 5 : x + 1, 0, ax == 0 ? z + 1 : z + 5, BARREL);   // une barrique a l'arriere, parfois
    }

    private void boats() {
        for (int[] bs : boatSpots) boatAt(bs[0], bs[1], bs[2]);
        List<int[]> placed = new ArrayList<>();
        for (int k = 0; k < 900 && placed.size() < 14; k++) {                                 // quelques barques amarrees dans les canaux, bien espacees
            int x = 20 + (int) (rnd(k, 3, 7) * (W - 40)), z = 20 + (int) (rnd(k, 4, 8) * (D - 40));
            if (!wetAt(x, z) || Math.hypot(x - CX, z - CZ) > 90) continue;
            boolean near = false;
            for (int[] q : placed) if (Math.hypot(q[0] - x, q[1] - z) < 24) { near = true; break; }
            if (near) continue;
            boatAt(x, z, hash(k, 5, 9) & 1);
            if (get(x + 1, -1, z + 1) == SPPLANK) placed.add(new int[]{x, z});
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
