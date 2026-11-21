package net.ringsofash.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * Chef des ombres : il apparait sur son dragon (un autre mob : on peut le tuer sans toucher au dragon, qui reste alors sans cavalier).
 * Assis sur la selle tant que le dragon vole ; sans lui, il ne touche jamais le sol : il vole de lui-meme, en plus robuste que ses cavaliers.
 */
public class ShadowChiefEntity extends Monster {
    public static final double NORMAL_RANGE = 40.0, RING_RANGE = 96.0;

    public ShadowChiefEntity(EntityType<? extends ShadowChiefEntity> type, Level level) {
        super(type, level);
        this.xpReward = 90;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return Flight.navigation(this, level); }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 150).add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.MOVEMENT_SPEED, 0.38).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ARMOR, 10).add(Attributes.FOLLOW_RANGE, RING_RANGE).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new Flight.AttackGoal(this, 1.1, 1.6));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new RingSenseGoal(this));
    }

    /** Cible les joueurs : 40 blocs en temps normal, 96 blocs (meme invisibles, meme a travers les murs) s'ils portent un anneau. */
    static class RingSenseGoal extends NearestAttackableTargetGoal<Player> {
        RingSenseGoal(ShadowChiefEntity mob) {
            super(mob, Player.class, 10, false, false, null);
            this.targetConditions = TargetingConditions.forCombat().range(RING_RANGE).ignoreInvisibilityTesting().ignoreLineOfSight()
                    .selector(e -> e instanceof Player p && (RingItem.wearsRing(p) || mob.distanceToSqr(p) < NORMAL_RANGE * NORMAL_RANGE));
        }

        @Override
        protected double getFollowDistance() { return RING_RANGE; }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason != MobSpawnType.JOCKEY && level.getLevel().getEntitiesOfClass(ShadowDragonEntity.class, getBoundingBox().inflate(1.5)).isEmpty()) {
            // il arrive toujours sur son dragon (oeuf, apparition naturelle, commande)
            ShadowDragonEntity d = ModEntities.SHADOW_DRAGON.get().create(level.getLevel());
            if (d != null) {
                d.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                d.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
                level.addFreshEntity(d);
                startRiding(d);
            }
        }
        return data;
    }

    /** Assis sur la selle, il regarde dans le sens du dragon. */
    @Override
    public void rideTick() {
        super.rideTick();
        Entity v = getVehicle();
        if (v instanceof ShadowDragonEntity) {
            setYRot(v.getYRot());
            yRotO = v.yRotO;
            yBodyRot = v.getYRot();
            yHeadRot = v.getYRot();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);
        if (level().isClientSide) {
            if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.01, 0);
            return;
        }
        // sa cible devient celle de son dragon
        if (getVehicle() instanceof ShadowDragonEntity d && getTarget() != null && d.getTarget() == null && !d.isTamed()) d.setTarget(getTarget());
        if (tickCount % 40 == 0) {                                       // aura de terreur
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(14))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false), this);
                if (RingItem.wearsRing(p)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false), this);
            }
        }
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

    @Override public boolean removeWhenFarAway(double dist) { return true; }
    @Override public boolean causeFallDamage(float dist, float mult, net.minecraft.world.damagesource.DamageSource source) { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override public float getVoicePitch() { return 0.55F + random.nextFloat() * 0.1F; }
}
