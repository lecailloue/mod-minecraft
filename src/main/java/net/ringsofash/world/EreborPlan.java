package net.ringsofash.world;

import static net.ringsofash.world.MoriaPlan.AIR;
import static net.ringsofash.world.MoriaPlan.KEEP;
import static net.ringsofash.world.MoriaPlan.r01;
import static net.ringsofash.world.MoriaPlan.sm;
import static net.ringsofash.world.MoriaPlan.vn;

/**
 * Erebor, la Montagne Solitaire, en (878, -1152). Comme la Moria : un volume procedural, fonction pure de (x, y, z, hauteur du terrain), aucun tableau.
 * Repere local : u vers l'est, v vers le sud (la facade et la Porte regardent le sud), y absolu. Le plateau est a y = 150.
 * - la montagne : un massif a falaise haut de 230 blocs ; la cour devant la facade est taillee dans la falaise ;
 * - la facade : nervures, niches a statues de nains geants, rangees de fenetres, ecu de la Porte, corniche et creneaux ;
 * - la Porte, la Grande Salle de Thror (colonnes, statues, braseros), le Grand Escalier, la Galerie des Rois et la salle du trone ;
 * - la Tresorerie : un tas d'or, l'Arkenstone, des colonnes geantes, reliee a la Grande Salle par deux escaliers.
 * Tous les blocs sont des blocs vanille (pierre taillee, andesite, tuf, or, cuivre, lanternes).
 */
public final class EreborPlan {
    private EreborPlan() {}

    public static final int EX = 878, EZ = -1152;
    static final int FL0 = 150;            // sol de la cour, de la Porte et de la Grande Salle
    static final int FL1 = 186;            // sol de la Galerie des Rois et du trone
    static final int FT = 100;             // sol de la Tresorerie
    static final int VF = 140;             // plan de la facade (la roche est au nord de ce plan)

    // palette : les identifiants de MoriaPlan (< 119) restent valables, ceux ci s'y ajoutent
    public static final int ROCK = 119, SNOWB = 120, STB = 121, MSTB = 122, CSTB = 123, PANDES = 124, SMST = 125, TUFFB = 126, PTUFF = 127, CTUFF = 128, COBBLE = 129,
            ANDES = 130, CUCUT = 131, CHCU = 132, SEAL = 133, GLASS = 134, SLANT = 135, GLOW = 136, CALC = 137, POLBK = 138,
            STS = 140 /* 140..143 : N E S O, monte vers cette direction */, STST = 144 /* 144..147 : a l'envers */, PAS = 148 /* 148..151 : andesite polie */, TFS = 152 /* 152..155 : tuf */,
            SLB = 156, SLT = 157, PSLB = 158, PSLT = 159, WALLSTB = 160;
    public static final int NIDS = 161;

    private static final int gold = MoriaPlan.GOLD;

    // ------------------------------------------------------------------ la montagne
    private static final int RX = 175, RZ = 180;

    private static double ring(int u, int v) {
        double a = Math.abs(u) / (double) RX, b = Math.abs(v) / (double) RZ;
        a *= a; a *= a; b *= b; b *= b;
        return Math.sqrt(Math.sqrt(a + b));
    }

    /** altitude du sommet du massif a cet endroit (coordonnees du monde), MIN_VALUE hors de l'emprise */
    private static final ThreadLocal<int[]> TC = ThreadLocal.withInitial(() -> new int[]{Integer.MIN_VALUE, 0, 0});

    static int top(int x, int z) {
        int[] c = TC.get();
        if (c[0] == x && c[1] == z) return c[2];
        int t = top0(x, z);
        c[0] = x; c[1] = z; c[2] = t;
        return t;
    }

    private static int top0(int x, int z) {
        int u = x - EX, v = z - EZ;
        if (Math.abs(u) > RX || Math.abs(v) > RZ) return Integer.MIN_VALUE;
        double r = ring(u, v);
        if (r >= 1) return Integer.MIN_VALUE;
        double wall = Math.pow(sm((1 - r) / 0.16), 0.9);
        double hh = 230 * (0.62 * wall + 0.38 * Math.pow(1 - r, 1.3));
        hh *= 0.93 + 0.14 * vn(x * 0.03, z * 0.03, 5);
        double crag = Math.abs(vn(x * 0.045, z * 0.045, 7) - 0.5) * 2;
        hh += 26 * Math.pow(1 - crag, 2) * (1 - r) * wall;
        hh += 5 * (vn(x * 0.13, z * 0.13, 6) - 0.5);
        return FL0 + (int) hh;
    }

    /** altitude jusqu'a laquelle le plan construit au dessus du sol (MIN_VALUE hors du site) */
    public static int aboveTop(int x, int z) {
        int t = top(x, z);
        int u = x - EX, v = z - EZ;
        if (Math.abs(u) <= 90 && v > VF - 8 && v <= 240) t = Math.max(t, FL0 + 96);
        return t;
    }

    public static boolean inBox(int x, int y, int z) {
        int u = x - EX, v = z - EZ;
        return Math.abs(u) <= RX + 2 && v >= -RZ - 2 && v <= 242 && y >= 60 && y <= 460;
    }

    // ------------------------------------------------------------------ assemblage
    public static int at(int x, int y, int z, int h) {
        if (!inBox(x, y, z)) return KEEP;
        int u = x - EX, v = z - EZ;
        if (air(u, y, v)) return decor(u, y, v);
        int r;
        if (nearAir(u, y, v)) { int l = lining(u, y, v); if (l != KEEP) return l; }
        if ((r = statues(u, y, v)) != KEEP) return r;
        if ((r = facade(u, y, v)) != KEEP) return r;
        if ((r = court(u, y, v, h)) != KEEP) return r;
        return massif(x, y, z, h);
    }

    private static int massif(int x, int y, int z, int h) {
        if (y <= h) return KEEP;
        int t = top(x, z);
        if (t == Integer.MIN_VALUE || y > t) return KEEP;
        double line = 292 + 18 * vn(x * 0.05, z * 0.05, 9);
        if (y >= t - 1 && y >= line) return SNOWB;
        return ROCK;
    }

    private static boolean in(int a, int lo, int hi) { return a >= lo && a <= hi; }

    // ------------------------------------------------------------------ les volumes creuses
    private static int halfW(int yy) { return yy <= 30 ? 12 : 12 - (yy - 30) * 3 / 4; }

    /** niveau de la cellule la plus basse creusee dans l'axe Porte - Grande Salle - Escalier - Galerie (v <= 127) */
    private static int axisFloor(int v) {
        if (v >= 24) return FL0;
        if (v >= -12) return 149 + (24 - v);                 // les marches du Grand Escalier
        return FL1;
    }

    private static int axisCeil(int u, int v) {
        int drop = Math.max(0, (Math.abs(u) - 26) / 2);
        return (v >= -12 ? FL0 + 66 : FL1 + 66) - drop;
    }

    private static double d2t(int u, int v) {
        double a = u / 86.0, b = (v + 72) / 56.0;
        return a * a + b * b;
    }

    private static int treasCeil(int u, int v) { return 172 - (int) (9 * d2t(u, v)); }

    /** marche : renvoie le niveau de la marche a (u, v) ou MIN_VALUE hors de la volee. dir : 0 N, 1 E, 2 S, 3 O (monte vers cette direction) */
    private static int tread(int u, int v, int dir, int a0, int n, int lat0, int lat1, int yBase) {
        int lat = (dir == 0 || dir == 2) ? u : v;
        if (lat < lat0 || lat > lat1) return Integer.MIN_VALUE;
        int along = switch (dir) { case 0 -> -v; case 1 -> u; case 2 -> v; default -> -u; };
        int k = along - a0;
        if (k < 1 || k > n) return Integer.MIN_VALUE;
        return yBase + k;
    }

    // les deux escaliers de la Tresorerie : le cote ouest (s = -1) et le cote est (s = +1)
    private static int sideTread(int u, int v) {
        int au = Math.abs(u);
        if (au < 58 || au > 66) return Integer.MIN_VALUE;
        return tread(au, v, 2, -20, 50, 58, 66, 99);
    }

    private static boolean air(int u, int y, int v) {
        if (y < 94 || y > 262 || u < -100 || u > 100 || v < -130 || v > 144) return false;
        int au = Math.abs(u);
        // la Porte
        if (v >= 127 && v <= VF + 3 && y >= FL0 && y < FL0 + 46 && au <= halfW(y - FL0)) return true;
        // l'axe : Grande Salle, Grand Escalier, Galerie des Rois
        if (au <= 36 && v >= -126 && v <= 127 && y >= axisFloor(v) && y < axisCeil(u, v)) return true;
        // la Tresorerie
        if (u >= -86 && u <= 86 && v >= -128 && v <= -16 && y >= FT && y < treasCeil(u, v) && d2t(u, v) <= 1.75) return true;
        // les tunnels et les escaliers vers la Tresorerie
        if (au >= 36 && au <= 66 && v >= 27 && v <= 33 && y >= FL0 && y <= FL0 + 6) return true;
        int st = sideTread(u, v);
        return st != Integer.MIN_VALUE && y >= st && y <= st + 7;
    }

    private static boolean nearAir(int u, int y, int v) {
        if (y < 93 || y > 263 || u < -101 || u > 101 || v < -131 || v > 145) return false;
        return air(u + 1, y, v) || air(u - 1, y, v) || air(u, y + 1, v) || air(u, y - 1, v) || air(u, y, v + 1) || air(u, y, v - 1);
    }

    // ------------------------------------------------------------------ materiaux
    private static int stone(int u, int y, int v) {
        double q = r01(u, y, v);
        return q < 0.58 ? STB : q < 0.74 ? TUFFB : q < 0.86 ? MSTB : q < 0.94 ? PANDES : MoriaPlan.CRACKB;
    }

    private static int lining(int u, int y, int v) {
        boolean up = air(u, y + 1, v), dn = air(u, y - 1, v);
        if (up) return floorPat(u, y, v);
        if (v >= VF - 6 && v <= VF + 4 && Math.abs(u) <= 30 && y <= FL0 + 60) return KEEP;          // le cadre de la Porte est dessine par la facade
        if (dn) return ceilPat(u, y, v);
        int w = (air(u + 1, y, v) || air(u - 1, y, v)) ? v : u;
        int yy = y % 24;
        if (yy == 0) return CSTB;
        if (yy == 12) return Math.abs(u) < 100 && (w & 3) == 0 ? CUCUT : PANDES;
        if (Math.floorMod(w + y / 3, 12) == 0) return PANDES;
        return stone(u, y, v);
    }

    private static int floorPat(int u, int y, int v) {
        int au = Math.abs(u);
        if (au <= 36 && v >= -126 && v <= 127 && (y == FL0 - 1 || y == FL1 - 1)) {            // le grand axe : tapis sombre bordure d'or
            if (au <= 3) return (v & 7) == 0 ? CSTB : POLBK;
            if (au == 4) return gold;
            if (au <= 36 && (Math.abs(au - 12) <= 0 || au == 28)) return CSTB;
            return ((u >> 2) + (v >> 2) & 1) == 0 ? PANDES : SMST;
        }
        if ((v & 15) == 0 || (u & 15) == 0) return CSTB;
        return ((u >> 1) + (v >> 1) & 1) == 0 ? PANDES : (r01(u, y, v) < 0.3 ? POLBK : SMST);
    }

    private static int ceilPat(int u, int y, int v) {
        boolean rib = (u & 7) == 0 || (v & 7) == 0;
        if (rib) return (u & 7) == 0 && (v & 7) == 0 ? CSTB : STB;
        return r01(u, y, v) < 0.5 ? PANDES : TUFFB;
    }

    private static int mapStatue(int id) {
        return switch (id) {
            case MoriaPlan.DSTONE, MoriaPlan.DPILLAR -> PANDES;
            case MoriaPlan.DCOBBLE -> COBBLE;
            case MoriaPlan.DBRICKS -> STB;
            case MoriaPlan.DCHISEL -> CSTB;
            case MoriaPlan.DEEPSLATE -> MoriaPlan.POLDEEP;
            case MoriaPlan.DRUNES, MoriaPlan.DLAMP -> CUCUT;
            default -> id;
        };
    }

    // ------------------------------------------------------------------ le decor des volumes creuses
    private static int decor(int u, int y, int v) {
        int au = Math.abs(u), r;
        if (au <= 36 && v >= -126 && v <= 127 && y >= axisFloor(v) && y < axisCeil(u, v)) {
            if ((r = axis(u, y, v)) != KEEP) return r;
        }
        if (v >= 127 && v <= VF + 3 && au <= 12 && (r = gate(u, y, v)) != KEEP) return r;
        if (v >= -128 && v <= -16 && (r = treasury(u, y, v)) != KEEP) return r;
        int st = sideTread(u, v);
        if (st != Integer.MIN_VALUE && y == st) return STS + 2;
        if (st != Integer.MIN_VALUE && y == st + 5 && v % 10 == 0 && (au == 58 || au == 66)) return SLANT;
        return AIR;
    }

    private static int gate(int u, int y, int v) {
        int yy = y - FL0;
        if (u == 0 && (v == 133 || v == 139) && yy >= 36 && yy <= 41) return yy == 36 ? SLANT : MoriaPlan.CHAIN;
        return KEEP;
    }

    private static int axis(int u, int y, int v) {
        int au = Math.abs(u);
        int r;
        if (v >= 24) {                                                           // la Grande Salle de Thror
            int yy = y - FL0;
            if ((r = column(au, v, yy, new int[]{40, 56, 72, 88, 104, 120})) != KEEP) return r;
            if (au >= 25 && au <= 36 && yy <= 54) {                              // des nains geants dans les bas-cotes
                for (int vc : new int[]{52, 84, 116}) {
                    if (Math.abs(v - vc) <= 9) { int d = MoriaPlan.dwarf(v - vc, 36 - au, yy); if (d != KEEP) return mapStatue(d); }
                }
            }
            if (au == 9 && (v & 15) == 8 && v >= 32 && v <= 120) {              // braseros
                if (yy <= 2) return yy == 0 ? CSTB : STB;
                if (yy == 3) return MoriaPlan.CAMPFIRE;
            }
            return KEEP;
        }
        if (v >= -12) {                                                          // le Grand Escalier
            int tr = 149 + (24 - v);
            if (y == tr) return STS;
            if (au >= 34 && y <= tr + 2) return y == tr + 2 ? PSLB : PANDES;
            if (au == 20 && (v & 7) == 0 && y == tr + 1) return MoriaPlan.CAMPFIRE;
            return KEEP;
        }
        // la Galerie des Rois et le trone
        int yy = y - FL1;
        if (au >= 25 && au <= 36 && yy <= 54) {
            for (int vc : new int[]{-32, -54, -76}) {
                if (Math.abs(v - vc) <= 9) { int d = MoriaPlan.dwarf(v - vc, 36 - au, yy); if (d != KEEP) return mapStatue(d); }
            }
        }
        if (au == 12 && Math.floorMod(v, 22) == 4 && v > -92) {
            if (yy <= 2) return yy == 0 ? CSTB : STB;
            if (yy == 3) return MoriaPlan.CAMPFIRE;
        }
        if ((u & 15) == 8 && Math.floorMod(v, 20) == 0 && v < -14 && v > -110) {          // lustres
            int top = axisCeil(u, v);
            if (y >= top - 9 && y < top) return y == top - 9 ? SLANT : MoriaPlan.CHAIN;
        }
        if ((r = throne(u, y, v, yy)) != KEEP) return r;
        return KEEP;
    }

    /** une file de colonnes de 4 x 4 sur deux rangs (|u| = 17..20) */
    private static int column(int au, int v, int yy, int[] vcs) {
        if (yy > 65) return KEEP;
        for (int vc : vcs) {
            int dv = v - vc;
            if (dv < -1 || dv > 2) continue;
            if (au >= 17 && au <= 20) {
                if (yy <= 1) return STB;
                if (yy == 2) return CUCUT;
                if (yy >= 58) return yy == 62 ? gold : yy >= 63 ? CSTB : (yy % 2 == 0 ? CSTB : STB);
                if (yy % 10 == 0) return CSTB;
                return PANDES;
            }
            if (yy >= 58 && yy <= 62 && au <= 16) {                                  // l'architrave traverse la nef
                return yy == 58 ? CSTB : yy == 62 ? CSTB : STB;
            }
        }
        return KEEP;
    }

    private static int throne(int u, int y, int v, int yy) {
        int au = Math.abs(u);
        // l'estrade a gradins
        for (int j = 5; j >= 1; j--) {
            if (v <= -90 - 4 * j && au <= 30 - 2 * j) {
                if (y <= FL1 - 1 + j) { return y == FL1 - 1 + j ? ((au == 30 - 2 * j || v == -90 - 4 * j) ? CSTB : (au <= 3 ? POLBK : SMST)) : PANDES; }
                break;
            }
        }
        if (v <= -110) {
            int dy = y - (FL1 + 5);                                                  // dy = 0 : la cellule au dessus de l'estrade
            if (v >= -117 && v <= -113 && au <= 3 && dy == 0) return STB;                                                  // l'assise
            if (v >= -118 && v <= -112 && au >= 4 && au <= 5 && dy >= 0 && dy <= 2) return dy == 2 ? gold : STB;          // les accoudoirs
            if (v >= -123 && v <= -119 && dy >= 0 && dy <= 30) {                      // le dossier
                int hw = dy <= 12 ? 7 : 7 - (dy - 12) / 3;
                if (au <= hw) {
                    if (au == hw) return dy % 4 == 0 ? CSTB : PANDES;
                    if (dy >= 14 && dy <= 26 && au <= 3 && v == -119) return (au + dy) % 3 == 0 ? gold : MoriaPlan.DRUNES;
                    return STB;
                }
            }
        }
        if (v == -126 && au <= 22 && yy >= 4 && yy <= 56) {                         // le grand treillis d'or et de lumiere
            int dd = Math.abs(au) + Math.abs(yy - 30);
            if (dd % 8 == 0) return gold;
            return dd % 8 == 4 ? SEAL : CHCU;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ la Tresorerie
    private static final int HU = 0, HV = -72;

    /** haut du tas d'or (cellule du dessus), ou 99 s'il n'y en a pas */
    private static int heap(int u, int v) {
        double a = (u - HU) / 78.0, b = (v - HV) / 52.0, d2 = a * a + b * b;
        if (d2 >= 1) return 99;
        double t = FT + 44 * Math.pow(1 - d2, 1.3) + 0.8 * (vn(u * 0.09, v * 0.09, 11) - 0.5);
        return t >= 101 ? (int) Math.floor(t) - 1 : 99;
    }

    private static int treasury(int u, int y, int v) {
        int au = Math.abs(u);
        if (d2t(u, v) > 1.75 || y < FT) return KEEP;
        int ceil = treasCeil(u, v);
        if (y >= ceil) return KEEP;
        // les colonnes geantes (6 x 6)
        int[][] cols = {{-40, -34}, {40, -34}, {-40, -110}, {40, -110}, {-72, -72}, {72, -72}, {-20, -122}, {20, -122}};
        for (int[] c : cols) {
            if (u >= c[0] - 3 && u <= c[0] + 2 && v >= c[1] - 3 && v <= c[1] + 2 && y < ceil) {
                int yy = y - FT;
                if (yy >= ceil - FT - 4) return yy == ceil - FT - 3 ? gold : CSTB;
                if (yy % 12 == 0) return CSTB;
                if (yy % 12 == 6) return CUCUT;
                return PANDES;
            }
        }
        // l'Arkenstone sur son piedestal
        int du = u - HU, dv = v - HV;
        if (Math.abs(du) <= 3 && Math.abs(dv) <= 3) {
            if (y >= 140 && y <= 144) return y == 144 && (Math.abs(du) == 3 || Math.abs(dv) == 3) ? gold : POLBK;
        }
        int cy = 148, dy = y - cy, s = Math.abs(du) + Math.abs(dy) + Math.abs(dv);
        if (s <= 3 && y > 144) return s <= 2 ? SEAL : GLASS;
        if (Math.abs(du) == 5 && Math.abs(dv) == 5 && y >= 142 && y <= 150) return y == 150 ? SLANT : y <= 145 ? gold : CSTB;
        int top = heap(u, v);
        if (y <= top) {
            if (y > 143 && Math.abs(du) <= 3 && Math.abs(dv) <= 3) return POLBK;
            double q = r01(u, y, v);
            if (y < top - 2) return gold;
            return q < 0.55 ? gold : q < 0.82 ? MoriaPlan.RAWGOLD : q < 0.93 ? MoriaPlan.COPPER : CUCUT;
        }
        if (y == top + 1) {                                                            // des braseros posses sur l'or
            int[][] br = {{-36, -72}, {36, -72}, {0, -46}, {0, -98}, {-24, -52}, {24, -52}, {-24, -92}, {24, -92}};
            for (int[] b : br) if (u == b[0] && v == b[1]) return MoriaPlan.CAMPFIRE;
        }
        if (Math.floorMod(u, 24) == 12 && Math.floorMod(v, 24) == 4 && y >= ceil - 8 && y < ceil)                    // lustres
            return y == ceil - 8 ? SLANT : MoriaPlan.CHAIN;
        return KEEP;
    }

    // ------------------------------------------------------------------ la facade
    private static int statues(int u, int y, int v) {
        int p = v - VF, yy = y - FL0;
        if (p < 0 || p > 11 || yy < 0 || yy > 54) return KEEP;
        int au = Math.abs(u);
        if (au < 28 || au > 48) return KEEP;
        int d = MoriaPlan.dwarf(Math.abs(u) - 38, p, yy);
        return d == KEEP ? KEEP : mapStatue(d);
    }

    private static int fmat(int u, int yy, int p, int kind) {
        double q = r01(u, yy, p);
        switch (kind) {
            case 1: return (yy + Math.abs(u)) % 4 == 0 ? CSTB : PANDES;                   // cadre de la Porte
            case 2: return yy % 16 < 2 ? CSTB : PANDES;                                    // nervures
            case 3: return q < 0.5 ? TUFFB : MSTB;                                         // dos des niches
            case 4: return q < 0.4 ? GLOW : CSTB;                                          // fond des fenetres
            case 5: return PTUFF;                                                          // encadrement des fenetres
            default: return q < 0.52 ? STB : q < 0.72 ? TUFFB : q < 0.84 ? MSTB : q < 0.93 ? PANDES : MoriaPlan.CRACKB;
        }
    }

    private static int facade(int u, int y, int v) {
        int p = v - VF, yy = y - FL0;
        if (p < -6 || p > 4 || yy < 0 || yy > 131) return KEEP;
        int au = Math.abs(u);
        if (au > 88) return KEEP;
        int reach = 0, recess = 0, kind = 0;
        // le cadre de la Porte
        int hw = yy <= 30 ? 12 : 12 - (yy - 30) * 3 / 4;
        double dOut = au - hw;
        if (yy <= 52 && dOut > 0) {
            if (dOut <= 3) { reach = 2; kind = 1; }
            else if (dOut <= 6) { reach = 1; kind = 1; }
            if (dOut == 7 && yy < 44) { kind = 1; }
        }
        if (yy >= 46 && yy <= 54 && au <= 7 && (au + (yy - 46)) <= 8) { reach = 2; kind = 1; }
        // les nervures verticales
        for (int c : new int[]{26, 56, 86}) {
            if (Math.abs(au - c) <= 1 && yy <= 124) { reach = yy >= 114 ? 3 : 2; kind = 2; }
        }
        // les niches des statues
        int niche = yy <= 48 ? 47 : 47 - (yy - 48);
        if (au >= 29 && au <= niche && yy <= 60) { reach = 0; recess = 3; kind = 3; }
        // les fenetres
        if (au >= 58 && au <= 84) {
            for (int r0 : new int[]{10, 28, 46, 64, 82, 100}) {
                for (int c = 60; c <= 78; c += 6) {
                    boolean core = au >= c && au <= c + 1 && yy >= r0 && yy <= r0 + 7;
                    boolean frame = au >= c - 1 && au <= c + 2 && yy >= r0 - 1 && yy <= r0 + 8;
                    if (core) { reach = 0; recess = 3; kind = 4; }
                    else if (frame && recess == 0) { reach = 1; kind = 5; }
                }
            }
        }
        // l'ecu au dessus de la Porte
        if (au <= 24 && yy >= 56 && yy <= 108 && kind == 0) { recess = 2; kind = 6; }
        // la corniche et les creneaux
        if (kind == 0 || kind == 6) {
            if (yy >= 116 && yy <= 117) { reach = 1; recess = 0; kind = 1; }
            else if (yy >= 118 && yy <= 121) { reach = (yy == 118 && au % 2 == 0) ? 1 : 2; recess = 0; kind = 1; }
            else if (yy == 122) { reach = 1; recess = 0; kind = 1; }
            else if (yy >= 123) { reach = 0; recess = (au % 6 < 4) ? 0 : 2; kind = 0; }
        }
        int s = reach - recess;
        if (p > s) return p <= 0 ? AIR : KEEP;
        if (recess > 0 && p < s) return KEEP;
        if (kind == 6) {                                                                   // le treillis et l'embleme
            int dd = au + Math.abs(yy - 82);
            if (dd <= 3) return SEAL;
            if (dd == 18) return gold;
            if (dd <= 17 && dd >= 10) return dd % 3 == 0 ? CUCUT : PANDES;
            if (dd <= 9) return dd % 4 == 0 ? CSTB : TUFFB;
            if ((au + yy) % 8 == 0 || Math.floorMod(au - yy, 8) == 0) return CSTB;
            return r01(u, yy, 3) < 0.5 ? STB : TUFFB;
        }
        if (yy % 24 == 23 && kind == 0) return CSTB;
        if (kind == 1 && yy < 52 && au - hw == 4 && p <= 0) return gold;                  // filet d'or autour du portail
        return fmat(u, yy, p, kind);
    }

    // ------------------------------------------------------------------ la cour devant la facade
    private static int court(int u, int y, int v, int h) {
        int au = Math.abs(u);
        if (au > 86 || v <= VF || v > 238) return KEEP;
        if (y >= FL0) {
            // les braseros le long de la chaussee
            if (au == 16 && Math.floorMod(v - VF, 16) == 8 && v < 230) {
                int yy = y - FL0;
                if (yy <= 2) return yy == 0 ? CSTB : STB;
                if (yy == 3) return MoriaPlan.CAMPFIRE;
            }
            return AIR;
        }
        if (y == FL0 - 1) return pavement(u, v);
        if (y > h) return ROCK;
        return KEEP;
    }

    private static int pavement(int u, int v) {
        int au = Math.abs(u);
        if (au <= 13) {
            if (au == 13 || Math.floorMod(v - VF, 16) == 0) return CSTB;
            return ((u >> 1) + (v >> 1) & 1) == 0 ? PANDES : SMST;
        }
        if (au <= 15) return CSTB;
        return r01(u, v, 5) < 0.5 ? STB : r01(u, v, 6) < 0.5 ? COBBLE : TUFFB;
    }
}
