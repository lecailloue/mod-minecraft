"""Soldats spectraux de la Hautemarche : 3 alternatives au soldat classique (hoplite a lance et bouclier, silhouette a capuche, guerrier-crane a hache)
et le Roi des morts (couronne, cape royale, grande epee), qui a 5 % de chances d'apparaitre.
Memes conventions que orcs.py : y vers le haut, sol y=0, avant = -z, cote -x = 'left'. Genere .bbmodel + texture + texture lumineuse (yeux)."""
import numpy as np
import trollcore as T
from trollcore import *
import orcs as O
from orcs import C, X, chain, spike, translate, scale_about, scale_all, unique_names

MAT = {
    's_cloth': dict(base=(62, 108, 100), n1=8, n2=8, tint=(.9, 1, 1)),
    's_cloth2': dict(base=(34, 62, 74), n1=7, n2=8, tint=(.9, 1, 1.1)),
    's_rag': dict(base=(96, 138, 120), n1=9, n2=8, tint=(.95, 1, 1)),
    's_metal': dict(base=(112, 142, 140), n1=6, n2=7, tint=(.95, 1, 1)),
    's_rust': dict(base=(82, 104, 96), n1=8, n2=9, tint=(.95, 1, .95)),
    's_blade': dict(base=(170, 202, 198), n1=7, n2=6, tint=(.95, 1, 1)),
    's_bone': dict(base=(208, 224, 208), n1=7, n2=6, tint=(1, 1, .95)),
    's_wood': dict(base=(72, 80, 64), n1=6, n2=6, tint=(.95, 1, .9)),
    's_gold': dict(base=(212, 176, 74), n1=8, n2=8, tint=(1, .95, .7)),
    's_gem': dict(base=(96, 236, 204), n1=5, n2=5, tint=(.9, 1, 1)),
    's_dark': dict(base=(12, 24, 22), n1=3, n2=4, tint=(1, 1, 1)),
}


def skin_mat(hexcol):
    name = 's_sk_' + hexcol
    if name not in T.MATS:
        c = tuple(int(hexcol[i:i + 2], 16) for i in (0, 2, 4))
        T.MATS[name] = dict(base=c, n1=7, n2=8, tint=(1, 1, 1))
        T.DECOR[name] = d_ghost
    return name


def d_ghost(arr, face, rng):
    h, w, _ = arr.shape
    m = arr.reshape(-1, 3).mean(0)
    for _ in range(int(h * w / 26)):
        blob(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 1, tuple(m * .7), .4, rng)
    for _ in range(int(h * w / 40)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), tuple(np.clip(m * 1.25 + 10, 0, 255)), .5)
    if h >= 4 and w >= 4 and rng.random() < .5:
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(3, h + 1)), tuple(m * .66), .5)


def d_plain(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.06, .88, h)[:, None, None]


T.MATS.update(MAT)
T.DECOR.update({'s_cloth': O.d_cloth2, 's_cloth2': O.d_cloth2, 's_rag': O.d_cloth2, 's_metal': lambda a, f, r: O.d_iron2(a, f, r, rust=.2),
                's_rust': lambda a, f, r: O.d_iron2(a, f, r, rust=.8), 's_blade': O.d_blade2, 's_bone': O.d_tooth, 's_wood': O.d_wood,
                's_gold': d_plain, 's_gem': d_plain, 's_dark': d_plain})


# ---------------------------------------------------------------- visages : orbites sombres, yeux lumineux
def fx_eyes(col):
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape                                   # 9 x 9
        for (x0, y0) in ((1, 3), (w - 3, 3)):
            for dx in range(2):
                for dy in range(3):
                    put(n, x0 + dx, y0 + dy, (6, 14, 12), .95)
            for dy in (1, 2):
                glow('north', x0 + 0, y0 + dy, col)
                glow('north', x0 + 1, y0 + dy, col)
                put(n, x0, y0 + dy, col, 1)
                put(n, x0 + 1, y0 + dy, col, 1)
        put(n, w // 2, 5, (8, 16, 14), .9)                  # fente du nez
        put(n, w // 2, 6, (8, 16, 14), .9)
    return f


def fx_skull(col):
    base = fx_eyes(col)

    def f(F, rng, glow):
        base(F, rng, glow)
        n = F['north']
        h, w, _ = n.shape
        for x in range(w):                                   # rangee de dents
            put(n, x, h - 2, (20, 30, 26), .9)
            if x % 2 == 0:
                put(n, x, h - 3, (236, 244, 230), 1)
    return f


def fx_hood(col):
    """visage cache sous la capuche : seulement deux points lumineux"""
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        n[:, :] = (8, 16, 15)
        for x0 in (1, w - 4):
            for dx in range(3):
                for y in (3, 4, 5):
                    glow('north', x0 + dx, y, col)
                    put(n, x0 + dx, y, col, 1)
    return f


# ---------------------------------------------------------------- corps
def body_parts(sk, eye, head_mat, head_fx, ax=11, rib=False):
    legs = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        k = [C('thigh_' + n, X(sg, 5, 6), 19, -3, 6, 15, 6, sk, key='s_thigh'),
             C('shin_' + n, X(sg, 5, 5), 3, -2.5, 5, 17, 5, sk, key='s_shin'),
             C('foot_' + n, X(sg, 5, 6), 0, -5, 6, 3, 9, sk, key='s_foot')]
        legs[sg] = Group('leg_' + n, (sg * 5, 34, 0), (0, 0, 0), k)
    pelvis = Group('pelvis', (0, 34, 0), children=[C('pelvis', -6, 31, -3.5, 12, 5, 7, sk, key='s_pelvis')])
    bk = [C('chest', -8, 41, -4.5, 16, 14, 9, sk, key='s_chest'),
          C('abd', -6, 34, -4, 12, 8, 8, sk, key='s_abd'),
          C('neck', -3, 55, -3, 6, 3, 6, sk, key='s_neck')]
    if rib:
        for i in range(4):
            bk.append(C(f'rib_{i}', -7, 43 + i * 3, -5.2, 14, 1, 1, 's_bone', key='s_rib'))
        bk.append(C('spine', -.5, 36, -5.2, 1, 17, 1, 's_bone', key='s_spine'))
    hk = [C('skull', -4.5, 58, -5.5, 9, 9, 9, head_mat, key='s_skull' + eye.hex() + head_mat, fx=head_fx),
          C('jaw', -3.5, 55.5, -4.5, 7, 3, 7, head_mat, key='s_jawm' + head_mat)]
    arms, fores = {}, {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        cx = ax + .5
        arms[sg] = [C('shoulder_' + n, X(sg, cx, 5), 50, -2.5, 5, 5, 5, sk, key='s_sh'),
                    C('uarm_' + n, X(sg, cx, 4), 41, -2, 4, 10, 4, sk, key='s_ua')]
        fores[sg] = [C('farm_' + n, X(sg, cx, 4), 30, -2, 4, 11, 4, sk, key='s_fa'),
                     C('hand_' + n, X(sg, cx, 3), 27, -1.5, 3, 3, 3, 's_bone', key='s_hand')]
    return legs, pelvis, bk, hk, arms, fores


def tunic(bk, mat='s_cloth', n=6, belt='s_cloth2', rag=1.0):
    """tunique en loques : ceinture et pans de tissu dechires, de longueurs inegales"""
    bk.append(C('belt', -7, 33, -4.7, 14, 2, 9, belt, key='s_belt'))
    for i in range(n):
        x = -6.5 + i * (13 / n)
        ln = int((11 + (i * 7 % 5) * 2) * rag)
        bk.append(C(f'rag_{i}', x, 33 - ln, -5 - (i % 2) * .6, 13 / n + .5, ln, 1, mat, key=f's_rag{ln}', rot=(0, 0, (-3, 4, -2, 5)[i % 4])))
    for i in range(3):
        bk.append(C(f'rag_back_{i}', -5 + i * 4, 22, 4.6, 4, 11, 1, mat, key='s_ragb'))


def cape(color_mat, trim=None, w=16, top=54, wide=1.0):
    """cape en pans articules (cape > cape_c > cape_d > cape_e), animee par GeoModel"""
    z = 5.2

    def pan(name, ytop, hgt, ww, nxt=None, tails=False):
        kids = [C(name + '_b', -ww / 2, ytop - hgt, z, ww, hgt, 2, color_mat, key='sc_' + name)]
        if tails:
            for i, (x, tw, th) in enumerate([(-ww / 2, 5, 5), (-ww / 2 + 5, 5, 8), (-ww / 2 + 10, 5, 4), (ww / 2 - 15, 5, 7), (ww / 2 - 10, 5, 9), (ww / 2 - 5, 5, 5)]):
                kids.append(C(f'{name}_tail_{i}', x, ytop - hgt - th, z, tw, th, 2, color_mat, key=f'sc_t{i}'))
        if trim:
            kids.append(C(name + '_trim', -ww / 2, ytop - 1, z - .1, ww, 1, 2, trim, key='sc_trim' + name))
        if nxt:
            kids.append(nxt)
        return Group(name, (0, ytop, z + 1), rot=(2, 0, 0), children=kids)
    e = pan('cape_e', 16, 16, int(w * wide) - 2, tails=True)
    d = pan('cape_d', 30, 14, int(w * wide) - 1, e)
    c = pan('cape_c', 44, 14, int(w * wide), d)
    return Group('cape', (0, top, z + 1), rot=(3, 0, 0), children=[
        C('cape_top', -int(w * wide) / 2 + 1, 44, z, int(w * wide) - 2, 10, 2, color_mat, key='sc_top'),
        C('cape_collar', -7, top - 2, 3.5, 14, 4, 3, color_mat, key='sc_collar'), c])


def finish(legs, pelvis, bk, hk, arms, fores, weapon, wsg=1, lean=-8, hand_rot=(-50, 0, -6), head_f=1.12, ax=11, capes=None, shield=None):
    scale_about(hk, head_f, (0, 57, -2))
    head = Group('head', (0, 57, -2), (-lean * .8, 0, 0), hk)
    cx = ax + .5
    farms = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        farms[sg] = Group('forearm_' + n, (sg * cx, 41, 0), (-16, 0, 0), fores[sg])
    if weapon:
        hp = (wsg * cx, 28.5, -2.2)
        translate(weapon, *hp)
        farms[wsg].children.append(Group('weapon', hp, hand_rot, weapon))
    if shield:
        farms[-wsg].children.extend(shield)
    arm_groups = [Group('arm_' + ('left' if sg < 0 else 'right'), (sg * ax, 54, 0), (-4, 0, sg * -3), arms[sg] + [farms[sg]]) for sg in (-1, 1)]
    kids = bk + [head] + arm_groups + ([capes] if capes else [])
    body = Group('body', (0, 34, 0), (lean, 0, 0), kids)
    groups = [legs[-1], legs[1], pelvis, body]
    unique_names(groups)
    return groups


# ---------------------------------------------------------------- armes
def w_sword(length=24, w=3, mat='s_blade'):
    return [C('grip', -1, -3, -1, 2, 7, 2, 's_wood', key='sw_grip'),
            C('pommel', -1.5, -4.5, -1.5, 3, 1, 3, 's_rust', key='sw_pom'),
            C('guard', -3.5, 4, -1, 7, 1, 2, 's_rust', key=f'sw_guard{w}'),
            C('blade', -.5, 5, -w / 2, 1, length, w, mat, key=f'sw_blade{length}'),
            C('tip', -.5, 5 + length, -w / 2 + .5, 1, 3, max(1, w - 1), mat, key=f'sw_tip{length}')]


def w_spear():
    return [C('shaft', -1, -18, -1, 2, 50, 2, 's_wood', key='sp_shaft'),
            C('head', -.5, 32, -2, 1, 9, 4, 's_blade', key='sp_head'),
            C('head_tip', -.5, 41, -1, 1, 4, 2, 's_blade', key='sp_tip'),
            C('collar', -1.5, 30, -1.5, 3, 2, 3, 's_rust', key='sp_col')]


def w_axe():
    return [C('shaft', -1, -14, -1, 2, 44, 2, 's_wood', key='ax_shaft'),
            C('head', -.5, 25, -9, 1, 10, 9, 's_rust', key='ax_head'),
            C('beard', -.5, 22, -7, 1, 3, 6, 's_rust', key='ax_beard'),
            C('cap', -1.5, 28, -1.5, 3, 5, 3, 's_metal', key='ax_cap'),
            C('spike', -.5, 28, 1, 1, 3, 4, 's_metal', key='ax_spike')]


def shield(sg=-1, ax=11):
    """bouclier rond au bras gauche"""
    x = sg * (ax + .5) - 5.5 - 2
    return [C('shield', x, 23, -9, 12, 13, 2, 's_rust', key='sh_disc'),
            C('shield_rim', x - 1, 26, -9.2, 14, 8, 1, 's_metal', key='sh_rim'),
            C('shield_boss', x + 4.5, 28, -10.2, 3, 3, 1, 's_metal', key='sh_boss')]


# ---------------------------------------------------------------- les modeles
TEAL = np.array((120, 255, 220))
ICE = np.array((200, 235, 255))
GOLD = np.array((255, 230, 140))


def col(c):
    return O.col(c)


def spectre_lance(skin='6fc4b4'):
    """hoplite : casque a crete et a joues, bouclier rond rouille, lance ; tunique en loques"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(TEAL), sk, fx_eyes(col(TEAL)))
    tunic(bk)
    bk.append(C('strap_a', -1.5, 40, -5.1, 2, 15, 1, 's_cloth2', key='s_strapa', rot=(0, 0, -30)))
    hk += [C('helm_dome', -5.5, 65, -6.5, 11, 4, 11, 's_rust', key='s_hdome'),
           C('helm_back', -5.5, 57, 3, 11, 7, 2, 's_rust', key='s_hback'),
           C('helm_nasal', -.5, 58, -6.4, 1, 6, 1, 's_metal', key='s_hnasal'),
           C('crest', -.5, 69, -7, 1, 5, 12, 's_cloth2', key='s_crest')]
    for sg in (-1, 1):
        hk.append(C('cheek_' + ('l' if sg < 0 else 'r'), X(sg, 5, 2), 57, -6, 2, 7, 7, 's_rust', key='s_cheek'))
    bk.append(C('paul_l', -14, 52, -3.5, 5, 3, 7, 's_rust', key='s_paul'))
    return finish(legs, pel, bk, hk, arms, fores, w_spear(), hand_rot=(-8, 0, -6), shield=shield())


def spectre_capuche(skin='8ab8d0'):
    """silhouette a capuche : visage noir, deux yeux, cape deteriorée, longue epee"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(ICE), 's_dark', fx_hood(col(ICE)))
    tunic(bk, 's_cloth2', 7, 's_cloth', rag=1.7)
    bk.append(C('robe', -8.5, 36, -5, 17, 19, 10, 's_cloth2', key='s_robe'))
    hk += [C('hood_top', -5.5, 62, -6.5, 11, 6, 11, 's_cloth2', key='s_htop'),
           C('hood_back', -5.5, 54, 3, 11, 10, 3, 's_cloth2', key='s_hback'),
           C('hood_peak', -5.5, 63, -8, 11, 3, 2, 's_cloth2', key='s_hpeak'),
           C('hood_point', -1, 66, 3, 2, 6, 3, 's_cloth2', key='s_hpoint', rot=(30, 0, 0))]
    for sg in (-1, 1):
        hk.append(C('hood_side_' + ('l' if sg < 0 else 'r'), X(sg, 5.5, 2), 55, -6, 2, 10, 9, 's_cloth2', key='s_hside'))
    bk.append(C('mantle', -9.5, 50, -4.5, 19, 5, 9, 's_cloth2', key='s_mantle'))
    bk.append(C('mantle_l', -12, 47, -4, 3, 5, 8, 's_cloth2', key='s_mantle2', rot=(0, 0, 12)))
    bk.append(C('mantle_r', 9, 47, -4, 3, 5, 8, 's_cloth2', key='s_mantle2', rot=(0, 0, -12)))
    return finish(legs, pel, bk, hk, arms, fores, w_sword(26, 3), hand_rot=(-30, 0, -6), capes=cape('s_cloth2', 's_cloth', 15))


def spectre_crane(skin='cfeede'):
    """guerrier-crane : visage de squelette, cotte de mailles en lambeaux, epaulieres rouillees, cotes apparentes, hache"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(TEAL), 's_bone', fx_skull(col(TEAL)), rib=True)
    bk.append(C('belt', -7, 33, -4.7, 14, 2, 9, 's_rust', key='s_belt2'))
    for i in range(7):
        ln = 9 + (i * 5 % 4) * 2
        bk.append(C(f'mail_{i}', -6.5 + i * 1.9, 33 - ln, -5, 2, ln, 1, 's_rust', key=f's_mail{ln}', rot=(0, 0, (-4, 3, -2, 4)[i % 4])))
    for i in range(3):
        bk.append(C(f'mail_back_{i}', -5 + i * 4, 22, 4.6, 4, 11, 1, 's_rust', key='s_mailb'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('paul_' + n, X(sg, 12.5, 7), 53, -4, 7, 3, 8, 's_rust', key='s_paulr', rot=(0, 0, sg * -10), pivot=(sg * 12.5, 55, 0)))
        bk.append(C('paul2_' + n, X(sg, 13, 6), 50, -3.5, 6, 3, 7, 's_metal', key='s_paulm', rot=(0, 0, sg * -14), pivot=(sg * 13, 52, 0)))
    hk += [C('helm_band', -5.5, 64, -6.5, 11, 2, 11, 's_rust', key='s_hband'),
           C('helm_back', -5.5, 59, 3, 11, 6, 2, 's_rust', key='s_hback2')]
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        hk.append(spike('horn_' + n, sg * 5.5, 65, -2, 6, 2, (0, 0, sg * -40), mat='s_bone'))
    return finish(legs, pel, bk, hk, arms, fores, w_axe(), hand_rot=(-44, 0, -6))


def spectre_roi(skin='bff2d4'):
    """Roi des morts : grande couronne d'or, cape royale doublee d'or, plastron et epaulieres, grande epee ; plus grand que les autres"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(GOLD), 's_bone', fx_skull(col(GOLD)), rib=False)
    bk += [C('plate', -8.5, 41, -5.4, 17, 13, 2, 's_metal', key='k_plate'),
           C('plate_trim', -8.5, 41, -5.6, 17, 2, 1, 's_gold', key='k_ptrim'),
           C('plate_gem', -1.5, 47, -6.2, 3, 3, 1, 's_gem', key='k_pgem'),
           C('belt', -7, 33, -4.7, 14, 3, 9, 's_gold', key='k_belt')]
    tunic(bk, 's_cloth2', 6, 's_cloth2')
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('paul_' + n, X(sg, 13, 8), 53, -4.5, 8, 3, 9, 's_metal', key='k_paul', rot=(0, 0, sg * -10), pivot=(sg * 13, 55, 0)))
        bk.append(C('paul_trim_' + n, X(sg, 13, 8), 56, -4.5, 8, 1, 9, 's_gold', key='k_pault', rot=(0, 0, sg * -10), pivot=(sg * 13, 55, 0)))
        bk.append(spike('paul_spike_' + n, sg * 13.5, 56, -.5, 5, 2, (0, 0, sg * -12), mat='s_gold'))
    # couronne : bandeau, cinq pointes, gemme centrale
    hk += [C('crown_band', -5.5, 66, -6.5, 11, 2, 11, 's_gold', key='k_cband'),
           C('crown_gem', -1, 66.5, -7.2, 2, 1, 1, 's_gem', key='k_cgem', fx=None)]
    for i, (x, z, h) in enumerate([(-4.5, -6, 4), (-0.5, -6.4, 6), (3.5, -6, 4), (-4.5, 2.2, 4), (3.5, 2.2, 4), (-0.5, 2.6, 5), (-4.7, -2, 3), (3.7, -2, 3)]):
        hk.append(C(f'crown_{i}', x, 68, z, 2, h, 2, 's_gold', key=f'k_cs{h}'))
    # barbe de lambeaux sous le menton
    for i in range(3):
        hk.append(C(f'beard_{i}', -3 + i * 2.6, 49 - i % 2 * 2, -5 - (i % 2) * .5, 2, 7 + i % 2 * 3, 1, 's_rag', key=f'k_beard{i % 2}'))
    return finish(legs, pel, bk, hk, arms, fores, w_sword(34, 5), hand_rot=(-24, 0, -6), head_f=1.16,
                  capes=cape('s_cloth2', 's_gold', 20, wide=1.0))


MODELS = {
    'spectre_lance': (spectre_lance, 1.0),
    'spectre_capuche': (spectre_capuche, 1.0),
    'spectre_crane': (spectre_crane, 1.0),
    'spectre_roi': (spectre_roi, 1.22),
}


def build(name):
    fn, f = MODELS[name]
    T._N[0] = 0
    groups = fn()
    if f != 1.0:
        scale_all(groups, f)
    normalize(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    return groups, cubes, W, H, tex, glow


def sheet(path, names=None, views=(0, 40, 180)):
    from PIL import Image
    rows = []
    for name in names or MODELS:
        fn, f = MODELS[name]
        groups, cubes, W, H, tex, glow = build(name)
        sc = 5.6 / f
        rows.append(np.concatenate([render(groups, tex, y, pitch=-8, W=330, H=520, scale=sc) for y in views], axis=1))
    Image.fromarray(np.concatenate(rows, axis=0)).save(path)


if __name__ == '__main__':
    sheet('/tmp/spectres_a.png', ['spectre_lance', 'spectre_capuche'])
    sheet('/tmp/spectres_b.png', ['spectre_crane', 'spectre_roi'])
