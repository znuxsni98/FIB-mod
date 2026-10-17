package com.fib.fib.init.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 冰冻效果（移植自 26.4-snapshot-3 的 net.minecraft.world.effect.FreezingMobEffect）。
 *
 * 快照行为：每 tick 累积 1 点冻结值（freezeForTicks(1)）、冻结值不衰减、满冻结伤害 ×1.5、像细雪一样灭火。
 * 这些均由 Mixin 让实体「视同身处细雪 / 拥有细雪效果」实现：
 *  - LivingEntityMixin：aiStep 的冻结累积与不衰减、满冻结伤害 ×1.5
 *  - EntityMixin：baseTick 的灭火
 * 故本效果类仅作标记与颜色/分类定义。
 */
public class FreezingMobEffect extends MobEffect {

    /** 满冻结时的伤害倍率（快照 DAMAGE_VALUE）。 */
    public static final float DAMAGE_VALUE = 1.5F;

    public FreezingMobEffect() {
        // 快照颜色 #9EEEEE
        super(MobEffectCategory.HARMFUL, 0x9EEEEE);
    }
}
