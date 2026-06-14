package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class TACZ {
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

    /*
数组下标分段说明，单条交易共12个有效参数，大写F代表关闭该项功能
【0 村民等级, 1 单次刷新最大交易次数, 2 交易后村民获得经验】
【3 第一消耗物品ID, 4 第一消耗SNBT标签(F=无NBT), 5 第一消耗物品数量】
【6 第二消耗物品ID(F=无第二消耗), 7 第二消耗SNBT标签(F=无NBT), 8 第二消耗物品数量】
【9 唯一产物物品ID, 10 产物SNBT标签(F=无NBT), 11 产物物品数量】

启用/关闭规则：
1. 消耗2ID填 F → 不生成第二消耗物品栈，对应数量参数填任意数字无效
2. 任意SNBT字段填 F → 该物品不附加任何自定义NBT标签
3. SNBT字段填写合法SNBT字符串，自动给物品写入对应复合标签
4. 物品ID填写模组/原版完整注册名即可启用该物品
*/
    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(com.fib.fib.init.Villager.ModVillager.TACZ_MAKER.get())) return;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> tierTradePool = event.getTrades();

        String[][] tradeConfigs = {
                {
                        "1", "8", "1",
                        "minecraft:emerald", "F", "1",
                        "F", "F", "0",
                        "minecraft:gunpowder", "F", "8"
                },
                {
                        "1", "6", "2",
                        "minecraft:copper_ingot", "F", "1",
                        "F", "F", "0",
                        "minecraft:emerald", "F", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:quenching\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:shadowkiller\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:silencefd\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:polaris\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:thieve_sea\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:mercy\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:cobra_vanguard\"}", "1"
                },
                {
                        "2", "8", "1",
                        "minecraft:emerald", "F", "2",
                        "F", "F", "0",
                        "lrtactical:melee", "{MeleeWeaponId:\"delta_wt:treasure_kn\"}", "1"
                }
        };

        for (String[] cfg : tradeConfigs) {
            int tier, maxUses, exp;
            int buy1Cnt, buy2Cnt;
            int sell1Cnt;
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

            Item buy1Item = getItemFromID(buy1Id);
            Item sell1Item = getItemFromID(sell1Id);
            if (buy1Item == null || sell1Item == null) continue;

            List<VillagerTrades.ItemListing> pool = tierTradePool.get(tier);
            if (pool == null) continue;

            pool.add((trader, random) -> {
                ItemStack costA = buildStack(buy1Item, buy1Cnt, buy1Snbt);
                ItemStack costB = ItemStack.EMPTY;
                if (!"F".equals(buy2Id)) {
                    Item buy2Item = getItemFromID(buy2Id);
                    if (buy2Item != null) {
                        costB = buildStack(buy2Item, buy2Cnt, buy2Snbt);
                    }
                }
                ItemStack reward = buildStack(sell1Item, sell1Cnt, sell1Snbt);
                return new MerchantOffer(costA, costB, reward, maxUses, exp, PRICE_MULTIPLIER);
            });
        }
    }

    private static ItemStack buildStack(Item item, int count, String snbt) {
        ItemStack stack = new ItemStack(item, count);
        if (!"F".equals(snbt)) {
            try {
                CompoundTag tag = NbtUtils.snbtToStructure(snbt);
                stack.setTag(tag);
            } catch (Exception ignored) {}
        }
        return stack;
    }

    public static @Nullable Item getItemFromID(String itemId) {
        if ("F".equals(itemId)) return null;
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return null;
        return ForgeRegistries.ITEMS.getValue(loc);
    }
}