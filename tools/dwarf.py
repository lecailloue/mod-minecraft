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

def d_bricks(seed=103): return bricks('#d6d6d6', '#8c8c8c', seed, var=0.09)

def d_pillar(seed=104): return pillar_side('#d6d6d6', seed), framed('#d6d6d6', seed + 1)

def d_chiseled(seed=105):
    im = framed('#cfcfcf', seed)
    pat = ['..oooooooo..', '.o........o.', 'o..oooooo..o', 'o.o......o.o', 'o.o.oooo.o.o', 'o.o.o..o.o.o',
           'o.o.o..o.o.o', 'o.o.oooo.o.o', 'o.o......o.o', 'o..oooooo..o', '.o........o.', '..oooooooo..']
    stamp(im, pat, {'o': gray(120)}, 2, 2)
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
