package net.ringsofash.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.item.RingItem;

/** Corbeau espion : il ne se pose jamais, vole en permanence et alerte les ennemis s'il voit un porteur d'anneau. */
public class BeastEntity extends PathfinderMob {
    public BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
        if (isCrow()) { this.moveControl = new FlyingMoveControl(this, 12, true); setNoGravity(true); }
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return isCrow() ? Flight.navigation(this, level) : super.createNavigation(level); }

    public boolean isCrow() { return getType() == ModEntities.CROW.get(); }

    @Override
    protected void registerGoals() {
        if (isCrow()) {
            goalSelector.addGoal(1, new Flight.FleeGoal(this, 7.0, 0.7));
            goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.4));
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            return;
        }
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 7.0F, 1.2, 1.6));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isCrow()) {
            setNoGravity(true);
            if (!level().isClientSide && tickCount % 100 == 0) {
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(16))) {
                    if (p.isCreative() || p.isSpectator() || !RingItem.wearsRing(p) || !hasLineOfSight(p)) continue;
                    playSound(SoundEvents.PARROT_IMITATE_PHANTOM, 2.0F, 0.7F);
                    for (Monster mob : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(48))) {
                        if (mob instanceof Mob m2 && m2.getTarget() == null) m2.setTarget(p);
                    }
                }
            }
        }
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource source) { return !isCrow() && super.causeFallDamage(dist, mult, source); }
    @Override
    protected SoundEvent getAmbientSound() { return isCrow() ? SoundEvents.PARROT_AMBIENT : null; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return isCrow() ? SoundEvents.PARROT_HURT : SoundEvents.GOAT_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return isCrow() ? SoundEvents.PARROT_DEATH : SoundEvents.GOAT_DEATH; }
    @Override
    public float getVoicePitch() { return isCrow() ? 0.5F : 1.2F; }
}
