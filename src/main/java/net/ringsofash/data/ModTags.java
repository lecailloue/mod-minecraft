package net.ringsofash.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.ringsofash.RingsOfAsh;

/** Les etiquettes du mod. */
public final class ModTags {
    /** armes qui peuvent porter des runes */
    public static final TagKey<Item> RUNIC = TagKey.create(Registries.ITEM, RingsOfAsh.id("runic"));

    private ModTags() {}
}
