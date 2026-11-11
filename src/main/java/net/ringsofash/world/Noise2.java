package net.ringsofash.world;

/** Petit bruit de valeur deterministe (pour les formes de terrain des structures). */
public final class Noise2 {
    private final long seed;

    public Noise2(long seed) { this.seed = seed; }

    public double hash(int x, int z) {
        long v = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
        v = (v ^ (v >>> 29)) * 0x9E3779B97F4A7C15L;
        v ^= v >>> 32;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        return (v & 0xFFFFFFL) / (double) 0xFFFFFFL;
    }

    public double value(double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double tx = x - x0, tz = z - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = hash(x0, z0) * (1 - tx) + hash(x0 + 1, z0) * tx;
        double b = hash(x0, z0 + 1) * (1 - tx) + hash(x0 + 1, z0 + 1) * tx;
        return a * (1 - tz) + b * tz;
    }

    /** bruit fractal dans [-1, 1] */
    public double fbm(double x, double z) {
        return (value(x, z) * 0.57 + value(x * 2.1 + 17, z * 2.1 + 31) * 0.29 + value(x * 4.3 + 71, z * 4.3 + 13) * 0.14) * 2 - 1;
    }
}
