package com.fib.fib;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * --------------------------------------------------------------------------
 * ⚙️ 模组通用配置文件 (Config)
 * --------------------------------------------------------------------------
 * 【初中生大白话讲解】：
 * 很多玩家喜欢按自己的喜好调参数（比如：“这个机器耗电太快了，能不能改成 50 FE/tick？”）。
 * Forge 提供了这个机制，它会在 config/fib_mod-common.toml 生成一个文本文件，
 * 玩家用记事本修改里面的数字，不用重新编译模组就能生效！
 * 
 * 配置文件分三类：
 * 1. COMMON（通用）：服务端和客户端共用，比如机器能耗、掉落率。
 * 2. CLIENT（客户端）：只有自己电脑生效，比如特效开关、UI 界面位置。
 * 3. SERVER（单世界专属）：保存在每个世界的 serverconfig 文件夹里。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==================== 机器参数配置 ====================
    public static final ForgeConfigSpec.IntValue ICE_MAKER_BASE_ENERGY_USAGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIMENSIONAL_CRACK_PARTICLES;

    static {
        BUILDER.push("IceMaker");
        ICE_MAKER_BASE_ENERGY_USAGE = BUILDER
                .comment("制冰机每 tick 基础消耗的能量 (FE/tick)，默认为 20")
                .defineInRange("baseEnergyUsage", 20, 1, 10000);
        BUILDER.pop();

        BUILDER.push("Visuals");
        ENABLE_DIMENSIONAL_CRACK_PARTICLES = BUILDER
                .comment("是否启用维度裂缝的粒子与光效")
                .define("enableCrackVisuals", true);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 当配置文件首次加载或重新加载时触发
        FIBMod.LOGGER.debug("[FIBMod] 配置文件加载完成。");
    }
}
