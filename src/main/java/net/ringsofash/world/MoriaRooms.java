package net.ringsofash.world;

import static net.ringsofash.world.MoriaPlan.*;

/**
 * Les salles ajoutees aux mines de la Moria, toutes au niveau des Forges (F2) : la Grande Rue a piliers, le Quartier des nains (maisons, place, maison du roi),
 * la salle des festins, trois salles a piliers (armurerie, archives, crypte), la salle du trone, le tresor scelle (montagne d'or) et l'enorme mine a ciel
 * ouvert avec sa rampe en spirale. Meme principe que MoriaPlan : fonction pure de (x, y, z, hauteur du terrain), KEEP = ne rien changer.
 */
final class MoriaRooms {
    private MoriaRooms() {}

    private static final int F = F2;

    static final int AVE = 0, VILL = 1, DINE = 2, PH = 3, THRONE = 4, TREAS = 5;

    private record Room(int x0, int x1, int z0, int z1, int hgt, int kind, int variant) {}

    private static final Room[] R = {
            new Room(-262, -120, -748, -712, 36, AVE, 0),
            new Room(-262, -120, -700, -560, 44, VILL, 0),
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
        return mine(x, y, z, h);
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

    private static int room(int x, int y, int z, int h, Room r) {
        if (x < r.x0 - 1 || x > r.x1 + 1 || z < r.z0 - 1 || z > r.z1 + 1 || y < F || y > F + r.hgt + 6) return KEEP;
        int ceil = Math.min(F + r.hgt + (int) Math.round(3 * domes(r, x, z)), h - 11);
        if (ceil < F + 14) return KEEP;
        boolean in = x >= r.x0 && x <= r.x1 && z >= r.z0 && z <= r.z1;
        if (!in) {
            if (y > ceil + 1) return KEEP;
            boolean xw = x < r.x0 || x > r.x1, zw = z < r.z0 || z > r.z1;
            if (xw && zw) return KEEP;
            int m = Math.floorMod(xw ? z - r.z0 : x - r.x0, pitch(r));
            boolean band = (y - F) % 12 == 11;
            if (y == F + 8 && m == 0) return DLAMP;
            if (m <= 1) return band ? DCHISEL : DPILLAR;
            return band ? DCHISEL : DBRICKS;
        }
        if (y == F) return floorRoom(r, x, z);
        if (y == ceil + 1) return vaultTop(x, z);
        if (y > ceil) return KEEP;
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
        int q = hangingLamp(x, y, z, ceil, 16);
        if (q != KEEP) return q;
        if (y == ceil && r01(x, 12, z) < 0.012) return WEB;
        return AIR;
    }

    private static int vaultTop(int x, int z) { return r01(x, 33, z) < 0.45 ? DSTONE : DBRICKS; }

    private static int floorRoom(Room r, int x, int z) {
        int cx = (r.x0 + r.x1) / 2;
        if (r.kind == THRONE && Math.abs(x - 140) == 6 && z > -918) return DCHISEL;
        if (r.kind == VILL) {
            double d = Math.hypot(x + 192, z + 630);
            if (d <= 4.5) return DBRICKS;
            if (d <= 8 && (Math.floorMod(x + z, 4) == 0)) return DCHISEL;
        }
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
        if (v == 1 && Math.abs(z - zc) <= 1) return RCARPET;
        if (v == 1 && Math.abs(z - zc) == 5 && Math.floorMod(x, 14) < 3) return BSLAB;
        if (v == 1 && (Math.abs(z - zc) == 14 || Math.abs(z - zc) == 15) && Math.floorMod(x, 14) == 7) return BARREL;
        if (z == r.z0 || z == r.z1) {
            int b = banner(x, r.x0 + 21 + 14 * Math.floorDiv(x - r.x0 - 14, 14), y, F + 20, F + 7);
            if (b != KEEP && Math.abs(Math.floorMod(x - r.x0, 14)) <= 7 + 7) return b;
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ le Quartier des nains
    private static int village(Room r, int x, int y, int z, int ceil) {
        int v = y - F;
        int k = kingsHouse(x, y, z);
        if (k != KEEP) return k;
        // la place et sa fontaine
        double d = Math.hypot(x + 192, z + 630);
        if (d <= 5.6) {
            if (d <= 1.3) return v <= 8 ? DPILLAR : v == 9 ? DLAMP : KEEP;
            if (d >= 4.4) return v <= 2 ? DBRICKS : KEEP;
            return v == 1 ? WATER : KEEP;
        }
        int cxi = Math.floorDiv(x - r.x0, 20), czi = Math.floorDiv(z - r.z0, 20);
        if (cxi >= 0 && cxi <= 6 && czi >= 0 && czi <= 6 && !(cxi <= 1 && czi >= 4) && !(cxi == 3 && czi == 3) && r01(cxi, 41, czi) < 0.84) {
            int hv = house(r, x, y, z, cxi, czi);
            if (hv != KEEP) return hv;
        }
        if (Math.floorMod(x - r.x0, 20) == 0 && Math.floorMod(z - r.z0, 20) == 0) {      // lampadaires aux carrefours
            int lp = lampPost(v, 5);
            if (lp != KEEP) return lp;
        }
        return KEEP;
    }

    private static int house(Room r, int x, int y, int z, int cxi, int czi) {
        int bx0 = r.x0 + 20 * cxi + 4, bx1 = bx0 + 11, bz0 = r.z0 + 20 * czi + 5, bz1 = bz0 + 9;
        if (x < bx0 || x > bx1 || z < bz0 || z > bz1) return KEEP;
        int v = y - F, xl = x - bx0, zl = z - bz0;
        boolean xe = xl == 0 || xl == 11, ze = zl == 0 || zl == 9;
        int k = Math.min(zl, 9 - zl), roofV = 6 + k;
        if (v >= 6) {
            if (v > roofV) return KEEP;
            if (v == roofV) return k == 4 ? DSTONE : DSTAIR + (zl < 5 ? 2 : 0);
            if (v == 6 && xl >= 3 && xl <= 8 && ze) return DBRICKS;
            return xe ? DBRICKS : (k == 4 && xl == 5 && zl == 4 ? CHAIN : AIR);
        }
        if (xe || ze) {
            if (xe && ze) return DPILLAR;
            if (zl == 0 && (xl == 5 || xl == 6) && v <= 3) return AIR;                       // porte
            if (v == 3 && ((ze && (xl == 2 || xl == 9)) || (xe && (zl == 4 || zl == 5)))) return BARS;
            if (v == 5) return DCHISEL;
            return DBRICKS;
        }
        if (v == 5 && xl == 5 && zl == 4) return LANTERN;
        int var = hsh(cxi, 43, czi) % 4;
        switch (var) {
            case 0:                                                                           // chambre
                if (v == 1 && xl <= 2 && zl >= 6) return xl == 1 ? BLACKWOOL : REDWOOL;
                if (v <= 2 && zl == 8 && xl >= 6 && xl <= 9) return BOOKS;
                if (v == 1 && xl == 7 && zl == 2) return FENCE;
                if (v == 2 && xl >= 6 && xl <= 8 && zl == 2) return BSLAB;
                break;
            case 1:                                                                           // forge
                if (v == 1 && xl == 9 && zl == 2) return FORGE;
                if (v == 1 && xl == 7 && zl == 2) return ANVIL;
                if (v <= 2 && ((xl == 1 || xl == 2) && zl == 8) && !(v == 2 && xl == 2)) return BARREL;
                break;
            case 2:                                                                           // cuisine
                if (v == 1 && xl == 9 && zl == 8) return FURNACE;
                if (v == 1 && xl == 8 && zl == 8) return CAULDRON;
                if (v == 1 && xl == 5 && zl == 7) return CAMPFIRE;
                if (v <= 2 && xl == 1 && (zl == 7 || zl == 8) && !(v == 2 && zl == 7)) return BARREL;
                if (v == 1 && xl == 4 && zl == 3) return FENCE;
                if (v == 2 && xl >= 3 && xl <= 5 && zl == 3) return BSLAB;
                break;
            default:                                                                          // etude
                if (v <= 2 && zl == 8 && xl >= 1 && xl <= 10) return BOOKS;
                if (v == 1 && xl == 9 && zl == 2) return CRAFT;
                if (v == 1 && xl == 4 && zl == 3) return FENCE;
                if (v == 2 && xl >= 3 && xl <= 5 && zl == 3) return BSLAB;
        }
        return AIR;
    }

    private static int kingsHouse(int x, int y, int z) {
        final int X0 = -258, X1 = -226, Z0 = -616, Z1 = -566, WH = 11;
        if (x < X0 - 1 || x > X1 + 1 || z < Z0 - 1 || z > Z1 + 1) return KEEP;
        int v = y - F, xl = x - X0, zl = z - Z0;
        boolean inb = x >= X0 && x <= X1 && z >= Z0 && z <= Z1;
        // tourelles d'angle
        int cxl = Math.min(xl, 32 - xl), czl = Math.min(zl, 50 - zl);
        if (cxl >= -1 && cxl <= 1 && czl >= -1 && czl <= 1 && (cxl <= 0 || czl <= 0 || true)) {
            if (v >= 1 && v <= WH + 8) return (cxl == 1 && czl == 1) ? KEEP : DPILLAR;
            if (v == WH + 9) return DLAMP;
        }
        if (!inb) return KEEP;
        boolean xe = xl == 0 || xl == 32, ze = zl == 0 || zl == 50;
        if (v == WH + 1) return DSTONE;
        if (v == WH + 2) return (xe || ze) ? ((xl + zl) % 2 == 0 ? BWALL : KEEP) : KEEP;
        if (v > WH + 2) return KEEP;
        if (xe || ze) {
            if (xl == 32 && zl >= 13 && zl <= 19 && v <= 7) return AIR;                       // grande porte, a l'est
            if (xl == 32 && zl >= 12 && zl <= 20 && v <= 8 && (zl == 12 || zl == 20 || v == 8)) return DCHISEL;
            if (v >= 5 && v <= 7 && ((xe && zl % 8 == 4) || (ze && xl % 8 == 4))) return BARS;
            if (v == WH) return DCHISEL;
            if ((xe && zl % 10 == 0) || (ze && xl % 8 == 0)) return DPILLAR;
            return DBRICKS;
        }
        // interieur : la salle du roi
        int dzc = zl - 25;
        if (xl <= 6 && Math.abs(dzc) <= 6 && v == 1) return DBRICKS;                          // estrade
        if (xl == 3 && dzc == 0 && v == 2) return DSTAIR + 3;                                 // le fauteuil du roi
        if (xl <= 2 && Math.abs(dzc) <= 2 && v >= 2 && v <= 8 - Math.abs(dzc) * 2) return Math.abs(dzc) == 2 ? GOLD : DCHISEL;
        if (xl == 3 && Math.abs(dzc) == 2 && v == 2) return DSTONE;
        if (v == 1 && xl >= 7 && Math.abs(dzc) <= 2) return RCARPET;
        if (v == 1 && xl >= 7 && Math.abs(dzc) == 3 && Math.floorMod(xl, 6) == 1) return BARREL;
        int px = xl, pz = zl;
        if ((px == 10 || px == 18 || px == 26) && (pz == 12 || pz == 38)) return v <= WH ? DPILLAR : KEEP;
        if (zl == 1 && v <= 3 && xl >= 9 && xl <= 29) return BOOKS;
        if (zl == 49 && v <= 3 && xl >= 9 && xl <= 20) return BOOKS;
        if (v == 1 && xl >= 24 && xl <= 27 && zl >= 44 && zl <= 48) return xl == 24 ? BLACKWOOL : REDWOOL;      // le lit du roi
        if (v == 1 && xl >= 12 && xl <= 24 && (zl == 40 || zl == 10)) return FENCE;
        if (v == 2 && xl >= 12 && xl <= 24 && (zl == 40 || zl == 10)) return BSLAB;
        if (xl % 8 == 4 && (zl == 13 || zl == 25 || zl == 37) && v >= WH - 3 && v < WH) return CHAIN;
        if (xl % 8 == 4 && (zl == 13 || zl == 25 || zl == 37) && v == WH - 4) return LANTERN;
        return AIR;
    }

    // ------------------------------------------------------------------ la salle des festins
    private static final int[] ROWS = {-515, -501, -481, -467};

    private static int dining(Room r, int x, int y, int z, int ceil) {
        int zc = -491, v = y - F;
        int ix = Math.round((x - (r.x0 + 12)) / 24f);
        if (ix >= 0 && ix <= 5) {
            int p = pillar(x, y, z, r.x0 + 12 + 24 * ix, zc, ceil, 3, false, ceil);
            if (p != KEEP) return p;
        }
        // les tables et les bancs
        if (x >= -250 && x <= -134) {
            for (int zt : ROWS) {
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
                if (v == 0) return KEEP;
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
        int ix = Math.floorDiv(x - cx - 0, 12), iz = Math.floorDiv(z - cz - 0, 12);
        int px = cx + 6 + 12 * ix, pz = cz + 6 + 12 * iz;
        // le pilier le plus proche (la cellule contient (x,z) de cx+12*ix a cx+12*ix+11 : le pilier est a +6)
        if (ix >= -3 && ix <= 2 && iz >= -3 && iz <= 2) {
            boolean broken = r01(px, 4, pz) < 0.14;
            int top = broken ? F + 8 + (int) (r01(px, 5, pz) * 20) : ceil;
            int p = pillar(x, y, z, px, pz, ceil, 2, broken, top);
            if (p != KEEP) return p;
            if (broken && Math.max(Math.abs(x - px), Math.abs(z - pz)) <= 5 && v <= 3 - Math.max(Math.abs(x - px), Math.abs(z - pz)) / 2 + (int) (r01(x, y, z) * 1.5))
                return r01(x, y, z) < 0.5 ? DCOBBLE : DSTONE;
        }
        int dx = x - cx, dz = z - cz;
        switch (r.variant) {
            case 1: {                                                                        // armurerie : ratelier, enclumes, tonneaux
                if (v == 1 && Math.abs(dx) >= 18 && Math.abs(dx) <= 19 && Math.abs(dz) <= 24 && Math.floorMod(dz, 8) == 0) return ANVIL;
                if ((z == r.z0 || z == r.z1) && v >= 1 && v <= 3 && Math.floorMod(x, 6) < 4) return v == 2 ? BARS : (v == 1 ? FENCE : OAKP);
                if (v <= 2 && (x == r.x0 || x == r.x1) && Math.floorMod(z, 5) < 3) return v == 2 ? BARS : OAKP;
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
        int s = statue(x, y, z, cx - 32, -938, F, 1);
        if (s != KEEP) return s;
        s = statue(x, y, z, cx + 32, -938, F, 1);
        if (s != KEEP) return s;
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
        if ((x == r.x0 || x == r.x1) && y > F + 6) {
            int b = banner(z, -870, y, F + 30, F + 12);
            if (b == KEEP) b = banner(z, -900, y, F + 30, F + 12);
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

    static double oreP(int x, int y, int z) {
        if (x < -300 || x > -128 || z < -960 || z > -770 || y > F + 40) return 0;
        double dd = mdd(x, z);
        if (dd >= 0.97 && dd < 1.2) return 0.05;
        if (dd < 0.97 && y < F - 1) return 0.03;
        return 0;
    }

    private static int mine(int x, int y, int z, int h) {
        if (x < -292 || x > -136 || z < -950 || z > -776 || y < F - 28 || y > F + 40) return KEEP;
        double dd = mdd(x, z);
        if (dd > 1.0) return KEEP;
        int fy = mineFloor(x, z, dd, true);
        int ceilM = Math.min(F + 38 - (int) Math.round(14 * dd * dd), h - 11);
        if (ceilM < fy + 16 || y < fy) return KEEP;
        if (y > ceilM) return KEEP;
        int dx = x - MCX, dz = z - MCZ, v = y - fy;
        boolean flat = fy == mineFloor(x, z, dd, false) ? false : true;     // true = terrasse (pas de rampe)
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
        if (ex == 6 && ez == 6) return v <= 34 ? OAKLOG : KEEP;
        if ((v == 12 || v == 24 || v == 34) && !(ex <= 1 && ez <= 1)) return OAKP;
        if ((v == 13 || v == 25 || v == 35) && (ex == 6 || ez == 6)) return FENCE;
        if ((ex == 3 && ez == 6 || ex == 6 && ez == 3) && v <= 11) return v % 4 == 3 ? OAKP : FENCE;
        if (ex == 0 && ez == 0 && v <= 34) return v % 2 == 0 ? OAKLOG : FENCE;
        return KEEP;
    }
}
