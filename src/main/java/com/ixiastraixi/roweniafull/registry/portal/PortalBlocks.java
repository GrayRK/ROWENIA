package com.ixiastraixi.roweniafull.registry.portal;

import com.ixiastraixi.roweniafull.RoweniaFull;
import com.ixiastraixi.roweniafull.mechanics.portal.PortalBlock;
import com.ixiastraixi.roweniafull.mechanics.portal.PortalReceiverBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class PortalBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RoweniaFull.MOD_ID);

    public static final RegistryObject<Block> PORTAL_BLOCK = BLOCKS.register("portal_block",
            () -> new PortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()
            ));

    public static final RegistryObject<Block> PORTAL_RECEIVER = BLOCKS.register("portal_receiver",
            () -> new PortalReceiverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()
            ));
}