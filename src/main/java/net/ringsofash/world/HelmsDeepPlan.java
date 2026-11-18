package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan du Gouffre de Helm : 240 x 216 blocs. Une vallee etroite fermee par de hautes falaises au nord, barree a son embouchure par la Muraille profonde (creneaux, deux tours
 * de garde, un caniveau grille, deux escaliers) ; a son extremite est, la grande tour carree, et derriere elle le Burg : une enceinte ronde en trois terrasses adossee a la montagne, avec
 * sa porte au sud, ses tourelles, ses escaliers, et tout en haut la salle a arcades taillee dans la roche. Une longue rampe a parapets descend de la porte vers la plaine.
 * Grille de blocs abstraits, sans dependance a Minecraft.
 * Repere local : x de 0 a W - 1 (ouest a est), z de 0 a D - 1 (nord a sud ; la plaine est au sud) ; y = 0 : premier niveau d'air au-dessus du sol de la plaine (au centre du site).
 */
public final class HelmsDeepPlan {
    public static final int W = 240, D = 216, OFF = 14, TOP = 100, H = OFF + TOP + 1, CX = 120, CZ = 108;
    public static final int BAND = 26;

    public static final int AIR = 0, STONE = 1, ANDESITE = 2, TUFF = 3, COBBLE = 4, MOSSC = 5, GRASS = 6, DIRT = 7, GRAVEL = 8, COARSE = 9, BRICK = 10, MBRICK = 11, CRACK = 12,
            CHISEL = 13, POLAND = 14, SMOOTH = 15, WALLB = 16, LANTERN = 17, HLANT = 18, BARS = 19, DKPLANK = 20, DKFENCE = 21, LADN = 22, LADE = 23, LADS = 24, LADW = 25, REDC = 26,
            PODZOL = 27, SPRLOG = 28, SPRLEAF = 29;
    /** formes : id = SHAPE + famille * 16 + code ; code 0..7 escalier (direction de la montee * 2 + moitie haute ; 0 N, 1 E, 2 S, 3 O), 8 dalle basse, 9 dalle haute ; familles : 0 briques, 1 pierre taillee, 2 cobble */
    public static final int SHAPE = 32, SHAPES = 3;
    public static final int N = 0, E = 1, S_ = 2, W_ = 3;

    // geometrie du site
    private static final int GX = 180, GZ = 80, GR = 33;                 // centre et rayon de l'enceinte du Burg
    private static final int WZ0 = 84, WZ1 = 88, WX0 = 48, WX1 = 137;    // Muraille profonde
    private static final int T1 = 8, T2 = 16, T3 = 24, RING = 17;        // sommets (derniere couche solide) des terrasses et du chemin de ronde

    private final byte[] g = new byte[W * H * D];
    private final int[] th = new int[W * D];
    private final boolean[] built = new boolean[W * D];
    private final byte[] par = new byte[W * D];
    private final byte[] edge = new byte[W * D];
    private final int[] top = new int[W * D];
    private final float[] dist = new float[W * D];
    public final List<int[]> spawns = new ArrayList<>();         // {x, y, z}

    private static HelmsDeepPlan inst;
    public static synchronized HelmsDeepPlan get() { if (inst == null) inst = new HelmsDeepPlan(); return inst; }

    // ------------------------------------------------------------------ outils
    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public byte get(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int top(int x, int z) { return top[z * W + x]; }
    public int terrain(int x, int z) { return th[z * W + x]; }
    public int weight(int x, int z) { return edge[z * W + x] & 255; }

    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 362437 + 91;
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
    private static int stair(int fam, int dir, boolean topHalf) { return SHAPE + fam * 16 + dir * 2 + (topHalf ? 1 : 0); }
    private static int slab(int fam, boolean topHalf) { return SHAPE + fam * 16 + (topHalf ? 9 : 8); }

    private void box(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, id);
    }
    /** briques : surtout neuves, quelques-unes moussues ou fendues */
    private int brickAt(int x, int y, int z) {
        double r = rnd(x, y * 7 + 3, z);
        return r < 0.07 ? MBRICK : r < 0.14 ? CRACK : BRICK;
    }
    private void bricks(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, brickAt(x, y, z));
    }

    // ------------------------------------------------------------------ construction
    public HelmsDeepPlan() {
        terrain();
        fortress();
        materialize();
        wall();
        tower();
        ramp();
        gate();
        hall();
        keepers();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
            double e = Math.min(Math.min(x, W - 1 - x), Math.min(z, D - 1 - z));
            edge[z * W + x] = (byte) Math.round(255 * sm(e / BAND));
        }
    }

    // ------------------------------------------------------------------ relief
    private static boolean valley(int x, int z) {
        if (z < 20 || z >= 100) return false;
        double k = sm((60 - z) / 40.0);
        return x >= 52 + 34 * k && x <= 150 - 34 * k;
    }
    private static boolean circle(int x, int z, double r) { double dx = x - GX, dz = z - GZ; return dx * dx + dz * dz <= r * r; }
    private static double mound(int x, int z) {
        return 56 * sm((67.5 - z) / 2.5) * (1 - sm((Math.abs(x - GX) - 27) / 9.0));
    }

    private void terrain() {
        // distance a l'espace ouvert (vallee, plaine, enceinte) : sert a elever les falaises
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) dist[z * W + x] = (z >= 100 || valley(x, z) || circle(x, z, GR + 1)) ? 0f : 999f;
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x; float d = dist[i];
            if (x > 0) d = Math.min(d, dist[i - 1] + 1f);
            if (z > 0) { d = Math.min(d, dist[i - W] + 1f); if (x > 0) d = Math.min(d, dist[i - W - 1] + 1.414f); if (x < W - 1) d = Math.min(d, dist[i - W + 1] + 1.414f); }
            dist[i] = d;
        }
        for (int z = D - 1; z >= 0; z--) for (int x = W - 1; x >= 0; x--) {
            int i = z * W + x; float d = dist[i];
            if (x < W - 1) d = Math.min(d, dist[i + 1] + 1f);
            if (z < D - 1) { d = Math.min(d, dist[i + W] + 1f); if (x < W - 1) d = Math.min(d, dist[i + W + 1] + 1.414f); if (x > 0) d = Math.min(d, dist[i + W - 1] + 1.414f); }
            dist[i] = d;
        }
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            double d = dist[i], hr = 0;
            if (d > 0) {
                double base = 68 * (0.62 * sm(d / 14.0) + 0.38 * sm((d - 6) / 55.0));
                double ridge = 1 - Math.abs(vn(x / 24.0, z / 24.0, 13));
                double n = (7 * vn(x / 17.0, z / 17.0, 11) + 3.5 * vn(x / 7.0, z / 7.0, 12) + 1.0 * vn(x / 3.0, z / 3.0, 15) + 9 * ridge * ridge) * sm(d / 12.0);
                hr = base + n + 14 * sm((46 - z) / 46.0) * sm(d / 20.0);
                hr *= sm(Math.min(Math.min(x, W - 1 - x), z) / 30.0);
            }
            double m = mound(x, z);
            if (m > 0.5) hr = Math.max(hr, m + 2 * vn(x / 5.0, z / 5.0, 21) * sm(m / 8.0));
            hr = Math.min(hr, 88);
            th[i] = hr >= 1 ? (int) Math.round(hr) - 1 : -1;
        }
    }

    // ------------------------------------------------------------------ le Burg : terrasses, escaliers, parapets
    private void fortress() {
        boolean[] ringOut = new boolean[W * D], turret = new boolean[W * D], stairCol = new boolean[W * D];
        int[] stairF = new int[W * D];
        double[] ang = {30, 60, 125, 150, 180, 212};
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            double dx = x - GX, dz = z - GZ, d = Math.hypot(dx, dz);
            if (d > GR || mound(x, z) >= 0.5) continue;
            int i = z * W + x, f;
            boolean pl = d < 12 || (Math.abs(dx) <= 16 && z >= 68 && z <= 84);
            if (d >= 29.5) { f = RING; if (d >= 32.5) ringOut[i] = true; }
            else if (d >= 22) f = T1;
            else if (pl) f = T3;
            else f = T2;
            if (Math.abs(dx) <= 2 && dz > 0 && d < 29.5) {                                        // escaliers de l'axe sud : vers la terrasse suivante
                if (d >= 22) { f = T1 + (int) Math.floor((29.5 - d) * 8 / 7.5); stairCol[i] = true; }
                else if (d >= 12) { f = T2 + (int) Math.floor((22 - d) * 8 / 10.0); stairCol[i] = true; }
                stairF[i] = f;
            }
            for (double a : ang) {                                                                  // tourelles rondes sur le chemin de ronde
                double ax = GX + 33 * Math.cos(Math.toRadians(a)), az = GZ + 33 * Math.sin(Math.toRadians(a));
                double dd = Math.hypot(x - ax, z - az);
                if (dd <= 3.6) { f = 23; if (dd >= 2.6) { turret[i] = true; } }
            }
            th[i] = f; built[i] = true;
        }
        // les tourelles debordent un peu de l'enceinte : colonnes hors du cercle
        for (double a : ang) {
            double ax = GX + 33 * Math.cos(Math.toRadians(a)), az = GZ + 33 * Math.sin(Math.toRadians(a));
            for (int z = (int) az - 4; z <= (int) az + 4; z++) for (int x = (int) ax - 4; x <= (int) ax + 4; x++) {
                if (!area(x, z)) continue;
                double dd = Math.hypot(x - ax, z - az);
                int i = z * W + x;
                if (dd <= 3.6 && !built[i] && mound(x, z) < 0.5) { th[i] = 23; built[i] = true; if (dd >= 2.6) turret[i] = true; }
            }
        }
        // parapets : toute colonne batie qui domine de 3 blocs ou plus un voisin
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            if (!built[i]) continue;
            int f = th[i];
            boolean low = false;
            int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] n : nb) {
                int xx = x + n[0], zz = z + n[1];
                int tn = area(xx, zz) ? th[zz * W + xx] : -1;
                if (tn <= f - 3) low = true;
            }
            if (!low) continue;
            par[i] = (byte) ((ringOut[i] || turret[i]) ? 2 : 1);
        }
        this.stairCol = stairCol; this.stairF = stairF;
    }
    private boolean[] stairCol;
    private int[] stairF;

    // ------------------------------------------------------------------ matiere du relief
    private int natural(int x, int y, int z) {
        double v = vn(x / 7.0 + y / 9.0, z / 7.0 + y / 13.0, 21);
        return v > 0.5 ? ANDESITE : v < -0.55 ? TUFF : STONE;
    }

    private void materialize() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x, t = th[i];
            boolean b = built[i];
            for (int y = -OFF; y < t; y++) set(x, y, z, b ? brickAt(x, y, z) : natural(x, y, z));
            if (b) {
                double r = rnd(x, 5, z);
                set(x, t, z, stairCol[i] ? stair(0, N, false) : r < 0.22 ? COBBLE : r < 0.34 ? GRAVEL : r < 0.5 ? POLAND : brickAt(x, t, z));
                if (par[i] == 1) set(x, t + 1, z, WALLB);
                else if (par[i] == 2) {
                    set(x, t + 1, z, BRICK);
                    boolean mer = ((int) Math.floor(Math.atan2(z - GZ, x - GX) * 33.0 / 1.8) & 1) == 0 || ((x + z) & 1) == 0 && Math.hypot(x - GX, z - GZ) > 33.4;
                    if (mer) set(x, t + 2, z, BRICK);
                }
                continue;
            }
            // surface naturelle
            boolean gentle = true;
            int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] n : nb) { int xx = x + n[0], zz = z + n[1]; if (area(xx, zz) && Math.abs(th[zz * W + xx] - t) > 1) gentle = false; }
            double dd = dist[i];
            if (t < 0) {                                                                      // plaine et fond de vallee
                double r = rnd(x, 9, z);
                set(x, -1, z, r < 0.06 ? COARSE : r < 0.09 && dd < 3 ? GRAVEL : GRASS);
                for (int y = -4; y <= -2; y++) set(x, y, z, DIRT);
            } else if (gentle && t < 24) {
                set(x, t, z, GRASS); for (int y = t - 3; y < t; y++) set(x, y, z, DIRT);
            } else if (t < 10 && dd < 12 && rnd(x, 6, z) < 0.5) {
                set(x, t, z, GRAVEL);
            } else {
                set(x, t, z, natural(x, t, z) == STONE && rnd(x, 7, z) < 0.18 ? COBBLE : natural(x, t, z));
            }
        }
    }

    // ------------------------------------------------------------------ la Muraille profonde
    private void wall() {
        // pied ouest enfonce dans la roche
        box(WX0 - 4, -1, WZ0 - 2, WX0 + 4, 24, WZ1 + 2, STONE);
        bricks(WX0, -1, WZ0, WX1, 14, WZ1);
        for (int x = WX0; x <= WX1; x++) {
            for (int z = WZ0 + 1; z <= WZ1 - 1; z++) set(x, 14, z, ((x + z) & 3) == 0 ? POLAND : BRICK);
            set(x, 15, WZ1, BRICK);
            if ((x & 3) < 2) set(x, 16, WZ1, BRICK);
            set(x, 15, WZ0, WALLB);
            if (x % 12 == 6) set(x, 16, WZ0, LANTERN);
            for (int y = -1; y <= 3; y++) set(x, y, WZ1 + 1, ((x + y) & 1) == 0 ? BRICK : CRACK);          // empattement
        }
        // escaliers sur la face nord
        for (int[] fl : new int[][]{{58, 60}, {126, 128}}) {
            for (int z = 69; z <= 83; z++) {
                int y = z - 69;
                for (int x = fl[0] - 1; x <= fl[1] + 1; x++) {
                    box(x, -1, z, x, y - 1, z, BRICK);
                    if (x == fl[0] - 1 || x == fl[1] + 1) { set(x, y, z, BRICK); set(x, y + 1, z, WALLB); }
                    else set(x, y, z, stair(0, S_, false));
                }
            }
            for (int x = fl[0]; x <= fl[1]; x++) set(x, 15, WZ0, AIR);
            for (int x = fl[0]; x <= fl[1]; x++) { set(x, 15, WZ0, AIR); set(x, 14, WZ0, POLAND); }
        }
        // deux tours de garde : un passage couvert les traverse au niveau du chemin de ronde
        for (int cx : new int[]{72, 108}) {
            bricks(cx - 3, -1, WZ0 - 2, cx + 3, 23, WZ1 + 4);
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = WZ0 - 2; z <= WZ1 + 4; z++) {
                boolean rim = x == cx - 3 || x == cx + 3 || z == WZ0 - 2 || z == WZ1 + 4;
                set(x, 24, z, POLAND);
                if (rim) { set(x, 25, z, BRICK); if (((x + z) & 1) == 0) set(x, 26, z, BRICK); }
            }
            box(cx - 3, 15, WZ0 + 1, cx + 3, 17, WZ1 - 1, AIR);
            for (int x = cx - 3; x <= cx + 3; x++) set(x, 18, WZ0 + 1, CHISEL);
            for (int s = 0; s < 2; s++) {                                                          // meurtrieres
                int zz = WZ1 + 4; int xx = cx - 1 + s * 2;
                box(xx, 19, zz, xx, 20, zz, AIR); box(xx, 8, zz, xx, 9, zz, AIR);
            }
            set(cx, 24 + 1, WZ0 - 2, LANTERN);
            spawns.add(new int[]{cx, 15, WZ0 + 2});
        }
        // caniveau grille
        box(89, 0, WZ0, 91, 2, WZ1 + 1, AIR); set(90, 3, WZ0, AIR); box(90, 3, WZ0, 90, 3, WZ1 + 1, AIR);
        box(89, 0, WZ1 + 1, 91, 3, WZ1 + 1, BARS);
        box(89, -1, WZ0, 91, -1, WZ1 + 1, GRAVEL);
        for (int x = WX0 + 8; x < WX1 - 4; x += 14) spawns.add(new int[]{x, 15, WZ0 + 2});
    }

    // ------------------------------------------------------------------ la grande tour carree
    private void tower() {
        int x0 = 138, x1 = 146, z0 = 78, z1 = 86;
        bricks(x0 - 1, -1, z0 - 1, x1 + 1, 5, z1 + 1);
        for (int y = -1; y <= 56; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean w = x <= x0 + 1 || x >= x1 - 1 || z <= z0 + 1 || z >= z1 - 1;
            if (w) set(x, y, z, brickAt(x, y, z));
            else set(x, y, z, AIR);
        }
        for (int fy : new int[]{14, 28, 42}) {
            for (int x = x0 + 2; x <= x1 - 2; x++) for (int z = z0 + 2; z <= z1 - 2; z++) set(x, fy, z, POLAND);
            set(142, fy, z0 + 2, AIR);
        }
        for (int y = 0; y <= 57; y++) set(142, y, z0 + 2, LADS);
        // toit-terrasse
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            set(x, 56, z, (x == 142 && z == z0 + 2) ? AIR : POLAND);
            boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
            if (rim) { set(x, 57, z, BRICK); if (((x + z) & 1) == 0) set(x, 58, z, BRICK); }
        }
        for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) { box(c[0], 57, c[1], c[0], 61, c[1], CHISEL); set(c[0], 62, c[1], LANTERN); }
        // portes : au pied (face sud) et sur le chemin de ronde de la muraille (face ouest)
        box(142, 0, z1 - 1, 142, 2, z1, AIR);
        box(141, 0, z1 - 1, 141, 2, z1 - 1, AIR); box(143, 0, z1 - 1, 143, 2, z1 - 1, AIR);
        box(x0, 15, 85, x0 + 1, 16, 85, AIR);
        set(142, 3, z1, CHISEL); set(141, 3, z1, CHISEL); set(143, 3, z1, CHISEL);
        set(141, 2, z1 + 1, LANTERN); set(143, 2, z1 + 1, LANTERN);
        // meurtrieres
        for (int l : new int[]{7, 21, 35, 49}) {
            box(142, l, z0, 142, l + 1, z0 + 1, AIR); box(142, l, z1 - 1, 142, l + 1, z1, AIR);
            box(x0, l, 82, x0 + 1, l + 1, 82, AIR); box(x1 - 1, l, 82, x1, l + 1, 82, AIR);
        }
        // bandeaux de pierre taillee et eclairage
        for (int y : new int[]{13, 27, 41, 55}) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (x == x0 || x == x1 || z == z0 || z == z1) set(x, y, z, CHISEL);
        for (int fy : new int[]{14, 28, 42, 56}) set(142, fy + 3, 84, HLANT);
        spawns.add(new int[]{140, 57, 84}); spawns.add(new int[]{144, 57, 80}); spawns.add(new int[]{142, 15, 82});
    }

    // ------------------------------------------------------------------ rampe de la porte vers la plaine
    private void ramp() {
        double[][] pts = {{180, 114}, {180, 126}, {172, 138}, {156, 146}, {140, 148}, {128, 154}};
        double total = 0;
        for (int s = 0; s + 1 < pts.length; s++) total += Math.hypot(pts[s + 1][0] - pts[s][0], pts[s + 1][1] - pts[s][1]);
        double[] best = new double[W * D], hd = new double[W * D];
        java.util.Arrays.fill(best, 99);
        double acc = 0;
        for (int s = 0; s + 1 < pts.length; s++) {
            double len = Math.hypot(pts[s + 1][0] - pts[s][0], pts[s + 1][1] - pts[s][1]);
            for (double t = 0; t <= len; t += 0.4) {
                double px = pts[s][0] + (pts[s + 1][0] - pts[s][0]) * t / len, pz = pts[s][1] + (pts[s + 1][1] - pts[s][1]) * t / len;
                double h = 8.0 - 8.6 * (acc + t) / total;
                for (int x = (int) px - 4; x <= (int) px + 4; x++) for (int z = (int) pz - 4; z <= (int) pz + 4; z++) {
                    if (!area(x, z)) continue;
                    double dd = Math.hypot(x + 0.5 - px, z + 0.5 - pz);
                    int i = z * W + x;
                    if (dd <= 3.4 && dd < best[i]) { best[i] = dd; hd[i] = h; }
                }
            }
            acc += len;
        }
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            if (best[i] > 3.4) continue;
            if (built[i]) continue;
            double h = hd[i];
            int ft = (int) Math.floor(h);
            boolean sl = h - ft >= 0.5;
            for (int y = Math.min(0, ft) - 1; y < ft; y++) set(x, y, z, brickAt(x, y, z));
            set(x, ft, z, ((x + z) & 3) == 0 ? POLAND : BRICK);
            int up = ft + 1;
            if (sl) { set(x, up, z, slab(0, false)); up++; }
            else for (int y = up; y <= up + 2; y++) if (get(x, y, z) != AIR && y > ft) set(x, y, z, AIR);
            if (best[i] > 2.5) {
                set(x, up, z, WALLB);
                if (x % 7 == 0 && z % 2 == 0) set(x, up + 1, z, LANTERN);
            }
        }
    }

    // ------------------------------------------------------------------ la porte du Burg
    private void gate() {
        box(178, T1 + 1, 108, 182, T1 + 5, 114, AIR);
        for (int z = 110; z <= 113; z++) for (int x = 178; x <= 182; x++) set(x, T1, z, POLAND);
        for (int z = 110; z <= 113; z++) for (int y = T1 + 1; y <= T1 + 6; y++) { set(177, y, z, CHISEL); set(183, y, z, CHISEL); }
        for (int x = 177; x <= 183; x++) { set(x, T1 + 6, 113, CHISEL); set(x, T1 + 7, 113, CHISEL); }
        box(178, T1 + 5, 113, 182, T1 + 5, 113, BARS);
        set(180, T1 + 5, 111, HLANT);
        set(177, T1 + 8, 114, LANTERN); set(183, T1 + 8, 114, LANTERN);
        spawns.add(new int[]{178, T1 + 1, 107}); spawns.add(new int[]{182, T1 + 1, 107});
    }

    // ------------------------------------------------------------------ la salle a arcades, taillee dans la roche
    private void hall() {
        // coque de briques autour de la salle (la pierre nue y attirerait des sources de lave) et socle plein sous la facade
        bricks(165, -OFF, 66, 195, 23, 67);
        bricks(165, 24, 55, 195, 34, 55); bricks(165, 24, 55, 165, 34, 65); bricks(195, 24, 55, 195, 34, 65); bricks(165, 34, 55, 195, 34, 67);
        bricks(165, 24, 55, 195, 24, 65);
        box(166, 25, 56, 194, 33, 65, AIR);
        box(166, 24, 56, 194, 24, 65, POLAND);
        box(165, 24, 66, 195, 24, 67, POLAND);
        // facade : mur plein de 25 a 36, percee de cinq arcades en gradins
        bricks(165, 25, 66, 195, 36, 67);
        for (int p = 165; p <= 195; p += 6) box(p, 25, 66, p, 36, 67, CHISEL);
        for (int p = 165; p < 195; p += 6) {
            int a = p + 1;
            box(a, 25, 66, a + 4, 28, 67, AIR);
            box(a + 1, 29, 66, a + 3, 29, 67, AIR);
            box(a + 2, 30, 66, a + 2, 30, 67, AIR);
        }
        for (int x = 165; x <= 195; x++) { set(x, 36, 66, CHISEL); set(x, 36, 67, CHISEL); set(x, 37, 66, slab(0, false)); set(x, 37, 67, slab(0, false)); set(x, 37, 68, slab(0, false)); }
        for (int x = 165; x <= 195; x++) set(x, 24, 68, POLAND);
        for (int x = 165; x <= 195; x++) { set(x, 25, 68, AIR); set(x, 25, 69, AIR); }          // pas de garde-corps devant les arcades
        // piliers et lumieres
        for (int x : new int[]{171, 177, 183, 189}) box(x, 25, 61, x, 33, 61, CHISEL);
        for (int x : new int[]{174, 180, 186}) set(x, 33, 61, HLANT);
        for (int x = 166; x <= 194; x += 7) set(x, 33, 58, HLANT);
        // tapis et estrade
        for (int z = 59; z <= 66; z++) { set(179, 25, z, REDC); set(180, 25, z, REDC); set(181, 25, z, REDC); }
        for (int x = 177; x <= 183; x++) for (int z = 56; z <= 58; z++) set(x, 25, z, slab(0, false));
        spawns.add(new int[]{176, 25, 62}); spawns.add(new int[]{184, 25, 62});
    }

    private void keepers() {
        // gardes du chemin de ronde de l'enceinte et des terrasses
        for (double a : new double[]{20, 70, 110, 160, 200, 240}) {
            int x = (int) Math.round(GX + 31 * Math.cos(Math.toRadians(a))), z = (int) Math.round(GZ + 31 * Math.sin(Math.toRadians(a)));
            if (area(x, z) && built[z * W + x] && mound(x, z) < 0.5) spawns.add(new int[]{x, RING + 1, z});
        }
        for (double a : new double[]{0, 90, 180, 270}) {
            int x = (int) Math.round(GX + 26 * Math.cos(Math.toRadians(a))), z = (int) Math.round(GZ + 26 * Math.sin(Math.toRadians(a)));
            if (area(x, z) && built[z * W + x] && mound(x, z) < 0.5) spawns.add(new int[]{x, T1 + 1, z});
        }
        spawns.add(new int[]{GX + 6, T3 + 1, GZ - 2}); spawns.add(new int[]{GX - 6, T3 + 1, GZ - 2});
    }
}
