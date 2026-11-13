"""Generation du monde : dimension, bruit, biomes, features, structures."""
from genlib import *

W = f'{D}/worldgen'
def R(x): return f'{MOD}:{x}'

# ------------------------------------------------------------ features
def cf(name, typ, config): wjson(f'{W}/configured_feature/{name}.json', {'type': typ if ':' in typ else f'minecraft:{typ}', 'config': config})
def pf(name, feature, placement): wjson(f'{W}/placed_feature/{name}.json', {'feature': R(feature) if ':' not in feature else feature, 'placement': placement})
def st(name, **props):
    s = {'Name': mid(name) if ':' not in name else name}
    if props: s['Properties'] = {k: str(v) for k, v in props.items()}
    return s
def simple(state): return {'type': 'minecraft:simple_state_provider', 'state': state}
COUNT = lambda n: {'type': 'minecraft:count', 'count': n}
RARE = lambda n: {'type': 'minecraft:rarity_filter', 'chance': n}
SQ = {'type': 'minecraft:in_square'}
BIOME = {'type': 'minecraft:biome'}
HM = lambda h='MOTION_BLOCKING': {'type': 'minecraft:heightmap', 'heightmap': h}
NOWATER = {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0}
def yrange(lo, hi, kind='uniform'):
    return {'type': 'minecraft:height_range', 'height': {'type': f'minecraft:{kind}', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}}
def survive(sap): return {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': sap}}
OAK_SAP = st('minecraft:oak_sapling', stage=0)
TREE_PL = lambda *pre: list(pre) + [SQ, NOWATER, HM('OCEAN_FLOOR'), survive(OAK_SAP), BIOME]

def patch(name, block_state, tries=32, xz=6, y=2):
    cf(name, 'random_patch', {'tries': tries, 'xz_spread': xz, 'y_spread': y, 'feature': {
        'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': simple(block_state)}},
        'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
            {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'},
            {'type': 'minecraft:would_survive', 'state': block_state}]}}]}})

def tree(name, log, leaves_b, trunk, foliage, size=(1, 0, 1)):
    cf(name, 'tree', {'ignore_vines': True, 'force_dirt': False,
                      'minimum_size': {'type': 'minecraft:two_layers_feature_size', 'limit': size[0], 'lower_size': size[1], 'upper_size': size[2]},
                      'dirt_provider': simple(st('minecraft:dirt')), 'trunk_provider': simple(st(log, axis='y')),
                      'foliage_provider': simple(st(leaves_b, distance=7, persistent='false', waterlogged='false')),
                      'trunk_placer': trunk, 'foliage_placer': foliage, 'decorators': []})

def features():
    # volcan (monde normal)
    cf('volcanic_surface', R('volcanic_surface'), {})
    pf('volcanic_surface', 'volcanic_surface', [])
    cf('basalt_spire', R('spire'), {'block': st('minecraft:basalt', axis='y'), 'cap': st('volcanic_rock'), 'min_height': 4, 'max_height': 14, 'radius': 2})
    pf('basalt_spires', 'basalt_spire', [COUNT(2), SQ, HM('WORLD_SURFACE_WG'), BIOME])
    patch('ashroot_patch', st('ashroot'), 20, 5, 2)
    pf('ashroot_patch', 'ashroot_patch', [RARE(2), SQ, HM(), BIOME])
    patch('smoke_vent_patch', st('smoke_vent'), 5, 14, 3)
    pf('smoke_vents', 'smoke_vent_patch', [COUNT(1), SQ, HM(), BIOME])
    pf('smoke_vents_dense', 'smoke_vent_patch', [COUNT(3), SQ, HM(), BIOME])
    cf('crater_smoke', R('crater_smoke'), {})
    pf('crater_smoke', 'crater_smoke', [])
    # terres de cendre
    cf('ash_spire', R('spire'), {'block': st('volcanic_rock'), 'cap': st('minecraft:basalt', axis='y'), 'min_height': 5, 'max_height': 22, 'radius': 3})
    pf('ash_spires', 'ash_spire', [RARE(2), SQ, HM('WORLD_SURFACE_WG'), BIOME])
    cf('dead_tree', R('spire'), {'block': st('minecraft:dark_oak_log', axis='y'), 'cap': st('minecraft:dark_oak_log', axis='y'), 'min_height': 3, 'max_height': 7, 'radius': 0})
    pf('dead_trees', 'dead_tree', [RARE(2), SQ, HM('WORLD_SURFACE_WG'), BIOME])
    pf('dead_trees_marsh', 'dead_tree', [RARE(3), SQ, NOWATER, HM('OCEAN_FLOOR'), BIOME])
    cf('boulder', R('spire'), {'block': st('minecraft:mossy_cobblestone'), 'cap': st('minecraft:cobblestone'), 'min_height': 1, 'max_height': 3, 'radius': 2})
    pf('boulders', 'boulder', [RARE(3), SQ, HM('WORLD_SURFACE_WG'), BIOME])
    cf('standing_stone', R('spire'), {'block': st('minecraft:stone'), 'cap': st('minecraft:mossy_cobblestone'), 'min_height': 3, 'max_height': 6, 'radius': 0})
    pf('standing_stones', 'standing_stone', [RARE(6), SQ, HM('WORLD_SURFACE_WG'), BIOME])
    # arbres
    blob = {'type': 'minecraft:blob_foliage_placer', 'radius': 2, 'offset': 0, 'height': 3}
    tree('golden_tree_small', 'golden_log', 'golden_leaves', {'type': 'minecraft:straight_trunk_placer', 'base_height': 6, 'height_rand_a': 3, 'height_rand_b': 0}, blob)
    cf('golden_tree_big', R('big_tree'), {'log': st('golden_log', axis='y'), 'leaves': st('golden_leaves', distance=1, persistent='false', waterlogged='false'),
                                          'min_height': 22, 'max_height': 36, 'trunk': 1, 'canopy': 8, 'style': 'crown'})
    cf('ancient_tree_big', R('big_tree'), {'log': st('ancient_log', axis='y'), 'leaves': st('ancient_leaves', distance=1, persistent='false', waterlogged='false'),
                                           'min_height': 14, 'max_height': 26, 'trunk': 2, 'canopy': 9, 'style': 'gnarled'})
    tree('ancient_tree_small', 'ancient_log', 'ancient_leaves', {'type': 'minecraft:fancy_trunk_placer', 'base_height': 5, 'height_rand_a': 6, 'height_rand_b': 0},
         {'type': 'minecraft:fancy_foliage_placer', 'radius': 2, 'offset': 4, 'height': 4}, (0, 0, 0))
    pf('trees_golden_big', 'golden_tree_big', TREE_PL(COUNT(2)))
    pf('trees_golden_small', 'golden_tree_small', TREE_PL(COUNT(3)))
    pf('trees_golden_sparse', 'golden_tree_small', TREE_PL(RARE(6)))
    pf('trees_ancient_big', 'ancient_tree_big', TREE_PL(COUNT(2)))
    pf('trees_ancient_small', 'ancient_tree_small', TREE_PL(COUNT(5)))
    # arbres vanilla, legerement plus grands
    tree('oak_big', 'minecraft:oak_log', 'minecraft:oak_leaves', {'type': 'minecraft:straight_trunk_placer', 'base_height': 6, 'height_rand_a': 2, 'height_rand_b': 0},
         {'type': 'minecraft:blob_foliage_placer', 'radius': 3, 'offset': 0, 'height': 3})
    tree('birch_big', 'minecraft:birch_log', 'minecraft:birch_leaves', {'type': 'minecraft:straight_trunk_placer', 'base_height': 7, 'height_rand_a': 2, 'height_rand_b': 0},
         {'type': 'minecraft:blob_foliage_placer', 'radius': 2, 'offset': 0, 'height': 3})
    tree('spruce_big', 'minecraft:spruce_log', 'minecraft:spruce_leaves', {'type': 'minecraft:straight_trunk_placer', 'base_height': 8, 'height_rand_a': 3, 'height_rand_b': 1},
         {'type': 'minecraft:spruce_foliage_placer', 'radius': {'type': 'minecraft:uniform', 'min_inclusive': 3, 'max_inclusive': 4},
          'offset': {'type': 'minecraft:uniform', 'min_inclusive': 0, 'max_inclusive': 2},
          'trunk_height': {'type': 'minecraft:uniform', 'min_inclusive': 2, 'max_inclusive': 3}})
    for nm, lg, lv in (('oak', 'minecraft:oak_log', 'minecraft:oak_leaves'), ('birch', 'minecraft:birch_log', 'minecraft:birch_leaves'), ('spruce', 'minecraft:spruce_log', 'minecraft:spruce_leaves')):
        tree(f'bush_{nm}', lg, lv, {'type': 'minecraft:straight_trunk_placer', 'base_height': 1, 'height_rand_a': 0, 'height_rand_b': 0},
             {'type': 'minecraft:blob_foliage_placer', 'radius': 2, 'offset': 0, 'height': 2})
        pf(f'bushes_{nm}', f'bush_{nm}', TREE_PL(COUNT(2)))
        pf(f'bushes_{nm}_few', f'bush_{nm}', TREE_PL(RARE(2)))
    for nm, lg in (('oak', 'minecraft:oak_log'), ('birch', 'minecraft:birch_log'), ('spruce', 'minecraft:spruce_log')):
        cf(f'fallen_{nm}', R('fallen_log'), {'log': st(lg, axis='x'), 'min_length': 3, 'max_length': 6})
        pf(f'fallen_{nm}_logs', f'fallen_{nm}', [RARE(3), SQ, NOWATER, HM(), BIOME])
        pf(f'fallen_{nm}_logs_few', f'fallen_{nm}', [RARE(8), SQ, NOWATER, HM(), BIOME])
    pf('trees_oak_big', 'oak_big', TREE_PL(COUNT(4)))
    pf('trees_oak_sparse', 'oak_big', TREE_PL(RARE(2)))
    pf('trees_birch_big', 'birch_big', TREE_PL(COUNT(3)))
    pf('trees_birch_sparse', 'birch_big', TREE_PL(RARE(3)))
    pf('trees_spruce_big', 'spruce_big', TREE_PL(COUNT(3)))
    pf('trees_spruce_sparse', 'spruce_big', TREE_PL(RARE(2)))
    # fleurs
    patch('sunstar_patch', st('sunstar'), 40, 6, 2)
    pf('sunstar_patch', 'sunstar_patch', [RARE(2), SQ, HM(), BIOME])
    pf('sunstar_patch_rare', 'sunstar_patch', [RARE(12), SQ, HM(), BIOME])
    patch('moonbell_patch', st('moonbell'), 32, 5, 2)
    pf('moonbell_patch', 'moonbell_patch', [RARE(3), SQ, HM(), BIOME])
    patch('pipeweed_patch', st('pipeweed'), 24, 4, 2)
    pf('pipeweed_patch', 'pipeweed_patch', [RARE(10), SQ, HM(), BIOME])
    patch('cobweb_patch', st('minecraft:cobweb'), 12, 4, 3)
    pf('cobweb_patch', 'cobweb_patch', [RARE(3), SQ, HM(), BIOME])
    # minerais
    targets = [{'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}, 'state': st('lunargent_ore')},
               {'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}, 'state': st('lunargent_ore')}]
    cf('ore_lunargent', 'ore', {'size': 4, 'discard_chance_on_air_exposure': 0.3, 'targets': targets})
    cf('ore_lunargent_rich', 'ore', {'size': 8, 'discard_chance_on_air_exposure': 0.6, 'targets': targets})
    pf('ore_lunargent', 'ore_lunargent', [COUNT(5), SQ, yrange(-60, 24, 'trapezoid'), BIOME])
    pf('ore_lunargent_rich', 'ore_lunargent_rich', [COUNT(2), SQ, yrange(-62, -20), BIOME])
    for nm, sz, cnt, rng in (('sombrefer', 7, 6, yrange(-40, 90, 'trapezoid')), ('braisite', 5, 3, yrange(-62, 20)),
                             ('brumecristal', 5, 4, yrange(30, 220, 'trapezoid')), ('orrune', 6, 3, yrange(-62, 40, 'trapezoid'))):
        tg = [{'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}, 'state': st(f'{nm}_ore')},
              {'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}, 'state': st(f'{nm}_ore')}]
        cf(f'ore_{nm}', 'ore', {'size': sz, 'discard_chance_on_air_exposure': 0.2, 'targets': tg})
        pf(f'ore_{nm}', f'ore_{nm}', [COUNT(cnt), SQ, rng, BIOME])
    pf('ore_lunargent_scarce', 'ore_lunargent', [COUNT(1), SQ, yrange(-60, -10), BIOME])

# ------------------------------------------------------------ biomes
ORES = ['minecraft:ore_dirt', 'minecraft:ore_gravel', 'minecraft:ore_granite_upper', 'minecraft:ore_granite_lower', 'minecraft:ore_diorite_upper',
        'minecraft:ore_diorite_lower', 'minecraft:ore_andesite_upper', 'minecraft:ore_andesite_lower', 'minecraft:ore_tuff', 'minecraft:ore_coal_upper',
        'minecraft:ore_coal_lower', 'minecraft:ore_iron_upper', 'minecraft:ore_iron_middle', 'minecraft:ore_iron_small', 'minecraft:ore_gold',
        'minecraft:ore_gold_lower', 'minecraft:ore_redstone', 'minecraft:ore_redstone_lower', 'minecraft:ore_diamond', 'minecraft:ore_diamond_medium',
        'minecraft:ore_diamond_large', 'minecraft:ore_diamond_buried', 'minecraft:ore_lapis', 'minecraft:ore_lapis_buried', 'minecraft:ore_copper']
ORES_TAIL = ['ringsofash:ore_sombrefer', 'ringsofash:ore_braisite', 'ringsofash:ore_brumecristal', 'ringsofash:ore_orrune', 'minecraft:underwater_magma', 'minecraft:disk_sand', 'minecraft:disk_clay', 'minecraft:disk_gravel']
# ordre global de la vegetation (toujours respecter cet ordre relatif)
VEG_ORDER = ['minecraft:glow_lichen', 'ash_spires', 'dead_trees', 'dead_trees_marsh', 'boulders', 'standing_stones',
             'trees_golden_big', 'trees_golden_small', 'trees_golden_sparse', 'trees_ancient_big', 'trees_ancient_small',
             'trees_oak_big', 'trees_oak_sparse', 'trees_birch_big', 'trees_birch_sparse', 'trees_spruce_big', 'trees_spruce_sparse',
             'fallen_oak_logs', 'fallen_oak_logs_few', 'fallen_birch_logs', 'fallen_birch_logs_few', 'fallen_spruce_logs', 'fallen_spruce_logs_few',
             'bushes_oak', 'bushes_oak_few', 'bushes_birch', 'bushes_birch_few', 'bushes_spruce', 'bushes_spruce_few',
             'minecraft:dark_forest_vegetation', 'minecraft:trees_swamp', 'minecraft:trees_taiga', 'minecraft:trees_windswept_hills',
             'minecraft:trees_plains', 'minecraft:trees_savanna', 'minecraft:trees_birch_and_oak',
             'sunstar_patch', 'sunstar_patch_rare', 'moonbell_patch', 'pipeweed_patch', 'cobweb_patch', 'ashroot_patch', 'smoke_vents', 'smoke_vents_dense',
             'minecraft:flower_default', 'minecraft:flower_plains', 'minecraft:patch_tall_grass_2', 'minecraft:patch_tall_grass',
             'minecraft:patch_grass_plain', 'minecraft:patch_grass_forest', 'minecraft:patch_grass_savanna', 'minecraft:patch_large_fern',
             'minecraft:patch_dead_bush_2', 'minecraft:patch_waterlily', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal',
             'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin', 'minecraft:patch_berry_common',
             'minecraft:seagrass_normal', 'minecraft:seagrass_swamp', 'minecraft:seagrass_river', 'minecraft:kelp_cold']

def col(h): return int(h.lstrip('#'), 16)
def fid(x): return x if ':' in x else R(x)

BIOMES = {}
def biome(id, fr, temp, down, sky, fog, water='#3f76e4', wfog='#050533', grass=None, foliage=None, veg=(), ores=True, lunar=None,
          lava_surface=False, snow=False, spawns=None, particle=None, precip=True, carvers=True, extra_steps=None, loop=None, additions=None):
    vset = set(veg)
    unknown = vset - set(VEG_ORDER)
    assert not unknown, unknown
    steps = [[] for _ in range(11)]
    steps[1] = ['minecraft:lake_lava_underground'] + (['minecraft:lake_lava_surface'] if lava_surface else [])
    steps[2] = ['minecraft:amethyst_geode']
    if ores:
        steps[6] = [o for o in ORES if id == 'volcan' or o != 'minecraft:ore_dirt'] + ([R(lunar)] if lunar else []) + ORES_TAIL
    steps[8] = ['minecraft:spring_water', 'minecraft:spring_lava'] if id in ('volcan',) else []      # pas de cascades ni de coulees de lave
    steps[9] = [fid(v) for v in VEG_ORDER if v in vset or v == 'minecraft:glow_lichen']
    steps[10] = ['minecraft:freeze_top_layer']
    for k, v in (extra_steps or {}).items(): steps[k] = [f for f in v if id == 'volcan' or f != 'minecraft:ore_dirt']
    eff = {'fog_color': col(fog), 'sky_color': col(sky), 'water_color': col(water), 'water_fog_color': col(wfog),
           'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}}
    if loop: eff['ambient_sound'] = loop
    if additions: eff['additions_sound'] = {'sound': additions, 'tick_chance': 0.0111}
    if grass: eff['grass_color'] = col(grass)
    if foliage: eff['foliage_color'] = col(foliage)
    if particle: eff['particle'] = {'options': {'type': particle[0]}, 'probability': particle[1]}
    sp = {c: [] for c in ('monster', 'creature', 'ambient', 'axolotls', 'underground_water_creature', 'water_creature', 'water_ambient', 'misc')}
    for cat, lst in (spawns or {}).items():
        sp[cat] = [{'type': t if ':' in t else R(t), 'weight': w, 'minCount': a, 'maxCount': b} for (t, w, a, b) in lst]
    BIOMES[id] = {'has_precipitation': precip, 'temperature': temp, 'downfall': down, 'effects': eff, 'spawners': sp, 'spawn_costs': {},
                  'carvers': {'air': ['minecraft:cave', 'minecraft:cave_extra_underground', 'minecraft:canyon'] if carvers else []},
                  'features': steps}
    LANG[f'biome.{MOD}.{id}'] = fr

ORCS = [('orc', 40, 2, 4), ('warg', 18, 2, 3), ('pale_rider', 3, 1, 2), ('goblin', 30, 3, 5), ('cave_troll', 6, 1, 1), ('cave_thief', 3, 1, 1)]
FARM = [('minecraft:sheep', 12, 4, 4), ('minecraft:pig', 10, 4, 4), ('minecraft:chicken', 10, 4, 4), ('minecraft:cow', 8, 4, 4)]
BATS = [('minecraft:bat', 10, 8, 8)]
FISH = [('minecraft:cod', 15, 3, 6), ('minecraft:salmon', 10, 1, 5)]
SQUID = [('minecraft:squid', 4, 1, 4)]

def biomes():
    # monde normal
    biome('volcan', 'Terres volcaniques', 2.0, 0.0, '#7a2c14', '#3a1810', water='#5a4a44', wfog='#2a1c18', grass='#5b5a4c', foliage='#5b5a4c',
          veg=('ashroot_patch', 'smoke_vents_dense'), precip=False, particle=('minecraft:white_ash', 0.04),
          spawns={'monster': [('minecraft:magma_cube', 10, 1, 3)], 'ambient': BATS},
          extra_steps={1: ['minecraft:lake_lava_underground', 'minecraft:lake_lava_surface'], 2: ['minecraft:amethyst_geode', R('basalt_spires')],
                       10: ['minecraft:freeze_top_layer', R('volcanic_surface'), R('crater_smoke')]})
    # Hautemarche : trois plaines (herbe coloree), forets, montagnes, cimes, mer
    biome('plaines_vertes', 'Plaines Vertes', 0.7, 0.6, '#79b4ff', '#c4dcff', grass='#BEEB9E', foliage='#9bd36a',
          veg=('trees_oak_sparse', 'bushes_oak_few', 'fallen_oak_logs_few', 'sunstar_patch_rare', 'pipeweed_patch', 'minecraft:flower_plains', 'minecraft:patch_tall_grass_2',
               'minecraft:patch_grass_plain', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin', 'minecraft:patch_berry_common'),
          spawns={'creature': FARM + [('minecraft:rabbit', 6, 2, 3)], 'ambient': BATS, 'monster': [('orc', 20, 1, 3), ('warg', 8, 1, 2), ('pale_rider', 2, 1, 1), ('goblin', 30, 3, 5), ('cave_thief', 3, 1, 1)]})
    biome('plaines_jaunes', 'Plaines Pâles', 0.6, 0.35, '#86b8f5', '#d2e2f2', grass='#FFEB9E', foliage='#d9c85a',
          veg=('trees_birch_sparse', 'trees_oak_sparse', 'bushes_birch_few', 'bushes_oak_few', 'fallen_birch_logs_few', 'minecraft:flower_default', 'minecraft:patch_tall_grass', 'minecraft:patch_grass_plain'),
          spawns={'creature': [('minecraft:horse', 10, 2, 6), ('minecraft:cow', 8, 4, 4), ('minecraft:sheep', 6, 4, 4), ('stag', 2, 1, 2)], 'ambient': BATS, 'monster': ORCS + [('warg', 20, 2, 4)]})
    biome('plaines_cendre', 'Plaines de Cendre', 0.9, 0.1, '#7a2c14', '#3a1810', water='#4a4a44', wfog='#1c1c18', grass='#96938E', foliage='#7d7a6a',
          veg=('ashroot_patch', 'smoke_vents'), precip=False, particle=('minecraft:ash', 0.06), loop='minecraft:ambient.basalt_deltas.loop', additions='minecraft:ambient.basalt_deltas.additions',
          spawns={'ambient': BATS + [('crow', 10, 2, 4)], 'monster': [('orc', 60, 3, 5), ('war_orc', 35, 2, 3), ('warg', 25, 2, 4), ('cave_troll', 10, 1, 1), ('pale_rider', 5, 1, 2), ('goblin', 20, 3, 5)]})
    biome('grande_foret', 'Grande Forêt', 0.6, 0.9, '#6f9ed8', '#7f9a78', water='#3d6a55', wfog='#0c1f16', grass='#3f7a2e', foliage='#2f6a25',
          veg=('trees_oak_big', 'trees_birch_big', 'trees_spruce_big', 'fallen_oak_logs', 'fallen_spruce_logs', 'fallen_birch_logs_few', 'bushes_oak', 'bushes_spruce', 'moonbell_patch', 'minecraft:patch_grass_forest', 'minecraft:patch_large_fern',
               'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'),
          spawns={'creature': [('minecraft:fox', 4, 1, 2), ('minecraft:rabbit', 4, 2, 3), ('treeman', 5, 1, 1), ('stag', 6, 1, 3)], 'ambient': BATS,
                  'monster': ORCS + [('giant_spider', 10, 1, 1)]})
    biome('monts_rocheux', 'Monts Rocheux', 0.3, 0.4, '#7fa6e0', '#c0d0e0', grass='#7a9a64', foliage='#6a8a54', lunar='ore_lunargent',
          veg=('trees_spruce_sparse', 'fallen_spruce_logs_few', 'bushes_spruce_few', 'minecraft:patch_grass_plain', 'minecraft:patch_berry_common'),
          spawns={'creature': [('minecraft:goat', 10, 1, 3), ('minecraft:sheep', 4, 2, 3), ('eagle', 3, 1, 1)], 'ambient': BATS, 'monster': [('orc', 25, 2, 4), ('goblin', 60, 3, 6), ('cave_troll', 14, 1, 1), ('stone_troll', 12, 1, 1), ('warg', 10, 2, 3), ('cave_thief', 6, 1, 1), ('pale_rider', 2, 1, 1)]},
          extra_steps={6: ORES + [R('ore_lunargent'), R('ore_lunargent_rich')] + ORES_TAIL})
    biome('foret_clairsemee', 'Forêt Clairsemée', 0.65, 0.7, '#74a6e0', '#a8c4a0', water='#3d6a55', wfog='#0c1f16', grass='#8fc56f', foliage='#5f9a3f',
          veg=('trees_oak_sparse', 'trees_birch_sparse', 'trees_spruce_sparse', 'fallen_oak_logs', 'fallen_birch_logs_few', 'bushes_oak', 'bushes_birch_few', 'minecraft:patch_grass_forest', 'minecraft:flower_default'),
          spawns={'creature': [('minecraft:fox', 3, 1, 2), ('minecraft:rabbit', 5, 2, 3), ('stag', 5, 1, 3)], 'ambient': BATS, 'monster': ORCS})
    biome('bosquets', 'Bosquets', 0.65, 0.5, '#79b4ff', '#c4dcff', grass='#AEE08A', foliage='#82bf55',
          veg=('trees_oak_sparse', 'trees_birch_sparse', 'fallen_oak_logs_few', 'bushes_oak', 'bushes_birch', 'minecraft:flower_plains', 'minecraft:patch_tall_grass', 'minecraft:patch_grass_plain'),
          spawns={'creature': FARM + [('minecraft:rabbit', 6, 2, 3)], 'ambient': BATS, 'monster': ORCS})
    biome('monts_de_cendre', 'Monts de Cendre', 1.0, 0.0, '#6a2410', '#2e130c', water='#4a3a36', wfog='#1c100c', grass='#57503f', foliage='#57503f',
          veg=('smoke_vents_dense',), precip=False, particle=('minecraft:ash', 0.06), loop='minecraft:ambient.basalt_deltas.loop', additions='minecraft:ambient.basalt_deltas.additions',
          spawns={'ambient': BATS + [('crow', 10, 2, 4)], 'monster': [('orc', 40, 2, 4), ('war_orc', 25, 2, 3), ('warg', 20, 2, 3), ('cave_troll', 14, 1, 1), ('stone_troll', 10, 1, 1), ('goblin', 40, 3, 6), ('pale_rider', 4, 1, 2)]})
    biome('rivieres_de_lave', 'Rivières de Lave', 2.0, 0.0, '#6a2410', '#2e130c', water='#4a3a36', wfog='#1c100c', veg=('smoke_vents',), precip=False,
          particle=('minecraft:ash', 0.05), loop='minecraft:ambient.basalt_deltas.loop', additions='minecraft:ambient.basalt_deltas.additions',
          spawns={'monster': [('minecraft:magma_cube', 20, 1, 3)], 'ambient': BATS})
    biome('cimes_neigeuses', 'Cimes Neigeuses', -0.5, 0.6, '#9cc4ff', '#e6f0ff', grass='#8ab08a', foliage='#7aa07a', lunar='ore_lunargent_scarce',
          veg=('trees_spruce_sparse',),
          spawns={'creature': [('minecraft:goat', 10, 1, 3), ('eagle', 8, 1, 2)], 'ambient': BATS, 'monster': [('stone_troll', 20, 1, 1), ('warg', 20, 2, 3), ('goblin', 40, 3, 5), ('cave_troll', 8, 1, 1)]})
    biome('mer_grise', 'Mer Grise', 0.5, 0.5, '#7fa6d8', '#b8c8d8', water='#3a6a8a', wfog='#061a2a',
          veg=('minecraft:seagrass_normal', 'minecraft:kelp_cold'),
          spawns={'water_ambient': FISH, 'water_creature': SQUID, 'monster': [('lurker', 4, 1, 1)]})
    biome('rivage', 'Rivage', 0.7, 0.5, '#82b4f0', '#c8dcf0', veg=('minecraft:patch_sugar_cane',), spawns={'creature': [('minecraft:turtle', 3, 1, 2)]})
    biome('riviere', 'Rivière', 0.6, 0.6, '#82b4f0', '#c8dcf0', veg=('minecraft:patch_sugar_cane', 'minecraft:seagrass_river'),
          spawns={'water_ambient': [('minecraft:salmon', 6, 1, 5)], 'water_creature': SQUID, 'monster': [('lurker', 6, 1, 1), ('orc', 10, 1, 2)]})
    for id in ('plaines_cendre', 'monts_de_cendre', 'rivieres_de_lave'):      # fumee du cratere (la fonction ne fait rien loin du volcan)
        BIOMES[id]['features'][10].append(R('crater_smoke'))
    for id, b in BIOMES.items(): wjson(f'{W}/biome/{id}.json', b)

# ------------------------------------------------------------ source de biomes (carte predefinie)
BIOME_ORDER = ['plaines_vertes', 'plaines_jaunes', 'plaines_cendre', 'grande_foret', 'monts_rocheux', 'cimes_neigeuses', 'mer_grise', 'rivage', 'riviere', 'monts_de_cendre', 'foret_clairsemee', 'bosquets', 'rivieres_de_lave']

def biome_source():
    return {'type': R('map'), 'biomes': [R(b) for b in BIOME_ORDER]}

def structures():
    def structure(name, typ, biomes_list, extra, spacing, separation, salt, step='surface_structures', adapt='none'):
        tag('worldgen/biome', f'has_structure/{name}', *[R(b) for b in biomes_list], ns=MOD)
        o = {'type': R(typ), 'biomes': f'#{MOD}:has_structure/{name}', 'step': step, 'spawn_overrides': {}, 'terrain_adaptation': adapt}
        o.update(extra)
        wjson(f'{W}/structure/{name}.json', o)
        wjson(f'{W}/structure_set/{name}.json', {'structures': [{'structure': R(name), 'weight': 1}],
              'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': separation, 'salt': salt}})
    structure('volcano', 'volcano', ['volcan'], {'radius': 78, 'height': 104, 'doom': False}, 10, 5, 774411)
    structure('mont_funeste', 'volcano', [], {'radius': 96, 'height': 132, 'doom': True}, 36, 18, 774412)
    def settlement(name, biomes_list, spacing, separation, salt, tags_):
        structure(name, 'settlement', biomes_list, {'kind': name}, spacing, separation, salt)
        for t in tags_: tag('worldgen/structure', t, R(name), ns=MOD)
    settlement('halfling_village', [], 14, 7, 5101, ['villages_halfling', 'arrival_villages'])
    settlement('human_village', [], 17, 8, 5102, ['villages_human'])
    settlement('elf_village', [], 12, 6, 5103, ['villages_elf'])
    settlement('dwarf_mine', [], 20, 10, 5104, ['villages_dwarf', 'dwarf_mines'])
    settlement('orc_fort', [], 13, 6, 5105, ['orc_forts'])
    settlement('dark_tower', [], 24, 11, 5106, ['dark_towers'])
    settlement('barrow', [], 6, 3, 5107, ['barrows'])
    settlement('hoard', [], 38, 18, 5108, ['hoards'])
    settlement('pyre_tower', [], 19, 9, 5109, [])
    settlement('ruin', [], 9, 4, 5110, [])
    tag('worldgen/structure', 'volcanoes', R('volcano'), R('mont_funeste'), ns=MOD)
    tag('worldgen/structure', 'doom', R('mont_funeste'), ns=MOD)
    LANG['structure.ringsofash.volcano'] = 'Volcan'

def build():
    features()
    biomes()
    structures()
    wjson(f'{D}/dimension_type/hautemarche.json', {
        'ultrawarm': False, 'natural': True, 'coordinate_scale': 1.0, 'has_skylight': True, 'has_ceiling': False, 'ambient_light': 0.0,
        'monster_spawn_light_level': {'type': 'minecraft:uniform', 'min_inclusive': 0, 'max_inclusive': 7}, 'monster_spawn_block_light_limit': 0,
        'piglin_safe': False, 'bed_works': True, 'respawn_anchor_works': False, 'has_raids': False, 'logical_height': 512, 'min_y': -64,
        'height': 512, 'infiniburn': '#minecraft:infiniburn_overworld', 'effects': 'minecraft:overworld'})
    wjson(f'{D}/dimension/hautemarche.json', {'type': R('hautemarche'), 'generator': {'type': R('map'), 'biome_source': biome_source()}})
