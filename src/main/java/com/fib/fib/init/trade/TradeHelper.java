package com.fib.fib.init.trade;

import com.fib.fib.init.Villager.ModVillager;
import com.fib.fib.util.FIBUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;

import java.util.List;

/**
 * 村民交易通用解析与注册工具类 (Trade Registration Helper)。
 * 提供表驱动 (Table-Driven) 的交易批量注册支持，统一处理物品查找、数量解析与 NBT 反序列化。
 *
 * 📋 交易数据行参数结构 (12 项)：
 * [0] 村民等级 (1-5)
 * [1] 最大交易次数
 * [2] 经验值
 * [3] 消耗品1 ID
 * [4] 消耗品1 NBT ("F" 表示无)
 * [5] 消耗品1 数量
 * [6] 消耗品2 ID ("F" 表示无)
 * [7] 消耗品2 NBT ("F" 表示无)
 * [8] 消耗品2 数量
 * [9] 产出物 ID
 * [10] 产出物 NBT ("F" 表示无)
 * [11] 产出物 数量
 */
public class TradeHelper {

    private static final int TIER_IDX = 0;
    private static final int MAX_USES_IDX = 1;
    private static final int EXP_IDX = 2;

    private static final int BUY1_ID_IDX = 3;
    private static final int BUY1_SNBT_IDX = 4;
    private static final int BUY1_COUNT_IDX = 5;

    private static final int BUY2_ID_IDX = 6;
    private static final int BUY2_SNBT_IDX = 7;
    private static final int BUY2_COUNT_IDX = 8;

    private static final int SELL1_ID_IDX = 9;
    private static final int SELL1_SNBT_IDX = 10;
    private static final int SELL1_COUNT_IDX = 11;

    private static final float PRICE_MULTIPLIER = 0.4F;

    /**
     * 将二维字符串数组注册到村民交易池中。
     *
     * @param event         Forge 村民交易事件
     * @param professionKey 职业键名（对应 ModVillager 中的常量，如 ModVillager.RS）
     * @param tradeConfigs  交易配置二维数组
     */
    public static void registerTradePool(VillagerTradesEvent event, String professionKey, String[][] tradeConfigs) {
        ModVillager.RegEntry regEntry = ModVillager.VILLAGERS.get(professionKey);
        if (regEntry == null || !event.getType().equals(regEntry.profession().get())) {
            return;
        }

        Int2ObjectMap<List<VillagerTrades.ItemListing>> tierTradePool = event.getTrades();

        for (String[] cfg : tradeConfigs) {
            int tier, maxUses, exp;
            int buy1Cnt, buy2Cnt, sell1Cnt;

            try {
                tier = Integer.parseInt(cfg[TIER_IDX]);
                maxUses = Integer.parseInt(cfg[MAX_USES_IDX]);
                exp = Integer.parseInt(cfg[EXP_IDX]);

                buy1Cnt = Integer.parseInt(cfg[BUY1_COUNT_IDX]);
                buy2Cnt = Integer.parseInt(cfg[BUY2_COUNT_IDX]);
                sell1Cnt = Integer.parseInt(cfg[SELL1_COUNT_IDX]);
            } catch (NumberFormatException e) {
                continue;
            }

            String buy1Id = cfg[BUY1_ID_IDX];
            String buy1Snbt = cfg[BUY1_SNBT_IDX];
            String buy2Id = cfg[BUY2_ID_IDX];
            String buy2Snbt = cfg[BUY2_SNBT_IDX];

            String sell1Id = cfg[SELL1_ID_IDX];
            String sell1Snbt = cfg[SELL1_SNBT_IDX];

            Item buy1Item = FIBUtils.getItemById(buy1Id).orElse(Items.AIR);
            Item sell1Item = FIBUtils.getItemById(sell1Id).orElse(Items.AIR);
            if (buy1Item == Items.AIR || sell1Item == Items.AIR) {
                continue;
            }

            List<VillagerTrades.ItemListing> pool = tierTradePool.get(tier);
            if (pool == null) {
                continue;
            }

            pool.add((trader, random) -> {
                ItemStack costA = buildStack(buy1Item, buy1Cnt, buy1Snbt);
                ItemStack costB = ItemStack.EMPTY;
                if (!"F".equalsIgnoreCase(buy2Id)) {
                    Item buy2Item = FIBUtils.getItemById(buy2Id).orElse(Items.AIR);
                    if (buy2Item != Items.AIR) {
                        costB = buildStack(buy2Item, buy2Cnt, buy2Snbt);
                    }
                }
                ItemStack reward = buildStack(sell1Item, sell1Cnt, sell1Snbt);
                return new MerchantOffer(costA, costB, reward, maxUses, exp, PRICE_MULTIPLIER);
            });
        }
    }

    /**
     * 根据物品、数量和 SNBT 字符串构造 ItemStack。
     */
    private static ItemStack buildStack(Item item, int count, String snbt) {
        ItemStack stack = new ItemStack(item, count);
        if (snbt != null && !"F".equalsIgnoreCase(snbt.trim())) {
            try {
                CompoundTag tag = NbtUtils.snbtToStructure(snbt);
                stack.setTag(tag);
            } catch (Exception ignored) {
            }
        }
        return stack;
    }
}
