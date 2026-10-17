package com.fib.fib.init.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 冰冻效果（移植自 26.4-snapshot-3 的 net.minecraft.world.effect.FreezingMobEffect）。
 *
 * 行为机制：
 *  - 效果本类：每 tick 累积冻结值（基础 +1，受药水等级 Amplifier 增益），无惧皮革护甲免疫
 *  - LivingEntityMixin：aiStep 阻止冻结值衰减，且满冻结受击时伤害 ×1.5 (DAMAGE_VALUE)
 *  - EntityMixin：baseTick 灭火
 */
public class FreezingMobEffect extends MobEffect {

    /** 满冻结时的伤害倍率（快照 DAMAGE_VALUE）。 */
    public static final float DAMAGE_VALUE = 1.5F;

    public FreezingMobEffect() {
        // 快照颜色 #9EEEEE
        super(MobEffectCategory.HARMFUL, 0x9EEEEE);
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        // 每 tick 增加冻结值：等级 0 (冰冻 I) 加 1，等级每高 1 级多加 1，不受皮革护甲免疫影响
        int freezeIncrement = amplifier + 1;
        int nextTicks = Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen() + freezeIncrement);
        entity.setTicksFrozen(nextTicks);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
