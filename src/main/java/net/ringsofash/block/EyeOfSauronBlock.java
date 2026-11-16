package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Point d'ancrage de l'Oeil de Sauron : invisible, intangible, incassable ; le modele 3D est dessine par EyeOfSauronRenderer. */
public class EyeOfSauronBlock extends Block implements EntityBlock {
    public EyeOfSauronBlock(Properties p) { super(p); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.empty(); }
    @Override public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }
    @Override public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1.0F; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EyeOfSauronBlockEntity(pos, state); }
}
