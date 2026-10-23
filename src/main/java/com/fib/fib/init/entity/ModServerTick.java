package com.fib.fib.init.entity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端 Tick 监听：驱动钩爪的「先回收再发射」待办队列。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModServerTick {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        GrapplingHookEntity.processPendingQuickShoot();
    }

    /** 服务器关闭：清空待发射队列，避免静态 Map 跨存档持有旧 ServerPlayer。 */
    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        GrapplingHookEntity.clearPendingQuickShoot();
    }
}
