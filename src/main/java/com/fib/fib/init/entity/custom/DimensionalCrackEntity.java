package com.fib.fib.init.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class DimensionalCrackEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(DimensionalCrackEntity.class, EntityDataSerializers.INT);

    /** 碰撞箱厚度（格）—— 沿朝向方向。宽/高由 EntityType 的 sized(2, 3) 给出 */
    private static final float THICKNESS = 0.5F;

    /** 存在时间（tick），用于控制开启动画进度 */
    private int age = 0;
    /** 是否已完全开启（客户端也可据此切换渲染状态） */
    private boolean fullyOpened = false;

    public DimensionalCrackEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;          // 无碰撞
        this.noCulling = false;
    }

    /**
     * 碰撞箱：宽 2、高 3、<b>厚度 0.5</b>，与扁平的裂缝视觉体一致。
     * <p>{@code EntityType.Builder#sized} 只能给出"宽 = 深"的立方体盒，厚度做不出来，所以整个盒子在这里重算。
     * 宽高仍然取自 EntityType 的尺寸（保持单一来源），只有厚度是额外定死的。
     * <p>水平范围取旋转后的矩形在 X/Z 上的投影，即轴对齐包围盒：比真实形状略大，用于剔除和拾取足够。
     */
    @Override
    protected AABB makeBoundingBox() {
        EntityDimensions dim = this.getDimensions(this.getPose());

        float yawRad = -this.getYRot() * Mth.DEG_TO_RAD;
        // 局部 X 轴（宽度方向）与 Z 轴（法线/厚度方向）在世界中的投影
        float rightX = Mth.cos(yawRad);
        float rightZ = -Mth.sin(yawRad);
        float normX = Mth.sin(yawRad);
        float normZ = Mth.cos(yawRad);

        double ex = Math.abs(rightX) * (dim.width * 0.5D) + Math.abs(normX) * (THICKNESS * 0.5D);
        double ez = Math.abs(rightZ) * (dim.width * 0.5D) + Math.abs(normZ) * (THICKNESS * 0.5D);

        double px = this.getX();
        double py = this.getY();
        double pz = this.getZ();

        return new AABB(px - ex, py, pz - ez, px + ex, py + dim.height, pz + ez);
    }

    /**
     * 渲染出来的裂缝是一个 3.8 × 5.7 格的四边形（外圈还要再算上气焰和水波纹），比碰撞箱大得多，
     * 而 EntityRenderer#shouldRender 用的是这个盒子。
     * 不放大盒子的话，贴着裂缝看边缘时碰撞箱会先离开视锥，整条裂缝会毫无征兆地消失。
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.5D, 2.1D, 1.5D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            age++;
            if (age >= 40 && !fullyOpened) {
                fullyOpened = true;
            }
            if (age <= 50 || age % 20 == 0) {
                this.entityData.set(DATA_AGE, age);
            }
        } else {
            age = this.entityData.get(DATA_AGE);
        }
    }

    /** 锁定初始方向，不接受外部旋转 */
    @Override
    public void setYRot(float yRot) {
        // 方向在生成时设置一次，之后不再改变
        if (this.tickCount > 0) return;
        super.setYRot(yRot);
        // 碰撞箱是按朝向算出来的，朝向变了必须跟着刷新（Entity.setYRot 本身不会重算盒子）
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.fullyOpened = tag.getBoolean("FullyOpened");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putBoolean("FullyOpened", fullyOpened);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // ===== 公共 getter =====
    public int getAge() {
        return age;
    }

    public boolean isFullyOpened() {
        return fullyOpened;
    }

    /** 开启动画进度 0.0 → 1.0，基于 tick 插值 */
    public float getOpenProgress(float partialTick) {
        float currentAge = age + partialTick;
        return Math.min(currentAge / 40.0F, 1.0F); // 40 tick 内完成
    }
}
