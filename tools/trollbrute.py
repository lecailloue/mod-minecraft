from trollcore import *
from trollcore import _N

RED = (240, 60, 40)
DRED = (170, 26, 20)
GLOWC = (255, 70, 50)


def make_builders(s):
    def ax(p, l, ground=False):
        sz = max(1, int(round(l * s)))
        c = (p + l / 2) * s
        q = c - sz / 2
        if ground and p == 0:
            q = 0.0
        return q, sz

    def B(name, x, y, z, w, h, d, mat, **kw):
        x2, w2 = ax(x, w)
        y2, h2 = ax(y, h, True)
        z2, d2 = ax(z, d)
        if kw.get('pivot') is not None:
            kw['pivot'] = tuple(v * s for v in kw['pivot'])
        if 'inflate' in kw:
            kw['inflate'] = kw['inflate'] * s
        return Cube(name, x2, y2, z2, w2, h2, d2, mat, **kw)

    def G(name, pivot, rot=(0, 0, 0), children=None):
        return Group(name, tuple(v * s for v in pivot), rot, children)
    return B, G


# ------------------------------------------------------------------ pixel details
def muscle_lines(arr, pts_dark, pts_light=()):
    for (x, y) in pts_dark:
        put(arr, x, y, (0, 0, 0), .44)
    for (x, y) in pts_light:
        arr[y, x] = np.clip(arr[y, x] + 12, 0, 255) if 0 <= y < arr.shape[0] and 0 <= x < arr.shape[1] else arr[0, 0]


def fx_chest(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    c = w // 2
    for y in range(0, h - 2):                       # sternum
        put(n, c - 1, y, (0, 0, 0), .42); put(n, c, y, (0, 0, 0), .42)
    for x in range(1, c - 1):                       # under-pec shadow
        put(n, x, h - 3, (0, 0, 0), .44); put(n, w - 1 - x, h - 3, (0, 0, 0), .44)
    for x in range(0, w):                           # collar-bone / trap line
        put(n, x, 0, (0, 0, 0), .22)
    for i in range(3):                              # claw scars
        line(n, c + 3 + i * 2, 1 + i, 1, 1, 4, (160, 150, 138), .7)
        line(n, c + 4 + i * 2, 1 + i, 1, 1, 4, (30, 20, 20), .4)
    s = F['south']
    hh, ww, _ = s.shape
    for y in range(hh):                             # spine + lats
        put(s, ww // 2, y, (0, 0, 0), .42); put(s, ww // 2 - 1, y, (0, 0, 0), .42)
    for x in range(1, ww // 2 - 1):
        put(s, x, hh // 2, (0, 0, 0), .22); put(s, ww - 1 - x, hh // 2, (0, 0, 0), .22)


def fx_pec(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    n[h - 1, :] *= .72
    put(n, w // 2, h - 2, (60, 40, 44), .9)         # nipple
    F['up'][:] *= 1.05


def fx_abs(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    c = w // 2
    for y in range(h):
        put(n, c - 1, y, (0, 0, 0), .46); put(n, c, y, (0, 0, 0), .46)
    for y in range(1, h, 2):
        for x in range(1, w - 1):
            put(n, x, y, (0, 0, 0), .38)
    put(n, c + 1, h // 2, (30, 24, 24), .9)         # navel


def fx_hump(F, rng, glow):
    s = F['south']
    hh, ww, _ = s.shape
    line(s, 1, 1, 1, 1, 4, (160, 150, 138), .75)
    for (x, y) in [(3, 2), (ww - 4, 3)]:
        blob(s, x, y, 1, (72, 50, 44), .55)
    for x in range(ww):
        put(s, x, 0, (0, 0, 0), .2)


def fx_cranium(dark):
    def fx(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        n[1:3, :] *= .72
        L = (1, 2); R = (w - 3, w - 2)
        for cols in (L, R):
            for x in cols:
                for y in ((3, 4) if not dark else (4, 5)):
                    put(n, x, y, (24, 18, 16) if not dark else (10, 10, 12))
        if not dark:
            put(n, 2, 3, (238, 204, 52)); put(n, 1, 4, (206, 116, 28))
            put(n, w - 3, 3, (238, 204, 52)); put(n, w - 2, 4, (206, 116, 28))
            line(n, w - 3, 5, 1, 1, 2, (160, 150, 138), .9)
            up = F['up']
            uh, uw, _ = up.shape
            for y in range(uh):                      # ragged mohawk
                for x in range(uw // 2 - 1, uw // 2 + 1):
                    up[y, x] = (26 + rng.integers(0, 10), 24 + rng.integers(0, 8), 22)
                for x in (uw // 2 - 2, uw // 2 + 1):
                    if rng.random() < .55:
                        up[y, x] = (28, 24, 22)
        else:
            for (x, c) in [(2, RED), (1, DRED), (w - 3, RED), (w - 2, DRED)]:
                put(n, x, 4, c)
                glow('north', x, 4, GLOWC if c == RED else (200, 40, 30))
            line(n, 0, 5, 1, 1, 2, (96, 90, 94), .8)
    return fx


def fx_jaw(dark):
    def fx(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        n[0, :] *= .5
        if dark:
            n[0, :] = (20, 8, 10)
            for x in range(1, w, 2):
                put(n, x, 0, (212, 206, 184)); put(n, x, 1, (196, 188, 164), .8)
            for x in range(3, w - 3, 2):
                put(n, x, 2, (180, 172, 150), .7)
        else:
            for x in range(1, w, 2):
                put(n, x, 1, (36, 28, 26), .55)
            line(n, w - 3, 1, 0, 3, 3, (160, 150, 138), .9)
        up = F['up']
        up[:] = np.array((96, 38, 48) if not dark else (54, 20, 24)) + \
            (noise(up.shape[0], up.shape[1], 2, rng)[:, :, None] - .5) * 12
    return fx


def fx_brow(F, rng, glow):
    F['north'][:] *= .82
    F['down'][:] *= .6


def fx_nose(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    put(n, 0, h - 1, (24, 18, 16)); put(n, w - 1, h - 1, (24, 18, 16))
    for x in range(1, w - 1):
        put(n, x, h - 1, (24, 18, 16), .4)


def fx_tooth(F, rng, glow):
    for f in F.values():
        f *= np.linspace(.78, 1.05, f.shape[0])[:, None, None]


def fx_foot(claw):
    def fx(F, rng, glow):
        n = F['north']
        h, w, _ = n.shape
        for x in range(w):
            put(n, x, h - 1, claw)
        for x in range(2, w - 1, 2):
            put(n, x, h - 2, (0, 0, 0), .35)
        for f in ('east', 'west'):
            a = F[f]
            a[-1, :] = a[-1, :] * .4
    return fx


def fx_hand(claw):
    def fx(F, rng, glow):
        for f in ('north', 'south', 'east', 'west'):
            a = F[f]
            for x in range(a.shape[1]):
                put(a, x, a.shape[0] - 1, claw)
            a[0, :] *= .85
        n = F['north']
        for x in range(1, n.shape[1] - 1, 2):
            put(n, x, 1, (0, 0, 0), .42)           # knuckles
    return fx


def fx_helm(F, rng, glow):
    n = F['north']
    h, w, _ = n.shape
    line(n, 0, 0, 1, 1, 3, (92, 94, 106), .6)
    line(n, w - 1, 0, -1, 1, 3, (92, 94, 106), .6)
    n[h - 1, :] *= .6
    F['up'][:, w // 2 - 1:w // 2 + 1] += 14


def fx_crest(F, rng, glow):
    for f in ('east', 'west'):
        F[f][0, :] += 20


def fx_pauldron(F, rng, glow):
    for f in ('north', 'south', 'east', 'west'):
        a = F[f]
        a[-1, :] *= .6
        a[0, :] += 14


def fx_vamb(F, rng, glow):
    for f in ('north', 'south', 'east', 'west'):
        a = F[f]
        a[-1, :] *= .55
        put(a, 1, 1, (126, 128, 140)); put(a, a.shape[1] - 2, 1, (126, 128, 140))


# ------------------------------------------------------------------ builder
def build(dark):
    """dark=False : troll classique ; dark=True : chef de meute (plus grand, casque, sans epaulieres ni cornes)"""
    s = 1.12 if dark else 1.0
    B, G = make_builders(s)
    skin = 'bskin' if dark else 'skin'
    claw = (128, 122, 108) if dark else (52, 46, 40)
    spike_mat = 'steel' if dark else 'bone'
    hips_mat = 'bleather' if dark else 'cloth'

    def leg(side):
        sg = -1 if side == 'left' else 1
        x0 = -9 if sg < 0 else 2
        return G('leg_' + side, (sg * 5.5, 15, 0), children=[
            B('thigh_' + side, x0, 8, -4, 7, 7, 8, skin, key='b_thigh'),
            B('calf_' + side, x0 + .5, 3, -3.5, 6, 5, 7, skin, key='b_calf'),
            B('foot_' + side, x0 + .5, 0, -6, 6, 3, 9, skin, key='b_foot', fx=fx_foot(claw)),
        ])

    pel_children = [B('loin_band', -8, 12, -4.5, 16, 3, 9, hips_mat, key='b_hips')]
    if dark:
        pel_children += [
            B('tasset_center', -2, 5, -5.5, 4, 10, 1, 'dmetal', key='b_tc'),
            B('tasset_right', -8, 8, -5.5, 5, 7, 1, 'dmetal', key='b_ts'),
            B('tasset_left', 3, 8, -5.5, 5, 7, 1, 'dmetal', key='b_ts'),
        ]
    else:
        pel_children += [
            B('loin_front', -3, 6, -5.5, 6, 9, 1, 'cloth', key='b_lf'),
            B('loin_back', -3, 7, 4.5, 6, 8, 1, 'cloth', key='b_lb'),
        ]
    pelvis = G('pelvis', (0, 15, 0), children=pel_children)

    def arm(side):
        sg = -1 if side == 'left' else 1
        ax_ = -18 if sg < 0 else 10
        g = G('arm_' + side, (sg * 13, 29, 0), rot=(18 if not dark else 12, 0, 0))
        kids = [
            B('upper_arm_' + side, ax_, 19, -4, 8, 10, 8, skin, key='b_arm_u'),
            B('forearm_' + side, ax_, 9, -4.5, 8, 10, 9, skin, key='b_arm_f'),
            B('hand_' + side, ax_ + .5, 4, -3.5, 7, 5, 7, skin, key='b_hand', fx=fx_hand(claw)),
        ]
        if not dark:
            kids += [
                B('shoulder_spike_base_' + side, ax_ + 3, 29, -1, 2, 2, 2, 'bone', key='b_ssb', fx=fx_tooth),
                B('shoulder_spike_tip_' + side, ax_ + 3.5, 31, -.5, 1, 3, 1, 'bone', key='b_sst', fx=fx_tooth),
            ]
        else:
            out = -20.8 if sg < 0 else 18.8
            kids += [
                B('vambrace_' + side, ax_, 9, -4.5, 8, 6, 9, 'dmetal', key='b_vamb', inflate=.8, fx=fx_vamb),
                B('vamb_spike_a_' + side, out, 11, -.5, 2, 1, 1, 'steel', key='b_vsp'),
            ]
            if sg < 0:
                kids.append(B('vamb_spike_b_' + side, out, 13.5, -.5, 2, 1, 1, 'steel', key='b_vsp'))
        g.children = kids
        return g

    head_kids = [
        B('cranium', -4.5, 30, -4.5, 9, 8, 9, skin, key='b_head', fx=fx_cranium(dark)),
        B('jaw', -5, 26, -7, 10, 5, 10, skin, key='b_jaw', fx=fx_jaw(dark)),
        B('brow', -5, 35, -6.5, 10, 2, 2, 'dmetal' if dark else skin, key='b_brow', fx=None if dark else fx_brow),
        B('nose', -2, 32, -6.5, 4, 2, 2, skin, key='b_nose', fx=fx_nose),
        B('tusk_a', -5, 31, -7.4, 1, 4, 1, 'bone', key='b_tusk_a', fx=fx_tooth),
        B('tusk_b', 4, 31, -7.4, 1, 3, 1, 'bone', key='b_tusk_b', fx=fx_tooth),
    ]
    if not dark:
        head_kids += [
            B('tooth_1', -2.8, 31, -6.9, 1, 1, 1, 'bone', key='b_t1', fx=fx_tooth),
            B('tooth_2', -0.6, 31, -6.9, 1, 2, 1, 'bone', key='b_t2', fx=fx_tooth),
            B('tooth_3', 1.6, 31, -6.9, 1, 1, 1, 'bone', key='b_t1', fx=fx_tooth),
        ]
    else:
        head_kids += [
            B('helmet', -4.5, 35, -4.5, 9, 3, 9, 'dmetal', key='b_helm', inflate=.8, fx=fx_helm),
            B('nose_guard', -.5, 32, -7.1, 1, 6, 1, 'dmetal', key='b_ng'),
            B('cheek_guard_r', -5.5, 31, -3, 1, 5, 7, 'dmetal', key='b_cheek'),
            B('cheek_guard_l', 4.5, 31, -3, 1, 5, 7, 'dmetal', key='b_cheek'),
            B('neck_guard', -5, 31, 5.3, 10, 8, 1, 'dmetal', key='b_neckg'),
        ]
    head = G('head', (0, 31, -2), rot=(16, 0, 0), children=head_kids)

    body_kids = [
        B('abdomen', -7, 15, -5, 14, 6, 10, skin, key='b_abdomen', fx=fx_abs),
        B('chest', -10, 21, -5.5, 20, 10, 11, skin, key='b_chest', fx=fx_chest),
        B('pec_right', -9.5, 23, -7, 9, 5, 2, skin, key='b_pec', fx=fx_pec),
        B('pec_left', .5, 23, -7, 9, 5, 2, skin, key='b_pec', fx=fx_pec),
        B('hump', -7, 29, 2, 14, 6, 7, skin, key='b_hump', fx=fx_hump),
        B('spine_spike_top', -.5, 35, 4, 1, 3, 1, spike_mat, key='b_sp1', fx=fx_tooth if not dark else None),
        B('spine_spike_back', -.5, 31, 9, 1, 2, 2, spike_mat, key='b_sp2', fx=fx_tooth if not dark else None),
        B('spine_spike_low', -.5, 25, 5.5, 1, 2, 2, spike_mat, key='b_sp2', fx=fx_tooth if not dark else None),
    ]
    if dark:
        body_kids += [
            B('belt', -8, 14.5, -5.5, 16, 3, 11, 'bleather', key='b_belt'),
            B('buckle', -2, 15, -6.5, 4, 2, 1, 'steel', key='b_buckle'),
            B('chest_strap', -1.5, 16, -7.9, 3, 18, 1, 'bleather', key='b_strap', rot=(0, 0, 35)),
        ]
    body_kids += [head, arm('left'), arm('right')]
    body = G('body', (0, 15, 0), rot=(-14 if dark else -22, 0, 0), children=body_kids)
    return [leg('left'), leg('right'), pelvis, body]


def generate(outdir):
    """ecrit tools/assets/troll.bbmodel et troll_chief.bbmodel (+ troll_chief_glow.png) et renvoie les modeles"""
    res = {}
    for name, dark in (('troll', False), ('troll_chief', True)):
        _N[0] = 0
        groups = build(dark)
        normalize(groups)
        cubes = [c for c, _ in walk(groups)]
        W, H = pack(cubes)
        tex, glow = paint_all(cubes, W, H)
        export_bbmodel(name, groups, cubes, W, H, tex, os.path.join(outdir, name + '.bbmodel'))
        if glow[..., 3].any():
            Image.fromarray(glow, 'RGBA').save(os.path.join(outdir, name + '_glow.png'))
        res[name] = (groups, tex, glow)
    return res
