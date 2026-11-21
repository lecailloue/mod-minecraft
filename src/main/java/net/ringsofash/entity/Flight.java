package net.ringsofash.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Outils communs aux mobs qui ne se deplacent qu'en volant (aigle, corbeau, Vieux Ver, chef des ombres, dragon des ombres). */
public final class Flight {
    private Flight() {}

    /** Navigation aerienne : jamais de marche, les portes ne sont pas un obstacle. */
    public static PathNavigation navigation(Mob mob, Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(mob, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    /** Fonce sur sa cible en vol et la frappe des qu'elle est a portee. */
    public static class AttackGoal extends Goal {
        private final Mob mob;
        private final double speed, reach;
        private int cooldown = 0;

        public AttackGoal(Mob mob, double speed, double reach) {
            this.mob = mob; this.speed = speed; this.reach = reach;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { LivingEntity t = mob.getTarget(); return t != null && t.isAlive() && !mob.isPassenger(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void stop() { mob.getNavigation().stop(); }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30F, 30F);
            mob.getMoveControl().setWantedPosition(t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), speed);
            if (cooldown > 0) cooldown--;
            double r = mob.getBbWidth() * 0.5 + t.getBbWidth() * 0.5 + reach;
            if (cooldown <= 0 && mob.distanceToSqr(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ()) <= r * r) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(t);
                cooldown = 20;
            }
        }
    }

    /** Prend la fuite par les airs : s'eloigne et monte quand un joueur approche ou quand il vient d'etre blesse. */
    public static class FleeGoal extends Goal {
        private final Mob mob;
        private final double dist, speed;
        private Vec3 dest;

        public FleeGoal(Mob mob, double dist, double speed) {
            this.mob = mob; this.dist = dist; this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private LivingEntity threat() {
            LivingEntity hurtBy = mob.getLastHurtByMob();
            if (hurtBy != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < 100) return hurtBy;
            Player p = mob.level().getNearestPlayer(mob, dist);
            return p != null && !p.isCreative() && !p.isSpectator() ? p : null;
        }

        @Override
        public boolean canUse() {
            LivingEntity t = threat();
            if (t == null) return false;
            Vec3 away = mob.position().subtract(t.position());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double y = Math.min(mob.getY() + 2 + mob.getRandom().nextInt(4), mob.level().getMaxBuildHeight() - 8);
            dest = new Vec3(mob.getX() + away.x * 12, y, mob.getZ() + away.z * 12);
            return true;
        }

        @Override public boolean canContinueToUse() { return dest != null && mob.distanceToSqr(dest) > 4.0 && threat() != null; }
        @Override public void start() { mob.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, speed); }
        @Override public void tick() { mob.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, speed); }
        @Override public void stop() { dest = null; }
    }
}
