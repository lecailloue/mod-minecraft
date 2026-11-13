package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.ringsofash.entity.BossEntity;
import net.ringsofash.entity.FoeEntity;
import net.ringsofash.entity.PaleRiderEntity;

import java.util.List;

/** Armes avec un effet a l'impact. */
public class SpecialSword extends SwordItem {
    public enum Kind { NONE, ELVEN, WIGHT, HAMMER, CLUB, MACE, KING, DAGGER, SPEAR, FROST, BLOOD, VIPER, SUN, SENTINEL, STORM }
    public final Kind kind;

    public SpecialSword(Kind kind, Tier tier, Item.Properties p) { super(tier, p); this.kind = kind; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (kind) {
            case ELVEN -> { if (target instanceof FoeEntity f && isOrc(f)) target.hurt(attacker.damageSources().magic(), 3F); }
            case WIGHT -> { if (target instanceof PaleRiderEntity || (target instanceof FoeEntity f && f.kind() == FoeEntity.Kind.WIGHT)) target.hurt(attacker.damageSources().magic(), 6F); }
            case HAMMER, CLUB -> target.knockback(1.2, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            case MACE -> { target.igniteForSeconds(5); target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), attacker); }
            case KING -> { if (target instanceof Monster) { target.hurt(attacker.damageSources().magic(), target instanceof BossEntity ? 6F : 3F); attacker.heal(1F); } }
            case FROST -> { target.setTicksFrozen(target.getTicksRequiredToFreeze() + 120); target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), attacker); }
            case BLOOD -> { if (target instanceof Monster) attacker.heal(2F); }
            case VIPER -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1), attacker);
            case SUN -> { if (isUndead(target)) { target.igniteForSeconds(6); target.hurt(attacker.damageSources().magic(), 5F); } }
            case SENTINEL -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), attacker);
            case STORM -> {
                if (attacker.getRandom().nextFloat() < 0.25F && target.level() instanceof ServerLevel sl) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) { bolt.moveTo(target.position()); bolt.setVisualOnly(true); sl.addFreshEntity(bolt); }
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), e -> e != attacker && e instanceof Monster))
                        e.hurt(attacker.damageSources().lightningBolt(), 5F);
                }
            }
            default -> { }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    public static boolean isUndead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD) || e instanceof PaleRiderEntity
                || (e instanceof FoeEntity f && (f.kind() == FoeEntity.Kind.WIGHT || f.kind() == FoeEntity.Kind.DEAD_SOLDIER));
    }

    public static boolean isOrc(FoeEntity f) {
        return switch (f.kind()) { case ORC, WAR_ORC, ORC_CHIEF, GOBLIN -> true; default -> false; };
    }

    @Override
    public boolean isFoil(ItemStack stack) { return kind == Kind.KING || kind == Kind.MACE || kind == Kind.STORM || super.isFoil(stack); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String s = switch (kind) {
            case ELVEN -> "Luit de bleu quand des orques approchent. Dégâts bonus contre eux.";
            case WIGHT -> "Forgée contre les spectres : blesse cavaliers blêmes et êtres des tertres.";
            case HAMMER, CLUB -> "Repousse violemment l'ennemi.";
            case MACE -> "Embrase et flétrit ce qu'elle frappe.";
            case KING -> "Lame du roi elfe : purifie les monstres et soigne son porteur.";
            case DAGGER -> "Très rapide.";
            case SPEAR -> "Longue allonge.";
            case FROST -> "Gèle et ralentit l'ennemi touché.";
            case BLOOD -> "Chaque monstre touché vous rend un cœur.";
            case VIPER -> "Empoisonne l'ennemi (poison II). Très rapide.";
            case SUN -> "Luit près des morts-vivants. Les embrase et les brûle de lumière.";
            case SENTINEL -> "Luit quand un monstre approche. Les frappés brillent à travers les murs.";
            case STORM -> "25 % de chance de déchaîner la foudre sur les monstres proches de la cible.";
            default -> null;
        };
        if (s != null) tip.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }
}
