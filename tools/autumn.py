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
LEAF = ['.l.', 'lLo', '.o.']
LEAF2 = ['ll.', 'lLo', '.oo']
LEAF3 = ['.l', 'lo']


def litter(cols, seed, density=0.55, specks=0.25):
    """vu de dessus : des feuilles posees les unes sur les autres, le sol apparait entre elles"""
    rnd = random.Random(seed)
    im = new(S)
    pals = [{'l': rgb(c), 'L': mix(rgb(c), (255, 255, 255), 0.10), 'o': shade(rgb(c), 0.62)} for c in cols]
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
    'golden_leaf_litter': lambda: litter(['#b4861f', '#caa032', '#8e6318', '#7a5a2a'], 31, 0.45),
    'red_leaf_litter':    lambda: litter(['#8c2616', '#a73a1a', '#64200f', '#7a4a22'], 32, 0.45),
    'orange_leaf_litter': lambda: litter(['#a85516', '#bd7322', '#833c11', '#7a5a2a'], 33, 0.45),
    'deep_leaf_litter':   lambda: litter(['#a9631a', '#c2942f', '#8c2a15', '#954c14', '#6d4a22'], 34, 0.85, 0.45),
    'brown_leaf_litter':  lambda: litter(['#6f4820', '#8a5c28', '#54371a'], 35, 0.26, 0.3),
}


# ------------------------------------------------------------------ foret de cypres (vert, humide)
CONIFER = {
    'forest_poppy':   lambda: flower_cluster('#b02018', '#d8422a', '#e8c84a', 41, 5),
    'forest_gold':    lambda: flower_cluster('#d9a81c', '#f3d24e', '#a8740f', 42, 4),
    'snow_bell':      lambda: flower_cluster('#eef0ea', '#ffffff', '#d8c24a', 43, 4),
    'crimson_spire':  lambda: flower_spike('#a81c14', '#cf3a1e', '#e0b030', 44),
    'green_tuft':     lambda: tuft('#4f7a2a', '#7fae44', '#31511b', 45),
    'pale_green_tuft':lambda: tuft('#6d9a35', '#9ec85a', '#43661f', 46),
    'fern_bush':      lambda: bush('#3f6b24', '#6a9a38', '#2b4718', 47),
}


# ------------------------------------------------------------------ details du sol et sous-bois des cinq biomes
def scatter(items, seed, n=26):
    """petits motifs semes au hasard, vus de dessus (tapis d'aiguilles, cailloux, brindilles)"""
    rnd = random.Random(seed)
    im = new(S)
    for _ in range(n):
        art, pal = items[rnd.randrange(len(items))]
        stamp(im, art, pal, rnd.randrange(-1, S), rnd.randrange(-1, S))
    return im


def needles(base, light, dark, seed):
    a, b, c = {'l': C(base)}, {'l': C(light)}, {'l': C(dark)}
    arts = [(['ll.', '..l'], a), (['.l', 'l.'], b), (['lll'], c), (['l', 'l', 'l'], a), (['l.', '.l'], c), (['ll'], b)]
    return scatter(arts, seed, 34)


def pebbles(seed):
    pal = lambda h: {'l': rgb(h), 'L': shade(rgb(h), 1.25), 'o': shade(rgb(h), 0.7)}
    arts = [(['.ll.', 'lLLo', '.oo.'], pal('#8a8a84')), (['ll', 'Lo'], pal('#9b9890')), (['.l.', 'lLo'], pal('#787873')),
            (['ll', 'lo'], pal('#a09d94')), (['l'], pal('#6f6f6b'))]
    return scatter(arts, seed, 22)


def twigs(seed):
    pal = lambda h: {'l': rgb(h), 'o': shade(rgb(h), 0.72)}
    arts = [(['ll..', '..lo'], pal('#6b5236')), (['l...', '.ll.', '...o'], pal('#5a452e')), (['lll'], pal('#7a5f3e')),
            (['l', 'l', 'o'], pal('#604a30')), (['.l.', 'l.o'], pal('#72573a'))]
    return scatter(arts, seed, 13)


def mushrooms(cap, light, stem, seed, n=4, tall=False):
    """une touffe de champignons : chapeaux ronds sur de courts pieds"""
    rnd = random.Random(seed)
    im = new(S)
    pc, lc, sc = C(cap), C(light), C(stem)
    spots = [(2, 12), (7, 10), (12, 13), (5, 15), (13, 7)][:n]
    for (x, y) in spots:
        hh = (3 + rnd.randrange(0, 3)) if tall else (1 + rnd.randrange(0, 2))
        for k in range(hh):
            P(im, x, 15 - k, sc); P(im, x + 1, 15 - k, shade(sc, 0.8))
        top = 15 - hh
        art = ['.ll.', 'lLLl', '.oo.'] if rnd.random() < 0.5 else ['.l.', 'lLl', '.o.']
        stamp(im, art, {'l': pc, 'L': lc, 'o': shade(pc, 0.65)}, x - 1, top - 2)
    return im


def reeds(base, light, dark, seed, head=None):
    """roseaux : de longues tiges droites, parfois un epi brun au bout"""
    rnd = random.Random(seed)
    im = new(S)
    cols = [C(dark), C(base), C(light)]
    for b in range(7):
        x = 1 + b * 2 + rnd.randrange(0, 2)
        h = 10 + rnd.randrange(0, 6)
        bend = rnd.choice((-0.25, 0, 0, 0.25))
        xx = x
        for k in range(h):
            t = k / max(1, h - 1)
            xx = int(round(x + bend * t * t * 4))
            P(im, xx, 15 - k, cols[min(2, int(t * 2.5))])
        if head and rnd.random() < 0.6:
            for k in range(3):
                P(im, xx, 15 - h + 1 - k, C(head)); P(im, xx + 1, 15 - h + 1 - k, shade(C(head), 0.78))
        else:
            P(im, xx, 15 - h, cols[2])
    return im


def berry_bush(leaf, light, berry, seed):
    rnd = random.Random(seed)
    im = new(S)
    for b in range(7):
        a = -1.1 + b * 0.37
        x, y = 7.5, 15.0
        for k in range(4 + rnd.randrange(0, 3)):
            x += math.sin(a) * 1.15; y -= math.cos(a) * 1.1
            stamp(im, rnd.choice((['ll', 'll'], ['.l', 'll'], ['l'])), {'l': C(leaf) if rnd.random() < 0.6 else C(light)}, int(x) - 1, int(y) - 1)
    for _ in range(7):
        x, y = rnd.randrange(2, 14), rnd.randrange(4, 14)
        stamp(im, ['ll', 'lo'], {'l': C(berry), 'o': shade(C(berry), 0.7)}, x, y)
    return im


def clover(seed):
    rnd = random.Random(seed)
    im = new(S)
    g, g2 = rgb('#3f7a2a'), rgb('#5d9c38')
    for _ in range(14):
        x, y = rnd.randrange(1, 14), rnd.randrange(5, 14)
        for k in range(1 + rnd.randrange(0, 3)): P(im, x, 15 - k, g)
        stamp(im, ['.l.', 'lLl'], {'l': g2, 'L': g}, x - 1, y - 1)
        if rnd.random() < 0.3: P(im, x, y - 2, rgb('#f0f0e6'))
    return im


def wild_wheat(seed):
    rnd = random.Random(seed)
    im = new(S)
    stem, grain, pale = rgb('#8a8a3e'), rgb('#d9b53c'), rgb('#efd673')
    for b in range(6):
        x = 2 + b * 2 + rnd.randrange(0, 2)
        h = 9 + rnd.randrange(0, 5)
        for k in range(h): P(im, x, 15 - k, stem)
        for k in range(3 + rnd.randrange(0, 3)):
            y = 15 - h + k
            P(im, x, y, grain if k % 2 else pale)
            if rnd.random() < 0.6: P(im, x + (1 if k % 2 else -1), y, shade(grain, 0.85))
    return im


DETAILS = {
    'needle_litter':  lambda: needles('#2f4a24', '#486b31', '#5a4a2a', 51),
    'pebbles':        lambda: pebbles(52),
    'twigs':          lambda: twigs(53),
    'bog_moss':       lambda: scatter([(['.ll.', 'lLLl', '.ll.'], {'l': rgb('#4a5a2e'), 'L': rgb('#61753a')}),
                                       (['ll', 'll'], {'l': rgb('#55663a')}), (['l'], {'l': rgb('#6d7c48')})], 54, 30),
    'amber_mushroom': lambda: mushrooms('#c9711c', '#e8a03c', '#e0d2b0', 55, 5),
    'pale_mushroom':  lambda: mushrooms('#d8d6c4', '#f2f0e2', '#cfcab4', 56, 4, True),
    'swamp_mushroom': lambda: mushrooms('#6a5b78', '#8b7b9a', '#9a9486', 57, 4),
    'reed_tuft':      lambda: reeds('#5e7a30', '#88a845', '#3d5420', 58),
    'cattail':        lambda: reeds('#4e6b2c', '#7a9a3c', '#36491c', 59, head='#6b4a26'),
    'dead_tuft':      lambda: tuft('#8a7a56', '#a99a74', '#5e5238', 60),
    'clover':         lambda: clover(61),
    'field_daisies':  lambda: flower_cluster('#f2f2e8', '#ffffff', '#e8c84a', 62, 6),
    'wild_wheat':     lambda: wild_wheat(63),
    'blueberry_bush': lambda: berry_bush('#2f5a2a', '#447a38', '#49608f', 64),
}


def all_textures():
    out = {}
    for d in (FLOWERS, TUFTS, LITTERS, CONIFER, DETAILS):
        for k, f in d.items():
            out[k] = f()
    return out


if __name__ == '__main__':
    tex = all_textures()
    names = list(tex)
    cols, zoom = 7, 7
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * S * zoom, rows * S * zoom), (26, 24, 22, 255))
    for i, n in enumerate(names):
        im = tex[n].resize((S * zoom, S * zoom), Image.NEAREST)
        sheet.alpha_composite(im, ((i % cols) * S * zoom, (i // cols) * S * zoom))
    sheet.save('/tmp/autumn_preview.png')
    print(len(names), 'textures ->', ' '.join(names))
