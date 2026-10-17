package com.fib.fib.init.item.custom.hook;

/**
 * 钩爪头部材质部件类。
 */
public class HookMaterialItem extends GrapplingHookUpgradeItem implements IHookMaterial {
    private final float baseDamage;
    private final float waterBonusDamage;
    private final boolean canHookBlocks;
    private final boolean canHookHardBlocks;
    private final boolean waterResistance;

    public HookMaterialItem(Properties properties, String descriptionKey,
                            float baseDamage, float waterBonusDamage,
                            boolean canHookBlocks, boolean canHookHardBlocks, boolean waterResistance) {
        super(properties, HookUpgradeType.HOOK, descriptionKey);
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
}
