"""Prolonge le relief de Fondcombe hors du plan : le rebord du plan (352 x 352) se continue en montagnes qui redescendent doucement vers la plaine, sans mur.
Usage : python3 fondcombe_relief.py plan.bin  (plan.bin : grille du plan RivendellPlan, voir tools/rivdump) ; idempotent (hauteurs = max(existant, cible)).
La grille du plan est lue en octets [y+30][z][x] (352 x 181 x 352)."""
import sys, zlib, struct, json, os
import numpy as np
from scipy.ndimage import gaussian_filter

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'data', 'ringsofash', 'map', 'hautemarche.bin')
W = D = 352; OFF = 30; HH = OFF + 150 + 1
PX, PZ = -519, -118                      # centre du site
OX, OZ = PX - 176, PZ - 176              # origine du plan
Y0S = 132                                # hauteur stockee du niveau 0 du plan (carte : hauteur reelle + 64)


def sm(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


def main(plan):
    g = np.fromfile(plan, dtype=np.uint8).reshape(HH, D, W)
    nz = g != 0
    top = (HH - 1 - np.argmax(nz[::-1], axis=0)) - OFF                      # [z][x]
    b = open(BIN, 'rb').read()
    n = struct.unpack('>i', b[4:8])[0]
    raw = zlib.decompress(b[8:])
    h = np.frombuffer(raw[:n * n * 2], dtype='>i2').reshape(n, n).astype(np.int32)
    wat = np.frombuffer(raw[n * n * 2:n * n * 4], dtype='>i2').reshape(n, n).copy()
    bio = np.frombuffer(raw[n * n * 4:], dtype=np.uint8).reshape(n, n).copy()
    cs = 2.5
    # fenetre autour du plan
    R = 220
    gx0, gx1 = int((OX - R + 2500) / cs), int((OX + W + R + 2500) / cs)
    gz0, gz1 = int((OZ - R + 2500) / cs), int((OZ + D + R + 2500) / cs)
    xs = (np.arange(gx0, gx1) + 0.5) * cs - 2500
    zs = (np.arange(gz0, gz1) + 0.5) * cs - 2500
    X, Z = np.meshgrid(xs, zs)
    xc = np.clip(X, OX + 1, OX + W - 2); zc = np.clip(Z, OZ + 1, OZ + D - 2)
    d = np.hypot(X - xc, Z - zc)
    rim = top[(zc - OZ).astype(int), (xc - OX).astype(int)].astype(np.float32)
    # lisser le rebord le long de chaque cote (le plan est bosselé)
    rim = gaussian_filter(rim, 6)
    rimv = np.maximum(0, rim - 3)
    L = np.minimum(170, 70 + rimv)
    f = 1 - sm(d / L)
    rng = np.random.default_rng(1215)
    noise = gaussian_filter(rng.standard_normal(X.shape), 7); noise /= noise.std()
    ridge = np.clip(1 - np.abs(gaussian_filter(rng.standard_normal(X.shape), 5) * 3.2), 0, 1)
    bell = np.exp(-(((d - 0.45 * L) / (0.3 * L)) ** 2))
    t = rimv * f * (1 + 0.10 * noise) + 0.30 * rimv * bell * (0.6 + 0.4 * ridge)
    # on ne touche pas l'interieur du plan ni la plaine au sud ; on estompe vers l'est (Argonath)
    fe = 1 - sm((X - (OX + W)) / 70.0)
    fa = sm((np.hypot(X + 310, Z + 480) - 150) / 60.0)
    t = t * fe * fa
    t[d <= 0] = 0
    target = Y0S + 3 + t
    sub = h[gz0:gz1, gx0:gx1]
    raise_ = np.where((t > 1.5) & (target > sub), np.rint(target), sub).astype(np.int32)
    h[gz0:gz1, gx0:gx1] = raise_
    sb = bio[gz0:gz1, gx0:gx1]
    rocky = (t > 28) & (target > 150)
    sb[rocky & (sb != 6) & (sb != 7)] = 4
    cimes = rocky & (target >= 262)
    sb[cimes] = 5
    bio[gz0:gz1, gx0:gx1] = sb
    raw2 = h.astype('>i2').tobytes() + wat.astype('>i2').tobytes() + bio.tobytes()
    open(BIN, 'wb').write(b[:8] + zlib.compress(raw2, 9))
    print('hauteur max ajoutee', int((raise_ - sub).max()), 'cases changees', int((raise_ != sub).sum()))


if __name__ == '__main__':
    main(sys.argv[1])
