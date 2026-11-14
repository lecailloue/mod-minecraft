package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.entity.FoeEntity;
import net.ringsofash.entity.GiantSpiderEntity;
import net.ringsofash.entity.ShadowRiderEntity;
import net.ringsofash.entity.PeopleEntity;

import java.util.List;

/** Batons magiques. */
public class StaffItem extends Item {
    public enum Kind { FIRE, LIGHT, STORM, HEALING }
    public final Kind kind;

    public StaffItem(Kind kind, Properties p) { super(p); this.kind = kind; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel sl = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        int cd;
        switch (kind) {
            case FIRE -> {
                LargeFireball fb = new LargeFireball(sl, player, look.scale(1.5), 1);
                fb.setPos(player.getX() + look.x * 1.5, player.getEyeY() - 0.2, player.getZ() + look.z * 1.5);
                sl.addFreshEntity(fb);
                sl.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
                cd = 40;
            }
            case LIGHT -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
                sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.5, player.getZ(), 120, 4, 2, 4, 0.1);
                sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 2, player.getZ(), 1, 0, 0, 0, 0);
                for (Monster m : sl.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12))) {
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                    m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    boolean dark = m instanceof ShadowRiderEntity || m instanceof GiantSpiderEntity;
                    if (dark) { m.hurt(player.damageSources().indirectMagic(player, player), 8F); m.setTarget(null); }
                }
                sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
                cd = 120;
            }
            case STORM -> {
                Vec3 from = player.getEyePosition();
                BlockHitResult hit = sl.clip(new ClipContext(from, from.add(look.scale(48)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 at = hit.getLocation();
                // vise d'abord une creature proche de la ligne de mire
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48), e -> e != player)) {
                    Vec3 to = e.position().subtract(from);
                    double along = to.dot(look);
                    if (along > 2 && along < 48 && to.subtract(look.scale(along)).length() < 2.0 && along < at.subtract(from).length()) at = e.position();
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) { bolt.moveTo(at); sl.addFreshEntity(bolt); }
                cd = 100;
            }
            default -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8), e -> e instanceof Player || e instanceof PeopleEntity)) {
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
                    sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 5, 0.4, 0.3, 0.4, 0.02);
                }
                sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.0F);
                cd = 300;
            }
        }
        player.getCooldowns().addCooldown(this, cd);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(switch (kind) {
            case FIRE -> "Clic droit : lance une boule de feu.";
            case LIGHT -> "Clic droit : éclat de lumière qui affaiblit les monstres et brûle les êtres d'ombre.";
            case STORM -> "Clic droit : appelle la foudre là où vous regardez.";
            case HEALING -> "Clic droit : soigne vous et vos alliés proches.";
        }).withStyle(ChatFormatting.GRAY));
    }
}
