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

            // 快速模式：中键 = 立即回收空中/悬停的快速钩爪，并立即重发第 1 枚。
            // 不走待发射队列，避免"等旧钩爪飞回"的延迟 —— 快速模式本身是连续摆荡。
            if (GrapplingHookGunItem.getMode(held) == GrapplingHookGunItem.Mode.QUICK) {
                List<GrapplingHookEntity> quickHooks = player.level().getEntitiesOfClass(
                        GrapplingHookEntity.class,
                        player.getBoundingBox().inflate(256.0),
                        h -> h.getShooter() == player && !h.isRemoved() && h.isQuickMode()
                                && h.getHookState() != GrapplingHookEntity.State.RETRACTING
                                && h.getHookState() != GrapplingHookEntity.State.DROPPING
                );
                for (GrapplingHookEntity hook : quickHooks) {
                    hook.setRetracting();
                    hook.clearPartnerHook();
                }
                gun.fireQuickHook(player, held, hand);
                return;
            }

            List<GrapplingHookEntity> hooks = player.level().getEntitiesOfClass(
                    GrapplingHookEntity.class,
                    player.getBoundingBox().inflate(256.0),
                    h -> h.getShooter() == player && !h.isRemoved()
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
}
