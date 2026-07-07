package com.fib.fib.gui.container.screen.corpse;

import com.fib.fib.FIBMod;
import com.fib.fib.gui.container.menu.corpse.Corpse2Menu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class Corpse2Screen extends AbstractContainerScreen<Corpse2Menu> {


    private static final ResourceLocation GUI =
            new ResourceLocation(FIBMod.MOD_ID, "textures/container/generic_gui.png");


    public Corpse2Screen(Corpse2Menu menu,
                         Inventory playerInventory,
                         Component title) {
        super(menu, playerInventory, title);

        // GUI 的宽度与高度（像素）
        // 这些值通常需要与背景贴图尺寸保持一致
        this.imageWidth = 175;
        this.imageHeight = 165;
    }


    @Override
    protected void init() {
        super.init();
        // 物品栏标题位置
        this.inventoryLabelX = 7;
        this.inventoryLabelY = 72;
        // GUI标题位置
        this.titleLabelX = 7;
        this.titleLabelY = 4;
    }


    //渲染 GUI 背景。
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

        // 渲染鼠标悬停在物品上的提示信息
        renderTooltip(graphics, mouseX, mouseY);
    }
}
