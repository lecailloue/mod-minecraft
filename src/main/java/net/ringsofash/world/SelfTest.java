package net.ringsofash.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.gen.Reg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Tests automatiques lances sur le serveur de developpement (-Droa.selftest). */
public final class SelfTest {
    private static final File OUT = new File("selftest");
    private static int fails = 0;

    private SelfTest() {}

    private static void log(String s) { RingsOfAsh.LOGGER.info("[SELFTEST] " + s); }
    private static void fail(String s) { fails++; RingsOfAsh.LOGGER.error("[SELFTEST] ECHEC: " + s); }

    public static final Map<String, Integer> BIOME_COLORS = new LinkedHashMap<>();
    static {
        BIOME_COLORS.put("plaines_vertes", 0xBEEB9E); BIOME_COLORS.put("plaines_jaunes", 0xFFEB9E); BIOME_COLORS.put("plaines_cendre", 0x96938E);
        BIOME_COLORS.put("grande_foret", 0x1F6A2A); BIOME_COLORS.put("monts_rocheux", 0x808080); BIOME_COLORS.put("cimes_neigeuses", 0xFFFFFF);
        BIOME_COLORS.put("mer_grise", 0x2040A0); BIOME_COLORS.put("rivage", 0xF0E0A0); BIOME_COLORS.put("riviere", 0x40A0F0); BIOME_COLORS.put("monts_de_cendre", 0x604038); BIOME_COLORS.put("foret_clairsemee", 0x5FA040); BIOME_COLORS.put("bosquets", 0x8FD070); BIOME_COLORS.put("rivieres_de_lave", 0xFF6A14);
    }

    public static void run(MinecraftServer server) {
        OUT.mkdirs();
        try {
            ServerLevel ow = server.overworld();
            ServerLevel lands = server.getLevel(ModWorldgen.LANDS);
            if (lands == null) fail("dimension hautemarche absente");
            step("volcan", () -> volcano(ow, ModWorldgen.VOLCANO_STRUCTURE, new BlockPos(640, 0, 640), "volcan"));
            step("rarete", () -> rarity(ow));
            if (lands != null) {
                step("carte biomes", () -> biomeMap(lands));
                step("biomes", () -> eachBiome(lands));
                step("arrivee", () -> log("arrivee: " + Travel.arrival(server, lands)));
                step("carte", () -> mapCheck(lands));
            }
            Extra.run(server, OUT);
        } catch (Throwable t) {
            fails++;
            RingsOfAsh.LOGGER.error("[SELFTEST] EXCEPTION", t);
        }
        RingsOfAsh.LOGGER.info("[SELFTEST] RESULTAT: " + (fails == 0 ? "OK" : "ECHECS=" + fails));
        server.halt(false);
    }

    public interface Step { void run() throws Exception; }

    public static void step(String name, Step s) {
        long t = System.currentTimeMillis();
        try {
            s.run();
            log("etape '" + name + "' terminee en " + (System.currentTimeMillis() - t) + " ms");
        } catch (Throwable e) {
            fails++;
            RingsOfAsh.LOGGER.error("[SELFTEST] ECHEC etape '" + name + "'", e);
        }
    }

    public static StructureStart place(ServerLevel level, ResourceKey<Structure> key, BlockPos pos) {
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(key);
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        StructureStart start = s.generate(level.registryAccess(), gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureManager(), level.getSeed(), new ChunkPos(pos), 0, level, h -> true);
        if (!start.isValid()) return null;
        BoundingBox bb = start.getBoundingBox();
        ChunkPos c1 = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos c2 = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        ChunkPos.rangeClosed(c1, c2).forEach(cp -> level.getChunk(cp.x, cp.z));
        ChunkPos.rangeClosed(c1, c2).forEach(cp -> start.placeInChunk(level, level.structureManager(), gen, level.getRandom(),
                new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(), cp.getMaxBlockX(), level.getMaxBuildHeight(), cp.getMaxBlockZ()), cp));
        return start;
    }

    private static void volcano(ServerLevel level, ResourceKey<Structure> key, BlockPos pos, String name) throws Exception {
        StructureStart start = place(level, key, pos);
        if (start == null) { fail("structure " + key.location() + " invalide"); return; }
        BoundingBox bb = start.getBoundingBox();
        VolcanoPiece piece = (VolcanoPiece) start.getPieces().get(0);
        log(name + " bbox=" + bb + " centre=" + piece.center() + " lac=" + piece.lakeCenter());
        topDown(level, bb.minX(), bb.minZ(), bb.maxX(), bb.maxZ(), new File(OUT, name + "_dessus.png"), 3);
        section(level, bb.minX(), bb.maxX(), piece.center().getZ(), piece.center().getY() - 20, piece.rimY() + 10, new File(OUT, name + "_coupe.png"), 3);
        int n = Travel.awaken(level, piece.lakeCenter());
        log(name + " lave pale: " + n + " blocs");
        if (n < 50) fail("la lave du cratere ne s'est pas eveillee (" + n + ")");
        if (!level.getBlockState(piece.lakeCenter()).is(Reg.PALE_LAVA.get())) fail("pas de lave pale au centre");
        topDown(level, piece.center().getX() - 24, piece.center().getZ() - 24, piece.center().getX() + 24, piece.center().getZ() + 24, new File(OUT, name + "_cratere.png"), 6);
    }

    private static void rarity(ServerLevel ow) {
        int volc = 0, mush = 0, total = 0;
        BlockPos firstVolc = null;
        for (int x = -24000; x <= 24000; x += 96) {
            for (int z = -24000; z <= 24000; z += 96) {
                Holder<Biome> b = ow.getUncachedNoiseBiome(x >> 2, 16, z >> 2);
                total++;
                if (b.is(ModWorldgen.VOLCANO_BIOME)) { volc++; if (firstVolc == null) firstVolc = new BlockPos(x, 64, z); }
                else if (b.is(Biomes.MUSHROOM_FIELDS)) mush++;
            }
        }
        log("rarete monde normal: volcan=" + volc + " champignon=" + mush + " sur " + total + " points ; premier volcan " + firstVolc);
        if (volc == 0) { fail("aucun biome volcan dans 48000x48000"); return; }
        BlockPos s = ow.findNearestMapStructure(ModWorldgen.VOLCANOES, firstVolc, 60, false);
        log("volcan naturel le plus proche: " + s);
        if (s == null) fail("aucun volcan naturel pres du biome volcan");
    }

    /** Verifie que le terrain genere suit la carte predefinie, et fait quelques vues du dessus. */
    private static void mapCheck(ServerLevel lands) throws Exception {
        Map<String, int[]> spots = new LinkedHashMap<>(MapData.places());
        int[] fo = find(MapData.B_FORET, false), cs = find(MapData.B_CIMES, true), hi = find(-1, true);
        if (fo != null) spots.put("foret", fo);
        spots.put("cote_ouest", new int[]{-HALF_COAST, 0});
        int bad = 0, n = 0;
        for (Map.Entry<String, int[]> e : spots.entrySet()) {
            int px = e.getValue()[0], pz = e.getValue()[1];
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) lands.getChunk((px >> 4) + dx, (pz >> 4) + dz);
            for (int x = px - 24; x <= px + 24; x += 6) {
                for (int z = pz - 24; z <= pz + 24; z += 6) {
                    if (e.getKey().equals("barad_dur") || e.getKey().equals("edoras")) continue;                // site nivele et creuse (douve) expres
                    n++;
                    BlockPos at = new BlockPos(x, MapData.height(x, z), z);
                    var st = lands.getBlockState(at);
                    if (st.isAir() || !st.getFluidState().isEmpty()) { bad++; if (bad < 6) log("ecart de relief en " + at + " : " + st); }
                }
            }
            topDown(lands, px - 40, pz - 40, px + 39, pz + 39, new File(OUT, "carte_" + e.getKey() + ".png"), 4);
            log("carte " + e.getKey() + " (" + px + "," + pz + ") relief " + MapData.height(px, pz) + " biome " + MapData.BIOMES[MapData.biome(px, pz)]);
        }
        int[] mm = spots.get("minas_morgul");
        if (mm != null) {                                                    // Minas Morgul entier : vue de dessus et profil du sol le long de l'axe de la porte
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) lands.getChunk((mm[0] >> 4) + dx, (mm[1] >> 4) + dz);
            topDown(lands, mm[0] - 80, mm[1] - 80, mm[0] + 79, mm[1] + 79, new File(OUT, "morgul_dessus.png"), 5);
            StringBuilder sb = new StringBuilder();
            for (int x = mm[0] - 80; x <= mm[0] + 80; x += 4) sb.append(lands.getHeight(Heightmap.Types.OCEAN_FLOOR, x, mm[1] - 40)).append(' ');
            int ich = 0, cols = 0;
            for (int x = mm[0] - 80; x <= mm[0] + 80; x++) {
                int z = mm[1] - 40, y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                cols++;
                if (lands.getBlockState(new BlockPos(x, y, z)).is(net.ringsofash.fluid.ModFluids.ICHOR_BLOCK.get())) ich++;
            }
            var ichor = net.ringsofash.fluid.ModFluids.ICHOR_BLOCK.get().defaultBlockState();
            log("morgul ichor : " + ich + "/" + cols + " colonnes avec ichor en surface ; lave=" + ichor.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) + " lumiere=" + ichor.getLightEmission());
            if (!ichor.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) fail("l'ichor n'est pas dans le tag fluide lave (pas de degats de feu)");
            if (ich == 0) fail("aucun ichor visible dans le gouffre");
            log("morgul profil sol (z=" + (mm[1] - 40) + ", x de " + (mm[0] - 80) + " a " + (mm[0] + 80) + " pas 4) : " + sb);
        }
        int[] oc = spots.get("orthanc");
        if (oc != null) {                                                    // Orthanc : vue de dessus, coupe est-ouest, porte de la tour et plateforme du sommet
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) lands.getChunk((oc[0] >> 4) + dx, (oc[1] >> 4) + dz);
            topDown(lands, oc[0] - 100, oc[1] - 100, oc[0] + 99, oc[1] + 99, new File(OUT, "orthanc_dessus.png"), 3);
            int oy = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, oc[0] + 60, oc[1]);
            section(lands, oc[0] - 110, oc[0] + 110, oc[1], oy - 28, oy + 210, new File(OUT, "orthanc_coupe.png"), 3);
            var door = lands.getBlockState(new BlockPos(oc[0] + 12, oy + 1, oc[1]));
            var plat = lands.getBlockState(new BlockPos(oc[0] + 6, oy + 183, oc[1] + 6));
            var horn = lands.getBlockState(new BlockPos(oc[0] + 12, oy + 186, oc[1] - 12));
            log("orthanc : sol " + oy + " ; porte " + door + " ; plateforme " + plat + " ; crete est " + horn);
            if (!door.isAir()) fail("orthanc : la porte de la tour est bouchee en " + door);
            if (plat.isAir()) fail("orthanc : pas de plateforme au sommet de la tour");
            if (horn.isAir()) fail("orthanc : pas de crete au sommet de la tour");
        }
        int[] bd = spots.get("barad_dur");
        if (bd != null) {                                                    // Barad-dur : vue de dessus, coupe, porte, douve de lave, couronne
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) lands.getChunk((bd[0] >> 4) + dx, (bd[1] >> 4) + dz);
            topDown(lands, bd[0] - 100, bd[1] - 100, bd[0] + 99, bd[1] + 99, new File(OUT, "barad_dessus.png"), 3);
            int by = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, bd[0] - 65, bd[1]);
            section(lands, bd[0] - 100, bd[0] + 100, bd[1], by - 20, by + 262, new File(OUT, "barad_coupe.png"), 2);
            var gate = lands.getBlockState(new BlockPos(bd[0] - 24, by + 3, bd[1]));
            var lava = lands.getBlockState(new BlockPos(bd[0], by - 9, bd[1] - (int) (BaradPlan.outline(-Math.PI / 2) + 12)));
            var deck = lands.getBlockState(new BlockPos(bd[0] + 8, by + 223, bd[1] + 8));
            var prong = lands.getBlockState(new BlockPos(bd[0] + 11, by + 227, bd[1] + 11));
            log("barad_dur : sol " + by + " ; porte " + gate + " ; douve " + lava + " ; plateforme " + deck + " ; pointe " + prong);
            if (!gate.isAir()) fail("barad_dur : le tunnel de la porte est bouche en " + gate);
            if (!lava.is(net.minecraft.world.level.block.Blocks.LAVA)) fail("barad_dur : pas de lave dans la douve (" + lava + ")");
            if (deck.isAir()) fail("barad_dur : pas de plateforme au sommet de la tour");
            if (prong.isAir()) fail("barad_dur : pas de pointe a la couronne");
        }
        int[] ed = spots.get("edoras");
        if (ed != null) {                                                    // Edoras : colline, palais d'Or, tour de cloche
            for (int dx = -9; dx <= 9; dx++) for (int dz = -8; dz <= 8; dz++) lands.getChunk((ed[0] >> 4) + dx, (ed[1] >> 4) + dz);
            topDown(lands, ed[0] - 130, ed[1] - 110, ed[0] + 129, ed[1] + 109, new File(OUT, "edoras_dessus.png"), 3);
                        EdorasPlan ep = EdorasPlan.get();
            int by = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, ed[0] - 125, ed[1] + 60);
            section(lands, ed[0] - 130, ed[0] + 130, ed[1], by - 10, by + 90, new File(OUT, "edoras_coupe.png"), 3);
            int plat = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, ed[0] - 10, ed[1] + 10);
            int hall = lands.getHeight(Heightmap.Types.WORLD_SURFACE, ed[0] + ep.hallX - EdorasPlan.CX, ed[1] + ep.hallZ - 11 - EdorasPlan.CZ);
            int tow = lands.getHeight(Heightmap.Types.WORLD_SURFACE, ed[0] + ep.towerX - EdorasPlan.CX, ed[1] + ep.towerZ - EdorasPlan.CZ);
            log("edoras : sol " + by + " ; plateau " + plat + " ; palais " + hall + " ; tour " + tow + " ; maisons " + ep.houses);
            if (plat - by < 24) fail("edoras : la colline est trop basse (" + (plat - by) + ")");
            if (hall - plat < 16) fail("edoras : pas de palais au sommet (" + (hall - plat) + ")");
            if (tow - plat < 18) fail("edoras : pas de tour de cloche (" + (tow - plat) + ")");
            if (ep.houses < 8 || ep.lowHouses < 6) fail("edoras : trop peu de maisons (" + ep.houses + " dont " + ep.lowHouses + " en bas)");
            if (ep.wallCells < 300) fail("edoras : muraille trop courte (" + ep.wallCells + ")");
        }
        {                                                                    // rochers de cendre : on compte les blocs de roche de cendre / calcinee en surface d'une plaine de cendre
            int rx = 2165, rz = 1745, rocks = 0;
            for (int dx = -5; dx <= 4; dx++) for (int dz = -5; dz <= 4; dz++) lands.getChunk((rx >> 4) + dx, (rz >> 4) + dz);
            for (int x = rx - 64; x < rx + 64; x++) for (int z = rz - 64; z < rz + 64; z++) {
                int y = lands.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                var st = lands.getBlockState(new BlockPos(x, y, z));
                if (st.is(net.ringsofash.gen.Reg.ASH_ROCK.get()) || st.is(net.ringsofash.gen.Reg.SCORCHED_ROCK.get())) rocks++;
            }
            topDown(lands, rx - 64, rz - 64, rx + 63, rz + 63, new File(OUT, "rochers_cendre.png"), 4);
            log("rochers de cendre : " + rocks + " colonnes de roche en surface sur 128 x 128");
            if (rocks < 400 || rocks > 9000) fail("trop peu de rochers dans la plaine de cendre (" + rocks + ")");
        }
        for (int[] pk : new int[][]{cs != null ? cs : new int[]{0, 0}, hi}) {
            for (int dx = -10; dx <= 10; dx++) lands.getChunk((pk[0] >> 4) + dx, pk[1] >> 4);
            section(lands, pk[0] - 160, pk[0] + 160, pk[1], 50, 447, new File(OUT, "sommet_coupe_" + pk[0] + ".png"), 2);
            topDown(lands, pk[0] - 90, pk[1] - 90, pk[0] + 89, pk[1] + 89, new File(OUT, "sommet_dessus_" + pk[0] + ".png"), 3);
        }
        log("carte : " + bad + " ecarts sur " + n + " points");
        if (bad > n / 10) fail("le terrain ne suit pas la carte (" + bad + "/" + n + ")");
    }

    private static final int HALF_COAST = 1800;

    /** premier point (ou le plus haut) d'un biome sur la carte, pour les vues de test ; -1 = n'importe quel biome */
    private static int[] find(int biome, boolean highest) {
        int[] best = null; int bh = -999;
        for (int x = -MapData.HALF + 64; x < MapData.HALF - 64; x += 40) {
            for (int z = -MapData.HALF + 64; z < MapData.HALF - 64; z += 40) {
                int h = MapData.height(x, z);
                if (h <= MapData.SEA + 3 || (biome >= 0 && MapData.biome(x, z) != biome)) continue;
                if (!highest) return new int[]{x, z};
                if (h > bh) { bh = h; best = new int[]{x, z}; }
            }
        }
        return best;
    }

    private static void biomeMap(ServerLevel lands) throws Exception {
        int size = 384, step = 13;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Map<String, Integer> counts = new TreeMap<>();
        for (int px = 0; px < size; px++) {
            for (int pz = 0; pz < size; pz++) {
                int x = (px - size / 2) * step, z = (pz - size / 2) * step;
                Holder<Biome> b = lands.getUncachedNoiseBiome(x >> 2, 20, z >> 2);
                String id = b.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                counts.merge(id, 1, Integer::sum);
                img.setRGB(px, pz, BIOME_COLORS.getOrDefault(id, 0xFF00FF));
            }
        }
        ImageIO.write(img, "png", new File(OUT, "biomes.png"));
        StringBuilder sb = new StringBuilder("repartition des biomes (12 km x 12 km): ");
        for (Map.Entry<String, Integer> e : counts.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue() * 100 / (size * size)).append("% ");
        log(sb.toString());
    }

    private static void eachBiome(ServerLevel lands) throws Exception {
        for (String id : BIOME_COLORS.keySet()) {
            ResourceKey<Biome> key = ModWorldgen.biome(id);
            Pair<BlockPos, Holder<Biome>> b = lands.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 80, 0), 2400, 64, 64);
            if (b == null) { log("biome " + id + " introuvable dans 2400 blocs"); continue; }
            BlockPos p = b.getFirst();
            int cx = p.getX() >> 4, cz = p.getZ() >> 4;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) lands.getChunk(cx + dx, cz + dz);
            int x0 = (cx - 2) << 4, z0 = (cz - 2) << 4;
            topDown(lands, x0, z0, x0 + 79, z0 + 79, new File(OUT, "biome_" + id + ".png"), 4);
            Map<String, Integer> blocks = new TreeMap<>();
            int hMin = 999, hMax = -999;
            for (int x = x0; x < x0 + 80; x += 2) for (int z = z0; z < z0 + 80; z += 2) {
                int y = lands.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                hMin = Math.min(hMin, y); hMax = Math.max(hMax, y);
                blocks.merge(lands.getBlockState(new BlockPos(x, y, z)).getBlock().builtInRegistryHolder().key().location().getPath(), 1, Integer::sum);
            }
            log("biome " + id + " en " + p.getX() + "," + p.getZ() + " hauteur " + hMin + ".." + hMax + " surface " + blocks);
        }
    }

    public static void topDown(ServerLevel level, int x0, int z0, int x1, int z1, File f, int scale) throws Exception {
        int w = x1 - x0 + 1, h = z1 - z0 + 1;
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                int yn = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
                p.set(x, y, z);
                int col = color(level, p);
                double f2 = y > yn ? 1.18 : y < yn ? 0.78 : 1.0;
                f2 *= 0.75 + 0.25 * Math.min(1.0, Math.max(0.0, (y - 40) / 160.0)) + 0.1;
                int rgb = shade(col, f2);
                for (int a = 0; a < scale; a++) for (int b = 0; b < scale; b++) img.setRGB((x - x0) * scale + a, (z - z0) * scale + b, rgb);
            }
        }
        ImageIO.write(img, "png", f);
    }

    public static void section(ServerLevel level, int x0, int x1, int z, int y0, int y1, File f, int scale) throws Exception {
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                p.set(x, y, z);
                BlockState s = level.getBlockState(p);
                int rgb = s.isAir() ? 0xBFD8FF : color(level, p);
                for (int a = 0; a < scale; a++) for (int b = 0; b < scale; b++) img.setRGB((x - x0) * scale + a, (y1 - y) * scale + b, rgb);
            }
        }
        ImageIO.write(img, "png", f);
    }

    /** plan horizontal a une altitude donnee (vide = clair) */
    public static void slice(ServerLevel level, int x0, int z0, int x1, int z1, int y, File f, int scale) throws Exception {
        int w = x1 - x0 + 1, h = z1 - z0 + 1;
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, y, z);
                BlockState s = level.getBlockState(p);
                int rgb;
                if (s.isAir()) { p.set(x, y - 1, z); BlockState below = level.getBlockState(p); rgb = below.isAir() ? 0xE8F0FF : shade(color(level, p), 1.6) | 0x404040; }
                else rgb = shade(color(level, p), 0.55);
                for (int a = 0; a < scale; a++) for (int b = 0; b < scale; b++) img.setRGB((x - x0) * scale + a, (z - z0) * scale + b, rgb);
            }
        }
        ImageIO.write(img, "png", f);
    }

    private static int color(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.getFluidState().isEmpty()) return s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? 0xFF6A00 : 0x3F76E4;
        if (s.is(Reg.PALE_LAVA.get())) return 0xFFF2B0;
        int c = s.getMapColor(level, p).col;
        return c == 0 ? 0x101010 : c;
    }

    private static int shade(int c, double f) {
        int r = (int) Math.min(255, ((c >> 16) & 255) * f), g = (int) Math.min(255, ((c >> 8) & 255) * f), b = (int) Math.min(255, (c & 255) * f);
        return (r << 16) | (g << 8) | b;
    }
}
