package com.fib.fib.init.item.custom.hook;

import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 钩爪头部材质部件类。
 * 放入钩爪枪的「钩爪槽」（槽位 0）作为消耗品：发射后消耗。
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

    /** 手持钩爪右键：为另一只手的钩爪枪装填到「钩爪槽」（槽位 0）。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack thisStack = player.getItemInHand(hand);
        InteractionHand other = (hand == InteractionHand.MAIN_HAND)
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack gun = player.getItemInHand(other);

        if (!(gun.getItem() instanceof GrapplingHookGunItem)) {
            return InteractionResultHolder.pass(thisStack);
        }

        if (!level.isClientSide) {
            ItemStackHandler handler = GrapplingHookGunItem.getUpgradesHandler(gun);
            if (!handler.getStackInSlot(0).isEmpty()) {
                return InteractionResultHolder.fail(thisStack);
            }
            handler.setStackInSlot(0, thisStack.copyWithCount(1));
            GrapplingHookGunItem.saveUpgradesHandler(gun, handler);
            if (!player.getAbilities().instabuild) {
                thisStack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(thisStack, level.isClientSide);
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
