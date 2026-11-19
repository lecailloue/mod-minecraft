package net.ringsofash.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
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
    public static final KeyMapping DESCEND_KEY = new KeyMapping("key.ringsofash.descend", InputConstants.KEY_C, "key.categories.ringsofash");
    public static final KeyMapping SKILLS_KEY = new KeyMapping("key.ringsofash.skills", InputConstants.KEY_P, "key.categories.ringsofash");
    public static final KeyMapping SPECIAL_KEY = new KeyMapping("key.ringsofash.special", InputConstants.KEY_V, "key.categories.ringsofash");
    public static final KeyMapping MAP_KEY = new KeyMapping("key.ringsofash.map", InputConstants.KEY_M, "key.categories.ringsofash");
    private static final String[] GEO = {"troll", "troll_chief", "warg", "stag", "demon", "lurker", "treeman", "eagle", "crow", "war_boar", "giant_arrow", "dragon", "mammoth", "ash_lord", "orc_1_nu", "orc_2_fourrure", "orc_3_mailles", "orc_4_casque", "orc_noir_1_main", "orc_noir_2_fer", "orc_chef", "spectre_epee", "spectre_lance", "spectre_hache", "spectre_dague", "spectre_roi", "ombre_1", "ombre_2", "arme_orc_cleaver", "arme_orc_scimitar", "arme_orc_machete", "arme_orc_spear", "arme_orc_mace", "gobelin_1", "gobelin_2", "gobelin_3", "gobelin_roi", "arme_goblin_king_staff", "arme_goblin_pickaxe", "giant_spider", "sylvestre_ancien", "sylvestre_saule", "shadow_dragon", "shadow_chief", "arme_lunargent_sword", "arme_elven_blade", "arme_elven_glaive", "arme_frost_blade", "arme_blood_blade", "arme_viper_dagger", "arme_storm_blade", "arme_sentinel_blade", "arme_sun_blade", "arme_halfling_dagger", "arme_ranger_sword", "arme_wight_blade", "arme_kings_blade", "arme_valmoire_blade", "arme_dwarven_axe", "arme_dwarven_hammer", "arme_ash_mace", "arme_rider_spear", "arme_troll_club", "arme_lunargent_pickaxe", "arme_elven_bow", "arme_staff_fire", "arme_staff_light", "arme_staff_storm", "arme_staff_healing"};
    private static final String[] HUMAN = {"plain", "elf", "orc", "goblin", "dwarf"};

    public static ModelLayerLocation layer(String name) { return new ModelLayerLocation(RingsOfAsh.id(name), "main"); }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::layers);
        modBus.addListener(ClientSetup::renderers);
        Atmosphere.init(modBus);
        SwordAura.init();
        BowDraw.init();
        SylvanRender.init();
        EyeOfSauronSky.init();
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
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e) -> e.registerSpriteSet(net.ringsofash.fluid.ModFluids.ICHOR_SMOKE.get(), IchorSmokeParticle.Provider::new));
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
        e.register(net.ringsofash.menu.ModMenus.ARTIFACTS.get(), ArtifactScreen::new);
        e.register(net.ringsofash.menu.ModMenus.FURNACE.get(), PocketFurnaceScreen::new);
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

    private static void keys(RegisterKeyMappingsEvent e) { e.register(REP_KEY); e.register(MAP_KEY); e.register(RINGS_KEY); e.register(DESCEND_KEY); e.register(SKILLS_KEY); e.register(SPECIAL_KEY); }

    private static boolean jumpSent;

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (MAP_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new MapScreen());
        }
        while (SPECIAL_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ringsofash.data.Net.SpecialAttack());
        }
        while (SKILLS_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new SkillScreen());
        }
        while (RINGS_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ringsofash.data.Net.OpenRings());
        }
        boolean jump = mc.player != null && mc.screen == null && mc.options.keyJump.isDown();
        if (jump != jumpSent) { jumpSent = jump; net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ringsofash.data.Net.ArtifactJump(jump)); }
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
        e.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return RingsOfAsh.id("block/morgul_ichor_still"); }
            @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return RingsOfAsh.id("block/morgul_ichor_flow"); }
        }, net.ringsofash.fluid.ModFluids.ICHOR_TYPE.get());
        e.registerItem(armorExt("orc", "head"), Reg.ORC_HELMET.get());
        e.registerItem(armorExt("orc", "chest"), Reg.ORC_CHESTPLATE.get());
        e.registerItem(armorExt("orc", "legs"), Reg.ORC_LEGGINGS.get());
        e.registerItem(armorExt("orc", "boots"), Reg.ORC_BOOTS.get());
        e.registerItem(armorExt("dwarven", "head"), Reg.DWARVEN_HELMET.get());
        e.registerItem(armorExt("dwarven", "chest"), Reg.DWARVEN_CHESTPLATE.get());
        e.registerItem(armorExt("dwarven", "legs"), Reg.DWARVEN_LEGGINGS.get());
        e.registerItem(armorExt("dwarven", "boots"), Reg.DWARVEN_BOOTS.get());
        e.registerItem(armorExt("elven", "head"), Reg.ELVEN_HELMET.get());
        e.registerItem(armorExt("elven", "chest"), Reg.ELVEN_CHESTPLATE.get());
        e.registerItem(armorExt("elven", "legs"), Reg.ELVEN_LEGGINGS.get());
        e.registerItem(armorExt("elven", "boots"), Reg.ELVEN_BOOTS.get());
        e.registerItem(armorExt("gondor", "head"), Reg.GONDOR_HELMET.get());
        e.registerItem(armorExt("gondor", "chest"), Reg.GONDOR_CHESTPLATE.get());
        e.registerItem(armorExt("gondor", "legs"), Reg.GONDOR_LEGGINGS.get());
        e.registerItem(armorExt("gondor", "boots"), Reg.GONDOR_BOOTS.get());
        e.registerItem(armorExt("elfking", "head"), Reg.ELFKING_HELMET.get());
        e.registerItem(armorExt("elfking", "chest"), Reg.ELFKING_CHESTPLATE.get());
        e.registerItem(armorExt("elfking", "legs"), Reg.ELFKING_LEGGINGS.get());
        e.registerItem(armorExt("elfking", "boots"), Reg.ELFKING_BOOTS.get());
        e.registerItem(armorExt("orc_rusted", "head"), Reg.ORC_RUSTED_HELMET.get());
        e.registerItem(armorExt("orc_rusted", "chest"), Reg.ORC_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("orc_rusted", "legs"), Reg.ORC_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("orc_rusted", "boots"), Reg.ORC_RUSTED_BOOTS.get());
        e.registerItem(armorExt("dwarven_rusted", "head"), Reg.DWARVEN_RUSTED_HELMET.get());
        e.registerItem(armorExt("dwarven_rusted", "chest"), Reg.DWARVEN_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("dwarven_rusted", "legs"), Reg.DWARVEN_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("dwarven_rusted", "boots"), Reg.DWARVEN_RUSTED_BOOTS.get());
        e.registerItem(armorExt("elven_rusted", "head"), Reg.ELVEN_RUSTED_HELMET.get());
        e.registerItem(armorExt("elven_rusted", "chest"), Reg.ELVEN_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("elven_rusted", "legs"), Reg.ELVEN_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("elven_rusted", "boots"), Reg.ELVEN_RUSTED_BOOTS.get());
        e.registerItem(armorExt("rohan_rusted", "head"), Reg.ROHAN_RUSTED_HELMET.get());
        e.registerItem(armorExt("rohan_rusted", "chest"), Reg.ROHAN_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("rohan_rusted", "legs"), Reg.ROHAN_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("rohan_rusted", "boots"), Reg.ROHAN_RUSTED_BOOTS.get());
        e.registerItem(armorExt("gondor_rusted", "head"), Reg.GONDOR_RUSTED_HELMET.get());
        e.registerItem(armorExt("gondor_rusted", "chest"), Reg.GONDOR_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("gondor_rusted", "legs"), Reg.GONDOR_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("gondor_rusted", "boots"), Reg.GONDOR_RUSTED_BOOTS.get());
        e.registerItem(armorExt("elfking_rusted", "head"), Reg.ELFKING_RUSTED_HELMET.get());
        e.registerItem(armorExt("elfking_rusted", "chest"), Reg.ELFKING_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("elfking_rusted", "legs"), Reg.ELFKING_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("elfking_rusted", "boots"), Reg.ELFKING_RUSTED_BOOTS.get());
        e.registerItem(armorExt("knight_rusted", "head"), Reg.KNIGHT_RUSTED_HELMET.get());
        e.registerItem(armorExt("knight_rusted", "chest"), Reg.KNIGHT_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("knight_rusted", "legs"), Reg.KNIGHT_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("knight_rusted", "boots"), Reg.KNIGHT_RUSTED_BOOTS.get());
        e.registerItem(armorExt("knight", "head"), Reg.KNIGHT_HELMET.get());
        e.registerItem(armorExt("knight", "chest"), Reg.KNIGHT_CHESTPLATE.get());
        e.registerItem(armorExt("knight", "legs"), Reg.KNIGHT_LEGGINGS.get());
        e.registerItem(armorExt("knight", "boots"), Reg.KNIGHT_BOOTS.get());
        e.registerItem(armorExt("nazgul", "head"), Reg.NAZGUL_HELMET.get());
        e.registerItem(armorExt("nazgul", "chest"), Reg.NAZGUL_CHESTPLATE.get());
        e.registerItem(armorExt("nazgul", "legs"), Reg.NAZGUL_LEGGINGS.get());
        e.registerItem(armorExt("nazgul", "boots"), Reg.NAZGUL_BOOTS.get());
        e.registerItem(armorExt("mithril", "head"), Reg.MITHRIL_HELMET.get());
        e.registerItem(armorExt("mithril", "chest"), Reg.MITHRIL_CHESTPLATE.get());
        e.registerItem(armorExt("mithril", "legs"), Reg.MITHRIL_LEGGINGS.get());
        e.registerItem(armorExt("mithril", "boots"), Reg.MITHRIL_BOOTS.get());
        e.registerItem(armorExt("dwarfking", "head"), Reg.DWARFKING_HELMET.get());
        e.registerItem(armorExt("dwarfking", "chest"), Reg.DWARFKING_CHESTPLATE.get());
        e.registerItem(armorExt("dwarfking", "legs"), Reg.DWARFKING_LEGGINGS.get());
        e.registerItem(armorExt("dwarfking", "boots"), Reg.DWARFKING_BOOTS.get());
        e.registerItem(armorExt("dwarfking_rusted", "head"), Reg.DWARFKING_RUSTED_HELMET.get());
        e.registerItem(armorExt("dwarfking_rusted", "chest"), Reg.DWARFKING_RUSTED_CHESTPLATE.get());
        e.registerItem(armorExt("dwarfking_rusted", "legs"), Reg.DWARFKING_RUSTED_LEGGINGS.get());
        e.registerItem(armorExt("dwarfking_rusted", "boots"), Reg.DWARFKING_RUSTED_BOOTS.get());
        e.registerItem(armorExt("feet", "boots"), Reg.FEET_BOOTS.get());
        e.registerItem(armorExt("rohan", "head"), Reg.ROHAN_HELMET.get());
        e.registerItem(armorExt("rohan", "chest"), Reg.ROHAN_CHESTPLATE.get());
        e.registerItem(armorExt("rohan", "legs"), Reg.ROHAN_LEGGINGS.get());
        e.registerItem(armorExt("rohan", "boots"), Reg.ROHAN_BOOTS.get());
        e.registerItem(ext, Reg.ORC_CLEAVER.get(), Reg.ORC_SCIMITAR.get(), Reg.ORC_MACHETE.get(), Reg.ORC_SPEAR.get(), Reg.ORC_MACE.get(), Reg.GOBLIN_KING_STAFF.get(), Reg.GOBLIN_PICKAXE.get(),
                Reg.LUNARGENT_SWORD.get(), Reg.ELVEN_BLADE.get(), Reg.ELVEN_GLAIVE.get(), Reg.FROST_BLADE.get(), Reg.BLOOD_BLADE.get(), Reg.VIPER_DAGGER.get(), Reg.STORM_BLADE.get(), Reg.SENTINEL_BLADE.get(), Reg.SUN_BLADE.get(), Reg.HALFLING_DAGGER.get(), Reg.RANGER_SWORD.get(), Reg.WIGHT_BLADE.get(), Reg.KINGS_BLADE.get(), Reg.VALMOIRE_BLADE.get(), Reg.DWARVEN_AXE.get(), Reg.DWARVEN_HAMMER.get(), Reg.ASH_MACE.get(), Reg.RIDER_SPEAR.get(), Reg.TROLL_CLUB.get(), Reg.LUNARGENT_PICKAXE.get(), Reg.ELVEN_BOW.get(), Reg.STAFF_FIRE.get(), Reg.STAFF_LIGHT.get(), Reg.STAFF_STORM.get(), Reg.STAFF_HEALING.get());
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

    static boolean orcNear(LivingEntity holder) {
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
    static boolean near(int id, LivingEntity holder, java.util.function.Predicate<LivingEntity> filter) {
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
            case "elf", "dwarf" -> {
                PeopleParts.add(kind, mesh.getRoot());
                return LayerDefinition.create(mesh, 128, 256);
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
        for (String set : ARMOR_SETS) for (String pc : ARMOR_PIECES) {
            String n = "armor_" + set + "_" + pc;
            e.registerLayerDefinition(layer(n), () -> GeoDef.layer(n));
        }
    }

    // ---- armures 3D (orc, nain, elfe) : un modele par piece, pose copiee depuis le modele d'origine
    private static final String[] ARMOR_SETS = {"orc", "dwarven", "elven", "rohan", "gondor", "elfking", "orc_rusted", "dwarven_rusted", "elven_rusted", "rohan_rusted", "gondor_rusted", "elfking_rusted", "dwarfking", "dwarfking_rusted", "knight", "knight_rusted", "nazgul", "mithril", "feet"};
    private static final String[] ARMOR_PIECES = {"head", "chest", "legs", "boots"};
    private static final java.util.Map<String, HumanoidModel<LivingEntity>> ARMOR_MODELS = new java.util.HashMap<>();

    private static net.minecraft.client.model.geom.ModelPart child(net.minecraft.client.model.geom.ModelPart p, String n) { return p.hasChild(n) ? p.getChild(n) : null; }

    /** Animations discretes : la cape ondule quand on marche, les ailes du casque de Gondor bougent tres legerement. */
    private static void animate(HumanoidModel<LivingEntity> m, LivingEntity e, String set) {
        double t = System.nanoTime() / 1.0e9 + e.getId() * 1.7;
        float amp = set.endsWith("_rusted") ? 0.6F : 1F;
        float sp = net.minecraft.util.Mth.clamp(e.walkAnimation.speed(), 0F, 1F);
        var cape = child(m.body, "cape");
        if (cape != null) {
            cape.resetPose();
            cape.xRot += amp * sp * (0.22F + (float) Math.sin(t * 5.0) * 0.06F);          // se souleve et ondule a la marche
            cape.zRot += amp * sp * (float) Math.sin(t * 3.3) * 0.04F;
        }
        float flap = (float) Math.sin(t * 2.0) * 0.045F * amp;                           // battement tres leger
        var wr = child(m.head, "wing"); if (wr != null) { wr.resetPose(); wr.zRot += flap; }
        var wl = child(m.head, "wing_l"); if (wl != null) { wl.resetPose(); wl.zRot -= flap; }
    }

    private static net.neoforged.neoforge.client.extensions.common.IClientItemExtensions armorExt(String set, String piece) {
        return new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot, HumanoidModel<?> original) {
                HumanoidModel<LivingEntity> m = ARMOR_MODELS.computeIfAbsent(set + "_" + piece,
                        k -> new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(layer("armor_" + set + "_" + piece))));
                m.head.copyFrom(original.head); m.hat.copyFrom(original.hat); m.body.copyFrom(original.body);
                m.rightArm.copyFrom(original.rightArm); m.leftArm.copyFrom(original.leftArm);
                m.rightLeg.copyFrom(original.rightLeg); m.leftLeg.copyFrom(original.leftLeg);
                m.head.visible = original.head.visible; m.hat.visible = original.hat.visible; m.body.visible = original.body.visible;
                m.rightArm.visible = original.rightArm.visible; m.leftArm.visible = original.leftArm.visible;
                m.rightLeg.visible = original.rightLeg.visible; m.leftLeg.visible = original.leftLeg.visible;
                animate(m, living, set);
                return m;
            }
        };
    }

    private static String peopleTex(PeopleEntity e, String base, int n) {
        if (e.role() == PeopleEntity.GUARD) return base + "_guard";
        if (e.role() == PeopleEntity.ELDER && base.equals("halfling")) return "halfling_elder";
        return base + "_" + (1 + Math.floorMod(e.variant(), n));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.EYE_OF_SAURON.get(), EyeOfSauronRenderer::new);
        e.registerEntityRenderer(ModEntities.GIANT_ARROW.get(), GiantArrowRenderer::new);
        e.registerEntityRenderer(ModEntities.INVISIBLE_STAND.get(), net.minecraft.client.renderer.entity.ArmorStandRenderer::new);
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
        e.registerEntityRenderer(ModEntities.WARG.get(), c -> new Renderers.Geo<FoeEntity>(c, layer("geo_warg"), "warg", 0.8F, 1.15F, f -> "warg", "warg_glow"));
        e.registerEntityRenderer(ModEntities.MAMMOTH.get(), c -> new Renderers.Geo<net.ringsofash.entity.MountEntity>(c, layer("geo_mammoth"), "mammoth", 2.2F, 0.62F, f -> "mammoth", null));
        e.registerEntityRenderer(ModEntities.WAR_BOAR.get(), c -> new Renderers.Geo<net.ringsofash.entity.MountEntity>(c, layer("geo_war_boar"), "war_boar", 1.1F, 0.65F, f -> "war_boar", null));
        e.registerEntityRenderer(ModEntities.SHADOW_RIDER.get(), Renderers.Shadow::new);
        e.registerEntityRenderer(ModEntities.SHADOW_DRAGON.get(), Renderers.ShadowDragon::new);
        e.registerEntityRenderer(ModEntities.SHADOW_CHIEF.get(), Renderers.ShadowChief::new);
        e.registerEntityRenderer(ModEntities.GIANT_SPIDER.get(), c -> new Renderers.Geo<net.ringsofash.entity.GiantSpiderEntity>(c, layer("geo_giant_spider"), "giant_spider", 2.2F,
                2.5F * 16F / GeoDef.get("giant_spider").height, f -> "giant_spider", "giant_spider_glow"));
        e.registerEntityRenderer(ModEntities.LURKER.get(), c -> new Renderers.Geo<>(c, layer("geo_lurker"), "lurker", 1.5F, 1.6F, f -> "lurker", "lurker_glow"));
        e.registerEntityRenderer(ModEntities.CAVE_THIEF.get(), c -> new Renderers.Human<>(c, goblin, 0.68F, 0.66F, f -> "cave_thief", false));
        e.registerEntityRenderer(ModEntities.FIRE_DEMON.get(), c -> new Renderers.Geo<>(c, layer("geo_demon"), "demon", 2.2F, 1.2F, f -> "demon", "demon_glow"));
        e.registerEntityRenderer(ModEntities.ASH_LORD.get(), c -> new Renderers.Geo<>(c, layer("geo_ash_lord"), "ash_lord", 1.6F, 0.39F, f -> "ash_lord_geo", null));
        e.registerEntityRenderer(ModEntities.DRAGON.get(), c -> new Renderers.Geo<>(c, layer("geo_dragon"), "dragon", 2.5F, 1.0F, f -> "dragon", null));
        e.registerEntityRenderer(ModEntities.SORCERER.get(), c -> new Renderers.Human<>(c, plain, 1.05F, 1.08F, f -> "sorcerer", false));
        e.registerEntityRenderer(ModEntities.HUMAN.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.9375F, f -> peopleTex(f, "human", 4), false));
        e.registerEntityRenderer(ModEntities.ELF.get(), c -> new Renderers.Human<PeopleEntity>(c, elf, 0.94F, 1.14F, f -> peopleTex(f, "elf", 20), false));
        e.registerEntityRenderer(ModEntities.DWARF.get(), c -> new Renderers.Human<PeopleEntity>(c, dwarf, 1.1F, 0.78F, f -> peopleTex(f, "dwarf", 40), false));
        e.registerEntityRenderer(ModEntities.HALFLING.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.66F, 0.6F, f -> peopleTex(f, "halfling", 3), false));
        e.registerEntityRenderer(ModEntities.RANGER.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.9375F, f -> "ranger", false));
        e.registerEntityRenderer(ModEntities.MAGE.get(), c -> new Renderers.Human<PeopleEntity>(c, plain, 0.9375F, 0.97F, f -> "mage", false));
        e.registerEntityRenderer(ModEntities.TREEMAN.get(), SylvanRender.TreemanRenderer::new);
        e.registerEntityRenderer(ModEntities.EAGLE.get(), c -> new Renderers.Geo<>(c, layer("geo_eagle"), "eagle", 1.0F, 0.9F, f -> "eagle", null));
        e.registerEntityRenderer(ModEntities.STAG.get(), c -> new Renderers.Geo<net.ringsofash.entity.MountEntity>(c, layer("geo_stag"), "stag", 0.8F, 0.7F, f -> "stag", null));
        e.registerEntityRenderer(ModEntities.CROW.get(), c -> new Renderers.Geo<>(c, layer("geo_crow"), "crow", 0.25F, 0.5F, f -> "crow", null));
    }
}
