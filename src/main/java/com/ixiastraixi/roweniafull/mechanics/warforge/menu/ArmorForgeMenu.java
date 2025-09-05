package com.ixiastraixi.roweniafull.mechanics.warforge.menu;

import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.ArmorForgeBlockEntity;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.ArmorForgingRecipe;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeMenus;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorForgeMenu extends AbstractContainerMenu {

//-------------------------------------------------------------------
//       Константы: Слоты, Типы, Состояния
//-------------------------------------------------------------------

    // --- Вкладки ---
    private static final int TAB_HELMET = ArmorForgeBlockEntity.TYPE_HELMET;
    private static final int TAB_CHEST  = ArmorForgeBlockEntity.TYPE_CHEST;
    private static final int TAB_LEGS   = ArmorForgeBlockEntity.TYPE_LEGS;
    private static final int TAB_BOOTS  = ArmorForgeBlockEntity.TYPE_BOOTS;
    private static final int TAB_SHIELD = ArmorForgeBlockEntity.TYPE_SHIELD;

    // --- Индексы слотов ---
    private static final int IDX_A    = ArmorForgeBlockEntity.SLOT_A;
    private static final int IDX_B    = ArmorForgeBlockEntity.SLOT_B;
    private static final int IDX_C    = ArmorForgeBlockEntity.SLOT_C;
    private static final int IDX_D    = ArmorForgeBlockEntity.SLOT_D;
    private static final int IDX_E    = ArmorForgeBlockEntity.SLOT_E;
    private static final int IDX_F    = ArmorForgeBlockEntity.SLOT_F;
    private static final int IDX_G    = ArmorForgeBlockEntity.SLOT_G;
    private static final int IDX_H    = ArmorForgeBlockEntity.SLOT_H;
    private static final int IDX_I    = ArmorForgeBlockEntity.SLOT_I;
    private static final int IDX_FUEL = ArmorForgeBlockEntity.SLOT_FUEL;
    private static final int IDX_OUT  = ArmorForgeBlockEntity.SLOT_OUT;

    // --- Инвентарь игрока на текстуре ---
    private static final int PLAYER_INV_X = 45;
    private static final int PLAYER_INV_Y = 117;

    private final ArmorForgeBlockEntity be;
    private final ContainerData data;

    // Сетка слотов по индексам и вкладкам. v[beIndex][tab] — ссылка на
    // PositionedSlot для данного входного beIndex и вкладки (helmet..shield).
    private final PositionedSlot[][] v = new PositionedSlot[9][5];
    private PositionedSlot fuel;
    private ResultSlot     out;

    // Предпросмотр: отдельный контейнер, показывающий призрачный результат.
    private final SimpleContainer previewCont = new SimpleContainer(1);
    private PreviewSlot preview;
    private int previewMenuIdx = -1;
    private long lastPreviewFP = Long.MIN_VALUE;

    // Номера слотов для быстрого перемещения
    private int fuelMenuIdx    = -1;
    private int playerStartIdx =  0;
    private int playerEndIdx   =  0;
    private int cachedTypeIdx  = -1;


    //Основной конструктор, вызываемый на сервере.
    public ArmorForgeMenu(int id, Inventory inv, ArmorForgeBlockEntity be, ContainerData data) {
        super(WarforgeMenus.ARMOR_FORGE_MENU.get(), id);
        this.be   = be;
        this.data = data;

        // привязываем dataSlots, чтобы клиент видел прогресс/топливо/тип
        addDataSlots(data);
        Container cont = be.container();

//-----------------------------------
//       Размещение слотов
//-----------------------------------

        // --- Фиксированные слоты ---
        fuel = addFuelSlot(cont, IDX_FUEL, 218, 16);
        fuelMenuIdx = this.slots.size() - 1;
        out  = addResultSlot(cont, IDX_OUT, 114, 61, be);
        preview = addPreviewSlot(previewCont, 0, 189, 16);

        // --- входные ячейки по вкладкам ---
        // HELMET:
        v[IDX_A][TAB_HELMET] = addInputSlot(cont, IDX_A, 27, 39);
        v[IDX_B][TAB_HELMET] = addInputSlot(cont, IDX_B, 45, 39);
        v[IDX_C][TAB_HELMET] = addInputSlot(cont, IDX_C, 63, 39);
        v[IDX_D][TAB_HELMET] = addInputSlot(cont, IDX_D, 27, 57);
        v[IDX_F][TAB_HELMET] = addInputSlot(cont, IDX_F, 63, 57);

        // CHEST:
        v[IDX_A][TAB_CHEST] = addInputSlot(cont, IDX_A, 27, 39);
        v[IDX_C][TAB_CHEST] = addInputSlot(cont, IDX_C, 63, 39);
        v[IDX_D][TAB_CHEST] = addInputSlot(cont, IDX_D, 27, 57);
        v[IDX_E][TAB_CHEST] = addInputSlot(cont, IDX_E, 45, 57);
        v[IDX_F][TAB_CHEST] = addInputSlot(cont, IDX_F, 63, 57);
        v[IDX_G][TAB_CHEST] = addInputSlot(cont, IDX_G, 27, 75);
        v[IDX_H][TAB_CHEST] = addInputSlot(cont, IDX_H, 45, 75);
        v[IDX_I][TAB_CHEST] = addInputSlot(cont, IDX_I, 63, 75);

        // LEGS:
        v[IDX_A][TAB_LEGS] = addInputSlot(cont, IDX_A, 27, 39);
        v[IDX_B][TAB_LEGS] = addInputSlot(cont, IDX_B, 45, 39);
        v[IDX_C][TAB_LEGS] = addInputSlot(cont, IDX_C, 63, 39);
        v[IDX_D][TAB_LEGS] = addInputSlot(cont, IDX_D, 27, 57);
        v[IDX_F][TAB_LEGS] = addInputSlot(cont, IDX_F, 63, 57);
        v[IDX_G][TAB_LEGS] = addInputSlot(cont, IDX_G, 27, 75);
        v[IDX_I][TAB_LEGS] = addInputSlot(cont, IDX_I, 63, 75);

        // BOOTS:
        v[IDX_D][TAB_BOOTS] = addInputSlot(cont, IDX_D, 27, 57);
        v[IDX_F][TAB_BOOTS] = addInputSlot(cont, IDX_F, 63, 57);
        v[IDX_G][TAB_BOOTS] = addInputSlot(cont, IDX_G, 27, 75);
        v[IDX_I][TAB_BOOTS] = addInputSlot(cont, IDX_I, 63, 75);

        // SHIELD:
        v[IDX_A][TAB_SHIELD] = addInputSlot(cont, IDX_A, 27, 39);
        v[IDX_B][TAB_SHIELD] = addInputSlot(cont, IDX_B, 45, 39);
        v[IDX_C][TAB_SHIELD] = addInputSlot(cont, IDX_C, 63, 39);
        v[IDX_D][TAB_SHIELD] = addInputSlot(cont, IDX_D, 27, 57);
        v[IDX_E][TAB_SHIELD] = addInputSlot(cont, IDX_E, 45, 57);
        v[IDX_F][TAB_SHIELD] = addInputSlot(cont, IDX_F, 63, 57);
        v[IDX_H][TAB_SHIELD] = addInputSlot(cont, IDX_H, 45, 75);

        // Инвентарь игрока
        playerStartIdx = this.slots.size();
        // Инвентарь
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv,
                        col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        // Хотбар
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv,
                    col,
                    PLAYER_INV_X + col * 18,
                    PLAYER_INV_Y + 58));
        }
        playerEndIdx = this.slots.size();

        // Применяем раскладку для начальной вкладки
        applyLayout(currentTypeIdx());

        // Первичный расчёт предпросмотра на сервере: на клиенте синхра через broadcastChanges
        if (!inv.player.level().isClientSide) recalcPreviewServer();
    }

    // Клиентский конструктор
    public ArmorForgeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (ArmorForgeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }

    @Override
    public boolean stillValid(Player player) {
        return be.container().stillValid(player);
    }

    // === Методы для экрана ===
    public Container machineContainer() { return be.container(); }
    public int outMenuIndex() { return (out != null ? out.menuIndex : -1); }
    public int previewMenuIndex() { return previewMenuIdx; }

    public int progress()       { return data.get(0); }
    public int maxProgress()    { return data.get(1); }
    public int fuelStored()     { return data.get(2); } // 0..4
    public int fuelNeed()       { return data.get(3); }
    public int currentTypeIdx() { return data.get(4); }


     // Серверный пересчёт предпросмотра и рассылка изменений. Вызывается каждый тик,
     // когда клиент запрашивает broadcastChanges(), а также в нашем override.
    @Override
    public void broadcastChanges() {
        recalcPreviewServer();
        super.broadcastChanges();
    }

    private void recalcPreviewServer() {
        if (be.getLevel() == null || be.getLevel().isClientSide) return;
        if (!be.container().getItem(IDX_OUT).isEmpty()) {
            if (!previewCont.getItem(0).isEmpty()) {
                previewCont.setItem(0, ItemStack.EMPTY);
            }
            lastPreviewFP = Long.MIN_VALUE;
            return;
        }
        long fp = previewFingerprint();
        if (fp == lastPreviewFP) return;
        lastPreviewFP = fp;

        // Рассчитываем результат по активным слотам текущей вкладки
        SimpleContainer snap = activeSnapshot(currentTypeIdx());
        ItemStack outStack = ItemStack.EMPTY;
        for (ArmorForgingRecipe r : ((ServerLevel) be.getLevel())
                .getRecipeManager().getAllRecipesFor(WarforgeRecipes.ARMOR_FORGING_TYPE.get())) {
            if (r.form().ordinal() == currentTypeIdx() && r.matches(snap, be.getLevel())) {
                ItemStack res = r.assemble(snap, be.getLevel().registryAccess());
                if (!res.isEmpty()) { outStack = res; break; }
            }
        }
        previewCont.setItem(0, outStack);
    }

    // Вычисляет хэш предпросмотра для сравнения с предыдущим состоянием.
    // Включает предметы и количество в активных ячейках и текущую вкладку.
    private long previewFingerprint() {
        long h = 1469598103934665603L;
        int t = currentTypeIdx();
        int[] act = activeIndices(t);
        for (int idx : act) {
            PositionedSlot ps = v[idx][t];
            ItemStack s = (ps != null && ps.menuIndex >= 0 && ps.menuIndex < this.slots.size())
                    ? this.slots.get(ps.menuIndex).getItem()
                    : ItemStack.EMPTY;
            int id = s.isEmpty() ? -1 : Item.getId(s.getItem());
            int c  = s.isEmpty() ? 0  : s.getCount();
            h ^= (id * 1099511628211L) ^ (c * 1469598103934665603L);
            h *= 1099511628211L;
        }
        h ^= (long) t * 0x9E3779B97F4A7C15L;
        return h;
    }

    // Создаёт снимок активных ячеек. Возвращает контейнер размером 11 (как у BE),
    // где заполнены только активные позиции текущей вкладки. Остальные пусты.
    private SimpleContainer activeSnapshot(int tab) {
        SimpleContainer snap = new SimpleContainer(11);
        int[] act = activeIndices(tab);
        for (int idx : act) {
            PositionedSlot ps = v[idx][tab];
            ItemStack st = ItemStack.EMPTY;
            if (ps != null && ps.menuIndex >= 0 && ps.menuIndex < this.slots.size()) {
                st = this.slots.get(ps.menuIndex).getItem().copy();
            }
            snap.setItem(idx, st);
        }
        return snap;
    }

    // Возвращает список активных индексов для заданной вкладки. Эти же массивы
    // используются в ArmorForgeBlockEntity.consumeInputsForCurrentTab().
    private static int[] activeIndices(int tab) {
        return switch (tab) {
            case TAB_HELMET -> new int[]{ IDX_A, IDX_B, IDX_C, IDX_D, IDX_F };
            case TAB_CHEST  -> new int[]{ IDX_A, IDX_C, IDX_D, IDX_E, IDX_F, IDX_G, IDX_H, IDX_I };
            case TAB_LEGS   -> new int[]{ IDX_A, IDX_B, IDX_C, IDX_D, IDX_F, IDX_G, IDX_I };
            case TAB_BOOTS  -> new int[]{ IDX_D, IDX_F, IDX_G, IDX_I };
            case TAB_SHIELD -> new int[]{ IDX_A, IDX_B, IDX_C, IDX_D, IDX_E, IDX_F, IDX_H };
            default -> new int[]{ IDX_A };
        };
    }

//-------------------------------------------------------
//       Обработка кнопок, закрытие, Shift‑клики
//-------------------------------------------------------

    // ===== кнопки/закрытие =====
    @Override
    public void removed(Player player) {
        super.removed(player);
        be.setHoldingCraft(false);
        returnInputsToPlayer(player);
    }

     //id: 0=prev, 1=next, 2=hold‑start, 3=hold‑stop
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) return true;
        switch (id) {
            case 0 -> { returnInputsToPlayer(player); be.prevType(); }
            case 1 -> { returnInputsToPlayer(player); be.nextType(); }
            case 2 -> be.setHoldingCraft(true);
            case 3 -> be.setHoldingCraft(false);
            default -> {}
        }
        // синхронизируем вкладку и применяем раскладку
        applyLayout(currentTypeIdx());
        return true;
    }

    // ===== Shift+ПКМ =====
    @Override
    public ItemStack quickMoveStack(Player player, int slotIdx) {
        Slot clicked = this.slots.get(slotIdx);
        if (clicked == null || !clicked.hasItem()) return ItemStack.EMPTY;

        // --- Shift‑клик из OUT ---
        if (clicked == out) {
            // Выдаём результат только если крафт готов
            if (!be.isCraftReady()) return ItemStack.EMPTY;
            ItemStack stack = clicked.getItem();
            ItemStack ret   = stack.copy();
            // Перемещаем в инвентарь игрока
            if (!moveItemStackTo(stack, playerStartIdx, playerEndIdx, true)) {
                return ItemStack.EMPTY;
            }
            // сообщаем слоту, что предмет взят (спишутся ингредиенты, сбросится craftReady)
            clicked.onTake(player, ret.copy());
            // обновляем слот OUT
            if (stack.isEmpty()) clicked.set(ItemStack.EMPTY);
            else clicked.setChanged();
            return ret;
        }

        // --- Shift‑клик ---
        ItemStack stack = clicked.getItem();
        ItemStack ret   = stack.copy();
        boolean fromPlayer = slotIdx >= playerStartIdx && slotIdx < playerEndIdx;
        if (fromPlayer) {
            // Из инвентаря игрока в блок
            if (isFuelItem(stack.getItem())) {
                if (!moveItemStackTo(stack, fuelMenuIdx, fuelMenuIdx + 1, false))
                    return ItemStack.EMPTY;
            } else {
                if (!moveToActiveInputs(stack)) return ItemStack.EMPTY;
            }
        } else {
            // Из блока (кроме OUT) в инвентарь игрока
            if (!moveItemStackTo(stack, playerStartIdx, playerEndIdx, true))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) clicked.set(ItemStack.EMPTY);
        else clicked.setChanged();
        return ret;
    }

    // Проверяет, подходит ли предмет в качестве топлива.
    private boolean isFuelItem(Item it) {
        return it == Items.COAL || it == Items.CHARCOAL;
    }

    // Перекладывает предмет из инвентаря игрока в первую подходящую активную ячейку
    // текущей вкладки. Возвращает true, если удалось (или предмет опустошён).
    private boolean moveToActiveInputs(ItemStack stack) {
        int t = currentTypeIdx();
        int[] order = activeIndices(t);
        for (int beIdx : order) {
            PositionedSlot s = v[beIdx][t];
            if (s != null && s.isActive()) {
                int mi = s.menuIndex;
                if (moveItemStackTo(stack, mi, mi + 1, false) && stack.isEmpty())
                    return true;
            }
        }
        return stack.isEmpty();
    }

    // Возвращает все входы игроку при переключении вкладки. Если не помещается в
    // инвентарь — бросает предмет на землю.
    private void returnInputsToPlayer(Player player) {
        int t = currentTypeIdx();
        // перебираем все возможные входные индексы (0..8)
        for (int beIdx = 0; beIdx < 9; beIdx++) {
            PositionedSlot s = v[beIdx][t];
            if (s == null) continue;
            ItemStack st = s.getItem();
            if (st.isEmpty()) continue;
            ItemStack copy = st.copy();
            // пытаемся переложить в инвентарь игрока
            if (moveItemStackTo(copy, playerStartIdx, playerEndIdx, true)) {
                s.set(ItemStack.EMPTY);
                s.setChanged();
            } else {
                // нет места — дропаем на землю
                player.drop(st.copy(), false);
                s.set(ItemStack.EMPTY);
                s.setChanged();
            }
        }
    }

    // Применяет раскладку: включает слоты текущей вкладки и выключает остальные.
    // Также всегда оставляет видимыми fuel и out.
    public void refreshLayoutIfNeeded() {
        int t = currentTypeIdx();
        if (t != cachedTypeIdx) applyLayout(t);
    }

    private void applyLayout(int t) {
        cachedTypeIdx = t;
        // fuel и out всегда видимы
        if (fuel != null) fuel.setActive(true);
        if (out  != null) out.setActive(true);
        // выключаем все входные
        for (int i = 0; i < v.length; i++) {
            for (int tab = 0; tab < v[i].length; tab++) {
                if (v[i][tab] != null) v[i][tab].setActive(false);
            }
        }
        // включаем активные для текущей вкладки
        for (int i = 0; i < v.length; i++) {
            if (v[i][t] != null) v[i][t].setActive(true);
        }
    }

    // Добавляет ячейку входа. beIndex — индекс ячейки в блок‑сущности, x/y — координаты.
    private PositionedSlot addInputSlot(Container cont, int beIndex, int x, int y) {
        PositionedSlot s = new PositionedSlot(cont, beIndex, beIndex, x, y);
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }

    // Добавляет ячейку топлива. Разрешает класть только уголь/древесный уголь.
    private PositionedSlot addFuelSlot(Container cont, int beIndex, int x, int y) {
        FuelSlot s = new FuelSlot(cont, beIndex, beIndex, x, y);
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }

    // Добавляет ячейку выхода. Доступ к ней ограничивается готовностью крафта.
    private ResultSlot addResultSlot(Container cont, int beIndex, int x, int y, ArmorForgeBlockEntity be) {
        ResultSlot s = new ResultSlot(cont, beIndex, x, y, be);
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }

    // Добавляет ячейку предпросмотра (только для отображения, нельзя класть/забирать).
    private PreviewSlot addPreviewSlot(SimpleContainer cont, int index, int x, int y) {
        PreviewSlot s = new PreviewSlot(cont, index, x, y);
        addSlot(s);
        previewMenuIdx = this.slots.size() - 1;
        return s;
    }


    // ==== слоты ====
    private static class PositionedSlot extends Slot {
        private boolean active = true;
        int menuIndex = -1;
        private final int beIndex;

        public PositionedSlot(Container cont, int beIndex, int index, int x, int y) {
            super(cont, index, x, y);
            this.beIndex = beIndex;
        }
        public PositionedSlot setActive(boolean v) { this.active = v; return this; }
        @Override public boolean isActive() { return active; }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!active) return false;
            return stack.is(WarforgeTags.ARMOR_PLATE);
        }
        @Override public boolean mayPickup(Player p) { return active && super.mayPickup(p); }
    }

    //Слот топлива. Активен всегда, но ограничивает кладку углём.
    private static class FuelSlot extends PositionedSlot {
        public FuelSlot(Container cont, int beIndex, int index, int x, int y) {
            super(cont, beIndex, index, x, y);
        }
        @Override public boolean isActive() { return true; }
        @Override public boolean mayPlace(ItemStack st) {
            return st.is(Items.COAL) || st.is(Items.CHARCOAL);
        }
    }

    //Выход
    private static class ResultSlot extends Slot {
        private final ArmorForgeBlockEntity be;
        private boolean active = true;
        int menuIndex = -1;

        public ResultSlot setActive(boolean v) { this.active = v; return this; }
        @Override public boolean isActive() { return active; }

        public ResultSlot(Container cont, int index, int x, int y, ArmorForgeBlockEntity be) {
            super(cont, index, x, y);
            this.be = be;
        }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return active && be.isCraftReady() && super.mayPickup(p); }

        @Override
        public void onTake(Player p, ItemStack taken) {
            be.resetAfterTake(p.level());
            super.onTake(p, taken);
        }
    }

    //Слот предпросмотра
    private static class PreviewSlot extends Slot {
        public PreviewSlot(Container cont, int index, int x, int y) { super(cont, index, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }
}
