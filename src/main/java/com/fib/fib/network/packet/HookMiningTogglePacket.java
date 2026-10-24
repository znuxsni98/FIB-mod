package com.fib.fib.network.packet;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求切换钩爪枪挖掘模式的网络数据包（按下 G 键触发）。
 */
public class HookMiningTogglePacket {
    public HookMiningTogglePacket() {}

    public HookMiningTogglePacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack held = player.getMainHandItem();
                if (!(held.getItem() instanceof GrapplingHookGunItem)) {
                    held = player.getOffhandItem();
                }
                if (held.getItem() instanceof GrapplingHookGunItem) {
                    if (GrapplingHookGunItem.isGrappleModuleEffective(held)) {
                        GrapplingHookGunItem.cycleGrappleMode(held, player);
                    } else if (GrapplingHookGunItem.isMiningModuleEffective(held)) {
                        GrapplingHookGunItem.cycleMiningMode(held, player);
                    }
                    // 未安装相关模块时不提示。

                    // 回传模式，让客户端 KeyBindings / HUD 立即读到新值
                    com.fib.fib.network.ModMessages.sendToPlayer(
                            new com.fib.fib.network.packet.SyncGunModePacket(held), player);

                    // 立即把新模式同步到已发射的钩爪，使钻头模式即时切换
                    for (com.fib.fib.init.entity.custom.GrapplingHookEntity hook
                            : player.level().getEntitiesOfClass(
                                    com.fib.fib.init.entity.custom.GrapplingHookEntity.class,
                                    player.getBoundingBox().inflate(256.0),
                                    h -> h.getShooter() == player && !h.isRemoved())) {
                        hook.refreshGunState(player);
                    }
                }
            }
        });
        return true;
    }
}
