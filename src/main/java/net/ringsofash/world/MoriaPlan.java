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
            YELLOWWOOL = 71, CAULDRON = 72, COPPER = 73, SHROOM = 74, IRONB = 75, SMBAS = 76, CRACKB = 77, DTILES = 78, CRDEEP = 79,
            DCHEST = 80 /* 80..83 : N E S O, face avant vers cette direction */, DBEDF = 84 /* 84..87 : direction du pied vers la tete */, DBEDH = 88 /* 88..91 */,
            DFURN = 92 /* 92..95 */, DCRAFT = 96,
            DBEDFL = 97 /* 97..100 : lit accole a gauche (vu depuis le pied) */, DBEDFR = 101, DBEDHL = 105, DBEDHR = 109, BLUEW = 113, POLDEEP = 114, COBDEEP = 115, DEEPBR = 116, CHISDEEP = 117, POLBLK = 118;
    public static final int NIDS = 119;

    public static final int F0 = 80;       // sol de la cour et de la Grande Salle
    public static final int F1 = 17;       // sol du gouffre (le pont) et de son palier
    public static final int F2 = -20;      // sol des Forges et des galeries
    public static final int LW = 72;       // niveau du lac
    public static final int CT_X1 = 262;           // fin de la grande cour dallee
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

    /** le lac devant les Portes a ete supprime : la cour s'ouvre sur la pente de la montagne */
    public static boolean inLake(int x, int z) { return false; }

    /** niveau de l'eau du lac a cet endroit, ou MIN_VALUE */
    public static int lakeTop(int x, int z, int h) { return Integer.MIN_VALUE; }

    /** altitude jusqu'a laquelle la facade des Portes construit au dessus du sol (MIN_VALUE hors de la cour) */
    public static int aboveTop(int x, int z) {
        return x >= GX && x <= CT_X1 + 2 && Math.abs(z - GZ) <= 50 ? F0 + 72 : Integer.MIN_VALUE;
    }

    /** la cour taillee devant les Portes : sol de montagne sans neige */
    public static boolean inCourt(int x, int z) { return x >= GX - 2 && x <= CT_X1 + 14 && Math.abs(z - GZ) <= 62; }

    /** relief : la falaise taillee devant les Portes (grande cour plate a F0) */
    public static int surface(int x, int z, int g) {
        if (x < GX || x > CT_X1 + 18 || z < GZ - 64 || z > GZ + 64) return g;
        double u = Math.abs(z - GZ);
        double w = (1 - sm((u - 48) / 12.0)) * (1 - sm((x - CT_X1) / 16.0));
        return w > 0 ? (int) Math.round(g + (Math.min(g, F0) - g) * w) : g;
    }

    // ------------------------------------------------------------------ geometrie commune
    /** dallage : grands carreaux de 8 bordes de pierre taillee, chacun avec son motif (damier, rangees, fissures, diagonales, anneaux, rune), usure irreguliere */
    static int floorPat(int x, int z) {
        int tx = Math.floorDiv(x, 8), tz = Math.floorDiv(z, 8), lx = Math.floorMod(x, 8), lz = Math.floorMod(z, 8);
        int h = hsh(tx, tz, 5);
        double r = r01(x, 3, z);
        if (lx == 0 || lz == 0) return r < 0.12 ? DCOBBLE : DCHISEL;
        if (r < 0.04) return DCOBBLE;
        if (r < 0.07) return CRACKB;
        switch (h % 6) {
            case 0: return (((lx >> 1) + (lz >> 1)) & 1) == 0 ? DBRICKS : DSTONE;
            case 1: return (lx == 3 || lx == 4) && (lz == 3 || lz == 4) ? DRUNES : DTILES;
            case 2: return (lz & 1) == 0 ? DBRICKS : (((lx + lz) & 3) == 0 ? DTILES : DSTONE);
            case 3: return r < 0.4 ? CRACKB : r < 0.55 ? DCOBBLE : DBRICKS;
            case 4: return ((lx + lz) & 3) < 2 ? DSTONE : DTILES;
            default: {
                int d = Math.max(Math.abs(2 * lx - 7), Math.abs(2 * lz - 7)) / 2;
                return d == 0 ? DRUNES : (d & 1) == 0 ? DBRICKS : CRDEEP;
            }
        }
    }

    /** petit relief du sol (un bloc au dessus du dallage) : estrades, bornes, bosses */
    static int floorRel(int x, int z) {
        int tx = Math.floorDiv(x, 8), tz = Math.floorDiv(z, 8), lx = Math.floorMod(x, 8), lz = Math.floorMod(z, 8);
        int h = hsh(tx, tz, 9);
        if (h % 9 == 0 && lx >= 2 && lx <= 5 && lz >= 2 && lz <= 5) return BSLAB;
        if ((tx & 3) == 0 && (tz & 3) == 0 && lx == 0 && lz == 0) return BWALL;
        if (r01(x, 17, z) < 0.012) return BSLAB;
        return KEEP;
    }

    private static final int GW = 7, GH = 14, GT = 32;              // porte : demi-largeur, hauteur des montants, hauteur du sommet de l'ogive (la plus profonde)

    /** demi-largeur de l'ogive a la hauteur v ; ex = nombre de gradins vers l'exterieur (0 = la porte, 3 = l'entree evasee de la facade) */
    private static double wcd(int v, int ex) {
        double w = GW + 3 * ex, hh = GH + 3 * ex, t = GT + 5 * ex;
        if (v <= hh) return w;
        double q = Math.min(1, (v - hh) / (t - hh));
        return w * (1 - Math.pow(q, 1.5));
    }

    private static int halfW(int v, int ex) {                       // idem, entier (-1 hors de l'ogive)
        if (v < 1 || v >= GT + 5 * ex) return -1;
        return (int) Math.floor(wcd(v, ex));
    }

    private static double dOutE(int au, int v, int ex) {            // distance a l'ogive (negatif dedans)
        double t = GT + 5 * ex;
        return v <= t ? au - wcd(v, ex) : Math.hypot(au, v - t);
    }

    // ------------------------------------------------------------------ les Portes : un portail evase a quatre gradins, puis les vantaux et le couloir d'entree
    /** les blocs nains sont teintes (vert/bleu) selon l'endroit : aux Portes on prend de la pierre sombre sans teinte, grise et noire */
    private static int neutral(int id) {
        switch (id) {
            case DSTONE: return POLDEEP;
            case DCOBBLE: return COBDEEP;
            case DBRICKS: return DEEPBR;
            case DPILLAR: return POLBLK;
            case DCHISEL: return CHISDEEP;
            case DLAMP: return POLBLK;
            default: return id;
        }
    }

    private static int gate(int x, int y, int z, int h) {
        if (x >= GX) return court(x, y, z, h);
        if (x < 139 || z < GZ - 50 || z > GZ + 50 || y < F0 || y > F0 + 72) return KEEP;
        int u = z - GZ, v = y - F0, au = Math.abs(u);
        int dd = GX - 1 - x;                                         // 0 = la facade, 3 = l'ogive de la porte
        int ex = Math.max(0, 3 - dd);
        int w = halfW(v, ex);
        boolean inside = v >= 1 && au <= w;
        if (v == 0 && au <= GW + 3 * ex + 1) return (x % 8 == 0 && au <= 1) ? DRUNES : floorPat(x, z);
        // la niche en ogive creusee derriere chaque statue
        int nd = Math.abs(au - ST_AU), nh = nicheHalf(v);
        if (nh >= 0 && nd <= nh) {
            if (dd <= 2) return AIR;
            if (dd == 3) {
                if (nd == 5 && (v == 10 || v == 40)) return DLAMP;
                if (nd % 2 == 0 && v % 4 == 0 && nd <= nh - 1) return DRUNES;
                double q = r01(au, v, 37);
                return q < 0.45 ? DEEPSLATE : q < 0.8 ? DSTONE : BLACKSTONE;
            }
            return KEEP;
        }
        if (dd == 0 && v >= 2 && v <= 52 && ((nh >= 0 && nd == nh + 1) || (v >= 49 && nd <= 1 && nh < 0))) return DCHISEL;
        if (inside) {
            if (x == GX - 6 || x == GX - 5) {                        // les vantaux de pierre, fermes, avec une entrebaillure au centre
                boolean gap = (au <= 2 && v <= 19) || (au <= 1 && v <= 22) || (au == 0 && v <= 25);
                if (gap) return AIR;
                if (x == GX - 6) return DSTONE;
                double in = wcd(v, 0) - au;
                if (v >= 3 && in >= 1.0 && in < 2.0 && (v + au) % 2 == 0) return DRUNES;                  // arc d'ithildin le long de l'arche
                if (au >= 3 && (v == 5 || v == 11 || v == 17)) return BLACKSTONE;                         // les bandes de pierre noire des vantaux
                if (au >= 3 && v >= 4 && v <= 16 && (v == 8 || v == 14)) return (au + v) % 3 == 0 ? DSTONE : DRUNES;   // lignes d'inscription
                if ((au == 0 && v >= 26 && v <= 31) || (v == 28 && au <= 2)) return v == 28 && au == 0 ? DCHISEL : DRUNES;   // l'etoile de Durin
                return (au == 4 && v >= 3 && v <= 16) ? DCHISEL : DBRICKS;
            }
            if (dd == 3 && (au == 3 || au == 6) && (v == 8 || v == 14)) return DCHISEL;                 // les bossages qui saillent des vantaux
            if (dd <= 2 && v >= 3 && dOutE(au, v, ex) > -1.5 && (au + v) % 3 == 0) return (au + v) % 18 == 0 ? DLAMP : DCHISEL;   // les claveaux en dents, avec leurs lampes
            return AIR;
        }
        double dOut = dOutE(au, v, ex);
        if (dd <= 3) {                                               // les gradins du portail evase
            if (dd == 0) {
                if (dOut > 7) return facade(x, y, z, h, au, v);
                if (dOut <= 1.5) return DCHISEL;
                if (dOut <= 3.5) return (au + v) % 2 == 0 ? DEEPSLATE : BLACKSTONE;
                if (dOut <= 5.5 && v >= 6) return (v + au) % 3 == 0 ? DCHISEL : DEEPSLATE;                // la guirlande taillee autour du portail
                return r01(au, v, 41) < 0.6 ? DEEPSLATE : DBRICKS;
            }
            if (dOut <= 1) return (au + v) % 4 == 0 ? DRUNES : DCHISEL;                                 // le bandeau de l'arc
            boolean vis = v >= 1 && au <= halfW(v, ex + 1);                                             // face du gradin, visible depuis l'exterieur
            if (!vis) return KEEP;
            double q = r01(au, v, 31 + dd);
            int k = (int) Math.floor(dOut);
            if (k == 1) return (au + v) % 2 == 0 ? DCHISEL : DBRICKS;
            if (k == 2) return (au + v) % 11 == 0 ? DRUNES : q < 0.6 ? DEEPSLATE : DSTONE;
            if (k == 3) return (au + v) % 8 == 0 ? DCHISEL : DEEPSLATE;
            return q < 0.5 ? DBRICKS : q < 0.75 ? DSTONE : q < 0.9 ? DEEPSLATE : BLACKSTONE;
        }
        if (dOut <= 2) {                                             // parois du couloir
            if (dOut <= 1 && Math.floorMod(x, 9) == 0) return DPILLAR;
            return DBRICKS;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ la facade des Portes (inspiree d'Erebor) et la grande cour
    private static final int ST_AU = 27;        // axe des deux statues de nains geants (de part et d'autre de la porte)
    private static final int WF_AU = 43;        // axe des cascades

    /** demi-largeur de la niche des statues a la hauteur v (-1 hors de la niche) : droite puis en ogive */
    private static int nicheHalf(int v) {
        if (v < 3 || v > 51) return -1;
        if (v <= 40) return 7;
        return (int) Math.floor(7 * (1 - Math.pow((v - 40) / 12.0, 1.5)));
    }

    /** la falaise taillee au dessus et autour de l'ogive : nervures, bandeaux, panneaux, grand losange d'ithildin, cascades. x in GX-3..GX-1 */
    private static int facade(int x, int y, int z, int h, int au, int v) {
        if (au > 47 || v < 1 || v > 70 || y > h - 2) return KEEP;
        if (au >= WF_AU - 2 && au <= WF_AU + 2) {                                    // la cascade : un chenal taille dans la falaise
            if (au == WF_AU - 2 || au == WF_AU + 2) return x == GX - 1 && v <= 62 ? DCHISEL : KEEP;
            return y <= h - 3 ? WATER : KEEP;
        }
        if (x != GX - 1 || v > 58) return KEEP;
        if (au == 17 || au == 38) return DPILLAR;                                     // nervures (elles avancent dans la cour)
        if (v == 58) return DCHISEL;
        if (v % 14 == 13) return DCHISEL;
        int em = Math.abs(v - 53);
        if (au + em <= 4 && v >= 49) return au + em == 4 ? DCHISEL : au + em == 0 ? DRUNES : BLACKSTONE;            // l'embleme taille au dessus du portail
        if ((au + v) % 8 == 0 || Math.floorMod(au - v, 8) == 0) return DCHISEL;                                      // treillis en losanges
        double q = r01(au, v, 29);
        return q < 0.55 ? DEEPSLATE : q < 0.8 ? DBRICKS : q < 0.92 ? DSTONE : BLACKSTONE;
    }

    /** nain geant debout, de face vers +x, appuye sur une immense hache : l = decalage lateral, dx = distance a la falaise, v = hauteur (54 de haut) */
    static int dwarf(int l, int dx, int v) {
        int al = Math.abs(l);
        if (v < 0 || v > 54 || dx < 0 || dx > 11 || al > 9) return KEEP;
        if (v <= 4) {                                                                              // le socle a gradins
            int in = v / 2;
            if (dx < in || dx > 11 - in || al > 9 - in) return KEEP;
            return v == 4 ? DCHISEL : r01(l, v, dx) < 0.3 ? DEEPSLATE : DBRICKS;
        }
        if (dx >= 10 && dx <= 11) {                                                                // la hache : manche et lame
            if (al <= 1 && v <= 51) return v % 6 == 3 ? DCHISEL : DPILLAR;
            if (v >= 40 && v <= 52) {
                double hw = 7 - Math.abs(v - 46) * 1.1;
                if (al >= 2 && al <= hw) return al >= hw - 1 ? DCHISEL : BLACKSTONE;
            }
        }
        int la = Math.abs(al - 4);
        if (v <= 17) {                                                                             // les jambes (5 de large) et les bottes
            if (la > 2 || al < 1) return KEEP;
            if (v <= 7) return dx >= 3 && dx <= 10 && (dx <= 8 || v <= 6) ? (v == 7 ? DCHISEL : DEEPSLATE) : KEEP;
            if (dx < 3 || dx > (v >= 12 && v <= 14 ? 9 : 8)) return KEEP;
            return v == 13 && dx == 9 ? DCHISEL : r01(l, v, dx) < 0.25 ? DCOBBLE : DSTONE;
        }
        if (v <= 20) {                                                                             // la jupe de mailles
            if (dx < 2 || dx > 9 || al > 7) return KEEP;
            if (v == 18 && dx >= 8) return al % 2 == 0 ? DCHISEL : KEEP;
            return (al + v) % 2 == 0 ? DCOBBLE : DSTONE;
        }
        if (v <= 22) {                                                                             // la ceinture et sa boucle
            if (dx >= 2 && dx <= 9 && al <= 6) return DEEPSLATE;
            if (dx == 10 && al <= 2) return DCHISEL;
            return KEEP;
        }
        if (v >= 23 && v <= 30) {                                                                  // les bras (devant la barbe)
            if (dx >= 3 && dx <= 7 && al >= 8 && al <= 9 && v >= 26) return DSTONE;
            if (dx >= 5 && dx <= 10 && v >= 23 && v <= 26) {
                double alc = 8 - (dx - 5) * 1.0;
                if (al >= alc - 1 && al <= alc + 1) return DSTONE;
            }
            if (dx >= 9 && dx <= 11 && al <= 2 && v >= 22 && v <= 27) return DCHISEL;              // les poings
        }
        if (v >= 27 && v <= 32 && dx >= 3 && dx <= 7 && al >= 8 && al <= 9) return DSTONE;
        if (dx >= 9 && dx <= 10 && v >= 23 && v <= 40) {                                           // la barbe tressee
            int bw = Math.max(1, (v - 21) / 4);
            if (al <= bw) return (al + v) % 2 == 0 ? DCOBBLE : DEEPSLATE;
        }
        if (v >= 23 && v <= 34) {                                                                  // le torse
            int tw = v <= 26 ? 5 : v <= 29 ? 6 : 7;
            if (dx >= 2 && dx <= 8 && al <= tw) {
                if (dx == 8 && (v == 29 || (al == 0 && v >= 24))) return DCHISEL;
                return r01(l, v, dx) < 0.2 ? DEEPSLATE : DSTONE;
            }
            return KEEP;
        }
        if (v <= 38) {                                                                             // les epaulieres
            if (dx >= 1 && dx <= 9) return v == 38 ? DCHISEL : al >= 8 ? DEEPSLATE : DSTONE;
            return KEEP;
        }
        if (v <= 40) {
            if (al >= 7 && al <= 9 && dx >= 3 && dx <= 7) return DCHISEL;                           // les pointes des epaulieres
            if (dx >= 3 && dx <= 6 && al <= 2) return DSTONE;                                       // le cou
            return KEEP;
        }
        if (v <= 47) {                                                                             // la tete
            if (dx >= 2 && dx <= 8 && al <= 4) return dx == 8 && v == 44 && al == 2 ? BLACKSTONE : DSTONE;
            if (dx == 9 && al <= 4 && v == 45) return DCHISEL;                                      // l'arcade
            if (dx == 9 && al == 0 && v >= 42 && v <= 44) return DSTONE;                            // le nez
            if (dx == 9 && al <= 5 && v == 42) return DCOBBLE;                                      // la moustache
            return KEEP;
        }
        if (v <= 52) {                                                                             // le casque a cornes
            int cw = v <= 50 ? 5 : 3;
            if (dx >= 2 && dx <= 8 && al <= cw) return v == 48 ? DCHISEL : DEEPSLATE;
            if (v >= 48 && v <= 51 && al == 6 && dx >= 4 && dx <= 6) return DCHISEL;
            if (v >= 51 && al == 7 && dx >= 4 && dx <= 6) return DCHISEL;
            if (dx == 9 && al == 0 && v >= 46 && v <= 48) return DCHISEL;                           // le nasal
            return KEEP;
        }
        return dx >= 4 && dx <= 5 && al <= 1 ? DCHISEL : KEEP;                                      // la crete
    }

    /** la grande cour : chaussee dallee entre deux parapets a braziers, statues, bannieres, nervures, bassins des cascades */
    private static int court(int x, int y, int z, int h) {
        if (x > CT_X1 + 2 || z < GZ - 50 || z > GZ + 50 || y < F0 - 3 || y > F0 + 72) return KEEP;
        int u = z - GZ, au = Math.abs(u), v = y - F0, dx = x - GX;
        // les bassins au pied des cascades
        if (au >= WF_AU - 3 && au <= WF_AU + 3 && dx <= 8) {
            boolean rim = dx == 8 || au == WF_AU - 3 || au == WF_AU + 3;
            if (y == F0 && rim) return DCHISEL;
            if (!rim && (y == F0 || y == F0 - 1)) return WATER;
        }
        // les nervures et la corniche qui avancent devant la falaise
        int ra = Math.min(Math.abs(au - 17), Math.abs(au - 38));
        if (ra <= 1 && dx <= 2 && v >= 1 && v <= 55) {                              // les nervures : socle, fut, chapiteau d'or
            if (v <= 3) return v == 3 ? DCHISEL : DBRICKS;
            if (v >= 50) return (v == 50 || v == 52 || v == 55) ? DCHISEL : DEEPSLATE;
            if (ra == 0 && dx <= 1) return DPILLAR;
        }
        if (dx <= 2 && (v == 57 || v == 58) && au <= 47) return v == 57 ? DCHISEL : DEEPSLATE;
        if (dx <= 2 && (v == 59 || v == 60) && au <= 47 && au % 4 != 3) return v == 59 ? DBRICKS : BWALL;   // les creneaux
        if (dx == 3 && v == 56 && au % 6 == 0 && au <= 46 && au >= 12) return DCHISEL;                       // les consoles sous la corniche
        // les deux nains geants, et leurs bannieres
        int dl = Math.abs(au - ST_AU);
        if (au >= ST_AU - 9 && au <= ST_AU + 9) {
            int l = (u < 0 ? -1 : 1) * (au - ST_AU);
            int sd = dwarf(l, dx, v);
            if (sd != KEEP) return sd;
        }
        // les braziers : deux pres de la porte, puis un tous les 16 le long des parapets
        boolean poste = (dx == 5 && au == 17) || (au == 15 && dx >= 14 && (dx - 14) % 16 == 0);
        if (poste && v >= 1 && v <= 4 && y > h - 1) return v <= 2 ? DPILLAR : v == 3 ? DCHISEL : CAMPFIRE;
        // le parapet de la chaussee
        if (au == 15 && dx >= 12 && dx <= CT_X1 - GX && v == 1 && y > h - 1 && Math.floorMod(dx, 16) >= 3 && Math.floorMod(dx, 16) <= 12) return BWALL;
        // le dallage : la chaussee, et le parvis devant la falaise
        if (y == h && h >= F0 && h <= F0 + 3 && (au <= 15 || (dx <= 14 && au <= 46)) && !(au >= WF_AU - 3 && au <= WF_AU + 3 && dx <= 8)) {
            if (au == 0 && dx % 8 == 0) return DRUNES;
            return (x + z) % 6 == 0 && au <= 15 ? DCHISEL : floorPat(x, z);
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

    private static final int BZ0 = -644, BZ1 = -557, BH = 6;

    /** hauteur de la bosse du tablier au dessus de F1 (0 aux deux bouts, BH au milieu) */
    private static int bridgeDy(int z) {
        double zm = (BZ0 + BZ1) / 2.0, half = (BZ1 - BZ0) / 2.0, u = (z - zm) / half;
        return (int) Math.round(BH * (1 - u * u));
    }

    /** pont en arche : tablier de 3 de large qui monte en bosse, dessous une arche pleine qui s'epaissit vers les culees ; marches en pierre a chaque palier */
    private static int arcBridge(int x, int y, int z) {
        if (z < BZ0 || z > BZ1 || Math.abs(x - 76) > 1) return KEEP;
        double zm = (BZ0 + BZ1) / 2.0, half = (BZ1 - BZ0) / 2.0, u = (z - zm) / half;
        int dy = bridgeDy(z), top = F1 + dy;
        int yi = (int) Math.round(F1 + BH - 2 - 22 * u * u);
        if (y > top || y < yi) return KEEP;
        if (y == top) {
            boolean rising = z <= zm ? dy > bridgeDy(z - 1) : dy > bridgeDy(z + 1);
            if (rising) return DSTAIR + (z <= zm ? 2 : 0);
            return Math.abs(x - 76) == 1 ? DCHISEL : DBRICKS;
        }
        if (y == yi) return DCHISEL;
        return Math.floorMod(z, 6) == 0 && Math.abs(x - 76) == 1 ? DCHISEL : DBRICKS;
    }

    private static int chasm(int x, int y, int z, int h) {
        if (x < CX0 - 8 || x > CX1 + 8 || z < CZ0 - 3 || z > CZ1 + 3 || y < -52 || y > 170) return KEEP;
        // les parois : un trace general lisse, puis un relief qui change avec l'altitude (surplombs, replats, bosses) pour ne pas avoir de cannelures verticales
        double xl = CX0 + 7 * (2 * vn(z / 21.0, 3.1, 61) - 1) + 5 * (2 * vn(z / 7.0 + y * 0.09, y / 6.0, 64) - 1) + 2 * (2 * vn(z / 3.0, y / 3.0, 68) - 1);
        double xr = CX1 + 7 * (2 * vn(z / 19.0, 8.3, 62) - 1) + 5 * (2 * vn(z / 7.0 - y * 0.09, y / 6.0, 65) - 1) + 2 * (2 * vn(z / 3.0, y / 3.0, 69) - 1);
        double zn = CZ0 + 4 * (2 * vn(x / 7.0 + y * 0.08, y / 6.0, 66) - 1) + 1.5 * (2 * vn(x / 3.0, y / 3.0, 70) - 1);
        double zs = CZ1 + 4 * (2 * vn(x / 7.0 - y * 0.08, y / 6.0, 71) - 1) + 1.5 * (2 * vn(x / 3.0, y / 3.0, 72) - 1);
        // les deux entrees du gouffre (les grands escaliers) : la cavite doit se prolonger jusqu'a eux, sans bouchon de roche naturelle
        if (x >= 48 && x <= 102) { zn = Math.min(zn, CZ0 - 2); zs = Math.max(zs, CZ1 + 2); }
        if (x <= xl || x >= xr || z < zn || z > zs) return KEEP;
        double dEdge = Math.min(Math.min(x - xl, xr - x), Math.min(z - zn, zs - z) + 1);
        boolean ew = Math.min(x - xl, xr - x) < Math.min(z - zn, zs - z) + 1;
        int ceil = Math.min(h - 11 - (int) Math.round(3 * vn(x / 6.0, z / 6.0, 67)), 160);
        if (y > ceil) return KEEP;
        // le pont : une arche etroite sans garde-corps, comme celui de Khazad-dum
        int br = arcBridge(x, y, z);
        if (br != KEEP) return br;
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
        if ((r = neutral(gate(x, y, z, h))) != KEEP) return r;
        if ((r = stairRun(x, y, z, h, 50, 100, -552, -473, F0, F1, F0 + 50)) != KEEP) return r;
        if ((r = stairRun(x, y, z, h, 50, 100, -687, -649, F1, F2, F2 + 66)) != KEEP) return r;
        if ((r = chasm(x, y, z, h)) != KEEP) return r;
        if ((r = hall(x, y, z, h)) != KEEP) return r;
        if ((r = forge(x, y, z, h)) != KEEP) return r;
        return y > h ? KEEP : ore(x, y, z);                                  // pas de minerai dans l'air au dessus du sol
    }
}
