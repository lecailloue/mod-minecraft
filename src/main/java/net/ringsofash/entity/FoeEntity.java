package net.ringsofash.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.ringsofash.gen.Reg;

import javax.annotation.Nullable;

/** Ennemis de melee : orques, gobelins, trolls, loups de guerre, spectres des tertres, hommes sauvages, mammouths. */
public class FoeEntity extends Monster {
    public enum Kind { ORC, WAR_ORC, ORC_CHIEF, GOBLIN, CAVE_TROLL, STONE_TROLL, TROLL_CHIEF, WARG, WIGHT, WILD_MAN, MAMMOTH, DEAD_SOLDIER }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(FoeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STONE = SynchedEntityData.defineId(FoeEntity.class, EntityDataSerializers.BOOLEAN);
    /** Trolls : tick du coup de poing en cours (0 = aucun) et course a quatre pattes (synchronises pour l'animation). */
    private static final EntityDataAccessor<Integer> SLAM = SynchedEntityData.defineId(FoeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(FoeEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifeLeft = -1;
    private float runBlend;
    private boolean poseLock;

    public FoeEntity(EntityType<? extends FoeEntity> type, Level level) {
        super(type, level);
        Kind k = kind();
        this.xpReward = switch (k) { case ORC_CHIEF -> 40; case TROLL_CHIEF -> 35; case CAVE_TROLL, STONE_TROLL, MAMMOTH -> 20; case WAR_ORC, WIGHT -> 10; case GOBLIN -> 3; default -> 6; };
        if (k == Kind.DEAD_SOLDIER) lifeLeft = 20 * 90;
    }

    public Kind kind() { return ModEntities.kindOf(getType()); }
    public int variant() { return entityData.get(VARIANT); }
    /** Modele d'orque 0..6 : 0-3 orques, 4-5 orques noirs, 6 chef (variante orque = modele*4 + couleur, orque noir = modele*2 + couleur). */
    /** Mise en scene des tests client : impose la variante (modele + couleur). */
    public void testVariant(int v) { entityData.set(VARIANT, v); populateDefaultEquipmentSlots(random, null); }
    public int orcModel() {
        return switch (kind()) { case ORC -> Math.floorMod(variant() / 4, 4); case WAR_ORC -> 4 + Math.floorMod(variant() / 2, 2); case ORC_CHIEF -> 6; default -> 0; };
    }
    public int orcColor() {
        return switch (kind()) { case ORC -> Math.floorMod(variant(), 4); case WAR_ORC -> Math.floorMod(variant(), 2); default -> 0; };
    }
    /** L'arme que tient chaque modele d'orque (celle qu'il laisse tomber). */
    private ItemStack orcWeapon() {
        return new ItemStack(switch (orcModel()) {
            case 0, 5 -> Reg.ORC_CLEAVER.get();
            case 1, 4 -> Reg.ORC_SCIMITAR.get();
            case 2 -> Reg.ORC_MACHETE.get();
            case 3 -> Reg.ORC_SPEAR.get();
            default -> Reg.ORC_MACE.get();
        });
    }
    public boolean isStone() { return entityData.get(STONE); }
    public boolean isTroll() { Kind k = kind(); return k == Kind.CAVE_TROLL || k == Kind.STONE_TROLL || k == Kind.TROLL_CHIEF; }
    public int slamTick() { return entityData.get(SLAM); }
    public void setSlam(int t) { entityData.set(SLAM, t); }
    public boolean isRunning() { return entityData.get(RUNNING); }
    /** Fondu 0..1 vers la course a quatre pattes (cote client, pour l'animation). */
    public float runBlend() { return runBlend; }
    /** Mise en scene des tests client : fige la pose (coup de poing au tick donne, ou course). */
    public void testPose(boolean running, int slam) { poseLock = true; entityData.set(RUNNING, running); entityData.set(SLAM, slam); runBlend = running ? 1F : 0F; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, 0);
        b.define(STONE, false);
        b.define(SLAM, 0);
        b.define(RUNNING, false);
    }

    @Override
    protected void registerGoals() {
        Kind k = kind();
        goalSelector.addGoal(0, new FloatGoal(this));
        if (k == Kind.GOBLIN) { goalSelector.addGoal(1, new RestrictSunGoal(this)); goalSelector.addGoal(2, new FleeSunGoal(this, 1.1)); }
        if (k == Kind.GOBLIN || k == Kind.WARG) goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.42F));
        if (k == Kind.CAVE_TROLL || k == Kind.STONE_TROLL || k == Kind.TROLL_CHIEF) goalSelector.addGoal(4, new TrollSlamGoal(this, k == Kind.TROLL_CHIEF ? 1.35 : 1.2));
        else goalSelector.addGoal(4, new MeleeAttackGoal(this, k == Kind.WARG ? 1.25 : 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (k == Kind.DEAD_SOLDIER) {
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, 5, true, false, e -> !(e instanceof FoeEntity f && f.kind() == Kind.DEAD_SOLDIER)));
            return;
        }
        targetSelector.addGoal(1, new HurtByTargetGoal(this, FoeEntity.class).setAlertOthers());
        if (k == Kind.MAMMOTH) return;
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> !(e instanceof Player p && ((k == Kind.WILD_MAN && net.ringsofash.data.ModData.lore(p).general() >= 15)
                        || ((k == Kind.ORC || k == Kind.WAR_ORC || k == Kind.GOBLIN) && net.ringsofash.item.ModGear.fullSet(p, net.ringsofash.item.ModGear.ASHLORD))))));
        if (k != Kind.WIGHT) {
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PeopleEntity.class, true));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, FoeEntity.class, 10, true, false, e -> e instanceof FoeEntity f && f.kind() == Kind.DEAD_SOLDIER));
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (kind() != Kind.DEAD_SOLDIER && target instanceof FoeEntity f && f.kind() != Kind.DEAD_SOLDIER) return false;
        return super.canAttack(target);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        RandomSource r = level.getRandom();
        Kind k = kind();
        entityData.set(VARIANT, r.nextInt(k == Kind.ORC ? 16 : k == Kind.WAR_ORC ? 4 : 2));
        populateDefaultEquipmentSlots(r, difficulty);
        if (k == Kind.TROLL_CHIEF && reason != MobSpawnType.SPAWN_EGG && reason != MobSpawnType.COMMAND) {
            // le chef de meute arrive avec sa meute : deux ou trois trolls des cavernes autour de lui
            for (int i = 0, n = 2 + r.nextInt(2); i < n; i++) {
                FoeEntity t = ModEntities.CAVE_TROLL.get().create(level.getLevel());
                if (t == null) continue;
                t.moveTo(getX() + (r.nextDouble() - 0.5) * 7, getY(), getZ() + (r.nextDouble() - 0.5) * 7, r.nextFloat() * 360F, 0);
                if (!level.noCollision(t)) t.moveTo(getX(), getY(), getZ(), r.nextFloat() * 360F, 0);
                t.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
                level.addFreshEntity(t);
            }
        }
        if (k == Kind.WARG && reason != MobSpawnType.SPAWN_EGG && level.getLevel().isNight() && level.getBiome(blockPosition()).is(net.ringsofash.world.ModWorldgen.ASH_BIOME) && r.nextInt(4) == 0) {
            FoeEntity orc = ModEntities.ORC.get().create(level.getLevel());
            if (orc != null) {
                orc.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                orc.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
                orc.startRiding(this);
                level.addFreshEntity(orc);
            }
        }
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource r, DifficultyInstance difficulty) {
        ItemStack hand = switch (kind()) {
            case ORC, WAR_ORC, ORC_CHIEF -> orcWeapon();
            case GOBLIN -> r.nextInt(3) == 0 ? new ItemStack(Items.STONE_SWORD) : ItemStack.EMPTY;
            case WIGHT -> new ItemStack(Reg.WIGHT_BLADE.get());
            case WILD_MAN -> new ItemStack(r.nextBoolean() ? Items.STONE_AXE : Reg.RIDER_SPEAR.get());
            case DEAD_SOLDIER -> new ItemStack(Reg.RANGER_SWORD.get());
            default -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.MAINHAND, hand);
        setDropChance(EquipmentSlot.MAINHAND, kind() == Kind.WIGHT ? 0.2F : kind() == Kind.DEAD_SOLDIER ? 0.0F : kind() == Kind.ORC_CHIEF ? 0.4F : (kind() == Kind.ORC || kind() == Kind.WAR_ORC) ? 0.15F : 0.06F);
        if (kind() == Kind.WAR_ORC || kind() == Kind.ORC_CHIEF) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Reg.ORC_HELMET.get()));
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(Reg.ORC_CHESTPLATE.get()));
            setDropChance(EquipmentSlot.HEAD, 0.05F); setDropChance(EquipmentSlot.CHEST, 0.05F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isTroll()) runBlend += ((isRunning() ? 1F : 0F) - runBlend) * 0.25F;
        if (level().isClientSide) return;
        Kind k = kind();
        if (isTroll() && !poseLock) {
            // course a quatre pattes quand il chasse
            boolean run = !isStone() && isAggressive() && getTarget() != null && slamTick() == 0 && getDeltaMovement().horizontalDistanceSqr() > 0.004;
            if (run != isRunning()) entityData.set(RUNNING, run);
            if (k == Kind.TROLL_CHIEF && tickCount % 20 == 0 && getTarget() != null) rally(getTarget());
        }
        if (k == Kind.STONE_TROLL && tickCount % 20 == 0) {
            boolean sun = level().isDay() && level().canSeeSky(blockPosition().above(2)) && !level().isRaining();
            if (sun != isStone()) {
                entityData.set(STONE, sun);
                setNoAi(sun);
                setTarget(null);
                playSound(sun ? SoundEvents.STONE_PLACE : SoundEvents.STONE_BREAK, 2.0F, 0.5F);
            }
        }
        if (k == Kind.WIGHT && level().isDay() && tickCount % 40 == 0 && level().canSeeSky(blockPosition())) {
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
            discard();
        }
        if (lifeLeft > 0 && --lifeLeft == 0) {
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
            discard();
        }
    }

    /** Le chef de meute designe sa cible a tous les trolls proches. */
    private void rally(LivingEntity target) {
        for (FoeEntity f : level().getEntitiesOfClass(FoeEntity.class, getBoundingBox().inflate(24), o -> o != this && o.isTroll() && o.getTarget() == null)) {
            if (f.canAttack(target)) f.setTarget(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (kind() == Kind.STONE_TROLL && isStone()) {
            // change en pierre : seule une pioche l'entame
            Entity e = source.getEntity();
            boolean pick = e instanceof LivingEntity le && le.getMainHandItem().is(ItemTags.PICKAXES);
            if (!pick && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                playSound(SoundEvents.STONE_HIT, 1.0F, 0.7F);
                return false;
            }
            amount *= 3.0F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            switch (kind()) {
                case WIGHT -> { le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this); le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this); }
                case CAVE_TROLL, STONE_TROLL, MAMMOTH -> le.knockback(1.6, getX() - le.getX(), getZ() - le.getZ());
                case TROLL_CHIEF -> le.knockback(1.9, getX() - le.getX(), getZ() - le.getZ());
                case WARG -> { if (random.nextInt(4) == 0) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this); }
                default -> { }
            }
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        Kind k = kind();
        return k != Kind.ORC_CHIEF && k != Kind.MAMMOTH && !isPersistenceRequired();
    }

    @Override
    public boolean isSensitiveToWater() { return false; }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (kind()) {
            case ORC, WAR_ORC, ORC_CHIEF -> SoundEvents.PIGLIN_BRUTE_AMBIENT;
            case GOBLIN -> SoundEvents.PIGLIN_AMBIENT;
            case CAVE_TROLL, STONE_TROLL, TROLL_CHIEF -> isStone() ? null : SoundEvents.RAVAGER_AMBIENT;
            case WARG -> SoundEvents.WOLF_GROWL;
            case WIGHT, DEAD_SOLDIER -> SoundEvents.STRAY_AMBIENT;
            case WILD_MAN -> SoundEvents.PILLAGER_AMBIENT;
            case MAMMOTH -> SoundEvents.HOGLIN_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return switch (kind()) {
            case ORC, WAR_ORC, ORC_CHIEF -> SoundEvents.PIGLIN_BRUTE_HURT;
            case GOBLIN -> SoundEvents.PIGLIN_HURT;
            case CAVE_TROLL, STONE_TROLL, TROLL_CHIEF -> SoundEvents.RAVAGER_HURT;
            case WARG -> SoundEvents.WOLF_HURT;
            case WIGHT, DEAD_SOLDIER -> SoundEvents.STRAY_HURT;
            case WILD_MAN -> SoundEvents.PILLAGER_HURT;
            case MAMMOTH -> SoundEvents.HOGLIN_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return switch (kind()) {
            case ORC, WAR_ORC, ORC_CHIEF -> SoundEvents.PIGLIN_BRUTE_DEATH;
            case GOBLIN -> SoundEvents.PIGLIN_DEATH;
            case CAVE_TROLL, STONE_TROLL, TROLL_CHIEF -> SoundEvents.RAVAGER_DEATH;
            case WARG -> SoundEvents.WOLF_DEATH;
            case WIGHT, DEAD_SOLDIER -> SoundEvents.STRAY_DEATH;
            case WILD_MAN -> SoundEvents.PILLAGER_DEATH;
            case MAMMOTH -> SoundEvents.HOGLIN_DEATH;
        };
    }

    @Override
    public float getVoicePitch() {
        float base = switch (kind()) { case GOBLIN -> 1.5F; case WAR_ORC -> 0.8F; case ORC_CHIEF -> 0.65F; case CAVE_TROLL, STONE_TROLL -> 0.7F; case TROLL_CHIEF -> 0.58F; case WARG -> 0.75F; case MAMMOTH -> 0.5F; default -> 1.0F; };
        return base + (random.nextFloat() - 0.5F) * 0.15F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        Kind k = kind();
        if (k == Kind.CAVE_TROLL || k == Kind.STONE_TROLL || k == Kind.TROLL_CHIEF || k == Kind.MAMMOTH) playSound(SoundEvents.RAVAGER_STEP, 0.4F, 0.8F);
        else super.playStepSound(pos, state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putInt("LifeLeft", lifeLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, tag.getInt("Variant"));
        if (tag.contains("LifeLeft")) lifeLeft = tag.getInt("LifeLeft");
    }
}
