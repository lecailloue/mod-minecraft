package net.ringsofash.world;

import static net.ringsofash.world.OrthancPlan.AIR;
import static net.ringsofash.world.OrthancPlan.ANDESITE;
import static net.ringsofash.world.OrthancPlan.BARREL;
import static net.ringsofash.world.OrthancPlan.CAMPFIRE;
import static net.ringsofash.world.OrthancPlan.CARPET;
import static net.ringsofash.world.OrthancPlan.CHAIN;
import static net.ringsofash.world.OrthancPlan.COBBLE;
import static net.ringsofash.world.OrthancPlan.DLOG_X;
import static net.ringsofash.world.OrthancPlan.DLOG_Y;
import static net.ringsofash.world.OrthancPlan.DLOG_Z;
import static net.ringsofash.world.OrthancPlan.DPLANKS;
import static net.ringsofash.world.OrthancPlan.FENCE;
import static net.ringsofash.world.OrthancPlan.GOLD;
import static net.ringsofash.world.OrthancPlan.GRAVEL;
import static net.ringsofash.world.OrthancPlan.LANTERN;
import static net.ringsofash.world.OrthancPlan.LOG_X;
import static net.ringsofash.world.OrthancPlan.LOG_Y;
import static net.ringsofash.world.OrthancPlan.LOG_Z;
import static net.ringsofash.world.OrthancPlan.MOSSY_COBBLE;
import static net.ringsofash.world.OrthancPlan.PATH;
import static net.ringsofash.world.OrthancPlan.PLANKS;
import static net.ringsofash.world.OrthancPlan.SB;
import static net.ringsofash.world.OrthancPlan.SB_MOSS;
import static net.ringsofash.world.OrthancPlan.SHELF;
import static net.ringsofash.world.OrthancPlan.SLAB_SB;
import static net.ringsofash.world.OrthancPlan.SLAB_SP;
import static net.ringsofash.world.OrthancPlan.STAIR_SB;
import static net.ringsofash.world.OrthancPlan.TUFF;
import static net.ringsofash.world.OrthancPlan.WALL_SB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plan d'Edoras : une colline rocheuse au sommet plat, une route en lacets qui la gravit, le palais d'Or (Meduseld) sur une terrasse de pierre,
 * une tour de cloche, des maisons au toit de chaume aux faitages ornes de tetes de chevaux, une palissade et des enclos.
 * Grille de blocs abstraits (palette de {@link OrthancPlan} + quelques ajouts), sans dependance a Minecraft.
 * Repere local : x, z de 0 a W-1 / D-1 ; y = 0 : premier niveau d'air au-dessus du sol naturel (sous le sol : negatif).
 * La colline est tournee de ANG degres : son grand axe (u) va du sud-ouest (route) au nord-est (palais).
 */
public final class EdorasPlan {
    public static final int W = 260, D = 220, OFF = 12, CX = 130, CZ = 110;
    public static final int H0 = 30;                                       // hauteur du sommet de la colline au-dessus du sol
    public static final int H = OFF + H0 + 52;
    public static final double A = 88.0, B = 58.0, RP = 0.80, RW = 1.42;              // demi-axes de la colline, rayon (relatif) du plateau
    private static final double ANG = Math.toRadians(25);
    private static final double UX = Math.cos(ANG), UZ = -Math.sin(ANG), VX = Math.sin(ANG), VZ = Math.cos(ANG);

    public static final byte STONE = 100, DIRT = 101, GRASS = 102, COARSE = 103, GRANITE = 104, HAY = 105, MOSAIC = 106, GLASSPANE = 107, DFENCE = 108,
            BELL = 109, LAMP = 110, SLAB_MO = 111, SLAB_MO_TOP = 112, SLAB_DP = 113, SLAB_DP_TOP = 114, WALL_COBBLE = 115, DSTRIP_Y = 116, DSTRIP_X = 117,
            DSTRIP_Z = 118, STAIR_MO = 120;                                // STAIR_MO + dir * 2 + (haut ? 1 : 0)
    public static final int WLOG_Y = 128, WLOG_X = 129, WLOG_Z = 130, WPLANKS = 131, WSLAB = 132, WSLAB_TOP = 133, WFENCE = 134, CARVED = 135, TIMBER = 136, PLASTER = 137,
            SHEAF_Y = 138, SHEAF_X = 139, EARTH = 140, FARM = 160, WHEAT_R = 161, WHEAT_Y = 162, CARROT_R = 163, CARROT_Y = 164, POTATO_R = 165, POTATO_Y = 166, WATER = 167,
            CAULDRON_W = 168, COMPOSTER = 169, CRAFT = 170, ANVIL = 171, LAVA_CAULD = 172, POT_TULIP = 173, POT_DAND = 174, POT_CORN = 175, WOOL_G = 176, WOOL_W = 177,
            PLATE = 178, CARPET_G = 179, CARPET_W = 180;
    public static final int N = 0, E = 1, S = 2, W_ = 3;

    private final byte[] g = new byte[W * H * D];
    private final int[] top = new int[W * D];
    private final int[] surf = new int[W * D];                              // niveau debout de chaque colonne (0 = pas de colline)
    private final double[] roadD = new double[W * D];
    private final double[] roadS = new double[W * D];
    private final List<double[]> road = new ArrayList<>();                  // sommets de la route (x, z)
    private double roadLen, roadExt;
    private int lvl = H0;                                                   // niveau debout du groupe de batiments en cours (colline : H0, bas du village : 1)
    public int lowHouses, wallCells, lowFieldX = -1, lowFieldZ, stallX = -1, stallZ, smithX = -1, smithZ, stallL, smithL;
    private final List<int[]> boxes = new ArrayList<>();                    // emprises deja prises : x0, z0, x1, z1
    public int houses, hallX, hallZ, towerX, towerZ;

    private static EdorasPlan inst;
    public static synchronized EdorasPlan get() { if (inst == null) inst = new EdorasPlan(); return inst; }

    public EdorasPlan() {
        buildRoad();
        hill();
        village();
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int t = -OFF - 1;
            for (int y = H - OFF - 1; y >= -OFF; y--) if (g[idx(x, y, z)] != AIR) { t = y; break; }
            top[z * W + x] = t;
        }
    }

    public int top(int x, int z) { return top[z * W + x]; }
    public int surface(int x, int z) { return surf[z * W + x]; }

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
    private static double ux(double x, double z) { return (x + 0.5 - CX) * UX + (z + 0.5 - CZ) * UZ; }
    private static double vx(double x, double z) { return (x + 0.5 - CX) * VX + (z + 0.5 - CZ) * VZ; }
    private static double wx(double u, double v) { return CX + u * UX + v * VX; }
    private static double wz(double u, double v) { return CZ + u * UZ + v * VZ; }
    public static boolean area(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }

    // ------------------------------------------------------------------ route et colline
    private void buildRoad() {
        double[][] uv = {{-140, -14}, {-122, -5}, {-104, 2}, {-88, 26}, {-60, 46}, {-30, 54}, {-6, 46}, {-6, 28}, {6, 14}};
        for (double[] p : uv) road.add(new double[]{wx(p[0], p[1]), wz(p[0], p[1])});
        for (int i = 1; i < road.size(); i++) { double sg = Math.hypot(road.get(i)[0] - road.get(i - 1)[0], road.get(i)[1] - road.get(i - 1)[1]); if (i <= 2) roadExt += sg; }
        for (int i = 1; i < road.size(); i++) roadLen += Math.hypot(road.get(i)[0] - road.get(i - 1)[0], road.get(i)[1] - road.get(i - 1)[1]);
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            double best = 1e9, bs = 0, acc = 0;
            for (int i = 1; i < road.size(); i++) {
                double[] a = road.get(i - 1), b = road.get(i);
                double dx = b[0] - a[0], dz = b[1] - a[1], l2 = dx * dx + dz * dz, len = Math.sqrt(l2);
                double t = Math.max(0, Math.min(1, ((x + 0.5 - a[0]) * dx + (z + 0.5 - a[1]) * dz) / l2));
                double d = Math.hypot(x + 0.5 - (a[0] + dx * t), z + 0.5 - (a[1] + dz * t));
                if (d < best) { best = d; bs = acc + t * len; }
                acc += len;
            }
            roadD[z * W + x] = best; roadS[z * W + x] = bs;
        }
    }

    private double hillAt(double u, double v) {
        double r = Math.hypot(u / A, v / B);
        r += vn(u / 14.0, v / 14.0, 3) * 0.05 + vn(u / 5.0, v / 5.0, 4) * 0.02;
        if (r <= RP) return H0;
        if (r >= 1.0) return 0;
        double t = (r - RP) / (1 - RP);
        return H0 * (1 - smooth(t)) + 2.2 * vn(u / 3.5, v / 3.5, 6) * 4 * t * (1 - t);
    }

    private static double ringR(int x, int z) {
        double u = ux(x, z), v = vx(x, z);
        return Math.hypot(u / A, v / B) + 0.035 * vn(u / 20.0, v / 20.0, 21);
    }

    private void hill() {
        int[] slope = new int[W * D];
        double[] raw = new double[W * D];
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            double u = ux(x, z), v = vx(x, z);
            double hh = hillAt(u, v);
            double d = roadD[z * W + x], rs = roadS[z * W + x];
            double ry = Math.max(0.0, Math.min(1.0, (rs - roadExt) / ((roadLen - roadExt) * 0.97))) * H0;
            double bw = 4 + 0.7 * Math.abs(ry - hh);
            double wr = d <= 3.2 ? 1 : smooth(1 - (d - 3.2) / bw);
            raw[z * W + x] = hh * (1 - wr) + ry * wr;
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x, sv = (int) Math.round(raw[i]);
            if (sv < 1 && (ringR(x, z) < RW || roadD[i] <= 3.2)) sv = 1;       // le bas du village : une couche d'herbe plane qui cache les irregularites du sol
            surf[i] = sv;
        }
        for (int x = 1; x < W - 1; x++) for (int z = 1; z < D - 1; z++) {
            int s = surf[z * W + x], m = 0;
            m = Math.max(m, Math.abs(s - surf[z * W + x + 1])); m = Math.max(m, Math.abs(s - surf[z * W + x - 1]));
            m = Math.max(m, Math.abs(s - surf[(z + 1) * W + x])); m = Math.max(m, Math.abs(s - surf[(z - 1) * W + x]));
            slope[z * W + x] = m;
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x, s = surf[i];
            if (s < 1) continue;
            boolean onRoad = roadD[i] <= 3.2 && s <= H0;
            boolean steep = slope[i] >= 2;
            double nz = vn(x / 6.0, z / 6.0, 8);
            int topB;
            if (onRoad) topB = rnd(x, 5, z) < 0.45 ? PATH : rnd(x, 6, z) < 0.5 ? GRAVEL : EARTH;
            else if (steep) topB = rnd(x, 7, z) < 0.35 ? STONE : rnd(x, 8, z) < 0.45 ? ANDESITE : rnd(x, 9, z) < 0.5 ? COBBLE : rnd(x, 10, z) < 0.5 ? TUFF : rnd(x, 11, z) < 0.5 ? GRANITE : MOSSY_COBBLE;
            else if (s == 1 && Math.abs(ringR(x, z) - 1.095) < 0.022) topB = rnd(x, 14, z) < 0.65 ? EARTH : rnd(x, 15, z) < 0.6 ? PATH : COARSE;   // chemin de ronde du bas du village
            else topB = nz > 0.55 ? COARSE : GRASS;
            for (int y = 0; y < s; y++) {
                int b;
                if (y == s - 1) b = topB;
                else if (y >= s - 4 && (topB == GRASS || topB == COARSE || topB == PATH || topB == GRAVEL || topB == EARTH)) b = DIRT;
                else if (y >= s - 3 && steep) b = rnd(x, y, z) < 0.5 ? STONE : ANDESITE;
                else b = rnd(x, y, z) < 0.12 ? ANDESITE : STONE;
                set(x, y, z, b);
            }
            if (onRoad && s < H0 && roadD[i] > 2.0) {                          // muret du bord de route (le cote qui domine le vide)
                int lower = Math.min(surf[i + 1], Math.min(surf[i - 1], Math.min(surf[i + W], surf[i - W])));
                if (lower < s - 1) set(x, s, z, WALL_COBBLE);
            }
        }
        // sommet : dalles de la place devant le palais et chemins qui rejoignent les maisons ; fosse a feu sur la place
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            int i = z * W + x;
            if (surf[i] == H0 && roadD[i] <= 2.2) set(x, H0 - 1, z, rnd(x, 12, z) < 0.5 ? PATH : GRAVEL);
        }
    }

    // ------------------------------------------------------------------ village
    private boolean free(int x0, int z0, int x1, int z1, int margin) {
        for (int[] b : boxes) if (x0 - margin <= b[2] && x1 + margin >= b[0] && z0 - margin <= b[3] && z1 + margin >= b[1]) return false;
        for (int x = x0 - 1; x <= x1 + 1; x += 2) for (int z = z0 - 1; z <= z1 + 1; z += 2) {
            if (!area(x, z)) return false;
            if (surf[z * W + x] != lvl || roadD[z * W + x] < (lvl == H0 ? 3.4 : 5.5)) return false;
            double u = ux(x, z), v = vx(x, z), rr = Math.hypot(u / A, v / B);
            if (lvl == H0 ? rr > RP - 0.035 : (rr < 1.05 || ringR(x, z) > RW - 0.06)) return false;
        }
        return true;
    }
    private void take(int x0, int z0, int x1, int z1) { boxes.add(new int[]{x0, z0, x1, z1}); }

    private void village() {
        // palais d'Or : sur la terrasse, au bout nord-est de la colline, perron au sud
        int hx = (int) Math.round(wx(36, -4)), hz = (int) Math.round(wz(36, -4));
        hallX = hx; hallZ = hz;
        hall(hx, hz);
        take(hx - 22, hz - 25, hx + 22, hz + 25);
        // tour de cloche
        int tx = (int) Math.round(wx(10, -36)), tz = (int) Math.round(wz(10, -36));
        towerX = tx; towerZ = tz;
        if (free(tx - 3, tz - 3, tx + 3, tz + 3, 2)) { bellTower(tx, tz); take(tx - 3, tz - 3, tx + 3, tz + 3); }
        // palissade : de l'ouest vers le sud-ouest, le long du bord du plateau
        palisade();
        // maisons : positions (u, v) prises sur l'image de reference (rangee au nord-ouest, grappe au centre, quelques maisons au sud-est de la route) ;
        // si une place est prise, on cherche la plus proche
        double[][] spots = {
                {-62, -12, 0}, {-50, -26, 1}, {-34, -34, 0}, {-16, -37, 1}, {-36, 4, 2}, {-16, 12, 3}, {-52, 16, 0}, {-30, 30, 1}, {-10, 32, 0}, {-70, 4, 0},
                {-22, -16, 0}, {-4, -20, 1}, {-44, -8, 0}, {-2, 6, 0}, {10, 30, 1}, {-58, -2, 1}, {-24, 44, 0}, {-44, 36, 0}, {4, -38, 0}, {24, 26, 0},
                {-12, -2, 0}, {-28, -4, 0}, {-60, 26, 0}, {14, 12, 0}, {-40, -16, 0}, {-8, -28, 0}};
        Random r = new Random(77);
        for (double[] sp : spots) {
            int type = (int) sp[2];
            int w, d;
            switch (type) {
                case 1 -> { w = 9; d = 13; }
                case 2 -> { w = 11; d = 21; }
                case 3 -> { w = 11; d = 15; }
                default -> { w = 7; d = 11; }
            }
            for (int att = 0; att < 80; att++) {
                double ju = att == 0 ? 0 : (r.nextDouble() - 0.5) * 2 * (3 + att * 0.4), jv = att == 0 ? 0 : (r.nextDouble() - 0.5) * 2 * (3 + att * 0.4);
                int cx = (int) Math.round(wx(sp[0] + ju, sp[1] + jv)), cz = (int) Math.round(wz(sp[0] + ju, sp[1] + jv));
                int rot = r.nextInt(4);
                int ww = rot % 2 == 0 ? w : d, dd = rot % 2 == 0 ? d : w;
                int x0 = cx - ww / 2, z0 = cz - dd / 2;
                int extra = type == 3 && rot % 2 == 0 ? 8 : 0;
                if (!free(x0 - 2, z0 - 2, x0 + ww + 1 + extra, z0 + dd + 1, 1)) continue;
                gabled(x0, z0, w, d, 4, rot, type == 2 ? 3 : 0, type == 1 || type == 3, false);
                if (type == 3 && rot % 2 == 0) gabled(x0 + w, z0 + d / 2 - 3, 7, 9, 4, 1, 1, false, true);
                take(x0 - 1, z0 - 1, x0 + ww + 1 + extra, z0 + dd + 1);
                houses++;
                break;
            }
        }
        // enclos et foin
        for (double[] ps : new double[][]{{-48, 24}, {-60, -6}, {14, 22}}) {
            int ex = (int) Math.round(wx(ps[0], ps[1])), ez = (int) Math.round(wz(ps[0], ps[1]));
            if (!free(ex - 5, ez - 4, ex + 5, ez + 4, 1)) continue;
            corral(ex - 5, ez - 4, 11, 9);
            take(ex - 6, ez - 5, ex + 6, ez + 5);
        }
        // puits et torches de la place
        int px = (int) Math.round(wx(10, 16)), pz = (int) Math.round(wz(10, 16));
        if (free(px - 1, pz - 1, px + 1, pz + 1, 0)) well(px, pz);
        life(new Random(913));
        lowVillage();
        outerWall();
        gate();
        lampPosts();
    }

    /** bas du village : maisons et enclos au pied de la colline, dans l'anneau entre le pied et la muraille */
    private void lowVillage() {
        lvl = 1;
        Random r = new Random(311);
        int n = 64;
        for (int k = 0; k < n; k++) {
            double th = (k + 0.5) * 2 * Math.PI / n + (r.nextDouble() - 0.5) * 0.06;
            if (inFarm(th)) continue;
            double rad = 1.17 + r.nextDouble() * 0.10;
            double u = A * rad * Math.cos(th), v = B * rad * Math.sin(th);
            int type = r.nextDouble() < 0.18 ? 1 : r.nextDouble() < 0.12 ? 3 : 0;
            int w = type == 1 ? 9 : type == 3 ? 11 : 7, d = type == 1 ? 13 : type == 3 ? 15 : 11;
            for (int att = 0; att < 20; att++) {
                double jr = att == 0 ? 0 : (r.nextDouble() - 0.5) * 8;
                int cx = (int) Math.round(wx(u + jr, v + jr)), cz = (int) Math.round(wz(u + jr, v - jr));
                int rot = r.nextInt(4);
                int ww = rot % 2 == 0 ? w : d, dd = rot % 2 == 0 ? d : w;
                int x0 = cx - ww / 2, z0 = cz - dd / 2;
                int extra = type == 3 && rot % 2 == 0 ? 8 : 0;
                if (type == 3) { if (rot % 2 == 0) { /* aile a l'est : garde la place */ } else { type = 0; w = 7; d = 11; ww = d; dd = w; x0 = cx - ww / 2; z0 = cz - dd / 2; extra = 0; } }
                if (!free(x0 - 1, z0 - 1, x0 + ww + extra, z0 + dd, 1)) continue;
                gabled(x0, z0, w, d, 4, rot, 0, type == 1 || type == 3, false);
                if (type == 3 && rot % 2 == 0) gabled(x0 + w, z0 + d / 2 - 3, 7, 9, 4, 1, 1, false, true);
                take(x0 - 1, z0 - 1, x0 + ww + 1 + extra, z0 + dd + 1);
                houses++; lowHouses++;
                break;
            }
        }
        // enclos, puits, foin
        for (int k = 0; k < 7; k++) {
            double th = r.nextDouble() * 2 * Math.PI, rad = 1.18 + r.nextDouble() * 0.08;
            int ex = (int) Math.round(wx(A * rad * Math.cos(th), B * rad * Math.sin(th))), ez = (int) Math.round(wz(A * rad * Math.cos(th), B * rad * Math.sin(th)));
            if (free(ex - 5, ez - 4, ex + 5, ez + 4, 1)) { corral(ex - 5, ez - 4, 11, 9); take(ex - 6, ez - 5, ex + 6, ez + 5); }
        }
        for (int k = 0; k < 3; k++) {
            double th = r.nextDouble() * 2 * Math.PI;
            int px = (int) Math.round(wx(A * 1.1 * Math.cos(th), B * 1.1 * Math.sin(th))), pz = (int) Math.round(wz(A * 1.1 * Math.cos(th), B * 1.1 * Math.sin(th)));
            if (free(px - 1, pz - 1, px + 1, pz + 1, 1)) { well(px, pz); take(px - 1, pz - 1, px + 1, pz + 1); }
        }
        lowLife(r);
        lvl = H0;
    }

    /** cherche un emplacement libre de w x d sur l'ellipse (angle th0..th1, rayon relatif rad0..rad1) ; renvoie {x0, z0} ou null */
    private int[] findSpot(Random r, int w, int d, double th0, double th1, double rad0, double rad1, int tries) {
        for (int k = 0; k < tries; k++) {
            double th = th0 + r.nextDouble() * (th1 - th0), rad = rad0 + r.nextDouble() * (rad1 - rad0);
            int cx = (int) Math.round(wx(A * rad * Math.cos(th), B * rad * Math.sin(th))), cz = (int) Math.round(wz(A * rad * Math.cos(th), B * rad * Math.sin(th)));
            int x0 = cx - w / 2, z0 = cz - d / 2;
            if (free(x0 - 1, z0 - 1, x0 + w, z0 + d, 1)) { take(x0 - 1, z0 - 1, x0 + w, z0 + d); return new int[]{x0, z0}; }
        }
        return null;
    }

    private static final double TAU = Math.PI * 2;

    /** la vie du village sur la colline : etals, forge, tas de bois, meules, jardins */
    private void life(Random r) {
        int[] sm = findSpot(r, 9, 7, 0, TAU, 0.05, 0.74, 600);
        if (sm != null) smithy(sm[0] + 1, sm[1] + 1);
        for (int k = 0; k < 3; k++) { int[] sp = findSpot(r, 7, 5, 0, TAU, 0.05, 0.72, 250); if (sp != null) stall(sp[0] + 3, sp[1] + 2, k % 2 == 0); }
        for (int k = 0; k < 5; k++) { int[] sp = findSpot(r, 6, 3, 0, TAU, 0.05, 0.74, 250); if (sp != null) woodpile(sp[0] + 1, sp[1] + 1); }
        for (int k = 0; k < 3; k++) { int[] sp = findSpot(r, 5, 5, 0, TAU, 0.05, 0.74, 250); if (sp != null) haystack(sp[0] + 2, sp[1] + 2); }
        for (int k = 0; k < 3; k++) { int[] sp = findSpot(r, 8, 7, 0, TAU, 0.05, 0.72, 300); if (sp != null) field(sp[0], sp[1], 8, 7, k, r.nextDouble() < 0.7); }
    }

    private static boolean inFarm(double th) {
        double d = Math.toDegrees(th) % 360;
        if (d < 0) d += 360;
        return (d >= 300 && d <= 346) || (d >= 56 && d <= 112) || (d >= 214 && d <= 250);
    }

    /** la vie du bas du village : champs dans les secteurs cultives, etals pres de la porte, forge, bois, meules */
    private void lowLife(Random r) {
        double[][] farm = {{300, 346}, {56, 112}, {214, 250}};
        if (smithX < 0) { int[] sm = findSpot(r, 9, 7, 0, TAU, 1.14, 1.30, 600); if (sm != null) smithy(sm[0] + 1, sm[1] + 1); }
        int fields = 0;
        for (double[] f : farm) for (int k = 0; k < 4; k++) {
            int[] sp = findSpot(r, 9, 8, Math.toRadians(f[0]), Math.toRadians(f[1]), 1.14, 1.30, 120);
            if (sp != null) { field(sp[0], sp[1], 9, 8, fields++, r.nextDouble() < 0.7); }
        }
        for (int k = 0; k < 2; k++) { int[] sp = findSpot(r, 7, 5, Math.toRadians(150), Math.toRadians(205), 1.14, 1.30, 120); if (sp != null) stall(sp[0] + 3, sp[1] + 2, k % 2 == 0); }
        for (int k = 0; k < 7; k++) { int[] sp = findSpot(r, 6, 3, 0, TAU, 1.14, 1.32, 150); if (sp != null) woodpile(sp[0] + 1, sp[1] + 1); }
        for (int k = 0; k < 5; k++) {
            double[] f = farm[k % 3];
            int[] sp = findSpot(r, 5, 5, Math.toRadians(f[0] - 6), Math.toRadians(f[1] + 6), 1.14, 1.32, 120);
            if (sp != null) haystack(sp[0] + 2, sp[1] + 2);
        }
        lvl = 1;
    }

    /** petit champ clos : rangs de culture separes par des rigoles, cloture ajouree, epouvantail au coin */
    private void field(int x0, int z0, int w, int d, int crop, boolean ripe) {
        int y0 = lvl - 1;
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) {
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + d - 1;
            if (edge) { if (!(z == z0 + d - 1 && x == x0 + w / 2)) set(x, lvl, z, WFENCE); continue; }
            int iz = z - z0 - 1;
            if (iz % 3 == 1) { set(x, y0, z, WATER); continue; }
            set(x, y0, z, FARM);
            int id;
            switch ((crop + (iz / 3)) % 3) {
                case 0 -> id = ripe ? WHEAT_R : WHEAT_Y;
                case 1 -> id = ripe ? CARROT_R : CARROT_Y;
                default -> id = ripe ? POTATO_R : POTATO_Y;
            }
            set(x, lvl, z, id);
        }
        if (lvl == 1 && lowFieldX < 0) { lowFieldX = x0 + w / 2; lowFieldZ = z0 + d / 2; }
        scarecrow(x0 + w - 1, z0);
        if (rnd(x0, 24, z0) < 0.6) { set(x0, lvl, z0 + d - 1, SHEAF_Y); set(x0, lvl + 1, z0 + d - 1, SHEAF_Y); }
    }

    private void scarecrow(int x, int z) {
        set(x, lvl, z, WLOG_Y); set(x, lvl + 1, z, WLOG_Y);
        set(x - 1, lvl + 1, z, WFENCE); set(x + 1, lvl + 1, z, WFENCE);
        set(x, lvl + 2, z, HAY); set(x, lvl + 3, z, WSLAB); set(x - 1, lvl + 3, z, WSLAB_TOP); set(x + 1, lvl + 3, z, WSLAB_TOP);
    }

    /** etal de marche 5 x 3 : auvent de laine verte et blanche, comptoir, marchandises */
    private void stall(int cx, int cz, boolean alongX) {
        if (stallX < 0) { stallX = cx; stallZ = cz; stallL = lvl; }
        int hx = alongX ? 2 : 1, hz = alongX ? 1 : 2;
        for (int x = cx - hx; x <= cx + hx; x++) for (int z = cz - hz; z <= cz + hz; z++) {
            int k = alongX ? x - cx + hx : z - cz + hz;
            set(x, lvl + 4, z, k % 2 == 0 ? WOOL_G : WOOL_W);
            boolean corner = (x == cx - hx || x == cx + hx) && (z == cz - hz || z == cz + hz);
            if (corner) { for (int y = 0; y < 4; y++) set(x, lvl + y, z, WLOG_Y); continue; }
            boolean front = alongX ? z == cz + hz : x == cx + hx;
            boolean back = alongX ? z == cz - hz : x == cx - hx;
            if (front) {
                int[] cyc = {WSLAB, BARREL, COMPOSTER, HAY, WSLAB};
                int c = cyc[k % 5];
                set(x, lvl, z, c);
                if (c == WSLAB) set(x, lvl + 1, z, (hash(x, 9, z) & 1) == 0 ? POT_CORN : POT_TULIP);
                if (c == HAY || c == BARREL) set(x, lvl + 1, z, SHEAF_Y);
            } else if (back) { set(x, lvl, z, k % 2 == 0 ? BARREL : SHEAF_Y); }
        }
        set(cx, lvl + 3, cz, CHAIN); set(cx, lvl + 2, cz, LAMP);
    }

    /** forge ouverte : mur de pierre et cheminee au nord, foyer de lave, enclume, etabli, toit de chaume */
    private void smithy(int x0, int z0) {
        if (smithX < 0) { smithX = x0 + 3; smithZ = z0 + 2; smithL = lvl; }
        for (int x = x0 - 1; x <= x0 + 7; x++) for (int z = z0 - 1; z <= z0 + 5; z++) {
            boolean in = x >= x0 && x <= x0 + 6 && z >= z0 && z <= z0 + 4;
            if (in) set(x, lvl - 1, z, rnd(x, 25, z) < 0.6 ? COBBLE : GRAVEL);
            set(x, lvl + 4, z, SLAB_MO);
        }
        for (int x = x0; x <= x0 + 6; x++) for (int y = 0; y <= 3; y++) set(x, lvl + y, z0, y == 3 && x % 2 == 0 ? CARVED : COBBLE);
        for (int y = 4; y <= 8; y++) set(x0 + 1, lvl + y, z0, COBBLE);
        set(x0 + 1, lvl + 9, z0, CAMPFIRE);
        for (int[] c : new int[][]{{0, 4}, {6, 4}}) for (int y = 0; y < 4; y++) set(x0 + c[0], lvl + y, z0 + c[1], WLOG_Y);
        set(x0 + 1, lvl, z0 + 1, LAVA_CAULD); set(x0 + 3, lvl, z0 + 2, ANVIL);
        set(x0 + 5, lvl, z0 + 1, BARREL); set(x0 + 6, lvl, z0 + 1, CRAFT); set(x0 + 5, lvl, z0 + 3, WSLAB); set(x0 + 5, lvl + 1, z0 + 3, POT_CORN);
        set(x0 + 3, lvl + 3, z0 + 3, CHAIN); set(x0 + 3, lvl + 2, z0 + 3, LAMP);
    }

    private void woodpile(int x, int z) {
        for (int dz = 0; dz < 2; dz++) {
            for (int dx = 0; dx < 4; dx++) set(x + dx, lvl, z + dz, WLOG_X);
            for (int dx = 1; dx < 3; dx++) set(x + dx, lvl + 1, z + dz, WLOG_X);
        }
        set(x + 1, lvl + 2, z, WLOG_X); set(x + 2, lvl + 2, z, WLOG_X);
        set(x + 4, lvl, z + 1, WLOG_Y);                                    // billot
        set(x + 5, lvl, z, WLOG_Y); set(x + 5, lvl + 1, z, WSLAB);
    }

    private void haystack(int x, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(x + dx, lvl, z + dz, HAY);
        for (int[] c : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) set(x + c[0], lvl + 1, z + c[1], HAY);
        set(x, lvl + 2, z, HAY);
        set(x + 2, lvl, z, SHEAF_Y); set(x + 2, lvl, z + 1, SHEAF_Y); set(x + 2, lvl + 1, z, SHEAF_Y); set(x - 2, lvl, z - 1, SHEAF_Y);
    }

    /** lampadaires (poteau de bois patine, lanterne) le long de la route et sur la place */
    private void lampPosts() {
        double acc = 6;
        for (int i = 1; i < road.size(); i++) {
            double[] a = road.get(i - 1), b = road.get(i);
            double dx = b[0] - a[0], dz = b[1] - a[1], len = Math.hypot(dx, dz), nx = -dz / len, nz = dx / len;
            for (double t = 0; t < len; t += 1) {
                acc += 1;
                if (acc < 15) continue;
                acc = 0;
                double side = (i + (int) (t / 15)) % 2 == 0 ? 3.8 : -3.8;
                int x = (int) Math.round(a[0] + dx * t / len + nx * side), z = (int) Math.round(a[1] + dz * t / len + nz * side);
                if (!area(x, z)) continue;
                int sv = surf[z * W + x], rs = surf[(int) Math.round(a[1] + dz * t / len) * W + (int) Math.round(a[0] + dx * t / len)];
                if (sv < 1 || Math.abs(sv - rs) > 1 || ringR(x, z) > RW - 0.05) continue;
                boolean taken = false;
                for (int[] bx : boxes) if (x >= bx[0] - 1 && x <= bx[2] + 1 && z >= bx[1] - 1 && z <= bx[3] + 1) taken = true;
                if (taken) continue;
                for (int y = 0; y < 3; y++) set(x, sv + y, z, WFENCE);
                set(x, sv + 3, z, LAMP);
            }
        }
    }

    /** muraille de rondins patines tout autour du village, ouverte a la route */
    private void outerWall() {
        boolean[] in = new boolean[W * D];
        for (int x = 1; x < W - 1; x++) for (int z = 1; z < D - 1; z++) in[z * W + x] = ringR(x, z) < RW;
        for (int x = 1; x < W - 1; x++) for (int z = 1; z < D - 1; z++) {
            int i = z * W + x;
            if (!in[i] || (in[i + 1] && in[i - 1] && in[i + W] && in[i - W])) continue;
            if (surf[i] != 1) continue;
            if (roadD[i] < 5.0) continue;
            int h = (x + z) % 2 == 0 ? 6 : 5;
            boolean tall = (x * 7 + z * 13) % 17 == 0;
            if (tall) h = 8;
            else if (rnd(x, 31, z) < 0.15) h--;
            set(x, 0, z, rnd(x, 32, z) < 0.5 ? COBBLE : MOSSY_COBBLE);
            for (int y = 0; y < h; y++) set(x, 1 + y, z, y == 3 && !tall ? WLOG_Y : WLOG_Y);
            set(x, 1 + h, z, tall ? LAMP : WSLAB);
            if (tall) { set(x, 1 + h - 1, z, CARVED); set(x, 1 + h - 3, z, CARVED); }
            wallCells++;
        }
        // tours de guet : aux quatre coins de l'ellipse
        for (int k = 0; k < 4; k++) {
            double th = Math.toRadians(45 + 90 * k + 8), rad = RW - 0.075;
            int cx = (int) Math.round(wx(A * rad * Math.cos(th), B * rad * Math.sin(th))), cz = (int) Math.round(wz(A * rad * Math.cos(th), B * rad * Math.sin(th)));
            if (!area(cx - 3, cz - 3) || !area(cx + 3, cz + 3) || surf[cz * W + cx] != 1) continue;
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) set(x, 0, z, COBBLE);
            for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) for (int y = 0; y < 10; y++) set(cx + c[0], 1 + y, cz + c[1], y >= 4 && y <= 5 ? DLOG_Y : WLOG_Y);
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
                set(x, 1 + 7, z, WPLANKS);
                if (Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3) set(x, 1 + 8, z, WFENCE);
                if (Math.abs(x - cx) <= 2 && Math.abs(z - cz) <= 2) set(x, 1 + 11, z, SLAB_MO);
            }
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
                boolean ring = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
                if (ring && !(Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2) && !(x == cx || z == cz)) set(x, 1 + 12, z, SLAB_MO);
            }
            for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) set(cx + c[0], 1 + 10, cz + c[1], WLOG_Y);
            for (int y = 1; y <= 6; y++) for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) set(x, y, z, AIR);
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
                boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
                boolean cornr = Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2;
                if (edge && !cornr) for (int y = 1; y <= 6; y++) set(x, y, z, y == 4 && ((x + z) & 1) == 0 ? AIR : WPLANKS);
            }
            set(cx, 1 + 8, cz, LAMP);
        }
    }

    /** porte fortifiee : deux tours de bois de part et d'autre de la route, linteau sculpte, lanternes suspendues */
    private void gate() {
        double[] cp = null;
        double acc = 0;
        outer:
        for (int i = 1; i < road.size(); i++) {
            double[] a = road.get(i - 1), b = road.get(i);
            double dx = b[0] - a[0], dz = b[1] - a[1], len = Math.hypot(dx, dz);
            for (double t = 0; t < len; t += 0.5) {
                double px = a[0] + dx * t / len, pz = a[1] + dz * t / len;
                if (ringR((int) px, (int) pz) < RW) { cp = new double[]{px, pz, dx, dz}; break outer; }
            }
        }
        if (cp == null) return;
        int gx = (int) Math.round(cp[0]), gz = (int) Math.round(cp[1]);
        boolean alongX = Math.abs(cp[2]) >= Math.abs(cp[3]);          // la route traverse la muraille le long de x (sinon le long de z)
        for (int a = -4; a <= 4; a++) for (int b = -11; b <= 11; b++) {
            int x = alongX ? gx + a : gx + b, z = alongX ? gz + b : gz + a;
            if (!area(x, z)) continue;
            int bb = Math.abs(b);
            // on degage la muraille dans l'emprise
            for (int y = 1; y <= 12; y++) set(x, y, z, AIR);
            if (bb <= 5) { set(x, 0, z, rnd(x, 33, z) < 0.5 ? PATH : EARTH); continue; }
            if (bb > 10 || Math.abs(a) > 3) continue;
            set(x, 0, z, COBBLE);
            boolean cornerT = (bb == 6 || bb == 10) && Math.abs(a) == 3;
            boolean edgeT = bb == 6 || bb == 10 || Math.abs(a) == 3;
            for (int y = 1; y <= 8; y++) {
                if (cornerT) set(x, y, z, y == 1 ? COBBLE : DLOG_Y);
                else if (edgeT) set(x, y, z, y == 1 ? COBBLE : y == 4 || y == 8 ? CARVED : (y == 5 && ((a + b) & 1) == 0 ? AIR : WPLANKS));
            }
            set(x, 9, z, WPLANKS);
            if (edgeT) set(x, 10, z, WFENCE);
            if (cornerT) { set(x, 10, z, DLOG_Y); set(x, 11, z, DLOG_Y); set(x, 12, z, LAMP); }
        }
        // linteau : poutre maitresse sculptee au-dessus du passage, lanternes
        for (int b = -10; b <= 10; b++) for (int a = -1; a <= 1; a++) {
            int x = alongX ? gx + a : gx + b, z = alongX ? gz + b : gz + a;
            if (Math.abs(b) <= 5) {
                set(x, 8, z, a == 0 ? (alongX ? WLOG_Z : WLOG_X) : CARVED);
                set(x, 9, z, WPLANKS);
                if (a == 0) set(x, 10, z, WSLAB);
            }
        }
        for (int b : new int[]{-3, 3}) { int x = alongX ? gx : gx + b, z = alongX ? gz + b : gz; set(x, 7, z, CHAIN); set(x, 6, z, LAMP); }
    }

    private void well(int x, int z) {
        box(x - 1, lvl, z - 1, x + 1, lvl, z + 1, COBBLE);
        set(x, lvl, z, AIR); set(x, lvl - 1, z, AIR); set(x, lvl - 2, z, AIR);
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) { set(x + c[0], lvl + 1, z + c[1], FENCE); set(x + c[0], lvl + 2, z + c[1], FENCE); }
        box(x - 1, lvl + 3, z - 1, x + 1, lvl + 3, z + 1, SLAB_SP);
    }

    private void corral(int x0, int z0, int w, int d) {
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) {
            boolean edge = x == x0 || z == z0 || x == x0 + w - 1 || z == z0 + d - 1;
            if (edge) { if (!(z == z0 + d - 1 && Math.abs(x - (x0 + w / 2)) <= 1)) set(x, lvl, z, WFENCE); }
            else {
                if (rnd(x, 22, z) < 0.3) set(x, lvl - 1, z, COARSE);                       // sol pietine
                else if (rnd(x, 23, z) < 0.07) set(x, lvl - 1, z, EARTH);
                if (rnd(x, 20, z) < 0.07) set(x, lvl, z, HAY);
            }
        }
        for (int[] c : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) { set(x0 + c[0], lvl, z0 + c[1], WLOG_Y); set(x0 + c[0], lvl + 1, z0 + c[1], WLOG_Y); set(x0 + c[0], lvl + 2, z0 + c[1], LAMP); }
        set(x0 + w / 2 - 2, lvl, z0 + d - 1, WLOG_Y); set(x0 + w / 2 + 2, lvl, z0 + d - 1, WLOG_Y);
        // abri a toit de chaume dans l'angle nord-ouest, foin dessous
        int sw = Math.min(5, w - 4);
        for (int x = x0 + 1; x <= x0 + sw; x++) for (int z = z0 + 1; z <= z0 + 3; z++) { set(x, lvl, z, AIR); set(x, lvl + 3, z, SLAB_MO); }
        for (int[] c : new int[][]{{1, 1}, {sw, 1}, {1, 3}, {sw, 3}}) for (int y = 0; y < 3; y++) set(x0 + c[0], lvl + y, z0 + c[1], WLOG_Y);
        for (int x = x0 + 2; x < x0 + sw; x++) { set(x, lvl, z0 + 1, HAY); set(x, lvl + 1, z0 + 1, x == x0 + 3 ? SHEAF_Y : AIR); }
        // abreuvoir, tonneau et ratelier
        set(x0 + w - 3, lvl, z0 + d - 3, CAULDRON_W); set(x0 + w - 4, lvl, z0 + d - 3, CAULDRON_W);
        set(x0 + w - 2, lvl, z0 + d - 3, BARREL);
        set(x0 + w - 2, lvl, z0 + 2, SHEAF_Y); set(x0 + w - 2, lvl, z0 + 3, HAY); set(x0 + w - 3, lvl, z0 + 2, HAY);
    }

    private void palisade() {
        double[][] pts = {{-64, -4}, {-60, 12}, {-50, 27}, {-34, 36}, {-16, 40}};
        for (int i = 1; i < pts.length; i++) {
            double ax = wx(pts[i - 1][0], pts[i - 1][1]), az = wz(pts[i - 1][0], pts[i - 1][1]), bx = wx(pts[i][0], pts[i][1]), bz = wz(pts[i][0], pts[i][1]);
            int n = (int) Math.ceil(Math.hypot(bx - ax, bz - az));
            for (int k = 0; k <= n; k++) {
                int x = (int) Math.round(ax + (bx - ax) * k / n), z = (int) Math.round(az + (bz - az) * k / n);
                if (!area(x, z) || surf[z * W + x] != H0 || roadD[z * W + x] < 4.5) continue;
                if (k % 2 == 0) { for (int y = 0; y < 5; y++) set(x, H0 + y, z, WLOG_Y); set(x, H0 + 5, z, WSLAB); }
                else { for (int y = 0; y < 4; y++) set(x, H0 + y, z, WLOG_Y); set(x, H0 + 4, z, WSLAB); }
            }
        }
    }

    // ------------------------------------------------------------------ tour de cloche : quatre pieds de bois, plateforme, toit en pyramide
    private void bellTower(int cx, int cz) {
        int y0 = H0, ht = 16;
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int y = 0; y < ht; y++) set(cx + c[0], y0 + y, cz + c[1], DLOG_Y);
            box(cx + c[0] - (c[0] > 0 ? 0 : 0), y0 - 1, cz + c[1], cx + c[0], y0 - 1, cz + c[1], COBBLE);
        }
        for (int lv : new int[]{4, 9}) {                                    // croisillons a chaque niveau
            for (int k = -1; k <= 1; k++) {
                set(cx + k, y0 + lv, cz - 2, DLOG_X); set(cx + k, y0 + lv, cz + 2, DLOG_X);
                set(cx - 2, y0 + lv, cz + k, DLOG_Z); set(cx + 2, y0 + lv, cz + k, DLOG_Z);
            }
            for (int k = 0; k < 2; k++) {
                set(cx - 2 + (k == 0 ? 0 : 0), y0 + lv + 1 + k, cz - 2, DPLANKS);
            }
        }
        for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
            set(x, y0 + ht, z, DPLANKS);
            if (Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3) { set(x, y0 + ht + 1, z, FENCE); }
        }
        set(cx, y0 + ht + 1, cz, BELL);
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) set(cx + c[0], y0 + ht + 1, cz + c[1], DLOG_Y);
        // echelle contre un pied
        for (int y = 0; y < ht; y++) set(cx - 2, y0 + y, cz + 3, y == 0 ? LOG_Y : LOG_Y);
        // toit pyramidal de chaume
        for (int k = 0; k < 4; k++) {
            int r = 4 - k, y = y0 + ht + 2 + k;
            for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = Math.abs(x - cx) == r || Math.abs(z - cz) == r;
                if (!edge) continue;
                int dir = Math.abs(x - cx) == r ? (x < cx ? E : W_) : (z < cz ? S : N);
                if (Math.abs(x - cx) == r && Math.abs(z - cz) == r) set(x, y, z, MOSAIC);
                else set(x, y, z, stair(STAIR_MO, dir, false));
            }
        }
        set(cx, y0 + ht + 6, cz, MOSAIC); set(cx, y0 + ht + 7, cz, DFENCE); set(cx, y0 + ht + 8, cz, DFENCE);
        set(cx + 1, y0 + ht + 9, cz, DFENCE); set(cx - 1, y0 + ht + 9, cz, DFENCE);
    }

    // ------------------------------------------------------------------ maison a pignons (faitage selon z local, entree au pignon sud local)
    private int[] map(int r, int x0, int z0, int w, int d, int lx, int lz) {
        return switch (r & 3) {
            case 0 -> new int[]{x0 + lx, z0 + lz};
            case 1 -> new int[]{x0 + (d - 1 - lz), z0 + lx};
            case 2 -> new int[]{x0 + (w - 1 - lx), z0 + (d - 1 - lz)};
            default -> new int[]{x0 + lz, z0 + (w - 1 - lx)};
        };
    }
    private void put(int r, int x0, int z0, int w, int d, int lx, int y, int lz, int id, boolean stairs) {
        int[] p = map(r, x0, z0, w, d, lx, lz);
        int v = id;
        if (stairs) {                                                       // id = base + dir * 2 + haut ; la direction tourne avec la maison
            int base = id - (id - STAIR_SB) % 8;
            if (id >= STAIR_MO) base = STAIR_MO;
            int rel = id - base, dir = rel / 2, top = rel % 2;
            v = base + ((dir + r) & 3) * 2 + top;
        } else if (id == LOG_X || id == LOG_Z) {
            if (r % 2 == 1) v = id == LOG_X ? LOG_Z : LOG_X;
        } else if (id == WLOG_X || id == WLOG_Z) {
            if (r % 2 == 1) v = id == WLOG_X ? WLOG_Z : WLOG_X;
        } else if (id == DLOG_X || id == DLOG_Z) {
            if (r % 2 == 1) v = id == DLOG_X ? DLOG_Z : DLOG_X;
        }
        set(p[0], lvl + y, p[1], v);
    }

    /**
     * @param wh hauteur des murs ; style 0 = maison, 1 = chapelle (haute), 3 = grande halle ; porch = petit perron en facade ; sub = aile (pas de porte, mur plein)
     */
    private void gabled(int x0, int z0, int w, int d, int wh, int r, int style, boolean porch, boolean sub) {
        if (w % 2 == 0) w++;
        int hh = style == 3 ? wh + 1 : wh;
        int cxl = w / 2;
        int wm = (hash(x0, 3, z0) >>> 4) % 3;                                    // 0 torchis a colombages, 1 torchis uni, 2 planches patinees
        for (int lx = -1; lx <= w; lx++) for (int lz = -1; lz <= d; lz++) {
            boolean inside = lx >= 0 && lx < w && lz >= 0 && lz < d;
            if (inside) {
                boolean wall = lx == 0 || lx == w - 1 || lz == 0 || lz == d - 1;
                put(r, x0, z0, w, d, lx, 0, lz, wall ? COBBLE : PLANKS, false);                         // plancher
                if (!wall) for (int y = 1; y <= hh + 4; y++) put(r, x0, z0, w, d, lx, y, lz, AIR, false);
            }
        }
        for (int lx = 0; lx < w; lx++) for (int lz = 0; lz < d; lz++) {
            boolean wall = lx == 0 || lx == w - 1 || lz == 0 || lz == d - 1;
            if (!wall) continue;
            boolean corner = (lx == 0 || lx == w - 1) && (lz == 0 || lz == d - 1);
            boolean postX = (lx == 0 || lx == w - 1) && lz % 4 == 2;
            for (int y = 1; y <= hh; y++) {
                int id;
                if (y == 1) id = rnd(x0 + lx, y, z0 + lz) < 0.2 ? MOSSY_COBBLE : COBBLE;
                else if (corner || postX) id = DLOG_Y;
                else if (y == hh) id = style >= 1 ? CARVED : WPLANKS;                                  // poutre sous l'avant-toit
                else id = wm == 0 ? TIMBER : wm == 1 ? PLASTER : (rnd(x0 + lx, y, z0 + lz) < 0.2 ? PLANKS : WPLANKS);
                boolean winRow = y == 3 || (hh >= 5 && y == 4);
                boolean longSide = lx == 0 || lx == w - 1;
                if (winRow && !corner && !postX && ((longSide && lz % 4 == 0) || (!longSide && lx == cxl - 2 || (!longSide && lx == cxl + 2 && lz == 0)))) id = GLASSPANE;
                put(r, x0, z0, w, d, lx, y, lz, id, false);
            }
        }
        // porte au pignon sud local, perron de pierre
        if (!sub) {
            put(r, x0, z0, w, d, cxl, 1, d - 1, AIR, false); put(r, x0, z0, w, d, cxl, 2, d - 1, AIR, false);
            put(r, x0, z0, w, d, cxl, 3, d - 1, DPLANKS, false);
            put(r, x0, z0, w, d, cxl, 0, d, STAIR_SB + 0 * 2, true);
            put(r, x0, z0, w, d, cxl - 1, 0, d, COBBLE, false); put(r, x0, z0, w, d, cxl + 1, 0, d, COBBLE, false);
            put(r, x0, z0, w, d, cxl - 1, 1, d - 1, DLOG_Y, false); put(r, x0, z0, w, d, cxl + 1, 1, d - 1, DLOG_Y, false);
            put(r, x0, z0, w, d, cxl - 1, 2, d - 1, DLOG_Y, false); put(r, x0, z0, w, d, cxl + 1, 2, d - 1, DLOG_Y, false);
            put(r, x0, z0, w, d, cxl, 1, d, LAMP, false);
        } else {
            for (int y = 1; y <= hh; y++) put(r, x0, z0, w, d, cxl, y, d - 1, AIR, false);
        }
        // toit de chaume : deux pans de marches de bambou, avancee d'un bloc, pignons de planches
        int layers = (w - 1) / 2;
        for (int k = 0; k <= layers; k++) {
            int ya = hh + 1 + k;
            for (int lz = -1; lz <= d; lz++) {
                if (k < layers) {
                    put(r, x0, z0, w, d, k - 1, ya, lz, STAIR_MO + E * 2, true);
                    put(r, x0, z0, w, d, w - k, ya, lz, STAIR_MO + W_ * 2, true);
                } else {
                    put(r, x0, z0, w, d, cxl, ya, lz, MOSAIC, false);
                }
            }
            if (k < layers + 1) for (int lx = k; lx <= w - 1 - k; lx++) {                              // pignons
                int yy = ya - (k < layers ? 0 : 0);
                for (int lzp : new int[]{0, d - 1}) {
                    int id = (k == layers) ? DPLANKS : DPLANKS;
                    if (lx >= k && lx <= w - 1 - k && !(k < layers && (lx == k || lx == w - 1 - k) && false)) {
                        if (get(map(r, x0, z0, w, d, lx, lzp)[0], lvl + yy, map(r, x0, z0, w, d, lx, lzp)[1]) == AIR || true) {
                            if (k >= 1 && lx > k && lx < w - 1 - k) put(r, x0, z0, w, d, lx, yy, lzp, WPLANKS, false);
                        }
                    }
                }
            }
        }
        // pignon : plein sous le toit
        for (int k = 0; k < layers; k++) for (int lx = k + 1; lx <= w - 2 - k; lx++) for (int lzp : new int[]{0, d - 1}) put(r, x0, z0, w, d, lx, hh + 1 + k, lzp, WPLANKS, false);
        // ornements : poutres croisees en V (tetes de chevaux) a chaque bout du faitage, lanterne au plafond
        if (!sub) {
            int yr = hh + 1 + layers;
            for (int lzp : new int[]{-1, d}) {
                put(r, x0, z0, w, d, cxl, yr + 1, lzp, DFENCE, false);
                for (int s = -1; s <= 1; s += 2) {
                    put(r, x0, z0, w, d, cxl + s, yr + 1, lzp, DFENCE, false);
                    put(r, x0, z0, w, d, cxl + s, yr + 2, lzp, DFENCE, false);
                    put(r, x0, z0, w, d, cxl + 2 * s, yr + 3, lzp, DFENCE, false);
                }
            }
            put(r, x0, z0, w, d, cxl, hh + layers, d / 2, CHAIN, false);
            put(r, x0, z0, w, d, cxl, hh + layers - 1, d / 2, LANTERN, false);
            put(r, x0, z0, w, d, 1, 1, d - 2, BARREL, false);
            put(r, x0, z0, w, d, w - 2, 1, 1, SHELF, false);
            put(r, x0, z0, w, d, w - 2, 2, 1, SHELF, false);
            // interieur habite : table, tapis, etabli, jarre
            put(r, x0, z0, w, d, cxl, 1, d / 2 + 1, WFENCE, false); put(r, x0, z0, w, d, cxl, 2, d / 2 + 1, PLATE, false);
            for (int dx = -1; dx <= 1; dx += 2) { put(r, x0, z0, w, d, cxl + dx, 1, d / 2 + 1, CARPET_G, false); put(r, x0, z0, w, d, cxl + dx, 1, d / 2 - 1 + 2 * 0, CARPET_G, false); }
            put(r, x0, z0, w, d, w - 2, 1, d - 3, CRAFT, false);
            put(r, x0, z0, w, d, 1, 1, 1, COMPOSTER, false);
            // cheminee de pierre contre le pignon ouest : un feu de camp au sommet laisse monter la fumee
            int chz = Math.max(2, d / 2 - 2);
            for (int y = 1; y <= hh + 3; y++) put(r, x0, z0, w, d, -2, y, chz, y % 3 == 0 ? MOSSY_COBBLE : COBBLE, false);
            put(r, x0, z0, w, d, -2, hh + 4, chz, CAMPFIRE, false);
            // devant : pot de fleurs, tonneau, tas de bois contre le mur est
            put(r, x0, z0, w, d, cxl + 2, 1, d, (hash(x0, 5, z0) & 1) == 0 ? POT_TULIP : POT_DAND, false);
            put(r, x0, z0, w, d, cxl - 2, 1, d, BARREL, false);
            for (int lz = 2; lz <= Math.min(d - 3, 6); lz++) {
                put(r, x0, z0, w, d, w + 1, 1, lz, WLOG_Z, false); put(r, x0, z0, w, d, w + 2, 1, lz, WLOG_Z, false);
                if (lz >= 3 && lz <= Math.min(d - 4, 5)) put(r, x0, z0, w, d, w + 1, 2, lz, WLOG_Z, false);
            }
        }
    }

    // ------------------------------------------------------------------ palais d'Or (Meduseld)
    private void hall(int cx, int cz) {
        int tw = 33, td = 50, tx0 = cx - tw / 2, tz0 = cz - 31;                 // terrasse : x de tx0 a tx0 + tw - 1, z de tz0 a tz0 + td - 1
        int ty = H0 + 3;                                                        // niveau debout sur la terrasse
        for (int x = tx0; x < tx0 + tw; x++) for (int z = tz0; z < tz0 + td; z++) {
            for (int y = H0 - 3; y < ty; y++) set(x, y, z, y >= ty - 1 ? ((x + z) % 5 == 0 ? SB : (rnd(x, 40, z) < 0.3 ? ANDESITE : STONE)) : (rnd(x, y, z) < 0.5 ? SB : (rnd(x, y, z) < 0.5 ? SB_MOSS : COBBLE)));
        }
        for (int x = tx0; x < tx0 + tw; x++) for (int z = tz0; z < tz0 + td; z++) {                // parapet de la terrasse
            boolean edge = x == tx0 || x == tx0 + tw - 1 || z == tz0 || z == tz0 + td - 1;
            if (edge && !(z == tz0 + td - 1 && Math.abs(x - cx) <= 5)) { set(x, ty, z, rnd(x, 41, z) < 0.55 ? WALL_SB : AIR); if (((x + z) & 1) == 0) set(x, ty, z, WALL_SB); }
        }
        // escalier de pierre au sud (de la place vers la terrasse) puis marches de bois
        for (int k = 0; k < 3; k++) for (int x = cx - 5; x <= cx + 5; x++) for (int z = tz0 + td - 1 + (2 - k); z <= tz0 + td - 1 + (2 - k); z++) {
            for (int y = H0 - 1; y < H0 + k; y++) set(x, y, z, STONE);
            set(x, H0 + k, z, stair(STAIR_SB, N, false));
        }
        for (int x = cx - 5; x <= cx + 5; x++) for (int z = tz0 + td; z <= tz0 + td + 2; z++) { if (z == tz0 + td + 2 - 0 && false) continue; }
        // palais : mur de 17 x 33, murs de 7 de haut, pignon sud (porche a trois arcades)
        int hw = 17, hd = 33, hx0 = cx - hw / 2, hz0 = tz0 + 4, wh = 7;
        for (int lx = 0; lx < hw; lx++) for (int lz = 0; lz < hd; lz++) {
            int x = hx0 + lx, z = hz0 + lz;
            set(x, ty - 1, z, DPLANKS);
            boolean wall = lx == 0 || lx == hw - 1 || lz == 0 || lz == hd - 1;
            if (!wall) { set(x, ty, z, (lx >= hw / 2 - 1 && lx <= hw / 2 + 1 && lz >= 3 && lz <= hd - 6) ? CARPET : PLANKS); for (int y = 1; y <= wh + 9; y++) set(x, ty + y, z, AIR); continue; }
        }
        for (int lx = 0; lx < hw; lx++) for (int lz = 0; lz < hd; lz++) {
            int x = hx0 + lx, z = hz0 + lz;
            boolean wall = lx == 0 || lx == hw - 1 || lz == 0 || lz == hd - 1;
            if (!wall) continue;
            set(x, ty, z, PLANKS);
            boolean corner = (lx == 0 || lx == hw - 1) && (lz == 0 || lz == hd - 1);
            boolean pillar = ((lx == 0 || lx == hw - 1) && lz % 4 == 2) || (lz == hd - 1 && (lx % 5 == 0 || lx == 1 || lx == hw - 2));
            for (int y = 1; y <= wh; y++) {
                int id;
                if (y <= 2) id = rnd(x, y, z) < 0.18 ? MOSSY_COBBLE : (rnd(x, y, z) < 0.5 ? SB : COBBLE);
                else if (corner || pillar) id = DSTRIP_Y;
                else id = y == 3 || y == wh ? CARVED : rnd(x, y, z) < 0.3 ? PLANKS : DPLANKS;
                if ((lx == 0 || lx == hw - 1) && lz % 4 == 0 && (y == 4 || y == 5) && !pillar && !corner) id = GLASSPANE;
                set(x, ty + y, z, id);
            }
        }
        // facade sud : trois arcades (hauteur 5) entre des colonnes, balustrade au-dessus, soleil d'or dans le pignon
        for (int arc = 0; arc < 3; arc++) {
            int ax = hx0 + 2 + arc * 5;
            for (int dx = 0; dx < 3; dx++) for (int y = 1; y <= 5; y++) {
                if (y == 5 && dx != 1) continue;
                set(ax + dx, ty + y, hz0 + hd - 1, AIR);
            }
            set(ax + 1, ty + 5, hz0 + hd - 1, AIR);
            set(ax, ty + 6, hz0 + hd - 1, DSTRIP_X); set(ax + 2, ty + 6, hz0 + hd - 1, DSTRIP_X); set(ax + 1, ty + 6, hz0 + hd - 1, DPLANKS);
            set(ax, ty + 5, hz0 + hd - 1, stair(STAIR_SB, E, true)); set(ax + 2, ty + 5, hz0 + hd - 1, stair(STAIR_SB, W_, true));
        }
        for (int lx = 1; lx < hw - 1; lx++) set(hx0 + lx, ty + 7, hz0 + hd - 1, DFENCE);
        for (int lx = 0; lx < hw; lx++) set(hx0 + lx, ty + 7, hz0 + hd, lx % 2 == 0 ? DFENCE : AIR);
        for (int k = 0; k < 2; k++) for (int lx = 0; lx < hw; lx++) set(hx0 + lx, ty + 1 + k, hz0 + hd, AIR);
        // toit : chaume de bambou, avancee de deux blocs a chaque pignon
        int layers = (hw - 1) / 2;
        for (int k = 0; k <= layers; k++) {
            int ya = ty + wh + 1 + k;
            for (int z = hz0 - 2; z <= hz0 + hd + 1; z++) {
                if (k < layers) {
                    set(hx0 + k - 1, ya, z, stair(STAIR_MO, E, false));
                    set(hx0 + hw - k, ya, z, stair(STAIR_MO, W_, false));
                    if (k >= 1 && z >= hz0 - 1 && z <= hz0 + hd) { set(hx0 + k, ya - 1, z, stair(STAIR_MO, E, true)); set(hx0 + hw - 1 - k, ya - 1, z, stair(STAIR_MO, W_, true)); }
                } else set(hx0 + hw / 2, ya, z, MOSAIC);
            }
            for (int lx = k; lx <= hw - 1 - k; lx++) for (int z : new int[]{hz0, hz0 + hd - 1}) if (lx > k && lx < hw - 1 - k) set(hx0 + lx, ya, z, DPLANKS);
        }
        // poutres et lanternes dans la salle
        for (int lz = 4; lz < hd - 2; lz += 4) {
            for (int lx = 1; lx < hw - 1; lx++) set(hx0 + lx, ty + wh, hz0 + lz, DLOG_X);
            set(hx0 + hw / 2 - 3, ty + wh - 1, hz0 + lz, CHAIN); set(hx0 + hw / 2 - 3, ty + wh - 2, hz0 + lz, LANTERN);
            set(hx0 + hw / 2 + 3, ty + wh - 1, hz0 + lz, CHAIN); set(hx0 + hw / 2 + 3, ty + wh - 2, hz0 + lz, LANTERN);
        }
        for (int lz = 6; lz < hd - 6; lz += 6) { set(hx0 + hw / 2, ty, hz0 + lz, CAMPFIRE); }
        // estrade et trone au fond
        box(hx0 + 4, ty, hz0 + 1, hx0 + hw - 5, ty, hz0 + 3, SB);
        box(hx0 + 6, ty + 1, hz0 + 1, hx0 + hw - 7, ty + 1, hz0 + 2, SB);
        set(hx0 + hw / 2, ty + 2, hz0 + 1, stair(STAIR_SB, S, false));
        set(hx0 + hw / 2, ty + 2, hz0, DLOG_Y); set(hx0 + hw / 2, ty + 3, hz0, GOLD);
        for (int lx = 4; lx < hw - 4; lx += 2) { set(hx0 + lx, ty + 1, hz0 + hd - 6, BARREL); }
        // soleil d'or dans le pignon du sud
        int sy = ty + wh + 4;
        set(hx0 + hw / 2, sy, hz0 + hd - 1, GOLD);
        for (int[] d4 : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) set(hx0 + hw / 2 + d4[0], sy + d4[1], hz0 + hd - 1, GOLD);
        // colonnes sculptees de l'avant, tetes de chevaux au sommet des pignons
        for (int s = -1; s <= 1; s += 2) {
            int px = s < 0 ? hx0 - 1 : hx0 + hw, pz = hz0 + hd;
            for (int y = 1; y <= wh + 6; y++) set(px, ty + y, pz, DSTRIP_Y);
            set(px, ty + wh + 7, pz, GOLD);
            set(px + s, ty + wh + 8, pz, DFENCE); set(px + s, ty + wh + 9, pz, DFENCE); set(px + 2 * s, ty + wh + 10, pz, DFENCE);
            set(px - s, ty + wh + 8, pz, DFENCE); set(px - s, ty + wh + 9, pz, DFENCE); set(px - 2 * s, ty + wh + 10, pz, DFENCE);
        }
        int yr = ty + wh + 1 + layers;
        for (int zz : new int[]{hz0 - 2, hz0 + hd + 1}) {
            set(hx0 + hw / 2, yr + 1, zz, DFENCE);
            for (int s = -1; s <= 1; s += 2) { set(hx0 + hw / 2 + s, yr + 1, zz, DFENCE); set(hx0 + hw / 2 + s, yr + 2, zz, DFENCE); set(hx0 + hw / 2 + 2 * s, yr + 3, zz, DFENCE); set(hx0 + hw / 2 + 2 * s, yr + 4, zz, DFENCE); }
        }
        // lucarne de fumee sur le faitage
        int lzm = hz0 + hd / 2;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = 1; y <= 2; y++) set(hx0 + hw / 2 + dx, yr + y, lzm + dz, DPLANKS);
        for (int dz = -3; dz <= 3; dz++) { set(hx0 + hw / 2, yr + 3, lzm + dz, MOSAIC); set(hx0 + hw / 2 - 1, yr + 2, lzm + dz, stair(STAIR_MO, E, false)); set(hx0 + hw / 2 + 1, yr + 2, lzm + dz, stair(STAIR_MO, W_, false)); }
        // quatre bas-cotes a pignons de chaque cote (ailes basses de 5 de large)
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 3; k++) {
            int az = hz0 + 5 + k * 9;
            int ax0 = s < 0 ? hx0 - 5 : hx0 + hw;
            wing(ax0, az, s, ty);
        }
    }

    /** aile basse a pignon qui s'appuie sur le long cote du palais (faitage perpendiculaire) */
    private void wing(int ax0, int az0, int s, int ty) {
        int ww = 5, dd = 5, wh = 4;
        for (int lx = 0; lx < ww; lx++) for (int lz = 0; lz < dd; lz++) {
            int x = ax0 + lx, z = az0 + lz;
            boolean wall = lx == 0 || lx == ww - 1 || lz == 0 || lz == dd - 1;
            set(x, ty - 1, z, DPLANKS); set(x, ty, z, PLANKS);
            for (int y = 1; y <= wh + 4; y++) set(x, ty + y, z, AIR);
            if (wall) for (int y = 1; y <= wh; y++) {
                boolean corner = (lx == 0 || lx == ww - 1) && (lz == 0 || lz == dd - 1);
                set(x, ty + y, z, y == 1 ? COBBLE : corner ? DLOG_Y : (y == 3 && lx == (s < 0 ? 0 : ww - 1) && lz == 2) ? GLASSPANE : DPLANKS);
            }
        }
        // toit : faitage le long de x (perpendiculaire au grand toit) : pans nord et sud
        for (int k = 0; k <= 3; k++) {
            int ya = ty + wh + 1 + k;
            for (int lx = -1; lx < ww + (s < 0 ? 1 : 1); lx++) {
                int x = ax0 + lx;
                if (k < 2) { set(x, ya, az0 + k - 1, stair(STAIR_MO, S, false)); set(x, ya, az0 + dd - k, stair(STAIR_MO, N, false)); }
                else set(x, ya, az0 + 2, MOSAIC);
            }
            for (int lz = k; lz <= dd - 1 - k; lz++) { int x = s < 0 ? ax0 : ax0 + ww - 1; if (lz > k - 1 && k < 2 && lz >= k + 0 && lz <= dd - 1 - k) set(x, ya, az0 + lz, DPLANKS); }
        }
    }
}
