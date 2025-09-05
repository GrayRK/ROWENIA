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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ArmorForgeBlock extends Block implements EntityBlock {

//-----------------------------
//       Конструкторы
//-----------------------------

    // Конструктор по умолчанию
    public ArmorForgeBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
        );
    }

    //Второй конструктор: принимает готовые свойства
    public ArmorForgeBlock(BlockBehaviour.Properties props) {
        super(props.requiresCorrectToolForDrops());
    }

//-----------------------
//       Логика
//-----------------------

    // При клике правой кнопкой открываем меню через Forge NetworkHooks.
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, provider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // При замене блока/разрушении сбрасываем инвентарь и буфер топлива
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            var be = level.getBlockEntity(pos);
            if (be instanceof ArmorForgeBlockEntity forge) {
                forge.dropAllContents();
                forge.dropFuelBuffer();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // При разрушении рукой/инструментом также сбрасываем содержимое
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        var be = level.getBlockEntity(pos);
        if (be instanceof ArmorForgeBlockEntity forge) {
            forge.dropAllContents();
            forge.dropFuelBuffer();
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // Создаём новую block‑entity
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArmorForgeBlockEntity(pos, state);
    }

    // Возвращаем тикер только на сервере и только для нужного типа BE
    @Nullable
    @Override
    public BlockEntityTicker getTicker(Level level, BlockState state, BlockEntityType type) {
        if (level.isClientSide) return null;
        return type == WarforgeBlockEntities.ARMOR_FORGE_BE.get()
                ? (lvl, p, st, be) -> ArmorForgeBlockEntity.serverTick(lvl, p, st, (ArmorForgeBlockEntity) be)
                : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Базовая реализация размещения
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx);
    }
}
