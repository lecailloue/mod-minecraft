package net.ringsofash.world;

/**
 * Champs ajoutes autour de Hobbiteville : des polygones en coordonnees du fichier (x vers l'est, z vers le sud, depuis le coin nord-ouest du fichier source),
 * qui peuvent depasser du fichier (la marge est recopiee aussi, sur le relief naturel du monde).
 * Chaque polygone est un champ de ble (1) ou de pommes de terre (2).
 */
public final class HobbitonFields {
    private HobbitonFields() {}

    public static final int WHEAT = 1, POTATO = 2;

    private static final int[][] POLY = {
        // ble : au nord (prolonge le champ du nord-ouest), au nord-est et a l'est du champ de l'est
        {WHEAT, 20, -61, 215, -61, 235, -30, 225, 10, 125, 25, 100, 10, 20, 10},
        {WHEAT, 255, -40, 345, -40, 380, 0, 360, 95, 300, 105, 265, 70},
        {WHEAT, 300, 170, 406, 175, 406, 285, 300, 280},
        // pommes de terre : bande au sud, coin sud-ouest et bord ouest
        {POTATO, 30, 300, 200, 295, 205, 350, 190, 418, 40, 418, 25, 350},
        {POTATO, -82, 265, 20, 265, 35, 300, 35, 418, -82, 418},
        {POTATO, -82, -30, 20, -30, 30, 40, 25, 110, -82, 110},
    };

    /** 0 hors des champs, sinon WHEAT ou POTATO */
    public static int crop(int fx, int fz) {
        for (int[] p : POLY) {
            int n = (p.length - 1) / 2;
            boolean in = false;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                int xi = p[1 + 2 * i], zi = p[2 + 2 * i], xj = p[1 + 2 * j], zj = p[2 + 2 * j];
                if ((zi > fz) != (zj > fz) && fx < (long) (xj - xi) * (fz - zi) / (double) (zj - zi) + xi) in = !in;
            }
            if (in) return p[0];
        }
        return 0;
    }
}
