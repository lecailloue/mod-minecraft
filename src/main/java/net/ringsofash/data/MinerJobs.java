package net.ringsofash.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Metier de mineur : XP au minage, trois voies (niveau 5) et leurs options (niveau 10).
 * Voie 1 "Maitre main" : mine plus vite ; options : fortune / veine entiere / pioche incassable.
 * Voie 2 "Grosse main" : mine en 2x2 ; options : 3x3 / veine entiere / fortune.
 * Voie 3 "Fondeur" : les minerais cassés fondent ; options : four dans l'inventaire / fortune / veine entiere.
 */
public final class MinerJobs {
    private MinerJobs() {}

    private static final int FORTUNE = 3;
    private static final float SPEED_MULT = 1.4F;
    private static final int VEIN_MAX = 48;

    static void init(IEventBus bus) {
        bus.addListener(MinerJobs::onSpeed);
        bus.addListener(EventPriority.LOWEST, false, BlockEvent.BreakEvent.class, MinerJobs::onBreak);
        bus.addListener(MinerJobs::onDrops);
        bus.addListener(MinerJobs::onTick);
    }

    // ------------------------------------------------------------------ choix du joueur
    private static int path(ServerPlayer p) { return Jobs.choice(p, Jobs.Job.MINER, 0); }
    private static int opt(ServerPlayer p) { return Jobs.choice(p, Jobs.Job.MINER, 1); }

    private static boolean fortune(ServerPlayer p) {
        int a = path(p), o = opt(p);
        return (a == 1 && o == 1) || (a == 2 && o == 3) || (a == 3 && o == 2);
    }

    private static boolean vein(ServerPlayer p) {
        int a = path(p), o = opt(p);
        return (a == 1 && o == 2) || (a == 2 && o == 2) || (a == 3 && o == 3);
    }

    /** taille de la zone creusee : 0 = aucune, 2 ou 3 */
    private static int area(ServerPlayer p) { return path(p) == 2 ? (opt(p) == 1 ? 3 : 2) : 0; }

    // ------------------------------------------------------------------ blocs
    static boolean isOre(BlockState s) {
        return s.is(Tags.Blocks.ORES) || BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("_ore");
    }

    /** "iron_ore" et "deepslate_iron_ore" font partie du meme filon */
    private static String family(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().replace("deepslate_", ""); }

    private static int xpFor(BlockState s, ServerLevel lv, BlockPos pos) {
        float hard = s.getDestroySpeed(lv, pos);
        if (isOre(s)) return Math.max(4, Math.min(30, Math.round(4 + hard * 2)));
        return 0;
    }

    private static final Map<UUID, Integer> STONE = new HashMap<>();

    // ------------------------------------------------------------------ vitesse de minage (Maitre main)
    private static void onSpeed(PlayerEvent.BreakSpeed e) {
        if (e.getEntity() instanceof ServerPlayer p && path(p) == 1 && e.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) e.setNewSpeed(e.getNewSpeed() * SPEED_MULT);
    }

    // ------------------------------------------------------------------ casse d'un bloc
    private static final Set<UUID> BUSY = new HashSet<>();
    private static final Map<UUID, ItemStack> UNB = new HashMap<>();

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative() || !(e.getLevel() instanceof ServerLevel lv)) return;
        BlockState st = e.getState();
        BlockPos pos = e.getPos();
        // XP : minerais selon leur durete, pierre au compte-gouttes (1 XP tous les 4 blocs)
        int xp = xpFor(st, lv, pos);
        if (xp > 0) Jobs.addXp(p, Jobs.Job.MINER, xp);
        else if (st.is(BlockTags.MINEABLE_WITH_PICKAXE) && st.getDestroySpeed(lv, pos) >= 1.0F) {
            int n = STONE.merge(p.getUUID(), 1, Integer::sum);
            if (n >= 4) { STONE.put(p.getUUID(), 0); Jobs.addXp(p, Jobs.Job.MINER, 1); }
        }
        if (BUSY.contains(p.getUUID()) || path(p) == 0) return;
        ItemStack tool = p.getMainHandItem();
        // pioche incassable : le composant disparait a la fin du tick
        if (path(p) == 1 && opt(p) == 3 && tool.is(ItemTags.PICKAXES) && !tool.has(DataComponents.UNBREAKABLE)) {
            tool.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
            UNB.put(p.getUUID(), tool);
        }
        if (!tool.is(ItemTags.PICKAXES) || !st.is(BlockTags.MINEABLE_WITH_PICKAXE)) return;
        List<BlockPos> extra = new ArrayList<>();
        if (vein(p) && isOre(st)) extra = veinOf(lv, pos, st);
        else if (area(p) > 0) extra = areaOf(p, lv, pos, area(p));
        if (extra.isEmpty()) return;
        BUSY.add(p.getUUID());
        try {
            for (BlockPos q : extra) {
                if (p.getMainHandItem().isEmpty()) break;
                BlockState s = lv.getBlockState(q);
                if (s.isAir() || !s.is(BlockTags.MINEABLE_WITH_PICKAXE) || s.getDestroySpeed(lv, q) < 0 || !p.hasCorrectToolForDrops(s)) continue;
                p.gameMode.destroyBlock(q);
            }
        } finally {
            BUSY.remove(p.getUUID());
        }
    }

    /** filon connecte (26 voisins), meme famille de minerai, sans depasser VEIN_MAX blocs en plus de l'origine */
    private static List<BlockPos> veinOf(ServerLevel lv, BlockPos origin, BlockState st) {
        String fam = family(st);
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        seen.add(origin);
        todo.add(origin);
        while (!todo.isEmpty() && out.size() < VEIN_MAX) {
            BlockPos c = todo.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos q = c.offset(dx, dy, dz);
                if (!seen.add(q) || out.size() >= VEIN_MAX) continue;
                BlockState s = lv.getBlockState(q);
                if (isOre(s) && family(s).equals(fam)) { out.add(q); todo.add(q); }
            }
        }
        return out;
    }

    /** zone de n x n blocs dans le plan de la face visee (n = 2 : le quart de face vise ; n = 3 : centree) */
    private static List<BlockPos> areaOf(ServerPlayer p, ServerLevel lv, BlockPos pos, int n) {
        Direction face;
        Vec3 loc;
        HitResult hr = p.pick(p.blockInteractionRange() + 1.0, 1.0F, false);
        if (hr instanceof BlockHitResult bh && bh.getBlockPos().equals(pos)) {
            face = bh.getDirection();
            loc = bh.getLocation();
        } else {
            face = Direction.getNearest(p.getLookAngle()).getOpposite();
            loc = Vec3.atCenterOf(pos);
        }
        Direction.Axis a1, a2;
        switch (face.getAxis()) {
            case Y -> { a1 = Direction.Axis.X; a2 = Direction.Axis.Z; }
            case X -> { a1 = Direction.Axis.Y; a2 = Direction.Axis.Z; }
            default -> { a1 = Direction.Axis.X; a2 = Direction.Axis.Y; }
        }
        int[] r1, r2;
        if (n == 3) { r1 = new int[] {-1, 0, 1}; r2 = new int[] {-1, 0, 1}; }
        else { r1 = new int[] {0, frac(loc, pos, a1) >= 0.5 ? 1 : -1}; r2 = new int[] {0, frac(loc, pos, a2) >= 0.5 ? 1 : -1}; }
        List<BlockPos> out = new ArrayList<>();
        for (int i : r1) for (int j : r2) {
            if (i == 0 && j == 0) continue;
            out.add(pos.offset(comp(a1, i, a2, j, Direction.Axis.X), comp(a1, i, a2, j, Direction.Axis.Y), comp(a1, i, a2, j, Direction.Axis.Z)));
        }
        return out;
    }

    private static double frac(Vec3 loc, BlockPos pos, Direction.Axis a) {
        return a.choose(loc.x - pos.getX(), loc.y - pos.getY(), loc.z - pos.getZ());
    }

    private static int comp(Direction.Axis a1, int i, Direction.Axis a2, int j, Direction.Axis want) {
        return want == a1 ? i : want == a2 ? j : 0;
    }

    // ------------------------------------------------------------------ butin : fortune puis fonte
    private static void onDrops(BlockDropsEvent e) {
        if (!(e.getBreaker() instanceof ServerPlayer p) || path(p) == 0 || !isOre(e.getState())) return;
        boolean fort = fortune(p), melt = path(p) == 3;
        if (!fort && !melt) return;
        ServerLevel lv = e.getLevel();
        RandomSource rng = lv.random;
        for (ItemEntity ie : e.getDrops()) {
            ItemStack st = ie.getItem();
            if (st.getItem() == e.getState().getBlock().asItem()) continue; // toucher de soie : le bloc lui-meme
            int count = st.getCount();
            if (fort) count *= 1 + Math.max(0, rng.nextInt(FORTUNE + 2) - 1);
            ItemStack out = st.copyWithCount(count);
            if (melt) {
                ItemStack r = smelt(lv, st);
                if (!r.isEmpty()) out = r.copyWithCount(r.getCount() * count);
            }
            ie.setItem(out);
        }
    }

    /** resultat de fonte d'un objet (1 unite), ou vide s'il n'y a pas de recette */
    private static ItemStack smelt(ServerLevel lv, ItemStack in) {
        SingleRecipeInput inp = new SingleRecipeInput(in.copyWithCount(1));
        Optional<RecipeHolder<SmeltingRecipe>> r = lv.getRecipeManager().getRecipeFor(RecipeType.SMELTING, inp, lv);
        return r.map(h -> h.value().assemble(inp, lv.registryAccess())).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ tick : pioche incassable, four d'inventaire
    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack unb = UNB.remove(p.getUUID());
        if (unb != null) unb.remove(DataComponents.UNBREAKABLE);
        if (path(p) == 3 && opt(p) == 1 && p.tickCount % 30 == 0 && p.level() instanceof ServerLevel lv) {
            for (ItemStack s : p.getInventory().items) {
                if (s.isEmpty() || !(s.is(Tags.Items.ORES) || s.is(Tags.Items.RAW_MATERIALS))) continue;
                ItemStack r = smelt(lv, s);
                if (r.isEmpty()) continue;
                s.shrink(1);
                if (!p.getInventory().add(r)) p.drop(r, false);
                break;
            }
        }
    }
}
