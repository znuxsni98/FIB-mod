package com.fib.fib.client.render;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 钩爪枪专用 HUD 界面叠加层。
 * 实时显示准星射程测距指示、能量储备条、当前作业模式与 16 格存储仓容量。
 */
public class GrapplingHookOverlay {
    public static final IGuiOverlay HUD = GrapplingHookOverlay::renderHUD;

    private static void renderHUD(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GrapplingHookGunItem)) {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof GrapplingHookGunItem)) {
                return;
            }
        }

        Font font = mc.font;
        boolean pressurized = GrapplingHookGunItem.hasPressurizedModule(stack);
        float maxRange = pressurized ? 24.0f : 16.0f;

        // 1. 准星旁射程测距指示
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 lookVec = player.getViewVector(partialTick);
        Vec3 reachVec = eyePos.add(lookVec.scale(maxRange + 10.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eyePos, reachVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        if (hit.getType() == HitResult.Type.BLOCK) {
            double dist = eyePos.distanceTo(hit.getLocation());
            if (dist <= maxRange) {
                String distStr = String.format("%.1fm", dist);
                guiGraphics.drawString(font, distStr, centerX + 10, centerY - 4, 0x55FF55, true);
                // 绘制准星内锁点
                guiGraphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, 0xFF55FF55);
            } else {
                String distStr = String.format("%.1fm", dist);
                guiGraphics.drawString(font, distStr, centerX + 10, centerY - 4, 0xAAAAAA, true);
            }
        }

        // 2. 状态信息面板（右下角）
        float energy = GrapplingHookGunItem.getEnergy(stack);
        GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(stack);
        boolean hasMining = GrapplingHookGunItem.hasMiningModule(stack);
        GrapplingHookGunItem.MiningMode miningMode = GrapplingHookGunItem.getMiningMode(stack);

        int countStored = 0;
        var storage = GrapplingHookGunItem.getStorageHandler(stack);
        for (int i = 0; i < 16; i++) {
            if (!storage.getStackInSlot(i).isEmpty()) countStored++;
        }

        int panelX = screenWidth - 110;
        int panelY = screenHeight - 65;

        // 背景半透明暗框
        guiGraphics.fill(panelX - 4, panelY - 4, panelX + 105, panelY + 58, 0x77000000);
        guiGraphics.renderOutline(panelX - 4, panelY - 4, 109, 62, 0xFF445566);

        // 能量文本与进度条
        guiGraphics.drawString(font, Component.translatable("fib_mod.hud.energy", String.format("%.1f", energy)), panelX, panelY, 0x00E5FF, true);
        int barWidth = 98;
        int filledWidth = (int) (barWidth * (energy / GrapplingHookGunItem.MAX_ENERGY));
        guiGraphics.fill(panelX, panelY + 11, panelX + barWidth, panelY + 14, 0xFF222222);
        guiGraphics.fill(panelX, panelY + 11, panelX + filledWidth, panelY + 14, 0xFF00D0FF);

        // 工作模式
        guiGraphics.drawString(font, Component.translatable("fib_mod.hud.mode", Component.translatable(mode.getTranslationKey())), panelX, panelY + 18, 0xFFA000, true);

        // 挖掘状态
        if (hasMining) {
            guiGraphics.drawString(font, Component.translatable("fib_mod.hud.mining", Component.translatable(miningMode.getTranslationKey())), panelX, panelY + 29, 0xFFFF55, true);
        } else {
            guiGraphics.drawString(font, Component.translatable("fib_mod.hud.mining.none"), panelX, panelY + 29, 0x777777, true);
        }

        // 存储仓容量
        guiGraphics.drawString(font, Component.translatable("fib_mod.hud.storage", countStored, 16), panelX, panelY + 40, 0xCCCCCC, true);
    }
}
