package com.fib.fib.init.item.custom.hook;

/**
 * 挖掘模块：解锁远程挖掘功能与内部 8 格掉落物暂存仓，三态模式切换（关 / 全挖 / 仅破坏）。
 */
public class MiningModuleItem extends GrapplingHookUpgradeItem {
    public MiningModuleItem(Properties properties) {
        super(properties, HookUpgradeType.HOOK_MODULE, "fib_mod.hook_upgrade.mining_module.desc");
    }
}
