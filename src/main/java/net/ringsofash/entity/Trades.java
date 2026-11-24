package net.ringsofash.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.ringsofash.gen.Reg;

import java.util.Optional;

/** Commerce de chaque peuple. Les offres apres "premium" sont reservees aux amis (reputation >= 30). */
public final class Trades {
    private Trades() {}

    public static int professions(PeopleEntity e) {
        if (e.isRanger() || e.isMage()) return 1;
        return switch (e.people()) { case HUMAN -> 4; default -> 3; };
    }

    private static MerchantOffer o(ItemLike cost, int n, ItemLike res, int rn, int max) {
        return new MerchantOffer(new ItemCost(cost, n), new ItemStack(res, rn), max, 2, 0.0F);
    }
    private static MerchantOffer o2(ItemLike cost, int n, ItemLike cost2, int n2, ItemLike res, int rn, int max) {
        return new MerchantOffer(new ItemCost(cost, n), Optional.of(new ItemCost(cost2, n2)), new ItemStack(res, rn), max, 5, 0.0F);
    }
    private static MerchantOffer os(ItemLike cost, int n, ItemStack res, int max) {
        return new MerchantOffer(new ItemCost(cost, n), res, max, 2, 0.0F);
    }

    /** Remplit la liste ; renvoie l'indice de la premiere offre "premium". */
    public static int fill(PeopleEntity e, int prof, MerchantOffers l) {
        Item coin = Reg.SILVER_COIN.get();
        int premium;
        if (e.isMage()) {
            l.add(o(coin, 1, Items.FIREWORK_ROCKET, 6, 16));
            l.add(o(coin, 10, Reg.SEEING_STONE.get(), 1, 1));
            return l.size();
        }
        if (e.isRanger()) {
            l.add(o(coin, 3, Items.ARROW, 16, 12));
            l.add(o(Reg.WARG_PELT.get(), 3, coin, 4, 8));
            l.add(o(coin, 8, Items.BOW, 1, 3));
            l.add(o(coin, 10, Reg.ELVEN_CLOAK.get(), 1, 2));
            premium = l.size();
            l.add(o(coin, 14, Reg.LIGHT_PHIAL.get(), 1, 1));
            return premium;
        }
        switch (e.people()) {
            case HUMAN -> {
                switch (prof) {
                    case 0 -> {
                        l.add(o(Items.WHEAT, 20, coin, 1, 16)); l.add(o(Items.CARROT, 22, coin, 1, 16)); l.add(o(Items.POTATO, 22, coin, 1, 16));
                        l.add(o(coin, 1, Items.BREAD, 4, 16)); l.add(o(coin, 2, Items.APPLE, 5, 12));
                        premium = l.size(); l.add(o(coin, 10, Items.GOLDEN_CARROT, 6, 4));
                    }
                    case 1 -> {
                        l.add(o(Items.IRON_INGOT, 4, coin, 2, 12)); l.add(o(Items.COAL, 15, coin, 1, 12)); l.add(o(coin, 6, Items.IRON_SWORD, 1, 4));
                        l.add(o(coin, 7, Items.SHIELD, 1, 3));
                        premium = l.size(); l.add(o(coin, 18, Reg.WAR_HORN.get(), 1, 1));
                    }
                    case 2 -> {
                        l.add(o(coin, 1, Items.COOKED_BEEF, 3, 16)); l.add(o(Items.LEATHER, 6, coin, 1, 12)); l.add(o(Reg.WARG_PELT.get(), 2, coin, 3, 12));
                        l.add(o(Reg.TROLL_HIDE.get(), 1, coin, 4, 8)); l.add(o(coin, 8, Items.SADDLE, 1, 2));
                        premium = l.size(); l.add(o(coin, 14, Items.DIAMOND_HORSE_ARMOR, 1, 1));
                    }
                    default -> {
                        l.add(o(Items.PAPER, 24, coin, 1, 12)); l.add(o(coin, 4, Items.BOOK, 2, 8)); l.add(o(coin, 6, Items.COMPASS, 1, 4));
                        l.add(o(coin, 5, Items.MAP, 1, 4)); l.add(o(coin, 3, Items.LANTERN, 2, 8));
                        premium = l.size(); l.add(o(coin, 16, Reg.SEEING_STONE.get(), 1, 1));
                    }
                }
            }
            case ELF -> {
                switch (prof) {
                    case 0 -> {
                        l.add(o(Reg.GOLDEN_LOG.get(), 16, Reg.WAYBREAD.get(), 4, 8)); l.add(o(Reg.SUNSTAR.get(), 6, Reg.WAYBREAD.get(), 3, 8));
                        l.add(o(Items.HONEY_BOTTLE, 3, Reg.ELVEN_CLOAK.get(), 1, 2)); l.add(o(Reg.MOONBELL.get(), 6, Reg.LIGHT_PHIAL.get(), 1, 2));
                        l.add(o(Items.SWEET_BERRIES, 20, Reg.ELVEN_ROPE.get(), 2, 6));
                        premium = l.size(); l.add(o2(Reg.SUNSTAR.get(), 16, Reg.MOONBELL.get(), 16, Reg.RING_WINDS.get(), 1, 1));
                    }
                    case 1 -> {
                        l.add(o(Reg.GOLDEN_SAPLING.get(), 3, net.minecraft.world.item.Items.BOW, 1, 2));
                        l.add(o(Reg.SUNSTAR.get(), 10, Reg.ELVEN_HELMET.get(), 1, 2)); l.add(o(Reg.MOONBELL.get(), 12, Reg.ELVEN_CHESTPLATE.get(), 1, 2));
                        l.add(o(Reg.GOLDEN_LOG.get(), 20, Reg.ELVEN_LEGGINGS.get(), 1, 2)); l.add(o(Reg.SUNSTAR.get(), 8, Reg.ELVEN_BOOTS.get(), 1, 2));
                        premium = l.size(); l.add(o2(Reg.LUNARGENT_INGOT.get(), 6, Reg.SUNSTAR.get(), 12, Reg.MIRUVOR.get(), 4, 2));
                    }
                    default -> {
                        l.add(os(Reg.SUNSTAR.get(), 4, PotionContents.createItemStack(Items.POTION, Potions.HEALING), 6));
                        l.add(os(Reg.MOONBELL.get(), 4, PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION), 6));
                        l.add(os(Items.SWEET_BERRIES, 16, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS), 6));
                        l.add(o(Items.HONEY_BOTTLE, 2, Items.GOLDEN_APPLE, 1, 3));
                        premium = l.size(); l.add(o2(Reg.MOONBELL.get(), 20, Reg.SUNSTAR.get(), 20, Reg.RING_WATERS.get(), 1, 1));
                        l.add(o2(Reg.SUNSTAR.get(), 24, Items.BLAZE_POWDER, 6, Reg.RING_EMBERS.get(), 1, 1));
                    }
                }
            }
            case DWARF -> {
                switch (prof) {
                    case 0 -> {
                        l.add(o(Items.IRON_INGOT, 6, Reg.DWARVEN_HELMET.get(), 1, 2)); l.add(o(Items.DIAMOND, 3, Reg.DWARVEN_CHESTPLATE.get(), 1, 2));
                        l.add(o(Items.DIAMOND, 2, Reg.DWARVEN_LEGGINGS.get(), 1, 2)); l.add(o(Items.IRON_INGOT, 5, Reg.DWARVEN_BOOTS.get(), 1, 2));
                        premium = l.size(); l.add(o(Reg.LUNARGENT_INGOT.get(), 4, net.minecraft.world.item.Items.DIAMOND_PICKAXE, 1, 1));
                    }
                    case 1 -> {
                        l.add(o(Items.COAL, 24, Reg.DWARF_ALE.get(), 2, 8)); l.add(o(Items.RAW_IRON, 12, coin, 3, 12)); l.add(o(Items.RAW_GOLD, 8, coin, 4, 12));
                        l.add(o(Items.REDSTONE, 20, Reg.DWARVEN_LAMP.get(), 4, 6)); l.add(o(Items.DIAMOND, 1, coin, 6, 8));
                        premium = l.size(); l.add(o(Reg.RAW_LUNARGENT.get(), 10, Reg.RING_FORGE.get(), 1, 1));
                    }
                    default -> {
                        l.add(o(Items.GOLD_INGOT, 6, coin, 4, 12)); l.add(o(Items.EMERALD, 4, coin, 5, 12));
                        l.add(o(Reg.LUNARGENT_INGOT.get(), 5, Reg.LUNARGENT_HELMET.get(), 1, 1)); l.add(o(Reg.LUNARGENT_INGOT.get(), 4, Reg.LUNARGENT_BOOTS.get(), 1, 1));
                        premium = l.size(); l.add(o(Reg.LUNARGENT_INGOT.get(), 8, Reg.LUNARGENT_CHESTPLATE.get(), 1, 1)); l.add(o(Reg.LUNARGENT_INGOT.get(), 7, Reg.LUNARGENT_LEGGINGS.get(), 1, 1));
                        l.add(o2(Items.GOLD_BLOCK, 4, Items.DIAMOND, 4, Reg.RING_HOARD.get(), 1, 1));
                    }
                }
            }
            default -> {
                switch (prof) {
                    case 0 -> {
                        l.add(o(Items.WHEAT, 16, Reg.HONEY_CAKE.get(), 2, 12)); l.add(o(Items.PUMPKIN, 6, Reg.HALFLING_STEW.get(), 2, 12)); l.add(o(Items.CARROT, 16, Reg.HALFLING_STEW.get(), 1, 12));
                        l.add(o(Items.POTATO, 16, coin, 1, 12)); l.add(o(coin, 1, Reg.PIPEWEED.get(), 3, 8));
                        premium = l.size(); l.add(o(coin, 6, Reg.HONEY_CAKE.get(), 4, 8));
                    }
                    case 1 -> {
                        l.add(o(coin, 1, Reg.HALFLING_STEW.get(), 1, 16)); l.add(o(coin, 1, Reg.HONEY_CAKE.get(), 2, 16)); l.add(o(coin, 2, Reg.DWARF_ALE.get(), 2, 12));
                        l.add(o(Items.EGG, 12, coin, 1, 12)); l.add(o(Items.SUGAR, 10, coin, 1, 12));
                        premium = l.size(); l.add(o(coin, 6, Reg.WAYBREAD.get(), 4, 4));
                    }
                    default -> {
                        l.add(o(Items.BONE_MEAL, 12, coin, 1, 12)); l.add(o(coin, 1, Items.WHEAT_SEEDS, 8, 12)); l.add(o(coin, 2, Items.OAK_SAPLING, 4, 8));
                        l.add(o(coin, 3, Reg.SUNSTAR.get(), 4, 6)); l.add(o(coin, 1, Items.PUMPKIN_SEEDS, 4, 8));
                        premium = l.size(); l.add(o(coin, 6, Reg.GOLDEN_SAPLING.get(), 2, 2));
                    }
                }
            }
        }
        return premium;
    }
}
