#!/usr/bin/env python3
"""Les croquis construits a la main dans le jeu (/anneaux croquis) -> data/ringsofash/map/croquis.bin.

    python3 tools/map/croquis.py <croquis.json[.gz]> ...     # ajoute ou remplace ces croquis
    python3 tools/map/croquis.py --liste                      # ce que contient le fichier

Chaque croquis est une boite de blocs relevee a un endroit precis du monde. Le mod la repose
telle quelle apres la generation (CroquisOverlay), donc ce que le joueur a construit se retrouve
a l'identique, meubles et detail compris. Les cases vides de la boite deviennent de l'air :
ce qui a ete casse reste casse.

Format (zlib) : 'CRO1', nombre de croquis, puis pour chacun :
  nom (short + utf8), origine x y z (int32), taille dx dy dz (int32),
  palette (int32 + chaines short+utf8), puis dx*dy*dz indices en varint, ordre y, z, x.
"""
import gzip, json, os, struct, sys, zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
BIN = os.path.join(ROOT, 'src/main/resources/data/ringsofash/map/croquis.bin')
SRC = os.path.join(HERE, 'croquis')          # les croquis d'origine, pour pouvoir tout refaire


def write_varint(out, v):
    while True:
        c = v & 127
        v >>= 7
        out.append(c | (128 if v else 0))
        if not v: return


def read_varint(b, p):
    v = sh = 0
    while True:
        c = b[p]; p += 1
        v |= (c & 127) << sh; sh += 7
        if c < 128: return v, p


def load_croquis(path):
    op = gzip.open if path.endswith('.gz') else open
    with op(path, 'rt', encoding='utf-8') as f:
        return json.load(f)


def read_bin():
    if not os.path.exists(BIN): return []
    raw = zlib.decompress(open(BIN, 'rb').read())
    n = struct.unpack('>i', raw[4:8])[0]
    p = 8
    out = []
    for _ in range(n):
        ln = struct.unpack('>H', raw[p:p + 2])[0]; p += 2
        nom = raw[p:p + ln].decode(); p += ln
        x, y, z, dx, dy, dz, np_ = struct.unpack('>iiiiiii', raw[p:p + 28]); p += 28
        pal = []
        for _ in range(np_):
            l2 = struct.unpack('>H', raw[p:p + 2])[0]; p += 2
            pal.append(raw[p:p + l2].decode()); p += l2
        cells = []
        for _ in range(dx * dy * dz):
            v, p = read_varint(raw, p)
            cells.append(v)
        out.append(dict(nom=nom, origine=(x, y, z), taille=(dx, dy, dz), pal=pal, cells=cells))
    return out


def write_bin(regions):
    out = bytearray(b'CRO1' + struct.pack('>i', len(regions)))
    for r in regions:
        b = r['nom'].encode()
        out += struct.pack('>H', len(b)) + b
        out += struct.pack('>iiiiiii', *r['origine'], *r['taille'], len(r['pal']))
        for s in r['pal']:
            sb = s.encode()
            out += struct.pack('>H', len(sb)) + sb
        for v in r['cells']:
            write_varint(out, v)
    data = zlib.compress(bytes(out), 9)
    os.makedirs(os.path.dirname(BIN), exist_ok=True)
    open(BIN, 'wb').write(data)
    return len(out), len(data)


def to_region(cro):
    dx, dy, dz = cro['taille']
    pal = ['minecraft:air']
    idx = {'minecraft:air': 0}
    cells = [0] * (dx * dy * dz)
    for b in cro['blocs']:
        x, y, z = b['p']
        s = b['b']
        i = idx.get(s)
        if i is None:
            i = len(pal); pal.append(s); idx[s] = i
        cells[(y * dz + z) * dx + x] = i
    return dict(nom=cro['nom'], origine=tuple(cro['origine']), taille=(dx, dy, dz), pal=pal, cells=cells)


def main(paths):
    regions = read_bin()
    for p in paths:
        cro = load_croquis(p)
        r = to_region(cro)
        regions = [q for q in regions if q['nom'] != r['nom']] + [r]
        os.makedirs(SRC, exist_ok=True)
        with gzip.open(os.path.join(SRC, r['nom'] + '.json.gz'), 'wt', encoding='utf-8') as f:
            json.dump(cro, f)
        x, y, z = r['origine']; dx, dy, dz = r['taille']
        print('%-20s %2d x %2d x %2d en (%d, %d, %d), %d blocs, %d types'
              % (r['nom'], dx, dy, dz, x, y, z, sum(1 for c in r['cells'] if c), len(r['pal'])))
    n, c = write_bin(regions)
    print('croquis.bin : %d croquis, %d Ko compresses' % (len(regions), c // 1024))


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if '--liste' in sys.argv:
        for r in read_bin():
            print(r['nom'], r['origine'], r['taille'], len(r['pal']), 'types')
    else:
        main(args)
