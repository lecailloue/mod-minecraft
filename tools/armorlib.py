"""Armures 3D portees (orc, nain, elfe...) : boites sur les parties du HumanoidModel -> JSON de geometrie (GeoDef) + textures 32x32 par piece.
Repere : celui des modeles de Minecraft (y vers le bas), parties racine 'head' 'hat' 'body' 'right_arm' 'left_arm' 'right_leg' 'left_leg' aux pivots vanille.
Les pieces d'un meme calque (casque / plastron / bottes) partagent une feuille 64x64 (une case 32x32 par piece) ; les jambieres ont leur propre feuille.
"""
import copy, json, math, os, zlib
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(os.path.dirname(HERE), 'src/main/resources/assets/ringsofash')

PIVOT = {'head': (0, 0, 0), 'hat': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
         'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0)}
TILE = 32

# ------------------------------------------------------------------ materiaux
MATS = {}


def mat(name, base, n1=5, n2=6, tint=(1, 1, 1)):
    MATS[name] = dict(base=base, n1=n1, n2=n2, tint=tint)


class Cube:
    def __init__(s, o, sz, m, infl=0.0, key=None, deco=None, mirror=False, name=None):
        s.o, s.sz, s.mat, s.infl, s.key, s.deco, s.mirror, s.name = tuple(o), tuple(int(v) for v in sz), m, infl, key, deco, mirror, name
        s.uv = None


class Part:
    def __init__(s, name, pivot=None, rot=(0, 0, 0)):
        s.name = name
        s.pivot = tuple(pivot) if pivot is not None else PIVOT.get(name, (0, 0, 0))
        s.rot = tuple(rot)
        s.cubes, s.children = [], []

    def box(s, o, sz, m, infl=0.0, key=None, deco=None, name=None):
        c = Cube(o, sz, m, infl, key or f'k{len(s.cubes)}_{s.name}', deco, False, name)
        s.cubes.append(c)
        return c

    def sub(s, name, pivot, rot=(0, 0, 0)):
        p = Part(name, pivot, rot)
        s.children.append(p)
        return p


def mirrored(p, name, ox=None):
    """copie symetrique (x -> -x) : cubes miroir (meme UV), pivots et rotations reflechis"""
    q = Part(name, (-p.pivot[0], p.pivot[1], p.pivot[2]), (p.rot[0], -p.rot[1], -p.rot[2]))
    for c in p.cubes:
        n = Cube((-(c.o[0] + c.sz[0]), c.o[1], c.o[2]), c.sz, c.mat, c.infl, c.key, c.deco, not c.mirror, c.name)
        q.cubes.append(n)
    for ch in p.children:
        q.children.append(mirrored(ch, ch.name))
    return q


# ------------------------------------------------------------------ dessin
class Face:
    def __init__(s, arr, alpha, name):
        s.a, s.al, s.name = arr, alpha, name
        s.h, s.w = arr.shape[:2]

    def px(s, x, y, c, t=1.0):
        if 0 <= x < s.w and 0 <= y < s.h:
            s.a[y, x] = s.a[y, x] * (1 - t) + np.array(c, float) * t
            if t >= 0.5: s.al[y, x] = 255

    def rect(s, x, y, w, h, c, t=1.0):
        for yy in range(y, y + h):
            for xx in range(x, x + w): s.px(xx, yy, c, t)

    def hline(s, y, x0, x1, c, t=1.0):
        for x in range(x0, x1 + 1): s.px(x, y, c, t)

    def vline(s, x, y0, y1, c, t=1.0):
        for y in range(y0, y1 + 1): s.px(x, y, c, t)

    def clear(s, x, y, w, h):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if 0 <= xx < s.w and 0 <= yy < s.h: s.al[yy, xx] = 0

    def shade(s, x, y, w, h, f):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if 0 <= xx < s.w and 0 <= yy < s.h: s.a[yy, xx] *= f

    def art(s, x, y, rows, pal):
        for yy, row in enumerate(rows):
            for xx, ch in enumerate(row):
                if ch in '. ': continue
                s.px(x + xx, y + yy, pal[ch])


def lerp(a, b, t): return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def sh(c, f): return tuple(max(0, min(255, v * f)) for v in c)


SHADE = {'top': 1.12, 'bottom': 0.72, 'front': 1.0, 'back': 0.9, 'left': 0.93, 'right': 0.93}


def noise(h, w, s, rng):
    g = rng.random((h // s + 2, w // s + 2))
    return np.kron(g, np.ones((s, s)))[:h, :w]


def base_face(m, fw, fh, face, rng, bevel=True):
    d = MATS[m]
    lum = (noise(fh, fw, 1, rng) - .5) * d['n1'] * 2 + (noise(fh, fw, 3, rng) - .5) * d['n2'] * 2
    arr = np.array(d['base'], float)[None, None, :] + lum[:, :, None] * np.array(d['tint'])[None, None, :]
    arr *= SHADE[face]
    if bevel and fw >= 3 and fh >= 3:
        arr[0, :] *= 1.12
        arr[:, 0] *= 1.06
        arr[-1, :] *= .8
        arr[:, -1] *= .86
    return arr


def faces_of(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def all_cubes(parts):
    for p in parts:
        yield from p.cubes
        yield from all_cubes(p.children)


def _maxrects(items, tile):
    tw, th = tile if isinstance(tile, tuple) else (tile, tile)
    free = [(0, 0, tw, th)]
    place = {}
    for k, (rw, rh) in items:
        best = None
        for (fx, fy, fw, fh) in free:
            if rw <= fw and rh <= fh:
                sc = (min(fw - rw, fh - rh), max(fw - rw, fh - rh))
                if best is None or sc < best[0]: best = (sc, fx, fy)
        if best is None: return None
        _, x, y = best
        place[k] = (x, y)
        nf = []
        for (fx, fy, fw, fh) in free:
            if x >= fx + fw or x + rw <= fx or y >= fy + fh or y + rh <= fy:
                nf.append((fx, fy, fw, fh)); continue
            if x > fx: nf.append((fx, fy, x - fx, fh))
            if x + rw < fx + fw: nf.append((x + rw, fy, fx + fw - x - rw, fh))
            if y > fy: nf.append((fx, fy, fw, y - fy))
            if y + rh < fy + fh: nf.append((fx, y + rh, fw, fy + fh - y - rh))
        free = [r for i, r in enumerate(nf) if not any(j != i and r[0] >= o[0] and r[1] >= o[1] and r[0] + r[2] <= o[0] + o[2] and r[1] + r[3] <= o[1] + o[3] for j, o in enumerate(nf))]
    return place


def pack(parts, tile=TILE):
    """un rectangle (2(w+d) x (d+h)) par cle, rangement MaxRects (plusieurs ordres essayes)"""
    import random
    regs = {}
    for c in all_cubes(parts):
        w, h, d = c.sz
        r = (2 * (w + d), d + h)
        if c.key in regs: assert regs[c.key][0] == r, f'cle {c.key} : tailles differentes'
        else: regs[c.key] = (r, c)
    items = [(k, r) for k, (r, _) in regs.items()]
    orders = [sorted(items, key=lambda kv: -kv[1][1]), sorted(items, key=lambda kv: -kv[1][0]), sorted(items, key=lambda kv: -kv[1][0] * kv[1][1])]
    rnd = random.Random(7)
    for _ in range(3000):
        o = items[:]; rnd.shuffle(o); orders.append(o)
    place = None
    for o in orders:
        place = _maxrects(o, tile)
        if place: break
    if not place and items:
        used = sum(r[0] * r[1] for _, r in items)
        raise RuntimeError(f'hors de la case {tile} : aire {used} ; ' + ', '.join(f'{k}:{r[0]}x{r[1]}' for k, r in items))
    for c in all_cubes(parts): c.uv = place[c.key]
    return place, regs


def paint(parts, tile=TILE):
    place, regs = pack(parts, tile)
    tw, th = tile if isinstance(tile, tuple) else (tile, tile)
    im = np.zeros((th, tw, 4), np.uint8)
    for k, (r, c) in regs.items():
        u, v = place[k]
        w, h, d = c.sz
        for face, (x0, y0, fw, fh) in faces_of(u, v, w, h, d).items():
            rng = np.random.default_rng(zlib.crc32((k + face).encode()))
            arr = base_face(c.mat, fw, fh, face, rng)
            al = np.full((fh, fw), 255, np.uint8)
            F = Face(arr, al, face)
            if c.deco: c.deco(face, F, rng)
            im[y0:y0 + fh, x0:x0 + fw, :3] = np.clip(F.a, 0, 255)
            im[y0:y0 + fh, x0:x0 + fw, 3] = F.al
    return im


def to_json(parts, tx=0, ty=0, tw=64, th=64):
    out = []

    def walk(p, parent, pre):
        out.append({'name': p.name, 'parent': parent, 'pivot': [round(float(v), 4) for v in p.pivot], 'rot': [float(v) for v in p.rot],
                    'boxes': [{'o': [round(float(v), 4) for v in c.o], 's': list(c.sz), 'uv': [c.uv[0] + tx, c.uv[1] + ty], 'inflate': c.infl, 'mirror': c.mirror}
                              for c in p.cubes]})
        for ch in p.children: walk(ch, p.name, pre)

    names = ['head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg']
    have = {p.name: p for p in parts}
    for n in names:
        walk(have.get(n) or Part(n), None, '')
    return {'tw': tw, 'th': th, 'parts': out}


# ------------------------------------------------------------------ apercu local
def mannequin():
    """silhouette de joueur (parties pleines) pour juger de l'armure en place"""
    parts = []
    for n, o, s in [('head', (-4, -8, -4), (8, 8, 8)), ('body', (-4, 0, -2), (8, 12, 4)), ('right_arm', (-3, -2, -2), (4, 12, 4)),
                    ('left_arm', (-1, -2, -2), (4, 12, 4)), ('right_leg', (-2, 0, -2), (4, 12, 4)), ('left_leg', (-2, 0, -2), (4, 12, 4))]:
        parts.append({'name': n, 'parent': None, 'pivot': list(PIVOT[n]), 'rot': [0, 0, 0],
                      'boxes': [{'o': list(o), 's': list(s), 'uv': [0, 0], 'inflate': 0.0, 'mirror': False}]})
    return {'tw': 64, 'th': 64, 'parts': parts}


def raster(models, yaw=25, pitch=10, scale=9, size=(520, 700), crop=None, bg=(30, 30, 38)):
    """models : liste de (geo, texture RGBA ndarray) ; vue de face a yaw 0"""
    import render3d as R
    W, H = size
    f = np.array([math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)), -math.sin(math.radians(pitch)), math.cos(math.radians(yaw)) * math.cos(math.radians(pitch))])
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r); up = np.cross(r, f)
    L = np.array([-0.35, 0.8, -0.5]); L /= np.linalg.norm(L)
    img = np.zeros((H, W, 3)); img[:] = bg
    zb = np.full((H, W), 1e9)
    cy = crop if crop is not None else 8
    for geo, tex in models:
        tex = tex.astype(float)
        for pts, uvs in R.quads(geo):
            P = np.array([[(q @ r) * scale + W / 2, -((q @ up) - (24 - cy)) * scale + H / 2, q @ f] for q in pts])
            e1, e2 = P[1] - P[0], P[3] - P[0]
            det = e1[0] * e2[1] - e1[1] * e2[0]
            if abs(det) < 1e-6: continue
            x0, x1 = max(0, int(P[:, 0].min())), min(W - 1, int(P[:, 0].max()) + 1)
            y0, y1 = max(0, int(P[:, 1].min())), min(H - 1, int(P[:, 1].max()) + 1)
            if x1 < x0 or y1 < y0: continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            dx, dy = gx - P[0, 0], gy - P[0, 1]
            s = (dx * e2[1] - dy * e2[0]) / det; t = (e1[0] * dy - e1[1] * dx) / det
            m = (s >= 0) & (s <= 1) & (t >= 0) & (t <= 1)
            z = P[0, 2] + s * e1[2] + t * e2[2]
            (ua, va), (ub, vb), _, (ud, vd) = uvs
            U = ua + s * (ub - ua) + t * (ud - ua); V = va + s * (vb - va) + t * (vd - va)
            ui = np.clip(np.floor(U).astype(int), 0, tex.shape[1] - 1); vi = np.clip(np.floor(V).astype(int), 0, tex.shape[0] - 1)
            col = tex[vi, ui]
            m &= col[..., 3] > 25
            sub = zb[y0:y1 + 1, x0:x1 + 1]
            m &= z < sub
            if not m.any(): continue
            n = np.cross(pts[1] - pts[0], pts[3] - pts[0]); n /= (np.linalg.norm(n) or 1)
            if n @ f > 0: n = -n
            lum = 0.45 + 0.55 * max(0.0, n @ L)
            sub[m] = z[m]
            img[y0:y1 + 1, x0:x1 + 1][m] = np.clip(col[..., :3] * lum, 0, 255)[m]
    return Image.fromarray(img.astype(np.uint8))


def flat_tex(c=(120, 132, 156)):
    t = np.zeros((64, 64, 4), np.uint8); t[..., :3] = c; t[..., 3] = 255
    return t
