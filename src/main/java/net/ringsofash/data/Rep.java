package net.ringsofash.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Reputation aupres des peuples. */
public final class Rep {
    private Rep() {}

    public static void add(ServerPlayer p, People people, int amount, String reason) {
        PlayerLore lore = ModData.lore(p);
        int before = lore.rep(people);
        lore.addRep(people, amount);
        int after = lore.rep(people);
        if (after == before) return;
        ChatFormatting color = amount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        p.displayClientMessage(Component.literal((amount > 0 ? "+" : "") + (after - before) + " réputation chez les " + people.label + " (" + reason + ") : " + after)
                .withStyle(color), true);
        if (!People.rank(before).equals(People.rank(after))) {
            p.displayClientMessage(Component.literal("Les " + people.label + " vous considèrent désormais comme : " + People.rank(after)).withStyle(color), false);
        }
        Net.sync(p);
    }
}
