package com.fib.fib.init.item.custom.hook;

/**
 * 加压模块：提升钩爪发射速度与射程，但翻倍能量消耗。
 */
public class PressurizedModuleItem extends GrapplingHookUpgradeItem {
    public PressurizedModuleItem(Properties properties) {
        super(properties, HookUpgradeType.MODULE, "fib_mod.hook_upgrade.pressurized_module.desc");
    }
}
