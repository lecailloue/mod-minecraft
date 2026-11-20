package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lit nain : un vrai lit (on y dort, il definit le point d'apparition), rendu par un modele JSON et sans entite de bloc.
 * Deux lits poses cote a cote (meme sens) se raccordent en lit double : le modele supprime les pieds et les flancs du cote accole.
 */
public class DwarvenBedBlock extends BedBlock {
    /** un autre lit nain est accole a gauche / a droite (vu depuis le pied du lit, tete devant) */
    public static final BooleanProperty JOIN_LEFT = BooleanProperty.create("join_left");
    public static final BooleanProperty JOIN_RIGHT = BooleanProperty.create("join_right");

    public DwarvenBedBlock(Properties p) { super(DyeColor.RED, p); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(JOIN_LEFT, JOIN_RIGHT);
    }

    private boolean joinsWith(BlockState self, BlockState other) {
        return other.is(this) && other.getValue(FACING) == self.getValue(FACING) && other.getValue(PART) == self.getValue(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        if (s == null) return null;
        Direction f = s.getValue(FACING);
        BlockPos pos = ctx.getClickedPos();
        return s.setValue(JOIN_RIGHT, joinsWith(s, ctx.getLevel().getBlockState(pos.relative(f.getClockWise()))))
                .setValue(JOIN_LEFT, joinsWith(s, ctx.getLevel().getBlockState(pos.relative(f.getCounterClockWise()))));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        BlockState s = super.updateShape(state, dir, other, level, pos, otherPos);
        if (!s.is(this)) return s;
        Direction f = s.getValue(FACING);
        if (dir == f.getClockWise()) return s.setValue(JOIN_RIGHT, joinsWith(s, other));
        if (dir == f.getCounterClockWise()) return s.setValue(JOIN_LEFT, joinsWith(s, other));
        return s;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return null; }

    private static VoxelShape board(Direction side, double h) {
        return switch (side) {
            case NORTH -> Block.box(0, 0, 0, 16, h, 2);
            case SOUTH -> Block.box(0, 0, 14, 16, h, 16);
            case WEST -> Block.box(0, 0, 0, 2, h, 16);
            default -> Block.box(14, 0, 0, 16, h, 16);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        boolean head = state.getValue(PART) == BedPart.HEAD;
        return Shapes.or(Block.box(0, 0, 0, 16, 9, 16), head ? board(f, 15) : board(f.getOpposite(), 10));
    }
}
