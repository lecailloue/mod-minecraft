package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan de la Comte (Hobbiteville) : 240 x 240 blocs. Collines rondes dont le pied est creuse de terriers (facade ronde, porte ronde de couleur), une grande colline
 * portant l'Arbre de la fete et Cul-de-Sac, des chemins de terre qui serpentent, un lac que prolonge un ruisseau (pont de pierre, moulin, auberge du Dragon Vert),
 * des potagers et des arbres. Grille sans dependance a Minecraft. Repere local : x vers l'est, z vers le sud ; y = 0 : premier niveau d'air au-dessus du sol plat.
 * hg = hauteur de terre ajoutee (blocs y = 0 .. hg - 1), ou - profondeur de l'eau si negatif.
 */
public final class ShirePlan {
    public static final int W = 240, D = 240;
    public static final int GRASS = 0, PATH = 1, GARDEN = 2, FIELD = 3;

    public final int[] hg = new int[W * D];
    public final byte[] sf = new byte[W * D];
    public final boolean[] res = new boolean[W * D];
    public final List<Hole> holes = new ArrayList<>();
    public final List<int[]> trees = new ArrayList<>();         // x, z, genre (0 chene, 1 grand chene, 2 buisson)
    public final List<int[]> fields = new ArrayList<>();        // x0, z0, x1, z1, culture
    
    // lieux fixes (reperes locaux)
    public static final int PARTY_X = 124, PARTY_Z = 38;
    public static final int BRIDGE_X = 170, STREAM_Z0 = 179, STREAM_Z1 = 186, STREAM_X1 = 216;
    public static final int MILL_X0 = 197, MILL_Z0 = 167, INN_X0 = 176, INN_Z0 = 194;

    private static ShirePlan inst;
    public static synchronized ShirePlan get() { if (inst == null) inst = new ShirePlan(); return inst; }

    public static int idx(int x, int z) { return z * W + x; }
    public static boolean in(int x, int z) { return x >= 0 && x < W && z >= 0 && z < D; }
    public int h(int x, int z) { return in(x, z) ? hg[idx(x, z)] : 0; }
    public boolean water(int x, int z) { return in(x, z) && hg[idx(x, z)] < 0; }

    public static int hash(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return (int) (h & 0x7fffffff);
    }
    public static double rnd(int x, int z, int salt) { return (hash(x, z, salt) % 100000) / 100000.0; }

    /** un terrier : (fx, fz) = centre de la facade, (ox, oz) = direction de sortie ; u = decalage lateral, v = profondeur dans la colline (v < 0 : devant la porte) */
    public static final class Hole {
        public final int fx, fz, ox, oz, rw, door, wall;
        public final double h, dv, ru;
        public final boolean big;
        public Hole(int fx, int fz, int ox, int oz, boolean big, int door, int wall) {
            this.fx = fx; this.fz = fz; this.ox = ox; this.oz = oz; this.big = big; this.door = door; this.wall = wall;
            this.rw = big ? 12 : 7; this.h = big ? 13.5 : 8; this.dv = big ? 26 : 15; this.ru = rw + 1.3;
        }
        public int x(int u, int v) { return fx + u * (-oz) - v * ox; }
        public int z(int u, int v) { return fz + u * ox - v * oz; }
        public double dome(double u, double v) { return h * Math.sqrt(Math.max(0, 1 - (u / ru) * (u / ru) - (v / dv) * (v / dv))); }
        public int domeH(int u, int v) { return (int) Math.round(dome(u, v)); }
    }

    private void hill(int cx, int cz, int rx, int rz, double top, int salt) {
        for (int z = Math.max(0, cz - rz); z <= Math.min(D - 1, cz + rz); z++) for (int x = Math.max(0, cx - rx); x <= Math.min(W - 1, cx + rx); x++) {
            double d2 = ((x - cx) / (double) rx) * ((x - cx) / (double) rx) + ((z - cz) / (double) rz) * ((z - cz) / (double) rz);
            if (d2 >= 1) continue;
            double p = 1 - d2;
            double n = (MapChunkGenerator.fbm(x * 0.06, z * 0.06, salt) - 0.5) * 2.6 * Math.min(1, p * 3);
            int hh = (int) Math.round(top * Math.pow(p, 0.8) + n);
            if (hh > hg[idx(x, z)]) hg[idx(x, z)] = hh;
        }
    }

    private void road(int[][] pts, double half) {
        for (int i = 0; i + 1 < pts.length; i++) {
            double dx = pts[i + 1][0] - pts[i][0], dz = pts[i + 1][1] - pts[i][1];
            int n = (int) Math.ceil(Math.hypot(dx, dz) * 2);
            for (int k = 0; k <= n; k++) {
                double cx = pts[i][0] + dx * k / n, cz = pts[i][1] + dz * k / n;
                // le chemin ondule un peu
                cx += Math.sin(cz * 0.11 + i) * 0.8; cz += Math.sin(cx * 0.13 + i * 2) * 0.8;
                int r = (int) Math.ceil(half + 2);
                for (int zz = (int) cz - r; zz <= (int) cz + r; zz++) for (int xx = (int) cx - r; xx <= (int) cx + r; xx++) {
                    if (!in(xx, zz)) continue;
                    double d = Math.hypot(xx - cx, zz - cz);
                    int ix = idx(xx, zz);
                    if (d <= half && hg[ix] >= 0) {
                        sf[ix] = PATH;
                        if (hg[ix] > 0 && hg[ix] <= 2) hg[ix] = 0;
                    }
                    if (d <= half + 2) res[ix] = true;
                }
            }
        }
    }

    private void clear(int x0, int z0, int x1, int z1, int surf) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) {
            if (!in(x, z)) continue;
            int ix = idx(x, z);
            if (hg[ix] >= 0) { hg[ix] = 0; if (surf >= 0) sf[ix] = (byte) surf; }
            res[ix] = true;
        }
    }

    private ShirePlan() {
        // ----- collines
        hill(128, 40, 52, 34, 26, 61);       // Cul-de-Sac et l'Arbre de la fete
        hill(52, 56, 44, 30, 17, 62);
        hill(200, 64, 34, 34, 18, 63);
        hill(40, 150, 30, 26, 11, 64);
        hill(204, 130, 28, 24, 12, 65);
        // ----- terriers : facade, direction, grand ?, couleur de porte (0 vert, 1 bleu, 2 rouge, 3 jaune), mur (0 platre jaune, 1 gres, 2 brique)
        holes.add(new Hole(128, 70, 0, 1, true, 0, 0));
        int[] bx = {22, 44, 66, 88};
        int[] bd = {1, 2, 3, 0}, bw = {0, 2, 1, 0};
        for (int i = 0; i < 4; i++) holes.add(new Hole(bx[i], 80, 0, 1, false, bd[i], bw[i]));
        int[] cz = {46, 64, 82};
        for (int i = 0; i < 3; i++) holes.add(new Hole(178, cz[i], -1, 0, false, new int[]{2, 0, 1}[i], new int[]{1, 0, 2}[i]));
        holes.add(new Hole(62, 140, 1, 0, false, 3, 0));
        holes.add(new Hole(62, 160, 1, 0, false, 1, 2));
        holes.add(new Hole(190, 142, 0, 1, false, 0, 1));
        holes.add(new Hole(212, 142, 0, 1, false, 2, 0));
        // ----- plateau plat au sommet, sous l'Arbre de la fete
        int c0 = hg[idx(PARTY_X, PARTY_Z)];
        for (int z = PARTY_Z - 18; z <= PARTY_Z + 18; z++) for (int x = PARTY_X - 18; x <= PARTY_X + 18; x++) {
            double d = Math.hypot(x - PARTY_X, z - PARTY_Z);
            if (d > 17 || !in(x, z)) continue;
            double t = d <= 10 ? 1 : (17 - d) / 7.0;
            hg[idx(x, z)] = (int) Math.round(hg[idx(x, z)] * (1 - t) + c0 * t);
            res[idx(x, z)] = true;
        }
        // ----- la colline s'efface devant chaque terrier (jardin plat), sauf le dome du terrier lui-meme
        for (Hole h : holes) {
            int R = (int) Math.ceil(h.ru) + 2, span = 8;
            for (int u = -R - span; u <= R + span; u++) for (int v = -13 - span; v <= span; v++) {
                int x = h.x(u, v), z = h.z(u, v);
                if (!in(x, z)) continue;
                int du = Math.max(Math.abs(u) - R, 0), dv = v < -13 ? -13 - v : v > 0 ? v : 0;
                double d = Math.hypot(du, dv);
                if (d < span) hg[idx(x, z)] = (int) Math.floor(hg[idx(x, z)] * d / span);
            }
        }
        // ----- domes des terriers
        for (Hole h : holes) {
            int r = (int) Math.ceil(h.ru), dvv = (int) Math.ceil(h.dv);
            for (int u = -r; u <= r; u++) for (int v = 0; v <= dvv; v++) {
                int x = h.x(u, v), z = h.z(u, v);
                if (!in(x, z)) continue;
                int hh = h.domeH(u, v);
                if (hh > hg[idx(x, z)]) hg[idx(x, z)] = hh;
                res[idx(x, z)] = true;
            }
        }
        // ----- lac (ellipse) et ruisseau qui va vers le moulin
        for (int z = 150; z <= 215; z++) for (int x = 70; x <= STREAM_X1 + 2; x++) {
            double ex = (x - 118) / 34.0, ez = (z - 182) / 20.0, d2 = ex * ex + ez * ez;
            int depth = 0;
            if (d2 < 1) depth = 1 + (int) Math.round(4 * Math.pow(1 - d2, 0.6));
            if (x >= 140 && x <= STREAM_X1 && z >= STREAM_Z0 && z <= STREAM_Z1) {
                double cap = x > STREAM_X1 - 3 ? (STREAM_X1 + 1 - x) / 3.0 : 1;
                int dd = (int) Math.round((z == STREAM_Z0 || z == STREAM_Z1 ? 2 : 3) * cap);
                if (dd > depth) depth = dd;
            }
            if (depth > 0) { hg[idx(x, z)] = -depth; sf[idx(x, z)] = GRASS; }
        }
        for (int z = 148; z <= 217; z++) for (int x = 68; x <= STREAM_X1 + 4; x++) {
            boolean near = false;
            for (int dz = -3; dz <= 3 && !near; dz++) for (int dx = -3; dx <= 3; dx++) if (water(x + dx, z + dz)) { near = true; break; }
            if (near) res[idx(x, z)] = true;
        }
        // ----- devant chaque terrier : jardin plat
        for (Hole h : holes) {
            int R = (int) Math.ceil(h.ru) + 2;
            for (int u = -R; u <= R; u++) for (int v = -1; v >= -13; v--) {
                int x = h.x(u, v), z = h.z(u, v);
                if (!in(x, z)) continue;
                int ix = idx(x, z);
                if (hg[ix] < 0) continue;
                hg[ix] = 0; sf[ix] = GARDEN; res[ix] = true;
            }
        }
        // ----- emplacements : auberge, moulin, pont, potagers
        clear(INN_X0 - 6, INN_Z0 - 8, INN_X0 + 26, INN_Z0 + 22, -1);
        clear(MILL_X0 - 3, MILL_Z0 - 3, MILL_X0 + 15, MILL_Z0 + 14, -1);
        clear(BRIDGE_X - 6, STREAM_Z0 - 10, BRIDGE_X + 6, STREAM_Z1 + 10, -1);
        for (int z = STREAM_Z0; z <= STREAM_Z1; z++) for (int x = BRIDGE_X - 6; x <= BRIDGE_X + 6; x++) if (hg[idx(x, z)] >= 0) hg[idx(x, z)] = -3;
        fields.add(new int[]{14, 194, 44, 228, 0});
        fields.add(new int[]{48, 194, 76, 228, 1});
        fields.add(new int[]{206, 194, 228, 226, 2});
        for (int[] f : fields) for (int z = f[1]; z <= f[3]; z++) for (int x = f[0]; x <= f[2]; x++) { int ix = idx(x, z); hg[ix] = 0; sf[ix] = FIELD; res[ix] = true; }
        // ----- chemins
        road(new int[][]{{2, 99}, {40, 97}, {80, 93}, {118, 97}, {150, 92}, {176, 94}, {200, 100}, {237, 98}}, 1.6);
        road(new int[][]{{128, 96}, {128, 84}}, 1.5);                          // vers Cul-de-Sac
        road(new int[][]{{118, 97}, {124, 120}, {140, 138}, {152, 150}, {168, 152}, {BRIDGE_X, 160}, {BRIDGE_X, 176}}, 1.6);
        road(new int[][]{{BRIDGE_X, 190}, {BRIDGE_X + 6, 191}, {INN_X0 + 9, 192}, {INN_X0 + 9, 193}}, 1.6);
        road(new int[][]{{88, 96}, {74, 112}, {70, 130}, {72, 148}, {72, 170}}, 1.5);
        road(new int[][]{{176, 94}, {196, 110}, {200, 140}, {202, 156}, {205, 166}}, 1.5);
        road(new int[][]{{34, 150}, {50, 190}}, 1.2);
        // ----- arbres et buissons
        for (int gz = 6; gz < D - 6; gz += 8) for (int gx = 6; gx < W - 6; gx += 8) {
            int x = gx + hash(gx, gz, 1) % 7 - 3, z = gz + hash(gx, gz, 2) % 7 - 3;
            if (!in(x, z) || hg[idx(x, z)] < 0 || res[idx(x, z)]) continue;
            boolean ok = true;
            for (int d = 0; d < 8 && ok; d++) {
                int xx = x + (int) Math.round(Math.cos(d * 0.785) * 3), zz = z + (int) Math.round(Math.sin(d * 0.785) * 3);
                if (in(xx, zz) && (res[idx(xx, zz)] || hg[idx(xx, zz)] < 0)) ok = false;
            }
            if (!ok) continue;
            // l'arbre de la fete : rien dans son ombre
            if (Math.hypot(x - PARTY_X, z - PARTY_Z) < 16) continue;
            double r = rnd(x, z, 3);
            if (r < 0.42) trees.add(new int[]{x, z, r < 0.08 ? 1 : r < 0.3 ? 0 : 2});
        }
    }
}
