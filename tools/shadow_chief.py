"""Maitre des ombres : le chef qui chevauche le dragon, mob a part. Humanoide en armure noire a casque a pointes, grande cape, epee pale.
Pieces animables (GeoModel) : head, arm_l/r, leg_l/r, cape_0..3. 1 unite = 5 cm (echelle de rendu 0.8), pieds a y=0, avant = -z.
Assis sur le dragon, les jambes sont ployees par GeoModel.masterPose."""
import math
import numpy as np
import trollcore as T
from trollcore import Group
from orcs import C, unique_names
import weapons3d as W
import dragon_noir as D
import shadow_dragon as SD
from dragon_noir import P, bone

EYE = SD.EYE


def master_parts():
    K = []
    K.append(C('torso', -4.5, 21, -3, 9, 13, 6, 'dr_cloak', key='torso'))
    K.append(C('belt', -5, 21, -3.5, 10, 2, 7, 'dr_helm', key='belt'))
    K.append(C('chest', -4, 25, -3.6, 8, 8, 1, 'dr_helm', key='chest'))
    K.append(C('buckle', -1, 21, -4.1, 2, 2, 1, 'dr_steel', key='mbuckle'))
    for sg in (-1, 1):
        side = 'l' if sg < 0 else 'r'
        leg = [C(f'thigh{side}', sg * 3.5 - 2, 11, -2.5, 4, 10, 5, 'dr_cloak', key='thigh'),
               C(f'shin{side}', sg * 3.5 - 2, 3, -2.5, 4, 8, 5, 'dr_cloak', key='shin'),
               C(f'kneep{side}', sg * 3.5 - 2.5, 9, -3.5, 5, 3, 2, 'dr_helm', key='kneep'),
               C(f'boot{side}', sg * 3.5 - 2.5, 0, -4.5, 5, 3, 7, 'dr_helm', key='boot')]
        K.append(Group(f'leg_{side}', (sg * 3.5, 21, 0), (0, 0, 0), leg))
        arm = [C(f'uarm{side}', sg * 6.5 - 2, 22, -2, 4, 11, 4, 'dr_cloak', key='uarm'),
               C(f'pauld{side}', sg * 6.5 - 2.5, 32, -3, 5, 3, 6, 'dr_helm', key='pauld'),
               bone(f'pspike{side}', P(sg * 7.5, 35, 0), P(sg * 9.5, 39.5, 0), 1, 'dr_helm', key='pspike'),
               C(f'glove{side}', sg * 6.5 - 1.5, 21, -1.5, 3, 2, 3, 'dr_helm', key='glove')]
        if sg > 0:                                              # epee pale tenue le long de la jambe, pointe en avant
            hand = P(sg * 6.5, 21.5, 0)
            arm += [bone('sword', hand + P(0, 1, 0), P(sg * 7.2, 5, -9), 2, 'dr_blade', key='sword', d=1),
                    C('guard', sg * 6.5 - 3, 20, -1, 6, 1, 2, 'dr_steel', key='guard'),
                    C('swgrip', sg * 6.5 - 1, 22, -1, 2, 4, 2, 'dr_cloak', key='swgrip')]
        K.append(Group(f'arm_{side}', (sg * 6.5, 33, 0), (0, 0, 0), arm))
    # tete : casque plein a pointes, fente lumineuse
    hk = [C('rhead', -3.5, 34, -3.5, 7, 7, 7, 'dr_helm', key='rhead'),
          C('rvisor', -3, 37, -4.1, 6, 1, 1, 'dr_glow', key='rvisor', fx=EYE)]
    for i in range(8):
        a = math.radians(i * 45)
        base = P(math.cos(a) * 3.2, 41, math.sin(a) * 3.2)
        hk.append(bone(f'helmsp{i}', base, base + P(math.cos(a) * 1.6, 4.5, math.sin(a) * 1.6), 1, 'dr_helm', key='helmsp'))
    hk.append(bone('helmsp_c', P(0, 41, 0), P(0, 48, 0), 2, 'dr_helm', key='helmspc'))
    K.append(Group('head', (0, 34, 0), (0, 0, 0), hk))

    def cape(i):
        top = 33 - i * 7.5
        kids = [C(f'capep{i}', -5 + i * .6, top - 8, 3.2 + i * 1.2, 10 - i * 1.4, 8, 1, 'dr_cloak', key=f'capep{i}')]
        if i < 3:
            kids.append(cape(i + 1))
        return Group(f'cape_{i}', (0, top, 3.2 + i * 1.2), (-14 if i == 0 else -7, 0, 0), kids)
    K.append(cape(0))
    return [Group('body', (0, 0, 0), (0, 0, 0), K)]


def make():
    groups = master_parts()
    T._N[0] = 0
    unique_names(groups)
    T.normalize(groups)
    lo, hi = W.extents(groups)
    cubes, Wd, Ht, tex, glow = SD.finalize(groups)
    return groups, cubes, Wd, Ht, tex, glow, lo, hi


if __name__ == '__main__':
    import sys, os, bbimport
    from PIL import Image
    out = sys.argv[1] if len(sys.argv) > 1 else '/tmp/shadow_chief'
    os.makedirs(out, exist_ok=True)
    groups, cubes, Wd, Ht, tex, glow, lo, hi = make()
    T.export_bbmodel('shadow_chief', groups, cubes, Wd, Ht, tex, os.path.join(out, 'shadow_chief.bbmodel'))
    geo = bbimport.convert(os.path.join(out, 'shadow_chief.bbmodel'))
    print('pieces', len(geo['parts']), 'cubes', len(cubes), 'texture', Wd, Ht, 'etendue', np.round(hi - lo, 1), 'lo', np.round(lo, 1))
    D.preview(groups, tex, os.path.join(out, 'apercu.png'), lo, hi)
