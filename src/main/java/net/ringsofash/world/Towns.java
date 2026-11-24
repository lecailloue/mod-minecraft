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

    private static int[][] zones;      // {x, z, rayon au carre}

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
                z[n++] = new int[]{p[0], p[1], r * r};
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
}
