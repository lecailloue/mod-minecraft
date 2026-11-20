#!/usr/bin/env python3
"""Mobilier nain : textures, modeles, blockstates, items, recettes, loot, tags et traductions.
Autonome : N'EST PAS appele par build.py (qui efface assets/ et data/ avant de tout regenerer) -> a relancer apres un build.py.
Usage : python3 tools/furniture.py"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'ringsofash')
D = os.path.join(ROOT, 'data', 'ringsofash')
TEX = os.path.join(A, 'textures', 'block')
for p in (TEX, A + '/models/block', A + '/models/item', A + '/blockstates', D + '/recipe', D + '/loot_table/blocks',
          ROOT + '/data/minecraft/tags/block', ROOT + '/data/minecraft/tags/item'):
    os.makedirs(p, exist_ok=True)

def wj(path, obj):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, separators=(',', ':'))

# ---------------------------------------------------------------- textures
SL_A, SL_B = (77, 105, 94), (77, 88, 104)         # les deux teintes de la pierre naine
MORT = (34, 38, 46)

def clamp(v): return max(0, min(255, int(v)))
def add(c, d): return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d))
def lerp(a, b, t): return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))
def new(c=(0, 0, 0)): return Image.new('RGB', (16, 16), c)
def save(im, name): im.save(os.path.join(TEX, 'dwf_%s.png' % name))
def noisy(c, r, n=5): return add(c, r.randint(-n, n))

def stone(seed=1, w=16):
    r = random.Random(seed); im = new(MORT)
    for row in range(4):
        off = (row % 2) * 4
        for k in range(-1, 3):
            x0 = off + k * 8
            base = lerp(SL_A, SL_B, r.random())
            for y in range(row * 4, row * 4 + 3):
                for x in range(x0, x0 + 7):
                    if 0 <= x < 16:
                        c = noisy(base, r, 6)
                        if y == row * 4: c = add(c, 12)
                        if x == x0: c = add(c, 7)
                        im.putpixel((x, y), c)
    return im

def carved(seed=2):
    r = random.Random(seed); im = new(MORT)
    for y in range(1, 15):
        for x in range(1, 15):
            c = noisy(lerp(SL_A, SL_B, 0.5), r, 4)
            if y == 1 or x == 1: c = add(c, 14)
            if y == 14 or x == 14: c = add(c, -12)
            im.putpixel((x, y), c)
    gold = (196, 156, 60)
    for (x, y) in [(7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9),
                   (7, 6), (8, 6), (7, 7), (8, 7), (7, 11), (8, 11), (4, 11), (11, 11)]:
        im.putpixel((x, y), noisy(gold, r, 8))
    return im

def iron(seed=3):
    r = random.Random(seed); im = new()
    for y in range(16):
        for x in range(16):
            c = noisy((54, 57, 66), r, 4)
            if y == 0 or x == 0: c = (92, 96, 108)
            if y == 15 or x == 15: c = (30, 32, 38)
            im.putpixel((x, y), c)
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13), (8, 8)]:
        im.putpixel((x, y), (122, 126, 140)); im.putpixel((x + 1, y + 1), (24, 26, 30))
    return im

def gold(seed=4):
    r = random.Random(seed); im = new()
    for y in range(16):
        for x in range(16):
            c = noisy((200, 156, 54), r, 6)
            if y == 0 or x == 0: c = (246, 208, 104)
            if y == 15 or x == 15: c = (132, 94, 28)
            im.putpixel((x, y), c)
    return im

def wood(seed=5, dark=0):
    r = random.Random(seed); im = new()
    for b in range(4):
        base = [(98, 70, 44), (84, 59, 37), (106, 77, 49), (78, 55, 34)][(b + seed) % 4]
        base = add(base, -dark)
        for y in range(b * 4, b * 4 + 4):
            for x in range(16):
                c = noisy(base, r, 3)
                if (x + b * 5) % 7 == 0 and r.random() < .5: c = add(c, -8)
                if y == b * 4 + 3: c = add(base, -22)
                im.putpixel((x, y), c)
        im.putpixel(((b * 7 + 3) % 16, b * 4 + 1), (48, 36, 26))
    return im

def woodend():
    r = random.Random(6); im = new()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            c = (110, 80, 50) if int(d) % 2 == 0 else (92, 66, 41)
            if d > 7: c = (62, 44, 28)
            im.putpixel((x, y), noisy(c, r, 3))
    return im

def inside():
    r = random.Random(7); im = new()
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), noisy((32, 24, 18), r, 3))
    return im

def paste(dst, src, box):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1): dst.putpixel((x, y), src.getpixel((x, y)))

def chest_front(side=False):
    im = wood(8); st = stone(9); ir = iron(10); wd = wood(8, 18)
    out = new((24, 20, 18))
    paste(out, wd, (0, 0, 16, 16))
    paste(out, st, (1, 13, 15, 16))                       # le socle (y monde 0..3)
    paste(out, wood(8), (2, 3, 14, 13))                   # corps et couvercle
    for x in (4, 5, 10, 11):                              # les sangles
        for y in range(3, 13): out.putpixel((x, y), noisy((58, 61, 70), random.Random(x * y), 3))
    for x in (4, 10):
        for y in range(3, 13): out.putpixel((x, y), (96, 100, 112))
    for x in range(2, 14):                                # le joint entre le couvercle et le corps
        out.putpixel((x, 6), (30, 22, 16))
        out.putpixel((x, 3), (88, 92, 104))               # liseré de fer en haut du couvercle
    for y in range(1, 14):                                # montants d'angle de fer
        for x in (1, 14): out.putpixel((x, y), (46, 49, 57))
    return out

def chest_top():
    r = random.Random(11); im = wood(12)
    for y in range(16):
        for x in range(16):
            if x in (1, 14) or y in (1, 14): im.putpixel((x, y), noisy((60, 63, 72), r, 3))
            if x in (7, 8): im.putpixel((x, y), noisy((60, 63, 72), r, 3))
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]: im.putpixel((x, y), (200, 156, 54))
    return im

def blanket_top():
    r = random.Random(13); im = new()
    red = (138, 32, 34); g = (206, 164, 62)
    for y in range(16):
        for x in range(16):
            c = noisy(red, r, 5)
            if (x + y) % 4 == 0: c = add(c, -10)
            im.putpixel((x, y), c)
    for i in range(1, 15):                                 # liseré doré et frise
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i), (i, 3), (i, 12)):
            im.putpixel((x, y), g if (x != i or i % 2 == 0 or y in (1, 14)) else noisy(red, r, 4))
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8), (6, 7), (9, 8), (7, 6), (8, 9), (5, 7), (10, 8), (7, 5), (8, 10)]:
        im.putpixel((x, y), g)
    return im

def blanket_side():
    r = random.Random(14); im = new(wood(5).getpixel((4, 4)))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), noisy((126, 28, 31), r, 5))
    for x in range(16):
        im.putpixel((x, 10), (206, 164, 62)); im.putpixel((x, 9), (96, 20, 24))
    return im

def sheet_top():
    r = random.Random(15); im = new()
    for y in range(16):
        for x in range(16): im.putpixel((x, y), noisy((196, 186, 164), r, 5))
    for i in range(16): im.putpixel((i, 1), (170, 160, 138)); im.putpixel((i, 14), (170, 160, 138))
    return im

def pillow_top():
    r = random.Random(16); im = new()
    for y in range(16):
        for x in range(16):
            c = noisy((226, 216, 190), r, 4)
            if x in (2, 13) or y in (3, 12): c = (196, 160, 70)
            im.putpixel((x, y), c)
    return im

def pillow_side():
    r = random.Random(17); im = new()
    for y in range(16):
        for x in range(16): im.putpixel((x, y), noisy((214, 204, 178), r, 5))
    for x in range(16): im.putpixel((x, 9), (190, 152, 66))
    return im

def furnace_front():
    st = stone(18); r = random.Random(19)
    for y in range(8, 13):
        for x in range(5, 11):
            c = noisy((16, 14, 14), r, 2)
            if x in (6, 8, 10) and y < 12: c = (62, 62, 70)
            st.putpixel((x, y), c)
    for x in range(4, 12): st.putpixel((x, 7), (28, 30, 36))
    return st

def fire():
    r = random.Random(20); im = new()
    for y in range(16):
        for x in range(16):
            t = (y - 4) / 8.0
            c = lerp((255, 214, 90), (226, 82, 18), max(0, min(1, t)))
            c = noisy(c, r, 14)
            if x % 2 == 0 and 5 <= y <= 12 and False: c = (30, 30, 34)
            im.putpixel((x, y), c)
    for x in (6, 8, 10):                                                       # grille devant le feu
        for y in range(16): im.putpixel((x, y), (50, 50, 58))
    return im

def flue():
    st = stone(21); r = random.Random(22)
    for y in range(4, 12):
        for x in range(4, 12): st.putpixel((x, y), noisy((14, 13, 13), r, 2))
    return st

def slab_side():
    r = random.Random(23); im = new()
    for y in range(16):
        for x in range(16):
            c = noisy(lerp(SL_A, SL_B, .5), r, 4)
            if y == 0: c = (206, 164, 62)
            elif y == 1: c = (140, 106, 40)
            elif y == 3: c = (30, 33, 40)
            elif y > 3: c = (46, 50, 60)
            im.putpixel((x, y), c)
    return im

def table_top():
    r = random.Random(24); im = new(MORT)
    for y in range(1, 15):
        for x in range(1, 15):
            c = noisy(lerp(SL_A, SL_B, .4), r, 3)
            if y == 1 or x == 1: c = add(c, 12)
            im.putpixel((x, y), c)
    g = (204, 162, 60)
    for k in range(3):                                                         # la grille d'ecriture 3x3
        for j in range(3):
            x0, y0 = 2 + k * 4, 2 + j * 4
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 3): im.putpixel((x, y), noisy((50, 58, 72), r, 3))
    for i in range(2, 14):
        for t in (5, 9): im.putpixel((i, t), g); im.putpixel((t, i), g)
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]: im.putpixel((x, y), g)
    return im

def table_side():
    r = random.Random(25); im = wood(26, 6)
    for x in range(16):
        im.putpixel((x, 4), (58, 61, 70)); im.putpixel((x, 8), (58, 61, 70))
    for y in range(5, 8):
        for x in (2, 13): im.putpixel((x, y), (58, 61, 70))
    for x in (7, 8):
        im.putpixel((x, 6), (206, 164, 62))
    for x in range(3, 13): im.putpixel((x, 5), add(im.getpixel((x, 5)), -16))
    return im

TEXTURES = {
    'stone': stone(1), 'stone_b': stone(31), 'carved': carved(), 'iron': iron(), 'gold': gold(), 'wood': wood(5), 'wood_dark': wood(6, 22),
    'woodend': woodend(), 'inside': inside(), 'chest_front': chest_front(), 'chest_side': chest_front(True), 'chest_top': chest_top(),
    'blanket_top': blanket_top(), 'blanket_side': blanket_side(), 'sheet_top': sheet_top(), 'pillow_top': pillow_top(),
    'pillow_side': pillow_side(), 'furnace_front': furnace_front(), 'fire': fire(), 'flue': flue(), 'slab_side': slab_side(),
    'table_top': table_top(), 'table_side': table_side(),
}
# le cote du coffre : on enleve les sangles centrales et la serrure (pas de serrure ici de toute facon)
for n, im in TEXTURES.items(): save(im, n)

# ---------------------------------------------------------------- modeles
def tex(n): return 'ringsofash:block/dwf_' + n

class M:
    def __init__(self, textures): self.el = []; self.t = {k: tex(v) for k, v in textures.items()}; self.t['particle'] = tex('stone')
    def box(self, a, b, faces, rot=None, emit=None):
        """faces : dict direction -> nom de texture (ou un nom pour toutes les faces)"""
        if isinstance(faces, str): faces = {d: faces for d in ('north', 'south', 'east', 'west', 'up', 'down')}
        f = {}
        for d, n in faces.items():
            if n is None: continue
            f[d] = {'texture': '#' + n}
            if emit: f[d]['light_emission'] = emit
        e = {'from': list(a), 'to': list(b), 'faces': f}
        if rot: e['rotation'] = rot
        self.el.append(e); return self
    def json(self, parent='minecraft:block/block'):
        return {'parent': parent, 'textures': self.t, 'elements': self.el}
    def shift(self, dz):
        out = []
        for e in self.el:
            e2 = json.loads(json.dumps(e)); e2['from'][2] += dz; e2['to'][2] += dz
            if 'rotation' in e2: e2['rotation']['origin'][2] += dz
            out.append(e2)
        return out

def allf(n, up=None, down=None):
    d = {k: n for k in ('north', 'south', 'east', 'west')}; d['up'] = up or n; d['down'] = down or n; return d

# --- coffre
def chest(open_):
    m = M({'stone': 'stone', 'cf': 'chest_front', 'cs': 'chest_side', 'ct': 'chest_top', 'iron': 'iron', 'gold': 'gold',
           'ins': 'inside', 'carved': 'carved', 'wd': 'wood_dark'})
    m.box((1, 0, 1), (15, 3, 15), 'stone')
    m.box((2, 3, 2), (14, 10, 14), {'north': 'cf', 'south': 'cs', 'east': 'cs', 'west': 'cs', 'up': 'ins', 'down': 'wd'})
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.box((x, 3, z), (x + 2, 10, z + 2), 'iron')
    m.box((6, 5.5, 0.6), (10, 9.5, 1), {'north': 'gold', 'east': 'gold', 'west': 'gold', 'up': 'gold', 'down': 'gold'})
    rot = {'origin': [8, 10, 15], 'axis': 'x', 'angle': 45} if open_ else None
    m.box((1, 10, 1), (15, 13, 15), {'north': 'cf', 'south': 'cs', 'east': 'cs', 'west': 'cs', 'up': 'ct', 'down': 'wd'}, rot)
    m.box((3, 13, 3), (13, 14.5, 13), {'north': 'stone', 'south': 'stone', 'east': 'stone', 'west': 'stone', 'up': 'carved', 'down': 'stone'}, rot)
    return m
wj(A + '/models/block/dwarven_chest.json', chest(False).json())
wj(A + '/models/block/dwarven_chest_open.json', chest(True).json())
wj(A + '/models/item/dwarven_chest.json', {'parent': 'ringsofash:block/dwarven_chest'})
wj(A + '/blockstates/dwarven_chest.json', {'variants': {
    'facing=%s,open=%s' % (f, o): {'model': 'ringsofash:block/dwarven_chest' + ('_open' if o == 'true' else ''), 'y': y}
    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)) for o in ('false', 'true')}})

# --- lit
def bed(head, jl=False, jr=False):
    """jl / jr : un autre lit est accole a gauche / a droite (le lit regarde le nord dans le modele : gauche = ouest, droite = est)"""
    m = M({'stone': 'stone', 'carved': 'carved', 'wood': 'wood', 'wd': 'wood_dark', 'iron': 'iron', 'gold': 'gold', 'bt': 'blanket_top',
           'bs': 'blanket_side', 'sheet': 'sheet_top', 'pt': 'pillow_top', 'ps': 'pillow_side', 'we': 'woodend'})
    def sides(n, d=None):                       # faces d'un element qui touche le voisin : pas de face du cote accole
        f = dict(d) if d else {k: n for k in ('north', 'south', 'east', 'west', 'up', 'down')}
        if jl: f['west'] = None
        if jr: f['east'] = None
        return f
    for (x, z) in ((0, 0), (14, 0), (0, 14), (14, 14)):
        if (x == 0 and jl) or (x == 14 and jr): continue
        m.box((x, 0, z), (x + 2, 3, z + 2), allf('stone'))
    m.box((0, 3, 0), (16, 6, 16), sides(None, {'north': 'wood', 'south': 'wood', 'east': 'wood', 'west': 'wood', 'up': 'wd', 'down': 'wd'}))
    x0, x1 = (0 if jl else 1), (16 if jr else 15)
    if head:
        m.box((x0, 6, 2), (x1, 9, 16), sides(None, {'north': 'bs', 'south': 'bs', 'east': 'bs', 'west': 'bs', 'up': 'sheet', 'down': 'wd'}))
        m.box((2, 9, 3), (14, 11, 9), {'north': 'ps', 'south': 'ps', 'east': 'ps', 'west': 'ps', 'up': 'pt', 'down': 'ps'})
        m.box((0, 3, 0), (16, 15, 2), sides(None, {'north': 'carved', 'south': 'carved', 'east': 'stone', 'west': 'stone', 'up': 'stone', 'down': 'stone'}))
        m.box((0, 15, 0), (16, 16, 2), sides('gold'))
        for x in (2, 13): m.box((x, 12, 2), (x + 1, 13, 2.6), {'south': 'gold'})
    else:
        m.box((x0, 6, 0), (x1, 9.5, 15), sides(None, {'north': 'bs', 'south': 'bs', 'east': 'bs', 'west': 'bs', 'up': 'bt', 'down': 'wd'}))
        m.box((0, 3, 14), (16, 10, 16), sides(None, {'north': 'stone', 'south': 'carved', 'east': 'stone', 'west': 'stone', 'up': 'stone', 'down': 'stone'}))
        m.box((0, 10, 14), (16, 11, 16), sides('gold'))
    return m
for part in ('head', 'foot'):
    for jl in (0, 1):
        for jr in (0, 1):
            wj(A + '/models/block/dwarven_bed_%s_%d%d.json' % (part, jl, jr), bed(part == 'head', bool(jl), bool(jr)).json())
item = bed(True); item.el = item.shift(-16 + 8)                  # la tete devant (nord), les pieds derriere
foot = bed(False); item.el += foot.shift(0 + 8)
wj(A + '/models/block/dwarven_bed_item.json', item.json())
wj(A + '/models/item/dwarven_bed.json', {'parent': 'ringsofash:block/dwarven_bed_item', 'display': {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.42, 0.42, 0.42]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 90, 0], 'scale': [0.4, 0.4, 0.4]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.25, 0.25, 0.25]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.3, 0.3, 0.3]}}})
bv = {}
for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for part in ('head', 'foot'):
        for occ in ('false', 'true'):
            for jl in (0, 1):
                for jr in (0, 1):
                    bv['facing=%s,join_left=%s,join_right=%s,occupied=%s,part=%s' % (f, str(bool(jl)).lower(), str(bool(jr)).lower(), occ, part)] = \
                        {'model': 'ringsofash:block/dwarven_bed_%s_%d%d' % (part, jl, jr), 'y': y}
wj(A + '/blockstates/dwarven_bed.json', {'variants': bv})

# --- petit four
def furnace(lit):
    m = M({'stone': 'stone', 'sb': 'stone_b', 'front': 'furnace_front', 'carved': 'carved', 'iron': 'iron', 'gold': 'gold', 'flue': 'flue',
           'fire': 'fire', 'slab': 'slab_side'})
    m.box((2, 0, 2), (14, 3, 14), allf('sb'))
    m.box((3, 3, 3), (13, 10, 13), {'north': 'front', 'south': 'stone', 'east': 'stone', 'west': 'stone', 'up': 'stone', 'down': 'stone'})
    m.box((4, 8, 2.4), (12, 9.2, 3), allf('iron'))
    m.box((4, 3, 2.4), (5.2, 8, 3), allf('iron'))
    m.box((10.8, 3, 2.4), (12, 8, 3), allf('iron'))
    if lit: m.box((5.2, 3, 2.95), (10.8, 8, 3.05), {'north': 'fire'}, emit=15)
    m.box((2.6, 5, 2.6), (3, 6.5, 13.4), {'west': 'iron', 'up': 'iron', 'down': 'iron', 'north': 'iron', 'south': 'iron'})
    m.box((13, 5, 2.6), (13.4, 6.5, 13.4), {'east': 'iron', 'up': 'iron', 'down': 'iron', 'north': 'iron', 'south': 'iron'})
    m.box((3, 5, 13), (13, 6.5, 13.4), {'south': 'iron', 'up': 'iron', 'down': 'iron'})
    m.box((2, 10, 2), (14, 12, 14), {'north': 'slab', 'south': 'slab', 'east': 'slab', 'west': 'slab', 'up': 'carved', 'down': 'stone'})
    m.box((6, 12, 6), (10, 15, 10), allf('stone', up='flue'))
    m.box((5, 15, 5), (11, 16, 11), {'north': 'gold', 'south': 'gold', 'east': 'gold', 'west': 'gold', 'up': 'flue', 'down': 'gold'})
    m.box((7.4, 6, 2.2), (8.6, 7.6, 2.4), {'north': 'gold'})
    return m
wj(A + '/models/block/dwarven_furnace.json', furnace(False).json())
wj(A + '/models/block/dwarven_furnace_on.json', furnace(True).json())
wj(A + '/models/item/dwarven_furnace.json', {'parent': 'ringsofash:block/dwarven_furnace'})
wj(A + '/blockstates/dwarven_furnace.json', {'variants': {
    'facing=%s,lit=%s' % (f, l): {'model': 'ringsofash:block/dwarven_furnace' + ('_on' if l == 'true' else ''), 'y': y}
    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)) for l in ('false', 'true')}})

# --- table de craft
def table():
    m = M({'stone': 'stone', 'top': 'table_top', 'slab': 'slab_side', 'ts': 'table_side', 'wd': 'wood_dark', 'wood': 'wood', 'iron': 'iron',
           'carved': 'carved', 'gold': 'gold'})
    m.box((0, 12, 0), (16, 16, 16), {'north': 'slab', 'south': 'slab', 'east': 'slab', 'west': 'slab', 'up': 'top', 'down': 'wd'})
    m.box((2, 8, 2), (14, 12, 14), {'north': 'ts', 'south': 'ts', 'east': 'ts', 'west': 'ts', 'down': 'wd'})
    for (x, z) in ((1, 1), (12, 1), (1, 12), (12, 12)):
        m.box((x, 0, z), (x + 3, 12, z + 3), allf('stone'))
    m.box((4, 2, 4), (12, 3, 12), {'up': 'wood', 'down': 'wd', 'north': 'wood', 'south': 'wood', 'east': 'wood', 'west': 'wood'})
    m.box((2, 16, 12), (9, 17, 13), allf('wood'))
    m.box((9, 16, 11), (11, 18, 14), allf('iron'))
    m.box((10, 16, 3), (15, 16.8, 4), allf('iron'))
    m.box((11, 16, 5), (14, 16.8, 5.8), allf('gold'))
    return m
wj(A + '/models/block/dwarven_crafting_table.json', table().json())
wj(A + '/models/item/dwarven_crafting_table.json', {'parent': 'ringsofash:block/dwarven_crafting_table'})
wj(A + '/blockstates/dwarven_crafting_table.json', {'variants': {'': {'model': 'ringsofash:block/dwarven_crafting_table'}}})

# ---------------------------------------------------------------- donnees : recettes, loot, tags, traductions
NAMES = {'dwarven_chest': ('Coffre nain', 'Dwarven Chest'), 'dwarven_bed': ('Lit nain', 'Dwarven Bed'),
         'dwarven_furnace': ('Petit four nain', 'Small Dwarven Furnace'), 'dwarven_crafting_table': ('Table de craft naine', 'Dwarven Crafting Table')}
B, P, I = {'item': 'ringsofash:dwarven_bricks'}, {'item': 'ringsofash:ancient_planks'}, {'item': 'minecraft:iron_ingot'}
RECIPES = {
    'dwarven_chest': (['PPP', 'PIP', 'BBB'], {'P': P, 'I': I, 'B': B}),
    'dwarven_bed': (['RRR', 'PPP', 'B B'], {'R': {'item': 'minecraft:red_wool'}, 'P': P, 'B': B}),
    'dwarven_furnace': (['BIB', 'BFB', 'BBB'], {'B': B, 'I': I, 'F': {'item': 'minecraft:furnace'}}),
    'dwarven_crafting_table': (['BB', 'PP', 'PP'], {'B': B, 'P': P}),
}
for n, (pat, key) in RECIPES.items():
    wj(D + '/recipe/%s.json' % n, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pat, 'key': key,
                                   'result': {'id': 'ringsofash:' + n, 'count': 1}})
    entry = {'type': 'minecraft:item', 'name': 'ringsofash:' + n}
    pool = {'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}
    if n == 'dwarven_bed':
        pool['conditions'].insert(0, {'condition': 'minecraft:block_state_property', 'block': 'ringsofash:dwarven_bed', 'properties': {'part': 'foot'}})
    wj(D + '/loot_table/blocks/%s.json' % n, {'type': 'minecraft:block', 'random_sequence': 'ringsofash:blocks/' + n, 'pools': [pool]})

pk = ROOT + '/data/minecraft/tags/block/mineable/pickaxe.json'
d = json.load(open(pk, encoding='utf-8'))
for n in ('dwarven_chest', 'dwarven_furnace', 'dwarven_crafting_table'):
    if 'ringsofash:' + n not in d['values']: d['values'].append('ringsofash:' + n)
wj(pk, d)
wj(ROOT + '/data/minecraft/tags/block/beds.json', {'replace': False, 'values': ['ringsofash:dwarven_bed']})
wj(ROOT + '/data/minecraft/tags/item/beds.json', {'replace': False, 'values': ['ringsofash:dwarven_bed']})

for fn, idx in (('fr_fr.json', 0), ('en_us.json', 1)):
    path = A + '/lang/' + fn
    lang = json.load(open(path, encoding='utf-8'))
    for n, nm in NAMES.items(): lang['block.ringsofash.' + n] = nm[idx]
    lang['container.ringsofash.dwarven_chest'] = NAMES['dwarven_chest'][idx]
    lang['container.ringsofash.dwarven_furnace'] = NAMES['dwarven_furnace'][idx]
    lang['container.ringsofash.dwarven_crafting'] = NAMES['dwarven_crafting_table'][idx]
    wj(path, lang)
print('mobilier nain : ok')
