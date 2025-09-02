package com.ixiastraixi.roweniafull.registry.warforge;

/*
 * Rowenia / Warforge — cleaned SOT snapshot
 * File: WarforgeMenus.java
 * Purpose: Registration: menu/container types.
 * Notes:
 *  - Comments rewritten to explain code blocks and hot parameters (coords, textures, slots).
 *  - Keep this file as source of truth as of 2025-08-30 13:57:50.
 */


import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.ArmorForgeBlockEntity;
import com.ixiastraixi.roweniafull.mechanics.warforge.menu.ArmorForgeMenu;
import com.ixiastraixi.roweniafull.mechanics.warforge.menu.WeaponForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

/**
 * Регистрация контейнеров кузницы.
 */
public class WarforgeMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<MenuType<WeaponForgeMenu>> WEAPON_FORGE_MENU =
            MENUS.register("weapon_forge", () -> IForgeMenuType.create(WeaponForgeMenu::new));

    public static final RegistryObject<MenuType<ArmorForgeMenu>> ARMOR_FORGE_MENU =
            MENUS.register("armor_forge", () -> IForgeMenuType.create((id, inv, buf) -> {
                BlockPos pos = buf.readBlockPos();
                Level level = inv.player.level();
                BlockEntity be = level.getBlockEntity(pos);
                return (be instanceof ArmorForgeBlockEntity a) ? new ArmorForgeMenu(id, inv, a, a.data) : null;
            }));

    public static void init(IEventBus bus) {
        MENUS.register(bus);
    }
}
