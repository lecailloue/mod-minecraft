package net.ringsofash.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.ringsofash.RingsOfAsh;

/** Synchronisation des donnees du joueur vers son client (menu de reputation). */
public final class Net {
    private Net() {}

    public record LoreSync(int[] rep, int corruption, int eye, int quest) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<LoreSync> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("lore"));
        public static final StreamCodec<FriendlyByteBuf, LoreSync> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeVarIntArray(v.rep); buf.writeVarInt(v.corruption); buf.writeVarInt(v.eye); buf.writeVarInt(v.quest); },
                buf -> new LoreSync(buf.readVarIntArray(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Copie cote client des dernieres valeurs recues. */
    public static final class ClientLore {
        public static int[] rep = new int[People.values().length];
        public static int corruption, eye, quest;
        private ClientLore() {}
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(LoreSync.TYPE, LoreSync.CODEC, (payload, ctx) -> {
            ClientLore.rep = payload.rep(); ClientLore.corruption = payload.corruption(); ClientLore.eye = payload.eye(); ClientLore.quest = payload.quest();
        });
    }

    public static void sync(ServerPlayer p) {
        PlayerLore l = ModData.lore(p);
        PacketDistributor.sendToPlayer(p, new LoreSync(l.rep.clone(), l.corruption, l.eye, l.quest));
    }
}
