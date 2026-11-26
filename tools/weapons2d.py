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


# =============================================================== formes simples
# Toutes les armes sont baties sur quelques formes nettes, a la maniere des icones
# vanilla : lame droite, garde droite, manche court. Seules les couleurs et les
# proportions changent d'un peuple a l'autre. (Textures provisoires.)

def blade_weapon(steel, grip, metal, L=30, W=5, G=11, H=10, tip=5, fuller=True):
    """epee, dague, grande epee : lame droite a pointe, garde en croix, pommeau"""
    v = V(max(G + 4, W + 8), L + H + 8)
    c = v.w // 2
    for r in range(tip):                                   # la pointe
        half = (W / 2.0) * (r + 1) / tip
        v.span(r, c - half, c + half, steel)
    v.band(tip, L, c - W / 2.0, c + W / 2.0, steel)        # la lame
    v.vline(c - W // 2, tip, L, steel[0], steel[3])
    v.vline(c + W // 2, tip, L, steel[2], steel[3])
    if fuller and W >= 5:
        v.vline(c, tip + 2, L - 1, SILVER_D[1], steel[3])
    v.band(L, L + 2, c - G / 2.0, c + G / 2.0, metal)      # la garde
    v.band(L + 2, L + 2 + H, c - 1.5, c + 1.5, grip)       # le manche
    v.band(L + 2 + H, L + 4 + H, c - 2.5, c + 2.5, metal)  # le pommeau
    return outline(tilt(v))


def shaft_weapon(steel, shaft, metal, head=10, headw=5, L=44):
    """lance : hampe longue, fer en feuille"""
    v = V(headw + 8, L + head + 6)
    c = v.w // 2
    for r in range(4):
        half = (headw / 2.0) * (r + 1) / 4
        v.span(r, c - half, c + half, steel)
    v.band(4, head, c - headw / 2.0, c + headw / 2.0, steel)
    v.vline(c - headw // 2, 4, head, steel[0], steel[3])
    v.vline(c, 5, head, SILVER_D[1], steel[3])
    v.band(head, head + 2, c - 2.5, c + 2.5, metal)        # la douille
    v.band(head + 2, head + 2 + L, c - 1.5, c + 1.5, shaft)
    v.band(head + 2 + L, head + 4 + L, c - 2.0, c + 2.0, metal)
    return outline(tilt(v))


def head_weapon(steel, shaft, metal, kind='axe'):
    """hache, marteau, pioche, masse : manche en escalier, tete droite"""
    c = C()
    x, y = c.stair(32, 28, 26, 4, shaft)
    c.rect(y - 1, y + 3, x - 1, x + 4, metal)              # le talon
    if kind == 'axe':                                      # un seul fer, large
        c.rect(8, 30, lambda t: 34 - 0 * t, lambda t: 52 - 0 * t, steel)
        c.rect(6, 32, 34, 38, metal)
        c.vline(35, 8, 30, metal[0], metal[3])
        c.hline(9, 39, 51, steel[0], steel[3])
    elif kind == 'hammer':                                 # tete rectangulaire
        c.rect(10, 28, 30, 54, steel)
        c.rect(8, 30, 34, 40, metal)
        c.hline(11, 31, 53, steel[0], steel[3])
        c.hline(27, 31, 53, steel[2], steel[3])
    elif kind == 'pick':                                   # bec long et contre-bec
        for xx in range(36, 60):
            t = (xx - 36) / 23
            c.vspan(xx, 12 + 5.0 * t, 20 - 3.0 * t, steel)
        for xx in range(14, 34):
            t = (34 - xx) / 20
            c.vspan(xx, 13 + 4.0 * t, 21 - 2.0 * t, steel)
        c.rect(10, 24, 32, 40, metal)
    else:                                                  # masse : tete carree a pointes
        c.rect(10, 28, 32, 52, steel)
        c.rect(14, 24, 28, 32, steel)
        c.rect(14, 24, 52, 56, steel)
        c.rect(6, 10, 36, 48, steel)
        c.rect(28, 32, 36, 48, steel)
        c.hline(11, 33, 51, steel[0], steel[3])
    return outline(c.g)


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


def bow_weapon(wood, grip, w0=1.4, w1=3.0, bulge=14.0, h=56):
    """arc : un D simple, corde a droite, poignee de cuir"""
    return outline(upright(bow(V(22, h), wood, grip, w0, w1, bulge, wrap_n=3)))


# ---------------------------------------------------------------- la liste
def models():
    return {
        'elven_sword': ('Épée elfique', blade_weapon(SILVER, ELFWOOD, ELF_GOLD if False else BRONZE, L=32, W=5, G=11, H=10)),
        'elven_dagger': ('Dague elfique', blade_weapon(SILVER, ELFWOOD, BRONZE, L=16, W=4, G=7, H=7, tip=4, fuller=False)),
        'elven_bow': ('Arc elfique', bow_weapon(ELFWOOD, GREEN, 1.2, 2.6, 14.0, 56)),
        'elven_longbow': ('Arc long elfique', bow_weapon(ELFWOOD, GREEN, 1.2, 2.8, 16.0, 62)),
        'elven_spear': ('Lance elfique', shaft_weapon(SILVER, ELFWOOD, BRONZE, head=11, headw=5, L=42)),
        'dwarven_sword': ('Épée naine', blade_weapon(STEEL, LEATHER, BRONZE, L=26, W=7, G=13, H=9)),
        'dwarven_hammer': ('Marteau nain', head_weapon(STEEL, WOOD, DARKSTEEL, 'hammer')),
        'dwarven_axe': ('Hache naine', head_weapon(STEEL, WOOD, DARKSTEEL, 'axe')),
        'dwarven_war_pick': ('Pioche de combat', head_weapon(STEEL, PALEWOOD, BRASS, 'pick')),
        'dwarven_bow': ('Arc nain', bow_weapon(WOOD, DARKWOOD, 1.8, 3.4, 12.0, 50)),
        'man_sword': ('Épée des hommes', blade_weapon(STEEL, LEATHER, DARKSTEEL, L=30, W=5, G=11, H=9)),
        'man_spear': ('Lance des hommes', shaft_weapon(STEEL, PALEWOOD, DARKSTEEL, head=10, headw=6, L=44)),
        'man_greatsword': ('Grande épée', blade_weapon(STEEL, LEATHER, DARKSTEEL, L=38, W=7, G=15, H=13)),
        'man_bow': ('Arc des hommes', bow_weapon(PALEWOOD, DARKLEATHER, 1.4, 3.0, 14.0, 58)),
        'man_dagger': ('Dague des hommes', blade_weapon(STEEL, LEATHER, DARKSTEEL, L=15, W=4, G=7, H=7, tip=4, fuller=False)),
        'uruk_scimitar': ('Cimeterre uruk', blade_weapon(IRON_BLACK, DARKLEATHER, IRON_BLACK, L=28, W=6, G=9, H=8)),
        'uruk_cleaver': ('Couperet uruk', blade_weapon(IRON_BLACK, DARKLEATHER, IRON_BLACK, L=20, W=9, G=9, H=8, tip=3)),
        'uruk_machete': ('Machette uruk', blade_weapon(IRON_BLACK, DARKLEATHER, IRON_BLACK, L=24, W=5, G=7, H=8, fuller=False)),
        'uruk_spear': ('Lance uruk', shaft_weapon(IRON_BLACK, DARKWOOD, IRON_BLACK, head=9, headw=5, L=40)),
        'uruk_mace': ('Masse uruk', head_weapon(IRON_BLACK, DARKWOOD, IRON_BLACK, 'mace')),
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


GARDEES = ('elven_sword',)        # textures fournies par le joueur : on n'y touche pas


def gen():
    A = 'src/main/resources/assets/ringsofash'
    n = 0
    for k, (name, c) in models().items():
        if k in GARDEES:
            continue
        to_image(c).save(f'{A}/textures/item/{k}.png')
        n += 1
    print('icones ecrites :', n)


if __name__ == '__main__':
    (gen if len(sys.argv) > 1 and sys.argv[1] == 'gen' else preview)()
