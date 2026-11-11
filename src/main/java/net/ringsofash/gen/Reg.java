// FICHIER GENERE par tools/gen.py - ne pas modifier a la main
package net.ringsofash.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.block.*;
import net.ringsofash.item.*;
import java.util.Optional;

public final class Reg {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ringsofash");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ringsofash");
    static BlockBehaviour.Properties copy(Block b) { return BlockBehaviour.Properties.ofFullCopy(b); }
    static Item.Properties props() { return new Item.Properties(); }
    static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("ringsofash", p); }

    public static final DeferredBlock<Block> ASH_BLOCK = BLOCKS.register("ash_block", () -> new Block(copy(Blocks.SAND)));
    public static final DeferredBlock<Block> VOLCANIC_ROCK = BLOCKS.register("volcanic_rock", () -> new Block(copy(Blocks.BLACKSTONE)));
    public static final DeferredBlock<Block> SMOKING_ROCK = BLOCKS.register("smoking_rock", () -> new SmokingRockBlock(copy(Blocks.BLACKSTONE).lightLevel(s -> 6)));
    public static final DeferredBlock<Block> PALE_LAVA = BLOCKS.register("pale_lava", () -> new PaleLavaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).noCollission().forceSolidOn().strength(-1.0F, 3600000.0F).lightLevel(s -> 15).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Block> WHITE_STONE = BLOCKS.register("white_stone", () -> new Block(copy(Blocks.STONE)));
    public static final DeferredBlock<Block> WHITE_BRICKS = BLOCKS.register("white_bricks", () -> new Block(copy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> WHITE_BRICKS_STAIRS = BLOCKS.register("white_bricks_stairs", () -> new StairBlock(WHITE_BRICKS.get().defaultBlockState(), copy(WHITE_BRICKS.get())));
    public static final DeferredBlock<Block> WHITE_BRICKS_SLAB = BLOCKS.register("white_bricks_slab", () -> new SlabBlock(copy(WHITE_BRICKS.get())));
    public static final DeferredBlock<Block> WHITE_BRICKS_WALL = BLOCKS.register("white_bricks_wall", () -> new WallBlock(copy(WHITE_BRICKS.get()).forceSolidOn()));
    public static final DeferredBlock<Block> WHITE_PILLAR = BLOCKS.register("white_pillar", () -> new RotatedPillarBlock(copy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> DWARVEN_BRICKS = BLOCKS.register("dwarven_bricks", () -> new Block(copy(Blocks.DEEPSLATE_BRICKS)));
    public static final DeferredBlock<Block> DWARVEN_BRICKS_STAIRS = BLOCKS.register("dwarven_bricks_stairs", () -> new StairBlock(DWARVEN_BRICKS.get().defaultBlockState(), copy(DWARVEN_BRICKS.get())));
    public static final DeferredBlock<Block> DWARVEN_BRICKS_SLAB = BLOCKS.register("dwarven_bricks_slab", () -> new SlabBlock(copy(DWARVEN_BRICKS.get())));
    public static final DeferredBlock<Block> DWARVEN_BRICKS_WALL = BLOCKS.register("dwarven_bricks_wall", () -> new WallBlock(copy(DWARVEN_BRICKS.get()).forceSolidOn()));
    public static final DeferredBlock<Block> DWARVEN_PILLAR = BLOCKS.register("dwarven_pillar", () -> new RotatedPillarBlock(copy(Blocks.DEEPSLATE_BRICKS)));
    public static final DeferredBlock<Block> DWARVEN_RUNES = BLOCKS.register("dwarven_runes", () -> new Block(copy(Blocks.DEEPSLATE_BRICKS).lightLevel(s -> 7)));
    public static final DeferredBlock<Block> DWARVEN_LAMP = BLOCKS.register("dwarven_lamp", () -> new Block(copy(Blocks.DEEPSLATE_BRICKS).lightLevel(s -> 15)));
    public static final DeferredBlock<Block> BLACK_BRICKS = BLOCKS.register("black_bricks", () -> new Block(copy(Blocks.POLISHED_BLACKSTONE_BRICKS)));
    public static final DeferredBlock<Block> BLACK_BRICKS_STAIRS = BLOCKS.register("black_bricks_stairs", () -> new StairBlock(BLACK_BRICKS.get().defaultBlockState(), copy(BLACK_BRICKS.get())));
    public static final DeferredBlock<Block> BLACK_BRICKS_SLAB = BLOCKS.register("black_bricks_slab", () -> new SlabBlock(copy(BLACK_BRICKS.get())));
    public static final DeferredBlock<Block> BLACK_BRICKS_WALL = BLOCKS.register("black_bricks_wall", () -> new WallBlock(copy(BLACK_BRICKS.get()).forceSolidOn()));
    public static final DeferredBlock<Block> GOLDEN_LOG = BLOCKS.register("golden_log", () -> new RotatedPillarBlock(copy(Blocks.BIRCH_LOG)));
    public static final DeferredBlock<Block> GOLDEN_PLANKS = BLOCKS.register("golden_planks", () -> new Block(copy(Blocks.BIRCH_PLANKS)));
    public static final DeferredBlock<Block> GOLDEN_PLANKS_STAIRS = BLOCKS.register("golden_planks_stairs", () -> new StairBlock(GOLDEN_PLANKS.get().defaultBlockState(), copy(GOLDEN_PLANKS.get())));
    public static final DeferredBlock<Block> GOLDEN_PLANKS_SLAB = BLOCKS.register("golden_planks_slab", () -> new SlabBlock(copy(GOLDEN_PLANKS.get())));
    public static final DeferredBlock<Block> GOLDEN_PLANKS_FENCE = BLOCKS.register("golden_planks_fence", () -> new FenceBlock(copy(GOLDEN_PLANKS.get())));
    public static final DeferredBlock<Block> GOLDEN_LEAVES = BLOCKS.register("golden_leaves", () -> new LeavesBlock(copy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<Block> GOLDEN_SAPLING = BLOCKS.register("golden_sapling", () -> new SaplingBlock(new TreeGrower("roa_golden", Optional.empty(), Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, id("golden_tree_small"))), Optional.empty()), copy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<Block> ANCIENT_LOG = BLOCKS.register("ancient_log", () -> new RotatedPillarBlock(copy(Blocks.DARK_OAK_LOG)));
    public static final DeferredBlock<Block> ANCIENT_PLANKS = BLOCKS.register("ancient_planks", () -> new Block(copy(Blocks.DARK_OAK_PLANKS)));
    public static final DeferredBlock<Block> ANCIENT_PLANKS_STAIRS = BLOCKS.register("ancient_planks_stairs", () -> new StairBlock(ANCIENT_PLANKS.get().defaultBlockState(), copy(ANCIENT_PLANKS.get())));
    public static final DeferredBlock<Block> ANCIENT_PLANKS_SLAB = BLOCKS.register("ancient_planks_slab", () -> new SlabBlock(copy(ANCIENT_PLANKS.get())));
    public static final DeferredBlock<Block> ANCIENT_PLANKS_FENCE = BLOCKS.register("ancient_planks_fence", () -> new FenceBlock(copy(ANCIENT_PLANKS.get())));
    public static final DeferredBlock<Block> ANCIENT_LEAVES = BLOCKS.register("ancient_leaves", () -> new LeavesBlock(copy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<Block> THATCH = BLOCKS.register("thatch", () -> new Block(copy(Blocks.HAY_BLOCK)));
    public static final DeferredBlock<Block> THATCH_STAIRS = BLOCKS.register("thatch_stairs", () -> new StairBlock(THATCH.get().defaultBlockState(), copy(THATCH.get())));
    public static final DeferredBlock<Block> THATCH_SLAB = BLOCKS.register("thatch_slab", () -> new SlabBlock(copy(THATCH.get())));
    public static final DeferredBlock<Block> LUNARGENT_ORE = BLOCKS.register("lunargent_ore", () -> new DropExperienceBlock(UniformInt.of(3, 7), copy(Blocks.DEEPSLATE_DIAMOND_ORE)));
    public static final DeferredBlock<Block> LUNARGENT_BLOCK = BLOCKS.register("lunargent_block", () -> new Block(copy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> GOLD_PILE = BLOCKS.register("gold_pile", () -> new Block(copy(Blocks.GOLD_BLOCK)));
    public static final DeferredBlock<Block> SUNSTAR = BLOCKS.register("sunstar", () -> new FlowerBlock(MobEffects.REGENERATION, 6.0F, copy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> MOONBELL = BLOCKS.register("moonbell", () -> new FlowerBlock(MobEffects.NIGHT_VISION, 8.0F, copy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> ASHROOT = BLOCKS.register("ashroot", () -> new AshPlantBlock(copy(Blocks.DEAD_BUSH)));
    public static final DeferredBlock<Block> PIPEWEED = BLOCKS.register("pipeweed", () -> new FlowerBlock(MobEffects.SLOW_FALLING, 5.0F, copy(Blocks.DANDELION)));

    public static final DeferredItem<BlockItem> BI_ASH_BLOCK = ITEMS.registerSimpleBlockItem("ash_block", ASH_BLOCK);
    public static final DeferredItem<BlockItem> BI_VOLCANIC_ROCK = ITEMS.registerSimpleBlockItem("volcanic_rock", VOLCANIC_ROCK);
    public static final DeferredItem<BlockItem> BI_SMOKING_ROCK = ITEMS.registerSimpleBlockItem("smoking_rock", SMOKING_ROCK);
    public static final DeferredItem<BlockItem> BI_PALE_LAVA = ITEMS.registerSimpleBlockItem("pale_lava", PALE_LAVA);
    public static final DeferredItem<BlockItem> BI_WHITE_STONE = ITEMS.registerSimpleBlockItem("white_stone", WHITE_STONE);
    public static final DeferredItem<BlockItem> BI_WHITE_BRICKS = ITEMS.registerSimpleBlockItem("white_bricks", WHITE_BRICKS);
    public static final DeferredItem<BlockItem> BI_WHITE_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem("white_bricks_stairs", WHITE_BRICKS_STAIRS);
    public static final DeferredItem<BlockItem> BI_WHITE_BRICKS_SLAB = ITEMS.registerSimpleBlockItem("white_bricks_slab", WHITE_BRICKS_SLAB);
    public static final DeferredItem<BlockItem> BI_WHITE_BRICKS_WALL = ITEMS.registerSimpleBlockItem("white_bricks_wall", WHITE_BRICKS_WALL);
    public static final DeferredItem<BlockItem> BI_WHITE_PILLAR = ITEMS.registerSimpleBlockItem("white_pillar", WHITE_PILLAR);
    public static final DeferredItem<BlockItem> BI_DWARVEN_BRICKS = ITEMS.registerSimpleBlockItem("dwarven_bricks", DWARVEN_BRICKS);
    public static final DeferredItem<BlockItem> BI_DWARVEN_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem("dwarven_bricks_stairs", DWARVEN_BRICKS_STAIRS);
    public static final DeferredItem<BlockItem> BI_DWARVEN_BRICKS_SLAB = ITEMS.registerSimpleBlockItem("dwarven_bricks_slab", DWARVEN_BRICKS_SLAB);
    public static final DeferredItem<BlockItem> BI_DWARVEN_BRICKS_WALL = ITEMS.registerSimpleBlockItem("dwarven_bricks_wall", DWARVEN_BRICKS_WALL);
    public static final DeferredItem<BlockItem> BI_DWARVEN_PILLAR = ITEMS.registerSimpleBlockItem("dwarven_pillar", DWARVEN_PILLAR);
    public static final DeferredItem<BlockItem> BI_DWARVEN_RUNES = ITEMS.registerSimpleBlockItem("dwarven_runes", DWARVEN_RUNES);
    public static final DeferredItem<BlockItem> BI_DWARVEN_LAMP = ITEMS.registerSimpleBlockItem("dwarven_lamp", DWARVEN_LAMP);
    public static final DeferredItem<BlockItem> BI_BLACK_BRICKS = ITEMS.registerSimpleBlockItem("black_bricks", BLACK_BRICKS);
    public static final DeferredItem<BlockItem> BI_BLACK_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem("black_bricks_stairs", BLACK_BRICKS_STAIRS);
    public static final DeferredItem<BlockItem> BI_BLACK_BRICKS_SLAB = ITEMS.registerSimpleBlockItem("black_bricks_slab", BLACK_BRICKS_SLAB);
    public static final DeferredItem<BlockItem> BI_BLACK_BRICKS_WALL = ITEMS.registerSimpleBlockItem("black_bricks_wall", BLACK_BRICKS_WALL);
    public static final DeferredItem<BlockItem> BI_GOLDEN_LOG = ITEMS.registerSimpleBlockItem("golden_log", GOLDEN_LOG);
    public static final DeferredItem<BlockItem> BI_GOLDEN_PLANKS = ITEMS.registerSimpleBlockItem("golden_planks", GOLDEN_PLANKS);
    public static final DeferredItem<BlockItem> BI_GOLDEN_PLANKS_STAIRS = ITEMS.registerSimpleBlockItem("golden_planks_stairs", GOLDEN_PLANKS_STAIRS);
    public static final DeferredItem<BlockItem> BI_GOLDEN_PLANKS_SLAB = ITEMS.registerSimpleBlockItem("golden_planks_slab", GOLDEN_PLANKS_SLAB);
    public static final DeferredItem<BlockItem> BI_GOLDEN_PLANKS_FENCE = ITEMS.registerSimpleBlockItem("golden_planks_fence", GOLDEN_PLANKS_FENCE);
    public static final DeferredItem<BlockItem> BI_GOLDEN_LEAVES = ITEMS.registerSimpleBlockItem("golden_leaves", GOLDEN_LEAVES);
    public static final DeferredItem<BlockItem> BI_GOLDEN_SAPLING = ITEMS.registerSimpleBlockItem("golden_sapling", GOLDEN_SAPLING);
    public static final DeferredItem<BlockItem> BI_ANCIENT_LOG = ITEMS.registerSimpleBlockItem("ancient_log", ANCIENT_LOG);
    public static final DeferredItem<BlockItem> BI_ANCIENT_PLANKS = ITEMS.registerSimpleBlockItem("ancient_planks", ANCIENT_PLANKS);
    public static final DeferredItem<BlockItem> BI_ANCIENT_PLANKS_STAIRS = ITEMS.registerSimpleBlockItem("ancient_planks_stairs", ANCIENT_PLANKS_STAIRS);
    public static final DeferredItem<BlockItem> BI_ANCIENT_PLANKS_SLAB = ITEMS.registerSimpleBlockItem("ancient_planks_slab", ANCIENT_PLANKS_SLAB);
    public static final DeferredItem<BlockItem> BI_ANCIENT_PLANKS_FENCE = ITEMS.registerSimpleBlockItem("ancient_planks_fence", ANCIENT_PLANKS_FENCE);
    public static final DeferredItem<BlockItem> BI_ANCIENT_LEAVES = ITEMS.registerSimpleBlockItem("ancient_leaves", ANCIENT_LEAVES);
    public static final DeferredItem<BlockItem> BI_THATCH = ITEMS.registerSimpleBlockItem("thatch", THATCH);
    public static final DeferredItem<BlockItem> BI_THATCH_STAIRS = ITEMS.registerSimpleBlockItem("thatch_stairs", THATCH_STAIRS);
    public static final DeferredItem<BlockItem> BI_THATCH_SLAB = ITEMS.registerSimpleBlockItem("thatch_slab", THATCH_SLAB);
    public static final DeferredItem<BlockItem> BI_LUNARGENT_ORE = ITEMS.registerSimpleBlockItem("lunargent_ore", LUNARGENT_ORE);
    public static final DeferredItem<BlockItem> BI_LUNARGENT_BLOCK = ITEMS.registerSimpleBlockItem("lunargent_block", LUNARGENT_BLOCK);
    public static final DeferredItem<BlockItem> BI_GOLD_PILE = ITEMS.registerSimpleBlockItem("gold_pile", GOLD_PILE);
    public static final DeferredItem<BlockItem> BI_SUNSTAR = ITEMS.registerSimpleBlockItem("sunstar", SUNSTAR);
    public static final DeferredItem<BlockItem> BI_MOONBELL = ITEMS.registerSimpleBlockItem("moonbell", MOONBELL);
    public static final DeferredItem<BlockItem> BI_ASHROOT = ITEMS.registerSimpleBlockItem("ashroot", ASHROOT);
    public static final DeferredItem<BlockItem> BI_PIPEWEED = ITEMS.registerSimpleBlockItem("pipeweed", PIPEWEED);

    public static final DeferredItem<Item> RAW_LUNARGENT = ITEMS.register("raw_lunargent", () -> new Item(props()));
    public static final DeferredItem<Item> LUNARGENT_INGOT = ITEMS.register("lunargent_ingot", () -> new Item(props()));
    public static final DeferredItem<Item> BLACK_STEEL_INGOT = ITEMS.register("black_steel_ingot", () -> new Item(props()));
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.register("silver_coin", () -> new Item(props()));
    public static final DeferredItem<Item> HALFLING_RING = ITEMS.register("halfling_ring", () -> new TravelRingItem(props().stacksTo(1).fireResistant().rarity(Rarity.RARE)));
    private Reg() {}
}
