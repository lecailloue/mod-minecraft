"""Peaux humanoides (disposition de skin joueur 64x64, dans une image 128x64 : la moitie droite sert aux oreilles, defenses...)."""
import random
from PIL import Image
from tex import rgb, shade, mix
from genlib import A, save

def _c(c): return rgb(c) if isinstance(c, str) else c

class Skin:
    def __init__(self, seed=0, h=64):
        self.im = Image.new('RGBA', (128, h), (0, 0, 0, 0))
        self.rnd = random.Random(seed)
    def px(self, x, y, c, a=255):
        if 0 <= x < 128 and 0 <= y < self.im.height: self.im.putpixel((x, y), _c(c)[:3] + (a,))
    def rect(self, x, y, w, h, c, noise=0.08, grad=0.0, a=255):
        c = _c(c)
        for yy in range(h):
            for xx in range(w):
                f = 1 + (self.rnd.random() - 0.5) * 2 * noise - grad * yy / max(1, h)
                self.px(x + xx, y + yy, shade(c, f), a)
    def box(self, u, v, w, h, d, c, **kw):
        """remplit les 6 faces d'une boite (u,v = texOffs)"""
        self.rect(u + d, v, w, d, shade(_c(c), 1.08), **kw); self.rect(u + d + w, v, w, d, shade(_c(c), 0.75), **kw)
        self.rect(u, v + d, d, h, shade(_c(c), 0.92), **kw); self.rect(u + d, v + d, w, h, c, **kw)
        self.rect(u + d + w, v + d, d, h, shade(_c(c), 0.92), **kw); self.rect(u + d + w + d, v + d, w, h, shade(_c(c), 0.86), **kw)
    def art(self, x, y, rows, pal):
        for yy, row in enumerate(rows):
            for xx, ch in enumerate(row):
                if ch in '. ': continue
                self.px(x + xx, y + yy, pal[ch])

FACES = {
    'human': ['........', '........', '........', '.bb..bb.', '.we..ew.', '...nn...', '..mmmm..', '........'],
    'elf':   ['........', '........', '........', '.b....b.', '.we..ew.', '...n....', '..mmm...', '........'],
    'dwarf': ['........', '........', '.bb..bb.', '.we..ew.', '..nnnn..', '..nnnn..', '........', '........'],
    'half':  ['........', '........', '........', '.b....b.', '.we..ew.', '...nn...', '.rmmmmr.', '........'],
    'orc':   ['........', '........', 'dddddddd', '.ee..ee.', '.dd..dd.', '..nnnn..', '.tmmmmt.', '.t....t.'],
    'goblin': ['........', '........', '........', 'eee..eee', 'eke..eke', '...nn...', '.mtmmtm.', '........'],
    'ghost': ['........', '........', 'dd....dd', 'dee..eed', 'dd....dd', '...dd...', '..dddd..', '..d..d..'],
    'void':  ['kkkkkkkk', 'kkkkkkkk', 'kkkkkkkk', 'kkkkkkkk', 'kkkkkkkk', 'kkkkkkkk', 'kkkkkkkk', 'kkkkkkkk'],
    'lord':  ['........', '........', 'kk....kk', 'kee..eek', 'kk....kk', '.k.kk.k.', '.k.kk.k.', '.k.kk.k.'],
}

def skin(name, race='human', skin='#c69a7a', hair='#4a3020', hair_style='short', eyes='#3a2a1a', top='#7a5a3a', trim=None, legs='#4a3a2a',
         boots='#2a2018', belt='#3a2a1a', beard=None, beard_len=0, sleeves=True, armor=None, hood=None, seed=0, face=None, ears=None,
         tusks=False, gloves=None, alpha=255, emblem=None, crown=None, pauldrons=None, skirt=False, xg=None):
    big = race in ('elf', 'dwarf')
    s = Skin(seed, 128 if big else 64)
    sk = _c(skin); hr = _c(hair); tp = _c(top); tr = _c(trim) if trim else shade(tp, 0.7); lg = _c(legs); bt = _c(boots)
    a = alpha
    # --- tete
    s.box(0, 0, 8, 8, 8, sk, a=a)
    fx, fy = 8, 8
    if hair_style != 'bald':
        s.rect(8, 0, 8, 8, hr, a=a)                                  # dessus
        rows = {'short': 3, 'long': 8, 'mid': 5}.get(hair_style, 3)
        for (x, w) in ((0, 8), (16, 8), (24, 8)):                    # cotes + arriere
            s.rect(x, 8, w, rows, shade(hr, 0.92), a=a)
        s.rect(8, 8, 8, 2 if hair_style != 'long' else 2, hr, a=a)   # frange
        if hair_style == 'long':
            s.rect(8, 10, 1, 6, hr, a=a); s.rect(15, 10, 1, 6, hr, a=a)
    f = FACES[face or race]
    pal = {'b': shade(hr, 0.8) if hair_style != 'bald' else shade(sk, 0.7), 'w': (240, 240, 235), 'e': _c(eyes), 'n': shade(sk, 0.86),
           'm': shade(sk, 0.62), 'r': mix(sk, (220, 110, 100), 0.4), 'd': shade(sk, 0.6), 't': (235, 228, 200), 'k': (8, 6, 8)}
    s.art(fx, fy, f, pal)
    if beard:
        bd = _c(beard)
        s.rect(8, 13, 8, 3, bd, a=a); s.rect(9, 12, 1, 1, bd, a=a); s.rect(14, 12, 1, 1, bd, a=a)
        s.rect(0, 13, 8, 3, shade(bd, 0.9), a=a); s.rect(16, 13, 8, 3, shade(bd, 0.9), a=a)
        s.px(11, 14, shade(sk, 0.6)); s.px(12, 14, shade(sk, 0.6))
        s.rect(16, 0, 8, 8, bd, a=a)                                 # dessous de la tete (menton)
    # --- corps
    s.box(16, 16, 8, 12, 4, tp, grad=0.12, a=a)
    if armor == 'chain':
        for (x, y, w, h) in ((20, 20, 8, 12), (32, 20, 8, 12), (16, 20, 4, 12), (28, 20, 4, 12)):
            for yy in range(h):
                for xx in range(w):
                    if (xx + yy) % 2 == 0: s.px(x + xx, y + yy, shade(_c('#9aa0a8'), 0.8 + 0.3 * s.rnd.random()), a)
    if armor == 'plate':
        pc = _c('#8a8f98') if trim is None else tr
        s.rect(20, 20, 8, 7, pc, a=a); s.rect(32, 20, 8, 7, shade(pc, 0.85), a=a); s.rect(21, 21, 6, 1, shade(pc, 1.3), a=a)
    s.rect(20, 29, 8, 1, _c(belt), a=a); s.rect(32, 29, 8, 1, _c(belt), a=a); s.rect(16, 29, 4, 1, _c(belt), a=a); s.rect(28, 29, 4, 1, _c(belt), a=a)
    s.px(23, 29, (200, 170, 70)); s.px(24, 29, (200, 170, 70))
    s.rect(20, 20, 8, 1, tr, a=a); s.rect(23, 21, 2, 3, tr, a=a)     # col
    if emblem:
        s.art(21, 22, emblem[0], emblem[1])
    if beard and beard_len:
        s.rect(21, 20, 6, beard_len, _c(beard)); s.rect(22, 20 + beard_len, 4, 1, shade(_c(beard), 0.85))
    # --- bras (droit 40,16 ; gauche 32,48)
    for (u, v) in ((40, 16), (32, 48)):
        s.box(u, v, 4, 12, 4, tp if sleeves else sk, grad=0.1, a=a)
        hand = _c(gloves) if gloves else sk
        for x in (u, u + 4, u + 8, u + 12):
            s.rect(x, v + 4 + 9, 4, 3, hand, a=a)
        if sleeves:
            for x in (u, u + 4, u + 8, u + 12): s.rect(x, v + 4 + 8, 4, 1, tr, a=a)
        if pauldrons:
            pc = _c(pauldrons)
            s.rect(u + 4, v, 4, 4, pc, a=a)
            for x in (u, u + 4, u + 8, u + 12): s.rect(x, v + 4, 4, 3, shade(pc, 0.9), a=a)
    # --- jambes (droite 0,16 ; gauche 16,48)
    for (u, v) in ((0, 16), (16, 48)):
        s.box(u, v, 4, 12, 4, lg, grad=0.1, a=a)
        for x in (u, u + 4, u + 8, u + 12): s.rect(x, v + 4 + 9, 4, 3, bt, a=a)
        s.rect(u + 8, v, 4, 4, shade(bt, 0.7), a=a)
        if skirt:
            for x in (u, u + 4, u + 8, u + 12): s.rect(x, v + 4, 4, 5, shade(tp, 0.9), a=a)
    # --- couche "chapeau" : capuche, casque, couronne
    if hood:
        hc = _c(hood)
        s.box(32, 0, 8, 8, 8, hc, a=a)
        for yy in range(2, 8):
            for xx in range(1, 7): s.px(40 + xx, 8 + yy, (0, 0, 0), 0)     # ouverture du visage
        s.rect(41, 9, 6, 1, shade(hc, 0.6), a=a)
    if hair_style == 'helmet':
        mc = _c(hair)
        s.box(32, 0, 8, 8, 8, mc, a=a)
        for yy in range(3, 8):
            for xx in range(0, 8):
                if xx not in (3, 4) or yy > 5: s.px(40 + xx, 8 + yy, (0, 0, 0), 0)
        s.rect(40, 10, 8, 1, shade(mc, 1.25), a=a)
    if crown:
        cc = _c(crown)
        for x in range(32, 64):
            if 40 <= x < 48 or x < 40 or x >= 48:
                pass
        for (x0, w) in ((32, 8), (40, 8), (48, 8), (56, 8)):
            for xx in range(w):
                s.px(x0 + xx, 9, cc); 
                if xx % 2 == 0: s.px(x0 + xx, 8, cc)
        s.px(43, 9, (220, 40, 40)); s.px(44, 9, (220, 40, 40))
    # --- extras (moitie droite)
    if big:
        import people3d
        people3d.paint(s, race, name, dict(skin=skin, hair=hair, hair_style=hair_style, top=top, trim=trim, beard=beard, beard_len=beard_len, belt=belt, armor=armor, pauldrons=pauldrons, crown=crown, skirt=skirt, xg=xg))
        save(s.im, f'{A}/textures/entity/people/{name}.png')
        return s
    s.box(64, 0, 1, 5, 2, sk, a=a)        # oreille d'elfe      (boite 1x5x2 -> 6x7)
    s.box(64, 8, 4, 3, 1, sk, a=a)        # oreille de gobelin  (boite 4x3x1 -> 10x4)
    s.box(64, 16, 1, 3, 2, sk, a=a)       # oreille d'orque     (boite 1x3x2 -> 6x5)
    s.box(76, 0, 1, 2, 1, (235, 228, 200), a=a)   # defense       (boite 1x2x1 -> 4x3)
    s.box(80, 0, 6, 6, 2, _c(beard) if beard else hr, a=a)   # barbe en volume (boite 6x6x2 -> 16x8)
    save(s.im, f'{A}/textures/entity/people/{name}.png')
    return s
