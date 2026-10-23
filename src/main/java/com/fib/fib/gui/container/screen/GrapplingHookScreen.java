package com.fib.fib.gui.container.screen;

import com.fib.fib.FIBMod;
import com.fib.fib.gui.container.menu.GrapplingHookMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 钩爪枪改装与存储仓图形界面屏幕。
 */
public class GrapplingHookScreen extends AbstractContainerScreen<GrapplingHookMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/gui/grappling_hook_gui.png");

    public GrapplingHookScreen(GrapplingHookMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = 92;
        // 隐藏原版容器标题
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int left = this.leftPos;
        int top = this.topPos;

        // 绘制主背景底图
        guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight);

        // 若模块槽 2 未解锁，绘制锁定阴影遮罩
        if (!menu.isSlot2Unlocked()) {
            guiGraphics.fill(left + 38, top + 42, left + 54, top + 58, 0x88000000);
            guiGraphics.drawString(font, "✖", left + 43, top + 47, 0xFFFF5555, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 大标题（“钩爪枪改装与存储”）已通过贴图呈现，这里只画分区小标题
        guiGraphics.drawString(this.font,
                Component.translatable("fib_mod.gui.grappling_hook.upgrades"), 16, 6, 0x404040, false);
        guiGraphics.drawString(this.font,
                Component.translatable("fib_mod.gui.grappling_hook.storage"), 94, 20, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // 悬浮在被锁定的槽位 2 上时显示提示
        if (!menu.isSlot2Unlocked()) {
            int slot2X = leftPos + 38;
            int slot2Y = topPos + 42;
            if (mouseX >= slot2X && mouseX < slot2X + 16 && mouseY >= slot2Y && mouseY < slot2Y + 16) {
                guiGraphics.renderTooltip(font, Component.translatable("fib_mod.gui.slot_locked_smart_module"), mouseX, mouseY);
            }
        }
    }
}
