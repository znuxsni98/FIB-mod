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
 */
public class DualHookItem extends Item implements IHookMaterial {
    private final String descriptionKey;
    private final float baseDamage;
    private final float waterBonusDamage;
    private final boolean canHookBlocks;
    private final boolean canHookHardBlocks;
    private final boolean waterResistance;

    public DualHookItem(Properties properties, String descriptionKey,
                        float baseDamage, float waterBonusDamage,
                        boolean canHookBlocks, boolean canHookHardBlocks, boolean waterResistance) {
        super(properties);
        this.descriptionKey = descriptionKey;
        this.baseDamage = baseDamage;
        this.waterBonusDamage = waterBonusDamage;
        this.canHookBlocks = canHookBlocks;
        this.canHookHardBlocks = canHookHardBlocks;
        this.waterResistance = waterResistance;
    }

    @Override
    public float getDamage(boolean inWater) {
        return baseDamage + (inWater ? waterBonusDamage : 0.0f);
    }

    @Override
    public boolean canHookBlocks() {
        return canHookBlocks;
    }

    @Override
    public boolean canHookHardBlocks() {
        return canHookHardBlocks;
    }

    @Override
    public boolean hasWaterResistance() {
        return waterResistance;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}
