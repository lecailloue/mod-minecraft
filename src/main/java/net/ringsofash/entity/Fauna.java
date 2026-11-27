package net.ringsofash.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ringsofash.world.Travel;

/**
 * Le repeuplement des bois et des rivieres.
 *
 * Les listes d'apparition des biomes ne suffisent pas : la categorie « creature » ne se
 * remplit guere qu'a la creation des chunks, si bien qu'un monde deja parcouru reste
 * vide. On seme donc nous-memes, autour de chaque joueur, tant que le coin n'est pas
 * deja plein : un petit groupe a la fois, a bonne distance, et seulement la ou l'espece
 * a sa place.
 */
public final class Fauna {
    private Fauna() {}

    private static final int CHAQUE = 80;        // on tente toutes les quatre secondes
    private static final int RAYON = 72;         // le voisinage que l'on garde peuple
    private static final int PLEIN = 14;         // au-dela, on laisse tranquille
    private static final int LOIN = 28, PRES = 64;

    public static void init(IEventBus bus) { bus.addListener(Fauna::tick); }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % CHAQUE != 0) return;
        if (!Travel.isLands(p.level())) return;
        ServerLevel level = p.serverLevel();

        var box = p.getBoundingBox().inflate(RAYON);
        if (level.getEntitiesOfClass(BeastEntity.class, box).size() >= PLEIN) return;

        RandomSource rnd = level.random;
        for (int essai = 0; essai < 6; essai++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = LOIN + rnd.nextDouble() * (PRES - LOIN);
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            List<EntityType<BeastEntity>> choix = espece(level, pos);
            if (choix.isEmpty()) continue;
            if (!place(level, pos)) continue;
            semer(level, pos, choix.get(rnd.nextInt(choix.size())), 1 + rnd.nextInt(3));
            return;
        }
    }

    /** y a-t-il de quoi poser une bete ici ? */
    private static boolean place(ServerLevel level, BlockPos pos) {
        var sous = level.getBlockState(pos.below());
        boolean sol = sous.isSolid() || sous.is(BlockTags.LEAVES);
        boolean eau = level.getFluidState(pos.below()).is(FluidTags.WATER) || level.getFluidState(pos).is(FluidTags.WATER);
        if (!sol && !eau) return false;
        return level.getBlockState(pos).isAir() || eau;
    }

    /** ce qui vit dans ce coin-la */
    private static List<EntityType<BeastEntity>> espece(ServerLevel level, BlockPos pos) {
        boolean eau = level.getFluidState(pos.below()).is(FluidTags.WATER) || pres(level, pos);
        if (eau) return List.of(ModEntities.DUCK.get(), ModEntities.HERON.get(), ModEntities.BEAVER.get());

        ResourceLocation biome = level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
        String nom = biome == null ? "" : biome.getPath();
        if (nom.contains("foret") || nom.equals("fangorn") || nom.contains("bosquet"))
            return List.of(ModEntities.DEER.get(), ModEntities.ROE_DEER.get(), ModEntities.SQUIRREL.get(),
                    ModEntities.SQUIRREL.get(), ModEntities.DEER.get());          // les bois surtout
        if (nom.contains("plaine") && !nom.contains("cendre"))
            return List.of(ModEntities.ROE_DEER.get(), ModEntities.DEER.get());
        if (nom.contains("mont") && !nom.contains("cendre"))
            return List.of(ModEntities.ROE_DEER.get(), ModEntities.SQUIRREL.get());
        return List.of();
    }

    /** de l'eau a quatre blocs ? alors c'est un bord de riviere */
    private static boolean pres(ServerLevel level, BlockPos pos) {
        for (int dx = -4; dx <= 4; dx += 2) for (int dz = -4; dz <= 4; dz += 2)
            if (level.getFluidState(pos.offset(dx, -1, dz)).is(FluidTags.WATER)) return true;
        return false;
    }

    private static void semer(ServerLevel level, BlockPos pos, EntityType<BeastEntity> type, int combien) {
        for (int i = 0; i < combien; i++) {
            BeastEntity b = type.create(level);
            if (b == null) return;
            double ox = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 5;
            double oz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 5;
            b.moveTo(ox, pos.getY(), oz, level.random.nextFloat() * 360F, 0F);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            level.addFreshEntity(b);
        }
    }
}
