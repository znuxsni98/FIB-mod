package com.fib.fib.network.packet;

import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 钩爪拉扯接近平台边缘时按下跳跃键触发的高跳借力数据包（提升 2-3 格高度）。
 */
public class HookVaultPacket {
    public HookVaultPacket() {}

    public HookVaultPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.isAlive()) {
                List<GrapplingHookEntity> pullingHooks = player.level().getEntitiesOfClass(
                        GrapplingHookEntity.class,
                        player.getBoundingBox().inflate(16.0),
                        h -> h.getShooter() == player && (h.isHooked() && h.getHookState() != GrapplingHookEntity.State.MINING)
                );

                for (GrapplingHookEntity hook : pullingHooks) {
                    Vec3 anchor = hook.getAnchorPos();
                    double dist = player.position().distanceTo(anchor);
                    if (dist <= 4.5) {
                        // 施加向前与向上的 2.5~3 格高跳借力矢量
                        Vec3 fwd = new Vec3(anchor.x - player.getX(), 0, anchor.z - player.getZ()).normalize().scale(0.38);
                        Vec3 boost = new Vec3(fwd.x, 0.65, fwd.z);

                        player.setDeltaMovement(boost);
                        player.hurtMarked = true;
                        player.resetFallDistance();

                        // 回收该钩爪
                        hook.setRetracting();

                        // 音效与粒子表现
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.4f);

                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                                    12, 0.25, 0.1, 0.25, 0.05);
                        }
                        break;
                    }
                }
            }
        });
        return true;
    }
}
