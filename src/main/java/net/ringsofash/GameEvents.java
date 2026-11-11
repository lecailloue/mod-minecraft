package net.ringsofash;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ringsofash.world.ModWorldgen;
import net.ringsofash.world.SelfTest;
import net.ringsofash.world.Travel;

/** Evenements de jeu (cote serveur). */
public final class GameEvents {
    public static final TagKey<Structure> ARRIVAL_VILLAGES = TagKey.create(Registries.STRUCTURE, RingsOfAsh.id("arrival_villages"));

    private GameEvents() {}

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(GameEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(GameEvents::onSleepFinished);
        NeoForge.EVENT_BUS.addListener(GameEvents::onCommands);
        NeoForge.EVENT_BUS.addListener(GameEvents::onServerStarted);
    }

    private static void onEntityTick(EntityTickEvent.Pre e) {
        if (e.getEntity() instanceof ItemEntity item && !item.level().isClientSide) Travel.tickItem(item);
    }

    /** Dormir dans la Hautemarche fait bien passer la nuit (l'heure est celle du monde normal). */
    private static void onSleepFinished(SleepFinishedTimeEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && Travel.isLands(sl)) {
            sl.getServer().overworld().setDayTime(e.getNewTime());
        }
    }

    private static void onServerStarted(ServerStartedEvent e) {
        if (System.getProperty("roa.selftest") != null) SelfTest.run(e.getServer());
    }

    public static void onFirstArrival(ServerPlayer p) {
    }

    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("anneaux").requires(s -> s.hasPermission(2))
                .then(Commands.literal("hautemarche").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!Travel.isLands(p.level())) net.ringsofash.data.ModData.lore(p).setOverworld(p.blockPosition());
                    Travel.toLands(p);
                    return 1;
                }))
                .then(Commands.literal("retour").executes(ctx -> {
                    Travel.toOverworld(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("volcan").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel level = p.serverLevel();
                    BlockPos pos = level.findNearestMapStructure(ModWorldgen.VOLCANOES, p.blockPosition(), 200, false);
                    if (pos == null) {
                        ctx.getSource().sendFailure(Component.literal("Aucun volcan trouvé à proximité."));
                        return 0;
                    }
                    Travel.teleport(p, level, pos.offset(100, 0, 0), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("Volcan en " + pos.getX() + ", " + pos.getZ() + " (à l'ouest de vous).").withStyle(ChatFormatting.GOLD), false);
                    return 1;
                })));
    }
}
