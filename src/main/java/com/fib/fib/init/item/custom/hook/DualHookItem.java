package com.fib.fib.init.item.custom.hook;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 双钩物品：一对钩爪。
 * 放入钩爪枪的「钩爪槽」使用；配合双钩模块可发射两枚钩爪，两枚都发射后该物品被消耗。
 * 材质特性等同基础钩爪。
 */
public class DualHookItem extends Item implements IHookMaterial {
    public DualHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDamage(boolean inWater) {
        return 4.0f;
    }

    @Override
    public boolean canHookBlocks() {
        return true;
    }

    @Override
    public boolean canHookHardBlocks() {
        return false;
    }

    @Override
    public boolean hasWaterResistance() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.translatable("fib_mod.dual_hook.desc").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}
