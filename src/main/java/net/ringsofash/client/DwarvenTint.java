package net.ringsofash.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * Teinte de la pierre naine : un degrade doux entre deux couleurs, qui varie d'un endroit a l'autre du monde.
 * Pour changer les couleurs, modifier COLOR_A et COLOR_B (format 0xRRGGBB).
 */
public final class DwarvenTint {
    public static final int COLOR_A = 0x4D5868;      // bleu-ardoise
    public static final int COLOR_B = 0x6B5A4A;      // brun chaud
    /** les textures grises ont une luminosite moyenne d'environ 0,82 : on compense pour retrouver les couleurs voulues */
    private static final float LUM = 0.82F;

    private DwarvenTint() {}

    private static float lattice(int x, int y, int z) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    private static float noise(double x, double y, double z) {
        int ix = Mth.floor(x), iy = Mth.floor(y), iz = Mth.floor(z);
        float fx = (float) (x - ix), fy = (float) (y - iy), fz = (float) (z - iz);
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy); fz = fz * fz * (3 - 2 * fz);
        float r = 0;
        for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) for (int dz = 0; dz < 2; dz++)
            r += lattice(ix + dx, iy + dy, iz + dz) * (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (dz == 0 ? 1 - fz : fz);
        return r;
    }

    private static int mix(float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = Math.round(Mth.lerp(t, (COLOR_A >> 16) & 255, (COLOR_B >> 16) & 255) / LUM);
        int g = Math.round(Mth.lerp(t, (COLOR_A >> 8) & 255, (COLOR_B >> 8) & 255) / LUM);
        int b = Math.round(Mth.lerp(t, COLOR_A & 255, COLOR_B & 255) / LUM);
        return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
    }

    /** couleur de teinte d'un bloc : degrade continu en fonction de la position */
    public static int at(BlockPos p) {
        float n = noise(p.getX() * 0.05, p.getY() * 0.07, p.getZ() * 0.05) * 0.65f + noise(p.getX() * 0.17 + 9, p.getY() * 0.2, p.getZ() * 0.17) * 0.35f;
        float t = (n - 0.25f) / 0.5f;
        float c = Mth.clamp(t, 0f, 1f);
        return mix(c * c * (3 - 2 * c));
    }

    /** teinte d'un objet (inventaire) : milieu du degrade */
    public static int item() { return mix(0.5f); }
}
