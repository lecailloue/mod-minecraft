"""Croquis -> carte 2000x2000 (formes, biomes, relief) lue par le mod.
Usage: mapgen.py croquis.png places.json sortie_apercu/ [carte.bin] [places_out.json]
Le croquis contient : mer (damier cyan), plaines vert clair / jaune pale / cendre grise, forets vert fonce,
montagnes grises, hautes montagnes violettes, ruisseaux bleu fonce, zone orange (a niveler), volcan final (rouge)."""
import sys, os, json, struct, zlib, shutil
import numpy as np
from PIL import Image
from scipy.ndimage import (distance_transform_edt as edt, gaussian_filter, zoom, map_coordinates,
                           binary_closing, binary_dilation, binary_fill_holes, label)

sketch, places_src, out = sys.argv[1:4]
bin_out = sys.argv[4] if len(sys.argv) > 4 else None
places_out = sys.argv[5] if len(sys.argv) > 5 else None
os.makedirs(out, exist_ok=True)
N, TOP, SEA = 2000, 448, 63

# ------------------------------------------------------------------ lecture du croquis
src = Image.open(sketch).convert('RGB')
a = np.asarray(src).astype(int)
H0, W0 = a.shape[:2]
r, g, b = a[..., 0], a[..., 1], a[..., 2]
sea0 = (g > 150) & (b - r > 40) & (b > 195) & ~((r < 90) & (g < 90))
river0 = (b > 150) & (r < 90) & (g < 90)
purple0 = (b - g > 55) & (r - g > 15) & (b > 120) & ~river0
forest0 = (g > r + 40) & (g > b + 40) & (r < 100)
grey0 = (abs(r - g) < 14) & (abs(g - b) < 14) & (r > 115) & (r < 205)
orange0 = (r > 200) & (g > 40) & (g < 120) & (b < 60)
red0 = (r > 200) & (g < 60) & (b < 60)
black0 = (a.sum(2) < 100)

def up(mask, sigma=2.5, thr=.5):
    f = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).resize((N, N), Image.BILINEAR)).astype(np.float32) / 255
    return gaussian_filter(f, sigma) > thr
def clean(m, it, minsize=0):
    m = binary_fill_holes(binary_closing(m, iterations=it))
    if minsize:
        lab, n = label(m); sz = np.bincount(lab.ravel()); sz[0] = 0; m = sz[lab] >= minsize
    return m

# zones de plaine : couleur la plus proche, le reste est rempli par le voisin le plus proche
ZC = np.array([(190, 235, 158), (255, 235, 158), (188, 180, 150)])
d = ((a[:, :, None, :] - ZC[None, None]) ** 2).sum(3)
zone0 = d.argmin(2); okz = (d.min(2) < 260) & ~sea0 & ~grey0 & ~forest0 & ~river0 & ~purple0
idx = edt(~okz, return_distances=False, return_indices=True)
zone0 = zone0[idx[0], idx[1]]
oh = np.stack([(zone0 == i).astype(np.float32) for i in range(3)])
oh = np.stack([gaussian_filter(np.asarray(Image.fromarray(o).resize((N, N), Image.BILINEAR)), 5) for o in oh])
zone = oh.argmax(0).astype(np.uint8)                                 # 0 vert, 1 jaune, 2 cendre

# mer + bande de mer sur le pourtour (limite du monde)
rng = np.random.default_rng(7)
def octave(sz):
    return zoom(rng.random((N//sz+2, N//sz+2)).astype(np.float32), N/(N//sz+2), order=3)[:N, :N]
def nrm(o): return (o - o.mean()) / o.std()
yy, xx = np.mgrid[0:N, 0:N]
dedge = np.minimum(np.minimum(xx, yy), np.minimum(N-1-xx, N-1-yy))
sea = up(sea0 | (black0 & False), 3) | (dedge < (22 + 28 * octave(100)))
sea = binary_fill_holes(~binary_fill_holes(~sea)) if False else sea
land = ~sea
forest = up(clean(forest0, 2), 3) & land
rockf = up(clean(grey0, 3, 12), 2.5) & land
peak = up(clean(purple0, 2, 12), 4) & land
oz = orange0
orange = up(oz, 1.5, .3)
orzd = binary_dilation(orange, iterations=10)
rockf &= ~orzd; peak &= ~orzd            # la zone orange reste libre de montagnes : on y passe a pied
rock = rockf | peak
river = up(river0, 1.2, .3)

# ------------------------------------------------------------------ relief
def warped(f, amp=38):
    wx, wy = nrm(octave(130)) * amp, nrm(octave(110)) * amp
    return map_coordinates(f, [yy + wy, xx + wx], order=1, mode='reflect')
def bell(m, s): return gaussian_filter(m.astype(np.float32), s)
def ridged(sz): return np.clip(1 - np.abs(nrm(octave(sz))) * 0.9, 0, 1)

d_sea = edt(land)
ash_near = edt(zone != 2) < 40

# plaines : mini-reliefs
rel = 5 * nrm(octave(260)) + 3 * nrm(octave(110)) + 1.6 * nrm(octave(45)) + .7 * nrm(octave(18))
amp = np.where(zone == 2, .5, np.where(zone == 1, .95, 1.0))
h = 72 + rel * amp + np.where(forest, 3, 0)

# montagnes : enveloppe large + aretes (bruit en cretes) comme le relief vanilla
R = .5 * warped(ridged(140)) + .3 * warped(ridged(60)) + .2 * warped(ridged(26))
F = .32 + .95 * np.clip(R, 0, 1) ** 1.5
env_h = np.clip(np.maximum(1.25 * bell(rockf, 18), .5 * bell(rock, 55)), 0, 1)
env_p = np.clip(np.maximum(1.3 * bell(peak, 20), .5 * bell(peak, 58)), 0, 1)
Ah = np.where(ash_near, 150, 100); Ap = np.where(ash_near, 300, 175)
mount = np.maximum(Ah * env_h ** 1.15 * F, Ap * env_p ** 1.15 * F)
mount = mount + (octave(12) - .5) * 10 * np.clip(mount / 60, 0, 1)
h = h + mount * np.clip(d_sea / 45.0, 0, 1)

# volcan final (tache rouge du croquis)
ys, xs = np.where(red0)
vx, vy = xs.mean() / W0 * N, ys.mean() / H0 * N
dv = np.hypot(xx - vx, yy - vy)
RC, CD = 30.0, 62.0                                                # rayon et profondeur du cratere (cases de 4 blocs)
cone = 305 * np.clip(1 - np.maximum(dv, RC) / 150, 0, 1) ** 1.55 * (.93 + .14 * F)
bowl = CD * np.clip(1 - np.clip(dv / RC, 0, 1) ** 2, 0, 1) ** .6
volcano = np.clip(cone - bowl, 0, None) * (dv < 150)
h = np.maximum(h, 72 + volcano)

# zone orange : a peu pres au meme niveau (plateau), raccord progressif
orz = binary_fill_holes(binary_closing(orange, iterations=18)) | binary_dilation(orange, iterations=6)
w = np.clip(1 - edt(~orz) / 30, 0, 1); w = w * w * (3 - 2 * w)
w = w * np.clip((dv - 36) / 40, 0, 1)            # le volcan garde sa forme
level = 80 + .8 * nrm(octave(40)) + .4 * nrm(octave(14))
h = np.where(land, h * (1 - w) + level * w, h)

# cote : descente progressive
h = np.where(land, np.minimum(h, SEA + .8 + d_sea * .33 + (nrm(octave(60))) * 1.2 + (mount + volcano) * np.clip(d_sea / 45.0, 0, 1)), h)

# ruisseaux (comme en vanilla) : lit au niveau de la mer, berges en pente douce, hors montagnes
envm = np.maximum(env_h, env_p)
river_ok = river & land & (envm < .3) & (h < 100) & (w < .5)
dr = edt(~river_ok)
valley = (dr < 44) & land & (h < 104) & (envm < .45)
h = np.where(valley, np.minimum(h, SEA + .6 + dr * .5 + nrm(octave(30)) * .6), h)
carve = river_ok | ((dr <= 1.6) & valley)
carve &= (dr <= 1.6)
wl = np.zeros((N, N), np.float32); wl[carve] = SEA
h = np.where(carve, SEA - 4 + dr * 1.2, h)

# fonds marins
d_land = edt(sea)
h = np.where(sea, SEA - 3 - np.minimum(d_land * .38, 30) + nrm(octave(50)) * 1.5, h)
h = np.where(land, gaussian_filter(h, .8), h)
h = np.clip(h, -30, TOP - 12)

# ------------------------------------------------------------------ biomes
B_FORET, B_MONTS, B_CIMES, B_MER, B_RIVAGE, B_RIVIERE, B_MONTS_CENDRE, B_CLAIR, B_BOSQ = 3, 4, 5, 6, 7, 8, 9, 10, 11
bio = zone.astype(np.uint8).copy()
fd = gaussian_filter(forest.astype(np.float32), 16) + (octave(30) - .5) * .35        # degrade des forets
bio[(fd > .14) & (zone != 2)] = B_BOSQ
bio[(fd > .30) & (zone != 2)] = B_CLAIR
bio[(fd > .52) & (zone != 2)] = B_FORET
jit = (octave(20) - .5) * .12
roc = (envm + jit > .3) | (volcano > 25)
bio[roc] = B_MONTS
bio[(env_p + jit > .38) & ~ash_near] = B_CIMES
bio[(roc & ash_near) | (volcano > 25)] = B_MONTS_CENDRE
bio[carve] = B_RIVIERE
beach = land & (d_sea < 16) & (h < SEA + 4) & ~roc & ~carve
bio[beach] = B_RIVAGE
bio[sea] = B_MER

# ------------------------------------------------------------------ sorties
GR = np.array([(190, 235, 158), (255, 235, 158), (150, 147, 142)], np.float32)
prev = GR[zone].copy()
for i, col in ((B_FORET, (40, 110, 50)), (B_CLAIR, (110, 170, 90)), (B_BOSQ, (150, 205, 120)), (B_MONTS, (125, 125, 125)),
               (B_CIMES, (240, 245, 255)), (B_MONTS_CENDRE, (96, 80, 72)), (B_RIVIERE, (110, 170, 235)), (B_RIVAGE, (240, 225, 160)), (B_MER, (60, 120, 200))):
    prev[bio == i] = col
shade = np.clip(.85 + (np.roll(h, 2, 0) - np.roll(h, -2, 0)) * 0.02, .4, 1.3)[..., None]
prev = np.where(sea[..., None], prev * (0.75 + .25 * np.clip((h + 30) / 90, 0, 1)[..., None]), prev * shade)
Image.fromarray(np.clip(prev, 0, 255).astype(np.uint8)).resize((1000, 1000), Image.LANCZOS).save(out + '/apercu_final.png')
Image.fromarray((h + 64).astype(np.uint16)).save(out + '/relief.png')
print('hauteurs', int(h.min()), int(h.max()), 'volcan', round(vx - 1000), round(vy - 1000),
      {i: round(float((bio == i).mean() * 100), 1) for i in range(12)})

if bin_out:
    hh = np.clip(np.rint(h + 64), 0, 65535).astype('>u2')
    ww = (np.clip(np.rint(wl + 64), 0, 65535) * (wl > 0)).astype('>u2')
    payload = hh.tobytes() + ww.tobytes() + bio.astype('u1').tobytes()
    os.makedirs(os.path.dirname(bin_out), exist_ok=True)
    open(bin_out, 'wb').write(b'RAMP' + struct.pack('>i', N) + zlib.compress(payload, 9))
    print('carte', os.path.getsize(bin_out) // 1024, 'Ko')
if places_out:
    os.makedirs(os.path.dirname(places_out), exist_ok=True)
    SC = 4
    pl = json.load(open(places_src))
    walk = ~(sea | carve) & (volcano < 25)
    ids = np.where(walk)
    out_pl = {}
    for k, v in pl.items():
        gx, gy = int(np.clip(v['x'] + 1000, 0, N - 1)), int(np.clip(v['z'] + 1000, 0, N - 1))
        if not walk[gy, gx]:
            dd = (ids[1] - gx) ** 2 + (ids[0] - gy) ** 2; j = dd.argmin(); gx, gy = int(ids[1][j]), int(ids[0][j])
        out_pl[k] = {'x': (gx - 1000) * SC, 'z': (gy - 1000) * SC}
    rim = (int(vx + RC * 1.15), int(vy))
    out_pl['mont_du_destin'] = {'x': (rim[0] - 1000) * SC, 'z': (rim[1] - 1000) * SC}
    json.dump(out_pl, open(places_out, 'w'), indent=1)
    lava = int(h[int(vy), int(vx)]) + 30
    json.dump({'x': round((vx - 1000) * SC), 'z': round((vy - 1000) * SC), 'r': round(RC * SC * .98), 'level': lava},
              open(os.path.join(os.path.dirname(places_out), 'volcan.json'), 'w'))
    print('lave', lava, 'crater floor', int(h[int(vy), int(vx)]))
