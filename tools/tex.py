"""Peintres de textures pixel-art (blocs et objets)."""
import random, math, os
from PIL import Image

def rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))

def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3])

def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))

def ramp(base, n=5, lo=0.62, hi=1.22):
    base = rgb(base) if isinstance(base, str) else base
    return [shade(base, lo + (hi - lo) * i / (n - 1)) for i in range(n)]

def vnoise(size, cell, rnd):
    """bruit de valeur lisse, tuilable, dans [0,1]"""
    g = max(1, size // cell)
    grid = [[rnd.random() for _ in range(g)] for _ in range(g)]
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            fx, fy = x / cell, y / cell
            x0, y0 = int(fx) % g, int(fy) % g
            x1, y1 = (x0 + 1) % g, (y0 + 1) % g
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
            b = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out

def new(size=16, fill=(0, 0, 0, 0)):
    return Image.new('RGBA', (size, size), fill)

def stone(base, seed=0, size=16, var=1.0, cell=4, speck=0.5):
    rnd = random.Random(seed)
    pal = ramp(base, 6, 1 - 0.3 * var, 1 + 0.2 * var)
    n = vnoise(size, cell, rnd)
    im = new(size)
    for y in range(size):
        for x in range(size):
            v = n[y][x] + (rnd.random() - 0.5) * speck
            i = max(0, min(5, int(v * 6)))
            im.putpixel((x, y), pal[i] + (255,))
    return im

def bricks(base, mortar, seed=0, size=16, bw=8, bh=4, var=0.12):
    rnd = random.Random(seed)
    base = rgb(base); mortar = rgb(mortar)
    im = new(size)
    rows = size // bh
    for r in range(rows):
        off = (bw // 2) if r % 2 else 0
        for bx in range(-1, size // bw + 1):
            f = 1 + (rnd.random() - 0.5) * 2 * var
            col = shade(base, f)
            for yy in range(bh):
                for xx in range(bw):
                    x = bx * bw + off + xx; y = r * bh + yy
                    if not (0 <= x < size): continue
                    if yy == bh - 1 or xx == bw - 1:
                        c = mortar
                    elif yy == 0:
                        c = shade(col, 1.12)
                    elif yy == bh - 2:
                        c = shade(col, 0.88)
                    else:
                        c = shade(col, 1 + (rnd.random() - 0.5) * 0.08)
                    im.putpixel((x, y), c + (255,))
    return im

def planks(base, seed=0, size=16):
    rnd = random.Random(seed); base = rgb(base)
    im = new(size)
    for b in range(4):
        f = 1 + (rnd.random() - 0.5) * 0.14
        seam = rnd.randrange(size)
        for yy in range(4):
            for x in range(size):
                c = shade(base, f * (1 + (rnd.random() - 0.5) * 0.07))
                if yy == 3: c = shade(base, 0.62)
                elif x == seam: c = shade(base, 0.7)
                elif yy == 0: c = shade(c, 1.08)
                im.putpixel((x, b * 4 + yy), c + (255,))
    return im

def log_side(base, seed=0, size=16):
    rnd = random.Random(seed); base = rgb(base)
    im = new(size)
    cols = [1 + (rnd.random() - 0.5) * 0.3 for _ in range(size)]
    for x in range(size):
        for y in range(size):
            f = cols[x] * (1 + (rnd.random() - 0.5) * 0.1)
            if rnd.random() < 0.04: f *= 0.7
            im.putpixel((x, y), shade(base, f) + (255,))
    return im

def log_top(bark, wood, seed=0, size=16):
    rnd = random.Random(seed); bark = rgb(bark); wood = rgb(wood)
    im = new(size)
    for y in range(size):
        for x in range(size):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.6: c = shade(bark, 1 + (rnd.random() - 0.5) * 0.15)
            else:
                ring = int(d) % 2
                c = shade(wood, (0.86 if ring else 1.04) * (1 + (rnd.random() - 0.5) * 0.06))
            im.putpixel((x, y), c + (255,))
    return im

def leaves(base, seed=0, size=16, holes=0.18, accent=None):
    rnd = random.Random(seed)
    pal = ramp(base, 5, 0.6, 1.25)
    n = vnoise(size, 2, rnd)
    im = new(size)
    for y in range(size):
        for x in range(size):
            if rnd.random() < holes: continue
            v = n[y][x] * 0.6 + rnd.random() * 0.4
            c = pal[max(0, min(4, int(v * 5)))]
            if accent and rnd.random() < 0.06: c = rgb(accent)
            im.putpixel((x, y), c + (255,))
    return im

def ore(stone_im, specks, seed=0, n=6):
    rnd = random.Random(seed)
    im = stone_im.copy(); size = im.size[0]
    pal = [rgb(s) for s in specks]
    for _ in range(n):
        cx, cy = rnd.randrange(1, size - 2), rnd.randrange(1, size - 2)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)]:
            if rnd.random() < 0.75:
                x, y = (cx + dx) % size, (cy + dy) % size
                im.putpixel((x, y), pal[rnd.randrange(len(pal))] + (255,))
    return im

def pillar_side(base, seed=0, size=16):
    rnd = random.Random(seed); base = rgb(base)
    im = new(size)
    for x in range(size):
        k = x % 4
        f = [0.78, 1.08, 1.0, 0.9][k]
        for y in range(size):
            ff = f * (1 + (rnd.random() - 0.5) * 0.06)
            if y in (0, size - 1): ff = 1.15
            if y in (1, size - 2): ff = 0.7
            im.putpixel((x, y), shade(base, ff) + (255,))
    return im

def framed(base, seed=0, size=16, inner=None):
    """dalle encadree (dessus de pilier, bloc cisele)"""
    rnd = random.Random(seed); base = rgb(base)
    im = new(size)
    for y in range(size):
        for x in range(size):
            d = min(x, y, size - 1 - x, size - 1 - y)
            f = 1.12 if d == 0 else 0.72 if d == 1 else 1.0
            c = shade(base, f * (1 + (rnd.random() - 0.5) * 0.07))
            im.putpixel((x, y), c + (255,))
    return im

def stamp(im, art, pal, ox=0, oy=0):
    """dessine un motif ASCII. pal: char -> couleur (hex ou tuple) ; '.' transparent"""
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in '. ': continue
            c = pal[ch]
            if isinstance(c, str): c = rgb(c)
            if len(c) == 3: c = c + (255,)
            if 0 <= x + ox < im.size[0] and 0 <= y + oy < im.size[1]:
                im.putpixel((x + ox, y + oy), c)
    return im

def sprite(art, pal, size=16):
    return stamp(new(size), art, pal)

def matpal(base, trim='#c8a040', gem='#40c0e0', dark=None):
    """palette standard : o contour, d ombre, a base, b clair, c reflet, t garniture, u garniture sombre, g gemme, h gemme claire"""
    b = rgb(base) if isinstance(base, str) else base
    t = rgb(trim); g = rgb(gem)
    return {'o': shade(b, 0.38) if dark is None else rgb(dark), 'd': shade(b, 0.68), 'a': b, 'b': shade(b, 1.22),
            'c': mix(shade(b, 1.45), (255, 255, 255), 0.35), 't': t, 'u': shade(t, 0.62), 'v': shade(t, 1.3),
            'g': g, 'h': mix(g, (255, 255, 255), 0.5), 'k': (30, 26, 28), 'w': (240, 240, 235),
            's': (110, 78, 46), 'r': (74, 50, 30)}

def lava_frames(c0, c1, c2, frames=16, size=16, seed=3):
    """bande d'animation verticale style lave"""
    rnd = random.Random(seed)
    n1 = vnoise(32, 8, rnd); n2 = vnoise(32, 4, rnd)
    c0, c1, c2 = rgb(c0), rgb(c1), rgb(c2)
    im = Image.new('RGBA', (size, size * frames))
    for f in range(frames):
        ph = f / frames
        for y in range(size):
            for x in range(size):
                a = n1[(y + int(ph * 32)) % 32][(x + int(ph * 32)) % 32]
                b = n2[(y * 2 + 32 - int(ph * 32)) % 32][(x * 2 + int(ph * 64)) % 32]
                v = a * 0.65 + b * 0.35
                v = (math.sin(v * 9.0) + 1) / 2
                c = mix(c0, c1, min(1, v * 1.6)) if v < 0.6 else mix(c1, c2, (v - 0.6) / 0.4)
                im.putpixel((x, f * size + y), c + (255,))
    return im

# ---------- armes procedurales ----------
def _line(im, x0, y0, x1, y1, c):
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(n + 1):
        x = round(x0 + (x1 - x0) * i / n); y = round(y0 + (y1 - y0) * i / n)
        if 0 <= x < im.size[0] and 0 <= y < im.size[1]:
            im.putpixel((x, y), c + (255,) if len(c) == 3 else c)

def outline(im, col):
    src = im.copy(); w, h = im.size
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < w and 0 <= yy < h and src.getpixel((xx, yy))[3] > 0:
                        im.putpixel((x, y), col + (255,)); break
    return im

def sword(blade, guard, grip='#5a3a22', length=11, wide=False, curved=False, gem=None, size=16):
    """epee diagonale, poignee en bas a gauche"""
    im = new(size); b = rgb(blade); g = rgb(guard); gr = rgb(grip)
    s = size - 1
    # poignee
    for i in range(3):
        im.putpixel((1 + i, s - 1 - i), shade(gr, 1 - 0.15 * (i % 2)) + (255,))
    im.putpixel((0, s), shade(g, 0.9) + (255,))
    # garde
    gx, gy = 4, s - 4
    for d in (-2, -1, 0, 1, 2):
        im.putpixel((gx + d, gy + d), shade(g, 1.1 if d else 1.0) + (255,))
    if gem: im.putpixel((gx, gy), rgb(gem) + (255,))
    # lame
    for i in range(length):
        x, y = gx + 1 + i, gy - 1 - i
        if curved and i > length * 0.5: y += (i - length // 2) // 2
        if not (0 <= x < size and 0 <= y < size): break
        im.putpixel((x, y), shade(b, 1.0) + (255,))
        if 0 <= y - 1 and x - 0 < size and i < length - 1:
            im.putpixel((x, y - 1), shade(b, 1.3) + (255,))
        if x + 1 < size and y < size and i < length - 1:
            im.putpixel((min(size - 1, x + 1), y), shade(b, 0.72) + (255,)) if False else None
        if wide and i < length - 2 and y + 1 < size:
            im.putpixel((x, y + 1), shade(b, 0.75) + (255,))
    return outline(im, shade(b, 0.3))

def polearm(head_art, head_pal, shaft='#6b4a2a', size=16, ox=7, oy=0, shaft_from=(1, 14), shaft_to=(10, 5)):
    im = new(size); sc = rgb(shaft)
    _line(im, shaft_from[0], shaft_from[1], shaft_to[0], shaft_to[1], sc)
    _line(im, shaft_from[0] + 1, shaft_from[1], shaft_to[0] + 1, shaft_to[1], shade(sc, 0.75))
    stamp(im, head_art, head_pal, ox, oy)
    return outline(im, (30, 24, 20))

def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)

def tiles(base, seed=0, size=16, accent=None):
    """tuiles de toit en ecailles : rangees decalees, bord bas sombre, haut clair"""
    rnd = random.Random(seed); base = rgb(base)
    acc = rgb(accent) if accent else None
    im = new(size)
    rows, tw = 4, 8
    for r in range(rows):
        off = (tw // 2) if r % 2 else 0
        for bx in range(-1, size // tw + 1):
            f = 1 + (rnd.random() - 0.5) * 0.18
            col = shade(base, f)
            if acc and rnd.random() < 0.12: col = shade(acc, f)
            for yy in range(4):
                for xx in range(tw):
                    x = bx * tw + off + xx; y = r * 4 + yy
                    if not (0 <= x < size): continue
                    edge = xx == 0 or xx == tw - 1
                    if yy == 3: c = shade(col, 0.6)
                    elif yy == 2: c = shade(col, 0.84 if not edge else 0.7)
                    elif edge: c = shade(col, 0.8)
                    elif yy == 0: c = shade(col, 1.14)
                    else: c = shade(col, 1.0 + (rnd.random() - 0.5) * 0.06)
                    im.putpixel((x, y), c + (255,))
    return im

def plaster(base, seed=0, size=16):
    """enduit creme legerement ride"""
    rnd = random.Random(seed); base = rgb(base)
    n = vnoise(size, 4, rnd)
    im = new(size)
    for y in range(size):
        for x in range(size):
            f = 0.94 + n[y][x] * 0.1 + (rnd.random() - 0.5) * 0.035
            im.putpixel((x, y), shade(base, f) + (255,))
    return im

def timber(base, plast, seed=0, size=16):
    """colombage dore : cadre de bois sur enduit, croix de saint-andre"""
    rnd = random.Random(seed)
    im = plaster(plast, seed + 1, size)
    wood = rgb(base)
    for i in range(size):
        for (x, y) in ((i, i), (size - 1 - i, i)):
            im.putpixel((x, y), shade(wood, 0.95 + (rnd.random() - 0.5) * 0.1) + (255,))
    for i in range(size):
        for (x, y) in ((i, 0), (i, size - 1), (0, i), (size - 1, i), (i, 1), (1, i)):
            im.putpixel((x, y), shade(wood, 1.0 + (rnd.random() - 0.5) * 0.1) + (255,))
    return im

def filigree(base, gold, seed=0, size=16):
    """panneau ajoure orne : arc en ogive et rinceaux dores sur fond clair"""
    rnd = random.Random(seed)
    im = framed(base, seed, size)
    g = rgb(gold)
    for y in range(2, size - 2):
        half = max(0, 5 - max(0, (7 - y)) * 1)
        for x in range(size):
            d = abs(x - 7.5)
            if y >= 7 and abs(d - 3.5) < 0.7: im.putpixel((x, y), shade(g, 0.9) + (255,))
            elif y < 7 and abs(d - (7 - y) * 0.75) < 0.7 and y > 2: im.putpixel((x, y), shade(g, 1.05) + (255,))
    for x in (7, 8):
        for y in range(3, 14): im.putpixel((x, y), shade(g, 0.85) + (255,))
    return im
