package net.ringsofash.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.ringsofash.data.ModData;
import net.ringsofash.data.People;
import net.ringsofash.data.Rep;
import net.ringsofash.gen.Reg;

import javax.annotation.Nullable;

/** Habitants : hommes, elfes, nains, semi-hommes, rodeurs et mage errant. Ils commercent, et les gardes defendent le village. */
public class PeopleEntity extends AbstractVillager {
    public static final int CIVIL = 0, GUARD = 1, ELDER = 2;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(PeopleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(PeopleEntity.class, EntityDataSerializers.INT);
    private int profession = -1;
    private int premiumFrom = 0;
    private long lastRestock = 0;
    private long premiumDay = -1;
    private int tempLife = 0;

    public PeopleEntity(EntityType<? extends PeopleEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public int variant() { return entityData.get(VARIANT); }
    public int role() { return entityData.get(ROLE); }
    public boolean isRanger() { return getType() == ModEntities.RANGER.get(); }
    public boolean isMage() { return getType() == ModEntities.MAGE.get(); }

    public People people() {
        EntityType<?> t = getType();
        if (t == ModEntities.ELF.get()) return People.ELF;
        if (t == ModEntities.DWARF.get()) return People.DWARF;
        if (t == ModEntities.HALFLING.get()) return People.HALFLING;
        return People.HUMAN;
    }

    public void setRole(int role) {
        entityData.set(ROLE, role);
        if (role == GUARD) equipGuard();
    }

    public void setTempLife(int ticks) { this.tempLife = ticks; }

    public void setProfession(int p) { this.profession = p; this.offers = null; }

    private void equipGuard() {
        ItemStack w = switch (people()) {
            case ELF -> new ItemStack(Reg.ELVEN_BLADE.get());
            case DWARF -> new ItemStack(Reg.DWARVEN_AXE.get());
            case HALFLING -> new ItemStack(Reg.HALFLING_DAGGER.get());
            default -> new ItemStack(Reg.RANGER_SWORD.get());
        };
        setItemSlot(EquipmentSlot.MAINHAND, w);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, 0);
        b.define(ROLE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() { return fights() && super.canUse(); }
            @Override public boolean canContinueToUse() { return fights() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(3, new PanicGoal(this, 1.1) {
            @Override public boolean canUse() { return !fights() && super.canUse(); }
        });
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.0, 1.15, e -> !fights()));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.7));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return fights() && super.canUse(); }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> fights() && !(e instanceof net.minecraft.world.entity.monster.Creeper)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> fights() && e instanceof Player p && ModData.lore(p).rep(people()) <= -50));
    }

    public boolean fights() { return role() == GUARD || isRanger() || isMage(); }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        entityData.set(VARIANT, level.getRandom().nextInt(120));
        if (profession < 0) profession = level.getRandom().nextInt(Trades.professions(this));
        if (isRanger()) { setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Reg.RANGER_SWORD.get())); setDropChance(EquipmentSlot.MAINHAND, 0.0F); }
        if (isMage()) { setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Reg.STAFF_LIGHT.get())); setDropChance(EquipmentSlot.MAINHAND, 0.0F); }
        if (role() == GUARD) equipGuard();
        return data;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || player.isSecondaryUseActive()) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        int rep = ModData.lore(player).rep(people());
        if (rep <= -15) {
            player.displayClientMessage(Component.literal("« Je ne commerce pas avec les gens de votre espèce. »").withStyle(ChatFormatting.RED), true);
            playSound(SoundEvents.VILLAGER_NO, 1.0F, getVoicePitch());
            return InteractionResult.CONSUME;
        }
        if (role() == ELDER && player instanceof ServerPlayer sp) net.ringsofash.GameEvents.onElderTalk(sp, this);
        if (role() == GUARD) {
            player.displayClientMessage(Component.literal(rep >= 50 ? "« Honneur à vous, ami du village. »" : "« Circulez, et pas d'histoires. »"), true);
            return InteractionResult.CONSUME;
        }
        MerchantOffers list = getOffers();
        if (list.isEmpty()) return InteractionResult.CONSUME;
        // les meilleures offres sont reservees aux amis du peuple (et reviennent chaque jour)
        long day = level().getDayTime() / 24000L;
        for (int i = premiumFrom; i < list.size(); i++) {
            if (rep < 30) list.get(i).setToOutOfStock();
            else if (premiumDay != day) list.get(i).resetUses();
        }
        if (rep >= 30) premiumDay = day;
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void updateTrades() {
        if (profession < 0) profession = random.nextInt(Trades.professions(this));
        MerchantOffers list = getOffers();
        premiumFrom = Trades.fill(this, profession, list);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (level() instanceof ServerLevel sl) {
            sl.addFreshEntity(new ExperienceOrb(sl, getX(), getY() + 0.5, getZ(), 3 + random.nextInt(4)));
            if (getTradingPlayer() instanceof ServerPlayer sp) Rep.add(sp, people(), 2, "commerce");
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long day = level().getDayTime() / 24000L;
        if (day != lastRestock && offers != null && !isTrading()) {
            lastRestock = day;
            for (int i = 0; i < offers.size(); i++) if (i < premiumFrom) offers.get(i).resetUses();
        }
        if (tickCount % 100 == 0 && getHealth() < getMaxHealth()) heal(1);
        if (tempLife > 0 && --tempLife == 0) {
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            discard();
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override
    public boolean removeWhenFarAway(double dist) { return false; }
    @Override
    public boolean showProgressBar() { return false; }
    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PLAYER_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
    @Override
    public float getVoicePitch() {
        return switch (people()) { case DWARF -> 0.7F; case HALFLING -> 1.35F; case ELF -> 1.15F; default -> 1.0F; };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant()); tag.putInt("Role", role()); tag.putInt("Profession", profession);
        tag.putInt("PremiumFrom", premiumFrom); tag.putInt("TempLife", tempLife); tag.putLong("LastRestock", lastRestock);
        if (hasRestriction()) { tag.putLong("HomePos", getRestrictCenter().asLong()); tag.putInt("HomeRadius", (int) getRestrictRadius()); }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, tag.getInt("Variant")); entityData.set(ROLE, tag.getInt("Role"));
        profession = tag.contains("Profession") ? tag.getInt("Profession") : -1;
        premiumFrom = tag.getInt("PremiumFrom"); tempLife = tag.getInt("TempLife"); lastRestock = tag.getLong("LastRestock");
        if (tag.contains("HomePos")) restrictTo(net.minecraft.core.BlockPos.of(tag.getLong("HomePos")), tag.getInt("HomeRadius"));
    }
}
