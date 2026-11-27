package net.ringsofash.world;

import java.util.Map;

/**
 * Les villes libres et leurs abords : aucun monstre n'y apparait naturellement.
 * Les places fortes de l'ennemi (Moria, Orthanc, Morannon, Minas Morgul, Barad-dur, le Mont du Destin)
 * gardent bien sur leurs habitants.
 */
public final class Towns {
    /** lieu -> rayon protege, en blocs */
    private static final Map<String, Integer> SAFE = Map.of(
            "comte", 260,
            "fondcombe", 180,
            "edoras", 200,
            "gouffre_de_helm", 200,
            "minas_terith", 250,
            "esgaroth", 180,
            "montagne_solitaire", 170,
            "argonath", 90);

    /** a qui appartient chaque ville : c'est ce peuple qui vous en veut si vous la saccagez */
    private static final Map<String, net.ringsofash.data.People> OWNER = Map.of(
            "comte", net.ringsofash.data.People.HALFLING,
            "fondcombe", net.ringsofash.data.People.ELF,
            "edoras", net.ringsofash.data.People.HUMAN,
            "gouffre_de_helm", net.ringsofash.data.People.HUMAN,
            "minas_terith", net.ringsofash.data.People.HUMAN,
            "esgaroth", net.ringsofash.data.People.HUMAN,
            "montagne_solitaire", net.ringsofash.data.People.DWARF,
            "argonath", net.ringsofash.data.People.HUMAN);

    private static int[][] zones;      // {x, z, rayon au carre, rang du peuple}

    private Towns() {}

    private static int[][] zones() {
        int[][] z = zones;
        if (z == null) {
            Map<String, int[]> places = MapData.places();
            z = new int[SAFE.size()][];
            int n = 0;
            for (Map.Entry<String, Integer> e : SAFE.entrySet()) {
                int[] p = places.get(e.getKey());
                if (p == null) continue;
                int r = e.getValue();
                net.ringsofash.data.People who = OWNER.getOrDefault(e.getKey(), net.ringsofash.data.People.HUMAN);
                z[n++] = new int[]{p[0], p[1], r * r, who.ordinal()};
            }
            if (n < z.length) {
                int[][] cut = new int[n][];
                System.arraycopy(z, 0, cut, 0, n);
                z = cut;
            }
            zones = z;
        }
        return z;
    }

    /** vrai si le point est dans une ville libre (ou dans Hobbitbourg et ses alentours) */
    public static boolean safe(int x, int z) {
        for (int[] c : zones()) {
            long dx = x - c[0], dz = z - c[1];
            if (dx * dx + dz * dz <= c[2]) return true;
        }
        return ShireDecorFeature.inStamp(x, z) || ShireDecorFeature.nearField(x, z, 40);
    }

    /** le rayon de la ville, ou 0 si ce lieu n'en est pas une */
    public static int radius(String key) { return SAFE.getOrDefault(key, 0); }

    /** le peuple a qui appartient ce coin de terre, ou null si on est en pleine nature */
    public static net.ringsofash.data.People owner(int x, int z) {
        for (int[] c : zones()) {
            long dx = x - c[0], dz = z - c[1];
            if (dx * dx + dz * dz <= c[2]) return net.ringsofash.data.People.values()[c[3]];
        }
        if (ShireDecorFeature.inStamp(x, z) || ShireDecorFeature.nearField(x, z, 40))
            return net.ringsofash.data.People.HALFLING;
        return null;
    }
}
