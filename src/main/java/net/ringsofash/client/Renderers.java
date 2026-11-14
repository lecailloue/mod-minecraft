package net.ringsofash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import net.ringsofash.entity.FoeEntity;
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

    /** Orques : 7 modeles (4 orques, 2 orques noirs, 1 chef) ; le modele et la couleur de peau viennent de la variante de l'entite. */
    public static class Orc extends MobRenderer<FoeEntity, GeoModel<FoeEntity>> {
        public static final String[] NAMES = {"orc_1_nu", "orc_2_fourrure", "orc_3_mailles", "orc_4_casque", "orc_noir_1_main", "orc_noir_2_fer", "orc_chef"};
        /** hauteur visee a l'ecran (en blocs) de chaque modele */
        public static final float[] BLOCKS = {1.88F, 1.95F, 2.02F, 2.10F, 2.20F, 2.30F, 2.75F};
        private final GeoModel<FoeEntity>[] models;
        private final float[] scales = new float[NAMES.length];
        private final int first, count;

        @SuppressWarnings("unchecked")
        public Orc(EntityRendererProvider.Context ctx, int first, int count, float shadow) {
            super(ctx, new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[first])), NAMES[first]), shadow);
            this.first = first; this.count = count;
            models = new GeoModel[NAMES.length];
            for (int i = first; i < first + count; i++) {
                models[i] = new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[i])), NAMES[i]);
                scales[i] = BLOCKS[i] * 16F / GeoDef.get(NAMES[i]).height;
            }
        }

        private int idx(FoeEntity e) { return Math.min(first + count - 1, Math.max(first, e.orcModel())); }

        @Override
        public void render(FoeEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            this.model = models[idx(e)];
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override public ResourceLocation getTextureLocation(FoeEntity e) { return tex(NAMES[idx(e)] + "_" + e.orcColor()); }
        @Override protected void scale(FoeEntity e, PoseStack ps, float pt) { float s = scales[idx(e)]; ps.scale(s, s, s); }
    }

    /** Gobelins : 3 types (1,2 / 1,5 / 1,8 blocs) et leur roi (2,7 blocs) ; le type et la couleur de peau viennent de la variante de l'entite. */
    public static class Goblin extends MobRenderer<FoeEntity, GeoModel<FoeEntity>> {
        public static final String[] NAMES = {"gobelin_1", "gobelin_2", "gobelin_3", "gobelin_roi"};
        /** hauteur visee a l'ecran (en blocs) de chaque modele : 1 bloc = 1 m */
        public static final float[] BLOCKS = {1.2F, 1.5F, 1.8F, 2.7F};
        private final GeoModel<FoeEntity>[] models;
        private final float[] scales = new float[NAMES.length];

        @SuppressWarnings("unchecked")
        public Goblin(EntityRendererProvider.Context ctx, boolean king) {
            super(ctx, new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[king ? 3 : 0])), NAMES[king ? 3 : 0]), king ? 1.1F : 0.5F);
            models = new GeoModel[NAMES.length];
            for (int i = king ? 3 : 0; i < (king ? 4 : 3); i++) {
                models[i] = new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[i])), NAMES[i]);
                scales[i] = BLOCKS[i] * 16F / GeoDef.get(NAMES[i]).height;
            }
        }

        private int idx(FoeEntity e) { return Math.min(NAMES.length - 1, Math.max(0, e.goblinModel())); }

        @Override
        public void render(FoeEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            this.model = models[idx(e)];
            this.shadowRadius = 0.35F + 0.15F * idx(e);
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override public ResourceLocation getTextureLocation(FoeEntity e) { return tex(NAMES[idx(e)] + "_" + e.goblinSkin()); }
        @Override protected void scale(FoeEntity e, PoseStack ps, float pt) { float s = scales[idx(e)]; ps.scale(s, s, s); }
    }

    /** Etres des ombres : cavalier + cheval en un seul modele (2 cavaliers, 1 chef), yeux du cheval lumineux. */
    public static class Shadow extends MobRenderer<net.ringsofash.entity.ShadowRiderEntity, GeoModel<net.ringsofash.entity.ShadowRiderEntity>> {
        public static final String[] NAMES = {"ombre_1", "ombre_2", "ombre_chef"};
        public static final float[] BLOCKS = {2.8F, 2.8F, 3.8F};
        private final GeoModel<net.ringsofash.entity.ShadowRiderEntity>[] models;
        private final float[] scales = new float[NAMES.length];

        @SuppressWarnings("unchecked")
        public Shadow(EntityRendererProvider.Context ctx) {
            super(ctx, new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[0])), NAMES[0]), 0.9F);
            models = new GeoModel[NAMES.length];
            for (int i = 0; i < NAMES.length; i++) {
                models[i] = new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[i])), NAMES[i]);
                scales[i] = BLOCKS[i] * 16F / GeoDef.get(NAMES[i]).height;
            }
            addLayer(new RenderLayer<net.ringsofash.entity.ShadowRiderEntity, GeoModel<net.ringsofash.entity.ShadowRiderEntity>>(this) {
                @Override
                public void render(PoseStack ps, MultiBufferSource buf, int light, net.ringsofash.entity.ShadowRiderEntity e, float a, float b, float c, float d, float f, float g) {
                    VertexConsumer vc = buf.getBuffer(RenderType.eyes(tex(NAMES[e.shadowModel()] + "_glow")));
                    getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        public void render(net.ringsofash.entity.ShadowRiderEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            this.model = models[e.shadowModel()];
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override public ResourceLocation getTextureLocation(net.ringsofash.entity.ShadowRiderEntity e) { return tex(NAMES[e.shadowModel()]); }
        @Override protected void scale(net.ringsofash.entity.ShadowRiderEntity e, PoseStack ps, float pt) { float s = scales[e.shadowModel()]; ps.scale(s, s, s); }
    }

    /** Les 5 nouveaux modeles spectraux : translucides (peau et tissus par pixel), squelette visible dessous, yeux lumineux. */
    public static class Wraith extends MobRenderer<FoeEntity, GeoModel<FoeEntity>> {
        public static final String[] NAMES = {"spectre_epee", "spectre_lance", "spectre_hache", "spectre_dague", "spectre_roi"};
        public static final float[] BLOCKS = {1.95F, 1.95F, 1.9F, 1.9F, 2.65F};
        private final GeoModel<FoeEntity>[] models;
        private final float[] scales = new float[NAMES.length];

        @SuppressWarnings("unchecked")
        public Wraith(EntityRendererProvider.Context ctx) {
            super(ctx, new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[0])), NAMES[0]), 0.5F);
            models = new GeoModel[NAMES.length];
            for (int i = 0; i < NAMES.length; i++) {
                models[i] = new GeoModel<>(ctx.bakeLayer(ClientSetup.layer("geo_" + NAMES[i])), NAMES[i]);
                models[i].alpha = 255;
                scales[i] = BLOCKS[i] * 16F / GeoDef.get(NAMES[i]).height;
            }
            addLayer(new RenderLayer<FoeEntity, GeoModel<FoeEntity>>(this) {
                @Override
                public void render(PoseStack ps, MultiBufferSource buf, int light, FoeEntity e, float a, float b, float c, float d, float f, float g) {
                    VertexConsumer vc = buf.getBuffer(RenderType.eyes(tex(NAMES[idx(e)] + "_glow")));
                    getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        private int idx(FoeEntity e) { return Math.min(NAMES.length - 1, Math.max(0, e.spectreModel())); }

        @Override
        public void render(FoeEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            this.model = models[idx(e)];
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override public ResourceLocation getTextureLocation(FoeEntity e) { return tex(NAMES[idx(e)]); }
        @Override protected void scale(FoeEntity e, PoseStack ps, float pt) { float s = scales[idx(e)]; ps.scale(s, s, s); }

        @Nullable
        @Override
        protected RenderType getRenderType(FoeEntity e, boolean visible, boolean ghost, boolean glowing) {
            if (visible) return RenderType.entityTranslucent(getTextureLocation(e));
            return super.getRenderType(e, visible, ghost, glowing);
        }
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
