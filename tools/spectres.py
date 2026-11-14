"""Soldats spectraux de la Hautemarche : 3 alternatives au soldat classique (hoplite a lance et bouclier, silhouette a capuche, guerrier-crane a hache)
et le Roi des morts (couronne, cape royale, grande epee), qui a 5 % de chances d'apparaitre.
Memes conventions que orcs.py : y vers le haut, sol y=0, avant = -z, cote -x = 'left'. Genere .bbmodel + texture + texture lumineuse (yeux)."""
import numpy as np
import trollcore as T
from trollcore import *
import orcs as O
from orcs import C, X, chain, spike, translate, scale_about, scale_all, unique_names

MAT = {
    # tout est vert spectral, comme le soldat d'origine : peau, mailles, tissu
    's_mail': dict(base=(88, 146, 114), n1=6, n2=7, tint=(.92, 1, .96)),
    's_cloth': dict(base=(58, 124, 94), n1=8, n2=8, tint=(.9, 1, .95)),
    's_cloth2': dict(base=(36, 88, 70), n1=7, n2=8, tint=(.9, 1, .95)),
    's_rag': dict(base=(98, 160, 126), n1=9, n2=8, tint=(.95, 1, .98)),
    's_metal': dict(base=(104, 158, 134), n1=6, n2=7, tint=(.92, 1, .98)),
    's_rust': dict(base=(72, 120, 98), n1=7, n2=8, tint=(.92, 1, .96)),
    's_blade': dict(base=(192, 238, 218), n1=7, n2=6, tint=(.95, 1, 1)),
    's_bone': dict(base=(176, 226, 200), n1=7, n2=6, tint=(.95, 1, .98)),
    's_bone2': dict(base=(222, 226, 208), n1=7, n2=7, tint=(1, 1, .96)),
    's_wood': dict(base=(76, 100, 72), n1=6, n2=6, tint=(.92, 1, .92)),
    's_gold': dict(base=(206, 188, 96), n1=8, n2=8, tint=(.95, 1, .8)),
    's_gem': dict(base=(150, 255, 225), n1=5, n2=5, tint=(.9, 1, 1)),
    's_dark': dict(base=(14, 34, 28), n1=3, n2=4, tint=(1, 1, 1)),
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
T.DECOR.update({'s_mail': O.d_mail2, 's_cloth': O.d_cloth2, 's_cloth2': O.d_cloth2, 's_rag': O.d_cloth2, 's_metal': lambda a, f, r: O.d_iron2(a, f, r, rust=.2),
                's_rust': lambda a, f, r: O.d_iron2(a, f, r, rust=.6), 's_blade': O.d_blade2, 's_bone': O.d_tooth, 's_bone2': O.d_tooth, 's_wood': O.d_wood,
                's_gold': d_plain, 's_gem': d_plain, 's_dark': d_plain})


# ---------------------------------------------------------------- visages : orbites sombres, yeux lumineux
def fx_eyes(col):
    """visage : sourcils sombres, yeux (orbite sombre + point lumineux), bouche fermee"""
    def f(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape                                   # 9 x 9
        for (x0, y0) in ((1, 3), (w - 3, 3)):
            for dx in range(2):
                put(n, x0 + dx, y0 - 1, (8, 22, 16), .9)    # sourcil
                put(n, x0 + dx, y0, (6, 14, 12), .95)       # orbite
                put(n, x0 + dx, y0 + 1, col, 1)             # oeil
                glow('north', x0 + dx, y0 + 1, col)
        for x in range(3, 6):                                # bouche
            put(n, x, 7, (12, 36, 26), .85)
        put(n, 3, 8, (40, 90, 70), .5)
        put(n, 5, 8, (40, 90, 70), .5)
    return f


def fx_bonehead(F, rng, glow):
    """crane interieur : orbites sombres, trou du nez, rangee de dents"""
    n = F['north']
    h, w, _ = n.shape
    for (x0, y0) in ((1, 3), (w - 3, 3)):
        for dx in range(2):
            for dy in range(2):
                put(n, x0 + dx, y0 + dy, (14, 20, 16), .95)
    put(n, w // 2, 5, (20, 26, 22), .95)
    for x in range(w):
        put(n, x, h - 1, (30, 34, 30), .9)
        if x % 2 == 0:
            put(n, x, h - 2, (240, 242, 232), 1)


# ---------------------------------------------------------------- corps
def body_parts(sk, eye, head_mat, head_fx, ax=11, mail=True):
    """enveloppe fantomatique translucide (peau verte) autour d'un squelette opaque, a proportions humaines : genoux, mollets, epaules,
    pectoraux, cou, mains a doigts, visage (nez, arcade, pommettes, menton, oreilles). Dans chaque piece le squelette vient AVANT l'enveloppe
    (le rendu translucide ecrit la profondeur : il faut dessiner l'interieur d'abord)."""
    legs = {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        x = 5.2
        k = [C('femur_' + n, X(sg, x, 2), 20, -1, 2, 14, 2, 's_bone2', key='b_femur'),
             C('tibia_' + n, X(sg, x, 2), 4, -1, 2, 16, 2, 's_bone2', key='b_tibia'),
             C('footb_' + n, X(sg, x, 3), 0, -3.5, 3, 2, 6, 's_bone2', key='b_foot'),
             C('thigh_' + n, X(sg, x, 7), 20, -3.5, 7, 14, 7, sk, key='s_thigh'),
             C('knee_' + n, X(sg, x, 6), 17, -4.4, 6, 4, 1, sk, key='s_knee'),
             C('shin_' + n, X(sg, x, 5), 3, -2.5, 5, 17, 5, sk, key='s_shin'),
             C('calf_' + n, X(sg, x, 5), 7, 2.5, 5, 7, 2, sk, key='s_calf'),
             C('foot_' + n, X(sg, x, 5), 0, -4, 5, 3, 9, sk, key='s_foot'),
             C('toes_' + n, X(sg, x, 5), 0, -6.5, 5, 2, 3, sk, key='s_toes')]
        legs[sg] = Group('leg_' + n, (sg * x, 34, 0), (0, 0, 0), k)
    pelvis = Group('pelvis', (0, 34, 0), children=[C('pelvis_bone', -4, 31.5, -2.5, 8, 3, 5, 's_bone2', key='b_pelvis'),
                                                   C('pelvis', -6.5, 31, -3.7, 13, 5, 7, sk, key='s_pelvis')])
    bk = [C('spine', -1, 35, -1.5, 2, 21, 3, 's_bone2', key='b_spine'),
          C('sternum', -.5, 41, -3.9, 1, 11, 1, 's_bone2', key='b_sternum'),
          C('clavicle', -6, 52, -2, 12, 1, 2, 's_bone2', key='b_clav')]
    for i in range(5):
        bk.append(C(f'rib_{i}', -4.5, 41.5 + i * 2.6, -3.3, 9, 1, 6, 's_bone2', key='b_rib'))
    bk += [C('chest', -8, 41, -4.5, 16, 14, 9, sk, key='s_chest'),
           C('pecs', -7, 46, -5.6, 14, 5, 1, sk, key='s_pecs'),
           C('abd', -6, 34, -4, 12, 8, 8, sk, key='s_abd'),
           C('traps', -6, 54, -3.5, 12, 2, 7, sk, key='s_traps'),
           C('neck', -2, 55, -2.5, 4, 4, 5, sk, key='s_neck')]
    if mail:                                                # cotte de mailles et ceinture, comme le soldat d'origine
        bk += [C('mail_chest', -8.5, 40.5, -5, 17, 15, 10, 's_mail', key='s_mchest'),
               C('mail_abd', -6.5, 33.5, -4.5, 13, 8, 9, 's_mail', key='s_mabd'),
               C('belt', -7, 33, -4.9, 14, 2, 9, 's_cloth2', key='s_belt0'),
               C('buckle', -1, 33.2, -5.4, 2, 1.6, 1, 's_gold', key='s_buckle')]
    hk = [C('skull_bone', -3.5, 59, -4.5, 7, 7, 7, 's_bone2', key='b_skull', fx=fx_bonehead),
          C('jaw_bone', -2.5, 56.5, -3.5, 5, 2, 6, 's_bone2', key='b_jaw'),
          C('skull', -4.5, 58, -5.5, 9, 9, 9, head_mat, key='s_skull' + eye.hex() + head_mat, fx=head_fx),
          C('jaw', -3.5, 55.5, -4.5, 7, 3, 7, head_mat, key='s_jawm' + head_mat),
          C('nose', -1, 59, -7.2, 2, 3, 2, head_mat, key='s_nose'),
          C('brow', -4.5, 63, -6, 9, 1, 1, head_mat, key='s_brow'),
          C('chin', -1.5, 54.5, -5.3, 3, 2, 2, head_mat, key='s_chin'),
          C('cheek_l', -4.2, 58.2, -6.2, 2, 2, 1, head_mat, key='s_cheek0'),
          C('cheek_r', 2.2, 58.2, -6.2, 2, 2, 1, head_mat, key='s_cheek0'),
          C('ear_l', -5.5, 59.5, -2.5, 1, 3, 2, head_mat, key='s_ear'),
          C('ear_r', 4.5, 59.5, -2.5, 1, 3, 2, head_mat, key='s_ear')]
    arms, fores = {}, {}
    for sg in (-1, 1):
        n = 'left' if sg < 0 else 'right'
        cx = ax + .5
        arms[sg] = [C('humerus_' + n, X(sg, cx, 2), 41, -1, 2, 12, 2, 's_bone2', key='b_hum'),
                    C('shoulder_' + n, X(sg, cx, 6), 49, -3, 6, 6, 6, sk, key='s_sh'),
                    C('uarm_' + n, X(sg, cx, 5), 41, -2.5, 5, 9, 5, sk, key='s_ua')]
        fores[sg] = [C('radius_' + n, X(sg, cx, 2), 29, -1, 2, 12, 2, 's_bone2', key='b_rad'),
                     C('elbow_' + n, X(sg, cx, 4), 38.5, 1.7, 4, 3, 1, sk, key='s_elb'),
                     C('farm_' + n, X(sg, cx, 4), 30, -2, 4, 11, 4, sk, key='s_fa'),
                     C('forearm_top_' + n, X(sg, cx, 5), 35, -2.5, 5, 5, 5, sk, key='s_fat'),
                     C('palm_' + n, X(sg, cx, 4), 27, -2, 4, 3, 3, sk, key='s_palm'),
                     C('thumb_' + n, X(sg, cx, 4) + (1 if sg < 0 else -1) * -1 + (0 if sg < 0 else 3), 27.5, -3, 1, 2, 1, sk, key='s_thumb')]
        for f in range(4):
            fores[sg].append(C(f'finger_{n}{f}', X(sg, cx, 4) + f, 24, -1.5, 1, 3, 1, sk, key='s_fing'))
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
    groups = [pelvis, legs[-1], legs[1], body]
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
EYE = np.array((224, 255, 240))
GOLD = np.array((255, 236, 160))


def col(c):
    return O.col(c)


def spectre_epee(skin='7ad8a0'):
    """veteran : coiffe de mailles autour du visage, jupe de mailles, epee droite"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(EYE), sk, fx_eyes(col(EYE)))
    hk += [C('coif_top', -5.5, 65, -6.5, 11, 4, 11, 's_mail', key='s_ctop'),
           C('coif_back', -5.5, 56, 3, 11, 10, 2, 's_mail', key='s_cback'),
           C('coif_neck', -6, 52, -4.5, 12, 4, 10, 's_mail', key='s_cneck'),
           C('coif_brow', -5.5, 64, -7, 11, 1, 1, 's_metal', key='s_cbrow')]
    for sg in (-1, 1):
        hk.append(C('coif_side_' + ('l' if sg < 0 else 'r'), X(sg, 5.5, 2), 56, -6, 2, 9, 9, 's_mail', key='s_cside'))
    for i in range(7):
        ln = 9 + (i * 5 % 4) * 2
        bk.append(C(f'skirt_{i}', -6.5 + i * 1.9, 33 - ln, -5.2 - (i % 2) * .5, 2, ln, 1, 's_mail', key=f's_skirt{ln}', rot=(0, 0, (-3, 3, -2, 4)[i % 4])))
    for i in range(3):
        bk.append(C(f'skirt_back_{i}', -5 + i * 4, 22, 4.6, 4, 11, 1, 's_mail', key='s_skirtb'))
    return finish(legs, pel, bk, hk, arms, fores, w_sword(26, 3), hand_rot=(-30, 0, -6))


def spectre_lance(skin='6cc99a'):
    """hoplite : casque conique a nasal, bouclier rond, lance, pans de tissu"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(EYE), sk, fx_eyes(col(EYE)))
    tunic(bk)
    hk += [C('helm_dome', -5.5, 65, -6.5, 11, 3, 11, 's_metal', key='s_hdome'),
           C('helm_mid', -4.5, 68, -5.5, 9, 2, 9, 's_metal', key='s_hmid'),
           C('helm_top', -2.5, 70, -3.5, 5, 2, 5, 's_metal', key='s_htop'),
           C('helm_tip', -.5, 72, -1.5, 1, 3, 1, 's_rust', key='s_htip'),
           C('helm_back', -5.5, 57, 3, 11, 8, 2, 's_metal', key='s_hback'),
           C('helm_nasal', -.5, 58, -7.5, 1, 7, 1, 's_metal', key='s_hnasal')]
    for sg in (-1, 1):
        hk.append(C('cheek_' + ('l' if sg < 0 else 'r'), X(sg, 5, 2), 57, -6, 2, 7, 6, 's_metal', key='s_cheek'))
    bk.append(C('paul_l', -14, 52, -3.5, 5, 3, 7, 's_rust', key='s_paul'))
    return finish(legs, pel, bk, hk, arms, fores, w_spear(), hand_rot=(-8, 0, -6), shield=shield())


def spectre_hache(skin='8ae0b0'):
    """guerrier a cornes : casque ouvert a deux cornes, echarpe de tissu, epaulieres, hache, longs cheveux fantomatiques"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(EYE), sk, fx_eyes(col(EYE)))
    bk.append(C('belt2', -7, 33, -4.9, 14, 2, 9, 's_rust', key='s_belt2'))
    for i in range(6):
        ln = 8 + (i * 5 % 4) * 2
        bk.append(C(f'rag_{i}', -6.5 + i * 2.2, 33 - ln, -5.2, 2, ln, 1, 's_cloth', key=f's_hrag{ln}', rot=(0, 0, (-4, 3, -2, 5)[i % 4])))
    hk += [C('helm_band', -5.5, 64, -6.5, 11, 2, 11, 's_metal', key='s_hband'),
           C('helm_cap', -5.5, 66, -6.5, 11, 3, 11, 's_rust', key='s_hcap'),
           C('helm_back', -5.5, 58, 3, 11, 6, 2, 's_metal', key='s_hback2')]
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        hk.append(spike('horn_' + n, sg * 5.5, 65, -2, 6, 2, (0, 0, sg * -40), mat='s_bone'))
        hk.append(C('hair_' + n, X(sg, 5.6, 2), 52, 1, 2, 12, 3, 's_rag', key='s_hair'))
    for i in range(3):
        hk.append(C(f'hair_back_{i}', -4 + i * 3.4, 46, 3.6 + (i % 2) * .6, 3, 18 - i % 2 * 4, 1, 's_rag', key=f's_hairb{i % 2}'))
    bk.append(C('scarf', -7, 52, -5, 14, 4, 10, 's_cloth2', key='s_scarf'))
    bk.append(C('scarf_tail', 3, 38, -5.6, 3, 15, 1, 's_cloth2', key='s_scarft', rot=(0, 0, -6)))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('paul_' + n, X(sg, 12.5, 7), 53, -4, 7, 3, 8, 's_rust', key='s_paulr', rot=(0, 0, sg * -10), pivot=(sg * 12.5, 55, 0)))
    return finish(legs, pel, bk, hk, arms, fores, w_axe(), hand_rot=(-44, 0, -6))


def spectre_dague(skin='84dcab'):
    """rodeur en loques : sans armure, bandeau de tissu, echarpe et courte cape, pans de tissu dechires, dague ; le squelette se voit le mieux"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(EYE), sk, fx_eyes(col(EYE)), mail=False)
    bk.append(C('belt', -7, 33, -4.9, 14, 2, 9, 's_cloth2', key='s_beltd'))
    for i in range(6):
        ln = 12 + (i * 7 % 5) * 3
        bk.append(C(f'rag_{i}', -6.5 + i * 2.2, 33 - ln, -5.2 - (i % 2) * .6, 2, ln, 1, 's_cloth', key=f's_drag{ln}', rot=(0, 0, (-4, 3, -2, 5)[i % 4])))
    for i in range(3):
        bk.append(C(f'shred_{i}', -6 + i * 5, 42 + i % 2 * 4, -5.2, 3, 8 - i, 1, 's_cloth2', key=f's_shred{i}', rot=(0, 0, (6, -5, 4)[i])))
    bk.append(C('scarf', -7, 52, -5, 14, 3, 10, 's_cloth', key='s_scarfd'))
    bk.append(C('scarf_tail', 2, 36, -5.6, 3, 17, 1, 's_cloth', key='s_scarfdt', rot=(0, 0, -5)))
    hk += [C('headband', -5, 63, -6.2, 10, 2, 10, 's_cloth2', key='s_hbd'),
           C('headband_tail', -4, 56, 4.6, 3, 9, 1, 's_cloth2', key='s_hbdt'),
           C('headband_tail2', 1, 55, 4.6, 3, 11, 1, 's_cloth2', key='s_hbdt2')]
    return finish(legs, pel, bk, hk, arms, fores, w_sword(14, 2), hand_rot=(-50, 0, -6))


def spectre_roi(skin='9ae8c0'):
    """Roi des morts : le soldat d'origine en grand, avec une couronne d'or, une cape royale et une grande epee"""
    sk = skin_mat(skin)
    legs, pel, bk, hk, arms, fores = body_parts(sk, col(GOLD), sk, fx_eyes(col(GOLD)))
    bk += [C('plate', -8.5, 43, -5.5, 17, 11, 1, 's_metal', key='k_plate'),
           C('plate_trim', -8.5, 53, -5.6, 17, 2, 1, 's_gold', key='k_ptrim'),
           C('plate_gem', -1.5, 47, -6.2, 3, 3, 1, 's_gem', key='k_pgem'),
           C('belt_g', -7, 33, -5.2, 14, 3, 10, 's_gold', key='k_belt')]
    tunic(bk, 's_cloth2', 6, 's_cloth2')
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        bk.append(C('paul_' + n, X(sg, 13, 8), 53, -4.5, 8, 3, 9, 's_metal', key='k_paul', rot=(0, 0, sg * -10), pivot=(sg * 13, 55, 0)))
        bk.append(C('paul_trim_' + n, X(sg, 13, 8), 56, -4.5, 8, 1, 9, 's_gold', key='k_pault', rot=(0, 0, sg * -10), pivot=(sg * 13, 55, 0)))
        bk.append(spike('paul_spike_' + n, sg * 13.5, 56, -.5, 5, 2, (0, 0, sg * -12), mat='s_gold'))
    hk += [C('crown_band', -5.5, 66, -6.5, 11, 2, 11, 's_gold', key='k_cband'),
           C('crown_gem', -1, 66.5, -7.2, 2, 1, 1, 's_gem', key='k_cgem')]
    for i, (x, z, h) in enumerate([(-4.5, -6, 4), (-0.5, -6.4, 6), (3.5, -6, 4), (-4.5, 2.2, 4), (3.5, 2.2, 4), (-0.5, 2.6, 5), (-4.7, -2, 3), (3.7, -2, 3)]):
        hk.append(C(f'crown_{i}', x, 68, z, 2, h, 2, 's_gold', key=f'k_cs{h}'))
    for i in range(3):
        hk.append(C(f'beard_{i}', -3 + i * 2.6, 49 - i % 2 * 2, -5 - (i % 2) * .5, 2, 7 + i % 2 * 3, 1, 's_rag', key=f'k_beard{i % 2}'))
    return finish(legs, pel, bk, hk, arms, fores, w_sword(34, 5), hand_rot=(-24, 0, -6), head_f=1.16,
                  capes=cape('s_cloth2', 's_gold', 20))


MODELS = {
    'spectre_epee': (spectre_epee, 1.0),
    'spectre_lance': (spectre_lance, 1.0),
    'spectre_hache': (spectre_hache, 1.0),
    'spectre_dague': (spectre_dague, 1.0),
}


ALPHA = {'s_cloth': 165, 's_cloth2': 175, 's_rag': 160, 's_metal': 205, 's_rust': 205, 's_blade': 230, 's_wood': 225, 's_gold': 235, 's_gem': 255,
         's_dark': 255, 's_bone': 255, 's_bone2': 255}
SKIN_ALPHA = 105


def apply_alpha(cubes, tex):
    """transparence par pixel : peau fantomatique tres translucide, mailles ajourees, os opaques"""
    done = set()
    for c in cubes:
        if c.key in done:
            continue
        done.add(c.key)
        w, h, d = c.size
        for f, (x0, y0, fw, fh) in face_rects(c.u, c.v, w, h, d).items():
            lum = tex[y0:y0 + fh, x0:x0 + fw, :3].astype(float).mean(2)
            if c.mat.startswith('s_sk_'):
                v = np.where(lum > 232, 255, SKIN_ALPHA)             # les yeux restent lumineux
            elif c.mat == 's_mail':
                v = np.where(lum >= lum.mean(), 215, 80)             # mailles ajourees : on voit les cotes a travers
            else:
                v = ALPHA.get(c.mat, 255)
            tex[y0:y0 + fh, x0:x0 + fw, 3] = v
    return tex


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
    tex = apply_alpha(cubes, tex)
    return groups, cubes, W, H, tex, glow


def sheet(path, names=None, views=(0, 40, 180)):
    from PIL import Image
    rows = []
    for name in names or MODELS:
        fn, f = MODELS[name]
        groups, cubes, W, H, tex, glow = build(name)
        sc = 5.6 / f
        ims = []
        for y in views:
            im, zb = render(groups, tex, y, pitch=-8, W=330, H=520, scale=sc, mode='opaque')
            ims.append(render(groups, tex, y, pitch=-8, W=330, H=520, scale=sc, mode='trans', img=im, zbuf=zb))
        rows.append(np.concatenate(ims, axis=1))
    Image.fromarray(np.concatenate(rows, axis=0)).save(path)


if __name__ == '__main__':
    sheet('/tmp/spectres_a.png', ['spectre_epee', 'spectre_lance'])
    sheet('/tmp/spectres_b.png', ['spectre_hache', 'spectre_dague'])
