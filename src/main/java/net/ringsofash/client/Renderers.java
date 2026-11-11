package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.entity.GiantSpiderEntity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Renderers {
    private Renderers() {}

    private static final Map<String, ResourceLocation> TEX = new ConcurrentHashMap<>();
    public static ResourceLocation tex(String path) { return TEX.computeIfAbsent(path, p -> RingsOfAsh.id("textures/entity/" + p + ".png")); }

    /** Creature a modele sur mesure. */
    public static class Geo<T extends Mob> extends MobRenderer<T, GeoModel<T>> {
        private final float scale;
        private final Function<T, String> texture;

        public Geo(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String geo, float shadow, float scale, Function<T, String> texture, @Nullable String glow) {
            super(ctx, new GeoModel<>(ctx.bakeLayer(layer), geo), shadow);
            this.scale = scale;
            this.texture = texture;
            if (glow != null) {
                final RenderType type = RenderType.eyes(tex(glow));
                addLayer(new EyesLayer<T, GeoModel<T>>(this) {
                    @Override public RenderType renderType() { return type; }
                });
            }
        }

        @Override public ResourceLocation getTextureLocation(T e) { return tex(texture.apply(e)); }
        @Override protected void scale(T e, PoseStack ps, float pt) { ps.scale(scale, scale, scale); }
    }

    /** Humanoide (modele de joueur + oreilles, defenses ou barbe selon le peuple). */
    public static class Human<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
        private final float sx, sy;
        private final Function<T, String> texture;
        private final boolean translucent;

        public Human(EntityRendererProvider.Context ctx, ModelLayerLocation layer, float sx, float sy, Function<T, String> texture, boolean translucent) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(layer), false), 0.5F * sx);
            this.sx = sx; this.sy = sy; this.texture = texture; this.translucent = translucent;
            addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        }

        @Override public ResourceLocation getTextureLocation(T e) { return tex("people/" + texture.apply(e)); }
        @Override protected void scale(T e, PoseStack ps, float pt) { ps.scale(sx, sy, sx); }

        @Nullable
        @Override
        protected RenderType getRenderType(T e, boolean visible, boolean ghost, boolean glowing) {
            if (translucent && visible) return RenderType.entityTranslucent(getTextureLocation(e));
            return super.getRenderType(e, visible, ghost, glowing);
        }
    }

    public static class GiantSpider extends SpiderRenderer<GiantSpiderEntity> {
        public GiantSpider(EntityRendererProvider.Context ctx) { super(ctx); this.shadowRadius = 2.0F; }
        @Override public ResourceLocation getTextureLocation(GiantSpiderEntity e) { return tex("giant_spider"); }
        @Override protected void scale(GiantSpiderEntity e, PoseStack ps, float pt) { ps.scale(2.5F, 2.5F, 2.5F); }
    }
}
