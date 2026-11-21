#!/usr/bin/env python3
"""Icones d'inventaire des armures : rendu 3D (memes modeles et textures qu'en jeu) de chaque piece, vue de 3/4, contour sombre.
Usage : python3 tools/gen_armor_icons.py [ensemble ...]   (tous par defaut)"""
import math, os, sys
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import armors3d as A3
import armorlib as AL
import render3d as R

ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, 'src/main/resources/assets/ringsofash')
ITEM = os.path.join(RES, 'textures/item')
SZ = 64
NAME = {'head': 'helmet', 'chest': 'chestplate', 'legs': 'leggings', 'boots': 'boots'}


def render_rgba(models, yaw=22, pitch=10, S=2048, hide=()):
    """rendu 3D -> image RGBA (fond transparent), cadree sur la piece, avec reflets"""
    f = np.array([math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)), -math.sin(math.radians(pitch)), math.cos(math.radians(yaw)) * math.cos(math.radians(pitch))])
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r); up = np.cross(r, f)
    L = np.array([-0.35, 0.8, -0.5]); L /= np.linalg.norm(L)
    quads = []
    for geo, tex in models:
        g = {'tw': geo.get('tw', 64), 'th': geo.get('th', 64), 'parts': [p for p in geo['parts'] if p['name'] not in hide and p.get('parent') not in hide]}
        for pts, uvs in R.quads(g): quads.append((pts, uvs, tex.astype(float)))
    allp = np.array([q for pts, _, _ in quads for q in pts])
    sx, sy = allp @ r, allp @ up
    if not len(allp): return None
    w, h = sx.max() - sx.min(), sy.max() - sy.min()
    scale = (S - 40) / max(w, h)
    W = H = S
    cxm, cym = (sx.min() + sx.max()) / 2, (sy.min() + sy.max()) / 2
    img = np.zeros((H, W, 3)); zb = np.full((H, W), 1e9); al = np.zeros((H, W), bool)
    for pts, uvs, tex in quads:
        P = np.array([[((q @ r) - cxm) * scale + W / 2, -((q @ up) - cym) * scale + H / 2, q @ f] for q in pts])
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
        lum = 0.55 + 0.6 * max(0.0, n @ L)
        sub[m] = z[m]
        img[y0:y1 + 1, x0:x1 + 1][m] = np.clip(col[..., :3] * lum, 0, 255)[m]
        al[y0:y1 + 1, x0:x1 + 1] |= m
    return img, al


def finish(img, al, size=SZ, pad=3):
    """reduit le rendu en icone : cadrage serre, contour sombre d'un pixel, coloris un peu pousses"""
    ys, xs = np.where(al)
    y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    side = max(y1 - y0, x1 - x0)
    cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
    half = side // 2 + 2
    crop = np.zeros((2 * half, 2 * half, 4), np.uint8)
    a = np.zeros((2 * half, 2 * half), np.uint8)
    for yy in range(2 * half):
        pass
    sy0, sx0 = cy - half, cx - half
    ys0, ye = max(0, sy0), min(img.shape[0], sy0 + 2 * half)
    xs0, xe = max(0, sx0), min(img.shape[1], sx0 + 2 * half)
    crop[ys0 - sy0:ye - sy0, xs0 - sx0:xe - sx0, :3] = img[ys0:ye, xs0:xe].astype(np.uint8)
    crop[ys0 - sy0:ye - sy0, xs0 - sx0:xe - sx0, 3] = al[ys0:ye, xs0:xe] * 255
    im = Image.fromarray(crop, 'RGBA')
    inner = size - 2 * pad
    # reduction par moyenne ponderee par l'alpha (pas de halo sombre)
    prem = np.asarray(im).astype(float); prem[..., :3] *= prem[..., 3:4] / 255.0
    pm = Image.fromarray(prem.astype(np.uint8), 'RGBA').resize((inner, inner), Image.BOX)
    arr = np.asarray(pm).astype(float)
    alpha = arr[..., 3]
    rgb = np.where(alpha[..., None] > 0, arr[..., :3] / np.maximum(alpha[..., None] / 255.0, 1e-3), 0)
    alpha = np.where(alpha > 110, 255, 0).astype(np.uint8)
    # contraste / saturation un peu pousses
    mean = rgb.mean(axis=2, keepdims=True)
    rgb = np.clip(mean + (rgb - mean) * 1.18, 0, 255)
    rgb = np.clip((rgb - 128) * 1.1 + 128, 0, 255)
    out = np.zeros((size, size, 4), np.uint8)
    out[pad:pad + inner, pad:pad + inner, :3] = rgb.astype(np.uint8)
    out[pad:pad + inner, pad:pad + inner, 3] = alpha
    # contour sombre
    m = out[..., 3] > 0
    edge = ndi.binary_dilation(m, structure=np.ones((3, 3), bool)) & ~m
    inside_edge = m & ~ndi.binary_erosion(m, structure=np.ones((3, 3), bool))
    out[inside_edge, :3] = (out[inside_edge, :3].astype(float) * 0.62).astype(np.uint8)
    out[edge] = (14, 12, 16, 255)
    return Image.fromarray(out, 'RGBA')


def vanilla_geo(kind, layer_inf):
    """geometrie d'une armure vanilla (boites gonflees) pour les ensembles sans modele 3D"""
    P = AL.PIVOT
    def part(n, o, s, uv, inf, mirror=False):
        return {'name': n, 'parent': None, 'pivot': list(P[n]), 'rot': [0, 0, 0], 'boxes': [{'o': o, 's': s, 'uv': uv, 'inflate': inf, 'mirror': mirror}]}
    if kind == 'head': return {'tw': 64, 'th': 32, 'parts': [part('head', [-4, -8, -4], [8, 8, 8], [0, 0], layer_inf)]}
    if kind == 'chest':
        return {'tw': 64, 'th': 32, 'parts': [part('body', [-4, 0, -2], [8, 12, 4], [16, 16], layer_inf), part('right_arm', [-3, -2, -2], [4, 12, 4], [40, 16], layer_inf),
                                              part('left_arm', [-1, -2, -2], [4, 12, 4], [40, 16], layer_inf, True)]}
    if kind == 'legs':
        return {'tw': 64, 'th': 32, 'parts': [part('body', [-4, 0, -2], [8, 12, 4], [16, 16], layer_inf), part('right_leg', [-2, 0, -2], [4, 12, 4], [0, 16], layer_inf),
                                              part('left_leg', [-2, 0, -2], [4, 12, 4], [0, 16], layer_inf, True)]}
    return {'tw': 64, 'th': 32, 'parts': [part('right_leg', [-2, 0, -2], [4, 12, 4], [0, 16], layer_inf), part('left_leg', [-2, 0, -2], [4, 12, 4], [0, 16], layer_inf, True)]}


def icon_3d(name, only=None):
    built = A3.build_set(name)
    sheets = A3.sheet_textures(built)
    done = []
    for pc in A3.PIECES:
        if only and NAME[pc] not in only: continue
        tx, ty, layer = A3.TILE_OF[pc]
        geo = A3.piece_geo(built, pc)
        hide = ('cape',)
        res = render_rgba([(geo, sheets[layer])], yaw=24, pitch=10, hide=hide)
        ic = finish(*res)
        ic.save(os.path.join(ITEM, f'{name}_{NAME[pc]}.png')); done.append(ic)
    return done


def icon_vanilla(name, layer1, layer2, pieces=('head', 'chest', 'legs', 'boots')):
    done = []
    for pc in pieces:
        t = Image.open(os.path.join(RES, 'textures/models/armor', f'{name}_layer_{2 if pc == "legs" else 1}.png')).convert('RGBA')
        inf = 0.5 if pc == 'legs' else 1.0
        res = render_rgba([(vanilla_geo(pc, inf), np.asarray(t))], yaw=24, pitch=10)
        if res is None or not res[1].any(): continue
        ic = finish(*res)
        ic.save(os.path.join(ITEM, f'{name}_{NAME[pc]}.png')); done.append(ic)
    return done


if __name__ == '__main__':
    want = sys.argv[1:]
    sets3d = [n for n in A3.SETS if n != 'feet']
    sets3d = [n for n in sets3d if os.path.exists(os.path.join(RES, f'geo/armor_{n}_head.json'))]
    van = ['ashlord', 'lunargent']
    sheet_imgs = []
    for n in sets3d:
        if want and n not in want: continue
        sheet_imgs.append((n, icon_3d(n))); print('3d', n)
    for n in van:
        if want and n not in want: continue
        sheet_imgs.append((n, icon_vanilla(n, 1, 2))); print('vanilla', n)
    W = 4 * (SZ + 4) + 8
    pv = Image.new('RGBA', (W * 2 if False else W, (len(sheet_imgs)) * (SZ + 4) + 4), (120, 120, 126, 255))
    for i, (n, ims) in enumerate(sheet_imgs):
        for j, ic in enumerate(ims): pv.paste(ic, (4 + j * (SZ + 4), 4 + i * (SZ + 4)), ic)
    pv.resize((pv.width * 2, pv.height * 2), Image.NEAREST).save('/tmp/armor_icons_preview.png')
