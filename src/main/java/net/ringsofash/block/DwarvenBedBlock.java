package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lit nain : un vrai lit (on y dort, il definit le point d'apparition), mais rendu par un modele JSON et sans entite de bloc. */
public class DwarvenBedBlock extends BedBlock {
    public DwarvenBedBlock(Properties p) { super(DyeColor.RED, p); }

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
