package com.ixiastraixi.roweniafull.mechanics.warforge.blockentity;

import com.ixiastraixi.roweniafull.mechanics.warforge.blocks.WeaponForgeBlock;
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
import net.minecraft.world.SimpleContainer;
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

//-------------------------------------------------
//       Константы: Слоты, Типы, Состояния
//-------------------------------------------------

    // --- Вкладки ---
    public static final int TYPE_SHORT      = 0; // Короткое
    public static final int TYPE_LONG       = 1; // Длинное
    public static final int TYPE_TWO_HANDED = 2; // Двуручное
    public static final int TYPE_POLEARM    = 3; // Древковое

    // --- Индексы слотов ---
    public static final int SLOT_A    = 0; // Лезвие
    public static final int SLOT_B    = 1; // Лезвие
    public static final int SLOT_C    = 2; // Лезвие
    public static final int SLOT_D    = 3; // Лезвие
    public static final int SLOT_E    = 4; // Нижняя рукоять
    public static final int SLOT_F    = 5; // Верхняя рукоять
    public static final int SLOT_FUEL = 6; // Топливо
    public static final int SLOT_OUT  = 7; // Выход

    // --- Параметры крафта ---
    private int  progress          = 0;          // Текущий прогресс
    private int  maxProgress       = 0;          // Сколько тиков требуется
    private int  currentTypeIdx    = TYPE_SHORT; // Выбранная вкладка
    private boolean isHoldingCraft = false;      // Кнопка крафта удерживается
    private boolean craftReady     = false;      // Результат готов
    private boolean skipOutClear   = false;      // Пропуск автоочистки OUT

    // --- Параметры топлива ---
    private static final int FUEL_BUFFER_CAP = 4;     // Сколько максимум хранит буфер
    private int fuelCoal                     = 0;     // Сколько сейчас "Угля" в буфере
    private int fuelCharcoal                 = 0;     // Сколько сейчас "Древесного Угля" в буфере
    private int fuelPullCooldown             = 0;     // Задержка для забора в буфер
    private int fuelNeed                     = 1;     // Кол-во топлива на крафт
    private boolean fuelHadItemLastTick      = false; // Проверка было ли топливо в прошлый тик
    private boolean litCached = false;

    // --- Утилиты ---
    private long lastInputsFingerprint = Long.MIN_VALUE;

    // --- Внутренний инвентарь ---
    private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);

    // Обёртка, через которую меню работает с инвентарём
    private final net.minecraft.world.Container container = new net.minecraft.world.Container() {
        @Override
            public int getContainerSize() {
            return items.size();
        }
        @Override
            public boolean isEmpty() {
            for (ItemStack s : items) if (!s.isEmpty()) return false;
            return true;
        }
        @Override
            public ItemStack getItem(int i) {
            return items.get(i);
        }
        @Override
            public ItemStack removeItem(int i, int count) {
            ItemStack res = items.get(i).split(count);
            if (!res.isEmpty()) setChanged();
            return res;
        }
        @Override
            public ItemStack removeItemNoUpdate(int i) {
            ItemStack s = items.get(i);
            items.set(i, ItemStack.EMPTY);
            return s;
        }
        @Override
            public void setItem(int i, ItemStack st) {
            items.set(i, st);
            if (st.getCount() > st.getMaxStackSize())
                st.setCount(st.getMaxStackSize());
            setChanged();
        }
        @Override
            public void setChanged() {
            WeaponForgeBlockEntity.this.setChanged();
        }
        @Override
            public boolean stillValid(Player p) {
            if (level == null || level.getBlockEntity(worldPosition)
                    != WeaponForgeBlockEntity.this) return false;
            return p.distanceToSqr(
                    worldPosition.getX()+0.5,
                    worldPosition.getY()+0.5,
                    worldPosition.getZ()+0.5) <= 64.0;
        }
        @Override
            public void clearContent() {
            items.clear();
        }
    };

//-----------------------------------------------------------------
//       Контейнерные слоты для синхронизации с клиентом:
//-----------------------------------------------------------------

    private final ContainerData dataAccess = new SimpleContainerData(5) {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> totalBuffer();
                case 3 -> fuelNeed;
                case 4 -> currentTypeIdx;
                default -> 0;
            };
        }
        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> maxProgress = v;
                case 2 -> { int c = Math.max(0, Math.min(FUEL_BUFFER_CAP, v));
                    fuelCoal=c; fuelCharcoal=0; }
                case 3 -> fuelNeed = v;
                case 4 -> currentTypeIdx = v;
            }
        }
        @Override
        public int getCount() { return 5; }
    };

//---------------------------
//       Конструктор
//---------------------------

    public WeaponForgeBlockEntity(BlockPos pos, BlockState state) {
        super(WarforgeBlockEntities.WEAPON_FORGE_BE.get(), pos, state);
        recalcFuelNeedAndTime();
    }

//---------------------------------------------
//          Реализация MenuProvider
//---------------------------------------------

    public ContainerData dataAccess() { return dataAccess; }

    public net.minecraft.world.Container container() { return container; }

    @Override
    public Component getDisplayName() {
        return Component.literal("Weapon Forge");
    }
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new WeaponForgeMenu(id, inv, this, dataAccess);
    }

//-----------------------------------------------
//          Методы для меню / клиента
//-----------------------------------------------

    public void setHoldingCraft(boolean hold) { isHoldingCraft = hold; }

    public boolean isCraftReady() { return craftReady; }

    public void nextType() {
        currentTypeIdx = (currentTypeIdx + 1) % 4;
        onInputsChanged();
    }
    public void prevType() {
        currentTypeIdx = (currentTypeIdx + 3) % 4;
        onInputsChanged();
    }

    private void onInputsChanged() {
        recalcFuelNeedAndTime();
        progress = 0;
        craftReady = false;
        // OUT должен быть пуст до завершения — чистим
        if (!items.get(SLOT_OUT).isEmpty()) items.set(SLOT_OUT, ItemStack.EMPTY);
        setChanged();
    }

    private void syncLitToState() {
        if (level == null || level.isClientSide) return;

        boolean wantLit = totalBuffer() > 0;

        if (wantLit == litCached) return;
        litCached = wantLit;

        BlockState st = level.getBlockState(worldPosition);
        if (st.hasProperty(WeaponForgeBlock.LIT) && st.getValue(WeaponForgeBlock.LIT) != wantLit) {
            level.setBlock(worldPosition, st.setValue(WeaponForgeBlock.LIT, wantLit), 3);
        }
    }

    private boolean hasFuelInBuffer() {
        return this.totalBuffer() > 0; // подставь своё поле/логику (или fuelStoredInternal() > 0)
    }

//--------------------------------------------
//          Главный тик сервера
//--------------------------------------------

    public static void serverTick(Level lvl, BlockPos pos, BlockState st, WeaponForgeBlockEntity be) {
        if (lvl.isClientSide) return;

        // 1. Проверяем, изменились ли входы/вкладка: если да, сбрасываем прогресс и OUT
        long fp = be.inputsFingerprint();
        if (fp != be.lastInputsFingerprint) {
            be.lastInputsFingerprint = fp;
            if (be.skipOutClear) {
                be.skipOutClear = false;
            } else {
                be.progress = 0;
                be.craftReady = false;
                if (!be.items.get(SLOT_OUT).isEmpty()) be.items.set(SLOT_OUT, ItemStack.EMPTY);
                be.setChanged();
            }
        }

        // 2. Подтягиваем топливо в буфер с задержкой
        final boolean hadFuelBefore = be.hasFuelInBuffer();
        boolean fuelNowNotEmpty = !be.items.get(SLOT_FUEL).isEmpty();
        if (fuelNowNotEmpty && !be.fuelHadItemLastTick) {
            be.fuelPullCooldown = Math.max(be.fuelPullCooldown, 15);
        }
        be.fuelHadItemLastTick = fuelNowNotEmpty;
        if (be.fuelPullCooldown > 0) be.fuelPullCooldown--;
        if (be.fuelPullCooldown == 0 && be.refillBufferFromFuelSlot()) {
            be.fuelPullCooldown = 15;
            be.syncLitToState();
        }
        if (hadFuelBefore && !be.hasFuelInBuffer()) {
            be.syncLitToState();
        }

        // 3. Ищем рецепт для текущей вкладки
        Optional<WeaponForgingRecipe> rOpt = be.findRecipeForCurrentTab(lvl.getRecipeManager());

        // 4. Считаем, можем ли увеличить прогресс: нужна удержанная кнопка, рецепт и достаточный буфер топлива
        boolean canStepUp = false;
        if (!be.craftReady && be.isHoldingCraft && rOpt.isPresent() && be.totalBuffer() >= be.fuelNeed) {
            ItemStack outTry = rOpt.get().assemble(be.activeSnapshot(), lvl.registryAccess());
            canStepUp = be.canOutputAccept(outTry);
        }

        if (canStepUp) {
            be.progress++;
            if (be.progress >= Math.max(1, be.maxProgress)) {
                if (be.consumeBuffer(be.fuelNeed)) {
                    ItemStack outStack = rOpt.get().assemble(be.activeSnapshot(), lvl.registryAccess());
                    be.consumeInputsForCurrentTab();
                    be.skipOutClear = true;
                    if (!outStack.isEmpty()) be.mergeOutput(outStack);
                    be.craftReady = true;
                    be.fuelPullCooldown = Math.max(be.fuelPullCooldown, 15);
                }
                be.progress = 0;
                be.setChanged();
            }
        } else {
            if (be.progress > 0 && !be.isHoldingCraft) {
                be.progress--;
                be.setChanged();
            }
        }

        // 5. обновляем maxProgress/fuelNeed на случай смены вкладки
        be.recalcFuelNeedAndTime();
        be.setChanged();
    }

//-----------------------------------------------------
//          Поиск рецепта и снимки входов
//-----------------------------------------------------

    // Находит рецепт для текущей вкладки (формы брони), если он есть.
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

    // Снимок только активных слотов (остальные пустые).
    private SimpleContainer activeSnapshot() {
        var snap = new SimpleContainer(8);
        for (int idx : activeIndices()) {
            snap.setItem(idx, items.get(idx).copy());
        }
        return snap;
    }

    // Индексы активных слотов для текущего типа брони.
    // Эти массивы можно настраивать при изменении расположения слотов.
    private int[] activeIndices() {
        return switch (currentTypeIdx) {
            case TYPE_SHORT      -> new int[]{SLOT_B, SLOT_E};
            case TYPE_LONG       -> new int[]{SLOT_A, SLOT_B, SLOT_E};
            case TYPE_TWO_HANDED -> new int[]{SLOT_A, SLOT_B, SLOT_C, SLOT_D, SLOT_E};
            case TYPE_POLEARM    -> new int[]{SLOT_B, SLOT_F, SLOT_E};
            default              -> new int[]{SLOT_B, SLOT_E};
        };
    }

    // Отпечаток входов: используется для сброса прогресса при изменении слотов или вкладки.
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

//--------------------------------------------------------------
//          Топливо: буфер, пополнение и потребление
//--------------------------------------------------------------

    // Сколько всего топлива лежит в буфере (0..4)
    private int totalBuffer() { return Math.min(FUEL_BUFFER_CAP, fuelCoal + fuelCharcoal); }

    // Пытается потратить amt единиц топлива, возвращает true, если удалось.
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

    // Подтягивает 1 единицу топлива из слота в буфер (если есть место).
    private boolean refillBufferFromFuelSlot() {
        int space = FUEL_BUFFER_CAP - totalBuffer();
        if (space <= 0) return false;
        ItemStack f = items.get(SLOT_FUEL);
        if (f.isEmpty()) return false;
        Item it = f.getItem();
        if (it == Items.COAL && space >= 1) {
            f.shrink(1); if (f.isEmpty()) items.set(SLOT_FUEL, ItemStack.EMPTY);
            fuelCoal += 1;
            setChanged();
            return true;
        }
        if (it == Items.CHARCOAL && space >= 1) {
            f.shrink(1); if (f.isEmpty()) items.set(SLOT_FUEL, ItemStack.EMPTY);
            fuelCharcoal += 1;
            setChanged();
            return true;
        }
        return false;
    }

//----------------------------------------------------------------------
//          Работа с выходным слотом и списание ингредиентов
//----------------------------------------------------------------------

    // Проверяет, поместится ли в выходной слот новый предмет.
    private boolean canOutputAccept(ItemStack toInsert) {
        if (toInsert.isEmpty()) return false;
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, toInsert)) return false;
        return out.getCount() + toInsert.getCount() <= out.getMaxStackSize();
    }

    // Сливает новый предмет в выходной слот.
    private void mergeOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) items.set(SLOT_OUT, stack.copy());
        else if (ItemStack.isSameItemSameTags(out, stack)) out.grow(stack.getCount());
    }

    // Списывает ингредиенты по текущей вкладке (каждой ячейке -1).
    public void consumeInputsForCurrentTab() {
        switch (currentTypeIdx) {
            case TYPE_SHORT -> {
                items.get(SLOT_B).shrink(1);
                items.get(SLOT_E).shrink(1);
            }
            case TYPE_LONG  -> {
                items.get(SLOT_A).shrink(1);
                items.get(SLOT_B).shrink(1);
                items.get(SLOT_E).shrink(1);
            }
            case TYPE_TWO_HANDED -> {
                items.get(SLOT_A).shrink(1);
                items.get(SLOT_B).shrink(1);
                items.get(SLOT_C).shrink(1);
                items.get(SLOT_D).shrink(1);
                items.get(SLOT_E).shrink(1);
            }
            case TYPE_POLEARM -> {
                items.get(SLOT_B).shrink(1);
                items.get(SLOT_F).shrink(1);
                items.get(SLOT_E).shrink(1);
            }
        }
        // очищаем ячейки, где количество стало <=0
        for (int i : activeIndices()) {
            if (items.get(i).getCount() <= 0) items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

//----------------------------------------------------------------------------
//          Настройка времени и топлива в зависимости от вкладки
//----------------------------------------------------------------------------

    private void recalcFuelNeedAndTime() {
        fuelNeed = switch (currentTypeIdx) {
            case TYPE_SHORT      -> 2;
            case TYPE_POLEARM    -> 2;
            case TYPE_LONG       -> 3;
            case TYPE_TWO_HANDED -> 4;
            default              -> 1;
        };
        maxProgress = switch (currentTypeIdx) {
            case TYPE_SHORT      -> 100;
            case TYPE_POLEARM    -> 120;
            case TYPE_LONG       -> 160;
            case TYPE_TWO_HANDED -> 220;
            default              -> 100;
        };
    }

//------------------------------------------------------
//          Сохранение и загрузка состояния
//------------------------------------------------------

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

//------------------------------------------------------
//          Дроп содержимого при разрушении
//------------------------------------------------------

    // Выбрасывает все предметы из инвентаря.
    public void dropAllContents() {
        if (level instanceof ServerLevel sl) {
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                Containers.dropItemStack(sl,
                        worldPosition.getX()+0.5,
                        worldPosition.getY()+0.5,
                        worldPosition.getZ()+0.5,
                        s);
                }
            }
        }
    }

    // Выбрасывает весь буфер топлива.
    public void dropFuelBuffer() {
        if (!(level instanceof ServerLevel sl)) return;
        while (fuelCoal-- > 0) {
            Containers.dropItemStack(sl,
                    worldPosition.getX()+0.5,
                    worldPosition.getY()+0.5,
                    worldPosition.getZ()+0.5,
                    new ItemStack(Items.COAL));
        }
        fuelCoal = 0;
        while (fuelCharcoal-- > 0) {
            Containers.dropItemStack(sl,
                    worldPosition.getX()+0.5,
                    worldPosition.getY()+0.5,
                    worldPosition.getZ()+0.5,
                    new ItemStack(Items.CHARCOAL));
        }
        fuelCharcoal = 0;
    }

    // Сброс состояния при взятии предмета из OUT — вызывается из меню.
    public void resetAfterTake(Level lvl) {
        craftReady = false;
        progress = 0;
        if (!items.get(SLOT_OUT).isEmpty()) items.set(SLOT_OUT, ItemStack.EMPTY);
        setChanged();
    }
}
