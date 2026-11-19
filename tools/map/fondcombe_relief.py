"""Prolonge le relief de Fondcombe hors du plan : le rebord du plan (352 x 352) se continue en montagnes qui redescendent doucement vers la plaine, sans mur.
Usage : python3 fondcombe_relief.py plan.bin  (plan.bin : grille du plan RivendellPlan, voir tools/rivdump) ; idempotent (hauteurs = max(existant, cible)).
La grille du plan est lue en octets [y+30][z][x] (352 x 181 x 352)."""
import sys, zlib, struct, json, os
import numpy as np
from scipy.ndimage import gaussian_filter

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
    # distance au plan mesuree dans un espace deforme : la crete ne suit plus le rectangle du plan, elle serpente
    Xw, Zw = X + field(20, 55), Z + field(20, 55)
    xcw = np.clip(Xw, OX + 1, OX + W - 2); zcw = np.clip(Zw, OZ + 1, OZ + D - 2)
    d = np.hypot(Xw - xcw, Zw - zcw)
    xc = np.clip(X, OX + 1, OX + W - 2); zc = np.clip(Z, OZ + 1, OZ + D - 2)
    dl, dr_, du, dd = X - OX, OX + W - X, Z - OZ, OZ + D - Z                 # a l'interieur du plan : on projette sur le bord le plus proche
    mn = np.minimum(np.minimum(dl, dr_), np.minimum(du, dd))
    ins = (X > OX) & (X < OX + W) & (Z > OZ) & (Z < OZ + D)
    xc = np.where(ins & (mn == dl), OX + 1, np.where(ins & (mn == dr_), OX + W - 2, xc))
    zc = np.where(ins & (mn == du), OZ + 1, np.where(ins & (mn == dd), OZ + D - 2, zc))
    rim = top[(zc - OZ).astype(int), (xc - OX).astype(int)].astype(np.float32)
    rim = gaussian_filter(rim, 6)                                           # lisser le rebord le long de chaque cote (le plan est bosselé)
    rimv = np.maximum(0, rim - 3)
    rimv = np.minimum(95, rimv * sm((rimv - 14) / 24.0))                    # un rebord bas (arbres, toits) ne fait pas de montagne ; plafond de hauteur
    mod = np.clip(0.95 + 0.22 * field(26), 0.65, 1.2)                          # hauteurs variables : cols et sommets
    L = np.minimum(600, 220 + 4.2 * rimv) * np.clip(1 + 0.25 * field(30), 0.7, 1.35)   # pente tres longue : environ 1 bloc de haut pour 3 a 4 de large
    s_ = np.clip(d / L, 0, 1)
    f = (1 - s_) ** 2.2                                                     # profil concave : raide pres de la crete, longue queue douce vers la plaine
    noise = field(9)
    ridge = np.clip(1 - np.abs(gaussian_filter(rng.standard_normal(X.shape), 7) * 3.0), 0, 1)
    t = rimv * mod * f * (1 + 0.12 * noise * (1 - s_))
    for k in range(14):                                                     # sommets isoles derriere le rebord, a longues pentes concaves
        ang = rng.uniform(0, 2 * np.pi)
        rr = rng.uniform(210, 330)
        px_, pz_ = OX + W / 2 + np.cos(ang) * rr * 1.05, OZ + D / 2 + np.sin(ang) * rr * 1.05
        a_ = rng.uniform(0.2, 0.45)
        rho = rng.uniform(110, 190)
        if pz_ > OZ + D - 40:
            continue                                                        # pas de sommet au sud (la plaine)
        cx_ = int(np.clip(px_, OX + 1, OX + W - 2) - OX); cz_ = int(np.clip(pz_, OZ + 1, OZ + D - 2) - OZ)
        a_ *= max(30.0, float(top[cz_, cx_]) - 3)
        rp = np.hypot(X - px_, Z - pz_)
        t = t + a_ * (1 + (rp / rho) ** 2) ** -1.3 * (0.8 + 0.2 * ridge)
    # on ne touche pas l'interieur du plan ni la plaine au sud ; on estompe vers l'est (Argonath)
    fe = 1.0
    fa = sm((np.hypot(X + 1112, Z + 525) - 40) / 100.0)                    # on ne touche pas au Comte, juste au nord-est du site
    t = t * fe * fa
    # a l'interieur, seule la bande de raccord du plan (30 blocs) est relevee : le sol de la carte y continue le relief exterieur (sinon mur droit au bord du plan)
    inside = (X > OX) & (X < OX + W) & (Z > OZ) & (Z < OZ + D)
    din = np.minimum(np.minimum(X - OX, OX + W - X), np.minimum(Z - OZ, OZ + D - Z))
    t = t * np.where(inside, 1 - sm((din - 26) / 12.0), 1.0)                # dans le plan, seule la bande de raccord garde le relief (continuite avec l'exterieur)
    target = Y0S + 3 + t
    sub = h[gz0:gz1, gx0:gx1]
    raise_ = np.where((t > 1.5) & (target > sub), np.rint(target), sub).astype(np.int32)
    h[gz0:gz1, gx0:gx1] = raise_
    sb = bio[gz0:gz1, gx0:gx1]
    rocky = t > 75
    sb[rocky & (sb != 6) & (sb != 7)] = 4
    cimes = rocky & (t > 135)
    sb[cimes] = 5
    bio[gz0:gz1, gx0:gx1] = sb
    raw2 = h.astype('>i2').tobytes() + wat.astype('>i2').tobytes() + bio.tobytes()
    open(BIN, 'wb').write(b[:8] + zlib.compress(raw2, 9))
    print('hauteur max ajoutee', int((raise_ - sub).max()), 'cases changees', int((raise_ != sub).sum()))


if __name__ == '__main__':
    main(sys.argv[1])
