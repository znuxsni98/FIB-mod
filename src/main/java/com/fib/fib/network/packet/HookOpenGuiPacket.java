package com.fib.fib.network.packet;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端请求打开钩爪枪改装界面（H 键触发）。
 */
public class HookOpenGuiPacket {
    public HookOpenGuiPacket() {}

    public HookOpenGuiPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.getMainHandItem().getItem() instanceof GrapplingHookGunItem) {
                GrapplingHookGunItem.openUpgradeMenu(player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
            } else if (player.getOffhandItem().getItem() instanceof GrapplingHookGunItem) {
                GrapplingHookGunItem.openUpgradeMenu(player, player.getOffhandItem(), InteractionHand.OFF_HAND);
            }
        });
        return true;
    }
}
