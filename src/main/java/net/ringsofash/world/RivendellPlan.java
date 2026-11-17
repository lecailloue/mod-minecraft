package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan de Fondcombe (Rivendell) : 352 x 352 blocs. Une haute falaise au nord, trois etages de roche (la vallee, la grande terrasse, un gradin) et une gorge a l'est ou la plus grande
 * des cascades tombe de plus de cent blocs dans un bassin ; trois autres cascades descendent par gradins. Sur la terrasse : la maison d'Elrond (longue salle aux toits de tuiles
 * orange, vert-de-gris et or, trois hauts pignons, tour centrale de cinq etages), la tour grise a toit vert, l'aiguille doree, la maison d'etude, la pagode a quatre etages, le gradin
 * de la salle du Feu, la terrasse du Conseil et son grand arbre, des gloriettes et deux pavillons de pont ; une grande arche franchit la gorge ; au pied de la falaise, une arcade
 * a deux niveaux sur de hautes colonnes. Grille de blocs abstraits, sans dependance a Minecraft.
 * Repere local : x de 0 a W - 1 (ouest a est), z de 0 a D - 1 (nord a sud) ; y = 0 : niveau de la vallee (le sol au centre du site) ; S : la grande terrasse.
 */
public final class RivendellPlan {
    public static final int W = 352, D = 352, OFF = 30, TOP = 150, H = OFF + TOP + 1, CX = 176, CZ = 176;
    public static final int S = 32, T2 = 56, T3 = 112;   // grande terrasse, gradin, plateau de la falaise
    public static final int BAND = 30;                  // largeur de la bande de raccord avec le relief naturel, tout autour

    // terrain et vegetation
    public static final int AIR = 0, STONE = 1, ANDESITE = 2, TUFF = 3, CALCITE = 4, DIORITE = 5, COBBLE = 6, MOSSC = 7, DIRT = 8, GRASS = 9, PODZOL = 10, GRAVEL = 11,
            SAND = 12, COARSE = 13, MOSS = 14, WATER = 15, WATERF = 16, SHORTG = 17, FERN = 18, POPPY = 19, DANDEL = 20, CORNFL = 21, BLUET = 22, SPRLOG = 23, SPRLEAF = 24;
    // nouveaux blocs du mod
    public static final int PLASTER = 25, TIMBER = 26, TSTONE = 27, TILE_O = 28, TILE_G = 29, TILE_Y = 30, FILIG = 31;
    // architecture
    public static final int WB = 32, WS = 33, WPIL = 34, QB = 35, SQ = 36, GP = 37, GLOG_Y = 38, GLOG_X = 39, GLOG_Z = 40, GOLDB = 41, GLASS = 42, LADE = 43, LANTERN = 44,
            ENDROD = 45, GFENCE = 46, WWALL = 47, CHAIN = 48, HLANT = 49, LEAF_G = 50, LEAF_Y = 51, LEAF_O = 52, LEAF_R = 53, LEAF_B = 54, LADW = 55, LADS = 56, LADN = 57,
            STB = 58, MOSSB = 59, PAVE2 = 60, TWALL = 61, WLOG = 62, BENCHW = 63;
    // familles d'escaliers : base + direction * 2 + moitie haute (direction = sens de la montee : 0 N, 1 E, 2 S, 3 O) ; base + 8 dalle basse, base + 9 dalle haute
    public static final int WBS = 64, GPS = 76, TOS = 88, TGS = 100, TYS = 112, PLS = 124, TSS = 136, STBS = 148;
    public static final int N = 0, E = 1, S_ = 2, W_ = 3;

    private final byte[] g = new byte[W * H * D];
    private final int[] th = new int[W * D];                       // y du dernier bloc plein du terrain
    private final short[] wl = new short[W * D];                   // niveau de l'eau (sommet), ou -999
    private final byte[] edge = new byte[W * D];                   // 0 en bordure (relief naturel), 255 au coeur : poids du plan
    private final byte[] ramp = new byte[W * D];                   // 1 + direction de montee des escaliers creuses dans le relief, 0 sinon
    private final boolean[] res = new boolean[W * D];              // reserve (batiments) : pas d'arbres
    private final int[] top = new int[W * D];
    public final List<int[]> spawns = new ArrayList<>();           // {x, y, z}
    private final List<int[]> curtains = new ArrayList<>();        // {x, y, z} : eau tombante des cascades

    private static RivendellPlan inst;
    public static synchronized RivendellPlan get() { if (inst == null) inst = new RivendellPlan(); return inst; }

    // ------------------------------------------------------------------ outils
    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public static final int AIRC = 158;                            // air creuse par l'erosion (ne doit pas etre rebouche par du sol)
    public byte raw(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    public byte get(int x, int y, int z) { byte v = raw(x, y, z); return v == (byte) AIRC ? AIR : v; }
    private int gi(int x, int y, int z) { return get(x, y, z) & 255; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    private void setIfAir(int x, int y, int z, int id) { if (ok(x, y, z) && (g[idx(x, y, z)] == AIR || g[idx(x, y, z)] == (byte) AIRC)) g[idx(x, y, z)] = (byte) id; }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int top(int x, int z) { return top[z * W + x]; }
    public int terrain(int x, int z) { return th[z * W + x]; }
    public int water(int x, int z) { return wl[z * W + x]; }
    public int weight(int x, int z) { return edge[z * W + x] & 255; }
    private int tAt(int x, int z) { return area(x, z) ? th[z * W + x] : -999; }

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

    private void box(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, id);
    }
    private void put(boolean ax, int a, int f, int y, int id) { if (ax) set(a, y, f, id); else set(f, y, a, id); }
    private void put2(int axis, int perp, int len, int y, int id) { if (axis == 0) set(len, y, perp, id); else set(perp, y, len, id); }
    private void reserve(int x0, int z0, int x1, int z1, int m) {
        for (int x = x0 - m; x <= x1 + m; x++) for (int z = z0 - m; z <= z1 + m; z++) if (area(x, z)) res[z * W + x] = true;
    }
    private void found(int x0, int z0, int x1, int z1, int yTop, int id) {          // comble sous un batiment jusqu'au terrain
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (!area(x, z)) continue;
            for (int y = th[z * W + x] + 1; y <= yTop; y++) if (get(x, y, z) == AIR || get(x, y, z) == WATER) set(x, y, z, id);
        }
    }

    // ------------------------------------------------------------------ geometrie
    private static double zA(double x) { return 160 + 5 * vn(x / 23.0, 3.3, 21); }       // bord sud de la terrasse
    private static double zB(double x) { return 102 + 6 * vn(x / 31.0, 7.7, 22) + 3 * vn(x / 9.0, 2.2, 42); }       // pied du gradin
    private static double zC(double x) {                                                  // pied de la haute falaise (constant face a la grande cascade)
        if (x > 236 && x < 288) x = 262;
        double z = 66 + 6 * vn(x / 27.0, 1.1, 23) + 3 * vn(x / 8.0, 4.4, 43);
        return Math.min(z, zB(x) - 26);
    }
    private static double cxW(double z) { return 232 + 6 * vn(z / 23.0, 5.1, 26) + 2.5 * vn(z / 7.0, 1.5, 44); }       // parois de la gorge
    private static double cxE(double z) { return 290 + 6 * vn(z / 23.0, 9.2, 27) + 2.5 * vn(z / 7.0, 8.5, 45); }
    private static double rid(double x, double z, int seed) { double v = 1 - Math.abs(vn(x, z, seed)); return v * v; }
    private static final double[][] KEEP = {{124, 220}, {145, 203}, {205, 218}, {190, 256}, {168, 180}, {176, 205}, {150, 175}, {46, 175}};
    private static double flat(double x, double z) {                                      // 0 pres des batiments de la vallee, 1 loin
        double m = 1e9;
        for (double[] k : KEEP) m = Math.min(m, Math.hypot(x - k[0], z - k[1]));
        return sm((m - 12) / 26.0);
    }
    private static final double[][] RIV = {{262, 100}, {262, 150}, {262, 185}, {248, 225}, {212, 250}, {160, 262}, {100, 258}, {55, 280}, {22, 325}, {5, 352}};
    private static final double POOL_X = 262, POOL_Z = 88, POOL_R = 23;

    private static double dist(double x, double z, double[][] poly) {
        double best = 1e9;
        for (int i = 1; i < poly.length; i++) {
            double ax = poly[i - 1][0], az = poly[i - 1][1], bx = poly[i][0], bz = poly[i][1];
            double dx = bx - ax, dz = bz - az, t = Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz)));
            double d = Math.hypot(x - (ax + dx * t), z - (az + dz * t));
            if (d < best) best = d;
        }
        return best;
    }

    // ------------------------------------------------------------------ construction
    public RivendellPlan() {
        java.util.Arrays.fill(wl, (short) -999);
        terrain();
        carve();
        materialize();
        cascades();
        stairsOnRamps();
        erode();
        mainHall();
        bellTower();
        needle();
        studyHouse();
        pagoda();
        fireHall();
        arcade();
        council();
        gazebos();
        bridges();
        valley();
        promenade();
        gardens();
        decorate();
        trees();
        drape();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR && g[idx(x, y, z)] != (byte) AIRC) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    /** hauteur d'une marche de roche : sur la face (largeur w, noyee de ledges), du cote haut seulement, pour que la terrasse reste plane */
    private static double face(double zEdge, double z, double lower, double upper, double w, double x) {
        if (z >= zEdge) return lower;
        double t = (zEdge - z) / w;
        double sv = sm(t);
        return lower + (upper - lower) * sv + 5 * vn(x / 5.0, z / 5.0, 33) * 4 * sv * (1 - sv);
    }

    private void terrain() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            double xw = x + 0.5, zw = z + 0.5, zA = zA(xw), zB = zB(xw), zC = zC(xw);
            double nearFall = Math.min(Math.min(Math.abs(xw - 262) / 14.0, Math.abs(xw - 150) / 6.0), Math.abs(xw - 46) / 6.0);
            double w = 1.0 + 10.0 * sm(nearFall - 0.4) * (0.6 + 0.4 * (vn(xw / 17.0, 6.6, 46) + 1) / 2);   // falaise droite pres des cascades, plus couchee ailleurs
            double side = Math.max(0, 60 - xw) + Math.max(0, xw - 296);
            double sideK = 0.9 + 0.35 * vn(zw / 25.0, xw / 30.0, 47);
            double h;
            boolean valley = false;
            if (zw >= zA) {
                valley = true;
                h = side * (0.45 + 0.2 * vn(zw / 31.0, xw / 29.0, 48));
                double fl = flat(xw, zw);
                h += fl * sm((zw - zA - 12) / 24.0) * (3.5 * vn(xw / 38.0, zw / 38.0, 36) + 1.5 * vn(xw / 13.0, zw / 13.0, 37));
                if (zw < zA + 11) h += 3.2 * (1 - (zw - zA) / 11.0) * (0.55 + 0.45 * vn(xw / 4.0, zw / 4.0, 34));       // eboulis au pied de la falaise
            } else {
                double lowerS = S + side * sideK, lowerT2 = T2 + side * sideK;
                double mount = T3 + Math.min(12, (zC - zw) * 0.1) + 12 * vn(xw / 45.0, zw / 45.0, 24) + 5 * vn(xw / 12.0, zw / 12.0, 25)
                        + 16 * rid(xw / 30.0, zw / 30.0, 35) * sm((zC - zw) / 20.0) + side * 0.4;
                double h2 = face(zC, zw, lowerT2, mount, 1.0 + 7.0 * sm(nearFall - 0.4), xw);
                h = face(zB, zw, lowerS, h2 > lowerT2 ? Math.max(lowerT2, h2) : lowerT2, w, xw);
                if (zw < zB) { double hh = face(zC, zw, lowerT2, mount, 1.0 + 7.0 * sm(nearFall - 0.4), xw); h = zw >= zC ? face(zB, zw, lowerS, lowerT2, w, xw) : hh; }
            }
            if (h > 138) h = 138 + (h - 138) * 0.2;
            if (zw >= zC && zw < zA) {                                                     // gorge : parois en pente, bosselees, au lieu d'un mur
                double cw = cxW(zw), ce = cxE(zw);
                if (xw > cw && xw < ce) {
                    double d = Math.min(xw - cw, ce - xw) + 3.5 * vn(xw / 8.0, zw / 8.0, 49) + 2 * vn(xw / 3.0, zw / 3.0, 50);
                    h = d <= 0 ? h : h * sm(1 - d / 15.0);
                    valley = true;
                }
            }
            int water = -999;
            if (valley) {
                double d = dist(xw, zw, RIV) + 2.5 * vn(xw / 15.0, zw / 15.0, 38);
                if (d < 14) {
                    double bed = -9 + Math.max(0, d - 3.5) * 1.3;
                    if (bed < h) h = bed;
                }
                double dp = Math.hypot(xw - POOL_X, zw - POOL_Z) * (1 + 0.13 * vn(xw / 9.0, zw / 9.0, 39));
                if (dp < POOL_R) {
                    double bed = -15 + Math.max(0, dp - 9) * 1.1;
                    if (bed < h) h = bed;
                }
                if (h < -2.5) water = -2;
            }
            th[i] = (int) Math.round(h);
            wl[i] = (short) water;
            int e = Math.min(Math.min(x, z), Math.min(W - 1 - x, D - 1 - z));
            edge[i] = (byte) Math.round(255 * sm(e / (double) BAND));
        }
        // etangs au pied des cascades de la terrasse
        pond(150, (int) Math.round(zA(150)) + 9, 9);
        pond(46, (int) Math.round(zA(46)) + 9, 9);
        // escalier de la vallee a la grande terrasse (rampe pleine, marches posees dessus), a cote du Conseil
        for (int z = 161; z <= 194; z++) {
            int h = (int) Math.round((194 - z) * (double) S / 33.0);
            for (int x = 164; x <= 170; x++) { th[z * W + x] = Math.max(th[z * W + x], Math.min(S, h)); ramp[z * W + x] = (byte) (1 + N); }
        }
        // escalier de la terrasse au gradin de la salle du Feu : tranchee creusee dans la roche, de plus en plus haute vers le nord
        for (int z = 101; z >= 78; z--) {
            int h = S + (102 - z);
            if (h > T2 - 1) break;
            for (int x = 128; x <= 130; x++) { th[z * W + x] = h; ramp[z * W + x] = (byte) (1 + N); }
        }
    }

    private void pond(int cx, int cz, int r) {
        for (int dz = -r - 2; dz <= r + 2; dz++) for (int dx = -r - 2; dx <= r + 2; dx++) {
            int x = cx + dx, z = cz + dz;
            if (!area(x, z)) continue;
            double d = Math.hypot(dx, dz) * (1 + 0.15 * vn(x / 4.0, z / 4.0, 80));
            int i = z * W + x;
            if (d < r) { th[i] = Math.min(th[i], -6 + (int) Math.round(Math.max(0, d - 4) * 0.6)); wl[i] = (short) -2; }
            else if (d < r + 2.5) th[i] = Math.min(th[i], 1 - (int) Math.round((r + 2.5 - d) * 0.4));
        }
    }

    // ------------------------------------------------------------------ cascades : lits creuses, nappes d'eau tombante devant chaque marche de roche
    private void carve() {
        channel(262, 3.0, 9, 2, (int) Math.round(zC(262)) + 2, 3, 91);
        channel(150, 2.0, 2, 28, (int) Math.round(zA(150)) + 2, 2, 92);
        channel(46, 2.0, 2, 24, (int) Math.round(zA(46)) + 2, 2, 93);
    }

    private void channel(int cx, double wob, int hw, int z0, int z1, int thick, int seed) {
        int[] lv = new int[z1 + 6], cc = new int[z1 + 6];
        for (int z = z0; z < z1 + 6 && z < D; z++) {
            cc[z] = cx + (int) Math.round(wob * vn(z / 16.0, seed * 0.37, seed));
            int i = z * W + cc[z];
            lv[z] = wl[i] > -999 ? wl[i] : th[i];
        }
        for (int z = z0; z <= z1 && z < D; z++) {
            for (int dx = -hw; dx <= hw; dx++) {
                int i = z * W + cc[z] + dx;
                if (wl[i] > -999) continue;
                th[i] = lv[z] - 1;
                wl[i] = (short) lv[z];
            }
        }
        for (int z = z0; z < z1 && z < D; z++) {
            int d = lv[z] - lv[z + 1];
            if (d < 2) continue;
            for (int dx = -hw; dx <= hw; dx++) for (int k = 1; k <= thick; k++) {
                for (int y = lv[z + 1] + 1; y <= lv[z]; y++) curtains.add(new int[]{cc[z] + dx, y, z + k});
            }
        }
    }

    // ------------------------------------------------------------------ matiere du terrain
    private static final int[] STRATA = {STONE, ANDESITE, TUFF, STONE, ANDESITE, STONE, CALCITE, COBBLE};

    private int rock(int x, int y, int z, boolean surface) {
        double layer = Math.floor((y + 5 * vn(x / 23.0, z / 23.0, 11) + 2 * vn(x / 7.0, z / 7.0, 12)) / 4.0);
        int k = (int) (rnd((int) layer, 17, 3) * 8) & 7;
        double q = rnd(x, y, z);
        if (surface && y < 40 && q < 0.2) return MOSSC;
        if (surface && vn(x / 3.0 + y / 5.0, z / 3.0, 57) > 0.74) return COBBLE;
        if (y > 4 && vn(x / 2.2, z / 2.2, 58) > 0.62 && vn(x / 11.0, z / 11.0, 59) > -0.1) return q < 0.5 ? MOSSC : MOSS;   // coulees de mousse le long des parois
        if (q < 0.18) return STRATA[(k + 3) & 7];
        return STRATA[k];
    }

    private void materialize() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x, t = th[i];
            int s = 0;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int xx = x + o[0], zz = z + o[1];
                if (area(xx, zz)) s = Math.max(s, Math.abs(th[zz * W + xx] - t));
            }
            boolean gentle = s <= 2 && t < 130;
            boolean wet = wl[i] > -999;
            for (int y = -OFF + 2; y <= t; y++) {
                int id;
                int depth = t - y;
                if (depth == 0) {
                    double q = rnd(x, 3, z);
                    if (wet && t < wl[i]) id = q < 0.4 ? GRAVEL : q < 0.7 ? SAND : COBBLE;
                    else if (ramp[i] != 0) id = STB;
                    else if (gentle) id = q < 0.05 ? PODZOL : q < 0.09 ? COARSE : q < 0.10 ? MOSS : GRASS;
                    else id = rock(x, y, z, true);
                } else if (depth <= 3 && gentle) id = rnd(x, y, z) < 0.2 ? COARSE : DIRT;
                else id = rock(x, y, z, false);
                set(x, y, z, id);
            }
            if (wet) for (int y = t + 1; y <= wl[i]; y++) set(x, y, z, WATER);
        }
    }

    private boolean rockId(int id) { return id >= STONE && id <= MOSSC; }

    private boolean nearWater(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            int id = gi(x + dx, y + dy, z + dz);
            if (id == WATER || id == WATERF) return true;
        }
        return false;
    }

    /** erosion des parois : les couches tendres reculent (surplombs, replats), les dures font saillie (blocs, eperons) ; plus de falaise lisse */
    private void erode() {
        boolean[] avoid = new boolean[W * D];
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) if (ramp[z * W + x] != 0)
            for (int dz = -5; dz <= 5; dz++) for (int dx = -5; dx <= 5; dx++) if (area(x + dx, z + dz)) avoid[(z + dz) * W + x + dx] = true;
        int[] buf = new int[1 << 18];
        for (int pass = 0; pass < 4; pass++) {
            boolean carve = pass < 2;
            int ps = pass & 1, n = 0;
            for (int y = -OFF + 4; y < 142; y++) for (int z = 8; z < D - 8; z++) for (int x = 8; x < W - 8; x++) {
                if (avoid[z * W + x]) continue;
                int id = gi(x, y, z);
                if (carve ? !rockId(id) : id != AIR) continue;
                int c = 0;
                int[][] dd = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] o : dd) {
                    int nid = gi(x + o[0], y, z + o[1]);
                    if (carve ? nid == AIR : (rockId(nid) && rockId(gi(x + o[0], y + 1, z + o[1])))) c++;
                }
                if (c == 0 || (carve && c > 2)) continue;
                double lay = Math.floor((y + 4 * vn(x / 19.0, z / 19.0, 61)) / 3.0);
                boolean soft = rnd((int) lay, 29, 5) < 0.4;
                boolean hit;
                if (carve) {
                    double nz = vn(x / 4.5 + y * 0.13, z / 4.5 - y * 0.11, 62 + ps);
                    hit = nz > (soft ? (ps == 0 ? 0.0 : 0.2) : (ps == 0 ? 0.5 : 0.62));
                } else {
                    double nz = vn(x / 3.5 - y * 0.09, z / 3.5 + y * 0.1, 64 + ps);
                    hit = nz > (soft ? 0.72 : 0.4);
                }
                if (!hit || nearWater(x, y, z)) continue;
                if (n == buf.length) buf = java.util.Arrays.copyOf(buf, n * 2);
                buf[n++] = idx(x, y, z);
            }
            for (int k = 0; k < n; k++) {
                int i = buf[k], x = i % W, z = (i / W) % D, y = i / (W * D) - OFF;
                g[i] = (byte) (carve ? AIRC : rock(x, y, z, true));
            }
        }
    }

    private void cascades() {
        for (int[] c : curtains) setIfAir(c[0], c[1], c[2], WATERF);
    }

    private void stairsOnRamps() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int r = ramp[z * W + x];
            if (r == 0) continue;
            set(x, th[z * W + x], z, stair(WBS, r - 1, false));
        }
        // balustrades de l'escalier de la vallee
        for (int z = 161; z <= 194; z++) for (int x : new int[]{163, 171}) {
            if (!area(x, z)) continue;
            int t = th[z * W + 167];
            found(x, z, x, z, t, WB);
            set(x, t + 1, z, WB);
            set(x, t + 2, z, WWALL);
            if (z % 8 == 0) set(x, t + 3, z, LANTERN);
        }
        // l'escalier de la salle du Feu : lanternes
        for (int z = 78; z <= 101; z++) for (int x : new int[]{127, 131}) {
            if (th[z * W + 129] >= T2 - 1) continue;
            if (z % 6 == 0) { set(x, th[z * W + 129] + 1, z, WWALL); set(x, th[z * W + 129] + 2, z, LANTERN); }
        }
    }

    // ------------------------------------------------------------------ toits
    private static int pick(int x, int z, int i) { return ((hash(x >> 1, i, z >> 1) >>> 3) % 10); }
    private int tileB(int x, int z, int i) { int m = pick(x, z, i); return m < 6 ? TILE_O : m < 9 ? TILE_G : TILE_Y; }
    private int tileS(int x, int z, int i, int dir) { int m = pick(x, z, i); return stair(m < 6 ? TOS : m < 9 ? TGS : TYS, dir, false); }

    /** hauteur d'un rang de toit : creux, plat pres de l'egout (debord releve) et de plus en plus raide vers le faitage, comme les toits elfiques */
    private static int stepOf(int i, int rise) { return Math.max(1, Math.min(3, (int) Math.round(rise * (0.55 + 0.18 * i)))); }

    /** toit a deux pans : faitage le long de x (axis 0) ou de z (axis 1) ; murs exterieurs de pmin a pmax (perpendiculaire) et de l0 a l1 ; yb : premiere rangee du toit */
    private void gable(int axis, int pmin, int pmax, int l0, int l1, int yb, int rise) {
        int a0 = pmin - 1, b0 = pmax + 1, rows = 0;
        while (a0 + rows <= b0 - rows) rows++;
        int[] hy = new int[rows + 1];
        hy[0] = yb;
        for (int i = 0; i < rows; i++) hy[i + 1] = hy[i] + stepOf(i, rise);
        for (int i = 0; i < rows; i++) {
            int a = a0 + i, b = b0 - i, h = hy[i], st = hy[i + 1] - hy[i];
            for (int l = l0 - 1; l <= l1 + 1; l++) {
                int xl = axis == 0 ? l : a, zl = axis == 0 ? a : l;
                int xb = axis == 0 ? l : b, zb = axis == 0 ? b : l;
                if (a == b) { for (int k = 0; k < Math.max(2, st); k++) put2(axis, a, l, h + k, tileB(xl, zl, i)); }
                else {
                    put2(axis, a, l, h, tileS(xl, zl, i, axis == 0 ? S_ : E));
                    put2(axis, b, l, h, tileS(xb, zb, i, axis == 0 ? N : W_));
                    for (int k = 1; k < st; k++) { put2(axis, a, l, h + k, tileB(xl, zl, i)); put2(axis, b, l, h + k, tileB(xb, zb, i)); }
                }
            }
            for (int e : new int[]{l0, l1}) for (int p : new int[]{a, b}) {
                if (p < pmin || p > pmax) continue;
                for (int y = yb; y < h; y++) put2(axis, p, e, y, (y == h - 1 || p == pmin || p == pmax) ? TIMBER : PLASTER);
            }
        }
        // fenetre ronde cerclee d'or au milieu de chaque pignon (ou une ogive etroite si le pignon est bas)
        int pc = (pmin + pmax) / 2, ic = Math.min(pc - a0, rows - 1), hc = hy[ic];
        for (int e : new int[]{l0, l1}) {
            double yc = yb + (hc - yb) * 0.5, rr = Math.min(2.4, (hc - yb) * 0.24);
            for (int p = pmin + 1; p <= pmax - 1; p++) for (int y = yb + 1; y < hy[Math.min(p - a0, b0 - p)] - 1; y++) {
                double d = Math.hypot(p - pc, y - yc);
                if (rr >= 1.3 && d <= rr) put2(axis, p, e, y, GLASS);
                else if (rr >= 1.3 && d <= rr + 1.1) put2(axis, p, e, y, GP);
            }
            if (rr < 1.3) for (int y = yb + 1; y <= Math.min(hc - 2, yb + 5); y++) put2(axis, pc, e, y, GLASS);
            int fx = axis == 0 ? e : pc, fz = axis == 0 ? pc : e;
            finial(fx, fz, hy[rows] + 1, 6);
        }
    }

    /** toit en pyramide sur un rectangle (avec 1 bloc de debord), profil creux ; s'arrete quand le demi-cote tombe sous stop (le dessus est alors plein) */
    private void hip(int x0, int z0, int x1, int z1, int yb, int rise, int stop) {
        int hy = yb;
        for (int i = 0; ; i++) {
            int xa = x0 - 1 + i, xb = x1 + 1 - i, za = z0 - 1 + i, zb = z1 + 1 - i, st = stepOf(i, rise);
            if (xa > xb || za > zb) break;
            if (Math.min(xb - xa, zb - za) < 2 * stop) { for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) for (int k = 0; k < Math.max(1, st); k++) set(x, hy + k, z, tileB(x, z, i)); break; }
            for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) {
                boolean ring = x == xa || x == xb || z == za || z == zb;
                if (!ring) continue;
                if (xa == xb || za == zb) { for (int k = 0; k < Math.max(2, st); k++) set(x, hy + k, z, tileB(x, z, i)); continue; }
                int dir = z == za ? S_ : z == zb ? N : x == xa ? E : W_;
                set(x, hy, z, tileS(x, z, i, dir));
                for (int k = 1; k < st; k++) set(x, hy + k, z, tileB(x, z, i));
            }
            hy += st;
        }
    }

    private void finial(int cx, int cz, int y, int len) {
        for (int k = 0; k < len; k++) set(cx, y + k, cz, k < len - 4 ? GFENCE : k == len - 4 ? GOLDB : ENDROD);
    }

    /** toit conique (tours rondes) */
    private void cone(int cx, int cz, int r, int y, int height, boolean green) {
        for (int i = 0; i < height; i++) {
            double ri = (r + 1.8) * Math.pow(1 - i / (double) height, 1.6);
            int R = (int) Math.ceil(ri) + 1;
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                double d = Math.hypot(dx, dz);
                if (d <= ri && d > ri - 2.0) set(cx + dx, y + i, cz + dz, green ? (pick(cx + dx, cz + dz, i) < 8 ? TILE_G : TILE_Y) : tileB(cx + dx, cz + dz, i));
            }
        }
        finial(cx, cz, y + height, 9);
    }

    // ------------------------------------------------------------------ murs : pilastres, bandeaux dores, fenetres en ogive
    private final List<int[]> decos = new ArrayList<>();       // {x, y, z, id} : reliefs de facade, poses apres coup (sauf devant les portes)
    private final List<int[]> doors = new ArrayList<>();        // {x, z, y}

    private void deco(boolean ax, int a, int f, int y, int id) { if (ax) decos.add(new int[]{a, y, f, id}); else decos.add(new int[]{f, y, a, id}); }

    private int plinthMix(int a, int f, int y) {
        double q = rnd(a * 7 + f, y, f * 3 + a);
        return q < 0.3 ? WB : q < 0.55 ? STB : q < 0.75 ? MOSSB : q < 0.9 ? TSTONE : COBBLE;
    }

    /** murs : enduit a colombages, volets de bois, pilastres en relief, appuis de fenetre fleuris, corniches, lierre, soubassement de pierre mousseuse ; out = sens vers l'exterieur */
    private void wall(boolean ax, int a0, int a1, int f, int y0, int floors, int fh, int bay, int mat, int plinth, int out) {
        int c = bay / 2;
        for (int a = a0; a <= a1; a++) {
            int p = (a - a0) % bay;
            boolean pil = p == 0 || a == a1;
            boolean full = a0 + ((a - a0) / bay) * bay + bay <= a1;
            for (int s = 0; s < floors; s++) for (int dy = 0; dy < fh; dy++) {
                int y = y0 + s * fh + dy, id = mat;
                if (pil) id = dy == 0 ? QB : WPIL;
                else if (dy == 0) id = GP;
                else if (s == floors - 1 && dy == fh - 1) id = GP;
                else {
                    int hw = dy <= fh - 5 ? 1 : 0;
                    boolean win = full && dy >= 2 && dy <= fh - 3 && Math.abs(p - c) <= hw;
                    if (win) id = GLASS;
                    else if (full && dy == fh - 2 && Math.abs(p - c) <= 1) id = GP;
                    else if (full && s > 0 && dy == 1 && Math.abs(p - c) <= 1) id = FILIG;
                    else if (s == 0 && dy < plinth) id = plinthMix(a, f, y);
                    else if (full && dy >= 2 && dy <= fh - 3 && Math.abs(p - c) == 2) id = TIMBER;                  // volets
                    else if (full && s == 0 && dy == 1 && Math.abs(p - c) <= 1) id = TIMBER;                       // panneau a croix sous la fenetre
                    else if (dy == fh - 2 && !pil) id = TIMBER;                                                   // poutre sous le plafond
                    else if (rnd(a, y, f) < 0.05) id = WS;
                }
                put(ax, a, f, y, id);
                int fo = f + out;
                if (pil) {
                    if (dy == 0) deco(ax, a, fo, y, QB);
                    else if (dy == fh - 1) deco(ax, a, fo, y, QB);
                    else deco(ax, a, fo, y, WPIL);
                } else if (full) {
                    if (dy == fh - 1 && Math.abs(p - c) <= 2) deco(ax, a, fo, y, slab(GPS, true));                // corniche / bandeau saillant
                    if (dy == 1 && Math.abs(p - c) <= 1) deco(ax, a, fo, y, slab(GPS, false));                    // appui de fenetre
                    if (dy == 2 && Math.abs(p - c) <= 1 && rnd(a, y, f) < 0.7) deco(ax, a, fo, y, rnd(a, f, y) < 0.5 ? LEAF_R : LEAF_Y);   // jardiniere
                }
                if (s == 0 && !pil && dy < fh - 1 && rnd(a * 5, f, a) < 0.07 && dy < 2 + 4 * rnd(a, f, 9)) deco(ax, a, fo, y, LEAF_G);   // lierre
            }
        }
    }

    private void doorway(int side, int x0, int z0, int x1, int z1, int yb) {
        boolean ax = side == 0 || side == 2;
        int f = side == 0 ? z0 : side == 2 ? z1 : side == 1 ? x1 : x0;
        int c = ax ? (x0 + x1) / 2 : (z0 + z1) / 2;
        doorAt(ax, c, f, yb);
    }

    private void doorAt(boolean ax, int c, int f, int yb) {
        doors.add(ax ? new int[]{c, f, yb} : new int[]{f, c, yb});
        for (int d = -1; d <= 1; d++) for (int y = yb; y <= yb + 3; y++) put(ax, c + d, f, y, AIR);
        put(ax, c, f, yb + 4, AIR); put(ax, c, f, yb + 5, AIR);
        for (int y = yb; y <= yb + 4; y++) { put(ax, c - 2, f, y, WPIL); put(ax, c + 2, f, y, WPIL); }
        put(ax, c - 1, f, yb + 4, GP); put(ax, c + 1, f, yb + 4, GP); put(ax, c - 1, f, yb + 5, WB); put(ax, c + 1, f, yb + 5, WB); put(ax, c, f, yb + 6, GP);
    }

    /** maison a etages : murs d'enduit, pilastres blancs, fenetres en ogive, toit de tuiles ; echelle dans le coin nord-ouest */
    private void house(int x0, int z0, int x1, int z1, int yb, int floors, int fh, int bay, int axis, int rise, int door, boolean spawn) {
        int hgt = floors * fh, wd = axis == 0 ? z1 - z0 : x1 - x0;
        box(x0, yb, z0, x1, yb + hgt + rise * (wd / 2 + 3) + 6, z1, AIR);
        box(x0, yb - 1, z0, x1, yb - 1, z1, GP);
        found(x0, z0, x1, z1, yb - 2, WB);
        wall(true, x0, x1, z0, yb, floors, fh, bay, PLASTER, 2, -1);
        wall(true, x0, x1, z1, yb, floors, fh, bay, PLASTER, 2, 1);
        wall(false, z0, z1, x0, yb, floors, fh, bay, PLASTER, 2, -1);
        wall(false, z0, z1, x1, yb, floors, fh, bay, PLASTER, 2, 1);
        for (int s = 1; s < floors; s++) box(x0 + 1, yb + s * fh, z0 + 1, x1 - 1, yb + s * fh, z1 - 1, GP);
        for (int y = yb; y < yb + hgt; y++) set(x0 + 1, y, z0 + 1, LADE);
        int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        for (int s = 0; s < floors; s++) {
            set(cx, yb + s * fh + (s == 0 ? 0 : 1), cz, GFENCE);
            set(cx, yb + s * fh + (s == 0 ? 1 : 2), cz, LANTERN);
        }
        if (door >= 0) doorway(door, x0, z0, x1, z1, yb);
        gable(axis, axis == 0 ? z0 : x0, axis == 0 ? z1 : x1, axis == 0 ? x0 : z0, axis == 0 ? x1 : z1, yb + hgt, rise);
        reserve(x0, z0, x1, z1, 4);
        if (spawn) spawns.add(new int[]{cx, yb, cz});
    }

    // ------------------------------------------------------------------ la maison d'Elrond
    private void mainHall() {
        int b = S + 1;
        // longue salle aux toits de tuiles, trois portes sur la promenade
        house(74, 128, 140, 148, b, 2, 7, 6, 0, 1, 2, true);
        doorAt(true, 116, 148, b);
        doorAt(true, 98, 148, b);
        // table du banquet et bancs
        for (int x : new int[]{79, 121}) { box(x, b, 140, x + 14, b, 140, GP); for (int k = 0; k <= 14; k += 2) { set(x + k, b, 138, stair(GPS, S_, false)); set(x + k, b, 142, stair(GPS, N, false)); } }
        // trois hauts pignons : ouest 3 etages, centre 5 (tour), est 4
        house(76, 114, 90, 134, b, 3, 7, 7, 1, 2, -1, true);
        house(98, 112, 116, 134, b, 5, 7, 6, 1, 2, -1, true);
        house(124, 110, 138, 134, b, 4, 7, 7, 1, 2, -1, true);
        spawns.add(new int[]{107, b, 140});
        spawns.add(new int[]{85, b, 140});
    }

    // ------------------------------------------------------------------ tour grise a toit vert, beffroi
    private void bellTower() {
        int cx = 68, cz = 142, r = 7, yb = S + 1, h = 58;
        reserve(cx - r, cz - r, cx + r, cz + r, 4);
        found(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, yb - 1, TSTONE);
        for (int y = yb; y < yb + h; y++) {
            int dy = y - yb;
            for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
                double d = Math.hypot(dx, dz);
                boolean wallCell = d <= r + 0.4 && d > r - 0.8;
                if (dy < 4 && d <= r + 1.5 && d > r - 0.8) wallCell = true;
                if (wallCell) {
                    int id = TSTONE;
                    boolean slit = dy % 11 == 6 && dy > 5 && ((dx == 0 && Math.abs(dz) >= r - 1) || (dz == 0 && Math.abs(dx) >= r - 1) || (Math.abs(dx) == Math.abs(dz) && d > r - 1));
                    if (dy % 11 == 10) id = QB;
                    if (slit) id = GLASS;
                    boolean belfry = dy >= h - 12 && dy <= h - 4;
                    if (belfry) {
                        boolean opening = (Math.abs(dx) <= 1 && Math.abs(dz) >= r - 1) || (Math.abs(dz) <= 1 && Math.abs(dx) >= r - 1);
                        id = opening ? (dy == h - 4 ? QB : AIR) : (dy == h - 12 ? QB : WPIL);
                    }
                    set(cx + dx, y, cz + dz, id);
                } else if (d <= r - 0.8) set(cx + dx, y, cz + dz, AIR);
            }
            if (dy % 10 == 9 && dy < h - 12) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) if (Math.hypot(dx, dz) <= r - 0.8) set(cx + dx, y, cz + dz, GP);
        }
        for (int y = yb; y < yb + h - 4; y++) set(cx, y, cz - r + 1, LADS);
        set(cx, yb + h - 6, cz, CHAIN); set(cx, yb + h - 7, cz, GOLDB);                    // la cloche
        for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
            double d = Math.hypot(dx, dz);
            if (d <= r + 1.6) set(cx + dx, yb + h, cz + dz, d > r + 0.4 ? QB : GP);
        }
        for (int a = 0; a < 20; a++) { double an = a * Math.PI / 10; set(cx + (int) Math.round(Math.cos(an) * (r + 1.2)), yb + h + 1, cz + (int) Math.round(Math.sin(an) * (r + 1.2)), TWALL); }
        cone(cx, cz, r + 1, yb + h + 1, 28, true);
        for (int d = -1; d <= 1; d++) for (int y = yb; y < yb + 4; y++) { set(cx + d, y, cz + r, AIR); set(cx + d, y, cz + r - 1, AIR); }
        set(cx, yb + 4, cz + r, AIR);
        for (int y = yb; y < yb + 5; y++) { set(cx - 2, y, cz + r, WPIL); set(cx + 2, y, cz + r, WPIL); }
        spawns.add(new int[]{cx, yb, cz + 2});
    }

    // ------------------------------------------------------------------ l'aiguille doree
    private void needle() {
        int cx = 84, cz = 106, y = S + 1;
        reserve(cx - 6, cz - 6, cx + 6, cz + 6, 3);
        found(cx - 5, cz - 5, cx + 5, cz + 5, y - 1, WB);
        int[] half = {4, 3, 2, 1}, len = {22, 18, 18, 16};
        for (int s = 0; s < 4; s++) {
            int hw = half[s];
            for (int k = 0; k < len[s]; k++) {
                for (int dx = -hw; dx <= hw; dx++) for (int dz = -hw; dz <= hw; dz++) {
                    boolean edge = Math.abs(dx) == hw || Math.abs(dz) == hw, corner = Math.abs(dx) == hw && Math.abs(dz) == hw;
                    int id = AIR;
                    if (edge) {
                        id = corner ? WPIL : PLASTER;
                        if (!corner && k % 7 == 4 && (dx == 0 || dz == 0)) id = GLASS;
                        else if (!corner && k % 7 == 0) id = GP;
                        else if (!corner && hw >= 2 && k % 7 == 5 && Math.abs(dx + dz) % 2 == 0) id = WB;
                    }
                    set(cx + dx, y + k, cz + dz, id);
                }
            }
            y += len[s];
            for (int dx = -hw - 1; dx <= hw + 1; dx++) for (int dz = -hw - 1; dz <= hw + 1; dz++) {
                boolean ring = Math.abs(dx) == hw + 1 || Math.abs(dz) == hw + 1;
                set(cx + dx, y, cz + dz, ring ? QB : GP);
            }
            y++;
            if (s < 3) for (int dx = -hw - 1; dx <= hw + 1; dx++) for (int dz = -hw - 1; dz <= hw + 1; dz++) {
                if (Math.abs(dx) == hw + 1 || Math.abs(dz) == hw + 1) {
                    int dir = Math.abs(dz) == hw + 1 ? (dz < 0 ? S_ : N) : (dx < 0 ? E : W_);
                    set(cx + dx, y, cz + dz, tileS(cx + dx, cz + dz, s, dir));
                }
            }
        }
        for (int k = 0; k < 4; k++) set(cx, y + k, cz, TILE_Y);
        finial(cx, cz, y + 4, 16);
        for (int k = 0; k < 2; k++) set(cx, S + 1 + k, cz + 4, AIR);
    }

    // ------------------------------------------------------------------ maison d'etude, a cote du ruisseau
    private void studyHouse() {
        house(158, 104, 174, 124, S + 1, 4, 7, 6, 0, 2, 2, true);
        // balcon sur le ruisseau
        for (int x = 148; x <= 157; x++) for (int z = 112; z <= 116; z++) set(x, S + 15, z, GP);
        for (int x = 148; x <= 157; x++) { set(x, S + 16, 112, GFENCE); set(x, S + 16, 116, GFENCE); }
        for (int z = 112; z <= 116; z++) set(148, S + 16, z, GFENCE);
        for (int z : new int[]{112, 116}) for (int y = S + 1; y <= S + 14; y++) set(148, y, z, WPIL);
        for (int dz = -1; dz <= 1; dz++) for (int y = S + 16; y <= S + 18; y++) set(158, y, 114 + dz, AIR);
    }

    // ------------------------------------------------------------------ pagode a quatre etages
    private void pagoda() {
        int cx = 214, cz = 116, y = S + 1;
        int[] hw = {8, 6, 4, 3};
        reserve(cx - 11, cz - 11, cx + 11, cz + 11, 3);
        found(cx - 9, cz - 9, cx + 9, cz + 9, y - 1, WB);
        box(cx - 10, S, cz - 10, cx + 10, S, cz + 10, WS);
        for (int t = 0; t < 4; t++) {
            int w = hw[t], x0 = cx - w, x1 = cx + w, z0 = cz - w, z1 = cz + w;
            box(x0, y - 1, z0, x1, y - 1, z1, GP);
            for (int k = 0; k < 7; k++) box(x0 + 1, y + k, z0 + 1, x1 - 1, y + k, z1 - 1, AIR);
            wall(true, x0, x1, z0, y, 1, 7, 2 * w, PLASTER, t == 0 ? 2 : 0, -1);
            wall(true, x0, x1, z1, y, 1, 7, 2 * w, PLASTER, t == 0 ? 2 : 0, 1);
            wall(false, z0, z1, x0, y, 1, 7, 2 * w, PLASTER, t == 0 ? 2 : 0, -1);
            wall(false, z0, z1, x1, y, 1, 7, 2 * w, PLASTER, t == 0 ? 2 : 0, 1);
            for (int k = 0; k < 7; k++) set(x0 + 1, y + k, z0 + 1, LADE);
            set(cx, y, cz, GFENCE); set(cx, y + 1, cz, LANTERN);
            if (t == 0) doorway(2, x0, z0, x1, z1, y);
            y += 7;
            if (t < 3) {
                int stopHW = hw[t + 1] + 1;
                hip(x0, z0, x1, z1, y, 1, stopHW);
                y += w + 1 - stopHW + 1;
                box(cx - hw[t + 1], y - 1, cz - hw[t + 1], cx + hw[t + 1], y - 1, cz + hw[t + 1], GP);
            } else {
                hip(x0, z0, x1, z1, y, 2, 0);
                y += 2 * (w + 1) + 1;
                finial(cx, cz, y, 18);
            }
        }
        spawns.add(new int[]{cx, S + 1, cz + 3});
    }

    // ------------------------------------------------------------------ gradin : salle du Feu
    private void fireHall() {
        house(92, 80, 120, 96, T2 + 1, 1, 10, 6, 0, 2, 2, true);
        reserve(128, 78, 130, 101, 3);
        for (int x = 92; x <= 130; x++) for (int z = 97; z <= 99; z++) if (tAt(x, z) == T2) set(x, T2, z, WS);
    }

    // ------------------------------------------------------------------ arcade a deux niveaux au pied de la falaise
    private void arcade() {
        for (int x0 = 74; x0 < 152; x0 += 6) {
            int zc = (int) Math.round(zA(x0 + 3)) + 3;
            for (int k = 0; k <= 6; k++) {
                int x = x0 + k, zf = (int) Math.round(zA(x + 0.5));
                for (int z = zf; z <= zc; z++) { if (tAt(x, z) < S - 1) found(x, z, x, z, 15, STB); }
            }
            int base = th[zc * W + x0];
            for (int y = base + 1; y <= S; y++) set(x0, y, zc, (y - base) % 16 == 1 ? QB : WPIL);
            // arcs en ogive, deux niveaux
            for (int tier = 0; tier < 2; tier++) {
                int yt = 14 + 16 * tier;
                for (int dx = 1; dx <= 5; dx++) {
                    int x = x0 + dx;
                    for (int y = yt - 1; y <= yt + 1; y++) {
                        int level = y - (yt - 1);
                        boolean hole = dx == 3 ? level <= 1 : (dx == 2 || dx == 4) ? level <= 0 : false;
                        set(x, y, zc, hole ? AIR : WB);
                    }
                    set(x, yt + 2, zc, GP);
                    set(x, yt + 3, zc, QB);
                }
                if (tier == 1) continue;
                for (int z = (int) Math.round(zA(x0 + 3)); z <= zc; z++) for (int dx = 0; dx <= 6; dx++) if (get(x0 + dx, 16, z) == AIR) set(x0 + dx, 16, z, WS);
            }
        }
        for (int x = 74; x <= 152; x++) { int zc = (int) Math.round(zA(x + 0.5)) + 3; reserve(x, zc, x, zc, 2); }
    }

    // ------------------------------------------------------------------ terrasse du Conseil
    private void council() {
        int cx = 195, cz = 150, r = 22;
        reserve(cx - r, cz - r, cx + r, cz + r, 3);
        for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
            double d = Math.hypot(dx, dz);
            int x = cx + dx, z = cz + dz;
            if (!area(x, z)) continue;
            if (d <= r + 0.5) {
                found(x, z, x, z, S - 1, d > r - 2 ? WB : STB);
                double an = Math.atan2(dz, dx);
                boolean star = d < 13 && Math.abs(Math.cos(an * 4 + 0.4)) * 10 > d - 3 && d > 2;
                boolean ring = Math.abs(d - 8) < 0.6 || Math.abs(d - 15) < 0.6;
                int id = star ? GP : ring ? PAVE2 : (((x + z) & 3) == 0 ? PAVE2 : WS);
                if (d < 2.5) id = GOLDB;
                set(x, S, z, id);
                for (int y = S + 1; y <= S + 14; y++) if (get(x, y, z) != AIR) set(x, y, z, AIR);
            }
            if (d > r - 0.5 && d <= r + 0.6) { set(x, S + 1, z, QB); set(x, S + 2, z, WWALL); }
        }
        // chaises du Conseil, tournees vers la table
        for (int k = 0; k < 14; k++) {
            double an = k * Math.PI * 2 / 14 + 0.2;
            int x = cx + (int) Math.round(Math.cos(an) * 10.5), z = cz + (int) Math.round(Math.sin(an) * 10.5);
            int dir = Math.abs(Math.cos(an)) > Math.abs(Math.sin(an)) ? (Math.cos(an) > 0 ? W_ : E) : (Math.sin(an) > 0 ? N : S_);
            set(x, S + 1, z, stair(GPS, dir, false));
            if (k % 2 == 0) spawns.add(new int[]{x, S + 1, z});
        }
        set(cx, S + 1, cz, GP);
        bigTree(cx, S + 1, cz - 17);
        for (int k = 0; k < 8; k++) {
            double an = Math.PI * 0.15 + k * Math.PI * 1.7 / 8;
            int x = cx + (int) Math.round(Math.cos(an) * (r - 1.5)), z = cz + (int) Math.round(Math.sin(an) * (r - 1.5));
            for (int h = 1; h <= 4; h++) set(x, S + h, z, WWALL);
            set(x, S + 5, z, LANTERN);
        }
        // contreforts sous la terrasse, du cote de la vallee
        for (int k = -6; k <= 6; k++) {
            double an = Math.PI / 2 + k * 0.17;
            int x = cx + (int) Math.round(Math.cos(an) * (r + 1)), z = cz + (int) Math.round(Math.sin(an) * (r + 1));
            if (!area(x, z)) continue;
            for (int y = th[z * W + x] + 1; y < S; y++) set(x, y, z, WPIL);
        }
    }

    // ------------------------------------------------------------------ gloriettes
    private void gazebo(int cx, int cz, int y0, boolean base) {
        int r = 6;
        reserve(cx - r, cz - r, cx + r, cz + r, 4);
        for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) {
            double d = Math.hypot(dx, dz);
            int x = cx + dx, z = cz + dz;
            if (!area(x, z)) continue;
            if (d <= r + 0.8) {
                if (base) found(x, z, x, z, y0 - 1, WB);
                set(x, y0 - 1, z, d > r - 0.6 ? QB : (((x + z) & 1) == 0 ? WS : PAVE2));
                for (int y = y0; y <= y0 + 14; y++) if (get(x, y, z) != AIR) set(x, y, z, AIR);
            }
        }
        for (int k = 0; k < 8; k++) {
            double an = k * Math.PI / 4;
            int x = cx + (int) Math.round(Math.cos(an) * (r - 0.5)), z = cz + (int) Math.round(Math.sin(an) * (r - 0.5));
            for (int y = y0; y < y0 + 10; y++) set(x, y, z, y == y0 ? QB : WPIL);
            double an2 = (k + 0.5) * Math.PI / 4;
            int mx = cx + (int) Math.round(Math.cos(an2) * (r - 0.5)), mz = cz + (int) Math.round(Math.sin(an2) * (r - 0.5));
            for (int y = y0 + 5; y < y0 + 10; y++) set(mx, y, mz, y == y0 + 9 ? WB : (y == y0 + 5 ? FILIG : GFENCE));
            set(mx, y0, mz, WWALL);
        }
        for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) if (Math.hypot(dx, dz) <= r + 0.8 && Math.hypot(dx, dz) > r - 0.9) set(cx + dx, y0 + 10, cz + dz, QB);
        cone(cx, cz, r, y0 + 11, 11, false);
        set(cx, y0 + 10, cz, CHAIN); set(cx, y0 + 9, cz, HLANT);
        spawns.add(new int[]{cx, y0, cz + 1});
    }

    private void gazebos() {
        gazebo(322, 120, S + 1, false);
        gazebo(205, 218, 1, false);
        gazebo(60, 112, S + 1, false);
    }

    // ------------------------------------------------------------------ ponts
    private void bridgeX(int xa, int xb, int z, int y, int drop) {
        int hw = 3, xm = (xa + xb) / 2, half = (xb - xa) / 2;
        for (int x = xa; x <= xb; x++) {
            double t = (x - xm) / (double) half;
            int yb = y - 3 - (int) Math.round(drop * t * t);
            for (int dz = -hw; dz <= hw; dz++) {
                for (int yy = Math.max(yb, -OFF + 2); yy <= y; yy++) {
                    if (yy == y) set(x, yy, z + dz, ((x + dz) & 1) == 0 ? WS : PAVE2);
                    else if (get(x, yy, z + dz) == AIR || get(x, yy, z + dz) == WATER) set(x, yy, z + dz, Math.abs(dz) == hw || yy == yb ? STB : WB);
                }
            }
            for (int dz : new int[]{-hw - 1, hw + 1}) {
                set(x, y, z + dz, QB);
                set(x, y + 1, z + dz, WWALL);
                if ((x - xa) % 6 == 3) { set(x, y + 2, z + dz, WWALL); set(x, y + 3, z + dz, LANTERN); }
            }
        }
    }

    private void bridgeZ(int x, int za, int zb, int y, int drop) {
        int hw = 3, zm = (za + zb) / 2, half = (zb - za) / 2;
        for (int z = za; z <= zb; z++) {
            double t = (z - zm) / (double) half;
            int yb = y - 3 - (int) Math.round(drop * t * t);
            for (int dx = -hw; dx <= hw; dx++) {
                for (int yy = Math.max(yb, -OFF + 2); yy <= y; yy++) {
                    if (yy == y) set(x + dx, yy, z, ((x + z + dx) & 1) == 0 ? WS : PAVE2);
                    else if (get(x + dx, yy, z) == AIR || get(x + dx, yy, z) == WATER) set(x + dx, yy, z, Math.abs(dx) == hw || yy == yb ? STB : WB);
                }
            }
            for (int dx : new int[]{-hw - 1, hw + 1}) {
                set(x + dx, y, z, QB);
                set(x + dx, y + 1, z, WWALL);
                if ((z - za) % 6 == 3) { set(x + dx, y + 2, z, WWALL); set(x + dx, y + 3, z, LANTERN); }
            }
        }
    }

    private void gate(int cx, int cz, int y0) {                       // pavillon de pont : quatre piliers, toit en pyramide, lanterne
        reserve(cx - 5, cz - 5, cx + 5, cz + 5, 2);
        for (int dx : new int[]{-4, 4}) for (int dz : new int[]{-4, 4}) for (int y = y0; y < y0 + 12; y++) set(cx + dx, y, cz + dz, y == y0 ? QB : WPIL);
        for (int d = -3; d <= 3; d++) {
            for (int[] e : new int[][]{{d, -4}, {d, 4}, {-4, d}, {4, d}}) {
                set(cx + e[0], y0 + 11, cz + e[1], GP);
                if (Math.abs(d) >= 2) { set(cx + e[0], y0 + 10, cz + e[1], GFENCE); set(cx + e[0], y0 + 9, cz + e[1], GFENCE); }
            }
        }
        hip(cx - 4, cz - 4, cx + 4, cz + 4, y0 + 12, 2, 0);
        finial(cx, cz, y0 + 12 + 2 * 6 + 1, 10);
        set(cx, y0 + 11, cz, CHAIN); set(cx, y0 + 10, cz, HLANT);
    }

    private void bridges() {
        galleryBridge(132);
        // passerelle sur le ruisseau de la terrasse
        for (int x = 146; x <= 154; x++) for (int z = 136; z <= 138; z++) set(x, S + 1, z, (x == 146 || x == 154) ? stair(WBS, x == 146 ? E : W_, false) : WS);
        for (int x = 147; x <= 153; x++) { set(x, S + 2, 135, WWALL); set(x, S + 2, 139, WWALL); }
        for (int x = 147; x <= 153; x++) for (int z = 136; z <= 138; z++) set(x, S, z, WB);
    }

    private int pierMix(int x, int y, int z) {                                       // pont tout blanc : briques blanches, quelques pierres lisses
        double q = rnd(x * 3 + z, y, z * 7 + x);
        return q < 0.8 ? WB : q < 0.92 ? WS : QB;
    }

    private void turret(int cx, int cz, int yb, int h) {
        found(cx - 3, cz - 3, cx + 3, cz + 3, yb - 1, TSTONE);
        for (int k = 0; k < h; k++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            double d = Math.hypot(dx, dz);
            if (d <= 2.6 && d > 1.4) set(cx + dx, yb + k, cz + dz, (k % 8 == 4 && (dx == 0 || dz == 0)) ? GLASS : (k % 8 == 7 ? QB : TSTONE));
            else if (d <= 1.4) set(cx + dx, yb + k, cz + dz, AIR);
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) if (Math.hypot(dx, dz) <= 3.4) set(cx + dx, yb + h, cz + dz, Math.hypot(dx, dz) > 2.4 ? QB : GP);
        cone(cx, cz, 3, yb + h + 1, 13, true);
    }

    /**
     * Le grand pont-galerie sur la gorge, d'apres l'aquarelle : un tablier porte sur de gros piliers de pierre et de hautes arches, une galerie de deux files d'arcs en ogive
     * (une plaque ajouree au sommet de chacun, un petit toit pointu au-dessus), un grand toit a deux pans entre les files, quatre tourelles aux extremites.
     */
    private void galleryBridge(int zc) {
        int xw = (int) Math.round(cxW(zc)), xe = (int) Math.round(cxE(zc)), hw = 5;
        reserve(xw - 8, zc - 9, xe + 8, zc + 9, 2);
        int n = 5, spring = S - 13;
        int[] xp = new int[n + 1];
        for (int k = 0; k <= n; k++) xp[k] = xw + (int) Math.round((xe - xw) * k / (double) n);
        for (int x = xw - 4; x <= xe + 4; x++) {
            int pier = -1, bay = -1;
            for (int k = 1; k < n; k++) if (Math.abs(x - xp[k]) <= 1) pier = k;
            if (pier < 0) for (int k = 0; k < n; k++) if (x >= xp[k] && x <= xp[k + 1]) bay = k;
            for (int dz = -hw; dz <= hw; dz++) {
                int z = zc + dz;
                if (x < xw || x > xe) found(x, z, x, z, S - 1, WB);
                else if (pier >= 0) {
                    for (int y = Math.max(th[z * W + x] + 1, -OFF + 2); y <= S - 1; y++) if (get(x, y, z) == AIR || get(x, y, z) == WATER) set(x, y, z, pierMix(x, y, z));
                } else if (bay >= 0) {
                    double xc = (xp[bay] + xp[bay + 1]) / 2.0, half = (xp[bay + 1] - xp[bay]) / 2.0 - 1.6;
                    double t = Math.abs(x - xc) / half;
                    int top = t > 1 ? -OFF : spring + (int) Math.round(9 * (1 - Math.pow(t, 1.7)));
                    for (int y = Math.max(top, th[z * W + x]) + 1; y <= S - 1; y++) if (get(x, y, z) == AIR || get(x, y, z) == WATER) set(x, y, z, y == top + 1 ? QB : pierMix(x, y, z));
                }
                set(x, S, z, ((x + z) & 1) == 0 ? WS : PAVE2);
                if (Math.abs(dz) == hw) { set(x, S + 1, z, WWALL); if ((x - xw) % 12 == 6) { set(x, S + 2, z, WWALL); set(x, S + 3, z, LANTERN); } }
            }
        }
        int g0 = xw - 2, g1 = xe + 2, len = g1 - g0, m = Math.max(1, (int) Math.round(len / 6.0));
        int[] cols = new int[m + 1];
        for (int i = 0; i <= m; i++) cols[i] = g0 + (int) Math.round(len * i / (double) m);
        for (int side = -1; side <= 1; side += 2) {
            int zs = zc + side * 4;
            for (int i = 0; i <= m; i++) for (int y = S + 1; y <= S + 9; y++) set(cols[i], y, zs, (y == S + 1 || y == S + 9) ? QB : WPIL);
            for (int i = 0; i < m; i++) {
                int c0 = cols[i], c1 = cols[i + 1], w = c1 - c0;
                double c = w / 2.0;
                for (int dx = 1; dx < w; dx++) {
                    double ddx = Math.abs(dx - c);
                    for (int l = 0; l <= 8; l++) {
                        boolean open = l <= 5 || (l == 6 && ddx <= c - 1.4) || (l == 7 && ddx <= c - 2.4) || (l == 8 && ddx <= 0.6);
                        set(c0 + dx, S + 1 + l, zs, open ? AIR : WB);
                    }
                    set(c0 + dx, S + 10, zs, Math.abs(dx - c) <= 0.6 ? FILIG : GP);
                }
                hip(c0, zs - 1, c1, zs + 1, S + 11, 2, 0);
                finial(c0 + w / 2, zs, S + 16, 5);
            }
        }
        gable(0, zc - 3, zc + 3, g0, g1, S + 13, 1);
        for (int sx : new int[]{g0 - 2, g1 + 2}) for (int side = -1; side <= 1; side += 2) turret(sx, zc + side * 7, S + 1, 22);
        spawns.add(new int[]{(xw + xe) / 2 - 8, S + 1, zc});
        spawns.add(new int[]{(xw + xe) / 2 + 8, S + 1, zc});
    }

    // ------------------------------------------------------------------ la vallee : maison du gue, pont sur la riviere
    private void valley() {
        house(118, 214, 130, 226, 1, 3, 7, 6, 1, 2, 0, true);
        house(138, 196, 152, 210, 1, 4, 7, 7, 0, 2, 2, true);                                  // haute maison au pied de l'escalier
        house(310, 140, 324, 156, S + 1, 3, 7, 7, 1, 2, 3, true);                               // maison de l'epaule est, face a l'arche
        house(300, 100, 312, 112, S + 1, 4, 7, 6, 0, 2, 2, true);
        for (int x = 296; x <= 312; x++) for (int z = 136; z <= 138; z++) pave(x, z, S, WS);
        int xb = 190, ya = 5;
        bridgeZ(xb, 244, 268, ya, 3);
        for (int k = 0; k < 6; k++) for (int dx = -3; dx <= 3; dx++) {
            int yy = ya - 1 - k;
            if (yy < 1) continue;
            for (int[] zz : new int[][]{{243 - k, S_}, {269 + k, N}}) {
                int z = zz[0];
                set(xb + dx, yy, z, stair(WBS, zz[1] == S_ ? S_ : N, false));
                for (int y = th[z * W + xb + dx] + 1; y < yy; y++) set(xb + dx, y, z, WB);
            }
        }
        spawns.add(new int[]{xb, ya + 1, 256});
    }

    // ------------------------------------------------------------------ dallage
    private void pave(int x, int z, int level, int id) {
        if (!area(x, z) || th[z * W + x] != level || ramp[z * W + x] != 0) return;
        int t = gi(x, level, z);
        if (t != GRASS && t != PODZOL && t != COARSE && t != MOSS && t != DIRT) return;
        set(x, level, z, id);
    }

    private void band(double x0, double z0, double x1, double z1, int w, int level, boolean alt) {
        double len = Math.hypot(x1 - x0, z1 - z0);
        for (double t = 0; t <= len; t += 0.5) {
            double px = x0 + (x1 - x0) * t / len, pz = z0 + (z1 - z0) * t / len;
            for (int dx = -w; dx <= w; dx++) for (int dz = -w; dz <= w; dz++) {
                if (dx * dx + dz * dz > w * w + 1) continue;
                int x = (int) Math.round(px) + dx, z = (int) Math.round(pz) + dz;
                pave(x, z, level, alt && ((x / 3 + z / 3) & 1) == 0 ? PAVE2 : (rnd(x, 41, z) < 0.1 ? STB : WS));
            }
        }
    }

    private void promenade() {
        for (int x = 62; x <= 226; x++) {
            int zf = (int) Math.round(zA(x + 0.5));
            for (int z = 149; z < zf; z++) pave(x, z, S, (x / 3 + z / 3) % 2 == 0 ? PAVE2 : WS);
            if (x > 74 && x % 2 == 0 && !(x >= 164 && x <= 170) && tAt(x, zf - 1) == S) { set(x, S + 1, zf - 1, WWALL); if (x % 10 == 0) { set(x, S + 2, zf - 1, WWALL); set(x, S + 3, zf - 1, LANTERN); } }
        }
        band(176, 124, 176, 149, 1, S, false);
        band(142, 112, 142, 148, 1, S, false);
        band(150, 125, 227, 134, 1, S, true);
        band(92, 99, 130, 101, 1, S, false);
        band(168, 160, 168, 193, 2, 0, true);
        band(168, 194, 190, 240, 2, 0, true);
        band(150, 180, 200, 215, 1, 0, false);
        band(130, 226, 190, 242, 1, 0, false);
        band(190, 270, 215, 300, 1, 0, false);
        for (int x = 294; x <= 330; x++) for (int z = 128; z <= 136; z++) pave(x, z, S, WS);
    }

    // ------------------------------------------------------------------ jardins
    private void gardens() {
        for (int z = 20; z < D - 20; z++) for (int x = 20; x < W - 20; x++) {
            int t = th[z * W + x];
            if (res[z * W + x] || gi(x, t, z) != GRASS || get(x, t + 1, z) != AIR) continue;
            double q = rnd(x, 9, z);
            if (q < 0.10) set(x, t + 1, z, SHORTG);
            else if (q < 0.125) set(x, t + 1, z, FERN);
            else if (q < 0.14) set(x, t + 1, z, ((x * 7 + z) & 3) == 0 ? POPPY : ((x + z) & 1) == 0 ? BLUET : (q < 0.135 ? DANDEL : CORNFL));
        }
        for (int x = 80; x <= 220; x += 14) lamp(x, 151);
        for (int z = 170; z <= 240; z += 12) lamp(172, z);
        for (int x = 200; x <= 320; x += 14) lamp(x, 129);
    }

    private void lamp(int x, int z) {
        if (!area(x, z)) return;
        int t = th[z * W + x];
        if (get(x, t + 1, z) != AIR) return;
        for (int h = 1; h <= 3; h++) set(x, t + h, z, WWALL);
        set(x, t + 4, z, LANTERN);
    }

    // ------------------------------------------------------------------ arbres : automne dore, orange et rouille, quelques verts
    private int leafFor(int x, int y, int z, int main) {
        double q = rnd(x, y, z);
        if (q < 0.14) return LEAF_G;
        if (q < 0.30) return main == LEAF_O ? LEAF_Y : LEAF_O;
        if (q < 0.36) return LEAF_R;
        return main;
    }

    private void blob(double cx, double cy, double cz, double rad, double flat, int main) {
        int rr = (int) Math.ceil(rad) + 1;
        for (int dx = -rr; dx <= rr; dx++) for (int dy = -rr; dy <= rr; dy++) for (int dz = -rr; dz <= rr; dz++) {
            int bx = (int) Math.round(cx) + dx, by = (int) Math.round(cy) + dy, bz = (int) Math.round(cz) + dz;
            double e = ((bx - cx) * (bx - cx) + (bz - cz) * (bz - cz)) / (rad * rad) + (by - cy) * (by - cy) / (rad * rad * flat * flat) + 0.32 * vn(bx / 2.0 + by * 0.4, bz / 2.0, 23);
            if (e < 1) setIfAir(bx, by, bz, leafFor(bx, by, bz, main));
        }
    }

    private void tree(int x, int z, int h, double rad, int main, long seed) {
        int y0 = th[z * W + x] + 1;
        Random r = new Random(seed);
        double lean = r.nextDouble() * 0.4 - 0.2;
        int tx = x, tz = z;
        for (int y = 0; y < h; y++) {
            if (y > 3 && r.nextDouble() < 0.12) { tx += lean > 0 ? 1 : -1; }
            set(tx, y0 + y, tz, GLOG_Y);
            if (y < 2) { set(tx + 1, y0 + y, tz, GLOG_Y); set(tx, y0 + y, tz + 1, GLOG_Y); }
        }
        for (int i = 0; i < 4; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double bx = tx, bz = tz, by = y0 + h * (0.5 + 0.25 * r.nextDouble());
            int len = 3 + r.nextInt(4);
            for (int k = 0; k < len; k++) { bx += Math.cos(a); bz += Math.sin(a); by += 0.6; set((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), (Math.abs(Math.cos(a)) > 0.7) ? GLOG_X : GLOG_Z); }
            blob(bx, by + 1, bz, rad * 0.62, 0.8, main);
        }
        blob(tx, y0 + h, tz, rad, 0.75, main);
    }

    private void conifer(int x, int z, int h) {
        int y0 = th[z * W + x] + 1;
        for (int y = 0; y < h; y++) set(x, y0 + y, z, SPRLOG);
        for (int y = 2; y < h + 2; y++) {
            double rr = 3.2 * (1 - (y - 2) / (double) (h + 1)) + 0.4;
            int R = (int) Math.ceil(rr);
            if (y % 2 == 1 && y < h - 2) rr *= 0.6;
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) if (Math.hypot(dx, dz) <= rr) setIfAir(x + dx, y0 + y, z + dz, SPRLEAF);
        }
    }

    private void bigTree(int x, int y0, int z) {
        Random r = new Random(4242);
        int h = 30;
        for (int y = 0; y < h; y++) {
            double t = y / (double) h;
            double rad = y < 4 ? 3.2 : t < 0.5 ? 2.5 : t < 0.75 ? 1.8 : 1.2;
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) if (Math.hypot(dx, dz) <= rad) set(x + dx, y0 + y, z + dz, GLOG_Y);
        }
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2 / 7 + r.nextDouble() * 0.4;
            double px = x, pz = z; int py = y0 + 2;
            for (int k = 0; k < 6; k++) { px += Math.cos(a); pz += Math.sin(a); if (k > 1 && r.nextBoolean()) py--; set((int) Math.round(px + Math.cos(a) * 2.5), Math.max(y0, py), (int) Math.round(pz + Math.sin(a) * 2.5), GLOG_Y); }
        }
        List<double[]> tips = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 9 + r.nextDouble() * 0.6;
            double bx = x, bz = z, by = y0 + h * (0.4 + 0.4 * r.nextDouble());
            int len = 7 + r.nextInt(6);
            for (int k = 0; k < len; k++) {
                bx += Math.cos(a); bz += Math.sin(a); by += 0.5; a += (r.nextDouble() - 0.5) * 0.3;
                set((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), Math.abs(Math.cos(a)) > 0.7 ? GLOG_X : GLOG_Z);
                if (k > 2) set((int) Math.round(bx), (int) Math.round(by) - 1, (int) Math.round(bz), GLOG_Y);
            }
            tips.add(new double[]{bx, by, bz, 6 + r.nextDouble() * 2.5});
        }
        tips.add(new double[]{x, y0 + h + 3, z, 8.5});
        for (double[] t : tips) blob(t[0], t[1], t[2], t[3], 0.62, ((int) (t[0] * 3 + t[2]) & 3) < 2 ? LEAF_Y : LEAF_O);
    }

    /** reliefs de facade (hors portes), puis patine generale des pierres basses : mousse, lierre qui pend des ponts et des soubassements */
    private void decorate() {
        for (int[] d : decos) {
            boolean skip = false;
            for (int[] o : doors) if (Math.abs(d[0] - o[0]) <= 3 && Math.abs(d[2] - o[1]) <= 3 && d[1] >= o[2] - 1 && d[1] <= o[2] + 7) { skip = true; break; }
            if (!skip) setIfAir(d[0], d[1], d[2], d[3]);
        }
        int[][] dd = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int z = 4; z < D - 4; z++) for (int x = 4; x < W - 4; x++) for (int y = -OFF + 4; y <= S + 12; y++) {
            int id = gi(x, y, z);
            if (id != STB && id != TSTONE) continue;
            boolean exposed = false;
            for (int[] o : dd) if (gi(x + o[0], y, z + o[1]) == AIR) { exposed = true; break; }
            double q = rnd(x * 11 + z, y, x + z * 13);
            if (exposed && y <= S + 3 && id != WS && q < 0.12) { set(x, y, z, q < 0.04 ? MOSS : MOSSB); continue; }
            if (gi(x, y - 1, z) == AIR && y <= S + 2 && q > 0.9 && y > 3) {
                int len = 1 + (int) (rnd(x, y, z) * 5);
                for (int k = 1; k <= len; k++) { if (gi(x, y - k, z) != AIR) break; set(x, y - k, z, q > 0.96 ? LEAF_Y : LEAF_G); }
            }
        }
    }

    /** la roche est vivante : taches de mousse sur les parois et buissons accroches aux replats, pour casser les falaises nues */
    private void drape() {
        int[][] dd = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int z = 6; z < D - 6; z++) for (int x = 6; x < W - 6; x++) {
            int t = th[z * W + x];
            for (int y = Math.max(-OFF + 3, t - 6); y <= t; y++) {
                int b = gi(x, y, z);
                if (b < STONE || b > MOSSC) continue;
                for (int[] o : dd) {
                    if (get(x + o[0], y, z + o[1]) != AIR) continue;
                    double q = rnd(x * 3 + y, y, z * 5 + o[0]);
                    if (q < 0.07) set(x, y, z, MOSS);
                    else if (q < 0.085 && y > 2) setIfAir(x + o[0], y, z + o[1], q < 0.078 ? LEAF_G : (q < 0.082 ? LEAF_Y : LEAF_O));
                    break;
                }
            }
        }
    }

    private boolean clear(int x, int z, int r) {
        for (int dx = -r; dx <= r; dx += 2) for (int dz = -r; dz <= r; dz += 2) {
            int xx = x + dx, zz = z + dz;
            if (!area(xx, zz) || res[zz * W + xx] || ramp[zz * W + xx] != 0) return false;
            int t = th[zz * W + xx];
            int id = gi(xx, t, zz);
            if (id != GRASS && id != PODZOL && id != COARSE && id != MOSS) return false;
            int up = gi(xx, t + 1, zz);
            if (up != AIR && up != SHORTG && up != FERN) return false;
        }
        return true;
    }

    private void trees() {
        for (int i = 0; i < 4200; i++) {
            int x = BAND + 2 + (int) (rnd(i, 1, 2) * (W - 2 * BAND - 4)), z = BAND + 2 + (int) (rnd(i, 3, 4) * (D - 2 * BAND - 4));
            int t = th[z * W + x];
            if (wl[z * W + x] > -999 || t < 0) continue;
            if (Math.hypot(x - POOL_X, z - POOL_Z) < 40 && t < 30) continue;                     // le bassin de la grande cascade reste degage
            if (t >= 62) {
                if (rnd(i, 5, 6) < 0.6 && clear(x, z, 2) && gi(x, t, z) == GRASS && t < 130) conifer(x, z, 9 + (int) (rnd(i, 7, 8) * 8));
                continue;
            }
            boolean shelf = t >= S - 1 && t <= S + 1;
            if (shelf && rnd(i, 9, 10) < 0.55) continue;                                      // la terrasse reste degagee
            if (!clear(x, z, 3)) continue;
            double q = rnd(i, 11, 12);
            int main = q < 0.4 ? LEAF_O : q < 0.75 ? LEAF_Y : q < 0.9 ? LEAF_R : LEAF_G;
            tree(x, z, 8 + (int) (rnd(i, 13, 14) * 7), 3.4 + rnd(i, 15, 16) * 2.2, main, i * 31L + 7);
        }
    }
}
