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

    private static ResourceLocation rl(String path) { return Objects.requireNonNull(ResourceLocation.tryParse(MOD_ID + ":" + path)); }

    // фоны по вкладкам
    private static final ResourceLocation TEX_SHORT      = rl("textures/gui/weapon_forge_short.png");
    private static final ResourceLocation TEX_LONG       = rl("textures/gui/weapon_forge_long.png");
    private static final ResourceLocation TEX_TWO_HANDED = rl("textures/gui/weapon_forge_two_handed.png");
    private static final ResourceLocation TEX_POLEARM    = rl("textures/gui/weapon_forge_polearm.png");

    // виджеты
    private static final ResourceLocation ARROW_L       = rl("textures/gui/widgets/arrow_left.png");
    private static final ResourceLocation ARROW_L_HOVER = rl("textures/gui/widgets/arrow_left_hover.png");
    private static final ResourceLocation ARROW_R       = rl("textures/gui/widgets/arrow_right.png");
    private static final ResourceLocation ARROW_R_HOVER = rl("textures/gui/widgets/arrow_right_hover.png");
    private static final ResourceLocation CRAFT_BTN     = rl("textures/gui/widgets/craft.png");
    private static final ResourceLocation CRAFT_BTN_H   = rl("textures/gui/widgets/craft_hover.png");

    // индикатор топлива (120x47, срезы высотой 46)
    private static final ResourceLocation FUEL_IND      = rl("textures/gui/widgets/fuel_indicator.png");

    // анимация кузни (атлас 186x57; кадры 62x56)
    private static final ResourceLocation FORGE_ANIM    = rl("textures/gui/widgets/forge_animation.png");

    // размеры
    private static final int TEX_W=206, TEX_H=206, ATLAS_W=206, ATLAS_H=218;

    // прогрессбар
    private static final int PB_EMPTY_U=0, PB_EMPTY_V=206, PB_FULL_U=0, PB_FULL_V=212, PB_W=80, PB_H=6;
    private static final int PB_X=80, PB_Y=89;

    // стрелки/крафт
    private static final int AR_W=17, AR_H=10, AR_L_X=23, AR_R_X=136, AR_Y=5;
    private static final int CRAFT_W=16, CRAFT_H=9, CRAFT_X=112, CRAFT_Y=101;

    // индикатор топлива
    private static final int FUEL_X=179, FUEL_Y=67, FUEL_TEX_W=120, FUEL_TEX_H=47, FUEL_SLICE_H=46;

    // анимация кузни
    private static final int FORGE_X=88, FORGE_Y=28, FORGE_W=62, FORGE_H=56;
    // скорость переключения кадров (в тиках)
    private static final int HAMMER_SPEED_TICKS = 6;
    // U-координаты двух кадров молота на атласе
    private static final int FORGE_U_A = 62;   // первый кадр
    private static final int FORGE_U_B = 124;  // второй кадр
    // стартовая фаза анимации (0 — начать с A, 1 — начать с B)
    private int hammerStartPhase = 0;
    // тик начала удержания (для фазы)
    private long pressStartTick = 0L;

    // звук/антифлад
    private int  lastProgress = 0;
    private long lastHitSoundTick = -1000L;
    private boolean holdingCraft = false;

    //хелпер нажатия кнопок
    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    public WeaponForgeScreen(WeaponForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;
    }

    private ResourceLocation currentBg() {
        return switch (menu.currentTypeIdx()) {
            case 0 -> TEX_SHORT;
            case 1 -> TEX_LONG;
            case 2 -> TEX_TWO_HANDED;
            default -> TEX_POLEARM;
        };
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { /* без надписей */ }

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
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        // фон
        g.blit(currentBg(), leftPos, topPos, 0, 0, TEX_W, TEX_H, ATLAS_W, ATLAS_H);

        // прогресс
        g.blit(currentBg(), leftPos + PB_X, topPos + PB_Y, PB_EMPTY_U, PB_EMPTY_V, PB_W, PB_H, ATLAS_W, ATLAS_H);
        int max = Math.max(1, menu.maxProgress());
        int prog = menu.progress();
        int filled = Math.min(PB_W, Math.round((prog / (float) max) * PB_W));
        if (filled > 0)
            g.blit(currentBg(), leftPos + PB_X, topPos + PB_Y, PB_FULL_U, PB_FULL_V, filled, PB_H, ATLAS_W, ATLAS_H);

        // индикатор топлива
        int buf = Math.max(0, Math.min(4, menu.fuelStored()));
        int u, w; switch (buf) {
            case 0 -> { u = 0;  w = 24; }
            case 1 -> { u = 24; w = 24; }
            case 2 -> { u = 48; w = 24; }
            case 3 -> { u = 72; w = 24; }
            default -> { u = 96; w = 23; }
        }
        g.blit(FUEL_IND, leftPos + FUEL_X, topPos + FUEL_Y, u, 0, w, FUEL_SLICE_H, FUEL_TEX_W, FUEL_TEX_H);

        // === анимация молота и звук ===
        int forgeU = 0;

        boolean animActive = holdingCraft && prog > 0;
        if (animActive) {
            long gt = (this.minecraft != null && this.minecraft.level != null)
                    ? this.minecraft.level.getGameTime() : 0L;

            // Считаем индекс кадра с учётом момента нажатия и стартовой фазы
            long ticksSincePress = Math.max(0L, gt - pressStartTick);
            int frameIndex = (int)(((ticksSincePress / HAMMER_SPEED_TICKS) + hammerStartPhase) & 1);

            // 0 -> кадр A, 1 -> кадр B
            forgeU = (frameIndex == 0) ? FORGE_U_A : FORGE_U_B;

            // если звук удара должен быть на кадре B — вот так:
            boolean hitFrame = (frameIndex == 1);
            if (prog > lastProgress && hitFrame && gt - lastHitSoundTick >= HAMMER_SPEED_TICKS) {
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.9F));
                lastHitSoundTick = gt;
            }
        } else {
            forgeU = 0; // стоп-кадр, если у тебя есть «пустой» U — можешь оставить 0
        }

        lastProgress = prog;

// рисуем молот
        g.blit(FORGE_ANIM, leftPos + FORGE_X, topPos + FORGE_Y, forgeU, 0, FORGE_W, FORGE_H, 186, 57);

        // кнопки
        boolean hoverL = isHover(mouseX - leftPos, mouseY - topPos, AR_L_X, AR_Y, AR_W, AR_H);
        boolean hoverR = isHover(mouseX - leftPos, mouseY - topPos, AR_R_X, AR_Y, AR_W, AR_H);
        boolean hoverC = isHover(mouseX - leftPos, mouseY - topPos, CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H);
        g.blit(hoverL ? ARROW_L_HOVER : ARROW_L, leftPos + AR_L_X, topPos + AR_Y, 0, 0, AR_W, AR_H, AR_W, AR_H);
        g.blit(hoverR ? ARROW_R_HOVER : ARROW_R, leftPos + AR_R_X, topPos + AR_Y, 0, 0, AR_W, AR_H, AR_W, AR_H);
        g.blit(hoverC ? CRAFT_BTN_H : CRAFT_BTN, leftPos + CRAFT_X, topPos + CRAFT_Y, 0, 0, CRAFT_W, CRAFT_H, CRAFT_W, CRAFT_H);
    }

    // клики
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double x = mx - leftPos, y = my - topPos;
        if (isHover(x, y, AR_L_X, AR_Y, AR_W, AR_H)) { clickButton(0); playClick(); return true; }
        if (isHover(x, y, AR_R_X, AR_Y, AR_W, AR_H)) { clickButton(1); playClick(); return true; }
        if (isHover(x, y, CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H)) {
            hammerStartPhase = 0;
            pressStartTick = (minecraft != null && minecraft.level != null) ? minecraft.level.getGameTime() : 0L;

            holdingCraft = true;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2); // press
            playClick();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (holdingCraft) {
            holdingCraft = false;
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3); // release
            }
            // мягкий сброс анимации/звука
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
            if (this.minecraft != null && this.minecraft.gameMode != null)
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
        }
        super.onClose();
    }

    private void playClick() {
        if (this.minecraft != null)
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean isHover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @SuppressWarnings("unused")
    private void debugDrawAlpha(GuiGraphics g) { // на всякий, вдруг пригодится
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 0f, 0f, 0.25f);
        g.fill(leftPos, topPos, leftPos + TEX_W, topPos + TEX_H, 0x40FF0000);
        RenderSystem.setShaderColor(1f,1f,1f,1f);
        RenderSystem.disableBlend();
    }
}