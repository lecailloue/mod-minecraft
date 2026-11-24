"""Faune des bois et des rivieres : cerf, chevreuil, renard, ecureuil, canard, heron, castor.
Meme chaine que beasts.py (cubes + groupes -> bbmodel -> geo du mod), textures peintes par materiau.

    python3 tools/fauna.py                 -> apercus dans /tmp/fauna (4 vues par bete)
    python3 tools/fauna.py cerf renard     -> seulement celles-la
    python3 tools/fauna.py gen             -> ecrit geo + textures dans les assets du mod

Rien n'est branche en jeu : ce fichier ne fait que les modeles et leurs textures.
"""
import os, sys
import trollcore as tc
from trollcore import *
import beasts
from beasts import mkT, fx_eye, d_fur_w, d_antler, d_hoof, d_hide, d_bristle, d_feather, d_beak, d_ebeak, d_talon, d_grey, d_black

HERE = os.path.dirname(os.path.abspath(__file__))

# ------------------------------------------------------------------ materiaux (base, grain, teinte)
tc.MATS.update({
    'deer':      dict(base=(134, 100, 68), n1=2, n2=2, tint=(1, .97, .9)),
    'deer_pale': dict(base=(198, 176, 146), n1=2, n2=2, tint=(1, .99, .94)),
    'roe':       dict(base=(160, 112, 68), n1=2, n2=2, tint=(1, .96, .88)),
    'roe_pale':  dict(base=(226, 216, 198), n1=2, n2=2, tint=(1, .99, .97)),
    'antler':    dict(base=(174, 156, 124), n1=2, n2=3, tint=(1, .97, .88)),
    'hoofd':     dict(base=(46, 40, 36), n1=2, n2=2, tint=(1, 1, 1.04)),
    'fox':       dict(base=(192, 100, 40), n1=2, n2=2, tint=(1, .94, .82)),
    'fox_pale':  dict(base=(234, 228, 218), n1=2, n2=2, tint=(1, .99, .98)),
    'fox_dark':  dict(base=(48, 40, 38), n1=2, n2=2, tint=(1, 1, 1.03)),
    'squirrel':  dict(base=(168, 92, 50), n1=2, n2=2, tint=(1, .94, .83)),
    'squi_pale': dict(base=(232, 224, 212), n1=2, n2=2, tint=(1, .99, .98)),
    'duck_head': dict(base=(34, 96, 66), n1=5, n2=7, tint=(.9, 1, .95)),
    'duck_body': dict(base=(150, 146, 138), n1=5, n2=7, tint=(1, 1, 1.02)),
    'duck_br':   dict(base=(122, 74, 52), n1=5, n2=6, tint=(1, .94, .86)),
    'duck_bill': dict(base=(214, 178, 62), n1=5, n2=6, tint=(1, .97, .78)),
    'duck_white': dict(base=(238, 234, 226), n1=3, n2=4, tint=(1, 1, 1)),
    'duck_wing': dict(base=(74, 86, 122), n1=5, n2=6, tint=(.95, .98, 1.06)),
    'heron':     dict(base=(196, 198, 202), n1=2, n2=2, tint=(1, 1, 1.02)),
    'heron_pale': dict(base=(232, 233, 234), n1=1, n2=2, tint=(1, 1, 1.01)),
    'heron_wing': dict(base=(96, 100, 108), n1=2, n2=2, tint=(1, 1, 1.04)),
    'heron_dark': dict(base=(40, 42, 48), n1=2, n2=2, tint=(1, 1, 1.05)),
    'heron_bill': dict(base=(208, 132, 54), n1=2, n2=2, tint=(1, .96, .8)),
    'heron_leg':  dict(base=(84, 72, 62), n1=2, n2=2, tint=(1, .98, .94)),
    'beaver':    dict(base=(100, 68, 44), n1=2, n2=2, tint=(1, .96, .88)),
    'beaver_d':  dict(base=(70, 48, 32), n1=2, n2=2, tint=(1, .96, .9)),
    'beaver_tl': dict(base=(60, 48, 44), n1=2, n2=3, tint=(1, .99, .97)),
    'tooth':     dict(base=(224, 206, 140), n1=2, n2=2, tint=(1, .99, .84)),
})

def flat(dark, density=22, a=.42, grad=.985):
    """texture a la maniere de vanilla : aplat, quelques pixels plus sombres, et surtout pas de
    liseré sur les bords ni d'ecart de lumiere d'une face a l'autre (sinon on voit les cubes)"""
    import numpy as _np
    def f(arr, face, rng):
        h, w, _ = arr.shape
        if w >= 3 and h >= 3:                      # on annule l'assombrissement des bords de base_face
            arr[0, :] /= .9
            arr[-1, :] /= .88
            arr[:, 0] /= .92
            arr[:, -1] /= .9
        arr /= SHADE[face] ** .72                  # presque plus d'ecart entre les faces
        for _ in range(max(1, int(h * w / density))):
            x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
            put(arr, x, y, dark, a)
            if rng.random() < .35: put(arr, x + 1, y, dark, a * .8)
            if rng.random() < .2: put(arr, x, y + 1, dark, a * .7)
        if face not in ('up', 'down') and h > 5:
            arr *= _np.linspace(1.01, grad, h)[:, None, None]
    return f


# peinture de chaque matiere : fourrure, plume, corne, bec
tc.DECOR.update({
    'deer': flat((92, 66, 42)), 'deer_pale': flat((160, 138, 110), 30),
    'roe': flat((112, 74, 42)), 'roe_pale': flat((190, 178, 158), 30),
    'antler': flat((132, 114, 84), 26), 'hoofd': flat((28, 24, 22), 40),
    'fox': flat((136, 62, 18)), 'fox_pale': flat((196, 190, 180), 30),
    'fox_dark': flat((26, 22, 22), 34),
    'squirrel': flat((116, 58, 26)), 'squi_pale': flat((194, 186, 174), 30),
    'duck_head': d_feather((56, 134, 96), (14, 56, 38), 2), 'duck_body': d_feather((186, 182, 174), (92, 88, 82), 3),
    'duck_br': d_feather((156, 102, 72), (70, 40, 26), 3), 'duck_bill': d_ebeak, 'duck_white': d_feather((250, 248, 244), (190, 186, 178), 3),
    'duck_wing': d_feather((104, 118, 158), (36, 44, 72), 3),
    'heron': flat((150, 154, 160), 30), 'heron_pale': flat((198, 200, 202), 34),
    'heron_wing': flat((56, 60, 68), 28), 'heron_dark': flat((22, 24, 30), 40),
    'heron_bill': flat((162, 96, 32), 30), 'heron_leg': flat((52, 44, 38), 30),
    'beaver': flat((62, 40, 24)), 'beaver_d': flat((44, 28, 18), 26),
    'beaver_tl': flat((36, 28, 26), 14, .5), 'tooth': flat((186, 166, 100), 34),
})

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


# ------------------------------------------------------------------ pattes communes aux quadrupedes
def quad_legs(B, G, mat, hoof, y_top, y_hoof, thick, zf, zb, spread, keys, hoof_h=2.0):
    """quatre pattes droites : (avant droite, avant gauche, arriere droite, arriere gauche)"""
    out = []
    for nm, s, z, hind in (('leg_fr', 1, zf, False), ('leg_fl', -1, zf, False), ('leg_br', 1, zb, True), ('leg_bl', -1, zb, True)):
        x = spread * s
        kids = [B(f'{nm}_up', x - thick / 2 - .4, y_hoof + (y_top - y_hoof) * .45, z - thick / 2 - .6,
                  thick + .8, (y_top - y_hoof) * .55, thick + 1.2, mat, key=keys[0] + ('h' if hind else 'f')),
                B(f'{nm}_lo', x - thick / 2, y_hoof, z - thick / 2, thick, (y_top - y_hoof) * .5, thick, mat, key=keys[1]),
                B(f'{nm}_hoof', x - thick / 2 - .3, 0, z - thick / 2 - .6, thick + .6, hoof_h, thick + 1.2, hoof, key=keys[2])]
        out.append(G(nm, (x, y_top, z), children=kids))
    return out


def antlers(B, G, s, key, y, z, size=1.0):
    """bois : un merrain qui monte vers l'arriere et trois andouillers"""
    x = 1.7 * s
    return G(f'antler_{"r" if s > 0 else "l"}', (x, y, z), rot=(0, 0, -14 * s), children=[
        B(f'ant_{s}_b1', x - .9, y, z - .9, 1.8, 5 * size, 1.8, 'antler', key=key + 'b1'),
        B(f'ant_{s}_b2', x - .8, y + 4.6 * size, z + .4, 1.6, 5 * size, 1.6, 'antler', key=key + 'b2'),
        B(f'ant_{s}_b3', x - .7, y + 9 * size, z + 1.8, 1.4, 4 * size, 1.4, 'antler', key=key + 'b3'),
        B(f'ant_{s}_w', x - .7, y + 1.6, z - 3.6, 1.4, 1.4, 3, 'antler', key=key + 'w'),
        B(f'ant_{s}_m', x - .7, y + 5.4 * size, z - 2.6, 1.4, 1.4, 3.4, 'antler', key=key + 'm'),
        B(f'ant_{s}_t', x - .7, y + 11.5 * size, z - 1.4, 1.4, 1.4, 3.2, 'antler', key=key + 't'),
    ])


# ================================================================== cerf
@model
def cerf():
    """meme charpente que le cerf blanc, en robe brune et sans le harnais elfique"""
    B, G = mkT(0.94)
    legs = []

    def leg(nm, s, z, hind):
        x = 4 * s
        if hind:
            kids = [B(f'{nm}_thigh', x - 3, 15, z - 3.5, 6, 11, 7, 'deer', key='c_thigh'),
                    B(f'{nm}_hockb', x - 1.5, 11, z + 1.2, 3, 5, 2.4, 'deer', key='c_hockb'),
                    B(f'{nm}_shin', x - 1.6, 3, z - 1.6, 3.2, 12, 3.2, 'deer', key='c_shin'),
                    B(f'{nm}_hoof', x - 2, 0, z - 2.6, 4, 3.5, 5, 'hoofd', key='c_hoof')]
        else:
            kids = [B(f'{nm}_arm', x - 2.4, 15, z - 2.8, 4.8, 11, 5.6, 'deer', key='c_arm'),
                    B(f'{nm}_shin', x - 1.6, 3, z - 1.6, 3.2, 13, 3.2, 'deer', key='c_fshin'),
                    B(f'{nm}_hoof', x - 2, 0, z - 2.6, 4, 3.5, 5, 'hoofd', key='c_fhoof')]
        return G(nm, (x, 26, z), children=kids)
    legs += [leg('leg_fr', 1, -9, False), leg('leg_fl', -1, -9, False), leg('leg_br', 1, 10, True), leg('leg_bl', -1, 10, True)]

    body_kids = [
        B('chest', -5, 21, -14, 10, 14, 10, 'deer', key='c_chest'),
        B('torso', -4.6, 22, -5, 9.2, 12, 20, 'deer', key='c_torso'),
        B('rump', -4.8, 23, 12, 9.6, 11, 5, 'deer', key='c_rump'),
        B('belly', -3.6, 20.5, -8, 7.2, 2, 22, 'deer_pale', key='c_belly'),
        B('ruff', -4.2, 26, -17, 8.4, 11, 5, 'deer_pale', key='c_ruff'),
        B('tail', -1.6, 29, 16, 3.2, 6, 3, 'deer_pale', key='c_tail'),
    ]
    neck = G('neck', (0, 31, -12), rot=(-40, 0, 0), children=[
        B('neck_base', -3.9, 31, -15.6, 7.8, 8, 7.6, 'deer', key='c_nbase'),
        B('neck_a', -3.2, 38, -15.2, 6.4, 11.5, 6.4, 'deer', key='c_neck'),
        B('neck_ruff', -3.8, 33, -16.6, 7.6, 12, 2.2, 'deer_pale', key='c_nruff'),
        B('neck_mane', -2, 33.4, -9.8, 4, 14, 1.8, 'deer_pale', key='c_nmane'),
    ])
    head = G('head', (0, 49, -12), rot=(26, 0, 0), children=[
        B('skull', -3.4, 45.5, -19, 6.8, 7, 9, 'deer', key='c_skull', fx=fx_eye(1, 2, col=(30, 20, 16), glint=(250, 250, 255))),
        B('muzzle', -2.4, 43.9, -25, 4.8, 5, 7, 'deer', key='c_muzzle'),
        B('nose', -2.5, 43.5, -26.5, 5, 3, 2, 'dnose', key='c_nose'),
        B('jaw_s', -2, 42.5, -24, 4, 1.8, 6, 'deer', key='c_jaw'),
        beasts.antler(B, G, 1, 'c_antr'), beasts.antler(B, G, -1, 'c_antl'),
        # oreilles : feuille large ouverte vers l'avant (face interieure rose devant), ecartee de cote et penchee en avant
        G('ear_r', (3.2, 49.5, -12), rot=(24, 0, -62), children=[B('ear_r_o', 3.0, 49.5, -12.7, 3.8, 9, 1.4, 'deer', key='c_ear'), B('ear_r_i', 3.7, 50.3, -13.3, 2.4, 7.2, .6, 'earin', key='c_earin')]),
        G('ear_l', (-3.2, 49.5, -12), rot=(24, 0, 62), children=[B('ear_l_o', -6.8, 49.5, -12.7, 3.8, 9, 1.4, 'deer', key='c_ear'), B('ear_l_i', -6.1, 50.3, -13.3, 2.4, 7.2, .6, 'earin', key='c_earin')]),
    ])
    neck.children.append(head)
    body = G('body', (0, 28, 0), children=body_kids + [neck])
    return legs + [body]


# ================================================================== chevreuil
@model
def chevreuil():
    B, G = mkT(0.74)
    legs = quad_legs(B, G, 'roe', 'hoofd', 20, 2.2, 3.2, -6.5, 7.5, 3.4, ('rl_u', 'rl_l', 'rl_h'))
    body = [
        B('chest', -4.2, 17, -10, 8.4, 11, 8, 'roe', key='r_chest'),
        B('torso', -3.9, 18, -3, 7.8, 10, 12, 'roe', key='r_torso'),
        B('rump', -4.1, 18.5, 9, 8.2, 10, 4.5, 'roe', key='r_rump'),
        B('patch', -3.6, 21, 12.6, 7.2, 7, 1.2, 'roe_pale', key='r_patch'),
        B('belly', -3, 16.6, -5, 6, 2, 15, 'roe_pale', key='r_belly'),
    ]
    head = G('head', (0, 33, -9), rot=(20, 0, 0), children=[
        B('skull', -2.6, 30, -14, 5.2, 5.6, 6.6, 'roe', key='r_skull', fx=fx_eye(1, 2, col=(24, 16, 12))),
        B('muzzle', -1.9, 29, -18, 3.8, 3.8, 4.4, 'roe', key='r_muzzle'),
        B('nose', -2, 28.9, -18.9, 4, 2.2, 1.4, 'hoofd', key='r_nose'),
        B('chin', -1.6, 28.2, -18.4, 3.2, 1.4, 2.6, 'roe_pale', key='r_chin'),
        G('ear_r', (2.4, 34, -10), rot=(14, 0, -54), children=[
            B('ear_r_o', 2.2, 34, -10.6, 3.0, 6.8, 1.1, 'roe', key='r_ear'),
            B('ear_r_i', 2.8, 34.7, -11.1, 1.8, 5.2, .6, 'roe_pale', key='r_earin')]),
        G('ear_l', (-2.4, 34, -10), rot=(14, 0, 54), children=[
            B('ear_l_o', -5.2, 34, -10.6, 3.0, 6.8, 1.1, 'roe', key='r_ear'),
            B('ear_l_i', -4.6, 34.7, -11.1, 1.8, 5.2, .6, 'roe_pale', key='r_earin')]),
        # petits bois droits a deux pointes
        B('horn_r', 1.3, 35, -11.4, 1.4, 6, 1.4, 'antler', key='r_hornb'),
        B('horn_r2', 1.3, 40, -12.6, 1.2, 3.4, 1.2, 'antler', key='r_hornt'),
        B('horn_l', -2.7, 35, -11.4, 1.4, 6, 1.4, 'antler', key='r_hornb'),
        B('horn_l2', -2.5, 40, -12.6, 1.2, 3.4, 1.2, 'antler', key='r_hornt'),
    ])
    neck = G('neck', (0, 24, -7.5), rot=(-30, 0, 0), children=[
        B('neck_a', -2.6, 24, -11, 5.2, 10, 5.4, 'roe', key='r_neck'),
        head,
    ])
    return [G('body', (0, 0, 0), children=body + legs + [neck])]


# ================================================================== renard
@model
def renard():
    B, G = mkT(0.92)
    legs = []
    for nm, s, z in (('leg_fr', 1, -5), ('leg_fl', -1, -5), ('leg_br', 1, 5.5), ('leg_bl', -1, 5.5)):
        x = 2.6 * s
        legs.append(G(nm, (x, 9, z), children=[
            B(f'{nm}_up', x - 1.5, 4, z - 1.8, 3, 5.5, 3.6, 'fox', key='f_lu'),
            B(f'{nm}_lo', x - 1.2, 0, z - 1.4, 2.4, 4.6, 2.8, 'fox_dark', key='f_ll')]))
    body = [
        B('chest', -3.2, 8, -7, 6.4, 7, 6, 'fox', key='f_chest'),
        B('torso', -3, 8.4, -2, 6, 6.6, 9, 'fox', key='f_torso'),
        B('rump', -3.1, 8.2, 6, 6.2, 6.8, 4, 'fox', key='f_rump'),
        B('bib', -2.4, 7.6, -8.4, 4.8, 4.4, 2, 'fox_pale', key='f_bib'),
        B('belly', -2.2, 7.4, -4, 4.4, 1.6, 11, 'fox_pale', key='f_belly'),
    ]
    tail = G('tail', (0, 11, 9), rot=(-18, 0, 0), children=[
        B('tail_a', -2.4, 9, 9, 4.8, 5, 5, 'fox', key='f_tail1'),
        B('tail_b', -2.8, 9.4, 13, 5.6, 5.6, 5, 'fox', key='f_tail2'),
        B('tail_c', -2.2, 9.6, 17.5, 4.4, 4.4, 4, 'fox_pale', key='f_tail3'),
    ])
    head = G('head', (0, 13, -7), rot=(6, 0, 0), children=[
        B('skull', -2.8, 11.5, -12, 5.6, 5, 5.6, 'fox', key='f_skull', fx=fx_eye(1, 2, col=(22, 16, 12), glint=(250, 230, 160))),
        B('cheek_r', 2.3, 11.6, -11.4, 1.4, 3.4, 4.2, 'fox_pale', key='f_cheek'),
        B('cheek_l', -3.7, 11.6, -11.4, 1.4, 3.4, 4.2, 'fox_pale', key='f_cheek'),
        B('snout', -1.4, 11.2, -15.6, 2.8, 2.8, 4, 'fox_pale', key='f_snout'),
        B('nose', -1, 12.2, -16.4, 2, 1.6, 1.2, 'fox_dark', key='f_nose'),
        G('ear_r', (2, 16.4, -9.4), rot=(0, 0, -12), children=[
            B('ear_r_o', 1.2, 16.4, -10.2, 2.8, 4.4, 1.2, 'fox', key='f_ear'),
            B('ear_r_t', 1.6, 19.8, -10.0, 2.0, 1.4, .9, 'fox_dark', key='f_eart')]),
        G('ear_l', (-2, 16.4, -9.4), rot=(0, 0, 12), children=[
            B('ear_l_o', -4.0, 16.4, -10.2, 2.8, 4.4, 1.2, 'fox', key='f_ear'),
            B('ear_l_t', -3.6, 19.8, -10.0, 2.0, 1.4, .9, 'fox_dark', key='f_eart')]),
    ])
    return [G('body', (0, 0, 0), children=body + legs + [tail, head])]


# ================================================================== ecureuil
@model
def ecureuil():
    """assis sur l'arriere-train, grosse tete, panache en C au dessus du dos"""
    B, G = mkT(0.8)
    body = [
        B('torso', -2.6, 4, -3.4, 5.2, 7.5, 6.6, 'squirrel', key='e_torso'),
        B('belly', -2, 4.4, -4.2, 4, 6, 1.6, 'squi_pale', key='e_belly'),
        B('leg_r', 1.4, 0, -2.6, 2.4, 4.4, 4.6, 'squirrel', key='e_leg'),
        B('leg_l', -3.8, 0, -2.6, 2.4, 4.4, 4.6, 'squirrel', key='e_leg'),
        B('arm_r', 1.6, 5.2, -5, 1.8, 3.6, 2, 'squirrel', key='e_arm'),
        B('arm_l', -3.4, 5.2, -5, 1.8, 3.6, 2, 'squirrel', key='e_arm'),
    ]
    head = G('head', (0, 11.5, -2.6), rot=(-6, 0, 0), children=[
        B('skull', -2.8, 10.6, -7.4, 5.6, 5.4, 5.4, 'squirrel', key='e_skull', fx=fx_eye(1, 1, col=(16, 12, 10), glint=(246, 244, 238))),
        B('muzzle', -1.6, 10.6, -9, 3.2, 3, 1.8, 'squi_pale', key='e_muzzle'),
        B('nose', -.9, 12.6, -9.3, 1.8, 1.2, 1, 'fox_dark', key='e_nose'),
        B('ear_r', 1.8, 15.6, -5.6, 1.8, 3, 1.2, 'squirrel', key='e_ear'),
        B('ear_l', -3.6, 15.6, -5.6, 1.8, 3, 1.2, 'squirrel', key='e_ear'),
    ])
    # le panache : il part bas derriere, monte, puis revient vers l'avant au dessus de la tete
    tail = [
        B('tail_a', -1.8, 1.6, 3, 3.6, 6, 4.4, 'squirrel', key='e_tail1'),
        B('tail_b', -1.9, 7, 3.6, 3.8, 6.4, 4.6, 'squirrel', key='e_tail2'),
        B('tail_c', -1.9, 12.6, 2.4, 3.8, 6, 4.6, 'squirrel', key='e_tail3'),
        B('tail_d', -1.8, 17.4, -1, 3.6, 4.8, 4.6, 'squirrel', key='e_tail4'),
        B('tail_tip', -1.4, 17.8, -1.9, 2.8, 4, .9, 'squi_pale', key='e_tailtip'),
    ]
    return [G('body', (0, 0, 0), children=body + tail + [head])]


# ================================================================== canard
@model
def canard():
    B, G = mkT(0.86)
    body = [
        B('hull', -3.2, 4, -5, 6.4, 6, 11, 'duck_body', key='d_hull'),
        B('breast', -3, 4.6, -8, 6, 5.4, 4, 'duck_br', key='d_breast'),
        B('collar', -3.1, 8.4, -8.2, 6.2, 1.6, 2.2, 'duck_white', key='d_collar'),
        B('belly', -2.6, 3.6, -4, 5.2, 1.6, 9, 'duck_white', key='d_belly'),
        B('wing_r', 3, 5.2, -4.6, 1.2, 4.4, 9, 'duck_body', key='d_wing'),
        B('wing_l', -4.2, 5.2, -4.6, 1.2, 4.4, 9, 'duck_body', key='d_wing'),
        B('spec_r', 3.05, 5.6, -1.2, 1.2, 1.6, 4, 'duck_wing', key='d_spec'),
        B('spec_l', -4.25, 5.6, -1.2, 1.2, 1.6, 4, 'duck_wing', key='d_spec'),
        B('tail', -2, 7.4, 5.6, 4, 2, 4, 'duck_body', key='d_tail'),
        B('tail_up', -1, 9, 6.6, 2, 1.8, 2.4, 'duck_br', key='d_tailu'),
    ]
    legs = [
        B('leg_r', 1, 1.6, 0, 1.4, 2.6, 1.4, 'duck_bill', key='d_leg'),
        B('leg_l', -2.4, 1.6, 0, 1.4, 2.6, 1.4, 'duck_bill', key='d_leg'),
        B('foot_r', .6, 0, -1.4, 2.2, 1.2, 4, 'duck_bill', key='d_foot'),
        B('foot_l', -2.8, 0, -1.4, 2.2, 1.2, 4, 'duck_bill', key='d_foot'),
    ]
    head = G('head', (0, 10, -6), rot=(-6, 0, 0), children=[
        B('neck', -1.8, 9, -7.4, 3.6, 4, 3.6, 'duck_head', key='d_neck'),
        B('skull', -2.2, 12, -8.6, 4.4, 4.2, 5, 'duck_head', key='d_skull', fx=fx_eye(1, 1, col=(16, 14, 12), glint=(248, 248, 250))),
        B('bill', -1.9, 12.4, -12.4, 3.8, 1.8, 4.2, 'duck_bill', key='d_bill'),
        B('bill_tip', -1.9, 12.3, -12.9, 3.8, 1.6, .8, 'duck_bill', key='d_billt'),
    ])
    return [G('body', (0, 0, 0), children=body + legs + [head])]


# ================================================================== heron
@model
def heron():
    """d'apres les references : corps porte a l'horizontale, cou fin penche en avant,
    long bec orange, calotte noire a huppe, grandes remiges sombres"""
    B, G = mkT(1.0)
    legs = []
    for nm, s in (('leg_r', 1), ('leg_l', -1)):
        x = 1.5 * s
        legs.append(G(nm, (x, 14, 2), children=[
            B(f'{nm}_th', x - .6, 7, 1.6, 1.2, 7.4, 1.2, 'heron_leg', key='h_th'),
            B(f'{nm}_sh', x - .5, 0, 1.8, 1.0, 7.6, 1.0, 'heron_leg', key='h_sh'),
            B(f'{nm}_ft', x - .9, 0, -1.6, 1.8, .8, 4.2, 'heron_leg', key='h_ft')]))
    body = [
        B('hull', -2.8, 13.8, -5, 5.6, 6.4, 9, 'heron', key='h_hull'),
        B('back', -2.4, 19.4, -4.2, 4.8, 1.4, 8, 'heron', key='h_back'),
        B('breast', -2.4, 14.4, -7.4, 4.8, 5.6, 2.8, 'heron_pale', key='h_breast'),
        B('rump', -2.2, 14.6, 3.8, 4.4, 5, 3.6, 'heron', key='h_rump'),
        B('belly', -2.2, 13.2, -4, 4.4, 1.4, 8, 'heron_pale', key='h_belly'),
        # ailes repliees : le haut gris clair, les remiges sombres qui depassent vers l'arriere
        B('wing_r', 2.7, 15.4, -4.2, 1.0, 5, 9.4, 'heron', key='h_wing'),
        B('wing_l', -3.7, 15.4, -4.2, 1.0, 5, 9.4, 'heron', key='h_wing'),
        B('flight_r', 2.6, 14.2, 1, 1.2, 3.4, 7.5, 'heron_wing', key='h_flight'),
        B('flight_l', -3.8, 14.2, 1, 1.2, 3.4, 7.5, 'heron_wing', key='h_flight'),
        B('tail', -1.8, 15, 8, 3.6, 2, 4, 'heron_wing', key='h_tail'),
    ]
    head = G('head', (0, 29.5, -5.5), rot=(-48, 0, 0), children=[
        B('skull', -1.3, 29, -8.6, 2.6, 2.8, 4.2, 'heron_pale', key='h_skull',
          fx=fx_eye(1, 1, col=(16, 16, 18), glint=(252, 248, 240))),
        B('crown', -1.35, 31.3, -8.4, 2.7, 1.0, 3.6, 'heron_dark', key='h_crown'),
        B('plume', -.6, 31.1, -5.6, 1.2, .8, 4.4, 'heron_dark', key='h_plume'),
        B('bill', -.6, 29.6, -16, 1.2, 1.3, 7.6, 'heron_bill', key='h_bill'),
    ])
    neck = G('neck', (0, 19.5, -3.5), rot=(14, 0, 0), children=[
        B('neck_a', -1.3, 19.5, -4.6, 2.6, 6, 2.6, 'heron_pale', key='h_neck1'),
        B('neck_b', -1.15, 25, -5.4, 2.3, 5.4, 2.3, 'heron_pale', key='h_neck2'),
        head,
    ])
    return [G('body', (0, 0, 0), children=body + legs + [neck])]


# ================================================================== castor
@model
def castor():
    """trapu : un corps d'un bloc, une grosse tete carree a museau sombre et queue plate"""
    B, G = mkT(1.0)
    legs = []
    for nm, sgn, z in (('leg_fr', 1, -4), ('leg_fl', -1, -4), ('leg_br', 1, 5), ('leg_bl', -1, 5)):
        x = 3.2 * sgn
        legs.append(G(nm, (x, 4, z), children=[
            B(f'{nm}_up', x - 1.7, 0, z - 1.8, 3.4, 4.2, 3.6, 'beaver_d', key='b_leg'),
            B(f'{nm}_ft', x - 1.9, 0, z - 2.8, 3.8, 1.3, 5.2, 'beaver_d', key='b_foot')]))
    body = [
        B('torso', -4.5, 3.6, -7, 9, 9, 15, 'beaver', key='b_torso'),
        B('belly', -3.6, 3, -6, 7.2, 1.4, 13, 'beaver_d', key='b_belly'),
    ]
    tail = [
        B('tail_a', -3.4, 3.8, 7.6, 6.8, 1.6, 7, 'beaver_tl', key='b_tail1'),
        B('tail_b', -4, 3.9, 14, 8, 1.4, 7, 'beaver_tl', key='b_tail2'),
        B('tail_c', -3, 4, 20.5, 6, 1.2, 3, 'beaver_tl', key='b_tail3'),
    ]
    head = G('head', (0, 10, -6), rot=(-6, 0, 0), children=[
        B('skull', -3.6, 8.5, -13.5, 7.2, 7, 7, 'beaver', key='b_skull', fx=fx_eye(1, 2, col=(16, 12, 10))),
        B('muzzle', -2.4, 8.6, -15.6, 4.8, 4, 2.4, 'beaver_d', key='b_muzzle'),
        B('nose', -1.5, 11.2, -15.9, 3, 1.6, 1.2, 'fox_dark', key='b_nose'),
        B('teeth', -1.2, 8.2, -15.9, 2.4, 2, 1, 'tooth', key='b_teeth'),
        B('ear_r', 3, 14.8, -11, 1.8, 1.8, 1.4, 'beaver_d', key='b_ear'),
        B('ear_l', -4.8, 14.8, -11, 1.8, 1.8, 1.4, 'beaver_d', key='b_ear'),
    ])
    return [G('body', (0, 0, 0), children=body + legs + tail + [head])]


# ------------------------------------------------------------------ apercus et export
PSCALE = {'cerf': 9.0, 'chevreuil': 10.0, 'renard': 14.0, 'ecureuil': 20.0, 'canard': 20.0, 'heron': 12.0, 'castor': 14.0}
MODNAME = {'cerf': 'deer', 'chevreuil': 'roe_deer', 'renard': 'fox', 'ecureuil': 'squirrel',
           'canard': 'duck', 'heron': 'heron', 'castor': 'beaver'}


def gen():
    """ecrit geo + texture de chaque bete dans les assets du mod (rien n'est branche en jeu)"""
    import bbimport
    from genlib import wjson, save, A
    out = os.path.join(HERE, 'assets')
    for n, mod in MODNAME.items():
        beasts.build_model(n, out)
        src = os.path.join(out, n + '.bbmodel')
        wjson(f'{A}/geo/{mod}.json', bbimport.convert(src))
        save(bbimport.texture(src), f'{A}/textures/entity/{mod}.png')


if __name__ == '__main__':
    beasts.MODELS.update(MODELS)
    if sys.argv[1:2] == ['gen']:
        gen()
    else:
        names = [a for a in sys.argv[1:] if a in MODELS] or list(MODELS)
        for n in names:
            g, tex, W, H, k = beasts.build_model(n, '/tmp/fauna')
            print(n, 'cubes', k, 'texture', W, H)
            beasts.preview(n, g, tex, scale=PSCALE.get(n, 10.0), out='/tmp/fauna')
