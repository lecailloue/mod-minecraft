#!/usr/bin/env python3
"""Icones d'inventaire des epees : rendu 3D du modele de l'arme (geo + texture + lueur), lame inclinee a 45 degres comme un sprite.
Usage : python3 tools/gen_sword_icons.py [id ...]"""
import json, os, sys
import numpy as np
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import gen_armor_icons as GI

RES = GI.RES
SWORDS = ['lunargent_sword', 'elven_blade', 'frost_blade', 'blood_blade', 'viper_dagger', 'storm_blade', 'sentinel_blade', 'sun_blade',
          'halfling_dagger', 'ranger_sword', 'wight_blade', 'kings_blade', 'orc_cleaver', 'orc_scimitar', 'orc_machete']


def tex_of(name, glow):
    t = np.asarray(Image.open(f'{RES}/textures/entity/arme_{name}.png').convert('RGBA')).astype(float)
    gp = f'{RES}/textures/entity/arme_{name}_glow.png'
    if glow and os.path.exists(gp):
        g = np.asarray(Image.open(gp).convert('RGBA')).astype(float)
        a = g[..., 3:4] / 255.0
        t[..., :3] = np.clip(t[..., :3] * (1 - 0.35 * a) + g[..., :3] * a * 1.15, 0, 255)
        t[..., 3] = np.maximum(t[..., 3], g[..., 3])
    return t.astype(np.uint8)


def icon(name, glow, yaw=75, pitch=0):
    geo = json.load(open(f'{RES}/geo/arme_{name}.json'))
    img, al = GI.render_rgba([(geo, tex_of(name, glow))], yaw=yaw, pitch=pitch, S=1800)
    rgba = np.dstack([img.astype(np.uint8), (al * 255).astype(np.uint8)])
    im = Image.fromarray(rgba, 'RGBA').rotate(-45, resample=Image.BICUBIC, expand=True)
    a = np.asarray(im)
    return GI.finish(a[..., :3].astype(float), a[..., 3] > 100, pad=1)


if __name__ == '__main__':
    want = sys.argv[1:] or SWORDS
    tiles = []
    for n in want:
        has_glow = os.path.exists(f'{RES}/textures/entity/arme_{n}_glow.png')
        always = n not in ('elven_blade', 'sentinel_blade', 'sun_blade')
        ic = icon(n, has_glow and always)
        ic.save(f'{GI.ITEM}/{n}.png'); tiles.append(ic)
        if has_glow and not always:
            g = icon(n, True); g.save(f'{GI.ITEM}/{n}_glow.png'); tiles.append(g)
        print(n)
    pv = Image.new('RGBA', (len(tiles) * 68 + 4, 72), (120, 120, 126, 255))
    for i, t in enumerate(tiles): pv.paste(t, (4 + i * 68, 4), t)
    pv.resize((pv.width * 2, pv.height * 2), Image.NEAREST).save('/tmp/sword_icons_preview.png')
