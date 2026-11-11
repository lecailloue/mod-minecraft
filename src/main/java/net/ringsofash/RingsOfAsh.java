package net.ringsofash;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.data.ModData;
import net.ringsofash.gen.Reg;
import net.ringsofash.world.ModWorldgen;
import org.slf4j.Logger;

@Mod(RingsOfAsh.MODID)
public class RingsOfAsh {
    public static final String MODID = "ringsofash";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ringsofash"))
            .icon(() -> new ItemStack(Reg.HALFLING_RING.get()))
            .displayItems((params, out) -> Reg.ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());

    public RingsOfAsh(IEventBus modBus) {
        Reg.BLOCKS.register(modBus);
        Reg.ITEMS.register(modBus);
        TABS.register(modBus);
        ModWorldgen.register(modBus);
        ModData.ATTACHMENTS.register(modBus);
        GameEvents.init(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
