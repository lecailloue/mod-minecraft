package net.ringsofash.item;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.data.ModData;
import net.ringsofash.data.PlayerLore;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Systeme d'artefacts. Un onglet de l'inventaire contient une case de chaine (collier) ; la chaine de niveau n ouvre n cases d'artefacts.
 * Un objet n'est un artefact que s'il porte le composant secret {@link #ARTIFACT} : rien ne le montre au joueur (il faudra l'expertiser chez le forgeron elfe),
 * mais s'il le sait il peut le ranger et profiter de l'effet sans expertise.
 */
public final class Artifacts {
    private Artifacts() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RingsOfAsh.MODID);
    /** drapeau secret : cet exemplaire est un vrai artefact */
    /** rarete d'une arme : 0 commune ... 5 ultime (voir Runes) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RARITY = COMPONENTS.register("rarity_tier",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ARTIFACT = COMPONENTS.register("artifact",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** 1 case de chaine + 4 cases d'artefacts au maximum */
    public static final int SLOTS = 5, MAX_LEVEL = 4;

    public enum Kind { SCALE, FEATHER, HORN }

    /** Objet qui peut etre un artefact (ecaille de dragon, plume d'aigle geant, corne). */
    public static class Piece extends Item {
        public final Kind kind;
        public Piece(Kind kind, Properties p) { super(p); this.kind = kind; }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            // l'attribut est secret : seul l'onglet creatif (aide aux tests) le signale
            if (flag.isCreative() && isArtifact(stack)) tooltip.add(Component.literal("(artefact)").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Chaine en forme de collier : son niveau donne le nombre de cases d'artefacts. */
    public static class Chain extends Item {
        public final int level;
        public Chain(int level, Properties p) { super(p); this.level = level; }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Niveau " + level + " : " + level + " case" + (level > 1 ? "s" : "") + " d'artefact").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Se range dans l'onglet Artefacts.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean isArtifact(ItemStack s) { return s.getItem() instanceof Piece && s.getOrDefault(ARTIFACT.get(), false); }

    public static ItemStack artifact(Item item) {
        ItemStack s = new ItemStack(item);
        s.set(ARTIFACT.get(), true);
        return s;
    }

    /** niveau de la chaine rangee (0 = aucune) */
    public static int level(net.minecraft.world.Container c) { return c.getItem(0).getItem() instanceof Chain ch ? ch.level : 0; }

    // ------------------------------------------------------------------ effets (cote serveur)
    private static final Map<UUID, Boolean> JUMP = new ConcurrentHashMap<>();

    public static void setJump(ServerPlayer sp, boolean on) { if (on) JUMP.put(sp.getUUID(), true); else JUMP.remove(sp.getUUID()); }

    public static void init(IEventBus modBus) {
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Artifacts::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> JUMP.remove(e.getEntity().getUUID()));
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        PlayerLore l = ModData.lore(sp);
        int lv = level(l.artifacts);
        boolean scale = false, feather = false, horn = false;
        for (int i = 1; i <= lv; i++) {
            ItemStack s = l.artifacts.getItem(i);
            if (!isArtifact(s)) continue;
            switch (((Piece) s.getItem()).kind) { case SCALE -> scale = true; case FEATHER -> feather = true; case HORN -> horn = true; }
        }
        if (sp.tickCount % 10 == 0) {
            if (scale) effect(sp, MobEffects.FIRE_RESISTANCE, 60);
            if (horn) effect(sp, MobEffects.DAMAGE_BOOST, 60);
        }
        if (feather && !sp.getAbilities().flying && !sp.isInWater()) {
            // vol plane : saut maintenu = montee lente, sinon chute tres lente
            if (JUMP.containsKey(sp.getUUID())) { MobEffectInstance m = sp.getEffect(MobEffects.LEVITATION); if (m == null || m.getDuration() < 3) effect(sp, MobEffects.LEVITATION, 6); }
            else { MobEffectInstance m = sp.getEffect(MobEffects.SLOW_FALLING); if (m == null || m.getDuration() < 8) effect(sp, MobEffects.SLOW_FALLING, 20); }
        }
    }

    private static void effect(ServerPlayer sp, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> eff, int ticks) {
        sp.addEffect(new MobEffectInstance(eff, ticks, 0, true, false, false));
    }
}
