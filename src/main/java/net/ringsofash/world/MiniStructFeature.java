package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.ringsofash.gen.Reg;

/**
 * Les petites traces de passage semees dans les terres : un puits, un campement au feu eteint,
 * une cabane vide, un batiment en ruine. Une chance sur une trentaine de chunks, loin des villes,
 * sur un terrain plat, avec les materiaux du biome.
 */
public class MiniStructFeature extends Feature<NoneFeatureConfiguration> {
    /** ce qui a ete pose pendant la session : lu par l'autotest pour pointer une camera dessus */
    public static final java.util.List<String> PLACED = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public MiniStructFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static int hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** materiaux : planches, bois de charpente, pierre, pierre moussue, porte */
    private record Pal(Block planks, Block fence, Block stairs, Block slab, Block stone, Block mossy, Block door, Block roof) {}

    private static Pal palette(int biome) {
        return switch (biome) {
            case 14 -> new Pal(Reg.GOLDEN_PLANKS.get(), Reg.GOLDEN_PLANKS_FENCE.get(), Reg.GOLDEN_PLANKS_STAIRS.get(), Reg.GOLDEN_PLANKS_SLAB.get(),
                    Reg.WHITE_STONE.get(), Blocks.MOSSY_COBBLESTONE, Blocks.BIRCH_DOOR, Reg.ELVEN_TILES_GREEN.get());
            case 15 -> new Pal(Reg.ANCIENT_PLANKS.get(), Reg.ANCIENT_PLANKS_FENCE.get(), Reg.ANCIENT_PLANKS_STAIRS.get(), Reg.ANCIENT_PLANKS_SLAB.get(),
                    Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.DARK_OAK_DOOR, Reg.ANCIENT_PLANKS.get());
            case 16 -> new Pal(Reg.WEATHERED_PLANKS.get(), Reg.WEATHERED_PLANKS_FENCE.get(), Reg.WEATHERED_PLANKS_STAIRS.get(), Reg.WEATHERED_PLANKS_SLAB.get(),
                    Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.DARK_OAK_DOOR, Reg.WEATHERED_PLANKS.get());
            case 17 -> new Pal(Reg.CYPRESS_PLANKS.get(), Reg.CYPRESS_PLANKS_FENCE.get(), Reg.CYPRESS_PLANKS_STAIRS.get(), Reg.CYPRESS_PLANKS_SLAB.get(),
                    Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.SPRUCE_DOOR, Reg.CYPRESS_PLANKS.get());
            case 13 -> new Pal(Blocks.OAK_PLANKS, Blocks.OAK_FENCE, Blocks.OAK_STAIRS, Blocks.OAK_SLAB,
                    Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.OAK_DOOR, Reg.THATCH.get());
            default -> new Pal(Blocks.OAK_PLANKS, Blocks.OAK_FENCE, Blocks.OAK_STAIRS, Blocks.OAK_SLAB,
                    Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.OAK_DOOR, Blocks.OAK_PLANKS);
        };
    }

    private static boolean landBiome(int b) {
        return b == 0 || b == 1 || b == 3 || b == 4 || b == 10 || b == 11 || b == 13 || b == 14 || b == 15 || b == 16 || b == 17;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        int hh = hash(x0 >> 4, z0 >> 4, 701);
        if (((hh >>> 8) & 0x3FF) >= 32) return false;              // environ un chunk sur trente
        int h2 = hash(x0 >> 4, z0 >> 4, 703);                      // tirages independants du filtre ci-dessus
        int x = x0 + 5 + ((h2 >>> 3) & 5), z = z0 + 5 + ((h2 >>> 9) & 5);
        int raw = MapData.biome(x, z);
        int biome = Zones.biomeFor(raw, x, z);
        if (!landBiome(biome)) return false;
        for (int[] pl : MapData.places().values())                 // jamais a l'approche d'une ville
            if (Math.abs(pl[0] - x) < 240 && Math.abs(pl[1] - z) < 240) return false;
        if (ShireDecorFeature.inStamp(x, z) || ShireDecorFeature.nearField(x, z, 10)) return false;

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        if (gy <= MapData.SEA + 1) return false;
        int lo = gy, hi = gy;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + dx, z + dz) - 1;
            lo = Math.min(lo, g); hi = Math.max(hi, g);
            if (!level.getFluidState(p.set(x + dx, g + 1, z + dz)).isEmpty()) return false;
        }
        if (hi - lo > 1) return false;                             // il faut du plat

        RandomSource r = ctx.random();
        BoundingBox clip = new BoundingBox(x0 - 16, level.getMinBuildHeight(), z0 - 16, x0 + 31, level.getMaxBuildHeight(), z0 + 31);
        Build b = new Build(level, clip, x, lo, z, (h2 >>> 15) & 3, hash(x, z, 702));
        Pal pal = palette(biome);
        int kind = ((h2 >>> 17) & 0x7FFF) % 100;
        String name;
        if (kind < 24) { well(b, pal, r); name = "puits"; }
        else if (kind < 56) { camp(b, pal, r); name = "campement"; }
        else if (kind < 80) { cabin(b, pal, r); name = "cabane"; }
        else { ruin(b, pal, r); name = "ruine"; }
        if (PLACED.size() < 120) PLACED.add(name + " " + x + "," + (lo + 1) + "," + z + " biome " + biome);
        return true;
    }

    /** un puits de pierre, son toit sur quatre poteaux, de l'eau au fond */
    private void well(Build b, Pal pal, RandomSource r) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
            if (corner) continue;
            if (ring) b.s(dx, 0, dz, r.nextInt(4) == 0 ? pal.mossy() : pal.stone());
            else if (dx == 0 && dz == 0) continue;
            else b.s(dx, 0, dz, r.nextInt(5) == 0 ? pal.mossy() : pal.stone());
        }
        for (int y = -1; y >= -4; y--) {                            // la margelle et le puits
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) b.s(dx, y, dz, pal.stone());
            b.s(0, y, 0, y <= -3 ? Blocks.WATER : Blocks.AIR);
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (dx != 0 || dz != 0) b.s(dx, 1, dz, r.nextInt(6) == 0 ? pal.mossy() : pal.stone());
        for (int[] c : new int[][] {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}) {
            b.s(c[0], 2, c[1], pal.fence());
            b.s(c[0], 3, c[1], pal.fence());
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)   // le toit
            if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) b.s(dx, 4, dz, pal.roof());
        b.s(0, 3, 0, Blocks.CHAIN);
        b.s(0, 2, 0, Blocks.CHAIN);
    }

    /** un campement : le cercle de pierres, le feu eteint, deux troncs pour s'asseoir */
    private void camp(Build b, Pal pal, RandomSource r) {
        for (int[] c : new int[][] {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}})
            if (r.nextInt(5) != 0) b.s(c[0], 0, c[1], r.nextInt(3) == 0 ? pal.mossy() : Blocks.COBBLESTONE);
        b.s(0, 0, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {   // la terre battue autour du feu
            if (Math.abs(dx) + Math.abs(dz) > 4 || (Math.abs(dx) <= 1 && Math.abs(dz) <= 1)) continue;
            if (r.nextInt(3) == 0) b.s(dx, -1, dz, Blocks.COARSE_DIRT);
        }
        for (int i = 0; i < 2; i++) {                                // les bancs
            int side = i == 0 ? -3 : 3;
            boolean alongX = r.nextBoolean();
            for (int k = -1; k <= 1; k++) {
                int dx = alongX ? k : side, dz = alongX ? side : k;
                b.s(dx, 0, dz, Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                        alongX ? Direction.Axis.X : Direction.Axis.Z));
            }
        }
        for (int k = 0; k < 2 + r.nextInt(2); k++)                   // le tas de bois
            b.s(-2, 1 + k, 2, Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
        if (r.nextInt(2) == 0) {                                     // un piquet et sa lanterne
            b.s(2, 1, 2, pal.fence());
            b.s(2, 2, 2, pal.fence());
            b.s(2, 3, 2, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, false));
        }
        if (r.nextInt(4) == 0) b.chest(-2, 1, 0, Direction.SOUTH, "human_house");
    }

    /** une cabane vide : quatre murs, une porte, une fenetre, un toit en pente */
    private void cabin(Build b, Pal pal, RandomSource r) {
        int w = 2, d = 2;                                            // 5 x 5
        b.foundation(-w, -d, w, d, pal.stone());
        b.fill(-w, 0, -d, w, 0, d, pal.planks());
        b.walls(-w, 1, -d, w, 3, d, pal.planks());
        b.air(-w + 1, 1, -d + 1, w - 1, 3, d - 1);
        for (int[] c : new int[][] {{-w, -d}, {-w, d}, {w, -d}, {w, d}})
            for (int y = 1; y <= 3; y++) b.s(c[0], y, c[1], Blocks.OAK_LOG);
        b.door(0, 1, -d, pal.door(), Direction.NORTH);
        b.s(w, 2, 0, Blocks.AIR);                                    // les fenetres
        b.s(-w, 2, 0, Blocks.AIR);
        b.s(w, 2, 0, pal.fence());
        b.s(-w, 2, 0, pal.fence());
        b.gable(-w, -d, w, d, 4, pal.stairs(), pal.slab(), pal.planks());
        if (r.nextInt(3) == 0) b.s(-w + 1, 1, d - 1, Blocks.CRAFTING_TABLE);
        if (r.nextInt(3) == 0) b.chest(w - 1, 1, d - 1, Direction.WEST, "human_house");
        for (int dx = -w; dx <= w; dx++) for (int dz = -d; dz <= d; dz++)   // le temps passe
            if (r.nextInt(9) == 0) b.s(dx, 3, dz, Blocks.AIR);
    }

    /** un batiment abandonne : des pans de mur, un seuil, des gravats */
    private void ruin(Build b, Pal pal, RandomSource r) {
        int w = 3, d = 2;
        b.foundation(-w, -d, w, d, pal.stone());
        b.fill(-w, 0, -d, w, 0, d, r.nextBoolean() ? pal.stone() : Blocks.COBBLESTONE);
        for (int dx = -w; dx <= w; dx++) for (int dz = -d; dz <= d; dz++) {
            boolean edge = Math.abs(dx) == w || Math.abs(dz) == d;
            if (!edge) { if (r.nextInt(4) == 0) b.s(dx, 0, dz, Blocks.COARSE_DIRT); continue; }
            int hgt = 1 + r.nextInt(3) + (Math.abs(dx) == w && Math.abs(dz) == d ? 1 : 0);
            for (int y = 1; y <= hgt; y++) {
                if (r.nextInt(7) == 0) continue;                     // les breches
                b.s(dx, y, dz, r.nextInt(3) == 0 ? pal.mossy() : pal.stone());
            }
        }
        b.air(0, 1, -d, 0, 2, -d);                                   // le seuil
        for (int i = 0; i < 6; i++) {                                 // les gravats
            int dx = -w + r.nextInt(2 * w + 1), dz = -d - 1 + r.nextInt(2 * d + 3);
            b.s(dx, 1, dz, r.nextBoolean() ? Blocks.COBBLESTONE : pal.mossy());
        }
        if (r.nextInt(3) == 0) b.s(-w + 1, 1, 0, Blocks.MOSSY_COBBLESTONE_WALL);
    }
}
