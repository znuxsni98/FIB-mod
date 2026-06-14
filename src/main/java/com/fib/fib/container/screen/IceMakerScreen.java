package com.fib.fib.container.screen;

import com.fib.fib.FIBMod;
import com.fib.fib.container.menu.IceMakerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IceMakerScreen extends AbstractContainerScreen<IceMakerMenu> {


    private static final ResourceLocation GUI =
            new ResourceLocation(FIBMod.MOD_ID, "textures/container/ice_maker.png");


    public IceMakerScreen(IceMakerMenu menu,
                                          Inventory playerInventory,
                                          Component title) {
        super(menu, playerInventory, title);

        // GUI 的宽度与高度（像素）
        // 这些值通常需要与背景贴图尺寸保持一致
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * 渲染 GUI 背景。
     *
     * 该方法负责绘制界面的底层贴图。
     * 在这里我们只绘制一张固定的 GUI 背景图。
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

        // 设置渲染使用的 Shader
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // 设置颜色（RGBA），1 表示不改变原贴图颜色
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        // 绑定要绘制的纹理
        RenderSystem.setShaderTexture(0, GUI);

        // 计算 GUI 左上角的位置，使界面居中显示
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 绘制贴图
        // 参数含义：
        // GUI：纹理
        // x,y：屏幕上的绘制位置
        // 0,0：纹理起始坐标
        // imageWidth,imageHeight：绘制区域大小
        guiGraphics.blit(GUI, x, y, 0, 0, imageWidth, imageHeight);
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // 绘制界面背景（灰色遮罩）
        renderBackground(graphics);

        // 调用父类渲染 GUI 元素
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
