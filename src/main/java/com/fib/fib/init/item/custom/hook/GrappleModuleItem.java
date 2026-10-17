package com.fib.fib.init.item.custom.hook;

/**
 * 抓取模块：安装于钩爪模块槽，支持 G 键在【稳固模式】与【拉向模式】之间切换。
 * 1. 稳固模式：自身固定不动，最大化将目标拉向玩家，遇不可拉取目标（如方块）碰撞即收回。
 * 2. 拉向模式：除碰撞箱极小的实体外，其余情况下一律将玩家高速拉向目标。
 */
public class GrappleModuleItem extends GrapplingHookUpgradeItem {
    public GrappleModuleItem(Properties properties) {
        super(properties, HookUpgradeType.HOOK_MODULE, "fib_mod.hook_upgrade.grapple_module.desc");
    }
}
