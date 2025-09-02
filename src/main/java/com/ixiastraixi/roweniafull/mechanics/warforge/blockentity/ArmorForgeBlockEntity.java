
package com.ixiastraixi.roweniafull.mechanics.warforge.blockentity;

import com.ixiastraixi.roweniafull.mechanics.warforge.menu.ArmorForgeMenu;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.ArmorForgingRecipe;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeBlockEntities;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ArmorForgeBlockEntity extends BlockEntity implements MenuProvider {

    // === Индексы слотов (горячие) ===
    // 0..8 — входы 3x3; 9 — топливо; 10 — выход
    public static final int SLOT_0=0, SLOT_1=1, SLOT_2=2, SLOT_3=3, SLOT_4=4, SLOT_5=5, SLOT_6=6, SLOT_7=7, SLOT_8=8;
    public static final int SLOT_FUEL = 9;
    public static final int SLOT_OUT  = 10;

    // === Вкладки ===
    public static final int TAB_HELMET=0, TAB_CHEST=1, TAB_LEGS=2, TAB_BOOTS=3, TAB_SHIELD=4;

    // === DataSlots (для меню/экрана) ===
    // 0:progress, 1:maxProgress, 2:fuelBuffer(0..4), 3:cooldown, 4:currentTab
    public final ContainerData data = new SimpleContainerData(5);

    // --- инвентарь (11 слотов) ---
    private final NonNullList<ItemStack> items = NonNullList.withSize(11, ItemStack.EMPTY);

    // --- состояние прогресса/топлива ---
    private int progress = 0;
    private int maxProgress = 100;
    private int fuelBuffer = 0;
    private int fuelPullCooldown = 15;
    private long nextFuelGrabTick = -1L;    // когда можно забирать следующий уголь из слота (gameTime сервера); -1 = ещё не запланировано
    private boolean fuelHadItemLastTick = false;

    // --- крафт ---
    private boolean craftReady = false;
    private boolean holdingCraft = false;

    // --- вкладка ---
    private int currentTab = TAB_HELMET;

    // --- отпечаток входов для автосброса результата ---
    private long lastInputsFingerprint = 0L;
    private boolean suppressClearOnInputsChangeOnce = false;
    // --- сколько ждать между подбором угля из слота топлива ---
    private static final int FUEL_DELAY_TICKS = 15;

    public ArmorForgeBlockEntity(BlockPos pos, BlockState state) { super(WarforgeBlockEntities.ARMOR_FORGE_BE.get(), pos, state); }

    // === Контейнер для меню ===
    private final net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(items.size()) {
        @Override public int getMaxStackSize() { return 64; }
        @Override public int getContainerSize() { return items.size(); }
        @Override public boolean isEmpty() { for (ItemStack s : items) if (!s.isEmpty()) return false; return true; }
        @Override public ItemStack getItem(int i) { return items.get(i); }
        @Override public ItemStack removeItem(int i, int count) {
            ItemStack res = items.get(i).split(count);
            if (!res.isEmpty()) setChanged();
            return res;
        }
        @Override public ItemStack removeItemNoUpdate(int i) { ItemStack s = items.get(i); items.set(i, ItemStack.EMPTY); return s; }
        @Override public void setItem(int i, ItemStack st) {
            items.set(i, st);
            if (st.getCount() > st.getMaxStackSize()) st.setCount(st.getMaxStackSize());
            setChanged();
        }
        @Override public void setChanged() { ArmorForgeBlockEntity.this.setChanged(); }
        @Override public boolean stillValid(Player p) { return p.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64; }
        @Override public void clearContent() { for (int i=0;i<items.size();i++) items.set(i, ItemStack.EMPTY); setChanged(); }
    };

    public net.minecraft.world.SimpleContainer container() { return container; }

    // === MenuProvider ===
    @Override public Component getDisplayName() { return Component.translatable("block.roweniafull.armor_forge"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ArmorForgeMenu(id, inv, this, data); }

    // === Паблик-API для меню ===
    public void setHoldingCraft(boolean v) { holdingCraft = v; }
    public boolean isCraftReady() { return craftReady; }
    public int currentTabIdx() { return currentTab; }
    public void nextTab() { currentTab = (currentTab + 1) % 5; setChanged(); }
    public void prevTab() { currentTab = (currentTab + 4) % 5; setChanged(); }

    private void consumeInputsForRecipe(ArmorForgingRecipe r) {
        int[] act = ArmorForgingRecipe.activeIndices(r.form());
        for (int idx : act) {
            ItemStack in = this.items.get(idx);
            if (!in.isEmpty()) in.shrink(1); // по 1 предмету из активных слотов
        }
        setChanged();
    }

    // === Серверный тик ===
    public static void serverTick(Level lvl, BlockPos pos, BlockState st, ArmorForgeBlockEntity be) {
        if (lvl.isClientSide) return;

        // Сброс результата при изменении входов
        long fp = be.inputsFingerprint();
        if (fp != be.lastInputsFingerprint) {
            be.lastInputsFingerprint = fp;
            if (be.suppressClearOnInputsChangeOnce) {
                be.suppressClearOnInputsChangeOnce = false;
            } else {
                be.progress = 0;
                be.craftReady = false;
                if (!be.items.get(SLOT_OUT).isEmpty()) be.items.set(SLOT_OUT, ItemStack.EMPTY);
                be.setChanged();
            }
        }

        // Подбор топлива в буфер с задержкой
        boolean fuelNow = !be.items.get(SLOT_FUEL).isEmpty();
        long now = lvl.getGameTime();
        if (fuelNow && !be.fuelHadItemLastTick) {
            be.nextFuelGrabTick = Math.max(be.nextFuelGrabTick, now + FUEL_DELAY_TICKS);
        }
        be.fuelHadItemLastTick = fuelNow;
        if (fuelNow && be.fuelBuffer < 4 && be.nextFuelGrabTick >= 0 && now >= be.nextFuelGrabTick) {
            if (be.refillBufferFromFuelSlot()) {
                be.nextFuelGrabTick = now + FUEL_DELAY_TICKS;
            }
        }

        // Поиск рецепта для текущей вкладки
        Optional<ArmorForgingRecipe> rOpt = be.findRecipeForCurrentTab(lvl.getRecipeManager());

        // Шаг прогресса
        boolean canStep = false;
        if (!be.craftReady && rOpt.isPresent() && be.fuelBuffer > 0) {
            ItemStack outTry = rOpt.get().assemble(be.activeSnapshot(), lvl.registryAccess());
            canStep = be.canOutputAccept(outTry);
        }

        if (be.holdingCraft && canStep) {
            be.progress++;
            if (be.progress >= Math.max(1, be.maxProgress)) {
                be.progress = be.maxProgress;
                be.craftReady = true;
            }
        } else if (be.progress > 0) {
            be.progress = Math.max(0, be.progress - 1);
        }

        // Завершение крафта при удержании
        if (be.craftReady && be.holdingCraft) {
            ArmorForgingRecipe r = rOpt.orElse(null);
            if (r != null && be.fuelBuffer > 0) {
                ItemStack result = r.assemble(be.activeSnapshot(), lvl.registryAccess());
                if (be.pushToOut(result)) {
                    be.consumeInputsForRecipe(r);
                    be.consumeFuelUnit(now);      // <-- передаём now, чтобы задать паузу
                    be.progress = 0;
                    be.craftReady = false;
                    be.suppressClearOnInputsChangeOnce = true;
                }
            } else be.craftReady = false;
        }

        be.recalcFuelNeedAndTime();
        be.pushData();
        be.setChanged();
    }

    private boolean canOutputAccept(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, stack)) return false;
        int sum = out.getCount() + stack.getCount();
        return sum <= out.getMaxStackSize();
    }

    private boolean pushToOut(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) { items.set(SLOT_OUT, stack.copy()); return true; }
        if (!ItemStack.isSameItemSameTags(out, stack)) return false;
        out.grow(stack.getCount());
        return true;
    }

    private void consumeFuelUnit(long now) {
        if (fuelBuffer > 0) {
            fuelBuffer--;
            // ВАЖНО: даже если nextFuelGrabTick уже "в прошлом", переносим окно вперёд.
            nextFuelGrabTick = Math.max(nextFuelGrabTick, now + FUEL_DELAY_TICKS);
            setChanged();
        }
    }

    private boolean refillBufferFromFuelSlot() {
        if (fuelBuffer >= 4) return false;
        ItemStack f = items.get(SLOT_FUEL);
        if (f.isEmpty()) return false;
        if (f.getItem() != Items.COAL && f.getItem() != Items.CHARCOAL) return false;
        f.shrink(1);
        fuelBuffer++;
        setChanged();
        return true;
    }

    private Optional<ArmorForgingRecipe> findRecipeForCurrentTab(RecipeManager rm) {
        if (!(level instanceof ServerLevel sl)) return Optional.empty();
        net.minecraft.world.SimpleContainer snap = activeSnapshot();
        for (ArmorForgingRecipe r : sl.getRecipeManager().getAllRecipesFor(WarforgeRecipes.ARMOR_FORGING_TYPE.get())) {
            if (r.form().ordinal() == currentTab && r.matches(snap, level)) return Optional.of(r);
        }
        return Optional.empty();
    }

    /** Снимок только активных входов под форму вкладки */
    private net.minecraft.world.SimpleContainer activeSnapshot() {
        net.minecraft.world.SimpleContainer snap = new net.minecraft.world.SimpleContainer(9);
        int[] act = activeIndices();
        for (int i=0;i<9;i++) {
            boolean use=false; for (int a:act) if (a==i){use=true;break;}
            snap.setItem(i, use ? items.get(i).copy() : ItemStack.EMPTY);
        }
        return snap;
    }

    private int[] activeIndices() {
        return switch (currentTab) {
            case TAB_HELMET    -> new int[]{0,1,2,3,5};
            case TAB_CHEST     -> new int[]{0,2,3,4,5,6,7,8};
            case TAB_LEGS      -> new int[]{0,1,2,3,5,6,8};
            case TAB_BOOTS     -> new int[]{3,5,6,8};
            case TAB_SHIELD    -> new int[]{0,1,2,3,4,5,7};
            default -> new int[]{0};
        };
    }

    private void recalcFuelNeedAndTime() {
        switch (currentTab) {
            case TAB_HELMET -> maxProgress = 80;
            case TAB_CHEST  -> maxProgress = 140;
            case TAB_LEGS   -> maxProgress = 120;
            case TAB_BOOTS  -> maxProgress = 70;
            case TAB_SHIELD -> maxProgress = 100;
            default -> maxProgress = 100;
        }
    }

    private long inputsFingerprint() {
        long h = 1469598103934665603L;
        int[] act = activeIndices();
        for (int idx: act) {
            ItemStack s = items.get(idx);
            int id = s.isEmpty() ? -1 : net.minecraft.world.item.Item.getId(s.getItem());
            int c  = s.isEmpty()? 0  : s.getCount();
            h ^= (id * 1099511628211L) ^ (c * 1469598103934665603L);
            h *= 1099511628211L;
        }
        h ^= currentTab * 0x9E3779B97F4A7C15L;
        return h;
    }

    // === Дроп содержимого при разрушении ===
    public void dropAll(Level lvl, BlockPos pos) {
        for (int i=0;i<items.size();i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) Containers.dropItemStack(lvl, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, s);
            items.set(i, ItemStack.EMPTY);
        }

        setChanged();
    }

    // === Save/Load ===
    private void pushData() { data.set(0,progress); data.set(1,maxProgress); data.set(2,fuelBuffer); data.set(3,fuelPullCooldown); data.set(4,currentTab); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("FuelBuffer", fuelBuffer);
        tag.putLong("NextFuelGrabTick", nextFuelGrabTick);
        tag.putBoolean("CraftReady", craftReady);
        tag.putBoolean("Hold", holdingCraft);
        tag.putInt("Tab", currentTab);
        var list = new net.minecraft.nbt.ListTag();
        for (int i=0;i<items.size();i++) {
            CompoundTag e = new CompoundTag();
            e.putByte("Slot",(byte)i);
            items.get(i).save(e);
            list.add(e);
        }
        tag.put("Items", list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        fuelBuffer = tag.getInt("FuelBuffer");
        nextFuelGrabTick = tag.contains("NextFuelGrabTick") ? tag.getLong("NextFuelGrabTick") : -1L;
        if (nextFuelGrabTick < 0 && !items.get(SLOT_FUEL).isEmpty() && fuelBuffer < 4) {
            long now = (level != null) ? level.getGameTime() : 0L;
            nextFuelGrabTick = now + 15;
        }
        craftReady = tag.getBoolean("CraftReady");
        holdingCraft = tag.getBoolean("Hold");
        currentTab = tag.getInt("Tab");
        var list = tag.getList("Items",10);
        for (int i=0;i<list.size();i++) {
            CompoundTag e = list.getCompound(i);
            int slot = e.getByte("Slot") & 255;
            if (slot>=0 && slot<items.size()) items.set(slot, ItemStack.of(e));
        }
    }


    // вызывался из меню для сброса локальных эффектов при забирании результата
    public void resetAfterTake(Level lvl) { /* держим для единообразия */ }
}
