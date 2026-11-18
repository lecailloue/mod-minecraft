package net.ringsofash.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.entity.InvisibleStand;
import net.ringsofash.entity.ModEntities;

/** Pose un support d'armure invisible (meme logique que l'item vanilla). Creatif uniquement : aucune recette. */
public class InvisibleStandItem extends Item {
    public InvisibleStandItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction dir = ctx.getClickedFace();
        if (dir == Direction.DOWN) return InteractionResult.FAIL;
        Level level = ctx.getLevel();
        BlockPlaceContext bpc = new BlockPlaceContext(ctx);
        BlockPos pos = bpc.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        Vec3 v = Vec3.atBottomCenterOf(pos);
        EntityType<InvisibleStand> type = ModEntities.INVISIBLE_STAND.get();
        AABB box = type.getDimensions().makeBoundingBox(v.x(), v.y(), v.z());
        if (!level.noCollision(null, box) || !level.getEntities(null, box).isEmpty()) return InteractionResult.FAIL;
        if (level instanceof ServerLevel sl) {
            InvisibleStand s = type.create(sl, EntityType.createDefaultStackConfig(sl, stack, ctx.getPlayer()), pos, MobSpawnType.SPAWN_EGG, true, true);
            if (s == null) return InteractionResult.FAIL;
            float yaw = (float) Mth.floor((Mth.wrapDegrees(ctx.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
            s.moveTo(s.getX(), s.getY(), s.getZ(), yaw, 0.0F);
            sl.addFreshEntityWithPassengers(s);
            level.playSound(null, s.getX(), s.getY(), s.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
            s.gameEvent(GameEvent.ENTITY_PLACE, ctx.getPlayer());
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
