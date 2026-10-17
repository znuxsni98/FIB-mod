package com.fib.fib.mixin;

import com.fib.fib.init.effect.ModEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 对齐 26.4-snapshot-3 对原版 LivingEntity 的冻结改动（方案 A）。
 *
 * 快照把「身处细雪才不衰减冻结值」改成「身处细雪 或 拥有 freezing 效果」都不衰减，
 * 并让满冻结伤害在拥有 freezing 效果时变为 1.5 倍。这里用 Mixin 复刻。
 *
 * 冻结值的累积由 FreezingMobEffect 每 tick 负责（对应快照的 freezeForTicks(1)），
 * 本 Mixin 只负责阻止衰减。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * 目标：LivingEntity.aiStep 内读取 Entity.isInPowderSnow 的那处判断
     * （字节码中 Fieldref owner 为 LivingEntity，故此处以 LivingEntity 指定）。
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
     * 目标：LivingEntity.aiStep 内满冻结时造成的 freeze 伤害调用。
     * 作用：拥有 freezing 效果时，冻结伤害 ×1.5（与原版 1.0 相对）。
     */
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    ordinal = 0)
    )
    private boolean fib$boostFreezeDamage(LivingEntity self, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FREEZING)
                && self.hasEffect(ModEffects.FREEZING.get())) {
            amount *= 1.5F;
        }
        return self.hurt(source, amount);
    }
}
