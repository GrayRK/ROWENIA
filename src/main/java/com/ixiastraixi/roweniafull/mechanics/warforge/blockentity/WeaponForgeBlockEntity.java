package com.ixiastraixi.roweniafull.mechanics.warforge.blockentity;

import com.ixiastraixi.roweniafull.mechanics.warforge.menu.WeaponForgeMenu;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.WeaponForgingRecipe;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeBlockEntities;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WeaponForgeBlockEntity extends BlockEntity implements MenuProvider {

    // --- вкладки форм оружия ---
    public static final int TYPE_SHORT = 0;      // короткое
    public static final int TYPE_LONG = 1;       // длинное
    public static final int TYPE_TWO_HANDED = 2; // двуручное
    public static final int TYPE_POLEARM = 3;    // древковое

    // --- индексы слотов инвентаря ---
    public static final int SLOT_A = 0; // лезвие/часть А
    public static final int SLOT_B = 1; // лезвие/часть B
    public static final int SLOT_C = 2; // часть C
    public static final int SLOT_D = 3; // часть D
    public static final int SLOT_E = 4; // нижняя рукоять
    public static final int SLOT_FUEL = 5; // топливо
    public static final int SLOT_OUT  = 6; // результат
    public static final int SLOT_F    = 7; // верхняя рукоять для polearm

    // --- внутренний инвентарь кузницы ---
    private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
    /** Обёртка, через которую меню получает доступ к слотам. */
    private final net.minecraft.world.Container container = new net.minecraft.world.Container() {
        @Override public int getContainerSize() { return items.size(); }
        @Override public boolean isEmpty() { for (ItemStack s : items) if (!s.isEmpty()) return false; return true; }
        @Override public ItemStack getItem(int i) { return items.get(i); }
        @Override public ItemStack removeItem(int i, int count) {
            ItemStack res = items.get(i).split(count);
            if (!res.isEmpty()) setChanged();
            return res;
        }
        @Override public ItemStack removeItemNoUpdate(int i) {
            ItemStack s = items.get(i); items.set(i, ItemStack.EMPTY); return s;
        }
        @Override public void setItem(int i, ItemStack st) {
            items.set(i, st);
            if (st.getCount() > st.getMaxStackSize()) st.setCount(st.getMaxStackSize());
            setChanged();
        }
        @Override public void setChanged() { WeaponForgeBlockEntity.this.setChanged(); }
        @Override public boolean stillValid(Player p) {
            if (level == null || level.getBlockEntity(worldPosition) != WeaponForgeBlockEntity.this) return false;
            return p.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64.0;
        }
        @Override public void clearContent() { items.clear(); }
    };

    // --- параметры крафта и состояния ---
    private int  progress = 0;              // текущий прогресс
    private int  maxProgress = 0;           // сколько тиков требуется
    private int  currentTypeIdx = TYPE_SHORT; // выбранная вкладка
    private boolean isHoldingCraft = false; // игрок удерживает кнопку крафта
    private boolean craftReady = false;     // результат готов и лежит в OUT
    private boolean fuelHadItemLastTick = false; // был ли предмет в слоте топлива в прошлый тик
    private boolean suppressClearOnInputsChangeOnce = false; // флаг для пропуска автоочистки OUT

    // отпечаток входов для отслеживания изменений
    private long lastInputsFingerprint = Long.MIN_VALUE;

    // --- буфер топлива и параметры потребления ---
    private static final int FUEL_BUFFER_CAP = 4; // максимальный запас
    private int fuelCoal = 0;       // количество угля
    private int fuelCharcoal = 0;   // количество древесного угля
    private int fuelPullCooldown = 0; // задержка подсоса топлива (тики)
    private int fuelNeed = 1;       // сколько топлива нужно на крафт

    // dataSlots: индексы для синхронизации с клиентом
    // 0=progress,1=max,2=fuel(0..4),3=fuelNeed,4=tab
    private final ContainerData dataAccess = new SimpleContainerData(5) {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> totalBuffer();
                case 3 -> fuelNeed;
                case 4 -> currentTypeIdx;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> maxProgress = v;
                case 2 -> { int c = Math.max(0, Math.min(FUEL_BUFFER_CAP, v)); fuelCoal=c; fuelCharcoal=0; }
                case 3 -> fuelNeed = v;
                case 4 -> currentTypeIdx = v;
            }
        }
        @Override public int getCount() { return 5; }
    };

    public WeaponForgeBlockEntity(BlockPos pos, BlockState state) {
        super(WarforgeBlockEntities.WEAPON_FORGE_BE.get(), pos, state);
        recalcFuelNeedAndTime();
    }

    // ===== UI/меню =====
    public ContainerData dataAccess() { return dataAccess; }
    public net.minecraft.world.Container container() { return container; }
    @Override public Component getDisplayName() { return Component.literal("Weapon Forge"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new WeaponForgeMenu(id, inv, this, dataAccess);
    }

    public void setHoldingCraft(boolean hold) { isHoldingCraft = hold; }
    public boolean isCraftReady() { return craftReady; }

    public void nextType() { currentTypeIdx = (currentTypeIdx + 1) % 4; onInputsChanged(); }
    public void prevType() { currentTypeIdx = (currentTypeIdx + 3) % 4; onInputsChanged(); }

    private void onInputsChanged() {
        recalcFuelNeedAndTime();
        progress = 0;
        craftReady = false;
        // OUT должен быть пуст до завершения — чистим
        if (!items.get(SLOT_OUT).isEmpty()) items.set(SLOT_OUT, ItemStack.EMPTY);
        setChanged();
    }

    // ===== Тик сервера =====
    public static void serverTick(Level lvl, BlockPos pos, BlockState st, WeaponForgeBlockEntity be) {
        if (lvl.isClientSide) return;

        // 0) если входы/вкладка поменялись — сбросить состояние и очистить OUT
        long fp = be.inputsFingerprint();
        if (fp != be.lastInputsFingerprint) {
            be.lastInputsFingerprint = fp;
            if (be.suppressClearOnInputsChangeOnce) {
                // пропускаем единожды: сами же только что изменили входы (списали ингредиенты)
                be.suppressClearOnInputsChangeOnce = false;
            } else {
                be.progress = 0;
                be.craftReady = false;
                if (!be.items.get(SLOT_OUT).isEmpty()) be.items.set(SLOT_OUT, ItemStack.EMPTY);
                be.setChanged();
            }
        }

        // 1) подсос топлива в буфер — по 1 шт с задержкой
        boolean fuelNowNotEmpty = !be.items.get(SLOT_FUEL).isEmpty();
        if (fuelNowNotEmpty && !be.fuelHadItemLastTick) {
            be.fuelPullCooldown = Math.max(be.fuelPullCooldown, 15); // ~0.75с
        }
        be.fuelHadItemLastTick = fuelNowNotEmpty;
        if (be.fuelPullCooldown > 0) be.fuelPullCooldown--;
        if (be.fuelPullCooldown == 0 && be.refillBufferFromFuelSlot()) {
            be.fuelPullCooldown = 15;
        }

        // 2) считаем рецепт на лету (только по активным слотам)
        Optional<WeaponForgingRecipe> rOpt = be.findRecipeForCurrentTab(lvl.getRecipeManager());

        // 3) шаг прогресса — только пока НЕ craftReady, есть рецепт и хватает буфера, и есть место в OUT
        boolean canStepUp = false;
        if (!be.craftReady && be.isHoldingCraft && rOpt.isPresent() && be.totalBuffer() >= be.fuelNeed) {
            ItemStack outTry = rOpt.get().assemble(be.activeSnapshot(), lvl.registryAccess());
            canStepUp = be.canOutputAccept(outTry);
        }

        if (canStepUp) {
            be.progress++;
            if (be.progress >= Math.max(1, be.maxProgress)) {
                // завершили: тратим буфер и КЛАДЁМ результат в OUT; теперь можно забирать
                if (be.consumeBuffer(be.fuelNeed)) {
                    ItemStack outStack = rOpt.get().assemble(be.activeSnapshot(), lvl.registryAccess());
                    be.consumeInputsForCurrentTab();
                    be.suppressClearOnInputsChangeOnce = true;
                    if (!outStack.isEmpty()) be.mergeOutput(outStack);
                    be.craftReady = true;
                    be.fuelPullCooldown = Math.max(be.fuelPullCooldown, 15); // ~0.75s пауза перед ПЕРВЫМ углём после крафта
                }
                be.progress = 0;
                be.setChanged();
            }
        } else {
            // плавный спад только когда кнопку отпустили
            if (be.progress > 0 && !be.isHoldingCraft) {
                be.progress--;
                be.setChanged();
            }
        }
    }

    // ===== Рецепты / снапшоты =====
    private Optional<WeaponForgingRecipe> findRecipeForCurrentTab(RecipeManager rm) {
        var snap = activeSnapshot();
        WeaponForgingRecipe.Form need = switch (currentTypeIdx) {
            case TYPE_SHORT      -> WeaponForgingRecipe.Form.SHORT;
            case TYPE_LONG       -> WeaponForgingRecipe.Form.LONG;
            case TYPE_TWO_HANDED -> WeaponForgingRecipe.Form.TWO_HANDED;
            case TYPE_POLEARM    -> WeaponForgingRecipe.Form.POLEARM;
            default              -> WeaponForgingRecipe.Form.SHORT;
        };
        for (WeaponForgingRecipe r : rm.getAllRecipesFor(WarforgeRecipes.WEAPON_FORGING_TYPE.get())) {
            if (r.form() == need && r.matches(snap, level)) return Optional.of(r);
        }
        return Optional.empty();
    }

    /**
     * Снимок только активных входных слотов. Остальные ячейки пустые.
     */
    private net.minecraft.world.SimpleContainer activeSnapshot() {
        var snap = new net.minecraft.world.SimpleContainer(8);
        for (int idx : activeIndices()) {
            snap.setItem(idx, items.get(idx).copy());
        }
        return snap;
    }

    private int[] activeIndices() {
        return switch (currentTypeIdx) {
            case TYPE_SHORT      -> new int[]{SLOT_B, SLOT_E};
            case TYPE_LONG       -> new int[]{SLOT_A, SLOT_B, SLOT_E};
            case TYPE_TWO_HANDED -> new int[]{SLOT_A, SLOT_B, SLOT_C, SLOT_D, SLOT_E};
            case TYPE_POLEARM    -> new int[]{SLOT_B, SLOT_F, SLOT_E};
            default              -> new int[]{SLOT_B, SLOT_E};
        };
    }

    private long inputsFingerprint() {
        long h = 1125899906842597L;
        for (int i : activeIndices()) {
            ItemStack s = items.get(i);
            int id = s.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(s.getItem());
            int c  = s.isEmpty() ? 0  : s.getCount();
            h = h * 31 + id;
            h = h * 31 + c;
        }
        h = h * 31 + currentTypeIdx;
        return h;
    }

    // ===== Топливо =====
    /** Суммарный запас топлива в буфере (0..4). */
    private int totalBuffer() { return Math.min(FUEL_BUFFER_CAP, fuelCoal + fuelCharcoal); }

    /** Пытается потратить указанное количество топлива. */
    private boolean consumeBuffer(int amt) {
        if (totalBuffer() < amt) return false;
        int take = amt;
        int fromCoal = Math.min(fuelCoal, take);
        fuelCoal -= fromCoal; take -= fromCoal;
        if (take > 0) {
            int fromChar = Math.min(fuelCharcoal, take);
            fuelCharcoal -= fromChar; take -= fromChar;
        }
        setChanged();
        return take == 0;
    }

    /** Подтягивает топливо из слота в буфер. */
    private boolean refillBufferFromFuelSlot() {
        int space = FUEL_BUFFER_CAP - totalBuffer();
        if (space <= 0) return false;

        ItemStack f = items.get(SLOT_FUEL);
        if (f.isEmpty()) return false;

        Item it = f.getItem();
        if (it == Items.COAL && space >= 1) {
            f.shrink(1); if (f.isEmpty()) items.set(SLOT_FUEL, ItemStack.EMPTY);
            fuelCoal += 1; setChanged(); return true;
        }
        if (it == Items.CHARCOAL && space >= 1) {
            f.shrink(1); if (f.isEmpty()) items.set(SLOT_FUEL, ItemStack.EMPTY);
            fuelCharcoal += 1; setChanged(); return true;
        }
        return false;
    }

    // ===== Работа с OUT/ингредиентами =====
    /** Проверяет, поместится ли результат в выходной слот. */
    private boolean canOutputAccept(ItemStack toInsert) {
        if (toInsert.isEmpty()) return false;
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, toInsert)) return false;
        return out.getCount() + toInsert.getCount() <= out.getMaxStackSize();
    }

    /** Сливает новый предмет в выходной слот. */
    private void mergeOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) items.set(SLOT_OUT, stack.copy());
        else if (ItemStack.isSameItemSameTags(out, stack)) out.grow(stack.getCount());
    }

    /** Списывает ингредиенты в соответствии с текущей вкладкой. */
    public void consumeInputsForCurrentTab() {
        switch (currentTypeIdx) {
            case TYPE_SHORT -> { items.get(SLOT_B).shrink(1); items.get(SLOT_E).shrink(1); }
            case TYPE_LONG  -> { items.get(SLOT_A).shrink(1); items.get(SLOT_B).shrink(1); items.get(SLOT_E).shrink(1); }
            case TYPE_TWO_HANDED -> {
                items.get(SLOT_A).shrink(1); items.get(SLOT_B).shrink(1);
                items.get(SLOT_C).shrink(1); items.get(SLOT_D).shrink(1);
                items.get(SLOT_E).shrink(1);
            }
            case TYPE_POLEARM -> { items.get(SLOT_B).shrink(1); items.get(SLOT_F).shrink(1); items.get(SLOT_E).shrink(1); }
        }
        for (int i : activeIndices()) if (items.get(i).getCount() <= 0) items.set(i, ItemStack.EMPTY);
        setChanged();
    }

    /** Вызывается из слота OUT при взятии предмета. */
    public void resetAfterTake(Level lvl) {
        craftReady = false;
        progress = 0;
        // OUT снова пуст до следующего завершения
        if (!items.get(SLOT_OUT).isEmpty()) items.set(SLOT_OUT, ItemStack.EMPTY);
        setChanged();
    }

    // ===== прогресс/тайминги =====
    /** Пересчёт потребности топлива и времени крафта по текущей вкладке. */
    private void recalcFuelNeedAndTime() {
        fuelNeed = switch (currentTypeIdx) {
            case TYPE_LONG       -> 2;
            case TYPE_TWO_HANDED -> 3;
            default              -> 1;
        };
        maxProgress = switch (currentTypeIdx) {
            case TYPE_SHORT      -> 100;
            case TYPE_POLEARM    -> 120;
            case TYPE_LONG       -> 160;
            case TYPE_TWO_HANDED -> 220;
            default              -> 140;
        };
    }

    // ===== save/load =====
    /** Сохранение состояния в NBT. */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("FuelCoal", fuelCoal);
        tag.putInt("FuelCharcoal", fuelCharcoal);
        tag.putInt("FuelNeed", fuelNeed);
        tag.putInt("TypeIdx", currentTypeIdx);
        tag.putBoolean("CraftReady", craftReady);
    }

    /** Загрузка состояния из NBT. */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        fuelCoal = tag.getInt("FuelCoal");
        fuelCharcoal = tag.getInt("FuelCharcoal");
        fuelNeed = tag.getInt("FuelNeed");
        currentTypeIdx = tag.getInt("TypeIdx");
        craftReady = tag.getBoolean("CraftReady");
    }

    // ===== дроп содержимого =====
    /** Выбрасывает все предметы из инвентаря. */
    public void dropAllContents() {
        if (level instanceof ServerLevel sl) {
            for (ItemStack s : items) if (!s.isEmpty())
                Containers.dropItemStack(sl, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, s);
        }
    }

    /** Выбрасывает накопленное топливо. */
    public void dropFuelBuffer() {
        if (!(level instanceof ServerLevel sl)) return;
        while (fuelCoal-- > 0)
            Containers.dropItemStack(sl, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, new ItemStack(Items.COAL));
        fuelCoal = 0;
        while (fuelCharcoal-- > 0)
            Containers.dropItemStack(sl, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, new ItemStack(Items.CHARCOAL));
        fuelCharcoal = 0;
    }
}
