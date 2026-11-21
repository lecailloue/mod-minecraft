#!/usr/bin/env python3
"""Genere les fonds des onglets Anneaux et Artefacts (176x180 dans une feuille 256x256), dans le style de l'inventaire :
cadre de bronze et d'or, fond brun sombre, cases dorees. Les PNG produits peuvent etre retouches a la main."""
import os
from PIL import Image, ImageDraw

BRONZE_D, BRONZE, GOLD, GOLD_L = (0x2B, 0x1D, 0x0C), (0x6E, 0x50, 0x26), (0xC9, 0xA0, 0x4A), (0xEB, 0xD2, 0x8A)
BG, BG2, SLOT, SLOT_D = (0x1D, 0x15, 0x0D), (0x26, 0x1B, 0x10), (0x7A, 0x5A, 0x22), (0x3A, 0x2A, 0x14)
W, H = 176, 180
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'ringsofash', 'textures', 'gui')

def base():
    im = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, W - 1, H - 1], fill=BRONZE_D)
    d.rectangle([1, 1, W - 2, H - 2], fill=BRONZE)
    d.rectangle([2, 2, W - 3, H - 3], fill=GOLD)
    d.rectangle([3, 3, W - 4, H - 4], fill=BG)
    d.rectangle([4, 4, W - 5, 82], fill=BG2)               # le haut (zone propre a l'onglet)
    d.rectangle([4, 83, W - 5, 83], fill=BRONZE)
    for cx, cy in ((1, 1), (W - 6, 1), (1, H - 6), (W - 6, H - 6)):   # rivets
        d.rectangle([cx, cy, cx + 4, cy + 4], fill=GOLD)
        d.rectangle([cx + 1, cy + 1, cx + 3, cy + 3], fill=BRONZE_D)
        d.point((cx + 2, cy + 2), fill=GOLD_L)
    return im, d

def slot(d, sx, sy):
    """case de 18x18 dont l'objet occupe les 16x16 centraux : (sx, sy) = position du Slot"""
    x, y = sx - 1, sy - 1
    d.rectangle([x, y, x + 17, y + 17], fill=SLOT_D)
    d.rectangle([x, y, x + 16, y + 16], fill=GOLD_L)
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=SLOT)
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=SLOT)
    d.line([x, y, x + 16, y], fill=GOLD_L)
    d.line([x, y, x, y + 16], fill=GOLD_L)
    d.line([x + 1, y + 17, x + 17, y + 17], fill=SLOT_D)
    d.line([x + 17, y + 1, x + 17, y + 17], fill=SLOT_D)

def player_inv(d):
    for r in range(3):
        for c in range(9): slot(d, 8 + c * 18, 98 + r * 18)
    for c in range(9): slot(d, 8 + c * 18, 156)

def rings():
    im, d = base()
    for i in range(10): slot(d, 44 + (i % 5) * 18, 34 + (i // 5) * 20)
    player_inv(d)
    return im

def artifacts():
    im, d = base()
    slot(d, 80, 26)                                         # la chaine
    for i in range(4): slot(d, 35 + i * 27, 64)
    player_inv(d)
    return im

os.makedirs(OUT, exist_ok=True)
rings().save(os.path.join(OUT, 'rings.png'))
artifacts().save(os.path.join(OUT, 'artifacts.png'))
print('ok', OUT)
