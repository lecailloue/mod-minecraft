"""Generation du monde : dimension, bruit, biomes, features, structures."""
from genlib import *

W = f'{D}/worldgen'
def R(x): return f'{MOD}:{x}'

# ------------------------------------------------------------ fonctions de densite
def add(a, b): return {'type': 'minecraft:add', 'argument1': a, 'argument2': b}
def mul(a, b): return {'type': 'minecraft:mul', 'argument1': a, 'argument2': b}
def dmin(a, b): return {'type': 'minecraft:min', 'argument1': a, 'argument2': b}
def dmax(a, b): return {'type': 'minecraft:max', 'argument1': a, 'argument2': b}
def noise(n, xz=1.0, y=1.0): return {'type': 'minecraft:noise', 'noise': n, 'xz_scale': xz, 'y_scale': y}
def grad(fy, ty, fv, tv): return {'type': 'minecraft:y_clamped_gradient', 'from_y': fy, 'to_y': ty, 'from_value': fv, 'to_value': tv}
def clamp(i, lo, hi): return {'type': 'minecraft:clamp', 'input': i, 'min': lo, 'max': hi}
def rchoice(i, lo, hi, a, b): return {'type': 'minecraft:range_choice', 'input': i, 'min_inclusive': lo, 'max_exclusive': hi, 'when_in_range': a, 'when_out_of_range': b}
def un(t, a): return {'type': f'minecraft:{t}', 'argument': a}
def shifted(n, scale): return {'type': 'minecraft:shifted_noise', 'noise': n, 'shift_x': 'minecraft:shift_x', 'shift_y': 0.0,
                               'shift_z': 'minecraft:shift_z', 'xz_scale': scale, 'y_scale': 0.0}

def slides(inner):
    return add(0.1171875, mul(grad(-64, -40, 0.0, 1.0), add(-0.1171875, add(-0.078125, mul(grad(240, 256, 1.0, 0.0), add(0.078125, inner))))))

def noise_router():
    sc = 'minecraft:overworld/sloped_cheese'
    ent = 'minecraft:overworld/caves/entrances'
    underground = dmax(
        dmin(dmin(add(mul(4.0, un('square', noise('minecraft:cave_layer', 1.0, 8.0))),
                      add(clamp(add(0.27, noise('minecraft:cave_cheese', 1.0, 0.6666666666666666)), -1.0, 1.0),
                          clamp(add(1.5, mul(-0.64, sc)), 0.0, 0.5))), ent),
             add('minecraft:overworld/caves/spaghetti_2d', 'minecraft:overworld/caves/spaghetti_roughness_function')),
        rchoice('minecraft:overworld/caves/pillars', -1000000.0, 0.03, -1000000.0, 'minecraft:overworld/caves/pillars'))
    final = dmin(un('squeeze', mul(0.64, un('interpolated', un('blend_density', slides(
        rchoice(sc, -1000000.0, 1.5625, dmin(sc, mul(5.0, ent)), underground)))))), 'minecraft:overworld/caves/noodle')
    initial = slides(clamp(add(-0.703125, mul(4.0, un('quarter_negative', mul('minecraft:overworld/depth', un('cache_2d', 'minecraft:overworld/factor'))))), -64.0, 64.0))
    return {
        'barrier': noise('minecraft:aquifer_barrier', 1.0, 0.5),
        'fluid_level_floodedness': noise('minecraft:aquifer_fluid_level_floodedness', 1.0, 0.67),
        'fluid_level_spread': noise('minecraft:aquifer_fluid_level_spread', 1.0, 0.7142857142857143),
        'lava': noise('minecraft:aquifer_lava', 1.0, 1.0),
        'temperature': shifted('minecraft:temperature', 0.45),
        'vegetation': shifted('minecraft:vegetation', 0.45),
        'continents': 'minecraft:overworld/continents',
        'erosion': 'minecraft:overworld/erosion',
        'depth': 'minecraft:overworld/depth',
        'ridges': 'minecraft:overworld/ridges',
        'initial_density_without_jaggedness': initial,
        'final_density': final,
        'vein_toggle': 0.0, 'vein_ridged': 0.0, 'vein_gap': 0.0,
    }

# ------------------------------------------------------------ regles de surface
def seq(*r): return {'type': 'minecraft:sequence', 'sequence': list(r)}
def cond(c, r): return {'type': 'minecraft:condition', 'if_true': c, 'then_run': r}
def blk(name, **props):
    st = {'Name': mid(name) if ':' not in name else name}
    if props: st['Properties'] = {k: str(v) for k, v in props.items()}
    return {'type': 'minecraft:block', 'result_state': st}
def biome_is(*b): return {'type': 'minecraft:biome', 'biome_is': [R(x) for x in b]}
def sdepth(offset=0, add_depth=False, secondary=0, kind='floor'):
    return {'type': 'minecraft:stone_depth', 'offset': offset, 'surface_type': kind, 'add_surface_depth': add_depth, 'secondary_depth_range': secondary}
def water(offset, mult=0, add_stone=False): return {'type': 'minecraft:water', 'offset': offset, 'surface_depth_multiplier': mult, 'add_stone_depth': add_stone}
def y_above(y, mult=0): return {'type': 'minecraft:y_above', 'anchor': {'absolute': y}, 'surface_depth_multiplier': mult, 'add_stone_depth': False}
def nthr(n, lo, hi): return {'type': 'minecraft:noise_threshold', 'noise': n, 'min_threshold': lo, 'max_threshold': hi}
def cnot(c): return {'type': 'minecraft:not', 'invert': c}
STEEP = {'type': 'minecraft:steep'}

def surface_rule():
    grass_or_dirt = seq(cond(water(0), blk('minecraft:grass_block')), blk('minecraft:dirt'))
    top = seq(
        cond(biome_is('terres_de_cendre'), seq(
            cond(nthr('minecraft:surface', 0.25, 9.0), blk('volcanic_rock')),
            cond(nthr('minecraft:surface', -9.0, -0.35), blk('minecraft:basalt', axis='y')),
            blk('ash_block'))),
        cond(biome_is('pics_blancs'), seq(
            cond(STEEP, blk('white_stone')),
            cond(y_above(118, 1), blk('minecraft:snow_block')),
            cond(nthr('minecraft:surface', 0.1, 9.0), blk('white_stone')),
            grass_or_dirt)),
        cond(biome_is('monts_de_fer'), seq(
            cond(STEEP, blk('minecraft:stone')),
            cond(y_above(150, 1), blk('minecraft:snow_block')),
            cond(y_above(112, 1), seq(cond(nthr('minecraft:surface', -0.2, 0.3), blk('minecraft:gravel')), blk('minecraft:stone'))),
            cond(nthr('minecraft:surface', 0.35, 9.0), blk('minecraft:coarse_dirt')),
            grass_or_dirt)),
        cond(biome_is('marais_des_morts'), seq(
            cond(y_above(62), cond(cnot(y_above(63)), cond(nthr('minecraft:surface_swamp', 0.0, 9.0), blk('minecraft:water', level=0)))),
            cond(cnot(y_above(64)), cond(nthr('minecraft:surface', -0.1, 9.0), blk('minecraft:mud'))),
            grass_or_dirt)),
        cond(biome_is('rivage'), blk('minecraft:sand')),
        cond(biome_is('landes_des_tertres'), seq(
            cond(nthr('minecraft:surface', 0.45, 9.0), blk('minecraft:coarse_dirt')),
            cond(nthr('minecraft:surface', -9.0, -0.5), blk('minecraft:podzol')), grass_or_dirt)),
        cond(biome_is('vieille_foret', 'bois_noir'), seq(
            cond(nthr('minecraft:surface', 0.3, 9.0), blk('minecraft:podzol')), grass_or_dirt)),
        cond(biome_is('mer_grise', 'riviere'), seq(
            cond(water(-1), grass_or_dirt),
            cond(nthr('minecraft:surface', -0.1, 0.25), blk('minecraft:gravel')), blk('minecraft:sand'))),
        cond(water(-1), grass_or_dirt),
        blk('minecraft:sand'))
    under = seq(
        cond(biome_is('terres_de_cendre'), blk('volcanic_rock')),
        cond(biome_is('pics_blancs'), seq(cond(y_above(118, 1), blk('white_stone')), blk('minecraft:stone'))),
        cond(biome_is('monts_de_fer'), blk('minecraft:stone')),
        cond(biome_is('rivage', 'mer_grise', 'riviere'), seq(cond(water(-1), blk('minecraft:dirt')), blk('minecraft:sand'))),
        blk('minecraft:dirt'))
    deep_white = cond(biome_is('pics_blancs'), cond(y_above(90), cond(sdepth(0, True, 6), blk('white_stone'))))
    return seq(
        cond({'type': 'minecraft:vertical_gradient', 'random_name': 'minecraft:bedrock_floor',
              'true_at_and_below': {'above_bottom': 0}, 'false_at_and_above': {'above_bottom': 5}}, blk('minecraft:bedrock')),
        cond({'type': 'minecraft:above_preliminary_surface'}, seq(
            cond(sdepth(0, False, 0), top),
            cond(sdepth(0, True, 0), under),
            deep_white)),
        cond({'type': 'minecraft:vertical_gradient', 'random_name': 'minecraft:deepslate',
              'true_at_and_below': {'absolute': 0}, 'false_at_and_above': {'absolute': 8}}, blk('minecraft:deepslate', axis='y')))

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
    pf('ore_lunargent_scarce', 'ore_lunargent', [COUNT(1), SQ, yrange(-60, -10), BIOME])

# ------------------------------------------------------------ biomes
ORES = ['minecraft:ore_dirt', 'minecraft:ore_gravel', 'minecraft:ore_granite_upper', 'minecraft:ore_granite_lower', 'minecraft:ore_diorite_upper',
        'minecraft:ore_diorite_lower', 'minecraft:ore_andesite_upper', 'minecraft:ore_andesite_lower', 'minecraft:ore_tuff', 'minecraft:ore_coal_upper',
        'minecraft:ore_coal_lower', 'minecraft:ore_iron_upper', 'minecraft:ore_iron_middle', 'minecraft:ore_iron_small', 'minecraft:ore_gold',
        'minecraft:ore_gold_lower', 'minecraft:ore_redstone', 'minecraft:ore_redstone_lower', 'minecraft:ore_diamond', 'minecraft:ore_diamond_medium',
        'minecraft:ore_diamond_large', 'minecraft:ore_diamond_buried', 'minecraft:ore_lapis', 'minecraft:ore_lapis_buried', 'minecraft:ore_copper']
ORES_TAIL = ['minecraft:underwater_magma', 'minecraft:disk_sand', 'minecraft:disk_clay', 'minecraft:disk_gravel']
# ordre global de la vegetation (toujours respecter cet ordre relatif)
VEG_ORDER = ['minecraft:glow_lichen', 'ash_spires', 'dead_trees', 'dead_trees_marsh', 'boulders', 'standing_stones',
             'trees_golden_big', 'trees_golden_small', 'trees_golden_sparse', 'trees_ancient_big', 'trees_ancient_small',
             'minecraft:dark_forest_vegetation', 'minecraft:trees_swamp', 'minecraft:trees_taiga', 'minecraft:trees_windswept_hills',
             'minecraft:trees_plains', 'minecraft:trees_savanna', 'minecraft:trees_birch_and_oak',
             'sunstar_patch', 'sunstar_patch_rare', 'moonbell_patch', 'pipeweed_patch', 'cobweb_patch', 'ashroot_patch',
             'minecraft:flower_default', 'minecraft:flower_plains', 'minecraft:patch_tall_grass_2', 'minecraft:patch_tall_grass',
             'minecraft:patch_grass_plain', 'minecraft:patch_grass_forest', 'minecraft:patch_grass_savanna', 'minecraft:patch_large_fern',
             'minecraft:patch_dead_bush_2', 'minecraft:patch_waterlily', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal',
             'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin', 'minecraft:patch_berry_common',
             'minecraft:seagrass_normal', 'minecraft:seagrass_swamp', 'minecraft:seagrass_river', 'minecraft:kelp_cold']

def col(h): return int(h.lstrip('#'), 16)
def fid(x): return x if ':' in x else R(x)

BIOMES = {}
def biome(id, fr, temp, down, sky, fog, water='#3f76e4', wfog='#050533', grass=None, foliage=None, veg=(), ores=True, lunar=None,
          lava_surface=False, snow=False, spawns=None, particle=None, precip=True, carvers=True, extra_steps=None):
    vset = set(veg)
    unknown = vset - set(VEG_ORDER)
    assert not unknown, unknown
    steps = [[] for _ in range(11)]
    steps[1] = ['minecraft:lake_lava_underground'] + (['minecraft:lake_lava_surface'] if lava_surface else [])
    steps[2] = ['minecraft:amethyst_geode']
    if ores:
        steps[6] = ORES + ([R(lunar)] if lunar else []) + ORES_TAIL
    steps[8] = ['minecraft:spring_water', 'minecraft:spring_lava']
    steps[9] = [fid(v) for v in VEG_ORDER if v in vset or v == 'minecraft:glow_lichen']
    steps[10] = ['minecraft:freeze_top_layer']
    for k, v in (extra_steps or {}).items(): steps[k] = v
    eff = {'fog_color': col(fog), 'sky_color': col(sky), 'water_color': col(water), 'water_fog_color': col(wfog),
           'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}}
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
    biome('volcan', 'Terres volcaniques', 2.0, 0.0, '#6b5a55', '#4a3a36', water='#5a4a44', wfog='#2a1c18', grass='#5b5a4c', foliage='#5b5a4c',
          veg=('ashroot_patch',), precip=False, particle=('minecraft:white_ash', 0.03),
          spawns={'monster': [('minecraft:magma_cube', 10, 1, 3)], 'ambient': BATS},
          extra_steps={1: ['minecraft:lake_lava_underground', 'minecraft:lake_lava_surface'], 2: ['minecraft:amethyst_geode', R('basalt_spires')],
                       10: ['minecraft:freeze_top_layer', R('volcanic_surface')]})
    # Hautemarche
    biome('vertes_collines', 'Vertes-Collines', 0.7, 0.6, '#79b4ff', '#c4dcff', grass='#7cc24a', foliage='#6fb53c',
          veg=('minecraft:trees_plains', 'sunstar_patch_rare', 'pipeweed_patch', 'minecraft:flower_plains', 'minecraft:patch_tall_grass_2',
               'minecraft:patch_grass_plain', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin', 'minecraft:patch_berry_common'),
          spawns={'creature': FARM + [('minecraft:rabbit', 6, 2, 3)], 'ambient': BATS, 'monster': [('orc', 20, 1, 3), ('warg', 8, 1, 2), ('pale_rider', 2, 1, 1), ('goblin', 30, 3, 5), ('cave_thief', 3, 1, 1)]})
    biome('plaines_du_vent', 'Plaines du Vent', 0.6, 0.35, '#86b8f5', '#d2e2f2', grass='#b3b45a', foliage='#9aa64a',
          veg=('boulders', 'trees_golden_sparse', 'minecraft:flower_default', 'minecraft:patch_tall_grass', 'minecraft:patch_grass_plain'),
          spawns={'creature': [('minecraft:horse', 10, 2, 6), ('minecraft:cow', 8, 4, 4), ('minecraft:sheep', 6, 4, 4), ('stag', 2, 1, 2)], 'ambient': BATS, 'monster': ORCS + [('warg', 20, 2, 4)]})
    biome('foret_doree', 'Forêt Dorée', 0.7, 0.8, '#8fc0ff', '#f1e6b8', water='#4f9ad6', grass='#a3c955', foliage='#d8b83a',
          veg=('trees_golden_big', 'trees_golden_small', 'sunstar_patch', 'moonbell_patch', 'minecraft:flower_default', 'minecraft:patch_grass_forest'),
          spawns={'creature': [('minecraft:rabbit', 6, 2, 3), ('minecraft:fox', 3, 1, 2), ('stag', 6, 1, 3)], 'ambient': BATS, 'monster': [('orc', 10, 1, 2), ('pale_rider', 2, 1, 1), ('goblin', 20, 2, 4)]})
    biome('vieille_foret', 'Vieille-Forêt', 0.6, 0.9, '#6f9ed8', '#7f9a78', water='#3d6a55', wfog='#0c1f16', grass='#4f8a3a', foliage='#3d7a2e',
          veg=('boulders', 'trees_ancient_big', 'trees_ancient_small', 'moonbell_patch', 'minecraft:patch_grass_forest', 'minecraft:patch_large_fern',
               'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'),
          spawns={'creature': [('minecraft:fox', 4, 1, 2), ('minecraft:rabbit', 4, 2, 3), ('treeman', 5, 1, 1), ('stag', 4, 1, 2)], 'ambient': BATS, 'monster': ORCS + [('giant_spider', 6, 1, 1)]})
    biome('bois_noir', 'Bois-Noir', 0.5, 0.8, '#4a5a70', '#2a3238', water='#2e4a48', wfog='#081210', grass='#3f5c32', foliage='#34502a',
          veg=('minecraft:dark_forest_vegetation', 'cobweb_patch', 'minecraft:patch_grass_forest', 'minecraft:patch_large_fern',
               'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'),
          spawns={'ambient': BATS + [('crow', 8, 1, 3)], 'monster': [('giant_spider', 50, 1, 2), ('orc', 15, 1, 3), ('goblin', 20, 2, 4), ('pale_rider', 2, 1, 1)]})
    biome('landes_des_tertres', 'Landes des Tertres', 0.35, 0.5, '#8a96a6', '#9aa2a8', water='#4a6a7a', grass='#7d8a5e', foliage='#6f7c50',
          veg=('boulders', 'standing_stones', 'minecraft:patch_tall_grass', 'minecraft:patch_grass_plain', 'minecraft:patch_dead_bush_2'),
          spawns={'creature': [('minecraft:sheep', 8, 2, 4), ('minecraft:rabbit', 6, 2, 3)], 'ambient': BATS + [('crow', 8, 1, 3)], 'monster': [('wight', 45, 1, 2), ('warg', 15, 2, 3), ('stone_troll', 12, 1, 1), ('pale_rider', 6, 1, 2), ('goblin', 20, 2, 4)]})
    biome('marais_des_morts', 'Marais des Morts', 0.6, 0.9, '#6c7a70', '#55604e', water='#4c5a3a', wfog='#1a2212', grass='#5c6a3c', foliage='#55632f',
          veg=('dead_trees_marsh', 'minecraft:trees_swamp', 'minecraft:patch_grass_plain', 'minecraft:patch_dead_bush_2', 'minecraft:patch_waterlily',
               'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane', 'minecraft:seagrass_swamp'),
          spawns={'creature': [('minecraft:frog', 10, 2, 5)], 'ambient': BATS + [('crow', 6, 1, 2)], 'monster': [('wight', 30, 1, 2), ('orc', 20, 2, 3), ('lurker', 12, 1, 1), ('pale_rider', 3, 1, 1), ('goblin', 20, 2, 4)]})
    biome('monts_de_fer', 'Monts de Fer', 0.3, 0.4, '#7fa6e0', '#c0d0e0', grass='#6f9a5a', foliage='#5f8a4c', lunar='ore_lunargent',
          veg=('boulders', 'minecraft:trees_taiga', 'minecraft:patch_grass_plain', 'minecraft:patch_large_fern', 'minecraft:patch_berry_common'),
          spawns={'creature': [('minecraft:goat', 10, 1, 3), ('minecraft:sheep', 4, 2, 3), ('eagle', 3, 1, 1)], 'ambient': BATS, 'monster': [('orc', 25, 2, 4), ('goblin', 60, 3, 6), ('cave_troll', 14, 1, 1), ('stone_troll', 12, 1, 1), ('warg', 10, 2, 3), ('cave_thief', 6, 1, 1), ('pale_rider', 2, 1, 1)]},
          extra_steps={6: ORES + [R('ore_lunargent'), R('ore_lunargent_rich')] + ORES_TAIL})
    biome('pics_blancs', 'Pics Blancs', -0.5, 0.6, '#9cc4ff', '#e6f0ff', grass='#8ab08a', foliage='#7aa07a', lunar='ore_lunargent_scarce',
          veg=('boulders', 'minecraft:trees_windswept_hills', 'minecraft:patch_grass_plain'),
          spawns={'creature': [('minecraft:goat', 10, 1, 3), ('eagle', 8, 1, 2)], 'ambient': BATS, 'monster': [('stone_troll', 20, 1, 1), ('warg', 20, 2, 3), ('goblin', 40, 3, 5), ('cave_troll', 8, 1, 1)]})
    biome('terres_de_cendre', 'Terres de Cendre', 2.0, 0.0, '#5a2a22', '#3a201c', water='#4a3a36', wfog='#1c100c', grass='#57503f', foliage='#57503f',
          veg=('ash_spires', 'dead_trees', 'ashroot_patch'), lava_surface=True, precip=False, particle=('minecraft:ash', 0.05),
          spawns={'ambient': BATS + [('crow', 10, 2, 4)], 'monster': [('orc', 60, 3, 5), ('war_orc', 35, 2, 3), ('warg', 25, 2, 4), ('cave_troll', 10, 1, 1), ('pale_rider', 5, 1, 2), ('goblin', 20, 3, 5)]})
    biome('terres_sauvages', 'Terres Sauvages', 1.2, 0.2, '#86b0e8', '#d8d2b0', grass='#b5a74f', foliage='#9a9a42', precip=False,
          veg=('boulders', 'minecraft:trees_savanna', 'minecraft:patch_tall_grass', 'minecraft:patch_grass_savanna', 'minecraft:patch_dead_bush_2'),
          spawns={'creature': [('minecraft:horse', 4, 2, 4), ('minecraft:donkey', 2, 1, 3), ('minecraft:cow', 6, 2, 4), ('mammoth', 5, 1, 2)], 'ambient': BATS, 'monster': [('wild_man', 30, 2, 4), ('warg', 25, 2, 4), ('orc', 20, 2, 3), ('pale_rider', 2, 1, 1), ('goblin', 20, 2, 4)]})
    biome('mer_grise', 'Mer Grise', 0.5, 0.5, '#7fa6d8', '#b8c8d8', water='#3a6a8a', wfog='#061a2a',
          veg=('minecraft:seagrass_normal', 'minecraft:kelp_cold'),
          spawns={'water_ambient': FISH, 'water_creature': SQUID, 'monster': [('lurker', 4, 1, 1)]})
    biome('rivage', 'Rivage', 0.7, 0.5, '#82b4f0', '#c8dcf0', veg=('minecraft:patch_sugar_cane',), spawns={'creature': [('minecraft:turtle', 3, 1, 2)]})
    biome('riviere', 'Rivière', 0.6, 0.6, '#82b4f0', '#c8dcf0', veg=('minecraft:patch_sugar_cane', 'minecraft:seagrass_river'),
          spawns={'water_ambient': [('minecraft:salmon', 6, 1, 5)], 'water_creature': SQUID, 'monster': [('lurker', 6, 1, 1), ('orc', 10, 1, 2)]})
    for id, b in BIOMES.items(): wjson(f'{W}/biome/{id}.json', b)

# ------------------------------------------------------------ source de biomes
T_B = {'cold': (-1.0, -0.35), 'mild': (-0.35, 0.3), 'hot': (0.3, 1.0)}
H_B = {'dry': (-1.0, -0.25), 'mid': (-0.25, 0.25), 'wet': (0.25, 1.0)}
E_B = {'peaks': (-1.0, -0.4), 'hills': (-0.4, 0.15), 'flat': (0.15, 1.0)}
TABLE = {
    'peaks': {'cold': ('pics_blancs',) * 3, 'mild': ('monts_de_fer',) * 3, 'hot': ('terres_de_cendre', 'terres_de_cendre', 'monts_de_fer')},
    'hills': {'cold': ('landes_des_tertres', 'bois_noir', 'bois_noir'), 'mild': ('plaines_du_vent', 'vieille_foret', 'foret_doree'),
              'hot': ('terres_de_cendre', 'terres_sauvages', 'foret_doree')},
    'flat': {'cold': ('landes_des_tertres', 'plaines_du_vent', 'marais_des_morts'), 'mild': ('plaines_du_vent', 'vertes_collines', 'vertes_collines'),
             'hot': ('terres_de_cendre', 'terres_sauvages', 'marais_des_morts')},
}
FULL = (-1.0, 1.0)

def biome_source():
    out = []
    def pt(b, t=FULL, h=FULL, c=FULL, e=FULL, w=FULL):
        out.append({'biome': R(b), 'parameters': {'temperature': list(t), 'humidity': list(h), 'continentalness': list(c),
                                                  'erosion': list(e), 'weirdness': list(w), 'depth': 0.0, 'offset': 0.0}})
    pt('mer_grise', c=(-1.2, -0.19))
    pt('rivage', c=(-0.19, -0.11))
    inland = (-0.11, 1.0)
    pt('riviere', c=inland, e=(-0.4, 1.0), w=(-0.05, 0.05))
    for en, e in E_B.items():
        for tn, t in T_B.items():
            for i, (hn, h) in enumerate(H_B.items()):
                b = TABLE[en][tn][i]
                if en == 'peaks':
                    pt(b, t, h, inland, e)
                else:
                    pt(b, t, h, inland, e, (-1.0, -0.05)); pt(b, t, h, inland, e, (0.05, 1.0))
    return {'type': 'minecraft:multi_noise', 'biomes': out}

def structures():
    def structure(name, typ, biomes_list, extra, spacing, separation, salt, step='surface_structures', adapt='none'):
        tag('worldgen/biome', f'has_structure/{name}', *[R(b) for b in biomes_list], ns=MOD)
        o = {'type': R(typ), 'biomes': f'#{MOD}:has_structure/{name}', 'step': step, 'spawn_overrides': {}, 'terrain_adaptation': adapt}
        o.update(extra)
        wjson(f'{W}/structure/{name}.json', o)
        wjson(f'{W}/structure_set/{name}.json', {'structures': [{'structure': R(name), 'weight': 1}],
              'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': separation, 'salt': salt}})
    structure('volcano', 'volcano', ['volcan'], {'radius': 78, 'height': 104, 'doom': False}, 10, 5, 774411)
    structure('mont_funeste', 'volcano', ['terres_de_cendre'], {'radius': 96, 'height': 132, 'doom': True}, 36, 18, 774412)
    tag('worldgen/structure', 'volcanoes', R('volcano'), R('mont_funeste'), ns=MOD)
    LANG['structure.ringsofash.volcano'] = 'Volcan'

def build():
    features()
    biomes()
    structures()
    wjson(f'{D}/dimension_type/hautemarche.json', {
        'ultrawarm': False, 'natural': True, 'coordinate_scale': 1.0, 'has_skylight': True, 'has_ceiling': False, 'ambient_light': 0.0,
        'monster_spawn_light_level': {'type': 'minecraft:uniform', 'min_inclusive': 0, 'max_inclusive': 7}, 'monster_spawn_block_light_limit': 0,
        'piglin_safe': False, 'bed_works': True, 'respawn_anchor_works': False, 'has_raids': False, 'logical_height': 384, 'min_y': -64,
        'height': 384, 'infiniburn': '#minecraft:infiniburn_overworld', 'effects': 'minecraft:overworld'})
    wjson(f'{W}/noise_settings/hautemarche.json', {
        'sea_level': 63, 'disable_mob_generation': False, 'aquifers_enabled': True, 'ore_veins_enabled': False, 'legacy_random_source': False,
        'default_block': {'Name': 'minecraft:stone'}, 'default_fluid': {'Name': 'minecraft:water', 'Properties': {'level': '0'}},
        'noise': {'min_y': -64, 'height': 384, 'size_horizontal': 1, 'size_vertical': 2},
        'noise_router': noise_router(), 'spawn_target': [], 'surface_rule': surface_rule()})
    wjson(f'{D}/dimension/hautemarche.json', {'type': R('hautemarche'), 'generator': {
        'type': 'minecraft:noise', 'settings': R('hautemarche'), 'biome_source': biome_source()}})
