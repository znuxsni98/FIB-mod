package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 💼 精致存储 (Refined Storage) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：refinedstorage:controller（未安装时兜底为橙色羊毛）
 * 如果要新增或修改交易，直接在下方的表格里加一行或改数字即可！
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class RS {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "12", "2",
                    "lightmanscurrency:coin_iron", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:processor_binding", "F", "8"
            },
            {
                    "1", "12", "3",
                    "refinedstorage:silicon", "F", "16",
                    "F", "F", "0",
                    "lightmanscurrency:coin_iron", "F", "1"
            },
            {
                    "2", "8", "5",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "refinedstorage:destruction_core", "F", "3"
            },
            {
                    "2", "8", "5",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "refinedstorage:construction_core", "F", "3"
            },
            {
                    "2", "8", "5",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "refinedstorage:cable", "F", "16"
            },
            {
                    "3", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:importer", "F", "1"
            },
            {
                    "3", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:exporter", "F", "1"
            },
            {
                    "3", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:external_storage", "F", "1"
            },
            {
                    "3", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:constructor", "F", "1"
            },
            {
                    "3", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:destructor", "F", "1"
            },
            {
                    "3", "16", "8",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "refinedstorage:pattern", "F", "8"
            },
            {
                    "3", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "refinedstorage:filter", "F", "8"
            },
            {
                    "4", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:speed_upgrade", "F", "2"
            },
            {
                    "4", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:range_upgrade", "F", "2"
            },
            {
                    "4", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:crafting_upgrade", "F", "2"
            },
            {
                    "4", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:silk_touch_upgrade", "F", "2"
            },
            {
                    "4", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:fortune_3_upgrade", "F", "2"
            },
            {
                    "4", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:regulator_upgrade", "F", "2"
            },
            {
                    "5", "8", "20",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "refinedstorage:1k_storage_disk", "F", "1"
            },
            {
                    "5", "8", "20",
                    "lightmanscurrency:coin_emerald", "F", "3",
                    "F", "F", "0",
                    "refinedstorage:4k_storage_disk", "F", "1"
            },
            {
                    "5", "8", "20",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "refinedstorage:stack_upgrade", "F", "1"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.RS, TRADE_CONFIGS);
    }
}