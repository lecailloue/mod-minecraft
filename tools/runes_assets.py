"""Textures de la table runique et variantes de la pierre runique."""
import os
from PIL import Image

ASSETS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'ringsofash')
ITEM = os.path.join(ASSETS, 'textures', 'item')
BLOCK = os.path.join(ASSETS, 'textures', 'block')

# la rune de base : pierre grise, trait gravé violet pâle
STONE = (92, 96, 106, 255)
LIGHT = (126, 130, 140, 255)
DARK = (62, 66, 74, 255)
EDGE = (32, 34, 40, 255)
GLOW = (226, 208, 255, 255)


def recolor(src, dst, mapping):
    """remplace les couleurs de la rune par une autre palette"""
    im = Image.open(src).convert('RGBA')
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    sp, op = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            p = sp[x, y]
            op[x, y] = mapping.get(p, p)
    out.save(dst)


def runes():
    src = os.path.join(ITEM, 'rune.png')
    # rune noire : pierre d'obsidienne, trait rouge sang
    recolor(src, os.path.join(ITEM, 'rune_black.png'), {
        STONE: (34, 28, 40, 255), LIGHT: (56, 46, 64, 255), DARK: (22, 18, 26, 255),
        EDGE: (10, 8, 12, 255), GLOW: (214, 60, 48, 255)})
    # amélioration runique : pierre claire, trait doré
    recolor(src, os.path.join(ITEM, 'runic_upgrade.png'), {
        STONE: (152, 140, 112, 255), LIGHT: (186, 172, 138, 255), DARK: (112, 102, 80, 255),
        EDGE: (48, 42, 32, 255), GLOW: (255, 224, 120, 255)})


def table():
    """dessus : dalle d'ardoise avec un cercle de runes qui luit ; cotes : pierre taillee"""
    n = 32
    rng = __import__('random').Random(7)

    def stone(base, spread):
        im = Image.new('RGBA', (n, n))
        px = im.load()
        for y in range(n):
            for x in range(n):
                k = rng.randint(-spread, spread)
                px[x, y] = (max(0, base[0] + k), max(0, base[1] + k), max(0, base[2] + k), 255)
        return im

    top = stone((58, 54, 66), 8)
    px = top.load()
    cx = cy = (n - 1) / 2.0
    for y in range(n):
        for x in range(n):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 10.0 <= d <= 11.6:                       # le cercle grave
                px[x, y] = (176, 148, 226, 255)
            elif 11.6 < d <= 12.4:
                px[x, y] = (36, 32, 44, 255)
    # quatre traits vers le centre, comme les branches d'une etoile
    for k in range(4, 10):
        for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            px[int(cx + sx * k), int(cy + sy * k)] = (206, 184, 255, 255)
    # bordure de la dalle
    for i in range(n):
        for j in (0, 1, n - 2, n - 1):
            px[i, j] = (40, 36, 48, 255)
            px[j, i] = (40, 36, 48, 255)
    top.save(os.path.join(BLOCK, 'runic_table_top.png'))

    side = stone((50, 46, 58), 7)
    px = side.load()
    for y in range(n):
        for x in range(n):
            if y < 3:                                   # le plateau
                px[x, y] = (72, 66, 84, 255)
            elif y == 3:
                px[x, y] = (30, 26, 36, 255)
            elif y % 10 == 0 or (x % 11 == 0 and y > 4):   # joints de pierre
                px[x, y] = (34, 30, 40, 255)
    for i in range(n):
        px[i, n - 1] = (28, 24, 34, 255)
    side.save(os.path.join(BLOCK, 'runic_table_side.png'))


if __name__ == '__main__':
    runes()
    table()
    print('ok')
