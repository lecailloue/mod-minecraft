package net.ringsofash.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ringsofash.RingsOfAsh;

/** Ichor de Morgul : liquide vert lumineux qui se comporte comme la lave (brule, lumiere 15, meme viscosite). */
public final class ModFluids {
    private static final String MODID = RingsOfAsh.MODID;
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredHolder<FluidType, FluidType> ICHOR_TYPE = TYPES.register("morgul_ichor", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.ringsofash.morgul_ichor")
            .canSwim(false).canDrown(false)
            .pathType(PathType.LAVA).adjacentPathType(null)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
            .lightLevel(15).density(3000).viscosity(6000).temperature(1300)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ICHOR = FLUIDS.register("morgul_ichor", () -> new BaseFlowingFluid.Source(ModFluids.PROPS));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_ICHOR = FLUIDS.register("flowing_morgul_ichor", () -> new BaseFlowingFluid.Flowing(ModFluids.PROPS));

    public static final DeferredBlock<LiquidBlock> ICHOR_BLOCK = BLOCKS.register("morgul_ichor", () -> new IchorBlock(ICHOR.get(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).mapColor(MapColor.EMERALD).lightLevel(s -> 15)));
    public static final DeferredItem<BucketItem> ICHOR_BUCKET = ITEMS.register("morgul_ichor_bucket", () -> new BucketItem(ICHOR.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final BaseFlowingFluid.Properties PROPS = new BaseFlowingFluid.Properties(ICHOR_TYPE, ICHOR, FLOWING_ICHOR)
            .block(ICHOR_BLOCK).bucket(ICHOR_BUCKET).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100F);

    private ModFluids() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
