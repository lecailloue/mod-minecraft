"""Armes de la Hautemarche en 3D (fichiers seulement, pas encore branchees dans le jeu) : epees, dagues, haches, marteau, lances, massue,
pioche, arc elfique et batons de mage. 1 unite = 1 cm ; origine = bas du manche, la lame / la tete vers +y, faces plates de la lame vers +-x,
largeur de la lame sur z. Les parties lumineuses (gemmes, gouttieres runiques) vont dans une texture _glow.png a part.
Hors liste (a la demande) : masse du Seigneur de Cendre et Aubelame.
CLI : python3 tools/weapons3d.py [gen outdir]  -> apercus /tmp/weapons_*.png"""
import math
import zlib
import numpy as np
import trollcore as T
from trollcore import *
import orcs as O
from orcs import C, spike, translate, unique_names

# ---------------------------------------------------------------- materiaux
W_MAT = {
    'w_steel': dict(base=(150, 154, 160), n1=6, n2=6, tint=(1, 1, 1.03)),
    'w_ranger': dict(base=(138, 142, 150), n1=7, n2=7, tint=(1, 1, 1.03)),
    'w_lunar': dict(base=(170, 200, 218), n1=5, n2=6, tint=(.95, 1, 1.05)),
    'w_elven': dict(base=(218, 228, 236), n1=4, n2=5, tint=(1, 1, 1.02)),
    'w_ice': dict(base=(150, 212, 240), n1=8, n2=9, tint=(.9, 1, 1.05)),
    'w_blood': dict(base=(128, 22, 28), n1=7, n2=8, tint=(1.1, .8, .8)),
    'w_viper': dict(base=(104, 186, 66), n1=7, n2=8, tint=(.9, 1.05, .85)),
    'w_storm': dict(base=(122, 138, 170), n1=6, n2=7, tint=(1, 1, 1.08)),
    'w_sent': dict(base=(134, 139, 148), n1=6, n2=6, tint=(1, 1, 1.02)),
    'w_sun': dict(base=(238, 206, 108), n1=6, n2=7, tint=(1.05, 1, .85)),
    'w_wight': dict(base=(184, 226, 214), n1=7, n2=8, tint=(.95, 1.05, 1.0)),
    'w_dawn': dict(base=(226, 233, 246), n1=4, n2=5, tint=(.98, 1, 1.04)),
    'w_king': dict(base=(158, 224, 250), n1=5, n2=6, tint=(.95, 1, 1.05)),
    'w_gold': dict(base=(206, 168, 66), n1=6, n2=7, tint=(1.05, 1, .8)),
    'w_dgold': dict(base=(150, 118, 48), n1=6, n2=7, tint=(1.05, 1, .8)),
    'w_dwarf': dict(base=(118, 124, 134), n1=6, n2=7, tint=(1, 1, 1.05)),
    'w_iron': dict(base=(96, 98, 104), n1=7, n2=8, tint=(1, 1, 1.02)),
    'w_dark': dict(base=(44, 44, 52), n1=5, n2=6, tint=(1, 1, 1.08)),
    'w_wood': dict(base=(104, 72, 46), n1=7, n2=7, tint=(1, .9, .8)),
    'w_dwood': dict(base=(70, 48, 32), n1=6, n2=7, tint=(1, .9, .8)),
    'w_ewood': dict(base=(150, 118, 66), n1=6, n2=7, tint=(1, .95, .8)),
    'w_wrap': dict(base=(96, 66, 42), n1=6, n2=7, tint=(1, .9, .8)),
    'w_green': dict(base=(58, 92, 58), n1=6, n2=7, tint=(.9, 1.05, .9)),
    'w_blue': dict(base=(48, 68, 104), n1=6, n2=7, tint=(.9, .95, 1.1)),
    'w_gem': dict(base=(200, 200, 200), n1=4, n2=4, tint=(1, 1, 1)),
}


def d_blade(arr, face, rng, shine=1.0):
    h, w, _ = arr.shape
    for _ in range(max(1, h * w // 14)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(3, 9)), (236, 238, 240), .22 * shine)
    for _ in range(max(1, h * w // 40)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (70, 74, 80), .22, rng)
    if w >= 3:
        arr[:, 0] += 22 * shine
        arr[:, -1] -= 14


def d_metal(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(max(1, h * w // 18)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, 0, int(rng.integers(2, 5)), (230, 230, 230), .18)
    for (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        if w > 5 and h > 5:
            put(arr, x, y, (225, 220, 205), .5)


def d_wood2(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= 1 + (rng.random() - .5) * .22
    for _ in range(max(1, h * w // 9)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(3, 9)), (40, 26, 16), .38)
    for _ in range(max(1, h * w // 60)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (34, 22, 14), .5, rng)


def d_gem(arr, face, rng):
    h, w, _ = arr.shape
    arr[:] = arr * .85 + 30
    put(arr, 0, 0, (255, 255, 255), .8)
    if w > 2 and h > 2:
        arr[h // 2, w // 2] = (255, 255, 255)


BLADES = ('w_dawn', 'w_steel', 'w_ranger', 'w_lunar', 'w_elven', 'w_ice', 'w_blood', 'w_viper', 'w_storm', 'w_sent', 'w_sun', 'w_wight', 'w_king')
for _b in BLADES:
    W_MAT[_b] = dict(W_MAT[_b], n1=1, n2=2)


def d_blade_simple(arr, face, rng):
    """lame lisse : juste un liseré clair sur le tranchant et un côté un peu plus sombre"""
    h, w, _ = arr.shape
    if w >= 3:
        arr[:, 0] += 20
        arr[:, -1] -= 12


T.MATS.update(W_MAT)
for _k in W_MAT:
    T.DECOR[_k] = d_blade_simple if _k in BLADES else d_blade
for _k in ('w_gold', 'w_dgold', 'w_dwarf', 'w_iron', 'w_dark', 'w_green', 'w_blue'):
    T.DECOR[_k] = d_metal
for _k in ('w_wood', 'w_dwood', 'w_ewood'):
    T.DECOR[_k] = d_wood2
T.DECOR['w_wrap'] = O.d_wrap
T.DECOR['w_gem'] = d_gem
GEMS = {}


def gem_mat(name, rgb):
    """gemme coloree (materiau + decor facette)"""
    if name not in T.MATS:
        T.MATS[name] = dict(base=rgb, n1=6, n2=6, tint=(1, 1, 1))
        T.DECOR[name] = d_gem
    return name


# ---------------------------------------------------------------- effets lumineux
def fx_fuller(col, runes=False, width=1):
    """gouttiere lumineuse (et runes) sur les grandes faces de la lame"""
    def f(F, rng, glow):
        for face in ('east', 'west'):
            a = F[face]
            h, w, _ = a.shape
            for y in range(2, h - 2):
                if False:
                    pass
                elif runes and y % 5 in (1, 2) and False:
                    xs = [w // 2 - 1, w // 2, w // 2 + 1]
                else:
                    xs = [w // 2 + i for i in range(width) if True]
                for x in xs:
                    if 0 <= x < w:
                        a[y, x] = np.array(col, float) * .9
                        glow(face, x, y, col)
    return f


def fx_edges(col):
    """seules les aretes de la coque brillent"""
    def f(F, rng, glow):
        for face, a in F.items():
            h, w, _ = a.shape
            for y in range(h):
                for x in range(w):
                    if x in (0, w - 1) or y in (0, h - 1):
                        a[y, x] = np.array(col, float)
                        glow(face, x, y, tuple(int(v * .7) for v in col))
    return f


def d_glass(arr, face, rng):
    """verre : teinte claire, reflet en diagonale"""
    h, w, _ = arr.shape
    arr[:] = arr * .9 + 25
    for i in range(min(h, w)):
        if i % 4 == 1 and rng.random() < .8:
            arr[i, min(w - 1, i)] = (255, 255, 255)


def fx_gem(col):
    """toute la piece brille"""
    def f(F, rng, glow):
        for face, a in F.items():
            h, w, _ = a.shape
            for y in range(h):
                for x in range(w):
                    edge = x in (0, w - 1) or y in (0, h - 1)
                    k = .62 if edge and w > 2 and h > 2 else 1.0
                    c = tuple(int(v * k) for v in col)
                    a[y, x] = np.array(c, float) * .95
                    glow(face, x, y, c)
    return f


def fx_cracks(col):
    """fissures de braise sur tous les cotes"""
    def f(F, rng, glow):
        for face, a in F.items():
            h, w, _ = a.shape
            for _ in range(max(1, h * w // 9)):
                x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
                for i in range(int(rng.integers(2, 5))):
                    yy = y + i
                    xx = x + (i % 2) * int(rng.integers(0, 2))
                    if 0 <= xx < w and 0 <= yy < h:
                        a[yy, xx] = np.array(col, float)
                        glow(face, xx, yy, col)
    return f


# ---------------------------------------------------------------- pieces communes
def grip(y0, length, t=3, mat='w_wrap', rings=True):
    k = [C('grip', -t / 2, y0, -t / 2, t, length, t, mat, key='grip')]
    if rings:
        for i in range(max(1, length // 4)):
            k.append(C(f'wrap{i}', -t / 2 - .5, y0 + 1 + i * 4, -t / 2 - .5, t + 1, 1, t + 1, 'w_dwood', key='wrap'))
    return k


def pommel(y0, mat='w_gold', size=4, style='ball'):
    s = size
    if style == 'disc':
        return [C('pommel', -s / 2, y0, -s / 2 - 1, s, 2, s + 2, mat, key='pomd'), C('pommel_c', -s / 2 + .5, y0 + 2, -s / 2, s - 1, 2, s, mat, key='pomd2')]
    if style == 'gem':
        return [C('pommel', -s / 2, y0, -s / 2, s, 2, s, mat, key='pomg'), C('pommel_top', -s / 2 + 1, y0 + 2, -s / 2 + 1, s - 2, 2, s - 2, mat, key='pomg2')]
    return [C('pommel', -s / 2, y0, -s / 2, s, s - 1, s, mat, key='pomb'), C('pommel_t', -s / 2 + 1, y0 + s - 1, -s / 2 + 1, s - 2, 1, s - 2, mat, key='pomb2')]


def crossguard(y0, width, mat='w_gold', thick=3, h=3, style='plain'):
    k = [C('guard', -thick / 2, y0, -width / 2, thick, h, width, mat, key=f'g{width}{style}')]
    k.append(C('guard_c', -thick / 2 - .5, y0 - 1, -3, thick + 1, h + 2, 6, mat, key='gcenter'))
    if style == 'drop':                                                   # quillons recourbes
        for sg in (-1, 1):
            k.append(C(f'quill_{sg}', -thick / 2, y0 - 3, sg * (width / 2) - (1 if sg > 0 else 0), thick, 4, 1, mat, key='quill'))
    if style == 'up':
        for sg in (-1, 1):
            k.append(C(f'quill_{sg}', -thick / 2, y0 + h, sg * (width / 2) - (2 if sg > 0 else 0), thick, 5, 2, mat, key='quillu'))
    return k


def blade(y0, L, W, tip, mat, thick=3, fx=None, edge_mat=None):
    """lame droite tres simple : une plaque et une pointe en deux marches"""
    body = L - tip
    k = [C('spine', -1, y0, -W / 2, 2, body, W, mat, key=f'sp{W}{L}{mat}', fx=fx)]
    k.append(C('tip0', -1, y0 + body - .3, -W / 2 + 1, 2, tip * .55 + .3, W - 2, mat, key=f'tp0{W}{tip}{mat}'))
    k.append(C('tip1', -1, y0 + body + tip * .55 - .3, -W / 4, 2, tip * .45 + .3, W / 2, mat, key=f'tp1{W}{tip}{mat}'))
    return k


def leaf_blade(y0, L, W, mat, fx=None, thick=2, seg=12, belly=.55):
    """lame elfique en feuille : s'elargit vers le milieu puis se resserre en pointe"""
    n = max(3, int(L // seg))
    seg = L / n
    k = []
    y = y0
    for i in range(n):
        t = (i + .5) / n
        wv = W * (0.62 + 0.38 * math.sin(math.pi * min(1, t / belly) * .5 + (0 if t < belly else 0))) if t < belly else W * max(.14, 1 - (t - belly) / (1 - belly)) ** .9
        w = max(2, int(round(wv)) // 2 * 2)
        th = 2
        k.append(C(f'lf{i}', -th / 2, y, -w / 2, th, seg + (.4 if i < n - 1 else 0), w, mat, key=f'lb{i}{w}{th}{mat}', fx=fx))
        y += seg
    return k


def curved(name, base, L, W, bend, mat, seg=4, thick=3, prof=.55, fx=None, rz=0.0, ry=0.0, rx=0.0, flat=0.0):
    """lame ou membre courbe : segments imbriques qui s'effilent ; bend = degres par segment (autour de x = courbure vers -z/+z)"""
    n = max(2, int(round(L / seg)))
    px, py, pz = base
    parts = []
    for i in range(n):
        t = i / (n - 1)
        f = 1.0 if t < flat else max(0.0, 1 - (t - flat) / (1 - flat + 1e-9)) ** prof
        w = max(1, int(round(thick * (0.55 + 0.45 * f))))
        d = max(2, int(round(W * f)))
        cube = C(f'{name}_{i}', px - w / 2, py - (1 if i else 0), pz - d / 2, w, seg + (1 if i else 0), d, mat, key=f'cv{name[:4]}{w}{d}{mat}', fx=fx if i else None)
        parts.append(([cube], (px, py, pz), (bend if i else 0, 0, 0)))
        py += seg
    g = None
    for i in range(n - 1, -1, -1):
        kids, piv, rot = parts[i]
        g = Group(f'{name}_{i}', piv, rot, kids + ([g] if g else []))
    return Group(name + '_o', base, (rx, ry, rz), [g])


def stack(name, x, y0, z, widths, hs, d, mat, key=None, fx=None, center=True):
    """empilement de cubes de largeur variable (tetes de hache, pointes de lance...) ; widths = largeur (axe x) de chaque etage"""
    k = []
    y = y0
    for i, (w, h) in enumerate(zip(widths, hs)):
        k.append(C(f'{name}{i}', x - w / 2, y, z, w, h, d, mat, key=f'{key or name}{i}{w}{h}{d}', fx=fx))
        y += h
    return k


def hold(kids):
    return [T.Group('arme', (0, 0, 0), (0, 0, 0), kids)]


# ---------------------------------------------------------------- epees
def sword(spec):
    g = spec.get
    gl = g('grip', 11)
    k = pommel(0, g('pommel_mat', 'w_gold'), 4, g('pommel', 'ball'))
    k += grip(3, gl, 3, g('grip_mat', 'w_wrap'), g('grip_rings', True))
    gy = 3 + gl
    k += crossguard(gy, g('guard_w', 18), g('guard_mat', 'w_gold'), 3, 3, g('guard', 'plain'))
    by = gy + 3
    if g('leaf'):
        k += leaf_blade(by, g('L', 80), g('W', 8), g('mat', 'w_steel'), g('fx'))
    elif g('curve'):
        k.append(curved('bl', (0, by, 0), g('L', 80), g('W', 7), g('curve') * 2.2, g('mat', 'w_steel'), seg=9, thick=2, prof=.8, fx=g('fx'), flat=g('flat', .6)))
    else:
        k += blade(by, g('L', 80), g('W', 8), g('tip', 9), g('mat', 'w_steel'), 3, g('fx'), g('edge_mat'))
    if g('gem'):
        k.append(C('gem', -2, gy - 1, -2.4, 4, 4, 5, gem_mat('w_gem_' + g('gem')[0], g('gem')[1]), key='gem' + g('gem')[0], fx=fx_gem(g('gem')[1])))
    for extra in g('extra', lambda: [])():
        k.append(extra)
    return k


def shards(name, y0, n, mat, h=8, fx=None, spread=3.0, W=9):
    """eclats de glace plantes sur les deux tranchants (cotes +-z de la lame), pointes vers le haut et vers l'exterieur"""
    out = []
    for i in range(n):
        for sg in (-1, 1):
            yy = y0 + i * (h * 1.15) + (h * .5 if sg > 0 else 0)
            out.append(spike(f'{name}{i}_{sg}', 0, yy, sg * (W / 2 - 1.5), h - (i % 2), 2, (sg * (38 + i * 3), 0, 0), mat=mat, d=2))
    return out


def dawn_rays(gy):
    """rayons dores en eventail de chaque cote de la garde (aube)"""
    out = []
    for sg in (-1, 1):
        for i, (zz, h, ang) in enumerate(((3.5, 9, 18), (6.5, 8, 40), (9.5, 6, 62))):
            out.append(spike(f'ray{i}_{sg}', 0, gy + 3, sg * zz, h, 2, (sg * ang, 0, 0), mat='w_gold', d=2))
    return out


def teeth(name, y0, n, mat, step=6, W=8):
    """dos denta : petites dents sur un cote de la lame"""
    return [C(f'{name}{i}', -1, y0 + i * step, W / 2 - 1, 2, 3, 3, mat, key=f'teeth{mat}') for i in range(n)]


def lightning_runes(y0, n, col):
    return [C(f'zig{i}', -2, y0 + i * 6, ((-1) ** i) * 1 - 1, 4, 2, 2, 'w_gem', key=f'zig{i%2}', fx=fx_gem(col)) for i in range(n)]


def build_swords():
    out = {}
    out['lunargent_sword'] = ('Épée de lunargent', sword(dict(mat='w_lunar', gem=('b', (80, 170, 235)), fx=fx_fuller((120, 200, 255)), guard='plain', pommel='ball')))
    out['elven_blade'] = ('Lame elfique', sword(dict(mat='w_elven', L=86, W=9, leaf=True, tip=14, grip=12, guard_mat='w_gold', guard='up', guard_w=14, grip_mat='w_green',
                                                        fx=fx_fuller((130, 200, 255), runes=True), pommel='gem', gem=('w', (255, 255, 255)))))
    out['elven_glaive'] = ('Glaive elfique', sword(dict(mat='w_elven', L=78, W=12, curve=-1.6, flat=.5, grip=14, guard_mat='w_gold', guard='drop', guard_w=16,
                                                           fx=fx_fuller((130, 200, 255), runes=True), pommel='disc')))
    out['frost_blade'] = ('Lame de givre', sword(dict(mat='w_ice', L=88, W=9, tip=12, grip_mat='w_blue', guard_mat='w_lunar', guard='up', guard_w=20,
                                                         fx=fx_fuller((210, 245, 255)), pommel='gem', gem=('i', (210, 245, 255)),
                                                         extra=lambda: shards('ice', 26, 4, 'w_ice', 9))))
    out['blood_blade'] = ('Lame sanguine', sword(dict(mat='w_blood', L=88, W=9, tip=10, grip_mat='w_dwood', guard_mat='w_dark', guard='drop', guard_w=22,
                                                         gem=('r', (255, 60, 60)), fx=fx_fuller((255, 70, 60), width=1),
                                                         extra=lambda: teeth('blt', 26, 5, 'w_dark', 12, 9))))
    out['viper_dagger'] = ('Dague de vipère', sword(dict(mat='w_viper', L=34, W=7, curve=-3.2, flat=.3, grip=10, grip_mat='w_green', guard_mat='w_dgold', guard='drop', guard_w=10,
                                                            gem=('v', (216, 255, 96)), fx=fx_fuller((216, 255, 96)), pommel='ball')))
    out['storm_blade'] = ('Lame de tempête', sword(dict(mat='w_storm', L=84, W=9, curve=-1.5, flat=.55, grip=12, grip_mat='w_dark', guard_mat='w_gold', guard='up', guard_w=17,
                                                           gem=('y', (250, 240, 90)), fx=fx_fuller((250, 240, 90), runes=True), pommel='gem')))
    out['sentinel_blade'] = ('Lame sentinelle', sword(dict(mat='w_sent', L=82, W=9, tip=9, grip_mat='w_wrap', guard_mat='w_gold', guard='plain', guard_w=20,
                                                              fx=fx_fuller((255, 176, 64)), gem=('o', (255, 176, 64)))))
    out['sun_blade'] = ('Lame solaire', sword(dict(mat='w_sun', L=84, W=9, tip=10, grip_mat='w_dwood', guard_mat='w_dgold', guard='up', guard_w=22,
                                                      fx=fx_fuller((255, 248, 190), width=2), gem=('s', (255, 250, 200)), pommel='disc')))
    out['halfling_dagger'] = ('Dague de semi-homme', sword(dict(mat='w_steel', L=28, W=5, tip=7, grip=9, grip_mat='w_wrap', guard_mat='w_gold', guard='plain', guard_w=9)))
    out['medieval_sword'] = ('Épée d\'armes', sword(dict(mat='w_steel', L=86, W=7, tip=11, grip=13, grip_mat='w_black', grip_rings=False, guard_mat='w_steel', guard='plain', guard_w=22, pommel='ball', pommel_mat='w_steel')))
    out['ranger_sword'] = ('Épée de rôdeur', sword(dict(mat='w_ranger', L=80, W=7, tip=9, grip=12, grip_mat='w_dwood', guard_mat='w_dark', guard='plain', guard_w=16, pommel='disc', pommel_mat='w_dark')))
    out['wight_blade'] = ('Lame des tertres', sword(dict(mat='w_wight', L=82, W=9, curve=-.9, flat=.6, grip_mat='w_dark', guard_mat='w_wight', guard='up', guard_w=18,
                                                            gem=('t', (112, 255, 216)), fx=fx_fuller((112, 255, 216), runes=True), pommel='gem')))
    out['kings_blade'] = ('Épée du roi elfe', sword(dict(mat='w_king', L=94, W=11, leaf=True, tip=16, grip=14, grip_mat='w_green', guard_mat='w_gold', guard='up', guard_w=24,
                                                            gem=('k', (64, 224, 128)), fx=fx_fuller((120, 255, 190), runes=True, width=1), pommel='disc')))
    out['valmoire_blade'] = ('Aubelame', sword(dict(mat='w_steel', L=86, W=7, tip=11, grip=13, grip_mat='w_black', grip_rings=False, guard_mat='w_steel', guard='plain', guard_w=22,
                                                      pommel='ball', pommel_mat='w_steel')))
    return out


def ash_mace():
    """masse du Seigneur de Cendre : repris du modele du mob (tools/ashlord.py), a l'echelle 0.85, sans rotation"""
    S = .85
    r = lambda v: int(round(v * S))
    cr = fx_cracks((255, 90, 24))
    k = [C('haft', -r(2.5), 0, -r(2.5), r(5), r(108), r(5), 'w_dwood', key='m_haft'),
         C('haft_butt', -r(3.5), -r(4), -r(3.5), r(7), r(5), r(7), 'w_dark', key='m_butt'),
         C('haft_ring_a', -r(3.5), r(36), -r(3.5), r(7), r(3), r(7), 'w_iron', key='m_ra'),
         C('haft_ring_b', -r(3.5), r(86), -r(3.5), r(7), r(3), r(7), 'w_iron', key='m_ra'),
         C('mace_collar', -r(5), r(106), -r(5), r(10), r(4), r(10), 'w_iron', key='m_coll'),
         C('mace_core', -r(6), r(110), -r(6), r(12), r(18), r(12), 'w_dark', key='m_core', fx=cr)]
    prof = [7, 13, 18, 21, 22, 20, 15, 9]
    for j, hw in enumerate(prof):
        y = r(108 + j * 3.4)
        k.append(C(f'fan_a_{j}', -r(hw), y, -1, r(hw) * 2, 3, 2, 'w_iron' if j % 2 else 'w_dark', key='m_fa'))
        k.append(C(f'fan_b_{j}', -1, y, -r(hw * .8), 2, 3, r(hw * 1.6), 'w_dark' if j % 2 else 'w_iron', key='m_fb'))
    for i, (w, h) in enumerate(((8, 7), (6, 6), (4, 6), (2, 5))):
        k.append(C(f'tip{i}', -w // 2, r(128) + sum(x[1] for x in ((8, 7), (6, 6), (4, 6), (2, 5))[:i]), -w // 2, w, h, w, 'w_iron', key=f'm_tip{i}'))
    return k


# ---------------------------------------------------------------- armes d'hast, haches, marteau, massue, pioche
def shaft(y0, L, t=3, mat='w_wood', wrap=None):
    k = [C('shaft', -t / 2, y0, -t / 2, t, L, t, mat, key=f'sh{t}{L}{mat}')]
    if wrap:
        for (a, b) in wrap:
            k += grip(a, b - a, t + 1, 'w_wrap')
    return k


T.MATS['w_black'] = dict(base=(30, 30, 34), n1=4, n2=5, tint=(1, 1, 1.05)); T.DECOR['w_black'] = d_wood2


def morning_star():
    """etoile du matin : manche noir, tout le reste en acier"""
    cy, R, SL = 82, 6, 6
    k = [C('shaft', -1.5, 0, -1.5, 3, 72, 3, 'w_black', key='ms_shaft'),
         C('butt', -2.5, -3, -2.5, 5, 4, 5, 'w_steel', key='ms_butt'),
         C('ring_a', -2.5, 20, -2.5, 5, 2, 5, 'w_steel', key='ms_ring'),
         C('ring_b', -2.5, 44, -2.5, 5, 2, 5, 'w_steel', key='ms_ring'),
         C('collar', -3.5, 68, -3.5, 7, 4, 7, 'w_iron', key='ms_collar'),
         C('collar2', -2.5, 72, -2.5, 5, 4, 5, 'w_steel', key='ms_collar2')]
    for sg in (-1, 1):                                              # languettes d'acier le long du manche
        k.append(C(f'langet_x{sg}', sg * 2 - .5, 46, -1, 1, 22, 2, 'w_steel', key='ms_lang'))
        k.append(C(f'langet_z{sg}', -1, 46, sg * 2 - .5, 2, 22, 1, 'w_steel', key='ms_lang'))
    k.append(C('ball', -R, cy - R, -R, 2 * R, 2 * R, 2 * R, 'w_steel', key='ms_ball'))
    k.append(C('ball45', -R, cy - R, -R, 2 * R, 2 * R, 2 * R, 'w_iron', key='ms_ball45', rot=(0, 45, 0)))
    k.append(C('band', -R - 1, cy - 1, -R - 1, 2 * R + 2, 2, 2 * R + 2, 'w_steel', key='ms_band'))
    r2 = R * .75
    sp = lambda n, x, y, z, rot: spike(n, x, y, z, SL, 2, rot, mat='w_steel', d=2)
    k.append(sp('sp_top', 0, cy + R, 0, (0, 0, 0)))
    k.append(sp('sp_bot', 0, cy - R, 0, (180, 0, 0)))
    for sg in (-1, 1):
        k.append(sp(f'sp_eq_x{sg}', sg * R, cy, 0, (0, 0, -sg * 90)))
        k.append(sp(f'sp_eq_z{sg}', 0, cy, sg * R, (sg * 90, 0, 0)))
        k.append(sp(f'sp_up_x{sg}', sg * r2, cy + r2, 0, (0, 0, -sg * 45)))
        k.append(sp(f'sp_up_z{sg}', 0, cy + r2, sg * r2, (sg * 45, 0, 0)))
    return k


def build_polearms():
    out = {'morning_star': ('Étoile du matin', morning_star())}
    # hache de guerre naine : double fer, manche de bois cercle de fer
    k = shaft(0, 92, 3, 'w_wood', wrap=[(8, 28)])
    k.append(C('cap', -2.5, -3, -2.5, 5, 4, 5, 'w_dwarf', key='cap'))
    k.append(C('hub', -3, 80, -3, 6, 14, 6, 'w_dwarf', key='hub'))
    for sg in (-1, 1):
        for i, (zz, hh, dy) in enumerate(((3, 14, 80), (6, 22, 76), (10, 28, 73), (13, 32, 71))):
            z0 = zz if sg > 0 else -zz - 4 + (0)
            k.append(C(f'axe_{sg}_{i}', -1.5, dy + (14 - hh) / 2 + 3 if False else 87 - hh / 2, (sg * (zz + 1)) - (0 if sg > 0 else 4), 3, hh, 4, 'w_dwarf', key=f'axh{i}{hh}'))
        k.append(C(f'edge_{sg}', -1, 87 - 17, (sg * 18) - (0 if sg > 0 else 1), 2, 34, 1, 'w_steel', key='axedge'))
    k.append(spike('top', 0, 94, 0, 8, 3, (0, 0, 0), mat='w_dwarf'))
    out['dwarven_axe'] = ('Hache de guerre naine', k)
    # marteau de guerre
    k = shaft(0, 84, 3, 'w_wood', wrap=[(6, 26)])
    k.append(C('cap', -2.5, -3, -2.5, 5, 4, 5, 'w_dwarf', key='cap'))
    k.append(C('collar', -4, 80, -4, 8, 4, 8, 'w_dwarf', key='hcoll'))
    k.append(C('head', -6, 84, -11, 12, 12, 22, 'w_dwarf', key='hhead'))
    k.append(C('face_l', -7, 85, -13, 14, 10, 2, 'w_iron', key='hface'))
    k.append(C('face_r', -7, 85, 11, 14, 10, 2, 'w_iron', key='hface'))
    k.append(C('rune', -6.5, 88, -4, 13, 4, 8, 'w_gold', key='hrune'))
    k.append(spike('back', 0, 94, 0, 8, 4, (0, 0, 0), mat='w_dwarf'))
    out['dwarven_hammer'] = ('Marteau de guerre nain', k)
    out['ash_mace'] = ('Masse du Seigneur de Cendre', ash_mace())
    # lance des cavaliers
    k = shaft(0, 188, 3, 'w_dwood', wrap=[(70, 92)])
    k.append(C('butt', -2, -6, -2, 4, 8, 4, 'w_steel', key='sbutt'))
    k.append(C('socket', -2.5, 186, -2.5, 5, 10, 5, 'w_steel', key='ssock'))
    k += leaf_blade(196, 36, 10, 'w_steel', None, thick=3, seg=6, belly=.4)
    for sg in (-1, 1):
        k.append(C(f'wing_{sg}', -1, 190, sg * 5 - (1 if sg > 0 else 0), 2, 6, 2, 'w_steel', key='swing'))
    k.append(C('tassel', -2.2, 178, -2.2, 4.4, 8, 4.4, 'w_blood', key='stas'))
    out['rider_spear'] = ('Lance des cavaliers', k)
    # massue de troll
    k = []
    prof = [(0, 7), (6, 7), (12, 8), (18, 9), (24, 11), (30, 13), (36, 15), (42, 16), (48, 16)]
    y = 0
    for i, (yy, w) in enumerate(prof):
        k.append(C(f'club{i}', -w / 2, yy, -w / 2, w, 6, w, 'w_dwood', key=f'club{i}{w}'))
    k.append(C('club_top', -6, 54, -6, 12, 4, 12, 'w_dwood', key='clubtop'))
    for i, (yy, ang) in enumerate(((30, 0), (36, 90), (42, 180), (48, 270), (38, 45), (44, 225), (50, 135), (33, 315))):
        r = 8 + (yy - 30) / 6.0 * 0.5
        x, z = math.cos(math.radians(ang)) * r, math.sin(math.radians(ang)) * r
        k.append(spike(f'stud{i}', x, yy, z, 5, 3, (0, 0, 0), mat='w_iron'))
    k.append(C('strap', -5, 20, -5, 10, 2, 10, 'w_wrap', key='strap'))
    out['troll_club'] = ('Massue de troll', k)
    # pioche de lunargent : tete en croissant
    k = shaft(0, 78, 3, 'w_ewood', wrap=[(6, 24)])
    k.append(C('cap', -2.5, -3, -2.5, 5, 4, 5, 'w_lunar', key='cap2'))
    k.append(C('hub', -3, 72, -3, 6, 10, 6, 'w_lunar', key='phub'))
    for sg in (-1, 1):
        k.append(curved(f'pick_{"l" if sg < 0 else "r"}', (0, 78, 0), 34, 5, sg * 5.5, 'w_lunar', seg=3, thick=3, prof=.7, rx=sg * 90, flat=.15))
    out['lunargent_pickaxe'] = ('Pioche de lunargent', k)
    return out


# ---------------------------------------------------------------- arc elfique
def arc_tip(L, seg, bend):
    n = max(2, int(round(L / seg)))
    y = z = 0.0
    a = 0.0
    for i in range(n):
        if i:
            a += math.radians(bend)
        y += seg * math.cos(a); z += seg * math.sin(a)
    return y, z


T.MATS['w_elfbow'] = dict(base=(176, 138, 78), n1=5, n2=6, tint=(1, .96, .8))


def d_elfbow(arr, face, rng):
    """bois clair elfique : fibres, liseres dores sur les bords et liane feuillue en relief (tout le detail est dans la texture)"""
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= 1 + (rng.random() - .5) * .16
    for _ in range(max(1, h * w // 14)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(3, 8)), (96, 66, 34), .35)
    if w >= 3 and h >= 4:
        for y in range(h):
            arr[y, 0] = arr[y, 0] * .5 + np.array((232, 196, 98)) * .5                 # lisere dore
            arr[y, w - 1] = arr[y, w - 1] * .5 + np.array((232, 196, 98)) * .5
    if w >= 4 and h >= 6:
        ph = rng.random() * 6.28
        for y in range(h):                                                              # liane sinueuse
            x = int(round((w - 1) / 2 + math.sin(y * .75 + ph) * min(1.6, (w - 3) / 2 + .6)))
            if 0 <= x < w:
                arr[y, x] = (92, 150, 70)
            if y % 3 == 1:                                                              # feuilles
                for dxl in (-1, 1):
                    xx = x + dxl
                    if 0 < xx < w - 1:
                        arr[y, xx] = (150, 205, 112)
        for y in range(2, h - 1, 5):                                                    # petits points dores
            arr[y, int(rng.integers(1, w - 1))] = (255, 232, 140)


T.DECOR['w_elfbow'] = d_elfbow


def build_bow():
    k = [C('grip', -1.5, -8, -1.5, 3, 16, 3, 'w_wrap', key='bgrip')]                     # poignee d'un seul bloc : cuir tresse peint
    k.append(C('riser', -2, 8, -2, 4, 4, 4, 'w_gold', key='bris'))
    k.append(C('riser2', -2, -12, -2, 4, 4, 4, 'w_gold', key='bris'))
    L, seg, bend = 50, 10, 7.5
    k.append(curved('limb_u', (0, 12, 0), L, 4, -bend, 'w_elfbow', seg=seg, thick=3, prof=.3, flat=.4))
    k.append(curved('limb_d', (0, -12, 0), L, 4, bend, 'w_elfbow', seg=seg, thick=3, prof=.3, flat=.4, rx=180))
    ty, tz = arc_tip(L, seg, bend)
    top = 12 + ty
    zs = tz * -1                                                                          # z de la corde au repos (a hauteur des encoches)
    k.append(T.Group('str_u', (0, top, zs), (0, 0, 0), [C('str_u_c', -.5, 0, zs - .5, 1, top, 1, 'w_gem', key='bstr')]))
    k.append(T.Group('str_l', (0, -top, zs), (0, 0, 0), [C('str_l_c', -.5, -top, zs - .5, 1, top, 1, 'w_gem', key='bstr')]))
    for sg in (1, -1):
        k.append(C(f'nock_{sg}', -2, sg * top - (1 if sg > 0 else 0), -tz - 1.5, 4, 2, 3, 'w_gold', key='bnock'))
    # fleche encochee (visible seulement quand on bande l'arc) : fut le long de z, pointe vers +z (cote ventre)
    k.append(T.Group('arrow', (0, 0, 0), (0, 0, 0), [
        C('arrow_shaft', -.5, -.5, zs - 4, 1, 1, 78, 'w_ewood', key='arsh'),
        C('arrow_head', -1.5, -1.5, zs + 74, 3, 3, 8, 'w_steel', key='arhd'),
        C('arrow_f1', -.5, -3.5, zs - 4, 1, 7, 12, 'w_green', key='arfl'),
        C('arrow_f2', -3.5, -.5, zs - 4, 7, 1, 12, 'w_green', key='arfl')]))
    return {'elven_bow': ('Arc elfique', k)}


# ---------------------------------------------------------------- batons de mage
STAFF_WOOD = {'fire': 'w_char', 'light': 'w_drift', 'storm': 'w_dkdrift', 'healing': 'w_drift'}
T.MATS.update({'w_drift': dict(base=(150, 138, 120), n1=7, n2=8, tint=(1, .98, .94)),
               'w_dkdrift': dict(base=(92, 86, 84), n1=7, n2=8, tint=(.98, .98, 1.02)),
               'w_char': dict(base=(58, 46, 42), n1=6, n2=7, tint=(1, .92, .88))})
for _k in ('w_drift', 'w_dkdrift', 'w_char'):
    T.DECOR[_k] = d_wood2


def driftwood(kind, gem_col, gem_name, crown=12, fx_wood=None, moss=False):
    """baton de bois flotte : tige noueuse et tordue, branches cassees, fourche de racines qui tient une pierre brute"""
    rng = np.random.default_rng({'fire': 3, 'light': 5, 'storm': 8, 'healing': 13}[kind])
    wood = STAFF_WOOD[kind]
    k = []
    lens = [20, 16, 24, 18, 22, 16, 20, 14]
    y = 0
    dx = dz = 0
    for i, L in enumerate(lens):
        t = (4, 3, 4, 3, 3, 4, 3, 3)[i]
        if i:
            dx = int(np.clip(dx + rng.integers(-1, 2), -2, 2)); dz = int(np.clip(dz + rng.integers(-1, 2), -2, 2))
        k.append(C(f'dw{i}', dx - t / 2, y, dz - t / 2, t, L, t, wood, key=f'{kind}dw{i}', fx=fx_wood if i in (2, 5) else None))
        if i < len(lens) - 1:                                             # noeud au raccord
            k.append(C(f'kn{i}', dx - (t + 2) / 2, y + L - 2, dz - (t + 2) / 2, t + 2, 3, t + 2, wood, key=f'{kind}kn{i}'))
        y += L
    top = (dx, y, dz)
    k.append(C('root0', -3, -2, -3, 6, 3, 6, wood, key=f'{kind}rt0'))     # pied evase
    k.append(C('root1', -2, -4, -2, 4, 3, 4, wood, key=f'{kind}rt1'))
    for j, (yy, ang, ln) in enumerate(((38, 55, 9), (84, -48, 10), (118, 62, 7))):   # branches cassees
        sg = -1 if j % 2 else 1
        k.append(spike(f'stub{j}', 0, yy, 0, ln, 2, (0, 0, -ang) if j != 1 else (ang, 0, 0), mat=wood, d=2))
    if moss:
        for j, yy in enumerate((20, 52, 96, 126)):
            k.append(C(f'moss{j}', -3 + (j % 2) * 2, yy, -3 + ((j + 1) % 2) * 2, 4, 4, 4, 'w_green', key=f'{kind}moss'))
    # fourche : trois racines qui montent et se resserrent autour de la pierre
    cx, cy, cz = top
    for i in range(3):
        a = math.radians(i * 120 + 20)
        x, z = cx + math.cos(a) * 2.5, cz + math.sin(a) * 2.5
        k.append(spike(f'fork{i}', x, cy - 2, z, crown + 4, 3, (math.sin(a) * 16, 0, -math.cos(a) * 16), mat=wood, d=3))
        k.append(spike(f'forktip{i}', x + math.cos(a) * 3.6, cy + crown - 2, z + math.sin(a) * 3.6, 6, 2, (-math.sin(a) * 26, 0, math.cos(a) * 26), mat=wood, d=2))
    gm = gem_mat(gem_name, gem_col)
    lite = tuple(min(255, int(v * .5 + 125)) for v in gem_col)
    gt = gem_mat(gem_name + '_t', lite)
    glass = 'w_glass_' + kind
    T.MATS[glass] = dict(base=tuple(min(255, int(v * .45 + 120)) for v in gem_col), n1=3, n2=4, tint=(1, 1, 1))
    T.DECOR[glass] = d_glass
    T.ALPHA[glass] = 105
    T.MATS['w_rock'] = dict(base=(78, 72, 70), n1=7, n2=8, tint=(1, 1, 1.02)); T.DECOR['w_rock'] = d_wood2

    def part(nm, px, py, pz, ln, ww, mat, col, rx=0, rz=0, fx=True):
        return C(nm, px - ww / 2, py, pz - ww / 2, ww, ln, ww, mat, key=f'{kind}{nm}', rot=(rx, 0, rz), pivot=(px, py, pz), fx=fx_gem(col) if fx else fx_edges(lite))

    def crystal(name, x, y, z, h, w, rx, rz):
        """cristal interieur lumineux : fut + pointe plus claire, suivant l'inclinaison"""
        ex = (-math.sin(math.radians(rz)) * math.cos(math.radians(rx)), math.cos(math.radians(rz)) * math.cos(math.radians(rx)), math.sin(math.radians(rx)))
        hb = h * .66
        return [part(name, x, y, z, hb, w, gm, gem_col, rx, rz), part(name + 't', x + ex[0] * hb, y + ex[1] * hb, z + ex[2] * hb, h - hb, max(2, w - 2), gt, lite, rx, rz)]

    cx, cy, cz = top
    for j, (dxo, dyo, dzo, w, hh) in enumerate(((-3, 0, -2, 6, 3), (1, 0, -3, 5, 3), (-1, 1, 2, 6, 3), (3, 0, 1, 4, 2))):   # socle de roche brute
        k.append(C(f'rock{j}', cx + dxo - w / 2, cy + dyo, cz + dzo - w / 2, w, hh, w, 'w_rock', key=f'{kind}rock{j}'))
    # coeur : cristal lumineux
    k += crystal('in_main', cx, cy + 3, cz, crown + 3, 4, 0, 0)
    for i, (ang, rt, h) in enumerate(((20, 22, 8), (140, 26, 7), (255, 20, 8))):
        a = math.radians(ang)
        k += crystal(f'in{i}', cx + math.cos(a) * 1.2, cy + 3, cz + math.sin(a) * 1.2, h, 3, math.sin(a) * rt, -math.cos(a) * rt)
    # coque : pierre transparente facettee par-dessus
    k.append(part('shell', cx, cy + 2, cz, crown + 1, 7, glass, lite, 0, 0, fx=False))
    k[-1].rot = (0, 45, 0)
    k.append(C('shell_t', cx - 2, cy + crown + 3, cz - 2, 4, 4, 4, glass, key=f'{kind}shellt', rot=(0, 45, 0), fx=fx_edges(lite)))
    k.append(C('shell_p', cx - 1, cy + crown + 7, cz - 1, 2, 2, 2, glass, key=f'{kind}shellp', rot=(0, 45, 0), fx=fx_edges(lite)))
    for i, (ang, rt, h) in enumerate(((60, 14, 7), (200, 16, 6))):
        a = math.radians(ang)
        k.append(part(f'lobe{i}', cx + math.cos(a) * 2.5, cy + 3, cz + math.sin(a) * 2.5, h, 4, glass, lite, math.sin(a) * rt, -math.cos(a) * rt, fx=False))
    return k


def build_staffs():
    out = {}
    out['staff_fire'] = ('Bâton de feu', driftwood('fire', (240, 100, 28), 'w_flame', crown=12, fx_wood=fx_cracks((255, 120, 30))))
    out['staff_light'] = ('Bâton de lumière', driftwood('light', (255, 226, 120), 'w_sunorb', crown=12))
    out['staff_storm'] = ('Bâton de foudre', driftwood('storm', (150, 110, 255), 'w_vio', crown=14))
    out['staff_healing'] = ('Bâton de soin', driftwood('healing', (80, 220, 130), 'w_emer', crown=12, moss=True))
    return out


# ---------------------------------------------------------------- assemblage
def all_weapons():
    out = {}
    for fn in (build_swords, build_polearms, build_bow, build_staffs):
        out.update(fn())
    return out


def extents(groups):
    lo = np.array([1e9] * 3); hi = np.array([-1e9] * 3)
    for c, chain in walk(groups):
        w, h, d = c.size
        for dx in (0, w):
            for dy in (0, h):
                for dz in (0, d):
                    p = np.array(c.frm) + (dx, dy, dz)
                    p = T.rot_mat(c.rot) @ (p - np.array(c.pivot)) + np.array(c.pivot)
                    for g in chain:
                        p = T.rot_mat(g.rot) @ (p - np.array(g.pivot)) + np.array(g.pivot)
                    lo = np.minimum(lo, p); hi = np.maximum(hi, p)
    return lo, hi


def rad2deg_groups(groups):
    pass


def build(name, weapons=None, shift=0):
    weapons = weapons or all_weapons()
    label, kids = weapons[name]
    if shift:
        kids = all_weapons()[name][1]
        translate(kids, 0, shift, 0)
    T._N[0] = 0
    groups = hold(kids)
    unique_names(groups)
    T.normalize(groups)
    cubes = [c for c, _ in T.walk(groups)]
    for c in cubes:
        fr, sz = list(c.frm), list(c.size)
        for i in range(3):
            if sz[i] < 0:
                fr[i] += sz[i]; sz[i] = -sz[i]
        c.frm, c.size = tuple(fr), tuple(max(1, v) for v in sz)
    W, H = T.pack(cubes)
    tex, glow = T.paint_all(cubes, W, H)
    return label, groups, cubes, W, H, tex, glow


def sheet(path, names, cols=4, view=(35, -14), cell=(330, 560)):
    from PIL import Image
    weapons = all_weapons()
    CW, CH = cell
    cells = []
    for n in names:
        lo, hi = extents(build(n, weapons)[1])
        cells.append((n, lo[1] - 3 if lo[1] < -3 else 0))
        if hi[1] - lo[1] > 120:
            cells.append((n, hi[1] - 55))                    # gros plan sur la tete
    rows = (len(cells) + cols - 1) // cols
    big = np.zeros((rows * CH, cols * CW, 3), np.uint8)
    for i, (n, crop) in enumerate(cells):
        label, groups, cubes, W, H, tex, glow = build(n, weapons, shift=-crop)
        lo, hi = extents(groups)
        hgt = 70 if crop > 0 else (hi[1] - lo[1])
        sc = min((CH - 90) / max(hgt, 30), 6.0)
        img = np.zeros((CH, CW, 3), float)
        yy = np.linspace(0, 1, CH)[:, None, None]
        img[:] = np.array((30, 33, 40)) * (1 - yy) + np.array((58, 62, 72)) * yy
        zb = np.full((CH, CW), -1e9)
        T.render(groups, tex, view[0], view[1], CW, CH, sc, 0, 'opaque', img, zb)
        out = T.render(groups, tex, view[0], view[1], CW, CH, sc, 0, 'trans', img, zb)
        out = img if out is None else out
        big[(i // cols) * CH:(i // cols + 1) * CH, (i % cols) * CW:(i % cols + 1) * CW] = np.clip(out, 0, 255).astype(np.uint8)
    Image.fromarray(big).save(path)


def generate(outdir):
    import json, os, bbimport, tempfile
    from PIL import Image
    import models
    os.makedirs(outdir, exist_ok=True)
    weapons = all_weapons()
    meta = {}
    for name in weapons:
        label, groups, cubes, W, H, tex, glow = build(name, weapons)
        T.export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
        geo = bbimport.convert(os.path.join(outdir, name + '.bbmodel'))
        geo['ymin'], geo['ymax'] = models.geo_extent(geo)
        geo['height'] = geo['ymax']
        json.dump(geo, open(os.path.join(outdir, name + '.json'), 'w'), separators=(',', ':'))
        Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, name + '.png'))
        if glow[..., 3].any():
            Image.fromarray(glow, 'RGBA').save(os.path.join(outdir, name + '_glow.png'))
        lo, hi = extents(groups)
        meta[name] = dict(nom=label, longueur_cm=int(round(hi[1] - lo[1])), cubes=len(cubes), lumineux=bool(glow[..., 3].any()), texture=f'{W}x{H}')
        print(name, meta[name])
    json.dump(meta, open(os.path.join(outdir, 'armes.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == 'gen':
        generate(sys.argv[2] if len(sys.argv) > 2 else '/tmp/weapons')
    else:
        names = list(all_weapons())
        sheet('/tmp/weapons_swords.png', names[:12], cols=6)
        sheet('/tmp/weapons_other.png', names[12:], cols=6)
        print(len(names), 'armes')
