package net.ringsofash.menu;

import net.minecraft.core.Holder;
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
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT, RingsOfAsh.id("dwarven_temper"));

    private ForgeUpgrade() {}

    public static boolean canForge(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof TieredItem || s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES) || s.is(ItemTags.PICKAXES)
                || s.is(ItemTags.SHOVELS) || s.is(ItemTags.HOES));
    }

    public static Holder<Enchantment> holder(RegistryAccess access) {
        HolderLookup.RegistryLookup<Enchantment> l = access.lookupOrThrow(Registries.ENCHANTMENT);
        return l.getOrThrow(KEY);
    }

    public static int level(ItemStack s, RegistryAccess access) {
        return s.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder(access), s);
    }
}
