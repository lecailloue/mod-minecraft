#!/usr/bin/env python3
"""Apercu 3D fidele d'un modele geo (memes conventions que ModelPart en jeu : pivots, rotations ZYX, UV, miroir).
Usage : python3 tools/render3d.py demon [sortie.png]   -> planche face / dos / profil avec la couche lumineuse."""
import json, math, os, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(os.path.dirname(HERE), 'src/main/resources/assets/ringsofash')

def rot_zyx(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    X = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]); Z = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Z @ Y @ X

def quads(geo, pose=None):
    """liste de (4 sommets monde, 4 uv) ; pose = {piece: (dx, dy, dz) rotations ajoutees, en radians}"""
    pose = pose or {}
    mats, out = {}, []
    for p in geo['parts']:
        R0, T0 = mats[p['parent']] if p['parent'] else (np.eye(3), np.zeros(3))
        add = pose.get(p['name'], (0, 0, 0))
        R = R0 @ rot_zyx(p['rot'][0] + add[0], p['rot'][1] + add[1], p['rot'][2] + add[2])
        T = T0 + R0 @ np.array(p['pivot'], float)
        mats[p['name']] = (R, T)
        for b in p['boxes']:
            (x0, y0, z0), (w, h, d), (u, v), g = b['o'], b['s'], b['uv'], b['inflate']
            x1, y1, z1 = x0 + w + g, y0 + h + g, z0 + d + g; x0 -= g; y0 -= g; z0 -= g
            if b['mirror']: x0, x1 = x1, x0
            V = [None, (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
            f4, f5, f6, f7, f8, f9, f10, f11, f12 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w, v, v + d, v + d + h
            faces = [((6, 5, 1, 2), f5, f10, f6, f11), ((3, 4, 8, 7), f6, f11, f7, f10), ((1, 5, 8, 4), f4, f11, f5, f12),
                     ((2, 1, 4, 3), f5, f11, f6, f12), ((6, 2, 3, 7), f6, f11, f8, f12), ((5, 6, 7, 8), f8, f11, f9, f12)]
            for idx, u0, v0, u1, v1 in faces:
                pts = [T + R @ np.array(V[i], float) for i in idx]
                pts = [np.array([-q[0], 24 - q[1], q[2]]) for q in pts]       # espace modele -> monde (y vers le haut)
                out.append((pts, [(u1, v0), (u0, v0), (u0, v1), (u1, v1)]))
    return out

def render(name, yaw, pitch=12, scale=5, pose=None, bg=(24, 22, 28), glow=True, size=None, center=None):
    geo = json.load(open(f'{A}/geo/{name}.json'))
    tex = np.asarray(Image.open(f'{A}/textures/entity/{name}.png').convert('RGBA')).astype(float)
    gp = f'{A}/textures/entity/{name}_glow.png'
    gl = np.asarray(Image.open(gp).convert('RGBA')).astype(float) if glow and os.path.exists(gp) else None
    qs = quads(geo, pose)
    a, b = math.radians(yaw), math.radians(pitch)
    f = np.array([math.sin(a) * math.cos(b), -math.sin(b), math.cos(a) * math.cos(b)])      # yaw 0 = vu de face
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r); up = np.cross(r, f)
    L = np.array([-0.35, 0.8, -0.5]); L /= np.linalg.norm(L)
    allp = np.array([q for pts, _ in qs for q in pts])
    if size is None:
        sx, sy = allp @ r, allp @ up
        center = ((sx.min() + sx.max()) / 2, (sy.min() + sy.max()) / 2)
        size = (int((sx.max() - sx.min()) * scale) + 24, int((sy.max() - sy.min()) * scale) + 24)
    W, H = size
    img = np.zeros((H, W, 3)); img[:] = bg
    zb = np.full((H, W), 1e9)
    for pts, uvs in qs:
        P = np.array([[(q @ r - center[0]) * scale + W / 2, -(q @ up - center[1]) * scale + H / 2, q @ f] for q in pts])
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
        (ua, va), (ub, vb), (uc, vc), _ = uvs
        U = ua + s * (ub - ua) + t * (uvs[3][0] - ua); Vv = va + s * (ub * 0 + vb - va) + t * (uvs[3][1] - va)
        ui = np.clip(np.floor(U).astype(int), 0, tex.shape[1] - 1); vi = np.clip(np.floor(Vv).astype(int), 0, tex.shape[0] - 1)
        col = tex[vi, ui]
        m &= col[..., 3] > 25
        sub = zb[y0:y1 + 1, x0:x1 + 1]
        m &= z < sub
        if not m.any(): continue
        n = np.cross(pts[1] - pts[0], pts[3] - pts[0]); n /= (np.linalg.norm(n) or 1)
        if n @ f > 0: n = -n
        lum = 0.5 + 0.5 * max(0.0, n @ L)
        c = col[..., :3] * lum
        if gl is not None:
            g = gl[vi, ui]; c = c + g[..., :3] * (g[..., 3:4] / 255.0)
        sub[m] = z[m]
        img[y0:y1 + 1, x0:x1 + 1][m] = np.clip(c, 0, 255)[m]
    return Image.fromarray(img.astype(np.uint8))

def sheet(name, path, views=((25, 10), (155, 10), (90, 5)), scale=5, pose=None):
    ims = [render(name, y, p, scale, pose) for y, p in views]
    H = max(i.height for i in ims); W = sum(i.width for i in ims)
    out = Image.new('RGB', (W, H), (24, 22, 28)); x = 0
    for i in ims: out.paste(i, (x, H - i.height)); x += i.width
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    out.save(path); return out

if __name__ == '__main__':
    n = sys.argv[1] if len(sys.argv) > 1 else 'demon'
    sheet(n, sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(HERE), 'build', 'apercu', f'{n}_3d.png'))
