package com.ixiastraixi.roweniafull.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public final class GameClassConfig {
    // регистрация: общий конфиг в ./config
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "RoweniaGameClass.toml");
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WARRIOR_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HUNTER_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SORCERER_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WIZARD_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COOK_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINER_BLOCKS;

    static {
        BUILDER.push("classes");

        // пустые дефолты; валидатор — только строки
        WARRIOR_BLOCKS = BUILDER
                .comment("Allowed block IDs for Warrior (e.g. minecraft:anvil)")
                .defineListAllowEmpty(List.of("warrior_blocks"), List::of, o -> o instanceof String);

        HUNTER_BLOCKS = BUILDER
                .comment("Allowed block IDs for Hunter (e.g. minecraft:furnace)")
                .defineListAllowEmpty(List.of("hunter_blocks"), List::of, o -> o instanceof String);

        SORCERER_BLOCKS = BUILDER
                .comment("Allowed block IDs for Sorcerer (e.g. minecraft:enchanting_table)")
                .defineListAllowEmpty(List.of("sorcerer_blocks"), List::of, o -> o instanceof String);

        WIZARD_BLOCKS = BUILDER
                .comment("Allowed block IDs for Wizard (e.g. minecraft:brewing_stand)")
                .defineListAllowEmpty(List.of("wizard_blocks"), List::of, o -> o instanceof String);

        COOK_BLOCKS = BUILDER
                .comment("Allowed block IDs for Cook (e.g. minecraft:smoker)")
                .defineListAllowEmpty(List.of("cook_blocks"), List::of, o -> o instanceof String);

        MINER_BLOCKS = BUILDER
                .comment("Allowed block IDs for Miner (e.g. minecraft:smithing_table)")
                .defineListAllowEmpty(List.of("miner_blocks"), List::of, o -> o instanceof String);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // геттеры
    public static List<? extends String> warrior()  { return WARRIOR_BLOCKS.get(); }
    public static List<? extends String> hunter()   { return HUNTER_BLOCKS.get(); }
    public static List<? extends String> sorcerer() { return SORCERER_BLOCKS.get(); }
    public static List<? extends String> wizard()   { return WIZARD_BLOCKS.get(); }
    public static List<? extends String> cook()     { return COOK_BLOCKS.get(); }
    public static List<? extends String> miner()    { return MINER_BLOCKS.get(); }
}
