package com.ixiastraixi.roweniafull.mechanics.warforge.screen;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ixiastraixi.roweniafull.mechanics.warforge.menu.ArmorForgeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.Objects;

public class ArmorForgeScreen extends AbstractContainerScreen<ArmorForgeMenu> {

//----------------------
//       Текстуры
//----------------------

    private static ResourceLocation rl(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse(MOD_ID + ":" + path));
    }

    // Фоны по вкладкам
    private static final ResourceLocation TEX_HELMET  = rl("textures/gui/armor_forge_helmet.png");
    private static final ResourceLocation TEX_CHEST   = rl("textures/gui/armor_forge_chestplate.png");
    private static final ResourceLocation TEX_LEGS    = rl("textures/gui/armor_forge_leggings.png");
    private static final ResourceLocation TEX_BOOTS   = rl("textures/gui/armor_forge_boots.png");
    private static final ResourceLocation TEX_SHIELD  = rl("textures/gui/armor_forge_shield.png");

    // Виджеты
    private static final ResourceLocation ARROW_L       = rl("textures/gui/widgets/arrow_left.png");
    private static final ResourceLocation ARROW_L_HOVER = rl("textures/gui/widgets/arrow_left_hover.png");
    private static final ResourceLocation ARROW_R       = rl("textures/gui/widgets/arrow_right.png");
    private static final ResourceLocation ARROW_R_HOVER = rl("textures/gui/widgets/arrow_right_hover.png");
    private static final ResourceLocation CRAFT_BTN     = rl("textures/gui/widgets/craft.png");
    private static final ResourceLocation CRAFT_BTN_H   = rl("textures/gui/widgets/craft_hover.png");

    // Индикатор топлива
    private static final ResourceLocation FUEL_IND   = rl("textures/gui/widgets/fuel_indicator.png");

    // Наковальня анимация
    private static final ResourceLocation FORGE_ANIM = rl("textures/gui/widgets/forge_animation.png");

//--------------------------------------------------------------------
//       Параметры объектов: Текстур, Анимаций, Виджетов
//--------------------------------------------------------------------

    // Размеры текстуры и видимой части
    private static final int TEX_W  = 270; // Ширина отображаемой текстуры GUI
    private static final int TEX_H  = 247; // Высота отображаемой текстуры GUI
    private static final int ATLAS_W = 270; // Ширина текстуры GUI целиком
    private static final int ATLAS_H = 270; // Высота текстуры GUI целиком

    // Прогрессбар
    private static final int PB_W       = 164;  // Ширина PB
    private static final int PB_H       = 11;   // Высота PB
    private static final int PB_U_EMPTY = 50;   // На какой X нарисован пустой PB в атласе GUI
    private static final int PB_V_EMPTY = 259; // На какой Y нарисован пустой PB в атласе GUI
    private static final int PB_U_FULL  = 50;   // На какой X нарисован заполненный PB в атласе GUI
    private static final int PB_V_FULL  = 248; // На какой Y нарисован заполненный PB в атласе GUI
    private static final int PB_X       = 56; // На какую X устанавливаем PB в GUI
    private static final int PB_Y       = 151;  // На какую Y устанавливаем PB в GUI

    // Размеры и координаты стрелок/кнопки крафта
        //Кнопки переключения вкладок
    private static final int AR_W    =22;  // Ширина кнопки переключения вкладок
    private static final int AR_H    =14;  // Высота кнопки переключения вкладок
    private static final int AR_L_X  =54;  // На какую X устанавливаем левую кнопку в GUI
    private static final int AR_R_X  =78; // На какую X устанавливаем правую кнопку в GUI
    private static final int AR_Y    =129;   // На какой Y устанавливаем обе кнопки в GUI
        //Кнопка крафт
    private static final int CRAFT_W =114;  // Ширина кнопки крафта
    private static final int CRAFT_H =14;   // Высота кнопки крафта
    private static final int CRAFT_X =108; // На какую X устанавливаем кнопку крафта в GUI
    private static final int CRAFT_Y =129; // На какую Y устанавливаем кнопку крафта в GUI

    // Индикатор топлива
    private static final int FUEL_TEX_W   =170; // Общая ширина текстуры индикатора
    private static final int FUEL_TEX_H   =60;  // Общая высота текстуры индикатора
    private static final int FUEL_SLICE_W =34;  // Шаг по ширине для смены отображаемой части текстуры
    private static final int FUEL_X       =231; // На какую X устанавливаем индикатор в GUI
    private static final int FUEL_Y       =156;  // На какую Y устанавливаем индикатор в GUI

    // Наковальня анимация
    private static final int FORGE_X =110; // На какую X устанавливаем наковальню в GUI
    private static final int FORGE_Y =25;  // На какую Y устанавливаем наковальню в GUI
    private static final int FORGE_SLICE_W =100; // Ширина кадра на текстуре для анимации
    private static final int FORGE_SLICE_H =100; // Высота кадра на текстуре для анимации
    private static final int FORGE_TEX_W =300;  // Общая ширина текстуры наковальни
    private static final int FORGE_TEX_H =100;   // Общая высота текстуры наковальни
    private static final int FORGE_U_A = 100;  // Начало кадра по X для фазы анимации A
    private static final int FORGE_U_B = 200; // Начало кадра по X для фазы анимаяции B
    private static final int HAMMER_SPEED_TICKS = 6; // Скорость анимации в тиках
    private int  hammerStartPhase   = 0;  // Стартовая фаза анимации (0 — начать с A, 1 — начать с B)
    private long pressStartTick     = 0L; // Тик начала удержания (для фазы)

    // Состояние анимации и звука
    private int  lastProgress       = 0; // Текущий прогресс крафта
    private long lastHitSoundTick   = -1000L;
    private boolean holdingCraft    = false; // Кнопка крафта удерживается

//--------------------------------------------
//       Привязка основных текстур
//--------------------------------------------

    // Задаём размеры в соответствии с текстурой
    public ArmorForgeScreen(ArmorForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = TEX_W + 50;
        this.imageHeight = TEX_H + 50;
    }

    // Выбирает фон для текущей вкладки
    private ResourceLocation currentBg() {
        int tab = this.menu.currentTypeIdx();
        return switch (tab) {
            case 1 -> TEX_CHEST;
            case 2 -> TEX_LEGS;
            case 3 -> TEX_BOOTS;
            case 4 -> TEX_SHIELD;
            default -> TEX_HELMET;
        };
    }

//------------------------------
//       Хелперы кнопок
//------------------------------

    // Обработка нажатий
    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    // Воспроизведение звука кнопки
    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    // Наведение мышкой на кнопки
    private boolean isHover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

//------------------------------
//       Основная логика
//------------------------------

    // Просим меню обновить раскладку, если вкладка изменилась
    @Override
    protected void containerTick() {
        super.containerTick();
        this.menu.refreshLayoutIfNeeded();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // На экране нет текстовых заголовков, оставляем пусто
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        // Фон
        ResourceLocation tex = currentBg();
        g.blit(tex, leftPos, topPos, 0, 0, TEX_W, TEX_H, ATLAS_W, ATLAS_H);

        // Прогрессбар
            // Пустая часть
        g.blit(tex, leftPos + PB_X, topPos + PB_Y, PB_U_EMPTY, PB_V_EMPTY, PB_W, PB_H, ATLAS_W, ATLAS_H);
        int max  = Math.max(1, menu.maxProgress());
        int prog = menu.progress();
        int filled = (int) Math.round((double) PB_W * prog / max);
            // Заполненная часть
        if (filled > 0) {
            g.blit(tex, leftPos + PB_X, topPos + PB_Y, PB_U_FULL, PB_V_FULL, filled, PB_H, ATLAS_W, ATLAS_H);
        }

        // Индикатор топлива
        int buf = Math.max(0, Math.min(4, menu.fuelStored()));
        int u, w;
        switch (buf) {
            case 0 -> { u = FUEL_SLICE_W * 0;  w = FUEL_SLICE_W; }
            case 1 -> { u = FUEL_SLICE_W * 1; w = FUEL_SLICE_W; }
            case 2 -> { u = FUEL_SLICE_W * 2; w = FUEL_SLICE_W; }
            case 3 -> { u = FUEL_SLICE_W * 3; w = FUEL_SLICE_W; }
            default -> { u = FUEL_SLICE_W * 4; w = FUEL_SLICE_W; }
        }
        g.blit(FUEL_IND, leftPos + FUEL_X, topPos + FUEL_Y, u, 0, w, FUEL_TEX_H, FUEL_TEX_W, FUEL_TEX_H);

        // Наковальня
        int forgeU;
        boolean animActive = holdingCraft && prog > 0;
        if (animActive) {
            long gt = (this.minecraft != null && this.minecraft.level != null)
                    ? this.minecraft.level.getGameTime() : 0L;
            long ticksSincePress = Math.max(0L, gt - pressStartTick);
            int frameIndex = (int) (((ticksSincePress / HAMMER_SPEED_TICKS) +
                    hammerStartPhase) & 1);
            forgeU = (frameIndex == 0 ? FORGE_U_A : FORGE_U_B);
            boolean hitFrame = (frameIndex == 1);
            if (prog > lastProgress && hitFrame && gt - lastHitSoundTick >= HAMMER_SPEED_TICKS) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.9F));
                lastHitSoundTick = gt;
            }
        } else {
            forgeU = 0;
        }
        lastProgress = prog;
        g.blit(FORGE_ANIM, leftPos + FORGE_X, topPos + FORGE_Y, forgeU, 0, FORGE_SLICE_W, FORGE_SLICE_H, FORGE_TEX_W, FORGE_TEX_H);

        // Кнопки
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos;
        boolean hoverL = isHover(relX, relY, AR_L_X, AR_Y, AR_W, AR_H);
        boolean hoverR = isHover(relX, relY, AR_R_X, AR_Y, AR_W, AR_H);
        boolean hoverC = isHover(relX, relY, CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H);
        g.blit(hoverL ? ARROW_L_HOVER : ARROW_L, leftPos + AR_L_X, topPos + AR_Y, 0, 0, AR_W, AR_H, AR_W, AR_H);
        g.blit(hoverR ? ARROW_R_HOVER : ARROW_R, leftPos + AR_R_X, topPos + AR_Y, 0, 0, AR_W, AR_H, AR_W, AR_H);
        g.blit(hoverC ? CRAFT_BTN_H : CRAFT_BTN, leftPos + CRAFT_X, topPos + CRAFT_Y, 0, 0, CRAFT_W, CRAFT_H, CRAFT_W, CRAFT_H);
    }

    // Клики
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double x = mx - leftPos;
        double y = my - topPos;
        // левая стрелка: кнопка 0
        if (isHover(x, y, AR_L_X, AR_Y, AR_W, AR_H)) {
            clickButton(0);
            playClick();
            return true;
        }
        // правая стрелка: кнопка 1
        if (isHover(x, y, AR_R_X, AR_Y, AR_W, AR_H)) {
            clickButton(1);
            playClick();
            return true;
        }
        // кнопка крафта: кнопка 2
        if (isHover(x, y, CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H)) {
            hammerStartPhase = 0;
            pressStartTick = (minecraft != null && minecraft.level != null)
                    ? minecraft.level.getGameTime() : 0L;
            holdingCraft = true;
            clickButton(2);
            playClick();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (holdingCraft) {
            holdingCraft = false;
            clickButton(3);
            lastProgress = 0;
            lastHitSoundTick = -1000L;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        if (holdingCraft) {
            holdingCraft = false;
            clickButton(3);
        }
        super.onClose();
    }
}
