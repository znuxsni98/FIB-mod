package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * --------------------------------------------------------------------------
 * 🔫 枪械军火商 (TACZ) 商人交易池
 * --------------------------------------------------------------------------
 * 【初学者说明】：
 * 对应工作站：tacz:gun_smith_table（枪械工作台，未找到时兜底为白色羊毛）
 * 出售火药、战术近战武器、投掷物、各类型通用弹药及弹药回收。
 * 详见 {@link TradeHelper} 的 12 个参数规范。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class TACZ {

    private static final String[][] TRADE_CONFIGS = {
            // [0]等级, [1]最大交易次数, [2]经验, [3]买1, [4]NBT, [5]买1数量, [6]买2, [7]NBT, [8]买2数量, [9]卖, [10]NBT, [11]卖数量
            {
                    "1", "8", "2",
                    "lightmanscurrency:coin_iron", "F", "5",
                    "F", "F", "0",
                    "minecraft:gunpowder", "F", "8"
            },
            {
                    "1", "6", "2",
                    "minecraft:copper_ingot", "F", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_iron", "F", "5"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:quenching\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:shadowkiller\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:silencefd\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:polaris\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:thieve_sea\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:mercy\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:cobra_vanguard\"}", "1"
            },
            {
                    "2", "1", "40",
                    "lightmanscurrency:coin_emerald", "F", "1",
                    "F", "F", "0",
                    "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:treasure_kn\"}", "1"
            },
            {
                    "3", "6", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "lrtactical:throwable", "{ThrowableId:\"lrtactical:rgn\"}", "3"
            },
            {
                    "3", "6", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "lrtactical:throwable", "{ThrowableId:\"lrtactical:m67\"}", "3"
            },
            {
                    "3", "8", "8",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "fib:gunpowder", "F", "16"
            },
            {
                    "4", "12", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:rifle\"}", "8"
            },
            {
                    "4", "16", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:pistol\"}", "8"
            },
            {
                    "4", "16", "10",
                    "lightmanscurrency:coin_gold", "F", "1",
                    "F", "F", "0",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:shot\"}", "4"
            },
            {
                    "4", "16", "10",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:rifle\"}", "24",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "4", "16", "10",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:pistol\"}", "24",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "4", "16", "10",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:shot\"}", "12",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "1"
            },
            {
                    "5", "12", "20",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:fuel_tank\"}", "1"
            },
            {
                    "5", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:barrel\"}", "1"
            },
            {
                    "5", "16", "20",
                    "lightmanscurrency:coin_gold", "F", "3",
                    "F", "F", "0",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:sniper\"}", "2"
            },
            {
                    "5", "16", "20",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:fuel_tank\"}", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "2"
            },
            {
                    "5", "16", "20",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:barrel\"}", "4",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "2"
            },
            {
                    "5", "16", "20",
                    "tacz:ammo", "{AmmoId:\"tacz_unidict:sniper\"}", "8",
                    "F", "F", "0",
                    "lightmanscurrency:coin_gold", "F", "2"
            }
    };

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        TradeHelper.registerTradePool(event, ModVillager.TACZ, TRADE_CONFIGS);
    }
}