package com.ixiastraixi.roweniafull;

import com.ixiastraixi.roweniafull.config.DeathItemLossConfig;
import com.ixiastraixi.roweniafull.config.GameClassConfig;
import com.ixiastraixi.roweniafull.config.StructureProtectionConfig;
import com.ixiastraixi.roweniafull.mechanics.gameclass.GameClassNamePrefix;
import com.ixiastraixi.roweniafull.registry.deathItemloss.DeathItemLossAttributes;
import com.ixiastraixi.roweniafull.registry.gameclass.GameClassAttributes;
import com.ixiastraixi.roweniafull.registry.portal.PortalBlockEntities;
import com.ixiastraixi.roweniafull.registry.portal.PortalBlocks;
import com.ixiastraixi.roweniafull.registry.portal.PortalItems;
import com.ixiastraixi.roweniafull.mechanics.deathItemloss.DeathItemLossMechanic;
import com.ixiastraixi.roweniafull.mechanics.gameclass.GameClassMechanic;
import com.ixiastraixi.roweniafull.mechanics.structureprotection.StructureProtectionMechanic;
import com.ixiastraixi.roweniafull.registry.warforge.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(RoweniaFull.MOD_ID)
public class RoweniaFull {
    public static final String MOD_ID = "roweniafull";

    public RoweniaFull() {
        // Конфиги
        DeathItemLossConfig.register();
        StructureProtectionConfig.register();
        GameClassConfig.register();

        // Регистрация атрибутов
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        GameClassAttributes.ATTRIBUTES.register(modBus);
        DeathItemLossAttributes.ATTRIBUTES.register(modBus);
        modBus.addListener(this::onEntityAttributeModification);

        // Регистрация главных механик
        MinecraftForge.EVENT_BUS.register(new DeathItemLossMechanic());
        MinecraftForge.EVENT_BUS.register(new StructureProtectionMechanic());
        MinecraftForge.EVENT_BUS.register(new GameClassMechanic());
        MinecraftForge.EVENT_BUS.register(new GameClassNamePrefix());

        // Отдельная регистрация для Warforge
        WarforgeBlocks.init(modBus);
        WarforgeItems.init(modBus);
        WarforgeBlockEntities.init(modBus);
        WarforgeMenus.init(modBus);
        WarforgeRecipes.init(modBus);


        // Регистрация блоков
        PortalBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        PortalItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Регистрация блочных сущностей
        PortalBlockEntities.BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());

    }

        // Регистрация каждого отдельного атрибута
        private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
            event.add(EntityType.PLAYER, DeathItemLossAttributes.ITEM_LOSS_CHANCE.get());

            event.add(EntityType.PLAYER, GameClassAttributes.CLASS_WARRIOR.get());
            event.add(EntityType.PLAYER, GameClassAttributes.CLASS_HUNTER.get());
            event.add(EntityType.PLAYER, GameClassAttributes.CLASS_SORCERER.get());
            event.add(EntityType.PLAYER, GameClassAttributes.CLASS_WIZARD.get());
            event.add(EntityType.PLAYER, GameClassAttributes.CLASS_COOK.get());
            event.add(EntityType.PLAYER, GameClassAttributes.CLASS_MINER.get());
        }
}
