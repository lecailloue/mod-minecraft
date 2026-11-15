package net.ringsofash.world;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ringsofash.data.ModData;
import net.ringsofash.data.Net;
import net.ringsofash.data.PlayerLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Decouverte des lieux : un lieu apparait sur la carte quand son chunk est charge autour du joueur (ou par la commande /anneaux map). */
public final class MapDiscover {
    /** Distance (en blocs) a laquelle un lieu est decouvert, si son chunk est charge. */
    public static final int RADIUS = 128;
    private MapDiscover() {}

    public static String pretty(String key) {
        String name = key.replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Verifie les lieux proches du joueur ; a appeler regulierement en Hautemarche. */
    public static void scan(ServerPlayer p, PlayerLore lore) {
        List<String> found = new ArrayList<>();
        double px = p.getX(), pz = p.getZ();
        for (Map.Entry<String, int[]> e : MapData.places().entrySet()) {
            if (lore.discovered.contains(e.getKey())) continue;
            int x = e.getValue()[0], z = e.getValue()[1];
            double dx = x - px, dz = z - pz;
            if (dx * dx + dz * dz > (double) RADIUS * RADIUS) continue;
            if (!p.serverLevel().hasChunk(x >> 4, z >> 4)) continue;      // la zone doit etre chargee
            found.add(e.getKey());
        }
        if (found.isEmpty()) return;
        lore.discovered.addAll(found);
        for (String k : found) p.displayClientMessage(Component.literal("Lieu découvert : " + pretty(k)).withStyle(ChatFormatting.GOLD), false);
        Net.sync(p);
    }

    /** Decouvre tous les lieux d'un coup (commande). Renvoie le nombre de lieux nouvellement decouverts. */
    public static int discoverAll(ServerPlayer p) {
        PlayerLore lore = ModData.lore(p);
        int n = 0;
        for (String k : MapData.places().keySet()) if (lore.discovered.add(k)) n++;
        Net.sync(p);
        return n;
    }
}
