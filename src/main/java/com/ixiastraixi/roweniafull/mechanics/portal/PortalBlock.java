package com.ixiastraixi.roweniafull.mechanics.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

public class PortalBlock extends Block {

    // Подкрути под свой модпак при желании
    private static final int INDEX_SEARCH_RADIUS_BLOCKS = 4096; // лимит для поиска в индексе
    private static final int LOCAL_SCAN_RADIUS_BLOCKS   = 128;  // локальный скан после прогрузки
    private static final int STRUCTURE_SEARCH_RADIUS_CH = 64;   // поиск крепости (чанки)
    private static final int TICKET_RADIUS_CH           = 2;    // выдаём region tickets на 5×5 чанков
    private static final int SNAP_DELAY_TICKS           = 20;   // через сколько тиков «доснапить» к приёмнику

    public PortalBlock(Properties props) { super(props); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        ServerLevel sl = (ServerLevel) level;

        // 1) Сохраняем точку возврата
        var pd = sp.getPersistentData();
        pd.putString(PortalReceiverBlock.TAG_LAST_PORTAL_DIM, sl.dimension().location().toString());
        pd.putInt(PortalReceiverBlock.TAG_LAST_PORTAL_X, pos.getX());
        pd.putInt(PortalReceiverBlock.TAG_LAST_PORTAL_Y, pos.getY());
        pd.putInt(PortalReceiverBlock.TAG_LAST_PORTAL_Z, pos.getZ());

        // 2) Пытаемся сразу найти приёмник (на прогруженных — мгновенно)
        BlockPos receiver = ReceiverIndex.get(sl).findNearest(sl, pos, INDEX_SEARCH_RADIUS_BLOCKS);
        if (receiver != null) {
            BlockPos tp = receiver.above();
            sp.teleportTo(sl, tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5, sp.getYRot(), sp.getXRot());
            return InteractionResult.CONSUME;
        }

        // 3) Ищем ближайшую крепость (integrated_stronghold:stronghold), берём из кэша или вычисляем
        BlockPos strongholdCenter = findNearestIntegratedStrongholdCenter(sl, pos);
        if (strongholdCenter == null) {
            sp.displayClientMessage(Component.translatable("message.roweniafull.portal.no_receiver"), true);
            return InteractionResult.CONSUME;
        }

        // 4) Асинхронная подгрузка области крепости (region tickets), БЕЗ блокировки треда сервера
        addRegionTickets(sl, strongholdCenter, TICKET_RADIUS_CH);

        // 5) Мгновенно тпшим игрока на безопасную поверхность у центра крепости
        BlockPos safe = safeSurfaceAt(sl, strongholdCenter);
        sp.teleportTo(sl, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, sp.getYRot(), sp.getXRot());

        // 6) Через SNAP_DELAY_TICKS тиков «доснапим» к конкретному приёмнику (когда чанки успеют догрузиться)
        scheduleSnapToReceiver(sl, sp.getUUID(), strongholdCenter, SNAP_DELAY_TICKS);

        return InteractionResult.CONSUME;
    }

    /** Ищем ближайшую крепость мода и возвращаем её центр (с кэшем по регионам). */
    private static BlockPos findNearestIntegratedStrongholdCenter(ServerLevel level, BlockPos from) {
        return StrongholdCache.get(level).getOrPut(level, from, () -> {
            HolderGetter<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            ResourceKey<Structure> KEY = ResourceKey.create(
                    Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath("integrated_stronghold", "stronghold")
            );
            Holder<Structure> holder;
            try { holder = structures.getOrThrow(KEY); } catch (Exception e) { return null; }
            var pair = level.getChunkSource().getGenerator().findNearestMapStructure(
                    level, HolderSet.direct(holder), from, STRUCTURE_SEARCH_RADIUS_CH, false
            );
            return pair != null ? pair.getFirst() : null;
        });
    }

    /** Выдаём регион‑тикеты для асинхронной прогрузки чанков вокруг центра. */
    private static void addRegionTickets(ServerLevel level, BlockPos center, int radiusChunks) {
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                level.getChunkSource().addRegionTicket(
                        TicketType.PORTAL, new ChunkPos(cx + dx, cz + dz), 2, center
                );
            }
        }
    }

    /** Планируем «доснап» к приёмнику через delayTicks тиков (без блокировки). */
    private static void scheduleSnapToReceiver(ServerLevel level, UUID playerId, BlockPos center, int delayTicks) {
        level.getServer().execute(new Runnable() {
            int ticks = 0;
            @Override public void run() {
                if (++ticks < delayTicks) {
                    level.getServer().execute(this); // подождём ещё тик
                    return;
                }
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                if (sp == null || sp.level() != level) return;

                // Сначала пробуем из индекса (BE приёмника должен был зарегистрироваться)
                BlockPos receiver = ReceiverIndex.get(level).findNearest(level, center, LOCAL_SCAN_RADIUS_BLOCKS);
                if (receiver == null) {
                    receiver = scanForReceiverBlocks(level, center, LOCAL_SCAN_RADIUS_BLOCKS);
                    if (receiver != null) ReceiverIndex.get(level).add(level, receiver);
                }
                if (receiver != null) {
                    BlockPos tp = receiver.above();
                    sp.teleportTo(level, tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5, sp.getYRot(), sp.getXRot());
                }
            }
        });
    }

    /** Локальный скан блоков вокруг точки на предмет PortalReceiverBlock (fallback). */
    private static BlockPos scanForReceiverBlocks(ServerLevel level, BlockPos from, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
                    if (level.getBlockState(m).getBlock() instanceof PortalReceiverBlock) {
                        double d2 = from.distSqr(m);
                        if (d2 < best) { best = d2; nearest = m.immutable(); }
                    }
                }
            }
        }
        return nearest;
    }

    /** Безопасная поверхность у центра (центр чанка по высоте WORLD_SURFACE). */
    private static BlockPos safeSurfaceAt(ServerLevel level, BlockPos center) {
        ChunkPos cp = new ChunkPos(center);
        int x = cp.getMiddleBlockX();
        int z = cp.getMiddleBlockZ();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return new BlockPos(x, y, z);
    }
}
