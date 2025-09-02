package com.ixiastraixi.roweniafull.mechanics.warforge.menu;

import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.WeaponForgeBlockEntity;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.WeaponForgingRecipe;
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

public class WeaponForgeMenu extends AbstractContainerMenu {
    /*
     * === SLOT LAYOUT GUIDE ===
     * Machine slots (BE indices): A=0, B=1, C=2, D=3, E=4, FUEL=5, OUT=6, F=7.
     * Tabs/forms and active inputs:
     *   SHORT:    B(26,55), E(26,83)
     *   LONG:     A(26,37), B(26,55), E(26,83)
     *   TWO-HAND: A(26,37), B(26,55), C(8,55), D(44,55), E(26,83)
     *   POLEARM:  B(26,55), F(26,65), E(26,83)
     * Preview slot (read-only) sits at (154,26).
     * Fuel slot at (183,37). OUT at (114,61) (real item appears only after craft ready).
     *
     * Change coords here if you redraw the texture — logic uses these positions.
     */

    private static final int TAB_SHORT=0, TAB_LONG=1, TAB_TWO=2, TAB_POLEARM=3;
    private static final int IDX_A=0, IDX_B=1, IDX_C=2, IDX_D=3, IDX_E=4, IDX_FUEL=5, IDX_OUT=6, IDX_F=7;
    private static boolean isHandleSlotIndex(int beIdx) {
        return beIdx == IDX_E || beIdx == IDX_F;
    }
    private static boolean isBladeSlotIndex(int beIdx) {
        return beIdx == IDX_A || beIdx == IDX_B || beIdx == IDX_C || beIdx == IDX_D;
    }

    private final WeaponForgeBlockEntity be;
    private final ContainerData data;

    private final PositionedSlot[][] v = new PositionedSlot[8][4];
    private PositionedSlot fuel;
    private ResultSlot out;

    // ---- preview-slot stuff ----
    private final SimpleContainer previewCont = new SimpleContainer(1); // только отображение
    private PreviewSlot preview;
    private int previewMenuIdx = -1;
    private long lastPreviewFP = Long.MIN_VALUE;

    private int fuelMenuIdx = -1;
    private int playerStartIdx, playerEndIdx;
    private int cachedTypeIdx = -1;

    public WeaponForgeMenu(int id, Inventory inv, WeaponForgeBlockEntity be, ContainerData data) {
        super(WarforgeMenus.WEAPON_FORGE_MENU.get(), id);
        this.be = be;
        this.data = data;

        addDataSlots(data);

        Container cont = be.container();

        // FUEL / OUT фикс
        fuel = addPosSlot(cont, IDX_FUEL, 183, 37);
        fuelMenuIdx = this.slots.size() - 1;
        out  = addResultSlot(cont, IDX_OUT, 114, 61, be);

        // === PREVIEW SLOT === (x=154, y=26)
        preview = addPreviewSlot(previewCont, 0, 154, 26);

        // SHORT: B,E
        v[IDX_B][TAB_SHORT] = addPosSlot(cont, IDX_B, 26, 55);
        v[IDX_E][TAB_SHORT] = addPosSlot(cont, IDX_E, 26, 83);

        // LONG: A,B,E
        v[IDX_A][TAB_LONG] = addPosSlot(cont, IDX_A, 26, 37);
        v[IDX_B][TAB_LONG] = addPosSlot(cont, IDX_B, 26, 55);
        v[IDX_E][TAB_LONG] = addPosSlot(cont, IDX_E, 26, 83);

        // TWO-HANDED: A,B,C,D,E
        v[IDX_A][TAB_TWO] = addPosSlot(cont, IDX_A, 26, 37);
        v[IDX_B][TAB_TWO] = addPosSlot(cont, IDX_B, 26, 55);
        v[IDX_C][TAB_TWO] = addPosSlot(cont, IDX_C,  8, 55);
        v[IDX_D][TAB_TWO] = addPosSlot(cont, IDX_D, 44, 55);
        v[IDX_E][TAB_TWO] = addPosSlot(cont, IDX_E, 26, 83);

        // POLEARM: B,F,E
        v[IDX_B][TAB_POLEARM] = addPosSlot(cont, IDX_B, 26, 37);
        v[IDX_F][TAB_POLEARM] = addPosSlot(cont, IDX_F, 26, 65);
        v[IDX_E][TAB_POLEARM] = addPosSlot(cont, IDX_E, 26, 83);

        // инвентарь игрока
        playerStartIdx = this.slots.size();
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                addSlot(new Slot(inv, x + y * 9 + 9, 8 + x * 18, 124 + y * 18));
        for (int x = 0; x < 9; x++)
            addSlot(new Slot(inv, x, 8 + x * 18, 124 + 58));
        playerEndIdx = this.slots.size();

        applyLayout(currentTypeIdx());
        // стартовый рассчёт превью (на сервере подхватится в broadcastChanges)
        if (!inv.player.level().isClientSide) recalcPreviewServer();
    }

    // клиентский конструктор
    public WeaponForgeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (WeaponForgeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5)); // 0..4
    }

    @Override public boolean stillValid(Player p) { return true; }

    // ===== доступ для экрана (если нужно) =====
    public Container machineContainer() { return be.container(); }
    public int outMenuIndex() { return (out != null ? out.menuIndex : -1); }
    public int previewMenuIndex() { return previewMenuIdx; }

    public int progress()       { return data.get(0); }
    public int maxProgress()    { return data.get(1); }
    public int fuelStored()     { return data.get(2); } // 0..4
    public int fuelNeed()       { return data.get(3); }
    public int currentTypeIdx() { return data.get(4); }

    // ====== серверная синхра превью ======
    @Override
    public void broadcastChanges() {
        // сначала пересчитаем превью (чтобы оно успело засинкаться),
        // потом вызовем стандартную рассылку
        recalcPreviewServer();
        super.broadcastChanges();
    }

    private void recalcPreviewServer() {
        if (be.getLevel() == null || be.getLevel().isClientSide) return;

        // ⬇⬇⬇ Новая короткая проверка: если OUT не пуст — превью не показываем
        if (!be.container().getItem(IDX_OUT).isEmpty()) {
            if (!previewCont.getItem(0).isEmpty()) {
                previewCont.setItem(0, ItemStack.EMPTY); // мгновенно скрыть у клиента
            }
            // Сбросим хеш, чтобы после опустошения OUT превью точно пересчиталось
            lastPreviewFP = Long.MIN_VALUE;
            return;
        }
        // ⬆⬆⬆ конец новой проверки

        long fp = previewFingerprint();
        if (fp == lastPreviewFP) return;
        lastPreviewFP = fp;

        // считаем превью только по активным слотам текущей вкладки
        SimpleContainer snap = activeSnapshot(be.container(), currentTypeIdx());
        ItemStack out = ItemStack.EMPTY;

        for (WeaponForgingRecipe r : ((ServerLevel) be.getLevel())
                .getRecipeManager().getAllRecipesFor(WarforgeRecipes.WEAPON_FORGING_TYPE.get())) {
            if (r.form().ordinal() == currentTypeIdx() && r.matches(snap, be.getLevel())) {
                ItemStack res = r.assemble(snap, be.getLevel().registryAccess());
                if (!res.isEmpty()) { out = res; break; }
            }
        }

        previewCont.setItem(0, out); // обычная синхра слота
    }

    private long previewFingerprint() {
        long h = 1469598103934665603L; // FNV-ish
        int[] act = activeIndices(currentTypeIdx());
        Container cont = be.container();
        for (int idx : act) {
            ItemStack s = cont.getItem(idx);
            int id = s.isEmpty() ? -1 : Item.getId(s.getItem());
            int c  = s.isEmpty() ? 0  : s.getCount();
            h ^= (id * 1315423911L + c);
            h *= 1099511628211L;
        }
        // включаем вкладку в хеш, чтобы переключение сразу меняло превью
        h ^= (currentTypeIdx() * 16777619L);
        return h;
    }

    private SimpleContainer activeSnapshot(Container cont, int tab) {
        SimpleContainer snap = new SimpleContainer(8);
        int[] act = activeIndices(tab);
        // заполняем только активные; FUEL/OUT/прочие — пусто
        for (int i = 0; i < 8; i++) {
            boolean use = false;
            for (int a : act) if (a == i) { use = true; break; }
            if (use) snap.setItem(i, cont.getItem(i).copy());
            else     snap.setItem(i, ItemStack.EMPTY);
        }
        return snap;
    }

    private int[] activeIndices(int tab) {
        return switch (tab) {
            case TAB_SHORT   -> new int[]{ IDX_B, IDX_E };
            case TAB_LONG    -> new int[]{ IDX_A, IDX_B, IDX_E };
            case TAB_TWO     -> new int[]{ IDX_A, IDX_B, IDX_C, IDX_D, IDX_E };
            case TAB_POLEARM -> new int[]{ IDX_B, IDX_F, IDX_E };
            default -> new int[]{ IDX_B, IDX_E };
        };
    }

    // ===== кнопки/закрытие =====
    @Override public void removed(Player player) {
        super.removed(player);
        be.setHoldingCraft(false);
        returnInputsToPlayer(player);
    }

    // 0=prev,1=next,2=hold-press,3=hold-release
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
        applyLayout(currentTypeIdx());
        return true;
    }

    // ===== Shift+ПКМ =====
    @Override
    public ItemStack quickMoveStack(Player player, int slotIdx) {
        Slot clicked = this.slots.get(slotIdx);
        if (clicked == null || !clicked.hasItem()) return ItemStack.EMPTY;

        // --- спец-случай: Shift-клик из OUT ---
        if (clicked == out) {
            if (!be.isCraftReady()) return ItemStack.EMPTY; // замок до готовности

            ItemStack stack = clicked.getItem();
            ItemStack ret   = stack.copy();

            // пробуем переложить в инвентарь игрока
            if (!moveItemStackTo(stack, playerStartIdx, playerEndIdx, true)) {
                return ItemStack.EMPTY; // нет места — ничего не трогаем
            }

            // ВАЖНО: сообщаем слоту, что предмет взят => спишутся ингредиенты и сбросится craftReady
            clicked.onTake(player, ret.copy());

            // привести визуал в порядок
            if (stack.isEmpty()) clicked.set(ItemStack.EMPTY);
            else clicked.setChanged();

            return ret;
        }

        // --- обычные случаи (как было) ---
        ItemStack stack = clicked.getItem();
        ItemStack ret   = stack.copy();

        boolean fromPlayer = slotIdx >= playerStartIdx && slotIdx < playerEndIdx;
        if (fromPlayer) {
            if (isFuelItem(stack.getItem())) {
                if (!moveItemStackTo(stack, fuelMenuIdx, fuelMenuIdx + 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveToActiveInputs(stack)) return ItemStack.EMPTY;
            }
        } else { // из машины (не OUT) в игрока
            if (!moveItemStackTo(stack, playerStartIdx, playerEndIdx, true)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) clicked.set(ItemStack.EMPTY);
        else clicked.setChanged();
        return ret;
    }

    private boolean isFuelItem(Item it) {
        return it == Items.COAL || it == Items.CHARCOAL || it == Items.COAL_BLOCK;
    }

    private boolean moveToActiveInputs(ItemStack stack) {
        int t = currentTypeIdx();
        int[] order = switch (t) {
            case TAB_SHORT   -> new int[]{ IDX_B, IDX_E };
            case TAB_LONG    -> new int[]{ IDX_A, IDX_B, IDX_E };
            case TAB_TWO     -> new int[]{ IDX_A, IDX_B, IDX_C, IDX_D, IDX_E };
            case TAB_POLEARM -> new int[]{ IDX_B, IDX_F, IDX_E };
            default -> new int[]{ IDX_B, IDX_E };
        };
        for (int beIdx : order) {
            PositionedSlot s = v[beIdx][t];
            if (s != null && s.isActive()) {
                int mi = s.menuIndex;
                if (moveItemStackTo(stack, mi, mi + 1, false) && stack.isEmpty()) return true;
            }
        }
        return stack.isEmpty();
    }

    private void returnInputsToPlayer(Player player) {
        int t = currentTypeIdx();
        int[] allInputs = new int[]{ IDX_A, IDX_B, IDX_C, IDX_D, IDX_E, IDX_F };
        for (int beIdx : allInputs) {
            PositionedSlot s = v[beIdx][t];
            if (s == null) continue;
            ItemStack st = s.getItem();
            if (st.isEmpty()) continue;

            ItemStack copy = st.copy();
            if (moveItemStackTo(copy, playerStartIdx, playerEndIdx, true)) {
                s.set(ItemStack.EMPTY); s.setChanged();
            } else {
                player.drop(st.copy(), false);
                s.set(ItemStack.EMPTY); s.setChanged();
            }
        }
    }

    // ===== раскладки =====
    public void refreshLayoutIfNeeded() {
        int t = currentTypeIdx();
        if (t != cachedTypeIdx) applyLayout(t);
    }

    private void applyLayout(int t) {
        cachedTypeIdx = t;
        if (fuel != null) fuel.setActive(true);
        if (out  != null) out.setActive(true); // у ResultSlot есть setActive

        // выключаем все входные
        for (int i = 0; i < v.length; i++)
            for (int tab = 0; tab < 4; tab++)
                if (v[i][tab] != null) v[i][tab].setActive(false);

        // включаем активные для текущей вкладки
        for (int i = 0; i < v.length; i++)
            if (v[i][t] != null) v[i][t].setActive(true);
    }

    // ===== создание слотов =====
    private PositionedSlot addPosSlot(Container cont, int beIndex, int x, int y) {
        PositionedSlot s = new PositionedSlot(cont, beIndex, beIndex, x, y);
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }
    private PositionedSlot addPosSlot(Container cont, int beIndex, int x, int y, boolean mayPlace) {
        PositionedSlot s = new PositionedSlot(cont, beIndex, beIndex, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return mayPlace && super.mayPlace(stack); }
        };
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }

    private ResultSlot addResultSlot(Container cont, int beIndex, int x, int y, WeaponForgeBlockEntity be) {
        ResultSlot s = new ResultSlot(cont, beIndex, x, y, be);
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }

    private PreviewSlot addPreviewSlot(Container cont, int index, int x, int y) {
        PreviewSlot s = new PreviewSlot(cont, index, x, y);
        addSlot(s);
        previewMenuIdx = this.slots.size() - 1;
        return s;
    }

    // ==== слоты ====
    private static class PositionedSlot extends Slot {
        private boolean active = true;
        private int menuIndex = -1;
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

            if (isHandleSlotIndex(beIndex)) {
                return stack.is(WarforgeTags.HANDLES);
            }
            if (isBladeSlotIndex(beIndex)) {
                return stack.is(WarforgeTags.BLADE_BLANKS);
            }

            return super.mayPlace(stack);
        }

        @Override public boolean mayPickup(Player p) { return active && super.mayPickup(p); }
    }

    /** Слот результата: нельзя класть, забирать можно только когда be.isCraftReady()==true */
    private static class ResultSlot extends Slot {
        private final WeaponForgeBlockEntity be;
        private boolean active = true;
        int menuIndex = -1;

        public ResultSlot setActive(boolean v) { this.active = v; return this; }
        @Override public boolean isActive() { return active; }

        public ResultSlot(Container cont, int index, int x, int y, WeaponForgeBlockEntity be) {
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

    /** Слот предпросмотра: только показываем, класть/забирать нельзя */
    private static class PreviewSlot extends Slot {
        public PreviewSlot(Container cont, int index, int x, int y) { super(cont, index, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }
}