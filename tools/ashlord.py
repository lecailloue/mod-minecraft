"""Seigneur de Cendre (boss guerrier geant) : armure noire, casque a cornes tordues, epaulieres a pointes, cape, masse a deux mains.
Genere tools/assets/ash_lord.bbmodel (+ ash_lord_glow.png). Systeme de coordonnees Blockbench : y vers le haut, sol y=0, avant = -z,
cote -x = 'left' (renomme leg_l par models.py)."""
import numpy as np
from scipy.optimize import minimize
from trollcore import *
from trollcore import _N
import trollcore as T

# ---------------------------------------------------------------- materiaux : tout en metal noir / gris sombre, aucune autre couleur
T.MATS.update({
    'ash':    dict(base=(34, 34, 40), n1=3, n2=4, tint=(1, 1, 1.06)),     # noir
    'ashd':   dict(base=(18, 18, 22), n1=2, n2=3, tint=(1, 1, 1.05)),     # noir profond (sous-couches, fentes)
    'plate':  dict(base=(64, 65, 72), n1=4, n2=5, tint=(1, 1, 1.05)),     # gris sombre
    'steel':  dict(base=(90, 92, 100), n1=5, n2=6, tint=(1, 1, 1.04)),  # gris acier (cornes, pointes, masque)
    'mask':   dict(base=(70, 72, 80), n1=4, n2=5, tint=(1, 1, 1.04)),
    'dome':   dict(base=(36, 36, 43), n1=3, n2=3, tint=(1, 1, 1.06)),
    'mail':   dict(base=(46, 47, 54), n1=4, n2=4, tint=(1, 1, 1.06)),
    'horn':   dict(base=(74, 76, 84), n1=4, n2=5, tint=(1, 1, 1.04)),
    'cape':   dict(base=(30, 30, 35), n1=3, n2=5, tint=(1, 1, 1.05)),
    'leath':  dict(base=(28, 27, 31), n1=3, n2=4, tint=(1, 1, 1)),
})


def d_plate(arr, face, rng):
    h, w, _ = arr.shape
    if h >= 2 and w >= 2:
        arr[0, :] += 20
        arr[:, 0] += 12
        arr[-1, :] -= 14
        arr[:, -1] -= 9
    for _ in range(max(1, (h * w) // 55)):                      # rayures de metal
        x = int(rng.integers(0, w)); y = int(rng.integers(0, h))
        line(arr, x, y, 1, 1 if rng.random() < .5 else 0, int(rng.integers(2, 5)), (130, 132, 142), .30)
    if w >= 9 and h >= 8:
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            put(arr, x, y, (110, 112, 122))


def d_steel(arr, face, rng):
    h, w, _ = arr.shape
    arr *= np.linspace(.74, 1.18, h)[:, None, None]
    if w >= 3:
        arr[:, 0] += 22
        arr[:, -1] -= 20
    for _ in range(max(1, (h * w) // 28)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (170, 172, 184), .28)


def d_horn(arr, face, rng):
    h, w, _ = arr.shape
    if w >= 3:
        arr[:, 0] += 26
        arr[:, 1] += 8
        arr[:, -1] -= 30
    for _ in range(max(1, (h * w) // 24)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (150, 152, 164), .22)
    arr[-1, :] -= 6


def d_mail(arr, face, rng):
    h, w, _ = arr.shape
    for y in range(h):                                     # ecailles / mailles : rangees decalees, creux sombre + reflet
        for x in range(w):
            if (x + (y % 2)) % 2 == 0:
                put(arr, x, y, (14, 14, 18), .55)
            elif y % 2 == 0:
                put(arr, x, y, (120, 122, 134), .30)
    arr[0, :] += 10
    arr[-1, :] -= 12


def d_smooth(arr, face, rng):
    h, w, _ = arr.shape
    for _ in range(max(1, (h * w) // 70)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (96, 98, 110), .22)


def d_capefold(arr, face, rng):
    h, w, _ = arr.shape
    for x in range(w):
        arr[:, x] *= 1.0 + .20 * math.sin(x * 1.1) + (rng.random() - .5) * .05
    if h >= 3:
        for y in range(h):
            a = (y / h) ** 2 * .5
            arr[y] = arr[y] * (1 - a) + np.array((12, 12, 15)) * a
        for x in range(w):
            if rng.random() < .4:
                put(arr, x, h - 1, (10, 10, 12))
    for _ in range(max(1, (h * w) // 200)):
        line(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), 0, 1, int(rng.integers(3, 8)), (14, 14, 17), .5)


def d_ashd(arr, face, rng):
    h, w, _ = arr.shape
    arr[:, ::2, :] *= .93
    for _ in range(max(1, (h * w) // 90)):
        put(arr, int(rng.integers(0, w)), int(rng.integers(0, h)), (52, 52, 60), .5)


T.DECOR.update({'dome': d_smooth, 'mail': d_mail, 'mask': d_plate, 'horn': d_horn, 'ash': d_plate, 'plate': d_plate, 'steel': d_steel, 'cape': d_capefold, 'ashd': d_ashd, 'leath': d_ashd})


# ---------------------------------------------------------------- details de texture
def fx_eyes(F, rng, glow):
    """orbites profondes et obliques (comme sur la photo) : deux fentes noires qui descendent vers le nez, arcades sombres"""
    n = F['north']
    h, w, _ = n.shape                          # 15 x 6
    for side in (0, 1):
        for (x, y) in [(1, 0), (2, 0), (2, 1), (3, 1), (3, 2), (4, 2), (5, 2), (5, 3), (6, 3)]:
            xx = x if side == 0 else w - 1 - x
            put(n, xx, y, (3, 3, 5))
            put(n, xx, y + 1, (6, 6, 8), .85)
        for (x, y) in [(0, 0), (0, 1), (1, 2), (2, 3), (4, 4), (5, 4), (6, 4)]:        # creux autour de l'orbite
            xx = x if side == 0 else w - 1 - x
            put(n, xx, y, (12, 12, 15), .7)
    n[0, :] *= .75


def fx_cheek(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    for y in range(1, h - 1):
        for x in (2, w - 3):
            put(n, x, y, (6, 6, 8), .85)


def fx_slat(F, rng, glow):
    n = F['north']
    n[:, 0] *= .55
    n[:, -1] *= .55


def fx_ridge(F, rng, glow):
    for f in ('north', 'south'):
        F[f][:, F[f].shape[1] // 2] += 18


def fx_pauld(F, rng, glow):
    for f in ('north', 'south', 'east', 'west'):
        a = F[f]
        a[0, :] += 18
        a[-1, :] *= .55


def fx_breast(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    c = w // 2
    for y in range(h):
        put(n, c - 1, y, (0, 0, 0), .45); put(n, c, y, (0, 0, 0), .45)
    n[h - 1, :] *= .55
    for x in range(w):
        put(n, x, 0, (140, 142, 154), .25)


def fx_boot(F, rng, glow):
    F['north'][-1, :] *= .5


# ---------------------------------------------------------------- aides de construction
def C(name, x, y, z, w, h, d, mat, key=None, **kw):
    return Cube(name, x, y, z, w, h, d, mat, key=f'{key or name}_{w}x{h}x{d}_{mat}', **kw)


def horn(name, base, side, lens, widths, rz0, drz, rx0, drx, ry=0.0, mat='horn', dr=1.0):
    """corne = chaine de segments imbriques (chaque groupe tourne autour de la base de son segment), la courbure vient
    des angles cumules. side=+1 : cote +x, -1 : cote -x ; rz0 = angle de depart depuis la verticale (vers l'exterieur),
    drz = courbure ajoutee a chaque segment (negatif : la corne se redresse puis revient vers le centre), rx = bascule vers
    l'arriere, ry = torsion, dr = epaisseur (profondeur) relative a la largeur."""
    segs = []
    px, py, pz = base
    for i, (L, w) in enumerate(zip(lens, widths)):
        rz = (rz0 if i == 0 else drz) * -side
        rx = rx0 if i == 0 else drx
        d = max(1, int(round(w * dr)))
        ov = 1.0 if i else 0.0
        cube = C(f'{name}_{i}', px - w / 2, py - ov, pz - d / 2, w, L + ov, d, mat, key='bl')
        segs.append(([cube], (px, py, pz), (rx, (ry if i else 0) * -side, rz)))
        py += L
    g = None
    for i in range(len(segs) - 1, -1, -1):
        kids, piv, rot = segs[i]
        g = Group(f'{name}_{i}', piv, rot, kids + ([g] if g else []))
    return g


def arm_ik(shoulder, la, lf, target, init=(40, 0, 60, 0)):
    S = np.array(shoulder, float)

    def hand(a):
        R1 = rot_mat((a[0], 0, a[1]))
        R2 = rot_mat((a[2], 0, a[3]))
        E0 = S + np.array((0, -la, 0))
        H0 = E0 + np.array((0, -lf, 0))
        return S + R1 @ (E0 + R2 @ (H0 - E0) - S)

    def cost(a):
        return float(np.sum((hand(a) - np.array(target)) ** 2) + .004 * np.sum(np.array(a) ** 2))
    r = minimize(cost, init, method='Nelder-Mead', options={'xatol': 1e-3, 'fatol': 1e-4, 'maxiter': 4000})
    return r.x, hand(r.x)


# ---------------------------------------------------------------- modele
def blade(name, base, sg, length, w0, d0, rz0, drz, rx0, drx, step=2, prof=.85, mat='horn'):
    """lame effilee en forme de feuille : largeur ET epaisseur diminuent regulierement vers la pointe (pas de 1 px), courbure par
    angles cumules. rz0 : inclinaison de depart vers l'exterieur ; drz : courbure ajoutee par segment (negatif = revient vers le centre) ;
    rx0/drx : bascule vers l'arriere."""
    n = max(2, int(round(length / step)))
    lens, widths, deps = [], [], []
    for i in range(n):
        t = i / (n - 1)
        f = max(0.0, 1 - t) ** prof
        lens.append(step)
        widths.append(max(1, int(round(w0 * f))))
        deps.append(max(1, int(round(d0 * f))))
    segs = []
    px, py, pz = base
    for i in range(n):
        rz = (rz0 if i == 0 else drz) * -sg
        rx = rx0 if i == 0 else drx
        ov = 1.0 if i else 0.0
        w, d = widths[i], deps[i]
        cube = C(f'{name}_{i}', px - w / 2, py - ov, pz - d / 2, w, lens[i] + ov, d, mat, key='bl')
        segs.append(([cube], (px, py, pz), (rx, 0, rz)))
        py += lens[i]
    g = None
    for i in range(len(segs) - 1, -1, -1):
        kids, piv, rot = segs[i]
        g = Group(f'{name}_{i}', piv, rot, kids + ([g] if g else []))
    return g


def make_head(HY):
    """casque, version epuree d'apres les photos : crane ovoide lisse, visage long et facette qui s'effile en bec, grandes orbites
    obliques, arete nasale ; couronne de lames larges en feuille, bien espacees et toutes penchees de la meme facon."""
    k = []
    # crane ovoide lisse (couches de 2 px, arrondi), plus large que haut pour porter une couronne de lames espacees
    for i, (y, w, d, z0) in enumerate([(3, 14, 15, -5), (5, 16, 17, -6), (7, 18, 18, -7), (9, 18, 18, -7), (11, 18, 18, -7), (13, 18, 18, -7),
                                       (15, 18, 18, -7), (17, 16, 16, -6), (19, 14, 14, -5), (21, 10, 11, -3.5), (23, 6, 7, -1.5)]):
        k.append(C(f'skull_{i}', -w / 2, HY + y, z0, w, 2, d, 'dome', key=f'a_sk{i}'))
    k.append(C('dome_band', -9.5, HY + 15, -7.5, 19, 2, 19, 'plate', key='a_band'))
    k.append(C('dome_ridge', -1, HY + 24, -2, 2, 1, 11, 'plate', key='a_ridge'))
    # visage : six couches de 4 px, de plus en plus etroites et avancees (profil en bec)
    face = [(12, 16, -12.5, 'ash', None), (8, 15, -13.2, 'mask', fx_eyes), (4, 13, -13.9, 'plate', fx_cheek),
            (0, 11, -14.6, 'mask', None), (-4, 9, -15.3, 'plate', None), (-8, 6, -16.0, 'mask', None)]
    for i, (y, w, z0, m, fxf) in enumerate(face):
        k.append(C(f'face_{i}', -w / 2, HY + y, z0, w, 4, 6, m, key=f'a_f{i}', fx=fxf))
    k.append(C('nose_ridge', -1.5, HY - 9, -16.5, 3, 28, 3, 'plate', key='a_nose', fx=fx_ridge, rot=(10, 0, 0)))
    k.append(C('brow_l', -9, HY + 15, -14.5, 9, 2, 3, 'ashd', key='a_brow', rot=(0, 0, 22)))
    k.append(C('brow_r', 0, HY + 15, -14.5, 9, 2, 3, 'ashd', key='a_brow', rot=(0, 0, -22)))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k += [C('cheek_' + n, X0(sg, 9.5, 3), HY - 2, -11, 3, 16, 8, 'plate', key='a_cheek'),
              C('jaw_' + n, X0(sg, 6.6, 2), HY - 6, -13, 2, 8, 5, 'ash', key='a_jaw', rot=(0, 0, 8 * sg))]
    # nuque, capuche, gorgerin
    k += [C('hood', -9, HY - 12, -5, 18, 9, 13, 'ashd', key='a_hood'),
          C('hood_low', -11, HY - 14, -4, 22, 3, 14, 'ash', key='a_hoodl'),
          C('neck_back', -9.5, HY - 8, 11, 19, 15, 3, 'ash', key='a_neckb')]
    # couronne : trois paires de lames larges en feuille, espacees, penchees vers l'arriere
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        k.append(blade('horn_out_' + n, (sg * 11.5, HY + 9, -3), sg, 28, 6, 5, 8, -1.7, 7, .3, prof=.8))
        k.append(blade('horn_mid_' + n, (sg * 7.0, HY + 13, -6), sg, 23, 5, 4, 4, -.6, 11, .3, prof=.8))
        k.append(blade('horn_ctr_' + n, (sg * 2.6, HY + 16, -6), sg, 23, 4, 3, 1, -.2, 12, .3, prof=.8))
        k.append(blade('fin_' + n, (sg * 9.5, HY + 7, 5), sg, 10, 2, 7, 8, 0, 20, 2, prof=.7))
    return Group('head', (0, HY, 0), children=k)


def X0(sg, cx, w):
    return sg * cx - w / 2


def fx_scales(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    c = w // 2
    for y in range(h):
        put(n, c, y, (0, 0, 0), .35)


def build():
    """Seigneur de Cendre d'apres les photos : armure noire a plaques superposees, plastron nervure a panneau d'ecailles, tablier de
    mailles effile, epaulieres a eventail de lames, avant-bras a pointes, grande masse a deux ailerons, longue cape, casque a lames."""
    LH = 84                                    # hanches
    SY = 128                                   # epaules
    HY = 146                                   # cou (menton a HY-6)

    def X(sg, cx, w):
        return sg * cx - w / 2

    def leg(side):
        sg = -1 if side == 'left' else 1
        cx = 11
        k = [C('thigh_' + side, X(sg, cx, 15), 52, -8, 15, 32, 16, 'ash', key='a_thigh'),
             C('thigh_side_' + side, X(sg, cx + 8.5, 3), 54, -7, 3, 28, 14, 'plate', key='a_ths'),
             C('thigh_back_' + side, X(sg, cx, 13), 54, 8, 13, 28, 3, 'ashd', key='a_thb'),
             C('thigh_pl_a_' + side, X(sg, cx, 17), 74, -11.5, 17, 10, 4, 'plate', key='a_tpa', rot=(-10, 0, 0)),
             C('thigh_pl_b_' + side, X(sg, cx, 16), 63, -11.5, 16, 10, 4, 'ash', key='a_tpb', rot=(-10, 0, 0)),
             C('thigh_pl_c_' + side, X(sg, cx, 15), 53, -11.5, 15, 10, 4, 'plate', key='a_tpc', rot=(-10, 0, 0)),
             C('knee_' + side, X(sg, cx, 17), 42, -12, 17, 12, 7, 'plate', key='a_knee', rot=(-6, 0, 0)),
             C('knee_spike_a_' + side, X(sg, cx, 5), 45, -20, 5, 5, 9, 'steel', key='a_ksa'),
             C('knee_spike_b_' + side, X(sg, cx, 3), 46.5, -25, 3, 3, 6, 'steel', key='a_ksb'),
             C('shin_' + side, X(sg, cx, 13), 12, -7, 13, 30, 14, 'ash', key='a_shin'),
             C('shin_side_' + side, X(sg, cx + 7.5, 3), 14, -6, 3, 28, 12, 'plate', key='a_shs'),
             C('shin_back_' + side, X(sg, cx, 12), 16, 7, 12, 24, 3, 'ashd', key='a_shb'),
             C('shin_pl_a_' + side, X(sg, cx, 14), 34, -10.5, 14, 10, 3, 'plate', key='a_spa', rot=(-8, 0, 0)),
             C('shin_pl_b_' + side, X(sg, cx, 13), 24, -10.5, 13, 10, 3, 'ash', key='a_spb', rot=(-8, 0, 0)),
             C('shin_pl_c_' + side, X(sg, cx, 12), 15, -10.5, 12, 10, 3, 'plate', key='a_spc', rot=(-8, 0, 0)),
             C('ankle_' + side, X(sg, cx, 16), 10, -8, 16, 4, 18, 'plate', key='a_ank'),
             C('boot_' + side, X(sg, cx, 15), 0, -11, 15, 11, 24, 'ash', key='a_boot', fx=fx_boot),
             C('toe_a_' + side, X(sg, cx, 13), 0, -17, 13, 5, 7, 'plate', key='a_toea'),
             C('toe_b_' + side, X(sg, cx, 9), 0, -22, 9, 3, 6, 'ash', key='a_toeb')]
        return Group('leg_' + side, (sg * cx, LH, 0), children=k)

    # --- tassettes de hanche + long tablier de mailles qui s'effile
    pel = [C('back_fauld', -13, 60, 9, 26, 24, 3, 'ash', key='a_bfauld')]
    for i, (w, y) in enumerate([(24, 76), (22, 72), (20, 68), (18, 64), (16, 60), (14, 56), (11, 52), (8, 48), (5, 45), (2, 43)]):
        pel.append(C(f'apron_{i}', -w / 2, y, -14, w, 4, 2, 'mail', key=f'a_ap{i}'))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        pel += [C('tasset_a_' + n, X(sg, 19, 6), 64, -9, 6, 20, 17, 'plate', key='a_tsa', rot=(0, 0, 10 * sg)),
                C('tasset_b_' + n, X(sg, 22, 5), 52, -8, 5, 14, 15, 'ash', key='a_tsb', rot=(0, 0, 14 * sg)),
                C('tasset_tip_' + n, X(sg, 24, 3), 46, -4, 3, 7, 7, 'steel', key='a_tst', rot=(0, 0, 14 * sg))]
    pelvis = Group('pelvis', (0, LH, 0), children=pel)

    head = make_head(HY)

    # --- epaulieres : quatre lames superposees, dome, eventail de lames courbes
    def pauldron(side):
        sg = -1 if side == 'left' else 1
        k = [C('pa_dome_' + side, X(sg, 31, 16), SY + 6, -10, 16, 4, 20, 'plate', key='a_pad', fx=fx_pauld),
             C('pa_top_' + side, X(sg, 34, 24), SY, -14, 24, 6, 28, 'ash', key='a_pat', fx=fx_pauld),
             C('pa_2_' + side, X(sg, 37, 22), SY - 6, -13, 22, 6, 26, 'plate', key='a_pa2', fx=fx_pauld),
             C('pa_3_' + side, X(sg, 39, 20), SY - 12, -12, 20, 6, 24, 'ash', key='a_pa3', fx=fx_pauld),
             C('pa_4_' + side, X(sg, 40, 16), SY - 18, -10, 16, 6, 20, 'plate', key='a_pa4', fx=fx_pauld),
             C('pa_front_' + side, X(sg, 31, 14), SY - 2, -19, 14, 10, 5, 'plate', key='a_paf', rot=(-16, 0, 0)),
             C('pa_back_' + side, X(sg, 31, 14), SY - 2, 14, 14, 10, 5, 'plate', key='a_pab', rot=(16, 0, 0))]
        for i, (ang, z, ln) in enumerate([(-6, -9, 22), (6, -3, 30), (18, 3, 28), (30, 9, 18)]):
            k.append(blade(f'pspike_{i}_{side}', (sg * (27 + i * 1.5), SY + 9, z), sg, ln, 4, 3, ang, 0.5, 10, 1.2))
        return Group('pauldron_' + side, (sg * 27, SY, 0), children=k)

    # --- bras
    la, lf = 30.0, 28.0

    def arm(side, target, init, claws):
        sg = -1 if side == 'left' else 1
        S = (sg * 28, SY - 3, 0)
        ang, _ = arm_ik(S, la, lf, target, init=init)
        ax = lambda w: sg * 28 - w / 2
        y0 = SY - 3
        fk = [C('elbow_' + side, ax(17), y0 - la - 5, -9, 17, 11, 18, 'plate', key='a_elb'),
              C('elbow_spike_a_' + side, ax(5), y0 - la - 2, 8, 5, 5, 11, 'steel', key='a_esa'),
              C('elbow_spike_b_' + side, ax(3), y0 - la - 0.5, 18, 3, 3, 8, 'steel', key='a_esb'),
              C('forearm_' + side, ax(14), y0 - la - lf + 6, -7, 14, lf - 8, 14, 'ash', key='a_fore'),
              C('fore_ring_a_' + side, ax(16), y0 - la - 12, -8, 16, 5, 16, 'plate', key='a_fra'),
              C('fore_ring_b_' + side, ax(16), y0 - la - 19, -8, 16, 5, 16, 'ash', key='a_frb'),
              C('fore_ring_c_' + side, ax(15), y0 - la - 25, -7.5, 15, 4, 15, 'plate', key='a_frc'),
              C('gauntlet_' + side, ax(15), y0 - la - lf - 6, -8, 15, 14, 16, 'ash', key='a_gau'),
              C('gauntlet_cuff_' + side, ax(17), y0 - la - lf + 4, -9, 17, 4, 18, 'plate', key='a_gcuff')]
        for i in range(4):                       # rangee de pointes sur le cote exterieur de l'avant-bras
            xo = sg * (28 + 8) if sg > 0 else sg * (28 + 8) - 6
            fk.append(C(f'fore_spike_{i}_{side}', xo, y0 - la - 6 - i * 5.5, -1, 6, 2, 2, 'steel', key='a_fsp'))
        if claws:                                 # main ouverte : quatre griffes courbes
            for i, fxo in enumerate([-5.5, -1.8, 1.8, 5.5]):
                fk.append(blade(f'claw_{i}_{side}', (sg * 28 + fxo, y0 - la - lf - 6, -10), 1, 12, 3, 3, 0, 0, 180 - 12 * (i % 2), -9, step=2, prof=.8))
        return Group('upper_arm_' + side, S, rot=(ang[0], 0, ang[1]), children=[
            C('upper_arm_' + side, ax(15), y0 - la, -7.5, 15, la, 15, 'ash', key='a_up'),
            C('upper_pl_a_' + side, ax(17), y0 - 12, -9, 17, 6, 18, 'plate', key='a_upa'),
            C('upper_pl_b_' + side, ax(17), y0 - 21, -9, 17, 6, 18, 'ash', key='a_upb'),
            Group('forearm_' + side, (sg * 28, y0 - la, 0), rot=(ang[2], 0, ang[3]), children=fk)])

    # --- masse : long manche, tete a deux grands ailerons en eventail (comme sur la photo), tenue vers l'avant et le bas
    GR = (36.0, 76.0, -16.0)
    mp = GR
    mace_rot = (-128, 0, -6)
    mk = [C('haft', mp[0] - 2.5, mp[1] - 22, mp[2] - 2.5, 5, 108, 5, 'leath', key='m_haft'),
          C('haft_butt', mp[0] - 3.5, mp[1] - 26, mp[2] - 3.5, 7, 5, 7, 'plate', key='m_butt'),
          C('haft_ring_a', mp[0] - 3.5, mp[1] + 10, mp[2] - 3.5, 7, 3, 7, 'steel', key='m_ra'),
          C('haft_ring_b', mp[0] - 3.5, mp[1] + 60, mp[2] - 3.5, 7, 3, 7, 'steel', key='m_ra'),
          C('mace_collar', mp[0] - 5, mp[1] + 80, mp[2] - 5, 10, 4, 10, 'steel', key='m_coll'),
          C('mace_core', mp[0] - 6, mp[1] + 84, mp[2] - 6, 12, 18, 12, 'ash', key='m_core')]
    prof = [7, 13, 18, 21, 22, 20, 15, 9]
    for j, hw in enumerate(prof):                 # aileron A (plan x) et aileron B (plan z) : feuilles larges, plates
        y = mp[1] + 82 + j * 3.4
        mk.append(C(f'fan_a_{j}', mp[0] - hw, y, mp[2] - 1, hw * 2, 3, 2, 'plate' if j % 2 else 'ash', key='m_fa'))
        mk.append(C(f'fan_b_{j}', mp[0] - 1, y, mp[2] - hw * .8, 2, 3, round(hw * 1.6), 'ash' if j % 2 else 'plate', key='m_fb'))
    mk.append(blade('mace_tip', (mp[0], mp[1] + 102, mp[2]), 1, 16, 4, 4, 0, 0, 0, 0, step=2))
    mace = Group('mace', mp, mace_rot, children=mk)

    # --- cape longue, en pans articules
    def pan(name, ytop, hgt, w, nxt=None, tails=False):
        kids = [C(name + '_b', -w / 2, ytop - hgt, 15, w, hgt, 3, 'cape', key='c_' + name)]
        if tails:
            for i, (x, tw, th) in enumerate([(-w / 2, 12, 9), (-w / 2 + 11, 12, 15), (-w / 2 + 22, 12, 8), (w / 2 - 34, 12, 16), (w / 2 - 23, 12, 10), (w / 2 - 12, 12, 13)]):
                kids.append(C(f'{name}_tail_{i}', x, ytop - hgt - th, 15, tw, th, 3, 'cape', key=f'c_t{i}'))
        if nxt:
            kids.append(nxt)
        return Group(name, (0, ytop, 16.5), rot=(2, 0, 0), children=kids)
    cape_e = pan('cape_e', 36, 28, 56, tails=True)
    cape_d = pan('cape_d', 64, 28, 52, cape_e)
    cape_c = pan('cape_c', 92, 28, 48, cape_d)
    cape = Group('cape', (0, SY + 2, 16.5), rot=(4, 0, 0), children=[
        C('cape_top', -22, 92, 15, 44, 38, 3, 'cape', key='c_top'),
        C('cape_collar', -15, SY + 2, 10, 30, 12, 5, 'cape', key='c_collar'),
        C('cape_yoke', -26, SY - 6, -13, 52, 7, 28, 'cape', key='c_yoke'),
        cape_c])

    body_kids = [
        C('waist', -11, 84, -7, 22, 16, 14, 'ashd', key='a_waist'),
        C('belt', -14, 82, -10, 28, 5, 20, 'leath', key='a_belt'),
        C('buckle', -6, 81, -11.5, 12, 8, 2, 'plate', key='a_buckle'),
        C('buckle_gem', -2, 84, -12.5, 4, 3, 1, 'steel', key='a_gem'),
        C('abs_mail', -7, 88, -14, 14, 20, 3, 'mail', key='a_amail'),
        C('chest', -23, 100, -13, 46, 32, 26, 'ash', key='a_chest'),
        C('chest_mid', -9, 103, -18, 18, 25, 5, 'mail', key='a_cmid', fx=fx_scales, rot=(-4, 0, 0)),
        C('chest_keel', -2, 104, -20, 4, 24, 3, 'steel', key='a_keel', fx=fx_ridge),
        C('back_plate', -21, 100, 13, 42, 30, 4, 'ash', key='a_back'),
        C('back_ridge', -2, 102, 17, 4, 28, 3, 'plate', key='a_bridge'),
        C('gorget', -13, 132, -10, 26, 7, 20, 'plate', key='a_gorget'),
        C('collar_front', -5, 136, -13, 10, 11, 3, 'ash', key='a_colf', rot=(-8, 0, 0)),
        C('collar_back', -11, 134, 10, 22, 14, 3, 'plate', key='a_colb', rot=(8, 0, 0)),
        head]
    for i, y in enumerate([88, 93, 98]):         # lames d'abdomen : plus larges vers le haut, bord inferieur en pointe
        w = 26 + i * 3
        body_kids.append(C(f'abs_pl_{i}', -w / 2, y, -11, w, 5, 22, 'plate' if i % 2 == 0 else 'ash', key=f'a_abp{i}', rot=(-5, 0, 0)))
        body_kids.append(C(f'abs_tip_{i}', -3, y - 3, -13, 6, 4, 4, 'plate', key=f'a_abt{i}', rot=(-5, 0, 0)))
    for sg in (-1, 1):
        n = 'l' if sg < 0 else 'r'
        body_kids += [C('chest_wing_hi_' + n, X(sg, 13, 16), 122, -17, 16, 8, 4, 'plate', key='a_cwh', rot=(0, 0, 26 * sg)),
                      C('chest_wing_lo_' + n, X(sg, 13, 14), 102, -17, 14, 7, 4, 'plate', key='a_cwl', rot=(0, 0, -20 * sg)),
                      C('chest_edge_' + n, X(sg, 17, 10), 112, -16, 10, 10, 3, 'ash', key='a_ced', rot=(-5, 0, 0)),
                      C('collar_side_' + n, X(sg, 11, 3), 134, -4, 3, 13, 12, 'plate', key='a_cols', rot=(0, 0, -10 * sg))]
    body_kids += [pauldron('left'), pauldron('right'),
                  arm('left', (-34, 78, -10), (20, 6, 40, 4), True), arm('right', GR, (35, -10, 60, -10), False),
                  mace, cape]
    body = Group('body', (0, LH, 0), children=body_kids)
    return [leg('left'), leg('right'), pelvis, body]


def check_names(groups):
    seen = set()

    def rec(gs):
        for g in gs:
            assert g.name not in seen, 'nom de groupe en double : ' + g.name
            seen.add(g.name)
            rec([c for c in g.children if isinstance(c, Group)])
    rec(groups)


def generate(outdir):
    _N[0] = 0
    groups = build()
    normalize(groups)
    check_names(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    export_bbmodel('ash_lord', groups, cubes, W, H, tex, os.path.join(outdir, 'ash_lord.bbmodel'))
    Image.fromarray(tex, 'RGBA').save(os.path.join(outdir, 'ash_lord_texture.png'))
    return groups, tex, glow


def preview(path, groups=None, tex=None, big=False):
    if groups is None:
        groups, tex, _ = generate('/tmp')
    views = [0, 35, 90, 180]
    ims = [render(groups, tex, y, pitch=-8, W=470, H=860, scale=3.9) for y in views]
    Image.fromarray(np.concatenate(ims, axis=1)).save(path)


def head_preview(path):
    _N[0] = 0
    groups = [make_head(77)]
    normalize(groups)
    cubes = [c for c, _ in walk(groups)]
    W, H = pack(cubes)
    tex, glow = paint_all(cubes, W, H)
    export_bbmodel('ash_lord_head', groups, cubes, W, H, tex, '/tmp/ash_lord_head.bbmodel')
    sc, Wd, Hd = 13, 560, 1900
    ims = []
    for yaw in (0, 30, 90):
        img = render(groups, tex, yaw, pitch=-6, W=Wd, H=Hd, scale=sc)
        y1 = int(Hd - 40 - 60 * sc); y0 = int(Hd - 40 - 122 * sc)
        ims.append(img[max(0, y0):y1])
    h = min(i.shape[0] for i in ims)
    Image.fromarray(np.concatenate([i[:h] for i in ims], axis=1)).save(path)


if __name__ == '__main__':
    import sys
    g, t, gl = generate('/tmp')
    print('texture', t.shape, 'cubes', len(list(walk(g))))
    preview(sys.argv[1] if len(sys.argv) > 1 else '/tmp/ash.png', g, t)
