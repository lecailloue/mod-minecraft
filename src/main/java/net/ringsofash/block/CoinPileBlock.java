package net.ringsofash.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.ringsofash.gen.Reg;

/** Tas de pieces d'or : se pose en couches (1 a 8) exactement comme la neige. */
public class CoinPileBlock extends SnowLayerBlock {
    public CoinPileBlock(BlockBehaviour.Properties props) { super(props); }

    /** etat avec le nombre de couches voulu (1..8) */
    public static BlockState of(int layers) {
        return Reg.GOLD_PILE.get().defaultBlockState().setValue(LAYERS, Math.max(1, Math.min(8, layers)));
    }

    public static Block block() { return Reg.GOLD_PILE.get(); }
}
