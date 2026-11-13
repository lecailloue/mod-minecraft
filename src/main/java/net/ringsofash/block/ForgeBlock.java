package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.ringsofash.gen.Reg;
import net.ringsofash.menu.ForgeMenu;

/**
 * Forge naine : un bloc geant de 3 x 3 x 3. Le bloc central (ox=oy=oz=1) porte le modele ; les 26 autres sont invisibles
 * mais pleins, et tout casse ensemble. Un clic droit ouvre la fenetre d'amelioration.
 */
public class ForgeBlock extends Block {
    public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2);
    public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 2);
    public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2);

    public ForgeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(OX, 1).setValue(OY, 1).setValue(OZ, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(OX, OY, OZ); }

    public static boolean isCenter(BlockState s) { return s.getValue(OX) == 1 && s.getValue(OY) == 1 && s.getValue(OZ) == 1; }

    /** pose la forge : (x, y, z) = coin bas du centre horizontal ; le bloc occupe y..y+2 */
    public static void build(Level level, int x, int y, int z) {
        for (int dx = 0; dx < 3; dx++) for (int dy = 0; dy < 3; dy++) for (int dz = 0; dz < 3; dz++) {
            level.setBlock(new BlockPos(x + dx - 1, y + dy, z + dz - 1), Reg.DWARVEN_FORGE.get().defaultBlockState()
                    .setValue(OX, dx).setValue(OY, dy).setValue(OZ, dz), 2);
        }
    }

    public static BlockPos core(BlockState s, BlockPos pos) { return pos.offset(1 - s.getValue(OX), 1 - s.getValue(OY), 1 - s.getValue(OZ)); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos core = core(state, pos);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ForgeMenu(id, inv, ContainerLevelAccess.create(level, core)), Component.literal("Forge naine")));
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            BlockPos c = core(state, pos);
            popResource(level, c, new ItemStack(Reg.BI_DWARVEN_FORGE.get()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos c = core(state, pos);
            for (int dx = 0; dx < 3; dx++) for (int dy = 0; dy < 3; dy++) for (int dz = 0; dz < 3; dz++) {
                BlockPos p = c.offset(dx - 1, dy - 1, dz - 1);
                if (p.equals(pos)) continue;
                BlockState o = level.getBlockState(p);
                if (o.is(this) && o.getValue(OX) == dx && o.getValue(OY) == dy && o.getValue(OZ) == dz) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
