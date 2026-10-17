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
 * 钩爪枪升级模块基类。
 */
public class GrapplingHookUpgradeItem extends Item {
    private final HookUpgradeType upgradeType;
    private final String descriptionKey;

    public GrapplingHookUpgradeItem(Properties properties, HookUpgradeType upgradeType, String descriptionKey) {
        super(properties);
        this.upgradeType = upgradeType;
        this.descriptionKey = descriptionKey;
    }

    public HookUpgradeType getUpgradeType() {
        return upgradeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        String typeTranslation = switch (upgradeType) {
            case HOOK -> "fib_mod.hook_upgrade.type.hook";
            case MODULE -> "fib_mod.hook_upgrade.type.module";
            case FUNCTION -> "fib_mod.hook_upgrade.type.function";
            case IMPROVEMENT -> "fib_mod.hook_upgrade.type.improvement";
            case HOOK_MODULE -> "fib_mod.hook_upgrade.type.hook_module";
        };
        tooltip.add(Component.translatable(typeTranslation).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}
