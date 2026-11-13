package net.ringsofash.effect;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.gen.Reg;

/** Effets du mod : forme de Sylvgarde (coeur de Sylvgarde) et enracinement (racines). */
public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RingsOfAsh.MODID);

    /** Le porteur devient un Sylvgarde : double taille, robuste, frappe fort, mais lent et vulnerable au feu. */
    public static final DeferredHolder<MobEffect, MobEffect> SYLVAN_FORM = EFFECTS.register("sylvan_form", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x4F7A34) {}
            .addAttributeModifier(Attributes.SCALE, RingsOfAsh.id("sylvan_scale"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.MAX_HEALTH, RingsOfAsh.id("sylvan_health"), 20.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, RingsOfAsh.id("sylvan_damage"), 7.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.ATTACK_KNOCKBACK, RingsOfAsh.id("sylvan_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.ARMOR, RingsOfAsh.id("sylvan_armor"), 8.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, RingsOfAsh.id("sylvan_speed"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.STEP_HEIGHT, RingsOfAsh.id("sylvan_step"), 1.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE, RingsOfAsh.id("sylvan_reach"), 1.5, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.BLOCK_INTERACTION_RANGE, RingsOfAsh.id("sylvan_block_reach"), 1.5, AttributeModifier.Operation.ADD_VALUE));

    /** Racines : la cible ne peut plus bouger ni sauter. */
    public static final DeferredHolder<MobEffect, MobEffect> ROOTED = EFFECTS.register("rooted", () -> new Rooted()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, RingsOfAsh.id("rooted_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, RingsOfAsh.id("rooted_fly"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, RingsOfAsh.id("rooted_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, RingsOfAsh.id("rooted_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE));

    static class Rooted extends MobEffect {
        Rooted() { super(MobEffectCategory.HARMFUL, 0x5A4630); }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amp) { return true; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amp) {
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            if (e.level() instanceof ServerLevel sl && e.tickCount % 6 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Reg.ANCIENT_LOG.get().defaultBlockState()),
                        e.getX(), e.getY() + 0.3, e.getZ(), 6, e.getBbWidth() * 0.5, 0.25, e.getBbWidth() * 0.5, 0.05);
            }
            return true;
        }
    }
}
