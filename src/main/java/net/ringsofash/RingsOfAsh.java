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
            .displayItems((params, out) -> {
                out.accept(net.ringsofash.data.Guide.create(null));
                Reg.ITEMS.getEntries().forEach(e -> {
                    out.accept(e.get());
                    if (e.get() instanceof net.ringsofash.item.Artifacts.Piece) out.accept(net.ringsofash.item.Artifacts.artifact(e.get()));     // version artefact (secrete) a cote de la version ordinaire
                });
                net.ringsofash.block.Furniture.ITEMS.getEntries().forEach(e -> out.accept(e.get()));
                out.accept(net.ringsofash.fluid.ModFluids.ICHOR_BUCKET.get());
            })
            .build());

    public RingsOfAsh(IEventBus modBus) {
        Reg.BLOCKS.register(modBus);
        Reg.ITEMS.register(modBus);
        net.ringsofash.block.Furniture.register(modBus);
        TABS.register(modBus);
        net.ringsofash.menu.ModMenus.MENUS.register(modBus);
        net.ringsofash.item.Artifacts.init(modBus);
        net.ringsofash.item.Runes.init(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        net.ringsofash.item.RuneEffects.init(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        net.ringsofash.world.TownRespect.init(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        net.ringsofash.item.UdunFire.init(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        net.ringsofash.effect.ModEffects.EFFECTS.register(modBus);
        net.ringsofash.fluid.ModFluids.register(modBus);
        ModWorldgen.register(modBus);
        ModData.ATTACHMENTS.register(modBus);
        net.ringsofash.entity.ModEntities.register(modBus);
        net.ringsofash.item.ModGear.ARMOR_MATERIALS.register(modBus);
        net.ringsofash.sound.ModSounds.register(modBus);
        GameEvents.init(modBus);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) net.ringsofash.client.ClientSetup.init(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
