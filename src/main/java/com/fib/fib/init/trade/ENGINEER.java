package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * ⚙️ 工程师 (Engineer) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：fib_mod:engineer_workbench（工程师工作台，未找到时兜底为品红色羊毛）
 * 出售机械动力 (Create) 相关零件、扳手、齿轮箱、烈焰蛋糕等。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class ENGINEER {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "4", "2",
                    "lightmanscurrency:coin_iron", "F", "5",
                    "F", "F", "0",
                    "create:shaft", "F", "8"
            },
            {
                    "1", "2", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "createdieselgenerators:hammer", "F", "1"
            },
            {
                    "1", "2", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "createdieselgenerators:wire_cutters", "F", "1"
            },
            {
                    "2", "3", "20",
                    "create:goggles", "F", "3",
                    "F", "F", "0",
                    "createdieselgenerators:wire_cutters", "F", "1"
            },
            {
                    "2", "3", "20",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "create:wrench", "F", "1"
            },
            {
                    "3", "8", "8",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "create:cardboard", "F", "16"
            },
            {
                    "3", "8", "8",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "create:andesite_alloy", "F", "8"
            },
            {
                    "3", "8", "8",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "create:polished_rose_quartz", "F", "1"
            },
            {
                    "4", "12", "10",
                    "create:builders_tea", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "3"
            },
            {
                    "4", "12", "10",
                    "create:blaze_cake", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "2"
            },
            {
                    "4", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "create:item_vault", "F", "2"
            },
            {
                    "5", "16", "8",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "create_connected:brass_gearbox", "F", "4"
            },
            {
                    "5", "16", "8",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "create_connected:six_way_gearbox", "F", "5"
            },
            {
                    "5", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "2",
                    "F", "F", "0",
                    "create:gearbox", "F", "1"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.ENGINEER, TRADE_CONFIGS);
    }
}