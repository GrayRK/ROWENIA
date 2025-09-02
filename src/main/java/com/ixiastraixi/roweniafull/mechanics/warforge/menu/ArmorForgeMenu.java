package com.ixiastraixi.roweniafull.mechanics.warforge.menu;

import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.ArmorForgeBlockEntity;
import com.ixiastraixi.roweniafull.mechanics.warforge.recipe.ArmorForgingRecipe;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeMenus;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorForgeMenu extends AbstractContainerMenu {

    private static final int TAB_HELMET=0, TAB_CHEST=1, TAB_LEGS=2, TAB_BOOTS=3, TAB_SHIELD=4;

    public final ArmorForgeBlockEntity be;
    public final ContainerData data;

    // сетка ссылок на слоты по индексам 0..8 для каждой вкладки
    private final PositionedSlot[][] v = new PositionedSlot[9][5];

    // fuel / out / preview
    private PositionedSlot fuel;
    private ResultSlot out;
    private final SimpleContainer previewCont = new SimpleContainer(1);
    private PreviewSlot preview;
    private long lastPreviewFP = 0L;

    // индексы игрока для moveItemStackTo
    private int playerStartIdx, playerEndIdx;
    private int fuelMenuIdx;

    public ArmorForgeMenu(int id, Inventory inv, ArmorForgeBlockEntity be, ContainerData data) {
        super(WarforgeMenus.ARMOR_FORGE_MENU.get(), id);
        this.be = be;
        this.data = data;

        Container cont = be.container();

        // общие слоты
        fuel = addFuelSlot(cont, ArmorForgeBlockEntity.SLOT_FUEL, 218, 16); fuelMenuIdx = this.slots.size()-1;
        out  = addResultSlot(cont, ArmorForgeBlockEntity.SLOT_OUT, 114, 61, be);
        preview = addPreviewSlot(previewCont, 0, 189, 16);

        // --- входные слоты по вкладкам (каждый slot привязываем к tab) ---
        // HELMET: 0,1,2,3,5
        v[0][TAB_HELMET] = addInputSlot(cont, 0, 27, 39, TAB_HELMET);
        v[1][TAB_HELMET] = addInputSlot(cont, 1, 45, 39, TAB_HELMET);
        v[2][TAB_HELMET] = addInputSlot(cont, 2, 63, 39, TAB_HELMET);
        v[3][TAB_HELMET] = addInputSlot(cont, 3, 27, 57, TAB_HELMET);
        v[5][TAB_HELMET] = addInputSlot(cont, 5, 63, 57, TAB_HELMET);

        // CHEST: 0,2,3,4,5,6,7,8
        v[0][TAB_CHEST] = addInputSlot(cont, 0, 27, 39, TAB_CHEST);
        v[2][TAB_CHEST] = addInputSlot(cont, 2, 63, 39, TAB_CHEST);
        v[3][TAB_CHEST] = addInputSlot(cont, 3, 27, 57, TAB_CHEST);
        v[4][TAB_CHEST] = addInputSlot(cont, 4, 45, 57, TAB_CHEST);
        v[5][TAB_CHEST] = addInputSlot(cont, 5, 63, 57, TAB_CHEST);
        v[6][TAB_CHEST] = addInputSlot(cont, 6, 27, 75, TAB_CHEST);
        v[7][TAB_CHEST] = addInputSlot(cont, 7, 45, 75, TAB_CHEST);
        v[8][TAB_CHEST] = addInputSlot(cont, 8, 63, 75, TAB_CHEST);

        // LEGS: 0,1,2,3,5,6,8
        v[0][TAB_LEGS] = addInputSlot(cont, 0, 27, 39, TAB_LEGS);
        v[1][TAB_LEGS] = addInputSlot(cont, 1, 45, 39, TAB_LEGS);
        v[2][TAB_LEGS] = addInputSlot(cont, 2, 63, 39, TAB_LEGS);
        v[3][TAB_LEGS] = addInputSlot(cont, 3, 27, 57, TAB_LEGS);
        v[5][TAB_LEGS] = addInputSlot(cont, 5, 63, 57, TAB_LEGS);
        v[6][TAB_LEGS] = addInputSlot(cont, 6, 27, 75, TAB_LEGS);
        v[8][TAB_LEGS] = addInputSlot(cont, 8, 63, 75, TAB_LEGS);

        // BOOTS: 3,5,6,8
        v[3][TAB_BOOTS] = addInputSlot(cont, 3, 27, 57, TAB_BOOTS);
        v[5][TAB_BOOTS] = addInputSlot(cont, 5, 63, 57, TAB_BOOTS);
        v[6][TAB_BOOTS] = addInputSlot(cont, 6, 27, 75, TAB_BOOTS);
        v[8][TAB_BOOTS] = addInputSlot(cont, 8, 63, 75, TAB_BOOTS);

        // SHIELD: 0,1,2,3,4,5,7
        v[0][TAB_SHIELD] = addInputSlot(cont, 0, 27, 39, TAB_SHIELD);
        v[1][TAB_SHIELD] = addInputSlot(cont, 1, 45, 39, TAB_SHIELD);
        v[2][TAB_SHIELD] = addInputSlot(cont, 2, 63, 39, TAB_SHIELD);
        v[3][TAB_SHIELD] = addInputSlot(cont, 3, 27, 57, TAB_SHIELD);
        v[4][TAB_SHIELD] = addInputSlot(cont, 4, 45, 57, TAB_SHIELD);
        v[5][TAB_SHIELD] = addInputSlot(cont, 5, 63, 57, TAB_SHIELD);
        v[7][TAB_SHIELD] = addInputSlot(cont, 7, 45, 75, TAB_SHIELD);

        // инвентарь игрока
        playerStartIdx = this.slots.size();
        addPlayerInventory(inv, 45, 117);
        playerEndIdx = this.slots.size();

        addDataSlots(data);
        applyLayout(currentTabIdx());
    }

    private int currentTabIdx() { return data.get(4); }

    private void addPlayerInventory(Inventory inv, int x, int y) {
        for (int r=0;r<3;r++) for (int c=0;c<9;c++) addSlot(new Slot(inv, c + r*9 + 9, x + c*18, y + r*18));
        for (int c=0;c<9;c++) addSlot(new Slot(inv, c, x + c*18, y + 58));
    }

    private PositionedSlot addInputSlot(Container cont, int beIndex, int x, int y, int tab) {
        PositionedSlot s = new PositionedSlot(cont, beIndex, beIndex, x, y);
        s.tab = tab;
        addSlot(s); s.menuIndex = this.slots.size()-1;
        return s;
    }

    private class FuelSlot extends PositionedSlot {
        public FuelSlot(Container cont, int beIndex, int index, int x, int y) {
            super(cont, beIndex, index, x, y);
        }
        @Override public boolean isActive() { return true; } // ← ВСЕГДА видим
        @Override public boolean mayPlace(ItemStack st) {
            return st.is(Items.COAL) || st.is(Items.CHARCOAL);
        }
    }

    private PositionedSlot addFuelSlot(Container cont, int beIndex, int x, int y) {
        FuelSlot s = new FuelSlot(cont, beIndex, beIndex, x, y);
        addSlot(s);
        s.menuIndex = this.slots.size() - 1;
        return s;
    }

    private ResultSlot addResultSlot(Container cont, int beIndex, int x, int y, ArmorForgeBlockEntity be) {
        ResultSlot s = new ResultSlot(cont, beIndex, x, y, be);
        addSlot(s); s.menuIndex = this.slots.size()-1;
        return s;
    }

    private PreviewSlot addPreviewSlot(SimpleContainer cont, int index, int x, int y) {
        PreviewSlot s = new PreviewSlot(cont, index, x, y);
        addSlot(s); s.menuIndex = this.slots.size()-1;
        return s;
    }

    // --- сервер: обработка "кнопок" от экрана ---
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) return true;

        switch (id) {
            case 0 -> { returnInputsToPlayer(player); be.prevTab(); } // prev
            case 1 -> { returnInputsToPlayer(player); be.nextTab(); } // next
            case 2 -> be.setHoldingCraft(true);                       // hold start
            case 3 -> be.setHoldingCraft(false);                      // hold stop
            default -> {}
        }

        // мгновенно синкаем вкладку клиенту (и применяем раскладку)
        int t = be.currentTabIdx();
        data.set(4, t);
        applyLayout(t);
        broadcastChanges();
        return true;
    }

    // --- раскладка слотов: включаем только текущую вкладку ---
    private void applyLayout(int t) {
        if (fuel != null) fuel.setActive(true);
        if (out  != null) out.setActive(true);
        for (int i=0;i<v.length;i++)
            for (int tab=0; tab<5; tab++)
                if (v[i][tab]!=null)
                    v[i][tab].setActive(tab == t);
    }

    // --- превью (клиент): собираем снапшот из СЛОТОВ МЕНЮ, а не BE ---
    public void refreshLayoutIfNeeded() {
        int t = currentTabIdx();
        applyLayout(t);

        // если OUT занят — предосмотр не рисуем
        if (out != null && this.slots.get(out.menuIndex).hasItem()) {
            previewCont.setItem(0, ItemStack.EMPTY);
            lastPreviewFP = -1L;
            return;
        }

        var cl = net.minecraft.client.Minecraft.getInstance().level;
        if (cl == null) return;

        long fp = previewFingerprint();
        if (fp == lastPreviewFP) return;
        lastPreviewFP = fp;

        SimpleContainer snap = activeSnapshotFromMenu();
        ItemStack outStack = ItemStack.EMPTY;

        var rm = cl.getRecipeManager();
        for (var r : rm.getAllRecipesFor(WarforgeRecipes.ARMOR_FORGING_TYPE.get())) {
            if (r.form().ordinal() == t && r.matches(snap, cl)) {
                outStack = r.assemble(snap, cl.registryAccess());
                break;
            }
        }
        previewCont.setItem(0, outStack);
    }

    private SimpleContainer activeSnapshotFromMenu() {
        SimpleContainer snap = new SimpleContainer(9);
        int tab = currentTabIdx();
        int[] act = activeIndices(tab);
        for (int i=0;i<9;i++) snap.setItem(i, ItemStack.EMPTY);
        for (int idx: act) {
            PositionedSlot ps = v[idx][tab];
            ItemStack st = ItemStack.EMPTY;
            if (ps != null && ps.menuIndex >= 0 && ps.menuIndex < this.slots.size()) {
                st = this.slots.get(ps.menuIndex).getItem().copy();
            }
            snap.setItem(idx, st);
        }
        return snap;
    }

    private long previewFingerprint() {
        long h = 1469598103934665603L;
        int tab = currentTabIdx();
        int[] act = activeIndices(tab);
        for (int idx: act) {
            PositionedSlot ps = v[idx][tab];
            ItemStack s = (ps != null && ps.menuIndex >= 0 && ps.menuIndex < this.slots.size())
                    ? this.slots.get(ps.menuIndex).getItem()
                    : ItemStack.EMPTY;
            int id = s.isEmpty()? -1 : net.minecraft.world.item.Item.getId(s.getItem());
            int c  = s.isEmpty()? 0  : s.getCount();
            h ^= (id * 1099511628211L) ^ (c * 1469598103934665603L);
            h *= 1099511628211L;
        }
        h ^= tab * 0x9E3779B97F4A7C15L;
        return h;
    }

    private static int[] activeIndices(int tab) {
        return switch (tab) {
            case TAB_HELMET -> new int[]{0,1,2,3,5};
            case TAB_CHEST  -> new int[]{0,2,3,4,5,6,7,8};
            case TAB_LEGS   -> new int[]{0,1,2,3,5,6,8};
            case TAB_BOOTS  -> new int[]{3,5,6,8};
            case TAB_SHIELD -> new int[]{0,1,2,3,4,5,7};
            default -> new int[]{0};
        };
    }

    // --- жизненный цикл меню ---
    @Override public void removed(Player player) {
        super.removed(player);
        be.setHoldingCraft(false);
        returnInputsToPlayer(player);
    }

    @Override public boolean stillValid(Player player) {
        return be.container().stillValid(player);
    }

    // --- Shift-клики и возврат входов ---
    @Override
    public ItemStack quickMoveStack(Player player, int slotIdx) {
        Slot clicked = this.slots.get(slotIdx);
        if (clicked == null || !clicked.hasItem()) return ItemStack.EMPTY;

        if (clicked == out) {
            ItemStack stack = clicked.getItem();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack ret = stack.copy();
            if (!moveItemStackTo(stack, playerStartIdx, playerEndIdx, true)) return ItemStack.EMPTY;
            clicked.onTake(player, ret.copy());
            if (stack.isEmpty()) clicked.set(ItemStack.EMPTY); else clicked.setChanged();
            return ret;
        }

        ItemStack stack = clicked.getItem();
        ItemStack ret   = stack.copy();
        boolean fromPlayer = slotIdx >= playerStartIdx && slotIdx < playerEndIdx;

        if (fromPlayer) {
            if (isFuelItem(stack.getItem())) {
                if (!moveItemStackTo(stack, fuelMenuIdx, fuelMenuIdx+1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveToActiveInputs(stack)) return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, playerStartIdx, playerEndIdx, true)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) clicked.set(ItemStack.EMPTY); else clicked.setChanged();
        return ret;
    }

    private boolean isFuelItem(Item it) { return it == Items.COAL || it == Items.CHARCOAL; }

    private boolean moveToActiveInputs(ItemStack stack) {
        int t = currentTabIdx();
        for (int i=0;i<v.length;i++) if (v[i][t]!=null && v[i][t].isActive() && v[i][t].mayPlace(stack)) {
            if (moveItemStackTo(stack, v[i][t].menuIndex, v[i][t].menuIndex+1, false)) return true;
        }
        return false;
    }

    private void returnInputsToPlayer(Player player) {
        for (int i=0;i<v.length;i++) for (int t=0;t<5;t++) {
            PositionedSlot s = v[i][t]; if (s==null) continue;
            Slot slot = this.slots.get(s.menuIndex);
            if (slot.hasItem()) this.moveItemStackTo(slot.getItem(), playerStartIdx, playerEndIdx, false);
        }
    }

    // --- слоты ---

    /** НЕ static — чтобы видеть currentTabIdx() у внешнего меню */
    private class PositionedSlot extends Slot {
        private boolean active = true;
        int menuIndex = -1;
        final int beIndex;
        int tab = -1; // вкладка-владелец

        public PositionedSlot(Container cont, int beIndex, int index, int x, int y) {
            super(cont, index, x, y);
            this.beIndex = beIndex;
        }

        public PositionedSlot setActive(boolean v) { this.active = v; return this; }

        @Override public boolean isActive() {
            return active && (tab == ArmorForgeMenu.this.currentTabIdx());
        }

        @Override public boolean mayPlace(ItemStack s) {
            return isActive() && s.is(WarforgeTags.ARMOR_PLATE);
        }

        @Override public boolean mayPickup(Player p) {
            return isActive() && super.mayPickup(p);
        }
    }

    private static class ResultSlot extends Slot {
        private final ArmorForgeBlockEntity be;
        private boolean active = true;
        int menuIndex = -1;

        public ResultSlot(Container cont, int index, int x, int y, ArmorForgeBlockEntity be) {
            super(cont, index, x, y);
            this.be = be;
        }

        public ResultSlot setActive(boolean v) { this.active = v; return this; }
        @Override public boolean isActive() { return active; }

        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return active && super.mayPickup(p); }

        @Override public void onTake(Player p, ItemStack taken) {
            be.resetAfterTake(p.level());
            super.onTake(p, taken);
        }
    }

    private static class PreviewSlot extends Slot {
        int menuIndex = -1;
        public PreviewSlot(SimpleContainer cont, int index, int x, int y) { super(cont, index, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }
}
