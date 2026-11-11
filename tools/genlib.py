#!/usr/bin/env python3
"""Generateur principal : ressources (assets + data) et registre Java des blocs/objets."""
import json, os, shutil, sys
sys.path.insert(0, os.path.dirname(__file__))
from tex import *

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src/main/resources')
MOD = 'ringsofash'
A = os.path.join(RES, 'assets', MOD)
D = os.path.join(RES, 'data', MOD)
DMC = os.path.join(RES, 'data', 'minecraft')
JAVA_GEN = os.path.join(ROOT, 'src/main/java/net/ringsofash/gen')

LANG = {}
TAGS = {}       # (registry, ns, name) -> [values]
BLOCKS = []     # dicts
ITEMS = []      # dicts

def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, separators=(',', ':'))

def tag(reg, name, *vals, ns='minecraft'):
    TAGS.setdefault((reg, ns, name), [])
    for v in vals:
        if v not in TAGS[(reg, ns, name)]: TAGS[(reg, ns, name)].append(v)

def mid(x): return x if ':' in x else f'{MOD}:{x}'

def btex(name, im): save(im, f'{A}/textures/block/{name}.png')
def itex(name, im): save(im, f'{A}/textures/item/{name}.png')

# ------------------------------------------------------------------ blocs
def block(id, fr, kind='cube', **kw):
    b = dict(id=id, fr=fr, kind=kind, **kw)
    BLOCKS.append(b)
    LANG[f'block.{MOD}.{id}'] = fr
    return b

def bmodel(name, parent, textures, render=None):
    m = {'parent': parent, 'textures': textures}
    if render: m['render_type'] = render
    wjson(f'{A}/models/block/{name}.json', m)

def bstate(id, obj): wjson(f'{A}/blockstates/{id}.json', obj)
def imodel(id, obj): wjson(f'{A}/models/item/{id}.json', obj)
def T(n): return f'{MOD}:block/{n}'

def stairs_state(id):
    v = {}
    base = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for f, b in base.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = id if shape == 'straight' else id + ('_inner' if 'inner' in shape else '_outer')
                if half == 'bottom':
                    y = b if shape in ('straight',) or shape.endswith('right') else (b - 90) % 360
                else:
                    y = b if shape == 'straight' or shape.endswith('left') else (b + 90) % 360
                e = {'model': f'{MOD}:block/{model}'}
                if half == 'top': e['x'] = 180
                if y: e['y'] = y
                if 'x' in e or 'y' in e: e['uvlock'] = True
                v[f'facing={f},half={half},shape={shape}'] = e
    bstate(id, {'variants': v})

def gen_block_assets(b):
    id, kind = b['id'], b['kind']
    tx = b.get('tex', id)       # nom de texture principal
    if kind in ('cube', 'ore', 'custom_cube'):
        faces = b.get('faces')
        if faces:
            bmodel(id, 'minecraft:block/cube_bottom_top', {k: T(v) for k, v in faces.items()})
        else:
            bmodel(id, 'minecraft:block/cube_all', {'all': T(tx)}, b.get('render'))
        bstate(id, {'variants': {'': {'model': T(id)}}})
        imodel(id, {'parent': T(id)})
    elif kind == 'pillar':
        bmodel(id, 'minecraft:block/cube_column', {'end': T(tx + '_top'), 'side': T(tx)})
        bmodel(id + '_horizontal', 'minecraft:block/cube_column_horizontal', {'end': T(tx + '_top'), 'side': T(tx)})
        bstate(id, {'variants': {'axis=y': {'model': T(id)},
                                 'axis=z': {'model': T(id + '_horizontal'), 'x': 90},
                                 'axis=x': {'model': T(id + '_horizontal'), 'x': 90, 'y': 90}}})
        imodel(id, {'parent': T(id)})
    elif kind == 'leaves':
        bmodel(id, 'minecraft:block/cube_all', {'all': T(tx)}, 'minecraft:cutout_mipped')
        bstate(id, {'variants': {'': {'model': T(id)}}})
        imodel(id, {'parent': T(id)})
    elif kind == 'stairs':
        t = {'bottom': T(tx), 'top': T(tx), 'side': T(tx)}
        bmodel(id, 'minecraft:block/stairs', t)
        bmodel(id + '_inner', 'minecraft:block/inner_stairs', t)
        bmodel(id + '_outer', 'minecraft:block/outer_stairs', t)
        stairs_state(id)
        imodel(id, {'parent': T(id)})
    elif kind == 'slab':
        t = {'bottom': T(tx), 'top': T(tx), 'side': T(tx)}
        bmodel(id, 'minecraft:block/slab', t)
        bmodel(id + '_top', 'minecraft:block/slab_top', t)
        bstate(id, {'variants': {'type=bottom': {'model': T(id)}, 'type=top': {'model': T(id + '_top')},
                                 'type=double': {'model': T(b['base'])}}})
        imodel(id, {'parent': T(id)})
    elif kind == 'wall':
        bmodel(id + '_post', 'minecraft:block/template_wall_post', {'wall': T(tx)})
        bmodel(id + '_side', 'minecraft:block/template_wall_side', {'wall': T(tx)})
        bmodel(id + '_side_tall', 'minecraft:block/template_wall_side_tall', {'wall': T(tx)})
        bmodel(id + '_inventory', 'minecraft:block/wall_inventory', {'wall': T(tx)})
        mp = [{'when': {'up': 'true'}, 'apply': {'model': T(id + '_post')}}]
        for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for h, suf in (('low', '_side'), ('tall', '_side_tall')):
                ap = {'model': T(id + suf), 'uvlock': True}
                if y: ap['y'] = y
                mp.append({'when': {d: h}, 'apply': ap})
        bstate(id, {'multipart': mp})
        imodel(id, {'parent': T(id + '_inventory')})
    elif kind == 'fence':
        bmodel(id + '_post', 'minecraft:block/fence_post', {'texture': T(tx)})
        bmodel(id + '_side', 'minecraft:block/fence_side', {'texture': T(tx)})
        bmodel(id + '_inventory', 'minecraft:block/fence_inventory', {'texture': T(tx)})
        mp = [{'apply': {'model': T(id + '_post')}}]
        for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            ap = {'model': T(id + '_side'), 'uvlock': True}
            if y: ap['y'] = y
            mp.append({'when': {d: 'true'}, 'apply': ap})
        bstate(id, {'multipart': mp})
        imodel(id, {'parent': T(id + '_inventory')})
    elif kind in ('cross', 'sapling'):
        bmodel(id, 'minecraft:block/cross', {'cross': T(tx)}, 'minecraft:cutout')
        bstate(id, {'variants': {'': {'model': T(id)}}})
        imodel(id, {'parent': 'minecraft:item/generated', 'textures': {'layer0': T(tx)}})
    elif kind == 'custom':
        pass  # assets fournis par l'appelant
    else:
        raise ValueError(kind)

def self_drop(id, cond=True):
    pool = {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': mid(id)}]}
    if cond: pool['conditions'] = [{'condition': 'minecraft:survives_explosion'}]
    return {'type': 'minecraft:block', 'pools': [pool]}

SILK = {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
    {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}

def gen_block_data(b):
    id, kind = b['id'], b['kind']
    drop = b.get('drop', 'self')
    lt = None
    if drop == 'none': lt = None
    elif kind == 'slab':
        lt = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': mid(id),
              'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [
                  {'condition': 'minecraft:block_state_property', 'block': mid(id), 'properties': {'type': 'double'}}]},
                  {'function': 'minecraft:explosion_decay'}]}]}]}
    elif kind == 'ore':
        lt = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': mid(id), 'conditions': [SILK]},
            {'type': 'minecraft:item', 'name': mid(b['ore_drop']), 'functions': [
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                {'function': 'minecraft:explosion_decay'}]}]}]}]}
    elif kind == 'leaves':
        shears = {'condition': 'minecraft:any_of', 'terms': [
            {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}}, SILK]}
        ch = [{'type': 'minecraft:item', 'name': mid(id), 'conditions': [shears]}]
        if b.get('sapling'):
            ch.append({'type': 'minecraft:item', 'name': mid(b['sapling']), 'conditions': [
                {'condition': 'minecraft:survives_explosion'}, {'condition': 'minecraft:random_chance', 'chance': 0.05}]})
        else:
            ch.append({'type': 'minecraft:item', 'name': 'minecraft:stick', 'conditions': [
                {'condition': 'minecraft:random_chance', 'chance': 0.04}]})
        lt = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': ch}]}]}
    elif isinstance(drop, dict): lt = drop
    elif drop == 'self': lt = self_drop(id)
    else: lt = self_drop(drop)
    if lt: wjson(f'{D}/loot_table/blocks/{id}.json', lt)
    tool = b.get('tool')
    if tool: tag('block', f'mineable/{tool}', mid(id))
    if b.get('level'): tag('block', f'needs_{b["level"]}_tool', mid(id))
    for t in b.get('tags', []): tag('block', t, mid(id))
    for t in b.get('itags', []): tag('item', t, mid(id))
    if kind == 'stairs': tag('block', 'stairs', mid(id)); tag('item', 'stairs', mid(id))
    if kind == 'slab': tag('block', 'slabs', mid(id)); tag('item', 'slabs', mid(id))
    if kind == 'wall': tag('block', 'walls', mid(id)); tag('item', 'walls', mid(id))
    if kind == 'fence':
        tag('block', 'fences', mid(id)); tag('block', 'wooden_fences', mid(id)); tag('item', 'fences', mid(id)); tag('item', 'wooden_fences', mid(id))
    if kind == 'leaves': tag('block', 'leaves', mid(id)); tag('item', 'leaves', mid(id)); tag('block', 'mineable/hoe', mid(id))
    if kind == 'sapling': tag('block', 'saplings', mid(id)); tag('item', 'saplings', mid(id))

JAVA_KIND = {
    'cube': 'new Block(copy(Blocks.{copy}){extra})',
    'custom_cube': None,
    'pillar': 'new RotatedPillarBlock(copy(Blocks.{copy}){extra})',
    'leaves': 'new LeavesBlock(copy(Blocks.OAK_LEAVES){extra})',
    'stairs': 'new StairBlock({BASE}.get().defaultBlockState(), copy({BASE}.get()))',
    'slab': 'new SlabBlock(copy({BASE}.get()))',
    'wall': 'new WallBlock(copy({BASE}.get()).forceSolidOn())',
    'fence': 'new FenceBlock(copy({BASE}.get()))',
    'ore': 'new DropExperienceBlock(UniformInt.of(3, 7), copy(Blocks.{copy}){extra})',
}

def block_java(b):
    if b.get('java'): return b['java']
    t = JAVA_KIND[b['kind']]
    return t.format(copy=b.get('copy', 'STONE'), extra=b.get('extra', ''), BASE=(b.get('base') or '').upper())

def family(base_id, fr_base, fr_plural_fem, kinds=('stairs', 'slab', 'wall'), tool='pickaxe', tags=()):
    names = {'stairs': ('Escalier en ' + fr_plural_fem), 'slab': ('Dalle en ' + fr_plural_fem),
             'wall': ('Muret en ' + fr_plural_fem), 'fence': ('Barrière en ' + fr_plural_fem)}
    for k in kinds:
        block(f'{base_id}_{k}', names[k], k, base=base_id, tex=base_id, tool=tool, tags=list(tags))

# ------------------------------------------------------------------ objets
def item(id, fr, java='new Item(props())', model='generated', tex=None, tags=(), tex_name=None):
    it = dict(id=id, fr=fr, java=java, model=model)
    ITEMS.append(it)
    LANG[f'item.{MOD}.{id}'] = fr
    if tex is not None: itex(tex_name or id, tex)
    if model in ('generated', 'handheld'):
        imodel(id, {'parent': f'minecraft:item/{model}', 'textures': {'layer0': f'{MOD}:item/{tex_name or id}'}})
    elif model == 'egg':
        imodel(id, {'parent': 'minecraft:item/template_spawn_egg'})
    elif isinstance(model, dict):
        imodel(id, model)
    for t in tags: tag('item', t, mid(id))
    return it

def recipe(name, obj): wjson(f'{D}/recipe/{name}.json', obj)

def shaped(name, pattern, key, result, count=1, cat='misc'):
    k = {}
    for ch, v in key.items():
        k[ch] = {'tag': v[1:]} if v.startswith('#') else {'item': mid(v) if ':' not in v else v}
    recipe(name, {'type': 'minecraft:crafting_shaped', 'category': cat, 'pattern': pattern, 'key': k,
                  'result': {'id': mid(result), 'count': count}})

def shapeless(name, ings, result, count=1, cat='misc'):
    recipe(name, {'type': 'minecraft:crafting_shapeless', 'category': cat,
                  'ingredients': [({'tag': v[1:]} if v.startswith('#') else {'item': mid(v) if ':' not in v else v}) for v in ings],
                  'result': {'id': mid(result), 'count': count}})

def smelt(name, ing, result, xp=0.7, blast=True):
    for typ, t in (('smelting', 200),) + ((('blasting', 100),) if blast else ()):
        recipe(f'{name}_{typ}', {'type': f'minecraft:{typ}', 'category': 'misc', 'ingredient': {'item': mid(ing)},
                                 'result': {'id': mid(result)}, 'experience': xp, 'cookingtime': t})

def stonecut(base, result, count=1):
    recipe(f'{result}_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': {'item': mid(base)},
                                      'result': {'id': mid(result), 'count': count}})

# ------------------------------------------------------------------ ecriture finale
def write_java():
    os.makedirs(JAVA_GEN, exist_ok=True)
    L = ['// FICHIER GENERE par tools/gen.py - ne pas modifier a la main', 'package net.ringsofash.gen;', '',
         'import net.minecraft.core.registries.Registries;', 'import net.minecraft.resources.ResourceKey;',
         'import net.minecraft.resources.ResourceLocation;', 'import net.minecraft.util.valueproviders.UniformInt;',
         'import net.minecraft.world.effect.MobEffects;', 'import net.minecraft.world.item.*;',
         'import net.minecraft.world.level.block.*;', 'import net.minecraft.world.level.block.grower.TreeGrower;',
         'import net.minecraft.world.level.block.state.BlockBehaviour;', 'import net.minecraft.world.level.material.MapColor;',
         'import net.minecraft.world.level.material.PushReaction;',
         'import net.neoforged.neoforge.registries.DeferredBlock;', 'import net.neoforged.neoforge.registries.DeferredItem;',
         'import net.neoforged.neoforge.registries.DeferredRegister;', 'import net.ringsofash.block.*;',
         'import net.ringsofash.item.*;', 'import net.ringsofash.entity.ModEntities;', 'import net.minecraft.world.food.FoodProperties;',
         'import net.neoforged.neoforge.common.DeferredSpawnEggItem;', 'import java.util.Optional;', '',
         'public final class Reg {',
         f'    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("{MOD}");',
         f'    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("{MOD}");',
         '    static BlockBehaviour.Properties copy(Block b) { return BlockBehaviour.Properties.ofFullCopy(b); }',
         '    static Item.Properties props() { return new Item.Properties(); }',
         f'    static ResourceLocation id(String p) {{ return ResourceLocation.fromNamespaceAndPath("{MOD}", p); }}', '']
    for b in BLOCKS:
        L.append(f'    public static final DeferredBlock<Block> {b["id"].upper()} = BLOCKS.register("{b["id"]}", () -> {block_java(b)});')
    L.append('')
    for b in BLOCKS:
        if b.get('item', True):
            L.append(f'    public static final DeferredItem<BlockItem> BI_{b["id"].upper()} = ITEMS.registerSimpleBlockItem("{b["id"]}", {b["id"].upper()});')
    L.append('')
    for it in ITEMS:
        L.append(f'    public static final DeferredItem<Item> {it["id"].upper()} = ITEMS.register("{it["id"]}", () -> {it["java"]});')
    L += ['    private Reg() {}', '}', '']
    with open(os.path.join(JAVA_GEN, 'Reg.java'), 'w', encoding='utf-8') as f: f.write('\n'.join(L))

def finish():
    for b in BLOCKS:
        gen_block_assets(b); gen_block_data(b)
    for (reg, ns, name), vals in TAGS.items():
        base = os.path.join(RES, 'data', ns, 'tags', reg, name + '.json')
        wjson(base, {'replace': False, 'values': vals})
    wjson(f'{A}/lang/fr_fr.json', LANG)
    wjson(f'{A}/lang/en_us.json', LANG)
    for extra in ('fr_ca', 'fr_be', 'fr_ch'):
        pass
    write_java()

def clean():
    for p in (os.path.join(RES, 'assets'), os.path.join(RES, 'data')):
        shutil.rmtree(p, ignore_errors=True)

