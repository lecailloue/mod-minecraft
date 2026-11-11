package net.ringsofash.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Guetteur des profondeurs : tapi dans l'eau, il happe ses proies avec ses tentacules. */
public class LurkerEntity extends Monster {
    public LurkerEntity(EntityType<? extends LurkerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PeopleEntity.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        double d = distanceTo(t);
        getLookControl().setLookAt(t, 30, 30);
        if (d < 14 && tickCount % 30 == 0) {
            // un tentacule saisit la proie et la tire vers l'eau
            Vec3 pull = position().subtract(t.position()).normalize().scale(d > 5 ? 1.1 : 0.6);
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.28, pull.z));
            t.hurtMarked = true;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            playSound(SoundEvents.SQUID_SQUIRT, 1.5F, 0.5F);
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SPLASH, t.getX(), t.getY() + 0.5, t.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        }
        if (d < 4.2 && tickCount % 20 == 0) doHurtTarget(t);
    }

    @Override
    public void travel(Vec3 input) {
        // ne se deplace pas : il flotte juste sous la surface
        Vec3 m = getDeltaMovement();
        double vy = isInWater() ? (level().getFluidState(blockPosition().above()).isEmpty() ? -0.01 : 0.03) : m.y - 0.08;
        setDeltaMovement(m.x * 0.5, vy, m.z * 0.5);
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean canBreatheUnderwater() { return true; }
    @Override
    public boolean isPushedByFluid() { return false; }
    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.GUARDIAN_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ELDER_GUARDIAN_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ELDER_GUARDIAN_DEATH; }
    @Override
    public float getVoicePitch() { return 0.5F; }
}
