package com.ixiastraixi.roweniafull.mechanics.warforge.blocks;

/*
 * Rowenia / Warforge — cleaned SOT snapshot
 * File: WeaponForgeBlock.java
 * Purpose: Block behavior: placement, interaction (open menu), drops on break, BE ticker.
 * Notes:
 *  - Comments rewritten to explain code blocks and hot parameters (coords, textures, slots).
 *  - Keep this file as source of truth as of 2025-08-30 13:57:50.
 */


import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.WeaponForgeBlockEntity;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class WeaponForgeBlock extends Block implements EntityBlock {

    public WeaponForgeBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        );
    }
    public WeaponForgeBlock(BlockBehaviour.Properties props) {
        super(props.requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            var be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, provider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // дроп всех слотов + буфера топлива при замене/ломании
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            var be = level.getBlockEntity(pos);
            if (be instanceof WeaponForgeBlockEntity forge) {
                forge.dropAllContents();
                forge.dropFuelBuffer();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        var be = level.getBlockEntity(pos);
        if (be instanceof WeaponForgeBlockEntity forge) {
            forge.dropAllContents();
            forge.dropFuelBuffer();
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponForgeBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == WarforgeBlockEntities.WEAPON_FORGE_BE.get()
                ? (lvl, p, st, be) -> WeaponForgeBlockEntity.serverTick(lvl, p, st, (WeaponForgeBlockEntity) be)
                : null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return super.getStateForPlacement(ctx); }
}
