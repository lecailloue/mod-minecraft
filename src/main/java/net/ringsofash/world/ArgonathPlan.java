package net.ringsofash.world;

/**
 * Plan de l'Argonath : une grande chaine de collines et de falaises (plus basse que les statues) que traverse une riviere du nord au sud, et deux rois de
 * pierre de 110 blocs, adosses a la falaise sur de petits promontoires, qui regardent vers l'amont (le nord) et levent la main au-dessus de l'eau.
 * Grille de blocs abstraits, sans dependance a Minecraft. Repere local : x de 0 a W - 1 (ouest a est), z de 0 a D - 1 (nord a sud, la riviere coule vers
 * le sud) ; y = 0 : premier niveau d'air au-dessus du sol du centre ; la surface de l'eau est a WATER_Y.
 */
public final class ArgonathPlan {
    public static final int W = 256, D = 256, OFF = 30, TOP = 126, H = OFF + TOP + 1, CX = 128, CZ = 128;
    public static final int WATER_Y = -3;
    public static final int STATUE_TOP = 110, CREST_MAX = 88;

    public static final int AIR = 0, STONE = 1, ANDESITE = 2, TUFF = 3, COBBLE = 4, MOSSC = 5, GRASS = 6, DIRT = 7, GRAVEL = 8, MOSS = 9, POLISHED = 10,
            LOG = 11, LEAVES = 12, BRICKS = 13, MBRICKS = 14, PODZOL = 15, COARSE = 16, SAND = 17;

    private final byte[] g = new byte[W * H * D];
    private final double[] th = new double[W * D];                              // hauteur de la masse rocheuse (>= 2) ou -99
    private final byte[] kind = new byte[W * D];                                // 0 rien, 1 riviere, 2 masse
    private final byte[] depth = new byte[W * D];                               // profondeur de la riviere
    private final int[] top = new int[W * D];
    public int statueW_x, statueE_x, statue_z;

    private static ArgonathPlan inst;
    public static synchronized ArgonathPlan get() { if (inst == null) inst = new ArgonathPlan(); return inst; }

    private static int idx(int x, int y, int z) { return ((y + OFF) * D + z) * W + x; }
    private static boolean ok(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < D && y >= -OFF && y < H - OFF; }
    public byte get(int x, int y, int z) { return ok(x, y, z) ? g[idx(x, y, z)] : AIR; }
    private void set(int x, int y, int z, int id) { if (ok(x, y, z)) g[idx(x, y, z)] = (byte) id; }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int kindAt(int x, int z) { return kind[z * W + x]; }
    public int depthAt(int x, int z) { return depth[z * W + x]; }
    public int top(int x, int z) { return top[z * W + x]; }

    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 362437 + 77;
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

    // ------------------------------------------------------------------ geometrie de la riviere
    /** x de l'axe de la riviere : presque droit, il ondule en s'eloignant des statues */
    private static double axis(int z) { return CX + 12 * vn(z / 60.0, 0.7, 7) * sm(Math.abs(z - CZ) / 40.0); }
    /** demi-largeur de l'eau : etroite et profonde en aval des statues, plus large vers l'amont, arrondie aux deux bouts du plan */
    private static double half(int z) {
        double h = 14;
        if (z < CZ - 10) h += 17 * sm((CZ - 10 - z) / 55.0);
        h += 2 * vn(z / 25.0, 1.3, 8) * sm((z - CZ - 10) / 20.0);
        return h * sm(z / 24.0) * sm((D - 1 - z) / 24.0);
    }
    /** la rive recule d'un coup au nord des statues : un golfe ouvert sur lequel elles regardent */
    private static double bay(int z) { return z < CZ - 7.5 ? 24 * sm((CZ - 7.5 - z) / 2.5) : 0; }

    // ------------------------------------------------------------------ construction
    public ArgonathPlan() {
        terrain();
        materialize();
        trees();
        statue(-1, 0);
        statue(1, 1);
        mossPass();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    private void terrain() {
        for (int z = 0; z < D; z++) {
            double ax = axis(z), hw = half(z), bz = bay(z);
            double zf = sm((z - 8) / 45.0) * (1 - sm((z - (D - 62)) / 54.0));
            for (int x = 0; x < W; x++) {
                int i = z * W + x;
                double u = Math.abs(x + 0.5 - ax), uu = u - hw - bz;
                th[i] = -99;
                boolean ledge = Math.abs(z + 0.5 - CZ) <= 9.5 && u >= hw && u < hw + 20;
                if (ledge) { th[i] = 7; kind[i] = 2; continue; }
                if (uu < 0 && hw > 1.2) {
                    kind[i] = 1;
                    depth[i] = (byte) Math.max(1, Math.round(8 * sm(-uu / 7.0) * sm(hw / 8.0)));
                    continue;
                }
                double ue = Math.max(uu, 0);
                double base = 76 * (0.55 * sm(ue / 4.0) + 0.45 * sm((ue - 3) / 30.0));
                double fall = 1 - sm((ue - 42) / 75.0);
                double ridge = 1 - Math.abs(vn(x / 26.0, z / 26.0, 13));
                double n = 8 * vn(x / 16.0, z / 16.0, 11) + 4.5 * vn(x / 7.0, z / 7.0, 12) + 2.2 * vn(x / 3.3, z / 3.3, 15) + 9 * ridge * ridge * sm(ue / 20.0);
                double rib = 9 * Math.max(0, vn(x / 5.0, z / 12.0, 14)) * sm(ue / 6.0) * (1 - sm((ue - 16) / 12.0))
                        + 6 * Math.max(0, vn(x / 9.0, z / 4.0, 16)) * sm(ue / 6.0) * (1 - sm((ue - 14) / 10.0));
                double lowNear = 1 - 0.24 * Math.exp(-Math.pow((z - CZ) / 42.0, 2));                 // un peu plus bas autour des rois : ils se detachent mieux
                double h = (base + n * sm(ue / 10.0) + rib) * fall * zf * lowNear;
                h = Math.min(h, CREST_MAX);
                if (h >= 2) { th[i] = h; kind[i] = 2; }
            }
        }
    }

    private double thAt(int x, int z) {
        if (x < 0 || x >= W || z < 0 || z >= D) return 0;
        double t = th[z * W + x];
        return t < 0 ? -9 : t;
    }

    private void materialize() {
        for (int z = 0; z < D; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            if (kind[i] != 2) continue;
            int t = (int) Math.floor(th[i]);
            double s = Math.max(Math.max(Math.abs(thAt(x + 1, z) - th[i]), Math.abs(thAt(x - 1, z) - th[i])), Math.max(Math.abs(thAt(x, z + 1) - th[i]), Math.abs(thAt(x, z - 1) - th[i])));
            boolean gentle = s <= 1.7 && t >= 2 && t < 62;
            for (int y = -OFF + 2; y <= t; y++) {
                int dpt = t - y, id;
                double q = rnd(x, y, z);
                if (dpt == 0) id = gentle ? (q < 0.08 ? PODZOL : q < 0.14 ? COARSE : GRASS) : rockMix(x, y, z, s);
                else if (dpt <= 3) id = gentle ? (q < 0.2 ? COARSE : DIRT) : rockMix(x, y, z, s);
                else id = q < 0.12 ? ANDESITE : q < 0.17 ? TUFF : STONE;
                if (kind[i] == 2 && t <= 8 && dpt <= 1 && nearWater(x, z)) id = q < 0.5 ? GRAVEL : q < 0.8 ? COBBLE : STONE;
                set(x, y, z, id);
            }
        }
    }

    private boolean nearWater(int x, int z) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int xx = x + dx, zz = z + dz;
            if (xx >= 0 && xx < W && zz >= 0 && zz < D && kind[zz * W + xx] == 1) return true;
        }
        return false;
    }

    /** pierre des falaises : strates grises en bandes, taches de tuf, de gravier et de mousse */
    private static int rockMix(int x, int y, int z, double s) {
        double q = rnd(x, y, z), band = rnd(x / 5, y / 3, z / 5);
        if (band < 0.18) return q < 0.7 ? ANDESITE : TUFF;
        if (q < 0.04) return GRAVEL;
        if (q < 0.10) return MOSSC;
        if (q < 0.16) return COBBLE;
        if (q < 0.24) return TUFF;
        return q < 0.58 ? STONE : ANDESITE;
    }

    private void trees() {
        for (int z = 6; z < D - 6; z++) for (int x = 6; x < W - 6; x++) {
            int i = z * W + x;
            if (kind[i] != 2) continue;
            int t = (int) Math.floor(th[i]);
            if (t < 4 || t > 62 || get(x, t, z) != GRASS) continue;
            double dens = 0.010 + 0.012 * Math.max(0, vn(x / 22.0, z / 22.0, 21) + 0.3);
            if (rnd(x, 3, z) > dens) continue;
            int h = 7 + (int) (rnd(x, 4, z) * 6);
            for (int k = 1; k <= h; k++) set(x, t + k, z, LOG);
            for (int ly = 2; ly <= h + 2; ly++) {
                double r = 3.0 * (1 - (ly - 2.0) / (h + 1.0)) + (ly % 2 == 0 ? 0.6 : 0);
                int ri = (int) Math.ceil(r);
                for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz > r * r + 0.3) continue;
                    if (get(x + dx, t + ly, z + dz) == AIR) set(x + dx, t + ly, z + dz, LEAVES);
                }
            }
            set(x, t + h + 3, z, LEAVES);
        }
    }

    // ------------------------------------------------------------------ les rois
    /**
     * Un roi de pierre. side : -1 = statue de la rive ouest (le fleuve est a l'est), 1 = rive est. variant 0 : casque ailes, 1 : couronne et barbe.
     * Repere de la statue : sx > 0 vers la riviere, sy hauteur (0 = socle), sz < 0 vers le nord (le visage).
     */
    private void statue(int side, int variant) {
        double hw = half(CZ);
        int xc = (int) Math.round(CX + side * (hw + 10));
        int inner = -side;
        if (side < 0) statueW_x = xc; else statueE_x = xc;
        statue_z = CZ;
        for (int sx = -24; sx <= 24; sx++) for (int sy = 0; sy <= STATUE_TOP; sy++) for (int sz = -10; sz <= 10; sz++) {
            if (!solid(sx + 0.0, sy + 0.5, sz + 0.0, variant)) continue;
            int wx = xc + inner * sx, wz = CZ + sz;
            double q = rnd(wx, sy, wz);
            int id;
            if (sy < 8) id = q < 0.18 ? MBRICKS : BRICKS;
            else if (face(sx, sy, sz, variant)) id = POLISHED;
            else if (groove(sx, sy, sz)) id = q < 0.6 ? ANDESITE : TUFF;
            else id = q < 0.68 ? STONE : q < 0.86 ? ANDESITE : q < 0.95 ? POLISHED : TUFF;
            set(wx, sy, wz, id);
        }
    }

    private static boolean groove(int sx, int sy, int sz) { return sy > 10 && sy < 62 && sz < 0 && Math.floorMod((int) Math.floor((sx + 40) / 2.2), 3) == 0; }
    private static boolean face(int sx, int sy, int sz, int variant) { return sy >= 87 && sy <= 99 && sz < -2; }

    private static boolean solid(double sx, double sy, double sz, int variant) {
        double ax = Math.abs(sx);
        if (sy < 8) return ax <= 9.5 && Math.abs(sz) <= 7.5;
        double a, d;
        if (sy < 62) {
            a = 8.2 - 2.4 * (sy - 8) / 54.0;
            d = 5.8 - 1.0 * (sy - 8) / 54.0;
            if (sz < 0 && sy > 10 && Math.floorMod((int) Math.floor((sx + 40) / 2.2), 3) == 0) d -= 1.2;
            if (Math.pow(ax / a, 3) + Math.pow(Math.abs(sz) / d, 3) <= 1) return true;
        } else if (sy < 87) {
            double[][] prof = {{62, 5.6}, {76, 11.0}, {82, 10.0}, {84, 6.0}, {87, 3.6}};
            a = 3.6;
            for (int k = 0; k < prof.length - 1; k++) if (sy >= prof[k][0] && sy <= prof[k + 1][0]) a = prof[k][1] + (prof[k + 1][1] - prof[k][1]) * (sy - prof[k][0]) / (prof[k + 1][0] - prof[k][0]);
            d = 4.8;
            if (Math.pow(ax / a, 3) + Math.pow(Math.abs(sz) / d, 3) <= 1) return true;
        }
        // tete
        double hy = (sy - 92.5) / 6.5, hx = sx / 4.3, hz = (sz + 0.5) / 4.8;
        if (hx * hx + hy * hy + hz * hz <= 1) return true;
        if (ax <= 0.8 && sz >= -6.0 && sz <= -4.0 && sy >= 89.5 && sy <= 93.5) return true;                       // nez
        if (sy >= 94 && sy <= 95 && ax >= 1.4 && ax <= 3.2 && sz >= -4.9 && sz <= -3.7) return false;                // orbites
        if (variant == 1 && sy >= 79 && sy <= 91 && sz >= -6.2 && sz <= -3.4 && ax <= 1.2 + (sy - 79) * 0.22 && sy < 91) return true;   // barbe
        // casque ailes / couronne
        if (variant == 0) {
            double cy = (sy - 98.0) / 6.5, cx2 = sx / 4.9, cz2 = (sz + 0.5) / 5.0;
            if (sy >= 98 && cx2 * cx2 + cy * cy + cz2 * cz2 <= 1) return true;
            if (ax >= 4 && ax <= 9 && Math.abs(sz) <= 1.3 && sy >= 99 + (ax - 4) * 1.4 && sy <= 105 + (ax - 4) * 1.4) return true;
            if (ax <= 0.8 && sz >= -3 && sz <= 3 && sy >= 103 && sy <= 107) return true;
        } else {
            if (sy >= 98 && sy <= 101 && Math.pow(sx / 4.9, 2) + Math.pow((sz + 0.5) / 5.0, 2) <= 1) return true;
            double[][] sp = {{-3.5, -0.5, 106}, {3.5, -0.5, 106}, {0, -3.5, 106}, {0, 2.5, 106}, {0, -0.5, 109.5}};
            for (double[] s : sp) if (Math.abs(sx - s[0]) <= 0.9 && Math.abs(sz - s[1]) <= 0.9 && sy >= 101 && sy <= s[2]) return true;
        }
        // bras leve (cote riviere) : manche, avant-bras, main ouverte
        if (sx >= 9 && sx <= 18.5 && sy >= 74 && sy <= 83 && Math.abs(sz) <= 3.2) return true;
        if (sx >= 10 && sx <= 18 && sy >= 54 && sy <= 74 && Math.abs(sz) <= 3.0 && sx >= 10 + (74 - sy) * 0.0) return true;
        if (sx >= 14.5 && sx <= 18.5 && sy >= 83 && sy <= 98 && Math.abs(sz) <= 2.2) return true;
        if (sy >= 98 && sy <= 110 && Math.abs(sz) <= 1.0) {
            if (sy <= 103 && sx >= 13.5 && sx <= 19.8) return true;
            double[][] fg = {{13.8, 15.0, 107.5}, {15.4, 16.6, 109.5}, {17.0, 18.2, 108.5}, {18.6, 19.8, 106.5}};
            for (double[] f : fg) if (sx >= f[0] && sx <= f[1] && sy <= f[2]) return true;
            if (sx >= 12.0 && sx <= 13.4 && sy >= 98 && sy <= 102) return true;                                    // pouce
        }
        // bras exterieur : manche tombante, avant-bras replie, poing sur la garde de l'epee plantee devant
        if (sx <= -9 && sx >= -14.5 && sy >= 48 && sy <= 80 && Math.abs(sz) <= 3.2) return true;
        if (sx >= -9 && sx <= -4 && sy >= 60 && sy <= 66 && sz >= -7.5 && sz <= -3) return true;
        if (sx >= -5.5 && sx <= -2 && sy >= 61 && sy <= 65 && sz >= -8.5 && sz <= -5) return true;
        if (sx >= -4.6 && sx <= -2.6 && sz >= -8.2 && sz <= -6.6 && sy >= 22 && sy <= 58) return true;              // lame
        if (sx >= -8.2 && sx <= 1.2 && sz >= -8.4 && sz <= -6.2 && sy >= 58 && sy <= 59.8) return true;             // garde
        if (sx >= -4.2 && sx <= -3.0 && sz >= -7.8 && sz <= -7.0 && sy >= 66 && sy <= 70) return true;              // pommeau
        return false;
    }

    private void mossPass() {
        for (int y = 8; y < STATUE_TOP; y++) for (int z = CZ - 10; z <= CZ + 10; z++) for (int x = 0; x < W; x++) {
            byte id = g[idx(x, y, z)];
            if (id != STONE && id != ANDESITE && id != TUFF && id != BRICKS && id != COBBLE) continue;
            if (g[idx(x, y + 1, z)] == AIR && rnd(x, y, z) < 0.18) g[idx(x, y, z)] = (byte) MOSS;
        }
    }
}
