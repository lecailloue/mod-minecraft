#!/usr/bin/env python3
"""Exporte un modele geo (assets/ringsofash/geo/<nom>.json) en projet Blockbench (.bbmodel, format Modded Entity, box UV).
Usage : python3 tools/bbmodel.py demon [sortie.bbmodel]
Conventions Blockbench (Java) : x et y inverses par rapport a ModelPart, rotations x/y de signe oppose."""
import base64, json, math, os, sys, uuid

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(os.path.dirname(HERE), 'src/main/resources/assets/ringsofash')

def uid(): return str(uuid.uuid4())

def face_uvs(u, v, w, h, d, mirror):
    f = {'north': [u + d, v + d, u + d + w, v + d + h], 'east': [u, v + d, u + d, v + d + h],
         'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], 'west': [u + d + w, v + d, u + 2 * d + w, v + d + h],
         'up': [u + d + w, v + d, u + d, v], 'down': [u + d + 2 * w, v, u + d + w, v + d]}
    if mirror:
        f['east'], f['west'] = f['west'], f['east']
        for k in f: f[k] = [f[k][2], f[k][1], f[k][0], f[k][3]]
    return {k: {'uv': uv, 'texture': 0} for k, uv in f.items()}

def tex_entry(name, path, tw, th, i):
    data = base64.b64encode(open(path, 'rb').read()).decode()
    return {'path': '', 'name': name, 'folder': 'entity', 'namespace': 'ringsofash', 'id': str(i), 'width': tw, 'height': th,
            'uv_width': tw, 'uv_height': th, 'particle': i == 0, 'render_mode': 'default', 'render_sides': 'auto',
            'visible': True, 'mode': 'bitmap', 'saved': True, 'uuid': uid(), 'source': 'data:image/png;base64,' + data}

def convert(name):
    geo = json.load(open(f'{A}/geo/{name}.json'))
    tw, th = geo['tw'], geo['th']
    absp, groups, elements, roots = {}, {}, [], []
    for p in geo['parts']:
        base = absp[p['parent']] if p['parent'] else (0.0, 0.0, 0.0)
        P = tuple(base[i] + p['pivot'][i] for i in range(3))               # pivot absolu, espace ModelPart
        absp[p['name']] = P
        origin = [-P[0], 24 - P[1], P[2]]
        g = {'name': p['name'], 'origin': origin, 'rotation': [-math.degrees(p['rot'][0]), -math.degrees(p['rot'][1]), math.degrees(p['rot'][2])],
             'color': 0, 'uuid': uid(), 'export': True, 'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True, 'autouv': 0, 'children': []}
        g['rotation'] = [round(r, 4) + 0.0 for r in g['rotation']]
        for k, b in enumerate(p['boxes']):
            (ox, oy, oz), (w, h, d), (u, v) = b['o'], b['s'], b['uv']
            x0, y0, z0 = P[0] + ox, P[1] + oy, P[2] + oz
            e = {'name': f"{p['name']}_{k}", 'box_uv': True, 'rescale': False, 'locked': False, 'render_order': 'default', 'allow_mirror_modeling': True,
                 'from': [-(x0 + w), 24 - (y0 + h), z0], 'to': [-x0, 24 - y0, z0 + d], 'autouv': 0, 'color': k % 8,
                 'origin': origin, 'uv_offset': [u, v], 'inflate': b['inflate'], 'mirror_uv': b['mirror'],
                 'faces': face_uvs(u, v, w, h, d, b['mirror']), 'type': 'cube', 'uuid': uid()}
            elements.append(e); g['children'].append(e['uuid'])
        groups[p['name']] = g
        (groups[p['parent']]['children'] if p['parent'] else roots).append(g)
    texs = [tex_entry(f'{name}.png', f'{A}/textures/entity/{name}.png', tw, th, 0)]
    gp = f'{A}/textures/entity/{name}_glow.png'
    if os.path.exists(gp): texs.append(tex_entry(f'{name}_glow.png', gp, tw, th, 1))
    return {'meta': {'format_version': '4.10', 'model_format': 'modded_entity', 'box_uv': True},
            'name': name, 'model_identifier': name, 'modded_entity_entity_class': 'Entity', 'modded_entity_version': '1.17',
            'modded_entity_flip_y': True, 'visible_box': [4, 7, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
            'timeline_setups': [], 'unhandled_root_fields': {}, 'resolution': {'width': tw, 'height': th},
            'elements': elements, 'outliner': roots, 'textures': texs}

def check(name, bb):
    """re-derive la geometrie ModelPart depuis le .bbmodel (comme l'export Java de Blockbench) et compare"""
    geo = json.load(open(f'{A}/geo/{name}.json'))
    byid = {e['uuid']: e for e in bb['elements']}
    out = {}
    def walk(g, parent_origin):
        o = g['origin']
        piv = [-(o[0] - parent_origin[0]) if parent_origin else -o[0], -(o[1] - parent_origin[1]) if parent_origin else 24 - o[1], (o[2] - parent_origin[2]) if parent_origin else o[2]]
        piv = [piv[0], piv[1], piv[2]]
        boxes = []
        for c in g['children']:
            if isinstance(c, str):
                e = byid[c]; f, t = e['from'], e['to']
                boxes.append(([-(t[0] - o[0]), -(t[1] - o[1]), f[2] - o[2]], [t[i] - f[i] for i in range(3)], e['uv_offset']))
            else: walk(c, o)
        out[g['name']] = (piv, [math.radians(-g['rotation'][0]), math.radians(-g['rotation'][1]), math.radians(g['rotation'][2])], boxes)
    for g in bb['outliner']: walk(g, None)
    bad = 0
    for p in geo['parts']:
        piv, rot, boxes = out[p['name']]
        ok = all(abs(a - b) < 1e-6 for a, b in zip(piv, p['pivot'])) and all(abs(a - b) < 1e-6 for a, b in zip(rot, p['rot']))
        for b, (o, s, uv) in zip(p['boxes'], boxes):
            ok &= all(abs(a - c) < 1e-6 for a, c in zip(b['o'], o)) and list(b['s']) == [round(x) for x in s] and list(b['uv']) == uv
        bad += not ok
    return bad

if __name__ == '__main__':
    n = sys.argv[1] if len(sys.argv) > 1 else 'demon'
    path = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(HERE), 'build', f'{n}.bbmodel')
    bb = convert(n)
    print('pieces incoherentes :', check(n, bb))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    json.dump(bb, open(path, 'w'), separators=(',', ':'))
    print(path, len(bb['elements']), 'cubes')
