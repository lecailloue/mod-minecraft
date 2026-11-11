package net.ringsofash.data;

public enum People {
    HUMAN("Hommes", 0xC9A45A),
    ELF("Elfes", 0x7FD6A0),
    DWARF("Nains", 0xD08A4A),
    HALFLING("Semi-hommes", 0x9BCB5A);

    public final String label;
    public final int color;

    People(String label, int color) { this.label = label; this.color = color; }

    public static String rank(int rep) {
        if (rep <= -50) return "Ennemi";
        if (rep <= -15) return "Méfiant";
        if (rep < 15) return "Neutre";
        if (rep < 50) return "Ami";
        return "Héros";
    }
}
