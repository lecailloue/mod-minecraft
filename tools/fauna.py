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
    'deer':      dict(base=(132, 96, 62), n1=5, n2=7, tint=(1, .96, .88)),
    'deer_pale': dict(base=(196, 170, 136), n1=5, n2=6, tint=(1, .98, .92)),
    'roe':       dict(base=(158, 108, 62), n1=5, n2=7, tint=(1, .95, .85)),
    'roe_pale':  dict(base=(224, 212, 190), n1=4, n2=6, tint=(1, .99, .96)),
    'antler':    dict(base=(176, 156, 120), n1=6, n2=7, tint=(1, .96, .86)),
    'hoofd':     dict(base=(44, 38, 34), n1=4, n2=5, tint=(1, 1, 1.05)),
    'fox':       dict(base=(190, 96, 34), n1=6, n2=8, tint=(1, .92, .78)),
    'fox_pale':  dict(base=(236, 230, 220), n1=4, n2=5, tint=(1, .99, .97)),
    'fox_dark':  dict(base=(46, 38, 36), n1=4, n2=5, tint=(1, 1, 1.04)),
    'squirrel':  dict(base=(166, 86, 44), n1=6, n2=8, tint=(1, .93, .8)),
    'squi_pale': dict(base=(234, 226, 214), n1=4, n2=5, tint=(1, .99, .97)),
    'duck_head': dict(base=(34, 96, 66), n1=5, n2=7, tint=(.9, 1, .95)),
    'duck_body': dict(base=(150, 146, 138), n1=5, n2=7, tint=(1, 1, 1.02)),
    'duck_br':   dict(base=(122, 74, 52), n1=5, n2=6, tint=(1, .94, .86)),
    'duck_bill': dict(base=(214, 178, 62), n1=5, n2=6, tint=(1, .97, .78)),
    'duck_white': dict(base=(238, 234, 226), n1=3, n2=4, tint=(1, 1, 1)),
    'duck_wing': dict(base=(74, 86, 122), n1=5, n2=6, tint=(.95, .98, 1.06)),
    'heron':     dict(base=(168, 174, 182), n1=4, n2=6, tint=(1, 1, 1.05)),
    'heron_pale': dict(base=(232, 232, 230), n1=3, n2=4, tint=(1, 1, 1.01)),
    'heron_dark': dict(base=(42, 44, 52), n1=4, n2=5, tint=(1, 1, 1.08)),
    'heron_bill': dict(base=(216, 184, 70), n1=5, n2=6, tint=(1, .97, .76)),
    'beaver':    dict(base=(96, 64, 40), n1=5, n2=7, tint=(1, .95, .86)),
    'beaver_d':  dict(base=(66, 44, 30), n1=5, n2=6, tint=(1, .95, .88)),
    'beaver_tl': dict(base=(58, 46, 42), n1=7, n2=9, tint=(1, .98, .96)),
    'tooth':     dict(base=(226, 206, 134), n1=4, n2=5, tint=(1, .98, .8)),
})

# peinture de chaque matiere : fourrure, plume, corne, bec
tc.DECOR.update({
    'deer': d_bristle((178, 140, 98), (74, 52, 32), 4), 'deer_pale': d_bristle((226, 208, 180), (140, 118, 92), 4),
    'roe': d_bristle((198, 146, 92), (92, 60, 34), 4), 'roe_pale': d_bristle((242, 236, 222), (168, 152, 130), 4),
    'antler': d_antler, 'hoofd': d_hoof,
    'fox': d_bristle((232, 134, 52), (104, 46, 14), 3), 'fox_pale': d_bristle((250, 246, 240), (168, 160, 150), 3),
    'fox_dark': d_bristle((80, 68, 64), (18, 14, 14), 3),
    'squirrel': d_bristle((210, 118, 62), (86, 42, 20), 3), 'squi_pale': d_bristle((248, 242, 234), (176, 166, 154), 3),
    'duck_head': d_feather((56, 134, 96), (14, 56, 38), 2), 'duck_body': d_feather((186, 182, 174), (92, 88, 82), 3),
    'duck_br': d_feather((156, 102, 72), (70, 40, 26), 3), 'duck_bill': d_ebeak, 'duck_white': d_feather((250, 248, 244), (190, 186, 178), 3),
    'duck_wing': d_feather((104, 118, 158), (36, 44, 72), 3),
    'heron': d_feather((198, 204, 212), (104, 110, 120), 3), 'heron_pale': d_feather((248, 248, 246), (190, 190, 188), 3),
    'heron_dark': d_black, 'heron_bill': d_ebeak,
    'beaver': d_bristle((132, 92, 60), (48, 30, 18), 3), 'beaver_d': d_bristle((96, 66, 44), (34, 22, 14), 3),
    'beaver_tl': d_hide(.26, 2, .5), 'tooth': d_antler,
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
    B, G = mkT(1.0)
    legs = quad_legs(B, G, 'deer', 'hoofd', 20, 2.4, 3.4, -7, 8, 3.6, ('cl_u', 'cl_l', 'cl_h'))
    body = [
        B('chest', -4.6, 17, -11, 9.2, 12, 9, 'deer', key='c_chest'),
        B('torso', -4.2, 18, -3, 8.4, 11, 14, 'deer', key='c_torso'),
        B('rump', -4.4, 18.5, 10, 8.8, 10.5, 5, 'deer', key='c_rump'),
        B('belly', -3.2, 16.6, -6, 6.4, 2, 17, 'deer_pale', key='c_belly'),
        B('tail', -1.4, 26, 14, 2.8, 4.5, 2.4, 'deer_pale', key='c_tail'),
    ]
    head = G('head', (0, 34, -10), rot=(22, 0, 0), children=[
        B('skull', -2.8, 31, -15.5, 5.6, 6, 7.5, 'deer', key='c_skull', fx=fx_eye(1, 2, col=(26, 18, 14))),
        B('muzzle', -2, 29.8, -20, 4, 4.2, 5, 'deer', key='c_muzzle'),
        B('nose', -2.1, 29.6, -21, 4.2, 2.4, 1.6, 'hoofd', key='c_nose'),
        B('jaw', -1.7, 28.6, -19.5, 3.4, 1.6, 4.5, 'deer_pale', key='c_jaw'),
        G('ear_r', (2.6, 35, -11), rot=(16, 0, -58), children=[
            B('ear_r_o', 2.4, 35, -11.7, 3.2, 6.5, 1.2, 'deer', key='c_ear'),
            B('ear_r_i', 3.0, 35.7, -12.2, 2.0, 5.0, .6, 'deer_pale', key='c_earin')]),
        G('ear_l', (-2.6, 35, -11), rot=(16, 0, 58), children=[
            B('ear_l_o', -5.6, 35, -11.7, 3.2, 6.5, 1.2, 'deer', key='c_ear'),
            B('ear_l_i', -5.0, 35.7, -12.2, 2.0, 5.0, .6, 'deer_pale', key='c_earin')]),
        antlers(B, G, 1, 'c_ant', 36, -12), antlers(B, G, -1, 'c_ant', 36, -12),
    ])
    neck = G('neck', (0, 24, -8), rot=(-34, 0, 0), children=[
        B('neck_a', -2.9, 24, -12, 5.8, 11, 6, 'deer', key='c_neck'),
        B('neck_b', -2.6, 32, -12.6, 5.2, 5, 5.4, 'deer', key='c_neck2'),
        head,
    ])
    return [G('body', (0, 0, 0), children=body + legs + [neck])]


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
    B, G = mkT(0.62)
    body = [
        B('torso', -2.4, 5, -3, 4.8, 7, 7, 'squirrel', key='e_torso'),
        B('chest', -2.2, 6, -6, 4.4, 5.4, 4, 'squirrel', key='e_chest'),
        B('belly', -1.6, 5.2, -5.2, 3.2, 3.4, 7, 'squi_pale', key='e_belly'),
    ]
    legs = [
        B('arm_r', 1.4, 3.4, -5.6, 1.8, 4, 2.2, 'squirrel', key='e_arm'),
        B('arm_l', -3.2, 3.4, -5.6, 1.8, 4, 2.2, 'squirrel', key='e_arm'),
        B('leg_r', 1.6, 0, -1.4, 2.2, 5, 3.4, 'squirrel', key='e_leg'),
        B('leg_l', -3.8, 0, -1.4, 2.2, 5, 3.4, 'squirrel', key='e_leg'),
        B('foot_r', 1.6, 0, -2.6, 2.2, 1.4, 3.4, 'squi_pale', key='e_foot'),
        B('foot_l', -3.8, 0, -2.6, 2.2, 1.4, 3.4, 'squi_pale', key='e_foot'),
    ]
    head = G('head', (0, 11, -4), rot=(-8, 0, 0), children=[
        B('skull', -2.2, 10, -8.4, 4.4, 4.4, 5, 'squirrel', key='e_skull', fx=fx_eye(1, 1, col=(18, 14, 12), glint=(240, 240, 250))),
        B('snout', -1.2, 10.2, -10.2, 2.4, 2.4, 2, 'squi_pale', key='e_snout'),
        B('nose', -.8, 11.4, -10.8, 1.6, 1.2, 1, 'fox_dark', key='e_nose'),
        G('ear_r', (1.6, 14, -6), rot=(-6, 0, -10), children=[
            B('ear_r_o', 1.0, 14, -6.6, 1.8, 3.2, 1.0, 'squirrel', key='e_ear'),
            B('ear_r_t', 1.2, 16.8, -6.5, 1.4, 1.6, .8, 'squirrel', key='e_eart')]),
        G('ear_l', (-1.6, 14, -6), rot=(-6, 0, 10), children=[
            B('ear_l_o', -2.8, 14, -6.6, 1.8, 3.2, 1.0, 'squirrel', key='e_ear'),
            B('ear_l_t', -2.6, 16.8, -6.5, 1.4, 1.6, .8, 'squirrel', key='e_eart')]),
    ])
    tail = [
        B('tail_a', -1.9, 4.2, 4.0, 3.8, 5.6, 3.8, 'squirrel', key='e_tail1'),
        B('tail_b', -2.2, 9.2, 3.8, 4.4, 6.4, 4.0, 'squirrel', key='e_tail2'),
        B('tail_c', -2.4, 14.8, 3.0, 4.8, 5.6, 4.2, 'squirrel', key='e_tail3'),
        B('tail_d', -2.1, 19.2, 1.6, 4.2, 4.2, 4, 'squirrel', key='e_tail4'),
    ]
    return [G('body', (0, 0, 0), children=body + legs + tail + [head])]


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
    B, G = mkT(0.95)
    legs = []
    for nm, s in (('leg_r', 1), ('leg_l', -1)):
        x = 1.8 * s
        legs.append(G(nm, (x, 14, 1), children=[
            B(f'{nm}_th', x - 1, 7, .2, 2, 7.4, 2, 'heron_bill', key='h_th'),
            B(f'{nm}_sh', x - .7, 0, .4, 1.4, 7.4, 1.4, 'heron_bill', key='h_sh'),
            B(f'{nm}_ft', x - 1.2, 0, -2.6, 2.4, 1, 5, 'heron_bill', key='h_ft')]))
    body = [
        B('hull', -3, 14, -4, 6, 7.4, 10, 'heron', key='h_hull'),
        B('breast', -2.6, 14.6, -6.6, 5.2, 6.4, 3, 'heron_pale', key='h_breast'),
        B('wing_r', 2.8, 15, -3.6, 1.2, 5.6, 9.4, 'heron', key='h_wing'),
        B('wing_l', -4, 15, -3.6, 1.2, 5.6, 9.4, 'heron', key='h_wing'),
        B('tail', -1.8, 15.6, 5.6, 3.6, 2.4, 4.4, 'heron_dark', key='h_tail'),
    ]
    neck = [
        B('neck_a', -1.6, 20, -2.6, 3.2, 6, 3.2, 'heron_pale', key='h_neck1'),
        B('neck_b', -1.5, 25.6, -4, 3, 5.2, 3, 'heron_pale', key='h_neck2'),
    ]
    head = [
        B('skull', -1.7, 30.4, -7.4, 3.4, 3.6, 5, 'heron_pale', key='h_skull', fx=fx_eye(1, 1, col=(18, 16, 14), glint=(250, 230, 120))),
        B('crest', -.8, 33.4, -4.4, 1.6, 1.2, 5.4, 'heron_dark', key='h_crest'),
        B('stripe_r', 1.2, 32.6, -7, 1.0, 1.2, 4.2, 'heron_dark', key='h_stripe'),
        B('stripe_l', -2.2, 32.6, -7, 1.0, 1.2, 4.2, 'heron_dark', key='h_stripe'),
        B('bill', -.9, 30.9, -14, 1.8, 1.8, 7, 'heron_bill', key='h_bill'),
    ]
    return [G('body', (0, 0, 0), children=body + legs + neck + head)]


# ================================================================== castor
@model
def castor():
    B, G = mkT(0.9)
    legs = []
    for nm, s, z, h in (('leg_fr', 1, -4.5, 3.4), ('leg_fl', -1, -4.5, 3.4), ('leg_br', 1, 4.5, 4.2), ('leg_bl', -1, 4.5, 4.2)):
        x = 2.8 * s
        legs.append(G(nm, (x, h, z), children=[
            B(f'{nm}_up', x - 1.6, 0, z - 1.8, 3.2, h, 3.6, 'beaver_d', key='b_leg' + ('b' if h > 4 else 'f')),
            B(f'{nm}_ft', x - 1.8, 0, z - 2.6, 3.6, 1.2, 5, 'beaver_d', key='b_foot')]))
    body = [
        B('rump', -4, 3, 1, 8, 8.4, 8, 'beaver', key='b_rump'),
        B('torso', -3.6, 3.2, -6, 7.2, 7.6, 8, 'beaver', key='b_torso'),
        B('belly', -3, 2.6, -5, 6, 1.8, 11, 'beaver_d', key='b_belly'),
    ]
    tail = G('tail', (0, 4, 9), rot=(8, 0, 0), children=[
        B('tail_a', -3, 3.2, 9, 6, 1.6, 6, 'beaver_tl', key='b_tail1'),
        B('tail_b', -3.6, 3.3, 14, 7.2, 1.4, 6, 'beaver_tl', key='b_tail2'),
        B('tail_c', -2.6, 3.4, 19.5, 5.2, 1.2, 3, 'beaver_tl', key='b_tail3'),
    ])
    head = G('head', (0, 9, -7), rot=(4, 0, 0), children=[
        B('skull', -3, 7.6, -12, 6, 5.6, 5.4, 'beaver', key='b_skull', fx=fx_eye(1, 1, col=(18, 14, 12), glint=(240, 236, 226))),
        B('snout', -1.8, 7.6, -14.2, 3.6, 3.4, 2.6, 'beaver_d', key='b_snout'),
        B('nose', -1.2, 9.6, -14.8, 2.4, 1.4, 1.2, 'fox_dark', key='b_nose'),
        B('teeth', -1, 7.2, -14.6, 2, 1.8, 1, 'tooth', key='b_teeth'),
        B('ear_r', 2.4, 11.4, -9.4, 1.6, 1.6, 1.2, 'beaver_d', key='b_ear'),
        B('ear_l', -4, 11.4, -9.4, 1.6, 1.6, 1.2, 'beaver_d', key='b_ear'),
    ])
    return [G('body', (0, 0, 0), children=body + legs + [tail, head])]


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
