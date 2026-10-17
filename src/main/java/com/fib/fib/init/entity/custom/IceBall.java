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
 * 冰球投射物（移植自 26.4-snapshot-3 的 net.minecraft.world.entity.projectile.throwableitemprojectile.IceBall）。
 *
 * 伤害 = ceil(速度 × 1.395)，命中播放碎裂音效。
 */
public class IceBall extends ThrowableItemProjectile {

    public IceBall(EntityType<? extends IceBall> type, Level level) {
        super(type, level);
    }

    public IceBall(Level level, LivingEntity shooter) {
        super(ModEntities.ICE_BALL.get(), shooter, level);
    }

    public IceBall(Level level, double x, double y, double z) {
        super(ModEntities.ICE_BALL.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ICE_BALL.get();
    }

    private ParticleOptions getParticle() {
        ItemStack stack = getItemRaw();
        return stack.isEmpty()
                ? ParticleTypes.ITEM_SNOWBALL
                : new ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particle = getParticle();
            for (int i = 0; i < 8; i++) {
                level().addParticle(particle, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        float speed = (float) getDeltaMovement().length();
        int damage = Mth.ceil(Mth.clamp(speed * 1.395F, 0.0F, (float) Integer.MAX_VALUE));
        target.hurt(damageSources().thrown(this, getOwner()), (float) damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.5F,
                    0.4F / (level().getRandom().nextFloat() * 0.4F + 0.8F));
            discard();
        }
    }
}
