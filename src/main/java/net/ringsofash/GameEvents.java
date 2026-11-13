package net.ringsofash;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ringsofash.data.Guide;
import net.ringsofash.data.ModData;
import net.ringsofash.data.Net;
import net.ringsofash.data.People;
import net.ringsofash.data.PlayerLore;
import net.ringsofash.data.Rep;
import net.ringsofash.entity.BossEntity;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PaleRiderEntity;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.entity.TreemanEntity;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.ModGear;
import net.ringsofash.item.RingItem;
import net.ringsofash.world.ModWorldgen;
import net.ringsofash.world.SelfTest;
import net.ringsofash.world.Travel;
import net.ringsofash.world.VolcanoPiece;

/** Evenements de jeu (cote serveur) : anneaux, corruption, reputation, quete. */
public final class GameEvents {
    public static final TagKey<Structure> ARRIVAL_VILLAGES = stag("arrival_villages");
    public static final TagKey<Structure> T_HUMAN = stag("villages_human"), T_ELF = stag("villages_elf"), T_DWARF = stag("villages_dwarf"), T_HALFLING = stag("villages_halfling");
    public static final TagKey<Structure> T_MINES = stag("dwarf_mines"), T_FORTS = stag("orc_forts"), T_TOWERS = stag("dark_towers"), T_BARROWS = stag("barrows"), T_HOARDS = stag("hoards");
    private static final ResourceLocation GROW_ID = RingsOfAsh.id("grow");

    private GameEvents() {}

    private static TagKey<Structure> stag(String n) { return TagKey.create(Registries.STRUCTURE, RingsOfAsh.id(n)); }

    public static void init(IEventBus modBus) {
        modBus.addListener(Net::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking ev) -> {
            if (ev.getEntity() instanceof ServerPlayer viewer && ev.getTarget() instanceof ServerPlayer target) net.ringsofash.item.SylvanHeart.sendTo(viewer, target);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove ev) -> {
            if (ev.getEntity() instanceof ServerPlayer sp && ev.getEffect() != null && ev.getEffect().is(net.ringsofash.effect.ModEffects.SYLVAN_FORM)) net.ringsofash.item.SylvanHeart.broadcast(sp, 0);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired ev) -> {
            if (ev.getEntity() instanceof ServerPlayer sp && ev.getEffectInstance() != null && ev.getEffectInstance().getEffect().is(net.ringsofash.effect.ModEffects.SYLVAN_FORM)) net.ringsofash.item.SylvanHeart.broadcast(sp, 0);
        });
        // sous forme de Sylvgarde, le clic droit ne pose ni n'utilise rien (il lance les racines)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock ev) -> {
            if (ev.getEntity().hasEffect(net.ringsofash.effect.ModEffects.SYLVAN_FORM)) ev.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem ev) -> {
            if (ev.getEntity().hasEffect(net.ringsofash.effect.ModEffects.SYLVAN_FORM)) ev.setCanceled(true);
        });
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(GameEvents::onEntityTick);
        bus.addListener(GameEvents::onSleepFinished);
        bus.addListener(GameEvents::onCommands);
        bus.addListener(GameEvents::onServerStarted);
        bus.addListener(GameEvents::onPlayerTick);
        bus.addListener(GameEvents::onVisibility);
        bus.addListener(GameEvents::onDeath);
        bus.addListener(GameEvents::onDamage);
        bus.addListener(GameEvents::onDrops);
        bus.addListener(GameEvents::onClone);
        bus.addListener(GameEvents::onLogin);
        bus.addListener(GameEvents::onChangeDim);
        bus.addListener(GameEvents::onRightClickBlock);
        bus.addListener(GameEvents::onRightClickItem);
        bus.addListener(GameEvents::onBreak);
    }

    // ------------------------------------------------------------------ objets jetes
    private static void onEntityTick(EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof ItemEntity item) || item.level().isClientSide) return;
        Travel.tickItem(item);
        if (item.getItem().is(Reg.ASH_RING.get()) && item.level() instanceof ServerLevel sl && Travel.isLands(sl)) {
            BlockPos pos = item.blockPosition();
            if (!sl.getFluidState(pos).is(FluidTags.LAVA) && !sl.getFluidState(pos.below()).is(FluidTags.LAVA)) return;
            StructureStart start = sl.structureManager().getStructureWithPieceAt(pos, ModWorldgen.VOLCANOES);
            if (!start.isValid() || !(start.getPieces().get(0) instanceof VolcanoPiece piece)) return;
            item.discard();
            summonAshLord(sl, piece.lakeCenter());
        }
    }

    /** L'Anneau de Cendre est detruit : le lac se fige et le Seigneur de Cendre apparait. */
    public static void summonAshLord(ServerLevel sl, BlockPos lake) {
        BlockPos p = lake;
        if (!sl.getFluidState(p).is(FluidTags.LAVA)) p = p.below();
        Travel.transformLake(sl, p, Reg.VOLCANIC_ROCK.get().defaultBlockState(), Reg.SMOKING_ROCK.get().defaultBlockState());
        BossEntity lord = ModEntities.ASH_LORD.get().create(sl);
        if (lord == null) return;
        lord.moveTo(lake.getX() + 0.5, lake.getY() + 1, lake.getZ() + 0.5, 0, 0);
        lord.finalizeSpawn(sl, sl.getCurrentDifficultyAt(lake), MobSpawnType.EVENT, null);
        sl.addFreshEntity(lord);
        sl.playSound(null, lake, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 10.0F, 0.5F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, lake.getX(), lake.getY() + 2, lake.getZ(), 6, 4, 1, 4, 0);
        for (ServerPlayer pl : sl.getPlayers(q -> q.distanceToSqr(lake.getX(), lake.getY(), lake.getZ()) < 200 * 200)) {
            pl.displayClientMessage(Component.literal("L'Anneau fond... et son maître vient le réclamer. Le lac s'est figé : descendez l'affronter !").withStyle(ChatFormatting.DARK_RED), false);
            pl.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
            PlayerLore lore = ModData.lore(pl);
            lore.quest = Math.max(lore.quest, 2);
            Net.sync(pl);
        }
    }

    private static void onSleepFinished(SleepFinishedTimeEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && Travel.isLands(sl)) sl.getServer().overworld().setDayTime(e.getNewTime());
    }

    private static void onServerStarted(ServerStartedEvent e) {
        if (System.getProperty("roa.selftest") != null) SelfTest.run(e.getServer());
    }

    // ------------------------------------------------------------------ arrivee, ancien
    public static void onFirstArrival(ServerPlayer p) {
        Travel.give(p, new ItemStack(Reg.HALFLING_RING.get()));
        Travel.give(p, Guide.create(p));
        Net.sync(p);
    }

    public static void onElderTalk(ServerPlayer p, PeopleEntity elder) {
        if (!hasItem(p, Reg.HALFLING_RING.get())) {
            Travel.give(p, new ItemStack(Reg.HALFLING_RING.get()));
            p.displayClientMessage(Component.literal("« Vous avez égaré votre anneau ? Tenez, et gardez-le mieux cette fois. »").withStyle(ChatFormatting.GREEN), false);
        }
        if (!hasGuide(p)) Travel.give(p, Guide.create(p));
        PlayerLore lore = ModData.lore(p);
        String hint = switch (lore.quest) {
            case 0 -> "« On dit qu'un démon de braise garde un anneau maudit au fond des mines naines des Monts de Fer. »";
            case 1 -> "« Cet anneau... ne le gardez pas. Portez-le au Mont Funeste, dans les Terres de Cendre, et jetez-le au feu. »";
            case 2 -> "« Le Seigneur de Cendre est sorti de l'ombre. Seule la lame du roi elfe le blesse vraiment. »";
            default -> "« La Hautemarche vous doit sa paix. Vous serez toujours chez vous ici. »";
        };
        p.displayClientMessage(Component.literal(hint).withStyle(ChatFormatting.YELLOW), false);
    }

    private static boolean hasItem(Player p, net.minecraft.world.item.Item item) {
        return p.getInventory().contains(new ItemStack(item)) || p.getOffhandItem().is(item);
    }
    private static boolean hasGuide(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (Guide.isGuide(p.getInventory().getItem(i))) return true;
        return false;
    }

    // ------------------------------------------------------------------ tick joueur : anneaux, corruption, oeil
    private static void eff(Player p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        p.addEffect(new MobEffectInstance(e, e == MobEffects.NIGHT_VISION ? 320 : 70, amp, true, false, true));
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Travel.keepInside(p);
        if (p.tickCount % 20 != 0) return;
        PlayerLore lore = ModData.lore(p);
        RingItem.Kind ring = RingItem.worn(p);
        int corrupt = 0;
        if (ring != null) {
            switch (ring) {
                case ASH -> { eff(p, MobEffects.INVISIBILITY, 0); corrupt = 3; lore.eye += 1; }
                case WATERS -> { eff(p, MobEffects.REGENERATION, 0); eff(p, MobEffects.WATER_BREATHING, 0); }
                case WINDS -> { eff(p, MobEffects.MOVEMENT_SPEED, 0); eff(p, MobEffects.JUMP, 1); eff(p, MobEffects.SLOW_FALLING, 0); }
                case EMBERS -> { eff(p, MobEffects.FIRE_RESISTANCE, 0); eff(p, MobEffects.DAMAGE_BOOST, 0); }
                case FORGE -> eff(p, MobEffects.DIG_SPEED, 1);
                case HOARD -> { eff(p, MobEffects.LUCK, 1); eff(p, MobEffects.NIGHT_VISION, 0); if (p.tickCount % 100 == 0) corrupt = 1; }
                case KINGS -> { eff(p, MobEffects.DAMAGE_BOOST, 1); eff(p, MobEffects.DAMAGE_RESISTANCE, 0); corrupt = 1; }
                case SHADOWS -> { eff(p, MobEffects.NIGHT_VISION, 0); corrupt = 1; }
            }
        }
        if (corrupt > 0) lore.corruption = Math.min(1000, lore.corruption + corrupt);
        else if (lore.corruption > 0 && p.tickCount % 60 == 0) lore.corruption--;
        if (ring != RingItem.Kind.ASH && lore.eye > 0) lore.eye = Math.max(0, lore.eye - 2);
        // effets de la corruption
        if (lore.corruption > 200 && p.getRandom().nextInt(30) == 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        if (lore.corruption > 500 && p.getRandom().nextInt(40) == 0) { p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1)); p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0)); }
        if (lore.corruption > 800 && p.getRandom().nextInt(40) == 0) p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        // l'Oeil
        if (lore.eye >= 120) {
            lore.eye = 0;
            summonRiders(p, 2 + p.getRandom().nextInt(2));
        } else if (lore.eye == 60 || lore.eye == 90) {
            p.displayClientMessage(Component.literal("Vous sentez un regard brûlant se tourner vers vous...").withStyle(ChatFormatting.DARK_RED), true);
        }
        // equipements du lore : casque de mineur, bottes de semi-homme, cape de cendre
        if (p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Reg.MINER_HELMET.get()) && p.getY() < 60 && !p.level().canSeeSky(p.blockPosition())) {
            eff(p, MobEffects.NIGHT_VISION, 0); eff(p, MobEffects.DIG_SPEED, 0);
        }
        if (p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Reg.HALFLING_BOOTS.get())) eff(p, MobEffects.MOVEMENT_SPEED, 0);
        if (p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Reg.ASH_CLOAK.get())) eff(p, MobEffects.FIRE_RESISTANCE, 0);
        // couronne de Valmoire : les Hommes proches se sentent soutenus
        if (p.tickCount % 40 == 0 && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Reg.VALMOIRE_CROWN.get())) {
            for (PeopleEntity pe : p.level().getEntitiesOfClass(PeopleEntity.class, p.getBoundingBox().inflate(24), q -> q.people() == People.HUMAN)) {
                pe.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
                pe.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            }
        }
        // ensembles d'armure
        if (ModGear.fullSet(p, ModGear.LUNARGENT)) eff(p, MobEffects.DAMAGE_RESISTANCE, 0);
        else if (ModGear.fullSet(p, ModGear.ELVEN)) eff(p, MobEffects.MOVEMENT_SPEED, 0);
        else if (ModGear.fullSet(p, ModGear.DWARVEN)) eff(p, MobEffects.DIG_SPEED, 0);
        else if (ModGear.fullSet(p, ModGear.ORC)) eff(p, MobEffects.DAMAGE_BOOST, 0);
        else if (ModGear.fullSet(p, ModGear.ASHLORD)) { eff(p, MobEffects.FIRE_RESISTANCE, 0); eff(p, MobEffects.DAMAGE_BOOST, 1); }
        else if (ModGear.fullSet(p, ModGear.ELFKING)) { eff(p, MobEffects.REGENERATION, 0); eff(p, MobEffects.MOVEMENT_SPEED, 0); }
        // quete : l'anneau de cendre est en votre possession
        if (lore.quest == 0 && hasItem(p, Reg.ASH_RING.get())) {
            lore.quest = 1;
            p.displayClientMessage(Component.literal("Quête : portez l'Anneau de Cendre au Mont Funeste et jetez-le dans la lave du cratère.").withStyle(ChatFormatting.GOLD), false);
        }
        // seve vive
        if (lore.grow > 0) {
            lore.grow -= 20;
            if (lore.grow <= 0) {
                AttributeInstance a = p.getAttribute(Attributes.SCALE);
                if (a != null) a.removeModifier(GROW_ID);
            }
        }
        if (p.tickCount % 100 == 0) Net.sync(p);
    }

    public static void grow(ServerPlayer p, int ticks) {
        AttributeInstance a = p.getAttribute(Attributes.SCALE);
        if (a == null) return;
        a.removeModifier(GROW_ID);
        a.addTransientModifier(new AttributeModifier(GROW_ID, 0.8, AttributeModifier.Operation.ADD_VALUE));
        ModData.lore(p).grow = ticks;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
    }

    public static void summonRiders(ServerPlayer p, int n) {
        ServerLevel sl = p.serverLevel();
        int done = 0;
        for (int i = 0; i < n * 6 && done < n; i++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            int r = 22 + p.getRandom().nextInt(12);
            int x = p.getBlockX() + (int) (Math.cos(a) * r), z = p.getBlockZ() + (int) (Math.sin(a) * r);
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - p.getBlockY()) > 24) y = p.getBlockY();
            BlockPos pos = new BlockPos(x, y, z);
            if (!sl.getBlockState(pos).getCollisionShape(sl, pos).isEmpty() || !sl.getFluidState(pos).isEmpty()) continue;
            PaleRiderEntity rider = ModEntities.PALE_RIDER.get().create(sl);
            if (rider == null) continue;
            rider.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            rider.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            rider.setSummoned(2400);
            rider.setTarget(p);
            sl.addFreshEntity(rider);
            done++;
        }
        if (done > 0) {
            p.displayClientMessage(Component.literal("L'Œil vous a trouvé. Les cavaliers blêmes arrivent !").withStyle(ChatFormatting.DARK_RED), false);
            sl.playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }

    private static void onVisibility(LivingEvent.LivingVisibilityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getLookingEntity() instanceof PaleRiderEntity) return;
        RingItem.Kind ring = RingItem.worn(p);
        if (ring == RingItem.Kind.ASH) e.modifyVisibility(0.0);
        else if (ring == RingItem.Kind.SHADOWS) e.modifyVisibility(0.4);
        ItemStack cloak = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (cloak.is(Reg.ELVEN_CLOAK.get())) e.modifyVisibility(p.isShiftKeyDown() ? 0.3 : 0.75);
        else if (cloak.is(Reg.RANGER_CLOAK.get())) e.modifyVisibility(p.isShiftKeyDown() ? 0.2 : 0.55);
    }

    // ------------------------------------------------------------------ reputation, bestiaire
    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (dead instanceof PeopleEntity pe) {
            Rep.add(p, pe.people(), -20, "meurtre");
            for (People other : People.values()) if (other != pe.people()) ModData.lore(p).addRep(other, -3);
            Net.sync(p);
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (id.getNamespace().equals(RingsOfAsh.MODID)) discover(p, id.getPath());
    }

    public static void discover(ServerPlayer p, String id) {
        if (!Guide.BESTIARY.containsKey(id)) return;
        if (ModData.lore(p).bestiary.add(id)) {
            p.displayClientMessage(Component.literal("Bestiaire : nouvelle page — " + Guide.BESTIARY.get(id)[0]).withStyle(ChatFormatting.AQUA), false);
            p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && e.getEntity().hasEffect(net.ringsofash.effect.ModEffects.SYLVAN_FORM)) e.setAmount(e.getAmount() * 3.0F);
        if (e.getEntity() instanceof Player pl && e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                && pl.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Reg.HALFLING_BOOTS.get())) e.setAmount(e.getAmount() * 0.4F);
        if (e.getEntity() instanceof PeopleEntity pe && e.getSource().getEntity() instanceof ServerPlayer p && !p.isCreative() && pe.invulnerableTime <= 10) {
            Rep.add(p, pe.people(), -4, "agression");
        }
    }

    public static void onBossDeath(BossEntity boss, DamageSource source) {
        ServerLevel sl = (ServerLevel) boss.level();
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp ? sp : sl.getNearestPlayer(boss, 64) instanceof ServerPlayer np ? np : null;
        if (boss.kind() != BossEntity.Kind.ASH_LORD) return;
        boolean dark = killer != null && ModData.lore(killer).general() < 0;
        ItemStack[] reward = dark
                ? new ItemStack[]{new ItemStack(Reg.ASHLORD_HELMET.get()), new ItemStack(Reg.ASHLORD_CHESTPLATE.get()), new ItemStack(Reg.ASHLORD_LEGGINGS.get()), new ItemStack(Reg.ASHLORD_BOOTS.get()), new ItemStack(Reg.ASH_MACE.get())}
                : new ItemStack[]{new ItemStack(Reg.ELFKING_HELMET.get()), new ItemStack(Reg.ELFKING_CHESTPLATE.get()), new ItemStack(Reg.ELFKING_LEGGINGS.get()), new ItemStack(Reg.ELFKING_BOOTS.get()), new ItemStack(Reg.KINGS_BLADE.get())};
        for (ItemStack s : reward) {
            ItemEntity it = new ItemEntity(sl, boss.getX(), boss.getY() + 1, boss.getZ(), s);
            it.setUnlimitedLifetime();
            it.setGlowingTag(true);
            sl.addFreshEntity(it);
        }
        for (ServerPlayer pl : sl.getPlayers(q -> q.distanceToSqr(boss) < 128 * 128)) {
            PlayerLore lore = ModData.lore(pl);
            lore.quest = 3;
            lore.rewarded = true;
            if (!dark) for (People pp : People.values()) lore.addRep(pp, 30);
            pl.displayClientMessage(Component.literal(dark
                    ? "Le Seigneur de Cendre est tombé... et son armure vous appelle. Un nouveau maître se lève."
                    : "Le Seigneur de Cendre est tombé. Les peuples libres vous acclament : vous voici Héros de la Hautemarche !").withStyle(dark ? ChatFormatting.DARK_RED : ChatFormatting.GOLD), false);
            Net.sync(pl);
        }
    }

    /** L'anneau des semi-hommes et le livre ne se perdent pas a la mort. */
    private static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer)) return;
        e.getDrops().removeIf(it -> it.getItem().is(Reg.HALFLING_RING.get()) || Guide.isGuide(it.getItem()));
    }

    private static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (ModData.lore(p).arrived && !p.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            Travel.give(p, new ItemStack(Reg.HALFLING_RING.get()));
            Travel.give(p, Guide.create(p));
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }
    private static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }

    private static People ownerAt(ServerLevel sl, BlockPos pos) {
        var sm = sl.structureManager();
        if (sm.getStructureWithPieceAt(pos, T_HUMAN).isValid()) return People.HUMAN;
        if (sm.getStructureWithPieceAt(pos, T_ELF).isValid()) return People.ELF;
        if (sm.getStructureWithPieceAt(pos, T_DWARF).isValid()) return People.DWARF;
        if (sm.getStructureWithPieceAt(pos, T_HALFLING).isValid()) return People.HALFLING;
        return null;
    }

    /** Ouvrir pour la premiere fois un coffre d'un village, c'est le piller. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel sl) || !Travel.isLands(sl)) return;
        if (sl.getBlockEntity(e.getPos()) instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null) {
            People owner = ownerAt(sl, e.getPos());
            if (owner != null) Rep.add(p, owner, -6, "pillage d'un coffre");
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (Guide.isGuide(e.getItemStack()) && e.getEntity() instanceof ServerPlayer p) Guide.fill(e.getItemStack(), p);
    }

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !e.getState().is(BlockTags.LOGS)) return;
        for (TreemanEntity t : p.level().getEntitiesOfClass(TreemanEntity.class, p.getBoundingBox().inflate(20))) {
            t.angerAt(p);
            p.displayClientMessage(Component.literal("Un sylvegarde vous a vu abattre un arbre !").withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ pierre de vision
    public static void seeingStone(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        p.displayClientMessage(Component.literal("La pierre s'éclaire...").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        String[] names = {"Village de semi-hommes", "Village des hommes", "Village elfe", "Cité naine", "Mine naine", "Forteresse orque", "Tour noire", "Tertres", "Trésor du Ver", "Volcan"};
        java.util.List<TagKey<Structure>> tags = java.util.List.of(T_HALFLING, T_HUMAN, T_ELF, T_DWARF, T_MINES, T_FORTS, T_TOWERS, T_BARROWS, T_HOARDS, ModWorldgen.VOLCANOES);
        for (int i = 0; i < names.length; i++) {
            BlockPos pos = null;
            try { pos = sl.findNearestMapStructure(tags.get(i), p.blockPosition(), 60, false); } catch (Exception ex) { RingsOfAsh.LOGGER.debug("recherche", ex); }
            if (pos == null) continue;
            int dx = pos.getX() - p.getBlockX(), dz = pos.getZ() - p.getBlockZ();
            int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
            p.displayClientMessage(Component.literal(" • " + names[i] + " : " + dist + " blocs, " + direction(dx, dz) + " (" + pos.getX() + ", " + pos.getZ() + ")").withStyle(ChatFormatting.GRAY), false);
        }
    }

    private static String direction(int dx, int dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        if (a < 0) a += 360;
        String[] d = {"au nord", "au nord-est", "à l'est", "au sud-est", "au sud", "au sud-ouest", "à l'ouest", "au nord-ouest"};
        return d[(int) Math.round(a / 45) % 8];
    }

    // ------------------------------------------------------------------ commandes
    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("anneaux")
                .then(Commands.literal("reputation").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    PlayerLore l = ModData.lore(p);
                    for (People pp : People.values()) p.displayClientMessage(Component.literal(pp.label + " : " + l.rep(pp) + " (" + People.rank(l.rep(pp)) + ")"), false);
                    p.displayClientMessage(Component.literal("Réputation générale : " + l.general() + " — corruption : " + l.corruption / 10 + " %").withStyle(ChatFormatting.GOLD), false);
                    return 1;
                }))
                .then(Commands.literal("livre").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Travel.give(p, Guide.create(p));
                    return 1;
                }))
                .then(Commands.literal("hautemarche").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!Travel.isLands(p.level())) ModData.lore(p).setOverworld(p.blockPosition());
                    Travel.toLands(p);
                    return 1;
                }))
                .then(Commands.literal("retour").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    Travel.toOverworld(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("volcan").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel level = p.serverLevel();
                    BlockPos pos = level.findNearestMapStructure(ModWorldgen.VOLCANOES, p.blockPosition(), 200, false);
                    if (pos == null) { ctx.getSource().sendFailure(Component.literal("Aucun volcan trouvé à proximité.")); return 0; }
                    Travel.teleport(p, level, pos.offset(100, 0, 0), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("Volcan en " + pos.getX() + ", " + pos.getZ() + " (à l'ouest de vous).").withStyle(ChatFormatting.GOLD), false);
                    return 1;
                }))
                .then(Commands.literal("carte").requires(s -> s.hasPermission(2)).then(Commands.argument("lieu", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(net.ringsofash.world.MapData.places().keySet(), b))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "lieu");
                            int[] at = net.ringsofash.world.MapData.place(name);
                            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                            if (at == null || lands == null) { ctx.getSource().sendFailure(Component.literal("Lieu inconnu : " + name)); return 0; }
                            if (!Travel.isLands(p.level())) ModData.lore(p).setOverworld(p.blockPosition());
                            Travel.teleport(p, lands, new BlockPos(at[0], 0, at[1]), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(name + " : x=" + at[0] + " z=" + at[1]).withStyle(ChatFormatting.GOLD), false);
                            return 1;
                        })))
                .then(Commands.literal("lieux").requires(s -> s.hasPermission(2)).executes(ctx -> { seeingStone(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("rep").requires(s -> s.hasPermission(2)).then(Commands.argument("valeur", IntegerArgumentType.integer(-100, 100)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int v = IntegerArgumentType.getInteger(ctx, "valeur");
                    for (People pp : People.values()) ModData.lore(p).rep[pp.ordinal()] = v;
                    Net.sync(p);
                    return 1;
                }))));
    }
}
