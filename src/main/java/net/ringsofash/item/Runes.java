package net.ringsofash.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    /** la rareté s'écrit sous le nom de l'arme, puis les emplacements et les runes scellées */
    private static void tooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!runic(s)) return;
        int t = tier(s);
        if (t < 0) return;
        List<Component> lines = e.getToolTip();
        List<Component> add = new java.util.ArrayList<>();
        add.add(Component.literal(NAMES[t]).withStyle(COLORS[t]));

        // les cases elles-memes sont dessinees cote client (voir RuneSlotsTooltip)
        List<Rune> runes = Rune.on(s);
        for (Rune r : runes) {
            MutableComponent line = Component.literal(" ").append(Component.literal(r.label()).withStyle(r.color()));
            String d = effects(r, s);
            if (!d.isEmpty()) line.append(Component.literal("  " + d).withStyle(ChatFormatting.GRAY));
            add.add(line);
            float a = r.affinity(s);
            String quoi = Rune.gear(s).armor() ? "cette pièce" : "cette arme";
            if (a > 1.01F) add.add(Component.literal("   affinité ×" + fmt(a) + " avec " + quoi).withStyle(ChatFormatting.DARK_GREEN));
            else if (a < 0.99F) add.add(Component.literal("   mal adaptée à " + quoi + " (×" + fmt(a) + ")").withStyle(ChatFormatting.DARK_RED));
        }

        Rune.Combo c = Rune.combo(s);
        if (c != null) {
            add.add(Component.literal("Combo : " + c.label).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            add.add(Component.literal(" " + c.text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        // une arme ne porte qu'un combo : les autres restent endormis
        for (Rune.Combo d : Rune.dormant(s))
            add.add(Component.literal("En sommeil : " + d.label).withStyle(ChatFormatting.DARK_GRAY));

        int at = Math.min(lines.size(), 1);
        lines.addAll(at, add);
    }

    /** ce que la rune fait sur cette arme-la, affinite comprise */
    private static String effects(Rune r, ItemStack weapon) {
        float a = r.affinity(weapon);
        List<String> parts = new java.util.ArrayList<>();
        boolean arme = !Rune.gear(weapon).armor();
        if (arme && r.dmg != 0) parts.add(pct(r.dmg > 0 ? r.dmg * a : r.dmg) + " dégâts");
        if (arme && r.speed != 0) parts.add(pct(r.speed > 0 ? r.speed * a : r.speed) + " vitesse");
        if (arme && r.reach != 0) parts.add(sign(r.reach * a) + fmt(Math.abs(r.reach * a)) + " bloc d'allonge");
        if (r.wear != 0) parts.add(r.wear < 0 ? pct(r.wear * a) + " d'usure" : "usure doublée");
        if (r == Rune.FIRE) parts.add("les coups brûlent");
        if (r == Rune.ICE) parts.add("gèle et ralentit");
        if (r == Rune.ELVEN) parts.add("armure elfique complète +20 %, sinon −20 %");
        if (r == Rune.HOBBIT) parts.add("pieds nus + artefact : mithril incassable, armure doublée");
        if (r == Rune.FOREST) parts.add("Force I sur un arbre");
        if (r == Rune.MAGIC) parts.add("réveille les pouvoirs des autres runes");
        if (r.armorBonus() != 0) parts.add("+" + fmt(r.armorBonus() * a) + " d'armure");
        if (!arme && r.moveBonus() != 0) parts.add(pct(r.moveBonus() > 0 ? r.moveBonus() * a : r.moveBonus()) + " de déplacement");
        if (r.thorns() != 0) parts.add("renvoie " + fmt(r.thorns() * a) + " dégâts");
        if (r.jumpLevel() != 0) parts.add(new String[]{"+½ bloc de saut", "+1 bloc de saut", "+1½ bloc de saut"}[r.jumpLevel() - 1]);
        if (r.swim() != 0) parts.add("aisance dans l'eau +" + fmt(r.swim() * a * 100) + " %");
        if (r.oxygen() != 0) parts.add("souffle ×" + fmt(1 + r.oxygen()) + " sous l'eau");
        if (r.dig() != 0) parts.add("+" + fmt(r.dig() * a) + " % de vitesse de creusement");
        if (r == Rune.WIND) parts.add("5 % de projection");
        if (r == Rune.POISON) parts.add("50 % d'empoisonnement");
        if (r.corrupt > 0) parts.add("+" + r.corrupt + " de malédiction par coup");
        if (r.bonusSlot > 0) parts.add("+" + r.bonusSlot + " emplacement");
        return String.join(", ", parts);
    }

    private static String pct(float v) { return sign(v) + fmt(Math.abs(v)) + " %"; }
    private static String sign(float v) { return v < 0 ? "−" : "+"; }
    private static String fmt(float v) {
        return Math.abs(v - Math.round(v)) < 0.05F ? String.valueOf(Math.round(v)) : String.format("%.1f", v).replace('.', ',');
    }

    static {
        RingsOfAsh.MODID.isEmpty();     // garde la classe liée au mod
    }
}
