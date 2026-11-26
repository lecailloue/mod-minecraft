"""Ou trouver les runes. Elles ne se craftent pas : coffres de ruines et gros adversaires.

Chaque lot est une reserve (pool) a part, avec une entree vide tres lourde : une rune
reste un evenement. On relance le script sans risque, il remplace ses propres lots.
"""
import io
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'ringsofash', 'loot_table')
STATS = ['rune_damage_1', 'rune_speed_1', 'rune_reach_1', 'rune_solidity_1']
EFFETS = ['rune_fire', 'rune_wind', 'rune_poison']

# fichier -> (lot de statistiques, lot d'effets, amelioration, rune noire)
#            chaque valeur est le poids total de la chance sur 100, 0 = pas de lot
CHESTS = {
    'mine_ruins':    (10, 0, 0, 0),
    'mine_treasure': (18, 4, 5, 0),
    'orc_fort':      (12, 0, 0, 0),
    'barrow':        (14, 4, 0, 0),
    'dwarf_hall':    (16, 0, 3, 0),
    'hoard':         (20, 6, 6, 0),
    'dark_tower':    (20, 8, 6, 1),
}
MOBS = {
    'orc_chief':     (8, 0, 0, 0),
    'goblin_king':   (10, 2, 2, 0),
    'shadow_chief':  (10, 6, 3, 1),
    'shadow_dragon': (12, 10, 6, 2),
    'dragon':        (12, 10, 6, 0),
    'fire_demon':    (8, 6, 2, 0),
    'sorcerer':      (6, 4, 0, 0),
}


def pool(items, chance):
    """une reserve qui tire au plus un objet, avec `chance` % de ne pas sortir vide"""
    weight = max(1, int(round(chance)))
    entries = [{'type': 'minecraft:empty', 'weight': 100 - weight}]
    share = max(1, int(round(weight / float(len(items)))))
    for name in items:
        entries.append({'type': 'minecraft:item', 'name': 'ringsofash:' + name, 'weight': share})
    return {'rolls': 1, 'entries': entries}


def ours(p):
    """un de nos lots : rien que du vide et des runes"""
    names = [e.get('name', '') for e in p.get('entries', []) if e.get('type') == 'minecraft:item']
    return bool(names) and all(n.startswith('ringsofash:rune') or n == 'ringsofash:runic_upgrade' for n in names)


def patch(path, stats, effets, upgrade, black):
    with io.open(path, encoding='utf-8') as f:
        d = json.load(f)
    pools = [p for p in d.get('pools', []) if not ours(p)]
    if stats:
        pools.append(pool(STATS, stats))
    if effets:
        pools.append(pool(EFFETS, effets))
    if upgrade:
        pools.append(pool(['runic_upgrade'], upgrade))
    if black:
        pools.append(pool(['rune_black'], black))
    d['pools'] = pools
    with io.open(path, 'w', encoding='utf-8') as f:
        json.dump(d, f, ensure_ascii=False, separators=(',', ':'))
    return len(pools)


def main():
    for folder, table in (('chests', CHESTS), ('entities', MOBS)):
        for name, nums in table.items():
            path = os.path.join(ROOT, folder, name + '.json')
            if not os.path.exists(path):
                print('absent :', path)
                continue
            print(folder, name, '->', patch(path, *nums), 'lots')


if __name__ == '__main__':
    main()
