package net.ringsofash.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;
import net.ringsofash.gen.Reg;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RingsOfAsh.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EyeOfSauronBlockEntity>> EYE_OF_SAURON = TYPES.register("eye_of_sauron",
            () -> BlockEntityType.Builder.of(EyeOfSauronBlockEntity::new, Reg.EYE_OF_SAURON.get()).build(null));

    public static void register(IEventBus bus) { TYPES.register(bus); }
}
