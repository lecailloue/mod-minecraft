package net.ringsofash.menu;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.ringsofash.RingsOfAsh;

/** Regles de la forge naine : 6 niveaux de trempe, couts en lingots de lunargent, 20 % d'echec. */
public final class ForgeUpgrade {
    public static final int[] COSTS = {1, 5, 10, 20, 40, 64};
    public static final int MAX = COSTS.length;
    public static final float FAIL_CHANCE = 0.20F;
    /** Chance (par trempe reussie) d'obtenir la Trempe ancestrale : effet d'un niveau 7, -20 % de durabilite. */
    public static final float MEGA_CHANCE = 0.01F;
    public static final float MEGA_DURABILITY = 0.80F;
    public static final int MEGA_LEVEL = 7;

    private static ResourceKey<Enchantment> key(String n) { return ResourceKey.create(Registries.ENCHANTMENT, RingsOfAsh.id(n)); }
    public static final ResourceKey<Enchantment> KEY = key("dwarven_temper");
    public static final ResourceKey<Enchantment> KEY_ARMOR = key("dwarven_temper_armor");
    public static final ResourceKey<Enchantment> MEGA = key("ancestral_temper");
    public static final ResourceKey<Enchantment> MEGA_ARMOR = key("ancestral_temper_armor");

    private ForgeUpgrade() {}

    public static boolean isArmor(ItemStack s) {
        return s.getItem() instanceof ArmorItem || s.is(ItemTags.HEAD_ARMOR) || s.is(ItemTags.CHEST_ARMOR) || s.is(ItemTags.LEG_ARMOR) || s.is(ItemTags.FOOT_ARMOR);
    }

    public static boolean canForge(ItemStack s) {
        return !s.isEmpty() && (isArmor(s) || s.getItem() instanceof TieredItem || s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES) || s.is(ItemTags.PICKAXES)
                || s.is(ItemTags.SHOVELS) || s.is(ItemTags.HOES));
    }

    public static Holder<Enchantment> holder(RegistryAccess access, ItemStack s) {
        HolderLookup.RegistryLookup<Enchantment> l = access.lookupOrThrow(Registries.ENCHANTMENT);
        return l.getOrThrow(isArmor(s) ? KEY_ARMOR : KEY);
    }

    public static Holder<Enchantment> megaHolder(RegistryAccess access, ItemStack s) {
        HolderLookup.RegistryLookup<Enchantment> l = access.lookupOrThrow(Registries.ENCHANTMENT);
        return l.getOrThrow(isArmor(s) ? MEGA_ARMOR : MEGA);
    }

    public static boolean isMega(ItemStack s, RegistryAccess access) {
        return !s.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(megaHolder(access, s), s) > 0;
    }

    /** Niveau de trempe ; une Trempe ancestrale compte comme le sommet. */
    public static int level(ItemStack s, RegistryAccess access) {
        if (s.isEmpty()) return 0;
        if (isMega(s, access)) return MAX;
        return EnchantmentHelper.getItemEnchantmentLevel(holder(access, s), s);
    }

    /** Trempe ancestrale : remplace la trempe naine, durabilite maximale reduite de 20 %. */
    public static void applyMega(ItemStack s, RegistryAccess access) {
        Holder<Enchantment> t = holder(access, s), m = megaHolder(access, s);
        EnchantmentHelper.updateEnchantments(s, e -> { e.removeIf(h -> h.equals(t)); e.set(m, 1); });
        if (s.isDamageableItem()) {
            int max = s.getMaxDamage();
            int nm = Math.max(1, Math.round(max * MEGA_DURABILITY));
            s.set(DataComponents.MAX_DAMAGE, nm);
            if (s.getDamageValue() >= nm) s.setDamageValue(nm - 1);
        }
    }
}
