package com.ixiastraixi.roweniafull.registry.warforge;

import com.ixiastraixi.roweniafull.mechanics.warforge.item.QualityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import com.ixiastraixi.roweniafull.RoweniaFull;

public class WarforgeItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RoweniaFull.MOD_ID);

    //Forge
    public static final RegistryObject<Item> WEAPON_FORGE_ITEM = ITEMS.register("weapon_forge",
            () -> new BlockItem(WarforgeBlocks.WEAPON_FORGE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARMOR_FORGE_ITEM = ITEMS.register("armor_forge",
            () -> new BlockItem(WarforgeBlocks.ARMOR_FORGE.get(), new Item.Properties()));

    //Blade blank
    public static final RegistryObject<Item> BLADE_BLANK_STONE =
            ITEMS.register("blade_blank_stone",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> BLADE_BLANK_IRON =
            ITEMS.register("blade_blank_iron",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> BLADE_BLANK_GOLD =
            ITEMS.register("blade_blank_gold",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> BLADE_BLANK_DIAMOND =
            ITEMS.register("blade_blank_diamond",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> BLADE_BLANK_NETHERITE =
            ITEMS.register("blade_blank_netherite",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));

    public static void init(IEventBus bus) { ITEMS.register(bus); }

    //Handle
    public static final RegistryObject<Item> HANDLE =
            ITEMS.register("handle",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> STURDY_HANDLE =
            ITEMS.register("sturdy_handle",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> ERGONOMIC_HANDLE =
            ITEMS.register("ergonomic_handle",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> HEAVY_HANDLE =
            ITEMS.register("heavy_handle",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));

    //Armor plate
    public static final RegistryObject<Item> ARMOR_PLATE_STONE =
            ITEMS.register("armor_plate_stone",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> ARMOR_PLATE_IRON =
            ITEMS.register("armor_plate_iron",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> ARMOR_PLATE_GOLD =
            ITEMS.register("armor_plate_gold",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> ARMOR_PLATE_DIAMOND =
            ITEMS.register("armor_plate_diamond",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
    public static final RegistryObject<Item> ARMOR_PLATE_NETHERITE =
            ITEMS.register("armor_plate_netherite",
                    () -> new QualityItem(new Item.Properties(), QualityItem.PrefixForm.FEM));
}
