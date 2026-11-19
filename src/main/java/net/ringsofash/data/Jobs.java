package net.ringsofash.data;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ringsofash.RingsOfAsh;

/** Metiers : XP, niveaux, choix de competences (paliers 5, 10 et 15) et leurs effets. Guerrier (paliers 5/10/15) et mineur (paliers 5/10) sont codes ; le forgeron (paliers 5/10) aussi. */
public final class Jobs {
    private Jobs() {}

    public enum Job {
        WARRIOR("Guerrier"), MINER("Mineur"), SMITH("Forgeron");
        public final String fr;
        Job(String fr) { this.fr = fr; }
    }

    public static final int MAX_LEVEL = 20;
    public static final int[] TIERS = {5, 10, 15};

    /** Une competence : nom, description, et "live" = effet deja code. */
    public record Skill(String name, String desc, boolean live) {}

    // ------------------------------------------------------------------ niveaux
    /** XP cumulee necessaire pour atteindre le niveau n (niveau 1 = 0). */
    public static int xpFor(int level) { return 25 * level * (level - 1); }

    public static int level(int xp) {
        int l = 1;
        while (l < MAX_LEVEL && xp >= xpFor(l + 1)) l++;
        return l;
    }

    // ------------------------------------------------------------------ arbre (noms, descriptions)
    private static final Skill[] W5 = {
            new Skill("Deux mains", "Toutes vos armes se tiennent à deux mains : plus de bouclier ni d'objet en main secondaire. Dégâts de mêlée +35 %, attaque 20 % plus lente.", true),
            new Skill("Épée et bouclier", "Dégâts de mêlée +10 %. Vous gardez le bouclier.", true),
            new Skill("Archer", "Dégâts des lames -40 %. Les arcs se bandent deux fois plus vite.", true)};
    private static final Skill[][] W10 = {
            {new Skill("Coup écrasant", "Touche V : toutes les 5 s, un coup énorme (dégâts x3) qui blesse tous les ennemis autour de la cible.", true),
             new Skill("Frappe rapide", "Vous attaquez bien plus vite : le malus de vitesse des deux mains presque disparaît.", true),
             new Skill("Frappe lourde", "Encore plus lent, mais dégâts de mêlée +60 % au total.", true)},
            {new Skill("Tourbillon", "Touche V : une attaque tournoyante qui blesse tous les ennemis autour de vous (recharge 4 s).", true),
             new Skill("Vitesse", "Vitesse d'attaque +15 %.", true),
             new Skill("Frappe renforcée", "Dégâts de mêlée +20 % au total.", true)},
            {new Skill("Tir rapide", "Les arcs se bandent trois fois plus vite qu'à l'ordinaire.", true),
             new Skill("Double flèche", "Vous tirez deux flèches à la fois.", true),
             new Skill("Flèche géante", "Tir à vitesse normale, mais une flèche puissante (dégâts x2, critique) qui traverse jusqu'à 5 ennemis.", true)}};
    private static final Skill[] W15_ARCHER = {
            new Skill("Flèches de feu", "Tous vos projectiles enflamment la cible.", true),
            new Skill("Flèche téléportée", "La flèche file droit, sans chute, et touche instantanément l'ennemi que vous visez (portée 64 blocs).", true),
            new Skill("Flèche de soin", "Chaque flèche qui touche vous rend 1 PV (4 PV par seconde au maximum).", true)};

    private static final Skill[] M5 = {
            new Skill("Maître main", "Vous minez 40 % plus vite à la pioche.", true),
            new Skill("Grosse main", "Votre pioche creuse en 2×2 : les blocs voisins de la face visée tombent avec le premier.", true),
            new Skill("Fondeur", "Les minerais que vous cassez fondent aussitôt : vous récoltez directement des lingots.", true)};
    private static final Skill FORT = new Skill("Effet de fortune", "Les minerais donnent davantage de butin (comme Fortune III).", true);
    private static final Skill VEIN = new Skill("Veine entière", "Casser un minerai casse tout le filon connecté (jusqu'à 48 blocs).", true);
    private static final Skill[][] M10 = {
            {FORT, VEIN, new Skill("Pioche incassable", "Votre pioche ne s'use plus.", true)},
            {new Skill("Casse en 3×3", "La pioche creuse en 3×3 au lieu de 2×2.", true), VEIN, FORT},
            {new Skill("Four portable", "Four dans l'inventaire (onglet dédié) : il fond tout avec du combustible, en tâche de fond. XP et résultat vous arrivent directement.", true), FORT, VEIN}};

    private static final Skill[] S5 = {
            new Skill("Trempeur", "Maîtrise de la forge naine : l'échec de trempe passe de 20 % à 10 %.", true),
            new Skill("Armurier", "Chaque pièce d'armure trempée que vous portez donne +1 d'armure.", true),
            new Skill("Maître d'enclume", "Enclume : la moitié des niveaux d'XP vous est rendue, et les objets ne gagnent plus de pénalité de travail.", true)};
    private static final Skill[][] S10 = {
            {new Skill("Lingots économes", "Le coût de trempe en lingots de lunargent baisse de 25 %.", true),
             new Skill("Trempe ancestrale", "La chance de Trempe ancestrale passe de 1 % à 5 %.", true),
             new Skill("Double trempe", "Une trempe réussie a 20 % de chances de monter de 2 niveaux d'un coup.", true)},
            {new Skill("Réparation à chaud", "L'armure que vous portez se répare toute seule, lentement (1 point toutes les 10 s).", true),
             new Skill("Épines", "Vous renvoyez 15 % des dégâts de mêlée reçus à l'attaquant.", true),
             new Skill("Cuirasse lourde", "Avec 4 pièces d'armure trempées, vous subissez 15 % de dégâts en moins.", true)},
            {new Skill("Réparation complète", "L'enclume répare l'objet à 100 % dès qu'on lui donne un matériau de réparation.", true),
             new Skill("Fusion gratuite", "Combiner des enchantements à l'enclume ne coûte plus d'XP.", true),
             new Skill("Armes affûtées", "Une arme passée à l'enclume inflige +10 % de dégâts jusqu'à avoir perdu 20 % de sa durabilité.", true)}};

    /** Competences proposees au palier t (0 = niveau 5, 1 = niveau 10, 2 = niveau 15) ; path = voie choisie au niveau 5 (1 a 3, 0 = aucune). */
    public static Skill[] skills(Job j, int t, int path) {
        if (j == Job.MINER) {
            if (t == 0) return M5;
            return t == 1 && path >= 1 && path <= 3 ? M10[path - 1] : new Skill[0];
        }
        if (j == Job.SMITH) {
            if (t == 0) return S5;
            return t == 1 && path >= 1 && path <= 3 ? S10[path - 1] : new Skill[0];
        }
        if (j != Job.WARRIOR) return new Skill[0];
        if (t == 0) return W5;
        if (path < 1 || path > 3) return new Skill[0];
        if (t == 1) return W10[path - 1];
        return path == 3 ? W15_ARCHER : new Skill[0];
    }

    // ------------------------------------------------------------------ donnees du joueur (serveur ou copie client)
    public static int[] choices(Player p) { return p.level().isClientSide ? Net.ClientJobs.choice : ModData.lore(p).jobChoice; }
    public static int choice(Player p, Job j, int t) { return choices(p)[j.ordinal() * 3 + t]; }

    // ------------------------------------------------------------------ effets du guerrier
    static float meleeMult(int path, int opt10) {
        return switch (path) {
            case 1 -> opt10 == 3 ? 1.6F : 1.35F;
            case 2 -> opt10 == 3 ? 1.2F : 1.1F;
            case 3 -> 0.6F;
            default -> 1.0F;
        };
    }

    static double speedMod(int path, int opt10) {
        return switch (path) {
            case 1 -> opt10 == 2 ? -0.05 : opt10 == 3 ? -0.35 : -0.20;
            case 2 -> opt10 == 2 ? 0.15 : 0.0;
            default -> 0.0;
        };
    }

    /** Ticks de charge d'arc retires en plus a chaque tick (1 = 2x plus vite, 2 = 3x). */
    static int bowExtra(int path, int opt10) { return path == 3 ? (opt10 == 1 ? 2 : opt10 == 3 ? 0 : 1) : 0; }

    private static final ResourceLocation SPEED_ID = RingsOfAsh.id("job_attack_speed");

    // ------------------------------------------------------------------ XP et choix (serveur)
    public static void addXp(ServerPlayer p, Job j, int amount) {
        if (amount <= 0) return;
        PlayerLore l = ModData.lore(p);
        int before = level(l.jobXp[j.ordinal()]);
        l.jobXp[j.ordinal()] += amount;
        int after = level(l.jobXp[j.ordinal()]);
        p.displayClientMessage(Component.literal("+" + amount + " XP " + j.fr).withStyle(ChatFormatting.GOLD), true);
        if (after > before) {
            p.sendSystemMessage(Component.literal(j.fr + " : niveau " + after + " !").withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
            for (int t = 0; t < 3; t++) {
                if (before < TIERS[t] && after >= TIERS[t]) p.sendSystemMessage(Component.literal("Un choix de compétence vous attend (touche P).").withStyle(ChatFormatting.YELLOW));
            }
        }
        Net.sync(p);
    }

    /** Choix definitif d'une competence ; verifie niveau, ordre des paliers et competence disponible. */
    public static void choose(ServerPlayer p, int jobIdx, int t, int opt) {
        if (jobIdx < 0 || jobIdx >= Job.values().length || t < 0 || t > 2) return;
        Job j = Job.values()[jobIdx];
        PlayerLore l = ModData.lore(p);
        if (level(l.jobXp[jobIdx]) < TIERS[t]) return;
        if (l.jobChoice[jobIdx * 3 + t] != 0) return;
        if (t > 0 && l.jobChoice[jobIdx * 3 + t - 1] == 0) return;
        Skill[] opts = skills(j, t, l.jobChoice[jobIdx * 3]);
        if (opt < 1 || opt > opts.length || !opts[opt - 1].live()) return;
        l.jobChoice[jobIdx * 3 + t] = opt;
        p.sendSystemMessage(Component.literal("Compétence apprise : " + opts[opt - 1].name()).withStyle(ChatFormatting.GREEN));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        Net.sync(p);
    }

    // ------------------------------------------------------------------ evenements
    public static void init(IEventBus bus) {
        bus.addListener(Jobs::onDeath);
        bus.addListener(Jobs::onDamage);
        bus.addListener(Jobs::onTick);
        bus.addListener(Jobs::onShield);
        bus.addListener(Jobs::onUse);
        bus.addListener(Jobs::onArrowJoin);
        bus.addListener(Jobs::onImpact);
        MinerJobs.init(bus);
        SmithJobs.init(bus);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        LivingEntity dead = e.getEntity();
        if (!(dead instanceof Mob) || dead instanceof net.ringsofash.entity.PeopleEntity) return;
        int xp = Math.max(1, Math.min(300, Math.round(dead.getMaxHealth() / 2.0F)));
        boolean hostile = dead instanceof Enemy || dead instanceof net.ringsofash.entity.FoeEntity || dead instanceof net.ringsofash.entity.BossEntity;
        if (!hostile) xp = Math.max(1, xp / 4);
        addXp(p, Job.WARRIOR, xp);
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p) return;
        int path = choice(p, Job.WARRIOR, 0);
        if (path == 0) return;
        e.setAmount(e.getAmount() * meleeMult(path, choice(p, Job.WARRIOR, 1)));
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        int path = choice(p, Job.WARRIOR, 0);
        // deux mains : rien en main secondaire
        if (path == 1 && !p.getOffhandItem().isEmpty()) {
            ItemStack s = p.getOffhandItem().copy();
            p.getInventory().offhand.set(0, ItemStack.EMPTY);
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
        if (p.tickCount % 10 != 0) return;
        double speed = speedMod(path, choice(p, Job.WARRIOR, 1));
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_SPEED);
        if (a == null) return;
        AttributeModifier cur = a.getModifier(SPEED_ID);
        if (speed == 0.0) { if (cur != null) a.removeModifier(SPEED_ID); }
        else if (cur == null || cur.amount() != speed) a.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void onShield(LivingShieldBlockEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && choice(p, Job.WARRIOR, 0) == 1) e.setCanceled(true);
    }

    /** arcs : charge plus rapide (s'execute des deux cotes pour que l'animation suive). */
    private static void onUse(LivingEntityUseItemEvent.Tick e) {
        if (!(e.getEntity() instanceof Player p) || !(e.getItem().getItem() instanceof BowItem)) return;
        int extra = bowExtra(choice(p, Job.WARRIOR, 0), choice(p, Job.WARRIOR, 1));
        if (extra > 0) e.setDuration(Math.max(0, e.getDuration() - extra));
    }

    // ------------------------------------------------------------------ attaque speciale (touche V)
    private static final Map<UUID, Long> SPECIAL_CD = new HashMap<>();
    private static final Map<UUID, long[]> HEAL = new HashMap<>();
    private static final String CLONE = "ringsofash_clone";

    public static void special(ServerPlayer p) {
        int path = choice(p, Job.WARRIOR, 0), opt = choice(p, Job.WARRIOR, 1);
        boolean slam = path == 1 && opt == 1, whirl = path == 2 && opt == 1;
        if (!slam && !whirl) return;
        long now = p.level().getGameTime();
        Long until = SPECIAL_CD.get(p.getUUID());
        if (until != null && now < until) {
            p.displayClientMessage(Component.literal("Recharge : " + ((until - now + 19) / 20) + " s").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        SPECIAL_CD.put(p.getUUID(), now + (slam ? 100 : 80));
        ServerLevel sl = p.serverLevel();
        p.swing(InteractionHand.MAIN_HAND, true);
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 c;
        double r;
        float mult;
        if (slam) {
            Vec3 look = p.getLookAngle(), eye = p.getEyePosition(), end = eye.add(look.scale(4.5));
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, p, eye, end, new AABB(eye, end).inflate(1.0),
                    en -> en instanceof LivingEntity && en != p && !en.isSpectator());
            c = hit != null ? hit.getLocation() : new Vec3(p.getX() + look.x * 3.0, p.getY(), p.getZ() + look.z * 3.0);
            r = 3.5;
            mult = 3.0F;
        } else {
            c = p.position();
            r = 3.2;
            mult = 1.0F;
        }
        DamageSource ds = p.damageSources().playerAttack(p);
        final Vec3 centre = c;
        final double rad = r;
        for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 1.5, r), x -> x != p && x.isAlive() && x != p.getVehicle()
                && !x.isPassengerOfSameVehicle(p) && !(x instanceof AbstractVillager) && !x.isAlliedTo(p)
                && !(x instanceof Player pl && !p.canHarmPlayer(pl)) && !(x instanceof TamableAnimal ta && ta.isOwnedBy(p)))) {
            if (t.distanceToSqr(centre) > rad * rad) continue;
            t.hurt(ds, dmg * mult);
            t.knockback(slam ? 1.0 : 0.7, centre.x - t.getX(), centre.z - t.getZ());
        }
        if (slam) {
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                sl.sendParticles(ParticleTypes.CRIT, c.x + Math.cos(a) * 2.5, c.y + 0.2, c.z + Math.sin(a) * 2.5, 2, 0.1, 0.1, 0.1, 0.1);
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.6F);
        } else {
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5;
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * 2.2, c.y + 1.0, c.z + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
    }

    // ------------------------------------------------------------------ fleches de l'archer
    private static void onArrowJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof AbstractArrow arrow) || arrow.getTags().contains(CLONE)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer p) || choice(p, Job.WARRIOR, 0) != 3) return;
        int o10 = choice(p, Job.WARRIOR, 1), o15 = choice(p, Job.WARRIOR, 2);
        // fleche geante : l'arme tire une fleche ordinaire, on la remplace par la grande (meme etat, memes degats)
        if (o10 == 3 && !(arrow instanceof net.ringsofash.entity.GiantArrowEntity)) {
            net.ringsofash.entity.GiantArrowEntity big = net.ringsofash.entity.ModEntities.GIANT_ARROW.get().create(arrow.level());
            if (big != null) {
                CompoundTag tag = new CompoundTag();
                arrow.saveWithoutId(tag);
                big.load(tag);
                big.setUUID(UUID.randomUUID());
                big.setOwner(p);
                arrow.level().addFreshEntity(big);
                e.setCanceled(true);
            }
            return;
        }
        if (o15 == 1) arrow.setRemainingFireTicks(200);
        if (o10 == 3) {
            arrow.setBaseDamage(arrow.getBaseDamage() * 2.5);
            arrow.setCritArrow(true);
        }
        if (o15 == 2) {
            Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(64.0));
            BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 stop = bh.getType() == HitResult.Type.MISS ? end : bh.getLocation();
            EntityHitResult eh = ProjectileUtil.getEntityHitResult(p.level(), p, eye, stop, new AABB(eye, stop).inflate(1.0),
                    t -> t instanceof LivingEntity && t != p && !t.isSpectator() && t != p.getVehicle() && !t.isAlliedTo(p));
            arrow.setNoGravity(true);
            if (eh != null) {
                arrow.setPos(eh.getLocation().subtract(look.scale(0.8)));
                arrow.setDeltaMovement(look.scale(3.0));
            }
        }
        if (o10 == 2) {
            Entity c = arrow.getType().create(arrow.level());
            if (c instanceof AbstractArrow a2) {
                CompoundTag tag = new CompoundTag();
                arrow.saveWithoutId(tag);
                a2.load(tag);
                a2.setUUID(UUID.randomUUID());
                a2.addTag(CLONE);
                a2.setOwner(p);
                a2.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                a2.setDeltaMovement(arrow.getDeltaMovement().yRot(0.12F));
                arrow.level().addFreshEntity(a2);
            }
        }
    }

    /** fleche de soin : chaque cible touchee rend 1 PV a l'archer, 4 PV par seconde au plus */
    private static void onImpact(ProjectileImpactEvent e) {
        if (!(e.getProjectile() instanceof AbstractArrow a) || a.level().isClientSide || !(a.getOwner() instanceof ServerPlayer p)) return;
        if (!(e.getRayTraceResult() instanceof EntityHitResult eh) || !(eh.getEntity() instanceof LivingEntity t) || t == p) return;
        if (choice(p, Job.WARRIOR, 0) != 3 || choice(p, Job.WARRIOR, 2) != 3) return;
        long now = p.level().getGameTime();
        long[] w = HEAL.computeIfAbsent(p.getUUID(), k -> new long[]{now, 0});
        if (now - w[0] >= 20) { w[0] = now; w[1] = 0; }
        if (w[1] >= 4) return;
        w[1]++;
        p.heal(1.0F);
    }
}
