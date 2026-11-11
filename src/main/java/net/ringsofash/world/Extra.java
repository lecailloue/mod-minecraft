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
        ServerLevel lands = server.getLevel(ModWorldgen.LANDS);
        if (lands != null) {
            String[][] list = {{"halfling_village", "vertes_collines"}, {"human_village", "plaines_du_vent"}, {"elf_village", "foret_doree"}, {"dwarf_mine", "monts_de_fer"},
                    {"orc_fort", "terres_de_cendre"}, {"dark_tower", "bois_noir"}, {"barrow", "landes_des_tertres"}, {"hoard", "monts_de_fer"}, {"pyre_tower", "plaines_du_vent"}, {"ruin", "vieille_foret"}};
            for (String[] e : list) SelfTest.step("structure " + e[0], () -> settlement(lands, e[0], e[1], out));
        }
        SelfTest.step("objets", () -> {
            int n = 0;
            for (var e : Reg.ITEMS.getEntries()) { new ItemStack(e.get()).getHoverName().getString(); n++; }
            ItemStack guide = Guide.create(null);
            RingsOfAsh.LOGGER.info("[SELFTEST] " + n + " objets ; livre-guide : " + guide.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT).pages().size() + " pages");
        });
    }

    private static void settlement(ServerLevel lands, String name, String biome, File out) throws Exception {
        var bkey = ModWorldgen.biome(biome);
        var found = lands.findClosestBiome3d(h -> h.is(bkey), new BlockPos(0, 80, 0), 6400, 64, 64);
        if (found == null) { RingsOfAsh.LOGGER.info("[SELFTEST] structure " + name + " : biome introuvable"); return; }
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, RingsOfAsh.id(name));
        net.minecraft.world.level.levelgen.structure.StructureStart start = null;
        BlockPos at = null;
        for (int i = 0; i < 60 && start == null; i++) {
            at = found.getFirst().offset((i % 8 - 3) * 40, 0, (i / 8 - 3) * 40);
            if (!lands.getUncachedNoiseBiome(at.getX() >> 2, 20, at.getZ() >> 2).is(bkey)) continue;
            start = SelfTest.place(lands, key, at);
        }
        if (start == null) throw new IllegalStateException("aucun emplacement valide pour " + name);
        var bb = start.getBoundingBox();
        SelfTest.topDown(lands, bb.minX() - 4, bb.minZ() - 4, bb.maxX() + 4, bb.maxZ() + 4, new File(out, "st_" + name + ".png"), bb.getXSpan() > 100 ? 3 : 5);
        java.util.Map<String, Integer> mobs = new java.util.TreeMap<>();
        for (Mob m : lands.getEntitiesOfClass(Mob.class, net.minecraft.world.phys.AABB.of(bb).inflate(2))) mobs.merge(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath(), 1, Integer::sum);
        RingsOfAsh.LOGGER.info("[SELFTEST] structure " + name + " en " + at + " : " + start.getPieces().size() + " morceaux, bbox " + bb + ", creatures " + mobs);
        for (var piece : start.getPieces()) {
            if (piece instanceof Settlement.Piece sp && (sp.type == Settlement.T.DHALL || sp.type == Settlement.T.LAIR || sp.type == Settlement.T.HOARD || sp.type == Settlement.T.BARROW)) {
                int y = sp.type == Settlement.T.HOARD ? sp.oy - 20 : sp.type == Settlement.T.BARROW ? sp.oy - 3 : sp.oy + 2;
                SelfTest.slice(lands, bb.minX() - 2, bb.minZ() - 2, bb.maxX() + 2, bb.maxZ() + 2, y, new File(out, "st_" + name + "_plan_" + sp.type.name().toLowerCase() + ".png"), 4);
            }
        }
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
