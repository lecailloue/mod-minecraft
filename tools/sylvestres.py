"""Sylvestres (Sylvgardes) : 2 variantes en plus du modele actuel (tools/assets/treeman.bbmodel). Fichiers 3D uniquement.
  sylvestre_ancien : le vieux Sylvestre massif, courbe, au visage ride et a la longue barbe de mousse (photos 1 a 3)
  sylvestre_saule  : le Sylvestre-saule, grand et maigre, aux membres tordus et aux rideaux de feuilles qui pendent (photos 5 et 6)
Conventions : y vers le haut, sol y=0, avant = -z, cote -x = 'left'. 1 unite = 3,2 cm (comme le modele actuel : 157 unites = 5 m).
Pieces animables (noms reconnus par GeoModel) : body, head, arm_l/arm_r (+ forearm_*, doigts), leg_l/leg_r (+ shin_*), beard_*, frond_*, branches de la couronne."""
import math
import numpy as np
import trollcore as T
from trollcore import *
from orcs import C, translate, scale_all, unique_names

UNIT_CM = 3.2

# ---------------------------------------------------------------- bruit et materiaux
def vnoise(x, y, z, seed):
    xi, yi, zi = np.floor(x).astype(np.int64), np.floor(y).astype(np.int64), np.floor(z).astype(np.int64)
    fx, fy, fz = x - xi, y - yi, z - zi
    u = lambda t: t * t * (3 - 2 * t)
    fx, fy, fz = u(fx), u(fy), u(fz)

    def h(a, b, c):
        v = (a * 73856093) ^ (b * 19349663) ^ (c * 83492791) ^ (seed * 374761393)
        v = (v ^ (v >> 13)) * 1274126177
        return ((v ^ (v >> 16)) & 0xFFFF) / 65535.0
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                out = out + h(xi + dx, yi + dy, zi + dz) * (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
    return out


MAT = {
    'sy_bark': dict(base=(88, 66, 46), n1=6, n2=8, tint=(1, 1, 1)),
    'sy_dark': dict(base=(58, 44, 32), n1=5, n2=6, tint=(1, 1, 1)),
    'sy_moss': dict(base=(74, 102, 46), n1=8, n2=8, tint=(.9, 1, .8)),
    'sy_beard': dict(base=(124, 138, 70), n1=8, n2=9, tint=(1, 1, .8)),
    'sy_leaf': dict(base=(76, 118, 40), n1=8, n2=8, tint=(.9, 1, .8)),
    'sy_leaf2': dict(base=(126, 150, 50), n1=8, n2=8, tint=(1, 1, .8)),
    'sy_eye': dict(base=(14, 10, 6), n1=2, n2=2, tint=(1, 1, 1)),
}
T.MATS.update(MAT)


def bark_rgb(X, Y, Z, face, base=(88, 66, 46), moss=1.0, up=0.0, seed=0):
    """ecorce : sillons verticaux, creux sombres, aretes claires, mousse (surtout en haut) et lichen gris"""
    f1 = vnoise(X / 1.5, Y / 8, Z / 1.5, 1 + seed)
    f2 = vnoise(X / .8, Y / 3, Z / .8, 2 + seed)
    lum = 1 + (f1 - .5) * .55 + (f2 - .5) * .28
    col = np.array(base, float)[None, None, :] * lum[..., None]
    crev = np.clip((.40 - f1) / .12, 0, 1)
    col = col * (1 - crev[..., None] * .45)
    ridge = np.clip((f1 - .68) / .15, 0, 1)
    col = col + ridge[..., None] * np.array((18, 12, 4))[None, None, :]
    m = vnoise(X / 4, Y / 4, Z / 4, 3 + seed) + up + np.clip(Y / 1500.0, 0, .12)
    mm = np.clip((m - .74) / .12, 0, 1) * .85 * moss
    mc = np.array((70, 100, 40), float)[None, None, :] * (.85 + .3 * vnoise(X / 1.1, Y / 1.1, Z / 1.1, 7 + seed)[..., None])
    col = col * (1 - mm[..., None]) + mc * mm[..., None]
    li = np.clip((vnoise(X / 3, Y / 3, Z / 3, 5 + seed) - .78) / .1, 0, 1) * .8 * moss
    col = col * (1 - li[..., None]) + np.array((150, 156, 136))[None, None, :] * li[..., None]
    col *= SHADE[face]
    return col


def hair_rgb(X, Y, Z, face, a=(86, 102, 46), b=(178, 186, 106), seed=0):
    """barbe de mousse : meches verticales claires et sombres"""
    n = vnoise(X / .8, Y / 7, Z / .8, 11 + seed)
    n2 = vnoise(X / 2.2, Y / 3, Z / 2.2, 12 + seed)
    t = np.clip(n * .7 + n2 * .5 - .1, 0, 1)
    col = np.array(a, float)[None, None, :] * (1 - t[..., None]) + np.array(b, float)[None, None, :] * t[..., None]
    col *= SHADE[face]
    return col


def leaf_rgb(X, Y, Z, face, a, seed=0):
    n = vnoise(X / 1.2, Y / 1.2, Z / 1.2, 21 + seed)
    n2 = vnoise(X / .6, Y / .6, Z / .6, 22 + seed)
    col = np.array(a, float)[None, None, :] * (.78 + .5 * n[..., None] * .8 + .2 * n2[..., None])
    vein = np.clip(1 - np.abs(vnoise(X / 2, Y / 5, Z / 2, 23 + seed) - .5) / .04, 0, 1) * .25
    col = col * (1 - vein[..., None]) + np.array((190, 210, 120))[None, None, :] * vein[..., None]
    col *= SHADE[face]
    return col


def _face_grid(arr, off):
    fh, fw, _ = arr.shape
    X = np.broadcast_to(np.arange(fw)[None, :] + .5, (fh, fw)).astype(float) + off[0]
    Y = np.broadcast_to(np.arange(fh)[:, None] + .5, (fh, fw)).astype(float) + off[1]
    Z = np.full((fh, fw), float(off[2]))
    return X, Y, Z


def mk_decor(kind):
    def d(arr, face, rng):
        off = (rng.integers(0, 500), rng.integers(0, 500), rng.integers(0, 500))
        X, Y, Z = _face_grid(arr, off)
        up = .25 if face == 'up' else (-.1 if face == 'down' else 0)
        if kind == 'bark': arr[:] = bark_rgb(X, Y, Z, face, up=up)
        elif kind == 'dark': arr[:] = bark_rgb(X, Y, Z, face, base=(58, 44, 32), moss=.2, up=up)
        elif kind == 'moss': arr[:] = bark_rgb(X, Y, Z, face, base=(72, 98, 44), moss=2.0, up=.4)
        elif kind == 'beard': arr[:] = hair_rgb(X, Y, Z, face)
        elif kind == 'leaf': arr[:] = leaf_rgb(X, Y, Z, face, (76, 118, 40))
        elif kind == 'leaf2': arr[:] = leaf_rgb(X, Y, Z, face, (130, 154, 52))
        elif kind == 'eye': pass
    return d


T.DECOR.update({'sy_bark': mk_decor('bark'), 'sy_dark': mk_decor('dark'), 'sy_moss': mk_decor('moss'), 'sy_beard': mk_decor('beard'),
                'sy_leaf': mk_decor('leaf'), 'sy_leaf2': mk_decor('leaf2'), 'sy_eye': mk_decor('eye')})


def body_fx(c, base=(88, 66, 46), moss=1.0):
    """ecorce continue d'un cube a l'autre (position dans l'espace) : le tronc ne montre pas les joints entre tranches"""
    w, h, d = c.size
    lo = np.array(c.frm, float)
    hi = lo + np.array((w, h, d), float)

    def f(F, rng, glow):
        for face in list(F):
            arr = F[face]
            fh, fw, _ = arr.shape
            tl, tr, bl, br = quad_corners(face, lo, hi)
            px = (np.arange(fw)[None, :] + .5) / fw
            py = (np.arange(fh)[:, None] + .5) / fh
            P = tl[None, None, :] + (tr - tl)[None, None, :] * px[:, :, None] + (bl - tl)[None, None, :] * py[:, :, None]
            up = .25 if face == 'up' else (-.1 if face == 'down' else 0)
            F[face][:] = bark_rgb(P[..., 0], P[..., 1], P[..., 2], face, base=base, moss=moss, up=up)
    return f


def fx_eye(col, big=False):
    """yeux enfonces : orbite sombre, iris qui luit (couche lumineuse)"""
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        for y in range(h):
            for x in range(w):
                put(n, x, y, (16, 10, 6), 1)
        for y in range(1, h - 1):
            for x in range(1, w - 1):
                put(n, x, y, col, .9)
                glow('north', x, y, col)
        if w >= 3 and h >= 3:
            put(n, w // 2, h // 2, (20, 12, 4), 1)
    return f


# ---------------------------------------------------------------- formes
def stub(name, x, y, z, w, h, d, mat='sy_bark', key=None, **kw):
    return C(name, x - w / 2, y, z - d / 2, w, h, d, mat, key=key, **kw)


def trunk(name, cx, cz, y0, y1, prof, mat='sy_bark', base=(88, 66, 46), n=None, moss=1.0):
    """colonne ovoide : prof = [(t, largeur, profondeur)] avec t de 0 (bas) a 1 (haut) ; tranches octogonales faites de 2 boites imbriquees"""
    n = n or max(3, int((y1 - y0) / 4))
    ts = np.array([p[0] for p in prof]); ws = np.array([p[1] for p in prof]); ds = np.array([p[2] for p in prof])
    ys = np.linspace(y0, y1, n + 1)
    out = []
    for i in range(n):
        t = (i + .5) / n
        w = float(np.interp(t, ts, ws)); d = float(np.interp(t, ts, ds))
        h = ys[i + 1] - ys[i]
        a = C(f'{name}{i}a', cx - w / 2, ys[i], cz - d * .38, int(round(w)), int(round(h)), int(round(d * .76)), mat, key=f'{name}{i}a')
        b = C(f'{name}{i}b', cx - w * .38, ys[i] + .03, cz - d / 2, int(round(w * .76)), int(round(h)) - 0, int(round(d)), mat, key=f'{name}{i}b')
        c = C(f'{name}{i}c', cx - w * .47, ys[i] + .06, cz - d * .47, max(1, int(round(w * .94))), int(round(h)), max(1, int(round(d * .94))), mat, key=f'{name}{i}c')
        for cube in (a, b, c):
            cube.fx = body_fx(cube, base=base, moss=moss)
        out += [a, b, c]
    return out


def down_chain(name, pivot, lens, ws, ds, rots, mat='sy_bark', key=None, fx=None):
    """chaine de segments qui pendent (bras, jambes, meches) : segment i = boite de hauteur lens[i] sous le pivot ; rots[i] = rotation (rx, ry, rz) relative"""
    px, py, pz = pivot
    pivs = []
    for L in lens:
        pivs.append((px, py, pz))
        py -= L
    g = None
    for i in range(len(lens) - 1, -1, -1):
        w, d, L = ws[i], ds[i], lens[i]
        cube = C(f'{name}s{i}', pivs[i][0] - w / 2, pivs[i][1] - L, pivs[i][2] - d / 2, w, L, d, mat, key=(key or name) + f's{i}')
        kids = [cube] + ([g] if g else [])
        g = Group(f'{name}' if i == 0 else f'{name}_{i}', pivs[i], rots[i], kids)
    return g


def up_branch(name, pivot, n, w0, seg, rz0, drz, rx0=0, drx=0, mat='sy_dark', leaf=2, rng=None, key=None, leaf_size=7):
    """branche qui part vers le haut en s'effilant, courbee par rotations successives ; feuilles au bout"""
    rng = rng or np.random.default_rng(3)
    px, py, pz = pivot
    pivs = [(px, py + i * seg, pz) for i in range(n)]
    g = None
    for i in range(n - 1, -1, -1):
        w = max(1, int(round(w0 * (1 - .75 * i / max(1, n - 1)))))
        kids = [C(f'{name}s{i}', pivs[i][0] - w / 2, pivs[i][1], pivs[i][2] - w / 2, w, int(seg), w, mat, key=(key or name[:6]) + f'b{i}')]
        if i == n - 1 and leaf:
            top = (pivs[i][0], pivs[i][1] + seg, pivs[i][2])
            for k in range(leaf):
                ls = leaf_size - k
                rot = (float(rng.integers(-40, 41)), float(rng.integers(0, 90)), float(rng.integers(-40, 41)))
                kids.append(C(f'{name}leaf{k}', top[0] - ls / 2, top[1] - 1 + k * 1.5, top[2] - ls / 2, ls, 2, ls, 'sy_leaf' if k % 2 == 0 else 'sy_leaf2', key=f'leaf{ls}{k % 2}', rot=rot,
                              pivot=(top[0], top[1] + k * 1.5, top[2])))
        if g:
            kids.append(g)
        g = Group(name if i == 0 else f'{name}_{i}', pivs[i], (rx0 if i == 0 else drx, 0, rz0 if i == 0 else drz), kids)
    return g


def hand(side, sg, pivot, size=1.0, fingers=4):
    """main : paume + 4 doigts a deux phalanges + pouce, qui pendent"""
    px, py, pz = pivot
    pw, ph = int(round(10 * size)), int(round(10 * size))
    kids = [C(f'palm{side}', px - pw / 2, py - ph, pz - 3.5 * size, pw, ph, int(round(7 * size)), 'sy_bark', key=f'palm{size}')]
    for k in range(fingers):
        fx_ = px - pw / 2 + 1.2 * size + k * (pw - 2.4 * size) / (fingers - 1)
        fl = [int(round(9 * size)), int(round(8 * size))]
        f = down_chain(f'finger{side}{k}', (fx_, py - ph, pz), fl, [3, 2], [3, 2], [(0.15 + 0.1 * (k % 2), 0, sg * (k - 1.5) * .06), (0.35, 0, 0)], mat='sy_dark', key=f'fing{size}')
        kids.append(f)
    th = down_chain(f'thumb{side}', (px + sg * pw / 2, py - ph * .3, pz - 1), [int(round(7 * size)), int(round(6 * size))], [3, 2], [3, 2],
                    [(0.0, 0, sg * 0.7), (0.0, 0, sg * .3)], mat='sy_dark', key=f'thumb{size}')
    kids.append(th)
    return Group(f'hand_{side}', pivot, (0, 0, 0), kids)


def roots(side, sg, cx, cz, n=6, rng=None, length=22):
    """racines aplaties qui rampent depuis le pied"""
    rng = rng or np.random.default_rng(5)
    out = []
    for k in range(n):
        ang = (k / n) * 2 * math.pi + (0.3 if sg > 0 else 0)
        dx, dz = math.cos(ang), math.sin(ang) * 1.1
        L = length * (0.6 + .5 * rng.random())
        segs = []
        for j in range(3):
            w = 4 - j
            ln = L / 3
            x = cx + dx * (j * ln + ln / 2); z = cz + dz * (j * ln + ln / 2)
            segs.append(C(f'root{side}{k}_{j}', x - (abs(dx) * ln + w * abs(dz)) / 2 - 0, 0 + 0.0, z - (abs(dz) * ln + w * abs(dx)) / 2,
                          max(1, int(round(abs(dx) * ln + w * abs(dz)))), max(1, 4 - j), max(1, int(round(abs(dz) * ln + w * abs(dx)))), 'sy_dark', key=f'rt{int(abs(dx) * ln + w)}{int(abs(dz) * ln + w)}{j}'))
        out += segs
    return out


def face_fx(c, lines, base=(88, 66, 46), moss=1.0):
    """ecorce continue + rides horizontales sur la face avant (front, joues, pattes d'oie)"""
    bf = body_fx(c, base=base, moss=moss)

    def f(F, rng, glow):
        bf(F, rng, glow)
        n = F['north']
        h, w, _ = n.shape
        for (fy, fl, fa) in lines:                 # (hauteur relative 0..1, longueur relative, intensite)
            y = int(fy * (h - 1))
            x0 = int((1 - fl) * w / 2)
            for x in range(x0, w - x0):
                put(n, x, y, (30, 20, 12), fa)
                if (x + y) % 3 == 0: put(n, x, y + 1, (30, 20, 12), fa * .5)
    return f


def arc_bough(name, pivot, yaw, n=6, w0=6, seg=9, rz0=.55, drz=-.3, fronds=(2, 4), flen=5, rng=None):
    """branche en parasol : monte puis retombe (rotations successives) ; des rideaux de feuilles pendent des segments choisis"""
    rng = rng or np.random.default_rng(4)
    px, py, pz = pivot
    pivs = [(px + i * seg, py, pz) for i in range(n)]
    g = None
    for i in range(n - 1, -1, -1):
        w = max(2, int(round(w0 * (1 - .6 * i / max(1, n - 1)))))
        kids = [C(f'{name}s{i}', pivs[i][0], py - w / 2, pz - w / 2, int(seg), w, w, 'sy_dark', key=f'{name[:5]}arc{i}')]
        cum = rz0 + i * drz
        if i in fronds:
            ex = pivs[i][0] + seg
            for k in range(2):
                ln = [9] * (flen + k)
                ws = [max(2, 5 - j // 2) for j in range(len(ln))]
                kids.append(down_chain(f'frond_{name}{i}{k}', (ex - k * 3, py, pz + (k - .5) * 3), ln, ws, [2] * len(ln),
                                       [(0, 0, -cum)] + [(0.04 * (1 if k else -1), 0, 0.03)] * (len(ln) - 1), mat='sy_leaf' if (i + k) % 2 else 'sy_leaf2', key=f'fr{(i + k) % 2}{flen + k}'))
        if g:
            kids.append(g)
        g = Group(name if i == 0 else f'{name}_{i}', pivs[i], (0, 0, rz0 if i == 0 else drz), kids)
    return Group(name + '_yaw', pivot, (0, yaw, 0), [g])


# ---------------------------------------------------------------- ANCIEN
def ancien():
    rng = np.random.default_rng(11)
    kids_body = []
    # tronc / torse : barrique large, ventre, poitrine, epaules
    prof = [(0.0, 30, 28), (.25, 40, 34), (.55, 48, 38), (.85, 50, 36), (1.0, 42, 30)]
    kids_body += trunk('tr', 0, 0, 58, 122, prof, n=18)
    for i, (x, y, z, s_) in enumerate(((-22, 96, -8, 7), (22, 84, 6, 8), (-12, 70, -17, 6), (14, 104, -15, 6), (0, 110, 17, 9), (-20, 66, 10, 6))):
        kids_body.append(stub(f'knot{i}', x, y, z, s_, s_, s_, 'sy_dark', key=f'knot{s_}'))
    kids_body.append(C('moss_l', -30, 116, -13, 18, 5, 26, 'sy_moss', key='mossS'))
    kids_body.append(C('moss_r', 12, 116, -13, 18, 5, 26, 'sy_moss', key='mossS'))
    kids_body.append(C('moss_b', -14, 120, -5, 28, 4, 16, 'sy_moss', key='mossB'))
    # ----- tete : grosse, ridee, arcades lourdes, nez long, yeux enfonces
    head = []
    sk = C('skull', -19, 116, -16, 38, 36, 32, 'sy_bark', key='skull')
    sk.fx = face_fx(sk, [(.30, .8, .5), (.38, .7, .45), (.46, .8, .4)], moss=1.4)
    head.append(sk)
    head.append(C('brow', -21, 140, -26, 42, 8, 12, 'sy_dark', key='brow'))
    head.append(C('brow_l', -21, 135, -25, 12, 6, 10, 'sy_bark', key='browS'))
    head.append(C('brow_r', 9, 135, -25, 12, 6, 10, 'sy_bark', key='browS'))
    fh_ = C('forehead', -16, 148, -20, 32, 6, 10, 'sy_bark', key='fore')
    fh_.fx = face_fx(fh_, [(.2, .8, .6), (.55, .7, .5)], moss=1.2)
    head.append(fh_)
    head.append(C('eye_l', -12, 131, -16.5, 7, 4, 2, 'sy_eye', key='eyeL', fx=fx_eye((235, 170, 40))))
    head.append(C('eye_r', 5, 131, -16.5, 7, 4, 2, 'sy_eye', key='eyeR', fx=fx_eye((235, 170, 40))))
    head.append(C('nose_a', -4, 120, -25, 8, 14, 6, 'sy_bark', key='noseA'))
    head.append(C('nose_b', -5, 113, -29, 10, 9, 8, 'sy_bark', key='noseB'))
    head.append(C('cheek_l', -23, 116, -21, 11, 13, 10, 'sy_bark', key='cheek'))
    head.append(C('cheek_r', 12, 116, -21, 11, 13, 10, 'sy_bark', key='cheek'))
    head.append(C('crown_base', -14, 152, -12, 28, 4, 24, 'sy_moss', key='cbase'))
    # barbe : trois rangs de meches ondulees qui descendent jusqu'au ventre
    for r, (yy, L0) in enumerate(((120, 30), (116, 38), (112, 46))):
        cols = [-19 + 5.4 * k for k in range(8)]
        for k, x in enumerate(cols):
            if r == 2 and k in (0, 7): continue
            z = -24 - r * 2.5 + abs(x) * .03
            L = L0 + int(rng.integers(-8, 16)) - (8 if k in (0, 7) else 0)
            segs = 5
            lens = [max(3, L // segs)] * segs
            ws = [8, 8, 7, 6, 4]
            ds = [7, 7, 6, 5, 3]
            rots = [(0.12 + .03 * (k % 2), 0, (k - 3.5) * .035)] + [(0.02, 0, (-1) ** (k + j) * .07) for j in range(segs - 1)]
            head.append(down_chain(f'beard{r}{k}', (x, yy - 2 * r, z), lens, ws, ds, rots, mat='sy_beard', key=f'bd{r}{k % 2}'))
    for sg, x in ((-1, -9), (1, 9)):
        head.append(down_chain(f'must{"l" if sg < 0 else "r"}', (x, 118, -27), [6, 6, 5], [7, 6, 4], [5, 5, 4], [(0.25, 0, sg * -.3), (0.1, 0, sg * -.25), (0.05, 0, 0)], mat='sy_beard', key='must'))
    # couronne : branches noueuses
    crng = np.random.default_rng(11)
    ncr = 7
    for i in range(ncr):
        a_ = (i + crng.uniform(-.45, .45)) / ncr * 2 * math.pi
        rr = crng.uniform(.3, 1.0)
        bx, bz = math.cos(a_) * 12 * rr, math.sin(a_) * 10 * rr
        n_ = int(crng.integers(2, 7))                      # hauteurs tres variees
        w_ = int(crng.integers(4, 9))
        lean = crng.uniform(.15, .9)                       # inclinaison vers l'exterieur
        twist = crng.uniform(-.35, .35)                    # courbure irreguliere
        head.append(up_branch(f'cr{i}', (bx, 152, bz), n_, w_, int(crng.integers(6, 10)),
                              -math.cos(a_) * lean + twist * .4, -math.cos(a_) * .22 + twist * .5,
                              rx0=math.sin(a_) * lean + twist * .3, drx=math.sin(a_) * .2 - twist * .4,
                              rng=crng, key='crw', leaf=int(crng.integers(1, 4)), leaf_size=int(crng.integers(6, 10))))
    head_g = Group('head', (0, 124, -2), (0.12, 0, 0), head)
    kids_body.append(head_g)
    # ----- bras longs et lourds, ecartes du corps, qui pendent jusqu'aux genoux
    for side, sg in (('l', -1), ('r', 1)):
        sh = (sg * 33, 114, 0)
        ar = down_chain(f'arm_{side}', sh, [20, 20], [15, 12], [15, 12], [(0, 0, sg * .08), (-.15, 0, sg * .04)], mat='sy_bark', key=f'arm{side}')
        inner = ar.children[-1]
        ar.children.append(C(f'elbow{side}', sh[0] - 9, 114 - 20 - 4, -9, 18, 9, 18, 'sy_dark', key='elbow'))
        fore = down_chain(f'forearm_{side}', (sh[0], 114 - 40, 0), [13, 12], [11, 9], [11, 9], [(-.05, 0, sg * .02), (-.08, 0, 0)], mat='sy_bark', key=f'fore{side}')
        fore.children[-1].children.append(hand(side, sg, (sh[0], 114 - 40 - 25, 0), 0.9))
        inner.children.append(fore)
        ar.children.append(C(f'shmoss{side}', sh[0] - 10, 110, -10, 20, 6, 20, 'sy_moss', key='shmoss'))
        for j in range(3):
            piv = (sh[0] + sg * (j - 1) * 3, 118, (j - 1) * 6)
            # tournees de 180 degres autour de la verticale : elles penchent vers l'exterieur
            ar.children.append(Group(f'shb{side}{j}_yaw', piv, (0, math.pi, 0), [up_branch(f'shb{side}{j}', piv, 5, 5, 7, sg * (.3 + j * .12), sg * .15, rng=rng, key='shb', leaf=3, leaf_size=8)]))
        kids_body.append(ar)
    for j in range(3):
        kids_body.append(up_branch(f'back{j}', ((j - 1) * 10, 100 + j * 6, 17), 6, 6, 8, 0, (j - 1) * .1, rx0=.45, drx=-.12, rng=rng, key='bk', leaf=3, leaf_size=8))
    # ----- jambes epaisses ecartees + racines
    legs = []
    for side, sg in (('l', -1), ('r', 1)):
        lg = down_chain(f'leg_{side}', (sg * 14, 62, 0), [30, 28], [18, 15], [18, 15], [(0, 0, sg * .03), (0, 0, sg * -.03)], mat='sy_bark', key=f'leg{side}')
        sh_ = lg.children[-1]
        sh_.children.append(C(f'foot{side}', sg * 14 - 12, 0, -17, 24, 6, 27, 'sy_dark', key='foot'))
        sh_.name = f'shin_{side}'
        legs.append(lg)
        for c in roots(side, sg, sg * 14, -3, rng=rng):
            lg.children.append(c)
    body_g = Group('body', (0, 62, 0), (-.12, 0, 0), kids_body)
    return [body_g] + legs


# ---------------------------------------------------------------- SAULE
def saule():
    rng = np.random.default_rng(23)
    kb = []
    prof = [(0.0, 20, 18), (.3, 26, 22), (.6, 24, 20), (.9, 22, 18), (1.0, 18, 16)]
    kb += trunk('tr', 0, 0, 74, 126, prof, base=(98, 76, 56), n=14)
    for k in range(12):
        t = k / 12
        a_ = t * 2.2 * math.pi
        y = 76 + t * 46
        kb.append(stub(f'twist{k}', math.cos(a_) * 11.5, y, math.sin(a_) * 10, 5, 7, 5, 'sy_dark', key='twA', rot=(0, math.degrees(-a_), 0)))
        a2 = a_ + math.pi
        kb.append(stub(f'twist2{k}', math.cos(a2) * 11.5, y, math.sin(a2) * 10, 5, 7, 5, 'sy_dark', key='twA', rot=(0, math.degrees(-a2), 0)))
    kb.append(C('moss_l', -16, 124, -9, 12, 4, 18, 'sy_moss', key='mossS'))
    kb.append(C('moss_r', 4, 124, -9, 12, 4, 18, 'sy_moss', key='mossS'))
    head = []
    head.append(C('neck', -4, 124, -4, 8, 6, 8, 'sy_bark', key='neck'))
    sk = C('skull', -10, 128, -10, 20, 24, 20, 'sy_bark', key='skullS')
    sk.fx = face_fx(sk, [(.25, .8, .5), (.33, .7, .4)], base=(98, 76, 56), moss=1.2)
    head.append(sk)
    head.append(C('brow', -11, 142, -14, 22, 5, 7, 'sy_dark', key='browW'))
    head.append(C('eye_l', -6.5, 136, -10.5, 4, 3, 2, 'sy_eye', key='eyeLs', fx=fx_eye((170, 235, 90))))
    head.append(C('eye_r', 2.5, 136, -10.5, 4, 3, 2, 'sy_eye', key='eyeRs', fx=fx_eye((170, 235, 90))))
    head.append(C('nose', -2, 127, -15, 4, 12, 5, 'sy_bark', key='noseW'))
    head.append(C('chin', -4, 124, -12, 8, 6, 6, 'sy_bark', key='chinW'))
    for k, x in enumerate((-7, -4, -1, 2, 5, 8)):
        L = 18 + int(rng.integers(0, 14))
        head.append(down_chain(f'beard{k}', (x - .5, 129, -12), [L // 3] * 3, [3, 3, 2], [3, 2, 2], [(0.1, 0, (k - 2.5) * .05)] + [(0.03, 0, (-1) ** k * .06)] * 2,
                               mat='sy_beard', key=f'bs{k % 2}'))
    # couronne en parasol : 9 branches qui montent, puis retombent en rideaux de feuilles
    for i in range(9):
        head.append(arc_bough(f'cr{i}', (0, 150, 0), i / 9 * 360, n=6, w0=6, seg=9, rz0=.55, drz=-.34, fronds=(2, 4), flen=5 + (i % 2), rng=rng))
    # petites branches feuillues au sommet
    for i in range(5):
        a_ = i / 5 * 2 * math.pi
        head.append(up_branch(f'top{i}', (math.cos(a_) * 4, 150, math.sin(a_) * 4), 3, 4, 7, -math.cos(a_) * .3, -math.cos(a_) * .2, rx0=math.sin(a_) * .3, drx=math.sin(a_) * .2, rng=rng, key='topw', leaf=2, leaf_size=8))
    head_g = Group('head', (0, 126, 0), (0.10, 0, 0), head)
    kb.append(head_g)
    # bras tres longs, tordus, ecartes ; quelques rideaux de feuilles
    for side, sg in (('l', -1), ('r', 1)):
        sh = (sg * 17, 120, 0)
        ar = down_chain(f'arm_{side}', sh, [22, 22], [8, 7], [8, 7], [(0, 0, sg * .14), (-.2, 0, sg * .06)], mat='sy_bark', key=f'armS{side}')
        inner = ar.children[-1]
        ar.children.append(C(f'elbow{side}', sh[0] - 5, 120 - 22 - 3, -5, 10, 6, 10, 'sy_dark', key='elbowS'))
        fore = down_chain(f'forearm_{side}', (sh[0], 120 - 44, 0), [14, 12], [6, 5], [6, 5], [(-.04, 0, 0), (-.08, 0, 0)], mat='sy_bark', key=f'foreS{side}')
        fore.children[-1].children.append(hand(side, sg, (sh[0], 120 - 44 - 26, 0), .7))
        inner.children.append(fore)
        for k in range(3):
            yy = 120 - 10 - k * 22
            ar.children.append(down_chain(f'frond_a{side}{k}', (sh[0] + sg * 2, yy, (k % 2 - .5) * 6), [9, 9, 9, 8], [4, 4, 3, 3], [2, 2, 2, 2],
                                          [(0.0, 0, sg * -.16)] + [(0.05, 0, sg * .03)] * 3, mat='sy_leaf' if k % 2 else 'sy_leaf2', key=f'frondA{k % 2}'))
        kb.append(ar)
    for j in range(4):
        kb.append(down_chain(f'frond_b{j}', ((j - 1.5) * 6, 124 - j * 3, 11), [9, 9, 9, 8, 7], [5, 5, 4, 4, 3], [2, 2, 2, 2, 2], [(0.15, 0, 0)] + [(0.03, 0, 0)] * 4,
                             mat='sy_leaf' if j % 2 else 'sy_leaf2', key=f'frondB{j % 2}'))
    legs = []
    for side, sg in (('l', -1), ('r', 1)):
        lg = down_chain(f'leg_{side}', (sg * 8, 78, 0), [40, 36], [10, 8], [10, 8], [(0.0, 0, sg * .03), (0.0, 0, sg * -.03)], mat='sy_bark', key=f'legS{side}')
        lg.children[-1].name = f'shin_{side}'
        lg.children[-1].children.append(C(f'foot{side}', sg * 8 - 7, 0, -12, 14, 5, 20, 'sy_dark', key='footS'))
        for c in roots(side, sg, sg * 8, -3, n=5, rng=rng, length=16):
            lg.children.append(c)
        legs.append(lg)
    body_g = Group('body', (0, 78, 0), (-.05, 0, 0), kb)
    return [body_g] + legs


def racines():
    """Racines qui jaillissent du sol et se referment sur une creature (sort du Sylvgarde)."""
    rng = np.random.default_rng(7)
    kids = []
    N = 5
    for k in range(N):
        ang = 2 * math.pi * k / N
        R = 15
        n = 4 + int(rng.integers(0, 2))
        lean = -0.02 + 0.06 * float(rng.random())
        br = up_branch(f'rac{k}', (R, 0, 0), n, 6, 10, lean, 0.22, mat='sy_dark', leaf=0, key=f'rac{k}')
        kids.append(Group(f'ring{k}', (0, 0, 0), (0, ang, 0), [br]))
    for k in range(N):                                   # bourrelet de terre soulevee
        ang = 2 * math.pi * (k + 0.5) / N
        x, z = 12 * math.cos(ang), 12 * math.sin(ang)
        kids.append(C(f'sol{k}', x - 5, 0, z - 5, 10, 4, 10, 'sy_bark', key='sol'))
    kids += roots('c', 1, 0, 0, n=7, rng=rng, length=17)  # racines rampantes au sol
    return [Group('body', (0, 0, 0), (0, 0, 0), kids)]


ALL = {
    'sylvestre_ancien': dict(fn=ancien, m=5.4, label='Sylvestre ancien (barbu)'),
    'sylvestre_saule': dict(fn=saule, m=5.2, label='Sylvestre saule (maigre, rideaux de feuilles)'),
    'racines': dict(fn=racines, m=1.05, label='Racines qui emprisonnent une creature'),
}


# ---------------------------------------------------------------- mise a l'echelle / export
def extents(groups):
    lo = np.array([1e9] * 3); hi = np.array([-1e9] * 3)
    for c, chain in walk(groups):
        w, h, d = c.size
        l = np.array(c.frm) - c.inflate
        u = np.array(c.frm) + np.array((w, h, d)) + c.inflate
        Rc = rot_mat(c.rot)
        for x in (l[0], u[0]):
            for y in (l[1], u[1]):
                for z in (l[2], u[2]):
                    p = Rc @ (np.array((x, y, z)) - np.array(c.pivot)) + np.array(c.pivot)
                    for g in chain:
                        p = rot_mat(g.rot) @ (p - np.array(g.pivot)) + np.array(g.pivot)
                    lo = np.minimum(lo, p); hi = np.maximum(hi, p)
    return lo, hi


def rad2deg_groups(groups):
    """les rotations ecrites en radians dans les generateurs sont converties en degres (convention de trollcore)"""
    for g in groups:
        g.rot = tuple(math.degrees(v) for v in g.rot)
        for ch in g.children:
            if isinstance(ch, Group):
                rad2deg_groups([ch])


def build(name):
    T._N[0] = 0
    spec = ALL[name]
    groups = spec['fn']()
    rad2deg_groups(groups)
    lo, hi = extents(groups)
    f = spec['m'] * 100 / UNIT_CM / hi[1]
    scale_all(groups, f)
    normalize(groups)
    unique_names(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    return groups, cubes, W, H, tex, glow, f


def sheet(path, names=None, views=(0, 40, 90)):
    from PIL import Image
    rows = []
    for name in names or ALL:
        groups, cubes, W, H, tex, glow, f = build(name)
        lo, hi = extents(groups)
        sc = 560 / hi[1]
        ims = [render(groups, tex, y, pitch=-8, W=470, H=620, scale=sc) for y in views]
        rows.append(np.concatenate(ims, axis=1))
        print(name, 'cubes', len(cubes), 'texture', W, H, 'haut(m)', round(hi[1] * UNIT_CM / 100, 2), 'large(m)', round((hi[0] - lo[0]) * UNIT_CM / 100, 2))
    Image.fromarray(np.concatenate(rows, axis=0)).save(path)


def generate(outdir):
    import json, os, bbimport
    from PIL import Image
    import models
    os.makedirs(outdir, exist_ok=True)
    meta = {}
    ren = {}
    for name, spec in ALL.items():
        groups, cubes, W, H, tex, glow, f = build(name)
        Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, f'{name}.png'))
        export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(outdir, name + '.bbmodel'), rename={'leg_left': 'leg_l'})
        geo['height'] = models.geo_height(geo)
        json.dump(geo, open(os.path.join(outdir, name + '.json'), 'w'), separators=(',', ':'))
        if glow[..., 3].any():
            Image.fromarray(glow, 'RGBA').save(os.path.join(outdir, name + '_glow.png'))
        lo, hi = extents(groups)
        meta[name] = dict(nom=spec['label'], hauteur_m=round(hi[1] * UNIT_CM / 100, 2), cubes=len(cubes), pieces=len(geo['parts']), texture=f'{W}x{H}')
        print(name, meta[name])
    json.dump(meta, open(os.path.join(outdir, 'sylvestres.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == 'gen':
        generate(sys.argv[2] if len(sys.argv) > 2 else '/tmp/sylvestres')
    sheet('/tmp/sylv.png')
