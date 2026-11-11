package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.ringsofash.RingsOfAsh;

import java.util.function.Consumer;

/** Petit outil de construction en coordonnees locales (x largeur, y hauteur, z profondeur ; la facade est en z = 0). */
public class Build {
    public final WorldGenLevel level;
    public final BoundingBox clip;
    public final int ox, oy, oz, rot;
    public final RandomSource rnd;
    private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
    private static final Rotation[] ROTS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    public static final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, W = Direction.WEST;
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public Build(WorldGenLevel level, BoundingBox clip, int ox, int oy, int oz, int rot, long seed) {
        this.level = level; this.clip = clip; this.ox = ox; this.oy = oy; this.oz = oz; this.rot = rot & 3;
        this.rnd = RandomSource.create(seed);
    }

    public static int wx(int ox, int rot, int x, int z) { return ox + switch (rot & 3) { case 0 -> x; case 1 -> -z; case 2 -> -x; default -> z; }; }
    public static int wz(int oz, int rot, int x, int z) { return oz + switch (rot & 3) { case 0 -> z; case 1 -> x; case 2 -> -z; default -> -x; }; }

    public BlockPos pos(int x, int y, int z) { return new BlockPos(wx(ox, rot, x, z), oy + y, wz(oz, rot, x, z)); }
    public boolean in(int x, int y, int z) { m.set(wx(ox, rot, x, z), oy + y, wz(oz, rot, x, z)); return clip.isInside(m); }
    public BlockState get(int x, int y, int z) { return in(x, y, z) ? level.getBlockState(m) : Blocks.STONE.defaultBlockState(); }

    public void s(int x, int y, int z, BlockState st) {
        if (!in(x, y, z)) return;
        if (m.getY() <= level.getMinBuildHeight() || m.getY() >= level.getMaxBuildHeight()) return;
        st = st.rotate(ROTS[rot]);
        level.setBlock(m, st, 2);
        Block b = st.getBlock();
        if (b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock) level.getChunk(m).markPosForPostprocessing(m);
    }
    public void s(int x, int y, int z, Block b) { s(x, y, z, b.defaultBlockState()); }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState st) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) s(x, y, z, st);
    }
    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) { fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState()); }
    public void air(int x0, int y0, int z0, int x1, int y1, int z1) { fill(x0, y0, z0, x1, y1, z1, AIR); }

    /** murs verticaux d'une boite (sans sol ni plafond) */
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState st) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) { s(x, y, z0, st); s(x, y, z1, st); }
            for (int z = z0; z <= z1; z++) { s(x0, y, z, st); s(x1, y, z, st); }
        }
    }
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, Block b) { walls(x0, y0, z0, x1, y1, z1, b.defaultBlockState()); }

    /** melange aleatoire de deux blocs */
    public void mix(int x0, int y0, int z0, int x1, int y1, int z1, Block a, Block b, int oneIn) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) s(x, y, z, rnd.nextInt(oneIn) == 0 ? b : a);
    }

    /** remplit sous le batiment jusqu'au sol ferme */
    public void foundation(int x0, int z0, int x1, int z1, BlockState st, int max) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            for (int y = -1; y >= -max; y--) {
                if (!in(x, y, z)) continue;
                BlockState cur = level.getBlockState(m);
                if (cur.isSolidRender(level, m) && !cur.is(net.minecraft.tags.BlockTags.LEAVES) && !cur.is(net.minecraft.tags.BlockTags.LOGS)) break;
                level.setBlock(m, st, 2);
            }
        }
    }
    public void foundation(int x0, int z0, int x1, int z1, Block b) { foundation(x0, z0, x1, z1, b.defaultBlockState(), 14); }

    public static BlockState stairs(Block b, Direction facing, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }
    public static BlockState slab(Block b, boolean top) { return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM); }

    /** toit a deux pans le long de z (faitage parallele a z), pose a partir de y sur [x0..x1] */
    public void gable(int x0, int z0, int x1, int z1, int y, Block stair, Block ridge, Block wall) {
        int w = x1 - x0 + 1;
        for (int i = 0; i <= w / 2; i++) {
            int xa = x0 + i, xb = x1 - i;
            if (xa > xb) break;
            for (int z = z0; z <= z1; z++) {
                if (xa == xb) { s(xa, y + i, z, ridge.defaultBlockState()); }
                else { s(xa, y + i, z, stairs(stair, E, false)); s(xb, y + i, z, stairs(stair, W, false)); }
            }
            // pignons
            for (int x = xa + 1; x <= xb - 1; x++) { s(x, y + i, z0 + 1, wall); s(x, y + i, z1 - 1, wall); }
        }
    }

    public void door(int x, int y, int z, Block door, Direction facing) {
        s(x, y, z, door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        s(x, y + 1, z, door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** lit : tete en (x,z), pied dans la direction opposee a facing */
    public void bed(int x, int y, int z, Block bed, Direction facing) {
        s(x, y, z, bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD));
        s(x - facing.getStepX(), y, z - facing.getStepZ(), bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT));
    }

    public void chest(int x, int y, int z, Direction facing, String loot) {
        long seed = rnd.nextLong();
        if (!in(x, y, z)) return;
        BlockPos p = m.immutable();
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).rotate(ROTS[rot]), 2);
        if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity be) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, RingsOfAsh.id("chests/" + loot));
            be.setLootTable(key, seed);
        }
    }

    /** fait apparaitre une creature (une seule fois : seulement si sa position est dans le chunk en cours) */
    public <T extends Mob> void mob(EntityType<T> type, int x, int y, int z, int homeRadius, Consumer<T> setup) {
        if (!in(x, y, z)) return;
        BlockPos p = m.immutable();
        T e = type.create(level.getLevel());
        if (e == null) return;
        e.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, rnd.nextFloat() * 360F, 0);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        e.setPersistenceRequired();
        if (homeRadius > 0) e.restrictTo(p, homeRadius);
        if (setup != null) setup.accept(e);
        level.addFreshEntityWithPassengers(e);
        if (System.getProperty("roa.selftest") != null) RingsOfAsh.LOGGER.info("[SELFTEST] + " + type.toShortString() + " en " + p + " retire=" + e.isRemoved());
    }

    /** hauteur du sol (bloc solide le plus haut) a une position locale, relative a l'origine */
    public int groundY(int x, int z) {
        int wxx = wx(ox, rot, x, z), wzz = wz(oz, rot, x, z);
        boolean wg = level instanceof net.minecraft.server.level.WorldGenRegion;
        return level.getHeight(wg ? net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG : net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, wxx, wzz) - 1 - oy;
    }
}
