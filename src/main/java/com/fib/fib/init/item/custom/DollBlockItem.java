package com.fib.fib.init.item.custom;

import com.fib.fib.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 如梦玩偶的物品形式。
 *
 * 物品携带 NBT（BlockEntityTag），放置时原版会自动把这份 NBT 写入方块实体。
 * NBT 字段与 RuMengDollBlockEntity 保持一致：
 *   preset、skin、name、description
 */
public class DollBlockItem extends BlockItem {

    public DollBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    /**
     * 创建一个携带指定预设 NBT 的玩偶物品。
     *
     * @param block        玩偶方块
     * @param preset       预设 id
     * @param skin         皮肤贴图资源路径（如 "fib_mod:block/ru_meng_doll"）
     * @param name         物品名称
     * @param description  物品描述
     */
    public static ItemStack createPreset(Block block, String preset, String skin, String name, String description) {
        ItemStack stack = new ItemStack(block);
        CompoundTag tag = new CompoundTag();
        tag.putString("preset", preset);
        tag.putString("skin", skin);
        tag.putString("name", name);
        tag.putString("description", description);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.RU_MENG_DOLL_BE.get(), tag);
        return stack;
    }

    /**
     * 物品显示名直接使用 NBT 里预设的名称。
     */
    @Override
    public Component getName(ItemStack pStack) {
        CompoundTag tag = getPresetTag(pStack);
        if (tag != null && tag.contains("name") && !tag.getString("name").isEmpty()) {
            return Component.literal(tag.getString("name"));
        }
        return super.getName(pStack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        CompoundTag tag = getPresetTag(pStack);
        if (tag != null) {
            if (tag.contains("description") && !tag.getString("description").isEmpty()) {
                pTooltipComponents.add(Component.literal(tag.getString("description")).withStyle(ChatFormatting.GRAY));
            }
            if (tag.contains("preset") && !tag.getString("preset").isEmpty()) {
                pTooltipComponents.add(Component.translatable("item.fib_mod.ru_meng_doll.preset", tag.getString("preset"))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Nullable
    private static CompoundTag getPresetTag(ItemStack stack) {
        return BlockItem.getBlockEntityData(stack);
    }
}
