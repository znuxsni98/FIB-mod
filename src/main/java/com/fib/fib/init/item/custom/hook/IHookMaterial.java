package com.fib.fib.init.item.custom.hook;

/**
 * 钩爪头部材质特性接口。
 */
public interface IHookMaterial {
    /** 基础伤害 */
    float getDamage(boolean inWater);

    /** 是否能够锚定常规方块 */
    boolean canHookBlocks();

    /** 是否能够锚定高硬度方块（如黑曜石、哭泣的黑曜石、远古残骸） */
    boolean canHookHardBlocks();

    /** 在水中是否保持速度甚至获得加速（不受流体阻力衰减） */
    boolean hasWaterResistance();
}
