package com.ixiastraixi.roweniafull.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public final class StructureProtectionConfig {

    public static final ForgeConfigSpec SPEC;

    // Чёрный список структур, которые ИГНОРИРУЮТСЯ защитой (ResourceLocation, напр. "minecraft:village_plains")
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SP_IGNORED_STRUCTURES;

    // Зарегистрировать отдельный файл RoweniaStructureProtection.toml
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "RoweniaStructureProtection.toml");
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment(
                "Механика защиты структур.",
                "Запрещает ломать/ставить блоки и взаимодействовать с жидкостями внутри структур.",
                "Список ниже позволяет исключать отдельные структуры из-под защиты."
        );

        b.push("structure_protection");

        SP_IGNORED_STRUCTURES = b.comment(
                "Список структур (ResourceLocation), которые НЕ защищаются (игнорируются механикой).",
                "Формат каждого элемента: \"namespace:path\". Примеры:",
                "  \"minecraft:village_plains\", \"integrated_stronghold:library\"",
                "Оставьте список пустым, чтобы защита применялась ко всем найденным структурам."
        ).defineListAllowEmpty(
                List.of("structure_protection", "ignoredStructures"),
                () -> new ArrayList<String>(),
                o -> (o instanceof String s) && s.matches("^[a-z0-9_\\-./]+:[a-z0-9_\\-./]+$")
        );

        b.pop(); // structure_protection

        SPEC = b.build();
    }

    private StructureProtectionConfig() {}
}
