package net.ringsofash.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.ringsofash.world.Travel;

import java.util.List;

/** Anneau des semi-hommes : maintenir clic droit pour passer d'un monde a l'autre. */
public class TravelRingItem extends Item {
    public TravelRingItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 60; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp && Travel.useRing(sp)) {
            sp.getCooldowns().addCooldown(this, 1200);
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Maintenir clic droit : voyager entre").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("le monde et la Hautemarche.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Ne marche pas en plein combat.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
