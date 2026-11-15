package net.ringsofash.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.RingItem;

import javax.annotation.Nullable;

/**
 * Etre des ombres : cavalier noir sous une grande cape, rien sous la capuche. Ne sort que la nuit, tres rapide, et sent de tres loin
 * quiconque porte un anneau. Le cheval fait partie du modele (une seule entite). Le chef (casque a pointes) est un autre type d'entite, qui chevauche un dragon.
 */
public class ShadowRiderEntity extends Monster {
    public static final double NORMAL_RANGE = 20.0, RING_RANGE = 96.0;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ShadowRiderEntity.class, EntityDataSerializers.INT);
    private int dayGrace = 0;

    public ShadowRiderEntity(EntityType<? extends ShadowRiderEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    /** Modele : 0 et 1 = deux cavaliers (cape et pose differentes), tous deux sur le cheval cuirasse. */
    public int shadowModel() { return Math.max(0, Math.min(1, entityData.get(VARIANT))); }
    public void testVariant(int v) { entityData.set(VARIANT, v); dayGrace = 1_000_000; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, 0);
    }

    public void setSummoned(int ticks) { this.dayGrace = ticks; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new RingSenseGoal(this));
    }

    /** Cible les joueurs : 20 blocs en temps normal, 96 blocs (meme invisibles, meme a travers les murs) s'ils portent un anneau. */
    static class RingSenseGoal extends NearestAttackableTargetGoal<Player> {
        RingSenseGoal(ShadowRiderEntity mob) {
            super(mob, Player.class, 10, false, false, null);
            this.targetConditions = TargetingConditions.forCombat().range(RING_RANGE).ignoreInvisibilityTesting().ignoreLineOfSight()
                    .selector(e -> e instanceof Player p && (RingItem.wearsRing(p) || (mob.distanceToSqr(p) < NORMAL_RANGE * NORMAL_RANGE && mob.hasLineOfSight(p))));
        }

        @Override
        protected double getFollowDistance() { return RING_RANGE; }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Reg.WIGHT_BLADE.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        entityData.set(VARIANT, level.getRandom().nextInt(2));
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.01, 0);
            return;
        }
        if (dayGrace > 0) dayGrace--;
        if (tickCount % 40 == 0) {
            // aura de terreur
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(9))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false), this);
                if (RingItem.wearsRing(p)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false), this);
            }
        }
    }

    public void vanish() {
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.4, 0.9, 0.4, 0.02);
        playSound(SoundEvents.PHANTOM_DEATH, 1.0F, 0.5F);
        discard();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && getTarget() == null && !level().isClientSide) playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.2F, 0.6F);
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
        // craint le feu et les lames des tertres ; le reste le blesse peu
        boolean fire = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
        boolean blade = source.getEntity() instanceof LivingEntity le && (le.getMainHandItem().is(Reg.WIGHT_BLADE.get()) || le.getMainHandItem().is(Reg.ELVEN_BLADE.get())
                || le.getMainHandItem().is(Reg.KINGS_BLADE.get()));
        if (fire) amount *= 2.0F;
        else if (!blade && !source.is(DamageTypes.MAGIC) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) amount *= 0.5F;
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return true; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.PHANTOM_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PHANTOM_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PHANTOM_DEATH; }
    @Override
    public float getVoicePitch() { return 0.45F + random.nextFloat() * 0.1F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("DayGrace", dayGrace); tag.putInt("Variant", entityData.get(VARIANT)); }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); dayGrace = tag.getInt("DayGrace"); entityData.set(VARIANT, tag.getInt("Variant")); }
}
