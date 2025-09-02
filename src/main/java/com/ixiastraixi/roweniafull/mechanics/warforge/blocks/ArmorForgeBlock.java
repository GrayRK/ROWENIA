package com.ixiastraixi.roweniafull.mechanics.warforge.blocks;

import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.ArmorForgeBlockEntity;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ArmorForgeBlock extends Block implements EntityBlock {
    public ArmorForgeBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArmorForgeBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == WarforgeBlockEntities.ARMOR_FORGE_BE.get()
                ? (lvl, p, st, be) -> ArmorForgeBlockEntity.serverTick(lvl, p, st, (ArmorForgeBlockEntity) be)
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ArmorForgeBlockEntity forge && player instanceof ServerPlayer sp) {
                // важно: передаём pos (Forge сам положит в буфер для Menu)
                NetworkHooks.openScreen(sp, (MenuProvider) forge, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ArmorForgeBlockEntity forge) forge.dropAll(level, pos);
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState(); }
}
