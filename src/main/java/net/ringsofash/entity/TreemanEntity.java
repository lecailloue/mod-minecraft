package net.ringsofash.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Sylvegarde : gardien de la Vieille-Foret. Paisible, sauf si on le frappe ou si l'on abat des arbres pres de lui. Deteste les orques. */
public class TreemanEntity extends PathfinderMob {
    public TreemanEntity(EntityType<? extends TreemanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, FoeEntity.class, 10, true, false,
                e -> e instanceof FoeEntity f && f.kind() != FoeEntity.Kind.DEAD_SOLDIER && f.kind() != FoeEntity.Kind.MAMMOTH));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.5, 0));
            target.hurtMarked = true;
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.6F);
        }
        return hit;
    }

    public void angerAt(LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        setTarget(e);
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
    @Override
    protected SoundEvent getAmbientSound() { return random.nextInt(3) == 0 ? SoundEvents.WOOD_STEP : null; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_BREAK; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WOOD_BREAK; }
    @Override
    public float getVoicePitch() { return 0.5F; }
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.WOOD_STEP, 0.6F, 0.5F); }
}
