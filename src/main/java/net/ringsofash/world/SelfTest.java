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
        BIOME_COLORS.put("vertes_collines", 0x7CD04A); BIOME_COLORS.put("plaines_du_vent", 0xD8D070); BIOME_COLORS.put("foret_doree", 0xF0B020);
        BIOME_COLORS.put("vieille_foret", 0x1F6A2A); BIOME_COLORS.put("bois_noir", 0x203020); BIOME_COLORS.put("landes_des_tertres", 0x9A9A80);
        BIOME_COLORS.put("marais_des_morts", 0x5A6A8A); BIOME_COLORS.put("monts_de_fer", 0x808080); BIOME_COLORS.put("pics_blancs", 0xFFFFFF);
        BIOME_COLORS.put("terres_de_cendre", 0xC02010); BIOME_COLORS.put("terres_sauvages", 0xC09040); BIOME_COLORS.put("mer_grise", 0x2040A0);
        BIOME_COLORS.put("rivage", 0xF0E0A0); BIOME_COLORS.put("riviere", 0x40A0F0);
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
                step("mont funeste", () -> {
                    Pair<BlockPos, Holder<Biome>> b = lands.findClosestBiome3d(h -> h.is(ModWorldgen.ASH_BIOME), new BlockPos(0, 80, 0), 6400, 64, 64);
                    if (b == null) { fail("pas de terres de cendre"); return; }
                    volcano(lands, ModWorldgen.DOOM_STRUCTURE, b.getFirst(), "mont_funeste");
                });
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

    private static void biomeMap(ServerLevel lands) throws Exception {
        int size = 384, step = 32;
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
            Pair<BlockPos, Holder<Biome>> b = lands.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 80, 0), 6400, 64, 64);
            if (b == null) { log("biome " + id + " introuvable dans 6400 blocs"); continue; }
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
