package net.ringsofash.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ringsofash.gen.Reg;

/** Creature fourbe des cavernes : vole un objet et detale. Il faut la rattraper pour le recuperer. */
public class ThiefEntity extends Monster {
    public ThiefEntity(EntityType<? extends ThiefEntity> type, Level level) { super(type, level); this.xpReward = 12; }

    public boolean hasLoot() { return !getMainHandItem().isEmpty(); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 24.0F, 1.5, 1.8, e -> hasLoot()));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !hasLoot()));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!hasLoot() && target instanceof Player p && !level().isClientSide) {
            for (int tries = 0; tries < 12; tries++) {
                int slot = random.nextInt(9);
                ItemStack s = p.getInventory().getItem(slot);
                if (s.isEmpty() || s.is(Reg.HALFLING_RING.get()) || net.ringsofash.data.Guide.isGuide(s)) continue;
                setItemSlot(EquipmentSlot.MAINHAND, s.copy());
                setDropChance(EquipmentSlot.MAINHAND, 2.0F);
                p.getInventory().setItem(slot, ItemStack.EMPTY);
                setPersistenceRequired();
                setTarget(null);
                playSound(SoundEvents.ITEM_PICKUP, 1.0F, 0.6F);
                p.displayClientMessage(Component.literal("Une créature blême vous a volé : ").append(s.getHoverName()).append(" !"), false);
                return true;
            }
        }
        return super.doHurtTarget(target);
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return !hasLoot(); }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.FOX_SNIFF; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.FOX_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.FOX_DEATH; }
    @Override
    public float getVoicePitch() { return 0.6F; }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
