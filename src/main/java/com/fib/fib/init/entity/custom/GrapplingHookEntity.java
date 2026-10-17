package com.fib.fib.init.entity.custom;

import com.fib.fib.init.entity.ModEntities;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import com.fib.fib.init.item.custom.hook.IHookMaterial;
import com.fib.fib.init.sound.ModSounds;
import com.fib.fib.network.ModMessages;
import com.fib.fib.network.packet.HookClimbPacket;
import com.fib.fib.network.packet.SyncPlayerMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 钩爪实体类。
 * 驱动状态机（飞行、锚定、牵引、攀爬、远程挖掘、收回与双钩拴绳），
 * 负责方块/生物碰撞检测、物理移动矢量同步及音效粒子表现。
 */
public class GrapplingHookEntity extends Projectile {

    public enum State {
        FLYING,
        HOOKED,
        PULLING,
        CLIMBING,
        MINING,
        RETRACTING
    }

    public enum TargetType {
        NONE,
        BLOCK,
        LIVING,
        ITEM
    }

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_TYPE =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ENTITY_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROPE_LENGTH =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_AUTO_ASCEND =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PARTNER_HOOK_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOOK_MATERIAL_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_X =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_Y =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_Z =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);

    private ItemStack gunStack = ItemStack.EMPTY;
    private int obstructedTicks = 0;
    private Vec3 lastShooterPos = Vec3.ZERO;
    private int miningTicks = 0;
    private int maxMiningTicks = 20;
    private BlockPos miningBlockPos = BlockPos.ZERO;
    private int continuousDamageTimer = 0;
    private float maxFlyRange = 16.0f;
    private boolean pressurized = false;
    private boolean dualHook = false;
    private boolean itemProtection = false;
    private boolean miningModule = false;

    @Nullable
    private Entity hookedEntity = null;
    @Nullable
    private GrapplingHookEntity partnerHook = null;

    public GrapplingHookEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
        this.noCulling = true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    public GrapplingHookEntity(Level level, Player shooter, ItemStack gunStack) {
        this(ModEntities.GRAPPLING_HOOK.get(), level);
        setOwner(shooter);
        this.gunStack = gunStack.copy();
        setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());

        this.pressurized = GrapplingHookGunItem.hasPressurizedModule(gunStack);
        this.dualHook = GrapplingHookGunItem.hasDualHookModule(gunStack);
        this.itemProtection = GrapplingHookGunItem.hasItemProtectionModule(gunStack);
        this.miningModule = GrapplingHookGunItem.hasMiningModule(gunStack);
        this.maxFlyRange = pressurized ? 24.0f : 16.0f;

        IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);
        int matId = 0;
        if (material.hasWaterResistance()) matId = 1; // Prismarine
        else if (!material.canHookBlocks()) matId = 2; // Wool
        else if (material.canHookHardBlocks()) matId = 3; // Netherite
        entityData.set(DATA_HOOK_MATERIAL_ID, matId);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_STATE, State.FLYING.ordinal());
        entityData.define(DATA_TARGET_TYPE, TargetType.NONE.ordinal());
        entityData.define(DATA_TARGET_ENTITY_ID, -1);
        entityData.define(DATA_ROPE_LENGTH, 16.0f);
        entityData.define(DATA_AUTO_ASCEND, false);
        entityData.define(DATA_PARTNER_HOOK_ID, -1);
        entityData.define(DATA_HOOK_MATERIAL_ID, 0);
        entityData.define(DATA_ANCHOR_X, 0.0f);
        entityData.define(DATA_ANCHOR_Y, 0.0f);
        entityData.define(DATA_ANCHOR_Z, 0.0f);
    }

    public State getHookState() {
        int idx = entityData.get(DATA_STATE);
        State[] values = State.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : State.FLYING;
    }

    public void setHookState(State state) {
        entityData.set(DATA_STATE, state.ordinal());
    }

    public TargetType getTargetType() {
        int idx = entityData.get(DATA_TARGET_TYPE);
        TargetType[] values = TargetType.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : TargetType.NONE;
    }

    public void setTargetType(TargetType type) {
        entityData.set(DATA_TARGET_TYPE, type.ordinal());
    }

    public int getHookMaterialId() {
        return entityData.get(DATA_HOOK_MATERIAL_ID);
    }

    public Vec3 getAnchorPos() {
        return new Vec3(entityData.get(DATA_ANCHOR_X), entityData.get(DATA_ANCHOR_Y), entityData.get(DATA_ANCHOR_Z));
    }

    public void setAnchorPos(Vec3 pos) {
        entityData.set(DATA_ANCHOR_X, (float) pos.x);
        entityData.set(DATA_ANCHOR_Y, (float) pos.y);
        entityData.set(DATA_ANCHOR_Z, (float) pos.z);
    }

    public float getRopeLength() {
        return entityData.get(DATA_ROPE_LENGTH);
    }

    public void setRopeLength(float length) {
        entityData.set(DATA_ROPE_LENGTH, Math.max(1.0f, length));
    }

    public boolean isHooked() {
        State s = getHookState();
        return s == State.HOOKED || s == State.PULLING || s == State.CLIMBING || s == State.MINING;
    }

    public void setRetracting() {
        setHookState(State.RETRACTING);
        noPhysics = true;
    }

    public void startPulling() {
        setHookState(State.PULLING);
        obstructedTicks = 0;
    }

    public void toggleAutoAscend() {
        boolean current = entityData.get(DATA_AUTO_ASCEND);
        entityData.set(DATA_AUTO_ASCEND, !current);
    }

    public void setPartnerHook(GrapplingHookEntity partner) {
        this.partnerHook = partner;
        entityData.set(DATA_PARTNER_HOOK_ID, partner.getId());
    }

    @Nullable
    public GrapplingHookEntity getPartnerHook() {
        if (partnerHook == null && entityData.get(DATA_PARTNER_HOOK_ID) != -1) {
            Entity ent = level().getEntity(entityData.get(DATA_PARTNER_HOOK_ID));
            if (ent instanceof GrapplingHookEntity hook) {
                partnerHook = hook;
            }
        }
        return partnerHook;
    }

    public void clearPartnerHook() {
        this.partnerHook = null;
        entityData.set(DATA_PARTNER_HOOK_ID, -1);
    }

    @Nullable
    public Player getShooter() {
        Entity owner = getOwner();
        return owner instanceof Player player ? player : null;
    }

    // ==================== 主循环 Tick ====================

    @Override
    public void tick() {
        super.tick();

        Player shooter = getShooter();
        if (shooter == null || (!level().isClientSide && !shooter.isAlive())) {
            discard();
            return;
        }

        // 仅在服务端运行状态机核心逻辑
        if (!level().isClientSide) {
            checkItemSwitch(shooter);

            switch (getHookState()) {
                case FLYING -> tickFlying(shooter);
                case HOOKED -> tickHooked(shooter);
                case PULLING -> tickPulling(shooter);
                case CLIMBING -> tickClimbing(shooter);
                case MINING -> tickMining(shooter);
                case RETRACTING -> tickRetracting(shooter);
            }

            // 双钩拴绳连接距离限制（最大 20 格）
            if (dualHook) {
                GrapplingHookEntity partner = getPartnerHook();
                if (partner != null && !partner.isRemoved() && partner.getHookState() != State.RETRACTING && getHookState() != State.RETRACTING) {
                    if (position().distanceTo(partner.position()) > 20.0) {
                        setRetracting();
                        partner.setRetracting();
                    }
                }
            }
        } else {
            // 客户端位置更新
            if (getHookState() == State.HOOKED || getHookState() == State.MINING) {
                Vec3 anchor = getAnchorPos();
                setPos(anchor.x, anchor.y, anchor.z);
            }
        }
    }

    /** 检测玩家是否切换了非钩爪枪物品，若是则自动收回 */
    private void checkItemSwitch(Player shooter) {
        ItemStack held = shooter.getMainHandItem();
        if (!(held.getItem() instanceof GrapplingHookGunItem)) {
            // 切换到非钩爪物品时收回
            setRetracting();
        }
    }

    // ==================== 1. 飞行阶段 ====================

    private void tickFlying(Player shooter) {
        Vec3 pos = position();
        Vec3 motion = getDeltaMovement();

        // 检查飞行距离上限
        if (pos.distanceTo(shooter.position()) > maxFlyRange) {
            setRetracting();
            return;
        }

        // 水中阻力
        if (isInWater()) {
            if (getHookMaterialId() == 1) {
                // 海晶钩爪：水中无阻力并加速 20%
                setDeltaMovement(motion.scale(1.2));
            } else {
                // 普通材质：水中速度严重衰减
                setDeltaMovement(motion.scale(0.6).subtract(0, 0.05, 0));
            }
        } else {
            // 空气重力
            setDeltaMovement(motion.subtract(0, 0.03, 0));
        }

        Vec3 nextPos = pos.add(getDeltaMovement());
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hitResult.getType() != HitResult.Type.MISS) {
            onHit(hitResult);
        } else {
            setPos(nextPos.x, nextPos.y, nextPos.z);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !(target instanceof GrapplingHookEntity);
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level().isClientSide) return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level().getBlockState(pos);
        IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);

        // 羊毛钩爪无法钩方块
        if (!material.canHookBlocks()) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 0.8f, 1.0f);
            setRetracting();
            return;
        }

        // 高硬度方块（黑曜石等）判定
        float hardness = state.getDestroySpeed(level(), pos);
        if (hardness > 25.0f && !material.canHookHardBlocks()) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.8f, 1.8f);
            setRetracting();
            return;
        }

        // 锚定方块
        Vec3 hitPos = hitResult.getLocation();
        setPos(hitPos.x, hitPos.y, hitPos.z);
        setAnchorPos(hitPos);
        setDeltaMovement(Vec3.ZERO);
        setTargetType(TargetType.BLOCK);

        Player shooter = getShooter();
        if (shooter != null) {
            setRopeLength((float) hitPos.distanceTo(shooter.position()));
        }

        level().playSound(null, hitPos.x, hitPos.y, hitPos.z, ModSounds.HOOK_HIT_BLOCK.get(), SoundSource.PLAYERS, 0.9f, 1.0f);

        // 挖掘模式检测：若装备挖掘模块且处于开启状态
        if (miningModule && !dualHook) {
            GrapplingHookGunItem.MiningMode mMode = GrapplingHookGunItem.getMiningMode(gunStack);
            if (mMode != GrapplingHookGunItem.MiningMode.OFF && hardness >= 0.0f) {
                this.miningBlockPos = pos;
                this.miningTicks = 0;
                this.maxMiningTicks = Math.max(10, (int) (hardness * 12));
                setHookState(State.MINING);
                return;
            }
        }

        // 普通或攀岩模式锚定
        GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(gunStack);
        if (mode == GrapplingHookGunItem.Mode.CLIMB) {
            setHookState(State.CLIMBING);
        } else {
            setHookState(State.HOOKED);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (level().isClientSide) return;

        Entity target = hitResult.getEntity();
        Player shooter = getShooter();
        if (shooter == null || target == shooter) return;

        // 双钩模块不钩掉落物
        if (target instanceof ItemEntity item && dualHook) {
            return;
        }

        // 掉落物处理
        if (target instanceof ItemEntity item) {
            hookedEntity = item;
            entityData.set(DATA_TARGET_ENTITY_ID, item.getId());
            setTargetType(TargetType.ITEM);
            setAnchorPos(item.position());
            setHookState(State.PULLING);

            if (itemProtection) {
                item.setInvulnerable(true);
                item.clearFire();
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            return;
        }

        // 生物处理
        if (target instanceof LivingEntity living) {
            IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);
            float damage = material.getDamage(isInWater());

            if (damage > 0.0f) {
                living.hurt(level().damageSources().playerAttack(shooter), damage);
            }

            // Boss 或免疫牵引实体判定：仅造成伤害立即收回
            if (!living.canChangeDimensions() || living.getType().getDescriptionId().contains("boss")
                    || living.getType() == EntityType.WITHER || living.getType() == EntityType.ENDER_DRAGON) {
                setRetracting();
                return;
            }

            hookedEntity = living;
            entityData.set(DATA_TARGET_ENTITY_ID, living.getId());
            setTargetType(TargetType.LIVING);
            setAnchorPos(living.position().add(0, living.getBbHeight() / 2.0, 0));
            setRopeLength((float) living.position().distanceTo(shooter.position()));

            level().playSound(null, getX(), getY(), getZ(), ModSounds.HOOK_HIT_ENTITY.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            // 若激活挖掘模块，切入持续磨血状态
            if (miningModule && !dualHook && GrapplingHookGunItem.getMiningMode(gunStack) != GrapplingHookGunItem.MiningMode.OFF) {
                setHookState(State.MINING);
            } else {
                setHookState(State.HOOKED);
            }
        }
    }

    // ==================== 2. 锚定阶段 ====================

    private void tickHooked(Player shooter) {
        updateAnchorTarget();

        // 超过绳索极限距离 32 格自动脱钩
        if (position().distanceTo(shooter.position()) > 32.0) {
            setRetracting();
        }
    }

    private void updateAnchorTarget() {
        if (getTargetType() == TargetType.LIVING || getTargetType() == TargetType.ITEM) {
            if (hookedEntity == null || !hookedEntity.isAlive() || hookedEntity.isRemoved()) {
                setRetracting();
                return;
            }
            Vec3 pos = hookedEntity.position().add(0, hookedEntity.getBbHeight() / 2.0, 0);
            setPos(pos.x, pos.y, pos.z);
            setAnchorPos(pos);
        }
    }

    // ==================== 3. 牵引阶段 ====================

    private void tickPulling(Player shooter) {
        updateAnchorTarget();

        // 牵引能量持续消耗（0.05/tick，加压模块下 0.1/tick）
        float energyCost = pressurized ? 0.10f : 0.05f;
        ItemStack held = shooter.getMainHandItem();
        if (held.getItem() instanceof GrapplingHookGunItem) {
            if (!GrapplingHookGunItem.consumeEnergy(held, energyCost)) {
                setRetracting();
                return;
            }
        } else {
            setRetracting();
            return;
        }

        TargetType tType = getTargetType();
        Vec3 anchor = getAnchorPos();

        // A. 方块牵引
        if (tType == TargetType.BLOCK) {
            Vec3 playerPos = shooter.position();
            double distance = playerPos.distanceTo(anchor);

            // 停在方块前 1.5 格，防止穿模卡死
            if (distance <= 1.5) {
                setHookState(State.HOOKED);
                return;
            }

            // 障碍物阻挡检测：0.5s（10 ticks）位移过小则自动收回
            if (lastShooterPos != Vec3.ZERO && playerPos.distanceTo(lastShooterPos) < 0.05) {
                obstructedTicks++;
                if (obstructedTicks >= 10) {
                    setRetracting();
                    return;
                }
            } else {
                obstructedTicks = 0;
            }
            lastShooterPos = playerPos;

            // 运动矢量施加
            float pullSpeed = pressurized ? 0.9f : 0.6f;
            Vec3 dir = anchor.subtract(playerPos).normalize();
            Vec3 motion = dir.scale(pullSpeed);
            applyMotionToPlayer(shooter, motion);

            shooter.resetFallDistance();
            if (tickCount % 6 == 0) {
                level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), ModSounds.HOOK_PULL.get(), SoundSource.PLAYERS, 0.4f, 1.2f);
            }
        }

        // B. 生物牵引
        else if (tType == TargetType.LIVING && hookedEntity instanceof LivingEntity target) {
            double playerVol = shooter.getBbWidth() * shooter.getBbWidth() * shooter.getBbHeight();
            double targetVol = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
            float pullSpeed = pressurized ? 0.9f : 0.6f;

            if (targetVol < playerVol) {
                // 目标体积小于玩家：目标单向拉向玩家
                Vec3 dir = shooter.position().subtract(target.position()).normalize();
                target.setDeltaMovement(dir.scale(0.8));
                target.hurtMarked = true;
            } else {
                // 目标体积大于等于玩家：按最大生命值反比相向拉扯
                float hpPlayer = Math.max(1.0f, shooter.getMaxHealth());
                float hpTarget = Math.max(1.0f, target.getMaxHealth());
                float totalHp = hpPlayer + hpTarget;

                float playerRatio = hpTarget / totalHp;
                float targetRatio = hpPlayer / totalHp;

                Vec3 toTarget = target.position().subtract(shooter.position()).normalize();
                Vec3 playerMotion = toTarget.scale(pullSpeed * playerRatio);
                Vec3 targetMotion = toTarget.reverse().scale(pullSpeed * targetRatio);

                applyMotionToPlayer(shooter, playerMotion);
                target.setDeltaMovement(targetMotion);
                target.hurtMarked = true;
            }
            shooter.resetFallDistance();
        }

        // C. 掉落物牵引
        else if (tType == TargetType.ITEM && hookedEntity instanceof ItemEntity item) {
            Vec3 toPlayer = shooter.position().add(0, 0.5, 0).subtract(item.position());
            double dist = toPlayer.length();

            if (dist < 1.5) {
                // 接近玩家自动拾取
                item.playerTouch(shooter);
                if (itemProtection) {
                    item.setInvulnerable(false);
                }
                setRetracting();
            } else {
                Vec3 motion = toPlayer.normalize().scale(0.8);
                item.setDeltaMovement(motion);
                if (itemProtection) {
                    item.clearFire();
                    // 防止沉入岩浆与虚空
                    if (item.getY() < level().getMinBuildHeight() + 1) {
                        item.setDeltaMovement(motion.x, 0.5, motion.z);
                    }
                }
            }
        }
    }

    private void applyMotionToPlayer(Player player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new SyncPlayerMotionPacket(motion), serverPlayer);
        }
    }

    // ==================== 4. 攀岩阶段 ====================

    private void tickClimbing(Player shooter) {
        if (getTargetType() != TargetType.BLOCK) {
            setRetracting();
            return;
        }

        Vec3 anchor = getAnchorPos();
        Vec3 playerPos = shooter.position();
        float currentRope = getRopeLength();
        double dist = playerPos.distanceTo(anchor);

        // 自动上升处理
        if (entityData.get(DATA_AUTO_ASCEND)) {
            if (dist <= 1.5) {
                entityData.set(DATA_AUTO_ASCEND, false);
            } else {
                setRopeLength((float) (dist - 0.25));
                Vec3 dir = anchor.subtract(playerPos).normalize();
                applyMotionToPlayer(shooter, dir.scale(0.5));
            }
        }

        // 绳索张力约束：当超出绳索长度时限制移动
        if (dist > currentRope + 0.3) {
            Vec3 dir = anchor.subtract(playerPos).normalize();
            applyMotionToPlayer(shooter, dir.scale(0.3));
        }

        shooter.resetFallDistance();
    }

    public void handleClimbAction(int action) {
        switch (action) {
            case HookClimbPacket.ACTION_CLIMB_UP -> {
                entityData.set(DATA_AUTO_ASCEND, false);
                setRopeLength(getRopeLength() - 0.3f);
                Player shooter = getShooter();
                if (shooter != null) {
                    Vec3 dir = getAnchorPos().subtract(shooter.position()).normalize();
                    applyMotionToPlayer(shooter, dir.scale(0.4));
                }
            }
            case HookClimbPacket.ACTION_CLIMB_DOWN -> {
                entityData.set(DATA_AUTO_ASCEND, false);
                setRopeLength(getRopeLength() + 0.3f);
            }
            case HookClimbPacket.ACTION_AUTO_ASCEND_TOGGLE -> toggleAutoAscend();
            case HookClimbPacket.ACTION_STOP -> entityData.set(DATA_AUTO_ASCEND, false);
        }
    }

    // ==================== 5. 挖掘阶段 ====================

    private void tickMining(Player shooter) {
        // A. 挖掘方块
        if (getTargetType() == TargetType.BLOCK) {
            BlockState state = level().getBlockState(miningBlockPos);
            if (state.isAir()) {
                setRetracting();
                return;
            }

            miningTicks++;

            // 钻头运转音效与粒子
            if (miningTicks % 4 == 0) {
                level().playSound(null, miningBlockPos.getX(), miningBlockPos.getY(), miningBlockPos.getZ(),
                        ModSounds.HOOK_DRILL.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            miningBlockPos.getX() + 0.5, miningBlockPos.getY() + 0.5, miningBlockPos.getZ() + 0.5,
                            6, 0.2, 0.2, 0.2, 0.05
                    );
                }
            }

            if (miningTicks >= maxMiningTicks) {
                // 挖掘完成
                if (level() instanceof ServerLevel serverLevel) {
                    ItemStack held = shooter.getMainHandItem();
                    List<ItemStack> drops = Block.getDrops(state, serverLevel, miningBlockPos, null, shooter, held);
                    serverLevel.destroyBlock(miningBlockPos, false);

                    // 将掉落物存入钩爪 16 格仓
                    ItemStackHandler storage = GrapplingHookGunItem.getStorageHandler(held);
                    for (ItemStack drop : drops) {
                        ItemStack remainder = ItemHandlerHelper.insertItem(storage, drop, false);
                        if (!remainder.isEmpty()) {
                            // 溢出掉落物散落于世界中
                            Block.popResource(serverLevel, miningBlockPos, remainder);
                        }
                    }
                    GrapplingHookGunItem.saveStorageHandler(held, storage);
                }
                setRetracting();
            }
        }

        // B. 钻头持续磨血
        else if (getTargetType() == TargetType.LIVING && hookedEntity instanceof LivingEntity living) {
            if (!living.isAlive() || living.isRemoved()) {
                setRetracting();
                return;
            }

            continuousDamageTimer++;
            if (continuousDamageTimer % 10 == 0) {
                living.hurt(level().damageSources().playerAttack(shooter), 2.0f);
                level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        ModSounds.HOOK_DRILL.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        } else {
            setRetracting();
        }
    }

    // ==================== 6. 收回阶段 ====================

    private void tickRetracting(Player shooter) {
        Vec3 targetPos = shooter.getEyePosition().subtract(0, 0.2, 0);
        Vec3 currentPos = position();
        Vec3 toShooter = targetPos.subtract(currentPos);

        if (toShooter.length() < 1.2) {
            discard();
            return;
        }

        Vec3 returnMotion = toShooter.normalize().scale(1.8);
        setDeltaMovement(returnMotion);
        setPos(currentPos.x + returnMotion.x, currentPos.y + returnMotion.y, currentPos.z + returnMotion.z);
    }

    // ==================== 网络同步与持久化 ====================

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_STATE, tag.getInt("HookState"));
        entityData.set(DATA_TARGET_TYPE, tag.getInt("TargetType"));
        entityData.set(DATA_ROPE_LENGTH, tag.getFloat("RopeLength"));
        entityData.set(DATA_HOOK_MATERIAL_ID, tag.getInt("MaterialId"));
        if (tag.contains("GunStack")) {
            gunStack = ItemStack.of(tag.getCompound("GunStack"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("HookState", entityData.get(DATA_STATE));
        tag.putInt("TargetType", entityData.get(DATA_TARGET_TYPE));
        tag.putFloat("RopeLength", entityData.get(DATA_ROPE_LENGTH));
        tag.putInt("MaterialId", entityData.get(DATA_HOOK_MATERIAL_ID));
        if (!gunStack.isEmpty()) {
            tag.put("GunStack", gunStack.save(new CompoundTag()));
        }
    }
}
