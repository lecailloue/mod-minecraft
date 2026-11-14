package net.ringsofash.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import java.util.List;
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

    /** Onglet d'anneaux : contenu des 10 emplacements et anneau actif, envoyes au client. */
    public record RingSync(List<ItemStack> stacks, int active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RingSync> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("rings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RingSync> CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_LIST_STREAM_CODEC, RingSync::stacks, ByteBufCodecs.VAR_INT, RingSync::active, RingSync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Menu Alt : choix de l'anneau actif (numero d'emplacement, -1 = aucun). */
    public record SelectRing(int slot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SelectRing> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("select_ring"));
        public static final StreamCodec<FriendlyByteBuf, SelectRing> CODEC = StreamCodec.of((buf, v) -> buf.writeVarInt(v.slot + 1), buf -> new SelectRing(buf.readVarInt() - 1));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Ouvre l'onglet d'anneaux. */
    public record OpenRings() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenRings> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("open_rings"));
        public static final StreamCodec<FriendlyByteBuf, OpenRings> CODEC = StreamCodec.of((buf, v) -> { }, buf -> new OpenRings());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Copie cote client des anneaux ranges. */
    public static final class ClientRings {
        public static final ItemStack[] stacks = new ItemStack[PlayerLore.RING_SLOTS];
        public static int active = -1;
        static { java.util.Arrays.fill(stacks, ItemStack.EMPTY); }
        public static ItemStack active() { return active >= 0 && active < stacks.length ? stacks[active] : ItemStack.EMPTY; }
        private ClientRings() {}
    }

    /** Copie cote client des dernieres valeurs recues. */
    public static final class ClientLore {
        public static int[] rep = new int[People.values().length];
        public static int corruption, eye, quest;
        private ClientLore() {}
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(RingSync.TYPE, RingSync.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            for (int i = 0; i < ClientRings.stacks.length; i++) ClientRings.stacks[i] = i < payload.stacks().size() ? payload.stacks().get(i) : ItemStack.EMPTY;
            ClientRings.active = payload.active();
        }));
        e.registrar("1").playToServer(SelectRing.TYPE, SelectRing.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.menu.RingsMenu.select(sp, payload.slot());
        }));
        e.registrar("1").playToServer(OpenRings.TYPE, OpenRings.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.menu.RingsMenu.open(sp);
        }));
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
        l.fixActiveRing();
        PacketDistributor.sendToPlayer(p, new RingSync(java.util.stream.IntStream.range(0, PlayerLore.RING_SLOTS).mapToObj(l.rings::getItem).map(ItemStack::copy).toList(), l.activeRing));
    }
}
