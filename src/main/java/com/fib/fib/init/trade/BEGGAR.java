package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 🥣 乞丐 (Beggar) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：fib_mod:bowl_block（破碗，未找到时兜底为黄色羊毛）
 * 收取食物和腐败物，兑换腐质。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class BEGGAR {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "64", "1",
                    "minecraft:potato", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "1", "64", "1",
                    "fib:putrid_substance", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "1", "64", "1",
                    "farmersdelight:tomato", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "1", "64", "1",
                    "minecraft:rotten_flesh", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "2", "128", "1",
                    "minecraft:melon_slice", "F", "2",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "2", "64", "2",
                    "minecraft:sweet_berries", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "2", "32", "4",
                    "minecraft:apple", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "3"
            },
            {
                    "2", "32", "4",
                    "minecraft:carrot", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "2"
            },
            {
                    "3", "8", "16",
                    "spore:mutated_fiber", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "6"
            },
            {
                    "3", "16", "8",
                    "spore:biomass", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "4"
            },
            {
                    "3", "16", "8",
                    "spore:biomass_bacon", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "8"
            },
            {
                    "4", "32", "16",
                    "spore:mycelium_veins", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "1"
            },
            {
                    "4", "32", "16",
                    "spore:bloomfung2", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "4"
            },
            {
                    "4", "32", "16",
                    "spore:growth_mycelium", "F", "1",
                    "F", "F", "0",
                    "fib:putrid_substance", "F", "2"
            },
            {
                    "5", "128", "8",
                    "biomancy:nutrient_paste", "F", "1",
                    "fib:putrid_substance", "F", "1",
                    "fib:putrid_substance", "F", "3"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.BEGGAR, TRADE_CONFIGS);
    }
}