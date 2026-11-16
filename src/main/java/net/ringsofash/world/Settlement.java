package net.ringsofash.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Villages, mines, forteresses, tours, tertres... : structures procedurales faites de batiments independants. */
public final class Settlement {
    private Settlement() {}

    /** Types de batiments : largeur, profondeur, hauteur, profondeur sous le sol. */
    public enum T {
        BURROW(11, 11, 9, 4), FARM(9, 9, 4, 4), INN(13, 10, 11, 4), HOUSE(7, 9, 10, 4), HALL(15, 21, 14, 5), SMITHY(7, 7, 6, 4), STALL(5, 4, 5, 4),
        STABLE(9, 7, 6, 4), WATCHTOWER(5, 5, 15, 4), PLAZA(25, 25, 16, 3), PAVILION(9, 9, 13, 4), ETOWER(7, 7, 26, 4),
        GATE(15, 8, 13, 3), POOL(11, 9, 3, 6), STAIR(7, 48, 8, 50), DHALL(33, 27, 12, 2), MINE(21, 17, 9, 10), LAIR(27, 27, 15, 3),
        FORT(33, 33, 15, 5), DTOWER(13, 13, 46, 5), BARROW(13, 13, 7, 6), HOARD(41, 41, 7, 28), PYRE(5, 5, 13, 5), RUIN(9, 9, 9, 3), MORGUL(150, 150, 150, 50), ORTHANC(200, 200, 236, 24), BARADDUR(200, 200, 256, 20), EDORAS(260, 220, 64, 24), TIRITH(320, 270, 282, 24);
        public final int w, d, h, below;
        T(int w, int d, int h, int below) { this.w = w; this.d = d; this.h = h; this.below = below; }
    }

    public static class S extends Structure {
        public static final MapCodec<S> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i), Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)).apply(i, S::new));
        private final String kind;

        public S(StructureSettings settings, String kind) { super(settings); this.kind = kind; }

        @Override
        public StructureType<?> type() { return ModWorldgen.SETTLEMENT.get(); }

        private static int h(GenerationContext c, int x, int z) {
            return c.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
            ChunkPos cp = c.chunkPos();
            if (kind.equals("minas_morgul")) {
                // une seule instance, a l'endroit marque sur la carte (places.json) : la structure n'existe que dans le chunk de ce lieu
                int[] pl = MapData.place("minas_morgul");
                if (pl == null || cp.x != (pl[0] >> 4) || cp.z != (pl[1] >> 4)) return Optional.empty();
                int py = h(c, pl[0], pl[1]);
                if (py < c.chunkGenerator().getSeaLevel()) return Optional.empty();
                List<Piece> one = new ArrayList<>();
                single(one, T.MORGUL, pl[0], py, pl[1], 3, c.seed() ^ 0x4d4f5247554cL, 0);      // rot 3 : la porte regarde vers l'ouest
                return Optional.of(new GenerationStub(new BlockPos(pl[0], py, pl[1]), b -> one.forEach(b::addPiece)));
            }
            if (kind.equals("edoras")) {
                // une seule instance, au lieu marque sur la carte : colline rocheuse, palais d'Or, tour de cloche et maisons au toit de chaume
                int[] pl = MapData.place("edoras");
                if (pl == null || cp.x != (pl[0] >> 4) || cp.z != (pl[1] >> 4)) return Optional.empty();
                int py = h(c, pl[0], pl[1]);
                if (py < c.chunkGenerator().getSeaLevel()) return Optional.empty();
                List<Piece> one = new ArrayList<>();
                single(one, T.EDORAS, pl[0], py, pl[1], 0, c.seed() ^ 0x45444f524153L, 0);
                return Optional.of(new GenerationStub(new BlockPos(pl[0], py, pl[1]), b -> one.forEach(b::addPiece)));
            }
            if (kind.equals("minas_terith")) {
                // une seule instance, au lieu marque sur la carte : cite de sept niveaux adossee a la montagne, tour d'Ecthelion et cour de l'Arbre Blanc
                int[] pl = MapData.place("minas_terith");
                if (pl == null || cp.x != (pl[0] >> 4) || cp.z != (pl[1] >> 4)) return Optional.empty();
                int py = h(c, pl[0], pl[1] + 60);
                if (py < c.chunkGenerator().getSeaLevel()) return Optional.empty();
                List<Piece> one = new ArrayList<>();
                single(one, T.TIRITH, pl[0], py, pl[1], 0, c.seed() ^ 0x4d494e415354L, 0);
                return Optional.of(new GenerationStub(new BlockPos(pl[0], py, pl[1]), b -> one.forEach(b::addPiece)));
            }
            if (kind.equals("barad_dur")) {
                // une seule instance, au lieu marque sur la carte : forteresse de 80 x 80 et tour de 250 blocs, ceinte d'une douve de lave
                int[] pl = MapData.place("barad_dur");
                if (pl == null || cp.x != (pl[0] >> 4) || cp.z != (pl[1] >> 4)) return Optional.empty();
                int py = h(c, pl[0], pl[1]);
                if (py < c.chunkGenerator().getSeaLevel()) return Optional.empty();
                List<Piece> one = new ArrayList<>();
                single(one, T.BARADDUR, pl[0], py, pl[1], 0, c.seed() ^ 0x4241524144L, 0);
                return Optional.of(new GenerationStub(new BlockPos(pl[0], py, pl[1]), b -> one.forEach(b::addPiece)));
            }
            if (kind.equals("orthanc")) {
                // une seule instance, au lieu marque sur la carte : tour de 30 x 30 au centre d'une enceinte de 200 blocs de diametre
                int[] pl = MapData.place("orthanc");
                if (pl == null || cp.x != (pl[0] >> 4) || cp.z != (pl[1] >> 4)) return Optional.empty();
                int py = h(c, pl[0], pl[1]);
                if (py < c.chunkGenerator().getSeaLevel()) return Optional.empty();
                List<Piece> one = new ArrayList<>();
                single(one, T.ORTHANC, pl[0], py, pl[1], 0, c.seed() ^ 0x4f525448414eL, 0);
                return Optional.of(new GenerationStub(new BlockPos(pl[0], py, pl[1]), b -> one.forEach(b::addPiece)));
            }
            int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
            int cy = h(c, cx, cz);
            int sea = c.chunkGenerator().getSeaLevel();
            if (cy < sea) return Optional.empty();
            RandomSource r = c.random();
            long seed = c.seed() ^ (cx * 73428767L) ^ (cz * 912931L);
            List<Piece> out = new ArrayList<>();
            switch (kind) {
                case "halfling_village" -> {
                    single(out, T.PLAZA, cx, cy, cz, 0, seed, 0);
                    ring(out, c, cx, cy, cz, 23, new T[]{T.BURROW, T.BURROW, T.FARM, T.BURROW, T.INN, T.BURROW, T.BURROW, T.FARM, T.BURROW}, seed, sea);
                    if (out.size() < 5) return Optional.empty();
                }
                case "human_village" -> {
                    single(out, T.PLAZA, cx, cy, cz, 0, seed, 1);
                    ring(out, c, cx, cy, cz, 27, new T[]{T.HOUSE, T.STALL, T.HOUSE, T.SMITHY, T.HOUSE, T.HALL, T.HOUSE, T.STABLE, T.HOUSE, T.STALL, T.WATCHTOWER}, seed, sea);
                    if (out.size() < 6) return Optional.empty();
                }
                case "elf_village" -> {
                    single(out, T.PLAZA, cx, cy, cz, 0, seed, 2);
                    ring(out, c, cx, cy, cz, 22, new T[]{T.PAVILION, T.PAVILION, T.ETOWER, T.PAVILION, T.PAVILION, T.PAVILION, T.ETOWER, T.PAVILION}, seed, sea);
                    if (out.size() < 5) return Optional.empty();
                }
                case "dwarf_mine" -> {
                    if (cy < 80) return Optional.empty();
                    int rot = r.nextInt(4);
                    int len = Math.min(36, Math.max(24, cy - 40));
                    int ox = cx, oz = cz;
                    // tout est aligne dans le meme repere local : x de -?, z vers l'interieur de la montagne
                    out.add(new Piece(T.POOL, Build.wx(ox, rot, -5, -10), cy, Build.wz(oz, rot, -5, -10), rot, seed + 1, 0));
                    out.add(new Piece(T.GATE, Build.wx(ox, rot, -7, 0), cy, Build.wz(oz, rot, -7, 0), rot, seed + 2, 0));
                    out.add(new Piece(T.STAIR, Build.wx(ox, rot, -3, 8), cy, Build.wz(oz, rot, -3, 8), rot, seed + 3, len));
                    int hy = cy - len;
                    out.add(new Piece(T.DHALL, Build.wx(ox, rot, -16, 8 + len), hy, Build.wz(oz, rot, -16, 8 + len), rot, seed + 4, 0));
                    out.add(new Piece(T.MINE, Build.wx(ox, rot, -10, 35 + len), hy, Build.wz(oz, rot, -10, 35 + len), rot, seed + 5, 0));
                    out.add(new Piece(T.LAIR, Build.wx(ox, rot, -13, 52 + len), hy - 6, Build.wz(oz, rot, -13, 52 + len), rot, seed + 6, 0));
                }
                case "orc_fort" -> single(out, T.FORT, cx, cy, cz, r.nextInt(4), seed, 0);
                case "dark_tower" -> single(out, T.DTOWER, cx, cy, cz, r.nextInt(4), seed, 0);
                case "barrow" -> single(out, T.BARROW, cx, cy, cz, r.nextInt(4), seed, 0);
                case "hoard" -> { if (cy < 75) return Optional.empty(); single(out, T.HOARD, cx, cy, cz, r.nextInt(4), seed, 0); }
                case "pyre_tower" -> single(out, T.PYRE, cx, cy, cz, r.nextInt(4), seed, 0);
                default -> single(out, T.RUIN, cx, cy, cz, r.nextInt(4), seed, r.nextInt(3));
            }
            if (out.isEmpty()) return Optional.empty();
            return Optional.of(new GenerationStub(new BlockPos(cx, cy, cz), b -> out.forEach(b::addPiece)));
        }

        /** batiment centre sur (cx, cz) */
        private static void single(List<Piece> out, T t, int cx, int cy, int cz, int rot, long seed, int aux) {
            out.add(new Piece(t, Build.wx(cx, rot, -t.w / 2, -t.d / 2), cy, Build.wz(cz, rot, -t.w / 2, -t.d / 2), rot, seed, aux));
        }

        /** batiments en cercle autour du centre, facade tournee vers lui */
        private static void ring(List<Piece> out, GenerationContext c, int cx, int cy, int cz, int radius, T[] types, long seed, int sea) {
            int n = types.length;
            for (int i = 0; i < n; i++) {
                T t = types[i];
                double a = Math.PI * 2 * i / n + 0.35;
                double rr = radius + (t == T.WATCHTOWER ? 10 : 0);
                int fx = cx + (int) Math.round(Math.cos(a) * rr), fz = cz + (int) Math.round(Math.sin(a) * rr);
                double dx = cx - fx, dz = cz - fz;
                // direction locale -z (facade) -> vers le centre
                int rot = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 1 : 3) : (dz > 0 ? 2 : 0);
                int ox = Build.wx(fx, rot, -t.w / 2, 0), oz = Build.wz(fz, rot, -t.w / 2, 0);
                int mx = Build.wx(ox, rot, t.w / 2, t.d / 2), mz = Build.wz(oz, rot, t.w / 2, t.d / 2);
                int y = h(c, mx, mz);
                if (y < sea || Math.abs(y - cy) > 9) continue;
                Piece p = new Piece(t, ox, y, oz, rot, seed + 31L * (i + 1), i);
                boolean clash = false;
                for (Piece q : out) if (q.type != T.PLAZA && q.getBoundingBox().inflatedBy(1).intersects(p.getBoundingBox())) { clash = true; break; }
                if (!clash) out.add(p);
            }
        }
    }

    public static class Piece extends StructurePiece {
        public final T type;
        public final int ox, oy, oz, rot, aux;
        public final long seed;

        private static BoundingBox box(T t, int ox, int oy, int oz, int rot, int aux) {
            int d = t == T.STAIR ? aux + 2 : t.d;
            int below = t == T.STAIR ? aux + 3 : t.below;
            int x0 = Build.wx(ox, rot, 0, 0), z0 = Build.wz(oz, rot, 0, 0), x1 = Build.wx(ox, rot, t.w - 1, d - 1), z1 = Build.wz(oz, rot, t.w - 1, d - 1);
            return new BoundingBox(Math.min(x0, x1), oy - below, Math.min(z0, z1), Math.max(x0, x1), oy + t.h, Math.max(z0, z1));
        }

        public Piece(T type, int ox, int oy, int oz, int rot, long seed, int aux) {
            super(ModWorldgen.SETTLEMENT_PIECE.get(), 0, box(type, ox, oy, oz, rot, aux));
            this.type = type; this.ox = ox; this.oy = oy; this.oz = oz; this.rot = rot; this.seed = seed; this.aux = aux;
        }

        public Piece(CompoundTag tag) {
            super(ModWorldgen.SETTLEMENT_PIECE.get(), tag);
            type = T.values()[Math.floorMod(tag.getInt("t"), T.values().length)];
            ox = tag.getInt("ox"); oy = tag.getInt("oy"); oz = tag.getInt("oz"); rot = tag.getInt("rot"); aux = tag.getInt("aux"); seed = tag.getLong("seed");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            tag.putInt("t", type.ordinal()); tag.putInt("ox", ox); tag.putInt("oy", oy); tag.putInt("oz", oz);
            tag.putInt("rot", rot); tag.putInt("aux", aux); tag.putLong("seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
            Build b = new Build(level, box, ox, oy, oz, rot, seed);
            Buildings.build(b, type, aux);
        }
    }
}
