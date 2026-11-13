package net.ringsofash.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;

/** Teleportation depuis la carte (clic droit sur un lieu connu). */
public final class MapTravel {
    private MapTravel() {}

    public static void teleport(ServerPlayer sp, int x, int z) {
        boolean known = false;
        for (Map.Entry<String, int[]> e : MapData.places().entrySet())
            if (e.getValue()[0] == x && e.getValue()[1] == z) { known = true; break; }
        if (!known) return;                                     // uniquement les lieux de la carte
        ServerLevel lvl = sp.server.getLevel(ModWorldgen.LANDS);
        if (lvl == null) return;
        lvl.getChunk(x >> 4, z >> 4);                            // genere le chunk si besoin
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (sp.isPassenger()) sp.stopRiding();
        ((ServerLevel) sp.level()).sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.5);
        sp.teleportTo(lvl, x + 0.5, y + 0.1, z + 0.5, sp.getYRot(), sp.getXRot());
        sp.fallDistance = 0;
        lvl.playSound(null, pos, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        sp.displayClientMessage(Component.literal("Vous voyagez jusqu'à ce lieu.").withStyle(ChatFormatting.GOLD), true);
    }
}
