package net.ringsofash.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.ringsofash.gen.Reg;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Araignee geante du Bois-Noir : venin, toiles, et peur de la fiole de lumiere. */
public class GiantSpiderEntity extends Spider {
    public GiantSpiderEntity(EntityType<? extends GiantSpiderEntity> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    /** Pas de cavalier : l'araignee du Bois-Noir ne porte jamais de squelette sur le dos. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                                                   net.minecraft.world.DifficultyInstance difficulty,
                                                                   net.minecraft.world.entity.MobSpawnType type,
                                                                   net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        for (Entity rider : getPassengers()) rider.discard();      // l'araignee vanilla monte parfois un squelette
        return out;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new WebSprayGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.2, 1.5,
                e -> e instanceof Player p && (p.getMainHandItem().is(Reg.LIGHT_PHIAL.get()) || p.getOffhandItem().is(Reg.LIGHT_PHIAL.get()))));
    }

    /** Sa propre toile ne la retient pas (la toile vanilla non plus). */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        if (state.is(Reg.SPIDER_WEB.get())) return;
        super.makeStuckInBlock(state, multiplier);
    }

    /** Crache de la toile sur une cible a distance : fils qui volent, toile au pied (et a hauteur de tete), ralentissement. */
    static class WebSprayGoal extends Goal {
        private final GiantSpiderEntity m;
        private int cool = 80;
        WebSprayGoal(GiantSpiderEntity m) { this.m = m; }

        @Override public boolean canUse() {
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive()) return false;
            if (cool > 0) { cool--; return false; }
            double d = m.distanceToSqr(t);
            return d > 25 && d < 20 * 20 && m.hasLineOfSight(t);
        }

        @Override public boolean canContinueToUse() { return false; }

        @Override public void start() {
            LivingEntity t = m.getTarget();
            cool = 140 + m.getRandom().nextInt(80);
            if (t == null || !(m.level() instanceof ServerLevel sl)) return;
            Vec3 from = m.getEyePosition().add(0, -0.4, 0), to = t.getBoundingBox().getCenter();
            ItemParticleOption str = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.STRING));
            for (int i = 0; i <= 18; i++) {
                Vec3 q = from.lerp(to, i / 18.0);
                sl.sendParticles(str, q.x, q.y, q.z, 3, 0.08, 0.08, 0.08, 0.02);
            }
            m.playSound(SoundEvents.SPIDER_AMBIENT, 1.6F, 0.5F);
            BlockPos feet = t.blockPosition();
            for (BlockPos q : new BlockPos[]{feet, feet.above()}) {
                if (sl.getBlockState(q).isAir()) sl.setBlock(q, Reg.SPIDER_WEB.get().defaultBlockState(), 3);
            }
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), m);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1), this);
            BlockPos p = le.blockPosition();
            if (random.nextInt(3) == 0 && level().getBlockState(p).isAir() && !level().isClientSide) level().setBlock(p, Reg.SPIDER_WEB.get().defaultBlockState(), 3);
        }
        return hit;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player p && (p.getMainHandItem().is(Reg.LIGHT_PHIAL.get()) || p.getOffhandItem().is(Reg.LIGHT_PHIAL.get()))) return false;
        return super.canAttack(target);
    }

    @Override
    public float getVoicePitch() { return 0.5F + random.nextFloat() * 0.1F; }
}
