"""La texture de la Flamme d'Udun : un feu rouge sang, anime sur huit images."""
import json
import math
import os
import random

from PIL import Image

ASSETS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'ringsofash')
BLOCK = os.path.join(ASSETS, 'textures', 'block')

N = 16          # une image fait 16 x 16
FRAMES = 8

# du coeur blanc-rouge jusqu'au rouge sombre qui s'eteint
RAMP = [
    (255, 236, 214), (255, 190, 120), (252, 120, 48),
    (226, 52, 26), (176, 18, 14), (116, 8, 10), (62, 4, 8),
]


def frame(rng, phase):
    """une langue de feu : haute au milieu, dechiquetee sur les bords"""
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    px = im.load()
    for x in range(N):
        d = abs(x - (N - 1) / 2.0) / ((N - 1) / 2.0)          # 0 au centre, 1 sur les bords
        # la hauteur ondule avec le temps, et chaque colonne a son propre frisson
        wave = math.sin(phase * 2 * math.pi + x * 0.9) * 1.6
        top = (1.0 - d * d) * (N - 2) + wave - rng.random() * 2.2
        h = max(0, int(top))
        for y in range(N - 1, N - 1 - h, -1):
            up = (N - 1 - y) / max(1.0, float(h))            # 0 au pied, 1 a la pointe
            # le coeur est clair, la pointe sombre, les bords aussi
            t = up * 0.75 + d * 0.45 + rng.random() * 0.12
            k = min(len(RAMP) - 1, int(t * len(RAMP)))
            r, g, b = RAMP[k]
            a = 255 if up < 0.82 else 170
            px[x, y] = (r, g, b, a)
    return im


def main():
    sheet = Image.new('RGBA', (N, N * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        rng = random.Random(1000 + f)                        # stable d'une generation a l'autre
        sheet.paste(frame(rng, f / float(FRAMES)), (0, f * N))
    sheet.save(os.path.join(BLOCK, 'udun_fire.png'))
    with open(os.path.join(BLOCK, 'udun_fire.png.mcmeta'), 'w') as fh:
        json.dump({'animation': {'frametime': 2}}, fh)
    print('ok', sheet.size)


if __name__ == '__main__':
    main()
