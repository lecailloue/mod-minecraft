package net.ringsofash.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan de Minas Morgul : une grille de blocs abstraits (identifiants de palette), sans aucune dependance a Minecraft.
 * Repere local : x de 0 a 96 (l'axe du chateau est x = 48), z de 0 a 96 (la porte est a z = 0, vers l'ouest dans le monde),
 * y = 0 est le sol naturel (premier bloc d'air), y negatif = sous le sol (douves, fondations).
 * {@link Buildings} traduit les identifiants en blocs ; le plan peut aussi etre dessine hors du jeu pour verification.
 */
public final class MorgulPlan {
    public static final int W = 97, D = 97, OFF = 6, H = 128;           // y local de -OFF a H - OFF - 1
    public static final int CX = 48;                                    // axe du chateau
    public static final int TX = 48, TZ = 77;                           // axe de la grande tour
    public static final int FLOOR = 4;                                  // niveau de marche du socle (sol plein jusqu'a y = 3)

    public static final byte AIR = 0, BB = 1, PBB = 2, CBB = 3, TILE = 4, BLACKSTONE = 5, GLOW = 6, GLASS = 7, BARS = 8, FIRE = 9,
            SOUL_SOIL = 10, WATER = 11, LANTERN = 12, SLAB_BOT = 13, SLAB_TOP = 14, WALL = 15, STAIR = 16;      // STAIR + dir * 2 + (haut ? 1 : 0)
    public static final int N = 0, E = 1, S = 2, W_ = 3;                // directions : N = -z, E = +x, S = +z, W = -x

    private final byte[] g = new byte[W * H * D];
    private final int[] top = new int[W * D];
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
    /** pierre noire : melange de briques */
    private byte mat(int x, int y, int z) {
        int r = (hash(x, y, z) >>> 4) & 127;
        return r < 96 ? BB : r < 110 ? PBB : r < 122 ? CBB : TILE;
    }
    private void box(int x0, int y0, int z0, int x1, int y1, int z1, int id) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, id);
    }
    private void boxM(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, mat(x, y, z));
    }
    private void air(int x0, int y0, int z0, int x1, int y1, int z1) { box(x0, y0, z0, x1, y1, z1, AIR); }
    private static int stair(int dir, boolean topHalf) { return STAIR + dir * 2 + (topHalf ? 1 : 0); }
    /** demi-largeur d'une arche en ogive a la hauteur y (ressaut a spring, pointe a apex) */
    private static int arch(int y, int spring, int apex, int hw) {
        if (y <= spring) return hw;
        if (y > apex) return -1;
        double t = (y - spring) / (double) (apex - spring + 1);
        return (int) Math.round(hw * (1 - Math.pow(t, 1.8)));
    }

    /** mur-aileron le long de x : sommet concave, pointu aux extremites, fentes lumineuses pendant depuis le haut. side : 0 face exterieure en z0, 1 en z0 + th - 1, 2 les deux */
    private void finX(int x0, int x1, int z0, int th, int base, int amp, int side) {
        for (int x = x0; x <= x1; x++) {
            double t = x1 == x0 ? 0.5 : (x - x0) / (double) (x1 - x0), u = Math.abs(2 * t - 1);
            int tp = base + (int) Math.round(amp * Math.pow(u, 2.3));
            for (int z = z0; z < z0 + th; z++) for (int y = FLOOR; y <= tp; y++) set(x, y, z, mat(x, y, z));
            if ((x - x0) % 4 != 0 && x - x0 >= 2 && x1 - x >= 2) {
                int len = 6 + (hash(x, 1, z0) >>> 3 & 7) + (hash(x, 5, z0) >>> 5 & 3);
                for (int y = tp - 1; y > tp - 1 - len && y >= 9; y--) {
                    if (side != 1) set(x, y, z0, GLOW);
                    if (side != 0) set(x, y, z0 + th - 1, GLOW);
                }
            }
        }
    }
    private void finZ(int z0, int z1, int x0, int th, int base, int amp, int side) {
        for (int z = z0; z <= z1; z++) {
            double t = z1 == z0 ? 0.5 : (z - z0) / (double) (z1 - z0), u = Math.abs(2 * t - 1);
            int tp = base + (int) Math.round(amp * Math.pow(u, 2.3));
            for (int x = x0; x < x0 + th; x++) for (int y = FLOOR; y <= tp; y++) set(x, y, z, mat(x, y, z));
            if ((z - z0) % 4 != 0 && z - z0 >= 2 && z1 - z >= 2) {
                int len = 6 + (hash(2, z, x0) >>> 3 & 7) + (hash(7, z, x0) >>> 5 & 3);
                for (int y = tp - 1; y > tp - 1 - len && y >= 9; y--) {
                    if (side != 1) set(x0, y, z, GLOW);
                    if (side != 0) set(x0 + th - 1, y, z, GLOW);
                }
            }
        }
    }

    /** pylone effile : 5x5, puis 3x3, puis une aiguille, avec des bandes lumineuses */
    private void pylon(int cx, int cz, int h1, int h2, int h3) {
        boxM(cx - 2, FLOOR, cz - 2, cx + 2, h1, cz + 2);
        boxM(cx - 1, h1 + 1, cz - 1, cx + 1, h2, cz + 1);
        box(cx, h2 + 1, cz, cx, h3, cz, BB);
        set(cx, h3 + 1, cz, SLAB_BOT);
        for (int y = Math.max(9, h1 - 15); y <= h1 - 3; y++) {
            set(cx - 2, y, cz, GLOW); set(cx + 2, y, cz, GLOW); set(cx, y, cz - 2, GLOW); set(cx, y, cz + 2, GLOW);
        }
        for (int y = h1 + 3; y <= h2 - 2; y++) {
            set(cx - 1, y, cz, GLOW); set(cx + 1, y, cz, GLOW); set(cx, y, cz - 1, GLOW); set(cx, y, cz + 1, GLOW);
        }
        set(cx, h1 + 1, cz, GLOW);
    }

    // ------------------------------------------------------------------ construction
    private void build() {
        plinth();
        moatAndBridge();
        outerWalls();
        centralBlock();
        keep();
        tower();
        hallAndGate();
        shaft();
        crown();
        decor();
    }

    private void plinth() {
        boxM(3, 0, 16, 93, 3, 96);
        // lisere et marches du socle
        for (int x = 3; x <= 93; x++) { set(x, 3, 16, PBB); }
    }

    private void moatAndBridge() {
        // douves : un fosse en lentille devant la porte, borde d'un lisere de pierre
        boolean[][] moat = new boolean[W][D];
        for (int x = 6; x <= 90; x++) {
            int e = Math.max(0, 12 - Math.min(x, 96 - x));
            for (int z = 5 + e; z <= 12 - e; z++) moat[x][z] = true;
        }
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) {
            if (moat[x][z]) {
                for (int y = -6; y <= 0; y++) set(x, y, z, y <= -5 ? PBB : y <= -1 ? WATER : AIR);
                continue;
            }
            boolean rim = false;
            for (int dx = -1; dx <= 1 && !rim; dx++) for (int dz = -1; dz <= 1; dz++)
                if (x + dx >= 0 && x + dx < W && z + dz >= 0 && z + dz < D && moat[x + dx][z + dz]) { rim = true; break; }
            if (rim) for (int y = -6; y <= 0; y++) set(x, y, z, PBB);
        }
        // chaussee en rampe puis pont en arche
        for (int x = 43; x <= 53; x++) {
            for (int z = 0; z <= 3; z++) {
                for (int y = 0; y < z; y++) set(x, y, z, BB);
                set(x, z, z, stair(S, false));
            }
            for (int z = 4; z <= 15; z++) {
                for (int y = -6; y <= 3; y++) set(x, y, z, mat(x, y, z));
                set(x, 3, z, z % 4 == 3 ? PBB : BB);
            }
            for (int z = 5; z <= 12; z++) {
                double d = Math.abs(z - 8.5);
                int open = d <= 1.5 ? 2 : d <= 2.5 ? 1 : 0;
                for (int y = -4; y <= open; y++) set(x, y, z, y <= -1 ? WATER : AIR);
            }
        }
        for (int z = 4; z <= 15; z++) for (int x : new int[]{43, 53}) {
            set(x, 4, z, WALL); set(x, 5, z, WALL);
            if (z == 5 || z == 10 || z == 15) { set(x, 6, z, WALL); set(x, 7, z, GLOW); }
        }
    }

    private void outerWalls() {
        // ailes de facade
        finX(3, 23, 16, 4, 22, 14, 0);
        finX(73, 93, 16, 4, 22, 14, 0);
        // murs lateraux, en trois pans chacun
        for (int[] seg : new int[][]{{20, 44}, {44, 68}, {68, 92}}) {
            finZ(seg[0], seg[1], 3, 4, 22, 15, 0);
            finZ(seg[0], seg[1], 90, 4, 22, 15, 1);
        }
        // mur du fond
        for (int[] seg : new int[][]{{3, 33}, {33, 63}, {63, 93}}) finX(seg[0], seg[1], 90, 4, 22, 15, 1);
        // second rang d'ailerons, plus hauts, dans les ailes
        for (int[] seg : new int[][]{{28, 56}, {56, 84}}) {
            finZ(seg[0], seg[1], 13, 4, 30, 14, 2);
            finZ(seg[0], seg[1], 80, 4, 30, 14, 2);
        }
        // pylones aux sommets des murs
        int[][] corners = {{5, 18}, {91, 18}, {5, 92}, {91, 92}};
        for (int[] c : corners) pylon(c[0], c[1], 36, 58, 66);
        for (int[] c : new int[][]{{5, 44}, {91, 44}, {5, 68}, {91, 68}, {33, 92}, {63, 92}}) pylon(c[0], c[1], 32, 50, 58);
        for (int[] c : new int[][]{{15, 28}, {81, 28}, {15, 56}, {81, 56}, {15, 84}, {81, 84}}) pylon(c[0], c[1], 40, 62, 70);
    }

    private void centralBlock() {
        // grand corps : salle et facade
        boxM(24, 4, 16, 72, 28, 72);
        finZ(20, 44, 24, 4, 30, 8, 0); finZ(44, 68, 24, 4, 30, 8, 0);
        finZ(20, 44, 69, 4, 30, 8, 1); finZ(44, 68, 69, 4, 30, 8, 1);
        finX(24, 72, 69, 4, 30, 8, 1);
        boxM(24, 4, 16, 72, 34, 19);                                        // facade
        for (int x = 24; x <= 72; x += 2) set(x, 35, 16, BB);                // creneaux
        // pignon au-dessus de la porte
        for (int y = 35; y <= 49; y++) {
            int hw = 11 - (y - 35);
            if (hw < 0) break;
            for (int x = CX - hw; x <= CX + hw; x++) for (int z = 16; z <= 19; z++) set(x, y, z, mat(x, y, z));
        }
        // franges lumineuses sous le haut de la facade
        for (int x = 25; x <= 71; x++) {
            if (x >= 36 && x <= 60) continue;
            if (x % 4 == 0) continue;
            int len = 7 + (hash(x, 3, 16) >>> 3 & 7) + (hash(x, 6, 16) >>> 5 & 3);
            for (int y = 33; y > 33 - len; y--) set(x, y, 16, GLOW);
        }
        // pylones flanquant la porte (les plus hauts) et aux angles
        pylon(38, 18, 40, 66, 76);
        pylon(58, 18, 40, 66, 76);
        pylon(26, 18, 36, 54, 62);
        pylon(70, 18, 36, 54, 62);
        pylon(26, 70, 34, 50, 58);
        pylon(70, 70, 34, 50, 58);
    }

    private void keep() {
        // donjon en gradins derriere la salle
        boxM(33, 4, 69, 63, 40, 88);
        finZ(73, 88, 33, 4, 42, 10, 2);
        finZ(73, 88, 60, 4, 42, 10, 2);
        finX(33, 63, 85, 4, 42, 10, 1);
        for (int[] c : new int[][]{{35, 71}, {61, 71}, {35, 86}, {61, 86}}) pylon(c[0], c[1], 46, 66, 76);
    }

    private double towerR(int y) {
        if (y <= 62) return 9.2;
        if (y <= 80) return 9.2 - 1.8 * (y - 62) / 18.0;
        if (y <= 90) return 7.4;
        if (y <= 98) return 7.4 + 2.2 * (y - 90) / 8.0;
        return 10.5;
    }

    private void tower() {
        for (int y = 41; y <= 98; y++) {
            double r = towerR(y);
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > r * r + 0.25) continue;
                boolean outer = d2 > (r - 1.6) * (r - 1.6);
                byte id = mat(TX + dx, y, TZ + dz);
                if (outer && y >= 46) {
                    double a = Math.atan2(dz, dx);
                    double s = (a + Math.PI) / (2 * Math.PI) * 16;
                    double fr = s - Math.floor(s);
                    boolean rib = fr < 0.24 || y % 8 == 0;
                    if (!rib) id = GLOW;
                }
                set(TX + dx, y, TZ + dz, id);
            }
        }
        // pont de pierre entre la salle et la base de la tour (la tour fait une bosse dans la salle : on la retire plus bas)
    }

    private void hallAndGate() {
        // grande salle
        air(28, FLOOR, 20, 68, 27, 68);
        // sol : damier et tapis
        for (int x = 28; x <= 68; x++) for (int z = 20; z <= 68; z++) {
            boolean chk = ((x >> 1) + (z >> 1) & 1) == 0;
            set(x, 3, z, chk ? PBB : BB);
            if (x >= 47 && x <= 49) set(x, 3, z, TILE);
        }
        // colonnes
        for (int cx : new int[]{36, 60}) for (int cz : new int[]{28, 38, 48, 58}) {
            box(cx, FLOOR, cz, cx + 1, 27, cz + 1, BB);
            box(cx - 1, 24, cz - 1, cx + 2, 27, cz + 2, PBB);
            for (int y = 10; y <= 14; y++) { set(cx, y, cz, GLOW); set(cx + 1, y, cz + 1, GLOW); }
        }
        // lampes au plafond et sur les murs
        for (int x : new int[]{31, 48, 65}) for (int z = 24; z <= 64; z += 8) set(x, 27, z, GLOW);
        for (int z = 24; z <= 64; z += 8) for (int y = 13; y <= 16; y++) { set(27, y, z, GLOW); set(69, y, z, GLOW); }
        // porte en ogive dans la facade, avec des crocs
        for (int y = FLOOR; y <= 27; y++) {
            int hw = arch(y, 12, 26, 5);
            if (hw < 0) break;
            for (int x = CX - hw; x <= CX + hw; x++) for (int z = 16; z <= 19; z++) set(x, y, z, AIR);
        }
        for (int x = CX - 5; x <= CX + 5; x++) {
            if ((x - CX) % 2 == 0) continue;
            int ty = -1;
            for (int y = FLOOR; y <= 27; y++) if (arch(y, 12, 26, 5) >= Math.abs(x - CX)) ty = y;
            if (ty < 0) continue;
            for (int z = 16; z <= 17; z++) { set(x, ty, z, BB); set(x, ty - 1, z, BB); if (Math.abs(x - CX) <= 1) set(x, ty - 2, z, BB); }
        }
        // grande fenetre lumineuse en ogive au-dessus de la porte
        for (int y = 30; y <= 46; y++) {
            int hw = arch(y, 34, 46, 3);
            if (hw < 0) break;
            for (int x = CX - hw; x <= CX + hw; x++) { set(x, y, 16, GLASS); for (int z = 17; z <= 19; z++) set(x, y, z, GLOW); }
        }
        // passage vers la tour : sa bosse dans la salle disparait, une arche s'ouvre dans le mur du fond
        air(47, FLOOR, 69, 49, 7, 72);
        set(48, 8, 69, AIR); set(48, 8, 70, AIR); set(48, 8, 71, AIR); set(48, 8, 72, AIR);
        // alcoves a coffres au fond de la salle
        for (int x : new int[]{30, 66}) {
            box(x - 1, FLOOR, 66, x + 1, 4, 68, BB);
            chests.add(new int[]{x, 5, 67, N});
            set(x, 6, 66, GLOW);
        }
    }

    private void shaft() {
        // cage d'escalier 9x9 dans la tour (y = 4 a 98)
        air(TX - 4, FLOOR, TZ - 4, TX + 4, 98, TZ + 4);
        // pilier central 5x5 avec des lumieres
        box(TX - 2, FLOOR, TZ - 2, TX + 2, 98, TZ + 2, BB);
        for (int y = 8; y <= 96; y += 8) {
            set(TX - 2, y, TZ, GLOW); set(TX + 2, y, TZ, GLOW); set(TX, y, TZ - 2, GLOW); set(TX, y, TZ + 2, GLOW);
        }
        // colimacon carre sur 4 tours (24 marches chacune) : anneau de 24 cases, demi-cote 3, large de 2 (cases exterieures a 4)
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
        // plateforme du sommet (sol plein a y = 99) : on la pose avant, les marches la percent
        boxDisc(99, 10.5);
        for (int k = 0; k < 96; k++) {
            int i = k % 24, y = FLOOR + k;
            int[] c = cells.get(i);
            int d = dirs[i];
            int[][] extra = outward(c);
            set(TX + c[0], y, TZ + c[1], stair(d, false));
            for (int[] e : extra) set(TX + e[0], y, TZ + e[1], stair(d, false));
            if (k >= 93) {                                                  // percer le plancher du sommet au-dessus des dernieres marches
                set(TX + c[0], 99, TZ + c[1], k == 95 ? stair(d, false) : AIR);
                for (int[] e : extra) set(TX + e[0], 99, TZ + e[1], k == 95 ? stair(d, false) : AIR);
            }
        }
        for (int k = 93; k < 96; k++) {                                     // trou jusqu'au sommet de la cage, au-dessus des marches 93, 94 et 95
            int[] c = cells.get(k % 24);
            for (int y = 100; y <= 103; y++) { set(TX + c[0], y, TZ + c[1], AIR); for (int[] e : outward(c)) set(TX + e[0], y, TZ + e[1], AIR); }
        }
        // entree : tunnel jusqu'a la salle (deja ouvert), coffres a quelques paliers
        chests.add(new int[]{TX - 5 + 1, 100, TZ + 6, E});
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

    private void boxDisc(int y, double r) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) if (dx * dx + dz * dz <= r * r + 0.25) set(TX + dx, y, TZ + dz, mat(TX + dx, y, TZ + dz));
    }

    private void crown() {
        // parapet crenele autour de la plateforme
        for (int dx = -11; dx <= 11; dx++) for (int dz = -11; dz <= 11; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 9.3 && d <= 10.7) {
                int a = (int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (2 * Math.PI) * 48);
                set(TX + dx, 100, TZ + dz, BB);
                if (a % 2 == 0) set(TX + dx, 101, TZ + dz, BB);
            }
        }
        // couronne de pointes
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            int x = TX + (int) Math.round(Math.cos(a) * 9.4), z = TZ + (int) Math.round(Math.sin(a) * 9.4);
            int h = 110 + (i % 2) * 6 + (i % 4 == 0 ? 4 : 0);
            box(x, 102, z, x, h, z, BB);
            set(x, h + 1, z, SLAB_BOT);
            set(x, h - 2, z, GLOW);
        }
        // aiguille centrale
        box(TX - 1, 100, TZ - 1, TX + 1, 104, TZ + 1, BB);
        box(TX, 105, TZ, TX, 120, TZ, BB);
        for (int y = 106; y <= 118; y += 4) set(TX, y, TZ, GLOW);
        set(TX, 121, TZ, SLAB_BOT);
        // braseros d'ames sur la plateforme
        for (int[] p : new int[][]{{6, 0}, {-6, 0}, {0, 6}, {0, -6}}) {
            set(TX + p[0], 100, TZ + p[1], SOUL_SOIL);
            set(TX + p[0], 101, TZ + p[1], FIRE);
        }
    }

    private void decor() {
        // bandes lumineuses sur les faces du donjon (cote cour et arriere)
        for (int x = 36; x <= 60; x += 4) {
            for (int y = 12; y <= 36; y++) { if (y % 12 < 9) set(x, y, 88, GLOW); }
        }
        for (int z = 76; z <= 85; z += 3) {
            for (int y = 12; y <= 36; y++) { if (y % 12 < 9) { set(33, y, z, GLOW); set(63, y, z, GLOW); } }
        }
    }
}
