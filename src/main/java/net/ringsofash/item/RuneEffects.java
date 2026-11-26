package net.ringsofash.item;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ringsofash.RingsOfAsh;

/**
 * Ce que les runes font vraiment.
 *
 * Les chiffres (degats, vitesse, allonge) passent par des modificateurs d'attribut, donc
 * ils apparaissent dans l'infobulle de l'arme comme ceux d'une enchantement. Les effets
 * a l'impact (feu, vent, poison, rune noire) se declenchent au coup porte. L'usure, elle,
 * est reglee une fois pour toutes au moment du scellement (voir Rune.socket).
 */
public final class RuneEffects {
    private RuneEffects() {}

    public static void init(IEventBus bus) {
        bus.addListener(RuneEffects::attributes);
        bus.addListener(RuneEffects::onHit);
        bus.addListener(RuneEffects::onDraw);
        bus.addListener(RuneEffects::onArrow);
    }

    // ------------------------------------------------------------------ arcs

    /**
     * Sur un arc, la rune de vitesse raccourcit le bandage : la charge compte comme si
     * on avait tire un peu plus longtemps.
     */
    private static void onDraw(net.neoforged.neoforge.event.entity.player.ArrowLooseEvent e) {
        ItemStack bow = e.getBow();
        if (!Runes.runic(bow) || Rune.on(bow).isEmpty()) return;
        float speed = Rune.totals(bow).speed();
        if (speed != 0) e.setCharge(Math.max(0, Math.round(e.getCharge() * (1F + speed / 100F))));
    }

    /**
     * La fleche part plus fort avec une rune de portee (la portee varie comme le carre
     * de la vitesse, d'ou la racine), et s'enflamme avec une rune de feu.
     */
    private static void onArrow(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide) return;
        if (!(e.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof LivingEntity owner)) return;
        ItemStack bow = owner.getMainHandItem();
        if (!Runes.runic(bow) || Rune.grip(bow) != Rune.Grip.BOW) return;
        List<Rune> runes = Rune.on(bow);
        if (runes.isEmpty()) return;

        float reach = Rune.totals(bow).reach();
        if (reach != 0) {
            // +2 blocs par point d'allonge sur une portee a plat d'environ 55 blocs
            double k = Math.sqrt(Math.max(0.1, 1.0 + 2.0 * reach / 55.0));
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(k));
        }
        if (runes.contains(Rune.FIRE)) arrow.igniteForSeconds(100);
    }

    // ------------------------------------------------------------------ chiffres

    private static void attributes(ItemAttributeModifierEvent e) {
        ItemStack s = e.getItemStack();
        if (!Runes.runic(s)) return;
        List<Rune> runes = Rune.on(s);
        if (runes.isEmpty()) return;
        Rune.Totals t = Rune.totals(s);

        if (t.dmg() != 0) e.addModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(RingsOfAsh.id("rune_damage"), t.dmg() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.MAINHAND);
        if (t.speed() != 0) e.addModifier(Attributes.ATTACK_SPEED,
                new AttributeModifier(RingsOfAsh.id("rune_speed"), t.speed() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.MAINHAND);
        if (t.reach() != 0) {
            e.addModifier(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(RingsOfAsh.id("rune_reach"), t.reach(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            e.addModifier(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(RingsOfAsh.id("rune_reach_block"), t.reach(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        // combo Mur : les coups recus ne font plus reculer
        if (Rune.combos(s).contains(Rune.Combo.MUR)) e.addModifier(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(RingsOfAsh.id("rune_wall"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    // ------------------------------------------------------------------ a l'impact

    private static void onHit(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof LivingEntity attacker)) return;
        // un coup de main, ou une fleche de l'arc encore tenu : la magie et le reste ne comptent pas
        boolean melee = src.getDirectEntity() == attacker;
        boolean shot = src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow a && a.getOwner() == attacker;
        if (!melee && !shot) return;
        ItemStack w = attacker.getMainHandItem();
        if (!Runes.runic(w)) return;
        if (shot && Rune.grip(w) != Rune.Grip.BOW) return;
        List<Rune> runes = Rune.on(w);
        if (runes.isEmpty()) return;

        LivingEntity target = e.getEntity();
        List<Rune.Combo> combos = Rune.combos(w);
        var rnd = attacker.getRandom();

        if (runes.contains(Rune.FIRE) && melee) {               // la fleche brule deja d'elle-meme
            target.igniteForSeconds(combos.contains(Rune.Combo.FOURNAISE) ? 8 : 4);
            // combo Flamme d'Udun : tout ce qui entoure la cible prend feu aussi
            if (combos.contains(Rune.Combo.UDUN)) {
                for (LivingEntity near : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0), x -> x != attacker))
                    near.igniteForSeconds(4);
            }
        }
        if (runes.contains(Rune.POISON) && rnd.nextFloat() < 0.5F)
            target.addEffect(new MobEffectInstance(MobEffects.POISON, combos.contains(Rune.Combo.VENIN) ? 80 : 40, 0), attacker);
        if (runes.contains(Rune.WIND) && rnd.nextFloat() < (combos.contains(Rune.Combo.BOURRASQUE) ? 0.15F : 0.05F)) {
            target.setDeltaMovement(target.getDeltaMovement().x, 1.05, target.getDeltaMovement().z);
            target.hurtMarked = true;
        }
        if (runes.contains(Rune.BLACK) && attacker instanceof Player p && !p.level().isClientSide) {
            var lore = net.ringsofash.data.ModData.lore(p);
            lore.corruption = Math.min(1000, lore.corruption + Rune.BLACK.corrupt);
        }
    }
}
