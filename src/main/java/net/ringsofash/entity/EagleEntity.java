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

/** Aigle geant : il ne se pose jamais, il vole en permanence. Tout joueur peut le monter (clic droit), sans rien lui donner ; meme vol que le dragon des ombres. */
public class EagleEntity extends PathfinderMob {
    public EagleEntity(EntityType<? extends EagleEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return Flight.navigation(this, level); }

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
        if (food && getHealth() < getMaxHealth()) {                         // la viande le soigne ; il n'en faut pas pour monter
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                heal(6);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 1.5, getZ(), 7, 0.6, 0.5, 0.6, 0.02);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isVehicle() && !player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                player.startRiding(this);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("WASD pour voler, Espace monte, flèche du bas descend, Maj pour mettre pied à terre."), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** n'importe quel joueur peut le monter, sans rien lui donner */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setRot(player.getYRot(), 0F);
        this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
    }

    /** Meme vol que le dragon des ombres : WASD/ZQSD (avant, arriere, cotes), Espace monte, fleche du bas descend ; sans touche, il plane. */
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa * 0.8, ShadowDragonEntity.VERTICAL.applyAsInt(player) * 0.5, player.zza);
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
    public boolean removeWhenFarAway(double dist) { return !isVehicle(); }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.PARROT_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PARROT_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PARROT_DEATH; }
    @Override
    public float getVoicePitch() { return 0.35F; }

}
