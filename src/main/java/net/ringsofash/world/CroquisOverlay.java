package net.ringsofash.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ringsofash.RingsOfAsh;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

/**
 * Les croquis : des morceaux de monde construits a la main par le joueur (commande /anneaux croquis),
 * releves dans data/ringsofash/map/croquis.bin et reposes ici tels quels, a leurs coordonnees d'origine.
 * Pose en dernier, apres le terrain, les villages et le decor, donc le croquis a toujours le dernier mot.
 */
public class CroquisOverlay extends Feature<NoneFeatureConfiguration> {
    public CroquisOverlay() { super(NoneFeatureConfiguration.CODEC); }

    private record Region(String nom, int x, int y, int z, int dx, int dy, int dz, BlockState[] pal, int[] cells) {
        int at(int ix, int iy, int iz) { return cells[(iy * dz + iz) * dx + ix]; }
    }

    private static List<Region> regions;

    private static synchronized void load() {
        if (regions != null) return;
        List<Region> out = new ArrayList<>();
        try (InputStream raw = CroquisOverlay.class.getResourceAsStream("/data/ringsofash/map/croquis.bin")) {
            if (raw != null) {
                ByteBuffer bb = ByteBuffer.wrap(new InflaterInputStream(raw).readAllBytes());
                bb.position(4);
                int n = bb.getInt();
                for (int i = 0; i < n; i++) {
                    byte[] nm = new byte[bb.getShort() & 0xFFFF];
                    bb.get(nm);
                    int x = bb.getInt(), y = bb.getInt(), z = bb.getInt();
                    int dx = bb.getInt(), dy = bb.getInt(), dz = bb.getInt();
                    int np = bb.getInt();
                    BlockState[] pal = new BlockState[np];
                    for (int k = 0; k < np; k++) {
                        byte[] s = new byte[bb.getShort() & 0xFFFF];
                        bb.get(s);
                        pal[k] = parse(new String(s, StandardCharsets.UTF_8));
                    }
                    int[] cells = new int[dx * dy * dz];
                    for (int k = 0; k < cells.length; k++) {
                        int v = 0, sh = 0, b;
                        do { b = bb.get() & 0xFF; v |= (b & 127) << sh; sh += 7; } while (b >= 128);
                        cells[k] = v;
                    }
                    out.add(new Region(new String(nm, StandardCharsets.UTF_8), x, y, z, dx, dy, dz, pal, cells));
                    RingsOfAsh.LOGGER.info("[CROQUIS] " + new String(nm, StandardCharsets.UTF_8) + " " + dx + "x" + dy + "x" + dz + " en " + x + "," + y + "," + z);
                }
            }
        } catch (Exception e) {
            RingsOfAsh.LOGGER.error("croquis.bin illisible", e);
        }
        regions = out;
    }

    private static <T extends Comparable<T>> BlockState set(BlockState st, Property<T> p, String v) {
        return p.getValue(v).map(x -> st.setValue(p, x)).orElse(st);
    }

    /** minecraft:oak_log[axis=x] -> l'etat correspondant */
    private static BlockState parse(String s) {
        int br = s.indexOf('[');
        String name = br < 0 ? s : s.substring(0, br);
        Block bl = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
        BlockState st = bl.defaultBlockState();
        if (br >= 0) {
            for (String kv : s.substring(br + 1, s.length() - 1).split(",")) {
                int eq = kv.indexOf('=');
                if (eq < 0) continue;
                Property<?> p = bl.getStateDefinition().getProperty(kv.substring(0, eq));
                if (p != null) st = set(st, p, kv.substring(eq + 1));
            }
        }
        return st;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        load();
        if (regions.isEmpty()) return false;
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Region r : regions) {
            int ax = Math.max(x0, r.x), bx = Math.min(x0 + 15, r.x + r.dx - 1);
            int az = Math.max(z0, r.z), bz = Math.min(z0 + 15, r.z + r.dz - 1);
            if (ax > bx || az > bz) continue;
            for (int y = 0; y < r.dy; y++) {
                int wy = r.y + y;
                if (wy < level.getMinBuildHeight() + 1 || wy >= level.getMaxBuildHeight() - 1) continue;
                for (int z = az; z <= bz; z++) for (int x = ax; x <= bx; x++) {
                    int id = r.at(x - r.x, y, z - r.z);
                    BlockState st = id == 0 ? Blocks.AIR.defaultBlockState() : r.pal[id];
                    level.setBlock(p.set(x, wy, z), st, 2);
                }
            }
            any = true;
        }
        return any;
    }
}
