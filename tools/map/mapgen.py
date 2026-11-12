"""Croquis -> carte 2000x2000 fidele aux formes dessinees.
Usage: mapgen.py croquis_relief.png croquis_zones.png sortie/
  croquis_relief : forets (vert fonce), montagnes (gris), hautes montagnes (violet), rivieres
  croquis_zones  : mer (cyan) + herbes (vert clair / jaune pale / gris cendre)
Sorties : classes.png, zones.png, relief.png (hauteur+64 en uint16), apercu_final.png"""
import sys, os, numpy as np
from PIL import Image, ImageFilter
from scipy.ndimage import (distance_transform_edt as edt, binary_dilation, binary_closing,
                           gaussian_filter, zoom, label)

relief_src, zones_src, out = sys.argv[1:4]
bin_out = sys.argv[4] if len(sys.argv) > 4 else None
places_out = sys.argv[5] if len(sys.argv) > 5 else None
os.makedirs(out, exist_ok=True)
N = 2000
TOP = 448            # hauteur max de la dimension (de -64 a 448)
SEA_LEVEL = 63
GRASS = {'vert': (190, 235, 158), 'jaune': (255, 235, 158), 'gris': (150, 147, 142)}

# ---------- zones : mer + herbes (cadre noir du 2e croquis rogne) ----------
z = np.asarray(Image.open(zones_src).convert('RGB').crop((125, 125, 1875, 1205))
               .resize((N, N), Image.NEAREST)).astype(int)
zr, zg, zb = z[..., 0], z[..., 1], z[..., 2]
sea = (zb - zr > 35) & (zb > 200)
_rng0 = np.random.default_rng(11)
def _oct(sz):
    return zoom(_rng0.random((N//sz+2, N//sz+2)).astype(np.float32), N/(N//sz+2), order=3)[:N, :N]
_yy, _xx = np.mgrid[0:N, 0:N]
_dedge = np.minimum(np.minimum(_xx, _yy), np.minimum(N-1-_xx, N-1-_yy))
_band = _dedge < (45 + 55*(_oct(120)*.7 + _oct(40)*.3))                 # bande de mer sur le pourtour
sea = sea | _band
unknown = (z.sum(2) < 200)                                   # traits noirs de cote
ref = np.array(list(GRASS.values()))
zone = ((z[:, :, None, :] - ref[None, None]) ** 2).sum(3).argmin(2).astype(np.uint8)
# traits noirs : on prend le voisin connu le plus proche
known = ~unknown
idx = edt(~known, return_distances=False, return_indices=True)
zone = zone[idx[0], idx[1]]
sea = sea[idx[0], idx[1]]
land = ~sea

# ---------- relief : forets, montagnes, pics, rivieres ----------
a = np.asarray(Image.open(relief_src).convert('RGB').resize((N, N), Image.BILINEAR)).astype(int)
r, g, b = a[..., 0], a[..., 1], a[..., 2]
red = binary_dilation((r > 170) & (g < 110) & (b < 110), iterations=5)     # texte des lieux
grey = (abs(r-g) < 22) & (abs(g-b) < 28) & (r < 195) & (r > 70)
violet = (b-g > 55) & (r-g > 15) & (b > 120)
forest = (g > r+18) & (g > b+18)
river = (b > g+12) & (b > r+30) & ~((b-r > 35) & (g >= r) & (b-g < 14))
ridx = edt(red, return_distances=False, return_indices=True)     # texte efface : valeur du voisin
grey, violet, forest, river = (m[ridx[0], ridx[1]] for m in (grey, violet, forest, river))
def clean(m, close=7, minsize=400):
    m = binary_closing(m, iterations=close) & ~red
    lab, n = label(m); sz = np.bincount(lab.ravel()); sz[0] = 0
    return (sz[lab] >= minsize)
forest = clean(forest, 3) & land
hill = clean(grey, 6) & land
peak = clean(violet, 5, 600) & land
river = binary_dilation(river & ~red & land, iterations=1)
hill &= ~forest | hill        # une montagne peut etre dans une foret

# classes : 0 mer, 1 plaine, 2 foret, 3 montagne, 4 haute montagne, 5 riviere
c = np.ones((N, N), np.uint8)
c[forest] = 2; c[hill] = 3; c[peak] = 4; c[river & ~hill & ~peak] = 5; c[sea] = 0
Image.fromarray(c).save(out + '/classes.png')
Image.fromarray(zone).save(out + '/zones.png')

# ---------- hauteurs en blocs (le bord de chaque forme reste a sa place) ----------
def smooth(t):
    t = np.clip(t, 0, 1); return t*t*(3-2*t)
rng = np.random.default_rng(7)
def octave(sz):
    return zoom(rng.random((N//sz+2, N//sz+2)).astype(np.float32), N/(N//sz+2), order=3)[:N, :N]
nz = .5*octave(250) + .3*octave(90) + .15*octave(30) + .05*octave(10)
ridge = 1 - abs(2*octave(55) - 1)

h = 72 + (nz-.5)*12                                   # plaines douces
h = np.where(forest, h + 3, h)
d_sea = edt(land)                                     # distance a la mer (cote)
h = np.where(land, np.minimum(h, SEA_LEVEL + 2 + d_sea*0.8), h)     # plages
from scipy.ndimage import maximum_filter
def mountain(mask, base_top, var):
    """Sommets en pointe : la hauteur monte jusqu'a l'axe de la chaine (profil concave),
    plusieurs sommets d'altitude differente le long de la chaine."""
    d = edt(mask)
    dloc = maximum_filter(d, size=90)                       # force locale de la crete
    t = np.clip(d / np.maximum(dloc, 1), 0, 1)
    summits = np.clip(octave(110)*2.2 - .35, .35, 1.0)      # quelques grands pics, le reste plus bas
    top = 90 + (base_top - 90) * summits
    prof = t ** 1.45                                         # pointe, pas de plateau
    crest = ridge * t * var                                  # aretes
    return 72 + (top - 72) * prof + crest, d
hh, d_hill = mountain(hill, 250, 25)
h = np.where(hill, np.maximum(h, hh), h)
hp, d_peak = mountain(peak, TOP - 14, 40)
hp = np.minimum(hp, TOP - 8)
h = np.where(peak, np.maximum(h, hp), h)
# la montagne grise sert de pied aux pics : lissage tres leger sans bouger les bords
h = np.where(land, gaussian_filter(h, 1.0), h)
# rivieres creusees dans les terres basses
carve = river & land & (h < 100)
wl = np.zeros((N, N), np.float32)
wl[carve] = h[carve] - 1                 # niveau de l'eau de la riviere
h = np.where(carve, h - 4, h)            # lit creuse
# fonds marins
d_land = edt(sea)
h = np.where(sea, SEA_LEVEL - 4 - np.minimum(d_land/40, 1)*30 + (nz-.5)*6, h)
h = np.clip(h, -30, TOP-8)
Image.fromarray((h + 64).astype(np.uint16)).save(out + '/relief.png')

# ---------- apercu ----------
cols = np.array(list(GRASS.values()), np.float32)
prev = cols[zone].copy()
prev[forest] = (40, 110, 50); prev[hill] = (125, 125, 125); prev[peak] = (170, 90, 210)
prev[river & ~hill & ~peak] = (110, 170, 235); prev[sea] = (60, 120, 200)
shade = np.clip(.85 + (np.roll(h, 2, 0) - np.roll(h, -2, 0)) * 0.012, .45, 1.25)[..., None]
prev = np.where(sea[..., None], prev * (0.75 + .25*np.clip((h+30)/90, 0, 1)[..., None]), prev * shade)
Image.fromarray(np.clip(prev, 0, 255).astype(np.uint8)).resize((1000, 1000), Image.LANCZOS).save(out + '/apercu_final.png')
print('hauteurs min/max', int(h.min()), int(h.max()),
      {k: round(float((c == i).mean()*100), 1) for i, k in enumerate(['mer', 'plaine', 'foret', 'montagne', 'pics', 'riviere'])})

# ---------- biomes finaux (indices lus par le mod) ----------
B_FORET, B_MONTS, B_CIMES, B_MER, B_RIVAGE, B_RIVIERE = 3, 4, 5, 6, 7, 8
bio = zone.astype(np.uint8).copy()                          # 0 vert, 1 jaune, 2 cendre
bio[forest] = B_FORET; bio[hill] = B_MONTS; bio[peak] = B_CIMES
bio[carve] = B_RIVIERE
beach = land & (edt(land) < 7) & (h < SEA_LEVEL + 6) & ~hill & ~peak & ~carve
bio[beach] = B_RIVAGE
bio[sea] = B_MER
print('biomes', {i: round(float((bio == i).mean()*100), 1) for i in range(9)})

# ---------- lieux (points rouges / bleus du croquis) ----------
from scipy.ndimage import binary_erosion
o = np.asarray(Image.open(relief_src).convert('RGB')).astype(int)
H0, W0 = o.shape[:2]
PLACES = {'comte': (385, 336), 'fondcombe': (335, 376), 'moria': (747, 366), 'montagne_solitaire': (939, 230),
          'ortanc': (754, 539), 'argonath': (839, 625), 'edoras': (749, 648), 'gouffre_de_helm': (709, 710),
          'minas_terith': (830, 743), 'morannon': (1015, 697), 'minas_morgul': (1077, 701),
          'barad_dur': (1167, 693), 'mont_du_destin': (1163, 791)}
redm = (o[..., 0] > 200) & (o[..., 1] < 70) & (o[..., 2] < 70)
core = binary_erosion(redm, iterations=2)
places = {}
for name, (px, py) in PLACES.items():
    win = np.zeros_like(core); win[max(py-22, 0):py+22, max(px-22, 0):px+22] = True
    ys, xs = np.where(core & win)
    cx, cy = (xs.mean(), ys.mean()) if len(xs) > 3 else (px, py)
    places[name] = {'x': int(round(cx / W0 * N - N/2)), 'z': int(round(cy / H0 * N - N/2))}
print('lieux', places)

if bin_out:
    import zlib, struct, json
    hh = np.clip(np.rint(h + 64), 0, 65535).astype('>u2')
    ww = np.clip(np.rint(wl + 64), 0, 65535).astype('>u2') * (wl > 0)
    ww = ww.astype('>u2')
    payload = hh.tobytes() + ww.tobytes() + bio.astype('u1').tobytes()
    os.makedirs(os.path.dirname(bin_out), exist_ok=True)
    open(bin_out, 'wb').write(b'RAMP' + struct.pack('>i', N) + zlib.compress(payload, 9))
    print('carte', bin_out, os.path.getsize(bin_out)//1024, 'Ko')
if places_out:
    import json
    os.makedirs(os.path.dirname(places_out), exist_ok=True)
    json.dump(places, open(places_out, 'w'), indent=1)
