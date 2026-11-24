package net.ringsofash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ringsofash.GameEvents;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.data.ModData;
import net.ringsofash.entity.FoeEntity;
import net.ringsofash.entity.ShadowRiderEntity;
import net.ringsofash.entity.ModEntities;
import net.ringsofash.entity.PeopleEntity;
import net.ringsofash.gen.Reg;
import net.ringsofash.world.ModWorldgen;
import net.ringsofash.world.SelfTest;
import net.ringsofash.world.Settlement;
import net.ringsofash.world.Travel;
import net.ringsofash.world.VolcanoPiece;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Test automatique du client (-Droa.clienttest) : cree un monde, met en scene le contenu du mod et prend des captures d'ecran. */
public final class ClientTest {
    private ClientTest() {}

    private record Scene(String name, int ticks, Consumer<ServerPlayer> setup) {}
    private static final List<Scene> SCENES = new ArrayList<>();
    private static int state = 0, wait = 0, index = 0;
    private static final int SX = 0, SY = 200, SZ = 0;
    /** true : seulement les scenes d armures (iteration rapide) */
    private static final boolean ARMOR_ONLY = false;
    /** si non vide : seules les scenes dont le nom commence par l un de ces prefixes sont jouees (iteration rapide sur les sites) */
    private static final String[] ONLY = {"zon", "cyp", "mini", "cro", "riv", "pon"};
    private static Runnable hook = null;
    private static BlockPos volcano = BlockPos.ZERO;
    private static int volcanoRim = 0;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientTest::tick);
    }

    private static void log(String s) { RingsOfAsh.LOGGER.info("[CLIENTTEST] " + s); }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        try {
            switch (state) {
                case 0 -> {
                    if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++wait > 60) {
                        wait = 0; state = 1;
                        log("creation du monde");
                        mc.options.renderDistance().set(12);
                        mc.createWorldOpenFlows().createFreshLevel("roa_test", new LevelSettings("roa_test", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(20261111L, true, false), WorldPresets::createNormalWorldDimensions, mc.screen);
                    } else if (!(mc.screen instanceof TitleScreen) && mc.screen != null && mc.getOverlay() == null && ++wait > 400) {
                        log("ecran inattendu au demarrage : " + mc.screen.getClass().getName());
                        mc.setScreen(new TitleScreen());
                        wait = 0;
                    }
                }
                case 1 -> {
                    if (mc.player != null && mc.level != null && mc.screen == null) {
                        if (++wait > 120) { wait = 0; state = 2; mc.options.hideGui = true; log("monde pret"); }
                    } else if (++wait > 6000) throw new IllegalStateException("le monde ne s'est pas charge ; ecran=" + (mc.screen == null ? "aucun" : mc.screen.getClass().getName()));
                }
                case 2 -> {
                    if (SCENES.isEmpty()) {
                        scenes();
                        if (ONLY.length > 0) SCENES.removeIf(sc -> { for (String pf : ONLY) if (sc.name().startsWith(pf)) return false; return true; });
                    }
                    if (index >= SCENES.size()) { state = 9; break; }
                    Scene s = SCENES.get(index);
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server == null) throw new IllegalStateException("pas de serveur integre");
                    log("scene " + s.name);
                    if (mc.screen != null) mc.setScreen(null);
                    if (s.name.contains("reputation")) { mc.options.hideGui = false; mc.setScreen(new QuestScreen(1)); }
                    if (s.name.contains("quetes")) { mc.options.hideGui = false; QuestScreen.tracked = 1; mc.setScreen(new QuestScreen(0)); }
                    if (s.name.contains("carte")) {
                        mc.options.hideGui = false;
                        if (MapScreen.markers().isEmpty()) {
                            MapScreen.addMarker(-445, -230, "Refuge", 0x3060E0); MapScreen.addMarker(250, -297, "", 0xE03030);
                            MapScreen.addMarker(-400, -150, "Tour du guet", 0x40C040);
                        }
                        mc.setScreen(new MapScreen(s.name.contains("legende"), s.name.contains("legende") ? 1 : 2.5, s.name.contains("legende"), s.name.contains("edition") ? 2 : -1));
                    }
                    if (s.name.contains("boussole")) {
                        mc.options.hideGui = false;
                        MapScreen.addMarker(-410, -160, "Tour du guet", 0x40C040); MapScreen.addMarker(-485, -170, "Camp", 0xF08A20);
                        MapScreen.addMarker(-440, -80, "Mine", 0xA040E0); MapScreen.addMarker(-700, -230, "Loin", 0xE03030);
                    }
                    RingWheel.keepOpen = false;
                    if (s.name.contains("onglet")) { mc.options.hideGui = false; mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); }
                    if (s.name.contains("roue")) { mc.options.hideGui = false; RingWheel.keepOpen = true; mc.setScreen(new RingWheel()); }
                    if (s.name.contains("anneaux") || s.name.contains("artefacts")) mc.options.hideGui = false;
                    if (s.name.contains("suivi")) { mc.options.hideGui = false; QuestScreen.tracked = 1; }
                    server.execute(() -> {
                        try { s.setup.accept(server.getPlayerList().getPlayers().get(0)); }
                        catch (Throwable t) { RingsOfAsh.LOGGER.error("[CLIENTTEST] ECHEC scene " + s.name, t); }
                    });
                    if (s.name.contains("_1p")) mc.options.hideGui = false;
                    mc.options.keyUse.setDown(s.name.contains("arc_bande"));            // clic droit maintenu : l'arc reste bande
                    if (s.name.contains("arme_face")) mc.options.hideGui = true;      // F1 cache aussi la main
                    if (s.name.contains("coup_face")) mc.options.hideGui = true;
                    mc.options.setCameraType(s.name.contains("monte") ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : (s.name.contains("face")) ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT : net.minecraft.client.CameraType.FIRST_PERSON);
                    wait = s.ticks; state = 3;
                }
                case 3 -> {
                    if (--wait <= 0) {
                        if (hook != null && mc.getSingleplayerServer() != null) { Runnable h = hook; hook = null; mc.getSingleplayerServer().execute(h); }
                        Screenshot.grab(mc.gameDirectory, SCENES.get(index).name + ".png", mc.getMainRenderTarget(), c -> { });
                        index++; state = 2;
                    }
                }
                default -> {
                    if (++wait > 40) { log("TERMINE"); mc.stop(); }
                }
            }
        } catch (Throwable t) {
            RingsOfAsh.LOGGER.error("[CLIENTTEST] EXCEPTION", t);
            mc.stop();
        }
    }

    // ------------------------------------------------------------------ outils de mise en scene (cote serveur)
    /** premier bloc plein a partir de y0 avec 3 blocs d'air au-dessus : le sol ou l'on peut se tenir */
    private static int floorY(ServerLevel lv, int x, int z, int y0) {
        for (int y = y0; y < y0 + 90; y++) {
            if (!lv.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir()
                && lv.getBlockState(new net.minecraft.core.BlockPos(x, y + 1, z)).isAir()
                && lv.getBlockState(new net.minecraft.core.BlockPos(x, y + 2, z)).isAir()
                && lv.getBlockState(new net.minecraft.core.BlockPos(x, y + 3, z)).isAir()) return y + 1;
        }
        return y0 + 40;
    }

    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, double tx, double ty, double tz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z)), pitch = (float) -Math.toDegrees(Math.atan2(ty - y, Math.hypot(tx - x, tz - z)));
        view(p, level, x, y, z, yaw, pitch);
    }
    private static void view(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        EyeOfSauronSky.force = false;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void heldScene(ServerPlayer p, String id, boolean face) {
        ServerLevel ow = p.server.overworld();
        clear(ow);
        view(p, ow, SX + 0.5, SY, SZ + 0.5, 0F, 8F);
        p.getAbilities().flying = false; p.onUpdateAbilities();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(RingsOfAsh.id(id))));
    }

    private static void swingScene(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        clear(ow);
        view(p, ow, SX + 0.5, SY, SZ + 0.5, 0F, 12F);
        p.getAbilities().flying = false; p.onUpdateAbilities();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.AIR));
        p.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void clear(ServerLevel level) {
        net.ringsofash.entity.ShadowDragonEntity.VERTICAL = pl -> 0;
        List<Entity> all = new ArrayList<>();
        level.getAllEntities().forEach(all::add);
        for (Entity e : all) if (!(e instanceof Player)) e.discard();
    }

    private static <T extends Mob> T actor(ServerLevel level, EntityType<T> type, double x, double z, Consumer<T> setup) {
        T m = type.create(level);
        if (m == null) return null;
        m.moveTo(x, SY, z, 180F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
        m.setNoAi(true);
        m.setYHeadRot(180F); m.setYBodyRot(180F);
        m.setPersistenceRequired();
        if (setup != null) setup.accept(m);
        level.addFreshEntity(m);
        return m;
    }

    @SafeVarargs
    private static void row(ServerLevel level, double z, double spacing, EntityType<? extends Mob>... types) {
        double x = SX + (types.length - 1) * spacing / 2.0;
        for (EntityType<? extends Mob> t : types) { actor(level, t, x, z, null); x -= spacing; }
    }

    private static void lineup(String name, double dist, float pitch, double spacing, List<EntityType<? extends Mob>> types) {
        SCENES.add(new Scene(name, 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            double x = SX + (types.size() - 1) * spacing / 2.0;
            for (EntityType<? extends Mob> t : types) { actor(ow, t, x, SZ + dist, null); x -= spacing; }
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, pitch);
        }));
    }

    /** une rangee de semi-hommes (hobbits) : variantes imposees (0 a 29), de face */
    private static void halflingLine(String name, double dist, float pitch, double spacing, int[] vs) {
        SCENES.add(new Scene(name, 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            double x = SX + (vs.length - 1) * spacing / 2.0;
            for (int v : vs) { actor(ow, ModEntities.HALFLING.get(), x, SZ + dist, h -> h.testVariant(v)); x -= spacing; }
            view(p, ow, SX + 0.5, SY + 0.9, SZ, 0F, pitch);
        }));
    }

    /** 4 orques (variantes), 2 orques noirs, puis le chef ; variants = variantes imposees des orques (les 4 premiers) puis noirs. */
    private static void orcLine(String name, double dist, float pitch, double spacing, int[] v, boolean full) {
        SCENES.add(new Scene(name, 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            int n = full ? (v.length == 1 ? 1 : 7) : 7;
            double x = SX + (n - 1) * spacing / 2.0;
            for (int i = 0; i < n; i++) {
                final int k = i;
                if (v.length == 1) actor(ow, ModEntities.ORC_CHIEF.get(), x, SZ + dist, null);
                else if (i < 4) actor(ow, ModEntities.ORC.get(), x, SZ + dist, o -> o.testVariant(v[k]));
                else if (i < 6) actor(ow, ModEntities.WAR_ORC.get(), x, SZ + dist, o -> o.testVariant(v[k] % 4));
                else actor(ow, ModEntities.ORC_CHIEF.get(), x, SZ + dist, null);
                x -= spacing;
            }
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, pitch);
        }));
    }

    /** Les 6 soldats spectraux : ancien modele, veteran, hoplite, guerrier a cornes, rodeur, Roi des morts (seul le roi pour la vue rapprochee). */
    private static void spectreLine(String name, double dist, float pitch, double spacing) {
        SCENES.add(new Scene(name, 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            boolean close = name.contains("proche");
            int n = close ? 1 : 5;
            double x = SX + (n - 1) * spacing / 2.0;
            for (int i = 0; i < n; i++) {
                final int v = close ? 4 : i;
                actor(ow, ModEntities.DEAD_SOLDIER.get(), x, SZ + dist, o -> o.testVariant(v));
                x -= spacing;
            }
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, pitch);
        }));
    }

    private static void spot(String name, int x, int z, int up, float yaw, float pitch) {
        SCENES.add(new Scene(name, 320, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            lands.getChunk(x >> 4, z >> 4);
            int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            lands.setDayTime(6000);
            view(p, lands, x + 0.5, y + up, z + 0.5, yaw, pitch);
        }));
    }

    private static void locate(String name, TagKey<Structure> tag, Settlement.T inside) {
        SCENES.add(new Scene(name, 320, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            BlockPos pos = lands.findNearestMapStructure(tag, p.blockPosition(), 100, false);
            if (pos == null) { log("introuvable : " + name); return; }
            int cx = pos.getX() + 8, cz = pos.getZ() + 8;
            lands.getChunk(cx >> 4, cz >> 4);
            int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
            log(name + " en " + cx + "," + y + "," + cz);
            if (inside == null) { view(p, lands, cx - 26, y + 22, cz - 26, -45F, 30F); return; }
            for (StructureStart st : lands.getChunk(cx >> 4, cz >> 4).getAllStarts().values()) {
                for (StructurePiece piece : st.getPieces()) {
                    if (piece instanceof Settlement.Piece sp && sp.type == inside) {
                        var bb = sp.getBoundingBox();
                        int mx = (bb.minX() + bb.maxX()) / 2, mz = (bb.minZ() + bb.maxZ()) / 2;
                        float yaw = switch (sp.rot) { case 0 -> 0F; case 1 -> 90F; case 2 -> 180F; default -> -90F; };
                        double back = sp.type.d / 2.0 - 3;
                        double vx = mx + Math.sin(Math.toRadians(yaw)) * back, vz = mz - Math.cos(Math.toRadians(yaw)) * back;
                        view(p, lands, vx + 0.5, sp.oy + (sp.type == Settlement.T.LAIR ? 9 : 5), vz + 0.5, yaw, 12F);
                        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2000, 0, true, false));
                        return;
                    }
                }
            }
            log("morceau " + inside + " introuvable pour " + name);
        }));
    }

    /** une porte de maison du plan dans le cadre donne : {x, y, z, direction exterieure} */
    private static int[] findDoor(net.ringsofash.world.TirithPlan tp, int x0, int x1, int z0, int z1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) for (int y = 20; y < 130; y++) {
            int id = tp.get(x, y, z) & 255;
            if (id >= net.ringsofash.world.TirithPlan.DOOR && id < net.ringsofash.world.TirithPlan.DOOR + 8 && ((id - net.ringsofash.world.TirithPlan.DOOR) & 1) == 0) return new int[]{x, y, z, (id - net.ringsofash.world.TirithPlan.DOOR) / 2};
        }
        return null;
    }

    /** Armures 3D en premier : la serie de captures peut etre coupee par le delai du CI. */
    private static void armourScenes() {
        SCENES.add(new Scene("05_armures_decor", 60, p -> {
            MinecraftServer sv = p.server;
            ServerLevel ow = sv.overworld();
            sv.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, sv);
            sv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, sv);
            sv.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, sv);
            ow.setDayTime(6000);
            ow.setWeatherParameters(100000, 0, false, false);
            for (int x = -8; x <= 8; x++) for (int z = -4; z <= 12; z++) {
                ow.setBlock(new BlockPos(SX + x, SY - 1, SZ + z), ((x + z) & 1) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 8; y++) ow.setBlock(new BlockPos(SX + x, SY + y, SZ + z), z == 8 ? Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
            clear(ow);
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, 0F);
        }));
        Item[][] close = {
                {Reg.ORC_HELMET.get(), Reg.ORC_CHESTPLATE.get(), Reg.ORC_LEGGINGS.get(), Reg.ORC_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.DWARVEN_HELMET.get(), Reg.DWARVEN_CHESTPLATE.get(), Reg.DWARVEN_LEGGINGS.get(), Reg.DWARVEN_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ELVEN_HELMET.get(), Reg.ELVEN_CHESTPLATE.get(), Reg.ELVEN_LEGGINGS.get(), Reg.ELVEN_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ROHAN_HELMET.get(), Reg.ROHAN_CHESTPLATE.get(), Reg.ROHAN_LEGGINGS.get(), Reg.ROHAN_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.GONDOR_HELMET.get(), Reg.GONDOR_CHESTPLATE.get(), Reg.GONDOR_LEGGINGS.get(), Reg.GONDOR_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ELFKING_HELMET.get(), Reg.ELFKING_CHESTPLATE.get(), Reg.ELFKING_LEGGINGS.get(), Reg.ELFKING_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ORC_RUSTED_HELMET.get(), Reg.ORC_RUSTED_CHESTPLATE.get(), Reg.ORC_RUSTED_LEGGINGS.get(), Reg.ORC_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.DWARVEN_RUSTED_HELMET.get(), Reg.DWARVEN_RUSTED_CHESTPLATE.get(), Reg.DWARVEN_RUSTED_LEGGINGS.get(), Reg.DWARVEN_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ELVEN_RUSTED_HELMET.get(), Reg.ELVEN_RUSTED_CHESTPLATE.get(), Reg.ELVEN_RUSTED_LEGGINGS.get(), Reg.ELVEN_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ROHAN_RUSTED_HELMET.get(), Reg.ROHAN_RUSTED_CHESTPLATE.get(), Reg.ROHAN_RUSTED_LEGGINGS.get(), Reg.ROHAN_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.GONDOR_RUSTED_HELMET.get(), Reg.GONDOR_RUSTED_CHESTPLATE.get(), Reg.GONDOR_RUSTED_LEGGINGS.get(), Reg.GONDOR_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.ELFKING_RUSTED_HELMET.get(), Reg.ELFKING_RUSTED_CHESTPLATE.get(), Reg.ELFKING_RUSTED_LEGGINGS.get(), Reg.ELFKING_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.DWARFKING_HELMET.get(), Reg.DWARFKING_CHESTPLATE.get(), Reg.DWARFKING_LEGGINGS.get(), Reg.DWARFKING_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.DWARFKING_RUSTED_HELMET.get(), Reg.DWARFKING_RUSTED_CHESTPLATE.get(), Reg.DWARFKING_RUSTED_LEGGINGS.get(), Reg.DWARFKING_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.KNIGHT_HELMET.get(), Reg.KNIGHT_CHESTPLATE.get(), Reg.KNIGHT_LEGGINGS.get(), Reg.KNIGHT_BOOTS.get(), net.minecraft.world.item.Items.IRON_SWORD},
                {Reg.NAZGUL_HELMET.get(), Reg.NAZGUL_CHESTPLATE.get(), Reg.NAZGUL_LEGGINGS.get(), Reg.NAZGUL_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.MITHRIL_HELMET.get(), Reg.MITHRIL_CHESTPLATE.get(), Reg.MITHRIL_LEGGINGS.get(), Reg.MITHRIL_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                {Reg.KNIGHT_RUSTED_HELMET.get(), Reg.KNIGHT_RUSTED_CHESTPLATE.get(), Reg.KNIGHT_RUSTED_LEGGINGS.get(), Reg.KNIGHT_RUSTED_BOOTS.get(), net.minecraft.world.item.Items.IRON_SWORD}};
        String[] closeNames = {"07b_orc_3d", "07c_nain_3d", "07d_elfe_3d", "07e_rohan_3d", "07f_gondor_3d", "07g_roi_elfe_3d",
                "08b_orc_rouille", "08c_nain_rouille", "08d_elfe_rouille", "08e_rohan_rouille", "08f_gondor_rouille", "08g_roi_elfe_rouille", "07h_roi_nain_3d", "08h_roi_nain_rouille", "07i_chevalier_3d", "07j_nazgul_3d", "07k_mithril_3d", "08j_chevalier_rouille"};
        for (int ci = 0; ci < 18; ci++) {
            Item[] set = close[ci];
            SCENES.add(new Scene(closeNames[ci], 70, p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                float[] yaws = {180F, 90F, 0F};
                for (int k = 0; k < 3; k++) {
                    float yw = yaws[k];
                    actor(ow, ModEntities.HUMAN.get(), SX + 1.3 - k * 1.3, SZ + 3.7, m -> {
                        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
                        for (int i = 0; i < 5; i++) m.setItemSlot(slots[i], new ItemStack(set[i]));
                        m.setYRot(yw); m.setYHeadRot(yw); m.setYBodyRot(yw);
                    });
                }
                view(p, ow, SX + 0.5, SY + 1.3, SZ, 0F, 7F);
            }));
        }
        // bottes « Pieds » : gros plan sur les pieds nus (face, profil, dos)
        SCENES.add(new Scene("09h_pieds", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            float[] yaws = {180F, 140F};
            for (int k = 0; k < 2; k++) {
                float yw = yaws[k];
                actor(ow, ModEntities.HUMAN.get(), SX + 0.1 + k * 0.8, SZ + 1.45, m -> {
                    m.setItemSlot(EquipmentSlot.FEET, new ItemStack(Reg.FEET_BOOTS.get()));
                    m.setYRot(yw); m.setYHeadRot(yw); m.setYBodyRot(yw);
                });
            }
            view(p, ow, SX + 0.5, SY + 0.75, SZ, 0F, 44F);
        }));
        // 3 textures de rouille : meme armure, variantes 1 / 2 / 3 (tag rust_variant)
        SCENES.add(new Scene("08i_variantes_rouille", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            Item[] set = close[7];
            for (int k = 0; k < 3; k++) {
                final int variant = k + 1;
                actor(ow, ModEntities.HUMAN.get(), SX + 1.3 - k * 1.3, SZ + 3.7, m -> {
                    EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                    for (int i = 0; i < 4; i++) {
                        ItemStack st = new ItemStack(set[i]);
                        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, st, t -> t.putInt("rust_variant", variant));
                        m.setItemSlot(slots[i], st);
                    }
                    m.setYRot(180F); m.setYHeadRot(180F); m.setYBodyRot(180F);
                });
            }
            view(p, ow, SX + 0.5, SY + 1.3, SZ, 0F, 7F);
        }));
        // supports invisibles : armures visibles, support invisible, incassable (on tente de le detruire)
        SCENES.add(new Scene("06_support_invisible", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            Item[][] sets = {close[3], close[4], close[5]};
            for (int k = 0; k < 3; k++) {
                net.ringsofash.entity.InvisibleStand st = ModEntities.INVISIBLE_STAND.get().create(ow);
                st.moveTo(SX + 1.3 - k * 1.3, SY, SZ + 3.7, 180F, 0F);
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
                for (int i = 0; i < 5; i++) st.setItemSlot(slots[i], new ItemStack(sets[k][i]));
                ow.addFreshEntity(st);
                st.hurt(ow.damageSources().generic(), 1000F);
                st.hurt(ow.damageSources().playerAttack(p), 1000F);
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                p.attack(st); p.attack(st);
                st.hurt(ow.damageSources().explosion(null, null), 1000F);
                st.hurt(ow.damageSources().onFire(), 1000F);
                System.out.println("[ClientTest] support invisible " + k + " vivant apres coups : " + st.isAlive() + " invisible : " + st.isInvisible());
            }
            view(p, ow, SX + 0.5, SY + 1.3, SZ, 0F, 7F);
        }));
    }

    private static void scenes() {
        armourScenes();
        if (ARMOR_ONLY) return;
        // l'Oeil de Sauron en premier : la serie de captures est longue et peut etre coupee par le delai du CI
        eyeT("95_oeil_sud", 1712, 1742, 0, 0, 40, 180F, 0F, 6000);
        eyeT("99_oeil_haut", 1712, 1742, 20, 25, 30, 150F, 35F, 6000);
        eyeFar("96_oeil_loin_nuit", 1712, 1742, 700, true, 18000);
        eyeFar("97_oeil_loin_cache", 1712, 1742, 700, false, 18000);
        eyeFar("98_oeil_tres_loin_jour", 1712, 1742, 1800, true, 6000);
        rivT("399_fondcombe_chargement", 176, 60, 395, 176, 20, 176, 6000);    // charge les chunks du site (tres long) : sa capture ne compte pas
        rivT("401_fondcombe_vue_sud", 110, 70, 310, 110, 45, 140, 6000);
        rivT("402_fondcombe_maison_vallee", 112, 50, 188, 108, 52, 140, 6000);
        rivT("403_fondcombe_promenade", 107, 35, 158, 107, 52, 128, 6000);
        rivT("404_fondcombe_conseil", 195, 36, 176, 195, 42, 136, 6000);
        rivT("405_fondcombe_grande_cascade", 262, 12, 140, 262, 45, 76, 6000);
        rivT("405b_fondcombe_cascade_loin", 262, 70, 215, 262, 35, 72, 6000);
        rivT("406_fondcombe_pont", 262, 24, 200, 262, 30, 132, 6000);
        rivT("414_fondcombe_pont_dessus", 214, 48, 160, 262, 36, 132, 6000);
        rivT("407_fondcombe_tours", 104, 52, 168, 74, 62, 126, 6000);
        rivT("408_fondcombe_pagode", 214, 46, 152, 214, 62, 116, 6000);
        rivT("409_fondcombe_salle", 80, 35, 138, 132, 36, 138, 6000);
        rivT("410_fondcombe_vue_haute", 176, 125, 335, 176, 40, 140, 6000);
        rivT("411_fondcombe_gradin", 112, 62, 112, 106, 68, 88, 6000);
        rivT("412_fondcombe_arcade", 110, 16, 200, 110, 20, 163, 6000);
        rivT("413_fondcombe_coucher", 236, 42, 170, 110, 55, 130, 12800);
        rivT("414b_fondcombe_lac", 240, 104, 112, 262, 88, 48, 6000);
        rivT("415_fondcombe_epaule_est", 335, 58, 205, 305, 48, 140, 6000);
        rivT("416_fondcombe_galerie", 110, 34, 186, 110, 22, 146, 6000);
        rivT("417_fondcombe_pilier_sud", 140, 26, 345, 140, 14, 300, 6000);
        rivT("418_fondcombe_pilier_ouest", 20, 24, 300, 70, 14, 300, 6000);
        rivT("419_fondcombe_pilier_haut", 130, 36, 262, 130, 14, 300, 6000);
        rivO("420_fondcombe_pente_ouest", -170, 80, 120, 10, 50, 120, 10, 0, 0);
        rivO("422_fondcombe_pente_nord", 160, 90, -190, 160, 50, 20, 4, 10, 4);
        rivO("423_fondcombe_pente_nordouest", -150, 90, -150, 40, 50, 40, 10, 10, 0);
        helmT("430_helm_chargement", 120, 40, 215, 150, 15, 80, 6000);              // charge les chunks du site : sa capture ne compte pas
        helmT("431_helm_vue_sud", 130, 46, 214, 150, 18, 80, 6000);
        helmT("432_helm_muraille", 80, 12, 130, 92, 14, 86, 6000);
        helmT("433_helm_burg", 214, 34, 160, 180, 16, 84, 6000);
        helmT("434_helm_tour", 112, 22, 114, 142, 30, 82, 6000);
        helmT("435_helm_cour", 180, 12, 110, 180, 22, 82, 6000);
        helmT("436_helm_salle", 180, 27, 79, 180, 28, 58, 6000);
        helmT("437_helm_rampe", 138, 9, 172, 168, 5, 124, 6000);
        helmT("438_helm_vue_haute", 120, 120, 215, 150, 30, 80, 6000);
        helmT("439_helm_vallee", 96, 14, 80, 100, 28, 30, 6000);
        esgT("419_esgaroth_chargement", 160, 30, 290, 160, 4, 160, 6000);          // charge les chunks du site (tres long) : sa capture ne compte pas
        esgT("421_esgaroth_vue_sud", 160, 50, 300, 160, 8, 150, 7000);
        esgT("422_esgaroth_vue_haute", 40, 90, 290, 150, 5, 150, 7000);
        esgT("423_esgaroth_halle", 160, 9, 190, 158, 14, 140, 7000);
        esgT("424_esgaroth_canal", 108, 6, 190, 108, 4, 140, 7000);
        esgT("425_esgaroth_chaussee", 160, 8, 262, 160, 6, 190, 7000);
        esgT("426_esgaroth_ponton", 108, 3, 164, 142, 9, 150, 7000);
        esgT("427_esgaroth_tour", 100, 22, 130, 84, 24, 160, 7000);
        esgT("428_esgaroth_rive", 160, 40, 60, 160, 6, 120, 7000);
        esgT("429_esgaroth_crepuscule", 130, 12, 214, 160, 12, 150, 12600);
        esgT("430_esgaroth_arbalete", 62, 46, 136, 84, 38, 151, 7000);
        esgT("431_esgaroth_arbalete_haut", 90, 44, 140, 82, 36, 152, 7000);
        esgT("432_esgaroth_interieur", 83, 5, 134, 87, 3, 139, 6000);
        esgT("433_esgaroth_peche", 100, 12, 126, 78, 2, 108, 7000);
        esgT("434_esgaroth_peche_sud", 196, 14, 236, 178, 2, 258, 7000);
        esgT("435_esgaroth_halle_interieur", 150, 8, 150, 160, 7, 140, 6000);
        argT("299_argonath_chargement", 0, 60, -30, 0, 50, 40, 6000);       // charge les chunks du site (long) : sa capture ne compte pas
        argT("301_argonath_vue_haute", 70, 135, -122, 0, 50, 0, 6000);
        argT("302_argonath_visages", 0, 98, -48, 0, 92, 0, 6000);
        argT("303_argonath_dos_sud", 0, 38, 105, 0, 62, 0, 6000);
        argT("304_argonath_cote_ouest", -48, 30, -72, -24, 60, 0, 6000);
        argT("305_argonath_relief_est", 175, 28, 0, 0, 45, 0, 6000);
        argT("306_argonath_riviere_sud", 0, 3, 70, 0, 48, -10, 6000);
        argT("307_argonath_coucher", 0, 6, -118, 0, 62, 0, 12800);
        argT("308_argonath_mains", 0, 96, -85, 0, 90, -27, 6000);
        argT("309_argonath_base_ouest", -20, 14, -56, -25, 10, 0, 6000);
        argT("310_argonath_profil_drape", 55, 100, -45, -12, 84, -14, 6000);
        argT("311_argonath_rive_est", -20, 25, -75, 62, 20, -70, 6000);
        argT("312_argonath_rive_ouest", 20, 25, -75, -62, 20, -70, 6000);
        argT("313_argonath_bout_sud", 0, 40, 175, 0, 12, 100, 6000);
        SCENES.add(new Scene("00_scene", 120, p -> {
            MinecraftServer s = p.server;
            ServerLevel ow = s.overworld();
            s.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
            s.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, s);
            s.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, s);
            ow.setDayTime(6000);
            ow.setWeatherParameters(100000, 0, false, false);
            for (int x = -44; x <= 44; x++) for (int z = -8; z <= 44; z++) {
                ow.setBlock(new BlockPos(SX + x, SY - 1, SZ + z), ((x + z) & 1) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                if (z == 44) for (int y = 0; y < 14; y++) ow.setBlock(new BlockPos(SX + x, SY + y, SZ + z), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2);
            }
            clear(ow);
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, 0F);
        }));
        // Minas Tirith (place (480, 1855), plan de 320 x 340 centre sur le lieu) ; py = niveau 0 du plan ; (px, y, pz) = coordonnees du plan (x vers l'est, z vers le sud)
        final int TX = 480, TZ = 1855;
        final String[] tn = {"200_tirith_loin_sud", "201_tirith_sud_ouest", "202_tirith_porte", "203_tirith_route", "204_tirith_cour", "205_tirith_tour", "206_tirith_arbre", "207_tirith_nuit",
                "208_tirith_est", "209_tirith_crete", "210_tirith_falaise", "211_tirith_escalier", "212_tirith_tunnel", "213_tirith_maison", "214_tirith_rue", "215_tirith_arbre_large",
                "216_tirith_porte_haut", "217_tirith_route_cote", "218_tirith_montagne_ouest", "219_tirith_proue", "220_tirith_crete_dessus", "221_tirith_tour_porte", "222_tirith_tour_traversee", "223_tirith_rampe_haut"};
        SCENES.add(new Scene("199_tirith_chargement", 100, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            for (int dx = -11; dx <= 11; dx++) for (int dz = -9; dz <= 15; dz++) lands.getChunk((TX >> 4) + dx, (TZ >> 4) + dz);
            view(p, lands, TX + 0.5, 200, TZ + 0.5, 180F, 0F);
        }));
        for (int k = 0; k < tn.length; k++) {
            final int kk = k;
            SCENES.add(new Scene(tn[k], 420, p -> {
                ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                for (int dx = -11; dx <= 11; dx++) for (int dz = -9; dz <= 15; dz++) lands.getChunk((TX >> 4) + dx, (TZ >> 4) + dz);
                net.ringsofash.world.TirithPlan tp = net.ringsofash.world.TirithPlan.get();
                final int CX = net.ringsofash.world.TirithPlan.CX, CZ = net.ringsofash.world.TirithPlan.CZ;
                int py = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, TX - 40, TZ + 100) - 18;               // le premier niveau est a 18 blocs au-dessus de la plaine
                int ox = TX - 160, oz = TZ - net.ringsofash.world.TirithPlan.D / 2;                          // origine du plan : x_plan = x_monde - ox
                int top = py + net.ringsofash.world.TirithPlan.PROW_TOP;
                lands.setDayTime(kk == 7 ? 18000 : 6000);
                int rd = (kk == 0 || kk == 1 || kk == 7 || kk == 8 || kk == 17 || kk == 18) ? 20 : 12;
                p.server.getPlayerList().setViewDistance(rd);
                net.minecraft.client.Minecraft.getInstance().options.renderDistance().set(rd);
                switch (kk) {
                    case 0, 7 -> view(p, lands, TX + 0.5, py + 70, TZ + 200.5, 180F, -22F);
                    case 1 -> view(p, lands, TX - 149.5, py + 45, TZ + 140.5, -135F, -16F);
                    case 2 -> look(p, lands, ox + CX + 0.5, py + 8, oz + CZ + 160.5, ox + CX + 0.5, py + 14, oz + CZ + 118);                  // la grande porte, depuis l'esplanade
                    case 3 -> look(p, lands, ox + CX + 0.5, py + 12, oz + 336.5, ox + CX + 0.5, py + 8, oz + CZ + 110);                       // la route, depuis son bout, vers la porte
                    case 4 -> look(p, lands, TX + 7.5, top + 5, oz + tp.court_z + 8.5, TX, top + 8, oz + tp.tower_z + 6);                       // la cour, du portail vers la tour
                    case 5 -> look(p, lands, TX - 30.5, top + 34, oz + tp.court_z + 22.5, TX, top + 70, oz + tp.tower_z);                         // la tour d'Ecthelion
                    case 6 -> look(p, lands, TX + 9.5, top + 5, oz + tp.court_z + 6.5, TX - 2, top + 3, oz + tp.court_z - 4);                     // fontaine et Arbre Blanc
                    case 8 -> view(p, lands, TX + 200.5, py + 120, TZ + 40.5, 90F, -14F);
                    case 9 -> look(p, lands, ox + CX - 40.5, py + 44, oz + CZ + 96.5, ox + CX - 4, py + 74, oz + CZ + 78);                     // l'eperon de roche vu de la rue du niveau 2
                    case 10 -> look(p, lands, ox + CX - 95.5, py + 150, oz + CZ + 20.5, ox + CX - 15, py + 185, oz + CZ - 28);                  // la falaise de la montagne
                    case 11 -> look(p, lands, ox + CX + 42.5, top + 4, oz + CZ + 26.5, ox + CX + 24, top - 3, oz + CZ + 23.5);                  // la rampe du flanc, qui debouche dans la cour
                    case 12 -> look(p, lands, ox + CX - 9.5, py + 56, oz + CZ + 80.5, ox + CX + 10, py + 56.5, oz + CZ + 78.5);                  // un tunnel sous l'eperon
                    case 13 -> {                                                                                                               // une porte de maison, de pres
                        int[] d = findDoor(tp, CX - 48, CX - 16, CZ + 60, CZ + 125);
                        if (d == null) view(p, lands, TX + 0.5, py + 40, TZ + 60.5, 180F, 0F);
                        else {
                            int[][] o4 = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
                            double cxw = ox + d[0] + 0.5 + o4[d[3]][0] * 4.0, czw = oz + d[2] + 0.5 + o4[d[3]][1] * 4.0;
                            look(p, lands, cxw, py + d[1] + 2.2, czw, ox + d[0] + 0.5, py + d[1] + 1.3, oz + d[2] + 0.5);
                        }
                    }
                    case 14 -> look(p, lands, ox + CX + 22, py + 74, oz + CZ + 57, ox + CX + 58, py + 78, oz + CZ + 32);                      // une rue du niveau 4, ses etals et ses passants
                    case 15 -> look(p, lands, ox + CX + 15.5, top + 16, oz + CZ + 30.5, ox + CX, top + 14, oz + CZ + 8);                       // l'Arbre Blanc en entier
                    case 16 -> look(p, lands, ox + CX + 0.5, py + 40, oz + CZ + 150.5, ox + CX + 0.5, py + 30, oz + CZ + 116);                 // le porche en ogive, d'en haut
                    case 17 -> view(p, lands, TX - 60.5, py + 14, TZ + 160.5, -45F, -3F);                                                        // la route vue de cote
                    case 18 -> view(p, lands, TX - 200.5, py + 130, TZ + 20.5, -90F, -10F);                                                      // la montagne, a l'ouest
                    case 19 -> look(p, lands, ox + CX - 34.5, py + 104, oz + CZ + 136.5, ox + CX, py + 108, oz + CZ + 100);                      // le bout de l'eperon : face droite, terrasses blanches
                    case 20 -> look(p, lands, ox + CX + 0.5, top + 16, oz + CZ + 36.5, ox + CX + 0.5, top - 8, oz + CZ + 96);                    // le dessus de l'eperon : sol blanc, barrieres, marches
                    case 21 -> look(p, lands, ox + CX - 27.5, py + 20.6, oz + CZ + 123.5, ox + CX - 18, py + 20.4, oz + CZ + 116.5);               // la porte d'une tour de la grande porte, vue de la rue
                    case 23 -> look(p, lands, ox + 206 + 0.5, py + 52, oz + 280.5, ox + 206, py + 32, oz + 248);                                 // le haut d'une rampe, sans dalle sur les marches
                    default -> look(p, lands, ox + CX - 30.5, py + 19.8, oz + CZ + 116.5, ox + CX - 8, py + 19.8, oz + CZ + 116.5);               // a travers la tour, la galerie et l'autre tour
                }
            }));
        }
        // Moria : l'entree (falaise, lac, portes), la Grande Salle, l'escalier, le gouffre, les Forges, la tombe, une galerie
        morT("430_moria_porte", 192.5, 86, -360.5, 168, 92, -360.5, 6000);
        morT("441_moria_porte_loin", 222.5, 92, -372.5, 168, 92, -360.5, 6000);
        morT("442_moria_porte_dedans", 150.5, 84, -360.5, 168, 90, -360.5, 6000);
        morT("443_moria_rue", -128.5, -12, -730.5, -240, -4, -730.5, 6000);
        morT("444_moria_quartier", -125.5, -10, -630.5, -235, 0, -690.5, 6000);
        morT("445_moria_roi", -232.5, -16, -600.5, -255, -12, -591.5, 6000);
        morT("446_moria_festins", -126.5, -12, -507.5, -250, -12, -507.5, 6000);
        morT("447_moria_armurerie", -77.5, -14, -596.5, -77.5, -10, -660.5, 6000);
        morT("448_moria_archives", 193.5, -14, -598.5, 193.5, -10, -660.5, 6000);
        morT("449_moria_crypte", -77.5, -14, -862.5, -77.5, -10, -925.5, 6000);
        morT("450_moria_trone", 140.5, -8, -840.5, 140.5, 5, -950.5, 6000);
        morT("451_moria_trone_milieu", 140.5, 5, -870.5, 140.5, 5, -950.5, 6000);
        morT("452_moria_tresor", 205.5, 16, -878.5, 263, 10, -912, 6000);
        morT("453_moria_scelle", 214.5, -14, -836.5, 214.5, -10, -849, 6000);
        morT("454_moria_mine", -150.5, 0, -863.5, -214, -30, -863, 6000);
        morT("455_moria_mine_fond", -214.5, -8, -800.5, -214.5, -40, -880, 6000);
        morT("456_moria_four", -98.5, -8, -735.5, -98, -13, -700, 6000);
        morT("457_moria_relief_salle", 60.5, 95, -330.5, 60, 100, -250, 6000);
        morT("458_moria_galerie", -300.5, -15, -863.5, -380, -17, -850, 6000);
        morT("459_moria_descente", -205.5, -17, -990.5, -182, -48, -990, 6000);
        morT("460_moria_veine", -180.5, -45, -1078.5, -195, -45, -1092, 6000);
        morT("461_moria_falaise_nord", -150.5, -10, -660.5, -215, -5, -695.5, 6000);
        morT("462_moria_ravine", -202.5, -14, -640.5, -250, -5, -690.5, 6000);
        morT("463_moria_palais", -202.5, -14, -615.5, -178, -12, -578, 6000);
        morT("464_moria_falaise_ouest", -190.5, -12, -640.5, -262, -5, -640.5, 6000);
        morT("466_moria_four_b", -42.5, -8, -735.5, -42, -13, -698, 6000);
        morT("467_moria_four_c", 64.5, -8, -735.5, 64, -14, -698, 6000);
        morT("480_moria_four_nain", -170.5, -17.8, -693.5, -169.5, -18.6, -697.5, 6000);
        morT("481_moria_table_nain", -218.5, -17.6, -573.5, -218.5, -18.4, -569.5, 6000);
        morT("482_comte_ancienne_cuvette", -1105.5, 125, -455.5, -1105, 85, -540, 6000);
        morT("483_comte_cuvette_haut", -1105.5, 160, -535.5, -1105, 80, -520, 6000);
        // Hobbiteville : coordonnees du fichier source (x 0..324, z 9..366, y source : le sol du village est vers -38)
        hobT("hoba_hobbiteville_aerien", 180, 40, 330, 180, -40, 150);
        morT("zon1_elfe_a", -1550.5, 108, -75.5, -1520, 95, -75, 6000);
        morT("zon2_elfe_b", -1460.5, 128, -240.5, -1405, 96, -240, 6000);
        morT("zon3_fangorn_a", 715.5, 108, 5.5, 765, 78, 5, 6000);
        morT("zon4_fangorn_b", 685.5, 100, -1105.5, 715, 86, -1105, 6000);
        morT("zon5_marais_a", 1165.5, 92, 740.5, 1205, 66, 740, 6000);
        morT("zon6_marais_b", 1205.5, 112, 700.5, 1205, 64, 760, 6000);
        morT("zon7_marais_nuit", 1340.5, 84, 860.5, 1340, 66, 890, 18000);
        morT("zon8_comte_prairie", -590.5, 96, 740.5, -560, 82, 740, 6000);
        morT("zon9_comte_nord", 165.5, 96, -2060.5, 195, 83, -2060, 6000);
        morT("zona_elfe_sol", -1524.5, 100, -64.5, -1510, 95, -78, 6000);
        morT("zonb_elfe_sol2", -1437.5, 102, -228.5, -1450, 97, -244, 6000);
        morT("zonc_elfe_sol3", -1546.5, 102, -88.5, -1532, 96, -74, 6000);
        morT("cyp1_cypres_vue", 2008.5, 81, -1140.5, 2045, 77, -1140, 6000);
        morT("cyp2_cypres_sol", 1978.5, 87, -950.5, 2005, 83, -950, 6000);
        morT("cyp3_sequoia", 2078.5, 85, -798.5, 2088, 104, -788, 6000);
        morT("cyp4_cypres_aerien", 2268.5, 132, -630.5, 2330, 78, -630, 6000);
        morT("pon1_pont", 800.5, 70, 561.5, 808, 66, 568, 6000);
        morT("pon2_pont_cote", 814.5, 68, 573.5, 808, 66, 568, 6000);
        morT("riv1_berge", -196.5, 70, 60.5, -175, 65, 70, 6000);
        morT("riv2_berge_foret", -660.5, 76, 190.5, -640, 70, 200, 6000);
        morT("riv3_berge_pres", -200.5, 68, 72.5, -190, 65, 78, 6000);
        morT("cro1_interieur", -871.5, 83, -114.5, -861, 82, -114, 6000);
        morT("cro2_interieur_bas", -866.5, 81, -122.5, -866, 80, -114, 6000);
        morT("cro3_exterieur", -882.5, 97, -100.5, -862, 90, -100, 6000);
        morT("mini1_cabane", 1055.5, 77, -2065.5, 1061, 74, -2071, 6000);
        morT("mini2_ruine", 829.5, 78, -2064.5, 837, 74, -2070, 6000);
        morT("mini3_puits", -162.5, 88, -2069.5, -154, 84, -2075, 6000);
        morT("mini4_campement", 858.5, 77, -2068.5, 853, 74, -2074, 6000);
        morT("cyp5_cypres_matin", 1884.5, 82, -306.5, 1915, 77, -300, 23000);
        morT("comz1_champ_nord", -910.5, 105, -100.5, -910, 72, -185, 6000);
        morT("comz2_champ_est", -595.5, 100, 65.5, -655, 72, 65, 6000);
        morT("comz3_chemin_sud", -690.5, 105, 170.5, -750, 70, 235, 6000);
        morT("comz4_chemin_est", -610.5, 100, -40.5, -665, 72, -45, 6000);
        morT("comz5_ouest_ancienne_foret", -1050.5, 120, 120.5, -1100, 70, 200, 6000);
        morT("comz6_aerien_nord", -880.5, 190, -290.5, -925, 70, -180, 6000);
        morT("comz7_aerien_est", -570.5, 190, 10.5, -650, 70, 65, 6000);
        morT("comz8_nord_loin", -910.5, 110, -200.5, -910, 100, -300, 6000);
        morT("comz9_nord_bas", -910.5, 130, -215.5, -910, 60, -250, 6000);
        spot("comta_chemin", -913, 352, 10, 225F, 22F);
        spot("comtb_chemin_sud", -1125, 650, 14, 180F, 20F);
        spot("comtc_aerien", -1000, 600, 70, 200F, 38F);
        spot("comtd_arbres", -700, 420, 14, 90F, 18F);
        hobT("hobz1_arbre_a", 116, -28, 178, 98, -32, 162);
        hobT("hobz2_arbre_b", 190, -24, 190, 178, -30, 154);
        hobT("hobz3_arbres_ouest", 40, -20, 122, 20, -32, 108);
        hobT("hobm_porte_jaune", 216, -28.5, 87, 216, -30, 77);
        hobT("hobr_irrigation", 197, -41, 223, 191, -44, 215);
        hobT("hobt_ble_nord", 120, -14, 79, 120, -38, -2);
        hobT("hobu_patate_sud", 110, -14, 259, 110, -38, 369);
        hobT("hobv_ble_est", 250, -14, 189, 360, -38, 239);
        hobT("hobw_ble_coin_ne", 190, -10, 79, 300, -38, 29);
        hobT("hobx_patate_ouest", 90, -10, 69, -30, -38, 49);
        hobT("hoby_patate_sud_ouest", 70, -10, 339, -30, -38, 349);
        morT("484_spawn_hautemarche", -700.5, 80, -14.5, -700, 74, 0, 6000);
        morT("485_moria_village_escalier_nord", -150.5, -17.5, -668.5, -148, -8, -682, 6000);
        morT("486_moria_village_falaise_ouest", -215.5, -10.5, -625.5, -246, -14, -630, 6000);
        morT("487_moria_village_escalier_2", -232.5, -14.5, -660.5, -232, -5, -682, 6000);
        morT("488_moria_passage_avenue_forges", -125.5, -17.5, -730.5, -100, -17, -730, 6000);
        morT("489_moria_passage_trone", 140.5, -17.5, -826.5, 140, -17, -845, 6000);
        morT("490_moria_pied_escalier_forges", 75.5, -17.5, -708.5, 75, -14, -690, 6000);
        morT("491_moria_mine_escalier", -200.5, -13.5, -776.5, -200, -34, -830, 6000);
        morT("492_moria_pont_arche", 50.5, 22.5, -600.5, 76, 17, -600.5, 6000);
        morT("493_moria_pont_arche_bout", 76.5, 25.5, -650.5, 76, 18, -600, 6000);
        morT("494_moria_descente_galerie", -206.5, -18.5, -990.5, -185, -30, -990, 6000);
        morT("495_moria_mine_escalier_vue", -200.5, -12.5, -800.5, -200, -34, -845, 6000);
        morT("496_moria_descente_galerie_2", -192.5, -30.5, -990.5, -175, -48, -990.5, 6000);
        morT("497_portes_facade", 244.5, 98.5, -360.5, 168, 102, -360, 6000);
        morT("498_portes_parvis", 215.5, 83.5, -360.5, 168, 96, -360, 6000);
        morT("499_portes_statue_cascade", 205.5, 90.5, -325.5, 168, 98, -345, 6000);
        morT("500_portes_chaussee", 280.5, 90.5, -360.5, 200, 84, -360, 6000);
        morT("501_portes_cour_haut", 230.5, 135.5, -360.5, 170, 95, -360, 6000);
        morT("502_portes_portail_proche", 182.5, 84.5, -352.5, 166, 104, -360, 6000);
        morT("503_portes_seuil", 196.5, 82.5, -360.5, 166, 92, -360, 6000);
        morT("504_portes_nain_face", 212.5, 104.5, -346.5, 174, 104, -333, 6000);
        morT("505_portes_nain_profil", 200.5, 92.5, -352.5, 176, 100, -334, 6000);
        morT("600_erebor_loin", 1640.5, 116.5, -1105.5, 1640, 161, -1215, 6000);
        morT("601_erebor_facade", 1640.5, 91.5, -1163.5, 1640, 141, -1215, 6000);
        morT("602_erebor_porte", 1640.5, 79.5, -1178.5, 1640, 98, -1213, 6000);
        morT("603_erebor_grande_salle", 1640.5, 82.5, -1233.5, 1640, 98, -1313, 6000);
        morT("604_erebor_escalier", 1640.5, 79.5, -1315.5, 1640, 104, -1373, 6000);
        morT("605_erebor_galerie", 1640.5, 114.5, -1367.5, 1640, 126, -1473, 6000);
        morT("606_erebor_trone", 1640.5, 120.5, -1453.5, 1640, 131, -1475, 6000);
        morT("607_erebor_tresor", 1578.5, 36.5, -1375.5, 1640, 61, -1427, 6000);
        morT("608_erebor_arkenstone", 1640.5, 66.5, -1393.5, 1640, 74, -1427, 6000);
        morT("609_erebor_montagne", 1640.5, 256.5, -1063.5, 1640, 176, -1355, 6000);
        morT("610_erebor_forges", 1530.5, 79.5, -1315.5, 1510, 78, -1285, 6000);
        morT("611_erebor_quartiers", 1762.5, 79.5, -1319.5, 1762, 78, -1265, 6000);
        morT("612_erebor_reserves", 1762.5, 79.5, -1229.5, 1762, 78, -1205, 6000);
        morT("613_erebor_mine_hall", 1490.5, -8.5, -1280.5, 1510, -10, -1255, 6000);
        morT("614_erebor_galerie_mine", 1530.5, -9.5, -1265.5, 1600, -11, -1265, 6000);
        morT("615_erebor_coffre_secret", 1610.5, 114.5, -1506.5, 1610, 113, -1519, 6000);
        morT("616_erebor_descente", 1518.5, 67.5, -1267.5, 1518, 28, -1305, 6000);
        morT("617_erebor_salle_minerais", 1792.5, -8.5, -1271.5, 1802, -11, -1253, 6000);
        morT("618_erebor_salle_forge", 1610.5, 79.5, -1287.5, 1577, 82, -1287, 6000);
        morT("619_erebor_taverne", 1670.5, 79.5, -1287.5, 1703, 82, -1287, 6000);
        morT("620_erebor_atelier", 1610.5, 79.5, -1315.5, 1579, 81, -1315, 6000);
        morT("621_erebor_bibliotheque", 1610.5, 115.5, -1451.5, 1577, 117, -1451, 6000);
        morT("622_erebor_gardes", 1670.5, 115.5, -1451.5, 1703, 117, -1451, 6000);
        morT("623_erebor_forges_nef", 1519.5, 81.5, -1243.5, 1519, 92, -1303, 6000);
        morT("624_erebor_balcon", 1534.5, 92.5, -1287.5, 1505, 86, -1293, 6000);
        morT("625_erebor_quartiers_rue", 1762.5, 81.5, -1320.5, 1762, 78, -1265, 6000);
        morT("626_erebor_terrasse", 1777.5, 84.5, -1294.5, 1770, 86, -1283, 6000);
        morT("627_erebor_grande_salle_arcades", 1620.5, 82.5, -1321.5, 1642, 101, -1263, 6000);
        morT("628_erebor_galerie_niches", 1626.5, 116.5, -1383.5, 1657, 122, -1418, 6000);
        morT("629_erebor_vue_sud", 1700.5, 175.5, -1060.5, 1650, 140, -1215, 6000);
        morT("630_erebor_vue_est", 1860.5, 160.5, -1290.5, 1700, 140, -1300, 6000);
        morT("631_erebor_vue_ouest", 1530.5, 140.5, -1120.5, 1620, 140, -1215, 6000);
        morT("632_erebor_vue_dessus", 1640.5, 200.5, -1000.5, 1640, 150, -1215, 6000);
        morT("633_erebor_mines_portail", 1660.5, 78.5, -1140.5, 1745, 79, -1173, 6000);
        morT("634_erebor_mines_escalier", 1757.5, 73.5, -1179.5, 1757.5, 28, -1225, 6000);
        morT("635_erebor_mines_bas", 1757.5, 8.5, -1243.5, 1757.5, -9, -1267, 6000);
        morT("636_erebor_mines_vue", 1680.5, 95.5, -1128.5, 1745, 79, -1173, 6000);
        morT("637_erebor_passe_ouest", 1620.5, 79.5, -1140.5, 1510, 80, -1169, 6000);
        morT("704_moria_pont_bout_nord", 76.5, 26.5, -614.5, 76.5, 18, -650, 6000);
        morT("705_moria_pont_bout_sud", 76.5, 26.5, -588.5, 76.5, 18, -553, 6000);
        morT("700_moria_pont_fond", 76.5, 26.5, -628.5, 76, 24, -690, 6000);
        morT("701_moria_pont_fond_haut", 76.5, 44.5, -600.5, 60, 40, -655, 6000);
        morT("702_moria_pont_ouest", 112.5, 30.5, -600.5, 40, 25, -620, 6000);
        morT("703_moria_pont_est", 40.5, 30.5, -600.5, 112, 25, -620, 6000);
        morT("476_moria_chambre_lits", -183.5, -17.6, -690.5, -188.5, -19.2, -697.5, 6000);
        morT("477_moria_cuisine_four", -174.5, -17.5, -689.5, -173.5, -19, -697.5, 6000);
        morT("478_moria_etude_table", -221.5, -17.5, -576.5, -218.5, -19, -569, 6000);
        morT("479_moria_lit_gros_plan", -186.5, -17.8, -692.5, -188.5, -18.8, -696.5, 6000);
        morT("472_moria_pont_vue", -278.5, -16, -880.5, -150, -14, -880, 6000);
        morT("473_moria_pont_dessous", -214.5, -40, -845.5, -214, -14, -880, 6000);
        morT("474_moria_pont_wagons", -251.5, -13, -868.5, -252, -17, -880, 6000);
        morT("475_moria_pont_cotes", -214.5, -30, -905.5, -214, -14, -880, 6000);
        morT("469_moria_forges_vue", -95.5, 0, -745.5, 60, -5, -740, 6000);
        morT("470_moria_forges_mobilier", 20.5, -10, -745.5, 20, -17, -733, 6000);
        morT("471_moria_forges_tuyaux", 60.5, -10, -740.5, 60, 12, -700, 6000);
        morT("468_moria_four_d", 160.5, -8, -735.5, 160, -13, -702, 6000);
        morT("431_moria_lac", 305.5, 125, -360.5, 168, 100, -360.5, 6000);
        morT("432_moria_couloir", 158.5, 85, -360.5, 90, 95, -360.5, 6000);
        morT("433_moria_grande_salle", 128.5, 100, -296.5, 40, 100, -400.5, 6000);
        morT("434_moria_escalier", 75.5, 96, -478.5, 75.5, 50, -545.5, 6000);
        morT("435_moria_pont", 75.5, 40, -556.5, 76, 10, -640.5, 6000);
        morT("436_moria_forges", 75.5, -8, -692.5, 90, 10, -790.5, 6000);
        morT("437_moria_cascade", 56.5, -12, -770.5, 56, 10, -832.5, 6000);
        morT("438_moria_colosse", 26.5, 0, -664.5, 26.5, 14, -716.5, 6000);
        morT("439_moria_tombe", 266.5, -16, -760.5, 280, -16, -760.5, 6000);
        morT("440_moria_galerie", -125.5, -16, -762.5, -190, -16, -725.5, 6000);
        // Edoras (centre 352, 1180, perron du palais au sud, route en lacets depuis le sud-ouest) ; py = niveau du sol sous la colline
        final int EX = 352, EZ = 1180;
        for (Object[] it : new Object[][]{{"100_edoras_loin_sud_ouest", -125, 105, 60, -135F, -14F, 6000}}) {
            final Object[] c = it;
            SCENES.add(new Scene((String) c[0], 420, p -> {
                ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                for (int dx = -9; dx <= 9; dx++) for (int dz = -8; dz <= 8; dz++) lands.getChunk((EX >> 4) + dx, (EZ >> 4) + dz);
                int py = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, EX - 10, EZ + 10) - 30;
                int sx = EX + (Integer) c[1], sz = EZ + (Integer) c[2];
                int h = (Integer) c[3];
                int y = h < 0 ? lands.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz) + 6 : py + h;
                lands.setDayTime((Integer) c[6]);
                view(p, lands, sx + 0.5, y, sz + 0.5, (Float) c[4], (Float) c[5]);
            }));
        }
        // palais d'Or, tour de cloche, village, interieur, champs, etal, forge : positions tirees du plan
        for (int k = 0; k < 17; k++) {
            final int kk = k;
            if (k != 1 && k != 8 && k != 9 && k != 13 && k != 14 && k < 15) continue;                // les autres vues d'Edoras sont deja validees
            SCENES.add(new Scene(new String[]{"105_edoras_palais", "106_edoras_palais_cote", "107_edoras_tour", "108_edoras_village", "109_edoras_salle", "110_edoras_champs", "111_edoras_etal", "112_edoras_forge", "113_edoras_entree", "114_edoras_entree_dessus", "115_edoras_sol", "116_edoras_place", "117_edoras_fontaine", "118_edoras_porte_maison", "119_edoras_porte_aile", "120_edoras_sortie", "121_edoras_sortie_cote"}[k], 420, p -> {
                ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                for (int dx = -9; dx <= 9; dx++) for (int dz = -8; dz <= 8; dz++) lands.getChunk((EX >> 4) + dx, (EZ >> 4) + dz);
                net.ringsofash.world.EdorasPlan ep = net.ringsofash.world.EdorasPlan.get();
                int py = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, EX - 10, EZ + 10) - 30;
                int ox = EX - net.ringsofash.world.EdorasPlan.CX, oz = EZ - net.ringsofash.world.EdorasPlan.CZ;
                int hx = ox + ep.hallX, hz = oz + ep.hallZ, tx = ox + ep.towerX, tz = oz + ep.towerZ;
                lands.setDayTime(6000);
                switch (kk) {
                    case 0 -> view(p, lands, hx + 0.5, py + 34, hz + 40.5, 180F, 4F);
                    case 1 -> view(p, lands, hx + 40.5, py + 36, hz - 4.5, 90F, 8F);
                    case 2 -> view(p, lands, tx - 14.5, py + 36, tz + 24.5, -150F, -8F);
                    case 3 -> view(p, lands, EX - 20.5, py + 60, EZ + 70.5, 160F, 20F);
                    case 4 -> view(p, lands, hx + 0.5, py + 28, hz + 3.5, 180F, 2F);
                    case 5 -> look(p, lands, ox + ep.lowFieldX - 7.5, py + 7, oz + ep.lowFieldZ + 10.5, ox + ep.lowFieldX, py + 1, oz + ep.lowFieldZ);
                    case 6 -> look(p, lands, ox + ep.stallX - 6.5, py + ep.stallL + 3, oz + ep.stallZ + 8.5, ox + ep.stallX, py + ep.stallL + 1, oz + ep.stallZ);
                    case 7 -> look(p, lands, ox + ep.smithX - 7.5, py + ep.smithL + 3, oz + ep.smithZ + 9.5, ox + ep.smithX, py + ep.smithL + 1, oz + ep.smithZ);
                    case 8 -> {                                                           // l'entree : la porte fortifiee vue de la route
                        int gx = ox + ep.gateX, gz = oz + ep.gateZ;
                        int sg = ep.get(ep.gateX, 0, ep.gateZ + 14) == 0 ? 1 : -1;      // cote du passage qui donne sur l'exterieur
                        int sx = gx + 6, sz = gz + sg * 26;
                        int fy = floorY(lands, sx, sz, py - 20);
                        look(p, lands, sx + 0.5, fy + 4, sz + 0.5, gx, fy - 3, gz + sg * 10);
                    }
                    case 15, 16 -> {                                                      // la sortie de la porte : l'escalier jusqu'au sol naturel
                        int gx = ox + ep.gateX, gz = oz + ep.gateZ;
                        int sgn = ep.gateX < net.ringsofash.world.EdorasPlan.W - 1 - ep.gateX ? -1 : 1;
                        if (kk == 15) look(p, lands, gx - sgn * 7.5, py + 7, gz + 0.5, gx + sgn * 12, py - 3, gz + 0.5);
                        else look(p, lands, gx + sgn * 20.5, py + 9, gz + 15.5, gx + sgn * 8, py - 2, gz + 0.5);
                    }
                    case 9 -> {                                                           // l'entree vue de l'interieur du village, un peu en hauteur
                        int gx = ox + ep.gateX, gz = oz + ep.gateZ;
                        look(p, lands, gx + 22.5, py + 12, gz - 6.5, gx, py + 5, gz);
                    }
                    case 10 -> look(p, lands, ox + ep.gateX + 14.5, py + 6, oz + ep.gateZ + 16.5, ox + ep.gateX + 22, py + 1, oz + ep.gateZ + 8);   // le sol du bas du village
                    case 11 -> look(p, lands, ox + ep.plazaX + 0.5, py + 40, oz + ep.plazaZ + 22.5, ox + ep.plazaX, py + 31, oz + ep.plazaZ - 6);   // la place devant le palais
                    case 12 -> look(p, lands, ox + ep.plazaX + 11.5, py + 34, oz + ep.plazaZ + 7.5, ox + ep.plazaX - 1, py + 31, oz + ep.plazaZ);
                    case 13 -> {                                                          // l'entree d'une maison, vue de face
                        java.util.List<int[]> dl = ep.doorList();
                        int[] dr = dl.get(Math.max(0, dl.size() / 2));
                        int fx = ox + dr[0] + dr[3] * 3, fz = oz + dr[1] + dr[4] * 3;
                        int fy = floorY(lands, fx, fz, py - 10);
                        look(p, lands, fx + 0.5, fy + 1.7, fz + 0.5, ox + dr[0] - dr[3] * 3 + 0.5, fy + 1.4, oz + dr[1] - dr[4] * 3 + 0.5);
                    }
                    default -> {                                                          // dans la grande salle, vers la porte d'une aile
                        int fy = floorY(lands, hx + 3, hz - 19, py - 10);
                        look(p, lands, hx + 3.5, fy + 1.7, hz - 19.5, hx - 8, fy + 1.9, hz - 19.5);
                    }
                }
            }));
        }
        lineup("01_orques", 7.5, 2F, 2.0, List.of(ModEntities.ORC.get(), ModEntities.ORC.get(), ModEntities.WAR_ORC.get(), ModEntities.ORC_CHIEF.get(), ModEntities.GOBLIN.get(),
                ModEntities.WILD_MAN.get()));
        // gobelins : les 3 types (1,2 / 1,5 / 1,8 m) et leur roi (2,7 m), de face puis de profil ; on journalise les hauteurs reelles de hitbox
        for (float yaw : new float[]{0F, 90F}) {
            SCENES.add(new Scene(yaw == 0F ? "09_gobelins" : "09b_gobelins_profil", 70, p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                int[] vs = {0, 4, 8, 0};
                double[] xs = {SX + 5.2, SX + 3.0, SX + 0.6, SX - 3.4};
                List<FoeEntity> all = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    final int v = vs[i];
                    all.add(actor(ow, i < 3 ? ModEntities.GOBLIN.get() : ModEntities.GOBLIN_KING.get(), xs[i], SZ + 9.5, o -> {
                        o.testVariant(v);
                        o.setYRot(180F - yaw); o.setYHeadRot(180F - yaw); o.setYBodyRot(180F - yaw);
                    }));
                }
                view(p, ow, SX + 0.5, SY + 1.6, SZ, 0F, 4F);
                hook = () -> log("GOB_DIMS " + all.stream().map(f -> String.format("%.2f", f.getBbHeight())).toList()
                        + " pv=" + all.stream().map(f -> String.format("%.0f", f.getMaxHealth())).toList());
            }));
        }
        SCENES.add(new Scene("09c_gobelin_peaux", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            double x = SX + 4.0;
            for (int t = 0; t < 3; t++) {
                for (int sk = 0; sk < 3; sk++) {
                    final int v = t * 3 + sk;
                    actor(ow, ModEntities.GOBLIN.get(), SX + 2.8 - sk * 2.2 + 0.0 * x, SZ + 5.5 + t * 3.5, o -> o.testVariant(v));
                }
            }
            view(p, ow, SX + 0.5, SY + 4.5, SZ, 0F, 22F);
        }));
        SCENES.add(new Scene("09d_roi_marche", 140, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            // vraie apparition du roi, IA active : on mesure s'il avance (et la cour de gobelins qui l'accompagne)
            FoeEntity king = ModEntities.GOBLIN_KING.get().create(ow);
            king.moveTo(SX, SY, SZ + 14, 180F, 0F);
            king.finalizeSpawn(ow, ow.getCurrentDifficultyAt(king.blockPosition()), MobSpawnType.NATURAL, null);
            king.setPersistenceRequired();
            ow.addFreshEntity(king);
            net.minecraft.world.entity.monster.Spider bait = EntityType.SPIDER.create(ow);
            bait.moveTo(SX + 3, SY, SZ + 2, 0F, 0F);
            bait.setNoAi(true); bait.setPersistenceRequired();
            ow.addFreshEntity(bait);
            final double z0 = king.getZ();
            view(p, ow, SX + 0.5, SY + 3.5, SZ - 6, 0F, 14F);
            hook = () -> log("GOB_ROI_MARCHE avance=" + String.format("%.2f", z0 - king.getZ()) + " pv=" + king.getHealth() + " vivant=" + king.isAlive() + " cible=" + king.getTarget()
                    + " cour=" + ow.getEntitiesOfClass(FoeEntity.class, king.getBoundingBox().inflate(40), o -> o.kind() == FoeEntity.Kind.GOBLIN).size()
                    + " baton=" + king.getMainHandItem().getItem());
        }));
        // araignee geante : face, profil, marche (elle poursuit une vache : on mesure l'avance et les toiles qu'elle crache), bloc de toile
        for (float yaw : new float[]{0F, 90F}) {
            SCENES.add(new Scene(yaw == 0F ? "10_araignee" : "10b_araignee_profil", 70, p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                var sp = actor(ow, ModEntities.GIANT_SPIDER.get(), SX, SZ + 8.5, o -> { o.setYRot(180F - yaw); o.setYHeadRot(180F - yaw); o.setYBodyRot(180F - yaw); });
                view(p, ow, SX + 0.5, SY + 1.7, SZ, 0F, 6F);
                hook = () -> log("SPIDER_DIMS larg=" + String.format("%.2f", sp.getBbWidth()) + " haut=" + String.format("%.2f", sp.getBbHeight()) + " pv=" + sp.getMaxHealth());
            }));
        }
        SCENES.add(new Scene("10c_araignee_marche", 150, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            net.minecraft.world.entity.animal.Cow cow = actor(ow, EntityType.COW, SX + 1.0, SZ + 3.0, null);
            var sp = ModEntities.GIANT_SPIDER.get().create(ow);
            sp.moveTo(SX - 2.0, SY, SZ + 22, 180F, 0F);
            sp.finalizeSpawn(ow, ow.getCurrentDifficultyAt(sp.blockPosition()), MobSpawnType.COMMAND, null);
            sp.setPersistenceRequired();
            sp.setTarget(cow);
            ow.addFreshEntity(sp);
            final double z0 = sp.getZ();
            view(p, ow, SX + 0.5, SY + 2.2, SZ - 3, 0F, 8F);
            hook = () -> {
                int webs = 0;
                for (BlockPos q : BlockPos.betweenClosed(BlockPos.containing(SX - 6, SY - 1, SZ - 2), BlockPos.containing(SX + 8, SY + 3, SZ + 12)))
                    if (ow.getBlockState(q).is(net.ringsofash.gen.Reg.SPIDER_WEB.get())) webs++;
                log("SPIDER_MARCHE avance=" + String.format("%.2f", z0 - sp.getZ()) + " pv=" + sp.getHealth() + " vivante=" + sp.isAlive() + " cible=" + sp.getTarget() + " toiles=" + webs + " vache=" + cow.isAlive());
            };
        }));
        SCENES.add(new Scene("10d_toiles", 40, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            for (int i = 0; i < 6; i++) ow.setBlock(BlockPos.containing(SX - 2 + i * 1.2, SY, SZ + 4 + (i % 2)), net.ringsofash.gen.Reg.SPIDER_WEB.get().defaultBlockState(), 3);
            ow.setBlock(BlockPos.containing(SX, SY + 1, SZ + 4), net.ringsofash.gen.Reg.SPIDER_WEB.get().defaultBlockState(), 3);
            ow.setBlock(BlockPos.containing(SX + 1, SY + 2, SZ + 4), net.ringsofash.gen.Reg.SPIDER_WEB.get().defaultBlockState(), 3);
            view(p, ow, SX + 1.0, SY + 1.8, SZ + 0.5, 0F, 12F);
            hook = () -> ow.setBlock(BlockPos.containing(SX + 1, SY, SZ + 3), Blocks.AIR.defaultBlockState(), 3);
        }));
        SCENES.add(new Scene("09e_roi_gros_plan", 100, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            FoeEntity king = ModEntities.GOBLIN_KING.get().create(ow);
            king.moveTo(SX, SY, SZ + 9, 180F, 0F);
            king.finalizeSpawn(ow, ow.getCurrentDifficultyAt(king.blockPosition()), MobSpawnType.COMMAND, null);
            king.setPersistenceRequired();
            ow.addFreshEntity(king);
            view(p, ow, SX + 3.5, SY + 2.2, SZ + 3.0, 25F, 6F);
        }));
        SCENES.add(new Scene("03d_roi_marche", 140, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            // vraie apparition (comme dans le jeu) du roi, IA active, avec une cible : on mesure s'il avance
            FoeEntity king = ModEntities.DEAD_SOLDIER.get().create(ow);
            king.moveTo(SX, SY, SZ + 16, 180F, 0F);
            king.finalizeSpawn(ow, ow.getCurrentDifficultyAt(king.blockPosition()), MobSpawnType.NATURAL, null);
            king.testVariant(4);
            king.setPersistenceRequired();
            ow.addFreshEntity(king);
            net.minecraft.world.entity.monster.Spider bait = EntityType.SPIDER.create(ow);
            bait.moveTo(SX + 3, SY, SZ + 3, 0F, 0F);
            bait.setNoAi(true); bait.setPersistenceRequired();
            ow.addFreshEntity(bait);
            final double z0 = king.getZ();
            view(p, ow, SX + 0.5, SY + 3.5, SZ - 6, 0F, 14F);
            hook = () -> log("ROI_MARCHE avance=" + String.format("%.2f", z0 - king.getZ()) + " pv=" + king.getHealth() + " vivant=" + king.isAlive() + " cible=" + king.getTarget()
                    + " model=" + king.spectreModel());
        }));
        SCENES.add(new Scene("04_armes_orques", 90, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            net.minecraft.world.item.Item[] it = {net.minecraft.world.item.Items.AIR, net.minecraft.world.item.Items.AIR, net.minecraft.world.item.Items.AIR, net.minecraft.world.item.Items.AIR, net.minecraft.world.item.Items.AIR, net.minecraft.world.item.Items.AIR, Reg.GOBLIN_PICKAXE.get()};
            for (int i = 0; i < it.length; i++) {
                net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(ow, SX + 2.0 - i * 0.8, SY + 0.1, SZ + 3.0, new net.minecraft.world.item.ItemStack(it[i]));
                ie.setPickUpDelay(32767);
                ie.setUnlimitedLifetime();
                ow.addFreshEntity(ie);
            }
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, 18F);
        }));
        SCENES.add(new Scene("03e_ombres", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            double x = SX + 2.4;
            for (int v = 0; v < 2; v++) {
                final int vv = v;
                actor(ow, ModEntities.SHADOW_RIDER.get(), x, SZ + 9, o -> o.testVariant(vv));
                x -= 4.8;
            }
            view(p, ow, SX + 0.5, SY + 1.6, SZ, 0F, 4F);
        }));
        // le chef des ombres arrive sur son dragon (un seul oeuf) : vue de cote, puis chef tue -> dragon libre
        SCENES.add(new Scene("03f_chef_dragon", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var c = actor(ow, ModEntities.SHADOW_CHIEF.get(), SX, SZ + 9, o -> { });
            Entity dr = c.getVehicle();
            log("CHEF_DRAGON monte=" + (dr instanceof net.ringsofash.entity.ShadowDragonEntity) + " chefPV=" + c.getMaxHealth());
            if (dr instanceof net.ringsofash.entity.ShadowDragonEntity d) {
                d.setNoAi(true); d.setNoGravity(true);
                d.moveTo(SX, SY + 1.5, SZ + 9, 90F, 0F); d.setYBodyRot(90F); d.setYHeadRot(90F);
                c.setYRot(90F); c.setYBodyRot(90F); c.setYHeadRot(90F);
            }
            view(p, ow, SX + 0.5, SY + 3.5, SZ, 0F, 10F);
        }));
        // dragon des ombres : profil a deux instants (le cou ondule, les ailes battent), vue de face, puis vrai vol avec IA (on mesure qu'il rejoint et mord sa cible)
        for (int[] d : new int[][]{{60, 0}, {81, 1}}) {
            final int dd = d[1];
            SCENES.add(new Scene("14" + "ab".charAt(dd) + "_dragon_profil", d[0], p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                var dr = actor(ow, ModEntities.SHADOW_DRAGON.get(), SX, SZ + 20, o -> { o.setYRot(90F); o.setYHeadRot(90F); o.setYBodyRot(90F); });
                dr.setNoGravity(true); dr.moveTo(SX, SY + 2.0, SZ + 20, 90F, 0F); dr.setYBodyRot(90F); dr.setYHeadRot(90F);
                view(p, ow, SX + 0.5, SY + 5.5, SZ - 2, 0F, 8F);
            }));
        }
        SCENES.add(new Scene("14c_dragon_face", 60, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var dr = actor(ow, ModEntities.SHADOW_DRAGON.get(), SX, SZ + 18, o -> { });
            dr.setNoGravity(true); dr.moveTo(SX, SY + 2.0, SZ + 18, 180F, 0F);
            view(p, ow, SX + 0.5, SY + 5.0, SZ, 0F, 8F);
        }));
        SCENES.add(new Scene("14d_dragon_vol", 160, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var dr = net.ringsofash.entity.ModEntities.SHADOW_DRAGON.get().create(ow);
            dr.moveTo(SX, SY + 8, SZ + 26, 180F, 0F);
            dr.finalizeSpawn(ow, ow.getCurrentDifficultyAt(dr.blockPosition()), MobSpawnType.COMMAND, null);
            dr.setPersistenceRequired();
            ow.addFreshEntity(dr);
            net.minecraft.world.entity.monster.Spider bait = EntityType.SPIDER.create(ow);
            bait.moveTo(SX + 2, SY, SZ + 6, 0F, 0F);
            bait.setNoAi(true); bait.setPersistenceRequired();
            ow.addFreshEntity(bait);
            dr.setTarget(bait);
            final double x0 = dr.getX(), y0 = dr.getY(), z0 = dr.getZ();
            view(p, ow, SX + 0.5, SY + 6.0, SZ - 8, 0F, 10F);
            hook = () -> log("DRAGON_VOL avance=" + String.format("%.2f", z0 - dr.getZ()) + " dy=" + String.format("%.2f", dr.getY() - y0) + " distCible=" + String.format("%.2f", dr.distanceTo(bait))
                    + " pvCible=" + bait.getHealth() + " vivant=" + dr.isAlive() + " cible=" + dr.getTarget() + " auSol=" + dr.onGround());
        }));
        // mobs qui ne se deplacent qu'en volant : poses en vol (profil) puis vrai vol avec IA
        SCENES.add(new Scene("18a_vol_oiseaux", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var eg = actor(ow, ModEntities.EAGLE.get(), SX + 2.2, SZ + 9, o -> { });
            var cr = actor(ow, ModEntities.CROW.get(), SX - 1.4, SZ + 9, o -> { });
            for (Mob m : new Mob[]{eg, cr}) { m.setNoGravity(true); m.moveTo(m.getX(), SY + 3.0, m.getZ(), 90F, 0F); m.setYBodyRot(90F); m.setYHeadRot(90F); }
            view(p, ow, SX + 0.5, SY + 3.6, SZ, 0F, 0F);
        }));
        SCENES.add(new Scene("18b_ver_vol", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var w = actor(ow, ModEntities.DRAGON.get(), SX, SZ + 24, o -> { });
            w.setNoGravity(true); w.moveTo(SX, SY + 4.0, SZ + 24, 90F, 0F); w.setYBodyRot(90F); w.setYHeadRot(90F);
            view(p, ow, SX + 0.5, SY + 6.0, SZ, 0F, 4F);
        }));
        SCENES.add(new Scene("18c_chef_vol", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var c = actor(ow, ModEntities.SHADOW_CHIEF.get(), SX, SZ + 8, o -> { });
            Entity dr = c.getVehicle();
            c.stopRiding();
            if (dr != null) dr.discard();
            c.setNoGravity(true); c.moveTo(SX, SY + 2.0, SZ + 8, 90F, 0F); c.setYBodyRot(90F); c.setYHeadRot(90F);
            view(p, ow, SX + 0.5, SY + 3.0, SZ, 0F, 4F);
        }));
        SCENES.add(new Scene("18d_vol_ia", 240, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            List<Mob> fl = new ArrayList<>();
            double[] xs = {-6, -2, 2, 6};
            EntityType<?>[] ts = {ModEntities.EAGLE.get(), ModEntities.CROW.get(), ModEntities.DRAGON.get(), ModEntities.SHADOW_CHIEF.get()};
            for (int i = 0; i < 4; i++) {
                Mob m = (Mob) ts[i].create(ow);
                m.moveTo(SX + xs[i], SY + 0.5, SZ + 14, 180F, 0F);
                m.finalizeSpawn(ow, ow.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
                m.setPersistenceRequired();
                ow.addFreshEntity(m);
                if (m.getVehicle() != null) { Entity v = m.getVehicle(); m.stopRiding(); v.discard(); }
                fl.add(m);
            }
            net.minecraft.world.entity.monster.Spider bait = EntityType.SPIDER.create(ow);
            bait.moveTo(SX, SY, SZ + 4, 0F, 0F);
            bait.setNoAi(true); bait.setPersistenceRequired();
            ow.addFreshEntity(bait);
            fl.get(2).setTarget(bait); fl.get(3).setTarget(bait);
            final double[] y0 = new double[4];
            for (int i = 0; i < 4; i++) y0[i] = fl.get(i).getY();
            final int[] ground = new int[4];
            view(p, ow, SX + 0.5, SY + 7.0, SZ - 6, 0F, 8F);
            hook = () -> {
                StringBuilder sb = new StringBuilder("VOL_IA");
                String[] nm = {"aigle", "corbeau", "ver", "chef"};
                for (int i = 0; i < 4; i++) {
                    Mob m = fl.get(i);
                    if (m.onGround()) ground[i]++;
                    sb.append(' ').append(nm[i]).append("[vivant=").append(m.isAlive()).append(" dy=").append(String.format("%.1f", m.getY() - y0[i])).append(" auSol=").append(ground[i])
                            .append(" cible=").append(m.getTarget() != null).append(" dBait=").append(String.format("%.1f", m.distanceTo(bait))).append(']');
                }
                log(sb.toString());
            };
        }));
        // maitre tue : le dragon reste sans cavalier, selle de metal nue
        SCENES.add(new Scene("15a_dragon_libre", 40, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var dr = actor(ow, ModEntities.SHADOW_DRAGON.get(), SX, SZ + 14, o -> { });
            dr.setNoGravity(true);
            dr.moveTo(SX, SY + 1.0, SZ + 14, 90F, 0F); dr.setYBodyRot(90F); dr.setYHeadRot(90F);
            for (Entity pa : new ArrayList<>(dr.getPassengers())) { pa.stopRiding(); pa.discard(); }
            view(p, ow, SX + 0.5, SY + 3.5, SZ - 2, 0F, 6F);
        }));
        // 64 os : le dragon est apprivoise et le joueur est assis sur la selle (camera derriere)
        SCENES.add(new Scene("15b_dragon_monte", 60, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var dr = actor(ow, ModEntities.SHADOW_DRAGON.get(), SX, SZ + 14, o -> { });
            for (Entity pa : new ArrayList<>(dr.getPassengers())) { pa.stopRiding(); pa.discard(); }
            dr.setNoGravity(true);
            dr.moveTo(SX, SY + 3.0, SZ + 14, 180F, 0F);
            view(p, ow, SX + 0.5, SY + 3.0, SZ + 8, 180F, 15F);
            p.getAbilities().flying = false; p.onUpdateAbilities();
            p.getInventory().selected = 0;
            final int[] poor = {0};
            p.getInventory().setItem(0, new ItemStack(Items.BONE, 20));
            log("MONTE_PEU " + dr.interact(p, InteractionHand.MAIN_HAND) + " monte=" + (p.getVehicle() == dr) + " tamed=" + dr.isTamed());
            p.getInventory().setItem(0, new ItemStack(Items.BONE, 64));
            var r = dr.interact(p, InteractionHand.MAIN_HAND);
            log("MONTE_64 " + r + " monte=" + (p.getVehicle() == dr) + " tamed=" + dr.isTamed() + " os=" + p.getInventory().getItem(0).getCount() + " ctrl=" + (dr.getControllingPassenger() == p));
            hook = () -> log("MONTE_FIN monte=" + (p.getVehicle() == dr) + " dyJoueur=" + String.format("%.2f", p.getY() - dr.getY()) + " dragonY=" + String.format("%.2f", dr.getY()));
        }));
        // monte + Espace maintenu (monter) : le dragon doit prendre de l'altitude sans avancer
        SCENES.add(new Scene("15c_dragon_monte_vol", 80, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var dr = actor(ow, ModEntities.SHADOW_DRAGON.get(), SX, SZ + 14, o -> { });
            dr.moveTo(SX, SY + 3.0, SZ + 14, 180F, 0F);
            view(p, ow, SX + 0.5, SY + 3.0, SZ + 8, 180F, 15F);
            p.getAbilities().flying = false; p.onUpdateAbilities();
            p.getInventory().selected = 0;
            p.getInventory().setItem(0, new ItemStack(Items.BONE, 64));
            dr.interact(p, InteractionHand.MAIN_HAND);
            net.ringsofash.entity.ShadowDragonEntity.VERTICAL = pl -> 1;
            final double y0 = dr.getY();
            hook = () -> log("DRAGON_MONTE_VOL monte=" + (p.getVehicle() == dr) + " dy=" + String.format("%.2f", dr.getY() - y0) + " ctrl=" + (dr.getControllingPassenger() == p));
        }));
        // aigle geant : on monte dessus les mains vides, puis Espace maintenu : il doit prendre de l altitude
        SCENES.add(new Scene("15d_aigle_monte_vol", 80, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            var ea = actor(ow, ModEntities.EAGLE.get(), SX, SZ + 14, o -> { });
            ea.moveTo(SX, SY + 3.0, SZ + 14, 180F, 0F);
            view(p, ow, SX + 0.5, SY + 3.0, SZ + 8, 180F, 15F);
            p.getAbilities().flying = false; p.onUpdateAbilities();
            p.getInventory().selected = 0;
            p.getInventory().setItem(0, ItemStack.EMPTY);
            ea.interact(p, InteractionHand.MAIN_HAND);
            net.ringsofash.entity.ShadowDragonEntity.VERTICAL = pl -> 1;
            final double y0 = ea.getY();
            hook = () -> log("AIGLE_MONTE_VOL monte=" + (p.getVehicle() == ea) + " dy=" + String.format("%.2f", ea.getY() - y0) + " ctrl=" + (ea.getControllingPassenger() == p));
        }));
        // coup d'epee : vue subjective puis troisieme personne de face, a quelques ticks du debut du geste
        for (int n : new int[]{3, 4, 5, 6}) SCENES.add(new Scene("16a_coup_1p_" + n, n, p -> swingScene(p)));
        for (int n : new int[]{3, 4, 5, 6, 7}) SCENES.add(new Scene("16b_coup_face_" + n, n, p -> swingScene(p)));
        // armes en 3D : au sol (toutes), puis tenues en main en vue subjective (avec la barre d'objets : icones) et en troisieme personne
        SCENES.add(new Scene("17a_armes_sol", 40, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            String[] ids = {"lunargent_sword", "elven_blade", "elven_glaive", "frost_blade", "blood_blade", "viper_dagger", "storm_blade", "sentinel_blade", "sun_blade", "halfling_dagger",
                    "ranger_sword", "wight_blade", "kings_blade", "valmoire_blade", "dwarven_axe", "dwarven_hammer", "ash_mace", "rider_spear", "troll_club", "lunargent_pickaxe", "elven_bow",
                    "staff_fire", "staff_light", "staff_storm", "staff_healing"};
            for (int i = 0; i < ids.length; i++) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(RingsOfAsh.id(ids[i]));
                var ie = new net.minecraft.world.entity.item.ItemEntity(ow, SX - 6 + (i % 9) * 1.5, SY + 0.6 + (i / 9) * 1.6, SZ + 7.0, new ItemStack(item));
                ie.setNoGravity(true); ie.setPickUpDelay(32767); ie.setDeltaMovement(0, 0, 0);
                ow.addFreshEntity(ie);
            }
            view(p, ow, SX + 0.5, SY + 3.0, SZ - 2, 0F, 12F);
        }));
        for (String id : new String[]{"valmoire_blade", "elven_bow", "staff_storm", "dwarven_hammer"}) SCENES.add(new Scene("17b_arme_1p_" + id, 20, p -> heldScene(p, id, false)));
        for (String id : new String[]{"valmoire_blade", "elven_bow", "staff_fire", "dwarven_hammer", "rider_spear"}) SCENES.add(new Scene("17c_arme_face_" + id, 20, p -> heldScene(p, id, true)));
        // arc elfique bande : vue subjective, de face, de dos
        for (String nm : new String[]{"17e_arc_bande_1p", "17f_arc_bande_face", "17g_arc_bande_monte"}) SCENES.add(new Scene(nm, 24, p -> {
            heldScene(p, "elven_bow", false);
            p.getInventory().setItem(1, new ItemStack(Items.ARROW, 16));
            p.startUsingItem(InteractionHand.MAIN_HAND);
        }));
        SCENES.add(new Scene("17h_arc_repos_1p", 20, p -> heldScene(p, "elven_bow", false)));
        // armes tenues vues de profil (mannequins), pour juger l'orientation et la taille en main
        String[][] profils = {{"valmoire_blade", "kings_blade", "elven_glaive", "rider_spear", "dwarven_axe", "dwarven_hammer", "ash_mace", "troll_club"},
                {"lunargent_pickaxe", "elven_bow", "staff_fire", "staff_light", "staff_storm", "staff_healing", "viper_dagger", "frost_blade"}};
        for (int k = 0; k < profils.length; k++) {
            final String[] set = profils[k];
            SCENES.add(new Scene("17d_arme_profil_" + k, 25, p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                for (int i = 0; i < set.length; i++) {
                    var as = EntityType.ARMOR_STAND.create(ow);
                    as.moveTo(SX - 5.6 + i * 1.6, SY, SZ + 7, 90F, 0F);
                    as.setShowArms(true);
                    as.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(RingsOfAsh.id(set[i]))));
                    ow.addFreshEntity(as);
                }
                view(p, ow, SX + 0.5, SY + 1.6, SZ + 0.5, 0F, 5F);
            }));
        }
        spectreLine("03b_spectres", 9.0, 3F, 2.2);
        spectreLine("03c_roi_proche", 4.2, 6F, 2.2);
        orcLine("01b_orques_modeles", 11.0, 3F, 2.3, new int[]{0, 5, 10, 15, 2, 3, 0}, true);
        orcLine("01c_orques_couleurs", 11.0, 3F, 2.3, new int[]{4, 5, 6, 7, 8, 9, 12}, false);
        orcLine("01d_chef_proche", 4.5, 6F, 3.0, new int[]{0}, true);
        lineup("02_peuples", 8.5, 2F, 1.9, List.of(ModEntities.HUMAN.get(), ModEntities.HUMAN.get(), ModEntities.ELF.get(), ModEntities.ELF.get(), ModEntities.DWARF.get(), ModEntities.DWARF.get(),
                ModEntities.HALFLING.get(), ModEntities.HALFLING.get(), ModEntities.RANGER.get(), ModEntities.MAGE.get()));
        halflingLine("semi_a_hobbits_01_10", 9.0, 2F, 1.5, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        halflingLine("semi_b_hobbits_11_20", 9.0, 2F, 1.5, new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        halflingLine("semi_c_hobbits_21_30", 9.0, 2F, 1.5, new int[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29});
        halflingLine("semi_d_proches_a", 4.2, 4F, 1.3, new int[]{0, 4, 9, 12});
        halflingLine("semi_e_proches_b", 4.2, 4F, 1.3, new int[]{17, 21, 22, 27});
        lineup("03_divers", 7.0, 2F, 2.2, List.of(ModEntities.CAVE_THIEF.get(), ModEntities.SORCERER.get(), ModEntities.DEAD_SOLDIER.get(), ModEntities.CROW.get(), ModEntities.STAG.get(), ModEntities.WARG.get()));
        lineup("04_betes", 12.0, -4F, 5.5, List.of(ModEntities.CAVE_TROLL.get(), ModEntities.TROLL_CHIEF.get(), ModEntities.STONE_TROLL.get(), ModEntities.GIANT_SPIDER.get(), ModEntities.EAGLE.get(), ModEntities.LURKER.get()));
        // poses des trolls (profil + trois quarts) : course a quatre pattes, poings leves, poings abattus
        {
            Object[][] poses = {{"04b_troll_course", true, 0, 90F}, {"04c_troll_leve", false, 17, 90F}, {"04d_troll_frappe", false, 22, 90F}, {"04e_troll_face", true, 0, 20F}, {"04f_troll_leve_face", false, 17, 35F}};
            for (Object[] o : poses) {
                SCENES.add(new Scene((String) o[0], 60, p -> {
                    ServerLevel ow = p.server.overworld();
                    clear(ow);
                    float yaw = (Float) o[3];
                    for (int i = 0; i < 2; i++) {
                        EntityType<? extends Mob> type = i == 0 ? ModEntities.CAVE_TROLL.get() : ModEntities.TROLL_CHIEF.get();
                        double x = SX + (i == 0 ? 2.6 : -2.6);
                        actor(ow, type, x, SZ + 9, m -> {
                            m.setYRot(yaw); m.setYHeadRot(yaw); m.setYBodyRot(yaw);
                            if (m instanceof FoeEntity f) f.testPose((Boolean) o[1], (Integer) o[2]);
                        });
                    }
                    view(p, ow, SX + 0.5, SY + 2.0, SZ, 0F, 4F);
                }));
            }
        }
        lineup("05b_seigneur", 9.0, -3F, 4.0, List.of(ModEntities.ASH_LORD.get()));
        lineup("05b_seigneur_proche", 5.5, 4F, 4.0, List.of(ModEntities.ASH_LORD.get()));
        SCENES.add(new Scene("12a0_survie", 10, p -> p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)));
        SCENES.add(new Scene("12a_onglet_inventaire", 40, p -> { }));
        SCENES.add(new Scene("12b_anneaux_menu", 50, p -> {
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var l = net.ringsofash.data.ModData.lore(p);
            l.rings.clearContent();
            net.minecraft.world.item.Item[] rs = {Reg.RING_WATERS.get(), Reg.RING_WINDS.get(), Reg.RING_EMBERS.get(), Reg.RING_FORGE.get(), Reg.RING_HOARD.get(), Reg.RING_KINGS.get(), Reg.RING_SHADOWS.get(), Reg.ASH_RING.get()};
            for (int i = 0; i < rs.length; i++) l.rings.setItem(i + (i > 4 ? 1 : 0), new ItemStack(rs[i]));
            l.activeRing = 2;
            net.ringsofash.data.Net.sync(p);
            net.ringsofash.menu.RingsMenu.open(p);
        }));
        SCENES.add(new Scene("12c_anneaux_roue", 30, p -> { }));
        SCENES.add(new Scene("12e_artefacts_menu", 50, p -> {
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var l = net.ringsofash.data.ModData.lore(p);
            l.artifacts.clearContent();
            l.artifacts.setItem(0, new ItemStack(Reg.ARTIFACT_CHAIN_3.get()));
            l.artifacts.setItem(1, net.ringsofash.item.Artifacts.artifact(Reg.DRAGON_SCALE.get()));
            l.artifacts.setItem(2, net.ringsofash.item.Artifacts.artifact(Reg.GIANT_EAGLE_FEATHER.get()));
            l.artifacts.setItem(3, net.ringsofash.item.Artifacts.artifact(Reg.HORN.get()));
            net.ringsofash.menu.ArtifactMenu.open(p);
        }));
        SCENES.add(new Scene("12f_artefacts_chaine1", 50, p -> {
            var l = net.ringsofash.data.ModData.lore(p);
            l.artifacts.clearContent();
            l.artifacts.setItem(0, new ItemStack(Reg.ARTIFACT_CHAIN_1.get()));
            l.artifacts.setItem(1, new ItemStack(Reg.HORN.get()));                       // corne ordinaire : rangee mais sans effet
            net.ringsofash.menu.ArtifactMenu.open(p);
        }));
        SCENES.add(new Scene("12g_artefacts_effets", 100, p -> {
            p.closeContainer();
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var l = net.ringsofash.data.ModData.lore(p);
            l.artifacts.clearContent();
            l.artifacts.setItem(0, new ItemStack(Reg.ARTIFACT_CHAIN_4.get()));
            l.artifacts.setItem(1, net.ringsofash.item.Artifacts.artifact(Reg.DRAGON_SCALE.get()));
            l.artifacts.setItem(2, net.ringsofash.item.Artifacts.artifact(Reg.GIANT_EAGLE_FEATHER.get()));
            l.artifacts.setItem(3, net.ringsofash.item.Artifacts.artifact(Reg.HORN.get()));
            l.artifacts.setItem(4, new ItemStack(Reg.DRAGON_SCALE.get()));
            hook = () -> log("12g artefacts : feu=" + p.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE) + " force=" + p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                    + " chute lente=" + p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING) + " icone visible=" + p.getActiveEffects().stream().anyMatch(e -> e.isVisible() || e.showIcon()));
        }));
        SCENES.add(new Scene("12d_anneaux_actif", 80, p -> {
            net.ringsofash.menu.RingsMenu.select(p, 4);
            hook = () -> log("12d anneau actif=" + net.ringsofash.item.RingItem.worn(p) + " chance=" + p.hasEffect(net.minecraft.world.effect.MobEffects.LUCK) + " nuit=" + p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION));
        }));
        for (int[] v : new int[][]{{0, 14, -6, 40}, {1, 8, -4, 40}, {2, 8, -4, 110}}) {
            final int close = v[0];
            SCENES.add(new Scene("11" + "abc".charAt(close) + "_sylvestres", v[3], p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                double x = SX + 6.0;
                for (int var = 0; var < 3; var++) { final int vv = var; actor(ow, ModEntities.TREEMAN.get(), x, SZ + v[1] + (close == 0 ? 0 : 3.0), t -> t.setVariant(vv)); x -= 6.0; }
                view(p, ow, SX + 0.5, SY + 2.6, SZ, 0F, close == 0 ? -4F : v[2]);
            }));
        }
        lineup("05_geants", 15.0, -8F, 7.0, List.of(ModEntities.MAMMOTH.get(), ModEntities.TREEMAN.get(), ModEntities.FIRE_DEMON.get(), ModEntities.ASH_LORD.get()));
        lineup("05b_montures", 10.0, -4F, 3.2, List.of(ModEntities.WAR_BOAR.get(), ModEntities.STAG.get(), ModEntities.EAGLE.get(), ModEntities.CROW.get()));
        for (float[] v : new float[][]{{0, 155F}, {1, -25F}, {2, 90F}}) {
            SCENES.add(new Scene("05" + "bcd".charAt((int) v[0]) + "_fleau", 70, p -> {
                ServerLevel ow = p.server.overworld();
                clear(ow);
                actor(ow, ModEntities.FIRE_DEMON.get(), SX, SZ + 14, m -> { m.setYRot(v[1]); m.setYHeadRot(v[1]); m.setYBodyRot(v[1]); });
                view(p, ow, SX + 0.5, SY + 3.5, SZ, 0F, 0F);
            }));
        }
        SCENES.add(new Scene("06_dragon", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            actor(ow, ModEntities.DRAGON.get(), SX, SZ + 10, m -> { m.setYRot(120F); m.setYHeadRot(120F); m.setYBodyRot(120F); });
            view(p, ow, SX + 0.5, SY + 3, SZ, 0F, 8F);
        }));
        SCENES.add(new Scene("07_armures", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            Item[][] sets = {
                    {Reg.LUNARGENT_HELMET.get(), Reg.LUNARGENT_CHESTPLATE.get(), Reg.LUNARGENT_LEGGINGS.get(), Reg.LUNARGENT_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                    {Reg.ORC_HELMET.get(), Reg.ORC_CHESTPLATE.get(), Reg.ORC_LEGGINGS.get(), Reg.ORC_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                    {Reg.ELVEN_HELMET.get(), Reg.ELVEN_CHESTPLATE.get(), Reg.ELVEN_LEGGINGS.get(), Reg.ELVEN_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                    {Reg.DWARVEN_HELMET.get(), Reg.DWARVEN_CHESTPLATE.get(), Reg.DWARVEN_LEGGINGS.get(), Reg.DWARVEN_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                    {Reg.ASHLORD_HELMET.get(), Reg.ASHLORD_CHESTPLATE.get(), Reg.ASHLORD_LEGGINGS.get(), Reg.ASHLORD_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                    {Reg.ELFKING_HELMET.get(), Reg.ELFKING_CHESTPLATE.get(), Reg.ELFKING_LEGGINGS.get(), Reg.ELFKING_BOOTS.get(), net.minecraft.world.item.Items.AIR},
                    {null, Reg.ELVEN_CLOAK.get(), null, null, net.minecraft.world.item.Items.AIR}};
            double x = SX + 6;
            for (Item[] set : sets) {
                actor(ow, ModEntities.HUMAN.get(), x, SZ + 6, m -> {
                    EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
                    for (int i = 0; i < 5; i++) if (set[i] != null) m.setItemSlot(slots[i], new ItemStack(set[i]));
                });
                x -= 2;
            }
            view(p, ow, SX + 0.5, SY + 1.2, SZ, 0F, 2F);
        }));
        SCENES.add(new Scene("08_objets", 90, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            int i = 0, cols = 22;
            for (var e : Reg.ITEMS.getEntries()) {
                if (e.get() instanceof net.minecraft.world.item.SpawnEggItem) continue;
                int cx = SX + cols / 2 - (i % cols), cy = SY + 6 - (i / cols);
                BlockPos wall = new BlockPos(cx, cy, SZ + 12);
                ow.setBlock(wall, Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
                ItemFrame f = new ItemFrame(ow, wall.north(), Direction.NORTH);
                f.setItem(new ItemStack(e.get()), false);
                ow.addFreshEntity(f);
                i++;
            }
            view(p, ow, SX + 0.5, SY + 2.0, SZ - 2, 0F, 0F);
        }));
        SCENES.add(new Scene("09_blocs", 90, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            for (int x = -14; x <= 14; x++) for (int y = 0; y <= 8; y++) ow.setBlock(new BlockPos(SX + x, SY + y, SZ + 12), Blocks.AIR.defaultBlockState(), 2);
            int i = 0, cols = 12;
            for (var e : Reg.BLOCKS.getEntries()) {
                BlockPos at = new BlockPos(SX + 11 - (i % cols) * 2, SY + (i / cols) * 2, SZ + 9);
                ow.setBlock(at.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                ow.setBlock(at, e.get().defaultBlockState(), 2 | 16);
                i++;
            }
            view(p, ow, SX + 0.5, SY + 4.0, SZ - 3, 0F, 0F);
        }));
        SCENES.add(new Scene("10_volcan", 500, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            StructureStart st = SelfTest.place(ow, ModWorldgen.VOLCANO_STRUCTURE, new BlockPos(600, 0, 600));
            VolcanoPiece piece = (VolcanoPiece) st.getPieces().get(0);
            volcano = piece.center(); volcanoRim = piece.rimY();
            view(p, ow, volcano.getX() - 105, volcano.getY() + 62, volcano.getZ() - 105, -45F, 8F);
        }));
        SCENES.add(new Scene("11_cratere", 200, p -> view(p, p.server.overworld(), volcano.getX() + 0.5, volcanoRim + 22, volcano.getZ() - 14, 0F, 62F)));
        SCENES.add(new Scene("12_lave_pale", 120, p -> {
            ServerLevel ow = p.server.overworld();
            Travel.awaken(ow, new BlockPos(volcano.getX(), volcanoRim - 9, volcano.getZ()));
        }));
        SCENES.add(new Scene("13_arrivee", 320, p -> {
            ModData.lore(p).setOverworld(new BlockPos(SX, SY, SZ));
            Travel.toLands(p);
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            lands.setDayTime(6000);
            p.teleportTo(lands, p.getX(), p.getY(), p.getZ(), 135F, 5F);
        }));
        spot("14_plaines_vertes", -445, -230, 30, 160F, 12F);
        spot("15_grande_foret", 250, -297, 60, -45F, 25F);
        spot("16_plaines_jaunes", 800, 100, 30, 0F, 10F);
        spot("17_cimes", -90, -144, 120, -90F, 8F);
        spot("18_mer", -740, 0, 25, 90F, 6F);
        spot("19_plaines_cendre", 600, 700, 40, 90F, 15F);
        SCENES.add(new Scene("24_reputation", 60, p -> {
            for (var pp : net.ringsofash.data.People.values()) ModData.lore(p).rep[pp.ordinal()] = new int[]{35, 60, -20, 12}[pp.ordinal()];
            net.ringsofash.data.Net.sync(p);
        }));
        SCENES.add(new Scene("25_quetes", 60, p -> {
            ModData.lore(p).quest = 1;
            net.ringsofash.data.Net.sync(p);
        }));
        SCENES.add(new Scene("26_carte", 60, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            lands.getChunk(-445 >> 4, -230 >> 4);
            int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, -445, -230);
            view(p, lands, -444.5, y, -229.5, 160F, 12F);
        }));
        SCENES.add(new Scene("27_carte_legende", 40, p -> { }));
        SCENES.add(new Scene("28_suivi", 40, p -> { }));
        SCENES.add(new Scene("29_carte_edition", 40, p -> { }));
        spot("30_boussole", -445, -230, 30, 0F, 10F);
        // Minas Morgul (centre 1368, 1608, porte a l'ouest) : vues de jour puis de nuit, pour verifier la forme et la lueur verte
        spotT("40_morgul_loin", 1368 - 112, 1608, 62, -90F, -9F, 6000);
        spotT("41_morgul_pont", 1368 - 62, 1608, 3, -90F, 2F, 6000);
        spotT("42_morgul_cour", 1368 - 24, 1608, 2, -90F, -38F, 6000);
        spotT("43_morgul_aerien", 1368 - 84, 1608 - 84, 100, -45F, 24F, 6000);
        spotT("44_morgul_nuit_loin", 1368 - 112, 1608, 62, -90F, -9F, 18000);
        spotT("45_morgul_nuit_pont", 1368 - 62, 1608, 3, -90F, 2F, 18000);
        spotT("46_morgul_nuit_cour", 1368 - 24, 1608, 2, -90F, -38F, 18000);
        spotT("47_morgul_nuit_aerien", 1368 - 84, 1608 - 84, 100, -45F, 24F, 18000);
        spotT("48_morgul_statues", 1368 - 95, 1608, 7, -90F, 3F, 6000);
        spotT("49_morgul_gouffre", 1368, 1608 - 92, 14, 0F, 24F, 6000);
        spotT("50_morgul_gouffre_nuit", 1368, 1608 - 92, 14, 0F, 24F, 18000);
        spotT("54_morgul_fumee", 1368, 1608 - 66, 12, 0F, 38F, 6000);
        spotT("52_morgul_liquide", 1368, 1608 - 80, 26, 0F, 48F, 6000);
        spotT("53_morgul_liquide_pont", 1368 - 70, 1608 - 14, 24, -90F, 50F, 6000);
        spotT("51_morgul_statue_proche", 1368 - 88, 1608 - 14, 6, -60F, 8F, 18000);
        // Orthanc (centre 308, 745, porte a l'est) : vues lointaines, enceinte, zone de guerre, echafaudages, interieur
        spotT("60_orthanc_loin_ouest", 308 - 165, 745, 70, -90F, -26F, 6000);
        spotT("61_orthanc_loin_est", 308 + 165, 745, 70, 90F, -26F, 6000);
        spotT("74_orthanc_sommet_est", 308 + 105, 745, 150, 90F, -28F, 6000);
        spotT("75_orthanc_sommet_diag", 308 + 80, 745 + 80, 170, 135F, -26F, 6000);
        spotT("76_orthanc_sommet_nord", 308, 745 - 95, 150, 0F, -28F, 6000);
        spotT("62_orthanc_porte", 308 + 92, 745, 8, 90F, -6F, 6000);
        spotT("63_orthanc_route", 308 + 70, 745, 5, 90F, -34F, 6000);
        spotT("64_orthanc_pied", 308 + 34, 745 + 22, 4, 123F, -50F, 6000);
        spotT("65_orthanc_muraille", 308 - 70, 745 - 44, 9, 90F, 2F, 6000);
        spotT("66_orthanc_echafaudage", 308 - 100 + 79 - 14, 745 - 100 + 94 + 2, 10, -90F, -6F, 6000);
        spotT("67_orthanc_aerien", 308 + 70, 745 + 70, 80, 135F, 22F, 6000);
        spotT("68_orthanc_nuit_loin", 308 + 165, 745, 70, 90F, -26F, 18000);
        spotT("69_orthanc_nuit_sol", 308 + 70, 745, 5, 90F, -34F, 18000);
        for (int k = 0; k < 2; k++) {
            final int kk = k;
            SCENES.add(new Scene("7" + k + "_orthanc_puits", 420, p -> {
                ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                int ox = 308 - 100, oz = 745 - 100;
                List<double[]> shafts = new ArrayList<>();
                for (double[] q : net.ringsofash.world.OrthancPlan.get().pits()) if (q[4] > 0) shafts.add(q);
                double[] q = shafts.get(Math.min(kk * 3, shafts.size() - 1));
                double tx = ox + q[0], tz = oz + q[1], cx = 308, cz = 745;
                double len = Math.hypot(tx - cx, tz - cz), ux = (tx - cx) / len, uz = (tz - cz) / len;   // on regarde le puits depuis le cote de la tour
                double sx = tx - ux * 13, sz = tz - uz * 13;
                lands.getChunk((int) sx >> 4, (int) sz >> 4);
                lands.getChunk((int) tx >> 4, (int) tz >> 4);
                int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
                lands.setDayTime(6000);
                float yaw = (float) Math.toDegrees(Math.atan2(-(tx - sx), tz - sz));
                view(p, lands, sx + 0.5, y + 8, sz + 0.5, yaw, 38F);
            }));
        }
        // interieur : la hauteur du sol vient d'un point degage de la route
        for (int[] it : new int[][]{{72, 7, 7, 135, -18}, {73, 6, 0, 90, -4}}) {
            final int[] c = it;
            SCENES.add(new Scene(c[0] == 72 ? "72_orthanc_rez" : "73_orthanc_salle_trone", 420, p -> {
                ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) lands.getChunk((308 >> 4) + dx, (745 >> 4) + dz);
                int oy = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, 308 + 60, 745);
                lands.setDayTime(6000);
                view(p, lands, 308 + c[1] + 0.5, oy + (c[0] == 72 ? 0 : 160), 745 + c[2] + 0.5, c[3], c[4]);
            }));
        }
        spot("93_cendre_rocailles", 2125, 1725, 8, 60F, 6F);
        spot("94_cendre_rocailles_haut", 2165, 1745, 45, 90F, 28F);
        spotT("95_cendre_rocailles_nuit", 2195, 1765, 9, 150F, 6F, 18000);
        // Barad-dur (centre 1712, 1742, porte et pont a l'ouest) : vues lointaines, sommet, pont, douve, interieur
        final int BX = 1712, BZ = 1742;
        spotT("80_barad_loin_sud", BX, BZ + 235, 110, 180F, -20F, 6000);
        spotT("81_barad_loin_est", BX + 235, BZ, 110, 90F, -20F, 6000);
        spotT("82_barad_loin_nord", BX, BZ - 235, 110, 0F, -20F, 6000);
        spotT("83_barad_sommet_diag", BX + 55, BZ + 55, 245, 135F, -8F, 6000);
        spotT("84_barad_sommet_sud", BX, BZ + 60, 248, 180F, -6F, 6000);
        spotT("85_barad_pont", BX - 100, BZ, 12, -90F, -4F, 6000);
        spotT("86_barad_pont_nuit", BX - 100, BZ, 12, -90F, -4F, 18000);
        spotT("87_barad_douve", BX + 85, BZ + 85, 25, 135F, 18F, 6000);
        spotT("88_barad_aerien", BX + 140, BZ + 140, 130, 135F, 20F, 6000);
        spotT("89_barad_mi_hauteur", BX + 70, BZ, 120, 90F, -10F, 6000);
        spotT("90_barad_nuit_loin", BX, BZ + 235, 110, 180F, -20F, 18000);
        for (int[] it : new int[][]{{91, -30, 0, -90, -3, 0}, {92, -6, -7, -62, 5, 200}}) {
            final int[] c = it;
            SCENES.add(new Scene(c[0] == 91 ? "91_barad_tunnel" : "92_barad_salle_trone", 420, p -> {
                ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
                for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) lands.getChunk((BX >> 4) + dx, (BZ >> 4) + dz);
                int by = lands.getHeight(Heightmap.Types.OCEAN_FLOOR, BX - 65, BZ);
                lands.setDayTime(6000);
                view(p, lands, BX + c[1] + 0.5, by + c[5], BZ + c[2] + 0.5, c[3], c[4]);
            }));
        }
    }

    /** vue relative a l'Oeil de Sauron (cherche le bloc d'ancrage au centre de Barad-dur) : dx, dy, dz depuis l'oeil */
    private static void eyeT(String name, int bx, int bz, int dx, int dy, int dz, float yaw, float pitch, int time) {
        SCENES.add(new Scene(name, 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            lands.getChunk(bx >> 4, bz >> 4);
            int[] eye = net.ringsofash.world.EyeOfSauron.pos(lands);
            int ey = eye != null ? eye[1] : lands.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 236;
            if (eye != null) { net.ringsofash.data.Net.ClientLore.eyeX = eye[0]; net.ringsofash.data.Net.ClientLore.eyeY = eye[1]; net.ringsofash.data.Net.ClientLore.eyeZ = eye[2]; }
            lands.getChunk((bx + dx) >> 4, (bz + dz) >> 4);
            lands.setDayTime(time);
            view(p, lands, bx + dx + 0.5, ey + 0.5 + dy, bz + dz + 0.5, yaw, pitch);
            EyeOfSauronSky.force = true;
        }));
    }

    /** vue de tres loin sur l'oeil (aucun chunk charge autour de lui) ; show = l'anneau est actif et la corruption suffit, sinon il doit etre invisible */
    private static void eyeFar(String name, int bx, int bz, int off, boolean show, int time) {
        SCENES.add(new Scene(name, 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] eye = net.ringsofash.world.EyeOfSauron.pos(lands);
            if (eye == null) return;
            net.ringsofash.data.Net.ClientLore.eyeX = eye[0]; net.ringsofash.data.Net.ClientLore.eyeY = eye[1]; net.ringsofash.data.Net.ClientLore.eyeZ = eye[2];
            int px = bx + off;
            lands.getChunk(px >> 4, bz >> 4);
            double y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, px, bz) + 3;
            double dx = eye[0] + 0.5 - px, dy = eye[1] - y, dz = eye[2] + 0.5 - bz;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            lands.setDayTime(time);
            view(p, lands, px + 0.5, y, bz + 0.5, yaw, pitch);
            EyeOfSauronSky.force = show;
        }));
    }

    /** vue relative a l'Argonath : de (fx, fy, fz) vers (tx, ty, tz), tout depuis (x de la carte, sol, z de la carte) */
    private static void argT(String name, double fx, double fy, double fz, double tx, double ty, double tz, int time) {
        SCENES.add(new Scene(name, name.startsWith("299") ? 1500 : 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] pl = net.ringsofash.world.MapData.place("argonath");
            if (pl == null) return;
            for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) lands.getChunk((pl[0] >> 4) + dx, (pl[1] >> 4) + dz);
            int py = net.ringsofash.world.SiteHeight.py(lands, pl[0], pl[1]);
            double ddx = tx - fx, ddy = ty - fy, ddz = tz - fz;
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(time);
            view(p, lands, pl[0] + fx + 0.5, py + fy, pl[1] + fz + 0.5, yaw, pitch);
        }));
    }

    /** vue relative a Fondcombe : coordonnees du plan (352 x 352, centre en 176, 176 ; y = niveau du sol au centre) */
    private static void rivT(String name, double fx, double fy, double fz, double tx, double ty, double tz, int time) {
        SCENES.add(new Scene(name, name.startsWith("399") ? 3600 : 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] pl = net.ringsofash.world.MapData.place("fondcombe");
            if (pl == null) return;
            for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) lands.getChunk((pl[0] >> 4) + dx, (pl[1] >> 4) + dz);
            int py = net.ringsofash.world.SiteHeight.py(lands, pl[0], pl[1]);
            double ddx = tx - fx, ddy = ty - fy, ddz = tz - fz;
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(time);
            view(p, lands, pl[0] + fx - 176 + 0.5, py + fy, pl[1] + fz - 176 + 0.5, yaw, pitch);
        }));
    }

    /** vue sur Hobbiteville, en coordonnees du fichier source : de (fx, fy, fz) vers (tx, ty, tz) */
    private static void hobT(String name, double fx, double fy, double fz, double tx, double ty, double tz) {
        SCENES.add(new Scene(name, 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] pl = net.ringsofash.world.MapData.place("comte");
            if (pl == null) return;
            int rx = net.ringsofash.world.HobbitonStamp.originX(pl[0]), rz = net.ringsofash.world.HobbitonStamp.originZ(pl[1]);
            int off = net.ringsofash.world.HobbitonStamp.OFF, fz0 = net.ringsofash.world.HobbitonStamp.FILE_Z0;
            double cx = rx + fx, cz = rz + fz - fz0;
            for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) lands.getChunk(((int) cx >> 4) + dx, ((int) cz >> 4) + dz);
            double ddx = tx - fx, ddy = ty - fy, ddz = tz - fz;
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(6000);
            view(p, lands, cx + 0.5, fy + off, cz + 0.5, yaw, pitch);
        }));
    }

    /** vue dans les mines de la Moria, en coordonnees du monde : de (fx, fy, fz) vers (tx, ty, tz) ; vision nocturne pour voir les salles sombres */
    private static void morT(String name, double fx, double fy, double fz, double tx, double ty, double tz, int time) {
        SCENES.add(new Scene(name, 600, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int cx = (int) fx >> 4, cz = (int) fz >> 4;
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) lands.getChunk(cx + dx, cz + dz);
            double ddx = tx - fx, ddy = ty - fy, ddz = tz - fz;
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(time);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 100000, 0, true, false));
            view(p, lands, fx, fy, fz, yaw, pitch);
        }));
    }

    /** vue exterieure de Fondcombe : charge en plus une marge de chunks (west, north, east en chunks) autour du plan, pour voir le relief qui le prolonge */
    private static void rivO(String name, double fx, double fy, double fz, double tx, double ty, double tz, int mw, int mn, int me) {
        SCENES.add(new Scene(name, 3000, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] pl = net.ringsofash.world.MapData.place("fondcombe");
            if (pl == null) return;
            for (int dx = -12 - mw; dx <= 12 + me; dx++) for (int dz = -12 - mn; dz <= 12; dz++) lands.getChunk((pl[0] >> 4) + dx, (pl[1] >> 4) + dz);
            int py = net.ringsofash.world.SiteHeight.py(lands, pl[0], pl[1]);
            double ddx = tx - fx, ddy = ty - fy, ddz = tz - fz;
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(6000);
            view(p, lands, pl[0] + fx - 176 + 0.5, py + fy, pl[1] + fz - 176 + 0.5, yaw, pitch);
        }));
    }

    /** vue relative au Gouffre de Helm : coordonnees du plan (240 x 216, centre en 120, 108 ; y = niveau du sol de la plaine) */
    private static void helmT(String name, double fx, double fy, double fz, double tx, double ty, double tz, int time) {
        SCENES.add(new Scene(name, name.startsWith("430") ? 3600 : 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] pl = net.ringsofash.world.MapData.place("gouffre_de_helm");
            if (pl == null) return;
            for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) lands.getChunk((pl[0] >> 4) + dx, (pl[1] >> 4) + dz);
            int py = net.ringsofash.world.SiteHeight.py(lands, pl[0], pl[1]);
            double ddx = -(tx - fx), ddy = ty - fy, ddz = -(tz - fz);                                      // le site est tourne de 180 degres (rot 2)
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(time);
            view(p, lands, pl[0] - (fx - 120) + 0.5, py + fy, pl[1] - (fz - 108) + 0.5, yaw, pitch);
        }));
    }

    /** vue relative a Esgaroth : coordonnees du plan (320 x 320, centre en 160, 160 ; y = niveau du sol au centre, l'eau est a -1, les ponts a 2) */
    private static void esgT(String name, double fx, double fy, double fz, double tx, double ty, double tz, int time) {
        SCENES.add(new Scene(name, name.startsWith("419") ? 3600 : 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            int[] pl = net.ringsofash.world.MapData.place("esgaroth");
            if (pl == null) return;
            for (int dx = -11; dx <= 11; dx++) for (int dz = -11; dz <= 11; dz++) lands.getChunk((pl[0] >> 4) + dx, (pl[1] >> 4) + dz);
            int py = net.ringsofash.world.SiteHeight.py(lands, pl[0], pl[1]);
            double ddx = tx - fx, ddy = ty - fy, ddz = tz - fz;
            float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz)), pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            lands.setDayTime(time);
            view(p, lands, pl[0] + fx - 160 + 0.5, py + fy, pl[1] + fz - 160 + 0.5, yaw, pitch);
        }));
    }

    private static void spotT(String name, int x, int z, int up, float yaw, float pitch, int time) {
        SCENES.add(new Scene(name, 420, p -> {
            ServerLevel lands = p.server.getLevel(ModWorldgen.LANDS);
            lands.getChunk(x >> 4, z >> 4);
            int y = lands.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            lands.setDayTime(time);
            view(p, lands, x + 0.5, y + up, z + 0.5, yaw, pitch);
        }));
    }
}
