"""Contenu : blocs, objets, recettes, textures."""
from genlib import *
from tex import *
import random, math
import dwarf

def build():
    blocks()
    items()
    recipes()
    LANG.update({
        'itemGroup.ringsofash': 'Les Anneaux de Cendre',
        'dimension.ringsofash.hautemarche': 'La Hautemarche',
    })


def morgul_veins(seed, hot):
    """calque lumineux : les joints des briques (et, pour la version chaude, une rangee de briques) en vert vif"""
    import random as _r
    rnd = _r.Random(seed)
    im = new()
    for r in range(4):
        off = 4 if r % 2 else 0
        for bx in range(-1, 3):
            for yy in range(4):
                for xx in range(8):
                    x = bx * 8 + off + xx; y = r * 4 + yy
                    if not (0 <= x < 16): continue
                    joint = yy == 3 or xx == 7
                    if joint and rnd.random() < (0.97 if hot else 0.78):
                        c = mix(rgb('#a6ffb0'), rgb('#2fd46c'), rnd.random() * 0.75)
                    elif hot and yy == 2 and rnd.random() < 0.85:
                        c = mix(rgb('#33c46b'), rgb('#1c8a4a'), rnd.random())
                    elif hot and yy == 1 and rnd.random() < 0.35:
                        c = rgb('#1c8a4a')
                    else:
                        continue
                    im.putpixel((x, y), c + (255,))
    return im

def morgul_crystal(seed):
    """bloc entierement lumineux : cristal vert degrade"""
    rnd = random.Random(seed)
    im = new()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            c = mix(rgb('#d4ffd8'), rgb('#27b85c'), min(1.0, d * 0.9 + rnd.random() * 0.18))
            if min(x, y, 15 - x, 15 - y) == 0: c = shade(c, 0.78)
            im.putpixel((x, y), c + (255,))
    return im

def ichor_frames(size, frames, seed):
    """liquide style lave aux couleurs des cristaux de Morgul ; bruit periodique : se raccorde parfaitement d'un bloc a l'autre et d'une image a l'autre"""
    rnd = random.Random(seed)
    def lattice(n): return [[rnd.random() for _ in range(n)] for _ in range(n)]
    def smooth(t): return t * t * (3 - 2 * t)
    def noise(g, n, x, y):                       # x, y dans [0, size) ; periode = size
        fx, fy = x * n / size, y * n / size
        ix, iy = int(math.floor(fx)), int(math.floor(fy))
        tx, ty = smooth(fx - ix), smooth(fy - iy)
        a, b = g[iy % n][ix % n], g[iy % n][(ix + 1) % n]
        d, e = g[(iy + 1) % n][ix % n], g[(iy + 1) % n][(ix + 1) % n]
        return (a + (b - a) * tx) * (1 - ty) + (d + (e - d) * tx) * ty
    n1, n2 = size // 4, size // 2
    g1, g2 = lattice(n1), lattice(n2)
    ramp = [(0.0, rgb('#178040')), (0.35, rgb('#27b85c')), (0.7, rgb('#7fe898')), (1.0, rgb('#d4ffd8'))]
    def color(w):
        for (p0, c0), (p1, c1) in zip(ramp, ramp[1:]):
            if w <= p1: return mix(c0, c1, (w - p0) / (p1 - p0))
        return ramp[-1][1]
    im = Image.new('RGBA', (size, size * frames))
    for f in range(frames):
        sh = f * size / frames
        for y in range(size):
            for x in range(size):
                v = 0.6 * noise(g1, n1, x, (y + sh) % size) + 0.4 * noise(g2, n2, (x - sh * 2) % size, y)
                w = min(1.0, max(0.0, (v - 0.22) / 0.52))
                im.putpixel((x, f * size + y), color(w * w * (3 - 2 * w)) + (255,))
    return im

def glow_block(id, fr, base_tex, glow_tex, light):
    """bloc de pierre dont un calque de pixels est emissif (lumiere pleine, meme dans le noir), avec une vraie lumiere en jeu"""
    block(id, fr, 'custom', java=f'new Block(copy(Blocks.POLISHED_BLACKSTONE_BRICKS).lightLevel(s -> {light}))', tool='pickaxe')
    faces = ('down', 'up', 'north', 'south', 'west', 'east')
    def els(tex, lo, hi, light_data):
        e = {'from': [lo] * 3, 'to': [hi] * 3,
             'faces': {f: {'uv': [0, 0, 16, 16], 'texture': tex, 'cullface': f} for f in faces}}
        if light_data: e['neoforge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
        return e
    textures = {'base': T(base_tex), 'particle': T(base_tex)}
    elements = [els('#base', 0, 16, glow_tex is None)]
    if glow_tex:
        textures['glow'] = T(glow_tex)
        elements.append(els('#glow', -0.02, 16.02, True))
    wjson(f'{A}/models/block/{id}.json', {'render_type': 'minecraft:cutout', 'textures': textures, 'elements': elements})
    bstate(id, {'variants': {'': {'model': T(id)}}})
    imodel(id, {'parent': T(id)})

# ============================================================ BLOCS
def blocks():
    # --- volcan
    btex('ash_block', stone('#625f61', 1, var=0.5, cell=2, speck=0.9))
    block('ash_block', 'Bloc de cendre', copy='SAND', extra='.mapColor(MapColor.COLOR_GRAY)', tool='shovel', tags=['dirt'])
    vr = stone('#2f2b2d', 2, var=0.9, cell=4, speck=0.5)
    btex('volcanic_rock', ore(vr, ['#4a2a22', '#3a3436'], 5, 5))
    block('volcanic_rock', 'Roche volcanique', copy='BLACKSTONE', tool='pickaxe')
    # roches des terres de cendre : pierre grise couverte de cendre, et roche calcinee aux braises eteintes
    ar = stone('#5d5a56', 21, var=0.85, cell=3, speck=0.75)
    btex('ash_rock', ore(ar, ['#8a867f', '#3d3b39'], 22, 7))
    block('ash_rock', 'Roche de cendre', copy='STONE', tool='pickaxe')
    family('ash_rock', 'ash', 'roche de cendre')
    sr = stone('#3a3432', 23, var=0.9, cell=3, speck=0.6)
    btex('scorched_rock', ore(sr, ['#7a3a1e', '#52291c', '#8a8680'], 24, 6))
    block('scorched_rock', 'Roche calcinée', copy='BLACKSTONE', tool='pickaxe')
    btex('smoking_rock', ore(vr, ['#ff8a20', '#e0501a', '#ffc040'], 9, 9))
    block('smoking_rock', 'Roche fumante', 'custom_cube', java='new SmokingRockBlock(copy(Blocks.BLACKSTONE).lightLevel(s -> 6))',
          tool='pickaxe', drop='volcanic_rock')
    save(lava_frames('#e8a04a', '#f6d488', '#fff8e0'), f'{A}/textures/block/pale_lava.png')
    wjson(f'{A}/textures/block/pale_lava.png.mcmeta', {'animation': {'frametime': 3, 'interpolate': True}})
    block('pale_lava', 'Lave pâle', 'custom_cube', drop='none',
          java='new PaleLavaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).noCollission().forceSolidOn()'
               '.strength(-1.0F, 3600000.0F).lightLevel(s -> 15).noLootTable().pushReaction(PushReaction.BLOCK))')

    # --- ichor de Morgul : liquide vert lumineux (fluide de type lave, code dans fluid/ModFluids.java ; texture procedurale aux couleurs des cristaux de Morgul)
    save(ichor_frames(16, 16, 11), f'{A}/textures/block/morgul_ichor_still.png')
    wjson(f'{A}/textures/block/morgul_ichor_still.png.mcmeta', {'animation': {'frametime': 3, 'interpolate': True}})
    save(ichor_frames(32, 32, 12), f'{A}/textures/block/morgul_ichor_flow.png')
    wjson(f'{A}/textures/block/morgul_ichor_flow.png.mcmeta', {'animation': {'frametime': 2, 'interpolate': True}})
    wjson(f'{A}/particles/ichor_smoke.json', {'textures': [f'minecraft:generic_{i}' for i in range(8)]})
    bstate('morgul_ichor', {'variants': {'': {'model': f'{MOD}:block/morgul_ichor'}}})
    wjson(f'{A}/models/block/morgul_ichor.json', {'textures': {'particle': f'{MOD}:block/morgul_ichor_still'}})
    imodel('morgul_ichor_bucket', {'parent': 'neoforge:item/bucket_drip', 'loader': 'neoforge:fluid_container', 'fluid': f'{MOD}:morgul_ichor'})
    LANG[f'block.{MOD}.morgul_ichor'] = 'Ichor de Morgul'
    LANG[f'fluid_type.{MOD}.morgul_ichor'] = 'Ichor de Morgul'
    LANG[f'item.{MOD}.morgul_ichor_bucket'] = "Seau d'ichor de Morgul"
    tag('fluid', 'lava', f'{MOD}:morgul_ichor', f'{MOD}:flowing_morgul_ichor')        # brule comme la lave

    # --- pierre blanche (elfes / hommes)
    btex('white_stone', stone('#d8d5cb', 11, var=0.35, cell=4, speck=0.35))
    block('white_stone', 'Pierre blanche', copy='STONE', tool='pickaxe')
    btex('white_bricks', bricks('#e3e0d6', '#b3aea2', 12, var=0.05))
    block('white_bricks', 'Briques de pierre blanche', copy='STONE_BRICKS', tool='pickaxe')
    family('white_bricks', 'white', 'briques blanches')
    btex('white_pillar', pillar_side('#e6e3da', 13)); btex('white_pillar_top', framed('#e6e3da', 14))
    block('white_pillar', 'Pilier blanc', 'pillar', copy='STONE_BRICKS', tool='pickaxe')
    # --- Fondcombe : toits de tuiles (orange, vert-de-gris, or), enduit creme, colombage dore, pierre grise des tours, panneau ajoure
    for nm, fr, col, acc, sd in (('orange', 'orangées', '#c8662b', '#e08a3a', 61), ('green', 'vert-de-gris', '#4f9a82', '#79c0a6', 62), ('gold', 'dorées', '#d6a844', '#f0cd6e', 63)):
        btex(f'elven_tiles_{nm}', tiles(col, sd, accent=acc))
        block(f'elven_tiles_{nm}', f'Tuiles elfiques {fr}', copy='STONE_BRICKS', tool='pickaxe')
        family(f'elven_tiles_{nm}', f'tuiles_{nm}', f'tuiles elfiques {fr}', ('stairs', 'slab'))
    btex('elven_plaster', plaster('#eadfc2', 64))
    block('elven_plaster', 'Enduit elfique', copy='STONE_BRICKS', tool='pickaxe')
    family('elven_plaster', 'enduit', 'enduit elfique')
    btex('elven_timber', timber('#b98a3c', '#eadfc2', 65))
    block('elven_timber', 'Colombage elfique', copy='STONE_BRICKS', tool='pickaxe')
    btex('tower_stone', bricks('#a9abae', '#7d7f83', 66, bw=8, bh=4, var=0.08))
    block('tower_stone', 'Briques de tour grises', copy='STONE_BRICKS', tool='pickaxe')
    family('tower_stone', 'tour', 'briques de tour grises')
    btex('elven_filigree', filigree('#e8e4d8', '#c9a24a', 67))
    block('elven_filigree', 'Panneau ajouré elfique', copy='STONE_BRICKS', tool='pickaxe')
    # --- Minas Tirith : calcaire jaune pale (bandeaux, toits, murs) et roche de la Crete (eperon)
    btex('limestone_bricks', bricks('#ecdfae', '#bcae7e', 31, var=0.06))
    block('limestone_bricks', 'Briques de calcaire pâle', copy='STONE_BRICKS', tool='pickaxe')
    family('limestone_bricks', 'calcaire', 'briques de calcaire')
    btex('keel_stone', stone('#c4bda8', 32, var=0.55, cell=5, speck=0.45))
    block('keel_stone', 'Roche de la Crête', copy='STONE', tool='pickaxe')

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

    # --- forteresses noires
    bb = bricks('#2c2627', '#141112', 31, var=0.1)
    btex('black_bricks', ore(bb, ['#5a1f1a'], 32, 3))
    block('black_bricks', 'Briques noires', copy='POLISHED_BLACKSTONE_BRICKS', tool='pickaxe')
    family('black_bricks', 'black', 'briques noires')

    # --- Minas Morgul : pierre gris-vert (#343D38 base, #252A29 extremites, #596B55 details, #77766B reflets) et lueur verte emissive
    btex('morgul_bricks', ore(bricks('#343D38', '#252A29', 51, var=0.07), ['#596B55'], 52, 4))
    block('morgul_bricks', 'Briques de Morgul', copy='POLISHED_BLACKSTONE_BRICKS', tool='pickaxe')
    family('morgul_bricks', 'morgul', 'briques de Morgul')
    btex('morgul_dark_bricks', bricks('#252A29', '#171B1A', 53, var=0.08))
    block('morgul_dark_bricks', 'Briques sombres de Morgul', copy='POLISHED_BLACKSTONE_BRICKS', tool='pickaxe')
    family('morgul_dark_bricks', 'morgul', 'briques sombres de Morgul')
    btex('morgul_pale_bricks', bricks('#77766B', '#4f5a4c', 54, var=0.06))
    block('morgul_pale_bricks', 'Briques pâles de Morgul', copy='POLISHED_BLACKSTONE_BRICKS', tool='pickaxe')
    family('morgul_pale_bricks', 'morgul', 'briques pâles de Morgul')
    btex('morgul_detail', framed('#596B55', 55))
    block('morgul_detail', 'Pierre verte ciselée de Morgul', copy='POLISHED_BLACKSTONE_BRICKS', tool='pickaxe')
    btex('morgul_vein_glow', morgul_veins(56, False))
    btex('morgul_vein_hot_glow', morgul_veins(57, True))
    btex('morgul_glow_tex', morgul_crystal(58))
    glow_block('morgul_vein', 'Briques de Morgul veinées', 'morgul_bricks', 'morgul_vein_glow', 9)
    glow_block('morgul_vein_hot', 'Briques de Morgul incandescentes', 'morgul_bricks', 'morgul_vein_hot_glow', 13)
    glow_block('morgul_glow', 'Cristal vert de Morgul', 'morgul_glow_tex', None, 15)

    # --- bois doreal (elfes)
    btex('golden_log', log_side('#bdb8aa', 41)); btex('golden_log_top', log_top('#bdb8aa', '#ead79e', 42))
    block('golden_log', 'Bûche de doréal', 'pillar', copy='BIRCH_LOG', tool='axe', tags=['logs', 'logs_that_burn'], itags=['logs', 'logs_that_burn'])
    btex('golden_planks', planks('#e2c777', 43))
    block('golden_planks', 'Planches de doréal', copy='BIRCH_PLANKS', tool='axe', tags=['planks'], itags=['planks'])
    family('golden_planks', 'golden', 'doréal', ('stairs', 'slab', 'fence'), tool='axe')
    btex('golden_leaves', leaves('#dcb236', 44, accent='#fbe98a'))
    block('golden_leaves', 'Feuilles de doréal', 'leaves', sapling='golden_sapling')
    btex('autumn_orange_leaves', leaves('#d9731c', 45, accent='#f2a23a'))
    block('autumn_orange_leaves', "Feuilles d'automne orangées", 'leaves')
    btex('autumn_red_leaves', leaves('#a9391c', 46, accent='#d9601f'))
    block('autumn_red_leaves', "Feuilles d'automne rousses", 'leaves')
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

    # --- l'Arbre Blanc (Minas Tirith) : ecorce argentee, feuillage dense parseme de fleurs blanches
    import random as _rnd
    def _bloom(seed, density):                       # feuillage entierement blanc : neige et lune, aucun ton sombre
        r = _rnd.Random(seed * 7)
        im = leaves('#ffffff', seed, holes=0.0)
        pal = [(255, 255, 255), (250, 252, 255), (243, 247, 252), (234, 241, 249)]
        for y in range(16):
            for x in range(16):
                if r.random() < 0.035:
                    im.putpixel((x, y), (0, 0, 0, 0)); continue
                c = pal[0] if r.random() < 0.35 + density else r.choice(pal[1:])
                im.putpixel((x, y), c + (255,))
        return im
    btex('white_tree_log', log_side('#a9a79c', 71)); btex('white_tree_log_top', log_top('#a9a79c', '#e8e2c8', 72))
    block('white_tree_log', "Bûche de l'Arbre Blanc", 'pillar', copy='BIRCH_LOG', tool='axe', tags=['logs', 'logs_that_burn'], itags=['logs', 'logs_that_burn'])
    btex('white_tree_leaves', _bloom(73, 0.10))
    block('white_tree_leaves', "Feuilles de l'Arbre Blanc", 'leaves')
    btex('white_tree_bloom', _bloom(74, 0.34))
    block('white_tree_bloom', "Fleurs de l'Arbre Blanc", 'leaves')

    # --- chaume
    th = log_side('#c8a64c', 61).rotate(90)
    btex('thatch', th)
    block('thatch', 'Chaume', copy='HAY_BLOCK', tool='hoe')
    family('thatch', 'thatch', 'chaume', ('stairs', 'slab'), tool='hoe')

    # --- Rohan : bois patine du rempart, planches sculptees, torchis a colombages, gerbes de ble, terre battue
    btex('weathered_log', log_side('#6b6357', 81)); btex('weathered_log_top', log_top('#6b6357', '#b3a48a', 82))
    block('weathered_log', 'Bûche patinée', 'pillar', copy='OAK_LOG', tool='axe', tags=['logs', 'logs_that_burn'], itags=['logs', 'logs_that_burn'])
    btex('weathered_planks', planks('#8c8272', 83))
    block('weathered_planks', 'Planches patinées', copy='OAK_PLANKS', tool='axe', tags=['planks'], itags=['planks'])
    family('weathered_planks', 'patiné', 'bois patiné', ('stairs', 'slab', 'fence'), tool='axe')
    cp = planks('#46301f', 84)
    stamp(cp, ['g.g.g.g.g.g.g.g.', '.g.g.g.g.g.g.g.g'], {'g': '#d9a93a'}, 0, 4)
    stamp(cp, ['.g.g.g.g.g.g.g.g', 'g.g.g.g.g.g.g.g.'], {'g': '#d9a93a'}, 0, 10)
    btex('carved_planks', cp)
    block('carved_planks', 'Planches sculptées', copy='DARK_OAK_PLANKS', tool='axe', tags=['planks'], itags=['planks'])
    tp = stone('#d8cdb0', 85, var=0.35, cell=3, speck=0.3)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i), (7, i), (8, i)):
            tp.putpixel((x, y), rgb('#4d3826') + (255,))
        tp.putpixel((i, i), rgb('#5a4230') + (255,)); tp.putpixel((15 - i, i), rgb('#5a4230') + (255,))
    btex('timber_plaster', tp)
    block('timber_plaster', 'Torchis à colombages', copy='TERRACOTTA', tool='pickaxe')
    btex('rohan_plaster', stone('#d8cdb0', 86, var=0.4, cell=3, speck=0.35))
    block('rohan_plaster', 'Torchis', copy='TERRACOTTA', tool='pickaxe')
    sh = log_side('#dfbd52', 87)
    stamp(sh, ['bbbbbbbbbbbbbbbb', 'dddddddddddddddd'], {'b': '#8a622a', 'd': '#5e4220'}, 0, 7)
    btex('sheaf', sh); btex('sheaf_top', log_top('#c99a32', '#f0d37c', 88))
    block('sheaf', 'Gerbe de blé', 'pillar', copy='HAY_BLOCK', tool='hoe')
    btex('packed_earth', stone('#8a7358', 89, var=0.5, cell=3, speck=0.6))
    block('packed_earth', 'Terre battue', copy='COARSE_DIRT', tool='shovel')

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
    for st in ('white_bricks', 'limestone_bricks', 'dwarven_bricks', 'black_bricks'):
        shaped(f'{st}_stairs', ['#  ', '## ', '###'], {'#': st}, f'{st}_stairs', 4, 'building')
        shaped(f'{st}_slab', ['###'], {'#': st}, f'{st}_slab', 6, 'building')
        shaped(f'{st}_wall', ['###', '###'], {'#': st}, f'{st}_wall', 6)
        for k in ('stairs', 'slab', 'wall'):
            stonecut(st, f'{st}_{k}', 2 if k == 'slab' else 1)
    shaped('white_bricks', ['##', '##'], {'#': 'white_stone'}, 'white_bricks', 4, 'building')
    shaped('white_pillar', ['#', '#'], {'#': 'white_bricks'}, 'white_pillar', 2, 'building')
    shapeless('limestone_bricks', ['white_bricks', 'minecraft:sand'], 'limestone_bricks', 2, 'building')
    shaped('dwarven_bricks', ['##', '##'], {'#': 'minecraft:deepslate'}, 'dwarven_bricks', 4, 'building')
    shaped('dwarven_pillar', ['#', '#'], {'#': 'dwarven_bricks'}, 'dwarven_pillar', 2, 'building')
    shaped('dwarven_runes', [' # ', '#g#', ' # '], {'#': 'dwarven_bricks', 'g': 'minecraft:gold_nugget'}, 'dwarven_runes', 4, 'building')
    shaped('dwarven_lamp', [' # ', '#g#', ' # '], {'#': 'dwarven_bricks', 'g': 'minecraft:glowstone'}, 'dwarven_lamp', 4, 'building')
    shaped('black_bricks', ['##', '##'], {'#': 'volcanic_rock'}, 'black_bricks', 4, 'building')
    shaped('morgul_bricks', ['##', '##'], {'#': 'minecraft:deepslate_bricks'}, 'morgul_bricks', 4, 'building')
    shapeless('morgul_dark_bricks', ['morgul_bricks', 'minecraft:coal'], 'morgul_dark_bricks', 1, 'building')
    shapeless('morgul_pale_bricks', ['morgul_bricks', 'minecraft:bone_meal'], 'morgul_pale_bricks', 1, 'building')
    shapeless('morgul_detail', ['morgul_bricks', 'minecraft:green_dye'], 'morgul_detail', 1, 'building')
    shapeless('morgul_vein', ['morgul_bricks', 'minecraft:slime_ball'], 'morgul_vein', 1, 'building')
    shapeless('morgul_vein_hot', ['morgul_vein', 'minecraft:glowstone_dust'], 'morgul_vein_hot', 1, 'building')
    shaped('morgul_glow', [' # ', '#g#', ' # '], {'#': 'morgul_vein', 'g': 'minecraft:emerald'}, 'morgul_glow', 2, 'building')
    for st in ('morgul_bricks', 'morgul_dark_bricks', 'morgul_pale_bricks'):
        shaped(f'{st}_stairs', ['#  ', '## ', '###'], {'#': st}, f'{st}_stairs', 4, 'building')
        shaped(f'{st}_slab', ['###'], {'#': st}, f'{st}_slab', 6, 'building')
        shaped(f'{st}_wall', ['###', '###'], {'#': st}, f'{st}_wall', 6)
        for k in ('stairs', 'slab', 'wall'):
            stonecut(st, f'{st}_{k}', 2 if k == 'slab' else 1)
    shaped('thatch', ['##', '##'], {'#': 'minecraft:wheat'}, 'thatch', 2, 'building')
    shaped('thatch_stairs', ['#  ', '## ', '###'], {'#': 'thatch'}, 'thatch_stairs', 4, 'building')
    shaped('thatch_slab', ['###'], {'#': 'thatch'}, 'thatch_slab', 6, 'building')
    shaped('lunargent_block', ['###', '###', '###'], {'#': 'lunargent_ingot'}, 'lunargent_block')
    shapeless('lunargent_ingot_from_block', ['lunargent_block'], 'lunargent_ingot', 9)
    smelt('lunargent_ingot_from_raw', 'raw_lunargent', 'lunargent_ingot', 1.5)
    smelt('lunargent_ingot_from_ore', 'lunargent_ore', 'lunargent_ingot', 1.5)
