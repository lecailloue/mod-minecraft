package net.ringsofash.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RingsOfAsh.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ForgeMenu>> FORGE = MENUS.register("dwarven_forge", () -> new MenuType<>(ForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<RingsMenu>> RINGS = MENUS.register("rings", () -> new MenuType<>(RingsMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ArtifactMenu>> ARTIFACTS = MENUS.register("artifacts", () -> new MenuType<>(ArtifactMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<PocketFurnaceMenu>> FURNACE = MENUS.register("pocket_furnace", () -> new MenuType<>(PocketFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
