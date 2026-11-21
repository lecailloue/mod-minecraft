package net.ringsofash.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.gen.Reg;

import javax.annotation.Nullable;

/** Boss : Fleau de Braise (demon de feu), Seigneur de Cendre, Vieux Ver (dragon) et Sorcier corrompu. */
public class BossEntity extends Monster {
    public enum Kind { FIRE_DEMON, ASH_LORD, DRAGON, SORCERER }

    private final ServerBossEvent bar;
    private int cooldownA = 100, cooldownB = 200;
    private BlockPos home;

    public BossEntity(EntityType<? extends BossEntity> type, Level level) {
        super(type, level);
        Kind k = kind();
        this.xpReward = switch (k) { case ASH_LORD -> 500; case FIRE_DEMON -> 300; case DRAGON -> 250; case SORCERER -> 80; };
        this.bar = new ServerBossEvent(getDisplayName(), switch (k) { case FIRE_DEMON, DRAGON -> BossEvent.BossBarColor.RED; case ASH_LORD -> BossEvent.BossBarColor.PURPLE; case SORCERER -> BossEvent.BossBarColor.WHITE; },
                BossEvent.BossBarOverlay.NOTCHED_10);
        this.bar.setDarkenScreen(k == Kind.ASH_LORD || k == Kind.FIRE_DEMON);
        if (k == Kind.DRAGON) { this.moveControl = new FlyingMoveControl(this, 6, true); setNoGravity(true); }   // le Vieux Ver ne se pose jamais
        setPersistenceRequired();
    }

    public Kind kind() { return ModEntities.bossKindOf(getType()); }

    @Override
    protected PathNavigation createNavigation(Level level) { return kind() == Kind.DRAGON ? Flight.navigation(this, level) : super.createNavigation(level); }

    @Override
    protected void registerGoals() {
        if (kind() == Kind.DRAGON) {
            goalSelector.addGoal(3, new Flight.AttackGoal(this, 1.0, 3.0));
            goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
            goalSelector.addGoal(8, new RandomLookAroundGoal(this));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PeopleEntity.class, false));
            return;
        }
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PeopleEntity.class, false));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        home = blockPosition();
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Kind k = kind();
        if (k == Kind.DRAGON) setNoGravity(true);
        if (level().isClientSide) {
            if (k == Kind.FIRE_DEMON) {
                for (int i = 0; i < 3; i++) level().addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.03, 0);
            } else if (k == Kind.ASH_LORD && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ASH, getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0, 0);
            }
            return;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth() && getTarget() == null) heal(k == Kind.SORCERER ? 1 : 3);
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        double d = distanceTo(t);
        ServerLevel sl = (ServerLevel) level();
        boolean enraged = getHealth() < getMaxHealth() * 0.4F;
        if (--cooldownA <= 0) {
            cooldownA = enraged ? 70 : 120;
            switch (k) {
                case FIRE_DEMON -> {
                    if (d > 7) {
                        // salve de boules de feu
                        for (int i = 0; i < (enraged ? 5 : 3); i++) {
                            Vec3 dir = t.getEyePosition().subtract(getEyePosition()).normalize().add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.2);
                            SmallFireball fb = new SmallFireball(sl, this, dir.scale(1.2));
                            fb.setPos(getX() + dir.x * 2, getEyeY() - 0.5, getZ() + dir.z * 2);
                            sl.addFreshEntity(fb);
                        }
                        playSound(SoundEvents.BLAZE_SHOOT, 3.0F, 0.5F);
                    } else {
                        // fouet de flammes : attire la cible
                        Vec3 pull = position().subtract(t.position()).normalize().scale(1.4);
                        t.setDeltaMovement(pull.x, 0.4, pull.z); t.hurtMarked = true;
                        t.igniteForSeconds(5);
                        playSound(SoundEvents.FIRECHARGE_USE, 3.0F, 0.6F);
                    }
                }
                case ASH_LORD -> shockwave(sl, 9, 10F, 2.2);
                case DRAGON -> fireBreath(sl, t, enraged ? 16 : 12);
                case SORCERER -> {
                    if (d > 5) {
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                        if (bolt != null) { bolt.moveTo(Vec3.atBottomCenterOf(t.blockPosition())); sl.addFreshEntity(bolt); }
                    } else fangs(sl, t);
                }
            }
        }
        if (--cooldownB <= 0) {
            cooldownB = enraged ? 160 : 260;
            switch (k) {
                case FIRE_DEMON -> {
                    // explosion de braises autour de lui
                    shockwave(sl, 6, 8F, 1.5);
                    for (int i = 0; i < 14; i++) {
                        BlockPos p = blockPosition().offset(random.nextInt(11) - 5, 0, random.nextInt(11) - 5);
                        if (sl.getBlockState(p).isAir() && sl.getBlockState(p.below()).isSolidRender(sl, p.below())) sl.setBlock(p, Blocks.FIRE.defaultBlockState(), 3);
                    }
                    sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 150, 3, 1, 3, 0.1);
                }
                case ASH_LORD -> {
                    summon(sl, enraged ? 4 : 3, ModEntities.WAR_ORC.get());
                    for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(24))) {
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0), this);
                        p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), this);
                    }
                    playSound(SoundEvents.WITHER_SPAWN, 2.0F, 0.6F);
                }
                case DRAGON -> { shockwave(sl, 7, 9F, 2.4); playSound(SoundEvents.ENDER_DRAGON_FLAP, 4.0F, 0.6F); }
                case SORCERER -> {
                    if (enraged) summon(sl, 2, ModEntities.ORC.get());
                    // se teleporte hors de portee
                    for (int i = 0; i < 16; i++) {
                        double x = getX() + (random.nextDouble() - 0.5) * 16, z = getZ() + (random.nextDouble() - 0.5) * 16;
                        if (randomTeleport(x, getY(), z, true)) { playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.7F); break; }
                    }
                }
            }
        }
    }

    private void shockwave(ServerLevel sl, double radius, float damage, double knock) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.6F);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 12, radius / 2, 0.3, radius / 2, 0);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 60, radius / 2, 0.2, radius / 2, 0.05);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), e -> e != this && !(e instanceof Monster))) {
            if (e.distanceTo(this) > radius) continue;
            e.hurt(damageSources().mobAttack(this), damage);
            e.knockback(knock, getX() - e.getX(), getZ() - e.getZ());
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0)); e.hurtMarked = true;
        }
    }

    private void fireBreath(ServerLevel sl, LivingEntity t, int range) {
        Vec3 dir = t.getEyePosition().subtract(getEyePosition()).normalize();
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.8F);
        for (int i = 2; i < range; i++) {
            Vec3 p = getEyePosition().add(dir.scale(i));
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 14, 0.3 + i * 0.07, 0.3 + i * 0.05, 0.3 + i * 0.07, 0.03);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(p, p).inflate(1.2 + i * 0.1), e -> e != this && !(e instanceof Monster))) {
                e.igniteForSeconds(6);
                e.hurt(damageSources().mobAttack(this), 7F);
            }
        }
    }

    private void fangs(ServerLevel sl, LivingEntity t) {
        Vec3 dir = t.position().subtract(position()).normalize();
        for (int i = 1; i < 12; i++) {
            Vec3 p = position().add(dir.scale(i * 1.2));
            sl.addFreshEntity(new EvokerFangs(sl, p.x, t.getY(), p.z, getYRot(), i, this));
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.8F);
    }

    private void summon(ServerLevel sl, int n, EntityType<FoeEntity> type) {
        for (int i = 0; i < n; i++) {
            FoeEntity f = type.create(sl);
            if (f == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            f.moveTo(getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4, random.nextFloat() * 360, 0);
            f.finalizeSpawn(sl, sl.getCurrentDifficultyAt(f.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            sl.addFreshEntity(f);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, f.getX(), f.getY() + 1, f.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
            if (getTarget() != null) f.setTarget(getTarget());
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            switch (kind()) {
                case FIRE_DEMON -> le.igniteForSeconds(6);
                case ASH_LORD -> { le.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), this); le.knockback(1.8, getX() - le.getX(), getZ() - le.getZ()); }
                case DRAGON -> le.knockback(1.4, getX() - le.getX(), getZ() - le.getZ());
                default -> { }
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && kind() != Kind.SORCERER) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING) && kind() == Kind.SORCERER) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) net.ringsofash.GameEvents.onBossDeath(this, source);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override
    public void setCustomName(@Nullable Component name) { super.setCustomName(name); bar.setName(getDisplayName()); }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
    @Override
    public boolean fireImmune() { return kind() != Kind.SORCERER; }
    @Override
    public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override
    public boolean isPushable() { return kind() == Kind.SORCERER; }
    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource source) { return kind() != Kind.DRAGON && super.causeFallDamage(dist, mult, source); }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (kind()) { case FIRE_DEMON -> SoundEvents.BLAZE_AMBIENT; case ASH_LORD -> SoundEvents.WITHER_AMBIENT; case DRAGON -> SoundEvents.ENDER_DRAGON_AMBIENT; case SORCERER -> SoundEvents.EVOKER_AMBIENT; };
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return switch (kind()) { case FIRE_DEMON -> SoundEvents.BLAZE_HURT; case ASH_LORD -> SoundEvents.WITHER_HURT; case DRAGON -> SoundEvents.ENDER_DRAGON_HURT; case SORCERER -> SoundEvents.EVOKER_HURT; };
    }
    @Override
    protected SoundEvent getDeathSound() {
        return switch (kind()) { case FIRE_DEMON -> SoundEvents.BLAZE_DEATH; case ASH_LORD -> SoundEvents.WITHER_DEATH; case DRAGON -> SoundEvents.ENDER_DRAGON_GROWL; case SORCERER -> SoundEvents.EVOKER_DEATH; };
    }
    @Override
    public float getVoicePitch() { return kind() == Kind.SORCERER ? 0.8F : 0.5F; }
    @Override
    protected float getSoundVolume() { return 2.5F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.putLong("Home", home.asLong());
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) home = BlockPos.of(tag.getLong("Home"));
        if (hasCustomName()) bar.setName(getDisplayName());
    }
}
