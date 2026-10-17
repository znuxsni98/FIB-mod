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
 * 客户端专属注册中心 (Client Setup)。
 * 仅在物理客户端 (Dist.CLIENT) 加载，集中负责 MenuScreens、EntityRenderers 与 BlockEntityRenderers 的注册绑定。
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

        // 钩爪实体渲染器
        event.registerEntityRenderer(
                ModEntities.GRAPPLING_HOOK.get(),
                com.fib.fib.client.render.entity.GrapplingHookRenderer::new
        );
    }

    /**
     * 注册 HUD 叠加层。
     */
    @SubscribeEvent
    public static void onRegisterOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id(),
                "grappling_hook_hud", com.fib.fib.client.render.GrapplingHookOverlay.HUD);
    }

    /**
     * 注册物品栏能量双条渲染器。
     */
    @SubscribeEvent
    public static void onRegisterItemDecorations(net.minecraftforge.client.event.RegisterItemDecorationsEvent event) {
        event.register(com.fib.fib.init.item.ModItems.GRAPPLING_HOOK_GUN.get(),
                new com.fib.fib.client.render.GrapplingHookEnergyBarDecorator());
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

        // 钩爪枪改装界面
        MenuScreens.register(
                ModMenuTypes.GRAPPLING_HOOK_MENU.get(),
                com.fib.fib.gui.container.screen.GrapplingHookScreen::new
        );
    }
}
