package net.ringsofash.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
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

/** Bete passive : le corbeau espion (il ne se pose jamais et alerte les ennemis s'il voit un porteur d'anneau),
 *  et toute la faune des bois et des rivieres (cerf, chevreuil, ecureuil, canard, heron, castor). */
public class BeastEntity extends PathfinderMob {
    public BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
        if (isCrow()) { this.moveControl = new FlyingMoveControl(this, 12, true); setNoGravity(true); }
        if (swims()) { setPathfindingMalus(PathType.WATER, 0.0F); setPathfindingMalus(PathType.WATER_BORDER, 0.0F); }
    }

    /** les betes qui vivent au bord de l'eau et qui savent nager */
    public boolean swims() {
        return getType() == ModEntities.DUCK.get() || getType() == ModEntities.BEAVER.get() || getType() == ModEntities.HERON.get();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (isCrow()) return Flight.navigation(this, level);
        if (swims()) return new AmphibiousPathNavigation(this, level);
        return super.createNavigation(level);
    }

    public boolean isCrow() { return getType() == ModEntities.CROW.get(); }

    /** les oiseaux d'eau se tiennent a la surface au lieu de couler */
    public boolean floats() {
        return getType() == ModEntities.DUCK.get() || getType() == ModEntities.HERON.get();
    }

    public boolean isSquirrel() { return getType() == ModEntities.SQUIRREL.get(); }

    /**
     * L'ecureuil grimpe aux arbres : des qu'il bute contre un tronc ou des feuilles,
     * Minecraft le laisse monter comme a une echelle.
     */
    @Override
    public boolean onClimbable() {
        if (!isSquirrel()) return super.onClimbable();
        if (super.onClimbable()) return true;
        if (!horizontalCollision) return false;
        var pos = blockPosition();
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var s = level().getBlockState(pos.relative(d));
            if (s.is(net.minecraft.tags.BlockTags.LOGS) || s.is(net.minecraft.tags.BlockTags.LEAVES)) return true;
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, net.minecraft.world.damagesource.DamageSource src) {
        if (isSquirrel()) return false;                 // il retombe toujours sur ses pattes
        return super.causeFallDamage(dist, mult, src);
    }

    @Override
    public boolean canBreatheUnderwater() { return floats() || super.canBreatheUnderwater(); }

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
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, swims() ? 5.0F : 8.0F, 1.2, 1.6));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 12.0F, 1.3, 1.7));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // canard et heron : ils remontent doucement tant qu'ils sont trop enfonces
        if (floats() && isInWater()) {
            double enfonce = getFluidHeight(net.minecraft.tags.FluidTags.WATER);
            var m = getDeltaMovement();
            double monte = enfonce > 0.35 ? 0.035 : 0.0;
            setDeltaMovement(m.x * 0.92, Math.min(m.y * 0.6 + monte, 0.07), m.z * 0.92);
            resetFallDistance();
        }
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
    protected SoundEvent getAmbientSound() {
        if (isCrow()) return SoundEvents.PARROT_AMBIENT;
        var t = getType();
        if (t == ModEntities.DUCK.get() || t == ModEntities.HERON.get()) return SoundEvents.CHICKEN_AMBIENT;
        if (t == ModEntities.SQUIRREL.get() || t == ModEntities.BEAVER.get()) return SoundEvents.RABBIT_AMBIENT;
        if (t == ModEntities.DEER.get() || t == ModEntities.ROE_DEER.get()) return SoundEvents.GOAT_AMBIENT;
        return null;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        if (isCrow()) return SoundEvents.PARROT_HURT;
        var t = getType();
        if (t == ModEntities.DUCK.get() || t == ModEntities.HERON.get()) return SoundEvents.CHICKEN_HURT;
        if (t == ModEntities.SQUIRREL.get() || t == ModEntities.BEAVER.get()) return SoundEvents.RABBIT_HURT;
        return SoundEvents.GOAT_HURT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        if (isCrow()) return SoundEvents.PARROT_DEATH;
        var t = getType();
        if (t == ModEntities.DUCK.get() || t == ModEntities.HERON.get()) return SoundEvents.CHICKEN_DEATH;
        if (t == ModEntities.SQUIRREL.get() || t == ModEntities.BEAVER.get()) return SoundEvents.RABBIT_DEATH;
        return SoundEvents.GOAT_DEATH;
    }
    @Override
    public float getVoicePitch() {
        if (isCrow()) return 0.5F;
        var t = getType();
        if (t == ModEntities.SQUIRREL.get()) return 1.5F;
        if (t == ModEntities.HERON.get()) return 0.6F;
        if (t == ModEntities.DEER.get()) return 0.7F;
        if (t == ModEntities.ROE_DEER.get()) return 0.95F;
        return 1.2F;
    }
}
