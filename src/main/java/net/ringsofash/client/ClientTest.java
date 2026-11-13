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
                    if (s.name.contains("reputation")) { mc.options.hideGui = false; mc.setScreen(new RepScreen()); }
                    server.execute(() -> {
                        try { s.setup.accept(server.getPlayerList().getPlayers().get(0)); }
                        catch (Throwable t) { RingsOfAsh.LOGGER.error("[CLIENTTEST] ECHEC scene " + s.name, t); }
                    });
                    wait = s.ticks; state = 3;
                }
                case 3 -> {
                    if (--wait <= 0) {
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
                ModEntities.WIGHT.get(), ModEntities.WILD_MAN.get(), ModEntities.PALE_RIDER.get()));
        lineup("02_peuples", 8.5, 2F, 1.9, List.of(ModEntities.HUMAN.get(), ModEntities.HUMAN.get(), ModEntities.ELF.get(), ModEntities.ELF.get(), ModEntities.DWARF.get(), ModEntities.DWARF.get(),
                ModEntities.HALFLING.get(), ModEntities.HALFLING.get(), ModEntities.RANGER.get(), ModEntities.MAGE.get()));
        lineup("03_divers", 7.0, 2F, 2.2, List.of(ModEntities.CAVE_THIEF.get(), ModEntities.SORCERER.get(), ModEntities.DEAD_SOLDIER.get(), ModEntities.CROW.get(), ModEntities.STAG.get(), ModEntities.WARG.get()));
        lineup("04_betes", 12.0, -4F, 5.5, List.of(ModEntities.CAVE_TROLL.get(), ModEntities.STONE_TROLL.get(), ModEntities.GIANT_SPIDER.get(), ModEntities.EAGLE.get(), ModEntities.LURKER.get()));
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
    }
}
