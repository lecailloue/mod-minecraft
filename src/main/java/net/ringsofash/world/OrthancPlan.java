package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan d'Orthanc : tour de 30 x 30 a la base, haute d'environ 200 et qui s'affine vers le sommet, au centre d'un cercle de 200 blocs de diametre ferme par une enceinte
 * a moitie detruite ; entre les deux, une zone de guerre (cratères, puits a echafaudages, gravats, chemins).
 * Grille de blocs abstraits (identifiants de palette), sans dependance a Minecraft ; {@link Buildings} la traduit en blocs.
 * Repere local : x, z de 0 a 199, axe de la tour en (100, 100) (la tour occupe x, z de 85 a 114), y = 0 est le premier niveau d'air
 * au-dessus du sol, y negatif = sous le sol. La porte de l'enceinte et celle de la tour regardent vers +x.
 */
public final class OrthancPlan {
    public static final int TOPY = 182;                                   // y de la dalle du sommet ; la surface est a TOPY + 2, les cretes montent a TOPY + 59
    public static final int COL0 = 118;
    public static final int CREST_H = 52;                               // hauteur de la partie haute des cretes au-dessus de la surface                                  // base de la collerette de pointes
    public static final int W = 200, D = 200, OFF = 24, H = OFF + TOPY + 60;       // y local de -OFF a H - OFF - 1
    public static final int CX = 100, CZ = 100;
    public static final double R_WALL = 100.0, R_LEVEL = 92.5;         // rayon exterieur de l'enceinte, rayon nivele

    public static final byte AIR = 0, PB = 1, BS = 2, DS = 3, CRACK = 4, CHISEL = 5, GILD = 6, BARS = 7, GLASS = 8, LANTERN = 9, CHAIN = 10, SHELF = 11,
            SLAB_PB = 13, SLAB_PB_TOP = 14, SLAB_BS = 15, SLAB_BS_TOP = 16, WALL_PB = 17, WALL_BS = 18,
            STAIR_PB = 20, STAIR_BS = 28, STAIR_SB = 36, STAIR_SP = 44,          // STAIR_x + dir * 2 + (haut ? 1 : 0)
            SB = 52, SB_CRACK = 53, SB_MOSS = 54, COBBLE = 55, ANDESITE = 56, SLAB_SB = 57, SLAB_SB_TOP = 58, WALL_SB = 59, MOSSY_COBBLE = 60,
            GRAVEL = 61, PACKED_MUD = 62, MUD = 63, PATH = 64, TUFF = 65, COBBLED_DS = 68, BASALT = 69,
            LOG_Y = 70, LOG_X = 71, LOG_Z = 72, PLANKS = 73, SLAB_SP = 74, SLAB_SP_TOP = 75, FENCE = 76,
            LADDER_N = 77, LADDER_E = 78, LADDER_S = 79, LADDER_W = 80,
            DLOG_Y = 81, DLOG_X = 82, DLOG_Z = 83, DPLANKS = 84, CAMPFIRE = 85, BARREL = 86, CARPET = 87, TORCH = 88, GOLD = 89, COAL = 90;
    public static final int N = 0, E = 1, S = 2, W_ = 3;                // directions : N = -z, E = +x, S = +z, W = -x

    private final byte[] g = new byte[W * H * D];
    private final int[] top = new int[W * D];
    private final int[] floor = new int[W * D];                          // y absolu du dessus du sol dans les colonnes nivelees
    private final byte[] cover = new byte[W * D];                        // bloc de surface des colonnes nivelees
    private final boolean[] lvl = new boolean[W * D];
    private final float[] pitDepth = new float[W * D];
    /** coffres : x, y, z, direction (face du coffre) */
    public final List<int[]> chests = new ArrayList<>();
    private final List<double[]> pits = new ArrayList<>();               // x, z, rayon, profondeur, puits (1) ou cratere (0)

    private static OrthancPlan inst;
    public static synchronized OrthancPlan get() { if (inst == null) inst = new OrthancPlan(); return inst; }

    public OrthancPlan() {
        terrain();
        roads();
        wall();
        tower();
        warZone();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    public byte[] raw() { return g; }
    public int top(int x, int z) { return top[z * W + x]; }
    public boolean level(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D && lvl[z * W + x]; }
    public int floor(int x, int z) { return floor[z * W + x]; }
    /** cratères et puits : x, z (locaux), rayon, profondeur, puits (1) ou cratère (0) */
    public List<double[]> pits() { return pits; }
    public byte cover(int x, int z) { return cover[z * W + x]; }
    /** colonne a l'interieur du cercle de 200 blocs (enceinte comprise) */
    public static boolean disc(int x, int z) { return Math.hypot(x + 0.5 - CX, z + 0.5 - CZ) < R_WALL; }

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
    private int fl(int x, int z) { return x < 0 || x >= W || z < 0 || z >= D ? -1 : floor[z * W + x]; }

    // ------------------------------------------------------------------ sol de la zone de guerre
    private void terrain() {
        Random rnd = new Random(20251115L);
        // cratères et puits : positions tirees une fois, sans chevaucher la porte, le chemin principal ni la place de la tour
        for (int tries = 0, shafts = 0; tries < 2500 && pits.size() < 70; tries++) {
            boolean shaft = shafts < 10 && pits.size() % 6 == 2;
            double a = rnd.nextDouble() * Math.PI * 2, r = 36 + rnd.nextDouble() * 50;
            double px = CX + r * Math.cos(a), pz = CZ + r * Math.sin(a);
            double rad = shaft ? 4.5 + rnd.nextDouble() : (rnd.nextInt(10) < 6 ? 2.5 + rnd.nextDouble() * 2.5 : 5.5 + rnd.nextDouble() * 4);
            double dep = shaft ? 14 + rnd.nextInt(3) : (rad < 5.5 ? 2 + rnd.nextInt(3) : 4 + rnd.nextInt(4));
            if (Math.abs(pz - CZ) < rad + 5 && px > CX) continue;                // axe de la porte
            boolean clash = false;
            for (double[] q : pits) if (Math.hypot(px - q[0], pz - q[1]) < rad + q[2] + 3) { clash = true; break; }
            if (clash) continue;
            pits.add(new double[]{px, pz, rad, dep, shaft ? 1 : 0});
            if (shaft) shafts++;
        }
        // petits trous d'obus
        for (int i = 0, n = 0; i < 400 && n < 80; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 32 + rnd.nextDouble() * 56;
            double px = CX + r * Math.cos(a), pz = CZ + r * Math.sin(a), rad = 1.4 + rnd.nextDouble() * 1.4;
            if (Math.abs(pz - CZ) < rad + 4 && px > CX) continue;
            boolean clash = false;
            for (double[] q : pits) if (Math.hypot(px - q[0], pz - q[1]) < rad + q[2] + 1) { clash = true; break; }
            if (clash) continue;
            pits.add(new double[]{px, pz, rad, 1 + rnd.nextInt(2), 0});
            n++;
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            double dx = x + 0.5 - CX, dz = z + 0.5 - CZ, r = Math.hypot(dx, dz);
            int i = z * W + x;
            lvl[i] = r < R_LEVEL;
            floor[i] = -1;
            if (!lvl[i]) continue;
            double depth = 0, rim = 0;
            for (double[] q : pits) {
                double d = Math.hypot(x + 0.5 - q[0], z + 0.5 - q[1]);
                double rad = q[2] * (1 + 0.18 * vn(x / 2.5, z / 2.5, 91));          // contour irregulier
                if (d < rad + 2) {
                    double t = d / rad;
                    double dd = q[4] > 0 ? q[3] * (t < 0.92 ? 1 : (1 - t) / 0.08 * 0.9) : q[3] * (1 - t * t);
                    if (t < 1) depth = Math.max(depth, dd);
                    else if (t < 1.4) rim = Math.max(rim, (q[4] > 0 ? 1.0 : 1.0 + q[3] / 5) * (1 - (t - 1) / 0.4));
                }
            }
            if (depth >= 0.6) { floor[i] = -1 - (int) Math.round(depth); pitDepth[i] = (float) depth; }
            else if (rim >= 0.5) floor[i] = -1 + (int) Math.round(rim);
            // surface : place pavee autour de la tour, terre brulee, boue dans les trous
            byte c;
            double n1 = vn(x / 13.0, z / 13.0, 5), n2 = vn(x / 4.0, z / 4.0, 6), h = rnd(x, 3, z);
            if (r < 28) {
                double q = rnd(x, 7, z);
                c = q < 0.40 ? PB : q < 0.68 ? BS : q < 0.84 ? CRACK : q < 0.93 ? DS : GRAVEL;
                if (r > 25 && rnd(x, 8, z) < (r - 25) / 3.0) c = GRAVEL;           // la place s'effrite vers l'exterieur
            } else {
                double v = n1 * 0.7 + n2 * 0.3;
                c = v < -0.3 ? MUD : v < 0.05 ? PACKED_MUD : v < 0.45 ? GRAVEL : TUFF;
                if (h < 0.05) c = BS; else if (h < 0.09) c = COBBLE;
            }
            if (pitDepth[i] > 0) c = pitDepth[i] > 0.5 && rnd(x, 9, z) < 0.7 ? MUD : PACKED_MUD;
            cover[i] = c;
        }
    }

    /** chemins pales : de la porte a la tour, trois chemins rayonnants et un chemin circulaire en partie efface */
    private void roads() {
        double[] angles = {1.75, 3.3, 4.55};
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x;
            if (!lvl[i] || pitDepth[i] > 0) continue;
            double dx = x + 0.5 - CX, dz = z + 0.5 - CZ, r = Math.hypot(dx, dz);
            if (r < 28) continue;
            double wob = 1.2 * vn(x / 9.0, z / 9.0, 31);
            boolean road = dx > 0 && Math.abs(dz + wob) <= 3.6;                       // vers la porte
            for (double a : angles) {
                double along = dx * Math.cos(a) + dz * Math.sin(a), perp = -dx * Math.sin(a) + dz * Math.cos(a);
                if (along > 20 && Math.abs(perp + wob) <= 2.2) road = true;
            }
            if (Math.abs(r - 60 + wob * 1.4) <= 1.6 && vn(x / 17.0, z / 17.0, 33) > -0.25) road = true;
            if (road && rnd(x, 12, z) > 0.07) cover[i] = PATH;
            if (road && floor[i] > -1) floor[i] = -1;
        }
    }

    // ------------------------------------------------------------------ enceinte
    private void wall() {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            double dx = x + 0.5 - CX, dz = z + 0.5 - CZ, r = Math.hypot(dx, dz);
            if (r < 93.0 || r >= R_WALL) continue;
            double th = Math.atan2(dz, dx);
            double s = (r - 93.0) / (R_WALL - 93.0);                                  // 0 = face interieure, 1 = face exterieure
            if (Math.abs(dz) <= 6.5 && dx > 0) { gateRubble(x, z); continue; }          // la porte
            double c = 0.55 * vn(Math.cos(th) * 2.7 + 5, Math.sin(th) * 2.7 + 5, 77) + 0.45 * (0.6 * vn(x / 11.0, z / 11.0, 5) + 0.4 * vn(x / 4.5, z / 4.5, 6));
            int hh;
            if (c > 0.12) hh = 13;
            else {
                double f = smooth((c + 0.5) / 0.62);
                hh = (int) Math.round(13 * Math.pow(f, 1.4)) + (rnd(x, 14, z) < 0.3 ? 1 : 0);
                if (c < -0.4) hh = rnd(x, 15, z) < 0.5 ? 0 : 1 + (int) (rnd(x, 16, z) * 3);       // simple tas de gravats
            }
            // faces rongees
            if (hh >= 3 && rnd(x, 17, z) < 0.10 && (s < 0.25 || s > 0.75)) hh -= 1 + (int) (rnd(x, 18, z) * 3);
            for (int y = -4; y < hh; y++) {
                int id;
                double q = rnd(x, y, z);
                if (hh <= 4 && y >= 0) id = q < 0.4 ? COBBLE : q < 0.6 ? MOSSY_COBBLE : q < 0.8 ? ANDESITE : SB_CRACK;
                else id = q < 0.62 ? SB : q < 0.80 ? SB_CRACK : q < 0.90 ? SB_MOSS : q < 0.96 ? COBBLE : ANDESITE;
                if (hh >= 13) {
                    // chemin de ronde et parapet exterieur crenele
                    if (y == 9 && s < 0.7) id = q < 0.5 ? SB : SB_CRACK;
                    if (y >= 10 && s < 0.72) continue;
                    if (y == 12 && ((x + z) & 1) == 0) continue;
                }
                set(x, y, z, id);
            }
            if (hh >= 1 && hh < 13 && rnd(x, 19, z) < 0.25) set(x, hh, z, rnd(x, 20, z) < 0.5 ? COBBLE : GRAVEL);
        }
        // tours rondes, certaines debout, d'autres a demi ecroulees
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(15 + 30 * k);
            if (Math.abs(Math.sin(a)) < 0.3 && Math.cos(a) > 0) continue;               // pas dans l'axe de la porte
            double tx = CX + 93.5 * Math.cos(a), tz = CZ + 93.5 * Math.sin(a);
            boolean standing = vn(k * 1.7, 4.2, 12) > -0.1;
            int tall = standing ? 21 + (int) (rnd(k, 3, 5) * 4) : 7 + (int) (rnd(k, 4, 5) * 10);
            roundTower(tx, tz, 6.4, tall, standing, k);
        }
        // tours de la porte : l'une debout, l'autre ecroulee
        roundTower(CX + 92.5, CZ - 12.5, 7.2, 27, true, 40);
        roundTower(CX + 92.5, CZ + 12.5, 7.2, 12, false, 41);
    }

    private void gateRubble(int x, int z) {
        double dz = z + 0.5 - CZ, dx = x + 0.5 - CX;
        // portail effondre : quelques gravats dans le passage, l'arche brisee de part et d'autre
        if (Math.abs(dz) > 3.5 && rnd(x, 21, z) < 0.45) {
            int h = 1 + (int) (rnd(x, 22, z) * 3);
            for (int y = 0; y < h; y++) set(x, y, z, rnd(x, y, z) < 0.5 ? COBBLE : SB_CRACK);
        }
        for (int y = -4; y < 0; y++) set(x, y, z, SB);
    }

    private void roundTower(double tx, double tz, double rad, int hgt, boolean standing, int seed) {
        int r0 = (int) Math.ceil(rad) + 1;
        for (int x = (int) tx - r0; x <= (int) tx + r0; x++) for (int z = (int) tz - r0; z <= (int) tz + r0; z++) {
            if (x < 0 || x >= W || z < 0 || z >= D) continue;
            double d = Math.hypot(x + 0.5 - tx, z + 0.5 - tz);
            if (d > rad) continue;
            if (Math.hypot(x + 0.5 - CX, z + 0.5 - CZ) >= R_WALL) continue;
            int h = hgt;
            if (!standing) h = Math.max(2, hgt - (int) Math.round(5 * (vn(x / 2.2, z / 2.2, 40 + seed) + 1)) - (d > rad - 1.5 ? 1 : 0) + (int) (rnd(x, 23, z) * 2));
            boolean shell = d > rad - 2.2;
            for (int y = -4; y < h; y++) {
                if (!shell && y >= 11) {
                    if (y == 11 && standing) set(x, y, z, PLANKS);                       // plancher de la plateforme
                    continue;
                }
                double q = rnd(x, y, z);
                set(x, y, z, q < 0.6 ? SB : q < 0.8 ? SB_CRACK : q < 0.9 ? SB_MOSS : COBBLE);
            }
            if (standing && shell && d > rad - 1.2) {                                       // creneaux
                if (((x + z) & 1) == 0) set(x, hgt, z, SB);
            }
        }
    }

    // ------------------------------------------------------------------ tour
    /** demi-largeur du fut sans collerette ni corniche : 14 au pied, 9 au sommet, affinement continu */
    private static double base(int y) {
        double t = Math.min(1.0, Math.max(0.0, (y - 4.0) / (TOPY - 5.0)));
        return 9.0 + 5.0 * Math.pow(1 - t, 1.15);
    }
    private static boolean collar(int y) { return y >= COL0 && y <= COL0 + 9; }
    private static boolean ledge(int y) { return y >= 11 && y <= TOPY - 3 && y % 12 == 11 && !collar(y); }
    private static double wAt(int y) {
        if (y <= 3) return 15;
        if (y >= TOPY) return base(TOPY - 1) + 2;
        if (y >= COL0 && y <= COL0 + 5) return base(y) + 2.5;
        if (y > COL0 + 5 && y <= COL0 + 9) return base(y) + 1.5;
        return base(y) + (ledge(y) ? 1 : 0);
    }
    /** demi-largeur du volume interieur : murs de 3 d'epaisseur */
    private static int hinAt(int y) { return Math.max(6, (int) Math.round(base(Math.max(4, y))) - 3); }
    private static final int[] FLOORS = {0, 20, 40, 60, 80, 100, 120, 140, 160};

    private void tower() {
        // fondations
        for (int y = -4; y <= -1; y++) for (int x = CX - 15; x < CX + 15; x++) for (int z = CZ - 15; z < CZ + 15; z++)
            set(x, y, z, rnd(x, y, z) < 0.15 ? BS : PB);
        for (int y = 0; y <= TOPY + 1; y++) {
            double hw = wAt(y);
            boolean plinth = y <= 3;
            int cham = plinth || y >= TOPY ? 0 : 2;
            int hin = hinAt(y);
            boolean solidRoof = y >= TOPY;
            for (int x = CX - 17; x < CX + 17; x++) for (int z = CZ - 17; z < CZ + 17; z++) {
                double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, ax = Math.abs(ox), az = Math.abs(oz), m = Math.max(ax, az);
                double dcD = hw - 0.5 - m;
                int dc = (int) Math.round(dcD);
                int dch = cham == 0 ? 99 : (int) Math.floor((2 * (hw - 0.5) - cham) - (ax + az));
                int d = Math.min(dc, dch);
                boolean east = ax >= az && ox > 0;
                int iu = ax >= az ? (int) Math.floor(oz) : (int) Math.floor(ox);          // position le long de la face
                double other = Math.min(ax, az);
                boolean faceMid = other <= hw - 0.5 - cham - 1;
                int id = 0;
                if (d >= 0) {
                    // cellule du corps de la tour
                    boolean cav = !solidRoof && m <= hin - 0.5;
                    if (cav) { set(x, y, z, AIR); continue; }
                    id = skin(x, y, z, d, iu);
                    // fenetres : fente de 1 x 5 traversant le mur, barreaux a l'exterieur
                    if (!plinth && !solidRoof && y >= 13 && y <= TOPY - 6 && !collar(y) && faceMid && d <= 2 && d >= 0 && Math.floorMod(iu, 4) == 2) {
                        int s = y % 12;
                        if (s >= 3 && s <= 7 && !ledge(y)) id = d == 0 ? BARS : AIR;
                    }
                    set(x, y, z, id);
                } else if (d == -1 && !plinth && !solidRoof && faceMid && dc == -1 && Math.floorMod(iu, 4) == 0 && !collar(y) && y <= TOPY - 2) {
                    set(x, y, z, BS);                 // pilastres en relief
                }
            }
        }
        // collerette de pointes : une rangee de dents sur le bord de la corniche
        int ih = (int) Math.round(wAt(COL0 + 9));
        for (int x = CX - 20; x < CX + 20; x++) for (int z = CZ - 20; z < CZ + 20; z++) {
            double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
            if (m != ih - 0.5) continue;
            int iu = Math.abs(ox) >= Math.abs(oz) ? (int) Math.floor(oz) : (int) Math.floor(ox);
            if (Math.floorMod(iu, 2) != 0) continue;
            int len = 3 + (int) (rnd(x, 31, z) * 6);
            for (int y = COL0 + 10; y < COL0 + 10 + len; y++) set(x, y, z, y > COL0 + 8 + len ? SLAB_BS : BS);
        }
        // sommets : plateforme, parapet, chapelle, cretes
        crown();
        interior();
        door();
    }

    private int skin(int x, int y, int z, int d, int iu) {
        if (ledge(y)) return d == 0 ? DS : (rnd(x, y, z) < 0.5 ? DS : PB);
        double q = rnd(x, y, z);
        if (y >= TOPY) return d == 0 ? DS : PB;
        if (collar(y)) return d <= 1 ? BS : PB;
        if (d == 0 && Math.floorMod(iu, 4) == 0) return BS;                       // fut du pilastre
        return q < 0.05 ? CRACK : q < 0.09 ? DS : q < 0.11 ? CHISEL : PB;
    }

    /** plateforme, parapet crenele, chapelle centrale et quatre cretes en croix */
    private void crown() {
        // surface a y = 124 (dalle 122..123 deja posee par la boucle) : parapet sur le bord
        for (int x = CX - 15; x < CX + 15; x++) for (int z = CZ - 15; z < CZ + 15; z++) {
            double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
            double pm = wAt(TOPY) - 0.5;
            if (m > pm || m < pm - 1) continue;
            set(x, TOPY + 2, z, WALL_BS);
            if (((x + z) & 1) == 0) set(x, TOPY + 3, z, WALL_BS);
        }
        // petite chapelle carree au centre de la plateforme (elle coiffe la sortie de l'escalier), toit plat
        int ty = TOPY + 2;
        for (int x = CX - 6; x < CX + 6; x++) for (int z = CZ - 6; z < CZ + 6; z++) {
            double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
            if (m > 5.5) continue;
            boolean ring = m > 4.5;
            boolean corner = Math.abs(ox) > 4.5 && Math.abs(oz) > 4.5;
            for (int y = ty; y <= ty + 9; y++) {
                if (y == ty + 9) { set(x, y, z, corner || ring ? BS : PB); continue; }          // toit
                if (!ring) continue;
                boolean door = Math.min(Math.abs(ox), Math.abs(oz)) < 1.6 && y <= ty + 3 && y >= ty;
                int iuT = Math.abs(ox) >= Math.abs(oz) ? (int) Math.floor(oz) : (int) Math.floor(ox);
                boolean slit = Math.floorMod(iuT, 3) == 0 && !corner && y >= ty + 5 && y <= ty + 7;
                if (door) { set(x, y, z, AIR); continue; }
                set(x, y, z, corner ? DS : slit ? BARS : (rnd(x, y, z) < 0.1 ? CRACK : PB));
            }
        }
        for (int x = CX - 6; x < CX + 6; x++) for (int z = CZ - 6; z < CZ + 6; z++) {                // corniche du toit
            double m = Math.max(Math.abs(x + 0.5 - CX), Math.abs(z + 0.5 - CZ));
            if (m > 5.5 || m < 4.5) continue;
            if (((x + z) & 1) == 0) set(x, ty + 10, z, SLAB_BS);
        }
        // quatre cretes en croix : grandes plaques de 3 d'epaisseur, posees sur chaque face de la plateforme. Chacune est haute et verticale
        // a une extremite, et son bord superieur redescend en courbe concave vers l'autre bout ; le bas remonte du cote haut (disposition en moulinet)
        double pe = wAt(TOPY) + 0.5;                                                                // premiere case hors de la plateforme
        for (int side = 0; side < 4; side++) {
            for (int u = -11; u <= 10; u++) {
                double sp = (u + 11.5) / 22.0;                                                      // 0 : bout haut, 1 : bout bas
                int top = TOPY + 2 + 5 + (int) Math.round((CREST_H - 5) * Math.pow(1 - sp, 2.1));
                int bot = TOPY - 2 + (int) Math.round(15 * Math.pow(1 - sp, 0.9));
                for (int y = bot; y <= top; y++) {
                    int lean = Math.max(0, (int) Math.round(0.09 * (y - TOPY)));
                    for (int t = 0; t < 3; t++) {
                        int mm = (int) Math.round(pe - 0.5) + lean + t;                            // distance a l'axe (cases)
                        int x, z;
                        switch (side) {
                            case 0 -> { x = CX + u; z = CZ - mm - 1; }
                            case 1 -> { x = CX + mm; z = CZ + u; }
                            case 2 -> { x = CX - u - 1; z = CZ + mm; }
                            default -> { x = CX - mm - 1; z = CZ - u - 1; }
                        }
                        boolean edge = y >= top - 1 || y <= bot + 0 || u == -11 || t == 2 && (u + 11) % 6 == 0;
                        double q = rnd(x, y, z);
                        set(x, y, z, edge ? BS : ((u + 11) % 5 == 2 ? DS : q < 0.08 ? CRACK : PB));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ interieur
    private void interior() {
        // noyau : pilier 4 x 4 et escalier en spirale (puits de 8 x 8)
        for (int y = 0; y <= TOPY - 1; y++) for (int dx = -2; dx <= 1; dx++) for (int dz = -2; dz <= 1; dz++)
            set(CX + dx, y, CZ + dz, y % 10 == 9 ? CHISEL : (rnd(CX + dx, y, CZ + dz) < 0.1 ? CRACK : DS));
        // planchers : tout le volume sauf le puits de l'escalier
        for (int fy : FLOORS) {
            if (fy == 0) continue;
            for (int x = CX - 15; x < CX + 15; x++) for (int z = CZ - 15; z < CZ + 15; z++) {
                double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
                int hin = hinAt(fy - 1);
                if (m > hin - 0.5) continue;
                if (x >= CX - 4 && x <= CX + 3 && z >= CZ - 4 && z <= CZ + 3) continue;        // puits
                set(x, fy - 1, z, ((x + z) & 1) == 0 ? PB : BS);
            }
        }
        // escalier : anneau de 20 cases, une marche par case
        List<int[]> ring = new ArrayList<>();
        for (int x = -3; x <= 2; x++) ring.add(new int[]{x, -3});
        for (int z = -2; z <= 2; z++) ring.add(new int[]{2, z});
        for (int x = 1; x >= -3; x--) ring.add(new int[]{x, 2});
        for (int z = 1; z >= -2; z--) ring.add(new int[]{-3, z});
        int n = ring.size();
        int[] dirs = new int[n];
        for (int i = 0; i < n; i++) {
            int[] a = ring.get(i), b = ring.get((i + 1) % n);
            int ddx = b[0] - a[0], ddz = b[1] - a[1];
            dirs[i] = ddx > 0 ? E : ddx < 0 ? W_ : ddz > 0 ? S : N;
        }
        int startIdx = 0;                                        // la premiere marche part au pied de la porte (cote +x), en bas
        for (int i = 0; i < n; i++) if (ring.get(i)[0] == 2 && ring.get(i)[1] == -1) startIdx = i;
        for (int k = 0; k <= TOPY + 1; k++) {
            int i = (startIdx + k) % n;
            int[] c = ring.get(i);
            int dr = dirs[i];
            int blk = stair(STAIR_BS, dr, false);
            set(CX + c[0], k, CZ + c[1], blk);
            for (int[] e : outward(c)) set(CX + e[0], k, CZ + e[1], blk);
            for (int h = 1; h <= 3; h++) {                       // degage la tete (les planchers ont deja le puits, mais pas le toit)
                setAirIfRoof(CX + c[0], k + h, CZ + c[1]);
                for (int[] e : outward(c)) setAirIfRoof(CX + e[0], k + h, CZ + e[1]);
            }
        }
        // decor et mobilier par niveau
        for (int lv = 0; lv < FLOORS.length; lv++) {
            int fy = FLOORS[lv];
            int hin = hinAt(fy);
            int lo = CX - hin, hi = CX + hin - 1, loz = CZ - hin, hiz = CZ + hin - 1;
            // piliers d'angle
            for (int y = fy; y < fy + 19; y++) {
                for (int[] cc : new int[][]{{lo, loz}, {hi - 1, loz}, {lo, hiz - 1}, {hi - 1, hiz - 1}}) {
                    for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) set(cc[0] + a, y, cc[1] + b, y == fy + 18 ? CHISEL : DS);
                }
            }
            // bibliotheques le long des murs (un niveau sur deux), lanternes pendues autour du puits
            if (lv % 2 == 1) {
                for (int x = lo + 3; x <= hi - 3; x += 3) { box(x, fy, loz, x + 1, fy + 1, loz, SHELF); box(x, fy, hiz, x + 1, fy + 1, hiz, SHELF); }
            }
            int[][] lampsAt = {{CX - 6, CZ - 6}, {CX + 5, CZ - 6}, {CX - 6, CZ + 5}, {CX + 5, CZ + 5}};
            for (int[] lp : lampsAt) {
                if (fy < FLOORS[FLOORS.length - 1] && get(lp[0], fy + 18, lp[1]) == AIR) {
                    for (int h = 0; h < 3; h++) set(lp[0], fy + 18 - h, lp[1], CHAIN);
                    set(lp[0], fy + 15, lp[1], LANTERN);
                }
            }
            // un coffre par niveau, contre le mur
            int cxc = lv % 2 == 0 ? lo + 1 : hi - 1, czc = lv % 3 == 0 ? loz + 2 : hiz - 2;
            if (lv >= 1) chests.add(new int[]{cxc, fy, czc, lv % 2 == 0 ? E : W_});
        }
        // salle du trone tout en haut : estrade et siege face a l'escalier
        int fy = FLOORS[FLOORS.length - 1];
        int hinT = hinAt(fy), tx = CX - hinT;
        box(tx, fy, CZ - 3, tx + 1, fy, CZ + 2, BS);
        box(tx, fy + 1, CZ - 2, tx, fy + 4, CZ + 1, DS);
        set(tx + 1, fy + 1, CZ - 1, stair(STAIR_BS, W_, false)); set(tx + 1, fy + 1, CZ, stair(STAIR_BS, W_, false));
        set(tx + 1, fy + 1, CZ - 2, SLAB_BS); set(tx + 1, fy + 1, CZ + 1, SLAB_BS);
        set(tx + 1, fy + 2, CZ - 2, WALL_BS); set(tx + 1, fy + 2, CZ + 1, WALL_BS);
        box(tx + 2, fy, CZ - 1, CX - 5, fy, CZ, CARPET);
        chests.add(new int[]{tx + 1, fy + 1, CZ + 2, E});
        // balustrade autour du puits sur la plateforme : barreaux, sauf la ou l'escalier debouche
        for (int x = CX - 4; x <= CX + 3; x++) for (int z = CZ - 4; z <= CZ + 3; z++) {
            boolean border = x == CX - 4 || x == CX + 3 || z == CZ - 4 || z == CZ + 3;
            boolean pillar = x >= CX - 2 && x <= CX + 1 && z >= CZ - 2 && z <= CZ + 1;
            if (pillar || get(x, TOPY + 1, z) != AIR) continue;
            if (border) set(x, TOPY + 1, z, BARS);
        }
        // les derniers escaliers doivent rester libres : on re-pose la fin de la vis et son degagement
        for (int k = TOPY - 2; k <= TOPY + 1; k++) {
            int i = (startIdx + k) % n;
            int[] c = ring.get(i);
            set(CX + c[0], k, CZ + c[1], stair(STAIR_BS, dirs[i], false));
            for (int[] e : outward(c)) set(CX + e[0], k, CZ + e[1], stair(STAIR_BS, dirs[i], false));
            for (int h = k + 1; h <= k + 3; h++) {
                set(CX + c[0], h, CZ + c[1], AIR);
                for (int[] e : outward(c)) set(CX + e[0], h, CZ + e[1], AIR);
            }
        }
        // le pilier se termine par un brasero sur la plateforme
        box(CX - 2, TOPY, CZ - 2, CX + 1, TOPY + 1, CZ + 1, DS);
        box(CX - 1, TOPY + 2, CZ - 1, CX, TOPY + 2, CZ, BS);
        set(CX - 1, TOPY + 3, CZ - 1, LANTERN); set(CX, TOPY + 3, CZ, LANTERN);
    }

    private void setAirIfRoof(int x, int y, int z) {
        if (y >= TOPY && y <= TOPY + 1) set(x, y, z, AIR);
    }

    private static int[][] outward(int[] c) {
        int x = c[0], z = c[1];
        List<int[]> l = new ArrayList<>();
        if (z == -3) l.add(new int[]{x, -4});
        if (z == 2) l.add(new int[]{x, 3});
        if (x == -3) l.add(new int[]{-4, z});
        if (x == 2) l.add(new int[]{3, z});
        if (x == 2 && z == -3) l.add(new int[]{3, -4});
        if (x == 2 && z == 2) l.add(new int[]{3, 3});
        if (x == -3 && z == 2) l.add(new int[]{-4, 3});
        if (x == -3 && z == -3) l.add(new int[]{-4, -4});
        return l.toArray(new int[0][]);
    }

    /** grande porte en ogive, du cote +x, jusqu'au puits de l'escalier */
    private void door() {
        for (int y = 0; y <= 10; y++) {
            double hwid = y <= 5 ? 2.6 : y <= 7 ? 1.6 : y <= 9 ? 0.6 : -1;
            if (hwid < 0) break;
            for (int x = CX + 5; x <= CX + 16; x++) for (int z = CZ - 4; z <= CZ + 3; z++) {
                if (Math.abs(z + 0.5 - CZ) <= hwid) set(x, y, z, AIR);
            }
        }
        // chambranle
        for (int y = 0; y <= 10; y++) {
            double hwid = y <= 5 ? 2.6 : y <= 7 ? 1.6 : y <= 9 ? 0.6 : -1;
            for (int z = CZ - 5; z <= CZ + 4; z++) {
                double a = Math.abs(z + 0.5 - CZ);
                if (a > hwid && a <= hwid + 1.2 && y <= 10 - 0) for (int x = CX + 13; x <= CX + 16; x++) setIfAir(x, y, z, DS);
            }
        }
        // marches devant la porte (le sol de la place est a -1) et tapis rouge dans le passage
        for (int i = 0; i < 3; i++) for (int z = CZ - 4; z <= CZ + 3; z++) set(CX + 16 + i, -1, z, i == 2 ? PB : DS);
        box(CX + 5, 0, CZ - 2, CX + 16, 0, CZ + 1, CARPET);
    }

    // ------------------------------------------------------------------ zone de guerre : echafaudages, gravats, feux
    private void warZone() {
        Random rnd = new Random(777L);
        // puits : echafaudage dans le trou, depuis le fond jusqu'au dessus du sol
        for (double[] q : pits) {
            if (q[4] > 0) pitScaffold((int) Math.round(q[0]), (int) Math.round(q[1]), (int) q[3]);
        }
        // tours d'echafaudage dans le champ
        int placed = 0;
        for (int tries = 0; tries < 400 && placed < 16; tries++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 33 + rnd.nextDouble() * 54;
            int x = (int) (CX + r * Math.cos(a)), z = (int) (CZ + r * Math.sin(a));
            if (!freeSpot(x, z, 4)) continue;
            scaffoldTower(x, z, 10 + rnd.nextInt(9), rnd.nextInt(3) == 0);
            placed++;
        }
        // deux grands echafaudages adosses a la tour
        scaffoldTower(CX - 21, CZ - 6, 24, false);
        scaffoldTower(CX + 4, CZ + 19, 22, true);
        // tas de gravats, troncs couches, souches brulees, feux de camp
        for (int i = 0, n = 0; i < 300 && n < 40; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 31 + rnd.nextDouble() * 58;
            int x = (int) (CX + r * Math.cos(a)), z = (int) (CZ + r * Math.sin(a));
            if (!freeSpot(x, z, 3)) continue;
            rubble(x, z, 2 + rnd.nextInt(3), 1 + rnd.nextInt(3));
            n++;
        }
        for (int i = 0, n = 0; i < 300 && n < 18; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 31 + rnd.nextDouble() * 58;
            int x = (int) (CX + r * Math.cos(a)), z = (int) (CZ + r * Math.sin(a));
            if (!freeSpot(x, z, 4)) continue;
            boolean alongX = rnd.nextBoolean();
            int len = 4 + rnd.nextInt(4);
            for (int k = 0; k < len; k++) {
                int px = alongX ? x + k : x, pz = alongX ? z : z + k;
                if (fl(px, pz) == -1) setIfAir(px, 0, pz, alongX ? DLOG_X : DLOG_Z);
            }
            n++;
        }
        for (int i = 0, n = 0; i < 300 && n < 26; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 31 + rnd.nextDouble() * 58;
            int x = (int) (CX + r * Math.cos(a)), z = (int) (CZ + r * Math.sin(a));
            if (!freeSpot(x, z, 2)) continue;
            int hgt = 2 + rnd.nextInt(4);
            for (int y = 0; y < hgt; y++) setIfAir(x, y, z, DLOG_Y);
            n++;
        }
        for (int i = 0, n = 0; i < 300 && n < 22; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 31 + rnd.nextDouble() * 58;
            int x = (int) (CX + r * Math.cos(a)), z = (int) (CZ + r * Math.sin(a));
            if (!freeSpot(x, z, 2)) continue;
            setIfAir(x, 0, z, CAMPFIRE);
            setIfAir(x + 1, 0, z, i % 3 == 0 ? BARREL : AIR);
            n++;
        }
    }

    /** emplacement libre : sol plat (non creuse), loin de la porte, de la tour et des autres constructions */
    private boolean freeSpot(int x, int z, int rad) {
        double r = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ);
        if (r < 31 || r > 88) return false;
        for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
            if (!level(x + dx, z + dz)) return false;
            if (fl(x + dx, z + dz) != -1) return false;
            if (get(x + dx, 0, z + dz) != AIR || get(x + dx, 1, z + dz) != AIR) return false;
            if (cover(x + dx, z + dz) == PATH && Math.abs(z + dz + 0.5 - CZ) <= 5 && x + dx > CX) return false;
        }
        return true;
    }

    private void rubble(int x, int z, int rad, int hgt) {
        for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
            double d = Math.hypot(dx, dz);
            if (d > rad + 0.3 * vn(x + dx, z + dz, 50)) continue;
            int h = (int) Math.round(hgt * (1 - d / (rad + 1)) + rnd(x + dx, 3, z + dz) * 0.9);
            for (int y = 0; y < h; y++) {
                double q = rnd(x + dx, y, z + dz);
                setIfAir(x + dx, y, z + dz, q < 0.4 ? COBBLE : q < 0.65 ? ANDESITE : q < 0.85 ? SB_CRACK : GRAVEL);
            }
        }
    }

    /** echafaudage en bois : quatre poteaux, traverses, croix de renfort, plateformes, echelle ; parfois une roue au sommet */
    private void scaffoldTower(int x0, int z0, int hgt, boolean wheel) {
        int w = 4;                                                   // ecart entre poteaux (4 -> empreinte 5 x 5)
        int[][] posts = {{x0, z0}, {x0 + w, z0}, {x0, z0 + w}, {x0 + w, z0 + w}};
        int base = fl(x0 + 2, z0 + 2) + 1;
        for (int[] p : posts) for (int y = base; y < base + hgt; y++) set(p[0], y, p[1], y % 7 == 0 ? DLOG_Y : LOG_Y);
        for (int y = base + 3; y < base + hgt; y += 5) {              // traverses horizontales
            for (int k = 1; k < w; k++) {
                set(x0 + k, y, z0, LOG_X); set(x0 + k, y, z0 + w, LOG_X);
                set(x0, y, z0 + k, LOG_Z); set(x0 + w, y, z0 + k, LOG_Z);
            }
        }
        for (int y = base; y + w < base + hgt; y += w + 1) {          // croix de renfort sur deux faces
            for (int k = 1; k < w; k++) {
                int yy = y + k;
                set(x0 + k, yy, z0, LOG_X);
                set(x0 + w - k, yy, z0 + w, LOG_X);
                set(x0, yy, z0 + k, LOG_Z);
                set(x0 + w, yy, z0 + w - k, LOG_Z);
            }
        }
        for (int y = base + 5; y <= base + hgt; y += 6) {              // plateformes (la case de l'echelle reste libre)
            int yy = Math.min(y, base + hgt - 1);
            for (int a = 1; a < w; a++) for (int b = 1; b < w; b++) if (!(a == 1 && b == 1)) set(x0 + a, yy, z0 + b, a == 2 && b == 2 ? SLAB_SP : PLANKS);
        }
        for (int y = base; y < base + hgt; y++) set(x0 + 1, y, z0 + 1, y == base ? AIR : LADDER_E);
        for (int y = base; y < base + hgt; y++) set(x0, y, z0 + 1, y % 7 == 0 ? DLOG_Y : LOG_Y);
        set(x0 + 1, base, z0 + 1, AIR);
        // couronnement : balustrade
        int ty = base + hgt;
        for (int k = 0; k <= w; k++) { set(x0 + k, ty, z0, FENCE); set(x0 + k, ty, z0 + w, FENCE); set(x0, ty, z0 + k, FENCE); set(x0 + w, ty, z0 + k, FENCE); }
        if (wheel) {                                                  // roue de bois, plan z, appuyee contre l'echafaudage
            int wx = x0 + 2, wy = base + Math.max(7, hgt - 5), wz = z0 + w + 1;
            for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) {
                double d = Math.hypot(a, b);
                if ((d >= 2.4 && d <= 3.4) || (a == 0 && Math.abs(b) <= 3) || (b == 0 && Math.abs(a) <= 3)) set(wx + a, wy + b, wz, d < 0.5 ? DLOG_Z : DPLANKS);
            }
        }
    }

    /** puits profond : cadre de bois du fond au-dessus du sol, plateformes, echelle, treuil et chaine */
    private void pitScaffold(int px, int pz, int depth) {
        int bottom = fl(px, pz);                                       // dessus du fond
        int topY = 3;
        for (int dx = -2; dx <= 2; dx += 4) for (int dz = -2; dz <= 2; dz += 4)
            for (int y = bottom + 1; y <= topY; y++) set(px + dx, y, pz + dz, (y - bottom) % 6 == 0 ? DLOG_Y : LOG_Y);
        for (int y = bottom + 4; y <= topY; y += 4) {
            for (int k = -1; k <= 1; k++) {
                set(px + k, y, pz - 2, LOG_X); set(px + k, y, pz + 2, LOG_X);
                set(px - 2, y, pz + k, LOG_Z); set(px + 2, y, pz + k, LOG_Z);
            }
        }
        for (int y = bottom + 8; y <= topY - 2; y += 6) for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) if (!(a == -1 && b == 0)) set(px + a, y, pz + b, PLANKS);
        for (int y = bottom + 1; y <= topY; y++) { set(px - 2, y, pz, LOG_Y); set(px - 1, y, pz, LADDER_E); }
        // treuil : poutre en saillie au sommet avec chaine et lanterne
        for (int k = -2; k <= 2; k++) set(px + k, topY + 1, pz, LOG_X);
        set(px, topY, pz, CHAIN);
        for (int y = topY - 1; y >= topY - 6 && y > bottom + 2; y--) set(px, y, pz, CHAIN);
        set(px, Math.max(bottom + 3, topY - 7), pz, LANTERN);
        // petite plateforme au bord, avec baril
        set(px + 3, 0, pz, BARREL);
        for (int a = -3; a <= -1; a++) setIfAir(px + a, 0, pz + 3, PLANKS);
    }
}
