package com.fib.fib.network.packet;

import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端攀岩操作请求数据包（Z+左键收绳上升，Z+右键放绳下降，中键自动上升）。
 */
public class HookClimbPacket {
    public static final int ACTION_CLIMB_UP = 0;
    public static final int ACTION_CLIMB_DOWN = 1;
    public static final int ACTION_AUTO_ASCEND_TOGGLE = 2;
    public static final int ACTION_STOP = 3;

    private final int action;

    public HookClimbPacket(int action) {
        this.action = action;
    }

    public HookClimbPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                List<GrapplingHookEntity> hooks = player.level().getEntitiesOfClass(
                        GrapplingHookEntity.class,
                        player.getBoundingBox().inflate(64.0),
                        h -> h.getShooter() == player && !h.isRemoved()
                );
                for (GrapplingHookEntity hook : hooks) {
                    hook.handleClimbAction(action);
                }
            }
        });
        return true;
    }
}
