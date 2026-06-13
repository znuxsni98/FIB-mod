package com.fib.fib.init.trade;

import com.fib.fib.FIBMod;
import com.fib.fib.init.Villager.ModVillager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

//如梦用豆包写的数据结构
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID)
public class TACZ {

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(ModVillager.TACZ_MAKER.get())) return;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> tierTradePool = event.getTrades();

        String[][] tradeConfigs = {
                {"1", "tacz:ammo", "{AmmoId:\"tacz_unidict:pistol\"}", "2", "4", "1", "true", "false"},
                {"1", "tacz:ammo", "{AmmoId:\"tacz_unidict:pistol\"}", "2", "4", "1", "true", "true"},
                {"2", "tacz:ammo", "{AmmoId:\"tacz_unidict:pistol\"}", "2", "8", "1", "true", "false"},

        };

        for (String[] config : tradeConfigs) {
            int tier, valueNum, maxUses, exp;
            try {
                tier = Integer.parseInt(config[TIER_IDX]);
                valueNum = Integer.parseInt(config[TRADE_VALUE_IDX]);
                maxUses = Integer.parseInt(config[MAX_USES_IDX]);
                exp = Integer.parseInt(config[EXP_REWARD_IDX]);
            } catch (NumberFormatException e) {
                continue;
            }

            boolean addAttachNbt = Boolean.parseBoolean(config[NEED_ATTACH_NBT_IDX]);
            boolean isReverseTrade = Boolean.parseBoolean(config[IS_REVERSE_TRADE_IDX]);
            String baseItemId = config[BASE_ITEM_IDX];
            String attachNbtData = config[ATTACH_NBT_IDX];

            Item tradeBaseItem = getItemFromID(baseItemId);
            if (tradeBaseItem == null) {
                continue;
            }

            List<VillagerTrades.ItemListing> tradePool = tierTradePool.get(tier);
            if (tradePool == null) continue;

            tradePool.add((trader, random) -> {
                ItemStack emeraldCost = new ItemStack(Items.EMERALD, valueNum);
                ItemStack itemStack = buildTradeItem(tradeBaseItem, attachNbtData, addAttachNbt);

                ItemStack costA, rewardItem;
                if (!isReverseTrade) {
                    costA = emeraldCost;
                    rewardItem = itemStack;
                } else {
                    costA = itemStack;
                    rewardItem = emeraldCost;
                }
                return new MerchantOffer(costA, ItemStack.EMPTY, rewardItem, maxUses, exp, PRICE_MULTIPLIER);
            });
        }
    }






    private static final int TIER_IDX = 0;
    private static final int BASE_ITEM_IDX = 1;
    private static final int ATTACH_NBT_IDX = 2;
    private static final int TRADE_VALUE_IDX = 3;
    private static final int MAX_USES_IDX = 4;
    private static final int EXP_REWARD_IDX = 5;
    private static final int NEED_ATTACH_NBT_IDX = 6;
    private static final int IS_REVERSE_TRADE_IDX = 7;

    private static final float PRICE_MULTIPLIER = 0.4F;

    public static @Nullable Item getItemFromID(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) return null;
        return ForgeRegistries.ITEMS.getValue(location);
    }

    private static ItemStack buildTradeItem(Item baseItem, String fullNbtStr, boolean addAttachNbt) {
        ItemStack stack = new ItemStack(baseItem);
        if (addAttachNbt && !"none".equals(fullNbtStr)) {
            try {
                CompoundTag customTag = NbtUtils.snbtToStructure(fullNbtStr);
                stack.setTag(customTag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return stack;
    }
}