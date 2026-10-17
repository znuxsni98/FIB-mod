package com.fib.fib.client.render;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

/**
 * 钩爪枪物品栏双条渲染器。
 * 在物品耐久条上方额外绘制一条高亮青色的能量储备条。
 */
public class GrapplingHookEnergyBarDecorator implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        float energy = GrapplingHookGunItem.getEnergy(stack);
        int barLength = Math.round(13.0f * (energy / GrapplingHookGunItem.MAX_ENERGY));
        int x = xOffset + 2;
        int y = yOffset + 11;

        // 绘制暗黑背景底线
        guiGraphics.fill(x, y, x + 13, y + 1, 0xFF000000);
        // 绘制青色能量储备线
        guiGraphics.fill(x, y, x + barLength, y + 1, 0xFF00D5FF);
        return true;
    }
}
