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
    public static final int W = 320, D = 340, OFF = 8, TOP = 280, H = OFF + TOP + 1, CX = 160, CZ = 165, ZS = 35;   // ZS : decalage du plan vers le sud (la route d acces occupe les 40 derniers blocs)
    public static final double[] R = {125, 107, 89, 71, 53, 35, 17};              // rayon exterieur de chaque niveau (largeur 18)
    public static final int[] FL = {18, 36, 54, 72, 90, 108, 126};                // niveau debout de chaque terrasse
    public static final int PROW_TOP = 126, PROW_TIP = 104;                         // sommet de l'eperon, longueur de l'eperon vers le sud

    public static final int AIR = 0, STONE = 1, ANDESITE = 2, TUFF = 3, CALCITE = 4, DIORITE = 5, GRASS = 6, DIRT = 7, SNOW = 8, COBBLE = 9, GRAVEL = 10,
            WB = 20, WB_STAIR = 24, WB_SLAB = 32, WB_SLAB_TOP = 33, WB_WALL = 34, QB = 35, SQ = 36, PD = 37, SBR = 38, QPILLAR_Y = 40, CHISEL = 41, PAVE = 42, PAVE2 = 43,
            GLASS = 50, BARS = 51, DPLANKS = 52, DLOG_Y = 53, LANTERN = 54, CHAIN = 55, FENCE_B = 56, WATER = 57, BIRCH_Y = 58, BIRCH_FENCE = 59,
            SLATE = 60, SLATE_STAIR = 64, SLATE_SLAB = 72, SLATE_SLAB_TOP = 73, COPPER = 74, COPPER_STAIR = 80, COPPER_SLAB = 88, COPPER_SLAB_TOP = 89,
            LEAF = 90, FLOWER1 = 91, FLOWER2 = 92, GRASS_S = 93, DWALL = 94, BANNER_G = 95, WOOL_W = 96, WOOL_G = 97, TORCH_B = 98, LAMPBLOCK = 99, SLAB_STONE = 100,
            QUARTZ_SLAB = 101, QUARTZ_SLAB_TOP = 102, GOLD = 103, DOOR_PL = 104, HLANTERN = 105, KEEL = 111, YB = 44, WTLOG = 106, WTLEAF = 107, WTBLOOM = 108,
            MOSS = 112, MOSSC = 113, PATH = 114, COARSE = 115, SBRS = 116, BARREL = 117, HAY = 118, CAMPF = 119, SBRW = 120, MELON = 121, PUMPKIN = 122, OAKLEAF = 123, GLOW = 124, TRAPD = 125,
            DOOR = 130;                                                              // DOOR + dir * 2 (+ 1 : moitie haute), dir = N E S W
    public static final int ROAD_N = 30, ROAD_HW = 7;                                 // longueur de la route d'acces au sud du pied de la grande rampe
    public static final int N = 0, E = 1, S = 2, W_ = 3;

    private final byte[] g = new byte[W * H * D];
    private final int[] fill = new int[W * D];                                   // y de la derniere cellule pleine de la colonne (-99 = rien)
    private final byte[] kind = new byte[W * D];                                 // 0 rien, 1 terrasse, 2 montagne, 3 eperon, 4 rampe, 5 contrefort
    private final byte[] ring = new byte[W * D];                                 // niveau de la terrasse (-1 hors terrasses)
    private final int[] top = new int[W * D];
    private final boolean[] flank = new boolean[W * D];                         // escalier du flanc de l'eperon (monte vers le sud)
    private final boolean[] occ = new boolean[W * D];                            // emprise prise par un batiment
    public final List<int[]> spawns = new ArrayList<>();                    // {genre (0 civil, 1 garde, 2 cheval, 3 poule), x, y, z}
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
        bridgeRamps();
        citadel();
        rocks();
        carveStreets();
        openStairTop();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    private static double cliffZ(int x) { return CZ - 3 + 6 * vn(x / 20.0, 0.5, 1); }

    private double mountain(int x, int z) {
        int zz = z - ZS;
        double b = cliffZ(x) - (z + 0.5);
        // face en crans : des anses creusees dans la falaise et des contreforts qui avancent, au lieu d'un mur droit
        double cove = 11 * Math.max(0, vn(x / 7.0, zz / 7.0, 31)) + 5 * Math.max(0, vn(x / 3.5, zz / 3.5, 32));
        b -= cove * (1 - smooth((b - 14) / 26));
        if (b < 0) return -1;
        double lat = Math.abs(x + 0.5 - CX);
        double lx = 1 - smooth((lat - 66) / 60);                                   // la montagne s'arrete a 126 blocs du centre : au-dela, Minecraft ne genere plus la structure
        double base = 128 + 150 * smooth(b / 115);
        double rg = vn(x / 40.0, zz / 40.0, 2) * 30 + vn(x / 16.0, zz / 16.0, 3) * 12 + (1 - Math.abs(vn(x / 55.0, zz / 55.0, 4))) * 35 * smooth(b / 40);
        // ravines et nervures verticales : la paroi n'est plus un plan
        double gr = 1 - smooth(Math.abs(vn(x / 6.0, zz / 15.0, 41)) / 0.14);
        double rib = 1 - smooth(Math.abs(vn(x / 11.0, zz / 28.0, 42)) / 0.2);
        double carve = (12 * gr + 16 * rib) * (1 - smooth((b - 70) / 40));
        double taper = 1 - smooth((b - 76) / 32.0);                                  // idem au nord : elle retombe a rien avant la limite
        double h = (base + rg - carve) * taper * lx * (0.35 + 0.65 * smooth((zz + 0.5) / 80.0));
        return h < 3 ? -1 : h;
    }

    /** la quille : plateau jusqu'a 26 blocs du centre, puis elle redescend vers la proue */
    private static int prowTop(int z) {
        double dz = z + 0.5 - CZ;
        if (dz <= 26) return PROW_TOP - 1;
        return PROW_TOP - 1;                                                   // la crete reste au niveau de la cour et de l'Arbre Blanc sur toute sa longueur
    }

    /** demi-largeur de l'eperon : large au plateau, puis une lame etroite qui fend la cite jusqu'au premier mur */
    private static double prowHW(double dz) {
        if (dz <= 26) return 21;
        return Math.max(0, 21 * (1 - (dz - 26) / (PROW_TIP - 26.0)));              // un triangle aux bords droits : la lame se resserre en ligne droite jusqu'a la pointe
    }

    /** distance a l'interieur du bord de l'eperon (negative : hors de l'eperon) ; le bord est irregulier, pas une ligne droite */
    private static double prowEdge(int x, int z) {
        double dz = z + 0.5 - CZ;
        if (dz < -14 || dz > PROW_TIP + 1) return -1;
        double hw = prowHW(dz), k = Math.min(1, hw / 10);
        if (dz <= 26) hw += 2.4 * vn(z / 7.0, 3.3, 5) * k + 1.3 * vn(z / 3.0, 7.7, 6) * k;       // le plateau garde un bord irregulier, la lame est droite
        return hw - Math.abs(x + 0.5 - CX);
    }

    private static boolean inProw(int x, int z) { return prowEdge(x, z) >= 0; }

    /** hauteur de l'eperon : plateau, puis epaule arrondie et rongee vers le bord */
    private static int prowTopAt(int x, int z) {
        double e = prowEdge(x, z);
        int t = PROW_TOP - 1;
        double dz = z + 0.5 - CZ;
        if (e < 3 && dz <= 26) { double sh = 3 - e; t -= (int) (sh * sh * 1.3 + 2.5 * Math.max(0, vn(x / 3.0, z / 3.0, 52)) * sh); }
        return t;
    }

    /** rampes : {x central, demi-largeur, z de depart (le pied, au sud), niveau du bas, niveau du haut} ; elles montent vers le nord, une marche par bloc */
    public final List<int[]> ramps = new ArrayList<>();

    private void columns() {
        ramps.add(new int[]{CX, 5, CZ + (int) R[0], 0, FL[0]});
        double[][] rp = {{46, 1}, {-40, 2}, {32, 3}, {-30, 4}, {29, 5}};
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
            if (m > 0 && !(m < PROW_TOP + 2 && inProw(x, z))) { fill[i] = (int) m; kind[i] = 2; continue; }
            if (inProw(x, z)) { fill[i] = prowTopAt(x, z); kind[i] = 3; continue; }
            if (r <= R[0] && z + 0.5 >= cliffZ(x)) {
                int k = Math.min(6, (int) ((R[0] - r) / 18));
                fill[i] = FL[k] - 1; kind[i] = 1; ring[i] = (byte) k; continue;
            }
            double lat = Math.abs(dx);
            if (lat > 84 && lat < 127 && z + 0.5 >= cliffZ(x) && z + 0.5 - cliffZ(x) < 78) {
                double fh = 70 * smooth((lat - 84) / 22) * (1 - smooth((lat - 104) / 22)) * (1 - smooth((z + 0.5 - cliffZ(x)) / 75)) + 6 * vn(x / 12.0, z / 12.0, 6) * smooth((lat - 84) / 22) * (1 - smooth((lat - 104) / 22));
                if (fh > 5) { fill[i] = (int) fh; kind[i] = 5; }
            }
        }
        // escalier du flanc de l'eperon (niveau 6 -> cour) : il part de la rue du niveau 6, longe le bord est de l'eperon vers le sud et debouche sur un palier de la cour
        for (int z = CZ + 6; z <= CZ + 27; z++) for (int x = CX + 22; x <= CX + 25; x++) {
            int i = z * W + x;
            if (z > CZ + 24 && x > CX + 22) continue;
            fill[i] = Math.min(PROW_TOP - 1, FL[5] - 1 + (z - (CZ + 6))); kind[i] = 4; flank[i] = true;
        }
        for (int z = CZ + 24; z <= CZ + 27; z++) for (int x = CX + 14; x <= CX + 22; x++) {          // palier de debarquement, a la hauteur de la cour
            int i = z * W + x;
            fill[i] = PROW_TOP - 1; kind[i] = 3;
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
                if (inside && (x + z) % 9 == 0) set(x, b + h - 1, z, HLANTERN);                   // la lanterne pend directement du plafond plein (plus de trou dans la voute)
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

    /** tout est plein : la montagne, l'eperon et les remparts sont de la roche ou de la maconnerie jusqu'au sol (plus de coque creuse, donc plus de trous ni d'arches) */
    private void materialize() {
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x, f = fill[i];
            if (f == -99) continue;
            int mn1 = nbMin(x, z, 1), mn2 = nbMin(x, z, 2);
            int k = kind[i];
            boolean rock = k == 2 || k == 5, keel = k == 3;
            for (int y = -6; y <= f; y++) {
                boolean surf = y >= f - 1 || y > mn1 || (rock || keel) && y > mn2 - 2;           // la peau visible, un peu plus epaisse pour la roche
                int id;
                if (keel && y == f && z + 0.5 - CZ > 26) id = paving(x, z);                           // le dessus de la lame : sol de pierre blanche
                else if (keel) id = surf ? keelBlock(x, y, z, f, y == f || y > mn1) : (rnd(x, y, z) < 0.5 ? STONE : ANDESITE);
                else if (rock) id = surf ? rockBlock(x, y, z, f, k, y == f || y > mn1) : (rnd(x, y, z) < 0.6 ? STONE : TUFF);
                else if (y == f) id = paving(x, z);
                else if (surf) id = wallBlock(x, y, z);
                else id = STONE;
                set(x, y, z, id);
            }
        }
    }

    /** roche de l'eperon : calcaire clair, veines, mousse sur les replats bas */
    private int keelBlock(int x, int y, int z, int f, boolean face) {
        double q = rnd(x, y, z);
        double layer = Math.floor((y + 5 * vn(x / 14.0, z / 14.0, 53)) / 3.0);
        double lh = rnd((int) layer, 19, 7);
        if (face && vn(x / 2.5 + y / 4.0, z / 2.5, 55) > 0.72) return ANDESITE;                  // fissures sombres
        if (lh < 0.28) return q < 0.7 ? CALCITE : DIORITE;
        if (lh < 0.5) return q < 0.7 ? DIORITE : STONE;
        if (lh < 0.7) return q < 0.6 ? STONE : (q < 0.85 ? ANDESITE : KEEL);
        if (lh < 0.85) return q < 0.7 ? KEEL : CALCITE;
        return q < 0.5 ? TUFF : DIORITE;
    }

    /** la falaise : strates inclinees, fissures, mousse, eboulis et neige en altitude */
    private int rockBlock(int x, int y, int z, int f, int k, boolean surface) {
        double q = rnd(x, y, z);
        if (surface && y >= 258 + 14 * vn(x / 20.0, z / 20.0, 8)) return SNOW;
        if (surface && f < 80 && y == f) return k == 5 ? (q < 0.12 ? COARSE : GRASS) : (q < 0.4 ? GRASS : (q < 0.55 ? GRAVEL : STONE));
        double layer = Math.floor((y + 11 * vn(x / 22.0, z / 22.0, 11) + 3 * vn(x / 6.0, z / 6.0, 12)) / 5.0);
        double lh = rnd((int) layer, 17, 3);
        if (surface && y < 150 && vn(x / 6.0, z / 6.0 + y / 9.0, 56) > 0.58) return q < 0.7 ? MOSS : MOSSC;
        if (surface && vn(x / 3.0 + y / 5.0, z / 3.0, 57) > 0.74) return COBBLE;                    // fissures
        if (surface && f - y < 3 && f < 200 && q < 0.08) return GRAVEL;
        if (lh < 0.22) return q < 0.8 ? STONE : ANDESITE;
        if (lh < 0.4) return q < 0.75 ? ANDESITE : STONE;
        if (lh < 0.52) return q < 0.7 ? TUFF : ANDESITE;
        if (lh < 0.68) return q < 0.85 ? STONE : DIORITE;
        if (lh < 0.8) return q < 0.7 ? CALCITE : STONE;
        if (lh < 0.9) return q < 0.7 ? ANDESITE : TUFF;
        return q < 0.5 ? DIORITE : STONE;
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
            if (flank[i]) { set(x, f, z, f < PROW_TOP - 1 ? stair(WB_STAIR, S, false) : WB); continue; }
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

    /** la rue de chaque niveau traverse la tranchee de la rampe qui monte au niveau suivant : un pont de pierre a hauteur de rue l'enjambe, sans parapet ni obstacle */
    private void bridgeRamps() {
        for (int n = 0; n < ramps.size(); n++) {
            int[] r = ramps.get(n);
            int xr = r[0], hw = r[1], b = r[4];
            int band = n;                                                          // la rampe n traverse la bande n (niveau b)
            double ro = R[band];
            for (int x = xr - hw - 1; x <= xr + hw + 1; x++) for (int z = r[2] - (r[4] - r[3]) - 2; z <= r[2] + 2; z++) {
                if (!area(x, z)) continue;
                double rr = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ);
                if (rr < ro - 12.5 || rr > ro - 7.5) continue;
                for (int y = b; y <= b + 3; y++) set(x, y, z, AIR);
                int below = get(x, b - 1, z);
                if (below == AIR || below == WB_WALL || below == FENCE_B) set(x, b - 1, z, PAVE);
            }
        }
    }

    /** garantit une rue continue (3 blocs de large, 4 de haut) sur chaque niveau : les tours de garde, maisons et decors ne la barrent jamais */
    private void carveStreets() {
        for (int k = 0; k < 6; k++) {
            double rm = R[k] - 10;
            int b = FL[k];
            for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
                int i = z * W + x;
                if (kind[i] != 1 && kind[i] != 4) continue;
                if (kind[i] == 1 && ring[i] != k) continue;
                double rr = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ);
                if (Math.abs(rr - rm) > 1.7) continue;
                if (kind[i] == 4 && (flank[i] || !(fill[i] >= b - 1 && fill[i] <= b))) continue;
                for (int y = b; y <= b + 3; y++) set(x, y, z, AIR);
                int below = get(x, b - 1, z);
                if (below == AIR || below == WB_WALL || below == FENCE_B) set(x, b - 1, z, PAVE);
            }
        }
    }

    /** le haut de l'escalier du flanc debouche dans la cour : le parapet du bord de l'eperon y est ouvert */
    private void openStairTop() {
        for (int x = CX + 14; x <= CX + 26; x++) for (int z = CZ + 19; z <= CZ + 25; z++) {
            for (int y = PROW_TOP; y <= PROW_TOP + 3; y++) set(x, y, z, AIR);
            if (x >= CX + 20 && z >= CZ + 23) {
                int b = get(x, PROW_TOP - 1, z);
                if (b == AIR || b == 111 || b == 20 || b == WB_STAIR) set(x, PROW_TOP - 1, z, WB);
            }
        }
    }

    private void terraces() {
        gates();
        for (int k = 0; k < 7; k++) houses(k);
        for (int k = 0; k < 7; k++) streetLife(k);
    }

    /** tours de garde de chaque porte : deux tours de part et d'autre de la rampe, reliees par un porche voute en ogive (plus de linteau flottant) */
    private void gates() {
        for (int n = 0; n < ramps.size(); n++) {
            int[] r = ramps.get(n);
            int xr = r[0], hw = r[1], z0 = r[2], lo = r[3], hi = r[4];
            boolean grand = n == 0;
            int sz = grand ? 11 : 7, th = grand ? 30 : 15, base = hi;
            int zc = z0 - (grand ? 9 : 5);
            int built = 0;
            for (int sd = -1; sd <= 1; sd += 2) {
                int cx0 = xr + sd * (hw + 2 + sz / 2 + 1);
                int x0 = cx0 - sz / 2, z1 = zc - sz / 2;
                boolean can = true;
                for (int x = x0; x < x0 + sz && can; x++) for (int z = z1; z < z1 + sz; z++) {
                    if (!area(x, z) || kind[z * W + x] != 1 || fill[z * W + x] != base - 1 || occ[z * W + x]) { can = false; break; }
                }
                if (!can) continue;
                gateTower(x0, z1, sz, base, th);
                built++;
            }
            boolean okL = true;
            for (int x = xr - hw - 2; x <= xr + hw + 2; x++) for (int z = zc - 2; z <= zc + 2; z++) if (!area(x, z) || kind[z * W + x] == 0) okL = false;
            if (okL) archGate(xr, zc, hw, base, grand ? base + 2 : base - 3, grand);
            if (okL && built == 2) {                                   // galerie : on traverse les deux tours et le porche d'un bout a l'autre de la rue, sur un plancher au niveau de la rue
                for (int x = xr - hw - 2; x <= xr + hw + 2; x++) for (int z = zc - 1; z <= zc + 1; z++) {
                    for (int y = base; y <= base + 3; y++) set(x, y, z, AIR);
                    if (get(x, base - 1, z) == AIR) set(x, base - 1, z, PAVE);
                }
            }
            int zTop = z0 - (hi - lo) - 2;
            spawns.add(new int[]{1, xr - hw + 1, hi, zTop}); spawns.add(new int[]{1, xr + hw - 1, hi, zTop});
            if (grand) { spawns.add(new int[]{1, xr - 3, 0, z0 + 3}); spawns.add(new int[]{1, xr + 3, 0, z0 + 3}); }
        }
    }

    /** hauteur du sommet de l'ogive (au-dessus de l'axe) pour une demi-ouverture hw, a l'abscisse ax */
    private static int archTop(int hw, int ax, int ys) {
        double q = 4.0 * hw * hw - (double) (hw + ax) * (hw + ax);
        return ys + (int) Math.floor(Math.sqrt(Math.max(0, q)));
    }

    /** porche : masse pleine entre les deux tours, ouverte par une ogive qui laisse passer la rampe ; herse relevee, lanternes, clef d'or, bannieres, creneaux */
    private void archGate(int xr, int zc, int hw, int base, int ys, boolean grand) {
        int half = hw + 2;
        int yTop = archTop(hw, 0, ys) + 4;
        for (int dx = -half; dx <= half; dx++) for (int z = zc - 2; z <= zc + 2; z++) {
            int ax = Math.abs(dx), x = xr + dx;
            for (int y = base; y <= yTop; y++) {
                boolean open = ax <= hw && y <= archTop(hw, ax, ys);
                if (open) continue;
                boolean edge = ax <= hw + 1 && (y == archTop(hw, Math.min(ax, hw), ys) + 1 || ax == hw + 1 && y <= ys + 1);
                boolean frontFace = z == zc + 2 || z == zc - 2;
                int id = edge && frontFace ? QB : (y % 9 == 8 ? YB : (rnd(x, y, z) < 0.05 ? PD : WB));
                set(x, y, z, id);
            }
            // creneaux au sommet
            if (z == zc - 2 || z == zc + 2) { set(x, yTop + 1, z, WB); if ((x + z) % 2 == 0) set(x, yTop + 2, z, WB); }
            else set(x, yTop + 1, z, WB);
        }
        // herse relevee : barreaux dans le haut de l'ogive, sur la face nord
        for (int dx = -hw + 1; dx <= hw - 1; dx++) for (int y = ys + 2; y < archTop(hw, Math.abs(dx), ys); y++) set(xr + dx, y, zc - 1, BARS);
        // lanternes pendues sous la voute, clef d'or, bannieres sur la face sud
        for (int dx : new int[]{-2, 2}) set(xr + dx, archTop(hw, 2, ys), zc + 1, HLANTERN);
        set(xr, archTop(hw, 0, ys) + 1, zc + 2, GOLD);
        for (int dx = -half; dx <= half; dx++) {
            int ax = Math.abs(dx);
            if (ax != hw + 1) continue;
            for (int j = 0; j < (grand ? 7 : 5); j++) set(xr + dx, yTop - 1 - j, zc + 3, j == (grand ? 6 : 4) ? WOOL_G : WOOL_W);
        }
    }

    private void gateTower(int x0, int z0, int sz, int base, int h) {
        for (int x = x0; x < x0 + sz; x++) for (int z = z0; z < z0 + sz; z++) {
            occ[z * W + x] = true;
            boolean edge = x == x0 || x == x0 + sz - 1 || z == z0 || z == z0 + sz - 1;
            boolean corner = (x == x0 || x == x0 + sz - 1) && (z == z0 || z == z0 + sz - 1);
            for (int y = 0; y <= h; y++) {
                int id = edge ? (corner ? QPILLAR_Y : (y % 10 == 9 ? QB : WB)) : AIR;
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
        // la tour est traversable : une porte sur chaque face le long de la rue (entree d'un cote, sortie de l'autre), au niveau de la chaussee
        int czd = z0 + sz / 2;
        for (int xd : new int[]{x0, x0 + sz - 1}) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 0; y <= 3; y++) set(xd, base + y, czd + dz, AIR);
                set(xd, base + 4, czd + dz, QB);
            }
            for (int dz : new int[]{-2, 2}) for (int y = 0; y <= 3; y++) set(xd, base + y, czd + dz, QB);
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
        java.util.Set<Long> inside = new java.util.HashSet<>();
        for (int[] c : cells) inside.add(((long) c[0] << 32) | (c[1] & 0xffffffffL));
        // la porte : une seule, la cellule de la face tournee vers la rue la plus proche de l'axe, avec un voisin exterieur a l'air libre
        double nx = faceIn ? -rx : rx, nz = faceIn ? -rz : rz;                          // normale sortante de la facade
        int[] doorCell = null; int doorDir = -1; double bestA = 9;
        for (int[] c : cells) {
            int x = c[0], z = c[1];
            double ax = x + 0.5 - hx, az = z + 0.5 - hz;
            double a = ax * tx + az * tz, b = ax * rx + az * rz;
            boolean doorFace = faceIn ? b < -d / 2 + 1.1 : b > d / 2 - 1.1;
            if (!doorFace || Math.abs(a) > 1.6) continue;
            int bestDir = -1; double bestDot = -9;
            int[][] o4 = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};                             // N E S W
            for (int dI = 0; dI < 4; dI++) {
                int xx = x + o4[dI][0], zz = z + o4[dI][1];
                if (inside.contains(((long) xx << 32) | (zz & 0xffffffffL))) continue;
                if (!area(xx, zz) || kind[zz * W + xx] != 1) continue;
                double dot = o4[dI][0] * nx + o4[dI][1] * nz;
                if (dot > bestDot) { bestDot = dot; bestDir = dI; }
            }
            if (bestDir < 0 || bestDot < 0.2) continue;
            if (Math.abs(a) < bestA) { bestA = Math.abs(a); doorCell = c; doorDir = bestDir; }
        }
        int roofX = 0, roofZ = 0, roofH = -1;
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
            boolean door = doorCell != null && doorCell[0] == x && doorCell[1] == z;
            for (int y = 0; y <= hh; y++) {
                int id;
                if (!edge) id = y == 0 ? WB : AIR;
                else if (door && y == 1) id = DOOR + doorDir * 2;
                else if (door && y == 2) id = DOOR + doorDir * 2 + 1;
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
            if (rh > roofH && edge == false) { roofH = rh; roofX = x; roofZ = z; }
            if (edge && !door && ((x + z) % 7 == 3) && rnd(x, z, 9) < 0.5) { set(x, base + hh + 5, z, FENCE_B); }
        }
        if (roofH >= 1 && rnd(ix, iz, 72) < 0.55) {                                       // cheminee fumante
            int ytop = base + hh + 1 + Math.min(roofH, 3);
            set(roofX, ytop, roofZ, SBR); set(roofX, ytop + 1, roofZ, SBR); set(roofX, ytop + 2, roofZ, SBR);
            set(roofX, ytop + 3, roofZ, CAMPF);
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
            if (q < 0.045) { set(x, b, z, rnd(x, z, 44) < 0.5 ? LEAF : OAKLEAF); if (rnd(x, z, 42) < 0.5) set(x, b + 1, z, rnd(x, z, 45) < 0.5 ? LEAF : OAKLEAF); else set(x, b + 1, z, FLOWER1); occ[z * W + x] = true; }
            else if (q < 0.07) { set(x, b, z, rnd(x, z, 43) < 0.5 ? FLOWER1 : FLOWER2); occ[z * W + x] = true; }
            else if (q < 0.082) { set(x, b, z, BARREL); if (rnd(x, z, 46) < 0.4) set(x, b + 1, z, BARREL); occ[z * W + x] = true; }
            else if (q < 0.09) { set(x, b, z, HAY); occ[z * W + x] = true; }
            else if (q < 0.097) { set(x, b, z, rnd(x, z, 47) < 0.5 ? PUMPKIN : MELON); occ[z * W + x] = true; }
            else if (q < 0.1 && freeCellAny(x + 1, z) && freeCellAny(x, z + 1) && freeCellAny(x + 1, z + 1)) {   // petit arbre de ruelle
                for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) occ[(z + dz) * W + x + dx] = true;
                for (int yy = 0; yy < 3; yy++) set(x, b + yy, z, DLOG_Y);
                for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++) for (int dy = 3; dy <= 5; dy++) {
                    double e = Math.pow((dx - 0.5) / 2.2, 2) + Math.pow((dz - 0.5) / 2.2, 2) + Math.pow((dy - 3.8) / 1.5, 2);
                    if (e < 1 && !(dx == 0 && dz == 0 && dy < 4)) setIfAir(x + dx, b + dy, z + dz, rnd(x + dx, dy, z + dz) < 0.5 ? OAKLEAF : LEAF);
                }
            }
        }
        // etals de marche dans la rue, sous une toile de laine
        for (double th = 0.55; th < Math.PI - 0.4; th += 31.0 / (ro - 10)) {
            double rr = ro - 9.7;
            int x0 = (int) Math.round(CX + Math.cos(th) * rr), z0 = (int) Math.round(CZ + Math.sin(th) * rr);
            double tgx = -Math.sin(th), tgz = Math.cos(th);
            int ux = Math.abs(tgx) > Math.abs(tgz) ? (tgx > 0 ? 1 : -1) : 0, uz = ux == 0 ? (tgz > 0 ? 1 : -1) : 0;
            int vx = -uz, vz = ux;                                                         // normale (vers l'exterieur ou l'interieur, peu importe)
            boolean okS = true;
            for (int i = -1; i <= 1 && okS; i++) for (int j = 0; j <= 1; j++) {
                int xx = x0 + ux * i + vx * j, zz = z0 + uz * i + vz * j;
                if (!freeCellAny(xx, zz) || ring[zz * W + xx] != k || nearRamp(xx, zz)) { okS = false; break; }
            }
            if (!okS || rnd(x0, z0, 61) < 0.25) continue;
            int b = FL[k];
            for (int i = -1; i <= 1; i++) for (int j = 0; j <= 1; j++) {
                int xx = x0 + ux * i + vx * j, zz = z0 + uz * i + vz * j;
                occ[zz * W + xx] = true;
                set(xx, b + 3, zz, (i + j + 2) % 2 == 0 ? WOOL_W : WOOL_G);
            }
            for (int i = -1; i <= 1; i += 2) for (int j = 0; j <= 1; j++) { int xx = x0 + ux * i + vx * j, zz = z0 + uz * i + vz * j; if (j == 1) { set(xx, b, zz, FENCE_B); set(xx, b + 1, zz, FENCE_B); set(xx, b + 2, zz, FENCE_B); } }
            for (int i = -1; i <= 1; i++) {
                int xx = x0 + ux * i, zz = z0 + uz * i;                                    // comptoir
                int pick = (int) (rnd(xx, zz, 62) * 4);
                set(xx, b, zz, pick == 0 ? BARREL : pick == 1 ? HAY : pick == 2 ? MELON : PUMPKIN);
                occ[zz * W + xx] = true;
            }
            spawns.add(new int[]{0, x0 + vx, b, z0 + vz});
        }
        // passants dans la rue
        for (int i = 0; i < 4; i++) {
            double th = 0.3 + rnd(k, i, 63) * (Math.PI - 0.6), rr = ro - 10.0;
            int x = (int) Math.round(CX + Math.cos(th) * rr), z = (int) Math.round(CZ + Math.sin(th) * rr);
            if (freeCellAny(x, z) && ring[z * W + x] == k && !nearRamp(x, z)) spawns.add(new int[]{0, x, FL[k], z});
        }
        // etendards de laine pendus au haut du rempart qui domine la rue
        if (k >= 1) for (double th = 0.15; th < Math.PI - 0.15; th += (10.0 + 5 * rnd(k, (int) (th * 100), 64)) / (R[k] - 4)) {
            for (int t = 0; t < 2; t++) {
                double a2 = th + t * 1.0 / R[k];
                int x = (int) Math.round(CX + Math.cos(a2) * (R[k] + 0.7)), z = (int) Math.round(CZ + Math.sin(a2) * (R[k] + 0.7));
                if (!area(x, z) || nearRamp(x, z)) continue;
                for (int j = 0; j < 7; j++) setIfAir(x, FL[k] - 3 - j, z, j == 6 ? WOOL_G : WOOL_W);
            }
        }
    }

    private boolean freeCellAny(int x, int z) { return area(x, z) && kind[z * W + x] == 1 && !occ[z * W + x]; }

    // ------------------------------------------------------------------ rochers, eboulis et arbres : la falaise et l'eperon ne sont plus lisses
    private int boulderBlock(int x, int y, int z) {
        double q = rnd(x, y, z);
        return q < 0.3 ? STONE : q < 0.56 ? ANDESITE : q < 0.7 ? CALCITE : q < 0.82 ? TUFF : q < 0.93 ? DIORITE : COBBLE;
    }

    /** vrai si la cellule est sur ou contre un passage (terrasse, rampe, escalier) : aucun rocher ne doit y tomber */
    private boolean passage(int x, int z) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int xx = x + dx, zz = z + dz;
            if (!area(xx, zz)) continue;
            int kd = kind[zz * W + xx];
            if (kd == 4 || (kd == 1 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1)) return true;
        }
        return false;
    }

    /** bloc de roche anguleux : un pave aux aretes vives, un peu penche et ebreche, a la place des anciennes boules */
    private void blob(double cx, double cy, double cz, double rad) {
        double rx = rad * (0.8 + 0.5 * rnd((int) (cx * 3), 61, (int) (cz * 3))), rz = rad * (0.8 + 0.5 * rnd((int) (cz * 3), 62, (int) (cx * 3))), ry = rad * 0.85;
        double sk = (rnd((int) (cx * 5), 63, (int) (cz * 5)) - 0.5) * 0.6;
        int rr = (int) Math.ceil(Math.max(rx, Math.max(ry, rz))) + 2;
        for (int dx = -rr; dx <= rr; dx++) for (int dy = -rr; dy <= rr; dy++) for (int dz = -rr; dz <= rr; dz++) {
            int bx = (int) Math.round(cx) + dx, by = (int) Math.round(cy) + dy, bz = (int) Math.round(cz) + dz;
            double u = (bx - cx) / rx, w = (by - cy) / ry - sk * (bx - cx) / rx, v = (bz - cz) / rz;
            double e = Math.pow(Math.abs(u), 4) + Math.pow(Math.abs(w), 4) + Math.pow(Math.abs(v), 4) + 0.25 * vn(bx / 1.7, bz / 1.7 + by * 0.5, 58);
            if (e >= 1 || !area(bx, bz) || passage(bx, bz)) continue;
            setIfAir(bx, by, bz, boulderBlock(bx, by, bz));
        }
    }

    private void rocks() {
        // rochers eboules au pied et sur les replats de la montagne
        for (int i = 0; i < 2600; i++) {
            int x = (int) (rnd(i, 1, 2) * W), z = (int) (rnd(i, 3, 4) * D);
            int kd = kind[z * W + x], f = fill[z * W + x];
            if (kd != 2 && kd != 5) continue;
            if (Math.abs(x - CX) < 38 && z > CZ - 32) continue;                              // la citadelle
            if (f > 150 && rnd(i, 5, 6) > 0.04) continue;
            double rad = 1.0 + rnd(i, 7, 8) * 1.7;
            if (rnd(i, 9, 10) < 0.03) rad += 1.2;                                            // de rares gros blocs
            blob(x + 0.5, f + rad * 0.35, z + 0.5, rad);
        }
        // arbres et buissons sur les pentes herbeuses
        for (int i = 0; i < 1400; i++) {
            int x = (int) (rnd(i, 11, 12) * W), z = (int) (rnd(i, 13, 14) * D);
            int kd = kind[z * W + x], f = fill[z * W + x];
            if ((kd != 5 && kd != 2) || f > 78 || get(x, f, z) != GRASS || get(x, f + 1, z) != AIR) continue;
            if (rnd(i, 15, 16) < 0.55) { set(x, f + 1, z, OAKLEAF); if (rnd(i, 17, 18) < 0.5) set(x, f + 2, z, OAKLEAF); continue; }
            int th = 4 + (int) (rnd(i, 19, 20) * 3);
            for (int y = 1; y <= th; y++) set(x, f + y, z, DLOG_Y);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = th - 1; dy <= th + 2; dy++) {
                double e = (dx * dx + dz * dz) / 5.0 + (dy - th - 0.5) * (dy - th - 0.5) / 2.2;
                if (e < 1) setIfAir(x + dx, f + dy, z + dz, OAKLEAF);
            }
        }
        // sur l'eperon : des ressauts de roche qui sortent de la paroi, entre les rues (jamais dans le passage)
        for (int i = 0; i < 520; i++) {
            double dz = 6 + rnd(i, 21, 22) * 92;
            int z = (int) (CZ + dz), side = rnd(i, 23, 24) < 0.5 ? -1 : 1;
            int y = 14 + (int) (rnd(i, 25, 26) * 104);
            if (y % 18 < 14 || y % 18 > 16) continue;
            int hw = (int) prowHW(dz) + 5;
            int xe = -1;
            for (int xx = CX + side * hw; Math.abs(xx - CX) >= 0 && xx != CX; xx -= side) { if (get(xx, y, z) != AIR) { xe = xx; break; } }
            if (xe < 0) continue;
            blob(xe + side * 0.4, y, z + 0.5, 0.9 + rnd(i, 27, 28) * 1.1);
        }
    }

    // ------------------------------------------------------------------ citadelle, tour d'Ecthelion, cour de l'Arbre Blanc
    private void citadel() {
        int cx = CX, cz = CZ, y0 = PROW_TOP;
        tower_x = cx; tower_z = cz - 5; court_x = cx; court_z = cz + 14;
        // parapet sur tout le bord de l'eperon (plateau et lame), et marches blanches la ou la crete descend
        for (int x = cx - 24; x <= cx + 24; x++) for (int z = cz - 12; z <= cz + PROW_TIP + 2; z++) {
            if (!area(x, z) || kind[z * W + x] != 3) continue;
            int f = fill[z * W + x];
            if (f < PROW_TOP - 14 || (z <= cz + 26 && f != PROW_TOP - 1)) continue;
            boolean edge = false;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int xx = x + o[0], zz = z + o[1];
                if (!area(xx, zz) || kind[zz * W + xx] != 3 || fill[zz * W + xx] < f - 1) edge = true;
            }
            if (edge) {
                set(x, f + 1, z, WB);
                if ((x + z) % 2 == 0) set(x, f + 2, z, WB); else set(x, f + 2, z, WB_SLAB);
            }
            if (z > cz + 26 && area(x, z + 1) && kind[(z + 1) * W + x] == 3 && fill[(z + 1) * W + x] == f - 1) set(x, f, z, stair(WB_STAIR, N, false));
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
        towerStair(cx, cz, y0);
    }

    /** escalier en colimacon a l'interieur de la tour : un couloir d'un bloc autour d'un pilier central, de la grande porte jusqu'a la plate-forme sous la lanterne sommitale */
    private void towerStair(int cx, int cz, int y0) {
        int[][] ring = {{0, 2}, {-1, 2}, {-2, 2}, {-2, 1}, {-2, 0}, {-2, -1}, {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, {2, -1}, {2, 0}, {2, 1}, {2, 2}, {1, 2}};
        final int top = 104, last = 105;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean isRing = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
            for (int y = 0; y <= top; y++) set(cx + dx, y0 + y, cz + dz, isRing ? AIR : WB);
        }
        for (int z = 3; z <= 7; z++) for (int dx = -1; dx <= 1; dx++) for (int y = 0; y <= 3; y++) set(cx + dx, y0 + y, cz + z, AIR);   // le couloir d'entree
        for (int n = 1; n <= last; n++) {
            int[] p = ring[n % 16], q = ring[(n + 1) % 16];
            int dx = q[0] - p[0], dz = q[1] - p[1];
            int dir = dz < 0 ? N : dx > 0 ? E : dz > 0 ? S : W_;
            set(cx + p[0], y0 + n - 1, cz + p[1], stair(WB_STAIR, dir, false));
            if (n % 8 == 0 && n + 16 <= last) set(cx + p[0], y0 + n + 14, cz + p[1], HLANTERN);   // suspendue sous la marche du tour suivant
        }
        int[] e = ring[last % 16], e2 = ring[(last - 1) % 16], e3 = ring[(last - 2) % 16];
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if ((dx == e[0] && dz == e[1]) || (dx == e2[0] && dz == e2[1]) || (dx == e3[0] && dz == e3[1])) continue;              // les deux dernieres marches gardent leur plafond haut
            set(cx + dx, y0 + last, cz + dz, WB);                                 // la plate-forme sommitale
        }
        set(cx, y0 + last + 1, cz, LANTERN);
    }

    private void wings(int cx, int cz, int y0) {
        for (int sd = -1; sd <= 1; sd += 2) {
            int xa = sd < 0 ? cx - 21 : cx + 12, xb = sd < 0 ? cx - 12 : cx + 21;
            int za = cz - 13, zb = cz + 3, hh = 20;                                  // les ailes s'arretent avant la rampe du flanc, qui debouche dans la cour
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
            for (int y = 0; y < 3; y++) for (int dz = -1; dz <= 0; dz++) set(xd, y0 + y, cz - 5 + dz, AIR);
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

    /** l'Arbre Blanc : vieux tronc noueux a racines saillantes, branches qui se tordent vers le haut, grosses touffes de feuilles entierement blanches qui pendent un peu */
    private void whiteTree(int tx, int tz, int y0) {
        java.util.Random r = new java.util.Random(7717);
        double ccx = tx + 0.5, ccz = tz + 0.5;
        int h = 11;
        for (int y = 1; y <= h; y++) {
            double t = y / (double) h;
            double rad = y < 3 ? 1.9 : t < 0.55 ? 1.35 : t < 0.8 ? 1.0 : 0.7;
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                double d = Math.hypot(tx + dx - ccx, tz + dz - ccz);
                if (d <= rad) set(tx + dx, y0 + y, tz + dz, WTLOG);
            }
        }
        // racines saillantes
        int roots = 6;
        for (int i = 0; i < roots; i++) {
            double a = (i + r.nextDouble() * 0.6) * Math.PI * 2 / roots;
            double x = Math.cos(a) * 1.6, z = Math.sin(a) * 1.6;
            int y = 1;
            int len = 1 + r.nextInt(2);
            for (int k = 0; k < len; k++) {
                x += Math.cos(a); z += Math.sin(a);
                if (k > 0 && r.nextBoolean()) y--;
                int bx = (int) Math.round(ccx + x - 0.5), bz = (int) Math.round(ccz + z - 0.5);
                if (Math.abs(bx - tx) > 3 || Math.abs(bz - tz) > 3) continue;                    // reste dans le parterre
                set(bx, y0 + Math.max(0, y), bz, WTLOG);
            }
        }
        // branches : surtout vers le sud et les cotes, pour ne pas manger la tour
        java.util.List<double[]> tips = new java.util.ArrayList<>();
        int branches = 8;
        for (int i = 0; i < branches; i++) {
            double a = -0.15 * Math.PI + (i + r.nextDouble() * 0.8) * 1.3 * Math.PI / branches;
            double startT = 0.38 + r.nextDouble() * 0.55;
            int len = 3 + r.nextInt(3);
            double rise = 0.3 + r.nextDouble() * 0.45;
            double x = 0, z = 0, y = h * startT;
            for (int k = 0; k < len; k++) {
                x += Math.cos(a); z += Math.sin(a); y += rise;
                a += (r.nextDouble() - 0.5) * 0.4;
                set((int) Math.round(ccx + x - 0.5), y0 + (int) Math.round(y), (int) Math.round(ccz + z - 0.5), WTLOG);
                if (k > 1 && k % 2 == 0) set((int) Math.round(ccx + x - 0.5), y0 + (int) Math.round(y) - 1, (int) Math.round(ccz + z - 0.5), WTLOG);
            }
            tips.add(new double[]{ccx + x - 0.5, y0 + y + 1, ccz + z - 0.5, 2.3 + r.nextDouble() * 0.6, 0.75});
        }
        tips.add(new double[]{ccx - 0.5, y0 + h + 1, ccz - 0.5, 3.3, 0.8});
        tips.add(new double[]{ccx - 0.5, y0 + h - 3, ccz - 0.5, 2.3, 0.6});
        for (double[] t : tips) {
            int rr = (int) Math.ceil(t[3]) + 1;
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) for (int dy = -rr; dy <= rr; dy++) {
                double e = (dx * dx + dz * dz) / (t[3] * t[3]) + dy * dy / (t[3] * t[4] * t[3] * t[4]);
                int bx = (int) Math.round(t[0]) + dx, by = (int) Math.round(t[1]) + dy, bz = (int) Math.round(t[2]) + dz;
                e += 0.35 * vn(bx / 2.0 + by * 0.4, bz / 2.0, 23) ;
                if (e >= 1) continue;
                setIfAir(bx, by, bz, rnd(bx, by, bz) < 0.3 ? WTBLOOM : WTLEAF);
                // franges pendantes sous la touffe
                if (e > 0.62 && dy < 0 && rnd(bx, by, bz + 5) < 0.22) {
                    int len = 1 + (int) (rnd(bz, by, bx) * 3);
                    for (int j = 1; j <= len; j++) setIfAir(bx, by - j, bz, WTLEAF);
                }
            }
        }
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
        int[][] br = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        whiteTree(tx, tz, y0);
        spawns.add(new int[]{1, cx - 3, y0 + 1, cz - 4}); spawns.add(new int[]{1, cx + 3, y0 + 1, cz - 4});
        spawns.add(new int[]{0, cx - 6, y0 + 1, cz + 12}); spawns.add(new int[]{0, cx + 6, y0 + 1, cz + 14});
        // fontaine : bassin carre, pilier central
        int fx = cx, fz = cz + 6;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
            if (rim) { set(fx + dx, y0, fz + dz, QB); set(fx + dx, y0 - 1, fz + dz, QB); if ((Math.abs(dx) == 4 && Math.abs(dz) == 4)) set(fx + dx, y0 + 1, fz + dz, QUARTZ_SLAB); }
            else { set(fx + dx, y0 - 1, fz + dz, WATER); set(fx + dx, y0 - 2, fz + dz, SQ); set(fx + dx, y0, fz + dz, WATER); }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(fx + dx, y0, fz + dz, QB);
        for (int y = 1; y <= 4; y++) set(fx, y0 + y, fz, QPILLAR_Y);
        set(fx, y0 + 5, fz, QB);
        for (int[] b : br) set(fx + b[0], y0 + 5, fz + b[1], QUARTZ_SLAB);
        set(fx, y0 + 6, fz, GOLD); set(fx, y0 + 7, fz, LANTERN);
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
