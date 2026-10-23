package com.fib.fib.client;

import com.fib.fib.FIBMod;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import com.fib.fib.network.ModMessages;
import com.fib.fib.network.packet.HookClimbPacket;
import com.fib.fib.network.packet.HookMiningTogglePacket;
import com.fib.fib.network.packet.HookModeSwitchPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键绑定与输入事件分发管理器。
 */
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.categories.fib_mod";

    public static final KeyMapping KEY_MODE_SWITCH = new KeyMapping(
            "key.fib_mod.hook_mode_switch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    public static final KeyMapping KEY_MINING_TOGGLE = new KeyMapping(
            "key.fib_mod.hook_mining_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY
    );

    public static final KeyMapping KEY_CLIMB = new KeyMapping(
            "key.fib_mod.hook_climb",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            KEY_CATEGORY
    );

    public static final KeyMapping KEY_OPEN_GUI = new KeyMapping(
            "key.fib_mod.hook_open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY
    );

    /** 取玩家主手/副手中的钩爪枪（主手优先）。 */
    @Nullable
    private static ItemStack getHeldGun(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GrapplingHookGunItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof GrapplingHookGunItem) return off;
        return null;
    }

    @Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(KEY_MODE_SWITCH);
            event.register(KEY_MINING_TOGGLE);
            event.register(KEY_CLIMB);
            event.register(KEY_OPEN_GUI);
        }
    }

    @Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.screen != null) return;

            // 主手优先、副手兜底
            ItemStack stack = getHeldGun(player);
            if (stack == null) return;

            // H 打开改装界面
            while (KEY_OPEN_GUI.consumeClick()) {
                ModMessages.sendToServer(new com.fib.fib.network.packet.HookOpenGuiPacket());
            }

            // R 模式切换（服务端按「主手优先、副手兜底」取枪）
            while (KEY_MODE_SWITCH.consumeClick()) {
                ModMessages.sendToServer(new HookModeSwitchPacket());
            }

            // G 挖掘模式切换
            while (KEY_MINING_TOGGLE.consumeClick()) {
                ModMessages.sendToServer(new HookMiningTogglePacket());
            }

            // 攀岩组合键：按住 Z 时持续收绳/放绳（左键放绳下降、右键收绳上升）
            // 读取用 GLFW 物理状态（不受 KeyMapping 影响）；
            // 同时清掉原版的 keyUse/keyAttack 状态，避免持续播放右键使用动画或挖方块。
            if (KEY_CLIMB.isDown() && GrapplingHookGunItem.getMode(stack) == GrapplingHookGunItem.Mode.CLIMB) {
                long window = mc.getWindow().getWindow();
                boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

                // 清掉原版按键状态，阻断“持续使用物品/挖掘”动画
                mc.options.keyUse.setDown(false);
                mc.options.keyAttack.setDown(false);
                if (mc.player != null && mc.player.isUsingItem()) {
                    mc.player.stopUsingItem();
                }

                if (left != right) {
                    ModMessages.sendToServer(new HookClimbPacket(
                            left ? HookClimbPacket.ACTION_CLIMB_DOWN : HookClimbPacket.ACTION_CLIMB_UP));
                }
            }
        }

        @SubscribeEvent
        public static void onMouseClick(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.screen != null) return;

            ItemStack stack = getHeldGun(player);
            if (stack == null) return;

            GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(stack);

            // 攀岩操作中：左/右键被征用为收放绳，阻止其破坏/放置方块
            if (mode == GrapplingHookGunItem.Mode.CLIMB && KEY_CLIMB.isDown()) {
                int button = event.getButton();
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    event.setCanceled(true);
                    return;
                }
            }

            // 中键：所有模式均为「先回收再发射」；
            // 攀岩模式下按住 Z + 中键 = 自动上升（自动拉取）。
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && event.getAction() == GLFW.GLFW_PRESS) {
                if (mode == GrapplingHookGunItem.Mode.CLIMB && KEY_CLIMB.isDown()) {
                    ModMessages.sendToServer(new HookClimbPacket(HookClimbPacket.ACTION_AUTO_ASCEND_TOGGLE));
                } else {
                    ModMessages.sendToServer(new com.fib.fib.network.packet.HookQuickShootPacket());
                }
                event.setCanceled(true);
                return;
            }
        }
    }
}
