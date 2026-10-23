package com.fib.fib.client.render.item.GrapplingHook;

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
 * 实时显示准星射程测距指示、能量储备条、当前作业模式与 8 格存储仓容量。
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

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // 1. 准星旁测距指示：仅在安装「智能升级模块」时显示
        if (GrapplingHookGunItem.hasSmartUpgradeModule(stack)) {
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 lookVec = player.getViewVector(partialTick);
        Vec3 reachVec = eyePos.add(lookVec.scale(256.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eyePos, reachVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.BLOCK) {
            double dist = eyePos.distanceTo(hit.getLocation());
            String distStr = String.format("%.1fm", dist);
            guiGraphics.drawString(font, distStr, centerX + 10, centerY - 4, 0x55FF55, true);
            guiGraphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, 0xFF55FF55);
        }
        }

        // 2. 状态信息面板（右下角）
        float energy = GrapplingHookGunItem.getEnergy(stack);
        // 背罐可换算能量（未穿戴时为 0）
        float backtankEnergy = Math.max(0.0f, GrapplingHookGunItem.getAvailableEnergy(player, stack) - energy);
        GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(stack);
        boolean hasMining = GrapplingHookGunItem.isMiningModuleEffective(stack);
        GrapplingHookGunItem.MiningMode miningMode = GrapplingHookGunItem.getMiningMode(stack);

        int countStored = 0;
        var storage = GrapplingHookGunItem.getStorageHandler(stack);
        for (int i = 0; i < GrapplingHookGunItem.STORAGE_SLOTS; i++) {
            if (!storage.getStackInSlot(i).isEmpty()) countStored++;
        }

        int panelX = screenWidth - 110;
        int panelY = screenHeight - 65;

        // 背景半透明暗框
        guiGraphics.fill(panelX - 4, panelY - 4, panelX + 105, panelY + 58, 0x77000000);
        guiGraphics.renderOutline(panelX - 4, panelY - 4, 109, 62, 0xFF445566);

        // 能量文本：有背罐时显示「枪内 + 背罐」（例 10.0 + 120.0），否则只显示枪内
        Component energyText = (backtankEnergy > 0.01f)
                ? Component.translatable("fib_mod.hud.energy",
                        String.format("%.1f", energy),
                        String.format("%.1f", backtankEnergy))
                : Component.translatable("fib_mod.hud.energy_single",
                        String.format("%.1f", energy));
        guiGraphics.drawString(font, energyText, panelX, panelY, 0x00E5FF, true);
        // 进度条按「枪内 + 背罐」总量填充（上限 20）
        float totalEnergy = energy + backtankEnergy;
        int barWidth = 98;
        int filledWidth = (int) (barWidth * Math.min(1.0f, totalEnergy / GrapplingHookGunItem.MAX_ENERGY));
        guiGraphics.fill(panelX, panelY + 11, panelX + barWidth, panelY + 14, 0xFF222222);
        guiGraphics.fill(panelX, panelY + 11, panelX + filledWidth, panelY + 14, 0xFF00D0FF);

        // 工作模式
        guiGraphics.drawString(font, Component.translatable("fib_mod.hud.mode", Component.translatable(mode.getTranslationKey())), panelX, panelY + 18, 0xFFA000, true);

        // 挖掘状态（仅在安装挖掘模块时显示）
        if (hasMining) {
            guiGraphics.drawString(font, Component.translatable("fib_mod.hud.mining", Component.translatable(miningMode.getTranslationKey())), panelX, panelY + 29, 0xFFFF55, true);
        }

        // 存储仓容量
        guiGraphics.drawString(font, Component.translatable("fib_mod.hud.storage", countStored, GrapplingHookGunItem.STORAGE_SLOTS), panelX, panelY + 40, 0xCCCCCC, true);
    }
}
