package net.ringsofash.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Catalogue des runes.
 *
 * Trois regles tiennent tout le systeme :
 *   1. une rune de niveau N occupe N emplacements, donc on ne peut pas tout empiler ;
 *   2. une seule rune par famille sur une arme, donc pas de feu + poison + vent ;
 *   3. chaque rune donne quelque chose et en reprend un peu ailleurs.
 *
 * S'y ajoutent les affinites (une rune ne vaut pas la meme chose sur une dague et sur
 * un marteau) et les combos (deux runes de familles differentes ouvrent un bonus cache).
 */
public enum Rune {
    //      chemin de l'objet   famille          niv  degats  vitesse  allonge  usure  corruption  emplacement
    DAMAGE_1("rune_damage_1",   Family.DEGATS,   1,    1.0F,   -1.0F,    0F,      0F,     0,          0),
    DAMAGE_2("rune_damage_2",   Family.DEGATS,   2,    2.5F,   -2.0F,    0F,      0F,     0,          0),
    DAMAGE_3("rune_damage_3",   Family.DEGATS,   3,    4.0F,   -3.0F,    0F,      0F,     0,          0),
    SPEED_1 ("rune_speed_1",    Family.TEMPO,    1,   -1.0F,    2.0F,    0F,      0F,     0,          0),
    SPEED_2 ("rune_speed_2",    Family.TEMPO,    2,   -2.0F,    4.0F,    0F,      0F,     0,          0),
    SPEED_3 ("rune_speed_3",    Family.TEMPO,    3,   -3.0F,    8.0F,    0F,      0F,     0,          0),
    REACH_1 ("rune_reach_1",    Family.ALLONGE,  1,   -3.0F,    0F,      1.0F,    0F,     0,          0),
    REACH_2 ("rune_reach_2",    Family.ALLONGE,  2,   -6.0F,    0F,      2.0F,    0F,     0,          0),
    REACH_3 ("rune_reach_3",    Family.ALLONGE,  3,  -10.0F,    0F,      3.0F,    0F,     0,          0),
    SOLID_1 ("rune_solidity_1", Family.CORPS,    1,    0F,     -2.0F,    0F,    -33F,     0,          0),
    SOLID_2 ("rune_solidity_2", Family.CORPS,    2,    0F,     -4.0F,    0F,    -60F,     0,          0),
    SOLID_3 ("rune_solidity_3", Family.CORPS,    3,    0F,     -6.0F,    0F,    -80F,     0,          0),
    FIRE    ("rune_fire",       Family.ELEMENT,  1,    0F,      0F,      0F,    100F,     0,          0),
    WIND    ("rune_wind",       Family.ELEMENT,  1,   -5.0F,    0F,      0F,      0F,     0,          0),
    POISON  ("rune_poison",     Family.ELEMENT,  1,   -5.0F,    0F,      0F,      0F,     0,          0),
    BLACK   ("rune_black",      Family.NOIRE,    1,   25.0F,    0F,      0F,      0F,     2,          1);

    /** une seule rune par famille sur une arme */
    public enum Family {
        DEGATS("Dégâts"), TEMPO("Tempo"), ALLONGE("Allonge"), ELEMENT("Élément"), CORPS("Corps"), NOIRE("Ténèbres");
        public final String label;
        Family(String l) { this.label = l; }
    }

    /** familles d'armes, pour les affinites */
    public enum Grip { SWORD, DAGGER, HEAVY, SPEAR, BOW }

    public final String path;
    public final Family family;
    public final int level;
    public final float dmg, speed, reach, wear;
    public final int corrupt, bonusSlot;

    Rune(String path, Family family, int level, float dmg, float speed, float reach, float wear, int corrupt, int bonusSlot) {
        this.path = path; this.family = family; this.level = level;
        this.dmg = dmg; this.speed = speed; this.reach = reach; this.wear = wear;
        this.corrupt = corrupt; this.bonusSlot = bonusSlot;
    }

    /** cout en emplacements : le niveau de la rune */
    public int cost() { return level; }

    public String label() {
        String n = switch (family) {
            case DEGATS -> "Dégât";
            case TEMPO -> "Vitesse";
            case ALLONGE -> "Portée";
            case CORPS -> "Solidité";
            case NOIRE -> "Rune noire";
            case ELEMENT -> this == FIRE ? "Feu" : (this == WIND ? "Vent" : "Poison");
        };
        if (family == Family.ELEMENT || family == Family.NOIRE) return n;
        return n + " " + new String[]{"I", "II", "III"}[level - 1];
    }

    public ChatFormatting color() {
        return switch (family) {
            case DEGATS -> ChatFormatting.RED;
            case TEMPO -> ChatFormatting.YELLOW;
            case ALLONGE -> ChatFormatting.GREEN;
            case ELEMENT -> ChatFormatting.GOLD;
            case CORPS -> ChatFormatting.AQUA;
            case NOIRE -> ChatFormatting.DARK_PURPLE;
        };
    }

    // ------------------------------------------------------------------ catalogue

    public static Rune byPath(String path) {
        for (Rune r : values()) if (r.path.equals(path)) return r;
        return null;
    }

    /** la rune portee par cet objet, ou null si ce n'en est pas une */
    public static Rune of(ItemStack s) {
        if (s.isEmpty()) return null;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
        return id == null ? null : byPath(id.getPath());
    }

    /** l'objet correspondant a cette rune */
    public ItemStack stack() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.ringsofash.RingsOfAsh.id(path))
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** la rune du niveau suivant dans la meme famille, pour la fusion ; null au niveau 3 */
    public Rune next() {
        for (Rune r : values()) if (r.family == family && r.level == level + 1 && sameLine(r)) return r;
        return null;
    }

    private boolean sameLine(Rune other) {
        return family != Family.ELEMENT || this == other;
    }

    // ------------------------------------------------------------------ affinites

    public static Grip grip(ItemStack weapon) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(weapon.getItem());
        String p = id == null ? "" : id.getPath();
        if (p.contains("bow")) return Grip.BOW;
        if (p.contains("dagger")) return Grip.DAGGER;
        if (p.contains("spear")) return Grip.SPEAR;
        if (p.contains("hammer") || p.contains("mace") || p.contains("axe") || p.contains("cleaver")
                || p.contains("war_pick") || p.contains("greatsword")) return Grip.HEAVY;
        return Grip.SWORD;
    }

    /** ce que vaut la rune sur cette arme : 0 = interdite, 1 = pleine, 1.5 = affinite */
    public float affinity(ItemStack weapon) {
        Grip g = grip(weapon);
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(weapon.getItem());
        boolean dwarven = id != null && id.getPath().startsWith("dwarven");
        return switch (family) {
            case TEMPO -> switch (g) { case DAGGER -> 1.5F; case HEAVY -> 0.5F; case SPEAR -> 0.75F; default -> 1.0F; };
            case ALLONGE -> switch (g) { case SPEAR, BOW -> 1.0F; default -> 0.5F; };
            case DEGATS -> switch (g) { case HEAVY -> 1.25F; case DAGGER -> 0.75F; default -> 1.0F; };
            case CORPS -> dwarven ? 1.25F : 1.0F;
            default -> 1.0F;
        };
    }

    // ------------------------------------------------------------------ emplacements

    /** les runes scellees sur cette arme */
    public static List<Rune> on(ItemStack weapon) {
        List<Rune> out = new ArrayList<>();
        List<String> keys = weapon.get(Artifacts.SOCKETS.get());
        if (keys == null) return out;
        for (String k : keys) { Rune r = byPath(k); if (r != null) out.add(r); }
        return out;
    }

    /** emplacements offerts par la rarete, plus ceux qu'une rune ajoute */
    public static int slots(ItemStack weapon) {
        int n = Runes.slots(Runes.tier(weapon));
        for (Rune r : on(weapon)) n += r.bonusSlot;
        return n;
    }

    /** emplacements deja occupes */
    public static int used(ItemStack weapon) {
        int n = 0;
        for (Rune r : on(weapon)) n += r.cost();
        return n;
    }

    public static int free(ItemStack weapon) { return slots(weapon) - used(weapon); }

    /** la famille est-elle deja prise ? */
    public static boolean taken(ItemStack weapon, Family f) {
        for (Rune r : on(weapon)) if (r.family == f) return true;
        return false;
    }

    /** pourquoi on ne peut pas sceller, ou null si c'est possible */
    public static String refuse(ItemStack weapon, Rune r) {
        if (!Runes.runic(weapon)) return "Seules les armes du mod portent des runes.";
        if (r == null) return "Posez une rune.";
        if (taken(weapon, r.family)) return "Cette arme porte déjà une rune de " + r.family.label.toLowerCase() + ".";
        if (r.affinity(weapon) <= 0F) return "Cette rune ne tient pas sur cette arme.";
        if (r.cost() > free(weapon)) return "Il faut " + r.cost() + " emplacement" + (r.cost() > 1 ? "s" : "") + " libre" + (r.cost() > 1 ? "s" : "") + ".";
        return null;
    }

    /** scelle la rune, definitivement */
    public static void socket(ItemStack weapon, Rune r) {
        List<String> keys = new ArrayList<>();
        List<String> old = weapon.get(Artifacts.SOCKETS.get());
        if (old != null) keys.addAll(old);
        keys.add(r.path);
        weapon.set(Artifacts.SOCKETS.get(), List.copyOf(keys));
    }

    // ------------------------------------------------------------------ combos

    /**
     * Deux runes de familles differentes ouvrent un bonus cache. Chaque exigence est soit
     * une rune precise (FIRE), soit une famille avec un niveau minimum (DEGATS au moins II).
     */
    public record Need(Rune exact, Family family, int min) {
        static Need rune(Rune r) { return new Need(r, null, 0); }
        static Need fam(Family f, int min) { return new Need(null, f, min); }
        boolean met(List<Rune> runes) {
            for (Rune r : runes) {
                if (exact != null ? r == exact : (r.family == family && r.level >= min)) return true;
            }
            return false;
        }
        String label() { return exact != null ? exact.label() : family.label + " " + new String[]{"I", "II", "III"}[min - 1]; }
    }

    public enum Combo {
        FOURNAISE("Fournaise", Need.rune(FIRE), Need.fam(Family.DEGATS, 2), "Les brûlures durent deux fois plus longtemps."),
        BOURRASQUE("Bourrasque", Need.rune(WIND), Need.fam(Family.ALLONGE, 2), "La projection passe de 5 % à 15 %."),
        VENIN("Venin vif", Need.rune(POISON), Need.fam(Family.TEMPO, 2), "Le poison dure 4 secondes au lieu de 2."),
        MUR("Mur", Need.fam(Family.CORPS, 2), Need.fam(Family.DEGATS, 2), "Les coups reçus ne te font plus reculer."),
        UDUN("Flamme d'Udûn", Need.rune(BLACK), Need.rune(FIRE), "Les coups embrasent tout dans 3 blocs.");

        public final String label, text;
        public final Need a, b;
        Combo(String label, Need a, Need b, String text) {
            this.label = label; this.a = a; this.b = b; this.text = text;
        }

        public boolean active(List<Rune> runes) { return a.met(runes) && b.met(runes); }

        /** ce qu'il faut pour l'ouvrir, pour l'ecrire dans la table */
        public String recipe() { return a.label() + " + " + b.label(); }
    }

    public static List<Combo> combos(ItemStack weapon) {
        List<Rune> runes = on(weapon);
        List<Combo> out = new ArrayList<>();
        if (runes.size() < 2) return out;
        for (Combo c : Combo.values()) if (c.active(runes)) out.add(c);
        return out;
    }

    // ------------------------------------------------------------------ total des effets

    /** somme des effets, affinites comprises */
    public record Totals(float dmg, float speed, float reach, float wear, int corrupt) {}

    public static Totals totals(ItemStack weapon) {
        float d = 0, s = 0, r = 0, w = 0;
        int c = 0;
        for (Rune u : on(weapon)) {
            float a = u.affinity(weapon);
            // le bonus suit l'affinite, le malus reste entier : une rune mal placee coute toujours
            d += u.dmg > 0 ? u.dmg * a : u.dmg;
            s += u.speed > 0 ? u.speed * a : u.speed;
            r += u.reach * a;
            w += u.wear < 0 ? u.wear * a : u.wear;
            c += u.corrupt;
        }
        return new Totals(d, s, r, Math.max(-90F, w), c);
    }
}
