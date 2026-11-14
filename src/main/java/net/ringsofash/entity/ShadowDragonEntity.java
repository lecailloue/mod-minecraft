package net.ringsofash.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.RingItem;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Dragon des ombres : la monture du chef des ombres. Le chef est assis sur son dos (il fait partie du modele, comme le cavalier et son cheval).
 * Vole en permanence (cou qui ondule, ailes qui battent), sent de tres loin quiconque porte un anneau, mord et jette la terreur autour de lui.
 */
public class ShadowDragonEntity extends Monster {
    public static final double NORMAL_RANGE = 40.0, RING_RANGE = 96.0;

    public ShadowDragonEntity(EntityType<? extends ShadowDragonEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.moveControl = new FlyingMoveControl(this, 12, true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 260).add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.75).add(Attributes.ARMOR, 10).add(Attributes.FOLLOW_RANGE, RING_RANGE).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new BiteGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new RingSenseGoal(this));
    }

    /** Fonce sur sa cible, la mord quand il est a portee. */
    static class BiteGoal extends Goal {
        private final ShadowDragonEntity mob;
        private int cooldown = 0;

        BiteGoal(ShadowDragonEntity mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { LivingEntity t = mob.getTarget(); return t != null && t.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30F, 30F);
            mob.getMoveControl().setWantedPosition(t.getX(), t.getEyeY() - 0.4, t.getZ(), 1.25);
            if (cooldown > 0) cooldown--;
            double reach = mob.getBbWidth() * 0.5 + t.getBbWidth() + 2.0;
            if (cooldown <= 0 && mob.distanceToSqr(t.getX(), t.getEyeY() - 0.4, t.getZ()) <= reach * reach) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(t);
                cooldown = 26;
            }
        }
    }

    /** Cible les joueurs : 40 blocs en temps normal, 96 blocs (meme invisibles, meme a travers les murs) s'ils portent un anneau. */
    static class RingSenseGoal extends NearestAttackableTargetGoal<Player> {
        RingSenseGoal(ShadowDragonEntity mob) {
            super(mob, Player.class, 10, false, false, null);
            this.targetConditions = TargetingConditions.forCombat().range(RING_RANGE).ignoreInvisibilityTesting().ignoreLineOfSight()
                    .selector(e -> e instanceof Player p && (RingItem.wearsRing(p) || mob.distanceToSqr(p) < NORMAL_RANGE * NORMAL_RANGE));
        }

        @Override
        protected double getFollowDistance() { return RING_RANGE; }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE, getRandomX(0.8), getY() + 1.0 + random.nextDouble() * 1.5, getRandomZ(0.8), 0, 0.01, 0);
            return;
        }
        if (tickCount % 22 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.7F + random.nextFloat() * 0.1F);
        if (tickCount % 40 == 0) {                                       // aura de terreur (comme ses cavaliers)
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(18))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false), this);
                if (RingItem.wearsRing(p)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false), this);
            }
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && getTarget() == null && !level().isClientSide) playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.6F);
        super.setTarget(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), this);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // comme les etres des ombres : craint le feu et les lames des tertres / elfiques ; le reste le blesse un peu moins
        boolean fire = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
        boolean blade = source.getEntity() instanceof LivingEntity le && (le.getMainHandItem().is(Reg.WIGHT_BLADE.get()) || le.getMainHandItem().is(Reg.ELVEN_BLADE.get())
                || le.getMainHandItem().is(Reg.KINGS_BLADE.get()));
        if (fire) amount *= 1.5F;
        else if (!blade && !source.is(DamageTypes.MAGIC) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) amount *= 0.75F;
        return super.hurt(source, amount);
    }

    @Override public boolean causeFallDamage(float dist, float mult, DamageSource source) { return false; }
    @Override public boolean removeWhenFarAway(double dist) { return true; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public int getMaxSpawnClusterSize() { return 1; }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
    @Override public float getVoicePitch() { return 0.6F + random.nextFloat() * 0.1F; }
    @Override public int getAmbientSoundInterval() { return 240; }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }
}
