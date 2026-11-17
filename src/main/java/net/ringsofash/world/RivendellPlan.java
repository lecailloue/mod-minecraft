package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan de Fondcombe (Rivendell) : une vallee cachee de 352 x 352 blocs, au creux d'un massif. Un plateau porte la grande maison d'Elrond (salle aux longs toits
 * de cuivre orange et vert, deux ailes, tour ronde a toit conique, tour-fleche), une galerie a arcades au-dessus d'une gorge, la terrasse du Conseil et son grand
 * arbre d'automne, des pavillons ouverts, des gloriettes et des jardins ; une riviere longe le plateau a l'est et au sud, enjambee par deux ponts en arc, et
 * quatre cascades tombent de la paroi nord. Grille de blocs abstraits, sans dependance a Minecraft.
 * Repere local : x de 0 a W - 1 (ouest a est), z de 0 a D - 1 (nord a sud) ; y = 0 : niveau du sol au centre du site ; le plateau est a FLOOR.
 */
public final class RivendellPlan {
    public static final int W = 352, D = 352, OFF = 30, TOP = 150, H = OFF + TOP + 1, CX = 176, CZ = 176;
    public static final int FLOOR = 12;                 // niveau du plateau (dernier bloc plein)
    public static final int BAND = 30;                  // largeur de la bande de raccord avec le relief naturel, tout autour

    // terrain et vegetation
    public static final int AIR = 0, STONE = 1, ANDESITE = 2, TUFF = 3, CALCITE = 4, DIORITE = 5, COBBLE = 6, MOSSC = 7, DIRT = 8, GRASS = 9, PODZOL = 10, GRAVEL = 11,
            SAND = 12, COARSE = 13, MOSS = 14, WATER = 15, WATERF = 16, SHORTG = 17, FERN = 18, POPPY = 19, DANDEL = 20, CORNFL = 21, BLUET = 22, SPRLOG = 23, SPRLEAF = 24;
    // architecture
    public static final int WB = 32, WS = 33, WPIL = 34, QB = 35, SQ = 36, GP = 37, GLOG_Y = 38, GLOG_X = 39, GLOG_Z = 40, GOLDB = 41, GLASS = 42, BARS = 43, LANTERN = 44,
            ENDROD = 45, GFENCE = 46, WWALL = 47, CHAIN = 48, HLANT = 49, LEAF_G = 50, LEAF_Y = 51, LEAF_O = 52, LEAF_R = 53, LEAF_B = 54, CUOB = 55, CUGB = 56, CUWB = 57,
            STB = 58, MOSSB = 59, PAVE2 = 60, LIGHT = 61, WLOG = 62, BENCHW = 63;
    // familles d'escaliers : base + direction * 2 + moitie haute (direction = sens de la montee : 0 N, 1 E, 2 S, 3 O) ; base + 8 dalle basse, base + 9 dalle haute
    public static final int WBS = 64, GPS = 76, CUOS = 88, CUGS = 100, CUWS = 112, STBS = 124, QS = 136;
    public static final int N = 0, E = 1, S = 2, W_ = 3;

    private final byte[] g = new byte[W * H * D];
    private final int[] th = new int[W * D];                       // y du dernier bloc plein du terrain
    private final short[] wl = new short[W * D];                   // niveau de l'eau (sommet), ou -999
    private final byte[] edge = new byte[W * D];                   // 0 en bordure (relief naturel), 255 au coeur : poids du plan
    private final int[] top = new int[W * D];
    public final List<int[]> spawns = new ArrayList<>();           // {x, y, z}

    private static RivendellPlan inst;
    public static synchronized RivendellPlan get() { if (inst == null) inst = new RivendellPlan(); return inst; }

    // ------------------------------------------------------------------ outils
    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public byte get(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    private void setIfAir(int x, int y, int z, int id) { if (ok(x, y, z) && g[idx(x, y, z)] == AIR) g[idx(x, y, z)] = (byte) id; }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int top(int x, int z) { return top[z * W + x]; }
    public int terrain(int x, int z) { return th[z * W + x]; }
    public int water(int x, int z) { return wl[z * W + x]; }
    public int weight(int x, int z) { return edge[z * W + x] & 255; }

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
    private void walls(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) if (x == x0 || x == x1 || z == z0 || z == z1) set(x, y, z, id);
    }

    // ------------------------------------------------------------------ geometrie des eaux
    private static final double[][] LOW = {{285, 148}, {287, 185}, {272, 232}, {235, 268}, {185, 288}, {125, 300}, {70, 322}, {45, 351}};
    private static final double[][] UP = {{250, 108}, {268, 118}, {283, 130}, {285, 148}};
    private static final int WL_LOW = -12, BED_LOW = -19, WL_UP = 9, BED_UP = 6;
    private static final int[] FALL_X = {70, 122, 205, 250};

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
        materialize();
        paths();
        hall();
        towers();
        gallery();
        council();
        pavilions();
        bridges();
        gardens();
        trees();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    private double base(int x, int z) {
        double u = x + 0.5 - CX, v = z + 0.5 - CZ;
        double d = Math.hypot(u / 1.15, v);
        double rim = 70 * sm((d - 105) / 80) * (0.8 + 0.4 * vn(x / 37.0, z / 37.0, 3));
        double north = 95 * sm((100 - z) / 62.0);
        double west = 42 * sm((72 - x) / 60.0);
        double east = 58 * sm((x - 340) / 14.0);
        double south = -22 * sm((z - 258) / 75.0);
        double far = sm((d - 60) / 60);                                              // le relief ne bouge qu'au-dela du plateau
        double rough = (9 * vn(x / 30.0, z / 30.0, 1) + 4 * vn(x / 11.0, z / 11.0, 2) + 14 * Math.pow(1 - Math.abs(vn(x / 21.0, z / 21.0, 5)), 2) * sm((100 - z) / 40.0)) * far;
        double t = FLOOR + rim + north + west + east + south + rough + 0.7 * vn(x / 9.0, z / 9.0, 6) * (1 - far);
        if (t > 110) t = 110 + (t - 110) * 0.4;
        return t;
    }

    private void terrain() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            double t = base(x, z);
            double dl = dist(x + 0.5, z + 0.5, LOW), du = dist(x + 0.5, z + 0.5, UP);
            double gorge = BED_LOW + Math.max(0, dl - 7) * 3.2;
            if (gorge < t) t = gorge;
            double stream = BED_UP + Math.max(0, du - 5) * 1.0;
            if (stream < t) t = stream;
            int water = -999;
            if (dl < 14 && t < WL_LOW) water = WL_LOW;
            if (du < 12 && t < WL_UP) water = Math.max(water, WL_UP);
            th[i] = (int) Math.round(t);
            wl[i] = (short) water;
            int e = Math.min(Math.min(x, z), Math.min(W - 1 - x, D - 1 - z));
            edge[i] = (byte) Math.round(255 * sm(e / (double) BAND));
        }
        // cascades de la paroi nord : un filet d'eau de 3 blocs de large (5 pour la plus grande) qui descend la pente jusqu'a une vasque au pied
        for (int k = 0; k < FALL_X.length; k++) {
            int fx = FALL_X[k], half = k == 1 ? 2 : 1;
            int zTop = 108;
            while (zTop > 20 && th[zTop * W + fx] < 80) zTop--;
            for (int z = 106; z >= zTop; z--) {
                int cx = fx + (int) Math.round(3 * vn(z / 14.0, k * 5 + 0.5, 70 + k));
                for (int dx = -half - 1; dx <= half + 1; dx++) {
                    int i = z * W + cx + dx;
                    if (Math.abs(dx) <= half) { th[i] = th[i] - 1; wl[i] = (short) th[i + 0 * W]; }
                    else th[i] = th[i] + 0;
                }
            }
            int py = 112;                                                                       // vasque
            for (int dz = -8; dz <= 8; dz++) for (int dx = -8; dx <= 8; dx++) {
                double r = Math.hypot(dx, dz) * (1 + 0.25 * vn((fx + dx) / 4.0, (py + dz) / 4.0, 80));
                int i = (py + dz) * W + fx + dx;
                if (r < 6.5) { th[i] = Math.min(th[i], 6); wl[i] = (short) 11; }
                else if (r < 8.5) th[i] = Math.min(th[i], 12 + (int) Math.round((r - 6.5) * 0.5) - 2);
            }
        }
        // la derniere cascade alimente le ruisseau (la vasque est a 11, le lit du ruisseau commence a 6 : on les relie)
        // la cascade est stockee sous forme de cellules d'eau a la surface : on reperera plus loin (materialize) les cellules dont wl == th
    }

    // ------------------------------------------------------------------ matiere du terrain
    private static final int[] STRATA = {STONE, ANDESITE, STONE, TUFF, CALCITE, DIORITE, STONE, ANDESITE};

    private int rock(int x, int y, int z, boolean surface) {
        double layer = Math.floor((y + 5 * vn(x / 23.0, z / 23.0, 11) + 2 * vn(x / 7.0, z / 7.0, 12)) / 4.0);
        int k = (int) (rnd((int) layer, 17, 3) * 8) & 7;
        double q = rnd(x, y, z);
        if (surface && y < 0 && q < 0.25) return MOSSC;
        if (surface && vn(x / 3.0 + y / 5.0, z / 3.0, 57) > 0.74) return COBBLE;
        if (q < 0.15) return STRATA[(k + 3) & 7];
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
            boolean gentle = s <= 2 && t > -3 && t < 105;
            boolean wet = wl[i] > -999;
            for (int y = -OFF + 2; y <= t; y++) {
                int id;
                int depth = t - y;
                if (depth == 0) {
                    double q = rnd(x, 3, z);
                    if (wet && t < wl[i]) id = q < 0.4 ? GRAVEL : q < 0.7 ? SAND : COBBLE;
                    else if (gentle) id = q < 0.05 ? PODZOL : q < 0.09 ? COARSE : q < 0.10 ? MOSS : GRASS;
                    else id = rock(x, y, z, true);
                } else if (depth <= 3 && gentle) id = rnd(x, y, z) < 0.2 ? COARSE : DIRT;
                else id = rock(x, y, z, false);
                set(x, y, z, id);
            }
            if (wet) for (int y = t + 1; y <= wl[i]; y++) set(x, y, z, WATER);
        }
        // cascades : de l'eau tombante au ras de la pente, par-dessus la roche
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            if (wl[i] > -999 && wl[i] == th[i] && th[i] > 14) {
                int y = th[i];
                set(x, y, z, WATERF);
                // la roche de dessous devient lit de ruisseau
                set(x, y - 1, z, STONE);
            }
        }
        // chute du ruisseau dans la gorge : filet tombant au droit de la tete de la gorge
        for (int z = 126; z <= 146; z++) for (int dx = -2; dx <= 2; dx++) {
            int x = 285 + dx, i = z * W + x;
            int t = th[i];
            if (t > WL_LOW && wl[i] == -999) { set(x, t, z, WATERF); }
        }
    }

    // ------------------------------------------------------------------ chemins : pierre blanche, en larges rubans
    private void pave(int x, int z, int id) {
        if (!area(x, z)) return;
        int y = th[z * W + x];
        if (y != FLOOR) return;
        if (get(x, y, z) == WATERF || get(x, y, z) == WATER) return;
        set(x, y, z, id);
    }

    private void line(double x0, double z0, double x1, double z1, int w, boolean alt) {
        double len = Math.hypot(x1 - x0, z1 - z0);
        for (double t = 0; t <= len; t += 0.5) {
            double px = x0 + (x1 - x0) * t / len, pz = z0 + (z1 - z0) * t / len;
            for (int dx = -w; dx <= w; dx++) for (int dz = -w; dz <= w; dz++) {
                if (dx * dx + dz * dz > w * w + 1) continue;
                int x = (int) Math.round(px) + dx, z = (int) Math.round(pz) + dz;
                double q = rnd(x, 41, z);
                pave(x, z, alt && ((x / 3 + z / 3) & 1) == 0 ? PAVE2 : (q < 0.1 ? STB : WS));
            }
        }
    }

    private void paths() {
        line(70, 176, 176, 176, 2, true);        // la grande allee, devant la maison
        line(176, 176, 268, 170, 2, true);       // vers le pont de l'est
        line(176, 176, 176, 235, 2, true);       // vers les jardins
        line(176, 176, 210, 262, 1, false);      // vers le pont du sud
        line(150, 150, 150, 176, 1, false);
        line(96, 176, 80, 196, 1, false);
        line(222, 176, 222, 200, 1, false);
        line(176, 110, 176, 146, 1, false);      // vers la salle du nord
        line(112, 176, 112, 146, 1, false);
    }

    // ------------------------------------------------------------------ toits
    private int roofStair(int x, int z, int i, int dir) {
        int m = ((x / 2) + (z / 2) + i) & 3;
        return stair(m == 0 || m == 3 ? CUOS : m == 1 ? CUGS : CUWS, dir, false);
    }
    private int roofBlock(int x, int z, int i) {
        int m = ((x / 2) + (z / 2) + i) & 3;
        return m == 0 || m == 3 ? CUOB : m == 1 ? CUGB : CUWB;
    }

    /** toit a deux pans dont le faitage suit x : les murs vont de z0 a z1 (1 bloc de debord) ; les pignons sont fermes par du mur blanc */
    private void gableX(int x0, int x1, int z0, int z1, int y) {
        int za = z0 - 1, zb = z1 + 1;
        for (int i = 0; za + i <= zb - i; i++) {
            int a = za + i, b = zb - i, yy = y + i;
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                if (a == b) { set(x, yy, a, roofBlock(x, a, i)); continue; }
                set(x, yy, a, roofStair(x, a, i, S));
                set(x, yy, b, roofStair(x, b, i, N));
            }
            for (int z = a + 1; z <= b - 1; z++) { set(x0, yy, z, WB); set(x1, yy, z, WB); }
            if (a == b - 1) { for (int x = x0 - 1; x <= x1 + 1; x++) set(x, yy + 1, a, slab(WBS, false)); }
        }
    }

    private void gableZ(int x0, int x1, int z0, int z1, int y) {
        int xa = x0 - 1, xb = x1 + 1;
        for (int i = 0; xa + i <= xb - i; i++) {
            int a = xa + i, b = xb - i, yy = y + i;
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                if (a == b) { set(a, yy, z, roofBlock(a, z, i)); continue; }
                set(a, yy, z, roofStair(a, z, i, E));
                set(b, yy, z, roofStair(b, z, i, W_));
            }
            for (int x = a + 1; x <= b - 1; x++) { set(x, yy, z0, WB); set(x, yy, z1, WB); }
            if (a == b - 1) { for (int z = z0 - 1; z <= z1 + 1; z++) set(a, yy + 1, z, slab(WBS, false)); }
        }
    }

    /** toit en pyramide (pavillons, gloriettes) : on peut le laisser ouvert par en dessous */
    private void hip(int x0, int z0, int x1, int z1, int y, boolean spire) {
        int i = 0;
        while (x0 + i <= x1 - i && z0 + i <= z1 - i) {
            int xa = x0 + i, xb = x1 - i, za = z0 + i, zb = z1 - i, yy = y + i;
            for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) {
                boolean ring = x == xa || x == xb || z == za || z == zb;
                if (!ring) continue;
                if (xa == xb || za == zb) { set(x, yy, z, roofBlock(x, z, i)); continue; }
                int dir = z == za ? S : z == zb ? N : x == xa ? E : W_;
                set(x, yy, z, roofStair(x, z, i, dir));
            }
            i++;
        }
        if (spire) {
            int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
            for (int k = 0; k < 6; k++) set(cx, y + i + k, cz, k < 4 ? GFENCE : ENDROD);
        }
    }

    /** toit conique (tours rondes) */
    private void cone(int cx, int cz, int r, int y, int height) {
        for (int i = 0; i < height; i++) {
            double ri = (r + 1.5) * (1 - i / (double) height);
            int R = (int) Math.ceil(ri) + 1;
            for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                double d = Math.hypot(dx, dz);
                if (d <= ri && d > ri - 1.6) set(cx + dx, y + i, cz + dz, roofBlock(cx + dx, cz + dz, i));
            }
        }
        for (int k = 0; k < 7; k++) set(cx, y + height + k, cz, k < 4 ? GFENCE : k == 4 ? GOLDB : ENDROD);
    }

    // ------------------------------------------------------------------ murs : pilastres, fenetres en ogive, bandeau dore
    private void wallX(int x0, int x1, int z, int y0, int h, int bay, boolean glassWall) {
        for (int x = x0; x <= x1; x++) {
            int p = (x - x0) % bay;
            boolean pil = p == 0 || x == x1;
            for (int y = y0; y < y0 + h; y++) {
                int id = WB;
                int dy = y - y0;
                if (pil) id = (dy == 0 || dy == h - 1) ? QB : WPIL;
                else if (dy == 2) id = GP;                                                  // appui
                else if (dy >= 3 && dy <= h - 4) {
                    int c = bay / 2;
                    int a = Math.abs(p - c), top = h - 4;
                    boolean win = a <= 1 && dy <= top - 1 || a == 0 && dy == top;
                    if (win) id = GLASS;
                    else if (dy == top - 1 && a == 2 || dy == top && a == 1) id = GP;
                } else if (dy == h - 3) id = GP;
                else if (dy == h - 1) id = GP;
                set(x, y, z, id);
            }
        }
    }

    private void wallZ(int x, int z0, int z1, int y0, int h, int bay) {
        for (int z = z0; z <= z1; z++) {
            int p = (z - z0) % bay;
            boolean pil = p == 0 || z == z1;
            for (int y = y0; y < y0 + h; y++) {
                int id = WB, dy = y - y0;
                if (pil) id = (dy == 0 || dy == h - 1) ? QB : WPIL;
                else if (dy == 2) id = GP;
                else if (dy >= 3 && dy <= h - 4) {
                    int c = bay / 2, a = Math.abs(p - c), top = h - 4;
                    boolean win = a <= 1 && dy <= top - 1 || a == 0 && dy == top;
                    if (win) id = GLASS;
                    else if (dy == top - 1 && a == 2 || dy == top && a == 1) id = GP;
                } else if (dy == h - 3 || dy == h - 1) id = GP;
                set(x, y, z, id);
            }
        }
    }

    private void door(int cx, int z, int y0, boolean alongX) {       // porte voutee de 3 de large, 6 de haut, dans un mur de plan z (ou x)
        for (int d = -1; d <= 1; d++) for (int y = y0; y <= y0 + 4; y++) { if (alongX) set(cx + d, y, z, AIR); else set(z, y, cx + d, AIR); }
        if (alongX) set(cx, y0 + 5, z, AIR); else set(z, y0 + 5, cx, AIR);
        for (int d = -2; d <= 2; d++) { if (alongX) set(cx + d, y0 + 5 + (Math.abs(d) <= 0 ? 1 : 0), z, GP); else set(z, y0 + 5 + (Math.abs(d) <= 0 ? 1 : 0), cx + d, GP); }
        if (alongX) { set(cx - 1, y0 + 5, z, GP); set(cx + 1, y0 + 5, z, GP); } else { set(z, y0 + 5, cx - 1, GP); set(z, y0 + 5, cx + 1, GP); }
    }

    private void lanternsHung(int x0, int x1, int z0, int z1, int y, int step) {
        for (int x = x0; x <= x1; x += step) for (int z = z0; z <= z1; z += step) { set(x, y, z, CHAIN); set(x, y - 1, z, HLANT); }
    }

    // ------------------------------------------------------------------ la grande salle et ses ailes
    private void hall() {
        int y0 = FLOOR + 1, h = 11;
        // salle principale : faitage est-ouest
        int x0 = 92, x1 = 200, z0 = 146, z1 = 170;
        box(x0, FLOOR, z0, x1, FLOOR, z1, GP);
        walls(x0, y0, z0, x1, y0 + h - 1, z1, WB);
        wallX(x0, x1, z0, y0, h, 6, true);
        wallX(x0, x1, z1, y0, h, 6, true);
        wallZ(x0, z0, z1, y0, h, 6);
        wallZ(x1, z0, z1, y0, h, 6);
        for (int d = 0; d < 3; d++) { door(176 - 12 + d * 12, z1, y0, true); }        // trois portes sur le jardin
        for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++) box(x, y0, z, x, y0 + h + 14, z, AIR);
        gableX(x0, x1, z0, z1, y0 + h);
        lanternsHung(x0 + 6, x1 - 6, z0 + 6, z1 - 4, y0 + h - 1, 12);
        // deux ailes au nord (faitage nord-sud)
        wing(112, 132, 124, 145, y0, h - 1);
        wing(164, 184, 124, 145, y0, h - 1);
        // salle du feu, grande salle du nord
        int hx0 = 134, hx1 = 162, hz0 = 104, hz1 = 128;
        box(hx0, FLOOR, hz0, hx1, FLOOR, hz1, GP);
        walls(hx0, y0, hz0, hx1, y0 + h, hz1, WB);
        wallX(hx0, hx1, hz0, y0, h + 1, 7, true);
        wallX(hx0, hx1, hz1, y0, h + 1, 7, true);
        wallZ(hx0, hz0, hz1, y0, h + 1, 6);
        wallZ(hx1, hz0, hz1, y0, h + 1, 6);
        for (int x = hx0 + 1; x < hx1; x++) for (int z = hz0 + 1; z < hz1; z++) box(x, y0, z, x, y0 + h + 12, z, AIR);
        door((hx0 + hx1) / 2, hz1, y0, true);
        gableX(hx0, hx1, hz0, hz1, y0 + h + 1);
        // porche a arcades au sud de la salle
        porch(106, 194, z1 + 1, z1 + 5, y0);
        // la tour du grand toit, au milieu de l'arriere (comme sur la maquette) : petite tour carree a toit pointu
        int tx = 150, tz = 140;
        walls(tx - 3, y0, tz - 3, tx + 3, y0 + h + 18, tz + 3, WB);
        for (int d = -1; d <= 1; d++) for (int y = y0; y < y0 + 4; y++) set(tx + d, y, tz + 3, AIR);
        for (int y = y0 + 6; y < y0 + h + 18; y += 5) { set(tx, y, tz - 3, GLASS); set(tx, y, tz + 3, GLASS); set(tx - 3, y, tz, GLASS); set(tx + 3, y, tz, GLASS); }
        box(tx - 4, y0 + h + 19, tz - 4, tx + 4, y0 + h + 19, tz + 4, QB);
        hip(tx - 3, tz - 3, tx + 3, tz + 3, y0 + h + 20, true);
    }

    private void wing(int x0, int x1, int z0, int z1, int y0, int h) {
        box(x0, FLOOR, z0, x1, FLOOR, z1 + 2, GP);
        walls(x0, y0, z0, x1, y0 + h - 1, z1, WB);
        wallX(x0, x1, z0, y0, h, 7, true);
        wallZ(x0, z0, z1, y0, h, 7);
        wallZ(x1, z0, z1, y0, h, 7);
        for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z <= z1; z++) box(x, y0, z, x, y0 + h + 12, z, AIR);
        gableZ(x0, x1, z0, z1 + 4, y0 + h);
    }

    /** galerie ouverte : colonnes fines, arcs en ogive, toit en appentis en tuiles */
    private void porch(int x0, int x1, int z0, int z1, int y0) {
        for (int x = x0; x <= x1; x += 6) for (int y = y0; y < y0 + 8; y++) set(x, y, z1, y < y0 + 1 ? QB : WPIL);
        for (int x = x0; x < x1; x += 6) {
            set(x + 1, y0 + 7, z1, stair(WBS, E, true)); set(x + 5, y0 + 7, z1, stair(WBS, W_, true));
            set(x + 2, y0 + 8, z1, stair(WBS, E, true)); set(x + 4, y0 + 8, z1, stair(WBS, W_, true)); set(x + 3, y0 + 8, z1, WB);
            for (int dx = 2; dx <= 4; dx++) set(x + dx, y0 + 7, z1, GFENCE);
            set(x + 3, y0 + 6, z1, GFENCE);
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) set(x, y0 + 9, z, roofBlock(x, z, 0));
            set(x, y0 + 9, z1 + 1, roofStair(x, z1 + 1, 0, N));
        }
        for (int x = x0; x <= x1; x += 6) set(x, y0 + 8, z1, QB);
        for (int x = x0; x <= x1; x += 12) { set(x, y0 + 8, z1 - 2, CHAIN); set(x, y0 + 7, z1 - 2, HLANT); }
    }

    // ------------------------------------------------------------------ tours
    private void roundTower(int cx, int cz, int r, int y0, int h, int coneH, boolean bands) {
        for (int y = y0; y < y0 + h; y++) {
            for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.hypot(dx, dz);
                if (d <= r + 0.4 && d > r - 0.8) {
                    int dy = y - y0;
                    int id = WB;
                    boolean slit = dy % 9 == 5 && ((Math.abs(dx) <= 0 && Math.abs(dz) >= r - 1) || (Math.abs(dz) <= 0 && Math.abs(dx) >= r - 1) || (Math.abs(dx) == Math.abs(dz) && d > r - 1));
                    if (dy % 9 == 8 && bands) id = GP;
                    if (slit && dy > 2) id = GLASS;
                    set(cx + dx, y, cz + dz, id);
                } else if (d <= r - 0.8) set(cx + dx, y, cz + dz, AIR);
            }
            if ((y - y0) % 9 == 0 && y > y0) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) if (Math.hypot(dx, dz) <= r - 0.8) set(cx + dx, y, cz + dz, GP);
        }
        // corniche
        int yt = y0 + h;
        for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
            double d = Math.hypot(dx, dz);
            if (d <= r + 1.6) set(cx + dx, yt, cz + dz, d > r + 0.6 ? QB : GP);
        }
        for (int a = 0; a < 16; a++) {
            double an = a * Math.PI / 8;
            set(cx + (int) Math.round(Math.cos(an) * (r + 1.3)), yt + 1, cz + (int) Math.round(Math.sin(an) * (r + 1.3)), WWALL);
        }
        // porte au sud
        for (int d = -1; d <= 1; d++) for (int y = y0; y < y0 + 4; y++) { set(cx + d, y, cz + r, AIR); set(cx + d, y, cz + r - 1, AIR); }
        set(cx, y0 + 4, cz + r, AIR);
        cone(cx, cz, r + 1, yt + 2, coneH);
    }

    private void spire(int cx, int cz, int y0, int h) {
        // tour-fleche : fut carre effile, balcon, pointe doree (comme la haute fleche des maquettes)
        for (int y = 0; y < h; y++) {
            int r = y < 10 ? 3 : y < 24 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                if (y == 0 || y == 9 || y == 23) set(cx + dx, y0 + y, cz + dz, GP);
                else set(cx + dx, y0 + y, cz + dz, edge ? (Math.abs(dx) == r && Math.abs(dz) == r ? WPIL : (y % 6 == 4 && r >= 2 ? GLASS : WB)) : AIR);
            }
        }
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) { if (Math.max(Math.abs(dx), Math.abs(dz)) == 4) set(cx + dx, y0 + 10, cz + dz, WWALL); }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) set(cx + dx, y0 + 9, cz + dz, GP);
        for (int k = 0; k < 14; k++) set(cx, y0 + h + k, cz, k < 9 ? (k % 3 == 2 ? GOLDB : GFENCE) : ENDROD);
        set(cx, y0 + h + 14, cz, LANTERN);
    }

    private void towers() {
        roundTower(84, 158, 7, FLOOR + 1, 36, 20, true);
        spire(96, 122, FLOOR + 1, 44);
        roundTower(224, 128, 5, FLOOR + 1, 22, 14, true);
    }

    // ------------------------------------------------------------------ galerie a arcades au-dessus de la gorge
    private void gallery() {
        int x0 = 226, x1 = 276, z0 = 140, z1 = 164, y0 = FLOOR + 1;
        // plateforme prolongee au-dessus de la gorge, sur des piles
        for (int x = 262; x <= x1; x++) for (int z = z0; z <= z1; z++) set(x, FLOOR, z, WS);
        for (int x = 264; x <= x1; x += 6) for (int z = z0 + 1; z <= z1; z += 8) pier(x, z, FLOOR - 1);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (x < 262) set(x, FLOOR, z, ((x + z) & 1) == 0 ? WS : PAVE2);
        // deux rangees d'arcades (nord et sud) et le bord est ; toit en pyramide allongee
        for (int x = x0; x <= x1; x += 5) for (int y = y0; y < y0 + 9; y++) for (int z : new int[]{z0, z1}) set(x, y, z, y == y0 ? QB : WPIL);
        for (int z = z0; z <= z1; z += 6) for (int y = y0; y < y0 + 9; y++) set(x1, y, z, y == y0 ? QB : WPIL);
        for (int x = x0; x < x1; x += 5) for (int z : new int[]{z0, z1}) {
            set(x + 1, y0 + 8, z, stair(WBS, E, true)); set(x + 4, y0 + 8, z, stair(WBS, W_, true));
            for (int dx = 2; dx <= 3; dx++) { set(x + dx, y0 + 8, z, WB); set(x + dx, y0 + 7, z, GFENCE); set(x + dx, y0 + 6, z, GFENCE); }
            set(x + 1, y0 + 7, z, GFENCE); set(x + 4, y0 + 7, z, GFENCE);
        }
        for (int z = z0; z < z1; z += 6) {
            set(x1, y0 + 8, z + 1, stair(WBS, S, true)); set(x1, y0 + 8, z + 5, stair(WBS, N, true));
            for (int dz = 2; dz <= 4; dz++) { set(x1, y0 + 8, z + dz, WB); set(x1, y0 + 7, z + dz, GFENCE); }
        }
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) set(x, y0 + 9, z, QB);
        for (int x = x1; x >= x0 + 6; x -= 0) break;
        gableX(x0 + 2, x1 - 2, z0 + 1, z1 - 1, y0 + 10);
        // balustrade au bord de la gorge
        for (int z = z0; z <= z1; z++) if (z % 2 == 0) { set(x1 + 1, y0, z, WWALL); }
        lanternsHung(x0 + 4, x1 - 4, z0 + 6, z1 - 4, y0 + 8, 8);
        spawns.add(new int[]{240, FLOOR + 1, 152});
    }

    private void pier(int x, int z, int yTop) {
        for (int y = yTop; y > -OFF + 2; y--) {
            if (get(x, y, z) != AIR && get(x, y, z) != WATER && get(x, y, z) != WATERF) break;
            set(x, y, z, y > yTop - 3 ? WPIL : STB);
            if (y % 6 == 0) { set(x + 1, y, z, STB); set(x - 1, y, z, STB); }
        }
    }

    // ------------------------------------------------------------------ terrasse du Conseil et son grand arbre
    private void council() {
        int cx = 222, cz = 206, r = 22, y = FLOOR;
        for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
            double d = Math.hypot(dx, dz);
            int x = cx + dx, z = cz + dz;
            if (!area(x, z) || th[z * W + x] != FLOOR) continue;
            if (d <= r) {
                double an = Math.atan2(dz, dx);
                boolean star = d < 13 && Math.abs(Math.cos(an * 4 + 0.4)) * 10 > d - 3 && d > 2;
                boolean ring = Math.abs(d - 8) < 0.6 || Math.abs(d - 15) < 0.6;
                int id = star ? GP : ring ? PAVE2 : (((x + z) & 3) == 0 ? PAVE2 : WS);
                if (d < 2.5) id = GOLDB;
                set(x, y, z, id);
            } else if (d <= r + 1.5) { set(x, y, z, QB); set(x, y + 1, z, WWALL); }
        }
        // chaises du Conseil : cercle de bancs tournes vers le centre
        for (int k = 0; k < 14; k++) {
            double an = k * Math.PI * 2 / 14 + 0.2;
            int x = cx + (int) Math.round(Math.cos(an) * 10.5), z = cz + (int) Math.round(Math.sin(an) * 10.5);
            int dir = Math.abs(Math.cos(an)) > Math.abs(Math.sin(an)) ? (Math.cos(an) > 0 ? W_ : E) : (Math.sin(an) > 0 ? N : S);
            set(x, y + 1, z, stair(GPS, dir, false));
            if (k % 2 == 0) spawns.add(new int[]{x, y + 1, z});
        }
        // le grand arbre : au nord de la terrasse, ses branches couvrent le bout de la maison
        bigTree(206, FLOOR + 1, 182);
        // lanternes sur poteaux autour de la terrasse
        for (int k = 0; k < 10; k++) {
            double an = k * Math.PI * 2 / 10 + 0.15;
            int x = cx + (int) Math.round(Math.cos(an) * (r - 0.5)), z = cz + (int) Math.round(Math.sin(an) * (r - 0.5));
            for (int h = 1; h <= 4; h++) set(x, y + h, z, WWALL);
            set(x, y + 5, z, LANTERN);
        }
    }

    // ------------------------------------------------------------------ pavillons et gloriettes
    private void pavilion(int cx, int cz, int hw, int y0) {
        int x0 = cx - hw, x1 = cx + hw, z0 = cz - hw, z1 = cz + hw;
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) if (th[z * W + x] == FLOOR) set(x, FLOOR, z, ((x + z) & 1) == 0 ? WS : PAVE2);
        for (int x = x0; x <= x1; x += hw) for (int z = z0; z <= z1; z += hw) for (int y = y0; y < y0 + 8; y++) set(x, y, z, y == y0 ? QB : WPIL);
        for (int s = 0; s < 4; s++) {
            for (int k = 1; k < hw; k++) {
                int x = s < 2 ? x0 + k : (s == 2 ? x0 : x1), z = s < 2 ? (s == 0 ? z0 : z1) : z0 + k;
                if (k == hw / 2 && hw >= 3) continue;
                set(x, y0 + 7, z, GFENCE); set(x, y0 + 6, z, k % 2 == 0 ? GFENCE : WWALL);
            }
            set(s < 2 ? cx : (s == 2 ? x0 : x1), y0 + 7, s < 2 ? (s == 0 ? z0 : z1) : cz, GFENCE);
        }
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (x == x0 || x == x1 || z == z0 || z == z1) set(x, y0 + 8, z, QB);
        hip(x0 - 1, z0 - 1, x1 + 1, z1 + 1, y0 + 9, true);
        set(cx, y0 + 7, cz, CHAIN); set(cx, y0 + 6, cz, HLANT);
        for (int dx = -1; dx <= 1; dx += 2) { set(cx + dx * 2, y0, cz, stair(GPS, dx > 0 ? W_ : E, false)); }
    }

    private void gazebo(int cx, int cz, int y0) {
        // gloriette blanche a colonnes et dentelle de bois dore, toit en dome ajoure
        int r = 6;
        for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) {
            double d = Math.hypot(dx, dz);
            int x = cx + dx, z = cz + dz;
            if (!area(x, z) || th[z * W + x] != FLOOR) continue;
            if (d <= r + 0.8) set(x, FLOOR, z, d > r - 0.6 ? QB : (((x + z) & 1) == 0 ? WS : PAVE2));
        }
        for (int k = 0; k < 8; k++) {
            double an = k * Math.PI / 4;
            int x = cx + (int) Math.round(Math.cos(an) * (r - 0.5)), z = cz + (int) Math.round(Math.sin(an) * (r - 0.5));
            for (int y = y0; y < y0 + 10; y++) set(x, y, z, y == y0 ? QB : WPIL);
            // arc entre deux colonnes : dentelle
            double an2 = (k + 0.5) * Math.PI / 4;
            int mx = cx + (int) Math.round(Math.cos(an2) * (r - 0.5)), mz = cz + (int) Math.round(Math.sin(an2) * (r - 0.5));
            for (int y = y0 + 6; y < y0 + 10; y++) set(mx, y, mz, y == y0 + 9 ? WB : GFENCE);
            set(mx, y0 + 1, mz, WWALL);
        }
        for (int i = 0; i < 6; i++) {
            double ri = (r + 0.6) * Math.cos(i * 0.3) - i * 0.6;
            for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.hypot(dx, dz);
                if (d <= ri && d > ri - 1.2 && ((dx + dz + i) & 1) == 0) set(cx + dx, y0 + 10 + i, cz + dz, i % 2 == 0 ? WB : GFENCE);
            }
        }
        for (int k = 0; k < 6; k++) set(cx, y0 + 10 + k, cz, k < 4 ? GFENCE : ENDROD);
        set(cx, y0 + 9, cz, CHAIN); set(cx, y0 + 8, cz, HLANT);
    }

    private void pavilions() {
        pavilion(140, 208, 5, FLOOR + 1);
        pavilion(80, 192, 4, FLOOR + 1);
        pavilion(246, 244, 5, FLOOR + 1);
        gazebo(176, 226, FLOOR + 1);
        gazebo(324, 176, FLOOR + 1);
        gazebo(120, 112, FLOOR + 1);
        spawns.add(new int[]{324, FLOOR + 1, 168});
    }

    // ------------------------------------------------------------------ ponts en arc
    private void bridgeX(int xa, int xb, int z, int y) {
        int hw = 3, xm = (xa + xb) / 2, half = (xb - xa) / 2;
        for (int x = xa; x <= xb; x++) {
            double t = (x - xm) / (double) half;
            int yb = y - 3 - (int) Math.round(22 * t * t);
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

    private void bridgeZ(int x, int za, int zb, int y) {
        int hw = 3, zm = (za + zb) / 2, half = (zb - za) / 2;
        for (int z = za; z <= zb; z++) {
            double t = (z - zm) / (double) half;
            int yb = y - 3 - (int) Math.round(18 * t * t);
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

    private void bridges() {
        bridgeX(262, 304, 170, FLOOR);
        // descente de l'est : le pont aboutit sur la terrasse de l'est, a la meme hauteur
        bridgeZ(210, 256, 304, FLOOR);
        // escalier du bout sud du pont, jusqu'au sol de la vallee
        int zEnd = 305;
        for (int k = 0; k < 14; k++) {
            int zz = zEnd + k, yy = FLOOR - 1 - k;
            for (int dx = -3; dx <= 3; dx++) {
                int x = 210 + dx;
                if (!area(x, zz)) continue;
                int t = th[zz * W + x];
                if (yy <= t) break;
                set(x, yy, zz, stair(WBS, S, false) - 0);
                for (int y = yy - 1; y > Math.max(t, yy - 14); y--) set(x, y, zz, WB);
            }
        }
    }

    // ------------------------------------------------------------------ jardins
    private void gardens() {
        // bassin et fontaine devant la grande allee
        int fx = 176, fz = 190;
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            double d = Math.hypot(dx, dz);
            if (d <= 6) { set(fx + dx, FLOOR, fz + dz, d > 5 ? QB : WATER); if (d > 5) set(fx + dx, FLOOR + 1, fz + dz, WWALL); }
        }
        box(fx - 1, FLOOR, fz - 1, fx + 1, FLOOR + 2, fz + 1, QB);
        for (int y = 3; y <= 6; y++) set(fx, FLOOR + y, fz, y < 6 ? WPIL : WATERF);
        set(fx, FLOOR + 7, fz, LANTERN);
        // haies et massifs le long de l'allee
        for (int x = 100; x <= 252; x += 1) {
            if (x > 164 && x < 188) continue;
            for (int s = 0; s < 2; s++) {
                int z = 180 + s * 12;
                if (area(x, z) && th[z * W + x] == FLOOR && get(x, FLOOR, z) == GRASS) {
                    double q = rnd(x, 7 + s, z);
                    if (q < 0.5) { set(x, FLOOR + 1, z, LEAF_G); if (q < 0.2) set(x, FLOOR + 2, z, LEAF_G); }
                    else if (q < 0.65) set(x, FLOOR + 1, z, POPPY);
                    else if (q < 0.8) set(x, FLOOR + 1, z, CORNFL);
                    else if (q < 0.9) set(x, FLOOR + 1, z, DANDEL);
                }
            }
        }
        // petite herbe, fougeres et fleurs sur tout le plateau
        for (int z = 20; z < D - 20; z++) for (int x = 20; x < W - 20; x++) {
            int t = th[z * W + x];
            if (get(x, t, z) != GRASS || get(x, t + 1, z) != AIR) continue;
            double q = rnd(x, 9, z);
            if (q < 0.10) set(x, t + 1, z, SHORTG);
            else if (q < 0.125) set(x, t + 1, z, FERN);
            else if (q < 0.135) set(x, t + 1, z, ((x * 7 + z) & 3) == 0 ? POPPY : ((x + z) & 1) == 0 ? BLUET : DANDEL);
        }
        // lanternes sur poteaux le long des allees
        for (int x = 80; x <= 260; x += 12) lamp(x, 178);
        for (int z = 190; z <= 250; z += 12) lamp(178, z);
        for (int x = 190; x <= 250; x += 12) lamp(x, 172);
    }

    private void lamp(int x, int z) {
        if (!area(x, z) || th[z * W + x] != FLOOR || get(x, FLOOR + 1, z) != AIR) return;
        for (int h = 1; h <= 3; h++) set(x, FLOOR + h, z, WWALL);
        set(x, FLOOR + 4, z, LANTERN);
    }

    // ------------------------------------------------------------------ arbres : automne dore, orange et rouille, quelques verts
    private int leafFor(int x, int y, int z, int main) {
        double q = rnd(x, y, z);
        if (q < 0.16) return LEAF_G;
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
        int h = 36;
        for (int y = 0; y < h; y++) {
            double t = y / (double) h;
            double rad = y < 4 ? 3.4 : t < 0.5 ? 2.6 : t < 0.75 ? 1.9 : 1.2;
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) if (Math.hypot(dx, dz) <= rad) set(x + dx, y0 + y, z + dz, GLOG_Y);
        }
        for (int i = 0; i < 7; i++) {                                           // racines
            double a = i * Math.PI * 2 / 7 + r.nextDouble() * 0.4;
            double px = x, pz = z; int py = y0 + 2;
            for (int k = 0; k < 6; k++) { px += Math.cos(a); pz += Math.sin(a); if (k > 1 && r.nextBoolean()) py--; set((int) Math.round(px + Math.cos(a) * 2.5), Math.max(y0, py), (int) Math.round(pz + Math.sin(a) * 2.5), GLOG_Y); }
        }
        List<double[]> tips = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 9 + r.nextDouble() * 0.6;
            double bx = x, bz = z, by = y0 + h * (0.4 + 0.4 * r.nextDouble());
            int len = 8 + r.nextInt(7);
            for (int k = 0; k < len; k++) {
                bx += Math.cos(a); bz += Math.sin(a); by += 0.5; a += (r.nextDouble() - 0.5) * 0.3;
                set((int) Math.round(bx), (int) Math.round(by), (int) Math.round(bz), Math.abs(Math.cos(a)) > 0.7 ? GLOG_X : GLOG_Z);
                if (k > 2) set((int) Math.round(bx), (int) Math.round(by) - 1, (int) Math.round(bz), GLOG_Y);
            }
            tips.add(new double[]{bx, by, bz, 6.5 + r.nextDouble() * 2.5});
        }
        tips.add(new double[]{x, y0 + h + 3, z, 9.5});
        for (double[] t : tips) blob(t[0], t[1], t[2], t[3], 0.62, i2main(t));
    }

    private int i2main(double[] t) { return ((int) (t[0] * 3 + t[2]) & 3) < 2 ? LEAF_Y : LEAF_O; }

    private boolean clear(int x, int z, int r) {
        for (int dx = -r; dx <= r; dx += 2) for (int dz = -r; dz <= r; dz += 2) {
            int xx = x + dx, zz = z + dz;
            if (!area(xx, zz)) return false;
            int t = th[zz * W + xx];
            int id = get(xx, t, zz);
            if (id != GRASS && id != PODZOL && id != COARSE && id != MOSS) return false;
            if (get(xx, t + 1, zz) != AIR && get(xx, t + 1, zz) != SHORTG && get(xx, t + 1, zz) != FERN) return false;
        }
        return true;
    }

    private void trees() {
        // arbres d'automne sur les pentes de l'ouest, le plateau et la terrasse de l'est ; epicea sur les hauteurs
        for (int i = 0; i < 2600; i++) {
            int x = BAND + 2 + (int) (rnd(i, 1, 2) * (W - 2 * BAND - 4)), z = BAND + 2 + (int) (rnd(i, 3, 4) * (D - 2 * BAND - 4));
            int t = th[z * W + x];
            if (wl[z * W + x] > -999) continue;
            if (t >= 62) {
                if (rnd(i, 5, 6) < 0.5 && clear(x, z, 2) && get(x, t, z) == GRASS && t < 108) conifer(x, z, 9 + (int) (rnd(i, 7, 8) * 8));
                continue;
            }
            if (t < FLOOR - 4) continue;
            boolean inHouse = x > 84 && x < 280 && z > 100 && z < 262;
            if (inHouse && rnd(i, 9, 10) < 0.82) continue;                                   // le plateau reste degage, quelques arbres seulement
            if (!clear(x, z, 3)) continue;
            double q = rnd(i, 11, 12);
            int main = q < 0.4 ? LEAF_O : q < 0.75 ? LEAF_Y : q < 0.9 ? LEAF_R : LEAF_G;
            tree(x, z, 9 + (int) (rnd(i, 13, 14) * 7), 3.6 + rnd(i, 15, 16) * 2.2, main, i * 31L + 7);
        }
    }
}
