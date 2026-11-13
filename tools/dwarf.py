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
    """grands blocs taillés irréguliers, SANS cadre : les joints ne tombent jamais sur le bord du bloc (les briques se prolongent d'un bloc a l'autre)"""
    rnd = random.Random(seed)
    courses = [(0, 5, [2, 10]), (5, 6, [6, 12]), (11, 5, [1, 8])]      # (y, hauteur, departs de briques ; elles se referment en boucle)
    base = rgb('#d6d6d6')
    im = new(16)
    for (y0, h, starts) in courses:
        bricks = []
        for i, st in enumerate(starts):
            nxt = starts[(i + 1) % len(starts)]
            bricks.append((st, (nxt - st) % 16 or 16, 0.9 + rnd.random() * 0.16))
        for yy in range(h):
            for x in range(16):
                for (st, w, f) in bricks:
                    xx = (x - st) % 16
                    if xx < w: break
                if yy == h - 1 or xx == w - 1: g = 0.4                       # joint
                elif yy == 0: g = f * 1.14                                   # biseau haut
                elif xx == 0: g = f * 1.07
                elif yy == h - 2 or xx == w - 2: g = f * 0.82               # ombre basse
                else: g = f * (1 + (rnd.random() - .5) * .10)
                im.putpixel((x, y0 + yy), shade(base, g) + (255,))
    return im

def d_pillar(seed=104):
    """fut cannele : un bloc empile sur un autre se prolonge sans coupure ; seul le dessus est encadre"""
    rnd = random.Random(seed); base = rgb('#d6d6d6')
    side = new(16)
    for x in range(16):
        f = 0.62 if x in (0, 15) else [0.78, 1.08, 1.0, 0.9][x % 4]
        for y in range(16):
            ff = f * (1 + (rnd.random() - 0.5) * 0.05)
            if y in (7, 8): ff = (1.18 if y == 7 else 0.78) if x not in (0, 15) else ff    # bague mediane (se repete a chaque bloc)
            side.putpixel((x, y), shade(base, ff) + (255,))
    return side, framed('#d6d6d6', seed + 1)

def d_chiseled(seed=105):
    """motif de losanges emboîtés SANS cadre : se raccorde parfaitement d'un bloc a l'autre"""
    rnd = random.Random(seed)
    im = new(16)
    base = rgb('#d4d4d4')
    for y in range(16):
        for x in range(16):
            m = abs(x - 7.5) + abs(y - 7.5)
            r = m % 7.5 if False else m
            if m < 1.6: f = 1.14
            elif m < 2.6: f = 0.72
            elif 3.4 < m < 4.6 or 9.4 < m < 10.6: f = 0.6                    # rainures
            elif 4.6 <= m < 5.4 or 10.6 <= m < 11.4: f = 1.14                # levre eclairee
            elif 7.0 <= m < 8.0: f = 0.8
            else: f = 0.96
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
