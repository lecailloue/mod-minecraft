package net.ringsofash.data;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ringsofash.RingsOfAsh;

import java.util.function.Supplier;

public final class ModData {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RingsOfAsh.MODID);
    public static final Supplier<AttachmentType<PlayerLore>> LORE = ATTACHMENTS.register("lore",
            () -> AttachmentType.serializable(PlayerLore::new).copyOnDeath().build());

    public static PlayerLore lore(Player p) { return p.getData(LORE); }

    private ModData() {}
}
