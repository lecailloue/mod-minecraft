"""Texture de la table runique.

Les pierres de runes ne sont plus generees ici : elles sont dessinees a la main et
rangees dans textures/item sous le nom de leur objet (rune_damage_1.png, rune_elven.png,
rune_black.png, runic_upgrade.png...). La fonction runes() ci-dessous ne sert plus qu'a
refaire des ebauches si on en a besoin, et elle refuse d'ecraser ce qui existe deja.
"""
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
    """remplace les couleurs de la rune par une autre palette, sans jamais ecraser un dessin"""
    if os.path.exists(dst):
        return
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
    # les runes a effet : chacune sa pierre et son trait
    for name, stone, light, dark, glow in (
            ('rune_fire', (134, 92, 74), (168, 118, 94), (94, 62, 48), (255, 170, 70)),
            ('rune_wind', (126, 134, 136), (160, 170, 172), (88, 94, 96), (200, 248, 255)),
            ('rune_poison', (96, 118, 82), (124, 150, 106), (66, 82, 56), (150, 255, 110)),
            ('rune_ice', (118, 142, 160), (152, 178, 196), (82, 100, 116), (150, 230, 255)),
            ('rune_elven', (138, 146, 128), (172, 180, 158), (96, 102, 88), (240, 226, 150)),
            ('rune_hobbit', (124, 102, 78), (156, 130, 100), (88, 72, 54), (150, 220, 120)),
            ('rune_forest', (86, 106, 76), (112, 136, 98), (60, 74, 52), (170, 240, 130)),
            ('rune_magic', (72, 76, 120), (98, 102, 154), (48, 50, 84), (210, 160, 255)),
            # ---- runes d'armure : ebauches, en attendant les dessins
            ('rune_protection_1', (96, 104, 124), (124, 134, 156), (66, 72, 88), (150, 180, 255)),
            ('rune_protection_2', (96, 104, 124), (124, 134, 156), (66, 72, 88), (120, 160, 255)),
            ('rune_protection_3', (96, 104, 124), (124, 134, 156), (66, 72, 88), (90, 140, 255)),
            ('rune_thorns_1', (112, 84, 84), (142, 108, 108), (78, 58, 58), (255, 120, 110)),
            ('rune_thorns_2', (112, 84, 84), (142, 108, 108), (78, 58, 58), (240, 80, 70)),
            ('rune_thorns_3', (112, 84, 84), (142, 108, 108), (78, 58, 58), (210, 40, 36)),
            ('rune_jump_1', (92, 112, 84), (120, 142, 108), (62, 78, 58), (170, 255, 140)),
            ('rune_jump_2', (92, 112, 84), (120, 142, 108), (62, 78, 58), (130, 250, 100)),
            ('rune_jump_3', (92, 112, 84), (120, 142, 108), (62, 78, 58), (90, 240, 60)),
            ('rune_swim_1', (80, 108, 120), (106, 138, 152), (54, 74, 84), (150, 230, 255)),
            ('rune_swim_2', (80, 108, 120), (106, 138, 152), (54, 74, 84), (110, 210, 255)),
            ('rune_swim_3', (80, 108, 120), (106, 138, 152), (54, 74, 84), (70, 190, 255)),
            ('rune_light', (140, 128, 90), (172, 158, 114), (98, 90, 62), (255, 240, 150)),
            ('rune_cave', (92, 92, 96), (118, 118, 124), (64, 64, 68), (190, 190, 200)),
            ('rune_breath', (84, 112, 116), (110, 142, 146), (58, 78, 80), (160, 255, 240))):
        recolor(src, os.path.join(ITEM, name + '.png'), {
            STONE: stone + (255,), LIGHT: light + (255,), DARK: dark + (255,),
            EDGE: (24, 24, 30, 255), GLOW: glow + (255,)})


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
    table()
    print('ok')
