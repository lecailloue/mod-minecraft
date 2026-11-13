"""Convertit un modele Blockbench (.bbmodel, format "modded entity") en JSON de geometrie du mod.
Usage : python3 bbimport.py modele.bbmodel sortie.json
Regles (verifiees sur le modele du Fleau de Braise) : x inverse, y vers le bas, pivots enfants relatifs au parent."""
import json, math, sys

def convert(path):
    b = json.load(open(path))
    els = {e['uuid']: e for e in b['elements']}
    res = b.get('resolution', {'width': 64, 'height': 64})
    parts = []
    def walk(nodes, parent):
        for n in nodes:
            if not isinstance(n, dict): continue
            ox, oy, oz = n['origin']
            if parent is None: pv = [-ox, 24 - oy, oz]
            else:
                px, py, pz = parent['origin']
                pv = [-(ox - px), py - oy, oz - pz]
            rot = n.get('rotation', [0, 0, 0])
            boxes = []
            for c in n['children']:
                if isinstance(c, str):
                    e = els[c]
                    if e.get('type', 'cube') != 'cube': continue
                    f, t = e['from'], e['to']
                    bx, by, bz = e.get('origin', n['origin']) if False else n['origin']
                    s = [t[0] - f[0], t[1] - f[1], t[2] - f[2]]
                    boxes.append({'o': [-(t[0] - ox), oy - t[1], f[2] - oz], 's': s, 'uv': e.get('uv_offset', [0, 0]),
                                  'inflate': e.get('inflate', 0.0), 'mirror': bool(e.get('mirror_uv', False))})
            parts.append({'name': n['name'], 'parent': parent['name'] if parent else None,
                          'pivot': [round(v, 4) for v in pv], 'rot': [-math.radians(rot[0]), -math.radians(rot[1]), math.radians(rot[2])], 'boxes': boxes})
            walk(n['children'], n)
    walk(b['outliner'], None)
    return {'tw': res['width'], 'th': res['height'], 'parts': parts}

if __name__ == '__main__':
    json.dump(convert(sys.argv[1]), open(sys.argv[2], 'w'), separators=(',', ':'))
