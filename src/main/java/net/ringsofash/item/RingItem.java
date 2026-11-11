package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Anneaux de pouvoir : actifs quand ils sont tenus en main (principale ou secondaire). Tous attirent les cavaliers blemes. */
public class RingItem extends Item {
    public enum Kind {
        ASH("Rend invisible aux yeux des vivants.", "Corrompt vite. L'Œil vous cherche."),
        WATERS("Régénération et souffle sous l'eau.", null),
        WINDS("Vitesse, saut et chute lente.", null),
        EMBERS("Résistance au feu et force.", null),
        FORGE("Hâte : mine et forge sans relâche.", null),
        HOARD("Chance et vision dans le noir.", "L'or appelle l'avidité : corrompt lentement."),
        KINGS("Grande force et résistance.", "Corrompt son porteur."),
        SHADOWS("Vision nocturne ; les monstres vous remarquent moins.", "Corrompt son porteur.");
        public final String power, curse;
        Kind(String power, String curse) { this.power = power; this.curse = curse; }
    }

    public final Kind kind;

    public RingItem(Kind kind, Properties p) { super(p); this.kind = kind; }

    public static Kind worn(Player p) {
        if (p.getOffhandItem().getItem() instanceof RingItem r) return r.kind;
        if (p.getMainHandItem().getItem() instanceof RingItem r) return r.kind;
        return null;
    }

    public static boolean wearsRing(Player p) { return worn(p) != null; }

    @Override
    public boolean isFoil(ItemStack stack) { return kind == Kind.ASH; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Porté en main (gauche ou droite) :").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal(kind.power).withStyle(ChatFormatting.GOLD));
        if (kind.curse != null) tip.add(Component.literal(kind.curse).withStyle(ChatFormatting.DARK_RED));
        tip.add(Component.literal("Les cavaliers blêmes le sentent de très loin.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
