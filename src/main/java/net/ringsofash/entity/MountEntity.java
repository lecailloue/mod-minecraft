package net.ringsofash.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Montures de la Hautemarche : Mumak (4 places), sanglier de guerre (1 place), cerf blanc (1 place).
 * Aucune n'apparait naturellement (oeuf, commande ou structure seulement). On les apprivoise en les nourrissant (quelques fois),
 * puis clic droit pour monter. Le proprietaire conduit ; pour le Mumak, les autres joueurs peuvent monter une fois le conducteur en selle.
 */
public class MountEntity extends PathfinderMob {
    public enum Kind { MAMMOTH, WAR_BOAR, STAG }

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(MountEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int fedNeeded, fed;

    public MountEntity(EntityType<? extends MountEntity> type, Level level) {
        super(type, level);
        this.xpReward = kind() == Kind.MAMMOTH ? 20 : 8;
    }

    public Kind kind() {
        EntityType<?> t = getType();
        if (t == ModEntities.MAMMOTH.get()) return Kind.MAMMOTH;
        if (t == ModEntities.WAR_BOAR.get()) return Kind.WAR_BOAR;
        return Kind.STAG;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, Optional.empty());
    }

    public boolean isTamed() { return entityData.get(OWNER).isPresent(); }
    public boolean isOwner(Entity e) { return e != null && entityData.get(OWNER).map(u -> u.equals(e.getUUID())).orElse(false); }

    public int seats() { return kind() == Kind.MAMMOTH ? 4 : 1; }

    private boolean isFood(ItemStack s) {
        return switch (kind()) {
            case MAMMOTH -> s.is(Items.HAY_BLOCK) || s.is(Items.WHEAT) || s.is(Items.SUGAR_CANE) || s.is(Items.APPLE);
            case WAR_BOAR -> s.is(Items.CARROT) || s.is(Items.POTATO) || s.is(Items.BEETROOT) || s.is(Items.ROTTEN_FLESH) || s.is(Items.PORKCHOP);
            case STAG -> s.is(Items.APPLE) || s.is(Items.SWEET_BERRIES) || s.is(Items.GOLDEN_CARROT) || s.is(Items.WHEAT);
        };
    }

    private String nameFr() {
        return switch (kind()) { case MAMMOTH -> "Le Mûmak"; case WAR_BOAR -> "Le sanglier de guerre"; case STAG -> "Le cerf blanc"; };
    }

    /** Immobile quand quelqu'un est en selle (c'est le cavalier qui conduit). */
    private class StayGoal extends Goal {
        StayGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return isVehicle(); }
        @Override public void start() { getNavigation().stop(); }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new StayGoal());
        goalSelector.addGoal(1, new FloatGoal(this));
        if (kind() == Kind.STAG) {
            goalSelector.addGoal(2, new PanicGoal(this, 1.5));
        } else {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
        }
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void setTarget(LivingEntity t) {
        if (t != null && isOwner(t)) return;
        super.setTarget(t);
    }

    // ---------- apprivoiser et monter

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (isFood(st)) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            if (!p.getAbilities().instabuild) st.shrink(1);
            ServerLevel sl = (ServerLevel) level();
            playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.8F);
            if (isTamed()) { heal(8); sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.7, getZ(), 8, 0.6, 0.6, 0.6, 0); return InteractionResult.CONSUME; }
            if (fedNeeded <= 0) fedNeeded = 3 + random.nextInt(4);
            fed++;
            if (fed >= fedNeeded) {
                entityData.set(OWNER, Optional.of(p.getUUID()));
                setTarget(null);
                setPersistenceRequired();
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.8, getZ(), 10, 0.8, 0.6, 0.8, 0);
                p.displayClientMessage(Component.literal(nameFr() + " vous accepte. Clic droit pour monter.").withStyle(ChatFormatting.GOLD), true);
            } else {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.8, getZ(), 6, 0.5, 0.4, 0.5, 0);
            }
            return InteractionResult.CONSUME;
        }
        if (isTamed() && !p.isSecondaryUseActive() && getPassengers().size() < seats()
                && (isOwner(p) || (seats() > 1 && getControllingPassenger() != null))) {
            if (!level().isClientSide) p.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(p, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity e) { return getPassengers().size() < seats(); }

    @Override
    public LivingEntity getControllingPassenger() {
        return isTamed() && getFirstPassenger() instanceof Player p && isOwner(p) ? p : null;
    }

    @Override
    protected void tickRidden(Player p, Vec3 input) {
        super.tickRidden(p, input);
        setRot(p.getYRot(), p.getXRot() * 0.4F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player p, Vec3 input) {
        float strafe = p.xxa * 0.4F, fwd = p.zza;
        if (fwd <= 0) fwd *= 0.3F;
        return new Vec3(strafe, 0, fwd);
    }

    @Override
    protected float getRiddenSpeed(Player p) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * (kind() == Kind.MAMMOTH ? 1.3F : 1.6F);
    }

    /** Sieges : coordonnees locales (x vers la gauche, y vers le haut, z vers l'avant), en blocs. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        Vec3 v = switch (kind()) {
            case MAMMOTH -> switch (i) {
                case 0 -> new Vec3(-0.32, 3.34, 0.3);
                case 1 -> new Vec3(0.32, 3.34, 0.3);
                case 2 -> new Vec3(-0.32, 3.34, -0.4);
                default -> new Vec3(0.32, 3.34, -0.4);
            };
            case WAR_BOAR -> new Vec3(0.0, 1.52, -0.02);
            case STAG -> new Vec3(0.0, 1.58, -0.05);
        };
        return v.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean isPushable() { return !isVehicle() && super.isPushable(); }

    // ---------- sons

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (kind()) { case MAMMOTH -> SoundEvents.HOGLIN_AMBIENT; case WAR_BOAR -> SoundEvents.PIG_AMBIENT; case STAG -> null; };
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return switch (kind()) { case MAMMOTH -> SoundEvents.HOGLIN_HURT; case WAR_BOAR -> SoundEvents.HOGLIN_HURT; case STAG -> SoundEvents.GOAT_HURT; };
    }
    @Override
    protected SoundEvent getDeathSound() {
        return switch (kind()) { case MAMMOTH -> SoundEvents.HOGLIN_DEATH; case WAR_BOAR -> SoundEvents.HOGLIN_DEATH; case STAG -> SoundEvents.GOAT_DEATH; };
    }
    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState st) {
        if (kind() == Kind.MAMMOTH) playSound(SoundEvents.RAVAGER_STEP, 0.5F, 0.7F);
        else if (kind() == Kind.WAR_BOAR) playSound(SoundEvents.HOGLIN_STEP, 0.5F, 0.8F);
        else super.playStepSound(pos, st);
    }
    @Override
    public float getVoicePitch() { return kind() == Kind.MAMMOTH ? 0.55F : kind() == Kind.WAR_BOAR ? 0.7F : 1.2F; }

    @Override
    public boolean removeWhenFarAway(double dist) { return !isTamed() && !hasCustomName() && !isPersistenceRequired(); }

    // ---------- sauvegarde

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        entityData.get(OWNER).ifPresent(u -> t.putUUID("Owner", u));
        t.putInt("FedNeeded", fedNeeded);
        t.putInt("Fed", fed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("Owner")) entityData.set(OWNER, Optional.of(t.getUUID("Owner")));
        fedNeeded = t.getInt("FedNeeded");
        fed = t.getInt("Fed");
    }
}
