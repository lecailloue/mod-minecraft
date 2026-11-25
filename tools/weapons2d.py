"""Les armes du mod, dessinees en 2D (icones 64x64), d'apres les references du croquis.

Chaque arme est dessinee dans son propre repere : u le long de l'arme (0 = bout du
manche), v en travers (v negatif = cote eclaire, en haut a gauche). Une lame est donc
une ligne de dos et une ligne de tranchant, ce qui permet les lames courbes elfiques
comme les lames droites naines.

    python3 tools/weapons2d.py preview   -> planche de contact dans /tmp/armes2d
    python3 tools/weapons2d.py gen       -> icones dans le mod
"""
import math
import os
import sys

W = 64
DIAG = math.radians(-45)      # arme en diagonale, pointe en haut a droite
VERT = math.radians(-90)      # arcs : debout
O_DIAG = (8.0, 56.0)
O_VERT = (24.0, 61.0)

# ----------------------------------------------------- matieres (clair, moyen, sombre, contour)
SILVER = ((0xF4, 0xF8, 0xFA), (0xCA, 0xD6, 0xDC), (0x8E, 0x9E, 0xA8), (0x44, 0x50, 0x58))
SILVER_D = ((0xB4, 0xC0, 0xC8), (0x86, 0x94, 0x9E), (0x5A, 0x66, 0x70), (0x2C, 0x34, 0x3A))
STEEL = ((0xEE, 0xF2, 0xF6), (0xBE, 0xC6, 0xCE), (0x84, 0x8E, 0x9A), (0x3A, 0x42, 0x4C))
DARKSTEEL = ((0x7A, 0x82, 0x8A), (0x55, 0x5C, 0x64), (0x36, 0x3C, 0x42), (0x1A, 0x1E, 0x22))
BRASS = ((0xEE, 0xC8, 0x72), (0xC2, 0x96, 0x42), (0x84, 0x62, 0x26), (0x3E, 0x2C, 0x0E))
BRONZE = ((0xE4, 0xB0, 0x5C), (0xB2, 0x80, 0x34), (0x74, 0x50, 0x1E), (0x36, 0x24, 0x0C))
BLACKIRON = ((0x5C, 0x52, 0x46), (0x40, 0x38, 0x30), (0x28, 0x22, 0x1C), (0x12, 0x0E, 0x0A))
BLACKIRON_L = ((0x8E, 0x82, 0x70), (0x66, 0x5C, 0x4E), (0x42, 0x3A, 0x30), (0x1C, 0x18, 0x12))
WOOD = ((0xA8, 0x7A, 0x4A), (0x7E, 0x58, 0x32), (0x56, 0x3A, 0x20), (0x28, 0x1A, 0x0E))
PALEWOOD = ((0xE2, 0xC4, 0x92), (0xC0, 0x9E, 0x6C), (0x92, 0x74, 0x48), (0x46, 0x36, 0x20))
DARKWOOD = ((0x6E, 0x5C, 0x44), (0x4E, 0x40, 0x2E), (0x34, 0x2A, 0x1E), (0x16, 0x12, 0x0C))
ELFWOOD = ((0x8A, 0x82, 0x68), (0x62, 0x5C, 0x48), (0x42, 0x3C, 0x2E), (0x1E, 0x1A, 0x14))
LEATHER = ((0x8C, 0x4E, 0x34), (0x66, 0x36, 0x24), (0x44, 0x22, 0x16), (0x20, 0x10, 0x0A))
DARKLEATHER = ((0x4E, 0x3C, 0x2C), (0x36, 0x28, 0x1E), (0x22, 0x18, 0x12), (0x0E, 0x0A, 0x08))
GREENWRAP = ((0x7E, 0xA2, 0x4E), (0x5A, 0x78, 0x34), (0x3A, 0x52, 0x20), (0x1A, 0x26, 0x0E))
STRING = ((0xF0, 0xEC, 0xE0), (0xC8, 0xC2, 0xB2), (0x90, 0x8A, 0x7C), (0x44, 0x40, 0x38))


# ---------------------------------------------------------------- base
def new():
    return [[None] * W for _ in range(W)]


def place(u, v, origin, ang):
    dx, dy = math.cos(ang), math.sin(ang)
    nx, ny = -dy, dx
    return origin[0] + u * dx + v * nx, origin[1] + u * dy + v * ny


def px(c, x, y, col, out=None):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < W and 0 <= y < W and col is not None:
        c[y][x] = (col, out)


def shape(c, pts, pal, origin=O_DIAG, ang=DIAG, hi=0.30, lo=0.68, step=0.34, flat=False, band=None):
    """remplit un polygone donne dans le repere de l'arme, lumiere sur le bord v-"""
    us = [p[0] for p in pts]
    u = min(us)
    while u <= max(us) + 1e-9:
        vs = []
        for i in range(len(pts)):
            (u0, v0), (u1, v1) = pts[i], pts[(i + 1) % len(pts)]
            if (u0 <= u < u1) or (u1 <= u < u0):
                vs.append(v0 + (u - u0) * (v1 - v0) / (u1 - u0))
        vs.sort()
        for k in range(0, len(vs) - 1, 2):
            a, b = vs[k], vs[k + 1]
            v = a
            while v <= b + 1e-9:
                r = (v - a) / (b - a) if b > a else 0.0
                col = pal[1] if flat else (pal[0] if r < hi else (pal[1] if r < lo else pal[2]))
                if band and band(u):
                    col = pal[2]
                x, y = place(u, v, origin, ang)
                px(c, x, y, col, pal[3])
                v += step
        u += step


def blade_pts(u0, u1, cf, wf, n=None):
    """polygone d'une lame : ligne de dos (v-) puis retour par le tranchant (v+)"""
    n = n or max(8, int((u1 - u0) * 2))
    top, bot = [], []
    for i in range(n + 1):
        u = u0 + (u1 - u0) * i / n
        cc, ww = cf(u), max(0.0, wf(u))
        top.append((u, cc - ww / 2))
        bot.append((u, cc + ww / 2))
    return top + bot[::-1]


def stroke_pts(path, wf):
    """polygone autour d'un chemin (u,v) de largeur variable : pour les arcs"""
    top, bot = [], []
    n = len(path) - 1
    for i, (u, v) in enumerate(path):
        t = i / n
        w = wf(t) / 2
        j, k = min(i + 1, n), max(i - 1, 0)
        du, dv = path[j][0] - path[k][0], path[j][1] - path[k][1]
        d = math.hypot(du, dv) or 1
        nu, nv = -dv / d, du / d
        top.append((u + nu * w, v + nv * w))
        bot.append((u - nu * w, v - nv * w))
    return top + bot[::-1]


def free_shape(c, pts, pal, origin=O_DIAG, ang=VERT, step=0.34, hi=0.3, lo=0.68):
    """meme chose mais pour un polygone quelconque (balayage en v)"""
    vs = [p[1] for p in pts]
    v = min(vs)
    while v <= max(vs) + 1e-9:
        us = []
        for i in range(len(pts)):
            (u0, v0), (u1, v1) = pts[i], pts[(i + 1) % len(pts)]
            if (v0 <= v < v1) or (v1 <= v < v0):
                us.append(u0 + (v - v0) * (u1 - u0) / (v1 - v0))
        us.sort()
        for k in range(0, len(us) - 1, 2):
            a, b = us[k], us[k + 1]
            u = a
            while u <= b + 1e-9:
                r = (u - a) / (b - a) if b > a else 0.0
                col = pal[0] if r < hi else (pal[1] if r < lo else pal[2])
                x, y = place(u, v, origin, ang)
                px(c, x, y, col, pal[3])
                u += step
        v += step


def thick(c, path, pal, wf, origin=O_DIAG, ang=DIAG, step=0.34, hi=0.30, lo=0.66):
    """trait epais le long d'un chemin (u,v) : marche aussi quand le chemin revient en arriere"""
    n = len(path) - 1
    for i, (u, v) in enumerate(path):
        t = i / n if n else 0
        w = wf(t) if callable(wf) else wf
        j, k = min(i + 1, n), max(i - 1, 0)
        du, dv = path[j][0] - path[k][0], path[j][1] - path[k][1]
        d = math.hypot(du, dv) or 1
        nu, nv = -dv / d, du / d
        s0 = -w / 2
        while s0 <= w / 2 + 1e-9:
            r = (s0 + w / 2) / w if w else 0
            col = pal[0] if r < hi else (pal[1] if r < lo else pal[2])
            x, y = place(u + nu * s0, v + nv * s0, origin, ang)
            px(c, x, y, pal[0] if col is None else col, pal[3])
            s0 += step


def line(c, p0, p1, col, origin=O_DIAG, ang=DIAG, out=None):
    n = int(max(abs(p1[0] - p0[0]), abs(p1[1] - p0[1])) * 3) + 1
    for i in range(n + 1):
        t = i / n
        x, y = place(p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t, origin, ang)
        px(c, x, y, col, out)


def hole(c, pts, origin=O_DIAG, ang=DIAG):
    """evide un polygone (ajours des lames elfiques, coeur des gardes naines)"""
    us = [p[0] for p in pts]
    u = min(us)
    while u <= max(us):
        vs = []
        for i in range(len(pts)):
            (u0, v0), (u1, v1) = pts[i], pts[(i + 1) % len(pts)]
            if (u0 <= u < u1) or (u1 <= u < u0):
                vs.append(v0 + (u - u0) * (v1 - v0) / (u1 - u0))
        vs.sort()
        for k in range(0, len(vs) - 1, 2):
            v = vs[k]
            while v <= vs[k + 1]:
                x, y = place(u, v, origin, ang)
                xi, yi = int(round(x)), int(round(y))
                if 0 <= xi < W and 0 <= yi < W:
                    c[yi][xi] = None
                v += 0.34
        u += 0.34


def bez(p0, p1, p2, n=40):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])
            for t in [i / n for i in range(n + 1)]]


def bez3(p0, p1, p2, p3, n=60):
    out = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        out.append((u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0],
                    u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1]))
    return out


def fill(c, pts, pal, origin=O_DIAG, ang=DIAG, hi=0.26, lo=0.64):
    """polygone quelconque : on teste chaque pixel (marche pour les croissants, becs, etc.)"""
    world = [place(u, v, origin, ang) for u, v in pts]
    xs = [p[0] for p in world]
    ys = [p[1] for p in world]
    dx, dy = math.cos(ang), math.sin(ang)
    for yy in range(max(0, int(min(ys)) - 1), min(W, int(max(ys)) + 2)):
        for xx in range(max(0, int(min(xs)) - 1), min(W, int(max(xs)) + 2)):
            inside = False
            j = len(world) - 1
            for i in range(len(world)):
                (x0, y0), (x1, y1) = world[i], world[j]
                if (y0 > yy) != (y1 > yy) and xx < x0 + (yy - y0) * (x1 - x0) / (y1 - y0):
                    inside = not inside
                j = i
            if not inside:
                continue
            u = (xx - origin[0]) * dx + (yy - origin[1]) * dy
            v = -(xx - origin[0]) * dy + (yy - origin[1]) * dx
            vs = []
            for i in range(len(pts)):
                (u0, v0), (u1, v1) = pts[i], pts[(i + 1) % len(pts)]
                if (u0 <= u < u1) or (u1 <= u < u0):
                    vs.append(v0 + (u - u0) * (v1 - v0) / (u1 - u0))
            vs.sort()
            r = 0.5
            for k in range(0, len(vs) - 1, 2):
                if vs[k] - 0.6 <= v <= vs[k + 1] + 0.6 and vs[k + 1] > vs[k]:
                    r = (v - vs[k]) / (vs[k + 1] - vs[k])
                    break
            c[yy][xx] = (pal[0] if r < hi else (pal[1] if r < lo else pal[2]), pal[3])


def outline(c):
    add = {}
    for y in range(W):
        for x in range(W):
            if c[y][x] is not None:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nb = c[y + dy][x + dx] if 0 <= x + dx < W and 0 <= y + dy < W else None
                if nb and nb[1]:
                    add[(x, y)] = nb[1]
                    break
    for (x, y), col in add.items():
        c[y][x] = (col, None)


def wrap(c, u0, u1, pal, half, step=2.6, origin=O_DIAG, ang=DIAG):
    """lanieres / anneaux sur un manche"""
    u = u0
    i = 0
    while u <= u1:
        line(c, (u, -half), (u + 0.8, half), pal[0] if i % 2 else pal[2], origin, ang, pal[3])
        u += step
        i += 1


# ======================================================================= ELFES
def elven_sword():
    """epee de Thranduil : longue lame courbe a un tranchant, tout en argent"""
    c = new()
    cf = lambda u: -0.0040 * max(0.0, u - 22) ** 2
    wf = lambda u: 4.0 if u < 52 else 4.0 * max(0.0, (66 - u) / 14) ** 0.55
    shape(c, blade_pts(22, 66, cf, wf, 140), SILVER, hi=0.26, lo=0.60)
    line(c, (24, cf(24) - 1.2), (60, cf(60) - 1.0), SILVER[0])
    for u in range(27, 58, 3):                                   # entrelacs graves
        line(c, (u, cf(u) - 0.4), (u + 1.6, cf(u) + 1.0), SILVER_D[1])
    fill(c, [(17.5, 1.6), (21.5, 2.4), (23.5, 7.0), (19.5, 6.2), (17.5, 3.2)], SILVER_D)   # aile
    shape(c, [(16.2, -3.4), (21.8, -2.6), (21.8, 2.6), (16.2, 3.4)], SILVER)               # virole
    shape(c, blade_pts(5, 18, lambda u: 0, lambda u: 3.2 + 0.9 * math.sin((u - 5) / 13 * math.pi)), SILVER_D)
    for u in (8.0, 11.0, 14.0):
        line(c, (u, -1.8), (u + 0.5, 1.8), SILVER[2], out=SILVER[3])
    shape(c, [(0.0, -2.4), (1.0, -4.8), (3.4, -4.4), (4.6, -1.8), (4.6, 1.8), (3.4, 4.4),
              (1.0, 4.8), (0.0, 2.4)], SILVER)                   # pommeau
    line(c, (1.2, -3.6), (3.4, -2.8), SILVER[0])
    line(c, (1.2, 3.6), (3.4, 2.8), SILVER[2])
    outline(c)
    return c


def elven_dagger():
    """dague elfique : large lame a un tranchant qui s'elargit vers la pointe, bois et laiton"""
    c = new()
    cf = lambda u: -0.035 * (u - 20)
    back = [(u, cf(u) - (2.0 + 2.8 * min(1.0, (u - 20) / 26.0))) for u in [20 + i * 0.3 for i in range(94)]]
    edge = [(u, cf(u) + (2.0 + 2.8 * min(1.0, (u - 20) / 26.0))) for u in [20 + i * 0.3 for i in range(94)]]
    nose = [(47.0, cf(47) - 4.8), (56.0, cf(56) - 0.4), (49.0, cf(49) + 4.8)]      # clip puis pointe
    shape(c, back + nose + edge[::-1], STEEL, hi=0.20, lo=0.52)
    line(c, (22, cf(22) - 3.4), (49, cf(49) - 4.4), STEEL[0])
    for u in range(25, 48, 4):                                    # ligne de trempe
        line(c, (u, cf(u) + 1.0), (u + 3.0, cf(u) + 1.6), SILVER_D[2])
    shape(c, [(17.0, -3.8), (20.0, -3.4), (20.0, 3.4), (17.0, 3.8)], BRASS)
    thick(c, [(u, 0.0) for u in [5 + i * 0.3 for i in range(40)]], WOOD,
          lambda t: 4.6 + 0.8 * math.sin(math.pi * t))
    shape(c, [(1.0, -4.6), (5.0, -3.4), (5.0, 3.4), (1.0, 4.6)], BRASS)
    outline(c)
    return c


def _bow(c, limb_up, wf, pal, grip_pal, grip, string_pal, tips=None, nock=None):
    """une branche + sa symetrique, la poignee, puis la corde"""
    dn = [(62 - u, v) for (u, v) in limb_up]
    for p in (limb_up, dn):
        thick(c, p, pal, wf, origin=O_VERT, ang=VERT)
    thick(c, [(u, grip[2]) for u in [grip[0] + i * 0.25 for i in range(int((grip[1] - grip[0]) / 0.25))]],
          grip_pal, grip[3], origin=O_VERT, ang=VERT)
    line(c, (limb_up[0][0], string_pal[4]), (62 - limb_up[0][0], string_pal[4]),
         string_pal[0], O_VERT, VERT, string_pal[3])


def elven_bow():
    """arc elfique : branches fines a double courbure, poignee entouree de vert"""
    c = new()
    up = bez3((59.0, 1.0), (49.0, -9.0), (41.0, -4.0), (33.0, -1.0), 80)
    _bow(c, up, lambda t: 1.6 + 2.0 * t, ELFWOOD, GREENWRAP, (26.0, 36.0, -1.0, lambda t: 5.0),
         STRING + (1.6,))
    wrap(c, 26.5, 35.5, GREENWRAP, 2.4, 1.8, origin=O_VERT, ang=VERT)
    for u in (59.0, 3.0):                                          # pointes sombres
        line(c, (u, -1.0), (u, 1.4), BLACKIRON[1], O_VERT, VERT, BLACKIRON[3])
    outline(c)
    return c


def elven_longbow():
    """arc long elfique : le plus grand, branches a feuilles de la Foret Noire"""
    c = new()
    up = bez3((62.0, 1.5), (50.0, -11.0), (42.0, -4.0), (33.0, -1.2), 90)
    _bow(c, up, lambda t: 1.4 + 2.2 * t, BLACKIRON, BLACKIRON, (27.0, 35.0, -1.4, lambda t: 5.4),
         STRING + (2.0,))
    for p in (up, [(64 - u, v) for (u, v) in up]):                 # nervures d'or
        for i in (26, 48, 68):
            u, v = p[i]
            line(c, (u, v - 1.2), (u, v + 1.2), BRONZE[1], O_VERT, VERT, BRONZE[3])
    wrap(c, 27.0, 35.0, BRONZE, 2.8, 2.0, origin=O_VERT, ang=VERT)
    for u in range(28, 37):                                        # tresse de la corde
        px(c, *place(u, 2.0, O_VERT, VERT), STRING[2], STRING[3])
    outline(c)
    return c


def elven_spear():
    """lance elfique a deux fers : hampe de bronze, lames courbes ajourees aux deux bouts"""
    c = new()
    thick(c, [(u, 0.0) for u in [18 + i * 0.3 for i in range(94)]], BRONZE, lambda t: 2.2)
    line(c, (20, -1.0), (44, -1.0), BRONZE[0])
    for a, s in ((46.0, 1), (18.0, -1)):
        pts = blade_pts(min(a, a + s * 21), max(a, a + s * 21),
                        lambda u, a=a, s=s: -0.016 * ((u - a) * s) ** 2,
                        lambda u, a=a, s=s: 5.0 * max(0.05, 1 - abs(u - a) / 22.0) ** 0.62, 80)
        shape(c, pts, BRONZE, hi=0.22, lo=0.56)
        shape(c, [(a - s * 2.0, -4.6), (a + s * 3.0, -3.6), (a + s * 3.0, 3.6), (a - s * 2.0, 4.6)], BRONZE)
        hole(c, [(a + s * 5.0, -1.4), (a + s * 11.0, -3.6), (a + s * 12.0, -1.8), (a + s * 6.0, 0.6)])
    outline(c)
    return c


# ======================================================================= NAINS
def dwarven_sword():
    """epee naine : lame droite large a pointe en ciseau, garniture d'or geometrique"""
    c = new()
    shape(c, [(22.0, -3.6), (52.0, -3.6), (58.0, -1.0), (60.0, 1.4), (52.0, 3.6), (22.0, 3.6)],
          STEEL, hi=0.18, lo=0.58)
    line(c, (24, -2.2), (54, -2.2), STEEL[0])
    line(c, (24, 2.2), (54, 2.2), SILVER_D[2])
    line(c, (24, 0.0), (58, 0.0), SILVER_D[1])
    shape(c, [(15.0, -4.2), (17.5, -9.4), (21.8, -6.0), (21.8, 6.0), (17.5, 9.4), (15.0, 4.2)], BRONZE)
    line(c, (16.6, -7.6), (20.6, -5.2), BRASS[0])
    line(c, (16.6, 7.6), (20.6, 5.2), BRONZE[2])
    hole(c, [(18.0, -1.6), (19.6, 0.0), (18.0, 1.6), (16.6, 0.0)])
    shape(c, blade_pts(4, 16, lambda u: 0, lambda u: 4.0), BRONZE)
    for u in (6.5, 9.5, 12.5):
        shape(c, [(u, -1.9), (u + 1.5, 0.0), (u, 1.9), (u - 1.5, 0.0)], BRASS)
    shape(c, [(-1.0, -5.0), (1.5, -6.6), (4.5, -5.2), (4.5, 5.2), (1.5, 6.6), (-1.0, 5.0)], BRONZE)
    line(c, (0.6, -5.4), (0.6, 5.4), BRASS[0])
    shape(c, [(1.6, -2.2), (3.4, 0.0), (1.6, 2.2), (0.0, 0.0)], BRASS)
    outline(c)
    return c


def dwarven_hammer():
    """marteau de guerre nain : tete a deux tables gravees, long manche"""
    c = new()
    thick(c, [(u, 0.0) for u in [2 + i * 0.3 for i in range(134)]], WOOD, lambda t: 3.6)
    for u in range(8, 36, 4):
        line(c, (u, -2.4), (u + 1.0, 2.4), DARKLEATHER[1], out=DARKLEATHER[3])
    shape(c, [(0.0, -4.4), (3.0, -3.6), (3.0, 3.6), (0.0, 4.4)], DARKSTEEL)
    for s in (-1, 1):
        shape(c, [(43.0, s * 4.6), (43.0, s * 13.0), (47.0, s * 14.6), (56.0, s * 13.6),
                  (58.0, s * 9.6), (58.0, s * 6.0), (54.0, s * 4.6)], STEEL, hi=0.16, lo=0.60)
        for u in (48.0, 52.0, 56.0):
            line(c, (u, s * 6.6), (u, s * 12.0), DARKSTEEL[1])
        line(c, (44.0, s * 12.6), (57.0, s * 11.6), STEEL[0])
    shape(c, [(41.0, -5.2), (60.0, -5.2), (60.0, 5.2), (41.0, 5.2)], DARKSTEEL)
    line(c, (43.0, -4.2), (58.0, -4.2), STEEL[1])
    line(c, (43.0, 4.2), (58.0, 4.2), DARKSTEEL[2])
    outline(c)
    return c


def dwarven_axe():
    """hache naine a deux tranchants : fers en croissant, colonne gravee au milieu"""
    c = new()
    thick(c, [(u, 0.0) for u in [2 + i * 0.3 for i in range(140)]], WOOD, lambda t: 3.2)
    for u in range(6, 30, 3):
        line(c, (u, -2.2), (u + 0.8, 2.2), DARKLEATHER[1], out=DARKLEATHER[3])
    shape(c, [(0.0, -3.8), (2.5, -3.0), (2.5, 3.0), (0.0, 3.8)], BLACKIRON)
    for s in (-1, 1):                                              # deux grands fers en demi-lune
        arc = bez3((40.0, s * 6.0), (42.0, s * 21.0), (58.0, s * 22.0), (62.0, s * 6.0), 70)
        fill(c, arc + [(62.0, s * 4.0), (40.0, s * 4.0)], BLACKIRON_L, hi=0.2, lo=0.6)
        thick(c, arc[16:54], BLACKIRON_L, lambda t: 1.4)           # tranchant clair
        hole(c, [(44.5, s * 7.0), (47.0, s * 13.0), (50.5, s * 7.5)])      # ajours
        hole(c, [(52.5, s * 7.5), (56.0, s * 13.0), (58.5, s * 7.0)])
    shape(c, [(41.0, -4.2), (59.0, -4.2), (59.0, 4.2), (41.0, 4.2)], BRONZE)       # colonne gravee
    for u in (44.0, 49.0, 54.0):
        line(c, (u, -3.4), (u, 3.4), BLACKIRON[1])
    line(c, (42.0, -3.5), (58.0, -3.5), BRASS[0])
    outline(c)
    return c


def dwarven_pick():
    """pioche de combat : long bec droit, contre-bec court, collier d'or"""
    c = new()
    thick(c, [(u, 0.0) for u in [2 + i * 0.3 for i in range(127)]], PALEWOOD, lambda t: 3.0)
    shape(c, [(0.0, -4.0), (3.5, -3.2), (3.5, 3.2), (0.0, 4.0)], DARKSTEEL)
    for u in (9.0, 28.0):
        shape(c, [(u, -3.6), (u + 2.2, -3.6), (u + 2.2, 3.6), (u, 3.6)], BRASS)
    shape(c, [(36.0, -6.2), (45.0, -5.4), (45.0, 5.4), (36.0, 6.2)], BRASS)
    line(c, (38.0, -5.0), (43.0, -4.4), BRASS[0])
    fill(c, [(43.0, -4.6), (50.0, -20.0), (55.0, -17.0), (46.0, -1.0)], STEEL)               # long bec
    fill(c, [(41.0, 3.6), (35.0, 11.0), (39.0, 13.5), (45.0, 4.6)], STEEL)                   # contre-bec
    line(c, (44.5, -5.4), (51.5, -19.0), STEEL[0])
    line(c, (41.5, 4.4), (36.5, 10.6), STEEL[0])
    outline(c)
    return c


def dwarven_bow():
    """arc nain (type Thorin) : bois epais, ferrures, poignee gravee"""
    c = new()
    up = bez3((57.0, 2.5), (49.0, -8.0), (41.0, -5.0), (33.0, -1.6), 80)
    _bow(c, up, lambda t: 2.4 + 2.8 * t, WOOD, DARKWOOD, (25.0, 37.0, -1.4, lambda t: 6.2),
         STRING + (2.8,))
    for p in (up, [(62 - u, v) for (u, v) in up]):
        for i in (28, 56):
            u, v = p[i]
            line(c, (u, v - 1.6), (u, v + 1.6), WOOD[0], O_VERT, VERT)
    for u in (28.0, 33.0):
        line(c, (u, -4.4), (u, 1.4), BRONZE[1], O_VERT, VERT, BRONZE[3])
    for u in (24.0, 38.0):
        line(c, (u, -4.6), (u, 1.8), DARKSTEEL[1], O_VERT, VERT, DARKSTEEL[3])
    outline(c)
    return c


# ======================================================================= HOMMES
def man_sword():
    """epee des hommes : lame droite a deux tranchants et gouttiere, longue garde en croix"""
    c = new()
    wf = lambda u: 5.2 if u < 48 else 5.2 * max(0.0, (60 - u) / 12) ** 0.8
    shape(c, blade_pts(21, 60, lambda u: 0, wf, 100), STEEL, hi=0.22, lo=0.60)
    shape(c, blade_pts(23, 52, lambda u: 0, lambda u: 1.5), SILVER_D, flat=True)
    line(c, (23, -2.3), (54, -2.3), STEEL[0])
    shape(c, [(18.5, -11.0), (20.8, -10.0), (20.8, 10.0), (18.5, 11.0)], DARKSTEEL)
    shape(c, [(17.6, -3.4), (21.8, -3.0), (21.8, 3.0), (17.6, 3.4)], DARKSTEEL)
    thick(c, [(u, 0.0) for u in [5 + i * 0.3 for i in range(42)]], LEATHER, lambda t: 3.4)
    for u in range(7, 17, 2):
        line(c, (u, -1.9), (u + 0.6, 1.9), LEATHER[2], out=LEATHER[3])
    shape(c, [(0.5, -3.0), (2.5, -4.2), (4.6, -1.8), (4.6, 1.8), (2.5, 4.2), (0.5, 3.0)], DARKSTEEL)
    outline(c)
    return c


def man_greatsword():
    """grande epee : plus longue, plus large, poignee a deux mains"""
    c = new()
    wf = lambda u: 6.8 if u < 54 else 6.8 * max(0.0, (66 - u) / 12) ** 0.8
    shape(c, blade_pts(26, 66, lambda u: 0, wf, 110), STEEL, hi=0.20, lo=0.58)
    shape(c, blade_pts(28, 58, lambda u: 0, lambda u: 2.0), SILVER_D, flat=True)
    line(c, (28, -3.0), (60, -3.0), STEEL[0])
    shape(c, [(23.0, -14.0), (25.6, -12.6), (25.6, 12.6), (23.0, 14.0)], DARKSTEEL)
    shape(c, [(22.0, -3.8), (26.4, -3.4), (26.4, 3.4), (22.0, 3.8)], DARKSTEEL)
    thick(c, [(u, 0.0) for u in [4 + i * 0.3 for i in range(60)]], LEATHER, lambda t: 3.6)
    for u in range(6, 22, 2):
        line(c, (u, -2.0), (u + 0.6, 2.0), LEATHER[2], out=LEATHER[3])
    shape(c, [(0.0, -3.4), (2.0, -4.6), (4.6, -2.0), (4.6, 2.0), (2.0, 4.6), (0.0, 3.4)], DARKSTEEL)
    outline(c)
    return c


def man_spear():
    """lance des hommes : fer en feuille a arete centrale, douille, hampe de frene"""
    c = new()
    thick(c, [(u, 0.0) for u in [2 + i * 0.3 for i in range(143)]], PALEWOOD, lambda t: 2.8)
    shape(c, [(0.0, -3.4), (2.5, -2.6), (2.5, 2.6), (0.0, 3.4)], DARKSTEEL)
    shape(c, [(41.0, -4.2), (46.0, -3.2), (46.0, 3.2), (41.0, 4.2)], DARKSTEEL)
    wf = lambda u: 6.0 * math.sin(math.pi * min(1.0, (u - 45) / 19.0)) ** 0.62
    shape(c, blade_pts(45, 64, lambda u: 0, wf, 80), STEEL, hi=0.22, lo=0.60)
    line(c, (46, 0.0), (62, 0.0), SILVER_D[1])
    line(c, (47, -1.8), (60, -0.8), STEEL[0])
    outline(c)
    return c


def man_dagger():
    """dague des hommes : lame a deux tranchants, garde a boules de laiton, cuir enroule"""
    c = new()
    wf = lambda u: 4.2 * math.sin(math.pi * min(1.0, (u - 20) / 32.0)) ** 0.38
    shape(c, blade_pts(20, 52, lambda u: 0, wf, 90), STEEL, hi=0.24, lo=0.62)
    shape(c, blade_pts(22, 46, lambda u: 0, lambda u: 1.3), DARKSTEEL, flat=True)
    line(c, (22, -1.7), (48, -0.9), STEEL[0])
    for s in (-1, 1):
        shape(c, [(16.6, s * 4.2), (19.4, s * 5.4), (18.8, s * 7.8), (16.0, s * 7.0)], BRASS)
    shape(c, [(16.2, -5.0), (19.4, -4.2), (19.4, 4.2), (16.2, 5.0)], BRASS)
    thick(c, [(u, 0.0) for u in [5 + i * 0.3 for i in range(37)]], LEATHER, lambda t: 3.4)
    for u in range(6, 16, 2):
        line(c, (u, -1.9), (u + 0.5, 1.9), LEATHER[2], out=LEATHER[3])
    shape(c, [(1.2, -3.8), (4.6, -3.0), (4.6, 3.0), (1.2, 3.8)], BRASS)
    outline(c)
    return c


def man_bow():
    """arc des hommes : grand arc de bois clair, poignee de cuir"""
    c = new()
    up = bez3((60.0, 1.5), (50.0, -8.0), (42.0, -5.0), (33.0, -1.4), 80)
    _bow(c, up, lambda t: 1.8 + 2.2 * t, PALEWOOD, DARKLEATHER, (27.0, 35.0, -1.2, lambda t: 5.0),
         DARKSTEEL + (2.2,))
    wrap(c, 27.5, 34.5, DARKLEATHER, 2.4, 1.8, origin=O_VERT, ang=VERT)
    outline(c)
    return c


# ---------------------------------------------------------------- la liste
def models():
    return {
        'elven_sword': ('Épée elfique', elven_sword()),
        'elven_dagger': ('Dague elfique', elven_dagger()),
        'elven_bow': ('Arc elfique', elven_bow()),
        'elven_longbow': ('Arc long elfique', elven_longbow()),
        'elven_spear': ('Lance elfique', elven_spear()),
        'dwarven_sword': ('Épée naine', dwarven_sword()),
        'dwarven_hammer': ('Marteau nain', dwarven_hammer()),
        'dwarven_axe': ('Hache naine', dwarven_axe()),
        'dwarven_war_pick': ('Pioche de combat', dwarven_pick()),
        'dwarven_bow': ('Arc nain', dwarven_bow()),
        'man_sword': ('Épée des hommes', man_sword()),
        'man_spear': ('Lance des hommes', man_spear()),
        'man_greatsword': ('Grande épée', man_greatsword()),
        'man_bow': ('Arc des hommes', man_bow()),
        'man_dagger': ('Dague des hommes', man_dagger()),
    }


# ---------------------------------------------------------------- sortie
def to_image(c):
    from PIL import Image
    im = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    p = im.load()
    for y in range(W):
        for x in range(W):
            if c[y][x]:
                p[x, y] = tuple(c[y][x][0]) + (255,)
    return im


def preview(out='/tmp/armes2d'):
    from PIL import Image, ImageDraw, ImageFont
    os.makedirs(out, exist_ok=True)
    M = models()
    ids = list(M)
    groups = [('ARMES ELFIQUES', ids[0:5]), ('ARMES NAINES', ids[5:10]), ('ARMES DES HOMMES', ids[10:15])]
    S, PAD, TOP, LBL = 192, 20, 38, 30
    img = Image.new('RGB', (5 * (S + PAD) + PAD, len(groups) * (S + LBL + TOP) + PAD), (26, 26, 30))
    d = ImageDraw.Draw(img)
    try:
        f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 19)
        fs = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
    except OSError:
        f = fs = ImageFont.load_default()
    for r, (title, keys) in enumerate(groups):
        y = PAD + r * (S + LBL + TOP)
        d.text((PAD, y), title, (0xE8, 0xC8, 0x80), font=f)
        for i, k in enumerate(keys):
            name, c = M[k]
            im = to_image(c).resize((S, S), Image.NEAREST)
            x = PAD + i * (S + PAD)
            d.rectangle([x - 2, y + TOP - 2, x + S + 1, y + TOP + S + 1], fill=(0x34, 0x34, 0x3C))
            img.paste(im, (x, y + TOP), im)
            d.text((x, y + TOP + S + 6), name, (0xDD, 0xDD, 0xDD), font=fs)
    img.save(out + '/planche.png')
    for k, (name, c) in M.items():
        to_image(c).resize((256, 256), Image.NEAREST).save(f'{out}/{k}.png')
    print('planche :', out + '/planche.png', '-', len(M), 'armes')


def gen():
    A = 'src/main/resources/assets/ringsofash'
    for k, (name, c) in models().items():
        to_image(c).save(f'{A}/textures/item/{k}.png')
    print('icones :', len(models()))


if __name__ == '__main__':
    (gen if len(sys.argv) > 1 and sys.argv[1] == 'gen' else preview)()
