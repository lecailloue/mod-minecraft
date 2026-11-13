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

    /** Carte : teleportation vers un lieu (clic droit). */
    public record MapTeleport(int x, int z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MapTeleport> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("map_tp"));
        public static final StreamCodec<FriendlyByteBuf, MapTeleport> CODEC = StreamCodec.of((buf, v) -> { buf.writeVarInt(v.x); buf.writeVarInt(v.z); }, buf -> new MapTeleport(buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Forme de Sylvgarde : clic droit = racines. */
    public record CastRoots() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CastRoots> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("roots"));
        public static final StreamCodec<FriendlyByteBuf, CastRoots> CODEC = StreamCodec.of((buf, v) -> { }, buf -> new CastRoots());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Forme de Sylvgarde d'un joueur, pour les clients qui le voient (duree en ticks, 0 = fin). */
    public record SylvanSync(int entity, int ticks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SylvanSync> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("sylvan"));
        public static final StreamCodec<FriendlyByteBuf, SylvanSync> CODEC = StreamCodec.of((buf, v) -> { buf.writeVarInt(v.entity); buf.writeVarInt(v.ticks); }, buf -> new SylvanSync(buf.readVarInt(), buf.readVarInt()));
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
        e.registrar("1").playToServer(MapTeleport.TYPE, MapTeleport.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.world.MapTravel.teleport(sp, payload.x(), payload.z());
        }));
        e.registrar("1").playToServer(CastRoots.TYPE, CastRoots.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.item.SylvanHeart.castRoots(sp);
        }));
        e.registrar("1").playToClient(SylvanSync.TYPE, SylvanSync.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> net.ringsofash.item.SylvanHeart.clientSync(payload.entity(), payload.ticks())));
    }

    public static void sync(ServerPlayer p) {
        PlayerLore l = ModData.lore(p);
        PacketDistributor.sendToPlayer(p, new LoreSync(l.rep.clone(), l.corruption, l.eye, l.quest));
    }
}
