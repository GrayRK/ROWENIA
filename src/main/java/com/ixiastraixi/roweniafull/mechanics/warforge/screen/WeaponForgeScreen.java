package com.ixiastraixi.roweniafull.mechanics.warforge.screen;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ixiastraixi.roweniafull.mechanics.warforge.menu.WeaponForgeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.Objects;

public class WeaponForgeScreen extends AbstractContainerScreen<WeaponForgeMenu> {

//----------------------
//       Текстуры
//----------------------

    private static ResourceLocation rl(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse(MOD_ID + ":" + path));
    }

    // Фоны по вкладкам
    private static final ResourceLocation TEX_SHORT      = rl("textures/gui/weapon_forge_short.png");
    private static final ResourceLocation TEX_LONG       = rl("textures/gui/weapon_forge_long.png");
    private static final ResourceLocation TEX_TWO_HANDED = rl("textures/gui/weapon_forge_two_handed.png");
    private static final ResourceLocation TEX_POLEARM    = rl("textures/gui/weapon_forge_polearm.png");

    // Виджеты
    private static final ResourceLocation ARROW_L       = rl("textures/gui/widgets/arrow_left.png");
    private static final ResourceLocation ARROW_L_HOVER = rl("textures/gui/widgets/arrow_left_hover.png");
    private static final ResourceLocation ARROW_R       = rl("textures/gui/widgets/arrow_right.png");
    private static final ResourceLocation ARROW_R_HOVER = rl("textures/gui/widgets/arrow_right_hover.png");
    private static final ResourceLocation CRAFT_BTN     = rl("textures/gui/widgets/craft.png");
    private static final ResourceLocation CRAFT_BTN_H   = rl("textures/gui/widgets/craft_hover.png");

    // Индикатор топлива
    private static final ResourceLocation FUEL_IND      = rl("textures/gui/widgets/fuel_indicator.png");

    // Наковальня анимация
    private static final ResourceLocation FORGE_ANIM    = rl("textures/gui/widgets/forge_animation.png");

//--------------------------------------------------------------------
//       Параметры объектов: Текстур, Анимаций, Виджетов
//--------------------------------------------------------------------

    // Размеры текстуры и видимой части
    private static final int TEX_W   =206; // Ширина отображаемой текстуры GUI
    private static final int TEX_H   =206; // Высота отображаемой текстуры GUI
    private static final int ATLAS_W =206; // Ширина текстуры GUI целиком
    private static final int ATLAS_H =218; // Высота текстуры GUI целиком

    // Прогрессбар
    private static final int PB_W       =80;  // Ширина PB
    private static final int PB_H       =6;   // Высота PB
    private static final int PB_EMPTY_U =0;   // На какой X нарисован пустой PB в атласе GUI
    private static final int PB_EMPTY_V =206; // На какой Y нарисован пустой PB в атласе GUI
    private static final int PB_FULL_U  =0;   // На какой X нарисован заполненный PB в атласе GUI
    private static final int PB_FULL_V  =212; // На какой Y нарисован заполненный PB в атласе GUI
    private static final int PB_X       =80;  // На какую X устанавливаем PB в GUI
    private static final int PB_Y       =89;  // На какую Y устанавливаем PB в GUI

    // Размеры и координаты стрелок/кнопки крафта
        //Кнопки переключения вкладок
    private static final int AR_W    =17;  // Ширина кнопки переключения вкладок
    private static final int AR_H    =10;  // Высота кнопки переключения вкладок
    private static final int AR_L_X  =23;  // На какую X устанавливаем левую кнопку в GUI
    private static final int AR_R_X  =136; // На какую X устанавливаем правую кнопку в GUI
    private static final int AR_Y    =5;   // На какой Y устанавливаем обе кнопки в GUI
        //Кнопка крафта
    private static final int CRAFT_W =16;  // Ширина кнопки крафта
    private static final int CRAFT_H =9;   // Высота кнопки крафта
    private static final int CRAFT_X =112; // На какую X устанавливаем кнопку крафта в GUI
    private static final int CRAFT_Y =101; // На какую Y устанавливаем кнопку крафта в GUI

    // Индикатор топлива
    private static final int FUEL_TEX_W   =120; // Общая ширина текстуры индикатора
    private static final int FUEL_TEX_H   =47;  // Общая высота текстуры индикатора
    private static final int FUEL_SLICE_H =46;  // Шаг по ширине для смены отображаемой части текстуры
    private static final int FUEL_X       =179; // На какую X устанавливаем индикатор в GUI
    private static final int FUEL_Y       =67;  // На какую Y устанавливаем индикатор в GUI

    // Наковальня анимация
    private static final int FORGE_X =88; // На какую X устанавливаем наковальню в GUI
    private static final int FORGE_Y =28; // На какую Y устанавливаем наковальню в GUI
    private static final int FORGE_W =62; // Ширина кадра на текстуре для анимации
    private static final int FORGE_H =57; // Высота кадра на текстуре для анимации
    private static final int HAMMER_SPEED_TICKS = 6; // Скорость анимации в тиках
    private static final int FORGE_U_A = 62;  // Начало кадра по X для фазы анимации A
    private static final int FORGE_U_B = 124; // Начало кадра по X для фазы анимации B
    private int hammerStartPhase = 0; // Стартовая фаза анимации (0 — начать с A, 1 — начать с B)
    private long pressStartTick = 0L; // Тик начала удержания (для фазы)

    // Состояние анимации и звука
    private int  lastProgress = 0; // Текущий прогресс крафта
    private long lastHitSoundTick = -1000L;
    private boolean holdingCraft = false; // Кнопка крафта удерживается

//--------------------------------------------
//       Привязка основных текстур
//--------------------------------------------

    // Задаём размеры в соответствии с текстурой
    public WeaponForgeScreen(WeaponForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;
    }

    // Выбирает фон для текущей вкладки
    private ResourceLocation currentBg() {
        return switch (menu.currentTypeIdx()) {
            case 0 -> TEX_SHORT;
            case 1 -> TEX_LONG;
            case 2 -> TEX_TWO_HANDED;
            default -> TEX_POLEARM;
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
        if (this.minecraft != null)
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
        g.blit(tex, leftPos + PB_X, topPos + PB_Y, PB_EMPTY_U, PB_EMPTY_V, PB_W, PB_H, ATLAS_W, ATLAS_H);
        int max = Math.max(1, menu.maxProgress());
        int prog = menu.progress();
        int filled = Math.min(PB_W, Math.round((prog / (float) max) * PB_W));
            // Заполненная часть
        if (filled > 0)
            g.blit(tex, leftPos + PB_X, topPos + PB_Y, PB_FULL_U, PB_FULL_V, filled, PB_H, ATLAS_W, ATLAS_H);

        // Индикатор топлива
        int buf = Math.max(0, Math.min(4, menu.fuelStored()));
        int u, w; switch (buf) {
            case 0 -> { u = 0;  w = 24; }
            case 1 -> { u = 24; w = 24; }
            case 2 -> { u = 48; w = 24; }
            case 3 -> { u = 72; w = 24; }
            default -> { u = 96; w = 23; }
        }
        g.blit(FUEL_IND, leftPos + FUEL_X, topPos + FUEL_Y, u, 0, w, FUEL_SLICE_H, FUEL_TEX_W, FUEL_TEX_H);

        // Наковальня
        int forgeU;
        boolean animActive = holdingCraft && prog > 0;
        if (animActive) {
            long gt = (this.minecraft != null && this.minecraft.level != null)
                    ? this.minecraft.level.getGameTime() : 0L;
            long ticksSincePress = Math.max(0L, gt - pressStartTick);
            int frameIndex = (int)(((ticksSincePress / HAMMER_SPEED_TICKS) +
                    hammerStartPhase) & 1);
            forgeU = (frameIndex == 0) ? FORGE_U_A : FORGE_U_B;
            boolean hitFrame = (frameIndex == 1);
            if (prog > lastProgress && hitFrame && gt - lastHitSoundTick >= HAMMER_SPEED_TICKS) {
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.9F));
                lastHitSoundTick = gt;
            }
        } else {
            forgeU = 0;
        }

        lastProgress = prog;
        g.blit(FORGE_ANIM, leftPos + FORGE_X, topPos + FORGE_Y, forgeU, 0, FORGE_W, FORGE_H, 186, 57);

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
        // Левая стрелка: кнопка 0
        if (isHover(x, y, AR_L_X, AR_Y, AR_W, AR_H)) { clickButton(0);
            playClick();
            return true;
        }
        // Правая стрелка: кнопка 1
        if (isHover(x, y, AR_R_X, AR_Y, AR_W, AR_H)) { clickButton(1);
            playClick();
            return true;
        }
        // Кнопка крафта: кнопка 2
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