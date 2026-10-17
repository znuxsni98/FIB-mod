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
 * 冰球手持物品类 (Ice Ball Item)。
 * 支持右键发射 IceBall 投射物实体。
 */
public class IceBallItem extends Item {

    /** 投掷力度初速度（决定能扔多远、砸多狠） */
    public static final float PROJECTILE_SHOOT_POWER = 1.5F;

    public IceBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 播放投掷音效
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        // ⚠️【核心规则】：实体的生成必须在服务端进行！
        // 客户端只是接收服务端的同步包。如果客户端也生成一个实体，就会出现假实体幽灵！
        if (!level.isClientSide) {
            IceBall ball = new IceBall(level, player);
            ball.setItem(stack);
            // 按照玩家的视线角度（Pitch 俯仰角，Yaw 偏航角）发射
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    PROJECTILE_SHOOT_POWER, 1.0F);
            level.addFreshEntity(ball);
        }

        // 记录统计数据
        player.awardStat(Stats.ITEM_USED.get(this));

        // 非创造模式消耗 1 个物品
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
