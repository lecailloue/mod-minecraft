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

/** Araignee geante du Bois-Noir : venin, toiles, et peur de la fiole de lumiere. */
public class GiantSpiderEntity extends Spider {
    public GiantSpiderEntity(EntityType<? extends GiantSpiderEntity> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.2, 1.5,
                e -> e instanceof Player p && (p.getMainHandItem().is(Reg.LIGHT_PHIAL.get()) || p.getOffhandItem().is(Reg.LIGHT_PHIAL.get()))));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1), this);
            BlockPos p = le.blockPosition();
            if (random.nextInt(3) == 0 && level().getBlockState(p).isAir() && !level().isClientSide) level().setBlock(p, Blocks.COBWEB.defaultBlockState(), 3);
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
