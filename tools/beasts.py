"""Montures et betes detaillees : mumak, sanglier de guerre, cerf blanc, corbeau, aigle.
Meme chaine que les trolls (trollcore) : cubes + groupes -> .bbmodel -> JSON du mod (bbimport) ; textures peintes pixel par pixel.
Usage : python3 beasts.py [mumak|boar|stag|crow|eagle ...]  -> apercus dans /tmp/beasts/ ; python3 beasts.py gen -> ecrit geo + textures du mod."""
import os, sys, math
import numpy as np
from PIL import Image
import trollcore as tc
from trollcore import *
from trollcore import _N
from trollbrute import make_builders

HERE = os.path.dirname(os.path.abspath(__file__))


# ================================================================== peinture
def streaks(arr, rng, col, n, minlen=3, maxlen=9, a=.85, vertical=True):
    h, w, _ = arr.shape
    for _ in range(n):
        x = int(rng.integers(0, w)); y = int(rng.integers(0, max(1, h - 1)))
        ln = int(rng.integers(minlen, maxlen + 1))
        for i in range(ln):
            put(arr, x, y + i, col, a * (1 - i / (ln + 2)) if i > ln * .6 else a)
            if i < ln * .5 and rng.random() < .5: put(arr, x + 1, y + i, col, a * .55)


def rows_dark(arr, spacing, depth, rng, gap=.2, off=1):
    h, w, _ = arr.shape
    for y in range(off, h, spacing):
        for x in range(w):
            if rng.random() > gap: arr[y, x] *= (1 - depth)


def vgrain(arr, rng, depth=.18, spacing=2):
    h, w, _ = arr.shape
    for x in range(0, w, spacing):
        arr[:, x] *= (1 - depth * (.4 + rng.random() * .6))


def scallops(arr, rng, light, dark, size=3, rows=True):
    """ecailles / plumes en rangees decalees : bord clair, centre sombre"""
    h, w, _ = arr.shape
    k = 0
    for y0 in range(0, h, 2):
        off = (k % 2) * (size // 2 + 1); k += 1
        for x0 in range(-off, w, size + 1):
            for i in range(size):
                x = x0 + i
                if 0 <= x < w and y0 + 1 < h:
                    put(arr, x, y0 + 1, dark, .5)
                    put(arr, x, y0, light, .38 if i in (0, size - 1) else .22)


# ------------------------------------------------------------------ materiaux
tc.MATS.update({
    # mumak
    'hide':    dict(base=(128, 120, 110), n1=4, n2=5, tint=(1, .98, .94)),
    'hide2':   dict(base=(108, 100, 92), n1=4, n2=5, tint=(1, .98, .95)),
    'trunkh':  dict(base=(134, 124, 114), n1=4, n2=5, tint=(1, .98, .94)),
    'earin':   dict(base=(176, 130, 124), n1=6, n2=9, tint=(1, .9, .88)),
    'ivory':   dict(base=(232, 222, 192), n1=5, n2=6, tint=(1, .97, .88)),
    'clothr':  dict(base=(142, 28, 36), n1=6, n2=7, tint=(1, .9, .9)),
    'clothr2': dict(base=(118, 22, 32), n1=6, n2=7, tint=(1, .9, .9)),
    'wood':    dict(base=(98, 70, 44), n1=6, n2=7, tint=(1, .92, .8)),
    'plank':   dict(base=(124, 90, 56), n1=6, n2=7, tint=(1, .92, .8)),
    'rope':    dict(base=(180, 154, 108), n1=8, n2=6, tint=(1, .95, .8)),
    'goldm':   dict(base=(212, 168, 64), n1=6, n2=6, tint=(1, .95, .7)),
    'ironm':   dict(base=(86, 88, 96), n1=5, n2=6, tint=(1, 1, 1.06)),
    'hair':    dict(base=(52, 46, 42), n1=6, n2=6, tint=(1, .95, .9)),
    'nail':    dict(base=(168, 158, 140), n1=5, n2=5, tint=(1, .97, .9)),
    # sanglier
    'bhide':   dict(base=(46, 38, 34), n1=6, n2=7, tint=(1, .94, .9)),
    'bfur':    dict(base=(70, 56, 46), n1=7, n2=8, tint=(1, .92, .85)),
    'bsnout':  dict(base=(122, 92, 84), n1=7, n2=8, tint=(1, .9, .88)),
    'chain':   dict(base=(98, 100, 110), n1=5, n2=5, tint=(1, 1, 1.06)),
    'redleath': dict(base=(116, 32, 28), n1=6, n2=7, tint=(1, .9, .88)),
    'rplate':  dict(base=(100, 102, 112), n1=5, n2=6, tint=(1, 1, 1.07)),
    'dwsteel': dict(base=(66, 68, 78), n1=4, n2=5, tint=(1, 1, 1.07)),
    'dwgold':  dict(base=(206, 160, 62), n1=5, n2=6, tint=(1.05, 1, .8)),
    'dwbronze': dict(base=(158, 108, 52), n1=5, n2=6, tint=(1.05, 1, .85)),
    'dwband':  dict(base=(206, 160, 62), n1=4, n2=5, tint=(1.05, 1, .8)),
    'dwmail':  dict(base=(84, 88, 100), n1=5, n2=6, tint=(1, 1, 1.05)),
    'dwcloth': dict(base=(34, 44, 96), n1=5, n2=6, tint=(.95, 1, 1.05)),
    'orcplate': dict(base=(54, 54, 62), n1=5, n2=7, tint=(1, 1, 1.06)),
    'orcrust': dict(base=(92, 58, 40), n1=6, n2=8, tint=(1, .92, .86)),
    # cerf
    'wcoat':   dict(base=(238, 236, 226), n1=4, n2=5, tint=(1, 1, 1.02)),
    'wcoat2':  dict(base=(246, 246, 240), n1=3, n2=4, tint=(1, 1, 1.02)),
    'antler':  dict(base=(216, 208, 190), n1=5, n2=6, tint=(1, .98, .92)),
    'hoof':    dict(base=(56, 50, 48), n1=4, n2=4, tint=(1, 1, 1)),
    'dnose':   dict(base=(78, 62, 64), n1=4, n2=5, tint=(1, .95, .95)),
    'elfcloth': dict(base=(46, 96, 66), n1=5, n2=6, tint=(.9, 1, .95)),
    'elfleath': dict(base=(104, 74, 46), n1=5, n2=6, tint=(1, .92, .8)),
    # corbeau
    'cblack':  dict(base=(26, 26, 34), n1=3, n2=4, tint=(1, 1, 1.25)),
    'cgrey':   dict(base=(104, 104, 112), n1=4, n2=5, tint=(1, 1, 1.08)),
    'cbeak':   dict(base=(34, 34, 38), n1=3, n2=3, tint=(1, 1, 1.1)),
    'cclaw':   dict(base=(52, 48, 50), n1=3, n2=3, tint=(1, 1, 1)),
    # aigle
    'ebrown':  dict(base=(110, 74, 42), n1=5, n2=6, tint=(1, .92, .82)),
    'edark':   dict(base=(72, 48, 30), n1=5, n2=5, tint=(1, .92, .82)),
    'egold':   dict(base=(166, 120, 62), n1=5, n2=6, tint=(1, .93, .8)),
    'ebeak':   dict(base=(196, 150, 40), n1=4, n2=4, tint=(1, .95, .7)),
    'etalon':  dict(base=(216, 178, 48), n1=5, n2=5, tint=(1, .95, .7)),
    'ebelly':  dict(base=(132, 92, 56), n1=5, n2=6, tint=(1, .93, .82)),
})


def d_hide(depth=.20, spacing=3, dust=1.0):
    def f(arr, face, rng):
        h, w, _ = arr.shape
        # plis : courbes de niveau d'un bruit etire horizontalement
        sx, sy = (6, 2) if face not in ('up', 'down') else (3, 3)
        g = rng.random((h // sy + 3, w // sx + 3))
        g = (g + np.roll(g, 1, 0) + np.roll(g, 1, 1) + np.roll(np.roll(g, 1, 0), 1, 1)) / 4
        g = np.kron(g, np.ones((sy, sx)))[:h, :w]
        for t in (.40, .47, .54, .61):
            m = np.abs(g - t) < (.012 + .004 * depth * 5)
            arr[m] *= (1 - depth * .9)
            m2 = np.zeros_like(m); m2[1:, :] = m[:-1, :]
            arr[m2 & ~m] *= 1.07
        arr *= (1 + (g[:, :, None] - .5) * .14)                          # grandes plaques plus claires / plus sombres
        for _ in range(int(h * w / 36 * dust)):
            blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), (150, 134, 110), .22, rng)
        for _ in range(int(h * w / 60)):
            put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (66, 60, 56), .6)
        if face not in ('up', 'down') and h > 5:
            for y in range(h - 3, h):
                a = .12 + .08 * (y - (h - 3))
                arr[y] = arr[y] * (1 - a) + np.array((150, 134, 110)) * a          # poussiere en bas
    return f


def d_ivory(arr, face, rng):
    h, w, _ = arr.shape
    vgrain(arr, rng, .10, 2)
    arr *= np.linspace(.9, 1.06, h)[:, None, None] if face not in ('up', 'down') else 1
    for _ in range(int(h * w / 25)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (160, 140, 100), .35)
    if h >= 4 and w >= 3: arr[h - 1, :] *= .9


def d_cloth(base_dark, hem=True):
    def f(arr, face, rng):
        h, w, _ = arr.shape
        for x in range(0, w, 3): arr[:, x] *= .9
        for x in range(1, w, 3): arr[:, x] *= 1.04
        for y in range(h):
            if rng.random() < .1: arr[y] *= .96
        if hem and face not in ('up', 'down') and h >= 4:
            arr[h - 1, :] = (200, 160, 60); arr[h - 2, :] = arr[h - 2, :] * .5 + np.array((200, 160, 60)) * .5
            if h >= 7:
                for x in range(1, w - 1, 4): put(arr, x, h - 4, (200, 160, 60), .8)
        if face in ('up', 'down'):
            for y in range(0, h, 2): arr[y, :] *= .95
    return f


def d_wood(arr, face, rng):
    h, w, _ = arr.shape
    if h >= w:
        vgrain(arr, rng, .22, 1)
        for y in range(3, h, 8):
            arr[y, :] *= .62
            if y + 1 < h: arr[y + 1, :] *= 1.10
    else:
        for y in range(0, h, 1):
            if rng.random() < .5: arr[y, :] *= .85


def d_plank(arr, face, rng):
    h, w, _ = arr.shape
    if face in ('up', 'down'):
        for x in range(0, w, 5):
            arr[:, x] *= .7
            for y in range(2, h, 7): put(arr, x + 2, y, (50, 36, 26), .9)
        for y in range(h): arr[y] *= 1 + (rng.random() - .5) * .08
        vgrain(arr, rng, .1, 3)
    else:
        arr[:, :] *= .86
        arr[0, :] *= 1.15


def d_rope(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):
        for x in range(w):
            if (x + y) % 3 == 0: arr[y, x] *= .76
            elif (x + y) % 3 == 1: arr[y, x] *= 1.05


def d_gold(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.12, .78, h)[:, None, None]
    if h >= 2 and w >= 2:
        arr[0, :] += 18; arr[:, 0] += 8


def d_iron(arr, face, rng):
    d_gold(arr, face, rng)
    arr *= .0 + 1.0
    h, w, _ = arr.shape
    for _ in range(int(h * w / 20)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (40, 36, 36), .4)
    if w >= 5 and h >= 5:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, (150, 152, 160))


def d_hair(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(int(h * w / 6)):
        x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
        for i in range(int(rng.integers(2, 5))):
            put(arr, x, y + i, (30, 26, 24) if rng.random() < .5 else (84, 74, 66), .6)


def d_bristle(light, dark, density=5):
    def f(arr, face, rng):
        h, w, _ = arr.shape
        for _ in range(int(h * w / density)):
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
            ln = int(rng.integers(2, 5))
            for i in range(ln):
                put(arr, x, y + i, light if rng.random() < .5 else dark, .55)
        if face not in ('up', 'down') and h > 4:
            arr *= np.linspace(1.08, .86, h)[:, None, None]
    return f


def d_chain(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):
        for x in range(w):
            if (x + y) % 2 == 0: arr[y, x] *= .62
            elif (x // 2 + y) % 2: arr[y, x] *= 1.12
    if h > 2: arr[0, :] *= 1.1; arr[-1, :] *= .7


def d_steel(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.12, .78, h)[:, None, None]
    for _ in range(int(h * w / 22)):
        x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
        for i in range(int(rng.integers(2, 5))): put(arr, x + i, y + i // 2, (172, 174, 182), .45)
    for _ in range(int(h * w / 40)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (120, 70, 36), .3, rng)
    if w >= 5 and h >= 5:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, (190, 190, 198)); put(arr, x + 1, y + 1, (30, 28, 30), .6) if x + 1 < w and y + 1 < h else None
    if h >= 2 and w >= 2:
        arr[0, :] += 14; arr[:, 0] += 8; arr[-1, :] *= .75


GOLD = (206, 160, 62); GOLD_D = (140, 96, 34); GOLD_L = (240, 214, 150)


def _rivets(arr, light=GOLD_L):
    h, w, _ = arr.shape
    if w >= 5 and h >= 5:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, light)


def d_dwsteel(arr, face, rng):
    """acier bleu-gris des nains : reflets, filet d'or fin incruste, rivets dores"""
    h, w, _ = arr.shape
    arr *= np.linspace(1.14, .82, h)[:, None, None]
    for _ in range(int(h * w / 18)):
        x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
        for i in range(int(rng.integers(2, 5))): put(arr, x + i, y + i // 3, (160, 166, 182), .35)
    for _ in range(int(h * w / 30)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (40, 42, 52), .3, rng)
    if h >= 5 and w >= 5:
        arr[0, :] = (170, 176, 192); arr[-1, :] *= .6
        arr[:, 0] *= .8; arr[:, -1] *= .7
        if face not in ('up', 'down') and h >= 7 and w >= 7:       # filet d'or incruste
            for x in range(2, w - 2): put(arr, x, 2, GOLD_D); put(arr, x, h - 3, GOLD_D)
            for y in range(2, h - 2): put(arr, 2, y, GOLD_D); put(arr, w - 3, y, GOLD_D)
    _rivets(arr)


def d_dwbronze(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(int(h * w / 10)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (110, 70, 34) if rng.random() < .5 else (200, 150, 84), .3, rng)
    if h >= 3: arr[0, :] = (214, 170, 100); arr[-1, :] *= .65
    _rivets(arr)


def d_dwgold(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(1, w, 3): arr[:, x] *= .8
    if h >= 3: arr[0, :] = GOLD_L; arr[-1, :] *= .72
    for _ in range(int(h * w / 24)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), GOLD_L, .4)
    _rivets(arr)


def d_dwband(arr, face, rng):
    """bandeau d'or a fenetres carrees sombres (comme le bandeau des casques nains)"""
    h, w, _ = arr.shape
    if face not in ('up', 'down') and h >= 3:
        y0 = max(0, h // 2 - 1)
        for x in range(1, w - 2, 4): arr[y0:y0 + 2, x:x + 2] = (38, 34, 40)
        arr[0, :] = GOLD_L; arr[-1, :] = GOLD_D
    _rivets(arr)


def d_dwmail(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):
        for x in range(w):
            if (x + y) % 2 == 0: arr[y, x] *= .66
            elif (x // 2 + y) % 2: arr[y, x] *= 1.1
    if h > 2: arr[0, :] *= 1.1; arr[-1, :] *= .6


def d_dwcloth(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(0, h, 2): arr[y, :] *= .88
    for x in range(0, w, 3): arr[:, x] *= .94
    if h >= 3: arr[-1, :] = GOLD
    if h >= 6 and w >= 6:
        for x in range(2, w - 2, 5): put(arr, x, h - 3, GOLD, .8)


def d_orc(rust):
    """fer noir martele des orques : bosses, eraflures claires, coulures de rouille, rivets aux angles"""
    def f(arr, face, rng):
        h, w, _ = arr.shape
        arr *= np.linspace(1.1, .8, h)[:, None, None]
        for _ in range(int(h * w / 14)):
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
            blob(arr, x, y, 1, (26, 26, 32) if rng.random() < .5 else (96, 96, 108), .28, rng)
        for _ in range(int(h * w / 26)):
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
            for i in range(int(rng.integers(2, 5))): put(arr, x + i, y + i // 3, (150, 150, 160), .4)
        for _ in range(int(h * w * rust / 9)):
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
            for i in range(int(rng.integers(2, 7))): put(arr, x, y + i, (138, 72, 34) if rng.random() < .6 else (100, 50, 26), .55)
        if w >= 5 and h >= 5:
            for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
                put(arr, x, y, (170, 168, 176)); put(arr, x + 1, y + 1, (14, 12, 14), .7) if x + 1 < w and y + 1 < h else None
        if h >= 2 and w >= 2:
            arr[0, :] += 12; arr[-1, :] *= .7
    return f


def d_leather2(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(int(h * w / 24)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (150, 110, 74), .22, rng)
    if h >= 4 and w >= 3:
        arr[0, :] *= 1.15; arr[-1, :] *= .78
        for x in range(1, w - 1, 3): put(arr, x, h // 2, (60, 40, 28), .5)


def d_fur_w(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(int(h * w / 4)):
        x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
        ln = int(rng.integers(2, 5))
        c = (252, 252, 248) if rng.random() < .5 else (214, 216, 218)
        for i in range(ln): put(arr, x, y + i, c, .45)
    if face not in ('up', 'down') and h > 3:
        arr *= np.linspace(1.04, .88, h)[:, None, None]
        arr[:, :, 2] *= np.linspace(1.0, 1.03, h)[:, None]          # reflet bleute dans l'ombre
    if face == 'down': arr *= .94


def d_antler(arr, face, rng):
    h, w, _ = arr.shape
    if h >= w: vgrain(arr, rng, .13, 1)
    else:
        for y in range(h):
            if rng.random() < .4: arr[y] *= .94
    for _ in range(int(h * w / 18)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (150, 140, 120), .4)
    arr *= np.linspace(.92, 1.08, h)[:, None, None] if h > 3 else 1


def d_hoof(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.15, .8, h)[:, None, None] if h > 2 else 1
    if w >= 3: arr[:, w // 2, :] *= .75


def d_feather(light, dark, size=3, rowstep=2):
    def f(arr, face, rng):
        scallops(arr, rng, light, dark, size)
        h, w, _ = arr.shape
        if face not in ('up', 'down') and h > 3: arr *= np.linspace(1.06, .86, h)[:, None, None]
    return f


def d_flight(tipc, dark, band=None):
    """plumes de vol : une barbe par colonne (2 px), pointe claire, rachis sombre"""
    def f(arr, face, rng):
        h, w, _ = arr.shape
        if face in ('up', 'down') or w >= h:
            for x in range(0, w, 2):
                col = arr[:, x]
                col *= (.78 + rng.random() * .1)
                if x + 1 < w: arr[:, x + 1] *= (1.05 + rng.random() * .06)
            arr[:, :] *= 1
            if h >= 3:
                for x in range(w): put(arr, x, h - 1, tipc, .55 if (x // 2) % 2 == 0 else .35)
        else:
            for y in range(0, h, 2):
                arr[y] *= (.8 + rng.random() * .1)
                if y + 1 < h: arr[y + 1] *= 1.05
            for y in range(h): put(arr, w - 1, y, tipc, .45)
        if band is not None and h >= 6:
            for y in range(h // 3, h // 3 + 1):
                for x in range(w): put(arr, x, y, band, .55)
    return f


def d_black(arr, face, rng):
    h, w, _ = arr.shape
    if face in ('up', 'north', 'east', 'west', 'south'):
        for y in range(1, h, 2):
            for x in range(w):
                if rng.random() > .2: arr[y, x] *= .84
    if face == 'up':
        for _ in range(int(h * w / 6)):
            put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (60, 70, 108), .45)      # reflet bleute
    else:
        for _ in range(int(h * w / 14)):
            put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (52, 56, 84), .35)


def d_grey(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(1, h, 2):
        for x in range(w):
            if rng.random() > .25: arr[y, x] *= .88
    for _ in range(int(h * w / 9)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (136, 136, 144), .35)


def d_beak(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.18, .8, h)[:, None, None] if h > 2 else 1
    if face in ('east', 'west', 'north') and h > 2: put(arr, w // 2, 0, (120, 120, 130), .5)


def d_ebeak(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.12, .82, h)[:, None, None] if h > 2 else 1
    for _ in range(int(h * w / 20)): put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (150, 110, 24), .4)
    if face in ('east', 'west') and w >= 3:
        for y in range(h):
            put(arr, w - 1, y, (46, 40, 32), .85); put(arr, w - 2, y, (46, 40, 32), .35)       # pointe sombre


def d_talon(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(0, h, 2): arr[y] *= .85
    if face not in ('up', 'down') and h > 2:
        arr[h - 1, :] = (30, 28, 26)


def d_egold(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(int(h * w / 3)):
        x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
        for i in range(int(rng.integers(2, 4))): put(arr, x, y + i, (206, 156, 80) if rng.random() < .5 else (110, 76, 40), .55)


tc.DECOR.update({
    'hide': d_hide(), 'hide2': d_hide(.24, 3, 1.3), 'trunkh': d_hide(.28, 2, .6), 'earin': d_hide(.14, 4, .4),
    'ivory': d_ivory, 'clothr': d_cloth(None), 'clothr2': d_cloth(None, hem=False),
    'wood': d_wood, 'plank': d_plank, 'rope': d_rope, 'goldm': d_gold, 'ironm': d_iron, 'hair': d_hair,
    'nail': d_hoof,
    'bhide': d_bristle((96, 80, 68), (22, 18, 16), 4), 'bfur': d_bristle((120, 100, 82), (40, 32, 26), 3),
    'bsnout': d_hide(.14, 3, .8), 'chain': d_chain, 'redleath': d_leather2, 'rplate': d_steel, 'dwsteel': d_dwsteel, 'dwgold': d_dwgold, 'dwbronze': d_dwbronze, 'dwband': d_dwband, 'dwmail': d_dwmail, 'dwcloth': d_dwcloth, 'orcplate': d_orc(.35), 'orcrust': d_orc(1.2),
    'wcoat': d_fur_w, 'wcoat2': d_fur_w, 'antler': d_antler, 'hoof': d_hoof, 'dnose': d_hide(.10, 5, .3),
    'elfcloth': d_cloth(None), 'elfleath': d_leather2,
    'cblack': d_black, 'cgrey': d_grey, 'cbeak': d_beak, 'cclaw': d_talon,
    'ebrown': d_feather((170, 126, 76), (60, 38, 22), 3), 'edark': d_flight((196, 160, 110), (40, 26, 18), (150, 116, 80)),
    'egold': d_egold, 'ebeak': d_ebeak, 'etalon': d_talon, 'ebelly': d_feather((176, 134, 88), (70, 46, 28), 3),
})


# ------------------------------------------------------------------ effets de texture
def fx_war(n=4, faces=('north', 'east', 'west', 'up'), lo=3, hi=10):
    """peinture de guerre rouge : coulures verticales"""
    def f(F, rng, glow):
        for fc in faces:
            a = F[fc]
            streaks(a, rng, (156, 24, 28), n, lo, hi, .9)
            streaks(a, rng, (110, 14, 18), max(1, n // 2), lo, hi, .6)
    return f


def fx_eye(face_x, y, col=(24, 18, 14), glint=(236, 226, 206), size=2):
    """oeil sur les faces laterales : face_x = 'front' -> pixel proche du bord avant (east: droite, west: gauche)"""
    def f(F, rng, glow):
        for fc in ('east', 'west'):
            a = F[fc]
            h, w, _ = a.shape
            x = (w - 2 - face_x) if fc == 'east' else (1 + face_x)
            for dy in range(size):
                for dx in range(size):
                    put(a, x + dx * (1 if fc == 'west' else -1), y + dy, col)
            put(a, x, y, glint, .9)
    return f


def fx_nails(n=4, col=(176, 166, 148)):
    def f(F, rng, glow):
        a = F['north']
        h, w, _ = a.shape
        step = max(2, w // n)
        for i in range(n):
            x = i * step + 1
            for y in range(h - 3, h):
                for dx in range(max(1, step - 2)):
                    put(a, x + dx, y, col if y < h - 1 else (92, 84, 74))
        for fc in ('east', 'west'):
            F[fc][-2:, :] = F[fc][-2:, :] * .5 + np.array(col) * .5
    return f


def fx_none(F, rng, glow):
    pass


def fx_join(*fs):
    def f(F, rng, glow):
        for g in fs: g(F, rng, glow)
    return f


def mkT(k, oy=0.0, ox=0.0, oz=0.0):
    """constructeurs a l'echelle k (autour de l'origine, puis translation) : le meme dessin en plus grand ou plus petit"""
    Bb, Gb = make_builders(1.0)

    def B(name, x, y, z, w, h, d, mat, **kw):
        if kw.get('pivot') is not None:
            px, py, pz = kw['pivot']; kw['pivot'] = (px * k + ox, py * k + oy, pz * k + oz)
        if 'inflate' in kw: kw['inflate'] = kw['inflate'] * k
        return Bb(name, x * k + ox, y * k + oy, z * k + oz, w * k, h * k, d * k, mat, **kw)

    def G(name, pivot, rot=(0, 0, 0), children=None):
        return Gb(name, (pivot[0] * k + ox, pivot[1] * k + oy, pivot[2] * k + oz), rot, children)
    return B, G


# ================================================================== modele : MUMAK
def tusk_chain(B, G, name, base, rot0, segs, key, mat='ivory', rope_at=(), spike=None, ring_mat='goldm'):
    """defile de segments : chaque groupe est tourne par rapport au precedent (courbe).
    segs = [(longueur, epaisseur, (rx, ry, rz) relatif)], base = pivot absolu du premier segment."""
    px, py, pz = base
    nodes = []
    for i, (L, t, rr) in enumerate(segs):
        kids = [B(f'{name}_s{i}', px - t / 2, py - t / 2, pz - L, t, t, L, mat, key=f'{key}{i}')]
        if i in rope_at:
            kids.append(B(f'{name}_wrap{i}', px - t / 2 - .8, py - t / 2 - .8, pz - L * .75, t + 1.6, t + 1.6, 3, 'rope', key=f'{key}w'))
        if i == len(segs) - 1 and spike:
            kids.append(B(f'{name}_spike', px - 1, py - 1, pz - L - 5, 2, 2, 5, 'ironm', key=f'{key}sp'))
            kids.append(B(f'{name}_cap', px - t / 2 - .6, py - t / 2 - .6, pz - L * .55, t + 1.2, t + 1.2, 2, ring_mat, key=f'{key}cap'))
        rot = rot0 if i == 0 else rr
        nodes.append((name if i == 0 else f'{name}{i}', (px, py, pz), rot, kids))
        pz = pz - L
    g = None
    for nm, pv, rot, kids in reversed(nodes):
        g = G(nm, pv, rot=rot, children=kids + ([g] if g else []))
    return g


def mumak():
    B, G = mkT(1.25)
    E = (B, G)
    Hw = mkT(.6, oy=84 - 66 * .6)
    legs = []

    def leg(name, sx, z, fore):
        x = sx * 14.5
        kids = [
            B(f'{name}_thigh', x - 7.5, 24, z - 8, 15, 16, 16, 'hide', key='m_thigh', fx=fx_war(2, ('east', 'west', 'north'), 3, 8) if fore else None),
            B(f'{name}_calf', x - 6, 8, z - 6.5, 12, 17, 13, 'hide2', key='m_calf'),
            B(f'{name}_anklet', x - 6.8, 8.5, z - 7.3, 13.6, 3, 14.6, 'clothr', key='m_anklet'),
            B(f'{name}_foot', x - 7.5, 0, z - 8, 15, 8, 16, 'hide2', key='m_foot', fx=fx_nails(4)),
        ]
        return G(name, (x, 40, z), children=kids)
    legs += [leg('leg_fr', -1, -17, True), leg('leg_fl', 1, -17, True), leg('leg_br', -1, 19, False), leg('leg_bl', 1, 19, False)]

    # ------------ corps
    body_kids = [
        B('torso', -21, 33, -27, 42, 19, 58, 'hide', key='m_torso', fx=fx_war(6, ('east', 'west'), 4, 11)),
        B('torso_up', -19, 52, -26, 38, 11, 55, 'hide', key='m_torso_up'),
        B('shoulder_r', -24, 40, -26, 4, 22, 14, 'hide', key='m_shoulder', fx=fx_war(4, ('east', 'west', 'north'), 4, 10)),
        B('shoulder_l', 20, 40, -26, 4, 22, 14, 'hide', key='m_shoulder'),
        B('haunch_r', -24, 38, 14, 4, 24, 16, 'hide', key='m_haunch'),
        B('haunch_l', 20, 38, 14, 4, 24, 16, 'hide', key='m_haunch'),
        B('belly', -18, 30, -20, 36, 4, 42, 'hide2', key='m_belly'),
        B('chest', -17, 36, -35, 34, 24, 9, 'hide', key='m_chest', fx=fx_war(4, ('north',), 4, 10)),
        B('withers', -15, 62, -26, 30, 4, 22, 'hide', key='m_withers'),
        B('rump', -18, 44, 29, 36, 18, 5, 'hide2', key='m_rump'),
        B('spine_a', -2, 65, 3, 4, 2, 10, 'hide2', key='m_spine'),
    ]
    # sangles de cuir et boucles d'or autour du ventre
    for i, zz in enumerate((-22, -6, 12)):
        body_kids += [
            B(f'strap{i}_top', -22.5, 61, zz, 45, 4, 6, 'leather', key='m_strap_t', inflate=0),
            B(f'strap{i}_r', -22.5, 32, zz, 2, 31, 6, 'leather', key='m_strap_s'),
            B(f'strap{i}_l', 20.5, 32, zz, 2, 31, 6, 'leather', key='m_strap_s'),
            B(f'strap{i}_bot', -22.5, 30, zz, 45, 3, 6, 'leather', key='m_strap_b'),
            B(f'buckle{i}_r', -23.4, 46, zz + 1, 1, 6, 4, 'goldm', key='m_buckle'),
            B(f'buckle{i}_l', 22.4, 46, zz + 1, 1, 6, 4, 'goldm', key='m_buckle'),
        ]
    # caparacon rouge : pans qui tombent des flancs, avec galon dore
    body_kids += [
        B('drape_r', -25, 44, -22, 2, 20, 50, 'clothr', key='m_drape'),
        B('drape_l', 23, 44, -22, 2, 20, 50, 'clothr', key='m_drape'),
        B('drape_b', -19, 44, 31, 38, 18, 2, 'clothr', key='m_drape_b'),
        B('drape_f', -19, 46, -37, 38, 14, 2, 'clothr', key='m_drape_f'),
    ]
    for i in range(-2, 3):                                  # lambrequins : pointes dorees en bas des pans
        for sd, xx in (('r', -26), ('l', 25)):
            body_kids.append(B(f'tassel{i}_{sd}', xx, 40, i * 9 - 1, 1, 5, 3, 'goldm', key='m_tassel'))

    # ------------ howdah (dessine plus petit que l'elephant : echelle .82)
    B, G = Hw
    deck = [
        B('deck', -32, 66, -24, 64, 3, 54, 'plank', key='m_deck'),
        B('deck_edge_f', -33, 66, -25, 66, 4, 2, 'wood', key='m_dedge_fb'),
        B('deck_edge_b', -33, 66, 29, 66, 4, 2, 'wood', key='m_dedge_fb'),
        B('deck_edge_r', -33, 66, -25, 2, 4, 56, 'wood', key='m_dedge_s'),
        B('deck_edge_l', 31, 66, -25, 2, 4, 56, 'wood', key='m_dedge_s'),
        B('valance_f', -32, 58, -26, 64, 8, 2, 'clothr', key='m_val_f'),
        B('valance_b', -32, 58, 29, 64, 8, 2, 'clothr', key='m_val_f'),
        B('valance_r', -34, 58, -24, 2, 8, 54, 'clothr', key='m_val_s'),
        B('valance_l', 32, 58, -24, 2, 8, 54, 'clothr', key='m_val_s'),
    ]
    for sg in (-1, 1):                                       # contre-fiches en bois : du flanc au bord du pont
        x0 = 24 * sg
        deck.append(G(f'brace{sg}', (x0, 62, 0), rot=(0, 0, -40 * sg), children=[B(f'brace_a{sg}', x0 - 2, 44, -18, 4, 24, 4, 'wood', key='m_brace'), B(f'brace_b{sg}', x0 - 2, 44, 16, 4, 24, 4, 'wood', key='m_brace')]))
    posts = []
    PH = 56
    for (px, pz) in ((-30, -22), (28, -22), (-30, 27), (28, 27)):
        posts.append(B(f'post1_{px}_{pz}', px, 69, pz, 4, PH, 4, 'wood', key='m_post1'))
        posts.append(B(f'post1_rope_{px}_{pz}', px - .6, 85, pz - .6, 5.2, 3, 5.2, 'rope', key='m_postw'))
        posts.append(B(f'post1_rope2_{px}_{pz}', px - .6, 110, pz - .6, 5.2, 3, 5.2, 'rope', key='m_postw'))
    for (px, pz) in ((-1, -22), (-1, 27)):
        posts.append(B(f'post1m_{pz}', px - 1, 69, pz, 4, PH, 4, 'wood', key='m_post1'))
    rail = [
        B('rail_f', -30, 80, -22, 62, 2, 2, 'wood', key='m_rail_f'), B('rail_b', -30, 80, 27, 62, 2, 2, 'wood', key='m_rail_f'),
        B('rail_r', -30, 80, -22, 2, 2, 51, 'wood', key='m_rail_s'), B('rail_l', 28, 80, -22, 2, 2, 51, 'wood', key='m_rail_s'),
        B('rail2_r', -30, 74, -22, 2, 2, 51, 'rope', key='m_rail2_s'), B('rail2_l', 28, 74, -22, 2, 2, 51, 'rope', key='m_rail2_s'),
    ]
    R1 = 125                                                  # toit du premier etage : paliers de toile rouge, cornes dorees aux angles
    roof1 = [
        B('r1a', -36, R1, -28, 72, 3, 62, 'clothr2', key='m_r1a'),
        B('r1b', -30, R1 + 3, -23, 60, 3, 52, 'clothr', key='m_r1b'),
        B('r1c', -23, R1 + 6, -17, 46, 3, 40, 'clothr2', key='m_r1c'),
        B('r1d', -15, R1 + 9, -12, 30, 3, 30, 'clothr', key='m_r1d'),
        B('r1ridge', -2, R1 + 12, -14, 4, 3, 34, 'wood', key='m_r1ridge'),
        B('r1val_f', -36, R1 - 6, -29, 72, 6, 2, 'clothr', key='m_r1val_f'),
        B('r1val_b', -36, R1 - 6, 33, 72, 6, 2, 'clothr', key='m_r1val_f'),
        B('r1val_r', -38, R1 - 6, -28, 2, 6, 62, 'clothr', key='m_r1val_s'),
        B('r1val_l', 36, R1 - 6, -28, 2, 6, 62, 'clothr', key='m_r1val_s'),
    ]
    roof1.append(B('finial1', -1, R1 + 15, 1.5, 2, 10, 2, 'goldm', key='m_finial1'))
    roof1.append(B('finial1_b', -3, R1 + 21, 2, 6, 1.5, 1, 'goldm', key='m_finial1b'))
    for (cx, cz, rz) in ((-38, -30, 28), (36, -30, -28), (-38, 33, 28), (36, 33, -28)):
        roof1.append(G(f'horn1_{cx}_{cz}', (cx + 1, R1, cz + 1), rot=(0, 0, rz), children=[
            B(f'horn1a_{cx}_{cz}', cx, R1, cz, 3, 9, 3, 'wood', key='m_horn1'), B(f'horn1b_{cx}_{cz}', cx + .5, R1 + 9, cz + .5, 2, 4, 2, 'goldm', key='m_horn1t')]))
    T2 = R1 + 12
    tier2 = []
    for (px, pz) in ((-14, -14), (11, -14), (-14, 11), (11, 11)):
        tier2.append(B(f'post2_{px}_{pz}', px, T2, pz, 3, 20, 3, 'wood', key='m_post2'))
    tier2 += [
        B('floor2', -17, T2 - 1, -17, 34, 2, 34, 'plank', key='m_floor2'),
        B('r2a', -24, T2 + 20, -24, 48, 3, 48, 'clothr2', key='m_r2a'),
        B('r2b', -19, T2 + 23, -19, 38, 3, 38, 'clothr', key='m_r2b'),
        B('r2c', -13, T2 + 26, -13, 26, 3, 26, 'clothr2', key='m_r2c'),
        B('r2d', -7, T2 + 29, -7, 14, 3, 14, 'clothr', key='m_r2d'),
        B('r2val_f', -24, T2 + 14, -25, 48, 6, 2, 'clothr', key='m_r2val_f'), B('r2val_b', -24, T2 + 14, 23, 48, 6, 2, 'clothr', key='m_r2val_f'),
        B('r2val_r', -26, T2 + 14, -24, 2, 6, 48, 'clothr', key='m_r2val_s'), B('r2val_l', 24, T2 + 14, -24, 2, 6, 48, 'clothr', key='m_r2val_s'),
    ]
    for (cx, cz, rz) in ((-26, -25, 28), (24, -25, -28), (-26, 23, 28), (24, 23, -28)):
        tier2.append(G(f'horn2_{cx}_{cz}', (cx + 1, T2 + 20, cz + 1), rot=(0, 0, rz), children=[
            B(f'horn2a_{cx}_{cz}', cx, T2 + 20, cz, 2, 7, 2, 'wood', key='m_horn2'), B(f'horn2b_{cx}_{cz}', cx + .5, T2 + 26, cz + .5, 1, 3, 1, 'goldm', key='m_horn2t')]))
    T3 = T2 + 29
    tier3 = []
    for (px, pz) in ((-8, -8), (6, -8), (-8, 6), (6, 6)):
        tier3.append(B(f'post3_{px}_{pz}', px, T3, pz, 2, 16, 2, 'wood', key='m_post3'))
    tier3 += [
        B('r3a', -15, T3 + 16, -15, 30, 3, 30, 'clothr2', key='m_r3a'),
        B('r3b', -11, T3 + 19, -11, 22, 3, 22, 'clothr', key='m_r3b'),
        B('r3c', -7, T3 + 22, -7, 14, 3, 14, 'clothr2', key='m_r3c'),
        B('r3d', -3, T3 + 25, -3, 6, 3, 6, 'clothr', key='m_r3d'),
        B('finial', -1, T3 + 28, -1, 2, 11, 2, 'goldm', key='m_finial'),
        B('finial_b', -3, T3 + 33, -.5, 6, 1.5, 1, 'goldm', key='m_finial_b'),
        B('r3val_f', -15, T3 + 11, -16, 30, 5, 2, 'clothr', key='m_r3val_f'), B('r3val_b', -15, T3 + 11, 14, 30, 5, 2, 'clothr', key='m_r3val_f'),
        B('r3val_r', -17, T3 + 11, -15, 2, 5, 30, 'clothr', key='m_r3val_s'), B('r3val_l', 15, T3 + 11, -15, 2, 5, 30, 'clothr', key='m_r3val_s'),
    ]
    spear = G('spear', (-34, 98, -6), rot=(8, 0, 0), children=[
        B('spear_shaft', -35, 96, -52, 3, 3, 78, 'wood', key='m_spear'),
        B('spear_head', -35.5, 95.5, -62, 4, 4, 10, 'ironm', key='m_spearh'),
        B('spear_band', -36, 95, -54, 4.8, 4.8, 3, 'goldm', key='m_spearb'),
    ])
    banners = [
        B('banner_a', -34, 88, -20, 1, 18, 8, 'clothr', key='m_ban'), B('banner_b', 33, 88, -20, 1, 18, 8, 'clothr', key='m_ban'),
        B('banner_c', -34, 88, 22, 1, 20, 8, 'clothr', key='m_ban2'), B('banner_d', 33, 88, 22, 1, 20, 8, 'clothr', key='m_ban2'),
    ]
    howdah = G('howdah', (0, 66, 0), children=deck + posts + rail + roof1 + [spear] + banners)
    B, G = E

    body = G('body', (0, 48, 0), children=body_kids + [howdah])

    # ------------ tete
    head_kids = [
        B('skull', -11, 40, -48, 22, 25, 22, 'hide', key='m_skull', fx=fx_join(fx_eye(2, 15, size=2), fx_war(4, ('north', 'up'), 3, 8))),
        B('dome', -9, 64, -44, 18, 6, 15, 'hide', key='m_dome'),
        B('brow', -11, 55, -49, 22, 3, 3, 'hide2', key='m_brow'),
        B('cheek_r', -12.5, 42, -42, 2, 13, 10, 'hide2', key='m_cheek'),
        B('cheek_l', 10.5, 42, -42, 2, 13, 10, 'hide2', key='m_cheek'),
        B('forehead_cloth', -6, 63, -47, 12, 2, 8, 'clothr', key='m_fcloth'),
        B('head_band', -11.8, 46, -45, 23.6, 3, 4, 'leather', key='m_hband'),
    ]
    earG = []
    for sg, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        x = 11 * sg
        earG.append(G(nm, (x, 64, -32), (0, 28 * sg, 0), children=[
            B(f'{nm}_back', x if sg > 0 else x - 3, 28, -36, 3, 39, 30, 'hide2', key='m_ear', fx=fx_war(6, ('east', 'west'), 5, 14)),
            B(f'{nm}_in', x + 3 if sg > 0 else x - 4, 30, -35, 1, 34, 26, 'earin', key='m_earin'),
            B(f'{nm}_rim', x if sg > 0 else x - 3, 66, -36, 3, 2, 30, 'hide2', key='m_earrim'),
        ]))
    trunk = G('trunk', (0, 62, -48), children=[
        B('trunk_a', -5.5, 42, -57, 11, 21, 10, 'trunkh', key='m_tr_a', fx=fx_war(3, ('north',), 5, 12)),
        B('trunk_ring_a', -6.2, 56, -57.7, 12.4, 2, 11.4, 'clothr', key='m_tr_ring'),
        G('trunk2', (0, 42, -52), children=[
            B('trunk_b', -4.5, 27, -56, 9, 16, 8, 'trunkh', key='m_tr_b'),
            G('trunk3', (0, 27, -52), rot=(10, 0, 0), children=[
                B('trunk_c', -4, 13, -55.5, 8, 15, 7, 'trunkh', key='m_tr_c'),
                G('trunk4', (0, 13, -52), rot=(-24, 0, 0), children=[
                    B('trunk_d', -3.5, 2, -55, 7, 12, 6, 'trunkh', key='m_tr_d'),
                    B('trunk_tip', -2.5, -1, -56, 5, 4, 7, 'trunkh', key='m_tr_tip'),
                ]),
            ]),
        ]),
    ])
    tuskU = [tusk_chain(B, G, f'tusk_u{sg}', (9 * sg + (0 if sg > 0 else 0), 49, -44), (-48, 9 * sg, 0),
                        [(10, 6.5, None), (10, 6, (14, -2 * sg, 0)), (10, 5.5, (14, -3 * sg, 0)), (9, 4.5, (14, -3 * sg, 0)), (8, 3.8, (14, -2 * sg, 0)), (7, 3, (12, 0, 0)), (6, 2.2, (12, 0, 0))],
                        key=f'm_tu{"r" if sg < 0 else "l"}', rope_at=(5,), spike=True) for sg in (-1, 1)]
    tuskL = [tusk_chain(B, G, f'tusk_d{sg}', (6.5 * sg, 43, -45), (-20, 14 * sg, 0),
                        [(8, 4.5, None), (8, 4, (9, -2 * sg, 0)), (7, 3.4, (9, -2 * sg, 0)), (6, 2.6, (9, 0, 0))],
                        key=f'm_td{"r" if sg < 0 else "l"}', rope_at=(2,), spike=True) for sg in (-1, 1)]
    jaw = G('jaw', (0, 44, -42), children=[B('lip', -6, 38, -48, 12, 5, 6, 'hide2', key='m_lip')])
    head = G('head', (0, 62, -30), children=head_kids + earG + [trunk, jaw] + tuskU + tuskL)

    # ------------ queue
    tail = G('tail', (0, 58, 31), children=[
        B('tail_a', -2.5, 42, 30, 5, 17, 5, 'hide2', key='m_tail_a'),
        G('tail2', (0, 42, 32), children=[B('tail_b', -2, 28, 31, 4, 15, 4, 'hide2', key='m_tail_b'), B('tail_tuft', -3, 20, 30.5, 6, 10, 6, 'hair', key='m_tuft')]),
    ])

    groups = legs + [body_with_head(body, head), tail]
    return groups


def body_with_head(body, head):
    """la tete est rattachee au corps (elle suit ses mouvements) ; le pivot de la tete est exprime en absolu"""
    body.children.append(head)
    return body


MODELS = {}


def register(name):
    def deco(fn):
        MODELS[name] = fn
        return fn
    return deco


# ================================================================== modele : SANGLIER DE GUERRE
def sd(s):
    return 'r' if s > 0 else 'l'          # cote droit = x positif dans le dessin (devient -x dans le jeu)


@register('boar')
def boar():
    B, G = mkT(1.0)
    legs = []

    def leg(nm, s, z, fore):
        x = 8.5 * s
        kids = [
            B(f'{nm}_up', x - 5.5, 6.5, z - 5.5, 11, 11, 11, 'bhide', key='b_up_f') if fore else B(f'{nm}_up', x - 5, 6, z - 5.5, 10, 12, 11, 'bhide', key='b_up_h'),
            B(f'{nm}_low', x - 3.5, 2.5, z - 3.5, 7, 6, 7, 'bhide', key='b_low'),
            B(f'{nm}_toe_a', x - 3.5, 0, z - 4.5, 3.4, 3, 7, 'hoof', key='b_toe'),
            B(f'{nm}_toe_b', x + .1, 0, z - 4.5, 3.4, 3, 7, 'hoof', key='b_toe'),
        ]
        if fore:
            kids += [B(f'{nm}_greave', x - 5, 3.5, z - 5, 10, 8, 10, 'dwsteel', key='b_greave'),
                     B(f'{nm}_cuff', x - 5.5, 11, z - 5.5, 11, 1.8, 11, 'dwgold', key='b_gcuff')]
        else:
            kids += [B(f'{nm}_wrap', x - 5, 10, z - 5, 10, 3, 10, 'leather', key='b_wrap'),
                     B(f'{nm}_cuff', x - 5.5, 13, z - 5.5, 11, 1.8, 11, 'dwbronze', key='b_bcuff')]
        return G(nm, (x, 17, z), children=kids)
    legs += [leg('leg_fr', 1, -14, True), leg('leg_fl', -1, -14, True), leg('leg_br', 1, 14, False), leg('leg_bl', -1, 14, False)]

    body_kids = [
        # poitrail et flancs en couches : large au milieu, plus etroit en haut et en bas (silhouette de tonneau)
        B('chest_a', -10.5, 14, -21, 21, 4.5, 22, 'bhide', key='b_chest_a'),
        B('chest_b', -13, 18.5, -21, 26, 6, 22, 'bhide', key='b_chest_b'),
        B('chest_c', -13.5, 24.5, -21, 27, 6, 22, 'bhide', key='b_chest'),
        B('chest_d', -11.5, 30.5, -21, 23, 4, 22, 'bhide', key='b_chest_d'),
        B('chest_front', -9, 16, -25, 18, 16, 4, 'bhide', key='b_chest_f'),
        B('chest_front_up', -7, 30, -24.5, 14, 4, 3.5, 'bfur', key='b_chest_fu'),
        B('hump', -10, 34, -19, 20, 4, 17, 'bfur', key='b_hump'),
        B('shoulder_hump_r', -14.2, 24, -18, 1.6, 8, 11, 'bhide', key='b_shump'),
        B('shoulder_hump_l', 12.6, 24, -18, 1.6, 8, 11, 'bhide', key='b_shump'),
        B('belly', -8, 12.5, -14, 16, 3, 34, 'bfur', key='b_belly'),
        B('rump', -7, 16, 25, 14, 11, 3, 'bhide', key='b_rump'),
        B('rump_low', -5.5, 13.5, 26, 11, 4, 2, 'bhide', key='b_rump_l'),
    ]
    # croupe : le dos descend vers l'arriere et se resserre (3 tranches), comme un vrai sanglier (epaules hautes, arriere-train plus bas)
    for i, (z0, ln, top, wf) in enumerate(((0, 9, 33.5, 1.0), (9, 9, 31, .94), (18, 8, 28.5, .86))):
        for nm, y0, y1, hw in (('a', 14, 18.5, 8.5), ('b', 18.5, 24.5, 11), ('c', 24.5, top, 11.5), ('d', 30.5, 33.5, 9.5)):
            if y1 <= y0 or (nm == 'd' and i > 0): continue
            body_kids.append(B(f'rear{nm}{i}', -hw * wf, y0, z0, 2 * hw * wf, y1 - y0, ln, 'bhide', key=f'b_rear{nm}{i}'))
    for i in range(7):                                     # crinière hérissée le long du dos
        hh = 8 - i * .5 if i < 3 else 6 - (i - 3) * .6
        body_kids.append(B(f'mane{i}', -2, 36 - (i > 1) * (i - 1) * .9, -19 + i * 6, 4, hh, 4, 'bfur', key=f'b_mane{i}'))
    # selle de guerre (couverture bleue a galon d'or)
    body_kids += [
        B('blanket', -12, 33, -12, 24, 2, 22, 'dwcloth', key='b_blanket'),
        B('saddle', -8, 35, -9, 16, 3, 17, 'leather', key='b_saddle'),
        B('pommel', -6, 38, -10, 12, 5, 2, 'leather', key='b_pommel'),
        B('cantle', -7, 38, 7, 14, 7, 2, 'leather', key='b_cantle'),
        B('girth', -13.8, 13, -3, 27.6, 22, 3, 'leather', key='b_girth'),
        B('girth_buckle_r', 13.9, 24, -3.5, 1, 5, 4, 'dwgold', key='b_gbuckle'),
        B('girth_buckle_l', -14.9, 24, -3.5, 1, 5, 4, 'dwgold', key='b_gbuckle'),
    ]
    for s in (1, -1):
        body_kids += [
            B(f'strap_{sd(s)}', 13 * s - (0 if s > 0 else 1), 22, -4, 1, 14, 2, 'leather', key='b_strap'),
            B(f'stirrup_{sd(s)}', 12.5 * s - (0 if s > 0 else 3), 20, -5, 3, 2, 4, 'dwgold', key='b_stirrup'),
        ]
    # barde naine : grands panneaux d'acier bleu a liseres d'or, separes par des montants d'or ; jupe de mailles dessous
    for s in (1, -1):
        def X(edge, t):                                       # x de depart d'une piece d'epaisseur t collee au flanc (cote s)
            return edge if s > 0 else -(edge + t)
        zs = ((-20, 9.4), (-10.2, 9.0), (-0.9, 8.4), (7.9, 8.4), (16.8, 8.6))
        hs = (11.2, 10.4, 9.6, 8.8, 8.0)
        for k, ((z0, ln), hh) in enumerate(zip(zs, hs)):
            top = (33.6, 33.6, 33.6, 31.2, 28.8)[k]
            wf = (1, 1, 1, .94, .86)[k]
            e = (13.7 if z0 + ln / 2 < 0 else 11.7 * wf) + (0.5 if k % 2 else 0)
            y0 = top - hh
            xc = e + 1.1 if s > 0 else -(e + 1.1)
            body_kids.append(G(f'pan{k}_{sd(s)}', (xc, y0, z0), rot=(0, 0, 9 * s), children=[
                B(f'pan{k}_{sd(s)}_b', X(e, 2.2), y0, z0, 2.2, hh, ln, 'dwsteel', key=f'b_pan{k}')]))
        body_kids.append(B(f'skirt_c_{sd(s)}', X(13.4, 2.2), 17, -20, 2.2, 5.5, 20, 'dwmail', key='b_skirt_c'))
        body_kids.append(B(f'skirt_r_{sd(s)}', X(10.4, 2.2), 17, 0.4, 2.2, 5.5, 25.6, 'dwmail', key='b_skirt_r'))
        body_kids.append(B(f'hem_c_{sd(s)}', X(13.3, 2.6), 16.2, -20.2, 2.6, 1, 20.4, 'dwgold', key='b_dhem_c'))
        body_kids.append(B(f'hem_r_{sd(s)}', X(10.3, 2.6), 16.2, 0.2, 2.6, 1, 26, 'dwgold', key='b_dhem_r'))
        # epaulieres : deux ailerons d'or evases vers l'exterieur, comme celles des armures naines
        for j, (rz, yy, ln) in enumerate(((13, 35.2, 12), (24, 33.6, 10))):
            x0 = 12.5 if s > 0 else -(12.5 + 7)
            body_kids.append(G(f'fin{j}_{sd(s)}', (12.5 * s, yy, -14), rot=(0, 0, rz * s), children=[
                B(f'fin{j}_{sd(s)}_b', x0, yy, -14 - ln / 2, 7, 1, ln, 'dwgold', key=f'b_fin{j}')]))
    body_kids += [
        B('rump_plate', -6.5, 13.5, 28, 13, 13, 2, 'dwsteel', key='b_rumpp'),
        B('rump_trim', -7.3, 25.5, 27.6, 14.6, 1.8, 2.8, 'dwband', key='b_rtrim'),
        B('rump_boss', -2, 17.5, 29.8, 4, 4, 1.2, 'dwgold', key='b_rboss'),
    ]
    for i in range(3):                                       # plaques d'acier le long de la selle
        body_kids.append(B(f'backplate{i}', -5.5 + i * .5, (31, 29.3, 28.5)[i], 9.5 + i * 5.4, 11 - i, 2, 4.4, 'dwsteel', key=f'b_bp{i}'))
    body_kids += [
        B('collar', -10.5, 18, -23, 21, 5, 5, 'dwsteel', key='b_collar'),
        B('collar_band', -10.9, 21, -23.4, 21.8, 1.6, 5.8, 'dwband', key='b_cband'),
    ]
    body = G('body', (0, 24, 0), children=body_kids)

    # tete : crane massif, groin, défenses, casque
    head_kids = [
        B('skull_a', -8.5, 14, -30, 17, 18, 10, 'bhide', key='b_skull_a'),
        B('skull', -7, 13, -38, 14, 15, 8.2, 'bhide', key='b_skull_h', fx=fx_eye(1, 5, col=(190, 54, 30), glint=(255, 200, 120))),
        B('snout', -4.8, 12, -47, 9.6, 9, 9.2, 'bsnout', key='b_snout'),
        B('snout_up', -4, 20.5, -45, 8, 2.5, 7, 'bhide', key='b_snout_u'),
        B('nose', -5.6, 11.5, -49, 11.2, 8.5, 2.2, 'bsnout', key='b_nose', fx=fx_nostrils),
        B('jowl_r', -9.8, 14, -35, 2, 10, 12, 'bfur', key='b_jowl'),
        B('jowl_l', 7.8, 14, -35, 2, 10, 12, 'bfur', key='b_jowl'),
        B('brow', -9.5, 25, -40, 19, 3, 4, 'bfur', key='b_brow'),
        # casque nain : calotte d'acier, crete de bronze, bandeau d'or a fenetres, joues et garde-nez de bronze
        B('helm_plate', -8, 31.5, -38, 16, 2.4, 16, 'dwsteel', key='b_helm'),
        B('helm_dome', -6, 33.8, -36, 12, 2, 12, 'dwsteel', key='b_hdome'),
        B('helm_crest', -1, 35.6, -37, 2, 2, 15, 'dwbronze', key='b_crest'),
        B('helm_brow', -8, 26.6, -39.6, 16, 3.2, 2.2, 'dwband', key='b_hbrow'),
        B('helm_side_r', -9.4, 26.6, -39, 1.4, 3.2, 12, 'dwband', key='b_hside'),
        B('helm_side_l', 8, 26.6, -39, 1.4, 3.2, 12, 'dwband', key='b_hside'),
        B('cheek_r', -10.3, 15, -36, 1.8, 11, 11, 'dwsteel', key='b_cheekp'),
        B('cheek_l', 8.5, 15, -36, 1.8, 11, 11, 'dwsteel', key='b_cheekp'),
        B('nose_guard', -3, 22.6, -48.6, 6, 1.8, 14, 'dwsteel', key='b_nguard'),
        B('nose_ring', -1.5, 14, -52, 3, 3, 1, 'dwgold', key='b_ring'),
    ]
    ears = []
    for s in (1, -1):
        x = 7.5 * s
        ears.append(G(f'ear_{sd(s)}', (x, 30, -26), rot=(0, 0, -25 * s), children=[
            B(f'ear_{sd(s)}_o', x - (0 if s > 0 else 2.5), 30, -28, 2.5, 8, 6, 'bhide', key='b_ear'),
            B(f'ear_{sd(s)}_i', x + (0 if s < 0 else 2.2) - (0 if s > 0 else 0.5), 30.5, -27, .6, 6, 4, 'redleath', key='b_earin')]))
    tuskU = [tusk_chain(B, G, f'tusk_u{sd(s)}', (4.8 * s, 15, -44), (-28, 24 * s, 0),
                        [(6, 4, None), (6, 3.6, (28, -3 * s, 0)), (6, 3.1, (26, -4 * s, 0)), (5, 2.5, (24, -3 * s, 0)), (4, 1.8, (22, 0, 0))],
                        key=f'b_tu{sd(s)}', rope_at=(1,), spike=False) for s in (1, -1)]
    tuskL = [tusk_chain(B, G, f'tusk_d{sd(s)}', (3.8 * s, 13.5, -47), (-8, 10 * s, 0),
                        [(5, 2.6, None), (5, 2.2, (14, -2 * s, 0)), (4, 1.6, (14, 0, 0))],
                        key=f'b_td{sd(s)}', spike=False) for s in (1, -1)]
    jaw = G('jaw', (0, 14, -42), children=[B('jaw_b', -4, 11, -50, 8, 3, 10, 'bsnout', key='b_jaw')])
    head = G('head', (0, 24, -20), children=head_kids + ears + [jaw] + tuskU + tuskL)
    body.children.append(head)
    tail = G('tail', (0, 26, 27), children=[B('tail_a', -1, 19, 26.5, 2, 9, 2, 'bhide', key='b_tail'), G('tail2', (0, 19, 27), rot=(25, 0, 0), children=[B('tail_b', -1.5, 13, 26, 3, 7, 3, 'bfur', key='b_tail2')])])
    return legs + [body, tail]


def fx_nostrils(F, rng, glow):
    a = F['north']
    h, w, _ = a.shape
    for x in (2, w - 4):
        for dy in range(2):
            for dx in range(2): put(a, x + dx, 3 + dy, (30, 16, 18))
    arr = F['up']; arr *= .9


# ================================================================== modele : CERF BLANC
def antler(B, G, s, key):
    """un bois majestueux (s = cote) : la perche part vers l'exterieur puis se redresse en lyre, avec andouillers de
    base (vers l'avant), de glace, de milieu et une couronne de pointes au sommet. Dessine droit (vertical) puis courbe par rotations."""
    px, py, pz = 2.6 * s, 52.0, -13
    n = sd(s)
    segs = [(9, 3.0, (12, 0, -72 * s)), (9, 2.6, (-4, 0, 17 * s)), (8.5, 2.3, (-3, 0, 17 * s)), (8, 2.0, (-3, 0, 17 * s)), (6, 1.7, (-3, 0, 12 * s))]
    # andouillers : (segment, hauteur sur le segment, rotation (x avant<0 / arriere>0, y, z), longueur, epaisseur)
    tines = {
        0: [(2.5, (-72, 0, -10 * s), 8.5, 2.1), (6.5, (-50, 0, 22 * s), 9.5, 2.0)],
        1: [(2.0, (-40, 0, 30 * s), 10, 1.9), (5.0, (-24, 0, 34 * s), 9.5, 1.8), (8.0, (-10, 0, 38 * s), 7.5, 1.6)],
        2: [(2.0, (-32, 0, 34 * s), 9.5, 1.7), (5.0, (-16, 0, 36 * s), 8.5, 1.6), (7.5, (-4, 0, 38 * s), 6.5, 1.5)],
        3: [(2.0, (-26, 0, 32 * s), 8.0, 1.5), (5.5, (-10, 0, 34 * s), 6.5, 1.4)],
        4: [(5.0, (-20, 0, 26 * s), 6.0, 1.4), (6.0, (0, 0, 0), 5.5, 1.4), (5.0, (16, 0, -26 * s), 6.0, 1.4)],
    }
    nodes = []
    y = py
    F = .92
    for i, (L, t, rot) in enumerate(segs):
        L = L * F
        kids = [B(f'ant{n}_{i}', px - t / 2, y, pz - t / 2, t, L, t, 'antler', key=f'{key}{i}')]
        for j, (hy, trot, tl, tt) in enumerate(tines.get(i, [])):
            hy, tl = hy * F, tl * F
            kids.append(G(f'tine{n}_{i}_{j}', (px, y + hy, pz), rot=trot, children=[
                B(f'tine{n}_{i}_{j}_c', px - tt / 2, y + hy, pz - tt / 2, tt, tl, tt, 'antler', key=f'{key}t{i}{j}')]))
        nodes.append((f'antler_{n}' if i == 0 else f'antler_{n}{i}', (px, y, pz), rot, kids))
        y += L
    g = None
    for nm, pv, rot, kids in reversed(nodes):
        g = G(nm, pv, rot=rot, children=kids + ([g] if g else []))
    return g


@register('stag')
def stag():
    B, G = mkT(1.0)
    legs = []

    def leg(nm, s, z, hind):
        x = 4 * s
        if hind:
            kids = [B(f'{nm}_thigh', x - 3, 15, z - 3.5, 6, 11, 7, 'wcoat', key='s_thigh'),
                    G(f'{nm}_hock', (x, 15, z), rot=(18, 0, 0), children=[
                        B(f'{nm}_shin', x - 1.6, 3, z - 1.6, 3.2, 12, 3.2, 'wcoat', key='s_shin'),
                        B(f'{nm}_hoof', x - 2, 0, z - 2.6, 4, 3.5, 5, 'hoof', key='s_hoof')])]
        else:
            kids = [B(f'{nm}_arm', x - 2.4, 15, z - 2.8, 4.8, 11, 5.6, 'wcoat', key='s_arm'),
                    B(f'{nm}_shin', x - 1.6, 3, z - 1.6, 3.2, 13, 3.2, 'wcoat', key='s_fshin'),
                    B(f'{nm}_hoof', x - 2, 0, z - 2.6, 4, 3.5, 5, 'hoof', key='s_fhoof')]
        return G(nm, (x, 26, z), children=kids)
    legs += [leg('leg_fr', 1, -9, False), leg('leg_fl', -1, -9, False), leg('leg_br', 1, 10, True), leg('leg_bl', -1, 10, True)]

    body_kids = [
        B('chest', -5, 21, -14, 10, 14, 10, 'wcoat', key='s_chest'),
        B('torso', -4.6, 22, -5, 9.2, 12, 20, 'wcoat', key='s_torso'),
        B('rump', -4.8, 23, 12, 9.6, 11, 5, 'wcoat', key='s_rump'),
        B('belly', -3.6, 20.5, -8, 7.2, 2, 22, 'wcoat2', key='s_belly'),
        B('ruff', -4.2, 26, -17, 8.4, 11, 5, 'wcoat2', key='s_ruff'),
        B('tail', -1.6, 29, 16, 3.2, 6, 3, 'wcoat2', key='s_tail'),
    ]
    neck = G('neck', (0, 31, -12), rot=(-40, 0, 0), children=[
        B('neck_base', -3.9, 31, -15.6, 7.8, 8, 7.6, 'wcoat', key='s_nbase'),
        B('neck_a', -3.2, 38, -15.2, 6.4, 11.5, 6.4, 'wcoat', key='s_neck'),
        B('neck_ruff', -3.8, 33, -16.6, 7.6, 12, 2.2, 'wcoat2', key='s_nruff'),
        B('neck_mane', -2, 33.4, -9.8, 4, 14, 1.8, 'wcoat2', key='s_nmane'),
    ])
    head = G('head', (0, 49, -12), rot=(26, 0, 0), children=[
        B('skull', -3.4, 45.5, -19, 6.8, 7, 9, 'wcoat', key='s_skull', fx=fx_eye(1, 2, col=(30, 20, 16), glint=(250, 250, 255))),
        B('muzzle', -2.4, 43.9, -25, 4.8, 5, 7, 'wcoat', key='s_muzzle'),
        B('nose', -2.5, 43.5, -26.5, 5, 3, 2, 'dnose', key='s_nose'),
        B('jaw_s', -2, 42.5, -24, 4, 1.8, 6, 'wcoat', key='s_jaw'),
        B('circlet', -3.7, 50.9, -16.5, 7.4, 1.4, 6, 'goldm', key='s_circlet'),
        B('cheek_strap', -3.6, 45.1, -22, 7.2, .8, 3, 'elfleath', key='s_cstrap'),
        B('nose_strap', -2.6, 46.1, -23, 5.2, .8, 3, 'elfleath', key='s_nstrap'),
        antler(B, G, 1, 's_antr'), antler(B, G, -1, 's_antl'),
        # oreilles : feuille large ouverte vers l'avant (face interieure rose devant), ecartee de cote et penchee en avant
        G('ear_r', (3.2, 49.5, -12), rot=(-10, 0, -62), children=[B('ear_r_o', 3.0, 49.5, -12.7, 3.8, 9, 1.4, 'wcoat', key='s_ear'), B('ear_r_i', 3.7, 50.3, -13.3, 2.4, 7.2, .6, 'earin', key='s_earin')]),
        G('ear_l', (-3.2, 49.5, -12), rot=(-10, 0, 62), children=[B('ear_l_o', -6.8, 49.5, -12.7, 3.8, 9, 1.4, 'wcoat', key='s_ear'), B('ear_l_i', -6.1, 50.3, -13.3, 2.4, 7.2, .6, 'earin', key='s_earin')]),
    ])
    neck.children.append(head)
    body = G('body', (0, 28, 0), children=body_kids + [neck])
    return legs + [body]


# ================================================================== modele : CORBEAU
@register('crow')
def crow():
    B, G = mkT(1.0)
    body = G('body', (0, 9, 0), children=[
        B('torso', -3.5, 6, -4, 7, 6, 10, 'cblack', key='c_torso'),
        B('breast', -3.1, 5.4, -6, 6.2, 6, 5, 'cblack', key='c_breast'),
        B('belly', -2.6, 4.8, -2, 5.2, 2, 8, 'cblack', key='c_belly'),
        B('back_grey', -3.2, 11.6, -3, 6.4, .8, 5, 'cblack', key='c_backg'),
        B('rump', -3, 7, 6, 6, 4, 2, 'cblack', key='c_rump'),
    ])
    head = G('head', (0, 11.5, -5), children=[
        B('skull', -2.6, 10, -9.5, 5.2, 5, 6, 'cblack', key='c_skull', fx=fx_eye(1, 1, col=(40, 34, 30), glint=(240, 232, 214))),
        B('nape', -2.4, 9.4, -5, 4.8, 4.4, 2.6, 'cblack', key='c_nape'),
        B('throat', -1.8, 8.6, -9, 3.6, 2, 4, 'cblack', key='c_throat'),
        B('beak_up', -1.3, 10.8, -13, 2.6, 2.4, 4.4, 'cbeak', key='c_beaku'),
        B('beak_hook', -1.0, 10.2, -14.2, 2, 1.4, 1.8, 'cbeak', key='c_beakh'),
        B('beak_low', -1.0, 9.6, -12, 2, 1.1, 3, 'cbeak', key='c_beakl'),
    ])
    body.children.append(head)
    wings = []
    for s in (1, -1):
        x0 = 3.3 * s
        n = sd(s)
        w2 = G(f'wing_{n}2', (x0 + 7 * s, 12.6, -1), children=[
            B(f'prim_a_{n}', min(x0 + 7 * s, x0 + 17 * s), 12.1, -5, 10, 1.2, 3, 'cblack', key='c_prim_a'),
            B(f'prim_b_{n}', min(x0 + 7 * s, x0 + 18 * s), 12.1, -2, 11, 1.2, 3, 'cblack', key='c_prim_b'),
            B(f'prim_c_{n}', min(x0 + 7 * s, x0 + 17 * s), 12.1, 1, 10, 1.2, 3, 'cblack', key='c_prim_a'),
            B(f'prim_d_{n}', min(x0 + 7 * s, x0 + 15 * s), 12.1, 4, 8, 1.2, 3, 'cblack', key='c_prim_d'),
        ])
        wings.append(G(f'wing_{n}', (x0, 12.6, -1), children=[
            B(f'wing_in_{n}', min(x0, x0 + 7 * s), 12, -4, 7, 1.6, 10, 'cblack', key='c_wingin'),
            B(f'wing_cov_{n}', min(x0, x0 + 6 * s), 13.4, -3, 6, .8, 5, 'cblack', key='c_wingcov'),
            w2]))
    tail = G('tail', (0, 10, 6), children=[
        B('tail_c', -2.2, 9, 6, 4.4, 1.2, 10, 'cblack', key='c_tailc'),
        G('tail_l', (0, 10, 6), rot=(0, 9, 0), children=[B('tail_l_f', -3.6, 9, 6, 2, 1.2, 8, 'cblack', key='c_tailf')]),
        G('tail_r', (0, 10, 6), rot=(0, -9, 0), children=[B('tail_r_f', 1.6, 9, 6, 2, 1.2, 8, 'cblack', key='c_tailf')]),
    ])
    legs = []
    for s in (1, -1):
        x = 1.7 * s
        legs.append(G(f'leg_{sd(s)}', (x, 6, 0), children=[
            B(f'thigh_{sd(s)}', x - 1.5, 4.4, -1.5, 3, 3, 4, 'cblack', key='c_thigh'),
            B(f'tarsus_{sd(s)}', x - .6, 1, -.4, 1.2, 5, 1.2, 'cclaw', key='c_tarsus'),
            B(f'toe_a_{sd(s)}', x - 1.7, 0, -3, .9, .8, 3.4, 'cclaw', key='c_toe'),
            B(f'toe_b_{sd(s)}', x - .4, 0, -3.4, .9, .8, 3.8, 'cclaw', key='c_toe2'),
            B(f'toe_c_{sd(s)}', x + .8, 0, -3, .9, .8, 3.4, 'cclaw', key='c_toe'),
            B(f'toe_d_{sd(s)}', x - .4, 0, .8, .9, .8, 2.6, 'cclaw', key='c_toe3')]))
    return [body] + wings + [tail] + legs


# ================================================================== modele : AIGLE GEANT
@register('eagle')
def eagle():
    B, G = mkT(1.0)
    body = G('body', (0, 14, 0), children=[
        B('chest', -6.5, 11, -12, 13, 12, 12, 'ebrown', key='e_chest'),
        B('back', -6, 12, -1, 12, 10, 14, 'ebrown', key='e_back'),
        B('belly', -5.5, 9, -9, 11, 3, 20, 'ebelly', key='e_belly'),
        B('shoulder_cape', -7, 22, -10, 14, 2, 12, 'egold', key='e_cape'),
        B('rump', -5, 12, 12, 10, 8, 4, 'edark', key='e_rump'),
        B('breast_feathers', -5, 9, -14, 10, 7, 3, 'ebelly', key='e_bfeath'),
    ])
    head = G('head', (0, 22, -11), children=[
        B('skull', -4.5, 19.5, -18, 9, 9, 8, 'egold', key='e_skull', fx=fx_eye(1, 3, col=(18, 14, 10), glint=(255, 214, 90), size=2)),
        B('brow', -4.8, 26.5, -17.5, 9.6, 1.6, 5, 'edark', key='e_brow'),
        B('crown', -3.5, 28, -16, 7, 1.4, 6, 'egold', key='e_crown'),
        B('beak_up', -1.8, 21, -23.5, 3.6, 5.5, 6.5, 'ebeak', key='e_beaku'),
        B('beak_hook', -1.5, 19, -24.5, 3, 3, 2.4, 'ebeak', key='e_beakh'),
        B('beak_low', -1.4, 20, -22, 2.8, 1.4, 4.4, 'ebeak', key='e_beakl'),
        B('cere', -1.9, 25.4, -20, 3.8, 1, 2.6, 'etalon', key='e_cere'),
        B('nape_a', -5, 17, -11, 10, 7, 3, 'egold', key='e_nape'),
        G('nape_spike_a', (-3, 24, -10), rot=(-30, 0, 0), children=[B('nape_sa', -3.6, 24, -12, 2, 2, 5, 'egold', key='e_nspa')]),
        G('nape_spike_b', (0, 25, -10), rot=(-30, 0, 0), children=[B('nape_sb', -1, 25, -12, 2, 2, 6, 'egold', key='e_nspb')]),
        G('nape_spike_c', (3, 24, -10), rot=(-30, 0, 0), children=[B('nape_sc', 1.6, 24, -12, 2, 2, 5, 'egold', key='e_nspa')]),
    ])
    body.children.append(head)
    wings = []
    for s in (1, -1):
        n = sd(s)
        x0 = 6 * s

        def bx(x_from, length):                              # boite etalee de x_from vers l'exterieur
            return (x_from if s > 0 else x_from - length)
        prim = []
        spec = [(-10, 22), (-7, 25), (-4, 26), (-1, 25), (2, 23), (5, 20), (8, 16)]
        for k, (zz, L) in enumerate(spec):
            prim.append(B(f'prim_{k}_{n}', bx(x0 + 18 * s, L), 17.6, zz, L, 1.6, 3, 'edark', key=f'e_prim{k}'))
        outer = G(f'wing_{n}2', (x0 + 18 * s, 18.4, -2), children=prim + [
            B(f'sec_{n}', bx(x0 + 18 * s, 14), 18.0, -9, 14, 2, 14, 'ebrown', key='e_sec'),
        ])
        wings.append(G(f'wing_{n}', (x0, 18.4, -2), children=[
            B(f'arm_{n}', bx(x0, 18), 17.5, -10, 18, 2.4, 18, 'ebrown', key='e_arm'),
            B(f'cov_{n}', bx(x0 + 2 * s, 14), 19.8, -8, 14, 1, 11, 'egold', key='e_cov'),
            B(f'sec_in_{n}', bx(x0, 16), 17.0, 8, 16, 1.6, 6, 'edark', key='e_secin'),
            outer]))
    tail = G('tail', (0, 15, 14), children=[B('tail_c', -3, 13, 13, 6, 1.8, 18, 'edark', key='e_tailc'),
                                              G('tail_l1', (0, 14, 14), rot=(0, 11, 0), children=[B('tail_l1b', -4.5, 13, 13, 3.6, 1.8, 16, 'edark', key='e_tailf1')]),
                                              G('tail_r1', (0, 14, 14), rot=(0, -11, 0), children=[B('tail_r1b', .9, 13, 13, 3.6, 1.8, 16, 'edark', key='e_tailf1')]),
                                              G('tail_l2', (0, 14, 14), rot=(0, 22, 0), children=[B('tail_l2b', -5.5, 13, 13, 3.4, 1.8, 14, 'edark', key='e_tailf2')]),
                                              G('tail_r2', (0, 14, 14), rot=(0, -22, 0), children=[B('tail_r2b', 2.1, 13, 13, 3.4, 1.8, 14, 'edark', key='e_tailf2')])])
    legs = []
    for s in (1, -1):
        x = 3.8 * s
        legs.append(G(f'leg_{sd(s)}', (x, 11, 2), children=[
            B(f'trousers_{sd(s)}', x - 3, 6, -1, 6, 7, 7, 'ebrown', key='e_trousers'),
            B(f'tarsus_{sd(s)}', x - 1.4, 1.5, .4, 2.8, 5, 2.8, 'etalon', key='e_tarsus'),
            B(f'toe_a_{sd(s)}', x - 3, 0, -3, 1.6, 1.6, 5.4, 'etalon', key='e_toe'),
            B(f'toe_b_{sd(s)}', x - .8, 0, -3.8, 1.6, 1.6, 6.2, 'etalon', key='e_toe2'),
            B(f'toe_c_{sd(s)}', x + 1.4, 0, -3, 1.6, 1.6, 5.4, 'etalon', key='e_toe'),
            B(f'toe_d_{sd(s)}', x - .8, 0, 2.6, 1.6, 1.6, 3.6, 'etalon', key='e_toe3'),
            B(f'claw_{sd(s)}', x - .8, 0, -5.6, 1.6, 1.2, 2, 'cclaw', key='e_claw')]))
    return [body] + wings + [tail] + legs


# ================================================================== generation / apercu
register('mumak')(mumak)


def build_model(name, outdir):
    _N[0] = 0
    groups = MODELS[name]()
    normalize(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    os.makedirs(outdir, exist_ok=True)
    export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
    return groups, tex, W, H, len(cubes)


def preview(name, groups, tex, yaws=(35, 125, 215, 305), scale=4.0, W=560, H=800, pitch=-14, out='/tmp/beasts'):
    os.makedirs(out, exist_ok=True)
    ims = []
    for y in yaws:
        im = render(groups, tex, y, pitch=pitch, W=W, H=H, scale=scale, mode='all')
        ims.append(Image.fromarray(im))
    sheet = Image.new('RGB', (W * len(ims), H))
    for i, im in enumerate(ims): sheet.paste(im, (i * W, 0))
    sheet.save(f'{out}/{name}.png')
    Image.fromarray(tex, 'RGBA').save(f'{out}/{name}_tex.png')


PSCALE = {'boar': 7.0, 'stag': 8.0, 'crow': 22.0, 'eagle': 9.0}

MODNAME = {'mumak': 'mammoth', 'boar': 'war_boar', 'stag': 'stag', 'crow': 'crow', 'eagle': 'eagle'}


def gen():
    """ecrit geo + texture de chaque bete dans les assets du mod (appele par models.build)"""
    import bbimport
    from genlib import wjson, save, A
    out = os.path.join(HERE, 'assets')
    for n, mod in MODNAME.items():
        build_model(n, out)
        src = os.path.join(out, n + '.bbmodel')
        wjson(f'{A}/geo/{mod}.json', bbimport.convert(src))
        save(bbimport.texture(src), f'{A}/textures/entity/{mod}.png')


if __name__ == '__main__' and sys.argv[1:2] == ['gen']:
    gen()
elif __name__ == '__main__':
    names = [a for a in sys.argv[1:] if a in MODELS] or list(MODELS)
    for n in names:
        g, tex, W, H, k = build_model(n, '/tmp/beasts')
        print(n, 'cubes', k, 'texture', W, H)
        preview(n, g, tex, scale=PSCALE.get(n, 4.0))
