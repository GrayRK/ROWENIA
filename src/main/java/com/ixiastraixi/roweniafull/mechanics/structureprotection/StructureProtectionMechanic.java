package com.ixiastraixi.roweniafull.mechanics.structureprotection;

import com.ixiastraixi.roweniafull.config.StructureProtectionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

// piece-level API
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

public class StructureProtectionMechanic {

    // ======= анти-спам уведомлений (action bar + звук) =======
    private static final Map<UUID, Long> LAST_NOTIFY_TICK = new HashMap<>();
    private static final int NOTIFY_COOLDOWN_TICKS = 10; // ~0.5 сек при 20 TPS

    private static void notifyActionBar(ServerPlayer player, ServerLevel level, Component msg, boolean withSound) {
        long now = level.getGameTime();
        long last = LAST_NOTIFY_TICK.getOrDefault(player.getUUID(), -1000L);

        // ActionBar заменяет предыдущее сообщение — чат не засоряем
        player.displayClientMessage(msg, true);

        if (withSound && (now - last >= NOTIFY_COOLDOWN_TICKS)) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
            LAST_NOTIFY_TICK.put(player.getUUID(), now);
        }
    }

    // ───────── ЛОМАНИЕ БЛОКОВ ─────────
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        BlockPos pos = event.getPos();
        Set<ResourceLocation> ignored = parseIgnoredStructures(StructureProtectionConfig.SP_IGNORED_STRUCTURES.get());
        if (isInsideAnyProtectedPiece(level, pos, ignored)) {
            notifyActionBar(player, level,
                    Component.literal("Магия хранителей: ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("ломать нельзя").withStyle(ChatFormatting.RED)),
                    true);
            event.setCanceled(true);
        }
    }

    // ───────── УСТАНОВКА БЛОКОВ ─────────
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        BlockPos pos = event.getPos();
        Set<ResourceLocation> ignored = parseIgnoredStructures(StructureProtectionConfig.SP_IGNORED_STRUCTURES.get());
        if (isInsideAnyProtectedPiece(level, pos, ignored)) {
            notifyActionBar(player, level,
                    Component.literal("Магия хранителей: ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("строить нельзя").withStyle(ChatFormatting.RED)),
                    true);
            event.setCanceled(true);
        }
    }

    // ───────── ЖИДКОСТИ: клик по блоку (любой FLUID_HANDLER_ITEM) ─────────
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator()) return;

        var cap = event.getItemStack().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (cap.isEmpty()) return;
        IFluidHandlerItem handler = cap.get();

        BlockPos clicked = event.getPos();
        BlockState clickedState = level.getBlockState(clicked);
        BlockPos targetPos = clickedState.canBeReplaced()
                ? clicked
                : clicked.relative(event.getFace());

        boolean hasFluidToPlace = hasFluidToPlaceHere(handler);
        boolean canPlaceHere = hasFluidToPlace && canPlaceFluidHere(level, clicked, clickedState, targetPos, handler);
        boolean canPickUpHere = canPickupFluidHere(level, clicked, handler);

        // Если тут ни лить, ни черпать нельзя — не мешаем обычному взаимодействию
        if (!(canPlaceHere || canPickUpHere)) return;

        // Разрешаем открытие «функциональных» блоков без шифта, но режем предмет (чтобы жидкость не лилась)
        if (!player.isShiftKeyDown() && isFunctionalBlock(level, clickedState, clicked)) {
            Set<ResourceLocation> ignored = parseIgnoredStructures(StructureProtectionConfig.SP_IGNORED_STRUCTURES.get());
            if (isInsideAnyProtectedPiece(level, canPickUpHere ? clicked : targetPos, ignored)) {
                event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
                resyncInventoryHard(player); // ← важен ресинк, чтобы ведро не «пустело» визуально
                return;
            }
        }

        // Обычный случай — полностью запрещаем действие
        Set<ResourceLocation> ignored = parseIgnoredStructures(StructureProtectionConfig.SP_IGNORED_STRUCTURES.get());
        BlockPos checkPos = canPickUpHere ? clicked : targetPos;
        if (isInsideAnyProtectedPiece(level, checkPos, ignored)) {
            notifyActionBar(player, level,
                    Component.literal("Магия хранителей: ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(canPickUpHere ? "черпать нельзя" : "разливать нельзя")
                                    .withStyle(ChatFormatting.RED)),
                    true);
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setCancellationResult(InteractionResult.FAIL);
            resyncInventoryHard(player); // ← и тут обязательно
        }
    }

    // ───────── ЖИДКОСТИ: клик «в воздух» ─────────
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator()) return;

        var cap = event.getItemStack().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (cap.isEmpty()) return;
        IFluidHandlerItem handler = cap.get();

        HitResult hit = player.pick(4.5D, 0F, false);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos clicked = ((BlockHitResult) hit).getBlockPos();
        BlockState clickedState = level.getBlockState(clicked);
        BlockPos targetPos = clickedState.canBeReplaced()
                ? clicked
                : clicked.relative(((BlockHitResult) hit).getDirection());

        boolean hasFluidToPlace = hasFluidToPlaceHere(handler);
        boolean canPlaceHere = hasFluidToPlace && canPlaceFluidHere(level, clicked, clickedState, targetPos, handler);
        boolean canPickUpHere = canPickupFluidHere(level, clicked, handler);

        if (!(canPlaceHere || canPickUpHere)) return;

        Set<ResourceLocation> ignored = parseIgnoredStructures(StructureProtectionConfig.SP_IGNORED_STRUCTURES.get());
        BlockPos checkPos = canPickUpHere ? clicked : targetPos;
        if (isInsideAnyProtectedPiece(level, checkPos, ignored)) {
            notifyActionBar(player, level,
                    Component.literal("Магия хранителей: ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(canPickUpHere ? "черпать нельзя" : "разливать нельзя")
                                    .withStyle(ChatFormatting.RED)),
                    true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            resyncInventoryHard(player); // ← и здесь
        }
    }

    // ───────── ГРЯДКИ: запрет топтания внутри постройки ─────────
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        Set<ResourceLocation> ignored = parseIgnoredStructures(StructureProtectionConfig.SP_IGNORED_STRUCTURES.get());
        if (isInsideAnyProtectedPiece(level, pos, ignored)) {
            event.setCanceled(true);
        }
    }

    // ───────── ЛОГИКА ЖИДКОСТЕЙ ─────────
    private static boolean hasFluidToPlaceHere(IFluidHandlerItem handler) {
        FluidStack sim = handler.drain(1000, IFluidHandlerItem.FluidAction.SIMULATE);
        return !sim.isEmpty();
    }

    private static boolean canPlaceFluidHere(ServerLevel level,
                                             BlockPos clicked, BlockState clickedState, BlockPos targetPos,
                                             IFluidHandlerItem handler) {
        FluidStack sim = handler.drain(1000, IFluidHandlerItem.FluidAction.SIMULATE);
        if (sim.isEmpty()) return false;
        var fluid = sim.getFluid();

        // waterlogging — в кликнутый блок
        if (clickedState.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(level, clicked, clickedState, fluid)) {
            return true;
        }
        // либо соседняя ячейка должна быть заменяема
        return level.getBlockState(targetPos).canBeReplaced();
    }

    private static boolean canPickupFluidHere(ServerLevel level, BlockPos clicked, IFluidHandlerItem handler) {
        var fs = level.getBlockState(clicked).getFluidState();
        if (fs.isEmpty()) return false;
        FluidStack want = new FluidStack(fs.getType(), 1000);
        return handler.fill(want, IFluidHandlerItem.FluidAction.SIMULATE) > 0;
    }

    private static boolean isFunctionalBlock(ServerLevel level, BlockState state, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) return true;
        return state.getMenuProvider(level, pos) != null;
    }

    // ───────── ДЕТЕКТОР (piece-level + чёрный список структур) ─────────
    private static boolean isInsideAnyProtectedPiece(ServerLevel level, BlockPos pos, Set<ResourceLocation> ignored) {
        if (!level.structureManager().hasAnyStructureAt(pos)) return false;

        Registry<Structure> reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Holder.Reference<Structure> holder : reg.holders().toList()) {
            ResourceLocation key = holder.key().location();
            if (ignored.contains(key)) continue;

            var start = level.structureManager().getStructureWithPieceAt(pos, holder.value());
            if (start == null || !start.isValid()) continue;

            for (StructurePiece piece : start.getPieces()) {
                BoundingBox bb = piece.getBoundingBox();
                if (bb != null && bb.isInside(pos)) return true;
            }
        }
        return false;
    }

    // ───────── КОНФИГ ─────────
    private static Set<ResourceLocation> parseIgnoredStructures(List<? extends String> raw) {
        Set<ResourceLocation> out = new HashSet<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s == null) continue;
            ResourceLocation rl = ResourceLocation.tryParse(s.trim());
            if (rl != null) out.add(rl);
        }
        return out;
    }

    // ───────── Жёсткий ресинк инвентаря (фикс «пустого ведра») ─────────
    private static void resyncInventoryHard(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        int stateId = player.inventoryMenu.getStateId();
        player.connection.send(new ClientboundContainerSetContentPacket(
                player.inventoryMenu.containerId,
                stateId,
                player.inventoryMenu.getItems(),
                player.inventoryMenu.getCarried()
        ));
    }
}
