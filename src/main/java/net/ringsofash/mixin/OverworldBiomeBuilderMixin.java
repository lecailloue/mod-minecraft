package net.ringsofash.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.ringsofash.world.ModWorldgen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/** Partage la zone des iles champignon : la moitie chaude devient le biome volcanique (meme rarete). */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    @ModifyVariable(method = "addBiomes", at = @At("HEAD"), argsOnly = true)
    private Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> roa$splitMushroom(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> key) {
        // Le jeu construit aussi une copie "vanilla pure" de ses registres (validation, generation de donnees) :
        // notre biome n'y existe pas, il ne faut donc pas l'y injecter.
        boolean vanillaBootstrap = StackWalker.getInstance().walk(st -> st.anyMatch(f ->
                f.getClassName().endsWith("RegistrySetBuilder") || f.getClassName().endsWith("VanillaRegistries")));
        if (vanillaBootstrap) return key;
        return pair -> {
            if (pair.getSecond() == Biomes.MUSHROOM_FIELDS) {
                Climate.ParameterPoint p = pair.getFirst();
                key.accept(Pair.of(new Climate.ParameterPoint(Climate.Parameter.span(-1.0F, 0.12F), p.humidity(), p.continentalness(),
                        p.erosion(), p.depth(), p.weirdness(), p.offset()), Biomes.MUSHROOM_FIELDS));
                key.accept(Pair.of(new Climate.ParameterPoint(Climate.Parameter.span(0.12F, 1.0F), p.humidity(), p.continentalness(),
                        p.erosion(), p.depth(), p.weirdness(), p.offset()), ModWorldgen.VOLCANO_BIOME));
            } else {
                key.accept(pair);
            }
        };
    }
}
