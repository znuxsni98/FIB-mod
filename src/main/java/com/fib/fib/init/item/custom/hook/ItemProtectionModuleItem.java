package com.fib.fib.init.item.custom.hook;

/**
 * 掉落物保护模块：保护被钩取的掉落物在回收途中免受火焰、岩浆、仙人掌及虚空销毁。
 */
public class ItemProtectionModuleItem extends GrapplingHookUpgradeItem {
    public ItemProtectionModuleItem(Properties properties) {
        super(properties, HookUpgradeType.IMPROVEMENT, "fib_mod.hook_upgrade.item_protection_module.desc");
    }
}
