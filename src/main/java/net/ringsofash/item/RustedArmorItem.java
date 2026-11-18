package net.ringsofash.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.ringsofash.RingsOfAsh;

/** Armure rouillee a 3 textures : la variante (1 a 3) est tiree au sort quand l'objet arrive dans un inventaire, puis reste fixe. */
public class RustedArmorItem extends ArmorItem {
    private final String name;

    public RustedArmorItem(String name, Holder<ArmorMaterial> m, Type type, Properties props) {
        super(m, type, props);
        this.name = name;
    }

    public static int variant(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        int v = d == null ? 0 : d.copyTag().getInt("rust_variant");
        return v >= 1 && v <= 3 ? v : 1;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide) return;
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null || !d.contains("rust_variant")) {
            int v = 1 + level.random.nextInt(3);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("rust_variant", v));
        }
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        int v = variant(stack);
        return ResourceLocation.fromNamespaceAndPath(RingsOfAsh.MODID, "textures/models/armor/" + name + (v == 1 ? "" : "_v" + v) + "_layer_" + (innerModel ? 2 : 1) + ".png");
    }
}
