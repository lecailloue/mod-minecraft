package net.ringsofash.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Attaque des trolls : poursuite (a quatre pattes, voir FoeEntity/GeoModel), puis un enorme coup de poing LENT :
 * les deux poings se levent (WINDUP), s'abattent (STRIKE) puis le troll reprend son souffle (RECOVER).
 * Les degats tombent au tick WINDUP, dans une zone devant lui : on peut donc esquiver en reculant ou en passant sur le cote.
 * Tout ce qui est dans la zone est blesse et projete.
 */
public class TrollSlamGoal extends Goal {
    public static final int WINDUP = 18, STRIKE = 4, RECOVER = 16, COOLDOWN = 10;

    private final FoeEntity troll;
    private final double speed;
    private int tick, cooldown, repath;

    public TrollSlamGoal(FoeEntity troll, double speed) {
        this.troll = troll;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = troll.getTarget();
        return t != null && t.isAlive() && troll.canAttack(t) && !troll.isStone();
    }

    @Override
    public boolean canContinueToUse() { return tick > 0 || canUse(); }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        troll.setAggressive(true);
        repath = 0;
    }

    @Override
    public void stop() {
        troll.setAggressive(false);
        troll.setSlam(0);
        troll.getNavigation().stop();
        tick = 0;
    }

    @Override
    public void tick() {
        if (cooldown > 0) cooldown--;
        LivingEntity t = troll.getTarget();
        if (tick == 0) {
            if (t == null) return;
            troll.getLookControl().setLookAt(t, 30F, 30F);
            if (--repath <= 0) {
                repath = 4 + troll.getRandom().nextInt(7);
                troll.getNavigation().moveTo(t, speed);
            }
            double reach = troll.getBbWidth() + t.getBbWidth() * 0.5 + 1.9;
            if (cooldown <= 0 && troll.distanceToSqr(t) <= reach * reach && troll.getSensing().hasLineOfSight(t)) {
                tick = 1;
                troll.getNavigation().stop();
                troll.playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.6F);
            }
            return;
        }
        troll.getNavigation().stop();
        if (t != null && tick < WINDUP - 6) troll.getLookControl().setLookAt(t, 40F, 40F);   // il verrouille sa direction juste avant de frapper
        tick++;
        troll.setSlam(tick);
        if (tick == WINDUP) impact();
        if (tick >= WINDUP + STRIKE + RECOVER) {
            tick = 0;
            troll.setSlam(0);
            cooldown = COOLDOWN;
        }
    }

    private void impact() {
        if (!(troll.level() instanceof ServerLevel sl)) return;
        boolean chief = troll.kind() == FoeEntity.Kind.TROLL_CHIEF;
        float yaw = troll.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        double dist = troll.getBbWidth() * 0.5 + (chief ? 1.7 : 1.4);
        double r = chief ? 3.0 : 2.6;
        Vec3 c = troll.position().add(fwd.scale(dist));
        AABB box = new AABB(c.x - r, troll.getY() - 0.6, c.z - r, c.x + r, troll.getY() + 2.8, c.z + r);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, o -> o != troll && o.isAlive() && troll.canAttack(o))) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            if (dx * dx + dz * dz > r * r) continue;
            troll.doHurtTarget(e);
        }
        troll.playSound(SoundEvents.RAVAGER_ATTACK, 1.6F, 0.6F);
        troll.playSound(SoundEvents.STONE_BREAK, 2.0F, 0.5F);
        troll.playSound(SoundEvents.ANVIL_LAND, 0.8F, 0.5F);
        sl.sendParticles(ParticleTypes.POOF, c.x, troll.getY() + 0.1, c.z, 24, r * 0.5, 0.1, r * 0.5, 0.06);
        BlockState ground = sl.getBlockState(BlockPos.containing(c.x, troll.getY() - 0.5, c.z));
        if (!ground.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, troll.getY() + 0.2, c.z, 40, r * 0.5, 0.1, r * 0.5, 0.2);
    }
}
