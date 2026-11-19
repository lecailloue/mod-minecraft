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


def d_voro(area=14, light=(230, 140, 90), dark=(30, 10, 8), var=.16, hl=.30):
    """ecailles irregulieres (cellules de Voronoi) : tailles et teintes qui varient, aucun motif en grille"""
    def f(arr, face, rng):
        h, w, _ = arr.shape
        n = max(3, int(h * w / area * (.7 + rng.random() * .6)))
        pts = np.column_stack([rng.random(n) * w, rng.random(n) * h]).astype(np.float32)
        ys, xs = np.mgrid[0:h, 0:w]
        gx = (xs + .5).astype(np.float32); gy = (ys + .5).astype(np.float32)
        d = (gx[..., None] - pts[:, 0]) ** 2 + (gy[..., None] - pts[:, 1]) ** 2
        two = np.argpartition(d, 1, axis=2)[:, :, :2]
        da = np.take_along_axis(d, two[:, :, :1], 2)[:, :, 0]; db = np.take_along_axis(d, two[:, :, 1:2], 2)[:, :, 0]
        swap = db < da
        cell = np.where(swap, two[:, :, 1], two[:, :, 0]); d1 = np.sqrt(np.minimum(da, db)); d2 = np.sqrt(np.maximum(da, db))
        tone = 1 + (rng.random(n) - .5) * 2 * var + (rng.random(n) < .12) * (rng.random(n) - .5) * .5     # quelques ecailles tres differentes
        arr *= tone[cell][:, :, None]
        blot = tc.noise(h, w, 7, rng); arr *= (.92 + .18 * blot)[:, :, None]                          # grandes taches claires / sombres
        border = (d2 - d1) < .85
        arr[border] *= .70
        dx = gx - pts[cell, 0]; dy = gy - pts[cell, 1]
        hlm = ((dx + dy) < -.5) & ~border
        arr[hlm] = arr[hlm] * (1 - hl) + np.array(light) * hl
        arr[((dx + dy) > 1.4) & ~border] *= .88
        for _ in range(max(1, h * w // 220)):                                                          # fissures / cicatrices
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h)); dxx = int(rng.integers(-1, 2))
            for i in range(int(rng.integers(3, 8))): put(arr, x + dxx * i // 2, y + i, (22, 8, 6), .7)
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
    'dscale': d_voro(22, var=.11), 'dscale2': d_voro(36, (170, 90, 60), (14, 4, 4), .09), 'dscale3': d_voro(28, (240, 160, 100), (50, 16, 10), .13),
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


def tube_z(B, nm, cx, cy, cz, w, h, d, mat, key, fx=None):
    """section arrondie extrudee le long de z : croix (2 boites) + 4 petits losanges aux coins = prisme octogonal"""
    if min(w, h) < 9:
        return [B(nm, cx - w / 2, cy - h / 2, cz - d / 2, w, h, d, mat, key=f'{key}_{round(w)}_{round(h)}_{round(d)}', fx=fx)]
    c = max(2, round(min(w, h) * .26)); sz = max(2, round(c * .72))
    out = [B(nm + 'a', cx - w / 2, cy - (h - 2 * c) / 2, cz - d / 2, w, h - 2 * c, d, mat, key=f'{key}a', fx=fx),
           B(nm + 'b', cx - (w - 2 * c) / 2, cy - h / 2, cz - d / 2, w - 2 * c, h, d, mat, key=f'{key}b')]
    for sx in (1, -1):
        for sy in (1, -1):
            out.append(B(f'{nm}c{sx}{sy}', cx + sx * (w / 2 - c / 2) - sz / 2, cy + sy * (h / 2 - c / 2) - sz / 2, cz - d / 2, sz, sz, d, mat, key=f'{key}c{sz}_{round(d)}', rot=(0, 0, 45)))
    return out


def tube_y(B, nm, cx, cy, cz, w, l, dp, mat, key, tilt=0.0, fx=None):
    """section arrondie extrudee le long de y (cou, pattes) ; tilt = inclinaison autour de x (negatif = penche vers l'avant)"""
    ca, sa = math.cos(ang(tilt)), math.sin(ang(tilt))
    if min(w, dp) < 9:
        return [B(nm, cx - w / 2, cy - l / 2, cz - dp / 2, w, l, dp, mat, key=f'{key}_{round(w)}_{round(l)}', rot=(tilt, 0, 0), fx=fx)]
    c = max(2, round(min(w, dp) * .26)); sz = max(2, round(c * .72))
    out = [B(nm + 'a', cx - w / 2, cy - l / 2, cz - (dp - 2 * c) / 2, w, l, dp - 2 * c, mat, key=f'{key}a', rot=(tilt, 0, 0), fx=fx),
           B(nm + 'b', cx - (w - 2 * c) / 2, cy - l / 2, cz - dp / 2, w - 2 * c, l, dp, mat, key=f'{key}b', rot=(tilt, 0, 0))]
    for sx in (1, -1):
        for sz_ in (1, -1):
            lx, lz = sx * (w / 2 - c / 2), sz_ * (dp / 2 - c / 2)
            wx, wy, wz = cx + lx, cy - lz * sa, cz + lz * ca
            out.append(B(f'{nm}c{sx}{sz_}', wx - sz / 2, wy - l / 2, wz - sz / 2, sz, l, sz, mat, key=f'{key}c{sz}_{round(l)}', rot=(tilt, 45, 0)))
    return out


def lerp(a, b, t):
    return a + (b - a) * t


def dragon_groups():
    B, G = mkT(1.0)
    s_ = lambda s: 'r' if s > 0 else 'l'
    # ---------------------------------------------------------------- corps : 14 tranches arrondies, profil d'epaules puissantes -> taille -> hanches
    W_ = [14, 22, 27, 30, 30, 29, 27, 25, 24, 25, 27, 28, 26, 22]
    H_ = [18, 24, 28, 30, 30, 29, 27, 25, 25, 26, 27, 27, 25, 21]
    CY = [44, 45, 46, 46, 46, 45.5, 44.5, 43.5, 43, 43, 43, 42.5, 42, 41]
    ZC = [-33 + i * 5 for i in range(14)]
    body_kids = []
    for i in range(14):
        body_kids += tube_z(B, f'sl{i}', 0, CY[i], ZC[i], W_[i], H_[i], 6, 'dscale', f'd_sl{i}')
        body_kids.append(B(f'bs{i}', -W_[i] * .26, CY[i] - H_[i] / 2 - .6, ZC[i] - 3, round(W_[i] * .52), 2, 6, 'dbelly', key=f'd_bs{i}', fx=fx_embers(8, ('down',))))
        if 3 <= i <= 11:                                                  # flancs : plaques plus claires
            for sx in (1, -1):
                body_kids.append(B(f'fl{i}{s_(sx)}', sx * (W_[i] / 2 - .2) - 1, CY[i] - 4, ZC[i] - 3, 2, 9, 6, 'dscale3', key=f'd_fl{i}', rot=(0, 0, sx * 14)))
    body_kids.append(B('breast', -8, CY[0] - 6, -37.5, 16, 14, 2, 'dbelly', key='d_breast', fx=fx_embers(26, ('north',))))
    for sx in (1, -1):                                                    # bosses d'epaules
        body_kids += tube_z(B, f'shd{s_(sx)}', sx * 15, 52, -22, 10, 11, 16, 'dscale2', f'd_shd{s_(sx)}')
    for i in range(2, 13):                                                # tuiles de plaques sur le dos (3 rangees) et les cotes
        ytop = CY[i] + H_[i] / 2
        for k, (xo, rz) in enumerate(((-1, -26), (0, 0), (1, 26))):
            body_kids.append(B(f'sh{i}_{k}', xo * W_[i] * .33 - 3.5, ytop - 1.2, ZC[i] - 3.4, 7, 2, 7, 'dscale2', key=f'd_sh{k}', rot=(10, 0, rz)))
    for i in range(12):                                                   # crete de pointes (3 etages effiles), plus hautes aux epaules
        k = min(13, i + 2); z = ZC[k]; y = CY[k] + H_[k] / 2 + 1
        h = max(6, 15 - i * .8)
        b1, b2, b3 = round(h * .42), round(h * .34), round(h * .26)
        body_kids.append(G(f'dsp{i}', (0, y, z), rot=(16, 0, 0), children=[
            B(f'dsp_a{i}', -3.5, y, z - 2.5, 7, b1, 5, 'dhorn', key=f'd_spa{b1}'),
            B(f'dsp_b{i}', -2.5, y + b1, z - 1.5, 5, b2, 3, 'dhorn', key=f'd_spb{b2}'),
            B(f'dsp_c{i}', -1.2, y + b1 + b2, z - .8, 2, b3, 2, 'dhorn', key=f'd_spc{b3}')]))
    # ---------------------------------------------------------------- cou en S : 8 segments arrondis, ventre dore, crete, pointes
    pos = np.array([0.0, 56.0, -33.0])
    angles = [5, 13, 22, 31, 40, 48, 55, 60]; widths = [22, 20.5, 19, 18, 17, 16, 15, 14]; length = 11
    neck_kids = []
    for i, (th, wd) in enumerate(zip(angles, widths)):
        dvec = np.array([0, math.cos(ang(th)), -math.sin(ang(th))]); nvec = np.array([0, -math.sin(ang(th)), -math.cos(ang(th))])
        dep = wd - 2
        c = pos + dvec * length / 2
        neck_kids += tube_y(B, f'nk{i}', c[0], c[1], c[2], wd, length + 1, dep, 'dscale', f'd_nk{i}', tilt=-th)
        cb = c + nvec * (dep / 2 + .6)
        neck_kids.append(B(f'nbelly{i}', cb[0] - wd * .27, cb[1] - length / 2, cb[2] - 1, round(wd * .54), length, 2, 'dbelly', key=f'd_nb{i}', rot=(-th, 0, 0), fx=fx_embers(10, ('north',))))
        cs = c - nvec * (dep / 2 + 3.2)
        neck_kids.append(B(f'nspike{i}', cs[0] - 2, cs[1] - 5, cs[2] - 2, 4, 10, 4, 'dhorn', key='d_nsp', rot=(90 - th + 6, 0, 0)))
        for sx in (1, -1):                                                # petites pointes laterales
            cl = c + np.array([sx * (wd / 2 - 1), 0, 0]) - nvec * (dep * .3)
            neck_kids.append(B(f'nside{i}{s_(sx)}', cl[0] - 1, cl[1] - 3, cl[2] - 1, 2, 6, 2, 'dhorn', key='d_nsd', rot=(90 - th + 6, 0, sx * 30)))
        pos = pos + dvec * length * .93
    hx, hy, hz0 = pos
    # ---------------------------------------------------------------- tete : crane + 3 sections de museau effilees, machoire en 3 sections
    head_kids = []
    head_kids += tube_z(B, 'sk', 0, hy, hz0 - 10, 20, 16, 24, 'dscale', 'd_skull', fx=fx_deye(1, 3))
    for sx in (1, -1):
        head_kids += tube_z(B, f'ck{s_(sx)}', sx * 9, hy - 2, hz0 - 4, 7, 11, 12, 'dscale3', f'd_ckb{s_(sx)}')
    head_kids += tube_z(B, 'sn1', 0, hy - 1.5, hz0 - 28, 15, 11, 12, 'dscale', 'd_sn1')
    head_kids += tube_z(B, 'sn2', 0, hy - 2.5, hz0 - 39, 13, 10, 12, 'dscale', 'd_sn2')
    head_kids += tube_z(B, 'sn3', 0, hy - 3, hz0 - 49, 11, 9, 10, 'dscale3', 'd_sn3', fx=None)
    head_kids += [
        B('nose', -5.5, hy - 6.5, hz0 - 56, 11, 8, 6, 'dscale3', key='d_nose', fx=fx_nostril),
        B('brow', -11, hy + 6.5, hz0 - 22, 22, 3, 12, 'dscale2', key='d_brow'),
        B('crown', -7, hy + 8, hz0 - 8, 14, 3, 14, 'dscale2', key='d_crown'),
        B('snout_top', -4, hy + 3.5, hz0 - 50, 8, 3, 26, 'dscale2', key='d_stop'),
        B('nose_horn', -1.5, hy + 4, hz0 - 54, 3, 4, 7, 'dhorn', key='d_nhorn'),
        B('roof', -5.5, hy - 7.2, hz0 - 50, 11, 1, 42, 'dmouth', key='d_roof', fx=fx_mouth),
    ]
    for sx in (1, -1):
        for k in range(8):
            th_ = 4 + (k % 2) * 2
            head_kids.append(B(f'tooth_u{s_(sx)}{k}', sx * 6.4 - 1, hy - 7 - th_, hz0 - 52 + k * 4.8, 2, th_, 2, 'dtooth', key=f'd_tu{th_}'))
        head_kids.append(B(f'canine_u{s_(sx)}', sx * 6.6 - 1.5, hy - 15, hz0 - 51, 3, 9, 3, 'dtooth', key='d_canu'))
        head_kids.append(B(f'tusk_{s_(sx)}', sx * 8 - 1.5, hy - 3, hz0 - 38, 3, 3, 10, 'dhorn', key='d_tuskb', rot=(0, sx * 10, 0)))
        # cornes : 6 segments qui s'incurvent vers l'arriere et le haut
        P = np.array([sx * 7.5, hy + 8.0, hz0 - 5.0])
        for i in range(6):
            sw = max(2, round(6.2 - i)); ln = 7
            pitch = -(14 + 13 * i); yaw = sx * (10 + 4 * i)
            c = P + np.array([sx * ln * .28, math.sin(ang(-pitch)) * ln * .5, math.cos(ang(-pitch)) * ln * .5])
            head_kids.append(B(f'horn{i}_{s_(sx)}', c[0] - sw / 2, c[1] - sw / 2, c[2] - ln / 2, sw, sw, ln, 'dhorn', key=f'd_horn{i}', rot=(pitch, yaw, 0)))
            P = P + np.array([sx * ln * .45, math.sin(ang(-pitch)) * ln * .8, math.cos(ang(-pitch)) * ln * .9])
        for i in range(3):                                                # nageoire de joue : 3 epines balayees
            head_kids.append(B(f'cheek{i}_{s_(sx)}', sx * 12 - 1.5, hy - 2 - i * 3, hz0 - 4 + i, 3, 3, 12, 'dhorn', key='d_cheek', rot=(0, sx * (36 + i * 12), 0)))
        head_kids.append(B(f'fin_{s_(sx)}', sx * 12 - .5, hy - 6, hz0 - 3, 1, 14, 16, 'dmemb', key='d_fin'))
        head_kids.append(B(f'browh_{s_(sx)}', sx * 10 - 1.5, hy + 8, hz0 - 20, 3, 5, 6, 'dhorn', key='d_browh', rot=(-20, 0, sx * -10)))
    for k in range(4): head_kids.append(B(f'tooth_f{k}', -4.5 + k * 3, hy - 11.5, hz0 - 56, 2, 5, 2, 'dtooth', key='d_tf'))
    jk = []
    jk += tube_z(B, 'j1', 0, hy - 12, hz0 - 22, 15, 7, 14, 'dscale', 'd_j1')
    jk += tube_z(B, 'j2', 0, hy - 12.5, hz0 - 35, 13, 6, 14, 'dscale', 'd_j2')
    jk += tube_z(B, 'j3', 0, hy - 12.5, hz0 - 46, 10, 6, 12, 'dscale3', 'd_j3')
    jk += [B('chin', -5, hy - 17, hz0 - 50, 10, 4, 36, 'dbelly', key='d_chin'),
           B('tongue', -3.5, hy - 8.8, hz0 - 46, 7, 2, 38, 'dmouth', key='d_tongue', fx=fx_mouth),
           B('chin_spike', -1.5, hy - 19, hz0 - 52, 3, 8, 3, 'dhorn', key='d_chinsp', rot=(14, 0, 0))]
    for sx in (1, -1):
        jk.append(B(f'jspike_{s_(sx)}', sx * 8 - 1, hy - 17, hz0 - 24, 2, 7, 3, 'dhorn', key='d_jsp'))
        jk.append(B(f'canine_l{s_(sx)}', sx * 5.2 - 1.5, hy - 12, hz0 - 52, 3, 7, 3, 'dtooth', key='d_canl'))
        for k in range(7):
            jk.append(B(f'tooth_l{s_(sx)}{k}', sx * 5.6 - 1, hy - 9.2, hz0 - 50 + k * 5.2, 2, 4 + (k % 2), 2, 'dtooth', key=f'd_tl{k % 2}'))
    jaw = G('jaw', (0, hy - 8, hz0 - 2), children=jk)
    head = G('head', (0, hy - 8, hz0), rot=(-18, 0, 0), children=head_kids + [jaw])
    neck = G('neck', (0, 56, -33), children=neck_kids + [head])
    body = G('body', (0, 40, 0), children=body_kids + [neck])
    # ---------------------------------------------------------------- ailes : bras, avant-bras, 4 doigts, membranes en bandes fines a bord festonne
    for s in (1, -1):
        n = s_(s)
        sx = 14 * s
        kids = []

        def seg(name, p0, direction_deg, L, th, mat, key, y):
            dx, dz = math.cos(ang(direction_deg)), math.sin(ang(direction_deg))
            c = (p0[0] + s * dx * L / 2, p0[1] + dz * L / 2)
            cube = B(name, c[0] - L / 2, y - th / 2, c[1] - th / 2, L, th, th, mat, key=key, rot=(0, -(direction_deg if s > 0 else -direction_deg), 0))
            return cube, (p0[0] + s * dx * L, p0[1] + dz * L)
        yw = 60
        shoulder = (sx, -20)
        arm, elbow = seg(f'arm_{n}', shoulder, 8, 26, 9, 'dscale', 'd_warm', yw)
        arm2, _ = seg(f'arm2_{n}', (shoulder[0], shoulder[1]), 8, 24, 6, 'dscale2', 'd_warm2', yw + 4)
        fore, wrist = seg(f'fore_{n}', elbow, -38, 36, 7, 'dscale2', 'd_wfore', yw + 2)
        fore2, _ = seg(f'fore2_{n}', elbow, -38, 34, 4, 'dscale', 'd_wfore2', yw + 5.5)
        kids += [arm, arm2, fore, fore2]
        kids.append(B(f'wclaw_{n}', wrist[0] - 1.5, yw + 2, wrist[1] - 1.5, 3, 10, 3, 'dclaw', key='d_wclaw', rot=(-25, 0, s * 12)))
        kids.append(B(f'wclaw2_{n}', wrist[0] - 1, yw + 1, wrist[1] - 6, 2, 3, 8, 'dclaw', key='d_wclaw2'))
        kids.append(B(f'wspike_{n}', elbow[0] - 2, yw + 3, elbow[1] - 2, 4, 9, 4, 'dhorn', key='d_wspike', rot=(0, 0, s * 18)))
        fingers = [(4, 96), (28, 90), (52, 78), (76, 64)]
        tips = []
        for k, (psi, Lf) in enumerate(fingers):
            fb, tip = seg(f'finger{k}_{n}', wrist, psi, Lf, 4, 'dscale2', f'd_fing{k}', yw + 2)
            kids.append(fb); tips.append((psi, Lf, tip))
            kids.append(B(f'fclaw{k}_{n}', tip[0] - 1, yw + 1, tip[1] - 1, 2, 2, 5, 'dclaw', key='d_fclaw', rot=(0, -(psi + 12 if s > 0 else -psi - 12), 0)))
        edges = [(psi, Lf) for psi, Lf, _ in tips] + [(100, 60)]
        for k in range(len(edges) - 1):
            (p1, L1), (p2, L2) = edges[k], edges[k + 1]
            pm = (p1 + p2) / 2; dpsi = abs(math.radians(p2 - p1))
            dx, dz = math.cos(ang(pm)), math.sin(ang(pm))
            nb = 4                                                        # 4 bandes radiales de plus en plus larges, bord exterieur festonne
            for part in range(nb):
                f0 = .08 + part * .22; f1 = f0 + .23
                Lm = lerp(L1, L2, .5) * (1 - .06 * (part == nb - 1) * (1 if k % 2 else .4))
                ln = (f1 - f0) * Lm
                dd = (f0 + f1) / 2 * Lm
                c = (wrist[0] + s * dx * dd, wrist[1] + dz * dd)
                wd = max(3, 2 * Lm * math.sin(dpsi / 2) * (f0 + f1) / 2 * .98)
                kids.append(B(f'memb{k}{part}_{n}', c[0] - ln / 2, yw + 1.2, c[1] - wd / 2, ln, 1, wd, 'dmemb', key=f'd_memb{k}{part}',
                              rot=(0, -(pm if s > 0 else -pm), 0)))
        for j, (zz, L) in enumerate(((-14, 40), (-2, 46), (10, 46), (22, 38))):         # membrane arriere jusqu'au flanc
            kids.append(B(f'rear{j}_{n}', sx + (s * 3 if s > 0 else -3 - L), yw + .6, zz, L, 1, 12, 'dmemb', key=f'd_rear{j}'))
        body.children.append(G(f'wing_{n}', (sx, yw, -20), rot=(0, 0, s * 26), children=kids))
    # ---------------------------------------------------------------- pattes : sections arrondies, genoux et coudes epineux, griffes
    legs = []

    def leg(nm, s, z, hind):
        x = 17 * s
        if hind:
            kids = tube_y(B, f'{nm}_th', x, 32, z, 20, 20, 20, 'dscale3', f'd_th{s_(s)}') + tube_y(B, f'{nm}_sh', x, 18, z + 3, 13, 16, 13, 'dscale3', f'd_sh{s_(s)}', tilt=14) + \
                [B(f'{nm}_knee', x - 3, 30, z - 13, 6, 6, 5, 'dhorn', key='d_knee', rot=(20, 0, 0)),
                 B(f'{nm}_heel', x - 2, 8, z + 11, 4, 4, 9, 'dhorn', key='d_heel', rot=(-18, 0, 0))] + \
                tube_z(B, f'{nm}_ft', x, 3, z - 4, 17, 6, 22, 'dscale2', f'd_ft{s_(s)}')
            for k in (-1, 0, 1):
                kids.append(B(f'{nm}_claw{k}', x + k * 4.8 - 1.5, 0, z - 20, 3, 4, 9, 'dclaw', key='d_claw', rot=(0, k * 10, 0)))
        else:
            kids = tube_y(B, f'{nm}_ar', x, 30, z, 16, 20, 16, 'dscale3', f'd_ar{s_(s)}') + tube_y(B, f'{nm}_fo', x, 14, z - 1, 13, 16, 13, 'dscale3', f'd_fo{s_(s)}') + \
                [B(f'{nm}_elbow', x + (5 if s > 0 else -9), 24, z + 4, 4, 6, 7, 'dhorn', key='d_elb', rot=(-20, 0, s * 16))] + \
                tube_z(B, f'{nm}_hd', x, 3, z - 3, 17, 6, 20, 'dscale2', f'd_hd{s_(s)}')
            for k in (-1, 0, 1):
                kids.append(B(f'{nm}_claw{k}', x + k * 4.8 - 1.5, 0, z - 19, 3, 4, 8, 'dclaw', key='d_fclaw', rot=(0, k * 10, 0)))
        return G(nm, (x, 40, z), children=kids)
    legs += [leg('leg_fr', 1, -18, False), leg('leg_fl', -1, -18, False), leg('leg_br', 1, 24, True), leg('leg_bl', -1, 24, True)]
    # ---------------------------------------------------------------- queue : 3 sections articulees faites de tranches qui s'effilent + pointes + dard
    def tail_slices(prefix, z0, n_, w0, w1, h0, h1, y0, y1, step, key):
        out = []
        for i in range(n_):
            t = i / max(1, n_ - 1)
            out += tube_z(B, f'{prefix}{i}', 0, lerp(y0, y1, t), z0 + i * step, round(lerp(w0, w1, t)), round(lerp(h0, h1, t)), step + 1, 'dscale', f'{key}{i}')
            out.append(B(f'{prefix}b{i}', -lerp(w0, w1, t) * .22, lerp(y0, y1, t) - lerp(h0, h1, t) / 2 - .5, z0 + i * step - step / 2, round(lerp(w0, w1, t) * .44), 2, step + 1, 'dbelly', key=f'{key}b{i}'))
            if i % 2 == 0:
                hh = round(lerp(8, 4, t))
                out.append(G(f'{prefix}sp{i}', (0, lerp(y0, y1, t) + lerp(h0, h1, t) / 2, z0 + i * step), rot=(14, 0, 0), children=[
                    B(f'{prefix}spk{i}', -lerp(2.5, 1.2, t), lerp(y0, y1, t) + lerp(h0, h1, t) / 2, z0 + i * step - 2, round(lerp(5, 2, t)), hh, 4, 'dhorn', key=f'{key}sp{i}')]))
        return out
    t3 = tail_slices('t3_', 84, 12, 13, 3, 12, 3, 36, 28, 6, 'd_t3')
    t3 += [B('barb_a', -1, 20, 158, 2, 7, 12, 'dclaw', key='d_barb', rot=(-12, 0, 0)),
           G('barb_l', (0, 28, 158), rot=(0, 28, 0), children=[B('barb_lb', 0, 27, 158, 2, 3, 13, 'dclaw', key='d_barbf')]),
           G('barb_r', (0, 28, 158), rot=(0, -28, 0), children=[B('barb_rb', -2, 27, 158, 2, 3, 13, 'dclaw', key='d_barbf')])]
    t2 = G('tail2', (0, 38, 58), children=tail_slices('t2_', 59, 5, 17, 13, 16, 12, 38, 36.5, 5, 'd_t2') + [G('tail3', (0, 36, 84), children=t3)])
    tail = G('tail', (0, 42, 36), children=tail_slices('t1_', 36, 4, 22, 18, 21, 17, 41.5, 40, 5.5, 'd_t1') + [t2])
    return legs + [body, tail]


# ================================================================== generation / apercu
def build(outdir='/tmp/dragon'):
    _N[0] = 0
    groups = dragon_groups()
    tc.normalize(groups)
    cubes = [c for c, _ in tc.walk(groups)]
    seen = {}
    for c in cubes:                                              # une cle = une taille + un materiau (sinon on la desambiguise)
        r = (c.size, c.mat)
        if c.key in seen and seen[c.key] != r: c.key = f'{c.key}_{c.size[0]}x{c.size[1]}x{c.size[2]}_{c.mat}'
        seen.setdefault(c.key, r)
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
