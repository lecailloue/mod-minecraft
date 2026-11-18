"""Armures 3D du film : orc (Uruk-hai d'Isengard), nain (Erebor), elfe (Foret Noire / Lorien).
python3 tools/armors3d.py preview   -> /tmp/armors_<set>.png (face / profil / dos sur mannequin) + /tmp/armors_tiles.png
python3 tools/armors3d.py gen       -> ecrit geo/armor_<set>_<piece>.json et textures/models/armor/<set>_layer_{1,2}.png"""
import math, os, sys, json, zlib
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


# ------------------------------------------------------------------ drap (tissu sous l'armure)
mat('cl_red', (112, 26, 30), 4, 5, (1.05, .9, .9))
mat('cl_dgrey', (56, 57, 66), 4, 5, (1, 1, 1.05))
mat('cl_lgrey', (176, 178, 186), 3, 4, (1, 1, 1.02))
mat('cl_green', (64, 106, 74), 4, 5, (.95, 1, .95))


def cloth(dark, light, trim):
    """tissu : fines rayures de trame + ourlet galonne en bas"""
    def f(face, F, rng):
        for y in range(F.h):
            F.hline(y, 0, F.w, dark if y % 3 == 0 else light, .22)
        for x in range(0, F.w, 4): F.vline(x, 0, F.h, dark, .25)
        if face not in ('top', 'bottom') and F.h >= 3:
            F.hline(F.h - 1, 0, F.w, trim, .9); F.hline(F.h - 2, 0, F.w, dark, .5)
            if face in ('front', 'back') and F.w >= 5: F.vline(0, 0, F.h, trim, .35); F.vline(F.w - 1, 0, F.h, trim, .35)
    return f


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
    body.box((-4, 12.5, -2), (8, 3, 4), 'cl_green', .85, 'e_skirt', d_e_cloth)
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 4, 4), 'egd', .55, 'e_thigh', G(leaf=False))
    rl.box((-2, 0.5, -2.9), (4, 11, 1), 'cl_green', .1, 'e_drape', d_e_cloth)
    rl.box((-2, 0.5, 1.9), (4, 11, 1), 'cl_green', .1, 'e_drape', d_e_cloth)
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


def d_r_band(face, F, rng):
    if face in ('top', 'bottom'):
        F.al[:, :] = 0                                  # couronne ouverte : on garde les cheveux
        return
    F.hline(0, 0, F.w, RGL, .95); F.hline(F.h - 1, 0, F.w, RGD, .8)
    for x in range(1, F.w, 2): F.px(x, 1, RGD, .55)       # filigrane
    if face == 'front' and F.w >= 8:
        F.px(3, 1, (200, 40, 40), .95); F.px(4, 1, (200, 40, 40), .95)
    scratches(F, rng, 2, RGL, .3)


def d_r_spike(face, F, rng):
    F.rect(0, 0, F.w, F.h, RG, .9)
    scratches(F, rng, 1, RGL, .4)
    if face not in ('top', 'bottom') and F.h > 1: F.hline(F.h - 1, 0, F.w, RGD, .6)


def d_r_gem(face, F, rng):
    F.rect(0, 0, F.w, F.h, (176, 30, 36), .95)
    if face in ('front', 'back') and F.w >= 3: F.px(0, 0, (255, 150, 150), .9)
    F.hline(F.h - 1, 0, F.w, RGD, .6)


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
    head = Part('head')                                                                  # couronne du roi de Rohan (plus de casque)
    head.box((-4, -8, -4), (8, 2, 8), 'rgo', .5, 'r_crown', d_r_band)
    for i, (px, pz) in enumerate(((-2.5, -4.5), (2.5, -4.5), (-4.5, -2.5), (4.5, -2.5), (-4.5, 0.5), (4.5, 0.5), (-4.5, 3.0), (4.5, 3.0), (-2.5, 4.5), (0.0, 4.5), (2.5, 4.5))):
        head.box((px - 0.5, -10.2, pz - 0.5), (1, 2, 1), 'rgo', 0.1, 'r_spike', d_r_spike)
    head.box((-0.5, -11.4, -4.7), (1, 3, 1), 'rgo', 0.1, 'r_spike_c', d_r_spike)        # grande pointe centrale
    head.box((-1.5, -9.4, -4.6), (3, 2, 1), 'rgo', 0.1, 'r_gem', d_r_gem)               # joyau au front

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


# ================================================================== GARDE DE GONDOR (acier clair, ailes, arbre blanc)
mat('gst', (172, 178, 190), 5, 6, (1, 1, 1.06))
mat('gdk', (74, 78, 90), 4, 5, (1, 1, 1.08))
mat('gwh', (226, 224, 214), 3, 4, (1, 1, .97))
mat('gmail', (104, 108, 120), 6, 7, (1, 1, 1.05))
mat('gbk', (34, 34, 42), 3, 4, (1, 1, 1.1))
mat('gfe', (240, 238, 232), 4, 5, (1, 1, 1))
mat('gle', (50, 40, 36), 4, 5, (1, .92, .85))
GG, GGD, GS = (214, 178, 84), (86, 66, 26), (236, 238, 246)


def d_g_steel(rows=(), gold=False):
    def f(face, F, rng):
        scratches(F, rng, 4, (255, 255, 255), .3)
        if face not in ('top', 'bottom'):
            for y in rows: ridge(F, y, (70, 74, 88), GS) if y + 1 < F.h else None
            if F.h > 2: F.hline(F.h - 1, 0, F.w, GG if gold else (70, 74, 88), .8)
            if gold: F.hline(0, 0, F.w, GG, .8)
        rivets(F, (250, 250, 255), 0)
    return f


def d_g_helm(face, F, rng):
    scratches(F, rng, 4, (255, 255, 255), .3)
    if face in ('front', 'back', 'left', 'right'):
        F.hline(0, 0, F.w, GG, .85); F.hline(F.h - 1, 0, F.w, (70, 74, 88), .7)
        if face == 'front': F.art(1, 1, ['#...#.#...#'][:1], {'#': GGD}) if F.w >= 8 and F.h >= 3 else None
    rivets(F, (250, 250, 255), 0)


def d_g_spine(face, F, rng):
    F.rect(0, 0, F.w, F.h, GG, .9)
    scratches(F, rng, 2, (255, 240, 190), .4)
    if face not in ('top', 'bottom'): F.hline(F.h - 1, 0, F.w, GGD, .5)


def d_g_wing(face, F, rng):
    """plume arrondie : fond blanc, nervure centrale, barbes en chevrons, bout clair"""
    for y in range(F.h):
        for x in range(F.w):
            F.px(x, y, (250, 250, 248) if (x + y) % 3 else (222, 224, 230), .65)
    if face in ('front', 'back') and F.w >= 5:
        F.hline(F.h // 2, 0, F.w, (160, 164, 176), .55)                               # rachis
        for x in range(1, F.w - 1, 2):
            F.px(x, max(0, F.h // 2 - 1), (255, 255, 255), .7); F.px(x + 1 if x + 1 < F.w else x, min(F.h - 1, F.h // 2 + 1), (200, 204, 214), .6)
        F.px(0, 0, (0, 0, 0), 0); F.al[0, 0] = 0; F.al[F.h - 1, 0] = 0                 # bout arrondi (cote libre)
    if face == 'top': F.rect(0, 0, F.w, F.h, GG, .5)


def d_g_body(face, F, rng):
    scratches(F, rng, 7, (255, 255, 255), .3)
    if face == 'front':
        for x, y in ((3, 1), (4, 1)): F.px(x, y, GG, .9)
        # l'Arbre Blanc grave : tronc, branches, feuilles
        F.vline(3, 4, 9, GG, .9); F.vline(4, 4, 9, GG, .9)
        for k, (x, y) in enumerate(((2, 3), (5, 3), (1, 5), (6, 5), (2, 6), (5, 6))): F.px(x, y, GG, .85)
        F.px(3, 3, GG, .9); F.px(4, 3, GG, .9); F.px(3, 2, GG, .8); F.px(4, 2, GG, .8)
        for x in (1, 6): F.px(x, 4, GGD, .6)
        F.rect(0, 10, 8, 2, (60, 62, 74)); F.hline(9, 0, 8, GS, .45)
    elif face == 'back':
        F.vline(3, 0, 10, (70, 74, 88), .7); F.vline(4, 0, 10, GS, .35)
        for y in (3, 6): ridge(F, y, (70, 74, 88), GS)
        F.rect(0, 10, 8, 2, (60, 62, 74))
    elif face in ('left', 'right'):
        for y in (3, 6): ridge(F, y, (70, 74, 88), GS)
        F.rect(0, 10, F.w, 2, (60, 62, 74))
        F.hline(0, 0, F.w, GG, .7)


def d_g_mail(face, F, rng):
    for y in range(F.h):
        for x in range(F.w):
            if (x + y) % 2 == 0: F.px(x, y, (44, 46, 56), .55)
            elif y % 2 == 0: F.px(x, y, (190, 194, 206), .22)
    if face in ('front', 'back'): jagged_bottom(F, rng, 1)


def d_g_tabard(face, F, rng):
    for y in range(0, F.h, 3): F.hline(y, 0, F.w, (190, 188, 178), .25)
    if face == 'front':
        F.vline(0, 0, F.h, GG, .8); F.vline(F.w - 1, 0, F.h, GG, .8)
        F.hline(F.h - 1, 0, F.w, GG, .9); F.hline(F.h - 2, 0, F.w, GGD, .5)
        F.vline(F.w // 2, 2, F.h - 4, (150, 150, 144), .4)
    else:
        F.hline(F.h - 1, 0, F.w, GGD, .5)


def d_g_boot(face, F, rng):
    scratches(F, rng, 4, (150, 130, 110), .15)
    for y in range(3, F.h, 3): F.hline(y, 0, F.w, (20, 16, 14), .4)
    if face == 'bottom': F.rect(0, 0, F.w, F.h, (26, 22, 22))
    if face in ('front', 'back', 'left', 'right'): F.hline(F.h - 1, 0, F.w, (16, 14, 14), .8)


def d_g_dome(face, F, rng):
    scratches(F, rng, 3, (255, 255, 255), .3)
    if face in ('front', 'back', 'left', 'right'):
        for x in range(1, F.w, 2): F.vline(x, 0, F.h, (206, 210, 222), .55)             # cannelures rayonnantes
        F.hline(F.h - 1, 0, F.w, (60, 62, 74), .6)
    if face == 'top':
        for k in range(F.w): F.px(k, F.h // 2, (206, 210, 222), .4)


def d_g_ring2(face, F, rng):
    scratches(F, rng, 4, (255, 255, 255), .25)
    if face in ('front', 'back', 'left', 'right'):
        F.hline(0, 0, F.w, GG, .95); F.hline(F.h - 1, 0, F.w, (60, 62, 74), .7)
        if face == 'front': F.px(3, 1, GG, .95); F.px(4, 1, GG, .95)                  # joyau dore au front
        for x in range(0, F.w, 3): F.px(x, 1, (206, 210, 222), .5)
    rivets(F, (250, 250, 255), 0)


def gondor(rusted=False):
    G = d_g_steel
    head = Part('head')
    head.box((-4, -8, -4), (8, 3, 8), 'gdk', .55, 'g_ring', d_g_ring2)
    head.box((-3, -10, -3), (6, 2, 6), 'gdk', .45, 'g_dome', d_g_dome)                 # calotte
    head.box((-2, -12, -2), (4, 2, 4), 'gdk', .35, 'g_dome2', d_g_dome)               # sommet pointu : dome en gradins
    head.box((-4.3, -5, -3), (1, 5, 4), 'gdk', .3, 'g_cheek', G(rows=(2,), gold=True))
    head.cubes.append(Cube((3.3, -5, -3), (1, 5, 4), 'gdk', .3, 'g_cheek', G(rows=(2,), gold=True), True))
    head.box((-0.5, -5, -4.6), (1, 5, 1), 'gst', .2, 'g_nose', G(gold=True))
    # aile (schema de l'utilisateur) : nervure doree courbe (croissant) fixee sur le casque, 3 plumes arrondies vers le haut + 3 vers le bas
    J = (-4.7, -8.4, 0.0)                                                            # point de jonction sur le flanc du casque
    wing = Part('wing', J); head.children.append(wing)
    prev = wing
    for k, (rz, fz) in enumerate(((-5, 36), (-9, 40), (-12, 44))):                    # moitie haute : monte en s'ecartant du casque
        seg = Part(f'sp_u{k}', (0, 0 if k == 0 else -2, 0), (0, 0, R(rz))); prev.children.append(seg)
        seg.box((-0.5, -2, -1), (1, 2, 2), 'gst', 0.1, 'g_spine', d_g_spine)
        fe = Part(f'fe_u{k}', (0, -1.0, 0.0), (0, 0, R(fz))); seg.children.append(fe)
        fe.box((-4, -1, -0.5), (4, 2, 1), 'gfe', 0, 'g_plume', d_g_wing)
        prev = seg
    prev = wing
    for k, (rz, fz) in enumerate(((5, -34), (9, -38), (12, -42))):                    # moitie basse : descend le long de la joue en s'ecartant
        seg = Part(f'sp_d{k}', (0, 0 if k == 0 else 2, 0), (0, 0, R(rz))); prev.children.append(seg)
        seg.box((-0.5, 0, -1), (1, 2, 2), 'gst', 0.1, 'g_spine', d_g_spine)
        fe = Part(f'fe_d{k}', (0, 1.0, 0.0), (0, 0, R(fz))); seg.children.append(fe)
        fe.box((-4, -1, -0.5), (4, 2, 1), 'gfe', 0, 'g_plume', d_g_wing)
        prev = seg
    wl = mirrored(wing, 'wing_l')
    if rusted:                                                   # variante rouillee : l'aile gauche a perdu une partie de ses plumes
        def drop(p, names):
            p.children = [c for c in p.children if c.name not in names]
            for c in p.children: drop(c, names)
        drop(wl, {'fe_u1', 'fe_u2', 'fe_d0', 'fe_d2'})
    head.children.append(wl)
    neck = Part('neck', (0, -2.6, 3.4), (R(12), 0, 0)); head.children.append(neck)
    neck.box((-4, 0, 0), (8, 2, 1), 'gmail', 0, 'g_neck', d_g_mail)

    body = Part('body')
    body.box((-4, 0, -2), (8, 12, 4), 'gst', .6, 'g_body', d_g_body)
    ra = Part('right_arm')
    for i, (py, rz) in enumerate(((-2.9, -8), (-1.4, -18))):
        pa = Part(f'pauld{i}', (0, py, 0), (0, 0, R(rz))); ra.children.append(pa)
        pa.box((-5, -1, -2.5), (6, 1, 5), 'gst', .15, 'g_paul', G(rows=(), gold=True))
    ra.box((-3, 4, -2), (4, 4, 4), 'gmail', .5, 'g_brac', d_g_mail)
    la = mirrored(ra, 'left_arm')
    chest = [body, ra, la]

    body = Part('body')
    body.box((-4, 11, -2), (8, 2, 4), 'gdk', .85, 'g_belt', G())
    body.box((-4, 12.5, -2), (8, 3, 4), 'gmail', .8, 'g_skirt', d_g_mail)
    body.box((-3, 11.5, -3.0), (6, 11, 1), 'gwh', .35, 'g_tab', d_g_tabard)
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 5, 4), 'gdk', .55, 'g_thigh', G(rows=(2,)))
    rl.box((-2.3, 4.4, -3.2), (4, 3, 2), 'gst', 0, 'g_knee', G(gold=True))
    ll = mirrored(rl, 'left_leg')
    legs = [body, rl, ll]

    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 6, 4), 'gle', .55, 'g_boot', d_g_boot)
    rl.box((-2, 2.5, -2.9), (4, 6, 1), 'gst', .1, 'g_greave', G(rows=(2,), gold=True))
    rl.box((-2.5, 5.2, -2.5), (5, 1, 5), 'gst', .1, 'g_cuff', G())
    rl.box((-2, 10.2, -3.6), (4, 2, 2), 'gdk', 0, 'g_toe', G())
    ll = mirrored(rl, 'left_leg')
    return {'head': [head], 'chest': chest, 'legs': legs, 'boots': [rl, ll]}


SETS['gondor'] = gondor


# ================================================================== ROI ELFE (diademe de laurier, tunique doree, cape verte a dorures)
mat('kgo', (206, 168, 84), 4, 5, (1.05, 1, .8))
mat('kgl', (240, 214, 130), 4, 5, (1.05, 1, .85))
mat('kcp', (82, 108, 90), 4, 5, (.92, 1, .95))          # cape vert-gris
mat('kcl', (176, 140, 70), 4, 5, (1.05, 1, .8))         # tissu dore matelasse
mat('kle', (96, 72, 44), 4, 5, (1, .9, .8))
mat('kgr', (64, 104, 76), 4, 5, (.95, 1, .95))          # drap vert sous l'armure
KG, KGD, KGL = (246, 214, 120), (104, 76, 28), (255, 244, 196)


def d_k_gold(rows=(), leaf=False):
    def f(face, F, rng):
        scratches(F, rng, 3, KGL, .3)
        if face not in ('top', 'bottom'):
            for y in rows: ridge(F, y, KGD, KGL) if y + 1 < F.h else None
            if F.h > 2: F.hline(F.h - 1, 0, F.w, KGD, .6)
            if leaf and F.h >= 3: F.vline(F.w // 2, 0, F.h, KGD, .6)
        rivets(F, KGL, 0)
    return f


def d_k_band(face, F, rng):
    if face in ('top', 'bottom'):
        F.al[:, :] = 0                                    # diademe ouvert : on garde les cheveux
        return
    F.hline(0, 0, F.w, KGL, .9); F.hline(F.h - 1, 0, F.w, KGD, .7)
    for x in range(1, F.w, 3): F.px(x, 0, KGD, .7)        # rinceau
    if face == 'front' and F.w >= 8:
        F.px(3, 1, KGL, .9); F.px(4, 1, KGL, .9)
    scratches(F, rng, 2, KGL, .3)


def d_k_cloth(face, F, rng):
    for y in range(0, F.h, 2):
        for x in range(F.w):
            if (x + y) % 4 == 0: F.px(x, y, (120, 90, 40), .3)
    if F.h > 4: F.hline(F.h - 1, 0, F.w, KGD, .5)


def d_k_body(face, F, rng):
    for y in range(F.h):
        for x in range(F.w):
            if (x + y) % 4 == 0: F.px(x, y, (120, 90, 40), .3)
    scratches(F, rng, 4, KGL, .3)
    if face == 'front':
        for k in range(8):                                                                   # sangles croisees a runes
            for t in (0, 1):
                F.px(min(7, k), min(11, k + 1 + t), KG, .9); F.px(max(0, 7 - k), min(11, k + 1 + t), KG, .9)
        for k in range(1, 8): F.px(k, 4 if k % 2 else 5, KGD, .5)
        F.rect(0, 8, 8, 4, (150, 118, 54), .95)                                              # large ceinture drapee
        for y in (8, 10): F.hline(y, 0, 8, KGL, .6)
        F.px(2, 11, KGL, .9); F.px(5, 11, KGL, .9)
    elif face == 'back':
        F.rect(0, 8, 8, 4, (150, 118, 54), .95)
        for y in (8, 10): F.hline(y, 0, 8, KGL, .6)
    elif face in ('left', 'right'):
        F.rect(0, 8, F.w, 4, (150, 118, 54), .95)
        F.hline(8, 0, F.w, KGL, .6)


def d_k_cape(face, F, rng):
    for y in range(F.h):
        F.hline(y, 0, F.w, (70, 94, 78) if y % 3 == 0 else (96, 124, 104), .35)
    if face == 'back':
        F.hline(F.h - 1, 0, F.w, KG, .9); F.hline(F.h - 2, 0, F.w, KGD, .5)
        F.vline(0, 0, F.h, KG, .9); F.vline(F.w - 1, 0, F.h, KG, .9)
        for y in range(2, F.h - 2, 3):                                                       # dorures : feuilles
            for x in (2, F.w - 3): F.px(x, y, KG, .85); F.px(x, y + 1, KGD, .5)
        for y in range(1, F.h - 2, 4):
            F.px(F.w // 2, y, KG, .85); F.px(F.w // 2 - 1, y + 1, KG, .6); F.px(F.w // 2 + 1, y + 1, KG, .6)
    elif face in ('left', 'right'):
        F.vline(0, 0, F.h, KG, .8)
    elif face == 'bottom':
        F.rect(0, 0, F.w, F.h, KGD, .6)


def d_k_drap(face, F, rng):
    for y in range(F.h):
        F.hline(y, 0, F.w, (46, 78, 56) if y % 3 == 0 else (86, 128, 96), .3)
    for x in range(0, F.w, 4): F.vline(x, 0, F.h, (40, 70, 50), .35)
    if face in ('front', 'back'):
        F.hline(F.h - 1, 0, F.w, KG, .9); F.hline(F.h - 2, 0, F.w, KGD, .5)
        if F.w >= 4 and F.h >= 6:
            for y in range(1, F.h - 3, 3): F.px(F.w // 2, y, KG, .85); F.px(F.w // 2 - 1, y + 1, KGD, .6); F.px(F.w // 2 + 1, y + 1, KGD, .6)
        F.vline(0, 0, F.h, KG, .6); F.vline(F.w - 1, 0, F.h, KG, .6)


def d_k_sleeve(face, F, rng):
    for y in range(F.h):
        F.hline(y, 0, F.w, (46, 78, 56) if y % 3 == 0 else (86, 128, 96), .3)
    if face not in ('top', 'bottom'):
        F.hline(F.h - 1, 0, F.w, KG, .85); F.hline(F.h - 2, 0, F.w, KGD, .5)
        if F.h > 6: F.hline(F.h // 2, 0, F.w, KGD, .35)


def elfking():
    G = d_k_gold
    head = Part('head')
    head.box((-4, -8, -4), (8, 2, 8), 'kgo', .5, 'k_band', d_k_band)
    for i, (rx, ry, rz, px, py, pz) in enumerate(((-14, 0, -10, -1.2, -8.3, -2.0), (-26, 0, -26, -2.6, -8.1, 0.0), (-38, 0, -42, -3.4, -7.8, 2.2))):
        lf = Part(f'leaf_r{i}', (px, py, pz), (R(rx), 0, R(rz))); head.children.append(lf)
        lf.box((-0.5, -4, -0.5), (1, 4, 2), 'kgl', 0, 'k_leaf', G(leaf=True))
        head.children.append(mirrored(lf, f'leaf_l{i}'))
    neck = Part('neck', (0, -2.6, 3.4), (R(12), 0, 0)); head.children.append(neck)
    neck.box((-4, 0, 0), (8, 2, 1), 'kgr', 0, 'k_neck', d_k_drap)

    body = Part('body')
    body.box((-4, 0, -2), (8, 12, 4), 'kcl', .6, 'k_body', d_k_body)
    cape = Part('cape', (0, -0.6, 2.4), (R(4), 0, 0)); body.children.append(cape)
    cape.box((-6, 0, 0), (12, 22, 1), 'kcp', 0.3, 'k_cape', d_k_cape)             # grande cape : des epaules aux chevilles
    ra = Part('right_arm')
    ra.box((-3, -2, -2), (4, 12, 4), 'kgr', .3, 'k_sleeve', d_k_sleeve)            # manche en drap vert
    for i, (py, rz) in enumerate(((-2.8, -8), (-1.4, -18))):
        pa = Part(f'lam{i}', (0, py, 0), (0, 0, R(rz))); ra.children.append(pa)
        pa.box((-4.8, -1, -2.2), (5, 1, 4), 'kgo', .12, 'k_lam', G(leaf=True))
    ra.box((-3, 4, -2), (4, 4, 4), 'kgo', .62, 'k_brac', G(rows=(1,)))
    la = mirrored(ra, 'left_arm')
    chest = [body, ra, la]

    body = Part('body')
    body.box((-4, 11, -2), (8, 2, 4), 'kle', .85, 'k_belt2', d_k_cloth)
    body.box((-4, 12.5, -2), (8, 4, 4), 'kgr', .8, 'k_skirt', d_k_drap)
    body.box((-3.5, 12, -2.9), (7, 12, 1), 'kgr', .3, 'k_drapf', d_k_drap)      # drap vert devant, sous la cuirasse doree
    body.box((-3.5, 12, 1.9), (7, 12, 1), 'kgr', .3, 'k_drapf', d_k_drap)       # et derriere
    rl = Part('right_leg')
    rl.box((-2, 0, -2), (4, 5, 4), 'kgr', .55, 'k_thigh', d_k_drap)
    rl.box((-2.3, 4.4, -3.2), (4, 3, 2), 'kgo', 0, 'k_knee', G(leaf=True))
    ll = mirrored(rl, 'left_leg')
    legs = [body, rl, ll]

    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 6, 4), 'kle', .5, 'k_boot', d_o_boot)
    rl.box((-2, 2, -2.9), (4, 7, 1), 'kgo', .1, 'k_greave', G(rows=(2, 4)))
    rl.box((-2.5, 4.6, -2.5), (5, 1, 5), 'kgl', .1, 'k_cuff', G())
    rl.box((-1, 10.4, -4.2), (2, 1, 2), 'kgo', 0, 'k_toe', G())
    ll = mirrored(rl, 'left_leg')
    return {'head': [head], 'chest': chest, 'legs': legs, 'boots': [rl, ll]}


SETS['elfking'] = elfking




# ================================================================== ROI NAIN (Erebor, regalien)
mat('dfur', (214, 212, 204), 5, 6, (1, 1, 1))
mat('dcp', (26, 40, 112), 4, 5, (.95, .95, 1.05))
mat('cl_blue', (30, 44, 118), 4, 5, (.95, .95, 1.05))
ANVIL = ['#######', '.#####.', '..###..', '.#####.']


def d_dk_gold(emb=False, rows=()):
    def f(face, F, rng):
        scratches(F, rng, 3, (255, 236, 176), .3)
        if face not in ('top', 'bottom') and F.h >= 3:
            F.hline(F.h - 1, 0, F.w, DGD, .6)
            for y in rows:
                if y + 1 < F.h: ridge(F, y, DGD, (255, 236, 176))
        if emb and face == 'front' and F.w >= 4 and F.h >= 3:
            F.art(max(0, (F.w - 4) // 2), max(0, (F.h - 3) // 2), ['####', '.##.', '####'][: F.h], {'#': (64, 40, 14)})
        rivets(F, (255, 240, 190), 0)
    return f


def d_dk_gem(col):
    def f(face, F, rng):
        F.rect(0, 0, F.w, F.h, col, .95)
        if F.w >= 1: F.px(0, 0, (255, 255, 255), .55)
    return f


def d_dk_fur(face, F, rng):
    for y in range(F.h):
        for x in range(F.w):
            r = rng.random()
            if r < .28: F.px(x, y, (130, 128, 122), .55)
            elif r < .42: F.px(x, y, (255, 255, 250), .5)
    if face not in ('top', 'bottom') and F.h >= 2: F.hline(F.h - 1, 0, F.w, (120, 118, 112), .5)


def d_dk_beard(face, F, rng):
    for y in range(F.h):
        F.hline(y, 0, F.w, (170, 120, 58) if y % 2 == 0 else (206, 156, 80), .45)
    for x in range(1, F.w, 2): F.vline(x, 0, F.h, (110, 74, 30), .35)
    if face not in ('top', 'bottom'): F.hline(F.h - 1, 0, F.w, DG, .8)


def d_dk_cape(face, F, rng):
    for y in range(F.h):
        F.hline(y, 0, F.w, (18, 28, 88) if y % 3 == 0 else (46, 66, 150), .35)
    if face == 'back':
        F.hline(F.h - 1, 0, F.w, DG, .9); F.hline(F.h - 2, 0, F.w, DGD, .5)
        F.vline(0, 0, F.h, DG, .9); F.vline(F.w - 1, 0, F.h, DG, .9)
        F.hline(0, 0, F.w, (236, 236, 226), .8)
        for x in range(0, F.w, 2): F.px(x, 1, (130, 128, 122), .7)                           # bande de fourrure en haut
        F.art(F.w // 2 - 3, 6, ANVIL, {'#': DG})                                              # emblème : enclume
        F.art(F.w // 2 - 2, 11, ['..#..', '.###.', '#####', '.###.', '..#..'], {'#': DG})     # soleil
    elif face in ('left', 'right'):
        F.vline(0, 0, F.h, DG, .8)
    elif face == 'bottom':
        F.rect(0, 0, F.w, F.h, DGD, .6)


def dwarfking():
    d = dwarf()
    head = d['head'][0]
    head.cubes = [c for c in head.cubes if c.key not in ('d_ridge', 'd_dome', 'd_cheek')]
    head.children = []                                                                      # (pas de camail : place de texture pour la couronne)
    G = d_dk_gold
    # couronne d'or sertie
    head.box((-4, -10, -4), (8, 2, 8), 'dgo', .65, 'dk_crown', G(rows=(0,)))
    for i, (x, z, tall) in enumerate(((-4.5, -4.5, 0), (-0.5, -4.5, 1), (3.5, -4.5, 0), (3.5, -0.5, 1), (3.5, 3.5, 0), (-0.5, 3.5, 1), (-4.5, 3.5, 0), (-4.5, -0.5, 1))):
        head.box((x, -13 - tall, z), (1, 3 + tall, 1), 'dgo', 0, 'dk_spk2' if tall else 'dk_spk', G())
    head.box((-0.5, -9.6, -5), (1, 1, 1), 'dgo', 0, 'dk_gem_r', d_dk_gem((210, 40, 50)))
    head.box((-5, -9.6, -0.5), (1, 1, 1), 'dgo', 0, 'dk_gem_b', d_dk_gem((60, 110, 230)))
    head.box((4, -9.6, -0.5), (1, 1, 1), 'dgo', 0, 'dk_gem_b', d_dk_gem((60, 110, 230)))
    # barbe royale tressee, bagues d'or
    head.box((-3, -1, -4.8), (6, 3, 2), 'dbr', .1, 'dk_beard1', d_dk_beard)
    head.box((-2, 2, -4.6), (4, 3, 2), 'dbr', .1, 'dk_beard2', d_dk_beard)
    head.box((-1.5, 5, -4.4), (3, 2, 2), 'dgo', .1, 'dk_bead', G())

    body = d['chest'][0]
    body.cubes = [c for c in body.cubes if c.key not in ('d_pec', 'd_buckle')]
    for ap in d['chest'][1:]: ap.cubes = [c for c in ap.cubes if c.key != 'd_brac']
    body.box((-3.6, 1, -3.4), (3, 5, 1), 'dgo', .1, 'dk_pec', G(rows=(2,)))
    body.cubes.append(Cube((0.6, 1, -3.4), (3, 5, 1), 'dgo', .1, 'dk_pec', G(rows=(2,)), True))
    body.box((-2, 8.4, -3.6), (4, 3, 1), 'dgo', .1, 'dk_buckle', G(emb=True))
    body.box((-4, -1.6, -1.5), (8, 2, 3), 'dfur', 1.0, 'dk_fur', d_dk_fur)                   # col de fourrure
    cape = Part('cape', (0, -0.6, 2.4), (R(4), 0, 0)); body.children.append(cape)
    cape.box((-5, 0, 0), (10, 19, 1), 'dcp', 0.3, 'dk_cape', d_dk_cape)
    for ap in d['chest'][1:]:
        left = ap.name.startswith('left')
        for pa in ap.children:                                                                # pointes d'or sur les epaulieres
            pa.cubes.append(Cube((-2.5 if not left else 1.5, -3, -0.5), (1, 2, 1), 'dgo', 0, 'dk_pspk', G()))
        ap.cubes.append(Cube((-1 if left else -3, 8, -2), (4, 3, 4), 'dgo', .5, 'dk_brac2', G(rows=(1,)), left))
    for lp in d['legs'][1:]:
        left = lp.name.startswith('left')
        lp.cubes.append(Cube((-0.5, 5, -4.2), (1, 1, 1), 'dgo', 0, 'dk_kspk', G()))
    for bp in d['boots']:
        bp.cubes = [c for c in bp.cubes if c.key != 'd_toe']
        bp.box((-2, 10.2, -3.6), (4, 2, 2), 'dgo', 0, 'dk_toe', G())
    return d


SETS['dwarfking'] = dwarfking


# ================================================================== PIEDS (bottes qui ressemblent a de vrais pieds nus)
mat('skin', (224, 172, 140), 5, 6, (1.0, .97, .96))
SK_D, SK_L, SK_V, SK_C, SK_N = (178, 120, 94), (246, 206, 176), (150, 140, 178), (214, 170, 112), (244, 208, 198)


def d_ft(kind):
    """texture de peau detaillee : pores, ombres, veines, tendons, callosites (plante), ongles, plis"""
    def f(face, F, rng):
        for y in range(F.h):                                                          # grain de peau
            for x in range(F.w):
                r = rng.random()
                if r < .18: F.px(x, y, SK_D, .30)
                elif r < .30: F.px(x, y, SK_L, .38)
                elif r < .33: F.px(x, y, (200, 96, 86), .22)                           # capillaires
        if face == 'bottom':                                                          # plante du pied
            if kind in ('heel', 'ball', 'mid'):
                for y in range(F.h):
                    for x in range(F.w):
                        if rng.random() < .7: F.px(x, y, SK_C, .45)                    # callosites jaunes
                if kind == 'mid': F.rect(0, 0, F.w, F.h, SK_L, .25)                    # voute : peau plus claire, non appuyee
            for y in range(1, F.h, 2):                                                # plis de la plante
                for x in range(F.w):
                    if rng.random() < .35: F.px(x, y, SK_D, .5)
        elif face == 'top':
            if kind == 'mid':                                                         # dessus du pied : tendons + veines
                for x in range(1, F.w, 1):
                    if x % 2 == 1: F.vline(x, 0, F.h, SK_L, .35)
                vx = 1
                for y in range(F.h):
                    F.px(min(F.w - 1, vx), y, SK_V, .55)
                    if rng.random() < .45: vx = max(0, min(F.w - 1, vx + int(rng.integers(-1, 2))))
                F.px(0, F.h // 2, SK_V, .45); F.px(F.w - 1, F.h // 2 + 1, SK_V, .45)
            elif kind in ('toe', 'toe5', 'big'):                                      # ongle en bout d'orteil
                nh = 2 if kind == 'big' else 1
                F.rect(0, 0, F.w, nh, SK_N, .95)
                if kind == 'big': F.hline(nh - 1, 0, F.w, (206, 150, 140), .6); F.px(0, 0, SK_D, .4); F.px(F.w - 1, 0, SK_D, .4)
                else: F.px(0, 0, (230, 178, 168), .6)
                if F.h > nh + 1: F.hline(F.h - 2, 0, F.w, SK_D, .4)                    # pli de l'articulation
            elif kind == 'ankle':
                F.rect(0, 0, F.w, F.h, SK_L, .15)
        elif kind == 'ankle' and face in ('front', 'back', 'left', 'right'):
            F.vline(F.w // 2, 0, F.h, SK_D, .3)                                       # tendon
            if face in ('left', 'right') and F.h >= 3: F.px(F.w // 2, F.h - 2, SK_L, .8); F.px(F.w // 2 + 1, F.h - 2, SK_D, .5)   # malleole
            if face == 'front': F.px(1, 1, SK_V, .5); F.px(2, 2, SK_V, .5)
            F.hline(F.h - 1, 0, F.w, SK_D, .25)
        elif kind in ('toe', 'toe5', 'big') and face in ('front', 'left', 'right'):
            if face == 'front': F.px(F.w // 2, 0, SK_N, .6)                           # bout d'orteil
            else: F.hline(F.h - 1, 0, F.w, SK_D, .5)                                   # dessous plus sombre
        elif kind in ('mid', 'ball', 'heel') and face in ('front', 'back', 'left', 'right'):
            F.hline(F.h - 1, 0, F.w, SK_D, .35)
            if kind == 'heel': F.rect(0, F.h - 1, F.w, 1, SK_C, .5)
    return f


def feet():
    rl = Part('right_leg')
    rl.box((-2, 6, -2), (4, 4, 4), 'skin', .3, 'ft_ankle', d_ft('ankle'))
    rl.box((-2, 10, -3), (4, 2, 5), 'skin', .12, 'ft_mid', d_ft('mid'))
    rl.box((-1.5, 10, 0.5), (3, 2, 2), 'skin', .22, 'ft_heel', d_ft('heel'))
    rl.box((-2, 10, -4), (4, 2, 1), 'skin', .2, 'ft_ball', d_ft('ball'))
    # orteils : cubes a inflation negative (largeur visible = taille + 2 x inflation)
    rl.box((0.35, 10.35, -6.35), (2, 2, 3), 'skin', -.35, 'ft_big', d_ft('big'))
    for nm, x, z in (('t2', -0.15, -5.85), ('t3', -0.85, -5.65), ('t4', -1.55, -5.35)):
        rl.box((x, 11.15, z), (1, 1, 2), 'skin', -.15, 'ft_toe', d_ft('toe'), nm)
    rl.box((-2.1, 11.2, -5.1), (1, 1, 1), 'skin', -.2, 'ft_toe5', d_ft('toe5'))
    ll = mirrored(rl, 'left_leg')
    return {'head': [], 'chest': [], 'legs': [], 'boots': [rl, ll]}


SETS['feet'] = feet


def _with_drape(fn, matn, dark, light, trim, tag, drop=()):
    """enveloppe un set : ajoute le drap sur chest (bras), legs (pans + jambes)"""
    def g():
        d = fn()
        dec = cloth(dark, light, trim)
        ch = d['chest']; lg = d['legs']
        arms = [p for p in ch if p.name.endswith('_arm')]
        for ap in arms:
            left = ap.name.startswith('left')
            ap.cubes.insert(0, Cube((-1 if left else -3, -2, -2), (4, 12, 4), matn, .3, f'{tag}_sleeve', dec, left))
        bd = [p for p in lg if p.name == 'body'][0]
        bd.cubes.insert(0, Cube((-3.5, 12, -2.6), (7, 12, 1), matn, .3, f'{tag}_drape', dec))
        bd.cubes.insert(0, Cube((-3.5, 12, 1.6), (7, 12, 1), matn, .3, f'{tag}_drape', dec))
        for lp in [p for p in lg if p.name.endswith('_leg')]:
            lp.cubes.insert(0, Cube((-2, 0, -2), (4, 12, 4), matn, .2, f'{tag}_legcl', dec))
        return d
    return g


SETS['dwarven'] = _with_drape(dwarf, 'cl_dgrey', (30, 30, 38), (90, 92, 104), (214, 172, 80), 'dd')
SETS['rohan'] = _with_drape(rohan, 'cl_red', (60, 10, 14), (160, 50, 52), (226, 186, 96), 'rd')
SETS['gondor'] = _with_drape(gondor, 'cl_lgrey', (110, 112, 122), (226, 228, 236), (214, 178, 84), 'gd')
SETS['dwarfking'] = _with_drape(dwarfking, 'cl_blue', (14, 20, 70), (54, 72, 150), (226, 186, 96), 'kd')
SETS['elven'] = _with_drape(elf, 'cl_green', (34, 66, 44), (104, 150, 112), (232, 200, 108), 'ed')


# variantes rouillees (meme modele, texture rouillee ; Gondor : aile gauche abimee)
for _b in ('orc', 'dwarven', 'elven', 'rohan', 'elfking', 'dwarfking'):
    SETS[_b + '_rusted'] = SETS[_b]
SETS['gondor_rusted'] = _with_drape(lambda: gondor(rusted=True), 'cl_lgrey', (110, 112, 122), (226, 228, 236), (214, 178, 84), 'gd')


# ------------------------------------------------------------------ sortie / apercu
PIECES = ('head', 'chest', 'boots', 'legs')
TILE_OF = {'head': (0, 0, 1), 'chest': (32, 0, 1), 'boots': (0, 32, 1), 'legs': (0, 0, 2)}
TILE_SIZE = {'head': (32, 32), 'chest': (32, 64), 'boots': (32, 32), 'legs': (64, 64)}   # taille de case disponible dans la feuille 64x64


def rustify(tex, seed, style=1):
    """version tres rouillee (200 ans d'abandon) : rouille epaisse, ecailles, piqures, vert-de-gris sur l'or, coulures, trous"""
    rng = np.random.default_rng(seed)
    h, w = tex.shape[:2]
    a = tex[..., :3].astype(float); al = tex[..., 3].copy()
    opaque = al > 0
    yy, xx = np.ogrid[:h, :w]
    noise = np.zeros((h, w))
    for _ in range(max(30, h * w // 45)):                                 # grandes plaques de corrosion
        cx, cy, r = int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(2, 6))
        noise += (((xx - cx) ** 2 + (yy - cy) ** 2) <= r * r) * rng.uniform(.35, .9)
    noise = np.clip(noise, 0, 1.4)
    # vert-de-gris : sur les pixels dores/bronze d'origine
    gold = (a[..., 0] > 140) & (a[..., 0] > a[..., 2] + 50) & (a[..., 1] > a[..., 2] + 20)
    verd = np.array((74., 118., 96.))
    a = np.where((gold & (noise < .9))[..., None], a * .35 + verd * .65, a)
    tone = np.zeros((h, w))                                                # teinte de rouille : plaques, pas du sel et poivre
    for _ in range(max(14, h * w // 80)):
        cx, cy, r = int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(2, 5))
        tone += (((xx - cx) ** 2 + (yy - cy) ** 2) <= r * r) * rng.uniform(.3, .8)
    pick = np.clip(tone / 1.2, 0, 1) * .8 + rng.random((h, w)) * .2
    pal = {1: ((84., 40., 20.), (136., 66., 28.), (188., 102., 44.)),
           2: ((62., 38., 26.), (104., 62., 36.), (150., 98., 56.)),          # style 2 : brun terne, tres moussu
           3: ((110., 44., 14.), (172., 78., 22.), (222., 124., 44.))}[style]  # style 3 : orange vif, croute epaisse
    rust = np.where((pick < .33)[..., None], np.array(pal[0]), np.where((pick < .7)[..., None], np.array(pal[1]), np.array(pal[2])))
    k = np.clip(noise * .95 + .02, 0, .9)[..., None]                       # couverture ~65 %, le reste laisse voir la couleur d'origine
    out = a * (1 - k) + rust * k
    # ecailles de rouille claires et piqures sombres
    flake = (rng.random((h, w)) < {1: .06, 2: .04, 3: .13}[style]) & (noise > .3)
    pit = (rng.random((h, w)) < .035) & (noise > .3)
    out[flake] = np.array((205., 120., 52.)) * .75 + out[flake] * .25
    out[pit] = np.array((38., 20., 12.))
    # coulures : la rouille descend sur 1 a 4 pixels
    drip = (rng.random((h, w)) < .045) & (noise > .45)
    for y, x in zip(*np.nonzero(drip)):
        for d in range(1, int(rng.integers(2, 5))):
            if y + d < h and opaque[y + d, x]: out[y + d, x] = out[y + d, x] * .45 + np.array((96., 44., 20.)) * .55
    g = out.mean(axis=2, keepdims=True)
    out = (out * .88 + g * .12) * .9                                      # ternit
    # --- details supplementaires ---
    # relief de la croute : cote eclaire clair / cote a l'ombre sombre
    nz = np.pad(noise, 1, mode='edge')
    rel = (nz[:-2, 1:-1] + nz[1:-1, :-2]) - (nz[2:, 1:-1] + nz[1:-1, 2:])
    out = out * 1.12 + (rel * 16)[..., None] * (noise > .25)[..., None]
    # usure des aretes : les contours (garnitures, rivets, jonctions) rongent en brun sombre
    lum = a.mean(axis=2); lp = np.pad(lum, 1, mode='edge')
    edge = (np.abs(lp[1:-1, 2:] - lp[1:-1, :-2]) + np.abs(lp[2:, 1:-1] - lp[:-2, 1:-1])) > 38
    ed = edge & (rng.random((h, w)) < .4)
    out[ed] = out[ed] * .6 + np.array((70., 32., 16.)) * .4
    # acier mis a nu : rayures claires dans la rouille
    for _ in range(max(4, h * w // 260)):
        x0, y0 = int(rng.integers(0, w)), int(rng.integers(0, h)); dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][int(rng.integers(0, 4))]
        for t in range(int(rng.integers(3, 8))):
            x, y = x0 + dx * t, y0 + dy * t
            if 0 <= x < w and 0 <= y < h and opaque[y, x]: out[y, x] = out[y, x] * .4 + np.array((150., 146., 140.)) * .6
    # fissures sombres (marche aleatoire)
    for _ in range(max(2, h * w // 600)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h)); dx = int(rng.integers(-1, 2)); dy = int(rng.integers(0, 2)) or -1
        for t in range(int(rng.integers(5, 14))):
            if 0 <= x < w and 0 <= y < h and opaque[y, x]: out[y, x] = np.array((30., 16., 10.))
            if rng.random() < .5: x += dx
            else: y += dy
            if rng.random() < .25: dx = int(rng.integers(-1, 2))
    # mousse / lichen verdatre dans les creux (taches ponctuees)
    moss = np.zeros((h, w))
    for _ in range(max(3, h * w // {1: 520, 2: 150, 3: 2000}[style])):
        cx, cy, r = int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(1, 3 if style != 2 else 4))
        moss += (((xx - cx) ** 2 + (yy - cy) ** 2) <= r * r) * rng.uniform(.4, 1.0)
    mm = (moss > .55) & (rng.random((h, w)) < .7) & (noise > .3)
    out[mm] = out[mm] * .3 + np.array((84., 104., 44.)) * .7
    spot = mm & (rng.random((h, w)) < .22)
    out[spot] = np.array((128., 146., 70.))
    # crasse sombre (terre, huile seche) en taches basses opacite
    grime = np.zeros((h, w))
    for _ in range(max(3, h * w // 300)):
        cx, cy, r = int(rng.integers(0, w)), int(rng.integers(0, h)), int(rng.integers(2, 5))
        grime += (((xx - cx) ** 2 + (yy - cy) ** 2) <= r * r) * .3
    out *= (1 - np.clip(grime, 0, .5) * .3)[..., None]
    # coulures de rouille plus longues et plus marquees sous les zones tres corrodees
    drip2 = (rng.random((h, w)) < .03) & (noise > .8)
    for y, x in zip(*np.nonzero(drip2)):
        for d in range(1, int(rng.integers(4, 8))):
            if y + d < h and opaque[y + d, x]: out[y + d, x] = out[y + d, x] * (.5 + .05 * d) + np.array((110., 50., 22.)) * (.5 - .05 * d)
    # trous (tissu pourri / metal perfore) dans les zones tres corrodees
    hole = (rng.random((h, w)) < .05) & (noise > .6)
    al[hole] = 0
    # bords rongés : quelques pixels d'aretes manquants
    bite = edge & (rng.random((h, w)) < .07) & (noise > .5)
    al[bite] = 0
    return np.dstack([np.clip(out, 0, 255), al]).astype(np.uint8)


RUIN_KEYS = ('dk_spk', 'dk_spk2', 'r_spike', 'r_spike_c', 'dk_pspk', 'o_spike', 'o_spike2', 'o_spikez', 'dk_kspk')


def _ruin(parts, rng):
    """version rouillee : pointes cassees / manquantes, feuilles arrachees"""
    for p in parts:
        p.cubes = [c for c in p.cubes if not (c.key in RUIN_KEYS and rng.random() < .4)]
        p.children = [ch for ch in p.children if not (ch.name.startswith('leaf_') and rng.random() < .4)]
        for c in p.cubes:
            if c.key in RUIN_KEYS and c.sz[1] > 1 and rng.random() < .5: c.key += '_t'; c.sz = (c.sz[0], max(1, c.sz[1] - 1), c.sz[2]); c.o = (c.o[0], c.o[1] + 1, c.o[2])   # pointe tronquee
        _ruin(p.children, rng)


def build_set(name, variant=1):
    pieces = SETS[name]()
    if name.endswith('_rusted') and name != 'gondor_rusted':
        for pc in PIECES: _ruin(pieces[pc], np.random.default_rng(zlib.crc32((name + pc + 'ruin').encode())))
    out = {}
    for pc in PIECES:
        parts = pieces[pc]
        tex = paint(parts, TILE_SIZE[pc])
        if name.endswith('_rusted'): tex = rustify(tex, zlib.crc32((name + pc + ('' if variant == 1 else f'v{variant}')).encode()), variant)
        out[pc] = (parts, tex)
    return out


def sheet_textures(built):
    sheets = {1: np.zeros((64, 64, 4), np.uint8), 2: np.zeros((64, 64, 4), np.uint8)}
    for pc, (parts, tex) in built.items():
        tx, ty, layer = TILE_OF[pc]
        sheets[layer][ty:ty + tex.shape[0], tx:tx + tex.shape[1]] = tex
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
        os.makedirs(f'{A}/textures/models/armor', exist_ok=True)
        os.makedirs(f'{A}/geo', exist_ok=True)
        for variant in ((1, 2, 3) if name.endswith('_rusted') else (1,)):        # 3 textures de rouille : variante 1 = <nom>, 2 = <nom>_v2, 3 = <nom>_v3
            built = build_set(name, variant)
            sheets = sheet_textures(built)
            suf = '' if variant == 1 else f'_v{variant}'
            for layer in (1, 2):
                Image.fromarray(sheets[layer], 'RGBA').save(f'{A}/textures/models/armor/{name}{suf}_layer_{layer}.png')
            if variant == 1:
                for pc in PIECES:
                    json.dump(piece_geo(built, pc), open(f'{A}/geo/armor_{name}_{pc}.json', 'w'), separators=(',', ':'))
        print('ecrit', name)


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'preview'
    names = sys.argv[2:] or list(SETS)
    (preview if cmd == 'preview' else gen)(names)
