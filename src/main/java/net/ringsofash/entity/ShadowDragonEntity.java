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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.RingItem;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Dragon des ombres : la monture du Chef des ombres, qui est un mob a part assis sur sa selle de metal. Sans cavalier (oeuf du dragon, ou chef tue),
 * contre une pile d'os (64) il se laisse apprivoiser et on peut le chevaucher comme en vol creatif (WASD/ZQSD, Espace monte, fleche du bas descend, Maj = pied a terre).
 * Vole en permanence (cou qui ondule, ailes qui battent), sent de tres loin quiconque porte un anneau, mord et jette la terreur autour de lui.
 */
public class ShadowDragonEntity extends Monster {
    public static final double NORMAL_RANGE = 40.0, RING_RANGE = 96.0;

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ShadowDragonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** hauteur de la selle au-dessus des griffes (blocs) */
    public static final double SEAT = 2.35;

    public ShadowDragonEntity(EntityType<? extends ShadowDragonEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.moveControl = new FlyingMoveControl(this, 12, true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, Optional.empty());
    }

    public boolean ridden() { return getFirstPassenger() instanceof Player; }
    public boolean isTamed() { return entityData.get(OWNER).isPresent(); }
    public boolean isOwner(Entity e) { return e != null && entityData.get(OWNER).map(u -> u.equals(e.getUUID())).orElse(false); }

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
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override public boolean canUse() { return !isTamed() && !ridden() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isTamed() && !ridden() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new RingSenseGoal(this));
    }

    /** Fonce sur sa cible, la mord quand il est a portee. */
    static class BiteGoal extends Goal {
        private final ShadowDragonEntity mob;
        private int cooldown = 0;

        BiteGoal(ShadowDragonEntity mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { LivingEntity t = mob.getTarget(); return t != null && t.isAlive() && !mob.isOwner(t) && mob.getControllingPassenger() == null; }
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
                    .selector(e -> !mob.isTamed() && e instanceof Player p && (RingItem.wearsRing(p) || mob.distanceToSqr(p) < NORMAL_RANGE * NORMAL_RANGE));
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
        setNoGravity(isTamed() ? ridden() : isNoGravity());       // apprivoise et sans cavalier : il se pose
        if (tickCount % 22 == 0 && (!isTamed() || ridden() || !onGround())) playSound(SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.7F + random.nextFloat() * 0.1F);
        if (tickCount % 40 == 0 && !isTamed()) {                         // aura de terreur (comme ses cavaliers)
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
        // comme les etres des ombres : craint le feu ; le reste le blesse un peu moins
        boolean fire = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
        if (fire) amount *= 1.5F;
        else if (!source.is(DamageTypes.MAGIC) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) amount *= 0.75F;
        return super.hurt(source, amount);
    }

    @Override public boolean causeFallDamage(float dist, float mult, DamageSource source) { return false; }
    @Override public boolean removeWhenFarAway(double dist) { return !isTamed(); }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public int getMaxSpawnClusterSize() { return 1; }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
    @Override public float getVoicePitch() { return 0.6F + random.nextFloat() * 0.1F; }
    @Override public int getAmbientSoundInterval() { return 240; }


    // ---------- monte : Chef des ombres sur la selle au depart ; sans lui, 64 os pour l'apprivoiser, puis on le chevauche

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isVehicle()) {                                                     // deja occupe (son maitre, ou un joueur)
            if (!level().isClientSide && getFirstPassenger() instanceof ShadowChiefEntity)
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Le Chef des ombres le monte encore. Il faut le tuer d'abord."), true);
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) return super.mobInteract(player, hand);
        if (isTamed()) {
            if (!isOwner(player)) return InteractionResult.PASS;
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BONE)) {
            if (stack.getCount() < 64) {
                if (!level().isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.literal("Il lui faut une pile entière d'os (64) pour vous laisser monter."), true);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(64);
                entityData.set(OWNER, Optional.of(player.getUUID()));
                setTarget(null);
                setPersistenceRequired();
                getNavigation().stop();
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 2.5, getZ(), 12, 1.2, 0.8, 1.2, 0.02);
                playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.9F);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Le dragon vous obéit. WASD pour avancer, reculer et aller sur les côtés, Espace monte, flèche du bas descend, Maj pour mettre pied à terre."), true);
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canAttack(LivingEntity target) { return !(isTamed() && isOwner(target)) && super.canAttack(target); }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return isTamed() && getFirstPassenger() instanceof Player p && isOwner(p) ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setRot(player.getYRot(), 0F);
        this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
    }

    /** Rempli cote client : +1 Espace (monter), -1 fleche du bas (descendre). */
    public static java.util.function.ToIntFunction<Player> VERTICAL = p -> 0;

    /** Comme en vol creatif : WASD/ZQSD (avant, arriere, cotes) dans le sens du regard a l'horizontale, Espace monte, fleche du bas descend ; sans touche, il plane. */
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa * 0.8, VERTICAL.applyAsInt(player) * 0.5, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) { return (float) getAttributeValue(Attributes.FLYING_SPEED); }

    @Override
    public void travel(Vec3 input) {
        if (isVehicle() && getControllingPassenger() != null) {
            moveRelative(getSpeed() * 0.16F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    /** La selle : le passager est assis sur l'assise de metal (les pieds pendent sous elle). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        double hip = passenger instanceof Player ? 0.7 : 1.05;                 // hauteur de la hanche au-dessus des pieds
        double lift = passenger.getVehicleAttachmentPoint(this).y;
        return new Vec3(0, SEAT - hip + lift, -0.1).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(OWNER).ifPresent(u -> tag.putUUID("Owner", u));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
    }
}
