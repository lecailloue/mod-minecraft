"""Dragon (Smaug) : tres grand, tres detaille. Meme chaine que les betes (cubes + groupes -> bbmodel -> JSON du mod), textures peintes pixel par pixel + calque lumineux.
Usage : python3 dragon3d.py          -> apercus dans /tmp/dragon/
        python3 dragon3d.py gen      -> ecrit geo + texture + calque lumineux dans les assets du mod"""
import os, sys, math
import numpy as np
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import trollcore as tc
from trollcore import put, line, blob, rect, _N
import beasts as bs
from beasts import mkT, vgrain

# ------------------------------------------------------------------ materiaux
tc.MATS.update({
    'dscale':  dict(base=(118, 34, 26), n1=5, n2=6, tint=(1, .9, .85)),      # ecailles rouge sombre
    'dscale2': dict(base=(76, 24, 20), n1=4, n2=5, tint=(1, .9, .86)),       # plaques du dos, presque noires
    'dscale3': dict(base=(142, 50, 30), n1=5, n2=6, tint=(1, .92, .8)),      # flancs, cuisses : plus clair, cuivre
    'dbelly':  dict(base=(186, 138, 52), n1=5, n2=6, tint=(1.05, 1, .8)),    # ventre dore incruste de joyaux
    'dmemb':   dict(base=(140, 46, 34), n1=6, n2=7, tint=(1, .88, .86)),     # membrane des ailes
    'dhorn':   dict(base=(54, 38, 32), n1=4, n2=5, tint=(1, .95, .9)),       # cornes, pointes
    'dmouth':  dict(base=(122, 28, 26), n1=6, n2=7, tint=(1, .9, .9)),       # gueule
    'dtooth':  dict(base=(226, 212, 176), n1=4, n2=5, tint=(1, .97, .88)),
    'dclaw':   dict(base=(34, 26, 24), n1=3, n2=4, tint=(1, .95, .95)),
})


def d_scales(light, dark, size=4, var=.12):
    def f(arr, face, rng):
        h, w, _ = arr.shape
        k, y0 = 0, 0
        while y0 < h:
            off = (k % 2) * (size // 2); k += 1
            for x0 in range(-off, w, size):
                tone = 1 + (rng.random() - .5) * 2 * var
                for j in range(size - 1):
                    for i in range(size):
                        x, y = x0 + i, y0 + j
                        if 0 <= x < w and 0 <= y < h: arr[y, x] *= tone
                for i in range(size):
                    put(arr, x0 + i, y0 + size - 2, dark, .5)
                    if 0 < i < size - 1: put(arr, x0 + i, y0, light, .30)
                put(arr, x0, y0 + 1, dark, .3)
            y0 += size - 1
        for _ in range(h * w // 40):
            put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (36, 14, 10), .45)
        for _ in range(h * w // 90):
            put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (205, 120, 70), .35)       # reflets cuivres
    return f


def d_belly(arr, face, rng):
    """plaques du ventre : bandes + joyaux et pieces d'or incrustes dans la peau"""
    h, w, _ = arr.shape
    for y in range(0, h, 3):
        arr[y] *= .6
        if y + 1 < h: arr[y + 1] *= 1.1
    for x in range(int(rng.integers(0, 5)), w, int(rng.integers(5, 9))):
        arr[:, x] *= .8
    gems = [(196, 36, 36), (40, 160, 96), (60, 110, 226), (244, 238, 222), (236, 196, 48), (150, 60, 200)]
    for _ in range(max(1, h * w // 34)):
        x = int(rng.integers(0, max(1, w - 1))); y = int(rng.integers(0, max(1, h - 1)))
        c = gems[int(rng.integers(0, len(gems)))]
        for dx in (0, 1):
            for dy in (0, 1): put(arr, x + dx, y + dy, c, .95)
        put(arr, x, y, (255, 255, 255), .5)
    for _ in range(h * w // 12):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (255, 224, 120), .7)


def d_memb(arr, face, rng):
    """membrane : taches, nervures sombres qui partent des doigts, dechirures"""
    h, w, _ = arr.shape
    g = rng.random((h // 3 + 2, w // 3 + 2)); g = np.kron(g, np.ones((3, 3)))[:h, :w]
    arr *= (.82 + .34 * g[:, :, None])
    for _ in range(max(3, w // 4)):
        x = float(rng.integers(0, w)); dx = float(rng.random() - .5) * .5
        for y in range(h):
            x += dx + (rng.random() - .5) * .4
            put(arr, int(x), y, (62, 16, 16), .6)
            if rng.random() < .06: dx = float(rng.random() - .5) * .9
    for _ in range(max(1, h * w // 90)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, (40, 12, 12), .5, rng)      # brulures
    arr[:, 0] *= .8; arr[:, -1] *= .8


def d_horn(arr, face, rng):
    h, w, _ = arr.shape
    vgrain(arr, rng, .32, 2)
    for _ in range(h * w // 14):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (124, 100, 84), .35)


def d_mouth(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(max(2, w // 3)):
        x = int(rng.integers(0, w)); y = 0;
        while y < h:
            put(arr, x, y, (70, 10, 12), .5); y += 1; x += int(rng.integers(-1, 2))
    for _ in range(h * w // 20): put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (210, 90, 80), .4)


def d_tooth(arr, face, rng):
    vgrain(arr, rng, .10, 2)
    h, w, _ = arr.shape
    if face not in ('up', 'down') and h > 2: arr[:2] *= 1.0; arr[-1:] *= .72     # bout jauni


tc.DECOR.update({
    'dscale': d_scales((220, 130, 80), (40, 12, 10), 4, .12), 'dscale2': d_scales((150, 70, 50), (20, 6, 6), 5, .10),
    'dscale3': d_scales((236, 150, 90), (60, 20, 12), 4, .13),
    'dbelly': d_belly, 'dmemb': d_memb, 'dhorn': d_horn, 'dmouth': d_mouth, 'dtooth': d_tooth, 'dclaw': d_horn,
})


# ------------------------------------------------------------------ effets (calque lumineux)
def fx_deye(face_x=1, y=2):
    """gros oeil ambre a pupille fendue, sur les deux cotes de la tete (brille dans le noir)"""
    def f(F, rng, glow):
        for fc in ('east', 'west'):
            a = F[fc]; h, w, _ = a.shape
            x = (w - 6 - face_x) if fc == 'east' else (1 + face_x)
            for dx in range(5):
                for dy in range(3):
                    if (dx in (0, 4) and dy == 0) or (dx in (0, 4) and dy == 2 and False): continue
                    c = (255, 214, 70) if dy == 0 else (255, 160, 30)
                    put(a, x + dx, y + dy, c); glow(fc, x + dx, y + dy, (255, 150, 20))
            for dy in range(3):
                put(a, x + 2, y + dy, (14, 6, 4)); glow(fc, x + 2, y + dy, (0, 0, 0))
            for dx in range(-1, 6): put(a, x + dx, y - 1, (30, 8, 6), .8)
    return f


def fx_embers(n=14, faces=('north', 'east', 'west', 'south', 'down')):
    """braises : fissures incandescentes entre les plaques"""
    def f(F, rng, glow):
        for fc in faces:
            a = F[fc]; h, w, _ = a.shape
            for _ in range(max(1, int(n * w * h / 300))):
                x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
                put(a, x, y, (255, 150, 40), .95); glow(fc, x, y, (255, 100, 20))
                if rng.random() < .5: put(a, x + 1, y, (255, 120, 30), .8); glow(fc, x + 1, y, (230, 80, 16))
    return f


def fx_nostril(F, rng, glow):
    a = F['north']; h, w, _ = a.shape
    for x in (1, w - 3):
        for dy in range(2):
            put(a, x, 1 + dy, (30, 8, 6)); put(a, x + 1, 1 + dy, (255, 130, 30)); glow('north', x + 1, 1 + dy, (255, 90, 20))


def fx_mouth(F, rng, glow):
    for fc in ('up', 'north', 'east', 'west'):
        a = F[fc]; h, w, _ = a.shape
        for _ in range(h * w // 5):
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
            put(a, x, y, (255, 120, 40), .5); glow(fc, x, y, (200, 60, 14))


# ================================================================== geometrie
def ang(v):
    return math.radians(v)


def dragon_groups():
    B, G = mkT(1.0)
    s_ = lambda s: 'r' if s > 0 else 'l'
    # ---------------------------------------------------------------- corps
    body_kids = [
        B('chest', -15, 32, -34, 30, 28, 26, 'dscale', key='d_chest'),
        B('mid', -14, 31, -10, 28, 26, 22, 'dscale', key='d_mid'),
        B('hips', -15, 30, 10, 30, 26, 22, 'dscale', key='d_hips'),
        B('belly', -11, 29, -33, 22, 3, 62, 'dbelly', key='d_belly', fx=fx_embers(8, ('down',))),
        B('breast', -10, 32, -36, 20, 22, 3, 'dbelly', key='d_breast', fx=fx_embers(26, ('north',))),
        B('shoulder_r', 11, 50, -32, 10, 10, 16, 'dscale2', key='d_shoulder'),
        B('shoulder_l', -21, 50, -32, 10, 10, 16, 'dscale2', key='d_shoulder'),
        B('flank_r', 14, 36, -8, 3, 16, 36, 'dscale3', key='d_flank'),
        B('flank_l', -17, 36, -8, 3, 16, 36, 'dscale3', key='d_flank'),
    ]
    for i in range(9):                                            # plaques du dos
        yb = 60 - i * 0.45
        body_kids.append(B(f'plate{i}', -9, yb, -32 + i * 8.4, 18, 3, 8, 'dscale2', key='d_plate'))
    for i in range(12):                                           # crete de pointes : plus hautes aux epaules
        z = -32 + i * 6.4; h = max(5, 13 - i * .75); y = 60 - i * .45 + 1
        b1 = max(2, round(h * .6)); b2 = max(2, round(h * .5))
        body_kids.append(G(f'dsp{i}', (0, y, z), rot=(16, 0, 0), children=[
            B(f'dsp_a{i}', -3, y, z - 2, 6, b1, 4, 'dhorn', key=f'd_spa{b1}'),
            B(f'dsp_b{i}', -1.5, y + b1, z - 1, 3, b2, 2, 'dhorn', key=f'd_spb{b2}')]))
    # ---------------------------------------------------------------- cou (S) : segments inclines + ventre dore + pointes dorsales
    pos = np.array([0.0, 56.0, -32.0])
    angles = [8, 20, 32, 44, 54, 60]; widths = [20, 18, 17, 15, 14, 13]; length = 14
    neck_kids = []
    for i, (th, wd) in enumerate(zip(angles, widths)):
        dvec = np.array([0, math.cos(ang(th)), -math.sin(ang(th))]); nvec = np.array([0, -math.sin(ang(th)), -math.cos(ang(th))])
        dep = wd - 2
        c = pos + dvec * length / 2
        neck_kids.append(B(f'neck{i}', c[0] - wd / 2, c[1] - length / 2, c[2] - dep / 2, wd, length, dep, 'dscale', key=f'd_neck{i}', rot=(-th, 0, 0)))
        cb = c + nvec * (dep / 2 + .8)
        neck_kids.append(B(f'nbelly{i}', cb[0] - wd * .3, cb[1] - length / 2, cb[2] - 1, round(wd * .6), length, 2, 'dbelly', key=f'd_nb{i}', rot=(-th, 0, 0), fx=fx_embers(10, ('north',))))
        cs = c - nvec * (dep / 2 + 3)
        neck_kids.append(B(f'nspike{i}', cs[0] - 1.5, cs[1] - 4, cs[2] - 1.5, 3, 8, 3, 'dhorn', key='d_nsp', rot=(90 - th + 8, 0, 0)))
        cs2 = c - nvec * (dep / 2 + .8)
        neck_kids.append(B(f'ncrest{i}', cs2[0] - 4, cs2[1] - length / 2, cs2[2] - 1, 8, length, 2, 'dscale2', key=f'd_ncr{i}', rot=(-th, 0, 0)))
        pos = pos + dvec * length * .92
    hx, hy, hz = pos
    # ---------------------------------------------------------------- tete (repere local : la tete regarde vers -z ; pivot au joint du cou)
    hz0 = hz
    head_kids = [
        B('skull', -10, hy - 8, hz0 - 22, 20, 16, 24, 'dscale', key='d_skull', fx=fx_deye(1, 3)),
        B('brow', -11, hy + 7, hz0 - 22, 22, 3, 12, 'dscale2', key='d_brow'),
        B('crown', -7, hy + 8, hz0 - 8, 14, 3, 14, 'dscale2', key='d_crown'),
        B('snout', -7, hy - 6, hz0 - 48, 14, 10, 28, 'dscale', key='d_snout'),
        B('snout_top', -6, hy + 3, hz0 - 46, 12, 2, 22, 'dscale2', key='d_stop'),
        B('nose', -6.5, hy - 5, hz0 - 54, 13, 9, 6, 'dscale3', key='d_nose', fx=fx_nostril),
        B('nose_horn', -1.5, hy + 4, hz0 - 50, 3, 4, 7, 'dhorn', key='d_nhorn'),
        B('roof', -6, hy - 7, hz0 - 46, 12, 1, 38, 'dmouth', key='d_roof', fx=fx_mouth),
        B('lip_r', 6.5, hy - 7, hz0 - 50, 2, 3, 36, 'dscale3', key='d_lip'),
        B('lip_l', -8.5, hy - 7, hz0 - 50, 2, 3, 36, 'dscale3', key='d_lip'),
    ]
    for s in (1, -1):                                             # dents du haut
        for k in range(8):
            th_ = 4 + (k % 2) * 2
            head_kids.append(B(f'tooth_u{s_(s)}{k}', s * 7.2 - 1, hy - 7 - th_, hz0 - 50 + k * 4.6, 2, th_, 2, 'dtooth', key=f'd_tu{th_}'))
        head_kids.append(B(f'canine_u{s_(s)}', s * 7.4 - 1.5, hy - 14, hz0 - 49, 3, 9, 3, 'dtooth', key='d_canu'))
        head_kids.append(B(f'tusk_{s_(s)}', s * 9 - 1.5, hy - 2, hz0 - 40, 3, 3, 10, 'dhorn', key='d_tuskb'))
    for k in range(4): head_kids.append(B(f'tooth_f{k}', -4.5 + k * 3, hy - 11, hz0 - 54.5, 2, 5, 2, 'dtooth', key='d_tf'))
    for s in (1, -1):                                             # cornes balayees vers l'arriere (escalier), cornes de joue, nageoires
        x0 = s * 8
        for i, (dx, dy, dz, w, h, d) in enumerate(((0, 0, -2, 5, 6, 9), (1, 2, 7, 4, 5, 9), (2, 5, 15, 3, 4, 9), (3, 9, 22, 2, 3, 8))):
            head_kids.append(B(f'horn{i}_{s_(s)}', x0 + s * dx - w / 2, hy + 7 + dy, hz0 - 4 + dz, w, h, d, 'dhorn', key=f'd_horn{i}'))
        for i, (dx, dy, dz, w, h, d) in enumerate(((0, 0, 0, 3, 3, 8), (1, 1, 7, 2, 2, 6))):
            head_kids.append(B(f'horns{i}_{s_(s)}', s * 9.5 + s * dx - w / 2, hy + 8 + dy, hz0 - 18 + dz, w, h, d, 'dhorn', key=f'd_hornb{i}'))
        for i in range(3):
            head_kids.append(B(f'cheek{i}_{s_(s)}', s * 11 - 1.5, hy - 3 + i * 0, hz0 - 4 - i * 0, 3, 3, 12, 'dhorn', key='d_cheek', rot=(0, s * (38 + i * 12), 0), pivot=(s * 11, hy - 1.5 - i * 3, hz0 - 4)))
        head_kids.append(B(f'fin_{s_(s)}', s * 11 - .5, hy - 6, hz0 - 4, 1, 14, 16, 'dmemb', key='d_fin'))
        head_kids.append(B(f'browh_{s_(s)}', s * 10 - 1.5, hy + 8, hz0 - 20, 3, 4, 5, 'dhorn', key='d_browh'))
    jaw = G('jaw', (0, hy - 8, hz0 - 2), children=[
        B('jaw_main', -8, hy - 14, hz0 - 50, 16, 6, 48, 'dscale', key='d_jaw'),
        B('chin', -5, hy - 19, hz0 - 46, 10, 5, 32, 'dbelly', key='d_chin'),
        B('tongue', -4, hy - 8.5, hz0 - 44, 8, 2, 36, 'dmouth', key='d_tongue', fx=fx_mouth),
        B('chin_spike', -1.5, hy - 17, hz0 - 52, 3, 8, 3, 'dhorn', key='d_chinsp'),
        B('jaw_spike_r', 7, hy - 16, hz0 - 24, 2, 6, 3, 'dhorn', key='d_jsp'),
        B('jaw_spike_l', -9, hy - 16, hz0 - 24, 2, 6, 3, 'dhorn', key='d_jsp'),
    ] + [B(f'tooth_l{s_(s)}{k}', s * 6.2 - 1, hy - 8.5, hz0 - 48 + k * 5.2, 2, 4 + (k % 2), 2, 'dtooth', key=f'd_tl{k % 2}') for s in (1, -1) for k in range(7)]
      + [B(f'canine_l{s_(s)}', s * 5.2 - 1.5, hy - 12, hz0 - 49, 3, 7, 3, 'dtooth', key='d_canl') for s in (1, -1)])
    head = G('head', (0, hy - 8, hz0), rot=(-18, 0, 0), children=head_kids + [jaw])
    neck = G('neck', (0, 56, -32), children=neck_kids + [head])
    body = G('body', (0, 40, 0), children=body_kids + [neck])
    # ---------------------------------------------------------------- ailes : bras, avant-bras, 4 doigts en eventail, membranes en panneaux
    wings = []
    for s in (1, -1):
        n = s_(s)
        sx = 14 * s
        kids = []

        def seg(name, p0, direction_deg, L, th, mat, key, y):
            """cube long de L dans le plan horizontal, de p0 vers la direction (angle depuis +x vers +z) ; retourne (cube, bout)"""
            dx, dz = math.cos(ang(direction_deg)), math.sin(ang(direction_deg))
            c = (p0[0] + s * dx * L / 2, p0[1] + dz * L / 2)
            cube = B(name, c[0] - L / 2, y - th / 2, c[1] - th / 2, L, th, th, mat, key=key, rot=(0, -(direction_deg if s > 0 else -direction_deg) , 0))
            return cube, (p0[0] + s * dx * L, p0[1] + dz * L)
        yw = 60
        shoulder = (sx, -18)
        arm, elbow = seg(f'arm_{n}', shoulder, 8, 26, 9, 'dscale', 'd_warm', yw)
        fore, wrist = seg(f'fore_{n}', elbow, -38, 36, 7, 'dscale2', 'd_wfore', yw + 2)
        kids += [arm, fore]
        kids.append(B(f'wclaw_{n}', wrist[0] - 1.5, yw + 2, wrist[1] - 1.5, 3, 10, 3, 'dclaw', key='d_wclaw'))
        kids.append(B(f'wspike_{n}', elbow[0] - 2, yw + 3, elbow[1] - 2, 4, 8, 4, 'dhorn', key='d_wspike'))
        fingers = [(4, 96), (28, 90), (52, 78), (76, 64)]
        tips = []
        for k, (psi, Lf) in enumerate(fingers):
            fb, tip = seg(f'finger{k}_{n}', wrist, psi, Lf, 4, 'dscale2', f'd_fing{k}', yw + 2)
            kids.append(fb); tips.append((psi, Lf, tip))
        # membranes : entre chaque paire de doigts (deux rectangles : interieur etroit, exterieur large)
        edges = [(psi, Lf) for psi, Lf, _ in tips] + [(100, 60)]
        for k in range(len(edges) - 1):
            (p1, L1), (p2, L2) = edges[k], edges[k + 1]
            pm = (p1 + p2) / 2; Lm = (L1 + L2) / 2; dpsi = abs(math.radians(p2 - p1))
            for part, (f0, f1, wf) in enumerate(((.08, .5, .5), (.5, .97, .95))):
                ln = (f1 - f0) * Lm
                dd = (f0 + f1) / 2 * Lm
                dx, dz = math.cos(ang(pm)), math.sin(ang(pm))
                c = (wrist[0] + s * dx * dd, wrist[1] + dz * dd)
                wd = max(3, 2 * Lm * math.sin(dpsi / 2) * wf * (.5 + .5 * f1))
                kids.append(B(f'memb{k}{part}_{n}', c[0] - ln / 2, yw + 1.2, c[1] - wd / 2, ln, 1, wd, 'dmemb', key=f'd_memb{k}{part}',
                              rot=(0, -(pm if s > 0 else -pm), 0)))
        # panneau entre le bras et le flanc (membrane arriere) : trois bandes
        for j, (zz, L) in enumerate(((-12, 44), (2, 48), (16, 42))):
            kids.append(B(f'rear{j}_{n}', sx + (s * 3 if s > 0 else -3 - L) + (0 if s > 0 else 0), yw + .6, zz, L, 1, 14, 'dmemb', key=f'd_rear{j}'))
        wings.append(G(f'wing_{n}', (sx, yw, -18), rot=(0, 0, s * 26), children=kids))
    for w in wings: body.children.append(w)
    # ---------------------------------------------------------------- pattes
    legs = []

    def leg(nm, s, z, hind):
        x = 17 * s
        if hind:
            kids = [B(f'{nm}_thigh', x - 10, 22, z - 10, 20, 20, 20, 'dscale3', key='d_thigh'),
                    B(f'{nm}_knee', x - 3, 30, z - 13, 6, 6, 5, 'dhorn', key='d_knee'),
                    B(f'{nm}_shin', x - 6.5, 9, z + 1, 13, 18, 13, 'dscale3', key='d_shin'),
                    B(f'{nm}_heel', x - 2, 8, z + 11, 4, 4, 8, 'dhorn', key='d_heel'),
                    B(f'{nm}_foot', x - 8.5, 0, z - 12, 17, 6, 24, 'dscale2', key='d_foot')]
            for k in (-1, 0, 1):
                kids.append(B(f'{nm}_claw{k}', x + k * 4.5 - 1.5, 0, z - 20, 3, 4, 9, 'dclaw', key='d_claw'))
        else:
            kids = [B(f'{nm}_arm', x - 8, 20, z - 8, 16, 20, 16, 'dscale3', key='d_uarm'),
                    B(f'{nm}_elbow', x + (4 if s > 0 else -8), 24, z + 4, 4, 5, 6, 'dhorn', key='d_elb'),
                    B(f'{nm}_fore', x - 6.5, 6, z - 5, 13, 16, 13, 'dscale3', key='d_farm'),
                    B(f'{nm}_hand', x - 8.5, 0, z - 13, 17, 6, 20, 'dscale2', key='d_hand')]
            for k in (-1, 0, 1):
                kids.append(B(f'{nm}_claw{k}', x + k * 4.5 - 1.5, 0, z - 19, 3, 4, 8, 'dclaw', key='d_fclaw'))
        return G(nm, (x, 38, z), children=kids)
    legs += [leg('leg_fr', 1, -18, False), leg('leg_fl', -1, -18, False), leg('leg_br', 1, 24, True), leg('leg_bl', -1, 24, True)]
    # ---------------------------------------------------------------- queue : trois sections articulees + dard
    t3k = [B('t3a', -6, 28, 82, 12, 13, 28, 'dscale', key='d_t3a'), B('t3b', -4.5, 27, 110, 9, 10, 22, 'dscale', key='d_t3b'),
           B('t3c', -3, 26, 132, 6, 8, 20, 'dscale', key='d_t3c'), B('t3d', -2, 25.5, 152, 4, 6, 14, 'dscale2', key='d_t3d'),
           B('t3_belly', -3, 27, 82, 6, 2, 50, 'dbelly', key='d_t3belly')]
    for i in range(5): t3k.append(B(f't3sp{i}', -1.5, 41 - i * 1.2, 86 + i * 14, 3, 6 - (i > 2), 3, 'dhorn', key=f'd_tsp{i > 2}'))
    t3k += [B('barb_a', -1, 22, 160, 2, 6, 10, 'dclaw', key='d_barb'),
            G('barb_l', (0, 28, 160), rot=(0, 28, 0), children=[B('barb_lb', 0, 27, 160, 2, 3, 12, 'dclaw', key='d_barbf')]),
            G('barb_r', (0, 28, 160), rot=(0, -28, 0), children=[B('barb_rb', -2, 27, 160, 2, 3, 12, 'dclaw', key='d_barbf')])]
    t2k = [B('t2a', -8, 31, 56, 16, 17, 26, 'dscale', key='d_t2a'), B('t2_belly', -5, 30, 56, 10, 2, 26, 'dbelly', key='d_t2belly')]
    for i in range(2): t2k.append(B(f't2sp{i}', -2, 49 - i * 1.5, 60 + i * 12, 4, 7, 4, 'dhorn', key='d_t2sp'))
    t2 = G('tail2', (0, 38, 56), children=t2k + [G('tail3', (0, 36, 82), children=t3k)])
    t1k = [B('t1a', -10, 30, 32, 20, 22, 24, 'dscale', key='d_t1a'), B('t1_belly', -7, 29, 32, 14, 2, 24, 'dbelly', key='d_t1belly')]
    for i in range(2): t1k.append(B(f't1sp{i}', -2.5, 53 - i * 1, 36 + i * 11, 5, 8, 5, 'dhorn', key='d_t1sp'))
    tail = G('tail', (0, 42, 32), children=t1k + [t2])
    return legs + [body, tail]


# ================================================================== generation / apercu
def build(outdir='/tmp/dragon'):
    _N[0] = 0
    groups = dragon_groups()
    tc.normalize(groups)
    cubes = [c for c, _ in tc.walk(groups)]
    W, H = tc.pack(cubes)
    tex, glow = tc.paint_all(cubes, W, H)
    os.makedirs(outdir, exist_ok=True)
    tc.export_bbmodel('dragon', groups, cubes, W, H, tex, os.path.join(outdir, 'dragon.bbmodel'))
    return groups, tex, glow, W, H, len(cubes)


def preview(groups, tex, out='/tmp/dragon', yaws=(35, 125, 215, 305), scale=2.3, W=760, H=700, pitch=-12):
    os.makedirs(out, exist_ok=True)
    ims = [Image.fromarray(tc.render(groups, tex, y, pitch=pitch, W=W, H=H, scale=scale, mode='all')) for y in yaws]
    sheet = Image.new('RGB', (W * 2, H * 2))
    for i, im in enumerate(ims): sheet.paste(im, ((i % 2) * W, (i // 2) * H))
    sheet.save(f'{out}/dragon.png')
    Image.fromarray(tex, 'RGBA').save(f'{out}/dragon_tex.png')


def gen():
    import bbimport
    from genlib import wjson, save, A
    groups, tex, glow, W, H, k = build(os.path.join(HERE, 'assets'))
    src = os.path.join(HERE, 'assets', 'dragon.bbmodel')
    wjson(f'{A}/geo/dragon.json', bbimport.convert(src))
    save(bbimport.texture(src), f'{A}/textures/entity/dragon.png')
    Image.fromarray(glow, 'RGBA').save(f'{A}/textures/entity/dragon_glow.png')
    print('dragon', k, 'cubes', W, H)


if __name__ == '__main__' and sys.argv[1:2] == ['gen']:
    gen()
elif __name__ == '__main__':
    g, tex, glow, W, H, k = build()
    print('cubes', k, 'texture', W, H)
    preview(g, tex)
