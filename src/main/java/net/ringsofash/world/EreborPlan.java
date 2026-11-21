package net.ringsofash.world;

import static net.ringsofash.world.MoriaPlan.AIR;
import static net.ringsofash.world.MoriaPlan.KEEP;
import static net.ringsofash.world.MoriaPlan.hsh;
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
            SLB = 156, SLT = 157, PSLB = 158, PSLT = 159, WALLSTB = 160, COALO = 162, IRONO = 163, COPPERO = 164, GOLDO = 165, DIAMO = 166, IRONBARS = 167, COALB = 168, RAWIRON = 169;
    public static final int NIDS = 170;

    private static final int gold = MoriaPlan.GOLD;

    // ------------------------------------------------------------------ la montagne
    private static final int RX = 175, RZ = 180;

    private static double ring(int u, int v) {
        // la facade reste plane ; ailleurs le contour est gauchi par du bruit pour casser le carre
        double w = sm(Math.max(Math.max(0, Math.abs(u) - 85), Math.max(0, 110 - v)) / 60.0);
        double wu = u + w * 34 * (vn((EX + u) * 0.02, (EZ + v) * 0.02, 21) - 0.5), wv = v + w * 34 * (vn((EX + u) * 0.02, (EZ + v) * 0.02, 22) - 0.5);
        double a = Math.abs(wu) / (double) RX, b = Math.abs(wv) / (double) RZ;
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
        if (y >= FL0 + 44 && chimney(u, y, v, x, z)) return AIR;
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
        if (y < 56 || y > 262 || u < -176 || u > 176 || v < -170 || v > 156) return false;
        int au = Math.abs(u);
        if (y < 100 || au > 100 || v < -130 || v > 144) return air2(u, y, v);
        // la Porte
        if (v >= 127 && v <= VF + 3 && y >= FL0 && y < FL0 + 46 && au <= halfW(y - FL0)) return true;
        // l'axe : Grande Salle, Grand Escalier, Galerie des Rois
        if (au <= 36 && v >= -126 && v <= 127 && y >= axisFloor(v) && y < axisCeil(u, v)) return true;
        // la Tresorerie
        if (u >= -86 && u <= 86 && v >= -128 && v <= -16 && y >= FT && y < treasCeil(u, v) && d2t(u, v) <= 1.75) return true;
        // les tunnels et les escaliers vers la Tresorerie
        if (au >= 36 && au <= 66 && v >= 27 && v <= 33 && y >= FL0 && y <= FL0 + 6) return true;
        int st = sideTread(u, v);
        if (st != Integer.MIN_VALUE && y >= st && y <= st + 7) return true;
        return air2(u, y, v);
    }

    // ------------------------------------------------------------------ les ailes, la mine, le coffre secret
    private static final int MINE = 62;                  // niveau de la mine : le sol est a y = 61

    private static int flightA(int u, int v) { return tread(u, v, 2, 40, 58, -156, -148, 91); }

    private static int flightB(int u, int v) { return tread(u, v, 0, -71, 30, -144, -136, 61); }

    private static boolean air2(int u, int y, int v) {
        int au = Math.abs(u);
        if (y >= FL0) {
            // les forges (ouest) et les quartiers (est), leurs portes et les entrepots
            if (au >= 100 && au <= 160 && v >= 30 && v <= 120 && y < FL0 + 44) return true;
            if (au >= 36 && au <= 100 && v >= 96 && v <= 104 && y <= FL0 + 9) return true;
            if (au >= 100 && au <= 158 && v >= 124 && v <= 152 && y < FL0 + 22) return true;
            if (au >= 124 && au <= 136 && v >= 120 && v <= 124 && y <= FL0 + 8) return true;
            // le coffre secret derriere le trone
            if (y >= FL1 && y <= FL1 + 8) {
                if (u >= -31 && u <= -29 && v >= -150 && v <= -126 && y <= FL1 + 3) return true;
                if (u >= -48 && u <= -12 && v >= -166 && v <= -150) return true;
            }
            return false;
        }
        if (y > 100) return false;
        // la descente vers la mine : deux volees et un palier
        int ta = flightA(u, v);
        if (ta != Integer.MIN_VALUE && y >= ta && y <= ta + 7) return true;
        int tb = flightB(u, v);
        if (tb != Integer.MIN_VALUE && y >= tb && y <= tb + 7) return true;
        if (u >= -158 && u <= -130 && v >= 36 && v <= 42 && y >= 92 && y <= 99) return true;
        if (y < MINE || y > 76) return false;
        // le grand hall de la mine, les galeries, la salle des minerais
        if (u >= -158 && u <= -124 && v >= 71 && v <= 112 && y < MINE + 10) return true;
        if (y < MINE + 5) {
            if (u >= -124 && u <= 150 && v >= 87 && v <= 91) return true;
            if (u >= -140 && u <= -136 && v >= -60 && v <= 71) return true;
            if (u >= -140 && u <= 140 && v >= -62 && v <= -58) return true;
            if (u >= 138 && u <= 142 && v >= -60 && v <= 90) return true;
        }
        return u >= 144 && u <= 172 && v >= 80 && v <= 112 && y < MINE + 12;
    }

    private static boolean nearAir(int u, int y, int v) {
        if (y < 55 || y > 263 || u < -177 || u > 177 || v < -171 || v > 157) return false;
        return air(u + 1, y, v) || air(u - 1, y, v) || air(u, y + 1, v) || air(u, y - 1, v) || air(u, y, v + 1) || air(u, y, v - 1);
    }

    // ------------------------------------------------------------------ materiaux
    private static int stone(int u, int y, int v) {
        double q = r01(u, y, v);
        return q < 0.58 ? STB : q < 0.74 ? TUFFB : q < 0.86 ? MSTB : q < 0.94 ? PANDES : MoriaPlan.CRACKB;
    }

    private static int mineLining(int u, int y, int v, boolean up) {
        double q = r01(u, y, v);
        if (up) return q < 0.4 ? COBBLE : q < 0.75 ? MoriaPlan.GRAVEL : ANDES;
        if (q < 0.06) {
            double o = r01(v, y, u);
            return o < 0.42 ? COALO : o < 0.7 ? IRONO : o < 0.86 ? COPPERO : o < 0.96 ? GOLDO : DIAMO;
        }
        return ROCK;
    }

    private static int lining(int u, int y, int v) {
        boolean up = air(u, y + 1, v), dn = air(u, y - 1, v);
        if (y <= 98) return mineLining(u, y, v, up);
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
        if (y == FL0 - 1 && au >= 100 && au <= 160 && v >= 30 && v <= 120) {
            if (u < 0) {                                                                       // les forges : sol noir et un canal de lave
                if (Math.abs(u + 130) <= 2 && v >= 36 && v <= 114) return Math.floorMod(v, 24) < 4 ? STB : MoriaPlan.LAVA;
                return r01(u, y, v) < 0.5 ? POLBK : MoriaPlan.BLACKSTONE;
            }
            return Math.abs(u - 130) <= 12 ? (((u >> 1) + (v >> 1) & 1) == 0 ? PANDES : SMST) : SMST;
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
        if (y >= FL0) {
            if (au >= 100 && au <= 160 && v >= 30 && v <= 120 && y < FL0 + 44) return u < 0 ? forge(u, y, v) : quarters(u, y, v);
            if (au >= 100 && au <= 158 && v >= 124 && v <= 152 && y < FL0 + 22) return store(u, y, v);
            if (y >= FL1 && y <= FL1 + 8 && v <= -150 && u >= -48 && u <= -12) return vault(u, y, v);
            return AIR;
        }
        if (y <= 100) return mine(u, y, v);
        return AIR;
    }

    private static boolean chimney(int u, int y, int v, int x, int z) {
        if (u < -145 || u > -137) return false;
        for (int vs : new int[]{40, 56, 72, 88, 104}) {
            if (Math.abs(u + 141) <= 1 && Math.abs(v - vs) <= 1) {
                int t = top(x, z);
                return t != Integer.MIN_VALUE && y <= t + 1;
            }
        }
        return false;
    }

    private static int forge(int u, int y, int v) {
        int yy = y - FL0;
        for (int vs : new int[]{40, 56, 72, 88, 104}) {
            if (v >= vs - 1 && v <= vs + 1) {
                if (u >= -142 && u <= -140 && yy <= 1) return yy == 0 ? MoriaPlan.FORGE : MoriaPlan.FURNACE;
                if (u == -141 && v == vs && yy >= 2 && yy <= 43) return IRONBARS;                 // le tuyau jusqu'a la cheminee
                if (u == -138 && v == vs && yy == 0) return MoriaPlan.ANVIL;
                if (u == -144 && v == vs && yy <= 1) return MoriaPlan.BARREL;
                if (u == -120 && v == vs && yy == 0) return MoriaPlan.CAULDRON;
                if (u == -118 && v == vs && yy <= 1) return MoriaPlan.BARREL;
                if (u == -119 && v == vs + 1 && yy == 0) return MoriaPlan.ANVIL;
            }
        }
        if (Math.floorMod(u, 12) == 6 && Math.floorMod(v, 16) == 8 && yy >= 36 && yy <= 43) return yy == 36 ? SLANT : MoriaPlan.CHAIN;
        if (u == -130 && yy == 1 && Math.floorMod(v, 24) < 4 && v % 24 == 0) return KEEP;
        return KEEP;
    }

    private static int quarters(int u, int y, int v) {
        int yy = y - FL0, d = Math.abs(u - 130);
        int row = Math.floorDiv(v - 34, 15), vr = Math.floorMod(v - 34, 15);
        if (d <= 12) {                                                                           // la rue
            if (d == 11 && vr == 8 && v < 118) { if (yy <= 2) return yy == 0 ? CSTB : STB; if (yy == 3) return MoriaPlan.CAMPFIRE; }
            if (u == 130 && Math.floorMod(v, 10) == 5 && yy >= 38 && yy <= 43) return yy == 38 ? SLANT : MoriaPlan.CHAIN;
            return KEEP;
        }
        if (row < 0 || row > 5 || vr > 10 || d < 13 || d > 26) return KEEP;
        if (yy >= 6 && yy <= 7) return yy == 7 ? (d % 2 == 0 ? PSLB : PANDES) : STB;           // le toit
        if (yy > 5) return KEEP;
        boolean wall = d == 13 || d == 26 || vr == 0 || vr == 10;
        if (wall) {
            if (d == 13 && vr >= 4 && vr <= 6 && yy <= 3) return AIR;                           // la porte
            if (d == 13 && (vr == 2 || vr == 8) && yy >= 2 && yy <= 3) return IRONBARS;          // les fenetres
            return yy == 5 ? CSTB : stone(u, y, v);
        }
        int dirChest = u < 130 ? 1 : 3;
        if (d == 24 && vr == 2 && yy == 0) return MoriaPlan.DBEDF + 2;
        if (d == 24 && vr == 3 && yy == 0) return MoriaPlan.DBEDH + 2;
        if (d == 24 && vr == 8 && yy == 0) return MoriaPlan.DCHEST + dirChest;
        if (d == 22 && vr == 5 && yy == 0) return MoriaPlan.CRAFT;
        if (d == 24 && vr == 6 && yy == 0) return MoriaPlan.DFURN + dirChest;
        if (d == 19 && vr == 5 && yy == 5) return SLANT;
        return KEEP;
    }

    private static int store(int u, int y, int v) {
        int au = Math.abs(u), yy = y - FL0;
        if (au >= 124 && au <= 136) return KEEP;                                                 // l'allee centrale
        if (Math.floorMod(au, 6) <= 2 && Math.floorMod(v, 6) <= 2 && v >= 126) {
            int hgt = 1 + (hsh(au / 6, 7, v / 6) & 3);
            if (yy < hgt) {
                int k = hsh(au / 6, 9, v / 6) % 6;
                return k <= 1 ? MoriaPlan.BARREL : k == 2 ? MoriaPlan.IRONB : k == 3 ? COALB : k == 4 ? RAWIRON : MoriaPlan.COPPER;
            }
        }
        if (Math.floorMod(au, 12) == 6 && Math.floorMod(v, 12) == 6 && yy >= 17 && yy <= 21) return yy == 17 ? SLANT : MoriaPlan.CHAIN;
        return KEEP;
    }

    private static int vault(int u, int y, int v) {
        int yy = y - FL1;
        if (v == -165 && yy == 0 && (u == -46 || u == -43 || u == -17 || u == -14)) return MoriaPlan.DCHEST + 2;
        if (v <= -162 && u >= -37 && u <= -23 && yy < 2) return yy == 0 ? (r01(u, y, v) < 0.7 ? gold : MoriaPlan.RAWGOLD) : (r01(u, y, v) < 0.3 ? gold : KEEP);
        if (u == -30 && v == -158 && yy == 8) return SLANT;
        return KEEP;
    }

    private static int mine(int u, int y, int v) {
        int yy = y - MINE;
        // les volees de la descente : marches et lanternes
        int ta = flightA(u, v);
        if (ta != Integer.MIN_VALUE && y >= ta && y <= ta + 7) {
            if (y == ta) return STS + 2;
            if (y == ta + 7 && Math.floorMod(v, 10) == 0 && u == -152) return SLANT;
            return AIR;
        }
        int tb = flightB(u, v);
        if (tb != Integer.MIN_VALUE && y >= tb && y <= tb + 7) {
            if (y == tb) return STS;
            if (y == tb + 7 && Math.floorMod(v, 10) == 0 && u == -140) return SLANT;
            return AIR;
        }
        if (yy < 0) return AIR;
        // le hall de la mine
        if (u >= -158 && u <= -124 && v >= 71 && v <= 112 && yy < 10) {
            if (yy == 9 && Math.floorMod(u, 9) == 0 && Math.floorMod(v, 9) == 0) return SLANT;
            if ((Math.floorMod(u + 5, 17) == 0 && Math.floorMod(v, 14) == 0) && yy <= 8) return yy % 3 == 0 ? MoriaPlan.OAKLOG : MoriaPlan.OAKLOG;
            return AIR;
        }
        // la salle des minerais
        if (u >= 144 && u <= 172 && v >= 80 && v <= 112 && yy < 12) {
            if (Math.floorMod(u, 5) <= 1 && Math.floorMod(v, 5) <= 1 && v != 96) {
                int hgt = 1 + (hsh(u / 5, 5, v / 5) % 4);
                if (yy < hgt) { int k = hsh(u / 5, 6, v / 5) % 5; return k == 0 ? MoriaPlan.IRONB : k == 1 ? COALB : k == 2 ? RAWIRON : k == 3 ? MoriaPlan.RAWGOLD : MoriaPlan.COPPER; }
            }
            if (yy == 11 && Math.floorMod(u, 8) == 0 && Math.floorMod(v, 8) == 0) return SLANT;
            return AIR;
        }
        // les galeries : rails, poteaux, poutres, lanternes
        boolean ew = false, ns = false;
        int lo = 0, hi = 0, mid = 0;
        if (u >= -124 && u <= 150 && v >= 87 && v <= 91) { ew = true; lo = 87; hi = 91; mid = 89; }
        else if (u >= -140 && u <= 140 && v >= -62 && v <= -58) { ew = true; lo = -62; hi = -58; mid = -60; }
        else if (u >= -140 && u <= -136 && v >= -60 && v <= 71) { ns = true; lo = -140; hi = -136; mid = -138; }
        else if (u >= 138 && u <= 142 && v >= -60 && v <= 90) { ns = true; lo = 138; hi = 142; mid = 140; }
        if (ew) {
            if (yy == 0 && v == mid) return MoriaPlan.RAILX;
            if (Math.floorMod(u, 8) == 0) {
                if ((v == lo || v == hi) && yy <= 3) return MoriaPlan.OAKLOG;
                if (yy == 4) return MoriaPlan.OAKLOGZ;
                if (yy == 3 && v == mid && Math.floorMod(u, 16) == 0) return SLANT;
            }
            return AIR;
        }
        if (ns) {
            if (yy == 0 && u == mid) return MoriaPlan.RAILZ;
            if (Math.floorMod(v, 8) == 0) {
                if ((u == lo || u == hi) && yy <= 3) return MoriaPlan.OAKLOG;
                if (yy == 4) return MoriaPlan.OAKLOGX;
                if (yy == 3 && u == mid && Math.floorMod(v, 16) == 0) return SLANT;
            }
            return AIR;
        }
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
            return dd % 8 == 4 ? SEAL : CSTB;
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
        if (Math.abs(du) <= 4 && Math.abs(dv) <= 4) {
            if (y >= 140 && y <= 144) return y == 144 && (Math.abs(du) == 4 || Math.abs(dv) == 4) ? gold : POLBK;
        }
        int cy = 150, dy = y - cy, s = Math.abs(du) + Math.abs(dy) + Math.abs(dv);
        if (s <= 5 && y > 144) return s <= 3 ? SEAL : GLASS;
        if (Math.abs(du) == 7 && Math.abs(dv) == 7 && y >= 142 && y <= 152) return y == 152 ? SLANT : y <= 146 ? gold : CSTB;
        int top = heap(u, v);
        if (y <= top) {
            if (y > 143 && Math.abs(du) <= 4 && Math.abs(dv) <= 4) return POLBK;
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
        if (p < 0 || p > 17 || yy < 0 || yy > 82) return KEEP;
        int au = Math.abs(u);
        if (au < 27 || au > 55) return KEEP;
        int d = MoriaPlan.dwarf(Math.floorDiv(2 * (Math.abs(u) - 41), 3), Math.floorDiv(2 * p, 3), Math.floorDiv(2 * yy, 3));
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
        for (int c : new int[]{26, 58, 86}) {
            if (Math.abs(au - c) <= 1 && yy <= 124) { reach = yy >= 114 ? 3 : 2; kind = 2; }
        }
        // les niches des statues
        int niche = yy <= 70 ? 54 : 54 - (yy - 70);
        if (au >= 28 && au <= niche && yy <= 88) { reach = 0; recess = 3; kind = 3; }
        // les fenetres
        if (au >= 61 && au <= 84) {
            for (int r0 : new int[]{10, 28, 46, 64, 82, 100}) {
                for (int c = 63; c <= 78; c += 5) {
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
