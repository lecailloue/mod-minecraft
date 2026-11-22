"""Modeles 3D des creatures + peaux des humanoides."""
import os
import numpy as np
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

def warg_paint(m, name='warg', scars=0.3, gore=0.3, seed=1313, stripe=0.74, extra=0):
    """texture detaillee du Warg : poils en meches (racines sombres, pointes pales), rayures de hyene, cotes saillantes, cicatrices,
    gueule baveuse sanglante avec crocs, yeux ambre a pupille fendue (calque lumineux warg_glow)"""
    import random
    from PIL import Image
    rnd = random.Random(seed)
    path = f'{A}/textures/entity/{name}.png'
    im = Image.open(path).convert('RGBA'); px = im.load()
    gl = Image.new('RGBA', im.size, (0, 0, 0, 0)); gp = gl.load()
    def put(x, y, c):
        if 0 <= x < im.width and 0 <= y < im.height: px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (255,)
    def mulc(x, y, f, tint=None):
        if not (0 <= x < im.width and 0 <= y < im.height): return
        r, g, b, a = px[x, y]
        if a == 0: return
        c = [r * f, g * f, b * f]
        if tint: c = [c[i] * 0.7 + tint[i] * 0.3 for i in range(3)]
        px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (255,)
    blood, gum, bone, pale = (96, 14, 12), (120, 24, 22), (232, 224, 196), (178, 164, 138)
    spikes, skull, muzzle, jaws = [], None, None, None
    for p in m.parts:
        for b in p.boxes:
            w, h, d = b.s
            if min(b.s) <= 1 and w * h * d <= 4: continue                        # crocs, griffes, truffe : laisses nets
            if p.name == 'head' and b.s == (10, 10, 9): skull = b
            if p.name == 'head' and b.s == (7, 6, 7): muzzle = b
            if p.name == 'jaw': jaws = b
            spike = p.name == 'body' and b.o[1] >= 21.5 and w == 2
            if spike: spikes.append(b)
            for face, (x0, y0, fw, fh) in m.face_rects(b).items():
                for xx in range(fw):
                    y = 0
                    while y < fh:
                        L = rnd.randint(2, 5); t = rnd.uniform(-0.17, 0.1)
                        for k in range(L):
                            if y + k >= fh: break
                            f = 1 + t - 0.06 * (k == 0) + (0.2 if k == L - 1 and rnd.random() < 0.6 else 0)
                            mulc(x0 + xx, y0 + y + k, f, pale if (k == L - 1 and rnd.random() < 0.5) else None)
                        y += L
                if face in ('left', 'right', 'front', 'back') and p.name in ('body', 'leg_fr', 'leg_fl', 'leg_br', 'leg_bl', 'neck') and fw >= 3 and fh >= 4:
                    off = rnd.randint(0, 3)                                         # rayures de hyene irregulieres
                    for xx in range(fw):
                        if (xx + off) % 4 == 0:
                            a, z = rnd.randint(0, fh // 3), rnd.randint(fh // 2, fh - 1)
                            for yy in range(a, z + 1):
                                if rnd.random() < 0.6: mulc(x0 + xx, y0 + yy, stripe)
                if p.name == 'body' and face in ('left', 'right') and h >= 10 and fw >= 10:  # cotes saillantes (maigre, affame)
                    for yy in (fh - 8, fh - 6, fh - 4, fh - 2):
                        for xx in range(2, fw - 1):
                            if rnd.random() < 0.8: mulc(x0 + xx, y0 + yy, 0.62)
                    for xx in range(2, fw - 1):
                        mulc(x0 + xx, y0 + fh - 7, 1.18); mulc(x0 + xx, y0 + fh - 5, 1.15)
                if p.name in ('body', 'head', 'neck') and face in ('left', 'right', 'front') and rnd.random() < scars and fw >= 5 and fh >= 5:   # cicatrices
                    sx, sy = x0 + rnd.randint(0, fw - 4), y0 + rnd.randint(0, fh - 4); n = rnd.randint(3, 4) + (2 if scars > 0.4 else 0)
                    for k in range(n): put(sx + k, sy + k, (170, 96, 88)); mulc(sx + k + 1, sy + k, 0.55); mulc(sx + k, sy + k + 1, 0.6)
                if face == 'bottom' or (p.name == 'body' and face == 'top' and not spike): mulc(x0 + fw // 2, y0 + fh // 2, 0.85)
    # crete : pointes givrees, bases sombres
    for b in spikes:
        for face, (x0, y0, fw, fh) in m.face_rects(b).items():
            if face == 'top':
                for xx in range(fw):
                    for yy in range(fh): put(x0 + xx, y0 + yy, (196, 184, 160))
            elif fh >= 2:
                for xx in range(fw):
                    mulc(x0 + xx, y0, 1.55, pale)
                    if fh > 3: mulc(x0 + xx, y0 + fh - 1, 0.55)
    # --- visage
    def eye(x0, y0, left):                                                         # petit oeil jaune oblique et mechant, sous une arcade sombre
        cx = x0 + (1 if left else 6)
        shape = [(0, 0, 'a'), (1, 0, 'a'), (1, 1, 'b'), (2, 1, 'b')] if left else [(1, 0, 'a'), (2, 0, 'a'), (0, 1, 'b'), (1, 1, 'b')]
        for dx in range(-1, 4): mulc(cx + dx, y0 + 1, 0.35); mulc(cx + dx, y0 + 4, 0.7)       # orbites creusees
        for dx, dy, k in shape:
            put(cx + dx, y0 + 2 + dy, (255, 222, 60) if k == 'a' else (236, 168, 24)); gp[cx + dx, y0 + 2 + dy] = (255, 190, 20, 255)
    if skull is not None:
        x0, y0, fw, fh = m.face_rects(skull)['front']
        eye(x0, y0, True); eye(x0, y0, False)
        for dx in range(0, 5): mulc(x0 + dx, y0 + 1, 0.4); mulc(x0 + 9 - dx, y0 + 1, 0.4)   # sourcils en colere
        for dx in (4, 5): 
            for dy in range(1, 6): mulc(x0 + dx, y0 + dy, 0.62)                        # sillon du front
        for k in range(3): put(x0 + 1 + k, y0 + 8 - (k % 2), (110, 20, 18)); put(x0 + 8 - k, y0 + 8 - (k % 2), (110, 20, 18))   # plis de rictus
        if extra >= 3:                                                              # balafre en travers du museau
            for k in range(5): put(x0 + 1 + k, y0 + 5 + (k + 1) // 2, (176, 100, 92)); mulc(x0 + 1 + k, y0 + 6 + (k + 1) // 2, 0.5)
        if extra >= 4:
            for k in range(4): put(x0 + 8 - k, y0 + 6 + k // 2, (176, 100, 92)); mulc(x0 + 8 - k, y0 + 7 + k // 2, 0.5)
        for face in ('left', 'right'):
            fx, fy, fw2, fh2 = m.face_rects(skull)[face]
            for k in range(4): mulc(fx + 1 + k, fy + 5 + k // 2, 0.4)
    if muzzle is not None:
        x0, y0, fw, fh = m.face_rects(muzzle)['front']
        for xx in range(fw):
            put(x0 + xx, y0 + fh - 1, gum); put(x0 + xx, y0 + fh - 2, gum)
            if xx % 2 == 0: put(x0 + xx, y0 + fh - 2, bone); put(x0 + xx, y0 + fh - 3, bone)           # rangee de crocs devoiles
        put(x0 + 1, y0 + 1, (14, 10, 8)); put(x0 + fw - 2, y0 + 1, (14, 10, 8))                           # narines
        for face in ('left', 'right'):
            fx, fy, fw2, fh2 = m.face_rects(muzzle)[face]
            for xx in range(fw2):
                put(fx + xx, fy + fh2 - 1, gum)
                if xx % 2 == 1: put(fx + xx, fy + fh2 - 2, bone)
                if rnd.random() < 0.5: mulc(fx + xx, fy + fh2 - 3, 0.5, blood)
    if jaws is not None:
        for face, (x0, y0, fw, fh) in m.face_rects(jaws).items():
            for xx in range(fw):
                for yy in range(fh):
                    if face == 'top': put(x0 + xx, y0 + yy, gum if (xx + yy) % 3 else (60, 8, 8))
                    elif rnd.random() < 0.55: mulc(x0 + xx, y0 + yy, 0.55, blood)             # menton ensanglante
            if face == 'front':
                for xx in range(0, fw, 2): put(x0 + xx, y0, bone)
    # sang seche sur le poitrail et les pattes, griffes pales
    for p in m.parts:
        if p.name in ('neck', 'leg_fr', 'leg_fl'):
            for b in p.boxes:
                if min(b.s) <= 1: continue
                for face in ('front', 'left', 'right'):
                    x0, y0, fw, fh = m.face_rects(b)[face]
                    for xx in range(fw):
                        if rnd.random() < gore:
                            for k in range(rnd.randint(1, 3)): mulc(x0 + xx, y0 + fh - 1 - k, 0.5, blood)
    im.save(path)
    gl.save(f'{A}/textures/entity/warg_glow.png')


def warg():
    """Warg : enorme loup-hyene de la Terre du Milieu, dos vouté et crinière hérissée, tete lourde, pas de selle"""
    m = Model('warg', 64, 64, 7)
    c = '#5a4a3a'; dark = '#2e251f'; mid = '#46392f'; lt = '#7c6850'; belly = '#5e4e3f'; fang = '#e6dec8'
    b = m.part('body', (0, 14, 0))
    b.box((-6, 10, -10), (12, 12, 11), c, noise=0.2, grad=lt)            # epaules massives, plus hautes que la croupe
    b.box((-5, 10, 1), (10, 9, 11), mid, noise=0.2, grad=c)              # arriere-train plus bas
    b.box((-4, 8, -6), (8, 3, 13), belly, noise=0.16)                    # ventre
    b.box((-7, 12, -9), (1, 8, 7), mid, noise=0.25); b.box((6, 12, -9), (1, 8, 7), mid, noise=0.25)   # touffes de flanc
    b.box((-5, 22, -9), (10, 2, 10), lt, noise=0.25)                     # bosse d'epaules (crinière)
    # crete de poils hérissés le long de l'echine (3 rangees) : plus haute aux epaules, decroit vers la croupe
    for i, z in enumerate((-9, -6.5, -4, -1.5, 1, 3.5, 6, 8.5, 11)):
        h = max(2, 6 - i * 0.5)
        y0 = 24 - max(0, (z + 1)) * 0.28 if z > -1 else 24
        b.box((-1, y0, z), (2, round(h), 2), lt if i % 2 else c, noise=0.3)
        if i < 7:
            b.box((-4, y0 - 1, z + 0.5), (2, round(h * 0.6) + 1, 2), c, noise=0.3)
            b.box((2, y0 - 1, z + 0.5), (2, round(h * 0.6) + 1, 2), c, noise=0.3)
    n = m.part('neck', (0, 19, -9))
    n.box((-4, 15, -16), (8, 8, 8), c, noise=0.22)                       # cou epais
    n.box((-5.5, 14, -15), (11, 3, 5), lt, noise=0.3)                    # collerette de poils
    n.box((-4.5, 21, -15), (9, 3, 6), lt, noise=0.3)
    h = m.part('head', (0, 17, -16), parent='neck')
    h.box((-5, 12, -24), (10, 10, 9), c)
    h.box((-3.5, 12, -31), (7, 6, 7), mid, noise=0.18)                     # museau
    h.box((-2.5, 15.5, -32), (5, 2, 1), dark)                            # truffe
    h.box((-5, 20.5, -24.6), (10, 2, 1), dark, noise=0.3)                 # arcade sourciliere
    h.box((-6, 12, -20), (1, 6, 6), lt, noise=0.3); h.box((5, 12, -20), (1, 6, 6), lt, noise=0.3)   # favoris
    h.box((-5.5, 22, -19), (3, 5, 2), mid, noise=0.18); h.box((2.5, 22, -19), (3, 5, 2), mid, noise=0.18)  # oreilles pointues
    h.box((-3.3, 10.2, -30), (1, 2, 1), fang); h.box((2.3, 10.2, -30), (1, 2, 1), fang)                  # crocs superieurs
    h.box((-3.3, 11, -27), (1, 1, 1), fang); h.box((2.3, 11, -27), (1, 1, 1), fang)
    j = m.part('jaw', (0, 13, -23), parent='head')
    j.box((-3, 10, -30), (6, 2, 7), dark, noise=0.18)
    j.box((-2, 12, -29.5), (1, 2, 1), fang); j.box((1, 12, -29.5), (1, 2, 1), fang)                  # crocs inferieurs
    # pattes avant : grosses, musclees, griffues
    m.part('leg_fr', (-4.5, 12, -6)).box((-7, 3, -9), (5, 9, 6), c, noise=0.22).box((-6.5, 0, -10), (5, 4, 7), dark, noise=0.2).box((-6.5, 0, -11), (1, 1, 1), fang).box((-5, 0, -11), (1, 1, 1), fang).box((-3.5, 0, -11), (1, 1, 1), fang)
    m.part('leg_fl', (4.5, 12, -6)).box((2, 3, -9), (5, 9, 6), c, noise=0.22, mirror=True).box((1.5, 0, -10), (5, 4, 7), dark, noise=0.2, mirror=True).box((1.5, 0, -11), (1, 1, 1), fang).box((3, 0, -11), (1, 1, 1), fang).box((4.5, 0, -11), (1, 1, 1), fang)
    # pattes arriere : cuisse puissante, jarret recule
    m.part('leg_br', (-4, 12, 9)).box((-6.5, 6, 7), (4, 7, 6), mid, noise=0.22).box((-6, 0, 9.5), (3, 7, 3), dark, noise=0.2).box((-6.5, 0, 8), (4, 2, 5), dark, noise=0.2)
    m.part('leg_bl', (4, 12, 9)).box((2.5, 6, 7), (4, 7, 6), mid, noise=0.22, mirror=True).box((3, 0, 9.5), (3, 7, 3), dark, noise=0.2, mirror=True).box((2.5, 0, 8), (4, 2, 5), dark, noise=0.2, mirror=True)
    t = m.part('tail', (0, 17, 12)); t.box((-1.5, 14, 12), (3, 3, 6), mid, noise=0.25)
    t2 = m.part('tail2', (0, 16, 18), parent='tail'); t2.box((-2, 11, 18), (4, 5, 6), dark, noise=0.3)
    m.export()
    warg_paint(m, 'warg', 0.3, 0.3, 1313)
    def lum(c): return sum(c[:3]) / 3
    def keep(c): return lum(c) > 190
    def silver(c): return c if keep(c) else tuple(min(255, int(lum(c) * 1.55 + k)) for k in (22, 24, 26))
    def black(c): return c if keep(c) else (int(lum(c) * 0.5), int(lum(c) * 0.5), int(lum(c) * 0.58))
    def tawny(c): return c if keep(c) else (min(255, int(c[0] * 1.2 + 16)), int(c[1] * 1.02 + 4), int(c[2] * 0.78))
    for n, (sc, go, sd) in enumerate(((0.0, 0.05, 7), (0.22, 0.15, 11), (0.5, 0.3, 13), (0.8, 0.45, 17)), 1):     # meme pelage, de plus en plus balafre
        m.retexture(f'warg_{n}', lambda c: c, seed=sd); warg_paint(m, f'warg_{n}', sc, go, 1313 + n, 0.74, extra=n)
    m.preview(f'{PREVIEW}/warg.png')

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

def spectres():
    """soldats spectraux (tools/spectres.py) : geometrie + texture + texture lumineuse des yeux"""
    import bbimport, tempfile
    import spectres as S
    from genlib import wjson, save
    from PIL import Image
    ren = {'leg_left': 'leg_l', 'leg_right': 'leg_r', 'arm_left': 'arm_l', 'arm_right': 'arm_r'}
    for name in S.MODELS:
        tmp = tempfile.mkdtemp()
        groups, cubes, W, H, tex, glow = S.build(name)
        S.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'), rename=ren)
        geo['height'] = geo_height(geo)
        wjson(f'{A}/geo/{name}.json', geo)
        save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}.png')
        save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/{name}_glow.png')
        print(f'spectre {name}: {len(geo["parts"])} pieces, hauteur {geo["height"]} px')

def geo_extent(geo):
    """etendue verticale du modele en pixels autour du point de prise (y=24 dans le geo) : (bas, haut), rotations appliquees"""
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
    lo, hi = 1e9, -1e9
    for n, p in by.items():
        R, t = frame(n)
        for b in p['boxes']:
            o, sz = b['o'], b['s']
            for dx in (0, 1):
                for dy in (0, 1):
                    for dz in (0, 1):
                        v = np.array([o[0] + dx * sz[0], o[1] + dy * sz[1], o[2] + dz * sz[2]])
                        y = 24.0 - (R @ v + t)[1]
                        lo, hi = min(lo, y), max(hi, y)
    return round(lo, 2), round(hi, 2)

# armes 3D du jeu (tools/weapons3d.py) : memes items qu'avant, rendus comme les armes d'orques (WeaponItemRenderer)
ITEM_3D = {'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 0.5, 0.5], 'scale': [0.85, 0.85, 0.85]},
           'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 0.5, 0.5], 'scale': [0.85, 0.85, 0.85]},
           'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 1.0, 1.13], 'scale': [0.68, 0.68, 0.68]},
           'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 1.0, 1.13], 'scale': [0.68, 0.68, 0.68]},
           'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.6, 0.6, 0.6]},
           'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
           'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
           'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}}
# armes d'hast (batons, lance, glaive) : tenues a la verticale, au milieu du manche (bras le long du corps : l'arme monte derriere la main)
POLE_3D = dict(ITEM_3D, thirdperson_righthand={'rotation': [0, -90, -45], 'translation': [0, 0.5, 0.5], 'scale': [0.85, 0.85, 0.85]},
               thirdperson_lefthand={'rotation': [0, 90, 45], 'translation': [0, 0.5, 0.5], 'scale': [0.85, 0.85, 0.85]},
               firstperson_righthand={'rotation': [0, 0, 45], 'translation': [1.13, 1.0, 1.13], 'scale': [0.68, 0.68, 0.68]},
               firstperson_lefthand={'rotation': [0, 0, -45], 'translation': [1.13, 1.0, 1.13], 'scale': [0.68, 0.68, 0.68]})
POLE_IDS = {'staff_fire', 'staff_light', 'staff_storm', 'staff_healing', 'rider_spear', 'elven_glaive'}
_NEUTRAL = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}
BOW_3D = dict(ITEM_3D, firstperson_righthand=_NEUTRAL, firstperson_lefthand=_NEUTRAL, thirdperson_righthand={'rotation': [-80, 260, -40], 'translation': [-1, -2, 2.5], 'scale': [0.9, 0.9, 0.9]},
              thirdperson_lefthand={'rotation': [-80, -280, 40], 'translation': [-1, -2, 2.5], 'scale': [0.9, 0.9, 0.9]})
# hauteur (unites du modele, pommeau = 0) a laquelle la main tient l'arme : poignee des epees, ou fraction de la longueur
HOLD_3D = {'viper_dagger': 8, 'halfling_dagger': 8}
HOLD_FRAC = {'elven_glaive': .45, 'rider_spear': .5, 'dwarven_axe': .3, 'dwarven_hammer': .3, 'troll_club': .3, 'ash_mace': .3, 'lunargent_pickaxe': .4,
             'staff_fire': .5, 'staff_light': .5, 'staff_storm': .5, 'staff_healing': .5, 'elven_bow': .5}
# facteur de taille en main / au sol (1 = taille du modele : 1 unite = 1 cm)
SCALE_3D = {'staff_fire': 1.4, 'staff_light': 1.4, 'staff_storm': 1.4, 'staff_healing': 1.4, 'rider_spear': 1.25, 'elven_glaive': 1.25, 'lunargent_pickaxe': 1.8,
            'dwarven_hammer': 2.1, 'dwarven_axe': 2.1, 'ash_mace': 1.3, 'troll_club': 4.4, 'valmoire_blade': 1.75, 'elven_bow': 1.15}
NO_ITEM_3D = {'medieval_sword', 'morning_star'}               # pas (encore) d'objets dans le jeu


def weapons_3d():
    """les modeles 3D d'armes (tools/weapons3d.py) remplacent les icones plates : geo + texture (+ lueur) + modele d'objet 'builtin/entity'"""
    import bbimport, tempfile
    import trollcore as T
    import weapons3d as WP
    from genlib import wjson, save
    from PIL import Image
    ws = WP.all_weapons()
    n = 0
    for id in ws:
        if id in NO_ITEM_3D:
            continue
        label, groups, cubes, W, H, tex, glow = WP.build(id, ws)
        name = 'arme_' + id
        tmp = tempfile.mkdtemp()
        T.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'))
        geo['ymin'], geo['ymax'] = geo_extent(geo)
        geo['height'] = geo['ymax']
        geo['hold'] = float(HOLD_3D.get(id, 11 if id not in HOLD_FRAC else geo['ymin'] + HOLD_FRAC[id] * (geo['ymax'] - geo['ymin'])))
        geo['scale'] = float(SCALE_3D.get(id, 1.0))
        geo['glow'] = bool(glow[..., 3].any())
        geo['translucent'] = bool(((tex[..., 3] > 0) & (tex[..., 3] < 255)).any())
        wjson(f'{A}/geo/{name}.json', geo)
        save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}.png')
        if geo['glow']:
            save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/{name}_glow.png')
        wjson(f'{A}/models/item/{id}.json', {'parent': 'minecraft:builtin/entity', 'display': BOW_3D if id == 'elven_bow' else POLE_3D if id in POLE_IDS else ITEM_3D})
        n += 1
    print(f'armes 3D : {n} objets')


def orc_weapons():
    """les armes d'orques en 3D (memes cubes que celles que portent les orques) : modele + texture, rendus comme objets (WeaponItemRenderer)"""
    import bbimport, tempfile
    import orcs as O
    import trollcore as T
    from genlib import wjson, save
    from PIL import Image
    import goblins as GB
    ws = {'orc_cleaver': O.w_cleaver, 'orc_scimitar': O.w_scimitar, 'orc_machete': O.w_notched, 'orc_spear': O.w_spear, 'orc_mace': O.w_mace, 'goblin_king_staff': GB.w_kstaff}
    item_model = {'parent': 'minecraft:builtin/entity', 'display': {
        'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 0.5, 0.5], 'scale': [0.85, 0.85, 0.85]},
        'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 0.5, 0.5], 'scale': [0.85, 0.85, 0.85]},
        'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 1.0, 1.13], 'scale': [0.68, 0.68, 0.68]},
        'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 1.0, 1.13], 'scale': [0.68, 0.68, 0.68]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.6, 0.6, 0.6]},
        'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
        'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}}}
    for id, fn in ws.items():
        name = 'arme_' + id
        T._N[0] = 0
        groups = [T.Group('arme', (0, 0, 0), (0, 0, 0), fn())]
        O.unique_names(groups)
        T.normalize(groups)
        cubes = [c for c, _ in T.walk(groups)]
        for c in cubes:                                      # une pointe a longueur negative (masse) : on la retourne
            fr, sz = list(c.frm), list(c.size)
            for i in range(3):
                if sz[i] < 0:
                    fr[i] += sz[i]; sz[i] = -sz[i]
            c.frm, c.size = tuple(fr), tuple(sz)
        W, H = T.pack(cubes)
        tex, glow = T.paint_all(cubes, W, H)
        tmp = tempfile.mkdtemp()
        T.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'))
        geo['ymin'], geo['ymax'] = geo_extent(geo)
        geo['height'] = geo['ymax']
        wjson(f'{A}/geo/{name}.json', geo)
        save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}.png')
        wjson(f'{A}/models/item/{id}.json', item_model)
        print(f'arme {id}: etendue {geo["ymin"]} .. {geo["ymax"]} px')
    wjson(f'{A}/models/item/goblin_pickaxe.json', item_model)     # la pioche du gobelin (modele issu de assets/gobelin_3.bbmodel, voir goblins())

def shadows():
    """etres des ombres (tools/shadows.py) : cavalier + cheval en un seul modele, texture lumineuse (yeux du cheval)"""
    import bbimport, tempfile
    import shadows as S
    from genlib import wjson, save
    from PIL import Image
    for name in S.MODELS:
        tmp = tempfile.mkdtemp()
        groups, cubes, W, H, tex, glow = S.build(name)
        S.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'))
        geo['height'] = geo_height(geo)
        wjson(f'{A}/geo/{name}.json', geo)
        save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}.png')
        save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/{name}_glow.png')
        print(f'ombre {name}: {len(geo["parts"])} pieces, hauteur {geo["height"]} px')

def goblins():
    """3 gobelins + leur roi (tools/goblins.py) : geometrie + une texture par couleur de peau (meme disposition UV)"""
    import bbimport, tempfile
    import goblins as GB
    from genlib import wjson, save
    from PIL import Image
    ren = {'leg_left': 'leg_l', 'leg_right': 'leg_r', 'arm_left': 'arm_l', 'arm_right': 'arm_r'}
    user = os.path.join(os.path.dirname(__file__), 'assets', 'gobelin_3.bbmodel')    # le grand gobelin a ete retouche a la main dans Blockbench
    for name, spec in GB.ALL.items():
        for i, sk in enumerate(spec['skins']):
            groups, cubes, W, H, tex, glow, f = GB.build(name, sk)
            if name == 'gobelin_3':
                if i == 0:
                    geo = bbimport.convert(user, rename=ren)
                    geo['height'] = geo_height(geo)
                    wjson(f'{A}/geo/{name}.json', geo)
                    print(f'gobelin {name} (modele Blockbench): {len(geo["parts"])} pieces, hauteur {geo["height"]} px')
                    # sa pioche : objet 3D, a plat (manche vertical), meme texture que le gobelin
                    pk = bbimport.convert(user, only='weapon', flat=True)
                    pk['ymin'], pk['ymax'] = geo_extent(pk)
                    pk['height'] = pk['ymax']
                    wjson(f'{A}/geo/arme_goblin_pickaxe.json', pk)
                    save(bbimport.texture(user), f'{A}/textures/entity/arme_goblin_pickaxe.png')
                    tex = np.array(bbimport.texture(user))
                save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}_{i}.png')
                continue
            if i == 0:
                tmp = tempfile.mkdtemp()
                GB.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
                geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'), rename=ren)
                geo['height'] = geo_height(geo)
                wjson(f'{A}/geo/{name}.json', geo)
                print(f'gobelin {name}: {len(geo["parts"])} pieces, hauteur {geo["height"]} px, texture {W}x{H}')
            save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}_{i}.png')

def giant_spider():
    """Araignee geante (tools/spiders.py) : geometrie + texture + texture lumineuse des yeux"""
    import bbimport, tempfile
    import spiders as S
    from genlib import wjson, save
    from PIL import Image
    groups, cubes, W, H, tex, glow, f = S.build()
    tmp = tempfile.mkdtemp()
    S.export_bbmodel('giant_spider', groups, cubes, W, H, tex, os.path.join(tmp, 'giant_spider.bbmodel'))
    geo = bbimport.convert(os.path.join(tmp, 'giant_spider.bbmodel'))
    geo['height'] = geo_height(geo)
    wjson(f'{A}/geo/giant_spider.json', geo)
    save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/giant_spider.png')
    save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/giant_spider_glow.png')
    print(f'araignee geante: {len(geo["parts"])} pieces, {len(cubes)} cubes, hauteur {geo["height"]} px, texture {W}x{H}')

def treeman_bb():
    """Sylvestre : modele Blockbench fourni (tools/assets/treeman.bbmodel)"""
    import bbimport, os
    from PIL import Image
    from genlib import wjson, save
    src = os.path.join(os.path.dirname(__file__), 'assets', 'treeman.bbmodel')
    geo = bbimport.convert(src, rename={'torse': 'body', 'tete': 'head', 'jambe_G': 'leg_l', 'jambe_D': 'leg_r', 'bras_G': 'arm_l', 'bras_D': 'arm_r'})
    wjson(f'{A}/geo/treeman.json', geo)
    tex = bbimport.texture(src)
    save(tex, f'{A}/textures/entity/treeman.png')
    # yeux lumineux : les quatre texels ambre (2x2 par oeil) du visage
    px = np.array(tex.convert('RGBA'))
    eyes = (px[..., 0] == 0x54) & (px[..., 1] == 0x42) & (px[..., 2] == 0x22) & (px[..., 3] > 0)
    gl = np.zeros_like(px)
    gl[eyes] = (255, 190, 50, 255)
    save(Image.fromarray(gl, 'RGBA'), f'{A}/textures/entity/treeman_glow.png')

def sylvestres():
    """Variantes du Sylvestre (tools/sylvestres.py) : geometrie + texture + texture lumineuse des yeux"""
    import bbimport, tempfile
    import trollcore as T
    import sylvestres as S
    from genlib import wjson, save
    from PIL import Image
    tmp = tempfile.mkdtemp()
    for name in S.ALL:
        groups, cubes, W, H, tex, glow, f = S.build(name)
        T.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(tmp, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(tmp, name + '.bbmodel'), rename={'leg_left': 'leg_l'})
        geo['height'] = geo_height(geo)
        wjson(f'{A}/geo/{name}.json', geo)
        save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/{name}.png')
        if glow[..., 3].any():
            save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/{name}_glow.png')
        print(f'{name}: {len(geo["parts"])} pieces, {len(cubes)} cubes, hauteur {geo["height"]} px, texture {W}x{H}')

def shadow_dragon():
    """Dragon des ombres (tools/shadow_dragon.py) : geometrie hierarchique + texture + texture lumineuse (yeux, fente du casque)"""
    import bbimport, tempfile
    import trollcore as T
    import shadow_dragon as SD
    from genlib import wjson, save
    from PIL import Image
    groups, cubes, W, H, tex, glow, lo, hi = SD.make()
    tmp = tempfile.mkdtemp()
    T.export_bbmodel('shadow_dragon', groups, cubes, W, H, tex, os.path.join(tmp, 'shadow_dragon.bbmodel'))
    geo = bbimport.convert(os.path.join(tmp, 'shadow_dragon.bbmodel'))
    geo['height'] = geo_height(geo)
    wjson(f'{A}/geo/shadow_dragon.json', geo)
    save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/shadow_dragon.png')
    save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/shadow_dragon_glow.png')
    print(f'shadow_dragon: {len(geo["parts"])} pieces, {len(cubes)} cubes, hauteur {geo["height"]} px, texture {W}x{H}')


def shadow_chief():
    """Chef des ombres (tools/shadow_chief.py) : le chef, humanoide a casque a pointes, mob a part du dragon"""
    import bbimport, tempfile
    import trollcore as T
    import shadow_chief as SM
    from genlib import wjson, save
    from PIL import Image
    groups, cubes, W, H, tex, glow, lo, hi = SM.make()
    tmp = tempfile.mkdtemp()
    T.export_bbmodel('shadow_chief', groups, cubes, W, H, tex, os.path.join(tmp, 'shadow_chief.bbmodel'))
    geo = bbimport.convert(os.path.join(tmp, 'shadow_chief.bbmodel'))
    geo['height'] = geo_height(geo)
    wjson(f'{A}/geo/shadow_chief.json', geo)
    save(Image.fromarray(tex, 'RGBA'), f'{A}/textures/entity/shadow_chief.png')
    save(Image.fromarray(glow, 'RGBA'), f'{A}/textures/entity/shadow_chief_glow.png')
    print(f'shadow_chief: {len(geo["parts"])} pieces, {len(cubes)} cubes, hauteur {geo["height"]} px, texture {W}x{H}')


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
    import dragon3d
    dragon3d.gen()

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
    skin('elf_guard', 'elf', skin='#ecd8c4', hair='#e8d890', hair_style='long', eyes='#4a8ac0', top='#2f5a40', trim='#e0c060', legs='#4a4a3a', boots='#3a3028', armor='plate', pauldrons='#d8c070', crown='#e0c060', seed=23)
    # --- nains
    skin('dwarf_guard', 'dwarf', skin='#c89070', hair='#7a7f88', hair_style='helmet', top='#3a3a44', trim='#c9a23a', legs='#2e2e34', boots='#22201e', beard='#6a3a1a', beard_len=7, armor='plate', pauldrons='#7a7f88', seed=33)
    # --- semi-hommes : voir people3d.py (30 hobbits en 3D)
    # --- spectres et seigneurs
    skin('sorcerer', 'human', skin='#b8b0a8', hair='#e8e8e8', hair_style='long', eyes='#ff3020', top='#e8e4dc', trim='#1c1c20', legs='#d8d4cc', boots='#2a2a2e', beard='#e8e8e8', beard_len=8, skirt=True, seed=53)
    skin('ash_lord', 'lord', skin='#141216', hair='#1c1a20', hair_style='helmet', eyes='#ff5010', top='#1a181c', trim='#c02a10', legs='#141216', boots='#0c0a0e', armor='plate', pauldrons='#24222a', crown='#2a2830', gloves='#1c1a20',
         emblem=(['r.rr.r', '.r..r.', 'r.rr.r', '.r..r.', 'r.rr.r'], {'r': '#ff5a18'}), seed=54)
    skin('cave_thief', 'goblin', skin='#b8b8a8', hair='#b8b8a8', hair_style='bald', eyes='#d8f0ff', top='#b0b0a0', legs='#6a6050', boots='#b8b8a8', sleeves=False, seed=55)
    import people3d
    for n, race, kw in people3d.all_skins(): skin(n, race, **kw)
    people3d.gen_java()

def beasts_gen():
    import beasts
    beasts.gen()

def build():
    troll(); warg(); demon(); ash_lord(); orcs(); spectres(); shadows(); goblins(); giant_spider(); orc_weapons(); lurker(); treeman_bb(); sylvestres(); shadow_dragon(); shadow_chief(); weapons_3d(); dragon(); beasts_gen(); skins()
