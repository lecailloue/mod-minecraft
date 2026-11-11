package net.ringsofash.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PeopleEntity;

/** Bucher d'alarme : l'allumer appelle des soldats a l'aide pendant quelques minutes. */
public class AlarmPyreBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public AlarmPyreBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LIT)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel sl = (ServerLevel) level;
        sl.setBlock(pos, state.setValue(LIT, true), 3);
        sl.scheduleTick(pos, this, 20 * 240);
        sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 2.0F, 0.8F);
        sl.playSound(null, pos, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 48.0F, 1.0F);
        for (int i = 0; i < 4; i++) {
            PeopleEntity g = ModEntities.HUMAN.get().create(sl);
            if (g == null) continue;
            double a = i * Math.PI / 2 + 0.5;
            g.moveTo(player.getX() + Math.cos(a) * 3, player.getY(), player.getZ() + Math.sin(a) * 3, 0, 0);
            g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.EVENT, null);
            g.setRole(PeopleEntity.GUARD);
            g.setTempLife(20 * 240);
            sl.addFreshEntity(g);
            sl.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 1, g.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
        player.displayClientMessage(Component.literal("Le bûcher s'embrase : quatre soldats accourent à votre aide !").withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, false), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.0 + r.nextDouble() * 0.5, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.04, 0);
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0, 0.08, 0);
        if (r.nextInt(8) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
    }
}
