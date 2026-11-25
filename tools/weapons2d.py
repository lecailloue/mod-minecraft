"""Les armes du mod, dessinees en 2D (icones 32x32).

Chaque race a sa propre silhouette, pas seulement sa couleur :
  elfes  : lames en feuille, fines, garde en ailes, or et argent clair
  nains  : lames courtes et larges en coin, grosses ferrures de bronze, tout est epais
  hommes : lames droites a gouttiere, longue garde en croix, cuir et acier
  uruk   : lames crochues et ebrechees, metal noir rouille, os et chiffons

On dessine au pixel pres : une lame = un chemin + un profil de largeur, l'ombrage
suit la normale (lumiere en haut a gauche), et un contour sombre ferme la silhouette.

    python3 tools/weapons2d.py preview   -> planche de contact dans /tmp/armes2d
    python3 tools/weapons2d.py gen       -> icones dans le mod
"""
import math
import os
import sys

W = 32  # cote de l'icone

# ------------------------------------------------------- matieres (clair, moyen, sombre, contour)
ELF_STEEL = ((0xF4, 0xFA, 0xFC), (0xCE, 0xE0, 0xE8), (0x93, 0xAC, 0xBA), (0x44, 0x5A, 0x68))
ELF_GOLD = ((0xF6, 0xE0, 0x9E), (0xD4, 0xAC, 0x52), (0x96, 0x74, 0x2E), (0x4C, 0x38, 0x14))
ELF_WOOD = ((0xEE, 0xE4, 0xCE), (0xCA, 0xBA, 0x98), (0x96, 0x84, 0x64), (0x4E, 0x42, 0x30))
DWA_STEEL = ((0xE2, 0xE4, 0xE6), (0xAE, 0xB4, 0xBA), (0x74, 0x7C, 0x86), (0x32, 0x36, 0x3E))
DWA_BRONZE = ((0xF0, 0xBC, 0x60), (0xC2, 0x88, 0x34), (0x86, 0x58, 0x20), (0x42, 0x2A, 0x0E))
DWA_GRIP = ((0xA8, 0x4C, 0x3C), (0x7A, 0x32, 0x28), (0x52, 0x1E, 0x18), (0x2A, 0x0E, 0x0A))
MAN_STEEL = ((0xEC, 0xF0, 0xF4), (0xBC, 0xC4, 0xCC), (0x82, 0x8C, 0x98), (0x3A, 0x40, 0x4A))
MAN_DARK = ((0x6E, 0x78, 0x86), (0x4C, 0x54, 0x60), (0x32, 0x38, 0x42), (0x18, 0x1C, 0x22))
MAN_GRIP = ((0x9A, 0x6C, 0x40), (0x6E, 0x4A, 0x2A), (0x4A, 0x30, 0x1A), (0x24, 0x16, 0x0C))
URU_STEEL = ((0x8A, 0x90, 0x8E), (0x5C, 0x62, 0x62), (0x38, 0x3C, 0x3E), (0x1A, 0x1C, 0x1E))
URU_RUST = ((0xB0, 0x5E, 0x30), (0x80, 0x40, 0x20), (0x54, 0x28, 0x14), (0x2A, 0x12, 0x08))
URU_GRIP = ((0x7E, 0x66, 0x46), (0x58, 0x46, 0x2E), (0x3A, 0x2C, 0x1C), (0x1C, 0x14, 0x0C))
BONE = ((0xF2, 0xEC, 0xD8), (0xD0, 0xC6, 0xAA), (0x9E, 0x92, 0x76), (0x4E, 0x46, 0x34))
GLOW_BLUE = ((0xF0, 0xFA, 0xFF), (0xC2, 0xE8, 0xFE), (0x86, 0xBE, 0xE4), (0x3C, 0x6E, 0x96))
GLOW_GOLD = ((0xFF, 0xF0, 0xB6), (0xF6, 0xCE, 0x62), (0xC0, 0x92, 0x2C), (0x5E, 0x42, 0x12))
EMBER = ((0xFF, 0xD0, 0x72), (0xF2, 0x7E, 0x24), (0xA8, 0x3A, 0x12), (0x4A, 0x16, 0x08))
GREEN = ((0xD2, 0xF0, 0x90), (0x92, 0xC8, 0x4C), (0x56, 0x86, 0x2C), (0x24, 0x40, 0x16))
LEATHER = ((0x5E, 0x4A, 0x38), (0x42, 0x32, 0x26), (0x2A, 0x1E, 0x16), (0x14, 0x0E, 0x0A))
DARKWOOD = ((0x7E, 0x60, 0x40), (0x5A, 0x42, 0x2A), (0x3C, 0x2A, 0x1A), (0x1E, 0x14, 0x0C))


# ---------------------------------------------------------------- primitives
def new():
    return [[None] * W for _ in range(W)]


def px(c, x, y, col, out=None):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < W and 0 <= y < W and col is not None:
        c[y][x] = (col, out)


def seg(c, p0, p1, col, out=None):
    x0, y0 = p0
    x1, y1 = p1
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2)
    for i in range(n + 1):
        t = i / n if n else 0
        px(c, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, col, out)


def bez(p0, p1, p2, n=64):
    out = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        out.append((u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
                    u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1]))
    return out


def bez3(p0, p1, p2, p3, n=80):
    out = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        out.append((u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0],
                    u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1]))
    return out


def stroke(c, path, pal, w=1.0, out=True):
    """trait epais le long d'un chemin, ombre sur le cote bas-droite"""
    hi, mid, lo, ol = pal
    for i, (x, y) in enumerate(path):
        j = min(i + 1, len(path) - 1)
        k = max(i - 1, 0)
        dx, dy = path[j][0] - path[k][0], path[j][1] - path[k][1]
        n = math.hypot(dx, dy) or 1
        nx, ny = -dy / n, dx / n
        half = w / 2
        s = -half
        while s <= half + 1e-6:
            col = hi if s < -half * 0.3 else (lo if s > half * 0.3 else mid)
            px(c, x + nx * s, y + ny * s, col, ol if out else None)
            s += 0.4


def taper(c, path, wfun, pal, out=True, fuller=None, serrate=None):
    """lame : chemin central + profil de largeur ; lumiere en haut a gauche"""
    hi, mid, lo, ol = pal
    n = len(path) - 1
    for i, (x, y) in enumerate(path):
        t = i / n
        half = wfun(t)
        if half <= 0:
            continue
        j = min(i + 1, n)
        k = max(i - 1, 0)
        dx, dy = path[j][0] - path[k][0], path[j][1] - path[k][1]
        d = math.hypot(dx, dy) or 1
        nx, ny = -dy / d, dx / d
        if serrate and serrate(t):
            half *= 0.55
        s = -half
        while s <= half + 1e-6:
            r = s / half if half else 0
            col = hi if r < -0.45 else (lo if r > 0.5 else mid)
            if fuller and abs(r) < 0.18 and 0.08 < t < fuller:
                col = lo
            px(c, x + nx * s, y + ny * s, col, ol if out else None)
            s += 0.4


def poly(c, pts, pal, out=True, shade=True):
    hi, mid, lo, ol = pal
    ys = [p[1] for p in pts]
    y0, y1 = int(math.floor(min(ys))), int(math.ceil(max(ys)))
    rows = {}
    for y in range(y0, y1 + 1):
        xs = []
        for i in range(len(pts)):
            (ax, ay), (bx, by) = pts[i], pts[(i + 1) % len(pts)]
            if (ay <= y < by) or (by <= y < ay):
                xs.append(ax + (y - ay) * (bx - ax) / (by - ay))
        xs.sort()
        rows[y] = xs
        for i in range(0, len(xs) - 1, 2):
            a, b = int(round(xs[i])), int(round(xs[i + 1]))
            for x in range(a, b + 1):
                col = mid
                if shade:
                    if x <= a + (b - a) * 0.22:
                        col = hi
                    elif x >= a + (b - a) * 0.72:
                        col = lo
                px(c, x, y, col, ol)
    return rows


def outline(c, deep=0.0):
    """ferme la silhouette avec le ton de contour de la matiere voisine"""
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


def spot(c, p, col):
    px(c, p[0], p[1], col, None)


# ---------------------------------------------------------------- profils de lame
def leaf(wmax, base=0.18):
    """elfe : etroite au talon, ventre aux deux tiers, pointe fine"""
    return lambda t: max(0.0, wmax * (math.sin(math.pi * min(1, t ** 0.78)) ** 0.62) + base * (1 - t))


def wedge(wmax):
    """nain : s'elargit franchement puis coupe net en pointe courte"""
    return lambda t: wmax * (0.45 + 0.55 * min(1.0, t / 0.72)) if t < 0.78 else wmax * max(0.0, (1 - t) / 0.22)


def straight(wmax):
    """homme : bords paralleles, pointe sur le dernier quart"""
    return lambda t: wmax if t < 0.74 else wmax * max(0.0, (1 - t) / 0.26) ** 0.7


def hook(wmax):
    """uruk : fin au talon, de plus en plus large vers la pointe"""
    return lambda t: wmax * (0.35 + 0.8 * t) if t < 0.86 else wmax * max(0.0, (1 - t) / 0.14) * 1.1


# ---------------------------------------------------------------- pieces de poignee
def elf_hilt(c, base, ang, metal, wood, length=9.0, gem=None):
    """garde en ailes relevees + manche fuseau"""
    ux, uy = math.cos(ang), math.sin(ang)
    nx, ny = -uy, ux
    for s in (-1, 1):
        a = (base[0] + nx * s * 1.2, base[1] + ny * s * 1.2)
        b = (base[0] + nx * s * 5.2 - ux * 1.0, base[1] + ny * s * 5.2 - uy * 1.0)
        m = (base[0] + nx * s * 3.4 + ux * 1.8, base[1] + ny * s * 3.4 + uy * 1.8)
        stroke(c, bez(a, m, b), metal, 1.6)
    e = (base[0] - ux * length, base[1] - uy * length)
    stroke(c, [(base[0] - ux * i * 0.4, base[1] - uy * i * 0.4) for i in range(int(length / 0.4))],
           wood, 2.6)
    stroke(c, bez((e[0] + ux * 1.2, e[1] + uy * 1.2), (e[0], e[1]), (e[0] - ux * 1.0, e[1] - uy * 1.0)), metal, 3.0)
    if gem:
        spot(c, (e[0] - ux * 0.4, e[1] - uy * 0.4), gem[0])
    return e


def dwa_hilt(c, base, ang, metal, grip, length=8.0):
    """garde : gros bloc rectangulaire ; manche a trois anneaux ; pommeau large"""
    ux, uy = math.cos(ang), math.sin(ang)
    nx, ny = -uy, ux
    a = (base[0] + nx * 4.6 + ux * 0.6, base[1] + ny * 4.6 + uy * 0.6)
    b = (base[0] - nx * 4.6 + ux * 0.6, base[1] - ny * 4.6 + uy * 0.6)
    poly(c, [(a[0] - ux * 2.2, a[1] - uy * 2.2), (a[0], a[1]), (b[0], b[1]),
             (b[0] - ux * 2.2, b[1] - uy * 2.2)], metal)
    e = (base[0] - ux * length, base[1] - uy * length)
    stroke(c, [(base[0] - ux * (2 + i * 0.4), base[1] - uy * (2 + i * 0.4)) for i in range(int(length / 0.4))],
           grip, 3.2)
    for i in (2.8, 5.0, 7.0):
        p0 = (base[0] - ux * i + nx * 2.0, base[1] - uy * i + ny * 2.0)
        p1 = (base[0] - ux * i - nx * 2.0, base[1] - uy * i - ny * 2.0)
        seg(c, p0, p1, metal[1], metal[3])
    poly(c, [(e[0] + nx * 3.0, e[1] + ny * 3.0), (e[0] - nx * 3.0, e[1] - ny * 3.0),
             (e[0] - nx * 2.2 - ux * 2.4, e[1] - ny * 2.2 - uy * 2.4),
             (e[0] + nx * 2.2 - ux * 2.4, e[1] + ny * 2.2 - uy * 2.4)], metal)
    return e


def man_hilt(c, base, ang, metal, grip, length=9.0, cross=6.0, round_pommel=True):
    """longue garde droite en croix, manche a lanieres croisees, pommeau rond"""
    ux, uy = math.cos(ang), math.sin(ang)
    nx, ny = -uy, ux
    stroke(c, [(base[0] + nx * s * 0.4, base[1] + ny * s * 0.4) for s in range(-int(cross / 0.4), int(cross / 0.4))],
           metal, 2.2)
    e = (base[0] - ux * length, base[1] - uy * length)
    stroke(c, [(base[0] - ux * (1.6 + i * 0.4), base[1] - uy * (1.6 + i * 0.4)) for i in range(int(length / 0.4))],
           grip, 2.6)
    for i in range(1, 5):                                  # lanieres
        p = (base[0] - ux * (2.0 + i * 1.6), base[1] - uy * (2.0 + i * 1.6))
        seg(c, (p[0] + nx * 1.4 - ux * 0.6, p[1] + ny * 1.4 - uy * 0.6),
            (p[0] - nx * 1.4 + ux * 0.6, p[1] - ny * 1.4 + uy * 0.6), grip[2], grip[3])
    if round_pommel:
        poly(c, [(e[0] + nx * 2.4, e[1] + ny * 2.4), (e[0] - ux * 1.6 + nx * 1.7, e[1] - uy * 1.6 + ny * 1.7),
                 (e[0] - ux * 2.4, e[1] - uy * 2.4), (e[0] - ux * 1.6 - nx * 1.7, e[1] - uy * 1.6 - ny * 1.7),
                 (e[0] - nx * 2.4, e[1] - ny * 2.4)], metal)
    return e


def uru_hilt(c, base, ang, metal, grip, length=8.0):
    """garde de travers avec un crochet, manche entoure de chiffons"""
    ux, uy = math.cos(ang), math.sin(ang)
    nx, ny = -uy, ux
    a = (base[0] + nx * 4.4 - ux * 0.4, base[1] + ny * 4.4 - uy * 0.4)
    b = (base[0] - nx * 2.6 + ux * 1.2, base[1] - ny * 2.6 + uy * 1.2)
    stroke(c, bez(a, base, b), metal, 2.0)
    stroke(c, bez(a, (a[0] + ux * 2.4, a[1] + uy * 2.4), (a[0] + nx * 1.0 + ux * 3.4, a[1] + ny * 1.0 + uy * 3.4)),
           metal, 1.4)                                     # crochet
    e = (base[0] - ux * length, base[1] - uy * length)
    stroke(c, [(base[0] - ux * (1.4 + i * 0.4), base[1] - uy * (1.4 + i * 0.4)) for i in range(int(length / 0.4))],
           grip, 2.8)
    for i in range(0, 5):                                  # chiffons
        p = (base[0] - ux * (2.4 + i * 1.5), base[1] - uy * (2.4 + i * 1.5))
        seg(c, (p[0] + nx * 1.6, p[1] + ny * 1.6), (p[0] - nx * 1.6 - ux * 0.8, p[1] - ny * 1.6 - uy * 0.8),
            grip[0] if i % 2 else grip[2], grip[3])
    poly(c, [(e[0] + nx * 2.0, e[1] + ny * 2.0), (e[0] - nx * 2.0, e[1] - ny * 2.0),
             (e[0] - nx * 1.4 - ux * 2.0, e[1] - ny * 1.4 - uy * 2.0),
             (e[0] + nx * 1.4 - ux * 2.0, e[1] + ny * 1.4 - uy * 2.0)], metal)
    return e


# ---------------------------------------------------------------- epees
def elven_sword(steel=ELF_STEEL, metal=ELF_GOLD, wood=ELF_WOOD, big=False, gem=None):
    c = new()
    base = (10.0, 21.0) if not big else (9.0, 22.0)
    tip = (27.0, 4.0) if not big else (29.0, 2.0)
    mid = ((base[0] + tip[0]) / 2 - 1.4, (base[1] + tip[1]) / 2 - 1.4)   # legere courbe
    path = bez(base, mid, tip, 90)
    taper(c, path, leaf(2.9 if not big else 3.3), steel)
    ang = math.atan2(tip[1] - base[1], tip[0] - base[0])
    elf_hilt(c, base, ang, metal, wood, length=9.5 if big else 8.4, gem=gem)
    for i, t in enumerate((0.18, 0.28)):                                 # gravure
        p = path[int(t * 90)]
        spot(c, (p[0] + 0.4, p[1] + 0.4), metal[1])
    outline(c)
    return c


def dwarven_sword(steel=DWA_STEEL, metal=DWA_BRONZE, grip=DWA_GRIP):
    c = new()
    base, tip = (10.0, 22.0), (24.0, 8.0)
    path = bez(base, ((base[0] + tip[0]) / 2, (base[1] + tip[1]) / 2), tip, 70)
    taper(c, path, wedge(4.0), steel)
    ang = math.atan2(tip[1] - base[1], tip[0] - base[0])
    dwa_hilt(c, base, ang, metal, grip, length=8.0)
    for t in (0.35, 0.5, 0.65):                                          # runes
        p = path[int(t * 70)]
        spot(c, (p[0] - 0.6, p[1] - 0.6), metal[1])
    outline(c)
    return c


def man_sword(steel=MAN_STEEL, metal=MAN_DARK, grip=MAN_GRIP, great=False, gem=None):
    c = new()
    base = (9.0, 22.0) if not great else (8.0, 23.0)
    tip = (26.0, 5.0) if not great else (28.0, 3.0)
    path = bez(base, ((base[0] + tip[0]) / 2, (base[1] + tip[1]) / 2), tip, 80)
    taper(c, path, straight(2.6 if not great else 3.6), steel, fuller=0.72)
    ang = math.atan2(tip[1] - base[1], tip[0] - base[0])
    man_hilt(c, base, ang, metal, grip, length=8.0 if not great else 11.0, cross=5.4 if not great else 7.0)
    if gem:
        spot(c, (base[0] - 0.4, base[1] - 0.4), gem[0])
    outline(c)
    return c


def uruk_scimitar(steel=URU_STEEL, metal=URU_RUST, grip=URU_GRIP):
    c = new()
    base, tip = (10.0, 22.0), (27.0, 7.0)
    path = bez3(base, (17.0, 19.0), (25.0, 16.0), tip, 90)               # dos tres courbe
    taper(c, path, hook(3.4), steel, serrate=lambda t: 0.55 < t < 0.95 and int(t * 26) % 2 == 0)
    ang = math.atan2(-6, 8)
    uru_hilt(c, base, ang, metal, grip, length=7.5)
    outline(c)
    return c


def dagger(race, steel, metal, third):
    c = new()
    if race == 'elf':
        base, tip = (11.0, 20.0), (23.0, 8.0)
        taper(c, bez(base, (16.0, 13.4), tip, 60), leaf(2.2), steel)
        elf_hilt(c, base, math.atan2(-1, 1), metal, third, length=6.0)
    else:
        base, tip = (11.0, 21.0), (23.0, 9.0)
        taper(c, bez(base, (17.0, 15.0), tip, 60), straight(1.9), steel, fuller=0.7)
        man_hilt(c, base, math.atan2(-1, 1), metal, third, length=6.0, cross=3.4)
    outline(c)
    return c


# ---------------------------------------------------------------- hampes
def elven_spear(steel=ELF_STEEL, metal=ELF_GOLD, wood=ELF_WOOD):
    c = new()
    a, b = (3.0, 29.0), (22.0, 10.0)
    stroke(c, [(a[0] + (b[0] - a[0]) * i / 60, a[1] + (b[1] - a[1]) * i / 60) for i in range(61)], wood, 2.4)
    for i in (0.25, 0.5):                                                # anneaux d'or
        p = (a[0] + (b[0] - a[0]) * i, a[1] + (b[1] - a[1]) * i)
        seg(c, (p[0] - 1.4, p[1] - 1.4), (p[0] + 1.4, p[1] + 1.4), metal[1], metal[3])
    taper(c, bez((21.0, 11.0), (25.0, 7.0), (29.0, 2.0), 50), leaf(2.4), steel)
    stroke(c, bez((19.4, 12.6), (21.0, 11.0), (22.6, 9.4)), metal, 2.4)  # douille
    outline(c)
    return c


def man_spear(steel=MAN_STEEL, metal=MAN_DARK, wood=MAN_GRIP, royal=False):
    c = new()
    a, b = (3.0, 29.0), (21.0, 11.0)
    stroke(c, [(a[0] + (b[0] - a[0]) * i / 60, a[1] + (b[1] - a[1]) * i / 60) for i in range(61)],
           wood if not royal else DARKWOOD, 2.8)
    taper(c, bez((20.0, 12.0), (24.0, 8.0), (29.0, 3.0), 50), straight(2.2), steel, fuller=0.75)
    poly(c, [(19.0, 13.0), (22.6, 9.4), (24.0, 10.8), (20.4, 14.4)], metal)   # collet
    for s in (1, -1):                                                         # ailerons
        poly(c, [(21.0, 11.0), (21.0 + 3.0 * s, 11.0 + 3.0 * s),
                 (22.4 + 2.2 * s, 9.6 + 2.2 * s), (22.4, 9.6)], steel)
    if royal:
        spot(c, (18.0, 14.0), GLOW_GOLD[0])
        spot(c, (7.0, 25.0), GLOW_GOLD[1])
        seg(c, (5.0, 27.0), (8.0, 24.0), GLOW_GOLD[1], GLOW_GOLD[3])
    outline(c)
    return c


def uruk_spear(steel=URU_STEEL, metal=URU_RUST, grip=URU_GRIP):
    c = new()
    path = bez3((3.0, 29.0), (9.0, 22.0), (15.0, 19.0), (21.0, 11.0), 70)     # hampe de travers
    stroke(c, path, DARKWOOD, 2.6)
    taper(c, bez((20.0, 12.0), (24.0, 9.0), (29.0, 3.0), 50), hook(2.2), steel,
          serrate=lambda t: 0.3 < t < 0.9 and int(t * 18) % 2 == 0)
    poly(c, [(19.6, 12.6), (22.0, 10.2), (23.2, 11.4), (20.8, 13.8)], metal)
    for i in range(3):                                                        # chiffons
        p = path[40 + i * 8]
        seg(c, (p[0] + 1.6, p[1] + 1.0), (p[0] - 1.6, p[1] - 1.0), grip[0] if i % 2 else grip[2], grip[3])
    poly(c, [(16.0, 16.0), (19.0, 15.0), (18.0, 17.6)], BONE)                 # os accroche
    outline(c)
    return c


# ---------------------------------------------------------------- haches, marteaux, masses
def dwarven_axe(steel=DWA_STEEL, metal=DWA_BRONZE, grip=DWA_GRIP):
    c = new()
    shaft = [(5.0, 28.0 - i * 0.58) for i in range(0, 1)]
    path = [(5.0 + i * 0.36, 28.0 - i * 0.40) for i in range(45)]
    stroke(c, path, grip, 3.0)
    top = path[-1]
    poly(c, [(top[0] - 0.5, top[1] + 1.0), (top[0] + 1.5, top[1] - 7.5), (top[0] + 9.0, top[1] - 5.0),
             (top[0] + 8.0, top[1] + 3.0)], steel)                      # grande lame droite
    poly(c, [(top[0] - 0.5, top[1] + 0.5), (top[0] - 1.5, top[1] - 6.5), (top[0] - 7.0, top[1] - 4.0),
             (top[0] - 6.0, top[1] + 2.0)], steel)                      # lame gauche (double)
    poly(c, [(top[0] - 2.4, top[1] - 1.2), (top[0] + 2.4, top[1] - 1.2),
             (top[0] + 2.4, top[1] + 2.6), (top[0] - 2.4, top[1] + 2.6)], metal)
    seg(c, (5.6, 26.0), (7.4, 26.0), metal[1], metal[3])
    outline(c)
    return c


def dwarven_hammer(steel=DWA_STEEL, metal=DWA_BRONZE, grip=DWA_GRIP):
    c = new()
    path = [(6.0 + i * 0.34, 28.0 - i * 0.40) for i in range(42)]
    stroke(c, path, grip, 3.2)
    t = path[-1]
    poly(c, [(t[0] - 7.5, t[1] - 3.0), (t[0] - 5.0, t[1] - 7.5), (t[0] + 6.5, t[1] - 4.0),
             (t[0] + 4.5, t[1] + 1.5), (t[0] - 6.0, t[1] + 1.0)], steel)     # tete massive
    poly(c, [(t[0] - 7.5, t[1] - 3.0), (t[0] - 6.0, t[1] + 1.0), (t[0] - 9.0, t[1] + 0.0),
             (t[0] - 9.5, t[1] - 2.6)], metal)                               # panne bronze
    seg(c, (t[0] - 3.4, t[1] - 4.6), (t[0] + 3.0, t[1] - 2.4), metal[1], metal[3])
    seg(c, (t[0] - 3.0, t[1] - 2.8), (t[0] + 2.4, t[1] - 0.8), metal[2], metal[3])
    seg(c, (7.0, 25.6), (9.2, 25.6), metal[1], metal[3])
    outline(c)
    return c


def dwarven_pick(steel=DWA_STEEL, metal=DWA_BRONZE, grip=DWA_GRIP):
    c = new()
    path = [(6.0 + i * 0.34, 28.0 - i * 0.40) for i in range(42)]
    stroke(c, path, grip, 3.0)
    t = path[-1]
    taper(c, bez((t[0] - 1.0, t[1] - 1.0), (t[0] + 5.0, t[1] - 5.0), (t[0] + 10.0, t[1] - 4.0), 50),
          lambda u: 3.0 * (1 - u) + 0.4, steel)                              # bec long
    poly(c, [(t[0] - 1.0, t[1] - 2.0), (t[0] - 7.0, t[1] - 5.0), (t[0] - 7.5, t[1] - 1.5),
             (t[0] - 1.5, t[1] + 1.0)], steel)                               # contre-bec
    poly(c, [(t[0] - 2.4, t[1] - 2.2), (t[0] + 2.0, t[1] - 2.8), (t[0] + 2.4, t[1] + 1.4),
             (t[0] - 2.2, t[1] + 1.8)], metal)
    seg(c, (7.0, 25.6), (9.2, 25.6), metal[1], metal[3])
    outline(c)
    return c


def uruk_mace(steel=URU_STEEL, metal=URU_RUST, grip=URU_GRIP):
    c = new()
    path = bez3((4.0, 29.0), (9.0, 23.0), (13.0, 20.0), (17.0, 14.0), 60)
    stroke(c, path, DARKWOOD, 3.0)
    for i in range(3):
        p = path[20 + i * 10]
        seg(c, (p[0] + 1.8, p[1] + 1.0), (p[0] - 1.8, p[1] - 1.0), grip[1], grip[3])
    cx, cy = 21.0, 10.0
    for a in range(0, 360, 45):                                              # pointes
        r = math.radians(a)
        poly(c, [(cx + math.cos(r) * 7.4, cy + math.sin(r) * 7.4),
                 (cx + math.cos(r + 0.5) * 3.6, cy + math.sin(r + 0.5) * 3.6),
                 (cx + math.cos(r - 0.5) * 3.6, cy + math.sin(r - 0.5) * 3.6)], metal)
    poly(c, [(cx + math.cos(math.radians(a)) * 4.6, cy + math.sin(math.radians(a)) * 4.6)
             for a in range(0, 360, 30)], steel)                             # boule
    spot(c, (cx - 2, cy - 2), steel[0])
    outline(c)
    return c


def uruk_cleaver(steel=URU_STEEL, metal=URU_RUST, grip=URU_GRIP):
    c = new()
    poly(c, [(11.0, 20.0), (15.0, 6.0), (26.0, 8.0), (27.0, 14.0), (17.0, 23.0)], steel)
    for y in range(8, 22, 3):                                                # tranchant ebreche
        px(c, 26.0 - (y - 8) * 0.68, y, None)
        px(c, 25.4 - (y - 8) * 0.68, y, steel[2], steel[3])
    seg(c, (13.0, 16.0), (22.0, 10.0), steel[0], None)                       # reflet
    poly(c, [(12.0, 22.0), (10.0, 19.0), (13.0, 17.0), (15.0, 20.0)], metal)
    uru_hilt(c, (12.6, 20.6), math.atan2(-1, 1.1), metal, grip, length=7.0)
    outline(c)
    return c


def uruk_machete(steel=URU_STEEL, metal=URU_RUST, grip=URU_GRIP):
    c = new()
    base, tip = (10.0, 22.0), (25.0, 7.0)
    path = bez(base, (18.0, 14.0), tip, 70)
    taper(c, path, lambda t: 1.4 + 2.2 * t if t < 0.8 else 3.2 * max(0.0, (1 - t) / 0.2), steel)
    poly(c, [(22.0, 11.0), (27.0, 4.0), (28.0, 8.0), (24.0, 13.0)], steel)   # bec releve
    for i in range(4):                                                       # dents de scie
        p = path[20 + i * 9]
        px(c, p[0] - 1.6, p[1] - 1.6, steel[0], steel[3])
    uru_hilt(c, base, math.atan2(-1, 1), metal, grip, length=7.0)
    outline(c)
    return c


# ---------------------------------------------------------------- arcs et arbaletes
def elven_bow(wood=ELF_WOOD, metal=ELF_GOLD, string=ELF_STEEL, long=False, ornate=False):
    c = new()
    y0, y1 = (2.0, 29.0) if long else (4.0, 27.0)
    belly = 7.0 if long else 9.0
    top, bot = (22.0, y0), (22.0, y1)
    mid = ((y0 + y1) / 2)
    arc = bez3(top, (belly - 3.0, y0 + 5.0), (belly - 3.0, y1 - 5.0), bot, 90)   # double courbure
    stroke(c, arc, wood, 2.6)
    stroke(c, bez((22.0, y0), (26.0, y0 + 2.0), (24.0, y0 + 5.0)), metal, 2.0)   # pointes relevees
    stroke(c, bez((22.0, y1), (26.0, y1 - 2.0), (24.0, y1 - 5.0)), metal, 2.0)
    seg(c, (24.0, y0 + 4.0), (24.0, y1 - 4.0), string[0], None)
    for i in range(int(y0) + 4, int(y1) - 3, 2):
        px(c, 24, i, string[1], None)
    stroke(c, [(belly - 3.0 + 0.0, mid - 3.0 + i * 0.5) for i in range(13)], metal, 2.8)   # poignee
    if ornate:
        for p in ((belly - 4.0, mid - 6.0), (belly - 4.0, mid + 6.0)):
            spot(c, p, GLOW_GOLD[0])
        spot(c, (belly - 2.0, mid), GLOW_BLUE[0])
    outline(c)
    return c


def man_bow(wood=MAN_GRIP, string=MAN_STEEL):
    c = new()
    y0, y1 = 2.0, 29.0
    arc = bez((21.0, y0), (7.0, 15.5), (21.0, y1), 90)            # grand arc simple en D
    stroke(c, arc, wood, 3.0)
    seg(c, (21.0, y0), (21.0, y1), string[0], None)
    for i in range(int(y0), int(y1), 2):
        px(c, 21, i, string[2], None)
    stroke(c, [(7.6, 13.0 + i * 0.5) for i in range(11)], LEATHER, 3.4)   # poignee de cuir
    for p in ((21.0, y0 + 1.0), (21.0, y1 - 1.0)):
        spot(c, p, MAN_DARK[1])
    outline(c)
    return c


def dwarven_bow(wood=DARKWOOD, metal=DWA_BRONZE, string=DWA_STEEL):
    c = new()
    y0, y1 = 6.0, 25.0
    arc = bez3((20.0, y0), (10.0, y0 + 3.0), (10.0, y1 - 3.0), (20.0, y1), 80)   # court et trapu
    stroke(c, arc, wood, 4.0)
    for yy in (y0 + 2.0, y1 - 2.0):                                              # ferrures
        stroke(c, [(15.0 + i * 0.4, yy + (1 if yy < 15 else -1) * i * 0.25) for i in range(10)], metal, 2.6)
    stroke(c, [(10.2, 13.0 + i * 0.5) for i in range(12)], metal, 3.4)           # poignee de fer
    seg(c, (20.0, y0), (20.0, y1), string[0], None)
    for i in range(int(y0), int(y1), 2):
        px(c, 20, i, string[2], None)
    for p in ((20.0, y0), (20.0, y1)):
        spot(c, p, metal[0])
    outline(c)
    return c


def dragon_crossbow(stock=BONE, limb=URU_RUST, metal=EMBER):
    c = new()
    body = [(5.0 + i * 0.44, 27.0 - i * 0.44) for i in range(38)]
    stroke(c, body, stock, 3.4)                                   # fut en os
    t = body[-1]
    arcs = bez3((t[0] - 7.0, t[1] - 8.0), (t[0] - 1.0, t[1] - 5.0), (t[0] + 3.0, t[1] - 1.0), (t[0] + 6.0, t[1] + 5.0), 70)
    stroke(c, arcs, limb, 3.0)                                    # branches
    seg(c, (t[0] - 7.0, t[1] - 8.0), (t[0] + 6.0, t[1] + 5.0), DWA_STEEL[0], None)   # corde
    poly(c, [(t[0] - 3.0, t[1] - 1.0), (t[0] + 1.0, t[1] - 4.0), (t[0] + 3.0, t[1] - 1.0),
             (t[0] - 1.0, t[1] + 2.0)], metal)                    # noix incandescente
    poly(c, [(9.0, 23.0), (12.0, 20.0), (14.0, 22.0), (11.0, 25.0)], limb)          # detente
    for p in ((t[0] - 5.0, t[1] - 6.0), (t[0] + 4.0, t[1] + 3.0), (12.0, 22.0)):
        spot(c, p, metal[0])
    outline(c)
    return c


# ---------------------------------------------------------------- uniques
def goblin_staff(wood=BONE, gem=GREEN, bind=LEATHER):
    c = new()
    path = bez3((6.0, 30.0), (10.0, 22.0), (12.0, 16.0), (14.0, 9.0), 70)   # baton tordu
    stroke(c, path, wood, 3.2)
    claw = bez((14.0, 9.0), (21.0, 7.0), (19.0, 1.0), 50)                   # griffe
    stroke(c, claw, wood, 2.2)
    claw2 = bez((14.0, 9.0), (10.0, 4.0), (14.0, 1.0), 50)
    stroke(c, claw2, wood, 2.0)
    cx, cy = 16.0, 5.0
    poly(c, [(cx + math.cos(math.radians(a)) * 3.2, cy + math.sin(math.radians(a)) * 3.2)
             for a in range(0, 360, 45)], gem)
    spot(c, (cx - 1, cy - 1), gem[0])
    spot(c, (cx + 2, cy + 2), gem[2])
    for i in range(3):
        p = path[25 + i * 14]
        seg(c, (p[0] + 2.0, p[1] + 0.8), (p[0] - 2.0, p[1] - 0.8), bind[1], bind[3])
    poly(c, [(11.0, 19.0), (14.0, 18.0), (13.0, 21.0)], BONE)               # petit crane
    outline(c)
    return c


def balrog_whip(cord=LEATHER, fire=EMBER):
    c = new()
    stroke(c, [(4.0 + i * 0.3, 29.0 - i * 0.3) for i in range(16)], DARKWOOD, 3.4)   # manche
    loop1 = bez3((9.0, 24.0), (22.0, 24.0), (26.0, 14.0), (17.0, 11.0), 90)
    loop2 = bez3((17.0, 11.0), (8.0, 8.0), (6.0, 18.0), (14.0, 19.0), 90)
    tail = bez3((14.0, 19.0), (20.0, 20.0), (24.0, 6.0), (28.0, 2.0), 90)
    for p, w in ((loop1, 2.6), (loop2, 2.2), (tail, 1.8)):
        stroke(c, p, cord, w)
    for i in range(0, 90, 9):                                                        # braises
        for p, col in ((loop1, fire[1]), (loop2, fire[2]), (tail, fire[0])):
            if (i // 9) % 2 == 0:
                spot(c, p[i], col)
    spot(c, (28.0, 2.0), fire[0])
    spot(c, (27.0, 3.0), fire[1])
    outline(c)
    return c


# ---------------------------------------------------------------- la liste
def models():
    M = {}
    M['elven_sword'] = ('Épée elfique', elven_sword())
    M['elven_dagger'] = ('Dague elfique', dagger('elf', ELF_STEEL, ELF_GOLD, ELF_WOOD))
    M['elven_bow'] = ('Arc elfique', elven_bow())
    M['elven_longbow'] = ('Arc long elfique', elven_bow(long=True))
    M['elven_spear'] = ('Lance elfique', elven_spear())

    M['dwarven_sword'] = ('Épée naine', dwarven_sword())
    M['dwarven_hammer'] = ('Marteau nain', dwarven_hammer())
    M['dwarven_axe'] = ('Hache naine', dwarven_axe())
    M['dwarven_war_pick'] = ('Pioche de combat', dwarven_pick())
    M['dwarven_bow'] = ('Arc nain', dwarven_bow())

    M['man_sword'] = ('Épée des hommes', man_sword())
    M['man_spear'] = ('Lance des hommes', man_spear())
    M['man_greatsword'] = ('Grande épée', man_sword(great=True))
    M['man_bow'] = ('Arc des hommes', man_bow())
    M['man_dagger'] = ('Dague des hommes', dagger('man', MAN_STEEL, MAN_DARK, MAN_GRIP))

    M['uruk_scimitar'] = ('Cimeterre uruk', uruk_scimitar())
    M['uruk_cleaver'] = ('Couperet uruk', uruk_cleaver())
    M['uruk_machete'] = ('Machette uruk', uruk_machete())
    M['uruk_spear'] = ('Lance uruk', uruk_spear())
    M['uruk_mace'] = ('Masse uruk', uruk_mace())

    M['gondor_guard_spear'] = ('Lance des gardes du Gondor', man_spear(steel=MAN_STEEL, metal=GLOW_GOLD, royal=True))
    M['goblin_king_staff'] = ('Bâton du roi gobelin', goblin_staff())

    M['elf_lord_sword'] = ('Épée du seigneur elfe', elven_sword(steel=GLOW_BLUE, big=True, gem=GLOW_BLUE))
    M['ancient_elven_bow'] = ('Arc des anciens elfes', elven_bow(long=True, ornate=True, string=GLOW_BLUE))
    M['gondor_king_sword'] = ('Épée du roi du Gondor', man_sword(metal=GLOW_GOLD, great=True, gem=GLOW_BLUE))
    M['rohan_sword'] = ('Épée du Rohan', man_sword(steel=DWA_STEEL, metal=DWA_BRONZE, grip=DWA_GRIP))
    M['dragon_crossbow'] = ('Arbalète du dragon', dragon_crossbow())
    M['balrog_whip'] = ('Fouet du Balrog', balrog_whip())
    return M


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
    groups = [('ARMES ELFIQUES', ids[0:5]), ('ARMES NAINES', ids[5:10]), ('ARMES DES HOMMES', ids[10:15]),
              ('ARMES URUK', ids[15:20]), ('ARMES UNIQUES', ids[20:22]), ('ARMES LEGENDAIRES', ids[22:])]
    S, PAD, TOP, LBL = 128, 18, 36, 28
    img = Image.new('RGB', (5 * (S + PAD) + PAD, len(groups) * (S + LBL + TOP) + PAD), (26, 26, 30))
    d = ImageDraw.Draw(img)
    try:
        f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 17)
        fs = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 14)
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
            d.text((x, y + TOP + S + 5), name, (0xDD, 0xDD, 0xDD), font=fs)
    img.save(out + '/planche.png')
    for k, (name, c) in M.items():
        to_image(c).resize((160, 160), Image.NEAREST).save(f'{out}/{k}.png')
    print('planche :', out + '/planche.png', '-', len(M), 'armes')


def gen():
    """ecrit les icones dans le mod (a n'appeler que quand on passe aux objets)"""
    A = 'src/main/resources/assets/ringsofash'
    for k, (name, c) in models().items():
        to_image(c).save(f'{A}/textures/item/{k}.png')
    print('icones :', len(models()))


if __name__ == '__main__':
    (gen if len(sys.argv) > 1 and sys.argv[1] == 'gen' else preview)()
