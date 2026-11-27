package net.ringsofash.item;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ringsofash.RingsOfAsh;

/**
 * Ce que les runes font vraiment.
 *
 * Les chiffres (degats, vitesse, allonge) passent par des modificateurs d'attribut, donc
 * ils apparaissent dans l'infobulle de l'arme comme ceux d'une enchantement. Les effets
 * a l'impact (feu, vent, poison, rune noire) se declenchent au coup porte. L'usure, elle,
 * est reglee une fois pour toutes au moment du scellement (voir Rune.socket).
 */
public final class RuneEffects {
    private RuneEffects() {}

    public static void init(IEventBus bus) {
        bus.addListener(RuneEffects::attributes);
        bus.addListener(RuneEffects::onHit);
        bus.addListener(RuneEffects::onDraw);
        bus.addListener(RuneEffects::onArrow);
        bus.addListener(RuneEffects::onTick);
        bus.addListener(RuneEffects::onRightClick);
        bus.addListener(RuneEffects::onKill);
        bus.addListener(RuneEffects::onLogout);
    }

    /** ce que chaque joueur traine d'un tick a l'autre : pouvoirs armes, ent, dernier tue */
    private static final java.util.Map<java.util.UUID, State> STATES = new java.util.HashMap<>();

    private static final class State {
        long udunUntil, elrondUntil, cooldown;       // en ticks de monde
        long entDead;                                // quand le Sylvegarde est mort
        java.util.UUID ent;
        java.util.UUID ally;                         // le dernier ennemi releve par le Pacte noir
        net.minecraft.core.BlockPos light;           // ou se trouve le bloc de lumiere pose
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lightDim;
    }

    private static State state(Player p) { return STATES.computeIfAbsent(p.getUUID(), u -> new State()); }

    static final net.minecraft.world.entity.EquipmentSlot[] ARMOR = {
            net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};

    // ------------------------------------------------------------------ arcs

    /**
     * Sur un arc, la rune de vitesse raccourcit le bandage : la charge compte comme si
     * on avait tire un peu plus longtemps.
     */
    private static void onDraw(net.neoforged.neoforge.event.entity.player.ArrowLooseEvent e) {
        ItemStack bow = e.getBow();
        if (!Runes.runic(bow) || Rune.on(bow).isEmpty()) return;
        float speed = Rune.totals(bow).speed();
        if (speed != 0) e.setCharge(Math.max(0, Math.round(e.getCharge() * (1F + speed / 100F))));
    }

    /**
     * La fleche part plus fort avec une rune de portee (la portee varie comme le carre
     * de la vitesse, d'ou la racine), et s'enflamme avec une rune de feu.
     */
    private static void onArrow(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide) return;
        // un rallie qui reparait dans le monde : ses buts ne survivent pas aux sauvegardes, on les repose
        if (e.getEntity() instanceof net.minecraft.world.entity.Mob mob && net.ringsofash.entity.Ally.marked(mob)) {
            net.ringsofash.entity.Ally.apply(mob);
            return;
        }
        if (!(e.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof LivingEntity owner)) return;
        ItemStack bow = owner.getMainHandItem();
        if (!Runes.runic(bow) || Rune.grip(bow) != Rune.Grip.BOW) return;
        List<Rune> runes = Rune.on(bow);
        if (runes.isEmpty()) return;

        float reach = Rune.totals(bow).reach();
        if (reach != 0) {
            // +2 blocs par point d'allonge sur une portee a plat d'environ 55 blocs
            double k = Math.sqrt(Math.max(0.1, 1.0 + 2.0 * reach / 55.0));
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(k));
        }
        if (runes.contains(Rune.FIRE)) arrow.igniteForSeconds(100);
    }

    // ------------------------------------------------------------------ chiffres

    private static void attributes(ItemAttributeModifierEvent e) {
        ItemStack s = e.getItemStack();
        if (!Runes.runic(s)) return;
        List<Rune> runes = Rune.on(s);
        if (runes.isEmpty()) return;
        if (Rune.gear(s).armor()) { armorAttributes(e, s, runes); return; }
        Rune.Totals t = Rune.totals(s);

        if (t.dmg() != 0) e.addModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(RingsOfAsh.id("rune_damage"), t.dmg() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.MAINHAND);
        if (t.speed() != 0) e.addModifier(Attributes.ATTACK_SPEED,
                new AttributeModifier(RingsOfAsh.id("rune_speed"), t.speed() / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.MAINHAND);
        if (t.reach() != 0) {
            e.addModifier(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(RingsOfAsh.id("rune_reach"), t.reach(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            e.addModifier(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(RingsOfAsh.id("rune_reach_block"), t.reach(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        // combo Mur : les coups recus ne font plus reculer
        if (Rune.combos(s).contains(Rune.Combo.MUR)) e.addModifier(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(RingsOfAsh.id("rune_wall"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    /** une piece d'armure : protection, vitesse de marche, nage, souffle, creusement */
    private static void armorAttributes(ItemAttributeModifierEvent e, ItemStack s, List<Rune> runes) {
        EquipmentSlotGroup group = switch (Rune.gear(s)) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case SHIELD -> EquipmentSlotGroup.HAND;
            default -> EquipmentSlotGroup.FEET;
        };
        float armor = 0, move = 0, swim = 0, oxy = 0, dig = 0;
        for (Rune r : runes) {
            float a = r.affinity(s);
            armor += r.armorBonus() * a;
            move += r.moveBonus() > 0 ? r.moveBonus() * a : r.moveBonus();   // le poids reste entier
            swim += r.swim() * a;
            oxy += r.oxygen();
            dig += r.dig() * a;
        }
        // chaque piece pose ses modificateurs sous un identifiant qui lui est propre
        String tag = group.name().toLowerCase();
        if (armor != 0) e.addModifier(Attributes.ARMOR,
                new AttributeModifier(RingsOfAsh.id("rune_armor_" + tag), armor, AttributeModifier.Operation.ADD_VALUE), group);
        if (move != 0) e.addModifier(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(RingsOfAsh.id("rune_move_" + tag), move / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        if (swim != 0) e.addModifier(Attributes.WATER_MOVEMENT_EFFICIENCY,
                new AttributeModifier(RingsOfAsh.id("rune_swim_" + tag), Math.min(1.0F, swim), AttributeModifier.Operation.ADD_VALUE), group);
        if (oxy != 0) e.addModifier(Attributes.OXYGEN_BONUS,
                new AttributeModifier(RingsOfAsh.id("rune_oxygen_" + tag), oxy, AttributeModifier.Operation.ADD_VALUE), group);
        if (dig != 0) e.addModifier(Attributes.BLOCK_BREAK_SPEED,
                new AttributeModifier(RingsOfAsh.id("rune_dig_" + tag), dig / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
    }

    // ------------------------------------------------------------------ a l'impact

    private static void onHit(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof LivingEntity attacker)) return;
        thorns(e.getEntity(), attacker);

        // un coup de main, ou une fleche de l'arc encore tenu : la magie et le reste ne comptent pas
        boolean melee = src.getDirectEntity() == attacker;
        boolean shot = src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow a && a.getOwner() == attacker;
        if (!melee && !shot) return;
        ItemStack w = attacker.getMainHandItem();
        if (!Runes.runic(w)) return;
        if (shot && Rune.grip(w) != Rune.Grip.BOW) return;
        List<Rune> runes = Rune.on(w);
        if (runes.isEmpty()) return;

        LivingEntity target = e.getEntity();
        List<Rune.Combo> combos = Rune.combos(w);
        var rnd = attacker.getRandom();

        if (runes.contains(Rune.FIRE) && melee) {               // la fleche brule deja d'elle-meme
            target.igniteForSeconds(combos.contains(Rune.Combo.FOURNAISE) ? 8 : 4);
            // combo Flamme d'Udun : tout ce qui entoure la cible prend feu aussi
            if (combos.contains(Rune.Combo.UDUN)) {
                for (LivingEntity near : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0), x -> x != attacker))
                    UdunFire.ignite(near, 100);
            }
        }
        if (runes.contains(Rune.POISON) && rnd.nextFloat() < 0.5F)
            target.addEffect(new MobEffectInstance(MobEffects.POISON, combos.contains(Rune.Combo.VENIN) ? 80 : 40, 0), attacker);
        if (runes.contains(Rune.WIND) && rnd.nextFloat() < (combos.contains(Rune.Combo.BOURRASQUE) ? 0.15F : 0.05F)) {
            target.setDeltaMovement(target.getDeltaMovement().x, 1.05, target.getDeltaMovement().z);
            target.hurtMarked = true;
        }
        if (runes.contains(Rune.ICE) && melee) {
            target.setTicksFrozen(target.getTicksRequiredToFreeze() + 120);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), attacker);
        }
        if (runes.contains(Rune.BLACK) && attacker instanceof Player p && !p.level().isClientSide) {
            var lore = net.ringsofash.data.ModData.lore(p);
            // le Pacte noir fait monter la malediction deux fois plus vite
            int c = Rune.BLACK.corrupt * (combos.contains(Rune.Combo.PACTE) ? 2 : 1);
            lore.corruption = Math.min(1000, lore.corruption + c);
        }
        // rune elfique : la tenue decide
        if (runes.contains(Rune.ELVEN) && attacker instanceof Player p2)
            e.setAmount(e.getAmount() * (fullElven(p2) ? 1.20F : 0.80F));

        // Flamme d'Udun arme par un clic droit : ce coup-ci embrase une zone de 3 sur 4
        if (attacker instanceof Player p3 && combos.contains(Rune.Combo.UDUN)) {
            State st = state(p3);
            if (st.udunUntil > p3.level().getGameTime()) {
                st.udunUntil = 0;
                blaze(target);
            }
        }
    }

    /** l'armure du frappe renvoie une part du coup, et s'use pour l'avoir fait */
    private static void thorns(LivingEntity victim, LivingEntity attacker) {
        if (victim.level().isClientSide || victim == attacker) return;
        float back = 0;
        for (net.minecraft.world.entity.EquipmentSlot sl : ARMOR) {
            ItemStack piece = victim.getItemBySlot(sl);
            if (!Runes.runic(piece)) continue;
            for (Rune r : Rune.on(piece)) back += r.thorns() * r.affinity(piece);
        }
        if (back <= 0) return;
        attacker.hurt(victim.damageSources().thorns(victim), back);
    }

    /** les quatre pieces d'armure sont-elles elfiques ? */
    private static boolean fullElven(Player p) {
        for (net.minecraft.world.entity.EquipmentSlot sl : ARMOR) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getItemBySlot(sl).getItem());
            if (id == null) return false;
            String path = id.getPath();
            if (!path.startsWith("elven") && !path.startsWith("elfking")) return false;
        }
        return true;
    }

    /** le rouge sombre de la Flamme d'Udun : une braise qui s'eteint vers le sang */
    private static final net.minecraft.core.particles.DustColorTransitionOptions UDUN_RED =
            new net.minecraft.core.particles.DustColorTransitionOptions(
                    new org.joml.Vector3f(0.98F, 0.13F, 0.04F),
                    new org.joml.Vector3f(0.38F, 0.01F, 0.00F), 1.7F);

    /** une nappe de feu de 3 blocs sur 4 autour de la cible */
    private static void blaze(LivingEntity target) {
        var level = target.level();
        if (level.isClientSide) return;
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            // le rouge par-dessus l'orange du feu : c'est lui qu'on voit
            sl.sendParticles(UDUN_RED, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    90, 1.4, 1.1, 1.4, 0.0);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                    target.getX(), target.getY() + 0.2, target.getZ(), 12, 0.9, 0.2, 0.9, 0.0);
        }
        var base = target.blockPosition();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -2; dz <= 1; dz++) {
            var pos = base.offset(dx, 0, dz);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below()))
                level.setBlock(pos, net.ringsofash.gen.Reg.UDUN_FIRE.get().defaultBlockState(), 3);
        }
        UdunFire.ignite(target, 160);
    }

    // ------------------------------------------------------------------ clic droit : les pouvoirs

    private static void onRightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        ItemStack w = e.getItemStack();
        if (!Runes.runic(w)) return;
        List<Rune.Combo> combos = Rune.combos(w);
        State st = state(p);
        long now = p.level().getGameTime();
        if (st.cooldown > now) return;

        if (combos.contains(Rune.Combo.UDUN)) {
            st.udunUntil = now + 200;                 // dix secondes pour placer le coup
            st.cooldown = now + 400;
            say(p, "La Flamme d'Udûn s'éveille : ton prochain coup embrasera la terre.", net.minecraft.ChatFormatting.DARK_RED);
            e.setCanceled(true);
        } else if (combos.contains(Rune.Combo.ELROND)) {
            st.elrondUntil = now + 200;               // dix secondes
            st.cooldown = now + 600;
            say(p, "Le pouvoir d'Elrond te traverse.", net.minecraft.ChatFormatting.AQUA);
            e.setCanceled(true);
        }
    }

    private static void say(Player p, String text, net.minecraft.ChatFormatting c) {
        p.displayClientMessage(net.minecraft.network.chat.Component.literal(text).withStyle(c), true);
    }

    // ------------------------------------------------------------------ chaque tick : les runes qui veillent

    private static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) return;
        ItemStack w = p.getMainHandItem();
        State st = state(p);
        long now = p.level().getGameTime();

        // vitesse quadruplee pendant le pouvoir d'Elrond
        var speed = p.getAttribute(Attributes.ATTACK_SPEED);
        if (speed != null) {
            var id = RingsOfAsh.id("rune_elrond");
            boolean on = st.elrondUntil > now;
            if (on && speed.getModifier(id) == null)
                speed.addTransientModifier(new AttributeModifier(id, 3.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!on && speed.getModifier(id) != null) speed.removeModifier(id);
        }

        if (p.tickCount % 10 == 0) allies(p);        // les rallies se battent meme l'arme rangee
        armorTick(p, st);                            // l'armure agit sans qu'on tienne quoi que ce soit

        if (!Runes.runic(w)) { hobbit(p, false); return; }
        List<Rune> runes = Rune.on(w);
        List<Rune.Combo> combos = Rune.combos(w);

        // rune de la foret : force tant qu'on touche du bois ou des feuilles
        if (runes.contains(Rune.FOREST) && p.tickCount % 20 == 0 && nearTree(p))
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false));

        // rune des Hobbits : pieds nus, un artefact au cou
        hobbit(p, runes.contains(Rune.HOBBIT) && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty()
                && Artifacts.anyEquipped(p));

        if (p.tickCount % 20 == 0) {
            if (combos.contains(Rune.Combo.RADAGAST)) radagast(p);
            if (combos.contains(Rune.Combo.ENTS)) ents(p, st, now);
        }
        if (p.tickCount % 4 == 0 && !combos.isEmpty()) aura(p, combos, st.udunUntil > now, st.elrondUntil > now);
    }

    /**
     * Les runes d'armure qui ne passent pas par un attribut : le saut, la lumiere et la
     * vision nocturne du combo Lumiere eternelle.
     */
    private static void armorTick(net.minecraft.server.level.ServerPlayer p, State st) {
        int jump = 0;
        boolean light = false, eternal = false;
        for (net.minecraft.world.entity.EquipmentSlot sl : ARMOR) {
            ItemStack piece = p.getItemBySlot(sl);
            if (!Runes.runic(piece)) continue;
            for (Rune r : Rune.on(piece)) {
                jump = Math.max(jump, r.jumpLevel());
                if (r == Rune.LIGHT) light = true;
            }
            if (Rune.combo(piece) == Rune.Combo.ETERNELLE) eternal = true;
        }
        if (jump > 0 && p.tickCount % 20 == 0)
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, jump - 1, true, false, false));
        if (eternal && p.tickCount % 100 == 0)
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        if (p.tickCount % 5 == 0) glow(p, st, light);
    }

    /**
     * La rune lumineuse pose un bloc de lumiere invisible a hauteur de tete et le retire
     * des qu'on bouge. On garde ou il est pour ne jamais en laisser trainer.
     */
    private static void glow(net.minecraft.server.level.ServerPlayer p, State st, boolean on) {
        net.minecraft.core.BlockPos want = on ? p.blockPosition().above() : null;
        if (st.light != null && !st.light.equals(want)) {
            net.minecraft.server.level.ServerLevel old = st.lightDim == null ? null : p.server.getLevel(st.lightDim);
            if (old != null && old.getBlockState(st.light).is(net.minecraft.world.level.block.Blocks.LIGHT))
                old.setBlock(st.light, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            st.light = null;
            st.lightDim = null;
        }
        if (want == null || st.light != null) return;
        if (!p.serverLevel().getBlockState(want).isAir()) return;
        p.serverLevel().setBlock(want, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15), 3);
        st.light = want.immutable();
        st.lightDim = p.serverLevel().dimension();
    }

    /** on eteint la lumiere derriere le joueur qui s'en va */
    private static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) glow(p, state(p), false);
    }

    /** debout sur un arbre, ou les mains dedans */
    private static boolean nearTree(Player p) {
        var below = p.level().getBlockState(p.blockPosition().below());
        var inside = p.level().getBlockState(p.blockPosition().above());
        return below.is(net.minecraft.tags.BlockTags.LOGS) || below.is(net.minecraft.tags.BlockTags.LEAVES)
                || inside.is(net.minecraft.tags.BlockTags.LOGS) || inside.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    /** le mithril ne s'use plus et protege deux fois plus */
    private static void hobbit(net.minecraft.server.level.ServerPlayer p, boolean on) {
        var armor = p.getAttribute(Attributes.ARMOR);
        var id = RingsOfAsh.id("rune_hobbit");
        if (armor != null) {
            if (on && armor.getModifier(id) == null)
                armor.addTransientModifier(new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!on && armor.getModifier(id) != null) armor.removeModifier(id);
        }
        if (!on || p.tickCount % 20 != 0) return;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(slot);
            var id2 = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
            if (id2 != null && id2.getPath().startsWith("mithril") && s.isDamaged()) s.setDamageValue(0);
        }
    }

    /**
     * Radagast : les betes du coin prennent ton parti. Une vache n'a pas d'attaque a elle,
     * alors on les lance sur l'ennemi et on compte le coup nous-memes quand elles le touchent.
     */
    private static void radagast(net.minecraft.server.level.ServerPlayer p) {
        LivingEntity foe = p.getLastHurtMob();
        if (foe == null || !foe.isAlive() || foe.distanceToSqr(p) > 400) return;
        for (net.minecraft.world.entity.Mob m : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                p.getBoundingBox().inflate(10.0), x -> x != foe && x.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE)) {
            m.getNavigation().moveTo(foe, 1.3);
            if (m.distanceToSqr(foe) < 4.0) foe.hurt(p.damageSources().mobAttack(m), 2.0F);
        }
    }

    /** Appel des Ents : sous cinq coeurs, un Sylvegarde arrive ; dix minutes de deuil s'il tombe */
    private static void ents(net.minecraft.server.level.ServerPlayer p, State st, long now) {
        if (p.getHealth() > 10.0F) return;
        if (st.ent != null) {
            var e = ((net.minecraft.server.level.ServerLevel) p.level()).getEntity(st.ent);
            if (e != null && e.isAlive()) return;
            if (st.entDead == 0) st.entDead = now;
            st.ent = null;
        }
        if (st.entDead != 0 && now - st.entDead < 12000) return;      // dix minutes
        var tree = net.ringsofash.entity.ModEntities.TREEMAN.get().create(p.level());
        if (tree == null) return;
        tree.moveTo(p.getX() + (p.getRandom().nextDouble() - 0.5) * 4, p.getY(), p.getZ() + (p.getRandom().nextDouble() - 0.5) * 4, p.getYRot(), 0);
        tree.bind(p);
        net.ringsofash.entity.Ally.markKeep(tree, p);        // il te suit, mais garde ses propres cibles
        p.level().addFreshEntity(tree);
        st.ent = tree.getUUID();
        st.entDead = 0;
        say(p, "La forêt répond à ton appel.", net.minecraft.ChatFormatting.DARK_GREEN);
    }

    // ------------------------------------------------------------------ Pacte noir : le dernier tue se releve

    private static void onKill(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof Player p) || p.level().isClientSide) return;
        ItemStack w = p.getMainHandItem();
        if (!Runes.runic(w) || !Rune.combos(w).contains(Rune.Combo.PACTE)) return;
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player) return;
        State st = state(p);
        var copy = dead.getType().create(p.level());
        if (!(copy instanceof net.minecraft.world.entity.Mob m)) return;
        // un seul releve a la fois : le precedent retourne a la poussiere
        if (st.ally != null && p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            var old = sl.getEntity(st.ally);
            if (old != null) old.discard();
        }
        m.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), 0);
        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 6000, 0, true, false));
        net.ringsofash.entity.Ally.mark(m, p);
        p.level().addFreshEntity(m);                  // l'arrivee dans le monde pose ses buts
        st.ally = m.getUUID();
        say(p, "Il se relève et marche à tes côtés.", net.minecraft.ChatFormatting.DARK_PURPLE);
    }

    /**
     * Les rallies suivent tout seuls (voir Ally), mais c'est ici qu'on leur dit qui frapper :
     * ce que tu frappes, ce qui te frappe, ou a defaut le monstre le plus proche de toi.
     */
    private static void allies(net.minecraft.server.level.ServerPlayer p) {
        List<net.minecraft.world.entity.Mob> mine = p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                p.getBoundingBox().inflate(48.0), m -> net.ringsofash.entity.Ally.marked(m)
                        && p.getUUID().equals(net.ringsofash.entity.Ally.ownerId(m)));
        if (mine.isEmpty()) return;

        LivingEntity foe = p.getLastHurtMob();
        if (foe == null || !foe.isAlive() || foe.distanceToSqr(p) > 1024) foe = p.getLastHurtByMob();
        if (foe == null || !foe.isAlive() || foe.distanceToSqr(p) > 1024) foe = null;

        for (net.minecraft.world.entity.Mob m : mine) {
            LivingEntity t = foe;
            if (t == null) {
                // personne de designe : il prend le monstre le plus proche de toi
                double best = 256;
                for (LivingEntity near : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16.0),
                        x -> x instanceof net.minecraft.world.entity.monster.Enemy && x.isAlive()
                                && !(x instanceof net.minecraft.world.entity.Mob mm && net.ringsofash.entity.Ally.marked(mm)))) {
                    double d = near.distanceToSqr(p);
                    if (d < best) { best = d; t = near; }
                }
            }
            if (t == null || t == m) { if (m.getTarget() == p) m.setTarget(null); continue; }
            if (m.getTarget() != t) m.setTarget(t);
            m.setLastHurtByMob(null);
        }
    }

    // ------------------------------------------------------------------ l'arme change d'allure

    /** chaque pouvoir laisse sa trace autour de la main */
    private static void aura(net.minecraft.server.level.ServerPlayer p, List<Rune.Combo> combos, boolean udun, boolean elrond) {
        net.minecraft.server.level.ServerLevel sl = p.serverLevel();
        net.minecraft.world.phys.Vec3 look = p.getLookAngle();
        net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(-look.z, 0, look.x).normalize().scale(0.45);
        double hx = p.getX() + side.x + look.x * 0.4, hy = p.getY() + 1.1, hz = p.getZ() + side.z + look.z * 0.4;

        if (udun || combos.contains(Rune.Combo.UDUN)) {
            int n = udun ? 12 : 4;
            sl.sendParticles(UDUN_RED, hx, hy, hz, n, 0.09, 0.15, 0.09, 0.0);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, hx, hy, hz, udun ? 3 : 1, 0.06, 0.12, 0.06, 0.003);
            if (udun) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, hx, hy + 0.2, hz, 3, 0.08, 0.1, 0.08, 0.004);
        }
        if (elrond || combos.contains(Rune.Combo.ELROND))
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, hx, hy, hz, elrond ? 6 : 2, 0.06, 0.12, 0.06, 0.0);
        if (combos.contains(Rune.Combo.ENTS))
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()), hx, hy, hz, 2, 0.08, 0.05, 0.08, 0.0);
        if (combos.contains(Rune.Combo.RADAGAST))
            sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                    new ItemStack(net.minecraft.world.item.Items.FEATHER)), hx, hy, hz, 2, 0.1, 0.08, 0.1, 0.01);
        if (combos.contains(Rune.Combo.PACTE)) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, hx, hy, hz, 2, 0.05, 0.08, 0.05, 0.0);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_OBSIDIAN_TEAR, hx, hy + 0.1, hz, 1, 0.06, 0.04, 0.06, 0.0);
        }
    }
}
