package net.ringsofash.world;

import static net.ringsofash.world.MoriaPlan.*;

/**
 * Les salles ajoutees aux mines de la Moria, toutes au niveau des Forges (F2) : la Grande Rue a piliers, le Quartier des nains (ravine de lave, terrasses,
 * maisons, ponts, maison du roi), la salle des festins, trois salles a piliers (armurerie, archives, crypte), la salle du trone, le tresor scelle (montagne
 * d'or), l'enorme mine a ciel ouvert avec ses galeries explorables, les fours geants des Forges, les bas-reliefs et les statues d'ornement.
 * Meme principe que MoriaPlan : fonction pure de (x, y, z, hauteur du terrain), KEEP = ne rien changer.
 */
final class MoriaRooms {
    private MoriaRooms() {}

    private static final int F = F2;

    static final int AVE = 0, VILL = 1, DINE = 2, PH = 3, THRONE = 4, TREAS = 5;

    private record Room(int x0, int x1, int z0, int z1, int hgt, int kind, int variant) {}

    private static final Room[] R = {
            new Room(-262, -120, -748, -712, 36, AVE, 0),
            new Room(-262, -120, -700, -560, 56, VILL, 0),
            new Room(-262, -120, -536, -446, 38, DINE, 0),
            new Room(-112, -42, -672, -590, 34, PH, 1),        // armurerie
            new Room(150, 236, -668, -592, 34, PH, 2),         // archives
            new Room(-112, -42, -930, -856, 34, PH, 3),        // crypte
            new Room(96, 184, -958, -838, 54, THRONE, 0),
            new Room(196, 330, -952, -872, 48, TREAS, 0),
    };

    /** passages : x0, x1, z0, z1, hauteur libre */
    private static final int[][] P = {
            {-119, -111, -737, -723, 24},     // 0 grande rue -> Forges
            {-205, -199, -711, -701, 16},     // 1 grande rue -> quartier
            {-205, -199, -559, -537, 16},     // 2 quartier -> festins
            {-205, -195, -779, -749, 22},     // 3 grande rue -> mine
            {-80, -74, -687, -673, 22},       // 4 Forges -> armurerie
            {190, 196, -687, -669, 22},       // 5 Forges -> archives
            {-80, -74, -855, -831, 22},       // 6 Forges -> crypte
            {133, 147, -837, -831, 30},       // 7 Forges -> salle du trone
            {210, 218, -868, -831, 18},       // 8 Forges -> tresor (eboule, scelle)
            {-119, -113, -634, -628, 18},     // 9 quartier -> armurerie
    };

    private static boolean inAny(int x, int z) {
        for (Room r : R) if (x >= r.x0 && x <= r.x1 && z >= r.z0 && z <= r.z1) return true;
        return false;
    }

    static int at(int x, int y, int z, int h) {
        int r;
        if ((r = passage(x, y, z)) != KEEP) return r;
        for (Room rm : R) if ((r = room(x, y, z, h, rm)) != KEEP) return r;
        if ((r = mine(x, y, z, h)) != KEEP) return r;
        if ((r = gal(x, y, z, h)) != KEEP) return r;
        if ((r = furnace(x, y, z, h)) != KEEP) return r;
        if ((r = forgeProps(x, y, z, h)) != KEEP) return r;
        if ((r = floorExtra(x, y, z)) != KEEP) return r;
        return bigRelief(x, y, z, h);
    }

    // ------------------------------------------------------------------ passages
    private static int passage(int x, int y, int z) {
        if (y < F || y > F + 33) return KEEP;
        for (int i = 0; i < P.length; i++) {
            int[] p = P[i];
            boolean alongZ = p[3] - p[2] > p[1] - p[0];
            int lx0 = alongZ ? p[0] - 1 : p[0], lx1 = alongZ ? p[1] + 1 : p[1], lz0 = alongZ ? p[2] : p[2] - 1, lz1 = alongZ ? p[3] : p[3] + 1;
            if (x < lx0 || x > lx1 || z < lz0 || z > lz1) continue;
            int hh = p[4];
            boolean inside = x >= p[0] && x <= p[1] && z >= p[2] && z <= p[3];
            if (inside) {
                if (i == 8 && z <= -847) {                                  // l'eboulement et la porte scellee
                    if (z >= -849) {
                        double d = Math.hypot(x - 214, y - (F + 9));
                        if (Math.abs(d - 3.5) < 0.75 || (x == 214 && y >= F + 3 && y <= F + 15) || (y == F + 9 && Math.abs(x - 214) <= 2)) return DRUNES;
                        if (y > F && y <= F + hh) return r01(x, y, z) < 0.12 ? DCHISEL : DBRICKS;
                    } else if (y > F && y <= F + hh) {
                        double q = r01(x, y, z);
                        return q < 0.9 ? (r01(x, 3, z) < 0.45 ? DCOBBLE : r01(x, 4, z) < 0.6 ? GRAVEL : DEEPSLATE) : AIR;
                    }
                }
                if (y == F) return floorPat(x, z);
                if (y <= F + hh) {
                    if (y == F + hh - 1 && (alongZ ? Math.floorMod(z, 10) == 0 && (x == p[0] || x == p[1]) : Math.floorMod(x, 10) == 0 && (z == p[2] || z == p[3]))) return DLAMP;
                    return AIR;
                }
                if (y == F + hh + 1) return DCHISEL;
                return KEEP;
            }
            // revetement
            if (inAny(x, z)) continue;
            if (y > F + hh + 1) continue;
            if (y == F) return DBRICKS;
            if (y == F + hh + 1) return DCHISEL;
            boolean rib = alongZ ? Math.floorMod(z, 8) == 0 : Math.floorMod(x, 8) == 0;
            return rib ? DPILLAR : DBRICKS;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ bas-reliefs geants (guerrier nain, visage sculpte)
    /** arche aveugle sculptee, ornee d'une rune d'ithildin */
    private static final String[] ARCH = {
            "....#....",
            "...#.#...",
            "..#...#..",
            "..#.o.#..",
            ".#..o..#.",
            ".#.....#.",
            ".#.ooo.#.",
            ".#..o..#.",
            ".#.ooo.#.",
            ".#..o..#.",
            ".#.....#.",
            ".#.ooo.#.",
            ".#..o..#.",
            ".#.....#.",
            ".#..o..#.",
            ".#.ooo.#.",
            ".#..o..#.",
            ".#.....#.",
            ".#.....#.",
            ".#.....#.",
            ".#.....#.",
            ".#.....#.",
            "#########",
    };

    /** a = coordonnee le long du mur, a0/pitch = les panneaux (centres a a0 + pitch/2 + k * pitch), dep = profondeur depuis la face du mur, v = hauteur au dessus du sol. */
    private static int relief(int a, int a0, int pitch, int v, int dep, int scale, int salt) {
        int idx = Math.floorDiv(a - a0, pitch);
        if ((hsh(idx, salt, 5) & 3) == 0) return KEEP;
        String[] m = ARCH;
        int cols = m[0].length(), w = cols * scale;
        int off = Math.floorMod(a - a0, pitch) - pitch / 2;
        if (off + w / 2 < 0) return KEEP;
        int col = (off + w / 2) / scale;
        if (col >= cols) return KEEP;
        int vv = v - 3;
        if (vv < 0 || vv >= m.length * scale || dep >= scale) return KEEP;
        char c = m[m.length - 1 - vv / scale].charAt(col);
        if (c == '.') return KEEP;
        return c == 'o' ? DRUNES : (dep == 0 ? DCHISEL : DSTONE);
    }

    private static int roomRelief(Room r, int x, int y, int z) {
        int scale, pitch;
        switch (r.kind) {
            case AVE: scale = 1; pitch = 28; break;
            case DINE: scale = 1; pitch = 48; break;
            case THRONE: case TREAS: case VILL: scale = 2; pitch = 44; break;
            default: return KEEP;
        }
        int dW = x - r.x0, dE = r.x1 - x, dN = z - r.z0, dS = r.z1 - z;
        int m = Math.min(Math.min(dW, dE), Math.min(dN, dS));
        if (m >= scale) return KEEP;
        int v = y - F;
        if (r.kind == AVE) return (m == dN || m == dS) ? relief(x, r.x0 + 7, pitch, v, m, scale, 11) : KEEP;
        if (m == dN || m == dS) return relief(x, r.x0, pitch, v, m, scale, 13 + r.kind);
        return relief(z, r.z0, pitch, v, m, scale, 17 + r.kind);
    }

    private static boolean nearPassage(int x, int z) {
        for (int[] p : P) if (x >= p[0] - 5 && x <= p[1] + 5 && z >= p[2] - 5 && z <= p[3] + 5) return true;
        return false;
    }

    /** relief des murs : pilastres, corniches, bossages irreguliers, panneaux et runes incrustees. m = distance au mur, a = abscisse le long du mur, v = hauteur */
    private static int wallTex(int m, int a, int v, int ceilV, int salt) {
        if (m > 3 || v < 1 || v > ceilV - 2) return KEEP;
        int am = Math.floorMod(a, 14), vm = Math.floorMod(v, 12);
        int pr;
        if (am <= 2) pr = (v >= ceilV - 8 || v <= 3) ? 4 : 3;
        else {
            double n = vn(a / 2.5, v / 2.5, salt);
            pr = n < 0.40 ? 0 : n < 0.64 ? 1 : n < 0.86 ? 2 : 3;
            if (am == 3 || am == 13) pr = Math.max(pr, 1);
            if (am >= 5 && am <= 11 && vm >= 3 && vm <= 8) pr = Math.min(pr, 1);          // le creux du panneau
        }
        if (vm >= 10) pr = Math.max(pr, vm == 11 ? 2 : 1);
        if (v <= 2) pr = Math.max(pr, 2);
        if (m >= pr) return KEEP;
        if (am <= 2) return vm == 11 ? DCHISEL : DPILLAR;
        if (vm >= 10) return DCHISEL;
        if (m == pr - 1 && am == 8 && vm == 5) return DRUNES;
        if (m == pr - 1 && (am == 5 || am == 11) && vm >= 3 && vm <= 8) return DCHISEL;
        double q = r01(a, v, m + salt);
        return q < 0.5 ? DBRICKS : q < 0.72 ? DCOBBLE : q < 0.9 ? DSTONE : DEEPSLATE;
    }

    /** Grande Salle (F0) et Forges (F2) : grands bas-reliefs et relief des murs */
    private static int bigRelief(int x, int y, int z, int h) {
        // Grande Salle : x [-12, 138], z [-472, -248], sol F0
        if (y > F0 + 0 && y < F0 + 56 && x >= -12 && x <= 138 && z >= -472 && z <= -248) {
            if (h - 11 < F0 + 54) return KEEP;
            int dW = x + 12, dE = 138 - x, dN = z + 472, dS = -248 - z;
            int m = Math.min(Math.min(dW, dE), Math.min(dN, dS));
            if (m > 3) return KEEP;
            int v = y - F0;
            if (m == dN && x >= 40 && x <= 110) return KEEP;                 // le haut de l'escalier
            if (m == dE && Math.abs(z + 360) <= 18) return KEEP;             // le couloir de la porte
            if (m < 2 && v > 3) {
                int rl = (m == dN || m == dS) ? relief(x, -6, 56, v, m, 2, 21) : relief(z, -360, 56, v, m, 2, 23);
                if (rl != KEEP) return rl;
            }
            boolean ns = m == dN || m == dS;
            return wallTex(m, ns ? x : z, v, 54, 41);
        }
        // Forges : x [-110, 250], z [-830, -688], sol F2
        if (y > F && y < F + 70 && x >= -110 && x <= 250 && z >= -830 && z <= -688) {
            int dW = x + 110, dE = 250 - x, dN = z + 830, dS = -688 - z;
            int m = Math.min(Math.min(dW, dE), Math.min(dN, dS));
            if (m > 3) return KEEP;
            if (nearPassage(x, z)) return KEEP;
            if (m == dN && x >= 36 && x <= 72) return KEEP;                  // la cascade
            if (m == dE && z >= -800 && z <= -720) return KEEP;              // vers la tombe
            int v = y - F;
            if (m < 2 && v > 3) {
                int rl = (m == dN || m == dS) ? relief(x, -16, 64, v, m, 2, 25) : relief(z, -16, 64, v, m, 2, 27);
                if (rl != KEEP) return rl;
            }
            boolean ns = m == dN || m == dS;
            return wallTex(m, ns ? x : z, v, Math.min(66, h - 11 - F), 43);
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ les fours geants des Forges
    private static final int[] FN = {-98, -50, 64, 160, 222};     // le long du mur nord (z = -688)
    private static final int[] FS = {-100, -40, 100, 180, 240};   // le long du mur sud (z = -830)
    /** 0 tour octogonale, 1 tour de brique a cheminee et rampe vers un moule, 2 forge a toit pyramidal, 3 forge a deux cheminees et rampe vers un moule */
    private static final int[] TN = {0, 1, 2, 3, 1};
    private static final int[] TS = {2, 0, 1, 3, 0};

    private static double oct(int au, int aw) { return Math.max(Math.max(au, aw), (au + aw) * 0.71); }

    /** goulotte de metal en fusion qui descend vers un moule : s = distance le long de la goulotte, t = decalage lateral, v = hauteur */
    private static int chuteMold(int s, int t, int v, int s0, int hs0) {
        int sm = s0 + hs0, at = Math.abs(t);
        if (s < s0 || s > sm + 4 || at > 2 || v < 0) return KEEP;
        if (s < sm) {
            int hs = Math.max(1, hs0 - (s - s0));
            if (at > 1) return KEEP;
            if (at == 1) return v < 1 ? KEEP : v <= hs ? DBRICKS : v == hs + 1 ? BWALL : KEEP;
            return v < 1 ? KEEP : v < hs ? DBRICKS : v == hs ? MAGMA : KEEP;
        }
        boolean ring = at == 2 || s == sm || s == sm + 4;
        if (v == 0) return ring ? KEEP : MAGMA;
        if (ring) {
            if (s == sm && t == 0 && v == 1) return MAGMA;
            if (v == 1) return DBRICKS;
            if (v == 2) return (at == 2 && (s == sm || s == sm + 4)) ? DCHISEL : DCHISEL;
            if (v == 3 && at == 2 && (s == sm || s == sm + 4)) return DPILLAR;
            if (v == 4 && at == 2 && (s == sm || s == sm + 4)) return DLAMP;
            return KEEP;
        }
        return v == 1 ? LAVA : AIR;
    }

    private static int furnace(int x, int y, int z, int h) {
        if (y < F || y > F + 13) return KEEP;
        int[] cs, ts;
        int f;
        boolean north;
        if (z >= -712 && z <= -688) { cs = FN; ts = TN; f = -688 - z; north = true; }
        else if (z >= -830 && z <= -806) { cs = FS; ts = TS; f = z + 830; north = false; }
        else return KEEP;
        int cx = Integer.MIN_VALUE, type = 0;
        for (int k = 0; k < cs.length; k++) if (Math.abs(x - cs[k]) <= 16) { cx = cs[k]; type = ts[k]; break; }
        if (cx == Integer.MIN_VALUE || h - 11 < F + 14) return KEEP;
        int u = x - cx, v = y - F;
        int face = north ? 2 : 0;
        switch (type) {
            case 0: return furnA(u, f - 12, v, face, x, y, z);
            case 1: return furnB(u, f - 10, v, x, y, z);
            case 2: return furnC(u, f - 10, v, x, y, z);
            default: return furnD(u, f - 7, v, x, y, z);
        }
    }

    /** tour octogonale qui s'effile, socle a deux gradins, escalier, ailes en biais, fenetres rougeoyantes, bassin de lave a creneaux (10 de haut) */
    private static int furnA(int u, int w, int v, int face, int x, int y, int z) {
        int au = Math.abs(u), aw = Math.abs(w);
        double o = oct(au, aw);
        if (w >= 10 && v >= 1) {
            if (au == 4 && w == 10) return lampPost(v, 3);
            if (v == 1 && au == 6 && w == 11) return u > 0 ? ANVIL : BARREL;
        }
        if (au >= 7 && au <= 9 && aw >= 7 && aw <= 9) {                   // les ailes (soufflets)
            if (v == 1) return au == 9 || aw == 9 ? DCHISEL : DBRICKS;
            if (v == 2 && au >= 8 && aw >= 8) return au == 9 && aw == 9 ? DRUNES : DCHISEL;
            if (v == 3 && au == 8 && aw == 8) return DLAMP;
            return KEEP;
        }
        if (au <= 2 && w == 10 && v == 1) return DSTAIR + face;
        if (au <= 2 && w == 9 && v == 2) return DSTAIR + face;
        if (au == 3 && ((w == 10 && v <= 1) || (w == 9 && v == 2))) return BWALL;
        if (v == 1 && o <= 9.2) return o > 8.3 ? DCHISEL : DBRICKS;
        if (v == 2 && o <= 8.0) return o > 7.2 ? DCHISEL : DBRICKS;
        int vc = 8;
        if (v < 3 || v > vc + 2) return KEEP;
        double rb = 6.4 - 0.3 * (Math.min(v, vc - 1) - 3), rc = rb + 0.9;
        if (v == vc) return o > rc ? KEEP : (o > rc - 1.5 ? DCHISEL : LAVA);
        if (v == vc + 1) return o > rc ? KEEP : (o > rc - 1.5 ? DBRICKS : AIR);
        if (v == vc + 2) return o > rc || o <= rc - 1.5 ? KEEP : ((au + aw) % 2 == 0 ? DCHISEL : AIR);
        if (o > rb) return KEEP;
        if (v == vc - 1 && o <= rb - 2.2) return MAGMA;
        if (o > rb - 1.1) {
            if (v == 3 || v == vc - 1) return DCHISEL;
            if (w > 0 && au <= 1 && v <= 6) {                               // la porte de fonte
                if (v == 6) return DCHISEL;
                return au == 1 ? DCHISEL : (v == 5 ? DCHISEL : (v == 3 ? DRUNES : DLAMP));
            }
            boolean pil = u == 0 || w == 0 || au == aw;
            if (pil && v >= 4) return DPILLAR;
            boolean slit = Math.abs(au * 5 - aw * 2) <= 3 || Math.abs(aw * 5 - au * 2) <= 3;
            if (slit && (v == 4 || v == 5 || v == 6)) return DLAMP;
        }
        double q = r01(x, y, z);
        return q < 0.55 ? DBRICKS : q < 0.8 ? DSTONE : DCOBBLE;
    }

    /** haute tour de brique sur un socle a trois gradins, cheminee fumante, robinets de cuivre, goulotte vers un moule de lave (10 de haut) */
    private static int furnB(int u, int w, int v, int x, int y, int z) {
        int au = Math.abs(u), aw = Math.abs(w);
        double o = oct(au, aw);
        if (u >= 6) { int c = chuteMold(u, w, v, 6, 5); if (c != KEEP) return c; }
        if (v == 1 && o <= 9.5) return o > 8.6 ? DCHISEL : DBRICKS;
        if (v == 2 && o <= 8.5) return o > 7.7 ? DCHISEL : DBRICKS;
        if (v == 3 && o <= 7.5) return o > 6.7 ? DCHISEL : DBRICKS;
        if (v < 4 || v > 10) return KEEP;
        if (v == 8) {                                                       // le chapeau
            if (o > 6.4) return KEEP;
            if (o <= 2.4) return LAVA;
            return o > 5.5 ? DCHISEL : DCHISEL;
        }
        if (v >= 9) {                                                       // la cheminee
            if (o > 3.4 || o <= 2.4) return o <= 2.4 ? AIR : KEEP;
            return v == 10 ? DCHISEL : DBRICKS;
        }
        if (o > 5.6) return KEEP;
        if (v == 7 && o <= 2.4) return MAGMA;
        if (o > 4.6) {
            boolean tap = (au == 5 && w == 0) || (u == 0 && aw == 5);
            if (tap && v == 5) return DCHISEL;
            if (au == aw && au >= 3 && au <= 4 && v == 6) return DLAMP;      // les hublots
            if (v == 7 || v == 4) return DCHISEL;
            if (v == 5 && o > 5.0) return Math.floorMod(au + aw, 3) == 0 ? DCHISEL : DBRICKS;
        }
        double q = r01(x, y, z);
        return q < 0.65 ? DBRICKS : q < 0.9 ? DSTONE : DCOBBLE;
    }

    /** grande forge ouverte a toit pyramidal, braises et feux derriere une grille, ventilation en haut (10 de haut) */
    private static int furnC(int u, int w, int v, int x, int y, int z) {
        int au = Math.abs(u), aw = Math.abs(w);
        if (au > 10 || aw > 7) return KEEP;
        if (v == 1) {
            if (au <= 8 && aw <= 5) return MAGMA;
            return au > 9 || aw > 6 ? DCHISEL : DBRICKS;
        }
        if (v >= 2 && v <= 6 && au <= 9 && aw <= 6) {
            boolean post = au >= 8 && aw >= 5;
            if (post) return v == 6 ? DCHISEL : DPILLAR;
            if (w == 6) {                                                    // la face avant : une grille
                if (v == 2) return DBRICKS;
                if (v == 6) return au % 4 == 0 ? DCHISEL : DCHISEL;
                if (au <= 7) return au % 4 == 0 ? DPILLAR : BARS;
                return DBRICKS;
            }
            if (w == -6) return v == 6 ? DCHISEL : (au % 3 == 0 && v >= 3 && v <= 5 ? DLAMP : DBRICKS);
            if (au == 9) return v == 6 ? DCHISEL : (aw <= 1 && v >= 3 && v <= 4 ? DLAMP : DBRICKS);
            if (v == 2 && ((au % 4 == 1) && (aw == 0 || aw == 3))) return CAMPFIRE;
            if (v == 2 && au <= 6 && aw <= 3 && r01(x, 2, z) < 0.18) return MAGMA;
            return AIR;
        }
        if (v == 7) return (au > 9 || aw > 6) ? DCHISEL : DSTONE;
        if (v == 8 && au <= 8 && aw <= 5) return (au > 7 || aw > 4) ? DCHISEL : DBRICKS;
        if (v == 9 && au <= 6 && aw <= 3) return (au > 5 || aw > 2) ? DCHISEL : DBRICKS;
        if (v == 10 && au <= 3 && aw <= 1) return (au == 3 || aw == 1) ? DCHISEL : LAVA;
        return KEEP;
    }

    /** forge a deux cheminees, gueule de lave et goulotte qui descend vers un moule dans la salle (10 de haut) */
    private static int furnD(int u, int w, int v, int x, int y, int z) {
        int au = Math.abs(u), aw = Math.abs(w);
        if (w >= 5) { int c = chuteMold(w, u, v, 5, 3); if (c != KEEP) return c; }
        if (au > 9) return KEEP;
        if ((v == 1 || v == 2) && aw <= 5) {
            if (v == 2 && au <= 6 && aw <= 3) return MAGMA;
            return v == 2 && (au > 8 || aw > 4) ? DCHISEL : DBRICKS;
        }
        if (v >= 3 && v <= 7 && au <= 8 && aw <= 4) {
            boolean shell = au == 8 || aw == 4;
            if (shell) {
                if (au == 8 && aw == 4) return v == 7 ? DCHISEL : DPILLAR;
                if (w == 4) {                                               // la face avant et sa gueule
                    int top = au <= 1 ? 5 : au <= 3 ? 4 : 0;
                    if (au <= 3 && v <= top) return v == 3 ? BSLAB : AIR;
                    if (au == 4 && v <= 5 || au == 5 && v <= 4) return DCHISEL;
                    if (au <= 3 && v == top + 1) return DCHISEL;
                    return v == 7 ? DCHISEL : (v == 6 && au % 2 == 0 ? DRUNES : DBRICKS);
                }
                if (v == 7) return DCHISEL;
                if (au == 8 && aw <= 1 && v >= 4 && v <= 5) return DLAMP;
                if (w == -4 && au % 4 == 2 && v == 5) return DLAMP;
                return r01(x, y, z) < 0.6 ? DBRICKS : DSTONE;
            }
            if (au <= 6 && aw <= 3) return v == 3 ? LAVA : AIR;
            return DBRICKS;
        }
        if (v == 8 && au <= 8 && aw <= 4) return (au > 7 || aw > 3) ? DCHISEL : DSTONE;
        if (v >= 9 && v <= 10 && au >= 4 && au <= 6 && w >= -3 && w <= -1) {
            if (au == 5 && w == -2) return v == 9 ? LAVA : AIR;
            return v == 10 ? DCHISEL : DBRICKS;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ le mobilier et la tuyauterie des Forges
    private static final int[] CROSS = {-70, 26, 125, 193};

    private static int pipeCell(int axisCoord, int dyc, int dOther) {
        if (Math.floorMod(axisCoord, 8) == 0) return IRONB;
        return SMBAS;
    }

    /** pointe de chaque descente de tuyau : {x, z, decalages en x} par four, selon son type */
    private static boolean drop(int x, int z, int[] cs, int[] ts, boolean north, int y, int yM) {
        for (int k = 0; k < cs.length; k++) {
            int t = ts[k], cx = cs[k];
            int zc;
            int[] offs;
            if (north) { zc = t == 0 ? -700 : t == 3 ? -693 : -698; }
            else { zc = t == 0 ? -818 : t == 3 ? -825 : -820; }
            offs = t == 3 ? new int[]{-5, 5} : new int[]{0};
            int zm = north ? -698 : -820;
            for (int o : offs) {
                if (Math.abs(x - (cx + o)) <= 1 && Math.abs(z - zc) <= 1 && y >= F + 12 && y <= yM + 1) return true;
                if (Math.abs(x - (cx + o)) <= 1 && z >= Math.min(zc, zm) && z <= Math.max(zc, zm) && Math.abs(y - yM) <= 1) return true;
            }
            if (t == 3 && Math.abs(z - zc) <= 1 && x >= cx - 5 && x <= cx + 5 && Math.abs(y - yM) <= 1) return true;
        }
        return false;
    }

    private static int pipes(int x, int y, int z, int top) {
        if (top < F + 40) return KEEP;
        int yM = F + 30, dy = y - yM;
        boolean inPipe = false;
        int axis = 0;
        for (int zm : new int[]{-698, -820}) if (Math.abs(z - zm) <= 1 && Math.abs(dy) <= 1) { inPipe = true; axis = x; }
        if (!inPipe) for (int cx2 : CROSS) if (Math.abs(x - cx2) <= 1 && Math.abs(dy) <= 1 && z >= -820 && z <= -698) { inPipe = true; axis = z; }
        if (!inPipe && (drop(x, z, FN, TN, true, y, yM) || drop(x, z, FS, TS, false, y, yM))) { inPipe = true; axis = y; }
        if (inPipe) return pipeCell(axis, dy, 0);
        // lanternes sous les collecteurs, chaines vers la voute
        for (int zm : new int[]{-698, -820}) {
            if (z == zm && dy == -2 && Math.floorMod(x, 12) == 6) return LANTERN;
            if (z == zm && dy > 1 && y <= top - 2 && Math.floorMod(x, 16) == 0) return CHAIN;
        }
        for (int cx2 : CROSS) {
            if (x == cx2 && dy == -2 && Math.floorMod(z, 12) == 6) return LANTERN;
            if (x == cx2 && dy > 1 && y <= top - 2 && Math.floorMod(z, 16) == 0) return CHAIN;
        }
        return KEEP;
    }

    private static int forgeProps(int x, int y, int z, int h) {
        if (x < -108 || x > 248 || z < -830 || z > -688 || y < F || y > F + 75) return KEEP;
        int top = Math.min(F + 72, h - 11);
        int r = pipes(x, y, z, top);
        if (r != KEEP) return r;
        int band, zc;
        if (z >= -739 && z <= -727) { band = 0; zc = -733; }
        else if (z >= -788 && z <= -776) { band = 1; zc = -782; }
        else return KEEP;
        int k = Math.floorDiv(x + 110, 14);
        int lx = x - (-110 + 14 * k + 7), lz = z - zc, v = y - F;
        if (Math.abs(lx) > 5 || Math.abs(lz) > 5 || v < 0 || v > 5) return KEEP;
        if (hsh(k, band, 32) % 4 == 0) return KEEP;
        int alx = Math.abs(lx), alz = Math.abs(lz);
        switch (hsh(k, band, 31) % 8) {
            case 0:                                                           // tonneaux empiles
                if (v >= 1 && v <= 2 && lx >= -1 && lx <= 0 && lz >= -1 && lz <= 0) return BARREL;
                if (v == 3 && lx == 0 && lz == 0) return BARREL;
                if (v == 1 && ((lx == 2 && lz == 0) || (lx == -3 && lz == 1) || (lx == 3 && lz == -2))) return BARREL;
                return KEEP;
            case 1:                                                           // poste de forge : enclume, forge, chaudron
                if (v == 0 && alx <= 4 && alz <= 2) return DBRICKS;
                if (v == 1) {
                    if (lx == 0 && lz == 0) return ANVIL;
                    if (lx == 2 && lz == 0) return FORGE;
                    if (lx == -2 && lz == 0) return CAULDRON;
                    if ((lx == -4 && lz == -1) || (lx == 4 && lz == 1)) return BARREL;
                }
                if (lx == 4 && lz == -2 && v >= 1) return lampPost(v, 3);
                return KEEP;
            case 2: {                                                         // lingots empiles
                if (v == 0 && alx <= 3 && alz <= 2) return DBRICKS;
                int hh = (alx <= 1 && lz == 0) ? 3 : (alx <= 2 && alz <= 1) ? 2 : 0;
                if (v >= 1 && v <= hh) { double c = r01(x, y, z); return c < 0.65 ? IRONB : c < 0.85 ? GOLD : RAWGOLD; }
                if (v == 1 && lx == 4 && lz == 0) return BARREL;
                return KEEP;
            }
            case 3:                                                           // voie ferree et chariot
                if (v == 0 && alz <= 1 && alx <= 5) return DBRICKS;
                if (v == 1 && lz == 0 && alx <= 4) return RAILX;
                if (v == 1 && alx == 5 && lz == 0) return DBRICKS;
                if (v == 1 && lx == 0 && lz == -2) return CAULDRON;
                if (v <= 2 && lx == 3 && lz == -2) return BARREL;
                if (v == 1 && lx == -3 && lz == 2) return BARREL;
                return KEEP;
            case 4:                                                           // ratelier d'armes
                if (lz == 0 && v >= 1 && v <= 3 && (lx == -4 || lx == 0 || lx == 4)) return FENCE;
                if (lz == 0 && v == 2 && alx < 4 && alx != 0) return BARS;
                if (lz == 0 && v == 4 && alx <= 4) return OAKP;
                if (v == 1 && lz == 1 && alx == 2) return BARREL;
                return KEEP;
            case 5:                                                           // caisses de bois
                if (v >= 1 && v <= 2 && lx >= -1 && lx <= 1 && lz >= -1 && lz <= 0) return r01(x, y, z) < 0.85 ? OAKP : KEEP;
                if (v == 3 && lx == 0 && lz == 0) return OAKP;
                if (v == 1 && lx == 3 && lz == 0) return OAKP;
                if (v == 2 && lx == 3 && lz == 0) return FENCE;
                return KEEP;
            case 6:                                                           // brasero
                if (alx <= 1 && alz <= 1) {
                    if (v == 1) return DBRICKS;
                    if (v == 2) return alx == 0 && alz == 0 ? CAMPFIRE : BWALL;
                    if (v == 3 && alx == 1 && alz == 1) return DLAMP;
                }
                return KEEP;
            default:
                return KEEP;
        }
    }

    /** relief du sol dans la Grande Salle et les Forges, hors des zones occupees */
    private static int floorExtra(int x, int y, int z) {
        if (y == F0 + 1 && x >= -8 && x <= 134 && z >= -468 && z <= -252) {
            if (z < -440 && x >= 38 && x <= 112) return KEEP;
            if (x >= 114 && Math.abs(z + 360) <= 18) return KEEP;
            if (Math.abs(z + 360) <= 4) return KEEP;
            int dx = Math.floorMod(x - 8, 28), dz = Math.floorMod(z + 346, 28);
            if ((dx <= 6 || dx >= 22) && (dz <= 6 || dz >= 22)) return KEEP;
            return floorRel(x, z);
        }
        if (y == F + 1 && x >= -106 && x <= 246 && z >= -826 && z <= -692) {
            if (Math.abs(z - (-759 + 5 * Math.sin(x / 31.0))) < 9) return KEEP;
            if ((z >= -712 && z <= -688) || (z >= -830 && z <= -806)) return KEEP;
            if (x >= 36 && x <= 72 && z <= -818) return KEEP;
            for (int fx : new int[]{-70, -34, 18, 138, 190, 230}) if (Math.abs(x - fx) <= 9 && z >= -806 && z <= -790) return KEEP;
            if (Math.abs(x - 102) <= 9 && Math.abs(z + 800) <= 9) return KEEP;
            if (x >= 236 && z >= -800 && z <= -720) return KEEP;
            int px = Math.floorMod(x + 54, 32);
            if ((px <= 6 || px >= 26) && (Math.abs(z + 806) <= 6 || Math.abs(z + 716) <= 6)) return KEEP;
            return floorRel(x, z);
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ salles
    private static double domes(Room r, int x, int z) {
        switch (r.kind) {
            case PH: {
                int cx = (r.x0 + r.x1) / 2, cz = (r.z0 + r.z1) / 2;
                double a = 0.5 - 0.5 * Math.cos(2 * Math.PI * (x - cx - 6) / 12.0), b = 0.5 - 0.5 * Math.cos(2 * Math.PI * (z - cz - 6) / 12.0);
                return a * b;
            }
            case AVE: return 0.5 - 0.5 * Math.cos(2 * Math.PI * (x - (r.x0 + 14)) / 14.0);
            default: return vn(x / 28.0, z / 28.0, 91);
        }
    }

    private static int pitch(Room r) {
        switch (r.kind) {
            case PH: return 12;
            case AVE: return 14;
            case VILL: return 20;
            case DINE: return 24;
            case THRONE: return 22;
            default: return 26;
        }
    }

    private static int nicheH(int off) { return off <= 1 ? 9 : off == 2 ? 6 : -1; }

    /** la grotte du quartier : coins arrondis (rayon 30) pour ne pas faire une boite */
    private static boolean roundedCorner(Room r, int x, int z) {
        final int R = 30;
        int cx = x < r.x0 + R ? r.x0 + R : (x > r.x1 - R ? r.x1 - R : x);
        int cz = z < r.z0 + R ? r.z0 + R : (z > r.z1 - R ? r.z1 - R : z);
        return Math.hypot(x - cx, z - cz) <= R;
    }

    /** le plafond de la grotte s'abaisse vers les bords : une coupole irreguliere */
    private static int vEdge(Room r, int x, int z) {
        double e = Math.hypot((x - (r.x0 + r.x1) / 2.0) / 71.0, (z - (r.z0 + r.z1) / 2.0) / 70.0);
        double t = Math.max(0, Math.min(1, (e - 0.5) / 0.6));
        return (int) Math.round(8 * t * t);
    }

    private static int room(int x, int y, int z, int h, Room r) {
        boolean vill = r.kind == VILL;
        if (x < r.x0 - 3 || x > r.x1 + 3 || z < r.z0 - 3 || z > r.z1 + 3 || y < (vill ? F - 50 : F) || y > F + r.hgt + (vill ? 12 : 6)) return KEEP;
        int ceil = vill ? Math.min(F + r.hgt + (int) Math.round(9 * vn(x / 15.0, z / 15.0, 91)) - vEdge(r, x, z), h - 11) : Math.min(F + r.hgt + (int) Math.round(3 * domes(r, x, z)), h - 11);
        if (ceil < F + 14) return KEEP;
        int ex1 = r.x1;
        if (vill) {
            double u = (z - VZ) / 70.0;
            ex1 -= (int) Math.round(18 * (1 - Math.sqrt(Math.max(0, 1 - 0.96 * u * u))));
            if (Math.abs(z - VZ) > 12) ex1 -= (int) Math.round(6 * vn(z / 12.0, 3.0, 92));
        }
        boolean in = x >= r.x0 && x <= ex1 && z >= r.z0 && z <= r.z1;
        if (in && vill) in = roundedCorner(r, x, z);
        if (!in && vill) return y <= ceil + 3 && y >= F - 12 ? rock(x, y, z) : KEEP;
        if (!in) {
            if (y < F || y > ceil + 1) return KEEP;
            boolean xw = x < r.x0 || x > r.x1, zw = z < r.z0 || z > r.z1;
            if (xw && zw) return KEEP;
            int layer = xw ? (x < r.x0 ? r.x0 - x : x - r.x1) : (z < r.z0 ? r.z0 - z : z - r.z1);
            int a = xw ? z - r.z0 : x - r.x0;
            int p = pitch(r);
            int m = Math.floorMod(a, p);
            boolean band = (y - F) % 12 == 11;
            int vy = y - F;
            // niches en ogive avec leur lampe (Grande Rue, festins, salles a piliers)
            if (layer <= 2 && (r.kind == PH || r.kind == AVE || r.kind == DINE) && (r.kind == PH || (Math.floorDiv(a, p) & 1) == 1)) {
                int off = Math.abs(m - p / 2);
                int hN = nicheH(off);
                boolean nearCorner = xw ? (z < r.z0 + 4 || z > r.z1 - 4) : (x < r.x0 + 4 || x > r.x1 - 4);
                if (hN >= 0 && vy >= 1 && vy <= hN && !nearCorner) {
                    if (layer == 2) return off == 0 && vy == 3 ? DLAMP : AIR;
                    if (off == 2 && vy == 4) return DLAMP;
                    return AIR;
                }
            }
            if (y == F + 8 && m == 0 && layer == 1) return DLAMP;
            if (m <= 1) return band ? DCHISEL : DPILLAR;
            if (layer == 1 && vy >= 4 && vy <= 5 && (m + 1) % 3 == 0 && r.kind != VILL) return DRUNES;      // frise d'ithildin
            return band ? DCHISEL : DBRICKS;
        }
        int fl = r.kind == VILL ? vFloor(x, z) : F;
        if (y < fl) return r.kind == VILL ? mason(x, y, z) : KEEP;
        if (y == fl) return r.kind == VILL ? vFloorBlock(x, z) : floorRoom(r, x, z);
        if (y > ceil) return vill ? (y <= ceil + 3 ? rock(x, y, z) : KEEP) : (y == ceil + 1 ? vaultTop(x, z) : KEEP);
        int rl = roomRelief(r, x, y, z);
        if (rl != KEEP) return rl;
        int v;
        switch (r.kind) {
            case AVE: v = ave(r, x, y, z, ceil); break;
            case VILL: v = village(r, x, y, z, ceil); break;
            case DINE: v = dining(r, x, y, z, ceil); break;
            case PH: v = pillarHall(r, x, y, z, ceil); break;
            case THRONE: v = throne(r, x, y, z, ceil); break;
            default: v = treasury(r, x, y, z, ceil); break;
        }
        if (v != KEEP) return v;
        if (!vill) {
            int dW = x - r.x0, dE = r.x1 - x, dN = z - r.z0, dS = r.z1 - z;
            int m = Math.min(Math.min(dW, dE), Math.min(dN, dS));
            if (m <= 3 && !nearPassage(x, z)) {
                boolean ns = m == dN || m == dS;
                int t = wallTex(m, ns ? x : z, y - F, ceil - F, 51 + r.kind);
                if (t != KEEP) return t;
            }
        }
        if (!vill && y == F + 1) {
            int dW = x - r.x0, dE = r.x1 - x, dN = z - r.z0, dS = r.z1 - z;
            if (Math.min(Math.min(dW, dE), Math.min(dN, dS)) > 3 && !nearPassage(x, z)) {
                int fr = floorRel(x, z);
                if (fr != KEEP) return fr;
            }
        }
        int q = hangingLamp(x, y, z, ceil, 16);
        if (q != KEEP) return q;
        if (!vill && y >= ceil - 1) {                                       // plafond a caissons
            boolean bx = Math.floorMod(x, 8) < 2, bz = Math.floorMod(z, 8) < 2;
            if (y == ceil && (bx || bz)) return DCHISEL;
            if (y == ceil - 1 && bx && bz) return DPILLAR;
        }
        if (y == ceil && r01(x, 12, z) < 0.012) return WEB;
        return AIR;
    }

    private static int vaultTop(int x, int z) { return r01(x, 33, z) < 0.45 ? DSTONE : DBRICKS; }

    private static int floorRoom(Room r, int x, int z) {
        if (r.kind == THRONE && Math.abs(x - 140) == 6 && z > -918) return DCHISEL;
        if (r.kind == AVE && Math.abs(z + 730) <= 3 && Math.floorMod(x, 6) < 2) return DCHISEL;
        return floorPat(x, z);
    }

    // ------------------------------------------------------------------ elements communs
    /** pilier de rayon r0 a (px, pz) : base evasee, chapiteau evase, bandeaux chaque 12 blocs. */
    private static int pillar(int x, int y, int z, int px, int pz, int ceil, int r0, boolean broken, int top) {
        int ax = Math.abs(x - px), az = Math.abs(z - pz);
        if (ax > r0 + 6 || az > r0 + 6) return KEEP;
        double rr = r0 + (y > ceil - 10 && !broken ? (y - (ceil - 10)) * 0.36 : 0) + (y < F + 4 ? 1 : 0);
        if (Math.max(ax, az) <= rr && ax + az <= rr + 2 && y <= top) {
            if (y == F + 8 && ((ax == r0 && az == 0) || (az == r0 && ax == 0))) return DLAMP;
            if (y < F + 4 || y > ceil - 3) return DBRICKS;
            return (y - F) % 12 == 11 ? DCHISEL : DPILLAR;
        }
        return KEEP;
    }

    private static int lampPost(int v, int h) { return v >= 1 && v < h ? DBRICKS : v == h ? DLAMP : KEEP; }

    /** bande d'etoffe verticale : axe 'a' (coordonnee le long du mur), centre, hauteur */
    private static int banner(int a, int center, int y, int yTop, int yBot) {
        int d = Math.abs(a - center);
        if (d > 2 || y > yTop || y < yBot) return KEEP;
        if (d == 2 || y == yBot) return BLACKWOOL;
        return (d == 0 && (y - yBot) % 6 == 3) ? YELLOWWOOL : REDWOOL;
    }

    private static int pile(int x, int y, int z, int fy, int cell, double prob, int mat) {
        int gx = Math.floorDiv(x, cell), gz = Math.floorDiv(z, cell);
        if (r01(gx, 71, gz) >= prob) return KEEP;
        int cx = gx * cell + cell / 3 + hsh(gx, 1, gz) % (cell / 3), cz = gz * cell + cell / 3 + hsh(gx, 2, gz) % (cell / 3);
        double d = Math.hypot(x - cx, z - cz);
        int hh = (int) (3.5 * (1 - d / 4.2) + r01(x, y, z) * 0.9);
        if (d < 4.2 && y - fy <= hh && y > fy) return mat == GOLD ? (r01(x, y, z) < 0.15 ? RAWGOLD : GOLD) : r01(x, y, z) < 0.35 ? ORE + hsh(gx, 3, gz) % 5 : (r01(x, 5, z) < 0.5 ? GRAVEL : DSTONE);
        return KEEP;
    }

    // ------------------------------------------------------------------ la Grande Rue
    private static int ave(Room r, int x, int y, int z, int ceil) {
        int zc = -730, v = y - F;
        int ix = Math.round((x - (r.x0 + 14)) / 14f);
        if (ix >= 0 && ix <= 8) {
            int px = r.x0 + 14 + 14 * ix;
            int p = pillar(x, y, z, px, zc - 8, ceil, 3, false, ceil);
            if (p != KEEP) return p;
            p = pillar(x, y, z, px, zc + 8, ceil, 3, false, ceil);
            if (p != KEEP) return p;
        }
        // gardiens contre les murs, entre les piliers
        for (int j = 0; j < 4; j++) {
            int gx = r.x0 + 35 + 28 * j;
            if (Math.abs(x - gx) <= 10) {
            }
        }
        if (v == 1 && Math.abs(z - zc) <= 1) return RCARPET;
        if (v == 1 && Math.abs(z - zc) == 5 && Math.floorMod(x, 14) < 3) return BSLAB;
        if (v == 1 && (Math.abs(z - zc) == 14 || Math.abs(z - zc) == 15) && Math.floorMod(x, 14) == 7) return BARREL;
        if (z == r.z0 || z == r.z1) {
            int b = banner(x, r.x0 + 21 + 14 * Math.floorDiv(x - r.x0 - 14, 14), y, F + 20, F + 7);
            if (b != KEEP) return b;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ le Quartier des nains : une ravine de lave, des terrasses, des ponts
    private static final int VZ = -630;
    private static final int[] LANES = {-245, -212, -177, -145};

    private static boolean ravine(int x, int z) {
        double a = (x + 199) / 57.0, b = (z - VZ) / 12.5;
        return a * a + b * b <= 1;
    }

    /** altitude du sol du quartier : corniche haute au pied des falaises, terrasses descendantes, ravine de lave au centre */
    static int vFloor(int x, int z) {
        if (ravine(x, z)) return F - 48;
        int d = Math.abs(z - VZ);
        if (x >= -138 && x <= -120 && Math.abs(z + 631) <= 6) return F - Math.min(18, -120 - x);
        for (int xs : LANES) {
            if (Math.abs(x - xs) <= 2) {
                if (d >= 38) return F;
                if (d >= 29) return F - (38 - d);
                if (d >= 25) return F - 9;
                if (d >= 16) return F - 9 - (25 - d);
                return F - 18;
            }
        }
        return d >= 38 ? F : d >= 25 ? F - 9 : F - 18;
    }

    /** direction (0 N, 1 E, 2 S, 3 O) de montee si la case est une marche d'escalier, sinon -1 */
    private static int stairFacing(int x, int z) {
        if (ravine(x, z)) return -1;
        if (x >= -138 && x <= -121 && Math.abs(z + 631) <= 6) return 1;
        int d = Math.abs(z - VZ);
        for (int xs : LANES) {
            if (Math.abs(x - xs) <= 2 && ((d >= 29 && d < 38) || (d >= 16 && d < 25))) return z < VZ ? 0 : 2;
        }
        return -1;
    }

    private static int vFloorBlock(int x, int z) {
        if (ravine(x, z)) { double q = r01(x, 2, z); return q < 0.4 ? MAGMA : BLACKSTONE; }
        int sf = stairFacing(x, z);
        if (sf >= 0) return BSTAIR + sf;
        return floorPat(x, z);
    }

    private static int minNeighbour(int x, int z) {
        return Math.min(Math.min(vFloor(x + 1, z), vFloor(x - 1, z)), Math.min(vFloor(x, z + 1), vFloor(x, z - 1)));
    }

    /** roche brute des falaises et des soutenements */
    private static int rock(int x, int y, int z) {
        double q = r01(x, y, z);
        return q < 0.3 ? DEEPSLATE : q < 0.56 ? DCOBBLE : q < 0.82 ? DSTONE : q < 0.93 ? DBRICKS : BLACKSTONE;
    }

    private static int mason(int x, int y, int z) {
        if (y > minNeighbour(x, z)) return rock(x, y, z);
        return KEEP;
    }

    private static int stalactite(int x, int y, int z, int ceil) {
        if (y < ceil - 9 || y > ceil) return KEEP;
        if (r01(x, 31, z) >= 0.045) return KEEP;
        int len = 2 + hsh(x, 9, z) % 8;
        return y > ceil - len ? DEEPSLATE : KEEP;
    }

    private static int village(Room r, int x, int y, int z, int ceil) {
        int v = y - F;
        int fy = vFloor(x, z);
        int cl = cliff(x, y, z);
        if (cl != KEEP) return cl;
        int st = stalactite(x, y, z, ceil);
        if (st != KEEP) return st;
        // le grand pont (de la Grande Rue aux festins) et ses piles
        int bx = Math.abs(x + 202);
        if (bx <= 4) {
            if (y == F || y == F - 1) return (y == F && bx == 0 && Math.floorMod(z, 4) < 2) ? DRUNES : (y == F && bx >= 3 ? DCHISEL : DBRICKS);
            if (y == F + 1 && bx == 4) return BWALL;
            if (bx == 4 && Math.floorMod(z, 10) == 0 && v >= 1 && v <= 4) return lampPost(v, 4);
            if (y < F - 1 && bx <= 2 && Math.floorMod(z, 20) <= 1) return (y - F) % 6 == 0 ? DCHISEL : DPILLAR;
            if (y < F - 1 && y > F - 5 && bx <= 2 && Math.floorMod(z, 20) <= 3) return DBRICKS;
        }
        // pont de pierre au dessus de la ravine
        int nz = Math.abs(z - VZ);
        if (Math.abs(x + 230) <= 2 && (ravine(x, z) || (nz >= 12 && nz <= 14))) {
            if (y == F - 18) return x == -230 ? DCHISEL : DBRICKS;
            if (y == F - 17 && Math.abs(x + 230) == 2) return BWALL;
            if (Math.abs(x + 230) <= 1 && nz <= 2 && y < F - 18 && y > F - 48) return DPILLAR;
        }
        // pont de bois
        if (Math.abs(x + 168) <= 2 && (ravine(x, z) || (nz >= 12 && nz <= 14))) {
            if (y == F - 18) return OAKP;
            if (y == F - 17 && Math.abs(x + 168) == 2) return FENCE;
            if (y == F - 16 && Math.abs(x + 168) == 2 && Math.floorMod(z, 6) == 0) return DLAMP;
            if (x == -168 && nz <= 1 && y < F - 18 && y > F - 48) return OAKLOG;
        }
        // plates-formes suspendues
        int p = platform(x, y, z, -184, F - 8, ceil);
        if (p != KEEP) return p;
        p = platform(x, y, z, -240, F - 26, ceil);
        if (p != KEEP) return p;
        // la lave de la ravine
        if (ravine(x, z)) return y <= F - 41 ? LAVA : AIR;
        // bordure des corniches : garde-corps et torches
        if (y >= fy + 1 && y <= fy + 3 && (y == fy + 1 || (x + z) % 8 == 0) && stairFacing(x, z) < 0 && minNeighbour(x, z) < fy - 1) {
            if (y == fy + 1) return (x + z) % 8 == 0 ? DBRICKS : BWALL;
            return y == fy + 2 ? DBRICKS : DLAMP;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ les falaises habitees : des appartements creuses dans la roche
    /** direction de la marche selon le mur (side 0 nord, 1 sud : +x = est ; side 2 ouest : +z = sud) */
    private static int stairDir(int side, boolean plus) {
        if (side < 2) return plus ? 1 : 3;
        return plus ? 2 : 0;
    }

    /** les falaises s'incurvent : les ailes s'avancent vers le centre, la grotte prend une forme ovale */
    private static int bow(int center, double half) {
        double u = (center - half) / half;
        return (int) Math.round(11 * (1 - Math.sqrt(Math.max(0, 1 - 0.96 * u * u))));
    }

    private static int cliff(int x, int y, int z) {
        int side, q0, a;
        int shNS = bow(Math.min(11, Math.max(0, (x + 262) / 12)) * 12 + 6, 72.0);
        int shW = bow(Math.min(8, Math.max(0, (z + 681) / 12)) * 12 + 6, 51.0);
        if (z <= -682 + shNS && x >= -262 && x <= -120) { side = 0; q0 = -682 + shNS - z; a = x + 262; }
        else if (z >= -578 - shNS && x >= -262 && x <= -120) { side = 1; q0 = z + 578 + shNS; a = x + 262; }
        else if (x <= -244 + shW && x >= -262 && z > -682 && z < -578) { side = 2; q0 = -244 + shW - x; a = z + 681; }
        else return KEEP;
        int r = y - F;
        if (r < 0) return side == 2 ? rock(x, y, z) : KEEP;
        int i = a / 12, xl = a % 12;
        boolean corridor = side < 2 && (i == 4 || i == 5);
        if (corridor) return KEEP;
        boolean stairU = side < 2 ? (i == 2 || i == 9) : i == 3;
        boolean palaceU = side == 1 && (i == 6 || i == 7);
        if (palaceU) { int pr = palace(a - 72, q0, r); if (pr != KEEP) return pr; }
        // les escaliers taillés le long des balcons
        if (stairU && r >= 1) {
            int ts = (r - 1) / 9;
            if (ts <= 2) {
                int rp = r - 9 * ts, dd = q0 - 3 * ts;
                boolean up = (ts & 1) == 0;
                if (xl >= 1 && xl <= 9) {
                    int kk = up ? xl - 1 : 9 - xl;
                    if (dd == -2 || dd == -1) {
                        if (rp == kk + 1) return DSTAIR + stairDir(side, up);
                        if (rp < kk + 1) return DBRICKS;
                        return AIR;
                    }
                    int jx = (ts & 1) == 0 ? 1 : 9;
                    if (dd == -3 && (ts >= 1 || side == 2) && rp <= kk + 2) return (ts >= 1 && xl == jx) ? AIR : BWALL;
                }
            }
        }
        int t = Math.min(r / 9, 4), rr = r - 9 * t;
        int d = q0 - 3 * t;
        if (rr == 0) {                                                       // le plancher / balcon de l'etage
            if (d < -3) return AIR;
            if (d == -3) return DCHISEL;
            double q = r01(x, y, z);
            return q < 0.55 ? DBRICKS : q < 0.8 ? DSTONE : DCOBBLE;
        }
        if (t >= 4) {                                                        // le sommet : roche brute qui recule
            int jit = (int) Math.round(2 * vn(a / 5.0, r / 5.0, 88));
            return d >= jit ? rock(x, y, z) : AIR;
        }
        if (d < 0) {                                                         // balcon
            if (d == -3 && (t >= 1 || side == 2)) {
                boolean junction = stairU && t >= 1 && xl == (((t - 1) & 1) == 0 ? 9 : 1);
                if (!junction) {
                    if (xl == 0) return rr <= 2 ? DPILLAR : rr == 3 ? DLAMP : AIR;
                    if (rr == 1) return BWALL;
                }
            }
            return AIR;
        }
        // d >= 0 : la roche et les appartements
        if (palaceU || stairU || rr == 8) {
            if (d == 0) return rr == 8 ? DCHISEL : (xl == 0 || xl == 9 ? DPILLAR : DBRICKS);
            return rock(x, y, z);
        }
        int dmax = 16 - 3 * t;
        boolean apt = xl >= 1 && xl <= 8;
        if (d == 0) {                                                        // la facade
            if (apt) {
                if (xl >= 4 && xl <= 5 && rr <= 3) return AIR;
                if (xl >= 3 && xl <= 6 && rr == 4) return DCHISEL;
                if (xl >= 4 && xl <= 5 && rr == 5) return DRUNES;
                if ((xl == 3 || xl == 6) && rr <= 3) return DCHISEL;
                if ((xl <= 2 || xl >= 7) && rr >= 3 && rr <= 5) return BARS;
                if ((xl <= 2 || xl >= 7) && (rr == 2 || rr == 6)) return DCHISEL;
                return rr == 7 ? DCHISEL : DBRICKS;
            }
            return xl == 0 || xl == 9 ? DPILLAR : DBRICKS;
        }
        if (apt && d <= dmax) {                                              // l'interieur
            int cell = hsh(i, t, side + 7);
            int zl = d - (dmax - 8);
            if (zl < 1) return AIR;
            if (rr == 7 && xl == 4 && zl == 4) return LANTERN;
            if (rr == 7 && xl == 5 && zl == 4) return AIR;
            return interior(cell, rr, xl + 1, zl);
        }
        return rock(x, y, z);
    }

    /** la salle du roi : un palais taille dans la falaise sud. ar = 0..23 le long de la facade, d = profondeur, r = hauteur */
    private static int palace(int ar, int d, int r) {
        if (r < 1 || r > 13) return KEEP;
        double dc = Math.abs(ar - 11.5);
        if (d < 0) {                                                         // les piles avancees
            if (d >= -2 && (ar <= 2 || ar >= 21) && r <= 12) return r >= 11 ? DCHISEL : DPILLAR;
            return KEEP;
        }
        if (d == 0) {
            int ow = r <= 6 ? 5 : r == 7 ? 4 : r == 8 ? 2 : -1;
            if (ow >= 0 && dc <= ow + 0.5) return AIR;
            if (ar <= 2 || ar >= 21) return r == 13 ? DCHISEL : DPILLAR;
            if (dc <= 6.5 && r <= 8) return DCHISEL;
            if (r >= 10 && r <= 12) return r == 11 && ar % 3 == 0 ? DRUNES : DCHISEL;
            return DBRICKS;
        }
        if (d > 16 || ar < 2 || ar > 21) return KEEP;
        if (d <= 3 && r >= 9) return KEEP;                                   // le vestibule reste bas
        if (r == 13) return d % 4 == 0 || ar % 6 == 2 ? DCHISEL : DBRICKS;
        if ((ar == 5 || ar == 6 || ar == 17 || ar == 18) && (d == 5 || d == 6 || d == 10 || d == 11)) return r == 12 ? DCHISEL : DPILLAR;
        if (d >= 13 && r <= (d >= 15 ? 2 : 1)) return DBRICKS;
        if ((ar == 11 || ar == 12) && d == 15 && r == 3) return GOLD;
        if (d == 16 && ar >= 10 && ar <= 13 && r >= 3 && r <= 11) return (ar == 10 || ar == 13 || r == 11) ? GOLD : DCHISEL;
        if (r == 1 && (ar == 11 || ar == 12) && d <= 12) return RCARPET;
        if (r == 12 && (ar == 3 || ar == 20) && d % 5 == 3) return DLAMP;
        if (r == 1 && (ar == 3 || ar == 20) && d % 4 == 2) return BARREL;
        return AIR;
    }

    private static int platform(int x, int y, int z, int px, int py, int ceil) {
        int dx = Math.abs(x - px), dz = Math.abs(z - VZ);
        if (dx > 4 || dz > 4) return KEEP;
        if (y == py) return dx == 4 || dz == 4 ? OAKLOGX : OAKP;
        if (y == py + 1 && dx == 4 && dz == 4) return LANTERN;
        if (y == py + 1 && dx == 2 && dz == 2) return BARREL;
        if (dx == 4 && dz == 4 && y > py + 1 && y <= ceil) return CHAIN;
        return KEEP;
    }

    private static int interior(int cell, int v, int xl, int zl) {
        switch ((cell >> 3) % 4) {
            case 0:                                                                          // chambre
                if (v == 1 && xl <= 2 && zl >= 6) return xl == 1 ? BLACKWOOL : REDWOOL;
                if (v <= 2 && zl == 8 && xl >= 6 && xl <= 9) return BOOKS;
                if (v == 1 && xl == 7 && zl == 2) return FENCE;
                if (v == 2 && xl >= 6 && xl <= 8 && zl == 2) return BSLAB;
                break;
            case 1:                                                                          // forge
                if (v == 1 && xl == 9 && zl == 2) return FORGE;
                if (v == 1 && xl == 7 && zl == 2) return ANVIL;
                if (v <= 2 && ((xl == 1 || xl == 2) && zl == 8) && !(v == 2 && xl == 2)) return BARREL;
                break;
            case 2:                                                                          // cuisine
                if (v == 1 && xl == 9 && zl == 8) return FURNACE;
                if (v == 1 && xl == 8 && zl == 8) return CAULDRON;
                if (v == 1 && xl == 5 && zl == 7) return CAMPFIRE;
                if (v <= 2 && xl == 1 && (zl == 7 || zl == 8) && !(v == 2 && zl == 7)) return BARREL;
                if (v == 1 && xl == 4 && zl == 3) return FENCE;
                if (v == 2 && xl >= 3 && xl <= 5 && zl == 3) return BSLAB;
                break;
            default:                                                                         // etude
                if (v <= 2 && zl == 8 && xl >= 1 && xl <= 9) return BOOKS;
                if (v == 1 && xl == 9 && zl == 2) return CRAFT;
                if (v == 1 && xl == 4 && zl == 3) return FENCE;
                if (v == 2 && xl >= 3 && xl <= 5 && zl == 3) return BSLAB;
        }
        return AIR;
    }

    // ------------------------------------------------------------------ la salle des festins
    private static final int[] ROWS_T = {-515, -501, -481, -467};

    private static int dining(Room r, int x, int y, int z, int ceil) {
        int zc = -491, v = y - F;
        int ix = Math.round((x - (r.x0 + 12)) / 24f);
        if (ix >= 0 && ix <= 5) {
            int p = pillar(x, y, z, r.x0 + 12 + 24 * ix, zc, ceil, 3, false, ceil);
            if (p != KEEP) return p;
        }
        // les gardiens des deux pignons
        // les tables et les bancs
        if (x >= -250 && x <= -134) {
            for (int zt : ROWS_T) {
                if (z == zt || z == zt + 1) {
                    if (v == 3) return BSLAB;
                    if (v <= 2 && Math.floorMod(x, 4) == 0) return FENCE;
                    if (v == 4 && Math.floorMod(x + zt, 6) == 0 && z == zt) return r01(x, 2, z) < 0.5 ? DLAMP : CAULDRON;
                }
                if ((z == zt - 1 || z == zt + 2) && v == 1) return BSLAB;
            }
        }
        // les cheminees
        if (x >= r.x0 && x <= r.x0 + 8 && Math.abs(z - zc) <= 7) {
            int dx = x - r.x0, dz = Math.abs(z - zc);
            if (dx <= 4 && dz <= 6) {
                boolean trough = dx >= 1 && dx <= 3 && dz <= 3;
                if (trough) return v == 1 ? (r01(x, 7, z) < 0.5 ? MAGMA : LAVA) : AIR;
                if (v <= 2) return dx == 0 || dz == 6 || dx == 4 ? DBRICKS : BWALL;
                if (dx == 0 && v <= 18) return DBRICKS;
            }
        }
        if (x == r.x1 - 1 && Math.abs(z - zc) <= 2 && v <= 3) return v == 3 ? BSLAB : FENCE;      // la table haute
        if (v == 1 && (z == r.z0 + 1 || z == r.z1 - 1) && Math.floorMod(x, 9) < 2) return BARREL;
        if (v == 2 && (z == r.z0 + 1 || z == r.z1 - 1) && Math.floorMod(x, 18) < 1) return BARREL;
        if (z == r.z0 || z == r.z1) {
            int b = banner(x, r.x0 + 12 + 24 * Math.floorDiv(x - r.x0, 24) + 12, y, F + 22, F + 8);
            if (b != KEEP) return b;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ salles a piliers (armurerie, archives, crypte)
    private static int pillarHall(Room r, int x, int y, int z, int ceil) {
        int cx = (r.x0 + r.x1) / 2, cz = (r.z0 + r.z1) / 2, v = y - F;
        int ix = Math.floorDiv(x - cx, 12), iz = Math.floorDiv(z - cz, 12);
        int px = cx + 6 + 12 * ix, pz = cz + 6 + 12 * iz;
        if (ix >= -3 && ix <= 2 && iz >= -3 && iz <= 2) {
            boolean broken = r01(px, 4, pz) < 0.14;
            int top = broken ? F + 8 + (int) (r01(px, 5, pz) * 20) : ceil;
            int p = pillar(x, y, z, px, pz, ceil, 2, broken, top);
            if (p != KEEP) return p;
            if (broken && Math.max(Math.abs(x - px), Math.abs(z - pz)) <= 5 && v <= 3 - Math.max(Math.abs(x - px), Math.abs(z - pz)) / 2 + (int) (r01(x, y, z) * 1.5))
                return r01(x, y, z) < 0.5 ? DCOBBLE : DSTONE;
        }
        // un gardien a chaque bout, dans l'axe
        int dx = x - cx, dz = z - cz;
        switch (r.variant) {
            case 1: {                                                                        // armurerie : enclumes, forge, tonneaux
                if (v == 1 && Math.abs(dx) >= 18 && Math.abs(dx) <= 19 && Math.abs(dz) <= 24 && Math.floorMod(dz, 8) == 0) return ANVIL;
                if (v == 1 && Math.abs(dx) <= 2 && Math.abs(dz) <= 2) return DBRICKS;
                if (v == 2 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return FORGE;
                if (v == 1 && Math.abs(dz) <= 1 && Math.abs(dx) >= 6 && Math.abs(dx) <= 8 && Math.floorMod(dx, 2) == 0) return BARREL;
                break;
            }
            case 2: {                                                                        // archives : rayonnages de livres
                if (v >= 1 && v <= 4 && Math.floorMod(x - r.x0, 12) >= 9 && Math.abs(dz) >= 9 && z >= r.z0 + 2 && z <= r.z1 - 2 && Math.floorMod(z - cz, 12) != 6 && Math.floorMod(z - cz, 12) != 5) return BOOKS;
                if (v == 1 && Math.abs(dx) <= 1 && Math.abs(dz) <= 4 && Math.floorMod(dz, 2) == 0) return FENCE;
                if (v == 2 && Math.abs(dx) <= 1 && Math.abs(dz) <= 4 && Math.floorMod(dz, 2) == 0) return BSLAB;
                if (v == 1 && Math.abs(dx) == 3 && Math.abs(dz) <= 4 && Math.floorMod(dz, 2) == 0) return BSLAB;
                break;
            }
            default: {                                                                       // crypte : sarcophages, os, toiles
                int sx = Math.floorMod(x - r.x0, 12), sz = Math.floorMod(z - r.z0, 12);
                if (sx >= 8 && sx <= 10 && sz >= 1 && sz <= 5 && z >= r.z0 + 2 && z <= r.z1 - 6 && Math.abs(dx) >= 10) {
                    if (v == 1) return QUARTZ;
                    if (v == 2 && sx == 9) return QSLAB;
                }
                if (v == 1 && r01(x, 8, z) < 0.012) return BONES;
                if (y == ceil - 1 && r01(x, 13, z) < 0.05) return WEB;
                break;
            }
        }
        int rb = rubble(x, y, z, F);
        if (rb != KEEP && v >= 1) return rb;
        return KEEP;
    }

    // ------------------------------------------------------------------ la salle du trone
    private static int throne(Room r, int x, int y, int z, int ceil) {
        int cx = 140, dx = x - cx, adx = Math.abs(dx);
        int n = 0;
        if (adx <= 14 && z <= -916) n = Math.min(7, (-916 - z) / 3 + 1);
        int top = F + n;
        if (n > 0 && y <= top) return n == 7 ? (adx == 14 || z == -957 ? DCHISEL : WHITE) : (y == top && (-916 - z) % 3 == 0 ? DCHISEL : DBRICKS);
        // le fauteuil et son dossier
        if (z >= -948 && z <= -944 && adx <= 3) {
            int vv = y - top;
            if (z == -946 && adx == 0 && vv == 1) return DSTAIR;
            if (adx == 2 && vv >= 1 && vv <= 2 && z >= -946 && z <= -945) return DSTONE;
            if (z <= -947) {
                int hw = vv <= 9 ? 3 : 3 - (vv - 9) * 3 / 10;
                if (vv >= 1 && vv <= 19 && adx <= hw) return (adx == hw || vv == 19 || (adx == 0 && vv % 5 == 0)) ? GOLD : DCHISEL;
            }
        }
        // les deux rangees de colosses
        for (int i = 0; i < 6; i++) {
            int pz = -852 - 16 * i;
            if (Math.abs(z - pz) <= 6) {
                int p = pillar(x, y, z, cx - 16, pz, ceil, 4, false, ceil);
                if (p != KEEP) return p;
                p = pillar(x, y, z, cx + 16, pz, ceil, 4, false, ceil);
                if (p != KEEP) return p;
            }
        }
        // deux colosses de plus, de chaque cote de l'entree
        int v = y - top;
        if (adx <= 2 && v == 1 && z <= -837 && z >= -944) return RCARPET;
        if (adx == 9 && (z + 850) % 12 == 0 && z <= -850 && z >= -946 && n == 0) { int lp = lampPost(y - F, 4); if (lp != KEEP) return lp; }
        // etendards au fond
        if (z == -957 && y > F + 7) {
            int b = banner(x, cx - 24, y, F + 44, F + 14);
            if (b == KEEP) b = banner(x, cx - 12, y, F + 40, F + 20);
            if (b == KEEP) b = banner(x, cx + 12, y, F + 40, F + 20);
            if (b == KEEP) b = banner(x, cx + 24, y, F + 44, F + 14);
            if (b != KEEP) return b;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ le tresor
    private static int treasury(Room r, int x, int y, int z, int ceil) {
        int cx = 263, cz = -912, v = y - F;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int p = pillar(x, y, z, cx + 46 * sx, cz + 28 * sz, ceil, 4, false, ceil);
            if (p != KEEP) return (p == DPILLAR && (y - F) % 6 == 2) ? GOLD : p;
        }
        double d = Math.hypot(x - cx, (z - cz) * 1.15);
        if (d < 58 && v >= 1) {
            double g = 38 * Math.pow(Math.max(0, 1 - d / 57), 1.4) + 3 * vn(x / 5.0, z / 5.0, 81) * Math.min(1, d / 10);
            int top = (int) Math.round(g);
            if (v <= top) { double q = r01(x, y, z); return q < 0.1 ? RAWGOLD : q < 0.115 ? ORE + 4 : GOLD; }
            if (v == top + 1 && r01(x, 22, z) < 0.012) return BONES;
        } else {
            int pl = pile(x, y, z, F, 11, 0.4, GOLD);
            if (pl != KEEP) return pl;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ la mine a ciel ouvert
    private static final int MCX = -214, MCZ = -863;
    private static final double MA = 74, MB = 84;

    private static double mdd(int x, int z) {
        double a = (x - MCX) / MA, b = (z - MCZ) / MB;
        return Math.hypot(a, b) + 0.05 * (vn(x / 9.0, z / 9.0, 95) - 0.5);
    }

    private static int mineFloor(int x, int z, double dd, boolean terrace) {
        double depth = 26 * sm((0.93 - dd) / 0.75);
        if (!terrace) return F - (int) Math.round(depth);
        double th = Math.atan2(z - MCZ, x - MCX), ref = 1.2 + 6.0 * dd;
        double dth = Math.atan2(Math.sin(th - ref), Math.cos(th - ref));
        if (Math.abs(dth) * dd * MA < 3.6) return F - (int) Math.round(depth);
        return F - 4 * (int) Math.round(depth / 4.0);
    }

    /** probabilite de minerai dans la roche autour de la mine, des galeries et des chambres */
    static double oreP(int x, int y, int z) {
        if (x < -510 || x > -40 || z < -1150 || z > -768 || y > F + 40) return 0;
        double p = 0;
        double dd = mdd(x, z);
        if (dd >= 0.97 && dd < 1.2) p = 0.05;
        else if (dd < 0.97 && y < F - 1) p = 0.03;
        for (Cham c : CHAM) {
            double d = Math.hypot(x - c.x, z - c.z);
            if (d < c.rad + 4 && Math.abs(y - c.lvl) < 12) p = Math.max(p, c.kind >= 2 ? 0.4 : 0.22);
        }
        for (Gal g : GAL) {
            int[][] pl = g.p;
            for (int i = 0; i + 1 < pl.length; i++) {
                double ax = pl[i][0], az = pl[i][1], dx = pl[i + 1][0] - ax, dz = pl[i + 1][1] - az, len2 = dx * dx + dz * dz;
                double t = Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / len2));
                double ex = ax + t * dx - x, ez = az + t * dz - z;
                if (Math.hypot(ex, ez) < 7.5 && Math.abs(y - (g.l0 + (g.l1 - g.l0) * t)) < 9) { p = Math.max(p, 0.06); break; }
            }
        }
        return p;
    }

    private static int mine(int x, int y, int z, int h) {
        if (x < -292 || x > -136 || z < -950 || z > -776 || y < F - 28 || y > F + 52) return KEEP;
        double dd = mdd(x, z);
        if (dd > 1.0) return KEEP;
        int fy = mineFloor(x, z, dd, true);
        int ceilM = Math.min(F + 50 - (int) Math.round(10 * dd * dd), h - 11);
        if (ceilM < fy + 16 || y < fy) return KEEP;
        if (y > ceilM) return KEEP;
        int dx = x - MCX, dz = z - MCZ, v = y - fy;
        boolean flat = fy != mineFloor(x, z, dd, false);        // true = terrasse (pas de rampe)
        if (y == fy) { double q = r01(x, 2, z); return q < 0.35 ? GRAVEL : q < 0.55 ? DCOBBLE : DEEPSLATE; }
        // le chevalement au centre
        int adx = Math.abs(dx), adz = Math.abs(dz);
        if (adx <= 6 && adz <= 6) {
            int base = F - 26;
            if (adx == 5 && adz == 5 && y > base && y <= F + 15) return OAKLOG;
            if (adx <= 5 && adz <= 5 && (y - base) % 9 == 0 && y > base && y <= F + 15) {
                if (adx == 5 && adz < 5) return OAKLOGZ;
                if (adz == 5 && adx < 5) return OAKLOGX;
                if (adx == adz && adx < 5) return OAKLOG;
            }
            if (y == F + 16 && adx <= 6 && adz <= 6) return OAKP;
            if (adx == 0 && adz == 0 && y >= F - 12 && y <= F + 15) return CHAIN;
            if (adx == 0 && adz == 0 && y == F - 13) return LANTERN;
            if (y == F + 17 && ((adx == 6 && adz % 2 == 0) || (adz == 6 && adx % 2 == 0))) return FENCE;
        }
        // rails sur les terrasses
        if (v == 1 && flat && Math.abs(dd - 0.55) < 0.012) return Math.abs(dz) > Math.abs(dx) ? RAILX : RAILZ;
        // lampadaires
        if (flat && Math.floorMod(x, 18) == 0 && Math.floorMod(z, 18) == 9) { int lp = lampPost(v, 4); if (lp != KEEP) return lp; }
        // echafaudages
        int s = scaffold(x, y, z, -262, -826);
        if (s != KEEP) return s;
        s = scaffold(x, y, z, -176, -912);
        if (s != KEEP) return s;
        s = scaffold(x, y, z, -176, -820);
        if (s != KEEP) return s;
        int pl = pile(x, y, z, fy, 13, 0.12, ORE);
        if (pl != KEEP && flat) return pl;
        if (Math.floorMod(x, 20) == 0 && Math.floorMod(z, 20) == 7 && r01(x, 9, z) > 0.3) {
            if (y >= ceilM - 9 && y <= ceilM) return CHAIN;
            if (y == ceilM - 10) return LANTERN;
        }
        if (y == ceilM && r01(x, 12, z) < 0.01) return WEB;
        return AIR;
    }

    private static int scaffold(int x, int y, int z, int px, int pz) {
        int ex = Math.abs(x - px), ez = Math.abs(z - pz);
        if (ex > 6 || ez > 6) return KEEP;
        int base = mineFloor(px, pz, mdd(px, pz), true), v = y - base;
        if (v > 34) return KEEP;
        if (ex == 6 && ez == 6) return OAKLOG;
        if (v < 1) return KEEP;
        if ((v == 12 || v == 24 || v == 34) && !(ex <= 1 && ez <= 1)) return OAKP;
        if ((v == 13 || v == 25 || v == 35) && (ex == 6 || ez == 6)) return FENCE;
        if ((ex == 3 && ez == 6 || ex == 6 && ez == 3) && v <= 11) return v % 4 == 3 ? OAKP : FENCE;
        if (ex == 0 && ez == 0 && v <= 34) return v % 2 == 0 ? OAKLOG : FENCE;
        return KEEP;
    }

    // ------------------------------------------------------------------ les galeries de la mine (niveau haut F, niveau bas F - 30)
    private static final int LB = F - 30;

    private record Gal(int l0, int l1, int[][] p) {}
    private record Cham(int x, int z, int lvl, int rad, int kind) {}

    private static final Gal[] GAL = {
            new Gal(F, F, new int[][]{{-289, -863}, {-330, -863}, {-380, -850}, {-430, -845}, {-470, -870}}),                   // 0 ouest
            new Gal(F, F, new int[][]{{-330, -863}, {-340, -900}, {-380, -930}, {-420, -950}}),                                 // 1 branche
            new Gal(F, F, new int[][]{{-266, -922}, {-300, -950}, {-320, -990}, {-360, -1030}, {-400, -1040}}),                 // 2 sud-ouest
            new Gal(F, F, new int[][]{{-214, -947}, {-214, -990}, {-214, -1040}, {-225, -1090}}),                               // 3 sud
            new Gal(F, F, new int[][]{{-189, -942}, {-175, -985}, {-150, -1030}}),                                              // 4 sud-est
            new Gal(F, F, new int[][]{{-278, -821}, {-320, -800}, {-360, -790}, {-400, -770}, {-440, -770}}),                   // 5 nord-ouest
            new Gal(F, F, new int[][]{{-380, -850}, {-370, -820}, {-360, -790}}),                                               // 6 liaison
            new Gal(F, F, new int[][]{{-320, -990}, {-270, -1010}, {-214, -1010}}),                                             // 7 liaison sud
            new Gal(F, LB, new int[][]{{-210, -990}, {-180, -990}}),                                                            // 8 la descente (escalier)
            new Gal(LB, LB, new int[][]{{-180, -990}, {-150, -990}, {-120, -1010}, {-90, -1040}, {-60, -1050}}),                // 9 niveau bas
            new Gal(LB, LB, new int[][]{{-150, -990}, {-170, -1030}, {-190, -1080}}),                                           // 10
            new Gal(LB, LB, new int[][]{{-90, -1040}, {-100, -1090}, {-140, -1100}}),                                           // 11 (inondee)
    };

    private static final Cham[] CHAM = {
            new Cham(-214, -990, F, 11, 0),         // la salle du treuil
            new Cham(-380, -850, F, 9, 0),          // croisement
            new Cham(-470, -870, F, 12, 1),         // fond de la galerie ouest
            new Cham(-420, -950, F, 10, 1),
            new Cham(-400, -1040, F, 11, 1),
            new Cham(-440, -770, F, 10, 1),
            new Cham(-225, -1090, F, 12, 1),
            new Cham(-150, -1030, F, 9, 1),
            new Cham(-60, -1050, LB, 11, 1),
            new Cham(-190, -1088, LB, 13, 2),       // la grande veine
            new Cham(-140, -1100, LB, 11, 3),       // la fissure de lave
    };

    private static int gal(int x, int y, int z, int h) {
        if (x < -500 || x > -40 || z < -1140 || z > -768 || y < LB - 3 || y > F + 14) return KEEP;
        if (y > h - 9) return KEEP;
        int r;
        for (Cham c : CHAM) if ((r = chamber(x, y, z, c)) != KEEP) return r;
        for (int gi = 0; gi < GAL.length; gi++) {
            Gal g = GAL[gi];
            int[][] p = g.p;
            double s0 = 0;
            for (int i = 0; i + 1 < p.length; i++) {
                double ax = p[i][0], az = p[i][1], bx = p[i + 1][0], bz = p[i + 1][1];
                double dx = bx - ax, dz = bz - az, len = Math.hypot(dx, dz);
                double t = ((x - ax) * dx + (z - az) * dz) / (len * len);
                double tc = Math.max(0, Math.min(1, t));
                double ex = ax + tc * dx - x, ez = az + tc * dz - z;
                double d = Math.hypot(ex, ez) + 0.35 * (vn(x / 6.0, z / 6.0, 70) - 0.5) * 2;
                if (d <= 3.4 && t >= 0 && t <= 1) {
                    boolean incl = g.l0 != g.l1;
                    int L = incl ? g.l0 + (int) Math.round((g.l1 - g.l0) * tc) : g.l0;
                    boolean alongX = Math.abs(dx) > Math.abs(dz);
                    int sd = 3;
                    if (incl) { double ax2 = -dx, az2 = -dz; sd = Math.abs(ax2) > Math.abs(az2) ? (ax2 > 0 ? 1 : 3) : (az2 > 0 ? 2 : 0); }
                    double s = s0 + tc * len;
                    r = galCell(x, y, z, d, s, L, alongX, incl, sd, gi == 11 && s > 18 && s < 55);
                    if (r != KEEP) return r;
                }
                s0 += len;
            }
        }
        return KEEP;
    }

    private static int galCell(int x, int y, int z, double d, double s, int L, boolean alongX, boolean incl, int stairDir, boolean flooded) {
        int v = y - L;
        boolean frame = Math.floorMod((int) Math.floor(s), 6) == 0;
        if (v == 0) {
            if (d > 3.2) return KEEP;
            if (incl) return DSTAIR + stairDir;
            if (d < 0.6) return DCOBBLE;
            double q = r01(x, 2, z);
            return q < 0.4 ? GRAVEL : q < 0.6 ? DCOBBLE : DEEPSLATE;
        }
        if (v < 0) return KEEP;
        if (v == 7) return frame && d <= 3.4 ? (alongX ? OAKLOGZ : OAKLOGX) : KEEP;
        if (v > 7 || d > 3.2) return KEEP;
        if (frame && d >= 2.3) return OAKLOG;
        if (flooded && v <= 2) return WATER;
        if (v == 1 && !incl && d < 0.6 && !flooded) return alongX ? RAILX : RAILZ;
        if (v == 6 && frame && d < 0.8 && Math.floorMod((int) Math.floor(s / 6), 2) == 0) return LANTERN;
        if (v >= 5 && r01(x, y, z) < 0.01) return WEB;
        if (v <= 2 && d > 1.8 && r01(x, y, z) < 0.03) return r01(x, 4, z) < 0.5 ? DCOBBLE : GRAVEL;
        return AIR;
    }

    private static int chamber(int x, int y, int z, Cham c) {
        int dx = x - c.x, dz = z - c.z;
        if (Math.abs(dx) > c.rad + 3 || Math.abs(dz) > c.rad + 3 || y < c.lvl - 2 || y > c.lvl + 16) return KEEP;
        double dd = Math.hypot(dx, dz) + 0.8 * (vn(x / 4.0, z / 4.0, 97) - 0.5);
        if (dd > c.rad) return KEEP;
        int v = y - c.lvl;
        if (c.kind == 3 && v == -1 && dd < 3.8) return MAGMA;
        if (v < 0) return KEEP;
        int ceilH = 5 + (int) Math.round((c.rad - dd) * 0.45);
        if (v > ceilH) return KEEP;
        int ad = Math.abs(dx), az = Math.abs(dz);
        if (v == 0) {
            if (c.kind == 3 && dd < 3.4) return LAVA;
            double q = r01(x, 2, z);
            return q < 0.4 ? GRAVEL : q < 0.6 ? DCOBBLE : DEEPSLATE;
        }
        switch (c.kind) {
            case 0: {                                                                        // le treuil
                if (ad == 3 && az == 3 && v <= 9) return OAKLOG;
                if (v == 9 && ad <= 3 && az <= 3 && (ad == 3 || az == 3)) return ad == 3 ? OAKLOGZ : OAKLOGX;
                if (ad == 0 && az == 0 && v >= 4 && v <= 8) return CHAIN;
                if (ad == 0 && az == 0 && v == 3) return CAULDRON;
                if (v <= 4 && ((ad == c.rad - 2 && az == 0) || (az == c.rad - 2 && ad == 0))) return lampPost(v, 4);
                if (v == 1 && dd > c.rad - 3 && r01(x, 3, z) < 0.08) return BARREL;
                break;
            }
            case 1: {
                if (v <= 3 && dd > 3 && r01(x, y, z) < 0.03) return ORE + hsh(x >> 2, 3, z >> 2) % 5;
                if (ad == 0 && az == 0 && v >= ceilH - 4) return CHAIN;
                if (ad == 0 && az == 0 && v == ceilH - 5) return LANTERN;
                break;
            }
            case 2: {
                if (v <= 4 && dd > 2 && r01(x, y, z) < 0.05) return ORE + 3;
                if ((ad == 5 || az == 5) && ad <= 5 && az <= 5 && v == ceilH - 1 && (ad + az) % 2 == 0) return LANTERN;
                break;
            }
            default: {
                if (v <= 2 && dd >= 3.4 && dd < 4.6 && r01(x, y, z) < 0.5) return BLACKSTONE;
                break;
            }
        }
        if (v >= ceilH - 1 && r01(x, y, z) < 0.01) return WEB;
        return AIR;
    }
}
