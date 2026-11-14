"""Modeles 3D des creatures + peaux des humanoides."""
import os
from geo import Model
from skins import skin
from tex import rgb, shade, mix
from genlib import A, ROOT

PREVIEW = os.path.join(ROOT, 'build', 'apercu')
EYE = {'e': '#ffb020', 'r': '#ff3010', 'k': '#101010', 'w': '#f0ead8', 'd': '#201818', 'g': '#9fe060', 'c': '#ff7a18', 'y': '#ffe070', 'm': '#3a5a2a'}

def troll():
    """Trolls : modeles generes par trollbrute.py (tools/assets/troll*.bbmodel), importes comme les modeles Blockbench.
    Classique (gris, presque nu, vouté) et chef de meute (plus grand, casque, yeux rouges lumineux)."""
    import bbimport, os, trollbrute
    from PIL import Image
    from genlib import wjson, save
    assets = os.path.join(os.path.dirname(__file__), 'assets')
    trollbrute.generate(assets)
    ren = {'leg_left': 'leg_l', 'leg_right': 'leg_r', 'arm_left': 'arm_l', 'arm_right': 'arm_r'}
    for name in ('troll', 'troll_chief'):
        src = os.path.join(assets, name + '.bbmodel')
        wjson(f'{A}/geo/{name}.json', bbimport.convert(src, rename=ren))
        save(bbimport.texture(src), f'{A}/textures/entity/{name}.png')
    save(Image.open(os.path.join(assets, 'troll_chief_glow.png')), f'{A}/textures/entity/troll_chief_glow.png')
    # variantes du troll de pierre : pierre claire, puis statue (soleil)
    base = bbimport.texture(os.path.join(assets, 'troll.bbmodel'))
    stone, statue = base.copy(), base.copy()
    sp, tp, bp = stone.load(), statue.load(), base.load()
    for y in range(base.height):
        for x in range(base.width):
            r, g, b, a = bp[x, y]
            if a == 0: continue
            sp[x, y] = tuple(mix((r, g, b), (150, 150, 146), 0.75)) + (a,)
            lum = (r + g + b) / 3
            v = max(0, min(255, int(128 + (lum - 100) * 0.22)))
            tp[x, y] = (v, v, max(0, v - 4), a)
    save(stone, f'{A}/textures/entity/troll_stone.png')
    save(statue, f'{A}/textures/entity/troll_statue.png')

def warg():
    m = Model('warg', 64, 64, 7)
    c = '#423c38'; dark = '#2c2826'
    b = m.part('body', (0, 14, 0))
    b.box((-5, 9, -6), (10, 9, 20), c)
    b.box((-6, 10, -11), (12, 12, 9), dark, noise=0.16)
    h = m.part('head', (0, 18, -11))
    h.box((-4, 14, -18), (8, 8, 7), c, art={'front': ['........', '........', '.r....r.', '........', '........']}, pal=EYE)
    h.box((-2, 14, -23), (4, 4, 5), dark, art={'front': ['.kk.', '....', 'w..w', '....']}, pal=EYE)
    h.box((-4, 22, -14), (2, 3, 1), dark); h.box((2, 22, -14), (2, 3, 1), dark)
    m.part('leg_fr', (-4, 10, -7)).box((-6, 0, -9), (4, 10, 4), c)
    m.part('leg_fl', (4, 10, -7)).box((2, 0, -9), (4, 10, 4), c, mirror=True)
    m.part('leg_br', (-4, 10, 11)).box((-6, 0, 9), (4, 10, 4), c)
    m.part('leg_bl', (4, 10, 11)).box((2, 0, 9), (4, 10, 4), c, mirror=True)
    m.part('tail', (0, 15, 14)).box((-1, 13, 14), (3, 3, 10), dark)
    m.export(); m.preview(f'{PREVIEW}/warg.png')

def stag():
    m = Model('stag', 64, 64, 9)
    c = '#f2efe6'; horn = '#c9b48a'
    m.part('body', (0, 16, 0)).box((-4, 12, -9), (8, 9, 18), c)
    h = m.part('head', (0, 20, -8))
    h.box((-2, 17, -11), (4, 9, 4), c)
    h.box((-3, 24, -16), (6, 5, 8), c, art={'right': ['........', '.k......'], 'left': ['........', '......k.'], 'front': ['......', '......', '......', '..kk..']}, pal=EYE)
    for sx in (-1, 1):
        x = -4 if sx < 0 else 3
        h.box((x, 29, -11), (1, 7, 1), horn); h.box((x + sx * 2 if sx > 0 else x - 2, 33, -11), (3, 1, 1), horn)
        h.box((x + sx * 3, 33, -11), (1, 4, 1), horn); h.box((x, 31, -13), (1, 1, 3), horn)
    m.part('leg_fr', (-3, 12, -7)).box((-4, 0, -8), (2, 12, 2), c)
    m.part('leg_fl', (3, 12, -7)).box((2, 0, -8), (2, 12, 2), c, mirror=True)
    m.part('leg_br', (-3, 12, 7)).box((-4, 0, 6), (2, 12, 2), c)
    m.part('leg_bl', (3, 12, 7)).box((2, 0, 6), (2, 12, 2), c, mirror=True)
    m.part('tail', (0, 20, 9)).box((-1, 17, 9), (2, 4, 2), c)
    m.export(); m.preview(f'{PREVIEW}/stag.png')

def demon():
    import demon as fleau
    fleau.demon(f'{PREVIEW}/demon.png')

def ash_lord():
    """Seigneur de Cendre : modele genere (tools/ashlord.py), importe via bbimport"""
    import bbimport, os, tempfile
    import ashlord
    from genlib import wjson, save
    from PIL import Image
    tmp = tempfile.mkdtemp()
    ashlord.generate(tmp)
    src = os.path.join(tmp, 'ash_lord.bbmodel')
    geo = bbimport.convert(src, rename={'leg_left': 'leg_l', 'leg_right': 'leg_r'})
    wjson(f'{A}/geo/ash_lord.json', geo)
    save(Image.open(os.path.join(tmp, 'ash_lord_texture.png')).convert('RGBA'), f'{A}/textures/entity/ash_lord_geo.png')

def geo_height(geo):
    """hauteur du modele en pixels (sol a y=24), arme exclue, rotations appliquees comme MC (rotateZYX)"""
    import math
    import numpy as np
    by = {p['name']: p for p in geo['parts']}
    def mat(r):
        x, y, z = r
        rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
        ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
        rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
        return rz @ ry @ rx
    world = {}
    def frame(n):
        if n in world: return world[n]
        p = by[n]
        R = mat(p['rot']); t = np.array(p['pivot'], float)
        if p['parent']:
            PR, Pt = frame(p['parent'])
            R, t = PR @ R, PR @ t + Pt
        world[n] = (R, t)
        return world[n]
    def is_weapon(n):
        while n:
            if n.startswith('weapon'): return True
            n = by[n]['parent']
        return False
    top = 24.0
    for n, p in by.items():
        if is_weapon(n): continue
        R, t = frame(n)
        for b in p['boxes']:
            o, sz, inf = b['o'], b['s'], b.get('inflate', 0)
            for dx in (0, 1):
                for dy in (0, 1):
                    for dz in (0, 1):
                        v = np.array([o[0] - inf + dx * (sz[0] + 2 * inf), o[1] - inf + dy * (sz[1] + 2 * inf), o[2] - inf + dz * (sz[2] + 2 * inf)])
                        top = min(top, (R @ v + t)[1])
    return round(24.0 - top, 2)

def orcs():
    """7 modeles d'orques (tools/orcs.py) : geometrie + une texture par couleur de peau (meme disposition UV)"""
    import bbimport, tempfile
    import orcs as O
    from genlib import wjson, save
    from PIL import Image
    ren = {'leg_left': 'leg_l', 'leg_right': 'leg_r', 'arm_left': 'arm_l', 'arm_right': 'arm_r'}
    for name, (fn, skins, f) in O.MODELS.items():
        ref = None
        for i, sk in enumerate(skins):
            tmp = tempfile.mkdtemp()
            groups, cubes, W, H, tex = O.build(name, sk)
            O.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
            geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'), rename=ren)
            if ref is None:
                ref = geo
                geo['height'] = geo_height(geo)
                wjson(f'{A}/geo/{name}.json', geo)
                print(f'orc {name}: {len(geo["parts"])} pieces, hauteur {geo["height"]} px, texture {W}x{H}')
            else:
                assert geo['parts'] == ref['parts'] or [ (p['name'], p['boxes']) for p in geo['parts'] ] == [ (p['name'], p['boxes']) for p in ref['parts'] ], f'{name}: disposition UV differente pour la peau {sk}'
            save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}_{i}.png')

def treeman_bb():
    """Sylvestre : modele Blockbench fourni (tools/assets/treeman.bbmodel)"""
    import bbimport, os
    from genlib import wjson, save
    src = os.path.join(os.path.dirname(__file__), 'assets', 'treeman.bbmodel')
    geo = bbimport.convert(src, rename={'torse': 'body', 'tete': 'head', 'jambe_G': 'leg_l', 'jambe_D': 'leg_r', 'bras_G': 'arm_l', 'bras_D': 'arm_r'})
    wjson(f'{A}/geo/treeman.json', geo)
    save(bbimport.texture(src), f'{A}/textures/entity/treeman.png')

def glow(m, name, chars):
    """texture lumineuse : uniquement les pixels d'art dont le caractere est dans chars (+ boites tres vives)"""
    from PIL import Image
    from genlib import save
    im = Image.new('RGBA', (m.tw, m.th), (0, 0, 0, 0))
    for p in m.parts:
        for b in p.boxes:
            for face, (x0, y0, fw, fh) in m.face_rects(b).items():
                art = b.art.get(face)
                if art:
                    for yy, row in enumerate(art):
                        for xx, ch in enumerate(row):
                            if ch in chars and yy < fh and xx < fw:
                                c = b.pal[ch]; c = rgb(c) if isinstance(c, str) else c
                                im.putpixel((x0 + xx, y0 + yy), c + (255,))
                if sum(b.color) > 380 and b.color[0] > 200 and b.color[2] < 90:
                    for yy in range(fh):
                        for xx in range(fw): im.putpixel((x0 + xx, y0 + yy), b.color + (255,))
    save(im, f'{A}/textures/entity/{name}.png')

def lurker():
    m = Model('lurker', 128, 64, 13)
    c = '#27332e'; lite = '#3d5146'
    b = m.part('body', (0, 8, 0))
    b.box((-10, 0, -10), (20, 16, 20), c, noise=0.2, art={'front': ['....................', '....................', '....................', '...ggg........ggg...', '...gkg........gkg...', '...ggg........ggg...',
          '....................', '....................', '.......wkkkkw.......', '......wkkkkkkw......', '.......wkkkkw.......']}, pal=EYE)
    b.box((-7, 16, -7), (14, 5, 14), lite, noise=0.2)
    import math
    for i in range(8):
        a = i * math.pi / 4 + 0.39
        x, z = round(math.cos(a) * 9), round(math.sin(a) * 9)
        t = m.part(f'tent{i + 1}', (x, 10, z))
        t.box((x - 2, 10, z - 2), (4, 16, 4), c, noise=0.2)
        t2 = m.part(f'tent{i + 1}b', (x, 26, z), f'tent{i + 1}')
        t2.box((x - 2, 26, z - 1), (3, 16, 3), lite, noise=0.2)
    m.export(); m.preview(f'{PREVIEW}/lurker.png')
    glow(m, 'lurker_glow', 'g')

def treeman():
    m = Model('treeman', 128, 128, 15)
    bark = '#5c4832'; dark = '#463624'; leaf = '#3f7a34'; moss = '#56743a'
    b = m.part('body', (0, 48, 0))
    b.box((-8, 30, -6), (16, 34, 12), bark, noise=0.22, art={'front': ['................', '................', '....m...........', '...mm......m....', '....m.....mm....']}, pal=EYE)
    h = m.part('head', (0, 64, 0), 'body')
    h.box((-6, 64, -6), (12, 12, 12), bark, noise=0.22, art={'front': ['............', '............', '.dd......dd.', '.ee......ee.', '............', '.....dd.....', '.....dd.....', '..mmmmmmmm..', '..mmdddmmm..', '...mmmmmm...', '....mmmm....', '.....mm.....']}, pal=EYE)
    h.box((-12, 75, -12), (24, 9, 24), leaf, noise=0.3); h.box((-8, 84, -8), (16, 6, 16), shade(rgb(leaf), 1.1), noise=0.3)
    ar = m.part('arm_r', (-9, 60, 0), 'body'); ar.box((-15, 26, -3), (6, 34, 6), dark, noise=0.22)
    ar.box((-17, 20, -2), (2, 8, 2), dark); ar.box((-12, 19, -4), (2, 8, 2), dark); ar.box((-14, 56, -5), (8, 6, 10), moss, noise=0.3)
    al = m.part('arm_l', (9, 60, 0), 'body'); al.box((9, 26, -3), (6, 34, 6), dark, noise=0.22, mirror=True)
    al.box((15, 20, -2), (2, 8, 2), dark); al.box((10, 19, -4), (2, 8, 2), dark); al.box((6, 56, -5), (8, 6, 10), moss, noise=0.3)
    lr = m.part('leg_r', (-4, 30, 0)); lr.box((-9, 0, -4), (8, 30, 8), bark, noise=0.22); lr.box((-11, 0, -7), (12, 3, 13), dark, noise=0.22)
    ll = m.part('leg_l', (4, 30, 0)); ll.box((1, 0, -4), (8, 30, 8), bark, noise=0.22, mirror=True); ll.box((-1, 0, -7), (12, 3, 13), dark, noise=0.22)
    m.export(); m.preview(f'{PREVIEW}/treeman.png')

def eagle():
    m = Model('eagle', 128, 64, 17)
    c = '#6b4a2c'; white = '#efe9dc'; yellow = '#e6b422'
    m.part('body', (0, 14, 0)).box((-6, 8, -10), (12, 10, 22), c, noise=0.16)
    h = m.part('head', (0, 18, -10))
    h.box((-4, 15, -17), (8, 8, 8), white, art={'right': ['........', '........', '.k......'], 'left': ['........', '........', '......k.']}, pal=EYE)
    h.box((-1, 16, -21), (2, 3, 4), yellow); h.box((-1, 15, -21), (2, 1, 2), yellow)
    m.part('wing_r', (-6, 17, -2)).box((-36, 15, -8), (30, 2, 16), shade(rgb(c), 0.85), noise=0.2)
    m.part('wing_l', (6, 17, -2)).box((6, 15, -8), (30, 2, 16), shade(rgb(c), 0.85), noise=0.2, mirror=True)
    m.part('tail', (0, 13, 12)).box((-5, 12, 12), (10, 2, 10), white)
    m.part('leg_r', (-3, 8, 2)).box((-4, 0, 1), (2, 8, 2), yellow)
    m.part('leg_l', (3, 8, 2)).box((2, 0, 1), (2, 8, 2), yellow, mirror=True)
    m.export(); m.preview(f'{PREVIEW}/eagle.png')
    m.retexture('crow', lambda col: (26, 24, 30) if sum(col) < 600 or col[2] > 180 else (60, 56, 50), 18)

def dragon():
    m = Model('dragon', 256, 128, 19)
    c = '#7c211b'; belly = '#c08a3a'; dark = '#521410'; bone = '#d8cfb4'
    b = m.part('body', (0, 20, 0))
    b.box((-10, 10, -16), (20, 16, 36), c, noise=0.2, faces={'bottom': belly})
    for k in range(5): b.box((-1, 26, -12 + k * 7), (2, 4, 4), dark)
    n = m.part('neck', (0, 22, -16)); n.box((-5, 17, -30), (10, 10, 14), c, noise=0.2, faces={'bottom': belly})
    h = m.part('head', (0, 25, -30), 'neck')
    h.box((-6, 22, -44), (12, 8, 14), c, noise=0.2, art={'right': ['..............', '..y...........', '..y...........'], 'left': ['..............', '...........y..', '...........y..'], 'front': ['............', '............', '............', '............', '...kk..kk...']}, pal=EYE)
    h.box((-5, 30, -34), (2, 6, 2), bone); h.box((3, 30, -34), (2, 6, 2), bone)
    j = m.part('jaw', (0, 22, -31), 'head'); j.box((-5, 20, -43), (10, 2, 12), dark, art={'front': ['w.w.w.w.w.']}, pal=EYE)
    wr = m.part('wing_r', (-10, 25, -6), 'body'); wr.box((-52, 24, -10), (42, 1, 28), dark, noise=0.25); wr.box((-52, 25, -10), (42, 2, 3), c)
    wl = m.part('wing_l', (10, 25, -6), 'body'); wl.box((10, 24, -10), (42, 1, 28), dark, noise=0.25, mirror=True); wl.box((10, 25, -10), (42, 2, 3), c, mirror=True)
    m.part('leg_fr', (-9, 12, -10)).box((-15, 0, -14), (8, 13, 8), c, noise=0.2)
    m.part('leg_fl', (9, 12, -10)).box((7, 0, -14), (8, 13, 8), c, noise=0.2, mirror=True)
    m.part('leg_br', (-9, 12, 14)).box((-15, 0, 10), (8, 13, 8), c, noise=0.2)
    m.part('leg_bl', (9, 12, 14)).box((7, 0, 10), (8, 13, 8), c, noise=0.2, mirror=True)
    t = m.part('tail', (0, 18, 20)); t.box((-6, 12, 20), (12, 10, 20), c, noise=0.2, faces={'bottom': belly})
    t2 = m.part('tail2', (0, 17, 40), 'tail'); t2.box((-4, 13, 40), (8, 7, 20), c, noise=0.2)
    m.part('tail3', (0, 16, 60), 'tail2').box((-2, 14, 60), (4, 4, 16), dark)
    m.export(); m.preview(f'{PREVIEW}/dragon.png')
    m.retexture('winged_beast', lambda col: shade((col[0] * 0.2 + 20, col[0] * 0.2 + 18, col[0] * 0.2 + 26), 1.0), 20)

def mammoth():
    m = Model('mammoth', 256, 128, 23)
    c = '#6a5544'; dark = '#4c3c30'; ivory = '#e8dfc4'
    m.part('body', (0, 34, 0)).box((-12, 22, -18), (24, 24, 38), c, noise=0.25)
    h = m.part('head', (0, 40, -18))
    h.box((-8, 28, -30), (16, 18, 12), c, noise=0.25, art={'front': ['................', '................', '................', '................', '................', '................', '.wk..........kw.']}, pal=EYE)
    h.box((-14, 30, -22), (6, 12, 2), dark); h.box((8, 30, -22), (6, 12, 2), dark)
    h.box((-7, 16, -34), (3, 14, 3), ivory); h.box((4, 16, -34), (3, 14, 3), ivory)
    h.box((-7, 16, -44), (3, 3, 10), ivory); h.box((4, 16, -44), (3, 3, 10), ivory)
    tr = m.part('tail2', (0, 30, -30), 'head'); tr.box((-3, 6, -34), (6, 24, 5), dark, noise=0.2)
    m.part('leg_fr', (-7, 22, -12)).box((-12, 0, -17), (9, 22, 9), dark, noise=0.2)
    m.part('leg_fl', (7, 22, -12)).box((3, 0, -17), (9, 22, 9), dark, noise=0.2, mirror=True)
    m.part('leg_br', (-7, 22, 14)).box((-12, 0, 10), (9, 22, 9), dark, noise=0.2)
    m.part('leg_bl', (7, 22, 14)).box((3, 0, 10), (9, 22, 9), dark, noise=0.2, mirror=True)
    m.part('tail', (0, 40, 20)).box((-1, 26, 20), (2, 14, 2), dark)
    m.export(); m.preview(f'{PREVIEW}/mammoth.png')

TREE = (['.g....', 'ggg...', '.g.g..', '.sggg.', '.s.g..', 'sss...'], {'g': '#e8e4d0', 's': '#e8e4d0'})

def skins():
    # --- orques et gobelins
    skin('orc_1', 'orc', skin='#5f7248', hair='#1c1a18', hair_style='bald', eyes='#e8b020', top='#3a302a', legs='#2c2622', boots='#1c1816', armor='chain', sleeves=False, seed=1)
    skin('orc_2', 'orc', skin='#6a6a4a', hair='#1c1a18', hair_style='short', eyes='#d04020', top='#4a3528', legs='#332a24', boots='#1c1816', sleeves=False, pauldrons='#3a3a40', seed=2)
    skin('orc_3', 'orc', skin='#4f6252', hair='#1c1a18', hair_style='bald', eyes='#e8b020', top='#2e2a2c', legs='#262224', boots='#151214', armor='chain', seed=3)
    skin('war_orc', 'orc', skin='#3a3a34', hair='#101010', hair_style='mid', eyes='#ff5020', top='#26262c', trim='#8a1a14', legs='#1e1e22', boots='#101012', armor='plate', pauldrons='#34343c',
         emblem=(['.rr.', 'rrrr', '.rr.', '.rr.'], {'r': '#e8e0d0'}), seed=4)
    skin('orc_chief', 'orc', skin='#4a4034', hair='#101010', hair_style='helmet', eyes='#ff3010', top='#2a2226', trim='#a02018', legs='#1e1a1c', boots='#101010', armor='plate', pauldrons='#5a1c16', seed=5)
    skin('goblin_1', 'goblin', skin='#7a8a52', hair='#2a2a20', hair_style='bald', eyes='#f0e040', top='#4a3a2a', legs='#3a2e22', boots='#5a6a3a', sleeves=False, seed=6)
    skin('goblin_2', 'goblin', skin='#8a8a6a', hair='#2a2a20', hair_style='bald', eyes='#f0e040', top='#3a3430', legs='#2e2a26', boots='#6a6a4a', sleeves=False, seed=7)
    # --- hommes
    skin('human_1', 'human', skin='#c99a78', hair='#5a3a22', top='#8a5a3a', trim='#5a3a22', legs='#4a4034', boots='#2a2018', seed=10)
    skin('human_2', 'human', skin='#d8ac88', hair='#c9a25a', hair_style='long', top='#4a6a8a', trim='#e0d6c0', legs='#5a4a3a', boots='#2a2018', skirt=True, seed=11)
    skin('human_3', 'human', skin='#b88662', hair='#2a2018', top='#6a7a4a', trim='#3a4a2a', legs='#3a3028', boots='#2a2018', beard='#2a2018', seed=12)
    skin('human_4', 'human', skin='#d0a080', hair='#8a4a22', hair_style='long', top='#8a3a3a', trim='#e0c070', legs='#4a3a34', boots='#2a2018', skirt=True, seed=13)
    skin('human_guard', 'human', skin='#c99a78', hair='#8a8f98', hair_style='helmet', top='#2a4a3a', trim='#c9a23a', legs='#3a3a3a', boots='#2a2018', armor='chain', pauldrons='#8a8f98',
         emblem=(['..yy..', '.yyyy.', '..yy..', '..yy..', '.yyyy.'], {'y': '#e8d8a0'}), seed=14)
    skin('ranger', 'human', skin='#c09070', hair='#3a2a1c', hair_style='mid', top='#3a4a34', trim='#2a3424', legs='#3a3428', boots='#2a2018', hood='#2f3d2a', beard='#3a2a1c', gloves='#3a2a1c', seed=15)
    skin('mage', 'human', skin='#d8b8a0', hair='#d8d8d8', hair_style='long', top='#7a7a80', trim='#5a5a60', legs='#6a6a70', boots='#3a3a3a', beard='#e0e0e0', beard_len=6, hood='#7a7a80', skirt=True, seed=16)
    skin('wild_man', 'human', skin='#a87a58', hair='#2a1c12', hair_style='long', top='#6a5038', trim='#3a2a1c', legs='#5a4430', boots='#a87a58', beard='#2a1c12', sleeves=False, seed=17)
    # --- elfes
    skin('elf_1', 'elf', skin='#ecd8c4', hair='#e8d890', hair_style='long', eyes='#4a8ac0', top='#3a6a4a', trim='#d8c070', legs='#5a5a4a', boots='#4a3a28', skirt=True, seed=20)
    skin('elf_2', 'elf', skin='#e8d0bc', hair='#2a2420', hair_style='long', eyes='#6a9a6a', top='#5a6a8a', trim='#c8d0e0', legs='#4a4a5a', boots='#3a3028', skirt=True, seed=21)
    skin('elf_3', 'elf', skin='#f0dcc8', hair='#c8c8d0', hair_style='long', eyes='#5a7ab0', top='#8a8a6a', trim='#e0d090', legs='#5a5a4a', boots='#4a3a28', skirt=True, seed=22)
    skin('elf_guard', 'elf', skin='#ecd8c4', hair='#e8d890', hair_style='long', eyes='#4a8ac0', top='#2f5a40', trim='#e0c060', legs='#4a4a3a', boots='#3a3028', armor='plate', pauldrons='#d8c070', crown='#e0c060', seed=23)
    # --- nains
    skin('dwarf_1', 'dwarf', skin='#c89070', hair='#8a3a1a', hair_style='mid', top='#4a3a2a', trim='#c9a23a', legs='#3a3430', boots='#2a2420', beard='#8a3a1a', beard_len=7, armor='chain', seed=30)
    skin('dwarf_2', 'dwarf', skin='#c08868', hair='#3a2a20', hair_style='helmet', top='#3a4a5a', trim='#8a8f98', legs='#34302c', boots='#2a2420', beard='#3a2a20', beard_len=8, pauldrons='#8a8f98', seed=31)
    skin('dwarf_3', 'dwarf', skin='#cc9878', hair='#b8b0a8', hair_style='mid', top='#5a2a22', trim='#c9a23a', legs='#3a3430', boots='#2a2420', beard='#c8c0b8', beard_len=9, seed=32)
    skin('dwarf_guard', 'dwarf', skin='#c89070', hair='#7a7f88', hair_style='helmet', top='#3a3a44', trim='#c9a23a', legs='#2e2e34', boots='#22201e', beard='#6a3a1a', beard_len=7, armor='plate', pauldrons='#7a7f88', seed=33)
    # --- semi-hommes
    skin('halfling_1', 'half', skin='#d8a888', hair='#6a4424', hair_style='mid', top='#5a7a3a', trim='#c9b070', legs='#6a5034', boots='#d8a888', seed=40)
    skin('halfling_2', 'half', skin='#e0b494', hair='#a86a2a', hair_style='long', top='#b8943a', trim='#7a3a2a', legs='#5a4a3a', boots='#e0b494', skirt=True, seed=41)
    skin('halfling_3', 'half', skin='#d0a080', hair='#3a2818', hair_style='mid', top='#7a3a34', trim='#e0d0a0', legs='#4a4034', boots='#d0a080', seed=42)
    skin('halfling_elder', 'half', skin='#d8ac90', hair='#d0d0d0', hair_style='mid', top='#4a5a7a', trim='#c9a23a', legs='#5a5044', boots='#d8ac90', seed=43)
    # --- spectres et seigneurs
    skin('pale_rider', 'void', skin='#0c0a0e', hair='#0c0a0e', hair_style='bald', top='#121016', trim='#0a080c', legs='#0e0c12', boots='#3a3a44', hood='#141218', gloves='#4a4a54', skirt=True, seed=50)
    skin('wight', 'ghost', skin='#b8c8c0', hair='#8a9a94', hair_style='long', eyes='#b0fff0', top='#7a8a84', trim='#5a6a64', legs='#6a7a74', boots='#5a6a64', crown='#9ab0a0', skirt=True, alpha=235, seed=51)
    skin('dead_soldier', 'ghost', skin='#7ad8a0', hair='#5ab080', hair_style='helmet', eyes='#e0fff0', top='#5ab888', trim='#3a9068', legs='#4aa878', boots='#3a9068', armor='chain', alpha=200, seed=52)
    skin('sorcerer', 'human', skin='#b8b0a8', hair='#e8e8e8', hair_style='long', eyes='#ff3020', top='#e8e4dc', trim='#1c1c20', legs='#d8d4cc', boots='#2a2a2e', beard='#e8e8e8', beard_len=8, skirt=True, seed=53)
    skin('ash_lord', 'lord', skin='#141216', hair='#1c1a20', hair_style='helmet', eyes='#ff5010', top='#1a181c', trim='#c02a10', legs='#141216', boots='#0c0a0e', armor='plate', pauldrons='#24222a', crown='#2a2830', gloves='#1c1a20',
         emblem=(['r.rr.r', '.r..r.', 'r.rr.r', '.r..r.', 'r.rr.r'], {'r': '#ff5a18'}), seed=54)
    skin('cave_thief', 'goblin', skin='#b8b8a8', hair='#b8b8a8', hair_style='bald', eyes='#d8f0ff', top='#b0b0a0', legs='#6a6050', boots='#b8b8a8', sleeves=False, seed=55)

def build():
    troll(); warg(); stag(); demon(); ash_lord(); orcs(); lurker(); treeman_bb(); eagle(); dragon(); mammoth(); skins()
