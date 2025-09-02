package com.ixiastraixi.roweniafull.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public final class DeathItemLossConfig {
    public static final ForgeConfigSpec SPEC;

    // ── VANILLA ──────────────────────────────────────────────────────────────────
    public static final ForgeConfigSpec.DoubleValue HOTBAR_LOSS_CHANCE;     // хотбар 0..8
    public static final ForgeConfigSpec.DoubleValue BACKPACK_LOSS_CHANCE;   // рюкзак 9..35
    public static final ForgeConfigSpec.DoubleValue OFFHAND_LOSS_CHANCE;    // offhand[0]
    public static final ForgeConfigSpec.DoubleValue HELMET_LOSS_CHANCE;     // head
    public static final ForgeConfigSpec.DoubleValue CHEST_LOSS_CHANCE;      // chest/elytra
    public static final ForgeConfigSpec.DoubleValue LEGS_LOSS_CHANCE;       // legs
    public static final ForgeConfigSpec.DoubleValue BOOTS_LOSS_CHANCE;      // feet

    // ── CURIOS ───────────────────────────────────────────────────────────────────
    public static final ForgeConfigSpec.DoubleValue CURIOS_DEFAULT_LOSS_CHANCE; // общий шанс для всех Curios
    // Популярные слоты (−1.0 = использовать общий шанс)
    public static final ForgeConfigSpec.DoubleValue CURIOS_RING_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_NECKLACE_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_BELT_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_BACK_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_HANDS_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_CHARM_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_HEAD_LOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CURIOS_BODY_LOSS_CHANCE;
    // Переопределения: "slotId=chance" (приоритетнее именных). -1 → общий шанс
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CURIOS_SLOT_OVERRIDES;

    // ── FILTERS (белый/чёрный списки) ────────────────────────────────────────────
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST_TAGS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_TAGS;

    // ── MODIFIERS (проценты по тегам) ────────────────────────────────────────────
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TAG_CHANCE_MODIFIERS;

    // ── CHAT ─────────────────────────────────────────────────────────────────────
    public static final ForgeConfigSpec.BooleanValue CHAT_SHOW_SUMMARY;
    public static final ForgeConfigSpec.IntValue CHAT_MAX_ENTRIES;

    // Зарегистрировать отдельный файл RoweniaDeathItemLoss.toml
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "RoweniaDeathItemLoss.toml");
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment(
                "Механика утраты предметов при смерти.",
                "ВАЖНО: работает ТОЛЬКО когда в мире включён gamerule keepInventory = true.",
                "Шансы применяются к каждому слоту отдельно."
        );

        b.push("death_item_loss");

        // ───────────────────────  VANILLA  ───────────────────────
        b.push("vanilla");

        HOTBAR_LOSS_CHANCE   = b.comment("Хотбар (ячейки 0..8). 0.0–1.0; напр. 0.7 = 70% шанс удалить каждую ячейку хотбара.")
                .defineInRange("hotbarLossChance", 0.70D, 0.0D, 1.0D);
        BACKPACK_LOSS_CHANCE = b.comment("Рюкзак (ячейки 9..35). 0.0–1.0.")
                .defineInRange("backpackLossChance", 0.70D, 0.0D, 1.0D);
        OFFHAND_LOSS_CHANCE  = b.comment("Вторая рука (оффхэнд). 0.0–1.0.")
                .defineInRange("offhandLossChance", 0.70D, 0.0D, 1.0D);

        HELMET_LOSS_CHANCE = b.comment("Шлем (HEAD). 0.0–1.0.")
                .defineInRange("helmetLossChance", 0.45D, 0.0D, 1.0D);
        CHEST_LOSS_CHANCE  = b.comment("Нагрудник / элитры (CHEST). 0.0–1.0.")
                .defineInRange("chestLossChance", 0.45D, 0.0D, 1.0D);
        LEGS_LOSS_CHANCE   = b.comment("Поножи (LEGS). 0.0–1.0.")
                .defineInRange("legsLossChance", 0.45D, 0.0D, 1.0D);
        BOOTS_LOSS_CHANCE  = b.comment("Ботинки (FEET). 0.0–1.0.")
                .defineInRange("bootsLossChance", 0.45D, 0.0D, 1.0D);

        b.pop(); // vanilla

        // ───────────────────────  CURIOS  ───────────────────────
        b.push("curios");

        CURIOS_DEFAULT_LOSS_CHANCE = b.comment(
                "ОБЩИЙ шанс для ВСЕХ слотов Curios (0.0–1.0).",
                "Для именованных/override-слотов можно задать -1.0, тогда будет использован этот общий шанс."
        ).defineInRange("curiosDefaultLossChance", 0.45D, 0.0D, 1.0D);

        CURIOS_RING_LOSS_CHANCE     = b.comment("Curios слот 'ring' (кольцо). -1.0 = использовать curiosDefaultLossChance.")
                .defineInRange("curiosRingLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_NECKLACE_LOSS_CHANCE = b.comment("Curios слот 'necklace' (ожерелье). -1.0 = использовать общий шанс.")
                .defineInRange("curiosNecklaceLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_BELT_LOSS_CHANCE     = b.comment("Curios слот 'belt' (пояс). -1.0 = использовать общий шанс.")
                .defineInRange("curiosBeltLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_BACK_LOSS_CHANCE     = b.comment("Curios слот 'back' (спина/рюкзак). -1.0 = использовать общий шанс.")
                .defineInRange("curiosBackLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_HANDS_LOSS_CHANCE    = b.comment("Curios слот 'hands' (руки). -1.0 = использовать общий шанс.")
                .defineInRange("curiosHandsLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_CHARM_LOSS_CHANCE    = b.comment("Curios слот 'charm' (амулет/талисман). -1.0 = использовать общий шанс.")
                .defineInRange("curiosCharmLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_HEAD_LOSS_CHANCE     = b.comment("Curios слот 'head' (голова). -1.0 = использовать общий шанс.")
                .defineInRange("curiosHeadLossChance", -1.0D, -1.0D, 1.0D);
        CURIOS_BODY_LOSS_CHANCE     = b.comment("Curios слот 'body' (тело/плащи и т.д.). -1.0 = использовать общий шанс.")
                .defineInRange("curiosBodyLossChance", -1.0D, -1.0D, 1.0D);

        CURIOS_SLOT_OVERRIDES = b.comment(
                "Переопределения для конкретных Curios-слотов по их ID. Формат: \"slotId=chance\".",
                "Примеры: \"ring=0.25\", \"belt=1.0\", \"back=-1\" (наследовать общий шанс).",
                "Приоритет: overrides > именованные слоты > curiosDefaultLossChance."
        ).defineListAllowEmpty(
                List.of("death_item_loss", "curios", "slotOverrides"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+=(-?\\d*(?:\\.\\d+)?)$")
        );

        b.pop(); // curios

        // ───────────────────────  FILTERS  ──────────────────────
        b.push("filters");

        WHITELIST_ITEMS = b.comment(
                "БЕЛЫЙ список предметов (items), которые НИКОГДА не удаляются.",
                "Формат каждого элемента: \"namespace:item\" (например, \"minecraft:netherite_sword\")."
        ).defineListAllowEmpty(
                List.of("death_item_loss", "filters", "whitelistItems"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+:[a-z0-9_\\-./]+$")
        );

        WHITELIST_TAGS = b.comment(
                "БЕЛЫЙ список тегов (tags), которые НИКОГДА не удаляются.",
                "Формат: \"namespace:tag\" (например, \"forge:ingots\")."
        ).defineListAllowEmpty(
                List.of("death_item_loss", "filters", "whitelistTags"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+:[a-z0-9_\\-./]+$")
        );

        BLACKLIST_ITEMS = b.comment(
                "ЧЁРНЫЙ список предметов (items), которые ВСЕГДА удаляются (если не перекрыты белым списком).",
                "Формат: \"namespace:item\"."
        ).defineListAllowEmpty(
                List.of("death_item_loss", "filters", "blacklistItems"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+:[a-z0-9_\\-./]+$")
        );

        BLACKLIST_TAGS = b.comment(
                "ЧЁРНЫЙ список тегов (tags), которые ВСЕГДА удаляются (если не перекрыты белым списком).",
                "Формат: \"namespace:tag\"."
        ).defineListAllowEmpty(
                List.of("death_item_loss", "filters", "blacklistTags"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+:[a-z0-9_\\-./]+$")
        );

        b.pop(); // filters

        // ───────────────────────  MODIFIERS  ────────────────────
        b.push("modifiers");

        TAG_CHANCE_MODIFIERS = b.comment(
                "Дополнительные модификаторы шанса по ТЕГАМ предметов.",
                "Формат каждой записи: \"namespace:tag = ±N%\" (проценты со знаком). Примеры:",
                "  \"forge:ingots = -10%\"  → снизить шанс удаления всех слитков на 10% от базового;",
                "  \"minecraft:wool = +25%\" → поднять шанс для всей шерсти на 25% от базового.",
                "Если на предмет подходит несколько правил — коэффициенты перемножаются.",
                "Белый/чёрный списки всегда в приоритете."
        ).defineListAllowEmpty(
                List.of("death_item_loss", "modifiers", "tagChanceModifiers"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+:[a-z0-9_\\-./]+\\s*=\\s*[+\\-]?\\d+(?:\\.\\d+)?%$")
        );

        b.pop(); // modifiers

        // ───────────────────────  CHAT  ─────────────────────────
        b.push("chat");

        CHAT_SHOW_SUMMARY = b.comment(
                "Показывать ли умершему игроку сводку потерянных предметов в чате."
        ).define("showLossSummary", true);

        CHAT_MAX_ENTRIES = b.comment(
                "Максимум строк в сводке (остальные будут скрыты с припиской \"+N ещё\")."
        ).defineInRange("lossSummaryMaxEntries", 12, 1, 100);

        b.pop(); // chat

        b.pop(); // death_item_loss

        SPEC = b.build();
    }

    private DeathItemLossConfig() {}
}
