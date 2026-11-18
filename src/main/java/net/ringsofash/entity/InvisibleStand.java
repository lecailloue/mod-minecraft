package net.ringsofash.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ringsofash.gen.Reg;

/** Support d'armure invisible et incassable : sert a exposer des armures sans voir le support. */
public class InvisibleStand extends ArmorStand {
    public InvisibleStand(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        setInvisible(true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // seul le vide / la commande /kill peuvent l'eliminer ; meme un joueur en creatif ne peut pas le casser
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        return false;
    }

    @Override
    public boolean isAttackable() { return false; }                          // les coups de joueur (survie ou creatif) sont ignores

    @Override
    public boolean skipAttackInteraction(net.minecraft.world.entity.Entity attacker) { return true; }

    @Override
    public ItemStack getPickResult() { return new ItemStack(Reg.INVISIBLE_ARMOR_STAND.get()); }
}
