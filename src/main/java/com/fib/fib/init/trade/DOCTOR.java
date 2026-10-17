package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 🩺 医生 (Doctor) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：spore:surgery_table（手术台，未安装时兜底为黄绿色羊毛）
 * 出售绷带、止痛药、草药、血包、吗啡等医疗用品。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class DOCTOR {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "8", "2",
                    "lightmanscurrency:coin_iron", "F", "5",
                    "F", "F", "0",
                    "majruszsdifficulty:bandage", "F", "4"
            },
            {
                    "1", "8", "2",
                    "lightmanscurrency:coin_iron", "F", "5",
                    "F", "F", "0",
                    "legendarysurvivaloverhaul:plaster", "F", "8"
            },
            {
                    "1", "8", "2",
                    "lightmanscurrency:coin_iron", "F", "5",
                    "F", "F", "0",
                    "lrtactical:consumable", "{ConsumableId:\"lrtactical:ibuprofen\"}", "1"
            },
            {
                    "2", "8", "20",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "legendarysurvivaloverhaul:healing_herbs", "F", "1"
            },
            {
                    "2", "24", "20",
                    "legendarysurvivaloverhaul:healing_herbs", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "3", "4", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "majruszsdifficulty:golden_bandage", "F", "16"
            },
            {
                    "3", "4", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "lrtactical:consumable", "{ConsumableId:\"lrtactical:blood_pack\"}", "2"
            },
            {
                    "3", "4", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "legendarysurvivaloverhaul:bandage", "F", "3"
            },
            {
                    "4", "6", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "legendarysurvivaloverhaul:tonic", "F", "1"
            },
            {
                    "4", "6", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "legendarysurvivaloverhaul:medkit", "F", "1"
            },
            {
                    "5", "3", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "legendarysurvivaloverhaul:morphine", "F", "1"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.DOCTOR, TRADE_CONFIGS);
    }
}