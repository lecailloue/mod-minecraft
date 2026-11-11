package net.ringsofash;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(RingsOfAsh.MODID)
public class RingsOfAsh {
    public static final String MODID = "ringsofash";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RingsOfAsh(IEventBus modBus) {
        LOGGER.info("Les Anneaux de Cendre: chargement");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
