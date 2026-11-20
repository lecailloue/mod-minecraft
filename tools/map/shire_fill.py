"""Comble la cuvette ("cratere") qui entourait le Comte dans la carte : remplissage des depressions fermees (remplissage par priorite, jusqu'a la hauteur du col),
lisse sur quelques cases pour que le sol reste naturel. Modifie hautemarche.bin sur place ; idempotent (les hauteurs ne font que monter).
Usage : python3 shire_fill.py"""
import heapq, os, struct, zlib
import numpy as np
from scipy.ndimage import gaussian_filter

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'data', 'ringsofash', 'map', 'hautemarche.bin')
CS = 2.5
X0, X1, Z0, Z1 = -1300, -900, -700, -380           # fenetre de travail autour du Comte (-1112, -525)


def fill(h, eps=0.04):
    """remplissage des depressions : toute case dont aucun chemin ne descend jusqu'au bord de la fenetre est remontee au col le plus bas (+ pente minime vers la sortie)"""
    ny, nx = h.shape
    out = np.full(h.shape, np.inf)
    pq = []
    for z in range(ny):
        for x in range(nx):
            if z in (0, ny - 1) or x in (0, nx - 1):
                out[z, x] = h[z, x]
                heapq.heappush(pq, (h[z, x], z, x))
    while pq:
        v, z, x = heapq.heappop(pq)
        if v > out[z, x]: continue
        for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            zz, xx = z + dz, x + dx
            if 0 <= zz < ny and 0 <= xx < nx:
                nv = max(h[zz, xx], v + eps)
                if nv < out[zz, xx]:
                    out[zz, xx] = nv
                    heapq.heappush(pq, (nv, zz, xx))
    return out


def main():
    b = open(BIN, 'rb').read()
    n = struct.unpack('>i', b[4:8])[0]
    raw = zlib.decompress(b[8:])
    h = np.frombuffer(raw[:n * n * 2], dtype='>i2').reshape(n, n).astype(np.float64)
    rest = raw[n * n * 2:]
    gx0, gx1, gz0, gz1 = [int((v + 2500) / CS) for v in (X0, X1, Z0, Z1)]
    win = h[gz0:gz1, gx0:gx1]
    delta = fill(win) - win
    delta[delta < 1.0] = 0                                   # pas de micro-remplissages (le relief fin reste intact)
    print('cases remontees : %d, remplissage max %.1f, moyen %.1f' % ((delta > 0).sum(), delta.max(), delta[delta > 0].mean() if (delta > 0).any() else 0))
    mask = gaussian_filter((delta > 0).astype(np.float64), 3)
    delta = gaussian_filter(delta, 2.5)
    bump = gaussian_filter(np.random.default_rng(1112).normal(size=delta.shape), 5)      # le sol rempli ondule doucement (+-2 blocs), il n'est pas plat comme une table
    delta = delta + mask * bump / bump.std() * 2.0
    # on ne touche pas aux cases d'eau / de mer
    new = np.where(win > 66 + 64, win + delta, win)
    h[gz0:gz1, gx0:gx1] = np.maximum(win, np.round(new))
    data = h.astype('>i2').tobytes() + rest
    open(BIN, 'wb').write(b[:8] + zlib.compress(data, 9))


if __name__ == '__main__':
    main()
