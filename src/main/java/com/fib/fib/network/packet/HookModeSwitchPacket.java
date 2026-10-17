package com.fib.fib.network.packet;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求切换钩爪枪工作模式的网络数据包（按下 R 键触发）。
 */
public class HookModeSwitchPacket {
    public HookModeSwitchPacket() {}

    public HookModeSwitchPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() instanceof GrapplingHookGunItem) {
                    GrapplingHookGunItem.cycleMode(mainHand, player);
                }
            }
        });
        return true;
    }
}
