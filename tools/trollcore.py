import json, uuid, math, base64, io, zlib, os
import numpy as np
from PIL import Image

_N = [0]


def uid():
    """identifiants deterministes : meme modele -> meme fichier"""
    _N[0] += 1
    return str(uuid.UUID(int=(_N[0] * 7919 + 12345) % (1 << 120)))


# ---------------------------------------------------------------- data model
class Cube:
    def __init__(s, name, x, y, z, w, h, d, mat, key=None, rot=(0, 0, 0), pivot=None,
                 inflate=0.0, fx=None):
        s.name = name
        s.frm = (x, y, z)
        s.size = (int(w), int(h), int(d))
        s.mat = mat
        s.key = key or name
        s.rot = tuple(rot)
        s.pivot = tuple(pivot) if pivot is not None else (x + w / 2, y + h / 2, z + d / 2)
        s.inflate = inflate
        s.fx = fx
        s.u = s.v = None
        s.uuid = uid()


class Group:
    def __init__(s, name, pivot, rot=(0, 0, 0), children=None):
        s.name = name
        s.pivot = tuple(pivot)
        s.rot = tuple(rot)
        s.children = children or []
        s.uuid = uid()


def normalize(groups):
    """un cube tourne devient un groupe tourne (le convertisseur du mod ne gere que les rotations de groupes)"""
    for g in groups:
        for i, ch in enumerate(g.children):
            if isinstance(ch, Group):
                normalize([ch])
            elif any(ch.rot):
                w = Group(ch.name, ch.pivot, ch.rot, [ch])
                ch.rot = (0, 0, 0)
                g.children[i] = w


def walk(groups, chain=()):
    """yield (cube, chain_of_groups_inner_to_outer)"""
    for g in groups:
        for c in g.children:
            if isinstance(c, Group):
                yield from walk([c], (g,) + chain)
            else:
                yield c, (g,) + chain


# ---------------------------------------------------------------- UV packing
def face_rects(u, v, w, h, d):
    return {
        'up': (u + d, v, w, d),
        'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h),
        'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h),
        'south': (u + 2 * d + w, v + d, w, h),
    }


def pack(cubes):
    regs = {}
    for c in cubes:
        w, h, d = c.size
        r = (2 * d + 2 * w, d + h)
        if c.key in regs:
            assert regs[c.key][0] == r and regs[c.key][1] == c.mat, f'key clash {c.key}'
        else:
            regs[c.key] = (r, c.mat)
    items = sorted(regs.items(), key=lambda kv: (-kv[1][0][1], -kv[1][0][0]))
    for W, H in [(128, 128), (128, 256), (256, 128), (256, 256), (256, 512), (512, 256), (512, 512), (512, 1024), (1024, 512)]:
        x = y = rowh = 0
        place = {}
        ok = True
        for k, ((rw, rh), _) in items:
            if x + rw > W:
                y += rowh
                x = 0
                rowh = 0
            if y + rh > H or rw > W:
                ok = False
                break
            place[k] = (x, y)
            x += rw
            rowh = max(rowh, rh)
        if ok:
            for c in cubes:
                c.u, c.v = place[c.key]
            return W, H
    raise RuntimeError('texture too big')


# ---------------------------------------------------------------- painting helpers
def noise(h, w, s, rng):
    g = rng.random((h // s + 2, w // s + 2))
    return np.kron(g, np.ones((s, s)))[:h, :w]


def put(arr, x, y, col, a=1.0):
    h, w, _ = arr.shape
    if 0 <= x < w and 0 <= y < h:
        arr[y, x] = arr[y, x] * (1 - a) + np.array(col, float) * a


def blob(arr, cx, cy, r, col, a, rng=None):
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            if dx * dx + dy * dy <= r * r + (1 if rng is None else int(rng.integers(0, 2))):
                put(arr, cx + dx, cy + dy, col, a)


def line(arr, x, y, dx, dy, n, col, a=1.0):
    for i in range(n):
        put(arr, x + dx * i, y + dy * i, col, a)


def rect(arr, x, y, w, h, col, a=1.0):
    for j in range(h):
        for i in range(w):
            put(arr, x + i, y + j, col, a)


MATS = {
    # classic orc
    'skin':    dict(base=(116, 117, 112), n1=9, n2=9, tint=(1, 1.0, .96)),
    'rust':    dict(base=(98, 90, 84), n1=8, n2=10, tint=(1, .95, .9)),
    'scrap':   dict(base=(68, 70, 74), n1=6, n2=8, tint=(1, 1, 1.05)),
    'leather': dict(base=(86, 58, 38), n1=7, n2=7, tint=(1, .9, .75)),
    'boot':    dict(base=(62, 44, 32), n1=6, n2=6, tint=(1, .9, .8)),
    'cloth':   dict(base=(98, 84, 62), n1=8, n2=8, tint=(1, .95, .85)),
    'bone':    dict(base=(214, 194, 138), n1=8, n2=6, tint=(1, .95, .8)),
    'spike':   dict(base=(122, 112, 102), n1=7, n2=7, tint=(1, .95, .9)),
    # elite black orc
    'bskin':   dict(base=(46, 46, 52), n1=4, n2=5, tint=(1, 1, 1.1)),
    'dmetal':  dict(base=(44, 45, 53), n1=4, n2=5, tint=(1, 1, 1.12)),
    'steel':   dict(base=(120, 122, 134), n1=6, n2=6, tint=(1, 1, 1.08)),
    'bleather': dict(base=(44, 31, 28), n1=4, n2=5, tint=(1, .9, .85)),
}

SHADE = {'up': 1.08, 'down': 0.78, 'north': 1.0, 'south': 0.92, 'east': 0.95, 'west': 0.9}


def base_face(mat, fw, fh, face, rng):
    m = MATS[mat]
    lum = (noise(fh, fw, 1, rng) - .5) * m['n1'] * 2 + (noise(fh, fw, 3, rng) - .5) * m['n2'] * 2 \
        + (noise(fh, fw, 2, rng) - .5) * m['n2']
    arr = np.array(m['base'], float)[None, None, :] + lum[:, :, None] * np.array(m['tint'])[None, None, :]
    arr *= SHADE[face]
    if fw >= 3 and fh >= 3:
        arr[0, :] *= .9
        arr[-1, :] *= .88
        arr[:, 0] *= .92
        arr[:, -1] *= .9
    return arr


# ---------------------------------------------------------------- material decor
def d_skin(arr, face, rng, dirt=1.0, scars=.45):
    h, w, _ = arr.shape
    area = h * w
    for _ in range(int(area / 40 * dirt)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), (70, 58, 40), .38, rng)
    for _ in range(int(area / 90)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (72, 72, 68), .6)
    if area >= 12 and rng.random() < scars:
        dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][int(rng.integers(0, 4))]
        n = int(rng.integers(3, max(4, min(7, max(w, h)))))
        x0 = int(rng.integers(0, w)); y0 = int(rng.integers(0, h))
        line(arr, x0, y0, dx, dy, n, (150, 138, 116), .85)
        line(arr, x0 + dy, y0 + dx, dx, dy, n, (52, 38, 34), .45)


def d_metal(arr, face, rng, rust=.5, rivets=True):
    h, w, _ = arr.shape
    if h >= 2 and w >= 2:
        m = noise(h, w, 2, rng) * .5 + noise(h, w, 3, rng) * .5
        mask = m > (1 - rust * .55)
        arr[mask] = arr[mask] * .35 + np.array((150, 74, 34)) * .65
        mask2 = (noise(h, w, 2, rng) > .86)
        arr[mask2] = arr[mask2] * .5 + np.array((46, 40, 38)) * .5
    if rivets and w >= 5 and h >= 5:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, (160, 154, 146))
            put(arr, x + 1, y + 1, (40, 36, 34), .7) if x + 1 < w and y + 1 < h else None
    if w >= 4 and h >= 4 and rng.random() < .7:
        line(arr, int(rng.integers(0, w - 2)), int(rng.integers(0, h - 2)), 1, 1, 3, (160, 160, 156), .4)


def d_leather(arr, face, rng):
    h, w, _ = arr.shape
    if w >= 5 and h >= 5:
        for x in range(1, w - 1):
            if x % 2 == 0:
                put(arr, x, 1, (132, 100, 68))
                put(arr, x, h - 2, (132, 100, 68))
    for _ in range(int(h * w / 30)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (130, 100, 70), .25, rng)


def d_cloth(arr, face, rng):
    h, w, _ = arr.shape
    arr[:, ::2, :] *= .93
    if h >= 3:
        for y in range(h // 2, h):
            a = (y - h // 2) / max(1, h - h // 2) * .4
            arr[y] = arr[y] * (1 - a) + np.array((66, 52, 38)) * a
        for x in range(w):
            if rng.random() < .35:
                put(arr, x, h - 1, (40, 30, 24))


def d_bone(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(.82, 1.05, h)[:, None, None]


def d_none(arr, face, rng):
    pass


def d_bskin(arr, face, rng):
    h, w, _ = arr.shape
    m = noise(h, w, 3, rng) > .74
    arr[m] += 13
    area = h * w
    if area >= 12 and rng.random() < .5:
        dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][int(rng.integers(0, 4))]
        n = int(rng.integers(3, max(4, min(7, max(w, h)))))
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), dx, dy, n, (92, 86, 90), .8)
    if area >= 16 and rng.random() < .5:
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, 3, (66, 34, 34), .6)


def d_dmetal(arr, face, rng):
    h, w, _ = arr.shape
    if h >= 2 and w >= 2:
        arr[0, :] += 16
        arr[:, 0] += 9
        arr[-1, :] -= 12
        arr[:, -1] -= 8
    for x in range(w):
        arr[:, x] *= 1 + (rng.random() - .5) * .08
    if w >= 9 and h >= 8:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, (96, 98, 110))
    if w >= 4 and h >= 4 and rng.random() < .6:
        line(arr, int(rng.integers(0, w - 2)), int(rng.integers(0, h - 2)), 1, 1, 3, (86, 86, 98), .35)


def d_steel(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.12, .8, h)[:, None, None]


DECOR = {
    'skin': d_skin, 'rust': d_metal, 'scrap': lambda a, f, r: d_metal(a, f, r, rust=.25),
    'leather': d_leather, 'boot': d_leather, 'cloth': d_cloth, 'bone': d_bone,
    'spike': lambda a, f, r: d_metal(a, f, r, rust=.3, rivets=False),
    'bskin': d_bskin, 'dmetal': d_dmetal, 'steel': d_steel, 'bleather': d_leather,
}


ALPHA = {}          # materiau -> opacite (0-255) pour les couches translucides


def paint_all(cubes, W, H):
    tex = np.zeros((H, W, 4), float)
    glow = np.zeros((H, W, 4), float)
    done = set()
    for c in cubes:
        if c.key in done:
            continue
        done.add(c.key)
        w, h, d = c.size
        rects = face_rects(c.u, c.v, w, h, d)
        F = {}
        for f, (x0, y0, fw, fh) in rects.items():
            rng = np.random.default_rng(zlib.crc32((c.key + f).encode()))
            arr = base_face(c.mat, fw, fh, f, rng)
            DECOR[c.mat](arr, f, rng)
            F[f] = arr
        if c.fx:
            rng = np.random.default_rng(zlib.crc32(c.key.encode()))

            def glowfn(face, x, y, col, rects=rects):
                x0, y0, fw, fh = rects[face]
                if 0 <= x < fw and 0 <= y < fh:
                    glow[y0 + y, x0 + x, :3] = col
                    glow[y0 + y, x0 + x, 3] = 255

            c.fx(F, rng, glowfn)
        for f, (x0, y0, fw, fh) in rects.items():
            tex[y0:y0 + fh, x0:x0 + fw, :3] = np.clip(F[f], 0, 255)
            tex[y0:y0 + fh, x0:x0 + fw, 3] = ALPHA.get(c.mat, 255)
    return tex.astype(np.uint8), glow.astype(np.uint8)


# ---------------------------------------------------------------- bbmodel export
def png_b64(img_arr):
    im = Image.fromarray(img_arr, 'RGBA')
    b = io.BytesIO()
    im.save(b, 'PNG')
    return base64.b64encode(b.getvalue()).decode()


def faces_json(c):
    w, h, d = c.size
    out = {}
    for f, (x0, y0, fw, fh) in face_rects(c.u, c.v, w, h, d).items():
        uv = [x0, y0, x0 + fw, y0 + fh]
        if f == 'down':  # Blockbench stores the down face vertically flipped
            uv = [x0, y0 + fh, x0 + fw, y0]
        out[f] = {'uv': uv, 'texture': 0}
    return out


def group_json(g):
    ch = []
    for c in g.children:
        ch.append(group_json(c) if isinstance(c, Group) else c.uuid)
    return {'name': g.name, 'origin': list(g.pivot), 'rotation': list(g.rot), 'color': 0,
            'uuid': g.uuid, 'export': True, 'mirror_uv': False, 'isOpen': True, 'locked': False,
            'visibility': True, 'autouv': 0, 'children': ch}


def export_bbmodel(name, groups, cubes, W, H, tex, path):
    els = []
    for i, c in enumerate(cubes):
        x, y, z = c.frm
        w, h, d = c.size
        els.append({
            'name': c.name, 'box_uv': True, 'rescale': False, 'locked': False, 'light_emission': 0,
            'render_order': 'default', 'allow_mirror_modeling': True,
            'from': [x, y, z], 'to': [x + w, y + h, z + d], 'autouv': 0, 'color': i % 8,
            'inflate': c.inflate, 'rotation': list(c.rot), 'origin': list(c.pivot),
            'uv_offset': [c.u, c.v], 'faces': faces_json(c), 'type': 'cube', 'uuid': c.uuid})
    data = {
        'meta': {'format_version': '4.10', 'model_format': 'modded_entity', 'box_uv': True},
        'name': name, 'geometry_name': name, 'modded_entity_version': 'Latest',
        'visible_box': [1, 1, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'unhandled_marker_colors': [], 'resolution': {'width': W, 'height': H},
        'elements': els, 'outliner': [group_json(g) for g in groups],
        'textures': [{'path': '', 'name': name + '.png', 'folder': '', 'namespace': '', 'id': '0',
                      'particle': False, 'render_mode': 'normal', 'render_sides': 'auto',
                      'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '',
                      'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False,
                      'uuid': uid(),
                      'source': 'data:image/png;base64,' + png_b64(tex)}],
    }
    with open(path, 'w') as f:
        json.dump(data, f)


# ---------------------------------------------------------------- software renderer
def rot_mat(rot):
    rx, ry, rz = [math.radians(a) for a in rot]
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0),
           'up': (0, 1, 0), 'down': (0, -1, 0)}


def quad_corners(f, lo, hi):
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    P = lambda x, y, z: np.array([x, y, z], float)
    if f == 'north':
        return P(x1, y1, z0), P(x0, y1, z0), P(x1, y0, z0), P(x0, y0, z0)
    if f == 'south':
        return P(x0, y1, z1), P(x1, y1, z1), P(x0, y0, z1), P(x1, y0, z1)
    if f == 'east':
        return P(x1, y1, z1), P(x1, y1, z0), P(x1, y0, z1), P(x1, y0, z0)
    if f == 'west':
        return P(x0, y1, z0), P(x0, y1, z1), P(x0, y0, z0), P(x0, y0, z1)
    if f == 'up':
        return P(x1, y1, z1), P(x0, y1, z1), P(x1, y1, z0), P(x0, y1, z0)
    return P(x1, y0, z0), P(x0, y0, z0), P(x1, y0, z1), P(x0, y0, z1)


def render(groups, tex, yaw, pitch=-16, W=420, H=560, scale=10.5, ybase=0, mode='all', img=None, zbuf=None):
    if img is None:
        img = np.zeros((H, W, 3), float)
        yy = np.linspace(0, 1, H)[:, None, None]
        img[:] = np.array((30, 33, 40)) * (1 - yy) + np.array((58, 62, 72)) * yy
        zbuf = np.full((H, W), -1e9)
    Rv = rot_mat((pitch, 0, 0)) @ rot_mat((0, yaw, 0))
    L = np.array((.45, .8, -.45)); L /= np.linalg.norm(L)
    cx, cy = W / 2, H - 40
    for c, chain in walk(groups):
        w, h, d = c.size
        lo = np.array(c.frm) - c.inflate
        hi = np.array(c.frm) + np.array((w, h, d)) + c.inflate
        Rc = rot_mat(c.rot)
        Rt = Rc.copy()
        for g in chain:
            Rt = rot_mat(g.rot) @ Rt
        rects = face_rects(c.u, c.v, w, h, d)
        for f, (x0, y0, fw, fh) in rects.items():
            corners = quad_corners(f, lo, hi)
            pts = []
            for p in corners:
                p = Rc @ (p - np.array(c.pivot)) + np.array(c.pivot)
                for g in chain:
                    p = rot_mat(g.rot) @ (p - np.array(g.pivot)) + np.array(g.pivot)
                pts.append(Rv @ p)
            n = Rv @ (Rt @ np.array(NORMALS[f], float))
            if n[2] > 0:
                continue
            nw = Rt @ np.array(NORMALS[f], float)
            light = .58 + .42 * max(0, float(nw @ L))
            sc = [(cx - p[0] * scale, cy - p[1] * scale, -p[2]) for p in pts]
            uvs = [(x0, y0), (x0 + fw, y0), (x0, y0 + fh), (x0 + fw, y0 + fh)]
            for tri in ((0, 1, 3), (0, 3, 2)):
                A, B, C = [sc[i] for i in tri]
                UA, UB, UC = [np.array(uvs[i], float) for i in tri]
                minx = int(max(0, math.floor(min(A[0], B[0], C[0]))))
                maxx = int(min(W - 1, math.ceil(max(A[0], B[0], C[0]))))
                miny = int(max(0, math.floor(min(A[1], B[1], C[1]))))
                maxy = int(min(H - 1, math.ceil(max(A[1], B[1], C[1]))))
                if minx > maxx or miny > maxy:
                    continue
                den = (B[1] - C[1]) * (A[0] - C[0]) + (C[0] - B[0]) * (A[1] - C[1])
                if abs(den) < 1e-9:
                    continue
                X, Y = np.meshgrid(np.arange(minx, maxx + 1) + .5, np.arange(miny, maxy + 1) + .5)
                l0 = ((B[1] - C[1]) * (X - C[0]) + (C[0] - B[0]) * (Y - C[1])) / den
                l1 = ((C[1] - A[1]) * (X - C[0]) + (A[0] - C[0]) * (Y - C[1])) / den
                l2 = 1 - l0 - l1
                inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
                if not inside.any():
                    continue
                depth = l0 * A[2] + l1 * B[2] + l2 * C[2]
                sub = zbuf[miny:maxy + 1, minx:maxx + 1]
                upd = inside & (depth > sub)
                if not upd.any():
                    continue
                U = l0 * UA[0] + l1 * UB[0] + l2 * UC[0]
                V = l0 * UA[1] + l1 * UB[1] + l2 * UC[1]
                tx = np.clip(np.floor(U - 1e-4).astype(int), 0, tex.shape[1] - 1)
                ty = np.clip(np.floor(V - 1e-4).astype(int), 0, tex.shape[0] - 1)
                al = tex[ty, tx, 3].astype(float) / 255
                if mode == 'opaque':
                    upd = upd & (al > .97)
                elif mode == 'trans':
                    upd = upd & (al <= .97)
                if not upd.any():
                    continue
                col = tex[ty, tx, :3].astype(float) * light
                region = img[miny:maxy + 1, minx:maxx + 1]
                if mode == 'trans':
                    region[upd] = region[upd] * (1 - al[upd, None]) + col[upd] * al[upd, None]
                else:
                    sub[upd] = depth[upd]
                    region[upd] = col[upd]
    if mode == 'opaque':
        return img, zbuf
    return np.clip(img, 0, 255).astype(np.uint8)
