"""Etres des ombres : cavaliers noirs sous de grandes capes (rien sous la capuche), et leur chef au casque a pointes.
Le cheval fait partie du modele (une seule entite). Memes conventions que spectres.py : y vers le haut, sol y=0, avant = -z, cote -x = 'left'.
Noms de pieces animes par GeoModel : leg_fl/fr/bl/br (trot), tail/tail2/tail3, cape/cape_c/cape_d/cape_e, head (capuche ou casque, suit le regard),
rarm_r (bras d'epee, leve a l'attaque par GeoModel)."""
import math
import numpy as np
import trollcore as T
from trollcore import *
import orcs as O
from orcs import C, X, spike, translate, scale_all, unique_names

MAT = {
    'h_cloth': dict(base=(46, 46, 58), n1=7, n2=8, tint=(.95, .97, 1)),
    'h_cloth2': dict(base=(30, 30, 40), n1=7, n2=8, tint=(.95, .97, 1)),
    'h_cape': dict(base=(38, 38, 50), n1=8, n2=8, tint=(.95, .97, 1)),
    'h_void': dict(base=(2, 2, 4), n1=2, n2=2, tint=(1, 1, 1)),
    'h_coat': dict(base=(36, 34, 42), n1=6, n2=7, tint=(1, 1, 1)),
    'h_coat2': dict(base=(54, 52, 62), n1=6, n2=7, tint=(1, 1, 1)),
    'h_mane': dict(base=(16, 16, 24), n1=8, n2=8, tint=(.95, .97, 1)),
    'h_leather': dict(base=(50, 40, 36), n1=6, n2=7, tint=(1, 1, 1)),
    'h_hoof': dict(base=(24, 22, 26), n1=4, n2=5, tint=(1, 1, 1)),
    'h_iron': dict(base=(78, 78, 88), n1=6, n2=7, tint=(.95, .95, 1)),
    'h_bronze': dict(base=(128, 100, 62), n1=7, n2=7, tint=(1, .95, .85)),
    'h_blade': dict(base=(166, 168, 176), n1=7, n2=6, tint=(.97, .98, 1)),
    'h_steel': dict(base=(92, 88, 90), n1=6, n2=7, tint=(.97, .95, .97)),
    'h_helm': dict(base=(158, 148, 128), n1=6, n2=7, tint=(1, .97, .9)),
    'h_spike': dict(base=(168, 160, 142), n1=6, n2=6, tint=(1, .97, .9)),
}


def d_pleat(arr, face, rng):
    """plis verticaux du tissu : colonnes plus sombres, quelques fils clairs, bas effiloche"""
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= (.72 if x % 3 == 0 else (.9 if x % 3 == 1 else 1.05)) * (1 + (rng.random() - .5) * .1)
    for _ in range(max(1, h * w // 16)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (62, 62, 74), .45)
    for x in range(w):
        if rng.random() < .4:
            put(arr, x, h - 1, (10, 10, 14))


def d_coat(arr, face, rng):
    """robe du cheval : reflet en haut, ombre en bas, quelques poils clairs"""
    h, w, _ = arr.shape
    arr *= np.linspace(1.18, .8, h)[:, None, None]
    for _ in range(max(1, h * w // 12)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (70, 68, 80), .35)


def d_plain(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(1.04, .92, h)[:, None, None]


def d_hair(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= (1.3, .75, 1.0, .6)[int(rng.integers(0, 4))]
    for x in range(w):
        if rng.random() < .5:
            put(arr, x, h - 1, (6, 6, 10))


T.MATS.update(MAT)
T.DECOR.update({'h_cloth': d_pleat, 'h_cloth2': d_pleat, 'h_cape': d_pleat, 'h_void': d_plain, 'h_coat': d_coat, 'h_coat2': d_coat,
                'h_mane': d_hair, 'h_leather': O.d_leather2 if hasattr(O, 'd_leather2') else d_plain, 'h_hoof': d_plain,
                'h_iron': lambda a, f, r: O.d_iron2(a, f, r, rust=.15), 'h_bronze': lambda a, f, r: O.d_iron2(a, f, r, rust=.3),
                'h_blade': O.d_blade2, 'h_steel': lambda a, f, r: O.d_iron2(a, f, r, rust=.25),
                'h_helm': lambda a, f, r: O.d_iron2(a, f, r, rust=.2), 'h_spike': lambda a, f, r: O.d_iron2(a, f, r, rust=.1)})


# ---------------------------------------------------------------- visages
def fx_mask(F, rng, glow):
    """masque du chef : deux fentes en biais pour les yeux, grande bouche ouverte et noire, arete du nez"""
    n = F['north']
    h, w, _ = n.shape                                        # 12 x 16
    dark = (3, 3, 5)
    for (x0, sgn) in ((2, 1), (w - 5, -1)):
        for dx in range(3):
            put(n, x0 + dx, 4 + (dx if sgn > 0 else 2 - dx) // 2, dark, 1)
            put(n, x0 + dx, 5 + (dx if sgn > 0 else 2 - dx) // 2, dark, 1)
    for y in range(7, h - 1):
        half = 3 - abs(y - 10) // 2 if y < 11 else 3 - (y - 10) // 2
        half = max(1, half)
        for x in range(w // 2 - half, w // 2 + half):
            put(n, x, y, dark, 1)
    for y in range(3, 7):
        put(n, w // 2, y, (200, 190, 168), .6)


# ---------------------------------------------------------------- cheval
def horse(chief=False):
    """cheval noir : corps, quatre pattes (leg_fl/fr/bl/br), encolure et tete, crinière, queue ; harnais ; cuirasse pour le chef"""
    legs = []
    for fz, sg_, n in ((-18, -1, 'fl'), (-18, 1, 'fr'), (18, -1, 'bl'), (18, 1, 'br')):
        back = fz > 0
        x = 6.0
        z = fz
        k = []
        if back:
            k += [C(f'thigh_{n}', X(sg_, x, 8), 16, z - 4, 8, 16, 9, 'h_coat', key='h_thigh'),
                  C(f'hock_{n}', X(sg_, x, 5), 9, z + 1, 5, 9, 5, 'h_coat', key='h_hock'),
                  C(f'cannon_{n}', X(sg_, x, 4), 4, z + 2, 4, 10, 4, 'h_coat', key='h_cannonb')]
        else:
            k += [C(f'forearm_{n}', X(sg_, x, 7), 16, z - 3.5, 7, 15, 7, 'h_coat', key='h_forearm'),
                  C(f'knee_{n}', X(sg_, x, 5), 14, z - 2.5, 5, 3, 5, 'h_coat', key='h_knee'),
                  C(f'cannon_{n}', X(sg_, x, 4), 4, z - 2, 4, 11, 4, 'h_coat', key='h_cannonf')]
        k += [C(f'pastern_{n}', X(sg_, x, 4), 2, z - 2.5 + (2 if back else 0), 4, 3, 5, 'h_coat', key='h_pastern'),
              C(f'hoof_{n}', X(sg_, x, 6), 0, z - 3.5 + (2 if back else 0), 6, 3, 7, 'h_hoof', key='h_hoof')]
        if chief:
            k.append(C(f'greave_{n}', X(sg_, x, 6), 5, z - 3 + (2 if back else 0), 6, 7, 6, 'h_steel', key='h_greave'))
        legs.append(Group('leg_' + n, (sg_ * x, 30, z), (0, 0, 0), k))
    body = [C('barrel', -9, 31, -22, 18, 27, 46, 'h_coat', key='hb_barrel'),
            C('chest', -8.5, 29, -31, 17, 30, 12, 'h_coat', key='hb_chest'),
            C('rump', -9, 32, 20, 18, 26, 14, 'h_coat', key='hb_rump'),
            C('belly', -7, 28, -14, 14, 3, 30, 'h_coat2', key='hb_belly'),
            C('withers', -4, 58, -18, 8, 3, 8, 'h_coat', key='hb_withers')]
    # neck et tete : la tete pend en avant et vers le bas
    neck = [C('neck', -5.5, 54, -33, 11, 30, 15, 'h_coat', key='hn_neck'),
            C('mane', -2, 56, -18.5, 4, 29, 4, 'h_mane', key='hn_mane')]
    for i in range(4):
        neck.append(C(f'mane_f{i}', -3.5, 60 + i * 6.5, -17 + (i % 2), 7, 5, 2, 'h_mane', key=f'hn_maneF{i % 2}'))
    head = [C('skull', -5, 77, -38, 10, 14, 17, 'h_coat', key='hh_skull'),
            C('muzzle', -4, 76, -53, 8, 10, 16, 'h_coat', key='hh_muzzle'),
            C('nose_l', -3.2, 77, -54, 2, 3, 1, 'h_void', key='hh_nostril'), C('nose_r', 1.2, 77, -54, 2, 3, 1, 'h_void', key='hh_nostril'),
            C('lip', -3, 74, -52, 6, 3, 14, 'h_coat2', key='hh_lip'),
            C('ear_l', -4.5, 90, -26, 3, 8, 3, 'h_coat', key='hh_ear', rot=(0, 0, 8), pivot=(-3, 90, -24.5)),
            C('ear_r', 1.5, 90, -26, 3, 8, 3, 'h_coat', key='hh_ear', rot=(0, 0, -8), pivot=(3, 90, -24.5)),
            C('eye_l', -5.6, 85, -33, 1, 3, 3, 'h_void', key='hh_eye', fx=fx_horse_eye), C('eye_r', 4.6, 85, -33, 1, 3, 3, 'h_void', key='hh_eye', fx=fx_horse_eye),
            C('cheekstrap', -5.4, 79, -37, 11, 2, 2, 'h_leather', key='hh_cheek'),
            C('noseband', -4.6, 79, -49, 9, 2, 9, 'h_leather', key='hh_noseband')]
    for sg_ in (-1, 1):
        for i in range(3):                                   # chaine de mors qui pend
            head.append(C(f'chain_{"l" if sg_ < 0 else "r"}{i}', X(sg_, 4.8, 1), 69 - i * 2, -44 - i * .5, 1, 2, 1, 'h_iron', key='hh_chain'))
    if chief:
        head += [C('chamfron', -5.5, 84, -45, 11, 3, 14, 'h_steel', key='hh_chamfron'),
                 C('chamfron_nose', -4.5, 77, -53.8, 9, 8, 2, 'h_steel', key='hh_chamfnose')]
        head.append(spike('chamfron_spike', 0, 87, -42, 9, 3, (-20, 0, 0), mat='h_spike'))
        head.append(spike('chamfron_spike_l', -4, 86, -36, 6, 2, (0, 0, 32), mat='h_spike'))
        head.append(spike('chamfron_spike_r', 4, 86, -36, 6, 2, (0, 0, -32), mat='h_spike'))
        for i in range(4):
            neck.append(spike(f'neck_spike{i}', 0, 60 + i * 6, -17, 7 - i % 2, 2, (30, 0, 0), mat='h_spike'))
        neck.append(C('neck_plate_l', -7, 56, -32, 2, 26, 13, 'h_steel', key='hn_plate'))
        neck.append(C('neck_plate_r', 5, 56, -32, 2, 26, 13, 'h_steel', key='hn_plate'))
    nk = Group('horse_neck', (0, 54, -26), (-34, 0, 0), neck + [Group('horse_head', (0, 84, -26), (-12, 0, 0), head)])
    # queue en trois segments
    tails = Group('tail', (0, 57, 29), (-22, 0, 0), [
        C('tail_a', -2.5, 44, 28, 5, 14, 5, 'h_mane', key='ht_a'),
        Group('tail2', (0, 44, 30.5), (-10, 0, 0), [
            C('tail_b', -2.5, 30, 28, 5, 14, 5, 'h_mane', key='ht_b'),
            Group('tail3', (0, 30, 30.5), (-8, 0, 0), [C('tail_c', -2, 18, 28.5, 4, 12, 4, 'h_mane', key='ht_c')])])])
    # selle, couverture, etriers, harnais
    gear = [C('blanket', -8.8, 54, -5, 17.6, 5, 20, 'h_cloth2', key='hg_blanket'),
            C('saddle', -7, 58, -5, 14, 2.5, 17, 'h_leather', key='hg_saddle'),
            C('saddle_back', -6, 60, 8, 12, 3, 2, 'h_leather', key='hg_sback'),
            C('girth', -9.2, 36, -3, 18.4, 2, 3, 'h_leather', key='hg_girth'),
            C('breastband', -9, 52, -29, 18, 2, 2, 'h_leather', key='hg_breast'),
            C('crupper', -9, 52, 30, 18, 2, 2, 'h_leather', key='hg_crupper')]
    for sg_ in (-1, 1):
        n = 'l' if sg_ < 0 else 'r'
        gear += [C('stirrup_strap_' + n, X(sg_, 9.5, 1), 44, -1, 1, 14, 2, 'h_leather', key='hg_strap'),
                 C('stirrup_' + n, X(sg_, 11, 4), 41, -1.5, 4, 2, 5, 'h_iron', key='hg_stirrup')]
    if chief:                                                # barde : plastron, flancs, croupe
        gear += [C('bard_chest', -9.6, 38, -33, 19.2, 18, 3, 'h_steel', key='hg_bchest'),
                 C('bard_side_l', -10.6, 38, -16, 2, 18, 30, 'h_steel', key='hg_bside'), C('bard_side_r', 8.6, 38, -16, 2, 18, 30, 'h_steel', key='hg_bside'),
                 C('bard_rump', -9.6, 40, 24, 19.2, 18, 3, 'h_steel', key='hg_brump'),
                 C('bard_rim_l', -10.8, 36, -16, 1, 2, 30, 'h_bronze', key='hg_brim'), C('bard_rim_r', 9.8, 36, -16, 1, 2, 30, 'h_bronze', key='hg_brim')]
        for i in range(5):
            for sg_ in (-1, 1):
                gear.append(spike(f'bard_spike_{i}{"l" if sg_ < 0 else "r"}', sg_ * 11, 46, -12 + i * 6, 6 - i % 2, 2, (0, 0, sg_ * -62), mat='h_spike'))
        gear.append(spike('bard_spike_c', 0, 56, -35, 8, 3, (-55, 0, 0), mat='h_spike'))
    return legs, body + gear + [nk, tails]


def fx_horse_eye(F, rng, glow):
    n = F['west']
    h, w, _ = n.shape
    put(n, w - 1, 1, (255, 40, 30), 1)
    glow('west', w - 1, 1, (255, 40, 30))
    e = F['east']
    h, w, _ = e.shape
    put(e, 0, 1, (255, 40, 30), 1)
    glow('east', 0, 1, (255, 40, 30))


# ---------------------------------------------------------------- cavalier
def cape_chain(w=24):
    """grande cape : tombe du dos, passe sur la croupe puis retombe derriere (cape > cape_c > cape_d > cape_e, anime par GeoModel)"""
    tails = []
    for i, L in enumerate((12, 18, 10, 20, 14, 8)):
        tails.append(C(f'cape_tail_{i}', -w / 2 + i * (w / 6), 36 - L, 30, w / 6 + .4, L, 2, 'h_cape', key=f'ca_t{L}'))
    e = Group('cape_e', (0, 36, 30), (0, 0, 0), tails)
    d = Group('cape_d', (0, 61, 30), (0, 0, 0), [C('cape_d_b', -w / 2, 36, 30, w, 25, 2, 'h_cape', key='ca_d'), e])
    c = Group('cape_c', (0, 62, 9), (0, 0, 0), [C('cape_c_b', -w / 2, 60.5, 8, w, 2, 22, 'h_cape', key='ca_c'), d])
    return Group('cape', (0, 87, 7.5), (0, 0, 0), [C('cape_top', -w / 2 + 1, 62, 7.5, w - 2, 26, 2, 'h_cape', key='ca_top'),
                                                  C('cape_fold_l', -w / 2 - 1, 64, 7, 3, 22, 3, 'h_cape', key='ca_fold', rot=(0, 0, 6), pivot=(-w / 2, 86, 8)),
                                                  C('cape_fold_r', w / 2 - 2, 64, 7, 3, 22, 3, 'h_cape', key='ca_fold', rot=(0, 0, -6), pivot=(w / 2, 86, 8)), c])


def hood(variant):
    """capuche profonde : l'interieur est un noir complet"""
    k = [C('void', -4.5, 88, -4, 9, 12, 9, 'h_void', key='hd_void'),
         C('hood_back', -7.5, 86, 4, 15, 18, 5, 'h_cloth', key='hd_back'),
         C('hood_sl', -7.5, 86, -5, 3, 18, 10, 'h_cloth', key='hd_side'), C('hood_sr', 4.5, 86, -5, 3, 18, 10, 'h_cloth', key='hd_side'),
         C('hood_top', -7.5, 100, -6, 15, 4, 15, 'h_cloth', key='hd_top'),
         C('hood_brow', -6, 96, -8, 12, 4, 3, 'h_cloth2', key='hd_brow'),
         C('hood_fold_l', -8.5, 84, -6, 2, 13, 4, 'h_cloth2', key='hd_fold', rot=(0, 0, 5), pivot=(-8, 96, -4)),
         C('hood_fold_r', 6.5, 84, -6, 2, 13, 4, 'h_cloth2', key='hd_fold', rot=(0, 0, -5), pivot=(8, 96, -4)),
         C('hood_peak', -4, 103, 1, 8, 9, 8, 'h_cloth', key='hd_peak', rot=(24, 0, 0), pivot=(0, 103, 5)),
         C('hood_tip', -2, 111, 4, 4, 5, 4, 'h_cloth2', key='hd_tip', rot=(40, 0, 0), pivot=(0, 111, 6))]
    if variant == 1:                                         # capuche plus haute et tombante sur le devant
        k.append(C('hood_drop', -5, 90, -9, 10, 8, 2, 'h_cloth2', key='hd_drop'))
    return k


def helm():
    """casque du chef : masque a fentes et bouche noire, calotte, crete, ailes de joues, couronne de pointes"""
    k = [C('void', -5, 88, -4, 10, 14, 9, 'h_void', key='hm_void'),
         C('mask', -6, 87, -7.5, 12, 16, 4, 'h_helm', key='hm_mask', fx=fx_mask),
         C('helm_back', -7, 87, -3.5, 14, 18, 12, 'h_helm', key='hm_back'),
         C('helm_top', -6.5, 104, -5, 13, 3, 13, 'h_helm', key='hm_top'),
         C('helm_crest', -1, 106, -6, 2, 5, 14, 'h_steel', key='hm_crest'),
         C('neckguard', -7, 80, 0, 14, 8, 10, 'h_steel', key='hm_neck'),
         C('brow_ridge', -6.5, 98, -8, 13, 2, 2, 'h_steel', key='hm_ridge')]
    for sg_ in (-1, 1):
        n = 'l' if sg_ < 0 else 'r'
        k.append(C('cheek_' + n, X(sg_, 7.5, 3), 86, -6.5, 3, 12, 7, 'h_helm', key='hm_cheek', rot=(0, 0, sg_ * -12), pivot=(sg_ * 7, 98, -4)))
    # couronne de pointes rayonnant du casque (comme sur la reference)
    for i, (ang, ln, tilt) in enumerate([(0, 8, 28), (40, 10, 52), (80, 9, 70), (120, 8, 56), (160, 8, 36), (200, 8, 36), (240, 8, 56), (280, 9, 70), (320, 10, 52)]):
        a = math.radians(ang)
        sx, cz = math.sin(a), math.cos(a)
        px, pz = 6.8 * sx, -1 - 6.6 * cz
        k.append(spike(f'helm_spike{i}', px, 99, pz, ln, 2, (-tilt * cz, 0, -sx * tilt), mat='h_spike'))
    k.append(spike('helm_tip', 0, 107, -1, 12, 3, (0, 0, 0), mat='h_spike'))
    return k


def arm_pair(variant, chief):
    """bras d'epee (rarm_r : leve par GeoModel a l'attaque) et bras des renes ; manches larges, gantelets"""
    sx = 1
    cx = 10.5
    arms = []
    # bras droit : epaule (cx,85,1)
    up = [C('sleeve_ur', cx - 3, 72, -2, 6, 14, 6, 'h_cloth', key='ar_up')]
    cuff = [C('sleeve_fr', cx - 4.5, 58, -3.5, 9, 14, 9, 'h_cloth2', key='ar_cuff'),
            C('cuff_hem_r', cx - 5, 56, -4, 10, 3, 10, 'h_cloth', key='ar_hem'),
            C('glove_r', cx - 2.2, 53, -1.5, 4.4, 5, 3.6, 'h_iron', key='ar_glove'),
            C('glove_edge_r', cx - 3, 57, -2.5, 6, 2, 5, 'h_bronze', key='ar_gedge')]
    for f in range(4):
        cuff.append(C(f'finger_r{f}', cx - 2.2 + f * 1.1, 50.5, -2.2, 1.1, 3, 1.4, 'h_iron', key='ar_fing'))
    if variant == 0:
        r_up, r_fo, sword_rot = -30, -80, 110
    elif variant == 1:
        r_up, r_fo, sword_rot = -22, -20, -108
    else:
        r_up, r_fo, sword_rot = -26, -70, 96
    blade_len = 50 if chief else (40 if variant == 0 else 34)
    sword = [C('grip', -1, -4, -1, 2, 8, 2, 'h_leather', key='sw_grip'),
             C('pommel', -2, -6, -2, 4, 2, 4, 'h_bronze', key='sw_pom'),
             C('guard', -6, 4, -1.5, 12, 2, 3, 'h_iron', key='sw_guard'),
             C('guard_tip_l', -7, 3, -1, 2, 4, 2, 'h_iron', key='sw_gtip'), C('guard_tip_r', 5, 3, -1, 2, 4, 2, 'h_iron', key='sw_gtip'),
             C('blade', -2, 6, -.75, 4, blade_len, 2, 'h_blade', key=f'sw_blade{blade_len}'),
             C('blade_core', -.5, 6, -1.4, 1, blade_len - 4, 3, 'h_steel', key=f'sw_core{blade_len}'),
             C('tip', -1.2, 6 + blade_len, -.5, 2.4, 6, 1, 'h_blade', key='sw_tip')]
    hp = (cx, 54, -0.3)
    translate(sword, *hp)
    fore = Group('forearm_r', (cx, 72, 1), (r_fo, 0, 0), cuff + [Group('weapon', hp, (sword_rot, 0, 0), sword)])
    arms.append(Group('rarm_r', (cx, 85, 1), (r_up, 0, 0), up + [fore]))
    # bras gauche (renes) : avance et bas
    upl = [C('sleeve_ul', -cx - 3, 72, -2, 6, 14, 6, 'h_cloth', key='ar_up')]
    cufl = [C('sleeve_fl', -cx - 4.5, 58, -3.5, 9, 14, 9, 'h_cloth2', key='ar_cuff'),
            C('cuff_hem_l', -cx - 5, 56, -4, 10, 3, 10, 'h_cloth', key='ar_hem'),
            C('glove_l', -cx - 2.2, 53, -1.5, 4.4, 5, 3.6, 'h_iron', key='ar_glove'),
            C('glove_edge_l', -cx - 3, 57, -2.5, 6, 2, 5, 'h_bronze', key='ar_gedge')]
    for f in range(4):
        cufl.append(C(f'finger_l{f}', -cx - 2.2 + f * 1.1, 50.5, -2.2, 1.1, 3, 1.4, 'h_iron', key='ar_fing'))
    forel = Group('forearm_l', (-cx, 72, 1), (-62, 0, 0), cufl)
    arms.append(Group('rarm_l', (-cx, 85, 1), (-34, 0, 0), upl + [forel]))
    return arms


def rider(variant, chief):
    k = [C('robe_torso', -8, 61, -3.5, 16, 26, 11, 'h_cloth', key='rd_torso'),
         C('robe_pleat', -5, 61, -4.4, 10, 22, 1, 'h_cloth2', key='rd_pleat'),
         C('belt_r', -8.3, 61, -3.8, 16.6, 3, 11.6, 'h_leather', key='rd_belt'),
         C('buckle', -2, 61.3, -4.4, 4, 2.4, 1, 'h_bronze', key='rd_buckle'),
         C('collar', -10, 83, -4.5, 20, 6, 13, 'h_cloth2', key='rd_collar'),
         C('lap', -8.5, 55, -9, 17, 8, 13, 'h_cloth', key='rd_lap')]
    for sg_ in (-1, 1):
        n = 'l' if sg_ < 0 else 'r'
        k.append(C('shoulder_' + n, X(sg_, 11, 8), 80, -3.5, 8, 7, 10, 'h_cloth', key='rd_shoulder'))
        # pans de robe qui retombent des deux cotes du cheval, bord effiloche (longueurs inegales)
        for j, (z0, L) in enumerate(((-6, 24), (0, 30), (6, 26), (12, 32), (18, 22))):
            k.append(C(f'drape_{n}{j}', X(sg_, 10.5, 4), 66 - L, z0, 4, L, 6.2, 'h_cloth', key=f'rd_drape{L}',
                       rot=(0, 0, sg_ * 6), pivot=(sg_ * 10.5, 66, z0 + 3)))
    head = Group('head', (0, 86, 2), (0, 0, 0), helm() if chief else hood(variant))
    k.append(head)
    k += arm_pair(variant, chief)
    if chief:                                                # epaulieres a lames superposees, gorgerin
        for sg_ in (-1, 1):
            n = 'l' if sg_ < 0 else 'r'
            for i, (w, yy) in enumerate(((11, 85), (10, 82), (9, 79))):
                k.append(C(f'pauld_{n}{i}', X(sg_, 12.5, w), yy, -4.5, w, 3, 11, 'h_steel', key=f'pd{i}', rot=(0, 0, sg_ * -14), pivot=(sg_ * 12, yy + 3, 1)))
            k.append(spike('pauld_spike_' + n, sg_ * 14, 88, 0, 8, 3, (0, 0, sg_ * -30), mat='h_spike'))
            k.append(spike('pauld_spike2_' + n, sg_ * 12, 87, -3.5, 6, 2, (-24, 0, sg_ * -18), mat='h_spike'))
        k.append(C('gorget', -7.5, 80, -5.5, 15, 6, 3, 'h_steel', key='rd_gorget'))
        k.append(C('breastplate', -7, 66, -4.8, 14, 16, 1.5, 'h_steel', key='rd_bplate'))
        k.append(C('breastplate_trim', -7, 80, -5, 14, 2, 1.5, 'h_bronze', key='rd_btrim'))
    return k


def shadow(variant=0, chief=False):
    legs, hbody = horse(chief)
    r = rider(variant, chief)
    cape = cape_chain(26 if chief else 24)
    body = Group('body', (0, 58, 0), (0, 0, 0), hbody + r + [cape])
    groups = legs + [body]
    unique_names(groups)
    return groups


MODELS = {
    'ombre_1': (lambda: shadow(0, False), 1.0),
    'ombre_2': (lambda: shadow(1, False), 1.0),
    'ombre_chef': (lambda: shadow(2, True), 1.12),
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


def sheet(path, names=None, views=(0, 45, 90, 180), scale=3.4):
    from PIL import Image
    rows = []
    for name in names or MODELS:
        fn, f = MODELS[name]
        groups, cubes, W, H, tex, glow = build(name)
        ims = []
        for y in views:
            im = render(groups, tex, y, pitch=-8, W=340, H=520, scale=scale / f, mode='all')
            ims.append(im[0] if isinstance(im, tuple) else im)
        rows.append(np.concatenate(ims, axis=1))
    Image.fromarray(np.concatenate(rows, axis=0)).save(path)


if __name__ == '__main__':
    sheet('/tmp/ombres.png', ['ombre_1', 'ombre_chef'])
