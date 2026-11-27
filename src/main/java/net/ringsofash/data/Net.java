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

    /** Position de l'Oeil de Sauron (centre) ; y < 0 : pas d'oeil dans cette dimension. */
    public record EyePos(int x, int y, int z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EyePos> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("eye_pos"));
        public static final StreamCodec<FriendlyByteBuf, EyePos> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeVarInt(v.x); buf.writeInt(v.y); buf.writeVarInt(v.z); },
                buf -> new EyePos(buf.readVarInt(), buf.readInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Carte : teleportation vers un lieu (clic droit). */
    public record MapTeleport(int x, int z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MapTeleport> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("map_tp"));
        public static final StreamCodec<FriendlyByteBuf, MapTeleport> CODEC = StreamCodec.of((buf, v) -> { buf.writeVarInt(v.x); buf.writeVarInt(v.z); }, buf -> new MapTeleport(buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Carte : /anneaux map, le serveur dit au client de tout devoiler. */
    public record RevealMap() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RevealMap> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("map_reveal"));
        public static final StreamCodec<FriendlyByteBuf, RevealMap> CODEC = StreamCodec.of((buf, v) -> { }, buf -> new RevealMap());
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

    /** Ouvre le four portable du Fondeur. */
    public record OpenFurnace() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenFurnace> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("open_furnace"));
        public static final StreamCodec<FriendlyByteBuf, OpenFurnace> CODEC = StreamCodec.of((buf, v) -> { }, buf -> new OpenFurnace());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Ouvre l'onglet d'artefacts. */
    public record OpenArtifacts() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenArtifacts> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("open_artifacts"));
        public static final StreamCodec<FriendlyByteBuf, OpenArtifacts> CODEC = StreamCodec.of((buf, v) -> { }, buf -> new OpenArtifacts());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Touche de saut maintenue ou relachee (plume d'aigle : vol plane). */
    public record ArtifactJump(boolean on) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ArtifactJump> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("artifact_jump"));
        public static final StreamCodec<FriendlyByteBuf, ArtifactJump> CODEC = StreamCodec.of((buf, v) -> buf.writeBoolean(v.on), buf -> new ArtifactJump(buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Metiers : XP et choix du joueur, envoyes au client. */
    public record JobSync(int[] xp, int[] choice) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<JobSync> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("jobs"));
        public static final StreamCodec<FriendlyByteBuf, JobSync> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeVarIntArray(v.xp); buf.writeVarIntArray(v.choice); },
                buf -> new JobSync(buf.readVarIntArray(), buf.readVarIntArray()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Arbre de competences : le joueur choisit une competence (metier, palier 0 a 2, option 1 a 3). */
    public record ChooseSkill(int job, int tier, int opt) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ChooseSkill> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("choose_skill"));
        public static final StreamCodec<FriendlyByteBuf, ChooseSkill> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeVarInt(v.job); buf.writeVarInt(v.tier); buf.writeVarInt(v.opt); },
                buf -> new ChooseSkill(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Touche d'attaque speciale (guerrier). */
    public record SpecialAttack() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpecialAttack> TYPE = new CustomPacketPayload.Type<>(RingsOfAsh.id("special_attack"));
        public static final StreamCodec<FriendlyByteBuf, SpecialAttack> CODEC = StreamCodec.of((buf, v) -> { }, buf -> new SpecialAttack());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Copie cote client des metiers. */
    public static final class ClientJobs {
        public static int[] xp = new int[3];
        public static int[] choice = new int[9];
        private ClientJobs() {}
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
        /** centre de l'Oeil de Sauron dans la dimension actuelle (y = -1 : aucun) */
        public static int eyeX, eyeY = -1, eyeZ;
        /** /anneaux map : toute la carte est devoilee, et tous les lieux nommes */
        public static boolean mapRevealed;
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
        e.registrar("1").playToServer(OpenArtifacts.TYPE, OpenArtifacts.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.menu.ArtifactMenu.open(sp);
        }));
        e.registrar("1").playToServer(OpenFurnace.TYPE, OpenFurnace.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.menu.PocketFurnaceMenu.open(sp);
        }));
        e.registrar("1").playToServer(ArtifactJump.TYPE, ArtifactJump.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) net.ringsofash.item.Artifacts.setJump(sp, payload.on());
        }));
        e.registrar("1").playToClient(JobSync.TYPE, JobSync.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            ClientJobs.xp = payload.xp(); ClientJobs.choice = payload.choice();
        }));
        e.registrar("1").playToServer(ChooseSkill.TYPE, ChooseSkill.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) Jobs.choose(sp, payload.job(), payload.tier(), payload.opt());
        }));
        e.registrar("1").playToServer(SpecialAttack.TYPE, SpecialAttack.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) Jobs.special(sp);
        }));
        e.registrar("1").playToClient(LoreSync.TYPE, LoreSync.CODEC, (payload, ctx) -> {
            ClientLore.rep = payload.rep(); ClientLore.corruption = payload.corruption(); ClientLore.eye = payload.eye(); ClientLore.quest = payload.quest();
        });
        e.registrar("1").playToClient(EyePos.TYPE, EyePos.CODEC, (payload, ctx) -> {
            ClientLore.eyeX = payload.x(); ClientLore.eyeY = payload.y(); ClientLore.eyeZ = payload.z();
        });
        e.registrar("1").playToClient(RevealMap.TYPE, RevealMap.CODEC, (payload, ctx) -> ClientLore.mapRevealed = true);
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
        PacketDistributor.sendToPlayer(p, new JobSync(l.jobXp.clone(), l.jobChoice.clone()));
        PacketDistributor.sendToPlayer(p, new LoreSync(l.rep.clone(), l.corruption, l.eye, l.quest));
        l.fixActiveRing();
        int[] eye = p.level().dimension() == net.ringsofash.world.ModWorldgen.LANDS ? net.ringsofash.world.EyeOfSauron.pos(p.serverLevel()) : null;
        PacketDistributor.sendToPlayer(p, eye == null ? new EyePos(0, -1, 0) : new EyePos(eye[0], eye[1], eye[2]));
        PacketDistributor.sendToPlayer(p, new RingSync(java.util.stream.IntStream.range(0, PlayerLore.RING_SLOTS).mapToObj(l.rings::getItem).map(ItemStack::copy).toList(), l.activeRing));
    }
}
