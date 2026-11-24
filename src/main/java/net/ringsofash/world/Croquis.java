package net.ringsofash.world;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.loading.FMLPaths;
import net.ringsofash.RingsOfAsh;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Croquis : le joueur construit un detail a la main dans le jeu, marque deux coins et l'enregistre.
 * Le fichier (roa_croquis/<nom>.json dans le dossier du jeu) est relu par les outils du mod pour
 * en faire un gabarit pose par la generation.
 */
public final class Croquis {
    private static final Map<UUID, BlockPos[]> SEL = new HashMap<>();

    private Croquis() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("croquis")
                .then(Commands.literal("coin1").executes(c -> mark(c.getSource().getPlayerOrException(), 0)))
                .then(Commands.literal("coin2").executes(c -> mark(c.getSource().getPlayerOrException(), 1)))
                .then(Commands.literal("sauver").then(Commands.argument("nom", StringArgumentType.word())
                        .executes(c -> save(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nom")))));
    }

    private static int mark(ServerPlayer p, int i) {
        BlockPos[] s = SEL.computeIfAbsent(p.getUUID(), k -> new BlockPos[2]);
        s[i] = p.blockPosition();
        msg(p, "Coin " + (i + 1) + " : " + s[i].getX() + " " + s[i].getY() + " " + s[i].getZ(), ChatFormatting.GREEN);
        if (s[0] != null && s[1] != null) {
            int dx = Math.abs(s[0].getX() - s[1].getX()) + 1, dy = Math.abs(s[0].getY() - s[1].getY()) + 1, dz = Math.abs(s[0].getZ() - s[1].getZ()) + 1;
            msg(p, "Zone " + dx + " x " + dy + " x " + dz + ". Tapez /anneaux croquis sauver <nom>", ChatFormatting.GRAY);
        }
        return 1;
    }

    /** nom du bloc et de ses etats, comme dans les commandes : minecraft:oak_log[axis=x] */
    private static String name(BlockState st) {
        StringBuilder sb = new StringBuilder(BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString());
        if (!st.getValues().isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Map.Entry<Property<?>, Comparable<?>> e : st.getValues().entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(e.getKey().getName()).append('=').append(e.getValue().toString().toLowerCase(java.util.Locale.ROOT));
            }
            sb.append(']');
        }
        return sb.toString();
    }

    private static int save(ServerPlayer p, String nom) {
        BlockPos[] s = SEL.get(p.getUUID());
        if (s == null || s[0] == null || s[1] == null) { msg(p, "Marquez d'abord les deux coins.", ChatFormatting.RED); return 0; }
        ServerLevel level = p.serverLevel();
        int x0 = Math.min(s[0].getX(), s[1].getX()), x1 = Math.max(s[0].getX(), s[1].getX());
        int y0 = Math.min(s[0].getY(), s[1].getY()), y1 = Math.max(s[0].getY(), s[1].getY());
        int z0 = Math.min(s[0].getZ(), s[1].getZ()), z1 = Math.max(s[0].getZ(), s[1].getZ());
        if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > 200000) { msg(p, "Zone trop grande.", ChatFormatting.RED); return 0; }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"nom\": \"").append(nom).append("\",\n");
        sb.append("  \"origine\": [").append(x0).append(", ").append(y0).append(", ").append(z0).append("],\n");
        sb.append("  \"taille\": [").append(x1 - x0 + 1).append(", ").append(y1 - y0 + 1).append(", ").append(z1 - z0 + 1).append("],\n");
        int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        sb.append("  \"sol\": ").append(level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz) - 1).append(",\n");
        sb.append("  \"mer\": ").append(MapData.SEA).append(",\n");
        sb.append("  \"biome\": \"").append(level.getBiome(new BlockPos(cx, (y0 + y1) / 2, cz)).unwrapKey().map(k -> k.location().toString()).orElse("?")).append("\",\n");
        sb.append("  \"blocs\": [");
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int n = 0;
        for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) {
            BlockState st = level.getBlockState(m.set(x, y, z));
            if (st.isAir() || st.is(Blocks.VOID_AIR)) continue;
            if (n > 0) sb.append(',');
            sb.append("\n    {\"p\": [").append(x - x0).append(", ").append(y - y0).append(", ").append(z - z0)
                    .append("], \"b\": \"").append(name(st)).append("\"}");
            n++;
        }
        sb.append("\n  ]\n}\n");
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("roa_croquis");
            Files.createDirectories(dir);
            Path f = dir.resolve(nom.replaceAll("[^A-Za-z0-9_-]", "_") + ".json");
            Files.writeString(f, sb.toString());
            msg(p, n + " blocs enregistres dans roa_croquis/" + f.getFileName(), ChatFormatting.GOLD);
            RingsOfAsh.LOGGER.info("[CROQUIS] " + f + " (" + n + " blocs)");
        } catch (Exception ex) {
            msg(p, "Echec de l'enregistrement : " + ex.getMessage(), ChatFormatting.RED);
            RingsOfAsh.LOGGER.error("croquis", ex);
            return 0;
        }
        return 1;
    }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) {
        p.displayClientMessage(Component.literal(s).withStyle(c), false);
    }
}
