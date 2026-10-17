package com.fib.fib.mixin;

import com.fib.fib.init.effect.ModEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让拥有 freezing 效果的实体在灭火判定中等同身处细雪。
 *
 * 原版 Entity.baseTick 用 Entity.isInPowderSnow 判定灭火（音效 + 清除火焰）。
 * 这里拦截 baseTick 中所有对 isInPowderSnow 的读取（GETFIELD），
 * 在拥有 freezing 效果时视为 true，从而立即灭火。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyExpressionValue(
            method = "baseTick",
            at = @At(value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/world/entity/Entity;isInPowderSnow:Z")
    )
    private boolean fib$freezingAsPowderSnow(boolean original) {
        if (original) {
            return true;
        }
        return (Object) this instanceof LivingEntity living
                && living.hasEffect(ModEffects.FREEZING.get());
    }
}
