package net.ringsofash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.item.SpecialSword;
import org.joml.Vector3f;

/** Aura lumineuse animee autour des lames a effet tenues en main (particules : compatible avec les shaders). */
public final class SwordAura {
    private SwordAura() {}

    public static void init() { NeoForge.EVENT_BUS.addListener(SwordAura::tick); }

    private static int checkTick = -100;
    private static boolean orcs, monsters, undead;

    /** Detection (joueur local, une fois toutes les 10 ticks) : les lames "sentinelles" ne rayonnent que si leur cible est proche. */
    private static void scan(Player p) {
        if (Math.abs(p.tickCount - checkTick) < 10) return;
        checkTick = p.tickCount;
        var box = p.getBoundingBox().inflate(20);
        orcs = !p.level().getEntitiesOfClass(net.ringsofash.entity.FoeEntity.class, box, SpecialSword::isOrc).isEmpty();
        monsters = !p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, m -> m != p && m instanceof net.minecraft.world.entity.monster.Enemy).isEmpty();
        undead = !p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, SpecialSword::isUndead).isEmpty();
    }

    /** Couleur de l'aura si la lame rayonne en ce moment, sinon null. */
    private static float[] aura(Player p, ItemStack st, SpecialSword s) {
        boolean local = p == Minecraft.getInstance().player;
        if (local) scan(p);
        var it = st.getItem();
        if (it == net.ringsofash.gen.Reg.ELVEN_BLADE.get() || it == net.ringsofash.gen.Reg.ELVEN_GLAIVE.get())
            return local && orcs ? new float[]{0.35F, 0.80F, 1.0F} : null;
        if (s.kind == SpecialSword.Kind.SENTINEL) return local && monsters ? new float[]{1.0F, 0.60F, 0.20F} : null;
        if (s.kind == SpecialSword.Kind.SUN) return local && undead ? color(s.kind) : null;
        return color(s.kind);
    }

    private static float[] color(SpecialSword.Kind k) {
        return switch (k) {
            case ROYAL -> new float[]{0.30F, 0.65F, 1.0F};
            case FROST -> new float[]{0.70F, 0.92F, 1.0F};
            case STORM -> new float[]{1.0F, 0.92F, 0.30F};
            case SUN -> new float[]{1.0F, 0.78F, 0.28F};
            case BLOOD -> new float[]{0.80F, 0.05F, 0.10F};
            case VIPER -> new float[]{0.40F, 1.0F, 0.20F};
            default -> null;
        };
    }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (Player p : mc.level.players()) {
            ItemStack st = p.getMainHandItem();
            if (!(st.getItem() instanceof SpecialSword s)) continue;
            float[] c = aura(p, st, s);
            if (c == null) continue;
            double yaw = Math.toRadians(p.yBodyRot == 0 ? p.getYRot() : p.getYRot());
            Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            Vec3 hand = p.position().add(0, p.getBbHeight() * 0.55, 0).add(right.scale(0.42)).add(fwd.scale(0.4));
            double t = p.tickCount * 0.45;
            int n = s.kind == SpecialSword.Kind.ROYAL ? 3 : 2;
            for (int i = 0; i < n; i++) {
                double along = mc.level.random.nextDouble();                    // position le long de la lame
                double a = t + along * 7.0;
                Vec3 pos = hand.add(0, 0.15 + along * 0.85, 0).add(fwd.scale(along * 0.25))
                        .add(right.scale(Math.cos(a) * 0.16)).add(fwd.scale(Math.sin(a) * 0.16));
                ParticleOptions o = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 0.7F);
                mc.level.addParticle(o, pos.x, pos.y, pos.z, 0, 0.012, 0);
            }
            if (mc.level.random.nextInt(s.kind == SpecialSword.Kind.ROYAL ? 3 : 8) == 0) {
                double a = mc.level.random.nextDouble() * 6.28;
                ParticleOptions o = s.kind == SpecialSword.Kind.STORM ? ParticleTypes.ELECTRIC_SPARK : s.kind == SpecialSword.Kind.FROST ? ParticleTypes.SNOWFLAKE : ParticleTypes.END_ROD;
                mc.level.addParticle(o, hand.x + Math.cos(a) * 0.2, hand.y + 0.3 + mc.level.random.nextDouble() * 0.7, hand.z + Math.sin(a) * 0.2, 0, 0.02, 0);
            }
        }
    }
}
