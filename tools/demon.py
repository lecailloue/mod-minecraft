"""Fleau de Braise : colosse de roche volcanique fissuree, criniere et fouet de feu, ailes de chauve-souris.
Tout est en boites (style Minecraft / Blockbench). Les flammes sont des pointes etagees posees sur le corps
(pieces 'flame_*', animees par GeoModel) et le fouet une chaine de segments ('whip1'..'whip9')."""
import random
from PIL import Image
from geo import Model
from tex import rgb, shade
from genlib import A, save

PAL = {'c': '#ff7a18', 'y': '#ffe070', 'r': '#e83a12', 'k': '#0b0807', 'd': '#1d1715', 'b': '#33231a', 'w': '#e2d6b8'}
GLOW = 'cyr'
ROCK, BROWN, PLATE, HORN, TIP, BONE = '#1d1715', '#2b1e17', '#3a281c', '#15100e', '#5a4636', '#d8ccb0'
WBONE, MEMB = '#191311', '#33201a'
FIRE = ['#ffdc50', '#ffb22a', '#ff8418', '#f05a16', '#d63a14', '#a82410']       # du coeur (jaune) a la pointe (rouge sombre)

def crack_art(w, h, seed, dens=1.0):
    """fissures de lave : marches aleatoires, coeur jaune, bords orange/rouge"""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    if w * h < 12: return [''.join(r) for r in g]
    for _ in range(max(1, round(w * h * dens / 85))):
        x, y = rnd.randrange(w), rnd.randrange(h)
        vert = h >= 4 and rnd.random() < 0.75
        d = rnd.choice((-1, 0, 1))
        for i in range(rnd.randint(4, max(5, int((h if vert else w) * 0.75)))):
            if 0 <= x < w and 0 <= y < h:
                q = rnd.random()
                g[y][x] = 'y' if q < 0.3 else 'c' if q < 0.8 else 'r'
            if rnd.random() < 0.38: d = rnd.choice((-1, 0, 1))
            if vert: y += 1; x += d
            else: x += 1; y += d
            if rnd.random() < 0.12 and 0 <= x + 1 < w and 0 <= y < h: g[y][x + 1] = 'r'
    return [''.join(r) for r in g]

def island_art(w, h, seed, cover=0.4):
    """membre embrase : ilots de roche sombre sur fond de feu"""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(max(1, round(w * h * cover / 5))):
        x, y = rnd.randrange(w), rnd.randrange(h)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
            if rnd.random() < 0.75 and 0 <= x + dx < w and 0 <= y + dy < h: g[y + dy][x + dx] = 'd' if rnd.random() < 0.7 else 'b'
    return [''.join(r) for r in g]

class Builder:
    def __init__(self):
        self.m = Model('demon', 256, 256, 11)
        self.seed = 100
        self.shift = (0, 0, 0)

    def part(self, name, pivot, parent=None, rot=(0, 0, 0), sx=1):
        """sx = -1 : piece symetrique du cote droit (x negatif)"""
        if sx < 0: pivot, rot = (-pivot[0], pivot[1], pivot[2]), (rot[0], -rot[1], -rot[2])
        pivot = tuple(a + b for a, b in zip(pivot, self.shift))
        p = self.m.part(name, pivot, parent, rot); p.sx = sx
        return p

    def box(self, p, o, s, color, **kw):
        o = tuple(a + b for a, b in zip(o, self.shift))
        if p.sx < 0: o = (-o[0] - s[0], o[1], o[2])
        p.box(o, s, color, mirror=p.sx > 0 and getattr(p, 'side', False), pal=PAL, **kw)

    def rock(self, p, o, s, color=ROCK, dens=1.0, art=None, faces='front back left right top'):
        w, h, d = s
        self.seed += 1
        dims = {'front': (w, h), 'back': (w, h), 'left': (d, h), 'right': (d, h), 'top': (w, d), 'bottom': (w, d)}
        a = {f: crack_art(*dims[f], self.seed * 7 + i, dens) for i, f in enumerate(faces.split())} if dens > 0 else {}
        a.update(art or {})
        self.box(p, o, s, color, art=a, noise=0.24)

    def fire(self, p, o, s, lo=1, hi=3, **kw):
        self.box(p, o, s, FIRE[lo], grad=FIRE[hi], glow=True, noise=0.16, **kw)

    def spike(self, p, x, y, z, w, h, d, lean=(0, 0), hot=0):
        """flamme pointue : boites empilees de plus en plus etroites, du jaune a la base au rouge a la pointe"""
        n = max(2, min(4, (min(w, d) + 1) // 2 + 1))
        share = [0.30, 0.27, 0.23, 0.20][:n]; tot = sum(share)
        for i in range(n):
            hi = max(1, round(h * share[i] / tot))
            wi, di = max(1, w - 2 * i), max(1, d - 2 * i)
            ox, oz = x + (w - wi) // 2 + round(lean[0] * i), z + (d - di) // 2 + round(lean[1] * i)
            c = min(len(FIRE) - 2, hot + round(i * 4 / n))
            self.fire(p, (ox, y, oz), (wi, hi, di), c, c + 1)
            y += hi

def build_model():
    B = Builder(); m = B.m

    # ------------------------------------------------------------ tronc
    body = B.part('body', (0, 38, 0))
    B.rock(body, (-11, 34, -6), (22, 8, 12), BROWN, 0.9)                       # bassin
    B.rock(body, (-10, 42, -6), (20, 8, 11), ROCK, 2.2)                        # ventre tres fissure
    B.rock(body, (-16, 50, -9), (32, 22, 17), ROCK, 1.5)                       # poitrine
    B.rock(body, (-15, 60, -11), (13, 10, 2), PLATE, 0.5, faces='front')        # pectoraux en plaques
    B.rock(body, (2, 60, -11), (13, 10, 2), PLATE, 0.5, faces='front')
    B.fire(body, (-2, 53, -10), (4, 17, 1), 0, 2)                              # fournaise entre les plaques
    B.rock(body, (-8, 43, -7), (7, 6, 1), BROWN, 0, faces='front'); B.rock(body, (1, 43, -7), (7, 6, 1), BROWN, 0, faces='front')
    B.rock(body, (-11, 72, -5), (22, 5, 12), BROWN, 1.0)                       # trapezes
    B.rock(body, (-5, 74, -8), (10, 8, 8), ROCK, 1.2)                          # cou
    B.rock(body, (-13, 52, 8), (26, 16, 2), PLATE, 1.2, faces='back')           # plaque dorsale

    f = B.part('flame_chest', (0, 70, -10), 'body'); B.spike(f, -2, 69, -11, 4, 7, 2)
    for sx in (1, -1):
        f = B.part('flame_hip_' + 'lr'[sx < 0], (11, 40, 0), 'body', (0, 0, 28), sx); B.spike(f, 10, 39, -3, 3, 9, 5, hot=1)

    # criniere de feu : nuque puis echine
    f = B.part('flame_mane2', (0, 77, 4), 'body', (-14, 0, 0))
    B.spike(f, -6, 76, 0, 12, 20, 8, (0, 0.6)); B.spike(f, -9, 75, 2, 4, 9, 4, hot=1); B.spike(f, 5, 75, 2, 4, 9, 4, hot=1)
    f = B.part('flame_spine1', (0, 66, 9), 'body', (-42, 0, 0)); B.spike(f, -4, 64, 6, 8, 17, 6)
    f = B.part('flame_spine2', (0, 55, 9), 'body', (-48, 0, 0)); B.spike(f, -3, 53, 7, 6, 14, 5, hot=1)
    f = B.part('flame_spine3', (0, 45, 6), 'body', (-55, 0, 0)); B.spike(f, -3, 43, 4, 6, 11, 4, hot=1)

    # ------------------------------------------------------------ tete
    B.shift = (0, 2, 0)
    head = B.part('head', (0, 79, -4), 'body')
    face = ['............', '............', '............', '............', '.yyc....cyy.', '..yyy..yyy..', '............', '.....kk.....', '..c......c..']
    B.rock(head, (-6, 79, -13), (12, 9, 12), ROCK, 1.3, art={'front': face})
    B.box(head, (-7, 84, -14), (14, 2, 3), HORN, noise=0.2)                    # arcade
    B.rock(head, (-5, 76, -15), (10, 3, 13), BROWN, 0.8, faces='left right top')   # machoire superieure
    B.fire(head, (-4, 71, -14), (8, 5, 11), 0, 2)                              # gueule pleine de feu
    for o, s in (((-5, 73, -15), (1, 3, 1)), ((4, 73, -15), (1, 3, 1)), ((-2, 74, -15), (1, 2, 1)), ((1, 74, -15), (1, 2, 1)),
                 ((-5, 74, -12), (1, 2, 1)), ((4, 74, -12), (1, 2, 1)), ((-5, 74, -8), (1, 2, 1)), ((4, 74, -8), (1, 2, 1))):
        B.box(head, o, s, BONE)
    jaw = B.part('jaw', (0, 72, -3), 'head')
    B.rock(jaw, (-5, 68, -15), (10, 3, 12), BROWN, 0.8, faces='front left right')
    B.box(jaw, (-3, 66, -14), (6, 2, 5), ROCK, noise=0.2)
    for o, s in (((-4, 71, -15), (1, 2, 1)), ((3, 71, -15), (1, 2, 1)), ((0, 71, -15), (1, 1, 1)), ((-5, 71, -10), (1, 2, 1)), ((4, 71, -10), (1, 2, 1))):
        B.box(jaw, o, s, BONE)
    # cornes de belier enroulees autour de la tete
    for sx in (1, -1):
        head.sx = sx
        for o, s, c in (((6, 83, -9), (5, 5, 5), '#241c18'), ((10, 82, -6), (5, 6, 10), '#2c221d'), ((10, 76, 1), (5, 7, 5), '#362a23'),
                        ((10, 75, -11), (5, 5, 14), '#42342b'), ((11, 79, -15), (4, 8, 4), '#584638'), ((12, 87, -16), (2, 5, 2), '#9a846c')):
            B.box(head, o, s, c, noise=0.2)
    head.sx = 1
    f = B.part('flame_mane1', (0, 88, -6), 'head', (-10, 0, 0))
    B.spike(f, -4, 87, -11, 8, 15, 8, (0, 0.7)); B.spike(f, -2, 87, -13, 4, 6, 2, hot=1); B.spike(f, -3, 86, -3, 6, 10, 3, (0, 0.5), hot=1)

    B.shift = (0, 0, 0)

    # ------------------------------------------------------------ bras
    for sx in (1, -1):
        n = 'lr'[sx < 0]
        arm = B.part('arm_' + n, (17, 70, 0), 'body', (0, 0, -6), sx); arm.side = True
        B.rock(arm, (15, 63, -7), (13, 13, 14), PLATE, 1.2)                    # epaule-rocher
        B.rock(arm, (17, 48, -5), (10, 16, 10), ROCK, 1.6)                     # bras
        if sx > 0:
            # avant-bras gauche presque entierement embrase
            for o, s in (((16, 30, -6), (12, 18, 12)), ((16, 22, -7), (12, 8, 13))):
                B.seed += 1
                w, h, d = s
                B.fire(arm, o, s, 3, 1, art={'front': island_art(w, h, B.seed), 'back': island_art(w, h, B.seed + 50), 'left': island_art(d, h, B.seed + 90),
                                              'right': island_art(d, h, B.seed + 130), 'bottom': island_art(w, d, B.seed + 170, 0.6)})
            B.fire(arm, (15, 45, -7), (14, 3, 14), 1, 2)                       # couronne de flammes au coude
            f = B.part('flame_fore_l', (22, 46, 0), 'arm_l', sx=sx)
            B.spike(f, 26, 47, -5, 4, 12, 4, hot=1); B.spike(f, 15, 47, 4, 4, 10, 4, hot=1); B.spike(f, 16, 47, -8, 3, 7, 3, hot=1); B.spike(f, 25, 47, 4, 4, 8, 4, hot=2)
            f = B.part('flame_fist_l', (28, 34, 0), 'arm_l', (0, 0, 18), sx); B.spike(f, 27, 30, -3, 3, 11, 6, hot=1)
        else:
            B.rock(arm, (16, 30, -6), (12, 18, 12), BROWN, 2.0)                # avant-bras
            B.rock(arm, (16, 22, -7), (12, 8, 13), ROCK, 1.2)                  # poing (tient le fouet)
            f = B.part('flame_fore_r', (28, 40, 0), 'arm_r', (0, 0, 20), sx); B.spike(f, 27, 37, -3, 3, 11, 5, hot=1)
        f = B.part('flame_sh_' + n, (22, 75, 0), 'arm_' + n, (0, 0, 16), sx)
        B.spike(f, 19, 75, -4, 7, 12, 7); B.spike(f, 25, 74, 2, 4, 8, 4, hot=1); B.spike(f, 17, 75, 3, 3, 6, 3, hot=1)
        f = B.part('flame_arm_' + n, (27, 56, 0), 'arm_' + n, (0, 0, 24), sx); B.spike(f, 26, 53, -3, 3, 10, 5, hot=1)

    # ------------------------------------------------------------ jambes
    for sx in (1, -1):
        n = 'lr'[sx < 0]
        leg = B.part('leg_' + n, (6, 36, 0), None, sx=sx); leg.side = True
        B.rock(leg, (1, 18, -6), (11, 18, 12), ROCK, 1.5)                      # cuisse
        B.rock(leg, (2, 15, -8), (9, 6, 3), PLATE, 0.4, faces='front')          # genou
        B.rock(leg, (2, 4, -4), (9, 14, 10), BROWN, 2.0)                       # tibia
        B.rock(leg, (1, 0, -8), (11, 4, 14), ROCK, 1.4)                        # sabot
        B.box(leg, (2, 0, -10), (3, 2, 2), HORN); B.box(leg, (8, 0, -10), (3, 2, 2), HORN)
        f = B.part('flame_leg_' + n, (12, 30, 0), 'leg_' + n, (0, 0, 22), sx); B.spike(f, 11, 25, -3, 3, 11, 6, hot=1)
        f = B.part('flame_calf_' + n, (6, 12, 6), 'leg_' + n, (-28, 0, 0), sx); B.spike(f, 4, 9, 5, 5, 11, 3, hot=1)

    # ------------------------------------------------------------ ailes (bras + doigts + membranes festonnees)
    for sx in (1, -1):
        n = 'lr'[sx < 0]
        w1 = B.part('wing_' + n, (5, 72, 8), 'body', (0, -22, -42), sx); w1.side = True
        B.box(w1, (5, 70, 8), (34, 4, 3), WBONE, noise=0.2)
        for o, s in (((7, 42, 9), (11, 28, 1)), ((18, 36, 9), (11, 34, 1)), ((29, 32, 9), (10, 38, 1))):
            B.rock(w1, o, s, MEMB, 0.5, faces='front back')
        f = B.part('flame_wing_' + n, (20, 74, 9), 'wing_' + n, sx=sx); B.spike(f, 17, 73, 8, 6, 10, 3, hot=1)
        w2 = B.part('wing_' + n + '2', (39, 72, 9), 'wing_' + n, (0, -10, 38), sx); w2.side = True
        B.box(w2, (39, 70, 8), (24, 3, 3), WBONE, noise=0.2); B.box(w2, (63, 71, 8), (16, 2, 2), WBONE, noise=0.2)
        for o, s in (((38, 24, 8), (2, 47, 2)), ((55, 32, 8), (2, 39, 2)), ((68, 44, 8), (2, 27, 2)), ((77, 48, 8), (2, 24, 2))): B.box(w2, o, s, WBONE, noise=0.2)   # doigts
        for o, s in (((40, 30, 9), (8, 40, 1)), ((48, 36, 9), (7, 34, 1)), ((57, 38, 9), (6, 32, 1)), ((63, 44, 9), (5, 26, 1)), ((70, 50, 9), (7, 20, 1))):
            B.rock(w2, o, s, MEMB, 0.5, faces='front back')
        B.box(w2, (37, 73, 8), (3, 6, 2), TIP); B.box(w2, (78, 72, 8), (2, 3, 2), TIP)      # griffes
        f = B.part('flame_wing_' + n + '2', (52, 73, 9), 'wing_' + n + '2', sx=sx); B.spike(f, 50, 72, 8, 4, 8, 3, hot=1)

    # ------------------------------------------------------------ fouet de feu (dans le poing droit)
    wave = [(52, 0), (-26, 14), (-30, -16), (-22, -14), (26, 16), (30, 12), (-24, -16), (-30, 10)]
    prev, z = 'arm_r', -7
    for i, (rx, ry) in enumerate(wave):
        t = 3 if i < 4 else 2
        x0 = -24 if i < 4 else -23; y0 = 25 if i < 4 else 25
        p = B.part(f'whip{i + 1}', (-22, 26, z), prev, (rx, ry, 0))
        B.fire(p, (x0, y0, z - 10), (t, t, 10), 1 + i % 2, 2 + i % 2)
        B.fire(p, (-23 if i % 2 else -22, y0 + t, z - 7), (1, 2, 4), 0, 3)       # langues de flamme
        B.fire(p, (-22 if i % 2 else -23, y0 - 2, z - 4), (1, 2, 3), 2, 4)
        prev, z = p.name, z - 10
    p = B.part('whip9', (-22, 26, z), prev, (18, 0, 0))                         # extremite embrasee
    B.fire(p, (-25, 23, z - 6), (6, 6, 6), 0, 1)
    B.fire(p, (-24, 24, z - 11), (4, 4, 5), 1, 3); B.fire(p, (-23, 25, z - 15), (2, 2, 4), 3, 5)
    B.spike(p, -24, 29, z - 5, 4, 7, 4, hot=1); B.fire(p, (-27, 25, z - 4), (2, 2, 3), 2, 4); B.fire(p, (-19, 25, z - 4), (2, 2, 3), 2, 4)
    B.fire(p, (-23, 20, z - 4), (2, 3, 2), 3, 2)
    return m

def glow_layer(m, name, k=0.85):
    """couche lumineuse : fissures (pixels d'art c/y/r) + boites de feu (texture entiere)"""
    tex = m.texture()
    im = Image.new('RGBA', (m.tw, m.th), (0, 0, 0, 0))
    for p in m.parts:
        for b in p.boxes:
            for face, (x0, y0, fw, fh) in m.face_rects(b).items():
                if b.glow:
                    for yy in range(fh):
                        for xx in range(fw): im.putpixel((x0 + xx, y0 + yy), shade(tex.getpixel((x0 + xx, y0 + yy)), k) + (255,))
                    continue
                for yy, row in enumerate(b.art.get(face) or []):
                    for xx, ch in enumerate(row):
                        if ch in GLOW and yy < fh and xx < fw: im.putpixel((x0 + xx, y0 + yy), rgb(b.pal[ch]) + (255,))
    save(im, f'{A}/textures/entity/{name}.png')

def demon(preview=None):
    m = build_model()
    m.export()
    if preview: m.preview(preview)
    glow_layer(m, 'demon_glow')
    return m
