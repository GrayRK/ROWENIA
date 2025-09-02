package com.ixiastraixi.roweniafull.registry.warforge;

/*
 * Rowenia / Warforge — cleaned SOT snapshot
 * File: WarforgeRecipes.java
 * Purpose: Registration: recipe type and serializer for Weapon Forging.
 * Notes:
 *  - Comments rewritten to explain code blocks and hot parameters (coords, textures, slots).
 *  - Keep this file as source of truth as of 2025-08-30 13:57:50.
 */


import com.ixiastraixi.roweniafull.RoweniaFull;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.ArmorForgingRecipe;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.WeaponForgingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

/**
 * Регистрация рецептов кузницы.
 */


public class WarforgeRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MOD_ID);

//Рецепты оружия
    public static final RegistryObject<RecipeType<WeaponForgingRecipe>> WEAPON_FORGING_TYPE =
            TYPES.register("weapon_forging", () -> new RecipeType<>() {
                @Override public String toString() { return MOD_ID + ":weapon_forging"; }
            });
    public static final RegistryObject<RecipeSerializer<WeaponForgingRecipe>> WEAPON_FORGING_SERIALIZER =
            SERIALIZERS.register("weapon_forging", WeaponForgingRecipe.Serializer::new);
//Рецепты броня
    public static final RegistryObject<RecipeType<ArmorForgingRecipe>> ARMOR_FORGING_TYPE =
            TYPES.register("armor_forging", () -> new RecipeType<>() { public String toString(){ return MOD_ID+":armor_forging"; } });
    public static final RegistryObject<RecipeSerializer<ArmorForgingRecipe>> ARMOR_FORGING_SERIALIZER =
            SERIALIZERS.register("armor_forging", ArmorForgingRecipe.Serializer::new);


    public static void init(IEventBus bus) {
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }
}
