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
    ICE     ("rune_ice",        Family.ELEMENT,  1,   -5.0F,    0F,      0F,      0F,     0,          0),
    ELVEN   ("rune_elven",      Family.PEUPLES,  2,    0F,      0F,      0F,      0F,     0,          0),
    HOBBIT  ("rune_hobbit",     Family.PEUPLES,  2,    0F,      0F,      0F,      0F,     0,          0),
    FOREST  ("rune_forest",     Family.PEUPLES,  2,    0F,      0F,      0F,      0F,     0,          0),
    MAGIC   ("rune_magic",      Family.MAGIE,    1,    0F,      0F,      0F,      0F,     0,          0),
    BLACK   ("rune_black",      Family.NOIRE,    1,   25.0F,    0F,      0F,      0F,     2,          1);

    /** une seule rune par famille sur une arme */
    public enum Family {
        DEGATS("Dégâts"), TEMPO("Tempo"), ALLONGE("Allonge"), ELEMENT("Élément"),
        CORPS("Corps"), PEUPLES("Peuples"), MAGIE("Magie"), NOIRE("Ténèbres");
        public final String label;
        Family(String l) { this.label = l; }

        /** seules ces familles ont trois niveaux, donc seules elles se fusionnent */
        public boolean graded() { return this == DEGATS || this == TEMPO || this == ALLONGE || this == CORPS; }
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
            case MAGIE -> "Magie";
            case PEUPLES -> this == ELVEN ? "Elfique" : (this == HOBBIT ? "Hobbit" : "Forêt");
            case ELEMENT -> this == FIRE ? "Feu" : (this == WIND ? "Vent" : (this == POISON ? "Poison" : "Glace"));
        };
        return family.graded() ? n + " " + new String[]{"I", "II", "III"}[level - 1] : n;
    }

    public ChatFormatting color() {
        return switch (family) {
            case DEGATS -> ChatFormatting.RED;
            case TEMPO -> ChatFormatting.YELLOW;
            case ALLONGE -> ChatFormatting.GREEN;
            case ELEMENT -> this == ICE ? ChatFormatting.BLUE : ChatFormatting.GOLD;
            case CORPS -> ChatFormatting.AQUA;
            case PEUPLES -> ChatFormatting.DARK_GREEN;
            case MAGIE -> ChatFormatting.LIGHT_PURPLE;
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
        if (!family.graded()) return null;
        for (Rune r : values()) if (r.family == family && r.level == level + 1) return r;
        return null;
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
    public static int used(ItemStack weapon) { return used(on(weapon)); }

    public static int used(List<Rune> runes) {
        int n = 0;
        for (Rune r : runes) n += r.cost();
        return n;
    }

    public static int free(ItemStack weapon) { return slots(weapon) - used(weapon); }

    /** la famille est-elle deja prise ? */
    public static boolean taken(ItemStack weapon, Family f) {
        for (Rune r : on(weapon)) if (r.family == f) return true;
        return false;
    }

    /** « de dégâts » mais « d'élément » : l'elision devant une voyelle */
    private static String de(String word) {
        String w = word.toLowerCase();
        return "aeiouéèêàâîôûy".indexOf(w.charAt(0)) >= 0 ? "d'" + w : "de " + w;
    }

    /** pourquoi on ne peut pas sceller, ou null si c'est possible */
    public static String refuse(ItemStack weapon, Rune r) {
        if (!Runes.runic(weapon)) return "Seules les armes du mod portent des runes.";
        if (r == null) return "Posez une rune.";
        if (taken(weapon, r.family)) return "Cette arme porte déjà une rune " + de(r.family.label) + ".";
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
        wear(weapon, r);
    }

    /**
     * L'usure se regle une fois pour toutes : on change la durabilite maximale de
     * l'exemplaire. Une rune de solidite I (-33 % d'usure) fait durer l'arme une fois
     * et demie plus longtemps ; la rune de feu la fait durer deux fois moins.
     */
    private static void wear(ItemStack weapon, Rune r) {
        if (r.wear == 0 || !weapon.isDamageableItem()) return;
        float w = r.wear < 0 ? r.wear * r.affinity(weapon) : r.wear;
        int max = Math.max(1, Math.round(weapon.getMaxDamage() * (100F / (100F + w))));
        weapon.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE, max);
        if (weapon.getDamageValue() > max - 1) weapon.setDamageValue(max - 1);
    }

    // ------------------------------------------------------------------ combos

    /**
     * Deux runes de familles differentes ouvrent un bonus cache. Chaque exigence est soit
     * une rune precise (FIRE), soit une famille avec un niveau minimum (DEGATS au moins II).
     */
    public record Need(Rune exact, Family family, int min, String other) {
        static Need rune(Rune r) { return new Need(r, null, 0, null); }
        static Need fam(Family f, int min) { return new Need(null, f, min, null); }
        /** une condition qui ne tient pas a une rune : elle est verifiee au moment de l'effet */
        static Need cond(String label) { return new Need(null, null, 0, label); }
        boolean met(List<Rune> runes) {
            if (other != null) return true;
            for (Rune r : runes) {
                if (exact != null ? r == exact : (r.family == family && r.level >= min)) return true;
            }
            return false;
        }
        String label() {
            if (other != null) return other;
            return exact != null ? exact.label() : family.label + " " + new String[]{"I", "II", "III"}[min - 1];
        }

        /** le rang, dans l'ordre de scellement, de la rune qui remplit cette exigence */
        int rank(List<Rune> runes) {
            if (other != null) return Integer.MAX_VALUE;          // une condition n'a pas de rang
            for (int i = 0; i < runes.size(); i++) {
                Rune r = runes.get(i);
                if (exact != null ? r == exact : (r.family == family && r.level >= min)) return i;
            }
            return Integer.MAX_VALUE;
        }
    }

    public enum Combo {
        FOURNAISE("Fournaise", Need.rune(FIRE), Need.fam(Family.DEGATS, 2), "Les brûlures durent deux fois plus longtemps."),
        BOURRASQUE("Bourrasque", Need.rune(WIND), Need.fam(Family.ALLONGE, 2), "La projection passe de 5 % à 15 %."),
        VENIN("Venin vif", Need.rune(POISON), Need.fam(Family.TEMPO, 2), "Le poison dure 4 secondes au lieu de 2."),
        MUR("Mur", Need.fam(Family.CORPS, 2), Need.fam(Family.DEGATS, 2), "Les coups reçus ne te font plus reculer."),
        UDUN("Flamme d'Udûn", Need.rune(BLACK), Need.rune(FIRE), "Clic droit : le prochain coup embrase une zone de 3 blocs sur 4."),
        ELROND("Pouvoir d'Elrond", Need.rune(ELVEN), Need.rune(WIND), "Clic droit : dix secondes de vitesse d'attaque quadruplée."),
        RADAGAST("Radagast", Need.rune(MAGIC), Need.rune(FOREST), "Les bêtes à dix blocs viennent se battre à tes côtés."),
        PACTE("Pacte noir", Need.rune(MAGIC), Need.rune(BLACK), "La malédiction monte deux fois plus vite, mais le dernier ennemi tué se relève pour toi."),
        ENTS("Appel des Ents", Need.rune(FOREST), Need.cond("sous cinq cœurs"), "Un Sylvegarde vient se battre ; il revient dix minutes après sa mort."),
        MEMOIRE("Mémoire des Hobbits", Need.rune(HOBBIT), Need.cond("le guide en poche"), "Toutes les quêtes et leur emplacement apparaissent sur la carte.");

        public final String label, text;
        public final Need a, b;
        Combo(String label, Need a, Need b, String text) {
            this.label = label; this.a = a; this.b = b; this.text = text;
        }

        public boolean active(List<Rune> runes) { return a.met(runes) && b.met(runes); }

        /**
         * De quelles runes ce combo depend, par leur ordre de scellement. Sert a departager
         * deux combos possibles : c'est celui dont les runes ont ete posees le plus tot
         * qui prend la main.
         */
        long key(List<Rune> runes) {
            int ra = Math.min(a.rank(runes), 999), rb = Math.min(b.rank(runes), 999);
            int lo = Math.min(ra, rb), hi = Math.max(ra, rb);
            return (long) lo * 1000L + hi;
        }

        /** ce qu'il faut pour l'ouvrir, pour l'ecrire dans la table */
        public String recipe() { return a.label() + " + " + b.label(); }
    }

    /**
     * Une arme n'a qu'un seul combo : les pouvoirs ne s'additionnent pas. Si deux combos
     * sont possibles, c'est celui dont les runes ont ete scellees le plus tot qui l'emporte.
     * Feu, puis magie, puis noire : la noire se marie au feu, pas a la magie.
     */
    public static Combo combo(ItemStack weapon) {
        List<Rune> runes = on(weapon);
        if (runes.isEmpty()) return null;
        Combo best = null;
        long bestKey = Long.MAX_VALUE;
        for (Combo c : Combo.values()) {
            if (!c.active(runes)) continue;
            long k = c.key(runes);
            if (k < bestKey) { bestKey = k; best = c; }
        }
        return best;
    }

    /** le combo retenu, sous forme de liste pour s'ecrire simplement ailleurs */
    public static List<Combo> combos(ItemStack weapon) {
        Combo c = combo(weapon);
        return c == null ? List.of() : List.of(c);
    }

    /** les combos que l'arme aurait pu avoir, mais qui dorment : un seul peut s'eveiller */
    public static List<Combo> dormant(ItemStack weapon) {
        List<Rune> runes = on(weapon);
        Combo keep = combo(weapon);
        List<Combo> out = new ArrayList<>();
        for (Combo c : Combo.values()) if (c != keep && c.active(runes)) out.add(c);
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
