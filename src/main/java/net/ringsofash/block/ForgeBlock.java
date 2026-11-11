package net.ringsofash.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ringsofash.gen.Reg;

/** Forge naine : arme, outil ou armure en main + lingot de lunargent dans l'autre main = +1 niveau et reparation. */
public class ForgeBlock extends Block {
    public ForgeBlock(Properties p) { super(p); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack ingot = player.getOffhandItem();
        ResourceKey<Enchantment> key; int max;
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) { key = Enchantments.SHARPNESS; max = 5; }
        else if (stack.getItem() instanceof ArmorItem) { key = Enchantments.PROTECTION; max = 4; }
        else if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)) { key = Enchantments.EFFICIENCY; max = 5; }
        else {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Forge naine : tenez une arme, un outil ou une armure, et un lingot de lunargent dans l'autre main."), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!ingot.is(Reg.LUNARGENT_INGOT.get())) {
            player.displayClientMessage(Component.literal("Il faut un lingot de lunargent dans l'autre main.").withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.CONSUME;
        }
        Holder<Enchantment> ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        int cur = EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        if (cur >= max && !stack.isDamaged()) {
            player.displayClientMessage(Component.literal("Cet objet est déjà au sommet de l'art nain.").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        if (cur < max) stack.enchant(ench, cur + 1);
        stack.setDamageValue(0);
        if (!player.getAbilities().instabuild) ingot.shrink(1);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        ((ServerLevel) level).sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0);
        player.displayClientMessage(Component.literal("L'objet ressort de la forge plus fort (niveau " + Math.min(max, cur + 1) + ") et comme neuf.").withStyle(ChatFormatting.GOLD), true);
        return ItemInteractionResult.CONSUME;
    }
}
