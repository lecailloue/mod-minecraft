package net.ringsofash.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.menu.ForgeUpgrade;

/**
 * Metier de forgeron : XP a la trempe, a la fabrication et a l'enclume ; trois voies (niveau 5) et leurs options (niveau 10).
 * Voie 1 "Trempeur" : moins d'echecs a la forge naine ; options : lingots economes / trempe ancestrale / double trempe.
 * Voie 2 "Armurier" : +1 armure par piece trempee portee ; options : reparation a chaud / epines / cuirasse lourde.
 * Voie 3 "Maitre d'enclume" : XP rendue a l'enclume, plus de penalite de travail ; options : reparation complete / fusion gratuite / armes affutees.
 */
public final class SmithJobs {
    private SmithJobs() {}

    private static final ResourceLocation ARMOR_ID = RingsOfAsh.id("smith_armor");
    private static final String SHARP = "ringsofash_sharp";
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Integer> ANVIL_COST = new HashMap<>();

    static void init(IEventBus bus) {
        bus.addListener(SmithJobs::onCrafted);
        bus.addListener(SmithJobs::onAnvil);
        bus.addListener(SmithJobs::onDamage);
        bus.addListener(SmithJobs::onTick);
    }

    // ------------------------------------------------------------------ choix du joueur
    private static int path(Player p) { return Jobs.choice(p, Jobs.Job.SMITH, 0); }
    private static int opt(Player p) { return Jobs.choice(p, Jobs.Job.SMITH, 1); }

    // ------------------------------------------------------------------ forge naine (appele par ForgeMenu et ForgeScreen)
    public static float failChance(Player p) { return path(p) == 1 ? 0.10F : ForgeUpgrade.FAIL_CHANCE; }

    public static int cost(Player p, int base) { return path(p) == 1 && opt(p) == 1 ? Math.max(1, (int) Math.ceil(base * 0.75)) : base; }

    public static float megaChance(Player p) { return path(p) == 1 && opt(p) == 2 ? 0.05F : ForgeUpgrade.MEGA_CHANCE; }

    /** nombre de niveaux gagnes par une trempe reussie (2 avec la double trempe, une fois sur cinq) */
    public static int temperGain(Player p, RandomSource r) { return path(p) == 1 && opt(p) == 3 && r.nextFloat() < 0.20F ? 2 : 1; }

    public static void forgeXp(ServerPlayer p, int xp) { Jobs.addXp(p, Jobs.Job.SMITH, xp); }

    // ------------------------------------------------------------------ armure
    private static int temperedPieces(Player p) {
        int n = 0;
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = p.getItemBySlot(s);
            if (!st.isEmpty() && ForgeUpgrade.level(st, p.level().registryAccess()) > 0) n++;
        }
        return n;
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        int path = path(p);
        if (path == 3 && p.containerMenu instanceof AnvilMenu am) ANVIL_COST.put(p.getUUID(), am.getCost());
        if (p.tickCount % 10 != 0) return;
        AttributeInstance a = p.getAttribute(Attributes.ARMOR);
        if (a != null) {
            int n = path == 2 ? temperedPieces(p) : 0;
            AttributeModifier cur = a.getModifier(ARMOR_ID);
            if (n == 0) { if (cur != null) a.removeModifier(ARMOR_ID); }
            else if (cur == null || cur.amount() != n) a.addOrUpdateTransientModifier(new AttributeModifier(ARMOR_ID, n, AttributeModifier.Operation.ADD_VALUE));
        }
        // reparation a chaud : 1 point de durabilite toutes les 10 s sur chaque piece portee
        if (path == 2 && opt(p) == 1 && p.tickCount % 200 == 0) {
            for (EquipmentSlot s : ARMOR) {
                ItemStack st = p.getItemBySlot(s);
                if (st.isDamaged()) st.setDamageValue(st.getDamageValue() - 1);
            }
        }
    }

    // ------------------------------------------------------------------ degats : epines, cuirasse lourde, armes affutees
    private static void onDamage(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (e.getEntity() instanceof ServerPlayer p && path(p) == 2) {
            if (opt(p) == 3 && temperedPieces(p) == 4) e.setAmount(e.getAmount() * 0.85F);
            if (opt(p) == 2 && !src.is(DamageTypes.THORNS) && src.getEntity() instanceof LivingEntity at && src.getDirectEntity() == at && at != p) {
                at.hurt(p.damageSources().thorns(p), e.getAmount() * 0.15F);
            }
        }
        if (src.getEntity() instanceof ServerPlayer p && src.getDirectEntity() == p && path(p) == 3 && opt(p) == 3 && sharp(p.getMainHandItem())) {
            e.setAmount(e.getAmount() * 1.10F);
        }
    }

    private static boolean weapon(ItemStack s) { return !s.isEmpty() && !ForgeUpgrade.isArmor(s) && ForgeUpgrade.canForge(s); }

    /** affutage actif tant que l'arme n'a pas perdu 20 % de sa durabilite depuis l'enclume */
    private static boolean sharp(ItemStack s) {
        if (!weapon(s)) return false;
        CompoundTagHolder h = new CompoundTagHolder(s);
        return h.has() && s.getDamageValue() - h.at() < Math.max(20, s.getMaxDamage() / 5);
    }

    private record CompoundTagHolder(ItemStack s) {
        boolean has() { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(SHARP); }
        int at() { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(SHARP); }
    }

    // ------------------------------------------------------------------ fabrication
    private static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack st = e.getCrafting();
        if (!ForgeUpgrade.canForge(st)) return;
        Jobs.addXp(p, Jobs.Job.SMITH, Math.max(2, Math.min(30, st.getMaxDamage() / 100)));
    }

    // ------------------------------------------------------------------ enclume
    private static void onAnvil(AnvilRepairEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Jobs.addXp(p, Jobs.Job.SMITH, 1);
        if (path(p) != 3) return;
        ItemStack out = e.getOutput(), left = e.getLeft(), right = e.getRight();
        if (!p.getAbilities().instabuild) {
            int cost = p.containerMenu instanceof AnvilMenu am && am.getCost() > 0 ? am.getCost() : ANVIL_COST.getOrDefault(p.getUUID(), 0);
            boolean fusion = opt(p) == 2 && (right.is(Items.ENCHANTED_BOOK) || right.isEnchanted());
            int refund = fusion ? cost : cost / 2;
            if (refund > 0) p.giveExperienceLevels(refund);
        }
        out.remove(DataComponents.REPAIR_COST);
        if (opt(p) == 1 && out.isDamaged() && left.isValidRepairItem(right)) out.setDamageValue(0);
        if (opt(p) == 3 && weapon(out)) {
            final int dmg = out.getDamageValue();
            CustomData.update(DataComponents.CUSTOM_DATA, out, t -> t.putInt(SHARP, dmg));
        }
    }
}
