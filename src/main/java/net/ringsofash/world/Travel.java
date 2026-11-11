package net.ringsofash.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.data.LandsData;
import net.ringsofash.data.ModData;
import net.ringsofash.data.PlayerLore;
import net.ringsofash.gen.Reg;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Passage entre le monde normal et la Hautemarche. */
public final class Travel {
    private Travel() {}

    public static boolean isLands(Level level) { return level.dimension() == ModWorldgen.LANDS; }

    // ------------------------------------------------------------- lave pale
    /** Appele chaque tick pour un objet au sol : un bloc d'or dans le coeur d'un volcan eveille la lave. */
    public static void tickItem(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || item.isRemoved()) return;
        ItemStack stack = item.getItem();
        if (!stack.is(Items.GOLD_BLOCK)) return;
        BlockPos pos = item.blockPosition();
        if (!level.getFluidState(pos).is(FluidTags.LAVA)) {
            pos = pos.below();
            if (!level.getFluidState(pos).is(FluidTags.LAVA)) return;
        }
        if (level.dimension() != Level.OVERWORLD) return;
        if (!level.structureManager().getStructureWithPieceAt(pos, ModWorldgen.VOLCANOES).isValid()) return;
        int n = awaken(level, pos);
        if (n > 0) {
            stack.shrink(1);
            if (stack.isEmpty()) item.discard(); else item.setItem(stack);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 4.0F, 0.6F);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 3.0F, 0.5F);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 200, 6, 1, 6, 0.05);
            for (ServerPlayer p : level.getPlayers(pl -> pl.distanceToSqr(item) < 96 * 96)) {
                p.displayClientMessage(Component.literal("L'or se dissout... la lave pâlit. Un passage s'ouvre.").withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    /** Transforme la surface du lac de lave en lave pale. Renvoie le nombre de blocs changes. */
    public static int awaken(ServerLevel level, BlockPos start) {
        return transformLake(level, start, Reg.PALE_LAVA.get().defaultBlockState(), null);
    }

    /** Remplace la surface d'un lac de lave (2 couches) par un autre bloc. */
    public static int transformLake(ServerLevel level, BlockPos start, BlockState pale, BlockState alt) {
        // remonte jusqu'a la surface du lac
        BlockPos top = start;
        while (level.getFluidState(top.above()).is(FluidTags.LAVA) && top.getY() < level.getMaxBuildHeight()) top = top.above();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(top); seen.add(top);
        int count = 0;
        while (!queue.isEmpty() && count < 3000) {
            BlockPos p = queue.poll();
            if (!level.getFluidState(p).is(FluidTags.LAVA)) continue;
            level.setBlock(p, alt != null && ((p.getX() * 31 + p.getZ() * 17) & 7) == 0 ? alt : pale, 2);
            count++;
            BlockPos below = p.below();
            if (level.getFluidState(below).is(FluidTags.LAVA) && top.getY() - below.getY() < 2) level.setBlock(below, pale, 2);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos q = p.relative(d);
                if (seen.add(q) && level.getFluidState(q).is(FluidTags.LAVA) && !level.getFluidState(q.above()).is(FluidTags.LAVA)) queue.add(q);
            }
        }
        return count;
    }

    // ------------------------------------------------------------- voyages
    public static void enterThroughLava(ServerPlayer p, BlockPos lavaPos) {
        if (p.isOnPortalCooldown()) return;
        ServerLevel from = p.serverLevel();
        if (isLands(from)) return;
        PlayerLore lore = ModData.lore(p);
        // point de retour : au pied du volcan, pas dans le cratere
        BlockPos back = lavaPos;
        StructureStart start = from.structureManager().getStructureWithPieceAt(lavaPos, ModWorldgen.VOLCANOES);
        if (start.isValid()) {
            var bb = start.getBoundingBox();
            back = new BlockPos(bb.maxX() + 6, 0, (bb.minZ() + bb.maxZ()) / 2);
        }
        lore.setOverworld(back);
        toLands(p);
    }

    public static void toLands(ServerPlayer p) {
        MinecraftServer server = p.server;
        ServerLevel lands = server.getLevel(ModWorldgen.LANDS);
        if (lands == null) {
            p.displayClientMessage(Component.literal("La Hautemarche est introuvable (dimension absente).").withStyle(ChatFormatting.RED), false);
            return;
        }
        PlayerLore lore = ModData.lore(p);
        boolean first = !lore.arrived;
        BlockPos dest = (!first && lore.hasLandsPos) ? BlockPos.of(lore.landsPos) : arrival(server, lands);
        teleport(p, lands, dest, !first && lore.hasLandsPos);
        if (first) {
            lore.arrived = true;
            net.ringsofash.GameEvents.onFirstArrival(p);
            p.displayClientMessage(Component.literal("Vous foulez la Hautemarche. Un anneau tiède repose dans votre main...").withStyle(ChatFormatting.GREEN), false);
        }
    }

    public static void toOverworld(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        PlayerLore lore = ModData.lore(p);
        BlockPos dest = lore.hasOverworldPos ? BlockPos.of(lore.overworldPos) : ow.getSharedSpawnPos();
        teleport(p, ow, dest, lore.hasOverworldPos && dest.getY() != 0);
    }

    /** Utilisation de l'anneau des semi-hommes. */
    public static boolean useRing(ServerPlayer p) {
        if (p.tickCount - p.getLastHurtByMobTimestamp() < 100 && p.getLastHurtByMob() != null) {
            p.displayClientMessage(Component.literal("L'anneau reste froid : impossible en plein combat.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        PlayerLore lore = ModData.lore(p);
        if (isLands(p.level())) {
            lore.setLands(p.blockPosition());
            toOverworld(p);
            return true;
        } else if (p.level().dimension() == Level.OVERWORLD) {
            if (!lore.arrived) {
                p.displayClientMessage(Component.literal("L'anneau ne connaît pas encore le chemin.").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            lore.setOverworld(p.blockPosition());
            toLands(p);
            return true;
        }
        p.displayClientMessage(Component.literal("L'anneau n'a aucun pouvoir ici.").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    /** Village d'arrivee (calcule une seule fois par monde). */
    public static BlockPos arrival(MinecraftServer server, ServerLevel lands) {
        LandsData data = LandsData.get(server);
        if (data.hasArrival) return BlockPos.of(data.arrival);
        BlockPos found = null;
        try {
            found = lands.findNearestMapStructure(net.ringsofash.GameEvents.ARRIVAL_VILLAGES, BlockPos.ZERO, 120, false);
        } catch (Exception e) {
            RingsOfAsh.LOGGER.warn("Recherche du village d'arrivee impossible", e);
        }
        if (found == null) {
            Pair<BlockPos, Holder<Biome>> b = lands.findClosestBiome3d(h -> h.is(ModWorldgen.HALFLING_BIOME), new BlockPos(0, 80, 0), 6400, 64, 64);
            found = b != null ? b.getFirst() : BlockPos.ZERO;
        }
        found = new BlockPos(found.getX(), 0, found.getZ());
        data.setArrival(found);
        return found;
    }

    public static void teleport(ServerPlayer p, ServerLevel dest, BlockPos pos, boolean exactY) {
        dest.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        int y = pos.getY();
        if (!exactY || !safe(dest, pos)) {
            y = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
            BlockPos surf = new BlockPos(pos.getX(), y, pos.getZ());
            // evite d'arriver dans l'eau ou la lave : cherche un sol sec a proximite
            if (!dest.getFluidState(surf.below()).isEmpty()) {
                search:
                for (int r = 2; r <= 48; r += 2) {
                    for (int i = 0; i < 16; i++) {
                        double a = i * Math.PI / 8;
                        int x = pos.getX() + (int) (Math.cos(a) * r), z = pos.getZ() + (int) (Math.sin(a) * r);
                        dest.getChunk(x >> 4, z >> 4);
                        int yy = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        BlockPos q = new BlockPos(x, yy, z);
                        if (dest.getFluidState(q.below()).isEmpty() && dest.getFluidState(q).isEmpty()) { pos = q; y = yy; break search; }
                    }
                }
            }
        }
        p.setPortalCooldown();
        p.clearFire();
        p.fallDistance = 0;
        p.teleportTo(dest, pos.getX() + 0.5, y + 0.1, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, true, false));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
        dest.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4F, 1.4F);
        dest.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.05);
    }

    private static boolean safe(ServerLevel level, BlockPos p) {
        if (p.getY() <= level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) return false;
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && level.getFluidState(p).isEmpty() && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty();
    }

    public static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
}
