package net.ringsofash.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * La couleur d'un bloc sur la carte.
 *
 * Minecraft ne donne qu'une poignee de teintes a ses blocs, et un bloc du mod se retrouve
 * souvent sans rien ou avec la couleur de la pierre. On prend donc la vraie texture du
 * bloc et on en fait la moyenne : chaque bloc a enfin sa couleur a lui. Pour l'herbe, les
 * feuilles et l'eau, dont la texture est grise et coloree au dernier moment selon le
 * biome, on applique la teinte du biome par-dessus.
 *
 * Le resultat est garde en memoire : une texture n'est lue qu'une fois.
 */
public final class BlockTint {
    private BlockTint() {}

    private static final Map<Block, Integer> CACHE = new HashMap<>();

    /** ce qui est teinte par le biome et doit l'etre a la main */
    private enum Tint { NONE, GRASS, FOLIAGE, WATER }

    public static void clear() { CACHE.clear(); }

    /** la couleur de ce bloc tel qu'il se presente a cet endroit */
    public static int of(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        int base = average(state.getBlock());
        if (base == 0) {                                  // texture illisible : on retombe sur Minecraft
            MapColor mc = state.getMapColor(level, pos);
            return mc == MapColor.NONE ? 0x7F7F7F : mc.col;
        }
        return switch (tint(state)) {
            case GRASS -> mix(base, BiomeColors.getAverageGrassColor(level, pos));
            case FOLIAGE -> mix(base, BiomeColors.getAverageFoliageColor(level, pos));
            case WATER -> mix(base, BiomeColors.getAverageWaterColor(level, pos));
            case NONE -> base;
        };
    }

    private static Tint tint(BlockState s) {
        if (s.is(BlockTags.LEAVES)) return Tint.FOLIAGE;
        if (s.is(Blocks.WATER) || s.is(Blocks.BUBBLE_COLUMN) || s.is(Blocks.WATER_CAULDRON)) return Tint.WATER;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) || s.is(Blocks.VINE)
                || s.is(Blocks.SUGAR_CANE) || s.is(BlockTags.REPLACEABLE_BY_TREES)) return Tint.GRASS;
        return Tint.NONE;
    }

    /** la texture d'un bloc, moyennee ; 0 si on n'a pas su la lire */
    private static int average(Block block) {
        Integer known = CACHE.get(block);
        if (known != null) return known;
        int col = 0;
        try {
            BlockState s = block.defaultBlockState();
            TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(s).getParticleIcon();
            ResourceLocation tex = sprite.contents().name();
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), "textures/" + tex.getPath() + ".png");
            var res = Minecraft.getInstance().getResourceManager().getResource(file);
            if (res.isPresent()) {
                try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                    col = average(img);
                }
            }
        } catch (Exception ignored) {
            col = 0;                                      // tant pis, Minecraft decidera
        }
        CACHE.put(block, col);
        return col;
    }

    /** la moyenne des pixels assez opaques, sur la premiere image si la texture est animee */
    private static int average(NativeImage img) {
        int w = img.getWidth(), h = Math.min(img.getHeight(), img.getWidth());
        long r = 0, g = 0, b = 0;
        int n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int abgr = img.getPixelRGBA(x, y);
                if (((abgr >> 24) & 255) < 128) continue;
                r += abgr & 255;
                g += (abgr >> 8) & 255;
                b += (abgr >> 16) & 255;
                n++;
            }
        }
        if (n == 0) return 0;
        return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
    }

    /** la texture grise prend la couleur du biome */
    private static int mix(int base, int tint) {
        int r = ((base >> 16) & 255) * ((tint >> 16) & 255) / 255;
        int g = ((base >> 8) & 255) * ((tint >> 8) & 255) / 255;
        int b = (base & 255) * (tint & 255) / 255;
        return r << 16 | g << 8 | b;
    }

}
