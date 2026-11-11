package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.data.Guide;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.gen.Reg;

import java.io.File;

/** Tests supplementaires (creatures, objets) ajoutes au fil des versions. */
public final class Extra {
    private Extra() {}

    public static void run(MinecraftServer server, File out) throws Exception {
        SelfTest.step("creatures", () -> entities(server));
        SelfTest.step("objets", () -> {
            int n = 0;
            for (var e : Reg.ITEMS.getEntries()) { new ItemStack(e.get()).getHoverName().getString(); n++; }
            ItemStack guide = Guide.create(null);
            RingsOfAsh.LOGGER.info("[SELFTEST] " + n + " objets ; livre-guide : " + guide.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT).pages().size() + " pages");
        });
    }

    private static void entities(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = level.getSharedSpawnPos();
        int i = 0, ok = 0;
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!id.getNamespace().equals(RingsOfAsh.MODID)) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            int x = base.getX() + (i % 6) * 8, z = base.getZ() + (i / 6) * 8;
            i++;
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            try {
                Entity e = type.create(level);
                if (e == null) { SelfTest.step("creature " + id, () -> { throw new IllegalStateException("create() a renvoye null"); }); continue; }
                e.moveTo(x + 0.5, y, z + 0.5, 0, 0);
                if (e instanceof Mob m) m.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null);
                level.addFreshEntity(e);
                for (int t = 0; t < 60; t++) e.tick();
                int offers = e instanceof PeopleEntity pe ? pe.getOffers().size() : -1;
                if (e instanceof Mob m) m.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
                for (int t = 0; t < 25; t++) e.tick();
                ok++;
                RingsOfAsh.LOGGER.info("[SELFTEST] creature " + id.getPath() + " ok" + (offers >= 0 ? " (" + offers + " offres)" : ""));
            } catch (Throwable t) {
                final Throwable err = t;
                SelfTest.step("creature " + id, () -> { throw new RuntimeException(err); });
            }
        }
        RingsOfAsh.LOGGER.info("[SELFTEST] creatures testees : " + ok + "/" + i);
    }
}
