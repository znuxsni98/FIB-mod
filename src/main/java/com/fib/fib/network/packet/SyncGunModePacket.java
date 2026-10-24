package com.fib.fib.network.packet;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步钩爪枪的模式字段（模式 / 挖掘模式 / 抓取模式）。
 *
 * 物品 NBT 不会随「服务端直接改 tag」自动同步到客户端，
 * 切换模式后若不回传，客户端 KeyBindings 与 HUD 读到的仍是旧模式，
 * 导致攀岩组合键（Z+左/右键）因 getMode() 仍为 PULL 而完全不发包。
 */
public class SyncGunModePacket {
    private final int mode;
    private final int miningMode;
    private final int grappleMode;

    public SyncGunModePacket(ItemStack stack) {
        this.mode = GrapplingHookGunItem.getMode(stack).ordinal();
        this.miningMode = GrapplingHookGunItem.getMiningMode(stack).ordinal();
        this.grappleMode = GrapplingHookGunItem.getGrappleMode(stack).ordinal();
    }

    public SyncGunModePacket(FriendlyByteBuf buf) {
        this.mode = buf.readInt();
        this.miningMode = buf.readInt();
        this.grappleMode = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(mode);
        buf.writeInt(miningMode);
        buf.writeInt(grappleMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GrapplingHookGunItem)) {
                stack = player.getOffhandItem();
            }
            if (!(stack.getItem() instanceof GrapplingHookGunItem)) return;
            // 只覆盖模式字段，不动能量 / 耐久 / 升级槽等
            var tag = stack.getOrCreateTag();
            tag.putInt(GrapplingHookGunItem.TAG_MODE, mode);
            tag.putInt(GrapplingHookGunItem.TAG_MINING_MODE, miningMode);
            tag.putInt(GrapplingHookGunItem.TAG_GRAPPLE_MODE, grappleMode);
        });
        return true;
    }
}
