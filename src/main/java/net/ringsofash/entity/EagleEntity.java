package net.ringsofash.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Aigle geant : il ne se pose jamais, il vole en permanence. On gagne sa confiance avec de la viande ou du poisson cru, puis on peut voler sur son dos. */
public class EagleEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.BOOLEAN);

    public EagleEntity(EntityType<? extends EagleEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return Flight.navigation(this, level); }

    public boolean isTamed() { return entityData.get(TAMED); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(TAMED, false); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.35) {
            @Override public boolean canUse() { return !isVehicle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isVehicle() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean food = stack.is(Items.RABBIT) || stack.is(Items.SALMON) || stack.is(Items.COD) || stack.is(Items.MUTTON) || stack.is(Items.BEEF) || stack.is(Items.CHICKEN);
        if (food) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                heal(6);
                if (!isTamed()) {
                    boolean ok = random.nextInt(3) == 0;
                    if (ok) { entityData.set(TAMED, true); setPersistenceRequired(); }
                    ((ServerLevel) level()).sendParticles(ok ? ParticleTypes.HEART : ParticleTypes.SMOKE, getX(), getY() + 1.5, getZ(), 7, 0.6, 0.5, 0.6, 0.02);
                    if (ok) player.displayClientMessage(net.minecraft.network.chat.Component.literal("L'aigle vous accorde sa confiance. Clic droit pour monter, Maj pour descendre."), true);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTamed() && !isVehicle() && !player.isSecondaryUseActive()) {
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return isTamed() && getFirstPassenger() instanceof Player p ? p : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setRot(player.getYRot(), player.getXRot() * 0.4F);
        this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float fwd = player.zza;
        double up = -Math.sin(Math.toRadians(player.getXRot())) * Math.max(0, fwd);
        return new Vec3(player.xxa * 0.4, up, fwd > 0 ? fwd : fwd * 0.3);
    }

    @Override
    protected float getRiddenSpeed(Player player) { return (float) getAttributeValue(Attributes.FLYING_SPEED); }

    @Override
    public void travel(Vec3 input) {
        if (isVehicle() && getControllingPassenger() != null) {
            moveRelative(getSpeed() * 0.35F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);                                  // jamais au sol : il plane quand il ne va nulle part
    }

    /** toujours en vol (animation d'ailes) */
    public boolean isFlying() { return true; }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource source) { return false; }
    @Override
    public boolean removeWhenFarAway(double dist) { return !isTamed(); }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.PARROT_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PARROT_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PARROT_DEATH; }
    @Override
    public float getVoicePitch() { return 0.35F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("Tamed", isTamed()); }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(TAMED, tag.getBoolean("Tamed")); }
}
