package com.fib.fib;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 模组通用配置类 (Common Config)。
 * 对应生成配置文件：config/fib_mod-common.toml，用于控制机器基础能耗与视觉特效开关。
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
