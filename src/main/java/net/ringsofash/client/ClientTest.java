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
                    if (SCENES.isEmpty()) scenes();
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
                    if (s.name.contains("anneaux")) mc.options.hideGui = false;
                    if (s.name.contains("suivi")) { mc.options.hideGui = false; QuestScreen.tracked = 1; }
                    server.execute(() -> {
                        try { s.setup.accept(server.getPlayerList().getPlayers().get(0)); }
                        catch (Throwable t) { RingsOfAsh.LOGGER.error("[CLIENTTEST] ECHEC scene " + s.name, t); }
                    });
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
    private static void view(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void clear(ServerLevel level) {
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

    private static void scenes() {
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
            net.minecraft.world.item.Item[] it = {Reg.ORC_CLEAVER.get(), Reg.ORC_SCIMITAR.get(), Reg.ORC_MACHETE.get(), Reg.ORC_SPEAR.get(), Reg.ORC_MACE.get(), Reg.GOBLIN_KING_STAFF.get(), Reg.GOBLIN_PICKAXE.get()};
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
            int[] vs = {0, 1, 2};
            double x = SX + 4.4;
            for (int v : vs) {
                final int vv = v;
                actor(ow, v == 2 ? ModEntities.SHADOW_CHIEF.get() : ModEntities.SHADOW_RIDER.get(), x, SZ + 11, o -> o.testVariant(vv));
                x -= 4.4;
            }
            view(p, ow, SX + 0.5, SY + 1.6, SZ, 0F, 4F);
        }));
        SCENES.add(new Scene("03f_ombres_cote", 70, p -> {
            ServerLevel ow = p.server.overworld();
            clear(ow);
            ShadowRiderEntity c = actor(ow, ModEntities.SHADOW_CHIEF.get(), SX, SZ + 8, o -> o.testVariant(2));
            c.moveTo(SX, SY, SZ + 8, 90F, 0F); c.setYBodyRot(90F); c.setYHeadRot(90F);
            view(p, ow, SX + 0.5, SY + 2.2, SZ, 0F, 8F);
        }));
        spectreLine("03b_spectres", 9.0, 3F, 2.2);
        spectreLine("03c_roi_proche", 4.2, 6F, 2.2);
        orcLine("01b_orques_modeles", 11.0, 3F, 2.3, new int[]{0, 5, 10, 15, 2, 3, 0}, true);
        orcLine("01c_orques_couleurs", 11.0, 3F, 2.3, new int[]{4, 5, 6, 7, 8, 9, 12}, false);
        orcLine("01d_chef_proche", 4.5, 6F, 3.0, new int[]{0}, true);
        lineup("02_peuples", 8.5, 2F, 1.9, List.of(ModEntities.HUMAN.get(), ModEntities.HUMAN.get(), ModEntities.ELF.get(), ModEntities.ELF.get(), ModEntities.DWARF.get(), ModEntities.DWARF.get(),
                ModEntities.HALFLING.get(), ModEntities.HALFLING.get(), ModEntities.RANGER.get(), ModEntities.MAGE.get()));
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
        SCENES.add(new Scene("12a_onglet_inventaire", 40, p -> { }));
        SCENES.add(new Scene("12b_anneaux_menu", 50, p -> {
            var l = net.ringsofash.data.ModData.lore(p);
            l.rings.clearContent();
            net.minecraft.world.item.Item[] rs = {Reg.RING_WATERS.get(), Reg.RING_WINDS.get(), Reg.RING_EMBERS.get(), Reg.RING_FORGE.get(), Reg.RING_HOARD.get(), Reg.RING_KINGS.get(), Reg.RING_SHADOWS.get(), Reg.ASH_RING.get()};
            for (int i = 0; i < rs.length; i++) l.rings.setItem(i + (i > 4 ? 1 : 0), new ItemStack(rs[i]));
            l.activeRing = 2;
            net.ringsofash.data.Net.sync(p);
            net.ringsofash.menu.RingsMenu.open(p);
        }));
        SCENES.add(new Scene("12c_anneaux_roue", 30, p -> { }));
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
                    {Reg.LUNARGENT_HELMET.get(), Reg.LUNARGENT_CHESTPLATE.get(), Reg.LUNARGENT_LEGGINGS.get(), Reg.LUNARGENT_BOOTS.get(), Reg.LUNARGENT_SWORD.get()},
                    {Reg.ORC_HELMET.get(), Reg.ORC_CHESTPLATE.get(), Reg.ORC_LEGGINGS.get(), Reg.ORC_BOOTS.get(), Reg.ORC_SCIMITAR.get()},
                    {Reg.ELVEN_HELMET.get(), Reg.ELVEN_CHESTPLATE.get(), Reg.ELVEN_LEGGINGS.get(), Reg.ELVEN_BOOTS.get(), Reg.ELVEN_BLADE.get()},
                    {Reg.DWARVEN_HELMET.get(), Reg.DWARVEN_CHESTPLATE.get(), Reg.DWARVEN_LEGGINGS.get(), Reg.DWARVEN_BOOTS.get(), Reg.DWARVEN_AXE.get()},
                    {Reg.ASHLORD_HELMET.get(), Reg.ASHLORD_CHESTPLATE.get(), Reg.ASHLORD_LEGGINGS.get(), Reg.ASHLORD_BOOTS.get(), Reg.ASH_MACE.get()},
                    {Reg.ELFKING_HELMET.get(), Reg.ELFKING_CHESTPLATE.get(), Reg.ELFKING_LEGGINGS.get(), Reg.ELFKING_BOOTS.get(), Reg.KINGS_BLADE.get()},
                    {null, Reg.ELVEN_CLOAK.get(), null, null, Reg.STAFF_FIRE.get()}};
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
    }
}
