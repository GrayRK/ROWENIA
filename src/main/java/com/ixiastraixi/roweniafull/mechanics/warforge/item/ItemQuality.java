package com.ixiastraixi.roweniafull.mechanics.warforge.item;

import net.minecraft.world.item.ItemStack;

/**
 * Утилиты для работы с качеством в NBT.
 * Храним в теге stack: { WarforgeQuality: <0..4> }.
 */
public final class ItemQuality {
    private ItemQuality() {}
    public static final String TAG_QUALITY = "WarforgeQuality";

    /** Прочитать качество из стака; по умолчанию COMMON. */
    public static Quality get(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_QUALITY)) {
            return Quality.byId(stack.getTag().getInt(TAG_QUALITY));
        }
        return Quality.COMMON;
    }

    /** Записать качество в стак. */
    public static void set(ItemStack stack, Quality q) {
        stack.getOrCreateTag().putInt(TAG_QUALITY, q.id);
    }

    /** Вернуть новый стак с заданным качеством. */
    public static ItemStack withQuality(ItemStack stack, Quality q) {
        ItemStack copy = stack.copy();
        set(copy, q);
        return copy;
    }
}
