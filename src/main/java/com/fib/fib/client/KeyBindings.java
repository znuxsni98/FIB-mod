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

    @Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(KEY_MODE_SWITCH);
            event.register(KEY_MINING_TOGGLE);
            event.register(KEY_CLIMB);
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

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GrapplingHookGunItem)) return;

            // R 模式切换
            while (KEY_MODE_SWITCH.consumeClick()) {
                ModMessages.sendToServer(new HookModeSwitchPacket());
            }

            // G 挖掘模式切换
            while (KEY_MINING_TOGGLE.consumeClick()) {
                ModMessages.sendToServer(new HookMiningTogglePacket());
            }

            // 空格键平台边缘借力高跳 (2-3格)
            if (mc.options.keyJump.consumeClick() && mc.level != null) {
                java.util.List<com.fib.fib.init.entity.custom.GrapplingHookEntity> hooks = mc.level.getEntitiesOfClass(
                        com.fib.fib.init.entity.custom.GrapplingHookEntity.class,
                        player.getBoundingBox().inflate(16.0),
                        h -> h.getShooter() == player && (h.isHooked() && h.getHookState() != com.fib.fib.init.entity.custom.GrapplingHookEntity.State.MINING)
                );

                for (com.fib.fib.init.entity.custom.GrapplingHookEntity hook : hooks) {
                    net.minecraft.world.phys.Vec3 anchor = hook.getAnchorPos();
                    double dist = player.position().distanceTo(anchor);
                    if (dist <= 4.5) {
                        ModMessages.sendToServer(new com.fib.fib.network.packet.HookVaultPacket());
                        net.minecraft.world.phys.Vec3 fwd = new net.minecraft.world.phys.Vec3(anchor.x - player.getX(), 0, anchor.z - player.getZ()).normalize().scale(0.38);
                        player.setDeltaMovement(fwd.x, 0.65, fwd.z);
                        player.resetFallDistance();
                        break;
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onMouseClick(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.screen != null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GrapplingHookGunItem)) return;

            GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(stack);
            if (mode != GrapplingHookGunItem.Mode.CLIMB) return;

            // 中键自动上升
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && event.getAction() == GLFW.GLFW_PRESS) {
                ModMessages.sendToServer(new HookClimbPacket(HookClimbPacket.ACTION_AUTO_ASCEND_TOGGLE));
                event.setCanceled(true);
                return;
            }

            // 按住 Z 时的攀岩放绳/收绳
            if (KEY_CLIMB.isDown() && event.getAction() == GLFW.GLFW_PRESS) {
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    // Z + 左键收绳上升
                    ModMessages.sendToServer(new HookClimbPacket(HookClimbPacket.ACTION_CLIMB_UP));
                    event.setCanceled(true);
                } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    // Z + 右键放绳下降
                    ModMessages.sendToServer(new HookClimbPacket(HookClimbPacket.ACTION_CLIMB_DOWN));
                    event.setCanceled(true);
                }
            }
        }
    }
}
