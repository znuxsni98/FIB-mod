package com.fib.fib.network.packet;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求切换钩爪枪工作模式（R 键）。
 * 服务端按「主手优先、副手兜底」取枪。
 */
public class HookModeSwitchPacket {

    public HookModeSwitchPacket() {}

    public HookModeSwitchPacket(FriendlyByteBuf buf) {
        // 无负载
    }

    public void toBytes(FriendlyByteBuf buf) {
        // 无负载
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 主手优先，主手不是钩爪枪时回退到副手
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof GrapplingHookGunItem)) {
                held = player.getOffhandItem();
            }
            if (held.getItem() instanceof GrapplingHookGunItem) {
                GrapplingHookGunItem.cycleMode(held, player);
            }
        });
        return true;
    }
}
