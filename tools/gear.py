"""Objets : armes, armures, anneaux, batons, nourriture, divers (textures + enregistrement)."""
from genlib import *
from tex import *
from content import INGOT, RING, GEM, COIN, RUNE
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
    if name in ('orc', 'dwarven', 'elven', 'rohan', 'gondor', 'elfking', 'dwarfking', 'knight', 'nazgul', 'mithril', 'feet') or name.endswith('_rusted'):
        return                                           # armures 3D : tools/armors3d.py
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

def armor_set(prefix, fr, mat, dur, base, trim, gem, crown=None, fem=False, tags=True, props='props()', adj=None):
    names = {'helmet': ('Casque', 'HELMET', HELMET, 'head_armor'), 'chestplate': ('Plastron', 'CHESTPLATE', CHEST, 'chest_armor'),
             'leggings': ('Jambières', 'LEGGINGS', LEGS, 'leg_armor'), 'boots': ('Bottes', 'BOOTS', BOOTS, 'foot_armor')}
    pal = matpal(base, trim=trim, gem=gem)
    for k, (frn, typ, art, tg) in names.items():
        item(f'{prefix}_{k}', f'{frn} {fr}' + (f' {adj[0 if k in ("helmet", "chestplate") else 1]}' if adj else ''), java=(f'ModGear.rustedArmor("{prefix}", ModGear.{mat}, ArmorItem.Type.{typ}, {dur}, {props})' if prefix.endswith('_rusted') else f'ModGear.armor(ModGear.{mat}, ArmorItem.Type.{typ}, {dur}, {props})'), tex=sprite(art, pal), tags=[tg])
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
    'necklace': ['................', '..oo........oo..', '.obbo......obbo.', '.oabo......oabo.', '..oabo....obao..', '..odabo..obado..', '...oabo..obao...', '...odabooabdo...', '....oabbbabo....', '....odaabado....', '.....oabbao.....', '......ohgo......', '.....ohgggo.....', '......ogso......', '.......oo.......', '................'],
    'feather': ['..........ooo...', '........oobbco..', '.......obbaaco..', '......obbaaaao..', '.....obbaaaado..', '....obaaaaaado..', '...obaaaaaadoo..', '..obaaaaaaado...', '..oaaaaaaadoo...', '.otaaaaaaoo.....', '.otooaaooo......', '.ot.oooo........', '.ot.............', 'oto.............', 'oo..............', '................'],
    'tusk': ['..........oo....', '.........obbo...', '........obbao...', '........obaao...', '.......obaaao...', '.......obaado...', '......obaaado...', '......obaaado...', '.....obaaaado...', '.....obaaaddo...', '....obaaaaddo...', '....otaaaaddo...', '....ottaaaado...', '....ottttooo....', '.....oooo.......', '................'],
    'key': ['................', '................', '..oooo..........', '.otvvto.........', '.ov..voooooooo..', '.ov..vttttttto..'[:16], '.otvvtoooototo..', '..oooo....o.o...', '................', '................', '................', '................', '................', '................', '................', '................'],
    'cloak': ['................', '....oooooooo....', '...obbbttbbbo...', '..obbaattaabbo..', '..obaaaaaaaabo..', '..oaaaaaaaaaao..', '.obaaaaaaaaaabo.', '.oaaaaaaaaaaaao.', '.oaaaaaaaaaaaao.', '.oaaadaaaadaaao.', '.oaadaaaaaadaao.', '.oadaaaaaaaadao.', '.oddaaddddaaddo.', '..oo.oo..oo.oo..', '................', '................'],
    'coil': ['................', '................', '....oooooooo....', '...obbbbbbbbo...', '..obaooooooabo..', '..obo.oooo.obo..', '..oao.obbo.oao..', '..oao.oaao.oao..', '..oao.oooo.oao..', '..oaaooooooaao..', '...oaaaaaaaao...', '....odddddo.o...', '.....ooooo.oao..', '...........oao..', '............o...', '................'],
}

def build():
    P = 'props()'
    mat = lambda c, **k: matpal(c, **k)
    # ---- materiaux et butin
    item('troll_hide', 'Cuir de troll', tex=sprite(FOODS['pelt'], mat('#6d7868')))
    item('warg_pelt', 'Fourrure de warg', tex=sprite(FOODS['pelt'], mat('#423c38')))
    item('spider_silk', "Soie d'araignée géante", tex=sprite(FOODS['coil'], mat('#e6e6ee')))
    item('wight_dust', 'Poussière des tertres', tex=sprite(FOODS['dust'], mat('#9adcc8')))
    item('demon_heart', 'Cœur de braise', java="new MiscItems.Info(\"Encore brûlant. Convoité par les mages.\", props().fireResistant().rarity(Rarity.EPIC))", tex=sprite(FOODS['heart'], mat('#e0501a', gem='#ffd040')))
    item('dragon_scale', 'Écaille de dragon', java='new Artifacts.Piece(Artifacts.Kind.SCALE, props().fireResistant().rarity(Rarity.RARE))', tex=sprite(FOODS['scale'], mat('#8a251e')))
    item('giant_eagle_feather', "Plume d'aigle géant", java='new Artifacts.Piece(Artifacts.Kind.FEATHER, props().rarity(Rarity.RARE))', tex=sprite(FOODS['feather'], mat('#c9b48a', trim='#7a5a30')))
    item('horn', 'Corne', java='new Artifacts.Piece(Artifacts.Kind.HORN, props().rarity(Rarity.RARE))', tex=sprite(FOODS['tusk'], mat('#e0d4b4', trim='#5a3e22')))
    for n, band, gem, rar in ((1, '#9aa0a8', '#c9d4e0', 'COMMON'), (2, '#e0b030', '#e84040', 'UNCOMMON'), (3, '#4ad0c8', '#a0f0ff', 'RARE'), (4, '#3a3440', '#ff5030', 'EPIC')):
        item(f'artifact_chain_{n}', f"Chaîne d'artefacts n°{n}", java=f'new Artifacts.Chain({n}, props().stacksTo(1).rarity(Rarity.{rar}))', tex=sprite(FOODS['necklace'], mat(band, trim='#c9a23a', gem=gem)))
    shapeless('artifact_chain_1', ['minecraft:chain', 'minecraft:gold_ingot', 'minecraft:gold_ingot'], 'artifact_chain_1')
    shapeless('artifact_chain_2', ['artifact_chain_1', 'minecraft:diamond', 'minecraft:diamond'], 'artifact_chain_2')
    shapeless('artifact_chain_3', ['artifact_chain_2', 'minecraft:netherite_scrap', 'minecraft:netherite_scrap'], 'artifact_chain_3')
    shapeless('artifact_chain_4', ['artifact_chain_3', 'minecraft:netherite_ingot', 'minecraft:nether_star'], 'artifact_chain_4')
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
    # ---- potions, runes et objets utilitaires (effets reels)
    fx = lambda n, t, a=0: f'new Elixirs.Fx(net.minecraft.world.effect.MobEffects.{n}, {t}, {a})'
    pot = lambda txt, heal, cure, *fxs, n=16: f'new Elixirs.Potion("{txt}", {heal}F, {str(cure).lower()}, props().stacksTo({n}), {", ".join(fxs) if fxs else "new Elixirs.Fx[0]"})'
    def phial(id, fr, gem, java, base='#7a5a34', trim='#5a4020'):
        item(id, fr, java=java, tex=sprite(FOODS['phial'], mat(base, gem=gem, trim=trim)))
    phial('ember_phial', 'Fiole de braise', '#ff7a1a', pot("Résistance au feu pendant 4 minutes.", 0, False, fx('FIRE_RESISTANCE', 4800)))
    phial('stone_phial', 'Élixir de pierre', '#9aa0a8', pot("Résistance II pendant 90 s, mais vous ralentit.", 0, False, fx('DAMAGE_RESISTANCE', 1800, 1), fx('MOVEMENT_SLOWDOWN', 1800, 0)))
    phial('night_phial', 'Philtre nocturne', '#3a50c8', pot("Vision nocturne pendant 5 minutes.", 0, False, fx('NIGHT_VISION', 6000)))
    phial('swift_phial', 'Philtre du vent', '#e8f0a0', pot("Vitesse II et Saut II pendant 2 minutes.", 0, False, fx('MOVEMENT_SPEED', 2400, 1), fx('JUMP', 2400, 1)))
    phial('healing_balm', 'Baume de guérison', '#e04a6a', pot("Soigne 4 cœurs et régénère II pendant 10 s.", 8, False, fx('REGENERATION', 200, 1)))
    phial('antidote', 'Antidote', '#58d890', pot("Purge poisons, malédictions et autres effets néfastes.", 0, True, fx('REGENERATION', 60, 0)))
    item('wind_feather', 'Plume de vent', java='new Elixirs.Feather(props().durability(24))', tex=sprite([
        '................', '..........ooooo.', '........ooabbbo.', '.......oabbbcbo.', '......oabbbbbo..', '.....oabbbbbo...', '....oaabbbbo....',
        '...oaaabbbo.....', '..oaaaabbo......', '..oaaaaoo.......', '.oddaaoo........', '.oddoo..........', 'oddo............', 'oo..............',
        '................', '................'], matpal('#dfe8f0')))
    item('recall_shard', 'Éclat de rappel', java='new Elixirs.Recall(props().stacksTo(8).rarity(Rarity.UNCOMMON))', tex=sprite(GEM, matpal('#9a6ae0')))
    item('smoke_powder', 'Poudre de fumée', java='new Elixirs.Smoke(props().stacksTo(16))', tex=sprite(FOODS['dust'], mat('#6a6a72')))
    item('haste_rune', 'Rune de hâte', java=f'new Elixirs.Rune("Hâte II pendant 4 minutes : creuse bien plus vite.", props().stacksTo(16), {fx("DIG_SPEED", 4800, 1)})',
         tex=sprite(RUNE, matpal('#7a7f88', trim='#ffd040')))
    for rid, parts, n in (('ember_phial', ['minecraft:glass_bottle', 'braisite', 'minecraft:blaze_powder'], 1),
                          ('stone_phial', ['minecraft:glass_bottle', 'minecraft:deepslate', 'raw_sombrefer'], 1),
                          ('night_phial', ['minecraft:glass_bottle', 'minecraft:golden_carrot', 'brumecristal'], 1),
                          ('swift_phial', ['minecraft:glass_bottle', 'minecraft:sugar', 'minecraft:feather'], 1),
                          ('healing_balm', ['minecraft:glass_bottle', 'minecraft:glistering_melon_slice', 'minecraft:honey_bottle'], 1),
                          ('antidote', ['minecraft:glass_bottle', 'minecraft:sweet_berries', 'minecraft:kelp'], 1),
                          ('wind_feather', ['minecraft:feather', 'minecraft:feather', 'lunargent_ingot'], 1),
                          ('recall_shard', ['brumecristal', 'minecraft:ender_pearl', 'lunargent_ingot'], 1),
                          ('smoke_powder', ['minecraft:gunpowder', 'minecraft:charcoal'], 2),
                          ('haste_rune', ['raw_orrune', 'minecraft:redstone', 'minecraft:glowstone_dust'], 1)):
        shapeless(rid, parts, rid, n)
    # ---- armes : on les remet une par une (texture dessinee a la main, donc pas de tex= ici)
    item('elven_sword', 'Épée elfique', model='handheld',
         java='ModGear.special(SpecialSword.Kind.ELVEN, ModGear.T_ELVEN, 3, -2.2F, 0, props())',
         tags=['swords'])
    # l'epee est longue : on l'agrandit en main (sinon elle fait la taille d'un couteau)
    EPEE_DISPLAY = {'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 9.0, 0.5], 'scale': [1.9, 1.9, 1.9]}, 'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 9.0, 0.5], 'scale': [1.9, 1.9, 1.9]}, 'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 5.0, 1.13], 'scale': [1.1, 1.1, 1.1]}, 'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 5.0, 1.13], 'scale': [1.1, 1.1, 1.1]}, 'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.8, 0.8, 0.8]}, 'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1.3, 1.3, 1.3]}, 'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}, 'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1.2, 1.2, 1.2]}}
    imodel('elven_sword', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{MOD}:item/elven_sword'},
                           'display': EPEE_DISPLAY,
                           'overrides': [{'predicate': {f'{MOD}:glow': 1}, 'model': f'{MOD}:item/elven_sword_glow'}]})
    imodel('elven_sword_glow', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'{MOD}:item/elven_sword_glow'},
                                'display': EPEE_DISPLAY})
    # ---- le reste des armes, arcs et batons : a refaire (voir git : tools/gear.py avant le commit a689a5f4)
    # ---- armures
    armor_set('lunargent', 'de lunargent', 'LUNARGENT', 35, '#cfe6f2', '#8fb0c8', '#58b0e0')
    armor_set('orc', 'orque', 'ORC', 14, '#3d3a42', '#6a2018', '#c03020')
    armor_set('elven', 'elfique', 'ELVEN', 22, '#c8d0b0', '#d8c070', '#40c080')
    armor_set('dwarven', 'naine', 'DWARVEN', 30, '#6a6f7a', '#c9a23a', '#d04030')
    armor_set('gondor', 'de la Garde de Gondor', 'GONDOR', 34, '#b8bec8', '#d6b254', '#e8e8f0', props='props().rarity(Rarity.RARE)')
    armor_set('rohan', 'de Rohan', 'ROHAN', 28, '#6a3022', '#d6a84c', '#a02020', props='props().rarity(Rarity.RARE)')
    armor_set('ashlord', 'du Seigneur de Cendre', 'ASHLORD', 40, '#1c1a20', '#c02a10', '#ff5a18', crown='#2a2830', props='props().fireResistant().rarity(Rarity.EPIC)')
    armor_set('dwarfking', 'du roi nain', 'DWARFKING', 36, '#6a6f7a', '#d6b040', '#d03040', crown='#e0c060', props='props().fireResistant().rarity(Rarity.EPIC)')
    armor_set('elfking', 'du roi elfe', 'ELFKING', 40, '#e6e2d0', '#d8b040', '#3090e0', crown='#e0c060', props='props().fireResistant().rarity(Rarity.EPIC)')
    armor_set('knight', "de chevalier", 'KNIGHT', 30, '#b4b8c0', '#8a2a24', '#d6b254', props='props().rarity(Rarity.RARE)')
    armor_set('nazgul', 'du Nazgûl', 'NAZGUL', 38, '#2a2a32', '#a0a4b4', '#c8f0e0', props='props().fireResistant().rarity(Rarity.EPIC)')
    armor_set('mithril', 'en mithril', 'MITHRIL', 38, '#c8d2e0', '#d8c070', '#a04a24', props='props().rarity(Rarity.EPIC)')
    # ---- versions rouillees (meme modele 3D, texture rouillee ; Gondor : aile gauche abimee)
    def _rust(h):
        c = rgb(h); r = (138, 70, 30)
        return '#%02x%02x%02x' % tuple(int(c[i] * .45 + r[i] * .55) for i in range(3))
    for pre, fr_, M, dur_, b_, t_, g_, pr_ in (('orc', 'orque', 'ORC', 14, '#3d3a42', '#6a2018', '#c03020', 'props()'),
                                               ('elven', 'elfique', 'ELVEN', 22, '#c8d0b0', '#d8c070', '#40c080', 'props()'),
                                               ('dwarven', 'naine', 'DWARVEN', 30, '#6a6f7a', '#c9a23a', '#d04030', 'props()'),
                                               ('gondor', 'de la Garde de Gondor', 'GONDOR', 34, '#b8bec8', '#d6b254', '#e8e8f0', 'props().rarity(Rarity.RARE)'),
                                               ('rohan', 'de Rohan', 'ROHAN', 28, '#6a3022', '#d6a84c', '#a02020', 'props().rarity(Rarity.RARE)'),
                                               ('dwarfking', 'du roi nain', 'DWARFKING', 36, '#6a6f7a', '#d6b040', '#d03040', 'props().fireResistant().rarity(Rarity.EPIC)'),
                                               ('elfking', 'du roi elfe', 'ELFKING', 40, '#e6e2d0', '#d8b040', '#3090e0', 'props().fireResistant().rarity(Rarity.EPIC)'),
                                               ('knight', 'de chevalier', 'KNIGHT', 30, '#b4b8c0', '#8a2a24', '#d6b254', 'props().rarity(Rarity.RARE)')):
        armor_set(f'{pre}_rusted', fr_, f'{M}_RUSTED', dur_, _rust(b_), _rust(t_), _rust(g_), props=pr_, adj=('rouillé', 'rouillée'))
    LANG[f'entity.{MOD}.invisible_armor_stand'] = "Support d'armure invisible"
    item('invisible_armor_stand', "Support d'armure invisible", java='new InvisibleStandItem(props())',
         tex=sprite(['................', '......oooo......', '.....oaaaao.....', '.....oaaaao.....', '......oooo......', '...oooooooooo...', '..oa.oaaaao.ao..', '..oa.oaaaao.ao..', '...o.oaaaao.o...', '......oaaao.....', '......oaaao.....', '.....oo..oo.....', '.....oa..ao.....', '.....oa..ao.....', '..oooooooooooo..', '................'],
                    {'o': (120, 190, 220, 255), 'a': (190, 230, 245, 140)}))
    item('feet_boots', 'Pieds', java='ModGear.armor(ModGear.FEET, ArmorItem.Type.BOOTS, 30, props())', tags=['foot_armor'],
         tex=sprite(['................', '................', '....oooooo......', '...oaabbaao.....', '...oaabbaao.....', '...oadabbao.....', '...oaaabbao.....', '...oaaabbao.....', '..oaaaaabbaoo...', '..oaaaaabbbaaoo.', '.oaabbbbbbbbbaao', '.oaabbbbbbbbbbao', '.oadnabnanbnabao', '..oddddddddddddo', '...oooooooooooo.', '................'],
                    {'o': (128, 78, 58, 255), 'a': (224, 172, 140, 255), 'b': (246, 206, 176, 255), 'd': (178, 120, 94, 255), 'n': (244, 208, 198, 255)}))
    item('elven_cloak', 'Cape elfique', java='ModGear.armor(ModGear.CLOAK, ArmorItem.Type.CHESTPLATE, 12, props())', tex=sprite(FOODS['cloak'], mat('#4a6a4a', trim='#c9c070')), tags=['chest_armor'])
    # la cape est dessinee par un calque 3D cote client : le calque d'armure du plastron reste invisible
    for lay in (1, 2): save(Image.new('RGBA', (64, 32), (0, 0, 0, 0)), f'{A}/textures/models/armor/cloak_layer_{lay}.png')
    def cloak_tex(name, base, trim, gem, seed):
        rnd = random.Random(seed)
        b = rgb(base); t = rgb(trim); g = rgb(gem)
        im = Image.new('RGBA', (64, 64))
        for y in range(64):
            for x in range(64):
                f = 1.0 + ((x + y) % 2) * 0.05 - 0.03 + rnd.uniform(-0.05, 0.05) + (0.06 if y % 4 == 0 else 0)
                im.putpixel((x, y), shade(b, f) + (255,))
        for x in range(14, 26):                           # ourlet de la cape (face exterieure du palier bas)
            im.putpixel((x, 20), shade(t, 0.8) + (255,)); im.putpixel((x, 21), t + (255,))
        for x in range(0, 34): im.putpixel((x, 32), t + (255,))      # bord de la collerette
        for x in range(0, 36): im.putpixel((x, 34), shade(t, 0.9) + (255,))   # visiere
        for y in range(45, 56):
            for x in (20, 21): im.putpixel((x, y), shade(t, 0.7) + (255,))
        for y in range(45, 48):                           # fermoir
            for x in range(46, 52): im.putpixel((x, y), g + (255,))
        save(im, f'{A}/textures/entity/cloak_{name}.png')
    cloak_tex('elven', '#4f6f50', '#c9c070', '#58e0a0', 11)
    cloak_tex('ranger', '#3a3e2c', '#6b4a2a', '#b08a40', 12)
    cloak_tex('ash', '#2a282e', '#e0501a', '#ff9a30', 13)
    item('ranger_cloak', 'Cape de rôdeur', java='ModGear.armor(ModGear.CLOAK, ArmorItem.Type.CHESTPLATE, 14, props())', tex=sprite(FOODS['cloak'], mat('#3a3e2c', trim='#6b4a2a')), tags=['chest_armor'])
    item('ash_cloak', 'Cape de cendre', java='ModGear.armor(ModGear.CLOAK, ArmorItem.Type.CHESTPLATE, 20, props().fireResistant().rarity(Rarity.EPIC))', tex=sprite(FOODS['cloak'], mat('#2a282e', trim='#e0501a')), tags=['chest_armor'])
    # ---- equipements du lore : bottes de semi-homme, casque de mineur nain
    item('halfling_boots', 'Bottes de semi-homme', java='ModGear.armor(ModGear.HALFLING, ArmorItem.Type.BOOTS, 18, props())', tex=sprite(BOOTS, matpal('#8a6a44', trim='#5a4020')), tags=['foot_armor'])
    armor_layer('halfling', '#8a6a44', '#5a4020', '#c9a23a')
    item('miner_helmet', 'Casque de mineur nain', java='ModGear.armor(ModGear.MINER, ArmorItem.Type.HELMET, 20, props())', tex=sprite(HELMET, matpal('#8a8f98', trim='#c9a23a', gem='#ffd040')), tags=['head_armor'])
    armor_layer('miner', '#8a8f98', '#c9a23a', '#ffd040', crown='#c9a23a')
    # ---- couronne et epee des rois de Valmoire (royaume des Hommes)
    gold = rgb('#e0c050'); pal = matpal('#e0c050', trim='#f4ecc8', gem='#3a8ae0')
    cr = new()
    for x in range(2, 14):
        for y in range(9, 12): cr.putpixel((x, y), pal['a'] + (255,))
    for x in (2, 5, 8, 11, 13):
        for y in range(5 if x in (5, 8, 11) else 6, 9): cr.putpixel((x, y), pal['b'] + (255,))
        cr.putpixel((x, 4 if x in (5, 8, 11) else 5), pal['h'] + (255,))
    for x in range(2, 14): cr.putpixel((x, 9), pal['c'] + (255,)); cr.putpixel((x, 11), pal['d'] + (255,))
    for x in (5, 8, 10): cr.putpixel((x, 10), pal['g'] + (255,))
    item('valmoire_crown', 'Couronne de Valmoire', java='ModGear.armor(ModGear.ROYAL, ArmorItem.Type.HELMET, 40, props().rarity(Rarity.EPIC))', tex=outline(cr, (60, 44, 16)), tags=['head_armor'])
    for lay in (1, 2):
        im = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
        if lay == 1:
            for x in range(0, 32):
                for y in (8, 9): im.putpixel((x, y), gold + (255,))
                if x % 2 == 0: im.putpixel((x, 10), rgb('#f4ecc8') + (255,))
            for x in (10, 11, 12, 13): im.putpixel((x, 9), rgb('#3a8ae0') + (255,)); im.putpixel((x, 8), rgb('#8ac0ff') + (255,))
            for k in range(8, 16):
                for y in (0, 7):
                    im.putpixel((k, y), gold + (255,))
                for y in range(0, 8):
                    if k in (8, 15): im.putpixel((k, y), gold + (255,))
        save(im, f'{A}/textures/models/armor/royal_layer_{lay}.png')
    # ---- coeur de Sylvgarde (5 % sur un Sylvgarde tue)
    item('sylvan_heart', 'Cœur de Sylvgarde', java='new SylvanHeart(props().stacksTo(16).rarity(Rarity.RARE))', tex=sprite(GEM, matpal('#6a8a3a', trim='#4b3b2b', gem='#a8e070')))
    LANG['effect.ringsofash.sylvan_form'] = 'Forme de Sylvgarde'
    LANG['effect.ringsofash.rooted'] = 'Enraciné'
    shaped('valmoire_crown', ['GDG', 'LLL'], {'G': 'minecraft:gold_ingot', 'D': 'minecraft:diamond', 'L': 'lunargent_ingot'}, 'valmoire_crown')
    shaped('valmoire_blade', [' L ', 'LDL', ' s '], {'L': 'lunargent_ingot', 'D': 'minecraft:diamond', 's': 'minecraft:gold_ingot'}, 'valmoire_blade')
    # ---- bouche de fumee : bloc invisible, intangible, incassable
    block('smoke_vent', 'Bouche de fumée', 'custom', drop='none',
          java='new SmokeVentBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.BLOCK))')
    bstate('smoke_vent', {'variants': {'': {'model': 'minecraft:block/air'}}})
    item_img = sprite(FOODS['dust'], mat('#6a6a72'))
    itex('smoke_vent', item_img)
    imodel('smoke_vent', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/smoke_vent'}})
    # ---- Oeil de Sauron : modele 3D (entite eye_of_sauron, dessinee par client/EyeOfSauronRenderer)
    import shutil as _sh
    _src = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets', 'eye_of_sauron')
    for _f, _dst in (('eye_of_sauron.geo.json', 'geo/eye_of_sauron.geo.json'), ('eye_of_sauron.animation.json', 'animations/eye_of_sauron.animation.json'),
                     ('eye_of_sauron.png', 'textures/entity/eye_of_sauron.png')):
        os.makedirs(os.path.dirname(os.path.join(A, _dst)), exist_ok=True)
        _sh.copyfile(os.path.join(_src, _f), os.path.join(A, _dst))
    shaped('ranger_cloak', ['PPP', 'LLL', 'L L'], {'P': 'warg_pelt', 'L': 'minecraft:leather'}, 'ranger_cloak')
    shaped('ash_cloak', ['DDD', '#H#', '# #'], {'D': 'dragon_scale', 'H': 'demon_heart', '#': 'black_steel_ingot'}, 'ash_cloak')
    shaped('halfling_boots', ['S S', 'L L'], {'S': 'spider_silk', 'L': 'minecraft:leather'}, 'halfling_boots')
    shaped('miner_helmet', ['###', '#l#'], {'#': 'minecraft:iron_ingot', 'l': 'minecraft:lantern'}, 'miner_helmet')
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
    block('dwarven_forge', 'Forge naine', 'custom', java='new ForgeBlock(copy(Blocks.DEEPSLATE_BRICKS).noOcclusion().strength(5.0F, 8.0F).lightLevel(s -> ForgeBlock.isCenter(s) ? 13 : 0))',
          tool='pickaxe', drop='none', tintlist=True, item_java='new ForgeItem(DWARVEN_FORGE.get(), props())')
    FT = {'stone': T('dwarven_stone'), 'bricks': T('dwarven_bricks'), 'pillar': T('dwarven_pillar'), 'pillar_top': T('dwarven_pillar_top'),
          'lava': 'minecraft:block/lava_still', 'front': T('dwarven_forge_side')}
    pil = {'up': '#pillar_top', 'down': '#pillar_top', 'north': '#pillar', 'south': '#pillar', 'west': '#pillar', 'east': '#pillar'}
    els = []
    els += tbox((-16, -16, -16), (32, -4, 32), '#stone')                       # socle
    els += tbox((-12, -4, -12), (28, 16, 28), '#bricks')                       # corps
    for (f_, t_) in (((-12, 16, -12), (28, 24, -8)), ((-12, 16, 24), (28, 24, 28)), ((-12, 16, -8), (-8, 24, 24)), ((24, 16, -8), (28, 24, 24))):
        els += tbox(f_, t_, '#stone')                                          # rebord du foyer
    els += tbox((-8, 16, -8), (24, 21, 24), '#lava', tint=False)               # bain de lave
    for cx in (-16, 24):
        for cz in (-16, 24):
            els += tbox((cx, -4, cz), (cx + 8, 22, cz + 8), '#pillar', per=pil)  # piliers d'angle
    els += tbox((-8, 24, 20), (24, 32, 28), '#bricks')                         # dosseret
    els += tbox((-4, 24, 12), (20, 32, 28), '#bricks')                         # cheminee
    els += tbox((0, 0, -12.5), (16, 12, -12), '#front', tint=False)            # bouche du foyer
    tmodel('dwarven_forge', els, FT, T('dwarven_stone'))
    tmodel('dwarven_forge_part', [], {}, T('dwarven_stone'))
    sv = {}
    for ox in range(3):
        for oy in range(3):
            for oz in range(3):
                sv[f'ox={ox},oy={oy},oz={oz}'] = {'model': T('dwarven_forge' if (ox, oy, oz) == (1, 1, 1) else 'dwarven_forge_part')}
    bstate('dwarven_forge', {'variants': sv})
    imodel('dwarven_forge', {'parent': T('dwarven_forge'), 'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, -1, 0], 'scale': [0.2, 0.2, 0.2]},
        'ground': {'translation': [0, 2, 0], 'scale': [0.1, 0.1, 0.1]},
        'fixed': {'scale': [0.18, 0.18, 0.18]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.08, 0.08, 0.08]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.1, 0.1, 0.1]}}})
    # trempe naine : enchantement a part, ajoute par la forge (6 niveaux)
    def temper(name, items, slots, effects, maxl, lang):
        wjson(f'{D}/enchantment/{name}.json', {
            'description': {'translate': f'enchantment.ringsofash.{name}'},
            'supported_items': items, 'weight': 1, 'max_level': maxl,
            'min_cost': {'base': 1, 'per_level_above_first': 10}, 'max_cost': {'base': 21, 'per_level_above_first': 10}, 'anvil_cost': 8,
            'slots': slots, 'effects': {'minecraft:attributes': effects}})
        LANG[f'enchantment.ringsofash.{name}'] = lang
    def lin(per):
        return {'type': 'minecraft:linear', 'base': per, 'per_level_above_first': per}
    def fix(per, mult):
        return {'type': 'minecraft:linear', 'base': per * mult, 'per_level_above_first': 0}
    def tool_fx(n, amt):
        return [{'attribute': 'minecraft:generic.attack_damage', 'id': f'ringsofash:{n}_damage', 'operation': 'add_value', 'amount': amt(1.5)},
                {'attribute': 'minecraft:player.mining_efficiency', 'id': f'ringsofash:{n}_mining', 'operation': 'add_value', 'amount': amt(2.0)}]
    def armor_fx(n, amt):
        return [{'attribute': 'minecraft:generic.armor', 'id': f'ringsofash:{n}', 'operation': 'add_value', 'amount': amt(1.5)}]
    # trempe naine : 6 niveaux (outils : degats/efficacite ; armures : +1,5 armure par niveau)
    temper('dwarven_temper', '#ringsofash:forgeable', ['mainhand'], tool_fx('dwarven_temper', lin), 6, 'Trempe naine')
    temper('dwarven_temper_armor', '#ringsofash:forge_armor', ['armor'], armor_fx('dwarven_temper_armor', lin), 6, 'Trempe naine')
    # trempe ancestrale : 1 % par trempe reussie, effet d'un niveau 7, durabilite -20 % (geree par la forge)
    temper('ancestral_temper', '#ringsofash:forgeable', ['mainhand'], tool_fx('ancestral_temper', lambda p: fix(p, 7)), 1, 'Trempe ancestrale')
    temper('ancestral_temper_armor', '#ringsofash:forge_armor', ['armor'], armor_fx('ancestral_temper_armor', lambda p: fix(p, 7)), 1, 'Trempe ancestrale')
    tag('item', 'forge_armor', '#minecraft:head_armor', '#minecraft:chest_armor', '#minecraft:leg_armor', '#minecraft:foot_armor', ns='ringsofash')
    tag('item', 'forgeable', '#minecraft:swords', '#minecraft:axes', '#minecraft:pickaxes', '#minecraft:shovels', '#minecraft:hoes', ns='ringsofash')
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
        shaped(f'rohan_{piece}', pat, {'#': 'minecraft:iron_ingot'}, f'rohan_{piece}', cat='equipment')
        shaped(f'gondor_{piece}', pat, {'#': 'lunargent_ingot'}, f'gondor_{piece}', cat='equipment')
        shaped(f'knight_{piece}', pat, {'#': 'minecraft:iron_ingot'}, f'knight_{piece}', cat='equipment')
        shaped(f'nazgul_{piece}', pat, {'#': 'black_steel_ingot'}, f'nazgul_{piece}', cat='equipment')
        shaped(f'mithril_{piece}', pat, {'#': 'lunargent_ingot'}, f'mithril_{piece}', cat='equipment')
    shaped('troll_club', [' ##', ' ##', 's  '], {'#': '#minecraft:logs', 's': 'troll_hide'}, 'troll_club', cat='equipment')
    shaped('staff_fire_heart', [' h', ' s', ' s'], {'h': 'demon_heart', 's': 'minecraft:blaze_rod'}, 'staff_fire', cat='equipment')
    shaped('dead_horn', ['ddd', 'dhd', 'ddd'], {'d': 'wight_dust', 'h': 'war_horn'}, 'dead_horn', cat='equipment')
    shaped('elven_rope', ['s', 's', 's'], {'s': 'spider_silk'}, 'elven_rope', 4)
    recipe('guide_book', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                          'ingredients': [{'item': 'minecraft:book'}, {'item': f'{MOD}:pipeweed'}],
                          'result': {'id': 'minecraft:written_book', 'count': 1, 'components': {'minecraft:custom_data': {'roa_guide': 1}}}})
    LANG['key.ringsofash.reputation'] = 'Menu de réputation'
    LANG['key.ringsofash.map'] = 'Carte de la Hautemarche'
    LANG['key.ringsofash.descend'] = 'Descendre en vol (dragon)'
    LANG['key.ringsofash.rings'] = "Onglet d'anneaux"
    LANG['key.ringsofash.skills'] = "Compétences (métiers)"
    LANG['entity.ringsofash.giant_arrow'] = 'Flèche géante'
    LANG['key.ringsofash.special'] = "Attaque spéciale (guerrier)"
    LANG['biome.ringsofash.rivieres_de_lave'] = 'Rivières de Lave'
    LANG['key.categories.ringsofash'] = 'Les Anneaux de Cendre'
