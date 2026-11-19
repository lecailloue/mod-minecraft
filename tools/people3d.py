#!/usr/bin/env python3
"""Nains et elfes tres detailles : le squelette de joueur (armures et objets en main continuent de marcher) + beaucoup de pieces 3D en plus.
Une seule description (PARTS) sert a : generer PeopleParts.java, peindre les zones de texture (peau 128x128), et faire l'apercu 3D.
Usage : python3 tools/people3d.py [dwarf_1 elf_2 ...]  -> apercus dans /tmp/people/"""
import math, os, sys
import numpy as np
from PIL import Image
from tex import rgb, shade, mix

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA = os.path.join(ROOT, 'src/main/java/net/ringsofash/client/PeopleParts.java')
SIZE = 256

PARTS = {'dwarf': [], 'elf': []}
PAIRED = {'dwarf': set(), 'elf': set()}
MIRROR_PARENT = {'right_arm': 'left_arm', 'right_leg': 'right_leg'}


def P(kind, grp, name, parent, mat, dims, at, g=0.0, off=(0, 0, 0), rot=(0, 0, 0), pair=False, style='flat'):
    PARTS[kind].append(dict(kind=kind, grp=grp, name=name, parent=parent, mat=mat, dims=dims, at=at, g=g, off=off, rot=rot, pair=pair, style=style))
    if pair: PAIRED[kind].add(name)


def expand(kind):
    """liste finale (une entree par cube), les pieces symetriques donnent _r et _l (la gauche est le miroir de la droite)"""
    out = []
    for s in PARTS[kind]:
        if not s['pair']:
            out.append(dict(s, mirror=False, full=s['name'])); continue
        pr = s['parent'] + '_r' if s['parent'] in PAIRED[kind] else s['parent']
        out.append(dict(s, parent=pr, name=s['name'] + '_r', mirror=False, full=s['name'] + '_r'))
        w = s['dims'][0]
        pl = {'right_arm': 'left_arm', 'right_leg': 'left_leg'}.get(s['parent'], s['parent'])
        if s['parent'] in PAIRED[kind]: pl = s['parent'] + '_l'
        out.append(dict(s, parent=pl, name=s['name'] + '_l', mirror=True, full=s['name'] + '_l',
                        at=(-(s['at'][0] + w), s['at'][1], s['at'][2]), off=(-s['off'][0], s['off'][1], s['off'][2]), rot=(s['rot'][0], -s['rot'][1], -s['rot'][2])))
    return out


# =====================================================================================================  NAINS
D = 'dwarf'
# --- visage
P(D, 'face', 'nose', 'head', 'skinD', (3, 3, 2), (-1.5, -3.8, -5.7), style='skin')
P(D, 'face', 'nose_tip', 'head', 'skinD', (2, 1, 1), (-1.0, -2.4, -6.2), g=0.1, style='skin')
P(D, 'face', 'ear', 'head', 'skin', (1, 3, 2), (-5.0, -4.6, -1.0), pair=True, style='skin')
P(D, 'brows', 'brow', 'head', 'brow', (4, 2, 1), (-3.9, -7.0, -4.9), g=0.15, pair=True, style='hair')
P(D, 'brows', 'brow_ridge', 'head', 'skinD', (8, 1, 1), (-4.0, -5.4, -4.5), g=-0.1, style='skin')
# --- moustache et barbe en etages, tresses
P(D, 'mous', 'mous', 'head', 'beard', (4, 2, 2), (-4.2, -3.1, -5.2), pair=True, style='hair')
P(D, 'mous', 'mous_tip', 'head', 'beard', (1, 2, 2), (-5.0, -4.3, -4.6), g=-0.1, pair=True, style='hair')
P(D, 'beard', 'beard0', 'head', 'beard', (8, 3, 2), (-4.0, -2.0, -4.9), style='hair')
P(D, 'beard', 'beard1', 'head', 'beard', (7, 3, 2), (-3.5, 1.0, -4.5), style='hair')
P(D, 'beard_l', 'beard2', 'head', 'beardD', (6, 3, 2), (-3.0, 4.0, -4.1), style='hair')
P(D, 'beard_l', 'beard3', 'head', 'beard', (5, 3, 2), (-2.5, 7.0, -3.7), style='hair')
P(D, 'beard_tip', 'beard4', 'head', 'beardD', (3, 3, 2), (-1.5, 10.0, -3.4), style='hair')
P(D, 'braid', 'braid', 'head', 'beard', (2, 6, 2), (-4.9, 2.0, -4.0), pair=True, style='braid')
P(D, 'braid', 'braid_ring', 'head', 'gold', (2, 1, 2), (-4.9, 7.8, -4.0), g=0.3, pair=True, style='metal')
P(D, 'braid', 'braid_tip', 'head', 'beardD', (2, 2, 2), (-4.9, 8.8, -4.0), g=-0.3, pair=True, style='braid')
P(D, 'braid', 'beard_ring', 'head', 'gold', (6, 1, 2), (-3.0, 5.4, -4.2), g=0.25, style='metal')
# --- cheveux
P(D, 'dh_big', 'hair_top', 'head', 'hair', (8, 1, 8), (-4.0, -9.3, -4.0), g=0.15, style='hair')
P(D, 'dh_big', 'hair_back', 'head', 'hair', (8, 8, 2), (-4.0, -8.2, 3.7), style='hair')
P(D, 'dh_big', 'hair_side', 'head', 'hair', (2, 6, 5), (-5.3, -6.8, -2.4), pair=True, style='hair')
P(D, 'dh_big', 'hair_nape', 'head', 'hair', (6, 3, 2), (-3.0, -0.4, 3.4), style='hair')
# --- casque de garde (crete, nasal, joues)
P(D, 'helm', 'crest', 'head', 'metal', (2, 3, 10), (-1.0, -11.2, -5.0), style='metal')
P(D, 'helm', 'crest_fin', 'head', 'metal', (2, 2, 4), (-1.0, -12.4, -2.0), style='metal')
P(D, 'helm', 'nasal', 'head', 'metal', (1, 5, 1), (-0.5, -8.5, -5.0), style='metal')
P(D, 'helm', 'cheek', 'head', 'metal', (1, 5, 5), (-5.0, -5.8, -3.5), pair=True, style='metal')
P(D, 'helm', 'helm_rim', 'head', 'gold', (8, 1, 8), (-4.0, -7.4, -4.0), g=0.5, style='metal')
# --- corps : ceinture, bourses, tabliers, sac a dos avec couchage et pioche
P(D, 'belt', 'belt', 'body', 'leather', (8, 2, 4), (-4.0, 9.2, -2.0), g=0.4, style='leather')
P(D, 'belt', 'buckle', 'body', 'gold', (3, 2, 1), (-1.5, 9.2, -2.9), g=0.15, style='metal')
P(D, 'belt', 'pouch', 'body', 'leather', (2, 3, 3), (-5.7, 9.6, -1.5), pair=True, style='leather')
P(D, 'belt', 'axe_loop', 'body', 'leather', (1, 4, 1), (-5.2, 9.6, 1.6), pair=True, style='leather')
P(D, 'tabard', 'tab_front', 'body', 'cloth', (6, 4, 1), (-3.0, 11.2, -2.9), g=0.1, style='cloth')
P(D, 'tabard', 'tab_back', 'body', 'cloth', (6, 4, 1), (-3.0, 11.2, 1.9), g=0.1, style='cloth')
P(D, 'pack', 'pack', 'body', 'leather', (7, 7, 3), (-3.5, 3.4, 2.0), style='leather')
P(D, 'pack', 'pack_flap', 'body', 'leather', (7, 2, 3), (-3.5, 3.0, 2.2), g=0.3, style='leather')
P(D, 'pack', 'roll', 'body', 'roll', (8, 3, 3), (-4.0, 0.2, 2.1), g=0.2, style='cloth')
P(D, 'pack', 'roll_strap', 'body', 'leather', (1, 3, 3), (-2.5, 0.2, 2.1), g=0.35, pair=True, style='leather')
P(D, 'pack', 'pick_shaft', 'body', 'wood', (1, 11, 1), (2.4, 1.0, 5.2), style='wood')
P(D, 'pack', 'pick_head', 'body', 'metal', (7, 1, 2), (-0.2, 0.2, 4.7), g=0.1, style='metal')
P(D, 'pack', 'pick_tip', 'body', 'metal', (1, 2, 2), (6.5, 0.7, 4.7), g=0.1, style='metal')
P(D, 'pack', 'pick_tip2', 'body', 'metal', (1, 2, 2), (-0.9, 0.7, 4.7), g=0.1, style='metal')
# --- epaules, avant-bras, jambes
P(D, 'pauldron', 'pauld', 'right_arm', 'metal', (6, 2, 6), (-4.0, -3.0, -3.0), pair=True, style='metal')
P(D, 'pauldron', 'pauld2', 'right_arm', 'metal', (5, 1, 5), (-3.5, -1.0, -2.5), pair=True, style='metal')
P(D, 'pauldron', 'pauld_spike', 'right_arm', 'gold', (1, 2, 1), (-1.5, -5.0, -0.5), pair=True, style='metal')
P(D, 'bracer', 'bracer', 'right_arm', 'metal', (4, 3, 4), (-3.0, 6.4, -2.0), g=0.4, pair=True, style='metal')
P(D, 'bracer', 'bracer_rim', 'right_arm', 'gold', (4, 1, 4), (-3.0, 6.0, -2.0), g=0.42, pair=True, style='metal')
P(D, 'cuff', 'cuff', 'right_leg', 'fur', (4, 2, 4), (-2.0, 8.4, -2.0), g=0.45, pair=True, style='hair')
P(D, 'cuff', 'toe', 'right_leg', 'metal', (4, 2, 1), (-2.0, 10.2, -3.0), g=0.15, pair=True, style='metal')
P(D, 'knee', 'kneepad', 'right_leg', 'metal', (4, 2, 1), (-2.0, 3.4, -2.8), g=0.3, pair=True, style='metal')

# --- nains : details supplementaires
P(D, 'sideburn', 'sideburn', 'head', 'beard', (1, 5, 3), (-4.9, -3.4, -3.6), pair=True, style='hair')
P(D, 'beard', 'beard_tuft0', 'head', 'beardD', (1, 2, 2), (-5.0, 1.4, -4.3), pair=True, style='hair')
P(D, 'beard_l', 'beard_tuft1', 'head', 'beard', (1, 2, 2), (-4.4, 4.6, -3.9), pair=True, style='hair')
P(D, 'beard_l', 'beard_tuft2', 'head', 'beardD', (1, 2, 2), (-3.8, 7.6, -3.5), pair=True, style='hair')
P(D, 'beard', 'beard_chin', 'head', 'beardD', (4, 1, 1), (-2.0, -0.5, -5.5), g=0.1, style='hair')
P(D, 'beard_tip', 'beard_tip_ring', 'head', 'gold', (3, 1, 2), (-1.5, 9.6, -3.4), g=0.25, style='metal')
P(D, 'topknot', 'topknot', 'head', 'hair', (3, 2, 3), (-1.5, -11.0, -1.0), g=0.15, style='hair')
P(D, 'topknot', 'topknot_band', 'head', 'gold', (3, 1, 3), (-1.5, -9.8, -1.0), g=0.4, style='metal')
P(D, 'hair_braid', 'nape_braid', 'head', 'hair', (2, 8, 2), (-3.4, -1.0, 5.7), pair=True, style='braid')
P(D, 'hair_braid', 'nape_braid_tie', 'head', 'gold', (2, 1, 2), (-3.4, 6.2, 5.7), g=0.3, pair=True, style='metal')
P(D, 'hair_braid', 'nape_braid_end', 'head', 'hairD', (2, 2, 2), (-3.4, 7.2, 5.7), g=-0.25, pair=True, style='hair')
P(D, 'helm', 'horn', 'head', 'bone', (2, 3, 2), (-1.0, -3.0, -1.0), off=(-4.9, -7.2, 0.0), rot=(0, 0, 0.45), pair=True, style='metal')
P(D, 'helm', 'horn_tip', 'horn', 'bone', (1, 3, 1), (-0.5, -3.0, -0.5), off=(0, -2.8, 0.0), rot=(0, 0, 0.65), pair=True, style='metal')
P(D, 'helm', 'brow_plate', 'head', 'metal', (8, 1, 1), (-4.0, -8.4, -5.0), g=0.35, style='metal')
P(D, 'collar', 'fur_collar', 'body', 'fur', (10, 2, 6), (-5.0, -1.3, -3.0), g=0.25, style='hair')
P(D, 'collar', 'fur_collar_back', 'body', 'fur', (8, 3, 2), (-4.0, -2.2, 1.8), g=0.2, style='hair')
P(D, 'harness', 'strap', 'body', 'leather', (1, 9, 1), (-2.8, 0.2, -2.6), g=0.15, pair=True, style='leather')
P(D, 'harness', 'strap_ring', 'body', 'gold', (1, 1, 1), (-2.8, 4.2, -2.8), g=0.3, pair=True, style='metal')
P(D, 'harness', 'medal', 'body', 'gold', (2, 2, 1), (-1.0, 2.6, -2.9), g=0.15, style='metal')
P(D, 'harness', 'medal_gem', 'body', 'gem', (1, 1, 1), (-0.5, 3.1, -3.3), g=0.1, style='gem')
P(D, 'mug', 'mug', 'body', 'wood', (3, 3, 3), (1.4, 9.6, -3.8), g=0.15, style='wood')
P(D, 'mug', 'mug_band', 'body', 'metal', (3, 1, 3), (1.4, 10.4, -3.8), g=0.3, style='metal')
P(D, 'mug', 'mug_handle', 'body', 'metal', (1, 2, 1), (4.5, 10.0, -2.8), style='metal')
P(D, 'pot', 'pot', 'body', 'metal', (3, 3, 3), (3.6, 7.4, 2.4), g=0.1, style='metal')
P(D, 'pot', 'pot_lid', 'body', 'metal', (3, 1, 3), (3.6, 6.8, 2.4), g=0.3, style='metal')
P(D, 'pot', 'rope', 'body', 'roll', (3, 3, 1), (-6.4, 6.6, 2.7), g=0.15, style='cloth')
P(D, 'gloves', 'glove', 'right_arm', 'leather', (4, 3, 4), (-3.0, 9.8, -2.0), g=0.3, pair=True, style='leather')
P(D, 'gloves', 'knuckle', 'right_arm', 'metal', (4, 1, 1), (-3.0, 10.0, -2.5), g=0.15, pair=True, style='metal')
P(D, 'strapb', 'bootstrap', 'right_leg', 'leather', (4, 1, 4), (-2.0, 9.6, -2.0), g=0.4, pair=True, style='leather')
P(D, 'strapb', 'bootbuckle', 'right_leg', 'gold', (1, 1, 1), (-0.5, 9.6, -2.6), g=0.3, pair=True, style='metal')
P(D, 'strapb', 'boot_sole', 'right_leg', 'leather', (4, 1, 5), (-2.0, 11.2, -3.0), g=0.15, pair=True, style='leather')

# --- nains : tabliers, casquettes de mineur, chapeaux, capuches, couronne, cache-oeil
P(D, 'apron', 'apron', 'body', 'leatherD', (7, 9, 1), (-3.5, 4.0, -2.9), g=0.05, style='leather')
P(D, 'apron', 'apron_pocket', 'body', 'leather', (3, 2, 1), (-1.5, 9.4, -3.3), g=0.05, style='leather')
P(D, 'apron', 'apron_strap', 'body', 'leather', (1, 4, 1), (-2.6, -0.2, -2.8), g=0.12, pair=True, style='leather')
P(D, 'apron', 'apron_hem', 'body', 'leatherD', (7, 2, 1), (-3.5, 13.0, -2.9), g=0.05, style='leather')
P(D, 'cap', 'cap', 'head', 'metal', (8, 2, 8), (-4.0, -9.0, -4.0), g=0.6, style='metal')
P(D, 'cap', 'cap_rim', 'head', 'metal', (8, 1, 1), (-4.0, -7.6, -5.0), g=0.3, style='metal')
P(D, 'cap', 'lamp', 'head', 'gold', (2, 2, 1), (-1.0, -9.2, -5.7), g=0.15, style='metal')
P(D, 'cap', 'lamp_glass', 'head', 'lampglow', (1, 1, 1), (-0.5, -8.7, -6.1), g=0.1, style='gem')
P(D, 'hat', 'brim', 'head', 'cloth', (10, 1, 10), (-5.0, -8.6, -5.0), g=0.1, style='cloth')
P(D, 'hat', 'crown_hat', 'head', 'cloth', (6, 3, 6), (-3.0, -11.6, -3.0), g=0.15, style='cloth')
P(D, 'hat', 'hat_band', 'head', 'trim', (6, 1, 6), (-3.0, -9.6, -3.0), g=0.3, style='cloth')
P(D, 'hat', 'hat_feather', 'head', 'fletch2', (1, 3, 1), (-3.4, -12.2, -0.5), g=0.0, rot=(0.0, 0.0, 0.35), style='cloth')
P(D, 'scout_hood', 'sh_top', 'head', 'cape', (10, 1, 10), (-5.0, -9.7, -5.0), g=0.1, style='cloth')
P(D, 'scout_hood', 'sh_back', 'head', 'cape', (10, 10, 2), (-5.0, -9.2, 4.2), style='cloth')
P(D, 'scout_hood', 'sh_side', 'head', 'cape', (1, 9, 9), (-5.6, -9.0, -4.5), pair=True, style='cloth')
P(D, 'scout_hood', 'sh_tip', 'head', 'capeD', (3, 4, 3), (-1.5, -4.0, 6.0), style='cloth')
P(D, 'scout_hood', 'sh_front', 'head', 'capeD', (10, 1, 1), (-5.0, -9.2, -5.4), g=0.1, style='cloth')
P(D, 'crown', 'crown_ring', 'head', 'gold', (8, 1, 8), (-4.0, -8.9, -4.0), g=0.45, style='metal')
P(D, 'crown', 'crown_prong', 'head', 'gold', (1, 3, 1), (-3.6, -11.6, -4.4), pair=True, style='metal')
P(D, 'crown', 'crown_mid', 'head', 'gold', (1, 4, 1), (-0.5, -12.4, -4.6), style='metal')
P(D, 'crown', 'crown_gem', 'head', 'gem', (1, 1, 1), (-0.5, -9.2, -5.0), g=0.15, style='gem')
P(D, 'eyepatch', 'patch', 'head', 'dark', (2, 2, 1), (1.0, -5.8, -4.7), g=0.05, style='leather')
P(D, 'eyepatch', 'patch_strap', 'head', 'dark', (8, 1, 1), (-4.0, -6.6, -4.4), g=-0.15, style='leather')
P(D, 'earring', 'earring', 'head', 'gold', (1, 1, 1), (-5.3, -2.4, 0.0), g=0.15, pair=True, style='metal')

# --- nains : styles de coiffure, de barbe, de casque, vetements et armure en plus
P(D, 'dh_short', 'dcap', 'head', 'hair', (8, 1, 8), (-4.0, -9.2, -4.0), g=0.12, style='hair')
P(D, 'dh_short', 'dback_s', 'head', 'hair', (8, 4, 2), (-4.0, -8.2, 3.7), style='hair')
P(D, 'dh_short', 'dside_s', 'head', 'hair', (1, 4, 4), (-4.7, -8.0, -2.0), pair=True, style='hair')
P(D, 'dh_mohawk', 'mohawk', 'head', 'hair', (2, 3, 10), (-1.0, -11.2, -5.0), style='hair')
P(D, 'dh_mohawk', 'mohawk_c', 'head', 'hairD', (2, 2, 10), (-1.0, -9.4, -5.0), g=-0.2, style='hair')
P(D, 'dh_mohawk', 'mohawk_b', 'head', 'hair', (2, 5, 2), (-1.0, -8.4, 4.0), style='hair')
P(D, 'dh_long', 'dlong_cap', 'head', 'hair', (8, 1, 8), (-4.0, -9.2, -4.0), g=0.12, style='hair')
P(D, 'dh_long', 'dlong_back', 'head', 'hair', (8, 12, 2), (-4.0, -8.2, 3.7), style='hair')
P(D, 'dh_long', 'dlong_side', 'head', 'hair', (2, 9, 3), (-5.2, -6.5, -1.5), pair=True, style='hair')
P(D, 'bs_fork', 'fork_a', 'head', 'beard', (3, 6, 2), (-3.8, 3.6, -4.2), pair=True, style='hair')
P(D, 'bs_fork', 'fork_b', 'head', 'beardD', (2, 4, 2), (-3.4, 9.4, -3.8), pair=True, style='hair')
P(D, 'bs_fork', 'fork_ring', 'head', 'gold', (3, 1, 2), (-3.8, 8.4, -4.2), g=0.25, pair=True, style='metal')
P(D, 'bs_square', 'sq0', 'head', 'beard', (8, 3, 2), (-4.0, -2.0, -4.9), style='hair')
P(D, 'bs_square', 'sq1', 'head', 'beardD', (8, 3, 2), (-4.0, 1.0, -4.5), style='hair')
P(D, 'bs_square', 'sq2', 'head', 'beard', (8, 3, 2), (-4.0, 4.0, -4.1), style='hair')
P(D, 'bs_square', 'sq3', 'head', 'beardD', (8, 3, 2), (-4.0, 7.0, -3.7), style='hair')
P(D, 'bs_square', 'sq_ring', 'head', 'gold', (8, 1, 2), (-4.0, 5.6, -4.1), g=0.2, style='metal')
P(D, 'bs_braid', 'bb_c', 'head', 'beard', (2, 9, 2), (-1.0, 1.0, -4.6), style='braid')
P(D, 'bs_braid', 'bb_s', 'head', 'beardD', (2, 8, 2), (-3.6, 1.2, -4.4), pair=True, style='braid')
P(D, 'bs_braid', 'bb_tc', 'head', 'gold', (2, 1, 2), (-1.0, 7.8, -4.6), g=0.3, style='metal')
P(D, 'bs_braid', 'bb_ts', 'head', 'gold', (2, 1, 2), (-3.6, 7.6, -4.4), g=0.3, pair=True, style='metal')
P(D, 'bs_braid', 'bb_top', 'head', 'beard', (8, 3, 2), (-4.0, -2.0, -4.9), style='hair')
P(D, 'bs_goatee', 'gt0', 'head', 'beard', (4, 3, 2), (-2.0, -1.8, -5.0), style='hair')
P(D, 'bs_goatee', 'gt1', 'head', 'beardD', (3, 4, 2), (-1.5, 1.2, -4.6), style='hair')
P(D, 'bs_goatee', 'gt2', 'head', 'beard', (2, 3, 2), (-1.0, 5.2, -4.2), style='hair')
P(D, 'bs_goatee', 'gt_ring', 'head', 'gold', (3, 1, 2), (-1.5, 4.6, -4.6), g=0.2, style='metal')
P(D, 'dmod', 'dm_ring', 'head', 'metal', (8, 4, 8), (-4.0, -8.0, -4.0), g=0.55, style='metal')
P(D, 'dmod', 'dm_dome', 'head', 'metal', (6, 2, 6), (-3.0, -9.5, -3.0), g=0.45, style='metal')
P(D, 'dmod', 'dm_ridge', 'head', 'gold', (1, 1, 7), (-0.5, -11.5, -3.5), style='metal')
P(D, 'dmod', 'dm_cheek', 'head', 'gold', (1, 4, 4), (-4.3, -4.0, -3.0), g=0.3, pair=True, style='metal')
P(D, 'dmod', 'dm_nose', 'head', 'gold', (1, 4, 1), (-0.5, -4.0, -4.6), g=0.2, style='metal')
P(D, 'dmod', 'dm_neck', 'head', 'metal', (8, 3, 1), (-4.0, -2.6, 3.4), style='metal')
P(D, 'ho', 'ho_dome', 'head', 'metal', (8, 3, 8), (-4.0, -9.6, -4.0), g=0.55, style='metal')
P(D, 'ho', 'ho_rim', 'head', 'gold', (8, 1, 8), (-4.0, -7.6, -4.0), g=0.62, style='metal')
P(D, 'ho', 'ho_nasal', 'head', 'metal', (1, 5, 1), (-0.5, -8.0, -5.1), style='metal')
P(D, 'ho', 'ho_flap', 'head', 'metal', (1, 4, 3), (-5.1, -6.4, -2.0), pair=True, style='metal')
P(D, 'ho', 'ho_stud', 'head', 'gold', (2, 1, 2), (-1.0, -11.0, -1.0), style='metal')
P(D, 'ht', 'ht_a', 'head', 'metal', (8, 2, 8), (-4.0, -9.2, -4.0), g=0.5, style='metal')
P(D, 'ht', 'ht_b', 'head', 'metal', (6, 2, 6), (-3.0, -11.0, -3.0), g=0.35, style='metal')
P(D, 'ht', 'ht_c', 'head', 'metal', (4, 2, 4), (-2.0, -12.8, -2.0), g=0.2, style='metal')
P(D, 'ht', 'ht_spike', 'head', 'gold', (1, 3, 1), (-0.5, -15.4, -0.5), style='metal')
P(D, 'ht', 'ht_rim', 'head', 'gold', (8, 1, 8), (-4.0, -7.6, -4.0), g=0.58, style='metal')
P(D, 'ht', 'ht_cheek', 'head', 'metal', (1, 5, 4), (-5.0, -6.2, -3.0), pair=True, style='metal')
P(D, 'ht', 'ht_brow', 'head', 'metal', (8, 1, 1), (-4.0, -8.0, -4.9), g=0.3, style='metal')
P(D, 'coat', 'coat_f', 'body', 'cloth', (8, 9, 1), (-4.0, 11.2, -3.0), g=0.05, style='cloth')
P(D, 'coat', 'coat_b', 'body', 'cloth', (8, 9, 1), (-4.0, 11.2, 2.0), g=0.05, style='cloth')
P(D, 'coat', 'coat_s', 'body', 'clothD', (1, 9, 3), (-4.9, 11.2, -1.5), pair=True, style='cloth')
P(D, 'coat', 'coat_hem', 'body', 'trim', (8, 1, 1), (-4.0, 19.6, -3.0), g=0.1, style='cloth')
P(D, 'coat', 'coat_hem_b', 'body', 'trim', (8, 1, 1), (-4.0, 19.6, 2.0), g=0.1, style='cloth')
P(D, 'robe', 'drobe_f', 'body', 'cloth', (8, 11, 1), (-4.0, 11.2, -3.0), g=0.05, style='cloth')
P(D, 'robe', 'drobe_b', 'body', 'cloth', (8, 11, 1), (-4.0, 11.2, 2.0), g=0.05, style='cloth')
P(D, 'robe', 'drobe_s', 'body', 'clothD', (1, 10, 3), (-4.9, 11.6, -1.5), pair=True, style='cloth')
P(D, 'robe', 'drobe_hem', 'body', 'trim', (8, 1, 1), (-4.0, 21.6, -3.0), g=0.1, style='cloth')
P(D, 'robe', 'drobe_hem_b', 'body', 'trim', (8, 1, 1), (-4.0, 21.6, 2.0), g=0.1, style='cloth')
P(D, 'vest', 'vest_f', 'body', 'leather', (8, 8, 1), (-4.0, 0.8, -3.0), g=0.05, style='leather')
P(D, 'vest', 'vest_lace', 'body', 'trim', (1, 7, 1), (-0.5, 1.2, -3.5), style='cloth')
P(D, 'vest', 'vest_b', 'body', 'leather', (8, 8, 1), (-4.0, 0.8, 2.0), g=0.05, style='leather')
P(D, 'fcloak', 'fcloak', 'body', 'fur', (9, 12, 2), (-4.5, -1.0, 2.0), g=0.15, style='hair')
P(D, 'fcloak', 'fcloak_t', 'body', 'fur', (9, 2, 3), (-4.5, -1.8, 1.5), g=0.2, style='hair')
P(D, 'dsash', 'sash_d', 'body', 'trim', (2, 15, 1), (-1.0, -7.5, 0.0), off=(0.0, 6.0, -3.1), rot=(0, 0, -0.5), style='cloth')
P(D, 'dsash', 'sash_knot', 'body', 'gold', (2, 2, 1), (-1.0, 5.0, 0.0), off=(0.0, 6.0, -3.2), rot=(0, 0, -0.5), g=0.1, style='metal')
P(D, 'dcuir', 'cu_breast', 'body', 'metal', (8, 6, 1), (-4.0, 1.0, -3.0), g=0.1, style='metal')
P(D, 'dcuir', 'cu_belly', 'body', 'metal', (8, 3, 1), (-4.0, 7.0, -3.0), g=0.1, style='metal')
P(D, 'dcuir', 'cu_ridge', 'body', 'gold', (2, 9, 1), (-1.0, 1.0, -3.5), g=0.05, style='metal')
P(D, 'dcuir', 'cu_back', 'body', 'metal', (8, 9, 1), (-4.0, 1.0, 2.0), g=0.1, style='metal')
P(D, 'dcuir', 'cu_gorget', 'body', 'metal', (8, 2, 5), (-4.0, -1.6, -2.5), g=0.35, style='metal')
P(D, 'dfauld', 'fauld', 'body', 'metal', (3, 4, 1), (-4.0, 10.4, -3.0), g=0.1, pair=True, style='metal')
P(D, 'dfauld', 'fauld_b', 'body', 'metal', (3, 4, 1), (-4.0, 10.4, 2.0), g=0.1, pair=True, style='metal')
P(D, 'pspike', 'pspike', 'right_arm', 'metal', (1, 2, 1), (-3.2, -5.2, -2.5), pair=True, style='metal')
P(D, 'pspike', 'pspike2', 'right_arm', 'metal', (1, 2, 1), (-3.2, -5.2, 1.5), pair=True, style='metal')
P(D, 'pspike', 'pspike3', 'right_arm', 'metal', (1, 3, 1), (-3.4, -6.0, -0.5), pair=True, style='metal')
P(D, 'dgreave', 'greave_d', 'right_leg', 'metal', (4, 4, 1), (-2.0, 4.4, -3.0), g=0.1, pair=True, style='metal')

# =====================================================================================================  ELFES
E = 'elf'
# --- oreilles longues (base + pointe)
P(E, 'ears', 'ear', 'head', 'skin', (1, 4, 2), (-0.5, -2.0, -1.0), off=(-4.4, -3.4, 0.8), rot=(0.0, 0.0, 0.5), pair=True, style='skin')
P(E, 'ears', 'ear_tip', 'ear', 'skin', (1, 4, 1), (-0.5, -4.0, -0.5), off=(0, -1.8, 0.0), rot=(-0.55, 0.0, 0.4), pair=True, style='skin')
P(E, 'face', 'nose', 'head', 'skinD', (1, 2, 1), (-0.5, -3.2, -4.8), style='skin')
# --- cheveux : calotte, meches de face, cascade dans le dos
P(E, 'hcap', 'hair_top', 'head', 'hair', (8, 1, 8), (-4.0, -9.1, -4.0), g=0.15, style='hair')
P(E, 'hback', 'hair_back', 'head', 'hair', (8, 8, 2), (-4.0, -8.0, 3.6), style='hair')
P(E, 'hside', 'hair_part', 'head', 'hair', (1, 6, 3), (-4.8, -7.6, -3.5), pair=True, style='hair')
P(E, 'locks', 'lock', 'head', 'hair', (1, 9, 2), (-4.9, -4.0, -3.2), pair=True, style='hair')
P(E, 'locks', 'lock_bead', 'head', 'gold', (1, 1, 2), (-4.9, 1.0, -3.2), g=0.2, pair=True, style='metal')
P(E, 'locks', 'lock_end', 'head', 'hairD', (1, 2, 2), (-4.9, 5.0, -3.2), g=-0.2, pair=True, style='hair')
P(E, 'cascade', 'casc0', 'body', 'hair', (8, 4, 2), (-4.0, -0.2, 2.3), style='hair')
P(E, 'cascade', 'casc1', 'body', 'hairD', (7, 4, 2), (-3.5, 3.8, 2.5), style='hair')
P(E, 'cascade', 'casc2', 'body', 'hair', (6, 4, 2), (-3.0, 7.8, 2.4), style='hair')
P(E, 'cascade', 'casc3', 'body', 'hairD', (5, 4, 2), (-2.5, 11.8, 2.6), style='hair')
P(E, 'cascade', 'casc4', 'body', 'hair', (3, 3, 2), (-1.5, 15.8, 2.5), g=-0.1, style='hair')
# --- diademe, fibule
P(E, 'circlet', 'band', 'head', 'gold', (8, 1, 1), (-4.0, -7.4, -4.6), g=0.2, style='metal')
P(E, 'circlet', 'band_side', 'head', 'gold', (1, 1, 8), (-4.4, -7.4, -4.0), g=0.2, pair=True, style='metal')
P(E, 'circlet', 'gem', 'head', 'gem', (1, 2, 1), (-0.5, -8.7, -4.9), g=0.15, style='gem')
P(E, 'circlet', 'prong', 'head', 'gold', (1, 2, 1), (-2.7, -8.6, -4.7), g=-0.1, pair=True, style='metal')
# --- cape et mantelet
P(E, 'mantle', 'mantle', 'body', 'cape', (12, 2, 6), (-6.0, -1.0, -3.0), g=0.1, style='cloth')
P(E, 'cape_l', 'cape_back', 'body', 'cape', (8, 16, 1), (-4.0, 0.6, 2.2), g=0.2, style='cloth')
P(E, 'cape_l', 'cape_hem', 'body', 'capeD', (8, 1, 1), (-4.0, 16.6, 2.2), g=0.2, style='cloth')
P(E, 'mantle', 'brooch', 'body', 'gold', (2, 2, 1), (-1.0, -0.3, -3.2), g=0.1, style='metal')
P(E, 'mantle', 'brooch_gem', 'body', 'gem', (1, 1, 1), (-0.5, 0.1, -3.5), g=0.1, style='gem')
# --- tunique : jupons, ceinture, echarpe
P(E, 'skirt', 'skirt_f', 'body', 'cloth', (6, 6, 1), (-3.0, 11.0, -2.6), g=0.1, style='cloth')
P(E, 'skirt', 'skirt_f2', 'body', 'clothD', (4, 7, 1), (-2.0, 11.2, -3.0), style='cloth')
P(E, 'skirt', 'skirt_b', 'body', 'cloth', (6, 6, 1), (-3.0, 11.0, 1.6), g=0.1, style='cloth')
P(E, 'belt', 'belt', 'body', 'leather', (8, 1, 4), (-4.0, 10.0, -2.0), g=0.3, style='leather')
P(E, 'belt', 'sash', 'body', 'trim', (1, 5, 1), (2.2, 10.4, -3.0), style='cloth')
P(E, 'belt', 'leafbuckle', 'body', 'gold', (2, 1, 1), (-1.0, 10.0, -2.7), g=0.2, style='metal')
# --- bras, jambes
P(E, 'bracer', 'bracer', 'right_arm', 'leather', (4, 4, 4), (-3.0, 5.6, -2.0), g=0.3, pair=True, style='leather')
P(E, 'bracer', 'bracer_gold', 'right_arm', 'gold', (4, 1, 4), (-3.0, 5.4, -2.0), g=0.4, pair=True, style='metal')
P(E, 'pauldron', 'pauld', 'right_arm', 'gold', (5, 2, 5), (-3.5, -2.8, -2.5), g=0.3, pair=True, style='metal')
P(E, 'pauldron', 'pauld_leaf', 'right_arm', 'gold', (3, 1, 3), (-2.5, -3.6, -1.5), pair=True, style='metal')
P(E, 'boots', 'boot', 'right_leg', 'leather', (4, 5, 4), (-2.0, 7.0, -2.0), g=0.3, pair=True, style='leather')
P(E, 'boots', 'boot_cuff', 'right_leg', 'trim', (4, 1, 4), (-2.0, 6.6, -2.0), g=0.45, pair=True, style='cloth')
P(E, 'boots', 'boot_toe', 'right_leg', 'leather', (3, 2, 2), (-1.5, 10.0, -3.4), g=-0.1, pair=True, style='leather')
# --- carquois
P(E, 'quiver', 'quiver', 'body', 'leather', (3, 10, 3), (-1.5, -5.0, -1.5), off=(2.2, 5.0, 4.4), rot=(0, 0, -0.3), style='leather')
P(E, 'quiver', 'quiver_rim', 'body', 'gold', (3, 1, 3), (-1.5, -5.5, -1.5), g=0.25, off=(2.2, 5.0, 4.4), rot=(0, 0, -0.3), style='metal')
P(E, 'quiver', 'fletch', 'body', 'fletch', (1, 3, 1), (-1.0, -8.4, -1.0), off=(2.2, 5.0, 4.4), rot=(0, 0, -0.3), pair=False, style='cloth')
P(E, 'quiver', 'fletch2', 'body', 'fletch2', (1, 3, 1), (0.0, -8.0, 0.0), off=(2.2, 5.0, 4.4), rot=(0, 0, -0.3), pair=False, style='cloth')

# --- elfes : details supplementaires
P(E, 'face', 'brow', 'head', 'hairD', (3, 1, 1), (-3.5, -6.7, -4.5), g=-0.25, pair=True, style='hair')
P(E, 'hfringe', 'fringe', 'head', 'hair', (8, 1, 1), (-4.0, -8.2, -4.4), g=-0.1, style='hair')
P(E, 'hfringe', 'fringe_a', 'head', 'hair', (2, 2, 1), (-3.5, -8.0, -4.4), g=-0.2, pair=True, style='hair')
P(E, 'hair_braid', 'tbraid', 'head', 'hair', (1, 8, 1), (-0.5, 0.0, -0.5), off=(-4.6, -1.0, -1.2), rot=(0.0, 0.0, 0.0), pair=True, style='braid')
P(E, 'hair_braid', 'tbraid_bead', 'head', 'gem', (1, 1, 1), (-0.5, 0.0, -0.5), off=(-4.6, 6.8, -1.2), g=0.25, pair=True, style='gem')
P(E, 'cascade', 'casc_sa', 'body', 'hairD', (1, 6, 2), (-4.9, 1.0, 2.5), pair=True, style='hair')
P(E, 'cascade', 'casc_sb', 'body', 'hair', (1, 5, 2), (-4.4, 9.0, 2.6), pair=True, style='hair')
P(E, 'circlet', 'wing', 'head', 'gold', (1, 1, 4), (-4.9, -7.4, -1.0), g=0.15, pair=True, style='metal')
P(E, 'circlet', 'wing_leaf', 'head', 'gold', (1, 2, 2), (-5.1, -8.6, 0.0), g=0.1, pair=True, style='metal')
P(E, 'hood', 'hood', 'body', 'cape', (8, 4, 3), (-4.0, -3.4, 2.0), g=0.15, style='cloth')
P(E, 'hood', 'hood_fold', 'body', 'capeD', (6, 2, 2), (-3.0, -4.6, 3.0), g=0.1, style='cloth')
P(E, 'stripe', 'tunic_stripe', 'body', 'trim', (2, 10, 1), (-1.0, 0.4, -2.6), g=0.05, style='cloth')
P(E, 'stripe', 'collar_v', 'body', 'trim', (4, 1, 1), (-2.0, -0.4, -2.6), g=0.15, style='cloth')
P(E, 'stripe', 'collar_stand', 'body', 'trim', (1, 2, 3), (-4.5, -1.4, -1.5), g=0.1, pair=True, style='cloth')
P(E, 'skirt', 'skirt_side', 'body', 'cloth', (1, 6, 3), (-4.5, 11.0, -1.5), pair=True, style='cloth')
P(E, 'skirt', 'skirt_hem', 'body', 'trim', (6, 1, 1), (-3.0, 16.6, -2.6), g=0.15, style='cloth')
P(E, 'skirt', 'skirt_hem_b', 'body', 'trim', (6, 1, 1), (-3.0, 16.6, 1.6), g=0.15, style='cloth')
P(E, 'skirt', 'skirt_train', 'body', 'clothD', (4, 3, 1), (-2.0, 17.0, 1.9), style='cloth')
P(E, 'dagger', 'sheath', 'body', 'leather', (6, 1, 1), (-3.0, 10.8, 2.6), g=0.1, style='leather')
P(E, 'dagger', 'hilt', 'body', 'gold', (1, 2, 1), (3.0, 10.2, 2.6), g=0.1, style='metal')
P(E, 'dagger', 'pommel', 'body', 'gem', (1, 1, 1), (3.0, 9.4, 2.6), g=0.1, style='gem')
P(E, 'belt', 'pouch', 'body', 'leather', (2, 3, 2), (-5.2, 9.8, -1.0), g=0.05, style='leather')
P(E, 'belt', 'pouch_flap', 'body', 'trim', (2, 1, 2), (-5.2, 9.6, -1.0), g=0.2, style='cloth')
P(E, 'pendant', 'chain', 'body', 'gold', (1, 3, 1), (-0.5, 0.2, -2.6), g=-0.25, style='metal')
P(E, 'pendant', 'jewel', 'body', 'gem', (1, 1, 1), (-0.5, 3.1, -2.8), g=0.2, style='gem')
P(E, 'plate', 'breast', 'body', 'gold', (7, 6, 1), (-3.5, 1.0, -3.0), g=0.1, style='metal')
P(E, 'plate', 'breast_leaf', 'body', 'gem', (1, 5, 1), (-0.5, 1.5, -3.5), g=0.05, style='gem')
P(E, 'plate', 'gorget', 'body', 'gold', (8, 1, 5), (-4.0, -1.0, -2.5), g=0.35, style='metal')
P(E, 'greaves', 'greave', 'right_leg', 'gold', (3, 4, 1), (-1.5, 5.0, -2.7), g=0.1, pair=True, style='metal')
P(E, 'greaves', 'knee_leaf', 'right_leg', 'gold', (3, 2, 1), (-1.5, 2.6, -2.9), g=0.2, pair=True, style='metal')
P(E, 'strapb', 'calf_strap', 'right_leg', 'trim', (4, 1, 4), (-2.0, 9.0, -2.0), g=0.4, pair=True, style='cloth')
P(E, 'strapx', 'bandolier', 'body', 'leather', (2, 16, 1), (-1.0, -8.0, 0.0), off=(0.0, 6.0, -2.8), rot=(0, 0, -0.52), style='leather')
P(E, 'strapx', 'bandolier_clasp', 'body', 'gold', (2, 2, 1), (-1.0, -2.0, 0.0), off=(0.0, 6.0, -3.0), rot=(0, 0, -0.52), g=0.1, style='metal')

# --- elfes : arc, couronne de feuilles, manteau de feuilles, queue de cheval, bijoux
P(E, 'bow', 'bow_mid', 'body', 'wood', (1, 8, 1), (-0.5, -4.0, -0.5), off=(-3.2, 6.0, 4.9), style='wood')
P(E, 'bow', 'bow_up', 'body', 'wood', (1, 5, 1), (-0.5, -5.0, -0.5), off=(-3.2, 2.0, 4.9), rot=(0.0, 0.0, -0.3), style='wood')
P(E, 'bow', 'bow_low', 'body', 'wood', (1, 5, 1), (-0.5, 0.0, -0.5), off=(-3.2, 10.0, 4.9), rot=(0.0, 0.0, 0.3), style='wood')
P(E, 'bow', 'bow_string', 'body', 'string', (1, 17, 1), (-0.5, -8.5, -0.5), off=(-1.9, 6.0, 4.9), g=-0.38, style='cloth')
P(E, 'bow', 'bow_grip', 'body', 'leather', (1, 3, 1), (-0.5, -1.5, -0.5), off=(-3.2, 6.0, 4.9), g=0.2, style='leather')
P(E, 'wreath', 'wreath', 'head', 'leaf', (8, 1, 8), (-4.0, -8.4, -4.0), g=0.4, style='cloth')
P(E, 'wreath', 'wreath_leaf', 'head', 'leaf2', (2, 1, 1), (-3.0, -9.6, -4.8), pair=True, style='cloth')
P(E, 'wreath', 'wreath_leaf2', 'head', 'leaf2', (1, 2, 2), (-5.0, -9.0, -2.0), pair=True, style='cloth')
P(E, 'wreath', 'wreath_flower', 'head', 'flower', (1, 1, 1), (-0.5, -9.2, -5.0), g=0.2, style='gem')
P(E, 'wreath', 'wreath_berry', 'head', 'fletch2', (1, 1, 1), (-2.8, -8.9, -5.0), g=0.1, pair=True, style='gem')
P(E, 'leaves', 'lf1', 'right_arm', 'leaf', (5, 1, 5), (-3.5, -2.4, -2.5), g=0.3, pair=True, style='cloth')
P(E, 'leaves', 'lf2', 'right_arm', 'leaf2', (4, 1, 4), (-3.0, -1.2, -2.0), g=0.3, pair=True, style='cloth')
P(E, 'leaves', 'lf3', 'right_arm', 'leaf', (3, 1, 3), (-2.5, 0.0, -1.5), g=0.3, pair=True, style='cloth')
P(E, 'pony', 'pony_tie', 'head', 'gold', (2, 1, 2), (-1.0, -5.4, 5.7), g=0.35, style='metal')
P(E, 'pony', 'pony0', 'head', 'hair', (3, 5, 2), (-1.5, -8.4, 5.5), style='hair')
P(E, 'pony', 'pony1', 'head', 'hairD', (3, 5, 2), (-1.5, -3.4, 5.9), style='hair')
P(E, 'pony', 'pony2', 'head', 'hair', (2, 5, 2), (-1.0, 1.6, 6.1), style='hair')
P(E, 'pony', 'pony3', 'head', 'hairD', (2, 4, 2), (-1.0, 6.6, 6.0), g=-0.15, style='hair')
P(E, 'earcuff', 'earcuff', 'ear_tip', 'gold', (1, 1, 1), (-0.5, -2.6, -0.5), g=0.2, pair=True, style='metal')
P(E, 'sashl', 'sash_long', 'body', 'trim', (2, 9, 1), (1.6, 10.4, -3.2), style='cloth')
P(E, 'sashl', 'sash_long_end', 'body', 'gem', (2, 1, 1), (1.6, 18.8, -3.2), g=0.1, style='gem')

# --- elfes : styles de coiffure, vetements, coiffes, objets et armure en plus
P(E, 'hback_s', 'hair_back_s', 'head', 'hair', (8, 5, 2), (-4.0, -8.0, 3.6), style='hair')
P(E, 'hbob', 'bob_back', 'head', 'hair', (8, 7, 3), (-4.0, -8.0, 3.4), style='hair')
P(E, 'hbob', 'bob_side', 'head', 'hair', (2, 7, 5), (-5.2, -7.0, -2.4), pair=True, style='hair')
P(E, 'hbob', 'bob_fringe', 'head', 'hair', (8, 2, 1), (-4.0, -8.4, -4.5), g=-0.1, style='hair')
P(E, 'hbun', 'bun', 'head', 'hair', (4, 3, 4), (-2.0, -12.1, -0.5), g=0.1, style='hair')
P(E, 'hbun', 'bun_ring', 'head', 'gold', (4, 1, 4), (-2.0, -10.2, -0.5), g=0.35, style='metal')
P(E, 'hbun', 'bun_pin', 'head', 'gold', (1, 1, 5), (-0.5, -11.6, -2.5), g=0.05, style='metal')
P(E, 'hbraid', 'br0', 'body', 'hair', (2, 4, 2), (-1.0, -0.2, 2.6), style='braid')
P(E, 'hbraid', 'br1', 'body', 'hairD', (2, 4, 2), (-1.0, 3.8, 2.7), style='braid')
P(E, 'hbraid', 'br2', 'body', 'hair', (2, 4, 2), (-1.0, 7.8, 2.6), style='braid')
P(E, 'hbraid', 'br3', 'body', 'hairD', (2, 4, 2), (-1.0, 11.8, 2.7), style='braid')
P(E, 'hbraid', 'br4', 'body', 'hair', (2, 4, 2), (-1.0, 15.8, 2.6), style='braid')
P(E, 'hbraid', 'br_tie', 'body', 'gold', (2, 1, 2), (-1.0, 7.4, 2.6), g=0.3, style='metal')
P(E, 'hbraid', 'br_tie2', 'body', 'gem', (2, 1, 2), (-1.0, 19.6, 2.6), g=0.25, style='gem')
P(E, 'htwin', 'tw0', 'body', 'hair', (2, 5, 2), (-4.7, 0.2, -3.1), pair=True, style='braid')
P(E, 'htwin', 'tw1', 'body', 'hairD', (2, 5, 2), (-4.7, 5.2, -3.1), pair=True, style='braid')
P(E, 'htwin', 'tw2', 'body', 'hair', (2, 4, 2), (-4.7, 10.2, -3.1), pair=True, style='braid')
P(E, 'htwin', 'tw_tie', 'body', 'gem', (2, 1, 2), (-4.7, 9.6, -3.1), g=0.25, pair=True, style='gem')
P(E, 'hcrown', 'cr_f', 'head', 'hair', (9, 1, 1), (-4.5, -8.6, -4.7), style='braid')
P(E, 'hcrown', 'cr_s', 'head', 'hairD', (1, 1, 9), (-4.7, -8.6, -4.5), pair=True, style='braid')
P(E, 'hcrown', 'cr_b', 'head', 'hair', (9, 1, 1), (-4.5, -8.6, 3.7), style='braid')
P(E, 'hcrown', 'cr_pin', 'head', 'gold', (1, 1, 1), (-0.5, -8.8, -4.9), g=0.2, style='metal')
P(E, 'hwild', 'spk_a', 'head', 'hair', (1, 4, 1), (-0.5, -4.0, -0.5), off=(-2.8, -9.0, -3.0), rot=(-0.55, 0.0, 0.4), pair=True, style='hair')
P(E, 'hwild', 'spk_b', 'head', 'hairD', (1, 4, 1), (-0.5, -4.0, -0.5), off=(-3.2, -9.0, -0.5), rot=(0.0, 0.0, 0.6), pair=True, style='hair')
P(E, 'hwild', 'spk_c', 'head', 'hair', (1, 4, 1), (-0.5, -4.0, -0.5), off=(-2.6, -9.0, 2.5), rot=(0.55, 0.0, 0.4), pair=True, style='hair')
P(E, 'hwild', 'spk_d', 'head', 'hairD', (1, 5, 1), (-0.5, -5.0, -0.5), off=(0.0, -9.0, 0.5), rot=(-0.25, 0.0, 0.0), style='hair')
P(E, 'robe', 'rb_f', 'body', 'cloth', (8, 11, 1), (-4.0, 11.0, -2.8), g=0.1, style='cloth')
P(E, 'robe', 'rb_b', 'body', 'cloth', (8, 11, 1), (-4.0, 11.0, 1.8), g=0.1, style='cloth')
P(E, 'robe', 'rb_s', 'body', 'clothD', (1, 10, 3), (-4.9, 11.4, -1.5), pair=True, style='cloth')
P(E, 'robe', 'rb_hem_f', 'body', 'trim', (8, 1, 1), (-4.0, 21.4, -2.8), g=0.15, style='cloth')
P(E, 'robe', 'rb_hem_b', 'body', 'trim', (8, 1, 1), (-4.0, 21.4, 1.8), g=0.15, style='cloth')
P(E, 'surcoat', 'sc_f', 'body', 'cloth', (6, 11, 1), (-3.0, 11.0, -2.9), g=0.05, style='cloth')
P(E, 'surcoat', 'sc_b', 'body', 'cloth', (6, 11, 1), (-3.0, 11.0, 1.9), g=0.05, style='cloth')
P(E, 'surcoat', 'sc_stripe', 'body', 'trim', (2, 11, 1), (-1.0, 11.0, -3.2), g=-0.1, style='cloth')
P(E, 'surcoat', 'sc_hem', 'body', 'trim', (6, 1, 1), (-3.0, 21.4, -2.9), g=0.1, style='cloth')
P(E, 'jerkin', 'jk_f', 'body', 'leather', (8, 9, 1), (-4.0, 0.8, -2.8), g=0.05, style='leather')
P(E, 'jerkin', 'jk_lace', 'body', 'trim', (1, 7, 1), (-0.5, 1.2, -3.3), style='cloth')
P(E, 'jerkin', 'jk_b', 'body', 'leather', (8, 9, 1), (-4.0, 0.8, 1.8), g=0.05, style='leather')
P(E, 'jerkin', 'jk_strap', 'body', 'leather', (2, 1, 5), (-3.8, -0.8, -2.5), g=0.15, pair=True, style='leather')
P(E, 'bell', 'bell', 'right_arm', 'cloth', (4, 3, 4), (-3.0, 7.0, -2.0), g=0.55, pair=True, style='cloth')
P(E, 'bell', 'bell_trim', 'right_arm', 'trim', (4, 1, 4), (-3.0, 10.0, -2.0), g=0.62, pair=True, style='cloth')
P(E, 'cape_s', 'cp_s', 'body', 'cape', (8, 9, 1), (-4.0, 0.6, 2.2), g=0.2, style='cloth')
P(E, 'cape_s', 'cp_s_hem', 'body', 'capeD', (8, 1, 1), (-4.0, 9.6, 2.2), g=0.2, style='cloth')
P(E, 'headband', 'hb_f', 'head', 'trim', (8, 1, 1), (-4.0, -7.2, -4.6), g=0.2, style='cloth')
P(E, 'headband', 'hb_s', 'head', 'trim', (1, 1, 8), (-4.4, -7.2, -4.0), g=0.2, pair=True, style='cloth')
P(E, 'headband', 'hb_gem', 'head', 'gem', (1, 1, 1), (-0.5, -7.2, -4.9), g=0.2, style='gem')
P(E, 'headband', 'hb_feather', 'head', 'fletch', (1, 5, 1), (-0.5, -5.0, -0.5), off=(-4.8, -7.2, 1.0), rot=(0.3, 0.0, 0.45), style='cloth')
P(E, 'tiara', 'ti_f', 'head', 'gold', (8, 1, 1), (-4.0, -7.4, -4.6), g=0.2, style='metal')
P(E, 'tiara', 'ti_c', 'head', 'gold', (2, 5, 1), (-1.0, -12.4, -4.6), g=-0.1, style='metal')
P(E, 'tiara', 'ti_s', 'head', 'gold', (1, 3, 1), (-3.0, -10.4, -4.6), pair=True, style='metal')
P(E, 'tiara', 'ti_gem', 'head', 'gem', (1, 1, 1), (-0.5, -9.4, -4.9), g=0.1, style='gem')
P(E, 'tiara', 'ti_gem2', 'head', 'gem', (1, 2, 1), (-0.5, -11.8, -4.9), g=-0.1, style='gem')
P(E, 'ehelm', 'eh_dome', 'head', 'metal', (8, 2, 8), (-4.0, -9.2, -4.0), g=0.6, style='metal')
P(E, 'ehelm', 'eh_brow', 'head', 'metal', (8, 1, 1), (-4.0, -7.8, -4.8), g=0.3, style='metal')
P(E, 'ehelm', 'eh_nasal', 'head', 'metal', (1, 4, 1), (-0.5, -8.0, -5.0), style='metal')
P(E, 'ehelm', 'eh_crest', 'head', 'leaf', (1, 3, 8), (-0.5, -12.0, -4.0), style='cloth')
P(E, 'ehelm', 'eh_crest2', 'head', 'leaf2', (1, 2, 4), (-0.5, -13.6, -2.0), style='cloth')
P(E, 'ehelm', 'eh_wing', 'head', 'gold', (1, 3, 4), (-0.5, -3.0, -2.0), off=(-5.2, -8.4, 0.5), rot=(0.0, 0.0, 0.55), pair=True, style='metal')
P(E, 'ehelm', 'eh_cheek', 'head', 'metal', (1, 4, 3), (-5.0, -6.0, -2.6), pair=True, style='metal')
P(E, 'efauld', 'ef_f', 'body', 'metal', (3, 4, 1), (-3.8, 10.2, -3.0), g=0.1, pair=True, style='metal')
P(E, 'efauld', 'ef_b', 'body', 'metal', (3, 4, 1), (-3.8, 10.2, 1.9), g=0.1, pair=True, style='metal')
P(E, 'lute', 'lu_bowl', 'body', 'wood', (5, 6, 2), (-2.5, 5.0, 4.9), g=0.1, style='wood')
P(E, 'lute', 'lu_neck', 'body', 'wood', (1, 9, 1), (-0.5, -4.0, 5.3), style='wood')
P(E, 'lute', 'lu_head', 'body', 'gold', (2, 2, 1), (-1.0, -6.0, 5.3), style='metal')
P(E, 'lute', 'lu_hole', 'body', 'dark', (2, 2, 1), (-1.0, 6.4, 6.9), g=-0.2, style='leather')
P(E, 'satchel', 'st_b', 'body', 'leather', (4, 5, 4), (4.4, 10.4, -2.0), g=0.05, style='leather')
P(E, 'satchel', 'st_flap', 'body', 'trim', (4, 1, 4), (4.4, 10.0, -2.0), g=0.12, style='cloth')
P(E, 'satchel', 'st_buckle', 'body', 'gold', (1, 1, 1), (5.9, 10.8, -2.5), g=0.12, style='metal')
P(E, 'scroll', 'sr_tube', 'body', 'leather', (2, 9, 2), (1.5, 0.5, 2.6), g=0.05, style='leather')
P(E, 'scroll', 'sr_cap', 'body', 'gold', (2, 1, 2), (1.5, -0.4, 2.6), g=0.1, style='metal')
P(E, 'scroll', 'sr_cap2', 'body', 'gold', (2, 1, 2), (1.5, 9.5, 2.6), g=0.1, style='metal')
P(E, 'lantern', 'ln_top', 'body', 'gold', (2, 1, 2), (-1.0, 9.6, 2.8), g=0.15, style='metal')
P(E, 'lantern', 'ln_glass', 'body', 'lampglow', (2, 3, 2), (-1.0, 10.6, 2.8), g=0.05, style='gem')
P(E, 'lantern', 'ln_bot', 'body', 'gold', (2, 1, 2), (-1.0, 13.6, 2.8), g=0.15, style='metal')
P(E, 'sandal', 'sd1', 'right_leg', 'leather', (4, 1, 4), (-2.0, 8.6, -2.0), g=0.25, pair=True, style='leather')
P(E, 'sandal', 'sd2', 'right_leg', 'leather', (4, 1, 4), (-2.0, 10.4, -2.0), g=0.25, pair=True, style='leather')
P(E, 'sandal', 'sd_sole', 'right_leg', 'leather', (4, 1, 5), (-2.0, 11.3, -2.8), g=0.1, pair=True, style='leather')
P(E, 'bshort', 'bsh', 'right_leg', 'leather', (4, 3, 4), (-2.0, 9.0, -2.0), g=0.3, pair=True, style='leather')
P(E, 'bshort', 'bsh_cuff', 'right_leg', 'trim', (4, 1, 4), (-2.0, 8.6, -2.0), g=0.42, pair=True, style='cloth')
P(E, 'bshort', 'bsh_toe', 'right_leg', 'leather', (3, 2, 2), (-1.5, 10.0, -3.4), g=-0.1, pair=True, style='leather')


# =====================================================================================================  groupes actifs et couleurs par variante
DW_BASE = {'face', 'brows', 'belt', 'cuff', 'bracer', 'strapb'}
EL_BASE = {'ears', 'face', 'locks', 'belt', 'bracer', 'boots'}
ALIAS = {'dwarf': {'hair': ['dh_big'], 'beard': ['beard', 'mous']}, 'elf': {'hair': ['hcap', 'hback', 'hside', 'hfringe'], 'cape': ['mantle', 'cape_l']}}
HIDE = {'dwarf': {'dh_big', 'dh_short', 'dh_mohawk', 'dh_long', 'topknot'}, 'elf': {'hcap', 'hback', 'hside', 'hfringe', 'hback_s', 'hbob', 'hbun', 'hcrown', 'hwild'}}


def groups(kind, name, p):
    g = _groups(kind, name, p)
    for k, v in ALIAS[kind].items():
        if k in g: g |= set(v)
    return g


def _groups(kind, name, p):
    if p.get('xg') is not None:
        xg = set(p['xg'].split()) if isinstance(p['xg'], str) else set(p['xg'])
        if kind == 'dwarf':
            xg |= DW_BASE
            if 'apron' not in xg: xg.add('tabard')
            if 'beard_l' in xg or 'beard_tip' in xg: xg.add('beard')
        else:
            xg |= EL_BASE
        return xg
    hs, bd, bl = p.get('hair_style', 'short'), p.get('beard'), p.get('beard_len') or 0
    g = set()
    if kind == 'dwarf':
        g |= {'face', 'brows', 'belt', 'pack', 'cuff', 'bracer', 'tabard'}
        if hs in ('short', 'mid', 'long'): g.add('hair')
        if hs == 'helmet': g.add('helm')
        if bd:
            g |= {'beard', 'beard_l'}
            if bl >= 8: g |= {'beard_tip', 'braid'}
        if p.get('pauldrons'): g.add('pauldron')
        if p.get('armor') == 'plate': g.add('knee')
        if bd or hs != 'bald': g.add('sideburn')
        if hs in ('mid', 'long'): g.add('hair_braid')
        if not p.get('pauldrons'): g.add('collar')
        if p.get('armor') in ('chain', 'plate'): g.add('harness')
        if p.get('armor') == 'plate' or name == 'dwarf_2': g.add('gloves')
        if p.get('armor') != 'plate': g.add('pot')
        g |= {'mug', 'strapb'}
    else:
        g |= {'ears', 'face', 'hair', 'belt', 'bracer', 'boots', 'locks'}
        if p.get('skirt'): g.add('skirt')
        if name in ('elf_2', 'elf_guard'): g.add('cape')
        else: g.add('cascade')
        if p.get('crown') or name in ('elf_1', 'elf_3'): g.add('circlet')
        g |= {'dagger', 'stripe', 'pendant', 'face'}
        if name in ('elf_2', 'elf_guard'): g.add('hood')
        if name != 'elf_guard': g.add('hair_braid')
        if p.get('crown') or name in ('elf_1', 'elf_3'): g.add('circlet')
        if name == 'elf_guard': g |= {'quiver', 'plate', 'greaves', 'strapx'}
        else: g.add('strapb')
        if p.get('pauldrons'): g.add('pauldron')
    return g


def palette(kind, p):
    sk = rgb(p['skin']); hr = rgb(p['hair']); bd = rgb(p['beard']) if p.get('beard') else hr
    tp = rgb(p['top']); tr = rgb(p['trim']) if p.get('trim') else shade(tp, 0.7)
    belt = rgb(p.get('belt') or '#3a2a1a')
    metal = rgb(p['pauldrons']) if p.get('pauldrons') else rgb('#8a8f98')
    if kind == 'dwarf' and p.get('hair_style') == 'helmet': metal = rgb(p['hair'])
    gem = (70, 200, 170) if kind == 'elf' else (200, 60, 50)
    return dict(skin=sk, skinD=shade(sk, 0.82), hair=hr, hairD=shade(hr, 0.9), brow=shade(bd if p.get('hair_style') in ('bald', 'helmet') else hr, 0.9),
                beard=bd, beardD=shade(bd, 0.84), metal=metal, gold=(205, 166, 60), leather=shade(belt, 1.15), cloth=tp, clothD=shade(tp, 0.8), trim=tr,
                cape=shade(mix(tp, (48, 86, 60), 0.7), 0.9), capeD=shade(tr, 0.9), fur=(150, 134, 110), gem=gem, wood=(112, 78, 46), roll=(128, 100, 66),
                fletch=(236, 232, 220), fletch2=(180, 60, 50), bone=(226, 214, 184), leaf=(62, 104, 58), leaf2=(98, 140, 70), flower=(244, 230, 238), dark=(26, 22, 24), leatherD=shade(belt, 0.9),
                string=(214, 206, 184), lampglow=(255, 226, 130))


# =====================================================================================================  20 nains et 10 elfes
# (nom, peau, cheveux, coiffure, barbe, longueur de barbe, tunique, galon, jambes, bottes, armure, epaulieres, extras)
DWARVES = [
    # nom, peau, cheveux, style(tete de base), barbe, long, haut, galon, jambes, bottes, armure, epaulieres, groupes
    ('mineur', '#c08868', '#5a3a22', 'short', '#5a3a22', 7, '#4a3a2a', '#c9a23a', '#3a3430', '#2a2420', None, None, 'cap pack pot gloves dh_short bs_square mous vest sideburn'),
    ('forgeron', '#c89070', '#3a2a20', 'bald', '#3a2a20', 8, '#4a4038', '#8a8f98', '#2e2a28', '#22201e', None, None, 'apron gloves bs_braid mous earring sideburn'),
    ('guerrier', '#bf8a68', '#7a7f88', 'helmet', '#6a3a1a', 7, '#3a3a44', '#c9a23a', '#2e2e34', '#22201e', 'plate', '#7a7f88', 'helm bs_fork mous dcuir dfauld harness gloves knee pauldron sideburn'),
    ('ancien', '#d4a080', '#e0dcd4', 'long', '#e0dcd4', 9, '#5a4a6a', '#c9a23a', '#3a3640', '#2a2420', None, None, 'dh_long hair_braid beard beard_l beard_tip braid robe collar sideburn'),
    ('brasseur', '#d8a888', '#b0501e', 'mid', '#b0501e', 7, '#7a4a2a', '#c9a23a', '#3a3430', '#2a2420', None, None, 'apron mug dh_big beard beard_l sideburn'),
    ('marchand', '#cc9878', '#5a3a22', 'short', '#5a3a22', 7, '#6a3a3a', '#e0c070', '#3a3430', '#2a2420', None, None, 'hat pack pot mug dh_short bs_goatee mous coat sideburn'),
    ('noble', '#d4a080', '#1c1816', 'short', '#1c1816', 8, '#5a2a22', '#c9a23a', '#2e2a2a', '#22201e', None, None, 'crown dh_short bs_goatee mous coat collar dsash sideburn'),
    ('eclaireur', '#a87250', '#3a2a20', 'mid', '#3a2a20', 7, '#3a4a34', '#2a3424', '#34302c', '#2a2420', None, None, 'scout_hood pack pot gloves bs_goatee vest sideburn'),
    ('garde_cotte', '#c89070', '#8a8f98', 'helmet', '#8a3a1a', 7, '#3a4a5a', '#8a8f98', '#2e2e34', '#22201e', 'chain', '#8a8f98', 'ht bs_square mous dfauld harness pauldron gloves sideburn'),
    ('barbare', '#a87250', '#8a3a1a', 'bald', '#8a3a1a', 9, '#6a5038', '#3a2a1c', '#3a2e24', '#2a2018', None, None, 'dh_mohawk eyepatch fcloak pspike bs_braid mous mug'),
    ('chasseur', '#c08868', '#6a4424', 'mid', '#6a4424', 7, '#4a5a34', '#2a3424', '#3a3430', '#2a2420', None, None, 'topknot collar pack gloves fcloak beard beard_l sideburn'),
    ('cuisinier', '#d8a888', '#b8b0a8', 'short', '#b8b0a8', 7, '#8a8a7a', '#c8c0b0', '#4a4640', '#2a2420', None, None, 'cap apron pot mug dh_short beard sideburn'),
    ('macon', '#c89070', '#8a8f98', 'bald', '#8a8f98', 8, '#6a6a70', '#8a8f98', '#3a3a40', '#2a2a2e', None, None, 'topknot apron gloves pack beard beard_l sideburn'),
    ('jeune', '#d8a888', '#6a4424', 'short', '#6a4424', 5, '#4a6a3a', '#c9a23a', '#3a3430', '#2a2420', None, None, 'dh_short beard pack mug dsash sideburn'),
    ('pretre', '#cc9878', '#e0dcd4', 'bald', '#e0dcd4', 8, '#8a7a60', '#c9a23a', '#5a5044', '#2a2420', None, None, 'collar harness dsash beard beard_l beard_tip braid sideburn'),
    ('veteran', '#b8805e', '#8a8f98', 'mid', '#8a8f98', 8, '#3a4a5a', '#8a8f98', '#2e2e34', '#22201e', 'chain', '#8a8f98', 'eyepatch harness pauldron gloves dh_big beard beard_l beard_tip braid fcloak dfauld mug sideburn'),
    ('runique', '#d4a080', '#b8b0a8', 'long', '#b8b0a8', 8, '#3a4a7a', '#c9a23a', '#2e3248', '#22201e', None, None, 'scout_hood harness collar robe bs_braid mous sideburn'),
    ('garde_roi', '#cc9878', '#c9a25a', 'helmet', '#c9a25a', 9, '#4a3a2a', '#c9a23a', '#2e2e34', '#22201e', 'plate', '#c9a23a', 'ho dcuir dfauld dgreave pauldron knee gloves bs_square mous sideburn'),
    ('aubergiste', '#d8a888', '#5a3a22', 'short', '#5a3a22', 8, '#8a5a3a', '#e0c070', '#3a3430', '#2a2420', None, None, 'apron mug dh_big bs_fork mous dsash sideburn'),
    ('prospecteur', '#c08868', '#c9a25a', 'long', '#c9a25a', 8, '#5a6a4a', '#8a7a4a', '#3a3430', '#2a2420', None, None, 'hat pack pot eyepatch dh_long bs_goatee mous vest mug sideburn'),
]
ELVES = [
    # nom, peau, cheveux, style(tete de base), yeux, haut, galon, jambes, bottes, jupe, groupes
    ('seigneur', '#ecd8c4', '#e8d890', 'long', '#4a8ac0', '#3a6a4a', '#d8c070', '#5a5a4a', '#4a3a28', False, 'hcap hback hside hfringe cascade circlet surcoat pendant dagger cape_s stripe'),
    ('archer', '#e8d0bc', '#5a3a22', 'short', '#6a9a6a', '#3a4a34', '#2a3424', '#3a3a2a', '#3a3028', False, 'hcap hback_s hfringe pony jerkin bow quiver bshort strapb leaves'),
    ('dame', '#f0dcc8', '#c8c8d0', 'long', '#5a7ab0', '#7a6a8a', '#e0d090', '#6a5a7a', '#4a3a28', True, 'hcap hfringe hbraid tiara robe pendant stripe sashl'),
    ('menestrel', '#ecd0b8', '#b8602a', 'mid', '#6a8a5a', '#8a5a3a', '#e0c070', '#5a4a3a', '#4a3a28', False, 'hcap hbob headband bell stripe lute sandal sashl'),
    ('gardien', '#e8d0bc', '#2a2420', 'mid', '#6a9a6a', '#2f4a40', '#c8d0e0', '#2a3a34', '#2a2420', False, 'hood mantle cape_l hfringe pony dagger strapb strapx bshort'),
    ('pretresse', '#f0dcc8', '#e8e8e0', 'long', '#5a7ab0', '#e0e0d0', '#c8b060', '#d0d0c0', '#8a8a7a', True, 'hcap hback_s hcrown tiara robe bell pendant stripe sashl hair_braid'),
    ('messager', '#e8d0bc', '#1c1816', 'short', '#4a8ac0', '#5a6a8a', '#c8d0e0', '#3a4a5a', '#2a2420', False, 'hcap hwild headband jerkin cape_s satchel dagger strapx sandal'),
    ('forestier', '#e0c8b0', '#6a4424', 'short', '#6a8a5a', '#4a5a3a', '#6a5a3a', '#4a4a3a', '#3a3028', False, 'hcap hback_s htwin wreath leaves surcoat bow quiver strapb bshort'),
    ('sage', '#ecd8c4', '#c8d0e0', 'mid', '#5a7ab0', '#5a6a8a', '#c8d0e0', '#4a5a7a', '#3a3a4a', True, 'hcap hback_s hbun robe bell pendant scroll lantern stripe'),
    ('jeune', '#f0dcc8', '#d8b860', 'short', '#4a8ac0', '#8a9a6a', '#e0e0b0', '#6a6a4a', '#4a3a28', False, 'hcap hback_s hfringe headband jerkin satchel dagger strapb sandal'),
]

DW_ARMOR_KEEP = {'beard', 'beard_l', 'beard_tip', 'braid', 'bs_fork', 'bs_square', 'bs_braid', 'bs_goatee', 'mous', 'eyepatch', 'earring', 'sideburn', 'pack', 'mug'}
EL_ARMOR_KEEP = {'cascade', 'pony', 'hbraid', 'htwin', 'hair_braid', 'bow', 'quiver', 'dagger', 'strapb', 'strapx', 'mantle', 'cape_l', 'leaves', 'lute', 'satchel', 'scroll', 'lantern', 'sashl', 'pendant'}
DW_HELMS = ['helm', 'ho', 'ht']


def armored_rows():
    """une version en armure naine par nain, une en armure elfique par elfe (corps armure sombre + or / tissu vert + plaques dorees)"""
    dw, el = [], []
    for i, (lbl, sk, hr, hs, bd, bl, top, trim, legs, boots, armor, paul, xg) in enumerate(DWARVES):
        keep = [g for g in xg.split() if g in DW_ARMOR_KEEP]
        g = keep + ['dmod', 'dcuir', 'dfauld', 'dgreave', 'gloves', 'knee', 'harness', 'pspike' if i % 2 else 'pauldron']
        if i % 3 == 0: g.append('fcloak')
        dw.append((lbl + '_arm', sk, hr, 'short', bd, bl, '#2a2a34', '#d6ac50', '#26262e', '#1c1c22', 'plate', '#5a5c68', ' '.join(dict.fromkeys(g))))
    for i, (lbl, sk, hr, hs, eyes, top, trim, legs, boots, skirt, xg) in enumerate(ELVES):
        keep = [g for g in xg.split() if g in EL_ARMOR_KEEP]
        g = keep + (['ehelm'] if i % 2 == 0 else ['circlet', 'hcap', 'hfringe', 'hback_s'] if i % 4 == 1 else ['headband', 'hcap', 'hfringe', 'hback']) + ['plate', 'efauld', 'greaves', 'pauldron']
        el.append((lbl + '_arm', sk, hr, 'short', eyes, '#22422c', '#e8c86c', '#2a4a34', '#2a2a20', False, ' '.join(dict.fromkeys(g))))
    return dw, el


def all_skins():
    """(nom de fichier, race, arguments de skins.skin) : nains 1..20 civils, 21..40 en armure ; elfes 1..10 civils, 11..20 en armure"""
    out = []
    adw, ael = armored_rows()
    for i, (lbl, sk, hr, hs, bd, bl, top, trim, legs, boots, armor, paul, xg) in enumerate(DWARVES + adw):
        out.append(('dwarf_%d' % (i + 1), 'dwarf', dict(skin=sk, hair=hr, hair_style=hs, beard=bd, beard_len=bl, top=top, trim=trim, legs=legs, boots=boots,
                                                         armor=armor, pauldrons=paul, gloves='#3a2a1c' if 'gloves' in xg.split() else None, sleeves=not lbl.startswith('barbare'), seed=200 + i, xg=xg)))
    for i, (lbl, sk, hr, hs, eyes, top, trim, legs, boots, skirt, xg) in enumerate(ELVES + ael):
        out.append(('elf_%d' % (i + 1), 'elf', dict(skin=sk, hair=hr, hair_style=hs, eyes=eyes, top=top, trim=trim, legs=legs, boots=boots, skirt=skirt, seed=300 + i, xg=xg,
                                                   pauldrons='#a6b8a2' if lbl.endswith('_arm') else None)))
    return out


# =====================================================================================================  UV : rangement automatique dans la moitie libre de la texture 128x128
def layout():
    """(u, v) de chaque piece ; deterministe. Zone libre : x>=64 (toute la hauteur) puis le quart bas-gauche."""
    res = {}
    for kind in PARTS:
        boxes = sorted(((s['dims'], s['name']) for s in PARTS[kind]), key=lambda t: (-(t[0][1] + t[0][2]), t[1]))
        regions = [[64, 0, 128, 256], [0, 64, 64, 256]]
        ri, x, y, rowh = 0, regions[0][0], regions[0][1], 0
        for (w, h, d), name in boxes:
            bw, bh = 2 * d + 2 * w, d + h
            if x + bw > regions[ri][2]: x = regions[ri][0]; y += rowh; rowh = 0
            if y + bh > regions[ri][3]:
                ri += 1; assert ri < len(regions), 'texture pleine'
                x, y, rowh = regions[ri][0], regions[ri][1], 0
            res[(kind, name)] = (x, y)
            x += bw; rowh = max(rowh, bh)
    return res

LAYOUT = layout()


def paint(s, kind, name, p):
    """peint les pieces supplementaires dans l'image du skin ; les groupes inactifs restent transparents (donc invisibles)"""
    pal = palette(kind, p); act = groups(kind, name, p)
    for sp in PARTS[kind]:
        if sp['grp'] not in act: continue
        u, v = LAYOUT[(kind, sp['name'])]
        w, h, d = sp['dims']
        c = pal[sp['mat']]
        s.box(u, v, w, h, d, c, noise=0.05)
        decorate(s, u, v, w, h, d, sp['style'], c, sp['name'])


def decorate(s, u, v, w, h, d, style, c, name):
    im = s.im
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * d + 2 * w):
            px = im.getpixel((xx, yy))
            if px[3] == 0: continue
            f = 1.0
            rx, ry = xx - u, yy - v
            if style == 'hair': f = 1.1 + 0.08 * math.sin(xx * 2.1 + (yy // 3) * 1.3) - (0.07 if (yy + xx // 4) % 6 == 0 else 0)
            elif style == 'braid': f = 1.14 if (xx + yy) % 4 < 2 else 0.80
            elif style == 'metal': f = 1.22 if (ry == d or rx == d) else (0.74 if (ry >= d + h - 1 or rx >= 2 * d + 2 * w - 1) else 1.0)
            elif style == 'leather': f = 0.78 if (ry + rx) % 7 == 0 else (1.12 if (rx % 4 == 0 and ry % 4 == 0) else 1.0)
            elif style == 'cloth': f = 0.88 if (xx + yy) % 2 == 0 else 1.06
            elif style == 'wood': f = 0.85 if xx % 3 == 0 else 1.05
            elif style == 'gem': f = 1.4 if (rx + ry) % 3 == 0 else 0.9
            elif style == 'skin': f = 1.0
            im.putpixel((xx, yy), shade(px[:3], f) + (px[3],))
    if name in ('braid_ring',):
        pass


# =====================================================================================================  Java
def fl(x):
    return ('%.2f' % x).rstrip('0').rstrip('.') + 'F' if x != int(x) else '%d.0F' % int(x)


def gen_java():
    L = ['package net.ringsofash.client;', '',
         'import net.minecraft.client.model.PlayerModel;', 'import net.minecraft.client.model.geom.PartPose;',
         'import net.minecraft.client.model.geom.builders.CubeDeformation;', 'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
         'import net.minecraft.client.model.geom.builders.PartDefinition;', '',
         '/** GENERE par tools/people3d.py : pieces 3D supplementaires des nains et des elfes (squelette de joueur + barbes, cheveux, ceintures, capes...). */',
         'final class PeopleParts {', '    private PeopleParts() {}', '']
    hair = {k: [] for k in PARTS}
    for kind in PARTS:
        L.append('    static void %s(PartDefinition root) {' % kind)
        for b in ('head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'):
            L.append('        PartDefinition %s = root.getChild("%s");' % (b, b))
        for sp in expand(kind):
            full = 'x_%s_%s' % (sp['grp'], sp['full'])
            u, v = LAYOUT[(kind, sp['name'].rsplit('_', 1)[0] if sp['mirror'] or (sp['name'].endswith('_r') and sp['name'][:-2] in PAIRED[kind]) else sp['name'])]
            w, h, d = sp['dims']; x, y, z = sp['at']; ox, oy, oz = sp['off']; rx, ry, rz = sp['rot']
            par = sp['parent']
            pv = par if par in ('head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg') else 'v_' + par
            L.append('        PartDefinition v_%s = %s.addOrReplaceChild("%s", CubeListBuilder.create().texOffs(%d, %d)%s.addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s)), PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));'
                     % (sp['full'], pv, full, u, v, '.mirror()' if sp['mirror'] else '', fl(x), fl(y), fl(z), fl(w), fl(h), fl(d), fl(sp['g']), fl(ox), fl(oy), fl(oz), fl(rx), fl(ry), fl(rz)))
            if sp['grp'] in HIDE[kind]: hair[kind].append(full)
        L.append('    }')
        L.append('')
    L.append('    private static final String[] HAIR = {%s};' % ', '.join('"%s"' % n for k in PARTS for n in hair[k]))
    L.append('')
    L.append('    static void add(String kind, PartDefinition root) {')
    L.append('        if (kind.equals("dwarf")) dwarf(root);')
    L.append('        else if (kind.equals("elf")) elf(root);')
    L.append('    }')
    L.append('')
    L.append('    /** les cheveux disparaissent sous un casque */')
    L.append('    static void hideHair(PlayerModel<?> m, boolean hide) {')
    L.append('        for (String n : HAIR) {')
    L.append('            if (m.head.hasChild(n)) m.head.getChild(n).visible = !hide;')
    L.append('            if (m.body.hasChild(n)) m.body.getChild(n).visible = !hide;')
    L.append('        }')
    L.append('    }')
    L.append('}')
    open(JAVA, 'w').write('\n'.join(L) + '\n')


# =====================================================================================================  apercu
BASE = [  # nom, parent, pivot, (origine, dims, uv, inflate)
    ('head', (0, 0, 0), (-4, -8, -4), (8, 8, 8), (0, 0), 0), ('hat', (0, 0, 0), (-4, -8, -4), (8, 8, 8), (32, 0), 0.5),
    ('body', (0, 0, 0), (-4, 0, -2), (8, 12, 4), (16, 16), 0), ('jacket', (0, 0, 0), (-4, 0, -2), (8, 12, 4), (16, 32), 0.25),
    ('right_arm', (-5, 2, 0), (-3, -2, -2), (4, 12, 4), (40, 16), 0), ('right_sleeve', (-5, 2, 0), (-3, -2, -2), (4, 12, 4), (40, 32), 0.25),
    ('left_arm', (5, 2, 0), (-1, -2, -2), (4, 12, 4), (32, 48), 0), ('left_sleeve', (5, 2, 0), (-1, -2, -2), (4, 12, 4), (48, 48), 0.25),
    ('right_leg', (-1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (0, 16), 0), ('right_pants', (-1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (0, 32), 0.25),
    ('left_leg', (1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (16, 48), 0), ('left_pants', (1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (0, 48), 0.25)]


def geo_of(kind, active=None):
    parts = [dict(name=n, parent=None, pivot=pv, rot=(0, 0, 0), boxes=[dict(o=o, s=s, uv=uv, inflate=g, mirror=False)]) for (n, pv, o, s, uv, g) in BASE]
    for sp in expand(kind):
        if active is not None and sp['grp'] not in active: continue
        u, v = LAYOUT[(kind, sp['name'].rsplit('_', 1)[0] if sp['name'].endswith(('_r', '_l')) and sp['name'][:-2] in PAIRED[kind] else sp['name'])]
        parts.append(dict(name=sp['full'], parent=sp['parent'], pivot=sp['off'], rot=sp['rot'],
                          boxes=[dict(o=sp['at'], s=sp['dims'], uv=(u, v), inflate=sp['g'], mirror=sp['mirror'])]))
    return dict(parts=parts)


def preview(name, out=None, views=((20, 8), (200, 8), (90, 4)), pose=None, scale=6):
    import render3d, models
    kind = 'dwarf' if name.startswith('dwarf') else 'elf'
    tex = Image.open(os.path.join(render3d.A, 'textures/entity/people', name + '.png')).convert('RGBA')
    geo = geo_of(kind)
    ims = [render3d.render(None, y, p, scale, pose, geo=geo, texim=tex) for y, p in views]
    H = max(i.height for i in ims); W = sum(i.width for i in ims)
    sheet = Image.new('RGB', (W, H), (24, 22, 28)); x = 0
    for i in ims: sheet.paste(i, (x, H - i.height)); x += i.width
    os.makedirs('/tmp/people', exist_ok=True)
    out = out or '/tmp/people/%s.png' % name
    sheet.save(out); return out


if __name__ == '__main__':
    import models
    models.skins()
    gen_java()
    for n in (sys.argv[1:] or ['dwarf_1', 'elf_1']):
        print(preview(n))
