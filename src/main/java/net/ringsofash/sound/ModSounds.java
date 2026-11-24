package net.ringsofash.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ringsofash.RingsOfAsh;

/** Les sons du mod. La musique de la Hautemarche est reclamee par les biomes (champ "music"). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RingsOfAsh.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_HAUTEMARCHE =
            SOUNDS.register("music.hautemarche", () -> SoundEvent.createVariableRangeEvent(RingsOfAsh.id("music.hautemarche")));

    private ModSounds() {}

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
