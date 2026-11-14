"""Dragon des Maitres des ombres (modele ORIGINAL, fichiers 3D seulement).
Dragon-squelette d'ombre : crane a couronne de cornes, cotes lumineuses, ailes aux longs doigts d'os et membrane dechiree violette,
queue a lame en croissant. 1 unite = 5 cm. y haut, avant = -z, gauche = -x. Pose ailes deployees, symetrique.
Usage : python3 tools/dragon_ombre.py [sortie]  ->  dragon_ombre.bbmodel / .json / .png / _glow.png + apercu.png"""
import math, os, sys, json
import numpy as np
import trollcore as T
from trollcore import Group
from orcs import C
import weapons3d as W

# ---------------------------------------------------------------- materiaux
VIO = (178, 120, 255)
T.MATS.update({
    'dr_skin': dict(base=(36, 33, 46), n1=5, n2=6, tint=(1, .98, 1.1)),
    'dr_plate': dict(base=(66, 60, 80), n1=6, n2=6, tint=(1, .98, 1.08)),
    'dr_bone': dict(base=(148, 140, 156), n1=6, n2=6, tint=(1, .98, 1.04)),
    'dr_horn': dict(base=(52, 46, 62), n1=5, n2=6, tint=(1, 1, 1.08)),
    'dr_mem': dict(base=(50, 38, 78), n1=5, n2=6, tint=(1, .95, 1.15)),
    'dr_glow': dict(base=(150, 96, 240), n1=3, n2=3, tint=(1, 1, 1)),
})
T.ALPHA['dr_mem'] = 185


def d_scales(arr, face, rng):
    h, w, _ = arr.shape
    arr[:] *= (1 + (rng.random((h, w, 1)) - .5) * .18)
    for y in range(h):
        for x in range(w):
            if y % 3 == 0 or (x + (y // 3) * 2) % 4 == 0:
                arr[y, x] *= .76
    for _ in range(max(1, h * w // 25)):
        arr[int(rng.integers(0, h)), int(rng.integers(0, w))] += (16, 8, 30)


def d_membrane(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= 1 + (rng.random() - .5) * .25
    for y in range(h):
        arr[y, :] *= 1 + (rng.random() - .5) * .12
    for _ in range(max(1, h * w // 16)):                                    # veinules plus claires
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        for i in range(int(rng.integers(3, 8))):
            if x + i < w:
                arr[y, x + i] = arr[y, x + i] * .6 + np.array((120, 84, 170)) * .4


def d_bonep(arr, face, rng):
    h, w, _ = arr.shape
    arr[:] *= (1 + (rng.random((h, w, 1)) - .5) * .2)
    for _ in range(max(1, h * w // 12)):
        T.line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(2, 6)), (24, 20, 30), .45)


T.DECOR.update({'dr_skin': d_scales, 'dr_plate': d_scales, 'dr_bone': d_bonep, 'dr_horn': d_bonep, 'dr_mem': d_membrane, 'dr_glow': W.d_gem})


def fx_edges_dim(col):
    def f(F, rng, glow):
        for face, a in F.items():
            h, w, _ = a.shape
            for y in range(h):
                for x in range(w):
                    if (x == 0 or y == 0) and rng.random() < .8:
                        a[y, x] = np.array(col, float)
                        glow(face, x, y, col)
    return f


# ---------------------------------------------------------------- outils geometriques
def P(x, y, z):
    return np.array((x, y, z), float)


def bone(name, a, b, w, mat, key=None, fx=None, d=None, extra=1.0):
    """cube long de a vers b (section w x d), tourne autour de a"""
    a, b = np.array(a, float), np.array(b, float)
    v = b - a
    ln = float(np.linalg.norm(v))
    n = v / ln
    rx = math.degrees(math.asin(max(-1, min(1, n[2]))))
    rz = math.degrees(math.atan2(-n[0], n[1]))
    d = d or w
    return C(name, a[0] - w / 2, a[1], a[2] - d / 2, w, int(round(ln + extra)), d, mat, key=key or name, rot=(rx, 0, rz), pivot=tuple(a), fx=fx)


def taper(prefix, pts, ws, mat, fx=None, key=None):
    return [bone(f'{prefix}{i}', pts[i], pts[i + 1], ws[i], mat, key=(f'{key}{i}' if key else None), fx=fx) for i in range(len(pts) - 1)]


def slat(name, A, B, depth, mat, fx=None, key=None):
    """lame de membrane : barre plate de A vers B, epaisseur 1, profondeur depth"""
    A, B = np.array(A, float), np.array(B, float)
    d = B - A
    L = max(2, int(round(np.linalg.norm(d))))
    M = (A + B) / 2
    yaw = math.degrees(math.atan2(-d[2], d[0]))
    tilt = math.degrees(math.atan2(d[1], math.hypot(d[0], d[2])))
    cube = C(name, M[0] - L / 2, M[1] - .5, M[2] - depth / 2, L, 1, max(1, int(depth)), mat, key=key or name, fx=fx, pivot=tuple(M))
    return Group(name + '_y', tuple(M), (0, yaw, 0), [Group(name + '_t', tuple(M), (0, 0, tilt), [cube])])


def lerp(a, b, t):
    return a + (b - a) * t


# ---------------------------------------------------------------- le dragon
def dragon():
    k = []
    rng = np.random.default_rng(7)
    glowc = fx_edges_dim(VIO)
    cr = W.fx_cracks(VIO)
    gem = W.fx_gem(VIO)

    # --- tronc
    k.append(C('torso', -10, 38, -12, 20, 18, 32, 'dr_skin', key='torso', fx=None))
    k.append(C('chest', -8, 34, -18, 16, 18, 8, 'dr_plate', key='chest'))
    k.append(C('belly', -7, 33, -8, 14, 5, 26, 'dr_plate', key='belly'))
    k.append(C('core', -4, 42, -8, 8, 10, 12, 'dr_glow', key='core', fx=gem))                      # coeur d'ombre visible entre les cotes
    for i, z in enumerate((-9, -5, -1, 3, 7, 11)):                                                  # cotes lumineuses
        for sg in (-1, 1):
            p0, p1, p2 = P(sg * 10, 55, z), P(sg * 13, 47, z + .5), P(sg * 10, 37, z + 1.5)
            k += taper(f'rib{i}_{sg}_', [p0, p1, p2], [2, 2], 'dr_bone', key=f'rib{i}', fx=cr)
    for i in range(10):                                                                             # plaques dorsales
        z = -12 + i * 3.2
        h = 7 - abs(i - 4) * .6
        k.append(bone(f'dors{i}', P(0, 55.5, z), P(0, 55.5 + h, z + 2.4), 3, 'dr_horn', key=f'dors{i}'))

    # --- cou
    neck = [P(0, 50, -14), P(0, 56, -24), P(0, 65, -32), P(0, 74, -37), P(0, 81, -40)]
    k += taper('neck', neck, [10, 9, 8, 7], 'dr_skin', fx=None, key='neck')
    for i in range(1, 4):
        k.append(bone(f'nspine{i}', neck[i] + P(0, 3, 0), neck[i] + P(0, 8 - i, 3), 2, 'dr_horn', key=f'nsp{i}'))
        for sg in (-1, 1):
            k.append(C(f'nglow{i}_{sg}', sg * 4.2 - .5, neck[i][1] - 1, neck[i][2] - 1, 1, 3, 2, 'dr_glow', key='nglow', fx=gem))

    # --- tete (crane, museau, machoire, dents, cornes)
    hb = P(0, 81, -40)
    k += taper('skull', [hb, P(0, 79, -46), P(0, 76, -53), P(0, 73, -60)], [10, 8, 6], 'dr_plate', key='skull')
    k.append(C('brow', -5, 82, -46, 10, 2, 6, 'dr_horn', key='brow'))
    k += taper('jaw', [P(0, 76, -45), P(0, 71, -53), P(0, 67, -61)], [5, 4], 'dr_skin', key='jaw')
    for i in range(6):
        for sg in (-1, 1):
            k.append(C(f'tooth_u{i}_{sg}', sg * 2.4 - .5, 74.5 - i * .5, -52 - i * 1.8, 1, 2, 1, 'dr_bone', key='tooth'))
            k.append(C(f'tooth_l{i}_{sg}', sg * 2.4 - .5, 70.5 - i * .4, -54 - i * 1.6, 1, 2, 1, 'dr_bone', key='tooth2'))
    k.append(C('mouth_glow', -1, 71, -56, 2, 2, 6, 'dr_glow', key='mglow', fx=gem))
    for sg in (-1, 1):
        k.append(C(f'eye{sg}', sg * 4.8 - .5, 80, -48, 1, 2, 3, 'dr_glow', key='eye', fx=gem))
        k.append(C(f'nostril{sg}', sg * 1.8 - .5, 75, -62, 1, 1, 1, 'dr_glow', key='nos', fx=gem))
    for sg in (-1, 1):                                                                              # couronne de cornes
        for j, (ox, h, lean) in enumerate(((2.5, 9, 6), (4, 12, 11), (5, 8, 16))):
            p0 = P(sg * ox, 85, -41 + j)
            p1 = p0 + P(sg * (2 + j * 1.5), h * .35, 5 + lean * .5)
            p2 = p1 + P(sg * (1.5 + j), h * .25, 6 + lean * .6)
            k += taper(f'horn{j}_{sg}_', [p0, p1, p2], [3, 2], 'dr_horn', key=f'horn{j}')
        p0 = P(sg * 5, 74, -46)                                                                      # eperons de joue
        k.append(bone(f'cheek{sg}', p0, p0 + P(sg * 6, -3, 5), 2, 'dr_horn', key='cheek'))

    # --- queue
    tail = [P(0, 46, 18), P(0, 43, 28), P(0, 40, 38), P(0, 40, 48), P(0, 43, 58), P(0, 48, 66), P(0, 53, 72), P(0, 56, 77)]
    k += taper('tail', tail, [9, 8, 7, 6, 5, 4, 3], 'dr_skin', key='tail')
    for i in range(1, 6):
        k.append(bone(f'tspine{i}', tail[i] + P(0, 3, 0), tail[i] + P(0, 7 - i * .7, 2.5), 2, 'dr_horn', key=f'tsp{i}'))
    tip = tail[-1]                                                                                  # lame en croissant
    k.append(bone('tblade0', tip, tip + P(0, 6, 4), 3, 'dr_plate', key='tbl0'))
    k.append(bone('tblade1', tip + P(0, 6, 4), tip + P(0, 14, 5), 3, 'dr_plate', key='tbl1'))
    for sg in (-1, 1):
        k.append(bone(f'tblade_s{sg}', tip + P(0, 4, 3), tip + P(sg * 8, 8, 8), 2, 'dr_plate', key='tbls'))
        k.append(C(f'tblade_g{sg}', tip[0] + sg * 3 - .5, tip[1] + 3, tip[2] + 3, 1, 3, 2, 'dr_glow', key='tbg', fx=gem))

    # --- pattes
    for sg in (-1, 1):
        # arriere : cuisse, jarret, pied + 3 griffes
        hip = P(sg * 10, 44, 10)
        k += taper(f'hleg{sg}_', [hip, P(sg * 13, 35, 14), P(sg * 11, 26, 9), P(sg * 11, 21, 3)], [7, 5, 4], 'dr_skin', key='hleg')
        for c in range(3):
            fo = P(sg * (11 + (c - 1) * 2.2), 21, 3)
            k.append(bone(f'hclaw{sg}_{c}', fo, fo + P(sg * (c - 1) * 1.2, -4, -6), 2, 'dr_bone', key='hcl'))
        # avant : plus courtes, repliees
        sh = P(sg * 10, 42, -10)
        k += taper(f'fleg{sg}_', [sh, P(sg * 12, 35, -9), P(sg * 10, 28, -15), P(sg * 10, 25, -19)], [6, 4, 3], 'dr_skin', key='fleg')
        for c in range(3):
            fo = P(sg * (10 + (c - 1) * 2), 25, -19)
            k.append(bone(f'fclaw{sg}_{c}', fo, fo + P(sg * (c - 1) * 1.2, -4, -5), 2, 'dr_bone', key='fcl'))

    # --- ailes : bras, avant-bras, 4 doigts + pouce, membrane dechiree
    for sg in (-1, 1):
        S = P(sg * 10, 54, -6)
        E = P(sg * 25, 64, -2)
        Wr = P(sg * 42, 77, -13)
        k += taper(f'wing{sg}_arm', [S, E, Wr], [5, 4], 'dr_skin', key='warm')
        k.append(C(f'wing{sg}_shoulder', sg * 10 - 3, 51, -9, 6, 8, 6, 'dr_plate', key='wsh'))
        tips = [P(sg * 79, 81, -28), P(sg * 77, 67, 2), P(sg * 64, 58, 23), P(sg * 48, 52, 36)]
        for i, tp in enumerate(tips):
            mid = lerp(Wr, tp, .55) + P(0, -3 - i * .8, 2 + i)
            k += taper(f'wing{sg}_f{i}_', [Wr, mid, tp], [3, 2], 'dr_bone', key=f'wf{i}')
            k.append(bone(f'wing{sg}_c{i}', tp, tp + (tp - mid) / np.linalg.norm(tp - mid) * 3 + P(0, -2, 0), 1, 'dr_bone', key=f'wc{i}'))
        k.append(bone(f'wing{sg}_thumb', Wr, Wr + P(sg * 2, 9, -9), 3, 'dr_horn', key='wthumb'))
        hipw = P(sg * 12, 47, 17)
        lines = [Wr] + tips + [hipw]
        # eventail de membrane entre les doigts (depuis le poignet)
        for g in range(len(lines) - 2):
            Ae, Be = lines[g + 1], lines[g + 2]
            nb = 6
            for s in range(nb):
                t = (s + .5) / nb
                depth = max(3, int(min(np.linalg.norm(Ae - Wr), np.linalg.norm(Be - Wr)) / nb))
                a = lerp(Wr, Ae, t); b = lerp(Wr, Be, t)
                if s >= nb - 2:                                                                     # bords dechires
                    cut = rng.random() * .22
                    b = lerp(b, a, cut)
                    if rng.random() < .35:
                        a = lerp(a, b, rng.random() * .18)
                k.append(slat(f'mem{sg}_{g}_{s}', a, b, depth, 'dr_mem', fx=glowc, key=f'mem{g}{s}{depth}'))
        # partie interne : triangle epaule / poignet / hanche
        for s in range(5):
            t = (s + .5) / 5
            a = lerp(S, Wr, t * .96 + .02) + P(0, -2, 1); b = lerp(S, hipw, t) + P(0, -1, 0)
            k.append(slat(f'meminner{sg}_{s}', a, b, 4, 'dr_mem', fx=glowc, key=f'mi{s}'))
    return k


def build_dragon():
    kids = dragon()
    # sol y=0 : on cale le point le plus bas
    T._N[0] = 0
    groups = W.hold(kids)
    from orcs import unique_names
    unique_names(groups)
    T.normalize(groups)
    lo, hi = W.extents(groups)
    return groups, lo, hi


def finish(groups):
    cubes = [c for c, _ in T.walk(groups)]
    for c in cubes:
        fr, sz = list(c.frm), list(c.size)
        for i in range(3):
            if sz[i] < 0:
                fr[i] += sz[i]; sz[i] = -sz[i]
        c.frm, c.size = tuple(fr), tuple(max(1, v) for v in sz)
    Wd, Ht = T.pack(cubes)
    tex, glow = T.paint_all(cubes, Wd, Ht)
    return cubes, Wd, Ht, tex, glow


def preview(groups, tex, path, lo, hi):
    from PIL import Image
    views = [(35, -18), (-145, -18), (0, -70)]
    CW, CH = 560, 560
    cx = (lo + hi) / 2
    ext = max(hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2])
    sc = CW / ext * 1.05
    big = np.zeros((CH, CW * len(views), 3), np.uint8)
    for i, v in enumerate(views):
        img = np.zeros((CH, CW, 3), float)
        yy = np.linspace(0, 1, CH)[:, None, None]
        img[:] = np.array((26, 28, 36)) * (1 - yy) + np.array((54, 58, 70)) * yy
        zb = np.full((CH, CW), -1e9)
        T.render(groups, tex, v[0], v[1], CW, CH, sc, -(cx[1] - 40) , 'opaque', img, zb)
        out = T.render(groups, tex, v[0], v[1], CW, CH, sc, -(cx[1] - 40), 'trans', img, zb)
        out = img if out is None else out
        big[:, i * CW:(i + 1) * CW] = np.clip(out, 0, 255).astype(np.uint8)
    Image.fromarray(big).save(path)


def main(outdir):
    import bbimport, models
    from PIL import Image
    os.makedirs(outdir, exist_ok=True)
    groups, lo, hi = build_dragon()
    cubes, Wd, Ht, tex, glow = finish(groups)
    name = 'dragon_ombre'
    T.export_bbmodel(name, groups, cubes, Wd, Ht, tex, os.path.join(outdir, name + '.bbmodel'))
    geo = bbimport.convert(os.path.join(outdir, name + '.bbmodel'))
    geo['ymin'], geo['ymax'] = models.geo_extent(geo)
    geo['height'] = geo['ymax']
    json.dump(geo, open(os.path.join(outdir, name + '.json'), 'w'), separators=(',', ':'))
    Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, name + '.png'))
    if glow[..., 3].any():
        Image.fromarray(glow, 'RGBA').save(os.path.join(outdir, name + '_glow.png'))
    preview(groups, tex, os.path.join(outdir, 'apercu.png'), lo, hi)
    print(f'{name}: {len(cubes)} cubes, texture {Wd}x{Ht}, envergure {(hi[0]-lo[0])*5/100:.1f} m, longueur {(hi[2]-lo[2])*5/100:.1f} m, hauteur {(hi[1]-lo[1])*5/100:.1f} m')


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '/tmp/dragon')
