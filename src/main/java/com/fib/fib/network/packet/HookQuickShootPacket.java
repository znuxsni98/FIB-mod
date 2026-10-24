package com.fib.fib.network.packet;

import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 中键快速发射：先让已有钩爪完全回收，回收完毕后再向准星方向发射新钩爪。
 */
public class HookQuickShootPacket {

    /** 待发射的玩家与剩余等待 tick（简化的延迟发射）。 */
    public HookQuickShootPacket() {}

    public HookQuickShootPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack held = player.getMainHandItem();
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (!(held.getItem() instanceof GrapplingHookGunItem)) {
                held = player.getOffhandItem();
                hand = InteractionHand.OFF_HAND;
            }
            if (!(held.getItem() instanceof GrapplingHookGunItem gun)) {
                return;
            }

            // 快速模式：中键 = 先回收所有快速钩爪，回收完毕后发射新的第 1 枚。
            // 关键：只回收快速钩爪（isQuickMode），双钩组是已部署装备，绝不能被中键波及。
            if (GrapplingHookGunItem.getMode(held) == GrapplingHookGunItem.Mode.QUICK) {
                List<GrapplingHookEntity> quickHooks = player.level().getEntitiesOfClass(
                        GrapplingHookEntity.class,
                        player.getBoundingBox().inflate(256.0),
                        h -> h.getShooter() == player && !h.isRemoved() && h.isQuickMode()
                                && h.getHookState() != GrapplingHookEntity.State.RETRACTING
                                && h.getHookState() != GrapplingHookEntity.State.DROPPING
                );
                if (quickHooks.isEmpty()) {
                    fireQuickNow(player);
                    return;
                }
                for (GrapplingHookEntity hook : quickHooks) {
                    hook.setRetracting();
                    hook.clearPartnerHook();
                }
                com.fib.fib.init.entity.custom.QuickShootQueue.scheduleQuickOnly(player);
                return;
            }

            // 注意排除已成对的双钩与孤钩：
            //  - 已配对：已部署装备，中键这种快速操作不应误伤
            //  - 孤钩（曾配对但 partner 已消失）：留给 tick 的孤钩检测返还，否则会静默丢物品
            List<GrapplingHookEntity> hooks = player.level().getEntitiesOfClass(
                    GrapplingHookEntity.class,
                    player.getBoundingBox().inflate(256.0),
                    h -> h.getShooter() == player && !h.isRemoved()
                            && !h.isPaired() && !h.isEverPaired()
            );

            if (hooks.isEmpty()) {
                fireNow(player);
                return;
            }

            // 已有钩爪：全部转入回收，并记录“回收完成后发射”的待办
            for (GrapplingHookEntity hook : hooks) {
                hook.setRetracting();
                hook.clearPartnerHook();
            }
            com.fib.fib.init.entity.custom.QuickShootQueue.schedule(player);
        });
        return true;
    }

    public static void fireNow(ServerPlayer player) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GrapplingHookGunItem)) {
            held = player.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
        }
        if (held.getItem() instanceof GrapplingHookGunItem gun) {
            gun.fireHook(player, held, hand);
        }
    }

    /**
     * 快速模式的发射（带 quickMode 标记，供 QUICK 模式中键回收完成后调用）。
     *
     * 边界：登记请求到真正发射之间可能隔了几 tick，玩家可能已经按 R 切走模式。
     * 此时不能再发一枚 quickMode=true 的钩爪（它会绕过双钩配对逻辑）。
     */
    public static void fireQuickNow(ServerPlayer player) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GrapplingHookGunItem)) {
            held = player.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
        }
        if (!(held.getItem() instanceof GrapplingHookGunItem gun)) {
            return;
        }
        // 模式已切走：放弃本次发射，玩家再按中键即可
        if (GrapplingHookGunItem.getMode(held) != GrapplingHookGunItem.Mode.QUICK) {
            return;
        }
        gun.fireQuickHook(player, held, hand);
    }
}
