package com.ixiastraixi.roweniafull.registry.warforge;

import com.ixiastraixi.roweniafull.RoweniaFull;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Единые теги для Warforge. */
public final class WarforgeTags {
    private WarforgeTags() {}

    /** Тег всех «заготовок лезвия» */
    public static final TagKey<Item> BLADE_BLANKS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RoweniaFull.MOD_ID, "blade_blanks"));

    /** Тег всех «рукоятей» */
    public static final TagKey<Item> HANDLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RoweniaFull.MOD_ID, "handles"));

    /** Тег всех «пластин» */
    public static final TagKey<Item> ARMOR_PLATE =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RoweniaFull.MOD_ID, "armor_plate"));
}
