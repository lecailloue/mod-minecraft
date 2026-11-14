package net.ringsofash.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.world.ModWorldgen;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, RingsOfAsh.MODID);

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> reg(String name, EntityType.EntityFactory<T> f, MobCategory cat, float w, float h, boolean fireImmune) {
        return ENTITIES.register(name, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(f, cat).sized(w, h).clientTrackingRange(10);
            if (fireImmune) b.fireImmune();
            return b.build(name);
        });
    }
    private static DeferredHolder<EntityType<?>, EntityType<FoeEntity>> foe(String name, float w, float h) { return reg(name, FoeEntity::new, MobCategory.MONSTER, w, h, false); }
    private static DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> people(String name, float w, float h) { return reg(name, PeopleEntity::new, MobCategory.MISC, w, h, false); }
    private static DeferredHolder<EntityType<?>, EntityType<BossEntity>> boss(String name, float w, float h, boolean fire) { return reg(name, BossEntity::new, MobCategory.MONSTER, w, h, fire); }

    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> ORC = foe("orc", 0.6F, 2.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> WAR_ORC = foe("war_orc", 0.7F, 2.35F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> ORC_CHIEF = foe("orc_chief", 0.85F, 2.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> GOBLIN = foe("goblin", 0.45F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> CAVE_TROLL = foe("cave_troll", 1.5F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> STONE_TROLL = foe("stone_troll", 1.5F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> TROLL_CHIEF = foe("troll_chief", 1.7F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> WARG = foe("warg", 1.1F, 1.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> WILD_MAN = foe("wild_man", 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> MAMMOTH = reg("mammoth", FoeEntity::new, MobCategory.CREATURE, 2.4F, 3.2F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<FoeEntity>> DEAD_SOLDIER = reg("dead_soldier", FoeEntity::new, MobCategory.MISC, 0.6F, 1.95F, true);
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowRiderEntity>> SHADOW_RIDER = reg("shadow_rider", ShadowRiderEntity::new, MobCategory.MONSTER, 1.3F, 2.7F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowRiderEntity>> SHADOW_CHIEF = reg("shadow_chief", ShadowRiderEntity::new, MobCategory.MONSTER, 1.5F, 3.0F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<GiantSpiderEntity>> GIANT_SPIDER = reg("giant_spider", GiantSpiderEntity::new, MobCategory.MONSTER, 3.0F, 2.3F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<LurkerEntity>> LURKER = reg("lurker", LurkerEntity::new, MobCategory.MONSTER, 2.4F, 2.0F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<ThiefEntity>> CAVE_THIEF = reg("cave_thief", ThiefEntity::new, MobCategory.MONSTER, 0.5F, 1.3F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<BossEntity>> FIRE_DEMON = boss("fire_demon", 2.2F, 6.4F, true);
    public static final DeferredHolder<EntityType<?>, EntityType<BossEntity>> ASH_LORD = boss("ash_lord", 1.8F, 4.8F, true);
    public static final DeferredHolder<EntityType<?>, EntityType<BossEntity>> DRAGON = boss("dragon", 3.4F, 2.6F, true);
    public static final DeferredHolder<EntityType<?>, EntityType<BossEntity>> SORCERER = boss("sorcerer", 0.6F, 2.1F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> HUMAN = people("human", 0.6F, 1.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> ELF = people("elf", 0.6F, 2.05F);
    public static final DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> DWARF = people("dwarf", 0.7F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> HALFLING = people("halfling", 0.5F, 1.15F);
    public static final DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> RANGER = people("ranger", 0.6F, 1.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<PeopleEntity>> MAGE = people("mage", 0.6F, 1.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<TreemanEntity>> TREEMAN = reg("treeman", TreemanEntity::new, MobCategory.CREATURE, 1.6F, 5.0F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<EagleEntity>> EAGLE = reg("eagle", EagleEntity::new, MobCategory.CREATURE, 1.6F, 1.6F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<BeastEntity>> STAG = reg("stag", BeastEntity::new, MobCategory.CREATURE, 0.9F, 1.5F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<BeastEntity>> CROW = reg("crow", BeastEntity::new, MobCategory.AMBIENT, 0.4F, 0.5F, false);

    public static FoeEntity.Kind kindOf(EntityType<?> t) {
        if (t == WAR_ORC.get()) return FoeEntity.Kind.WAR_ORC;
        if (t == ORC_CHIEF.get()) return FoeEntity.Kind.ORC_CHIEF;
        if (t == GOBLIN.get()) return FoeEntity.Kind.GOBLIN;
        if (t == CAVE_TROLL.get()) return FoeEntity.Kind.CAVE_TROLL;
        if (t == STONE_TROLL.get()) return FoeEntity.Kind.STONE_TROLL;
        if (t == TROLL_CHIEF.get()) return FoeEntity.Kind.TROLL_CHIEF;
        if (t == WARG.get()) return FoeEntity.Kind.WARG;
        if (t == WILD_MAN.get()) return FoeEntity.Kind.WILD_MAN;
        if (t == MAMMOTH.get()) return FoeEntity.Kind.MAMMOTH;
        if (t == DEAD_SOLDIER.get()) return FoeEntity.Kind.DEAD_SOLDIER;
        return FoeEntity.Kind.ORC;
    }

    public static BossEntity.Kind bossKindOf(EntityType<?> t) {
        if (t == ASH_LORD.get()) return BossEntity.Kind.ASH_LORD;
        if (t == DRAGON.get()) return BossEntity.Kind.DRAGON;
        if (t == SORCERER.get()) return BossEntity.Kind.SORCERER;
        return BossEntity.Kind.FIRE_DEMON;
    }

    private static AttributeSupplier mon(double hp, double dmg, double speed, double armor, double follow, double kb) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.ATTACK_DAMAGE, dmg).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor).add(Attributes.FOLLOW_RANGE, follow).add(Attributes.KNOCKBACK_RESISTANCE, kb).build();
    }
    private static AttributeSupplier mob(double hp, double dmg, double speed, double kb) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.ATTACK_DAMAGE, dmg).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, kb).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.FLYING_SPEED, 1.4).build();
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(ORC.get(), mon(24, 5, 0.27, 2, 32, 0));
        e.put(WAR_ORC.get(), mon(36, 7, 0.28, 8, 40, 0.2));
        e.put(ORC_CHIEF.get(), mon(90, 10, 0.27, 10, 40, 0.5));
        e.put(GOBLIN.get(), mon(12, 3, 0.33, 0, 24, 0));
        e.put(CAVE_TROLL.get(), mon(80, 18, 0.24, 4, 28, 0.8));
        e.put(STONE_TROLL.get(), mon(90, 18, 0.24, 6, 28, 0.9));
        e.put(TROLL_CHIEF.get(), mon(140, 24, 0.25, 6, 36, 1.0));
        e.put(WARG.get(), mon(22, 6, 0.36, 0, 36, 0));
        e.put(WILD_MAN.get(), mon(22, 4, 0.29, 0, 24, 0));
        e.put(MAMMOTH.get(), mon(120, 14, 0.22, 4, 20, 1.0));
        e.put(DEAD_SOLDIER.get(), mon(20, 7, 0.3, 4, 32, 0));
        e.put(SHADOW_RIDER.get(), mon(60, 9, 0.38, 6, 96, 0.6));
        e.put(SHADOW_CHIEF.get(), mon(150, 14, 0.42, 12, 96, 0.9));
        e.put(GIANT_SPIDER.get(), Spider.createAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 7).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 28).build());
        e.put(LURKER.get(), mon(120, 9, 0.0, 4, 20, 1.0));
        e.put(CAVE_THIEF.get(), mon(16, 2, 0.36, 0, 24, 0));
        e.put(FIRE_DEMON.get(), mon(400, 16, 0.27, 10, 48, 1.0));
        e.put(ASH_LORD.get(), mon(500, 18, 0.27, 14, 64, 1.0));
        e.put(DRAGON.get(), mon(350, 15, 0.25, 10, 40, 1.0));
        e.put(SORCERER.get(), mon(150, 6, 0.28, 2, 40, 0.2));
        for (var t : java.util.List.of(HUMAN, ELF, DWARF, HALFLING, RANGER, MAGE)) e.put(t.get(), mob(t == DWARF ? 30 : t == HALFLING ? 16 : 24, 5, 0.5, 0));
        e.put(TREEMAN.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 120).add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.STEP_HEIGHT, 1.5).build());
        e.put(EAGLE.get(), mob(40, 6, 0.25, 0.3));
        e.put(STAG.get(), mob(12, 1, 0.3, 0));
        e.put(CROW.get(), mob(4, 1, 0.25, 0));
    }

    private static boolean ash(ServerLevelAccessor l, BlockPos p) { return l.getBiome(p).is(ModWorldgen.ASH_BIOME); }

    private static <T extends Monster> boolean surface(EntityType<T> t, ServerLevelAccessor l, MobSpawnType r, BlockPos p, RandomSource rnd) {
        return Monster.checkMonsterSpawnRules(t, l, r, p, rnd) || (ash(l, p) && rnd.nextInt(3) == 0 && Monster.checkAnyLightMonsterSpawnRules(t, l, r, p, rnd));
    }
    /** Orques : uniquement la nuit, dans les biomes de cendre (les autres causes d'apparition : oeuf, forteresse, commande... restent libres). */
    private static <T extends Monster> boolean orcNight(EntityType<T> t, ServerLevelAccessor l, MobSpawnType r, BlockPos p, RandomSource rnd) {
        if (r != MobSpawnType.NATURAL && r != MobSpawnType.CHUNK_GENERATION) return Monster.checkAnyLightMonsterSpawnRules(t, l, r, p, rnd);
        return ash(l, p) && l.getLevel().isNight() && Monster.checkAnyLightMonsterSpawnRules(t, l, r, p, rnd);
    }
    private static <T extends Monster> boolean underground(EntityType<T> t, ServerLevelAccessor l, MobSpawnType r, BlockPos p, RandomSource rnd) {
        return p.getY() < 56 && !l.canSeeSky(p) && Monster.checkMonsterSpawnRules(t, l, r, p, rnd);
    }
    private static <T extends Monster> boolean nightSky(EntityType<T> t, ServerLevelAccessor l, MobSpawnType r, BlockPos p, RandomSource rnd) {
        return l.canSeeSky(p) && Monster.checkMonsterSpawnRules(t, l, r, p, rnd);
    }

    private static void placements(RegisterSpawnPlacementsEvent e) {
        var G = SpawnPlacementTypes.ON_GROUND;
        var H = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        var OP = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        e.register(ORC.get(), G, H, ModEntities::orcNight, OP);
        e.register(WAR_ORC.get(), G, H, ModEntities::orcNight, OP);
        e.register(WARG.get(), G, H, ModEntities::surface, OP);
        e.register(WILD_MAN.get(), G, H, ModEntities::surface, OP);
        e.register(GOBLIN.get(), G, H, ModEntities::underground, OP);
        e.register(CAVE_TROLL.get(), G, H, (t, l, r, p, rnd) -> underground(t, l, r, p, rnd) || (ash(l, p) && surface(t, l, r, p, rnd)), OP);
        e.register(CAVE_THIEF.get(), G, H, ModEntities::underground, OP);
        e.register(STONE_TROLL.get(), G, H, ModEntities::nightSky, OP);
        e.register(TROLL_CHIEF.get(), G, H, (t, l, r, p, rnd) -> underground(t, l, r, p, rnd) || (ash(l, p) && surface(t, l, r, p, rnd)) || nightSky(t, l, r, p, rnd), OP);
        e.register(SHADOW_RIDER.get(), G, H, (t, l, r, p, rnd) -> nightSky(t, l, r, p, rnd) && l.getLevel().isNight(), OP);
        e.register(SHADOW_CHIEF.get(), G, H, (t, l, r, p, rnd) -> nightSky(t, l, r, p, rnd) && l.getLevel().isNight(), OP);
        e.register(GIANT_SPIDER.get(), G, H, (t, l, r, p, rnd) -> rnd.nextInt(2) == 0 && Monster.checkAnyLightMonsterSpawnRules(t, l, r, p, rnd), OP);
        e.register(LURKER.get(), SpawnPlacementTypes.IN_WATER, H, (t, l, r, p, rnd) -> l.getDifficulty() != Difficulty.PEACEFUL && rnd.nextInt(10) == 0
                && l.getFluidState(p).is(FluidTags.WATER) && l.getFluidState(p.below(2)).is(FluidTags.WATER) && l.getFluidState(p.above()).isEmpty(), OP);
        e.register(MAMMOTH.get(), G, H, (t, l, r, p, rnd) -> Mob.checkMobSpawnRules(t, l, r, p, rnd), OP);
        e.register(TREEMAN.get(), G, H, (t, l, r, p, rnd) -> Mob.checkMobSpawnRules(t, l, r, p, rnd), OP);
        e.register(EAGLE.get(), G, H, (t, l, r, p, rnd) -> Mob.checkMobSpawnRules(t, l, r, p, rnd), OP);
        e.register(STAG.get(), G, H, (t, l, r, p, rnd) -> Mob.checkMobSpawnRules(t, l, r, p, rnd), OP);
        e.register(CROW.get(), G, H, (t, l, r, p, rnd) -> Mob.checkMobSpawnRules(t, l, r, p, rnd), OP);
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(ModEntities::placements);
    }

    private ModEntities() {}
}
