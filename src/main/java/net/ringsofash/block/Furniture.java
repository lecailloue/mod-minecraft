package net.ringsofash.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;

/** Le mobilier des nains : coffre, lit, petit four et table de craft (blocs a modeles JSON, textures dans tools/furniture.py). */
public final class Furniture {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RingsOfAsh.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RingsOfAsh.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RingsOfAsh.MODID);

    public static final DeferredBlock<DwarvenChestBlock> CHEST = BLOCKS.register("dwarven_chest",
            () -> new DwarvenChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.5F).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion()));
    public static final DeferredBlock<DwarvenBedBlock> BED = BLOCKS.register("dwarven_bed",
            () -> new DwarvenBedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DwarvenFurnaceBlock> FURNACE = BLOCKS.register("dwarven_furnace",
            () -> new DwarvenFurnaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion()
                    .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0)));
    public static final DeferredBlock<DwarvenCraftingBlock> CRAFTING = BLOCKS.register("dwarven_crafting_table",
            () -> new DwarvenCraftingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.5F).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion()));

    public static final DeferredBlock<ShelfBlock> SPRUCE_SHELF = BLOCKS.register("spruce_shelf",
            () -> new ShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).noOcclusion()));
    public static final DeferredItem<BlockItem> SPRUCE_SHELF_ITEM = ITEMS.registerSimpleBlockItem("spruce_shelf", SPRUCE_SHELF);

    public static final DeferredItem<BlockItem> CHEST_ITEM = ITEMS.registerSimpleBlockItem("dwarven_chest", CHEST);
    public static final DeferredItem<Item> BED_ITEM = ITEMS.register("dwarven_bed", () -> new BedItem(BED.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FURNACE_ITEM = ITEMS.registerSimpleBlockItem("dwarven_furnace", FURNACE);
    public static final DeferredItem<BlockItem> CRAFTING_ITEM = ITEMS.registerSimpleBlockItem("dwarven_crafting_table", CRAFTING);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DwarvenChestBlockEntity>> CHEST_BE = BLOCK_ENTITIES.register("dwarven_chest",
            () -> BlockEntityType.Builder.of(DwarvenChestBlockEntity::new, CHEST.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DwarvenFurnaceBlockEntity>> FURNACE_BE = BLOCK_ENTITIES.register("dwarven_furnace",
            () -> BlockEntityType.Builder.of(DwarvenFurnaceBlockEntity::new, FURNACE.get()).build(null));

    private Furniture() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
