"""Armures 3D du film : orc (Uruk-hai d'Isengard), nain (Erebor), elfe (Foret Noire / Lorien).
python3 tools/armors3d.py preview   -> /tmp/armors_<set>.png (face / profil / dos sur mannequin) + /tmp/armors_tiles.png
python3 tools/armors3d.py gen       -> ecrit geo/armor_<set>_<piece>.json et textures/models/armor/<set>_layer_{1,2}.png"""
import math, os, sys, json
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from armorlib import *

R = lambda deg: math.radians(deg)
SETS = {}


# ------------------------------------------------------------------ decor commun
def scratches(F, rng, n, light=(230, 230, 235), t=.18):
    for _ in range(n):
        x, y = int(rng.integers(0, max(1, F.w))), int(rng.integers(0, max(1, F.h)))
        for k in range(int(rng.integers(2, 5))): F.px(x + k, y, light, t)


def rust(F, rng, n, col=(120, 70, 40), t=.5):
    for _ in range(n):
        x, y = int(rng.integers(0, max(1, F.w))), int(rng.integers(0, max(1, F.h)))
        F.px(x, y, col, t)
        if rng.random() < .5: F.px(x + 1, y, col, t * .7)
        if rng.random() < .5: F.px(x, y + 1, col, t * .7)


def rivets(F, col=(210, 205, 190), inset=1):
    if F.w >= 5 and F.h >= 5:
        for x, y in ((inset, inset), (F.w - 1 - inset, inset), (inset, F.h - 1 - inset), (F.w - 1 - inset, F.h - 1 - inset)):
            F.px(x, y, col, .85)


def ridge(F, y, dark, light, x0=0, x1=None):
    x1 = F.w - 1 if x1 is None else x1
    F.hline(y, x0, x1, dark, .9)
    F.hline(y + 1, x0, x1, light, .55)


def jagged_bottom(F, rng, rows=2):
    for x in range(F.w):
        k = int(rng.integers(0, rows + 1))
        for y in range(F.h - k, F.h): F.al[y, x] = 0


def plate(rust_n=2, scr=4, riv=True, rows=(), dark=(30, 30, 36), light=(160, 160, 170), hem=True, rust_col=(120, 70, 40)):
    """decor de plaque generique : rayures, rouille, rivets, nervures horizontales, ourlet sombre"""
    def f(face, F, rng):
        scratches(F, rng, scr); rust(F, rng, rust_n, rust_col)
        if face not in ('top', 'bottom'):
            for y in rows: ridge(F, y, dark, light) if y + 1 < F.h else None
            if hem and F.h > 2: F.hline(F.h - 1, 0, F.w, dark, .65)
        if riv: rivets(F)
    return f


def gold_art(rows, col, dark):
    def f(face, F, rng):
        scratches(F, rng, 2, (255, 240, 190), .25)
        if face in ('front', 'back', 'left', 'right') and F.h >= 4:
            y0 = (F.h - len(rows)) // 2
            for x in range(0, F.w, len(rows[0]) + 1):
                F.art(x, y0, rows, {'#': col, 'o': dark})
    return f


# ================================================================== ORC (Uruk-hai)
mat('ost', (84, 84, 92), 5, 6, (1, 1, 1.06))
mat('oru', (112, 80, 58), 6, 8)
mat('olea', (56, 44, 38), 4, 5, (1, .92, .85))
mat('oblk', (38, 38, 44), 4, 5, (1, 1, 1.08))
HAND = (232, 232, 226)


def d_o_ring(face, F, rng):
    scratches(F, rng, 5); rust(F, rng, 3)
    if face == 'front':
        F.art(2, 0, ['#.#.', '####', '####', '.##.'], {'#': HAND})            # main blanche d'Isengard
        F.hline(F.h - 1, 0, F.w, (30, 30, 34), .7)
    elif face in ('left', 'right'):
        ridge(F, 2, (40, 40, 46), (140, 140, 150)); rivets(F)
    elif face == 'back':
        F.hline(1, 0, F.w, (36, 36, 42), .6); F.hline(3, 0, F.w, (36, 36, 42), .6)
    rivets(F)


def d_o_buckle(face, F, rng):
    F.rect(0, 0, F.w, F.h, (118, 96, 52), .9)
    if F.w >= 3 and F.h >= 2: F.rect(1, 0, max(1, F.w - 2), F.h, (36, 30, 26), .9); F.px(F.w // 2, 0, (190, 160, 90))


def d_o_spike(face, F, rng):
    F.rect(0, 0, F.w, F.h, (176, 168, 150), .35)
    if face == 'top': F.rect(0, 0, F.w, F.h, (232, 226, 210), .5)


def d_o_neck(face, F, rng):
    for y in range(0, F.h, 2): F.hline(y, 0, F.w, (30, 28, 30), .6)
    for x in range(0, F.w, 2): F.px(x, 0, (120, 118, 110), .5)
    if face in ('front', 'back'): jagged_bottom(F, rng, 1)


def d_o_body(face, F, rng):
    scratches(F, rng, 10); rust(F, rng, 4)
    if face == 'front':
        for y in (3, 6): ridge(F, y, (36, 36, 42), (150, 150, 160))
        F.vline(3, 0, 9, (36, 36, 42), .6); F.vline(4, 0, 9, (36, 36, 42), .6)
        for x, y in ((1, 1), (6, 1), (1, 4), (6, 4), (1, 7), (6, 7)): F.px(x, y, (215, 205, 180), .8)
        F.rect(0, 10, 8, 2, (44, 34, 30))
        F.rect(1, 9, 6, 1, (30, 28, 30), .6)
    elif face == 'back':
        F.vline(3, 0, 11, (36, 36, 42), .6); F.vline(4, 0, 11, (36, 36, 42), .6)
        for y in (3, 6, 9): ridge(F, y, (36, 36, 42), (140, 140, 150))
        F.rect(0, 10, 8, 2, (44, 34, 30))
    elif face in ('left', 'right'):
        for y in (3, 6, 9): ridge(F, y, (36, 36, 42), (130, 130, 140))
        F.rect(0, 10, F.w, 2, (44, 34, 30))
    elif face == 'top':
        F.rect(1, 1, 6, 2, (30, 30, 34), .6)


def d_o_belt(face, F, rng):
    for x in range(0, F.w, 2): F.px(x, 0, (20, 16, 14), .55)
    if F.h >= 2: F.hline(F.h - 1, 0, F.w, (24, 18, 16), .6)
    scratches(F, rng, 3, (150, 120, 90), .22)


def d_o_skirt(face, F, rng):
    for y in range(1, F.h, 2): F.hline(y, 0, F.w, (30, 24, 22), .45)
    if face in ('front', 'back'):
        for x in range(0, F.w, 2): F.vline(x, 0, F.h, (28, 22, 20), .45)
        jagged_bottom(F, rng, 2)
    scratches(F, rng, 4, (110, 90, 70), .2)


def d_o_boot(face, F, rng):
    scratches(F, rng, 5, (160, 140, 120), .15)
    for y in range(2, F.h, 3): F.hline(y, 0, F.w, (24, 20, 18), .4)
    if face == 'bottom': F.rect(0, 0, F.w, F.h, (28, 26, 28))
    if face in ('front', 'back', 'left', 'right'): F.hline(F.h - 1, 0, F.w, (18, 16, 16), .8)


def box_m(part, o, sz, m, infl, key, deco):
    c = part.box(o, sz, m, infl, key, deco)
    c.mirror = True
    return c


def orc():
    P = plate
    head = Part('head')
    head.box((-4, -8, -4), (8, 4, 8), 'ost', .55, 'o_ring', d_o_ring)
    head.box((-3, -9.6, -3), (6, 2, 6), 'ost', .45, 'o_dome', P(rows=(), rust_n=2))
    ck = head.box((-4.3, -4, -3), (1, 4, 4), 'ost', .3, 'o_cheek', P(rust_n=1, riv=False))
    head.children.append(mirrored(Part('x', (0, 0, 0)), 'x'))
    head.children.pop()
    cl = Cube((3.3, -4, -3), (1, 4, 4), 'ost', .3, 'o_cheek', P(rust_n=1, riv=False), True); head.cubes.append(cl)
    head.box((-0.5, -4, -4.6), (1, 4, 1), 'oblk', .2, 'o_nose', P(rust_n=1, riv=False))
    crest = Part('crest', (0, -9.9, 0)); head.children.append(crest)
    crest.box((-2, -0.5, -3), (4, 1, 6), 'ost', 0, 'o_crest', P(rust_n=1, riv=False, hem=False))
    wr = Part('crest_r', (-2, -9.8, 0), (0, 0, R(28))); head.children.append(wr)
    wr.box((-4, -0.5, -3), (4, 1, 6), 'ost', 0, 'o_crest', P(rust_n=1, riv=False, hem=False))
    head.children.append(mirrored(wr, 'crest_l'))
    neck = Part('neck', (0, -2.6, 3.4), (R(14), 0, 0)); head.children.append(neck)
    neck.box((-4, 0, 0), (8, 3, 1), 'olea', 0, 'o_neck', d_o_neck)

    body = Part('body')
    body.box((-4, 0, -2), (8, 12, 4), 'ost', .6, 'o_body', d_o_body)
    for y in (1.3, 4.3, 7.3): body.box((-1.5, y, -3.1), (3, 2, 1), 'oblk', .1, 'o_rib', P(rust_n=2, riv=False, rows=(0,), light=(175, 175, 185)))
    body.box((-1.5, 10, -3.5), (3, 2, 1), 'oru', 0, 'o_buckle', d_o_buckle)
    ra = Part('right_arm')
    pa = Part('pauld', (0, -2.7, 0), (0, 0, R(-20))); ra.children.append(pa)
    pa.box((-5, -1, -3), (7, 2, 6), 'ost', .1, 'o_paul', P(rows=(2,)))
    sp = Part('sp1', (-3.5, -1, 0), (0, 0, R(-40))); pa.children.append(sp)
    sp.box((-0.5, -3, -0.5), (1, 3, 1), 'oblk', 0, 'o_spike', d_o_spike)
    sp2 = Part('sp2', (-1.5, -1, 0), (0, 0, R(-15))); pa.children.append(sp2)
    sp2.box((-0.5, -3, -0.5), (1, 3, 1), 'oblk', 0, 'o_spike', d_o_spike)
    pb = Part('pauld2', (0, 0.4, 0), (0, 0, R(-34))); ra.children.append(pb)
    pb.box((-4, -1, -2), (4, 2, 4), 'oru', 0, 'o_paul2', P(rows=(), rust_n=3))
    ra.box((-3, 5, -2), (4, 3, 4), 'ost', .55, 'o_brac', P(rows=(), rust_n=2))
    la = mirrored(ra, 'left_arm')
    chest = [body, ra, la]

    body = Part('body')
    body.box((-4, 11, -2), (8, 2, 4), 'olea', .85, 'o_belt2', d_o_belt)
    body.box((-4, 12.5, -2), (8, 3, 4), 'olea', .8, 'o_skirt', d_o_skirt)
    body.box((-3, 12, -2), (6, 4, 1), 'ost', 1.05, 'o_apron', P(rows=(1,)))
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 5, 4), 'ost', .6, 'o_thigh', P(rows=(1,)))
    rl.box((-2.5, 4.2, -3.3), (4, 3, 2), 'oru', 0, 'o_knee', P(rows=()))
    rl.box((-1, 4.6, -5.0), (1, 1, 2), 'oblk', 0, 'o_spikez', d_o_spike)
    ll = mirrored(rl, 'left_leg')
    legs = [body, rl, ll]

    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 6, 4), 'olea', .55, 'o_boot', d_o_boot)
    rl.box((-2, 2.5, -2.9), (4, 6, 1), 'ost', .1, 'o_greave', P(rows=(2,)))
    rl.box((-2.5, 5.2, -2.5), (5, 1, 5), 'ost', .1, 'o_cuff', P(rows=()))
    rl.box((-2, 10.2, -3.6), (4, 2, 2), 'oblk', 0, 'o_toe', P(rows=()))
    rl.box((-3.8, 4.4, -0.5), (1, 2, 1), 'oblk', 0, 'o_spike2', d_o_spike)
    ll = mirrored(rl, 'left_leg')
    boots = [rl, ll]
    return {'head': [head], 'chest': chest, 'legs': legs, 'boots': boots}


SETS['orc'] = orc


# ================================================================== NAIN (Erebor)
mat('dst', (66, 68, 78), 4, 5, (1, 1, 1.07))
mat('dbr', (158, 108, 52), 5, 6, (1.05, 1, .85))
mat('dgo', (206, 160, 62), 5, 6, (1.05, 1, .8))
mat('dcl', (150, 128, 92), 5, 6, (1, .97, .9))
mat('dle', (92, 60, 38), 4, 5, (1, .9, .8))
mat('dmail', (108, 112, 124), 6, 7, (1, 1, 1.05))
DG, DGD = (222, 176, 70), (80, 52, 24)


def d_mail(face, F, rng):
    for y in range(F.h):
        for x in range(F.w):
            if (x + y) % 2 == 0: F.px(x, y, (54, 56, 66), .5)
            elif y % 2 == 0: F.px(x, y, (170, 174, 186), .25)
    if face in ('front', 'back'): jagged_bottom(F, rng, 1)


def d_d_ring(face, F, rng):
    scratches(F, rng, 4)
    if face in ('front', 'back', 'left', 'right'):
        F.hline(0, 0, F.w, DG, .9); F.hline(1, 0, F.w, DGD, .55)
        F.art(0, 1, ['.#..#..', '###.###', '.#..#..'][: F.h - 1], {'#': DG}) if face == 'front' else None
        if face != 'front':
            for x in range(0, F.w, 4): F.art(x, 1, ['#.#', '###'], {'#': DG})
        F.hline(F.h - 1, 0, F.w, DGD, .6)
    rivets(F, (230, 200, 130))
    if face == 'front':
        F.clear(1, F.h - 1, 2, 1) if False else None


def d_d_cap(face, F, rng):
    scratches(F, rng, 4)
    if face == 'top': F.vline(F.w // 2, 0, F.h - 1, DGD, .6)
    rivets(F, (230, 200, 130))


def d_d_trim(face, F, rng):
    scratches(F, rng, 2, (255, 230, 170), .3); rivets(F, (240, 215, 150), 0)
    if face not in ('top', 'bottom') and F.h >= 3: F.hline(F.h - 1, 0, F.w, DGD, .6)


def d_d_body(face, F, rng):
    scratches(F, rng, 8)
    if face == 'front':
        F.rect(2, 0, 4, 12, (160, 136, 98), .85)                                          # gambeson clair au centre
        for y in range(0, 12, 2): F.hline(y, 2, 5, (118, 98, 68), .6)
        F.vline(1, 0, 11, DGD, .8); F.vline(6, 0, 11, DGD, .8)                              # sangles
        F.rect(0, 9, 8, 3, (70, 46, 30))
        F.hline(9, 0, 7, (30, 20, 12), .8)
        for x in (0, 7): F.px(x, 2, DG)
    elif face == 'back':
        F.rect(2, 0, 4, 11, (150, 128, 92), .8); F.vline(1, 0, 11, DGD, .8); F.vline(6, 0, 11, DGD, .8)
        F.rect(0, 9, 8, 3, (70, 46, 30))
    elif face in ('left', 'right'):
        F.rect(0, 9, F.w, 3, (70, 46, 30)); F.hline(3, 0, F.w, DGD, .6)


def d_d_buckle(face, F, rng):
    F.rect(0, 0, F.w, F.h, DG, .95)
    if F.w >= 4: F.rect(1, 0, F.w - 2, F.h, (70, 50, 26), .9); F.px(F.w // 2, 0, DG); F.px(F.w // 2 - 1, 1, DG)


def d_d_pauld(face, F, rng):
    scratches(F, rng, 4)
    if face == 'top':
        F.hline(0, 0, F.w, DG, .8)
        for x in range(1, F.w, 3): F.px(x, 2, DG)
    F.hline(F.h - 1, 0, F.w, DG, .8) if face != 'top' and face != 'bottom' else None
    rivets(F, (235, 205, 140), 0)


def d_d_belt(face, F, rng):
    for x in range(0, F.w, 2): F.px(x, 0, (30, 20, 12), .5)
    F.hline(F.h - 1, 0, F.w, (30, 20, 12), .6)
    if face == 'front':
        F.rect(F.w // 2 - 1, 0, 2, F.h, DG, .0)


def d_d_greave(face, F, rng):
    scratches(F, rng, 3)
    if face in ('front', 'back'):
        F.hline(1, 0, F.w, DG, .9); F.hline(F.h - 2, 0, F.w, DG, .9)
        F.art(F.w // 2 - 1, 2, ['.#.', '###'], {'#': DG}) if F.h >= 6 else None
    rivets(F, (235, 205, 140))


def dwarf():
    P = plate
    head = Part('head')
    head.box((-4, -8, -4), (8, 4, 8), 'dst', .55, 'd_ring', d_d_ring)
    head.box((-3, -9.5, -3), (6, 2, 6), 'dst', .45, 'd_dome', d_d_cap)
    head.box((-0.5, -11.5, -3.5), (1, 1, 7), 'dbr', 0, 'd_ridge', d_d_trim)
    head.box((-4.3, -4, -3), (1, 4, 4), 'dbr', .3, 'd_cheek', d_d_trim)
    head.cubes.append(Cube((3.3, -4, -3), (1, 4, 4), 'dbr', .3, 'd_cheek', d_d_trim, True))
    head.box((-0.5, -4, -4.6), (1, 4, 1), 'dbr', .2, 'd_nose', d_d_trim)
    neck = Part('neck', (0, -2.6, 3.4), (R(12), 0, 0)); head.children.append(neck)
    neck.box((-4, 0, 0), (8, 3, 1), 'dmail', 0, 'd_neck', d_mail)

    body = Part('body')
    body.box((-4, 0, -2), (8, 12, 4), 'dst', .6, 'd_body', d_d_body)
    body.box((-1.5, 10, -3.5), (3, 2, 1), 'dgo', 0, 'd_buckle', d_d_buckle)
    for y in (1.3, 4.5): body.box((-1.5, y, -3.1), (3, 2, 1), 'dbr', .1, 'd_pec', d_d_trim)
    ra = Part('right_arm')
    for i, (py, rz, x0, w) in enumerate(((-2.8, -8, -4.5, 5), (-1.0, -17, -4, 5))):
        pa = Part(f'pauld{i}', (0, py, 0), (0, 0, R(rz))); ra.children.append(pa)
        pa.box((x0, -1, -2.5), (w, 1, 5), 'dst', .15, 'd_paul', d_d_pauld)
    ra.box((-3, 4.5, -2), (4, 3, 4), 'dst', .55, 'd_brac', d_d_trim)
    la = mirrored(ra, 'left_arm')
    chest = [body, ra, la]

    body = Part('body')
    body.box((-4, 11, -2), (8, 2, 4), 'dle', .85, 'd_belt2', d_d_belt)
    body.box((-4, 12.5, -2), (8, 4, 4), 'dmail', .8, 'd_skirt', d_mail)
    body.box((-3.6, 12, -2.9), (3, 4, 1), 'dst', .85, 'd_tass', d_d_greave)
    body.cubes.append(Cube((0.6, 12, -2.9), (3, 4, 1), 'dst', .85, 'd_tass', d_d_greave, True))
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 5, 4), 'dst', .6, 'd_thigh', d_d_trim)
    rl.box((-2.3, 4.4, -3.2), (4, 3, 2), 'dbr', 0, 'd_knee', d_d_trim)
    ll = mirrored(rl, 'left_leg')
    legs = [body, rl, ll]

    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 6, 4), 'dle', .55, 'd_boot', d_o_boot)
    rl.box((-2, 2.5, -2.9), (4, 6, 1), 'dst', .1, 'd_greave', d_d_greave)
    rl.box((-2.5, 5.2, -2.5), (5, 1, 5), 'dbr', .1, 'd_cuff', d_d_trim)
    rl.box((-2, 10.2, -3.6), (4, 2, 2), 'dst', 0, 'd_toe', d_d_trim)
    ll = mirrored(rl, 'left_leg')
    return {'head': [head], 'chest': chest, 'legs': legs, 'boots': [rl, ll]}


SETS['dwarven'] = dwarf


# ================================================================== ELFE (Foret Noire / Lorien)
mat('egd', (196, 150, 62), 4, 5, (1.05, 1, .8))
mat('egl', (232, 206, 118), 4, 5, (1.05, 1, .85))
mat('egr', (124, 136, 62), 5, 6, (.9, 1, .8))
mat('ecl', (74, 70, 42), 4, 5, (1, 1, .9))
mat('ere', (116, 52, 42), 4, 5, (1.05, .9, .85))
mat('ele', (106, 78, 46), 4, 5, (1, .9, .8))
EG, EGD, EGL = (238, 208, 120), (96, 64, 24), (255, 240, 190)


def d_e_gold(rows=(), leaf=True, tr=.18):
    def f(face, F, rng):
        scratches(F, rng, 4, EGL, .3)
        if face not in ('top', 'bottom'):
            for y in rows: ridge(F, y, EGD, EGL) if y + 1 < F.h else None
            if leaf and F.w >= 4 and F.h >= 4:                                              # nervures de feuille
                cx = F.w // 2
                for k in range(F.h - 1): F.px(cx, k, EGL, .5)
                for k in range(1, F.h - 2, 2):
                    F.px(cx - 1, k + 1, EGD, .55); F.px(cx + 1, k + 1, EGD, .55)
            if F.h > 2: F.hline(F.h - 1, 0, F.w, EGD, .6)
        rivets(F, EGL)
    return f


def d_e_cloth(face, F, rng):
    for y in range(0, F.h, 2): F.hline(y, 0, F.w, (20, 18, 10), .12)
    if F.h > 4: F.hline(F.h - 1, 0, F.w, (150, 120, 60), .6)
    if face in ('front', 'back'): jagged_bottom(F, rng, 1)


def d_e_body(face, F, rng):
    scratches(F, rng, 6, EGL, .3)
    if face == 'front':
        F.vline(3, 0, 9, EGD, .8); F.vline(4, 0, 9, EGL, .5)
        for k in range(0, 9, 2):                                                          # nervures en palme de chaque cote
            for d in (1, 2): F.px(3 - d, k + d, EGD, .7); F.px(4 + d, k + d, EGD, .7)
        F.rect(0, 10, 8, 2, (104, 70, 40))
    elif face == 'back':
        F.vline(3, 0, 10, EGD, .7); F.vline(4, 0, 10, EGL, .45)
        F.rect(0, 10, 8, 2, (104, 70, 40))
    elif face in ('left', 'right'):
        for y in (3, 6): ridge(F, y, EGD, EGL)
        F.rect(0, 10, F.w, 2, (104, 70, 40))


def d_e_sash(face, F, rng):
    for x in range(0, F.w, 2): F.px(x, 0, (40, 22, 14), .5)
    F.hline(F.h - 1, 0, F.w, (40, 22, 14), .5)


def elf():
    G = d_e_gold
    head = Part('head')
    head.box((-4, -8, -4), (8, 4, 8), 'egd', .5, 'e_ring', G(rows=(2,), leaf=False))
    head.box((-3, -9.5, -3), (6, 2, 6), 'egd', .4, 'e_dome', G(leaf=False))
    c1 = Part('crest1', (0, -9.6, -1.5), (R(-14), 0, 0)); head.children.append(c1)
    c1.box((-0.5, -3, 0), (1, 3, 3), 'egl', 0, 'e_c1', G())
    c2 = Part('crest2', (0, -2.8, 1.2), (R(-30), 0, 0)); c1.children.append(c2)
    c2.box((-0.5, -2, 0), (1, 2, 3), 'egl', 0, 'e_c2', G())
    hr = Part('horn_r', (-1.8, -9.4, -3), (R(-35), 0, R(22))); head.children.append(hr)
    hr.box((-0.5, -2, -0.5), (1, 2, 2), 'egl', 0, 'e_horn', G(leaf=False))
    head.children.append(mirrored(hr, 'horn_l'))
    head.box((-4.3, -4, -3), (1, 4, 4), 'egd', .3, 'e_cheek', G())
    head.cubes.append(Cube((3.3, -4, -3), (1, 4, 4), 'egd', .3, 'e_cheek', G(), True))
    head.box((-0.5, -4, -4.5), (1, 3, 1), 'egd', .2, 'e_nose', G(leaf=False))
    neck = Part('neck', (0, -2.6, 3.4), (R(12), 0, 0)); head.children.append(neck)
    neck.box((-4, 0, 0), (8, 3, 1), 'ecl', 0, 'e_neck', d_e_cloth)

    body = Part('body')
    body.box((-4, 0, -2), (8, 12, 4), 'egd', .6, 'e_body', d_e_body)
    body.box((-0.5, 0.8, -3.1), (1, 9, 1), 'egl', .1, 'e_spine', G(leaf=False))
    ra = Part('right_arm')
    for i, (py, rz, w) in enumerate(((-2.9, -6, 5), (-1.9, -13, 5), (-0.9, -20, 5))):
        pa = Part(f'lam{i}', (0, py, 0), (0, 0, R(rz))); ra.children.append(pa)
        pa.box((-4.5, -0.5, -2.5), (w, 1, 5), 'egl' if i == 0 else 'egd', .1, 'e_lam', G(leaf=False))
    ra.box((-3, 4, -2), (4, 4, 4), 'egd', .55, 'e_brac', G(rows=(1,)))
    la = mirrored(ra, 'left_arm')
    chest = [body, ra, la]

    body = Part('body')
    body.box((-4, 11, -2), (8, 2, 4), 'ele', .85, 'e_belt2', d_e_sash)
    body.box((-4, 12.5, -2), (8, 3, 4), 'ere', .85, 'e_skirt', d_e_cloth)
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 4, 4), 'egd', .55, 'e_thigh', G(leaf=False))
    rl.box((-2, 0.5, -2.9), (4, 11, 1), 'ecl', .1, 'e_drape', d_e_cloth)
    rl.box((-2, 0.5, 1.9), (4, 11, 1), 'ecl', .1, 'e_drape', d_e_cloth)
    ll = mirrored(rl, 'left_leg')
    legs = [body, rl, ll]

    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 6, 4), 'ele', .5, 'e_boot', d_o_boot)
    rl.box((-2, 2, -2.9), (4, 7, 1), 'egd', .1, 'e_greave', G(rows=(2, 5)))
    rl.box((-2.5, 4.6, -2.5), (5, 1, 5), 'egl', .1, 'e_cuff', G(leaf=False))
    rl.box((-1, 10.4, -4.2), (2, 1, 2), 'egd', 0, 'e_toe', G(leaf=False))
    ll = mirrored(rl, 'left_leg')
    return {'head': [head], 'chest': chest, 'legs': legs, 'boots': [rl, ll]}


SETS['elven'] = elf


# ================================================================== ROHAN (Theoden : cuir bordeaux, plaques dorees, jupe d'ecailles)
mat('rlb', (96, 48, 34), 5, 6, (1.1, .9, .85))      # cuir bordeaux grave
mat('rgo', (214, 168, 76), 5, 6, (1.05, 1, .8))     # or
mat('rld', (58, 36, 28), 4, 5, (1, .92, .85))       # cuir sombre
mat('rrd', (150, 34, 30), 4, 5, (1.05, .9, .85))    # tunique rouge
mat('rsc', (86, 84, 92), 5, 6, (1, 1, 1.05))        # ecailles d'acier
RG, RGD, RGL = (236, 196, 100), (92, 58, 24), (255, 234, 170)


def d_r_gold(rows=(), horse=False):
    def f(face, F, rng):
        scratches(F, rng, 3, RGL, .3)
        if face not in ('top', 'bottom'):
            for y in rows: ridge(F, y, RGD, RGL) if y + 1 < F.h else None
            if F.h > 2: F.hline(F.h - 1, 0, F.w, RGD, .6)
            if horse and F.w >= 5 and F.h >= 5:                                             # motif de cheval grave
                F.art(F.w // 2 - 2, 1, ['.##.', '###.', '.###', '.#.#'][: F.h - 1], {'#': RGD})
        rivets(F, RGL, 0)
    return f


def d_r_helm(face, F, rng):
    scratches(F, rng, 4, RGL, .3)
    if face in ('front', 'back', 'left', 'right'):
        F.hline(0, 0, F.w, RGL, .8); F.hline(1, 0, F.w, RGD, .5)
        F.hline(F.h - 1, 0, F.w, RGD, .7)
        for x in range(1, F.w, 3): F.px(x, 2, RGD, .7)                                      # frise
    if face == 'front':
        F.art(1, 3, ['.#..#.', '######'][: max(0, F.h - 3)], {'#': RGD}) if F.h >= 5 else None
    rivets(F, RGL, 0)


def d_r_body(face, F, rng):
    scratches(F, rng, 8, RGL, .25)
    if face == 'front':
        F.rect(0, 0, 8, 12, (110, 54, 38), .0)
        for y in (3, 6, 9): ridge(F, y, (50, 24, 18), (170, 100, 70))                       # lames horizontales
        F.rect(2, 0, 4, 3, RG, .8); F.art(3, 0, ['.##.', '####'], {'#': RGD})              # plastron doré haut
        F.vline(3, 3, 9, RGD, .6); F.vline(4, 3, 9, RGD, .6)
        F.rect(0, 10, 8, 2, (56, 32, 22))
        for x, y in ((1, 1), (6, 1), (1, 4), (6, 4), (1, 7), (6, 7)): F.px(x, y, RG, .85)
    elif face == 'back':
        for y in (3, 6, 9): ridge(F, y, (50, 24, 18), (170, 100, 70))
        F.vline(3, 0, 10, RGD, .6); F.vline(4, 0, 10, RGD, .6)
        F.rect(0, 10, 8, 2, (56, 32, 22))
    elif face in ('left', 'right'):
        for y in (3, 6, 9): ridge(F, y, (50, 24, 18), (160, 94, 66))
        F.rect(0, 10, F.w, 2, (56, 32, 22))
        F.hline(0, 0, F.w, RG, .7)


def d_r_scale(face, F, rng):
    for y in range(0, F.h, 2):
        for x in range(F.w):
            if (x + y // 2) % 2 == 0: F.px(x, y, (40, 40, 48), .55)
            else: F.px(x, y + 1 if y + 1 < F.h else y, (176, 176, 186), .3)
    for y in range(1, F.h, 2): F.hline(y, 0, F.w, (30, 30, 38), .35)
    if face in ('front', 'back'): jagged_bottom(F, rng, 1)
    scratches(F, rng, 3, (210, 210, 220), .25)


def d_r_belt(face, F, rng):
    for x in range(0, F.w, 2): F.px(x, 0, (30, 18, 12), .5)
    F.hline(F.h - 1, 0, F.w, (30, 18, 12), .6)
    if face == 'front' and F.w >= 4: F.rect(F.w // 2 - 1, 0, 2, F.h, RG, .85)


def d_r_boot(face, F, rng):
    scratches(F, rng, 4, (170, 140, 110), .15)
    for y in range(3, F.h, 3): F.hline(y, 0, F.w, (30, 20, 14), .4)
    if face == 'bottom': F.rect(0, 0, F.w, F.h, (28, 22, 20))
    if face in ('front', 'back', 'left', 'right'): F.hline(F.h - 1, 0, F.w, (20, 14, 12), .8)


def rohan():
    G = d_r_gold
    head = Part('head')
    head.box((-4, -8, -4), (8, 4, 8), 'rgo', .55, 'r_ring', d_r_helm)
    head.box((-3, -9.5, -3), (6, 2, 6), 'rgo', .45, 'r_dome', G())
    cr = Part('crest', (0, -10, -1.5), (R(10), 0, 0)); head.children.append(cr)                # cimier : cheval qui part vers l'arriere
    cr.box((-0.5, -2, 0), (1, 2, 5), 'rgo', 0, 'r_crest', G())
    head.box((-4.3, -4, -3), (1, 4, 4), 'rgo', .3, 'r_cheek', G(rows=(1,)))
    head.cubes.append(Cube((3.3, -4, -3), (1, 4, 4), 'rgo', .3, 'r_cheek', G(rows=(1,)), True))
    head.box((-0.5, -4, -4.6), (1, 4, 1), 'rgo', .2, 'r_nose', G())
    neck = Part('neck', (0, -2.6, 3.4), (R(12), 0, 0)); head.children.append(neck)
    neck.box((-4, 0, 0), (8, 3, 1), 'rld', 0, 'r_neck', d_o_neck)

    body = Part('body')
    body.box((-4, 0, -2), (8, 12, 4), 'rlb', .6, 'r_body', d_r_body)
    body.box((-1.5, 4, -3.1), (3, 2, 1), 'rgo', .1, 'r_emblem', G(horse=False))
    ra = Part('right_arm')
    for i, (py, rz, x0) in enumerate(((-2.9, -10, -5.0), (-1.0, -22, -4.5))):
        pa = Part(f'pauld{i}', (0, py, 0), (0, 0, R(rz))); ra.children.append(pa)
        pa.box((x0, -1, -2.5), (5, 1, 5), 'rlb', .15, 'r_paul', G(horse=True))
    ra.box((-3, 4.5, -2), (4, 3, 4), 'rgo', .55, 'r_brac', G(rows=(1,)))
    la = mirrored(ra, 'left_arm')
    chest = [body, ra, la]

    body = Part('body')
    body.box((-4, 11, -2), (8, 2, 4), 'rld', .85, 'r_belt2', d_r_belt)
    body.box((-4, 12.5, -2), (8, 4, 4), 'rsc', .8, 'r_skirt', d_r_scale)
    body.box((-3.6, 12, -2.9), (3, 5, 1), 'rlb', .85, 'r_tass', G(horse=False))
    body.cubes.append(Cube((0.6, 12, -2.9), (3, 5, 1), 'rlb', .85, 'r_tass', G(horse=False), True))
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 5, 4), 'rlb', .6, 'r_thigh', G(rows=(2,)))
    rl.box((-2.3, 4.4, -3.2), (4, 3, 2), 'rgo', 0, 'r_knee', G())
    ll = mirrored(rl, 'left_leg')
    legs = [body, rl, ll]

    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 6, 4), 'rld', .55, 'r_boot', d_r_boot)
    rl.box((-2, 2.5, -2.9), (4, 6, 1), 'rgo', .1, 'r_greave', G(rows=(2,), horse=False))
    rl.box((-2.5, 5.2, -2.5), (5, 1, 5), 'rlb', .1, 'r_cuff', G())
    rl.box((-2, 10.2, -3.6), (4, 2, 2), 'rld', 0, 'r_toe', G())
    ll = mirrored(rl, 'left_leg')
    return {'head': [head], 'chest': chest, 'legs': legs, 'boots': [rl, ll]}


SETS['rohan'] = rohan


# ------------------------------------------------------------------ sortie / apercu
PIECES = ('head', 'chest', 'boots', 'legs')
TILE_OF = {'head': (0, 0, 1), 'chest': (32, 0, 1), 'boots': (0, 32, 1), 'legs': (0, 0, 2)}


def build_set(name):
    pieces = SETS[name]()
    out = {}
    for pc in PIECES:
        parts = pieces[pc]
        tex = paint(parts)
        out[pc] = (parts, tex)
    return out


def sheet_textures(built):
    sheets = {1: np.zeros((64, 64, 4), np.uint8), 2: np.zeros((64, 64, 4), np.uint8)}
    for pc, (parts, tex) in built.items():
        tx, ty, layer = TILE_OF[pc]
        sheets[layer][ty:ty + 32, tx:tx + 32] = tex
    return sheets


def piece_geo(built, pc):
    tx, ty, _ = TILE_OF[pc]
    return to_json(built[pc][0], tx, ty)


def preview(names):
    tiles = []
    for name in names:
        built = build_set(name)
        sheets = sheet_textures(built)
        models = [(mannequin(), flat_tex())]
        for pc in PIECES:
            tex = sheets[TILE_OF[pc][2]]
            models.append((piece_geo(built, pc), tex))
        views = [raster(models, y, 8, 9, (360, 640)) for y in (20, 90, 160, 235)]
        W = sum(v.width for v in views)
        out = Image.new('RGB', (W, views[0].height))
        x = 0
        for v in views: out.paste(v, (x, 0)); x += v.width
        out.save(f'/tmp/armors_{name}.png')
        # gros plans : casque + plastron
        closes = [raster(models, y, 8, 22, (520, 520), crop=-4) for y in (25, 90)]
        c = Image.new('RGB', (1040, 520)); c.paste(closes[0], (0, 0)); c.paste(closes[1], (520, 0)); c.save(f'/tmp/armors_{name}_tete.png')
        t = Image.new('RGBA', (256, 128))
        for k, layer in ((1, 0), (2, 1)):
            s = Image.fromarray(sheets[k]).resize((128, 128), Image.NEAREST)
            bg = Image.new('RGBA', (128, 128), (60, 60, 70, 255)); bg.alpha_composite(s); t.paste(bg, (layer * 128, 0))
        t.save(f'/tmp/armors_{name}_tex.png')
        print(name, 'ok')


def gen(names):
    for name in names:
        built = build_set(name)
        sheets = sheet_textures(built)
        os.makedirs(f'{A}/textures/models/armor', exist_ok=True)
        os.makedirs(f'{A}/geo', exist_ok=True)
        for layer in (1, 2):
            Image.fromarray(sheets[layer], 'RGBA').save(f'{A}/textures/models/armor/{name}_layer_{layer}.png')
        for pc in PIECES:
            json.dump(piece_geo(built, pc), open(f'{A}/geo/armor_{name}_{pc}.json', 'w'), separators=(',', ':'))
        print('ecrit', name)


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'preview'
    names = sys.argv[2:] or list(SETS)
    (preview if cmd == 'preview' else gen)(names)
