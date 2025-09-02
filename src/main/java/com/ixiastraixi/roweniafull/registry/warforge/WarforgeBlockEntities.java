package com.ixiastraixi.roweniafull.registry.warforge;

import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.ArmorForgeBlockEntity;
import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.WeaponForgeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

public class WarforgeBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<BlockEntityType<WeaponForgeBlockEntity>> WEAPON_FORGE_BE =
            BLOCK_ENTITIES.register("weapon_forge",
                    () -> BlockEntityType.Builder.of(
                            WeaponForgeBlockEntity::new,
                            WarforgeBlocks.WEAPON_FORGE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ArmorForgeBlockEntity>> ARMOR_FORGE_BE =
            BLOCK_ENTITIES.register("armor_forge",
                    () -> BlockEntityType.Builder.of(
                            ArmorForgeBlockEntity::new,
                            WarforgeBlocks.ARMOR_FORGE.get()
                    ).build(null));

    public static void init(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }


}
