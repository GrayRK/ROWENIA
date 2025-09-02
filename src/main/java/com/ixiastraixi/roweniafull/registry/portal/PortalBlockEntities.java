package com.ixiastraixi.roweniafull.registry.portal;

import com.ixiastraixi.roweniafull.RoweniaFull;
import com.ixiastraixi.roweniafull.mechanics.portal.PortalReceiverBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class PortalBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RoweniaFull.MOD_ID);

    public static final RegistryObject<BlockEntityType<PortalReceiverBlockEntity>> PORTAL_RECEIVER_BE =
            BLOCK_ENTITIES.register("portal_receiver",
                    () -> BlockEntityType.Builder.of(PortalReceiverBlockEntity::new, PortalBlocks.PORTAL_RECEIVER.get())
                            .build(null));
}
