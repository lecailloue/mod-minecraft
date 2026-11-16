package net.ringsofash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Ne porte aucune donnee : existe pour que le client dessine l'oeil des que le chunk est charge. */
public class EyeOfSauronBlockEntity extends BlockEntity {
    public EyeOfSauronBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.EYE_OF_SAURON.get(), pos, state); }
}
