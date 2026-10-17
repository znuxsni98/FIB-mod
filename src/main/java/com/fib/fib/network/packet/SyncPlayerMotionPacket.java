package com.fib.fib.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端向客户端同步玩家运动矢量的网络数据包。
 */
public class SyncPlayerMotionPacket {
    private final double x;
    private final double y;
    private final double z;

    public SyncPlayerMotionPacket(Vec3 motion) {
        this.x = motion.x;
        this.y = motion.y;
        this.z = motion.z;
    }

    public SyncPlayerMotionPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SyncPlayerMotionPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.setDeltaMovement(new Vec3(x, y, z));
            }
        });
        return true;
    }
}
