package com.ixiastraixi.roweniafull.mechanics.portal;

import com.ixiastraixi.roweniafull.registry.portal.PortalBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PortalReceiverBlockEntity extends BlockEntity {
    public PortalReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(PortalBlockEntities.PORTAL_RECEIVER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel sl) {
            // При первой загрузке чанка (включая генерацию структуры) реестр пополнится
            ReceiverIndex.get(sl).add(sl, getBlockPos());
        }
    }
}
