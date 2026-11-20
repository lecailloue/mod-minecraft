package net.ringsofash.world;

/**
 * Les mines de la Moria, sous la carte : un volume procedural (aucun tableau, tout est calcule bloc par bloc, donc de taille quelconque).
 * Depuis la falaise de l'est, avec son lac et les Portes de Durin, on traverse la Grande Salle aux piliers, on descend l'Escalier de Durin jusqu'au gouffre
 * que franchit un pont etroit, puis un second escalier mene aux Forges (fleuve de lave, cascade, colosses, echafaudages), a la tombe de Mazarbul et a un
 * reseau de galeries etayees ou affleure le minerai.
 * at(x, y, z, h) renvoie un identifiant de la palette ci-dessous : KEEP = ne rien changer (le terrain generique reste), AIR = creuse.
 */
public final class MoriaPlan {
    private MoriaPlan() {}

    public static final int KEEP = -1, AIR = 0, DSTONE = 1, DCOBBLE = 2, DBRICKS = 3, DPILLAR = 4, DCHISEL = 5, DRUNES = 6, DLAMP = 7, WHITE = 8, DEEPSLATE = 9,
            LAVA = 10, WATER = 11, MAGMA = 12, OAKP = 13, OAKLOG = 14, FENCE = 15, CHAIN = 16, LANTERN = 17, SLANTERN = 18, WEB = 19, BONES = 20, QUARTZ = 21,
            QSLAB = 22, BOOKS = 23, BARS = 24, FORGE = 25, ANVIL = 26, BSLAB = 27, BSTAIR = 28 /* 28..31 : N E S O, monte vers cette direction */, BWALL = 32,
            GRAVEL = 33, SPRLOG = 34, SPRLEAF = 35, ORE = 40 /* 40..44 */, BLACKSTONE = 46, OAKLOGX = 47, OAKLOGZ = 48, DSTAIR = 52 /* 52..55 */, BSTAIR_TOP = 56 /* 56..59 */,
            GOLD = 60, RAWGOLD = 61, RAILX = 62, RAILZ = 63, BARREL = 64, RCARPET = 65, REDWOOL = 66, BLACKWOOL = 67, CAMPFIRE = 68, CRAFT = 69, FURNACE = 70,
            YELLOWWOOL = 71, CAULDRON = 72, COPPER = 73, SHROOM = 74, IRONB = 75, SMBAS = 76;
    public static final int NIDS = 80;

    public static final int F0 = 80;       // sol de la cour et de la Grande Salle
    public static final int F1 = 17;       // sol du gouffre (le pont) et de son palier
    public static final int F2 = -20;      // sol des Forges et des galeries
    public static final int LW = 72;       // niveau du lac
    public static final int GX = 168, GZ = -360;   // plan de la falaise (la porte est a l'ouest de x = GX), axe de la porte

    // ------------------------------------------------------------------ bruits
    static int hsh(int x, int y, int z) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L ^ 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) (h & 0x7fffffff);
    }

    static double r01(int x, int y, int z) { return (hsh(x, y, z) & 0xFFFFF) / 1048575.0; }

    static double sm(double t) { t = t < 0 ? 0 : t > 1 ? 1 : t; return t * t * (3 - 2 * t); }

    static double vn(double x, double z, int seed) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = sm(x - ix), fz = sm(z - iz);
        double a = (hsh(ix, seed, iz) & 0xFFFF) / 65535.0, b = (hsh(ix + 1, seed, iz) & 0xFFFF) / 65535.0;
        double c = (hsh(ix, seed, iz + 1) & 0xFFFF) / 65535.0, d = (hsh(ix + 1, seed, iz + 1) & 0xFFFF) / 65535.0;
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
    }

    // ------------------------------------------------------------------ le lac et la cour (relief exterieur)
    private static double lakeR(int x, int z) {
        double a = (x - 252) / 42.0, b = (z + 358) / 72.0;
        return Math.sqrt(a * a + b * b);
    }

    public static boolean inLake(int x, int z) {
        return x > 205 && x < 300 && z > -440 && z < -275 && lakeR(x, z) < 1.0;
    }

    /** niveau de l'eau du lac a cet endroit, ou MIN_VALUE */
    public static int lakeTop(int x, int z, int h) { return inLake(x, z) && h < LW ? LW : Integer.MIN_VALUE; }

    /** la cour taillee devant les Portes : sol de montagne sans neige */
    public static boolean inCourt(int x, int z) { return x >= GX - 2 && x <= 232 && Math.abs(z - GZ) <= 46; }

    /** relief : la falaise taillee devant les Portes (cour plate a F0), le bassin du lac et sa digue */
    public static int surface(int x, int z, int g) {
        if (x < GX || x > 335 || z < -450 || z > -265) return g;
        int out = g;
        double u = Math.abs(z - GZ);
        double w = (1 - sm((u - 30) / 14.0)) * (1 - sm((x - 222) / 14.0));
        if (w > 0) out = (int) Math.round(g + (Math.min(g, F0) - g) * w);
        double r = lakeR(x, z);
        if (r < 1.3) {                                                   // cuvette du lac et berges en pente douce
            int bed = r < 1.0 ? LW - 2 - (int) Math.round(14 * sm((1 - r) / 0.8)) : LW - 2 + (int) Math.round((r - 1) * 40);
            out = Math.min(out, bed);
        }
        return out;
    }

    // ------------------------------------------------------------------ geometrie commune
    static int floorPat(int x, int z) {
        int q = (Math.floorDiv(x, 4) + Math.floorDiv(z, 4)) & 1;
        double r = r01(x, 3, z);
        if (r < 0.06) return DCOBBLE;
        return q == 0 ? DBRICKS : DSTONE;
    }

    private static final int GW = 6, GH = 12, GT = 26;              // porte : demi-largeur, hauteur des montants, hauteur du sommet de l'ogive

    private static double wc(int v) {                               // demi-largeur de l'ogive a la hauteur v (continu)
        if (v <= GH) return GW;
        double t = Math.min(1, (v - GH) / (double) (GT - GH));
        return GW * (1 - Math.pow(t, 1.5));
    }

    private static int halfW(int v) {                               // idem, entier (-1 hors de l'arche)
        if (v < 1 || v >= GT) return -1;
        return (int) Math.floor(wc(v));
    }

    // ------------------------------------------------------------------ les Portes et le couloir d'entree
    private static int gate(int x, int y, int z, int h) {
        if (x < 139 || x > GX + 1 || z < GZ - 30 || z > GZ + 30 || y < F0 || y > F0 + 45) return KEEP;
        int u = z - GZ, v = y - F0, au = Math.abs(u);
        if (x >= GX) {
            if (y == h && y == F0 && x <= GX + 30 && au <= 8) return (x + z) % 6 == 0 ? DCHISEL : floorPat(x, z);       // dallage devant la porte
            return KEEP;
        }
        int w = halfW(v);
        boolean face = x >= GX - 3;
        if (v >= 1 && au <= w) {
            if (x == GX - 3 || x == GX - 2) {                        // les vantaux de pierre, fermes, avec une entrebaillure au centre
                boolean gap = (au <= 2 && v <= 13) || (au <= 1 && v <= 15) || (au == 0 && v <= 16);
                if (gap) return AIR;
                if (x == GX - 3) return DSTONE;
                double in = wc(v) - au;
                if (v >= 3 && in >= 1.0 && in < 2.0 && (v + au) % 2 == 0) return DRUNES;                  // arc d'ithildin le long de l'arche
                if (au >= 3 && v >= 4 && v <= 10 && (v == 6 || v == 9)) return (au + v) % 3 == 0 ? DSTONE : DRUNES;   // lignes d'inscription
                if ((au == 0 && v >= 17 && v <= 22) || (v == 19 && au <= 2)) return v == 19 && au == 0 ? WHITE : DRUNES;   // l'etoile de Durin
                return (au == 3 && v >= 3 && v <= 12) ? DCHISEL : DBRICKS;
            }
            return AIR;
        }
        if (v == 0 && au <= GW + 1) return (x % 8 == 0 && au <= 1) ? DRUNES : floorPat(x, z);
        double dOut = v <= GT ? au - wc(v) : Math.hypot(u, v - GT);
        if (face) {
            if (dOut > 7 || v > GT + 9) return KEEP;
            boolean front = x == GX - 1;
            if (dOut > 0 && dOut <= 1.5 && front) return WHITE;
            if (dOut > 1.5 && dOut <= 3.5) return DCHISEL;
            if (front) {
                if (dOut > 4.5 && dOut < 7 && r01(u, v, 17) < 0.05) return DRUNES;              // les etoiles
                if (dOut > 3.5 && dOut <= 4.5 && v >= 6 && (v + au) % 3 == 0) return DRUNES;      // la guirlande d'ithildin autour de l'arche
                return DBRICKS;
            }
            return DSTONE;
        }
        if (dOut <= 2) {                                             // parois du couloir
            if (dOut <= 1 && Math.floorMod(x, 9) == 0) return DPILLAR;
            return DBRICKS;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ colosses
    static int statue(int x, int y, int z, int cx, int cz, int base, int face) {
        int dx = x - cx, dz = (z - cz) * face, v = y - base, ax = Math.abs(dx), az = Math.abs(dz);
        if (v < 0 || v > 52 || ax > 12 || az > 12) return KEEP;
        if (v <= 4) return ax <= 9 && az <= 9 ? (v == 4 ? DCHISEL : DBRICKS) : KEEP;
        if (v <= 21) return ax >= 1 && ax <= 5 && az <= 3 ? DBRICKS : KEEP;
        if (v == 22) return ax <= 6 && az <= 4 ? DCHISEL : KEEP;
        if (v <= 38) {
            if (ax <= 7 && az <= 4) return (v >= 30 && v <= 36 && dz == 4 && ax <= 5) ? DCHISEL : DSTONE;
            if (ax >= 8 && ax <= 10 && az <= 2) return DBRICKS;
            if (dz >= 5 && dz <= 6 && ax <= 2 && v >= 32) return DCOBBLE;
            return KEEP;
        }
        if (v <= 46) {
            if (ax <= 3 && az <= 3) return (dz == 3 && v == 44 && ax == 1) ? DRUNES : DSTONE;
            if (dz >= 4 && dz <= 5 && ax <= 2 && v <= 42) return DCOBBLE;
            return KEEP;
        }
        if (v <= 49) return ax <= 4 && az <= 4 ? DBRICKS : KEEP;
        return v == 50 && ax <= 1 && az <= 1 ? DCHISEL : KEEP;
    }

    // ------------------------------------------------------------------ Grande Salle (piliers)
    private static final int HX0 = -12, HX1 = 138, HZ0 = -472, HZ1 = -248;

    private static double vault(int x, int z) {
        double a = 0.5 - 0.5 * Math.cos(2 * Math.PI * (x - 8) / 28.0), b = 0.5 - 0.5 * Math.cos(2 * Math.PI * (z + 346) / 28.0);
        return a * b;
    }

    static int rubble(int x, int y, int z, int fy) {
        int gx = Math.floorDiv(x, 9), gz = Math.floorDiv(z, 9);
        if (r01(gx, 7, gz) >= 0.13) return KEEP;
        int cx = gx * 9 + 3 + hsh(gx, 1, gz) % 3, cz = gz * 9 + 3 + hsh(gx, 2, gz) % 3;
        double d = Math.hypot(x - cx, z - cz);
        int hh = (int) (4.5 * (1 - d / 4.6) + r01(x, y, z) * 0.9);
        if (d < 4.6 && y - fy <= hh) return r01(x, y, z) < 0.5 ? DCOBBLE : r01(x, 5, z) < 0.5 ? GRAVEL : DSTONE;
        return KEEP;
    }

    static int hangingLamp(int x, int y, int z, int ceil, int grid) {
        if (Math.floorMod(x, grid) != 0 || Math.floorMod(z, grid) != 8 || r01(x, 9, z) < 0.35) return KEEP;
        if (y >= ceil - 8 && y <= ceil) return CHAIN;
        if (y == ceil - 9) return LANTERN;
        return KEEP;
    }

    private static int hall(int x, int y, int z, int h) {
        if (x < HX0 - 1 || x > HX1 + 1 || z < HZ0 - 1 || z > HZ1 + 1 || y < F0 || y > F0 + 85) return KEEP;
        int ceil = Math.min(F0 + 48 + (int) Math.round(8 * vault(x, z)), h - 11);
        if (ceil < F0 + 14) return KEEP;
        boolean in = x >= HX0 && x <= HX1 && z >= HZ0 && z <= HZ1;
        if (!in) {
            if (y > ceil + 1) return KEEP;
            boolean xw = x < HX0 || x > HX1, zw = z < HZ0 || z > HZ1;
            if (xw && zw) return KEEP;
            int m = Math.floorMod(xw ? z - (-346) : x - 8, 28);
            boolean pil = m <= 1 || m >= 27;
            if (y == F0 + 8 && (xw ? m == 0 : m == 0)) return DLAMP;
            if (pil) return y % 12 == 11 ? DCHISEL : DPILLAR;
            return y % 12 == 11 ? DCHISEL : DBRICKS;
        }
        if (y == F0) {
            if (Math.abs(z - GZ) <= 1 && Math.floorMod(x, 8) == 0) return DRUNES;
            return floorPat(x, z);
        }
        if (y == ceil + 1) return vault(x, z) < 0.18 ? DBRICKS : DSTONE;
        if (y > ceil + 1) return KEEP;
        if (y > ceil) return KEEP;
        // piliers
        int ix = (int) Math.round((x - 8) / 28.0), iz = (int) Math.round((z + 346) / 28.0);
        if (ix >= 0 && ix <= 4 && iz >= -4 && iz <= 3) {
            int px = 8 + 28 * ix, pz = -346 + 28 * iz, dx = x - px, dz = z - pz;
            int ax = Math.abs(dx), az = Math.abs(dz);
            boolean broken = r01(px, 4, pz) < 0.16;
            int top = broken ? F0 + 10 + (int) (r01(px, 5, pz) * 40) : ceil;
            double rr = 3 + (y > ceil - 10 && !broken ? (y - (ceil - 10)) * 0.35 : 0) + (y < F0 + 4 ? 1 : 0);
            if (Math.max(ax, az) <= rr && ax + az <= rr + 2 && y <= top) {
                if (y == F0 + 8 && (ax == 3 && az == 0 || az == 3 && ax == 0)) return DLAMP;
                if (y < F0 + 4 || y > ceil - 3) return DBRICKS;
                return (y - F0) % 12 == 11 ? DCHISEL : DPILLAR;
            }
            if (broken && Math.max(ax, az) <= 7 && y - F0 <= 3 - Math.max(ax, az) / 2 + (int) (r01(x, y, z) * 1.5)) return r01(x, y, z) < 0.5 ? DCOBBLE : DSTONE;   // eboulis de la colonne tombee
        }
        int r = rubble(x, y, z, F0);
        if (r != KEEP) return r;
        r = hangingLamp(x, y, z, ceil, 16);
        if (r != KEEP) return r;
        if (y == ceil && r01(x, 11, z) < 0.012) return WEB;
        return AIR;
    }

    // ------------------------------------------------------------------ escaliers monumentaux (un bloc de haut pour un bloc de long)
    private static int stairRun(int x, int y, int z, int h, int x0, int x1, int zEnd, int zTop, int yTop, int yBot, int ceilY) {
        if (x < x0 - 1 || x > x1 + 1 || z < zEnd || z > zTop || y < yBot - 1) return KEEP;
        int fy = z >= zTop ? yTop : Math.max(yBot, yTop - (zTop - z));
        int ceil = Math.min(ceilY, h - 11);
        if (ceil < fy + 12 || y > ceil + 1) return KEEP;
        boolean in = x >= x0 && x <= x1;
        if (!in) {
            if (y < fy - 1) return KEEP;
            if (Math.floorMod(z, 12) == 0) return y % 12 == 11 ? DCHISEL : DPILLAR;
            if (y == fy + 8 && Math.floorMod(z, 12) == 6) return DLAMP;
            return DBRICKS;
        }
        if (y < fy) return KEEP;
        if (y == fy) return (fy > yBot && z < zTop) ? BSTAIR + 2 : floorPat(x, z);
        if (y > ceil) return DSTONE;
        if ((x == x0 || x == x1) && Math.floorMod(z, 8) == 0 && z < zTop) {          // bornes lumineuses le long des marches
            if (y <= fy + 4) return DPILLAR;
            if (y == fy + 5) return DLAMP;
        }
        return AIR;
    }

    // ------------------------------------------------------------------ le gouffre et son pont
    private static final int CX0 = 26, CX1 = 126, CZ0 = -648, CZ1 = -553;

    private static int chasm(int x, int y, int z, int h) {
        if (x < CX0 - 8 || x > CX1 + 8 || z < CZ0 || z > CZ1 || y < -52 || y > 170) return KEEP;
        double xl = CX0 + 7 * (2 * vn(z / 21.0, 3.1, 61) - 1), xr = CX1 + 7 * (2 * vn(z / 19.0, 8.3, 62) - 1);
        if (x <= xl || x >= xr) return KEEP;
        double dEdge = Math.min(Math.min(x - xl, xr - x), Math.min(z - CZ0, CZ1 - z) + 1);
        boolean ew = Math.min(x - xl, xr - x) < Math.min(z - CZ0, CZ1 - z) + 1;
        int ceil = Math.min(h - 11, 160);
        if (y > ceil) return KEEP;
        // le pont
        if (x >= 74 && x <= 78 && (y == F1 || y == F1 - 1)) return y == F1 && (x == 74 || x == 78) ? DCHISEL : DBRICKS;
        if (dEdge < 5) {
            if (y == F1) return floorPat(x, z);
            if (y == F1 + 1 && ew && dEdge >= 4 && z > CZ0 + 6 && z < CZ1 - 6) return Math.floorMod(z, 10) == 0 ? DLAMP : BWALL;     // garde-corps du chemin de ronde
            if (y < F1) return DBRICKS;
            return AIR;
        }
        if (y < -51) return KEEP;
        if (y <= -46) return LAVA;
        double s = vn(x / 8.0, z / 8.0, 31);
        if (s > 0.71 && y <= -45 + (int) ((s - 0.71) / 0.29 * 38)) return BLACKSTONE;
        return AIR;
    }

    // ------------------------------------------------------------------ les Forges
    private static final int FX0 = -110, FX1 = 250, FZ0 = -830, FZ1 = -688;

    private static double lavaZ(int x) { return -759 + 5 * Math.sin(x / 31.0); }

    private static int waterfall(int x, int y, int z) {
        if (x < 50 || x > 60 || z < -842 || z > -828 || y < F2 - 5 || y > F2 + 56) return KEEP;
        if (z >= -830 && y >= F2 && y <= F2 + 38) return WATER;
        if (z >= -830 && y == F2 + 39) return KEEP;
        if (y >= F2 + 36 && y <= F2 + 39) return WATER;                   // la bouche, taillee dans la roche
        return KEEP;
    }

    private static int forge(int x, int y, int z, int h) {
        if (x < FX0 - 1 || x > FX1 + 1 || z < FZ0 - 1 || z > FZ1 + 1 || y < F2 - 6 || y > F2 + 80) return KEEP;
        int ceil = Math.min(F2 + 72, h - 11);
        if (ceil < F2 + 18) return KEEP;
        boolean in = x >= FX0 && x <= FX1 && z >= FZ0 && z <= FZ1;
        if (!in) {
            if (y < F2 || y > ceil + 1) return KEEP;
            boolean xw = x < FX0 || x > FX1, zw = z < FZ0 || z > FZ1;
            if (xw && zw) return KEEP;
            int m = Math.floorMod(xw ? z : x, 32);
            if (m <= 1 || m >= 31) return y % 12 == 11 ? DCHISEL : (y == F2 + 8 && m == 0 ? DLAMP : DPILLAR);
            return y % 12 == 11 ? DCHISEL : DBRICKS;
        }
        double zc = lavaZ(x), dz = z - zc;
        double adz = Math.abs(dz);
        int bx = Math.floorMod(x + 24, 48);
        boolean bridge = bx <= 4 || bx >= 44;
        // fleuve de lave
        if (adz <= 3.5) {
            if (y >= F2 - 3 && y <= F2) return bridge && y == F2 ? DBRICKS : LAVA;
            if (y < F2 - 3) return y == F2 - 4 ? KEEP : KEEP;
            if (bridge && y == F2 + 1 && (bx == 4 || bx == 44) && adz <= 3.5) return adz > 2.5 ? BWALL : AIR;
        } else if (adz <= 5 && y == F2) return DBRICKS;
        if (y < F2) return KEEP;
        if (y == F2) {
            // bassin de la cascade
            double q = Math.pow((x - 55) / 11.0, 2) + Math.pow((z + 820) / 7.5, 2);
            if (q < 1) return AIR;
            if (q < 1.25) return DBRICKS;
            return floorPat(x, z);
        }
        if (y < F2) return KEEP;
        double q = Math.pow((x - 55) / 11.0, 2) + Math.pow((z + 820) / 7.5, 2);
        if (q < 1 && y >= F2 - 5 && y <= F2) return AIR;
        if (y == ceil + 1) return DSTONE;
        if (y > ceil + 1) return KEEP;
        if (y > ceil) return KEEP;
        // piliers
        int ix = (int) Math.round((x + 54) / 32.0);
        if (ix >= -1 && ix <= 9) {
            int px = -54 + 32 * ix;
            for (int pz : new int[]{-806, -716}) {
                if (pz == -716 && px >= 42 && px <= 106) continue;
                int ax = Math.abs(x - px), az = Math.abs(z - pz);
                double rr = 3 + (y > ceil - 10 ? (y - (ceil - 10)) * 0.35 : 0) + (y < F2 + 4 ? 1 : 0);
                if (Math.max(ax, az) <= rr && ax + az <= rr + 2) {
                    if (y == F2 + 8 && (ax == 3 && az == 0 || az == 3 && ax == 0)) return DLAMP;
                    if (y < F2 + 4 || y > ceil - 3) return DBRICKS;
                    return (y - F2) % 12 == 11 ? DCHISEL : DPILLAR;
                }
            }
        }
        // les forges (plates-formes au nord du fleuve)
        for (int fx : new int[]{-70, -34, 18, 138, 190, 230}) {
            int ax = Math.abs(x - fx), az = Math.abs(z + 798);
            if (ax <= 7 && az <= 5) {
                if (y == F2 + 1) return DBRICKS;
                if (y == F2 + 2) {
                    if (az == 0 && (ax == 2 || ax == 3)) return FORGE;
                    if (az == 0 && ax == 5) return ANVIL;
                    if (ax <= 1 && z == -802) return DBRICKS;
                }
                if (ax <= 1 && z == -802 && y > F2 + 2) return DBRICKS;          // cheminee
                if (y == F2 + 3 && ax == 7 && az == 5) return DLAMP;
            }
        }
        int r;
        // echafaudage
        int ex = Math.abs(x - 102), ez = Math.abs(z + 800);
        if (ex <= 6 && ez <= 6 && y <= F2 + 36) {
            int v = y - F2;
            if (ex == 6 && ez == 6 && v >= 1) return OAKLOG;
            if ((v == 12 || v == 24 || v == 35) && !(ex <= 1 && ez <= 1 && v != 35)) return OAKP;
            if ((v == 13 || v == 25 || v == 36) && (ex == 6 || ez == 6) && !(ex == 6 && ez == 6)) return FENCE;
            if ((ex == 3 && ez == 6 || ex == 6 && ez == 3) && v >= 1 && v <= 11) return v % 4 == 3 ? OAKP : FENCE;
        }
        r = rubble(x, y, z, F2);
        if (r != KEEP && adz > 6) return r;
        r = hangingLamp(x, y, z, ceil, 16);
        if (r != KEEP) return r;
        if (y == ceil && r01(x, 12, z) < 0.012) return WEB;
        return AIR;
    }

    // ------------------------------------------------------------------ la tombe de Mazarbul
    private static int tomb(int x, int y, int z, int h) {
        boolean cor = x >= 251 && x <= 257 && z >= -764 && z <= -757;
        boolean room = x >= 258 && x <= 298 && z >= -790 && z <= -730;
        boolean shell = !room && !cor && x >= 257 && x <= 299 && z >= -791 && z <= -729;
        if (!cor && !room && !shell) return KEEP;
        int ceil = Math.min(F2 + 20, h - 11);
        if (y < F2 || y > ceil + 1 || ceil < F2 + 10) return KEEP;
        if (cor) {
            if (y == F2) return floorPat(x, z);
            if (y <= F2 + 9) return AIR;
            return KEEP;
        }
        if (shell) {
            if (y > ceil) return KEEP;
            boolean nsw = z == -791 || z == -729;
            if (nsw && y >= F2 + 1 && y <= F2 + 3 && x >= 262 && x <= 294 && r01(x, y, z) < 0.7) return BOOKS;
            if (y == F2 + 8 && (x - 258) % 10 == 0) return DLAMP;
            return DBRICKS;
        }
        if (y == F2) return floorPat(x, z);
        if (y == ceil + 1) return DSTONE;
        if (y > ceil) return KEEP;
        for (int[] p : new int[][]{{268, -775}, {288, -775}, {268, -745}, {288, -745}}) {
            int ax = Math.abs(x - p[0]), az = Math.abs(z - p[1]);
            if (Math.max(ax, az) <= 2 && ax + az <= 3) return y == F2 + 1 || y >= ceil - 1 ? DBRICKS : DPILLAR;
        }
        int ax = Math.abs(x - 278), az = Math.abs(z + 760);
        if (ax <= 4 && az <= 3) {                                                  // le tombeau blanc
            if (y == F2 + 1) return DCHISEL;
            if (y == F2 + 2) return QUARTZ;
            if (y == F2 + 3 && ax <= 3 && az <= 2) return QSLAB;
        }
        if (y == F2 + 1 && r01(x, 14, z) < 0.03) return BONES;
        if (y == ceil && r01(x, 13, z) < 0.04) return WEB;
        if (y == F2 + 1 && r01(x, 15, z) < 0.012) return DCOBBLE;
        return AIR;
    }

    // ------------------------------------------------------------------ galeries de mine
    private static final int[][][] TUN = {
            {{251, -715}, {285, -700}, {320, -660}, {335, -600}, {325, -540}},
    };

    private static int tunnels(int x, int y, int z, int h) {
        if (y < F2 || y > F2 + 8 || y > h - 8) return KEEP;
        for (int[][] pl : TUN) {
            double s0 = 0;
            for (int i = 0; i + 1 < pl.length; i++) {
                double ax = pl[i][0], az = pl[i][1], bx = pl[i + 1][0], bz = pl[i + 1][1];
                double dx = bx - ax, dz = bz - az, len = Math.hypot(dx, dz);
                double t = ((x - ax) * dx + (z - az) * dz) / (len * len);
                double tc = Math.max(0, Math.min(1, t));
                double ex = ax + tc * dx - x, ez = az + tc * dz - z;
                double d = Math.hypot(ex, ez) + 0.35 * (vn(x / 6.0, z / 6.0, 70) - 0.5) * 2;
                if (d <= 4.4 && (t >= 0 || i == 0 && false)) {
                    if (y == F2) return r01(x, 2, z) < 0.5 ? DCOBBLE : GRAVEL;
                    double s = s0 + t * len;
                    boolean frame = t >= 0 && t <= 1 && Math.floorMod((int) Math.floor(s), 7) == 0;
                    boolean alongX = Math.abs(dx) > Math.abs(dz);
                    if (frame) {
                        if (y == F2 + 7) return alongX ? OAKLOGZ : OAKLOGX;
                        if (y <= F2 + 6 && d > 3.2) return OAKLOG;
                        if (y == F2 + 6 && d < 0.9 && Math.floorMod((int) Math.floor(s / 7), 2) == 0) return LANTERN;
                    }
                    return y <= F2 + 6 ? AIR : (y == F2 + 7 && d < 3.5 ? AIR : KEEP);
                }
                s0 += len;
            }
        }
        // chambres au bout des galeries
        for (int[][] pl : TUN) {
            int[] e = pl[pl.length - 1];
            double d = Math.hypot(x - e[0], z - e[1]);
            if (d < 9 && y <= h - 8) {
                if (y == F2) return GRAVEL;
                if (y <= F2 + 7 + (d < 5 ? 2 : 0)) return AIR;
            }
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ minerai
    private static int ore(int x, int y, int z) {
        if (y > 110) return KEEP;
        double p = Math.max(0.0035, MoriaRooms.oreP(x, y, z));
        if (y < F2 + 12 && y > F2 - 10) {
            for (int[][] pl : TUN) { int[] e = pl[pl.length - 1]; if (Math.hypot(x - e[0], z - e[1]) < 15) { p = 0.07; break; } }
        }
        int q = hsh(x, y, z);
        if ((q & 0xFFFF) / 65535.0 >= p) return KEEP;
        return ORE + (q >> 16) % 5;
    }

    // ------------------------------------------------------------------ assemblage
    public static boolean inBox(int x, int y, int z) { return x >= -500 && x <= 345 && z >= -1140 && z <= -225 && y >= -63 && y <= 176; }

    public static int at(int x, int y, int z, int h) {
        if (!inBox(x, y, z)) return KEEP;
        int r;
        if ((r = waterfall(x, y, z)) != KEEP) return r;
        if ((r = tomb(x, y, z, h)) != KEEP) return r;
        if ((r = MoriaRooms.at(x, y, z, h)) != KEEP) return r;
        if ((r = tunnels(x, y, z, h)) != KEEP) return r;
        if ((r = gate(x, y, z, h)) != KEEP) return r;
        if ((r = stairRun(x, y, z, h, 50, 100, -552, -473, F0, F1, F0 + 50)) != KEEP) return r;
        if ((r = stairRun(x, y, z, h, 50, 100, -687, -649, F1, F2, F2 + 66)) != KEEP) return r;
        if ((r = chasm(x, y, z, h)) != KEEP) return r;
        if ((r = hall(x, y, z, h)) != KEEP) return r;
        if ((r = forge(x, y, z, h)) != KEEP) return r;
        return ore(x, y, z);
    }
}
