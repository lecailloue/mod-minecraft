package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ringsofash.data.ModData;
import net.ringsofash.data.PlayerLore;
import net.ringsofash.entity.FoeEntity;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.gen.Reg;

import java.util.List;

/** Objets utilitaires : cors, pierre de vision, boissons, corde, arc elfique. */
public final class MiscItems {
    private MiscItems() {}

    private static void tip(List<Component> tip, String s) { tip.add(Component.literal(s).withStyle(ChatFormatting.GRAY)); }

    /** Objet simple avec une ligne d'explication. */
    public static class Info extends Item {
        private final String text;
        public Info(String text, Properties p) { super(p); this.text = text; }
        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) { tip(t, text); }
    }

    /** Cor de guerre : renforce les allies. Cor des morts : invoque une armee de spectres. */
    public static class Horn extends Item {
        private final boolean dead;
        public Horn(boolean dead, Properties p) { super(p); this.dead = dead; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            ServerLevel sl = (ServerLevel) level;
            sl.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 64.0F, dead ? 0.6F : 1.0F);
            if (dead) {
                for (int i = 0; i < 8; i++) {
                    FoeEntity s = ModEntities.DEAD_SOLDIER.get().create(sl);
                    if (s == null) continue;
                    double a = i * Math.PI / 4;
                    s.moveTo(player.getX() + Math.cos(a) * 3, player.getY(), player.getZ() + Math.sin(a) * 3, player.getYRot(), 0);
                    s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(s.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    sl.addFreshEntity(s);
                    sl.sendParticles(ParticleTypes.SOUL, s.getX(), s.getY() + 1, s.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
                }
                stack.shrink(1);
                player.displayClientMessage(Component.literal("Les morts répondent à l'appel... pour un temps.").withStyle(ChatFormatting.AQUA), true);
            } else {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> e instanceof Player || e instanceof PeopleEntity)) {
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
                }
                player.getCooldowns().addCooldown(this, 1800);
            }
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            tip(t, dead ? "Usage unique : invoque huit soldats spectraux qui combattent pour vous." : "Donne force, résistance et vitesse aux alliés proches.");
        }
    }

    /** Pierre de vision : indique les lieux importants les plus proches, mais attire le regard de l'Oeil. */
    public static class SeeingStone extends Item {
        public SeeingStone(Properties p) { super(p); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
            net.ringsofash.GameEvents.seeingStone(sp);
            PlayerLore lore = ModData.lore(sp);
            lore.eye += 25; lore.corruption += 10;
            player.getCooldowns().addCooldown(this, 600);
            return InteractionResultHolder.consume(stack);
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            tip(t, "Révèle la direction des lieux importants proches."); t.add(Component.literal("Chaque regard attire celui de l'Œil.").withStyle(ChatFormatting.DARK_RED));
        }
    }

    /** Boissons : biere naine et seve vive (fait grandir un moment). */
    public static class Drink extends Item {
        private final boolean sap;
        public Drink(boolean sap, Properties p) { super(p); this.sap = sap; }
        @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.DRINK; }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack r = super.finishUsingItem(stack, level, entity);
            if (!level.isClientSide && entity instanceof ServerPlayer sp) {
                if (sap) net.ringsofash.GameEvents.grow(sp, 20 * 90);
                else { sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0)); sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0)); }
            }
            return r;
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            tip(t, sap ? "Breuvage des sylvegardes : vous fait grandir pendant une minute et demie." : "Réchauffe et endurcit... et fait un peu tourner la tête.");
        }
    }

    /** Corde elfique : se deroule vers le bas depuis le bloc vise. */
    public static class Rope extends Item {
        public Rope(Properties p) { super(p); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            if (level.isClientSide) return InteractionResult.SUCCESS;
            BlockState rope = Reg.ROPE.get().defaultBlockState();
            int n = 0;
            while (n < 32 && level.getBlockState(pos).isAir() && pos.getY() > level.getMinBuildHeight()) {
                level.setBlock(pos, rope, 3);
                pos = pos.below(); n++;
            }
            if (n == 0) return InteractionResult.FAIL;
            level.playSound(null, ctx.getClickedPos(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }

        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) {
            tip(t, "Clic droit sur un bord : déroule jusqu'à 32 blocs de corde vers le bas. Casser le haut la fait tomber.");
        }
    }

    public static class ElvenBow extends BowItem {
        public ElvenBow(Properties p) { super(p); }
        @Override public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
            arrow.setBaseDamage(arrow.getBaseDamage() + 2.0);
            return arrow;
        }
        @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, List<Component> t, TooltipFlag f) { tip(t, "Flèches plus puissantes."); }
    }
}
