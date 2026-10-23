package com.fib.fib.network;

import com.fib.fib.FIBMod;
import com.fib.fib.network.packet.HookClimbPacket;
import com.fib.fib.network.packet.HookMiningTogglePacket;
import com.fib.fib.network.packet.HookModeSwitchPacket;
import com.fib.fib.network.packet.HookOpenGuiPacket;
import com.fib.fib.network.packet.HookQuickShootPacket;
import com.fib.fib.network.packet.SyncPlayerMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通信管理类，负责注册和分发客户端与服务端之间的网络数据包。
 */
public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FIBMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        INSTANCE.messageBuilder(HookModeSwitchPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HookModeSwitchPacket::new)
                .encoder(HookModeSwitchPacket::toBytes)
                .consumerMainThread(HookModeSwitchPacket::handle)
                .add();

        INSTANCE.messageBuilder(HookMiningTogglePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HookMiningTogglePacket::new)
                .encoder(HookMiningTogglePacket::toBytes)
                .consumerMainThread(HookMiningTogglePacket::handle)
                .add();

        INSTANCE.messageBuilder(HookClimbPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HookClimbPacket::new)
                .encoder(HookClimbPacket::toBytes)
                .consumerMainThread(HookClimbPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncPlayerMotionPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncPlayerMotionPacket::new)
                .encoder(SyncPlayerMotionPacket::toBytes)
                .consumerMainThread(SyncPlayerMotionPacket::handle)
                .add();

        INSTANCE.messageBuilder(com.fib.fib.network.packet.HookVaultPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(com.fib.fib.network.packet.HookVaultPacket::new)
                .encoder(com.fib.fib.network.packet.HookVaultPacket::toBytes)
                .consumerMainThread(com.fib.fib.network.packet.HookVaultPacket::handle)
                .add();

        INSTANCE.messageBuilder(HookOpenGuiPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HookOpenGuiPacket::new)
                .encoder(HookOpenGuiPacket::toBytes)
                .consumerMainThread(HookOpenGuiPacket::handle)
                .add();

        INSTANCE.messageBuilder(HookQuickShootPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HookQuickShootPacket::new)
                .encoder(HookQuickShootPacket::toBytes)
                .consumerMainThread(HookQuickShootPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
