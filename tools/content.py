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
    family('dwarven_stone', 'dwarven', 'pierre naine', tint=True)
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

    # --- lumiere naine : bloc lumineux teinte comme la pierre naine (cadre de pierre teinte, coeur de lumiere) et torche murale
    fr = dwarf.d_stone().convert('RGBA')
    for (x0, x1) in ((3, 7), (9, 13)):                       # quatre fenetres separees par une croix de pierre
        for (y0, y1) in ((3, 7), (9, 13)):
            for y in range(y0, y1):
                for x in range(x0, x1): fr.putpixel((x, y), (0, 0, 0, 0))
    btex('dwarven_light_frame', fr)
    core = new()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            core.putpixel((x, y), mix(rgb('#fff6cf'), rgb('#f2a62e'), min(1, d)) + (255,))
    btex('dwarven_light_core', core)
    block('dwarven_light', 'Bloc de lumière naine', 'custom', java='new Block(copy(Blocks.DEEPSLATE_BRICKS).lightLevel(s -> 15).strength(2.0F, 6.0F))',
          tool='pickaxe', tintlist=True)
    els = [box((0, 0, 0), (16, 16, 16), '#frame'), box((1, 1, 1), (15, 15, 15), '#core', tint=False, cull=False)]
    wjson(f'{A}/models/block/dwarven_light.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
          'textures': {'frame': T('dwarven_light_frame'), 'core': T('dwarven_light_core'), 'particle': T('dwarven_light_core')}, 'elements': els})
    bstate('dwarven_light', {'variants': {'': {'model': T('dwarven_light')}}})
    imodel('dwarven_light', {'parent': T('dwarven_light')})

    btex('dwarven_torch_iron', stone('#30333b', 61, var=0.25, cell=3, speck=0.25))
    btex('dwarven_torch_bronze', stone('#b98a35', 62, var=0.18, cell=3, speck=0.2))
    btex('dwarven_torch_ember', stone('#e2681c', 63, var=0.35, cell=2, speck=0.5))
    fl = new()
    for y in range(16):
        for x in range(16):
            half = 1 + (y - 2) * 0.42                                   # flamme en goutte : large en bas, pointue en haut
            if 2 <= y <= 15 and abs(x - 7.5) <= half and not (y > 12 and abs(x - 7.5) > 5.5):
                t = (y - 2) / 13.0
                core_ = abs(x - 7.5) <= half * 0.45 and y > 6
                fl.putpixel((x, y), (mix(rgb('#ffe9a0'), rgb('#fff6d0'), t) if core_ else mix(rgb('#ff8a1e'), rgb('#ffc03a'), t)) + (255,))
    btex('dwarven_torch_flame', fl)
    block('dwarven_torch', 'Torche murale naine', 'custom', java='new DwarvenTorchBlock(BlockBehaviour.Properties.of().noCollission().strength(0.6F).sound(SoundType.METAL)'
          '.lightLevel(s -> 14).pushReaction(PushReaction.DESTROY))', tool='pickaxe', tintlist=True)
    te = [box((4.5, 2, 14.5), (11.5, 13, 16), '#stone', cull=False),                                           # platine de pierre (teintee)
          box((4, 12, 14), (12, 13.5, 16), '#bronze', tint=False, cull=False),                                  # bandeau de bronze
          box((7, 4, 8), (9, 6, 14.5), '#iron', tint=False, cull=False),                                       # bras de fer
          box((7, 5, 7), (9, 9, 9), '#iron', tint=False, cull=False),                                          # pied de la coupe
          box((5, 9, 5), (11, 11, 11), '#iron', tint=False, cull=False),                                       # coupe
          box((4, 11, 4), (12, 12, 5), '#bronze', tint=False, cull=False), box((4, 11, 11), (12, 12, 12), '#bronze', tint=False, cull=False),
          box((4, 11, 5), (5, 12, 11), '#bronze', tint=False, cull=False), box((11, 11, 5), (12, 12, 11), '#bronze', tint=False, cull=False),
          box((5.2, 10.9, 5.2), (10.8, 11.1, 10.8), '#ember', tint=False, cull=False)]                         # braises
    for ax in (0, 1):                                                                                           # flamme : deux plans croises
        a, b = ((8, 11.5, 5), (8, 16, 11)) if ax == 0 else ((5, 11.5, 8), (11, 16, 8))
        fcs = ('east', 'west') if ax == 0 else ('north', 'south')
        te.append({'from': list(a), 'to': list(b), 'faces': {f: {'uv': [0, 0, 16, 16], 'texture': '#flame'} for f in fcs}})
    wjson(f'{A}/models/block/dwarven_torch.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
          'textures': {'stone': T('dwarven_stone'), 'iron': T('dwarven_torch_iron'), 'bronze': T('dwarven_torch_bronze'), 'ember': T('dwarven_torch_ember'),
                       'flame': T('dwarven_torch_flame'), 'particle': T('dwarven_torch_iron')}, 'elements': te})
    bstate('dwarven_torch', {'variants': {f'facing={f}': ({'model': T('dwarven_torch')} if y == 0 else {'model': T('dwarven_torch'), 'y': y})
                                          for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
    ic = new()
    for y in range(16):
        for x in range(16):
            if 4 <= y <= 14 and 11 <= x <= 13: ic.putpixel((x, y), (86, 92, 104, 255))                          # platine
            if 8 <= y <= 9 and 6 <= x <= 10: ic.putpixel((x, y), (48, 51, 59, 255))                              # bras
            if y in (8, 9, 10) and 3 <= x <= 8: ic.putpixel((x, y), (185, 138, 53, 255) if y == 8 else (48, 51, 59, 255))   # coupe
    for (x, y, c) in ((5, 7, '#ffe9a0'), (6, 7, '#ffc03a'), (5, 6, '#ffc03a'), (5, 5, '#ff8a1e'), (4, 6, '#ff8a1e'), (6, 6, '#ff8a1e'), (5, 4, '#ff8a1e'), (4, 7, '#ff8a1e'), (6, 8, '#ffc03a')):
        ic.putpixel((x, y), rgb(c) + (255,))
    itex('dwarven_torch', ic)
    imodel('dwarven_torch', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/dwarven_torch'}})

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

    # toile d'araignee : bloc qui englue (comme la toile vanilla), dessinee en rayons et anneaux
    from PIL import Image as _I, ImageDraw as _D
    _w = _I.new('RGBA', (16, 16), (0, 0, 0, 0))
    _d = _D.Draw(_w)
    _c = (232, 234, 240, 255); _s = (196, 200, 212, 235)
    for p in ((0, 0, 15, 15), (15, 0, 0, 15), (7, 0, 8, 15), (0, 7, 15, 8)): _d.line(p, fill=_c)
    for r in (3, 6):
        pts = [(7.5 + r * dx, 7.5 + r * dy) for dx, dy in ((1, 0), (.7, .7), (0, 1), (-.7, .7), (-1, 0), (-.7, -.7), (0, -1), (.7, -.7))]
        for i in range(8):
            _d.line((round(pts[i][0]), round(pts[i][1]), round(pts[(i + 1) % 8][0]), round(pts[(i + 1) % 8][1])), fill=_s)
    btex('spider_web', _w)
    block('spider_web', "Toile d'araignée", 'cross', java='new WebBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.COBWEB).noCollission().noOcclusion().strength(0.6F).pushReaction(PushReaction.DESTROY))',
          drop={'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
              {'type': 'minecraft:item', 'name': mid('spider_web'), 'conditions': [{'condition': 'minecraft:any_of', 'terms': [
                  {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}},
                  {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]}]},
              {'type': 'minecraft:item', 'name': 'minecraft:string', 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]}]}]}]})

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
    shaped('dwarven_light', [' # ', '#g#', ' # '], {'#': 'dwarven_stone', 'g': 'minecraft:glowstone'}, 'dwarven_light', 4, 'building')
    shaped('dwarven_torch', ['c', 'i', '#'], {'c': 'minecraft:coal', 'i': 'minecraft:iron_ingot', '#': 'dwarven_bricks'}, 'dwarven_torch', 2, 'building')
    shaped('dwarven_lamp', [' # ', '#g#', ' # '], {'#': 'dwarven_bricks', 'g': 'minecraft:glowstone'}, 'dwarven_lamp', 4, 'building')
    shaped('black_bricks', ['##', '##'], {'#': 'volcanic_rock'}, 'black_bricks', 4, 'building')
    shaped('thatch', ['##', '##'], {'#': 'minecraft:wheat'}, 'thatch', 2, 'building')
    shaped('thatch_stairs', ['#  ', '## ', '###'], {'#': 'thatch'}, 'thatch_stairs', 4, 'building')
    shaped('thatch_slab', ['###'], {'#': 'thatch'}, 'thatch_slab', 6, 'building')
    shaped('lunargent_block', ['###', '###', '###'], {'#': 'lunargent_ingot'}, 'lunargent_block')
    shapeless('lunargent_ingot_from_block', ['lunargent_block'], 'lunargent_ingot', 9)
    smelt('lunargent_ingot_from_raw', 'raw_lunargent', 'lunargent_ingot', 1.5)
    smelt('lunargent_ingot_from_ore', 'lunargent_ore', 'lunargent_ingot', 1.5)
