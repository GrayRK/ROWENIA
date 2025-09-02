package com.ixiastraixi.roweniafull.mechanics.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class ReceiverIndex extends SavedData {
    private static final String SAVE_ID = "roweniafull_portal_receivers";

    private final List<BlockPos> receivers = new ArrayList<>();

    public static ReceiverIndex get(ServerLevel level) {
        // ❗ без Factory — просто две функции: load и конструктор
        return level.getDataStorage().computeIfAbsent(
                ReceiverIndex::load,
                ReceiverIndex::new,
                SAVE_ID
        );
    }

    public void add(ServerLevel level, BlockPos pos) {
        if (!receivers.contains(pos)) {
            receivers.add(pos.immutable());
            setDirty();
        }
    }

    public void remove(ServerLevel level, BlockPos pos) {
        if (receivers.remove(pos)) setDirty();
    }

    /** Находит ближайшего приёмника в радиусе (в блоках). Если radius <= 0 — без ограничения. */
    public BlockPos findNearest(ServerLevel level, BlockPos from, int radius) {
        BlockPos best = null;
        double bestDist2 = Double.MAX_VALUE;
        double radius2 = radius > 0 ? (double) radius * radius : Double.MAX_VALUE;
        for (BlockPos p : receivers) {
            double d2 = from.distSqr(p);
            if (d2 < bestDist2 && d2 <= radius2) {
                bestDist2 = d2;
                best = p;
            }
        }
        return best;
    }

    // ---------- SavedData NBT ----------
    public static ReceiverIndex load(CompoundTag tag) {
        ReceiverIndex idx = new ReceiverIndex();
        ListTag list = tag.getList("list", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ct = list.getCompound(i);
            BlockPos p = new BlockPos(ct.getInt("x"), ct.getInt("y"), ct.getInt("z"));
            idx.receivers.add(p);
        }
        return idx;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos p : receivers) {
            CompoundTag ct = new CompoundTag();
            ct.putInt("x", p.getX());
            ct.putInt("y", p.getY());
            ct.putInt("z", p.getZ());
            list.add(ct);
        }
        tag.put("list", list);
        return tag;
    }
}
