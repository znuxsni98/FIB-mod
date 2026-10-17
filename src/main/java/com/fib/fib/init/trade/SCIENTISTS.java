package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 🧪 科学家 (Scientist) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：fib_mod:scientists_experimental_platform（科学家实验台，未找到时兜底为淡蓝色羊毛）
 * 出售各种变异器官、血清、注射器等生化研究耗材。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class SCIENTISTS {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "16", "1",
                    "spore:claw_fragment", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_iron", "F", "3"
            },
            {
                    "1", "16", "1",
                    "spore:mutated_fiber", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_iron", "F", "3"
            },
            {
                    "1", "16", "1",
                    "spore:armor_fragment", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_iron", "F", "3"
            },
            {
                    "1", "16", "1",
                    "spore:mutated_heart", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_iron", "F", "3"
            },
            {
                    "1", "16", "2",
                    "biomancy:vial", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:volatile_fluid", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:regenerative_fluid", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:withering_ooze", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:hormone_secretion", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:bile", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:toxin_extract", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "2", "16", "3",
                    "spore:nerves", "F", "16",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "2", "16", "3",
                    "spore:wing_membrane", "F", "2",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "3", "16", "8",
                    "spore:cerebrum", "F", "2",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "3", "16", "8",
                    "spore:corrosive_sack", "F", "2",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "3", "16", "8",
                    "spore:alveolic_sack", "F", "2",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "3", "16", "8",
                    "spore:altered_spleen", "F", "2",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:ageing_serum", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:frenzy_serum", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:rejuvenation_serum", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:insomnia_serum", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:enlargement_serum", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:shrinking_serum", "F", "1"
            },
            {
                    "4", "6", "15",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:breeding_stimulant", "F", "1"
            },
            {
                    "5", "4", "30",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "biomancy:cleansing_serum", "F", "1"
            },
            {
                    "5", "4", "30",
                    "lightmanscurrency:coin_gold", "F", "8",
                    "F", "F", "0",
                    "biomancy:absorption_boost", "F", "8"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:vampiric_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:calcified_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:bezerk_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:toxic_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:rotten_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:reinforced_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:drowned_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:charred_syringe", "F", "1"
            },
            {
                    "5", "1", "30",
                    "lightmanscurrency:coin_emerald", "F", "5",
                    "F", "F", "0",
                    "spore:skeletal_syringe", "F", "1"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.SCIENTIST, TRADE_CONFIGS);
    }
}