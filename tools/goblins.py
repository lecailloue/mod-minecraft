"""Gobelins : 3 types (1m20, 1m50, 1m80) + le Roi des gobelins (2m40). Fichiers 3D uniquement (rien n'est branche dans le mod).
Memes conventions que orcs.py / shadows.py : y vers le haut, sol y=0, avant = -z, cote -x = 'left'. 1 unite = 2 cm.
Noms de pieces reconnus par GeoModel : head, leg_left/right, arm_left/right (+ forearm_*), flame* (torche).
Pieces prevues pour une animation future : belly (ventre du roi), dewlap/dewlap2/dewlap3 (bajoue), hump (bosse)."""
import math
import numpy as np
import trollcore as T
from trollcore import *
import orcs as O
from orcs import C, X, spike, chain, translate, scale_about, scale_all, unique_names, fx_nose, fx_mouth_kind

# ---------------------------------------------------------------- materiaux
MAT = {
    'g_straw': dict(base=(150, 128, 72), n1=8, n2=8, tint=(1, .95, .7)),
    'g_rope': dict(base=(154, 130, 98), n1=8, n2=7, tint=(1, .93, .8)),
    'g_gem': dict(base=(196, 30, 34), n1=10, n2=6, tint=(1, .5, .5)),
    'g_flame': dict(base=(255, 170, 40), n1=14, n2=10, tint=(1, 1, .5)),
    'g_rag': dict(base=(104, 76, 56), n1=9, n2=9, tint=(1, .9, .8)),
    'g_red': dict(base=(130, 52, 40), n1=8, n2=8, tint=(1, .8, .75)),
    'g_horn': dict(base=(196, 182, 138), n1=7, n2=6, tint=(1, .96, .82)),
    'g_wire': dict(base=(96, 78, 44), n1=6, n2=6, tint=(1, .95, .8)),
    'g_hide': dict(base=(112, 84, 62), n1=11, n2=9, tint=(1, .9, .8)),
    'g_hair': dict(base=(168, 150, 148), n1=6, n2=6, tint=(1, .97, .97)),
    'g_nail': dict(base=(188, 170, 126), n1=5, n2=5, tint=(1, .95, .8)),
    'g_eyew': dict(base=(210, 170, 70), n1=3, n2=3, tint=(1, 1, 1)),
}


def gskin(hexcol, kind='gob'):
    name = f'g_sk_{kind}_{hexcol}'
    if name not in T.MATS:
        c = tuple(int(hexcol[i:i + 2], 16) for i in (0, 2, 4))
        T.MATS[name] = dict(base=c, n1=6, n2=8, tint=(1, 1, 1))
        T.DECOR[name] = d_king if kind == 'king' else d_gskin
    return name


def d_gskin(arr, face, rng):
    """peau de gobelin : taches sombres et chaudes, grains clairs, verrues, plis"""
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    dark = tuple(m * .7)
    warm = tuple(np.clip(m * np.array((1.2, .92, .78)), 0, 255))
    lite = tuple(np.clip(m * 1.16 + 12, 0, 255))
    for _ in range(max(1, h * w // 22)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), dark if rng.random() < .55 else warm, .3, rng)
    for _ in range(h * w // 38):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), lite, .5)
    if h * w >= 16 and rng.random() < .3:
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        put(arr, x, y, lite, .95)
        put(arr, x + 1, y + 1, tuple(m * .5), .6)
    if w >= 5 and h >= 5 and rng.random() < .5:
        x0, y0 = int(rng.integers(0, w // 2)), int(rng.integers(1, h))
        line(arr, x0, y0, 1, 0, int(rng.integers(2, max(3, w // 2 + 1))), tuple(m * .58), .45)


def d_king(arr, face, rng):
    """peau du roi : une seule teinte, simples variations de ton (plus clair / plus sombre), quelques verrues et rides discretes"""
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    dark = tuple(m * .86)
    lite = tuple(np.clip(m * 1.1 + 6, 0, 255))
    for _ in range(max(1, h * w // 40)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3)), dark if rng.random() < .6 else lite, .22, rng)
    for _ in range(max(1, h * w // 60)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        put(arr, x, y, lite, .7)
        put(arr, x + 1, y + 1, dark, .5)
    if h >= 6 and rng.random() < .4:
        line(arr, 0, int(rng.integers(2, h - 1)), 1, 0, w, tuple(m * .78), .3)


def d_dew(arr, face, rng):
    """bajoue : du ton de la peau en haut vers un rouge-orange en bas, boursouflee de pustules et de veines"""
    h, w, _ = arr.shape
    for y in range(h):
        a = (y / max(1, h - 1)) ** 1.1 * .75
        arr[y] = arr[y] * (1 - a) + np.array((190, 112, 86)) * a
    for _ in range(max(2, h * w // 7)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        put(arr, x, y, (212, 170, 144), .8)
        put(arr, x + 1, y + 1, (122, 70, 60), .45)
    if w >= 3 and h >= 4:
        x = int(rng.integers(0, w))
        line(arr, x, 0, 0, 1, h, (150, 84, 70), .4)


def d_straw(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= .78 + (x % 3) * .14
    for _ in range(h * w // 5):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (96, 80, 40), .5)
    for x in range(w):
        if rng.random() < .5:
            put(arr, x, h - 1, (70, 56, 30))


def d_rope(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):
        for x in range(w):
            if (x + y) % 3 == 0:
                put(arr, x, y, (92, 72, 52), .5)
            elif (x + y) % 3 == 1:
                put(arr, x, y, (190, 166, 128), .25)


def d_gem(arr, face, rng):
    h, w, _ = arr.shape
    put(arr, max(0, w // 2 - 1), 0, (255, 190, 190), .9)
    arr[:] *= np.linspace(1.2, .75, h)[:, None, None]


def d_flame(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):
        t = y / max(1, h - 1)
        arr[y] = np.array((255, 236, 120)) * (1 - t) + np.array((236, 90, 20)) * t


def d_hide(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(h * w // 4):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (70, 50, 38) if rng.random() < .5 else (150, 118, 90), .5)
    for x in range(w):
        if rng.random() < .5:
            put(arr, x, h - 1, (40, 28, 22))


def d_horn(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(.78, 1.14, h)[:, None, None]
    for _ in range(max(1, h * w // 14)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(2, 5)), (120, 104, 76), .35)


def d_wire(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(0, h, 2):
        arr[y] *= .7
    for _ in range(max(1, h * w // 9)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (150, 130, 80), .5)


def d_nail(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(.9, 1.1, h)[:, None, None]


T.MATS.update(MAT)
T.DECOR.update({'g_straw': d_straw, 'g_rope': d_rope, 'g_gem': d_gem, 'g_flame': d_flame, 'g_rag': O.d_cloth2, 'g_red': O.d_leather2,
                'g_horn': d_horn, 'g_wire': d_wire, 'g_hide': d_hide, 'g_hair': O.d_hair2, 'g_nail': d_nail, 'g_eyew': lambda a, f, r: None})


# ---------------------------------------------------------------- details de visage
def fx_eyeball(col, rim=(120, 60, 24)):
    """gros oeil globuleux et rond : iris colore, bord sombre, pupille verticale, reflet ; paupiere lourde (peau sombre) en haut"""
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        base = n.copy()
        for y in range(h):
            for x in range(w):
                corner = x in (0, w - 1) and y in (0, h - 1)
                if corner:
                    continue
                edge = x in (0, w - 1) or y in (0, h - 1)
                put(n, x, y, rim if edge else col, 1)
        for x in range(w):
            n[0, x] = base[0, x] * .55                       # paupiere
        cx = w // 2
        for y in range(1, h - 1):
            put(n, cx, y, (10, 6, 4), 1)
        put(n, max(1, cx - 1), 1, (255, 238, 190), .85)
    return f


def fx_brow(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    n[h - 1, :] *= .6
    n[h - 2, :] *= .8


def fx_ribs(F, rng, glow):
    """cotes saillantes d'un torse maigre"""
    n = F['north']
    h, w, _ = n.shape
    c = w // 2
    for y in range(1, h - 1, 3):
        for x in range(1, c - 1):
            put(n, x, y, (0, 0, 0), .24)
            put(n, w - 1 - x, y, (0, 0, 0), .24)
            put(n, x, y + 1, (255, 255, 255), .08)
            put(n, w - 1 - x, y + 1, (255, 255, 255), .08)
    for y in range(0, h):
        put(n, c, y, (0, 0, 0), .2)


def fx_belly(F, rng, glow):
    """gros ventre : nombril, plis et vergetures"""
    n = F['north']
    h, w, _ = n.shape
    for y in range(3, h - 2, 4):
        for x in range(2, w - 2):
            if (x + y) % 5:
                put(n, x, y, (0, 0, 0), .14)
    cx, cy = w // 2, h // 2
    put(n, cx, cy, (50, 22, 24), .95)
    put(n, cx, cy - 1, (50, 22, 24), .6)
    put(n, cx - 1, cy, (50, 22, 24), .5)
    put(n, cx + 1, cy, (50, 22, 24), .5)


def fx_pecs(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    put(n, w // 2, h - 3, (110, 54, 56), .95)
    put(n, w // 2, h - 2, (110, 54, 56), .6)
    n[h - 1, :] *= .72


def fx_flame_glow(F, rng, glow):
    for f, arr in F.items():
        h, w, _ = arr.shape
        for y in range(h):
            for x in range(w):
                glow(f, x, y, tuple(int(v) for v in arr[y, x]))


def fx_gem_glow(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    for y in range(h):
        for x in range(w):
            glow('north', x, y, tuple(int(v) for v in n[y, x]))


def fx_skull_eyes(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    for x in range(w):
        for y in range(h):
            if (x in (1, w - 2)) and 1 <= y <= h - 2:
                put(n, x, y, (14, 10, 10), 1)
    put(n, w // 2, h - 1, (30, 24, 20), .9)


def fx_king_mouth(F, rng, glow):
    """gueule du roi : bouche entrouverte, dents jaunes tordues en haut et en bas, langue sombre"""
    n = F['north']
    h, w, _ = n.shape
    for x in range(2, w - 2):
        for y in range(2, h - 2):
            put(n, x, y, (28, 8, 10), 1)
        put(n, x, h - 3, (110, 36, 40), .85)
    for x in range(3, w - 3):
        if x % 3 == 0:
            for y in (2, 3):
                put(n, x, y, (218, 200, 130), 1)
        elif x % 3 == 1:
            put(n, x, 2, (190, 170, 104), .9)
    for x in range(4, w - 4, 4):
        put(n, x, h - 4, (218, 200, 130), 1)
        put(n, x, h - 5, (218, 200, 130), .8)
    n[h - 1, :] *= .8


# ---------------------------------------------------------------- tete
def goblin_head(c, sk):
    """tete de gobelin : grand crane bombe, gros yeux globuleux, nez, machoire, longues oreilles. Pivot de la tete : (0, 57, -1)."""
    sw, sh, sd = c.get('sw', 12), c.get('sh', 11), c.get('sd', 11)
    zf = -1 - sd / 2
    y0 = 58
    k = [C('skull', -sw / 2, y0, zf, sw, sh, sd, sk, key='g_skull'),
         C('skull_top', -(sw - 4) / 2, y0 + sh, zf + .8, sw - 4, 2, sd - 2, sk, key='g_skulltop'),
         C('skull_back', -(sw - 2) / 2, y0 + 2, zf + sd, sw - 2, sh - 2, 2, sk, key='g_skullb'),
         C('brow', -(sw + 1) / 2, y0 + 6, zf - 1.6, sw + 1, 3, 3, sk, key='g_brow', fx=fx_brow)]
    eye = c['eye']
    es = c.get('eyesize', 5)
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k.append(C('eye_' + n, X(sg, 3.6, es), y0 + 2.5, zf - 1.8, es, es - 1, 3, sk, key='g_eye%d' % es + ''.join('%02x' % v for v in eye), fx=fx_eyeball(eye)))
    k.append(C('cheeks', -(sw) / 2 - .5, y0 - .5, zf - 1, sw + 1, 3, 3, sk, key='g_cheeks'))
    nz = c.get('nose', 'small')
    if nz == 'small':
        k.append(C('nose', -1.5, y0 + 1, zf - 3.4, 3, 5, 4, sk, key='g_nose', fx=fx_nose))
    elif nz == 'long':
        k.append(C('nose', -1.5, y0 + 1, zf - 3.4, 3, 6, 4, sk, key='g_nosel', fx=fx_nose))
        k.append(C('nose_tip', -1.5, y0 - 1.5, zf - 5.4, 3, 4, 3, sk, key='g_nosetip', fx=fx_nose))
    else:                                                                         # gros nez crochu (3e type)
        k.append(C('nose', -2, y0 + 2, zf - 3.4, 4, 6, 4, sk, key='g_noseh'))
        k.append(C('nose_tip', -2, y0 - .5, zf - 6.4, 4, 4, 4, sk, key='g_nosehtip', fx=fx_nose))
        k.append(C('nose_bridge', -1.5, y0 + 5, zf - 3, 3, 3, 3, sk, key='g_nosebr'))
    jw = c.get('jw', 8)
    k.append(C('jaw', -jw / 2, y0 - 4.5, zf - 1, jw, 5, int(sd - 2), sk, key='g_jaw' + c['mouth'], fx=fx_mouth_kind(c['mouth'])))
    k.append(C('chin', -2, y0 - 7.5, zf - 1.8, 4, 3, 4, sk, key='g_chin'))
    for sg in (-1, 1):
        k.append(C('fang_' + ('l' if sg < 0 else 'r'), sg * jw / 2 * .62 - .5, y0 - 1.5, zf - 1.6, 1, 3, 1, 'o_tooth', key='g_fang'))
    ear = c['ear']
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k.append(chain('ear_' + n, (sg * (sw / 2 - .3), y0 + 6, zf + sd * .65), ear['len'], ear['w'], ear['d'], ear['rz'], ear['drz'], ear['rx'], ear['drx'], sg=sg, step=2, prof=.55, mat=sk, ry=ear.get('ry', 0)))
    if c.get('hair'):
        k.append(C('hair_a', -3, y0 + sh + 2, zf + 3, 6, 2, 6, 'g_hair', key='g_hairtuft'))
    return k


# ---------------------------------------------------------------- accessoires
def belt(bk, y=36, w=12, gem=True, mat='o_dleather'):
    bk.append(C('belt', -w / 2, y, -4.2, w, 3, 8, mat, key='g_belt%d' % w))
    if gem:
        bk.append(C('buckle', -2.5, y - .2, -4.9, 5, 3, 1, 'o_iron', key='g_buckle'))
        bk.append(C('gem', -1, y + .6, -5.6, 2, 2, 1, 'g_gem', key='g_gem', fx=fx_gem_glow))
    else:
        bk.append(C('buckle', -2, y, -4.9, 4, 3, 1, 'o_iron', key='g_buckle2'))


def skirt(bk, y, n, ln, mat, rag=1, w=11, back=True):
    for i in range(n):
        x = -w / 2 + i * (w / n)
        l = int(ln * (.8 + ((i * 7) % 5) * .1)) * rag
        bk.append(C(f'rag_{i}', x, y - l, -4.7 - (i % 2) * .6, w / n + .6, l, 1, mat, key=f'g_rag_{mat}{l}', rot=(0, 0, (-4, 3, -2, 5)[i % 4])))
    if back:
        for i in range(3):
            bk.append(C(f'rag_b{i}', -4 + i * 3.4, y - ln + 2, 3.8, 3, ln - 2, 1, mat, key=f'g_ragb_{mat}{ln}'))


def pauldron(bk, sg, w=8, spikes=0):
    n = 'l' if sg < 0 else 'r'
    for i in range(2):
        bk.append(C(f'paul_{n}{i}', X(sg, 9.2 + i * .6, w - i), 53 - i * 2.6, -4.6, w - i, 3, 9, 'o_iron' if i == 0 else 'o_dleather', key=f'g_paul{i}',
                    rot=(0, 0, sg * (-12 - i * 6)), pivot=(sg * 9.2, 56, 0)))
    for i in range(spikes):
        bk.append(spike(f'psp_{n}{i}', sg * (10 + i * 1.2), 56, -2 + i * 3, 4 + (i % 2), 1, (0, 0, sg * (-25 - i * 14)), mat='o_dark'))


def straps(bk, mat='o_leather', studs=True, studmat='o_iron'):
    bk.append(C('strap_a', -1.5, 44, -4.6, 2, 14, 1, mat, key='g_strapa', rot=(0, 0, -34), pivot=(0, 50, -4)))
    bk.append(C('strap_b', -1.5, 44, 3.6, 2, 14, 1, mat, key='g_strapb', rot=(0, 0, -34), pivot=(0, 50, 4)))
    if studs:
        for i in range(3):
            bk.append(C(f'stud_{i}', -2 + i * 2, 46 + i * 2.6, -5.1, 1, 1, 1, studmat, key='g_stud'))


def vest(bk, mat='o_dleather', trim='o_leather'):
    """gilet de cuir clouté (type 2)"""
    bk.append(C('vest', -7.4, 43, -4.7, 14.8, 10, 1.6, mat, key='g_vest', fx=O.fx_chest))
    bk.append(C('vest_back', -7.4, 42, 3.1, 14.8, 11, 1.6, mat, key='g_vestb'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('vest_side_' + n, X(sg, 7.2, 1.6), 42, -3.3, 1.6, 11, 6.6, mat, key='g_vests'))
    bk.append(C('vest_low', -7.4, 41, -5, 14.8, 3, 1.6, trim, key='g_vestlow'))
    for i in range(5):
        bk.append(C(f'vest_stud_{i}', -5.4 + i * 2.7, 49, -5.1, 1, 1, 1, 'o_iron', key='g_stud'))
    for i in range(4):
        bk.append(C(f'vest_stud2_{i}', -4 + i * 2.7, 46, -5.1, 1, 1, 1, 'o_iron', key='g_stud'))


def rope_shoulder(bk):
    """corde en bandouliere sur l'epaule, grappe de noeuds (type 3)"""
    bk.append(C('rope', -1.5, 40, -4.6, 3, 18, 2, 'g_rope', key='g_rope', rot=(0, 0, 38), pivot=(0, 50, -4)))
    bk.append(C('rope_b', -1.5, 40, 2.6, 3, 18, 2, 'g_rope', key='g_ropeb', rot=(0, 0, 38), pivot=(0, 50, 4)))
    for i, (dx, dy, dz) in enumerate(((0, 0, 0), (2, -1, 1), (-1, -2.5, 1.5), (1.5, -3.5, -.5), (-2, -1, -1))):
        bk.append(C(f'knot_{i}', 8.4 + dx, 54 + dy, -1 + dz, 2, 2, 2, 'g_rope', key='g_knot', rot=(i * 20, i * 30, i * 10)))
    bk.append(C('tassel', 8.8, 49.5, -.5, 1, 4, 1, 'g_rope', key='g_tassel'))


def wrist_wraps(fores, sg, mat='o_wrap'):
    n = 'left' if sg < 0 else 'right'
    fores[sg].append(C('wrap_' + n, X(sg, 8.8, 4.8), 27, -2.4, 5, 5, 5, mat, key='g_fwrap'))


def straw_feet(legs, sg):
    n = 'left' if sg < 0 else 'right'
    legs[sg].children.append(C('strawfoot_' + n, X(sg, 4.2, 7), 0, -8, 7, 5, 11, 'g_straw', key='g_sfoot'))
    for i in range(5):
        legs[sg].children.append(C(f'strawshin_{n}{i}', X(sg, 4.2, 5.4) + i * 1.0, 5, -2.9 + (i % 2) * .8, 1, 4 + (i % 3), 1, 'g_straw', key=f'g_sfr{i % 3}', rot=(0, 0, (i - 2) * 6)))


def greaves(legs, sg, mat='g_red', knee=True):
    n = 'left' if sg < 0 else 'right'
    legs[sg].children.append(C('greave_' + n, X(sg, 4.2, 5.4), 7, -3, 5, 10, 6, mat, key='g_grv' + mat))
    legs[sg].children.append(C('thighwrap_' + n, X(sg, 4.2, 6.2), 22, -3.5, 6, 7, 7, mat, key='g_twr' + mat))
    if knee:
        legs[sg].children.append(C('kneeplate_' + n, X(sg, 4.2, 4.4), 15, -4.4, 4, 4, 1, 'o_iron', key='g_knee'))


def boots(legs, sg, mat='o_dleather'):
    n = 'left' if sg < 0 else 'right'
    legs[sg].children.append(C('boot_' + n, X(sg, 4.2, 6), 0, -7.6, 6, 10, 10, mat, key='g_boot' + mat))
    legs[sg].children.append(C('bootcuff_' + n, X(sg, 4.2, 7), 9, -3.4, 7, 2, 7, 'o_leather', key='g_bootcuff'))


# ---------------------------------------------------------------- armes (autour de l'origine, pointe vers +y)
def w_gspear():
    k = [C('shaft', -.8, -26, -.8, 2, 66, 2, 'o_wood', key='gw_shaft'),
         C('head', -.8, 40, -2.4, 1, 10, 5, 'o_blade', key='gw_head'),
         C('head_tip', -.8, 50, -1.2, 1, 4, 2, 'o_blade', key='gw_tip'),
         C('collar', -1.4, 38, -1.4, 3, 2, 3, 'o_wrap', key='gw_col'),
         C('tuft', -1.2, 36, -1.2, 2, 2, 2, 'g_red', key='gw_tuft')]
    for sg in (-1, 1):
        k.append(spike('barb_' + ('l' if sg < 0 else 'r'), 0, 39, sg * 2.2, 4, 1, (sg * 38, 0, 0), mat='o_blade', d=1))
    return k


def w_gtorch():
    k = [C('stick', -1, -6, -1, 2, 16, 2, 'o_wood', key='gt_stick'),
         C('wrap', -2, 10, -2, 4, 4, 4, 'g_rag', key='gt_wrap'),
         C('flame_a', -2, 14, -2, 4, 7, 4, 'g_flame', key='gt_fa', fx=fx_flame_glow),
         C('flame_b', -.8, 20, -.8, 2, 5, 2, 'g_flame', key='gt_fb', fx=fx_flame_glow),
         C('flame_c', 1.2, 16, -.8, 1, 4, 1, 'g_flame', key='gt_fc', fx=fx_flame_glow, rot=(0, 0, -14))]
    return k


def w_gpickaxe():
    """pioche de la brute : manche de bois, tete de fer a deux becs recourbes (autour de l'origine, pointe vers +y)"""
    k = [C('shaft', -1.5, -16, -1.5, 3, 54, 3, 'o_wood', key='gp_shaft'),
         C('grip', -2, -16, -2, 4, 14, 4, 'o_wrap', key='gp_grip'),
         C('core', -2.5, 34, -3, 5, 6, 6, 'o_iron', key='gp_core'),
         C('eye', -2, 31, -2, 4, 3, 4, 'o_dleather', key='gp_eye')]
    k.append(chain('pick_f', (0, 37, -3), 14, 4, 4, 0, 0, -88, -8, sg=1, step=2, prof=.9, mat='o_iron'))
    k.append(chain('pick_b', (0, 37, 3), 14, 4, 4, 0, 0, 88, 8, sg=1, step=2, prof=.9, mat='o_iron'))
    return k


def w_gdagger():
    k = [C('grip', -.8, -4, -.8, 2, 7, 2, 'o_wrap', key='gd_grip'),
         C('pommel', -1.2, -5, -1.2, 3, 1, 3, 'o_iron', key='gd_pom'),
         C('guard', -1.2, 3, -1.8, 3, 1, 4, 'o_iron', key='gd_guard'),
         C('blade', -.6, 4, -1.6, 1, 15, 3, 'o_blade', key='gd_blade'),
         C('tip', -.6, 19, -.8, 1, 4, 2, 'o_blade', key='gd_tip')]
    for i in range(3):
        k.append(C(f'notch_{i}', -.8, 7 + i * 4, 1.4, 2, 1, 1, 'o_dark', key='gd_notch'))
    return k


# ---------------------------------------------------------------- corps de gobelin
def widen(kids, b, bz=1.0, skip=('head', 'weapon')):
    """elargit le corps (x seulement) pour les gobelins plus costauds ; tete et armes ne sont pas deformees (l'arme suit la main)"""
    for ch in kids:
        if isinstance(ch, Group):
            if ch.name.startswith(skip):
                dx, dz = ch.pivot[0] * (b - 1), ch.pivot[2] * (bz - 1)
                if dx or dz:
                    translate(ch.children, dx, 0, dz)
                    ch.pivot = (ch.pivot[0] + dx, ch.pivot[1], ch.pivot[2] + dz)
                continue
            ch.pivot = (ch.pivot[0] * b, ch.pivot[1], ch.pivot[2] * bz)
            widen(ch.children, b, bz, skip)
        else:
            ctr = ch.frm[0] + ch.size[0] / 2
            nw = max(1, int(round(ch.size[0] * b)))
            ctz = ch.frm[2] + ch.size[2] / 2
            nd = max(1, int(round(ch.size[2] * bz)))
            ch.frm = (ctr * b - nw / 2, ch.frm[1], ctz * bz - nd / 2)
            ch.size = (nw, ch.size[1], nd)
            ch.pivot = (ch.pivot[0] * b, ch.pivot[1], ch.pivot[2] * bz)
            ch.key = f'{ch.key}_w{int(b * 100)}_{int(bz * 100)}'


def thicken(kids, px, pz, f):
    """epaissit un membre sur place (x et z, autour de son axe) ; les armes ne sont pas touchees"""
    for ch in kids:
        if isinstance(ch, Group):
            if ch.name.startswith('weapon'):
                continue
            ch.pivot = (px + (ch.pivot[0] - px) * f, ch.pivot[1], pz + (ch.pivot[2] - pz) * f)
            thicken(ch.children, px, pz, f)
        else:
            cx, cz = ch.frm[0] + ch.size[0] / 2, ch.frm[2] + ch.size[2] / 2
            nw, nd = max(1, int(round(ch.size[0] * f))), max(1, int(round(ch.size[2] * f)))
            ch.frm = (px + (cx - px) * f - nw / 2, ch.frm[1], pz + (cz - pz) * f - nd / 2)
            ch.size = (nw, ch.size[1], nd)
            ch.pivot = (px + (ch.pivot[0] - px) * f, ch.pivot[1], pz + (ch.pivot[2] - pz) * f)
            ch.key = f'{ch.key}_t{int(f * 100)}'


def goblin(cfg, skin):
    sk = gskin(skin)
    lean = cfg.get('lean', -26)
    legs = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        k = [C('thigh_' + n, X(sg, 4.2, 5), 19, -3, 5, 11, 6, sk, key='g_thigh'),
             C('shin_' + n, X(sg, 4.2, 4), 6, -2.4, 4, 13, 5, sk, key='g_shin'),
             C('calf_' + n, X(sg, 4.2, 3), 9, 2.4, 3, 6, 2, sk, key='g_calf'),
             C('foot_' + n, X(sg, 4.2, 5), 0, -7, 5, 4, 10, sk, key='g_foot')]
        for i in range(3):
            k.append(C(f'toe_{n}{i}', X(sg, 4.2, 5) + i * 1.9, 0, -9, 1, 2, 2, 'g_nail', key='g_toe'))
        legs[sg] = Group('leg_' + n, (sg * 4.2, 30, 0), (0, 0, 0), k)
    thin = cfg.get('thin', True)
    bk = [C('pelvis', -5, 28, -3.5, 10, 8, 7, sk, key='g_pelvis'),
          C('abdomen', -4.6, 36, -3.4, 9, 8, 7, sk, key='g_abd'),
          C('chest', -6.6, 44, -4, 13, 11, 8, sk, key='g_chest', fx=fx_ribs if thin else None),
          C('hump', -4.4, 47, 3.8, 9, 9, 3, sk, key='g_hump'),
          C('traps', -4.6, 54.4, -3, 9, 2, 7, sk, key='g_traps'),
          C('neck', -2.5, 55, -3.4, 5, 4, 5, sk, key='g_neck')]
    if cfg.get('gut'):                                                    # ventre rond
        g = cfg['gut']
        bk.append(C('gut', -5.5, 34, -3.4 - g, 11, 11, g, sk, key=f'g_gut{g}'))
        bk.append(C('gut_low', -5, 32, -3.4 - g * .6, 10, 3, g * .6, sk, key=f'g_gutl{g}'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('shoulder_' + n, X(sg, 8.5, 4), 50, -2.6, 4, 5, 5, sk, key='g_shoulder'))
    hk = goblin_head(cfg['head'], sk)
    arms, fores = {}, {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        arms[sg] = [C('uarm_' + n, X(sg, 9, 4), 41, -2, 4, 13, 4, sk, key='g_uarm')]
        fores[sg] = [C('farm_' + n, X(sg, 9, 3.6), 26, -1.8, 3, 15, 4, sk, key='g_farm'),
                     C('hand_' + n, X(sg, 9, 4), 21.4, -2, 4, 5, 4, sk, key='g_hand')]
        for i in range(3):
            fores[sg].append(C(f'finger_{n}{i}', X(sg, 9, 4) + i * 1.5, 17, -3.4 + (i == 1) * 1 - .5 * (i == 2), 1, 5, 1, sk, key='g_fing'))
            fores[sg].append(C(f'nail_{n}{i}', X(sg, 9, 4) + i * 1.5, 15.4, -3.4 + (i == 1) * 1 - .5 * (i == 2), 1, 2, 1, 'g_nail', key='g_nailc'))
    cfg['dress'](legs, bk, hk, arms, fores, sk)
    head_f = cfg.get('head_f', 1.3)
    scale_about(hk, head_f, (0, 57, -1))
    head = Group('head', (0, 57, -1), (-lean * cfg.get('htilt', .4), 0, 0), hk)
    farms = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        farms[sg] = Group('forearm_' + n, (sg * 9, 41, 0), (cfg.get('elbow', -8), 0, 0), fores[sg])
    wk = cfg.get('weapons', {})
    for sg, (fn, rot, off) in wk.items():
        kids = fn()
        scale_about(kids, cfg.get('wscale', 1.0), (0, 0, 0))
        hp = (sg * 9 + off[0], 22 + off[1], -1.2 + off[2])
        translate(kids, *hp)
        farms[sg].children.append(Group('weapon' if sg > 0 else 'weapon_l', hp, rot, kids))
    arm_rot = cfg.get('arm', (34, 0, 0))
    arm_groups = [Group('arm_' + ('left' if sg < 0 else 'right'), (sg * 9, 54, 0), (arm_rot[0], arm_rot[1], sg * arm_rot[2]), arms[sg] + [farms[sg]]) for sg in (-1, 1)]
    body = Group('body', (0, 30, 0), (lean, 0, 0), bk + [head] + arm_groups)
    groups = [legs[-1], legs[1], body]
    if cfg.get('bulk', 1) != 1:
        widen(groups, cfg['bulk'], 1 + (cfg['bulk'] - 1) * .7)
    f = cfg.get('limb', 1.0)
    if f != 1.0:
        for g in groups:
            if g.name.startswith('leg_'):
                thicken(g.children, g.pivot[0], g.pivot[2], f)
        for a in body.children:
            if isinstance(a, Group) and a.name.startswith('arm_'):
                thicken(a.children, a.pivot[0], a.pivot[2], f)
    unique_names(groups)
    return groups


# ---- type 1 (1m20) : le maigrichon, peau verte d'olive, grosse tete, lance, pieds enveloppes d'herbe, sangle rouge a gemme
def dress_scout(legs, bk, hk, arms, fores, sk):
    straps(bk)
    belt(bk, 36, 11)
    skirt(bk, 36, 7, 9, 'g_red', w=11)
    bk.append(C('chest_plate', -3.6, 46, -4.8, 7, 6, 1, 'o_dleather', key='g_cplate'))
    bk.append(C('gem_chest', -1, 48.5, -5.4, 2, 2, 1, 'g_gem', key='g_gem', fx=fx_gem_glow))
    for sg in (-1, 1):
        straw_feet(legs, sg)
        greaves(legs, sg)
        wrist_wraps(fores, sg, 'g_rag')


# ---- type 2 (1m50) : le guerrier, gilet de cuir clouté, epaulette, jupe de paille, bottes, cimeterre et torche
def dress_warrior(legs, bk, hk, arms, fores, sk):
    vest(bk)
    belt(bk, 36, 12, gem=False)
    skirt(bk, 36, 8, 12, 'g_straw', w=12)
    pauldron(bk, 1, w=9, spikes=2)
    bk.append(C('strap_l', X(-1, 7.6, 1.6), 43, -4, 1.6, 10, 8, 'o_leather', key='g_strapl'))
    for sg in (-1, 1):
        boots(legs, sg)
        wrist_wraps(fores, sg, 'o_leather')
    hk.append(C('headband', -6.6, 63, -7, 13, 2, 12, 'g_red', key='g_hband'))


# ---- type 3 (1m80) : le brute, peau pale tachetee, gros nez crochu, corde et noeuds a l'epaule, pagne de peau, coutelas crante
def dress_brute(legs, bk, hk, arms, fores, sk):
    rope_shoulder(bk)
    belt(bk, 36, 12, gem=False, mat='g_rope')
    skirt(bk, 36, 6, 11, 'g_hide', w=12)
    bk.append(C('shoulder_pad', X(-1, 9, 6), 53, -3.4, 6, 3, 7, 'g_hide', key='g_spad', rot=(0, 0, 14), pivot=(-9, 56, 0)))
    for sg in (-1, 1):
        wrist_wraps(fores, sg, 'g_rope')
        legs[sg].children.append(C('anklewrap_' + ('l' if sg < 0 else 'r'), X(sg, 4.2, 5.6), 5, -2.8, 6, 3, 6, 'g_rag', key='g_ankw'))
    bk.append(C('bone_a', -5, 38, -4.8, 1, 4, 1, 'o_tooth', key='g_bonea', rot=(0, 0, 20)))
    bk.append(C('bone_b', 3.4, 38, -4.8, 1, 5, 1, 'o_tooth', key='g_boneb', rot=(0, 0, -16)))


ALL = {}
TYPES = {
    'gobelin_1': dict(label='Gobelin éclaireur', m=1.2, skins=['8c9a3c', '6f8c4a', '9a8f3a'], fn=lambda sk: goblin(dict(
        lean=-34, head_f=1.42, thin=True, bulk=1.3, limb=1.18, arm=(40, 0, 4), elbow=-8, dress=dress_scout, wscale=1.0,
        head=dict(sw=12, sh=11, sd=11, eye=(236, 142, 36), eyesize=5, nose='small', mouth='open', jw=8, hair=False,
                  ear=dict(len=18, w=8, d=2, rz=70, drz=2, rx=34, drx=2)),
        weapons={}), sk)),
    'gobelin_2': dict(label='Gobelin guerrier', m=1.5, skins=['7a7e3e', '8a6c46', '66803e'], fn=lambda sk: goblin(dict(
        lean=-24, head_f=1.22, thin=False, bulk=1.4, limb=1.18, arm=(34, 0, 4), elbow=-10, dress=dress_warrior, wscale=.62,
        head=dict(sw=13, sh=11, sd=11, eye=(224, 156, 40), eyesize=5, nose='long', mouth='snarl', jw=9, hair=False,
                  ear=dict(len=15, w=7, d=2, rz=76, drz=1, rx=30, drx=2)),
        weapons={}), sk)),
    'gobelin_3': dict(label='Gobelin brute', m=1.8, skins=['b09c88', '9ea08a', 'a8987e'], fn=lambda sk: goblin(dict(
        lean=-20, head_f=1.14, thin=False, bulk=1.6, limb=1.15, arm=(30, 0, 4), elbow=-10, dress=dress_brute, wscale=.85,
        head=dict(sw=14, sh=12, sd=12, eye=(214, 150, 40), eyesize=5, nose='hook', mouth='fangs', jw=10, hair=False,
                  ear=dict(len=17, w=8, d=2, rz=84, drz=3, rx=42, drx=3)),
        weapons={1: (w_gpickaxe, (-20, 0, -4), (0, 0, -.5))}), sk)),
}

ALL.update(TYPES)


# ---------------------------------------------------------------- le Roi des gobelins (2m40)
def w_kstaff():
    """baton du roi : long manche de bois clouté, crane de chevre aux cornes enroulees (modele de la figurine)"""
    k = [C('shaft', -1.6, -34, -1.6, 4, 96, 4, 'o_wood', key='kw_shaft'),
         C('grip', -2, -4, -2, 5, 16, 5, 'o_wrap', key='kw_grip'),
         C('ring', -2.6, 56, -2.6, 6, 3, 6, 'o_iron', key='kw_ring'),
         C('ring2', -2.6, -30, -2.6, 6, 3, 6, 'o_iron', key='kw_ring2')]
    for i in range(5):
        k.append(C(f'stud_{i}', -.8, -26 + i * 5, 2.2, 2, 2, 2, 'o_iron', key='kw_stud', rot=(0, 0, 0)))
        k.append(C(f'stud_b{i}', -.8, -24 + i * 5, -4.2, 2, 2, 2, 'o_iron', key='kw_stud'))
    k += [C('goat_cranium', -4, 62, -4.5, 8, 7, 9, 'o_tooth', key='kw_cran'),
          C('goat_muzzle', -2.6, 56, -9.5, 5, 6, 6, 'o_tooth', key='kw_muz', fx=fx_skull_eyes),
          C('goat_eyes', -4.2, 65, -5.2, 8, 3, 1, 'o_tooth', key='kw_eyes', fx=fx_skull_eyes),
          C('goat_jaw', -2, 54, -8, 4, 2, 5, 'o_tooth', key='kw_jaw')]
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k.append(chain('horn_' + n, (sg * 4, 68, -1), 16, 3, 3, 62, -4, -8, -4, sg=sg, step=2, prof=.6, mat='o_tooth'))
        k.append(spike('spk_' + n, sg * 2, 70, -2, 6, 1, (0, 0, sg * -10), mat='o_tooth'))
    return k


def king_head(sk):
    """tete du roi, enorme : crane large, arcade lourde, petits yeux jaunes enfonces, gros nez, machoire en prognathisme, couronne de cornes"""
    y0 = 78
    k = [C('skull', -13, y0, -20, 26, 22, 22, sk, key='k_skull'),
         C('skull_top', -10, y0 + 22, -17, 20, 3, 18, sk, key='k_skulltop'),
         C('skull_back', -12, y0 + 3, 2, 24, 17, 3, sk, key='k_skullb'),
         C('brow', -14, y0 + 11, -24, 28, 5, 5, sk, key='k_brow', fx=fx_brow),
         C('brow_ridge', -6, y0 + 10, -25, 12, 3, 3, sk, key='k_brow2')]
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k.append(C('socket_' + n, X(sg, 6.5, 8), y0 + 6, -22.5, 8, 5, 3, sk, key='k_sock', fx=lambda F, rng, g: [put(F['north'], x, y, (0, 0, 0), .3) for x in range(8) for y in range(5)]))
        k.append(C('eye_' + n, X(sg, 6.5, 4), y0 + 7, -23.4, 4, 3, 2, sk, key='k_eye', fx=fx_eyeball((232, 188, 64), rim=(96, 56, 28))))
        k.append(C('cheek_' + n, X(sg, 11.6, 7), y0 + 1, -22, 7, 7, 7, sk, key='k_cheek'))
        k.append(C('jowl_' + n, X(sg, 11.4, 5), y0 - 6, -20, 5, 7, 8, sk, key='k_jowl'))
        k.append(C('tooth_' + n, sg * 6 - 1, y0 - 3, -25.5, 2, 4, 2, 'o_tooth', key='k_tooth'))
    k += [C('nose', -4, y0 + 2, -28, 8, 9, 8, sk, key='k_nose'),
          C('nose_tip', -5, y0 - 1, -31, 10, 5, 7, sk, key='k_nosetip', fx=fx_nose),
          C('jaw', -10, y0 - 7, -25, 20, 9, 20, sk, key='k_jaw', fx=fx_king_mouth),
          C('lip', -7, y0 - 11, -26.5, 14, 4, 3, sk, key='k_lip')]
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k.append(chain('ear_' + n, (sg * 12.6, y0 + 12, -4), 16, 9, 3, 82, 2, 32, 2, sg=sg, step=2, prof=.55, mat=sk))
    # couronne : bandeau de fil de fer enroule + cornes d'os
    k.append(C('crown_band', -12, y0 + 24, -19, 24, 3, 20, 'g_wire', key='k_cband'))
    for i, (x, z, ln, rx, rz, sg) in enumerate(((0, -17, 24, -14, 0, 1), (-9, -9, 20, -4, 14, -1), (9, -9, 20, -4, 14, 1), (-5, -1, 16, 8, 8, -1), (5, -1, 16, 8, 8, 1))):
        k.append(chain(f'crown_spike_{i}', (x, y0 + 26, z), ln, 5, 5, rz, 1.5, rx, -1, sg=sg, step=3, prof=.8, mat='g_horn'))
        k.append(C(f'crown_wire_{i}', x - 3, y0 + 26, z - 3, 6, 3, 6, 'g_wire', key='k_cwire'))
        k.append(C(f'crown_wire2_{i}', x - 2.5, y0 + 31, z - 2.5, 5, 2, 5, 'g_wire', key='k_cwire2'))
    # cheveux clairsemes, fils pales qui pendent de chaque cote
    for sg in (-1, 1):
        for i in range(6):
            k.append(C(f'hair_{"l" if sg < 0 else "r"}{i}', sg * (11 + i * .4) - .5, y0 + 4 + (i % 3), -16 + i * 3, 1, 14 + (i * 5) % 6, 1, 'g_hair', key=f'k_hair{i}'))
    for i in range(5):
        k.append(C(f'hair_t{i}', -6 + i * 3, y0 + 21, -19 + (i % 2) * 2, 1, 6, 1, 'g_hair', key='k_hairt'))
    return k


def king_dewlap():
    """la bajoue qui pend du menton : poche lourde terminee par un gros sac arrondi (3 segments articules pour la balancer)"""
    d3 = Group('dewlap3', (0, 52, -19), (0, 0, 0), [C('dew3', -8, 36, -26, 16, 16, 14, 'g_dew', key='k_dew3'), C('dew_tip', -5, 30, -24, 10, 7, 10, 'g_dew', key='k_dewtip'),
                                                  C('dew_bump_a', 5, 40, -27, 3, 3, 2, 'g_dew', key='k_dewb'), C('dew_bump_b', -8.6, 44, -24, 2, 4, 3, 'g_dew', key='k_dewb4'),
                                                  C('dew_bump_e', -2, 46, -27.6, 3, 3, 2, 'g_dew', key='k_dewb')])
    d2 = Group('dewlap2', (0, 64, -19), (0, 0, 0), [C('dew2', -6, 51, -24, 12, 14, 10, 'g_dew', key='k_dew2'),
                                                  C('dew_bump_c', 3, 56, -25, 3, 3, 2, 'g_dew', key='k_dewb2'), C('dew_bump_d', -6.6, 55, -22, 2, 4, 3, 'g_dew', key='k_dewb3')] + [d3])
    d1 = Group('dewlap', (0, 76, -19), (0, 0, 0), [C('dew1', -5, 63, -23, 10, 14, 8, 'g_dew', key='k_dew1')] + [d2])
    return d1


def king(skin='c6ab9a'):
    sk = gskin(skin, 'king')
    lean = -30
    legs = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        k = [C('thigh_' + n, X(sg, 9.5, 12), 22, -6, 12, 15, 13, sk, key='k_thigh'),
             C('shin_' + n, X(sg, 9.5, 9), 6, -5, 9, 17, 10, sk, key='k_shin'),
             C('foot_' + n, X(sg, 9.5, 11), 0, -11, 11, 6, 16, sk, key='k_foot'),
             C('hide_' + n, X(sg, 9.5, 13), 28, -7, 13, 9, 14, 'g_hide', key='k_hidel')]
        for i in range(3):
            k.append(C(f'toe_{n}{i}', X(sg, 9.5, 11) + i * 3.8, 0, -14.4, 3, 3, 4, sk, key='k_toe'))
            k.append(C(f'toenail_{n}{i}', X(sg, 9.5, 11) + i * 3.8, 0, -16.6, 3, 2, 2, 'g_nail', key='k_toen'))
        legs[sg] = Group('leg_' + n, (sg * 9.5, 36, 0), (0, 0, 0), k)
    bk = [C('pelvis', -12, 30, -9, 24, 12, 18, sk, key='k_pelvis'),
          C('chest', -17, 50, -11, 34, 24, 22, sk, key='k_chest'),
          C('hump', -13, 60, 6, 26, 30, 16, sk, key='k_hump'),
          C('hump_top', -9, 88, 8, 18, 5, 11, sk, key='k_humptop'),
          C('neck', -7, 74, -8, 14, 6, 12, sk, key='k_neck'),
          C('belt_rope', -15, 32, -15, 30, 3, 22, 'g_rope', key='k_belt')]
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('pec_' + n, X(sg, 9, 12), 55, -17, 12, 10, 6, sk, key='k_pec', fx=fx_pecs))
        bk.append(C('trap_' + n, X(sg, 11, 10), 72, -7, 10, 7, 14, sk, key='k_trap'))
        bk.append(C('lovehandle_' + n, X(sg, 16, 6), 38, -14, 6, 10, 18, sk, key='k_love'))
    for i in range(7):                                                    # pagne de peaux sous le ventre
        bk.append(C(f'rag_{i}', -14 + i * 4.2, 14, -12.5 - (i % 2) * 1.2, 4, 18 + (i * 5) % 6, 1, 'g_hide', key=f'k_rag{i % 3}'))
    for i in range(5):
        bk.append(C(f'rag_b{i}', -10 + i * 4.4, 18, 8.5, 4, 14 + (i * 3) % 5, 1, 'g_hide', key=f'k_ragb{i % 2}'))
    # ventre : groupe articule (pour le faire ballotter a chaque pas)
    sl = []
    prof = [(24, 14, 54), (30, 20, 50), (34, 25, 45), (36, 28, 40), (36, 29, 35), (34, 27, 31), (30, 22, 28)]   # (largeur, profondeur, y bas) de haut en bas
    for i, (w, d, yb) in enumerate(prof):
        top = prof[i - 1][2] if i else 58
        sl.append(C(f'belly_{i}', -w / 2, yb, -d - 2, w, top - yb, d + 2, sk, key=f'k_belly{i}', fx=fx_belly if i == 4 else None))
    belly = Group('belly', (0, 52, -6), (0, 0, 0), sl)
    hk = king_head(sk)
    dew = king_dewlap()
    head_rot = -lean * .4
    dew.rot = (-(lean + head_rot) + 4, 0, 0)
    hk.append(dew)
    head = Group('head', (0, 78, -6), (head_rot, 0, 0), hk)
    arms = []
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        fore = [C('farm_' + n, X(sg, 22, 11), 27, -5.5, 11, 25, 11, sk, key='k_farm'),
                C('hand_' + n, X(sg, 22, 13), 16, -6.5, 13, 11, 13, sk, key='k_hand'),
                C('thumb_' + n, X(sg, 22 - sg * -1 * 0, 4) - sg * 7, 19, -9, 4, 6, 4, sk, key='k_thumb')]
        for i in range(3):
            fore.append(C(f'knuckle_{n}{i}', X(sg, 22, 12) + i * 4.5, 14, -9.5, 3, 3, 3, sk, key='k_knu'))
            fore.append(C(f'claw_{n}{i}', X(sg, 22, 12) + i * 4.5, 10.6, -9.2, 2, 4, 2, 'g_nail', key='k_claw'))
        fg = Group('forearm_' + n, (sg * 22, 52, 0), (-8, 0, 0), fore)
        if sg == 1:
            hp = (22, 21, -2)
            kids = w_kstaff()
            translate(kids, *hp)
            fg.children.append(Group('weapon', hp, (-16, 0, -4), kids))
        arms.append(Group('arm_' + n, (sg * 22, 76, 0), (40, 0, sg * 3), [
            C('shoulder_' + n, X(sg, 22, 15), 66, -7, 15, 14, 14, sk, key='k_shoulder'),
            C('uarm_' + n, X(sg, 22, 12), 51, -6, 12, 18, 12, sk, key='k_uarm'), fg]))
    body = Group('body', (0, 36, 0), (lean, 0, 0), bk + [belly, head] + arms)
    groups = [legs[-1], legs[1], body]
    unique_names(groups)
    return groups


T.MATS['g_dew'] = dict(base=(188, 150, 130), n1=8, n2=8, tint=(1, .9, .85))
T.DECOR['g_dew'] = d_dew

ALL['gobelin_roi'] = dict(label='Roi des gobelins', m=2.7, excl=('weapon', 'ear_', 'hair_'), skins=['c6ab9a', 'b8a48e'], fn=lambda sk: king(sk))


# ---------------------------------------------------------------- construction, mesure, export
EXCL = ('weapon', 'ear_', 'flame', 'crown', 'hair_', 'spike_')


def top_height(groups, excl=EXCL):
    """hauteur du modele (unites) : sommet du crane, oreilles/couronne/armes exclues, rotations appliquees comme le rendu"""
    top = 0.0
    for c, chn in walk(groups):
        if any(g.name.startswith(excl) for g in chn) or c.name.startswith(excl):
            continue
        w, h, d = c.size
        lo = np.array(c.frm) - c.inflate
        hi = np.array(c.frm) + np.array((w, h, d)) + c.inflate
        for dx in (0, 1):
            for dy in (0, 1):
                for dz in (0, 1):
                    p = np.array((lo[0] if not dx else hi[0], lo[1] if not dy else hi[1], lo[2] if not dz else hi[2]))
                    p = rot_mat(c.rot) @ (p - np.array(c.pivot)) + np.array(c.pivot)
                    for g in chn:
                        p = rot_mat(g.rot) @ (p - np.array(g.pivot)) + np.array(g.pivot)
                    top = max(top, p[1])
    return top


def build(name, skin=None):
    T._N[0] = 0
    spec = ALL[name]
    sk = skin or spec['skins'][0]
    groups = spec['fn'](sk)
    target = spec['m'] * 50                                  # 1 unite = 2 cm
    f = target / top_height(groups, spec.get('excl', EXCL))
    scale_all(groups, f)
    normalize(groups)
    unique_names(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    return groups, cubes, W, H, tex, glow, f


def sheet(path, names=None, views=(0, 40, 90, 180), scale=None):
    from PIL import Image
    rows = []
    for name in names or ALL:
        groups, cubes, W, H, tex, glow, f = build(name)
        sc = scale or 400 / (ALL[name]['m'] * 50)
        ims = []
        for y in views:
            ims.append(render(groups, tex, y, pitch=-8, W=330, H=520, scale=sc))
        rows.append(np.concatenate(ims, axis=1))
    Image.fromarray(np.concatenate(rows, axis=0)).save(path)


def generate(outdir):
    """ecrit, pour chaque modele : .bbmodel (Blockbench), geometrie JSON du mod, une texture par couleur de peau (+ texture lumineuse si besoin).
    Rien n'est ecrit dans src/main/resources : les modeles ne sont pas encore branches."""
    import json, os, bbimport
    from PIL import Image
    import models
    os.makedirs(outdir, exist_ok=True)
    ren = {'leg_left': 'leg_l', 'leg_right': 'leg_r', 'arm_left': 'arm_l', 'arm_right': 'arm_r'}
    meta = {}
    for name, spec in ALL.items():
        ref = None
        for i, sk in enumerate(spec['skins']):
            groups, cubes, W, H, tex, glow, f = build(name, sk)
            Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, f'{name}_{i}.png'))
            if i == 0:
                export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
                geo = bbimport.convert(os.path.join(outdir, name + '.bbmodel'), rename=ren)
                geo['height'] = models.geo_height(geo)
                json.dump(geo, open(os.path.join(outdir, name + '.json'), 'w'), separators=(',', ':'))
                if glow[..., 3].any():
                    Image.fromarray(glow, 'RGBA').save(os.path.join(outdir, name + '_glow.png'))
                ref = [(p['name'], p['boxes']) for p in geo['parts']]
                meta[name] = dict(nom=spec['label'], taille_m=spec['m'], hauteur_modele_px=geo['height'], pieces=len(geo['parts']), cubes=len(cubes),
                                  texture=f'{W}x{H}', peaux=len(spec['skins']))
            else:                                    # meme disposition UV pour toutes les peaux
                assert [(p['name'], p['boxes']) for p in [dict(name=n, boxes=b) for n, b in ref]] == ref
        print(name, meta[name])
    json.dump(meta, open(os.path.join(outdir, 'gobelins.json'), 'w'), ensure_ascii=False, indent=1)
    return meta


def lineup(path):
    """les 4 modeles cote a cote a la meme echelle (hauteurs reelles) + regle d'un humain de 1m80"""
    from PIL import Image, ImageDraw
    sc = 5.0
    ims = []
    for name in ALL:
        groups, cubes, W, H, tex, glow, f = build(name)
        ims.append(render(groups, tex, 35, pitch=-6, W=300, H=620, scale=sc))
    im = Image.fromarray(np.concatenate(ims, axis=1))
    d = ImageDraw.Draw(im)
    for m in (1.2, 1.5, 1.8, 2.7):
        y = 620 - 40 - m * 50 * sc
        d.line([(0, y), (im.width, y)], fill=(120, 160, 120), width=1)
        d.text((4, y - 11), f'{m:.1f} m', fill=(170, 210, 170))
    im.save(path)


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == 'gen':
        generate(sys.argv[2] if len(sys.argv) > 2 else '/tmp/gobelins')
        lineup('/tmp/gobelins_taille.png')
        sheet('/tmp/gobelins_vues.png')
    else:
        sheet('/tmp/gob.png', sys.argv[1:] or None)
