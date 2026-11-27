package net.ringsofash.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ringsofash.RingsOfAsh;

/**
 * La brulure de la Flamme d'Udun.
 *
 * Ce n'est pas le feu de Minecraft : elle fait trois fois plus mal, et quand elle prend
 * une creature deja en flammes, elle chasse le feu ordinaire et s'installe a sa place.
 * Le decompte vit dans les donnees de la creature ; un modificateur d'attribut nul, mais
 * synchronise, sert de signal au client pour dessiner les flammes rouges.
 */
public final class UdunFire {
    private UdunFire() {}

    private static final String TAG = "roa_udun";
    /** identifiant du signal envoye au client : la valeur ne change rien, c'est un drapeau */
    public static final net.minecraft.resources.ResourceLocation MARK = RingsOfAsh.id("udun_burn");

    private static final int HURT_EVERY = 10;
    private static final float HURT = 3.0F;

    public static void init(IEventBus bus) { bus.addListener(UdunFire::tick); }

    /** met la creature a bruler, et chasse le feu ordinaire s'il y en avait */
    public static void ignite(LivingEntity e, int ticks) {
        if (e.level().isClientSide || e.fireImmune()) return;
        int left = Math.max(ticks, e.getPersistentData().getInt(TAG));
        e.getPersistentData().putInt(TAG, left);
        e.setRemainingFireTicks(0);                    // la Flamme d'Udun prend le dessus
        mark(e, true);
    }

    public static boolean burning(LivingEntity e) { return e.getPersistentData().getInt(TAG) > 0; }

    /** cote client : la creature porte-t-elle le signal ? */
    public static boolean marked(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attributes.MOVEMENT_SPEED);
        return a != null && a.getModifier(MARK) != null;
    }

    private static void mark(LivingEntity e, boolean on) {
        AttributeInstance a = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return;
        if (on && a.getModifier(MARK) == null)
            a.addTransientModifier(new AttributeModifier(MARK, 0.0, AttributeModifier.Operation.ADD_VALUE));
        else if (!on && a.getModifier(MARK) != null) a.removeModifier(MARK);
    }

    private static void tick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof LivingEntity le) || le.level().isClientSide) return;
        int left = le.getPersistentData().getInt(TAG);
        if (left <= 0) return;
        if (le.isInWaterOrRain()) left = 0;            // l'eau en vient a bout, comme de tout feu
        else left--;
        le.getPersistentData().putInt(TAG, left);
        if (left <= 0) { mark(le, false); return; }
        le.setRemainingFireTicks(0);                   // rien d'autre ne brule tant qu'elle est la
        if (left % HURT_EVERY == 0) le.hurt(le.damageSources().inFire(), HURT);
    }
}
