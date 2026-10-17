package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 🥩 血肉学者 (Flesh & Blood Scholar) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：biomancy:primordial_cradle（原生摇篮，未找到时兜底为粉色羊毛）
 * 出售生化学 (Biomancy) 相关的器官、骨髓、爪牙、血肉机械等。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class FLESHANDBLOODSCHOLAR {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "16", "2",
                    "biomancy:mob_marrow", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:mob_sinew", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:mob_fang", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:flesh_bits", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:mob_claw", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "1", "16", "2",
                    "biomancy:mob_gland", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "2", "16", "40",
                    "biomancy:withered_mob_marrow", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "2"
            },
            {
                    "2", "1", "60",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:injector", "F", "1"
            },
            {
                    "2", "1", "60",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:extractor", "F", "1"
            },
            {
                    "3", "8", "20",
                    "biomancy:essence", "{colors:[I;44975,4602533],essence_data:{entity_type:\"minecraft:player\",name:\"entity.minecraft.player\"},essence_tier:1,sounds:{death:\"minecraft:entity.player.death\",hurt:\"minecraft:entity.player.hurt\"}}", "5",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "4"
            },
            {
                    "3", "4", "10",
                    "lightmanscurrency:coin_gold", "F", "2",
                    "F", "F", "0",
                    "biomancy:living_flesh", "F", "3"
            },
            {
                    "4", "32", "5",
                    "biomancy:malignant_flesh_veins", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "4", "32", "5",
                    "biomancy:malignant_flesh", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "4", "8", "30",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "biomancy:primal_bloom", "F", "1"
            },
            {
                    "4", "8", "30",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:digester", "F", "1"
            },
            {
                    "4", "8", "30",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:decomposer", "F", "1"
            },
            {
                    "5", "16", "5",
                    "biomancy:bloomberry", "F", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_emerald", "F", "1"
            },
            {
                    "5", "4", "30",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:biometric_membrane", "{BlockEntityTag:{id:\"biomancy:biometric_membrane\",membrane:{entity_colors:[I;44975,4602533],entity_type:\"minecraft:player\"}}}", "16"
            },
            {
                    "5", "6", "30",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:fertilizer", "F", "2"
            },
            {
                    "5", "8", "30",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "biomancy:fleshkin_pressure_plate", "F", "4"
            },
            {
                    "5", "8", "30",
                    "lightmanscurrency:coin_gold", "F", "5",
                    "F", "F", "0",
                    "biomancy:fleshkin_chest", "F", "1"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.FLESH_SCHOLAR, TRADE_CONFIGS);
    }
}