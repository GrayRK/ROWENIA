package com.ixiastraixi.roweniafull.mechanics.warforge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Базовый предмет с качеством:
 * - имя окрашивается по качеству;
 * - (опц.) добавляется префикс перед названием ("Легендарная ...") с учётом рода;
 * - блеск на EPIC/LEGEND;
 * - в тултипе строка "Качество: <...>".
 *
 * Использование: регистрируй предметы как new QualityItem(new Item.Properties(), PrefixForm.FEM) и т.п.
 * Для предметов без префикса (только цвет/тултип) укажи PrefixForm.NONE.
 */
public class QualityItem extends Item {

    /** Род для префикса качества в русском. */
    public enum PrefixForm { NONE, MASC, FEM, NEUT }

    private final PrefixForm prefixForm;

    public QualityItem(Properties props) {
        this(props, PrefixForm.NONE);
    }

    public QualityItem(Properties props, PrefixForm prefixForm) {
        super(props);
        this.prefixForm = prefixForm;
    }

    /** В какой набор ключей префиксов смотреть в зависимости от рода. */
    protected String prefixKeyOf(Quality q) {
        // Ключи должны существовать в lang: quality.roweniafull.prefix.{m|f|n}.<tier>
        String tier = switch (q) {
            case COMMON -> "common";
            case RARE   -> "rare";
            case FINE   -> "fine";
            case EPIC   -> "epic";
            case LEGEND -> "legend";
        };
        return switch (prefixForm) {
            case MASC -> "quality.roweniafull.prefix.m." + tier; // "Легендарный"
            case FEM  -> "quality.roweniafull.prefix.f." + tier; // "Легендарная"
            case NEUT -> "quality.roweniafull.prefix.n." + tier; // "Легендарное"
            case NONE -> ""; // без префикса
        };
    }

    /** Возвращаем совместимый vanilla Rarity, чтобы UI не ломался. */
    @Override
    public Rarity getRarity(ItemStack stack) {
        return ItemQuality.get(stack).rarity;
    }

    /** Блеск для эпик/легендарных. */
    @Override
    public boolean isFoil(ItemStack stack) {
        Quality q = ItemQuality.get(stack);
        return q == Quality.EPIC || q == Quality.LEGEND || super.isFoil(stack);
    }

    /** Окрашиваем имя по качеству и (опц.) добавляем префикс. */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        Quality q = ItemQuality.get(stack);

        // если пользователь переименовал предмет в наковальне — сохраняем его имя, только красим
        if (stack.hasCustomHoverName()) {
            return base.copy().withStyle(q.color);
        }

        // префикс по желанию
        String pKey = prefixKeyOf(q);
        Component colored = base.copy().withStyle(q.color);
        if (pKey.isEmpty()) {
            return colored;
        }
        // "Легендарная " + "<Имя предмета>"
        return Component.translatable(pKey).append(" ").append(colored).withStyle(q.color);
    }

    /** Добавляем строку "Качество: ..." в тултип. */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag); // ничего не добавляем
    }
}
