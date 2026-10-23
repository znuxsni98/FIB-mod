package com.fib.fib.client.render.item.GrapplingHook;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

/**
 * 钩爪枪物品栏双条渲染器。
 * 在物品耐久条上方额外绘制一条高亮青色的能量条（枪内 + 背罐合计）。
 */
public class GrapplingHookEnergyBarDecorator implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        var player = Minecraft.getInstance().player;
        float energy = GrapplingHookGunItem.getAvailableEnergy(player, stack);
        int barLength = Math.round(13.0f * Math.min(1.0f, energy / GrapplingHookGunItem.MAX_ENERGY));
        int x = xOffset + 2;
        // 原版耐久条位于 yOffset+13（占 2px）
        // 能量条放在 yOffset+11，与耐久条之间留 1px 缝隙
        int y = yOffset + 11;

        // 绘制暗黑背景底线
        guiGraphics.fill(x, y, x + 13, y + 1, 0xFF000000);
        // 绘制青色能量线（含背罐）
        guiGraphics.fill(x, y, x + barLength, y + 1, 0xFF00D5FF);
        return true;
    }
}
