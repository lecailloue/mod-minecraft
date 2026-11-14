"""Dragon des ombres (monture du chef des ombres) : modele de jeu, hierarchique (cou, tete, machoire, queue, ailes, cavalier, cape).
Reprend le dragon noir de tools/dragon_noir.py mais en groupes imbriques pour l'animation.
Pieces animables (GeoModel.dragonPose) : body, dneck_0..6, head, jaw, dtail_0..6, dwing_l/r (+ dwing_l2/r2 : doigts), rider, cape_0..3.
1 unite = 5 cm (echelle de rendu 0.8). y haut, sol y=0 (griffes), avant = -z, gauche = -x."""
import math
import numpy as np
import trollcore as T
from trollcore import Group
from orcs import C, unique_names
import weapons3d as W
import dragon_noir as D
from dragon_noir import P, bone, lerp

EYE = W.fx_gem(D.VIO)
T.MATS.update({
    'dr_cloak': dict(base=(18, 18, 24), n1=4, n2=5, tint=(1, 1, 1.05)),
    'dr_helm': dict(base=(44, 44, 54), n1=5, n2=5, tint=(1, 1, 1.05)),
})
T.DECOR.update({'dr_cloak': T.d_cloth, 'dr_helm': D.d_bonep})


def euler_zyx(R):
    """angles (rx, ry, rz) en degres tels que rot_mat((rx, ry, rz)) = R (ordre Rz @ Ry @ Rx)"""
    b = -math.asin(max(-1, min(1, R[2, 0])))
    if abs(math.cos(b)) < 1e-6:
        return (0.0, math.degrees(b), math.degrees(math.atan2(-R[0, 1], R[1, 1])))
    return (math.degrees(math.atan2(R[2, 1], R[2, 2])), math.degrees(b), math.degrees(math.atan2(R[1, 0], R[0, 0])))


def slat(name, A, B, depth, mat, key=None):
    """barre de membrane plate (epaisseur 1) de A vers B : un seul cube tourne (tilt puis lacet), donc une seule piece"""
    A, B = np.array(A, float), np.array(B, float)
    d = B - A
    L = max(2, int(round(np.linalg.norm(d))))
    M = (A + B) / 2
    yaw = math.atan2(-d[2], d[0])
    tilt = math.atan2(d[1], math.hypot(d[0], d[2]))
    Ry = np.array([[math.cos(yaw), 0, math.sin(yaw)], [0, 1, 0], [-math.sin(yaw), 0, math.cos(yaw)]])
    Rz = np.array([[math.cos(tilt), -math.sin(tilt), 0], [math.sin(tilt), math.cos(tilt), 0], [0, 0, 1]])
    rot = euler_zyx(Ry @ Rz)
    return C(name, M[0] - L / 2, M[1] - .5, M[2] - depth / 2, L, 1, max(1, int(depth)), mat, key=key or name, rot=rot, pivot=tuple(M))


def chain(prefix, pts, ws, mat, key, extras=None, last=None, first_extra=None):
    """chaine de groupes imbriques : groupe i pivote en pts[i] et porte l'os i (+ extras(i)) puis le groupe i+1"""
    def make(i):
        kids = [bone(f'{prefix}b{i}', pts[i], pts[i + 1], ws[i], mat, key=f'{key}{i}')]
        if extras:
            kids += extras(i)
        if i + 1 < len(pts) - 1:
            kids.append(make(i + 1))
        elif last:
            kids.append(last)
        return Group(f'{prefix}_{i}', tuple(pts[i]), (0, 0, 0), kids)
    return make(0)


def dragon_parts(rng):
    body = []
    # --- tronc
    body += [C('torso', -11, 37, -12, 22, 20, 32, 'dr_skin', key='torso'),
             C('chest', -9, 33, -19, 18, 20, 9, 'dr_plate', key='chest'),
             C('belly', -8, 32, -8, 16, 5, 26, 'dr_plate', key='belly'),
             C('back', -8, 57, -12, 16, 3, 30, 'dr_plate', key='backp')]
    for i, z in enumerate((-9, -4, 1, 6, 11)):
        for sg in (-1, 1):
            body.append(C(f'rib{i}_{sg}', sg * 11 + (0 if sg > 0 else -2), 40, z - 1, 2, 14, 3, 'dr_plate', key=f'rib{i}'))

    # --- cou (7 segments) + tete + machoire
    neck = [P(0, 50, -15), P(0, 54, -26), P(0, 59, -38), P(0, 63, -50), P(0, 66, -62), P(0, 67, -73), P(0, 67, -82), P(0, 66, -87)]
    Hd = P(0, 66, -87)
    head_kids = [C('cranium', -6, Hd[1] - 6, Hd[2] - 9, 12, 12, 12, 'dr_plate', key='cranium'),
                 C('cranium_top', -5, Hd[1] + 5, Hd[2] - 8, 10, 2, 10, 'dr_plate', key='cranium_t'),
                 C('cranium_back', -5, Hd[1] - 4, Hd[2] + 1, 10, 8, 2, 'dr_skin', key='cranium_b')]
    for sg in (-1, 1):
        head_kids.append(C(f'cheek{sg}', sg * 6 + (0 if sg > 0 else -2), Hd[1] - 5, Hd[2] - 7, 2, 8, 8, 'dr_skin', key='cheek'))
        head_kids.append(C(f'eye{sg}', sg * 6.2 - .5, Hd[1] - 1, Hd[2] - 8, 1, 3, 3, 'dr_glow', key='eye', fx=EYE))
    head_kids += D.taper('snout', [P(0, Hd[1] - 1, Hd[2] - 9), P(0, Hd[1] - 3, Hd[2] - 14), P(0, Hd[1] - 5, Hd[2] - 18)], [9, 8], 'dr_plate', key='snout')
    for i in range(5):
        for sg in (-1, 1):
            head_kids.append(C(f'tooth_u{i}_{sg}', sg * 3 - .5, Hd[1] - 5.5 - i * .3, Hd[2] - 11 - i * 1.4, 1, 2, 1, 'dr_bone', key='tooth'))
    jaw_piv = P(0, Hd[1] - 5, Hd[2] - 8)
    jaw_kids = D.taper('jawb', [P(0, Hd[1] - 5, Hd[2] - 8), P(0, Hd[1] - 8, Hd[2] - 13), P(0, Hd[1] - 10, Hd[2] - 18)], [7, 6], 'dr_skin', key='jaw')
    for i in range(5):
        for sg in (-1, 1):
            jaw_kids.append(C(f'tooth_l{i}_{sg}', sg * 3 - .5, Hd[1] - 8.5 - i * .3, Hd[2] - 11 - i * 1.4, 1, 2, 1, 'dr_bone', key='tooth2'))
    head_kids.append(Group('jaw', tuple(jaw_piv), (0, 0, 0), jaw_kids))
    head = Group('head', tuple(Hd), (0, 0, 0), head_kids)

    def neck_extras(i):
        return [C(f'nplate{i}', -3, neck[i][1] + 4, neck[i][2] - 2, 6, 1, 4, 'dr_plate', key='nplate')] if 1 <= i <= 5 else []
    body.append(chain('dneck', neck, [12, 10, 9, 8, 8, 7, 7], 'dr_skin', 'neck', extras=neck_extras, last=head))

    # --- queue (7 segments)
    tail = [P(0, 46, 18), P(0, 43, 28), P(0, 40, 38), P(0, 40, 48), P(0, 43, 58), P(0, 48, 66), P(0, 53, 72), P(0, 56, 77)]
    body.append(chain('dtail', tail, [10, 9, 8, 7, 5, 4, 3], 'dr_skin', 'tail'))

    # --- pattes
    for sg in (-1, 1):
        hip = P(sg * 10, 44, 10)
        body += D.taper(f'hleg{sg}_', [hip, P(sg * 13, 35, 14), P(sg * 11, 26, 9), P(sg * 11, 21, 3)], [8, 6, 5], 'dr_skin', key='hleg')
        for c in range(3):
            fo = P(sg * (11 + (c - 1) * 2.4), 21, 3)
            body.append(bone(f'hclaw{sg}_{c}', fo, fo + P(sg * (c - 1) * 1.2, -4, -5), 2, 'dr_bone', key='hcl'))
        sh = P(sg * 10, 42, -10)
        body += D.taper(f'fleg{sg}_', [sh, P(sg * 12, 35, -9), P(sg * 10, 28, -15), P(sg * 10, 25, -19)], [7, 5, 4], 'dr_skin', key='fleg')
        for c in range(3):
            fo = P(sg * (10 + (c - 1) * 2.2), 25, -19)
            body.append(bone(f'fclaw{sg}_{c}', fo, fo + P(sg * (c - 1) * 1.2, -4, -4), 2, 'dr_bone', key='fcl'))

    # --- ailes : groupe dwing_x (epaule) > bras + membrane interne + groupe dwing_x2 (poignet) > doigts + eventail de membrane
    for sg in (-1, 1):
        side = 'l' if sg < 0 else 'r'
        S, E, Wr = P(sg * 10, 54, -6), P(sg * 25, 64, -2), P(sg * 42, 77, -13)
        tips = [P(sg * 79, 81, -28), P(sg * 77, 67, 2), P(sg * 64, 58, 23), P(sg * 48, 52, 36)]
        hipw = P(sg * 12, 47, 17)
        arm = D.taper(f'warm{side}', [S, E, Wr], [6, 5], 'dr_skin', key='warm')
        arm.append(C(f'wsh{side}', sg * 10 - 3.5, 50, -9.5, 7, 9, 7, 'dr_plate', key='wsh'))
        outer = []
        for i, tp in enumerate(tips):
            mid = lerp(Wr, tp, .55) + P(0, -3 - i * .8, 2 + i)
            outer += D.taper(f'wf{side}{i}_', [Wr, mid, tp], [3, 2], 'dr_bone', key=f'wf{i}')
        lines = [Wr] + tips + [hipw]
        for g in range(len(lines) - 2):
            Ae, Be = lines[g + 1], lines[g + 2]
            nb = 6
            for s_ in range(nb):
                t = (s_ + .5) / nb
                depth = max(3, int(min(np.linalg.norm(Ae - Wr), np.linalg.norm(Be - Wr)) / nb))
                a, b = lerp(Wr, Ae, t), lerp(Wr, Be, t)
                if s_ >= nb - 2:
                    b = lerp(b, a, rng.random() * .22)
                    if rng.random() < .35:
                        a = lerp(a, b, rng.random() * .18)
                outer.append(slat(f'mem{side}{g}_{s_}', a, b, depth, 'dr_mem', key=f'mem{g}{s_}{depth}'))
        inner = []
        for s_ in range(5):
            t = (s_ + .5) / 5
            a = lerp(S, Wr, t * .96 + .02) + P(0, -2, 1); b = lerp(S, hipw, t) + P(0, -1, 0)
            inner.append(slat(f'mi{side}{s_}', a, b, 4, 'dr_mem', key=f'mi{s_}'))
        body.append(Group(f'dwing_{side}', tuple(S), (0, 0, 0), arm + inner + [Group(f'dwing_{side}2', tuple(Wr), (0, 0, 0), outer)]))
    return body


def rider_parts():
    """le chef des ombres, assis sur le dos du dragon : cape noire qui flotte, casque a pointes, rênes"""
    K = []
    K.append(C('saddle', -6, 59, -9, 12, 2, 14, 'dr_plate', key='saddle'))
    for sg in (-1, 1):
        K.append(C(f'thigh{sg}', sg * 3.5 - 1.5, 61, -11, 3, 3, 10, 'dr_cloak', key='thigh'))
        K.append(C(f'shin{sg}', sg * 7.5 - 1.5, 52, -12, 3, 10, 3, 'dr_cloak', key='shin'))
        K.append(C(f'boot{sg}', sg * 7.5 - 2, 50, -14, 4, 3, 5, 'dr_helm', key='boot'))
    K.append(C('torso_r', -4, 63, -7, 8, 13, 5, 'dr_cloak', key='rtorso'))
    K.append(C('belt', -4, 62, -7, 8, 1, 5, 'dr_helm', key='belt'))
    for sg in (-1, 1):                                                        # epaulieres a pointes
        K.append(C(f'pauld{sg}', sg * 5.5 - 1.5, 74, -7, 3, 3, 5, 'dr_helm', key='pauld'))
        K.append(bone(f'pspike{sg}', P(sg * 6.5, 77, -4.5), P(sg * 8.5, 81, -4.5), 1, 'dr_helm', key='pspike'))
        K.append(bone(f'rarm{sg}', P(sg * 5.5, 73, -4.5), P(sg * 6, 68, -10), 3, 'dr_cloak', key='rarm'))
        K.append(bone(f'rfore{sg}', P(sg * 6, 68, -10), P(sg * 2.5, 66, -16), 3, 'dr_cloak', key='rfore'))
        K.append(bone(f'rein{sg}', P(sg * 2.5, 66, -16), P(sg * 1.2, 56, -22), 1, 'dr_helm', key='rein'))
    # tete : casque plein a pointes, fente lumineuse
    K.append(C('rhead', -3.5, 76, -7, 7, 7, 7, 'dr_helm', key='rhead'))
    K.append(C('rvisor', -3, 79, -7.6, 6, 1, 1, 'dr_glow', key='rvisor', fx=EYE))
    for i in range(8):
        a = math.radians(i * 45)
        base = P(math.cos(a) * 3.2, 83, -3.5 + math.sin(a) * 3.2)
        K.append(bone(f'helmsp{i}', base, base + P(math.cos(a) * 1.6, 4.5, math.sin(a) * 1.6), 1, 'dr_helm', key='helmsp'))
    K.append(bone('helmsp_c', P(0, 83, -3.5), P(0, 90, -3.5), 2, 'dr_helm', key='helmspc'))
    # cape : 4 panneaux articules qui trainent derriere
    def cape(i):
        top = 74 - i * 7.5
        kids = [C(f'capep{i}', -5 + i * .6, top - 8, -2.5 + i * 1.2, 10 - i * 1.4, 8, 1, 'dr_cloak', key=f'capep{i}')]
        if i < 3:
            kids.append(cape(i + 1))
        return Group(f'cape_{i}', (0, top, -2.5 + i * 1.2), (-14 if i == 0 else -7, 0, 0), kids)
    K.append(cape(0))
    return [Group('rider', (0, 64, -5), (0, 0, 0), K)]


def build():
    rng = np.random.default_rng(7)
    body = dragon_parts(rng) + rider_parts()
    root = Group('body', (0, 50, 0), (0, 0, 0), body)
    T._N[0] = 0
    groups = [root]
    unique_names(groups)
    T.normalize(groups)
    lo, hi = W.extents(groups)
    return groups, lo, hi


def finalize(groups):
    cubes = [c for c, _ in T.walk(groups)]
    for c in cubes:
        fr, sz = list(c.frm), list(c.size)
        for i in range(3):
            if sz[i] < 0:
                fr[i] += sz[i]; sz[i] = -sz[i]
        c.frm, c.size = tuple(fr), tuple(max(1, v) for v in sz)
    Wd, Ht = T.pack(cubes)
    tex, glow = T.paint_all(cubes, Wd, Ht)
    return cubes, Wd, Ht, tex, glow


def shift_to_ground(groups, dy):
    """descend tout de dy (griffes a y = 0)"""
    def mv(g):
        g.pivot = (g.pivot[0], g.pivot[1] + dy, g.pivot[2])
        for ch in g.children:
            if isinstance(ch, Group):
                mv(ch)
            else:
                ch.frm = (ch.frm[0], ch.frm[1] + dy, ch.frm[2])
                ch.pivot = (ch.pivot[0], ch.pivot[1] + dy, ch.pivot[2])
    for g in groups:
        mv(g)


def make():
    groups, lo, hi = build()
    shift_to_ground(groups, -lo[1])
    lo, hi = W.extents(groups)
    cubes, Wd, Ht, tex, glow = finalize(groups)
    return groups, cubes, Wd, Ht, tex, glow, lo, hi


if __name__ == '__main__':
    import sys, os, json, bbimport, models
    from PIL import Image
    out = sys.argv[1] if len(sys.argv) > 1 else '/tmp/shadow_dragon'
    os.makedirs(out, exist_ok=True)
    groups, cubes, Wd, Ht, tex, glow, lo, hi = make()
    T.export_bbmodel('shadow_dragon', groups, cubes, Wd, Ht, tex, os.path.join(out, 'shadow_dragon.bbmodel'))
    geo = bbimport.convert(os.path.join(out, 'shadow_dragon.bbmodel'))
    print('pieces', len(geo['parts']), 'cubes', len(cubes), 'texture', Wd, Ht, 'etendue', np.round(hi - lo, 1))
    Image.fromarray(tex, 'RGBA').save(os.path.join(out, 'shadow_dragon.png'))
    Image.fromarray(glow, 'RGBA').save(os.path.join(out, 'shadow_dragon_glow.png'))
    import dragon_noir
    dragon_noir.preview(groups, tex, os.path.join(out, 'apercu.png'), lo, hi)
