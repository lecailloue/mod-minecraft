package net.ringsofash.world;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.block.CoinPileBlock;
import net.ringsofash.gen.Reg;

import static net.ringsofash.world.Build.E;
import static net.ringsofash.world.Build.N;
import static net.ringsofash.world.Build.S;
import static net.ringsofash.world.Build.W;
import static net.ringsofash.world.Build.slab;
import static net.ringsofash.world.Build.stairs;

/** Plans de tous les batiments. */
public final class Buildings {
    private Buildings() {}

    private static BlockState hang(Block lantern) { return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true); }
    private static BlockState ladder(Direction f) { return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, f); }
    private static BlockState log(Block b, Direction.Axis a) { return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, a); }

    public static void build(Build b, Settlement.T t, int aux) {
        switch (t) {
            case BURROW -> burrow(b);
            case FARM -> farm(b);
            case INN -> inn(b);
            case HOUSE -> house(b, aux);
            case HALL -> hall(b);
            case SMITHY -> smithy(b);
            case STALL -> stall(b, aux);
            case STABLE -> stable(b);
            case WATCHTOWER -> watchtower(b);
            case PLAZA -> plaza(b, aux);
            case PAVILION -> pavilion(b, aux);
            case ETOWER -> etower(b);
            case GATE -> gate(b);
            case POOL -> pool(b);
            case STAIR -> stair(b, aux);
            case DHALL -> dhall(b);
            case MINE -> mine(b);
            case LAIR -> lair(b);
            case FORT -> fort(b);
            case DTOWER -> dtower(b);
            case BARROW -> barrow(b);
            case HOARD -> hoard(b);
            case PYRE -> pyre(b);
            case RUIN -> ruin(b, aux);
            case MORGUL -> morgul(b);
            case ORTHANC -> orthanc(b);
            case BARADDUR -> baradDur(b);
            case EDORAS -> edoras(b);
            case TIRITH -> tirith(b);
        }
    }

    private static void people(Build b, EntityType<PeopleEntity> type, int x, int y, int z, int role, int prof, int home) {
        b.mob(type, x, y, z, home, e -> { if (prof >= 0) e.setProfession(prof); e.setRole(role); });
    }

    // ================================================================= SEMI-HOMMES
    private static void burrow(Build b) {
        Block lining = Blocks.SPRUCE_PLANKS;
        b.foundation(0, 1, 10, 10, Blocks.DIRT);
        for (int x = 0; x <= 10; x++) for (int z = 1; z <= 10; z++) {
            double dx = (x - 5) / 5.9, dz = (z - 5.5) / 5.6;
            int hh = (int) Math.round(5.6 * Math.sqrt(Math.max(0, 1 - dx * dx - dz * dz)));
            boolean inBox = x >= 1 && x <= 9 && z >= 1 && z <= 9;
            if (inBox) hh = Math.max(hh, 5);
            for (int y = 0; y <= hh; y++) b.s(x, y, z, y == hh ? Blocks.GRASS_BLOCK : Blocks.DIRT);
            if (hh >= 3 && b.rnd.nextInt(7) == 0) b.s(x, hh + 1, z, b.rnd.nextBoolean() ? Blocks.SHORT_GRASS : Blocks.DANDELION);
        }
        b.fill(1, 0, 1, 9, 4, 9, lining);
        b.air(2, 1, 2, 8, 3, 8);
        b.fill(2, 0, 2, 8, 0, 8, Blocks.OAK_PLANKS);
        // facade ronde
        b.fill(3, 1, 1, 7, 3, 1, Blocks.STRIPPED_SPRUCE_LOG);
        b.s(4, 3, 1, stairs(Blocks.SPRUCE_STAIRS, E, true)); b.s(6, 3, 1, stairs(Blocks.SPRUCE_STAIRS, W, true));
        b.s(4, 1, 1, Blocks.GREEN_TERRACOTTA); b.s(6, 1, 1, Blocks.GREEN_TERRACOTTA); b.s(4, 2, 1, Blocks.GREEN_TERRACOTTA); b.s(6, 2, 1, Blocks.GREEN_TERRACOTTA);
        b.door(5, 1, 1, Blocks.SPRUCE_DOOR, N);
        b.s(5, 3, 1, Blocks.GREEN_TERRACOTTA);
        b.s(2, 2, 1, Blocks.YELLOW_STAINED_GLASS); b.s(8, 2, 1, Blocks.YELLOW_STAINED_GLASS);
        // devant la porte
        for (int x = 2; x <= 8; x++) { b.s(x, 0, 0, x >= 4 && x <= 6 ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK); b.air(x, 1, 0, x, 4, 0); }
        b.foundation(2, 0, 8, 0, Blocks.DIRT);
        b.s(3, 1, 0, Blocks.POPPY); b.s(7, 1, 0, Blocks.AZURE_BLUET);
        // cheminee
        b.fill(8, 5, 7, 8, 7, 7, Blocks.BRICKS);
        // interieur
        b.bed(7, 1, 8, Blocks.GREEN_BED, S);
        b.chest(2, 1, 8, N, "halfling_house");
        b.s(2, 1, 2, Blocks.CRAFTING_TABLE); b.s(8, 1, 2, Blocks.FURNACE); b.s(3, 1, 8, Blocks.BARREL);
        b.s(5, 3, 5, hang(Blocks.LANTERN));
        b.s(8, 1, 5, Blocks.SPRUCE_FENCE); b.s(8, 2, 5, Blocks.SPRUCE_PRESSURE_PLATE);
        people(b, ModEntities.HALFLING.get(), 5, 1, 5, PeopleEntity.CIVIL, b.rnd.nextInt(3), 20);
        if (b.rnd.nextBoolean()) people(b, ModEntities.HALFLING.get(), 4, 1, 4, PeopleEntity.CIVIL, b.rnd.nextInt(3), 20);
    }

    private static void farm(Build b) {
        b.foundation(0, 0, 8, 8, Blocks.DIRT);
        b.fill(0, 0, 0, 8, 0, 8, Blocks.GRASS_BLOCK);
        b.air(0, 1, 0, 8, 3, 8);
        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES};
        Block c1 = crops[b.rnd.nextInt(3)], c2 = crops[b.rnd.nextInt(3)];
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) {
            if (x == 4) { b.s(x, 0, z, Blocks.WATER); continue; }
            b.s(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            Block c = x < 4 ? c1 : c2;
            if (b.rnd.nextInt(14) == 0) b.s(x, 1, z, Blocks.PUMPKIN);
            else b.s(x, 1, z, c.defaultBlockState().setValue(CropBlock.AGE, 4 + b.rnd.nextInt(4)));
        }
        b.walls(0, 1, 0, 8, 1, 8, Blocks.OAK_FENCE);
        b.s(4, 1, 0, Blocks.OAK_FENCE_GATE);
        b.s(0, 2, 0, Blocks.LANTERN); b.s(8, 2, 8, Blocks.LANTERN);
        b.s(8, 1, 0, Blocks.COMPOSTER);
    }

    private static void inn(Build b) {
        b.foundation(0, 0, 12, 9, Blocks.COBBLESTONE);
        b.fill(0, 0, 0, 12, 0, 9, Blocks.COBBLESTONE);
        b.air(0, 1, 0, 12, 10, 9);
        b.walls(0, 1, 0, 12, 4, 9, Blocks.BIRCH_PLANKS);
        b.walls(0, 1, 0, 12, 1, 9, Blocks.COBBLESTONE);
        for (int[] c : new int[][]{{0, 0}, {12, 0}, {0, 9}, {12, 9}, {6, 0}, {6, 9}}) b.fill(c[0], 1, c[1], c[0], 4, c[1], Blocks.OAK_LOG);
        b.fill(1, 0, 1, 11, 0, 8, Blocks.SPRUCE_PLANKS);
        b.gable(-1, -1, 13, 10, 5, Reg.THATCH_STAIRS.get(), Reg.THATCH.get(), Blocks.BIRCH_PLANKS);
        b.s(6, 1, 0, Blocks.AIR); b.door(5, 1, 0, Blocks.SPRUCE_DOOR, N); b.fill(6, 1, 0, 6, 4, 0, Blocks.OAK_LOG);
        for (int x : new int[]{2, 3, 9, 10}) { b.s(x, 2, 0, Blocks.GLASS_PANE); b.s(x, 2, 9, Blocks.GLASS_PANE); }
        b.s(0, 2, 4, Blocks.GLASS_PANE); b.s(12, 2, 4, Blocks.GLASS_PANE);
        // comptoir, tonneaux, tables
        b.fill(8, 1, 6, 11, 1, 6, slab(Blocks.SPRUCE_SLAB, true));
        b.fill(8, 1, 8, 11, 2, 8, Blocks.BARREL);
        for (int[] c : new int[][]{{2, 3}, {2, 6}, {5, 6}}) {
            b.s(c[0], 1, c[1], Blocks.SPRUCE_FENCE); b.s(c[0], 2, c[1], Blocks.SPRUCE_PRESSURE_PLATE);
            b.s(c[0] + 1, 1, c[1], stairs(Blocks.SPRUCE_STAIRS, E, false)); b.s(c[0] - 1, 1, c[1], stairs(Blocks.SPRUCE_STAIRS, W, false));
        }
        b.s(3, 4, 4, hang(Blocks.LANTERN)); b.s(9, 4, 4, hang(Blocks.LANTERN));
        b.s(11, 1, 1, Blocks.CAMPFIRE); b.fill(11, 2, 1, 11, 9, 1, Blocks.AIR); b.fill(11, 5, 1, 11, 9, 1, Blocks.BRICKS); b.s(11, 4, 1, Blocks.AIR);
        b.chest(1, 1, 8, N, "halfling_house");
        people(b, ModEntities.HALFLING.get(), 6, 1, 4, PeopleEntity.ELDER, 1, 12);
        people(b, ModEntities.HALFLING.get(), 9, 1, 7, PeopleEntity.CIVIL, 1, 12);
        people(b, ModEntities.HALFLING.get(), 3, 1, 5, PeopleEntity.CIVIL, 0, 16);
    }

    // ================================================================= HOMMES
    private static void house(Build b, int aux) {
        b.foundation(0, 0, 6, 8, Blocks.COBBLESTONE);
        b.fill(0, 0, 0, 6, 0, 8, Blocks.COBBLESTONE);
        b.air(0, 1, 0, 6, 9, 8);
        b.walls(0, 1, 0, 6, 4, 8, Blocks.CALCITE);
        b.walls(0, 1, 0, 6, 1, 8, Blocks.STONE_BRICKS);
        b.walls(0, 4, 0, 6, 4, 8, Blocks.DARK_OAK_PLANKS);
        for (int[] c : new int[][]{{0, 0}, {6, 0}, {0, 8}, {6, 8}, {0, 4}, {6, 4}}) b.fill(c[0], 1, c[1], c[0], 4, c[1], Blocks.DARK_OAK_LOG);
        b.fill(1, 0, 1, 5, 0, 7, Blocks.OAK_PLANKS);
        b.gable(-1, -1, 7, 9, 5, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.CALCITE);
        b.door(3, 1, 0, Blocks.SPRUCE_DOOR, N);
        b.s(1, 2, 0, Blocks.GLASS_PANE); b.s(5, 2, 0, Blocks.GLASS_PANE); b.s(0, 2, 2, Blocks.GLASS_PANE); b.s(6, 2, 2, Blocks.GLASS_PANE);
        b.s(0, 2, 6, Blocks.GLASS_PANE); b.s(6, 2, 6, Blocks.GLASS_PANE); b.s(3, 2, 8, Blocks.GLASS_PANE);
        b.bed(5, 1, 7, Blocks.RED_BED, S);
        b.chest(1, 1, 7, N, "human_house");
        b.s(1, 1, 1, Blocks.CRAFTING_TABLE); b.s(5, 1, 1, Blocks.FURNACE); b.s(3, 4, 4, hang(Blocks.LANTERN));
        b.s(1, 1, 4, Blocks.OAK_FENCE); b.s(1, 2, 4, Blocks.OAK_PRESSURE_PLATE);
        people(b, ModEntities.HUMAN.get(), 3, 1, 4, PeopleEntity.CIVIL, aux % 4, 20);
    }

    private static void hall(Build b) {
        b.foundation(0, 0, 14, 20, Blocks.STONE_BRICKS);
        b.fill(0, 0, 0, 14, 1, 20, Blocks.STONE_BRICKS);
        b.air(0, 2, 0, 14, 13, 20);
        for (int x = 5; x <= 9; x++) { b.s(x, 1, 0, stairs(Blocks.STONE_BRICK_STAIRS, S, false)); b.s(x, 2, 0, Blocks.AIR); }
        b.fill(0, 1, 0, 4, 1, 0, Blocks.AIR); b.fill(10, 1, 0, 14, 1, 0, Blocks.AIR); b.fill(0, 0, 0, 14, 0, 0, Blocks.GRASS_BLOCK); b.fill(5, 0, 0, 9, 0, 0, Blocks.STONE_BRICKS);
        b.walls(1, 2, 2, 13, 6, 19, Blocks.SPRUCE_PLANKS);
        for (int z = 2; z <= 19; z += (z == 14 ? 5 : 4)) { b.fill(1, 2, z, 1, 6, z, Blocks.OAK_LOG); b.fill(13, 2, z, 13, 6, z, Blocks.OAK_LOG); }
        for (int z = 6; z <= 16; z += 5) { b.fill(4, 2, z, 4, 9, z, Blocks.STRIPPED_OAK_LOG); b.fill(10, 2, z, 10, 9, z, Blocks.STRIPPED_OAK_LOG); }
        b.gable(0, 1, 14, 20, 7, Reg.THATCH_STAIRS.get(), Reg.THATCH.get(), Blocks.SPRUCE_PLANKS);
        b.fill(6, 2, 2, 8, 4, 2, Blocks.AIR);
        b.fill(2, 1, 3, 12, 1, 18, Blocks.OAK_PLANKS);
        b.fill(7, 1, 3, 7, 1, 16, Blocks.YELLOW_TERRACOTTA);
        for (int z = 5; z <= 17; z += 4) { b.s(1, 4, z + 1, Blocks.GLASS_PANE); b.s(13, 4, z + 1, Blocks.GLASS_PANE); b.s(3, 6, z, hang(Blocks.LANTERN)); b.s(11, 6, z, hang(Blocks.LANTERN)); }
        b.s(7, 2, 10, Blocks.CAMPFIRE);
        b.fill(6, 1, 17, 8, 1, 18, Blocks.POLISHED_ANDESITE); b.s(7, 2, 18, stairs(Blocks.QUARTZ_STAIRS, N, false));
        b.s(6, 2, 18, Blocks.GOLD_BLOCK); b.s(8, 2, 18, Blocks.GOLD_BLOCK);
        b.chest(2, 2, 18, N, "human_house"); b.chest(12, 2, 18, N, "human_house");
        people(b, ModEntities.HUMAN.get(), 7, 2, 15, PeopleEntity.CIVIL, 3, 10);
        people(b, ModEntities.HUMAN.get(), 5, 2, 4, PeopleEntity.GUARD, -1, 16);
        people(b, ModEntities.HUMAN.get(), 9, 2, 4, PeopleEntity.GUARD, -1, 16);
    }

    private static void smithy(Build b) {
        b.foundation(0, 0, 6, 6, Blocks.COBBLESTONE);
        b.fill(0, 0, 0, 6, 0, 6, Blocks.COBBLESTONE);
        b.air(0, 1, 0, 6, 5, 6);
        for (int[] c : new int[][]{{0, 0}, {6, 0}, {0, 6}, {6, 6}}) b.fill(c[0], 1, c[1], c[0], 3, c[1], Blocks.SPRUCE_LOG);
        b.fill(0, 1, 6, 6, 3, 6, Blocks.COBBLESTONE);
        b.fill(-1 + 1, 4, 0, 6, 4, 6, slab(Blocks.SPRUCE_SLAB, false));
        b.s(1, 1, 5, Blocks.BLAST_FURNACE); b.s(2, 1, 5, Blocks.BLAST_FURNACE); b.s(4, 1, 5, Blocks.LAVA_CAULDRON); b.s(5, 1, 5, Blocks.SMITHING_TABLE);
        b.s(3, 1, 3, Blocks.ANVIL); b.s(5, 1, 2, Blocks.GRINDSTONE); b.s(3, 3, 3, hang(Blocks.LANTERN));
        b.chest(1, 1, 3, E, "human_house");
        people(b, ModEntities.HUMAN.get(), 3, 1, 2, PeopleEntity.CIVIL, 1, 12);
    }

    private static void stall(Build b, int aux) {
        Block[] wools = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.GREEN_WOOL};
        Block col = wools[Math.floorMod(aux, 4)];
        b.foundation(0, 0, 4, 3, Blocks.DIRT);
        b.fill(0, 0, 0, 4, 0, 3, Blocks.DIRT_PATH);
        b.air(0, 1, 0, 4, 4, 3);
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 3}, {4, 3}}) b.fill(c[0], 1, c[1], c[0], 2, c[1], Blocks.OAK_FENCE);
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 3; z++) b.s(x, 3, z, x % 2 == 0 ? col : Blocks.WHITE_WOOL);
        b.fill(1, 1, 1, 3, 1, 1, Blocks.BARREL);
        b.s(1, 2, 1, b.rnd.nextBoolean() ? Blocks.MELON : Blocks.HAY_BLOCK); b.s(3, 2, 1, Blocks.PUMPKIN);
        people(b, ModEntities.HUMAN.get(), 2, 1, 2, PeopleEntity.CIVIL, aux % 2 == 0 ? 0 : 2, 6);
    }

    private static void stable(Build b) {
        b.foundation(0, 0, 8, 6, Blocks.DIRT);
        b.fill(0, 0, 0, 8, 0, 6, Blocks.COARSE_DIRT);
        b.air(0, 1, 0, 8, 5, 6);
        b.walls(0, 1, 0, 8, 1, 6, Blocks.SPRUCE_FENCE);
        b.fill(4, 1, 0, 4, 1, 6, Blocks.SPRUCE_FENCE);
        b.s(2, 1, 0, Blocks.SPRUCE_FENCE_GATE); b.s(6, 1, 0, Blocks.SPRUCE_FENCE_GATE);
        for (int[] c : new int[][]{{0, 0}, {8, 0}, {0, 6}, {8, 6}, {4, 0}, {4, 6}}) b.fill(c[0], 1, c[1], c[0], 3, c[1], Blocks.SPRUCE_LOG);
        b.fill(0, 4, 0, 8, 4, 6, slab(Blocks.SPRUCE_SLAB, false));
        b.s(1, 1, 5, Blocks.HAY_BLOCK); b.s(7, 1, 5, Blocks.HAY_BLOCK); b.s(7, 2, 5, Blocks.HAY_BLOCK); b.s(3, 1, 5, Blocks.WATER_CAULDRON);
        b.mob(EntityType.HORSE, 2, 1, 3, 0, null);
        b.mob(EntityType.HORSE, 6, 1, 3, 0, null);
    }

    private static void watchtower(Build b) {
        b.foundation(0, 0, 4, 4, Blocks.COBBLESTONE);
        b.fill(0, 0, 0, 4, 0, 4, Blocks.COBBLESTONE);
        b.air(0, 1, 0, 4, 14, 4);
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) b.fill(c[0], 1, c[1], c[0], 10, c[1], Blocks.SPRUCE_LOG);
        b.fill(2, 1, 2, 2, 10, 2, Blocks.SPRUCE_LOG);
        for (int y = 1; y <= 10; y++) b.s(2, y, 1, ladder(N));
        b.fill(0, 10, 0, 4, 10, 4, Blocks.SPRUCE_PLANKS);
        b.s(2, 10, 1, ladder(N));
        b.walls(0, 11, 0, 4, 11, 4, Blocks.SPRUCE_FENCE);
        b.s(2, 11, 2, Reg.ALARM_PYRE.get());
        for (int y = 3; y <= 9; y += 3) { b.fill(0, y, 0, 4, y, 0, Blocks.SPRUCE_FENCE); b.fill(0, y, 4, 4, y, 4, Blocks.SPRUCE_FENCE); }
        people(b, ModEntities.HUMAN.get(), 2, 1, 3, PeopleEntity.GUARD, -1, 12);
    }

    // ================================================================= PLACES (0 semi-hommes, 1 hommes, 2 elfes)
    private static void plaza(Build b, int kind) {
        int c = 12;
        Block path = kind == 0 ? Blocks.DIRT_PATH : kind == 1 ? Blocks.COBBLESTONE : Reg.WHITE_BRICKS.get();
        for (int x = 0; x <= 24; x++) for (int z = 0; z <= 24; z++) {
            if (!b.in(x, 0, z)) continue;
            double d = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
            boolean ring = Math.abs(d - 8) < 1.2;
            boolean spoke = (Math.abs(x - c) <= 1 || Math.abs(z - c) <= 1) && d > 8;
            boolean diag = Math.abs(Math.abs(x - c) - Math.abs(z - c)) <= 1 && d > 8;
            if (!(ring || spoke || diag)) continue;
            int gy = b.groundY(x, z);
            if (gy < -8 || gy > 8) continue;
            BlockState cur = b.get(x, gy, z);
            if (cur.is(Blocks.GRASS_BLOCK) || cur.is(Blocks.DIRT) || cur.is(Blocks.PODZOL) || cur.is(Blocks.COARSE_DIRT)) {
                b.s(x, gy, z, kind == 1 && b.rnd.nextInt(3) == 0 ? Blocks.GRAVEL : path);
                b.s(x, gy + 1, z, Blocks.AIR);
            }
        }
        if (kind == 0) {
            // l'arbre de la fete
            b.foundation(c - 1, c - 1, c + 1, c + 1, Blocks.DIRT);
            b.fill(c, 0, c, c + 1, 9, c + 1, Blocks.OAK_LOG);
            for (int x = -6; x <= 7; x++) for (int y = 6; y <= 14; y++) for (int z = -6; z <= 7; z++) {
                double dd = Math.sqrt(x * x + (y - 10) * (y - 10) * 2.2 + z * z);
                if (dd < 6.3 && (dd < 5.3 || b.rnd.nextInt(3) != 0) && b.get(c + x, y, c + z).isAir())
                    b.s(c + x, y, c + z, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
            }
            for (int[] p : new int[][]{{-3, -2}, {4, -3}, {-2, 4}, {5, 3}, {0, -5}, {1, 6}}) b.s(c + p[0], 5, c + p[1], hang(Blocks.LANTERN));
            for (int[] p : new int[][]{{-4, 0}, {5, 1}}) { b.s(c + p[0], 1, c + p[1], Blocks.OAK_FENCE); b.s(c + p[0], 2, c + p[1], Blocks.OAK_PRESSURE_PLATE); b.s(c + p[0], 1, c + p[1] + 1, Blocks.BARREL); }
        } else {
            Block stone = kind == 1 ? Blocks.STONE_BRICKS : Reg.WHITE_BRICKS.get();
            b.foundation(c - 2, c - 2, c + 2, c + 2, stone);
            b.fill(c - 2, 0, c - 2, c + 2, 1, c + 2, stone);
            b.fill(c - 1, 1, c - 1, c + 1, 1, c + 1, Blocks.WATER);
            b.fill(c, 1, c, c, 3, c, kind == 1 ? Blocks.STONE_BRICK_WALL : Reg.WHITE_PILLAR.get());
            b.s(c, 4, c, kind == 1 ? Blocks.WATER : Blocks.SEA_LANTERN);
            b.air(c - 2, 2, c - 2, c - 1, 6, c + 2); b.air(c + 1, 2, c - 2, c + 2, 6, c + 2);
            for (int[] p : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
                int gy = b.in(c + p[0], 0, c + p[1]) ? b.groundY(c + p[0], c + p[1]) : 0;
                if (kind == 1) {
                    b.fill(c + p[0], gy + 1, c + p[1], c + p[0], gy + 3, c + p[1], Blocks.OAK_FENCE); b.s(c + p[0], gy + 4, c + p[1], Blocks.LANTERN);
                } else {
                    // cerisiers
                    b.fill(c + p[0], gy + 1, c + p[1], c + p[0], gy + 4, c + p[1], Blocks.CHERRY_LOG);
                    for (int x = -3; x <= 3; x++) for (int y = 3; y <= 7; y++) for (int z = -3; z <= 3; z++) {
                        if (x * x + (y - 5) * (y - 5) * 2 + z * z <= 10 && b.get(c + p[0] + x, gy + y, c + p[1] + z).isAir())
                            b.s(c + p[0] + x, gy + y, c + p[1] + z, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
                    }
                    b.s(c + p[0] + 1, gy + 1, c + p[1], Reg.SUNSTAR.get()); b.s(c + p[0], gy + 1, c + p[1] + 1, Reg.MOONBELL.get());
                }
            }
            people(b, kind == 1 ? ModEntities.HUMAN.get() : ModEntities.ELF.get(), c + 4, 1, c, PeopleEntity.GUARD, -1, 20);
            people(b, kind == 1 ? ModEntities.HUMAN.get() : ModEntities.ELF.get(), c - 4, 1, c, PeopleEntity.GUARD, -1, 20);
        }
    }

    // ================================================================= ELFES
    private static void pavilion(Build b, int aux) {
        Block wb = Reg.WHITE_BRICKS.get(), roof = Blocks.DARK_PRISMARINE_STAIRS;
        b.foundation(0, 0, 8, 8, wb);
        b.fill(0, 0, 0, 8, 1, 8, wb);
        b.air(0, 2, 0, 8, 12, 8);
        for (int x = 3; x <= 5; x++) b.s(x, 1, 0, stairs(Reg.WHITE_BRICKS_STAIRS.get(), S, false));
        for (int[] c : new int[][]{{1, 1}, {7, 1}, {1, 7}, {7, 7}}) b.fill(c[0], 2, c[1], c[0], 5, c[1], Reg.WHITE_PILLAR.get());
        b.walls(1, 6, 1, 7, 6, 7, wb);
        for (int i = 2; i <= 6; i += 4) {
            b.s(i, 5, 1, stairs(Reg.WHITE_BRICKS_STAIRS.get(), i == 2 ? W : E, true)); b.s(i, 5, 7, stairs(Reg.WHITE_BRICKS_STAIRS.get(), i == 2 ? W : E, true));
            b.s(1, 5, i, stairs(Reg.WHITE_BRICKS_STAIRS.get(), i == 2 ? N : S, true)); b.s(7, 5, i, stairs(Reg.WHITE_BRICKS_STAIRS.get(), i == 2 ? N : S, true));
        }
        for (int i = 2; i <= 6; i++) { b.s(1, 2, i, Reg.WHITE_BRICKS_WALL.get()); b.s(7, 2, i, Reg.WHITE_BRICKS_WALL.get()); b.s(i, 2, 7, Reg.WHITE_BRICKS_WALL.get()); }
        for (int i = 3; i <= 5; i++) { b.fill(i, 3, 7, i, 4, 7, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE); }
        // toit en pyramide
        for (int k = 0; k <= 3; k++) {
            int lo = k, hi = 8 - k, y = 7 + k;
            for (int i = lo; i <= hi; i++) {
                b.s(i, y, lo, stairs(roof, S, false)); b.s(i, y, hi, stairs(roof, N, false));
                b.s(lo, y, i, stairs(roof, E, false)); b.s(hi, y, i, stairs(roof, W, false));
            }
        }
        b.s(4, 10, 4, Blocks.DARK_PRISMARINE); b.s(4, 11, 4, Reg.WHITE_BRICKS_WALL.get()); b.s(4, 12, 4, Blocks.END_ROD);
        b.s(4, 6, 4, hang(Blocks.SOUL_LANTERN)); b.s(4, 7, 4, Blocks.DARK_PRISMARINE);
        b.s(4, 1, 4, Blocks.SEA_LANTERN);
        b.bed(6, 2, 6, Blocks.WHITE_BED, S);
        b.chest(2, 2, 6, N, "elf_house");
        b.s(2, 2, 2, Blocks.LECTERN); b.s(6, 2, 2, Blocks.BOOKSHELF);
        people(b, ModEntities.ELF.get(), 4, 2, 4, PeopleEntity.CIVIL, aux % 3, 18);
    }

    private static void etower(Build b) {
        Block wb = Reg.WHITE_BRICKS.get();
        b.foundation(0, 0, 6, 6, wb);
        for (int y = 0; y <= 18; y++) for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) {
            boolean corner = (x == 0 || x == 6) && (z == 0 || z == 6);
            boolean edge = x == 0 || x == 6 || z == 0 || z == 6;
            if (corner) { b.s(x, y, z, Blocks.AIR); continue; }
            boolean floor = y == 0 || y == 6 || y == 12 || y == 18;
            if (edge || floor) b.s(x, y, z, (y % 6 == 5 && edge) ? Reg.WHITE_PILLAR.get() : wb); else b.s(x, y, z, Blocks.AIR);
        }
        b.fill(3, 1, 0, 3, 2, 0, Blocks.AIR);
        for (int y = 1; y <= 18; y++) b.s(3, y, 5, ladder(N));
        for (int f = 0; f < 3; f++) {
            int y = 3 + f * 6;
            b.s(0, y, 3, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE); b.s(6, y, 3, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE); b.s(3, y, 0, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
            b.s(1, y + 1, 1, hang(Blocks.SOUL_LANTERN));
        }
        b.walls(0, 19, 0, 6, 19, 6, Reg.WHITE_BRICKS_WALL.get());
        for (int k = 0; k <= 3; k++) {
            int lo = k, hi = 6 - k, y = 20 + k;
            for (int i = lo; i <= hi; i++) {
                b.s(i, y, lo, stairs(Blocks.DARK_PRISMARINE_STAIRS, S, false)); b.s(i, y, hi, stairs(Blocks.DARK_PRISMARINE_STAIRS, N, false));
                b.s(lo, y, i, stairs(Blocks.DARK_PRISMARINE_STAIRS, E, false)); b.s(hi, y, i, stairs(Blocks.DARK_PRISMARINE_STAIRS, W, false));
            }
        }
        for (int[] c : new int[][]{{1, 1}, {5, 1}, {1, 5}, {5, 5}}) b.s(c[0], 19, c[1], Reg.WHITE_PILLAR.get());
        b.s(3, 24, 3, Blocks.END_ROD);
        b.chest(1, 13, 3, E, "elf_house");
        people(b, ModEntities.ELF.get(), 3, 1, 3, PeopleEntity.GUARD, -1, 16);
        people(b, ModEntities.ELF.get(), 3, 7, 3, PeopleEntity.CIVIL, 1, 8);
    }

    // ================================================================= NAINS
    private static void gate(Build b) {
        Block db = Reg.DWARVEN_BRICKS.get();
        b.foundation(0, 0, 14, 7, db);
        b.fill(0, 0, 0, 14, 0, 7, db);
        b.air(0, 1, 0, 14, 12, 1);
        b.fill(0, 1, 2, 14, 11, 7, db);
        b.air(5, 1, 2, 9, 6, 7);
        b.fill(3, 1, 1, 3, 10, 1, Reg.DWARVEN_PILLAR.get()); b.fill(11, 1, 1, 11, 10, 1, Reg.DWARVEN_PILLAR.get());
        b.fill(4, 7, 2, 10, 7, 2, Reg.DWARVEN_RUNES.get());
        b.s(4, 4, 2, Reg.DWARVEN_LAMP.get()); b.s(10, 4, 2, Reg.DWARVEN_LAMP.get());
        b.s(5, 6, 2, stairs(Reg.DWARVEN_BRICKS_STAIRS.get(), E, true)); b.s(9, 6, 2, stairs(Reg.DWARVEN_BRICKS_STAIRS.get(), W, true));
        for (int x = 0; x <= 14; x += 2) b.s(x, 12, 2, db);
        b.s(7, 9, 2, Reg.DWARVEN_RUNES.get()); b.s(7, 5, 5, Reg.DWARVEN_LAMP.get());
        people(b, ModEntities.DWARF.get(), 6, 1, 1, PeopleEntity.GUARD, -1, 6);
        people(b, ModEntities.DWARF.get(), 8, 1, 1, PeopleEntity.GUARD, -1, 6);
    }

    private static void pool(Build b) {
        // mare noire devant la porte, et son guetteur
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 8; z++) {
            double d = Math.pow((x - 5) / 5.5, 2) + Math.pow((z - 4) / 4.5, 2);
            if (d > 1) continue;
            int depth = d < 0.5 ? 5 : 3;
            for (int y = 0; y >= -depth; y--) b.s(x, y, z, y == -depth ? Blocks.MUD : Blocks.WATER);
            b.air(x, 1, z, x, 2, z);
        }
        b.mob(ModEntities.LURKER.get(), 5, -3, 4, 0, null);
    }

    private static void stair(Build b, int len) {
        Block db = Reg.DWARVEN_BRICKS.get();
        for (int i = 0; i <= len + 1; i++) {
            int fy = -Math.min(i, len);
            b.fill(0, fy, i, 6, fy + 7, i, db);
            b.air(1, fy + 1, i, 5, fy + 6, i);
            if (i <= len && i > 0) for (int x = 1; x <= 5; x++) b.s(x, fy + 1, i, stairs(Reg.DWARVEN_BRICKS_STAIRS.get(), N, false));
            if (i % 6 == 3) { b.s(0, fy + 4, i, Reg.DWARVEN_LAMP.get()); b.s(6, fy + 4, i, Reg.DWARVEN_LAMP.get()); }
            if (i % 12 == 9) { b.s(0, fy + 3, i, Reg.DWARVEN_RUNES.get()); b.s(6, fy + 3, i, Reg.DWARVEN_RUNES.get()); }
        }
    }

    private static void dhall(Build b) {
        Block db = Reg.DWARVEN_BRICKS.get();
        b.fill(0, 0, 0, 32, 11, 26, db);
        b.air(1, 1, 1, 31, 10, 25);
        for (int x = 1; x <= 31; x++) for (int z = 1; z <= 25; z++) if ((x + z) % 2 == 0) b.s(x, 0, z, Blocks.POLISHED_DEEPSLATE);
        b.fill(15, 0, 1, 17, 0, 25, Blocks.RED_TERRACOTTA);
        for (int x : new int[]{6, 12, 20, 26}) for (int z : new int[]{5, 11, 17, 22}) {
            b.fill(x, 1, z, x, 10, z, Reg.DWARVEN_PILLAR.get());
            b.s(x, 5, z, Reg.DWARVEN_RUNES.get()); b.s(x, 10, z, Reg.DWARVEN_LAMP.get()); b.s(x, 1, z, db);
            b.s(x + 1, 10, z, Reg.DWARVEN_LAMP.get());
        }
        b.air(14, 1, 0, 18, 6, 0);                 // entree (escalier)
        // sortie vers la vieille mine : porte de fer et bouton
        b.fill(15, 1, 26, 17, 3, 26, Blocks.IRON_BARS);
        b.door(16, 1, 26, Blocks.IRON_DOOR, N);
        b.s(15, 2, 25, Blocks.STONE_BUTTON.defaultBlockState().setValue(net.minecraft.world.level.block.ButtonBlock.FACING, N));
        b.s(18, 2, 25, Reg.DWARVEN_RUNES.get());
        // forge (cote gauche)
        b.fill(1, 1, 3, 1, 1, 9, Blocks.BLAST_FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.BlastFurnaceBlock.FACING, E));
        b.s(3, 1, 4, Blocks.ANVIL); b.s(3, 1, 8, Blocks.ANVIL); for (int fx = 0; fx < 3; fx++) for (int fy = 0; fy < 3; fy++) for (int fz = 0; fz < 3; fz++) b.s(2 + fx, 1 + fy, 5 + fz, Reg.DWARVEN_FORGE.get().defaultBlockState().setValue(net.ringsofash.block.ForgeBlock.OX, fx).setValue(net.ringsofash.block.ForgeBlock.OY, fy).setValue(net.ringsofash.block.ForgeBlock.OZ, fz));
        b.fill(1, 0, 11, 3, 0, 13, Blocks.LAVA); b.fill(1, 1, 11, 3, 1, 13, Blocks.IRON_BARS); b.fill(4, 1, 11, 4, 1, 13, Reg.DWARVEN_BRICKS_WALL.get());
        // taverne (cote droit)
        b.fill(31, 1, 3, 31, 2, 9, Blocks.BARREL);
        for (int z = 4; z <= 8; z += 4) { b.s(28, 1, z, Blocks.DARK_OAK_FENCE); b.s(28, 2, z, Blocks.DARK_OAK_PRESSURE_PLATE); b.s(27, 1, z, stairs(Blocks.DARK_OAK_STAIRS, W, false)); b.s(29, 1, z, stairs(Blocks.DARK_OAK_STAIRS, E, false)); }
        // dortoir et tresor (au fond)
        for (int x = 2; x <= 8; x += 3) b.bed(x, 1, 24, Blocks.BROWN_BED, S);
        b.chest(30, 1, 24, W, "dwarf_hall"); b.chest(30, 1, 22, W, "dwarf_hall"); b.chest(30, 1, 20, W, "dwarf_hall");
        b.fill(28, 1, 24, 29, 1, 24, Blocks.GOLD_BLOCK); b.s(28, 2, 24, CoinPileBlock.of(5));
        for (int i = 0; i < 6; i++) people(b, ModEntities.DWARF.get(), 8 + i * 3, 1, 8 + (i % 3) * 5, PeopleEntity.CIVIL, i % 3, 14);
        people(b, ModEntities.DWARF.get(), 15, 1, 3, PeopleEntity.GUARD, -1, 10);
        people(b, ModEntities.DWARF.get(), 17, 1, 3, PeopleEntity.GUARD, -1, 10);
        people(b, ModEntities.DWARF.get(), 16, 1, 23, PeopleEntity.GUARD, -1, 6);
    }

    private static void mine(Build b) {
        // vieille mine abandonnee : galerie, pont au-dessus du gouffre, gobelins
        b.air(8, 1, 0, 12, 4, 16);
        b.fill(8, 0, 0, 12, 0, 16, Blocks.DEEPSLATE_TILES);
        for (int z = 1; z <= 15; z += 4) {
            b.fill(8, 1, z, 8, 3, z, Blocks.DARK_OAK_FENCE); b.fill(12, 1, z, 12, 3, z, Blocks.DARK_OAK_FENCE); b.fill(8, 4, z, 12, 4, z, Blocks.DARK_OAK_PLANKS);
        }
        for (int z = 0; z <= 16; z++) if (b.rnd.nextInt(4) != 0) b.s(10, 1, z, Blocks.RAIL);
        // gouffre de lave et pont etroit
        b.air(2, -8, 6, 18, 6, 10);
        b.fill(2, -9, 6, 18, -9, 10, Blocks.LAVA);
        b.fill(2, -10, 6, 18, -10, 10, Blocks.DEEPSLATE);
        b.fill(10, 0, 6, 10, 0, 10, Reg.DWARVEN_BRICKS.get());
        // galeries laterales
        b.air(1, 1, 2, 7, 3, 3); b.air(13, 1, 13, 19, 3, 14);
        b.chest(1, 1, 2, E, "mine_ruins"); b.chest(19, 1, 14, W, "mine_ruins");
        for (int i = 0; i < 10; i++) {
            int x = b.rnd.nextBoolean() ? 1 + b.rnd.nextInt(6) : 13 + b.rnd.nextInt(6), z = x < 8 ? 1 : 15;
            b.s(x, 1 + b.rnd.nextInt(3), z, new Block[]{Reg.LUNARGENT_ORE.get(), Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.COBWEB}[b.rnd.nextInt(4)]);
        }
        b.s(9, 3, 3, Blocks.COBWEB); b.s(11, 3, 12, Blocks.COBWEB); b.s(8, 2, 13, Blocks.TORCH);
        for (int i = 0; i < 6; i++) b.mob(ModEntities.GOBLIN.get(), 9 + (i % 3), 1, 1 + (i / 3) * 12 + (i % 2), 0, null);
        b.mob(ModEntities.CAVE_TROLL.get(), 10, 1, 13, 0, null);
        b.mob(ModEntities.CAVE_THIEF.get(), 16, 1, 13, 0, null);
    }

    private static void lair(Build b) {
        Block db = Reg.DWARVEN_BRICKS.get();
        b.fill(0, 0, 0, 26, 14, 26, db);
        b.air(1, 1, 1, 25, 13, 25);
        for (int x = 1; x <= 25; x++) for (int z = 1; z <= 25; z++) {
            double d = Math.sqrt((x - 13) * (x - 13) + (z - 13) * (z - 13));
            if (Math.abs(d - 9) < 0.8 && (x + z) % 5 != 0) b.s(x, 0, z, Blocks.MAGMA_BLOCK);
            else if ((x * 7 + z * 13) % 11 == 0) b.s(x, 0, z, Blocks.CRACKED_DEEPSLATE_BRICKS);
        }
        for (int x : new int[]{5, 21}) for (int z : new int[]{5, 13, 21}) { b.fill(x, 1, z, x + 1, 13, z + 1, Reg.DWARVEN_PILLAR.get()); b.s(x, 7, z, Reg.DWARVEN_RUNES.get()); }
        // balcon d'arrivee (la galerie debouche 6 blocs plus haut) et escalier
        b.air(11, 7, 0, 15, 10, 0);
        b.fill(10, 6, 1, 16, 6, 3, db);
        for (int i = 0; i < 6; i++) b.fill(11, 5 - i, 4 + i, 15, 5 - i, 4 + i, Reg.DWARVEN_BRICKS_STAIRS.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, N));
        for (int i = 0; i < 6; i++) b.fill(11, 1, 4 + i, 15, 4 - i, 4 + i, db);
        b.s(10, 7, 3, Reg.DWARVEN_LAMP.get()); b.s(16, 7, 3, Reg.DWARVEN_LAMP.get());
        // estrade du tresor
        b.fill(9, 1, 22, 17, 1, 25, Blocks.POLISHED_DEEPSLATE);
        b.chest(11, 2, 24, N, "mine_treasure"); b.chest(13, 2, 24, N, "mine_treasure"); b.chest(15, 2, 24, N, "mine_treasure");
        b.s(10, 2, 24, CoinPileBlock.of(4)); b.s(16, 2, 24, CoinPileBlock.of(6)); b.s(13, 2, 23, Reg.DWARVEN_LAMP.get());
        b.mob(ModEntities.FIRE_DEMON.get(), 13, 1, 15, 0, null);
    }

    // ================================================================= ORQUES, TOURS, TERTRES, DRAGON
    private static void fort(Build b) {
        Block bb = Reg.BLACK_BRICKS.get();
        b.foundation(0, 0, 32, 32, Reg.VOLCANIC_ROCK.get());
        b.fill(0, 0, 0, 32, 0, 32, Reg.VOLCANIC_ROCK.get());
        b.air(0, 1, 0, 32, 14, 32);
        b.mix(1, 0, 1, 31, 0, 31, Reg.VOLCANIC_ROCK.get(), Reg.ASH_BLOCK.get(), 3);
        b.walls(0, 1, 0, 32, 6, 32, bb); b.walls(1, 1, 1, 31, 5, 31, bb);
        for (int i = 0; i <= 32; i += 2) { b.s(i, 7, 0, bb); b.s(i, 7, 32, bb); b.s(0, 7, i, bb); b.s(32, 7, i, bb); }
        for (int[] c : new int[][]{{0, 0}, {28, 0}, {0, 28}, {28, 28}}) {
            b.fill(c[0], 1, c[1], c[0] + 4, 10, c[1] + 4, bb); b.air(c[0] + 1, 7, c[1] + 1, c[0] + 3, 10, c[1] + 3);
            for (int i = 0; i <= 4; i += 2) { b.s(c[0] + i, 11, c[1], bb); b.s(c[0] + i, 11, c[1] + 4, bb); b.s(c[0], 11, c[1] + i, bb); b.s(c[0] + 4, 11, c[1] + i, bb); }
            b.s(c[0] + 2, 7, c[1] + 2, Blocks.SOUL_CAMPFIRE);
        }
        b.air(14, 1, 0, 18, 4, 1);                                  // porte
        b.s(14, 5, 0, Blocks.IRON_BARS); b.s(18, 5, 0, Blocks.IRON_BARS);
        // donjon
        b.fill(12, 1, 20, 20, 9, 28, bb); b.air(13, 1, 21, 19, 8, 27); b.air(15, 1, 20, 17, 3, 20);
        for (int i = 12; i <= 20; i += 2) { b.s(i, 10, 20, bb); b.s(i, 10, 28, bb); b.s(12, 10, i + 8, bb); b.s(20, 10, i + 8, bb); }
        b.chest(14, 1, 27, N, "orc_fort"); b.chest(18, 1, 27, N, "orc_fort"); b.s(16, 1, 26, Blocks.SOUL_CAMPFIRE);
        b.s(13, 4, 21, hang(Blocks.SOUL_LANTERN)); b.s(19, 4, 27, hang(Blocks.SOUL_LANTERN));
        // tentes et feux
        for (int[] c : new int[][]{{4, 6}, {24, 6}, {4, 16}, {24, 16}}) {
            for (int i = 0; i <= 2; i++) { b.fill(c[0] + i, 1 + i, c[1], c[0] + 4 - i, 1 + i, c[1] + 4, Blocks.BROWN_WOOL); if (i < 2) b.air(c[0] + i + 1, 1 + i, c[1], c[0] + 3 - i, 1 + i, c[1] + 3); }
            b.s(c[0] + 2, 1, c[1] + 4, Blocks.BROWN_WOOL);
        }
        b.s(16, 1, 10, Blocks.CAMPFIRE); b.s(10, 1, 12, Blocks.CAMPFIRE); b.s(22, 1, 12, Blocks.CAMPFIRE);
        b.fill(7, 1, 26, 9, 3, 28, Blocks.IRON_BARS); b.air(8, 1, 27, 8, 2, 27); b.s(8, 1, 27, Blocks.BONE_BLOCK);
        for (int i = 0; i < 8; i++) b.mob(ModEntities.ORC.get(), 8 + (i % 4) * 5, 1, 5 + (i / 4) * 9, 0, null);
        for (int i = 0; i < 3; i++) b.mob(ModEntities.WAR_ORC.get(), 14 + i * 2, 1, 4, 0, null);
        b.mob(ModEntities.WARG.get(), 6, 1, 22, 0, null); b.mob(ModEntities.WARG.get(), 26, 1, 22, 0, null);
        b.mob(ModEntities.ORC_CHIEF.get(), 16, 1, 24, 0, null);
    }

    private static void dtower(Build b) {
        Block bb = Reg.BLACK_BRICKS.get();
        int c = 6;
        b.foundation(0, 0, 12, 12, bb);
        for (int y = 0; y <= 36; y++) for (int x = 0; x <= 12; x++) for (int z = 0; z <= 12; z++) {
            double d = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
            double r = y < 4 ? 5.6 : 4.6;
            if (d > r) continue;
            boolean wall = d > r - 1.1, floor = y % 6 == 0;
            if (wall || floor) b.s(x, y, z, (y % 6 == 3 && wall && (x + z) % 4 == 0) ? Blocks.IRON_BARS : bb); else b.s(x, y, z, Blocks.AIR);
        }
        b.air(c, 1, 0, c, 2, 2);
        for (int y = 1; y <= 36; y++) b.s(c, y, c + 3, ladder(N));
        for (int y = 5; y <= 35; y += 6) b.s(c - 2, y, c, hang(Blocks.SOUL_LANTERN));
        // couronne de pointes
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int x = c + (int) Math.round(Math.cos(a) * 4.6), z = c + (int) Math.round(Math.sin(a) * 4.6);
            b.fill(x, 37, z, x, 39 + (i % 2) * 3, z, bb);
        }
        b.s(c, 37, c, Blocks.SOUL_SOIL); b.s(c, 38, c, Blocks.SOUL_FIRE);
        b.chest(c + 2, 19, c, W, "dark_tower"); b.chest(c - 2, 37, c - 1, E, "dark_tower");
        for (int i = 0; i < 3; i++) b.mob(ModEntities.ORC.get(), c - 1 + i, 1, c - 1, 0, null);
        b.mob(ModEntities.WAR_ORC.get(), c, 13, c, 0, null);
        b.mob(ModEntities.SORCERER.get(), c, 37, c - 2, 0, null);
    }

    private static void barrow(Build b) {
        int c = 6;
        for (int x = 0; x <= 12; x++) for (int z = 0; z <= 12; z++) {
            double d = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
            if (d > 6.4) continue;
            int hh = (int) Math.round(4.2 * Math.sqrt(Math.max(0, 1 - (d / 6.4) * (d / 6.4))));
            for (int y = -1; y <= hh; y++) b.s(x, y, z, y == hh ? Blocks.GRASS_BLOCK : Blocks.DIRT);
        }
        b.foundation(0, 0, 12, 12, Blocks.DIRT);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + 0.39;
            int x = c + (int) Math.round(Math.cos(a) * 5.6), z = c + (int) Math.round(Math.sin(a) * 5.6);
            int hh = (int) Math.round(4.2 * Math.sqrt(Math.max(0, 1 - 0.76)));
            b.fill(x, hh, z, x, hh + 2 + (i % 2), z, i % 3 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
        }
        // crypte
        b.fill(3, -5, 3, 9, 0, 9, Blocks.MOSSY_STONE_BRICKS);
        b.air(4, -4, 4, 8, -1, 8);
        b.mix(4, -5, 4, 8, -5, 8, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, 3);
        b.s(6, -4, 6, Blocks.CHISELED_STONE_BRICKS); b.chest(6, -3, 6, N, "barrow");
        b.s(4, -2, 4, Blocks.SOUL_LANTERN); b.s(8, -4, 8, Blocks.BONE_BLOCK); b.s(4, -4, 8, Blocks.SKELETON_SKULL);
        // descente depuis la facade
        for (int i = 0; i <= 3; i++) { b.air(6, -i + 1, i, 6, -i + 3, i); b.s(6, -i, i, stairs(Blocks.MOSSY_STONE_BRICK_STAIRS, N, false)); }
        b.air(6, -3, 3, 6, -2, 3); b.air(6, -4, 3, 6, -4, 3); b.s(6, -4, 3, stairs(Blocks.MOSSY_STONE_BRICK_STAIRS, N, false));
        b.mob(EntityType.SKELETON, 5, -4, 7, 0, null); b.mob(EntityType.SKELETON, 7, -4, 5, 0, null);
    }

    private static void hoard(Build b) {
        // tunnel en pente puis grande caverne au tresor
        for (int i = 0; i <= 16; i++) {
            int y = -i;
            b.air(18, y + 1, i, 22, y + 5, i + 1);
            b.fill(18, y, i, 22, y, i + 1, i % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.STONE);
        }
        for (int[] p : new int[][]{{17, 0}, {23, 0}, {17, 2}, {23, 2}}) b.fill(p[0], 1, p[1], p[0], 2, p[1], Blocks.BONE_BLOCK);
        Noise2 n = new Noise2(b.rnd.nextLong());
        for (int x = 2; x <= 38; x++) for (int z = 12; z <= 39; z++) {
            double dx = (x - 20) / 17.0, dz = (z - 26) / 13.0;
            double base = 1 - dx * dx - dz * dz;
            if (base <= 0) continue;
            int top = -16 + (int) Math.round(9 * Math.sqrt(base));
            b.air(x, -22, z, x, top, z);
            b.s(x, -23, z, Blocks.STONE);
            int gold = (int) Math.round(Math.max(0, n.fbm(x * 0.18, z * 0.18) + 0.25) * 4 * Math.sqrt(base));
            for (int y = 0; y < gold; y++) b.s(x, -22 + y, z, y == gold - 1 ? CoinPileBlock.of(3 + b.rnd.nextInt(5)) : Blocks.GOLD_BLOCK.defaultBlockState());
            if (gold == 0 && b.rnd.nextInt(60) == 0) b.s(x, -22, z, Blocks.BONE_BLOCK);
            if (b.rnd.nextInt(90) == 0) b.s(x, top + 1, z, Blocks.SHROOMLIGHT);
        }
        b.chest(20, -22, 34, N, "hoard"); b.chest(14, -22, 30, E, "hoard"); b.chest(26, -22, 30, W, "hoard");
        b.air(18, -22, 16, 22, -16, 20);
        b.mob(ModEntities.DRAGON.get(), 20, -20, 27, 0, null);
    }

    private static void pyre(Build b) {
        b.foundation(0, 0, 4, 4, Blocks.STONE_BRICKS);
        b.fill(0, 0, 0, 4, 9, 4, Blocks.STONE_BRICKS);
        b.air(1, 1, 1, 3, 8, 3);
        b.mix(0, 1, 0, 4, 8, 4, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, 5);
        b.air(1, 1, 1, 3, 8, 3);
        b.air(2, 1, 0, 2, 2, 0);
        for (int y = 1; y <= 9; y++) b.s(2, y, 3, ladder(N));
        b.s(2, 9, 3, ladder(N));
        for (int i = 0; i <= 4; i += 2) { b.s(i, 10, 0, Blocks.STONE_BRICK_WALL); b.s(i, 10, 4, Blocks.STONE_BRICK_WALL); b.s(0, 10, i, Blocks.STONE_BRICK_WALL); b.s(4, 10, i, Blocks.STONE_BRICK_WALL); }
        b.s(2, 10, 1, Reg.ALARM_PYRE.get());
        b.s(1, 5, 1, Blocks.TORCH);
    }

    private static void ruin(Build b, int kind) {
        Block wb = Reg.WHITE_BRICKS.get();
        if (kind == 0) {
            // arche ancienne
            b.foundation(1, 3, 7, 5, wb);
            b.fill(1, 0, 3, 2, 5, 5, wb); b.fill(6, 0, 3, 7, 5, 5, wb);
            b.fill(1, 6, 3, 7, 7, 5, wb);
            b.s(3, 5, 4, stairs(Reg.WHITE_BRICKS_STAIRS.get(), W, true)); b.s(5, 5, 4, stairs(Reg.WHITE_BRICKS_STAIRS.get(), E, true));
            b.s(4, 8, 4, Reg.WHITE_PILLAR.get()); b.s(7, 7, 3, Blocks.AIR); b.s(7, 7, 5, Blocks.AIR); b.s(1, 7, 4, Blocks.MOSS_BLOCK);
        } else if (kind == 1) {
            // cercle de colonnes brisees
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                int x = 4 + (int) Math.round(Math.cos(a) * 3.4), z = 4 + (int) Math.round(Math.sin(a) * 3.4);
                int hh = 1 + b.rnd.nextInt(5);
                b.foundation(x, z, x, z, wb);
                b.fill(x, 0, z, x, hh, z, Reg.WHITE_PILLAR.get());
            }
            b.s(4, 0, 4, Blocks.CHISELED_STONE_BRICKS);
            if (b.rnd.nextInt(3) == 0) b.chest(4, 1, 4, N, "barrow");
        } else {
            // statue voilee
            b.foundation(3, 3, 5, 5, wb);
            b.fill(3, 0, 3, 5, 1, 5, wb);
            b.fill(4, 2, 4, 4, 4, 4, Blocks.STONE); b.s(3, 3, 4, Blocks.STONE_BRICK_WALL); b.s(5, 3, 4, Blocks.STONE_BRICK_WALL);
            b.s(4, 5, 4, Blocks.CHISELED_STONE_BRICKS); b.s(4, 6, 4, Blocks.STONE_BRICK_WALL);
            b.s(4, 2, 3, Blocks.SOUL_LANTERN);
        }
    }
    // ================================================================= MINAS MORGUL
    private static BlockState[] morgulPal;

    private static BlockState[] stairSet(BlockState[] p, int base, Block stairs) {
        Direction[] dirs = {N, E, S, W};
        for (int d = 0; d < 4; d++) {
            p[base + d * 2] = stairs(stairs, dirs[d], false);
            p[base + d * 2 + 1] = stairs(stairs, dirs[d], true);
        }
        return p;
    }

    private static synchronized BlockState[] morgulPal() {
        if (morgulPal != null) return morgulPal;
        BlockState[] p = new BlockState[256];
        java.util.Arrays.fill(p, Build.AIR);
        p[MorgulPlan.STONE] = Reg.MORGUL_BRICKS.get().defaultBlockState();
        p[MorgulPlan.DARK] = Reg.MORGUL_DARK_BRICKS.get().defaultBlockState();
        p[MorgulPlan.DETAIL] = Reg.MORGUL_DETAIL.get().defaultBlockState();
        p[MorgulPlan.PALE] = Reg.MORGUL_PALE_BRICKS.get().defaultBlockState();
        p[MorgulPlan.VEIN] = Reg.MORGUL_VEIN.get().defaultBlockState();
        p[MorgulPlan.HOT] = Reg.MORGUL_VEIN_HOT.get().defaultBlockState();
        p[MorgulPlan.GLOW] = Reg.MORGUL_GLOW.get().defaultBlockState();
        p[MorgulPlan.GLASS] = Blocks.GREEN_STAINED_GLASS.defaultBlockState();
        p[MorgulPlan.BARS] = Blocks.IRON_BARS.defaultBlockState();
        p[MorgulPlan.BLACKSTONE] = Blocks.BLACKSTONE.defaultBlockState();
        p[MorgulPlan.SLAB_S] = slab(Reg.MORGUL_BRICKS_SLAB.get(), false);
        p[MorgulPlan.SLAB_S_TOP] = slab(Reg.MORGUL_BRICKS_SLAB.get(), true);
        p[MorgulPlan.SLAB_P] = slab(Reg.MORGUL_PALE_BRICKS_SLAB.get(), false);
        p[MorgulPlan.SLAB_P_TOP] = slab(Reg.MORGUL_PALE_BRICKS_SLAB.get(), true);
        p[MorgulPlan.SLAB_D] = slab(Reg.MORGUL_DARK_BRICKS_SLAB.get(), false);
        p[MorgulPlan.SLAB_D_TOP] = slab(Reg.MORGUL_DARK_BRICKS_SLAB.get(), true);
        p[MorgulPlan.WALL_S] = Reg.MORGUL_BRICKS_WALL.get().defaultBlockState();
        p[MorgulPlan.WALL_D] = Reg.MORGUL_DARK_BRICKS_WALL.get().defaultBlockState();
        stairSet(p, MorgulPlan.STAIR_S, Reg.MORGUL_BRICKS_STAIRS.get());
        stairSet(p, MorgulPlan.STAIR_P, Reg.MORGUL_PALE_BRICKS_STAIRS.get());
        stairSet(p, MorgulPlan.STAIR_D, Reg.MORGUL_DARK_BRICKS_STAIRS.get());
        morgulPal = p;
        return p;
    }

    /** Minas Morgul : la forme est calculee par MorgulPlan (grille de blocs), ici on la pose dans le monde, morceau par morceau. */
    private static void morgul(Build b) {
        MorgulPlan p = MorgulPlan.get();
        BlockState[] pal = morgulPal();
        Block base = Reg.MORGUL_DARK_BRICKS.get();
        BlockState bottom = Blocks.BLACKSTONE.defaultBlockState(), lit = Reg.MORGUL_GLOW.get().defaultBlockState(), water = net.ringsofash.fluid.ModFluids.ICHOR_BLOCK.get().defaultBlockState();
        int maxY = MorgulPlan.H - MorgulPlan.OFF - 1;
        for (int x = 0; x < MorgulPlan.W; x++) {
            for (int z = 0; z < MorgulPlan.D; z++) {
                if (!b.in(x, 0, z)) continue;
                int top = p.top(x, z);
                int cd = p.chasm(x, z);
                int ground = Math.min(b.groundY(x, z), maxY);
                if (p.get(x, -1, z) != MorgulPlan.AIR) b.foundation(x, z, x, z, base);          // le socle repose sur le sol
                float sw = p.soft(x, z);
                if (cd == 0 && sw > 0.01F) {                                                      // raccord progressif : le relief monte ou descend doucement vers le niveau du pont
                    int target = Math.round(ground * (1 - sw) - sw);
                    if (target > ground) {
                        BlockState surf = b.get(x, ground, z);
                        if (surf.isAir() || !surf.getFluidState().isEmpty()) surf = Blocks.STONE.defaultBlockState();
                        for (int y = ground + 1; y <= target; y++) if (p.get(x, y, z) == MorgulPlan.AIR) b.s(x, y, z, surf);
                    } else {
                        for (int y = ground; y > target; y--) if (p.get(x, y, z) == MorgulPlan.AIR) b.s(x, y, z, Build.AIR);
                    }
                }
                int lo = cd > 0 ? -cd + 1 : p.flat(x, z) ? 0 : Integer.MAX_VALUE;                // on rase le relief des cours et on creuse le gouffre
                for (int y = ground; y >= lo; y--) if (p.get(x, y, z) == MorgulPlan.AIR) b.s(x, y, z, Build.AIR);
                if (cd > 0) {
                    b.s(x, -cd, z, (x * 7 + z * 13) % 6 == 0 ? lit : bottom);
                    b.s(x, -cd - 1, z, bottom);
                    for (int y = -cd + 1; y <= MorgulPlan.WATER_TOP; y++) b.s(x, y, z, water);       // ichor vert (liquide de type lave) au fond du gouffre
                }
                for (int y = -MorgulPlan.OFF; y <= top; y++) {
                    byte id = p.get(x, y, z);
                    if (id != MorgulPlan.AIR) b.s(x, y, z, pal[id]);
                }
            }
        }
        Direction[] dirs = {N, E, S, W};
        for (int[] c : p.chests) b.chest(c[0], c[1], c[2], dirs[c[3]], "dark_tower");
    }

    // ================================================================= ORTHANC
    private static BlockState[] orthancPal;

    private static synchronized BlockState[] orthancPal() {
        if (orthancPal != null) return orthancPal;
        BlockState[] p = new BlockState[256];
        java.util.Arrays.fill(p, Build.AIR);
        final int A = OrthancPlan.AIR;
        p[OrthancPlan.PB] = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        p[OrthancPlan.BS] = Blocks.BLACKSTONE.defaultBlockState();
        p[OrthancPlan.DS] = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        p[OrthancPlan.CRACK] = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        p[OrthancPlan.CHISEL] = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        p[OrthancPlan.GILD] = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        p[OrthancPlan.BARS] = Blocks.IRON_BARS.defaultBlockState();
        p[OrthancPlan.GLASS] = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
        p[OrthancPlan.LANTERN] = hang(Blocks.LANTERN);
        p[OrthancPlan.CHAIN] = Blocks.CHAIN.defaultBlockState();
        p[OrthancPlan.SHELF] = Blocks.BOOKSHELF.defaultBlockState();
        p[OrthancPlan.SLAB_PB] = slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, false);
        p[OrthancPlan.SLAB_PB_TOP] = slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, true);
        p[OrthancPlan.SLAB_BS] = slab(Blocks.BLACKSTONE_SLAB, false);
        p[OrthancPlan.SLAB_BS_TOP] = slab(Blocks.BLACKSTONE_SLAB, true);
        p[OrthancPlan.WALL_PB] = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        p[OrthancPlan.WALL_BS] = Blocks.BLACKSTONE_WALL.defaultBlockState();
        stairSet(p, OrthancPlan.STAIR_PB, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
        stairSet(p, OrthancPlan.STAIR_BS, Blocks.BLACKSTONE_STAIRS);
        stairSet(p, OrthancPlan.STAIR_SB, Blocks.STONE_BRICK_STAIRS);
        stairSet(p, OrthancPlan.STAIR_SP, Blocks.SPRUCE_STAIRS);
        p[OrthancPlan.SB] = Blocks.STONE_BRICKS.defaultBlockState();
        p[OrthancPlan.SB_CRACK] = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        p[OrthancPlan.SB_MOSS] = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        p[OrthancPlan.COBBLE] = Blocks.COBBLESTONE.defaultBlockState();
        p[OrthancPlan.ANDESITE] = Blocks.ANDESITE.defaultBlockState();
        p[OrthancPlan.SLAB_SB] = slab(Blocks.STONE_BRICK_SLAB, false);
        p[OrthancPlan.SLAB_SB_TOP] = slab(Blocks.STONE_BRICK_SLAB, true);
        p[OrthancPlan.WALL_SB] = Blocks.STONE_BRICK_WALL.defaultBlockState();
        p[OrthancPlan.MOSSY_COBBLE] = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        p[OrthancPlan.GRAVEL] = Blocks.GRAVEL.defaultBlockState();
        p[OrthancPlan.PACKED_MUD] = Blocks.PACKED_MUD.defaultBlockState();
        p[OrthancPlan.MUD] = Blocks.COBBLED_DEEPSLATE.defaultBlockState();                  // pas de bloc de l'etiquette "dirt" : l'herbe ne repousserait pas ici
        p[OrthancPlan.PATH] = Blocks.DIRT_PATH.defaultBlockState();
        p[OrthancPlan.TUFF] = Blocks.TUFF.defaultBlockState();
        p[OrthancPlan.COBBLED_DS] = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        p[OrthancPlan.BASALT] = Blocks.BASALT.defaultBlockState();
        p[OrthancPlan.LOG_Y] = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        p[OrthancPlan.LOG_X] = log(Blocks.SPRUCE_LOG, Direction.Axis.X);
        p[OrthancPlan.LOG_Z] = log(Blocks.SPRUCE_LOG, Direction.Axis.Z);
        p[OrthancPlan.PLANKS] = Blocks.SPRUCE_PLANKS.defaultBlockState();
        p[OrthancPlan.SLAB_SP] = slab(Blocks.SPRUCE_SLAB, false);
        p[OrthancPlan.SLAB_SP_TOP] = slab(Blocks.SPRUCE_SLAB, true);
        p[OrthancPlan.FENCE] = Blocks.SPRUCE_FENCE.defaultBlockState();
        p[OrthancPlan.LADDER_N] = ladder(N);
        p[OrthancPlan.LADDER_E] = ladder(E);
        p[OrthancPlan.LADDER_S] = ladder(S);
        p[OrthancPlan.LADDER_W] = ladder(W);
        p[OrthancPlan.DLOG_Y] = log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
        p[OrthancPlan.DLOG_X] = log(Blocks.DARK_OAK_LOG, Direction.Axis.X);
        p[OrthancPlan.DLOG_Z] = log(Blocks.DARK_OAK_LOG, Direction.Axis.Z);
        p[OrthancPlan.DPLANKS] = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        p[OrthancPlan.CAMPFIRE] = Blocks.CAMPFIRE.defaultBlockState();
        p[OrthancPlan.BARREL] = Blocks.BARREL.defaultBlockState();
        p[OrthancPlan.CARPET] = Blocks.RED_CARPET.defaultBlockState();
        p[OrthancPlan.TORCH] = Blocks.TORCH.defaultBlockState();
        p[OrthancPlan.GOLD] = Blocks.GOLD_BLOCK.defaultBlockState();
        p[OrthancPlan.COAL] = Blocks.COAL_BLOCK.defaultBlockState();
        orthancPal = p;
        return p;
    }

    /** Orthanc : tour de 30 x 30 x 140, enceinte ruinee de 200 blocs de diametre et zone de guerre ; la forme vient de OrthancPlan. */
    private static void orthanc(Build b) {
        OrthancPlan p = OrthancPlan.get();
        BlockState[] pal = orthancPal();
        BlockState filler = Blocks.STONE.defaultBlockState(), foot = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        int maxY = OrthancPlan.H - OrthancPlan.OFF - 1;
        for (int x = 0; x < OrthancPlan.W; x++) {
            for (int z = 0; z < OrthancPlan.D; z++) {
                if (!OrthancPlan.disc(x, z) || !b.in(x, 0, z)) continue;
                int top = p.top(x, z);
                boolean lv = p.level(x, z);
                int f = lv ? p.floor(x, z) : -1;
                int ground = Math.min(b.groundY(x, z), maxY);
                if (p.get(x, -1, z) != OrthancPlan.AIR) b.foundation(x, z, x, z, foot, 20);        // tour, enceinte : posees sur le sol ferme
                // on nivelle : tout ce qui depasse le sol de la zone de guerre est rase, les trous sont creuses plus bas
                int hi = Math.max(ground, f) + 14;
                for (int y = hi; y > f; y--) {
                    if (p.get(x, y, z) != OrthancPlan.AIR) continue;
                    if (y > ground && b.get(x, y, z).isAir()) continue;
                    b.s(x, y, z, Build.AIR);
                }
                if (ground < f) for (int y = ground + 1; y < f; y++) if (p.get(x, y, z) == OrthancPlan.AIR) b.s(x, y, z, filler);
                byte cv = lv ? p.cover(x, z) : OrthancPlan.AIR;
                if (cv != OrthancPlan.AIR && p.get(x, f, z) == OrthancPlan.AIR) b.s(x, f, z, pal[cv]);
                else if (lv && ground < f && p.get(x, f, z) == OrthancPlan.AIR) b.s(x, f, z, filler);
                for (int y = -OrthancPlan.OFF; y <= top; y++) {
                    byte id = p.get(x, y, z);
                    if (id != OrthancPlan.AIR) b.s(x, y, z, pal[id]);
                }
            }
        }
        Direction[] dirs = {N, E, S, W};
        for (int[] c : p.chests) b.chest(c[0], c[1], c[2], dirs[c[3]], "dark_tower");
    }

    // ================================================================= BARAD-DUR
    private static BlockState[] baradPal;

    private static synchronized BlockState[] baradPal() {
        if (baradPal != null) return baradPal;
        BlockState[] p = orthancPal().clone();
        p[BaradPlan.LAVA] = Blocks.LAVA.defaultBlockState();
        p[BaradPlan.SBASALT] = Blocks.SMOOTH_BASALT.defaultBlockState();
        p[BaradPlan.DTILE] = Blocks.DEEPSLATE_TILES.defaultBlockState();
        p[BaradPlan.MAGMA] = Blocks.MAGMA_BLOCK.defaultBlockState();
        stairSet(p, BaradPlan.STAIR_DT, Blocks.DEEPSLATE_TILE_STAIRS);
        p[BaradPlan.SLAB_DT] = slab(Blocks.DEEPSLATE_TILE_SLAB, false);
        p[BaradPlan.SLAB_DT_TOP] = slab(Blocks.DEEPSLATE_TILE_SLAB, true);
        p[BaradPlan.WALL_DT] = Blocks.DEEPSLATE_TILE_WALL.defaultBlockState();
        baradPal = p;
        return p;
    }

    /** Barad-dur : forteresse de 80 x 80, tour de 250 blocs sans l'oeil, douve de lave et pont ; la forme vient de BaradPlan. */
    private static void baradDur(Build b) {
        BaradPlan p = BaradPlan.get();
        BlockState[] pal = baradPal();
        BlockState filler = Blocks.STONE.defaultBlockState(), foot = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState[] ash = {Blocks.GRAVEL.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.BASALT.defaultBlockState()};
        int maxY = BaradPlan.H - BaradPlan.OFF - 1;
        for (int x = 0; x < BaradPlan.W; x++) {
            for (int z = 0; z < BaradPlan.D; z++) {
                if (!BaradPlan.area(x, z) || !b.in(x, 0, z)) continue;
                int top = p.top(x, z);
                boolean lv = BaradPlan.level(x, z);
                int ground = Math.min(b.groundY(x, z), maxY);
                int f;
                if (lv) f = p.floor(x, z);
                else f = (int) Math.round(-1 + (ground + 1) * BaradPlan.band(x, z));
                if (p.get(x, -1, z) != OrthancPlan.AIR) b.foundation(x, z, x, z, foot, 20);
                int hi = Math.max(ground, f) + 14;
                for (int y = hi; y > f; y--) {
                    if (p.get(x, y, z) != OrthancPlan.AIR) continue;
                    if (y > ground && b.get(x, y, z).isAir()) continue;
                    b.s(x, y, z, Build.AIR);
                }
                if (ground < f) for (int y = ground + 1; y < f; y++) if (p.get(x, y, z) == OrthancPlan.AIR) b.s(x, y, z, filler);
                if (p.get(x, f, z) == OrthancPlan.AIR) {
                    if (lv) {
                        byte cv = p.cover(x, z);
                        if (cv != OrthancPlan.AIR) b.s(x, f, z, pal[cv]);
                    } else if (ground != f) {
                        b.s(x, f, z, ash[(x * 7 + z * 13 + (x ^ z)) & 3]);
                    }
                }
                for (int y = -BaradPlan.OFF; y <= top; y++) {
                    byte id = p.get(x, y, z);
                    if (id != OrthancPlan.AIR) b.s(x, y, z, pal[id]);
                }
            }
        }
        Direction[] dirs = {N, E, S, W};
        for (int[] c : p.chests) b.chest(c[0], c[1], c[2], dirs[c[3]], "dark_tower");
    }

    // ================================================================= EDORAS
    private static BlockState[] edorasPal;

    private static synchronized BlockState[] edorasPal() {
        if (edorasPal != null) return edorasPal;
        BlockState[] p = orthancPal().clone();
        p[EdorasPlan.STONE] = Blocks.STONE.defaultBlockState();
        p[EdorasPlan.DIRT] = Blocks.DIRT.defaultBlockState();
        p[EdorasPlan.GRASS] = Blocks.GRASS_BLOCK.defaultBlockState();
        p[EdorasPlan.COARSE] = Blocks.COARSE_DIRT.defaultBlockState();
        p[EdorasPlan.GRANITE] = Blocks.GRANITE.defaultBlockState();
        p[EdorasPlan.HAY] = Blocks.HAY_BLOCK.defaultBlockState();
        p[EdorasPlan.MOSAIC] = Blocks.BAMBOO_MOSAIC.defaultBlockState();
        p[EdorasPlan.GLASSPANE] = Blocks.GLASS_PANE.defaultBlockState();
        p[EdorasPlan.DFENCE] = Blocks.DARK_OAK_FENCE.defaultBlockState();
        p[EdorasPlan.BELL] = Blocks.BELL.defaultBlockState();
        p[EdorasPlan.LAMP] = Blocks.LANTERN.defaultBlockState();
        p[EdorasPlan.SLAB_MO] = slab(Blocks.BAMBOO_MOSAIC_SLAB, false);
        p[EdorasPlan.SLAB_MO_TOP] = slab(Blocks.BAMBOO_MOSAIC_SLAB, true);
        p[EdorasPlan.SLAB_DP] = slab(Blocks.DARK_OAK_SLAB, false);
        p[EdorasPlan.SLAB_DP_TOP] = slab(Blocks.DARK_OAK_SLAB, true);
        p[EdorasPlan.WALL_COBBLE] = Blocks.COBBLESTONE_WALL.defaultBlockState();
        p[EdorasPlan.DSTRIP_Y] = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
        p[EdorasPlan.DSTRIP_X] = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X);
        p[EdorasPlan.DSTRIP_Z] = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z);
        // chaume du mod pour les toits, bois patine, torchis, bois sculpte, gerbes, terre battue
        p[EdorasPlan.MOSAIC] = Reg.THATCH.get().defaultBlockState();
        p[EdorasPlan.SLAB_MO] = slab(Reg.THATCH_SLAB.get(), false);
        p[EdorasPlan.SLAB_MO_TOP] = slab(Reg.THATCH_SLAB.get(), true);
        stairSet(p, EdorasPlan.STAIR_MO, Reg.THATCH_STAIRS.get());
        p[EdorasPlan.WLOG_Y] = log(Reg.WEATHERED_LOG.get(), Direction.Axis.Y);
        p[EdorasPlan.WLOG_X] = log(Reg.WEATHERED_LOG.get(), Direction.Axis.X);
        p[EdorasPlan.WLOG_Z] = log(Reg.WEATHERED_LOG.get(), Direction.Axis.Z);
        p[EdorasPlan.WPLANKS] = Reg.WEATHERED_PLANKS.get().defaultBlockState();
        p[EdorasPlan.WSLAB] = slab(Reg.WEATHERED_PLANKS_SLAB.get(), false);
        p[EdorasPlan.WSLAB_TOP] = slab(Reg.WEATHERED_PLANKS_SLAB.get(), true);
        p[EdorasPlan.WFENCE] = Reg.WEATHERED_PLANKS_FENCE.get().defaultBlockState();
        p[OrthancPlan.FENCE] = Reg.WEATHERED_PLANKS_FENCE.get().defaultBlockState();
        p[EdorasPlan.CARVED] = Reg.CARVED_PLANKS.get().defaultBlockState();
        p[EdorasPlan.TIMBER] = Reg.TIMBER_PLASTER.get().defaultBlockState();
        p[EdorasPlan.PLASTER] = Reg.ROHAN_PLASTER.get().defaultBlockState();
        p[EdorasPlan.SHEAF_Y] = log(Reg.SHEAF.get(), Direction.Axis.Y);
        p[EdorasPlan.SHEAF_X] = log(Reg.SHEAF.get(), Direction.Axis.X);
        p[EdorasPlan.EARTH] = Reg.PACKED_EARTH.get().defaultBlockState();
        // vie du village : cultures, eau, cuves, etabli, enclume, fleurs, laine, tapis
        p[EdorasPlan.FARM] = Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
        p[EdorasPlan.WHEAT_R] = Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        p[EdorasPlan.WHEAT_Y] = Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 3);
        p[EdorasPlan.CARROT_R] = Blocks.CARROTS.defaultBlockState().setValue(net.minecraft.world.level.block.CarrotBlock.AGE, 7);
        p[EdorasPlan.CARROT_Y] = Blocks.CARROTS.defaultBlockState().setValue(net.minecraft.world.level.block.CarrotBlock.AGE, 3);
        p[EdorasPlan.POTATO_R] = Blocks.POTATOES.defaultBlockState().setValue(net.minecraft.world.level.block.PotatoBlock.AGE, 7);
        p[EdorasPlan.POTATO_Y] = Blocks.POTATOES.defaultBlockState().setValue(net.minecraft.world.level.block.PotatoBlock.AGE, 3);
        p[EdorasPlan.WATER] = Blocks.WATER.defaultBlockState();
        p[EdorasPlan.CAULDRON_W] = Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3);
        p[EdorasPlan.COMPOSTER] = Blocks.COMPOSTER.defaultBlockState().setValue(net.minecraft.world.level.block.ComposterBlock.LEVEL, 6);
        p[EdorasPlan.CRAFT] = Blocks.CRAFTING_TABLE.defaultBlockState();
        p[EdorasPlan.ANVIL] = Blocks.ANVIL.defaultBlockState();
        p[EdorasPlan.LAVA_CAULD] = Blocks.LAVA_CAULDRON.defaultBlockState();
        p[EdorasPlan.POT_TULIP] = Blocks.POTTED_RED_TULIP.defaultBlockState();
        p[EdorasPlan.POT_DAND] = Blocks.POTTED_DANDELION.defaultBlockState();
        p[EdorasPlan.POT_CORN] = Blocks.POTTED_CORNFLOWER.defaultBlockState();
        p[EdorasPlan.WOOL_G] = Blocks.GREEN_WOOL.defaultBlockState();
        p[EdorasPlan.WOOL_W] = Blocks.WHITE_WOOL.defaultBlockState();
        p[EdorasPlan.PLATE] = Blocks.OAK_PRESSURE_PLATE.defaultBlockState();
        p[EdorasPlan.CARPET_G] = Blocks.GREEN_CARPET.defaultBlockState();
        p[EdorasPlan.CARPET_W] = Blocks.WHITE_CARPET.defaultBlockState();
        p[EdorasPlan.PODZOL] = Blocks.PODZOL.defaultBlockState();
        p[EdorasPlan.ROOTED] = Blocks.ROOTED_DIRT.defaultBlockState();
        p[EdorasPlan.MOSSB] = Blocks.MOSS_BLOCK.defaultBlockState();
        p[EdorasPlan.MUD] = Blocks.MUD.defaultBlockState();
        p[EdorasPlan.FARM_DRY] = Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 3);
        p[EdorasPlan.GRASS_S] = Blocks.SHORT_GRASS.defaultBlockState();
        p[EdorasPlan.FERN] = Blocks.FERN.defaultBlockState();
        p[EdorasPlan.DANDELION] = Blocks.DANDELION.defaultBlockState();
        p[EdorasPlan.POPPY] = Blocks.POPPY.defaultBlockState();
        p[EdorasPlan.CORNFL] = Blocks.CORNFLOWER.defaultBlockState();
        p[EdorasPlan.DAISY] = Blocks.OXEYE_DAISY.defaultBlockState();
        p[EdorasPlan.TALL_L] = Blocks.TALL_GRASS.defaultBlockState();
        p[EdorasPlan.TALL_U] = Blocks.TALL_GRASS.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        p[EdorasPlan.LEAF] = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        p[EdorasPlan.LEAF_AZ] = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        stairSet(p, EdorasPlan.STAIR_W, Reg.WEATHERED_PLANKS_STAIRS.get());
        p[EdorasPlan.BERRY] = Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 3);
        edorasPal = p;
        return p;
    }

    /** Edoras : colline rocheuse, route en lacets, palais d'Or sur sa terrasse, tour de cloche et maisons au toit de chaume ; la forme vient de EdorasPlan. */
    private static boolean softGround(int id) {
        return id == EdorasPlan.GRASS || id == EdorasPlan.COARSE || id == EdorasPlan.EARTH || id == OrthancPlan.PATH || id == EdorasPlan.DIRT || id == EdorasPlan.PODZOL
                || id == EdorasPlan.ROOTED || id == EdorasPlan.MOSSB || id == EdorasPlan.MUD || id == EdorasPlan.FARM || id == EdorasPlan.FARM_DRY || id == OrthancPlan.GRAVEL;
    }

    private static void edoras(Build b) {
        EdorasPlan p = EdorasPlan.get();
        BlockState[] pal = edorasPal();
        BlockState foot = Blocks.STONE.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState();
        int maxY = EdorasPlan.H - EdorasPlan.OFF - 1;
        for (int x = 0; x < EdorasPlan.W; x++) {
            for (int z = 0; z < EdorasPlan.D; z++) {
                int top = p.top(x, z);
                if (top < 0 || !b.in(x, 0, z)) continue;
                int ground = Math.min(b.groundY(x, z), maxY);
                if (p.get(x, 0, z) != OrthancPlan.AIR) b.foundation(x, z, x, z, softGround(p.get(x, 0, z) & 255) ? dirt : foot, 24);   // la colline repose sur le sol ferme
                for (int y = ground; y > top; y--) {                                                     // on rase ce qui depasse du terrain naturel
                    if (y > ground || b.get(x, y, z).isAir()) continue;
                    if (p.get(x, y, z) == OrthancPlan.AIR) b.s(x, y, z, Build.AIR);
                }
                for (int y = 0; y <= top; y++) {
                    byte id = p.get(x, y, z);
                    if (id != OrthancPlan.AIR) b.s(x, y, z, pal[id & 255]);
                }
            }
        }
    }

    // ================================================================= MINAS TIRITH
    private static BlockState[] tirithPal;

    private static synchronized BlockState[] tirithPal() {
        if (tirithPal != null) return tirithPal;
        BlockState[] p = new BlockState[256];
        java.util.Arrays.fill(p, Build.AIR);
        p[TirithPlan.STONE] = Blocks.STONE.defaultBlockState();
        p[TirithPlan.ANDESITE] = Blocks.ANDESITE.defaultBlockState();
        p[TirithPlan.TUFF] = Blocks.TUFF.defaultBlockState();
        p[TirithPlan.CALCITE] = Blocks.CALCITE.defaultBlockState();
        p[TirithPlan.DIORITE] = Blocks.DIORITE.defaultBlockState();
        p[TirithPlan.GRASS] = Blocks.GRASS_BLOCK.defaultBlockState();
        p[TirithPlan.DIRT] = Blocks.DIRT.defaultBlockState();
        p[TirithPlan.SNOW] = Blocks.SNOW_BLOCK.defaultBlockState();
        p[TirithPlan.COBBLE] = Blocks.COBBLESTONE.defaultBlockState();
        p[TirithPlan.GRAVEL] = Blocks.GRAVEL.defaultBlockState();
        p[TirithPlan.WB] = Reg.WHITE_BRICKS.get().defaultBlockState();
        stairSet(p, TirithPlan.WB_STAIR, Reg.WHITE_BRICKS_STAIRS.get());
        p[TirithPlan.WB_SLAB] = slab(Reg.WHITE_BRICKS_SLAB.get(), false);
        p[TirithPlan.WB_SLAB_TOP] = slab(Reg.WHITE_BRICKS_SLAB.get(), true);
        p[TirithPlan.WB_WALL] = Reg.WHITE_BRICKS_WALL.get().defaultBlockState();
        p[TirithPlan.QB] = Blocks.QUARTZ_BRICKS.defaultBlockState();
        p[TirithPlan.SQ] = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        p[TirithPlan.PD] = Blocks.POLISHED_DIORITE.defaultBlockState();
        p[TirithPlan.SBR] = Blocks.STONE_BRICKS.defaultBlockState();
        p[TirithPlan.QPILLAR_Y] = Blocks.QUARTZ_PILLAR.defaultBlockState();
        p[TirithPlan.CHISEL] = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        p[TirithPlan.PAVE] = Reg.WHITE_STONE.get().defaultBlockState();
        p[TirithPlan.PAVE2] = Blocks.SMOOTH_STONE.defaultBlockState();
        p[TirithPlan.GLASS] = Blocks.GLASS_PANE.defaultBlockState();
        p[TirithPlan.BARS] = Blocks.IRON_BARS.defaultBlockState();
        p[TirithPlan.DPLANKS] = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        p[TirithPlan.DLOG_Y] = log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
        p[TirithPlan.LANTERN] = Blocks.LANTERN.defaultBlockState();
        p[TirithPlan.HLANTERN] = Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);
        p[TirithPlan.CHAIN] = Blocks.CHAIN.defaultBlockState();
        p[TirithPlan.FENCE_B] = Blocks.DARK_OAK_FENCE.defaultBlockState();
        p[TirithPlan.WATER] = Blocks.WATER.defaultBlockState();
        p[TirithPlan.BIRCH_Y] = log(Blocks.BIRCH_LOG, Direction.Axis.Y);
        p[TirithPlan.BIRCH_FENCE] = Blocks.BIRCH_FENCE.defaultBlockState();
        // toits et ornements : calcaire jaune pale (SLATE) et pierre blanche (COPPER)
        p[TirithPlan.SLATE] = Reg.LIMESTONE_BRICKS.get().defaultBlockState();
        stairSet(p, TirithPlan.SLATE_STAIR, Reg.LIMESTONE_BRICKS_STAIRS.get());
        p[TirithPlan.SLATE_SLAB] = slab(Reg.LIMESTONE_BRICKS_SLAB.get(), false);
        p[TirithPlan.SLATE_SLAB_TOP] = slab(Reg.LIMESTONE_BRICKS_SLAB.get(), true);
        p[TirithPlan.COPPER] = Reg.WHITE_BRICKS.get().defaultBlockState();
        stairSet(p, TirithPlan.COPPER_STAIR, Reg.WHITE_BRICKS_STAIRS.get());
        p[TirithPlan.COPPER_SLAB] = slab(Reg.WHITE_BRICKS_SLAB.get(), false);
        p[TirithPlan.COPPER_SLAB_TOP] = slab(Reg.WHITE_BRICKS_SLAB.get(), true);
        p[TirithPlan.YB] = Reg.LIMESTONE_BRICKS.get().defaultBlockState();
        p[TirithPlan.KEEL] = Reg.KEEL_STONE.get().defaultBlockState();
        p[TirithPlan.LEAF] = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        p[TirithPlan.WTLOG] = log(Reg.WHITE_TREE_LOG.get(), Direction.Axis.Y);
        p[TirithPlan.WTLEAF] = Reg.WHITE_TREE_LEAVES.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        p[TirithPlan.WTBLOOM] = Reg.WHITE_TREE_BLOOM.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        p[TirithPlan.FLOWER1] = Blocks.WHITE_TULIP.defaultBlockState();
        p[TirithPlan.FLOWER2] = Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
        p[TirithPlan.GRASS_S] = Blocks.SHORT_GRASS.defaultBlockState();
        p[TirithPlan.DWALL] = Blocks.DEEPSLATE_TILE_WALL.defaultBlockState();
        p[TirithPlan.WOOL_W] = Blocks.WHITE_WOOL.defaultBlockState();
        p[TirithPlan.WOOL_G] = Blocks.GRAY_WOOL.defaultBlockState();
        p[TirithPlan.SLAB_STONE] = slab(Blocks.SMOOTH_STONE_SLAB, false);
        p[TirithPlan.QUARTZ_SLAB] = slab(Blocks.QUARTZ_SLAB, false);
        p[TirithPlan.QUARTZ_SLAB_TOP] = slab(Blocks.QUARTZ_SLAB, true);
        p[TirithPlan.GOLD] = Blocks.GOLD_BLOCK.defaultBlockState();
        tirithPal = p;
        return p;
    }

    /** Minas Tirith : sept niveaux de remparts blancs, eperon de roche, tour d'Ecthelion, cour de l'Arbre Blanc, le tout adosse a la montagne ; la forme vient de TirithPlan. */
    private static void tirith(Build b) {
        TirithPlan p = TirithPlan.get();
        BlockState[] pal = tirithPal();
        BlockState foot = Blocks.STONE.defaultBlockState();
        int maxY = TirithPlan.H - TirithPlan.OFF - 1;
        for (int x = 0; x < TirithPlan.W; x++) {
            for (int z = 0; z < TirithPlan.D; z++) {
                int top = p.top(x, z);
                if (top < 0 || !b.in(x, 0, z)) continue;
                int kd = p.kindAt(x, z);
                int ground = Math.min(b.groundY(x, z), maxY);
                if (p.get(x, 0, z) != TirithPlan.AIR) b.foundation(x, z, x, z, foot, 24);              // la cite repose sur le sol ferme
                if (kd != 2 && kd != 5) {
                    for (int y = ground; y > top; y--) {                                             // on rase ce qui depasse du terrain naturel (sauf la montagne, qui reste)
                        if (b.get(x, y, z).isAir()) continue;
                        if (p.get(x, y, z) == TirithPlan.AIR) b.s(x, y, z, Build.AIR);
                    }
                }
                for (int y = -TirithPlan.OFF + 2; y <= top; y++) {
                    byte id = p.get(x, y, z);
                    if (id != TirithPlan.AIR) b.s(x, y, z, pal[id & 255]);
                }
            }
        }
    }
}
