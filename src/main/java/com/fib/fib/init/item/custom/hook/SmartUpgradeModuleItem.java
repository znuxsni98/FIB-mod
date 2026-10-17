package com.fib.fib.init.item.custom.hook;

/**
 * 智能升级模块：解锁模式切换（普通/攀岩/拾取），额外提供一个模块槽位。
 */
public class SmartUpgradeModuleItem extends GrapplingHookUpgradeItem {
    public SmartUpgradeModuleItem(Properties properties) {
        super(properties, HookUpgradeType.FUNCTION, "fib_mod.hook_upgrade.smart_module.desc");
    }
}
