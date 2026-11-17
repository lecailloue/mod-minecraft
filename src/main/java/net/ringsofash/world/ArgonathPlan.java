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
    /** formes (escaliers, dalles) de la pierre des statues : id = SHAPE + matiere * 16 + code ; code 0..7 escalier (direction * 2 + moitie), 8 dalle basse, 9 dalle haute ; matieres : 0 pierre, 1 andesite, 2 andesite polie */
    public static final int SHAPE = 32, SHAPES = 3;
    public static boolean isShape(int id) { return id >= SHAPE; }

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
        double hwc = half(CZ);
        for (int z = 0; z < D; z++) {
            double ax = axis(z), hw = half(z), bz = bay(z);
            double zf = sm((z - 8) / 45.0) * (1 - sm((z - (D - 62)) / 54.0));
            for (int x = 0; x < W; x++) {
                int i = z * W + x;
                double u = Math.abs(x + 0.5 - ax);
                th[i] = -99;
                // promontoire sous chaque roi : un affleurement de roche irregulier, plus haut au centre, qui plonge dans l'eau
                double cu = hwc + 11, du = (u - cu) / 14.0, dz = (z + 0.5 - CZ) / 13.0;
                double r2 = (du * du + dz * dz) * (1 + 0.35 * vn(x / 5.0, z / 5.0, 31));
                if (r2 < 1 && Math.abs(x + 0.5 - ax) > 0) {
                    th[i] = 4 + 9 * Math.sqrt(1 - r2) + 2.5 * vn(x / 3.0, z / 3.0, 32) * (1 - r2);
                    kind[i] = 2;
                    continue;
                }
                double edge = hw + bz + 3 * vn(z / 9.0, 0.4, 33) * (bz > 0 ? 1 : 0);
                if (u < edge && hw > 1.2) {
                    kind[i] = 1;
                    depth[i] = (byte) Math.max(1, Math.round(8 * sm((edge - u) / 7.0) * sm(hw / 8.0)));
                    continue;
                }
                // profil de la masse : continu avec la distance a l'eau, la rive qui recule n'y ajoute qu'une entaille
                double ue = Math.max(u - hw, 0);
                double ue2 = Math.max(0, ue + 5 * vn(x / 12.0, z / 12.0, 41) + 2.5 * vn(x / 5.0, z / 5.0, 42));
                double base = 76 * (0.42 * sm(ue2 / 8.0) + 0.58 * sm((ue2 - 2) / 46.0));
                double fall = 1 - sm((ue - 42) / 75.0);
                double ridge = 1 - Math.abs(vn(x / 26.0, z / 26.0, 13));
                double n = 8 * vn(x / 16.0, z / 16.0, 11) + 4.5 * vn(x / 7.0, z / 7.0, 12) + 2.2 * vn(x / 3.3, z / 3.3, 15) + 9 * ridge * ridge * sm(ue / 20.0);
                double lowNear = 1 - 0.24 * Math.exp(-Math.pow((z - CZ) / 42.0, 2));                 // un peu plus bas autour des rois : ils se detachent mieux
                double h = (base + n * sm(ue / 10.0)) * fall * zf * lowNear;
                h = Math.max(h, 4 * Math.min(1, fall * 3) * Math.min(1, zf * 4));                     // pas de bande vide entre l'eau et la masse
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
            if (Math.abs(z + 0.5 - CZ) < 17 && Math.abs(x - CX) < 46) continue;
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
     * Un roi de pierre vetu d'un long drap, le bras tendu vers la riviere, la paume ouverte vers le nord. Le corps est modele a demi-bloc : chaque bloc est echantillonne
     * en 8 sous-cubes, puis devient un bloc plein, un escalier ou une dalle (formes lissees) ; les details fins (main, epee, pointes) sont modeles au bloc.
     * side : -1 = rive ouest (le fleuve est a l'est), 1 = rive est. variant 0 : casque aile, 1 : couronne et barbe.
     * Repere de la statue : sx > 0 vers la riviere, sy hauteur, sz < 0 vers le nord (le visage).
     */
    private void statue(int side, int variant) {
        double hw = half(CZ);
        int xc = (int) Math.round(CX + side * (hw + 11));
        int inner = -side;
        if (side < 0) statueW_x = xc; else statueE_x = xc;
        statue_z = CZ;
        for (int wx = xc - 27; wx <= xc + 27; wx++) for (int y = 6; y <= STATUE_TOP; y++) for (int wz = CZ - 11; wz <= CZ + 11; wz++) {
            double bsx = inner * ((wx - xc) + 0.5), bsy = y + 0.5, bsz = (wz - CZ) + 0.5;
            if (carve(bsx, bsy, bsz, variant)) { set(wx, y, wz, AIR); continue; }
            boolean fineB = fine(bsx, bsy, bsz, variant);
            int mask = 0;
            for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) for (int k = 0; k < 2; k++) {
                double sx = inner * ((wx - xc) + 0.25 + 0.5 * i), sy = y + 0.25 + 0.5 * j, sz = (wz - CZ) + 0.25 + 0.5 * k;
                if (fineB || body(sx, sy, sz, variant)) mask |= 1 << (i + 2 * j + 4 * k);
            }
            if (mask == 0) continue;
            double q = rnd(wx, y, wz);
            int mat = (y >= 87 && y <= 99 && bsz < -2) ? 2 : q < 0.62 ? 0 : q < 0.86 ? 1 : 2;
            int id = shapeOf(mask, mat, q);
            if (id == AIR) continue;
            int cur = get(wx, y, wz);
            if (cur != AIR && !isShape(cur)) { if (isShape(id)) continue; }
            set(wx, y, wz, id);
        }
    }

    private static int shapeOf(int mask, int mat, double q) {
        int n = Integer.bitCount(mask), bot = Integer.bitCount(mask & 0x33), tp = Integer.bitCount(mask & 0xCC);
        if (n == 8) return mat == 0 ? STONE : mat == 1 ? ANDESITE : q < 0.5 ? POLISHED : TUFF;
        int m = SHAPE + mat * 16;
        if (mask == 0x33) return m + 8;
        if (mask == 0xCC) return m + 9;
        if ((mask & 0x33) == 0x33 && tp == 2) {                               // escalier a l'endroit : la partie haute est a l'arriere
            int hi = mask & 0xCC;
            if (hi == 0xC0) return m + 0 * 2;       // ouvert au nord
            if (hi == 0x0C) return m + 2 * 2;       // ouvert au sud
            if (hi == 0x88) return m + 3 * 2;       // ouvert a l'ouest
            if (hi == 0x44) return m + 1 * 2;       // ouvert a l'est
        }
        if ((mask & 0xCC) == 0xCC && bot == 2) {                              // escalier a l'envers
            int lo = mask & 0x33;
            if (lo == 0x30) return m + 0 * 2 + 1;
            if (lo == 0x03) return m + 2 * 2 + 1;
            if (lo == 0x22) return m + 3 * 2 + 1;
            if (lo == 0x11) return m + 1 * 2 + 1;
        }
        if (n >= 5) return mat == 0 ? STONE : mat == 1 ? ANDESITE : POLISHED;
        if (bot >= 3) return m + 8;
        if (tp >= 3) return m + 9;
        return AIR;
    }

    private static double sg(double a, double b) { return Math.pow(Math.abs(a), 2.6) + Math.pow(Math.abs(b), 2.6); }

    /** le corps, a demi-bloc : robe longue plissee, torse et epaules drapes, cape, tete, bras tendu et son long drap, manche du bras replie */
    private static boolean body(double sx, double sy, double sz, int variant) {
        double ax = Math.abs(sx);
        if (sy < 6) return false;
        if (sy < 64) {                                                    // robe : evasee en bas, plissee devant
            double k = 1 - (sy - 6) / 58.0;
            double a = 5.8 + 3.4 * k * k, d = 5.0 + 1.0 * k * k;
            if (sz < 0) d -= 0.9 * (0.5 + 0.5 * Math.sin(sx * 1.5 + sy * 0.03));
            if (sg(ax / a, sz / d) <= 1) return true;
        } else if (sy < 87) {                                             // torse, epaules couvertes d'un drap
            double[][] prof = {{64, 5.8}, {72, 7.6}, {80, 10.2}, {83, 9.4}, {85.5, 5.5}, {87, 3.6}};
            double a = 3.6;
            for (int k = 0; k < prof.length - 1; k++) if (sy >= prof[k][0] && sy <= prof[k + 1][0]) a = prof[k][1] + (prof[k + 1][1] - prof[k][1]) * (sy - prof[k][0]) / (prof[k + 1][0] - prof[k][0]);
            double d = 5.0 + 0.4 * (sy - 64) / 23.0;
            if (sg(ax / a, sz / d) <= 1) return true;
        }
        if (sy >= 26 && sy < 80 && sz > 0 && sz <= 8.4 && ax <= 6.2 + 3.0 * (sy - 26) / 54.0) return true;       // la cape dans le dos, qui rejoint la falaise
        // tete
        double hy = (sy - 93) / 6.0, hx = sx / 4.2, hz = (sz + 0.3) / 4.6;
        if (hx * hx + hy * hy + hz * hz <= 1) return true;
        if (variant == 1 && sy >= 80 && sy < 91 && sz >= -5.9 && sz <= -3.4 && ax <= 1.0 + (sy - 80) * 0.28 && ax <= 3.2) return true;    // barbe
        if (variant == 0) {
            double cy = (sy - 98.5) / 6.0, cx2 = sx / 4.9, cz2 = (sz + 0.4) / 5.0;
            if (sy >= 98.5 && cx2 * cx2 + cy * cy + cz2 * cz2 <= 1) return true;
        } else if (sy >= 98.5 && sy <= 101.5 && Math.pow(sx / 4.9, 2) + Math.pow((sz + 0.4) / 5.0, 2) <= 1) return true;
        // bras tendu vers la riviere, un peu en avant : un cylindre de l'epaule au poignet
        double[] S = {9.0, 77.0, 0.0}, Wr = {19.5, 93.5, -2.5};
        if (sx > 5) {
            double vx = Wr[0] - S[0], vy = Wr[1] - S[1], vz = Wr[2] - S[2], wx = sx - S[0], wy = sy - S[1], wz = sz - S[2];
            double t = Math.max(0, Math.min(1, (wx * vx + wy * vy + wz * vz) / (vx * vx + vy * vy + vz * vz)));
            double dx = wx - vx * t, dy = wy - vy * t, dz = wz - vz * t;
            if (dx * dx + dy * dy + dz * dz <= 2.8 * 2.8) return true;
            // le grand drap de la manche : il pend sous le bras, plus long au poignet, plisse
            if (sx >= 9 && sx <= 21.5) {
                double t2 = (sx - 9) / 12.5, yTop = 77 + 16.5 * t2 + 1.2;
                double yLow = 69 - 11 * sm((sx - 9) / 9.0) + 1.6 * Math.sin(sx * 2.0);
                double zc = -1.0 - 1.5 * t2, half = 2.7 - 1.3 * sm((yTop - sy) / 24.0) - 0.8 * (0.5 + 0.5 * Math.sin(sx * 2.3 + sy * 0.05));
                if (sy >= yLow && sy <= yTop && Math.abs(sz - zc) <= half) return true;
            }
        }
        // bras replie (cote exterieur) : manche tombante qui se resserre, avant-bras vers l'epee
        if (sx <= -8.5 && sx >= -14.5 && sy >= 46 + 2 * Math.sin(sx * 2.0) && sy <= 80 && Math.abs(sz) <= 3.4 - 0.7 * (80 - sy) / 34.0) return true;
        if (sx >= -9 && sx <= -3.5 && sy >= 59 && sy <= 66 && sz >= -8 && sz <= -3) return true;
        return false;
    }

    /** details fins, modeles au bloc : main ouverte (paume vers le nord), poing et epee, ailes et pointes, nez et arcade */
    private static boolean fine(double sx, double sy, double sz, int variant) {
        double ax = Math.abs(sx);
        // main : paume de 6 x 9, deux blocs d'epaisseur, quatre doigts et le pouce
        if (sz >= -3.6 && sz <= -1.6) {
            if (sy >= 94 && sy <= 102.9 && sx >= 17.4 && sx <= 23.6) return true;
            double[][] fg = {{18, 19, 107.5}, {19, 20, 109.5}, {20, 21, 108.5}, {21, 22, 106.5}, {22, 23, 104.5}};
            for (double[] f : fg) if (sx >= f[0] && sx <= f[1] && sy >= 103 && sy <= f[2]) return true;
            if (sx >= 15.4 && sx <= 17.4 && sy >= 95 && sy <= 99.9) return true;
        }
        // poing sur la garde de l'epee plantee devant, pommeau, garde, lame
        if (sx >= -5.6 && sx <= -2.4 && sy >= 61 && sy <= 65.9 && sz >= -8.6 && sz <= -5) return true;
        if (sx >= -4.6 && sx <= -2.6 && sz >= -8.2 && sz <= -6.6 && sy >= 20 && sy <= 58) return true;
        if (sx >= -8.2 && sx <= 1.2 && sz >= -8.4 && sz <= -6.2 && sy >= 58 && sy <= 59.9) return true;
        if (sx >= -4.2 && sx <= -3.0 && sz >= -7.8 && sz <= -7.0 && sy >= 66 && sy <= 70) return true;
        // visage : nez et arcade
        if (ax <= 0.9 && sz >= -6.2 && sz <= -4.0 && sy >= 89.5 && sy <= 93.9) return true;
        if (ax <= 3.6 && sz >= -5.4 && sz <= -4.0 && sy >= 94.1 && sy <= 95.9) return true;
        if (variant == 0) {
            if (ax >= 4 && ax <= 10 && Math.abs(sz) <= 1.3 && sy >= 99 + (ax - 4) * 1.4 && sy <= 105 + (ax - 4) * 1.4) return true;     // ailes
            if (ax <= 0.9 && sz >= -3 && sz <= 3 && sy >= 103 && sy <= 107) return true;
        } else {
            double[][] sp = {{-3.5, -0.5, 106}, {3.5, -0.5, 106}, {0, -3.5, 106}, {0, 2.5, 106}, {0, -0.5, 109.5}};
            for (double[] s : sp) if (Math.abs(sx - s[0]) <= 0.9 && Math.abs(sz - s[1]) <= 0.9 && sy >= 101 && sy <= s[2]) return true;
        }
        return false;
    }

    /** orbites des yeux : on creuse des blocs entiers */
    private static boolean carve(double sx, double sy, double sz, int variant) {
        double ax = Math.abs(sx);
        return sy >= 93.9 && sy <= 94.9 && ax >= 1.4 && ax <= 3.1 && sz >= -5.4 && sz <= -3.9;
    }

    private void mossPass() {
        for (int y = 8; y < STATUE_TOP; y++) for (int z = CZ - 10; z <= CZ + 10; z++) for (int x = 0; x < W; x++) {
            byte id = g[idx(x, y, z)];
            if (id != STONE && id != ANDESITE && id != TUFF && id != COBBLE) continue;
            if (g[idx(x, y + 1, z)] == AIR && rnd(x, y, z) < 0.18) g[idx(x, y, z)] = (byte) MOSS;
        }
    }
}
