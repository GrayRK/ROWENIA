package com.ixiastraixi.roweniafull.mechanics.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class PortalReceiverBlock extends BaseEntityBlock {
    public static final String TAG_LAST_PORTAL_DIM = "RoweniaLastPortalDim";
    public static final String TAG_LAST_PORTAL_X = "RoweniaLastPortalX";
    public static final String TAG_LAST_PORTAL_Y = "RoweniaLastPortalY";
    public static final String TAG_LAST_PORTAL_Z = "RoweniaLastPortalZ";

    public PortalReceiverBlock(Properties props) { super(props); }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock() && level instanceof ServerLevel sl) {
            ReceiverIndex.get(sl).remove(sl, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;

        var tag = sp.getPersistentData();
        if (!tag.contains(TAG_LAST_PORTAL_DIM)) {
            sp.displayClientMessage(Component.translatable("message.roweniafull.portal.no_return_point"), true);
            return InteractionResult.CONSUME;
        }

        var dimKey = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.tryParse(tag.getString(TAG_LAST_PORTAL_DIM))
        );
        ServerLevel target = sp.server.getLevel(dimKey);
        if (target == null) {
            sp.displayClientMessage(Component.translatable("message.roweniafull.portal.dimension_missing"), true);
            return InteractionResult.CONSUME;
        }

        var x = tag.getInt(TAG_LAST_PORTAL_X);
        var y = tag.getInt(TAG_LAST_PORTAL_Y);
        var z = tag.getInt(TAG_LAST_PORTAL_Z);
        var safe = new BlockPos(x, y, z);
        // Подстрахуемся: поднимем чуть выше, если внутри блока
        var m = safe.mutable();
        for (int i = 0; i < 6 && !target.isEmptyBlock(m); i++) m.move(0, 1, 0);

        sp.teleportTo(target, m.getX() + 0.5, m.getY(), m.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        return InteractionResult.CONSUME;
    }

    // --- BE ---
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalReceiverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // тикер не нужен
        return null;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }
}
