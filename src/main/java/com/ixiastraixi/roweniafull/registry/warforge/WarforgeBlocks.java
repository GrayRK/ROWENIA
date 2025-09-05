package com.ixiastraixi.roweniafull.registry.warforge;

import com.ixiastraixi.roweniafull.mechanics.warforge.blocks.ArmorForgeBlock;
import com.ixiastraixi.roweniafull.mechanics.warforge.blocks.WeaponForgeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

public class WarforgeBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    // Кузница оружия
    public static final RegistryObject<Block> WEAPON_FORGE =
            BLOCKS.register("weapon_forge", WeaponForgeBlock::new);
    // Кузница брони
    public static final RegistryObject<Block> ARMOR_FORGE =
            BLOCKS.register("armor_forge", ArmorForgeBlock::new);

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
