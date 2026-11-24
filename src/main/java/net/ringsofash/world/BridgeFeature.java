package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Les petits ponts de pierre, dans la maniere de celui de Hobbiteville : un tablier de pavés
 * entre deux parapets, des piles dans le courant, des marches aux deux bouts et du lierre.
 * La longueur suit la largeur de la riviere.
 */
public class BridgeFeature extends Feature<NoneFeatureConfiguration> {
    /** ce qui a ete pose pendant la session : l'autotest s'en sert pour pointer une camera */
    public static final java.util.List<String> PLACED = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public BridgeFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static int hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static final int MAX = 18;

    private WorldGenLevel level;
    private BlockPos.MutableBlockPos p;
    private int cx0, cz0;

    private boolean inReach(int x, int z) { return x >= cx0 - 15 && x <= cx0 + 30 && z >= cz0 - 15 && z <= cz0 + 30; }

    private void set(int x, int y, int z, BlockState st) {
        if (inReach(x, z) && y > level.getMinBuildHeight() + 1 && y < level.getMaxBuildHeight() - 1) level.setBlock(p.set(x, y, z), st, 2);
    }

    private boolean water(int x, int z, int y) { return level.getFluidState(p.set(x, y, z)).is(FluidTags.WATER); }

    private int floor(int x, int z) { return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; }

    private BlockState stone(int x, int z, int s) {
        int h = (hash(x, z, s) >>> 8) % 10;
        return h < 3 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                : h < 4 ? Blocks.ANDESITE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        level = ctx.level();
        RandomSource r = ctx.random();
        p = new BlockPos.MutableBlockPos();
        cx0 = ctx.origin().getX() & ~15;
        cz0 = ctx.origin().getZ() & ~15;
        int gate = hash(cx0 >> 4, cz0 >> 4, 911);
        if (((gate >>> 8) & 0xFF) % 100 >= 22) return false;

        int cx = cx0 + 6 + ((gate >>> 3) & 3), cz = cz0 + 6 + ((gate >>> 6) & 3);
        int biome = Zones.biomeFor(MapData.biome(cx, cz), cx, cz);
        if (biome == MapData.B_MER || biome == 12) return false;
        for (int[] pl : MapData.places().values())
            if (Math.abs(pl[0] - cx) < 200 && Math.abs(pl[1] - cz) < 200) return false;
        if (ShireDecorFeature.inStamp(cx, cz)) return false;

        int fy = floor(cx, cz);
        if (!water(cx, cz, fy + 1)) return false;
        int wy = fy;                                             // surface de l'eau
        while (wy < fy + 6 && water(cx, cz, wy + 1)) wy++;

        // largeur de l'eau sur les deux axes : on traverse par le plus court
        int bestLen = 99, bestAxis = -1, bestA = 0, bestB = 0;
        for (int axis = 0; axis < 2; axis++) {
            int a = 0, b = 0;
            while (a < MAX && water(cx - (axis == 0 ? a + 1 : 0), cz - (axis == 0 ? 0 : a + 1), wy)) a++;
            while (b < MAX && water(cx + (axis == 0 ? b + 1 : 0), cz + (axis == 0 ? 0 : b + 1), wy)) b++;
            int len = a + b + 1;
            if (a >= MAX || b >= MAX) continue;
            if (len < bestLen) { bestLen = len; bestAxis = axis; bestA = a; bestB = b; }
        }
        if (bestAxis < 0 || bestLen < 3 || bestLen > MAX) return false;

        int dirX = bestAxis == 0 ? 1 : 0, dirZ = 1 - dirX;       // le long du pont
        int perX = dirZ, perZ = dirX;                            // en travers
        int sx = cx - dirX * bestA, sz = cz - dirZ * bestA;      // premiere case d'eau
        int n = bestLen;

        // les deux berges doivent se ressembler et ne pas surplomber
        int bank0 = floor(sx - dirX, sz - dirZ), bank1 = floor(sx + dirX * n, sz + dirZ * n);
        if (Math.abs(bank0 - bank1) > 2) return false;
        int deck = Math.max(wy + 2, Math.max(bank0, bank1) + 1);
        if (deck - wy > 5) return false;
        for (int w = -2; w <= 2; w++)                            // pas de pont dans une gorge
            for (int e = -1; e <= 1; e += 2) {
                int x = sx + dirX * (e < 0 ? -1 : n) + perX * w, z = sz + dirZ * (e < 0 ? -1 : n) + perZ * w;
                if (floor(x, z) > deck + 2) return false;
            }

        BlockState slab = Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, SlabType.BOTTOM);
        for (int i = -1; i <= n; i++) {
            int bx = sx + dirX * i, bz = sz + dirZ * i;
            boolean end = i == -1 || i == n;
            for (int w = -2; w <= 2; w++) {
                int x = bx + perX * w, z = bz + perZ * w;
                boolean wall = Math.abs(w) == 2;
                set(x, deck, z, stone(x, z, 71));
                for (int y = deck + 1; y <= deck + 3; y++) set(x, y, z, Blocks.AIR.defaultBlockState());
                if (wall && !end) {
                    set(x, deck + 1, z, stone(x, z, 72));
                    if ((hash(x, z, 73) >>> 8) % 3 == 0) set(x, deck + 2, z, slab);
                } else if (!wall && (hash(x, z, 74) >>> 8) % 9 == 0) {
                    set(x, deck + 1, z, Blocks.MOSS_CARPET.defaultBlockState());
                }
            }
            if (end) {                                           // les marches qui rejoignent la berge
                Direction f = i == -1 ? (dirX == 1 ? Direction.EAST : Direction.SOUTH) : (dirX == 1 ? Direction.WEST : Direction.NORTH);
                for (int w = -1; w <= 1; w++) {
                    int x = bx + perX * w + dirX * (i == -1 ? -1 : 1), z = bz + perZ * w + dirZ * (i == -1 ? -1 : 1);
                    int g = floor(x, z);
                    if (g < deck) set(x, deck, z, Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, Half.BOTTOM));
                }
            }
            // les piles dans le courant
            if (!end && (i % 5 == 2 || n <= 4 && i == n / 2)) {
                for (int w = -1; w <= 1; w += 2) {
                    int x = bx + perX * w, z = bz + perZ * w;
                    int g = floor(x, z);
                    for (int y = g; y < deck; y++) set(x, y, z, stone(x, y + z, 75));
                }
            }
            // du lierre sur les parapets
            for (int w = -2; w <= 2; w += 4) {
                int x = bx + perX * w, z = bz + perZ * w;
                if ((hash(x, z, 76) >>> 8) % 100 >= 28) continue;
                Direction face = perX == 1 ? (w < 0 ? Direction.EAST : Direction.WEST) : (w < 0 ? Direction.SOUTH : Direction.NORTH);
                int ox = x + (perX == 1 ? (w < 0 ? -1 : 1) : 0), oz = z + (perZ == 1 ? (w < 0 ? -1 : 1) : 0);
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.PROPERTY_BY_DIRECTION.get(face.getOpposite()), true);
                for (int k = 0; k < 1 + ((hash(x, z, 77) >>> 8) % 3); k++) {
                    int y = deck - k;
                    if (level.getBlockState(p.set(ox, y, oz)).isAir()) set(ox, y, oz, vine);
                }
            }
        }
        if (PLACED.size() < 60) PLACED.add("pont de " + n + " blocs en " + cx + "," + deck + "," + cz + (dirX == 1 ? " est-ouest" : " nord-sud"));
        return true;
    }
}
