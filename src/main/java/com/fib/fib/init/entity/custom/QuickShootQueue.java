package com.fib.fib.init.entity.custom;

import com.fib.fib.network.packet.HookQuickShootPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中键「先回收再发射」的待发射队列。
 *
 * 玩家按下中键后，若有钩爪存在，先让它们全部回收；
 * 本队列在每个服务端 tick 检查「钩爪是否已全部消失」，消失后立即发射。
 *
 * 清理策略（防止静态 Map 长期持有 ServerPlayer 引用）：
 *  1. 玩家死亡/被移除/下线 -> 丢弃；
 *  2. 超时（10 秒未完成）-> 丢弃；
 *  3. 服务器关闭 -> 清空。
 */
public final class QuickShootQueue {

    /** 超时时长（tick）：10 秒未完成则丢弃。 */
    private static final long TIMEOUT_TICKS = 200L;

    /** 一条待发射请求：玩家 + 登记时的游戏时间。 */
    private record PendingShot(ServerPlayer player, long scheduledAt) {}

    private static final Map<UUID, PendingShot> PENDING = new ConcurrentHashMap<>();

    private QuickShootQueue() {}

    /** 登记一个「回收完成后立即发射」的请求。 */
    public static void schedule(ServerPlayer player) {
        PENDING.put(player.getUUID(), new PendingShot(player, player.serverLevel().getGameTime()));
    }

    /** 服务器关闭时清空队列。 */
    public static void clear() {
        PENDING.clear();
    }

    /** 每个服务端 tick 调用：处理到期的待发射请求。 */
    public static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, PendingShot>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingShot> entry = it.next();
            PendingShot req = entry.getValue();
            ServerPlayer player = (req == null) ? null : req.player();

            // 1. 玩家无效 -> 丢弃
            if (player == null || !player.isAlive() || player.isRemoved() || player.hasDisconnected()) {
                it.remove();
                continue;
            }

            // 2. 超时 -> 丢弃
            long now = player.serverLevel().getGameTime();
            if (req.scheduledAt() > 0 && now - req.scheduledAt() > TIMEOUT_TICKS) {
                it.remove();
                continue;
            }

            // 3. 钩爪已全部消失 -> 发射并移除
            boolean hasHook = !player.level().getEntitiesOfClass(
                    GrapplingHookEntity.class,
                    player.getBoundingBox().inflate(256.0),
                    h -> h.getShooter() == player && !h.isRemoved()).isEmpty();
            if (!hasHook) {
                it.remove();
                HookQuickShootPacket.fireNow(player);
            }
        }
    }
}
