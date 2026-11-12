"""Textures grises de la pierre naine : la teinte (degrade entre deux couleurs) est appliquee en jeu par le code client."""
import random
from tex import *

G = '#d2d2d2'

def gray(v): return (v, v, v)

def d_stone(seed=101):
    return stone(G, seed, var=0.55, cell=4, speck=0.35)

def d_cobble(seed=102):
    rnd = random.Random(seed); size = 16
    pts = [(rnd.random() * size, rnd.random() * size) for _ in range(11)]
    shades = [0.8 + rnd.random() * 0.22 for _ in pts]
    im = new(size)
    for y in range(size):
        for x in range(size):
            best = []
            for i, (px, py) in enumerate(pts):
                for ox in (-size, 0, size):
                    for oy in (-size, 0, size):
                        best.append(((x - px - ox) ** 2 + (y - py - oy) ** 2, i))
            best.sort()
            d1, i1 = best[0]
            d2 = best[1][0] if best[1][1] != i1 else best[2][0]
            edge = (d2 ** .5) - (d1 ** .5)
            f = shades[i1] * (1 + (rnd.random() - .5) * .08)
            if edge < 0.9: f = 0.66
            elif edge < 1.7: f = f * 0.9
            im.putpixel((x, y), shade(rgb(G), f) + (255,))
    return im

def d_bricks(seed=103):
    """grands blocs taillés irréguliers, joints profonds, biseaux marqués : très différent des briques vanilla"""
    rnd = random.Random(seed)
    rows = [(0, 5, [10, 6]), (5, 6, [6, 10]), (11, 5, [12, 4])]
    im = new(16)
    base = rgb('#d6d6d6')
    for (y0, h, widths) in rows:
        x0 = 0
        for w in widths:
            f = 0.9 + rnd.random() * 0.16
            for yy in range(h):
                for xx in range(w):
                    x, y = x0 + xx, y0 + yy
                    if yy == h - 1 or xx == w - 1: g = 0.38                      # joint profond
                    elif yy == 0: g = f * 1.16                                  # biseau haut
                    elif xx == 0: g = f * 1.08
                    elif yy == h - 2 or xx == w - 2: g = f * 0.8                # ombre basse
                    else: g = f * (1 + (rnd.random() - .5) * .10)
                    im.putpixel((x % 16, y), shade(base, g) + (255,))
            # éclats dans les coins
            cx, cy = x0 + (w - 3), y0 + 1
            im.putpixel((cx % 16, cy), shade(base, 0.62) + (255,))
            x0 += w
    return im

def d_pillar(seed=104): return pillar_side('#d6d6d6', seed), framed('#d6d6d6', seed + 1)

def d_chiseled(seed=105):
    """dalle biseautée avec un relief gravé : losanges emboîtés et noyau central"""
    rnd = random.Random(seed)
    im = new(16)
    base = rgb('#d4d4d4')
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            if d == 0: f = 1.15 if (x + y) < 16 else 0.62
            elif d == 1: f = 0.78
            elif d == 2: f = 1.1 if (x + y) < 16 else 0.86
            else:
                m = abs(x - 7.5) + abs(y - 7.5)
                if 3.4 < m < 4.6 or 6.4 < m < 7.6: f = 0.6                       # rainures
                elif 4.6 <= m < 5.4 or 7.6 <= m < 8.4: f = 1.14                  # lèvre éclairée
                elif m < 2.2: f = 0.72 if m > 1.2 else 1.12                      # noyau gravé
                else: f = 0.95
            f *= 1 + (rnd.random() - .5) * .05
            im.putpixel((x, y), shade(base, f) + (255,))
    return im

def d_door():
    """(bas, haut, icone) : panneaux riveles avec une barre centrale"""
    def panel(top):
        im = new(16)
        for y in range(16):
            for x in range(16):
                d = min(x, 15 - x, y if top else 15 - y if False else y)
                f = 1.0
                edge = min(x, 15 - x)
                if edge == 0: f = 0.62
                elif edge == 1: f = 1.08
                elif x in (7, 8): f = 0.72
                else: f = 0.9 + 0.1 * ((y // 4 + x // 8) % 2)
                if y == 0 and top is False: f = 1.1
                if y == 15 and not top: f = 0.62
                if y == 0 and top: f = 0.62
                if (y % 8) in (0, 7) and 1 < x < 14 and x not in (7, 8): f *= 0.82
                im.putpixel((x, y), shade(rgb('#d4d4d4'), f) + (255,))
        for (rx, ry) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            im.putpixel((rx, ry), gray(120) + (255,))
        return im
    low, up = panel(False), panel(True)
    # poignee et rune
    for (x, y) in ((12, 7), (12, 8), (11, 8)): low.putpixel((x, y), gray(70) + (255,))
    for (x, y) in ((4, 8), (5, 8), (5, 9), (4, 9)): up.putpixel((x, y), gray(150) + (255,))
    icon = new(16)
    for y in range(1, 15):
        for x in range(4, 12):
            src = up if y < 8 else low
            icon.putpixel((x, y), src.getpixel((x + 2, (y * 2) % 16)))
    for x in range(3, 13):
        icon.putpixel((x, 0), gray(130) + (255,)); icon.putpixel((x, 15), gray(130) + (255,))
    for y in range(0, 16):
        icon.putpixel((3, y), gray(130) + (255,)); icon.putpixel((12, y), gray(130) + (255,))
    return low, up, icon
