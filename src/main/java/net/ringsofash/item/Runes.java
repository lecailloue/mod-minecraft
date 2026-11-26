package net.ringsofash.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ringsofash.RingsOfAsh;

/**
 * Rareté des armes et runes.
 *
 * Chaque arme reçoit une rareté quand elle arrive dans l'inventaire : commune 50 %,
 * rare 35 %, épique 15 %. La rareté donne le nombre d'emplacements de runes
 * (commune 0, rare 1, épique 2) ; les trois niveaux suivants (unique 3, légendaire 4,
 * ultime 5) ne s'obtiennent qu'en améliorant l'arme à la table runique.
 */
public final class Runes {
    public static final String[] NAMES = {"Commune", "Rare", "Épique", "Unique", "Légendaire", "Ultime"};
    public static final ChatFormatting[] COLORS = {ChatFormatting.GRAY, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.RED};

    private Runes() {}

    public static void init(IEventBus bus) {
        bus.addListener(Runes::tooltip);
    }

    /** nombre d'emplacements de runes offerts par la rareté */
    public static int slots(int tier) {
        return Math.max(0, Math.min(5, tier));
    }

    /** rareté de l'arme, -1 si l'objet n'en a pas */
    public static int tier(ItemStack s) {
        Integer t = s.get(Artifacts.RARITY.get());
        return t == null ? -1 : Math.max(0, Math.min(5, t));
    }

    public static void setTier(ItemStack s, int tier) {
        s.set(Artifacts.RARITY.get(), Math.max(0, Math.min(5, tier)));
    }

    /** tirage à l'obtention : commune 50 %, rare 35 %, épique 15 % */
    public static int roll(RandomSource r) {
        int n = r.nextInt(100);
        return n < 50 ? 0 : (n < 85 ? 1 : 2);
    }

    /** vrai si l'objet peut porter des runes (toutes les armes du mod) */
    public static boolean runic(ItemStack s) {
        return !s.isEmpty() && s.is(net.ringsofash.data.ModTags.RUNIC);
    }

    /** donne une rareté aux armes qui n'en ont pas encore */
    public static void ensure(Player p) {
        RandomSource r = p.getRandom();
        for (ItemStack s : p.getInventory().items) {
            if (runic(s) && tier(s) < 0) setTier(s, roll(r));
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (runic(s) && tier(s) < 0) setTier(s, roll(r));
        }
    }

    /** la rareté s'écrit sous le nom de l'arme, en couleur */
    private static void tooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!runic(s)) return;
        int t = tier(s);
        if (t < 0) return;
        List<Component> lines = e.getToolTip();
        Component line = Component.literal(NAMES[t]).withStyle(COLORS[t]);
        if (lines.size() > 1) lines.add(1, line);
        else lines.add(line);
    }

    static {
        RingsOfAsh.MODID.isEmpty();     // garde la classe liée au mod
    }
}
