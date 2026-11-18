package net.ringsofash.item;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.gen.Reg;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/** Materiaux d'armes et d'armures. */
public final class ModGear {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, RingsOfAsh.MODID);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> mat(String name, int boots, int legs, int chest, int helm, int ench, Holder<SoundEvent> sound,
                                                                    float tough, float kb, Supplier<Ingredient> repair) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            m.put(ArmorItem.Type.BOOTS, boots); m.put(ArmorItem.Type.LEGGINGS, legs); m.put(ArmorItem.Type.CHESTPLATE, chest);
            m.put(ArmorItem.Type.HELMET, helm); m.put(ArmorItem.Type.BODY, chest);
        }), ench, sound, repair, List.of(new ArmorMaterial.Layer(RingsOfAsh.id(name))), tough, kb));
    }

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LUNARGENT = mat("lunargent", 3, 6, 8, 3, 18, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5F, 0.0F, () -> Ingredient.of(Reg.LUNARGENT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ORC = mat("orc", 2, 5, 6, 2, 8, SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.0F, () -> Ingredient.of(Reg.BLACK_STEEL_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ELVEN = mat("elven", 2, 5, 6, 3, 25, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.0F, () -> Ingredient.of(Reg.SUNSTAR.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DWARVEN = mat("dwarven", 3, 6, 8, 3, 12, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, () -> Ingredient.of(Items.IRON_INGOT));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROHAN = mat("rohan", 3, 6, 7, 3, 14, SoundEvents.ARMOR_EQUIP_IRON, 1.5F, 0.0F, () -> Ingredient.of(Items.IRON_INGOT));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GONDOR = mat("gondor", 3, 6, 8, 3, 16, SoundEvents.ARMOR_EQUIP_IRON, 2.0F, 0.0F, () -> Ingredient.of(Reg.LUNARGENT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ASHLORD = mat("ashlord", 4, 7, 9, 4, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F, () -> Ingredient.of(Reg.BLACK_STEEL_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ELFKING = mat("elfking", 4, 7, 9, 4, 25, SoundEvents.ARMOR_EQUIP_DIAMOND, 3.5F, 0.0F, () -> Ingredient.of(Reg.LUNARGENT_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CLOAK = mat("cloak", 0, 0, 2, 0, 20, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(Items.LEATHER));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HALFLING = mat("halfling", 1, 0, 0, 0, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> Ingredient.of(Items.LEATHER));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MINER = mat("miner", 0, 0, 0, 2, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.0F, () -> Ingredient.of(Items.IRON_INGOT));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROYAL = mat("royal", 0, 0, 0, 3, 25, SoundEvents.ARMOR_EQUIP_GOLD, 2.0F, 0.0F, () -> Ingredient.of(Items.GOLD_INGOT));

    public static final Tier T_LUNARGENT = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 9.0F, 4.0F, 18, () -> Ingredient.of(Reg.LUNARGENT_INGOT.get()));
    public static final Tier T_ELVEN = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 7.0F, 3.0F, 22, () -> Ingredient.of(Reg.SUNSTAR.get()));
    public static final Tier T_DWARVEN = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1400, 7.0F, 3.5F, 12, () -> Ingredient.of(Items.IRON_INGOT));
    public static final Tier T_ORC = new SimpleTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 350, 5.0F, 2.0F, 8, () -> Ingredient.of(Reg.BLACK_STEEL_INGOT.get()));
    public static final Tier T_STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 6.0F, 2.5F, 14, () -> Ingredient.of(Items.IRON_INGOT));
    public static final Tier T_LEGEND = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 9.0F, 5.0F, 20, () -> Ingredient.of(Reg.LUNARGENT_INGOT.get()));

    public static Item sword(Tier t, int dmg, float speed, Item.Properties p) { return special(SpecialSword.Kind.NONE, t, dmg, speed, 0, p); }

    public static Item special(SpecialSword.Kind kind, Tier t, int dmg, float speed, double reach, Item.Properties p) {
        ItemAttributeModifiers mods = SwordItem.createAttributes(t, dmg, speed);
        if (reach != 0) mods = mods.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(RingsOfAsh.id("reach"), reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return new SpecialSword(kind, t, p.attributes(mods));
    }

    public static Item axe(Tier t, float dmg, float speed, Item.Properties p) { return new AxeItem(t, p.attributes(AxeItem.createAttributes(t, dmg, speed))); }
    public static Item pickaxe(Tier t, Item.Properties p) { return new PickaxeItem(t, p.attributes(PickaxeItem.createAttributes(t, 1.0F, -2.8F))); }

    public static Item armor(Holder<ArmorMaterial> m, ArmorItem.Type type, int durability, Item.Properties p) {
        return new ArmorItem(m, type, p.durability(type.getDurability(durability)));
    }

    public static boolean fullSet(Player p, Holder<ArmorMaterial> m) {
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (!(st.getItem() instanceof ArmorItem a) || !a.getMaterial().is(m)) return false;
        }
        return true;
    }

    private ModGear() {}
}
