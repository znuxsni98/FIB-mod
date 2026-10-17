package com.fib.fib.init.item.custom;

import com.fib.fib.init.entity.custom.IceBall;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 冰球物品（移植自 26.4-snapshot-3 的 net.minecraft.world.item.IceBallItem）。
 * 右键投掷冰球投射物。
 */
public class IceBallItem extends Item {

    /** 投掷力度（快照 IceBallItem 的 PROJECTILE_SHOOT_POWER）。 */
    public static final float PROJECTILE_SHOOT_POWER = 1.5F;

    public IceBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            IceBall ball = new IceBall(level, player);
            ball.setItem(stack);
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    PROJECTILE_SHOOT_POWER, 1.0F);
            level.addFreshEntity(ball);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
