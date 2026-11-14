"""Creatures : noms, butin, oeufs d'apparition, texture de l'araignee geante."""
from genlib import *
from PIL import Image

MOBS = [  # id, nom, constante, couleur1, couleur2
    ('orc', 'Orque', 0x5f7248, 0x2a2622), ('war_orc', 'Orque noir', 0x3a3a34, 0x8a1a14), ('orc_chief', 'Chef orque', 0x4a4034, 0xc02018),
    ('goblin', 'Gobelin', 0x7a8a52, 0xf0e040), ('goblin_king', 'Roi des gobelins', 0xc4a898, 0xd8a838), ('cave_troll', 'Troll des cavernes', 0x6d7868, 0x8a947e), ('stone_troll', 'Troll de pierre', 0x8a8a84, 0x5a5a56), ('troll_chief', 'Troll chef de meute', 0x2b2b30, 0xc02018),
    ('warg', 'Loup de guerre', 0x423c38, 0xc02010), ('wild_man', 'Homme sauvage', 0xa87a58, 0x2a1c12),
    ('mammoth', 'Mammouth', 0x6a5544, 0xe8dfc4), ('dead_soldier', 'Soldat spectral', 0x7ad8a0, 0xe0fff0), ('shadow_rider', 'Être des ombres', 0x0c0a0e, 0x4a4a54), ('shadow_chief', 'Chef des ombres', 0x08060a, 0xa8a8b8),
    ('giant_spider', 'Araignée géante', 0x1a1420, 0xa01828), ('lurker', 'Guetteur des profondeurs', 0x27332e, 0x9fe060), ('cave_thief', 'Créature blême', 0xb8b8a8, 0xd8f0ff),
    ('fire_demon', 'Fléau de Braise', 0x1f1717, 0xff7a18), ('ash_lord', 'Seigneur de Cendre', 0x141216, 0xff5a18), ('dragon', 'Vieux Ver', 0x7c211b, 0xc08a3a),
    ('sorcerer', 'Sorcier corrompu', 0xe8e4dc, 0x1c1c20), ('human', 'Villageois', 0xc99a78, 0x8a5a3a), ('elf', 'Elfe', 0xecd8c4, 0x3a6a4a),
    ('dwarf', 'Nain', 0xc89070, 0x8a3a1a), ('halfling', 'Semi-homme', 0xd8a888, 0x5a7a3a), ('ranger', 'Rôdeur', 0x2f3d2a, 0xc09070),
    ('mage', 'Mage errant', 0x7a7a80, 0xe0e0e0), ('treeman', 'Sylvegarde', 0x5c4832, 0x3f7a34), ('eagle', 'Aigle géant', 0x6b4a2c, 0xefe9dc),
    ('stag', 'Cerf blanc', 0xf2efe6, 0xc9b48a), ('crow', 'Corbeau espion', 0x1a181e, 0x3c3832),
]

def entry(name, lo=1, hi=1, chance=None):
    e = {'type': 'minecraft:item', 'name': mid(name) if ':' not in name else name}
    if (lo, hi) != (1, 1):
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    pool = {'rolls': 1, 'entries': [e]}
    if chance is not None: pool['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
    return pool

def loot(id, *pools):
    wjson(f'{D}/loot_table/entities/{id}.json', {'type': 'minecraft:entity', 'pools': list(pools)})

def chest(name, rolls, entries):
    """entries : (item, poids, min, max)"""
    es = []
    for (it, w, lo, hi) in entries:
        e = {'type': 'minecraft:item', 'name': mid(it) if ':' not in it else it, 'weight': w}
        if (lo, hi) != (1, 1): e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
        es.append(e)
    wjson(f'{D}/loot_table/chests/{name}.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'min': rolls[0], 'max': rolls[1]}, 'entries': es}]})

def build():
    for id, fr, c1, c2 in MOBS:
        LANG[f'entity.{MOD}.{id}'] = fr
        item(f'{id}_spawn_egg', f"Œuf d'apparition : {fr}", java=f'new DeferredSpawnEggItem(ModEntities.{id.upper()}, 0x{c1:06x}, 0x{c2:06x}, props())', model='egg')
    loot('orc', entry('minecraft:rotten_flesh', 0, 2), entry('black_steel_ingot', 1, 1, 0.5), entry('silver_coin', 1, 2, 0.3))
    loot('war_orc', entry('black_steel_ingot', 1, 2), entry('silver_coin', 0, 3))
    loot('orc_chief', entry('black_steel_ingot', 3, 5), entry('silver_coin', 6, 12), entry('rune_key'), entry('ring_kings', 1, 1, 0.25))
    loot('goblin', entry('silver_coin', 1, 1, 0.4), entry('minecraft:bone', 0, 1), entry('minecraft:raw_iron', 1, 2, 0.1))
    loot('goblin_king', entry('silver_coin', 6, 12), entry('minecraft:raw_iron', 3, 6), entry('minecraft:bone', 2, 5), entry('ring_kings', 1, 1, 0.12))
    for t in ('cave_troll', 'stone_troll'):
        loot(t, entry('troll_hide', 1, 3), entry('minecraft:bone', 1, 3), entry('silver_coin', 0, 4), entry('troll_club', 1, 1, 0.06))
    loot('troll_chief', entry('troll_hide', 3, 5), entry('minecraft:bone', 2, 4), entry('silver_coin', 3, 8), entry('black_steel_ingot', 0, 2), entry('troll_club', 1, 1, 0.2))
    loot('warg', entry('warg_pelt', 0, 2), entry('minecraft:bone', 0, 1))
    loot('wild_man', entry('minecraft:leather', 0, 1), entry('silver_coin', 0, 1))
    loot('mammoth', entry('minecraft:beef', 3, 6), entry('minecraft:leather', 2, 4), entry('minecraft:bone', 2, 4))
    loot('shadow_rider', entry('wight_dust', 2, 4), entry('ring_shadows', 1, 1, 0.08), entry('ring_kings', 1, 1, 0.05))
    loot('shadow_chief', entry('wight_dust', 4, 8), entry('wight_blade', 1, 1, 0.3), entry('ring_shadows', 1, 1, 0.3), entry('ring_kings', 1, 1, 0.15))
    loot('giant_spider', entry('spider_silk', 1, 3), entry('minecraft:string', 2, 5), entry('minecraft:spider_eye', 1, 2))
    loot('lurker', entry('minecraft:ink_sac', 3, 6), entry('minecraft:prismarine_shard', 2, 5), entry('minecraft:heart_of_the_sea', 1, 1, 0.15), entry('ring_waters', 1, 1, 0.2))
    loot('cave_thief', entry('silver_coin', 1, 3))
    loot('fire_demon', entry('ash_ring'), entry('demon_heart'), entry('minecraft:blaze_rod', 4, 8), entry('lunargent_ingot', 2, 5))
    loot('dragon', entry('dragon_scale', 6, 10), entry('ring_hoard'), entry('minecraft:gold_block', 4, 8))
    loot('sorcerer', entry('staff_storm'), entry('seeing_stone', 1, 1, 0.5), entry('silver_coin', 8, 16))
    loot('treeman', entry('ancient_log', 4, 8), entry('living_sap', 1, 2))
    loot('eagle', entry('minecraft:feather', 2, 5))
    loot('stag', entry('minecraft:leather', 1, 2), entry('minecraft:mutton', 1, 2))
    loot('crow', entry('minecraft:feather', 0, 1))
    # coffres
    chest('halfling_house', (3, 6), [('minecraft:bread', 10, 1, 4), ('honey_cake', 8, 1, 3), ('halfling_stew', 6, 1, 2), ('pipeweed', 6, 1, 4), ('minecraft:wheat_seeds', 6, 2, 6),
                                     ('silver_coin', 6, 1, 3), ('minecraft:carrot', 8, 2, 6), ('dwarf_ale', 4, 1, 2), ('halfling_dagger', 1, 1, 1)])
    chest('human_house', (3, 6), [('minecraft:bread', 10, 1, 4), ('silver_coin', 10, 1, 5), ('minecraft:iron_ingot', 6, 1, 3), ('minecraft:arrow', 6, 4, 12), ('minecraft:leather', 6, 1, 3),
                                  ('ranger_sword', 2, 1, 1), ('rider_spear', 2, 1, 1), ('minecraft:saddle', 2, 1, 1), ('minecraft:wheat', 8, 3, 8)])
    chest('elf_house', (3, 6), [('waybread', 10, 1, 3), ('sunstar', 8, 1, 4), ('moonbell', 8, 1, 4), ('golden_sapling', 5, 1, 2), ('minecraft:honey_bottle', 5, 1, 2),
                                ('elven_rope', 5, 1, 3), ('light_phial', 2, 1, 1), ('elven_bow', 2, 1, 1), ('minecraft:arrow', 8, 6, 16)])
    chest('dwarf_hall', (4, 7), [('minecraft:iron_ingot', 10, 2, 6), ('minecraft:gold_ingot', 8, 1, 4), ('minecraft:coal', 10, 4, 10), ('dwarf_ale', 6, 1, 3), ('silver_coin', 8, 2, 6),
                                 ('raw_lunargent', 3, 1, 2), ('minecraft:diamond', 3, 1, 2), ('dwarven_axe', 2, 1, 1), ('dwarven_lamp', 4, 1, 3)])
    chest('mine_ruins', (3, 6), [('minecraft:raw_iron', 10, 2, 6), ('minecraft:raw_gold', 8, 1, 4), ('raw_lunargent', 5, 1, 3), ('minecraft:diamond', 4, 1, 2), ('silver_coin', 8, 2, 8),
                                 ('minecraft:bone', 8, 1, 4), ('dwarven_hammer', 2, 1, 1), ('lunargent_ingot', 3, 1, 2), ('rune_key', 2, 1, 1), ('minecraft:torch', 8, 4, 12)])
    chest('mine_treasure', (6, 9), [('lunargent_ingot', 10, 2, 5), ('minecraft:diamond', 8, 2, 4), ('minecraft:gold_ingot', 10, 3, 8), ('silver_coin', 10, 5, 14), ('ring_forge', 2, 1, 1),
                                    ('dwarven_helmet', 3, 1, 1), ('dwarven_chestplate', 2, 1, 1), ('minecraft:emerald', 6, 2, 5), ('lunargent_pickaxe', 2, 1, 1)])
    chest('orc_fort', (4, 7), [('black_steel_ingot', 10, 2, 5), ('silver_coin', 10, 3, 9), ('minecraft:rotten_flesh', 8, 2, 6), ('orc_scimitar', 4, 1, 1), ('orc_cleaver', 3, 1, 1), ('orc_machete', 3, 1, 1), ('orc_spear', 2, 1, 1), ('orc_mace', 1, 1, 1),
                               ('orc_helmet', 3, 1, 1), ('orc_chestplate', 2, 1, 1), ('minecraft:gunpowder', 6, 1, 4), ('war_horn', 1, 1, 1), ('minecraft:iron_ingot', 6, 1, 4)])
    chest('barrow', (3, 5), [('wight_blade', 6, 1, 1), ('silver_coin', 10, 3, 10), ('minecraft:gold_ingot', 8, 1, 5), ('ring_kings', 2, 1, 1), ('ring_shadows', 2, 1, 1),
                             ('wight_dust', 8, 1, 3), ('minecraft:bone', 8, 1, 4), ('minecraft:emerald', 5, 1, 3), ('dead_horn', 1, 1, 1)])
    chest('dark_tower', (4, 7), [('seeing_stone', 3, 1, 1), ('silver_coin', 10, 4, 12), ('minecraft:book', 8, 1, 4), ('minecraft:ender_pearl', 5, 1, 2), ('staff_light', 2, 1, 1),
                                 ('minecraft:experience_bottle', 6, 1, 4), ('black_steel_ingot', 8, 1, 4), ('minecraft:lapis_lazuli', 6, 2, 6)])
    chest('hoard', (6, 9), [('minecraft:gold_ingot', 12, 4, 10), ('minecraft:gold_block', 6, 1, 3), ('minecraft:diamond', 8, 2, 5), ('minecraft:emerald', 8, 2, 6), ('silver_coin', 10, 8, 20),
                            ('lunargent_ingot', 6, 2, 5), ('ring_hoard', 1, 1, 1), ('kings_blade', 0 + 1, 1, 1)])
