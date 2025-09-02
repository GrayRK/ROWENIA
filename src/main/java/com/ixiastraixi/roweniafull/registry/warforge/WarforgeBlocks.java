package com.ixiastraixi.roweniafull.registry.warforge;

/*
 * Rowenia / Warforge — cleaned SOT snapshot
 * File: WarforgeBlocks.java
 * Purpose: Registration: blocks, Weapon Forge block.
 * Notes:
 *  - Comments rewritten to explain code blocks and hot parameters (coords, textures, slots).
 *  - Keep this file as source of truth as of 2025-08-30 13:57:50.
 */


import com.ixiastraixi.roweniafull.mechanics.warforge.blocks.ArmorForgeBlock;
import com.ixiastraixi.roweniafull.mechanics.warforge.blocks.WeaponForgeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

/**
 * Регистрация блоков кузницы.
 */
public class WarforgeBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    // Кузница оружия
    public static final RegistryObject<Block> WEAPON_FORGE =
            BLOCKS.register("weapon_forge", WeaponForgeBlock::new);
    // Кузница брони
    public static final RegistryObject<Block> ARMOR_FORGE = BLOCKS.register("armor_forge",
            () -> new ArmorForgeBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.ANVIL)));

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
