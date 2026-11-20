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

    // ------------------------------------------------------------------ statues d'ornement
    /** colosse (52 de haut, MoriaPlan.statue) tourne : dir 0 = face vers +z, 1 = -z, 2 = +x, 3 = -x */
    static int colossus(int x, int y, int z, int cx, int cz, int base, int dir) {
        if (Math.abs(x - cx) > 13 || Math.abs(z - cz) > 13) return KEEP;
        switch (dir) {
            case 0: return statue(x, y, z, cx, cz, base, 1);
            case 1: return statue(x, y, z, cx, cz, base, -1);
            case 2: return statue(cx + (z - cz), y, cz + (x - cx), cx, cz, base, 1);
            default: return statue(cx + (z - cz), y, cz - (x - cx), cx, cz, base, 1);
        }
    }

    /** gardien (30 de haut, hache a la main), meme orientation */
    static int guard(int x, int y, int z, int cx, int cz, int base, int dir) {
        int dx = x - cx, dz = z - cz;
        if (Math.abs(dx) > 10 || Math.abs(dz) > 10) return KEEP;
        int l, f;
        switch (dir) {
            case 0: l = dx; f = dz; break;
            case 1: l = dx; f = -dz; break;
            case 2: l = dz; f = dx; break;
            default: l = dz; f = -dx; break;
        }
        return guardCore(l, f, y - base);
    }

    private static int guardCore(int l, int f, int v) {
        int al = Math.abs(l), af = Math.abs(f);
        if (v < 0 || v > 30 || al > 9 || af > 4) return KEEP;
        if (v <= 2) return al <= 4 && af <= 4 ? (v == 2 ? DCHISEL : DBRICKS) : KEEP;
        if (l == 7 && f == 0 && v >= 3 && v <= 24) return DPILLAR;                                        // le manche de la hache
        if (l >= 6 && l <= 9 && af <= 1 && v >= 21 && v <= 24 && !(l == 9 && (v == 21 || v == 24))) return DCHISEL;       // le fer
        if (v <= 10) return al >= 1 && al <= 3 && af <= 2 ? DSTONE : KEEP;
        if (v == 11) return al <= 4 && af <= 2 ? DCHISEL : KEEP;
        if (v <= 19) {
            if (al <= 4 && af <= 2) return (v >= 14 && v <= 17 && f == 2) ? DCHISEL : DSTONE;
            if (al >= 5 && al <= 6 && af <= 1) return DBRICKS;
            return KEEP;
        }
        if (v <= 21) return al <= 5 && af <= 2 ? DSTONE : KEEP;
        if (v == 22) return al <= 1 && af <= 1 ? DSTONE : KEEP;
        if (v <= 26) {
            if (al <= 2 && af <= 2) {
                if (f == 2 && v == 25 && al == 1) return DRUNES;
                if (f >= 1 && v <= 24 && al <= 1) return DCOBBLE;
                return DSTONE;
            }
            return KEEP;
        }
        if (v <= 28) return al <= 3 && af <= 3 ? DBRICKS : KEEP;
        return v == 29 && al <= 1 && af <= 1 ? DCHISEL : (v == 30 && al == 0 && af == 0 ? DCHISEL : KEEP);
    }

    // ------------------------------------------------------------------ bas-reliefs geants (guerrier nain, visage sculpte)
    private static final String[] WARRIOR = {
            "....###....",
            "...#####...",
            "..#######..",
            "..##o#o##..",
            "..#######..",
            "...#####...",
            "..#######..",
            ".#########.",
            ".#########.",
            "###.###.###",
            "#.#######.#",
            "#.#######.#",
            "#.##ooo##.#",
            "#.#######.#",
            "..#######..",
            "..###.###..",
            "..###.###..",
            "..###.###..",
            "..###.###..",
            "..###.###..",
            "..###.###..",
            ".####.####.",
            ".####.####.",
            "#####.#####",
    };
    private static final String[] FACE = {
            "..#########..",
            ".###########.",
            "#############",
            "##ooo###ooo##",
            "##o#o###o#o##",
            "#############",
            "######o######",
            "######o######",
            "#####ooo#####",
            "#############",
            "###ooooooo###",
            "#############",
            ".###########.",
            "..#########..",
            "...#######...",
            "...#######...",
            "....#####....",
            "....#####....",
            ".....###.....",
            ".....###.....",
            "......#......",
    };

    /** a = coordonnee le long du mur, a0/pitch = les panneaux (centres a a0 + pitch/2 + k * pitch), dep = profondeur depuis la face du mur, v = hauteur au dessus du sol. */
    private static int relief(int a, int a0, int pitch, int v, int dep, int scale, int salt) {
        int idx = Math.floorDiv(a - a0, pitch);
        if ((hsh(idx, salt, 5) & 3) == 0) return KEEP;
        String[] m = (hsh(idx, salt, 7) & 1) == 0 ? WARRIOR : FACE;
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

    /** Grande Salle (F0) et Forges (F2) : grands bas-reliefs sur les murs interieurs */
    private static int bigRelief(int x, int y, int z, int h) {
        // Grande Salle : x [-12, 138], z [-472, -248], sol F0
        if (y > F0 + 3 && y < F0 + 56 && x >= -12 && x <= 138 && z >= -472 && z <= -248) {
            if (h - 11 < F0 + 54) return KEEP;
            int dW = x + 12, dE = 138 - x, dN = z + 472, dS = -248 - z;
            int m = Math.min(Math.min(dW, dE), Math.min(dN, dS));
            if (m < 2) {
                int v = y - F0;
                if (m == dN && x >= 44 && x <= 106) return KEEP;                 // le haut de l'escalier
                if (m == dE && Math.abs(z + 360) <= 14) return KEEP;             // le couloir de la porte
                if (m == dN || m == dS) return relief(x, -6, 56, v, m, 2, 21);
                return relief(z, -360, 56, v, m, 2, 23);
            }
            return KEEP;
        }
        // Forges : x [-110, 250], z [-830, -688], sol F2
        if (y > F + 3 && y < F + 56 && x >= -110 && x <= 250 && z >= -830 && z <= -688) {
            int dW = x + 110, dE = 250 - x, dN = z + 830, dS = -688 - z;
            int m = Math.min(Math.min(dW, dE), Math.min(dN, dS));
            if (m < 2) {
                int v = y - F;
                if (m == dN || m == dS) return relief(x, -16, 64, v, m, 2, 25);
                return relief(z, -16, 64, v, m, 2, 27);
            }
        }
        return KEEP;
    }

    // ------------------------------------------------------------------ les fours geants des Forges
    private static final int[] FN = {-98, -50, 64, 160, 222};     // contre le mur nord (z = -688)
    private static final int[] FS = {-100, -40, 100, 180, 240};   // contre le mur sud (z = -830)

    private static int furnace(int x, int y, int z, int h) {
        if (y < F || y > F + 75) return KEEP;
        int[] cs;
        int f;
        if (z >= -711 && z <= -688) { cs = FN; f = -688 - z; }
        else if (z >= -830 && z <= -807) { cs = FS; f = z + 830; }
        else return KEEP;
        int cx = Integer.MIN_VALUE;
        for (int c : cs) if (Math.abs(x - c) <= 9) { cx = c; break; }
        if (cx == Integer.MIN_VALUE) return KEEP;
        int ceil = Math.min(F + 72, h - 11);
        if (ceil < F + 40) return KEEP;
        int u = x - cx, au = Math.abs(u), v = y - F;
        if (f >= 20) {                                                   // le tablier devant le four
            if (v == 1 && au == 6 && f == 20) return ANVIL;
            if (v == 1 && au == 8 && f == 21) return BARREL;
            return KEEP;
        }
        int mouthTop = au <= 1 ? 9 : au == 2 ? 8 : au == 3 ? 6 : -1;
        // le creux (chambre de chauffe)
        boolean chamber = f >= 4 && f <= 19 && v >= 1 && v <= 10 && (f >= 15 ? v <= mouthTop : au <= 4);
        if (chamber) {
            if (v == 1) {
                if (f >= 6 && f <= 13 && au <= 3) return LAVA;
                if (f >= 14) return BSLAB;
            }
            return AIR;
        }
        if (v == 0) return f >= 6 && f <= 13 && au <= 3 ? MAGMA : KEEP;
        // la cheminee
        if (v >= 11) {
            if (v > ceil) return KEEP;
            if (au <= 1 && f >= 6 && f <= 10) return AIR;
            if (v <= 14 && au <= 7 && f <= 19) {
                if (f == 19) return (v == 11 || v == 14) ? DCHISEL : (v == 12 && au <= 6 && (au + 1) % 3 == 0 ? DRUNES : DBRICKS);
                return DBRICKS;
            }
            if (au <= 3 && f >= 4 && f <= 12) return (v - F) % 12 == 11 ? DCHISEL : DBRICKS;
            return KEEP;
        }
        int hw = v <= 9 ? 8 : 7;
        if (au > hw) return KEEP;
        if (f == 19) {                                                   // la facade
            if (au >= 7 && au <= 8) return DPILLAR;
            if (v == 10) return DCHISEL;
            if (au == 6 && v == 3) return DLAMP;
            if (mouthTop >= 0 && (v == mouthTop + 1 || v == mouthTop + 2 || au == 4 || (au == 5 && v <= 6))) return DCHISEL;
            if (v >= 5 && v <= 9 && au >= 4 && au <= 6 && (au + v) % 3 == 0) return DRUNES;
            return DBRICKS;
        }
        if (au == hw) return v % 6 == 5 ? DCHISEL : DBRICKS;
        return DBRICKS;
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

    private static int room(int x, int y, int z, int h, Room r) {
        if (x < r.x0 - 3 || x > r.x1 + 3 || z < r.z0 - 3 || z > r.z1 + 3 || y < (r.kind == VILL ? F - 50 : F) || y > F + r.hgt + 6) return KEEP;
        int ceil = Math.min(F + r.hgt + (int) Math.round(3 * domes(r, x, z)), h - 11);
        if (ceil < F + 14) return KEEP;
        boolean in = x >= r.x0 && x <= r.x1 && z >= r.z0 && z <= r.z1;
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
            // niches en ogive avec leur gardien de pierre (Grande Rue, festins, salles a piliers)
            if (layer <= 2 && (r.kind == PH || r.kind == AVE || r.kind == DINE) && (r.kind == PH || (Math.floorDiv(a, p) & 1) == 1)) {
                int off = Math.abs(m - p / 2);
                int hN = nicheH(off);
                boolean nearCorner = xw ? (z < r.z0 + 4 || z > r.z1 - 4) : (x < r.x0 + 4 || x > r.x1 - 4);
                if (hN >= 0 && vy >= 1 && vy <= hN && !nearCorner) {
                    if (layer == 2) {                                              // la statuette
                        if (off <= 1 && vy <= 3) return DSTONE;
                        if (off <= 1 && vy <= 6) return off == 0 ? DCHISEL : DSTONE;
                        if (off == 0 && vy <= 8) return vy == 7 ? DRUNES : DPILLAR;
                        return AIR;
                    }
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
        if (y == ceil + 1) return vaultTop(x, z);
        if (y > ceil) return KEEP;
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
        int q = hangingLamp(x, y, z, ceil, 16);
        if (q != KEEP) return q;
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
                int g = guard(x, y, z, gx, r.z0 + 3, F, 0);
                if (g != KEEP) return g;
                g = guard(x, y, z, gx, r.z1 - 3, F, 1);
                if (g != KEEP) return g;
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
    private static final int VZ = -640;
    private static final int[] LANES = {-245, -212, -177, -145};
    private static final int[] HX = {-258, -242, -226, -190, -174, -158};
    /** zLo, zHi, direction de la porte (+1 : cote zHi), decalage de niveau */
    private static final int[][] HROWS = {
            {-696, -687, 1, 0}, {-679, -670, 1, -9}, {-664, -655, 1, -18},
            {-625, -616, -1, -18}, {-611, -602, -1, -9}, {-597, -588, -1, 0}, {-583, -574, -1, 0},
    };

    private static boolean ravine(int x, int z) {
        double a = (x + 199) / 57.0, b = (z - VZ) / 12.5;
        return a * a + b * b <= 1;
    }

    /** altitude du sol du quartier (corniches au bord, terrasses descendantes, ravine au centre) */
    static int vFloor(int x, int z) {
        if (ravine(x, z)) return F - 48;
        int d = Math.abs(z - VZ);
        if (x >= -138 && x <= -120 && Math.abs(z + 631) <= 6) return F - Math.min(18, -120 - x);
        for (int xs : LANES) {
            if (Math.abs(x - xs) <= 2) {
                if (d >= 40) return F;
                if (d >= 31) return F - (40 - d);
                if (d >= 26) return F - 9;
                if (d >= 17) return F - 9 - (26 - d);
                return F - 18;
            }
        }
        return d >= 40 ? F : d >= 26 ? F - 9 : F - 18;
    }

    /** direction (0 N, 1 E, 2 S, 3 O) de montee si la case est une marche d'escalier, sinon -1 */
    private static int stairFacing(int x, int z) {
        if (ravine(x, z)) return -1;
        if (x >= -138 && x <= -121 && Math.abs(z + 631) <= 6) return 1;
        int d = Math.abs(z - VZ);
        for (int xs : LANES) {
            if (Math.abs(x - xs) <= 2 && ((d >= 31 && d < 40) || (d >= 17 && d < 26))) return z < VZ ? 0 : 2;
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

    private static int mason(int x, int y, int z) {
        if (y > minNeighbour(x, z)) return (y - F) % 9 == 0 || r01(x, y, z) < 0.07 ? DCHISEL : DBRICKS;
        return KEEP;
    }

    private static int village(Room r, int x, int y, int z, int ceil) {
        int v = y - F;
        int fy = vFloor(x, z);
        // la maison du roi, au nord
        int k = kingsHouse(x, y, z);
        if (k != KEEP) return k;
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
        // les statues geantes sur les corniches
        int s = colossus(x, y, z, -172, -572, F, 1);
        if (s != KEEP) return s;
        s = colossus(x, y, z, -134, -688, F, 0);
        if (s != KEEP) return s;
        // les maisons
        for (int[] row : HROWS) {
            if (z < row[0] - 2 || z > row[1] + 2) continue;
            int base = F + row[3];
            for (int hx : HX) {
                if (x < hx - 2 || x > hx + 12) continue;
                if (row[0] == -583 && (hx == -190 || hx == -174 || hx <= -226)) continue;
                if (row[0] == -597 && hx <= -226) continue;
                int hv = house(x, y, z, hx, row, base);
                if (hv != KEEP) return hv;
            }
        }
        // bordure des corniches : garde-corps et torches
        if (y >= fy + 1 && y <= fy + 3 && (y == fy + 1 || (x + z) % 8 == 0) && stairFacing(x, z) < 0 && minNeighbour(x, z) < fy - 1) {
            if (y == fy + 1) return (x + z) % 8 == 0 ? DBRICKS : BWALL;
            return y == fy + 2 ? DBRICKS : DLAMP;
        }
        return KEEP;
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

    private static int house(int x, int y, int z, int hx, int[] row, int base) {
        int xl = x - hx;
        int zl = row[2] > 0 ? row[1] - z : z - row[0];             // 0 = cote de la porte
        int v = y - base;
        // lampadaires devant la porte
        if (zl == -1 && (xl == 1 || xl == 9)) return lampPost(v, 3);
        if (xl < 0 || xl > 10 || zl < 0 || zl > 9) return KEEP;
        int cell = hsh(hx, row[0], 3);
        boolean temple = cell % 3 == 0;
        boolean xe = xl == 0 || xl == 10, ze = zl == 0 || zl == 9;
        if (!temple) {
            int k = Math.min(zl, 9 - zl), roofV = 6 + k;
            if (v >= 6) {
                if (v > roofV) return KEEP;
                if (v == roofV) return k == 4 ? DSTONE : DSTAIR + ((zl < 5) == (row[2] > 0) ? 0 : 2);
                return xe ? DBRICKS : (k == 4 && xl == 5 && zl == 4 ? CHAIN : AIR);
            }
            if (v < 1) return KEEP;
            if (xe || ze) {
                if (xe && ze) return DPILLAR;
                if (zl == 0 && xl >= 4 && xl <= 6 && v <= 3) return AIR;
                if (v == 3 && ((ze && (xl == 2 || xl == 8)) || (xe && (zl == 4 || zl == 5)))) return BARS;
                if (v == 5) return DCHISEL;
                return DBRICKS;
            }
            if (v == 5 && xl == 5 && zl == 4) return LANTERN;
            return interior(cell, v, xl, zl);
        }
        // maison a colonnade et fronton
        int k = Math.min(xl, 10 - xl), roofV = 7 + k;
        if (v >= 7) {
            if (v > roofV) return KEEP;
            if (v == roofV) return k == 5 ? DSTONE : DSTAIR + (xl < 5 ? 1 : 3);
            if (zl == 0) return (v == roofV - 1 && xl == 5) ? DRUNES : DBRICKS;      // le fronton
            if (zl == 9) return DBRICKS;
            return AIR;
        }
        if (v < 1) return KEEP;
        if (zl <= 1) {                                                              // le portique
            if (zl == 0 && (xl == 0 || xl == 3 || xl == 7 || xl == 10)) return v == 6 ? DCHISEL : DPILLAR;
            if (v == 6 && zl == 0) return DCHISEL;
            return AIR;
        }
        if (xe || ze || zl == 2) {
            if (zl == 2 && xl >= 4 && xl <= 6 && v <= 3) return AIR;
            if (v == 3 && ((zl == 2 && (xl == 2 || xl == 8)) || (xe && (zl == 5 || zl == 6)))) return BARS;
            if (xe && ze) return DPILLAR;
            return v == 6 ? DCHISEL : DBRICKS;
        }
        if (v == 5 && xl == 5 && zl == 5) return LANTERN;
        return interior(cell, v, xl, zl - 1);
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

    /** la maison du roi : un palais pose sur la corniche nord, face a la ravine */
    private static int kingsHouse(int x, int y, int z) {
        final int X0 = -258, X1 = -208, Z0 = -596, Z1 = -566, WH = 12;
        if (x < X0 - 1 || x > X1 + 1 || z < Z0 - 1 || z > Z1 + 1) return KEEP;
        int v = y - F, xl = x - X0, zl = z - Z0;
        boolean inb = x >= X0 && x <= X1 && z >= Z0 && z <= Z1;
        int cxl = Math.min(xl, 50 - xl), czl = Math.min(zl, 30 - zl);
        if (cxl >= -1 && cxl <= 1 && czl >= -1 && czl <= 1) {                              // tourelles d'angle
            if (v >= 1 && v <= WH + 8) return (cxl == 1 && czl == 1) ? KEEP : DPILLAR;
            if (v == WH + 9) return DLAMP;
        }
        if (!inb) return KEEP;
        boolean xe = xl == 0 || xl == 50, ze = zl == 0 || zl == 30;
        if (v == WH + 1) return DSTONE;
        if (v == WH + 2) return (xe || ze) ? ((xl + zl) % 2 == 0 ? BWALL : KEEP) : KEEP;
        if (v > WH + 2) return KEEP;
        if (xe || ze) {
            if (zl == 0 && xl >= 27 && xl <= 33 && v <= 8) return AIR;                       // la grande porte, face a la ravine
            if (zl == 0 && xl >= 26 && xl <= 34 && v <= 9 && (xl == 26 || xl == 34 || v == 9)) return DCHISEL;
            if (v >= 6 && v <= 8 && ((ze && xl % 8 == 4) || (xe && zl % 8 == 4))) return BARS;
            if (v == WH) return DCHISEL;
            if ((xe && zl % 10 == 0) || (ze && xl % 8 == 0)) return DPILLAR;
            return DBRICKS;
        }
        // interieur : la salle du roi (trone a l'ouest)
        int dzc = zl - 15;
        if (xl <= 7 && Math.abs(dzc) <= 6 && v == 1) return DBRICKS;                          // estrade
        if (xl == 3 && dzc == 0 && v == 2) return DSTAIR + 3;                                 // le fauteuil du roi
        if (xl <= 2 && Math.abs(dzc) <= 2 && v >= 2 && v <= 9 - Math.abs(dzc) * 2) return Math.abs(dzc) == 2 ? GOLD : DCHISEL;
        if (xl == 3 && Math.abs(dzc) == 2 && v == 2) return DSTONE;
        if (v == 1 && xl >= 8 && Math.abs(dzc) <= 2) return RCARPET;
        if (v == 1 && xl >= 8 && Math.abs(dzc) == 3 && Math.floorMod(xl, 6) == 1) return BARREL;
        if ((xl == 14 || xl == 22 || xl == 30 || xl == 38 || xl == 46) && (zl == 7 || zl == 23)) return v <= WH ? DPILLAR : KEEP;
        if (zl == 29 && v <= 3 && xl >= 10 && xl <= 24) return BOOKS;
        if (zl == 29 && v <= 3 && xl >= 38 && xl <= 48) return BOOKS;
        if (v == 1 && xl >= 41 && xl <= 48 && zl >= 24 && zl <= 28) return xl == 41 ? BLACKWOOL : REDWOOL;       // le lit du roi
        if (v == 1 && xl >= 14 && xl <= 34 && (zl == 4 || zl == 26)) return FENCE;
        if (v == 2 && xl >= 14 && xl <= 34 && (zl == 4 || zl == 26)) return BSLAB;
        if (xl % 8 == 6 && (zl == 7 || zl == 15 || zl == 23) && v >= WH - 3 && v < WH) return CHAIN;
        if (xl % 8 == 6 && (zl == 7 || zl == 15 || zl == 23) && v == WH - 4) return LANTERN;
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
        int g = guard(x, y, z, r.x0 + 10, r.z0 + 12, F, 2);
        if (g != KEEP) return g;
        g = guard(x, y, z, r.x0 + 10, r.z1 - 12, F, 2);
        if (g != KEEP) return g;
        g = guard(x, y, z, r.x1 - 6, r.z0 + 12, F, 3);
        if (g != KEEP) return g;
        g = guard(x, y, z, r.x1 - 6, r.z1 - 12, F, 3);
        if (g != KEEP) return g;
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
        int g = guard(x, y, z, cx, r.z0 + 4, F, 0);
        if (g != KEEP) return g;
        g = guard(x, y, z, cx, r.z1 - 4, F, 1);
        if (g != KEEP) return g;
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
        int s = statue(x, y, z, cx - 32, -938, F, 1);
        if (s != KEEP) return s;
        s = statue(x, y, z, cx + 32, -938, F, 1);
        if (s != KEEP) return s;
        // deux colosses de plus, de chaque cote de l'entree
        s = colossus(x, y, z, cx - 34, -856, F, 0);
        if (s != KEEP) return s;
        s = colossus(x, y, z, cx + 34, -856, F, 0);
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
            int g = guard(x, y, z, 240, -876, F, 0);
            if (g != KEEP) return g;
            g = guard(x, y, z, 286, -876, F, 0);
            if (g != KEEP) return g;
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

    private static final double[][] RIM = new double[6][3];
    static {
        for (int i = 0; i < 6; i++) {
            double th = Math.toRadians(30 + 60 * i), cs = Math.cos(th), sn = Math.sin(th);
            RIM[i][0] = Math.round(MCX + 0.92 * MA * cs);
            RIM[i][1] = Math.round(MCZ + 0.92 * MB * sn);
            RIM[i][2] = Math.abs(cs) > Math.abs(sn) ? (cs > 0 ? 3 : 2) : (sn > 0 ? 1 : 0);
        }
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
        // les gardiens du bord
        for (double[] rm : RIM) {
            if (Math.abs(x - rm[0]) <= 10 && Math.abs(z - rm[1]) <= 10) {
                int g = guard(x, y, z, (int) rm[0], (int) rm[1], F, (int) rm[2]);
                if (g != KEEP) return g;
            }
        }
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
