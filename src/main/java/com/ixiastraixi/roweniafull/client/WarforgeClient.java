package com.ixiastraixi.roweniafull.client;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

import com.ixiastraixi.roweniafull.mechanics.warforge.screen.WeaponForgeScreen;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class WarforgeClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(WarforgeMenus.WEAPON_FORGE_MENU.get(), WeaponForgeScreen::new);
        });
    }
}
