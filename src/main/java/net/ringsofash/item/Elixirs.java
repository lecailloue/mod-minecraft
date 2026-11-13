package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Potions, runes et petits objets utilitaires de la Hautemarche. */
public final class Elixirs {
    private Elixirs() {}

    public record Fx(Holder<MobEffect> effect, int ticks, int amp) {}

    private static void apply(LivingEntity e, Fx[] fx, float heal, boolean cure) {
        if (cure) {
            for (MobEffectInstance i : new ArrayList<>(e.getActiveEffects()))
                if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(i.getEffect());
        }
        if (heal > 0) e.heal(heal);
        for (Fx f : fx) e.addEffect(new MobEffectInstance(f.effect(), f.ticks(), f.amp()));
    }

    /** Boisson : effets, soin immediat, purge des effets nefastes ; rend la fiole vide. */
    public static class Potion extends Item {
        private final String text; private final float heal; private final boolean cure; private final Fx[] fx;
        public Potion(String text, float heal, boolean cure, Properties p, Fx... fx) { super(p); this.text = text; this.heal = heal; this.cure = cure; this.fx = fx; }
        @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.DRINK; }
        @Override public int getUseDuration(ItemStack s, LivingEntity e) { return 32; }
        @Override public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) { return ItemUtils.startUsingInstantly(l, p, h); }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide) {
                apply(entity, fx, heal, cure);
                level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (entity instanceof Player p && !p.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) return bottle;
                if (!p.getInventory().add(bottle)) p.drop(bottle, false);
            }
            return stack;
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) { t.add(Component.literal(text).withStyle(ChatFormatting.GRAY)); }
    }

    /** Rune : usage instantane, consommee. */
    public static class Rune extends Item {
        private final String text; private final Fx[] fx;
        public Rune(String text, Properties p, Fx... fx) { super(p); this.text = text; this.fx = fx; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand h) {
            ItemStack st = p.getItemInHand(h);
            if (!level.isClientSide) {
                apply(p, fx, 0, false);
                level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
                ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.5);
            }
            if (!p.getAbilities().instabuild) st.shrink(1);
            return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) { t.add(Component.literal(text).withStyle(ChatFormatting.GRAY)); }
    }

    /** Plume de vent : bond vers l'avant puis chute lente. Durabilite limitee. */
    public static class Feather extends Item {
        public Feather(Properties p) { super(p); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand h) {
            ItemStack st = p.getItemInHand(h);
            if (!level.isClientSide) {
                Vec3 look = p.getLookAngle();
                p.setDeltaMovement(look.x * 1.6, Math.max(look.y * 1.2, 0.0) + 0.7, look.z * 1.6);
                p.hurtMarked = true;
                p.fallDistance = 0;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.4F);
                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 20, 0.3, 0.1, 0.3, 0.05);
                st.hurtAndBreak(1, p, LivingEntity.getSlotForHand(h));
            }
            p.getCooldowns().addCooldown(this, 300);
            return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("Clic droit : bondit dans la direction du regard, puis chute lentement (recharge 15 s).").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Eclat de rappel : maintenir 3 secondes pour revenir au lit (ou au point d'apparition du monde). */
    public static class Recall extends Item {
        public Recall(Properties p) { super(p); }
        @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
        @Override public int getUseDuration(ItemStack s, LivingEntity e) { return 60; }
        @Override public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) { return ItemUtils.startUsingInstantly(l, p, h); }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide && entity instanceof ServerPlayer sp) {
                BlockPos bed = sp.getRespawnPosition();
                ServerLevel target = bed != null ? sp.server.getLevel(sp.getRespawnDimension()) : sp.server.overworld();
                if (target == null) target = sp.server.overworld();
                BlockPos pos = bed != null ? bed : target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getSharedSpawnPos());
                ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.5);
                sp.teleportTo(target, pos.getX() + 0.5, pos.getY() + (bed != null ? 0.1 : 0), pos.getZ() + 0.5, sp.getYRot(), 0);
                sp.fallDistance = 0;
                target.playSound(null, pos, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!sp.getAbilities().instabuild) stack.shrink(1);
            }
            return stack;
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("Maintenir clic droit 3 s : vous ramène à votre lit, ou au point d'apparition du monde.").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Poudre de fumee : aveugle et ralentit les monstres proches, vous rend invisible un instant. */
    public static class Smoke extends Item {
        public Smoke(Properties p) { super(p); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand h) {
            ItemStack st = p.getItemInHand(h);
            if (!level.isClientSide) {
                ServerLevel sl = (ServerLevel) level;
                for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8), m -> m instanceof Enemy)) {
                    m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
                    m.setTarget(null);
                }
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
                sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 80, 1.5, 0.8, 1.5, 0.02);
            }
            p.getCooldowns().addCooldown(this, 100);
            if (!p.getAbilities().instabuild) st.shrink(1);
            return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("Aveugle et ralentit les monstres à 8 blocs ; vous devenez invisible 8 s.").withStyle(ChatFormatting.GRAY));
        }
    }
}
