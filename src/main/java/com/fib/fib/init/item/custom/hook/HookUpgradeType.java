package com.fib.fib.init.item.custom.hook;

/**
 * 钩爪枪升级部件槽位类型枚举。
 */
public enum HookUpgradeType {
    /** 钩爪槽：决定钩爪的基础材质、伤害及方块判定特性 */
    HOOK("hook"),
    /** 模块槽：增强发射性能与牵引性能（如加压模块） */
    MODULE("module"),
    /** 功能槽：解锁核心功能与模式切换（如智能升级模块） */
    FUNCTION("function"),
    /** 改进槽：进阶功能强化（如双钩模块、掉落物保护模块） */
    IMPROVEMENT("improvement"),
    /** 钩爪模块槽：钩爪头部挂载的拓展功能（如挖掘模块） */
    HOOK_MODULE("hook_module");

    private final String serializedName;

    HookUpgradeType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }
}
