package com.ixiastraixi.roweniafull.mechanics.warforge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * Уровни качества предмета.
 * id хранится в NBT (0..4), цвет используется для окраски имени,
 * getRarity возвращает близкий vanilla Rarity для совместимости с UI.
 */
public enum Quality {
    COMMON (0, ChatFormatting.WHITE,        "quality.roweniafull.common",     Rarity.COMMON),
    RARE   (1, ChatFormatting.GREEN,         "quality.roweniafull.rare",       Rarity.RARE),
    FINE   (2, ChatFormatting.BLUE,        "quality.roweniafull.fine",       Rarity.UNCOMMON),
    EPIC   (3, ChatFormatting.LIGHT_PURPLE, "quality.roweniafull.epic",       Rarity.EPIC),
    LEGEND (4, ChatFormatting.GOLD,         "quality.roweniafull.legendary",  Rarity.EPIC); // цвет возьмём GOLD через getName

    public final int id;
    public final ChatFormatting color;
    /** Ключ локали для строки "Качество: ..." */
    public final String nameKey;
    /** Vanilla Rarity — для совместимости (цвет имён по редкости и прочее) */
    public final Rarity rarity;

    Quality(int id, ChatFormatting color, String nameKey, Rarity rarity) {
        this.id = id;
        this.color = color;
        this.nameKey = nameKey;
        this.rarity = rarity;
    }

    public static Quality byId(int v) {
        return switch (v) {
            case 1 -> RARE;
            case 2 -> FINE;
            case 3 -> EPIC;
            case 4 -> LEGEND;
            default -> COMMON;
        };
    }
}
