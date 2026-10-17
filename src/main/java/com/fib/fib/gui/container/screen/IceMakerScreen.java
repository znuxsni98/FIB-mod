package com.fib.fib.gui.container.screen;

import com.fib.fib.FIBMod;
import com.fib.fib.blockentity.IceMakerBlockEntity;
import com.fib.fib.gui.container.menu.IceMakerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * 制冰机界面。
 *
 * <p>下面这些坐标都是从 {@code textures/container/ice_maker_gui.png} 里逐像素量出来的，
 * 不是估的。那张 PNG 是 256×256，真正的界面 175×165 摆在左上角（和原版贴图一个套路），
 * 所以下面所有坐标都是相对界面左上角的偏移：
 * <ul>
 *   <li>液罐内壁：x 39..52，y 10..72 —— 14 宽 63 高，刻度尺在左半边</li>
 *   <li>进度箭头（画在背景里的灰色空箭头）：x 97..118，y 35..50 —— 22 宽 16 高</li>
 *   <li>白色实心箭头素材：同一张贴图右上角 x 177..198，y 0..15 —— 也是 22×16</li>
 * </ul>
 */
public class IceMakerScreen extends AbstractContainerScreen<IceMakerMenu> {


    private static final ResourceLocation GUI =
            new ResourceLocation(FIBMod.MOD_ID, "textures/container/ice_maker_gui.png");

    // ==================== 贴图内实测坐标 ====================
    private static final int TANK_X = 39;
    private static final int TANK_Y = 10;
    private static final int TANK_W = 14;
    private static final int TANK_H = 63;

    private static final int ARROW_X = 97;
    private static final int ARROW_Y = 35;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 16;

    /**
     * 白色箭头素材在贴图里的起点。
     *
     * <p>那块素材名义上是 24×17（x 176..199、y 0..16），但外面多出来的一圈是纯底色
     * {@code (198,198,198)} —— 和主界面的底色一模一样，是画的时候留的余量。
     * 真正的箭头只有里面 22×16（x 177..198、y 0..15），正好和背景那个灰色空箭头同尺寸。
     * <b>所以要从 177,0 起取 22×16</b>；整块 24×17 盖上去会偏一个像素。
     */
    private static final int ARROW_SRC_X = 177;
    private static final int ARROW_SRC_Y = 0;

    private static final int LABEL_COLOR = 0xFF404040;
    private static final int VALUE_COLOR = 0xFF1A5FB4;


    public IceMakerScreen(IceMakerMenu menu,
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
        // 物品栏标题位置：和背包第一行槽位（物品 x=8）左对齐
        this.inventoryLabelX = 8;
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

        renderFluidLevel(guiGraphics, x, y);
        renderProgress(guiGraphics, x, y);
    }

    /** 液罐：按存量从底往上填，颜色取流体的 tint。 */
    private void renderFluidLevel(GuiGraphics guiGraphics, int left, int top) {
        IceMakerBlockEntity be = this.menu.getBlockEntity();
        if (be == null) {
            return;
        }

        FluidStack fluid = be.getFluidTank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }

        int amount = fluid.getAmount();
        int capacity = be.getFluidTank().getCapacity();
        if (capacity <= 0 || amount <= 0) {
            return;
        }

        int h = Math.max(1, Math.round(TANK_H * amount / (float) capacity));
        int y2 = top + TANK_Y + TANK_H;

        // 只保留 tint 的 RGB，alpha 压到 0xB0 —— 底下的刻度尺还透得出来，
        // 灌满了也能看见还剩多少格。
        int tint = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        int color = (tint & 0x00FFFFFF) | 0xB0000000;

        guiGraphics.fill(left + TANK_X, y2 - h, left + TANK_X + TANK_W, y2, color);
    }

    /**
     * 箭头：把白色箭头素材按进度从左往右"揭开"，盖在背景那个灰色空箭头上。
     *
     * <p>只取素材最左边 {@code w} 列。先露出的是一小截箭杆，随着进度推进箭头才被填满 ——
     * 和原版熔炉一个套路。没走到的地方保持背景里的灰色空箭头。
     *
     * <p>素材底色和主界面底色是同一个灰，所以被盖住的那一段不会出现色块接缝。
     */
    private void renderProgress(GuiGraphics guiGraphics, int left, int top) {
        int progress = this.menu.getData().get(0);
        int max = this.menu.getData().get(1);
        if (max <= 0 || progress <= 0) {
            return;
        }

        int w = Math.round(ARROW_W * Math.min(progress, max) / (float) max);
        if (w <= 0) {
            return;
        }

        guiGraphics.blit(GUI, left + ARROW_X, top + ARROW_Y,
                ARROW_SRC_X, ARROW_SRC_Y, w, ARROW_H);
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // 绘制界面背景（灰色遮罩）
        renderBackground(graphics);

        // 调用父类渲染 GUI 元素
        super.render(graphics, mouseX, mouseY, partialTick);

        renderEnergyText(graphics);
        renderFluidTooltip(graphics, mouseX, mouseY);

        // 渲染鼠标悬停在物品上的提示信息
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** 右上角画 "1234 / 20000 FE"。 */
    private void renderEnergyText(GuiGraphics graphics) {
        int stored = this.menu.getData().get(2);
        int capacity = this.menu.getData().get(3);
        String value = stored + " / " + capacity + " " + Component.translatable("fib_mod.generic.unit.fe").getString();

        int x = this.leftPos + imageWidth - 8 - this.font.width(value);
        int y = this.topPos + 4;
        graphics.drawString(this.font, value, x, y, VALUE_COLOR, false);
    }

    /** 鼠标悬在液罐上时显示流体名和具体数值。 */
    private void renderFluidTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            return;
        }

        IceMakerBlockEntity be = this.menu.getBlockEntity();
        if (be == null) {
            return;
        }

        FluidStack fluid = be.getFluidTank().getFluid();
        Component line = fluid.isEmpty()
                ? Component.literal("0 / " + be.getFluidTank().getCapacity() + " mB")
                        .withStyle(ChatFormatting.GRAY)
                : Component.literal(fluid.getDisplayName().getString() + "  "
                        + fluid.getAmount() + " / " + be.getFluidTank().getCapacity() + " mB");

        // 1.20.1 的 GuiGraphics 没有 renderTooltip(Font, List<Component>, ...) 这个重载，
        // 多行文本要走 renderComponentTooltip。
        graphics.renderComponentTooltip(this.font, List.of(line), mouseX, mouseY);
    }
}
