package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Rochers irreguliers des terres de cendre : quelques masses ovoides qui se recouvrent, des eclats de roche calcinee sur le dessus et des cailloux autour.
 * Reutilise la configuration de {@link SpireFeature} (block = roche, cap = roche calcinee, hauteur min/max, radius = demi-largeur maximale).
 * Evite les sites des grandes forteresses et tout ce qui est construit (briques, bois, tapis...).
 */
public class RockFeature extends Feature<SpireFeature.Config> {
    private static final String[] MAN_MADE = {"brick", "carpet", "plank", "log", "slab", "stair", "wall", "bars", "lantern", "campfire", "barrel", "door", "chest", "fence",
            "glass", "hay", "wool", "shelf", "chain", "torch", "gold", "coal_block", "polished", "chiseled", "smooth"};
    private static final String[][] SITES = {{"minas_morgul", "90"}, {"orthanc", "115"}, {"barad_dur", "110"}};

    public RockFeature() { super(SpireFeature.Config.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<SpireFeature.Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        SpireFeature.Config c = ctx.config();
        BlockPos o = ctx.origin();
        for (String[] s : SITES) {
            int[] p = MapData.place(s[0]);
            if (p != null && Math.abs(p[0] - o.getX()) < Integer.parseInt(s[1]) && Math.abs(p[1] - o.getZ()) < Integer.parseInt(s[1])) return false;
        }
        BlockState ground = level.getBlockState(o.below());
        if (ground.isAir() || !level.getFluidState(o).isEmpty() || !level.getFluidState(o.below()).isEmpty() || !level.getBlockState(o).isAir()) return false;
        String gp = BuiltInRegistries.BLOCK.getKey(ground.getBlock()).getPath();
        for (String m : MAN_MADE) if (gp.contains(m)) return false;
        if (ground.is(net.minecraft.tags.BlockTags.LEAVES) || ground.is(net.minecraft.tags.BlockTags.LOGS)) return false;

        int h = c.minHeight() + r.nextInt(Math.max(1, c.maxHeight() - c.minHeight() + 1));
        int lumps = h <= 2 ? 1 + r.nextInt(2) : 2 + r.nextInt(3);
        double[][] ls = new double[lumps][5];                                // cx, cy, cz, rayon horizontal, rayon vertical
        for (int i = 0; i < lumps; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = i == 0 ? 0 : r.nextDouble() * c.radius() * 0.75;
            double rh = c.radius() * (i == 0 ? 0.7 + r.nextDouble() * 0.3 : 0.4 + r.nextDouble() * 0.45) + 0.4;
            double rv = Math.max(0.9, h * (i == 0 ? 0.6 + r.nextDouble() * 0.4 : 0.3 + r.nextDouble() * 0.5));
            ls[i] = new double[]{Math.cos(a) * d, i == 0 ? 0 : r.nextDouble() * h * 0.4, Math.sin(a) * d, rh, rv};
        }
        int ext = (int) Math.ceil(c.radius() * 1.9) + 1;
        for (int dx = -ext; dx <= ext; dx++) for (int dz = -ext; dz <= ext; dz++) {
            for (int dy = -2; dy <= h + 1; dy++) {
                boolean in = false;
                for (double[] l : ls) {
                    double ex = (dx - l[0]) / l[3], ey = (dy - l[1]) / l[4], ez = (dz - l[2]) / l[3];
                    double q = ex * ex + ey * ey + ez * ez + (r.nextDouble() - 0.5) * 0.28;
                    if (q <= 1.0) { in = true; break; }
                }
                if (!in) continue;
                BlockPos p = o.offset(dx, dy, dz);
                boolean exposed = level.getBlockState(p.above()).isAir() || dy >= h - 1;
                put(level, p, exposed && r.nextInt(100) < 38 ? c.cap() : c.block());
            }
        }
        // cailloux alentour
        int pebbles = 2 + r.nextInt(5) + c.radius();
        for (int i = 0; i < pebbles; i++) {
            int px = o.getX() + (int) Math.round((r.nextDouble() - 0.5) * 2 * (ext + 3)), pz = o.getZ() + (int) Math.round((r.nextDouble() - 0.5) * 2 * (ext + 3));
            int py = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, px, pz);
            BlockPos pp = new BlockPos(px, py, pz);
            BlockState below = level.getBlockState(pp.below());
            String bp = BuiltInRegistries.BLOCK.getKey(below.getBlock()).getPath();
            boolean bad = below.isAir() || !level.getFluidState(pp).isEmpty() || !level.getFluidState(pp.below()).isEmpty();
            for (String m : MAN_MADE) if (bp.contains(m)) bad = true;
            if (bad || !level.getBlockState(pp).isAir() || Math.abs(py - o.getY()) > 4) continue;
            put(level, pp, r.nextInt(3) == 0 ? c.cap() : c.block());
            if (r.nextInt(4) == 0) { BlockPos q = pp.relative(net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(r)); if (level.getBlockState(q).isAir() && !level.getBlockState(q.below()).isAir()) put(level, q, c.block()); }
        }
        return true;
    }

    private static void put(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced() || !cur.isSolid() || cur.is(net.minecraft.tags.BlockTags.DIRT)) level.setBlock(p, s, 2);
    }
}
