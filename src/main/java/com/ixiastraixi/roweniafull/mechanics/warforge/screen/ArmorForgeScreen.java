package com.ixiastraixi.roweniafull.mechanics.warforge.screen;

import static com.ixiastraixi.roweniafull.RoweniaFull.MOD_ID;

import com.ixiastraixi.roweniafull.mechanics.warforge.menu.ArmorForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.Objects;

public class ArmorForgeScreen extends AbstractContainerScreen<ArmorForgeMenu> {

    private static ResourceLocation rl(String path) { return Objects.requireNonNull(ResourceLocation.tryParse(MOD_ID + ":" + path)); }

    // фоновые текстуры вкладок
    private static final ResourceLocation TEX_HELMET  = rl("textures/gui/armor_forge_helmet.png");
    private static final ResourceLocation TEX_CHEST   = rl("textures/gui/armor_forge_chestplate.png");
    private static final ResourceLocation TEX_LEGS    = rl("textures/gui/armor_forge_leggings.png");
    private static final ResourceLocation TEX_BOOTS   = rl("textures/gui/armor_forge_boots.png");
    private static final ResourceLocation TEX_SHIELD  = rl("textures/gui/armor_forge_shield.png");

    // виджеты (как в WeaponForge)
    private static final ResourceLocation ARROW_L       = rl("textures/gui/widgets/arrow_left.png");
    private static final ResourceLocation ARROW_L_HOVER = rl("textures/gui/widgets/arrow_left_hover.png");
    private static final ResourceLocation ARROW_R       = rl("textures/gui/widgets/arrow_right.png");
    private static final ResourceLocation ARROW_R_HOVER = rl("textures/gui/widgets/arrow_right_hover.png");
    private static final ResourceLocation CRAFT_BTN     = rl("textures/gui/widgets/craft.png");
    private static final ResourceLocation CRAFT_BTN_H   = rl("textures/gui/widgets/craft_hover.png");

    // индикатор топлива (120x47; режем полоски 0..4)
    private static final ResourceLocation FUEL_IND      = rl("textures/gui/widgets/fuel_indicator.png");

    // анимация молота: спрайт 186x57; кадры U=62 и U=124 размером 62x56; U=0 — idle
    private static final ResourceLocation FORGE_ANIM    = rl("textures/gui/widgets/forge_animation.png");

    private static final int TEX_W=241, TEX_H=211, VIEW_H=199;

    public ArmorForgeScreen(ArmorForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = TEX_W;
        this.imageHeight = VIEW_H;
    }

    // позиции (горячие)
    private static final int PB_X=119, PB_Y=85, PB_W=80, PB_H=6;
    private static final int PB_U_EMPTY=0, PB_V_EMPTY=199, PB_U_FULL=0, PB_V_FULL=205;

    private static final int AR_W=17, AR_H=10, AR_L_X=106, AR_R_X=194, AR_Y=0;
    private static final int CRAFT_W=16, CRAFT_H=9, CRAFT_X=152, CRAFT_Y=94;

    private static final int FUEL_X=214, FUEL_Y=61, FUEL_TEX_W=120, FUEL_TEX_H=47, FUEL_SLICE_H=46;

    private static final int FORGE_X=126, FORGE_Y=24, FORGE_W=62, FORGE_H=56;
    private static final int HAMMER_SPEED_TICKS = 6;
    private static final int FORGE_U_A = 62, FORGE_U_B = 124;

    // локальное состояние для анимации/нажатия
    private boolean holdingCraft = false;
    private long pressStartTick = 0L;
    private int lastProgress = 0;
    private long lastHitSoundTick = -1000L;
    private int hammerStartPhase = 0;

    private ResourceLocation tabTex() {
        int tab = this.menu.data.get(4);
        return switch (tab) {
            case 1 -> TEX_CHEST;
            case 2 -> TEX_LEGS;
            case 3 -> TEX_BOOTS;
            case 4 -> TEX_SHIELD;
            default -> TEX_HELMET;
        };
    }

    // helper отправки «кнопок» в меню (server)
    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
    private void playClick() {
        if (this.minecraft != null)
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
    private boolean isHover(double mx, double my, int x, int y, int w, int h) { return mx>=x && mx<x+w && my>=y && my<y+h; }

    @Override protected void containerTick() {
        super.containerTick();
        this.menu.refreshLayoutIfNeeded();
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ResourceLocation tex = tabTex();
        g.blit(tex, leftPos, topPos, 0, 0, TEX_W, VIEW_H, TEX_W, TEX_H);

        // прогресс
        int prog = this.menu.data.get(0);
        int max  = Math.max(1, this.menu.data.get(1));
        int w = (int)Math.round((double)PB_W * prog / max);
        g.blit(tex, leftPos + PB_X, topPos + PB_Y, PB_U_EMPTY, PB_V_EMPTY, PB_W, PB_H, TEX_W, TEX_H);
        if (w > 0) g.blit(tex, leftPos + PB_X, topPos + PB_Y, PB_U_FULL, PB_V_FULL, w, PB_H, TEX_W, TEX_H);

        // индикатор топлива
        int fuel = this.menu.data.get(2);
        int u=0, wseg=23;
        switch (Math.max(0, Math.min(4, fuel))) {
            case 0 -> { u = 0;  wseg = 23; }
            case 1 -> { u = 24; wseg = 24; }
            case 2 -> { u = 48; wseg = 24; }
            case 3 -> { u = 72; wseg = 24; }
            default -> { u = 97; wseg = 23; }
        }
        g.blit(FUEL_IND, leftPos + FUEL_X, topPos + FUEL_Y, u, 0, wseg, FUEL_SLICE_H, FUEL_TEX_W, FUEL_TEX_H);

        // анимация молота
        int forgeU = 0;
        boolean animActive = holdingCraft && prog > 0;
        if (animActive) {
            long gt = (this.minecraft != null && this.minecraft.level != null) ? this.minecraft.level.getGameTime() : 0L;
            long ticksSincePress = Math.max(0L, gt - pressStartTick);
            int frameIndex = (int)(((ticksSincePress / HAMMER_SPEED_TICKS) + hammerStartPhase) & 1);
            forgeU = (frameIndex == 0) ? FORGE_U_A : FORGE_U_B;
            boolean hitFrame = (frameIndex == 1);
            if (prog > lastProgress && hitFrame && gt - lastHitSoundTick >= HAMMER_SPEED_TICKS) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.9F));
                lastHitSoundTick = gt;
            }
        } else forgeU = 0;
        lastProgress = prog;
        g.blit(FORGE_ANIM, leftPos + FORGE_X, topPos + FORGE_Y, forgeU, 0, FORGE_W, FORGE_H, 186, 57);

        // виджеты
        boolean hoverL = isHover(mouseX - leftPos, mouseY - topPos, AR_L_X, AR_Y, AR_W, AR_H);
        boolean hoverR = isHover(mouseX - leftPos, mouseY - topPos, AR_R_X, AR_Y, AR_W, AR_H);
        boolean hoverC = isHover(mouseX - leftPos, mouseY - topPos, CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H);
        g.blit(hoverL ? ARROW_L_HOVER : ARROW_L, leftPos + AR_L_X, topPos + AR_Y, 0, 0, AR_W, AR_H, AR_W, AR_H);
        g.blit(hoverR ? ARROW_R_HOVER : ARROW_R, leftPos + AR_R_X, topPos + AR_Y, 0, 0, AR_W, AR_H, AR_W, AR_H);
        g.blit(hoverC ? CRAFT_BTN_H : CRAFT_BTN, leftPos + CRAFT_X, topPos + CRAFT_Y, 0, 0, CRAFT_W, CRAFT_H, CRAFT_W, CRAFT_H);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // без названий “Инвентарь/блок” — оставляем пусто
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        double x = mx - leftPos, y = my - topPos;
        if (isHover(x, y, AR_L_X, AR_Y, AR_W, AR_H)) { clickButton(0); playClick(); return true; }
        if (isHover(x, y, AR_R_X, AR_Y, AR_W, AR_H)) { clickButton(1); playClick(); return true; }
        if (isHover(x, y, CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H)) {
            holdingCraft   = true;
            pressStartTick = (minecraft != null && minecraft.level != null) ? minecraft.level.getGameTime() : 0L;
            hammerStartPhase = 0;
            clickButton(2);
            playClick();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (holdingCraft) {
            holdingCraft = false;
            clickButton(3);
            lastProgress = 0; lastHitSoundTick = -1000L;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override public void onClose() {
        holdingCraft = false;
        clickButton(3);
        super.onClose();
    }
}
