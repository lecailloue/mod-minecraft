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
    public enum Kind { ORC, WAR_ORC, ORC_CHIEF, GOBLIN, CAVE_TROLL, STONE_TROLL, WARG, WIGHT, WILD_MAN, MAMMOTH, DEAD_SOLDIER }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(FoeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STONE = SynchedEntityData.defineId(FoeEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifeLeft = -1;

    public FoeEntity(EntityType<? extends FoeEntity> type, Level level) {
        super(type, level);
        Kind k = kind();
        this.xpReward = switch (k) { case ORC_CHIEF -> 40; case CAVE_TROLL, STONE_TROLL, MAMMOTH -> 20; case WAR_ORC, WIGHT -> 10; case GOBLIN -> 3; default -> 6; };
        if (k == Kind.DEAD_SOLDIER) lifeLeft = 20 * 90;
    }

    public Kind kind() { return ModEntities.kindOf(getType()); }
    public int variant() { return entityData.get(VARIANT); }
    public boolean isStone() { return entityData.get(STONE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, 0);
        b.define(STONE, false);
    }

    @Override
    protected void registerGoals() {
        Kind k = kind();
        goalSelector.addGoal(0, new FloatGoal(this));
        if (k == Kind.GOBLIN) { goalSelector.addGoal(1, new RestrictSunGoal(this)); goalSelector.addGoal(2, new FleeSunGoal(this, 1.1)); }
        if (k == Kind.GOBLIN || k == Kind.WARG) goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.42F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, k == Kind.WARG ? 1.25 : 1.1, false));
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
        entityData.set(VARIANT, r.nextInt(k == Kind.ORC ? 3 : 2));
        populateDefaultEquipmentSlots(r, difficulty);
        if (k == Kind.WARG && reason != MobSpawnType.SPAWN_EGG && r.nextInt(4) == 0) {
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
            case ORC -> new ItemStack(r.nextInt(3) == 0 ? Reg.ORC_CLEAVER.get() : Reg.ORC_SCIMITAR.get());
            case WAR_ORC -> new ItemStack(r.nextBoolean() ? Reg.ORC_SCIMITAR.get() : Reg.ORC_CLEAVER.get());
            case ORC_CHIEF -> new ItemStack(Reg.ORC_CLEAVER.get());
            case GOBLIN -> r.nextInt(3) == 0 ? new ItemStack(Items.STONE_SWORD) : ItemStack.EMPTY;
            case WIGHT -> new ItemStack(Reg.WIGHT_BLADE.get());
            case WILD_MAN -> new ItemStack(r.nextBoolean() ? Items.STONE_AXE : Reg.RIDER_SPEAR.get());
            case DEAD_SOLDIER -> new ItemStack(Reg.RANGER_SWORD.get());
            default -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.MAINHAND, hand);
        setDropChance(EquipmentSlot.MAINHAND, kind() == Kind.WIGHT ? 0.2F : kind() == Kind.DEAD_SOLDIER ? 0.0F : 0.06F);
        if (kind() == Kind.WAR_ORC || kind() == Kind.ORC_CHIEF) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Reg.ORC_HELMET.get()));
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(Reg.ORC_CHESTPLATE.get()));
            setDropChance(EquipmentSlot.HEAD, 0.05F); setDropChance(EquipmentSlot.CHEST, 0.05F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        Kind k = kind();
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
            case CAVE_TROLL, STONE_TROLL -> isStone() ? null : SoundEvents.RAVAGER_AMBIENT;
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
            case CAVE_TROLL, STONE_TROLL -> SoundEvents.RAVAGER_HURT;
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
            case CAVE_TROLL, STONE_TROLL -> SoundEvents.RAVAGER_DEATH;
            case WARG -> SoundEvents.WOLF_DEATH;
            case WIGHT, DEAD_SOLDIER -> SoundEvents.STRAY_DEATH;
            case WILD_MAN -> SoundEvents.PILLAGER_DEATH;
            case MAMMOTH -> SoundEvents.HOGLIN_DEATH;
        };
    }

    @Override
    public float getVoicePitch() {
        float base = switch (kind()) { case GOBLIN -> 1.5F; case WAR_ORC -> 0.8F; case ORC_CHIEF -> 0.65F; case CAVE_TROLL, STONE_TROLL -> 0.7F; case WARG -> 0.75F; case MAMMOTH -> 0.5F; default -> 1.0F; };
        return base + (random.nextFloat() - 0.5F) * 0.15F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        Kind k = kind();
        if (k == Kind.CAVE_TROLL || k == Kind.STONE_TROLL || k == Kind.MAMMOTH) playSound(SoundEvents.RAVAGER_STEP, 0.4F, 0.8F);
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
