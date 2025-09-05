package com.ixiastraixi.roweniafull.registry.warforge;

import com.ixiastraixi.roweniafull.mechanics.warforge.menu.ArmorForgeMenu;
import com.ixiastraixi.roweniafull.mechanics.warforge.menu.WeaponForgeMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;


public class WarforgeMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<MenuType<ArmorForgeMenu>> ARMOR_FORGE_MENU =
            MENUS.register("armor_forge", () -> IForgeMenuType.create(ArmorForgeMenu::new));

    public static final RegistryObject<MenuType<WeaponForgeMenu>> WEAPON_FORGE_MENU =
            MENUS.register("weapon_forge", () -> IForgeMenuType.create(WeaponForgeMenu::new));

    public static void init(IEventBus bus) {
        MENUS.register(bus);
    }
}
