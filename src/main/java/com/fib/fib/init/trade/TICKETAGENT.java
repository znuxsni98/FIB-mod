package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 🎟️ 票务/银行代理人 (Ticket Agent) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：lightmanscurrency:ticket_machine（售票机，未找到时兜底为灰色羊毛）
 * 出售莱特曼货币 (Lightman's Currency) 相关交易机、ATM 卡、不同等级钱袋。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class TICKETAGENT {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "1", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:transaction_register", "F", "1"
            },
            {
                    "1", "4", "5",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:atm_card", "F", "1"
            },
            {
                    "2", "4", "40",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "lightmanscurrency:wallet_gold", "F", "1"
            },
            {
                    "3", "4", "60",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:wallet_diamond", "F", "1"
            },
            {
                    "4", "4", "100",
                    "lightmanscurrency:coin_diamond", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:wallet_netherite", "F", "1"
            },
            {
                    "5", "4", "100",
                    "lightmanscurrency:coin_netherite", "F", "1",
                    "F", "F", "0",
                    "lightmanscurrency:wallet_ender_dragon", "F", "1"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.TICKET_AGENT, TRADE_CONFIGS);
    }
}