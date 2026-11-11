package net.ringsofash.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Grands arbres : doreal elance a couronne (style "crown") ou venerable noueux (style "gnarled"). */
public class BigTreeFeature extends Feature<BigTreeFeature.Config> {
    public record Config(BlockState log, BlockState leaves, int minHeight, int maxHeight, int trunk, int canopy, String style) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                BlockState.CODEC.fieldOf("leaves").forGetter(Config::leaves),
                Codec.INT.fieldOf("min_height").forGetter(Config::minHeight),
                Codec.INT.fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.INT.fieldOf("trunk").forGetter(Config::trunk),
                Codec.INT.fieldOf("canopy").forGetter(Config::canopy),
                Codec.STRING.fieldOf("style").forGetter(Config::style)).apply(i, Config::new));
    }

    public BigTreeFeature() { super(Config.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        Config c = ctx.config();
        BlockPos o = ctx.origin();
        if (!level.getBlockState(o.below()).is(BlockTags.DIRT)) return false;
        int h = c.minHeight + r.nextInt(Math.max(1, c.maxHeight - c.minHeight + 1));
        if (o.getY() + h + 10 >= level.getMaxBuildHeight()) return false;
        boolean gnarled = c.style.equals("gnarled");
        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        double off = c.trunk == 1 ? 0.5 : 0.0;

        // tronc
        for (int y = -2; y < h; y++) {
            double t = y / (double) h;
            double rad;
            if (c.trunk == 1) rad = y < 2 ? 1.6 : 0.9;
            else rad = y < 3 ? c.trunk + 0.9 : t < 0.55 ? c.trunk - 0.4 : t < 0.8 ? c.trunk - 0.9 : 0.9;
            disc(logs, o.above(y), rad, off);
        }
        // racines
        int roots = 4 + r.nextInt(4);
        for (int i = 0; i < roots; i++) {
            double a = (i + r.nextDouble() * 0.6) * Math.PI * 2 / roots;
            double x = Math.cos(a) * (c.trunk + 0.5), z = Math.sin(a) * (c.trunk + 0.5);
            int y = 1 + r.nextInt(2);
            int len = 2 + r.nextInt(gnarled ? 4 : 2);
            for (int k = 0; k < len; k++) {
                x += Math.cos(a); z += Math.sin(a);
                if (k > 0 && r.nextBoolean()) y--;
                BlockPos p = o.offset((int) Math.round(x), y, (int) Math.round(z));
                for (int d = 0; d < 4; d++) {
                    BlockPos q = p.below(d);
                    BlockState s = level.getBlockState(q);
                    if (!(s.isAir() || s.canBeReplaced() || s.is(BlockTags.DIRT) || s.is(BlockTags.LEAVES))) break;
                    logs.add(q);
                    if (s.is(BlockTags.DIRT)) break;
                }
            }
        }
        // branches
        int branches = gnarled ? 6 + r.nextInt(4) : 5 + r.nextInt(3);
        for (int i = 0; i < branches; i++) {
            double a = (i + r.nextDouble() * 0.8) * Math.PI * 2 / branches;
            double startT = gnarled ? 0.35 + r.nextDouble() * 0.6 : 0.68 + r.nextDouble() * 0.3;
            int len = gnarled ? 5 + r.nextInt(5) : 4 + r.nextInt(4);
            double rise = gnarled ? 0.25 + r.nextDouble() * 0.4 : 0.45 + r.nextDouble() * 0.3;
            double x = 0, z = 0, y = h * startT;
            for (int k = 0; k < len; k++) {
                x += Math.cos(a); z += Math.sin(a); y += rise;
                a += (r.nextDouble() - 0.5) * 0.35;
                logs.add(o.offset((int) Math.round(x + off), (int) Math.round(y), (int) Math.round(z + off)));
            }
            blob(leaves, o.offset((int) Math.round(x + off), (int) Math.round(y) + 1, (int) Math.round(z + off)), gnarled ? 3.6 : 3.2, 0.75, r);
        }
        // couronne
        if (gnarled) {
            blob(leaves, o.above(h), 4.5, 0.8, r);
            blob(leaves, o.above(h - 4), c.canopy * 0.6, 0.55, r);
        } else {
            blob(leaves, o.above(h - 1), c.canopy, 0.5, r);
            blob(leaves, o.above(h + 2), c.canopy * 0.6, 0.6, r);
        }
        leaves.removeAll(logs);

        boolean placed = false;
        for (BlockPos p : logs) {
            BlockState s = level.getBlockState(p);
            if (TreeFeature.validTreePos(level, p) || s.is(BlockTags.DIRT) || s.canBeReplaced()) {
                level.setBlock(p, c.log, 19);
                placed = true;
            }
        }
        // distances des feuilles (sinon elles tombent)
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos p : logs) { dist.put(p, 0); queue.add(p); }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = dist.get(p);
            if (d >= 7) continue;
            for (Direction dir : Direction.values()) {
                BlockPos q = p.relative(dir);
                if (leaves.contains(q) && !dist.containsKey(q)) { dist.put(q, d + 1); queue.add(q); }
            }
        }
        for (BlockPos p : leaves) {
            if (!TreeFeature.validTreePos(level, p)) continue;
            int d = dist.getOrDefault(p, 7);
            BlockState s = c.leaves;
            if (s.hasProperty(LeavesBlock.DISTANCE)) s = s.setValue(LeavesBlock.DISTANCE, Math.max(1, Math.min(7, d)));
            if (d >= 7 && s.hasProperty(LeavesBlock.PERSISTENT)) s = s.setValue(LeavesBlock.PERSISTENT, true);
            level.setBlock(p, s, 19);
        }
        return placed;
    }

    private static void disc(Set<BlockPos> out, BlockPos c, double rad, double off) {
        int ir = (int) Math.ceil(rad) + 1;
        for (int dx = -ir; dx <= ir; dx++)
            for (int dz = -ir; dz <= ir; dz++) {
                double x = dx - off, z = dz - off;
                if (x * x + z * z <= rad * rad) out.add(c.offset(dx, 0, dz));
            }
    }

    private static void blob(Set<BlockPos> out, BlockPos c, double rad, double flat, RandomSource r) {
        int ir = (int) Math.ceil(rad);
        int iy = (int) Math.ceil(rad * flat);
        for (int dx = -ir; dx <= ir; dx++)
            for (int dy = -iy; dy <= iy; dy++)
                for (int dz = -ir; dz <= ir; dz++) {
                    double yy = dy / flat;
                    double d = Math.sqrt(dx * dx + yy * yy + dz * dz);
                    if (d <= rad - 0.5 || (d <= rad && r.nextInt(3) != 0)) out.add(c.offset(dx, dy, dz));
                }
    }
}
