"""Objets : armes, armures, anneaux, batons, nourriture, divers (textures + enregistrement)."""
from genlib import *
from tex import *
from content import INGOT, RING
from PIL import Image
import random

HELMET = ['................', '................', '....oooooooo....', '...obbbbbbcco...', '..obbaaaaaacco..', '..obaaaaaaaaco..', '..oaaaaaaaaaao..', '..oattttttttao.'.replace(' ', 't')[:16],
          '..oaaoooooaaao..', '..oado....daao..', '..oado....daao..', '..odo......odo..', '..ooo......ooo..', '................', '................', '................']
CHEST = ['................', '..ooo......ooo..', '.obbco....obbco.', '.obaacoooocaaco.', '.oaaaaccccaaaao.', '.oaaaaaaaaaaaao.', '.oodaaaggaaadoo.', '...oaaaggaaao...', '...oaaaaaaaao...', '...oattttttao...',
         '...oaaaaaaaao...', '...oaaaaaaaao...', '...odaaaaaado...', '...oddddddddo...', '....oooooooo....', '................']
LEGS = ['................', '................', '...oooooooooo...', '...ottttttttto..'[:16], '...obbbbbbbbco..'[:16], '...oaaaaaaaaao..'[:16], '...oaaaoooaaao..'[:16], '...oaao...oaao..'[:16], '...oaao...oaao..'[:16], '...oaao...oaao..'[:16],
        '...oaao...oaao..'[:16], '...oado...oado..'[:16], '...oddo...oddo..'[:16], '...oooo...oooo..'[:16], '................', '................']
BOOTS = ['................', '................', '................', '................', '..oooo....oooo..', '..obco....obco..', '..oaao....oaao..', '..oaao....oaao..', '..otto....otto..', '..oaao....oaao..',
         '.ooaao...ooaao..', '.obaao...obaao..', '.oaaado..oaaado.', '.oddddo..oddddo.', '.oooooo..oooooo.', '................']

def armor_layer(name, base, trim, gem, crown=None, seed=0):
    from skins import Skin
    b = rgb(base); t = rgb(trim)
    for layer in (1, 2):
        s = Skin(seed + layer)
        s.im = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
        if layer == 1:
            s.box(0, 0, 8, 8, 8, b)                      # casque
            for x in range(8, 16):                        # visage ouvert
                for y in range(11, 16):
                    if x not in (11, 12) or y > 13: s.px(x, y, (0, 0, 0), 0)
            s.rect(8, 10, 8, 1, t); s.rect(0, 10, 8, 1, shade(t, 0.9)); s.rect(16, 10, 16, 1, shade(t, 0.9))
            if crown:
                c = rgb(crown)
                for x in range(0, 32):
                    if x % 2 == 0: s.px(x, 8, c)
                    s.px(x, 9, c)
                s.px(11, 9, rgb(gem)); s.px(12, 9, rgb(gem))
            s.box(16, 16, 8, 12, 4, b, grad=0.1)          # plastron
            s.rect(20, 20, 8, 1, t); s.rect(20, 29, 8, 1, t); s.rect(23, 23, 2, 2, rgb(gem)); s.rect(32, 20, 8, 1, shade(t, 0.9))
            for yy in range(22, 28):
                s.px(21, yy, shade(b, 1.25)); s.px(26, yy, shade(b, 0.75))
            s.box(40, 16, 4, 12, 4, b, grad=0.1)          # bras
            s.rect(40, 20, 16, 2, t); s.rect(40, 27, 16, 1, shade(t, 0.9))
            for x in range(0, 16):                        # bottes : bas des jambes seulement
                for y in range(27, 32): s.px(x, y, shade(b, 1 - 0.08 * (y - 27)))
                s.px(x, 27, t)
            s.rect(4, 16, 4, 4, (0, 0, 0), a=0); s.rect(8, 16, 4, 4, shade(b, 0.7))
        else:
            s.box(16, 16, 8, 12, 4, b)
            for x in range(16, 40):
                for y in range(16, 27): s.px(x, y, (0, 0, 0), 0)
            s.rect(16, 27, 24, 1, t)
            s.box(0, 16, 4, 12, 4, b, grad=0.12)
            s.rect(0, 20, 16, 1, t); s.rect(0, 26, 16, 1, shade(t, 0.85))
            for x in range(0, 16):
                for y in range(29, 32): s.px(x, y, (0, 0, 0), 0)
        save(s.im, f'{A}/textures/models/armor/{name}_layer_{layer}.png')

def armor_set(prefix, fr, mat, dur, base, trim, gem, crown=None, fem=False, tags=True, props='props()'):
    names = {'helmet': ('Casque', 'HELMET', HELMET, 'head_armor'), 'chestplate': ('Plastron', 'CHESTPLATE', CHEST, 'chest_armor'),
             'leggings': ('Jambières', 'LEGGINGS', LEGS, 'leg_armor'), 'boots': ('Bottes', 'BOOTS', BOOTS, 'foot_armor')}
    pal = matpal(base, trim=trim, gem=gem)
    for k, (frn, typ, art, tg) in names.items():
        item(f'{prefix}_{k}', f'{frn} {fr}', java=f'ModGear.armor(ModGear.{mat}, ArmorItem.Type.{typ}, {dur}, {props})', tex=sprite(art, pal), tags=[tg])
    armor_layer(prefix if prefix != 'elven_cloak' else 'cloak', base, trim, gem, crown)

def axe_icon(blade, shaft='#6b4a2a', double=False):
    pal = matpal(blade)
    head = ['.oooo..', 'obbbco.', 'obaaco.', 'oaaaao.', '.odaao.', '..odo..', '...o...']
    im = polearm(head, pal, shaft, ox=7, oy=0, shaft_from=(2, 14), shaft_to=(11, 5))
    if double: stamp(im, ['..o...', '.odo..', 'odaao.', 'oaabo.', '.oao..', '..o...'], pal, 9, 6)
    return im

def hammer_icon(head_c, shaft='#6b4a2a'):
    pal = matpal(head_c)
    return polearm(['..ooo...', '.obbco..', 'obaaaco.', 'oaaaaao.', '.odaaao.', '..oddo..', '...oo...'], pal, shaft, ox=7, oy=0, shaft_from=(2, 14), shaft_to=(10, 6))

def spear_icon(tip, shaft='#6b4a2a'):
    pal = matpal(tip)
    return polearm(['...oo', '..oco', '.obao', 'oaao.', 'ooo..'], pal, shaft, ox=11, oy=0, shaft_from=(1, 15), shaft_to=(12, 4))

def staff_icon(orb, wood='#5a3e24'):
    im = new(); sc = rgb(wood); o = rgb(orb)
    from tex import _line
    _line(im, 2, 14, 11, 5, sc); _line(im, 3, 14, 12, 5, shade(sc, 0.75))
    stamp(im, ['.ww.', 'wllw', 'wlow', '.ow.'], {'w': shade(sc, 1.2), 'l': mix(o, (255, 255, 255), 0.5), 'o': o}, 10, 1)
    return outline(im, (30, 22, 16))

def mace_icon(c, glow):
    pal = matpal(c, gem=glow)
    return polearm(['..o.o...', '.oaoao..', 'oagggao.', '.ogggo..', 'oagggao.', '.oaoao..', '..o.o...'], pal, '#2a2428', ox=7, oy=0, shaft_from=(2, 14), shaft_to=(10, 6))

def bow_icons(c, string='#e8e8e8'):
    out = []
    b = rgb(c)
    for pull in range(4):
        im = new()
        pts = [(13, 1), (14, 3), (14, 5), (13, 8), (11, 11), (8, 13), (5, 14), (3, 14), (1, 13)]
        for (x, y) in pts: im.putpixel((x, y), b + (255,))
        for (x, y) in pts[1:-1]: im.putpixel((x - 1, y - 1), shade(b, 1.25) + (255,)) if im.getpixel((x - 1, y - 1))[3] == 0 else None
        from tex import _line
        mid = (7 - pull * 1, 7 - pull * 1) if pull else (7, 7)
        mx = 7 - pull; my = 7 - pull
        if pull == 0:
            _line(im, 13, 1, 1, 13, rgb(string))
        else:
            mx, my = 7 - pull * 2 + 1, 7 - pull * 2 + 1
            _line(im, 13, 1, mx, my, rgb(string)); _line(im, mx, my, 1, 13, rgb(string))
            _line(im, mx, my, min(15, mx + 8), min(15, my + 8), (120, 90, 60))
            im.putpixel((min(15, mx + 8), min(15, my + 8)), (200, 200, 210, 255))
        out.append(outline(im, shade(b, 0.4)))
    return out

FOODS = {
    'waybread': ['................', '................', '................', '....oooooooo....', '...obbbbbbbbo...', '..obbcbbbbcbbo..', '..obbbbtbbbbbo..', '..oabbttttbbao..', '..oabbbttbbbao..', '..oaabbbbbbaao..', '...oaaaaaaaao...', '....oddddddo....', '.....oooooo.....', '................', '................', '................'],
    'mug': ['................', '................', '....wwwwwww.....', '...wwwwwwwww....', '...obbbbbbboooo.', '...obaaaaaao..o.', '...obattttao..o.', '...obaaaaaao..o.', '...obattttao..o.', '...obaaaaaaooo..', '...obaaaaaao....', '...odaaaaado....', '....ooooooo.....', '................', '................', '................'],
    'bowl': ['................', '................', '................', '................', '.....g..g.......', '....g..g..g.....', '..oooooooooooo..', '..otgtttgttgto..', '..oattttttttao..', '...oaaaaaaaao...', '...odaaaaaado...', '....oddddddo....', '.....oooooo.....', '................', '................', '................'],
    'cake': ['................', '................', '................', '.......gg.......', '....oooggooo....', '...occcccccco...', '..occbbbbbbcco..', '..obbbbbbbbbbo..', '..ottttttttto...'[:16], '..oaaaaaaaaaao..', '..oaaaaaaaaaao..', '..oddddddddddo..', '...oooooooooo...', '................', '................', '................'],
    'phial': ['................', '......oooo......', '......ottoo.....'[:16], '.......oo.......', '......ohho......', '.....ohcgho.....', '....ohccggho....', '....ohcgggho....', '....ohggggho....', '....ogggggao....', '.....ogggao.....', '......oooo......', '................', '................', '................', '................'],
    'orb': ['................', '................', '.....oooooo.....', '....ohhggggo....', '...ohhcgggggo...', '...ohcggggggo...', '...oggggggggo...', '...oggggggggo...', '...ogggggggao...', '....oggggaao....', '.....oooooo.....', '....ottttttto...'[:16], '...ouuuuuuuuo...', '....oooooooo....', '................', '................'],
    'horn': ['................', '................', '.........ooo....', '.......oobbco...', '......obbaaao...', '.....obaaaao....', '....otaaaao.....', '...obaaato......'[:16], '..obaaaao.......', '.obaaaoo........', '.oaaoo..........', '.ooo............', '................', '................', '................', '................'],
    'pelt': ['................', '..oo........oo..', '.oaaoooooooobao.', '.oabbbbbbbbbbao.', '..obbaaaaaabbo..', '..obaaaaaaaabo..', '..obaaddddaabo..', '..obaaddddaabo..', '..obaaaaaaaabo..', '..obbaaaaaabbo..', '.oabbbbbbbbbbao.', '.oaaoooooooobao.', '..oo........oo..', '................', '................', '................'],
    'dust': ['................', '................', '................', '.......c........', '.....c..b..c....', '....b..aab......', '...c.aabbaa.c...', '....aabccbaa....', '...aabbccbbaa...', '..oaaabbbbaaao..', '..odaaaaaaaado..', '...oddddddddo...', '....oooooooo....', '................', '................', '................'],
    'heart': ['................', '................', '...ooo...ooo....', '..occbo.obbbo...', '.ocgcbbobbbbbo..', '.ocggbbbbbbbbo..', '.obgbbbgbbbbao..', '.obbbbggbbbaao..', '..obbbbgbbaao...', '...obbbbbaao....', '....obbbaao.....', '.....obaao......', '......oao.......', '.......o........', '................', '................'],
    'scale': ['................', '................', '......oooo......', '.....obbbco.....', '....obbaaaco....', '....obaaaaao....', '...obaaaaaaao...', '...obaadaaaao...', '...oaaaddaaao...', '....oaaadaao....', '....odaaaado....', '.....oddddo.....', '......oooo......', '................', '................', '................'],
    'key': ['................', '................', '..oooo..........', '.otvvto.........', '.ov..voooooooo..', '.ov..vttttttto..'[:16], '.otvvtoooototo..', '..oooo....o.o...', '................', '................', '................', '................', '................', '................', '................', '................'],
    'cloak': ['................', '....oooooooo....', '...obbbttbbbo...', '..obbaattaabbo..', '..obaaaaaaaabo..', '..oaaaaaaaaaao..', '.obaaaaaaaaaabo.', '.oaaaaaaaaaaaao.', '.oaaaaaaaaaaaao.', '.oaaadaaaadaaao.', '.oaadaaaaaadaao.', '.oadaaaaaaaadao.', '.oddaaddddaaddo.', '..oo.oo..oo.oo..', '................', '................'],
    'coil': ['................', '................', '....oooooooo....', '...obbbbbbbbo...', '..obaooooooabo..', '..obo.oooo.obo..', '..oao.obbo.oao..', '..oao.oaao.oao..', '..oao.oooo.oao..', '..oaaooooooaao..', '...oaaaaaaaao...', '....odddddo.o...', '.....ooooo.oao..', '...........oao..', '............o...', '................'],
}

def build():
    P = 'props()'
    mat = lambda c, **k: matpal(c, **k)
    # ---- materiaux et butin
    item('troll_hide', 'Cuir de troll', tex=sprite(FOODS['pelt'], mat('#6d7868')))
    item('warg_pelt', 'Fourrure de loup de guerre', tex=sprite(FOODS['pelt'], mat('#423c38')))
    item('spider_silk', "Soie d'araignée géante", tex=sprite(FOODS['coil'], mat('#e6e6ee')))
    item('wight_dust', 'Poussière des tertres', tex=sprite(FOODS['dust'], mat('#9adcc8')))
    item('demon_heart', 'Cœur de braise', java="new MiscItems.Info(\"Encore brûlant. Convoité par les mages.\", props().fireResistant().rarity(Rarity.EPIC))", tex=sprite(FOODS['heart'], mat('#e0501a', gem='#ffd040')))
    item('dragon_scale', 'Écaille de dragon', java='new Item(props().fireResistant().rarity(Rarity.RARE))', tex=sprite(FOODS['scale'], mat('#8a251e')))
    item('rune_key', 'Clé runique', java="new MiscItems.Info(\"Ouvre les portes scellées des mines naines.\", props())", tex=sprite(FOODS['key'], mat('#8a8f98', trim='#c9a23a')))
    # ---- nourriture
    food = lambda n, s, extra='': f'new Item(props().food(new FoodProperties.Builder().nutrition({n}).saturationModifier({s}F){extra}.build()))'
    item('waybread', 'Pain de route', java=food(10, 1.4, '.fast()'), tex=sprite(FOODS['waybread'], mat('#e6cf8a', trim='#4f8a3a')))
    item('halfling_stew', 'Ragoût des semi-hommes', java=food(9, 0.9), tex=sprite(FOODS['bowl'], mat('#8a5a34', trim='#b0702a', gem='#d8d8d0')))
    item('honey_cake', 'Gâteau au miel', java=food(5, 0.7, '.fast()'), tex=sprite(FOODS['cake'], mat('#c98a3a', trim='#f2c040', gem='#c02020')))
    item('dwarf_ale', 'Bière naine', java='new MiscItems.Drink(false, props().stacksTo(16).food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).alwaysEdible().build()))',
         tex=sprite(FOODS['mug'], mat('#8a6a44', trim='#d8a030') | {'w': (245, 240, 225)}))
    item('living_sap', 'Sève vive', java='new MiscItems.Drink(true, props().stacksTo(8).rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).alwaysEdible().build()))',
         tex=sprite(FOODS['phial'], mat('#7a5a34', gem='#58c040', trim='#5a4020')))
    # ---- armes
    sw = lambda kind, tier, dmg, spd, reach=0, p=P: f'ModGear.special(SpecialSword.Kind.{kind}, ModGear.{tier}, {dmg}, {spd}F, {reach}, {p})'
    def weapon(id, fr, java, tex_im, tags=('swords',)):
        item(id, fr, java=java, model='handheld', tex=tex_im, tags=list(tags))
    weapon('lunargent_sword', 'Épée de lunargent', sw('NONE', 'T_LUNARGENT', 3, -2.4), sword('#cfe6f2', '#8fa8b8', gem='#58b0e0'))
    eb = sword('#dfe8ee', '#d8c070', grip='#3a5a3a', length=11)
    weapon('elven_blade', 'Lame elfique', sw('ELVEN', 'T_ELVEN', 3, -2.2), eb)
    itex('elven_blade_glow', sword('#8fd0ff', '#d8c070', grip='#3a5a3a', length=11, gem='#ffffff'))
    eg = sword('#dfe8ee', '#d8c070', grip='#6b4a2a', length=10, curved=True, wide=True)
    weapon('elven_glaive', 'Glaive elfique', sw('ELVEN', 'T_ELVEN', 5, -2.8, 1.0), eg)
    itex('elven_glaive_glow', sword('#8fd0ff', '#d8c070', grip='#6b4a2a', length=10, curved=True, wide=True))
    for wid in ('elven_blade', 'elven_glaive'):
        imodel(wid, {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{MOD}:item/{wid}'},
                     'overrides': [{'predicate': {f'{MOD}:glow': 1}, 'model': f'{MOD}:item/{wid}_glow'}]})
        imodel(wid + '_glow', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{MOD}:item/{wid}_glow'}})
    weapon('dwarven_axe', 'Hache de guerre naine', 'ModGear.axe(ModGear.T_DWARVEN, 6.0F, -3.0F, props())', axe_icon('#8a8f98', double=True), tags=('axes',))
    weapon('dwarven_hammer', 'Marteau de guerre nain', sw('HAMMER', 'T_DWARVEN', 7, -3.3), hammer_icon('#7a7f88'))
    weapon('orc_scimitar', 'Cimeterre orque', sw('NONE', 'T_ORC', 3, -2.4), sword('#4a474e', '#2a2428', grip='#3a2a20', length=10, curved=True))
    weapon('orc_cleaver', 'Fendoir orque', sw('NONE', 'T_ORC', 5, -3.0), sword('#55525a', '#2a2428', grip='#3a2a20', length=8, wide=True))
    weapon('rider_spear', 'Lance des cavaliers', sw('SPEAR', 'T_STEEL', 3, -2.6, 1.5), spear_icon('#b8bcc4'))
    weapon('halfling_dagger', 'Dague de semi-homme', sw('DAGGER', 'T_STEEL', 1, -1.6), sword('#c8ccd4', '#c9a23a', length=6))
    weapon('ranger_sword', 'Épée de rôdeur', sw('NONE', 'T_STEEL', 3, -2.4), sword('#b8bcc4', '#5a5a60', length=11))
    weapon('wight_blade', 'Lame des tertres', sw('WIGHT', 'T_STEEL', 3, -2.3), sword('#bfe8dc', '#7a9a8c', grip='#2a3a34', length=10, gem='#70ffd8'))
    weapon('troll_club', 'Massue de troll', sw('CLUB', 'T_ORC', 7, -3.4), hammer_icon('#6b4a2a', '#5a3e24'))
    weapon('ash_mace', 'Masse du Seigneur de Cendre', sw('MACE', 'T_LEGEND', 9, -3.2, 0, 'props().fireResistant().rarity(Rarity.EPIC)'), mace_icon('#2a2630', '#ff5a18'))
    weapon('kings_blade', 'Épée du roi elfe', sw('KING', 'T_LEGEND', 6, -2.2, 0, 'props().fireResistant().rarity(Rarity.EPIC)'), sword('#9fe0ff', '#e8c860', grip='#2a5a4a', length=11, gem='#40e080'))
    item('lunargent_pickaxe', 'Pioche de lunargent', java='ModGear.pickaxe(ModGear.T_LUNARGENT, props())', model='handheld', tags=['pickaxes'],
         tex=polearm(['.ooooooo', 'obbbbbcco', '.oo..oaao', '......oao', '.......o.'], matpal('#cfe6f2'), ox=6, oy=1, shaft_from=(2, 14), shaft_to=(10, 6)))
    bows = bow_icons('#d8c070')
    item('elven_bow', 'Arc elfique', java='new MiscItems.ElvenBow(props().durability(640))', tex=bows[0], model={
        'parent': 'minecraft:item/bow', 'textures': {'layer0': f'{MOD}:item/elven_bow'},
        'overrides': [{'predicate': {f'{MOD}:pulling': 1}, 'model': f'{MOD}:item/elven_bow_pulling_0'},
                      {'predicate': {f'{MOD}:pulling': 1, f'{MOD}:pull': 0.65}, 'model': f'{MOD}:item/elven_bow_pulling_1'},
                      {'predicate': {f'{MOD}:pulling': 1, f'{MOD}:pull': 0.9}, 'model': f'{MOD}:item/elven_bow_pulling_2'}]})
    for i in range(3):
        itex(f'elven_bow_pulling_{i}', bows[i + 1])
        imodel(f'elven_bow_pulling_{i}', {'parent': f'{MOD}:item/elven_bow', 'textures': {'layer0': f'{MOD}:item/elven_bow_pulling_{i}'}})
    # ---- batons
    for id, fr, kind, col in (('staff_fire', 'Bâton de feu', 'FIRE', '#ff6a18'), ('staff_light', 'Bâton de lumière', 'LIGHT', '#fff2a0'),
                              ('staff_storm', 'Bâton de foudre', 'STORM', '#9a7aff'), ('staff_healing', 'Bâton de soin', 'HEALING', '#60e090')):
        item(id, fr, java=f'new StaffItem(StaffItem.Kind.{kind}, props().durability(200).rarity(Rarity.RARE))', model='handheld', tex=staff_icon(col))
    # ---- armures
    armor_set('lunargent', 'de lunargent', 'LUNARGENT', 35, '#cfe6f2', '#8fb0c8', '#58b0e0')
    armor_set('orc', 'orque', 'ORC', 14, '#3d3a42', '#6a2018', '#c03020')
    armor_set('elven', 'elfique', 'ELVEN', 22, '#c8d0b0', '#d8c070', '#40c080')
    armor_set('dwarven', 'naine', 'DWARVEN', 30, '#6a6f7a', '#c9a23a', '#d04030')
    armor_set('ashlord', 'du Seigneur de Cendre', 'ASHLORD', 40, '#1c1a20', '#c02a10', '#ff5a18', crown='#2a2830', props='props().fireResistant().rarity(Rarity.EPIC)')
    armor_set('elfking', 'du roi elfe', 'ELFKING', 40, '#e6e2d0', '#d8b040', '#3090e0', crown='#e0c060', props='props().fireResistant().rarity(Rarity.EPIC)')
    item('elven_cloak', 'Cape elfique', java='ModGear.armor(ModGear.CLOAK, ArmorItem.Type.CHESTPLATE, 12, props())', tex=sprite(FOODS['cloak'], mat('#4a6a4a', trim='#c9c070')), tags=['chest_armor'])
    armor_layer('cloak', '#4a6a4a', '#3a5a3a', '#c9c070')
    # ---- anneaux
    rings = (('ash_ring', "Anneau de Cendre", 'ASH', '#e8c040', '#ff5a18', 'EPIC'), ('ring_waters', 'Anneau des Eaux', 'WATERS', '#d8dce4', '#40a0f0', 'RARE'),
             ('ring_winds', 'Anneau des Vents', 'WINDS', '#d8dce4', '#f0f0ff', 'RARE'), ('ring_embers', 'Anneau des Braises', 'EMBERS', '#e0b050', '#f04020', 'RARE'),
             ('ring_forge', 'Anneau de la Forge', 'FORGE', '#8a8f98', '#f0a030', 'RARE'), ('ring_hoard', 'Anneau du Trésor', 'HOARD', '#e8c040', '#40d060', 'RARE'),
             ('ring_kings', 'Anneau des Rois', 'KINGS', '#b08a4a', '#c02030', 'RARE'), ('ring_shadows', 'Anneau des Ombres', 'SHADOWS', '#5a5a66', '#8040c0', 'RARE'))
    for id, fr, kind, band, gem, rar in rings:
        item(id, fr, java=f'new RingItem(RingItem.Kind.{kind}, props().stacksTo(1).fireResistant().rarity(Rarity.{rar}))', tex=sprite(RING, matpal(band, gem=gem)))
    # ---- divers
    item('war_horn', 'Cor de guerre', java='new MiscItems.Horn(false, props().stacksTo(1))', tex=sprite(FOODS['horn'], mat('#d8cfb4', trim='#c9a23a')))
    item('dead_horn', 'Cor des morts', java='new MiscItems.Horn(true, props().stacksTo(1).rarity(Rarity.EPIC))', tex=sprite(FOODS['horn'], mat('#7ad8a0', trim='#3a9068')))
    item('light_phial', 'Fiole de lumière', java="new MiscItems.Info(\"En main : les araignées géantes vous fuient.\", props().stacksTo(1).rarity(Rarity.UNCOMMON))",
         tex=sprite(FOODS['phial'], mat('#d8e8f0', gem='#fff8c0', trim='#c9c9d0')))
    item('seeing_stone', 'Pierre de vision', java='new MiscItems.SeeingStone(props().stacksTo(1).rarity(Rarity.RARE))', tex=sprite(FOODS['orb'], mat('#1a1a2a', gem='#3a2a6a', trim='#8a8f98')))
    item('elven_rope', 'Corde elfique', java='new MiscItems.Rope(props())', tex=sprite(FOODS['coil'], mat('#b8b08a')))
    # ---- corde (bloc)
    rp = new()
    for y in range(16):
        for x in (6, 7, 8, 9):
            rp.putpixel((x, y), shade(rgb('#b8b08a'), 0.8 + 0.3 * ((x + y) % 3 == 0)) + (255,))
    btex('rope', rp)
    block('rope', 'Corde elfique', 'cross', java='new RopeBlock(BlockBehaviour.Properties.of().noCollission().strength(0.2F).sound(SoundType.WOOL).noOcclusion())',
          drop='elven_rope', tags=['climbable'], item=False)
    # ---- forge naine et bucher d'alarme
    fs = bricks('#4b505b', '#2a2d34', 81, var=0.09)
    stamp(fs, ['oooooooo', 'offyyffo', 'ofyyyyfo', 'offyyffo', 'oooooooo'], {'o': '#1c1e24', 'f': '#e0601a', 'y': '#ffc040'}, 4, 6)
    btex('dwarven_forge_side', fs)
    ft = framed('#3a3e48', 82)
    for y in range(4, 12):
        for x in range(4, 12): ft.putpixel((x, y), mix(rgb('#ffb030'), rgb('#c03010'), ((x * 7 + y * 3) % 5) / 5) + (255,))
    btex('dwarven_forge_top', ft)
    block('dwarven_forge', 'Forge naine', 'custom_cube', java='new ForgeBlock(copy(Blocks.DEEPSLATE_BRICKS).lightLevel(s -> 10))', tool='pickaxe',
          faces={'top': 'dwarven_forge_top', 'bottom': 'dwarven_bricks', 'side': 'dwarven_forge_side'})
    shaped('dwarven_forge', ['###', '#l#', '###'], {'#': 'dwarven_bricks', 'l': 'minecraft:lava_bucket'}, 'dwarven_forge')
    ps = log_side('#6b4a2a', 83).rotate(90)
    btex('alarm_pyre_side', ps)
    pt = log_top('#6b4a2a', '#9a7448', 84); btex('alarm_pyre_top', pt)
    pl = pt.copy()
    for y in range(3, 13):
        for x in range(3, 13):
            if (x + y) % 3: pl.putpixel((x, y), mix(rgb('#ffd040'), rgb('#e04010'), abs(x - 7.5) / 6) + (255,))
    btex('alarm_pyre_top_lit', pl)
    block('alarm_pyre', "Bûcher d'alarme", 'custom', java='new AlarmPyreBlock(copy(Blocks.OAK_PLANKS).lightLevel(s -> s.getValue(AlarmPyreBlock.LIT) ? 15 : 0))', tool='axe')
    for lit in ('', '_lit'):
        bmodel('alarm_pyre' + lit, 'minecraft:block/cube_bottom_top', {'top': T('alarm_pyre_top' + lit), 'bottom': T('alarm_pyre_top'), 'side': T('alarm_pyre_side')})
    bstate('alarm_pyre', {'variants': {'lit=false': {'model': T('alarm_pyre')}, 'lit=true': {'model': T('alarm_pyre_lit')}}})
    imodel('alarm_pyre', {'parent': T('alarm_pyre')})
    shaped('alarm_pyre', ['###', '#c#', '###'], {'#': '#minecraft:logs', 'c': 'minecraft:coal'}, 'alarm_pyre')
    # ---- recettes
    shaped('lunargent_sword', [' #', ' #', ' s'], {'#': 'lunargent_ingot', 's': 'minecraft:stick'}, 'lunargent_sword', cat='equipment')
    shaped('lunargent_pickaxe', ['###', ' s ', ' s '], {'#': 'lunargent_ingot', 's': 'minecraft:stick'}, 'lunargent_pickaxe', cat='equipment')
    for piece, pat in (('helmet', ['###', '# #']), ('chestplate', ['# #', '###', '###']), ('leggings', ['###', '# #', '# #']), ('boots', ['# #', '# #'])):
        shaped(f'lunargent_{piece}', pat, {'#': 'lunargent_ingot'}, f'lunargent_{piece}', cat='equipment')
        shaped(f'orc_{piece}', pat, {'#': 'black_steel_ingot'}, f'orc_{piece}', cat='equipment')
    shaped('orc_scimitar', [' #', '# ', 's '], {'#': 'black_steel_ingot', 's': 'minecraft:stick'}, 'orc_scimitar', cat='equipment')
    shaped('troll_club', [' ##', ' ##', 's  '], {'#': '#minecraft:logs', 's': 'troll_hide'}, 'troll_club', cat='equipment')
    shaped('staff_fire_heart', [' h', ' s', ' s'], {'h': 'demon_heart', 's': 'minecraft:blaze_rod'}, 'staff_fire', cat='equipment')
    shaped('dead_horn', ['ddd', 'dhd', 'ddd'], {'d': 'wight_dust', 'h': 'war_horn'}, 'dead_horn', cat='equipment')
    shaped('elven_rope', ['s', 's', 's'], {'s': 'spider_silk'}, 'elven_rope', 4)
    recipe('guide_book', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                          'ingredients': [{'item': 'minecraft:book'}, {'item': f'{MOD}:pipeweed'}],
                          'result': {'id': 'minecraft:written_book', 'count': 1, 'components': {'minecraft:custom_data': {'roa_guide': 1}}}})
    LANG['key.ringsofash.reputation'] = 'Menu de réputation'
    LANG['key.ringsofash.map'] = 'Carte de la Hautemarche'
    LANG['biome.ringsofash.rivieres_de_lave'] = 'Rivières de Lave'
    LANG['key.categories.ringsofash'] = 'Les Anneaux de Cendre'
