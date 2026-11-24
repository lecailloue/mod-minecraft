#!/usr/bin/env python3
"""Les zones peintes a la main sur la carte du monde -> data/ringsofash/map/zones.bin.

    python3 tools/map/zones.py <carte_peinte.png> [apercu.png]

L'image est la carte du monde telle que le joueur la voit (654 x 646), repeinte par-dessus avec
une couleur pleine par zone. Le repere image -> monde est celui de la carte du mod :
    px = (x + 2500) / 7.50856 - 8.479      py = (z + 2500) / 7.50856
Le fichier ecrit contient 'ZON1', la taille n de la grille (1000 cases de 5 blocs), puis les
n*n classes compressees en zlib. Zones.java relit ce fichier et adoucit les bords.
"""
import os, struct, sys, zlib
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(os.path.dirname(os.path.dirname(HERE)), 'src/main/resources/data/ringsofash/map/zones.bin')

SCALE, OX, OY = 7.50855779, -8.47920587, -6.97259634e-05
N = 1000                     # cases de 5 blocs sur 5000 blocs
HALF = 2500

# classe -> (nom, couleur peinte sur la carte)
CLASSES = [
    (1, 'comte',            (16, 255, 0)),
    (2, 'foret_elfique',    (255, 84, 0)),
    (3, 'fangorn',          (1, 107, 0)),
    (4, 'marais_des_morts', (199, 0, 255)),
    (5, 'foret_de_cypres',  (185, 53, 53)),
]
TOL = 60                     # distance max a la couleur de reference


def build(src, preview=None):
    im = np.array(Image.open(src).convert('RGB')).astype(int)
    h, w, _ = im.shape
    grid = np.zeros((N, N), np.uint8)
    # pour chaque case de la grille, la couleur du pixel correspondant
    j, i = np.meshgrid(np.arange(N), np.arange(N), indexing='ij')
    x = -HALF + i * (2 * HALF / N) + (HALF / N)
    z = -HALF + j * (2 * HALF / N) + (HALF / N)
    px = np.rint((x + HALF) / SCALE + OX).astype(int)
    py = np.rint((z + HALF) / SCALE + OY).astype(int)
    ok = (px >= 0) & (px < w) & (py >= 0) & (py < h)
    cols = np.zeros((N, N, 3), int)
    cols[ok] = im[py[ok], px[ok]]
    for cls, name, ref in CLASSES:
        d = np.abs(cols - np.array(ref)).sum(2)
        grid[ok & (d < TOL) & (grid == 0)] = cls
    # les petites taches (la lave du Mordor passe pour de l'orange) ne sont pas des zones
    try:
        from scipy import ndimage
        for cls, _, _ in CLASSES:
            lab, nb = ndimage.label(grid == cls)
            for k, size in enumerate(ndimage.sum(np.ones_like(lab), lab, range(1, nb + 1)), start=1):
                if size < 60: grid[lab == k] = 0
    except ImportError:
        pass
    raw = zlib.compress(grid.tobytes(), 9)
    with open(OUT, 'wb') as f:
        f.write(b'ZON1' + struct.pack('>i', N) + raw)
    tot = N * N
    print('zones.bin', len(raw) + 8, 'octets')
    for cls, name, _ in CLASSES:
        print('  %-18s %5d cases  %.2f %%' % (name, (grid == cls).sum(), 100.0 * (grid == cls).sum() / tot))
    if preview:
        pal = {0: (40, 40, 40)}
        for cls, _, ref in CLASSES:
            pal[cls] = ref
        out = np.zeros((N, N, 3), np.uint8)
        for k, c in pal.items():
            out[grid == k] = c
        Image.fromarray(out).save(preview)
    return grid


if __name__ == '__main__':
    build(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
