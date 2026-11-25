"""Les armes du mod, en icones 64x64 dessinees a la maniere de Minecraft.

Methode : chaque arme est d'abord dessinee bien droite (rangee par rangee, la pointe
en haut), puis penchee a 45 degres exactement, une rangee decalee d'un pixel. C'est
comme ca que sont faites les icones vanilla : les bords tombent en escalier regulier
au lieu de partir de travers.

    python3 tools/weapons2d.py preview   -> planche de contact dans /tmp/armes2d
    python3 tools/weapons2d.py gen       -> icones dans le mod
"""
import math
import os
import sys

OUT = 64

# ----------------------------------------------- matieres (clair, moyen, sombre, contour)
SILVER = ((0xF2, 0xF7, 0xFA), (0xC4, 0xD2, 0xDA), (0x8A, 0x9C, 0xA8), (0x42, 0x50, 0x5A))
SILVER_D = ((0xB6, 0xC4, 0xCE), (0x8A, 0x98, 0xA4), (0x5E, 0x6A, 0x76), (0x2C, 0x34, 0x3C))
STEEL = ((0xEE, 0xF2, 0xF6), (0xBA, 0xC2, 0xCA), (0x82, 0x8C, 0x98), (0x3A, 0x42, 0x4C))
DARKSTEEL = ((0x82, 0x8A, 0x94), (0x5A, 0x62, 0x6C), (0x3A, 0x40, 0x48), (0x1A, 0x1E, 0x24))
IRON_BLACK = ((0x6E, 0x66, 0x58), (0x4C, 0x45, 0x3A), (0x30, 0x2A, 0x22), (0x14, 0x11, 0x0C))
BRASS = ((0xF0, 0xCC, 0x76), (0xC6, 0x9A, 0x44), (0x88, 0x66, 0x26), (0x3E, 0x2E, 0x0E))
BRONZE = ((0xE2, 0xB0, 0x5E), (0xB4, 0x82, 0x36), (0x76, 0x52, 0x1E), (0x36, 0x24, 0x0C))
WOOD = ((0xA6, 0x78, 0x48), (0x7C, 0x56, 0x30), (0x54, 0x38, 0x1E), (0x26, 0x18, 0x0C))
PALEWOOD = ((0xE0, 0xC2, 0x90), (0xBC, 0x9A, 0x68), (0x8E, 0x70, 0x46), (0x44, 0x34, 0x1E))
DARKWOOD = ((0x74, 0x60, 0x46), (0x52, 0x42, 0x30), (0x36, 0x2A, 0x1E), (0x16, 0x12, 0x0C))
ELFWOOD = ((0x90, 0x88, 0x6C), (0x66, 0x60, 0x4A), (0x44, 0x3E, 0x30), (0x1E, 0x1A, 0x14))
LEATHER = ((0x8E, 0x4E, 0x34), (0x66, 0x36, 0x22), (0x44, 0x22, 0x14), (0x1E, 0x10, 0x0A))
DARKLEATHER = ((0x50, 0x3E, 0x2C), (0x38, 0x2A, 0x1E), (0x24, 0x1A, 0x12), (0x0E, 0x0A, 0x06))
GREEN = ((0x86, 0xA8, 0x52), (0x5E, 0x7C, 0x36), (0x3C, 0x54, 0x20), (0x1A, 0x26, 0x0E))
STRING = ((0xF0, 0xEC, 0xE0), (0xC6, 0xC0, 0xB0), (0x92, 0x8C, 0x7E), (0x46, 0x42, 0x3A))


# ---------------------------------------------------------------- toile droite
class V:
    """toile verticale : rangee 0 = la pointe, derniere rangee = le bout du manche"""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.g = [[None] * w for _ in range(h)]

    def put(self, col, row, c, out=None):
        col, row = int(round(col)), int(round(row))
        if 0 <= col < self.w and 0 <= row < self.h and c is not None:
            self.g[row][col] = (c, out)

    def span(self, row, a, b, pal, shade=True):
        """remplit une rangee : clair a gauche, sombre a droite"""
        a, b = int(round(a)), int(round(b))
        row = int(round(row))
        if b < a or not (0 <= row < self.h):
            return
        for x in range(a, b + 1):
            if not shade or b == a:
                col = pal[1]
            elif x == a or (x == a + 1 and b - a >= 5):
                col = pal[0]
            elif x == b:
                col = pal[2]
            else:
                col = pal[1]
            self.put(x, row, col, pal[3])

    def band(self, r0, r1, lf, rf, pal, shade=True):
        """bande entre deux rangees ; bords gauche et droit = nombre ou fonction de t"""
        n = max(1, r1 - r0)
        for r in range(r0, r1):
            t = (r - r0) / n
            a = lf(t) if callable(lf) else lf
            b = rf(t) if callable(rf) else rf
            self.span(r, a, b, pal, shade)

    def blade(self, r0, r1, cf, wf, pal, shade=True):
        self.band(r0, r1,
                  lambda t: (cf(t) if callable(cf) else cf) - (wf(t) if callable(wf) else wf),
                  lambda t: (cf(t) if callable(cf) else cf) + (wf(t) if callable(wf) else wf), pal, shade)

    def vline(self, col, r0, r1, c, out=None):
        for r in range(r0, r1):
            if 0 <= r < self.h and 0 <= int(col) < self.w and self.g[r][int(col)] is not None:
                self.put(col, r, c, out)

    def hline(self, row, a, b, c, out=None):
        row = int(round(row))
        for x in range(int(round(a)), int(round(b)) + 1):
            if 0 <= x < self.w and 0 <= row < self.h and self.g[row][x] is not None:
                self.put(x, row, c, out)

    def erase(self, col, row):
        if 0 <= col < self.w and 0 <= row < self.h:
            self.g[row][col] = None


def lerp(a, b, p=1.0):
    return lambda t: a + (b - a) * (t ** p)


# ---------------------------------------------------------------- mise en page
def tilt(v, dx=0, dy=0):
    """penche la toile a 45 degres : une rangee = un pixel de decalage"""
    out = [[None] * OUT for _ in range(OUT)]
    w, h = v.w + v.h - 1, v.h
    ox, oy = (OUT - w) // 2 + dx, (OUT - h) // 2 + dy
    for r in range(v.h):
        for col in range(v.w):
            cell = v.g[r][col]
            if cell:
                x, y = ox + col + (v.h - 1 - r), oy + r
                if 0 <= x < OUT and 0 <= y < OUT:
                    out[y][x] = cell
    return out


def upright(v, dx=0, dy=0):
    """garde la toile droite (les arcs) et la centre"""
    out = [[None] * OUT for _ in range(OUT)]
    ox, oy = (OUT - v.w) // 2 + dx, (OUT - v.h) // 2 + dy
    for r in range(v.h):
        for col in range(v.w):
            if v.g[r][col]:
                x, y = ox + col, oy + r
                if 0 <= x < OUT and 0 <= y < OUT:
                    out[y][x] = v.g[r][col]
    return out


def outline(c):
    add = {}
    for y in range(OUT):
        for x in range(OUT):
            if c[y][x] is not None:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nb = c[y + dy][x + dx] if 0 <= x + dx < OUT and 0 <= y + dy < OUT else None
                if nb and nb[1]:
                    add[(x, y)] = nb[1]
                    break
    for (x, y), col in add.items():
        c[y][x] = (col, None)
    return c


class C:
    """toile a l'echelle de l'icone : pour les armes a tete (manche en escalier, tete droite)"""

    def __init__(self):
        self.g = [[None] * OUT for _ in range(OUT)]

    def put(self, x, y, c, out=None):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < OUT and 0 <= y < OUT and c is not None:
            self.g[y][x] = (c, out)

    def span(self, y, a, b, pal, shade=True):
        a, b = int(round(a)), int(round(b))
        for x in range(a, b + 1):
            if not shade or b == a:
                col = pal[1]
            elif x == a or (x == a + 1 and b - a >= 6):
                col = pal[0]
            elif x == b:
                col = pal[2]
            else:
                col = pal[1]
            self.put(x, y, col, pal[3])

    def rect(self, y0, y1, lf, rf, pal, shade=True):
        n = max(1, y1 - y0)
        for y in range(y0, y1):
            t = (y - y0) / n
            self.span(y, lf(t) if callable(lf) else lf, rf(t) if callable(rf) else rf, pal, shade)

    def vspan(self, x, a, b, pal):
        """colonne : clair en haut, sombre en bas"""
        a, b = int(round(a)), int(round(b))
        for y in range(a, b + 1):
            col = pal[0] if y == a else (pal[2] if y == b else pal[1])
            self.put(x, y, col, pal[3])

    def hline(self, y, a, b, c, out=None):
        for x in range(int(round(a)), int(round(b)) + 1):
            if 0 <= x < OUT and 0 <= y < OUT and self.g[y][x] is not None:
                self.put(x, y, c, out)

    def vline(self, x, y0, y1, c, out=None):
        for y in range(y0, y1):
            if 0 <= y < OUT and 0 <= x < OUT and self.g[y][x] is not None:
                self.put(x, y, c, out)

    def erase(self, x, y):
        if 0 <= x < OUT and 0 <= y < OUT:
            self.g[y][x] = None

    def stair(self, x0, y0, n, w, pal, wrap=None, wrap_pal=None):
        """manche en diagonale : une rangee = un pixel de decalage (escalier regulier)"""
        for i in range(n):
            x, y = x0 - i, y0 + i
            self.span(y, x, x + w - 1, pal)
            if wrap and i % wrap == 0 and i > 1:
                self.hline(y, x, x + w - 1, wrap_pal[1] if wrap_pal else pal[2],
                           (wrap_pal or pal)[3])
        return x0 - n + 1, y0 + n - 1


# ======================================================================= ELFES
def elven_sword():
    """epee elfique : longue lame courbe a un tranchant, tout en argent"""
    v = V(18, 52)
    cf = lambda t: 5.0 + 4.6 * t ** 0.9                          # la lame s'incurve
    wf = lambda t: 0.8 + 2.2 * min(1.0, t * 2.6)
    v.blade(0, 33, cf, wf, SILVER)
    for r in range(4, 32):                                       # dos lumineux
        t = r / 33
        v.put(cf(t) - wf(t), r, SILVER[0], SILVER[3])
    for r in range(8, 31, 4):                                    # entrelacs graves
        t = r / 33
        v.put(cf(t), r, SILVER_D[1], SILVER[3])
    v.band(33, 36, 3.0, 14.0, SILVER_D)                          # garde
    v.band(36, 39, lerp(11.0, 12.0), lerp(15.0, 13.0), SILVER_D)  # aile relevee
    v.band(33, 35, 7.0, 10.0, SILVER)
    v.band(36, 46, 7.0, 10.0, SILVER_D)                          # fusee
    for r in (38, 41, 44):
        v.hline(r, 7, 10, SILVER[2], SILVER[3])
    v.band(46, 52, lerp(5.0, 6.0), lerp(12.0, 11.0), SILVER)     # pommeau evase
    v.hline(49, 7, 10, SILVER_D[1], SILVER[3])
    return outline(tilt(v))


def elven_dagger():
    """dague elfique : lame large a un tranchant, pointe coupee, bois et laiton"""
    v = V(16, 40)
    v.band(0, 4, lerp(9.0, 3.6), lerp(10.5, 11.2), STEEL)        # clip de la pointe
    v.band(4, 22, lerp(3.6, 5.0), lerp(11.2, 9.8), STEEL)        # lame
    for r in range(1, 22):                                       # dos clair
        t = max(0.0, (r - 4) / 18)
        v.put(3.6 + 1.4 * t if r >= 4 else 9.0 - 1.4 * r, r, STEEL[0], STEEL[3])
    for r in range(7, 21, 4):
        v.put(7.0, r, SILVER_D[1], STEEL[3])
    v.band(22, 25, 4.8, 10.2, BRASS)                             # collet
    v.band(25, 35, lerp(5.6, 5.2), lerp(9.4, 9.8), WOOD)         # manche
    v.vline(6, 26, 35, WOOD[0], WOOD[3])
    v.band(35, 40, lerp(4.4, 3.6), lerp(10.6, 11.4), BRASS)      # talon evase
    return outline(tilt(v))


def elven_spear():
    """lance elfique a deux fers : hampe de bronze, lames courbes aux deux bouts"""
    v = V(18, 58)
    for end in (0, 1):
        for i in range(16):
            r = i if end == 0 else 57 - i
            t = i / 15
            off = 6.0 * (1 - t) ** 1.3                            # la lame part de cote
            c = 9.0 + (off if end == 0 else -off)
            w = 0.8 + 4.0 * math.sin(math.pi * min(1.0, 0.08 + t * 0.9)) ** 0.6
            v.span(r, c - w, c + w, SILVER_D)
            if 4 < i < 12:                                        # ajour
                v.erase(int(round(c)), r)
        base = 16 if end == 0 else 42
        v.band(base, base + 3, 6.0, 12.0, BRONZE)                 # viroles
    v.band(19, 39, 7.6, 10.4, BRONZE)                            # hampe
    v.vline(7, 20, 39, BRONZE[0], BRONZE[3])
    for r in (23, 29, 35):
        v.hline(r, 7, 10, BRONZE[2], BRONZE[3])
    return outline(tilt(v))


def bow(v, wood, grip, w0, w1, bulge, wrap_n=4, gold=False, string_col=STRING, deep=0.72):
    """arc dessine droit : branches en S, poignee au milieu, corde a droite"""
    h = v.h
    sx = v.w - 3
    KEY = [(0.0, 0.04), (0.05, 0.46), (0.13, 0.38), (0.27, 0.66), (0.5, 1.0)]    # branche recourbee

    def prof(t):
        t = min(t, 1 - t)
        for i in range(len(KEY) - 1):
            (a, fa), (b, fb) = KEY[i], KEY[i + 1]
            if a <= t <= b:
                return fa + (fb - fa) * (t - a) / (b - a)
        return KEY[-1][1]

    curve = lambda t: sx - bulge * prof(t) ** deep
    for r in range(1, h - 1):
        t = (r - 1) / (h - 3)
        w = w0 + (w1 - w0) * (1 - abs(t - 0.5) * 2) ** 0.6
        if t < 0.05 or t > 0.95:
            w = max(0.6, w0 * 0.7)
        v.span(r, curve(t) - w, curve(t) + w, wood)
    mid = h // 2
    for r in range(mid - 6, mid + 7):                            # poignee
        t = (r - 1) / (h - 3)
        v.span(r, curve(t) - w1 - 0.8, curve(t) + w1 + 0.8, grip)
    for i in range(wrap_n):                                      # ligatures
        r = mid - 5 + i * 3
        t = (r - 1) / (h - 3)
        v.hline(r, curve(t) - w1 - 0.8, curve(t) + w1 + 0.8, grip[2] if i % 2 else grip[0], grip[3])
    if gold:
        for r in (mid - 18, mid + 18):
            t = (r - 1) / (h - 3)
            v.hline(r, curve(t) - 3, curve(t) + 3, BRONZE[1], BRONZE[3])
    for r in range(1, h - 1):                                    # corde
        v.put(sx, r, string_col[0] if r % 2 else string_col[1], string_col[3])
    return v


def elven_bow():
    """arc elfique : branches fines a double courbure, poignee entouree de vert"""
    return outline(upright(bow(V(22, 60), ELFWOOD, GREEN, 1.0, 2.6, 15.0, wrap_n=5)))


def elven_longbow():
    """arc long elfique : le plus grand, branches sombres a nervures d'or"""
    return outline(upright(bow(V(24, 64), IRON_BLACK, IRON_BLACK, 1.0, 3.0, 17.0, wrap_n=4, gold=True)))


# ======================================================================= NAINS
def dwarven_sword():
    """epee naine : lame droite large a pointe en ciseau, garniture d'or"""
    v = V(18, 50)
    v.band(0, 5, lerp(8.5, 4.0), lerp(9.5, 13.0), STEEL)         # pointe en ciseau
    v.band(5, 30, 4.0, 13.0, STEEL)
    v.vline(4, 5, 30, STEEL[0], STEEL[3])
    v.vline(8, 6, 30, SILVER_D[0], STEEL[3])                     # arete centrale
    v.vline(9, 6, 30, SILVER_D[2], STEEL[3])
    v.band(30, 33, 1.0, 16.0, BRONZE)                            # garde evasee
    v.band(33, 35, 3.0, 14.0, BRONZE)
    for r in (31, 32):
        v.erase(8, r), v.erase(9, r)
    v.band(35, 44, 6.0, 11.0, BRONZE)                            # manche
    for r in (36, 39, 42):
        v.hline(r, 6, 11, BRASS[0], BRASS[3])
    v.band(44, 50, lerp(4.0, 5.0), lerp(13.0, 12.0), BRONZE)     # pommeau
    v.hline(47, 6, 11, BRASS[0], BRASS[3])
    return outline(tilt(v))


def dwarven_hammer():
    """marteau de guerre nain : grosse tete a deux tables gravees, long manche"""
    c = C()
    x, y = c.stair(34, 27, 26, 5, WOOD, wrap=3, wrap_pal=DARKLEATHER)
    c.rect(y - 2, y + 4, x - 1, x + 5, DARKSTEEL)                 # talon
    c.rect(5, 8, 20, 48, DARKSTEEL)                               # tete
    c.rect(8, 24, 16, 52, STEEL)
    c.rect(24, 27, 20, 48, DARKSTEEL)
    c.rect(8, 24, 30, 38, DARKSTEEL, shade=False)                 # oeil du manche
    c.rect(24, 30, 31, 37, DARKSTEEL)                             # collier
    for xx in (21, 25, 43, 47):                                   # runes
        c.vline(xx, 11, 21, DARKSTEEL[1], STEEL[3])
    c.hline(9, 17, 51, STEEL[0], STEEL[3])
    c.hline(10, 17, 51, STEEL[0], STEEL[3])
    c.hline(22, 17, 51, DARKSTEEL[2], STEEL[3])
    return outline(c.g)


def dwarven_axe():
    """hache naine a deux tranchants : deux grands fers, colonne gravee au milieu"""
    c = C()
    x, y = c.stair(31, 29, 25, 5, WOOD, wrap=3, wrap_pal=DARKLEATHER)
    c.rect(y - 2, y + 4, x - 1, x + 5, IRON_BLACK)                # talon
    for row in range(4, 30):                                      # les deux fers
        t = (row - 4) / 25
        if t < 0.22:
            sp = 4 + 70 * t
        elif t > 0.78:
            sp = 4 + 70 * (1 - t)
        else:
            sp = 19
        c.span(row, 27 - sp, 27, IRON_BLACK)
        c.span(row, 37, 37 + sp, IRON_BLACK)
        c.put(27 - sp, row, DARKSTEEL[0], IRON_BLACK[3])
        c.put(37 + sp, row, DARKSTEEL[1], IRON_BLACK[3])
    for row in range(10, 24):                                     # ajours en triangle
        d = 5 - abs(row - 17)
        for k in range(max(0, d)):
            c.erase(21 - k, row)
            c.erase(43 + k, row)
    c.rect(2, 31, 27, 38, BRONZE)                                 # colonne gravee
    for row in (5, 11, 17, 23, 29):
        c.hline(row, 27, 37, IRON_BLACK[1], BRONZE[3])
    c.vline(28, 3, 31, BRASS[0], BRONZE[3])
    c.rect(31, 34, 29, 36, IRON_BLACK)
    return outline(c.g)


def dwarven_pick():
    """pioche de combat : long bec d'un cote, contre-bec court de l'autre, collier d'or"""
    c = C()
    x, y = c.stair(30, 26, 27, 5, PALEWOOD)
    c.rect(y - 2, y + 4, x - 1, x + 5, DARKSTEEL)                 # talon
    for xx in range(34, 62):                                      # long bec
        t = (xx - 34) / 27
        c.vspan(xx, 8 + 7.0 * t, 22 - 4.0 * t, STEEL)
    for xx in range(10, 32):                                      # contre-bec
        t = (32 - xx) / 22
        c.vspan(xx, 11 + 5.0 * t, 23 - 3.0 * t, STEEL)
    c.rect(10, 28, 28, 40, BRASS)                                 # collier
    c.hline(11, 28, 40, BRASS[0], BRASS[3])
    c.hline(27, 28, 40, BRONZE[2], BRASS[3])
    c.hline(12, 41, 58, STEEL[0], STEEL[3])
    c.hline(14, 12, 27, STEEL[0], STEEL[3])
    for yy in (33, 41):
        c.span(yy, 30 - (yy - 26), 34 - (yy - 26), BRASS)
    return outline(c.g)


def dwarven_bow():
    """arc nain : bois epais, ferrures, poignee gravee"""
    v = bow(V(22, 54), WOOD, DARKWOOD, 1.6, 3.6, 13.0, wrap_n=4, deep=0.8)
    mid = v.h // 2
    for r in (mid - 11, mid + 11):
        v.hline(r, 0, v.w - 1, BRONZE[1], BRONZE[3])
    return outline(upright(v))


# ======================================================================= HOMMES
def man_sword():
    """epee des hommes : lame droite a deux tranchants et gouttiere, garde en croix"""
    v = V(18, 50)
    v.band(0, 6, lerp(9.0, 5.5), lerp(9.0, 12.5), STEEL)         # pointe
    v.band(6, 31, 5.5, 12.5, STEEL)
    v.vline(5, 6, 31, STEEL[0], STEEL[3])
    v.vline(8, 7, 29, SILVER_D[2], STEEL[3])                     # gouttiere
    v.vline(9, 7, 29, SILVER_D[1], STEEL[3])
    v.band(31, 34, 1.0, 17.0, DARKSTEEL)                         # garde en croix
    v.hline(31, 1, 17, DARKSTEEL[0], DARKSTEEL[3])
    v.band(34, 44, 7.0, 11.0, LEATHER)                           # fusee
    for r in range(35, 44, 2):
        v.hline(r, 7, 11, LEATHER[2], LEATHER[3])
    v.band(44, 49, lerp(5.5, 6.5), lerp(12.5, 11.5), DARKSTEEL)  # pommeau
    return outline(tilt(v))


def man_greatsword():
    """grande epee : plus longue, plus large, poignee a deux mains"""
    v = V(20, 58)
    v.band(0, 8, lerp(10.0, 5.0), lerp(10.0, 15.0), STEEL)
    v.band(8, 36, 5.0, 15.0, STEEL)
    v.vline(5, 8, 36, STEEL[0], STEEL[3])
    v.vline(9, 9, 34, SILVER_D[2], STEEL[3])
    v.vline(10, 9, 34, SILVER_D[1], STEEL[3])
    v.vline(11, 9, 34, SILVER_D[2], STEEL[3])
    v.band(36, 39, 0.0, 19.0, DARKSTEEL)
    v.hline(36, 0, 19, DARKSTEEL[0], DARKSTEEL[3])
    v.band(39, 53, 8.0, 12.0, LEATHER)
    for r in range(40, 53, 2):
        v.hline(r, 8, 12, LEATHER[2], LEATHER[3])
    v.band(53, 58, lerp(6.5, 7.5), lerp(13.5, 12.5), DARKSTEEL)
    return outline(tilt(v))


def man_spear():
    """lance des hommes : fer en feuille a arete centrale, douille, hampe de frene"""
    v = V(14, 58)
    v.band(0, 4, lerp(7.0, 4.6), lerp(7.0, 9.4), STEEL)          # pointe
    v.band(4, 14, lerp(4.6, 3.6), lerp(9.4, 10.4), STEEL)        # feuille
    v.band(14, 18, lerp(3.6, 5.2), lerp(10.4, 8.8), STEEL)
    v.vline(4, 4, 16, STEEL[0], STEEL[3])
    v.vline(7, 2, 18, SILVER_D[1], STEEL[3])
    v.band(18, 22, 5.0, 9.0, DARKSTEEL)                          # douille
    v.band(22, 54, 5.5, 8.5, PALEWOOD)                           # hampe
    v.vline(5, 23, 54, PALEWOOD[0], PALEWOOD[3])
    v.band(54, 58, 4.0, 10.0, DARKSTEEL)
    return outline(tilt(v))


def man_dagger():
    """dague des hommes : lame a deux tranchants, garde a boules de laiton"""
    v = V(16, 38)
    v.band(0, 5, lerp(8.0, 5.6), lerp(8.0, 10.4), STEEL)
    v.band(5, 21, lerp(5.6, 6.0), lerp(10.4, 10.0), STEEL)
    v.vline(6, 5, 21, STEEL[0], STEEL[3])
    v.vline(8, 4, 20, DARKSTEEL[1], STEEL[3])
    v.band(21, 24, 3.0, 13.0, BRASS)                             # garde
    v.band(20, 22, 2.0, 14.0, BRASS)
    v.band(24, 33, 6.0, 10.0, LEATHER)                           # fusee
    for r in range(25, 33, 2):
        v.hline(r, 6, 10, LEATHER[2], LEATHER[3])
    v.band(33, 38, lerp(4.6, 5.6), lerp(11.4, 10.4), BRASS)      # pommeau
    return outline(tilt(v))


def man_bow():
    """arc des hommes : grand arc de bois clair, poignee de cuir"""
    return outline(upright(bow(V(22, 60), PALEWOOD, DARKLEATHER, 1.2, 2.8, 14.0,
                               wrap_n=4, string_col=DARKSTEEL)))


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
    im = Image.new('RGBA', (OUT, OUT), (0, 0, 0, 0))
    p = im.load()
    for y in range(OUT):
        for x in range(OUT):
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
