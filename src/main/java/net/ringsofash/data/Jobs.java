package net.ringsofash.data;

import net.minecraft.ChatFormatting;
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

/** Metiers : XP, niveaux, choix de competences (paliers 5, 10 et 15) et leurs effets. Seul le guerrier est code pour l'instant. */
public final class Jobs {
    private Jobs() {}

    public enum Job {
        WARRIOR("Guerrier"), MINER("Mineur"), MAGE("Magicien");
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
            {new Skill("Coup écrasant", "Toutes les 5 s, un coup énorme qui blesse tous les ennemis autour de la cible.", false),
             new Skill("Frappe rapide", "Vous attaquez bien plus vite : le malus de vitesse des deux mains presque disparaît.", true),
             new Skill("Frappe lourde", "Encore plus lent, mais dégâts de mêlée +60 % au total.", true)},
            {new Skill("Tourbillon", "Une attaque tournoyante qui blesse tous les ennemis autour de vous.", false),
             new Skill("Vitesse", "Vitesse d'attaque +15 %.", true),
             new Skill("Frappe renforcée", "Dégâts de mêlée +20 % au total.", true)},
            {new Skill("Tir rapide", "Les arcs se bandent trois fois plus vite qu'à l'ordinaire.", true),
             new Skill("Double flèche", "Vous tirez deux flèches à la fois.", false),
             new Skill("Flèche géante", "Tir à vitesse normale, mais une énorme flèche qui traverse plusieurs ennemis.", false)}};
    private static final Skill[] W15_ARCHER = {
            new Skill("Flèches de feu", "Tous vos projectiles enflamment la cible.", false),
            new Skill("Flèche téléportée", "La flèche file droit et touche instantanément l'ennemi visé, sans chute.", false),
            new Skill("Flèche de soin", "Chaque flèche qui touche vous vole un peu de vie.", false)};

    /** Competences proposees au palier t (0 = niveau 5, 1 = niveau 10, 2 = niveau 15) ; path = voie choisie au niveau 5 (1 a 3, 0 = aucune). */
    public static Skill[] skills(Job j, int t, int path) {
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
    static int bowExtra(int path, int opt10) { return path == 3 ? (opt10 == 1 ? 2 : 1) : 0; }

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
}
