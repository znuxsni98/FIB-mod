package com.fib.fib.init.item.custom.hook;

/**
 * 双钩模块：允许发射第二枚钩爪，在两枚钩爪间建立最长 20 格的拴绳连接。
 */
public class DualHookModuleItem extends GrapplingHookUpgradeItem {
    public DualHookModuleItem(Properties properties) {
        super(properties, HookUpgradeType.IMPROVEMENT, "fib_mod.hook_upgrade.dual_hook_module.desc");
    }
}
