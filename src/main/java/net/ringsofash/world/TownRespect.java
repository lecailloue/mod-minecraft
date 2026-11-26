package net.ringsofash.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.ringsofash.data.People;
import net.ringsofash.data.Rep;

/**
 * Casser des blocs chez les gens, ca se paie.
 *
 * Chaque bloc casse dans l'enceinte d'une ville compte des points de grief : presque rien
 * pour une motte de terre, beaucoup pour un mur taille, enormement pour un minerai. Dix
 * points de grief valent un point de reputation en moins aupres du peuple de la ville.
 *
 * Ce que le joueur a pose lui-meme ne compte pas : on retient ses poses le temps de la
 * partie, pour qu'il puisse batir et se reprendre sans se faire detester.
 */
public final class TownRespect {
    private TownRespect() {}

    /** dix points de grief = un point de reputation */
    private static final int PER_POINT = 10;

    private static final Map<UUID, Integer> GRIEF = new HashMap<>();
    private static final Set<Long> PLACED = new HashSet<>();
    private static final int PLACED_MAX = 40000;

    public static void init(IEventBus bus) {
        bus.addListener(TownRespect::onBreak);
        bus.addListener(TownRespect::onPlace);
    }

    /** ce que le joueur a pose, il peut le reprendre */
    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer)) return;
        if (Towns.owner(e.getPos().getX(), e.getPos().getZ()) == null) return;
        if (PLACED.size() > PLACED_MAX) PLACED.clear();
        PLACED.add(e.getPos().asLong());
    }

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
        People who = Towns.owner(e.getPos().getX(), e.getPos().getZ());
        if (who == null) return;
        if (PLACED.remove(e.getPos().asLong())) return;

        int grief = grief(e.getState());
        if (grief <= 0) return;
        int total = GRIEF.merge(p.getUUID(), grief, Integer::sum);
        int lost = total / PER_POINT;
        if (lost <= 0) return;
        GRIEF.put(p.getUUID(), total - lost * PER_POINT);
        Rep.add(p, who, -lost, raid(e.getState()) ? "pillage" : "dégâts en ville");
    }

    /** ce que vaut le bloc aux yeux de ses habitants */
    private static int grief(BlockState s) {
        if (raid(s)) return 50;                                   // un minerai, c'est du vol pur
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(BlockTags.CROPS)
                || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)) return 2;
        if (worked(s)) return 5;                                  // un mur, une porte, une vitre
        if (s.getBlock().defaultDestroyTime() <= 0.0F) return 0;   // herbe haute, torche posee a terre
        return 1;                                                 // terre, pierre, sable
    }

    /** minerai ou bloc precieux : la ville le prend tres mal */
    private static boolean raid(BlockState s) {
        if (s.is(Tags.Blocks.ORES) || s.is(Tags.Blocks.STORAGE_BLOCKS)) return true;
        var id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.endsWith("_ore") || path.equals("ancient_debris");
    }

    /** un bloc faconne de main d'homme : il a fallu le batir */
    private static boolean worked(BlockState s) {
        var id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if (id == null) return false;
        String p = id.getPath();
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS)
                || s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.STAIRS) || s.is(BlockTags.SLABS)
                || s.is(BlockTags.FENCES) || s.is(BlockTags.WALLS) || s.is(BlockTags.WOOL)
                || s.is(BlockTags.BEDS) || s.is(BlockTags.CANDLES)
                || p.contains("glass") || p.contains("brick") || p.contains("lantern")
                || p.contains("chest") || p.contains("barrel") || p.contains("table")
                || p.contains("tiles") || p.contains("polished") || p.contains("chiseled");
    }
}
