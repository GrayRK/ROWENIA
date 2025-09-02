package com.ixiastraixi.roweniafull.mechanics.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Кэш ближайшего центра крепости на сетке регионов 512×512, чтобы не дергать поиск каждый раз. */
public class StrongholdCache extends SavedData {
    private static final String SAVE_ID = "roweniafull_stronghold_cache";
    private final Map<Long, BlockPos> byRegion = new HashMap<>();

    public static StrongholdCache get(ServerLevel lvl) {
        return lvl.getDataStorage().computeIfAbsent(StrongholdCache::load, StrongholdCache::new, SAVE_ID);
    }

    /** Возвращает кэшированное значение или вычисляет через finder и сохраняет. */
    public BlockPos getOrPut(ServerLevel lvl, BlockPos from, Supplier<BlockPos> finder) {
        long key = regionKey(from);
        BlockPos cached = byRegion.get(key);
        if (cached != null) return cached;
        BlockPos found = finder.get();
        if (found != null) {
            byRegion.put(key, found.immutable());
            setDirty();
        }
        return found;
    }

    private static long regionKey(BlockPos p) {
        // 512-блочные регионы: быстрее конвергенция кэша и достаточно для наших поисков
        int rx = p.getX() >> 9;
        int rz = p.getZ() >> 9;
        return (((long) rx) << 32) | (rz & 0xffffffffL);
    }

    // ---------- SavedData NBT ----------
    public static StrongholdCache load(CompoundTag tag) {
        StrongholdCache c = new StrongholdCache();
        ListTag list = tag.getList("e", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            long k = t.getLong("k");
            BlockPos p = new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z"));
            c.byRegion.put(k, p);
        }
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, BlockPos> e : byRegion.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("k", e.getKey());
            t.putInt("x", e.getValue().getX());
            t.putInt("y", e.getValue().getY());
            t.putInt("z", e.getValue().getZ());
            list.add(t);
        }
        tag.put("e", list);
        return tag;
    }
}
