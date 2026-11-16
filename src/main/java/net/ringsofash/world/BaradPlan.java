package net.ringsofash.world;

import static net.ringsofash.world.OrthancPlan.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan de Barad-dur : forteresse de 80 x 80 (tambour cannele, bastions, terrasses, eboulis de roche) d'ou monte une tour noire de 250 blocs
 * sans l'oeil, coiffee d'une couronne de pointes vide ; elle est ceinte d'une douve de lave et ouverte a l'ouest par un pont.
 * Grille de blocs abstraits (palette de {@link OrthancPlan} + quelques ajouts), sans dependance a Minecraft.
 * Repere local : x, z de 0 a 149, axe de la tour en (75, 75), y = 0 : premier niveau d'air au-dessus du sol, y negatif : sous le sol.
 */
public final class BaradPlan {
    public static final int W = 150, D = 150, OFF = 20;
    public static final int CX = 75, CZ = 75;
    public static final int TOP = 222;                                   // y de la dalle du sommet ; surface a TOP + 2, pointes jusqu'a TOP + 27
    public static final int H = OFF + TOP + 34;
    public static final double R_LEVEL = 62.0, R_BAND = 75.0;            // distance (Chebyshev) nivelee, puis raccord au terrain naturel
    public static final double FORT = 40.0, MOAT = 52.0;                 // demi-cote de la forteresse, bord exterieur de la douve
    public static final int MOAT_FLOOR = -14, LAVA_TOP = -6;

    public static final byte LAVA = 100, SBASALT = 101, DTILE = 102, MAGMA = 103, STAIR_DT = 104, SLAB_DT = 112, SLAB_DT_TOP = 113, WALL_DT = 114;

    private final byte[] g = new byte[W * H * D];
    private final int[] top = new int[W * D];
    private final int[] floor = new int[W * D];
    private final byte[] cover = new byte[W * D];
    public final List<int[]> chests = new ArrayList<>();

    private static BaradPlan inst;
    public static synchronized BaradPlan get() { if (inst == null) inst = new BaradPlan(); return inst; }

    public BaradPlan() {
        terrain();
        fort();
        bridge();
        rocks();
        tower();
        village();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    public int top(int x, int z) { return top[z * W + x]; }
    public int floor(int x, int z) { return floor[z * W + x]; }
    public byte cover(int x, int z) { return cover[z * W + x]; }
    public static double cheb(int x, int z) { return Math.max(Math.abs(x + 0.5 - CX), Math.abs(z + 0.5 - CZ)); }
    public static boolean area(int x, int z) { return cheb(x, z) < R_BAND; }
    public static boolean level(int x, int z) { return cheb(x, z) <= R_LEVEL; }
    /** 0 dans la zone nivelee, 1 au bord exterieur du raccord */
    public static double band(int x, int z) { return smooth((cheb(x, z) - R_LEVEL) / (R_BAND - R_LEVEL)); }

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

    // ------------------------------------------------------------------ sol, douve
    private void terrain() {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x;
            double m = cheb(x, z);
            floor[i] = -1;
            double n1 = vn(x / 9.0, z / 9.0, 5), n2 = vn(x / 3.0, z / 3.0, 6), h = rnd(x, 3, z);
            double v = n1 * 0.7 + n2 * 0.3;
            byte c = v < -0.35 ? COBBLED_DS : v < 0.0 ? BASALT : v < 0.4 ? TUFF : GRAVEL;
            if (h < 0.06) c = BS; else if (h < 0.09) c = ANDESITE;
            if (m > MOAT && m < MOAT + 1.5) c = BS;                                  // levre de la douve
            if (m >= FORT && m <= MOAT) {
                floor[i] = MOAT_FLOOR;
                c = BASALT;
                for (int y = MOAT_FLOOR + 1; y <= LAVA_TOP; y++) set(x, y, z, LAVA);
            }
            cover[i] = c;
        }
    }

    // ------------------------------------------------------------------ forteresse
    private static double hwDrum(int y) { return FORT - 4.0 * y / 34.0; }
    private static double hwTier(int y) { return 30.0 - 3.5 * (y - 35) / 22.0; }

    private void fort() {
        // pied de la forteresse, dans la douve
        for (int y = MOAT_FLOOR; y <= -1; y++) for (int x = CX - 40; x < CX + 40; x++) for (int z = CZ - 40; z < CZ + 40; z++)
            set(x, y, z, rnd(x, y, z) < 0.2 ? BASALT : BS);
        // tambour cannele (de 80 de cote en bas, un peu moins en haut), puis second etage en retrait
        massif(0, 34, FORT, 4.0, 10, 3);
        massif(35, 57, 30.0, 3.5, 8, 3);
        // bastions des angles, tourelles a mi-mur, tours de la porte
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            roundTower(CX + sx * 33.0, CZ + sz * 33.0, 7.4, 52, 20, 0, 1);
            roundTower(CX + sx * 25.0, CZ + sz * 25.0, 4.6, 76, 14, 35, 2);
        }
        for (int s = -1; s <= 1; s += 2) {
            roundTower(CX + s * 17.0, CZ - 37.5, 4.0, 42, 12, 0, 3);
            roundTower(CX + s * 17.0, CZ + 37.5, 4.0, 42, 12, 0, 3);
            roundTower(CX + 37.5, CZ + s * 17.0, 4.0, 42, 12, 0, 3);
            if (s != 0) roundTower(CX - 37.5, CZ + s * 17.0, 4.0, 42, 12, 0, 3);
        }
        roundTower(CX - 36.0, CZ - 11.0, 6.0, 62, 22, 0, 4);                 // tours de la porte (ouest)
        roundTower(CX - 36.0, CZ + 11.0, 6.0, 62, 22, 0, 4);
        // tunnel d'entree : de la porte jusqu'a la grande salle de la tour
        for (int x = CX - 42; x <= CX - 12; x++) for (int y = 0; y <= 9; y++) for (int z = CZ - 4; z <= CZ + 3; z++) {
            double a = Math.abs(z + 0.5 - CZ), hw = y <= 5 ? 3.5 : y <= 7 ? 2.6 : y <= 8 ? 1.6 : 0.6;
            if (y == 9 || a > hw) continue;
            set(x, y, z, AIR);
        }
        for (int x = CX - 41; x <= CX - 12; x += 5) {                            // lanternes pendues
            set(x, 8, CZ - 1, CHAIN); set(x, 7, CZ - 1, LANTERN);
            set(x, 8, CZ, CHAIN); set(x, 7, CZ, LANTERN);
        }
        for (int x = CX - 42; x <= CX - 12; x++) for (int z = CZ - 3; z <= CZ + 2; z++) set(x, 0, z, ((x + z) & 3) == 0 ? CARPET : CARPET);
        for (int z = CZ - 4; z <= CZ + 3; z++) {                                  // herse relevee
            double a = Math.abs(z + 0.5 - CZ);
            for (int y = 5; y <= 8; y++) if (a <= (y <= 5 ? 3.5 : y <= 7 ? 2.6 : 1.6) && (z & 1) == 0) set(CX - 40, y, z, BARS);
        }
        // creneaux et plateforme du tambour : merlons sur le bord de chaque etage
        for (int x = CX - 42; x < CX + 42; x++) for (int z = CZ - 42; z < CZ + 42; z++) {
            double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
            double e1 = hwDrum(34) - 0.5;
            if (get(x, 34, z) != AIR && m > e1 - 0.6 && ((x + z) & 1) == 0 && get(x, 35, z) == AIR) { set(x, 35, z, BS); if (rnd(x, 35, z) < 0.5) set(x, 36, z, SLAB_BS); }
            double e2 = hwTier(57) - 0.5;
            if (get(x, 57, z) != AIR && m > e2 - 0.6 && m < e2 + 0.6 && ((x + z) & 1) == 0 && get(x, 58, z) == AIR) { set(x, 58, z, BS); if (rnd(x, 58, z) < 0.5) set(x, 59, z, SLAB_BS); }
        }
    }

    /** bloc plein cannele : demi-largeur hw0 qui perd 'loss' blocs sur sa hauteur, coins coupes de 'cham' */
    private void massif(int y0, int y1, double hw0, double loss, int cham, int flute) {
        for (int y = y0; y <= y1; y++) {
            double hw = hw0 - loss * (y - y0) / Math.max(1, (y1 - y0));
            for (int x = CX - 42; x < CX + 42; x++) for (int z = CZ - 42; z < CZ + 42; z++) {
                double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, ax = Math.abs(ox), az = Math.abs(oz), m = Math.max(ax, az);
                int dc = (int) Math.round(hw - 0.5 - m);
                int dch = (int) Math.floor((2 * (hw - 0.5) - cham) - (ax + az));
                int d = Math.min(dc, dch);
                if (d < 0) continue;
                int iu = ax >= az ? (int) Math.floor(oz) : (int) Math.floor(ox);
                boolean groove = d == 0 && y >= y0 + 2 && y <= y1 - 2 && Math.floorMod(iu, flute) == 1 && ax - az != 0;
                if (groove) continue;
                double q = rnd(x, y, z);
                int id = d == 0 && Math.floorMod(iu, flute) == 0 ? BS : q < 0.45 ? BS : q < 0.65 ? PB : q < 0.80 ? DS : q < 0.88 ? BASALT : q < 0.94 ? DTILE : CRACK;
                set(x, y, z, id);
            }
        }
    }

    /** tour ronde cannelee : demi-largeur rad, jusqu'a la hauteur h, galerie a creneaux puis fleche conique de hauteur spire */
    private void roundTower(double tx, double tz, double rad, int h, int spire, int y0, int seed) {
        int r0 = (int) Math.ceil(rad) + 2;
        for (int x = (int) tx - r0; x <= (int) tx + r0; x++) for (int z = (int) tz - r0; z <= (int) tz + r0; z++) {
            if (x < 0 || x >= W || z < 0 || z >= D) continue;
            double dx = x + 0.5 - tx, dz = z + 0.5 - tz, d = Math.hypot(dx, dz);
            double ang = Math.atan2(dz, dx);
            int rib = (int) Math.floor((ang + Math.PI) / (2 * Math.PI) * Math.max(8, (int) (2 * Math.PI * rad / 2.0)));
            for (int y = y0; y <= h; y++) {
                if (d > rad) continue;
                if (d > rad - 1.0 && y > y0 + 2 && y < h - 2 && (rib & 1) == 1) continue;           // cannelures
                double q = rnd(x, y, z);
                boolean slit = d > rad - 2.0 && y > y0 + 8 && y < h - 6 && (y % 10) >= 3 && (y % 10) <= 6 && (rib % 4) == 0 && d > rad - 1.0;
                set(x, y, z, slit ? BARS : q < 0.45 ? BS : q < 0.7 ? PB : q < 0.88 ? DS : DTILE);
            }
            // galerie en encorbellement
            if (d <= rad + 1.0 && d > rad - 1.0) {
                set(x, h + 1, z, (rib & 1) == 0 ? BS : DS);
                if ((rib & 1) == 0) set(x, h + 2, z, SLAB_BS);
            }
            // fleche conique
            for (int k = 0; k < spire; k++) {
                double rs = (rad - 0.6) * (1.0 - (double) k / spire);
                if (d <= Math.max(0.6, rs) && (rs > 0.6 || d <= 0.8)) set(x, h + 2 + k, z, k > spire - 4 ? BS : (k % 6 == 5 ? DS : DTILE));
            }
        }
    }

    // ------------------------------------------------------------------ pont
    private void bridge() {
        for (int x = CX - 54; x <= CX - 40; x++) for (int z = CZ - 4; z <= CZ + 3; z++) {
            double a = Math.abs(z + 0.5 - CZ);
            if (a > 3.5) continue;
            set(x, -2, z, DS);
            set(x, -1, z, ((x + z) & 3) == 0 ? BS : PB);
            if (a > 2.9) { set(x, 0, z, WALL_BS); if (x % 5 == 0) { set(x, 1, z, WALL_BS); set(x, 2, z, LANTERN); } }
        }
        for (int x = CX - 54; x <= CX - 40; x += 5) for (int s = -1; s <= 1; s += 2) {          // piles dans la lave
            int z = CZ + (s < 0 ? -4 : 3);
            for (int y = -13; y <= -3; y++) { set(x, y, z, BS); set(x + 1, y, z, BS); }
        }
        // pylones a l'entree du pont, sur la levre de la douve
        roundTower(CX - 59.0, CZ - 6.0, 3.2, 26, 10, 0, 5);
        roundTower(CX - 59.0, CZ + 6.0, 3.2, 26, 10, 0, 5);
        // route pavee de la douve vers l'ouest
        for (int x = CX - 74; x <= CX - 55; x++) for (int z = CZ - 3; z <= CZ + 2; z++) cover[z * W + x] = ((x + z) & 1) == 0 ? PB : BS;
    }

    // ------------------------------------------------------------------ tour
    private static double base(int y) {
        double t = Math.min(1.0, Math.max(0.0, (y - 40.0) / (TOP - 1 - 40.0)));
        return 10.0 + 7.0 * Math.pow(1 - t, 1.1);
    }
    private static final int COLL = 196;
    private static boolean collar(int y) { return y >= COLL && y <= COLL + 9; }
    private static boolean crownBlock(int y) { return y >= COLL + 10 && y < TOP; }
    private static boolean ledge(int y) { return y >= 59 && y <= TOP - 3 && y % 14 == 13 && !collar(y) && !crownBlock(y); }
    private static double wAt(int y) {
        if (y >= TOP) return base(TOP - 1) + 5;
        if (crownBlock(y)) return base(y) + 3.5;
        if (y >= COLL && y <= COLL + 5) return base(y) + 3.0;
        if (collar(y)) return base(y) + 2.0;
        return base(y) + (ledge(y) ? 1 : 0);
    }
    private static int hinAt(int y) { return Math.max(6, (int) Math.round(base(Math.max(0, Math.min(y, TOP - 1)))) - 3); }
    private static final int[] FLOORS = {0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200};

    private void tower() {
        for (int y = 0; y <= TOP + 1; y++) {
            double hw = wAt(y);
            boolean solidRoof = y >= TOP;
            boolean plain = y < 59;                                            // dans le tambour : on creuse seulement la salle
            int cham = solidRoof ? 0 : 2;
            int hin = hinAt(y);
            for (int x = CX - 20; x < CX + 20; x++) for (int z = CZ - 20; z < CZ + 20; z++) {
                double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, ax = Math.abs(ox), az = Math.abs(oz), m = Math.max(ax, az);
                int dc = (int) Math.round(hw - 0.5 - m);
                int dch = cham == 0 ? 99 : (int) Math.floor((2 * (hw - 0.5) - cham) - (ax + az));
                int d = Math.min(dc, dch);
                int iu = ax >= az ? (int) Math.floor(oz) : (int) Math.floor(ox);
                boolean faceMid = Math.min(ax, az) <= hw - 0.5 - cham - 1;
                if (d >= 0) {
                    if (!solidRoof && m <= hin - 0.5) { set(x, y, z, AIR); continue; }
                    if (plain && y < 40) { if (get(x, y, z) == AIR) set(x, y, z, BS); continue; }
                    int id = skin(x, y, z, d, iu);
                    if (!plain && !solidRoof && y >= 112 && y <= COLL - 3 && !collar(y) && faceMid && d <= 2 && Math.floorMod(iu, 4) == 2) {
                        int s = y % 14;
                        if (s >= 3 && s <= 8 && !ledge(y)) id = d == 0 ? BARS : AIR;
                    }
                    set(x, y, z, id);
                } else if (d == -1 && y >= 59 && !solidRoof && faceMid && dc == -1 && Math.floorMod(iu, 4) == 0 && !collar(y) && !crownBlock(y) && y <= TOP - 2) {
                    set(x, y, z, BS);                                          // pilastres en relief
                }
            }
        }
        // collerette : dents sur le bord de la corniche
        int ih = (int) Math.round(wAt(COLL + 9));
        for (int x = CX - 24; x < CX + 24; x++) for (int z = CZ - 24; z < CZ + 24; z++) {
            double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
            if (m != ih - 0.5) continue;
            int iu = Math.abs(ox) >= Math.abs(oz) ? (int) Math.floor(oz) : (int) Math.floor(ox);
            if (Math.floorMod(iu, 2) != 0) continue;
            int len = 3 + (int) (rnd(x, 31, z) * 6);
            for (int y = COLL + 10; y < COLL + 10 + len; y++) if (get(x, y, z) == AIR) set(x, y, z, y > COLL + 8 + len ? SLAB_BS : BS);
        }
        for (int y = 60; y < COLL - 4; y++) if (ledge(y) && (y / 14) % 3 == 0) {
            int cc = (int) Math.round(wAt(y) - 2.6);
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) for (int h = 1; h <= 8; h++) {
                int o = h > 5 ? 0 : 1;
                for (int a = 0; a <= o; a++) for (int b = 0; b <= o; b++)
                    setIfAir(CX + sx * cc - (sx > 0 ? 0 : 1) + (sx > 0 ? -a : a), y + h, CZ + sz * cc - (sz > 0 ? 0 : 1) + (sz > 0 ? -b : b), h > 6 ? BS : PB);
            }
        }
        crown();
        interior();
        door();
    }

    private int skin(int x, int y, int z, int d, int iu) {
        if (ledge(y)) return d == 0 ? DS : (rnd(x, y, z) < 0.5 ? DS : PB);
        double q = rnd(x, y, z);
        if (y >= TOP) return d == 0 ? DS : PB;
        if (collar(y)) return d <= 1 ? BS : PB;
        if (crownBlock(y)) return d == 0 && Math.floorMod(iu, 3) == 0 ? BS : q < 0.1 ? DS : q < 0.15 ? CRACK : PB;
        if (d == 0 && Math.floorMod(iu, 4) == 0) return BS;
        return q < 0.05 ? CRACK : q < 0.1 ? DS : q < 0.12 ? CHISEL : q < 0.14 ? DTILE : PB;
    }

    /** couronne : plateforme, parapet et quatre pointes inclinees vers l'axe, qui forment un berceau vide (pas d'oeil) */
    private void crown() {
        double pm = wAt(TOP) - 0.5;
        for (int x = CX - 18; x < CX + 18; x++) for (int z = CZ - 18; z < CZ + 18; z++) {
            double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
            if (m > pm || m < pm - 1) continue;
            set(x, TOP + 2, z, WALL_BS);
            if (((x + z) & 1) == 0) set(x, TOP + 3, z, WALL_BS);
        }
        int len = 30;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            for (int h = 0; h < len; h++) {
                double f = (double) h / len;
                int size = f < 0.3 ? 5 : f < 0.5 ? 4 : f < 0.7 ? 3 : h < len - 5 ? 2 : 1;
                double c = 11.5 - 6.0 * Math.pow(f, 1.5);                     // la pointe se penche vers le centre
                int x0 = (int) Math.round(CX + sx * c - size / 2.0), z0 = (int) Math.round(CZ + sz * c - size / 2.0);
                for (int a = 0; a < size; a++) for (int b = 0; b < size; b++) {
                    if (size >= 4 && (a == 0 || a == size - 1) && (b == 0 || b == size - 1)) continue;
                    set(x0 + a, TOP + 2 + h, z0 + b, h > len - 5 ? BS : (rnd(x0 + a, h, z0 + b) < 0.12 ? DS : PB));
                }
            }
        }
        // quatre dents plus courtes au milieu de chaque cote
        for (int s = 0; s < 4; s++) {
            int ax = s < 2 ? (s == 0 ? 1 : -1) : 0, az = s < 2 ? 0 : (s == 2 ? 1 : -1);
            for (int h = 0; h < 15; h++) {
                int size = h < 5 ? 3 : h < 10 ? 2 : 1;
                double c = 12.0 - 3.0 * h / 15.0;
                int x0 = (int) Math.round(CX + ax * c - size / 2.0), z0 = (int) Math.round(CZ + az * c - size / 2.0);
                for (int a = 0; a < size; a++) for (int b = 0; b < size; b++) set(x0 + a, TOP + 2 + h, z0 + b, h > 10 ? BS : PB);
            }
        }
    }

    private void interior() {
        for (int y = 0; y <= TOP - 1; y++) for (int dx = -2; dx <= 1; dx++) for (int dz = -2; dz <= 1; dz++)
            set(CX + dx, y, CZ + dz, y % 10 == 9 ? CHISEL : (rnd(CX + dx, y, CZ + dz) < 0.1 ? CRACK : DS));
        for (int fy : FLOORS) {
            if (fy == 0) continue;
            for (int x = CX - 20; x < CX + 20; x++) for (int z = CZ - 20; z < CZ + 20; z++) {
                double ox = x + 0.5 - CX, oz = z + 0.5 - CZ, m = Math.max(Math.abs(ox), Math.abs(oz));
                int hin = hinAt(fy - 1);
                if (m > hin - 0.5) continue;
                if (x >= CX - 4 && x <= CX + 3 && z >= CZ - 4 && z <= CZ + 3) continue;
                set(x, fy - 1, z, ((x + z) & 1) == 0 ? PB : BS);
            }
        }
        // escalier : anneau de 20 cases, une marche par case (depart a l'ouest, au pied du tunnel)
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
        int startIdx = 0;
        for (int i = 0; i < n; i++) if (ring.get(i)[0] == -3 && ring.get(i)[1] == 1) startIdx = i;
        for (int k = 0; k <= TOP + 1; k++) {
            int i = (startIdx + k) % n;
            int[] c = ring.get(i);
            int blk = stair(STAIR_BS, dirs[i], false);
            set(CX + c[0], k, CZ + c[1], blk);
            for (int[] e : outward(c)) set(CX + e[0], k, CZ + e[1], blk);
            for (int h = 1; h <= 3; h++) {
                setAirIfRoof(CX + c[0], k + h, CZ + c[1]);
                for (int[] e : outward(c)) setAirIfRoof(CX + e[0], k + h, CZ + e[1]);
            }
        }
        for (int lv = 0; lv < FLOORS.length; lv++) {
            int fy = FLOORS[lv];
            int hin = hinAt(fy);
            int lo = CX - hin, hi = CX + hin - 1, loz = CZ - hin, hiz = CZ + hin - 1;
            for (int y = fy; y < fy + 19; y++)
                for (int[] cc : new int[][]{{lo, loz}, {hi - 1, loz}, {lo, hiz - 1}, {hi - 1, hiz - 1}})
                    for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) set(cc[0] + a, y, cc[1] + b, y == fy + 18 ? CHISEL : DS);
            if (lv % 2 == 1) for (int x = lo + 3; x <= hi - 3; x += 3) { box(x, fy, loz, x + 1, fy + 1, loz, SHELF); box(x, fy, hiz, x + 1, fy + 1, hiz, SHELF); }
            int[][] lampsAt = {{CX - 6, CZ - 6}, {CX + 5, CZ - 6}, {CX - 6, CZ + 5}, {CX + 5, CZ + 5}};
            for (int[] lp : lampsAt) {
                if (fy < FLOORS[FLOORS.length - 1] && get(lp[0], fy + 18, lp[1]) == AIR) {
                    for (int h = 0; h < 3; h++) set(lp[0], fy + 18 - h, lp[1], CHAIN);
                    set(lp[0], fy + 15, lp[1], LANTERN);
                }
            }
            int cxc = lv % 2 == 0 ? lo + 1 : hi - 1, czc = lv % 3 == 0 ? loz + 2 : hiz - 2;
            if (lv >= 1) chests.add(new int[]{cxc, fy, czc, lv % 2 == 0 ? E : W_});
        }
        // salle du trone tout en haut : estrade et siege contre le mur est, face a l'escalier
        int fy = FLOORS[FLOORS.length - 1];
        int hinT = hinAt(fy), hx = CX + hinT - 1;
        box(hx - 1, fy, CZ - 3, hx, fy, CZ + 2, BS);
        box(hx, fy + 1, CZ - 2, hx, fy + 4, CZ + 1, DS);
        set(hx - 1, fy + 1, CZ - 1, stair(STAIR_BS, E, false)); set(hx - 1, fy + 1, CZ, stair(STAIR_BS, E, false));
        set(hx - 1, fy + 1, CZ - 2, SLAB_BS); set(hx - 1, fy + 1, CZ + 1, SLAB_BS);
        set(hx - 1, fy + 2, CZ - 2, WALL_BS); set(hx - 1, fy + 2, CZ + 1, WALL_BS);
        box(CX + 4, fy, CZ - 1, hx - 2, fy, CZ, CARPET);
        chests.add(new int[]{hx - 1, fy + 1, CZ + 2, W_});
        for (int x = CX - 4; x <= CX + 3; x++) for (int z = CZ - 4; z <= CZ + 3; z++) {
            boolean border = x == CX - 4 || x == CX + 3 || z == CZ - 4 || z == CZ + 3;
            boolean pillar = x >= CX - 2 && x <= CX + 1 && z >= CZ - 2 && z <= CZ + 1;
            if (pillar || get(x, TOP + 1, z) != AIR) continue;
            if (border) set(x, TOP + 1, z, BARS);
        }
        for (int k = TOP - 2; k <= TOP + 1; k++) {
            int i = (startIdx + k) % n;
            int[] c = ring.get(i);
            set(CX + c[0], k, CZ + c[1], stair(STAIR_BS, dirs[i], false));
            for (int[] e : outward(c)) set(CX + e[0], k, CZ + e[1], stair(STAIR_BS, dirs[i], false));
            for (int h = k + 1; h <= k + 3; h++) {
                set(CX + c[0], h, CZ + c[1], AIR);
                for (int[] e : outward(c)) set(CX + e[0], h, CZ + e[1], AIR);
            }
        }
        box(CX - 2, TOP, CZ - 2, CX + 1, TOP + 1, CZ + 1, DS);
        box(CX - 1, TOP + 2, CZ - 1, CX, TOP + 2, CZ, BS);
        set(CX - 1, TOP + 3, CZ - 1, LANTERN); set(CX, TOP + 3, CZ, LANTERN);
    }

    private void setAirIfRoof(int x, int y, int z) { if (y >= TOP && y <= TOP + 1) set(x, y, z, AIR); }

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

    /** l'entree de la tour est le tunnel de la forteresse (cote ouest) : on le prolonge jusqu'au vide de l'escalier */
    private void door() {
        for (int x = CX - 16; x <= CX - 5; x++) for (int y = 0; y <= 9; y++) for (int z = CZ - 4; z <= CZ + 3; z++) {
            double a = Math.abs(z + 0.5 - CZ), hw = y <= 5 ? 3.5 : y <= 7 ? 2.6 : y <= 8 ? 1.6 : 0.6;
            if (y == 9 || a > hw) continue;
            set(x, y, z, AIR);
        }
        for (int x = CX - 16; x <= CX - 5; x++) for (int z = CZ - 3; z <= CZ + 2; z++) set(x, 0, z, CARPET);
    }

    private void rocks() {
        Random rnd = new Random(777L);
        // eboulis de roche autour de la tour, sur le second etage : cone irregulier de basalte et de pierre noire
        for (int x = CX - 24; x < CX + 24; x++) for (int z = CZ - 24; z < CZ + 24; z++) {
            double r = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ);
            if (r >= 24) continue;
            double hh = 58 + 56 * Math.pow(Math.max(0, 1 - r / 23.0), 1.1) + 9 * vn(x / 5.0, z / 5.0, 3) + 3 * vn(x / 2.2, z / 2.2, 4);
            for (int y = 58; y <= hh; y++) {
                if (get(x, y, z) != AIR) continue;
                double q = rnd(x, y, z);
                set(x, y, z, q < 0.28 ? BASALT : q < 0.45 ? SBASALT : q < 0.65 ? BS : q < 0.80 ? TUFF : q < 0.93 ? COBBLED_DS : MAGMA);
            }
        }
        // aiguilles de roche sur le cone
        for (int i = 0, n = 0; i < 400 && n < 40; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 6 + rnd.nextDouble() * 16;
            int x = (int) (CX + r * Math.cos(a)), z = (int) (CZ + r * Math.sin(a));
            int y = 58;
            while (y < 130 && get(x, y, z) != AIR) y++;
            int hgt = 4 + rnd.nextInt(8);
            for (int k = 0; k < hgt; k++) {
                int w = k < hgt * 0.5 ? 2 : 1;
                for (int ax = 0; ax < w; ax++) for (int az = 0; az < w; az++) setIfAir(x + ax, y + k, z + az, rnd.nextInt(4) == 0 ? SBASALT : BASALT);
            }
            n++;
        }
    }

    // ------------------------------------------------------------------ maisons des terrasses, eboulis de roche, brasiers
    private void village() {
        Random rnd = new Random(4242L);
        // maisons a toit pointu sur la terrasse du tambour (anneau entre les deux etages)
        for (int tries = 0, n = 0; tries < 900 && n < 70; tries++) {
            int sx = 4 + rnd.nextInt(2);
            int x0 = CX - 37 + rnd.nextInt(74), z0 = CZ - 37 + rnd.nextInt(74);
            boolean okp = true;
            for (int a = -1; a <= sx && okp; a++) for (int b = -1; b <= sx && okp; b++) {
                int x = x0 + a, z = z0 + b;
                double m = cheb(x, z);
                if (m < 31.0 || m > 35.5 || get(x, 34, z) == AIR || get(x, 35, z) != AIR || get(x, 36, z) != AIR || get(x, 37, z) != AIR) okp = false;
            }
            if (!okp) continue;
            int h = 4 + rnd.nextInt(4);
            for (int a = 0; a < sx; a++) for (int b = 0; b < sx; b++) for (int y = 35; y < 35 + h; y++)
                set(x0 + a, y, z0 + b, rnd.nextInt(9) == 0 ? BS : (a == 0 || b == 0 || a == sx - 1 || b == sx - 1) ? PB : DS);
            // toit : anneau d'escaliers vers le centre, pointe au-dessus
            for (int a = -1; a <= sx; a++) for (int b = -1; b <= sx; b++) {
                boolean edgeA = a == -1 || a == sx, edgeB = b == -1 || b == sx;
                if (!edgeA && !edgeB) continue;
                int dir = edgeA && !edgeB ? (a == -1 ? E : W_) : (b == -1 ? S : N);
                if (edgeA && edgeB) continue;
                set(x0 + a, 35 + h, z0 + b, stair(STAIR_DT, dir, false));
            }
            for (int a = 0; a < sx; a++) for (int b = 0; b < sx; b++) set(x0 + a, 35 + h, z0 + b, DTILE);
            set(x0 + sx / 2, 36 + h, z0 + sx / 2, DTILE); set(x0 + sx / 2, 37 + h, z0 + sx / 2, BS); set(x0 + sx / 2, 38 + h, z0 + sx / 2, LANTERN);
            n++;
        }
        // brasiers sur la terrasse basse
        for (int i = 0, n = 0; i < 600 && n < 16; i++) {
            int x = CX - 37 + rnd.nextInt(74), z = CZ - 37 + rnd.nextInt(74);
            double m = cheb(x, z);
            if (m < 31.0 || m > 36.0 || get(x, 34, z) == AIR || get(x, 35, z) != AIR || get(x, 36, z) != AIR) continue;
            set(x, 35, z, BS); set(x, 36, z, CAMPFIRE);
            n++;
        }
    }
}
