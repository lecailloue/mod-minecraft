"""Prolonge le relief de Fondcombe hors du plan : le rebord du plan (352 x 352) se continue en montagnes qui redescendent doucement vers la plaine, sans mur.
Usage : python3 fondcombe_relief.py plan.bin  (plan.bin : grille du plan RivendellPlan, voir tools/rivdump) ; idempotent (hauteurs = max(existant, cible)).
La grille du plan est lue en octets [y+30][z][x] (352 x 181 x 352)."""
import sys, zlib, struct, json, os
import numpy as np
from scipy.ndimage import gaussian_filter, distance_transform_edt

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'data', 'ringsofash', 'map', 'hautemarche.bin')
W = D = 352; OFF = 30; HH = OFF + 150 + 1
PX, PZ = -1298, -295                    # centre du site (places.json)
OX, OZ = PX - 176, PZ - 176              # origine du plan
Y0S = 150                                # hauteur stockee du niveau 0 du plan (carte : hauteur reelle + 64) au centre du site


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
    R = 620
    gx0, gx1 = int((OX - R + 2500) / cs), int((OX + W + R + 2500) / cs)
    gz0, gz1 = int((OZ - R + 2500) / cs), int((OZ + D + R + 2500) / cs)
    xs = (np.arange(gx0, gx1) + 0.5) * cs - 2500
    zs = (np.arange(gz0, gz1) + 0.5) * cs - 2500
    X, Z = np.meshgrid(xs, zs)
    rng = np.random.default_rng(1215)

    def field(sig, amp=1.0):
        f_ = gaussian_filter(rng.standard_normal(X.shape), sig)
        return f_ / f_.std() * amp
    # relief source : le rebord du plan (bande de 40 blocs du bord), lisse et plafonne ; le relief exterieur est la dilatation de ce rebord par un cone (pente douce, sans couture ni cuvette)
    ins = (X > OX) & (X < OX + W) & (Z > OZ) & (Z < OZ + D)
    pi = np.clip((X - OX).astype(int), 0, W - 1); pj = np.clip((Z - OZ).astype(int), 0, D - 1)
    din0 = np.minimum(np.minimum(X - OX, OX + W - X), np.minimum(Z - OZ, OZ + D - Z))
    E = np.where(ins & (din0 < 40), gaussian_filter(top.astype(np.float32), 5)[pj, pi], 0).astype(np.float32)
    E = np.maximum(0, E - 3)
    E = np.minimum(80, E * sm((E - 14) / 24.0))
    E = gaussian_filter(E, 2)
    SL = 0.30                                                               # 1 bloc de haut pour ~3,3 de large
    t = np.zeros_like(E)
    for lv in range(2, 82, 2):
        m = E >= lv
        if not m.any():
            break
        dist = distance_transform_edt(~m) * cs
        t = np.maximum(t, lv - SL * dist)
    t = np.maximum(t, 0)
    t = gaussian_filter(t, 5)                                               # arrondit le pied et la crete de la pente
    t = np.maximum(0, t * (1 + 0.10 * field(14)))
    # on ne touche pas l'interieur du plan ni la plaine au sud ; on estompe vers l'est (Argonath)
    fe = 1.0
    fa = sm((np.hypot(X + 1112, Z + 525) - 40) / 100.0)                    # on ne touche pas au Comte, juste au nord-est du site
    t = t * fe * fa
    # a l'interieur, seule la bande de raccord du plan (30 blocs) est relevee : le sol de la carte y continue le relief exterieur (sinon mur droit au bord du plan)
    inside = (X > OX) & (X < OX + W) & (Z > OZ) & (Z < OZ + D)
    din = np.minimum(np.minimum(X - OX, OX + W - X), np.minimum(Z - OZ, OZ + D - Z))
    t = t * np.where(inside, 1 - sm((din - 26) / 12.0), 1.0)                # dans le plan, seule la bande de raccord garde le relief (continuite avec l'exterieur)
    sub = h[gz0:gz1, gx0:gx1]
    gs = gaussian_filter(sub.astype(np.float32), 14)                         # sol local lisse : la pente part du terrain reel, pas d'un plateau absolu
    target = sub + t
    riv = bio[gz0:gz1, gx0:gx1] == 8                                         # les rivieres restent en place : le relief s'estompe a leur approche (berges en pente douce)
    if riv.any():
        wgt = sm(distance_transform_edt(~riv) / 8.0)
        t = gaussian_filter(t * wgt, 2.5)
        t[riv] = 0
        target = sub + t
    raise_ = np.where((t > 0.5) & (target > sub), np.rint(target), sub).astype(np.int32)
    h[gz0:gz1, gx0:gx1] = raise_
    raw2 = h.astype('>i2').tobytes() + wat.astype('>i2').tobytes() + bio.tobytes()
    open(BIN, 'wb').write(b[:8] + zlib.compress(raw2, 9))
    print('hauteur max ajoutee', int((raise_ - sub).max()), 'cases changees', int((raise_ != sub).sum()))


if __name__ == '__main__':
    main(sys.argv[1])
