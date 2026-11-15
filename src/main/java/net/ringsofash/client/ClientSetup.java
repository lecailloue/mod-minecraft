package net.ringsofash.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.entity.FoeEntity;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.gen.Reg;
import net.ringsofash.item.SpecialSword;

public final class ClientSetup {
    private ClientSetup() {}

    public static final KeyMapping REP_KEY = new KeyMapping("key.ringsofash.reputation", InputConstants.KEY_R, "key.categories.ringsofash");
    public static final KeyMapping RINGS_KEY = new KeyMapping("key.ringsofash.rings", InputConstants.KEY_G, "key.categories.ringsofash");
    public static final KeyMapping MAP_KEY = new KeyMapping("key.ringsofash.map", InputConstants.KEY_M, "key.categories.ringsofash");
    private static final String[] GEO = {"troll", "troll_chief", "warg", "stag", "demon", "lurker", "treeman", "eagle", "dragon", "mammoth", "ash_lord", "orc_1_nu", "orc_2_fourrure", "orc_3_mailles", "orc_4_casque", "orc_noir_1_main", "orc_noir_2_fer", "orc_chef", "spectre_epee", "spectre_lance", "spectre_hache", "spectre_dague", "spectre_roi", "ombre_1", "ombre_2", "ombre_chef", "arme_orc_cleaver", "arme_orc_scimitar", "arme_orc_machete", "arme_orc_spear", "arme_orc_mace", "gobelin_1", "gobelin_2", "gobelin_3", "gobelin_roi", "arme_goblin_king_staff", "arme_goblin_pickaxe", "giant_spider", "sylvestre_ancien", "sylvestre_saule", "shadow_dragon", "shadow_master"};
    private static final String[] HUMAN = {"plain", "elf", "orc", "goblin", "dwarf"};

    public static ModelLayerLocation layer(String name) { return new ModelLayerLocation(RingsOfAsh.id(name), "main"); }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::layers);
        modBus.addListener(ClientSetup::renderers);
        Atmosphere.init(modBus);
        SwordAura.init();
        SylvanRender.init();
        SwordSwing.init();
        RingTabs.init();
        modBus.addListener(ClientSetup::addLayers);
        modBus.addListener(ClientSetup::keys);
        modBus.addListener(ClientSetup::gui);
        modBus.addListener(ClientSetup::screens);
        modBus.addListener(ClientSetup::blockColors);
        modBus.addListener(ClientSetup::itemColors);
        modBus.addListener(ClientSetup::setup);
        modBus.addListener(ClientSetup::itemExtensions);
        NeoForge.EVENT_BUS.addListener(ClientSetup::tick);
        if (System.getProperty("roa.clienttest") != null) ClientTest.init();
    }

    private static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, idx) -> level == null || pos == null ? DwarvenTint.item() : DwarvenTint.at(pos), net.ringsofash.gen.Tinted.blocks());
    }

    private static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) {
        e.register((stack, idx) -> 0xFF000000 | DwarvenTint.item(), net.ringsofash.gen.Tinted.items());
    }

    private static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
        e.register(net.ringsofash.menu.ModMenus.FORGE.get(), ForgeScreen::new);
        e.register(net.ringsofash.menu.ModMenus.RINGS.get(), RingsScreen::new);
    }

    private static void gui(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) {
        e.registerAboveAll(RingsOfAsh.id("compass"), (g, dt) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            int cx = g.guiWidth() / 2, y = 4;
            float yaw = mc.player.getYRot();
            g.fill(cx - 62, y - 2, cx + 62, y + 11, 0x66000000);
            String[] names = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
            for (int i = 0; i < 8; i++) {
                float d = net.minecraft.util.Mth.wrapDegrees(i * 45F - yaw);
                if (Math.abs(d) > 55) continue;
                int px = cx + Math.round(d * 1.05F);
                boolean card = i % 2 == 0;
                int col = i == 4 ? 0xFFFF4040 : card ? 0xFFFFFFFF : 0xFFAAAAAA;
                g.drawString(mc.font, names[i], px - mc.font.width(names[i]) / 2, y, col, true);
            }
            g.fill(cx, y + 10, cx + 1, y + 13, 0xFFFFD070);
            // reperes poses sur la carte : une epingle coloree sous la boussole, a leur cap ; celui qu'on vise affiche nom et distance
            if (MapScreen.inLands()) {
                MapScreen.Marker aim = null;
                float aimD = 9F;
                double aimDist = 0;
                for (MapScreen.Marker m : MapScreen.markers()) {
                    double dx = m.x + 0.5 - mc.player.getX(), dz = m.z + 0.5 - mc.player.getZ();
                    float d = net.minecraft.util.Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
                    if (Math.abs(d) > 58) continue;
                    int px = cx + Math.round(d * 1.05F);
                    g.fill(px, y + 11, px + 1, y + 13, 0xFF000000 | m.color);
                    Ui.icon(g, Ui.I_PIN, px, y + 18, 0xFF000000 | m.color);
                    if (Math.abs(d) < aimD) { aimD = Math.abs(d); aim = m; aimDist = Math.sqrt(dx * dx + dz * dz); }
                }
                if (aim != null) {
                    String t = aim.label() + " · " + (aimDist >= 1000 ? String.format("%.1f km", aimDist / 1000) : (int) aimDist + " m");
                    g.drawString(mc.font, t, cx - mc.font.width(t) / 2, y + 26, 0xFF000000 | aim.color, true);
                }
            }
        });
        e.registerAboveAll(RingsOfAsh.id("active_ring"), (g, dt) -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.item.ItemStack st = net.ringsofash.data.Net.ClientRings.active();
            if (mc.player == null || mc.options.hideGui || st.isEmpty()) return;
            g.fill(4, 4, 24, 24, 0x66000000);
            g.renderItem(st, 6, 6);
        });
        e.registerAboveAll(RingsOfAsh.id("quest_tracker"), (g, dt) -> QuestScreen.renderTracker(g));
    }

    private static void keys(RegisterKeyMappingsEvent e) { e.register(REP_KEY); e.register(MAP_KEY); e.register(RINGS_KEY); }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (MAP_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new MapScreen());
        }
        while (RINGS_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ringsofash.data.Net.OpenRings());
        }
        if (!net.minecraft.client.gui.screens.Screen.hasAltDown()) RingWheel.waitRelease = false;
        else if (mc.player != null && mc.screen == null && !RingWheel.waitRelease && RingWheel.hasRings()) mc.setScreen(new RingWheel());
        while (REP_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new QuestScreen(0));
        }
    }

    /** Armes d'orques : rendu 3D (meme modele que celui porte par les orques). */
    private static void itemExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) {
        net.neoforged.neoforge.client.extensions.common.IClientItemExtensions ext = new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private WeaponItemRenderer renderer;
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new WeaponItemRenderer();
                return renderer;
            }
        };
        e.registerItem(ext, Reg.ORC_CLEAVER.get(), Reg.ORC_SCIMITAR.get(), Reg.ORC_MACHETE.get(), Reg.ORC_SPEAR.get(), Reg.ORC_MACE.get(), Reg.GOBLIN_KING_STAFF.get(), Reg.GOBLIN_PICKAXE.get());
    }

    private static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // la lame elfique luit quand des orques sont proches
            ItemProperties.register(Reg.ELVEN_BLADE.get(), RingsOfAsh.id("glow"), (stack, level, entity, seed) -> orcNear(entity) ? 1.0F : 0.0F);
            ItemProperties.register(Reg.ELVEN_GLAIVE.get(), RingsOfAsh.id("glow"), (stack, level, entity, seed) -> orcNear(entity) ? 1.0F : 0.0F);
            ItemProperties.register(Reg.SENTINEL_BLADE.get(), RingsOfAsh.id("glow"), (stack, level, entity, seed) -> near(1, entity, m -> m instanceof net.minecraft.world.entity.monster.Enemy) ? 1.0F : 0.0F);
            ItemProperties.register(Reg.SUN_BLADE.get(), RingsOfAsh.id("glow"), (stack, level, entity, seed) -> near(2, entity, SpecialSword::isUndead) ? 1.0F : 0.0F);
            ItemProperties.register(Reg.ELVEN_BOW.get(), RingsOfAsh.id("pull"), (stack, level, entity, seed) ->
                    entity == null || entity.getUseItem() != stack ? 0.0F : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
            ItemProperties.register(Reg.ELVEN_BOW.get(), RingsOfAsh.id("pulling"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

    private static int glowCheckTick = -100;
    private static boolean glowCached = false;

    private static boolean orcNear(LivingEntity holder) {
        if (holder == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || holder != mc.player) return false;
        if (Math.abs(holder.tickCount - glowCheckTick) >= 20) {
            glowCheckTick = holder.tickCount;
            glowCached = !holder.level().getEntitiesOfClass(FoeEntity.class, holder.getBoundingBox().inflate(20), SpecialSword::isOrc).isEmpty();
        }
        return glowCached;
    }

    private static final int[] nearTick = {-100, -100, -100};
    private static final boolean[] nearCache = new boolean[3];

    /** Un monstre repondant au filtre est-il a moins de 16 blocs du joueur ? (verifie une fois par seconde) */
    private static boolean near(int id, LivingEntity holder, java.util.function.Predicate<LivingEntity> filter) {
        if (holder == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || holder != mc.player) return false;
        if (Math.abs(holder.tickCount - nearTick[id]) >= 20) {
            nearTick[id] = holder.tickCount;
            nearCache[id] = !holder.level().getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(16), e -> e != holder && filter.test(e)).isEmpty();
        }
        return nearCache[id];
    }

    private static LayerDefinition human(String kind) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition head = mesh.getRoot().getChild("head");
        switch (kind) {
            case "elf" -> {
                head.addOrReplaceChild("roa_ear_r", CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, -4.0F, -1.0F, 1.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -3.5F, 0.5F, 0.45F, 0.0F, -0.3F));
                head.addOrReplaceChild("roa_ear_l", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(0.0F, -4.0F, -1.0F, 1.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -3.5F, 0.5F, 0.45F, 0.0F, 0.3F));
            }
            case "orc" -> {
                head.addOrReplaceChild("roa_ear_r", CubeListBuilder.create().texOffs(64, 16).addBox(-1.0F, -2.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.5F, 0.3F, 0.0F, -0.5F));
                head.addOrReplaceChild("roa_ear_l", CubeListBuilder.create().texOffs(64, 16).mirror().addBox(0.0F, -2.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -4.0F, 0.5F, 0.3F, 0.0F, 0.5F));
                head.addOrReplaceChild("roa_tusk_r", CubeListBuilder.create().texOffs(76, 0).addBox(-3.0F, -3.0F, -5.0F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
                head.addOrReplaceChild("roa_tusk_l", CubeListBuilder.create().texOffs(76, 0).addBox(2.0F, -3.0F, -5.0F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
            }
            case "goblin" -> {
                head.addOrReplaceChild("roa_ear_r", CubeListBuilder.create().texOffs(64, 8).addBox(-4.0F, -1.5F, 0.0F, 4.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, -4.5F, 0.0F, 0.0F, 0.5F, 0.3F));
                head.addOrReplaceChild("roa_ear_l", CubeListBuilder.create().texOffs(64, 8).mirror().addBox(0.0F, -1.5F, 0.0F, 4.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(4.0F, -4.5F, 0.0F, 0.0F, -0.5F, -0.3F));
            }
            case "dwarf" -> head.addOrReplaceChild("roa_beard", CubeListBuilder.create().texOffs(80, 0).addBox(-3.0F, -2.0F, -6.0F, 6.0F, 6.0F, 2.0F), PartPose.ZERO);
            default -> { }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static final ModelLayerLocation CLOAK = new ModelLayerLocation(RingsOfAsh.id("cloak"), "main");

    @SuppressWarnings("unchecked")
    private static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (net.minecraft.client.resources.PlayerSkin.Model m : new net.minecraft.client.resources.PlayerSkin.Model[]{net.minecraft.client.resources.PlayerSkin.Model.WIDE, net.minecraft.client.resources.PlayerSkin.Model.SLIM}) {
            var r = e.getSkin(m);
            if (r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                pr.addLayer(new CloakLayer(pr, e.getEntityModels().bakeLayer(CLOAK)));
                boolean slim = m == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
                try {                                                     // modele de joueur qui sait donner un coup d'epee realiste
                    var f = net.minecraft.client.renderer.entity.LivingEntityRenderer.class.getDeclaredField("model");
                    f.setAccessible(true);
                    f.set(pr, new SwordSwing.SwingPlayerModel(e.getEntityModels().bakeLayer(slim ? net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM : net.minecraft.client.model.geom.ModelLayers.PLAYER), slim));
                } catch (ReflectiveOperationException ex) {
                    RingsOfAsh.LOGGER.warn("coup d'epee 3e personne indisponible : " + ex);
                }
            }
        }
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CLOAK, CloakLayer::definition);
        for (String g : GEO) e.registerLayerDefinition(layer("geo_" + g), () -> GeoDef.layer(g));
        for (String h : HUMAN) e.registerLayerDefinition(layer("human_" + h), () -> human(h));
    }

    private static String peopleTex(PeopleEntity e, String base, int n) {
        if (e.role() == PeopleEntity.GUARD) return base + "_guard";
        if (e.role() == PeopleEntity.ELDER && base.equals("halfling")) return "halfling_elder";
        return base + "_" + (1 + Math.floorMod(e.variant(), n));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        ModelLayerLocation plain = layer("human_plain"), elf = layer("human_elf"), orc = layer("human_orc"), goblin = layer("human_goblin"), dwarf = layer("human_dwarf");
        e.registerEntityRenderer(ModEntities.ORC.get(), c -> new Renderers.Orc(c, 0, 4, 0.5F));
        e.registerEntityRenderer(ModEntities.WAR_ORC.get(), c -> new Renderers.Orc(c, 4, 2, 0.6F));
        e.registerEntityRenderer(ModEntities.ORC_CHIEF.get(), c -> new Renderers.Orc(c, 6, 1, 0.8F));
        e.registerEntityRenderer(ModEntities.GOBLIN.get(), c -> new Renderers.Goblin(c, false));
        e.registerEntityRenderer(ModEntities.GOBLIN_KING.get(), c -> new Renderers.Goblin(c, true));
        e.registerEntityRenderer(ModEntities.WILD_MAN.get(), c -> new Renderers.Human<FoeEntity>(c, plain, 1.0F, 1.0F, f -> "wild_man", false));
        e.registerEntityRenderer(ModEntities.DEAD_SOLDIER.get(), Renderers.Wraith::new);
        e.registerEntityRenderer(ModEntities.CAVE_TROLL.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_troll"), "troll", 1.5F, 1.3F, f -> "troll", null));
        e.registerEntityRenderer(ModEntities.STONE_TROLL.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_troll"), "troll", 1.5F, 1.3F, f -> f.isStone() ? "troll_statue" : "troll_stone", null));
        e.registerEntityRenderer(ModEntities.TROLL_CHIEF.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_troll_chief"), "troll_chief", 1.8F, 1.3F, f -> "troll_chief", "troll_chief_glow"));
        e.registerEntityRenderer(ModEntities.WARG.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_warg"), "warg", 0.8F, 1.15F, f -> "warg", null));
        e.registerEntityRenderer(ModEntities.MAMMOTH.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_mammoth"), "mammoth", 1.8F, 1.0F, f -> "mammoth", null));
        e.registerEntityRenderer(ModEntities.SHADOW_RIDER.get(), Renderers.Shadow::new);
        e.registerEntityRenderer(ModEntities.SHADOW_CHIEF.get(), Renderers.Shadow::new);
        e.registerEntityRenderer(ModEntities.SHADOW_DRAGON.get(), Renderers.ShadowDragon::new);
        e.registerEntityRenderer(ModEntities.SHADOW_MASTER.get(), Renderers.ShadowMaster::new);
        e.registerEntityRenderer(ModEntities.GIANT_SPIDER.get(), c -> new Renderers.Geo<net.ringsofash.entity.GiantSpiderEntity>(c, layer("geo_giant_spider"), "giant_spider", 2.2F,
                2.5F * 16F / GeoDef.get("giant_spider").height, f -> "giant_spider", "giant_spider_glow"));
        e.registerEntityRenderer(ModEntities.LURKER.get(), c -> new Renderers.Geo<>(c, layer("geo_lurker"), "lurker", 1.5F, 1.6F, f -> "lurker", "lurker_glow"));
        e.registerEntityRenderer(ModEntities.CAVE_THIEF.get(), c -> new Renderers.Human<>(c, goblin, 0.68F, 0.66F, f -> "cave_thief", false));
        e.registerEntityRenderer(ModEntities.FIRE_DEMON.get(), c -> new Renderers.Geo<>(c, layer("geo_demon"), "demon", 2.2F, 1.2F, f -> "demon", "demon_glow"));
        e.registerEntityRenderer(ModEntities.ASH_LORD.get(), c -> new Renderers.Geo<>(c, layer("geo_ash_lord"), "ash_lord", 1.6F, 0.39F, f -> "ash_lord_geo", null));
        e.registerEntityRenderer(ModEntities.DRAGON.get(), c -> new Renderers.Geo<>(c, layer("geo_dragon"), "dragon", 2.5F, 1.0F, f -> "dragon", null));
        e.registerEntityRenderer(ModEntities.SORCERER.get(), c -> new Renderers.Human<>(c, plain, 1.05F, 1.08F, f -> "sorcerer", false));
        e.registerEntityRenderer(ModEntities.HUMAN.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.9375F, f -> peopleTex(f, "human", 4), false));
        e.registerEntityRenderer(ModEntities.ELF.get(), c -> new Renderers.Human<PeopleEntity>(c, elf, 0.94F, 1.05F, f -> peopleTex(f, "elf", 3), false));
        e.registerEntityRenderer(ModEntities.DWARF.get(), c -> new Renderers.Human<PeopleEntity>(c, dwarf, 1.1F, 0.74F, f -> peopleTex(f, "dwarf", 3), false));
        e.registerEntityRenderer(ModEntities.HALFLING.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.66F, 0.6F, f -> peopleTex(f, "halfling", 3), false));
        e.registerEntityRenderer(ModEntities.RANGER.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.9375F, f -> "ranger", false));
        e.registerEntityRenderer(ModEntities.MAGE.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.97F, f -> "mage", false));
        e.registerEntityRenderer(ModEntities.TREEMAN.get(), SylvanRender.TreemanRenderer::new);
        e.registerEntityRenderer(ModEntities.EAGLE.get(), c -> new Renderers.Geo<>(c, layer("geo_eagle"), "eagle", 1.0F, 1.3F, f -> "eagle", null));
        e.registerEntityRenderer(ModEntities.STAG.get(), c -> new Renderers.Geo<>(c, layer("geo_stag"), "stag", 0.6F, 1.0F, f -> "stag", null));
        e.registerEntityRenderer(ModEntities.CROW.get(), c -> new Renderers.Geo<>(c, layer("geo_eagle"), "eagle", 0.25F, 0.35F, f -> "crow", null));
    }
}
