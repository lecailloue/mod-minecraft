package net.ringsofash.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ringsofash.gen.Reg;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Sylvgarde : gardien de la Vieille-Foret. Paisible, sauf si on le frappe ou si l'on abat des arbres pres de lui. Deteste les orques.
 * Il se fige parfois en arbre pour se cacher (toujours la nuit), craint le feu (x3) et la hache (+20 %).
 * Nourri de feuilles (1 a 10), il se laisse chevaucher sur l'epaule. Tue autrement que par le feu, il devient un arbre pour toujours.
 */
public class TreemanEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> TREE = SynchedEntityData.defineId(TreemanEntity.class, EntityDataSerializers.BOOLEAN);
    /** 0 = Sylvgarde classique, 1 = ancien (massif, longue barbe de mousse), 2 = saule (maigre, rideaux de feuilles) */
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(TreemanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(TreemanEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int leavesNeeded, leavesFed;

    public TreemanEntity(EntityType<? extends TreemanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TREE, false);
        b.define(VARIANT, 0);
        b.define(OWNER, Optional.empty());
    }

    public int variant() { return entityData.get(VARIANT); }
    public void setVariant(int v) { entityData.set(VARIANT, Mth.clamp(v, 0, 2)); }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        int r = level.getRandom().nextInt(100);
        setVariant(r < 40 ? 0 : r < 70 ? 1 : 2);
        return data;
    }

    public boolean isTree() { return entityData.get(TREE); }
    public boolean isTamed() { return entityData.get(OWNER).isPresent(); }

    /** appele par l'Appel des Ents : le Sylvegarde arrive deja acquis a la cause */
    public void bind(net.minecraft.world.entity.player.Player p) {
        entityData.set(OWNER, Optional.of(p.getUUID()));
        setTree(false);
        setTarget(null);
        setPersistenceRequired();
    }
    public boolean isOwner(Entity e) { return e != null && entityData.get(OWNER).map(u -> u.equals(e.getUUID())).orElse(false); }

    public void setTree(boolean tree) {
        if (tree == isTree()) return;
        entityData.set(TREE, tree);
        if (tree) { getNavigation().stop(); setTarget(null); setDeltaMovement(0, getDeltaMovement().y, 0); }
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), tree ? SoundEvents.WOOD_PLACE : SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.6F, 0.5F);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Reg.ANCIENT_LEAVES.get().defaultBlockState()), getX(), getY() + 3, getZ(), 40, 1.2, 1.5, 1.2, 0.1);
        }
    }

    /** Sous forme d'arbre, plus aucun deplacement ni attaque. */
    private class TreeGoal extends Goal {
        TreeGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return isTree() || isVehicle(); }
        @Override public void start() { getNavigation().stop(); }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new TreeGoal());
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, FoeEntity.class, 10, true, false,
                e -> e instanceof FoeEntity f && f.kind() != FoeEntity.Kind.DEAD_SOLDIER && f.kind() != FoeEntity.Kind.MAMMOTH));
    }

    @Override
    public void setTarget(LivingEntity t) {
        if (t != null && (isOwner(t) || isTree())) return;
        super.setTarget(t);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) return;
        if (isTree()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            boolean night = level().isNight() && level().dimensionType().hasSkyLight();
            if (!night && random.nextInt(1400) == 0) setTree(false);              // se reveille au hasard (le jour)
        } else if (!isVehicle() && getTarget() == null) {
            boolean night = level().isNight() && level().dimensionType().hasSkyLight();
            if (night || random.nextInt(4000) == 0) setTree(true);               // la nuit, ou au hasard pour se cacher
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.IS_FIRE)) amount *= 3.0F;                       // le bois brule
        if (src.getEntity() instanceof LivingEntity att) {
            ItemStack w = att.getMainHandItem();
            if (w.getItem() instanceof AxeItem || w.is(ItemTags.AXES)) amount *= 1.2F;   // la hache mord le bois
        }
        boolean hit = super.hurt(src, amount);
        if (hit && isTree() && isAlive() && !src.is(DamageTypeTags.IS_FIRE)) setTree(false);   // frappe : il se reveille
        return hit;
    }

    @Override public boolean isPushable() { return !isTree() && super.isPushable(); }
    @Override public void knockback(double s, double x, double z) { if (!isTree()) super.knockback(s, x, z); }

    // ---------- nourrir et chevaucher

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (st.is(ItemTags.LEAVES)) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            if (!p.getAbilities().instabuild) st.shrink(1);
            ServerLevel sl = (ServerLevel) level();
            sl.playSound(null, blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.NEUTRAL, 1.4F, 0.6F);
            if (isTamed()) { heal(6); sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 3, getZ(), 8, 0.6, 0.8, 0.6, 0); return InteractionResult.CONSUME; }
            if (leavesNeeded <= 0) leavesNeeded = 1 + random.nextInt(10);
            leavesFed++;
            if (leavesFed >= leavesNeeded) {
                entityData.set(OWNER, Optional.of(p.getUUID()));
                setTree(false);
                setTarget(null);
                setPersistenceRequired();
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 3.5, getZ(), 10, 0.8, 0.6, 0.8, 0);
                p.displayClientMessage(Component.literal("Le Sylvgarde vous accorde sa confiance. Clic droit pour monter sur son épaule.").withStyle(ChatFormatting.DARK_GREEN), true);
            } else {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 3.5, getZ(), 6, 0.5, 0.4, 0.5, 0);
            }
            return InteractionResult.CONSUME;
        }
        if (isTamed() && isOwner(p) && !p.isSecondaryUseActive() && !isVehicle()) {
            if (!level().isClientSide) { setTree(false); p.startRiding(this); }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(p, hand);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return isTamed() && getFirstPassenger() instanceof Player p && isOwner(p) ? p : super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player p, Vec3 input) {
        super.tickRidden(p, input);
        setRot(p.getYRot(), p.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player p, Vec3 input) {
        float strafe = p.xxa * 0.5F, fwd = p.zza;
        if (fwd <= 0) fwd *= 0.3F;
        return new Vec3(strafe, 0, fwd);
    }

    @Override
    protected float getRiddenSpeed(Player p) { return (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.35F; }

    /** Le cavalier est assis sur l'epaule droite. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(-0.5, dims.height() * 0.56, 0.05).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    // ---------- mort : un arbre pour toujours (sauf brule)

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, src, recentlyHit);
        if (random.nextFloat() < 0.05F) spawnAtLocation(new ItemStack(Reg.SYLVAN_HEART.get()));
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl && !src.is(DamageTypeTags.IS_FIRE) && !isOnFire()) growTree(sl, blockPosition(), random.nextInt(3));
    }

    public static void growTree(ServerLevel sl, BlockPos base, int extra) {
        BlockState log = Reg.ANCIENT_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        BlockState leaves = Reg.ANCIENT_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int h = 6 + extra;
        for (int y = 0; y < h; y++) set(sl, base.above(y), log, true);
        for (int r = 1; r <= 2; r++) {                                           // racines
            for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                if (r == 2 && (d.get2DDataValue() + extra) % 2 == 0) continue;
                BlockPos rp = base.relative(d, r);
                if (sl.getBlockState(rp.below()).isSolid()) set(sl, rp, log.setValue(RotatedPillarBlock.AXIS, d.getAxis()), false);
            }
        }
        BlockPos top = base.above(h - 1);
        for (int dx = -3; dx <= 3; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -3; dz <= 3; dz++) {
            double d = dx * dx / 9.0 + dy * dy / 4.0 + dz * dz / 9.0;
            if (d <= 1.0 && !(d > 0.8 && sl.random.nextInt(3) == 0)) set(sl, top.offset(dx, dy + 1, dz), leaves, false);
        }
        sl.playSound(null, base, SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 2.0F, 0.4F);
    }

    private static void set(ServerLevel sl, BlockPos p, BlockState s, boolean force) {
        BlockState cur = sl.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced() || (force && cur.is(net.minecraft.tags.BlockTags.LEAVES))) sl.setBlock(p, s, Block.UPDATE_ALL);
    }

    // ---------- sauvegarde

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("Tree", isTree());
        t.putInt("Variant", variant());
        entityData.get(OWNER).ifPresent(u -> t.putUUID("Owner", u));
        t.putInt("LeavesNeeded", leavesNeeded);
        t.putInt("LeavesFed", leavesFed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(TREE, t.getBoolean("Tree"));
        setVariant(t.getInt("Variant"));
        if (t.hasUUID("Owner")) entityData.set(OWNER, Optional.of(t.getUUID("Owner")));
        leavesNeeded = t.getInt("LeavesNeeded");
        leavesFed = t.getInt("LeavesFed");
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.5, 0));
            target.hurtMarked = true;
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.6F);
        }
        return hit;
    }

    public void angerAt(LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator() || isOwner(p))) return;
        setTree(false);
        setTarget(e);
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
    @Override
    protected SoundEvent getAmbientSound() { return isTree() ? null : random.nextInt(3) == 0 ? SoundEvents.WOOD_STEP : null; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_BREAK; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WOOD_BREAK; }
    @Override
    public float getVoicePitch() { return 0.5F; }
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.WOOD_STEP, 0.6F, 0.5F); }
}
