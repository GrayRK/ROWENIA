package com.ixiastraixi.roweniafull.registry.portal;

import com.ixiastraixi.roweniafull.RoweniaFull;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class PortalItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RoweniaFull.MOD_ID);

    public static final RegistryObject<Item> PORTAL_BLOCK_ITEM = ITEMS.register("portal_block",
            () -> new BlockItem(PortalBlocks.PORTAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> PORTAL_RECEIVER_ITEM = ITEMS.register("portal_receiver",
            () -> new BlockItem(PortalBlocks.PORTAL_RECEIVER.get(), new Item.Properties()));
}