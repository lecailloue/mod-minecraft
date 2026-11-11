package net.ringsofash.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Le Livre du Voyageur : guide en francais, dont le bestiaire se complete au fil des rencontres. */
public final class Guide {
    private Guide() {}

    public static final String[] PAGES = {
        "§l§6Le Livre du\n§l§6Voyageur§r\n\nCe que tout voyageur de la Hautemarche doit savoir.\n\n§oÉcrit par les Anciens des Vertes-Collines, pour qui tombe du ciel sans savoir où il est.",
        "§lSommaire§r\n\n1. Le passage\n2. L'anneau des semi-hommes\n3. Les terres\n4. Les peuples\n5. Commerce et réputation\n6. Les anneaux\n7. Armes et armures\n8. La quête\n9. Objets utiles\n10. Bestiaire",
        "§l1. Le passage§r\n\nDans le monde normal, cherchez les §lTerres volcaniques§r : une île noire, aussi rare qu'une île champignon, dominée par un volcan géant.\n\nGrimpez au cratère et jetez un §lbloc d'or§r dans la lave.",
        "La lave pâlit : le passage est ouvert. Sautez dedans, vous ne brûlerez pas.\n\nVous arriverez dans un village de semi-hommes, au cœur des Vertes-Collines.\n\nAstuce : /locate biome ringsofash:volcan",
        "§l2. L'anneau des semi-hommes§r\n\nOffert à votre arrivée. Tenez-le et §lmaintenez clic droit§r 3 secondes pour passer d'un monde à l'autre.\n\nVous revenez toujours là où vous étiez la dernière fois.",
        "Il se recharge en une minute et refuse de servir en plein combat.\n\nIl ne brûle pas, ne corrompt pas, et n'attire pas les cavaliers.\n\nPerdu ? Parlez à l'Ancien d'un village de semi-hommes.",
        "§l3. Les terres§r\n\n§2Vertes-Collines§r : douces prairies des semi-hommes.\n§6Plaines du Vent§r : villages des hommes, chevaux.\n§eForêt Dorée§r : arbres géants des elfes.\n§2Vieille-Forêt§r : sylvegardes.",
        "§8Bois-Noir§r : araignées géantes.\n§7Landes des Tertres§r : tombes et spectres.\n§3Marais des Morts§r : eaux traîtresses.\n§7Monts de Fer§r : mines naines, lunargent.\n§fPics Blancs§r : aigles géants.",
        "§4Terres de Cendre§r : royaume des orques. Au centre se dresse le §lMont Funeste§r.\n§6Terres Sauvages§r : hommes sauvages et mammouths.\n\nLa nuit est dangereuse partout. Dormez, ou restez près d'un village.",
        "§l4. Les peuples§r\n\n§lSemi-hommes§r : fermiers et aubergistes. Nourriture contre récoltes.\n\n§lHommes§r : un peu de tout contre un peu de tout. Fermier, forgeron, tanneur, érudit.",
        "§lElfes§r : donnez-leur les dons de la forêt (bûches de doréal, étoiles-d'or, clochelunes, miel, baies). Ils offrent arcs, lames, capes, potions.\n\n§lNains§r : minerais contre armes et armures.",
        "§l5. Commerce et réputation§r\n\nChaque échange fait §amonter§r votre réputation chez ce peuple.\n\nTuer un habitant ou piller ses coffres la fait §cchuter§r lourdement.\n\nTouche §lR§r : menu de réputation.",
        "Rangs : Ennemi, Méfiant, Neutre, Ami, Héros.\n\n§lMéfiant§r : on refuse de commercer.\n§lEnnemi§r : les gardes attaquent.\n§lAmi§r : les meilleures offres s'ouvrent (anneaux, armes rares).",
        "§l6. Les anneaux§r\n\nUn anneau agit quand il est §ltenu en main§r (gauche de préférence).\n\nElfes : §bEaux§r (soin), §bVents§r (vitesse), §bBraises§r (feu).\nNains : §6Forge§r (hâte), §6Trésor§r (chance).",
        "Hommes : §cRois§r (force) et §cOmbres§r (discrétion), trouvés dans les tombes.\n\n§4L'Anneau de Cendre§r rend invisible... mais corrompt, et l'Œil finit par vous trouver.\n\nTout anneau attire les cavaliers blêmes.",
        "§l7. Armes et armures§r\n\nLunargent : le meilleur métal, au fond des Monts de Fer.\nLames elfiques : luisent près des orques.\nLame des tertres : blesse les spectres.\nHaches et marteaux nains. Cimeterres orques.",
        "Bâtons : feu, lumière, foudre, soin (mage errant).\n\nArmures : lunargent, elfique, naine, orque.\nPorter l'ensemble complet donne un bonus.\n\n§lForge naine§r : améliore une arme avec du lunargent.",
        "§l8. La quête§r\n\nAu plus profond d'une mine naine dort le §4Fléau de Braise§r. Il garde l'§4Anneau de Cendre§r.\n\nPortez l'anneau au §lMont Funeste§r et jetez-le dans la lave du cratère.",
        "Alors le §4Seigneur de Cendre§r viendra le réclamer. Terrassez-le.\n\nVotre récompense dépend de votre §lréputation générale§r :\n§aBonne§r : armure et épée du roi elfe.\n§cMauvaise§r : armure et masse du Seigneur.",
        "§l9. Objets utiles§r\n\nPain de route : très nourrissant.\nFiole de lumière : les araignées vous fuient.\nCorde elfique : pour descendre.\nCape elfique : accroupi, on vous voit mal.\nCor de guerre : renforce les alliés.",
        "Pierre de vision : montre les lieux proches (mais l'Œil regarde aussi).\nCor des morts : armée de spectres, une seule fois.\nSève vive : vous fait grandir.\n\nBûcher d'alarme : allumez-le pour appeler des soldats.",
    };

    public static final Map<String, String[]> BESTIARY = new LinkedHashMap<>();
    static {
        b("orc", "Orque", "Pillards des Terres de Cendre. Sortent la nuit, en bandes. Faibles seuls, dangereux à plusieurs.");
        b("war_orc", "Orque noir", "Soldat d'élite en armure. Frappe fort. Visez-le en premier ou fuyez.");
        b("orc_chief", "Chef orque", "Commande les forteresses. Garde le meilleur butin.");
        b("goblin", "Gobelin", "Vermine des cavernes. Minuscule, rapide, toujours en nombre. Craint le soleil.");
        b("cave_troll", "Troll des cavernes", "Trois blocs de haut. Sa massue envoie valser. Lent : gardez vos distances.");
        b("stone_troll", "Troll de pierre", "Le soleil le change en statue. Une pioche en vient alors à bout sans risque.");
        b("warg", "Loup de guerre", "Chasse en meute dans les plaines. Parfois monté par un orque.");
        b("wight", "Être des tertres", "Hante les Landes la nuit. Glace et affaiblit. Disparaît à l'aube.");
        b("wild_man", "Homme sauvage", "Hostile... sauf si votre réputation générale est bonne.");
        b("mammoth", "Mammouth", "Paisible tant qu'on ne le frappe pas. Sinon, courez.");
        b("pale_rider", "Cavalier blême", "Ne sort que la nuit. Très rapide. Sent un anneau à près de cent blocs, même à travers les murs. Craint le feu et les lames des tertres.");
        b("giant_spider", "Araignée géante", "Reine du Bois-Noir. Venin et toiles. Fuit la fiole de lumière.");
        b("lurker", "Guetteur des profondeurs", "Tapi dans les eaux sombres. Ses tentacules vous tirent vers lui. Frappez de loin.");
        b("cave_thief", "Créature blême", "Vole un objet de votre barre et s'enfuit. Rattrapez-la pour le reprendre.");
        b("fire_demon", "Fléau de Braise", "Démon d'ombre et de flamme, un par mine naine. Insensible au feu. Fouet, boules de feu, explosion de braises.");
        b("dragon", "Vieux Ver", "Dort sur son or sous la montagne. Souffle de feu, coup de queue.");
        b("sorcerer", "Sorcier corrompu", "Maître d'une tour noire. Foudre, crocs, téléportation. Insensible à la foudre.");
        b("ash_lord", "Seigneur de Cendre", "Le maître de l'Anneau. Seule la lame du roi elfe le blesse vraiment ; les autres armes sont émoussées contre lui.");
        b("treeman", "Sylvegarde", "Gardien des arbres. Ne coupez pas de bois près de lui. Écrase les orques.");
        b("eagle", "Aigle géant", "Offrez-lui viande ou poisson cru pour gagner sa confiance, puis volez sur son dos.");
        b("stag", "Cerf blanc", "Présage de bonne fortune. Très craintif.");
        b("crow", "Corbeau espion", "S'il vous voit porter un anneau, il alerte tous les monstres alentour.");
    }
    private static void b(String id, String title, String text) { BESTIARY.put(id, new String[]{title, text}); }

    public static boolean isGuide(ItemStack s) {
        return s.is(Items.WRITTEN_BOOK) && s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains("roa_guide");
    }

    public static ItemStack create(Player owner) {
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("roa_guide", true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        fill(s, owner);
        return s;
    }

    /** (Re)remplit les pages, avec le bestiaire debloque par ce joueur. */
    public static void fill(ItemStack s, Player owner) {
        PlayerLore lore = owner == null ? null : ModData.lore(owner);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String p : PAGES) pages.add(Filterable.passThrough(Component.literal(p)));
        int known = 0;
        for (Map.Entry<String, String[]> e : BESTIARY.entrySet()) {
            boolean ok = lore != null && lore.bestiary.contains(e.getKey());
            if (ok) known++;
            String t = ok ? "§l" + e.getValue()[0] + "§r\n\n" + e.getValue()[1] : "§l???§r\n\n§oPage vierge. Affrontez ou rencontrez cette créature pour la remplir.";
            pages.add(Filterable.passThrough(Component.literal(t)));
        }
        pages.add(21, Filterable.passThrough(Component.literal("§l10. Bestiaire§r\n\nPages remplies : " + known + " / " + BESTIARY.size()
                + "\n\nChaque créature vaincue ou apprivoisée ajoute sa page, avec ses points faibles.")));
        s.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Le Livre du Voyageur"), "Les Anciens", 0, pages, true));
    }
}
