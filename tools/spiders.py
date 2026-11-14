"""Araignee geante (style du film : gris-noir, velue, abdomen bulbeux, visage de masque osseux). Fichiers 3D uniquement.
Memes conventions que goblins.py : y vers le haut, sol y=0, avant = -z, cote -x = 'left'. 1 unite = 3 cm.
Taille : 4,5 m d'envergure, 2,5 m de haut.
Pieces nommees pour une animation future (chaque patte est une chaine de groupes imbriques) :
  body (carapace + tete), abdomen, head, fang_l/fang_r (+ fang_*_1..), palp_l/palp_r,
  leg_1l .. leg_4r (hanche/lacet) -> leg_Nx_a (cuisse) -> _b (tibia) -> _c (metatarse) -> _d (tarse)."""
import math
import numpy as np
import trollcore as T
from trollcore import *
from orcs import C, spike, unique_names

UNIT_CM = 3.0
TARGET_H = 250 / UNIT_CM          # hauteur visee, en unites
TARGET_W = 450 / UNIT_CM          # envergure visee

# ---------------------------------------------------------------- materiaux
MAT = {
    'sp_chit': dict(base=(58, 60, 66), n1=7, n2=9, tint=(1, 1, 1.08)),
    'sp_abd': dict(base=(54, 55, 61), n1=7, n2=9, tint=(1, .98, 1.06)),
    'sp_leg': dict(base=(54, 55, 60), n1=6, n2=7, tint=(1, 1, 1.06)),
    'sp_hair': dict(base=(28, 28, 32), n1=4, n2=4, tint=(1, 1, 1.05)),
    'sp_fang': dict(base=(122, 110, 86), n1=7, n2=6, tint=(1, .96, .82)),
    'sp_pink': dict(base=(112, 74, 78), n1=8, n2=8, tint=(1, .8, .8)),
    'sp_eye': dict(base=(16, 20, 14), n1=2, n2=2, tint=(1, 1, 1)),
    'sp_mask': dict(base=(66, 70, 74), n1=7, n2=8, tint=(1, 1.02, 1.06)),
    'sp_dark': dict(base=(12, 11, 14), n1=3, n2=3, tint=(1, 1, 1)),
}
T.MATS.update(MAT)


def _veins(arr, rng, col, a, n):
    h, w, _ = arr.shape
    for _ in range(n):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        dx, dy = (1, 0) if rng.random() < .5 else (0, 1)
        for _ in range(int(rng.integers(3, 9))):
            put(arr, x, y, col, a)
            if rng.random() < .4:
                x += dx
            if rng.random() < .4:
                y += dy
            x += int(rng.integers(0, 2)) * dx
            y += int(rng.integers(0, 2)) * dy


def _unedge(arr):
    h, w, _ = arr.shape
    if w >= 3 and h >= 3:                            # annule le liseré sombre de base_face : les tranches doivent se fondre
        arr[0, :] /= .9; arr[-1, :] /= .88; arr[:, 0] /= .92; arr[:, -1] /= .9


def d_chit(arr, face, rng):
    """chitine du film : gris poussiereux marbre de plus clair/plus sombre, reflet bleute, grains et fissures"""
    h, w, _ = arr.shape
    _unedge(arr)
    m = arr.reshape(-1, 3).mean(0)
    lite = tuple(np.clip(m * 1.45 + np.array((2, 4, 10)), 0, 255))
    dark = tuple(m * .55)
    warm = tuple(np.clip(m * np.array((1.35, 1.15, 1.1)) + 6, 0, 255))
    for _ in range(max(1, h * w // 20)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), lite if rng.random() < .5 else dark, .28, rng)
    for _ in range(h * w // 40):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), lite, .55)
    if rng.random() < .35:
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), warm, .30, rng)
    _veins(arr, rng, dark, .55, max(1, h * w // 90))


def d_abd(arr, face, rng):
    """abdomen : gris sombre marbre, reseau de veines plus claires, quelques taches rougeatres (comme la statuette)"""
    h, w, _ = arr.shape
    _unedge(arr)
    m = arr.reshape(-1, 3).mean(0)
    lite = tuple(np.clip(m * 1.5 + np.array((4, 4, 12)), 0, 255))
    dark = tuple(m * .5)
    red = tuple(np.clip(m * np.array((1.9, .95, .95)) + np.array((10, 0, 0)), 0, 255))
    for _ in range(max(1, h * w // 16)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), lite if rng.random() < .45 else dark, .3, rng)
    for _ in range(h * w // 34):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), lite, .5)
    _veins(arr, rng, lite, .45, max(2, h * w // 70))
    if rng.random() < .22:
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 4)), red, .38, rng)


def d_leg(arr, face, rng):
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    lite = tuple(np.clip(m * 1.5 + 6, 0, 255))
    dark = tuple(m * .5)
    for _ in range(max(1, h * w // 14)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), lite if rng.random() < .5 else dark, .5)
    if w >= 6:                                    # anneaux plus clairs aux articulations
        for x in (1, w - 2):
            arr[:, x] = arr[:, x] * .72
    _veins(arr, rng, dark, .5, max(1, h * w // 60))


def d_hair(arr, face, rng):
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    for _ in range(h * w // 3 + 1):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), tuple(np.clip(m * 1.8 + 10, 0, 255)), .5)


def d_fang(arr, face, rng):
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    for _ in range(h * w // 5 + 1):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), tuple(m * .62), .45)
    if h >= 3:
        arr[-1, :] = arr[-1, :] * .8


def d_mask(arr, face, rng):
    """masque osseux du visage : os terne, sutures et creux plus sombres"""
    d_chit(arr, face, rng)
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    if rng.random() < .8 and w >= 3:
        line(arr, int(rng.integers(0, w)), 0, 0, 1, h, tuple(m * .55), .55)


def d_flat(arr, face, rng):
    pass


T.DECOR.update({'sp_chit': d_chit, 'sp_abd': d_abd, 'sp_leg': d_leg, 'sp_hair': d_hair, 'sp_fang': d_fang,
                'sp_pink': d_abd, 'sp_eye': d_flat, 'sp_mask': d_mask, 'sp_dark': d_flat})


def fx_eye(col=(200, 170, 40), rim=(40, 36, 20), big=True):
    """oeil d'araignee : bille noire luisante, iris verdatre qui luit, reflet"""
    def f(F, rng, glow):
        for face in ('north', 'up', 'east', 'west'):
            a = F[face]
            a *= .9
        n = F['north']
        h, w, _ = n.shape
        for y in range(h):
            for x in range(w):
                corner = x in (0, w - 1) and y in (0, h - 1)
                if corner:
                    continue
                edge = x in (0, w - 1) or y in (0, h - 1)
                put(n, x, y, rim if edge else (14, 18, 12), 1)
        if w >= 3 and h >= 3:
            for y in range(1, h - 1):
                for x in range(1, w - 1):
                    put(n, x, y, col, .62)
                    glow('north', x, y, tuple(int(c * .85) for c in col))
            put(n, 1, 1, (250, 250, 220), 1)
            glow('north', 1, 1, (250, 250, 210))
        else:
            for y in range(h):
                for x in range(w):
                    put(n, x, y, col, .7)
                    glow('north', x, y, tuple(int(c * .8) for c in col))
    return f


# ---------------------------------------------------------------- outils de forme
def octa(name, cx, cy, z0, w, h, d, mat, key_extra='', bands=7):
    """tranche de volume arrondi : section elliptique faite de bandes horizontales empilees (largeur de chaque bande = ellipse)"""
    w = max(2, int(round(w))); h = max(2, int(round(h))); d = max(1, int(d))
    edges = np.sin(np.linspace(-math.pi / 2, math.pi / 2, bands + 1))        # bandes plus fines vers le haut et le bas : section bien ronde
    out = []
    for i in range(len(edges) - 1):
        y0, y1 = cy + edges[i] * h / 2, cy + edges[i + 1] * h / 2
        yc = (edges[i] + edges[i + 1]) / 2
        bw = max(2, int(round(w * math.sqrt(max(0.0, 1 - yc * yc)) * .98 + 1)))
        bh = max(1, int(round(y1 - y0)))
        out.append(C(f'{name}{i}', cx - bw / 2, y0, z0, bw, bh, d, mat, key=f'{name}{key_extra}{i}'))
    return out


def ovoid(name, cx, cy, z0, z1, wmax, hmax, n, peak=.52, mat='sp_abd', pw=.85, wmin=3, tail=1.0):
    """volume ovoide : n tranches d'une ellipse (plus large a 'peak'), du z0 (avant) au z1 (arriere)"""
    kids = []
    zs = np.linspace(z0, z1, n + 1)
    for i in range(n):
        t = (i + .5) / n
        u = (t - peak) / (peak if t < peak else (1 - peak) * tail)
        r = max(0.0, 1 - u * u) ** .5
        r = r ** pw
        kids += octa(f'{name}{i}', cx, cy, zs[i], wmin + (wmax - wmin) * r, wmin + (hmax - wmin) * r, zs[i + 1] - zs[i], mat)
    return kids


def bristles(L, t, sg, n, mat, rng, ln=5, tilt=38, top=True, both=True):
    """poils raides le long d'un segment (repere du segment : part de x=0 vers sg*L, epaisseur t) ; renvoie des cubes"""
    out = []
    for i in range(n):
        x = sg * (L * (i + .7) / (n + .4))
        tl = tilt + float(rng.integers(-8, 9))
        rx = float(rng.integers(-28, 29))
        out.append(spike(f'ha{i}', x, t / 2 - .5, 0, ln + int(rng.integers(0, 3)), 1, (rx, 0, sg * tl), mat))
        if both and i % 2 == 0:
            out.append(spike(f'hb{i}', x, 0, (t / 2 - .5) * (1 if i % 4 == 0 else -1), ln - 1, 1, ((50 if i % 4 == 0 else -50), 0, sg * 20), mat))
    return out


def limb(name, sg, hip, yaw, angs, lens, ths, mat='sp_leg', hair=1, rng=None, claw=True):
    """chaine de segments imbriques partant d'une hanche. angs : angles absolus (deg, + = vers le haut) de chaque segment dans le plan de la patte ;
    yaw : angle vers l'avant (deg) de ce plan autour de la verticale. Segment 0 = hanche, dernier = tarse."""
    rng = rng or np.random.default_rng(1)
    n = len(lens)
    px, py, pz = hip
    pivs = []
    for i in range(n):
        pivs.append((px, py, pz))
        px += sg * lens[i]               # fin du segment dans le repere "a plat" (les rotations sont portees par les groupes)
    names = [name + '_' + 'xabcdefg'[i] if i else name + '_x' for i in range(n)]
    g = None
    for i in range(n - 1, -1, -1):
        L, t = lens[i], ths[i]
        x0 = pivs[i][0]
        xa, xb = (x0, x0 + L) if sg > 0 else (x0 - L, x0)
        kids = [C(f'{name}s{i}', xa, py - t / 2, pz - t / 2, L, t, t, mat, key=f'{name[:5]}seg{i}')]
        if i in (2, 3):
            kids.append(C(f'{name}knob{i}', x0 - (t + 2) / 2 if True else 0, py - (t + 2) / 2, pz - (t + 2) / 2, t + 2, t + 2, t + 2, 'sp_leg', key=f'{name[:5]}kn{i}'))
        if hair and i in (1, 2, 3):
            for b in bristles(L, t, sg, hair * (4 if i == 1 else 3), 'sp_hair', rng, ln=4 + (i == 1), top=True):
                b.frm = (b.frm[0] + x0, b.frm[1] + py, b.frm[2] + pz)
                b.pivot = (b.pivot[0] + x0, b.pivot[1] + py, b.pivot[2] + pz)
                kids.append(b)
        if i == n - 1 and claw:
            kids.append(spike(f'{name}claw', xb, py - 1, pz, 5, 2, (0, 0, sg * -20 + 0), 'sp_fang', d=2))
        if g:
            kids.append(g)
        rel = angs[i] - (angs[i - 1] if i else 0)
        g = Group(names[i], pivs[i], (0, 0, sg * rel), kids)
    g.name = name
    return Group(name + '_yaw', hip, (0, sg * yaw, 0), [g])


def solve_leg(hip_y, lens, a_fixed, last_free_idx, ground=0.0):
    """trouve l'angle du segment last_free_idx pour que le bout touche le sol"""
    def tip(a):
        ang = list(a_fixed)
        ang[last_free_idx] = a
        return hip_y + sum(L * math.sin(math.radians(x)) for L, x in zip(lens, ang))
    lo, hi = -89.0, 20.0
    for _ in range(60):
        mid = (lo + hi) / 2
        if tip(mid) > ground:
            hi = mid
        else:
            lo = mid
    return (lo + hi) / 2



# ---------------------------------------------------------------- texture continue (les tranches du corps se fondent)
def _vnoise(x, y, z, seed):
    """bruit de valeur 3D lisse (x, y, z : tableaux numpy en unites du modele)"""
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


def body_fx(c, kind):
    """texture du corps pilotee par la position dans l'espace : les faces voisines (tranches) se raccordent, comme une seule peau"""
    base = np.array(T.MATS['sp_abd' if kind == 'ab' else 'sp_chit']['base'], float)
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
            X, Y, Z = P[..., 0], P[..., 1], P[..., 2]
            n1 = _vnoise(X / 16, Y / 16, Z / 16, 1)
            n2 = _vnoise(X / 5, Y / 5, Z / 5, 2)
            n3 = _vnoise(X / 1.7, Y / 1.7, Z / 1.7, 3)
            lum = 1 + (n1 - .5) * .42 + (n2 - .5) * .30 + (n3 - .5) * .16
            col = base[None, None, :] * lum[..., None] * np.array((1, 1.0, 1.07))[None, None, :] * SHADE[face]
            # veines : lignes claires la ou un bruit "ridge" s'approche de 0,5
            r = np.abs(_vnoise(X / 7, Y / 7, Z / 7, 4) - .5)
            vein = np.clip(1 - r / .035, 0, 1) * (.35 if kind == 'ab' else .2)
            col = col * (1 - vein[..., None]) + np.array((130, 130, 144))[None, None, :] * vein[..., None]
            # taches rougeatres, rares
            red = np.clip((_vnoise(X / 11, Y / 11, Z / 11, 5) - .84) / .10, 0, 1) * (.32 if kind == 'ab' else .15)
            col = col * (1 - red[..., None]) + np.array((128, 66, 70))[None, None, :] * red[..., None]
            F[face][:] = col
    return f

# ---------------------------------------------------------------- l'araignee
def spider(skin=None):
    rng = np.random.default_rng(7)
    root = []

    # ----- carapace (cephalothorax) : tranches de l'avant (-z) vers l'arriere
    HY = 56.0
    thorax = ovoid('th', 0, HY, -44, -8, 30, 25, 9, peak=.62, mat='sp_chit', pw=.7, wmin=10, tail=.9)
    body_kids = list(thorax)
    # bosse dorsale et sillon
    body_kids.append(C('hump', -5, HY + 9, -30, 10, 5, 14, 'sp_chit'))
    body_kids.append(C('groove', -1, HY + 13.4, -32, 2, 1, 10, 'sp_dark'))
    for i, (x, z) in enumerate(((-10, -34), (10, -34), (-12, -22), (12, -22))):
        body_kids.append(spike(f'tsp{i}', x, HY + 8, z, 5, 2, (0, 0, -x / 3), 'sp_hair'))

    # ----- tete / visage de masque
    head = []
    head.append(C('face', -9, HY - 8, -49, 18, 18, 7, 'sp_mask'))
    head.append(C('brow', -11, HY + 6, -50, 22, 3, 7, 'sp_mask'))                # arcade
    head.append(C('nasal_l', -4, HY - 1, -51, 3, 5, 2, 'sp_dark'))                # creux nasal, comme un crane
    head.append(C('nasal_r', 1, HY - 1, -51, 3, 5, 2, 'sp_dark'))
    head.append(C('cheek_l', -13, HY - 5, -47, 5, 9, 6, 'sp_mask'))
    head.append(C('cheek_r', 8, HY - 5, -47, 5, 9, 6, 'sp_mask'))
    head.append(C('mouth', -6, HY - 14, -51, 12, 5, 4, 'sp_dark'))                # bouche
    for i in range(5):                                                            # dents fines
        head.append(spike(f'tooth{i}', -5 + i * 2.5, HY - 13.6, -51.3, 3, 1, (180, 0, 0), 'sp_fang', d=1))
    # yeux : deux gros yeux frontaux, deux lateraux, quatre petits au-dessus
    eyes = [('eye_a_l', -7.5, HY + 1.0, 6, 6, (190, 165, 40)), ('eye_a_r', 1.5, HY + 1.0, 6, 6, (190, 165, 40)),
            ('eye_b_l', -12.5, HY + 2.0, 4, 4, (160, 150, 40)), ('eye_b_r', 8.5, HY + 2.0, 4, 4, (160, 150, 40)),
            ('eye_c_l', -4.5, HY + 6.5, 2, 2, (150, 140, 40)), ('eye_c_r', 2.5, HY + 6.5, 2, 2, (150, 140, 40)),
            ('eye_d_l', -8.5, HY + 6.5, 2, 2, (140, 130, 40)), ('eye_d_r', 6.5, HY + 6.5, 2, 2, (140, 130, 40))]
    for nm, x, y, w, h, col in eyes:
        head.append(C(nm, x, y, -51.2, w, h, 2, 'sp_eye', fx=fx_eye(col), key=nm))
    head_g = Group('head', (0, HY, -44), (0, 0, 0), head)

    # ----- chelicerees (crocs) : chaine qui se courbe vers l'interieur et vers l'arriere
    def fang(side, sg):
        n, step = 7, 4.5
        px, py, pz = sg * 5.0, HY - 13.0, -48.0
        pivs = [(px, py - i * step, pz) for i in range(n)]
        g = None
        for i in range(n - 1, -1, -1):
            w = max(1, int(round(6 * (1 - i / n) ** .8)))
            kids = [C(f'fang{side}{i}', px - w / 2, pivs[i][1] - step, pz - w / 2, w, int(step), w, 'sp_fang' if i > 0 else 'sp_mask', key=f'fg{i}')]
            if g:
                kids.append(g)
            g = Group(f'fang_{side}' if i == 0 else f'fang_{side}_{i}', pivs[i], (-9 if i else 4, 0, -sg * (5 if i else 0)), kids)
        return g
    head_g.children += [fang('l', -1), fang('r', 1)]
    # pedipalpes : petits membres devant la bouche
    for side, sg in (('l', -1), ('r', 1)):
        pal = limb('palp_' + side, sg, (sg * 8, HY - 8, -46), 70, [-10, -30, -60], [4, 10, 9], [4, 4, 3], mat='sp_leg', hair=0, rng=rng, claw=False)
        head_g.children.append(pal)
    body_kids.append(head_g)
    # ornements de la carapace : pointes dorsales et collier de poils
    for i in range(6):
        x = -8 + i * 3.2
        body_kids.append(spike(f'nape{i}', x, HY + 9, -12, 5, 1, (-25, 0, 0), 'sp_hair'))
    body = Group('body', (0, HY, -26), (0, 0, 0), body_kids)

    # ----- abdomen : gros ovoide bulbeux, redresse vers l'arriere
    CY, WM, HM, Z0, Z1, PK, PW = HY - 2, 64, 58, -6, 68, .5, .75
    ab = ovoid('ab', 0, CY, Z0, Z1, WM, HM, 20, peak=PK, mat='sp_abd', pw=PW, wmin=5, tail=1.1)

    def prof(t):
        u = (t - PK) / (PK if t < PK else (1 - PK) * 1.1)
        return max(0.0, 1 - u * u) ** .5 ** 1 ** 1 if False else (max(0.0, 1 - u * u) ** .5) ** PW
    for i in range(26):                                                       # poils sur le dos et les flancs
        t = float(rng.uniform(.1, .95))
        z = Z0 + t * (Z1 - Z0)
        r = prof(t)
        x = float(rng.uniform(-.8, .8)) * WM / 2 * r
        yy = CY + HM / 2 * r * math.sqrt(max(0.0, 1 - (x / (WM / 2 * r + 1e-6)) ** 2)) - 1
        ab.append(spike(f'abh{i}', x, yy, z, 5, 1, (float(rng.integers(-25, 26)), 0, x * .9), 'sp_hair'))
    ab.append(C('spin_l', -6, CY - 1, Z1 - 1, 4, 4, 5, 'sp_chit'))            # filieres
    ab.append(C('spin_r', 2, CY - 1, Z1 - 1, 4, 4, 5, 'sp_chit'))
    ab.append(C('spin_m', -2, CY + 1, Z1, 4, 3, 4, 'sp_pink'))
    ab.append(C('sac', -10, CY - HM / 2 - 1.5, 16, 20, 3, 30, 'sp_pink'))     # dessous plus clair, rosatre
    abdomen = Group('abdomen', (0, HY + 2, -8), (-12, 0, 0), ab)

    root.append(Group('body_all', (0, HY, -20), (0, 0, 0), [body, abdomen]))
    root[0].name = 'body_root'

    # ----- huit pattes
    hip_y = 53.0
    hips_z = [-38.0, -30.0, -22.0, -14.0]
    yaws = [52.0, 24.0, -16.0, -42.0]                 # + = vers l'avant
    mult = [.96, 1.0, 1.0, 1.1]
    for i in range(4):
        for side, sg in (('l', -1), ('r', 1)):
            r = np.random.default_rng(100 + i * 2 + (sg > 0))
            m = mult[i]
            lens = [9, 38 * m, 47 * m, 42 * m, 15]
            ths = [8, 7, 6, 5, 4]
            jit = float(r.integers(-3, 4))
            angs = [16, 60 + jit, -30 + jit * .5, -76, -62]
            angs[3] = solve_leg(hip_y, lens, angs, 3)
            leg_ = limb(f'leg_{i + 1}{side}', sg, (sg * 12.0, hip_y, hips_z[i]), yaws[i], angs, lens, ths, rng=r)
            root.append(leg_)
    return root


ALL = {'araignee_geante': dict(fn=spider, label='Araignée géante', skins=['film'])}


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


def build(name='araignee_geante', skin=None):
    T._N[0] = 0
    spec = ALL[name]
    groups = spec['fn'](skin)
    # normalise la taille : hauteur 2,5 m (les pattes fixent aussi l'envergure)
    from orcs import scale_all
    lo, hi = extents(groups)
    f = TARGET_H / hi[1]
    scale_all(groups, f)
    normalize(groups)
    unique_names(groups)
    cubes = [c for c, _ in walk(groups)]
    import re
    for c in cubes:
        m = re.match(r'^(ab|th)\d', c.name)
        if m:
            c.fx = body_fx(c, m.group(1))
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    return groups, cubes, W, H, tex, glow, f


def sheet(path, views=(180, 135, 90, 40, 0)):
    from PIL import Image
    groups, cubes, W, H, tex, glow, f = build()
    ims = [render(groups, tex, y, pitch=-12, W=520, H=420, scale=2.9) for y in views]
    top = render(groups, tex, 180, pitch=-86, W=520, H=420, scale=2.9)
    row1 = np.concatenate(ims[:3], axis=1)
    row2 = np.concatenate(ims[3:] + [top], axis=1)
    Image.fromarray(np.concatenate([row1, row2], axis=0)).save(path)
    lo, hi = extents(groups)
    print('cubes', len(cubes), 'texture', W, H, 'echelle', round(f, 3), 'envergure(m)', round((hi[0] - lo[0]) * UNIT_CM / 100, 2), 'haut(m)', round(hi[1] * UNIT_CM / 100, 2),
          'long(m)', round((hi[2] - lo[2]) * UNIT_CM / 100, 2))


def generate(outdir):
    import json, os, bbimport
    from PIL import Image
    import models
    os.makedirs(outdir, exist_ok=True)
    meta = {}
    for name, spec in ALL.items():
        groups, cubes, W, H, tex, glow, f = build(name)
        Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, f'{name}.png'))
        export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(outdir, name + '.bbmodel'))
        geo['height'] = models.geo_height(geo)
        json.dump(geo, open(os.path.join(outdir, name + '.json'), 'w'), separators=(',', ':'))
        if glow[..., 3].any():
            Image.fromarray(glow, 'RGBA').save(os.path.join(outdir, name + '_glow.png'))
        lo, hi = extents(groups)
        meta[name] = dict(nom=spec['label'], envergure_m=round((hi[0] - lo[0]) * UNIT_CM / 100, 2), hauteur_m=round(hi[1] * UNIT_CM / 100, 2),
                          cubes=len(cubes), pieces=len(geo['parts']), texture=f'{W}x{H}')
        print(name, meta[name])
    json.dump(meta, open(os.path.join(outdir, 'araignee.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == 'gen':
        generate(sys.argv[2] if len(sys.argv) > 2 else '/tmp/araignee')
    sheet('/tmp/spider.png')
