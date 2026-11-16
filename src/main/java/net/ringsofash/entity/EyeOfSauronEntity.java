package net.ringsofash.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * L'Oeil de Sauron : un decor vivant. Il ne bouge pas, ne vieillit pas, ne disparait jamais et ne peut pas etre touche ; c'est le client
 * (EyeOfSauronRenderer) qui le dessine et le tourne en permanence vers le joueur. Il est pose au sommet de Barad-dur, ou par un oeuf.
 */
public class EyeOfSauronEntity extends Mob {
    public EyeOfSauronEntity(EntityType<? extends EyeOfSauronEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        noPhysics = true;
    }

    @Override public boolean isInvulnerableTo(DamageSource s) { return !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public void push(double x, double y, double z) {}
    @Override public void move(MoverType t, Vec3 v) {}
    @Override public void travel(Vec3 v) {}
    @Override public boolean canAttack(net.minecraft.world.entity.LivingEntity e) { return false; }

    /** le modele depasse un peu la boite de collision : on elargit celle qui sert a decider s'il est visible */
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(6.0); }
}
