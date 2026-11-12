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
    public static final KeyMapping MAP_KEY = new KeyMapping("key.ringsofash.map", InputConstants.KEY_M, "key.categories.ringsofash");
    private static final String[] GEO = {"troll", "warg", "stag", "demon", "lurker", "treeman", "eagle", "dragon", "mammoth"};
    private static final String[] HUMAN = {"plain", "elf", "orc", "goblin", "dwarf"};

    private static ModelLayerLocation layer(String name) { return new ModelLayerLocation(RingsOfAsh.id(name), "main"); }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::layers);
        modBus.addListener(ClientSetup::renderers);
        modBus.addListener(ClientSetup::keys);
        modBus.addListener(ClientSetup::gui);
        modBus.addListener(ClientSetup::setup);
        NeoForge.EVENT_BUS.addListener(ClientSetup::tick);
        if (System.getProperty("roa.clienttest") != null) ClientTest.init();
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
        });
    }

    private static void keys(RegisterKeyMappingsEvent e) { e.register(REP_KEY); e.register(MAP_KEY); }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (MAP_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new MapScreen());
        }
        while (REP_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new RepScreen());
        }
    }

    private static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // la lame elfique luit quand des orques sont proches
            ItemProperties.register(Reg.ELVEN_BLADE.get(), RingsOfAsh.id("glow"), (stack, level, entity, seed) -> orcNear(entity) ? 1.0F : 0.0F);
            ItemProperties.register(Reg.ELVEN_GLAIVE.get(), RingsOfAsh.id("glow"), (stack, level, entity, seed) -> orcNear(entity) ? 1.0F : 0.0F);
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

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
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
        e.registerEntityRenderer(ModEntities.ORC.get(), c -> new Renderers.Human<FoeEntity>(c, orc, 1.0F, 1.0F, f -> "orc_" + (1 + Math.floorMod(f.variant(), 3)), false));
        e.registerEntityRenderer(ModEntities.WAR_ORC.get(), c -> new Renderers.Human<FoeEntity>(c, orc, 1.1F, 1.1F, f -> "war_orc", false));
        e.registerEntityRenderer(ModEntities.ORC_CHIEF.get(), c -> new Renderers.Human<FoeEntity>(c, orc, 1.3F, 1.28F, f -> "orc_chief", false));
        e.registerEntityRenderer(ModEntities.GOBLIN.get(), c -> new Renderers.Human<FoeEntity>(c, goblin, 0.62F, 0.62F, f -> "goblin_" + (1 + Math.floorMod(f.variant(), 2)), false));
        e.registerEntityRenderer(ModEntities.WIGHT.get(), c -> new Renderers.Human<FoeEntity>(c, plain, 1.0F, 1.05F, f -> "wight", true));
        e.registerEntityRenderer(ModEntities.WILD_MAN.get(), c -> new Renderers.Human<FoeEntity>(c, plain, 1.0F, 1.0F, f -> "wild_man", false));
        e.registerEntityRenderer(ModEntities.DEAD_SOLDIER.get(), c -> new Renderers.Human<FoeEntity>(c, plain, 1.0F, 1.0F, f -> "dead_soldier", true));
        e.registerEntityRenderer(ModEntities.CAVE_TROLL.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_troll"), "troll", 1.2F, 1.0F, f -> "troll", null));
        e.registerEntityRenderer(ModEntities.STONE_TROLL.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_troll"), "troll", 1.2F, 1.0F, f -> f.isStone() ? "troll_statue" : "troll_stone", null));
        e.registerEntityRenderer(ModEntities.WARG.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_warg"), "warg", 0.8F, 1.15F, f -> "warg", null));
        e.registerEntityRenderer(ModEntities.MAMMOTH.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_mammoth"), "mammoth", 1.8F, 1.0F, f -> "mammoth", null));
        e.registerEntityRenderer(ModEntities.PALE_RIDER.get(), c -> new Renderers.Human<>(c, plain, 1.1F, 1.15F, f -> "pale_rider", false));
        e.registerEntityRenderer(ModEntities.GIANT_SPIDER.get(), Renderers.GiantSpider::new);
        e.registerEntityRenderer(ModEntities.LURKER.get(), c -> new Renderers.Geo<>(c, layer("geo_lurker"), "lurker", 1.5F, 1.6F, f -> "lurker", "lurker_glow"));
        e.registerEntityRenderer(ModEntities.CAVE_THIEF.get(), c -> new Renderers.Human<>(c, goblin, 0.68F, 0.66F, f -> "cave_thief", false));
        e.registerEntityRenderer(ModEntities.FIRE_DEMON.get(), c -> new Renderers.Geo<>(c, layer("geo_demon"), "demon", 1.6F, 1.0F, f -> "demon", "demon_glow"));
        e.registerEntityRenderer(ModEntities.ASH_LORD.get(), c -> new Renderers.Human<>(c, plain, 1.95F, 2.0F, f -> "ash_lord", false));
        e.registerEntityRenderer(ModEntities.DRAGON.get(), c -> new Renderers.Geo<>(c, layer("geo_dragon"), "dragon", 2.5F, 1.0F, f -> "dragon", null));
        e.registerEntityRenderer(ModEntities.SORCERER.get(), c -> new Renderers.Human<>(c, plain, 1.05F, 1.08F, f -> "sorcerer", false));
        e.registerEntityRenderer(ModEntities.HUMAN.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.9375F, f -> peopleTex(f, "human", 4), false));
        e.registerEntityRenderer(ModEntities.ELF.get(), c -> new Renderers.Human<PeopleEntity>(c, elf, 0.94F, 1.05F, f -> peopleTex(f, "elf", 3), false));
        e.registerEntityRenderer(ModEntities.DWARF.get(), c -> new Renderers.Human<PeopleEntity>(c, dwarf, 1.1F, 0.74F, f -> peopleTex(f, "dwarf", 3), false));
        e.registerEntityRenderer(ModEntities.HALFLING.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.66F, 0.6F, f -> peopleTex(f, "halfling", 3), false));
        e.registerEntityRenderer(ModEntities.RANGER.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.9375F, f -> "ranger", false));
        e.registerEntityRenderer(ModEntities.MAGE.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.97F, f -> "mage", false));
        e.registerEntityRenderer(ModEntities.TREEMAN.get(), c -> new Renderers.Geo<>(c, layer("geo_treeman"), "treeman", 1.2F, 1.0F, f -> "treeman", null));
        e.registerEntityRenderer(ModEntities.EAGLE.get(), c -> new Renderers.Geo<>(c, layer("geo_eagle"), "eagle", 1.0F, 1.3F, f -> "eagle", null));
        e.registerEntityRenderer(ModEntities.STAG.get(), c -> new Renderers.Geo<>(c, layer("geo_stag"), "stag", 0.6F, 1.0F, f -> "stag", null));
        e.registerEntityRenderer(ModEntities.CROW.get(), c -> new Renderers.Geo<>(c, layer("geo_eagle"), "eagle", 0.25F, 0.35F, f -> "crow", null));
    }
}
