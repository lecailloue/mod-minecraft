"""Plantes d'automne de Fondcombe : fleurs, touffes du sous-bois et tas de feuilles au sol.
python3 tools/autumn.py   -> /tmp/autumn_preview.png (planche zoomee de toutes les textures)
Les sprites sont composes a partir de petits motifs (fleur, clochette, feuille) poses sur une tige.
"""
import math, os, random, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from tex import rgb, shade, mix, new

S = 16

# ------------------------------------------------------------------ outils
def P(im, x, y, c, a=255):
    if 0 <= x < S and 0 <= y < S and c is not None:
        im.putpixel((x, y), (c[0], c[1], c[2], a))


def stamp(im, art, pal, ox, oy):
    for j, row in enumerate(art):
        for i, ch in enumerate(row):
            if ch != '.':
                P(im, ox + i, oy + j, pal.get(ch))


def stem(im, x, y0, y1, col, dark, bend=0.0):
    """tige de y0 (bas) jusqu'a y1 (haut), legerement courbee"""
    n = max(1, y0 - y1)
    for k in range(n + 1):
        t = k / n
        xx = int(round(x + bend * t * t * n * 0.5))
        y = y0 - k
        P(im, xx, y, col if k % 3 else dark)
    return int(round(x + bend * n * 0.5)), y1


def sprig(im, x, y, dx, n, col):
    """petite feuille / ramille partant de la tige"""
    for k in range(n):
        P(im, x + dx * (k + 1), y - k, col)


# ------------------------------------------------------------------ motifs
BLOOM = ['.pp.', 'pyyp', '.pp.']
BLOOM_S = ['.p.', 'pyp', '.p.']
BELL = ['pp', 'py', '.p']
STAR = ['.p.p.', 'ppypp', '.pyp.', '..p..']
TUFT = ['.l.', 'lll']


def C(c):
    return rgb(c) if isinstance(c, str) else tuple(c[:3])


def pal_of(base, light, dark, centre):
    return {'p': C(base), 'c': C(light), 'o': C(dark), 'y': C(centre),
            'l': C(base), 's': rgb('#4b5c22'), 'S': rgb('#657a2c')}


GREEN, GREEN_D = rgb('#5b6e28'), rgb('#3f5020')


# ------------------------------------------------------------------ fleurs (bloc en croix)
def flower_cluster(base, light, centre, seed, n=4, big=False):
    """plusieurs fleurs rondes sur une touffe de tiges, comme la premiere rangee de la planche"""
    rnd = random.Random(seed)
    im = new(S)
    pal = pal_of(base, light, shade(rgb(base), 0.6), centre)
    spots = [(3, 4), (9, 3), (6, 7), (11, 8), (2, 9), (8, 11)][:n]
    for i, (bx, by) in enumerate(spots):
        sx, sy = stem(im, bx + 1, 15 - rnd.randrange(0, 2), by + 3, GREEN, GREEN_D, (0.3 if i % 2 else -0.3))
        sprig(im, sx, sy + 2 + rnd.randrange(0, 2), 1 if i % 2 else -1, 2, GREEN)
    for i, (bx, by) in enumerate(spots):
        art = STAR if (big and i == 0) else BLOOM if i % 3 != 2 else BLOOM_S
        stamp(im, art, pal, bx, by)
    return im


def flower_spike(base, light, centre, seed):
    """epi : des clochettes etagees le long d'une tige (4e image de la planche)"""
    rnd = random.Random(seed)
    im = new(S)
    pal = pal_of(base, light, shade(rgb(base), 0.6), centre)
    for bx, y0, y1 in ((6, 15, 2), (10, 15, 6)):
        stem(im, bx, y0, y1, GREEN, GREEN_D)
        for k in range(y1, y0 - 2, 2):
            d = -1 if (k // 2) % 2 else 1
            stamp(im, BELL, pal, bx + (1 if d > 0 else -2), k)
        P(im, bx, y1 - 1, rgb(base))
        P(im, bx, y1 - 2, rgb(light))
    for y in (13, 11, 9):
        sprig(im, 6, y, -1, 2, GREEN); sprig(im, 10, y - 1, 1, 2, GREEN)
    return im


def flower_lily(base, light, centre, seed):
    """grande fleur unique, six petales (derniere image de la premiere rangee)"""
    im = new(S)
    pal = pal_of(base, light, shade(rgb(base), 0.6), centre)
    stem(im, 8, 15, 8, GREEN, GREEN_D)
    sprig(im, 8, 13, -1, 3, GREEN); sprig(im, 8, 11, 1, 3, GREEN)
    art = ['..c...c..', '.cpp.ppc.', '.cppcppc.', 'ccppyppcc', '.cppyppc.', '..cpppc..', '...c.c...']
    stamp(im, art, pal, 4, 1)
    return im


# ------------------------------------------------------------------ touffes du sous-bois
def tuft(base, light, dark, seed, blades=11):
    """herbe haute : un eventail de brins partant d'une meme touffe"""
    rnd = random.Random(seed)
    im = new(S)
    cols = [C(dark), C(base), C(light)]
    for b in range(blades):
        f = (b - (blades - 1) / 2) / ((blades - 1) / 2)
        x = 7 + rnd.randrange(0, 2)
        h = 8 + rnd.randrange(0, 6) - int(abs(f) * 3)
        xx = x
        for k in range(h):
            t = k / max(1, h - 1)
            xx = int(round(x + f * 6.5 * t * t))
            P(im, xx, 15 - k, cols[min(2, int(t * 3))])
        P(im, xx, 15 - h, cols[2])
        if rnd.random() < 0.4:
            P(im, xx + (1 if f > 0 else -1), 15 - h, cols[2])
    return im


def bush(base, light, dark, seed, leafy=True):
    """buisson bas : des ramilles chargees de petites feuilles"""
    rnd = random.Random(seed)
    im = new(S)
    cols = [C(dark), C(base), C(light)]
    tips = []
    for b in range(6):
        a = -1.15 + b * 0.46 + rnd.random() * 0.2
        x, y = 7.5, 15.0
        n = 7 + rnd.randrange(0, 4)
        for k in range(n):
            x += math.sin(a) * 1.25
            y -= math.cos(a) * 1.15
            P(im, int(round(x)), int(round(y)), cols[0])
            if k > 1 and rnd.random() < 0.6:
                tips.append((int(round(x)), int(round(y))))
        tips.append((int(round(x)), int(round(y))))
    for (x, y) in tips:
        if leafy:
            art = rnd.choice((['ll', 'll'], ['.l', 'll'], ['ll', 'l.'], ['l']))
        else:
            art = rnd.choice((['l'], ['ll'], ['.l', 'l.']))
        stamp(im, art, {'l': cols[1 + (rnd.random() < 0.45)]}, x - 1, y - 1)
    return im


# ------------------------------------------------------------------ tas de feuilles (tapis)
LEAF = ['.l.', 'lLl', '.l.']
LEAF2 = ['ll.', 'lLl', '.ll']
LEAF3 = ['.l', 'll']


def litter(cols, seed, density=0.55, specks=0.25):
    """vu de dessus : des feuilles posees les unes sur les autres, le sol apparait entre elles"""
    rnd = random.Random(seed)
    im = new(S)
    pals = [{'l': rgb(c), 'L': mix(rgb(c), (255, 255, 255), 0.22)} for c in cols]
    n = int(60 * density)
    for _ in range(n):
        x, y = rnd.randrange(-1, S), rnd.randrange(-1, S)
        art = rnd.choice((LEAF, LEAF2, LEAF2, LEAF3))
        stamp(im, art, rnd.choice(pals), x, y)
    for _ in range(int(40 * specks)):
        x, y = rnd.randrange(0, S), rnd.randrange(0, S)
        c = rgb(rnd.choice(cols))
        P(im, x, y, shade(c, 0.75))
    return im


# ------------------------------------------------------------------ la planche
FLOWERS = {
    'flame_rose':    lambda: flower_cluster('#b3271b', '#e2542c', '#e8b53a', 11, 5),
    'gold_bell':     lambda: flower_cluster('#e0a722', '#f5d055', '#b9731a', 12, 4),
    'ember_bloom':   lambda: flower_cluster('#d4571a', '#ef8a30', '#e8c04a', 13, 4),
    'scarlet_spike': lambda: flower_spike('#c02a18', '#e8631f', '#d8a030', 14),
    'pale_daisy':    lambda: flower_cluster('#efe3bd', '#fdf6e0', '#e0a324', 15, 4),
    'red_sprig':     lambda: flower_cluster('#a8281c', '#d04a24', '#c23a1c', 16, 6),
    'golden_lily':   lambda: flower_lily('#e4a51f', '#f6d85d', '#c9641a', 17),
}

TUFTS = {
    'gold_tuft':   lambda: tuft('#c99a24', '#f0cc55', '#7f6a1e', 21),
    'red_tuft':    lambda: tuft('#a22a18', '#d4502a', '#6c2615', 22),
    'orange_tuft': lambda: tuft('#c4651c', '#eb9a32', '#7d4214', 23),
    'gold_bush':   lambda: bush('#c9a428', '#f0cc55', '#6b6424', 24),
    'red_bush':    lambda: bush('#a8301c', '#cf4a26', '#5e3a1e', 25, False),
}

LITTERS = {
    'golden_leaf_litter': lambda: litter(['#d8a62c', '#e9c04a', '#c07f20'], 31, 0.5),
    'red_leaf_litter':    lambda: litter(['#a6301b', '#c4471f', '#7d2414'], 32, 0.5),
    'orange_leaf_litter': lambda: litter(['#cb6a1b', '#e08f2a', '#a04d16'], 33, 0.5),
    'deep_leaf_litter':   lambda: litter(['#cf7c1e', '#e6b23c', '#a8341a', '#b55f18'], 34, 0.95, 0.4),
    'brown_leaf_litter':  lambda: litter(['#8a5a26', '#a8702f', '#6d4420'], 35, 0.28, 0.3),
}


def all_textures():
    out = {}
    for d in (FLOWERS, TUFTS, LITTERS):
        for k, f in d.items():
            out[k] = f()
    return out


if __name__ == '__main__':
    tex = all_textures()
    names = list(tex)
    cols, zoom = 7, 8
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * S * zoom, rows * S * zoom), (26, 24, 22, 255))
    for i, n in enumerate(names):
        im = tex[n].resize((S * zoom, S * zoom), Image.NEAREST)
        sheet.alpha_composite(im, ((i % cols) * S * zoom, (i // cols) * S * zoom))
    sheet.save('/tmp/autumn_preview.png')
    print(len(names), 'textures ->', ' '.join(names))
