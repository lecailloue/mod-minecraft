package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.ringsofash.gen.Reg;

public class VolcanoPiece extends StructurePiece {
    private final int cx, cy, cz, radius, height;
    private final long seed;
    private final boolean doom;

    public VolcanoPiece(BlockPos c, int radius, int height, long seed, boolean doom, int minY) {
        super(ModWorldgen.VOLCANO_PIECE.get(), 0,
                new BoundingBox(c.getX() - radius - 8, minY, c.getZ() - radius - 8, c.getX() + radius + 8, c.getY() + height + 16, c.getZ() + radius + 8));
        this.cx = c.getX(); this.cy = c.getY(); this.cz = c.getZ();
        this.radius = radius; this.height = height; this.seed = seed; this.doom = doom;
    }

    public VolcanoPiece(CompoundTag tag) {
        super(ModWorldgen.VOLCANO_PIECE.get(), tag);
        cx = tag.getInt("cx"); cy = tag.getInt("cy"); cz = tag.getInt("cz");
        radius = tag.getInt("radius"); height = tag.getInt("height");
        seed = tag.getLong("seed"); doom = tag.getBoolean("doom");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("cx", cx); tag.putInt("cy", cy); tag.putInt("cz", cz);
        tag.putInt("radius", radius); tag.putInt("height", height);
        tag.putLong("seed", seed); tag.putBoolean("doom", doom);
    }

    public int rimRadius() { return doom ? 17 : 14; }
    public int lakeRadius() { return rimRadius() - 5; }
    public int rimY() { return cy + height; }
    public int lakeY() { return rimY() - 9; }
    public BlockPos center() { return new BlockPos(cx, cy, cz); }
    public BlockPos lakeCenter() { return new BlockPos(cx, lakeY(), cz); }
    public int radius() { return radius; }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        Noise2 n = new Noise2(seed);
        final int rimR = rimRadius(), lakeR = lakeRadius(), rimY = rimY(), lakeY = lakeY();
        final int streams = doom ? 7 : 5;
        double[] streamAngle = new double[streams];
        double[] streamLen = new double[streams];
        for (int k = 0; k < streams; k++) {
            streamAngle[k] = (k + n.hash(k, 77) * 0.7) * Math.PI * 2 / streams;
            streamLen[k] = 0.45 + n.hash(k, 99) * 0.4;
        }
        BlockState rock = Reg.VOLCANIC_ROCK.get().defaultBlockState();
        BlockState ash = Reg.ASH_BLOCK.get().defaultBlockState();
        BlockState smoke = Reg.SMOKING_ROCK.get().defaultBlockState();
        BlockState basalt = Blocks.BASALT.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = Math.max(box.minX(), boundingBox.minX()), x1 = Math.min(box.maxX(), boundingBox.maxX());
        int z0 = Math.max(box.minZ(), boundingBox.minZ()), z1 = Math.min(box.maxZ(), boundingBox.maxZ());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double dx = x - cx, dz = z - cz;
                double d = Math.sqrt(dx * dx + dz * dz);
                double ang = Math.atan2(dz, dx);
                double wob = 1.0 + 0.16 * n.fbm(Math.cos(ang) * 1.7 + 40, Math.sin(ang) * 1.7 + 40);
                double R = radius * wob;
                if (d > R) continue;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                double detail = n.fbm(x * 0.06, z * 0.06);
                double fine = n.fbm(x * 0.21 + 300, z * 0.21 + 300);
                int topY;
                boolean lake = false, stream = false;
                double prof = 0;
                if (d <= lakeR) {
                    lake = true;
                    topY = lakeY;
                } else if (d < rimR) {
                    double t = (d - lakeR) / (rimR - lakeR);
                    topY = lakeY + 1 + (int) Math.round((rimY - lakeY - 1) * Math.pow(t, 1.4) + fine * 1.2);
                } else {
                    double t = (d - rimR) / Math.max(1.0, R - rimR);
                    prof = Math.pow(1 - t, 1.75);
                    double ridge = Math.abs(n.fbm(Math.cos(ang) * 5.5 + 90, Math.sin(ang) * 5.5 + 90));
                    double y = cy - 3 + (height + 3) * prof;
                    y += detail * 6 * (0.3 + t) + fine * 1.5 - ridge * 9 * t * (1 - t) * 2.2;
                    topY = (int) Math.round(y);
                    // coulees de lave
                    for (int k = 0; k < streams; k++) {
                        double da = Math.atan2(Math.sin(ang - streamAngle[k] - 0.1 * Math.sin(d * 0.13 + k)), Math.cos(ang - streamAngle[k] - 0.1 * Math.sin(d * 0.13 + k)));
                        if (Math.abs(da) * d < 1.5 && t < streamLen[k]) { stream = true; break; }
                    }
                }
                if (lake) {
                    int floor = lakeY - 5;
                    for (int y = Math.min(ground, floor - 6); y <= floor; y++) { p.set(x, y, z); level.setBlock(p, y == floor ? magma : (fine > 0.2 ? basalt : rock), 2); }
                    for (int y = floor + 1; y <= lakeY; y++) { p.set(x, y, z); level.setBlock(p, lava, 2); }
                    for (int y = lakeY + 1; y <= rimY + 6; y++) { p.set(x, y, z); if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2); }
                    continue;
                }
                if (topY < ground) {
                    // pied du volcan : on se contente de noircir le sol
                    if (prof < 0.12 && detail > -0.3) {
                        p.set(x, ground - 1, z);
                        if (level.getFluidState(p).isEmpty() && !level.getBlockState(p).isAir()) level.setBlock(p, detail > 0.25 ? rock : ash, 2);
                    }
                    continue;
                }
                for (int y = Math.max(level.getMinBuildHeight() + 1, ground - 2); y <= topY; y++) {
                    p.set(x, y, z);
                    int depth = topY - y;
                    BlockState s;
                    if (stream && depth == 0) s = lava;
                    else if (stream && depth <= 2) s = magma;
                    else if (depth == 0) {
                        if (d < rimR + 2 && fine > 0.25) s = smoke;
                        else if (prof < 0.22 && detail < 0.1) s = ash;
                        else if (prof > 0.75 && fine > 0.45) s = magma;
                        else if (detail > 0.38) s = basalt;
                        else if (fine < -0.5) s = blackstone;
                        else s = rock;
                    } else if (depth < 4) s = detail > 0.38 ? basalt : rock;
                    else s = fine > 0.3 ? basalt : (fine < -0.45 ? blackstone : rock);
                    level.setBlock(p, s, 2);
                }
            }
        }
    }
}
