package com.fib.fib.network.packet;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求「用副手钩爪枪执行一次右键」。
 *
 * 原版只在主手物品不处理右键时才会触发副手 use，
 * 因此主手拿着别的物品时，副手钩爪枪的右键完全不会响应。
 * 快速模式需要边摆荡边用主手做事，故由客户端主动转发。
 */
public class OffhandGunUsePacket {

    public OffhandGunUsePacket() {}

    public OffhandGunUsePacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ItemStack off = player.getOffhandItem();
            if (off.getItem() instanceof GrapplingHookGunItem gun) {
                gun.use(player.level(), player, InteractionHand.OFF_HAND);
            }
        });
        return true;
    }
}
