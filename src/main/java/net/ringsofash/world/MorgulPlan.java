package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan de Minas Morgul : une grille de blocs abstraits (identifiants de palette), sans aucune dependance a Minecraft.
 * Repere local : x et z de 0 a 149, centre du chateau en (75, 75), la porte est du cote z = 0 (vers l'ouest dans le monde),
 * y = 0 est le premier niveau d'air au-dessus du sol (le sol plein s'arrete a y = -1), y negatif = sous le sol.
 * Forme : une etoile a six pointes (muraille exterieure), une seconde etoile plus petite et plus haute, une grande tour
 * centrale couronnee de trois lames, et un gouffre qui entoure le tout, franchi par un viaduc.
 * {@link Buildings} traduit les identifiants en blocs ; le plan peut aussi etre dessine hors du jeu pour verification.
 */
public final class MorgulPlan {
    public static final int W = 150, D = 150, OFF = 48, H = 200;        // y local de -OFF a H - OFF - 1
    public static final int CX = 75, CZ = 75;                           // centre : axe de la tour
    public static final int DECK = 95;                                  // plateforme du sommet de la tour (sol plein a y = 95)
    public static final int DEPTH = 44, CHW = 14, LEDGE = 2, WATER_TOP = -DEPTH + 10;            // gouffre : profondeur, largeur, corniche au pied de la muraille
    public static final double R1 = 55, R2 = 36, SHARP = 0.52;          // rayons des pointes ; creux = SHARP * pointe

    public static final byte AIR = 0, STONE = 1, DARK = 2, DETAIL = 3, PALE = 4, VEIN = 5, HOT = 6, GLOW = 7, GLASS = 8, BARS = 9,
            SLAB_S = 10, SLAB_S_TOP = 11, SLAB_P = 12, SLAB_P_TOP = 13, SLAB_D = 14, SLAB_D_TOP = 15, WALL_S = 16, WALL_D = 17,
            BLACKSTONE = 18, STAIR_S = 20, STAIR_P = 28, STAIR_D = 36;   // STAIR_x + dir * 2 + (haut ? 1 : 0)
    public static final int N = 0, E = 1, S = 2, W_ = 3;                // directions : N = -z, E = +x, S = +z, W = -x

    private final byte[] g = new byte[W * H * D];
    private final int[] top = new int[W * D];
    private final byte[] chasm = new byte[W * D];
    private final boolean[] flat = new boolean[W * D];
    private final double[] sdO = new double[W * D], sdI = new double[W * D];
    private double[] ovx, ovz, ivx, ivz;
    private double[][] tipsO, tipsI;
    /** coffres : x, y, z, direction (face du coffre) */
    public final List<int[]> chests = new ArrayList<>();

    private static MorgulPlan inst;
    public static synchronized MorgulPlan get() { if (inst == null) inst = new MorgulPlan(); return inst; }

    public MorgulPlan() {
        build();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    /** grille brute (pour les apercus hors jeu) */
    public byte[] raw() { return g; }
    /** profondeur du gouffre sous le sol dans cette colonne (0 = pas de gouffre) */
    public int chasm(int x, int z) { return x < 0 || x >= W || z < 0 || z >= D ? 0 : chasm[z * W + x]; }
    /** colonne dont le relief naturel est rase au niveau du chateau (cours, corniche, chemin d'acces) */
    public boolean flat(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D && flat[z * W + x]; }
    private void markFlat(int x0, int z0, int x1, int z1) {
        for (int x = Math.max(0, x0); x <= Math.min(W - 1, x1); x++) for (int z = Math.max(0, z0); z <= Math.min(D - 1, z1); z++) flat[z * W + x] = true;
    }
    /** bruit lisse dans [-1, 1] */
    private static double vn(double x, double z, int seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = rnd(x0, seed, z0), b = rnd(x0 + 1, seed, z0), c = rnd(x0, seed, z0 + 1), d = rnd(x0 + 1, seed, z0 + 1);
        return ((a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz) * 2 - 1;
    }
    private static double noise(double x, double z) { return 0.65 * vn(x / 9.0, z / 9.0, 11) + 0.35 * vn(x / 4.0, z / 4.0, 23); }

    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public byte get(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    /** plus haut bloc non vide de la colonne (-OFF - 1 s'il n'y en a pas) */
    public int top(int x, int z) { return top[z * W + x]; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    private boolean solid(int x, int y, int z) { return get(x, y, z) != AIR; }

    // ------------------------------------------------------------------ outils
    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 362437;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
    private static double rnd(int x, int y, int z) { return ((hash(x, y, z) >>> 5) & 1023) / 1024.0; }
    /** pierre de base : surtout la teinte principale, quelques blocs plus sombres */
    private byte mat(int x, int y, int z) { return rnd(x, y, z) < 0.09 ? DARK : STONE; }
    private void box(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, id);
    }
    private void air(int x0, int y0, int z0, int x1, int y1, int z1) { box(x0, y0, z0, x1, y1, z1, AIR); }
    private static int stair(int base, int dir, boolean topHalf) { return base + dir * 2 + (topHalf ? 1 : 0); }
    /** demi-largeur d'une arche en ogive a la hauteur y (ressaut a spring, pointe a apex) */
    private static int arch(int y, int spring, int apex, int hw) {
        if (y <= spring) return hw;
        if (y > apex) return -1;
        double t = (y - spring) / (double) (apex - spring + 1);
        return (int) Math.round(hw * (1 - Math.pow(t, 1.8)));
    }
    private static double sdPoly(double px, double pz, double[] vx, double[] vz) {
        int n = vx.length;
        double best = 1e9;
        boolean in = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double ax = vx[j], az = vz[j], bx = vx[i], bz = vz[i], dx = bx - ax, dz = bz - az;
            double t = Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz)));
            double qx = ax + t * dx - px, qz = az + t * dz - pz;
            best = Math.min(best, Math.sqrt(qx * qx + qz * qz));
            if ((az > pz) != (bz > pz) && px < (bx - ax) * (pz - az) / (bz - az) + ax) in = !in;
        }
        return in ? -best : best;
    }
    private static double dist(double ax, double az, double bx, double bz) { return Math.sqrt((ax - bx) * (ax - bx) + (az - bz) * (az - bz)); }

    // ------------------------------------------------------------------ assemblage
    private void build() {
        // etoiles : le creux a 0 degre est du cote de la porte (z = 0), les pointes sont a 30, 90, 150... degres
        ovx = new double[12]; ovz = new double[12]; ivx = new double[12]; ivz = new double[12];
        tipsO = new double[6][]; tipsI = new double[6][];
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(30 * k);
            boolean tip = (k & 1) == 1;
            double ro = tip ? R1 : R1 * SHARP, ri = tip ? R2 : R2 * SHARP;
            ovx[k] = CX + ro * Math.sin(a); ovz[k] = CZ - ro * Math.cos(a);
            ivx[k] = CX + ri * Math.sin(a); ivz[k] = CZ - ri * Math.cos(a);
            if (tip) { tipsO[k / 2] = new double[]{ovx[k], ovz[k]}; tipsI[k / 2] = new double[]{ivx[k], ivz[k]}; }
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            sdO[z * W + x] = sdPoly(x + 0.5, z + 0.5, ovx, ovz);
            sdI[z * W + x] = sdPoly(x + 0.5, z + 0.5, ivx, ivz);
            double s = sdO[z * W + x];
            int dep = 0;
            double sn = s + 2.4 * noise(x, z);                              // bords irreguliers
            if (s > 1.2 && sn > LEDGE && sn < LEDGE + CHW) {
                dep = (int) Math.round(Math.min(DEPTH, Math.min((sn - LEDGE) * 14, (LEDGE + CHW - sn) * 10)));
                dep -= (int) Math.round((vn(x / 3.0, z / 3.0, 31) + 1) * 1.2);   // parois et fond un peu accidentes
            }
            chasm[z * W + x] = (byte) Math.max(0, dep);
            if (s <= LEDGE + 0.5 && dep == 0) flat[z * W + x] = true;
        }
        ground();
        wall(sdO, 5, 15, tipsO, 24, 0.5, 20, true);
        wall(sdI, 4, 30, tipsI, 22, 0.62, 17, false);
        pinnacles(tipsO, R1, 15, 24, 0.5, 18);
        pinnacles(tipsI, R2, 30, 22, 0.62, 18);
        wardDecor();
        tower();
        shaft();
        crown();
        gates();
        bridge();
    }

    /** sol plein sous la muraille exterieure et les cours, avec un motif en etoile en pierre pale */
    private void ground() {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            double so = sdO[z * W + x], si = sdI[z * W + x];
            if (so > 0.4) continue;
            for (int y = -3; y <= -1; y++) set(x, y, z, mat(x, y, z));
            boolean inlay1 = Math.abs(so + 5 + 3.2) < 0.7 && si > 4.5;
            boolean inlay2 = Math.abs(si - 3.4) < 0.6 && so < -6;
            boolean path = Math.abs(x + 0.5 - CX) < 2.6 && z < CZ - 11 && z > 36;
            if (inlay1 || inlay2 || path) set(x, -1, z, PALE);
            else if (((x >> 1) + (z >> 1) & 1) == 0 && rnd(x, 1, z) < 0.25) set(x, -1, z, DARK);
        }
    }

    /** muraille en etoile : bande de largeur t le long du contour interieur, chemin de ronde, creneaux, pointes qui montent */
    private void wall(double[] sd, double t, int hb, double[][] tips, double reach, double slope, int glowH, boolean outer) {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            double s = sd[z * W + x];
            if (s > 0.0 || s < -t) continue;
            double di = -s, cx = x + 0.5, cz = z + 0.5;
            double dt = 1e9;
            for (double[] tp : tips) dt = Math.min(dt, dist(cx, cz, tp[0], tp[1]));
            int boost = dt < reach ? (int) Math.round((reach - dt) * slope) : 0;
            int tp = hb + boost;
            boolean face = di < 1.6 || di > t - 1.6;
            for (int y = -3; y <= tp; y++) {
                byte id = STONE;
                double r = rnd(x, y, z);
                if (y == tp) id = PALE;
                else if (y == tp - 1 && face) id = ((x + z) & 3) < 2 ? PALE : STONE;
                else {
                    if (r < 0.08) id = DARK;
                    if (dt < 7) id = DARK;
                    if (face && y >= 0) {
                        double gr = Math.max(0, 1 - y / (double) glowH);
                        double pg = 0.62 * gr + 0.03;
                        if (rnd(x, y + 7, z) < pg) id = rnd(x, y + 3, z) < gr * 0.9 ? HOT : VEIN;
                        if (y == 8) id = DETAIL;
                    }
                }
                set(x, y, z, id);
            }
            if (boost == 0 && di < 1.2) {                               // parapet et creneaux
                set(x, tp + 1, z, STONE);
                if (((x >> 1) + (z >> 1) & 1) == 0) { set(x, tp + 2, z, STONE); set(x, tp + 3, z, SLAB_P); }
            }
        }
    }

    private void pinnacles(double[][] tips, double rr, int hb, double reach, double slope, int len) {
        for (double[] tp : tips) {
            double fx = (tp[0] - CX) / rr, fz = (tp[1] - CZ) / rr;
            double px = CX + fx * (rr - 3.2), pz = CZ + fz * (rr - 3.2);
            int base = hb + (int) Math.round((reach - 3) * slope);
            for (int h = 1; h <= len + 2; h++) {
                double r = 3.0 * (1 - h / (double) (len + 2)) + 0.3;
                int ri = (int) Math.ceil(r) + 1;
                for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                    int xx = (int) Math.floor(px) + dx, zz = (int) Math.floor(pz) + dz;
                    double d = dist(xx + 0.5, zz + 0.5, px, pz);
                    if (d > r) continue;
                    byte id = DARK;
                    if (d < 0.9 && h % 4 == 0) id = HOT;
                    else if (d > r - 0.9 && h % 5 == 0) id = DETAIL;
                    set(xx, base + h, zz, id);
                }
            }
        }
    }

    /** lampes dans les cours */
    private void wardDecor() {
        for (int j = 1; j < 6; j++) {
            for (double rad : new double[]{23.5, 46.0}) {
                double a = Math.toRadians(60 * j + (rad > 30 ? 30 : 0));
                int x = (int) Math.round(CX + rad * Math.sin(a)), z = (int) Math.round(CZ - rad * Math.cos(a));
                if (sdO[z * W + x] > -8 || sdI[z * W + x] < 3) continue;
                box(x, 0, z, x, 4, z, DARK);
                set(x, 5, z, GLOW);
                set(x, 6, z, SLAB_P);
                set(x, 0, z, DETAIL);
            }
        }
    }

    private static double towerR(int y) { return 8.5 + 3.0 * Math.pow(Math.max(0, 1 - y / 20.0), 2); }

    private void tower() {
        for (int y = 0; y <= DECK - 1; y++) {
            double r = towerR(y);
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > r * r + 0.25) continue;
                boolean shell = d2 > (r - 1.8) * (r - 1.8);
                byte id = mat(CX + dx, y, CZ + dz);
                if (shell) {
                    double a = Math.atan2(dz, dx);
                    double sct = (a + Math.PI) / (2 * Math.PI) * 16;
                    boolean rib = sct - Math.floor(sct) < 0.3;
                    double gr = Math.max(0, 1 - y / 46.0);
                    if (rib) id = DETAIL;
                    else if (rnd(CX + dx, y + 5, CZ + dz) < 0.55 * gr + 0.02) id = rnd(CX + dx, y, CZ + dz) < gr ? HOT : VEIN;
                    if (y % 16 == 15 || y == DECK - 1) id = PALE;
                    if (y <= 2) id = DARK;
                }
                // fenetres : fentes vertes lumineuses sur les quatre faces
                for (int wy : new int[]{22, 46, 70, 87}) {
                    if (y < wy - 1 || y > wy + 6) continue;
                    int lat = Math.min(Math.abs(dx), Math.abs(dz)), lon = Math.max(Math.abs(dx), Math.abs(dz));
                    if (lon < 5) continue;
                    if (y >= wy && y <= wy + 5 && lat <= 1 && d2 > (r - 3.4) * (r - 3.4)) id = lat == 0 || y < wy + 4 ? GLOW : DETAIL;
                    else if (lat <= 2 && (y == wy - 1 || y == wy + 6) && shell) id = DETAIL;
                    else if (lat == 2 && y >= wy && y <= wy + 5 && shell) id = DETAIL;
                }
                set(CX + dx, y, CZ + dz, id);
            }
        }
        // plateforme du sommet, en encorbellement
        for (int dx = -13; dx <= 13; dx++) for (int dz = -13; dz <= 13; dz++) {
            double d2 = dx * dx + dz * dz;
            if (d2 <= 144) set(CX + dx, DECK, CZ + dz, d2 > 110 ? PALE : (d2 > 70 ? STONE : mat(CX + dx, DECK, CZ + dz)));
            for (int k = 1; k <= 6; k++) {
                double rc = 12 - k * 0.55;
                if (d2 <= rc * rc && !solid(CX + dx, DECK - k, CZ + dz)) set(CX + dx, DECK - k, CZ + dz, k % 2 == 0 ? DARK : DETAIL);
            }
        }
    }

    /** cage d'escalier 9x9, pilier central 5x5, escalier en colimacon carre de 96 marches */
    private void shaft() {
        air(CX - 4, 0, CZ - 4, CX + 4, DECK - 1, CZ + 4);
        for (int y = 0; y <= DECK - 1; y++)
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(CX + dx, y, CZ + dz, mat(CX + dx, y, CZ + dz));
        for (int y = 8; y <= DECK - 7; y += 8) {
            set(CX - 2, y, CZ, GLOW); set(CX + 2, y, CZ, GLOW); set(CX, y, CZ - 2, GLOW); set(CX, y, CZ + 2, GLOW);
        }
        List<int[]> ring = new ArrayList<>();
        for (int x = -3; x <= 3; x++) ring.add(new int[]{x, -3});
        for (int z = -2; z <= 3; z++) ring.add(new int[]{3, z});
        for (int x = 2; x >= -3; x--) ring.add(new int[]{x, 3});
        for (int z = 2; z >= -2; z--) ring.add(new int[]{-3, z});
        int start = 0;
        for (int i = 0; i < ring.size(); i++) if (ring.get(i)[0] == 2 && ring.get(i)[1] == -3) start = i;
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < 24; i++) cells.add(ring.get((start + i) % 24));
        int[] dirs = new int[24];
        for (int i = 0; i < 24; i++) {
            int[] a = cells.get(i), b = cells.get((i + 1) % 24);
            int ddx = b[0] - a[0], ddz = b[1] - a[1];
            dirs[i] = ddx > 0 ? E : ddx < 0 ? W_ : ddz > 0 ? S : N;
        }
        for (int k = 0; k < 96; k++) {
            int i = k % 24, y = k;
            int[] c = cells.get(i);
            int d = dirs[i];
            set(CX + c[0], y, CZ + c[1], stair(STAIR_S, d, false));
            for (int[] e : outward(c)) set(CX + e[0], y, CZ + e[1], stair(STAIR_S, d, false));
            if (k >= 93) {                                                  // percer le plancher du sommet au-dessus des dernieres marches
                set(CX + c[0], DECK, CZ + c[1], k == 95 ? stair(STAIR_S, d, false) : AIR);
                for (int[] e : outward(c)) set(CX + e[0], DECK, CZ + e[1], k == 95 ? stair(STAIR_S, d, false) : AIR);
            }
        }
        for (int k = 93; k < 96; k++) {
            int[] c = cells.get(k % 24);
            for (int y = DECK + 1; y <= DECK + 4; y++) { set(CX + c[0], y, CZ + c[1], AIR); for (int[] e : outward(c)) set(CX + e[0], y, CZ + e[1], AIR); }
        }
        // entree : tunnel en ogive du cote de la porte, jusqu'a la cage
        for (int y = 0; y <= 9; y++) {
            int hw = arch(y, 3, 8, 2);
            if (hw < 0) break;
            for (int x = CX - hw; x <= CX + hw; x++) for (int z = CZ - 14; z <= CZ - 5; z++) set(x, y, z, AIR);
        }
        for (int y = 0; y <= 8; y++) {                                      // chambranle de l'entree
            int hw = arch(y, 3, 8, 2);
            if (hw < 0) break;
            for (int z = CZ - 13; z <= CZ - 9; z++) {
                if (z < CZ - 11 && hw >= 0) { set(CX - hw - 1, y, z, DETAIL); set(CX + hw + 1, y, z, DETAIL); }
            }
        }
        chests.add(new int[]{CX - 4, DECK + 1, CZ - 8, S});
        chests.add(new int[]{CX + 4, DECK + 1, CZ - 8, S});
    }

    private static int[][] outward(int[] c) {
        int x = c[0], z = c[1];
        List<int[]> l = new ArrayList<>();
        if (z == -3) l.add(new int[]{x, -4});
        if (z == 3) l.add(new int[]{x, 4});
        if (x == -3) l.add(new int[]{-4, z});
        if (x == 3) l.add(new int[]{4, z});
        if (Math.abs(x) == 3 && Math.abs(z) == 3) l.add(new int[]{x * 4 / 3, z * 4 / 3});
        return l.toArray(new int[0][]);
    }

    /** couronne : couronne de creneaux, pointe centrale et deux lames qui s'ecartent vers l'exterieur */
    private void crown() {
        // couronne de creneaux autour de la plateforme
        for (int dx = -13; dx <= 13; dx++) for (int dz = -13; dz <= 13; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 10.2 || d > 12.3) continue;
            double a = Math.atan2(dz, dx);
            set(CX + dx, DECK + 1, CZ + dz, DARK);
            set(CX + dx, DECK + 2, CZ + dz, d > 11.5 ? DARK : DETAIL);
            if (((int) Math.floor((a + Math.PI) / (2 * Math.PI) * 24) & 1) == 0) { set(CX + dx, DECK + 3, CZ + dz, DARK); set(CX + dx, DECK + 4, CZ + dz, SLAB_D); }
        }
        // pointe centrale : un cone effile avec quatre rainures lumineuses
        int len = 47;
        for (int h = 1; h <= len + 1; h++) {
            double t = (h - 1) / (double) len, r = 6.2 * (1 - t) + 0.35;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > r * r) continue;
                byte id = DARK;
                if ((dx == 0 || dz == 0) && d2 > (r - 1.3) * (r - 1.3) && h > 4 && h < len - 2) id = GLOW;
                else if (h % 8 == 0) id = DETAIL;
                set(CX + dx, DECK + h, CZ + dz, id);
            }
        }
        // deux lames laterales, qui partent de la couronne et s'ecartent vers l'exterieur
        int plen = 41;
        for (int s = -1; s <= 1; s += 2) {
            for (int h = 1; h <= plen + 1; h++) {
                double t = (h - 1) / (double) plen;
                double cxp = s * (7 + 10.5 * Math.pow(t, 1.25));
                double wx = 3.4 * (1 - t) + 0.55, wz = 4.2 * (1 - t) + 0.6;
                int y = DECK + 1 + h;
                for (int xx = (int) Math.floor(cxp - wx) - 1; xx <= (int) Math.ceil(cxp + wx) + 1; xx++) for (int dz = -5; dz <= 5; dz++) {
                    double ex = (xx + 0.5 - cxp) / wx, ez = (dz + 0.5) / wz;
                    if (ex * ex + ez * ez > 1) continue;
                    byte id = DARK;
                    if (Math.abs(ez) < 0.25 && ex * ex < 0.35 && h > 5 && h < plen - 3) id = GLOW;
                    else if (h % 8 == 0) id = DETAIL;
                    set(CX + xx, y, CZ + dz, id);
                }
            }
        }
        // couloir sous la pointe, de la sortie de l'escalier jusqu'au bord libre de la plateforme
        for (int y = DECK + 1; y <= DECK + 4; y++) for (int x = CX - 1; x <= CX + 3; x++) for (int z = CZ - 8; z <= CZ - 3; z++) set(x, y, z, AIR);
        for (int y = DECK + 1; y <= DECK + 4; y++) for (int x = CX - 3; x <= CX + 3; x++) for (int z = CZ - 8; z <= CZ - 7; z++) set(x, y, z, AIR);
    }

    /** portes en ogive de la muraille exterieure et de la muraille interieure, alignees sur l'axe */
    private void gates() {
        int vo = (int) Math.floor(CZ - R1 * SHARP), vi = (int) Math.floor(CZ - R2 * SHARP);
        int[][] gt = {{vo - 6, vo + 8, 5, 13, 4}, {vi - 3, CZ - 13, 5, 18, 4}};   // z0, z1, ressaut, sommet, demi-largeur
        for (int[] q : gt) {
            for (int y = 0; y <= q[3]; y++) {
                int hw = arch(y, q[2], q[3], q[4]);
                if (hw < 0) break;
                for (int x = CX - hw; x <= CX + hw; x++) for (int z = q[0]; z <= q[1]; z++) if (z < CZ - 12 || y > 4) set(x, y, z, AIR);
            }
            // chambranle pale et crocs verts
            for (int y = 0; y <= q[3] + 1; y++) {
                int hw = arch(y, q[2], q[3], q[4]);
                if (hw < 0) hw = 0;
                for (int z = q[0]; z <= q[1]; z++) {
                    if (z % 3 != 0) continue;
                    set(CX - hw - 1, y, z, DETAIL); set(CX + hw + 1, y, z, DETAIL);
                }
            }
            int zf = q[0];
            for (int y = 0; y <= q[3]; y++) { int hw = arch(y, q[2], q[3], q[4]); if (hw >= 0) { set(CX - hw, y, zf, DETAIL); set(CX + hw, y, zf, DETAIL); } }
            for (int x = CX - q[4]; x <= CX + q[4]; x += 2) set(x, q[2] + 1 + (Math.abs(x - CX) < 3 ? 2 : 0), zf, GLOW);
        }
    }

    /** viaduc a arches au-dessus du gouffre : il va jusqu'a la terre ferme, puis un chemin pave et deux grandes statues */
    private void bridge() {
        int zB = (int) Math.floor(CZ - R1 * SHARP) + 1;
        int z = zB;
        boolean seen = false;
        for (; z >= 0; z--) {
            boolean any = false;
            for (int x = CX - 6; x <= CX + 6; x++) if (chasm(x, z) > 0) any = true;
            if (any) seen = true; else if (seen) break;
        }
        int zA = Math.max(10, z - 3);
        for (z = zA; z <= zB; z++) {
            int lz = (z - zA) % 7;
            boolean pier = lz <= 1;
            int dz = lz - 4;
            int archTop = -4 - (dz * dz) / 2;
            for (int x = CX - 4; x <= CX + 4; x++) {
                int dep = Math.max(chasm(x, z), 6);
                for (int y = -dep; y <= -1; y++) {
                    if (!pier && y <= archTop) continue;
                    set(x, y, z, y >= -2 ? (Math.abs(x - CX) < 2 ? PALE : STONE) : (rnd(x, y, z) < 0.12 ? DARK : STONE));
                }
            }
            for (int sx = -1; sx <= 1 && z <= zB - 6; sx += 2) {
                int x = CX + sx * 4;
                set(x, 0, z, WALL_S);
                if (z % 4 == 0) { set(x, 0, z, DARK); set(x, 1, z, DARK); set(x, 2, z, DARK); set(x, 3, z, GLOW); set(x, 4, z, SLAB_P); }
            }
        }
        // chemin d'acces sur la terre ferme (relief rase autour)
        for (z = zA - 1; z >= 4; z--) {
            for (int x = CX - 4; x <= CX + 4; x++) {
                for (int y = -3; y <= -1; y++) set(x, y, z, y == -1 && Math.abs(x - CX) < 2 ? PALE : STONE);
            }
            if (z % 4 == 0) for (int sx = -1; sx <= 1; sx += 2) { int x = CX + sx * 4; set(x, 0, z, DARK); set(x, 1, z, DARK); set(x, 2, z, GLOW); set(x, 3, z, SLAB_P); }
        }
        markFlat(CX - 7, 4, CX + 7, zA);
        // deux grandes statues devant le pont, sur la terre ferme
        int zc = zA - 2;
        for (; zc > 12; zc--) {
            boolean free = true;
            for (int x = CX - 15; x <= CX + 15 && free; x++) for (int zz = zc - 6; zz <= zc + 6; zz++) if (chasm(x, zz) > 0) { free = false; break; }
            if (free) break;
        }
        statue(CX - 9, zc);
        statue(CX + 9, zc);
        markFlat(CX - 16, zc - 7, CX + 16, zc + 7);
    }

    /** grande gargouille assise sur un piedestal pale, dans les couleurs du chateau, qui regarde vers l'exterieur (z negatif) */
    private void statue(int cx, int cz) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int y = -3; y <= -1; y++) set(cx + dx, y, cz + dz, STONE);
        box(cx - 2, 0, cz - 2, cx + 2, 0, cz + 2, DARK);
        box(cx - 2, 1, cz - 2, cx + 2, 2, cz + 2, PALE);
        for (int sx = -2; sx <= 2; sx += 4) for (int sz = -2; sz <= 2; sz += 4) { set(cx + sx, 1, cz + sz, DETAIL); set(cx + sx, 2, cz + sz, DETAIL); }
        box(cx - 2, 3, cz - 2, cx + 2, 3, cz + 2, STONE);
        int b = 4;
        box(cx - 2, b, cz - 1, cx - 1, b + 1, cz + 1, STONE);                    // cuisses
        box(cx + 1, b, cz - 1, cx + 2, b + 1, cz + 1, STONE);
        for (int sx : new int[]{-2, -1, 1, 2}) set(cx + sx, b, cz - 2, DARK);    // griffes
        box(cx - 1, b, cz, cx + 1, b + 6, cz + 1, DARK);                         // torse
        for (int y = b + 2; y <= b + 5; y++) for (int dx = -1; dx <= 1; dx++) set(cx + dx, y, cz - 1, y % 2 == 0 ? DETAIL : DARK);   // cotes
        box(cx - 2, b + 2, cz - 1, cx - 2, b + 4, cz, STONE);                    // bras sur les genoux
        box(cx + 2, b + 2, cz - 1, cx + 2, b + 4, cz, STONE);
        set(cx - 2, b + 1, cz - 2, DARK); set(cx + 2, b + 1, cz - 2, DARK);
        box(cx - 1, b + 7, cz - 1, cx + 1, b + 8, cz, DARK);                     // tete
        set(cx - 1, b + 8, cz - 2, GLOW); set(cx + 1, b + 8, cz - 2, GLOW);      // yeux verts
        set(cx, b + 7, cz - 2, DARK); set(cx, b + 6, cz - 2, DARK);              // museau
        for (int sx = -1; sx <= 1; sx += 2) {                                    // cornes recourbees
            set(cx + sx * 2, b + 9, cz, DARK); set(cx + sx * 2, b + 10, cz, DARK); set(cx + sx * 3, b + 11, cz, DARK); set(cx + sx * 3, b + 12, cz - 1, DARK);
        }
        int[] hw = {b + 13, b + 9, b + 12, b + 8, b + 11, b + 7};                // crete en ailes de chauve-souris, a pointes
        for (int dx = -5; dx <= 5; dx++) {
            int h = hw[Math.abs(dx)];
            for (int y = b + 3; y <= h; y++) set(cx + dx, y, cz + 2, y == h && (dx & 1) == 0 ? GLOW : (y + dx) % 4 == 0 ? DETAIL : DARK);
            if (Math.abs(dx) <= 3) for (int y = b + 6; y <= h - 2; y++) set(cx + dx, y, cz + 1, DARK);
        }
    }
}
