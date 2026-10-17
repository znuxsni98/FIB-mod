package com.fib.fib.init.entity.custom;

import com.fib.fib.init.entity.ModEntities;
import com.fib.fib.init.item.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * --------------------------------------------------------------------------
 * ❄️ 冰球投射物实体 (IceBall)
 * --------------------------------------------------------------------------
 * 【初中生大白话讲解】：
 * 你在游戏里右键扔出一个雪球、鸡蛋或末影珍珠，它们本质上都是一个“投掷物实体（ThrowableItemProjectile）”。
 * 
 * 一个投掷物的生命周期如下：
 * 1. 玩家右键：物品类 (IceBallItem) 生成这个实体，赋予一个向前的初速度；
 * 2. 飞行中：在空中画抛物线，游戏自动为它渲染物品贴图；
 * 3. 撞到生物 (onHitEntity)：造成伤害（这里是速度越快，伤害越高！）；
 * 4. 撞击落地 (onHit)：播放玻璃破碎音效、爆出冰碎屑粒子，然后销毁自身 (discard)。
 */
public class IceBall extends ThrowableItemProjectile {

    /** 原版实体反序列化或实体注册时需要的标准构造函数 */
    public IceBall(EntityType<? extends IceBall> type, Level level) {
        super(type, level);
    }

    /** 玩家或生物扔出冰球时调用的构造函数 */
    public IceBall(Level level, LivingEntity shooter) {
        super(ModEntities.ICE_BALL.get(), shooter, level);
    }

    /** 指定坐标生成冰球时调用的构造函数（比如发射器发射） */
    public IceBall(Level level, double x, double y, double z) {
        super(ModEntities.ICE_BALL.get(), x, y, z, level);
    }

    /**
     * 绑定的默认物品贴图。
     * 飞行时渲染器会用这个物品的图标画在天上。
     */
    @Override
    protected Item getDefaultItem() {
        return ModItems.ICE_BALL.get();
    }

    /**
     * 撞击碎裂时生成的粒子效果。
     */
    private ParticleOptions getParticle() {
        ItemStack stack = getItemRaw();
        return stack.isEmpty()
                ? ParticleTypes.ITEM_SNOWBALL
                : new ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    /**
     * 处理网络实体事件（客户端接收）。
     * id == 3 是 Minecraft 原版约定的“投掷物撞击爆炸特效”事件代号。
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particle = getParticle();
            for (int i = 0; i < 8; i++) {
                level().addParticle(particle, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    /**
     * 命中活体生物时的伤害计算逻辑。
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        
        // 伤害与投射物撞击时的速度成正比：速度越大，砸得越疼！
        float speed = (float) getDeltaMovement().length();
        int damage = Mth.ceil(Mth.clamp(speed * 1.395F, 0.0F, (float) Integer.MAX_VALUE));
        
        // 使用投掷物伤害源扣除目标血量
        target.hurt(damageSources().thrown(this, getOwner()), (float) damage);
    }

    /**
     * 命中任何东西（方块或生物）时的结算逻辑。
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            // 广播粒子事件给周围的客户端
            level().broadcastEntityEvent(this, (byte) 3);
            // 播放玻璃碎裂声音
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.5F,
                    0.4F / (level().getRandom().nextFloat() * 0.4F + 0.8F));
            // 命中后从世界上清除自身
            discard();
        }
    }
}
