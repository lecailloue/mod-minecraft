package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ringsofash.data.Net;
import net.ringsofash.effect.ModEffects;
import net.ringsofash.gen.Reg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Coeur de Sylvgarde : 3 minutes sous la forme d'un Sylvgarde (clic gauche : coup de branche ; clic droit : racines). */
public class SylvanHeart extends Item {
    public static final int DURATION = 20 * 60 * 3;
    public static final int ROOTS_COOLDOWN = 20 * 15;
    public static final int ROOTS_TIME = 20 * 10;

    public SylvanHeart(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        if (!level.isClientSide && p instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(ModEffects.SYLVAN_FORM, DURATION, 0, false, false, true));
            sp.setHealth(sp.getMaxHealth());
            ServerLevel sl = sp.serverLevel();
            sl.playSound(null, sp.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 2.0F, 0.4F);
            sl.playSound(null, sp.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 2.0F, 0.6F);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Reg.ANCIENT_LEAVES.get().defaultBlockState()), sp.getX(), sp.getY() + 1.5, sp.getZ(), 80, 0.8, 1.5, 0.8, 0.1);
            broadcast(sp, DURATION);
            sp.displayClientMessage(Component.literal("Vous devenez Sylvgarde pour 3 minutes. Clic droit : racines.").withStyle(ChatFormatting.DARK_GREEN), true);
            if (!sp.getAbilities().instabuild) st.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("Clic droit : devenez Sylvgarde pendant 3 minutes.").withStyle(ChatFormatting.GRAY));
        t.add(Component.literal("Clic gauche : coup de branche. Clic droit : racines (10 s, recharge 15 s).").withStyle(ChatFormatting.DARK_GRAY));
        t.add(Component.literal("Attention : le feu vous inflige trois fois plus de dégâts.").withStyle(ChatFormatting.RED));
    }

    public static boolean isSylvan(LivingEntity e) {
        if (e.level().isClientSide) {
            Long end = CLIENT.get(e.getId());
            if (end != null && e.level().getGameTime() < end) return true;
        }
        return e.hasEffect(ModEffects.SYLVAN_FORM);
    }

    /** Previent les clients qui voient le joueur (et lui-meme) de sa forme. */
    public static void broadcast(ServerPlayer sp, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new Net.SylvanSync(sp.getId(), ticks));
    }

    public static void sendTo(ServerPlayer viewer, ServerPlayer target) {
        MobEffectInstance i = target.getEffect(ModEffects.SYLVAN_FORM);
        if (i != null) PacketDistributor.sendToPlayer(viewer, new Net.SylvanSync(target.getId(), i.getDuration()));
    }

    // ---- cote client : fin de forme (temps de jeu) par entite
    private static final Map<Integer, Long> CLIENT = new HashMap<>();

    public static void clientSync(int id, int ticks) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        if (ticks <= 0) CLIENT.remove(id);
        else CLIENT.put(id, mc.level.getGameTime() + ticks);
    }

    /** Clic droit sous forme de Sylvgarde : des racines jaillissent sous la cible et l'immobilisent. */
    public static void castRoots(ServerPlayer sp) {
        if (!sp.hasEffect(ModEffects.SYLVAN_FORM)) return;
        long now = sp.level().getGameTime();
        long ready = sp.getPersistentData().getLong("roa_roots");
        if (now < ready) {
            sp.displayClientMessage(Component.literal("Les racines repoussent… " + ((ready - now) / 20 + 1) + " s").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vec3 eye = sp.getEyePosition(), end = eye.add(sp.getLookAngle().scale(18));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(sp, eye, end, new AABB(eye, end).inflate(1.5), e -> e instanceof LivingEntity && e.isAlive() && e != sp && !e.isSpectator(), 18 * 18);
        LivingEntity target = hit != null ? (LivingEntity) hit.getEntity() : null;
        if (target == null) {
            target = sp.level().getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(7), m -> m instanceof Enemy && m.isAlive())
                    .stream().min((a, b) -> Double.compare(a.distanceToSqr(sp), b.distanceToSqr(sp))).orElse(null);
        }
        if (target == null) { sp.displayClientMessage(Component.literal("Aucune cible pour vos racines.").withStyle(ChatFormatting.GRAY), true); return; }
        sp.getPersistentData().putLong("roa_roots", now + ROOTS_COOLDOWN);
        target.addEffect(new MobEffectInstance(ModEffects.ROOTED, ROOTS_TIME, 0), sp);
        target.hurt(sp.damageSources().playerAttack(sp), 3.0F);
        ServerLevel sl = sp.serverLevel();
        sl.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, target.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Reg.ANCIENT_LOG.get().defaultBlockState()), target.getX(), target.getY() + 0.5, target.getZ(), 60, target.getBbWidth() * 0.6, 0.6, target.getBbWidth() * 0.6, 0.15);
        sp.displayClientMessage(Component.literal("Racines ! (recharge 15 s)").withStyle(ChatFormatting.DARK_GREEN), true);
    }

}
