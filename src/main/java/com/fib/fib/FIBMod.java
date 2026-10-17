package com.fib.fib;

import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.init.ModMenuTypes;
import com.fib.fib.init.Villager.ModVillager;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.init.effect.ModEffects;
import com.fib.fib.init.entity.ModEntities;
import com.fib.fib.init.item.ModCreativeModeTabs;
import com.fib.fib.init.item.ModItems;
import com.fib.fib.init.potion.ModPotions;
import com.fib.fib.init.recipe.ModRecipes;
import com.fib.fib.init.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * --------------------------------------------------------------------------
 * 🚀 FIB 模组主入口 (FIBMod)
 * --------------------------------------------------------------------------
 * 【初中生大白话讲解】：
 * 这个类就像是整个模组的“总司令部”。
 * 当 Minecraft 刚启动、扫描到我们这个模组时，第一个执行的就是这个类的构造函数！
 * 
 * 它的核心职责非常纯粹，只有三件事：
 * 1. 拿到“开学注册大厅”（MOD 总线 `modEventBus`）；
 * 2. 把我们所有的方块、物品、生物、音效登记表，统统递交给注册大厅；
 * 3. 注册模组通用配置文件 (Config)。
 * 
 * ⚠️【注意】：不要把具体的方块代码、按键逻辑塞到这里！主类保持精简干净是专业模组的黄金法则。
 */
@Mod(FIBMod.MOD_ID)
public class FIBMod {

    /**
     * 模组的唯一标识符 (Mod ID)。
     * 游戏中所有属于本模组的内容命名空间都是它，比如 "fib_mod:ice_maker"。
     * 必须与 resources/META-INF/mods.toml 中的 modId 保持严格一致！
     */
    public static final String MOD_ID = "fib_mod";

    /** 全局统一日志输出工具，方便在后台排查 Bug */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 调试模式开关 */
    public static final boolean DEBUG = true;

    /**
     * 模组主构造函数。
     * Minecraft 加载模组时自动实例化此方法。
     */
    public FIBMod(FMLJavaModLoadingContext context) {
        // 1. 获取 MOD 事件总线（只在游戏启动初期生效的专用注册总线）
        IEventBus modEventBus = context.getModEventBus();

        // 2. 注册通用初始化事件监听
        modEventBus.addListener(this::commonSetup);

        // 3. 把各个模块的 DeferredRegister（延迟注册表）挂载到 MOD 总线上
        // 告诉 Forge：“等开放注册时，把我这里的方块、物品一并登记入库！”
        ModItems.register(modEventBus);               // 物品
        ModBlocks.register(modEventBus);             // 方块
        ModCreativeModeTabs.register(modEventBus);   // 创造模式物品栏标签页
        ModSounds.register(modEventBus);             // 自定义音效

        ModBlockEntities.register(modEventBus);      // 方块实体（机器大脑与存储）
        ModVillager.register(modEventBus);           // 特殊村民职业与工作站
        ModMenuTypes.register(modEventBus);          // 容器菜单网络同步
        ModEntities.register(modEventBus);           // 生物与投射物实体
        ModEffects.register(modEventBus);            // 状态药水效果
        ModPotions.register(modEventBus);            // 药水配方
        ModRecipes.register(modEventBus);            // 自定义合成与机器配方类型

        // 4. 将本类注册到全局 FORGE 广播总线（用来接收诸如服务器开启等日常游戏事件）
        MinecraftForge.EVENT_BUS.register(this);

        // 5. 注册配置文件
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * 通用初始化阶段 (FMLCommonSetupEvent)。
     * 在物品和方块都注册完毕后触发，客户端与服务端都会执行。适合做跨模组通用网络包、数据准备等。
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[FIBMod] 通用生命周期初始化完成 (Common Setup Finished).");
    }

    /**
     * 服务器启动事件。
     * 当进入单人世界或物理服务器启动时触发。
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[FIBMod] 服务器环境已就绪 (Server Starting).");
    }
}
