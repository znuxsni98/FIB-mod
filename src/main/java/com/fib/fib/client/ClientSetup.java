package com.fib.fib.client;

import com.fib.fib.FIBMod;
import com.fib.fib.client.render.block.RuMengDollBlockEntityRenderer;
import com.fib.fib.client.render.entity.DimensionalCrackRenderer;
import com.fib.fib.client.render.entity.HajjiAuthorRenderer;
import com.fib.fib.client.render.entity.IceBallRenderer;
import com.fib.fib.gui.container.screen.CrateScreen;
import com.fib.fib.gui.container.screen.IceMakerScreen;
import com.fib.fib.gui.container.screen.TrashCanScreen;
import com.fib.fib.gui.container.screen.corpse.Corpse1Screen;
import com.fib.fib.gui.container.screen.corpse.Corpse2Screen;
import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.init.ModMenuTypes;
import com.fib.fib.init.entity.ModEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * --------------------------------------------------------------------------
 * 🖥️ 客户端统一注册中心 (ClientSetup)
 * --------------------------------------------------------------------------
 * 【初中生大白话讲解】：
 * 想象你在玩联机游戏，你的电脑有屏幕有显卡（叫“客户端”），而开服的电脑可能根本连显示器都没有（叫“服务端”）。
 * 这个类里面所有的事情，都是只有【有显示器的客户端】才能干的！
 * 比如：
 * 1. 玩家打开箱子/制冰机时，屏幕上弹出的那个 GUI 贴图界面 (Screen)；
 * 2. 玩家眼里看到的 3D 实体模型与动作渲染 (Renderer)；
 * 
 * ⚠️【开发铁则】：
 * 这个类头顶加了 {@code value = Dist.CLIENT}，代表 Forge 会保护它，
 * 绝不会让无界面的服务端加载这个类。所有客户端视觉专属的内容，统一写在这里，严禁分散在其它地方！
 */
@Mod.EventBusSubscriber(
        modid = FIBMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientSetup {

    /**
     * 客户端初始化阶段 (FMLClientSetupEvent)。
     * 当游戏客户端启动到对应阶段时，Forge 会喊醒这个方法。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // enqueueWork 表示把任务排进主线程队列执行，防止多线程并发冲突导致界面注册崩溃
        event.enqueueWork(() -> {
            registerScreens();
        });
    }

    /**
     * 注册所有实体与方块实体的渲染器 (RegisterRenderers)。
     * 告诉游戏：“如果世界上刷新了这个实体/方块，请用哪一个类去把它画出来”。
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 1. 注册方块实体渲染器（比如：如梦玩偶带动画/特殊外形）
        event.registerBlockEntityRenderer(
                ModBlockEntities.RU_MENG_DOLL_BE.get(),
                RuMengDollBlockEntityRenderer::new
        );

        // 2. 注册普通实体渲染器
        // 冰球投射物（ThrownItemRenderer 自动渲染物品贴图）
        event.registerEntityRenderer(
                ModEntities.ICE_BALL.get(),
                IceBallRenderer::new
        );

        // 哈吉作者 Boss
        event.registerEntityRenderer(
                ModEntities.HAJJI_AUTHOR.get(),
                HajjiAuthorRenderer::new
        );

        // 维度裂缝（结合 Core Shader 逐像素渲染）
        event.registerEntityRenderer(
                ModEntities.DIMENSIONAL_CRACK.get(),
                DimensionalCrackRenderer::new
        );
    }

    /**
     * 绑定 MenuType 与 Screen（GUI 界面绑定）。
     * 
     * 【原理解释】：
     * Menu 是在服务端和客户端之间同步槽位物品数据的“数据管道”；
     * Screen 是玩家在屏幕上能看到的带有按钮、背景图的“视觉窗口”；
     * 这里通过 MenuScreens.register 让游戏知道：当某个 Menu 打开时，该在玩家屏幕上绘制哪一个 Screen！
     */
    private static void registerScreens() {
        // 制冰机界面
        MenuScreens.register(
                ModMenuTypes.ICE_MAKER_MENU.get(),
                IceMakerScreen::new
        );

        // 木板箱界面
        MenuScreens.register(
                ModMenuTypes.CRATE_MENU.get(),
                CrateScreen::new
        );

        // 垃圾桶界面
        MenuScreens.register(
                ModMenuTypes.TRASH_CAN_MENU.get(),
                TrashCanScreen::new
        );

        // 尸体1 容器界面
        MenuScreens.register(
                ModMenuTypes.CORPSE1_MENU.get(),
                Corpse1Screen::new
        );

        // 尸体2 容器界面
        MenuScreens.register(
                ModMenuTypes.CORPSE2_MENU.get(),
                Corpse2Screen::new
        );
    }
}
