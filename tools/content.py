"""Contenu : blocs, objets, recettes, textures."""
from genlib import *
from tex import *
import dwarf

def build():
    blocks()
    items()
    recipes()
    LANG.update({
        'itemGroup.ringsofash': 'Les Anneaux de Cendre',
        'dimension.ringsofash.hautemarche': 'La Hautemarche',
    })

# ============================================================ BLOCS
def blocks():
    # --- volcan
    btex('ash_block', stone('#625f61', 1, var=0.5, cell=2, speck=0.9))
    block('ash_block', 'Bloc de cendre', copy='SAND', extra='.mapColor(MapColor.COLOR_GRAY)', tool='shovel', tags=['dirt'])
    vr = stone('#2f2b2d', 2, var=0.9, cell=4, speck=0.5)
    btex('volcanic_rock', ore(vr, ['#4a2a22', '#3a3436'], 5, 5))
    block('volcanic_rock', 'Roche volcanique', copy='BLACKSTONE', tool='pickaxe')
    btex('smoking_rock', ore(vr, ['#ff8a20', '#e0501a', '#ffc040'], 9, 9))
    block('smoking_rock', 'Roche fumante', 'custom_cube', java='new SmokingRockBlock(copy(Blocks.BLACKSTONE).lightLevel(s -> 6))',
          tool='pickaxe', drop='volcanic_rock')
    save(lava_frames('#e8a04a', '#f6d488', '#fff8e0'), f'{A}/textures/block/pale_lava.png')
    wjson(f'{A}/textures/block/pale_lava.png.mcmeta', {'animation': {'frametime': 3, 'interpolate': True}})
    block('pale_lava', 'Lave pâle', 'custom_cube', drop='none',
          java='new PaleLavaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).noCollission().forceSolidOn()'
               '.strength(-1.0F, 3600000.0F).lightLevel(s -> 15).noLootTable().pushReaction(PushReaction.BLOCK))')

    # --- pierre blanche (elfes / hommes)
    btex('white_stone', stone('#d8d5cb', 11, var=0.35, cell=4, speck=0.35))
    block('white_stone', 'Pierre blanche', copy='STONE', tool='pickaxe')
    btex('white_bricks', bricks('#e3e0d6', '#b3aea2', 12, var=0.05))
    block('white_bricks', 'Briques de pierre blanche', copy='STONE_BRICKS', tool='pickaxe')
    family('white_bricks', 'white', 'briques blanches')
    btex('white_pillar', pillar_side('#e6e3da', 13)); btex('white_pillar_top', framed('#e6e3da', 14))
    block('white_pillar', 'Pilier blanc', 'pillar', copy='STONE_BRICKS', tool='pickaxe')

    # --- nains : pierre grise, teinte en jeu = degrade entre deux couleurs (voir DwarvenTint.java)
    btex('dwarven_bricks_plain', bricks('#4b505b', '#2a2d34', 21, var=0.09))
    btex('dwarven_stone', dwarf.d_stone())
    block('dwarven_stone', 'Pierre naine', copy='DEEPSLATE', tool='pickaxe', tint=True)
    btex('dwarven_cobblestone', dwarf.d_cobble())
    block('dwarven_cobblestone', 'Pavés nains', copy='COBBLED_DEEPSLATE', tool='pickaxe', tint=True)
    family('dwarven_cobblestone', 'dwarven', 'pavés nains', tint=True)
    btex('dwarven_bricks', dwarf.d_bricks())
    block('dwarven_bricks', 'Briques naines', copy='DEEPSLATE_BRICKS', tool='pickaxe', tint=True)
    family('dwarven_bricks', 'dwarven', 'briques naines', tint=True)
    ps, pt = dwarf.d_pillar()
    btex('dwarven_pillar', ps); btex('dwarven_pillar_top', pt)
    block('dwarven_pillar', 'Pilier nain', 'pillar', copy='DEEPSLATE_BRICKS', tool='pickaxe', tint=True)
    btex('dwarven_chiseled', dwarf.d_chiseled())
    block('dwarven_chiseled', 'Pierre naine ciselée', copy='CHISELED_DEEPSLATE', tool='pickaxe', tint=True)
    dl, du, di = dwarf.d_door()
    btex('dwarven_door_bottom', dl); btex('dwarven_door_top', du); itex('dwarven_door', di)
    block('dwarven_door', 'Porte naine', 'door', java='new DwarvenDoorBlock(copy(Blocks.IRON_DOOR).noOcclusion().strength(3.5F))', tool='pickaxe', tint=True)
    rune = framed('#474c57', 24)
    stamp(rune, ['..x..x..', '..x.xx..', '..xx.x..', '..x..x..', '.xxx.x..', '..x..xx.', '..x..x..', '..x..x..'],
          {'x': '#f2b23a'}, 4, 4)
    btex('dwarven_runes', rune)
    block('dwarven_runes', 'Pierre runique naine', copy='DEEPSLATE_BRICKS', extra='.lightLevel(s -> 7)', tool='pickaxe')
    lamp = framed('#4b505b', 25)
    for y in range(3, 13):
        for x in range(3, 13):
            d = max(abs(x - 7.5), abs(y - 7.5))
            lamp.putpixel((x, y), mix(rgb('#fff2c0'), rgb('#f0a030'), min(1, d / 5)) + (255,))
    btex('dwarven_lamp', lamp)
    block('dwarven_lamp', 'Lampe naine', copy='DEEPSLATE_BRICKS', extra='.lightLevel(s -> 15)', tool='pickaxe')

    # --- forteresses noires
    bb = bricks('#2c2627', '#141112', 31, var=0.1)
    btex('black_bricks', ore(bb, ['#5a1f1a'], 32, 3))
    block('black_bricks', 'Briques noires', copy='POLISHED_BLACKSTONE_BRICKS', tool='pickaxe')
    family('black_bricks', 'black', 'briques noires')

    # --- bois doreal (elfes)
    btex('golden_log', log_side('#bdb8aa', 41)); btex('golden_log_top', log_top('#bdb8aa', '#ead79e', 42))
    block('golden_log', 'Bûche de doréal', 'pillar', copy='BIRCH_LOG', tool='axe', tags=['logs', 'logs_that_burn'], itags=['logs', 'logs_that_burn'])
    btex('golden_planks', planks('#e2c777', 43))
    block('golden_planks', 'Planches de doréal', copy='BIRCH_PLANKS', tool='axe', tags=['planks'], itags=['planks'])
    family('golden_planks', 'golden', 'doréal', ('stairs', 'slab', 'fence'), tool='axe')
    btex('golden_leaves', leaves('#dcb236', 44, accent='#fbe98a'))
    block('golden_leaves', 'Feuilles de doréal', 'leaves', sapling='golden_sapling')
    btex('golden_sapling', sprite([
        '................', '.......bb.......', '......bcab......', '.....babbab.....', '....bbacbaab....', '.....abbabb.....',
        '......babb......', '.......ss.......', '..bb...ss..ab...', '.baab..ss.bbab..', '..bab..ssabb....', '...ba.ssss......',
        '.......ss.......', '.......ss.......', '.......rs.......', '......rrss......'], matpal('#dcb236')))
    block('golden_sapling', 'Pousse de doréal', 'sapling',
          java='new SaplingBlock(new TreeGrower("roa_golden", Optional.empty(), Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, id("golden_tree_small"))), Optional.empty()), copy(Blocks.OAK_SAPLING))')

    # --- bois venerable (vieille foret)
    btex('ancient_log', log_side('#4b3b2b', 51)); btex('ancient_log_top', log_top('#4b3b2b', '#806040', 52))
    block('ancient_log', 'Bûche de vénérable', 'pillar', copy='DARK_OAK_LOG', tool='axe', tags=['logs', 'logs_that_burn'], itags=['logs', 'logs_that_burn'])
    btex('ancient_planks', planks('#6f5034', 53))
    block('ancient_planks', 'Planches de vénérable', copy='DARK_OAK_PLANKS', tool='axe', tags=['planks'], itags=['planks'])
    family('ancient_planks', 'ancient', 'vénérable', ('stairs', 'slab', 'fence'), tool='axe')
    btex('ancient_leaves', leaves('#2f5c2b', 54, holes=0.12))
    block('ancient_leaves', 'Feuilles de vénérable', 'leaves')

    # --- chaume
    th = log_side('#c8a64c', 61).rotate(90)
    btex('thatch', th)
    block('thatch', 'Chaume', copy='HAY_BLOCK', tool='hoe')
    family('thatch', 'thatch', 'chaume', ('stairs', 'slab'), tool='hoe')

    # --- lunargent
    ds = stone('#4e4e56', 71, var=0.6, cell=4, speck=0.4)
    btex('lunargent_ore', ore(ds, ['#e6f6ff', '#a6dcf2', '#ffffff', '#7fc4e6'], 72, 7))
    block('lunargent_ore', 'Minerai de lunargent', 'ore', copy='DEEPSLATE_DIAMOND_ORE', tool='pickaxe', level='iron', ore_drop='raw_lunargent')
    lb = framed('#cfe6f2', 73)
    stamp(lb, ['c.......', '.c......', '..c.....', '...c....'], {'c': '#ffffff'}, 3, 3)
    btex('lunargent_block', lb)
    block('lunargent_block', 'Bloc de lunargent', copy='IRON_BLOCK', tool='pickaxe', level='iron', tags=['beacon_base_blocks'])
    gp = stone('#d9a520', 74, var=0.8, cell=2, speck=0.8)
    for (x, y) in [(2, 3), (9, 2), (5, 8), (12, 9), (3, 12), (9, 13)]:
        stamp(gp, ['.bb.', 'baad', 'baad', '.dd.'], {'a': '#f4c430', 'b': '#fff1a0', 'd': '#a87410'}, x, y)
    btex('gold_pile', gp)
    # tas d'or pose en couches (1 a 8) comme la neige
    loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': mid('gold_pile'), 'functions': [
        {'function': 'minecraft:set_count', 'count': n, 'add': False, 'conditions': [
            {'condition': 'minecraft:block_state_property', 'block': mid('gold_pile'), 'properties': {'layers': str(n)}}]} for n in range(1, 9)]}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
    block('gold_pile', "Tas d'or", 'custom', java='new CoinPileBlock(copy(Blocks.GOLD_BLOCK).noOcclusion().forceSolidOff().strength(1.2F).pushReaction(PushReaction.DESTROY)'
          '.isViewBlocking((s, l, p) -> s.getValue(SnowLayerBlock.LAYERS) >= 8))', tool='pickaxe', level='iron', drop=loot)
    for n in range(1, 8):
        bmodel(f'gold_pile_h{n}', f'minecraft:block/snow_height{2 * n}', {'texture': T('gold_pile'), 'particle': T('gold_pile')})
    bmodel('gold_pile_h8', 'minecraft:block/cube_all', {'all': T('gold_pile')})
    bstate('gold_pile', {'variants': {f'layers={n}': {'model': T(f'gold_pile_h{n}')} for n in range(1, 9)}})
    imodel('gold_pile', {'parent': T('gold_pile_h2')})

    # --- quatre nouveaux minerais
    base_ore = lambda seed: stone('#4e4e56', seed, var=0.6, cell=4, speck=0.4)
    for oid, fr, seed, specks, drop in (
            ('sombrefer', 'Minerai de sombrefer', 81, ['#8a7fa8', '#5d5270', '#b8aed0', '#463d5a'], 'raw_sombrefer'),
            ('braisite', 'Minerai de braisite', 82, ['#ff7a2a', '#d8381a', '#ffb060', '#8a2010'], 'braisite'),
            ('brumecristal', 'Minerai de brumecristal', 83, ['#e8fbff', '#b4e8f0', '#ffffff', '#86c8d8'], 'brumecristal'),
            ('orrune', "Minerai d'orrune", 84, ['#f2c64a', '#fff0a0', '#c89a28', '#fff8d0'], 'raw_orrune')):
        btex(f'{oid}_ore', ore(base_ore(seed), specks, seed + 10, 8))
        block(f'{oid}_ore', fr, 'ore', copy='DEEPSLATE_DIAMOND_ORE', tool='pickaxe', level='iron', ore_drop=drop)
    # tapis de pieces d'or (couche fine posee sur le sol, comme un tapis)
    bmodel('gold_carpet', 'minecraft:block/carpet', {'wool': T('gold_pile')})
    bstate('gold_carpet', {'variants': {'': {'model': T('gold_carpet')}}})
    imodel('gold_carpet', {'parent': T('gold_carpet')})
    block('gold_carpet', "Tapis de pièces d'or", 'custom', java='new CarpetBlock(copy(Blocks.GOLD_BLOCK).noOcclusion().forceSolidOff().strength(0.6F).pushReaction(PushReaction.DESTROY))', tool='pickaxe')

    # --- plantes
    fl = {'s': '#3f7a34', 'r': '#2d5a26', 'a': '#f2c21e', 'b': '#fff0a0', 'c': '#ffffff', 'x': '#8fb7ff', 'y': '#dfeaff', 'z': '#5d83d6',
          'k': '#3a3436', 'm': '#5a5053', 'e': '#c05a20', 'p': '#5c8a3a', 'q': '#86b25a'}
    btex('sunstar', sprite([
        '................', '................', '................', '.......b........', '....a..b..a.....', '.....a.b.a......',
        '...bbbbcbbbb....', '.....a.b.a......', '....a..b..a.....', '.......s........', '.......s..r.....', '....r..s.rr.....',
        '....rr.srr......', '.....rrsr.......', '......rs........', '.......s........'], fl))
    block('sunstar', "Étoile-d'or", 'cross', java='new FlowerBlock(MobEffects.REGENERATION, 6.0F, copy(Blocks.DANDELION))', tags=['small_flowers'], itags=['small_flowers'])
    btex('moonbell', sprite([
        '................', '................', '......ss........', '.....s..s.......', '....s....s......', '...yxy...s......',
        '...xxz..yxy.....', '...z.z..xxz.....', '.........z.z....', '.......s........', '.......s........', '...r...s..r.....',
        '....rr.s.rr.....', '.....rrsrr......', '......rsr.......', '.......s........'], fl))
    block('moonbell', 'Clochelune', 'cross', java='new FlowerBlock(MobEffects.NIGHT_VISION, 8.0F, copy(Blocks.DANDELION))', tags=['small_flowers'], itags=['small_flowers'])
    btex('ashroot', sprite([
        '................', '................', '................', '..k.........k...', '...k...k...k....', '...k...k..k.....',
        '....k..m..k..e..', '....mk.m.km.....', '..e..m.mkm......', '......mmm...k...', '..k...kmk..k....', '...k...m..m.....',
        '....mk.m.mk.....', '.....mmmmm......', '......kmk.......', '.......m........'], fl))
    block('ashroot', 'Racine de cendre', 'cross', java='new AshPlantBlock(copy(Blocks.DEAD_BUSH))')
    btex('pipeweed', sprite([
        '................', '.......c........', '......ccc.......', '.......c........', '...q...p...q....', '..qpq..p..qpq...',
        '...qpp.p.ppq....', '....qppppq......', '..q..pppp..q....', '.qpq..pp..qpq...', '..qpp.pp.ppq....', '...qpppppq......',
        '.....pppp.......', '......pp........', '......pp........', '......pp........'], fl))
    block('pipeweed', 'Herbe à pipe', 'cross', java='new FlowerBlock(MobEffects.SLOW_FALLING, 5.0F, copy(Blocks.DANDELION))')

# ============================================================ OBJETS
INGOT = ['................', '................', '................', '................', '......oooooo....', '....oobbbbcco...',
         '..oobbbbbccaao..', '.obbbbbccaaado..', '.oaaaaaaaaddo...', '.oaddddddddo....', '..oooooooooo....', '................',
         '................', '................', '................', '................']
RAW = ['................', '................', '.....ooo........', '....obbcoo......', '...obaaabcoo....', '..obaadaaabo....',
       '..oaadddaaabo...', '.obaaddaabaao...', '.oaaaadaaaado...', '.oadaaaaadddo...', '..odddaadddo....', '...oodddddo.....',
       '.....ooooo......', '................', '................', '................']
COIN = ['................', '................', '................', '.....oooooo.....', '....obbbbcco....', '...obaaaaaaco...',
        '...obatttaaao...', '...obataataao...', '...obatttaado...', '...obataaaado...', '...oaataaaddo...', '....oaaadddo....',
        '.....oooooo.....', '................', '................', '................']
RING = ['................', '................', '......ghg.......', '.....oggho......', '....oobbcoo.....', '...obbooocco....',
        '..obao...oaco...', '..oao.....oao...', '..oao.....oao...', '..oado...oaao...', '...odaoooaao....', '....oddaaao.....',
        '.....ooooo......', '................', '................', '................']

GEM = ['................', '................', '................', '.....oooooo.....', '....obbbbcco....', '...obbaaaaaco...', '...obaaaaaaao...', '....obaaaaado...',
       '.....obaaado....', '......obado.....', '.......oao......', '........o.......', '................', '................', '................', '................']

def items():
    item('raw_lunargent', 'Lunargent brut', tex=sprite(RAW, matpal('#a9d3e6')))
    item('raw_sombrefer', 'Sombrefer brut', tex=sprite(RAW, matpal('#5d5270')))
    item('braisite', 'Braisite', tex=sprite(GEM, matpal('#e0502a')))
    item('brumecristal', 'Brumecristal', tex=sprite(GEM, matpal('#b4e8f0')))
    item('raw_orrune', 'Orrune brute', tex=sprite(RAW, matpal('#e0b43a')))
    item('lunargent_ingot', 'Lingot de lunargent', tex=sprite(INGOT, matpal('#cfe6f2')), tags=['beacon_payment_items'])
    item('black_steel_ingot', "Lingot d'acier noir", tex=sprite(INGOT, matpal('#3d3a42')))
    item('silver_coin', "Pièce d'argent", tex=sprite(COIN, matpal('#c9ccd4', trim='#8e929c')))
    item('halfling_ring', 'Anneau des semi-hommes', java='new TravelRingItem(props().stacksTo(1).fireResistant().rarity(Rarity.RARE))',
         tex=sprite(RING, matpal('#c9a23a', gem='#58c070')))

# ============================================================ RECETTES
def recipes():
    for wood in ('golden', 'ancient'):
        shapeless(f'{wood}_planks', [f'{wood}_log'], f'{wood}_planks', 4, 'building')
        shaped(f'{wood}_planks_stairs', ['#  ', '## ', '###'], {'#': f'{wood}_planks'}, f'{wood}_planks_stairs', 4, 'building')
        shaped(f'{wood}_planks_slab', ['###'], {'#': f'{wood}_planks'}, f'{wood}_planks_slab', 6, 'building')
        shaped(f'{wood}_planks_fence', ['#s#', '#s#'], {'#': f'{wood}_planks', 's': 'minecraft:stick'}, f'{wood}_planks_fence', 3)
    for st in ('white_bricks', 'dwarven_bricks', 'black_bricks'):
        shaped(f'{st}_stairs', ['#  ', '## ', '###'], {'#': st}, f'{st}_stairs', 4, 'building')
        shaped(f'{st}_slab', ['###'], {'#': st}, f'{st}_slab', 6, 'building')
        shaped(f'{st}_wall', ['###', '###'], {'#': st}, f'{st}_wall', 6)
        for k in ('stairs', 'slab', 'wall'):
            stonecut(st, f'{st}_{k}', 2 if k == 'slab' else 1)
    shaped('white_bricks', ['##', '##'], {'#': 'white_stone'}, 'white_bricks', 4, 'building')
    shaped('white_pillar', ['#', '#'], {'#': 'white_bricks'}, 'white_pillar', 2, 'building')
    shaped('dwarven_bricks', ['##', '##'], {'#': 'minecraft:deepslate'}, 'dwarven_bricks', 4, 'building')
    shaped('dwarven_pillar', ['#', '#'], {'#': 'dwarven_bricks'}, 'dwarven_pillar', 2, 'building')
    shaped('dwarven_runes', [' # ', '#g#', ' # '], {'#': 'dwarven_bricks', 'g': 'minecraft:gold_nugget'}, 'dwarven_runes', 4, 'building')
    shaped('dwarven_lamp', [' # ', '#g#', ' # '], {'#': 'dwarven_bricks', 'g': 'minecraft:glowstone'}, 'dwarven_lamp', 4, 'building')
    shaped('black_bricks', ['##', '##'], {'#': 'volcanic_rock'}, 'black_bricks', 4, 'building')
    shaped('thatch', ['##', '##'], {'#': 'minecraft:wheat'}, 'thatch', 2, 'building')
    shaped('thatch_stairs', ['#  ', '## ', '###'], {'#': 'thatch'}, 'thatch_stairs', 4, 'building')
    shaped('thatch_slab', ['###'], {'#': 'thatch'}, 'thatch_slab', 6, 'building')
    shaped('lunargent_block', ['###', '###', '###'], {'#': 'lunargent_ingot'}, 'lunargent_block')
    shapeless('lunargent_ingot_from_block', ['lunargent_block'], 'lunargent_ingot', 9)
    smelt('lunargent_ingot_from_raw', 'raw_lunargent', 'lunargent_ingot', 1.5)
    smelt('lunargent_ingot_from_ore', 'lunargent_ore', 'lunargent_ingot', 1.5)
