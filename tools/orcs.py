"""Orques de la Hautemarche : 4 orques normaux, 2 orques noirs (marques blanches au visage), 1 chef (plastron, manches courtes, pointes de metal plantees).
Genere des .bbmodel (Blockbench, modded_entity) + textures + apercus. Coordonnees : y vers le haut, sol y=0, avant = -z, cote -x = 'left'.
Les modeles sont construits a l'echelle "orque normal" (~66 unites de haut) puis agrandis (noirs x1.1, chef x1.35)."""
import numpy as np
import trollcore as T
from trollcore import *

# ---------------------------------------------------------------- materiaux
MAT = {
    'o_leather': dict(base=(92, 62, 40), n1=7, n2=8, tint=(1, .9, .78)),
    'o_dleather': dict(base=(46, 34, 28), n1=5, n2=6, tint=(1, .92, .85)),
    'o_fur': dict(base=(100, 82, 64), n1=14, n2=10, tint=(1, .92, .8)),
    'o_iron': dict(base=(76, 80, 80), n1=7, n2=8, tint=(1, 1.02, 1.02)),
    'o_ironr': dict(base=(94, 76, 64), n1=8, n2=9, tint=(1, .9, .8)),
    'o_dark': dict(base=(40, 42, 46), n1=5, n2=6, tint=(1, 1, 1.08)),
    'o_blade': dict(base=(128, 130, 126), n1=7, n2=7, tint=(1, 1, 1)),
    'o_mail': dict(base=(74, 76, 80), n1=5, n2=6, tint=(1, 1, 1.05)),
    'o_cloth': dict(base=(90, 80, 62), n1=9, n2=8, tint=(1, .95, .85)),
    'o_tooth': dict(base=(208, 192, 142), n1=6, n2=5, tint=(1, .95, .8)),
    'o_hairk': dict(base=(26, 22, 22), n1=5, n2=6, tint=(1, 1, 1)),
    'o_hairb': dict(base=(66, 48, 34), n1=6, n2=7, tint=(1, .9, .8)),
    'o_hairg': dict(base=(104, 100, 92), n1=6, n2=7, tint=(1, 1, 1)),
    'o_tongue': dict(base=(128, 40, 48), n1=6, n2=7, tint=(1, .8, .8)),
    'o_wood': dict(base=(78, 54, 36), n1=6, n2=6, tint=(1, .9, .8)),
    'o_wrap': dict(base=(112, 84, 56), n1=7, n2=7, tint=(1, .9, .8)),
}


def skin_mat(hexcol):
    name = 'o_sk_' + hexcol
    if name not in MAT:
        c = tuple(int(hexcol[i:i + 2], 16) for i in (0, 2, 4))
        T.MATS[name] = dict(base=c, n1=7, n2=8, tint=(1, 1, 1))
        T.DECOR[name] = d_orcskin
    return name


def lerp(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def d_orcskin(arr, face, rng):
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    dark = tuple(m * .66)
    light = tuple(np.clip(m * 1.22 + 12, 0, 255))
    for _ in range(int(h * w / 22)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), dark, .32, rng)
    for _ in range(int(h * w / 45)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), light, .5)
    for _ in range(int(h * w / 70)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), tuple(m * .5), .55)
    if h * w >= 20 and rng.random() < .5:                 # cicatrice pale
        dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][int(rng.integers(0, 4))]
        n = int(rng.integers(3, max(4, min(8, max(w, h)))))
        x0, y0 = int(rng.integers(0, w)), int(rng.integers(0, h))
        line(arr, x0, y0, dx, dy, n, light, .8)
        line(arr, x0 + dy, y0 + dx, dx, dy, n, tuple(m * .45), .5)
    if h >= 4 and w >= 4 and rng.random() < .4:           # veine
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(3, h + 1)), tuple(m * .72), .5)


def d_leather2(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(max(1, h * w // 20)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (28, 20, 14), .3, rng)
    for _ in range(max(1, h * w // 40)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (150, 116, 80), .35)
    if h >= 3:
        arr[0, :] *= .85
        arr[-1, :] *= .8
    if w >= 6 and h >= 5 and rng.random() < .5:                         # coutures
        for y in range(1, h - 1, 2):
            put(arr, 1, y, (30, 22, 16), .7)


def d_fur2(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(h * w // 3):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        put(arr, x, y, (60, 46, 34) if rng.random() < .5 else (150, 128, 100), .55)
    for _ in range(h * w // 8):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, 2, (50, 38, 28), .5)


def d_iron2(arr, face, rng, rust=.4):
    """metal martele et pique : reflets sur les bords, taches de rouille, rivets"""
    h, w, _ = arr.shape
    if h >= 2 and w >= 2:
        arr[0, :] += 22
        arr[:, 0] += 12
        arr[-1, :] -= 18
        arr[:, -1] -= 12
    if h >= 2 and w >= 2:
        m = noise(h, w, 2, rng) * .5 + noise(h, w, 3, rng) * .5
        mask = m > (1 - rust * .5)
        arr[mask] = arr[mask] * .45 + np.array((130, 78, 44)) * .55
        mask2 = noise(h, w, 2, rng) > .84
        arr[mask2] = arr[mask2] * .6 + np.array((36, 34, 34)) * .4
    for _ in range(max(1, h * w // 30)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, 0 if rng.random() < .5 else 1, int(rng.integers(2, 5)), (170, 172, 168), .3)
    if w >= 6 and h >= 6:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, (176, 170, 160))


def d_dark2(arr, face, rng):
    d_iron2(arr, face, rng, rust=.15)


def d_blade2(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(max(1, h * w // 12)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(2, 7)), (176, 178, 172), .28)
    for _ in range(max(1, h * w // 25)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (96, 70, 52), .35, rng)
    if w >= 3:
        arr[:, 0] += 18
        arr[:, -1] -= 16


def d_mail2(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):
        for x in range(w):
            if (x + (y % 2)) % 2 == 0:
                put(arr, x, y, (20, 22, 26), .5)
            elif y % 2 == 0:
                put(arr, x, y, (150, 152, 158), .3)
    for _ in range(max(1, h * w // 40)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (130, 80, 46), .5)


def d_cloth2(arr, face, rng):
    h, w, _ = arr.shape
    arr[:, ::2, :] *= .94
    for _ in range(h * w // 14):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (50, 40, 30), .35, rng)
    for x in range(w):
        if rng.random() < .45:
            put(arr, x, h - 1, (30, 24, 18))
            if h > 2 and rng.random() < .5:
                put(arr, x, h - 2, (36, 28, 20))


def d_hair2(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= 1 + (rng.random() - .5) * .35
    for _ in range(h * w // 6):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (150, 146, 140), .12)


def d_tooth(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.08, .78, h)[:, None, None]


def d_wood(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= 1 + (rng.random() - .5) * .2
    for _ in range(max(1, h * w // 10)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(2, 6)), (46, 30, 20), .4)


def d_wrap(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(0, h, 2):
        arr[y] *= .78
    d_leather2(arr, face, rng)


T.MATS.update(MAT)
T.DECOR.update({'o_leather': d_leather2, 'o_dleather': d_leather2, 'o_fur': d_fur2, 'o_iron': d_iron2, 'o_ironr': lambda a, f, r: d_iron2(a, f, r, rust=.8),
                'o_dark': d_dark2, 'o_blade': d_blade2, 'o_mail': d_mail2, 'o_cloth': d_cloth2, 'o_tooth': d_tooth,
                'o_hairk': d_hair2, 'o_hairb': d_hair2, 'o_hairg': d_hair2, 'o_wood': d_wood, 'o_wrap': d_wrap, 'o_tongue': lambda a, f, r: None})


# ---------------------------------------------------------------- details de visage
def fx_eyes(col, deep=False):
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape                                   # 12 x 5
        n[0, :] *= .72                                       # arcade sombre
        if deep:                                             # orbites creusees (chef)
            for side in (0, 1):
                for x in range(0, 6):
                    for y in range(0, 4):
                        xx = x if side == 0 else w - 1 - x
                        put(n, xx, y, (20, 16, 16), .45)
        for side in (0, 1):
            for (x, y, c, a) in [(1, 1, (14, 10, 8), .9), (2, 1, col, 1), (3, 1, col, 1), (4, 2, (14, 10, 8), .9), (2, 2, (14, 10, 8), .8), (3, 2, (14, 10, 8), .6)]:
                xx = x if side == 0 else w - 1 - x
                put(n, xx, y, c, a)
            xx = 3 if side == 0 else w - 4
            put(n, xx, 1, (20, 6, 4), 1)                    # pupille fendue
    return f


def fx_mouth_big(F, rng, glow):
    """grimace du chef : large bouche sombre, crocs en haut et en bas"""
    n = F['north']
    h, w, _ = n.shape
    for x in range(1, w - 1):
        put(n, x, 2, (20, 6, 6))
        put(n, x, 3, (30, 10, 10), .7)
        if x % 2 == 1:
            put(n, x, 1, (214, 198, 150)); put(n, x, 2, (214, 198, 150), .8)
        else:
            put(n, x, 4, (214, 198, 150)); put(n, x, 3, (214, 198, 150), .8)
    n[h - 1, :] *= .75


def fx_mouth_kind(kind):
    """une bouche differente par modele : sourire denté, lèvres closes, gueule ouverte, dents clairsemées, rictus, crocs, grimace"""
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        T = (208, 192, 142)
        D = (22, 8, 8)
        R = (126, 34, 40)
        ym = h // 2
        if kind == 'grin':
            for x in range(1, w - 1):
                put(n, x, ym, D)
                put(n, x, ym - 1, T if x % 2 else D, 1)
                put(n, x, ym + 1, T if x % 2 == 0 else D, 1)
        elif kind == 'closed':
            for x in range(2, w - 2):
                put(n, x, ym, D)
                put(n, x, ym - 1, (0, 0, 0), .22)
            put(n, 1, ym + 1, D); put(n, w - 2, ym + 1, D)
            put(n, 3, ym + 1, T); put(n, w - 4, ym + 1, T)
        elif kind == 'open':
            for x in range(2, w - 2):
                for y in range(ym - 1, min(h - 1, ym + 2)):
                    put(n, x, y, D)
            for x in range(3, w - 3):
                put(n, x, ym + 1, R, .9)
            for x in range(3, w - 3, 2):
                put(n, x, ym - 1, T)
                put(n, x, ym, T, .8)
            put(n, 2, ym + 1, T); put(n, w - 3, ym + 1, T)
        elif kind == 'sparse':
            for x in range(1, w - 1):
                put(n, x, ym, D)
            for x in range(1, w - 1):
                r = rng.random()
                if r < .28:
                    put(n, x, ym - 1, T)
                elif r < .5:
                    put(n, x, ym + 1, T)
                    if rng.random() < .5:
                        put(n, x, ym + 2, T, .8)
        elif kind == 'snarl':
            for x in range(1, w - 1):
                put(n, x, ym if x < w // 2 else ym + 1, D)
                put(n, x, ym + 1 if x < w // 2 else ym + 2, (0, 0, 0), .35)
            for x in range(w // 2, w - 2, 2):
                put(n, x, ym, T); put(n, x, ym - 1, T)
            for y in range(ym - 1, h):
                put(n, 2, y, (176, 168, 150), .75)
        elif kind == 'fangs':
            for x in range(2, w - 2):
                put(n, x, ym, D)
            for x in (2, w - 3):
                for y in range(ym - 1, ym + 3):
                    put(n, x, y, T)
            for x in range(4, w - 4, 2):
                put(n, x, ym - 1, T, .8)
            put(n, 1, ym - 1, D); put(n, w - 2, ym - 1, D)
        else:                                                # 'big' : gueule beante, dents modelisees en 3D
            for x in range(1, w - 1):
                for y in range(1, h - 1):
                    put(n, x, y, (18, 5, 5), 1)
            for x in range(3, w - 3):
                put(n, x, h - 2, (110, 30, 36), .8)
        n[h - 1, :] *= .8
    return f


def fx_nose(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    put(n, 0, h - 1, (12, 8, 8), .9)
    put(n, w - 1, h - 1, (12, 8, 8), .9)
    n[h - 1, :] *= .7


def fx_mouth(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape                                       # 11 x 5
    for x in range(1, w - 1):
        put(n, x, 2, (22, 8, 8))
        if x % 2 == 1:
            put(n, x, 1, (206, 190, 140))
            put(n, x, 3, (206, 190, 140))
    n[h - 1, :] *= .8


def fx_marks(kind):
    """marques blanches des orques noirs : main sur le front (doigts vers le haut) ou bande oblique"""
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape                                   # front : 14 x 5
        white = (232, 230, 218)
        if kind == 'hand':                                  # paume sur le front, doigts vers le haut
            for x in range(4, 10):
                for y in range(2, h):
                    put(n, x, y, white, .95)
            for x in (4, 6, 8, 9):
                for y in range(0, 2):
                    put(n, x, y, white, .95)
        else:                                               # bande oblique + stries
            for i in range(h):
                for t in range(3):
                    put(n, 3 + i * 2 + t, i, white, .95)
            for x in (11, 12):
                for y in range(h):
                    put(n, x, y, white, .85)
    return f


def fx_cheekmarks(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    for y in range(2, h):
        put(n, 1, y, (232, 230, 218), .85)
        put(n, w - 2, y, (232, 230, 218), .85)


def fx_chest(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    c = w // 2
    for y in range(h - 3):
        put(n, c, y, (0, 0, 0), .3)
    for x in range(1, c - 1):
        put(n, x, h - 4, (0, 0, 0), .35)
        put(n, w - 1 - x, h - 4, (0, 0, 0), .35)
    for y in range(h - 1):
        pass


def fx_visor(F, rng, glow):
    """casque a masque : deux fentes sombres pour les yeux"""
    n = F['north']
    h, w, _ = n.shape
    for side in (0, 1):
        for (x, y) in [(2, 3), (3, 3), (4, 3), (3, 4), (4, 4)]:
            xx = x if side == 0 else w - 1 - x
            put(n, xx, y, (4, 4, 4), 1)
    n[0, :] *= .8


# ---------------------------------------------------------------- aides de construction
def C(name, x, y, z, w, h, d, mat, key=None, **kw):
    return Cube(name, x, y, z, w, h, d, mat, key=f'{key or name}_{w}x{h}x{d}_{mat}', **kw)


def X(sg, cx, w):
    return sg * cx - w / 2


def chain(name, base, length, w0, d0, rz0, drz, rx0, drx, sg=1, step=2, prof=.85, mat='o_blade', ry=0.0):
    """chaine de segments imbriques qui s'effile : oreilles, lames courbes (meme principe que les cornes du Seigneur de Cendre)"""
    n = max(2, int(round(length / step)))
    segs = []
    px, py, pz = base
    for i in range(n):
        t = i / (n - 1)
        f = max(0.0, 1 - t) ** prof
        w = max(1, int(round(w0 * f)))
        d = max(1, int(round(d0 * f)))
        rz = (rz0 if i == 0 else drz) * -sg
        rx = rx0 if i == 0 else drx
        ov = 1.0 if i else 0.0
        cube = C(f'{name}_{i}', px - w / 2, py - ov, pz - d / 2, w, step + ov, d, mat, key=f'ch{name[:3]}')
        segs.append(([cube], (px, py, pz), (rx, (ry if i == 0 else 0) * -sg, rz)))
        py += step
    g = None
    for i in range(len(segs) - 1, -1, -1):
        kids, piv, rot = segs[i]
        g = Group(f'{name}_{i}', piv, rot, kids + ([g] if g else []))
    return g


def spike(name, x, y, z, length, w, rot, mat='o_iron', d=None):
    """pointe / eclat de metal : cube allonge (pivote autour de sa base)"""
    return C(name, x - w / 2, y, z - (d or w) / 2, w, length, d or w, mat, key=f'sp{int(length)}{w}', rot=rot, pivot=(x, y, z))



# ---------------------------------------------------------------- formes de tete (une par modele)
HEADS = {
    # sw/sd : largeur et profondeur du crane ; bw/bh : arcade ; cw : joues ; nose (w,h,d) ; jaw (w,h,d,avancee) ; chin (w,h) ; tusk (ecart,longueur) ; ear
    'grunt': dict(mouth='grin', sw=14, sd=11, bw=15, bh=5, cw=13, nose=(6, 4, 3), jaw=(13, 5, 10, 0), chin=(7, 3), tusk=(5, 4.6),
                  ear=dict(len=7, w=4, rz=88, drz=-2, rx=35)),
    'furred': dict(mouth='closed', sw=11, sd=14, bw=12, bh=4, cw=10, nose=(3, 6, 4), jaw=(9, 7, 11, 1), chin=(5, 4), tusk=(2.8, 3),
                   ear=dict(len=5, w=3, rz=70, drz=0, rx=45)),
    'mail': dict(mouth='open', sw=15, sd=12, bw=15, bh=5, cw=14, nose=(5, 5, 3), jaw=(10, 4, 9, 0), chin=(5, 3), tusk=(3.2, 3),
                 ear=dict(len=16, w=5, rz=84, drz=-2, rx=28)),
    'helmed': dict(mouth='sparse', sw=12, sd=12, bw=13, bh=4, cw=11, nose=(3, 5, 3), jaw=(10, 5, 10, 0), chin=(5, 3), tusk=(4, 3.6), ear=None),
    'black1': dict(mouth='snarl', sw=15, sd=12, bw=16, bh=6, cw=14, nose=(5, 4, 3), jaw=(15, 6, 11, 0), chin=(9, 3), tusk=(4.4, 4.6),
                   ear=dict(len=8, w=4, rz=80, drz=0, rx=40)),
    'black2': dict(mouth='fangs', sw=12, sd=13, bw=13, bh=5, cw=11, nose=(4, 6, 4), jaw=(8, 6, 11, 1), chin=(4, 4), tusk=(2.4, 3.6),
                   ear=dict(len=9, w=4, rz=64, drz=8, rx=22)),
    'chief': dict(mouth='big', sw=15, sd=12, bw=17, bh=7, cw=14, nose=(7, 5, 3), jaw=(14, 6, 11, 0), chin=(8, 4), tusk=(7, 6),
                  ear=dict(len=6, w=4, rz=86, drz=0, rx=40)),
}


def fx_scarface(F, rng, glow):
    """cicatrices et fissures de la peau (chef) : grandes entailles pales avec bord sombre"""
    n = F['north']
    h, w, _ = n.shape
    for (x, y, dx, dy, ln) in [(2, 0, 1, 1, 5), (w - 4, 1, -1, 1, 4), (w // 2, 0, 0, 1, h)]:
        line(n, x, y, dx, dy, ln, (206, 214, 204), .8)
        line(n, x + 1, y, dx, dy, ln, (46, 40, 40), .5)


def head_parts(sk, eye, style):
    hs = HEADS[style.get('head', 'grunt')]
    sw, sd, bw, bh, cw = hs['sw'], hs['sd'], hs['bw'], hs['bh'], hs['cw']
    nw, nh, nd = hs['nose']
    jw, jh, jd, jo = hs['jaw']
    cw2, ch = hs['chin']
    tg, tl = hs['tusk']
    F = 3.5 - sd
    mark = style.get('mark', '')
    brow_fx = fx_marks(mark) if mark else (fx_scarface if style.get('scars') else None)
    hk = [C('skull', -sw / 2, 58, F, sw, 9, sd, sk, key='o_skull'),
          C('brow', -bw / 2, 63, F - 2, bw, bh, 3, sk, key='o_brow' + mark + ('s' if style.get('scars') else ''), fx=brow_fx),
          C('cheeks', -cw / 2, 59, F - 2.5, cw, 5, 3, sk, key='o_cheeks' + eye.hex() + ('d' if style.get('scars') else ''), fx=fx_eyes(eye, deep=bool(style.get('scars')))),
          C('nose', -nw / 2, 63 - nh, F - 4.5, nw, nh, nd, sk, key='o_nose', fx=fx_nose),
          C('jaw', -jw / 2, 59 - jh, F - 2.5 - jo, jw, jh, jd, sk, key='o_jaw' + hs['mouth'], fx=fx_mouth_kind(hs['mouth'])),
          C('chin', -cw2 / 2, 59 - jh - ch, F - 3 - jo, cw2, ch, 5, sk, key='o_chin'),
          C('tusk_l', -tg / 2 - .7, 58, F - 3.3 - jo, 1.4, tl, 1.4, 'o_tooth', key='o_tusk'),
          C('tusk_r', tg / 2 - .7, 58, F - 3.3 - jo, 1.4, tl, 1.4, 'o_tooth', key='o_tusk')]
    ear = hs['ear']
    if ear:
        for sg in (-1, 1):
            n = 'l' if sg < 0 else 'r'
            hk.append(chain('ear_' + n, (sg * (sw / 2 - .3), 62, -1), ear['len'], ear['w'], 2, ear['rz'], ear['drz'], ear['rx'], 2, sg=sg, step=2, prof=.7, mat=sk))
    return hk


# ---------------------------------------------------------------- corps
def body_parts(sk, eye, style):
    """retourne (legs, pelvis, body_kids, head_kids, arm_kids{sg}, forearm_kids{sg}) ; tout est ensuite complete par les accessoires"""
    legs = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        k = [C('thigh_' + n, X(sg, 6.4, 10), 20, -5.5, 10, 15, 11, sk, key='o_thigh'),
             C('knee_' + n, X(sg, 6.4, 8), 18, -7, 8, 5, 3, sk, key='o_knee'),
             C('shin_' + n, X(sg, 6.4, 8), 5, -4, 8, 14, 8, sk, key='o_shin'),
             C('calf_' + n, X(sg, 6.4, 7), 8, 3.5, 7, 8, 3, sk, key='o_calf'),
             C('foot_' + n, X(sg, 6.4, 8), 0, -8, 8, 4, 12, sk, key='o_foot')]
        for i in range(3):
            k.append(C(f'toe_{n}{i}', X(sg, 6.4, 8) + i * 2.7, 0, -10, 2, 2, 2, 'o_tooth', key='o_toe'))
        legs[sg] = Group('leg_' + n, (sg * 6.4, 34, 0), (0, 0, 0), k)
    pelvis = Group('pelvis', (0, 34, 0), children=[C('pelvis', -9, 31, -5.5, 18, 8, 11, sk, key='o_pelvis')])
    bk = [          C('chest', -12, 42, -7.5, 24, 14, 15, sk, key='o_chest', fx=fx_chest),
          C('hump', -10, 50, 4, 20, 12, 5, sk, key='o_hump'),
          C('traps', -9, 55, -5, 18, 4, 11, sk, key='o_traps'),
          C('neck', -5, 54, -6.5, 10, 6, 10, sk, key='o_neck'),
          C('abd2', -7, 34, -5, 14, 9, 10, sk, key='o_abd2')]
    hk = head_parts(sk, eye, style)
    arms = {}
    fores = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        arms[sg] = [C('shoulder_' + n, X(sg, 14.5, 8), 52, -4.5, 8, 7, 9, sk, key='o_sh'),
                    C('uarm_' + n, X(sg, 15, 8), 40, -4, 8, 13, 8, sk, key='o_ua')]
        fores[sg] = [C('farm_' + n, X(sg, 15, 8), 27, -4, 8, 14, 8, sk, key='o_fa'),
                     C('hand_' + n, X(sg, 15, 6), 21, -3.5, 6, 6, 7, sk, key='o_hand'),
                     C('knuckles_' + n, X(sg, 15, 6), 21, -6, 6, 3, 2, sk, key='o_knu')]
        for i in range(3):
            fores[sg].append(C(f'claw_{n}{i}', X(sg, 15, 6) + i * 2.4, 19, -6, 1, 2, 1, 'o_tooth', key='o_claw'))
    return legs, pelvis, bk, hk, arms, fores


# ---------------------------------------------------------------- accessoires
def loin_skirt(bk, mat='o_cloth', n=7, rag=1.0, belt='o_dleather'):
    bk.append(C('belt', -10, 33, -7.4, 20, 3, 15, belt, key='o_belt'))
    bk.append(C('buckle', -2, 33.5, -7.9, 4, 2, 1, 'o_iron', key='o_buckle'))
    for i in range(n):
        x = -9.5 + i * (19 / n)
        ln = int((12 + (i * 7 % 5) * 2) * rag)
        bk.append(C(f'rag_{i}', x, 33 - ln, -7.6 - (i % 2) * .8, 19 / n + .6, ln, 1, mat, key=f'o_rag{ln}', rot=(0, 0, (-4, 3, -2, 5)[i % 4])))
    for i in range(3):
        bk.append(C(f'rag_back_{i}', -8 + i * 6, 20, 5.6, 5, 14, 1, mat, key='o_ragb'))


def strap_chest(bk, mat='o_leather'):
    bk.append(C('strap_a', -2.5, 40, -8.2, 3, 22, 1.4, mat, key='o_strapa', rot=(0, 0, -32)))
    bk.append(C('strap_b', -2.5, 40, 6.6, 3, 22, 1.4, mat, key='o_strapb', rot=(0, 0, -32)))
    for i in range(3):
        bk.append(C(f'stud_{i}', -3.4 + i * 3.2, 45 + i * 3.4, -8.9, 1, 1, 1, 'o_iron', key='o_stud'))


def layered_plates(bk, mat='o_dleather', plate='o_iron', n=4):
    """cuirasse de plaques superposees (inspiree de la 1re photo) : bandes horizontales qui se chevauchent + gorgerin releve"""
    for i in range(n):
        y = 53 - i * 3.6
        m = plate if i % 2 == 0 else mat
        bk.append(C(f'plate_{i}', -11.5 - (i == 0) * .5, y, -8.6 - i * .15, 23 + (i == 0), 4, 2.4, m, key=f'o_pl{i % 2}', rot=(-7, 0, 0)))
    bk.append(C('collar', -9, 55, -8.4, 18, 5, 3, 'o_dleather', key='o_collar', rot=(-18, 0, 0)))
    bk.append(C('back_plate', -11, 44, 7.6, 22, 11, 2, 'o_dleather', key='o_bplate'))
    bk.append(C('back_plate2', -10, 36, 7.0, 20, 8, 2, plate, key='o_bplate2'))


def pauldron(bk, sg, big=False, spikes=0, mat='o_iron'):
    n = 'l' if sg < 0 else 'r'
    w = 11 if big else 9
    for i in range(3):
        bk.append(C(f'paul_{n}{i}', X(sg, 15.5 + i * .8, w - i), 56 - i * 3.2, -5.5, w - i, 4, 11, mat if i != 1 else 'o_dleather', key=f'o_paul{i}{big}',
                    rot=(0, 0, sg * (-12 - i * 5)), pivot=(sg * 15.5, 58 - i * 3, 0)))
    for i in range(spikes):
        bk.append(spike(f'psp_{n}{i}', sg * (16 + i * 1.4), 58, -3 + i * 3.4, 6 + (i % 2) * 2, 2, (0, 0, sg * (-25 - i * 16)), mat='o_dark'))


def fur_collar(bk):
    for i in range(7):
        a = i * 360 / 7
        bk.append(C(f'fur_{i}', -3 + math.cos(math.radians(a)) * 9, 55 + (i % 2), -2 + math.sin(math.radians(a)) * 7, 5, 4, 5, 'o_fur', key='o_fur', rot=(0, a, 0)))
    bk.append(C('fur_l', -13, 53, -4, 6, 4, 9, 'o_fur', key='o_furs'))
    bk.append(C('fur_r', 7, 53, -4, 6, 4, 9, 'o_fur', key='o_furs'))


def hair(hk, bk, mat, length=14, front=False, zb=4.6):
    hk.append(C('hair_cap', -7, 66, -9, 14, 2, 13, mat, key='o_hcap'))
    hk.append(C('hair_back', -7.5, 56, 3.5, 15, 11, 3, mat, key='o_hback'))
    for i in range(5):
        bk.append(C(f'hair_{i}', -7 + i * 3.4, 56 - length + 4, zb + (i % 2) * .6, 3, length, 1.4, mat, key=f'o_hs{length}'))
    for sg in (-1, 1):
        hk.append(C('hair_side_' + ('l' if sg < 0 else 'r'), X(sg, 7.2, 2), 57, -6, 2, 9, 8, mat, key='o_hside'))
    if front:
        hk.append(C('hair_front', -7, 64, -10, 14, 3, 3, mat, key='o_hfront'))


def bracers(fores, sg, mat='o_leather', plate=True):
    n = 'left' if sg < 0 else 'right'
    fores[sg].append(C('bracer_' + n, X(sg, 15, 9.4), 28, -4.7, 9.4, 7, 9.4, mat, key='o_brc'))
    if plate:
        fores[sg].append(C('bracer_pl_' + n, X(sg, 15, 8), 35, -5.2, 8, 4, 2, 'o_iron', key='o_brp'))
    fores[sg].append(C('wrist_' + n, X(sg, 15, 8.6), 27, -4.3, 8.6, 1.4, 8.6, 'o_wrap', key='o_wr'))


def boots(legs, sg):
    n = 'left' if sg < 0 else 'right'
    legs[sg].children.append(C('wrap_' + n, X(sg, 6.4, 9.4), 5, -4.7, 9.4, 8, 9.4, 'o_wrap', key='o_lwrap'))
    legs[sg].children.append(C('wrapb_' + n, X(sg, 6.4, 9.2), 15, -4.6, 9.2, 4, 9.2, 'o_leather', key='o_lwrap2'))


def helm_spiked(hk):
    """casque a masque de fer hérissé de dents et de lames (photos 6, 9, 10)"""
    hk.append(C('helm_dome', -7.6, 63, -9.5, 15.2, 6, 13.6, 'o_iron', key='o_hdome'))
    hk.append(C('helm_top', -6.4, 68, -8.5, 12.8, 2, 11.5, 'o_ironr', key='o_htop'))
    hk.append(C('helm_back', -7.6, 55, 3.5, 15.2, 12, 2, 'o_iron', key='o_hback2'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        hk.append(C('helm_side_' + n, X(sg, 7.6, 2), 56, -8, 2, 10, 11.5, 'o_ironr', key='o_hside2'))
    hk.append(C('helm_mask', -6.8, 57, -12.2, 13.6, 9, 2, 'o_iron', key='o_hmask', fx=fx_visor))
    hk.append(C('helm_nose', -1.5, 56, -13.6, 3, 10, 1.6, 'o_ironr', key='o_hnose'))
    for i in range(6):
        hk.append(C(f'helm_tooth_{i}', -6 + i * 2.3, 52.5, -12.4, 1.6, 4, 1.4, 'o_iron', key='o_htooth', rot=(0, 0, (i - 2.5) * 5)))
    for i in range(5):
        hk.append(spike(f'helm_crest_{i}', 0, 68, -6 + i * 3.4, 7 - abs(i - 2) * 1.2, 2, (-20 - i * 4, 0, 0), mat='o_dark', d=1.4))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        hk.append(spike('helm_horn_' + n, sg * 7.4, 64, -4, 6, 2, (0, 0, sg * -38), mat='o_dark'))


def coif(hk):
    """capuche de mailles (photo 4)"""
    hk.append(C('coif_top', -7.4, 66, -9.5, 14.8, 3, 13.4, 'o_mail', key='o_ctop'))
    hk.append(C('coif_back', -7.4, 54, 3.2, 14.8, 13, 2.4, 'o_mail', key='o_cback'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        hk.append(C('coif_side_' + n, X(sg, 7.5, 2.4), 55, -8.5, 2.4, 11, 12, 'o_mail', key='o_cside'))
    hk.append(C('coif_neck', -8.5, 51, -6, 17, 5, 12, 'o_mail', key='o_cneck'))
    hk.append(C('coif_brow', -7, 63, -10.9, 14, 2, 1, 'o_mail', key='o_cbrow'))


def patch_shoulder(bk, sg):
    n = 'l' if sg < 0 else 'r'
    bk.append(C('patch_' + n, X(sg, 15.8, 11), 54, -5.8, 11, 6, 11.6, 'o_leather', key='o_patch', rot=(0, 0, sg * -14), pivot=(sg * 15.8, 58, 0)))
    bk.append(C('patch2_' + n, X(sg, 16.4, 9), 49.5, -5.3, 9, 5, 10.6, 'o_dleather', key='o_patch2', rot=(0, 0, sg * -10), pivot=(sg * 16.4, 54, 0)))
    for i in range(3):
        bk.append(C(f'patchst_{n}{i}', sg * (13 + i * 2.4) - .5, 57.4 - i * .6, -6.2, 1, 1, 1, 'o_iron', key='o_stud'))


# ---------------------------------------------------------------- armes (pointent vers le haut dans leur repere ; placees dans la main)
def w_cleaver(mat='o_blade'):
    """coutelas a crochet (photo 7) : lame plate et droite, tranchant d'un cote, pointe en equerre"""
    k = [C('grip', -1, -4, -1, 2, 9, 2, 'o_wrap', key='w_grip'),
         C('pommel', -1.5, -5.2, -1.5, 3, 1.4, 3, 'o_iron', key='w_pom'),
         C('guard', -1.5, 4.6, -2, 3, 1.4, 4, 'o_iron', key='w_guard'),
         C('blade', -.7, 5.5, -2.2, 1.4, 28, 4.4, mat, key='w_blade'),
         C('hook', -.7, 31.5, -2.2, 1.4, 3, 9, mat, key='w_hook'),
         C('hook_tip', -.7, 31.5, -9, 1.4, 3, 3, mat, key='w_hooktip', rot=(0, 0, 0))]
    return k


def w_scimitar():
    """cimeterre courbe, manche enroule de cuir et pommeau carre (photo 8)"""
    k = [C('grip', -1, -4, -1, 2, 8, 2, 'o_wrap', key='w_grip2'),
         C('pommel', -2, -6, -2, 4, 2, 4, 'o_iron', key='w_pom2'),
         C('guard', -2, 4, -2.4, 4, 2, 4.8, 'o_iron', key='w_guard2')]
    g = chain('scim', (0, 6, 0), 26, 1, 6, 0, 0, 0, -6, sg=1, step=2, prof=.45, mat='o_blade')
    return k + [g]


def w_notched():
    """machette crantee (photos 9 et 10) : dos a encoches, bout elargi"""
    k = [C('grip', -1, -4, -1, 2, 9, 2, 'o_wrap', key='w_grip3'),
         C('pommel', -1.8, -5.4, -1.8, 3.6, 1.6, 3.6, 'o_iron', key='w_pom3'),
         C('guard', -1.5, 4.6, -2.2, 3, 1.4, 4.4, 'o_iron', key='w_guard3'),
         C('blade', -.7, 5.5, -2.4, 1.4, 26, 4.8, 'o_blade', key='w_blade3'),
         C('tipw', -.7, 28, -2.9, 1.4, 6, 5.8, 'o_blade', key='w_tip3', rot=(0, 0, 0))]
    for i in range(5):
        k.append(C(f'notch_{i}', -.9, 8 + i * 4.2, 2.2, 1.8, 2, 2, 'o_dark', key='w_notch'))
    return k


def w_spear():
    k = [C('shaft', -.9, -18, -.9, 1.8, 52, 1.8, 'o_wood', key='w_shaft'),
         C('head', -.8, 34, -2.6, 1.6, 11, 5.2, 'o_blade', key='w_shead'),
         C('head_tip', -.8, 45, -1.4, 1.6, 4, 2.8, 'o_blade', key='w_stip'),
         C('collar', -1.4, 32, -1.4, 2.8, 2, 2.8, 'o_wrap', key='w_scol')]
    for sg in (-1, 1):
        k.append(spike('barb_' + ('l' if sg < 0 else 'r'), 0, 33, sg * 2.4, 5, 1.6, (sg * 38, 0, 0), mat='o_blade', d=1.6))
    return k


def w_mace():
    """massue du chef : manche epais, tete a ailerons et pointes"""
    k = [C('shaft', -1.5, -10, -1.5, 3, 40, 3, 'o_wood', key='m_shaft'),
         C('grip', -1.9, -10, -1.9, 3.8, 14, 3.8, 'o_wrap', key='m_grip'),
         C('pommel', -2.3, -12, -2.3, 4.6, 2, 4.6, 'o_iron', key='m_pom'),
         C('core', -3.5, 30, -3.5, 7, 7, 7, 'o_dark', key='m_core')]
    for i in range(4):
        k.append(C(f'flange_{i}', -.9, 28, -5.6, 1.8, 11, 4, 'o_iron', key='m_fl', rot=(0, i * 45, 0), pivot=(0, 33, 0)))
    for (dx, dz) in [(5, 0), (-5, 0), (0, 5), (0, -5)]:
        k.append(spike(f'ms_{dx}_{dz}', dx * .7, 33.5, dz * .7, 5, 2, (dz * 14, 0, -dx * 14), mat='o_dark'))
    k.append(spike('ms_top', 0, 36.6, 0, 7, 2.4, (0, 0, 0), mat='o_dark'))
    k.append(spike('ms_bot', 0, 30.4, 0, -4, 2, (0, 0, 0), mat='o_dark'))
    return k


def hold(group_name, hand_pos, kids, rot=(-62, 0, 0)):
    return Group(group_name, hand_pos, rot, [Group(group_name + '_pos', hand_pos, (0, 0, 0), kids)])


def translate(kids, dx, dy, dz):
    """deplace des cubes (et groupes) : les armes sont modelisees autour de l'origine puis posees dans la main"""
    for c in kids:
        if isinstance(c, Group):
            c.pivot = (c.pivot[0] + dx, c.pivot[1] + dy, c.pivot[2] + dz)
            translate(c.children, dx, dy, dz)
        else:
            c.frm = (c.frm[0] + dx, c.frm[1] + dy, c.frm[2] + dz)
            c.pivot = (c.pivot[0] + dx, c.pivot[1] + dy, c.pivot[2] + dz)
    return kids


def scale_all(groups, f):
    """agrandit un modele : positions, pivots et tailles (les tailles sont arrondies a l'entier, mini 1)"""
    for g in groups:
        g.pivot = tuple(v * f for v in g.pivot)
        for i, ch in enumerate(g.children):
            if isinstance(ch, Group):
                scale_all([ch], f)
            else:
                ctr = [ch.frm[j] + ch.size[j] / 2 for j in range(3)]
                ns = tuple(max(1, int(round(s * f))) for s in ch.size)
                ch.frm = tuple(ctr[j] * f - ns[j] / 2 for j in range(3))
                ch.size = ns
                ch.pivot = tuple(v * f for v in ch.pivot)
                ch.inflate *= f
                ch.key = f'{ch.key}_x{int(f * 100)}'


def unique_names(groups):
    seen = {}

    def rec(gs):
        for g in gs:
            seen[g.name] = seen.get(g.name, 0) + 1
            if seen[g.name] > 1:
                g.name += f'_{seen[g.name]}'
            rec([c for c in g.children if isinstance(c, Group)])
    rec(groups)


def scale_about(kids, f, c):
    for ch in kids:
        if isinstance(ch, Group):
            ch.pivot = tuple(c[j] + (ch.pivot[j] - c[j]) * f for j in range(3))
            scale_about(ch.children, f, c)
        else:
            ctr = [ch.frm[j] + ch.size[j] / 2 for j in range(3)]
            ns = tuple(max(1, int(round(s_ * f))) for s_ in ch.size)
            ch.frm = tuple(c[j] + (ctr[j] - c[j]) * f - ns[j] / 2 for j in range(3))
            ch.size = ns
            ch.pivot = tuple(c[j] + (ch.pivot[j] - c[j]) * f for j in range(3))
            ch.key = f'{ch.key}_h'


def finish(legs, pelvis, bk, hk, arms, fores, weapon, wsg=1, lean=-16, hand_rot=(-48, 0, -10), weapon2=None, head_f=1.22):
    scale_about(hk, head_f, (0, 57, -2))
    head = Group('head', (0, 57, -2), (-lean * .8, 0, 0), hk)
    farms = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        farms[sg] = Group('forearm_' + n, (sg * 15, 41, 0), (-14, 0, 0), fores[sg])
    if weapon:
        hp = (wsg * 15, 24.5, -3.8)
        translate(weapon, *hp)
        farms[wsg].children.append(Group('weapon', hp, hand_rot, weapon))
    if weapon2:
        hp = (-wsg * 15, 24.5, -3.8)
        translate(weapon2, *hp)
        farms[-wsg].children.append(Group('weapon_l', hp, hand_rot, weapon2))
    arm_groups = [Group('arm_' + ('left' if sg < 0 else 'right'), (sg * 14.5, 55, 0), (-4, 0, sg * -3), arms[sg] + [farms[sg]]) for sg in (-1, 1)]
    body = Group('body', (0, 34, 0), (lean, 0, 0), bk + [head] + arm_groups)
    groups = [legs[-1], legs[1], pelvis, body]
    unique_names(groups)
    return groups


# ---------------------------------------------------------------- les 7 modeles
YEL = np.array((236, 206, 52))
ORA = np.array((228, 140, 30))
RED = np.array((214, 50, 38))
for e in (YEL, ORA, RED):
    pass


class Col(tuple):
    def hex(self):
        return '%02x%02x%02x' % tuple(self)


def col(c):
    return Col(int(v) for v in c)


def orc_grunt(skin='5A6B56'):
    """orque 1 : torse nu, bandoulieres de cuir, pagne en loques, longs cheveux noirs, coutelas a crochet"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(YEL), {'head': 'grunt'})
    strap_chest(bk)
    loin_skirt(bk)
    hair(hk, bk, 'o_hairk', 12)
    bracers(fores, -1, 'o_wrap', plate=False)
    bracers(fores, 1, 'o_wrap', plate=False)
    for sg in (-1, 1):
        boots(legs, sg)
    return finish(legs, pel, bk, hk, arms, fores, w_cleaver())


def orc_furred(skin='725E57'):
    """orque 2 : gilet de cuir a col de fourrure, epaulette de cuir, chauve, cimeterre"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(ORA), {'head': 'furred'})
    bk.append(C('vest', -12.4, 37, -8.2, 24.8, 17, 3, 'o_leather', key='o_vest'))
    bk.append(C('vest_back', -12.4, 38, 7.0, 24.8, 16, 2.4, 'o_dleather', key='o_vestb'))
    fur_collar(bk)
    patch_shoulder(bk, 1)
    loin_skirt(bk, mat='o_leather', n=6)
    bracers(fores, 1, 'o_leather')
    for sg in (-1, 1):
        boots(legs, sg)
    hk.append(C('scar_wrap', -7, 60, -9, 14, 2, 12.6, 'o_wrap', key='o_hwrap'))
    return finish(legs, pel, bk, hk, arms, fores, w_scimitar())


def orc_mail(skin='968569'):
    """orque 3 : coiffe de mailles, tunique en loques, epaulette cloutee, machette crantee"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(RED), {'head': 'mail'})
    coif(hk)
    bk.append(C('tunic', -12.6, 36, -8.2, 25.2, 17, 2.4, 'o_cloth', key='o_tunic'))
    bk.append(C('tunic_back', -12.6, 36, 7.2, 25.2, 18, 2.4, 'o_cloth', key='o_tunicb'))
    bk.append(C('mail_chest', -12.6, 44, -8.7, 25.2, 8, 1.2, 'o_mail', key='o_mchest'))
    for sg in (-1, 1):
        bk.append(C('sleeve_' + ('l' if sg < 0 else 'r'), X(sg, 15.4, 9.6), 46, -4.8, 9.6, 8, 9.6, 'o_cloth', key='o_sleeve'))
    patch_shoulder(bk, -1)
    loin_skirt(bk, mat='o_cloth', n=7, rag=1.15)
    bracers(fores, -1, 'o_wrap', plate=True)
    for sg in (-1, 1):
        boots(legs, sg)
    return finish(legs, pel, bk, hk, arms, fores, w_notched(), wsg=1)


def orc_helmed(skin='606060'):
    """orque 4 : casque a masque d'acier, epaulieres a pointes, cuirasse de plaques rouillees, lance"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(YEL), {'head': 'helmed'})
    helm_spiked(hk)
    layered_plates(bk, mat='o_leather', plate='o_ironr')
    pauldron(bk, -1, big=True, spikes=3)
    pauldron(bk, 1, big=True, spikes=3)
    loin_skirt(bk, mat='o_dleather', n=6)
    bracers(fores, -1, 'o_dleather')
    bracers(fores, 1, 'o_dleather')
    for sg in (-1, 1):
        boots(legs, sg)
    return finish(legs, pel, bk, hk, arms, fores, w_spear(), hand_rot=(-6, 0, -6))


def black_hand(skin='2D1C17'):
    """orque noir 1 (photo 1) : peau tres sombre, main blanche sur le visage, longs cheveux noirs, plaques de cuir et de fer superposees,
    bandoulieres, jupe de lanieres, epaulette ragged a droite"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(YEL), {'mark': 'hand', 'head': 'black1'})
    for c in hk:
        if c.name == 'cheeks':
            c.fx = _chain_fx(c.fx, fx_cheekmarks)
    layered_plates(bk, mat='o_leather', plate='o_dark')
    strap_chest(bk, 'o_wrap')
    pauldron(bk, 1, big=False, spikes=0, mat='o_dark')
    bk.append(C('rag_sleeve', X(-1, 15.6, 9.6), 47, -4.8, 9.6, 8, 9.6, 'o_cloth', key='o_sleeve2'))
    loin_skirt(bk, mat='o_dleather', n=8, rag=1.2)
    hair(hk, bk, 'o_hairk', 20, zb=9.6)
    bracers(fores, 1, 'o_dleather')
    bracers(fores, -1, 'o_dleather')
    for sg in (-1, 1):
        boots(legs, sg)
    return finish(legs, pel, bk, hk, arms, fores, w_scimitar())


def black_plate(skin='1D211D'):
    """orque noir 2 : heaume ouvert a cornes courtes, lourdes epaulieres de fer a pointes, cotte de mailles et plastron, bande blanche oblique"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(ORA), {'mark': 'band', 'head': 'black2'})
    hk.append(C('cap', -7.4, 65, -9.4, 14.8, 3, 13, 'o_dark', key='o_cap'))
    hk.append(C('cap_back', -7.4, 56, 3.4, 14.8, 11, 2, 'o_dark', key='o_capb'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        hk.append(C('cap_side_' + n, X(sg, 7.4, 2), 59, -8, 2, 7, 11, 'o_dark', key='o_caps'))
        hk.append(spike('cap_horn_' + n, sg * 6.6, 67, -3, 9, 2.4, (-8, 0, sg * -32), mat='o_dark'))
    hk.append(C('nasal', -1, 59, -11.9, 2, 7, 1, 'o_dark', key='o_nasal'))
    layered_plates(bk, mat='o_mail', plate='o_dark', n=3)
    bk.append(C('hauberk', -12.4, 36, -8.1, 24.8, 8, 2, 'o_mail', key='o_hauberk'))
    pauldron(bk, -1, big=True, spikes=4, mat='o_dark')
    pauldron(bk, 1, big=True, spikes=4, mat='o_dark')
    loin_skirt(bk, mat='o_mail', n=7, rag=1.0, belt='o_dleather')
    hair(hk, bk, 'o_hairk', 12, zb=9.6)
    bracers(fores, -1, 'o_leather')
    bracers(fores, 1, 'o_leather')
    for sg in (-1, 1):
        boots(legs, sg)
    return finish(legs, pel, bk, hk, arms, fores, w_cleaver())


def _chain_fx(f1, f2):
    def f(F, rng, glow):
        f1(F, rng, glow)
        f2(F, rng, glow)
    return f


JAW_OPEN = 0


def fx_lip(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    n[h - 1, :] *= .55


def fx_jaw_closed(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    n[0, :] *= .5
    for x in range(2, w - 2):
        put(n, x, 0, (20, 6, 6), .8)


def orc_chief(skin='A3AFA3'):
    """chef (photo 2) : peau grise pale couturee de cicatrices, plastron a manches courtes, enormes pointes de metal plantees dans l'epaule,
    le dos, le torse, le bras et le crane ; massue"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(RED), {'head': 'chief', 'scars': True})
    # plastron de fer et manches courtes de cuir
    bk.append(C('cuirass', -12.6, 41, -8.9, 25.2, 15, 2.6, 'o_dark', key='o_cuir'))
    bk.append(C('cuirass_ridge', -1.2, 41, -9.5, 2.4, 15, 1, 'o_iron', key='o_cridge'))
    bk.append(C('cuirass_back', -12.6, 41, 7.4, 25.2, 14, 2.4, 'o_dark', key='o_cuirb'))
    bk.append(C('cuirass_low', -11.6, 36, -8.4, 23.2, 6, 2, 'o_dleather', key='o_cuirl'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('cuir_side_' + n, X(sg, 12.4, 2), 40, -7, 2, 15, 14.4, 'o_dark', key='o_cuirs'))
        bk.append(C('sleeve_' + n, X(sg, 15.5, 10), 44, -5.2, 10, 11, 10.4, 'o_dleather', key='o_sleeve3'))
        bk.append(C('sleeve_rim_' + n, X(sg, 15.5, 10.6), 43.2, -5.5, 10.6, 1.6, 10.8, 'o_iron', key='o_slrim'))
    # pointes de metal plantees : epaules (eventail), dos, flancs, avant-bras, cuisses, crane
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        for i, (ang, ln) in enumerate([(30, 12), (52, 15), (74, 12), (96, 9)]):
            bk.append(spike(f'shsp_{n}{i}', sg * 15.5, 56, -2 + i * 1.8, ln, 2.2, (-8 + i * 6, 0, sg * -ang), mat='o_dark'))
        bk.append(spike(f'flank_{n}', sg * 11, 44, -6, 7, 1.8, (-70, 0, sg * -20), mat='o_iron'))
    for i, (x, y, ang) in enumerate([(-6, 52, 20), (-2, 56, 4), (3, 53, -12), (7, 47, -28), (-7, 46, 30)]):
        bk.append(spike(f'back_{i}', x, y, 6.5, 9 + (i % 2) * 3, 2, (62 + i * 4, 0, ang), mat='o_dark' if i % 2 else 'o_iron'))
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        fores[sg].append(spike('farm_sp_' + n, sg * 18.6, 34, -1, 7, 1.8, (0, 0, sg * -72), mat='o_iron'))
        fores[sg].append(spike('farm_sp2_' + n, sg * 17, 31, 3, 6, 1.6, (-60, 0, sg * -30), mat='o_dark'))
    for sg in (-1, 1):
        k = legs[sg].children
        k.append(spike('thigh_sp_' + ('left' if sg < 0 else 'right'), sg * 10.8, 28, -2, 6, 1.8, (0, 0, sg * -76), mat='o_iron'))
    # bouche fermee avec 4 crocs qui depassent (2 en haut, 2 en bas) ; la machoire est un groupe articule : ouverte, on voit les autres dents
    hk[:] = [c for c in hk if getattr(c, 'name', '') not in ('jaw', 'chin', 'tusk_l', 'tusk_r')]
    hk.append(C('upper_lip', -7, 57, -11.2, 14, 2, 3, sk, key='o_ulip', fx=fx_lip))
    hk.append(C('mouth_in', -6, 55, -10, 12, 4, 8, 'o_dark', key='o_min'))
    for i, x in enumerate((-4.6, -2.3, 0, 2.3, 4.6)):                      # petites dents du haut (cachees quand la gueule est fermee)
        hk.append(C(f'tooth_up_{i}', x - .5, 55, -10.4, 1, 2, 1.2, 'o_tooth', key='o_tup'))
    for sg in (-1, 1):
        hk.append(C('fang_up_' + ('l' if sg < 0 else 'r'), sg * 2.8 - .5, 53, -12.4, 1, 5, 1.6, 'o_tooth', key='o_fgu5'))
    jk = [C('jaw', -7, 53, -11, 14, 4, 11, sk, key='o_jawc', fx=fx_jaw_closed),
          C('chin', -4, 50, -11.5, 8, 3, 5, sk, key='o_chinc'),
          C('tongue', -3, 57, -9.5, 6, 1, 7, 'o_tongue', key='o_tng')]
    for i, x in enumerate((-4.6, -2.3, 0, 2.3, 4.6)):                      # petites dents du bas
        jk.append(C(f'tooth_dn_{i}', x - .5, 57, -10.4, 1, 2, 1.2, 'o_tooth', key='o_tdn'))
    for sg in (-1, 1):                                                       # grandes defenses du bas, toujours visibles
        jk.append(C('tusk_' + ('l' if sg < 0 else 'r'), sg * 5.4 - .7, 55, -12.4, 1, 6, 1.6, 'o_tooth', key='o_tsk6'))
    hk.append(Group('jaw', (0, 57, -1), (JAW_OPEN, 0, 0), jk))
    hk.append(spike('skull_sp_a', 3.5, 66, -3, 6, 1.8, (-14, 0, 22), mat='o_dark'))
    hk.append(spike('skull_sp_b', -4.2, 65, 1, 5, 1.6, (12, 0, -28), mat='o_iron'))
    hk.append(spike('chin_sp', 2.4, 53, -9, 4, 1.4, (40, 0, 0), mat='o_dark'))
    # grosses dents inferieures, bandeau de cuir, ceinture lourde
    loin_skirt(bk, mat='o_dleather', n=6, rag=1.1, belt='o_dark')
    bk.append(C('hip_plate', -3, 28, -8.4, 6, 8, 1.4, 'o_iron', key='o_hip'))
    bracers(fores, -1, 'o_dleather', plate=False)
    bracers(fores, 1, 'o_dleather', plate=False)
    for sg in (-1, 1):
        boots(legs, sg)
    for sg in (-1, 1):                                   # trapezes enormes, epaules remontees
        n = 'l' if sg < 0 else 'r'
        bk.append(C('trap_' + n, X(sg, 9.5, 9), 55, -6, 9, 5, 12, sk, key='o_trapc', rot=(0, 0, sg * -18), pivot=(sg * 9.5, 55, 0)))
    groups = finish(legs, pel, bk, hk, arms, fores, w_mace(), hand_rot=(-34, 0, -8), lean=-22, head_f=1.18)
    return groups


MODELS = {
    'orc_1_nu': (orc_grunt, ['5A6B56', '725E57', '968569', '606060'], 0.96),
    'orc_2_fourrure': (orc_furred, ['725E57', '5A6B56', '968569', '606060'], 1.0),
    'orc_3_mailles': (orc_mail, ['968569', '606060', '5A6B56', '725E57'], 1.04),
    'orc_4_casque': (orc_helmed, ['606060', '725E57', '968569', '5A6B56'], 1.08),
    'orc_noir_1_main': (black_hand, ['2D1C17', '1D211D'], 1.12),
    'orc_noir_2_fer': (black_plate, ['1D211D', '2D1C17'], 1.16),
    'orc_chef': (orc_chief, ['A3AFA3'], 1.32),
}


def build(name, skin=None):
    fn, skins, f = MODELS[name]
    T._N[0] = 0
    groups = fn(skin or skins[0])
    if f != 1.0:
        scale_all(groups, f)
    normalize(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    return groups, cubes, W, H, tex


def height_of(groups):
    top = 0
    for c, chain in walk(groups):
        pass
    return None


def generate(outdir, names=None):
    os.makedirs(outdir, exist_ok=True)
    out = {}
    for name in names or MODELS:
        groups, cubes, W, H, tex = build(name)
        export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
        Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, name + '.png'))
        out[name] = (groups, tex)
    return out


def sheet(path, names, views=(0, 35), skins=None, scale=None):
    rows = []
    for name in names:
        fn, sk, f = MODELS[name]
        ims = []
        groups, cubes, W, H, tex = build(name, (skins or {}).get(name))
        sc = scale or (5.6 if f == 1.0 else 5.6 / f * 1.0)
        for y in views:
            ims.append(render(groups, tex, y, pitch=-8, W=330, H=520, scale=sc))
        rows.append(np.concatenate(ims, axis=1))
    Image.fromarray(np.concatenate(rows, axis=0) if len(rows) > 0 else None).save(path)


if __name__ == '__main__':
    import sys
    generate('/tmp/orcs')
    names = list(MODELS)
    sheet('/tmp/orcs_a.png', names[:4], views=(0, 40, 180))
    sheet('/tmp/orcs_b.png', names[4:], views=(0, 40, 180))


def colors_sheet(path):
    """chaque modele dans toutes ses couleurs de peau possibles (vue 3/4)"""
    rows = []
    for name, (fn, skins, f) in MODELS.items():
        ims = []
        for sk in skins:
            groups, cubes, W, H, tex = build(name, sk)
            sc = 3.3 if f == 1.0 else 3.3 / f
            ims.append(render(groups, tex, 30, pitch=-8, W=250, H=330, scale=sc))
        while len(ims) < 4:
            ims.append(np.zeros_like(ims[0]))
        rows.append(np.concatenate(ims, axis=1))
    Image.fromarray(np.concatenate(rows, axis=0)).save(path)
