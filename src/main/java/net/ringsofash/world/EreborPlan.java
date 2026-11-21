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
 * Les murs sont en pierre naine (brique, pierre, moellon, taillee, pilier) et ardoise des profondeurs ; or, cuivre et lanternes en decor.
 */
public final class EreborPlan {
    private EreborPlan() {}

    public static final int EX = 1640, EZ = -1355;
    /** decalage vertical : le plan est ecrit comme si la plaine etait a y = 150 ; le vrai sol du site est a y = 76, donc tout est descendu de 74 blocs */
    public static final int D = -74;
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
    private static final int RX = 175, RZ = 185;

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

    private static double cl(double a) { return a < 0 ? 0 : a > 1 ? 1 : a; }

    private static final int GU = RX + 45, GV0 = -RZ - 70, GW = 2 * GU + 1, GH = 175 - GV0 + 1;
    private static volatile short[] COVER;

    /**
     * Pour chaque colonne du site : l'altitude minimale (plan) que la roche doit atteindre pour recouvrir les salles et la facade,
     * puis une pente de 1 pour 1 vers l'exterieur. La montagne epouse donc les salles au lieu d'etre un dome pose dessus,
     * et rejoint la vraie montagne de la carte ou celle-ci est plus haute.
     */
    private static short[] cover() {
        short[] c = COVER;
        if (c != null) return c;
        synchronized (EreborPlan.class) {
            if (COVER != null) return COVER;
            final short NO = Short.MIN_VALUE;
            c = new short[GW * GH];
            for (int iv = 0; iv < GH; iv++) {
                int v = iv + GV0;
                for (int iu = 0; iu < GW; iu++) {
                    int u = iu - GU;
                    short r = NO;
                    if (Math.abs(u) <= RX && v >= -RZ && v <= 160) {
                        for (int y = 300; y >= 56; y -= 2) {
                            if (air(u, y, v) || air(u, y - 1, v) || (v >= VF - 12 && facade(u, y, v) != KEEP)) { r = (short) (y + 9); break; }
                        }
                    }
                    c[iv * GW + iu] = r;
                }
            }
            // pente : chaque colonne perd 1 bloc de hauteur (1,4 en diagonale) par bloc d'eloignement
            float[] f = new float[GW * GH];
            for (int k = 0; k < f.length; k++) f[k] = c[k] == NO ? -9999 : c[k];
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < f.length; k++) {
                    int idx = pass == 0 ? k : f.length - 1 - k;
                    int iu = idx % GW, iv = idx / GW;
                    float best = f[idx];
                    int du = pass == 0 ? -1 : 1;
                    int[][] nb = {{du, 0}, {0, du}, {du, du}, {-du, du}};
                    for (int[] n : nb) {
                        int ju = iu + n[0], jv = iv + n[1];
                        if (ju < 0 || ju >= GW || jv < 0 || jv >= GH) continue;
                        float cand = f[jv * GW + ju] - (n[0] != 0 && n[1] != 0 ? 1.45f : 1f);
                        if (cand > best) best = cand;
                    }
                    f[idx] = best;
                }
            }
            for (int k = 0; k < f.length; k++) c[k] = f[k] < 40 ? NO : (short) f[k];
            // la pente doit atteindre le niveau de la plaine : on borne par le bas a NO (sans roche)
            COVER = c;
            return c;
        }
    }

    /**
     * Le massif : la vraie montagne de la carte (au nord-est, la zone neigeuse) est conservee ; on ne rajoute de la roche que
     * la ou elle ne suffit pas a couvrir les salles, avec des pentes naturelles et un sommet irregulier.
     */
    private static int top0(int x, int z) {
        int u = x - EX, v = z - EZ;
        if (Math.abs(u) > GU - 1 || v < GV0 || v > 175) return Integer.MIN_VALUE;
        short[] c = cover();
        int cv = c[(v - GV0) * GW + u + GU];
        // un renflement bas et irregulier qui donne une vraie silhouette a la montagne
        double wu = u + 40 * (vn(x * 0.011, z * 0.011, 21) - 0.5), wv = v + 30 + 40 * (vn(x * 0.011, z * 0.011, 22) - 0.5);
        double a = wu / RX, b = wv / RZ;
        double r = Math.sqrt(a * a + b * b);
        double hh = 0;
        if (r < 1) {
            hh = 105 * Math.pow(1 - Math.pow(r, 2.2), 0.8) * (0.86 + 0.28 * vn(x * 0.02, z * 0.02, 5));
            double crag = Math.abs(vn(x * 0.045, z * 0.045, 7) - 0.5) * 2;
            hh += 20 * Math.pow(1 - crag, 2) * (1 - r);
        }
        double t = FL0 + hh;
        double rough = 7 * (vn(x * 0.06, z * 0.06, 6) - 0.5) + 3 * (vn(x * 0.19, z * 0.19, 8) - 0.5);
        if (cv != Short.MIN_VALUE) t = Math.max(t, cv + rough);
        if (t < FL0 + 2) return Integer.MIN_VALUE;
        return (int) t;
    }

    /** altitude jusqu'a laquelle le plan construit au dessus du sol (MIN_VALUE hors du site) */
    public static int aboveTop(int x, int z) {
        int t = top(x, z);
        int u = x - EX, v = z - EZ;
        if (t != Integer.MIN_VALUE) t += D;
        if (Math.abs(u) <= 90 && v > VF - 8 && v <= 240) t = Math.max(t, FL0 + 96 + D);
        return t;
    }

    public static boolean inBox(int x, int y, int z) {
        int u = x - EX, v = z - EZ;
        return Math.abs(u) <= RX + 2 && v >= -RZ - 50 && v <= 242 && y - D >= 56 && y - D <= 460;
    }

    // ------------------------------------------------------------------ assemblage
    public static int at(int x, int y, int z, int h) {
        if (!inBox(x, y, z)) return KEEP;
        y -= D; h -= D;
        int u = x - EX, v = z - EZ;
        if (air(u, y, v)) { int dc = decor(u, y, v); return dc == KEEP ? AIR : dc; }
        int r;
        if (y >= FL0 + 12 && chimney(u, y, v, x, z, h)) return AIR;
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
        int au = Math.abs(u);
        int drop = (int) (12.0 * au * au / 1296.0);                    // une voute en berceau
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
        // les niches des murs et les salles laterales
        if (au >= 37 && au <= 38 && niche(y, v)) return true;
        if (au >= 37 && au <= 64 && sideRoom(u, y, v) != 0) return true;
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

    private static int flightA(int u, int v) { return tread(u, v, 2, 40, 58, -126, -118, 91); }

    private static int flightB(int u, int v) { return tread(u, v, 0, -71, 30, -144, -136, 61); }

    private static boolean niche(int y, int v) {
        boolean hall = v >= 28 && v <= 122, gal = v <= -18 && v >= -122;
        if (!hall && !gal) return false;
        int yy = y - (hall ? FL0 : FL1);
        if (yy < 0) return false;
        int m = Math.floorMod(v, 12);
        if (m < 2 || m > 9) return false;
        double t = (m - 5.5) / 4.0;
        return yy <= 18 + (int) (9 * Math.sqrt(Math.max(0, 1 - t * t)));
    }

    /** les salles laterales : 1 forge, 2 taverne, 3 atelier, 4 brasserie (Grande Salle) ; 5 bibliotheque, 6 salle des gardes (Galerie) */
    private static int sideRoom(int u, int y, int v) {
        int au = Math.abs(u);
        if (au < 37 || au > 64) return 0;
        int vc, half, f, kind;
        if (v >= 61 && v <= 75) { vc = 68; half = 7; f = FL0; kind = u < 0 ? 1 : 2; }
        else if (v >= 35 && v <= 45) { vc = 40; half = 5; f = FL0; kind = u < 0 ? 3 : 4; }
        else if (v >= -103 && v <= -89) { vc = -96; half = 7; f = FL1; kind = u < 0 ? 5 : 6; }
        else return 0;
        int yy = y - f;
        if (yy < 0) return 0;
        if (au > 59 && Math.abs(v - vc) > half - (au - 59)) return 0;
        return yy < roomH(v, vc, half) ? kind : 0;
    }

    private static int roomH(int v, int vc, int half) {
        double t = (v - vc) / (half + 0.5);
        return 10 + (int) ((half + 4) * Math.sqrt(Math.max(0, 1 - t * t)));
    }

    // ailes : forges (u < 0) et quartiers (u > 0)
    private static int wingH(int a) {
        double da = (a - 123) / 23.5;
        return 12 + (int) (32 * Math.sqrt(Math.max(0, 1 - da * da)));
    }

    private static boolean wingAir(int u, int y, int v) {
        int a = Math.abs(u), yy = y - FL0;
        if (yy < 0 || a < 100 || a > 146 || v < 30 || v > 120) return false;
        double da = (a - 123) / 23.5, dv = (v - 75) / 45.5;
        if (Math.abs(da * da * da) + Math.abs(dv * dv * dv) > 1) return false;
        return yy < wingH(a);
    }

    private static boolean storeAir(int u, int y, int v) {
        int a = Math.abs(u), yy = y - FL0;
        if (yy < 0 || a < 100 || a > 146 || v < 124 || v > 152) return false;
        double da = (a - 123) / 23.5, dv = (v - 138) / 14.5;
        if (Math.abs(da * da * da) + Math.abs(dv * dv * dv) > 1) return false;
        return yy < 9 + (int) (14 * Math.sqrt(Math.max(0, 1 - da * da)));
    }

    private static int storeH(int a) { return 9 + (int) (14 * Math.sqrt(Math.max(0, 1 - ((a - 123) / 23.5) * ((a - 123) / 23.5)))); }

    private static final int[] CH_V = {68, 80, 92, 104};

    private static int chamberTop(int dv) { return 18 + (int) (4 * Math.sqrt(Math.max(0, 1 - (dv / 4.5) * (dv / 4.5)))); }

    private static boolean chamber(int u, int y, int v) {
        int a = -u, yy = y - FL0;
        if (u >= 0 || a < 91 || a > 103 || yy < 15) return false;
        for (int vc : CH_V) if (Math.abs(v - vc) <= 4 && yy < chamberTop(Math.abs(v - vc))) return true;
        return false;
    }

    /** palier et escalier du balcon des forges : y de la marche */
    private static int balStair(int u, int v) { return tread(u, v, 2, 49, 14, -109, -107, FL0); }

    /** passage de l'allee des quartiers : y de la marche, ou MIN_VALUE ; dir : 1 vers l'est (cote exterieur), 3 vers l'ouest (cote interieur) */
    private static int alleyT(int u, int v) {
        for (int[] al : new int[][]{{60, 63}, {90, 93}}) {
            int t = tread(u, v, 1, 130, 7, al[0], al[1], FL0);
            if (t != Integer.MIN_VALUE) return t;
            t = tread(u, v, 3, -114, 7, al[0], al[1], FL0);
            if (t != Integer.MIN_VALUE) return t;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean air2(int u, int y, int v) {
        int au = Math.abs(u);
        if (y >= 91 && y <= FL0 + 7) {
            int fa = flightA(u, v);
            if (fa != Integer.MIN_VALUE && y >= fa && y <= fa + 7) return true;
        }
        if (y >= FL0) {
            // les forges (ouest) et les quartiers (est), leurs portes et les entrepots
            if (wingAir(u, y, v) || storeAir(u, y, v) || chamber(u, y, v)) return true;
            if (au >= 36 && au <= 108 && v >= 96 && v <= 104) {                                  // la grande rue des forges : un tunnel voute
                double t = Math.abs(v - 100) / 4.5;
                if (y - FL0 < 7 + (int) (5 * Math.sqrt(Math.max(0, 1 - t * t)))) return true;
            }
            if (au >= 118 && au <= 128 && v >= 112 && v <= 126 && y - FL0 < 7 + (int) (4 * Math.sqrt(Math.max(0, 1 - ((au - 123) / 5.5) * ((au - 123) / 5.5))))) return true;
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
        if (u >= -158 && u <= -116 && v >= 36 && v <= 42 && y >= 92 && y <= 99) return true;
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
        return q < 0.5 ? STB : q < 0.68 ? TUFFB : q < 0.82 ? MSTB : q < 0.94 ? PANDES : MoriaPlan.CRDEEP;
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
        return wallPat(u, y, v, w);
    }

    /** les murs : pilastres, bandeaux, soubassement en moellon, panneaux de briques et runes, tous de la meme teinte et de textures differentes */
    private static int wallPat(int u, int y, int v, int w) {
        int b = Math.floorMod(y, 24), m = Math.floorMod(w, 12);
        double q = r01(u, y, v);
        if (b == 0) return CSTB;
        if (b == 12) return Math.abs(u) < 100 && (w & 3) == 0 ? CUCUT : PANDES;
        if (m <= 1) return b == 6 || b == 18 ? CSTB : MoriaPlan.DPILLAR;
        if ((m == 6 || m == 7) && b == 6) return MoriaPlan.DRUNES;
        if (b < 6) return q < 0.7 ? MSTB : STB;
        if (b < 12) return q < 0.8 ? PANDES : STB;
        return q < 0.58 ? STB : q < 0.78 ? TUFFB : q < 0.92 ? PANDES : MoriaPlan.CRDEEP;
    }

    private static int floorPat(int u, int y, int v) {
        int au = Math.abs(u);
        int rk = sideRoom(u, y + 1, v);
        if (rk != 0) {
            if (rk == 1 && au >= 56 && au <= 60 && Math.abs(v - 68) <= 2) return Math.abs(v - 68) == 2 || au == 56 || au == 60 ? STB : MoriaPlan.LAVA;
            if (rk == 1) return r01(u, y, v) < 0.5 ? POLBK : MoriaPlan.BLACKSTONE;
            return ((u >> 1) + (v >> 1) & 1) == 0 ? PANDES : (r01(u, y, v) < 0.4 ? MSTB : SMST);
        }
        if (y == FL0 + 13 && u < 0 && au >= 91 && au <= 103) return PANDES;                  // le sol des chambres
        if (y >= FL0 + 13 && y <= FL0 + 14 && au >= 91 && au <= 103) return r01(u, y, v) < 0.5 ? PANDES : STB;
        if (au <= 36 && v >= -126 && v <= 127 && (y == FL0 - 1 || y == FL1 - 1)) {            // le grand axe : tapis sombre bordure d'or
            if (au <= 3) return (v & 7) == 0 ? CSTB : POLBK;
            if (au == 4) return CSTB;
            if (au <= 36 && (Math.abs(au - 12) <= 0 || au == 28)) return CSTB;
            return ((u >> 2) + (v >> 2) & 1) == 0 ? PANDES : SMST;
        }
        if (y == FL0 - 1 && au >= 100 && au <= 146 && v >= 30 && v <= 120) {
            if (u < 0) {                                                                       // les forges : sol noir et un canal de lave
                if ((au == 137 || au == 138) && v >= 40 && v <= 110) return Math.floorMod(v, 24) < 4 ? STB : MoriaPlan.LAVA;
                if (au == 136 || au == 139) return v >= 40 && v <= 110 ? CSTB : MoriaPlan.BLACKSTONE;
                double q = r01(u, y, v);
                return q < 0.4 ? POLBK : q < 0.75 ? MoriaPlan.BLACKSTONE : MoriaPlan.DTILES;
            }
            return Math.abs(u - 122) <= 8 ? (((u >> 1) + (v >> 1) & 1) == 0 ? PANDES : SMST) : SMST;
        }
        if ((v & 15) == 0 || (u & 15) == 0) return CSTB;
        return ((u >> 1) + (v >> 1) & 1) == 0 ? PANDES : (r01(u, y, v) < 0.3 ? POLBK : SMST);
    }

    private static int ceilPat(int u, int y, int v) {
        boolean rib = (u & 7) == 0 || (v & 7) == 0;
        if (rib) return (u & 7) == 0 && (v & 7) == 0 ? CSTB : STB;
        return r01(u, y, v) < 0.5 ? PANDES : TUFFB;
    }

    private static int mapStatue(int id) { return id; }

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
        if (y >= 91 && y <= FL0 + 7) {
            int fa = flightA(u, v);
            if (fa != Integer.MIN_VALUE && y >= fa && y <= fa + 7) {
                if (y == fa) return STS + 2;
                if (y == fa + 7 && Math.floorMod(v, 10) == 0 && u == -122) return SLANT;
                return AIR;
            }
        }
        if (au >= 37 && au <= 64 && (r = sideRoomDecor(u, y, v)) != KEEP) return r;
        if (au >= 37 && au <= 38 && niche(y, v)) return nicheDecor(u, y, v);
        if (y >= FL0) {
            if (wingAir(u, y, v)) return u < 0 ? forge(u, y, v) : quarters(u, y, v);
            if (chamber(u, y, v)) return chamberDecor(u, y, v);
            if (storeAir(u, y, v)) return store(u, y, v);
            if (y >= FL1 && y <= FL1 + 8 && v <= -150 && u >= -48 && u <= -12) return vault(u, y, v);
            return AIR;
        }
        if (y <= 100) return mine(u, y, v);
        return AIR;
    }

    private static boolean chimney(int u, int y, int v, int x, int z, int h) {
        if (u < -145 || u > -143) return false;
        for (int vs : new int[]{56, 68, 80, 92}) {
            if (Math.abs(v - vs) <= 1) {
                int t = Math.max(top(x, z), h);
                return y <= t + 1;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ les salles laterales, les niches
    private static int nicheDecor(int u, int y, int v) {
        boolean hall = v >= 28;
        int yy = y - (hall ? FL0 : FL1), au = Math.abs(u), m = Math.floorMod(v, 12);
        double t = (m - 5.5) / 4.0;
        int top = 18 + (int) (9 * Math.sqrt(Math.max(0, 1 - t * t)));
        if (m == 5 || m == 6) {
            if (au == 37 && yy == 0 && (Math.floorDiv(v, 12) & 1) == 0) return MoriaPlan.CAMPFIRE;
            if (au == 37 && yy == top) return SLANT;
        }
        return KEEP;
    }

    private static int sideRoomDecor(int u, int y, int v) {
        int k = sideRoom(u, y, v);
        if (k == 0) return KEEP;
        int a = Math.abs(u), vc = k <= 2 ? 68 : k <= 4 ? 40 : -96, half = k == 3 || k == 4 ? 5 : 7;
        int yy = y - (k <= 4 ? FL0 : FL1), h = roomH(v, vc, half), dv = v - vc;
        // lustres : une chaine et une lanterne
        if (dv == 0 && a % 8 == 4 && a >= 44 && a <= 60) { if (yy == h - 4) return SLANT; if (yy > h - 4) return MoriaPlan.CHAIN; }
        boolean w = u < 0;
        switch (k) {
            case 1 -> {                                                         // la forge de la salle
                if (yy <= 1 && (v == 62 || v == 74) && a >= 40 && a <= 52 && a % 4 == 0) return yy == 0 ? MoriaPlan.DFURN + (v == 62 ? 2 : 0) : MoriaPlan.BARREL;
                if (yy == 0 && ((a == 44 && v == 66) || (a == 48 && v == 70) || (a == 52 && v == 66) || (a == 56 && v == 70))) return MoriaPlan.ANVIL;
                if (yy <= 1 && a >= 62 && (v <= 63 || v >= 73)) return MoriaPlan.BARREL;
                if (yy == 0 && a == 40 && v == 68) return MoriaPlan.CAULDRON;
            }
            case 2 -> {                                                         // la taverne : deux longues tables et des bancs
                if ((v == 65 || v == 71) && a >= 42 && a <= 58) { if (yy == 1) return MoriaPlan.OAKP; if (yy == 0 && a % 4 == 2) return MoriaPlan.FENCE; }
                if (yy == 0 && a >= 42 && a <= 58 && a % 2 == 0) {
                    if (v == 63 || v == 69) return MoriaPlan.BSTAIR;
                    if (v == 67 || v == 73) return MoriaPlan.BSTAIR + 2;
                }
                if (yy <= 1 && a >= 62 && (v <= 63 || v >= 73)) return MoriaPlan.BARREL;
                if (yy == 0 && a == 63 && v == 68) return MoriaPlan.CAMPFIRE;
            }
            case 3 -> {                                                         // l'atelier des bijoutiers
                if (yy == 0 && (v == 36 || v == 44) && a >= 40 && a <= 60) { int c = a % 8; if (c == 0) return MoriaPlan.DCRAFT; if (c == 4) return MoriaPlan.DCHEST + (v == 36 ? 2 : 0); }
                if (yy == 0 && v == 40 && a >= 46 && a <= 58 && a % 4 == 2) return MoriaPlan.ANVIL;
                if (yy <= 1 && a >= 62 && Math.abs(dv) <= 2) return yy == 0 ? gold : MoriaPlan.RAWGOLD;
            }
            case 4 -> {                                                         // la brasserie : des futs
                if (yy <= 1 && (v == 36 || v == 44) && a >= 40 && a <= 62 && a % 6 != 0) return MoriaPlan.BARREL;
                if (yy == 0 && v == 40 && a >= 44 && a <= 60 && a % 8 == 4) return MoriaPlan.CAULDRON;
                if (yy <= 1 && a >= 62) return MoriaPlan.BARREL;
            }
            case 5 -> {                                                         // la bibliotheque
                if (a >= 40 && a <= 60 && a % 4 == 0 && yy <= 4 && Math.abs(dv) >= 2 && Math.abs(dv) <= 7) return MoriaPlan.BOOKS;
                if (yy <= 4 && a >= 62) return MoriaPlan.BOOKS;
                if (yy == 0 && a == 50 && Math.abs(dv) <= 1) return MoriaPlan.CRAFT;
            }
            case 6 -> {                                                         // la salle des gardes : lits en rang, coffres
                if ((v == -102 || v == -90) && yy == 0 && a >= 40 && a <= 57) { int c = a % 4; if (c == 0) return MoriaPlan.DBEDF + 1; if (c == 1) return MoriaPlan.DBEDH + 1; }
                if (yy == 0 && a == 62 && (v == -101 || v == -91)) return MoriaPlan.DCHEST + 3;
                if (yy == 0 && a == 48 && v == -96) return MoriaPlan.OAKP;
                if (yy == 1 && a == 48 && v == -96) return MoriaPlan.BARREL;
                if (yy == 0 && a == 56 && Math.abs(dv) == 1) return MoriaPlan.ANVIL;
            }
            default -> { }
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ la salle des forges (ouest)
    private static int forge(int u, int y, int v) {
        int a = -u, yy = y - FL0, H = wingH(a);
        int ts = balStair(u, v);
        if (ts != Integer.MIN_VALUE && y <= ts) return y == ts ? STS + 2 : y < ts ? STB : KEEP;
        // les piliers de la nef, leurs nervures et les lustres
        for (int vp = 48; vp <= 108; vp += 12) {
            for (int ap : new int[]{114, 132}) {
                if (Math.abs(a - ap) <= 1 && Math.abs(v - vp) <= 1) {
                    if (yy <= 1) return STB;
                    if (yy >= H - 4) return CSTB;
                    return yy % 8 == 7 ? CSTB : MoriaPlan.DPILLAR;
                }
            }
            if (v == vp && yy >= H - 3 && a >= 100 && a <= 146) return yy == H - 3 ? CSTB : STB;
            if (v == vp + 6 && a == 123 && yy >= H - 9) return yy == H - 9 ? SLANT : MoriaPlan.CHAIN;
        }
        // le balcon des chambres : un plancher a mi-hauteur, une rambarde, des piliers
        if (a >= 103 && a <= 110 && v >= 64 && v <= 108) {
            if (yy == 14) return r01(u, y, v) < 0.5 ? PANDES : MSTB;
            if (yy == 15 || yy == 16) { if (a == 110) return IRONBARS; }
            if (yy < 14 && a >= 109 && (v == 74 || v == 75 || v == 86 || v == 87 || v == 98 || v == 99 || v == 64 || v == 108)) return yy <= 1 || yy >= 12 ? CSTB : MoriaPlan.DPILLAR;
            if (yy <= 1 && a <= 105 && v % 3 == 0) { int k = hsh(v, 3, 5) % 6; return k == 0 || k == 1 ? MoriaPlan.BARREL : k == 2 ? MoriaPlan.DCHEST + 1 : k == 3 ? MoriaPlan.CRAFT : k == 4 ? MoriaPlan.ANVIL : KEEP; }
            if (yy == 13 && a == 106 && v % 12 == 6) return SLANT;
        }
        // les postes de forge contre la paroi exterieure
        for (int vs : new int[]{56, 68, 80, 92}) {
            if (Math.abs(v - vs) <= 1 && a >= 143 && a <= 144 && yy <= 1) return yy == 0 ? MoriaPlan.DFURN + 1 : STB;
            if (a == 144 && v == vs && yy >= 2 && yy < H) return IRONBARS;                     // le tuyau
            if (v == vs && a == 141 && yy == 0) return MoriaPlan.ANVIL;
            if (a == 141 && (v == vs - 2 || v == vs + 2) && yy <= 1) return MoriaPlan.BARREL;
            if (a == 142 && v == vs + 3 && yy == 0) return MoriaPlan.CRAFT;
            if (a == 140 && v == vs - 1 && yy == 0) return MoriaPlan.CAULDRON;
            if (a == 140 && v == vs + 1 && yy <= 1 && a == 140) return MoriaPlan.RAWGOLD;
        }
        // bras de lave : ponts et lampes
        if (a == 139 && yy == 0 && v % 12 == 4 && v >= 40 && v <= 110) return CAMP();
        return KEEP;
    }

    private static int CAMP() { return MoriaPlan.CAMPFIRE; }

    private static int chamberDecor(int u, int y, int v) {
        int a = -u, yy = y - FL0, bv = 0;
        for (int vc : CH_V) if (Math.abs(v - vc) <= 4) bv = vc;
        int dv = v - bv;
        if (yy == 15) {
            if (a == 93 && dv == -2) return MoriaPlan.DBEDF + 3;
            if (a == 92 && dv == -2) return MoriaPlan.DBEDH + 3;
            if (a == 92 && dv == 3) return MoriaPlan.DCHEST + 1;
            if (a == 92 && dv == 1) return MoriaPlan.CRAFT;
        }
        if (a == 97 && dv == 0 && yy == chamberTop(0) - 1) return SLANT;
        return KEEP;
    }

    // ------------------------------------------------------------------ les quartiers (est) : rue, maisons, terrasses et cabanes
    private static int quarters(int u, int y, int v) {
        int yy = y - FL0, d = Math.abs(u - 122), H = wingH(Math.abs(u));
        int row = Math.floorDiv(v - 34, 15), vr = Math.floorMod(v - 34, 15);
        boolean outer = u > 122;
        // la fontaine de la place nord
        if (v >= 36 && v <= 44 && d <= 4) {
            int c = Math.max(d, Math.abs(v - 40));
            if (c == 4) return yy <= 1 ? CSTB : KEEP;
            if (c <= 3) { if (yy == 0) return MoriaPlan.WATER; if (c == 0 && yy <= 7) return yy == 7 ? MoriaPlan.CAMPFIRE : MoriaPlan.DPILLAR; }
        }
        // le grand brasero de la place sud
        if (v >= 111 && v <= 113 && d <= 1) { if (yy <= 1) return STB; if (yy == 2 && d == 0 && v == 112) return MoriaPlan.CAMPFIRE; }
        // les marches des allees et les terrasses
        int ta = alleyT(u, v);
        if (ta != Integer.MIN_VALUE) return y == ta ? STS + (outer ? 1 : 3) : y < ta ? STB : KEEP;
        if ((v >= 60 && v <= 63 || v >= 90 && v <= 93) && d >= 16 && d <= 20 && yy <= 7) return yy == 7 ? PANDES : STB;
        if (d <= 8) {                                                                            // la rue
            if (d == 7 && vr == 8 && v < 118 && row >= 0) { if (yy <= 2) return yy == 0 ? CSTB : STB; if (yy == 3) return MoriaPlan.CAMPFIRE; }
            if (u == 122 && Math.floorMod(v, 10) == 5 && yy >= H - 6 && yy < H) return yy == H - 6 ? SLANT : MoriaPlan.CHAIN;
            if (d == 5 && (row == 1 || row == 3) && vr >= 3 && vr <= 7) {                          // un etal de marche
                if (yy == 0) return (vr == 3 || vr == 7) ? MoriaPlan.FENCE : MoriaPlan.OAKP;
                if (yy == 1) return (vr == 3 || vr == 7) ? MoriaPlan.FENCE : vr == 5 ? MoriaPlan.BARREL : KEEP;
                if (yy == 2 && (vr == 3 || vr == 7)) return MoriaPlan.FENCE;
                if (yy == 3) return MoriaPlan.OAKP;
            }
            return KEEP;
        }
        if (row < 1 || row > 4 || vr > 10 || d > 20) return KEEP;
        if (yy == 6) return STB;                                                                 // le toit : une terrasse
        if (yy == 7) return r01(u, y, v) < 0.5 ? PANDES : MSTB;
        if (yy > 7) {
            if (d == 9 && yy == 8) return IRONBARS;                                              // la rambarde cote rue
            // la cabane sur le toit
            if (d >= 12 && d <= 19 && vr >= 2 && vr <= 8 && yy <= 12) {
                if (yy == 12) return STB;
                boolean wall = d == 12 || d == 19 || vr == 2 || vr == 8;
                if (wall) {
                    if (d == 12 && vr == 5 && yy <= 9) return AIR;
                    if (yy >= 9 && yy <= 10 && ((d == 19 && vr >= 4 && vr <= 6) || ((vr == 2 || vr == 8) && d >= 15 && d <= 16))) return IRONBARS;
                    return yy == 11 ? CSTB : stone(u, y, v);
                }
                if (yy == 8) {
                    if (d == 17 && vr == 3) return MoriaPlan.DBEDF + (outer ? 1 : 3);
                    if (d == 18 && vr == 3) return MoriaPlan.DBEDH + (outer ? 1 : 3);
                    if (d == 18 && vr == 7) return MoriaPlan.DCHEST + (outer ? 3 : 1);
                    if (d == 15 && vr == 7) return MoriaPlan.CRAFT;
                }
                if (yy == 11 && d == 15 && vr == 5) return SLANT;
            }
            return KEEP;
        }
        boolean wall = d == 9 || d == 20 || vr == 0 || vr == 10;
        if (wall) {
            if (d == 9 && vr >= 4 && vr <= 6 && yy <= 3) return AIR;                            // la porte
            if (d == 9 && (vr == 2 || vr == 8) && yy >= 2 && yy <= 3) return IRONBARS;          // les fenetres
            if (yy == 5) return CSTB;
            return stone(u, y, v);
        }
        int dirChest = outer ? 3 : 1;
        if (d == 18 && vr == 2 && yy == 0) return MoriaPlan.DBEDF + (outer ? 1 : 3);
        if (d == 19 && vr == 2 && yy == 0) return MoriaPlan.DBEDH + (outer ? 1 : 3);
        if (d == 19 && vr == 8 && yy == 0) return MoriaPlan.DCHEST + dirChest;
        if (d == 16 && vr == 5 && yy == 0) return MoriaPlan.CRAFT;
        if (d == 19 && vr == 6 && yy == 0) return MoriaPlan.DFURN + dirChest;
        if (d == 14 && vr == 5 && yy == 5) return SLANT;
        return KEEP;
    }

    private static int store(int u, int y, int v) {
        int au = Math.abs(u), yy = y - FL0;
        if (au >= 118 && au <= 130) return KEEP;                                                 // l'allee centrale
        if (Math.floorMod(au, 6) <= 2 && Math.floorMod(v, 6) <= 2 && v >= 126) {
            int hgt = 1 + (hsh(au / 6, 7, v / 6) & 3);
            if (yy < hgt) {
                int k = hsh(au / 6, 9, v / 6) % 6;
                return k <= 1 ? MoriaPlan.BARREL : k == 2 ? MoriaPlan.IRONB : k == 3 ? COALB : k == 4 ? RAWIRON : MoriaPlan.COPPER;
            }
        }
        int H = storeH(au);
        if (Math.floorMod(au, 12) == 6 && Math.floorMod(v, 12) == 6 && yy >= H - 5) return yy == H - 5 ? SLANT : MoriaPlan.CHAIN;
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
                if (yy >= 58) return yy == 62 ? CSTB : yy >= 63 ? CSTB : (yy % 2 == 0 ? CSTB : STB);
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
            if (v >= -118 && v <= -112 && au >= 4 && au <= 5 && dy >= 0 && dy <= 2) return dy == 2 ? CSTB : PANDES;          // les accoudoirs
            if (v >= -123 && v <= -119 && dy >= 0 && dy <= 30) {                      // le dossier
                int hw = dy <= 12 ? 7 : 7 - (dy - 12) / 3;
                if (au <= hw) {
                    if (au == hw) return dy % 4 == 0 ? CSTB : PANDES;
                    if (dy >= 14 && dy <= 26 && au <= 3 && v == -119) return (au + dy) % 3 == 0 ? CSTB : MoriaPlan.DRUNES;
                    return STB;
                }
            }
        }
        if (v == -126 && au <= 22 && yy >= 2 && yy <= 56) {                         // le fond de la salle du trone, sobre, dans le style de la Moria
            int top = 50 - (au > 14 ? (au - 14) * 3 / 2 : 0);
            if (yy > top) return KEEP;
            if (au % 11 <= 1) return au > 18 && yy < 6 ? KEEP : MoriaPlan.DPILLAR;                    // de grands pilastres
            if (yy == top) return CSTB;
            if (yy % 12 == 6 && au % 11 >= 5 && au % 11 <= 6) return MoriaPlan.DRUNES;
            return yy < 8 ? MSTB : (yy < 30 ? STB : PANDES);
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
            default: return q < 0.52 ? STB : q < 0.72 ? TUFFB : q < 0.84 ? MSTB : q < 0.93 ? PANDES : MoriaPlan.CRDEEP;
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
            if (dd == 18) return CSTB;
            if (dd <= 17 && dd >= 10) return dd % 3 == 0 ? CUCUT : PANDES;
            if (dd <= 9) return dd % 4 == 0 ? CSTB : TUFFB;
            if ((au + yy) % 8 == 0 || Math.floorMod(au - yy, 8) == 0) return CSTB;
            return r01(u, yy, 3) < 0.5 ? STB : TUFFB;
        }
        if (yy % 24 == 23 && kind == 0) return CSTB;
        if (kind == 1 && yy < 52 && au - hw == 4 && p <= 0) return CSTB;                  // filet de pierre taillee autour du portail
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
