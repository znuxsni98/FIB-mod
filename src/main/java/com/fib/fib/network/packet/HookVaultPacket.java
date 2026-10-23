package com.fib.fib.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 原「钩爪拉扯边缘跳跃借力」数据包，功能已按需求关闭，仅保留空壳以维持网络包注册顺序。
 */
public class HookVaultPacket {
    public HookVaultPacket() {}

    public HookVaultPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            // 跳跃借力已关闭，不做任何处理。
        });
        return true;
    }
}
