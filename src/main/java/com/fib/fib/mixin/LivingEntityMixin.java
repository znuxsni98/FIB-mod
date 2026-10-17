package com.fib.fib.mixin;

import com.fib.fib.init.effect.FreezingMobEffect;
import com.fib.fib.init.effect.ModEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 冻结机制增强 Mixin。
 *
 * 1. 拥有 freezing 药水效果时，视同身处细雪，阻止冻结值衰减；
 * 2. 满冻结受击时，伤害乘以 FreezingMobEffect.DAMAGE_VALUE (1.5倍)；
 * 3. 采用 MixinExtras 的 @ModifyArg 替代高侵入性 @Redirect，杜绝与 Cold Sweat / Spore / LSO 等模组的独占冲突！
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * 目标：LivingEntity.aiStep 内读取 Entity.isInPowderSnow 的那处判断。
     * 作用：拥有 freezing 效果时，视同身处细雪，从而不进入冻结值衰减分支。
     */
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInPowderSnow:Z")
    )
    private boolean fib$freezingCountsAsPowderSnow(boolean original) {
        return original || ((LivingEntity) (Object) this).hasEffect(ModEffects.FREEZING.get());
    }

    /**
     * 目标：LivingEntity.aiStep 内满冻结时造成的 freeze 伤害调用中的第二个参数 (amount)。
     * 作用：拥有 freezing 效果时，冻结伤害 × FreezingMobEffect.DAMAGE_VALUE。
     * 使用 @ModifyArg 仅修改参数，不独占 hurt() 方法调用，与所有整合包模组 100% 兼容。
     */
    @ModifyArg(
            method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    ordinal = 0),
            index = 1
    )
    private float fib$boostFreezeDamage(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModEffects.FREEZING.get())) {
            return amount * FreezingMobEffect.DAMAGE_VALUE;
        }
        return amount;
    }
}
