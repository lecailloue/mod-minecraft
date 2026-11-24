"""Gros lot d'objets supplementaires : materiaux, butin, nourriture, potions, runes, armes, outils, objets de lore.
Utilise uniquement des classes Java deja presentes (Item, Info, Potion, Rune, ModGear.*, SpecialSword.Kind)."""
from genlib import *
from tex import *
from content import INGOT, RING, GEM, COIN, RAW, RUNE
from gear import FOODS, axe_icon, hammer_icon, spear_icon, mace_icon

SHOVEL = ['.oo.', 'obco', 'obao', 'oaao', '.odo']
HOE = ['.oooo.', 'obbbco', 'oaaado', '.oddo.']

def build():
    P = 'props()'
    mat = lambda c, **k: matpal(c, **k)
    # ---------------------------------------------------------------- materiaux
    item('orrune_ingot', "Lingot d'orrune", tex=sprite(INGOT, mat('#e8b838', trim='#fff0a0')))
    smelt('orrune_ingot', 'raw_orrune', 'orrune_ingot', 1.0)
    item('orrune_nugget', "Pépite d'orrune", tex=sprite(COIN, mat('#e8b838', trim='#fff0a0')))
    item('lunargent_nugget', 'Pépite de lunargent', tex=sprite(COIN, mat('#cfe6f2')))
    item('black_steel_nugget', "Pépite d'acier noir", tex=sprite(COIN, mat('#3d3a42')))
    for pre, ing in (('orrune', 'orrune_ingot'), ('lunargent', 'lunargent_ingot'), ('black_steel', 'black_steel_ingot')):
        shapeless(f'{pre}_nugget_from_ingot', [ing], f'{pre}_nugget', 9)
        shaped(f'{ing}_from_nuggets', ['###', '###', '###'], {'#': f'{pre}_nugget'}, ing)
    item('gondor_coin', 'Pièce de Gondor', tex=sprite(COIN, mat('#e0c860', trim='#fff4c0')))
    item('dwarven_coin', 'Pièce naine', tex=sprite(COIN, mat('#b08a4a', trim='#e0c070')))
    item('elven_coin', 'Pièce elfique', tex=sprite(COIN, mat('#dfe8ee', trim='#8fd0ff')))
    item('ash_shard', 'Éclat de cendre', java='new MiscItems.Info("Froid au toucher, malgré ses veines de braise.", props().fireResistant())',
         tex=sprite(GEM, mat('#2a2630', gem='#ff5a18', trim='#c02a10')))
    item('ember_core', 'Noyau de braise', java='new MiscItems.Info("Palpite comme un cœur de forge.", props().fireResistant().rarity(Rarity.UNCOMMON))',
         tex=sprite(FOODS['orb'], mat('#6a2010', gem='#ff7a1a', trim='#3a3440')))
    item('wraith_essence', 'Essence de spectre', java='new MiscItems.Info("Un souffle glacé emprisonné.", props().rarity(Rarity.UNCOMMON))',
         tex=sprite(FOODS['phial'], mat('#d8e8f0', gem='#9afff0', trim='#8a9aa0')))
    item('spider_venom', "Venin d'araignée géante", tex=sprite(FOODS['phial'], mat('#5a4a3a', gem='#8ad030', trim='#3a3a2a')))
    item('nazgul_shroud', 'Linceul de Nazgûl', java='new MiscItems.Info("Il glace les os de qui le touche.", props().rarity(Rarity.RARE))',
         tex=sprite(FOODS['cloak'], mat('#2a2a32', trim='#a0a4b4')))
    # ---------------------------------------------------------------- butin des creatures
    item('orc_tooth', "Croc d'orque", tex=sprite(FOODS['tusk'], mat('#cfc6a0', trim='#4a3a2a')))
    item('warg_fang', 'Croc de warg', tex=sprite(FOODS['tusk'], mat('#e0dccc', trim='#3a3430')))
    item('goblin_ear', 'Oreille de gobelin', tex=sprite(FOODS['scale'], mat('#6f8a4a')))
    item('troll_tooth', 'Dent de troll', tex=sprite(FOODS['tusk'], mat('#b8b090', trim='#5a4a30')))
    item('boar_tusk', 'Défense de sanglier', tex=sprite(FOODS['tusk'], mat('#e8e0c8', trim='#6a5a3a')))
    item('mammoth_tusk', 'Défense de mûmak', java='new MiscItems.Info("Longue comme un homme et lourde comme une enclume.", props().rarity(Rarity.UNCOMMON))',
         tex=sprite(FOODS['tusk'], mat('#f0e8d0', trim='#8a7a5a')))
    item('stag_antler', 'Andouiller de cerf blanc', java='new MiscItems.Info("Blanc comme la neige des hauts cols.", props().rarity(Rarity.UNCOMMON))',
         tex=sprite(FOODS['horn'], mat('#f4f0e4', trim='#a8a090')))
    item('crow_feather', 'Plume de corbeau', tex=sprite(FOODS['feather'], mat('#26262e', trim='#16161c')))
    item('eagle_talon', "Serre d'aigle", tex=sprite(FOODS['tusk'], mat('#d8b84a', trim='#5a4020')))
    item('ancient_bone', 'Os ancien', tex=sprite(FOODS['tusk'], mat('#d8d2bc', trim='#6a6450')))
    # ---------------------------------------------------------------- nourriture
    food = lambda n, s, extra='': f'new Item(props().food(new FoodProperties.Builder().nutrition({n}).saturationModifier({s}F){extra}.build()))'
    item('lembas', 'Lembas', java=food(12, 1.6, '.fast()'), tex=sprite(FOODS['waybread'], mat('#f0e8c8', trim='#3f8a4a')))
    item('cram', 'Biscuit de route', java=food(6, 0.8), tex=sprite(FOODS['waybread'], mat('#b8905a', trim='#6a4a2a')))
    item('roast_boar', 'Sanglier rôti', java=food(10, 1.2), tex=sprite(FOODS['heart'], mat('#a05a30', gem='#e8b070')))
    item('smoked_venison', 'Venaison fumée', java=food(8, 1.1), tex=sprite(FOODS['heart'], mat('#7a4a30', gem='#c88a5a')))
    item('hobbit_pie', 'Tarte des semi-hommes', java=food(7, 0.8), tex=sprite(FOODS['cake'], mat('#d8a860', trim='#f4d890', gem='#c03030')))
    item('mushroom_soup', 'Soupe aux champignons', java=food(6, 0.7), tex=sprite(FOODS['bowl'], mat('#a07a50', trim='#c8a070', gem='#e8dcc0')))
    shapeless('lembas', ['minecraft:wheat', 'minecraft:wheat', 'minecraft:honey_bottle', 'minecraft:sugar'], 'lembas', 2)
    shapeless('cram', ['minecraft:wheat', 'minecraft:wheat', 'minecraft:egg'], 'cram', 3)
    shapeless('roast_boar', ['minecraft:cooked_porkchop', 'minecraft:baked_potato', 'minecraft:sweet_berries'], 'roast_boar')
    shapeless('smoked_venison', ['minecraft:cooked_beef', 'minecraft:charcoal', 'minecraft:sweet_berries'], 'smoked_venison')
    shapeless('hobbit_pie', ['minecraft:apple', 'minecraft:apple', 'minecraft:sugar', 'minecraft:egg', 'minecraft:wheat'], 'hobbit_pie')
    shapeless('mushroom_soup', ['minecraft:bowl', 'minecraft:brown_mushroom', 'minecraft:brown_mushroom', 'minecraft:baked_potato'], 'mushroom_soup')
    # ---------------------------------------------------------------- potions
    fx = lambda n, t, a=0: f'new Elixirs.Fx(net.minecraft.world.effect.MobEffects.{n}, {t}, {a})'
    pot = lambda txt, heal, cure, *fxs, n=16, p='': f'new Elixirs.Potion("{txt}", {heal}F, {str(cure).lower()}, props().stacksTo({n}){p}, {", ".join(fxs) if fxs else "new Elixirs.Fx[0]"})'
    def phial(id, fr, gem, java, parts, base='#7a5a34', trim='#5a4020', n=1):
        item(id, fr, java=java, tex=sprite(FOODS['phial'], mat(base, gem=gem, trim=trim)))
        shapeless(id, ['minecraft:glass_bottle'] + parts, id, n)
    phial('strength_phial', 'Philtre de force', '#c83a2a', pot("Force II pendant 90 s.", 0, False, fx('DAMAGE_BOOST', 1800, 1)),
          ['minecraft:blaze_powder', 'minecraft:golden_apple'])
    phial('water_phial', 'Philtre des profondeurs', '#3a8ad8', pot("Respiration aquatique pendant 5 minutes.", 0, False, fx('WATER_BREATHING', 6000)),
          ['minecraft:pufferfish', 'minecraft:kelp'])
    phial('shade_phial', "Voile d'ombre", '#4a4a6a', pot("Invisibilité pendant 2 minutes.", 0, False, fx('INVISIBILITY', 2400)),
          ['minecraft:fermented_spider_eye', 'brumecristal'])
    phial('feather_phial', 'Philtre de plume', '#f0f0f8', pot("Chute amortie pendant 3 minutes.", 0, False, fx('SLOW_FALLING', 3600)),
          ['minecraft:phantom_membrane', 'minecraft:feather'])
    phial('vigor_phial', 'Élixir de vigueur', '#e0a030', pot("Santé accrue et absorption pendant 3 minutes.", 0, False, fx('HEALTH_BOOST', 3600, 1), fx('ABSORPTION', 3600, 1)),
          ['minecraft:golden_carrot', 'minecraft:honey_bottle'])
    phial('miruvor', 'Miruvor', '#f0d890', pot("Cordial des elfes : soigne, rassasie et donne de l'allant.", 6, False, fx('SATURATION', 20, 1), fx('MOVEMENT_SPEED', 1200, 0), fx('REGENERATION', 200, 0), n=8, p='.rarity(Rarity.UNCOMMON)'),
          ['lembas', 'minecraft:honey_bottle'], base='#c8c0a0', trim='#e0c860')
    phial('elven_cordial', 'Cordial de Fondcombe', '#80e0c0', pot("Soigne 5 cœurs, régénère et protège.", 10, False, fx('REGENERATION', 300, 1), fx('ABSORPTION', 1200, 1), n=8, p='.rarity(Rarity.RARE)'),
          ['healing_balm', 'minecraft:golden_apple'], base='#a8b8a0', trim='#d8c070')
    phial('elven_wine', 'Vin elfique', '#a02850', pot("Réchauffe le cœur : régénération et un peu de vitesse.", 2, False, fx('REGENERATION', 160, 0), fx('MOVEMENT_SPEED', 600, 0)),
          ['minecraft:sweet_berries', 'minecraft:sweet_berries', 'minecraft:sugar'], base='#8a7a50', trim='#d8c070')
    # ---------------------------------------------------------------- herbes et runes
    item('athelas', 'Athelas', java=f'new Elixirs.Rune("Herbe des rois : régénération III pendant 15 s.", props().stacksTo(16).rarity(Rarity.UNCOMMON), {fx("REGENERATION", 300, 2)})',
         tex=sprite(FOODS['feather'], mat('#58a840', trim='#3a6a28')))
    shapeless('athelas', ['minecraft:fern', 'minecraft:fern', 'minecraft:dandelion'], 'athelas')
    runes = (('strength_rune', 'Rune de force', 'Force II pendant 3 minutes.', 'DAMAGE_BOOST', 3600, 1, '#c83a2a', 'minecraft:blaze_powder'),
             ('speed_rune', 'Rune de vélocité', 'Vitesse II pendant 3 minutes.', 'MOVEMENT_SPEED', 3600, 1, '#f0f0a0', 'minecraft:sugar'),
             ('guard_rune', 'Rune de garde', 'Résistance II pendant 2 minutes.', 'DAMAGE_RESISTANCE', 2400, 1, '#9aa0b0', 'minecraft:iron_ingot'),
             ('leap_rune', 'Rune de bond', 'Saut III pendant 3 minutes.', 'JUMP', 3600, 2, '#80e060', 'minecraft:slime_ball'),
             ('sight_rune', 'Rune de vision', 'Vision nocturne pendant 10 minutes.', 'NIGHT_VISION', 12000, 0, '#3a50c8', 'minecraft:golden_carrot'),
             ('tide_rune', 'Rune des marées', 'Respiration aquatique et grâce du dauphin, 5 minutes.', 'WATER_BREATHING', 6000, 0, '#3a8ad8', 'minecraft:prismarine_shard'),
             ('flame_rune', 'Rune de flamme', 'Résistance au feu pendant 8 minutes.', 'FIRE_RESISTANCE', 9600, 0, '#ff7a1a', 'minecraft:magma_cream'),
             ('life_rune', 'Rune de vie', 'Régénération II pendant 30 s.', 'REGENERATION', 600, 1, '#e04a6a', 'minecraft:glistering_melon_slice'),
             ('wind_rune', 'Rune du vent', 'Chute amortie pendant 5 minutes.', 'SLOW_FALLING', 6000, 0, '#e8f0f8', 'minecraft:feather'),
             ('luck_rune', 'Rune de fortune', 'Chance pendant 10 minutes.', 'LUCK', 12000, 0, '#40d060', 'minecraft:emerald'))
    for rid, fr, txt, eff, t, a, col, extra in runes:
        item(rid, fr, java=f'new Elixirs.Rune("{txt}", props().stacksTo(16), {fx(eff, t, a)})', tex=sprite(RUNE, mat('#7a7f88', trim=col)))
        shapeless(rid, ['raw_orrune', 'minecraft:redstone', extra], rid)
    # ---------------------------------------------------------------- objets de lore
    info = lambda txt, rar='COMMON', n=64: f'new MiscItems.Info("{txt}", props().stacksTo({n}).rarity(Rarity.{rar}))'
    item('mallorn_leaf', 'Feuille de mallorn', java=info("Elle ne se flétrit jamais."), tex=sprite(FOODS['feather'], mat('#e8d070', trim='#b89a30')))
    item('ancient_scroll', 'Parchemin ancien', java=info("Une écriture oubliée couvre ce parchemin.", 'UNCOMMON'), tex=sprite(FOODS['cloak'], mat('#d8c898', trim='#8a6a3a')))
    item('dwarven_ledger', 'Registre de la mine', java=info("Les comptes d'un chantier abandonné depuis longtemps.", 'UNCOMMON'), tex=sprite(FOODS['cake'], mat('#6a4a2a', trim='#c9a23a', gem='#d8d0b0')))
    item('goblin_trinket', 'Babiole de gobelin', java=info("Volée, évidemment."), tex=sprite(FOODS['necklace'], mat('#7a6a3a', trim='#a09050', gem='#c04040')))
    item('orc_banner', "Fanion d'orques", java=info("Un lambeau noir marqué d'un œil rouge."), tex=sprite(FOODS['cloak'], mat('#2a2428', trim='#c02a10')))
    item('elven_brooch', 'Broche elfique', java=info("Une feuille d'argent qui scintille sous la lune.", 'UNCOMMON'), tex=sprite(FOODS['necklace'], mat('#dfe8ee', trim='#8fd0ff', gem='#58e0a0')))
    item('dwarven_signet', 'Sceau nain', java=info("Cachet d'un clan des Montagnes.", 'UNCOMMON'), tex=sprite(RING, mat('#b08a4a', gem='#d04030')))
    item('dragon_tooth', 'Dent de dragon', java=info("Elle fume encore.", 'RARE', 16), tex=sprite(FOODS['tusk'], mat('#e8e0d0', trim='#8a251e')))
    # ---- armes et outils : retires, a refaire (voir git : tools/moreitems.py avant ce commit)
