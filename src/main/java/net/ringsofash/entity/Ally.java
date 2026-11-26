package net.ringsofash.entity;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Un mob rallie a un joueur : le Pacte noir releve le dernier ennemi tue, et il doit
 * alors se battre a cote de toi et te suivre.
 *
 * Le lien tient dans les donnees persistantes du mob, pas dans ses buts : les buts ne
 * survivent pas a un rechargement du monde, la marque si. On repose les buts a chaque
 * fois que le mob reparait dans le monde (voir RuneEffects.onArrow).
 */
public final class Ally {
    private Ally() {}

    public static final String TAG = "roa_ally";

    /** le mob se rallie a toi et oublie ses anciennes cibles */
    public static void mark(Mob m, Player p) { m.getPersistentData().putUUID(TAG, p.getUUID()); }

    /** le mob te suit mais garde ses propres cibles : pour un Sylvegarde, qui sait deja qui frapper */
    public static void markKeep(Mob m, Player p) {
        mark(m, p);
        m.getPersistentData().putBoolean(TAG + "_keep", true);
    }

    public static boolean marked(Mob m) { return m.getPersistentData().hasUUID(TAG); }

    public static UUID ownerId(Mob m) { return marked(m) ? m.getPersistentData().getUUID(TAG) : null; }

    public static Player owner(Mob m) {
        UUID id = ownerId(m);
        return id == null ? null : m.level().getPlayerByUUID(id);
    }

    /** a poser une fois, quand le mob entre dans le monde */
    public static void apply(Mob m) {
        if (!marked(m)) return;
        if (!m.getPersistentData().getBoolean(TAG + "_keep")) {
            m.targetSelector.removeAllGoals(g -> true);      // il ne s'en prend plus a toi
            m.setCustomName(net.minecraft.network.chat.Component.literal("Relevé")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
        m.goalSelector.addGoal(2, new FollowGoal(m, 1.25, 6.0F));
        m.setPersistenceRequired();
    }

    /** il reste dans tes pattes tant qu'il n'a personne a frapper */
    private static class FollowGoal extends Goal {
        private final Mob mob;
        private final double speed;
        private final float keep;
        private Player target;
        private int timer;

        FollowGoal(Mob mob, double speed, float keep) {
            this.mob = mob; this.speed = speed; this.keep = keep;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = owner(mob);
            if (p == null || p.isSpectator() || p.isDeadOrDying()) return false;
            if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;   // il se bat deja
            if (mob.distanceToSqr(p) < keep * keep) return false;
            target = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && mob.distanceToSqr(target) > keep * keep;
        }

        @Override
        public void start() { timer = 0; }

        @Override
        public void stop() { target = null; mob.getNavigation().stop(); }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(target, 10.0F, mob.getMaxHeadXRot());
            if (--timer > 0) return;
            timer = adjustedTickDelay(10);
            // trop loin ou coince : il rattrape d'un bond plutot que de se perdre
            if (mob.distanceToSqr(target) > 32 * 32 || mob.getNavigation().isStuck())
                mob.moveTo(target.getX() + (mob.getRandom().nextDouble() - 0.5) * 3,
                        target.getY(), target.getZ() + (mob.getRandom().nextDouble() - 0.5) * 3,
                        mob.getYRot(), mob.getXRot());
            else mob.getNavigation().moveTo(target, speed);
        }
    }
}
